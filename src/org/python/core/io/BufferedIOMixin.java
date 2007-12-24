/* Copyright (c) 2007 Jython Developers */
package org.python.core.io;

import org.python.core.Py;
import org.python.core.PyException;

/**
 * A mixin implementation of BufferedIOBase with an underlying raw
 * stream.
 *
 * This passes most requests on to the underlying raw stream. It does
 * *not* provide implementations of read(), readinto() or write().
 *
 * @author Philip Jenvey
 */
public abstract class BufferedIOMixin extends BufferedIOBase {

    /** The underlying raw io stream */
    protected RawIOBase rawIO;

    /** The size of the buffer */
    protected int bufferSize;

    /**
     * Initialize this buffer, wrapping the given RawIOBase.
     *
     * @param rawIO a RawIOBase to wrap
     */
    public BufferedIOMixin(RawIOBase rawIO) {
        this(rawIO, DEFAULT_BUFFER_SIZE);
    }

    /**
     * Initialize this buffer, wrapping the given RawIOBase.
     *
     * @param rawIO a RawIOBase to wrap
     * @param bufferSize the size of the buffer
     */
    public BufferedIOMixin(RawIOBase rawIO, int bufferSize) {
        this.rawIO = rawIO;
        this.bufferSize = bufferSize;
    }

    /** {@inheritDoc} */
    public long seek(long pos, int whence) {
        return rawIO.seek(pos, whence);
    }

    /** {@inheritDoc} */
    public long tell() {
        return rawIO.tell();
    }

    /** {@inheritDoc} */
    public long truncate(long size) {
        return rawIO.truncate(size);
    }

    /** {@inheritDoc} */
    public void flush() {
        rawIO.flush();
    }

    /** {@inheritDoc} */
    public void close() {
        if (closed()) {
            return;
        }
        try {
            flush();
        } catch (PyException pye) {
            if (!Py.matchException(pye, Py.IOError)) {
                throw pye;
            }
            // If flush() fails, just give up
        }
        rawIO.close();
    }

    /** {@inheritDoc} */
    public RawIOBase fileno() {
        return rawIO.fileno();
    }

    /** {@inheritDoc} */
    public boolean isatty() {
        return rawIO.isatty();
    }

    /** {@inheritDoc} */
    public boolean readable() {
        return rawIO.readable();
    }

    /** {@inheritDoc} */
    public boolean writable() {
        return rawIO.writable();
    }

    /** {@inheritDoc} */
    public boolean closed() {
        return rawIO.closed();
    }

    /** {@inheritDoc} */
    public Object __tojava__(Class cls) {
        return rawIO.__tojava__(cls);
    }
}
