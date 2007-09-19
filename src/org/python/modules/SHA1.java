/*
 * SHA1.java - An implementation of the SHA-1 Algorithm
 *
 * Modified for Jython by Finn Bock. The original was split
 * into two files.
 *
 * Original author and copyright:
 *
 * Copyright (c) 1997 Systemics Ltd
 * on behalf of the Cryptix Development Team.  All rights reserved.
 * @author  David Hopwood
 *
 * Cryptix General License
 * Copyright (c) 1995, 1996, 1997, 1998, 1999, 2000 The Cryptix Foundation
 * Limited.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are permitted provided that the
 * following conditions are met:
 *
 * - Redistributions of source code must retain the copyright notice, this
 *   list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the following
 *   disclaimer in the documentation and/or other materials
 *   provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE CRYPTIX FOUNDATION LIMITED
 * AND CONTRIBUTORS ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE CRYPTIX FOUNDATION LIMITED
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.python.modules;

import java.io.UnsupportedEncodingException;
import org.python.core.*;

/**
 * This class implements the SHA-1 message digest algorithm.
 * <p>
 * <b>References:</b>
 * <ol>
 *   <li> Bruce Schneier,
 *        "Section 18.7 Secure Hash Algorithm (SHA),"
 *        <cite>Applied Cryptography, 2nd edition</cite>,
 *        John Wiley &amp; Sons, 1996
 *        <p>
 *   <li> NIST FIPS PUB 180-1,
 *        "Secure Hash Standard",
 *        U.S. Department of Commerce, May 1993.<br>
 *        <a href="http://www.itl.nist.gov/div897/pubs/fip180-1.htm">
 *        http://www.itl.nist.gov/div897/pubs/fip180-1.htm</a>
 * </ol>
 * <p>
 * <b>Copyright</b> &copy; 1995-1997
 * <a href="http://www.systemics.com/">Systemics Ltd</a> on behalf of the
 * <a href="http://www.systemics.com/docs/cryptix/">Cryptix Development
 *    Team</a>.
 * <br>All rights reserved.
 * <p>
 * <b>Revision: 1.7</b>
 * @author Systemics Ltd
 * @author David Hopwood
 * @since  Cryptix 2.2.2
 */
public final class SHA1 {
    /**
     * The buffer used to store the last incomplete block.
     */
    private byte[] buffer;

    /**
     * The number of bytes currently stored in <code>buffer</code>.
     */
    private int buffered;

    /**
     * The number of bytes that have been input to the digest.
     */
    private long count;

    public int digest_size = 20;


    /**
     * <b>SPI</b>: Updates the message digest with a byte of new data.
     *
     * @param b     the byte to be added.
     */
    protected void engineUpdate(byte b)
    {
        byte[] data = { b };
        engineUpdate(data, 0, 1);
    }

    /**
     * <b>SPI</b>: Updates the message digest with new data.
     *
     * @param data      the data to be added.
     * @param offset    the start of the data in the array.
     * @param length    the number of bytes of data to add.
     */
    protected void engineUpdate(byte[] data, int offset, int length)
    {
        count += length;

        int datalen = DATA_LENGTH;
        int remainder;

        while (length >= (remainder = datalen - buffered)) {
            System.arraycopy(data, offset, buffer, buffered, remainder);
            engineTransform(buffer);
            length -= remainder;
            offset += remainder;
            buffered = 0;
        }

        if (length > 0) {
            System.arraycopy(data, offset, buffer, buffered, length);
            buffered += length;
        }
    }

    /**
     * <b>SPI</b>: Calculates the final digest. BlockMessageDigest
     * subclasses should not usually override this method.
     *
     * @return the digest as a byte array.
     */
    protected byte[] engineDigest()
    {
        return engineDigest(buffer, buffered);
    }



// SHA-1 constants and variables
//...........................................................................

    /**
     * Length of the final hash (in bytes).
     */
    private static final int HASH_LENGTH = 20;

    /**
     * Length of a block (i.e. the number of bytes hashed in every transform).
     */
    private static final int DATA_LENGTH = 64;

    private int[] data;
    private int[] digest;
    private byte[] tmp;
    private int[] w;

    /**
     * Constructs a SHA-1 message digest.
     */
    public SHA1()
    {
        buffer = new byte[DATA_LENGTH];
        java_init();
        engineReset();
    }

    private void java_init()
    {
        digest = new int[HASH_LENGTH/4];
        data = new int[DATA_LENGTH/4];
        tmp = new byte[DATA_LENGTH];
        w = new int[80];
    }

    /**
     *    This constructor is here to implement cloneability of this class.
     */
    private SHA1 (SHA1 md) {
        this();
        data = md.data.clone();
        digest = md.digest.clone();
        tmp = md.tmp.clone();
        w = md.w.clone();
        buffer = md.buffer.clone();
        buffered = md.buffered;
        count = md.count;
    }

    /**
     * Initializes (resets) the message digest.
     */
    protected void engineReset()
    {
        buffered = 0;
        count = 0;
        java_reset();
    }

    private void java_reset()
    {
        digest[0] = 0x67452301;
        digest[1] = 0xefcdab89;
        digest[2] = 0x98badcfe;
        digest[3] = 0x10325476;
        digest[4] = 0xc3d2e1f0;
    }

    /**
     * Adds data to the message digest.
     *
     * @param data    The data to be added.
     * @param offset  The start of the data in the array.
     * @param length  The amount of data to add.
     */
    protected void engineTransform(byte[] in)
    {
        java_transform(in);
    }

    private void java_transform(byte[] in)
    {
        byte2int(in, 0, data, 0, DATA_LENGTH/4);
        transform(data);
    }

    /**
     * Returns the digest of the data added and resets the digest.
     * @return    the digest of all the data added to the message digest
     *            as a byte array.
     */
    protected byte[] engineDigest(byte[] in, int length)
    {
        byte b[] = java_digest(in, length);
        return b;
    }

    private byte[] java_digest(byte[] in, int pos)
    {
	int[] digest_save = digest.clone();
        if (pos != 0) System.arraycopy(in, 0, tmp, 0, pos);
	
        tmp[pos++] = (byte)0x80;

        if (pos > DATA_LENGTH - 8)
        {
            while (pos < DATA_LENGTH)
                tmp[pos++] = 0;

            byte2int(tmp, 0, data, 0, DATA_LENGTH/4);
            transform(data);
            pos = 0;
        }

        while (pos < DATA_LENGTH - 8)
            tmp[pos++] = 0;

        byte2int(tmp, 0, data, 0, (DATA_LENGTH/4)-2);

        // Big endian
        // WARNING: int>>>32 != 0 !!!
        // bitcount() used to return a long, now it's an int.
        long bc = count * 8;
        data[14] = (int) (bc>>>32);
        data[15] = (int) bc;

        transform(data);

        byte buf[] = new byte[HASH_LENGTH];

        // Big endian
        int off = 0;
        for (int i = 0; i < HASH_LENGTH/4; ++i) {
            int d = digest[i];
            buf[off++] = (byte) (d>>>24);
            buf[off++] = (byte) (d>>>16);
            buf[off++] = (byte) (d>>>8);
            buf[off++] = (byte)  d;
        }
	digest = digest_save;
        return buf;
    }


// SHA-1 transform routines
//...........................................................................

    private static int f1(int a, int b, int c) {
        return (c^(a&(b^c))) + 0x5A827999;
    }
    private static int f2(int a, int b, int c) {
        return (a^b^c) + 0x6ED9EBA1;
    }
    private static int f3(int a, int b, int c) {
        return ((a&b)|(c&(a|b))) + 0x8F1BBCDC;
    }
    private static int f4(int a, int b, int c) {
        return (a^b^c) + 0xCA62C1D6;
    }

    private void transform (int[] X)
    {
        int A = digest[0];
        int B = digest[1];
        int C = digest[2];
        int D = digest[3];
        int E = digest[4];

        int W[] = w;
        for (int i=0; i<16; i++)
        {
            W[i] = X[i];
        }
        for (int i=16; i<80; i++)
        {
            int j = W[i-16] ^ W[i-14] ^ W[i-8] ^ W[i-3];
            W[i] = j;
            W[i] = (j << 1) | (j >>> -1);
        }

        E += ((A<<5)|(A >>> -5)) + f1(B,C,D) + W[0];  B =((B<<30)|(B>>>-30));
        D += ((E<<5)|(E >>> -5)) + f1(A,B,C) + W[1];  A =((A<<30)|(A>>>-30));
        C += ((D<<5)|(D >>> -5)) + f1(E,A,B) + W[2];  E =((E<<30)|(E>>>-30));
        B += ((C<<5)|(C >>> -5)) + f1(D,E,A) + W[3];  D =((D<<30)|(D>>>-30));
        A += ((B<<5)|(B >>> -5)) + f1(C,D,E) + W[4];  C =((C<<30)|(C>>>-30));
        E += ((A<<5)|(A >>> -5)) + f1(B,C,D) + W[5];  B =((B<<30)|(B>>>-30));
        D += ((E<<5)|(E >>> -5)) + f1(A,B,C) + W[6];  A =((A<<30)|(A>>>-30));
        C += ((D<<5)|(D >>> -5)) + f1(E,A,B) + W[7];  E =((E<<30)|(E>>>-30));
        B += ((C<<5)|(C >>> -5)) + f1(D,E,A) + W[8];  D =((D<<30)|(D>>>-30));
        A += ((B<<5)|(B >>> -5)) + f1(C,D,E) + W[9];  C =((C<<30)|(C>>>-30));
        E += ((A<<5)|(A >>> -5)) + f1(B,C,D) + W[10]; B =((B<<30)|(B>>>-30));
        D += ((E<<5)|(E >>> -5)) + f1(A,B,C) + W[11]; A =((A<<30)|(A>>>-30));
        C += ((D<<5)|(D >>> -5)) + f1(E,A,B) + W[12]; E =((E<<30)|(E>>>-30));
        B += ((C<<5)|(C >>> -5)) + f1(D,E,A) + W[13]; D =((D<<30)|(D>>>-30));
        A += ((B<<5)|(B >>> -5)) + f1(C,D,E) + W[14]; C =((C<<30)|(C>>>-30));
        E += ((A<<5)|(A >>> -5)) + f1(B,C,D) + W[15]; B =((B<<30)|(B>>>-30));
        D += ((E<<5)|(E >>> -5)) + f1(A,B,C) + W[16]; A =((A<<30)|(A>>>-30));
        C += ((D<<5)|(D >>> -5)) + f1(E,A,B) + W[17]; E =((E<<30)|(E>>>-30));
        B += ((C<<5)|(C >>> -5)) + f1(D,E,A) + W[18]; D =((D<<30)|(D>>>-30));
        A += ((B<<5)|(B >>> -5)) + f1(C,D,E) + W[19]; C =((C<<30)|(C>>>-30));
        E += ((A<<5)|(A >>> -5)) + f2(B,C,D) + W[20]; B =((B<<30)|(B>>>-30));
        D += ((E<<5)|(E >>> -5)) + f2(A,B,C) + W[21]; A =((A<<30)|(A>>>-30));
        C += ((D<<5)|(D >>> -5)) + f2(E,A,B) + W[22]; E =((E<<30)|(E>>>-30));
        B += ((C<<5)|(C >>> -5)) + f2(D,E,A) + W[23]; D =((D<<30)|(D>>>-30));
        A += ((B<<5)|(B >>> -5)) + f2(C,D,E) + W[24]; C =((C<<30)|(C>>>-30));
        E += ((A<<5)|(A >>> -5)) + f2(B,C,D) + W[25]; B =((B<<30)|(B>>>-30));
        D += ((E<<5)|(E >>> -5)) + f2(A,B,C) + W[26]; A =((A<<30)|(A>>>-30));
        C += ((D<<5)|(D >>> -5)) + f2(E,A,B) + W[27]; E =((E<<30)|(E>>>-30));
        B += ((C<<5)|(C >>> -5)) + f2(D,E,A) + W[28]; D =((D<<30)|(D>>>-30));
        A += ((B<<5)|(B >>> -5)) + f2(C,D,E) + W[29]; C =((C<<30)|(C>>>-30));
        E += ((A<<5)|(A >>> -5)) + f2(B,C,D) + W[30]; B =((B<<30)|(B>>>-30));
        D += ((E<<5)|(E >>> -5)) + f2(A,B,C) + W[31]; A =((A<<30)|(A>>>-30));
        C += ((D<<5)|(D >>> -5)) + f2(E,A,B) + W[32]; E =((E<<30)|(E>>>-30));
        B += ((C<<5)|(C >>> -5)) + f2(D,E,A) + W[33]; D =((D<<30)|(D>>>-30));
        A += ((B<<5)|(B >>> -5)) + f2(C,D,E) + W[34]; C =((C<<30)|(C>>>-30));
        E += ((A<<5)|(A >>> -5)) + f2(B,C,D) + W[35]; B =((B<<30)|(B>>>-30));
        D += ((E<<5)|(E >>> -5)) + f2(A,B,C) + W[36]; A =((A<<30)|(A>>>-30));
        C += ((D<<5)|(D >>> -5)) + f2(E,A,B) + W[37]; E =((E<<30)|(E>>>-30));
        B += ((C<<5)|(C >>> -5)) + f2(D,E,A) + W[38]; D =((D<<30)|(D>>>-30));
        A += ((B<<5)|(B >>> -5)) + f2(C,D,E) + W[39]; C =((C<<30)|(C>>>-30));
        E += ((A<<5)|(A >>> -5)) + f3(B,C,D) + W[40]; B =((B<<30)|(B>>>-30));
        D += ((E<<5)|(E >>> -5)) + f3(A,B,C) + W[41]; A =((A<<30)|(A>>>-30));
        C += ((D<<5)|(D >>> -5)) + f3(E,A,B) + W[42]; E =((E<<30)|(E>>>-30));
        B += ((C<<5)|(C >>> -5)) + f3(D,E,A) + W[43]; D =((D<<30)|(D>>>-30));
        A += ((B<<5)|(B >>> -5)) + f3(C,D,E) + W[44]; C =((C<<30)|(C>>>-30));
        E += ((A<<5)|(A >>> -5)) + f3(B,C,D) + W[45]; B =((B<<30)|(B>>>-30));
        D += ((E<<5)|(E >>> -5)) + f3(A,B,C) + W[46]; A =((A<<30)|(A>>>-30));
        C += ((D<<5)|(D >>> -5)) + f3(E,A,B) + W[47]; E =((E<<30)|(E>>>-30));
        B += ((C<<5)|(C >>> -5)) + f3(D,E,A) + W[48]; D =((D<<30)|(D>>>-30));
        A += ((B<<5)|(B >>> -5)) + f3(C,D,E) + W[49]; C =((C<<30)|(C>>>-30));
        E += ((A<<5)|(A >>> -5)) + f3(B,C,D) + W[50]; B =((B<<30)|(B>>>-30));
        D += ((E<<5)|(E >>> -5)) + f3(A,B,C) + W[51]; A =((A<<30)|(A>>>-30));
        C += ((D<<5)|(D >>> -5)) + f3(E,A,B) + W[52]; E =((E<<30)|(E>>>-30));
        B += ((C<<5)|(C >>> -5)) + f3(D,E,A) + W[53]; D =((D<<30)|(D>>>-30));
        A += ((B<<5)|(B >>> -5)) + f3(C,D,E) + W[54]; C =((C<<30)|(C>>>-30));
        E += ((A<<5)|(A >>> -5)) + f3(B,C,D) + W[55]; B =((B<<30)|(B>>>-30));
        D += ((E<<5)|(E >>> -5)) + f3(A,B,C) + W[56]; A =((A<<30)|(A>>>-30));
        C += ((D<<5)|(D >>> -5)) + f3(E,A,B) + W[57]; E =((E<<30)|(E>>>-30));
        B += ((C<<5)|(C >>> -5)) + f3(D,E,A) + W[58]; D =((D<<30)|(D>>>-30));
        A += ((B<<5)|(B >>> -5)) + f3(C,D,E) + W[59]; C =((C<<30)|(C>>>-30));
        E += ((A<<5)|(A >>> -5)) + f4(B,C,D) + W[60]; B =((B<<30)|(B>>>-30));
        D += ((E<<5)|(E >>> -5)) + f4(A,B,C) + W[61]; A =((A<<30)|(A>>>-30));
        C += ((D<<5)|(D >>> -5)) + f4(E,A,B) + W[62]; E =((E<<30)|(E>>>-30));
        B += ((C<<5)|(C >>> -5)) + f4(D,E,A) + W[63]; D =((D<<30)|(D>>>-30));
        A += ((B<<5)|(B >>> -5)) + f4(C,D,E) + W[64]; C =((C<<30)|(C>>>-30));
        E += ((A<<5)|(A >>> -5)) + f4(B,C,D) + W[65]; B =((B<<30)|(B>>>-30));
        D += ((E<<5)|(E >>> -5)) + f4(A,B,C) + W[66]; A =((A<<30)|(A>>>-30));
        C += ((D<<5)|(D >>> -5)) + f4(E,A,B) + W[67]; E =((E<<30)|(E>>>-30));
        B += ((C<<5)|(C >>> -5)) + f4(D,E,A) + W[68]; D =((D<<30)|(D>>>-30));
        A += ((B<<5)|(B >>> -5)) + f4(C,D,E) + W[69]; C =((C<<30)|(C>>>-30));
        E += ((A<<5)|(A >>> -5)) + f4(B,C,D) + W[70]; B =((B<<30)|(B>>>-30));
        D += ((E<<5)|(E >>> -5)) + f4(A,B,C) + W[71]; A =((A<<30)|(A>>>-30));
        C += ((D<<5)|(D >>> -5)) + f4(E,A,B) + W[72]; E =((E<<30)|(E>>>-30));
        B += ((C<<5)|(C >>> -5)) + f4(D,E,A) + W[73]; D =((D<<30)|(D>>>-30));
        A += ((B<<5)|(B >>> -5)) + f4(C,D,E) + W[74]; C =((C<<30)|(C>>>-30));
        E += ((A<<5)|(A >>> -5)) + f4(B,C,D) + W[75]; B =((B<<30)|(B>>>-30));
        D += ((E<<5)|(E >>> -5)) + f4(A,B,C) + W[76]; A =((A<<30)|(A>>>-30));
        C += ((D<<5)|(D >>> -5)) + f4(E,A,B) + W[77]; E =((E<<30)|(E>>>-30));
        B += ((C<<5)|(C >>> -5)) + f4(D,E,A) + W[78]; D =((D<<30)|(D>>>-30));
        A += ((B<<5)|(B >>> -5)) + f4(C,D,E) + W[79]; C =((C<<30)|(C>>>-30));

        digest[0] += A;
        digest[1] += B;
        digest[2] += C;
        digest[3] += D;
        digest[4] += E;
    }

    // why was this public?
    // Note: parameter order changed to be consistent with System.arraycopy.
    private static void byte2int(byte[] src, int srcOffset,
                                 int[] dst, int dstOffset, int length)
    {
        while (length-- > 0)
        {
            // Big endian
            dst[dstOffset++] = (src[srcOffset++]         << 24) |
                               ((src[srcOffset++] & 0xFF) << 16) |
                               ((src[srcOffset++] & 0xFF) <<  8) |
                                (src[srcOffset++] & 0xFF);
        }
    }







    public static PyString __doc__update = new PyString(
        "Update this hashing object's state with the provided string."
    );

    /**
     * Add an array of bytes to the digest.
     */
    public synchronized void update(byte input[]) {
        engineUpdate(input, 0, input.length);
    }



    public static PyString __doc__copy = new PyString(
        "Return a copy of the hashing object."
    );

    /**
     * Add an array of bytes to the digest.
     */
    public SHA1 copy() {
        return new SHA1(this);
    }



    public static PyString __doc__hexdigest = new PyString(
        "Return the digest value as a string of hexadecimal digits."
    );

    /**
     * Print out the digest in a form that can be easily compared
     * to the test vectors.
     */
    public String hexdigest() {
	byte[] digestBits = engineDigest();

        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < 20; i++) {
            char c1, c2;

            c1 = (char) ((digestBits[i] >>> 4) & 0xf);
            c2 = (char) (digestBits[i] & 0xf);
            c1 = (char) ((c1 > 9) ? 'a' + (c1 - 10) : '0' + c1);
            c2 = (char) ((c2 > 9) ? 'a' + (c2 - 10) : '0' + c2);
            sb.append(c1);
            sb.append(c2);
        }
        return sb.toString();
    }


    public static PyString __doc__digest = new PyString(
        "Return the digest value as a string of binary data."
    );

    public String digest() {
        return PyString.from_bytes(engineDigest());
    }

    // XXX should become PyObject and use Py.idstr?
    public String toString() {
        return "<SHA object at" + System.identityHashCode(this) + ">";
    }
}
