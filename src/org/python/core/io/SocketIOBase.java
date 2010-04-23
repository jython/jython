/* Copyright (c) 2007 Jython Developers */
package org.python.core.io;

import java.io.IOException;
import java.nio.channels.Channel;

import org.python.core.Py;

/**
 * Base raw I/O implementation for sockets.
 *
 * @author Philip Jenvey
 */
public abstract class SocketIOBase<T extends Channel> extends RawIOBase {

    /** The underlying socket */
    protected T socketChannel;

    /** true if the socket is allowed to be read from */
    private boolean readable = false;

    /** true if the socket is allowed to be written to */
    private boolean writable = false;

    /**
     * Construct a SocketIOBase for the given socket Channel
     *
     * @param socketChannel a Channel to wrap
     * @param mode a raw io socket mode String
     */
    public SocketIOBase(T socketChannel, String mode) {
        this.socketChannel = socketChannel;
        parseMode(mode);
    }

    /**
     * Parse the raw io socket mode string.
     *
     * The mode can be 'r', 'w' or 'rw' for reading, writing or
     * reading and writing.
     *
     * @param mode a raw io socket mode String
     */
    protected void parseMode(String mode) {
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

    @Override
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

    @Override
    public T getChannel() {
        return socketChannel;
    }

    @Override
    public boolean readable() {
        return readable;
    }

    @Override
    public boolean writable() {
        return writable;
    }
}
