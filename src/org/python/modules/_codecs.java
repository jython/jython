/*
 * Copyright (c)2012 Jython Developers Original Java version copyright 2000 Finn Bock
 *
 * This program contains material copyrighted by: Copyright (c) Corporation for National Research
 * Initiatives. Originally written by Marc-Andre Lemburg (mal@lemburg.com).
 */
package org.python.modules;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Iterator;

import org.python.core.Py;
import org.python.core.PyDictionary;
import org.python.core.PyInteger;
import org.python.core.PyNone;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.core.PySystemState;
import org.python.core.PyTuple;
import org.python.core.PyUnicode;
import org.python.core.codecs;
import org.python.expose.ExposedType;

/**
 * This class corresponds to the Python _codecs module, which in turn lends its functions to the
 * codecs module (in Lib/codecs.py). It exposes the implementing functions of several codec families
 * called out in the Python codecs library Lib/encodings/*.py, where it is usually claimed that they
 * are bound "as C functions". Obviously, C stands for "compiled" in this context, rather than
 * dependence on a particular implementation language. Actual transcoding methods often come from
 * the related {@link codecs} class.
 */
public class _codecs {

    public static void register(PyObject search_function) {
        codecs.register(search_function);
    }

    public static PyTuple lookup(String encoding) {
        return codecs.lookup(encoding);
    }

    public static PyObject lookup_error(String handlerName) {
        return codecs.lookup_error(handlerName);
    }

    public static void register_error(String name, PyObject errorHandler) {
        codecs.register_error(name, errorHandler);
    }

    public static PyObject charmap_build(PyUnicode map) {
        return EncodingMap.buildEncodingMap(map);
    }

    private static PyTuple decode_tuple(String s, int len) {
        return new PyTuple(new PyUnicode(s), Py.newInteger(len));
    }

    private static PyTuple decode_tuple_str(String s, int len) {
        return new PyTuple(new PyString(s), Py.newInteger(len));
    }

    private static PyTuple encode_tuple(String s, int len) {
        return new PyTuple(new PyString(s), Py.newInteger(len));
    }

    /* --- UTF-8 Codec --------------------------------------------------- */
    public static PyTuple utf_8_decode(String str) {
        return utf_8_decode(str, null);
    }

    public static PyTuple utf_8_decode(String str, String errors) {
        return utf_8_decode(str, errors, false);
    }

    public static PyTuple utf_8_decode(String str, String errors, boolean final_) {
        int[] consumed = final_ ? null : new int[1];
        return decode_tuple(codecs.PyUnicode_DecodeUTF8Stateful(str, errors, consumed), final_
                ? str.length() : consumed[0]);
    }

    public static PyTuple utf_8_encode(String str) {
        return utf_8_encode(str, null);
    }

    public static PyTuple utf_8_encode(String str, String errors) {
        int size = str.length();
        return encode_tuple(codecs.PyUnicode_EncodeUTF8(str, errors), size);
    }

    /* --- UTF-7 Codec --------------------------------------------------- */
    public static PyTuple utf_7_decode(String str) {
        return utf_7_decode(str, null);
    }

    public static PyTuple utf_7_decode(String str, String errors) {
        int size = str.length();
        return decode_tuple(codecs.PyUnicode_DecodeUTF7(str, errors), size);
    }

    public static PyTuple utf_7_encode(String str) {
        return utf_7_encode(str, null);
    }

    public static PyTuple utf_7_encode(String str, String errors) {
        int size = str.length();
        return encode_tuple(codecs.PyUnicode_EncodeUTF7(str, false, false, errors), size);
    }

    public static PyTuple escape_decode(String str) {
        return escape_decode(str, null);
    }

    public static PyTuple escape_decode(String str, String errors) {
        return decode_tuple_str(PyString.decode_UnicodeEscape(str, 0, str.length(), errors, true),
                str.length());
    }

    public static PyTuple escape_encode(String str) {
        return escape_encode(str, null);
    }

    public static PyTuple escape_encode(String str, String errors) {
        return encode_tuple(PyString.encode_UnicodeEscape(str, false), str.length());
    }

    /* --- Character Mapping Codec --------------------------------------- */

    /**
     * Equivalent to <code>charmap_decode(bytes, errors, null)</code>. This method is here so the
     * error and mapping arguments can be optional at the Python level.
     *
     * @param bytes sequence of bytes to decode
     * @return decoded string and number of bytes consumed
     */
    public static PyTuple charmap_decode(String bytes) {
        return charmap_decode(bytes, null, null);
    }

    /**
     * Equivalent to <code>charmap_decode(bytes, errors, null)</code>. This method is here so the
     * error argument can be optional at the Python level.
     *
     * @param bytes sequence of bytes to decode
     * @param errors error policy
     * @return decoded string and number of bytes consumed
     */
    public static PyTuple charmap_decode(String bytes, String errors) {
        return charmap_decode(bytes, errors, null);
    }

    /**
     * Decode a sequence of bytes into Unicode characters via a mapping supplied as a container to
     * be indexed by the byte values (as unsigned integers). If the mapping is null or None, decode
     * with latin-1 (essentially treating bytes as character codes directly).
     *
     * @param bytes sequence of bytes to decode
     * @param errors error policy
     * @param mapping to convert bytes to characters
     * @return decoded string and number of bytes consumed
     */
    public static PyTuple charmap_decode(String str, String errors, PyObject mapping) {
        if (mapping == null || mapping == Py.None) {
            // Default to Latin-1
            return latin_1_decode(str, errors);
        } else {
            return charmap_decode(str, errors, mapping, false);
        }
    }

    /**
     * Decode a sequence of bytes into Unicode characters via a mapping supplied as a container to
     * be indexed by the byte values (as unsigned integers).
     *
     * @param bytes sequence of bytes to decode
     * @param errors error policy
     * @param mapping to convert bytes to characters
     * @param ignoreUnmapped if true, pass unmapped byte values as character codes [0..256)
     * @return decoded string and number of bytes consumed
     */
    public static PyTuple charmap_decode(String bytes, String errors, PyObject mapping,
            boolean ignoreUnmapped) {
        // XXX bytes: would prefer to accept any object with buffer API
        int size = bytes.length();
        StringBuilder v = new StringBuilder(size);

        for (int i = 0; i < size; i++) {

            // Process the i.th input byte
            int b = bytes.charAt(i);
            if (b > 0xff) {
                i = codecs.insertReplacementAndGetResume(v, errors, "charmap", bytes, //
                        i, i + 1, "ordinal not in range(255)") - 1;
                continue;
            }

            // Map the byte to an output character code (or possibly string)
            PyObject w = Py.newInteger(b);
            PyObject x = mapping.__finditem__(w);

            // Apply to the output
            if (x == null) {
                // Error case: mapping not found
                if (ignoreUnmapped) {
                    v.appendCodePoint(b);
                } else {
                    i = codecs.insertReplacementAndGetResume(v, errors, "charmap", bytes, //
                            i, i + 1, "no mapping found") - 1;
                }

            } else if (x instanceof PyInteger) {
                // Mapping was to an int: treat as character code
                int value = ((PyInteger)x).getValue();
                if (value < 0 || value > PySystemState.maxunicode) {
                    throw Py.TypeError("character mapping must return "
                            + "integer greater than 0 and less than sys.maxunicode");
                }
                v.appendCodePoint(value);

            } else if (x == Py.None) {
                i = codecs.insertReplacementAndGetResume(v, errors, "charmap", bytes, //
                        i, i + 1, "character maps to <undefined>") - 1;

            } else if (x instanceof PyString) {
                String s = x.toString();
                if (s.charAt(0) == 0xfffe) {
                    // Invalid indicates "undefined" see C-API PyUnicode_DecodeCharmap()
                    i = codecs.insertReplacementAndGetResume(v, errors, "charmap", bytes, //
                            i, i + 1, "character maps to <undefined>") - 1;
                } else {
                    v.append(s);
                }

            } else {
                /* wrong return value */
                throw Py.TypeError("character mapping must return " + "integer, None or str");
            }
        }

        return decode_tuple(v.toString(), size);
    }

    // parallel to CPython's PyUnicode_TranslateCharmap
    public static PyObject translateCharmap(PyUnicode str, String errors, PyObject mapping) {
        StringBuilder buf = new StringBuilder(str.toString().length());

        for (Iterator<Integer> iter = str.newSubsequenceIterator(); iter.hasNext();) {
            int codePoint = iter.next();
            PyObject result = mapping.__finditem__(Py.newInteger(codePoint));
            if (result == null) {
                // No mapping found means: use 1:1 mapping
                buf.appendCodePoint(codePoint);
            } else if (result == Py.None) {
                // XXX: We don't support the fancier error handling CPython does here of
                // capturing regions of chars removed by the None mapping to optionally
                // pass to an error handler. Though we don't seem to even use this
                // functionality anywhere either
                ;
            } else if (result instanceof PyInteger) {
                int value = result.asInt();
                if (value < 0 || value > PySystemState.maxunicode) {
                    throw Py.TypeError(String.format("character mapping must be in range(0x%x)",
                            PySystemState.maxunicode + 1));
                }
                buf.appendCodePoint(value);
            } else if (result instanceof PyUnicode) {
                buf.append(result.toString());
            } else {
                // wrong return value
                throw Py.TypeError("character mapping must return integer, None or unicode");
            }
        }
        return new PyUnicode(buf.toString());
    }

    /**
     * Equivalent to <code>charmap_encode(str, null, null)</code>. This method is here so the error
     * and mapping arguments can be optional at the Python level.
     *
     * @param str to be encoded
     * @return (encoded data, size(str)) as a pair
     */
    public static PyTuple charmap_encode(String str) {
        return charmap_encode(str, null, null);
    }

    /**
     * Equivalent to <code>charmap_encode(str, errors, null)</code>. This method is here so the
     * mapping can be optional at the Python level.
     *
     * @param str to be encoded
     * @param errors error policy name (e.g. "ignore")
     * @return (encoded data, size(str)) as a pair
     */
    public static PyTuple charmap_encode(String str, String errors) {
        return charmap_encode(str, errors, null);
    }

    /**
     * Encoder based on an optional character mapping. This mapping is either an
     * <code>EncodingMap</code> of 256 entries, or an arbitrary container indexable with integers
     * using <code>__finditem__</code> and yielding byte strings. If the mapping is null, latin-1
     * (effectively a mapping of character code to the numerically-equal byte) is used
     *
     * @param str to be encoded
     * @param errors error policy name (e.g. "ignore")
     * @param mapping from character code to output byte (or string)
     * @return (encoded data, size(str)) as a pair
     */
    public static PyTuple charmap_encode(String str, String errors, PyObject mapping) {
        if (mapping == null || mapping == Py.None) {
            // Default to Latin-1
            return latin_1_encode(str, errors);
        } else {
            return charmap_encode_internal(str, errors, mapping, new StringBuilder(str.length()),
                    true);
        }
    }

    /**
     * Helper to implement the several variants of <code>charmap_encode</code>, given an optional
     * mapping. This mapping is either an <code>EncodingMap</code> of 256 entries, or an arbitrary
     * container indexable with integers using <code>__finditem__</code> and yielding byte strings.
     *
     * @param str to be encoded
     * @param errors error policy name (e.g. "ignore")
     * @param mapping from character code to output byte (or string)
     * @param v to contain the encoded bytes
     * @param letLookupHandleError
     * @return (encoded data, size(str)) as a pair
     */
    private static PyTuple charmap_encode_internal(String str, String errors, PyObject mapping,
            StringBuilder v, boolean letLookupHandleError) {

        EncodingMap encodingMap = mapping instanceof EncodingMap ? (EncodingMap)mapping : null;
        int size = str.length();

        for (int i = 0; i < size; i++) {

            // Map the i.th character of str to some value
            char ch = str.charAt(i);
            PyObject x;
            if (encodingMap != null) {
                // The mapping given was an EncodingMap [0,256) => on-negative int
                int result = encodingMap.lookup(ch);
                x = (result == -1) ? null : Py.newInteger(result);
            } else {
                // The mapping was a map or similar: non-negative int -> object
                x = mapping.__finditem__(Py.newInteger(ch));
            }

            // And map this object to an output character
            if (x == null) {
                // Error during lookup
                if (letLookupHandleError) {
                    // Some kind of substitute can be placed in the output
                    i = handleBadMapping(str, errors, mapping, v, size, i);
                } else {
                    // Hard error
                    throw Py.UnicodeEncodeError("charmap", str, i, i + 1,
                            "character maps to <undefined>");
                }

            } else if (x instanceof PyInteger) {
                // Look-up had integer result: output as byte value
                int value = ((PyInteger)x).getValue();
                if (value < 0 || value > 255) {
                    throw Py.TypeError("character mapping must be in range(256)");
                }
                v.append((char)value);

            } else if (x instanceof PyString && !(x instanceof PyUnicode)) {
                // Look-up had str or unicode result: output as Java String
                // XXX: (Py3k) Look-up had bytes or str result: output as ... this is a problem
                v.append(x.toString());

            } else if (x instanceof PyNone) {
                i = handleBadMapping(str, errors, mapping, v, size, i);

            } else {
                /* wrong return value */
                throw Py.TypeError("character mapping must return " + "integer, None or str");
            }
        }

        return encode_tuple(v.toString(), size);
    }

    /**
     * Helper for {@link #charmap_encode_internal(String, String, PyObject, StringBuilder, boolean)}
     * called when we need some kind of substitute in the output for an invalid input.
     *
     * @param str to be encoded
     * @param errors error policy name (e.g. "ignore")
     * @param mapping from character code to output byte (or string)
     * @param v to contain the encoded bytes
     * @param size of str
     * @param i index in str of current (and problematic) character
     * @return index of last character of problematic section
     */
    private static int handleBadMapping(String str, String errors, PyObject mapping,
            StringBuilder v, int size, int i) {

        // If error policy specified, execute it
        if (errors != null) {

            if (errors.equals(codecs.IGNORE)) {
                return i;

            } else if (errors.equals(codecs.REPLACE)) {
                String replStr = "?";
                charmap_encode_internal(replStr, errors, mapping, v, false);
                return i;

            } else if (errors.equals(codecs.XMLCHARREFREPLACE)) {
                String replStr = codecs.xmlcharrefreplace(i, i + 1, str).toString();
                charmap_encode_internal(replStr, errors, mapping, v, false);
                return i;

            } else if (errors.equals(codecs.BACKSLASHREPLACE)) {
                String replStr = codecs.backslashreplace(i, i + 1, str).toString();
                charmap_encode_internal(replStr, errors, mapping, v, false);
                return i;
            }
        }

        // Default behaviour (error==null or does not match known case)
        String msg = "character maps to <undefined>";
        PyObject replacement = codecs.encoding_error(errors, "charmap", str, i, i + 1, msg);
        String replStr = replacement.__getitem__(0).toString();
        charmap_encode_internal(replStr, errors, mapping, v, false);

        return codecs.calcNewPosition(size, replacement) - 1;
    }

    /* --- ascii Codec ---------------------------------------------- */
    public static PyTuple ascii_decode(String str) {
        return ascii_decode(str, null);
    }

    public static PyTuple ascii_decode(String str, String errors) {
        int size = str.length();
        return decode_tuple(codecs.PyUnicode_DecodeASCII(str, size, errors), size);
    }

    public static PyTuple ascii_encode(String str) {
        return ascii_encode(str, null);
    }

    public static PyTuple ascii_encode(String str, String errors) {
        int size = str.length();
        return encode_tuple(codecs.PyUnicode_EncodeASCII(str, size, errors), size);
    }

    /* --- Latin-1 Codec -------------------------------------------- */
    public static PyTuple latin_1_decode(String str) {
        return latin_1_decode(str, null);
    }

    public static PyTuple latin_1_decode(String str, String errors) {
        int size = str.length();
        return decode_tuple(codecs.PyUnicode_DecodeLatin1(str, size, errors), size);
    }

    public static PyTuple latin_1_encode(String str) {
        return latin_1_encode(str, null);
    }

    public static PyTuple latin_1_encode(String str, String errors) {
        int size = str.length();
        return encode_tuple(codecs.PyUnicode_EncodeLatin1(str, size, errors), size);
    }

    /* --- UTF16 Codec -------------------------------------------- */
    public static PyTuple utf_16_encode(String str) {
        return utf_16_encode(str, null);
    }

    public static PyTuple utf_16_encode(String str, String errors) {
        return encode_tuple(encode_UTF16(str, errors, 0), str.length());
    }

    public static PyTuple utf_16_encode(String str, String errors, int byteorder) {
        return encode_tuple(encode_UTF16(str, errors, byteorder), str.length());
    }

    public static PyTuple utf_16_le_encode(String str) {
        return utf_16_le_encode(str, null);
    }

    public static PyTuple utf_16_le_encode(String str, String errors) {
        return encode_tuple(encode_UTF16(str, errors, -1), str.length());
    }

    public static PyTuple utf_16_be_encode(String str) {
        return utf_16_be_encode(str, null);
    }

    public static PyTuple utf_16_be_encode(String str, String errors) {
        return encode_tuple(encode_UTF16(str, errors, 1), str.length());
    }

    public static String encode_UTF16(String str, String errors, int byteorder) {
        final Charset utf16;
        if (byteorder == 0) {
            utf16 = Charset.forName("UTF-16");
        } else if (byteorder == -1) {
            utf16 = Charset.forName("UTF-16LE");
        } else {
            utf16 = Charset.forName("UTF-16BE");
        }
        final ByteBuffer bbuf = utf16.encode(str);
        final StringBuilder v = new StringBuilder(bbuf.limit());
        while (bbuf.remaining() > 0) {
            int val = bbuf.get();
            if (val < 0) {
                val = 256 + val;
            }
            v.appendCodePoint(val);
        }
        return v.toString();
    }

    public static PyTuple utf_16_decode(String str) {
        return utf_16_decode(str, null);
    }

    public static PyTuple utf_16_decode(String str, String errors) {
        return utf_16_decode(str, errors, false);
    }

    public static PyTuple utf_16_decode(String str, String errors, boolean final_) {
        int[] bo = new int[] {0};
        int[] consumed = final_ ? null : new int[1];
        return decode_tuple(decode_UTF16(str, errors, bo, consumed), final_ ? str.length()
                : consumed[0]);
    }

    public static PyTuple utf_16_le_decode(String str) {
        return utf_16_le_decode(str, null);
    }

    public static PyTuple utf_16_le_decode(String str, String errors) {
        return utf_16_le_decode(str, errors, false);
    }

    public static PyTuple utf_16_le_decode(String str, String errors, boolean final_) {
        int[] bo = new int[] {-1};
        int[] consumed = final_ ? null : new int[1];
        return decode_tuple(decode_UTF16(str, errors, bo, consumed), final_ ? str.length()
                : consumed[0]);
    }

    public static PyTuple utf_16_be_decode(String str) {
        return utf_16_be_decode(str, null);
    }

    public static PyTuple utf_16_be_decode(String str, String errors) {
        return utf_16_be_decode(str, errors, false);
    }

    public static PyTuple utf_16_be_decode(String str, String errors, boolean final_) {
        int[] bo = new int[] {1};
        int[] consumed = final_ ? null : new int[1];
        return decode_tuple(decode_UTF16(str, errors, bo, consumed), final_ ? str.length()
                : consumed[0]);
    }

    public static PyTuple utf_16_ex_decode(String str) {
        return utf_16_ex_decode(str, null);
    }

    public static PyTuple utf_16_ex_decode(String str, String errors) {
        return utf_16_ex_decode(str, errors, 0);
    }

    public static PyTuple utf_16_ex_decode(String str, String errors, int byteorder) {
        return utf_16_ex_decode(str, errors, byteorder, false);
    }

    public static PyTuple
            utf_16_ex_decode(String str, String errors, int byteorder, boolean final_) {
        int[] bo = new int[] {0};
        int[] consumed = final_ ? null : new int[1];
        String decoded = decode_UTF16(str, errors, bo, consumed);
        return new PyTuple(new PyUnicode(decoded), Py.newInteger(final_ ? str.length()
                : consumed[0]), Py.newInteger(bo[0]));
    }

    private static String decode_UTF16(String str, String errors, int[] byteorder) {
        return decode_UTF16(str, errors, byteorder, null);
    }

    private static String decode_UTF16(String str, String errors, int[] byteorder, int[] consumed) {
        int bo = 0;
        if (byteorder != null) {
            bo = byteorder[0];
        }
        int size = str.length();
        StringBuilder v = new StringBuilder(size / 2);
        int i;
        for (i = 0; i < size; i += 2) {
            char ch1 = str.charAt(i);
            if (i + 1 == size) {
                if (consumed != null) {
                    break;
                }
                i = codecs.insertReplacementAndGetResume(v, errors, "utf-16", str, //
                        i, i + 1, "truncated data");
                continue;
            }
            char ch2 = str.charAt(i + 1);
            if (ch1 == 0xFE && ch2 == 0xFF) {
                bo = 1;
                continue;
            } else if (ch1 == 0xFF && ch2 == 0xFE) {
                bo = -1;
                continue;
            }
            int W1;
            if (bo == -1) {
                W1 = (ch2 << 8 | ch1);
            } else {
                W1 = (ch1 << 8 | ch2);
            }

            if (W1 < 0xD800 || W1 > 0xDFFF) {
                v.appendCodePoint(W1);
                continue;
            } else if (W1 >= 0xD800 && W1 <= 0xDBFF && i < size - 1) {
                i += 2;
                char ch3 = str.charAt(i);
                char ch4 = str.charAt(i + 1);
                int W2;
                if (bo == -1) {
                    W2 = (ch4 << 8 | ch3);
                } else {
                    W2 = (ch3 << 8 | ch4);
                }
                if (W2 >= 0xDC00 && W2 <= 0xDFFF) {
                    int U = (((W1 & 0x3FF) << 10) | (W2 & 0x3FF)) + 0x10000;
                    v.appendCodePoint(U);
                    continue;
                }
                i = codecs.insertReplacementAndGetResume(v, errors, "utf-16", str, //
                        i, i + 1, "illegal UTF-16 surrogate");
                continue;
            }

            i = codecs.insertReplacementAndGetResume(v, errors, "utf-16", str, //
                    i, i + 1, "illegal encoding");
        }
        if (byteorder != null) {
            byteorder[0] = bo;
        }
        if (consumed != null) {
            consumed[0] = i;
        }
        return v.toString();
    }

    /* --- RawUnicodeEscape Codec ----------------------------------------- */
    public static PyTuple raw_unicode_escape_encode(String str) {
        return raw_unicode_escape_encode(str, null);
    }

    public static PyTuple raw_unicode_escape_encode(String str, String errors) {
        return encode_tuple(codecs.PyUnicode_EncodeRawUnicodeEscape(str, errors, false),
                str.length());
    }

    public static PyTuple raw_unicode_escape_decode(String str) {
        return raw_unicode_escape_decode(str, null);
    }

    public static PyTuple raw_unicode_escape_decode(String str, String errors) {
        return decode_tuple(codecs.PyUnicode_DecodeRawUnicodeEscape(str, errors), str.length());
    }

    /* --- UnicodeEscape Codec -------------------------------------------- */
    public static PyTuple unicode_escape_encode(String str) {
        return unicode_escape_encode(str, null);
    }

    public static PyTuple unicode_escape_encode(String str, String errors) {
        return encode_tuple(PyString.encode_UnicodeEscape(str, false), str.length());
    }

    public static PyTuple unicode_escape_decode(String str) {
        return unicode_escape_decode(str, null);
    }

    public static PyTuple unicode_escape_decode(String str, String errors) {
        int n = str.length();
        return decode_tuple(PyString.decode_UnicodeEscape(str, 0, n, errors, true), n);
    }

    /* --- UnicodeInternal Codec ------------------------------------------ */
    // XXX Should deprecate unicode-internal codec and delegate to UTF-32BE (when we have one)
    /*
     * This codec is supposed to deal with an encoded form equal to the internal representation of
     * the unicode object considered as bytes in memory. This was confusing in CPython as it varied
     * with machine architecture (width and endian-ness). In Jython, the most compatible choice
     * would be UTF-32BE since unicode objects report their length as if UCS-4 and
     * sys.byteorder=='big'. The codec is deprecated in v3.3 as irrelevant, or impossible, in view
     * of the flexible string representation (which Jython emulates in its own way).
     *
     * See http://mail.python.org/pipermail/python-dev/2011-November/114415.html
     */
    public static PyTuple unicode_internal_encode(String str) {
        return unicode_internal_encode(str, null);
    }

    public static PyTuple unicode_internal_encode(String str, String errors) {
        return encode_tuple(str, str.length());
    }

    public static PyTuple unicode_internal_decode(String str) {
        return unicode_internal_decode(str, null);
    }

    public static PyTuple unicode_internal_decode(String str, String errors) {
        return decode_tuple(str, str.length());
    }

    /**
     * Optimized charmap encoder mapping.
     *
     * Uses a trie structure instead of a dictionary; the speedup primarily comes from not creating
     * integer objects in the process. The trie is created by inverting the encoding map.
     */
    @ExposedType(name = "EncodingMap", isBaseType = false)
    public static class EncodingMap extends PyObject {

        char[] level1;
        char[] level23;
        int count2;
        int count3;

        private EncodingMap(char[] level1, char[] level23, int count2, int count3) {
            this.level1 = level1;
            this.level23 = level23;
            this.count2 = count2;
            this.count3 = count3;
        }

        /**
         * Create and populate an EncodingMap from a 256 length PyUnicode char. Returns a
         * PyDictionary if the mapping isn't easily optimized.
         *
         * @param string a 256 length unicode mapping
         * @return an encoder mapping
         */
        public static PyObject buildEncodingMap(PyObject string) {
            if (!(string instanceof PyUnicode) || string.__len__() != 256) {
                throw Py.TypeError("bad argument type for built-in operation");
            }

            boolean needDict = false;
            char[] level1 = new char[32];
            char[] level23 = new char[512];
            int i;
            int count2 = 0;
            int count3 = 0;
            String decode = string.toString();
            for (i = 0; i < level1.length; i++) {
                level1[i] = 0xFF;
            }
            for (i = 0; i < level23.length; i++) {
                level23[i] = 0xFF;
            }
            if (decode.charAt(0) != 0) {
                needDict = true;
            }
            for (i = 1; i < 256; i++) {
                int l1, l2;
                char charAt = decode.charAt(i);
                if (charAt == 0) {
                    needDict = true;
                }
                if (charAt == 0xFFFE) {
                    // unmapped character
                    continue;
                }
                l1 = charAt >> 11;
                l2 = charAt >> 7;
                if (level1[l1] == 0xFF) {
                    level1[l1] = (char)count2++;
                }
                if (level23[l2] == 0xFF) {
                    level23[l2] = (char)count3++;
                }
            }

            if (count2 > 0xFF || count3 > 0xFF) {
                needDict = true;
            }

            if (needDict) {
                PyObject result = new PyDictionary();
                for (i = 0; i < 256; i++) {
                    result.__setitem__(Py.newInteger(decode.charAt(i)), Py.newInteger(i));
                }
                return result;
            }

            // Create a three-level trie
            int length2 = 16 * count2;
            int length3 = 128 * count3;
            level23 = new char[length2 + length3];
            PyObject result = new EncodingMap(level1, level23, count2, count3);
            for (i = 0; i < length2; i++) {
                level23[i] = 0xFF;
            }
            for (i = length2; i < length2 + length3; i++) {
                level23[i] = 0;
            }
            count3 = 0;
            for (i = 1; i < 256; i++) {
                int o1, o2, o3, i2, i3;
                char charAt = decode.charAt(i);
                if (charAt == 0xFFFE) {
                    // unmapped character
                    continue;
                }
                o1 = charAt >> 11;
                o2 = (charAt >> 7) & 0xF;
                i2 = 16 * level1[o1] + o2;
                if (level23[i2] == 0xFF) {
                    level23[i2] = (char)count3++;
                }
                o3 = charAt & 0x7F;
                i3 = 128 * level23[i2] + o3;
                level23[length2 + i3] = (char)i;
            }
            return result;
        }

        /**
         * Lookup a char in the EncodingMap.
         *
         * @param c a char
         * @return an int, -1 for failure
         */
        public int lookup(char c) {
            int l1 = c >> 11;
            int l2 = (c >> 7) & 0xF;
            int l3 = c & 0x7F;
            int i;
            if (c == 0) {
                return 0;
            }
            // level 1
            i = level1[l1];
            if (i == 0xFF) {
                return -1;
            }
            // level 2
            i = level23[16 * i + l2];
            if (i == 0xFF) {
                return -1;
            }
            // level 3
            i = level23[16 * count2 + 128 * i + l3];
            if (i == 0) {
                return -1;
            }
            return i;
        }
    }
}
