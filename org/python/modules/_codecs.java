/*
 * Copyright 2000 Finn Bock
 *
 * This program contains material copyrighted by:
 * Copyright (c) Corporation for National Research Initiatives.
 * Originally written by Marc-Andre Lemburg (mal@lemburg.com).
 */

package org.python.modules;

import org.python.core.*;
import org.python.core.imp;

public class _codecs {

    public static void register(PyObject search_function) {
        codecs.register(search_function);
    }


    public static PyTuple lookup(String encoding) {
        return codecs.lookup(encoding);
    }




    private static PyTuple codec_tuple(String s, int len) {
        return new PyTuple(new PyObject[] {
            Py.java2py(s),
            Py.newInteger(len)
        });
    }


    /* --- UTF-8 Codec -------------------------------------------------------- */

    public static PyTuple utf_8_decode(String str, String errors) {
        int size = str.length();
        return codec_tuple(codecs.PyUnicode_DecodeUTF8(str, errors), size);
    }


    public static PyTuple utf_8_encode(String str) {
        return utf_8_encode(str, null);
    }

    public static PyTuple utf_8_encode(String str, String errors) {
        int size = str.length();
        return codec_tuple(codecs.PyUnicode_EncodeUTF8(str, errors), size);
    }
  


    /* --- Character Mapping Codec -------------------------------------------- */

    public static PyTuple charmap_decode(String str, String errors,
                                         PyObject mapping) {
        int size = str.length();
        StringBuffer v = new StringBuffer(size);

        for (int i = 0; i < size; i++) {
            char ch = str.charAt(i);
            if (ch > 0xFF) {
                codecs.decoding_error("charmap", v, errors, 
                                      "ordinal not in range(255)");
                i++;
                continue;
            }

            PyObject w = Py.newInteger(ch);
            PyObject x = mapping.__finditem__(w);
            if (x == null) {
                /* No mapping found: default to Latin-1 mapping if possible */
                v.append(ch);
                continue;
            }

            /* Apply mapping */
            if (x instanceof PyInteger) {
                int value = ((PyInteger) x).getValue();
                if (value < 0 || value > 65535)
                    throw Py.TypeError(
                             "character mapping must be in range(65535)");
                v.append((char) value);
            } else if (x == Py.None) {
                codecs.decoding_error("charmap", v,  errors, 
                                      "character maps to <undefined>");
            } else if (x instanceof PyString) {
                if (x.__len__() != 1) {
                    /* 1-n mapping */
                    throw new PyException(Py.NotImplementedError,
                          "1-n mappings are currently not implemented");
                }
                v.append(x.toString());
            }
            else {
                /* wrong return value */
                throw Py.TypeError(
                     "character mapping must return integer, None or unicode");
            }
        }
        return codec_tuple(v.toString(), size);
    }





    public static PyTuple charmap_encode(String str, String errors, 
                                         PyObject mapping) {
        int size = str.length();
        StringBuffer v = new StringBuffer(size);
 
        for (int i = 0; i < size; i++) {
            char ch = str.charAt(i);
            PyObject w = Py.newInteger(ch);
            PyObject x = mapping.__finditem__(w);
            if (x == null) {
                /* No mapping found: default to Latin-1 mapping if possible */
                if (ch < 256)
                    v.append(ch);
                else
                    codecs.encoding_error("charmap", v, errors, 
                                          "missing character mapping");
                continue;
            }
            if (x instanceof PyInteger) {
                int value = ((PyInteger) x).getValue();
                if (value < 0 || value > 255)
                    throw Py.TypeError(
                            "character mapping must be in range(256)");
                v.append((char) value);
            } else if (x == Py.None) {
                codecs.encoding_error("charmap", v,  errors, 
                                      "character maps to <undefined>");
            } else if (x instanceof PyString) {
                if (x.__len__() != 1) {
                    /* 1-n mapping */
                    throw new PyException(Py.NotImplementedError,
                          "1-n mappings are currently not implemented");
                }
                v.append(x.toString());
            }
            else {
                /* wrong return value */
                throw Py.TypeError(
                     "character mapping must return integer, None or unicode");
            }
        }
        return codec_tuple(v.toString(), size);
    }
            


    /* --- 7-bit ASCII Codec -------------------------------------------- */

    public static PyTuple ascii_decode(String str, String errors) {
        int size = str.length();
        return codec_tuple(codecs.PyUnicode_DecodeASCII(str, size, errors),
                                                                        size);
    }


    public static PyTuple ascii_encode(String str, String errors) {
        int size = str.length();
        return codec_tuple(codecs.PyUnicode_EncodeASCII(str, size, errors), 
                                                                        size);
    }
           

    /* --- Latin-1 Codec -------------------------------------------- */

    public static PyTuple latin_1_decode(String str, String errors) {
        int size = str.length();
        StringBuffer v = new StringBuffer(size);

        for (int i = 0; i < size; i++) {
            char ch = str.charAt(i);
            if (ch < 256) {
                v.append(ch);
            } else {
                codecs.decoding_error("latin-1", v, errors, 
                                      "ordinal not in range(256)");
                i++;
                continue;
            }
        }

        return codec_tuple(v.toString(), size);
    }




    public static PyTuple latin_1_encode(String str, String errors) {
        int size = str.length();
        StringBuffer v = new StringBuffer(size);
 
        for (int i = 0; i < size; i++) {
            char ch = str.charAt(i);
            if (ch >= 256) {
                codecs.encoding_error("latin-1", v, errors, 
                                      "ordinal not in range(256)");
            } else
                v.append(ch);
        }
        return codec_tuple(v.toString(), size);
    }


    /* --- UTF16 Codec -------------------------------------------- */


    public static PyTuple utf_16_encode(String str, String errors) {
        return codec_tuple(encode_UTF16(str, errors, 0), str.length());
    }

    public static PyTuple utf_16_encode(String str, String errors,
                                       int byteorder) {
        return codec_tuple(encode_UTF16(str, errors, byteorder), str.length());
    }

    public static PyTuple utf_16_le_encode(String str, String errors) {
        return codec_tuple(encode_UTF16(str, errors, -1), str.length());
    }

    public static PyTuple utf_16_be_encode(String str, String errors) {
        return codec_tuple(encode_UTF16(str, errors, 1), str.length());
    }


    private static String encode_UTF16(String str, String errors, 
                                      int byteorder) {
        int size = str.length();
        StringBuffer v = new StringBuffer((size + 
                                       (byteorder == 0 ? 1 : 0)) * 2);

        if (byteorder == 0) {
            v.append((char) 0xFE);
            v.append((char) 0xFF);
        }

        if (byteorder == 0 || byteorder == 1)
            for (int i = 0; i < size; i++) {
                char ch = str.charAt(i);
                v.append((char) ((ch >>> 8) & 0xFF));
                v.append((char) (ch & 0xFF));
            }
        else {
            for (int i = 0; i < size; i++) {
                char ch = str.charAt(i);
                v.append((char) (ch & 0xFF));
                v.append((char) ((ch >>> 8) & 0xFF));
            }
        }

        return v.toString();
    }

    


    public static PyTuple utf_16_decode(String str, String errors) {
        int[] bo = new int[] { 0 };
        return codec_tuple(decode_UTF16(str, errors, bo), str.length());
    }

    public static PyTuple utf_16_decode(String str, String errors, 
                                        int byteorder) {
        int[] bo = new int[] { byteorder };
        return codec_tuple(decode_UTF16(str, errors, bo), str.length());
    }

    public static PyTuple utf_16_le_decode(String str, String errors) {
        int[] bo = new int[] { -1 };
        return codec_tuple(decode_UTF16(str, errors, bo), str.length());
    }

    public static PyTuple utf_16_be_decode(String str, String errors) {
        int[] bo = new int[] { 1 };
        return codec_tuple(decode_UTF16(str, errors, bo), str.length());
    }

    public static PyTuple utf_16_ex_decode(String str, String errors) {
        return utf_16_ex_decode(str, errors, 0);
    }

    public static PyTuple utf_16_ex_decode(String str, String errors,
                                           int byteorder) {
        int[] bo = new int[] { 0 };
        String s = decode_UTF16(str, errors, bo);
        return new PyTuple(new PyObject[] { 
             Py.newString(s),  
             Py.newInteger(str.length()), 
             Py.newInteger(bo[0])
        });
    }

    private static String decode_UTF16(String str, String errors, 
                                       int[] byteorder) {
        int bo = 0;
        if (byteorder != null)
             bo = byteorder[0];

        int size = str.length();

        if (size % 2 != 0)
            codecs.decoding_error("UTF16", null, errors, "truncated data");

        StringBuffer v = new StringBuffer(size/2);

        for (int i = 0; i < size; i += 2) {
            char ch1 = str.charAt(i);
            char ch2 = str.charAt(i+1);
            if (ch1 == 0xFE && ch2 == 0xFF) {
                bo = 1;
                continue;
            } else if (ch1 == 0xFF && ch2 == 0xFE) {
                bo = -1;
                continue;
            }
            
            char ch;
            if (bo == -1)
                ch = (char) (ch2 << 8 | ch1);
            else
                ch = (char) (ch1 << 8 | ch2);

            if (ch < 0xD800 || ch > 0xDFFF) {
                v.append(ch);
                continue;
            }
            

            /* UTF-16 code pair: */
            if (i == size-1) {
                codecs.decoding_error("UTF-16", v, errors,
                                      "unexpected end of data");
                continue;
            }

            ch = str.charAt(++i);
            if (0xDC00 <= ch && ch <= 0xDFFF) {
                ch = str.charAt(++i);
                if (0xD800 <= ch && ch <= 0xDBFF)
                    /* This is valid data (a UTF-16 surrogate pair), but
                       we are not able to store this information since our
                       Py_UNICODE type only has 16 bits... this might
                       change someday, even though it's unlikely. */
                    codecs.decoding_error("UTF-16", v, errors, 
                                          "code pairs are not supported");
                continue;
            }
            codecs.decoding_error("UTF-16", v, errors, "illegal encoding");
        }

        if (byteorder != null)
            byteorder[0] = bo;

        return v.toString();
    }



    /* --- RawUnicodeEscape Codec ----------------------------------------- */


    public static PyTuple raw_unicode_escape_encode(String str, 
                                                   String errors) {
        return codec_tuple(codecs.PyUnicode_EncodeRawUnicodeEscape(str, errors, false),
                           str.length());
    }


    public static PyTuple raw_unicode_escape_decode(String str, 
                                                    String errors) {
        return codec_tuple(codecs.PyUnicode_DecodeRawUnicodeEscape(str, errors), 
                           str.length());
    }



    /* --- UnicodeEscape Codec -------------------------------------------- */


    public static PyTuple unicode_escape_encode(String str, String errors) {
        return codec_tuple(PyString.encode_UnicodeEscape(str, false), str.length());
    }

    public static PyTuple unicode_escape_decode(String str, String errors) {
        int n = str.length();
        return codec_tuple(PyString.decode_UnicodeEscape(str, 0, n, errors, true), n);
    }



    /* --- UnicodeInternal Codec -------------------------------------------- */


    public static PyTuple unicode_internal_encode(String str, String errors) {
        return codec_tuple(str, str.length());
    }

    public static PyTuple unicode_internal_decode(String str, String errors) {
        return codec_tuple(str, str.length());
    }

}

