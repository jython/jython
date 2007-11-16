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

    /** {@inheritDoc} */
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
        bytes.limit(bytesToWrite);

        int totalToWrite = total - toBuffer;
        int count = totalToWrite;
        // Prepare the buffer for writing
        buffer.flip();
        while (count > 0) {
            count -= rawIO.write(new ByteBuffer[] {buffer, bytes});
        }
        // Prepare the buffer for buffering
        buffer.clear();

        if (toBuffer > 0) {
            bytes.limit(origBytesLimit);
            bytes.position(bytesToWrite);
            buffer.put(bytes);
        }

        return totalToWrite;
    }

    /** {@inheritDoc} */
    public void flush() {
        if (buffer.position() == 0) {
            // Empty buffer
            return;
        }

        buffer.flip();
        while (buffer.hasRemaining()) {
            rawIO.write(buffer);
        }
        buffer.clear();
    }

    /** {@inheritDoc} */
    public long tell() {
        return rawIO.tell() + buffer.position();
    }

    /** {@inheritDoc} */
    public long seek(long pos, int whence) {
        flush();
        return rawIO.seek(pos, whence);
    }

    /** {@inheritDoc} */
    public boolean buffered() {
        return buffer.position() > 0;
    }

    /** {@inheritDoc} */
    public ByteBuffer readall() {
        // Never readable; just raise the appropriate exception
        checkClosed();
        checkReadable();
        return null;
    }

    /** {@inheritDoc} */
    public int readinto(ByteBuffer bytes) {
        // Never readable; just raise the appropriate exception
        checkClosed();
        checkReadable();
        return -1;
    }

    /** {@inheritDoc} */
    public int read1(ByteBuffer bytes) {
        // Never readable; just raise the appropriate exception
        checkClosed();
        checkReadable();
        return -1;
    }

    /** {@inheritDoc} */
    public boolean readable() {
        return false;
    }
}
