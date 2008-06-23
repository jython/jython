// Copyright (c) 2003 Jython project
package org.python.core.util;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;

/**
 * Utility methods for Java file handling.
 */
public class FileUtil {
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
