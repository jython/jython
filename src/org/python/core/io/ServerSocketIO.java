/* Copyright (c) 2007 Jython Developers */
package org.python.core.io;

import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;

import com.kenai.constantine.platform.Errno;
import org.python.core.Py;

/**
 * Raw I/O implementation for server sockets.
 *
 * @author Philip Jenvey
 */
public class ServerSocketIO extends SocketIOBase<ServerSocketChannel> {

    /**
     * Construct a ServerSocketIO for the given ServerSocketChannel.
     *
     * @param socketChannel a ServerSocketChannel to wrap
     * @param mode a raw io socket mode String
     */
    public ServerSocketIO(ServerSocketChannel socketChannel, String mode) {
        super(socketChannel, mode);
    }

    @Override
    public int readinto(ByteBuffer buf) {
        checkClosed();
        checkReadable();
        throw Py.IOError(Errno.ENOTCONN);
    }

    @Override
    public int write(ByteBuffer buf) {
        checkClosed();
        checkWritable();
        throw Py.IOError(Errno.EBADF);
    }
}
