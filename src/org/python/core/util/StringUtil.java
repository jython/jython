/* Copyright (c) 2007 Jython Developers */
package org.python.core.util;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Set;

import org.python.core.Py;
import org.python.util.Generic;

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
    @SuppressWarnings("deprecation")
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

    /**
     * Decapitalize a String if it begins with a capital letter, e.g.:
     * FooBar -> fooBar
     *
     * @param string a String
     * @return a decapitalized String
     */
    public static String decapitalize(String string) {
        char c0 = string.charAt(0);
        if (!Character.isUpperCase(c0)) {
            return string;
        }
        if (string.length() > 1 && Character.isUpperCase(string.charAt(1))) {
            return string;
        }
        char[] chars = string.toCharArray();
        chars[0] = Character.toLowerCase(c0);
        return new String(chars);
    }

    /**
     * Returns true if each segment of <code>name</code> produced by splitting it on '.' is a valid
     * Java identifier.
     */
    public static boolean isJavaClassName(String name) {
        for (String segment : name.split("\\.")) {
            if (!isJavaIdentifier(segment)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns true if ident is a valid Java identifier as defined by
     * http://java.sun.com/docs/books/jls/second_edition/html/lexical.doc.html#40625
     */
    public static boolean isJavaIdentifier(String ident) {
        if (ident.length() == 0 || JAVA_LITERALS.contains(ident)) {
            return false;
        }
        int cp = ident.codePointAt(0);
        if (!Character.isJavaIdentifierStart(cp)) {
            return false;
        }
        for (int i = Character.charCount(cp); i < ident.length(); i += Character.charCount(cp)) {
            cp = ident.codePointAt(i);
            if (!Character.isJavaIdentifierPart(cp)) {
                return false;
            }
        }
        return true;
    }

    // True false and null are just literals, the rest are keywords
    private static final Set<String> JAVA_LITERALS = Generic.set("abstract", "continue", "for",
        "new", "switch", "assert", "default", "goto", "package", "synchronized", "boolean", "do",
        "if", "private", "this", "break", "double", "implements", "protected", "throw", "byte",
        "else", "import", "public", "throws", "case", "enum", "instanceof", "return", "transient",
        "catch", "extends", "int", "short", "try", "char", "final", "interface", "static", "void",
        "class", "finally", "long", "strictfp", "volatile", "const", "float", "native", "super",
        "while", "true", "false", "null");
}
