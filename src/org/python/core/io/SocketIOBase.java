/* Copyright (c) 2007 Jython Developers */
package org.python.core.io;

import org.python.core.Py;

/**
 * Base raw I/O implementation for sockets.
 *
 * @author Philip Jenvey
 */
public abstract class SocketIOBase extends RawIOBase {

    /** true if the socket is allowed to be read from */
    private boolean readable = false;

    /** true if the socket is allowed to be written to */
    private boolean writable = false;

    /**
     * Construct a SocketIOBase.
     *
     * @param mode a raw io socket mode String
     */
    public SocketIOBase(String mode) {
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
    public boolean readable() {
        return readable;
    }

    /** {@inheritDoc} */
    public boolean writable() {
        return writable;
    }
}
