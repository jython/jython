/*
 * Copyright 1999 Finn Bock.
 *
 * This program contains material copyrighted by:
 * Copyright 1991-1995 by Stichting Mathematisch Centrum, Amsterdam,
 * The Netherlands.
 */

package org.python.modules;

import java.io.*;
import org.python.core.*;

/**
 * This module performs conversions between Python values and C
 * structs represented as Python strings.  It uses <i>format strings</i>
 * (explained below) as compact descriptions of the lay-out of the C
 * structs and the intended conversion to/from Python values.
 *
 * <P>
 * The module defines the following exception and functions:
 *
 * <P>
 * <dl><dt><b><tt>error</tt></b>
 * <dd>
 *   Exception raised on various occasions; argument is a string
 *   describing what is wrong.
 * </dl>
 *
 * <P>
 * <dl><dt><b><tt>pack</tt></b> (<var>fmt, v1, v2,  ...</var>)
 * <dd>
 *   Return a string containing the values
 *   <tt><i>v1</i>, <i>v2</i>,  ...</tt> packed according to the given
 *   format.  The arguments must match the values required by the format
 *   exactly.
 * </dl>
 *
 * <P>
 * <dl><dt><b><tt>unpack</tt>></b> (<var>fmt, string</var>)
 * <dd>
 *   Unpack the string (presumably packed by <tt>pack(<i>fmt</i>,
 *    ...)</tt>) according to the given format.  The result is a
 *   tuple even if it contains exactly one item.  The string must contain
 *   exactly the amount of data required by the format (i.e.
 *   <tt>len(<i>string</i>)</tt> must equal <tt>calcsize(<i>fmt</i>)</tt>).
 * </dl>
 *
 * <P>
 * <dl><dt><b><tt>calcsize</tt></b> (<var>fmt</var>)
 * <dd>
 *   Return the size of the struct (and hence of the string)
 *   corresponding to the given format.
 * </dl>
 *
 * <P>
 * Format characters have the following meaning; the conversion between
 * C and Python values should be obvious given their types:
 *
 * <P>
 * <table border align=center>
 *   <tr><th><b>Format</b></th>
 *       <th align=left><b>C Type</b></th>
 *       <th align=left><b>Python</b></th>
 *   <tr><td align=center><samp>x</samp></td>
 *       <td>pad byte</td>
 *       <td>no value</td>
 *   <tr><td align=center><samp>c</samp></td>
 *       <td><tt>char</tt></td>
 *       <td>string of length 1</td>
 *   <tr><td align=center><samp>b</samp></td>
 *       <td><tt>signed char</tt></td>
 *       <td>integer</td>
 *   <tr><td align=center><samp>B</samp></td>
 *       <td><tt>unsigned char</tt></td>
 *       <td>integer</td>
 *   <tr><td align=center><samp>h</samp></td>
 *       <td><tt>short</tt></td>
 *       <td>integer</td>
 *   <tr><td align=center><samp>H</samp></td>
 *       <td><tt>unsigned short</tt></td>
 *       <td>integer</td>
 *   <tr><td align=center><samp>i</samp></td>
 *       <td><tt>int</tt></td>
 *       <td>integer</td>
 *   <tr><td align=center><samp>I</samp></td>
 *       <td><tt>unsigned int</tt></td>
 *       <td>integer</td>
 *   <tr><td align=center><samp>l</samp></td>
 *       <td><tt>long</tt></td>
 *       <td>integer</td>
 *   <tr><td align=center><samp>L</samp></td>
 *       <td><tt>unsigned long</tt></td>
 *       <td>integer</td>
 *   <tr><td align=center><samp>f</samp></td>
 *       <td><tt>float</tt></td>
 *       <td>float</td>
 *   <tr><td align=center><samp>d</samp></td>
 *       <td><tt>double</tt></td>
 *       <td>float</td>
 *   <tr><td align=center><samp>s</samp></td>
 *       <td><tt>char[]</tt></td>
 *       <td>string</td>
 *   <tr><td align=center><samp>p</samp></td>
 *       <td><tt>char[]</tt></td>
 *       <td>string</td>
 * </table>
 *
 * <P>
 * A format character may be preceded by an integral repeat count;
 * e.g. the format string <tt>'4h'</tt> means exactly the same as
 * <tt>'hhhh'</tt>.
 *
 * <P>
 * Whitespace characters between formats are ignored; a count and its
 * format must not contain whitespace though.
 *
 * <P>
 * For the "<tt>s</tt>" format character, the count is interpreted as the
 * size of the string, not a repeat count like for the other format
 * characters; e.g. <tt>'10s'</tt> means a single 10-byte string, while
 * <tt>'10c'</tt> means 10 characters.  For packing, the string is
 * truncated or padded with null bytes as appropriate to make it fit.
 * For unpacking, the resulting string always has exactly the specified
 * number of bytes.  As a special case, <tt>'0s'</tt> means a single, empty
 * string (while <tt>'0c'</tt> means 0 characters).
 *
 * <P>
 * The "<tt>p</tt>" format character can be used to encode a Pascal
 * string.  The first byte is the length of the stored string, with the
 * bytes of the string following.  If count is given, it is used as the
 * total number of bytes used, including the length byte.  If the string
 * passed in to <tt>pack()</tt> is too long, the stored representation
 * is truncated.  If the string is too short, padding is used to ensure
 * that exactly enough bytes are used to satisfy the count.
 *
 * <P>
 * For the "<tt>I</tt>" and "<tt>L</tt>" format characters, the return
 * value is a Python long integer.
 *
 * <P>
 * By default, C numbers are represented in the machine's native format
 * and byte order, and properly aligned by skipping pad bytes if
 * necessary (according to the rules used by the C compiler).
 *
 * <P>
 * Alternatively, the first character of the format string can be used to
 * indicate the byte order, size and alignment of the packed data,
 * according to the following table:
 *
 * <P>
 * <table border align=center>
 *
 *   <tr><th><b>Character</b></th>
 *       <th align=left><b>Byte order</b></th>
 *       <th align=left><b>Size and alignment</b></th>
 *   <tr><td align=center><samp>@</samp></td>
 *       <td>native</td>
 *       <td>native</td>
 *   <tr><td align=center><samp>=</samp></td>
 *       <td>native</td>
 *       <td>standard</td>
 *   <tr><td align=center><samp>&lt;</samp></td>
 *       <td>little-endian</td>
 *       <td>standard</td>
 *   <tr><td align=center><samp>&gt;</samp></td>
 *       <td>big-endian</td>
 *       <td>standard</td>
 *   <tr><td align=center><samp>!</samp></td>
 *       <td>network (= big-endian)</td>
 *       <td>standard</td>
 *
 * </table>
 *
 * <P>
 * If the first character is not one of these, "<tt>@</tt>" is assumed.
 *
 * <P>
 * Native byte order is big-endian or little-endian, depending on the
 * host system (e.g. Motorola and Sun are big-endian; Intel and DEC are
 * little-endian).
 *
 * <P>
 * Native size and alignment are defined as follows: <tt>short</tt> is
 * 2 bytes; <tt>int</tt> and <tt>long</tt> are 4 bytes; <tt>float</tt>
 * are 4 bytes and <tt>double</tt> are 8 bytes. Native byte order is
 * chosen as big-endian.
 *
 * <P>
 * Standard size and alignment are as follows: no alignment is required
 * for any type (so you have to use pad bytes); <tt>short</tt> is 2 bytes;
 * <tt>int</tt> and <tt>long</tt> are 4 bytes.  <tt>float</tt> and
 * <tt>double</tt> are 32-bit and 64-bit IEEE floating point numbers,
 * respectively.
 *
 * <P>
 * Note the difference between "<tt>@</tt>" and "<tt>=</tt>": both use
 * native byte order, but the size and alignment of the latter is
 * standardized.
 *
 * <P>
 * The form "<tt>!</tt>" is available for those poor souls who claim they
 * can't remember whether network byte order is big-endian or
 * little-endian.
 *
 * <P>
 * There is no way to indicate non-native byte order (i.e. force
 * byte-swapping); use the appropriate choice of "<tt>&lt;</tt>" or
 * "<tt>&gt;</tt>".
 *
 * <P>
 * Examples (all using native byte order, size and alignment, on a
 * big-endian machine):
 *
 * <P>
 * <dl><dd><pre>
 * &gt;&gt;&gt; from struct import *
 * &gt;&gt;&gt; pack('hhl', 1, 2, 3)
 * '\000\001\000\002\000\000\000\003'
 * &gt;&gt;&gt; unpack('hhl', '\000\001\000\002\000\000\000\003')
 * (1, 2, 3)
 * &gt;&gt;&gt; calcsize('hhl')
 * 8
 * &gt;&gt;&gt;
 * </pre></dl>
 *
 * <P>
 * Hint: to align the end of a structure to the alignment requirement of
 * a particular type, end the format with the code for that type with a
 * repeat count of zero, e.g. the format <tt>'llh0l'</tt> specifies two
 * pad bytes at the end, assuming longs are aligned on 4-byte boundaries.
 * This only works when native size and alignment are in effect;
 * standard size and alignment does not enforce any alignment.
 *
 * For the complete documentation on the struct module, please see the
 * "Python Library Reference"
 * <p><hr><p>
 *
 * The module is based on the original structmodule.c except that all
 * mistakes and errors are my own. Original author unknown.
 * <p>
 * @author Finn Bock, bckfnn@pipmail.dknet.dk
 * @version struct.java,v 1.6 1999/04/17 12:04:34 fb Exp
 */
public class struct {

    /**
     * Exception raised on various occasions; argument is a
     * string describing what is wrong.
     */
    public static PyString error = new PyString("struct.error");

    public static String __doc__ =
        "Functions to convert between Python values and C structs.\n" +
        "Python strings are used to hold the data representing the C\n" +
        "struct and also as format strings to describe the layout of\n" +
        "data in the C struct.\n" +
        "\n" +
        "The optional first format char indicates byte ordering and\n" +
        "alignment:\n" +
        " @: native w/native alignment(default)\n" +
        " =: native w/standard alignment\n" +
        " <: little-endian, std. alignment\n" +
        " >: big-endian, std. alignment\n" +
        " !: network, std (same as >)\n" +
        "\n" +
        "The remaining chars indicate types of args and must match\n" +
        "exactly; these can be preceded by a decimal repeat count:\n" +
        " x: pad byte (no data); c:char; b:signed byte; B:unsigned byte;\n" +
        " h:short; H:unsigned short; i:int; I:unsigned int;\n" +
        " l:long; L:unsigned long; f:float; d:double.\n" +
        "Special cases (preceding decimal count indicates length):\n" +
        " s:string (array of char); p: pascal string (w. count byte).\n" +
        "Whitespace between formats is ignored.\n" +
        "\n" +
        "The variable struct.error is an exception raised on errors.";


    static class FormatDef {
        char name;
        int size;
        int alignment;

        FormatDef init(char name, int size, int alignment) {
            this.name = name;
            this.size = size;
            this.alignment = alignment;
            return this;
        }

        void pack(ByteStream buf, PyObject value)  {}

        Object unpack(ByteStream buf) {
            return null;
        }

        int doPack(ByteStream buf, int count, int pos, PyObject[] args) {
            if (pos + count > args.length)
                throw StructError("insufficient arguments to pack");

            int cnt = count;
            while (count-- > 0)
                pack(buf, args[pos++]);
            return cnt;
        }

        void doUnpack(ByteStream buf, int count, PyList list) {
            while (count-- > 0)
                list.append(Py.java2py(unpack(buf)));
        }


        int get_int(PyObject value) {
            try {
                return value.__int__().getValue();
            } catch (PyException ex) {
                throw StructError("required argument is not an integer");
            }
        }

        long get_ulong(PyObject value) {
            if (value instanceof PyLong){
                Object v = value.__tojava__(Long.TYPE);
                if (v == Py.NoConversion)
                    throw Py.OverflowError("long int too long to convert");
                return ((Long) v).longValue();
            } else
                return get_int(value);
        }

        double get_float(PyObject value) {
            if (!(value instanceof PyFloat))
                throw StructError("required argument is not an float");
            return value.__float__().getValue();
        }


        void BEwriteInt(ByteStream buf, int v) {
            buf.writeByte((int)(v >>> 24) & 0xFF);
            buf.writeByte((int)(v >>> 16) & 0xFF);
            buf.writeByte((int)(v >>>  8) & 0xFF);
            buf.writeByte((int)(v >>>  0) & 0xFF);
        }

        void LEwriteInt(ByteStream buf, int v) {
            buf.writeByte((int)(v >>>  0) & 0xFF);
            buf.writeByte((int)(v >>>  8) & 0xFF);
            buf.writeByte((int)(v >>> 16) & 0xFF);
            buf.writeByte((int)(v >>> 24) & 0xFF);
        }

        int BEreadInt(ByteStream buf) {
            int b1 = buf.readByte();
            int b2 = buf.readByte();
            int b3 = buf.readByte();
            int b4 = buf.readByte();
            return ((b1 << 24) + (b2 << 16) + (b3 << 8) + (b4 << 0));
        }

        int LEreadInt(ByteStream buf) {
            int b1 = buf.readByte();
            int b2 = buf.readByte();
            int b3 = buf.readByte();
            int b4 = buf.readByte();
            return ((b1 << 0) + (b2 << 8) + (b3 << 16) + (b4 << 24));
        }
    }


    static class ByteStream {
        char[] data;
        int len;
        int pos;

        ByteStream() {
            data = new char[10];
            len = 0;
            pos = 0;
        }

        ByteStream(String s) {
            int l = s.length();
            data = new char[l];
            s.getChars(0, l, data, 0);
            len = l;
            pos = 0;
        }

        int readByte() {
            return data[pos++] & 0xFF;
        }

        void read(char[] buf, int pos, int len) {
            System.arraycopy(data, this.pos, buf, pos, len);
            this.pos += len;
        }


        String readString(int l) {
            char[] data = new char[l];
            read(data, 0, l);
            return new String(data);
        }


        private void ensureCapacity(int l) {
            if (pos + l >= data.length) {
                char[] b = new char[(pos + l) * 2];
                System.arraycopy(data, 0, b, 0, pos);
                data = b;
            }
        }


        void writeByte(int b) {
            ensureCapacity(1);
            data[pos++] = (char)(b & 0xFF);
        }


        void write(char[] buf, int pos, int len) {
            ensureCapacity(len);
            System.arraycopy(buf, pos, data, this.pos, len);
            this.pos += len;
        }

        void writeString(String s, int pos, int len) {
            char[] data = new char[len];
            s.getChars(pos, len, data, 0);
            write(data, 0, len);
        }


        int skip(int l) {
            pos += l;
            return pos;
        }

        int size() {
            return pos;
        }

        public String toString() {
            return new String(data, 0, pos);
        }
    }


    static class PadFormatDef extends FormatDef {
        int doPack(ByteStream buf, int count, int pos, PyObject[] args) {
            while (count-- > 0)
                buf.writeByte(0);
            return 0;
        }

        void doUnpack(ByteStream buf, int count, PyList list) {
            while (count-- > 0)
                buf.readByte();
        }
    }


    static class StringFormatDef extends FormatDef {
        int doPack(ByteStream buf, int count, int pos, PyObject[] args) {
            PyObject value = args[pos];

            if (!(value instanceof PyString))
                throw StructError("argument for 's' must be a string");

            String s = value.toString();
            int len = s.length();
            buf.writeString(s, 0, Math.min(count, len));
            if (len < count) {
                count -= len;
                for (int i = 0; i < count; i++)
                    buf.writeByte(0);
            }
            return 1;
        }

        void doUnpack(ByteStream buf, int count, PyList list) {
            list.append(Py.newString(buf.readString(count)));
        }
    }


    static class PascalStringFormatDef extends StringFormatDef {
        int doPack(ByteStream buf, int count, int pos, PyObject[] args) {
            PyObject value = args[pos];

            if (!(value instanceof PyString))
                throw StructError("argument for 'p' must be a string");

            buf.writeByte(Math.min(value.toString().length(), count-1));
            return super.doPack(buf, count-1, pos, args);
        }

        void doUnpack(ByteStream buf, int count, PyList list) {
            int n = buf.readByte();
            if (n >= count)
                n = count-1;
            super.doUnpack(buf, n, list);
            buf.skip(Math.max(count-n-1, 0));
        }
    }


    static class CharFormatDef extends FormatDef {
        void pack(ByteStream buf, PyObject value) {
            if (!(value instanceof PyString) || value.__len__() != 1)
                throw StructError("char format require string of length 1");
            buf.writeByte(value.toString().charAt(0));
        }

        Object unpack(ByteStream buf) {
            return Py.newString((char)buf.readByte());
        }
    }


    static class ByteFormatDef extends FormatDef {
        void pack(ByteStream buf, PyObject value) {
            buf.writeByte(get_int(value));
        }

        Object unpack(ByteStream buf) {
            int b = buf.readByte();
            if (b > Byte.MAX_VALUE)
                b -= 0x100;
            return Py.newInteger(b);
        }
    }

    static class UnsignedByteFormatDef extends ByteFormatDef {
        Object unpack(ByteStream buf) {
            return Py.newInteger(buf.readByte());
        }
    }

    static class LEShortFormatDef extends FormatDef {
        void pack(ByteStream buf, PyObject value) {
            int v = get_int(value);
            buf.writeByte(v & 0xFF);
            buf.writeByte((v >> 8) & 0xFF);
        }

        Object unpack(ByteStream buf) {
            int v = buf.readByte() |
                   (buf.readByte() << 8);
            if (v > Short.MAX_VALUE)
                v -= 0x10000 ;
            return Py.newInteger(v);
        }
    }

    static class LEUnsignedShortFormatDef extends LEShortFormatDef {
        Object unpack(ByteStream buf) {
            int v = buf.readByte() |
                   (buf.readByte() << 8);
            return Py.newInteger(v);
        }
    }


    static class BEShortFormatDef extends FormatDef {
        void pack(ByteStream buf, PyObject value) {
            int v = get_int(value);
            buf.writeByte((v >> 8) & 0xFF);
            buf.writeByte(v & 0xFF);
        }

        Object unpack(ByteStream buf) {
            int v = (buf.readByte() << 8) |
                     buf.readByte();
            if (v > Short.MAX_VALUE)
                v -= 0x10000;
            return Py.newInteger(v);
        }
    }


    static class BEUnsignedShortFormatDef extends BEShortFormatDef {
        Object unpack(ByteStream buf) {
            int v = (buf.readByte() << 8) |
                     buf.readByte();
            return Py.newInteger(v);
        }
    }


    static class LEIntFormatDef extends FormatDef {
        void pack(ByteStream buf, PyObject value) {
            LEwriteInt(buf, get_int(value));
        }

        Object unpack(ByteStream buf) {
            int v = LEreadInt(buf);
            return Py.newInteger(v);
        }
    }


    static class LEUnsignedIntFormatDef extends FormatDef {
        void pack(ByteStream buf, PyObject value) {
            LEwriteInt(buf, (int)(get_ulong(value) & 0xFFFFFFFF));
        }

        Object unpack(ByteStream buf) {
            long v = LEreadInt(buf);
            if (v < 0)
                v += 0x100000000L;
            return new PyLong(v);
        }
    }


    static class BEIntFormatDef extends FormatDef {
        void pack(ByteStream buf, PyObject value) {
            BEwriteInt(buf, get_int(value));
        }

        Object unpack(ByteStream buf) {
            return Py.newInteger(BEreadInt(buf));
        }
    }


    static class BEUnsignedIntFormatDef extends FormatDef {
        void pack(ByteStream buf, PyObject value) {
            BEwriteInt(buf, (int)(get_ulong(value) & 0xFFFFFFFF));
        }
        Object unpack(ByteStream buf) {
            long v = BEreadInt(buf);
            if (v < 0)
                v += 0x100000000L;
            return new PyLong(v);
        }
    }

    static class LEUnsignedLongFormatDef extends FormatDef {
        void pack(ByteStream buf, PyObject value) {
            long lvalue  = get_ulong( value );
            if (lvalue < 0) {
                throw StructError("can't convert negative long to unsigned");
            }
            int high    = (int) ( (lvalue & 0xFFFFFFFF00000000L)>>32 );
            int low     = (int) ( lvalue & 0x00000000FFFFFFFFL );
            LEwriteInt( buf, low );
            LEwriteInt( buf, high );
        }

        Object unpack(ByteStream buf) {
            long low       = ( LEreadInt( buf ) & 0X00000000FFFFFFFFL );
            long high      = ( LEreadInt( buf ) & 0X00000000FFFFFFFFL );
            java.math.BigInteger result=java.math.BigInteger.valueOf(high);
            result=result.multiply(java.math.BigInteger.valueOf(0x100000000L));
            result=result.add(java.math.BigInteger.valueOf(low));
            return new PyLong(result);
        }
    }


    static class BEUnsignedLongFormatDef extends FormatDef {
        void pack(ByteStream buf, PyObject value) {
            long lvalue  = get_ulong( value );
            if (lvalue < 0) {
                throw StructError("can't convert negative long to unsigned");
            }
            int high    = (int) ( (lvalue & 0xFFFFFFFF00000000L)>>32 );
            int low     = (int) ( lvalue & 0x00000000FFFFFFFFL );
            BEwriteInt( buf, high );
            BEwriteInt( buf, low );
        }

        Object unpack(ByteStream buf) {
            long high      = ( BEreadInt( buf ) & 0X00000000FFFFFFFFL );
            long low       = ( BEreadInt( buf ) & 0X00000000FFFFFFFFL );
            java.math.BigInteger result=java.math.BigInteger.valueOf(high);
            result=result.multiply(java.math.BigInteger.valueOf(0x100000000L));
            result=result.add(java.math.BigInteger.valueOf(low));
            return new PyLong(result);
        }
    }


    static class LELongFormatDef extends FormatDef {
        void pack(ByteStream buf, PyObject value) {
            long lvalue  = get_ulong( value );
            int high    = (int) ( (lvalue & 0xFFFFFFFF00000000L)>>32 );
            int low     = (int) ( lvalue & 0x00000000FFFFFFFFL );
            LEwriteInt( buf, low );
            LEwriteInt( buf, high );
        }

        Object unpack(ByteStream buf) {
            long low= ( LEreadInt( buf )&(0x00000000FFFFFFFFL) );
            long high= ( (LEreadInt( buf )<<32)&(0xFFFFFFFF00000000L) );
            long result=(high|low);
            return new PyLong(result);
        }
    }


    static class BELongFormatDef extends FormatDef {
        void pack(ByteStream buf, PyObject value) {
            long lvalue  = get_ulong( value );
            int high    = (int) ( (lvalue & 0xFFFFFFFF00000000L)>>32 );
            int low     = (int) ( lvalue & 0x00000000FFFFFFFFL );
            BEwriteInt( buf, high );
            BEwriteInt( buf, low );
        }

        Object unpack(ByteStream buf) {
            long high= ( (BEreadInt( buf )<<32)&(0xFFFFFFFF00000000L) );
            long low= ( BEreadInt( buf )&(0x00000000FFFFFFFFL) );
            long result=(high|low);
            return new PyLong(result);
        }
    }


    static class LEFloatFormatDef extends FormatDef {
        void pack(ByteStream buf, PyObject value) {
            int bits = Float.floatToIntBits((float)get_float(value));
            LEwriteInt(buf, bits);
        }

        Object unpack(ByteStream buf) {
            int v = LEreadInt(buf);
            return Py.newFloat(Float.intBitsToFloat(v));
        }
    }

    static class LEDoubleFormatDef extends FormatDef {
        void pack(ByteStream buf, PyObject value) {
            long bits = Double.doubleToLongBits(get_float(value));
            LEwriteInt(buf, (int)(bits & 0xFFFFFFFF));
            LEwriteInt(buf, (int)(bits >>> 32));
        }

        Object unpack(ByteStream buf) {
            long bits = (LEreadInt(buf) & 0xFFFFFFFFL) +
                        (((long)LEreadInt(buf)) << 32);
            return Py.newFloat(Double.longBitsToDouble(bits));
        }
    }


    static class BEFloatFormatDef extends FormatDef {
        void pack(ByteStream buf, PyObject value) {
            int bits = Float.floatToIntBits((float)get_float(value));
            BEwriteInt(buf, bits);
        }

        Object unpack(ByteStream buf) {
            int v = BEreadInt(buf);
            return Py.newFloat(Float.intBitsToFloat(v));
        }
    }

    static class BEDoubleFormatDef extends FormatDef {
        void pack(ByteStream buf, PyObject value) {
            long bits = Double.doubleToLongBits(get_float(value));
            BEwriteInt(buf, (int)(bits >>> 32));
            BEwriteInt(buf, (int)(bits & 0xFFFFFFFF));
        }

        Object unpack(ByteStream buf) {
            long bits = (((long)BEreadInt(buf)) << 32) +
                        (BEreadInt(buf) & 0xFFFFFFFFL);
            return Py.newFloat(Double.longBitsToDouble(bits));
        }
    }


    private static FormatDef[] lilendian_table = {
        new PadFormatDef()              .init('x', 1, 0),
        new ByteFormatDef()             .init('b', 1, 0),
        new UnsignedByteFormatDef()     .init('B', 1, 0),
        new CharFormatDef()             .init('c', 1, 0),
        new StringFormatDef()           .init('s', 1, 0),
        new PascalStringFormatDef()     .init('p', 1, 0),
        new LEShortFormatDef()          .init('h', 2, 0),
        new LEUnsignedShortFormatDef()  .init('H', 2, 0),
        new LEIntFormatDef()            .init('i', 4, 0),
        new LEUnsignedIntFormatDef()    .init('I', 4, 0),
        new LEIntFormatDef()            .init('l', 4, 0),
        new LEUnsignedIntFormatDef()    .init('L', 4, 0),
        new LELongFormatDef()           .init('q', 8, 8),
        new LEUnsignedLongFormatDef()   .init('Q', 8, 8),
        new LEFloatFormatDef()          .init('f', 4, 0),
        new LEDoubleFormatDef()         .init('d', 8, 0),
    };

    private static FormatDef[] bigendian_table = {
        new PadFormatDef()              .init('x', 1, 0),
        new ByteFormatDef()             .init('b', 1, 0),
        new UnsignedByteFormatDef()     .init('B', 1, 0),
        new CharFormatDef()             .init('c', 1, 0),
        new StringFormatDef()           .init('s', 1, 0),
        new PascalStringFormatDef()     .init('p', 1, 0),
        new BEShortFormatDef()          .init('h', 2, 0),
        new BEUnsignedShortFormatDef()  .init('H', 2, 0),
        new BEIntFormatDef()            .init('i', 4, 0),
        new BEUnsignedIntFormatDef()    .init('I', 4, 0),
        new BEIntFormatDef()            .init('l', 4, 0),
        new BEUnsignedIntFormatDef()    .init('L', 4, 0),
        new BELongFormatDef()           .init('q', 8, 8),
        new BEUnsignedLongFormatDef()   .init('Q', 8, 8),
        new BEFloatFormatDef()          .init('f', 4, 0),
        new BEDoubleFormatDef()         .init('d', 8, 0),
    };

    private static FormatDef[] native_table = {
        new PadFormatDef()              .init('x', 1, 0),
        new ByteFormatDef()             .init('b', 1, 0),
        new UnsignedByteFormatDef()     .init('B', 1, 0),
        new CharFormatDef()             .init('c', 1, 0),
        new StringFormatDef()           .init('s', 1, 0),
        new PascalStringFormatDef()     .init('p', 1, 0),
        new BEShortFormatDef()          .init('h', 2, 2),
        new BEUnsignedShortFormatDef()  .init('H', 2, 2),
        new BEIntFormatDef()            .init('i', 4, 4),
        new BEUnsignedIntFormatDef()    .init('I', 4, 4),
        new BEIntFormatDef()            .init('l', 4, 4),
        new BEUnsignedIntFormatDef()    .init('L', 4, 4),
        new BELongFormatDef()           .init('q', 8, 8),
        new BEUnsignedLongFormatDef()   .init('Q', 8, 8),
        new BEFloatFormatDef()          .init('f', 4, 4),
        new BEDoubleFormatDef()         .init('d', 8, 8),
    };



    private static FormatDef[] whichtable(String pfmt) {
        char c = pfmt.charAt(0);
        switch (c) {
        case '<' :
            return lilendian_table;
        case '>':
        case '!':
            // Network byte order is big-endian
            return bigendian_table;
        case '=':
            return bigendian_table;
        case '@':
        default:
            return native_table;
        }
    }


    private static FormatDef getentry(char c, FormatDef[] f) {
        for (int i = 0; i < f.length; i++) {
            if (f[i].name == c)
                return f[i];
        }
        throw StructError("bad char in struct format");
    }



    private static int align(int size, FormatDef e) {
        if (e.alignment != 0) {
            size = ((size + e.alignment - 1)
                                / e.alignment)
                                * e.alignment;
        }
        return size;
    }



    private static int calcsize(String format, FormatDef[] f) {
        int size = 0;

        int len = format.length();
        for (int j = 0; j < len; j++) {
            char c = format.charAt(j);
            if (j == 0 && (c=='@' || c=='<' || c=='>' || c=='=' || c=='!'))
                continue;
            if (Character.isWhitespace(c))
                continue;
            int num = 1;
            if (Character.isDigit(c)) {
                num = Character.digit(c, 10);
                while (++j < len &&
                          Character.isDigit((c = format.charAt(j)))) {
                    int x = num*10 + Character.digit(c, 10);
                    if (x/10 != num)
                        throw StructError("overflow in item count");
                    num = x;
                }
                if (j >= len)
                    break;
            }

            FormatDef e = getentry(c, f);

            int itemsize = e.size;
            size = align(size, e);
            int x = num * itemsize;
            size += x;
            if (x/itemsize != num || size < 0)
                throw StructError("total struct size too long");
        }
        return size;
    }


    /**
     * Return the size of the struct (and hence of the string)
     * corresponding to the given format.
     */
    static public int calcsize(String format) {
        FormatDef[] f = whichtable(format);
        return calcsize(format, f);
    }


    /**
     * Return a string containing the values v1, v2, ... packed according
     * to the given format. The arguments must match the
     * values required by the format exactly.
     */
    static public String pack(PyObject[] args) {
        if (args.length < 1)
            Py.TypeError("illegal argument type for built-in operation");

        String format = args[0].toString();

        FormatDef[] f = whichtable(format);
        int size = calcsize(format, f);

        ByteStream res = new ByteStream();

        int i = 1;
        int len = format.length();
        for (int j = 0; j < len; j++) {
            char c = format.charAt(j);
            if (j == 0 && (c=='@' || c=='<' || c=='>' || c=='=' || c=='!'))
                continue;
            if (Character.isWhitespace(c))
                continue;
            int num = 1;
            if (Character.isDigit(c)) {
                num = Character.digit(c, 10);
                while (++j < len && Character.isDigit((c = format.charAt(j))))
                    num = num*10 + Character.digit(c, 10);
                if (j >= len)
                    break;
            }

            FormatDef e = getentry(c, f);

            // Fill padd bytes with zeros
            int nres = align(res.size(), e) - res.size();
            while (nres-- > 0)
                res.writeByte(0);
            i += e.doPack(res, num, i, args);
        }

        if (i < args.length)
            throw StructError("too many arguments for pack format");

        return res.toString();
    }



    /**
     * Unpack the string (presumably packed by pack(fmt, ...)) according
     * to the given format. The result is a tuple even if it contains
     * exactly one item.
     * The string must contain exactly the amount of data required by
     * the format (i.e. len(string) must equal calcsize(fmt)).
     */
    public static PyTuple unpack(String format, String string) {
        int len = string.length();

        FormatDef[] f = whichtable(format);
        int size = calcsize(format, f);

        if (size != len)
            throw StructError("unpack str size does not match format");

        PyList res = new PyList();

        ByteStream str = new ByteStream(string);

        int flen = format.length();
        for (int j = 0; j < flen; j++) {
            char c = format.charAt(j);
            if (j == 0 && (c=='@' || c=='<' || c=='>' || c=='=' || c=='!'))
                continue;
            if (Character.isWhitespace(c))
                continue;
            int num = 1;
            if (Character.isDigit(c)) {
                num = Character.digit(c, 10);
                while (++j < flen &&
                           Character.isDigit((c = format.charAt(j))))
                    num = num*10 + Character.digit(c, 10);
                if (j > flen)
                    break;
            }

            FormatDef e = getentry(c, f);

            str.skip(align(str.size(), e) - str.size());

            e.doUnpack(str, num, res);
        }
        return __builtin__.tuple(res);
    }



    private static PyException StructError(String explanation) {
        return new PyException(error, explanation);
    }
}
