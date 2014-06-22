// Copyright (c) 2003 Jython project
package org.python.core.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.python.core.PyFile;

/**
 * Utility methods for Java file handling.
 */
public class FileUtil {
    /**
     * Creates a PyFile with mode that reads from the given <code>InputStream</code> using bufsize.
     */
    public static PyFile wrap(InputStream is, String mode, int bufsize) {
        return new PyFile(is, "<Java InputStream '" + is + "' as file>", mode, bufsize, true);
    }

    /**
     * Creates a PyFile with mode that reads from the <code>InputStream</code>.
     */
    public static PyFile wrap(InputStream is, String mode) {
        return wrap(is, mode, -1);
    }

    /**
     * Creates a PyFile in text mode that reads from the given <code>InputStream</code>
     * using bufsize.
     */
    public static PyFile wrap(InputStream is, int bufsize) {
        return wrap(is, "r", bufsize);
    }

    /**
     * Creates a PyFile in text mode that reads from the given <code>InputStream</code>.
     */
    public static PyFile wrap(InputStream is) {
        return wrap(is, -1);
    }

    /**
     * Creates a PyFile with mode that writes to the given <code>OutputStream</code> with the
     * given bufsize.
     */
    public static PyFile wrap(OutputStream os, String mode, int bufsize) {
        return new PyFile(os, mode, bufsize);
    }

    /**
     * Creates a PyFile with mode that writes to the given <code>OutputStream</code>
     */
    public static PyFile wrap(OutputStream os, String mode) {
        return wrap(os, mode, -1);
    }

    /**
     * Creates a PyFile in text mode that writes to the given <code>OutputStream</code>
     * with bufsize.
     */
    public static PyFile wrap(OutputStream os, int bufsize) {
        return wrap(os, "w", bufsize);
    }

    /**
     * Creates a PyFile in text mode that writes to the given <code>OutputStream</code>.
     */
    public static PyFile wrap(OutputStream os) {
        return wrap(os, -1);
    }

    /**
     * Read all bytes from the input stream. <p/> Note that using this method to
     * read very large streams could cause out-of-memory exceptions and/or block
     * for large periods of time.
     */
    public static byte[] readBytes(InputStream in) throws IOException {
        final int bufsize = 8192; // nice buffer size used in JDK
        byte[] buf = new byte[bufsize];
        ByteArrayOutputStream out = new ByteArrayOutputStream(bufsize);
        int count;
        while (true) {
            count = in.read(buf, 0, bufsize);
            if (count < 0) {
                break;
            }
            out.write(buf, 0, count);
        }
        return out.toByteArray();
    }
}
