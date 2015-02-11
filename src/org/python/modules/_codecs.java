/*
 * Copyright (c)2013 Jython Developers. Original Java version copyright 2000 Finn Bock.
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
import org.python.core.Untraversable;
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

    private static String _castString(PyString pystr) {
        // Jython used to treat String as equivalent to PyString, or maybe PyUnicode, as
        // it made sense. We need to be more careful now! Insert this cast check as necessary
        // to ensure the appropriate compliance.
        if (pystr == null) {
            return null;
        }
        String s = pystr.toString();
        if (pystr instanceof PyUnicode) {
            return s;
        } else {
            // May  throw UnicodeEncodeError, per CPython behavior
            return codecs.PyUnicode_EncodeASCII(s, s.length(), null);
        }
    }

    public static PyTuple lookup(PyString encoding) {
        return codecs.lookup(_castString(encoding));
    }

    public static PyObject lookup_error(PyString handlerName) {
        return codecs.lookup_error(_castString(handlerName));
    }

    public static void register_error(String name, PyObject errorHandler) {
        codecs.register_error(name, errorHandler);
    }

    /**
     * Decode <code>bytes</code> using the system default encoding (see
     * {@link codecs#getDefaultEncoding()}). Decoding errors raise a <code>ValueError</code>.
     *
     * @param bytes to be decoded
     * @return Unicode string decoded from <code>bytes</code>
     */
    public static PyObject decode(PyString bytes) {
        return decode(bytes, null, null);
    }

    /**
     * Decode <code>bytes</code> using the codec registered for the <code>encoding</code>. The
     * <code>encoding</code> defaults to the system default encoding (see
     * {@link codecs#getDefaultEncoding()}). Decoding errors raise a <code>ValueError</code>.
     *
     * @param bytes to be decoded
     * @param encoding name of encoding (to look up in codec registry)
     * @return Unicode string decoded from <code>bytes</code>
     */
    public static PyObject decode(PyString bytes, PyString encoding) {
        return decode(bytes, encoding, null);
    }

    /**
     * Decode <code>bytes</code> using the codec registered for the <code>encoding</code>. The
     * <code>encoding</code> defaults to the system default encoding (see
     * {@link codecs#getDefaultEncoding()}). The string <code>errors</code> may name a different
     * error handling policy (built-in or registered with {@link #register_error(String, PyObject)}
     * ). The default error policy is 'strict' meaning that decoding errors raise a
     * <code>ValueError</code>.
     *
     * @param bytes to be decoded
     * @param encoding name of encoding (to look up in codec registry)
     * @param errors error policy name (e.g. "ignore")
     * @return Unicode string decoded from <code>bytes</code>
     */
    public static PyObject decode(PyString bytes, PyString encoding, PyString errors) {
        return codecs.decode(bytes, _castString(encoding), _castString(errors));
    }

    /**
     * Encode <code>unicode</code> using the system default encoding (see
     * {@link codecs#getDefaultEncoding()}). Encoding errors raise a <code>ValueError</code>.
     *
     * @param unicode string to be encoded
     * @return bytes object encoding <code>unicode</code>
     */
    public static PyString encode(PyUnicode unicode) {
        return encode(unicode, null, null);
    }

    /**
     * Encode <code>unicode</code> using the codec registered for the <code>encoding</code>. The
     * <code>encoding</code> defaults to the system default encoding (see
     * {@link codecs#getDefaultEncoding()}). Encoding errors raise a <code>ValueError</code>.
     *
     * @param unicode string to be encoded
     * @param encoding name of encoding (to look up in codec registry)
     * @return bytes object encoding <code>unicode</code>
     */
    public static PyString encode(PyUnicode unicode, PyString encoding) {
        return encode(unicode, encoding, null);
    }

    /**
     * Encode <code>unicode</code> using the codec registered for the <code>encoding</code>. The
     * <code>encoding</code> defaults to the system default encoding (see
     * {@link codecs#getDefaultEncoding()}). The string <code>errors</code> may name a different
     * error handling policy (built-in or registered with {@link #register_error(String, PyObject)}
     * ). The default error policy is 'strict' meaning that encoding errors raise a
     * <code>ValueError</code>.
     *
     * @param unicode string to be encoded
     * @param encoding name of encoding (to look up in codec registry)
     * @param errors error policy name (e.g. "ignore")
     * @return bytes object encoding <code>unicode</code>
     */
    public static PyString encode(PyUnicode unicode, PyString encoding, PyString errors) {
        return Py.newString(codecs.encode(unicode, _castString(encoding), _castString(errors)));
    }

    /* --- Some codec support methods -------------------------------------------- */

    public static PyObject charmap_build(PyUnicode map) {
        return EncodingMap.buildEncodingMap(map);
    }

    /**
     * Enumeration representing the possible endianness of UTF-32 (possibly UTF-16) encodings.
     * Python uses integers <code>{-1, 0, 1}</code>, but we can be more expressive. For encoding
     * UNDEFINED means choose the endianness of the platform and insert a byte order mark (BOM). But
     * since the platform is Java, that is always big-endian. For decoding it means read the BOM
     * from the stream, and it is an error not to find one (compare
     * <code>Lib/encodings/utf_32.py</code>).
     */
    enum ByteOrder {
        LE, UNDEFINED, BE;

        /** Returns the Python equivalent code -1 = LE, 0 = as marked/platform, +1 = BE */
        int code() {
            return ordinal() - 1;
        }

        /** Returns equivalent to the Python code -1 = LE, 0 = as marked/platform, +1 = BE */
        static ByteOrder fromInt(int byteorder) {
            switch (byteorder) {
                case -1:
                    return LE;
                case 1:
                    return BE;
                default:
                    return UNDEFINED;
            }
        }
    }

    /**
     * Convenience method to construct the return value of decoders, providing the Unicode result as
     * a String, and the number of bytes consumed.
     *
     * @param u the unicode result as a UTF-16 Java String
     * @param bytesConsumed the number of bytes consumed
     * @return the tuple (unicode(u), bytesConsumed)
     */
    private static PyTuple decode_tuple(String u, int bytesConsumed) {
        return new PyTuple(new PyUnicode(u), Py.newInteger(bytesConsumed));
    }

    /**
     * Convenience method to construct the return value of decoders, providing the Unicode result as
     * a String, and the number of bytes consumed in decoding as either a single-element array or an
     * int to be used if the array argument is null.
     *
     * @param u the unicode result as a UTF-16 Java String
     * @param consumed if not null, element [0] is the number of bytes consumed
     * @param defConsumed if consumed==null, use this as the number of bytes consumed
     * @return the tuple (unicode(u), bytesConsumed)
     */
    private static PyTuple decode_tuple(String u, int[] consumed, int defConsumed) {
        return decode_tuple(u, consumed != null ? consumed[0] : defConsumed);
    }

    /**
     * Convenience method to construct the return value of decoders that infer the byte order from
     * the byte-order mark.
     *
     * @param u the unicode result as a UTF-16 Java String
     * @param bytesConsumed the number of bytes consumed
     * @param order the byte order (deduced by codec)
     * @return the tuple (unicode(u), bytesConsumed, byteOrder)
     */
    private static PyTuple decode_tuple(String u, int bytesConsumed, ByteOrder order) {
        int bo = order.code();
        return new PyTuple(new PyUnicode(u), Py.newInteger(bytesConsumed), Py.newInteger(bo));
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

    public static PyTuple utf_8_decode(String str, String errors, PyObject final_) {
        return utf_8_decode(str, errors, final_.__nonzero__());
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
    public static PyTuple utf_7_decode(String bytes) {
        return utf_7_decode(bytes, null);
    }

    public static PyTuple utf_7_decode(String bytes, String errors) {
        return utf_7_decode(bytes, null, false);
    }

    public static PyTuple utf_7_decode(String bytes, String errors, boolean finalFlag) {
        int[] consumed = finalFlag ? null : new int[1];
        String decoded = codecs.PyUnicode_DecodeUTF7Stateful(bytes, errors, consumed);
        return decode_tuple(decoded, consumed, bytes.length());
    }

    public static PyTuple utf_7_encode(String str) {
        return utf_7_encode(str, null);
    }

    public static PyTuple utf_7_encode(String str, String errors) {
        int size = str.length();
        return encode_tuple(codecs.PyUnicode_EncodeUTF7(str, false, false, errors), size);
    }

    /* --- string-escape Codec -------------------------------------------- */
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
    public static PyTuple charmap_decode(String bytes, String errors, PyObject mapping) {
        if (mapping == null || mapping == Py.None) {
            // Default to Latin-1
            return latin_1_decode(bytes, errors);
        } else {
            return charmap_decode(bytes, errors, mapping, false);
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

    /* --- UTF-16 Codec ------------------------------------------- */
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

        // XXX errors argument ignored: Java's codecs implement "replace"

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

    /* --- UTF-32 Codec ------------------------------------------- */

    /**
     * Encode a Unicode Java String as UTF-32 with byte order mark. (Encoding is in platform byte
     * order, which is big-endian for Java.)
     *
     * @param unicode to be encoded
     * @return tuple (encoded_bytes, unicode_consumed)
     */
    public static PyTuple utf_32_encode(String unicode) {
        return utf_32_encode(unicode, null);
    }

    /**
     * Encode a Unicode Java String as UTF-32 with byte order mark. (Encoding is in platform byte
     * order, which is big-endian for Java.)
     *
     * @param unicode to be encoded
     * @param errors error policy name or null meaning "strict"
     * @return tuple (encoded_bytes, unicode_consumed)
     */
    public static PyTuple utf_32_encode(String unicode, String errors) {
        return PyUnicode_EncodeUTF32(unicode, errors, ByteOrder.UNDEFINED);
    }

    /**
     * Encode a Unicode Java String as UTF-32 in specified byte order with byte order mark.
     *
     * @param unicode to be encoded
     * @param errors error policy name or null meaning "strict"
     * @param byteorder decoding "endianness" specified (in the Python -1, 0, +1 convention)
     * @return tuple (encoded_bytes, unicode_consumed)
     */
    public static PyTuple utf_32_encode(String unicode, String errors, int byteorder) {
        ByteOrder order = ByteOrder.fromInt(byteorder);
        return PyUnicode_EncodeUTF32(unicode, errors, order);
    }

    /**
     * Encode a Unicode Java String as UTF-32 with little-endian byte order. No byte-order mark is
     * generated.
     *
     * @param unicode to be encoded
     * @return tuple (encoded_bytes, unicode_consumed)
     */
    public static PyTuple utf_32_le_encode(String unicode) {
        return utf_32_le_encode(unicode, null);
    }

    /**
     * Encode a Unicode Java String as UTF-32 with little-endian byte order. No byte-order mark is
     * generated.
     *
     * @param unicode to be encoded
     * @param errors error policy name or null meaning "strict"
     * @return tuple (encoded_bytes, unicode_consumed)
     */
    public static PyTuple utf_32_le_encode(String unicode, String errors) {
        return PyUnicode_EncodeUTF32(unicode, errors, ByteOrder.LE);
    }

    /**
     * Encode a Unicode Java String as UTF-32 with big-endian byte order. No byte-order mark is
     * generated.
     *
     * @param unicode to be encoded
     * @return tuple (encoded_bytes, unicode_consumed)
     */
    public static PyTuple utf_32_be_encode(String unicode) {
        return utf_32_be_encode(unicode, null);
    }

    /**
     * Encode a Unicode Java String as UTF-32 with big-endian byte order. No byte-order mark is
     * generated.
     *
     * @param unicode to be encoded
     * @param errors error policy name or null meaning "strict"
     * @return tuple (encoded_bytes, unicode_consumed)
     */
    public static PyTuple utf_32_be_encode(String unicode, String errors) {
        return PyUnicode_EncodeUTF32(unicode, errors, ByteOrder.BE);
    }

    /**
     * Encode a Unicode Java String as UTF-32 in specified byte order. A byte-order mark is
     * generated if <code>order = ByteOrder.UNDEFINED</code>, and the byte order in that case will
     * be the platform default, which is BE since the platform is Java.
     * <p>
     * The input String <b>must</b> be valid UTF-16, in particular, if it contains surrogate code
     * units they must be ordered and paired correctly. The last char in <code>unicode</code> is not
     * allowed to be an unpaired surrogate. These criteria will be met if the String
     * <code>unicode</code> is the contents of a valid {@link PyUnicode} or {@link PyString}.
     *
     * @param unicode to be encoded
     * @param errors error policy name or null meaning "strict"
     * @param order byte order to use BE, LE or UNDEFINED (a BOM will be written)
     * @return tuple (encoded_bytes, unicode_consumed)
     */
    private static PyTuple PyUnicode_EncodeUTF32(String unicode, String errors, ByteOrder order) {

        // We use a StringBuilder but we are really storing encoded bytes
        StringBuilder v = new StringBuilder(4 * (unicode.length() + 1));
        int uptr = 0;

        // Write a BOM (if required to)
        if (order == ByteOrder.UNDEFINED) {
            v.append("\u0000\u0000\u00fe\u00ff");
            order = ByteOrder.BE;
        }

        if (order != ByteOrder.LE) {
            uptr = PyUnicode_EncodeUTF32BELoop(v, unicode, errors);
        } else {
            uptr = PyUnicode_EncodeUTF32LELoop(v, unicode, errors);
        }

        // XXX Issue #2002: should probably report length consumed in Unicode characters
        return encode_tuple(v.toString(), uptr);
    }

    /**
     * Helper to {@link #PyUnicode_EncodeUTF32(String, String, ByteOrder)} when big-endian encoding
     * is to be carried out.
     *
     * @param v output buffer building String of bytes (Jython PyString convention)
     * @param unicode character input
     * @param errors error policy name (e.g. "ignore", "replace")
     * @return number of Java characters consumed from unicode
     */
    private static int PyUnicode_EncodeUTF32BELoop(StringBuilder v, String unicode, String errors) {

        int len = unicode.length();
        int uptr = 0;
        char[] buf = new char[6];   // first 3 elements always zero

        /*
         * Main codec loop outputs arrays of 4 bytes at a time.
         */
        while (uptr < len) {

            int ch = unicode.charAt(uptr++);

            if ((ch & 0xF800) == 0xD800) {
                /*
                 * This is a surrogate. In Jython, unicode should always be the internal value of a
                 * PyUnicode, and since this should never contain invalid data, it should be a lead
                 * surrogate, uptr < len, and the next char must be the trail surrogate. We ought
                 * not to have to chech that, however ...
                 */
                if ((ch & 0x0400) == 0) {
                    // Yes, it's a lead surrogate
                    if (uptr < len) {
                        // And there is something to follow
                        int ch2 = unicode.charAt(uptr++);
                        if ((ch2 & 0xFC00) == 0xDC00) {
                            // And it is a trail surrogate, so we can get on with the encoding
                            ch = ((ch & 0x3ff) << 10) + (ch2 & 0x3ff) + 0x10000;
                            buf[3] = (char)((ch >> 16) & 0xff);
                            buf[4] = (char)((ch >> 8) & 0xff);
                            buf[5] = (char)(ch & 0xff);
                            v.append(buf, 2, 4);
                        } else {
                            // The trail surrogate was missing: accuse ch at uptr-2
                            uptr = PyUnicode_EncodeUTF32Error(v, errors, ByteOrder.BE, //
                                    unicode, uptr - 2, uptr - 1, "second surrogate missing");
                        }
                    } else {
                        // End of input instread of trail surrogate: accuse ch at uptr-1
                        uptr = PyUnicode_EncodeUTF32Error(v, errors, ByteOrder.BE, //
                                unicode, uptr - 1, len, "truncated data");
                    }
                } else {
                    // The trail encountered in lead position: accuse ch at uptr-2
                    uptr = PyUnicode_EncodeUTF32Error(v, errors, ByteOrder.BE, //
                            unicode, uptr - 2, uptr - 1, "unexpected second surrogate");
                }

            } else if (ch > 255) {
                // This is a BMP character: only two bytes non-zero
                buf[3] = (char)((ch >> 8) & 0xff);
                buf[4] = (char)(ch & 0xff);
                v.append(buf, 1, 4);

            } else {
                // This is one-byte BMP character: only one byte non-zero
                buf[3] = (char)(ch & 0xff);
                v.append(buf, 0, 4);
            }
        }

        // XXX Issue #2002: should probably report length consumed in Unicode characters
        return uptr;
    }

    /**
     * Helper to {@link #PyUnicode_EncodeUTF32(String, String, ByteOrder)} when big-endian encoding
     * is to be carried out.
     *
     * @param v output buffer building String of bytes (Jython PyString convention)
     * @param unicode character input
     * @param errors error policy name (e.g. "ignore", "replace")
     * @return number of Java characters consumed from unicode
     */
    private static int PyUnicode_EncodeUTF32LELoop(StringBuilder v, String unicode, String errors) {

        int len = unicode.length();
        int uptr = 0;
        char[] buf = new char[6];   // last 3 elements always zero

        /*
         * Main codec loop outputs arrays of 4 bytes at a time.
         */
        while (uptr < len) {

            int ch = unicode.charAt(uptr++);

            if ((ch & 0xF800) == 0xD800) {
                /*
                 * This is a surrogate. In Jython, unicode should always be the internal value of a
                 * PyUnicode, and since this should never contain invalid data, it should be a lead
                 * surrogate, uptr < len, and the next char must be the trail surrogate. We ought
                 * not to have to chech that, however ...
                 */
                if ((ch & 0x0400) == 0) {
                    // Yes, it's a lead surrogate
                    if (uptr < len) {
                        // And there is something to follow
                        int ch2 = unicode.charAt(uptr++);
                        if ((ch2 & 0xFC00) == 0xDC00) {
                            // And it is a trail surrogate, so we can get on with the encoding
                            ch = ((ch & 0x3ff) << 10) + (ch2 & 0x3ff) + 0x10000;
                            buf[0] = (char)(ch & 0xff);
                            buf[1] = (char)((ch >> 8) & 0xff);
                            buf[2] = (char)((ch >> 16) & 0xff);
                            v.append(buf, 0, 4);
                        } else {
                            // The trail surrogate was missing: accuse ch at uptr-2
                            uptr = PyUnicode_EncodeUTF32Error(v, errors, ByteOrder.LE, //
                                    unicode, uptr - 2, uptr - 1, "second surrogate missing");
                        }
                    } else {
                        // End of input instread of trail surrogate: accuse ch at uptr-1
                        uptr = PyUnicode_EncodeUTF32Error(v, errors, ByteOrder.LE, //
                                unicode, uptr - 1, len, "truncated data");
                    }
                } else {
                    // The trail encountered in lead position: accuse ch at uptr-2
                    uptr = PyUnicode_EncodeUTF32Error(v, errors, ByteOrder.LE, //
                            unicode, uptr - 2, uptr - 1, "unexpected second surrogate");
                }

            } else if (ch > 255) {
                // This is a BMP character: only two bytes non-zero
                buf[1] = (char)(ch & 0xff);
                buf[2] = (char)((ch >> 8) & 0xff);
                v.append(buf, 1, 4);

            } else {
                // This is one-byte BMP character: only one byte non-zero
                buf[2] = (char)(ch & 0xff);
                v.append(buf, 2, 4);
            }
        }

        // XXX Issue #2002: should probably report length consumed in Unicode characters
        return uptr;
    }

    /**
     * Specific UTF-32 encoder error handler. This is a helper called in the inner loop of
     * {@link #PyUnicode_EncodeUTF32(String, String, ByteOrder)} when the Unicode input is in valid.
     * In theory, since the input Unicode data should come from a {@link PyUnicode}, there should
     * never be any errors.
     *
     * @param v output buffer building String of bytes (Jython PyString convention)
     * @param errors error policy name (e.g. "ignore", "replace")
     * @param order LE or BE indicator
     * @param toEncode character input
     * @param start index of first problematic character
     * @param end index of character after the last problematic character
     * @param reason text contribution to the exception raised (if any)
     * @return position within input at which to restart
     */
    private static int PyUnicode_EncodeUTF32Error(StringBuilder v, String errors, ByteOrder order,
            String toEncode, int start, int end, String reason) {

        // Handle special cases locally
        if (errors != null) {
            if (errors.equals(codecs.IGNORE)) {
                // Just skip to the first non-problem byte
                return end;
            } else if (errors.equals(codecs.REPLACE)) {
                // Insert a replacement UTF-32 character(s) and skip
                for (int i = start; i < end; i++) {
                    if (order != ByteOrder.LE) {
                        v.append("\000\000\000?");
                    } else {
                        v.append("?\000\000\000");
                    }
                }
                return end;
            }
        }

        // If errors not one of those, invoke the generic mechanism
        PyObject replacementSpec =
                codecs.encoding_error(errors, "utf-32", toEncode, start, end, reason);

        // Note the replacement is unicode text that still needs to be encoded
        String u = replacementSpec.__getitem__(0).toString();
        PyUnicode_EncodeUTF32BELoop(v, u, errors);

        // Return the index in toEncode at which we should resume
        return codecs.calcNewPosition(toEncode.length(), replacementSpec);
    }

    /**
     * Decode (perhaps partially) a sequence of bytes representing the UTF-32 encoded form of a
     * Unicode string and return as a tuple the unicode text, and the amount of input consumed. The
     * endianness used will have been deduced from a byte-order mark, if present, or will be
     * big-endian (Java platform default). The unicode text is presented as a Java String (the
     * UTF-16 representation used by {@link PyUnicode}). It is an error for the input bytes not to
     * form a whole number of valid UTF-32 codes.
     *
     * @param bytes to be decoded (Jython {@link PyString} convention)
     * @return tuple (unicode_result, bytes_consumed)
     */
    public static PyTuple utf_32_decode(String bytes) {
        return utf_32_decode(bytes, null);
    }

    /**
     * Decode a sequence of bytes representing the UTF-32 encoded form of a Unicode string and
     * return as a tuple the unicode text, and the amount of input consumed. The endianness used
     * will have been deduced from a byte-order mark, if present, or will be big-endian (Java
     * platform default). The unicode text is presented as a Java String (the UTF-16 representation
     * used by {@link PyUnicode}). It is an error for the input bytes not to form a whole number of
     * valid UTF-32 codes.
     *
     * @param bytes to be decoded (Jython {@link PyString} convention)
     * @param errors error policy name (e.g. "ignore", "replace")
     * @return tuple (unicode_result, bytes_consumed)
     */
    public static PyTuple utf_32_decode(String bytes, String errors) {
        return utf_32_decode(bytes, errors, false);
    }

    /**
     * Decode (perhaps partially) a sequence of bytes representing the UTF-32 encoded form of a
     * Unicode string and return as a tuple the unicode text, and the amount of input consumed. The
     * endianness used will have been deduced from a byte-order mark, if present, or will be
     * big-endian (Java platform default). The unicode text is presented as a Java String (the
     * UTF-16 representation used by {@link PyUnicode}).
     *
     * @param bytes to be decoded (Jython {@link PyString} convention)
     * @param errors error policy name (e.g. "ignore", "replace")
     * @param isFinal if a "final" call, meaning the input must all be consumed
     * @return tuple (unicode_result, bytes_consumed)
     */
    public static PyTuple utf_32_decode(String bytes, String errors, boolean isFinal) {
        return PyUnicode_DecodeUTF32Stateful(bytes, errors, ByteOrder.UNDEFINED, isFinal, false);
    }

    /**
     * Decode a sequence of bytes representing the UTF-32 little-endian encoded form of a Unicode
     * string and return as a tuple the unicode text, and the amount of input consumed. A
     * (correctly-oriented) byte-order mark will pass as a zero-width non-breaking space. The
     * unicode text is presented as a Java String (the UTF-16 representation used by
     * {@link PyUnicode}). It is an error for the input bytes not to form a whole number of valid
     * UTF-32 codes.
     *
     * @param bytes to be decoded (Jython {@link PyString} convention)
     * @return tuple (unicode_result, bytes_consumed)
     */
    public static PyTuple utf_32_le_decode(String bytes) {
        return utf_32_le_decode(bytes, null);
    }

    /**
     * Decode a sequence of bytes representing the UTF-32 little-endian encoded form of a Unicode
     * string and return as a tuple the unicode text, and the amount of input consumed. A
     * (correctly-oriented) byte-order mark will pass as a zero-width non-breaking space. The
     * unicode text is presented as a Java String (the UTF-16 representation used by
     * {@link PyUnicode}). It is an error for the input bytes not to form a whole number of valid
     * UTF-32 codes.
     *
     * @param bytes to be decoded (Jython {@link PyString} convention)
     * @param errors error policy name (e.g. "ignore", "replace")
     * @return tuple (unicode_result, bytes_consumed)
     */
    public static PyTuple utf_32_le_decode(String bytes, String errors) {
        return utf_32_le_decode(bytes, errors, false);
    }

    /**
     * Decode (perhaps partially) a sequence of bytes representing the UTF-32 little-endian encoded
     * form of a Unicode string and return as a tuple the unicode text, and the amount of input
     * consumed. A (correctly-oriented) byte-order mark will pass as a zero-width non-breaking
     * space. The unicode text is presented as a Java String (the UTF-16 representation used by
     * {@link PyUnicode}).
     *
     * @param bytes to be decoded (Jython {@link PyString} convention)
     * @param errors error policy name (e.g. "ignore", "replace")
     * @param isFinal if a "final" call, meaning the input must all be consumed
     * @return tuple (unicode_result, bytes_consumed)
     */
    public static PyTuple utf_32_le_decode(String bytes, String errors, boolean isFinal) {
        return PyUnicode_DecodeUTF32Stateful(bytes, errors, ByteOrder.LE, isFinal, false);
    }

    /**
     * Decode a sequence of bytes representing the UTF-32 big-endian encoded form of a Unicode
     * string and return as a tuple the unicode text, and the amount of input consumed. A
     * (correctly-oriented) byte-order mark will pass as a zero-width non-breaking space. The
     * unicode text is presented as a Java String (the UTF-16 representation used by
     * {@link PyUnicode}). It is an error for the input bytes not to form a whole number of valid
     * UTF-32 codes.
     *
     * @param bytes to be decoded (Jython {@link PyString} convention)
     * @return tuple (unicode_result, bytes_consumed)
     */
    public static PyTuple utf_32_be_decode(String bytes) {
        return utf_32_be_decode(bytes, null);
    }

    /**
     * Decode a sequence of bytes representing the UTF-32 big-endian encoded form of a Unicode
     * string and return as a tuple the unicode text, and the amount of input consumed. A
     * (correctly-oriented) byte-order mark will pass as a zero-width non-breaking space. The
     * unicode text is presented as a Java String (the UTF-16 representation used by
     * {@link PyUnicode}). It is an error for the input bytes not to form a whole number of valid
     * UTF-32 codes.
     *
     * @param bytes to be decoded (Jython {@link PyString} convention)
     * @param errors error policy name (e.g. "ignore", "replace")
     * @return tuple (unicode_result, bytes_consumed)
     */
    public static PyTuple utf_32_be_decode(String bytes, String errors) {
        return utf_32_be_decode(bytes, errors, false);
    }

    /**
     * Decode (perhaps partially) a sequence of bytes representing the UTF-32 big-endian encoded
     * form of a Unicode string and return as a tuple the unicode text, and the amount of input
     * consumed. A (correctly-oriented) byte-order mark will pass as a zero-width non-breaking
     * space. Unicode string and return as a tuple the unicode text, the amount of input consumed.
     * The unicode text is presented as a Java String (the UTF-16 representation used by
     * {@link PyUnicode}).
     *
     * @param bytes to be decoded (Jython {@link PyString} convention)
     * @param errors error policy name (e.g. "ignore", "replace")
     * @param isFinal if a "final" call, meaning the input must all be consumed
     * @return tuple (unicode_result, bytes_consumed)
     */
    public static PyTuple utf_32_be_decode(String bytes, String errors, boolean isFinal) {
        return PyUnicode_DecodeUTF32Stateful(bytes, errors, ByteOrder.BE, isFinal, false);
    }

    /**
     * Decode a sequence of bytes representing the UTF-32 encoded form of a Unicode string and
     * return as a tuple the unicode text, the amount of input consumed, and the decoding
     * "endianness" used (in the Python -1, 0, +1 convention). The endianness, if not unspecified
     * (=0), will be deduced from a byte-order mark and returned. (This codec entrypoint is used in
     * that way in the <code>utf_32.py</code> codec, but only until the byte order is known.) When
     * not defined by a BOM, processing assumes big-endian coding (Java platform default), but
     * returns "unspecified". (The <code>utf_32.py</code> codec treats this as an error, once more
     * than 4 bytes have been processed.) (Java platform default). The unicode text is presented as
     * a Java String (the UTF-16 representation used by {@link PyUnicode}).
     *
     * @param bytes to be decoded (Jython {@link PyString} convention)
     * @param errors error policy name (e.g. "ignore", "replace")
     * @param byteorder decoding "endianness" specified (in the Python -1, 0, +1 convention)
     * @return tuple (unicode_result, bytes_consumed, endianness)
     */
    public static PyTuple utf_32_ex_decode(String bytes, String errors, int byteorder) {
        return utf_32_ex_decode(bytes, errors, byteorder, false);
    }

    /**
     * Decode (perhaps partially) a sequence of bytes representing the UTF-32 encoded form of a
     * Unicode string and return as a tuple the unicode text, the amount of input consumed, and the
     * decoding "endianness" used (in the Python -1, 0, +1 convention). The endianness will be that
     * specified, will have been deduced from a byte-order mark, if present, or will be big-endian
     * (Java platform default). Or it may still be undefined if fewer than 4 bytes are presented.
     * (This codec entrypoint is used in the utf-32 codec only untile the byte order is known.) The
     * unicode text is presented as a Java String (the UTF-16 representation used by
     * {@link PyUnicode}).
     *
     * @param bytes to be decoded (Jython {@link PyString} convention)
     * @param errors error policy name (e.g. "ignore", "replace")
     * @param byteorder decoding "endianness" specified (in the Python -1, 0, +1 convention)
     * @param isFinal if a "final" call, meaning the input must all be consumed
     * @return tuple (unicode_result, bytes_consumed, endianness)
     */
    public static PyTuple utf_32_ex_decode(String bytes, String errors, int byteorder,
            boolean isFinal) {
        ByteOrder order = ByteOrder.fromInt(byteorder);
        return PyUnicode_DecodeUTF32Stateful(bytes, errors, order, isFinal, true);
    }

    /**
     * Decode (perhaps partially) a sequence of bytes representing the UTF-32 encoded form of a
     * Unicode string and return as a tuple the (Jython internal representation of) the unicode
     * text, the amount of input consumed, and if requested, the decoding "endianness" used (in
     * Python -1, 0, +1 conventions). The state we preserve is our read position, i.e. how many
     * bytes we have consumed and the byte order (endianness). If the input ends part way through a
     * UTF-32 sequence (4 bytes) the data reported as consumed is just that up to and not including
     * the first of these bytes. The Java String in the returned tuple is a UTF-16 representation of
     * the Unicode result, in line with Java conventions, where Unicode characters above the BMP are
     * represented as surrogate pairs.
     *
     * @param bytes input represented as String (Jython PyString convention)
     * @param errors error policy name (e.g. "ignore", "replace")
     * @param order LE, BE or UNDEFINED (meaning bytes may begin with a byte order mark)
     * @param isFinal if a "final" call, meaning the input must all be consumed
     * @param findOrder if the returned tuple should include a report of the byte order
     * @return tuple (unicode_result, bytes_consumed [, endianness])
     */
    private static PyTuple PyUnicode_DecodeUTF32Stateful(String bytes, String errors,
            ByteOrder order, boolean isFinal, boolean findOrder) {

        int size = bytes.length();  // Number of bytes waiting (not necessarily multiple of 4)
        int limit = size & ~0x3;    // First index at which fewer than 4 bytes will be available

        // Output Unicode characters will build up here (as UTF-16:
        StringBuilder unicode = new StringBuilder(1 + limit / 4);
        int q = 0;                  // Read pointer in bytes

        if (limit > 0) {
            /*
             * Check for BOM (U+FEFF) in the input and adjust current byte order setting
             * accordingly. If we know the byte order (it is LE or BE) then bytes ressembling a byte
             * order mark are actually a ZERO WIDTH NON-BREAKING SPACE and will be passed through to
             * the output in the main codec loop as such.
             */
            if (order == ByteOrder.UNDEFINED) {
                /*
                 * The byte order is not known. If the first 4 bytes is a BOM for LE or BE, that
                 * will set the byte order and the BOM will not be copied to the output. Otherwise
                 * these bytes are data and will be left for the main codec loop to consume.
                 */
                char a = bytes.charAt(q);
                if (a == 0xff) {
                    if (bytes.charAt(q + 1) == 0xfe && bytes.charAt(q + 2) == 0
                            && bytes.charAt(q + 3) == 0) {
                        // Somebody set up us the BOM (0xff 0xfe 0x00 0x00) - LE
                        order = ByteOrder.LE;
                        q += 4;
                    }

                } else if (a == 0) {
                    if (bytes.charAt(q + 1) == 0 && bytes.charAt(q + 2) == 0xfe
                            && bytes.charAt(q + 3) == 0xff) {
                        // Other (big-endian) BOM (0x00 0x00 0xfe 0xff) - already set BE
                        order = ByteOrder.BE;
                        q += 4;
                    }
                }
                /*
                 * If no BOM found, order is still undefined. This is an error to utf_32.py, but
                 * here is treated as big-endian.
                 */
            }

            /*
             * Main codec loop consumes 4 bytes and emits one code point with each pass, until there
             * are fewer than 4 bytes left. There's a version for each endianness
             */
            if (order != ByteOrder.LE) {
                q = PyUnicode_DecodeUTF32BELoop(unicode, bytes, q, limit, errors);
            } else {
                q = PyUnicode_DecodeUTF32LELoop(unicode, bytes, q, limit, errors);
            }

        }

        /*
         * We have processed all we can: if we have some bytes left over that we can't store for
         * next time, that's an error.
         */
        if (isFinal && q < size) {
            q = codecs.insertReplacementAndGetResume(unicode, errors, "utf-32", //
                    bytes, q, size, "truncated data");
        }

        // Finally, the return depends whether we were asked to work out the byte order
        if (findOrder) {
            return decode_tuple(unicode.toString(), q, order);
        } else {
            return decode_tuple(unicode.toString(), q);
        }
    }

    /**
     * Helper to {@link #PyUnicode_DecodeUTF32Stateful(String, String, ByteOrder, boolean, boolean)}
     * when big-endian decoding is to be carried out.
     *
     * @param unicode character output
     * @param bytes input represented as String (Jython PyString convention)
     * @param q number of elements already consumed from <code>bytes</code> array
     * @param limit (multiple of 4) first byte not to process
     * @param errors error policy name (e.g. "ignore", "replace")
     * @return number of elements consumed now from <code>bytes</code> array
     */
    private static int PyUnicode_DecodeUTF32BELoop(StringBuilder unicode, String bytes, int q,
            int limit, String errors) {

        /*
         * Main codec loop consumes 4 bytes and emits one code point with each pass, until there are
         * fewer than 4 bytes left.
         */
        while (q < limit) {
            // Read 4 bytes in two 16-bit chunks according to byte order
            int hi, lo;
            hi = (bytes.charAt(q) << 8) | bytes.charAt(q + 1);
            lo = (bytes.charAt(q + 2) << 8) | bytes.charAt(q + 3);

            if (hi == 0) {
                // It's a BMP character so we can't go wrong
                unicode.append((char)lo);
                q += 4;
            } else {
                // Code may be invalid: let the appendCodePoint method detect that
                try {
                    unicode.appendCodePoint((hi << 16) + lo);
                    q += 4;
                } catch (IllegalArgumentException e) {
                    q = codecs.insertReplacementAndGetResume(unicode, errors, "utf-32", //
                            bytes, q, q + 4, "codepoint not in range(0x110000)");
                }
            }
        }

        return q;
    }

    /**
     * Helper to {@link #PyUnicode_DecodeUTF32Stateful(String, String, ByteOrder, boolean, boolean)}
     * when little-endian decoding is to be carried out.
     *
     * @param unicode character output
     * @param bytes input represented as String (Jython PyString convention)
     * @param q number of elements already consumed from <code>bytes</code> array
     * @param limit (multiple of 4) first byte not to process
     * @param errors error policy name (e.g. "ignore", "replace")
     * @return number of elements consumed now from <code>bytes</code> array
     */
    private static int PyUnicode_DecodeUTF32LELoop(StringBuilder unicode, String bytes, int q,
            int limit, String errors) {
        /*
         * Main codec loop consumes 4 bytes and emits one code point with each pass, until there are
         * fewer than 4 bytes left.
         */
        while (q < limit) {
            // Read 4 bytes in two 16-bit chunks according to byte order
            int hi, lo;
            hi = (bytes.charAt(q + 3) << 8) | bytes.charAt(q + 2);
            lo = (bytes.charAt(q + 1) << 8) | bytes.charAt(q);

            if (hi == 0) {
                // It's a BMP character so we can't go wrong
                unicode.append((char)lo);
                q += 4;
            } else {
                // Code may be invalid: let the appendCodePoint method detect that
                try {
                    unicode.appendCodePoint((hi << 16) + lo);
                    q += 4;
                } catch (IllegalArgumentException e) {
                    q = codecs.insertReplacementAndGetResume(unicode, errors, "utf-32", //
                            bytes, q, q + 4, "codepoint not in range(0x110000)");
                }
            }
        }

        return q;
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

    /* --- unicode-escape Codec ------------------------------------------- */
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

    /*
     * This codec is supposed to deal with an encoded form equal to the internal representation of
     * the unicode object considered as bytes in memory. This was confusing in CPython as it varied
     * with machine architecture (width and endian-ness). In Jython, where both are fixed, the most
     * compatible choice is UTF-32BE. The codec is deprecated in v3.3 as irrelevant, or impossible,
     * in view of the flexible string representation (which Jython emulates in its own way).
     *
     * See http://mail.python.org/pipermail/python-dev/2011-November/114415.html
     */
    /**
     * Legacy method to encode given unicode in CPython wide-build internal format (equivalent
     * UTF-32BE).
     */
    @Deprecated
    public static PyTuple unicode_internal_encode(String unicode) {
        return utf_32_be_encode(unicode, null);
    }

    /**
     * Legacy method to encode given unicode in CPython wide-build internal format (equivalent
     * UTF-32BE). There must be a multiple of 4 bytes.
     */
    @Deprecated
    public static PyTuple unicode_internal_encode(String unicode, String errors) {
        return utf_32_be_encode(unicode, errors);
    }

    /**
     * Legacy method to decode given bytes as if CPython wide-build internal format (equivalent
     * UTF-32BE). There must be a multiple of 4 bytes.
     */
    @Deprecated
    public static PyTuple unicode_internal_decode(String bytes) {
        return utf_32_be_decode(bytes, null, true);
    }

    /**
     * Legacy method to decode given bytes as if CPython wide-build internal format (equivalent
     * UTF-32BE). There must be a multiple of 4 bytes.
     */
    @Deprecated
    public static PyTuple unicode_internal_decode(String bytes, String errors) {
        return utf_32_be_decode(bytes, errors, true);
    }

    /**
     * Optimized charmap encoder mapping.
     *
     * Uses a trie structure instead of a dictionary; the speedup primarily comes from not creating
     * integer objects in the process. The trie is created by inverting the encoding map.
     */
    @Untraversable
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
