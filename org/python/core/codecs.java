/*
 * Copyright 2000 Finn Bock
 *
 * This program contains material copyrighted by:
 * Copyright (c) Corporation for National Research Initiatives.
 * Originally written by Marc-Andre Lemburg (mal@lemburg.com).
 */

package org.python.core;


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
        if (!search_function.isCallable()) 
           throw Py.TypeError("argument must be callable");
        searchPath.append(search_function);
    }


    public static PyTuple lookup(String encoding) {
        import_encodings();
        PyString v = new PyString(normalizestring(encoding));
        PyObject result = searchCache.__finditem__(v);
        if (result != null)
            return (PyTuple)result;

        if (searchPath.__len__() == 0)
             throw new PyException(Py.LookupError,
                   "no codec search functions registered: can't find encoding");

        int i = 0;
        for (PyObject func = null; (func = searchPath.__finditem__(i)) != null; i++) {
            result = func.__call__(v);
            if (result == Py.None)
                continue;
            if (!(result instanceof PyTuple) || result.__len__() != 4)
                throw Py.TypeError("codec search functions must return 4-tuples"); 
            break;
        }
        if (i == searchPath.__len__())
            throw new PyException(Py.LookupError, "unknown encoding " + encoding);
        searchCache.__setitem__(v, result);
        return (PyTuple)result;
    }




    private static String normalizestring(String string) {
        return string.toLowerCase().replace(' ', '-');
    }
 

    private static boolean import_encodings_called = false;

    private static void import_encodings() {
        if (!import_encodings_called) {
            import_encodings_called = true;
            try {
                __builtin__.__import__("encodings");
            } catch (PyException exc) {
                if (exc.type != Py.ImportError)
                    throw exc;
            }
        }
    }







    public static PyString decode(PyString v, String encoding, String errors) {
        if (encoding == null)
            encoding = getDefaultEncoding();
        if (errors != null)
            errors = errors.intern();

        /* Shortcuts for common default encodings */
/*
        if (encoding.equals("utf-8"))
            return utf_8_decode(v.toString(), errors).__getitem__(0).__str__();
        else if (encoding.equals("latin-1"))
            ; //return PyUnicode_DecodeLatin1(s, size, errors);
        else if (encoding.equals("ascii"))
            ; //return PyUnicode_DecodeASCII(s, size, errors);
*/
        if (encoding.equals("ascii"))
            return new PyString(PyUnicode_DecodeASCII(v.toString(), v.__len__(), errors));

        /* Decode via the codec registry */
        PyObject decoder = getDecoder(encoding);
        PyObject result = decoder.__call__(v, new PyString(errors));
        
        if (!(result instanceof PyTuple) || result.__len__() != 2) 
            throw Py.TypeError("decoder must return a tuple (object,integer)");
        return result.__getitem__(0).__str__();
    }


    private static PyObject getDecoder(String encoding) {
        PyObject codecs = lookup(encoding);
        return codecs.__getitem__(1);
    }



    public static PyString encode(PyString v, String encoding, String errors) {
        if (encoding == null)
            encoding = getDefaultEncoding();
        if (errors != null)
            errors = errors.intern();

        /* Shortcuts for common default encodings */
/*
        if (encoding.equals("utf-8"))
            return PyUnicode_DecodeUTF8(v.toString(), v.__len__(), errors);
        else if (encoding.equals("latin-1"))
            return PyUnicode_DecodeLatin1(v.toString(), v.__len__(), errors);
        else 
*/

        if (encoding.equals("ascii"))
            return new PyString(PyUnicode_EncodeASCII(v.toString(), v.__len__(), errors));

        /* Decode via the codec registry */
        PyObject encoder = getEncoder(encoding);
        PyObject result = encoder.__call__(v, new PyString(errors));
        
        if (!(result instanceof PyTuple) || result.__len__() != 2) 
            throw Py.TypeError("encoder must return a tuple (object,integer)");
        return result.__getitem__(0).__str__();
    }

    private static PyObject getEncoder(String encoding) {
        PyObject codecs = lookup(encoding);
        return codecs.__getitem__(0);
    }



    /* --- 7-bit ASCII Codec -------------------------------------------- */

    public static String PyUnicode_DecodeASCII(String str, int size, String errors) {
        StringBuffer v = new StringBuffer(size);

        for (int i = 0; i < size; i++) {
            char ch = str.charAt(i);
            if (ch < 128) {
                v.append(ch);
            } else {
                decoding_error("ascii", v, errors, "ordinal not in range(128)");
                continue;
            }
        }

        return v.toString();
    }


    public static String PyUnicode_EncodeASCII(String str, int size, String errors) {
        StringBuffer v = new StringBuffer(size);

        for (int i = 0; i < size; i++) {
            char ch = str.charAt(i);
            if (ch >= 128) {
                encoding_error("ascii", v, errors, "ordinal not in range(128)");
            } else
                v.append(ch);
        }
        return v.toString();
    }



    /* --- RawUnicodeEscape Codec -------------------------------------------- */

    private static char[] hexdigit = "0123456789ABCDEF".toCharArray();

    // The modified flag is used by cPickle.
    public static String PyUnicode_EncodeRawUnicodeEscape(String str, String errors, 
                  boolean modifed) {

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
            } else
                v.append(ch);
        }

        return v.toString();
    }


    public static String PyUnicode_DecodeRawUnicodeEscape(String str, String errors) {
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

    public static void encoding_error(String type, StringBuffer dest, String errors, String details) {
        if (errors == null || errors == "strict")
            throw Py.UnicodeError(type + " encoding error: " + details);
        else if (errors == "ignore") { }
        else if (errors == "replace") 
            dest.append('?');
        else
            throw Py.ValueError(type + " encoding error; unknown error handling code: " + errors);
    }




    public static void decoding_error(String type, StringBuffer dest, String errors, String details) {
        if (errors == null || errors == "strict")
            throw Py.UnicodeError(type + " decoding error: " + details);
        else if (errors == "ignore") { }
        else if (errors == "replace") {
            if (dest != null)
                dest.append(Py_UNICODE_REPLACEMENT_CHARACTER);
        } else
            throw Py.ValueError(type + " decoding error; unknown error handling code: " + errors);
    }
}