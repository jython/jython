/*
 * Copyright 2000 Finn Bock
 *
 * This program contains material copyrighted by:
 * Copyright (c) Corporation for National Research Initiatives.
 * Originally written by Marc-Andre Lemburg (mal@lemburg.com).
 */

package org.python.core;

/**
 * Contains the implementation of the builtin codecs.
 * @since Jython 2.0
 */

public class codecs {
    private static char Py_UNICODE_REPLACEMENT_CHARACTER = 0xFFFD;

    private static PyList searchPath = new PyList();
    private static PyStringMap searchCache = new PyStringMap();

    private static String default_encoding = "ascii";

    public static String getDefaultEncoding() {
        return default_encoding;
    }

    public static void setDefaultEncoding(String encoding) {
        lookup(encoding);
        default_encoding = encoding;
    }

    public static void register(PyObject search_function) {
        if (!search_function.isCallable()) {
           throw Py.TypeError("argument must be callable");
        }
        searchPath.append(search_function);
    }


    public static PyTuple lookup(String encoding) {
        import_encodings();
        PyString v = new PyString(normalizestring(encoding));
        PyObject result = searchCache.__finditem__(v);
        if (result != null) {
            return (PyTuple)result;
        }

        if (searchPath.__len__() == 0) {
             throw new PyException(Py.LookupError,
                   "no codec search functions registered: " +
                   "can't find encoding");
        }

        PyObject iter = searchPath.__iter__();
        PyObject func = null;
        while ((func = iter.__iternext__()) != null) {
            result = func.__call__(v);
            if (result == Py.None) {
                continue;
            }
            if (!(result instanceof PyTuple) || result.__len__() != 4) {
                throw Py.TypeError("codec search functions must "+
                                   "return 4-tuples");
            }
            break;
        }
        if (func == null) {
            throw new PyException(Py.LookupError, "unknown encoding " +
                                  encoding);
        }
        searchCache.__setitem__(v, result);
        return (PyTuple)result;
    }

    private static String normalizestring(String string) {
        return string.toLowerCase().replace(' ', '-');
    }


    private static boolean import_encodings_called = false;

    private static final Object IMPORT_LOCK = new Object();

    private static void import_encodings() {
        synchronized (IMPORT_LOCK) {
            if (!import_encodings_called) {
                import_encodings_called = true;
                try {
                    __builtin__.__import__("encodings");
                } catch (PyException exc) {
                    if (exc.type != Py.ImportError) {
                        throw exc;
                    }
                }
            }
        }
    }



    public static String decode(PyString v, String encoding,
                                  String errors)
    {
        if (encoding == null) {
            encoding = getDefaultEncoding();
        } else {
            encoding = normalizestring(encoding);
        }

        if (errors != null) {
            errors = errors.intern();
        }

        /* Shortcuts for common default encodings */
/*
        if (encoding.equals("utf-8"))
            return utf_8_decode(v, errors).__getitem__(0).__str__();
        else if (encoding.equals("latin-1"))
            ; //return PyUnicode_DecodeLatin1(s, size, errors);
        else if (encoding.equals("ascii"))
            ; //return PyUnicode_DecodeASCII(s, size, errors);
*/
        if (encoding.equals("ascii")) {
            return PyUnicode_DecodeASCII(v.toString(),
                                                      v.__len__(), errors);
        }

        /* Decode via the codec registry */
        PyObject decoder = getDecoder(encoding);
        PyObject result = null;
        if (errors != null) {
            result = decoder.__call__(v, new PyString(errors));
        } else {
            result = decoder.__call__(v);
        }

        if (!(result instanceof PyTuple) || result.__len__() != 2) {
            throw Py.TypeError("decoder must return a tuple " +
                               "(object,integer)");
        }
        return result.__getitem__(0).toString();
    }


    private static PyObject getDecoder(String encoding) {
        PyObject codecs = lookup(encoding);
        return codecs.__getitem__(1);
    }



    public static String encode(PyString v, String encoding,
                                  String errors)
    {
        if (encoding == null) {
            encoding = getDefaultEncoding();
        } else {
            encoding = normalizestring(encoding);
        }

        if (errors != null) {
            errors = errors.intern();
        }

        /* Shortcuts for common default encodings */
/*
        if (encoding.equals("utf-8"))
            return PyUnicode_DecodeUTF8(v.toString(), v.__len__(), errors);
        else if (encoding.equals("latin-1"))
            return PyUnicode_DecodeLatin1(v.toString(), v.__len__(), errors);
        else
*/

        if (encoding.equals("ascii")) {
            return PyUnicode_EncodeASCII(v.toString(),
                                                      v.__len__(), errors);
        }

        /* Decode via the codec registry */
        PyObject encoder = getEncoder(encoding);
        PyObject result = null;
        if (errors != null) {
            result = encoder.__call__(v, new PyString(errors));
        } else {
            result = encoder.__call__(v);
        }

        if (!(result instanceof PyTuple) || result.__len__() != 2) {
            throw Py.TypeError("encoder must return a tuple " +
                               "(object,integer)");
        }
        return result.__getitem__(0).toString();
    }

    private static PyObject getEncoder(String encoding) {
        PyObject codecs = lookup(encoding);
        return codecs.__getitem__(0);
    }


    /* --- UTF-8 Codec ---------------------------------------------------- */
    private static byte utf8_code_length[] = {
       /* Map UTF-8 encoded prefix byte to sequence length.  zero means
           illegal prefix.  see RFC 2279 for details */
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,
        2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,
        3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3,
        4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 5, 5, 6, 6, 0, 0
    };


    public static String PyUnicode_DecodeUTF8(String str, String errors) {
        int size = str.length();
        StringBuffer unicode = new StringBuffer(size);

        /* Unpack UTF-8 encoded data */
        for (int i = 0; i < size; ) {
            int ch = str.charAt(i);
            if (ch > 0xFF) {
                codecs.decoding_error("utf-8", unicode, errors,
                                      "ordinal not in range(255)");
                i++;
                continue;
            }

            if (ch < 0x80) {
                unicode.append((char) ch);
                i++;
                continue;
            }

            int n = utf8_code_length[ch];

            if (i + n > size) {
                codecs.decoding_error("utf-8", unicode, errors,
                                      "unexpected end of data");
                i++;
                continue;
            }


            switch (n) {
            case 0:
                codecs.decoding_error("utf-8", unicode, errors,
                                      "unexpected code byte");
                i++;
                continue;
            case 1:
                codecs.decoding_error("utf-8", unicode, errors,
                                      "internal error");
                i++;
                continue;
            case 2:
                char ch1 = str.charAt(i+1);
                if ((ch1 & 0xc0) != 0x80) {
                    codecs.decoding_error("utf-8", unicode, errors,
                                          "invalid data");
                    i++;
                    continue;
                }
                ch = ((ch & 0x1f) << 6) + (ch1 & 0x3f);
                if (ch < 0x80) {
                    codecs.decoding_error("utf-8", unicode, errors,
                                          "illegal encoding");
                    i++;
                    continue;
                } else
                    unicode.append((char) ch);
                break;

            case 3:
                ch1 = str.charAt(i+1);
                char ch2 = str.charAt(i+2);
                if ((ch1 & 0xc0) != 0x80 || (ch2 & 0xc0) != 0x80) {
                    codecs.decoding_error("utf-8", unicode, errors,
                                          "invalid data");
                    i++;
                    continue;
                }
                ch = ((ch & 0x0f) << 12) + ((ch1 & 0x3f) << 6) + (ch2 & 0x3f);
                if (ch < 0x800 || (ch >= 0xd800 && ch < 0xe000)) {
                    codecs.decoding_error("utf-8", unicode, errors,
                                          "illegal encoding");
                    i++;
                    continue;
                } else
                   unicode.append((char) ch);
                break;

            case 4:
                ch1 = str.charAt(i+1);
                ch2 = str.charAt(i+2);
                char ch3 = str.charAt(i+3);
                if ((ch1 & 0xc0) != 0x80 ||
                    (ch2 & 0xc0) != 0x80 ||
                    (ch3 & 0xc0) != 0x80) {
                    codecs.decoding_error("utf-8", unicode, errors,
                                          "invalid data");
                    i++;
                    continue;
                }
                ch = ((ch & 0x7) << 18) + ((ch1 & 0x3f) << 12) +
                     ((ch2 & 0x3f) << 6) + (ch3 & 0x3f);
                /* validate and convert to UTF-16 */
                if ((ch < 0x10000) ||   /* minimum value allowed for 4
                                           byte encoding */
                    (ch > 0x10ffff)) {  /* maximum value allowed for
                                           UTF-16 */
                    codecs.decoding_error("utf-8", unicode, errors,
                                          "illegal encoding");
                    i++;
                    continue;
                }
                /*  compute and append the two surrogates: */

                /*  translate from 10000..10FFFF to 0..FFFF */
                ch -= 0x10000;

                /*  high surrogate = top 10 bits added to D800 */
                unicode.append((char) (0xD800 + (ch >> 10)));

                /*  low surrogate = bottom 10 bits added to DC00 */
                unicode.append((char) (0xDC00 + (ch & ~0xFC00)));
                break;

            default:
                /* Other sizes are only needed for UCS-4 */
                codecs.decoding_error("utf-8", unicode, errors,
                                      "unsupported Unicode code range");
                i++;
            }
            i += n;
        }

        return unicode.toString();
    }


    public static String PyUnicode_EncodeUTF8(String str, String errors) {
        int size = str.length();
        StringBuffer v = new StringBuffer(size * 3);

        for (int i = 0; i < size; ) {
            int ch = str.charAt(i++);
            if (ch < 0x80) {
                v.append((char) ch);
            } else if (ch < 0x0800) {
                v.append((char) (0xc0 | (ch >> 6)));
                v.append((char) (0x80 | (ch & 0x3f)));
            } else {
                if (0xD800 <= ch && ch <= 0xDFFF) {
                    if (i != size) {
                        int ch2 = str.charAt(i);
                        if (0xDC00 <= ch2 && ch2 <= 0xDFFF) {
                            /* combine the two values */
                            ch = ((ch - 0xD800)<<10 | (ch2-0xDC00))+0x10000;

                            v.append((char)((ch >> 18) | 0xf0));
                            v.append((char)(0x80 | ((ch >> 12) & 0x3f)));
                            i++;
                        }
                    }
                } else {
                    v.append((char)(0xe0 | (ch >> 12)));
                }
                v.append((char) (0x80 | ((ch >> 6) & 0x3f)));
                v.append((char) (0x80 | (ch & 0x3f)));
            }
        }
        return v.toString();
    }



    /* --- 7-bit ASCII Codec -------------------------------------------- */

    public static String PyUnicode_DecodeASCII(String str, int size,
                                               String errors)
    {
        StringBuffer v = new StringBuffer(size);

        for (int i = 0; i < size; i++) {
            char ch = str.charAt(i);
            if (ch < 128) {
                v.append(ch);
            } else {
                decoding_error("ascii", v, errors,
                               "ordinal not in range(128)");
                continue;
            }
        }

        return v.toString();
    }


    public static String PyUnicode_EncodeASCII(String str, int size,
                                               String errors)
    {
        StringBuffer v = new StringBuffer(size);

        for (int i = 0; i < size; i++) {
            char ch = str.charAt(i);
            if (ch >= 128) {
                encoding_error("ascii", v, errors,
                               "ordinal not in range(128)");
            } else {
                v.append(ch);
            }
        }
        return v.toString();
    }



    /* --- RawUnicodeEscape Codec ---------------------------------------- */

    private static char[] hexdigit = "0123456789ABCDEF".toCharArray();

    // The modified flag is used by cPickle.
    public static String PyUnicode_EncodeRawUnicodeEscape(String str,
                                                          String errors,
                                                          boolean modifed)
    {

        int size = str.length();
        StringBuffer v = new StringBuffer(str.length());

        for (int i = 0; i < size; i++) {
            char ch = str.charAt(i);
            if (ch >= 256 || (modifed && (ch == '\n' || ch == '\\'))) {
                v.append("\\u");
                v.append(hexdigit[(ch >>> 12) & 0xF]);
                v.append(hexdigit[(ch >>> 8) & 0xF]);
                v.append(hexdigit[(ch >>> 4) & 0xF]);
                v.append(hexdigit[ch & 0xF]);
            } else {
                v.append(ch);
            }
        }

        return v.toString();
    }


    public static String PyUnicode_DecodeRawUnicodeEscape(String str,
                                                          String errors)
    {
        int size = str.length();
        StringBuffer v = new StringBuffer(size);

        for (int i = 0; i < size; ) {
            char ch = str.charAt(i);

            /* Non-escape characters are interpreted as Unicode ordinals */
            if (ch != '\\') {
                v.append(ch);
                i++;
                continue;
            }

            /* \\u-escapes are only interpreted iff the number of leading
               backslashes is odd */
            int bs = i;
            while (i < size) {
                ch = str.charAt(i);
                if (ch != '\\')
                    break;
                v.append(ch);
                i++;
            }
            if (((i - bs) & 1) == 0 || i >= size || ch != 'u') {
                continue;
            }
            v.setLength(v.length() - 1);
            i++;

            /* \\uXXXX with 4 hex digits */
            int x = 0;
            for (int j = 0; j < 4; j++) {
                ch = str.charAt(i+j);
                int d  = Character.digit(ch, 16);
                if (d == -1) {
                    codecs.decoding_error("unicode escape", v, errors,
                                          "truncated \\uXXXX");
                    break;
                }
                x = ((x<<4) & ~0xF) + d;
            }
            i += 4;
            v.append((char) x);
       }
       return v.toString();
    }


    /* --- Utility methods -------------------------------------------- */

    public static void encoding_error(String type, StringBuffer dest,
                                      String errors, String details)
    {
        if (errors == null || errors == "strict") {
            throw Py.UnicodeError(type + " encoding error: " + details);
        } else if (errors == "ignore") {
            //ignore
        } else if (errors == "replace") {
            dest.append('?');
        } else {
            throw Py.ValueError(type + " encoding error; "+
                                "unknown error handling code: " + errors);
        }
    }


    public static void decoding_error(String type, StringBuffer dest,
                                      String errors, String details)
    {
        if (errors == null || errors == "strict") {
            throw Py.UnicodeError(type + " decoding error: " + details);
        }
        else if (errors == "ignore") {
            //ignore
        } else if (errors == "replace") {
            if (dest != null) {
                dest.append(Py_UNICODE_REPLACEMENT_CHARACTER);
            }
        } else {
            throw Py.ValueError(type + " decoding error; "+
                                "unknown error handling code: " + errors);
        }
    }
}
