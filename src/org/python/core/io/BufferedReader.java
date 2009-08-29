/* Copyright (c) 2007 Jython Developers */
package org.python.core.io;

import java.nio.ByteBuffer;

/**
 * Buffer for a readable sequential RawIO object.
 *
 * @author Philip Jenvey
 */
public class BufferedReader extends BufferedIOMixin {

    /** The underlying buffer */
    protected ByteBuffer buffer;

    /**
     * Construct a BufferedReader of bufferSize, wrapping the given
     * RawIOBase.
     *
     * @param rawIO {@inheritDoc}
     * @param bufferSize {@inheritDoc}
     */
    public BufferedReader(RawIOBase rawIO, int bufferSize) {
        super(rawIO, bufferSize);
        rawIO.checkReadable();
        buffer = ByteBuffer.allocate(this.bufferSize);
        clear();
    }

    @Override
    public int readinto(ByteBuffer bytes) {
        int size = bytes.remaining();

        if (size == 0) {
            return 0;
        }

        if (buffer.remaining() >= size) {
            // Fulfill the read entirely from the buffer
            int bufferLimit = buffer.limit();
            buffer.limit(buffer.position() + size);
            bytes.put(buffer);
            buffer.limit(bufferLimit);
            return size;
        }

        // Drain the buffer then request more from the RawIO
        bytes.put(buffer);
        buffer.clear();

        // Only attempt one read. The buffering layer should not wait
        // for more data (block) to fulfill the entire read
        long read = rawIO.readinto(new ByteBuffer[] {bytes, buffer});
        read -= buffer.flip().limit();

        // This is an int after subtracting the buffer size anyway
        return (int)read;
    }

    @Override
    public ByteBuffer readall() {
        ByteBuffer remaining = rawIO.readall();

        if (!buffer.hasRemaining()) {
            return remaining;
        }

        ByteBuffer all = ByteBuffer.allocate(buffer.remaining() + remaining.remaining());
        all.put(buffer);
        clear();
        all.put(remaining);
        all.flip();
        return all;
    }

    @Override
    public ByteBuffer peek(int size) {
        if (buffer.remaining() < Math.min(size, bufferSize)) {
            // Prepare to fill the buffer
            if (buffer.position() == 0) {
                buffer.limit(buffer.capacity());
            } else {
                buffer.compact();
            }

            rawIO.readinto(buffer);
            buffer.flip();
        }
        return buffer;
    }

    @Override
    public int read1(ByteBuffer bytes) {
        int size = bytes.remaining();
        if (size == 0) {
            return 0;
        }

        if (bufferSize > 0) {
            peek(1);
            int bufferedSize = buffer.remaining();
            if (bufferedSize < size) {
                bytes.limit(bytes.position() + bufferedSize);
            }
        }
        return readinto(bytes);
    }

    @Override
    public long tell() {
        return rawIO.tell() - buffer.remaining();
    }

    @Override
    public long seek(long pos, int whence) {
        if (whence == 1) {
            pos -= buffer.remaining();
        }
        pos = rawIO.seek(pos, whence);
        clear();
        return pos;
    }

    @Override
    public boolean buffered() {
        return buffer.hasRemaining();
    }

    @Override
    public void clear() {
        buffer.clear().limit(0);
    }

    @Override
    public int write(ByteBuffer bytes) {
        // Never writable; just raise the appropriate exception
        checkClosed();
        checkWritable();
        return -1;
    }

    @Override
    public boolean writable() {
        return false;
    }
}
