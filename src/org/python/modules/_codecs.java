/*
 * Copyright 2000 Finn Bock
 *
 * This program contains material copyrighted by:
 * Copyright (c) Corporation for National Research Initiatives.
 * Originally written by Marc-Andre Lemburg (mal@lemburg.com).
 */
package org.python.modules;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

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
        int[] consumed = final_ ? new int[1] : null;
        return decode_tuple(codecs.PyUnicode_DecodeUTF8Stateful(str, errors, consumed),
                            final_ ? consumed[0] : str.length());
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
        return decode_tuple_str(PyString.decode_UnicodeEscape(str,
                0,
                str.length(),
                errors,
                true), str.length());
    }

    public static PyTuple escape_encode(String str) {
        return escape_encode(str, null);
    }

    public static PyTuple escape_encode(String str, String errors) {
        return encode_tuple(PyString.encode_UnicodeEscape(str, false),
                str.length());

    }

    /* --- Character Mapping Codec --------------------------------------- */
    public static PyTuple charmap_decode(String str,
            String errors,
            PyObject mapping) {
        return charmap_decode(str, errors, mapping, false);
    }

    public static PyTuple charmap_decode(String str,
            String errors,
            PyObject mapping, boolean ignoreUnmapped) {


        int size = str.length();
        StringBuilder v = new StringBuilder(size);
        for (int i = 0; i < size; i++) {
            char ch = str.charAt(i);
            if (ch > 0xFF) {
                i = codecs.insertReplacementAndGetResume(v,
                        errors,
                        "charmap",
                        str,
                        i,
                        i + 1,
                        "ordinal not in range(255)") - 1;
                continue;
            }
            PyObject w = Py.newInteger(ch);
            PyObject x = mapping.__finditem__(w);
            if (x == null) {
                if (ignoreUnmapped) {
                    v.append(ch);
                } else {
                    i = codecs.insertReplacementAndGetResume(v, errors, "charmap", str, i, i + 1, "no mapping found") - 1;
                }
                continue;
            }
            /* Apply mapping */
            if (x instanceof PyInteger) {
                int value = ((PyInteger) x).getValue();
                if (value < 0 || value > PySystemState.maxunicode) {
                    throw Py.TypeError("character mapping must return " + "integer greater than 0 and less than sys.maxunicode");
                }
                v.append((char) value);
            } else if (x == Py.None) {
                i = codecs.insertReplacementAndGetResume(v,
                        errors,
                        "charmap",
                        str,
                        i,
                        i + 1,
                        "character maps to <undefined>") - 1;
            } else if (x instanceof PyString) {
                v.append(x.toString());
            } else {
                /* wrong return value */
                throw Py.TypeError("character mapping must return " + "integer, None or str");
            }
        }
        return decode_tuple(v.toString(), size);
    }

    // parallel to CPython's PyUnicode_TranslateCharmap
    public static PyTuple translate_charmap(String str,
            String errors,
            PyObject mapping, boolean ignoreUnmapped) {

        int size = str.length();
        StringBuilder v = new StringBuilder(size);
        for (int i = 0; i < size; i++) {
            char ch = str.charAt(i);
            if (ch > 0xFF) {
                i = codecs.insertReplacementAndGetResume(v,
                        errors,
                        "charmap",
                        str,
                        i,
                        i + 1,
                        "ordinal not in range(255)") - 1;
                continue;
            }
            PyObject w = Py.newInteger(ch);
            PyObject x = mapping.__finditem__(w);
            if (x == null) {
                if (ignoreUnmapped) {
                    v.append(ch);
                } else {
                    i = codecs.insertReplacementAndGetResume(v, errors, "charmap", str, i, i + 1, "no mapping found") - 1;
                }
                continue;
            }
            /* Apply mapping */
            if (x instanceof PyInteger) {
                int value = ((PyInteger) x).getValue();
                if (value < 0 || value > PySystemState.maxunicode) {
                    throw Py.TypeError("character mapping must return " + "integer greater than 0 and less than sys.maxunicode");
                }
                v.append((char) value);
            } else if (x == Py.None) {
                i = codecs.insertReplacementAndGetResume(v,
                        errors,
                        "charmap",
                        str,
                        i,
                        i + 1,
                        "character maps to <undefined>") - 1;
            } else if (x instanceof PyUnicode) {
                v.append(x.toString());
            } else {
                /* wrong return value */
                throw Py.TypeError("character mapping must return " + "integer, None or unicode");
            }
        }
        return decode_tuple(v.toString(), size);
    }
    
    
    public static PyTuple charmap_encode(String str, String errors,
            PyObject mapping) {
        //Default to Latin-1
        if (mapping == null) {
            return latin_1_encode(str, errors);
        }
        return charmap_encode_internal(str, errors, mapping, new StringBuilder(str.length()), true);
    }

    private static PyTuple charmap_encode_internal(String str,
            String errors,
            PyObject mapping,
            StringBuilder v,
            boolean letLookupHandleError) {
        EncodingMap encodingMap = mapping instanceof EncodingMap ? (EncodingMap)mapping : null;
        int size = str.length();
        for (int i = 0; i < size; i++) {
            char ch = str.charAt(i);
            PyObject x;
            if (encodingMap != null) {
                int result = encodingMap.lookup(ch);
                if (result == -1) {
                    x = null;
                } else {
                    x = Py.newInteger(result);
                }
            } else {
                x = mapping.__finditem__(Py.newInteger(ch));
            }
            if (x == null) {
                if (letLookupHandleError) {
                    i = handleBadMapping(str, errors, mapping, v, size, i);
                } else {
                    throw Py.UnicodeEncodeError("charmap",
                            str,
                            i,
                            i + 1,
                            "character maps to <undefined>");
                }
            } else if (x instanceof PyInteger) {
                int value = ((PyInteger) x).getValue();
                if (value < 0 || value > 255) {
                    throw Py.TypeError("character mapping must be in range(256)");
                }
                v.append((char) value);
            } else if (x instanceof PyString && !(x instanceof PyUnicode)) {
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

    private static int handleBadMapping(String str,
            String errors,
            PyObject mapping,
            StringBuilder v,
            int size,
            int i) {
        if (errors != null) {
            if (errors.equals(codecs.IGNORE)) {
                return i;
            } else if (errors.equals(codecs.REPLACE)) {
                charmap_encode_internal("?", errors, mapping, v, false);
                return i;
            } else if (errors.equals(codecs.XMLCHARREFREPLACE)) {
                charmap_encode_internal(codecs.xmlcharrefreplace(i, i + 1, str).toString(), errors, mapping, v, false);
                return i;
            } else if (errors.equals(codecs.BACKSLASHREPLACE)) {
                charmap_encode_internal(codecs.backslashreplace(i, i + 1, str).toString(), errors, mapping, v, false);
                return i;
            }
        }
        PyObject replacement = codecs.encoding_error(errors,
                "charmap",
                str,
                i,
                i + 1,
                "character maps to <undefined>");
        String replStr = replacement.__getitem__(0).toString();
        charmap_encode_internal(replStr, errors, mapping, v, false);
        return codecs.calcNewPosition(size, replacement) - 1;
    }

    public static PyTuple ascii_decode(String str) {
        return ascii_decode(str, null);
    }

    public static PyTuple ascii_decode(String str, String errors) {
        int size = str.length();
        return decode_tuple(codecs.PyUnicode_DecodeASCII(str, size, errors),
                size);
    }

    public static PyTuple ascii_encode(String str) {
        return ascii_encode(str, null);
    }

    public static PyTuple ascii_encode(String str, String errors) {
        int size = str.length();
        return encode_tuple(codecs.PyUnicode_EncodeASCII(str, size, errors),
                size);
    }


    /* --- Latin-1 Codec -------------------------------------------- */
    public static PyTuple latin_1_decode(String str) {
        return latin_1_decode(str, null);
    }

    public static PyTuple latin_1_decode(String str, String errors) {
        int size = str.length();
        return decode_tuple(codecs.PyUnicode_DecodeLatin1(str, size, errors),
                size);
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

    public static PyTuple utf_16_encode(String str, String errors,
            int byteorder) {
        return encode_tuple(encode_UTF16(str, errors, byteorder),
                str.length());
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
        int[] bo = new int[] { 0 };
        int[] consumed = final_ ? new int[1] : null;
        return decode_tuple(decode_UTF16(str, errors, bo, consumed),
                            final_ ? consumed[0] : str.length());
    }

    public static PyTuple utf_16_le_decode(String str) {
        return utf_16_le_decode(str, null);
    }

    public static PyTuple utf_16_le_decode(String str, String errors) {
        return utf_16_le_decode(str, errors, false);
    }
        
    public static PyTuple utf_16_le_decode(String str, String errors, boolean final_) {
        int[] bo = new int[] { -1 };
        int[] consumed = final_ ? new int[1] : null;
        return decode_tuple(decode_UTF16(str, errors, bo, consumed),
                            final_ ? consumed[0] : str.length());
    }

    public static PyTuple utf_16_be_decode(String str) {
        return utf_16_be_decode(str, null);
    }
    
    public static PyTuple utf_16_be_decode(String str, String errors) {
        return utf_16_be_decode(str, errors, false);
    }

    public static PyTuple utf_16_be_decode(String str, String errors, boolean final_) {
        int[] bo = new int[] { 1 };
        int[] consumed = final_ ? new int[1] : null;
        return decode_tuple(decode_UTF16(str, errors, bo, consumed),
                            final_ ? consumed[0] : str.length());
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
    
    public static PyTuple utf_16_ex_decode(String str, String errors, int byteorder,
                                           boolean final_) {
        int[] bo = new int[] { 0 };
        int[] consumed = final_ ? new int[1] : null;
        String decoded = decode_UTF16(str, errors, bo, consumed);
        return new PyTuple(Py.newString(decoded),
                           Py.newInteger(final_ ? consumed[0] : str.length()),
                           Py.newInteger(bo[0]));
    }

    private static String decode_UTF16(String str,
            String errors,
            int[] byteorder) {
        return decode_UTF16(str, errors, byteorder, null);
    }

        private static String decode_UTF16(String str,
            String errors,
            int[] byteorder,
            int[] consumed) {
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
                i = codecs.insertReplacementAndGetResume(v,
                        errors,
                        "utf-16",
                        str,
                        i,
                        i + 1,
                        "truncated data");
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
                i = codecs.insertReplacementAndGetResume(v,
                        errors,
                        "utf-16",
                        str,
                        i,
                        i + 1,
                        "illegal UTF-16 surrogate");
                continue;
            }

            i = codecs.insertReplacementAndGetResume(v,
                    errors,
                    "utf-16",
                    str,
                    i,
                    i + 1,
                    "illegal encoding");
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

    public static PyTuple raw_unicode_escape_encode(String str,
            String errors) {
        return encode_tuple(codecs.PyUnicode_EncodeRawUnicodeEscape(str,
                errors, false),
                str.length());
    }

    public static PyTuple raw_unicode_escape_decode(String str) {
        return raw_unicode_escape_decode(str, null);
    }

    public static PyTuple raw_unicode_escape_decode(String str,
            String errors) {
        return decode_tuple(codecs.PyUnicode_DecodeRawUnicodeEscape(str,
                errors),
                str.length());
    }

    /* --- UnicodeEscape Codec -------------------------------------------- */
    public static PyTuple unicode_escape_encode(String str) {
        return unicode_escape_encode(str, null);
    }

    public static PyTuple unicode_escape_encode(String str, String errors) {
        return encode_tuple(PyString.encode_UnicodeEscape(str, false),
                str.length());
    }

    public static PyTuple unicode_escape_decode(String str) {
        return unicode_escape_decode(str, null);
    }

    public static PyTuple unicode_escape_decode(String str, String errors) {
        int n = str.length();
        return decode_tuple(PyString.decode_UnicodeEscape(str,
                0,
                n,
                errors,
                true), n);
    }

    /* --- UnicodeInternal Codec ------------------------------------------ */
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
     * Uses a trie structure instead of a dictionary; the speedup primarily comes from not
     * creating integer objects in the process. The trie is created by inverting the
     * encoding map.
     */
    @ExposedType(name = "EncodingMap")
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

