package org.python.core;

/** Stop-gap definition to satisfy references in the project. */
public class BaseBytes {

    //
    // Character class operations
    //

    // Bit to twiddle (XOR) for lowercase letter to uppercase and
    // vice-versa.
    private static final int SWAP_CASE = 0x20;

    // Bit masks and sets to use with the byte classification table
    private static final byte UPPER = 0b1;
    private static final byte LOWER = 0b10;
    private static final byte DIGIT = 0b100;
    private static final byte SPACE = 0b1000;
    private static final byte ALPHA = UPPER | LOWER;
    private static final byte ALNUM = ALPHA | DIGIT;

    // Character (byte) classification table.
    private static final byte[] ctype = new byte[256];
    static {
        for (int c = 'A'; c <= 'Z'; c++) {
            ctype[0x80 + c] = UPPER;
            ctype[0x80 + SWAP_CASE + c] = LOWER;
        }
        for (int c = '0'; c <= '9'; c++) { ctype[0x80 + c] = DIGIT; }
        for (char c : " \t\n\u000b\f\r".toCharArray()) { ctype[0x80 + c] = SPACE; }
    }

    /**
     * @param b to classify
     * @return b in ' \t\n\v\f\r'
     */
    static final boolean isspace(byte b) {
        return (ctype[0x80 + b] & SPACE) != 0;
    }

}
