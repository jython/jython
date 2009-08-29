/* Copyright (c) 2007 Jython Developers */
package org.python.core.io;

import java.nio.ByteBuffer;

/**
 * Buffer for a writable sequential RawIO object.
 *
 * @author Philip Jenvey
 */
public class BufferedWriter extends BufferedIOMixin {

    /** The underlying buffer */
    protected ByteBuffer buffer;

    /**
     * Construct a BufferedWriter of bufferSize, wrapping the given
     * RawIOBase.
     *
     * @param rawIO {@inheritDoc}
     * @param bufferSize {@inheritDoc}
     */
    public BufferedWriter(RawIOBase rawIO, int bufferSize) {
        super(rawIO, bufferSize);
        rawIO.checkWritable();
        buffer = ByteBuffer.allocate(this.bufferSize);
    }

    @Override
    public int write(ByteBuffer bytes) {
        if (bufferSize == 0) {
            return rawIO.write(bytes);
        }

        int bytesSize = bytes.remaining();
        // The total amount of data on hand; the buffer plus 'bytes'
        int total = buffer.position() + bytesSize;

        if (total < bufferSize) {
            // Have less than bufferSize on hand: just buffer
            buffer.put(bytes);
            return bytesSize;
        }

        // The amount of 'bytes' leftover, to buffer, after writing in
        // bufferSize chunks
        int toBuffer = total % bufferSize;
        // The amount of 'bytes' to be written
        int bytesToWrite = bytesSize - toBuffer;

        int origBytesLimit = bytes.limit();
        bytes.limit(bytes.position() + bytesToWrite);

        int totalToWrite = total - toBuffer;
        int count = totalToWrite;
        ByteBuffer[] bulk = new ByteBuffer[] {buffer, bytes};
        // Prepare the buffer for writing
        buffer.flip();
        while (count > 0) {
            count -= rawIO.write(bulk);
        }
        // Prepare the buffer for buffering
        buffer.clear();

        if (toBuffer > 0) {
            bytes.limit(origBytesLimit);
            buffer.put(bytes);
        }

        return totalToWrite;
    }

    @Override
    public void flush() {
        if (buffer.position() > 0) {
            buffer.flip();
            while (buffer.hasRemaining()) {
                rawIO.write(buffer);
            }
            buffer.clear();
        }
        super.flush();
    }

    @Override
    public long tell() {
        return rawIO.tell() + buffer.position();
    }

    @Override
    public long seek(long pos, int whence) {
        flush();
        return rawIO.seek(pos, whence);
    }

    @Override
    public boolean buffered() {
        return buffer.position() > 0;
    }

    @Override
    public ByteBuffer readall() {
        // Never readable; just raise the appropriate exception
        checkClosed();
        checkReadable();
        return null;
    }

    @Override
    public int readinto(ByteBuffer bytes) {
        // Never readable; just raise the appropriate exception
        checkClosed();
        checkReadable();
        return -1;
    }

    @Override
    public int read1(ByteBuffer bytes) {
        // Never readable; just raise the appropriate exception
        checkClosed();
        checkReadable();
        return -1;
    }

    @Override
    public boolean readable() {
        return false;
    }
}
