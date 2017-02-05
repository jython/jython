/* Copyright 1998 Finn Bock.
 * Updated 2017 by Stefan Richthofer to support Unicode 9.0
 */

package org.python.modules;

import java.io.InputStream;
import java.io.DataInputStream;
import java.io.BufferedInputStream;
import java.io.IOException;
import org.python.core.ucnhashAPI;

public class ucnhash implements ucnhashAPI {

    // Parameters for the word hash.
    private static int n;
    private static int m;
    private static int minchar;
    private static int alphasz;
    private static int maxlen;
    private static int maxidx;
    private static int maxklen;

    private static short[] G;
    private static short[] T0;
    private static short[] T1;
    private static short[] T2;

    // Map the hashed values into the text (as bytes).
    private static byte[] worddata;
    private static short[] wordoffs;

    // wordindex greate then cutoff is stored as into two bytes.
    private static short wordstart;
    private static short wordcutoff;

    // The raw data and indexes into start of each name
    // The rawindex is sorted based on the wordindexes.
    private static byte[] rawdata;
    private static int[] rawindex;

    // The mapping from raw data index to unicode code points.
    private static int[] codepoint;


    public static String[] __depends__ = new String[] {
        "/org/python/modules/ucnhash.dat",
    };


    public static void loadTables() throws Exception {
        InputStream instream =
                ucnhash.class.getResourceAsStream("ucnhash.dat");
        if (instream == null)
            throw new IOException(
                    "Unicode name database not found: ucnhash.dat");

        DataInputStream in =
                new DataInputStream(new BufferedInputStream(instream));

        n = in.readShort();
        m = in.readShort();
        minchar = in.readShort();
        alphasz = in.readShort();
        maxlen = in.readShort();
        maxidx = maxlen*alphasz-minchar;
        /*
        if (debug) {
            System.out.println("n "+n+"  m "+m+"  maxlen "+maxlen+
                    "  minchar "+minchar+"  alphasz "+alphasz);
        } */
        G = readShortTable(in);
        if (in.readShort() != 3)
            throw new IOException("UnicodeNameMap file corrupt, " +
                                  "unknown dimension");

        T0 = readShortTable(in);
        T1 = readShortTable(in);
        T2 = readShortTable(in);

        wordoffs = readShortTable(in);
        worddata = readByteTable(in);
        /*
        if (debug) {
            System.out.println("G "+G.length+"  T0 "+T0.length+
                    "  T1 "+T1.length+"  T2 "+T2.length);
            System.out.println("wordoffs: "+wordoffs.length+
                    "  worddata: "+worddata.length);
        }*/

        wordstart  = in.readShort();
        wordcutoff = in.readShort();
        maxklen = in.readShort();

        rawdata = readByteTable(in);
        // Formerly rawindex and codepoint were 16 bit
        //rawindex = readCharTable(in);
        //codepoint = readCharTable(in);
        rawindex = readIntTable(in);
        codepoint = readIntTable(in);
        /*
        if (debug) {
            System.out.println("wordstart: "+wordstart+
                    "  wordcutoff: "+wordcutoff+"  maxklen: "+maxklen);
            System.out.println("rawdata: "+rawdata.length);
            System.out.println("rawindex: "+rawindex.length+
                    "  codepoint: "+codepoint.length);
        }*/
    }

    private static short[] readShortTable(DataInputStream in)
            throws IOException
    {
        if (in.read() != 't') {
            throw new IOException("UnicodeNameMap file corrupt, shorttable");
        }
        int n = in.readInt() / 2;
        short[] table = new short[n];
        for (int i = 0; i < n; i++) {
            table[i] = in.readShort();
        }
        return table;
    }

    private static int[] readIntTable(DataInputStream in)
            throws IOException
    {
        if (in.read() != 't') {
            throw new IOException("UnicodeNameMap file corrupt, inttable");
        }
        int n = in.readInt() / 4;
        int[] table = new int[n];
        for (int i = 0; i < n; i++) {
            table[i] = in.readInt();
        }
        return table;
    }

    private static char[] readCharTable(DataInputStream in)
            throws IOException
    {
        if (in.read() != 't') {
            throw new IOException("UnicodeNameMap file corrupt, chartable");
        }
        int n = in.readInt() / 2;
        char[] table = new char[n];
        for (int i = 0; i < n; i++) {
            table[i] = in.readChar();
        }
        return table;
    }

    private static byte[] readByteTable(DataInputStream in)
            throws IOException
    {
        if (in.read() != 't') {
            throw new IOException("UnicodeNameMap file corrupt, byte table");
        }
        int n = in.readInt();
        byte[] table = new byte[n];
        in.readFully(table);
        return table;
    }

    public static int hash(String key) {
        return hash(key, 0, key.length());
    }

    public static int hash(String key, int start, int end) {
        int i, j;
        int f0, f1, f2;

        for (j = start, i=-minchar, f0=f1=f2=0; j < end; j++) {
            char ch = key.charAt(j);
            if (ch >= 'a' && ch <= 'z')
                ch = (char) (ch  - 'a' + 'A');
            f0 += T0[i + ch];
            f1 += T1[i + ch];
            f2 += T2[i + ch];
            i += alphasz;
            if (i >= maxidx)
                i = -minchar;
        }

        f0 %= n;
        f1 %= n;
        f2 %= n;

        return (G[f0] + G[f1] + G[f2]) % m;
    }

    private static final char[] charmap =
            " ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-()".toCharArray();

    private static String getWord(int idx) {
        int offset = wordoffs[idx];
        int end = worddata.length;
        if (idx < wordoffs.length-1)
            end = wordoffs[idx+1];
        StringBuilder buf = new StringBuilder();
        for (int i = offset; i < end; i++)
            buf.append(charmap[worddata[i]]);
        return buf.toString();
    }

    private static boolean match(int idx, byte[] raw, int begin, int end) {
        int woff = wordoffs[idx];
        int wend = worddata.length;
        if (idx < wordoffs.length-1) {
            wend = wordoffs[idx+1];
        }
        if (end-begin != wend - woff) {
            return false;
        }
        int l = end-begin;
        for (int i = 0; i < l; i++) {
            if (worddata[woff + i] != raw[begin + i]) {
                return false;
            }
        }
        return true;
    }

    private static int compare(byte[] a1, int off1, int len1,
                               byte[] a2, int off2, int len2)
    {
        for (int i = 0; i < len1 && i < len2; i++) {
            int d = (a1[off1 + i] & 0xFF) - (a2[off2 + i] & 0xFF);
            if (d != 0) {
                return d;
            }
        }
        return len1 - len2;
    }

    // Was formerly 5, before rawindex changed to 32 bit:
    private static final int raw_block = 3;

    private static int binarysearch(byte[] rawlist, int start, int end) {
        int floor = 0;
        int ceiling = (rawindex.length) / raw_block;
        int middle, off, len, d;
        while (floor < ceiling - 1) {
            middle = (floor + ceiling) / 2;
            /*if (debug)
                System.out.println("floor:" + floor + " ceiling:" +
                                   ceiling +" => " + middle); */
            off = rawindex[middle*raw_block];
            len = rawindex[middle*raw_block+raw_block-1] & 0x1F;
            d = compare(rawlist, start, end - start, rawdata, off, len);
            if (d < 0)
                ceiling = middle;
            else if (d > 0)
                floor = middle;
            else
                return middle * 12;
        }

        int tmp = floor*raw_block;
        off = rawindex[tmp++];
        long lengths = (long) rawindex[tmp++] << 32 |
                              rawindex[tmp++] & 0xFFFFFFFFL;
        floor *= 12;
        for (int i = 0; i < 12; i++) {
            len = (int) (lengths >> (i * 5)) & 0x1F;
            if (compare(rawlist, start, end, rawdata, off, len) == 0)
                return floor;
            off += len;
            floor++;
        }
        return -1;
    }

    public static int lookup(String name) {
        return lookup(name, 0, name.length());
    }

    private static int lookup(String name, int start, int end) {
        byte[] rawlist = new byte[32];
        int ridx = 0;
        int rbegin = 0;
        int rstart = 0;

        int i, begin; char ch; byte v;
        while (true) {
            rbegin = ridx;
            begin = start;
lfor:       for (i = start; i < end; i++) {
                ch = name.charAt(i);
                /*
                if (ch == ' ') {
                    start = i+1;
                    break;
                }
                if (ch >= 'a' && ch <= 'z')
                    ch = (char) (ch  - 'a' + 'A');
                if (ch >= 'A' && ch <= 'Z')
                    v = ch - 'A' + 1;
                else if (ch >= '0' && ch <= '9')
                    v = ch - '0' + 27;
                else {
                    switch (ch) {
                        case '-': v = 37; break;
                        case '(': v = 38; break;
                        case ')': v = 39; break;
                        default: return -1;
                    }
                } */ // Unfold this logic into one switch:
                // (generated by printCharCases(), see below)
                switch (ch) {
                    case ' ': start = i+1; break lfor;
                    case 'a': v =  1; break;
                    case 'b': v =  2; break;
                    case 'c': v =  3; break;
                    case 'd': v =  4; break;
                    case 'e': v =  5; break;
                    case 'f': v =  6; break;
                    case 'g': v =  7; break;
                    case 'h': v =  8; break;
                    case 'i': v =  9; break;
                    case 'j': v = 10; break;
                    case 'k': v = 11; break;
                    case 'l': v = 12; break;
                    case 'm': v = 13; break;
                    case 'n': v = 14; break;
                    case 'o': v = 15; break;
                    case 'p': v = 16; break;
                    case 'q': v = 17; break;
                    case 'r': v = 18; break;
                    case 's': v = 19; break;
                    case 't': v = 20; break;
                    case 'u': v = 21; break;
                    case 'v': v = 22; break;
                    case 'w': v = 23; break;
                    case 'x': v = 24; break;
                    case 'y': v = 25; break;
                    case 'z': v = 26; break;
                    case 'A': v =  1; break;
                    case 'B': v =  2; break;
                    case 'C': v =  3; break;
                    case 'D': v =  4; break;
                    case 'E': v =  5; break;
                    case 'F': v =  6; break;
                    case 'G': v =  7; break;
                    case 'H': v =  8; break;
                    case 'I': v =  9; break;
                    case 'J': v = 10; break;
                    case 'K': v = 11; break;
                    case 'L': v = 12; break;
                    case 'M': v = 13; break;
                    case 'N': v = 14; break;
                    case 'O': v = 15; break;
                    case 'P': v = 16; break;
                    case 'Q': v = 17; break;
                    case 'R': v = 18; break;
                    case 'S': v = 19; break;
                    case 'T': v = 20; break;
                    case 'U': v = 21; break;
                    case 'V': v = 22; break;
                    case 'W': v = 23; break;
                    case 'X': v = 24; break;
                    case 'Y': v = 25; break;
                    case 'Z': v = 26; break;
                    case '0': v = 27; break;
                    case '1': v = 28; break;
                    case '2': v = 29; break;
                    case '3': v = 30; break;
                    case '4': v = 31; break;
                    case '5': v = 32; break;
                    case '6': v = 33; break;
                    case '7': v = 34; break;
                    case '8': v = 35; break;
                    case '9': v = 36; break;
                    case '-': v = 37; break;
                    case '(': v = 38; break;
                    case ')': v = 39; break;
                    default: return -1;
                }

                rawlist[ridx++] = v;
                if (ch == '-' && start != i) {
                    start = ++i;
                    break;
                }
            }

            int hash = hash(name, begin, i);
            /*
            We skip this try for now, because the issue doesn't occur
            with Unicode 9.0 ucnhash.dat bundled with Jython.
            Anyway, this might point to some subtle bug.
            Todo: Investigate

            int hash;
            // Currently needed if with older Unicode a
            // name containing '(' or ')' is searched:
            try {
                hash = hash(name, begin, i);
            } catch (ArrayIndexOutOfBoundsException aexc) {
                return -1;
            }
            */

            // if (debug) System.out.println(name.substring(begin, i) + " " + hash);

            if (hash >= 0 && ridx - rbegin > 1 &&
                    match(hash, rawlist, rbegin, ridx)) {
                // if (debug) System.out.println("match " + getWord(hash));
                hash += wordstart;
                ridx = rstart;
                if (hash > wordcutoff) {
                    rawlist[ridx++] = (byte) ((hash >> 8) + wordcutoff);
                    rawlist[ridx++] = (byte) (hash & 0xFF);
                } else {
                    rawlist[ridx++] = (byte) hash;
                }
                rstart = ridx;
                if (i >= end) {
                    break;
                }
            } else {
                rstart = ridx;
                if (i >= end) {
                    break;
                }
                rawlist[ridx++] = 0;
            }
        }

        /*
        if (debug) {
            System.out.print("rawdata: ");
            for (int k = 0; k < ridx; k++)
                System.out.print((rawlist[k] & 0xFF) + " ");
            System.out.println();
        } */

        int idx = binarysearch(rawlist, 0, ridx);
        if (idx < 0) {
            return idx;
        }

        /*
        if (debug) {
            System.out.println("idx:" + idx);
            System.out.println("codepoint:" + codepoint[idx] + " " +
                               Integer.toHexString(codepoint[idx]));
        } */

        return codepoint[idx];
    }

    // From the ucnhashAPI interface
    public int getCchMax() {
        if (!initialized())
           return -1;
        return maxklen;
    }


    private static String cjkPrefix = "CJK COMPATIBILITY IDEOGRAPH-";
    private static int cjkPrefixLen = cjkPrefix.length();

    // From the ucnhashAPI interface
    public int getValue(String s, int start, int end) {
        if (!initialized()) {
            return -1;
        }

        if (s.regionMatches(start, cjkPrefix, 0, cjkPrefixLen)) {
            try {
                String hex = s.substring(start + cjkPrefixLen, end);
                int v = Integer.parseInt(hex, 16);
                return v;
            } catch (NumberFormatException exc) {
                return -1; // Maybe fall through to the main algorithm.
            }
        }
        return lookup(s, start, end);
    }


    private static boolean initialized = false;
    private static boolean loaded = false;

    private synchronized boolean initialized() {
        if (initialized) {
            return loaded;
        }
        try {
            loadTables();
            loaded = true;
        } catch (Exception exc) {
            return false;
        }
        initialized = true;
        return true;
    }


    /*
    public static int lookupChar(char ch) {
        int v;
        if (ch >= 'a' && ch <= 'z')
            ch = (char) (ch  - 'a' + 'A');
        if (ch >= 'A' && ch <= 'Z')
            v = ch - 'A' + 1;
        else if (ch >= '0' && ch <= '9')
            v = ch - '0' + 27;
        else {
            switch (ch) {
                case '-': v = 37; break;
                case '(': v = 38; break;
                case ')': v = 39; break;
                default: return -1;
            }
        }
        return v;
    }
    
    public static void printCharCases() {
        char[] charmapFull =
                " abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-()"
                .toCharArray();
        for (char ch: charmapFull) {
            System.out.println("case \'"+ch+"\': v = "+lookupChar(ch)+"; break;");
        }
    } */

    /*
    private static boolean debug = false;
    public static void main(String[] args) throws Exception {
        loadTables();

        debug = true;

        System.out.println(getWord(hash("ARABIC")));
        System.out.println(getWord(hash("SMALL")));
        System.out.println(getWord(hash("YI")));
        System.out.println(getWord(hash("SYLLABLE")));
        System.out.println(getWord(hash("WITH")));
        System.out.println(getWord(hash("LETTER")));
        
        System.out.println(lookup("NULL")); // 0
        System.out.println(lookup("LATIN CAPITAL LETTER AFRICAN D")); // 393
        System.out.println(lookup("DOUBLE-STRUCK ITALIC SMALL D")); // 8518
        System.out.println(lookup("GURMUKHI TIPPI")); // 2672
        System.out.println(lookup("TIBETAN MARK GTER YIG MGO -UM" +
                " RNAM BCAD MA")); // 3842
        System.out.println(lookup("HANGUL CHOSEONG PIEUP")); // 4359
        System.out.println(lookup("SINGLE LOW-9 QUOTATION MARK")); // 8218
        
        System.out.println(lookup("BACKSPACE")); // 8
        System.out.println(lookup("ACTIVATE SYMMETRIC SWAPPING")); // 8299
        
        System.out.println(lookup("LATIN CAPITAL LETTER A")); // 65
        System.out.println(lookup("GREATER-THAN SIGN")); // 62
        System.out.println(lookup("EURO-CURRENCY SIGN")); // 8352
        System.out.println(lookup("FORM FEED (FF)")); // 12
        System.out.println(lookup("FORM FEED (F")); // -1
    } */
}
