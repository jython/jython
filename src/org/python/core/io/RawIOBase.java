/* Copyright (c) 2007 Jython Developers */
package org.python.core.io;

import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.python.core.Py;

/**
 * Base class for raw binary I/O.
 *
 * RawIOBases wrap raw Java I/O objects (typically nio Channels). They
 * provide a convenient means of handling raw Java I/O objects in the
 * context of Python files.
 *
 * RawIOBases maintain state about their underlying I/O objects (such
 * as their mode) and translate Java exceptions into PyExceptions.
 *
 * The read() method is implemented by calling readinto(); derived
 * classes that want to support read() only need to implement
 * readinto() as a primitive operation. In general, readinto() can be
 * more efficient than read().
 *
 * @author Philip Jenvey
 */
public abstract class RawIOBase extends IOBase {

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

        ByteBuffer buf = ByteBuffer.allocate(size);
        readinto(buf);
        // flip()ing here is more convenient as there's no real use
        // case for appending to buffers returned from read. readinto
        // doesn't/shouldn't flip()
        buf.flip();
        return buf;
    }

    /**
     * Read until EOF, using multiple read() calls.
     *
     * @return a ByteBuffer containing the bytes read
     */
    public ByteBuffer readall() {
        long count = 0;
        List chunks = new ArrayList();
        while (true) {
            ByteBuffer chunk = read(DEFAULT_BUFFER_SIZE);
            int chunkSize = chunk.remaining();
            if (chunkSize == 0) {
                break;
            }
            chunks.add(chunk);
            count += chunkSize;
        }

        if (count > Integer.MAX_VALUE) {
            throw Py.OverflowError("requested number of bytes is more than a Python " +
                                   "string can hold");
        }

        ByteBuffer all = ByteBuffer.allocate((int)count);
        for (Iterator chunkIter = chunks.iterator(); chunkIter.hasNext();) {
            all.put((ByteBuffer)chunkIter.next());
        }
        all.flip();
        return all;
    }

    /**
     * Read up to buf.remaining() bytes into buf.
     *
     * Returns number of bytes read (0 for EOF).
     *
     * @param buf a ByteBuffer to read bytes into
     * @return the amount of data read as an int
     */
    public int readinto(ByteBuffer buf) {
        unsupported("readinto");
        return -1;
    }

    /**
     * Read bytes into each of the specified ByteBuffers.
     *
     * Returns number of bytes read (0 for EOF).
     *
     * @param bufs an array of ByteBuffers to read bytes into
     * @return the amount of data read as a long
     */
    public long readinto(ByteBuffer[] bufs) {
        long count = 0;
        int bufCount;
        for (int i = 0; i < bufs.length; i++) {
            ByteBuffer buf = (ByteBuffer)bufs[i];
            if (!buf.hasRemaining()) {
                continue;
            }
            if ((bufCount = readinto(buf)) == 0) {
                break;
            }
            count += bufCount;
        }
        return count;
    }

    /**
     * Write the given ByteBuffer to the IO stream.
     *
     * Returns the number of bytes written, which may be less than
     * buf.remaining().
     *
     * @param buf a ByteBuffer value
     * @return the number of bytes written as an int
     */
    public int write(ByteBuffer buf) {
        unsupported("write");
        return -1;
    }

    /**
     * Write the given ByteBuffers to the IO stream.
     *
     * Returns the number of bytes written, which may be less than the
     * combined value of all the buf.remaining()'s.
     *
     * @param bufs an array of  ByteBuffers
     * @return the number of bytes written as a long
     */
    public long write(ByteBuffer[] bufs) {
        long count = 0;
        int bufCount;
        for (int i = 0; i < bufs.length; i++) {
            ByteBuffer buf = (ByteBuffer)bufs[i];
            if (!buf.hasRemaining()) {
                continue;
            }
            if ((bufCount = write(buf)) == 0) {
                break;
            }
            count += bufCount;
        }
        return count;
    }

    /** {@inheritDoc} */
    public RawIOBase fileno() {
        checkClosed();
        return this;
    }

    /**
     * Return the underlying Java nio Channel.
     *
     * @return the underlying Java nio Channel
     */
    public abstract Channel getChannel();
}
