// Copyright (c) Corporation for National Research Initiatives
package org.python.core;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.python.core.buffer.BaseBuffer;
import org.python.core.buffer.SimpleStringBuffer;
import org.python.core.stringlib.FieldNameIterator;
import org.python.core.stringlib.FloatFormatter;
import org.python.core.stringlib.IntegerFormatter;
import org.python.core.stringlib.InternalFormat;
import org.python.core.stringlib.InternalFormat.Formatter;
import org.python.core.stringlib.InternalFormat.Spec;
import org.python.core.stringlib.MarkupIterator;
import org.python.core.stringlib.TextFormatter;
import org.python.core.util.StringUtil;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedType;
import org.python.expose.MethodType;

/**
 * A builtin python string.
 */
@Untraversable
@ExposedType(name = "str", base = PyBaseString.class, doc = BuiltinDocs.str_doc)
public class PyString extends PyBaseString implements BufferProtocol {

    public static final PyType TYPE = PyType.fromClass(PyString.class);
    protected String string; // cannot make final because of Python intern support
    protected transient boolean interned = false;
    /** Supports the buffer API, see {@link #getBuffer(int)}. */
    private Reference<BaseBuffer> export;

    public String getString() {
        return string;
    }

    // for PyJavaClass.init()
    public PyString() {
        this("", true);
    }

    protected PyString(PyType subType, String string, boolean isBytes) {
        super(subType);
        if (string == null) {
            throw new IllegalArgumentException("Cannot create PyString from null");
        } else if (!isBytes && !isBytes(string)) {
            throw new IllegalArgumentException("Cannot create PyString with non-byte value");
        }
        this.string = string;
    }

    /**
     * Fundamental constructor for <code>PyString</code> objects when the client provides a Java
     * <code>String</code>, necessitating that we range check the characters.
     *
     * @param subType the actual type being constructed
     * @param string a Java String to be wrapped
     */
    public PyString(PyType subType, String string) {
        this(subType, string, false);
    }

    public PyString(String string) {
        this(TYPE, string);
    }

    public PyString(char c) {
        this(TYPE, String.valueOf(c));
    }

    PyString(StringBuilder buffer) {
        this(TYPE, buffer.toString());
    }

    PyString(PyBuffer buffer) {
        this(TYPE, buffer.toString());
    }

    /**
     * Local-use constructor in which the client is allowed to guarantee that the
     * <code>String</code> argument contains only characters in the byte range. We do not then
     * range-check the characters.
     *
     * @param string a Java String to be wrapped (not null)
     * @param isBytes true if the client guarantees we are dealing with bytes
     */
    private PyString(String string, boolean isBytes) {
        super(TYPE);
        if (isBytes || isBytes(string)) {
            this.string = string;
        } else {
            throw new IllegalArgumentException("Cannot create PyString with non-byte value");
        }
    }

    /**
     * Determine whether a string consists entirely of characters in the range 0 to 255. Only such
     * characters are allowed in the <code>PyString</code> (<code>str</code>) type, when it is not a
     * {@link PyUnicode}.
     *
     * @return true if and only if every character has a code less than 256
     */
    private static boolean isBytes(String s) {
        int k = s.length();
        if (k == 0) {
            return true;
        } else {
            // Bitwise-or the character codes together in order to test once.
            char c = 0;
            // Blocks of 8 to reduce loop tests
            while (k > 8) {
                c |= s.charAt(--k);
                c |= s.charAt(--k);
                c |= s.charAt(--k);
                c |= s.charAt(--k);
                c |= s.charAt(--k);
                c |= s.charAt(--k);
                c |= s.charAt(--k);
                c |= s.charAt(--k);
            }
            // Now the rest
            while (k > 0) {
                c |= s.charAt(--k);
            }
            // We require there to be no bits set from 0x100 upwards
            return c < 0x100;
        }
    }

    /**
     * Creates a PyString from an already interned String. Just means it won't be reinterned if used
     * in a place that requires interned Strings.
     */
    public static PyString fromInterned(String interned) {
        PyString str = new PyString(TYPE, interned);
        str.interned = true;
        return str;
    }

    /**
     * Determine whether the string consists entirely of basic-plane characters. For a
     * {@link PyString}, of course, it is always <code>true</code>, but this is useful in cases
     * where either a <code>PyString</code> or a {@link PyUnicode} is acceptable.
     *
     * @return true
     */
    public boolean isBasicPlane() {
        return true;
    }

    @ExposedNew
    static PyObject str_new(PyNewWrapper new_, boolean init, PyType subtype, PyObject[] args,
            String[] keywords) {
        ArgParser ap = new ArgParser("str", args, keywords, new String[] {"object"}, 0);
        PyObject S = ap.getPyObject(0, null);
        // Get the textual representation of the object into str/bytes form
        String str;
        if (S == null) {
            str = "";
        } else {
            // Let the object tell us its representation: this may be str or unicode.
            S = S.__str__();
            if (S instanceof PyUnicode) {
                // Encoding will raise UnicodeEncodeError if not 7-bit clean.
                str = codecs.encode((PyUnicode) S, null, null);
            } else {
                // Must be str/bytes, and should be 8-bit clean already.
                str = S.toString();
            }
        }
        if (new_.for_type == subtype) {
            return new PyString(str);
        } else {
            return new PyStringDerived(subtype, str);
        }
    }

    public int[] toCodePoints() {
        int n = getString().length();
        int[] codePoints = new int[n];
        for (int i = 0; i < n; i++) {
            codePoints[i] = getString().charAt(i);
        }
        return codePoints;
    }

    /**
     * Return a read-only buffer view of the contents of the string, treating it as a sequence of
     * unsigned bytes. The caller specifies its requirements and navigational capabilities in the
     * <code>flags</code> argument (see the constants in interface {@link PyBUF} for an
     * explanation). The method may return the same PyBuffer object to more than one consumer.
     *
     * @param flags consumer requirements
     * @return the requested buffer
     */
    @Override
    public synchronized PyBuffer getBuffer(int flags) {
        // If we have already exported a buffer it may still be available for re-use
        BaseBuffer pybuf = getExistingBuffer(flags);
        if (pybuf == null) {
            /*
             * No existing export we can re-use. Return a buffer, but specialised to defer
             * construction of the buf object, and cache a soft reference to it.
             */
            pybuf = new SimpleStringBuffer(flags, this, getString());
            export = new SoftReference<BaseBuffer>(pybuf);
        }
        return pybuf;
    }

    /**
     * Helper for {@link #getBuffer(int)} that tries to re-use an existing exported buffer, or
     * returns null if can't.
     */
    private BaseBuffer getExistingBuffer(int flags) {
        BaseBuffer pybuf = null;
        if (export != null) {
            // A buffer was exported at some time.
            pybuf = export.get();
            if (pybuf != null) {
                /*
                 * And this buffer still exists. Even in the case where the buffer has been released
                 * by all its consumers, it remains safe to re-acquire it because the target String
                 * has not changed.
                 */
                pybuf = pybuf.getBufferAgain(flags);
            }
        }
        return pybuf;
    }

    /**
     * Return a substring of this object as a Java String.
     *
     * @param start the beginning index, inclusive.
     * @param end the ending index, exclusive.
     * @return the specified substring.
     */
    public String substring(int start, int end) {
        return getString().substring(start, end);
    }

    @Override
    public PyString __str__() {
        return str___str__();
    }

    @ExposedMethod(doc = BuiltinDocs.str___str___doc)
    final PyString str___str__() {
        if (getClass() == PyString.class) {
            return this;
        }
        return new PyString(getString(), true);
    }

    @Override
    public PyUnicode __unicode__() {
        return new PyUnicode(this);  // Decodes with default codec.
    }

    @Override
    public int __len__() {
        return str___len__();
    }

    @ExposedMethod(doc = BuiltinDocs.str___len___doc)
    final int str___len__() {
        return getString().length();
    }

    @Override
    public String toString() {
        return getString();
    }

    public String internedString() {
        if (interned) {
            return getString();
        } else {
            string = getString().intern();
            interned = true;
            return getString();
        }
    }

    @Override
    public PyString __repr__() {
        return str___repr__();
    }

    @ExposedMethod(doc = BuiltinDocs.str___repr___doc)
    final PyString str___repr__() {
        return new PyString(encode_UnicodeEscape(getString(), true));
    }

    private static char[] hexdigit = "0123456789abcdef".toCharArray();

    public static String encode_UnicodeEscape(String str, boolean use_quotes) {
        char quote = use_quotes ? '?' : 0;
        return encode_UnicodeEscape(str, quote);
    }

    /**
     * The inner logic of the string __repr__ producing an ASCII representation of the target
     * string, optionally in quotations. The caller can determine whether the returned string will
     * be wrapped in quotation marks, and whether Python rules are used to choose them through
     * <code>quote</code>.
     *
     * @param str
     * @param quoteChar '"' or '\'' use that, '?' = let Python choose, 0 or anything = no quotes
     * @return encoded string (possibly the same string if unchanged)
     */
    static String encode_UnicodeEscape(String str, char quote) {

        // Choose whether to quote and the actual quote character
        boolean use_quotes;
        switch (quote) {
            case '?':
                use_quotes = true;
                // Python rules
                quote = str.indexOf('\'') >= 0 && str.indexOf('"') == -1 ? '"' : '\'';
                break;
            case '"':
            case '\'':
                use_quotes = true;
                break;
            default:
                use_quotes = false;
                break;
        }

        // Allocate a buffer for the result (25% bigger and room for quotes)
        int size = str.length();
        StringBuilder v = new StringBuilder(size + (size >> 2) + 2);

        if (use_quotes) {
            v.append(quote);
        }

        // Now chunter through the original string a character at a time
        for (int i = 0; size-- > 0;) {
            int ch = str.charAt(i++);
            // Escape quotes and backslash
            if ((use_quotes && ch == quote) || ch == '\\') {
                v.append('\\');
                v.append((char) ch);
                continue;
            }
            /* Map UTF-16 surrogate pairs to Unicode \UXXXXXXXX escapes */
            else if (size > 0 && ch >= 0xD800 && ch < 0xDC00) {
                char ch2 = str.charAt(i++);
                size--;
                if (ch2 >= 0xDC00 && ch2 <= 0xDFFF) {
                    int ucs = (((ch & 0x03FF) << 10) | (ch2 & 0x03FF)) + 0x00010000;
                    v.append('\\');
                    v.append('U');
                    v.append(hexdigit[(ucs >> 28) & 0xf]);
                    v.append(hexdigit[(ucs >> 24) & 0xf]);
                    v.append(hexdigit[(ucs >> 20) & 0xf]);
                    v.append(hexdigit[(ucs >> 16) & 0xf]);
                    v.append(hexdigit[(ucs >> 12) & 0xf]);
                    v.append(hexdigit[(ucs >> 8) & 0xf]);
                    v.append(hexdigit[(ucs >> 4) & 0xf]);
                    v.append(hexdigit[ucs & 0xf]);
                    continue;
                }
                /* Fall through: isolated surrogates are copied as-is */
                i--;
                size++;
            }
            /* Map 16-bit characters to '\\uxxxx' */
            if (ch >= 256) {
                v.append('\\');
                v.append('u');
                v.append(hexdigit[(ch >> 12) & 0xf]);
                v.append(hexdigit[(ch >> 8) & 0xf]);
                v.append(hexdigit[(ch >> 4) & 0xf]);
                v.append(hexdigit[ch & 15]);
            }
            /* Map special whitespace to '\t', \n', '\r' */
            else if (ch == '\t') {
                v.append("\\t");
            } else if (ch == '\n') {
                v.append("\\n");
            } else if (ch == '\r') {
                v.append("\\r");
            } else if (ch < ' ' || ch >= 127) {
                /* Map non-printable US ASCII to '\xNN' */
                v.append('\\');
                v.append('x');
                v.append(hexdigit[(ch >> 4) & 0xf]);
                v.append(hexdigit[ch & 0xf]);
            } else {/* Copy everything else as-is */
                v.append((char) ch);
            }
        }

        if (use_quotes) {
            v.append(quote);
        }

        // Return the original string if we didn't quote or escape anything
        return v.length() > size ? v.toString() : str;
    }

    private static ucnhashAPI pucnHash = null;

    public static String decode_UnicodeEscape(String str, int start, int end, String errors,
            boolean unicode) {
        StringBuilder v = new StringBuilder(end - start);
        for (int s = start; s < end;) {
            char ch = str.charAt(s);
            /* Non-escape characters are interpreted as Unicode ordinals */
            if (ch != '\\') {
                v.append(ch);
                s++;
                continue;
            }
            int loopStart = s;
            /* \ - Escapes */
            s++;
            if (s == end) {
                s = codecs.insertReplacementAndGetResume(v, errors, "unicodeescape", //
                        str, loopStart, s + 1, "\\ at end of string");
                continue;
            }
            ch = str.charAt(s++);
            switch (ch) {
                /* \x escapes */
                case '\n':
                    break;
                case '\\':
                    v.append('\\');
                    break;
                case '\'':
                    v.append('\'');
                    break;
                case '\"':
                    v.append('\"');
                    break;
                case 'b':
                    v.append('\b');
                    break;
                case 'f':
                    v.append('\014');
                    break; /* FF */
                case 't':
                    v.append('\t');
                    break;
                case 'n':
                    v.append('\n');
                    break;
                case 'r':
                    v.append('\r');
                    break;
                case 'v':
                    v.append('\013');
                    break; /* VT */
                case 'a':
                    v.append('\007');
                    break; /* BEL, not classic C */
                /* \OOO (octal) escapes */
                case '0':
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                    int x = Character.digit(ch, 8);
                    for (int j = 0; j < 2 && s < end; j++, s++) {
                        ch = str.charAt(s);
                        if (ch < '0' || ch > '7') {
                            break;
                        }
                        x = (x << 3) + Character.digit(ch, 8);
                    }
                    v.append((char) x);
                    break;
                case 'x':
                    s = hexescape(v, errors, 2, s, str, end, "truncated \\xXX");
                    break;
                case 'u':
                    if (!unicode) {
                        v.append('\\');
                        v.append('u');
                        break;
                    }
                    s = hexescape(v, errors, 4, s, str, end, "truncated \\uXXXX");
                    break;
                case 'U':
                    if (!unicode) {
                        v.append('\\');
                        v.append('U');
                        break;
                    }
                    s = hexescape(v, errors, 8, s, str, end, "truncated \\UXXXXXXXX");
                    break;
                case 'N':
                    if (!unicode) {
                        v.append('\\');
                        v.append('N');
                        break;
                    }
                    /*
                     * Ok, we need to deal with Unicode Character Names now, make sure we've
                     * imported the hash table data...
                     */
                    if (pucnHash == null) {
                        PyObject mod = imp.importName("ucnhash", true);
                        mod = mod.__call__();
                        pucnHash = (ucnhashAPI) mod.__tojava__(Object.class);
                        if (pucnHash.getCchMax() < 0) {
                            throw Py.UnicodeError("Unicode names not loaded");
                        }
                    }
                    if (str.charAt(s) == '{') {
                        int startName = s + 1;
                        int endBrace = startName;
                        /*
                         * look for either the closing brace, or we exceed the maximum length of the
                         * unicode character names
                         */
                        int maxLen = pucnHash.getCchMax();
                        while (endBrace < end && str.charAt(endBrace) != '}'
                                && (endBrace - startName) <= maxLen) {
                            endBrace++;
                        }
                        if (endBrace != end && str.charAt(endBrace) == '}') {
                            int value = pucnHash.getValue(str, startName, endBrace);
                            if (storeUnicodeCharacter(value, v)) {
                                s = endBrace + 1;
                            } else {
                                s = codecs.insertReplacementAndGetResume( //
                                        v, errors, "unicodeescape", //
                                        str, loopStart, endBrace + 1, "illegal Unicode character");
                            }
                        } else {
                            s = codecs.insertReplacementAndGetResume(v, errors, "unicodeescape", //
                                    str, loopStart, endBrace, "malformed \\N character escape");
                        }
                        break;
                    } else {
                        s = codecs.insertReplacementAndGetResume(v, errors, "unicodeescape", //
                                str, loopStart, s + 1, "malformed \\N character escape");
                    }
                    break;
                default:
                    v.append('\\');
                    v.append(str.charAt(s - 1));
                    break;
            }
        }
        return v.toString();
    }

    private static int hexescape(StringBuilder partialDecode, String errors, int digits,
            int hexDigitStart, String str, int size, String errorMessage) {
        if (hexDigitStart + digits > size) {
            return codecs.insertReplacementAndGetResume(partialDecode, errors, "unicodeescape", str,
                    hexDigitStart - 2, size, errorMessage);
        }
        int i = 0;
        int x = 0;
        for (; i < digits; ++i) {
            char c = str.charAt(hexDigitStart + i);
            int d = Character.digit(c, 16);
            if (d == -1) {
                return codecs.insertReplacementAndGetResume(partialDecode, errors, "unicodeescape",
                        str, hexDigitStart - 2, hexDigitStart + i + 1, errorMessage);
            }
            x = (x << 4) & ~0xF;
            if (c >= '0' && c <= '9') {
                x += c - '0';
            } else if (c >= 'a' && c <= 'f') {
                x += 10 + c - 'a';
            } else {
                x += 10 + c - 'A';
            }
        }
        if (storeUnicodeCharacter(x, partialDecode)) {
            return hexDigitStart + i;
        } else {
            return codecs.insertReplacementAndGetResume(partialDecode, errors, "unicodeescape", str,
                    hexDigitStart - 2, hexDigitStart + i + 1, "illegal Unicode character");
        }
    }

    /* pass in an int since this can be a UCS-4 character */
    private static boolean storeUnicodeCharacter(int value, StringBuilder partialDecode) {
        if (value < 0 || (value >= 0xD800 && value <= 0xDFFF)) {
            return false;
        } else if (value <= PySystemState.maxunicode) {
            partialDecode.appendCodePoint(value);
            return true;
        }
        return false;
    }

    @ExposedMethod(doc = BuiltinDocs.str___getitem___doc)
    final PyObject str___getitem__(PyObject index) {
        PyObject ret = seq___finditem__(index);
        if (ret == null) {
            throw Py.IndexError("string index out of range");
        }
        return ret;
    }

    // XXX: need doc
    @ExposedMethod(defaults = "null")
    final PyObject str___getslice__(PyObject start, PyObject stop, PyObject step) {
        return seq___getslice__(start, stop, step);
    }

    @Override
    public int __cmp__(PyObject other) {
        return str___cmp__(other);
    }

    @ExposedMethod(type = MethodType.CMP)
    final int str___cmp__(PyObject other) {
        if (!(other instanceof PyString)) {
            return -2;
        }

        int c = getString().compareTo(((PyString) other).getString());
        return c < 0 ? -1 : c > 0 ? 1 : 0;
    }

    @Override
    public PyObject __eq__(PyObject other) {
        return str___eq__(other);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.str___eq___doc)
    final PyObject str___eq__(PyObject other) {
        String s = coerce(other);
        if (s == null) {
            return null;
        }
        return getString().equals(s) ? Py.True : Py.False;
    }

    @Override
    public PyObject __ne__(PyObject other) {
        return str___ne__(other);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.str___ne___doc)
    final PyObject str___ne__(PyObject other) {
        String s = coerce(other);
        if (s == null) {
            return null;
        }
        return getString().equals(s) ? Py.False : Py.True;
    }

    @Override
    public PyObject __lt__(PyObject other) {
        return str___lt__(other);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.str___lt___doc)
    final PyObject str___lt__(PyObject other) {
        String s = coerce(other);
        if (s == null) {
            return null;
        }
        return getString().compareTo(s) < 0 ? Py.True : Py.False;
    }

    @Override
    public PyObject __le__(PyObject other) {
        return str___le__(other);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.str___le___doc)
    final PyObject str___le__(PyObject other) {
        String s = coerce(other);
        if (s == null) {
            return null;
        }
        return getString().compareTo(s) <= 0 ? Py.True : Py.False;
    }

    @Override
    public PyObject __gt__(PyObject other) {
        return str___gt__(other);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.str___gt___doc)
    final PyObject str___gt__(PyObject other) {
        String s = coerce(other);
        if (s == null) {
            return null;
        }
        return getString().compareTo(s) > 0 ? Py.True : Py.False;
    }

    @Override
    public PyObject __ge__(PyObject other) {
        return str___ge__(other);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.str___ge___doc)
    final PyObject str___ge__(PyObject other) {
        String s = coerce(other);
        if (s == null) {
            return null;
        }
        return getString().compareTo(s) >= 0 ? Py.True : Py.False;
    }

    /** Interpret the object as a Java String representing bytes or return <code>null</code>. */
    private static String coerce(PyObject o) {
        if (o instanceof PyString && !(o instanceof PyUnicode)) {
            return o.toString();
        }
        return null;
    }

    @Override
    public int hashCode() {
        return str___hash__();
    }

    @ExposedMethod(doc = BuiltinDocs.str___hash___doc)
    final int str___hash__() {
        return getString().hashCode();
    }

    /**
     * @return a byte array with one byte for each char in this object's underlying String. Each
     *         byte contains the low-order bits of its corresponding char.
     */
    public byte[] toBytes() {
        return StringUtil.toBytes(getString());
    }

    @Override
    public Object __tojava__(Class<?> c) {
        if (c.isAssignableFrom(String.class)) {
            /*
             * If c is a CharSequence we assume the caller is prepared to get maybe not an actual
             * String. In that case we avoid conversion so the caller can do special stuff with the
             * returned PyString or PyUnicode or whatever. (If c is Object.class, the caller usually
             * expects to get actually a String)
             */
            return c == CharSequence.class ? this : getString();
        }

        if (c == Character.TYPE || c == Character.class) {
            if (getString().length() == 1) {
                return getString().charAt(0);
            }
        }

        if (c.isArray()) {
            if (c.getComponentType() == Byte.TYPE) {
                return toBytes();
            }
            if (c.getComponentType() == Character.TYPE) {
                return getString().toCharArray();
            }
        }

        if (c.isAssignableFrom(Collection.class)) {
            List<Object> list = new ArrayList();
            for (int i = 0; i < __len__(); i++) {
                list.add(pyget(i).__tojava__(String.class));
            }
            return list;
        }

        if (c.isInstance(this)) {
            return this;
        }

        return Py.NoConversion;
    }

    @Override
    protected PyObject pyget(int i) {
        // Method is overridden in PyUnicode, so definitely a PyString
        return Py.makeCharacter(string.charAt(i));
    }

    public int getInt(int i) {
        return string.charAt(i);
    }

    @Override
    protected PyObject getslice(int start, int stop, int step) {
        if (step > 0 && stop < start) {
            stop = start;
        }
        if (step == 1) {
            return fromSubstring(start, stop);
        } else {
            int n = sliceLength(start, stop, step);
            char new_chars[] = new char[n];
            int j = 0;
            for (int i = start; j < n; i += step) {
                new_chars[j++] = getString().charAt(i);
            }

            return createInstance(new String(new_chars), true);
        }
    }

    /**
     * Create an instance of the same type as this object, from the Java String given as argument.
     * This is to be overridden in a subclass to return its own type.
     *
     * @param str to wrap
     * @return instance wrapping {@code str}
     */
    public PyString createInstance(String str) {
        return new PyString(str);
    }

    /**
     * Create an instance of the same type as this object, from the Java String given as argument.
     * This is to be overridden in a subclass to return its own type.
     *
     * @param str Java string representing the characters (as Java UTF-16).
     * @param isBasic is ignored in <code>PyString</code> (effectively true).
     * @return instance wrapping {@code str}
     */
    protected PyString createInstance(String str, boolean isBasic) {
        // ignore isBasic, doesn't apply to PyString, just PyUnicode
        return new PyString(str);
    }

    /**
     * Return a Java <code>String</code> that is the Jython-internal equivalent of the byte-like
     * argument (a <code>str</code> or any object that supports a one-dimensional byte buffer). If
     * the argument is not acceptable (this includes a <code>unicode</code> argument) return null.
     *
     * @param obj to coerce to a String
     * @return coerced value or <code>null</code> if it can't be
     */
    private static String asU16BytesOrNull(PyObject obj) {
        if (obj instanceof PyString) {
            if (obj instanceof PyUnicode) {
                return null;
            }
            // str but not unicode object: go directly to the String
            return ((PyString) obj).getString();
        } else if (obj instanceof BufferProtocol) {
            // Other object with buffer API: briefly access the buffer
            try (PyBuffer buf = ((BufferProtocol) obj).getBuffer(PyBUF.FULL_RO)) {
                return buf.toString();
            }
        } else {
            return null;
        }
    }

    /**
     * Return a String equivalent to the argument. This is a helper function to those methods that
     * accept any byte array type (any object that supports a one-dimensional byte buffer), but
     * <b>not</b> a <code>unicode</code>.
     *
     * @param obj to coerce to a String
     * @return coerced value
     * @throws PyException {@code TypeError} if the coercion fails (including <code>unicode</code>)
     */
    protected static String asU16BytesOrError(PyObject obj) throws PyException {
        String ret = asU16BytesOrNull(obj);
        if (ret != null) {
            return ret;
        } else {
            throw Py.TypeError("expected str, bytearray or other buffer compatible object");
        }
    }

    /**
     * Return a String equivalent to the argument according to the calling conventions of methods
     * that accept as a byte string anything bearing the buffer interface, or accept
     * <code>PyNone</code>, but <b>not</b> a <code>unicode</code>. (Or the argument may be omitted,
     * showing up here as null.) These include the <code>strip</code> and <code>split</code> methods
     * of <code>str</code>, where a null indicates that the criterion is whitespace, and
     * <code>str.translate</code>.
     *
     * @param obj to coerce to a String or null
     * @param name of method
     * @return coerced value or null
     * @throws PyException if the coercion fails (including <code>unicode</code>)
     */
    private static String asU16BytesNullOrError(PyObject obj, String name) throws PyException {
        if (obj == null || obj == Py.None) {
            return null;
        } else {
            String ret = asU16BytesOrNull(obj);
            if (ret != null) {
                return ret;
            } else if (name == null) {
                // A nameless method is the client
                throw Py.TypeError("expected None, str or buffer compatible object");
            } else {
                // Tuned for .strip and its relations, which supply their name
                throw Py.TypeError(name + " arg must be None, str or buffer compatible object");
            }
        }
    }

    @Override
    public boolean __contains__(PyObject o) {
        return str___contains__(o);
    }

    @ExposedMethod(doc = BuiltinDocs.str___contains___doc)
    final boolean str___contains__(PyObject o) {
        String other = asU16BytesOrNull(o);
        if (other != null) {
            return getString().indexOf(other) >= 0;
        } else if (o instanceof PyUnicode) {
            return decode().__contains__(o);
        } else {
            throw Py.TypeError("'in <string>' requires string as left operand, not "
                    + (o == null ? Py.None : o).getType().fastGetName());
        }
    }

    @Override
    protected PyObject repeat(int count) {
        if (count < 0) {
            count = 0;
        }
        int s = getString().length();
        if ((long) s * count > Integer.MAX_VALUE) {
            // Since Strings store their data in an array, we can't make one
            // longer than Integer.MAX_VALUE. Without this check we get
            // NegativeArraySize exceptions when we create the array on the
            // line with a wrapped int.
            throw Py.OverflowError("max str len is " + Integer.MAX_VALUE);
        }
        char new_chars[] = new char[s * count];
        for (int i = 0; i < count; i++) {
            getString().getChars(0, s, new_chars, i * s);
        }
        return createInstance(new String(new_chars));
    }

    @Override
    public PyObject __mul__(PyObject o) {
        return str___mul__(o);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.str___mul___doc)
    final PyObject str___mul__(PyObject o) {
        if (!o.isIndex()) {
            return null;
        }
        return repeat(o.asIndex(Py.OverflowError));
    }

    @Override
    public PyObject __rmul__(PyObject o) {
        return str___rmul__(o);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.str___rmul___doc)
    final PyObject str___rmul__(PyObject o) {
        if (!o.isIndex()) {
            return null;
        }
        return repeat(o.asIndex(Py.OverflowError));
    }

    /**
     * {@inheritDoc} For a <code>str</code> addition means concatenation and returns a
     * <code>str</code> ({@link PyString}) result, except when a {@link PyUnicode} argument is
     * given, when a <code>PyUnicode</code> results.
     */
    @Override
    public PyObject __add__(PyObject other) {
        return str___add__(other);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.str___add___doc)
    final PyObject str___add__(PyObject other) {
        // Expect other to be some kind of byte-like object.
        String otherStr = asU16BytesOrNull(other);
        if (otherStr != null) {
            // Yes it is: concatenate as strings, which are guaranteed byte-like.
            return new PyString(getString().concat(otherStr), true);
        } else if (other instanceof PyUnicode) {
            // Escalate the problem to PyUnicode
            return decode().__add__(other);
        } else {
            // Allow PyObject._basic_add to pick up the pieces or raise informative error
            return null;
        }
    }

    @ExposedMethod(doc = BuiltinDocs.str___getnewargs___doc)
    final PyTuple str___getnewargs__() {
        return new PyTuple(new PyString(this.getString()));
    }

    @Override
    public PyTuple __getnewargs__() {
        return str___getnewargs__();
    }

    @Override
    public PyObject __mod__(PyObject other) {
        return str___mod__(other);
    }

    @ExposedMethod(doc = BuiltinDocs.str___mod___doc)
    public PyObject str___mod__(PyObject other) {
        StringFormatter fmt = new StringFormatter(getString(), false);
        return fmt.format(other);
    }

    @Override
    public PyObject __int__() {
        try {
            return Py.newInteger(atoi(10));
        } catch (PyException e) {
            if (e.match(Py.OverflowError)) {
                return atol(10);
            }
            throw e;
        }
    }

    @Override
    public PyObject __long__() {
        return atol(10);
    }

    @Override
    public PyFloat __float__() {
        return new PyFloat(atof());
    }

    @Override
    public PyObject __pos__() {
        throw Py.TypeError("bad operand type for unary +");
    }

    @Override
    public PyObject __neg__() {
        throw Py.TypeError("bad operand type for unary -");
    }

    @Override
    public PyObject __invert__() {
        throw Py.TypeError("bad operand type for unary ~");
    }

    @Override
    public PyComplex __complex__() {
        return atocx();
    }

    // Add in methods from string module
    public String lower() {
        return str_lower();
    }

    @ExposedMethod(doc = BuiltinDocs.str_lower_doc)
    final String str_lower() {
        String s = getString();
        int n = s.length();
        if (n == 1) {
            // Special-case single byte string
            char c = s.charAt(0);
            return _isupper(c) ? String.valueOf((char) (c ^ SWAP_CASE)) : s;
        } else {
            // Copy chars to buffer, converting to lower-case.
            char[] buf = new char[n];
            for (int i = 0; i < n; i++) {
                char c = s.charAt(i);
                buf[i] = _isupper(c) ? (char) (c ^ SWAP_CASE) : c;
            }
            return new String(buf);
        }
    }

    public String upper() {
        return str_upper();
    }

    @ExposedMethod(doc = BuiltinDocs.str_upper_doc)
    final String str_upper() {
        String s = getString();
        int n = s.length();
        if (n == 1) {
            // Special-case single byte string
            char c = s.charAt(0);
            return _islower(c) ? String.valueOf((char) (c ^ SWAP_CASE)) : s;
        } else {
            // Copy chars to buffer, converting to upper-case.
            char[] buf = new char[n];
            for (int i = 0; i < n; i++) {
                char c = s.charAt(i);
                buf[i] = _islower(c) ? (char) (c ^ SWAP_CASE) : c;
            }
            return new String(buf);
        }
    }

    public String title() {
        return str_title();
    }

    @ExposedMethod(doc = BuiltinDocs.str_title_doc)
    final String str_title() {
        char[] chars = getString().toCharArray();
        int n = chars.length;
        boolean previous_is_cased = false;
        for (int i = 0; i < n; i++) {
            char ch = chars[i];
            if (_isalpha(ch)) {
                if (previous_is_cased) {
                    // Should be lower case
                    if (_isupper(ch)) {
                        chars[i] = (char) (ch ^ SWAP_CASE);
                    }
                } else {
                    // Should be upper case
                    if (_islower(ch)) {
                        chars[i] = (char) (ch ^ SWAP_CASE);
                    }
                }
                // And this was a letter
                previous_is_cased = true;
            } else {
                // This was not a letter
                previous_is_cased = false;
            }
        }
        return new String(chars);
    }

    public String swapcase() {
        return str_swapcase();
    }

    @ExposedMethod(doc = BuiltinDocs.str_swapcase_doc)
    final String str_swapcase() {
        String s = getString();
        int n = s.length();
        if (n == 1) {
            // Special-case single byte string
            char c = s.charAt(0);
            return _isalpha(c) ? String.valueOf((char) (c ^ SWAP_CASE)) : s;
        } else {
            // Copy chars to buffer, converting lower to upper case, upper to lower case.
            char[] buf = new char[n];
            for (int i = 0; i < n; i++) {
                char c = s.charAt(i);
                buf[i] = _isalpha(c) ? (char) (c ^ SWAP_CASE) : c;
            }
            return new String(buf);
        }
    }

    // Bit to twiddle (XOR) for lowercase letter to uppercase and vice-versa.
    private static final int SWAP_CASE = 0x20;

    /**
     * Equivalent of Python <code>str.strip()</code> with no argument, meaning strip whitespace. Any
     * whitespace byte/character will be discarded from either end of this <code>str</code>.
     *
     * @return a new String, stripped of the whitespace characters/bytes
     */
    public String strip() {
        return _strip();
    }

    /**
     * Equivalent of Python <code>str.strip()</code>.
     *
     * @param stripChars characters to strip from either end of this str/bytes, or null
     * @return a new String, stripped of the specified characters/bytes
     */
    public String strip(String stripChars) {
        return _strip(stripChars);
    }

    /**
     * Equivalent of Python <code>str.strip()</code>. Any byte/character matching one of those in
     * <code>stripChars</code> will be discarded from either end of this <code>str</code>. If
     * <code>stripChars == null</code>, whitespace will be stripped. If <code>stripChars</code> is a
     * <code>PyUnicode</code>, the result will also be a <code>PyUnicode</code>.
     *
     * @param stripChars characters to strip from either end of this str/bytes, or null
     * @return a new <code>PyString</code> (or {@link PyUnicode}), stripped of the specified
     *         characters/bytes
     */
    public PyObject strip(PyObject stripChars) {
        return str_strip(stripChars);
    }

    @ExposedMethod(defaults = "null", doc = BuiltinDocs.str_strip_doc)
    final PyObject str_strip(PyObject chars) {
        if (chars instanceof PyUnicode) {
            // Promote the problem to a Unicode one
            return ((PyUnicode) decode()).unicode_strip(chars);
        } else {
            // It ought to be None, null, some kind of bytes with the buffer API.
            String stripChars = asU16BytesNullOrError(chars, "strip");
            // Strip specified characters or whitespace if stripChars == null
            return new PyString(_strip(stripChars), true);
        }
    }

    /**
     * Implementation of Python <code>str.strip()</code> common to exposed and Java API, when
     * stripping whitespace. Any whitespace byte/character will be discarded from either end of this
     * <code>str</code>.
     * <p>
     * Implementation note: although a <code>str</code> contains only bytes, this method is also
     * called by {@link PyUnicode#unicode_strip(PyObject)} when this is a basic-plane string.
     *
     * @return a new String, stripped of the whitespace characters/bytes
     */
    protected final String _strip() {
        // Rightmost non-whitespace
        int right = _findRight();
        if (right < 0) {
            // They're all whitespace
            return "";
        } else {
            // Leftmost non-whitespace character: right known not to be a whitespace
            int left = _findLeft(right);
            return getString().substring(left, right + 1);
        }
    }

    /**
     * Implementation of Python <code>str.strip()</code> common to exposed and Java API. Any
     * byte/character matching one of those in <code>stripChars</code> will be discarded from either
     * end of this <code>str</code>. If <code>stripChars == null</code>, whitespace will be
     * stripped.
     * <p>
     * Implementation note: although a <code>str</code> contains only bytes, this method is also
     * called by {@link PyUnicode#unicode_strip(PyObject)} when both arguments are basic-plane
     * strings.
     *
     * @param stripChars characters to strip or null
     * @return a new String, stripped of the specified characters/bytes
     */
    protected final String _strip(String stripChars) {
        if (stripChars == null) {
            // Divert to the whitespace version
            return _strip();
        } else {
            // Rightmost non-matching character
            int right = _findRight(stripChars);
            if (right < 0) {
                // They all match
                return "";
            } else {
                // Leftmost non-matching character: right is known not to match
                int left = _findLeft(stripChars, right);
                return getString().substring(left, right + 1);
            }
        }
    }

    /**
     * Helper for <code>strip</code>, <code>lstrip</code> implementation, when stripping whitespace.
     *
     * @param right rightmost extent of string search
     * @return index of leftmost non-whitespace character or <code>right</code> if they all are.
     */
    protected int _findLeft(int right) {
        String s = getString();
        for (int left = 0; left < right; left++) {
            if (!BaseBytes.isspace((byte) s.charAt(left))) {
                return left;
            }
        }
        return right;
    }

    /**
     * Helper for <code>strip</code>, <code>lstrip</code> implementation, when stripping specified
     * characters.
     *
     * @param stripChars specifies set of characters to strip
     * @param right rightmost extent of string search
     * @return index of leftmost character not in <code>stripChars</code> or <code>right</code> if
     *         they all are.
     */
    private int _findLeft(String stripChars, int right) {
        String s = getString();
        for (int left = 0; left < right; left++) {
            if (stripChars.indexOf(s.charAt(left)) < 0) {
                return left;
            }
        }
        return right;
    }

    /**
     * Helper for <code>strip</code>, <code>rstrip</code> implementation, when stripping whitespace.
     *
     * @return index of rightmost non-whitespace character or -1 if they all are.
     */
    protected int _findRight() {
        String s = getString();
        for (int right = s.length(); --right >= 0;) {
            if (!BaseBytes.isspace((byte) s.charAt(right))) {
                return right;
            }
        }
        return -1;
    }

    /**
     * Helper for <code>strip</code>, <code>rstrip</code> implementation, when stripping specified
     * characters.
     *
     * @param stripChars specifies set of characters to strip
     * @return index of rightmost character not in <code>stripChars</code> or -1 if they all are.
     */
    private int _findRight(String stripChars) {
        String s = getString();
        for (int right = s.length(); --right >= 0;) {
            if (stripChars.indexOf(s.charAt(right)) < 0) {
                return right;
            }
        }
        return -1;
    }

    /**
     * Equivalent of Python <code>str.lstrip()</code> with no argument, meaning strip whitespace.
     * Any whitespace byte/character will be discarded from the left of this <code>str</code>.
     *
     * @return a new String, stripped of the whitespace characters/bytes
     */
    public String lstrip() {
        return _lstrip();
    }

    /**
     * Equivalent of Python <code>str.lstrip()</code>.
     *
     * @param stripChars characters to strip from the left end of this str/bytes, or null
     * @return a new String, stripped of the specified characters/bytes
     */
    public String lstrip(String stripChars) {
        return _lstrip(stripChars);
    }

    /**
     * Equivalent of Python <code>str.lstrip()</code>. Any byte/character matching one of those in
     * <code>stripChars</code> will be discarded from the left end of this <code>str</code>. If
     * <code>stripChars == null</code>, whitespace will be stripped. If <code>stripChars</code> is a
     * <code>PyUnicode</code>, the result will also be a <code>PyUnicode</code>.
     *
     * @param stripChars characters to strip from the left end of this str/bytes, or null
     * @return a new <code>PyString</code> (or {@link PyUnicode}), stripped of the specified
     *         characters/bytes
     */
    public PyObject lstrip(PyObject stripChars) {
        return str_lstrip(stripChars);
    }

    @ExposedMethod(defaults = "null", doc = BuiltinDocs.str_lstrip_doc)
    final PyObject str_lstrip(PyObject chars) {
        if (chars instanceof PyUnicode) {
            // Promote the problem to a Unicode one
            return ((PyUnicode) decode()).unicode_lstrip(chars);
        } else {
            // It ought to be None, null, some kind of bytes with the buffer API.
            String stripChars = asU16BytesNullOrError(chars, "lstrip");
            // Strip specified characters or whitespace if stripChars == null
            return new PyString(_lstrip(stripChars), true);
        }
    }

    /**
     * Implementation of Python <code>str.lstrip()</code> common to exposed and Java API, when
     * stripping whitespace. Any whitespace byte/character will be discarded from the left end of
     * this <code>str</code>.
     * <p>
     * Implementation note: although a str contains only bytes, this method is also called by
     * {@link PyUnicode#unicode_lstrip(PyObject)} when this is a basic-plane string.
     *
     * @return a new String, stripped of the whitespace characters/bytes
     */
    protected final String _lstrip() {
        String s = getString();
        // Leftmost non-whitespace character: cannot exceed length
        int left = _findLeft(s.length());
        return s.substring(left);
    }

    /**
     * Implementation of Python <code>str.lstrip()</code> common to exposed and Java API. Any
     * byte/character matching one of those in <code>stripChars</code> will be discarded from the
     * left end of this <code>str</code>. If <code>stripChars == null</code>, whitespace will be
     * stripped.
     * <p>
     * Implementation note: although a <code>str</code> contains only bytes, this method is also
     * called by {@link PyUnicode#unicode_lstrip(PyObject)} when both arguments are basic-plane
     * strings.
     *
     * @param stripChars characters to strip or null
     * @return a new String, stripped of the specified characters/bytes
     */
    protected final String _lstrip(String stripChars) {
        if (stripChars == null) {
            // Divert to the whitespace version
            return _lstrip();
        } else {
            String s = getString();
            // Leftmost matching character: cannot exceed length
            int left = _findLeft(stripChars, s.length());
            return s.substring(left);
        }
    }

    /**
     * Equivalent of Python <code>str.rstrip()</code> with no argument, meaning strip whitespace.
     * Any whitespace byte/character will be discarded from the right end of this <code>str</code>.
     *
     * @return a new String, stripped of the whitespace characters/bytes
     */
    public String rstrip() {
        return _rstrip();
    }

    /**
     * Equivalent of Python <code>str.rstrip()</code>.
     *
     * @param stripChars characters to strip from either end of this str/bytes, or null
     * @return a new String, stripped of the specified characters/bytes
     */
    public String rstrip(String stripChars) {
        return _rstrip(stripChars);
    }

    /**
     * Equivalent of Python <code>str.rstrip()</code>. Any byte/character matching one of those in
     * <code>stripChars</code> will be discarded from the right end of this <code>str</code>. If
     * <code>stripChars == null</code>, whitespace will be stripped. If <code>stripChars</code> is a
     * <code>PyUnicode</code>, the result will also be a <code>PyUnicode</code>.
     *
     * @param stripChars characters to strip from the right end of this str/bytes, or null
     * @return a new <code>PyString</code> (or {@link PyUnicode}), stripped of the specified
     *         characters/bytes
     */
    public PyObject rstrip(PyObject stripChars) {
        return str_rstrip(stripChars);
    }

    @ExposedMethod(defaults = "null", doc = BuiltinDocs.str_rstrip_doc)
    final PyObject str_rstrip(PyObject chars) {
        if (chars instanceof PyUnicode) {
            // Promote the problem to a Unicode one
            return ((PyUnicode) decode()).unicode_rstrip(chars);
        } else {
            // It ought to be None, null, some kind of bytes with the buffer API.
            String stripChars = asU16BytesNullOrError(chars, "rstrip");
            // Strip specified characters or whitespace if stripChars == null
            return new PyString(_rstrip(stripChars), true);
        }
    }

    /**
     * Implementation of Python <code>str.rstrip()</code> common to exposed and Java API, when
     * stripping whitespace. Any whitespace byte/character will be discarded from the right end of
     * this <code>str</code>.
     * <p>
     * Implementation note: although a <code>str</code> contains only bytes, this method is also
     * called by {@link PyUnicode#unicode_rstrip(PyObject)} when this is a basic-plane string.
     *
     * @return a new String, stripped of the whitespace characters/bytes
     */
    protected final String _rstrip() {
        // Rightmost non-whitespace
        int right = _findRight();
        if (right < 0) {
            // They're all whitespace
            return "";
        } else {
            // Substring up to and including this rightmost non-whitespace
            return getString().substring(0, right + 1);
        }
    }

    /**
     * Implementation of Python <code>str.rstrip()</code> common to exposed and Java API. Any
     * byte/character matching one of those in <code>stripChars</code> will be discarded from the
     * right end of this <code>str</code>. If <code>stripChars == null</code>, whitespace will be
     * stripped.
     * <p>
     * Implementation note: although a <code>str</code> contains only bytes, this method is also
     * called by {@link PyUnicode#unicode_strip(PyObject)} when both arguments are basic-plane
     * strings.
     *
     * @param stripChars characters to strip or null
     * @return a new String, stripped of the specified characters/bytes
     */
    protected final String _rstrip(String stripChars) {
        if (stripChars == null) {
            // Divert to the whitespace version
            return _rstrip();
        } else {
            // Rightmost non-matching character
            int right = _findRight(stripChars);
            // Substring up to and including this rightmost non-matching character (or "")
            return getString().substring(0, right + 1);
        }
    }

    /**
     * Equivalent to Python <code>str.split()</code>, splitting on runs of whitespace.
     *
     * @return list(str) result
     */
    public PyList split() {
        return _split(null, -1);
    }

    /**
     * Equivalent to Python <code>str.split()</code>, splitting on a specified string.
     *
     * @param sep string to use as separator (or <code>null</code> if to split on whitespace)
     * @return list(str) result
     */
    public PyList split(String sep) {
        return _split(sep, -1);
    }

    /**
     * Equivalent to Python <code>str.split()</code>, splitting on a specified string.
     *
     * @param sep string to use as separator (or <code>null</code> if to split on whitespace)
     * @param maxsplit maximum number of splits to make (there may be <code>maxsplit+1</code>
     *            parts).
     * @return list(str) result
     */
    public PyList split(String sep, int maxsplit) {
        return _split(sep, maxsplit);
    }

    /**
     * Equivalent to Python <code>str.split()</code> returning a {@link PyList} of
     * <code>PyString</code>s (or <code>PyUnicode</code>s). The <code>str</code> will be split at
     * each occurrence of <code>sep</code>. If <code>sep == null</code>, whitespace will be used as
     * the criterion. If <code>sep</code> has zero length, a Python <code>ValueError</code> is
     * raised.
     *
     * @param sep string to use as separator (or <code>null</code> if to split on whitespace)
     * @return list(str) result
     */
    public PyList split(PyObject sep) {
        return str_split(sep, -1);
    }

    /**
     * As {@link #split(PyObject)} but if <code>maxsplit</code> &gt;=0 and there are more feasible
     * splits than <code>maxsplit</code>, the last element of the list contains the rest of the
     * string.
     *
     * @param sep string to use as separator (or <code>null</code> if to split on whitespace)
     * @param maxsplit maximum number of splits to make (there may be <code>maxsplit+1</code>
     *            parts).
     * @return list(str) result
     */
    public PyList split(PyObject sep, int maxsplit) {
        return str_split(sep, maxsplit);
    }

    @ExposedMethod(defaults = {"null", "-1"}, doc = BuiltinDocs.str_split_doc)
    final PyList str_split(PyObject sepObj, int maxsplit) {
        if (sepObj instanceof PyUnicode) {
            // Promote the problem to a Unicode one
            return ((PyUnicode) decode()).unicode_split(sepObj, maxsplit);
        } else {
            // It ought to be None, null, some kind of bytes with the buffer API.
            String sep = asU16BytesNullOrError(sepObj, "split");
            // Split on specified string or whitespace if sep == null
            return _split(sep, maxsplit);
        }
    }

    /**
     * Implementation of Python str.split() common to exposed and Java API returning a
     * {@link PyList} of <code>PyString</code>s. The <code>str</code> will be split at each
     * occurrence of <code>sep</code>. If <code>sep == null</code>, whitespace will be used as the
     * criterion. If <code>sep</code> has zero length, a Python <code>ValueError</code> is raised.
     * If <code>maxsplit</code> &gt;=0 and there are more feasible splits than <code>maxsplit</code>
     * the last element of the list contains the what is left over after the last split.
     * <p>
     * Implementation note: although a str contains only bytes, this method is also called by
     * {@link PyUnicode#unicode_split(PyObject, int)}.
     *
     * @param sep string to use as separator (or <code>null</code> if to split on whitespace)
     * @param maxsplit maximum number of splits to make (there may be <code>maxsplit+1</code>
     *            parts).
     * @return list(str) result
     */
    protected final PyList _split(String sep, int maxsplit) {
        if (sep == null) {
            // Split on runs of whitespace
            return splitfields(maxsplit);
        } else if (sep.length() == 0) {
            throw Py.ValueError("empty separator");
        } else {
            // Split on specified (non-empty) string
            return splitfields(sep, maxsplit);
        }
    }

    /**
     * Helper function for <code>.split</code>, in <code>str</code> and (when overridden) in
     * <code>unicode</code>, splitting on white space and returning a list of the separated parts.
     * If there are more than <code>maxsplit</code> feasible splits the last element of the list is
     * the remainder of the original (this) string.
     *
     * @param maxsplit limit on the number of splits (if &gt;=0)
     * @return <code>PyList</code> of split sections
     */
    protected PyList splitfields(int maxsplit) {
        /*
         * Result built here is a list of split parts, exactly as required for s.split(None,
         * maxsplit). If there are to be n splits, there will be n+1 elements in L.
         */
        PyList list = new PyList();

        String s = getString();
        int length = s.length(), start = 0, splits = 0, index;

        if (maxsplit < 0) {
            // Make all possible splits: there can't be more than:
            maxsplit = length;
        }

        // start is always the first character not consumed into a piece on the list
        while (start < length) {

            // Find the next occurrence of non-whitespace
            while (start < length) {
                if (!BaseBytes.isspace((byte) s.charAt(start))) {
                    // Break leaving start pointing at non-whitespace
                    break;
                }
                start++;
            }

            if (start >= length) {
                // Only found whitespace so there is no next segment
                break;

            } else if (splits >= maxsplit) {
                // The next segment is the last and contains all characters up to the end
                index = length;

            } else {
                // The next segment runs up to the next next whitespace or end
                for (index = start; index < length; index++) {
                    if (BaseBytes.isspace((byte) s.charAt(index))) {
                        // Break leaving index pointing at whitespace
                        break;
                    }
                }
            }

            // Make a piece from start up to index
            list.append(fromSubstring(start, index));
            splits++;

            // Start next segment search at that point
            start = index;
        }

        return list;
    }

    /**
     * Helper function for <code>.split</code> and <code>.replace</code>, in <code>str</code> and
     * <code>unicode</code>, returning a list of the separated parts. If there are more than
     * <code>maxsplit</code> occurrences of <code>sep</code> the last element of the list is the
     * remainder of the original (this) string. If <code>sep</code> is the zero-length string, the
     * split is between each character (as needed by <code>.replace</code>). The split sections will
     * be {@link PyUnicode} if this object is a <code>PyUnicode</code>.
     *
     * @param sep at occurrences of which this string should be split
     * @param maxsplit limit on the number of splits (if &gt;=0)
     * @return <code>PyList</code> of split sections
     */
    private PyList splitfields(String sep, int maxsplit) {
        /*
         * Result built here is a list of split parts, exactly as required for s.split(sep), or to
         * produce the result of s.replace(sep, r) by a subsequent call r.join(L). If there are to
         * be n splits, there will be n+1 elements in L.
         */
        PyList list = new PyList();

        String s = getString();
        int length = s.length();
        int sepLength = sep.length();

        if (maxsplit < 0) {
            // Make all possible splits: there can't be more than:
            maxsplit = length + 1;
        }

        if (maxsplit == 0) {
            // Degenerate case
            list.append(this);

        } else if (sepLength == 0) {
            /*
             * The separator is "". This cannot happen with s.split(""), as that's an error, but it
             * is used by s.replace("", A) and means that the result should be A interleaved between
             * the characters of s, before the first, and after the last, the number always limited
             * by maxsplit.
             */

            // There will be m+1 parts, where m = maxsplit or length+1 whichever is smaller.
            int m = (maxsplit > length) ? length + 1 : maxsplit;

            // Put an empty string first to make one split before the first character
            list.append(createInstance("")); // PyString or PyUnicode as this class
            int index;

            // Add m-1 pieces one character long
            for (index = 0; index < m - 1; index++) {
                list.append(fromSubstring(index, index + 1));
            }

            // And add the last piece, so there are m+1 splits (m+1 pieces)
            list.append(fromSubstring(index, length));

        } else {
            // Index of first character not yet in a piece on the list
            int start = 0;

            // Add at most maxsplit pieces
            for (int splits = 0; splits < maxsplit; splits++) {

                // Find the next occurrence of sep
                int index = s.indexOf(sep, start);

                if (index < 0) {
                    // No more occurrences of sep: we're done
                    break;

                } else {
                    // Make a piece from start up to where we found sep
                    list.append(fromSubstring(start, index));
                    // New start (of next piece) is just after sep
                    start = index + sepLength;
                }
            }

            // Last piece is the rest of the string (even if start==length)
            list.append(fromSubstring(start, length));
        }

        return list;
    }

    /**
     * Equivalent to Python <code>str.rsplit()</code>, splitting on runs of whitespace.
     *
     * @return list(str) result
     */
    public PyList rsplit() {
        return _rsplit(null, -1);
    }

    /**
     * Equivalent to Python <code>str.rsplit()</code>, splitting on a specified string.
     *
     * @param sep string to use as separator (or <code>null</code> if to split on whitespace)
     * @return list(str) result
     */
    public PyList rsplit(String sep) {
        return _rsplit(sep, -1);
    }

    /**
     * Equivalent to Python <code>str.rsplit()</code>, splitting on a specified string.
     *
     * @param sep string to use as separator (or <code>null</code> if to split on whitespace)
     * @param maxsplit maximum number of splits to make (there may be <code>maxsplit+1</code>
     *            parts).
     * @return list(str) result
     */
    public PyList rsplit(String sep, int maxsplit) {
        return _rsplit(sep, maxsplit);
    }

    /**
     * Equivalent to Python <code>str.rsplit()</code> returning a {@link PyList} of
     * <code>PyString</code>s (or <code>PyUnicode</code>s). The <code>str</code> will be split at
     * each occurrence of <code>sep</code>, working from the right. If <code>sep == null</code>,
     * whitespace will be used as the criterion. If <code>sep</code> has zero length, a Python
     * <code>ValueError</code> is raised.
     *
     * @param sep string to use as separator (or <code>null</code> if to split on whitespace)
     * @return list(str) result
     */
    public PyList rsplit(PyObject sep) {
        return str_rsplit(sep, -1);
    }

    /**
     * As {@link #rsplit(PyObject)} but if <code>maxsplit</code> &gt;=0 and there are more feasible
     * splits than <code>maxsplit</code> the last element of the list contains the rest of the
     * string.
     *
     * @param sep string to use as separator (or <code>null</code> if to split on whitespace)
     * @param maxsplit maximum number of splits to make (there may be <code>maxsplit+1</code>
     *            parts).
     * @return list(str) result
     */
    public PyList rsplit(PyObject sep, int maxsplit) {
        return str_rsplit(sep, maxsplit);
    }

    @ExposedMethod(defaults = {"null", "-1"}, doc = BuiltinDocs.str_split_doc)
    final PyList str_rsplit(PyObject sepObj, int maxsplit) {
        if (sepObj instanceof PyUnicode) {
            // Promote the problem to a Unicode one
            return ((PyUnicode) decode()).unicode_rsplit(sepObj, maxsplit);
        } else {
            // It ought to be None, null, some kind of bytes with the buffer API.
            String sep = asU16BytesNullOrError(sepObj, "rsplit");
            // Split on specified string or whitespace if sep == null
            return _rsplit(sep, maxsplit);
        }
    }

    /**
     * Implementation of Python <code>str.rsplit()</code> common to exposed and Java API returning a
     * {@link PyList} of <code>PyString</code>s. The <code>str</code> will be split at each
     * occurrence of <code>sep</code>, working from the right. If <code>sep == null</code>,
     * whitespace will be used as the criterion. If <code>sep</code> has zero length, a Python
     * <code>ValueError</code> is raised. If <code>maxsplit</code> &gt;=0 and there are more
     * feasible splits than <code>maxsplit</code> the first element of the list contains the what is
     * left over after the last split.
     * <p>
     * Implementation note: although a str contains only bytes, this method is also called by
     * {@link PyUnicode#unicode_rsplit(PyObject, int)} .
     *
     * @param sep string to use as separator (or <code>null</code> if to split on whitespace)
     * @param maxsplit maximum number of splits to make (there may be <code>maxsplit+1</code>
     *            parts).
     * @return list(str) result
     */
    protected final PyList _rsplit(String sep, int maxsplit) {
        if (sep == null) {
            // Split on runs of whitespace
            return rsplitfields(maxsplit);
        } else if (sep.length() == 0) {
            throw Py.ValueError("empty separator");
        } else {
            // Split on specified (non-empty) string
            return rsplitfields(sep, maxsplit);
        }
    }

    /**
     * Helper function for <code>.rsplit</code>, in <code>str</code> and (when overridden) in
     * <code>unicode</code>, splitting on white space and returning a list of the separated parts.
     * If there are more than <code>maxsplit</code> feasible splits the first element of the list is
     * the remainder of the original (this) string.
     *
     * @param maxsplit limit on the number of splits (if &gt;=0)
     * @return <code>PyList</code> of split sections
     */
    protected PyList rsplitfields(int maxsplit) {
        /*
         * Result built here (in reverse) is a list of split parts, exactly as required for
         * s.rsplit(None, maxsplit). If there are to be n splits, there will be n+1 elements.
         */
        PyList list = new PyList();

        String s = getString();
        int length = s.length(), end = length - 1, splits = 0, index;

        if (maxsplit < 0) {
            // Make all possible splits: there can't be more than:
            maxsplit = length;
        }

        // end is always the rightmost character not consumed into a piece on the list
        while (end >= 0) {

            // Find the next occurrence of non-whitespace (working leftwards)
            while (end >= 0) {
                if (!BaseBytes.isspace((byte) s.charAt(end))) {
                    // Break leaving end pointing at non-whitespace
                    break;
                }
                --end;
            }

            if (end < 0) {
                // Only found whitespace so there is no next segment
                break;

            } else if (splits >= maxsplit) {
                // The next segment is the last and contains all characters back to the beginning
                index = -1;

            } else {
                // The next segment runs back to the next next whitespace or beginning
                for (index = end; index >= 0; --index) {
                    if (BaseBytes.isspace((byte) s.charAt(index))) {
                        // Break leaving index pointing at whitespace
                        break;
                    }
                }
            }

            // Make a piece from index+1 start up to end+1
            list.append(fromSubstring(index + 1, end + 1));
            splits++;

            // Start next segment search at that point
            end = index;
        }

        list.reverse();
        return list;
    }

    /**
     * Helper function for <code>.rsplit</code>, in <code>str</code> and <code>unicode</code>,
     * returning a list of the separated parts, <em>in the reverse order</em> of their occurrence in
     * this string. If there are more than <code>maxsplit</code> occurrences of <code>sep</code> the
     * first element of the list is the left end of the original (this) string. The split sections
     * will be {@link PyUnicode} if this object is a <code>PyUnicode</code>.
     *
     * @param sep at occurrences of which this string should be split
     * @param maxsplit limit on the number of splits (if &gt;=0)
     * @return <code>PyList</code> of split sections
     */
    private PyList rsplitfields(String sep, int maxsplit) {
        /*
         * Result built here (in reverse) is a list of split parts, exactly as required for
         * s.rsplit(sep, maxsplit). If there are to be n splits, there will be n+1 elements.
         */
        PyList list = new PyList();

        String s = getString();
        int length = s.length();
        int sepLength = sep.length();

        if (maxsplit < 0) {
            // Make all possible splits: there can't be more than:
            maxsplit = length + 1;
        }

        if (maxsplit == 0) {
            // Degenerate case
            list.append(this);

        } else if (sepLength == 0) {
            // Empty separator is not allowed
            throw Py.ValueError("empty separator");

        } else {
            // Index of first character of the last piece already on the list
            int end = length;

            // Add at most maxsplit pieces
            for (int splits = 0; splits < maxsplit; splits++) {

                // Find the next occurrence of sep (working leftwards)
                int index = s.lastIndexOf(sep, end - sepLength);

                if (index < 0) {
                    // No more occurrences of sep: we're done
                    break;

                } else {
                    // Make a piece from where we found sep up to end
                    list.append(fromSubstring(index + sepLength, end));
                    // New end (of next piece) is where we found sep
                    end = index;
                }
            }

            // Last piece is the rest of the string (even if end==0)
            list.append(fromSubstring(0, end));
        }

        list.reverse();
        return list;
    }

    /**
     * Equivalent to Python <code>str.partition()</code>, splits the <code>PyString</code> at the
     * first occurrence of <code>sepObj</code> returning a {@link PyTuple} containing the part
     * before the separator, the separator itself, and the part after the separator.
     *
     * @param sepObj str, unicode or object implementing {@link BufferProtocol}
     * @return tuple of parts
     */
    public PyTuple partition(PyObject sepObj) {
        return str_partition(sepObj);
    }

    @ExposedMethod(doc = BuiltinDocs.str_partition_doc)
    final PyTuple str_partition(PyObject sepObj) {

        if (sepObj instanceof PyUnicode) {
            // Deal with Unicode separately
            return unicodePartition(sepObj);

        } else {
            // It ought to be some kind of bytes with the buffer API.
            String sep = asU16BytesOrError(sepObj);

            if (sep.length() == 0) {
                throw Py.ValueError("empty separator");
            }

            int index = getString().indexOf(sep);
            if (index != -1) {
                return new PyTuple(fromSubstring(0, index), sepObj,
                        fromSubstring(index + sep.length(), getString().length()));
            } else {
                return new PyTuple(this, Py.EmptyString, Py.EmptyString);
            }
        }
    }

    final PyTuple unicodePartition(PyObject sepObj) {
        PyUnicode strObj = __unicode__();
        String str = strObj.getString();

        // Will throw a TypeError if not a basestring
        String sep = sepObj.asString();
        sepObj = sepObj.__unicode__();

        if (sep.length() == 0) {
            throw Py.ValueError("empty separator");
        }

        int index = str.indexOf(sep);
        if (index != -1) {
            return new PyTuple(strObj.fromSubstring(0, index), sepObj,
                    strObj.fromSubstring(index + sep.length(), str.length()));
        } else {
            PyUnicode emptyUnicode = Py.newUnicode("");
            return new PyTuple(this, emptyUnicode, emptyUnicode);
        }
    }

    /**
     * Equivalent to Python <code>str.rpartition()</code>, splits the <code>PyString</code> at the
     * last occurrence of <code>sepObj</code> returning a {@link PyTuple} containing the part before
     * the separator, the separator itself, and the part after the separator.
     *
     * @param sepObj str, unicode or object implementing {@link BufferProtocol}
     * @return tuple of parts
     */
    public PyTuple rpartition(PyObject sepObj) {
        return str_rpartition(sepObj);
    }

    @ExposedMethod(doc = BuiltinDocs.str_rpartition_doc)
    final PyTuple str_rpartition(PyObject sepObj) {

        if (sepObj instanceof PyUnicode) {
            // Deal with Unicode separately
            return unicodeRpartition(sepObj);

        } else {
            // It ought to be some kind of bytes with the buffer API.
            String sep = asU16BytesOrError(sepObj);

            if (sep.length() == 0) {
                throw Py.ValueError("empty separator");
            }

            int index = getString().lastIndexOf(sep);
            if (index != -1) {
                return new PyTuple(fromSubstring(0, index), sepObj,
                        fromSubstring(index + sep.length(), getString().length()));
            } else {
                return new PyTuple(Py.EmptyString, Py.EmptyString, this);
            }
        }
    }

    final PyTuple unicodeRpartition(PyObject sepObj) {
        PyUnicode strObj = __unicode__();
        String str = strObj.getString();

        // Will throw a TypeError if not a basestring
        String sep = sepObj.asString();
        sepObj = sepObj.__unicode__();

        if (sep.length() == 0) {
            throw Py.ValueError("empty separator");
        }

        int index = str.lastIndexOf(sep);
        if (index != -1) {
            return new PyTuple(strObj.fromSubstring(0, index), sepObj,
                    strObj.fromSubstring(index + sep.length(), str.length()));
        } else {
            PyUnicode emptyUnicode = Py.newUnicode("");
            return new PyTuple(emptyUnicode, emptyUnicode, this);
        }
    }

    public PyList splitlines() {
        return str_splitlines(false);
    }

    public PyList splitlines(boolean keepends) {
        return str_splitlines(keepends);
    }

    @ExposedMethod(defaults = "false", doc = BuiltinDocs.str_splitlines_doc)
    final PyList str_splitlines(boolean keepends) {
        PyList list = new PyList();

        char[] chars = getString().toCharArray();
        int n = chars.length;

        int j = 0;
        for (int i = 0; i < n;) {
            /* Find a line and append it */
            while (i < n && chars[i] != '\n' && chars[i] != '\r'
                    && Character.getType(chars[i]) != Character.LINE_SEPARATOR) {
                i++;
            }

            /* Skip the line break reading CRLF as one line break */
            int eol = i;
            if (i < n) {
                if (chars[i] == '\r' && i + 1 < n && chars[i + 1] == '\n') {
                    i += 2;
                } else {
                    i++;
                }
                if (keepends) {
                    eol = i;
                }
            }
            list.append(fromSubstring(j, eol));
            j = i;
        }
        if (j < n) {
            list.append(fromSubstring(j, n));
        }
        return list;
    }

    /**
     * Return a new object <em>of the same type as this one</em> equal to the slice
     * <code>[begin:end]</code>. (Python end-relative indexes etc. are not supported.) Subclasses (
     * {@link PyUnicode#fromSubstring(int, int)}) override this to return their own type.)
     *
     * @param begin first included character.
     * @param end first excluded character.
     * @return new object.
     */
    protected PyString fromSubstring(int begin, int end) {
        // Method is overridden in PyUnicode, so definitely a PyString
        return new PyString(getString().substring(begin, end), true);
    }

    /**
     * Return the lowest index in the string where substring <code>sub</code> is found. Raises
     * <code>ValueError</code> if the substring is not found.
     *
     * @param sub substring to find.
     * @return index of <code>sub</code> in this object.
     * @throws PyException {@code ValueError} if not found.
     */
    public int index(PyObject sub) {
        return str_index(sub, null, null);
    }

    /**
     * Return the lowest index in the string where substring <code>sub</code> is found, such that
     * <code>sub</code> is contained in the slice <code>s[start:]</code>. Raises
     * <code>ValueError</code> if the substring is not found.
     *
     * @param sub substring to find.
     * @param start start of slice.
     * @return index of <code>sub</code> in this object.
     * @throws PyException {@code ValueError} if not found.
     */
    public int index(PyObject sub, PyObject start) throws PyException {
        return str_index(sub, start, null);
    }

    /**
     * Return the lowest index in the string where substring <code>sub</code> is found, such that
     * <code>sub</code> is contained in the slice <code>s[start:end]</code>. Arguments
     * <code>start</code> and <code>end</code> are interpreted as in slice notation, with null or
     * {@link Py#None} representing "missing". Raises <code>ValueError</code> if the substring is
     * not found.
     *
     * @param sub substring to find.
     * @param start start of slice.
     * @param end end of slice.
     * @return index of <code>sub</code> in this object.
     * @throws PyException {@code ValueError} if not found.
     */
    public int index(PyObject sub, PyObject start, PyObject end) throws PyException {
        return checkIndex(str_index(sub, start, end));
    }

    /** Equivalent to {@link #index(PyObject)} specialized to <code>String</code>. */
    public int index(String sub) {
        return index(sub, null, null);
    }

    /** Equivalent to {@link #index(PyObject, PyObject)} specialized to <code>String</code>. */
    public int index(String sub, PyObject start) {
        return index(sub, start, null);
    }

    /**
     * Equivalent to {@link #index(PyObject, PyObject, PyObject)} specialized to <code>String</code>
     * .
     */
    public int index(String sub, PyObject start, PyObject end) {
        return checkIndex(_find(sub, start, end));
    }

    @ExposedMethod(defaults = {"null", "null"}, doc = BuiltinDocs.str_index_doc)
    final int str_index(PyObject subObj, PyObject start, PyObject end) {
        return checkIndex(str_find(subObj, start, end));
    }

    /**
     * Return the highest index in the string where substring <code>sub</code> is found. Raises
     * <code>ValueError</code> if the substring is not found.
     *
     * @param sub substring to find.
     * @return index of <code>sub</code> in this object.
     * @throws PyException {@code ValueError} if not found.
     */
    public int rindex(PyObject sub) {
        return str_rindex(sub, null, null);
    }

    /**
     * Return the highest index in the string where substring <code>sub</code> is found, such that
     * <code>sub</code> is contained in the slice <code>s[start:]</code>. Raises
     * <code>ValueError</code> if the substring is not found.
     *
     * @param sub substring to find.
     * @param start start of slice.
     * @return index of <code>sub</code> in this object.
     * @throws PyException {@code ValueError} if not found.
     */
    public int rindex(PyObject sub, PyObject start) throws PyException {
        return str_rindex(sub, start, null);
    }

    /**
     * Return the highest index in the string where substring <code>sub</code> is found, such that
     * <code>sub</code> is contained in the slice <code>s[start:end]</code>. Arguments
     * <code>start</code> and <code>end</code> are interpreted as in slice notation, with null or
     * {@link Py#None} representing "missing". Raises <code>ValueError</code> if the substring is
     * not found.
     *
     * @param sub substring to find.
     * @param start start of slice.
     * @param end end of slice.
     * @return index of <code>sub</code> in this object.
     * @throws PyException {@code ValueError} if not found.
     */
    public int rindex(PyObject sub, PyObject start, PyObject end) throws PyException {
        return checkIndex(str_rindex(sub, start, end));
    }

    /** Equivalent to {@link #rindex(PyObject)} specialized to <code>String</code>. */
    public int rindex(String sub) {
        return rindex(sub, null, null);
    }

    /** Equivalent to {@link #rindex(PyObject, PyObject)} specialized to <code>String</code>. */
    public int rindex(String sub, PyObject start) {
        return rindex(sub, start, null);
    }

    /**
     * Equivalent to {@link #rindex(PyObject, PyObject, PyObject)} specialized to
     * <code>String</code>.
     */
    public int rindex(String sub, PyObject start, PyObject end) {
        return checkIndex(_rfind(sub, start, end));
    }

    @ExposedMethod(defaults = {"null", "null"}, doc = BuiltinDocs.str_rindex_doc)
    final int str_rindex(PyObject subObj, PyObject start, PyObject end) {
        return checkIndex(str_rfind(subObj, start, end));
    }

    /**
     * A little helper for converting str.find to str.index that will raise
     * <code>ValueError("substring not found")</code> if the argument is negative, otherwise passes
     * the argument through.
     *
     * @param index to check
     * @return <code>index</code> if non-negative
     * @throws PyException {@code ValueError} if not found
     */
    protected final int checkIndex(int index) throws PyException {
        if (index >= 0) {
            return index;
        } else {
            throw Py.ValueError("substring not found");
        }
    }

    /**
     * Return the number of non-overlapping occurrences of substring <code>sub</code>.
     *
     * @param sub substring to find.
     * @return count of occurrences.
     */
    public int count(PyObject sub) {
        return count(sub, null, null);
    }

    /**
     * Return the number of non-overlapping occurrences of substring <code>sub</code> in the range
     * <code>[start:]</code>.
     *
     * @param sub substring to find.
     * @param start start of slice.
     * @return count of occurrences.
     */
    public int count(PyObject sub, PyObject start) {
        return count(sub, start, null);
    }

    /**
     * Return the number of non-overlapping occurrences of substring <code>sub</code> in the range
     * <code>[start:end]</code>. Optional arguments <code>start</code> and <code>end</code> are
     * interpreted as in slice notation.
     *
     * @param sub substring to find.
     * @param start start of slice.
     * @param end end of slice.
     * @return count of occurrences.
     */
    public int count(PyObject sub, PyObject start, PyObject end) {
        return str_count(sub, start, end);
    }

    /** Equivalent to {@link #count(PyObject)} specialized to <code>String</code>. */
    public int count(String sub) {
        return count(sub, null, null);
    }

    /** Equivalent to {@link #count(PyObject, PyObject)} specialized to <code>String</code>. */
    public int count(String sub, PyObject start) {
        return count(sub, start, null);
    }

    /**
     * Equivalent to {@link #count(PyObject, PyObject, PyObject)} specialized to <code>String</code>
     * .
     */
    public int count(String sub, PyObject start, PyObject end) {
        return _count(sub, start, end);
    }

    @ExposedMethod(defaults = {"null", "null"}, doc = BuiltinDocs.str_count_doc)
    final int str_count(PyObject subObj, PyObject start, PyObject end) {
        if (subObj instanceof PyUnicode) {
            // Promote the problem to a Unicode one
            return asUnicode(start, end).unicode_count(subObj, null, null);
        } else {
            // It ought to be some kind of bytes with the buffer API.
            String sub = asU16BytesOrError(subObj);
            return _count(sub, start, end);
        }
    }

    /**
     * Helper common to the Python and Java API returning the number of occurrences of a substring.
     * It accepts slice-like arguments, which may be <code>None</code> or end-relative (negative).
     * This method also supports {@link PyUnicode#unicode_count(PyObject, PyObject, PyObject)}.
     *
     * @param sub substring to find.
     * @param startObj start of slice.
     * @param endObj end of slice.
     * @return count of occurrences
     */
    protected final int _count(String sub, PyObject startObj, PyObject endObj) {

        // Interpret the slice indices as concrete values
        int[] indices = translateIndices(startObj, endObj);
        int subLen = sub.length();

        if (subLen == 0) {
            // Special case counting the occurrences of an empty string.
            int start = indices[2], end = indices[3], n = __len__();
            if (end < 0 || end < start || start > n) {
                // Slice is reversed or does not overlap the string.
                return 0;
            } else {
                // Count of '' is one more than number of characters in overlap.
                return Math.min(end, n) - Math.max(start, 0) + 1;
            }

        } else {

            // Skip down this string finding occurrences of sub
            int start = indices[0], end = indices[1];
            int limit = end - subLen, count = 0;

            while (start <= limit) {
                int index = getString().indexOf(sub, start);
                if (index >= 0 && index <= limit) {
                    // Found at index.
                    count += 1;
                    // Next search begins after this instance, at:
                    start = index + subLen;
                } else {
                    // not found, or found too far right (index>limit)
                    break;
                }
            }
            return count;
        }
    }

    /**
     * Return the lowest index in the string where substring <code>sub</code> is found.
     *
     * @param sub substring to find.
     * @return index of <code>sub</code> in this object or -1 if not found.
     */
    public int find(PyObject sub) {
        return find(sub, null, null);
    }

    /**
     * Return the lowest index in the string where substring <code>sub</code> is found, such that
     * <code>sub</code> is contained in the slice <code>s[start:]</code>.
     *
     * @param sub substring to find.
     * @param start start of slice.
     * @return index of <code>sub</code> in this object or -1 if not found.
     */
    public int find(PyObject sub, PyObject start) {
        return find(sub, start, null);
    }

    /**
     * Return the lowest index in the string where substring <code>sub</code> is found, such that
     * <code>sub</code> is contained in the slice <code>s[start:end]</code>. Arguments
     * <code>start</code> and <code>end</code> are interpreted as in slice notation, with null or
     * {@link Py#None} representing "missing".
     *
     * @param sub substring to find.
     * @param start start of slice.
     * @param end end of slice.
     * @return index of <code>sub</code> in this object or -1 if not found.
     */
    public int find(PyObject sub, PyObject start, PyObject end) {
        return str_find(sub, start, end);
    }

    /** Equivalent to {@link #find(PyObject)} specialized to <code>String</code>. */
    public int find(String sub) {
        return find(sub, null, null);
    }

    /** Equivalent to {@link #find(PyObject, PyObject)} specialized to <code>String</code>. */
    public int find(String sub, PyObject start) {
        return find(sub, start, null);
    }

    /**
     * Equivalent to {@link #find(PyObject, PyObject, PyObject)} specialized to <code>String</code>.
     */
    public int find(String sub, PyObject start, PyObject end) {
        return _find(sub, start, end);
    }

    @ExposedMethod(defaults = {"null", "null"}, doc = BuiltinDocs.str_find_doc)
    final int str_find(PyObject subObj, PyObject start, PyObject end) {
        if (subObj instanceof PyUnicode) {
            // Promote the problem to a Unicode one
            // XXX Questionable: return is a Unicode character index not byte index
            return ((PyUnicode) decode()).unicode_find(subObj, start, end);
        } else {
            // It ought to be a bytes-like object.
            String sub = asU16BytesOrError(subObj);
            return _find(sub, start, end);
        }
    }

    /**
     * Helper common to the Python and Java API returning the index of the substring or -1 for not
     * found. It accepts slice-like arguments, which may be <code>None</code> or end-relative
     * (negative). This method also supports
     * {@link PyUnicode#unicode_find(PyObject, PyObject, PyObject)}.
     *
     * @param sub substring to find.
     * @param startObj start of slice.
     * @param endObj end of slice.
     * @return index of <code>sub</code> in this object or -1 if not found.
     */
    protected final int _find(String sub, PyObject startObj, PyObject endObj) {
        // Interpret the slice indices as concrete values
        int[] indices = translateIndices(startObj, endObj);
        int subLen = sub.length();

        if (subLen == 0) {
            // Special case: an empty string may be found anywhere, ...
            int start = indices[2], end = indices[3];
            if (end < 0 || end < start || start > __len__()) {
                // ... except ln a reverse slice or beyond the end of the string,
                return -1;
            } else {
                // ... and will be reported at the start of the overlap.
                return indices[0];
            }

        } else {
            // General case: search for first match then check against slice.
            int start = indices[0], end = indices[1];
            int found = getString().indexOf(sub, start);
            if (found >= 0 && found + subLen <= end) {
                return found;
            } else {
                return -1;
            }
        }
    }

    /**
     * Return the highest index in the string where substring <code>sub</code> is found.
     *
     * @param sub substring to find.
     * @return index of <code>sub</code> in this object or -1 if not found.
     */
    public int rfind(PyObject sub) {
        return rfind(sub, null, null);
    }

    /**
     * Return the highest index in the string where substring <code>sub</code> is found, such that
     * <code>sub</code> is contained in the slice <code>s[start:]</code>.
     *
     * @param sub substring to find.
     * @param start start of slice.
     * @return index of <code>sub</code> in this object or -1 if not found.
     */
    public int rfind(PyObject sub, PyObject start) {
        return rfind(sub, start, null);
    }

    /**
     * Return the highest index in the string where substring <code>sub</code> is found, such that
     * <code>sub</code> is contained in the slice <code>s[start:end]</code>. Arguments
     * <code>start</code> and <code>end</code> are interpreted as in slice notation, with null or
     * {@link Py#None} representing "missing".
     *
     * @param sub substring to find.
     * @param start start of slice.
     * @param end end of slice.
     * @return index of <code>sub</code> in this object or -1 if not found.
     */
    public int rfind(PyObject sub, PyObject start, PyObject end) {
        return str_rfind(sub, start, end);
    }

    /** Equivalent to {@link #find(PyObject)} specialized to <code>String</code>. */
    public int rfind(String sub) {
        return rfind(sub, null, null);
    }

    /** Equivalent to {@link #find(PyObject, PyObject)} specialized to <code>String</code>. */
    public int rfind(String sub, PyObject start) {
        return rfind(sub, start, null);
    }

    /**
     * Equivalent to {@link #find(PyObject, PyObject, PyObject)} specialized to <code>String</code>.
     */
    public int rfind(String sub, PyObject start, PyObject end) {
        return _rfind(sub, start, end);
    }

    @ExposedMethod(defaults = {"null", "null"}, doc = BuiltinDocs.str_rfind_doc)
    final int str_rfind(PyObject subObj, PyObject start, PyObject end) {
        if (subObj instanceof PyUnicode) {
            // Promote the problem to a Unicode one
            return ((PyUnicode) decode()).unicode_rfind(subObj, start, end);
        } else {
            // It ought to be some kind of bytes with the buffer API.
            String sub = asU16BytesOrError(subObj);
            return _rfind(sub, start, end);
        }
    }

    /**
     * Helper common to the Python and Java API returning the last index of the substring or -1 for
     * not found. It accepts slice-like arguments, which may be <code>None</code> or end-relative
     * (negative). This method also supports
     * {@link PyUnicode#unicode_rfind(PyObject, PyObject, PyObject)}.
     *
     * @param sub substring to find.
     * @param startObj start of slice.
     * @param endObj end of slice.
     * @return index of <code>sub</code> in this object or -1 if not found.
     */
    protected final int _rfind(String sub, PyObject startObj, PyObject endObj) {
        // Interpret the slice indices as concrete values
        int[] indices = translateIndices(startObj, endObj);
        int subLen = sub.length();

        if (subLen == 0) {
            // Special case: an empty string may be found anywhere, ...
            int start = indices[2], end = indices[3];
            if (end < 0 || end < start || start > __len__()) {
                // ... except ln a reverse slice or beyond the end of the string,
                return -1;
            } else {
                // ... and will be reported at the end of the overlap.
                return indices[1];
            }

        } else {
            // General case: search for first match then check against slice.
            int start = indices[0], end = indices[1];
            int found = getString().lastIndexOf(sub, end - subLen);
            if (found >= start) {
                return found;
            } else {
                return -1;
            }
        }
    }

    /**
     * Convert this PyString to a floating-point value according to Python rules.
     *
     * @return the value
     */
    public double atof() {
        double x = 0.0;
        Matcher m = getFloatPattern().matcher(getString());
        boolean valid = m.matches();

        if (valid) {
            // Might be a valid float: trimmed of white space in group 1.
            String number = m.group(1);
            try {
                char lastChar = number.charAt(number.length() - 1);
                if (Character.isLetter(lastChar)) {
                    // It's something like "nan", "-Inf" or "+nifty"
                    x = atofSpecials(m.group(1));
                } else {
                    // A numeric part was present, try to convert the whole
                    x = Double.parseDouble(m.group(1));
                }
            } catch (NumberFormatException e) {
                valid = false;
            }
        }

        // At this point, valid will have been cleared if there was a problem.
        if (valid) {
            return x;
        } else {
            String fmt = "invalid literal for float: %s";
            throw Py.ValueError(String.format(fmt, getString().trim()));
        }

    }

    /**
     * Regular expression for an unsigned Python float, accepting also any sequence of the letters
     * that belong to "NaN" or "Infinity" in whatever case. This is used within the regular
     * expression patterns that define a priori acceptable strings in the float and complex
     * constructors. The expression contributes no capture groups.
     */
    private static final String UF_RE =
            "(?:(?:(?:\\d+\\.?|\\.\\d)\\d*(?:[eE][+-]?\\d+)?)|[infatyINFATY]+)";

    /**
     * Return the (lazily) compiled regular expression that matches all valid a Python float()
     * arguments, in which Group 1 captures the number, stripped of white space. Various invalid
     * non-numerics are provisionally accepted (e.g. "+inanity" or "-faint").
     */
    private static synchronized Pattern getFloatPattern() {
        if (floatPattern == null) {
            floatPattern = Pattern.compile("\\s*([+-]?" + UF_RE + ")\\s*");
        }
        return floatPattern;
    }

    /** Access only through {@link #getFloatPattern()}. */
    private static Pattern floatPattern = null;

    /**
     * Return the (lazily) compiled regular expression for a Python complex number. This is used
     * within the regular expression patterns that define a priori acceptable strings in the complex
     * constructors. The expression contributes five named capture groups a, b, x, y and j. x and y
     * are the two floats encountered, and if j is present, one of them is the imaginary part. a and
     * b are the optional parentheses. They must either both be present or both omitted.
     */
    private static synchronized Pattern getComplexPattern() {
        if (complexPattern == null) {
            complexPattern = Pattern.compile("\\s*(?<a>\\(\\s*)?" // Parenthesis <a>
                    + "(?<x>[+-]?" + UF_RE + "?)" // <x>
                    + "(?<y>[+-]" + UF_RE + "?)?(?<j>[jJ])?" // + <y> <j>
                    + "\\s*(?<b>\\)\\s*)?"); // Parenthesis <b>
        }
        return complexPattern;
    }

    /** Access only through {@link #getComplexPattern()} */
    private static Pattern complexPattern = null;

    /**
     * Conversion for non-numeric floats, accepting signed or unsigned "inf" and "nan", in any case.
     *
     * @param s to convert
     * @return non-numeric result (if valid)
     * @throws NumberFormatException if not a valid non-numeric indicator
     */
    private static double atofSpecials(String s) throws NumberFormatException {
        switch (s.toLowerCase()) {
            case "nan":
            case "+nan":
            case "-nan":
                return Double.NaN;
            case "inf":
            case "+inf":
            case "infinity":
            case "+infinity":
                return Double.POSITIVE_INFINITY;
            case "-inf":
            case "-infinity":
                return Double.NEGATIVE_INFINITY;
            default:
                throw new NumberFormatException();
        }
    }

    /**
     * Convert this PyString to a complex value according to Python rules.
     *
     * @return the value
     */
    private PyComplex atocx() {
        double x = 0.0, y = 0.0;
        Matcher m = getComplexPattern().matcher(getString());
        boolean valid = m.matches();

        if (valid) {
            // Passes a priori, but we have some checks to make. Brackets: both or neither.
            if ((m.group("a") != null) != (m.group("b") != null)) {
                valid = false;

            } else {
                try {
                    // Pick up the two numbers [+-]? <x> [+-] <y> j?
                    String xs = m.group("x"), ys = m.group("y");

                    if (m.group("j") != null) {
                        // There is a 'j', so there is an imaginary part.
                        if (ys != null) {
                            // There were two numbers, so the second is the imaginary part.
                            y = toComplexPart(ys);
                            // And the first is the real part
                            x = toComplexPart(xs);
                        } else if (xs != null) {
                            // There was only one number (and a 'j')so it is the imaginary part.
                            y = toComplexPart(xs);
                            // x = 0.0;
                        } else {
                            // There were no numbers, just the 'j'. (Impossible return?)
                            y = 1.0;
                            // x = 0.0;
                        }

                    } else {
                        // There is no 'j' so can only be one number, the real part.
                        x = Double.parseDouble(xs);
                        if (ys != null) {
                            // Something like "123 +" or "123 + 456" but no 'j'.
                            throw new NumberFormatException();
                        }
                    }

                } catch (NumberFormatException e) {
                    valid = false;
                }
            }
        }

        // At this point, valid will have been cleared if there was a problem.
        if (valid) {
            return new PyComplex(x, y);
        } else {
            String fmt = "complex() arg is a malformed string: %s";
            throw Py.ValueError(String.format(fmt, getString().trim()));
        }

    }

    /**
     * Helper for interpreting each part (real and imaginary) of a complex number expressed as a
     * string in {@link #atocx(String)}. It deals with numbers, inf, nan and their variants, and
     * with the "implied one" in +j or 10-j.
     *
     * @param s to interpret
     * @return value of s
     * @throws NumberFormatException if the number is invalid
     */
    private static double toComplexPart(String s) throws NumberFormatException {
        if (s.length() == 0) {
            // Empty string (occurs only as 'j')
            return 1.0;
        } else {
            char lastChar = s.charAt(s.length() - 1);
            if (Character.isLetter(lastChar)) {
                // Possibly a sign, then letters that ought to be "nan" or "inf[inity]"
                return atofSpecials(s);
            } else if (lastChar == '+') {
                // Occurs only as "+j"
                return 1.0;
            } else if (lastChar == '-') {
                // Occurs only as "-j"
                return -1.0;
            } else {
                // Possibly a sign then an unsigned float
                return Double.parseDouble(s);
            }
        }
    }

    private BigInteger asciiToBigInteger(int base, boolean isLong) {
        String str = getString();

        int b = 0;
        int e = str.length();

        while (b < e && Character.isWhitespace(str.charAt(b))) {
            b++;
        }

        while (e > b && Character.isWhitespace(str.charAt(e - 1))) {
            e--;
        }

        char sign = 0;
        if (b < e) {
            sign = str.charAt(b);
            if (sign == '-' || sign == '+') {
                b++;
                while (b < e && Character.isWhitespace(str.charAt(b))) {
                    b++;
                }
            }

            if (base == 16) {
                if (str.charAt(b) == '0') {
                    if (b < e - 1 && Character.toUpperCase(str.charAt(b + 1)) == 'X') {
                        b += 2;
                    }
                }
            } else if (base == 0) {
                if (str.charAt(b) == '0') {
                    if (b < e - 1 && Character.toUpperCase(str.charAt(b + 1)) == 'X') {
                        base = 16;
                        b += 2;
                    } else if (b < e - 1 && Character.toUpperCase(str.charAt(b + 1)) == 'O') {
                        base = 8;
                        b += 2;
                    } else if (b < e - 1 && Character.toUpperCase(str.charAt(b + 1)) == 'B') {
                        base = 2;
                        b += 2;
                    } else {
                        base = 8;
                    }
                }
            } else if (base == 8) {
                if (b < e - 1 && Character.toUpperCase(str.charAt(b + 1)) == 'O') {
                    b += 2;
                }
            } else if (base == 2) {
                if (b < e - 1 && Character.toUpperCase(str.charAt(b + 1)) == 'B') {
                    b += 2;
                }
            }
        }

        if (base == 0) {
            base = 10;
        }

        // if the base >= 22, then an 'l' or 'L' is a digit!
        if (isLong && base < 22 && e > b
                && (str.charAt(e - 1) == 'L' || str.charAt(e - 1) == 'l')) {
            e--;
        }

        String s = str;
        if (b > 0 || e < str.length()) {
            s = str.substring(b, e);
        }

        BigInteger bi;
        if (sign == '-') {
            bi = new BigInteger("-" + s, base);
        } else {
            bi = new BigInteger(s, base);
        }
        return bi;
    }

    public int atoi() {
        return atoi(10);
    }

    public int atoi(int base) {
        if ((base != 0 && base < 2) || (base > 36)) {
            throw Py.ValueError("invalid base for atoi()");
        }

        try {
            BigInteger bi = asciiToBigInteger(base, false);
            if (bi.compareTo(PyInteger.MAX_INT) > 0 || bi.compareTo(PyInteger.MIN_INT) < 0) {
                throw Py.OverflowError("long int too large to convert to int");
            }
            return bi.intValue();
        } catch (NumberFormatException exc) {
            throw Py.ValueError(
                    "invalid literal for int() with base " + base + ": '" + getString() + "'");
        } catch (StringIndexOutOfBoundsException exc) {
            throw Py.ValueError(
                    "invalid literal for int() with base " + base + ": '" + getString() + "'");
        }
    }

    public PyLong atol() {
        return atol(10);
    }

    public PyLong atol(int base) {
        if ((base != 0 && base < 2) || (base > 36)) {
            throw Py.ValueError("invalid base for long literal:" + base);
        }

        try {
            BigInteger bi = asciiToBigInteger(base, true);
            return new PyLong(bi);
        } catch (NumberFormatException exc) {
            if (this instanceof PyUnicode) {
                // TODO: here's a basic issue: do we use the BigInteger constructor
                // above, or add an equivalent to CPython's PyUnicode_EncodeDecimal;
                // we should note that the current error string does not quite match
                // CPython regardless of the codec, that's going to require some more work
                throw Py.UnicodeEncodeError("decimal", "codec can't encode character", 0, 0,
                        "invalid decimal Unicode string");
            } else {
                throw Py.ValueError(
                        "invalid literal for long() with base " + base + ": '" + getString() + "'");
            }
        } catch (StringIndexOutOfBoundsException exc) {
            throw Py.ValueError(
                    "invalid literal for long() with base " + base + ": '" + getString() + "'");
        }
    }

    private static String padding(int n, char pad) {
        char[] chars = new char[n];
        for (int i = 0; i < n; i++) {
            chars[i] = pad;
        }
        return new String(chars);
    }

    private static char parse_fillchar(String function, String fillchar) {
        if (fillchar == null) {
            return ' ';
        }
        if (fillchar.length() != 1) {
            throw Py.TypeError(function + "() argument 2 must be char, not str");
        }
        return fillchar.charAt(0);
    }

    public String ljust(int width) {
        return str_ljust(width, null);
    }

    public String ljust(int width, String padding) {
        return str_ljust(width, padding);
    }

    @ExposedMethod(defaults = "null", doc = BuiltinDocs.str_ljust_doc)
    final String str_ljust(int width, String fillchar) {
        char pad = parse_fillchar("ljust", fillchar);
        int n = width - getString().length();
        if (n <= 0) {
            return getString();
        }
        return getString() + padding(n, pad);
    }

    public String rjust(int width) {
        return str_rjust(width, null);
    }

    @ExposedMethod(defaults = "null", doc = BuiltinDocs.str_rjust_doc)
    final String str_rjust(int width, String fillchar) {
        char pad = parse_fillchar("rjust", fillchar);
        int n = width - getString().length();
        if (n <= 0) {
            return getString();
        }
        return padding(n, pad) + getString();
    }

    public String center(int width) {
        return str_center(width, null);
    }

    @ExposedMethod(defaults = "null", doc = BuiltinDocs.str_center_doc)
    final String str_center(int width, String fillchar) {
        char pad = parse_fillchar("center", fillchar);
        int n = width - getString().length();
        if (n <= 0) {
            return getString();
        }
        int half = n / 2;
        if (n % 2 > 0 && width % 2 > 0) {
            half += 1;
        }

        return padding(half, pad) + getString() + padding(n - half, pad);
    }

    public String zfill(int width) {
        return str_zfill(width);
    }

    @ExposedMethod(doc = BuiltinDocs.str_zfill_doc)
    final String str_zfill(int width) {
        String s = getString();
        int n = s.length();
        if (n >= width) {
            return s;
        }
        char[] chars = new char[width];
        int nzeros = width - n;
        int i = 0;
        int sStart = 0;
        if (n > 0) {
            char start = s.charAt(0);
            if (start == '+' || start == '-') {
                chars[0] = start;
                i += 1;
                nzeros++;
                sStart = 1;
            }
        }
        for (; i < nzeros; i++) {
            chars[i] = '0';
        }
        s.getChars(sStart, s.length(), chars, i);
        return new String(chars);
    }

    public String expandtabs() {
        return str_expandtabs(8);
    }

    public String expandtabs(int tabsize) {
        return str_expandtabs(tabsize);
    }

    @ExposedMethod(defaults = "8", doc = BuiltinDocs.str_expandtabs_doc)
    final String str_expandtabs(int tabsize) {
        String s = getString();
        StringBuilder buf = new StringBuilder((int) (s.length() * 1.5));
        char[] chars = s.toCharArray();
        int n = chars.length;
        int position = 0;

        for (int i = 0; i < n; i++) {
            char c = chars[i];
            if (c == '\t') {
                int spaces = tabsize - position % tabsize;
                position += spaces;
                while (spaces-- > 0) {
                    buf.append(' ');
                }
                continue;
            }
            if (c == '\n' || c == '\r') {
                position = -1;
            }
            buf.append(c);
            position++;
        }
        return buf.toString();
    }

    public String capitalize() {
        return str_capitalize();
    }

    @ExposedMethod(doc = BuiltinDocs.str_capitalize_doc)
    final String str_capitalize() {
        String s = getString();
        int n = s.length();
        if (n == 0) {
            return s;
        } else {
            char[] buf = new char[n];
            // At least one byte: if lower convert to upper case.
            char c = s.charAt(0);
            buf[0] = _islower(c) ? (char) (c ^ SWAP_CASE) : c;
            // Copy the rest, converting to lower case.
            for (int i = 1; i < n; i++) {
                c = s.charAt(i);
                buf[i] = _isupper(c) ? (char) (c ^ SWAP_CASE) : c;
            }
            return new String(buf);
        }
    }

    /**
     * Equivalent to Python str.replace(old, new), returning a copy of the string with all
     * occurrences of substring old replaced by new. If either argument is a {@link PyUnicode} (or
     * this object is), the result will be a <code>PyUnicode</code>.
     *
     * @param oldPiece to replace where found.
     * @param newPiece replacement text.
     * @return PyString (or PyUnicode if any string is one), this string after replacements.
     */
    public PyString replace(PyObject oldPiece, PyObject newPiece) {
        return str_replace(oldPiece, newPiece, -1);
    }

    /**
     * Equivalent to Python str.replace(old, new[, count]), returning a copy of the string with all
     * occurrences of substring old replaced by new. If argument <code>count</code> is nonnegative,
     * only the first <code>count</code> occurrences are replaced. If either argument is a
     * {@link PyUnicode} (or this object is), the result will be a <code>PyUnicode</code>.
     *
     * @param oldPiece to replace where found.
     * @param newPiece replacement text.
     * @param count maximum number of replacements to make, or -1 meaning all of them.
     * @return PyString (or PyUnicode if any string is one), this string after replacements.
     */
    public PyString replace(PyObject oldPiece, PyObject newPiece, int count) {
        return str_replace(oldPiece, newPiece, count);
    }

    @ExposedMethod(defaults = "-1", doc = BuiltinDocs.str_replace_doc)
    final PyString str_replace(PyObject oldPieceObj, PyObject newPieceObj, int count) {
        if (oldPieceObj instanceof PyUnicode || newPieceObj instanceof PyUnicode) {
            // Promote the problem to a Unicode one
            return ((PyUnicode) decode()).unicode_replace(oldPieceObj, newPieceObj, count);
        } else {
            // Neither is a PyUnicode: both ought to be some kind of bytes with the buffer API.
            String oldPiece = asU16BytesOrError(oldPieceObj);
            String newPiece = asU16BytesOrError(newPieceObj);
            return _replace(oldPiece, newPiece, count);
        }
    }

    /**
     * Helper common to the Python and Java API for <code>str.replace</code>, returning a new string
     * equal to this string with ocurrences of <code>oldPiece</code> replaced by
     * <code>newPiece</code>, up to a maximum of <code>count</code> occurrences, or all of them.
     * This method also supports {@link PyUnicode#unicode_replace(PyObject, PyObject, int)}, in
     * which context it returns a <code>PyUnicode</code>
     *
     * @param oldPiece to replace where found.
     * @param newPiece replacement text.
     * @param count maximum number of replacements to make, or -1 meaning all of them.
     * @return PyString (or PyUnicode if this string is one), this string after replacements.
     */
    protected final PyString _replace(String oldPiece, String newPiece, int count) {

        String s = getString();
        int len = s.length();
        int oldLen = oldPiece.length();
        int newLen = newPiece.length();

        if (len == 0) {
            if (count < 0 && oldLen == 0) {
                return createInstance(newPiece, true);
            }
            return createInstance(s, true);

        } else if (oldLen == 0 && newLen != 0 && count != 0) {
            /*
             * old="" and new != "", interleave new piece with each char in original, taking into
             * account count
             */
            StringBuilder buffer = new StringBuilder();
            int i = 0;
            buffer.append(newPiece);
            for (; i < len && (count < 0 || i < count - 1); i++) {
                buffer.append(s.charAt(i)).append(newPiece);
            }
            buffer.append(s.substring(i));
            return createInstance(buffer.toString(), true);

        } else {
            if (count < 0) {
                count = (oldLen == 0) ? len + 1 : len;
            }
            return createInstance(newPiece).join(splitfields(oldPiece, count));
        }
    }

    public PyString join(PyObject seq) {
        return str_join(seq);
    }

    @ExposedMethod(doc = BuiltinDocs.str_join_doc)
    final PyString str_join(PyObject obj) {
        PySequence seq = fastSequence(obj, "");
        int seqLen = seq.__len__();
        if (seqLen == 0) {
            return Py.EmptyString;
        }

        PyObject item;
        if (seqLen == 1) {
            item = seq.pyget(0);
            if (item.getType() == PyString.TYPE || item.getType() == PyUnicode.TYPE) {
                return (PyString) item;
            }
        }

        // There are at least two things to join, or else we have a subclass of the
        // builtin types in the sequence. Do a pre-pass to figure out the total amount of
        // space we'll need, see whether any argument is absurd, and defer to the Unicode
        // join if appropriate
        int i = 0;
        long size = 0;
        int sepLen = getString().length();
        for (; i < seqLen; i++) {
            item = seq.pyget(i);
            if (!(item instanceof PyString)) {
                throw Py.TypeError(String.format("sequence item %d: expected string, %.80s found",
                        i, item.getType().fastGetName()));
            }
            if (item instanceof PyUnicode) {
                // Defer to Unicode join. CAUTION: There's no gurantee that the original
                // sequence can be iterated over again, so we must pass seq here
                return unicodeJoin(seq);
            }

            if (i != 0) {
                size += sepLen;
            }
            size += ((PyString) item).getString().length();
            if (size > Integer.MAX_VALUE) {
                throw Py.OverflowError("join() result is too long for a Python string");
            }
        }

        // Catenate everything
        StringBuilder buf = new StringBuilder((int) size);
        for (i = 0; i < seqLen; i++) {
            item = seq.pyget(i);
            if (i != 0) {
                buf.append(getString());
            }
            buf.append(((PyString) item).getString());
        }
        return new PyString(buf.toString(), true); // Guaranteed to be byte-like
    }

    final PyUnicode unicodeJoin(PyObject obj) {
        PySequence seq = fastSequence(obj, "");
        // A codec may be invoked to convert str objects to Unicode, and so it's possible
        // to call back into Python code during PyUnicode_FromObject(), and so it's
        // possible for a sick codec to change the size of fseq (if seq is a list).
        // Therefore we have to keep refetching the size -- can't assume seqlen is
        // invariant.
        int seqLen = seq.__len__();
        // If empty sequence, return u""
        if (seqLen == 0) {
            return new PyUnicode();
        }

        // If singleton sequence with an exact Unicode, return that
        PyObject item;
        if (seqLen == 1) {
            item = seq.pyget(0);
            if (item.getType() == PyUnicode.TYPE) {
                return (PyUnicode) item;
            }
        }

        String sep = null;
        if (seqLen > 1) {
            if (this instanceof PyUnicode) {
                sep = getString();
            } else {
                sep = ((PyUnicode) decode()).getString();
                // In case decode()'s codec mutated seq
                seqLen = seq.__len__();
            }
        }

        // At least two items to join, or one that isn't exact Unicode
        long size = 0;
        int sepLen = getString().length();
        StringBuilder buf = new StringBuilder();
        String itemString;
        for (int i = 0; i < seqLen; i++) {
            item = seq.pyget(i);
            // Convert item to Unicode
            if (!(item instanceof PyString)) {
                throw Py.TypeError(String.format(
                        "sequence item %d: expected string or Unicode," + " %.80s found", i,
                        item.getType().fastGetName()));
            }
            if (!(item instanceof PyUnicode)) {
                item = ((PyString) item).decode();
                // In case decode()'s codec mutated seq
                seqLen = seq.__len__();
            }
            itemString = ((PyUnicode) item).getString();

            if (i != 0) {
                size += sepLen;
                buf.append(sep);
            }
            size += itemString.length();
            if (size > Integer.MAX_VALUE) {
                throw Py.OverflowError("join() result is too long for a Python string");
            }
            buf.append(itemString);
        }
        return new PyUnicode(buf.toString());
    }

    /**
     * Equivalent to the Python <code>str.startswith</code> method testing whether a string starts
     * with a specified prefix. <code>prefix</code> can also be a tuple of prefixes to look for.
     *
     * @param prefix string to check for (or a <code>PyTuple</code> of them).
     * @return <code>true</code> if this string slice starts with a specified prefix, otherwise
     *         <code>false</code>.
     */
    public boolean startswith(PyObject prefix) {
        return startswith(prefix, null, null);
    }

    /**
     * Equivalent to the Python <code>str.startswith</code> method, testing whether a string starts
     * with a specified prefix, where a sub-range is specified by <code>[start:]</code>.
     * <code>start</code> is interpreted as in slice notation, with null or {@link Py#None}
     * representing "missing". <code>prefix</code> can also be a tuple of prefixes to look for.
     *
     * @param prefix string to check for (or a <code>PyTuple</code> of them).
     * @param start start of slice.
     * @return <code>true</code> if this string slice starts with a specified prefix, otherwise
     *         <code>false</code>.
     */
    public boolean startswith(PyObject prefix, PyObject start) {
        return startswith(prefix, start, null);
    }

    /**
     * Equivalent to the Python <code>str.startswith</code> method, testing whether a string starts
     * with a specified prefix, where a sub-range is specified by <code>[start:end]</code>.
     * Arguments <code>start</code> and <code>end</code> are interpreted as in slice notation, with
     * null or {@link Py#None} representing "missing". <code>prefix</code> can also be a tuple of
     * prefixes to look for.
     *
     * @param prefix string to check for (or a <code>PyTuple</code> of them).
     * @param start start of slice.
     * @param end end of slice.
     * @return <code>true</code> if this string slice starts with a specified prefix, otherwise
     *         <code>false</code>.
     */
    public boolean startswith(PyObject prefix, PyObject start, PyObject end) {
        return str_startswith(prefix, start, end);
    }

    @ExposedMethod(defaults = {"null", "null"}, doc = BuiltinDocs.str_startswith_doc)
    final boolean str_startswith(PyObject prefix, PyObject startObj, PyObject endObj) {

        int[] indices = translateIndices(startObj, endObj);
        int start = indices[0];
        int sliceLen = indices[1] - start;

        if (!(prefix instanceof PyTuple)) {
            if (prefix instanceof PyUnicode) {
                // Promote to a unicode problem on the decoded slice
                return asUnicode(startObj, endObj).unicode_startswith(prefix, null, null);
            } else {
                // It ought to be a bytes-like object.
                String s = asU16BytesOrError(prefix);
                return sliceLen >= s.length() && getString().startsWith(s, start);
            }

        } else {
            // It's a tuple so we have to iterate through the members.
            PyObject[] prefixes = ((PyTuple) prefix).getArray();
            String string = getString();

            // Test with only the bytes prefixes first and save the unicode ones
            int unicodeCount = 0;
            for (PyObject o : prefixes) {
                if (o instanceof PyUnicode) {
                    // Pack the unicode prefixes to the start of the array without trying them
                    prefixes[unicodeCount++] = o;
                } else {
                    // It ought to be a bytes-like object.
                    String s = asU16BytesOrError(o);
                    if (sliceLen >= s.length() && string.startsWith(s, start)) {
                        return true;
                    }
                }
            }

            if (unicodeCount == 0) {
                // Only bytes prefixes given and nothing matched
                return false;
            } else {
                // There were unicode prefixes: test the decoded slice for them.
                PyTuple t = new PyTuple(Arrays.copyOf(prefixes, unicodeCount));
                return asUnicode(startObj, endObj).unicode_startswith(t, null, null);
            }
        }
    }

    /**
     * Equivalent to the Python <code>str.endswith</code> method, testing whether a string ends with
     * a specified suffix. <code>suffix</code> can also be a tuple of suffixes to look for.
     *
     * @param suffix string to check for (or a <code>PyTuple</code> of them).
     * @return <code>true</code> if this string slice ends with a specified suffix, otherwise
     *         <code>false</code>.
     */
    public boolean endswith(PyObject suffix) {
        return endswith(suffix, null, null);
    }

    /**
     * Equivalent to the Python <code>str.endswith</code> method, testing whether a string ends with
     * a specified suffix, where a sub-range is specified by <code>[start:]</code>.
     * <code>start</code> is interpreted as in slice notation, with null or {@link Py#None}
     * representing "missing". <code>suffix</code> can also be a tuple of suffixes to look for.
     *
     * @param suffix string to check for (or a <code>PyTuple</code> of them).
     * @param start start of slice.
     * @return <code>true</code> if this string slice ends with a specified suffix, otherwise
     *         <code>false</code>.
     */
    public boolean endswith(PyObject suffix, PyObject start) {
        return endswith(suffix, start, null);
    }

    /**
     * Equivalent to the Python <code>str.endswith</code> method, testing whether a string ends with
     * a specified suffix, where a sub-range is specified by <code>[start:end]</code>. Arguments
     * <code>start</code> and <code>end</code> are interpreted as in slice notation, with null or
     * {@link Py#None} representing "missing". <code>suffix</code> can also be a tuple of suffixes
     * to look for.
     *
     * @param suffix string to check for (or a <code>PyTuple</code> of them).
     * @param start start of slice.
     * @param end end of slice.
     * @return <code>true</code> if this string slice ends with a specified suffix, otherwise
     *         <code>false</code>.
     */
    public boolean endswith(PyObject suffix, PyObject start, PyObject end) {
        return str_endswith(suffix, start, end);
    }

    @ExposedMethod(defaults = {"null", "null"}, doc = BuiltinDocs.str_endswith_doc)
    final boolean str_endswith(PyObject suffix, PyObject startObj, PyObject endObj) {

        int[] indices = translateIndices(startObj, endObj);

        if (!(suffix instanceof PyTuple)) {
            if (suffix instanceof PyUnicode) {
                // Promote to a unicode problem on the decoded slice
                return asUnicode(startObj, endObj).unicode_endswith(suffix, null, null);
            } else {
                // It ought to be a bytes-like object.
                String s = asU16BytesOrError(suffix);
                return getString().substring(indices[0], indices[1]).endsWith(s);
            }

        } else {
            // It's a tuple so we have to iterate through the members.
            PyObject[] suffixes = ((PyTuple) suffix).getArray();
            String string = getString().substring(indices[0], indices[1]);

            // Test with only the bytes suffixes first and save the unicode ones
            int unicodeCount = 0;
            for (PyObject o : suffixes) {
                if (o instanceof PyUnicode) {
                    // Pack the unicode suffixes to the start of the array without trying them
                    suffixes[unicodeCount++] = o;
                } else {
                    // It ought to be a bytes-like object.
                    String s = asU16BytesOrError(o);
                    if (string.endsWith(s)) {
                        return true;
                    }
                }
            }

            if (unicodeCount == 0) {
                // Only bytes suffixes given and nothing matched
                return false;
            } else {
                // There were unicode suffixes: test the decoded slice for them.
                PyTuple t = new PyTuple(Arrays.copyOf(suffixes, unicodeCount));
                return asUnicode(startObj, endObj).unicode_endswith(t, null, null);
            }
        }
    }

    /**
     * Many of the string methods deal with slices specified using Python slice semantics:
     * endpoints, which are <code>PyObject</code>s, may be <code>null</code> or <code>None</code>
     * (meaning default to one end or the other) or may be negative (meaning "from the end").
     * Meanwhile, the implementation methods need integer indices, both within the array, and
     * <code>0&lt;=start&lt;=end&lt;=N</code> the length of the array.
     * <p>
     * This method first translates the Python slice <code>startObj</code> and <code>endObj</code>
     * according to the slice semantics for null and negative values, and stores these in elements 2
     * and 3 of the result. Then, since the end points of the range may lie outside this sequence's
     * bounds (in either direction) it reduces them to the nearest points satisfying
     * <code>0&lt;=start&lt;=end&lt;=N</code>, and stores these in elements [0] and [1] of the
     * result.
     *
     * @param startObj Python start of slice
     * @param endObj Python end of slice
     * @return a 4 element array of two range-safe indices, and two original indices.
     */
    protected int[] translateIndices(PyObject startObj, PyObject endObj) {
        int start, end;
        int n = __len__();
        int[] result = new int[4];

        // Decode the start using slice semantics
        if (startObj == null || startObj == Py.None) {
            start = 0;
            // result[2] = 0 already
        } else {
            // Convert to int but limit to Integer.MIN_VALUE <= start <= Integer.MAX_VALUE
            start = startObj.asIndex(null);
            if (start < 0) {
                // Negative value means "from the end"
                start = n + start;
            }
            result[2] = start;
        }

        // Decode the end using slice semantics
        if (endObj == null || endObj == Py.None) {
            result[1] = result[3] = end = n;
        } else {
            // Convert to int but limit to Integer.MIN_VALUE <= end <= Integer.MAX_VALUE
            end = endObj.asIndex(null);
            if (end < 0) {
                // Negative value means "from the end"
                result[3] = end = end + n;
                // Ensure end is safe for String.substring(start,end).
                if (end < 0) {
                    end = 0;
                    // result[1] = 0 already
                } else {
                    result[1] = end;
                }
            } else {
                result[3] = end;
                // Ensure end is safe for String.substring(start,end).
                if (end > n) {
                    result[1] = end = n;
                } else {
                    result[1] = end;
                }
            }
        }

        // Ensure start is safe for String.substring(start,end).
        if (start < 0) {
            start = 0;
            // result[0] = 0 already
        } else if (start > end) {
            result[0] = start = end;
        } else {
            result[0] = start;
        }

        return result;
    }

    /**
     * Equivalent to Python <code>str.translate</code> returning a copy of this string where the
     * characters have been mapped through the translation <code>table</code>. <code>table</code>
     * must be equivalent to a string of length 256 (if it is not <code>null</code>).
     *
     * @param table of character (byte) translations (or <code>null</code>)
     * @return transformed byte string
     */
    public String translate(PyObject table) {
        return translate(table, null);
    }

    /**
     * Equivalent to Python <code>str.translate</code> returning a copy of this string where all
     * characters (bytes) occurring in the argument <code>deletechars</code> are removed (if it is
     * not <code>null</code>), and the remaining characters have been mapped through the translation
     * <code>table</code>. <code>table</code> must be equivalent to a string of length 256 (if it is
     * not <code>null</code>).
     *
     * @param table of character (byte) translations (or <code>null</code>)
     * @param deletechars set of characters to remove (or <code>null</code>)
     * @return transformed byte string
     */
    public String translate(PyObject table, PyObject deletechars) {
        return str_translate(table, deletechars);
    }

    /**
     * Equivalent to {@link #translate(PyObject)} specialized to <code>String</code>.
     */
    public String translate(String table) {
        return _translate(table, null);
    }

    /**
     * Equivalent to {@link #translate(PyObject, PyObject)} specialized to <code>String</code>.
     */
    public String translate(String table, String deletechars) {
        return _translate(table, deletechars);
    }

    @ExposedMethod(defaults = {"null", "null"}, doc = BuiltinDocs.str_translate_doc)
    final String str_translate(PyObject tableObj, PyObject deletecharsObj) {
        // Accept anythiong withthe buffer API or null
        String table = asU16BytesNullOrError(tableObj, null);
        String deletechars = asU16BytesNullOrError(deletecharsObj, null);
        return _translate(table, deletechars);
    }

    /**
     * Helper common to the Python and Java API implementing <code>str.translate</code> returning a
     * copy of this string where all characters (bytes) occurring in the argument
     * <code>deletechars</code> are removed (if it is not <code>null</code>), and the remaining
     * characters have been mapped through the translation <code>table</code>, which must be
     * equivalent to a string of length 256 (if it is not <code>null</code>).
     *
     * @param table of character (byte) translations (or <code>null</code>)
     * @param deletechars set of characters to remove (or <code>null</code>)
     * @return transformed byte string
     */
    private final String _translate(String table, String deletechars) {

        if (table != null && table.length() != 256) {
            throw Py.ValueError("translation table must be 256 characters long");
        }

        StringBuilder buf = new StringBuilder(getString().length());

        for (int i = 0; i < getString().length(); i++) {
            char c = getString().charAt(i);
            if (deletechars != null && deletechars.indexOf(c) >= 0) {
                continue;
            }
            if (table == null) {
                buf.append(c);
            } else {
                try {
                    buf.append(table.charAt(c));
                } catch (IndexOutOfBoundsException e) {
                    throw Py.TypeError("translate() only works for 8-bit character strings");
                }
            }
        }
        return buf.toString();
    }

    public boolean islower() {
        return str_islower();
    }

    @ExposedMethod(doc = BuiltinDocs.str_islower_doc)
    final boolean str_islower() {
        String s = getString();
        int n = s.length();

        if (n == 1) {
            // Special case single character strings.
            return _islower(s.charAt(0));
        }

        boolean cased = false;
        for (int i = 0; i < n; i++) {
            char ch = s.charAt(i);
            if (_isupper(ch)) {
                return false;
            } else if (!cased && _islower(ch)) {
                cased = true;
            }
        }
        return cased;
    }

    private boolean _islower(char ch) {
        if (ch < 256) {
            return BaseBytes.islower((byte) ch);
        } else {
            // This is an internal error. Really, the test should be unnecessary.
            throw new java.lang.IllegalArgumentException("non-byte character in PyString");
        }
    }

    public boolean isupper() {
        return str_isupper();
    }

    @ExposedMethod(doc = BuiltinDocs.str_isupper_doc)
    final boolean str_isupper() {
        String s = getString();
        int n = s.length();

        if (n == 1) {
            // Special case single character strings.
            return _isupper(s.charAt(0));
        }

        boolean cased = false;
        for (int i = 0; i < n; i++) {
            char ch = s.charAt(i);
            if (_islower(ch)) {
                return false;
            } else if (!cased && _isupper(ch)) {
                cased = true;
            }
        }
        return cased;
    }

    private boolean _isupper(char ch) {
        if (ch < 256) {
            return BaseBytes.isupper((byte) ch);
        } else {
            // This is an internal error. Really, the test should be unnecessary.
            throw new java.lang.IllegalArgumentException("non-byte character in PyString");
        }
    }

    public boolean isalpha() {
        return str_isalpha();
    }

    @ExposedMethod(doc = BuiltinDocs.str_isalpha_doc)
    final boolean str_isalpha() {
        String s = getString();
        int n = s.length();

        if (n == 1) {
            // Special case single character strings.
            return _isalpha(s.charAt(0));
        }

        for (int i = 0; i < n; i++) {
            if (!_isalpha(s.charAt(i))) {
                return false;
            }
        }
        return n > 0;
    }

    private boolean _isalpha(char ch) {
        if (ch < 256) {
            return BaseBytes.isalpha((byte) ch);
        } else {
            // This is an internal error. Really, the test should be unnecessary.
            throw new java.lang.IllegalArgumentException("non-byte character in PyString");
        }
    }

    public boolean isalnum() {
        return str_isalnum();
    }

    @ExposedMethod(doc = BuiltinDocs.str_isalnum_doc)
    final boolean str_isalnum() {
        String s = getString();
        int n = s.length();

        if (n == 1) {
            // Special case single character strings.
            return _isalnum(s.charAt(0));
        }

        for (int i = 0; i < n; i++) {
            if (!_isalnum(s.charAt(i))) {
                return false;
            }
        }
        return n > 0;
    }

    private boolean _isalnum(char ch) {
        // This is now entirely compatible with CPython, as long as only bytes are stored.
        if (ch < 256) {
            return BaseBytes.isalnum((byte) ch);
        } else {
            // This is an internal error. Really, the test should be unnecessary.
            throw new java.lang.IllegalArgumentException("non-byte character in PyString");
        }
    }

    public boolean isdecimal() {
        return str_isdecimal();
    }

    @ExposedMethod(doc = BuiltinDocs.unicode_isdecimal_doc)
    final boolean str_isdecimal() { // XXX this ought not to exist in str (in Python 2)
        return str_isdigit();
    }

    private boolean _isdecimal(char ch) {
        // See the comment in _isalnum. Here it is even worse.
        return Character.getType(ch) == Character.DECIMAL_DIGIT_NUMBER;
    }

    public boolean isdigit() {
        return str_isdigit();
    }

    @ExposedMethod(doc = BuiltinDocs.str_isdigit_doc)
    final boolean str_isdigit() {
        String s = getString();
        int n = s.length();

        if (n == 1) {
            // Special case single character strings.
            return _isdigit(s.charAt(0));
        }

        for (int i = 0; i < n; i++) {
            if (!_isdigit(s.charAt(i))) {
                return false;
            }
        }
        return n > 0;
    }

    private boolean _isdigit(char ch) {
        if (ch < 256) {
            return BaseBytes.isdigit((byte) ch);
        } else {
            // This is an internal error. Really, the test should be unnecessary.
            throw new java.lang.IllegalArgumentException("non-byte character in PyString");
        }
    }

    public boolean isnumeric() {
        return str_isnumeric();
    }

    @ExposedMethod(doc = BuiltinDocs.unicode_isnumeric_doc)
    final boolean str_isnumeric() { // XXX this ought not to exist in str (in Python 2)
        return str_isdigit();
    }

    public boolean istitle() {
        return str_istitle();
    }

    @ExposedMethod(doc = BuiltinDocs.str_istitle_doc)
    final boolean str_istitle() {
        String s = getString();
        int n = s.length();

        if (n == 1) {
            // Special case single character strings.
            return _isupper(s.charAt(0));
        }

        boolean cased = false;
        boolean previous_is_cased = false;
        for (int i = 0; i < n; i++) {
            char ch = s.charAt(i);
            if (_isupper(ch)) {
                if (previous_is_cased) {
                    return false;
                }
                previous_is_cased = true;
                cased = true;
            } else if (_islower(ch)) {
                if (!previous_is_cased) {
                    return false;
                }
                previous_is_cased = true;
                cased = true;
            } else {
                previous_is_cased = false;
            }
        }
        return cased;
    }

    public boolean isspace() {
        return str_isspace();
    }

    @ExposedMethod(doc = BuiltinDocs.str_isspace_doc)
    final boolean str_isspace() {
        String s = getString();
        int n = s.length();

        if (n == 1) {
            // Special case single character strings.
            return _isspace(s.charAt(0));
        }

        for (int i = 0; i < n; i++) {
            if (!_isspace(s.charAt(i))) {
                return false;
            }
        }
        return n > 0;
    }

    private boolean _isspace(char ch) {
        if (ch < 256) {
            return BaseBytes.isspace((byte) ch);
        } else {
            // This is an internal error. Really, the test should be unnecessary.
            throw new java.lang.IllegalArgumentException("non-byte character in PyString");
        }
    }

    public boolean isunicode() {
        return str_isunicode();
    }

    @ExposedMethod(doc = "isunicode is deprecated.")
    final boolean str_isunicode() {
        Py.warning(Py.DeprecationWarning, "isunicode is deprecated.");
        int n = getString().length();
        for (int i = 0; i < n; i++) {
            char ch = getString().charAt(i);
            if (ch > 255) {
                return true;
            }
        }
        return false;
    }

    public String encode() {
        return encode(null, null);
    }

    public String encode(String encoding) {
        return encode(encoding, null);
    }

    public String encode(String encoding, String errors) {
        return codecs.encode(this, encoding, errors);
    }

    @ExposedMethod(doc = BuiltinDocs.str_encode_doc)
    final String str_encode(PyObject[] args, String[] keywords) {
        ArgParser ap = new ArgParser("encode", args, keywords, "encoding", "errors");
        String encoding = ap.getString(0, null);
        String errors = ap.getString(1, null);
        return encode(encoding, errors);
    }

    public PyObject decode() {
        return decode(null, null);
    }

    public PyObject decode(String encoding) {
        return decode(encoding, null);
    }

    public PyObject decode(String encoding, String errors) {
        return codecs.decode(this, encoding, errors);
    }

    @ExposedMethod(doc = BuiltinDocs.str_decode_doc)
    final PyObject str_decode(PyObject[] args, String[] keywords) {
        ArgParser ap = new ArgParser("decode", args, keywords, "encoding", "errors");
        String encoding = ap.getString(0, null);
        String errors = ap.getString(1, null);
        return decode(encoding, errors);
    }

    @ExposedMethod(doc = BuiltinDocs.str__formatter_parser_doc)
    final PyObject str__formatter_parser() {
        return new MarkupIterator(this);
    }

    @ExposedMethod(doc = BuiltinDocs.str__formatter_field_name_split_doc)
    final PyObject str__formatter_field_name_split() {
        FieldNameIterator iterator = new FieldNameIterator(this);
        return new PyTuple(iterator.pyHead(), iterator);
    }

    @ExposedMethod(doc = BuiltinDocs.str_format_doc)
    final PyObject str_format(PyObject[] args, String[] keywords) {
        try {
            return new PyString(buildFormattedString(args, keywords, null, null));
        } catch (IllegalArgumentException e) {
            throw Py.ValueError(e.getMessage());
        }
    }

    /**
     * Implements PEP-3101 {}-formatting methods <code>str.format()</code> and
     * <code>unicode.format()</code>. When called with <code>enclosingIterator == null</code>, this
     * method takes this object as its formatting string. The method is also called (calls itself)
     * to deal with nested formatting specifications. In that case, <code>enclosingIterator</code>
     * is a {@link MarkupIterator} on this object and <code>value</code> is a substring of this
     * object needing recursive translation.
     *
     * @param args to be interpolated into the string
     * @param keywords for the trailing args
     * @param enclosingIterator when used nested, null if subject is this <code>PyString</code>
     * @param value the format string when <code>enclosingIterator</code> is not null
     * @return the formatted string based on the arguments
     */
    protected String buildFormattedString(PyObject[] args, String[] keywords,
            MarkupIterator enclosingIterator, String value) {

        MarkupIterator it;
        if (enclosingIterator == null) {
            // Top-level call acts on this object.
            it = new MarkupIterator(this);
        } else {
            // Nested call acts on the substring and some state from existing iterator.
            it = new MarkupIterator(enclosingIterator, value);
        }

        // Result will be formed here
        StringBuilder result = new StringBuilder();

        while (true) {
            MarkupIterator.Chunk chunk = it.nextChunk();
            if (chunk == null) {
                break;
            }
            // A Chunk encapsulates a literal part ...
            result.append(chunk.literalText);
            // ... and the parsed form of the replacement field that followed it (if any)
            if (chunk.fieldName != null) {
                // The grammar of the replacement field is:
                // "{" [field_name] ["!" conversion] [":" format_spec] "}"

                // Get the object referred to by the field name (which may be omitted).
                PyObject fieldObj = getFieldObject(chunk.fieldName, it.isBytes(), args, keywords);
                if (fieldObj == null) {
                    continue;
                }

                // The conversion specifier is s = __str__ or r = __repr__.
                if ("r".equals(chunk.conversion)) {
                    fieldObj = fieldObj.__repr__();
                } else if ("s".equals(chunk.conversion)) {
                    fieldObj = fieldObj.__str__();
                } else if (chunk.conversion != null) {
                    throw Py.ValueError("Unknown conversion specifier " + chunk.conversion);
                }

                // Check for "{}".format(u"abc")
                if (fieldObj instanceof PyUnicode && !(this instanceof PyUnicode)) {
                    // Down-convert to PyString, at the risk of raising UnicodeEncodingError
                    fieldObj = ((PyUnicode) fieldObj).__str__();
                }

                // The format_spec may be simple, or contained nested replacement fields.
                String formatSpec = chunk.formatSpec;
                if (chunk.formatSpecNeedsExpanding) {
                    if (enclosingIterator != null) {
                        // PEP 3101 says only 2 levels
                        throw Py.ValueError("Max string recursion exceeded");
                    }
                    // Recursively interpolate further args into chunk.formatSpec
                    formatSpec = buildFormattedString(args, keywords, it, formatSpec);
                }
                renderField(fieldObj, formatSpec, result);
            }
        }
        return result.toString();
    }

    /**
     * Return the object referenced by a given field name, interpreted in the context of the given
     * argument list, containing positional and keyword arguments.
     *
     * @param fieldName to interpret.
     * @param bytes true if the field name is from a PyString, false for PyUnicode.
     * @param args argument list (positional then keyword arguments).
     * @param keywords naming the keyword arguments.
     * @return the object designated or <code>null</code>.
     */
    private PyObject getFieldObject(String fieldName, boolean bytes, PyObject[] args,
            String[] keywords) {
        FieldNameIterator iterator = new FieldNameIterator(fieldName, bytes);
        PyObject head = iterator.pyHead();
        PyObject obj = null;
        int positionalCount = args.length - keywords.length;

        if (head.isIndex()) {
            // The field name begins with an integer argument index (not a [n]-type index).
            int index = head.asIndex();
            if (index >= positionalCount) {
                throw Py.IndexError("tuple index out of range");
            }
            obj = args[index];

        } else {
            // The field name begins with keyword.
            for (int i = 0; i < keywords.length; i++) {
                if (keywords[i].equals(head.asString())) {
                    obj = args[positionalCount + i];
                    break;
                }
            }
            // And if we don't find it, that's an error
            if (obj == null) {
                throw Py.KeyError(head);
            }
        }

        // Now deal with the iterated sub-fields
        while (obj != null) {
            FieldNameIterator.Chunk chunk = iterator.nextChunk();
            if (chunk == null) {
                // End of iterator
                break;
            }
            Object key = chunk.value;
            if (chunk.is_attr) {
                // key must be a String
                obj = obj.__getattr__((String) key);
            } else {
                if (key instanceof Integer) {
                    // Can this happen?
                    obj = obj.__getitem__(((Integer) key).intValue());
                } else {
                    obj = obj.__getitem__(new PyString(key.toString()));
                }
            }
        }

        return obj;
    }

    /**
     * Append to a formatting result, the presentation of one object, according to a given format
     * specification and the object's <code>__format__</code> method.
     *
     * @param fieldObj to format.
     * @param formatSpec specification to apply.
     * @param result to which the result will be appended.
     */
    private void renderField(PyObject fieldObj, String formatSpec, StringBuilder result) {
        PyString formatSpecStr = formatSpec == null ? Py.EmptyString : new PyString(formatSpec);
        result.append(fieldObj.__format__(formatSpecStr).asString());
    }

    @Override
    public PyObject __format__(PyObject formatSpec) {
        return str___format__(formatSpec);
    }

    @ExposedMethod(doc = BuiltinDocs.str___format___doc)
    final PyObject str___format__(PyObject formatSpec) {

        // Parse the specification
        Spec spec = InternalFormat.fromText(formatSpec, "__format__");

        // Get a formatter for the specification
        TextFormatter f = prepareFormatter(spec);
        if (f == null) {
            // The type code was not recognised
            throw Formatter.unknownFormat(spec.type, "string");
        }

        // Bytes mode if neither this nor formatSpec argument is Unicode.
        boolean unicode = this instanceof PyUnicode || formatSpec instanceof PyUnicode;
        f.setBytes(!unicode);

        // Convert as per specification.
        f.format(getString());

        // Return a result that has the same type (str or unicode) as the formatSpec argument.
        return f.pad().getPyResult();
    }

    /**
     * Common code for {@link PyString} and {@link PyUnicode} to prepare a {@link TextFormatter}
     * from a parsed specification. The object returned has format method
     * {@link TextFormatter#format(String)} that treats its argument as UTF-16 encoded unicode (not
     * just <code>char</code>s). That method will format its argument ( <code>str</code> or
     * <code>unicode</code>) according to the PEP 3101 formatting specification supplied here. This
     * would be used during <code>text.__format__(".5s")</code> or
     * <code>"{:.5s}".format(text)</code> where <code>text</code> is this Python string.
     *
     * @param spec a parsed PEP-3101 format specification.
     * @return a formatter ready to use, or null if the type is not a string format type.
     * @throws PyException {@code ValueError} if the specification is faulty.
     */
    @SuppressWarnings("fallthrough")
    static TextFormatter prepareFormatter(Spec spec) throws PyException {
        // Slight differences between format types
        switch (spec.type) {

            case Spec.NONE:
            case 's':
                // Check for disallowed parts of the specification
                if (spec.grouping) {
                    throw Formatter.notAllowed("Grouping", "string", spec.type);
                } else if (Spec.specified(spec.sign)) {
                    throw Formatter.signNotAllowed("string", '\0');
                } else if (spec.alternate) {
                    throw Formatter.alternateFormNotAllowed("string");
                } else if (spec.align == '=') {
                    throw Formatter.alignmentNotAllowed('=', "string");
                }
                // spec may be incomplete. The defaults are those commonly used for string formats.
                spec = spec.withDefaults(Spec.STRING);
                // Get a formatter for the specification
                return new TextFormatter(spec);

            default:
                // The type code was not recognised
                return null;
        }
    }

    @Override
    public String asString(int index) throws PyObject.ConversionException {
        return getString();
    }

    @Override
    public String asString() {
        return getString();
    }

    @Override
    public int asInt() {
        // We have to override asInt/Long/Double because we override __int/long/float__,
        // but generally don't want implicit atoi conversions for the base types. blah
        asNumberCheck("__int__", "an integer");
        return super.asInt();
    }

    @Override
    public long asLong() {
        asNumberCheck("__long__", "an integer");
        return super.asLong();
    }

    @Override
    public double asDouble() {
        asNumberCheck("__float__", "a float");
        return super.asDouble();
    }

    private void asNumberCheck(String methodName, String description) {
        PyType type = getType();
        if (type == PyString.TYPE || type == PyUnicode.TYPE || type.lookup(methodName) == null) {
            throw Py.TypeError(description + " is required");
        }
    }

    @Override
    public String asName(int index) throws PyObject.ConversionException {
        return internedString();
    }

    @Override
    protected String unsupportedopMessage(String op, PyObject o2) {
        if (op.equals("+")) {
            return "cannot concatenate ''{1}'' and ''{2}'' objects";
        }
        return super.unsupportedopMessage(op, o2);
    }

    @Override
    public char charAt(int index) {
        return string.charAt(index);
    }

    @Override
    public int length() {
        return string.length();
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return string.subSequence(start, end);
    }

    /**
     * Decode this <code>str</code> object to a <code>unicode</code>, like
     * <code>__unicode__()</code> but without the possibility it will be overridden.
     *
     * @return this as a <code>unicode</code> using the default encoding.
     */
    private PyUnicode asUnicode() {
        return new PyUnicode(this);
    }

    /**
     * Decode a slice of this <code>str</code> object to a <code>unicode</code>, using Python slice
     * semantics and the default encoding. This supports the many library methods that accept
     * slicing as part of the API, in the case where the calculation must be promoted due to a
     * <code>unicode</code> argument.
     *
     * @param startObj start index (or <code>null</code> or <code>None</code>)
     * @param endObj end index (or <code>null</code> or <code>None</code>)
     * @return the slice as a <code>unicode</code> using the default encoding.
     */
    private PyUnicode asUnicode(PyObject startObj, PyObject endObj) {
        if (startObj == null && endObj == null) {
            return asUnicode();
        } else {
            int[] indices = translateIndices(startObj, endObj);
            return new PyUnicode(fromSubstring(indices[0], indices[1]));
        }
    }
}


/**
 * Interpreter for %-format strings. (Note visible across the core package.)
 */
final class StringFormatter {

    /** Index into {@link #format} being interpreted. */
    int index;
    /** Format being interpreted. */
    String format;
    /** Where the output is built. */
    StringBuilder buffer;
    /**
     * Index into args of argument currently being worked, or special values indicating -1: a single
     * item that has not yet been used, -2: a single item that has already been used, -3: a mapping.
     */
    int argIndex;
    /** Arguments supplied to {@link #format(PyObject)} method. */
    PyObject args;
    /** Indicate a <code>PyUnicode</code> result is expected. */
    boolean needUnicode;

    final char pop() {
        try {
            return format.charAt(index++);
        } catch (StringIndexOutOfBoundsException e) {
            throw Py.ValueError("incomplete format");
        }
    }

    final char peek() {
        return format.charAt(index);
    }

    final void push() {
        index--;
    }

    /**
     * Initialise the interpreter with the given format string, ready for {@link #format(PyObject)}.
     *
     * @param format string to interpret
     */
    public StringFormatter(String format) {
        this(format, false);
    }

    /**
     * Initialise the interpreter with the given format string, ready for {@link #format(PyObject)}.
     *
     * @param format string to interpret
     * @param unicodeCoercion to indicate a <code>PyUnicode</code> result is expected
     */
    public StringFormatter(String format, boolean unicodeCoercion) {
        index = 0;
        this.format = format;
        this.needUnicode = unicodeCoercion;
        buffer = new StringBuilder(format.length() + 100);
    }

    /**
     * Read the next object from the argument list, taking special values of <code>argIndex</code>
     * into account.
     */
    PyObject getarg() {
        PyObject ret = null;
        switch (argIndex) {
            case -3: // special index indicating a mapping
                return args;
            case -2: // special index indicating a single item that has already been used
                break;
            case -1: // special index indicating a single item that has not yet been used
                argIndex = -2;
                return args;
            default:
                ret = args.__finditem__(argIndex++);
                break;
        }
        if (ret == null) {
            throw Py.TypeError("not enough arguments for format string");
        }
        return ret;
    }

    /**
     * Parse a number from the format, except if the next thing is "*", read it from the argument
     * list.
     */
    int getNumber() {
        char c = pop();
        if (c == '*') {
            PyObject o = getarg();
            if (o instanceof PyInteger) {
                return ((PyInteger) o).getValue();
            }
            throw Py.TypeError("* wants int");
        } else {
            if (Character.isDigit(c)) {
                int numStart = index - 1;
                while (Character.isDigit(c = pop())) {}
                index -= 1;
                Integer i = Integer.valueOf(format.substring(numStart, index));
                return i.intValue();
            }
            index -= 1;
            return 0;
        }
    }

    /**
     * Return the argument as either a {@link PyInteger} or a {@link PyLong} according to its
     * <code>__int__</code> method, or its <code>__long__</code> method. If the argument has neither
     * method, or both raise an exception, we return the argument itself. The caller must check the
     * return type.
     *
     * @param arg to convert
     * @return PyInteger or PyLong if possible
     */
    private PyObject asNumber(PyObject arg) {
        if (arg instanceof PyInteger || arg instanceof PyLong) {
            // arg is already acceptable
            return arg;

        } else {
            // use __int__ or __long__to get an int (or long)
            if (arg.getClass() == PyFloat.class) {
                // A common case where it is safe to return arg.__int__()
                return arg.__int__();

            } else {
                /*
                 * In general, we can't simply call arg.__int__() because PyString implements it
                 * without exposing it to python (str has no __int__). This would make str
                 * acceptacle to integer format specifiers, which is forbidden by CPython tests
                 * (test_format.py). PyString implements __int__ perhaps only to help the int
                 * constructor. Maybe that was a bad idea?
                 */
                try {
                    // Result is the result of arg.__int__() if that works
                    return arg.__getattr__("__int__").__call__();
                } catch (PyException e) {
                    // Swallow the exception
                }

                // Try again with arg.__long__()
                try {
                    // Result is the result of arg.__long__() if that works
                    return arg.__getattr__("__long__").__call__();
                } catch (PyException e) {
                    // No __long__ defined (at Python level)
                    return arg;
                }
            }
        }
    }

    /**
     * Return the argument as a {@link PyFloat} according to its <code>__float__</code> method. If
     * the argument has no such method, or it raises an exception, we return the argument itself.
     * The caller must check the return type.
     *
     * @param arg to convert
     * @return PyFloat if possible
     */
    private PyObject asFloat(PyObject arg) {

        if (arg instanceof PyFloat) {
            // arg is already acceptable
            return arg;

        } else {
            // use __float__ to get a float.
            if (arg.getClass() == PyFloat.class) {
                // A common case where it is safe to return arg.__float__()
                return arg.__float__();

            } else {
                /*
                 * In general, we can't simply call arg.__float__() because PyString implements it
                 * without exposing it to python (str has no __float__). This would make str
                 * acceptacle to float format specifiers, which is forbidden by CPython tests
                 * (test_format.py). PyString implements __float__ perhaps only to help the float
                 * constructor. Maybe that was a bad idea?
                 */
                try {
                    // Result is the result of arg.__float__() if that works
                    return arg.__getattr__("__float__").__call__();
                } catch (PyException e) {
                    // No __float__ defined (at Python level)
                    return arg;
                }
            }
        }
    }

    /**
     * Return the argument as either a {@link PyString} or a {@link PyUnicode}, and set the
     * {@link #needUnicode} member accordingly. If we already know we are building a Unicode string
     * (<code>needUnicode==true</code>), then any argument that is not already a
     * <code>PyUnicode</code> will be converted by calling its <code>__unicode__</code> method.
     * Conversely, if we are not yet building a Unicode string (<code>needUnicode==false</code> ),
     * then a PyString will pass unchanged, a <code>PyUnicode</code> will switch us to Unicode mode
     * (<code>needUnicode=true</code>), and any other type will be converted by calling its
     * <code>__str__</code> method, which will return a <code>PyString</code>, or possibly a
     * <code>PyUnicode</code>, which will switch us to Unicode mode.
     *
     * @param arg to convert
     * @return PyString or PyUnicode equivalent
     */
    private PyString asText(PyObject arg) {

        if (arg instanceof PyUnicode) {
            // arg is already acceptable.
            needUnicode = true;
            return (PyUnicode) arg;

        } else if (needUnicode) {
            // The string being built is unicode, so we need that version of the arg.
            return arg.__unicode__();

        } else if (arg instanceof PyString) {
            // The string being built is not unicode, so arg is already acceptable.
            return (PyString) arg;

        } else {
            // The string being built is not unicode, so use __str__ to get a PyString.
            PyString s = arg.__str__();
            // But __str__ might return PyUnicode, and we have to notice that.
            if (s instanceof PyUnicode) {
                needUnicode = true;
            }
            return s;
        }
    }

    /**
     * Main service of this class: format one or more arguments with the format string supplied at
     * construction.
     *
     * @param args tuple or map containing objects, or a single object, to convert
     * @return result of formatting
     */
    @SuppressWarnings("fallthrough")
    public PyString format(PyObject args) {
        PyObject dict = null;
        this.args = args;

        if (args instanceof PyTuple) {
            // We will simply work through the tuple elements
            argIndex = 0;
        } else {
            // Not a tuple, but possibly still some kind of container: use special argIndex values.
            argIndex = -1;
            if (args instanceof AbstractDict || (!(args instanceof PySequence) &&
            // See issue 2511: __getitem__ should be looked up directly in the dict, rather
            // than going through another __getattr__ call. We achieve this by using
            // object___findattr__ instead of generic __findattr__.
                    args.object___findattr__("__getitem__".intern()) != null)) {
                dict = args;
                argIndex = -3;
            }
        }

        while (index < format.length()) {

            // Read one character from the format string
            char c = pop();
            if (c != '%') {
                buffer.append(c);
                continue;
            }

            // It's a %, so the beginning of a conversion specifier. Parse it.

            // Attributes to be parsed from the next format specifier
            boolean altFlag = false;
            char sign = Spec.NONE;
            char fill = ' ';
            char align = '>';
            int width = Spec.UNSPECIFIED;
            int precision = Spec.UNSPECIFIED;

            // A conversion specifier contains the following components, in this order:
            // + The '%' character, which marks the start of the specifier.
            // + Mapping key (optional), consisting of a parenthesised sequence of characters.
            // + Conversion flags (optional), which affect the result of some conversion types.
            // + Minimum field width (optional), or an '*' (asterisk).
            // + Precision (optional), given as a '.' (dot) followed by the precision or '*'.
            // + Length modifier (optional).
            // + Conversion type.

            c = pop();
            if (c == '(') {
                // Mapping key, consisting of a parenthesised sequence of characters.
                if (dict == null) {
                    throw Py.TypeError("format requires a mapping");
                }
                // Scan along until a matching close parenthesis is found
                int parens = 1;
                int keyStart = index;
                while (parens > 0) {
                    c = pop();
                    if (c == ')') {
                        parens--;
                    } else if (c == '(') {
                        parens++;
                    }
                }
                // Last c=pop() is the closing ')' while indexKey is just after the opening '('
                String tmp = format.substring(keyStart, index - 1);
                // Look it up using this extent as the (right type of) key.
                this.args = dict.__getitem__(needUnicode ? new PyUnicode(tmp) : new PyString(tmp));
            } else {
                // Not a mapping key: next clause will re-read c.
                push();
            }

            // Conversion flags (optional) that affect the result of some conversion types.
            while (true) {
                switch (c = pop()) {
                    case '-':
                        align = '<';
                        continue;
                    case '+':
                        sign = '+';
                        continue;
                    case ' ':
                        if (!Spec.specified(sign)) {
                            // Blank sign only wins if '+' not specified.
                            sign = ' ';
                        }
                        continue;
                    case '#':
                        altFlag = true;
                        continue;
                    case '0':
                        fill = '0';
                        continue;
                }
                break;
            }
            // Push back c as next clause will re-read c.
            push();

            /*
             * Minimum field width (optional). If specified as an '*' (asterisk), the actual width
             * is read from the next element of the tuple in values, and the object to convert comes
             * after the minimum field width and optional precision. A custom getNumber() takes care
             * of the '*' case.
             */
            width = getNumber();
            if (width < 0) {
                width = -width;
                align = '<';
            }

            /*
             * Precision (optional), given as a '.' (dot) followed by the precision. If specified as
             * '*' (an asterisk), the actual precision is read from the next element of the tuple in
             * values, and the value to convert comes after the precision. A custom getNumber()
             * takes care of the '*' case.
             */
            c = pop();
            if (c == '.') {
                precision = getNumber();
                if (precision < -1) {
                    precision = 0;
                }
                c = pop();
            }

            // Length modifier (optional). (Compatibility feature?) It has no effect.
            if (c == 'h' || c == 'l' || c == 'L') {
                c = pop();
            }

            /*
             * As a function of the conversion type (currently in c) override some of the formatting
             * flags we read from the format specification.
             */
            switch (c) {
                case 's':
                case 'r':
                case 'c':
                case '%':
                    // These have string-like results: fill, if needed, is always blank.
                    fill = ' ';
                    break;

                default:
                    if (fill == '0' && align == '>') {
                        // Zero-fill comes after the sign in right-justification.
                        align = '=';
                    } else {
                        // If left-justifying, the fill is always blank.
                        fill = ' ';
                    }
            }

            /*
             * Encode as an InternalFormat.Spec. The values in the constructor always have specified
             * values, except for sign, width and precision.
             */
            Spec spec = new Spec(fill, align, sign, altFlag, width, false, precision, c);

            /*
             * Process argument according to format specification decoded from the string. It is
             * important we don't read the argument from the list until this point because of the
             * possibility that width and precision were specified via the argument list.
             */

            // Depending on the type of conversion, we use one of these formatters:
            FloatFormatter ff;
            IntegerFormatter fi;
            TextFormatter ft;
            Formatter f; // = ff, fi or ft, whichever we actually use.

            switch (spec.type) {

                case 's': // String: converts any object using __str__(), __unicode__() ...
                case 'r': // ... or repr().
                    PyObject arg = getarg();

                    // Get hold of the actual object to display (may set needUnicode)
                    PyString argAsString = asText(spec.type == 's' ? arg : arg.__repr__());
                    // Format the str/unicode form of the argument using this Spec.
                    f = ft = new TextFormatter(buffer, spec);
                    ft.setBytes(!needUnicode);
                    ft.format(argAsString.getString());
                    break;

                case 'd': // All integer formats (+case for X).
                case 'o':
                case 'x':
                case 'X':
                case 'c': // Single character (accepts integer or single character string).
                case 'u': // Obsolete type identical to 'd'.
                case 'i': // Compatibility with scanf().

                    // Format the argument using this Spec.
                    f = fi = new IntegerFormatter.Traditional(buffer, spec);
                    // If not producing PyUnicode, disallow codes >255.
                    fi.setBytes(!needUnicode);

                    arg = getarg();

                    if (arg instanceof PyString && spec.type == 'c') {
                        if (arg.__len__() != 1) {
                            throw Py.TypeError("%c requires int or char");
                        } else {
                            if (!needUnicode && arg instanceof PyUnicode) {
                                // Change of mind forced by encountering unicode object.
                                needUnicode = true;
                                fi.setBytes(false);
                            }
                            fi.format(((PyString) arg).getString().codePointAt(0));
                        }

                    } else {
                        // Note various types accepted here as long as they have an __int__ method.
                        PyObject argAsNumber = asNumber(arg);

                        // We have to check what we got back.
                        if (argAsNumber instanceof PyInteger) {
                            fi.format(((PyInteger) argAsNumber).getValue());
                        } else if (argAsNumber instanceof PyLong) {
                            fi.format(((PyLong) argAsNumber).getValue());
                        } else {
                            // It couldn't be converted, raise the error here
                            throw Py.TypeError(
                                    "%" + spec.type + " format: a number is required, not "
                                            + arg.getType().fastGetName());
                        }
                    }

                    break;

                case 'e': // All floating point formats (+case).
                case 'E':
                case 'f':
                case 'F':
                case 'g':
                case 'G':

                    // Format using this Spec the double form of the argument.
                    f = ff = new FloatFormatter(buffer, spec);
                    ff.setBytes(!needUnicode);

                    // Note various types accepted here as long as they have a __float__ method.
                    arg = getarg();
                    PyObject argAsFloat = asFloat(arg);

                    // We have to check what we got back..
                    if (argAsFloat instanceof PyFloat) {
                        ff.format(((PyFloat) argAsFloat).getValue());
                    } else {
                        // It couldn't be converted, raise the error here
                        throw Py.TypeError(
                                "float argument required, not " + arg.getType().fastGetName());
                    }

                    break;

                case '%': // Percent symbol, but surprisingly, padded.

                    // We use an integer formatter.
                    f = fi = new IntegerFormatter.Traditional(buffer, spec);
                    fi.setBytes(!needUnicode);
                    fi.format('%');
                    break;

                default:
                    throw Py.ValueError("unsupported format character '"
                            + codecs.encode(Py.newUnicode(spec.type), null, "replace") + "' (0x"
                            + Integer.toHexString(spec.type) + ") at index " + (index - 1));
            }

            // Pad the result as specified (in-place, in the buffer).
            f.pad();
        }

        /*
         * All fields in the format string have been used to convert arguments (or used the argument
         * as a width, etc.). This had better not leave any arguments unused. Note argIndex is an
         * index into args or has a special value. If args is a 'proper' index, It should now be out
         * of range; if a special value, it would be wrong if it were -1, indicating a single item
         * that has not yet been used.
         */
        if (argIndex == -1 || (argIndex >= 0 && args.__finditem__(argIndex) != null)) {
            throw Py.TypeError("not all arguments converted during string formatting");
        }

        // Return the final buffer contents as a str or unicode as appropriate.
        return needUnicode ? new PyUnicode(buffer) : new PyString(buffer);
    }

}
