/* Copyright (c) 2007 Jython Developers */
package org.python.core.io;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import org.python.core.Py;

/**
 * Raw I/O implementation for sockets.
 *
 * @author Philip Jenvey
 */
public class SocketIO extends RawIOBase {

    /** The underlying socket */
    private SocketChannel socketChannel;

    /** true if the file is readable */
    private boolean readable = false;

    /** true if the file is writable */
    private boolean writable = false;

    /**
     * Construct a SocketIO for the given SocketChannel.
     *
     * @param socketChannel a SocketChannel to wrap
     * @param mode a raw io file mode String
     */
    public SocketIO(SocketChannel socketChannel, String mode) {
        this.socketChannel = socketChannel;
        parseMode(mode);
    }

    /**
     * Parse the Python mode string.
     *
     * @param mode a python mode String
     */
    private void parseMode(String mode) {
        if (mode.equals("r")) {
            readable = true;
        } else if (mode.equals("w")) {
            writable = true;
        } else if (mode.equals("rw")) {
            readable = writable = true;
        } else {
            throw Py.ValueError("invalid mode: '" + mode + "'");
        }
    }

    /** {@inheritDoc} */
    public int readinto(ByteBuffer buf) {
        checkClosed();
        checkReadable();
        try {
            return socketChannel.read(buf);
        } catch (IOException ioe) {
            throw Py.IOError(ioe);
        }
    }

    /**
     * Read bytes into each of the specified ByteBuffers via scatter
     * i/o.
     *
     * @param bufs {@inheritDoc}
     * @return {@inheritDoc}
     */
    public long readinto(ByteBuffer[] bufs) {
        checkClosed();
        checkReadable();
        try {
            return socketChannel.read(bufs);
        } catch (IOException ioe) {
            throw Py.IOError(ioe);
        }
    }

    /** {@inheritDoc} */
    public int write(ByteBuffer buf) {
        checkClosed();
        checkWritable();
        try {
            return socketChannel.write(buf);
        } catch (IOException ioe) {
            throw Py.IOError(ioe);
        }
    }

    /**
     * Writes bytes from each of the specified ByteBuffers via gather
     * i/o.
     *
     * @param bufs {@inheritDoc}
     * @return {@inheritDoc}
     */
    public long write(ByteBuffer[] bufs) {
        checkClosed();
        checkWritable();
        try {
            return socketChannel.write(bufs);
        } catch (IOException ioe) {
            throw Py.IOError(ioe);
        }
    }

    /** {@inheritDoc} */
    public void close() {
        if (closed()) {
            return;
        }
        try {
            socketChannel.close();
        } catch (IOException ioe) {
            throw Py.IOError(ioe);
        }
        super.close();
    }

    /** {@inheritDoc} */
    public boolean readable() {
        return readable;
    }

    /** {@inheritDoc} */
    public boolean writable() {
        return writable;
    }
}
