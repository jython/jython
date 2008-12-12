/* Copyright (c) 2007 Jython Developers */
package org.python.core.io;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

import org.python.core.imp;
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

    /** true if the underlying file is actually closed on close() */
    private boolean closefd;

    /**
     * Construct a StreamIO for the given read channel.
     *
     * @param readChannel a ReadableByteChannel
     * @param closefd boolean whether the underlying file is closed on
     *                close() (defaults to True)
     */
    public StreamIO(ReadableByteChannel readChannel, boolean closefd) {
        this.readChannel = readChannel;
        this.closefd = closefd;
    }

    /**
     * Construct a StreamIO for the given read channel.
     *
     * @param readChannel a ReadableByteChannel
     */
    public StreamIO(ReadableByteChannel readChannel) {
        this(readChannel, true);
    }

    /**
     * Construct a StreamIO for the given write channel.
     *
     * @param writeChannel a WritableByteChannel
     * @param closefd boolean whether the underlying file is closed on
     *                close() (defaults to True)
     */
    public StreamIO(WritableByteChannel writeChannel, boolean closefd) {
        this.writeChannel = writeChannel;
        this.closefd = closefd;
    }

    /**
     * Construct a StreamIO for the given write channel.
     *
     * @param writeChannel a WritableByteChannel
     * @param isatty boolean whether this io object is a tty device
     * @param closefd boolean whether the underlying file is closed on
     *                close() (defaults to True)
     */
    public StreamIO(WritableByteChannel writeChannel) {
        this(writeChannel, true);
    }

    /**
     * Construct a StreamIO for the given read/write streams.
     *
     * @param inputStream an InputStream
     * @param closefd boolean whether the underlying file is closed on
     *                close() (defaults to True)
     */
    public StreamIO(InputStream inputStream, boolean closefd) {
        this(Channels.newChannel(inputStream), closefd);
        this.inputStream = inputStream;
    }

    /**
     * Construct a StreamIO for the given read/write streams.
     *
     * @param outputStream an OutputStream
     * @param closefd boolean whether the underlying file is closed on
     *                close() (defaults to True)
     */
    public StreamIO(OutputStream outputStream, boolean closefd) {
        this(Channels.newChannel(outputStream), closefd);
        this.outputStream = outputStream;
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

    /** Unwrap one or more nested FilterInputStreams. */
    private static FileDescriptor getInputFileDescriptor(InputStream stream) throws IOException {
	if (stream == null)
	    return null;
	if (stream instanceof FileInputStream)
	    return ((FileInputStream)stream).getFD();
	if (stream instanceof FilterInputStream) {
	    Field inField = null;
	    try {
		inField = FilterInputStream.class.getDeclaredField("in");
		inField.setAccessible(true);
		return getInputFileDescriptor((InputStream)inField.get(stream));
	    } catch (Exception e) {
	    } finally {
		if (inField != null && inField.isAccessible())
		    inField.setAccessible(false);
	    }
	}
	return null;
    }

    /** Unwrap one or more nested FilterOutputStreams. */
    private static FileDescriptor getOutputFileDescriptor(OutputStream stream) throws IOException {
	if (stream == null)
	    return null;
	if (stream instanceof FileOutputStream)
	    return ((FileOutputStream)stream).getFD();
	if (stream instanceof FilterOutputStream) {
	    Field outField = null;
	    try {
		outField = FilterOutputStream.class.getDeclaredField("out");
		outField.setAccessible(true);
		return getOutputFileDescriptor((OutputStream)outField.get(stream));
	    } catch (Exception e) {
	    } finally {
		if (outField != null && outField.isAccessible())
		    outField.setAccessible(false);
	    }
	}
	return null;
    }

    /** {@inheritDoc} */

    public boolean isatty() {
        checkClosed();

        FileDescriptor fd;
        try {
            if ( ((fd = getInputFileDescriptor(inputStream)) == null) &&
                 ((fd = getOutputFileDescriptor(outputStream)) == null))
                   return false;
        } catch (IOException e) {
            return false;
        }

        return imp.load("os").__getattr__("isatty").__call__(Py.java2py(fd)).__nonzero__();
    }

    /** {@inheritDoc} */
    public boolean readable() {
        return readChannel != null;
    }

    /** {@inheritDoc} */
    public boolean writable() {
        return writeChannel != null;
    }

    @Override
    public OutputStream asOutputStream() {
        if (writable()) {
            if (outputStream == null) {
                return Channels.newOutputStream(writeChannel);
            }
            return outputStream;
        }
        return super.asOutputStream();
    }

    @Override
    public InputStream asInputStream() {
        if (readable()) {
            if (inputStream == null) {
                return Channels.newInputStream(readChannel);
            }
            return inputStream;
        }
        return super.asInputStream();
    }

    /** {@inheritDoc} */
    public Channel getChannel() {
        return readable() ? readChannel : writeChannel;
    }
}
