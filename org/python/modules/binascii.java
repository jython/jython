/*
 * Copyright 1998 Finn Bock.
 * Permission to use, copy and distribute this software is hereby granted, 
 * provided that the above copyright notice appear in all copies and that 
 * both that copyright notice and this permission notice appear.
 * 
 * No Warranty
 * The software is provided "as is" without warranty of any kind.
 * 
 * If you have questions regarding this software, contact:
 *    Finn Bock, bckfnn@pipmail.dknet.dk
 * 
 * This program contains material copyrighted by:
 * Copyright © 1991, 1992, 1993, 1994 by Stichting Mathematisch Centrum,
 * Amsterdam, The Netherlands. 
 */

package org.python.modules;


import java.util.*;

import org.python.core.*;

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
    public static final PyString Error = new PyString("binascii.Error");

    public static final PyString Incomplete = new PyString("binascii.Incomplete");


    // hqx lookup table, ascii->binary.
    private static char RUNCHAR = 0x90;

    private static short DONE = 0x7F;
    private static short SKIP = 0x7E;
    private static short FAIL = 0x7D;

    private static short[] table_a2b_hqx = {
	/*       ^@    ^A    ^B    ^C    ^D    ^E    ^F    ^G   */
	/* 0*/	FAIL, FAIL, FAIL, FAIL, FAIL, FAIL, FAIL, FAIL,
	/*       \b    \t    \n    ^K    ^L    \r    ^N    ^O   */
	/* 1*/	FAIL, FAIL, SKIP, FAIL, FAIL, SKIP, FAIL, FAIL,
	/*       ^P    ^Q    ^R    ^S    ^T    ^U    ^V    ^W   */
	/* 2*/	FAIL, FAIL, FAIL, FAIL, FAIL, FAIL, FAIL, FAIL,
	/*       ^X    ^Y    ^Z    ^[    ^\    ^]    ^^    ^_   */
	/* 3*/	FAIL, FAIL, FAIL, FAIL, FAIL, FAIL, FAIL, FAIL,
	/*              !     "     #     $     %     &     '   */
	/* 4*/	FAIL, 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06,
	/*        (     )     *     +     ,     -     .     /   */
	/* 5*/	0x07, 0x08, 0x09, 0x0A, 0x0B, 0x0C, FAIL, FAIL,
	/*        0     1     2     3     4     5     6     7   */
	/* 6*/	0x0D, 0x0E, 0x0F, 0x10, 0x11, 0x12, 0x13, FAIL,
	/*        8     9     :     ;     <     =     >     ?   */
	/* 7*/	0x14, 0x15, DONE, FAIL, FAIL, FAIL, FAIL, FAIL,
	/*        @     A     B     C     D     E     F     G   */
	/* 8*/	0x16, 0x17, 0x18, 0x19, 0x1A, 0x1B, 0x1C, 0x1D,
	/*        H     I     J     K     L     M     N     O   */
	/* 9*/	0x1E, 0x1F, 0x20, 0x21, 0x22, 0x23, 0x24, FAIL,
	/*        P     Q     R     S     T     U     V     W   */
	/*10*/	0x25, 0x26, 0x27, 0x28, 0x29, 0x2A, 0x2B, FAIL,
	/*        X     Y     Z     [     \     ]     ^     _   */
	/*11*/	0x2C, 0x2D, 0x2E, 0x2F, FAIL, FAIL, FAIL, FAIL,
	/*        `     a     b     c     d     e     f     g   */
	/*12*/	0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, FAIL,
	/*        h     i     j     k     l     m     n     o   */
	/*13*/	0x37, 0x38, 0x39, 0x3A, 0x3B, 0x3C, FAIL, FAIL,
	/*        p     q     r     s     t     u     v     w   */
	/*14*/	0x3D, 0x3E, 0x3F, FAIL, FAIL, FAIL, FAIL, FAIL,
	/*        x     y     z     {     |     }     ~    ^?   */
	/*15*/	FAIL, FAIL, FAIL, FAIL, FAIL, FAIL, FAIL, FAIL,
	/*16*/	FAIL, FAIL, FAIL, FAIL, FAIL, FAIL, FAIL, FAIL,
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
	"!\"#$%&'()*+,-012345689@ABCDEFGHIJKLMNPQRSTUVXYZ[`abcdefhijklmpqr".getBytes();




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
    private static int BASE64_MAXBIN = 57; /* Max binary chunk size (76 char line) */

    private static byte[] table_b2a_base64 =
	"ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".getBytes();



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


    /**
     * Convert a single line of uuencoded data back to binary and return the
     * binary data. Lines normally contain 45 (binary) bytes, except for the
     * last line. Line data may be followed by whitespace.
     */
    public static String a2b_uu(String ascii_data) { 
	int leftbits = 0;
	int leftchar = 0;

	StringBuffer bin_data = new StringBuffer();

	char this_ch;
	int i;

	int ascii_len = ascii_data.length()-1;

	int bin_len = (ascii_data.charAt(0) - ' ') & 077;

	for (i = 0; bin_len > 0; i++, ascii_len--) {
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
	return bin_data.toString();
    }


    /**
     * Convert binary data to a line of ASCII characters, the return value
     * is the converted line, including a newline char. The length of
     * <i>data</i> should be at most 45.
     */
    public static String b2a_uu(String bin_data) {
	int leftbits = 0;
	char this_ch;
	int leftchar = 0;

	int bin_len = bin_data.length();
	if (bin_len > 45) {
	    // The 45 is a limit that appears in all uuencode's
	    throw new PyException(Error, "At most 45 bytes at once");
	}

	StringBuffer ascii_data = new StringBuffer();

	// Store the length */
	ascii_data.append((char)(' ' + (bin_len & 077)));
	
	for (int i = 0; bin_len > 0 || leftbits != 0; i++, bin_len--) {
	    // Shift the data (or padding) into our buffer 
	    if (bin_len > 0)	// Data 
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
	
	return ascii_data.toString();
    }


    /** 
     * Convert a block of base64 data back to binary and return the
     * binary data. More than one line may be passed at a time.
     */
    public static String a2b_base64(String ascii_data) {
	int leftbits = 0;
	char this_ch;
	int leftchar = 0;
	int npad = 0;

	int ascii_len = ascii_data.length();

	int bin_len = 0;
	StringBuffer bin_data = new StringBuffer();

	for(int i = 0; ascii_len > 0 ; ascii_len--, i++) {
	    // Skip some punctuation 
	    this_ch = (char)(ascii_data.charAt(i) & 0x7f);
	    if (this_ch == '\r' || this_ch == '\n' || this_ch == ' ')
		continue;
		
	    if (this_ch == BASE64_PAD)
		npad++;
	    this_ch = (char)table_a2b_base64[(ascii_data.charAt(i)) & 0x7f];
	    if (this_ch == -1) continue;
	    // Shift it in on the low end, and see if there's
	    // a byte ready for output.
	    leftchar = (leftchar << 6) | (this_ch);
	    leftbits += 6;
	    if (leftbits >= 8) {
		leftbits -= 8;
		bin_data.append((char)((leftchar >> leftbits) & 0xff));
		leftchar &= ((1 << leftbits) - 1);
		bin_len++;
	    }
	}
	// Check that no bits are left
	if (leftbits != 0) {
	    throw new PyException(Error, "Incorrect padding");
	}
	// and remove any padding
	bin_len -= npad;
	return bin_data.toString();
    }


    /**
     * Convert binary data to a line of ASCII characters in base64 coding.
     * The return value is the converted line, including a newline char.
     * The length of <i>data</i> should be at most 57 to adhere to the base64
     * standard.
     */
    public static String b2a_base64(String bin_data) {
	int leftbits = 0;
	char this_ch;
	int leftchar = 0;

	StringBuffer ascii_data = new StringBuffer();

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
	
	return ascii_data.toString();
    }


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

	StringBuffer bin_data = new StringBuffer();

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
	    throw new PyException(Incomplete, "String has incomplete number of bytes");
	}
	
	return new PyTuple(new PyObject[] 
		{ Py.java2py(bin_data.toString()), Py.newInteger(done ? 1 : 0) });
    }


    /**
     * Perform binhex4 style RLE-compression on <i>data</i> and return the
     * result.
     */
    static public String rlecode_hqx(String in_data) {
	int len = in_data.length();
	
	StringBuffer out_data = new StringBuffer();
	
	for (int in=0; in < len; in++) {
	    char ch = in_data.charAt(in);
	    if (ch == RUNCHAR) {
		// RUNCHAR. Escape it.
		out_data.append(RUNCHAR);
		out_data.append(0);
	    } else {
		// Check how many following are the same
		int inend;
		for(inend=in+1; 
			inend < len && in_data.charAt(inend) == ch && inend < in+255;
			    inend++) 
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


    /**
     * Perform hexbin4 binary-to-ASCII translation and return the
     * resulting string. The argument should already be RLE-coded, and have a
     * length divisible by 3 (except possibly the last fragment).
     */
    public static String b2a_hqx(String bin_data) {
	int leftbits = 0;
	char this_ch;
	int leftchar = 0;

	int len = bin_data.length();

	StringBuffer ascii_data = new StringBuffer();
	
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
	return ascii_data.toString();
    }


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

	StringBuffer out_data = new StringBuffer();

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


    /**
     * Compute the binhex4 crc value of <i>data</i>, starting with an initial
     * <i>crc</i> and returning the result.
     */
    public static int crc_hqx(String bin_data, int crc) {
	int len = bin_data.length();
	int i = 0;

	while(len-- > 0) {
	    crc=((crc<<8)&0xff00)^crctab_hqx[((crc>>8)&0xff)^bin_data.charAt(i++)];
	}

	return crc;
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
