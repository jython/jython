/*
 * Copyright 2019 Jython Developers
 *
 * Original conversion from CPython source copyright 1998 Finn Bock.
 *
 * This program contains material copyrighted by: Copyright (c) 1991, 1992, 1993, 1994 by Stichting
 * Mathematisch Centrum, Amsterdam, The Netherlands.
 */

package org.python.modules;

import org.python.core.ArgParser;
import org.python.core.BufferProtocol;
import org.python.core.Py;
import org.python.core.PyBUF;
import org.python.core.PyBuffer;
import org.python.core.PyException;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.core.PyStringMap;
import org.python.core.PyTuple;
import org.python.core.PyUnicode;
import org.python.core.buffer.SimpleStringBuffer;

/**
 * The <tt>binascii.java</tt> module contains a number of methods to convert between binary and
 * various ASCII-encoded binary representations. Normally, you will not use these modules directly
 * but use wrapper modules like <tt>uu</tt> or <tt>hexbin</tt> instead, this module solely exists
 * because bit-manipulation of large amounts of data is slow in Python.
 *
 * <P>
 * The <tt>binascii.java</tt> module defines the following functions:
 *
 * <dl>
 * <dt><b><tt>a2b_uu</tt></b> (string)</dt>
 * <dd>Convert a single line of uuencoded data back to binary and return the binary data. Lines
 * normally contain 45 (binary) bytes, except for the last line. Line data may be followed by
 * whitespace.</dd>
 *
 * <dt><b><tt>b2a_uu</tt></b> (data)</dt>
 * <dd>Convert binary data to a line of ASCII characters, the return value is the converted line,
 * including a newline char. The length of <i>data</i> should be at most 45.</dd>
 *
 * <dt><b><tt>a2b_base64</tt></b> (string)</dt>
 * <dd>Convert a block of base64 data back to binary and return the binary data. More than one line
 * may be passed at a time.</dd>
 *
 * <dt><b><tt>b2a_base64</tt></b> (data)</dt>
 * <dd>Convert binary data to a line of ASCII characters in base64 coding. The return value is the
 * converted line, including a newline char. The length of <i>data</i> should be at most 57 to
 * adhere to the base64 standard.</dd>
 *
 * <dt><b><tt>a2b_hqx</tt></b> (string)</dt>
 * <dd>Convert binhex4 formatted ASCII data to binary, without doing RLE-decompression. The string
 * should contain a complete number of binary bytes, or (in case of the last portion of the binhex4
 * data) have the remaining bits zero.</dd>
 *
 * <dt><b><tt>rledecode_hqx</tt></b> (data)</dt>
 * <dd>Perform RLE-decompression on the data, as per the binhex4 standard. The algorithm uses
 * <tt>0x90</tt> after a byte as a repeat indicator, followed by a count. A count of <tt>0</tt>
 * specifies a byte value of <tt>0x90</tt>. The routine returns the decompressed data, unless data
 * input data ends in an orphaned repeat indicator, in which case the <tt>Incomplete</tt> exception
 * is raised.</dd>
 *
 * <dt><b><tt>rlecode_hqx</tt></b> (data)</dt>
 * <dd>Perform binhex4 style RLE-compression on <i>data</i> and return the result.</dd>
 *
 * <dt><b><tt>b2a_hqx</tt></b> (data)</dt>
 * <dd>Perform hexbin4 binary-to-ASCII translation and return the resulting string. The argument
 * should already be RLE-coded, and have a length divisible by 3 (except possibly the last
 * fragment).</dd>
 *
 * <dt><b><tt>crc_hqx</tt></b> (data, crc)</dt>
 * <dd>Compute the binhex4 crc value of <i>data</i>, starting with an initial <i>crc</i> and
 * returning the result.</dd>
 *
 * <dt><b><tt>Error</tt></b></dt>
 * <dd>Exception raised on errors. These are usually programming errors.</dd>
 *
 * <dt><b><tt>Incomplete</tt></b></dt>
 * <dd>Exception raised on incomplete data. These are usually not programming errors, but may be
 * handled by reading a little more data and trying again.</dd>
 * </dl>
 *
 * The module is a line-by-line conversion of the original binasciimodule.c written by Jack Jansen,
 * except that all mistakes and errors are my own.
 *
 * @author Finn Bock, bckfnn@pipmail.dknet.dk
 * @version binascii.java,v 1.6 1999/02/20 11:37:07 fb Exp
 *
 */
public class binascii {

    public static String __doc__ = "Conversion between binary data and ASCII";

    public static final PyObject Error = Py.makeClass("Error", Py.Exception, exceptionNamespace());

    public static final PyObject Incomplete =
            Py.makeClass("Incomplete", Py.Exception, exceptionNamespace());

    public static PyObject exceptionNamespace() {
        PyObject dict = new PyStringMap();
        dict.__setitem__("__module__", new PyString("binascii"));
        return dict;
    }

    // hqx lookup table, ascii->binary.
    private static char RUNCHAR = 0x90;

    private static byte DONE = 0x7F;
    private static byte SKIP = 0x7E;
    private static byte FAIL = 0x7D;

    //@formatter:off
    private static byte[] table_a2b_hqx = {
        /*       ^@    ^A    ^B    ^C    ^D    ^E    ^F    ^G   */
        /* 0*/  FAIL, FAIL, FAIL, FAIL, FAIL, FAIL, FAIL, FAIL,
        /*       \b    \t    \n    ^K    ^L    \r    ^N    ^O   */
        /* 1*/  FAIL, FAIL, SKIP, FAIL, FAIL, SKIP, FAIL, FAIL,
        /*       ^P    ^Q    ^R    ^S    ^T    ^U    ^V    ^W   */
        /* 2*/  FAIL, FAIL, FAIL, FAIL, FAIL, FAIL, FAIL, FAIL,
        /*       ^X    ^Y    ^Z    ^[    ^\    ^]    ^^    ^_   */
        /* 3*/  FAIL, FAIL, FAIL, FAIL, FAIL, FAIL, FAIL, FAIL,
        /*              !     "     #     $     %     &     '   */
        /* 4*/  FAIL, 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06,
        /*        (     )     *     +     ,     -     .     /   */
        /* 5*/  0x07, 0x08, 0x09, 0x0A, 0x0B, 0x0C, FAIL, FAIL,
        /*        0     1     2     3     4     5     6     7   */
        /* 6*/  0x0D, 0x0E, 0x0F, 0x10, 0x11, 0x12, 0x13, FAIL,
        /*        8     9     :     ;     <     =     >     ?   */
        /* 7*/  0x14, 0x15, DONE, FAIL, FAIL, FAIL, FAIL, FAIL,
        /*        @     A     B     C     D     E     F     G   */
        /* 8*/  0x16, 0x17, 0x18, 0x19, 0x1A, 0x1B, 0x1C, 0x1D,
        /*        H     I     J     K     L     M     N     O   */
        /* 9*/  0x1E, 0x1F, 0x20, 0x21, 0x22, 0x23, 0x24, FAIL,
        /*        P     Q     R     S     T     U     V     W   */
        /*10*/  0x25, 0x26, 0x27, 0x28, 0x29, 0x2A, 0x2B, FAIL,
        /*        X     Y     Z     [     \     ]     ^     _   */
        /*11*/  0x2C, 0x2D, 0x2E, 0x2F, FAIL, FAIL, FAIL, FAIL,
        /*        `     a     b     c     d     e     f     g   */
        /*12*/  0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, FAIL,
        /*        h     i     j     k     l     m     n     o   */
        /*13*/  0x37, 0x38, 0x39, 0x3A, 0x3B, 0x3C, FAIL, FAIL,
        /*        p     q     r     s     t     u     v     w   */
        /*14*/  0x3D, 0x3E, 0x3F, FAIL, FAIL, FAIL, FAIL, FAIL,
        /*        x     y     z     {     |     }     ~    ^?   */
        /*15*/  FAIL, FAIL, FAIL, FAIL, FAIL, FAIL, FAIL, FAIL,
        /*16*/  FAIL, FAIL, FAIL, FAIL, FAIL, FAIL, FAIL, FAIL,
                FAIL, FAIL, FAIL, FAIL, FAIL, FAIL, FAIL, FAIL,
                FAIL, FAIL, FAIL, FAIL, FAIL, FAIL, FAIL, FAIL,
                FAIL, FAIL, FAIL, FAIL, FAIL, FAIL, FAIL, FAIL,
                FAIL, FAIL, FAIL, FAIL, FAIL, FAIL, FAIL, FAIL,
                FAIL, FAIL, FAIL, FAIL, FAIL, FAIL, FAIL, FAIL,
                FAIL, FAIL, FAIL, FAIL, FAIL, FAIL, FAIL, FAIL,
                FAIL, FAIL, FAIL, FAIL, FAIL, FAIL, FAIL, FAIL,
                FAIL, FAIL, FAIL, FAIL, FAIL, FAIL, FAIL, FAIL,
                FAIL, FAIL, FAIL, FAIL, FAIL, FAIL, FAIL, FAIL,
                FAIL, FAIL, FAIL, FAIL, FAIL, FAIL, FAIL, FAIL,
                FAIL, FAIL, FAIL, FAIL, FAIL, FAIL, FAIL, FAIL,
                FAIL, FAIL, FAIL, FAIL, FAIL, FAIL, FAIL, FAIL,
                FAIL, FAIL, FAIL, FAIL, FAIL, FAIL, FAIL, FAIL,
                FAIL, FAIL, FAIL, FAIL, FAIL, FAIL, FAIL, FAIL,
                FAIL, FAIL, FAIL, FAIL, FAIL, FAIL, FAIL, FAIL,
    };
    //@formatter:on

    private static char[] table_b2a_hqx =
            "!\"#$%&'()*+,-012345689@ABCDEFGHIJKLMNPQRSTUVXYZ[`abcdefhijklmpqr".toCharArray();

    //@formatter:off
    private static byte table_a2b_base64[] = {
        -1,-1,-1,-1, -1,-1,-1,-1, -1,-1,-1,-1, -1,-1,-1,-1,
        -1,-1,-1,-1, -1,-1,-1,-1, -1,-1,-1,-1, -1,-1,-1,-1,
        -1,-1,-1,-1, -1,-1,-1,-1, -1,-1,-1,62, -1,-1,-1,63,
        52,53,54,55, 56,57,58,59, 60,61,-1,-1, -1, 0,-1,-1, /* Note PAD->0 */
        -1, 0, 1, 2,  3, 4, 5, 6,  7, 8, 9,10, 11,12,13,14,
        15,16,17,18, 19,20,21,22, 23,24,25,-1, -1,-1,-1,-1,
        -1,26,27,28, 29,30,31,32, 33,34,35,36, 37,38,39,40,
        41,42,43,44, 45,46,47,48, 49,50,51,-1, -1,-1,-1,-1
    };
    //@formatter:on

    private static char BASE64_PAD = '=';

    /* Max binary chunk size */
    private static int BASE64_MAXBIN = Integer.MAX_VALUE / 2 - 3;

    private static char[] table_b2a_base64 =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".toCharArray();

    //@formatter:off
    private static int[] crctab_hqx = {
        0x0000, 0x1021, 0x2042, 0x3063, 0x4084, 0x50a5, 0x60c6, 0x70e7,
        0x8108, 0x9129, 0xa14a, 0xb16b, 0xc18c, 0xd1ad, 0xe1ce, 0xf1ef,
        0x1231, 0x0210, 0x3273, 0x2252, 0x52b5, 0x4294, 0x72f7, 0x62d6,
        0x9339, 0x8318, 0xb37b, 0xa35a, 0xd3bd, 0xc39c, 0xf3ff, 0xe3de,
        0x2462, 0x3443, 0x0420, 0x1401, 0x64e6, 0x74c7, 0x44a4, 0x5485,
        0xa56a, 0xb54b, 0x8528, 0x9509, 0xe5ee, 0xf5cf, 0xc5ac, 0xd58d,
        0x3653, 0x2672, 0x1611, 0x0630, 0x76d7, 0x66f6, 0x5695, 0x46b4,
        0xb75b, 0xa77a, 0x9719, 0x8738, 0xf7df, 0xe7fe, 0xd79d, 0xc7bc,
        0x48c4, 0x58e5, 0x6886, 0x78a7, 0x0840, 0x1861, 0x2802, 0x3823,
        0xc9cc, 0xd9ed, 0xe98e, 0xf9af, 0x8948, 0x9969, 0xa90a, 0xb92b,
        0x5af5, 0x4ad4, 0x7ab7, 0x6a96, 0x1a71, 0x0a50, 0x3a33, 0x2a12,
        0xdbfd, 0xcbdc, 0xfbbf, 0xeb9e, 0x9b79, 0x8b58, 0xbb3b, 0xab1a,
        0x6ca6, 0x7c87, 0x4ce4, 0x5cc5, 0x2c22, 0x3c03, 0x0c60, 0x1c41,
        0xedae, 0xfd8f, 0xcdec, 0xddcd, 0xad2a, 0xbd0b, 0x8d68, 0x9d49,
        0x7e97, 0x6eb6, 0x5ed5, 0x4ef4, 0x3e13, 0x2e32, 0x1e51, 0x0e70,
        0xff9f, 0xefbe, 0xdfdd, 0xcffc, 0xbf1b, 0xaf3a, 0x9f59, 0x8f78,
        0x9188, 0x81a9, 0xb1ca, 0xa1eb, 0xd10c, 0xc12d, 0xf14e, 0xe16f,
        0x1080, 0x00a1, 0x30c2, 0x20e3, 0x5004, 0x4025, 0x7046, 0x6067,
        0x83b9, 0x9398, 0xa3fb, 0xb3da, 0xc33d, 0xd31c, 0xe37f, 0xf35e,
        0x02b1, 0x1290, 0x22f3, 0x32d2, 0x4235, 0x5214, 0x6277, 0x7256,
        0xb5ea, 0xa5cb, 0x95a8, 0x8589, 0xf56e, 0xe54f, 0xd52c, 0xc50d,
        0x34e2, 0x24c3, 0x14a0, 0x0481, 0x7466, 0x6447, 0x5424, 0x4405,
        0xa7db, 0xb7fa, 0x8799, 0x97b8, 0xe75f, 0xf77e, 0xc71d, 0xd73c,
        0x26d3, 0x36f2, 0x0691, 0x16b0, 0x6657, 0x7676, 0x4615, 0x5634,
        0xd94c, 0xc96d, 0xf90e, 0xe92f, 0x99c8, 0x89e9, 0xb98a, 0xa9ab,
        0x5844, 0x4865, 0x7806, 0x6827, 0x18c0, 0x08e1, 0x3882, 0x28a3,
        0xcb7d, 0xdb5c, 0xeb3f, 0xfb1e, 0x8bf9, 0x9bd8, 0xabbb, 0xbb9a,
        0x4a75, 0x5a54, 0x6a37, 0x7a16, 0x0af1, 0x1ad0, 0x2ab3, 0x3a92,
        0xfd2e, 0xed0f, 0xdd6c, 0xcd4d, 0xbdaa, 0xad8b, 0x9de8, 0x8dc9,
        0x7c26, 0x6c07, 0x5c64, 0x4c45, 0x3ca2, 0x2c83, 0x1ce0, 0x0cc1,
        0xef1f, 0xff3e, 0xcf5d, 0xdf7c, 0xaf9b, 0xbfba, 0x8fd9, 0x9ff8,
        0x6e17, 0x7e36, 0x4e55, 0x5e74, 0x2e93, 0x3eb2, 0x0ed1, 0x1ef0,
    };
    //@formatter:on

    public static PyString __doc__a2b_uu =
            new PyString("(ascii) -> bin. Decode a line of uuencoded data");

    /**
     * Convert a single line of uuencoded data back to binary and return the binary data. Lines
     * normally contain 45 (binary) bytes, except for the last line. Line data may be followed by
     * whitespace.
     */
    public static PyString a2b_uu(PyObject text) {

        try (PyBuffer textBuf = getByteBuffer(text)) {
            int textLen = textBuf.getLen();
            if (textLen == 0) {
                return new PyString("");
            }

            StringBuilder dataBuf = new StringBuilder();

            int bits = 0;       // store bits not yet emitted (max 12 bits)
            int bitCount = 0;   // how many (valid) bits waiting
            int index = 0;

            int dataExpected = (textBuf.intAt(0) - ' ') & 077;
            textLen -= 1;

            for (; dataExpected > 0 && textLen > 0; index++, textLen--) {
                int ch = textBuf.intAt(index + 1);
                int sixBits;

                if (ch == '\n' || ch == '\r' || textLen <= 0) {
                    // Whitespace. Assume some spaces got eaten at end-of-line.
                    // (We check this later.)
                    sixBits = 0;
                } else {
                    /*
                     * Check the character for legality The 64 instead of the expected 63 is because
                     * there are a few uuencodes out there that use '@' as zero instead of space.
                     */
                    if (ch < ' ' || ch > (' ' + 64)) {
                        throw new PyException(Error, "Illegal char");
                    }
                    sixBits = (ch - ' ') & 0x3f;
                }

                // Shift it in on the low end, and see if there's a byte ready for output.
                bits = (bits << 6) | sixBits;
                bitCount += 6;
                if (bitCount >= 8) {
                    bitCount -= 8;
                    int b = (bits >> bitCount) & 0xff;
                    dataBuf.append((char) b); // byte
                    bits &= (1 << bitCount) - 1;
                    dataExpected--;
                }
            }

            // Finally, check that anything left on the line is white space.
            while (textLen-- > 0) {
                int ch = textBuf.intAt(++index);
                // Extra '@' may be written as padding in some cases
                if (ch != ' ' && ch != '@' && ch != '\n' && ch != '\r') {
                    throw new PyException(Error, "Trailing garbage");
                }
            }

            // finally, if we haven't decoded enough stuff, fill it up with zeros
            for (; index < dataExpected; index++) {
                dataBuf.append((char) 0);
            }

            return new PyString(dataBuf.toString());

        } catch (ClassCastException e) {
            throw argMustBeBytes("a2b_uu", text);
        }
    }

    public static PyString __doc__b2a_uu = new PyString("(bin) -> ascii. Uuencode line of data");

    /**
     * Convert binary data to a line of ASCII characters, the return value is the converted line,
     * including a newline char. The length of <i>data</i> should be at most 45.
     */
    public static PyString b2a_uu(PyObject data) {

        try (PyBuffer dataBuf = getByteBuffer(data)) {

            int dataLen = dataBuf.getLen();
            if (dataLen > 45) {
                // The 45 is a limit that appears in all uuencode's
                throw new PyException(Error, "At most 45 bytes at once");
            }

            // Each 3 bytes (rounded up) produce 4 characters, plus a 1 byte length and '\n'
            StringBuilder textBuf = new StringBuilder(4 * ((dataLen + 2) / 3) + 2);
            int bitCount = 0;
            int bits = 0;

            // Store the length
            textBuf.append((char) (' ' + (dataLen & 077)));

            for (int i = 0; dataLen > 0 || bitCount != 0; i++, dataLen--) {
                // Shift the data (or padding) into our buffer
                if (dataLen > 0) {
                    bits = (bits << 8) | dataBuf.intAt(i);
                } else {
                    bits <<= 8;
                }
                bitCount += 8;

                // See if there are 6-bit groups ready
                while (bitCount >= 6) {
                    bitCount -= 6;
                    int sixBits = (bits >> bitCount) & 0x3f;
                    textBuf.append((char) (sixBits + ' '));
                }
            }

            textBuf.append('\n'); // Append a courtesy newline
            return new PyString(textBuf.toString());

        } catch (ClassCastException e) {
            throw argMustBeBytes("b2a_uu", data);
        }
    }

    /** Finds & returns the (num+1)th valid character for base64, or -1 if none. */
    private static int binascii_find_valid(PyBuffer b, int offset, int num) {
        int blen = b.getLen() - offset;
        int ret = -1;

        while ((blen > 0) && (ret == -1)) {
            int c = b.intAt(offset);
            byte b64val = table_a2b_base64[c & 0x7f];
            if (((c <= 0x7f) && (b64val != -1))) {
                if (num == 0) {
                    ret = c;
                }
                num--;
            }
            offset++;
            blen--;
        }
        return ret;
    }

    public static PyString __doc__a2b_base64 =
            new PyString("(ascii) -> bin. Decode a line of base64 data");

    /**
     * Convert a block of base64 data back to binary and return the binary data. More than one line
     * may be passed at a time.
     */
    public static PyString a2b_base64(PyObject text) {

        try (PyBuffer textBuf = getByteBuffer(text)) {
            int textLen = textBuf.getLen();

            // Every 4 characters (rounded up) map to 3 bytes. (Or fewer, if there are extras.)
            int dataLen = 3 * ((textLen + 3) / 4);
            // These characters will represent bytes, in the usual Jython 2 way.
            StringBuilder dataBuf = new StringBuilder(dataLen);
            int bits = 0;       // store bits not yet emitted (max 12 bits)
            int bitCount = 0;   // how many (valid) bits waiting
            int quad_pos = 0;

            for (int i = 0; textLen > 0; textLen--, i++) {
                // Skip some punctuation
                int ch = textBuf.intAt(i);
                if (ch > 0x7F || ch == '\r' || ch == '\n' || ch == ' ') {
                    continue;

                } else

                if (ch == BASE64_PAD) {
                    if (quad_pos < 2 || (quad_pos == 2
                            && binascii_find_valid(textBuf, i, 1) != BASE64_PAD)) {
                        continue;
                    } else {
                        // A pad sequence means no more input. We've already interpreted the data
                        // from the quad at this point.
                        bitCount = 0;
                        break;
                    }
                } else {

                    int sixBits = table_a2b_base64[ch];
                    if (sixBits == -1) {
                        continue;
                    }

                    // Shift it in on the low end, and see if there's a byte ready for output.
                    quad_pos = (quad_pos + 1) & 0x03;
                    bits = (bits << 6) | sixBits;
                    bitCount += 6;
                    if (bitCount >= 8) {
                        bitCount -= 8;
                        dataBuf.append((char) ((bits >> bitCount) & 0xff)); // byte
                        // Erase the bits we emitted
                        bits &= (1 << bitCount) - 1;
                    }
                }
            }
            // Check that no bits are left
            if (bitCount != 0) {
                throw new PyException(Error, "Incorrect padding");
            }

            return new PyString(dataBuf.toString());

        } catch (ClassCastException e) {
            throw argMustBeBytes("a2b_base64", text);
        }
    }

    public static PyString __doc__b2a_base64 =
            new PyString("(bin) -> ascii. Base64-code line of data");

    /**
     * Convert binary data to a line of ASCII characters in base64 coding. The return value is the
     * converted line, including a newline char.
     */
    public static PyString b2a_base64(PyObject data) {

        try (PyBuffer dataBuf = getByteBuffer(data)) {
            int dataLen = dataBuf.getLen();
            if (dataLen > BASE64_MAXBIN) {
                throw new PyException(Error, "Too much data for base64 line");
            }
            // Every 3 bytes (rounded up) maps to 4 characters (and there's a newline)
            StringBuilder ascii_data = new StringBuilder(4 * ((dataLen + 2) / 3) + 1);
            int bits = 0;       // store bits not yet emitted (max 14 bits)
            int bitCount = 0;   // how many (valid) bits waiting

            for (int i = 0; i < dataLen; i++) {
                // Shift the data into our buffer
                bits = (bits << 8) | dataBuf.intAt(i);
                bitCount += 8;

                // While there are 6-bit groups available, emit them as characters.
                while (bitCount >= 6) {
                    bitCount -= 6;
                    ascii_data.append(table_b2a_base64[(bits >> bitCount) & 0x3f]);
                }
            }

            // Emit the balance of bits and append a newline
            if (bitCount == 2) {
                ascii_data.append(table_b2a_base64[(bits & 3) << 4]);
                ascii_data.append(BASE64_PAD);
                ascii_data.append(BASE64_PAD);
            } else if (bitCount == 4) {
                ascii_data.append(table_b2a_base64[(bits & 0xf) << 2]);
                ascii_data.append(BASE64_PAD);
            }
            ascii_data.append('\n');  // Append a courtesy newline

            return new PyString(ascii_data.toString());

        } catch (ClassCastException e) {
            throw argMustBeBytes("b2a_base64", data);
        }
    }

    public static PyString __doc__a2b_hqx = new PyString("ascii -> bin, done. Decode .hqx coding");

    /**
     * Convert binhex4 formatted ASCII data to binary, without doing RLE-decompression. The string
     * should contain a complete number of binary bytes, or (in case of the last portion of the
     * binhex4 data) have the remaining bits zero.
     */
    public static PyTuple a2b_hqx(PyObject text) {

        try (PyBuffer textBuf = getByteBuffer(text)) {

            int textLen = textBuf.getLen();
            StringBuilder dataBuf = new StringBuilder();
            int bitCount = 0;
            int bits = 0;
            boolean done = false;

            for (int i = 0; i < textLen; i++) {
                // Get the byte and look it up
                byte b = table_a2b_hqx[textBuf.intAt(i)];

                if (b == SKIP) {
                    continue;

                } else if (b == FAIL) {
                    throw new PyException(Error, "Illegal char");

                } else if (b == DONE) {
                    // The terminating colon
                    done = true;
                    break;

                } else {
                    // Shift it into the buffer and see if any bytes are ready
                    bits = (bits << 6) | b;
                    bitCount += 6;
                    if (bitCount >= 8) {
                        bitCount -= 8;
                        dataBuf.append((char) ((bits >> bitCount) & 0xff)); // byte
                        bits &= (1 << bitCount) - 1;
                    }
                }
            }

            if (bitCount != 0 && !done) {
                throw new PyException(Incomplete, "String has incomplete number of bytes");
            }

            return new PyTuple(new PyString(dataBuf.toString()), Py.newInteger(done ? 1 : 0));

        } catch (ClassCastException e) {
            throw argMustBeBytes("a2b_hqx", text);
        }
    }

    public static PyString __doc__rlecode_hqx = new PyString("Binhex RLE-code binary data");

    /** Perform binhex4 style RLE-compression on <i>data</i> and return the result. */
    static public PyString rlecode_hqx(PyObject data) {

        try (PyBuffer inBuf = getByteBuffer(data)) {
            int len = inBuf.getLen();
            StringBuilder outBuf = new StringBuilder();

            for (int in = 0; in < len; in++) {
                char ch = (char) inBuf.intAt(in);

                if (ch == RUNCHAR) {
                    // RUNCHAR. Escape it.
                    outBuf.append(RUNCHAR);
                    outBuf.append((char) 0);

                } else {
                    // Check how many following are the same
                    int inend;
                    for (inend = in + 1; inend < len && ((char) inBuf.intAt(inend)) == ch
                            && inend < in + 255; inend++) { /* nothing */ }
                    if (inend - in > 3) {
                        // More than 3 in a row. Output RLE.
                        outBuf.append(ch);
                        outBuf.append(RUNCHAR);
                        outBuf.append((char) (inend - in));
                        in = inend - 1;
                    } else {
                        // Less than 3. Output the byte itself
                        outBuf.append(ch);
                    }
                }
            }

            return new PyString(outBuf.toString());

        } catch (ClassCastException e) {
            throw argMustBeBytes("rlecode_hqx", data);
        }
    }

    public static PyString __doc__b2a_hqx = new PyString("Encode .hqx data");

    /**
     * Perform hexbin4 binary-to-ASCII translation and return the resulting string. The argument
     * should already be RLE-coded, and have a length divisible by 3 (except possibly the last
     * fragment).
     */
    public static PyString b2a_hqx(PyObject data) {

        try (PyBuffer dataBuf = getByteBuffer(data)) {
            int len = dataBuf.getLen();

            StringBuilder textBuf = new StringBuilder();
            int bits = 0;
            int bitCount = 0;

            for (int i = 0; len > 0; len--, i++) {
                // Shift into our buffer, and output any 6bits ready
                bits = (bits << 8) | (char) dataBuf.intAt(i);
                bitCount += 8;
                while (bitCount >= 6) {
                    bitCount -= 6;
                    textBuf.append(table_b2a_hqx[(bits >> bitCount) & 0x3f]);
                }
            }

            // Output a possible runt byte
            if (bitCount != 0) {
                bits <<= (6 - bitCount);
                textBuf.append(table_b2a_hqx[bits & 0x3f]);
            }

            return new PyString(textBuf.toString());

        } catch (ClassCastException e) {
            throw argMustBeBytes("b2a_hqx", data);
        }
    }

    public static PyString __doc__rledecode_hqx = new PyString("Decode hexbin RLE-coded string");

    /**
     * Perform RLE-decompression on the data, as per the binhex4 standard. The algorithm uses
     * <tt>0x90</tt> after a byte as a repeat indicator, followed by a count. A count of <tt>0</tt>
     * specifies a byte value of <tt>0x90</tt>. The routine returns the decompressed data, unless
     * data input data ends in an orphaned repeat indicator, in which case the <tt>Incomplete</tt>
     * exception is raised.
     */
    static public PyString rledecode_hqx(PyObject data) {

        try (PyBuffer inBuf = getByteBuffer(data)) {
            int inLen = inBuf.getLen();
            int index = 0;

            // Empty string is a special case
            if (inLen == 0) {
                return Py.EmptyString;
            }

            // Pretty much throughout, we use a char to store a byte :(
            StringBuilder outBuf = new StringBuilder();

            // Handle first byte separately (since we have to get angry
            // in case of an orphaned RLE code).
            if (--inLen < 0) {
                throw new PyException(Incomplete);
            }
            char outByte = (char) inBuf.intAt(index++);

            if (outByte == RUNCHAR) {
                if (--inLen < 0) {
                    throw new PyException(Incomplete);
                }
                int in_repeat = inBuf.intAt(index++);

                if (in_repeat != 0) {
                    // Note Error, not Incomplete (which is at the end
                    // of the string only). This is a programmer error.
                    throw new PyException(Error, "Orphaned RLE code at start");
                }
                outBuf.append(RUNCHAR);
            } else {
                outBuf.append(outByte);
            }

            while (inLen > 0) {
                if (--inLen < 0) {
                    throw new PyException(Incomplete);
                }
                outByte = (char) inBuf.intAt(index++);

                if (outByte == RUNCHAR) {
                    if (--inLen < 0) {
                        throw new PyException(Incomplete);
                    }
                    int in_repeat = inBuf.intAt(index++);

                    if (in_repeat == 0) {
                        // Just an escaped RUNCHAR value
                        outBuf.append(RUNCHAR);
                    } else {
                        // Pick up value and output a sequence of it
                        outByte = outBuf.charAt(outBuf.length() - 1);
                        while (--in_repeat > 0) {
                            outBuf.append(outByte);
                        }
                    }
                } else {
                    // Normal byte
                    outBuf.append(outByte);
                }
            }

            return new PyString(outBuf.toString());

        } catch (ClassCastException e) {
            throw argMustBeBytes("rledecode_hqx", data);
        }
    }

    public static PyString __doc__crc_hqx =
            new PyString("(data, oldcrc) -> newcrc. Compute hqx CRC incrementally");

    /**
     * Compute the binhex4 crc value of <i>data</i>, starting with an initial <i>crc</i> and
     * returning the result.
     */
    public static int crc_hqx(PyObject data, int crc) {
        try (PyBuffer buf = getByteBuffer(data)) {
            int len = buf.getLen();
            for (int i = 0; i < len; i++) {
                crc = ((crc << 8) & 0xff00) ^ crctab_hqx[((crc >> 8) & 0xff) ^ buf.intAt(i)];
            }
            return crc;
        } catch (ClassCastException e) {
            throw argMustBeBytes("crc_hqx", data);
        }
    }

    //@formatter:off
    static int[] crc_32_tab = new int[] {
    0x00000000, 0x77073096, 0xee0e612c, 0x990951ba, 0x076dc419,
    0x706af48f, 0xe963a535, 0x9e6495a3, 0x0edb8832, 0x79dcb8a4,
    0xe0d5e91e, 0x97d2d988, 0x09b64c2b, 0x7eb17cbd, 0xe7b82d07,
    0x90bf1d91, 0x1db71064, 0x6ab020f2, 0xf3b97148, 0x84be41de,
    0x1adad47d, 0x6ddde4eb, 0xf4d4b551, 0x83d385c7, 0x136c9856,
    0x646ba8c0, 0xfd62f97a, 0x8a65c9ec, 0x14015c4f, 0x63066cd9,
    0xfa0f3d63, 0x8d080df5, 0x3b6e20c8, 0x4c69105e, 0xd56041e4,
    0xa2677172, 0x3c03e4d1, 0x4b04d447, 0xd20d85fd, 0xa50ab56b,
    0x35b5a8fa, 0x42b2986c, 0xdbbbc9d6, 0xacbcf940, 0x32d86ce3,
    0x45df5c75, 0xdcd60dcf, 0xabd13d59, 0x26d930ac, 0x51de003a,
    0xc8d75180, 0xbfd06116, 0x21b4f4b5, 0x56b3c423, 0xcfba9599,
    0xb8bda50f, 0x2802b89e, 0x5f058808, 0xc60cd9b2, 0xb10be924,
    0x2f6f7c87, 0x58684c11, 0xc1611dab, 0xb6662d3d, 0x76dc4190,
    0x01db7106, 0x98d220bc, 0xefd5102a, 0x71b18589, 0x06b6b51f,
    0x9fbfe4a5, 0xe8b8d433, 0x7807c9a2, 0x0f00f934, 0x9609a88e,
    0xe10e9818, 0x7f6a0dbb, 0x086d3d2d, 0x91646c97, 0xe6635c01,
    0x6b6b51f4, 0x1c6c6162, 0x856530d8, 0xf262004e, 0x6c0695ed,
    0x1b01a57b, 0x8208f4c1, 0xf50fc457, 0x65b0d9c6, 0x12b7e950,
    0x8bbeb8ea, 0xfcb9887c, 0x62dd1ddf, 0x15da2d49, 0x8cd37cf3,
    0xfbd44c65, 0x4db26158, 0x3ab551ce, 0xa3bc0074, 0xd4bb30e2,
    0x4adfa541, 0x3dd895d7, 0xa4d1c46d, 0xd3d6f4fb, 0x4369e96a,
    0x346ed9fc, 0xad678846, 0xda60b8d0, 0x44042d73, 0x33031de5,
    0xaa0a4c5f, 0xdd0d7cc9, 0x5005713c, 0x270241aa, 0xbe0b1010,
    0xc90c2086, 0x5768b525, 0x206f85b3, 0xb966d409, 0xce61e49f,
    0x5edef90e, 0x29d9c998, 0xb0d09822, 0xc7d7a8b4, 0x59b33d17,
    0x2eb40d81, 0xb7bd5c3b, 0xc0ba6cad, 0xedb88320, 0x9abfb3b6,
    0x03b6e20c, 0x74b1d29a, 0xead54739, 0x9dd277af, 0x04db2615,
    0x73dc1683, 0xe3630b12, 0x94643b84, 0x0d6d6a3e, 0x7a6a5aa8,
    0xe40ecf0b, 0x9309ff9d, 0x0a00ae27, 0x7d079eb1, 0xf00f9344,
    0x8708a3d2, 0x1e01f268, 0x6906c2fe, 0xf762575d, 0x806567cb,
    0x196c3671, 0x6e6b06e7, 0xfed41b76, 0x89d32be0, 0x10da7a5a,
    0x67dd4acc, 0xf9b9df6f, 0x8ebeeff9, 0x17b7be43, 0x60b08ed5,
    0xd6d6a3e8, 0xa1d1937e, 0x38d8c2c4, 0x4fdff252, 0xd1bb67f1,
    0xa6bc5767, 0x3fb506dd, 0x48b2364b, 0xd80d2bda, 0xaf0a1b4c,
    0x36034af6, 0x41047a60, 0xdf60efc3, 0xa867df55, 0x316e8eef,
    0x4669be79, 0xcb61b38c, 0xbc66831a, 0x256fd2a0, 0x5268e236,
    0xcc0c7795, 0xbb0b4703, 0x220216b9, 0x5505262f, 0xc5ba3bbe,
    0xb2bd0b28, 0x2bb45a92, 0x5cb36a04, 0xc2d7ffa7, 0xb5d0cf31,
    0x2cd99e8b, 0x5bdeae1d, 0x9b64c2b0, 0xec63f226, 0x756aa39c,
    0x026d930a, 0x9c0906a9, 0xeb0e363f, 0x72076785, 0x05005713,
    0x95bf4a82, 0xe2b87a14, 0x7bb12bae, 0x0cb61b38, 0x92d28e9b,
    0xe5d5be0d, 0x7cdcefb7, 0x0bdbdf21, 0x86d3d2d4, 0xf1d4e242,
    0x68ddb3f8, 0x1fda836e, 0x81be16cd, 0xf6b9265b, 0x6fb077e1,
    0x18b74777, 0x88085ae6, 0xff0f6a70, 0x66063bca, 0x11010b5c,
    0x8f659eff, 0xf862ae69, 0x616bffd3, 0x166ccf45, 0xa00ae278,
    0xd70dd2ee, 0x4e048354, 0x3903b3c2, 0xa7672661, 0xd06016f7,
    0x4969474d, 0x3e6e77db, 0xaed16a4a, 0xd9d65adc, 0x40df0b66,
    0x37d83bf0, 0xa9bcae53, 0xdebb9ec5, 0x47b2cf7f, 0x30b5ffe9,
    0xbdbdf21c, 0xcabac28a, 0x53b39330, 0x24b4a3a6, 0xbad03605,
    0xcdd70693, 0x54de5729, 0x23d967bf, 0xb3667a2e, 0xc4614ab8,
    0x5d681b02, 0x2a6f2b94, 0xb40bbe37, 0xc30c8ea1, 0x5a05df1b,
    0x2d02ef8d
    };
    //@formatter:on

    public static int crc32(PyObject bp) {
        return crc32(bp, 0);
    }

    public static int crc32(PyObject data, long long_crc) {

        int crc = ~(int) long_crc;

        try (PyBuffer dataBuf = getByteBuffer(data)) {
            int len = dataBuf.getLen();
            for (int i = 0; i < len; i++) {
                int b = dataBuf.intAt(i);
                crc = crc_32_tab[(crc ^ b) & 0xff] ^ (crc >>> 8);
                /* Note: (crc >> 8) MUST zero fill on left */
            }
            return ~crc;

        } catch (ClassCastException e) {
            throw argMustBeBytes("crc32", data);
        }

    }

    private static char[] hexdigit = "0123456789abcdef".toCharArray();

    public static PyString __doc__b2a_hex =
            new PyString("b2a_hex(data) -> s; Hexadecimal representation of binary data.\n" + "\n"
                    + "This function is also available as \"hexlify()\".");

    public static PyString b2a_hex(PyObject data) {

        try (PyBuffer dataBuf = getByteBuffer(data)) {

            int dataLen = dataBuf.getLen();
            StringBuilder retbuf = new StringBuilder(dataLen * 2);

            // make hex version of string, taken from shamodule.c
            for (int i = 0; i < dataLen; i++) {
                int ch = dataBuf.intAt(i);
                retbuf.append(hexdigit[(ch >>> 4) & 0xF]);
                retbuf.append(hexdigit[ch & 0xF]);
            }

            return new PyString(retbuf.toString());

        } catch (ClassCastException e) {
            throw argMustBeBytes("b2a_hex", data);
        }
    }

    public static PyString hexlify(PyObject argbuf) {
        return b2a_hex(argbuf);
    }

    public static PyString a2b_hex$doc =
            new PyString("a2b_hex(hexstr) -> s; Binary data of hexadecimal representation.\n" + "\n"
                    + "hexstr must contain an even number of hex digits "
                    + "(upper or lower case).\n"
                    + "This function is also available as \"unhexlify()\"");

    public static PyString a2b_hex(PyObject hexstr) {

        try (PyBuffer buf = getByteBuffer(hexstr)) {

            int bufLen = buf.getLen();
            StringBuilder retbuf = new StringBuilder(bufLen / 2);
            /*
             * XXX What should we do about strings with an odd length? Should we add an implicit
             * leading zero, or a trailing zero? For now, raise an exception.
             */
            if (bufLen % 2 != 0) {
                throw Py.TypeError("Odd-length string");
            }

            for (int i = 0; i < bufLen; i += 2) {
                int top = Character.digit(buf.intAt(i), 16);
                int bot = Character.digit(buf.intAt(i + 1), 16);
                if (top == -1 || bot == -1) {
                    throw Py.TypeError("Non-hexadecimal digit found");
                }
                retbuf.append((char) ((top << 4) + bot));
            }

            return new PyString(retbuf.toString());

        } catch (ClassCastException e) {
            throw argMustBeBytes("a2b_hex", hexstr);
        }
    }

    public static PyString unhexlify(PyObject argbuf) {
        return a2b_hex(argbuf);
    }

    final private static char[] upper_hexdigit = "0123456789ABCDEF".toCharArray();

    private static StringBuilder qpEscape(StringBuilder sb, char c) {
        sb.append('=');
        sb.append(upper_hexdigit[(c >>> 4) & 0xF]);
        sb.append(upper_hexdigit[c & 0xF]);
        return sb;
    }

    final public static PyString __doc__a2b_qp = new PyString("Decode a string of qp-encoded data");

    private static boolean getIntFlagAsBool(ArgParser ap, int index, int dflt, String errMsg) {
        try {
            boolean val = ap.getInt(index, dflt) != 0;
            return val;
        } catch (PyException e) {
            if (e.match(Py.AttributeError) || e.match(Py.ValueError)) {
                throw Py.TypeError(errMsg);
            }
            throw e;
        }
    }

    public static PyString a2b_qp(PyObject[] arg, String[] kws) {
        ArgParser ap = new ArgParser("a2b_qp", arg, kws, new String[] {"s", "header"});

        PyObject bp = ap.getPyObject(0);

        StringBuilder sb = new StringBuilder();
        boolean header = getIntFlagAsBool(ap, 1, 0, "an integer is required");

        try (PyBuffer ascii_data = getByteBuffer((PyObject) bp)) {
            for (int i = 0, m = ascii_data.getLen(); i < m;) {
                char c = (char) ascii_data.intAt(i++);
                if (header && c == '_') {
                    sb.append(' ');
                } else if (c == '=') {
                    if (i < m) {
                        c = (char) ascii_data.intAt(i++);
                        if (c == '=') {
                            sb.append(c);
                        } else if (c == ' ') {
                            sb.append("= ");
                        } else if ((c >= '0' && c <= '9' || c >= 'A' && c <= 'F') && i < m) {
                            char nc = (char) ascii_data.intAt(i++);
                            if ((nc >= '0' && nc <= '9' || nc >= 'A' && nc <= 'F')) {
                                sb.append((char) (Character.digit(c, 16) << 4
                                        | Character.digit(nc, 16)));
                            } else {
                                sb.append('=').append(c).append(nc);
                            }
                        } else if (c != '\n') {
                            sb.append('=').append(c);
                        }
                    }
                } else {
                    sb.append(c);
                }
            }
            return new PyString(sb.toString());
        } catch (ClassCastException e) {
            throw argMustBeBytes("a2b_qp", bp);
        }
    }

    final public static PyString __doc__b2a_qp =
            new PyString("b2a_qp(data, quotetabs=0, istext=1, header=0) -> s;\n"
                    + "Encode a string using quoted-printable encoding.\n\n"
                    + "On encoding, when istext is set, newlines are not encoded, and white\n"
                    + "space at end of lines is.  When istext is not set, \r and \n (CR/LF) are\n"
                    + "both encoded.  When quotetabs is set, space and tabs are encoded.");

    public static PyString b2a_qp(PyObject[] arg, String[] kws) {
        ArgParser ap = new ArgParser("b2a_qp", arg, kws,
                new String[] {"s", "quotetabs", "istext", "header"});
        boolean quotetabs = getIntFlagAsBool(ap, 1, 0, "an integer is required");
        boolean istext = getIntFlagAsBool(ap, 2, 1, "an integer is required");
        boolean header = getIntFlagAsBool(ap, 3, 0, "an integer is required");

        PyObject data = ap.getPyObject(0);

        try (PyBuffer dataBuf = getByteBuffer(data)) {

            int dataLen = dataBuf.getLen();
            StringBuilder sb = new StringBuilder(dataLen);
            String lineEnd = "\n";

            // Work out if line endings should be crlf.
            for (int i = 0, m = dataBuf.getLen(); i < m; i++) {
                if ('\n' == dataBuf.intAt(i)) {
                    if (i > 0 && '\r' == dataBuf.intAt(i - 1)) {
                        lineEnd = "\r\n";
                    }
                    break;
                }
            }

            int count = 0;
            int MAXLINESIZE = 76;

            int in = 0;
            while (in < dataLen) {
                char ch = (char) dataBuf.intAt(in);
                if ((ch > 126) || (ch == '=') || (header && ch == '_')
                        || ((ch == '.') && (count == 0)
                                && ((in + 1 == dataLen) || (char) dataBuf.intAt(in + 1) == '\n'
                                        || (char) dataBuf.intAt(in + 1) == '\r'))
                        || (!istext && ((ch == '\r') || (ch == '\n')))
                        || ((ch == '\t' || ch == ' ') && (in + 1 == dataLen))
                        || ((ch < 33) && (ch != '\r') && (ch != '\n')
                                && (quotetabs || (!quotetabs && ((ch != '\t') && (ch != ' ')))))) {
                    if ((count + 3) >= MAXLINESIZE) {
                        sb.append('=');
                        sb.append(lineEnd);
                        count = 0;
                    }
                    qpEscape(sb, ch);
                    in++;
                    count += 3;
                } else {
                    if (istext && ((ch == '\n') || ((in + 1 < dataLen) && (ch == '\r')
                            && (dataBuf.intAt(in + 1) == '\n')))) {
                        count = 0;
                        // Protect against whitespace on end of line
                        int out = sb.length();
                        if (out > 0
                                && ((sb.charAt(out - 1) == ' ') || (sb.charAt(out - 1) == '\t'))) {
                            ch = sb.charAt(out - 1);
                            sb.setLength(out - 1);
                            qpEscape(sb, ch);
                        }

                        sb.append(lineEnd);
                        if (ch == '\r') {
                            in += 2;
                        } else {
                            in++;
                        }
                    } else {
                        if ((in + 1 != dataLen) && ((char) dataBuf.intAt(in + 1) != '\n')
                                && (count + 1) >= MAXLINESIZE) {
                            sb.append('=');
                            sb.append(lineEnd);
                            count = 0;
                        }
                        count++;
                        if (header && ch == ' ') {
                            sb.append('_');
                            in++;
                        } else {
                            sb.append(ch);
                            in++;
                        }
                    }
                }
            }

            return new PyString(sb.toString());

        } catch (ClassCastException e) {
            throw argMustBeBytes("b2a_qp", data);
        }

    }

    /**
     * We use this when the argument given to a conversion method is to be interpreted as text. If
     * it is byte-like, the bytes are used unchanged, assumed in the "intended" character set. It
     * may be a {@code PyUnicode}, in which case the it will be decoded to bytes using the default
     * encoding ({@code sys.getdefaultencoding()}.
     *
     * @param text an object with the buffer protocol (or {@code unicode})
     * @return a byte-buffer view of argument (or default decoding if {@code unicode})
     * @throws ClassCastException where the text object does not implement the buffer protocol
     */
    private static PyBuffer getByteBuffer(PyObject text) throws ClassCastException {
        if (text instanceof PyUnicode) {
            String s = ((PyUnicode) text).encode();
            return new SimpleStringBuffer(PyBUF.SIMPLE, null, s);
        } else {
            return ((BufferProtocol) text).getBuffer(PyBUF.SIMPLE);
        }
    }

    /**
     * Convenience method providing the exception when an argument is not the expected type, in the
     * format "<b>f</b>() argument 1 must bytes or unicode, not <code>type(arg)</code>."
     *
     * @param f name of function of error (or could be any text)
     * @param arg argument provided from which actual type will be reported
     * @return TypeError to throw
     */
    private static PyException argMustBeBytes(String f, PyObject arg) {
        String fmt = "%s() argument 1 must bytes or unicode, not %s";
        String type = "null";
        if (arg instanceof PyObject) {
            type = ((PyObject) arg).getType().fastGetName();
        } else if (arg != null) {
            type = arg.getClass().getName();
        }
        return Py.TypeError(String.format(fmt, f, type));
    }

}
