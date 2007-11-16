/* Copyright (c) 2007 Jython Developers */
package org.python.core.io;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;

/**
 * Line buffering for a writable sequential RawIO object.
 *
 * @author Philip Jenvey
 */
public class LineBufferedWriter extends BufferedWriter {

    /**
     * Construct a LineBufferedWriter wrapping the given RawIOBase.
     *
     * @param rawIO {@inheritDoc}
     */
    public LineBufferedWriter(RawIOBase rawIO) {
        super(rawIO, 0);
        buffer = ByteBuffer.allocate(DEFAULT_BUFFER_SIZE);
    }

    /** {@inheritDoc} */
    public int write(ByteBuffer bytes) {
        int written = 0;

        while (bytes.hasRemaining()) {
            byte next = bytes.get();

            try {
                buffer.put(next);
            } catch (BufferOverflowException boe) {
                // buffer is full; we *must* flush
                flush();
                buffer.put(next);
            }

            if (next == LF_BYTE) {
                written += buffer.position();
                flush();
            }
        }

        return written;
    }
}
