/* Copyright (c) 2007 Jython Developers */
package org.python.core.io;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

import org.python.core.Py;

/**
 * Raw I/O implementation for datagram sockets.
 *
 * @author Philip Jenvey
 */
public class DatagramSocketIO extends SocketIOBase<DatagramChannel> {

    /**
     * Construct a DatagramSocketIO for the given DatagramChannel.
     *
     * @param socketChannel a DatagramChannel to wrap
     * @param mode a raw io socket mode String
     */
    public DatagramSocketIO(DatagramChannel socketChannel, String mode) {
        super(socketChannel, mode);
    }

    @Override
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
    @Override
    public long readinto(ByteBuffer[] bufs) {
        checkClosed();
        checkReadable();
        try {
            return socketChannel.read(bufs);
        } catch (IOException ioe) {
            throw Py.IOError(ioe);
        }
    }

    @Override
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
    @Override
    public long write(ByteBuffer[] bufs) {
        checkClosed();
        checkWritable();
        try {
            return socketChannel.write(bufs);
        } catch (IOException ioe) {
            throw Py.IOError(ioe);
        }
    }
}
