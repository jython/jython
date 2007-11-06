/* Copyright (c) 2007 Jython Developers */
package org.python.core.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

import org.python.core.Py;

/**
 * Raw I/O implementation for simple streams.
 *
 * Supports Input/Outputstreams and Readable/WritableByteChannels.
 *
 * @author Philip Jenvey
 */
public class StreamIO extends RawIOBase {

    /** The underlying read channel */
    private ReadableByteChannel readChannel;

    /** The underlying write channel */
    private WritableByteChannel writeChannel;

    /** The original InputStream if one was passed in, used for
     * __tojava__ */
    private InputStream inputStream;

    /** The original OutputStream if one was passed in, used for
     * __tojava__ */
    private OutputStream outputStream;

    /** true if the Streams refer to a terminal device */
    private boolean isatty;

    /** true if the underlying file is actually closed on close() */
    private boolean closefd;

    /**
     * Construct a StreamIO for the given read/write channels.
     *
     * @param readChannel a ReadableByteChannel
     * @param writeChannel a WritableByteChannel
     * @param isatty boolean whether this io object is a tty device
     * @param closefd boolean whether the underlying file is closed on
     *                close() (defaults to True)
     */
    public StreamIO(ReadableByteChannel readChannel,
                    WritableByteChannel writeChannel, boolean isatty, boolean closefd) {
        this.readChannel = readChannel;
        this.writeChannel = writeChannel;
        this.isatty = isatty;
        this.closefd = closefd;
    }

    /**
     * Construct a StreamIO for the given read channel.
     *
     * @param readChannel a ReadableByteChannel
     * @param isatty boolean whether this io object is a tty device
     * @param closefd boolean whether the underlying file is closed on
     *                close() (defaults to True)
     */
    public StreamIO(ReadableByteChannel readChannel, boolean isatty, boolean closefd) {
        this(readChannel, null, isatty, closefd);
    }

    /**
     * Construct a StreamIO for the given write channel.
     *
     * @param writeChannel a WritableByteChannel
     * @param isatty boolean whether this io object is a tty device
     * @param closefd boolean whether the underlying file is closed on
     *                close() (defaults to True)
     */
    public StreamIO(WritableByteChannel writeChannel, boolean isatty, boolean closefd) {
        this(null, writeChannel, isatty, closefd);
    }

    /**
     * Construct a StreamIO for the given read/write streams.
     *
     * @param inputStream an InputStream
     * @param outputStream an OutputStream
     * @param isatty boolean whether this io object is a tty device
     * @param closefd boolean whether the underlying file is closed on
     *                close() (defaults to True)
     */
    public StreamIO(InputStream inputStream, OutputStream outputStream, boolean isatty,
                    boolean closefd) {
        this(inputStream == null ? null : Channels.newChannel(inputStream),
             outputStream == null ? null : Channels.newChannel(outputStream), isatty,
             closefd);
        this.inputStream = inputStream;
        this.outputStream = outputStream;
    }

    /**
     * Construct a StreamIO for the given read/write streams.
     *
     * @param inputStream an InputStream
     * @param isatty boolean whether this io object is a tty device
     * @param closefd boolean whether the underlying file is closed on
     *                close() (defaults to True)
     */
    public StreamIO(InputStream inputStream, boolean isatty, boolean closefd) {
        this(inputStream, null, isatty, closefd);
    }

    /**
     * Construct a StreamIO for the given read/write streams.
     *
     * @param outputStream an OutputStream
     * @param isatty boolean whether this io object is a tty device
     * @param closefd boolean whether the underlying file is closed on
     *                close() (defaults to True)
     */
    public StreamIO(OutputStream outputStream, boolean isatty, boolean closefd) {
        this(null, outputStream, isatty, closefd);
    }

    /** {@inheritDoc} */
    public int readinto(ByteBuffer buf) {
        checkClosed();
        checkReadable();
        try {
            return readChannel.read(buf);
        } catch (IOException ioe) {
            throw Py.IOError(ioe);
        }
    }

    /** {@inheritDoc} */
    public int write(ByteBuffer buf) {
        checkClosed();
        checkWritable();
        try {
            return writeChannel.write(buf);
        } catch (IOException ioe) {
            throw Py.IOError(ioe);
        }
    }

    /** {@inheritDoc} */
    public void close() {
        if (closed()) {
            return;
        }
        if (closefd) {
            try {
                if (readChannel != null) {
                    readChannel.close();
                    if (writeChannel != null && readChannel != writeChannel) {
                        writeChannel.close();
                    }
                } else {
                    writeChannel.close();
                }
            } catch (IOException ioe) {
                throw Py.IOError(ioe);
            }
        }
        super.close();
    }

    /** {@inheritDoc} */
    public boolean isatty() {
        checkClosed();
        return isatty;
    }

    /** {@inheritDoc} */
    public boolean readable() {
        return readChannel != null;
    }

    /** {@inheritDoc} */
    public boolean writable() {
        return writeChannel != null;
    }

    /** {@inheritDoc} */
    public Object __tojava__(Class cls) {
        if (OutputStream.class.isAssignableFrom(cls) && writable()) {
            if (outputStream == null) {
                return Channels.newOutputStream(writeChannel);
            }
            return outputStream;
        } else if (InputStream.class.isAssignableFrom(cls) && readable()) {
            if (inputStream == null) {
                return Channels.newInputStream(readChannel);
            }
            return inputStream;
        }
        return super.__tojava__(cls);
    }
}
