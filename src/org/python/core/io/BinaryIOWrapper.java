/* Copyright (c) 2007 Jython Developers */
package org.python.core.io;

import java.nio.ByteBuffer;

import org.python.core.PyString;

/**
 * A Buffered text stream in binary mode.
 *
 * @author Philip Jenvey
 */
public class BinaryIOWrapper extends TextIOBase {

    /**
     * Contruct a TextIOBase in binary mode, wrapping the given
     * BufferedIOBase.
     *
     * @param bufferedIO {@inheritDoc}
     */
    public BinaryIOWrapper(BufferedIOBase bufferedIO) {
        super(bufferedIO);
    }

    /** {@inheritDoc} */
    public String read(int size) {
        if (size < 0) {
            return readall();
        }

        if (!readahead.hasRemaining()) {
            return PyString.from_bytes(bufferedIO.read(size));
        }

        ByteBuffer data = ByteBuffer.allocate(size);
        if (readahead.remaining() >= size) {
            // Fulfill the read entirely from the readahead
            int readaheadLimit = readahead.limit();
            readahead.limit(readahead.position() + size);
            data.put(readahead);
            readahead.limit(readaheadLimit);
            data.flip();
            return PyString.from_bytes(data);
        }

        // Drain the readahead then request more from the buffer
        data.put(readahead);
        clearReadahead();
        bufferedIO.readinto(data);
        data.flip();
        return PyString.from_bytes(data);
    }

    /** {@inheritDoc} */
    public String readall() {
        if (!readahead.hasRemaining()) {
            return PyString.from_bytes(bufferedIO.readall());
        }

        ByteBuffer remaining = bufferedIO.readall();
        ByteBuffer all = ByteBuffer.allocate(readahead.remaining() +
                                             remaining.remaining());
        all.put(readahead);
        clearReadahead();
        all.put(remaining);
        all.flip();
        return PyString.from_bytes(all);
    }

    /** {@inheritDoc} */
    public String readline(int size) {
        // Avoid ByteBuffer (this.readahead) and StringBuilder
        // (this.builder) method calls in the inner loop by reading
        // directly from the readahead's backing array and writing to
        // an interim char array (this.interimBuilder)
        byte[] readaheadArray;
        int readaheadPos;
        int interimBuilderPos;

        do {
            readaheadArray = readahead.array();
            readaheadPos = readahead.position();
            interimBuilderPos = 0;

            while (readaheadPos < readahead.limit() &&
                   (size < 0 || builder.length() + interimBuilderPos < size)) {
                char next = (char)(readaheadArray[readaheadPos++] & 0xff);
                interimBuilder[interimBuilderPos++] = next;

                if (next == '\n') {
                    builder.append(interimBuilder, 0, interimBuilderPos);

                    // Reposition the readahead to where we ended
                    readahead.position(readaheadPos);

                    return drainBuilder();
                }
            }

            builder.append(interimBuilder, 0, interimBuilderPos);

        } while ((size < 0 || builder.length() < size) && readChunk() > 0);

        // Reposition the readahead to where we ended. The position is
        // invalid if the readahead is empty (at EOF; readChunk()
        // returned 0)
        if (readahead.hasRemaining()) {
            readahead.position(readaheadPos);
        }

        return drainBuilder();
    }

    /** {@inheritDoc} */
    public int write(String buf) {
        if (readahead.hasRemaining()) {
            clearReadahead();
        }
        return bufferedIO.write(ByteBuffer.wrap(PyString.to_bytes(buf)));
    }
}
