/* Copyright (c) Jython Developers */
package org.python.core.io;

import java.io.InputStream;
import java.io.IOException;

/**
 * An InputStream tie-in to a TextIOBase.
 */
public class TextIOInputStream extends InputStream {

    private TextIOBase textIO;

    /**
     * Creates an InputStream wrapper to a given TextIOBase.
     *
     * @param textIO a TextIOBase
     */
    public TextIOInputStream(TextIOBase textIO) {
        this.textIO = textIO;
    }

    @Override
    public int read() throws IOException {
        String result = textIO.read(1);
        if (result.length() == 0) {
            return -1;
        }
        return (int)result.charAt(0);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (b == null) {
            throw new NullPointerException();
        } else if ((off < 0) || (off > b.length) || (len < 0) || ((off + len) > b.length)
                   || ((off + len) < 0)) {
            throw new IndexOutOfBoundsException();
        } else if (len == 0) {
            return 0;
        }

        String result = textIO.read(len);
        len = result.length();
        for (int i = 0; i < len; i++) {
            b[off + i] = (byte)result.charAt(i);
        }
        return len == 0 ? -1 : len;
    }

    @Override
    public void close() throws IOException {
        textIO.close();
    }

    @Override
    public long skip(long n) throws IOException {
        return textIO.seek(n, 1);
    }
}
