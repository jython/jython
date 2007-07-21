// Copyright 1998 Finn Bock.


package org.python.modules;

import java.io.*;
import org.python.core.*;

public class ucnhash implements ucnhashAPI {

    // Parameters for the word hash.
    private static int n;
    private static int m;
    private static int minchar;
    private static int maxchar;
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
    private static char[] rawindex;

    // The mapping from raw data index to unicode code points.
    private static char[] codepoint;




    public static String[] __depends__ = new String[] {
        "/org/python/modules/ucnhash.dat",
    };


    public static void loadTables() throws Exception {
        InputStream instream = ucnhash.class.
                                    getResourceAsStream("ucnhash.dat");
        if (instream == null)
            throw new IOException("Unicode name database not found: " +
                                  "ucnhash.dat");

        DataInputStream in = new DataInputStream(
                                 new BufferedInputStream(instream));

        n = in.readShort();
        m = in.readShort();
        minchar= in.readShort();
        maxchar = in.readShort();
        alphasz = in.readShort();
        maxlen = in.readShort();
        maxidx = maxlen*alphasz-minchar;

        G = readShortTable(in);
        if (in.readShort() != 3)
            throw new IOException("UnicodeNameMap file corrupt, " +
                                  "unknown dimension");

        T0 = readShortTable(in);
        T1 = readShortTable(in);
        T2 = readShortTable(in);

        wordoffs = readShortTable(in);
        worddata = readByteTable(in);

        wordstart  = in.readShort();
        wordcutoff = in.readShort();
        maxklen = in.readShort();

        rawdata = readByteTable(in);
        rawindex = readCharTable(in);
        codepoint = readCharTable(in);
    }


    private static short[] readShortTable(DataInputStream in)
        throws IOException
    {
        if (in.read() != 't')
            throw new IOException("UnicodeNameMap file corrupt, shorttable");

        int n = in.readUnsignedShort() / 2;
        short[] table = new short[n];
        for (int i = 0; i < n; i++) {
            table[i] = in.readShort();
        }
        return table;
    }

    private static char[] readCharTable(DataInputStream in)
        throws IOException
    {
        if (in.read() != 't')
            throw new IOException("UnicodeNameMap file corrupt, chartable");

        int n = in.readUnsignedShort() / 2;
        char[] table = new char[n];
        for (int i = 0; i < n; i++) {
            table[i] = in.readChar();
        }
        return table;
    }

    private static byte[] readByteTable(DataInputStream in)
        throws IOException
    {
        if (in.read() != 't')
            throw new IOException("UnicodeNameMap file corrupt, byte table");
        int n = in.readUnsignedShort();
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
           " ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-".toCharArray();

    private static String getWord(int idx) {
       int offset = wordoffs[idx];
       int end = worddata.length;
       if (idx < wordoffs.length-1)
           end = wordoffs[idx+1];
       StringBuffer buf = new StringBuffer();
       for (int i = offset; i < end; i++)
           buf.append(charmap[worddata[i]]);
       return buf.toString();
    }


    private static boolean match(int idx, byte[] raw, int begin, int end) {
       int woff = wordoffs[idx];
       int wend = worddata.length;
       if (idx < wordoffs.length-1)
           wend = wordoffs[idx+1];

       if (end-begin != wend - woff)
           return false;
       int l = end-begin;
       for (int i = 0; i < l; i++) {
           if (worddata[woff + i] != raw[begin + i])
               return false;
       }
       return true;
    }



    private static int compare(byte[] a1, int off1, int len1,
                               byte[] a2, int off2, int len2)
    {
        for (int i = 0; i < len1 && i < len2; i++) {
            int d = (a1[off1 + i] & 0xFF) - (a2[off2 + i] & 0xFF);
            if (d != 0)
                return d;
        }
        return len1 - len2;
    }


    private static int binarysearch(byte[] rawlist, int start, int end) {
        int floor = 0;
        int ceiling = (rawindex.length) / 5;

        while (floor < ceiling - 1) {
           int middle = (floor + ceiling) / 2;
           if (debug)
               System.out.println("floor:" + floor + " ceiling:" +
                                  ceiling +" => " + middle);

           int off = rawindex[middle*5];
           int len = rawindex[middle*5+4] & 0x1F;
           int d = compare(rawlist, start, end - start, rawdata, off, len);
           if (d < 0)
               ceiling = middle;
           else if (d > 0)
               floor = middle;
           else
               return middle * 12;
        }

        int tmp = floor*5;

        int off = rawindex[tmp++];
        long lengths = ((long) rawindex[tmp++] << 48) |
                       ((long) rawindex[tmp++] << 32) |
                       ((long) rawindex[tmp++] << 16) |
                       rawindex[tmp++];

        floor *= 12;
        for (int i = 0; i < 12; i++) {
            int len = (int) (lengths >> (i * 5)) & 0x1F;
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

        int i;
        while (true) {
            rbegin = ridx;
            int begin = start;
            for (i = start; i < end; i++) {
                char ch = name.charAt(i);
                if (ch == ' ') {
                    start = i+1;
                    break;
                }
                int v;
                if (ch >= 'a' && ch <= 'z')
                    ch = (char) (ch  - 'a' + 'A');
                if (ch >= 'A' && ch <= 'Z')
                    v = ch - 'A' + 1;
                else if (ch >= '0' && ch <= '9')
                    v = ch - '0' + 27;
                else if (ch == '-')
                    v = 37;
                else
                    return -1;

                rawlist[ridx++] = (byte) v;
                if (ch == '-' && start != i) {
                    start = ++i;
                    break;
                }
            }

            int hash = hash(name, begin, i);

            if (debug)
                System.out.println(name.substring(begin, i) + " " + hash);

            boolean isWord = hash >= 0 &&
                             ridx - rbegin > 1 &&
                             match(hash, rawlist, rbegin, ridx);

            if (isWord) {
                if (debug)
                    System.out.println("match " + getWord(hash));
                hash += wordstart;
                ridx = rstart;
                if (hash > wordcutoff) {
                    rawlist[ridx++] = (byte) ((hash >> 8) + wordcutoff);
                    rawlist[ridx++] = (byte) (hash & 0xFF);
                } else
                   rawlist[ridx++] = (byte) hash;
            }
            rstart = ridx;

            if (i >= end)
               break;

            if (!isWord) {
                rawlist[ridx++] = 0;
            }

        }

        if (debug) {
            System.out.print("rawdata: ");
            for (int k = 0; k < ridx; k++)
                System.out.print((rawlist[k] & 0xFF) + " ");
            System.out.println();
        }

        int idx = binarysearch(rawlist, 0, ridx);
        if (idx < 0)
            return idx;
        if (debug) {
            System.out.println("idx:" + idx);
            System.out.println("codepoint:" + codepoint[idx] + " " +
                               Integer.toHexString(codepoint[idx]));
        }
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
        if (!initialized())
            return -1;

        if (s.regionMatches(start, cjkPrefix, 0, cjkPrefixLen)) {
            try {
               String hex = s.substring(start + cjkPrefixLen, end);
               int v = Integer.parseInt(hex, 16);
               return v;
            } catch (NumberFormatException exc) {
               return -1; // Maybe fallthrough to the main algorithme.
            }
        }

        return lookup(s, start, end);
    }


    private static boolean initialized = false;
    private static boolean loaded = false;


    private synchronized boolean initialized() {
        if (initialized && loaded)
            return true;
        if (initialized)
            return false;
        try {
            loadTables();
            loaded = true;
        } catch (Exception exc) {
            return false;
        }
        initialized = true;
        return true;
    }

    private static boolean debug = false;


    public static void main(String[] args) throws Exception {
       loadTables();

       debug = true;

/*
       System.out.println(getWord(hash("ARABIC")));
       System.out.println(getWord(hash("SMALL")));
       System.out.println(getWord(hash("YI")));
       System.out.println(getWord(hash("SYLLABLE")));
       System.out.println(getWord(hash("WITH")));
       System.out.println(getWord(hash("LETTER")));

       System.out.println(lookup("NULL"));
       System.out.println(lookup("LATIN CAPITAL LETTER AFRICAN D"));
       System.out.println(lookup("GURMUKHI TIPPI"));
       System.out.println(lookup("TIBETAN MARK GTER YIG MGO -UM " +
                                 "RNAM BCAD MA"));
       System.out.println(lookup("HANGUL CHOSEONG PIEUP"));
       System.out.println(lookup("SINGLE LOW-9 QUOTATION MARK"));
*/

       System.out.println(lookup("BACKSPACE"));
//       System.out.println(lookup("ACTIVATE SYMMETRIC SWAPPING"));

/*
       System.out.println(lookup("LATIN CAPITAL LETTER A"));
       System.out.println(lookup("GREATER-THAN SIGN"));
       System.out.println(lookup("EURO-CURRENCY SIGN"));
*/
    }
}
