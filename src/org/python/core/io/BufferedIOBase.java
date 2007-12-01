/* Copyright (c) 2007 Jython Developers */
package org.python.core.io;

import java.nio.ByteBuffer;

/**
 * Base class for buffered I/O objects.
 *
 * @author Philip Jenvey
 */
public abstract class BufferedIOBase extends IOBase {

    /**
     * Read and return up to size bytes, contained in a ByteBuffer.
     *
     * ByteBuffers returned from read are already flip()'d.
     *
     * Returns an empty ByteBuffer on EOF
     *
     * @param size the number of bytes to read
     * @return a ByteBuffer containing the bytes read
     */
    public ByteBuffer read(int size) {
        if (size < 0) {
            return readall();
        }

        ByteBuffer bytes = ByteBuffer.allocate(size);
        readinto(bytes);
        // flip()ing here is more convenient as there's no real use
        // case for appending to buffers returned from read. readinto
        // doesn't/shouldn't flip()
        bytes.flip();
        return bytes;
    }

    /**
     * Read until EOF.
     *
     * @return a ByteBuffer containing the bytes read
     */
    public ByteBuffer readall() {
        unsupported("readall");
        return null;
    }

    /**
     * Read up to bytes.remaining() bytes into the given ByteBuffer.
     *
     * Returns number of bytes read (0 for EOF).
     *
     * @param bytes a ByteBuffer to read bytes into
     * @return the amount of data read as an int
     */
    public int readinto(ByteBuffer bytes) {
        unsupported("readinto");
        return -1;
    }

    /**
     * Write the given ByteBuffer to the IO stream.
     *
     * Returns the number of bytes written, which may be less than
     * bytes.remaining().
     *
     * @param bytes a ByteBuffer value
     * @return the number of bytes written as an int
     */
    public int write(ByteBuffer bytes) {
        unsupported("write");
        return -1;
    }

    /**
     * Returns buffered bytes without advancing the position.
     *
     * The argument indicates a desired minimal number of bytes; we do
     * at most one raw read to satisfy it. We never return more than
     * the size of the underlying buffer;
     *
     * @param size the minimal number of bytes as an int
     * @return a ByteBuffer containing the bytes read
     */
    public ByteBuffer peek(int size) {
        unsupported("peek");
        return null;
    }

    /**
     * Reads up to bytes.remaining() bytes.
     *
     * Returns up to bytes.remaining() bytes. If at least one byte is
     * buffered, we only return buffered bytes. Otherwise, we do one
     * raw read.
     *
     * @param bytes a ByteBuffer to read bytes into
     * @return the amount of data read as an int
     */
    public int read1(ByteBuffer bytes) {
        unsupported("read1");
        return -1;
    }

    /**
     * Return true if this objects buffer contains any data.
     *
     * @return boolean whether or not any data is currently buffered
     */
    public boolean buffered() {
        unsupported("buffered");
        return false;
    }

    /**
     * Clear the read buffer if one exists.
     *
     */
    public void clear() {
        unsupported("clear");
    }
}
