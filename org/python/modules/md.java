package org.python.modules;

/* Class for implementing md4 and md5 hash algorithms.
 * There are constructors for prepping the hash algorithm (doing the
 * padding, mainly) for a String or a byte[], and an mdcalc() method
 * for generating the hash. The results can be accessed as an int array
 * by getregs(), or as a String of hex digits with toString().
 *
 * Written for jotp, by Harry Mantakos harry@meretrix.com
 *
 * Feel free to do whatever you like with this code.
 * If you do modify or use this code in another application,
 * I'd be interested in hearing from you!
 */

class md4 extends md {
    md4(String s) {
        super(s);
    }
    md4(byte in[]) {
        super(in);
    }
    static int F(int x, int y, int z) {
        return((x & y) | (~x & z));
    }
    static int G(int x, int y, int z) {
        return((x & y) | (x & z) | (y & z));
    }
    static int H(int x, int y, int z) {
        return(x ^ y ^ z);
    }
    void round1(int blk) {
        A = rotintlft((A + F(B, C, D) + d[0 + 16 * blk]), 3);
        D = rotintlft((D + F(A, B, C) + d[1 + 16 * blk]), 7);
        C = rotintlft((C + F(D, A, B) + d[2 + 16 * blk]), 11);
        B = rotintlft((B + F(C, D, A) + d[3 + 16 * blk]), 19);

        A = rotintlft((A + F(B, C, D) + d[4 + 16 * blk]), 3);
        D = rotintlft((D + F(A, B, C) + d[5 + 16 * blk]), 7);
        C = rotintlft((C + F(D, A, B) + d[6 + 16 * blk]), 11);
        B = rotintlft((B + F(C, D, A) + d[7 + 16 * blk]), 19);

        A = rotintlft((A + F(B, C, D) + d[8 + 16 * blk]), 3);
        D = rotintlft((D + F(A, B, C) + d[9 + 16 * blk]), 7);
        C = rotintlft((C + F(D, A, B) + d[10 + 16 * blk]), 11);
        B = rotintlft((B + F(C, D, A) + d[11 + 16 * blk]), 19);

        A = rotintlft((A + F(B, C, D) + d[12 + 16 * blk]), 3);
        D = rotintlft((D + F(A, B, C) + d[13 + 16 * blk]), 7);
        C = rotintlft((C + F(D, A, B) + d[14 + 16 * blk]), 11);
        B = rotintlft((B + F(C, D, A) + d[15 + 16 * blk]), 19);
    }
    void round2(int blk) {
        A = rotintlft((A + G(B, C, D) + d[0 + 16 * blk] + 0x5a827999), 3);
        D = rotintlft((D + G(A, B, C) + d[4 + 16 * blk] + 0x5a827999), 5);
        C = rotintlft((C + G(D, A, B) + d[8 + 16 * blk] + 0x5a827999), 9);
        B = rotintlft((B + G(C, D, A) + d[12 + 16 * blk] + 0x5a827999), 13);

        A = rotintlft((A + G(B, C, D) + d[1 + 16 * blk] + 0x5a827999), 3);
        D = rotintlft((D + G(A, B, C) + d[5 + 16 * blk] + 0x5a827999), 5);
        C = rotintlft((C + G(D, A, B) + d[9 + 16 * blk] + 0x5a827999), 9);
        B = rotintlft((B + G(C, D, A) + d[13 + 16 * blk] + 0x5a827999), 13);

        A = rotintlft((A + G(B, C, D) + d[2 + 16 * blk] + 0x5a827999), 3);
        D = rotintlft((D + G(A, B, C) + d[6 + 16 * blk] + 0x5a827999), 5);
        C = rotintlft((C + G(D, A, B) + d[10 + 16 * blk] + 0x5a827999), 9);
        B = rotintlft((B + G(C, D, A) + d[14 + 16 * blk] + 0x5a827999), 13);

        A = rotintlft((A + G(B, C, D) + d[3 + 16 * blk] + 0x5a827999), 3);
        D = rotintlft((D + G(A, B, C) + d[7 + 16 * blk] + 0x5a827999), 5);
        C = rotintlft((C + G(D, A, B) + d[11 + 16 * blk] + 0x5a827999), 9);
        B = rotintlft((B + G(C, D, A) + d[15 + 16 * blk] + 0x5a827999), 13);

    }
    void round3(int blk) {
        A = rotintlft((A + H(B, C, D) + d[0 + 16 * blk] + 0x6ed9eba1), 3);
        D = rotintlft((D + H(A, B, C) + d[8 + 16 * blk] + 0x6ed9eba1), 9);
        C = rotintlft((C + H(D, A, B) + d[4 + 16 * blk] + 0x6ed9eba1), 11);
        B = rotintlft((B + H(C, D, A) + d[12 + 16 * blk] + 0x6ed9eba1), 15);

        A = rotintlft((A + H(B, C, D) + d[2 + 16 * blk] + 0x6ed9eba1), 3);
        D = rotintlft((D + H(A, B, C) + d[10 + 16 * blk] + 0x6ed9eba1), 9);
        C = rotintlft((C + H(D, A, B) + d[6 + 16 * blk] + 0x6ed9eba1), 11);
        B = rotintlft((B + H(C, D, A) + d[14 + 16 * blk] + 0x6ed9eba1), 15);

        A = rotintlft((A + H(B, C, D) + d[1 + 16 * blk] + 0x6ed9eba1), 3);
        D = rotintlft((D + H(A, B, C) + d[9 + 16 * blk] + 0x6ed9eba1), 9);
        C = rotintlft((C + H(D, A, B) + d[5 + 16 * blk] + 0x6ed9eba1), 11);
        B = rotintlft((B + H(C, D, A) + d[13 + 16 * blk] + 0x6ed9eba1), 15);

        A = rotintlft((A + H(B, C, D) + d[3 + 16 * blk] + 0x6ed9eba1), 3);
        D = rotintlft((D + H(A, B, C) + d[11 + 16 * blk] + 0x6ed9eba1), 9);
        C = rotintlft((C + H(D, A, B) + d[7 + 16 * blk] + 0x6ed9eba1), 11);
        B = rotintlft((B + H(C, D, A) + d[15 + 16 * blk] + 0x6ed9eba1), 15);

    }
    void round4(int blk) {
        System.out.println(" must be md5, in round4!");
    }
}

class md5 extends md {
    md5(String s) {
        super(s);
    }
    md5(byte in[]) {
        super(in);
    }
    static int F(int x, int y, int z) {
        return((x & y) | (~x & z));
    }
    static int G(int x, int y, int z) {
        return((x & z) | (y & ~z));
    }
    static int H(int x, int y, int z) {
        return(x ^ y ^ z);
    }
    static int I(int x, int y, int z) {
        return(y ^ (x | ~z));
    }
    void round1(int blk) {
        A = rotintlft(A + F(B, C, D) + d[0 + 16 * blk] +
                      0xd76aa478, 7) + B;
        D = rotintlft(D + F(A, B, C) + d[1 + 16 * blk] +
                      0xe8c7b756, 12) + A;
        C = rotintlft(C + F(D, A, B) + d[2 + 16 * blk] +
                      0x242070db, 17) + D;
        B = rotintlft(B + F(C, D, A) + d[3 + 16 * blk] +
                      0xc1bdceee, 22) + C;

        A = rotintlft(A + F(B, C, D) + d[4 + 16 * blk] +
                      0xf57c0faf, 7) + B;
        D = rotintlft(D + F(A, B, C) + d[5 + 16 * blk] +
                      0x4787c62a, 12) + A;
        C = rotintlft(C + F(D, A, B) + d[6 + 16 * blk] +
                      0xa8304613, 17) + D;
        B = rotintlft(B + F(C, D, A) + d[7 + 16 * blk] +
                      0xfd469501, 22) + C;
        A = rotintlft(A + F(B, C, D) + d[8 + 16 * blk] +
                      0x698098d8, 7) + B;
        D = rotintlft(D + F(A, B, C) + d[9 + 16 * blk] +
                      0x8b44f7af, 12) + A;
        C = rotintlft(C + F(D, A, B) + d[10 + 16 * blk] +
                      0xffff5bb1, 17) + D;
        B = rotintlft(B + F(C, D, A) + d[11 + 16 * blk] +
                      0x895cd7be, 22) + C;
        A = rotintlft(A + F(B, C, D) + d[12 + 16 * blk] +
                      0x6b901122, 7) + B;
        D = rotintlft(D + F(A, B, C) + d[13 + 16 * blk] +
                      0xfd987193, 12) + A;
        C = rotintlft(C + F(D, A, B) + d[14 + 16 * blk] +
                      0xa679438e, 17) + D;
        B = rotintlft(B + F(C, D, A) + d[15 + 16 * blk] +
                      0x49b40821, 22) + C;
    }
    void round2(int blk) {
        A = rotintlft(A + G(B, C, D) + d[1 + 16 * blk] +
                      0xf61e2562, 5) + B;
        D = rotintlft(D + G(A, B, C) + d[6 + 16 * blk] +
                      0xc040b340, 9) + A;
        C = rotintlft(C + G(D, A, B) + d[11 + 16 * blk] +
                      0x265e5a51, 14) + D;
        B = rotintlft(B + G(C, D, A) + d[0 + 16 * blk] +
                      0xe9b6c7aa, 20) + C;
        A = rotintlft(A + G(B, C, D) + d[5 + 16 * blk] +
                      0xd62f105d, 5) + B;
        D = rotintlft(D + G(A, B, C) + d[10 + 16 * blk] +
                      0x02441453, 9) + A;
        C = rotintlft(C + G(D, A, B) + d[15 + 16 * blk] +
                      0xd8a1e681, 14) + D;
        B = rotintlft(B + G(C, D, A) + d[4 + 16 * blk] +
                      0xe7d3fbc8, 20) + C;
        A = rotintlft(A + G(B, C, D) + d[9 + 16 * blk] +
                      0x21e1cde6, 5) + B;
        D = rotintlft(D + G(A, B, C) + d[14 + 16 * blk] +
                      0xc33707d6, 9) + A;
        C = rotintlft(C + G(D, A, B) + d[3 + 16 * blk] +
                      0xf4d50d87, 14) + D;
        B = rotintlft(B + G(C, D, A) + d[8 + 16 * blk] +
                      0x455a14ed, 20) + C;
        A = rotintlft(A + G(B, C, D) + d[13 + 16 * blk] +
                      0xa9e3e905, 5) + B;
        D = rotintlft(D + G(A, B, C) + d[2 + 16 * blk] +
                      0xfcefa3f8, 9) + A;
        C = rotintlft(C + G(D, A, B) + d[7 + 16 * blk] +
                      0x676f02d9, 14) + D;
        B = rotintlft(B + G(C, D, A) + d[12 + 16 * blk] +
                      0x8d2a4c8a, 20) + C;
    }
    void round3(int blk) {
        A = rotintlft(A + H(B, C, D) + d[5 + 16 * blk] +
                      0xfffa3942, 4) + B;
        D = rotintlft(D + H(A, B, C) + d[8 + 16 * blk] +
                      0x8771f681, 11) + A;
        C = rotintlft(C + H(D, A, B) + d[11 + 16 * blk] +
                      0x6d9d6122, 16) + D;
        B = rotintlft(B + H(C, D, A) + d[14 + 16 * blk] +
                      0xfde5380c, 23) + C;
        A = rotintlft(A + H(B, C, D) + d[1 + 16 * blk] +
                      0xa4beea44, 4) + B;
        D = rotintlft(D + H(A, B, C) + d[4 + 16 * blk] +
                      0x4bdecfa9, 11) + A;
        C = rotintlft(C + H(D, A, B) + d[7 + 16 * blk] +
                      0xf6bb4b60, 16) + D;
        B = rotintlft(B + H(C, D, A) + d[10 + 16 * blk] +
                      0xbebfbc70, 23) + C;
        A = rotintlft(A + H(B, C, D) + d[13 + 16 * blk] +
                      0x289b7ec6, 4) + B;
        D = rotintlft(D + H(A, B, C) + d[0 + 16 * blk] +
                      0xeaa127fa, 11) + A;
        C = rotintlft(C + H(D, A, B) + d[3 + 16 * blk] +
                      0xd4ef3085, 16) + D;
        B = rotintlft(B + H(C, D, A) + d[6 + 16 * blk] +
                      0x04881d05, 23) + C;
        A = rotintlft(A + H(B, C, D) + d[9 + 16 * blk] +
                      0xd9d4d039, 4) + B;
        D = rotintlft(D + H(A, B, C) + d[12 + 16 * blk] +
                      0xe6db99e5, 11) + A;
        C = rotintlft(C + H(D, A, B) + d[15 + 16 * blk] +
                      0x1fa27cf8, 16) + D;
        B = rotintlft(B + H(C, D, A) + d[2 + 16 * blk] +
                      0xc4ac5665, 23) + C;
    }
    void round4(int blk) {
        A = rotintlft(A + I(B, C, D) + d[0 + 16 * blk] +
                      0xf4292244, 6) + B;
        D = rotintlft(D + I(A, B, C) + d[7 + 16 * blk] +
                      0x432aff97, 10) + A;
        C = rotintlft(C + I(D, A, B) + d[14 + 16 * blk] +
                      0xab9423a7, 15) + D;
        B = rotintlft(B + I(C, D, A) + d[5 + 16 * blk] +
                      0xfc93a039, 21) + C;
        A = rotintlft(A + I(B, C, D) + d[12 + 16 * blk] +
                      0x655b59c3, 6) + B;
        D = rotintlft(D + I(A, B, C) + d[3 + 16 * blk] +
                      0x8f0ccc92, 10) + A;
        C = rotintlft(C + I(D, A, B) + d[10 + 16 * blk] +
                      0xffeff47d, 15) + D;
        B = rotintlft(B + I(C, D, A) + d[1 + 16 * blk] +
                      0x85845dd1, 21) + C;
        A = rotintlft(A + I(B, C, D) + d[8 + 16 * blk] +
                      0x6fa87e4f, 6) + B;
        D = rotintlft(D + I(A, B, C) + d[15 + 16 * blk] +
                      0xfe2ce6e0, 10) + A;
        C = rotintlft(C + I(D, A, B) + d[6 + 16 * blk] +
                      0xa3014314, 15) + D;
        B = rotintlft(B + I(C, D, A) + d[13 + 16 * blk] +
                      0x4e0811a1, 21) + C;
        A = rotintlft(A + I(B, C, D) + d[4 + 16 * blk] +
                      0xf7537e82, 6) + B;
        D = rotintlft(D + I(A, B, C) + d[11 + 16 * blk] +
                      0xbd3af235, 10) + A;
        C = rotintlft(C + I(D, A, B) + d[2 + 16 * blk] +
                      0x2ad7d2bb, 15) + D;
        B = rotintlft(B + I(C, D, A) + d[9 + 16 * blk] +
                      0xeb86d391, 21) + C;
    }
}

class md {
    int A,B,C,D;
    int d[];
    int numwords;

    /* For verification of a modicum of sanity, run a few
     * test strings through
     */
    public static void main(String argv[]) {
        boolean doinmd4;
        String mdtype;
        /* Test cases, mostly taken from rfc 1320 */
        String str[] = { "" , "a", "abc", "message digest",
                         "abcdefghijklmnopqrstuvwxyz",
           "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789",
           "12345678901234567890123456789012345678901234567890123456789012" +
           "345678901234567890",
           "01234567890123456789012345678901234567890123456789012345"};

        if (argv.length == 0) {
            mdtype = "md4";
            doinmd4 = true;
        } else if (argv.length > 1) {
            System.err.println("Usage: md [4|5|md4|md5]");
            return;
        } else  if ((argv[0].equals("4")) || (argv[0].equals("md4"))) {
            mdtype = "md4";
            doinmd4 = true;
        } else if ((argv[0].equals("5")) || (argv[0].equals("md5"))) {
            mdtype = "md5";
            doinmd4 = false;
        } else {
            System.err.println("Usage: md [4|5|md4|md5]");
            return;
        }
        for(int i = 0; i < str.length; i++) {
            if (doinmd4) {
                md4 mdc = new md4(str[i]);
                mdc.calc();
                System.out.println(mdtype + "(\"" + str[i] + "\") = " + mdc);
            } else {
                md5 mdc = new md5(str[i]);
                mdc.calc();
                System.out.println(mdtype + "(\"" + str[i] + "\") = " + mdc);
            }
        }
    }
    md (String s) {
        byte in[] = new byte[s.length()];
        int i;

        for(i=0; i < s.length(); i++) {
            in[i] = (byte) (s.charAt(i) & 0xff);
        }
        mdinit(in);
    }
    md (byte in[]) {
        mdinit(in);
    }

    void mdinit (byte in[]) {
        int newlen, endblklen, pad, i;
        long datalenbits;

        datalenbits = in.length  * 8;
        endblklen = in.length % 64;
        if (endblklen < 56) {
            pad = 64 - endblklen;
        } else {
            pad = (64 - endblklen) + 64;
        }
        newlen = in.length + pad;
        byte b[] = new byte[newlen];
        for(i=0; i < in.length; i++) {
            b[i] = in[i];
        }
        b[in.length] = (byte) 0x80;
        for (i = b.length + 1; i < (newlen - 8); i++) {
            b[i] = 0;
        }
        for (i = 0; i < 8; i++) {
            b[newlen - 8 + i] = (byte) (datalenbits & 0xff);
            datalenbits >>= 8;
        }
        /* init registers */
        A = 0x67452301;
        B = 0xefcdab89;
        C = 0x98badcfe;
        D = 0x10325476;
        this.numwords = newlen/4;
        this.d = new int[this.numwords];
        for (i = 0; i < newlen; i += 4) {
            this.d[i/4] = (b[i] & 0xff) + ((b[i+1] & 0xff) << 8) +
                ((b[i+2] & 0xff) << 16) + ((b[i+3] & 0xff) << 24);
        }
   }

    public String toString() {
        String s;

        return(tohex(A) + tohex(B) + tohex(C) + tohex(D));
    }
    int[] getregs() {
        int regs[] = {this.A, this.B, this.C, this.D};

        return regs;
    }
    void calc() {
        int AA, BB, CC, DD, i;

        for(i=0; i < numwords/16; i++) {
            AA = A; BB = B; CC = C; DD = D;
            round1(i);
            round2(i);
            round3(i);
            if (this instanceof md5) {
                round4(i);
            }
            A += AA; B+= BB; C+= CC; D+= DD;
        }
    }
    /* Dummy round*() methods. these are overriden in the md4 and md5
     * subclasses
     */
    void round1(int blk) {
        System.err.println("Danger! Danger! Someone called md.round1()!");
    }
    void round2(int blk) {
        System.err.println("Danger! Danger! Someone called md.round2()!");
    }
    void round3(int blk) {
        System.err.println("Danger! Danger! Someone called md.round3()!");
    }
    void round4(int blk) {
        System.err.println("Danger! Danger! Someone called md.round4()!");
    }
    static int rotintlft(int val, int numbits) {
        return((val << numbits) | (val >>> (32 - numbits)));
    }
    static String tohex(int i) {
        int b;
        String tmpstr;

        tmpstr = "";
        for(b = 0; b < 4; b++) {
            tmpstr += Integer.toString((i >> 4) & 0xf, 16)
                + Integer.toString(i & 0xf, 16);
            i >>= 8;
        }
        return tmpstr;
    }

}
