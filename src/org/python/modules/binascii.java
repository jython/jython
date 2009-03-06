/*
 * Copyright 1998 Finn Bock.
 *
 * This program contains material copyrighted by:
 * Copyright (c) 1991, 1992, 1993, 1994 by Stichting Mathematisch Centrum,
 * Amsterdam, The Netherlands.
 */

package org.python.modules;


import java.util.regex.Pattern;

import org.python.core.ArgParser;
import org.python.core.Py;
import org.python.core.PyException;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.core.PyStringMap;
import org.python.core.PyTuple;
import org.python.core.util.StringUtil;

/**
 * The <tt>binascii.java</tt> module contains a number of methods to convert
 * between binary and various ASCII-encoded binary
 * representations. Normally, you will not use these modules directly but
 * use wrapper modules like <tt>uu</tt><a name="l2h-"></a> or
 * <tt>hexbin</tt><a name="l2h-"></a> instead, this module solely
 * exists because bit-manipuation of large amounts of data is slow in
 * Python.
 *
 * <P>
 * The <tt>binascii.java</tt> module defines the following functions:
 * <P>
 * <dl><dt><b><a name="l2h-19960"><tt>a2b_uu</tt></a></b> (<var>string</var>)
 * <dd>
 * Convert a single line of uuencoded data back to binary and return the
 * binary data. Lines normally contain 45 (binary) bytes, except for the
 * last line. Line data may be followed by whitespace.
 * </dl>
 *
 * <P>
 * <dl><dt><b><tt>b2a_uu</tt></b> (<var>data</var>)
 * <dd>
 * Convert binary data to a line of ASCII characters, the return value
 * is the converted line, including a newline char. The length of
 * <i>data</i> should be at most 45.
 * </dl>
 *
 * <P>
 * <dl><dt><b><tt>a2b_base64</tt></b> (<var>string</var>)
 * <dd>
 * Convert a block of base64 data back to binary and return the
 * binary data. More than one line may be passed at a time.
 * </dl>
 *
 * <P>
 * <dl><dt><b><tt>b2a_base64</tt></b> (<var>data</var>)
 * <dd>
 * Convert binary data to a line of ASCII characters in base64 coding.
 * The return value is the converted line, including a newline char.
 * The length of <i>data</i> should be at most 57 to adhere to the base64
 * standard.
 * </dl>
 *
 * <P>
 * <dl><dt><b><tt>a2b_hqx</tt></b> (<var>string</var>)
 * <dd>
 * Convert binhex4 formatted ASCII data to binary, without doing
 * RLE-decompression. The string should contain a complete number of
 * binary bytes, or (in case of the last portion of the binhex4 data)
 * have the remaining bits zero.
 * </dl>
 *
 * <P>
 * <dl><dt><b><tt>rledecode_hqx</tt></b> (<var>data</var>)
 * <dd>
 * Perform RLE-decompression on the data, as per the binhex4
 * standard. The algorithm uses <tt>0x90</tt> after a byte as a repeat
 * indicator, followed by a count. A count of <tt>0</tt> specifies a byte
 * value of <tt>0x90</tt>. The routine returns the decompressed data,
 * unless data input data ends in an orphaned repeat indicator, in which
 * case the <tt>Incomplete</tt> exception is raised.
 * </dl>
 *
 * <P>
 * <dl><dt><b><tt>rlecode_hqx</tt></b> (<var>data</var>)
 * <dd>
 * Perform binhex4 style RLE-compression on <i>data</i> and return the
 * result.
 * </dl>
 *
 * <P>
 * <dl><dt><b><tt>b2a_hqx</tt></b> (<var>data</var>)
 * <dd>
 * Perform hexbin4 binary-to-ASCII translation and return the
 * resulting string. The argument should already be RLE-coded, and have a
 * length divisible by 3 (except possibly the last fragment).
 * </dl>
 *
 * <P>
 * <dl><dt><b><tt>crc_hqx</tt></b> (<var>data, crc</var>)
 * <dd>
 * Compute the binhex4 crc value of <i>data</i>, starting with an initial
 * <i>crc</i> and returning the result.
 * </dl>
 *
 * <dl><dt><b><tt>Error</tt></b>
 * <dd>
 * Exception raised on errors. These are usually programming errors.
 * </dl>
 *
 * <P>
 * <dl><dt><b><tt>Incomplete</tt></b>
 * <dd>
 * Exception raised on incomplete data. These are usually not programming
 * errors, but may be handled by reading a little more data and trying
 * again.
 * </dl>
 *
 * The module is a line-by-line conversion of the original binasciimodule.c
 * written by Jack Jansen, except that all mistakes and errors are my own.
 * <p>
 * @author Finn Bock, bckfnn@pipmail.dknet.dk
 * @version binascii.java,v 1.6 1999/02/20 11:37:07 fb Exp

 */
public class binascii {

    public static String __doc__ = "Conversion between binary data and ASCII";

    public static final PyObject Error = Py.makeClass("Error", Py.Exception, exceptionNamespace());

    public static final PyObject Incomplete = Py.makeClass("Incomplete", Py.Exception,
                                                           exceptionNamespace());

    public static PyObject exceptionNamespace() {
        PyObject dict = new PyStringMap();
        dict.__setitem__("__module__", new PyString("binascii"));
        return dict;
    }

    // hqx lookup table, ascii->binary.
    private static char RUNCHAR = 0x90;

    private static short DONE = 0x7F;
    private static short SKIP = 0x7E;
    private static short FAIL = 0x7D;

    private static short[] table_a2b_hqx = {
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

    private static byte[] table_b2a_hqx =
        StringUtil.toBytes("!\"#$%&'()*+,-012345689@ABCDEFGHIJKLMNPQRSTUVXYZ[`abcdefhijklmpqr");




    private static short table_a2b_base64[] = {
        -1,-1,-1,-1, -1,-1,-1,-1, -1,-1,-1,-1, -1,-1,-1,-1,
        -1,-1,-1,-1, -1,-1,-1,-1, -1,-1,-1,-1, -1,-1,-1,-1,
        -1,-1,-1,-1, -1,-1,-1,-1, -1,-1,-1,62, -1,-1,-1,63,
        52,53,54,55, 56,57,58,59, 60,61,-1,-1, -1, 0,-1,-1, /* Note PAD->0 */
        -1, 0, 1, 2,  3, 4, 5, 6,  7, 8, 9,10, 11,12,13,14,
        15,16,17,18, 19,20,21,22, 23,24,25,-1, -1,-1,-1,-1,
        -1,26,27,28, 29,30,31,32, 33,34,35,36, 37,38,39,40,
        41,42,43,44, 45,46,47,48, 49,50,51,-1, -1,-1,-1,-1
    };

    private static char BASE64_PAD = '=';

    /* Max binary chunk size */
    private static int BASE64_MAXBIN = Integer.MAX_VALUE / 2 - 3;

    private static byte[] table_b2a_base64 =
        StringUtil.toBytes("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/");



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



    public static PyString __doc__a2b_uu = new PyString(
        "(ascii) -> bin. Decode a line of uuencoded data"
    );


    /**
     * Convert a single line of uuencoded data back to binary and return the
     * binary data. Lines normally contain 45 (binary) bytes, except for the
     * last line. Line data may be followed by whitespace.
     */
    public static PyString a2b_uu(String ascii_data) {
        int leftbits = 0;
        int leftchar = 0;

        if (ascii_data.length() == 0)
            return new PyString("");
        
        StringBuilder bin_data = new StringBuilder();

        char this_ch;
        int i;

        int ascii_len = ascii_data.length()-1;

        int bin_len = (ascii_data.charAt(0) - ' ') & 077;

        for (i = 0; bin_len > 0 && ascii_len > 0; i++, ascii_len--) {
            this_ch = ascii_data.charAt(i+1);
            if (this_ch == '\n' || this_ch == '\r' || ascii_len <= 0) {
                // Whitespace. Assume some spaces got eaten at
                // end-of-line. (We check this later)
                this_ch = 0;
            } else {
                // Check the character for legality
                // The 64 in stead of the expected 63 is because
                // there are a few uuencodes out there that use
                // '@' as zero instead of space.
                if ( this_ch < ' ' || this_ch > (' ' + 64)) {
                    throw new PyException(Error, "Illegal char");
                }
                this_ch = (char)((this_ch - ' ') & 077);
            }
            // Shift it in on the low end, and see if there's
            // a byte ready for output.
            leftchar = (leftchar << 6) | (this_ch);
            leftbits += 6;
            if (leftbits >= 8) {
                leftbits -= 8;
                bin_data.append((char)((leftchar >> leftbits) & 0xff));
                leftchar &= ((1 << leftbits) - 1);
                bin_len--;
            }
        }
        
        // Finally, check that if there's anything left on the line
        // that it's whitespace only.
        while (ascii_len-- > 0) {
            this_ch = ascii_data.charAt(++i);
            // Extra '@' may be written as padding in some cases
            if (this_ch != ' ' && this_ch != '@' &&
                     this_ch != '\n' && this_ch != '\r') {
                throw new PyException(Error, "Trailing garbage");
            }
        }
        
        // finally, if we haven't decoded enough stuff, fill it up with zeros
        for (; i<bin_len; i++)
        	bin_data.append((char)0);
        
        return new PyString(bin_data.toString());
    }



    public static PyString __doc__b2a_uu = new PyString(
        "(bin) -> ascii. Uuencode line of data"
    );


    /**
     * Convert binary data to a line of ASCII characters, the return value
     * is the converted line, including a newline char. The length of
     * <i>data</i> should be at most 45.
     */
    public static PyString b2a_uu(String bin_data) {
        int leftbits = 0;
        char this_ch;
        int leftchar = 0;

        int bin_len = bin_data.length();
        if (bin_len > 45) {
            // The 45 is a limit that appears in all uuencode's
            throw new PyException(Error, "At most 45 bytes at once");
        }

        StringBuilder ascii_data = new StringBuilder();

        // Store the length */
        ascii_data.append((char)(' ' + (bin_len & 077)));

        for (int i = 0; bin_len > 0 || leftbits != 0; i++, bin_len--) {
            // Shift the data (or padding) into our buffer
            if (bin_len > 0)    // Data
                leftchar = (leftchar << 8) | bin_data.charAt(i);
            else  // Padding
                leftchar <<= 8;
            leftbits += 8;

            // See if there are 6-bit groups ready
            while (leftbits >= 6) {
                this_ch = (char)((leftchar >> (leftbits-6)) & 0x3f);
                leftbits -= 6;
                ascii_data.append((char)(this_ch + ' '));
            }
        }
        ascii_data.append('\n'); // Append a courtesy newline

        return new PyString(ascii_data.toString());
    }


    private static int binascii_find_valid(String s, int offset, int num) {
        int slen = s.length() - offset;

        /* Finds & returns the (num+1)th
        ** valid character for base64, or -1 if none.
        */

        int ret = -1;

        while ((slen > 0) && (ret == -1)) {
            int c = s.charAt(offset);
            short b64val = table_a2b_base64[c & 0x7f];
            if (((c <= 0x7f) && (b64val != -1)) ) {
                if (num == 0)
                     ret = c;
                num--;
            }

            offset++;
            slen--;
        }
        return ret;
    }



    public static PyString __doc__a2b_base64 = new PyString(
         "(ascii) -> bin. Decode a line of base64 data"
    );

    /**
     * Convert a block of base64 data back to binary and return the
     * binary data. More than one line may be passed at a time.
     */
    public static PyString a2b_base64(String ascii_data) {
        int leftbits = 0;
        char this_ch;
        int leftchar = 0;
        int quad_pos = 0;

        int ascii_len = ascii_data.length();

        int bin_len = 0;
        StringBuilder bin_data = new StringBuilder();

        for(int i = 0; ascii_len > 0 ; ascii_len--, i++) {
            // Skip some punctuation
            this_ch = ascii_data.charAt(i);
            if (this_ch > 0x7F || this_ch == '\r' ||
                      this_ch == '\n' || this_ch == ' ')
                continue;

            if (this_ch == BASE64_PAD) {
                if (quad_pos < 2 || (quad_pos == 2 &&
                         binascii_find_valid(ascii_data, i, 1) != BASE64_PAD))
                    continue;
                else {
                    // A pad sequence means no more input.
                    // We've already interpreted the data
                    // from the quad at this point.
                    leftbits = 0;
                    break;
                }
            }

            short this_v = table_a2b_base64[this_ch];
            if (this_v == -1)
                continue;

            // Shift it in on the low end, and see if there's
            // a byte ready for output.
            quad_pos = (quad_pos + 1) & 0x03;
            leftchar = (leftchar << 6) | (this_v);
            leftbits += 6;
            if (leftbits >= 8) {
                leftbits -= 8;
                bin_data.append((char)((leftchar >> leftbits) & 0xff));
                bin_len++;
                leftchar &= ((1 << leftbits) - 1);
            }
        }
        // Check that no bits are left
        if (leftbits != 0) {
            throw new PyException(Error, "Incorrect padding");
        }
        return new PyString(bin_data.toString());
    }



    public static PyString __doc__b2a_base64 = new PyString(
        "(bin) -> ascii. Base64-code line of data"
    );


    /**
     * Convert binary data to a line of ASCII characters in base64 coding.
     * The return value is the converted line, including a newline char.
     */
    public static PyString b2a_base64(String bin_data) {
        int leftbits = 0;
        char this_ch;
        int leftchar = 0;

        StringBuilder ascii_data = new StringBuilder();

        int bin_len = bin_data.length();
        if (bin_len > BASE64_MAXBIN) {
            throw new PyException(Error,"Too much data for base64 line");
        }

        for (int i = 0; bin_len > 0 ; bin_len--, i++) {
            // Shift the data into our buffer
            leftchar = (leftchar << 8) | bin_data.charAt(i);
            leftbits += 8;

            // See if there are 6-bit groups ready
            while (leftbits >= 6) {
                this_ch = (char)((leftchar >> (leftbits-6)) & 0x3f);
                leftbits -= 6;
                ascii_data.append((char)table_b2a_base64[this_ch]);
            }
        }
        if (leftbits == 2) {
            ascii_data.append((char)table_b2a_base64[(leftchar&3) << 4]);
            ascii_data.append(BASE64_PAD);
            ascii_data.append(BASE64_PAD);
        } else if (leftbits == 4) {
            ascii_data.append((char)table_b2a_base64[(leftchar&0xf) << 2]);
            ascii_data.append(BASE64_PAD);
        }
        ascii_data.append('\n');  // Append a courtesy newline

        return new PyString(ascii_data.toString());
    }


    public static PyString __doc__a2b_hqx = new PyString(
         "ascii -> bin, done. Decode .hqx coding"
    );

    /**
     * Convert binhex4 formatted ASCII data to binary, without doing
     * RLE-decompression. The string should contain a complete number of
     * binary bytes, or (in case of the last portion of the binhex4 data)
     * have the remaining bits zero.
     */
    public static PyTuple a2b_hqx(String ascii_data) {
        int leftbits = 0;
        char this_ch;
        int leftchar = 0;

        boolean done = false;

        int len = ascii_data.length();

        StringBuilder bin_data = new StringBuilder();

        for(int i = 0; len > 0 ; len--, i++) {
            // Get the byte and look it up
            this_ch = (char) table_a2b_hqx[ascii_data.charAt(i)];
            if (this_ch == SKIP)
                continue;
            if (this_ch == FAIL) {
                throw new PyException(Error, "Illegal char");
            }
            if (this_ch == DONE) {
                // The terminating colon
                done = true;
                break;
            }

            // Shift it into the buffer and see if any bytes are ready
            leftchar = (leftchar << 6) | (this_ch);
            leftbits += 6;
            if (leftbits >= 8) {
                leftbits -= 8;
                bin_data.append((char)((leftchar >> leftbits) & 0xff));
                leftchar &= ((1 << leftbits) - 1);
            }
        }

        if (leftbits != 0 && !done) {
            throw new PyException(Incomplete,
                                  "String has incomplete number of bytes");
        }

        return new PyTuple(Py.java2py(bin_data.toString()), Py.newInteger(done ? 1 : 0));
    }


    public static PyString __doc__rlecode_hqx = new PyString(
         "Binhex RLE-code binary data"
    );

    /**
     * Perform binhex4 style RLE-compression on <i>data</i> and return the
     * result.
     */
    static public String rlecode_hqx(String in_data) {
        int len = in_data.length();

        StringBuilder out_data = new StringBuilder();

        for (int in=0; in < len; in++) {
            char ch = in_data.charAt(in);
            if (ch == RUNCHAR) {
                // RUNCHAR. Escape it.
                out_data.append(RUNCHAR);
                out_data.append(0);
            } else {
                // Check how many following are the same
                int inend;
                for (inend=in+1; inend < len &&
                                 in_data.charAt(inend) == ch &&
                                 inend < in+255; inend++)
                    ;
                if (inend - in > 3) {
                    // More than 3 in a row. Output RLE.
                    out_data.append(ch);
                    out_data.append(RUNCHAR);
                    out_data.append((char) (inend-in));
                    in = inend-1;
                } else {
                    // Less than 3. Output the byte itself
                    out_data.append(ch);
                }
            }
        }
        return out_data.toString();
    }


    public static PyString __doc__b2a_hqx = new PyString(
         "Encode .hqx data"
    );

    /**
     * Perform hexbin4 binary-to-ASCII translation and return the
     * resulting string. The argument should already be RLE-coded, and have a
     * length divisible by 3 (except possibly the last fragment).
     */
    public static PyString b2a_hqx(String bin_data) {
        int leftbits = 0;
        char this_ch;
        int leftchar = 0;

        int len = bin_data.length();

        StringBuilder ascii_data = new StringBuilder();

        for(int i = 0; len > 0; len--, i++) {
            // Shift into our buffer, and output any 6bits ready
            leftchar = (leftchar << 8) | bin_data.charAt(i);
            leftbits += 8;
            while (leftbits >= 6) {
                this_ch = (char) ((leftchar >> (leftbits-6)) & 0x3f);
                leftbits -= 6;
                ascii_data.append((char) table_b2a_hqx[this_ch]);
            }
        }
        // Output a possible runt byte
        if (leftbits != 0) {
            leftchar <<= (6-leftbits);
            ascii_data.append((char) table_b2a_hqx[leftchar & 0x3f]);
        }
        return new PyString(ascii_data.toString());
    }



    public static PyString __doc__rledecode_hqx = new PyString(
        "Decode hexbin RLE-coded string"
    );


    /**
     * Perform RLE-decompression on the data, as per the binhex4
     * standard. The algorithm uses <tt>0x90</tt> after a byte as a repeat
     * indicator, followed by a count. A count of <tt>0</tt> specifies a byte
     * value of <tt>0x90</tt>. The routine returns the decompressed data,
     * unless data input data ends in an orphaned repeat indicator, in which
     * case the <tt>Incomplete</tt> exception is raised.
     */
    static public String rledecode_hqx(String in_data) {
        char in_byte, in_repeat;

        int in_len = in_data.length();
        int i = 0;

        // Empty string is a special case
        if (in_len == 0)
            return "";

        StringBuilder out_data = new StringBuilder();

        // Handle first byte separately (since we have to get angry
        // in case of an orphaned RLE code).
        if (--in_len < 0) throw new PyException(Incomplete);
        in_byte = in_data.charAt(i++);

        if (in_byte == RUNCHAR) {
            if (--in_len < 0) throw new PyException(Incomplete);
            in_repeat = in_data.charAt(i++);

            if (in_repeat != 0) {
                // Note Error, not Incomplete (which is at the end
                // of the string only). This is a programmer error.
                throw new PyException(Error, "Orphaned RLE code at start");
            }
            out_data.append(RUNCHAR);
        } else {
            out_data.append(in_byte);
        }

        while (in_len > 0) {
            if (--in_len < 0) throw new PyException(Incomplete);
            in_byte = in_data.charAt(i++);

            if (in_byte == RUNCHAR) {
                if (--in_len < 0) throw new PyException(Incomplete);
                in_repeat = in_data.charAt(i++);

                if (in_repeat == 0) {
                    // Just an escaped RUNCHAR value
                    out_data.append(RUNCHAR);
                } else {
                    // Pick up value and output a sequence of it
                    in_byte = out_data.charAt(out_data.length()-1);
                    while (--in_repeat > 0)
                        out_data.append(in_byte);
                }
            } else {
                // Normal byte
                out_data.append(in_byte);
            }
        }
        return out_data.toString();
    }



    public static PyString __doc__crc_hqx = new PyString(
        "(data, oldcrc) -> newcrc. Compute hqx CRC incrementally"
    );


    /**
     * Compute the binhex4 crc value of <i>data</i>, starting with an initial
     * <i>crc</i> and returning the result.
     */
    public static int crc_hqx(String bin_data, int crc) {
        int len = bin_data.length();
        int i = 0;

        while(len-- > 0) {
            crc=((crc<<8)&0xff00) ^
                       crctab_hqx[((crc>>8)&0xff)^bin_data.charAt(i++)];
        }

        return crc;
    }




static long[] crc_32_tab = new long[] {
0x00000000L, 0x77073096L, 0xee0e612cL, 0x990951baL, 0x076dc419L,
0x706af48fL, 0xe963a535L, 0x9e6495a3L, 0x0edb8832L, 0x79dcb8a4L,
0xe0d5e91eL, 0x97d2d988L, 0x09b64c2bL, 0x7eb17cbdL, 0xe7b82d07L,
0x90bf1d91L, 0x1db71064L, 0x6ab020f2L, 0xf3b97148L, 0x84be41deL,
0x1adad47dL, 0x6ddde4ebL, 0xf4d4b551L, 0x83d385c7L, 0x136c9856L,
0x646ba8c0L, 0xfd62f97aL, 0x8a65c9ecL, 0x14015c4fL, 0x63066cd9L,
0xfa0f3d63L, 0x8d080df5L, 0x3b6e20c8L, 0x4c69105eL, 0xd56041e4L,
0xa2677172L, 0x3c03e4d1L, 0x4b04d447L, 0xd20d85fdL, 0xa50ab56bL,
0x35b5a8faL, 0x42b2986cL, 0xdbbbc9d6L, 0xacbcf940L, 0x32d86ce3L,
0x45df5c75L, 0xdcd60dcfL, 0xabd13d59L, 0x26d930acL, 0x51de003aL,
0xc8d75180L, 0xbfd06116L, 0x21b4f4b5L, 0x56b3c423L, 0xcfba9599L,
0xb8bda50fL, 0x2802b89eL, 0x5f058808L, 0xc60cd9b2L, 0xb10be924L,
0x2f6f7c87L, 0x58684c11L, 0xc1611dabL, 0xb6662d3dL, 0x76dc4190L,
0x01db7106L, 0x98d220bcL, 0xefd5102aL, 0x71b18589L, 0x06b6b51fL,
0x9fbfe4a5L, 0xe8b8d433L, 0x7807c9a2L, 0x0f00f934L, 0x9609a88eL,
0xe10e9818L, 0x7f6a0dbbL, 0x086d3d2dL, 0x91646c97L, 0xe6635c01L,
0x6b6b51f4L, 0x1c6c6162L, 0x856530d8L, 0xf262004eL, 0x6c0695edL,
0x1b01a57bL, 0x8208f4c1L, 0xf50fc457L, 0x65b0d9c6L, 0x12b7e950L,
0x8bbeb8eaL, 0xfcb9887cL, 0x62dd1ddfL, 0x15da2d49L, 0x8cd37cf3L,
0xfbd44c65L, 0x4db26158L, 0x3ab551ceL, 0xa3bc0074L, 0xd4bb30e2L,
0x4adfa541L, 0x3dd895d7L, 0xa4d1c46dL, 0xd3d6f4fbL, 0x4369e96aL,
0x346ed9fcL, 0xad678846L, 0xda60b8d0L, 0x44042d73L, 0x33031de5L,
0xaa0a4c5fL, 0xdd0d7cc9L, 0x5005713cL, 0x270241aaL, 0xbe0b1010L,
0xc90c2086L, 0x5768b525L, 0x206f85b3L, 0xb966d409L, 0xce61e49fL,
0x5edef90eL, 0x29d9c998L, 0xb0d09822L, 0xc7d7a8b4L, 0x59b33d17L,
0x2eb40d81L, 0xb7bd5c3bL, 0xc0ba6cadL, 0xedb88320L, 0x9abfb3b6L,
0x03b6e20cL, 0x74b1d29aL, 0xead54739L, 0x9dd277afL, 0x04db2615L,
0x73dc1683L, 0xe3630b12L, 0x94643b84L, 0x0d6d6a3eL, 0x7a6a5aa8L,
0xe40ecf0bL, 0x9309ff9dL, 0x0a00ae27L, 0x7d079eb1L, 0xf00f9344L,
0x8708a3d2L, 0x1e01f268L, 0x6906c2feL, 0xf762575dL, 0x806567cbL,
0x196c3671L, 0x6e6b06e7L, 0xfed41b76L, 0x89d32be0L, 0x10da7a5aL,
0x67dd4accL, 0xf9b9df6fL, 0x8ebeeff9L, 0x17b7be43L, 0x60b08ed5L,
0xd6d6a3e8L, 0xa1d1937eL, 0x38d8c2c4L, 0x4fdff252L, 0xd1bb67f1L,
0xa6bc5767L, 0x3fb506ddL, 0x48b2364bL, 0xd80d2bdaL, 0xaf0a1b4cL,
0x36034af6L, 0x41047a60L, 0xdf60efc3L, 0xa867df55L, 0x316e8eefL,
0x4669be79L, 0xcb61b38cL, 0xbc66831aL, 0x256fd2a0L, 0x5268e236L,
0xcc0c7795L, 0xbb0b4703L, 0x220216b9L, 0x5505262fL, 0xc5ba3bbeL,
0xb2bd0b28L, 0x2bb45a92L, 0x5cb36a04L, 0xc2d7ffa7L, 0xb5d0cf31L,
0x2cd99e8bL, 0x5bdeae1dL, 0x9b64c2b0L, 0xec63f226L, 0x756aa39cL,
0x026d930aL, 0x9c0906a9L, 0xeb0e363fL, 0x72076785L, 0x05005713L,
0x95bf4a82L, 0xe2b87a14L, 0x7bb12baeL, 0x0cb61b38L, 0x92d28e9bL,
0xe5d5be0dL, 0x7cdcefb7L, 0x0bdbdf21L, 0x86d3d2d4L, 0xf1d4e242L,
0x68ddb3f8L, 0x1fda836eL, 0x81be16cdL, 0xf6b9265bL, 0x6fb077e1L,
0x18b74777L, 0x88085ae6L, 0xff0f6a70L, 0x66063bcaL, 0x11010b5cL,
0x8f659effL, 0xf862ae69L, 0x616bffd3L, 0x166ccf45L, 0xa00ae278L,
0xd70dd2eeL, 0x4e048354L, 0x3903b3c2L, 0xa7672661L, 0xd06016f7L,
0x4969474dL, 0x3e6e77dbL, 0xaed16a4aL, 0xd9d65adcL, 0x40df0b66L,
0x37d83bf0L, 0xa9bcae53L, 0xdebb9ec5L, 0x47b2cf7fL, 0x30b5ffe9L,
0xbdbdf21cL, 0xcabac28aL, 0x53b39330L, 0x24b4a3a6L, 0xbad03605L,
0xcdd70693L, 0x54de5729L, 0x23d967bfL, 0xb3667a2eL, 0xc4614ab8L,
0x5d681b02L, 0x2a6f2b94L, 0xb40bbe37L, 0xc30c8ea1L, 0x5a05df1bL,
0x2d02ef8dL
};

    public static int crc32(String bin_data) {
        return crc32(bin_data, 0);
    }

    public static int crc32(String bin_data, long crc) {
        int len = bin_data.length();

        crc &= 0xFFFFFFFFL;
        crc = crc ^ 0xFFFFFFFFL;
        for (int i = 0; i < len; i++) {
            char ch = bin_data.charAt(i);
            crc = (int)crc_32_tab[(int) ((crc ^ ch) & 0xffL)] ^ (crc >> 8);
            /* Note:  (crc >> 8) MUST zero fill on left */
            crc &= 0xFFFFFFFFL;
        }
        if (crc >= 0x80000000)
            return -(int)(crc+1 & 0xFFFFFFFF);
        else
            return (int)(crc & 0xFFFFFFFF);
    }


    private static char[] hexdigit = "0123456789abcdef".toCharArray();

    public static PyString __doc__b2a_hex = new PyString(
        "b2a_hex(data) -> s; Hexadecimal representation of binary data.\n" +
        "\n" +
        "This function is also available as \"hexlify()\"."
    );

    public static PyString b2a_hex(String argbuf) {
        int arglen = argbuf.length();

        StringBuilder retbuf = new StringBuilder(arglen*2);

        /* make hex version of string, taken from shamodule.c */
        for (int i = 0; i < arglen; i++) {
            char ch = argbuf.charAt(i);
            retbuf.append(hexdigit[(ch >>> 4) & 0xF]);
            retbuf.append(hexdigit[ch & 0xF]);
        }
        return new PyString(retbuf.toString());

    }


    public static PyString hexlify(String argbuf) {
        return b2a_hex(argbuf);
    }


    public static PyString a2b_hex$doc = new PyString(
        "a2b_hex(hexstr) -> s; Binary data of hexadecimal representation.\n" +
        "\n" +
        "hexstr must contain an even number of hex digits "+
        "(upper or lower case).\n"+
        "This function is also available as \"unhexlify()\""
    );


    public static PyString a2b_hex(String argbuf) {
        int arglen = argbuf.length();

        /* XXX What should we do about strings with an odd length?  Should
         * we add an implicit leading zero, or a trailing zero?  For now,
         * raise an exception.
         */
        if (arglen % 2 != 0)
            throw Py.TypeError("Odd-length string");

        StringBuilder retbuf = new StringBuilder(arglen/2);

        for (int i = 0; i < arglen; i += 2) {
            int top = Character.digit(argbuf.charAt(i), 16);
            int bot = Character.digit(argbuf.charAt(i+1), 16);
            if (top == -1 || bot == -1)
                throw Py.TypeError("Non-hexadecimal digit found");
            retbuf.append((char) ((top << 4) + bot));
        }
        return new PyString(retbuf.toString());
    }


    public static PyString unhexlify(String argbuf) {
        return a2b_hex(argbuf);
    }

    final private static char[] upper_hexdigit = "0123456789ABCDEF".toCharArray();
    
    private static StringBuilder qpEscape(StringBuilder sb, char c)
    {
    	sb.append('=');
        sb.append(upper_hexdigit[(c >>> 4) & 0xF]);
        sb.append(upper_hexdigit[c & 0xF]);
        return sb;
    }

    final private static Pattern UNDERSCORE = Pattern.compile("_");

    final public static PyString __doc__a2b_qp = new PyString("Decode a string of qp-encoded data");

    public static boolean getIntFlagAsBool(ArgParser ap, int index, int dflt, String errMsg) {
        boolean val;
        try {
        	val = ap.getInt(index, dflt) != 0;
        } catch (PyException e) {
        	if (Py.matchException(e, Py.AttributeError) || Py.matchException(e, Py.ValueError))
			throw Py.TypeError(errMsg);
        	throw e;
        }
        return val;
    }

    public static PyString a2b_qp(PyObject[] arg, String[] kws)
    {
        ArgParser ap = new ArgParser("a2b_qp", arg, kws, new String[] {"s", "header"});
        String s = ap.getString(0);
        StringBuilder sb = new StringBuilder();
        boolean header = getIntFlagAsBool(ap, 1, 0, "an integer is required");

        if (header)
        	s = UNDERSCORE.matcher(s).replaceAll(" ");
        
        for (int i=0, m=s.length(); i<m;) {
        	char c = s.charAt(i++);
        	if (c == '=') {
        		if (i < m) {
        			c = s.charAt(i++);
        			if (c == '=') {
        				sb.append(c);
                                } else if (c == ' ') {
                                    sb.append("= ");     
        			} else if ((c >= '0' && c <= '9' || c >= 'A' && c <= 'F') && i < m) {
        				char nc = s.charAt(i++);
        				if ((nc >= '0' && nc <= '9' || nc >= 'A' && nc <= 'F')) {
        					sb.append((char)(Character.digit(c, 16) << 4 | Character.digit(nc, 16)));
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
    }

    final private static Pattern RN_TO_N = Pattern.compile("\r\n");
    final private static Pattern N_TO_RN = Pattern.compile("(?<!\r)\n");
    
    final public static PyString __doc__b2a_qp = new PyString("b2a_qp(data, quotetabs=0, istext=1, header=0) -> s;\n"
    		+ "Encode a string using quoted-printable encoding.\n\n"
    		+ "On encoding, when istext is set, newlines are not encoded, and white\n"
    		+ "space at end of lines is.  When istext is not set, \r and \n (CR/LF) are\n"
    		+ "both encoded.  When quotetabs is set, space and tabs are encoded.");

    public static PyString b2a_qp(PyObject[] arg, String[] kws) {
        ArgParser ap = new ArgParser("b2a_qp", arg, kws, new String[] {"s", "quotetabs", "istext", "header"});
        String s = ap.getString(0);
        boolean quotetabs = getIntFlagAsBool(ap, 1, 0, "an integer is required");
        boolean istext = getIntFlagAsBool(ap, 2, 1, "an integer is required");
        boolean header = getIntFlagAsBool(ap, 3, 0, "an integer is required");

        String lineEnd;
        int pos = s.indexOf('\n');
        if (pos > 0 && s.charAt(pos-1) == '\r') {
        	lineEnd = "\r\n";
        	s = N_TO_RN.matcher(s).replaceAll("\r\n");
        } else {
        	lineEnd = "\n";
        	s = RN_TO_N.matcher(s).replaceAll("\n");
        }
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (int i=0, m=s.length(); i<m; i++) {
        	char c = s.charAt(i);
                
                // RFC 1521 requires that the line ending in a space or tab must have
                // that trailing character encoded.
                if (lineEnding(s, lineEnd, i)) {
                    count = 0;
                    sb.append(lineEnd);
                    if (lineEnd.length() > 1) i++;
                }
                else if ((c == '\t' || c == ' ' ) && endOfLine(s, lineEnd, i + 1)) {
                    count += 3;
                    qpEscape(sb, c);
                }
                else if (('!' <= c && c <= '<')
        		|| ('>' <= c && c <= '^')
        		|| ('`' <= c && c <= '~')
        		|| (c == '_' && !header)
        		|| (c == '\n' || c == '\r' && istext)) {
//        		if (count == 75 && i < s.length() - 1) {
                        if (count == 75 && !endOfLine(s, lineEnd, i + 1)) {
        			sb.append("=").append(lineEnd);
        			count = 0;
        		}
        		sb.append(c);
        		count++;
        	}
        	else if (!quotetabs && (c == '\t' || c == ' ')) {
        		if (count >= 72) {
        			sb.append("=").append(lineEnd);
        			count = 0;
        		}
        		
        		if (count >= 71) {
        			count += 3;
        			qpEscape(sb, c);
        		} else {
        			if (c == ' ' && header)
        				sb.append('_');
        			else
        				sb.append(c);
        			count += 1;
        		}
        	} else {
        		if (count >= 72) {
        			sb.append("=").append(lineEnd);
        			count = 0;
        		}
        		count += 3;
        		qpEscape(sb, c);
        	}
        }
        return new PyString(sb.toString());
    }
    
    private static boolean endOfLine(String s, String lineEnd, int i) {
        return (s.length() == i || lineEnding(s, lineEnd, i));
    }
    
    private static boolean lineEnding(String s, String lineEnd, int i) {
        return 
            (s.length() > i && s.substring(i).startsWith(lineEnd));
    }
    
/*
    public static void main(String[] args) {
        String l = b2a_uu("Hello");
        System.out.println(l);
        System.out.println(a2b_uu(l));

        l = b2a_base64("Hello");
        System.out.println(l);
        System.out.println(a2b_base64(l));

        l = b2a_hqx("Hello-");
        System.out.println(l);
        System.out.println(a2b_hqx(l));
    }
*/
}
