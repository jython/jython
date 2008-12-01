/* Copyright (c) 2007 Jython Developers */
package org.python.core.io;

import java.io.InputStream;
import java.io.OutputStream;

import org.python.constantine.platform.Errno;
import org.python.core.Py;
import org.python.core.PyException;

/**
 * Base class for all I/O classes.
 *
 * IOBase and its descendents in org.python.core.io are based off
 * Python 3's new io module (PEP 3116).
 *
 * This does not define read(), readinto() and write(), nor readline()
 * and friends, since their signatures vary per layer.
 *
 * @author Philip Jenvey
 */
public abstract class IOBase {

    /** The default size of generic buffers */
    public static final int DEFAULT_BUFFER_SIZE = 8192;

    /** Byte representation of the Line Feed character */
    protected static final byte LF_BYTE = 10;

    /** true if the file has been closed */
    private boolean closed = false;

    /**
     * Seek to byte offset pos relative to the start of the stream.
     *
     * Returns the new absolute position.
     *
     * @param pos a long position value
     * @return a long position value seeked to
     */
    public long seek(long pos) {
        return seek(pos, 0);
    }

    /**
     * Seek to byte offset pos relative to position indicated by
     * whence:
     *   0 Start of stream (the default). pos should be >= 0;
     *   1 Current position - whence may be negative;
     *   2 End of stream - whence usually negative.
     *
     * Returns the new absolute position.
     *
     * @param pos a long position value
     * @param whence an int whence value
     * @return a long position value seeked to
     */
    public long seek(long pos, int whence) {
        unsupported("seek");
        return -1;
    }

    /**
     * Return the current stream position.
     *
     * @return a long position value
     */
    public long tell() {
        return seek(0, 1);
    }

    /**
     * Truncate file to size in bytes.
     *
     * Returns the new size.
     *
     * @param size a long size to truncate to
     * @return a long size value the file was truncated to
     */
    public long truncate(long size) {
        unsupported("truncate");
        return -1;
    }

    /**
     * Flushes write buffers, if applicable.
     *
     * This is a no-op for read-only and non-blocking streams.
     *
     */
    public void flush() {
    }

    /**
     * Flushes and closes the IO object.
     *
     * This must be idempotent. It should also set a flag for the
     * 'closed' property (see below) to test.
     */
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
        closed = true;
    }

    /**
     * Returns underlying file descriptor if one exists.
     *
     * Raises IOError if the IO object does not use a file descriptor.
     *
     * @return a file descriptor
     */
    public RawIOBase fileno() {
        unsupported("fileno");
        return null;
    }

    /**
     * Returns whether this is an 'interactive' stream.
     *
     * Returns False if we don't know.
     *
     * @return a boolean, true if an 'interactive' stream
     */
    public boolean isatty() {
        checkClosed();
        return false;
    }

    /**
     * Return whether this file was opened for reading.
     *
     * @return true if the file was opened for reading
     */
    public boolean readable() {
        return false;
    }

    /**
     * Raise an IOError if the file is not readable.
     *
     */
    public void checkReadable() {
        if (!readable()) {
            throw Py.IOError(Errno.EBADF);
        }
    }

    /**
     * Return whether this file was opened for writing.
     *
     * @return true if the file was opened for writing
     */
    public boolean writable() {
        return false;
    }

    /**
     * Raise an IOError if the file is not writable.
     *
     */
    public void checkWritable() {
        if (!writable()) {
            throw Py.IOError(Errno.EBADF);
        }
    }

    /**
     * Return whether this file has been closed.
     *
     * @return true if the file has been closed
     */
    public boolean closed() {
        return closed;
    }

    /**
     * Raise a ValueError if the file is closed.
     *
     */
    public void checkClosed() {
        if (closed()) {
            throw Py.ValueError("I/O operation on closed file");
        }
    }

    /**
     * Coerce this into an OutputStream if possible, or return null.
     */
    public OutputStream asOutputStream() {
        return null;
    }

    /**
     * Coerce this into an InputStream if possible, or return null.
     */
    public InputStream asInputStream() {
        return null;
    }

    /**
     * Raise a TypeError indicating the specified operation is not
     * supported.
     *
     * @param methodName the String name of the operation
     */
    protected void unsupported(String methodName) {
        String qualifiedName = getClass().getName();
        String className = qualifiedName.substring(qualifiedName.lastIndexOf('.') + 1);
        throw Py.IOError(String.format("%s.%s() not supported", className, methodName));
    }
}
