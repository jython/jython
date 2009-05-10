// Copyright (c) 2003 Jython project
package org.python.core.util;

import java.io.ByteArrayOutputStream;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.python.core.imp;
import org.python.core.Py;
import org.python.core.PyException;
import org.python.core.PyFile;

/**
 * Utility methods for Java file handling.
 */
public class FileUtil {

    /**
     * Creates a PyFile that reads from the given <code>InputStream</code> with bufsize.
     */
    public static PyFile wrap(InputStream is, int bufsize) {
        return new PyFile(is, bufsize);
    }

    /**
     * Creates a PyFile that reads from the given <code>InputStream</code>.
     */
    public static PyFile wrap(InputStream is) {
        return wrap(is, -1);
    }

    /**
     * Creates a PyFile that writes to the given <code>OutputStream</code> with bufsize.
     */
    public static PyFile wrap(OutputStream os, int bufsize) {
        return new PyFile(os, bufsize);
    }

    /**
     * Creates a PyFile that writes to the given <code>OutputStream</code>.
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

    public static boolean isatty(FileDescriptor fd) {
        try {
            return imp.load("os").__getattr__("isatty").__call__(Py.java2py(fd)).__nonzero__();
        } catch (PyException e) {
            // Weak isatty check copied from jna-posix JavaPOSIX class
            return fd == FileDescriptor.in || fd == FileDescriptor.out || fd == FileDescriptor.err;
        }
    }

}
