/* Copyright (c) 2007 Jython Developers */
package org.python.core.util;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

import org.python.core.Py;

/**
 * String Utility methods.
 *
 */
public class StringUtil {

    /**
     * Encodes this String into a sequence of bytes. Each byte
     * contains the low-order bits of its corresponding char.
     *
     * @param string a String value
     * @return a byte array with one byte for each char in string
     */
    public static byte[] toBytes(String string) {
        try {
            return string.getBytes("ISO-8859-1");
        } catch(UnsupportedEncodingException uee) {
            // This JVM is whacked, it doesn't even have iso-8859-1
            throw Py.SystemError("Java couldn't find the ISO-8859-1 encoding");
        }
    }

    /**
     * Return a new String with chars corresponding to buf from off to
     * off + len.
     *
     * @param buf an array of bytes
     * @param off the initial offset
     * @param len the length
     * @return a new String corresponding to the bytes in buf
     */
    public static String fromBytes(byte[] buf, int off, int len) {
        // Yes, I known the method is deprecated, but it is the fastest
        // way of converting between between byte[] and String
        return new String(buf, 0, off, len);
    }

    /**
     * Return a new String with chars corresponding to buf.
     *
     * @param buf an array of bytes
     * @return a new String corresponding to the bytes in buf
     */
    public static String fromBytes(byte[] buf) {
        return fromBytes(buf, 0, buf.length);
    }

    /**
     * Return a new String with chars corresponding to buf.
     *
     * @param buf a ByteBuffer of bytes
     * @return a new String corresponding to the bytes in buf
     */
    public static String fromBytes(ByteBuffer buf) {
        return fromBytes(buf.array(), buf.arrayOffset() + buf.position(),
                         buf.arrayOffset() + buf.limit());
    }
}
