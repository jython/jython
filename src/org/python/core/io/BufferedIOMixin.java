/* Copyright (c) 2007 Jython Developers */
package org.python.core.io;

import java.io.InputStream;
import java.io.OutputStream;

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

    @Override
    public long seek(long pos, int whence) {
        return rawIO.seek(pos, whence);
    }

    @Override
    public long tell() {
        return rawIO.tell();
    }

    @Override
    public long truncate(long size) {
        return rawIO.truncate(size);
    }

    @Override
    public void flush() {
        rawIO.flush();
    }

    @Override
    public void close() {
        if (closed()) {
            return;
        }
        try {
            flush();
        } catch (PyException pye) {
            if (!pye.match(Py.IOError)) {
                throw pye;
            }
            // If flush() fails, just give up
        }
        rawIO.close();
    }

    @Override
    public RawIOBase fileno() {
        return rawIO.fileno();
    }

    @Override
    public boolean isatty() {
        return rawIO.isatty();
    }

    @Override
    public boolean readable() {
        return rawIO.readable();
    }

    @Override
    public boolean writable() {
        return rawIO.writable();
    }

    @Override
    public boolean closed() {
        return rawIO.closed();
    }

    @Override
    public InputStream asInputStream() {
        return rawIO.asInputStream();
    }

    @Override
    public OutputStream asOutputStream() {
        return rawIO.asOutputStream();
    }
}
