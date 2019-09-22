/*
 * Copyright (c)2013 Jython Developers. Original Java version copyright 2000 Finn Bock.
 *
 * This program contains material copyrighted by: Copyright (c) Corporation for National Research
 * Initiatives. Originally written by Marc-Andre Lemburg (mal@lemburg.com).
 */
package org.python.core;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;

import org.python.core.util.StringUtil;
import org.python.modules._codecs;

/**
 * This class implements the codec registry and utility methods supporting codecs, such as those
 * providing the standard replacement strategies ("ignore", "backslashreplace", etc.). The _codecs
 * module relies heavily on apparatus implemented here, and therefore so does the Python
 * <code>codecs</code> module (in <code>Lib/codecs.py</code>). It corresponds approximately to
 * CPython's <code>Python/codecs.c</code>.
 * <p>
 * The class also contains the inner methods of the standard Unicode codecs, available for
 * transcoding of text at the Java level. These also are exposed through the <code>_codecs</code>
 * module. In CPython, the implementations are found in <code>Objects/unicodeobject.c</code>.
 *
 * @since Jython 2.0
 */
public class codecs {

    public static final String BACKSLASHREPLACE = "backslashreplace";
    public static final String IGNORE = "ignore";
    public static final String REPLACE = "replace";
    public static final String XMLCHARREFREPLACE = "xmlcharrefreplace";
    private static char Py_UNICODE_REPLACEMENT_CHARACTER = 0xFFFD;

    public static String getDefaultEncoding() {
        return Py.getSystemState().getCodecState().getDefaultEncoding();
    }

    public static void setDefaultEncoding(String encoding) {
        Py.getSystemState().getCodecState().setDefaultEncoding(encoding);
    }

    public static PyObject lookup_error(String handlerName) {
        return Py.getSystemState().getCodecState().lookup_error(handlerName);
    }

    public static void register_error(String name, PyObject error) {
        Py.getSystemState().getCodecState().register_error(name, error);
    }

    public static void register(PyObject search_function) {
        Py.getSystemState().getCodecState().register(search_function);
    }

    public static PyTuple lookup(String encoding) {
        return Py.getSystemState().getCodecState().lookup(encoding);
    }

    private static String normalizestring(String string) {
        return string.toLowerCase().replace(' ', '-');
    }

    /**
     * Decode the bytes <code>v</code> using the codec registered for the <code>encoding</code>. The
     * <code>encoding</code> defaults to the system default encoding (see
     * {@link codecs#getDefaultEncoding()}). The string <code>errors</code> may name a different
     * error handling policy (built-in or registered with
     * {@link #register_error(String, PyObject)}). The default error policy is 'strict' meaning that
     * encoding errors raise a <code>ValueError</code>. This method is exposed through the _codecs
     * module as {@link _codecs#decode(PyString, PyString, PyString)}
     *
     * @param v bytes to be decoded
     * @param encoding name of encoding (to look up in codec registry)
     * @param errors error policy name (e.g. "ignore", "replace")
     * @return Unicode string decoded from <code>bytes</code>
     */
    public static PyObject decode(PyString v, String encoding, String errors) {
        if (encoding == null) {
            encoding = getDefaultEncoding();
        } else {
            encoding = normalizestring(encoding);
        }

        if (errors != null) {
            errors = errors.intern();
        }

        /* Shortcut for ascii encoding */
        if (encoding.equals("ascii")) {
            return wrapDecodeResult(PyUnicode_DecodeASCII(v.toString(), v.__len__(), errors));
        }

        /* Decode via the codec registry */
        PyObject decoder;
        try {
            decoder = lookup(encoding).__getitem__(1);
        } catch (PyException ex) {
            if (ex.match(Py.LookupError)) {
                // If we couldn't find an encoding, see if we have a builtin
                if (encoding.equals("utf-8")) {
                    return wrapDecodeResult(PyUnicode_DecodeUTF8(v.toString(), errors));
                } else if (encoding.equals("utf-7")) {
                    return wrapDecodeResult(PyUnicode_DecodeUTF7(v.toString(), errors));
                } else if (encoding.equals("latin-1")) {
                    return wrapDecodeResult(PyUnicode_DecodeLatin1(v.toString(), v.__len__(),
                            errors));
                }
            }
            throw ex;
        }
        PyObject result;
        if (errors != null) {
            result = decoder.__call__(v, new PyString(errors));
        } else {
            result = decoder.__call__(v);
        }

        if (!(result instanceof PyTuple) || result.__len__() != 2) {
            throw Py.TypeError("decoder must return a tuple (object,integer)");
        }
        return result.__getitem__(0);
    }

    private static PyUnicode wrapDecodeResult(String result) {
        return new PyUnicode(result);
    }

    /**
     * Encode <code>v</code> using the codec registered for the <code>encoding</code>.
     * The <code>encoding</code> defaults to the system default encoding
     * (see {@link codecs#getDefaultEncoding()}).
     * The string <code>errors</code> may name a different error handling
     * policy (built-in or registered with {@link #register_error(String, PyObject)}).
     * The default error policy is 'strict' meaning that encoding errors raise a
     * <code>ValueError</code>.
     *
     * @param v unicode string to be encoded
     * @param encoding name of encoding (to look up in codec registry)
     * @param errors error policy name (e.g. "ignore")
     * @return bytes object encoding <code>v</code>
     */
    // XXX v should probably be declared PyUnicode (or thing delivering unicode code points)
    public static String encode(PyString v, String encoding, String errors) {
        if (encoding == null) {
            encoding = getDefaultEncoding();
        } else {
            encoding = normalizestring(encoding);
        }

        if (errors != null) {
            errors = errors.intern();
        }

        /*
         * Shortcuts for common default encodings. latin-1 must not use the lookup registry for the
         * encodings module to work correctly
         */
        if (encoding.equals("latin-1")) {
            return PyUnicode_EncodeLatin1(v.toString(), v.__len__(), errors);
        } else if (encoding.equals("ascii")) {
            return PyUnicode_EncodeASCII(v.toString(), v.__len__(), errors);
        }

        /* Encode via the codec registry */
        PyObject encoder;
        try {
            encoder = lookup(encoding).__getitem__(0);
        } catch (PyException ex) {
            if (ex.match(Py.LookupError)) {
                // If we couldn't find an encoding, see if we have a builtin
                if (encoding.equals("utf-8")) {
                    return PyUnicode_EncodeUTF8(v.toString(), errors);
                } else if (encoding.equals("utf-7")) {
                    return codecs.PyUnicode_EncodeUTF7(v.toString(), false, false, errors);
                }
            }
            throw ex;
        }
        PyObject result;
        if (errors != null) {
            result = encoder.__call__(v, new PyString(errors));
        } else {
            result = encoder.__call__(v);
        }

        if (!(result instanceof PyTuple) || result.__len__() != 2) {
            throw Py.TypeError("encoder must return a tuple (object,integer)");
        }
        PyObject encoded = result.__getitem__(0);
        if (encoded instanceof PyString) {
            return encoded.toString();
        } else {
            throw Py.TypeError("encoder did not return a string/unicode object (type="
                    + encoded.getType().fastGetName() + ")");
        }
    }

    public static PyObject strict_errors(PyObject[] args, String[] kws) {
        ArgParser ap = new ArgParser("strict_errors", args, kws, "exc");
        PyObject exc = ap.getPyObject(0);
        if (Py.isInstance(exc, Py.UnicodeDecodeError)) {
            throw new PyException(Py.UnicodeDecodeError, exc);
        } else if (Py.isInstance(exc, Py.UnicodeEncodeError)) {
            throw new PyException(Py.UnicodeEncodeError, exc);
        } else if (Py.isInstance(exc, Py.UnicodeTranslateError)) {
            throw new PyException(Py.UnicodeTranslateError, exc);
        }
        throw wrong_exception_type(exc);
    }

    public static PyObject ignore_errors(PyObject[] args, String[] kws) {
        ArgParser ap = new ArgParser("ignore_errors", args, kws, "exc");
        PyObject exc = ap.getPyObject(0);
        if (!isUnicodeError(exc)) {
            throw wrong_exception_type(exc);
        }
        PyObject end = exc.__getattr__("end");
        return new PyTuple(Py.EmptyUnicode, end);
    }

    private static boolean isUnicodeError(PyObject exc) {
        return Py.isInstance(exc, Py.UnicodeDecodeError)
                || Py.isInstance(exc, Py.UnicodeEncodeError)
                || Py.isInstance(exc, Py.UnicodeTranslateError);
    }

    public static PyObject replace_errors(PyObject[] args, String[] kws) {
        ArgParser ap = new ArgParser("replace_errors", args, kws, "exc");
        PyObject exc = ap.getPyObject(0);
        if (Py.isInstance(exc, Py.UnicodeEncodeError)) {
            int end = exceptions.getEnd(exc, true);
            return new PyTuple(new PyUnicode("?"), Py.newInteger(end));
        } else if (Py.isInstance(exc, Py.UnicodeDecodeError)) {
            int end = exceptions.getEnd(exc, false);
            return new PyTuple(new PyUnicode(Py_UNICODE_REPLACEMENT_CHARACTER), Py.newInteger(end));
        } else if (Py.isInstance(exc, Py.UnicodeTranslateError)) {
            int end = exceptions.getEnd(exc, true);
            return new PyTuple(new PyUnicode(Py_UNICODE_REPLACEMENT_CHARACTER), Py.newInteger(end));
        }
        throw wrong_exception_type(exc);
    }

    public static PyObject xmlcharrefreplace_errors(PyObject[] args, String[] kws) {
        ArgParser ap = new ArgParser("xmlcharrefreplace_errors", args, kws, "exc");
        PyObject exc = ap.getPyObject(0);
        if (!Py.isInstance(exc, Py.UnicodeEncodeError)) {
            throw wrong_exception_type(exc);
        }
        int start = ((PyInteger)exc.__getattr__("start")).getValue();
        int end = ((PyInteger)exc.__getattr__("end")).getValue();
        String object = exc.__getattr__("object").toString();
        StringBuilder replacement = new StringBuilder();
        xmlcharrefreplace_internal(start, end, object, replacement);
        return new PyTuple(Py.java2py(replacement.toString()), exc.__getattr__("end"));
    }

    public static StringBuilder xmlcharrefreplace(int start, int end, String toReplace) {
        StringBuilder replacement = new StringBuilder();
        xmlcharrefreplace_internal(start, end, toReplace, replacement);
        return replacement;
    }

    private static void xmlcharrefreplace_internal(int start, int end, String object,
            StringBuilder replacement) {
        for (int i = start; i < end; i++) {
            replacement.append("&#");
            char cur = object.charAt(i);
            int digits;
            int base;
            if (cur < 10) {
                digits = 1;
                base = 1;
            } else if (cur < 100) {
                digits = 2;
                base = 10;
            } else if (cur < 1000) {
                digits = 3;
                base = 100;
            } else if (cur < 10000) {
                digits = 4;
                base = 1000;
            } else if (cur < 100000) {
                digits = 5;
                base = 10000;
            } else if (cur < 1000000) {
                digits = 6;
                base = 100000;
            } else {
                digits = 7;
                base = 1000000;
            }
            while (digits-- > 0) {
                replacement.append((char)('0' + cur / base));
                cur %= base;
                base /= 10;
            }
            replacement.append(';');
        }
    }

    private static PyException wrong_exception_type(PyObject exc) {
        PyObject excClass = exc.__getattr__("__class__");
        PyObject className = excClass.__getattr__("__name__");
        return new PyException(Py.TypeError, "Don't know how to handle " + className
                + " in error callback");
    }

    static char hexdigits[] = {//@formatter:off
        '0', '1', '2', '3', '4', '5', '6', '7',
        '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
    }; //@formatter:on

    public static PyObject backslashreplace_errors(PyObject[] args, String[] kws) {
        ArgParser ap = new ArgParser("backslashreplace_errors", args, kws, "exc");
        PyObject exc = ap.getPyObject(0);
        if (!Py.isInstance(exc, Py.UnicodeEncodeError)) {
            throw wrong_exception_type(exc);
        }
        int start = ((PyInteger)exc.__getattr__("start")).getValue();
        int end = ((PyInteger)exc.__getattr__("end")).getValue();
        String object = exc.__getattr__("object").toString();
        StringBuilder replacement = new StringBuilder();
        backslashreplace_internal(start, end, object, replacement);
        return new PyTuple(Py.java2py(replacement.toString()), exc.__getattr__("end"));
    }

    public static StringBuilder backslashreplace(int start, int end, String toReplace) {
        StringBuilder replacement = new StringBuilder();
        backslashreplace_internal(start, end, toReplace, replacement);
        return replacement;
    }

    private static void backslashreplace_internal(int start, int end, String object,
            StringBuilder replacement) {
        for (Iterator<Integer> iter = new StringSubsequenceIterator(object, start, end, 1); iter
                .hasNext();) {
            int c = iter.next();
            replacement.append('\\');
            if (c >= 0x00010000) {
                replacement.append('U');
                replacement.append(hexdigits[(c >> 28) & 0xf]);
                replacement.append(hexdigits[(c >> 24) & 0xf]);
                replacement.append(hexdigits[(c >> 20) & 0xf]);
                replacement.append(hexdigits[(c >> 16) & 0xf]);
                replacement.append(hexdigits[(c >> 12) & 0xf]);
                replacement.append(hexdigits[(c >> 8) & 0xf]);
            } else if (c >= 0x100) {
                replacement.append('u');
                replacement.append(hexdigits[(c >> 12) & 0xf]);
                replacement.append(hexdigits[(c >> 8) & 0xf]);
            } else {
                replacement.append('x');
            }
            replacement.append(hexdigits[(c >> 4) & 0xf]);
            replacement.append(hexdigits[c & 0xf]);
        }
    }

    /* --- UTF-7 Codec -------------------------------------------------------- */

    /*
     * This codec was converted to Java from the CPython v2.7.3 final. See RFC2152 for details of
     * the encoding scheme. We encode conservatively and decode liberally.
     */

    /* //@formatter:off
     * The UTF-7 encoder treats ASCII characters differently according to whether they are Set D,
     * Set O, Whitespace, or special (i.e. none of the above). See RFC2152. This array identifies
     * these different sets:
     * 0 : "Set D"
     *     alphanumeric and '(),-./:?
     * 1 : "Set O"
     *     !"#$%&*;<=>@[]^_`{|}
     * 2 : "whitespace"
     *     ht nl cr sp
     * 3 : special (must be base64 encoded)
     *     everything else (i.e. +\~ and non-printing codes 0-8 11-12 14-31 127)
     */
    private static final byte[] utf7_category = {
    /* nul soh stx etx eot enq ack bel bs  ht  nl  vt  np  cr  so  si  */
        3,  3,  3,  3,  3,  3,  3,  3,  3,  2,  2,  3,  3,  2,  3,  3,
    /* dle dc1 dc2 dc3 dc4 nak syn etb can em  sub esc fs  gs  rs  us  */
        3,  3,  3,  3,  3,  3,  3,  3,  3,  3,  3,  3,  3,  3,  3,  3,
    /* sp   !   "   #   $   %   &   '   (   )   *   +   ,   -   .   /  */
        2,  1,  1,  1,  1,  1,  1,  0,  0,  0,  1,  3,  0,  0,  0,  0,
    /*  0   1   2   3   4   5   6   7   8   9   :   ;   <   =   >   ?  */
        0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  1,  1,  1,  1,  0,
    /*  @   A   B   C   D   E   F   G   H   I   J   K   L   M   N   O  */
        1,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,
    /*  P   Q   R   S   T   U   V   W   X   Y   Z   [   \   ]   ^   _  */
        0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  1,  3,  1,  1,  1,
    /*  `   a   b   c   d   e   f   g   h   i   j   k   l   m   n   o  */
        1,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,
    /*  p   q   r   s   t   u   v   w   x   y   z   {   |   }   ~  del */
        0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  1,  1,  1,  3,  3,
    };//@formatter:on

    /**
     * Determine whether, in the UTF-7 encoder, this character should be encoded as itself. The
     * answer depends on whether we are encoding set O (optional special characters) as itself, and
     * also on whether we are encoding whitespace as itself. RFC2152 makes it clear that the answers
     * to these questions vary between applications, so this code needs to be flexible.
     *
     * @param c code point of the character
     * @param directO true if characters in "set O" may be encoded as themselves
     * @param directWS true if whitespace characters may be encoded as themselves
     * @return {@code true} if {@code c} should be encoded as itself
     */
    private static boolean ENCODE_DIRECT(int c, boolean directO, boolean directWS) {

        if (c >= 128 || c < 0) {
            return false;   // Character not in table is always special
        } else {
            switch (utf7_category[c]) {
                case 0:     // This is a regular character
                    return true;
                case 1:     // This is a white space character
                    return directWS;
                case 2:     // This is an optional special character
                    return directO;
                default:    // This is always a special character (including '+')
                    return false;
            }
        }
    }

    /** Look-up for the Base64 encoded byte [0..0x3f] */
    private static final String B64_CHARS =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";

    /** What is the Base64 encoded byte for (the bottom 6 bits of) n? */
    private static char TO_BASE64(int n) {
        return B64_CHARS.charAt(n & 0x3f);
    }

    /**
     * Is c the code point of a Base64 character? And if so, what is the 6-bit quantity to be
     * decodec from c? Return the 6-bit equivalent of c in a Base64 segment, -1 if it cannot be used
     * in a Base64 segment, and -2 for the special case of '-' ending the segment.
     */
    private static int FROM_BASE64(int c) {
        return (c >= 128) ? -1 : BASE64_VALUE[c];
    }

    /**
     * Look-up table to convert ASCII byte to 6-bit Base64 value, -1 if not Base64, and -2 if
     * special terminator '-'.
     */
    private static final byte[] BASE64_VALUE = {//@formatter:off
    // nul soh stx etx eot enq ack bel  bs  ht  nl  vt  np  cr  so  si
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
    // dle dc1 dc2 dc3 dc4 nak syn etb can  em sub esc  fs  gs  rs  us
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
    //  sp   !   "   #   $   %   &   '   (   )   *   +   ,   -   .   /
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 62, -1, -2, -1, 63,
    //   0   1   2   3   4   5   6   7   8   9   :   ;   <   =   >   ?
        52, 53, 54, 55, 56, 57, 58, 59, 60, 61, -1, -1, -1, -1, -1, -1,
    //   @   A   B   C   D   E   F   G   H   I   J   K   L   M   N   O
        -1,  0,  1,  2,  3,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14,
    //   P   Q   R   S   T   U   V   W   X   Y   Z   [   \   ]   ^   _
        15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, -1, -1, -1, -1, -1,
    //   `   a   b   c   d   e   f   g   h   i   j   k   l   m   n   o
        -1, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40,
    //   p   q   r   s   t   u   v   w   x   y   z   {   |   }   ~ del
        41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, -1, -1, -1, -1, -1,
    };//@formatter:on

    /**
     * Enumeration of the error causes during decoding of the Base64 segment of UTF-7
     */
    static enum UTF7Error {
        NONE("No error"),                                               // No error
        PADDING("non-zero padding bits in shift sequence"),             // Error when at end
        PARTIAL("partial character in shift sequence"),                 // Error when at end
        TRUNCATED("second surrogate missing at end of shift sequence"), // Error when at end
        MISSING("second surrogate missing"),  // Lead surrogate followed by another, or BMP
        TRAIL("unexpected second surrogate"); // Trail surrogate not preceded by lead

        /** Suitable error message */
        final String msg;

        private UTF7Error(String msg) {
            this.msg = msg;
        }
    }

    /**
     * Decode (perhaps partially) a sequence of bytes representing the UTF-7 encoded form of a
     * Unicode string and return the (Jython internal representation of) the unicode object, and
     * amount of input consumed. The only state we preserve is our read position, i.e. how many
     * bytes we have consumed. So if the input ends part way through a Base64 sequence the data
     * reported as consumed is just that up to and not including the Base64 start marker ('+').
     * Performance will be poor (quadratic cost) on runs of Base64 data long enough to exceed the
     * input quantum in incremental decoding. The returned Java String is a UTF-16 representation of
     * the Unicode result, in line with Java conventions. Unicode characters above the BMP are
     * represented as surrogate pairs.
     *
     * @param bytes input represented as String (Jython PyString convention)
     * @param errors error policy name (e.g. "ignore", "replace")
     * @param consumed returns number of bytes consumed in element 0, or is null if a "final" call
     * @return unicode result (as UTF-16 Java String)
     */
    public static String PyUnicode_DecodeUTF7Stateful(String bytes, String errors, int[] consumed) {
        int s;                      // Index in the input bytes
        boolean inBase64 = false;   // Whether s is currently in a Base64 segment
        long base64buffer = 0;      // Stored bits buffer during Base64 decoding
        int base64bits = 0;         // Number of valid bits buffered during Base64 decoding
        int startInBytes = 0;       // Place in input bytes where most recent Base64 segment begins
        int syncInBytes = 0;        // Place in input bytes where stored bits buffer last empty
        int startInUnicode = 0;     // Place in output unicode where most recent Base64 segment begins

        int size = bytes.length();
        StringBuilder unicode = new StringBuilder(size);

        for (s = 0; s < size; s++) { // In error cases s may skip forwards in bytes

            // Next byte to process
            int b = bytes.charAt(s);

            if (b >= 128) {
                // The input was supposed to be 7-bit clean
                s = insertReplacementAndGetResume(unicode, errors, "utf-7", //
                        bytes, s, s + 1, "unexpected special character") - 1;

            } else if (inBase64) {
                // We are currently processing a Base64 section

                if (base64bits == 0) {
                    // Mark this point as latest easy error recovery point (bits buffer empty)
                    syncInBytes = s;
                }

                int sixBits = FROM_BASE64(b);   // returns -ve if not Base64
                if (sixBits >= 0) {
                    // And we continue processing a Base64 section
                    base64buffer = (base64buffer << 6) | sixBits;
                    base64bits += 6;

                    if (base64bits >= 32) {
                        // We have enough bits for a code point
                        base64bits = emitCodePoints(unicode, base64buffer, base64bits);

                        if (base64bits >= 32) {
                            // We stopped prematurely. Why?
                            UTF7Error error = emitCodePointsDiagnosis(base64buffer, base64bits);
                            // Difficult to know exactly what input characters to blame
                            s = insertReplacementAndGetResume(unicode, errors, "utf-7", //
                                    bytes, syncInBytes, s + 1, error.msg) - 1;
                            // Discard one UTF-16 output and hope for the best
                            base64bits -= 16;
                        }

                    }

                } else {
                    // We are now leaving a Base64 section
                    inBase64 = false;

                    // We should have a whole number of code points and < 6 bits zero padding
                    if (base64bits > 0) {
                        // Try to emit them all
                        base64bits = emitCodePoints(unicode, base64buffer, base64bits);
                        // Now check for errors
                        UTF7Error error = emitCodePointsDiagnosis(base64buffer, base64bits);
                        if (error != UTF7Error.NONE) {
                            // Difficult to know exactly what input characters to blame
                            s = insertReplacementAndGetResume(unicode, errors, "utf-7", //
                                    bytes, s, s + 1, error.msg) - 1;
                        }
                        // We are, in any case, discarding whatever is in the buffer
                        base64bits = 0;
                    }

                    if (b == '-') {
                        /*
                         * '-' signals the end of Base64. The byte is is simply absorbed, but in the
                         * special case where it is the first byte of the Base64 segment, the
                         * zero-length segment '+-' actually encodes "+".
                         */
                        if (s == startInBytes + 1) {
                            unicode.append('+');
                        }
                    } else {
                        /*
                         * This b is a US-ASCII byte for some character.
                         */
                        unicode.appendCodePoint(b);
                    }
                }

            } else if (b == '+') {
                /*
                 * We are not currently processing a Base64 section, but this starts one. Remember
                 * where it starts, in the input bytes and the output unicode so that, if we hit the
                 * end of input before it ends, we can leave it unprocessed for next time.
                 */
                startInBytes = s;
                startInUnicode = unicode.length();

                // Initialise the Base64 decoder
                base64bits = 0;
                inBase64 = true;

            } else {
                /*
                 * This b is a US-ASCII byte for some character. We are permissive on decoding; the
                 * only ASCII byte not decoding to itself is the + which begins a base64 string.
                 */
                unicode.appendCodePoint(b);
            }
        }

        /*
         * We hit the end of the input. If we were part way through some Base64 processing, since we
         * don't store all that state (inBase64, base64bits, base64buffer) the strategy is to back
         * up the input pointer to the '-' that started the current Base64 segment.
         */
        if (inBase64) {
            // Restore state to beginning of last Base64 sequence
            s = startInBytes;
            unicode.setLength(startInUnicode);
        }

        if (consumed != null) {
            // Not a final call, so report how much consumed in the consumed argument
            consumed[0] = s;
        } else if (s < size) {
            // This was final but we didn't exhaust the input: that's an error.
            s = insertReplacementAndGetResume(unicode, errors, "utf-7", //
                    bytes, startInBytes, size, "unterminated shift sequence");
        }

        return unicode.toString();
    }

    /**
     * Decode completely a sequence of bytes representing the UTF-7 encoded form of a Unicode string
     * and return the (Jython internal representation of) the unicode object. The retruned Java
     * String is a UTF-16 representation of the Unicode result, in line with Java conventions.
     * Unicode characters above the BMP are represented as surrogate pairs.
     *
     * @param bytes input represented as String (Jython PyString convention)
     * @param errors error policy name (e.g. "ignore", "replace")
     * @return unicode result (as UTF-16 Java String)
     */
    public static String PyUnicode_DecodeUTF7(String bytes, String errors) {
        return PyUnicode_DecodeUTF7Stateful(bytes, errors, null);
    }

    /**
     * Helper for {@link #PyUnicode_DecodeUTF7Stateful(String, String, int[])} to emit characters
     * that accumulated as UTF-16 code units in the bits of a long integer (from Base64 decoding,
     * say). The buffer variable may hold any number of bits (up to its 64-bit capacity). The number
     * of valid bits is given by argument <code>n</code> and they are the <code>n</code> least
     * significant of the buffer.
     * <p>
     * Only complete Unicode characters are emitted, which are obtained by consuming 16 bits (when
     * those bits identify a BMP character), or 32 bits (when those bits form a surrogate pair).
     * Consumed bits are not cleared from the buffer (it is passed by value), and there is no need
     * for the client to clear them, but the method returns the new number of valid bits n1, which
     * are in the least significant positions (that is, bits <code>n1-1</code> to <code>0</code>).
     *
     * If the method returns with 32 or more bits unconsumed, it has encountered an invalid sequence
     * of bits: the leading bits will then either be an "unaccompanied" trail surrogate, or a lead
     * surrogate not followed by a trail surrogate.
     *
     * @param v output UTF-16 sequence
     * @param buffer holding the bits
     * @param n the number of bits held (<=64)
     * @return the number of bits not emitted (<32 unless error)
     */
    private static int emitCodePoints(StringBuilder v, long buffer, int n) {

        // Emit code points until too few in the buffer to process.
        while (n >= 16) {

            /*
             * Get the top 16 bits of the buffer to bottom of an int. Note no 0xffff mask as bits to
             * left of bit-15 are harmless
             */
            int unit = (int)(buffer >>> (n - 16));
            boolean unitIsSurrogate = ((unit & 0xF800) == 0xD800);

            if (!unitIsSurrogate) {
                // This (or rather its bottom 16 bits) is a BMP codepoint: easy
                v.append((char)unit);
                n -= 16;

            } else if (n >= 32) {
                // This a surrogate unit and we have enough bits for the whole code point.
                if ((unit & 0x0400) == 0) {
                    // This is a lead surrogate as expected ... get the trail surrogate.
                    int unit2 = (int)(buffer >>> (n - 32));
                    if ((unit2 & 0xFC00) == 0xDC00) {
                        // And this is the trail surrogate we expected
                        v.appendCodePoint(0x10000 + ((unit & 0x3ff) << 10) + (unit2 & 0x3ff));
                        n -= 32;
                    } else {
                        // But this isn't a trail surrogate: jam at >=32
                        return n;
                    }
                } else {
                    // This is an unaccompanied trail surrogate: jam at >=32
                    return n;
                }

            } else {
                // This a non-BMP code point but we don't have enough bits to deal with it yet
                return n;
            }

        }

        return n;
    }

    /**
     * Helper for {@link #PyUnicode_DecodeUTF7Stateful(String, String, int[])} to diagnose what went
     * wrong in {@link #emitCodePoints(StringBuilder, long, int)}. When called with fewer than 32
     * bits in the buffer, it assumes we are in the run-down of processing at the end of the
     * decoder, where partial output characters are an error. For 32 bits or more, It duplicates
     * some logic, but is called only during abnormal processing. The return is:
     * <table>
     * <caption>Values returned</caption>
     * <tr>
     * <td>NONE</td>
     * <td>No error</td>
     * </tr>
     * <tr>
     * <td>PADDING</td>
     * <td>non-zero padding bits in shift sequence</td>
     * <td>(error if at end of shift sequence)</td>
     * </tr>
     * <tr>
     * <td>PARTIAL</td>
     * <td>partial character in shift sequence</td>
     * <td>(error if at end of shift sequence)</td>
     * </tr>
     * <tr>
     * <td>TRUNCATED</td>
     * <td>second surrogate missing at end of shift sequence</td>
     * </tr>
     * <tr>
     * <td>MISSING</td>
     * <td>second surrogate missing</td>
     * </tr>
     * <tr>
     * <td>TRAIL</td>
     * <td>unexpected second surrogate</td>
     * </tr>
     * </table>
     * <p>
     * We are compatible with CPython in using the term "second surrogate" in error messages rather
     * than "trail surrogate" (which is used in the code).
     * <p>
     * Note that CPython (see Issue13333) allows this codec to decode lone surrogates into the
     * internal data of unicode objects. It is difficult to reconcile this with the idea that the
     * v3.3 statement "Strings contain Unicode characters", but that reconciliation is probably to
     * be found in PEP383, not implemented in Jython.
     *
     * @param buffer holding the bits
     * @param n the number of bits held (<=64)
     * @return the diagnosis
     */
    private static UTF7Error emitCodePointsDiagnosis(long buffer, int n) {

        if (n >= 16) {
            /*
             * Get the top 16 bits of the buffer to bottom of an int. Note no 0xffff mask as bits to
             * left of bit-15 are harmless
             */
            int unit = (int)(buffer >>> (n - 16));
            boolean unitIsSurrogate = ((unit & 0xF800) == 0xD800);

            if (!unitIsSurrogate) {
                // No problem. In practice, we should never land here.
                return UTF7Error.NONE;

            } else if (n >= 32) {

                if ((unit & 0x0400) == 0) {
                    // This is a lead surrogate, which is valid: check the next 16 bits.
                    int unit2 = ((int)(buffer >>> (n - 32))) & 0xffff;
                    if ((unit2 & 0xFC00) == 0xDC00) {
                        // Hmm ... why was I called?
                        return UTF7Error.NONE;
                    } else {
                        // Not trail surrogate: that's the problem
                        return UTF7Error.MISSING;
                    }

                } else {
                    // This is an unexpected trail surrogate
                    return UTF7Error.TRAIL;
                }

            } else {
                // Note that 32 > n >= 16, so we are at the end of decoding

                if ((unit & 0x0400) == 0) {
                    /*
                     * This is a lead surrogate, but since decoding stopped we must have reched the
                     * end of a Base64 segment without the trail surrogate appearing.
                     */
                    return UTF7Error.TRUNCATED;

                } else {
                    // This is an unexpected trail surrogate
                    return UTF7Error.TRAIL;
                }
            }

        } else if (n >= 6) {
            // Fewer than 16 bits: at end of decoding with Base64 characters left over
            return UTF7Error.PARTIAL;

        } else {
            // Fewer than 6 bits, which should all be zero. Make a mask to extract them.
            int validBits = (1 << n) - 1;
            int padding = ((int)buffer) & validBits;
            if (padding != 0) {
                // At end of decoding with non-zero padding
                return UTF7Error.PADDING;
            } else {
                // Any bits left are zero: that's ok then.
                return UTF7Error.NONE;
            }
        }
    }

    /**
     * Encode a UTF-16 Java String as UTF-7 bytes represented by the low bytes of the characters in
     * a String. (String representation for byte data is chosen so that it may immediately become a
     * PyString.)
     *
     * This method differs from the CPython equivalent (in <code>Object/unicodeobject.c</code>)
     * which works with an array of code points that are, in a wide build, Unicode code points.
     *
     * @param unicode to be encoded
     * @param base64SetO true if characters in "set O" should be translated to base64
     * @param base64WhiteSpace true if white-space characters should be translated to base64
     * @param errors error policy name (e.g. "ignore", "replace")
     * @return bytes representing the encoded unicode string
     */
    public static String PyUnicode_EncodeUTF7(String unicode, boolean base64SetO,
            boolean base64WhiteSpace, String errors) {
        boolean inBase64 = false;
        int base64bits = 0;
        long base64buffer = 0;

        int size = unicode.length();

        // Output bytes here: sized for ASCII + a few non-BMP characters
        // We use a StringBuilder and return a String, but we are really storing encoded bytes
        StringBuilder v = new StringBuilder(size + size / 8 + 10);

        for (int i = 0; i < size; i++) {

            // Next UTF-16 code unit to process
            int ch = unicode.charAt(i);

            /*
             * Decide what to output and prepare for it. Mainly, decide whether to represent this
             * UTF-16 code unit in Base64 or US-ASCII, and switch modes, with output, accordingly.
             */
            if (inBase64) {
                // Currently we are in Base64 encoding: should we switch out?
                if (ENCODE_DIRECT(ch, !base64SetO, !base64WhiteSpace)) {
                    /*
                     * The next character is one for which we do not need to be in Base64, so pad
                     * out to 6n the Base64 bits we currently have buffered and emit them. Then
                     * switch to US-ASCII.
                     */
                    emitBase64Padded(v, base64buffer, base64bits);
                    inBase64 = false;

                    if (FROM_BASE64(ch) != -1) {
                        // Character is in the Base64 set, or is a '-': must signal end explicitly.
                        v.append('-');
                    }
                }

            } else {
                // Not currently in Base64 encoding: should we switch in?
                if (ch == '+') {
                    // Special case for + since it would otherwise flag a start
                    v.append('+');
                    ch = '-'; // Comes out as +-
                } else if (!ENCODE_DIRECT(ch, !base64SetO, !base64WhiteSpace)) {
                    /*
                     * The next character is one for which we need to be in Base64, so switch to it
                     * and emit the Base64 start marker and initialise the coder.
                     */
                    v.append('+');
                    inBase64 = true;
                    base64bits = 0;
                }
            }

            /*
             * We have decided what to do (US-ASCII or Base64) but we haven't done it yet.
             */
            if (!inBase64) {
                // We decided to encode the current character as US-ASCII and are in that mode
                v.append((char)ch);

            } else {
                // We decided to encode the current character as Base64 and are in that mode
                /*
                 * In the present implementation the characters are suppplied as a UTF-16 Java
                 * String. The UTF-7 approach to characters beyond the BMP is to encode the
                 * surrogate pair as two 16-bit pseudo-characters, which is how Jython represents it
                 * already, so the first part is already done for us by accessing the internal
                 * representation.
                 */
                // XXX see issue #2002: we should only count surrogate pairs as one character
                // if ((ch & 0xFC00)==0xD800) { count++; }

                if (base64bits > 48) {
                    // No room for the next 16 bits: emit all we have
                    base64bits = emitBase64(v, base64buffer, base64bits);
                }
                base64bits += 16;
                base64buffer = (base64buffer << 16) + ch;
            }
        }

        /*
         * We've run out of input to encode. If we are currently in US-ASCII mode, we can just stop.
         * If we are in Base64 mode, we have to come to a clean stop, since there is no opportunity
         * to store this fact as state for next time (and there may be no next time).
         */
        if (inBase64) {
            /*
             * Currently we are in Base64 encoding and must switch out. Pad out to 6n the bits we
             * currently have buffered and emit them. We don't know what might come next so emit a
             * '-' to round out the segment.
             */
            emitBase64Padded(v, base64buffer, base64bits);
            v.append('-');
        }

        return v.toString();
    }

    /**
     * Helper for {@link #PyUnicode_EncodeUTF7(String, boolean, boolean, String)} to emit 6-bit
     * Base64 code units as bytes to the output. The buffer variable may hold any number of bits (up
     * to its 64-bit capacity). The number of valid bits is given by argument <code>n</code> and
     * they are the <code>n</code> least significant of the buffer. Bits will be emitted in groups
     * of 6, represented by their Base64 character, starting with the 6 most-significant valid bits
     * of the buffer (that is, bits <code>n-6</code> to <code>n-1</code>). The buffer is not cleared
     * (it is passed by value), but the method returns the new number of valid bits n1, which are in
     * the least significant positions (that is, bits <code>n1-1</code> to <code>0</code>).
     *
     * @param v output byte array
     * @param buffer holding the bits
     * @param n the number of bits held (<=64)
     * @return the number of bits (<6) not emitted
     */
    private static int emitBase64(StringBuilder v, long buffer, int n) {
        while (n >= 6) {
            n -= 6;
            long sixBits = buffer >>> n;
            char b64byte = TO_BASE64((int)sixBits);
            v.append(b64byte);
        }
        return n;
    }

    /**
     * Helper for {@link #PyUnicode_EncodeUTF7(String, boolean, boolean, String)} to emit 6-bit
     * Base64 code units as bytes to the output. The buffer variable may hold any number of bits (up
     * to 60 bits). The number of valid bits is given by argument <code>n</code> and they are the
     * <code>n</code> least significant of the buffer. The buffer will be padded, by shifting in
     * zeros at the least significant end, until it the number of valid bits is a multiple of 6.
     * Bits will then be emitted in groups of 6, represented by their Base64 character, starting
     * with the 6 most-significant valid bits of the buffer (that is, bits <code>n-6</code> to
     * <code>n-1</code>). The buffer is not cleared (it is passed by value), but can be considered
     * empty.
     *
     * @param v output byte array
     * @param buffer holding the bits
     * @param n the number of bits held (<=60)
     */
    private static void emitBase64Padded(StringBuilder v, long buffer, int n) {
        if (n > 0) {
            int npad = 5 - (n + 5) % 6;                 // smallest such that (n+npad) mod 6 == 0
            emitBase64(v, buffer << npad, n + npad);    // == 0 as a result of the padding
        }
    }

    /* --- UTF-8 Codec ---------------------------------------------------- */

    private static byte utf8_code_length[] = {//@formatter:off
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
    }; //@formatter:on

    // TODO: need to modify to use a codepoint approach (which is almost the case now,
    // ch is an
    public static String PyUnicode_DecodeUTF8(String str, String errors) {
        return PyUnicode_DecodeUTF8Stateful(str, errors, null);
    }

    public static String PyUnicode_DecodeUTF8Stateful(String str, String errors, int[] consumed) {
        int size = str.length();
        StringBuilder unicode = new StringBuilder(size);

        /* Unpack UTF-8 encoded data */
        int i;
        for (i = 0; i < size;) {
            int ch = str.charAt(i);

            if (ch < 0x80) {
                unicode.append((char)ch);
                i++;
                continue;
            }
            if (ch > 0xFF) {
                i = insertReplacementAndGetResume(unicode, errors, "utf-8", str, //
                        i, i + 1, "ordinal not in range(255)");
                continue;
            }

            int n = utf8_code_length[ch];

            if (i + n > size) {
                if (consumed != null) {
                    break;
                }
                i = insertReplacementAndGetResume(unicode, errors, "utf-8", str, //
                        i, i + 1, "unexpected end of data");
                continue;
            }

            switch (n) {
                case 0:
                    i = insertReplacementAndGetResume(unicode, errors, "utf-8", str, //
                            i, i + 1, "unexpected code byte");
                    continue;
                case 1:
                    i = insertReplacementAndGetResume(unicode, errors, "utf-8", str, //
                            i, i + 1, "internal error");
                    continue;
                case 2:
                    char ch1 = str.charAt(i + 1);
                    if ((ch1 & 0xc0) != 0x80) {
                        i = insertReplacementAndGetResume(unicode, errors, "utf-8", str, //
                                i, i + 2, "invalid data");
                        continue;
                    }
                    ch = ((ch & 0x1f) << 6) + (ch1 & 0x3f);
                    if (ch < 0x80) {
                        i = insertReplacementAndGetResume(unicode, errors, "utf-8", str, //
                                i, i + 2, "illegal encoding");
                        continue;
                    } else {
                        unicode.appendCodePoint(ch);
                    }
                    break;

                case 3:
                    ch1 = str.charAt(i + 1);
                    char ch2 = str.charAt(i + 2);
                    if ((ch1 & 0xc0) != 0x80 || (ch2 & 0xc0) != 0x80) {
                        i = insertReplacementAndGetResume(unicode, errors, "utf-8", str, //
                                i, i + 3, "invalid data");
                        continue;
                    }
                    ch = ((ch & 0x0f) << 12) + ((ch1 & 0x3f) << 6) + (ch2 & 0x3f);
                    if (ch < 0x800 || (ch >= 0xd800 && ch < 0xe000)) {
                        i = insertReplacementAndGetResume(unicode, errors, "utf-8", str, //
                                i, i + 3, "illegal encoding");
                        continue;
                    } else {
                        unicode.appendCodePoint(ch);
                    }
                    break;

                case 4:
                    ch1 = str.charAt(i + 1);
                    ch2 = str.charAt(i + 2);
                    char ch3 = str.charAt(i + 3);
                    if ((ch1 & 0xc0) != 0x80 || (ch2 & 0xc0) != 0x80 || (ch3 & 0xc0) != 0x80) {
                        i = insertReplacementAndGetResume(unicode, errors, "utf-8", str, //
                                i, i + 4, "invalid data");
                        continue;
                    }
                    ch = ((ch & 0x7) << 18) + ((ch1 & 0x3f) << 12) + //
                            ((ch2 & 0x3f) << 6) + (ch3 & 0x3f);
                    // validate and convert to UTF-16
                    if ((ch < 0x10000) || // minimum value allowed for 4 byte encoding
                            (ch > 0x10ffff)) { // maximum value allowed for UTF-16
                        i = insertReplacementAndGetResume(unicode, errors, "utf-8", str, //
                                i, i + 4, "illegal encoding");
                        continue;
                    }

                    unicode.appendCodePoint(ch);
                    break;

                default:
                    // TODO: support
                    /* Other sizes are only needed for UCS-4 */
                    i = insertReplacementAndGetResume(unicode, errors, "utf-8", str, //
                            i, i + n, "unsupported Unicode code range");
                    continue;
            }
            i += n;
        }

        if (consumed != null) {
            consumed[0] = i;
        }

        return unicode.toString();
    }

    public static String PyUnicode_EncodeUTF8(String str, String errors) {
        return StringUtil.fromBytes(Charset.forName("UTF-8").encode(str));
    }

    /* --- ASCII and Latin-1 Codecs --------------------------------------- */
    public static String PyUnicode_DecodeASCII(String str, int size, String errors) {
        return PyUnicode_DecodeIntLimited(str, size, errors, "ascii", 128);
    }

    public static String PyUnicode_DecodeLatin1(String str, int size, String errors) {
        return PyUnicode_DecodeIntLimited(str, size, errors, "latin-1", 256);
    }

    private static String PyUnicode_DecodeIntLimited(String str, int size, String errors,
            String encoding, int limit) {
        StringBuilder v = new StringBuilder(size);

        String reason = "ordinal not in range(" + limit + ")";
        for (int i = 0; i < size; i++) {
            char ch = str.charAt(i);
            if (ch < limit) {
                v.append(ch);
            } else {
                i = insertReplacementAndGetResume(v, errors, encoding, str, i, i + 1, reason) - 1;
            }
        }

        return v.toString();
    }

    public static String PyUnicode_EncodeASCII(String str, int size, String errors) {
        return PyUnicode_EncodeIntLimited(str, size, errors, "ascii", 128);
    }

    public static String PyUnicode_EncodeLatin1(String str, int size, String errors) {

        return PyUnicode_EncodeIntLimited(str, size, errors, "latin-1", 256);
    }

    private static String PyUnicode_EncodeIntLimited(String str, int size, String errors,
            String encoding, int limit) {
        String reason = "ordinal not in range(" + limit + ")";
        StringBuilder v = new StringBuilder(size);
        for (int i = 0; i < size; i++) {
            char ch = str.charAt(i);
            if (ch >= limit) {
                int nextGood = i + 1;
                for (; nextGood < size; nextGood++) {
                    if (str.charAt(nextGood) < limit) {
                        break;
                    }
                }
                if (errors != null) {
                    if (errors.equals(IGNORE)) {
                        i = nextGood - 1;
                        continue;
                    } else if (errors.equals(REPLACE)) {
                        for (int j = i; j < nextGood; j++) {
                            v.append('?');
                        }
                        i = nextGood - 1;
                        continue;
                    } else if (errors.equals(XMLCHARREFREPLACE)) {
                        v.append(xmlcharrefreplace(i, nextGood, str));
                        i = nextGood - 1;
                        continue;
                    } else if (errors.equals(BACKSLASHREPLACE)) {
                        v.append(backslashreplace(i, nextGood, str));
                        i = nextGood - 1;
                        continue;
                    }
                }
                PyObject replacement = encoding_error(errors, encoding, str, i, nextGood, reason);
                String replStr = replacement.__getitem__(0).toString();
                for (int j = 0; j < replStr.length(); j++) {
                    if (replStr.charAt(j) >= limit) {
                        throw Py.UnicodeEncodeError(encoding, str, i + j, i + j + 1, reason);
                    }
                }
                v.append(replStr);
                i = calcNewPosition(size, replacement) - 1;
            } else {
                v.append(ch);
            }
        }
        return v.toString();
    }

    /* --- RawUnicodeEscape Codec ---------------------------------------- */
    private static char[] hexdigit = "0123456789ABCDEF".toCharArray();

    // The modified flag is used by cPickle.
    public static String
            PyUnicode_EncodeRawUnicodeEscape(String str, String errors, boolean modifed) {
        StringBuilder v = new StringBuilder(str.length());

        for (Iterator<Integer> iter = new PyUnicode(str).newSubsequenceIterator(); iter.hasNext();) {
            int codePoint = iter.next();
            if (codePoint >= Character.MIN_SUPPLEMENTARY_CODE_POINT) {
                // Map 32-bit characters to '\\Uxxxxxxxx'
                v.append("\\U");
                v.append(hexdigit[(codePoint >> 28) & 0xF]);
                v.append(hexdigit[(codePoint >> 24) & 0xF]);
                v.append(hexdigit[(codePoint >> 20) & 0xF]);
                v.append(hexdigit[(codePoint >> 16) & 0xF]);
                v.append(hexdigit[(codePoint >> 12) & 0xF]);
                v.append(hexdigit[(codePoint >> 8) & 0xF]);
                v.append(hexdigit[(codePoint >> 4) & 0xF]);
                v.append(hexdigit[codePoint & 0xF]);
            } else if (codePoint >= 256 || (modifed && (codePoint == '\\' || codePoint == '\n'))) {
                // Map 16-bit chararacters to '\\uxxxx'
                v.append("\\u");
                v.append(hexdigit[(codePoint >> 12) & 0xF]);
                v.append(hexdigit[(codePoint >> 8) & 0xF]);
                v.append(hexdigit[(codePoint >> 4) & 0xF]);
                v.append(hexdigit[codePoint & 0xF]);
            } else {
                v.append((char)codePoint);
            }
        }

        return v.toString();
    }

    public static String PyUnicode_DecodeRawUnicodeEscape(String str, String errors) {
        int size = str.length();
        StringBuilder v = new StringBuilder(size);

        for (int i = 0; i < size;) {
            char ch = str.charAt(i);
            // Non-escape characters are interpreted as Unicode ordinals
            if (ch != '\\') {
                v.append(ch);
                i++;
                continue;
            }

            // \\u-escapes are only interpreted if the number of leading backslashes is
            // odd
            int bs = i;
            while (i < size) {
                ch = str.charAt(i);
                if (ch != '\\') {
                    break;
                }
                v.append(ch);
                i++;
            }
            if (((i - bs) & 1) == 0 || i >= size || (ch != 'u' && ch != 'U')) {
                continue;
            }
            v.setLength(v.length() - 1);
            int count = ch == 'u' ? 4 : 8;
            i++;

            // \\uXXXX with 4 hex digits, \Uxxxxxxxx with 8
            int codePoint = 0, asDigit = -1;
            for (int j = 0; j < count; i++, j++) {
                if (i == size) {
                    // EOF in a truncated escape
                    asDigit = -1;
                    break;
                }

                ch = str.charAt(i);
                asDigit = Character.digit(ch, 16);
                if (asDigit == -1) {
                    break;
                }
                codePoint = ((codePoint << 4) & ~0xF) + asDigit;
            }
            if (asDigit == -1) {
                i = codecs.insertReplacementAndGetResume(v, errors, "rawunicodeescape", str, //
                        bs, i, "truncated \\uXXXX");
            } else {
                v.appendCodePoint(codePoint);
            }
        }

        return v.toString();
    }

    private static class Punycode {

        // specified by punycode, http://www.ietf.org/rfc/rfc3492.txt
        private static final int BASE = 36;
        private static final int TMIN = 1;
        private static final int TMAX = 26;
        private static final int SKEW = 38;
        private static final int DAMP = 700;
        private static final int INITIAL_BIAS = 72;
        private static final int INITIAL_N = 128;
        private static final int BASIC = 0x80;

        private Punycode() {

        }

        private static int adapt(int delta, int numpoints, boolean firsttime) {
            delta = firsttime ? delta / DAMP : delta >> 1;
            delta += delta / numpoints;
            int k = 0;
            while (delta > (((BASE - TMIN) * TMAX) / 2)) {
                delta /= BASE - TMIN;
                k += BASE;
            }
            return k + (((BASE - TMIN + 1) * delta) / (delta + SKEW));
        }

        private static boolean isBasic(int codePoint) {
            return codePoint < BASIC;
        }
    }

    public static String PyUnicode_EncodePunycode(PyUnicode input, String errors) {
        int n = Punycode.INITIAL_N;
        int delta = 0;
        long guard_delta;
        int bias = Punycode.INITIAL_BIAS;
        int b = 0;
        final StringBuilder buffer = new StringBuilder();
        for (Iterator<Integer> iter = input.iterator(); iter.hasNext();) {
            int c = iter.next();
            if (Punycode.isBasic(c)) {
                buffer.appendCodePoint(c);
                b++;
            }
        }
        if (b > 0) {
            buffer.appendCodePoint('-');
        }
        int h = b;
        int size = input.getCodePointCount();
        while (h < size) {
            int m = Integer.MAX_VALUE;
            int i = 0;
            int codePointIndex = 0;
            for (Iterator<Integer> iter = input.iterator(); iter.hasNext(); i++) {
                int c = iter.next();
                if (c > n && c < m) {
                    m = c;
                    codePointIndex = i;
                }
            }
            guard_delta = delta + ((m - n) * (h + 1));
            if (guard_delta > Integer.MAX_VALUE) {
                throw Py.UnicodeEncodeError("punycode", input.getString(), codePointIndex,
                        codePointIndex + 1, "overflow");
            }
            delta = (int)guard_delta;

            n = m;
            i = 0;
            for (Iterator<Integer> iter = input.iterator(); iter.hasNext(); i++) {
                int c = iter.next();
                if (c < n) {
                    guard_delta = delta + 1;
                    if (guard_delta > Integer.MAX_VALUE) {
                        throw Py.UnicodeEncodeError("punycode", input.getString(), i, i + 1,
                                "overflow");
                    }
                    delta = (int)guard_delta;
                }
                if (c == n) {
                    int q = delta;
                    for (int k = Punycode.BASE;; k += Punycode.BASE) {
                        int t = k <= bias ? Punycode.TMIN : //
                                (k >= bias + Punycode.TMAX ? Punycode.TMAX : k - bias);
                        if (q < t) {
                            break;
                        }
                        buffer.appendCodePoint(t + ((q - t) % (Punycode.BASE - t)));
                        q = (q - t) / (Punycode.BASE - t);
                    }
                    buffer.appendCodePoint(q);
                    bias = Punycode.adapt(delta, h + 1, h == b);
                    delta = 0;
                    h++;
                }
            }
            delta++;
            n++;
        }
        return buffer.toString();
    }

    public static PyUnicode PyUnicode_DecodePunycode(String input, String errors) {

        int input_size = input.length();
        int output_size = 0;
        ArrayList<Integer> ucs4 = new ArrayList<Integer>(input_size);
        int j = 0;
        for (; j < input_size; j++) {
            int c = input.charAt(j);
            if (!Punycode.isBasic(c)) {
                throw Py.UnicodeDecodeError("punycode", input, j, j + 1, "not basic");
            } else if (c == '-') {
                break;
            } else {
                ucs4.add(c);
                output_size++;
            }
        }

        int n = Punycode.INITIAL_N;
        int i = 0;
        int bias = Punycode.INITIAL_BIAS;
        while (j < input_size) {
            int old_i = i;
            int w = 1;
            for (int k = Punycode.BASE;; k += Punycode.BASE) {
                int c = input.charAt(j++);
                int digit = c - '0';
                long guard_i = i + digit * w;
                if (guard_i > Integer.MAX_VALUE) {
                    throw Py.UnicodeDecodeError("punycode", input, j, j + 1, "overflow");
                }
                i = (int)guard_i;
                int t = k <= bias ? Punycode.TMIN : //
                        (k >= bias + Punycode.TMAX ? Punycode.TMAX : k - bias);
                if (digit < t) {
                    break;
                }
                long guard_w = w * Punycode.BASE - t;
                if (guard_w > Integer.MAX_VALUE) {
                    throw Py.UnicodeDecodeError("punycode", input, j, j + 1, "overflow");
                }
            }
            bias = Punycode.adapt(i - old_i, output_size + 1, old_i == 0);
            n += i / (output_size + 1);
            i %= output_size + 1;
            ucs4.add(i, n);

        }
        return new PyUnicode(ucs4);
    }

    public static String PyUnicode_EncodeIDNA(PyUnicode input, String errors) {

        throw new UnsupportedOperationException();

        // 1. If the sequence contains any code points outside the ASCII range
        // (0..7F) then proceed to step 2, otherwise skip to step 3.
        //
        // 2. Perform the steps specified in [NAMEPREP] and fail if there is an
        // error. The AllowUnassigned flag is used in [NAMEPREP].
        // this basically enails changing out space, etc.
        //
        // 3. If the UseSTD3ASCIIRules flag is set, then perform these checks:
        //
        // (a) Verify the absence of non-LDH ASCII code points; that is, the
        // absence of 0..2C, 2E..2F, 3A..40, 5B..60, and 7B..7F.
        //
        // (b) Verify the absence of leading and trailing hyphen-minus; that
        // is, the absence of U+002D at the beginning and end of the
        // sequence.
        //
        // 4. If the sequence contains any code points outside the ASCII range
        // (0..7F) then proceed to step 5, otherwise skip to step 8.
        //
        // 5. Verify that the sequence does NOT begin with the ACE prefix.
        //
        // 6. Encode the sequence using the encoding algorithm in [PUNYCODE] and
        // fail if there is an error.
        //
        // 7. Prepend the ACE prefix.
        //
        // 8. Verify that the number of code points is in the range 1 to 63
        // inclusive.
    }

    public static PyUnicode PyUnicode_DecodeIDNA(String input, String errors) {
        throw new UnsupportedOperationException();
    }

    /* --- Utility methods -------------------------------------------- */

    /**
     * Invoke a user-defined error-handling mechanism, for errors encountered during encoding, as
     * registered through {@link #register_error(String, PyObject)}. The return value is the return
     * from the error handler indicating the replacement codec <b>input</b> and the the position at
     * which to resume encoding. Invokes the mechanism described in PEP-293.
     *
     * @param errors name of the error policy (or null meaning "strict")
     * @param encoding name of encoding that encountered the error
     * @param toEncode unicode string being encoded
     * @param start index of first char it couldn't encode
     * @param end index+1 of last char it couldn't encode (usually becomes the resume point)
     * @param reason contribution to error message if any
     * @return must be a tuple <code>(replacement_unicode, resume_index)</code>
     */
    public static PyObject encoding_error(String errors, String encoding, String toEncode,
            int start, int end, String reason) {
        // Retrieve handler registered through register_error(). null is equivalent to "strict".
        PyObject errorHandler = lookup_error(errors);
        // Construct an exception to hand to the error handler
        PyException exc = Py.UnicodeEncodeError(encoding, toEncode, start, end, reason);
        exc.normalize();
        // And invoke the handler.
        PyObject replacement = errorHandler.__call__(new PyObject[] {exc.value});
        checkErrorHandlerReturn(errors, replacement);
        return replacement;
    }

    /**
     * Handler for errors encountered during decoding, adjusting the output buffer contents and
     * returning the correct position to resume decoding (if the handler does not simply raise an
     * exception).
     *
     * @param partialDecode output buffer of unicode (as UTF-16) that the codec is building
     * @param errors name of the error policy (or null meaning "strict")
     * @param encoding name of encoding that encountered the error
     * @param toDecode bytes being decoded
     * @param start index of first byte it couldn't decode
     * @param end index+1 of last byte it couldn't decode (usually becomes the resume point)
     * @param reason contribution to error message if any
     * @return the resume position: index of next byte to decode
     */
    public static int insertReplacementAndGetResume(StringBuilder partialDecode, String errors,
            String encoding, String toDecode, int start, int end, String reason) {

        // Handle the two special cases "ignore" and "replace" locally
        if (errors != null) {
            if (errors.equals(IGNORE)) {
                // Just skip to the first non-problem byte
                return end;
            } else if (errors.equals(REPLACE)) {
                // Insert *one* Unicode replacement character and skip
                partialDecode.appendCodePoint(Py_UNICODE_REPLACEMENT_CHARACTER);
                return end;
            }
        }

        // If errors not one of those, invoke the generic mechanism
        PyObject replacementSpec = decoding_error(errors, encoding, toDecode, start, end, reason);

        // Deliver the replacement unicode text to the output buffer
        partialDecode.append(replacementSpec.__getitem__(0).toString());

        // Return the index in toDecode at which we should resume
        return calcNewPosition(toDecode.length(), replacementSpec);
    }

    /**
     * Invoke a user-defined error-handling mechanism, for errors encountered during decoding, as
     * registered through {@link #register_error(String, PyObject)}. The return value is the return
     * from the error handler indicating the replacement codec <b>output</b> and the the position at
     * which to resume decoding. Invokes the mechanism described in PEP-293.
     *
     * @param errors name of the error policy (or null meaning "strict")
     * @param encoding name of encoding that encountered the error
     * @param toDecode bytes being decoded
     * @param start index of first byte it couldn't decode
     * @param end index+1 of last byte it couldn't decode (usually becomes the resume point)
     * @param reason contribution to error message if any
     * @return must be a tuple <code>(replacement_unicode, resume_index)</code>
     */
    public static PyObject decoding_error(String errors, String encoding, String toDecode,
            int start, int end, String reason) {
        // Retrieve handler registered through register_error(). null is equivalent to "strict".
        PyObject errorHandler = lookup_error(errors);
        // Construct an exception to hand to the error handler
        PyException exc = Py.UnicodeDecodeError(encoding, toDecode, start, end, reason);
        exc.normalize();
        // And invoke the handler.
        PyObject replacementSpec = errorHandler.__call__(new PyObject[] {exc.value});
        checkErrorHandlerReturn(errors, replacementSpec);
        return replacementSpec;
    }

    /**
     * Check thet the error handler returned a tuple
     * <code>(replacement_unicode, resume_index)</code>.
     *
     * @param errors name of the error policy
     * @param replacementSpec from error handler
     */
    private static void checkErrorHandlerReturn(String errors, PyObject replacementSpec) {
        if (!(replacementSpec instanceof PyTuple) || replacementSpec.__len__() != 2
                || !(replacementSpec.__getitem__(0) instanceof PyBaseString)
                || !(replacementSpec.__getitem__(1) instanceof PyInteger)) {
            throw new PyException(Py.TypeError, "error_handler " + errors
                    + " must return a tuple of (replacement, new position)");
        }
    }

    /**
     * Given the return from some codec error handler (invoked while encoding or decoding), which
     * specifies a resume position, and the length of the input being encoded or decoded, check and
     * interpret the resume position. Negative indexes in the error handler return are interpreted
     * as "from the end". If the result would be out of bounds in the input, an
     * <code>IndexError</code> exception is raised.
     *
     * @param size of byte buffer being decoded
     * @param errorTuple returned from error handler
     * @return absolute resume position.
     */
    public static int calcNewPosition(int size, PyObject errorTuple) {
        int newPosition = ((PyInteger)errorTuple.__getitem__(1)).getValue();
        if (newPosition < 0) {
            newPosition = size + newPosition;
        }
        if (newPosition > size || newPosition < 0) {
            throw Py.IndexError(newPosition + " out of bounds of encoded string");
        }
        return newPosition;
    }

    public static class CodecState {
        private PyList searchPath;
        private PyStringMap searchCache;
        private PyStringMap errorHandlers;
        private String default_encoding = "ascii";

        public static final String[] BUILTIN_ERROR_HANDLERS = new String[]{"strict",
                IGNORE,
                REPLACE,
                XMLCHARREFREPLACE,
                BACKSLASHREPLACE
        };

        public CodecState() {
            searchPath = new PyList();
            searchCache = new PyStringMap();
            errorHandlers = new PyStringMap();

            for (String builtinErrorHandler : BUILTIN_ERROR_HANDLERS) {
                register_error(builtinErrorHandler, Py.newJavaFunc(codecs.class,
                        builtinErrorHandler + "_errors"));
            }
        }

        public String getDefaultEncoding() {
            return default_encoding;
        }

        public void setDefaultEncoding(String encoding) {
            lookup(encoding);
            default_encoding = encoding;
        }

        public void register_error(String name, PyObject error) {
            if (!error.isCallable()) {
                throw Py.TypeError("argument must be callable");
            }
            errorHandlers.__setitem__(name.intern(), error);
        }

        public void register(PyObject search_function) {
            if (!search_function.isCallable()) {
                throw Py.TypeError("argument must be callable");
            }
            searchPath.append(search_function);
        }

        public PyTuple lookup(String encoding) {
            PyString v = new PyString(normalizestring(encoding));
            PyObject cached = searchCache.__finditem__(v);
            if (cached != null) {
                return (PyTuple)cached;
            }

            if (searchPath.__len__() == 0) {
                throw new PyException(Py.LookupError,
                        "no codec search functions registered: can't find encoding '" + encoding + "'");
            }

            for (PyObject func : searchPath.asIterable()) {
                PyObject created = func.__call__(v);
                if (created == Py.None) {
                    continue;
                }
                if (!(created instanceof PyTuple) || created.__len__() != 4) {
                    throw Py.TypeError("codec search functions must return 4-tuples");
                }
                searchCache.__setitem__(v, created);
                return (PyTuple)created;
            }
            throw new PyException(Py.LookupError, "unknown encoding '" + encoding + "'");
        }

        public PyObject lookup_error(String handlerName) {
            if (handlerName == null) {
                handlerName = "strict";
            }
            PyObject handler = errorHandlers.__finditem__(handlerName.intern());
            if (handler == null) {
                throw new PyException(Py.LookupError,
                        "unknown error handler name '" + handlerName + "'");
            }
            return handler;
        }
    }
}


class StringSubsequenceIterator implements Iterator {

    private final String s;
    private int current, k, start, stop, step;

    StringSubsequenceIterator(String s, int start, int stop, int step) {
        // System.out.println("s=" + s.length() + ",start=" + start + ",stop=" + stop);
        this.s = s;
        k = 0;
        current = start;
        this.start = start;
        this.stop = stop;
        this.step = step;

        /*
         * this bounds checking is necessary to convert between use of code units elsewhere, and
         * codepoints here it would be nice if it were unnecessary!
         */
        int count = getCodePointCount(s);
        if (start >= count) {
            this.stop = -1;
        } else if (stop >= count) {
            this.stop = count;
        }

        for (int i = 0; i < start; i++) {
            nextCodePoint();
        }
    }

    StringSubsequenceIterator(String s) {
        this(s, 0, getCodePointCount(s), 1);
    }

    private static int getCodePointCount(String s) {
        return s.codePointCount(0, s.length());
    }

    @Override
    public boolean hasNext() {
        return current < stop;
    }

    @Override
    public Object next() {
        int codePoint = nextCodePoint();
        current += 1;
        for (int j = 1; j < step && hasNext(); j++) {
            nextCodePoint();
            current += 1;
        }
        return codePoint;
    }

    private int nextCodePoint() {
        int U;
        // System.out.println("k=" + k);
        int W1 = s.charAt(k);
        if (W1 >= 0xD800 && W1 < 0xDC00) {
            int W2 = s.charAt(k + 1);
            U = (((W1 & 0x3FF) << 10) | (W2 & 0x3FF)) + 0x10000;
            k += 2;
        } else {
            U = W1;
            k += 1;
        }
        return U;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("Not supported on String objects (immutable)");
    }
}
