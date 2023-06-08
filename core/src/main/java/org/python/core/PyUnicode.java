// Copyright (c)2023 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.core;

import java.lang.invoke.MethodHandles;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.PrimitiveIterator;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Function;
import java.util.function.IntUnaryOperator;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

import org.python.base.InterpreterError;
import org.python.base.MissingFeature;
import org.python.core.Exposed.Default;
import org.python.core.Exposed.Name;
import org.python.core.Exposed.PythonMethod;
import org.python.core.PyObjectUtil.NoConversion;
import org.python.core.PySequence.Delegate;
import org.python.core.PySlice.Indices;
import org.python.core.stringlib.FieldNameIterator;
import org.python.core.stringlib.IntArrayBuilder;
import org.python.core.stringlib.IntArrayReverseBuilder;
import org.python.core.stringlib.InternalFormat;
import org.python.core.stringlib.InternalFormat.AbstractFormatter;
import org.python.core.stringlib.InternalFormat.FormatError;
import org.python.core.stringlib.InternalFormat.FormatOverflow;
import org.python.core.stringlib.InternalFormat.Spec;
import org.python.core.stringlib.MarkupIterator;
import org.python.core.stringlib.TextFormatter;
import org.python.modules.ucnhashAPI;

/**
 * The Python {@code str} object is implemented by both
 * {@code PyUnicode} and Java {@code String}. All operations will
 * produce the same result for Python, whichever representation is
 * used. Both types are treated as an array of code points in
 * Python.
 * <p>
 * Most strings used as names (keys) and text are quite
 * satisfactorily represented by Java {@code String}. Java
 * {@code String}s are compact, but where they contain non-BMP
 * characters, these are represented by a pair of code units. That
 * makes certain operations (such as indexing or slicing) relatively
 * expensive compared to Java. Accessing the code points of a
 * {@code String} sequentially is still cheap.
 * <p>
 * By contrast, a {@code PyUnicode} is time-efficient, but each
 * character occupies one {@code int}.
 */
public class PyUnicode implements CraftedPyObject, PyDict.Key {

    /** The type {@code str}. */
    public static final PyType TYPE = PyType.fromSpec( //
            new PyType.Spec("str", MethodHandles.lookup()) //
                    .methods(PyUnicodeMethods.class) //
                    .adopt(String.class));

    /**
     * The actual Python type of this {@code PyUnicode}.
     */
    protected PyType type;

    /**
     * The implementation holds a Java {@code int} array of code points.
     */
    private final int[] value;

    /**
     * Helper to implement {@code __getitem__} and other index-related
     * operations.
     */
    private UnicodeAdapter delegate = new UnicodeAdapter();

    /**
     * Cached hash of the {@code str}, lazily computed in
     * {@link #hashCode()}. Zero if unknown, and nearly always unknown
     * if zero.
     */
    private int hash;

    /**
     * Construct an instance of {@code PyUnicode}, a {@code str} or a
     * sub-class, from a given array of code points, with the option to
     * re-use that array as the implementation. If the actual array is
     * is re-used the caller must give up ownership and never modify it
     * after the call. See {@link #fromCodePoint(int)} for a correct
     * use.
     *
     * @param type actual type the instance should have
     * @param iPromiseNotToModify if {@code true}, the array becomes the
     *     implementation array, otherwise the constructor takes a copy.
     * @param codePoints the array of code points
     */
    private PyUnicode(PyType type, boolean iPromiseNotToModify, int[] codePoints) {
        this.type = type;
        if (iPromiseNotToModify)
            this.value = codePoints;
        else
            this.value = Arrays.copyOf(codePoints, codePoints.length);
    }

    /**
     * Construct an instance of {@code PyUnicode}, a {@code str} or a
     * sub-class, from a given array of code points. The constructor
     * takes a copy.
     *
     * @param type actual type the instance should have
     * @param codePoints the array of code points
     */
    protected PyUnicode(PyType type, int[] codePoints) { this(type, false, codePoints); }

    /**
     * Construct an instance of {@code PyUnicode}, a {@code str} or a
     * sub-class, from the given code points. The constructor takes a
     * copy.
     *
     * @param codePoints the array of code points
     */
    protected PyUnicode(int... codePoints) { this(TYPE, false, codePoints); }

    /**
     * Construct an instance of {@code PyUnicode}, a {@code str} or a
     * sub-class, from a given {@link IntArrayBuilder}. This will reset
     * the builder to empty.
     *
     * @param value from which to take the code points
     */
    protected PyUnicode(IntArrayBuilder value) { this(TYPE, true, value.take()); }

    /**
     * Construct an instance of {@code PyUnicode}, a {@code str} or a
     * sub-class, from a given {@link IntArrayReverseBuilder}. This will
     * reset the builder to empty.
     *
     * @param value from which to take the code points
     */
    protected PyUnicode(IntArrayReverseBuilder value) { this(TYPE, true, value.take()); }

    /**
     * Construct an instance of {@code PyUnicode}, a {@code str} or a
     * sub-class, from a given Java {@code String}. The constructor
     * interprets surrogate pairs as defining one code point. Lone
     * surrogates are preserved (e.g. for byte smuggling).
     *
     * @param type actual type the instance should have
     * @param value to have
     */
    protected PyUnicode(PyType type, String value) {
        this(TYPE, true, value.codePoints().toArray());
    }

    // Factory methods ------------------------------------------------
    // These may return a Java String or a PyUnicode

    /**
     * Unsafely wrap an array of code points as a {@code PyUnicode}. The
     * caller must not hold a reference to the argument array (and
     * definitely not manipulate the contents).
     *
     * @param codePoints to wrap as a {@code str}
     * @return the {@code str}
     */
    private static PyUnicode wrap(int[] codePoints) {
        return new PyUnicode(TYPE, true, codePoints);
    }

    /**
     * Safely wrap the contents of an {@link IntArrayBuilder} of code
     * points as a {@code PyUnicode}.
     *
     * @param codePoints to wrap as a {@code str}
     * @return the {@code str}
     */
    public static PyUnicode wrap(IntArrayBuilder codePoints) {
        return new PyUnicode(codePoints);
    }

    /**
     * Return a Python {@code str} representing the single character
     * with the given code point. The return may be a Java
     * {@code String} (for BMP code points) or a {@code PyUnicode}.
     *
     * @param cp to code point convert
     * @return a Python {@code str}
     */
    public static Object fromCodePoint(int cp) {
        // We really need to know how the string will be used :(
        if (cp < Character.MIN_SUPPLEMENTARY_CODE_POINT)
            return String.valueOf((char)cp);
        else
            return wrap(new int[] {cp});
    }

    /**
     * Return a Python {@code str} representing the same sequence of
     * characters as the given Java {@code String} and implemented as a
     * {@code PyUnicode}.
     *
     * @param s to convert
     * @return a Python {@code str}
     */
    public static PyUnicode fromJavaString(String s) {
        // XXX share simple cases len==0 len==1 & ascii?
        return new PyUnicode(TYPE, s);
    }

    @Override
    public PyType getType() { return type; }

    // ------------------------------------------------------------------------------------------

    public static String checkEncoding(String s) {
        if (s == null || s.chars().allMatch(c -> c < 128)) { return s; }
        return codecs.PyUnicode_EncodeASCII(s, s.length(), null);
    }

    // @formatter:off
    /*
    @ExposedNew
    final static PyObject new(PyNewWrapper new_, boolean init, PyType subtype,
            PyObject[] args, String[] keywords) {
        ArgParser ap = new ArgParser("unicode", args, keywords,
                new String[] {"string", "encoding", "errors"}, 0);
        PyObject S = ap.getPyObject(0, null);
        String encoding = checkEncoding(ap.getString(1, null));
        String errors = checkEncoding(ap.getString(2, null));
        if (new_.for_type == subtype) {
            if (S == null) {
                return new PyUnicode("");
            }
            if (S instanceof PyUnicode) {
                return new PyUnicode(((PyUnicode) S).getString());
            }
            if (S instanceof PyString) {
                if (S.getType() != PyString.TYPE && encoding == null && errors == null) {
                    return S.__unicode__();
                }
                PyObject decoded = codecs.decode((PyString) S, encoding, errors);
                if (decoded instanceof PyUnicode) {
                    return new PyUnicode((PyUnicode) decoded);
                } else {
                    throw new TypeError("decoder did not return an unicode object (type="
                            + decoded.getType().fastGetName() + ")");
                }
            }
            return S.__unicode__();
        } else {
            if (S == null) {
                return new PyUnicodeDerived(subtype, new PyString(""));
            }
            if (S instanceof PyUnicode) {
                return new PyUnicodeDerived(subtype, (PyUnicode) S);
            } else {
                return new PyUnicodeDerived(subtype, S.__str__());
            }
        }
    }
    */


    // Special methods ------------------------------------------------

    /*
    @ExposedMethod(doc = BuiltinDocs.unicode___str___doc)
    */
    @SuppressWarnings("unused")
    private Object __str__() { return this; }

    @SuppressWarnings("unused")
    private static Object __str__(String self) { return self; }

    /*
    @ExposedMethod(doc = BuiltinDocs.unicode___repr___doc)
    */
    @SuppressWarnings("unused")
    private static Object __repr__(Object self) {
        try {
            // XXX make encode_UnicodeEscape (if needed) take a delegate
            return encode_UnicodeEscape(convertToString(self), true);
        } catch (NoConversion nc) {
            throw Abstract.impossibleArgumentError("str", self);
        }
    }

    /*
    @ExposedMethod(doc = BuiltinDocs.unicode___len___doc)
    */
    private int __len__() { return value.length; }

    @SuppressWarnings("unused")
    private static int __len__(String self) {
        return self.codePointCount(0, self.length());
    }

    /*
    @ExposedMethod(doc = BuiltinDocs.unicode___hash___doc)
    */
    private int __hash__() {
        // Reproduce on value the hash defined for java.lang.String
        if (hash == 0 && value.length > 0) {
            int h = 0;
            for (int c : value) {
                if (Character.isBmpCodePoint(c)) {
                    // c is represented by itself in a String
                    h = h * 31 + c;
                } else {
                    // c would be represented in a Java String by:
                    int hi = (c >>> 10) + HIGH_SURROGATE_OFFSET;
                    int lo = (c & 0x3ff) + Character.MIN_LOW_SURROGATE;
                    h = (h * 31 + hi) * 31 + lo;
                }
            }
            hash = h;
        }
        return hash;
    }

    @SuppressWarnings("unused")
    private static int __hash__(String self) { return self.hashCode(); }

    /*
    @ExposedMethod(doc = BuiltinDocs.unicode___getitem___doc)
    */
    @SuppressWarnings("unused")
    private Object __getitem__(Object item) throws Throwable {
        return delegate.__getitem__(item);
    }

    @SuppressWarnings("unused")
    private static Object __getitem__(String self, Object item)
            throws Throwable {
        StringAdapter delegate = adapt(self);
        return delegate.__getitem__(item);
    }

    // Copied from PyString
    public Object __tojava__(Class<?> c) {
        // XXX something like this necessary in Jython 3 but not used yet
        // Need PyUnicode and String versions
        if (c.isAssignableFrom(String.class)) {
            /*
             * If c is a CharSequence we assume the caller is prepared to get maybe not an actual
             * String. In that case we avoid conversion so the caller can do special stuff with the
             * returned PyString or PyUnicode or whatever. (If c is Object.class, the caller usually
             * expects to get actually a String)
             */
            // XXX this is a bit questionable if non-BMP
            return c == CharSequence.class ? this : asString();
        }

        if (c == Character.TYPE || c == Character.class) {
            // XXX ? non-BMP
            String s = asString();
            if (s.length() == 1) {
                return s.charAt(0);
            }
        }

        if (c.isArray()) {
//            if (c.getComponentType() == Byte.TYPE) {
//                return toBytes();
//            }
            if (c.getComponentType() == Character.TYPE) {
                // XXX ? non-BMP
                return asString().toCharArray();
            }
        }

//        if (c.isAssignableFrom(Collection.class)) {
//            List<Object> list = new ArrayList();
//            for (int i = 0; i < __len__(); i++) {
//                list.add(pyget(i).__tojava__(String.class));
//            }
//            return list;
//        }

        if (c.isInstance(this)) {
            return this;
        }

        throw new MissingFeature("default __tojava__ behaviour for %s", c.getSimpleName());
    }

    /*
    @ExposedMethod(doc = BuiltinDocs.unicode___contains___doc)
    */
    @SuppressWarnings("unused")
    private boolean __contains__(Object o) {
        return contains(delegate, o);
    }

    @SuppressWarnings("unused")
    private static boolean __contains__(String self, Object o) {
        return contains(adapt(self), o);
    }

    private static boolean contains(CodepointDelegate s, Object o) {
        try {
            CodepointDelegate p = adapt(o);
            PySlice.Indices slice = getSliceIndices(s, null, null);
            return find(s, p, slice) >= 0;
        } catch (NoConversion nc) {
            throw Abstract.typeError(IN_STRING_TYPE, o);
        }
    }

    private static final String IN_STRING_TYPE =
            "'in <string>' requires string as left operand, not %s";

    /*
    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.unicode___add___doc)
    */
    @SuppressWarnings("unused")
    private Object __add__(Object w) throws Throwable {
        return delegate.__add__(w);
    }

    @SuppressWarnings("unused")
    private static Object __add__(String v, Object w) throws Throwable {
        return adapt(v).__add__(w);
    }

    @SuppressWarnings("unused")
    private Object __radd__(Object v) throws Throwable {
        return delegate.__radd__(v);
    }

    @SuppressWarnings("unused")
    private static Object __radd__(String w, Object v) throws Throwable {
        return adapt(w).__radd__(v);
    }

    /*
    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.unicode___mul___doc)
    */
    private Object __mul__(Object n) throws Throwable {
        return delegate.__mul__(n);
    }

    private static Object __mul__(String self, Object n)
            throws Throwable {
        return adapt(self).__mul__(n);
    }

    /*
    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.unicode___rmul___doc)
    */
    @SuppressWarnings("unused")
    private Object __rmul__(Object n) throws Throwable {
        return __mul__(n);
    }

    @SuppressWarnings("unused")
    private static Object __rmul__(String self, Object n)
            throws Throwable {
        return __mul__(self, n);
    }

    /*
    @ExposedMethod(doc = BuiltinDocs.unicode___mod___doc)
    */
    static Object __mod__(Object self, Object other) {
        throw new MissingFeature("printf-style formatting");
    }


    // Strip methods --------------------------------------------------

    /**
     * Python {@code str.strip()}. Any character matching one of those
     * in {@code chars} will be discarded from either end of this
     * {@code str}. If {@code chars == None}, whitespace will be
     * stripped.
     *
     * @param chars characters to strip from either end of this
     *     {@code str}, or {@code None}
     * @return a new {@code str}, stripped of the specified characters
     * @throws TypeError on {@code chars} type errors
     */
    @PythonMethod(primary = false)
    Object strip(Object chars) throws TypeError {
        return strip(delegate, chars);
    }

    @PythonMethod
    static Object strip(String self, @Default("None") Object chars) throws TypeError {
        return strip(adapt(self), chars);
    }

    /**
     * Inner implementation of Python {@code str.strip()} independent of
     * the implementation type.
     *
     * @param s representing {@code self}
     * @param chars to remove, or {@code null} or {@code None}
     * @return the {@code str} stripped
     * @throws TypeError on {@code chars} type errors
     */
    private static Object strip(CodepointDelegate s, Object chars)
            throws TypeError {
        Set<Integer> p = adaptStripSet("strip", chars);
        int left, right;
        if (p == null) {
            // Stripping spaces
            right = findRight(s);
            // If it's all spaces, we know left==0
            left = right < 0 ? 0 : findLeft(s);
        } else {
            // Stripping specified characters
            right = findRight(s, p);
            // If it all matches, we know left==0
            left = right < 0 ? 0 : findLeft(s, p);
        }
        /*
         * Substring from leftmost non-matching character up to and
         * including the rightmost (or "")
         */
        PySlice.Indices slice = getSliceIndices(s, left, right + 1);
        return slice.slicelength == 0 ? "" : s.getSlice(slice);
    }

    /**
     * Helper for {@code strip}, {@code lstrip} implementation, when
     * stripping space.
     *
     * @return index of leftmost non-space character or
     *     {@code s.length()} if entirely spaces.
     */
    private static int findLeft(CodepointDelegate s) {
        CodepointIterator si = s.iterator(0);
        while (si.hasNext()) {
            if (!isPythonSpace(si.nextInt()))
                return si.previousIndex();
        }
        return s.length();
    }

    /**
     * Helper for {@code strip}, {@code lstrip} implementation, when
     * stripping specified characters.
     *
     * @param p specifies set of characters to strip
     * @return index of leftmost non-{@code p} character or
     *     {@code s.length()} if entirely found in {@code p}.
     */
    private static int findLeft(CodepointDelegate s, Set<Integer> p) {
        CodepointIterator si = s.iterator(0);
        while (si.hasNext()) {
            if (!p.contains(si.nextInt()))
                return si.previousIndex();
        }
        return s.length();
    }

    /**
     * Helper for {@code strip}, {@code rstrip} implementation, when
     * stripping space.
     *
     * @return index of rightmost non-space character or {@code -1} if
     *     entirely spaces.
     */
    private static int findRight(CodepointDelegate s) {
        CodepointIterator si = s.iteratorLast();
        while (si.hasPrevious()) {
            if (!isPythonSpace(si.previousInt()))
                return si.nextIndex();
        }
        return -1;
    }

    /**
     * Helper for {@code strip}, {@code rstrip} implementation, when
     * stripping specified characters.
     *
     * @param p specifies set of characters to strip
     * @return index of rightmost non-{@code p} character or {@code -1}
     *     if entirely found in {@code p}.
     */
    private static int findRight(CodepointDelegate s, Set<Integer> p) {
        CodepointIterator si = s.iteratorLast();
        while (si.hasPrevious()) {
            if (!p.contains(si.previousInt()))
                return si.nextIndex();
        }
        return -1;
    }

    /**
     * Python {@code str.lstrip()}. Any character matching one of those
     * in {@code chars} will be discarded from the left of this
     * {@code str}. If {@code chars == None}, whitespace will be
     * stripped.
     *
     * @param chars characters to strip from this {@code str}, or
     *     {@code None}
     * @return a new {@code str}, left-stripped of the specified
     *     characters
     * @throws TypeError on {@code chars} type errors
     */
    Object lstrip(Object chars) throws TypeError {
        return lstrip(delegate, chars);
    }

    static Object lstrip(String self, Object chars) throws TypeError {
        return lstrip(adapt(self), chars);
    }

    /**
     * Inner implementation of Python {@code str.lstrip()} independent
     * of the implementation type.
     *
     * @param s representing {@code self}
     * @param chars to remove, or {@code null} or {@code None}
     * @return the str stripped
     * @throws TypeError on {@code chars} type errors
     */
    private static Object lstrip(CodepointDelegate s, Object chars)
            throws TypeError {
        Set<Integer> p = adaptStripSet("lstrip", chars);
        int left;
        if (p == null) {
            // Stripping spaces
            left = findLeft(s);
        } else {
            // Stripping specified characters
            left = findLeft(s, p);
        }
        /*
         * Substring from this leftmost non-matching character (or "")
         */
        PySlice.Indices slice = getSliceIndices(s, left, null);
        return s.getSlice(slice);
    }

    /**
     * Python {@code str.rstrip()}. Any character matching one of those
     * in {@code chars} will be discarded from the right of this
     * {@code str}. If {@code chars == None}, whitespace will be
     * stripped.
     *
     * @param chars characters to strip from this {@code str}, or
     *     {@code None}
     * @return a new {@code str}, right-stripped of the specified
     *     characters
     * @throws TypeError on {@code chars} type errors
     */
    Object rstrip(Object chars) throws TypeError {
        return rstrip(delegate, chars);
    }

    static Object rstrip(String self, Object chars) throws TypeError {
        return rstrip(adapt(self), chars);
    }

    /**
     * Inner implementation of Python {@code str.rstrip()} independent
     * of the implementation type.
     *
     * @param s representing {@code self}
     * @param chars to remove, or {@code null} or {@code None}
     * @return the str stripped
     * @throws TypeError on {@code chars} type errors
     */
    private static Object rstrip(CodepointDelegate s, Object chars)
            throws TypeError {
        Set<Integer> p = adaptStripSet("rstrip", chars);
        int right;
        if (p == null) {
            // Stripping spaces
            right = findRight(s);
        } else {
            // Stripping specified characters
            right = findRight(s, p);
        }
        /*
         * Substring up to and including this rightmost non-matching
         * character (or "")
         */
        PySlice.Indices slice = getSliceIndices(s, null, right + 1);
        return s.getSlice(slice);
    }

    // Find-like methods ----------------------------------------------

    // @formatter:off

    /*
     * Several methods of str involve finding a target string within the
     * object receiving the call, to locate an occurrence, to count or
     * replace all occurrences, or to split the string at the first,
     * last or all occurrences.
     *
     * The fundamental algorithms are those that find the substring,
     * finding either the first occurrence, by scanning from the start
     * forwards, or the last by scanning from the end in reverse.
     *
     * Follow how find() and rfind() work, and the others will make
     * sense too, since they follow the same two patterns, but with
     * additional data movement to build the result, or repetition to
     * find all occurrences.
     */

    /**
     * Return the lowest index in the string where substring {@code sub}
     * is found, such that {@code sub} is contained in the slice
     * {@code [start:end]}. Arguments {@code start} and {@code end} are
     * interpreted as in slice notation, with {@code null} or
     * {@link Py#None} representing "missing".
     *
     * @param sub substring to find.
     * @param start start of slice.
     * @param end end of slice.
     * @return index of {@code sub} in this object or -1 if not found.
     * @throws TypeError on {@code sub} type errors
     */
    /*
    @ExposedMethod(defaults = {"null", "null"}, doc = BuiltinDocs.unicode_find_doc)
    */
    int find(Object sub, Object start, Object end) {
        return find(delegate, sub, start, end);
    }

    static int find(String self, Object sub, Object start, Object end) {
        return find(adapt(self), sub, start, end);
    }

    private static int find(CodepointDelegate s, Object sub,
            Object start, Object end) {
        CodepointDelegate p = adaptSub("find", sub);
        PySlice.Indices slice = getSliceIndices(s, start, end);
        if (p.length() == 0)
            return slice.start;
        else
            return find(s, p, slice);
    }

    /**
     * Inner implementation of Python {@code str.find()}. Return the
     * index of the leftmost occurrence of a (non-empty) substring in a
     * slice of some target string, or {@code -1} if there was no match.
     * Each string is specified by its delegate object.
     *
     * @param s to be searched
     * @param p the substring to look for
     * @param slice of {@code s} in which to search
     * @return the index of the occurrence or {@code -1}
     */
    private static int find(CodepointDelegate s, CodepointDelegate p,
            PySlice.Indices slice) {
        /*
         * Create an iterator for p (the needle string) and pick up the
         * first character we are seeking. We scan s for pChar = p[0],
         * and when it matches, divert into a full check using this
         * iterator.
         */
        CodepointIterator pi = p.iterator(0);
        int pChar = pi.nextInt(), pLength = p.length();
        CodepointIterator.Mark pMark = pi.mark(); // at p[1]
        assert pLength > 0;

        // Counting in pos avoids hasNext() calls
        int pos = slice.start, lastPos = slice.stop - pLength;

        // An iterator on s[start:end], the string being searched
        CodepointIterator si = s.iterator(pos, slice.start, slice.stop);

        while (pos++ <= lastPos) {
            if (si.nextInt() == pChar) {
                /*
                 * s[pos] matched p[0]: divert into matching the rest of
                 * p. Leave a mark in s where we shall resume if this is
                 * not a full match with p.
                 */
                CodepointIterator.Mark sPos = si.mark();
                int match = 1;
                while (match < pLength) {
                    if (pi.nextInt() != si.nextInt()) { break; }
                    match++;
                }
                // If we reached the end of p it's a match
                if (match == pLength) { return pos - 1; }
                // We stopped on a mismatch: reset si and pi
                sPos.restore();
                pMark.restore();
            }
        }
        return -1;
    }

    /**
     * Return the highest index in the string where substring
     * {@code sub} is found, such that {@code sub} is contained in the
     * slice {@code [start:end]}. Arguments {@code start} and
     * {@code end} are interpreted as in slice notation, with null or
     * {@link Py#None} representing "missing".
     *
     * @param sub substring to find.
     * @param start start of slice.
     * @param end end of slice.
     * @return index of {@code sub} in this object or -1 if not found.
     */
    /*
    @ExposedMethod(defaults = {"null", "null"}, doc = BuiltinDocs.unicode_rfind_doc)
    */
    int rfind(Object sub, Object start, Object end) {
        return rfind(delegate, sub, start, end);
    }

    static int rfind(String self, Object sub, Object start,
            Object end) {
        return rfind(adapt(self), sub, start, end);
    }

    private static int rfind(CodepointDelegate s, Object sub,
            Object start, Object end) {
        CodepointDelegate p = adaptSub("rfind", sub);
        PySlice.Indices slice = getSliceIndices(s, start, end);
        if (p.length() == 0)
            return slice.stop;
        else
            return rfind(s, p, slice);
    }

    /**
     * Inner implementation of Python {@code str.rfind()}. Return the
     * index of the rightmost occurrence of a (non-empty) substring in a
     * slice of some target string, or {@code -1} if there was no match.
     * Each string is specified by its delegate object.
     *
     * @param s to be searched
     * @param p the substring to look for
     * @param slice of {@code s} in which to search
     * @return the index of the occurrence or {@code -1}
     */
    private static int rfind(CodepointDelegate s, CodepointDelegate p,
            PySlice.Indices slice) {
        /*
         * Create an iterator for p (the needle string) and pick up the
         * last character we are seeking. We scan s in reverse for pChar
         * = p[-1], and when it matches, divert into a full check using
         * this iterator.
         */
        int pLength = p.length();
        CodepointIterator pi = p.iterator(pLength);
        int pChar = pi.previousInt();
        CodepointIterator.Mark pMark = pi.mark(); // p[-1]

        // Counting in pos avoids hasNext() calls. Start at the end.
        int pos = slice.stop, firstPos = slice.start + (pLength - 1);

        // An iterator on s[start:end], the string being searched.
        CodepointIterator si = s.iterator(pos, slice.start, slice.stop);

        while (--pos >= firstPos) {
            if (si.previousInt() == pChar) {
                /*
                 * s[pos] matched p[-1]: divert into matching the rest
                 * of p (still in reverse). Leave a mark in s where we
                 * shall resume if this is not a full match with p.
                 */
                CodepointIterator.Mark sPos = si.mark();
                int match = 1;
                while (match < pLength) {
                    if (pi.previousInt() != si.previousInt()) { break; }
                    match++;
                }
                // If we reached the start of p it's a match
                if (match == pLength) { return pos - (pLength - 1); }
                // We stopped on a mismatch: reset si and pi
                sPos.restore();
                pMark.restore();
            }
        }
        return -1;
    }

    /**
     * Python {@code str.partition()}, splits the {@code str} at the
     * first occurrence of {@code sep} returning a {@link PyTuple}
     * containing the part before the separator, the separator itself,
     * and the part after the separator.
     *
     * @param sep on which to split the string
     * @return tuple of parts
     */
    /*
    @ExposedMethod(doc = BuiltinDocs.unicode_partition_doc)
    */
    PyTuple partition(Object sep) {
        PyTuple r = partition(delegate, sep);
        return r != null ? r : Py.tuple(this, "", "");
    }

    static PyTuple partition(String self, Object sep) {
        PyTuple r = partition(adapt(self), sep);
        return r != null ? r : Py.tuple(self, "", "");
    }

    /**
     * Inner implementation of Python {@code str.partition()}. Return a
     * {@code tuple} of the split result {@code (before, sep, after)},
     * or {@code null} if there was no match.
     *
     * @param s to be split
     * @param sep the separator to look for
     * @return tuple of parts or {@code null}
     */
    private static PyTuple partition(CodepointDelegate s, Object sep) {
        /*
         * partition() uses the same pattern as find(), with the
         * difference that it records characters in a buffer as it scans
         * them, and the slice is always the whole string.
         */
        // An iterator on p, the separator.
        CodepointDelegate p = adaptSeparator("partition", sep);
        CodepointIterator pi = p.iterator(0);
        int sChar, pChar = pi.nextInt(), pLength = p.length();
        CodepointIterator.Mark pMark = pi.mark();
        assert pLength > 0;

        // Counting in pos avoids hasNext() calls.
        int pos = 0, lastPos = s.length() - pLength;

        // An iterator on s, the string being split.
        CodepointIterator si = s.iterator(pos);
        IntArrayBuilder buffer = new IntArrayBuilder();

        while (pos++ <= lastPos) {
            if ((sChar = si.nextInt()) == pChar) {
                /*
                 * s[pos] matched p[0]: divert into matching the rest of
                 * p. Leave a mark in s where we shall resume if this is
                 * not a full match with p.
                 */
                CodepointIterator.Mark sPos = si.mark();
                int match = 1;
                while (match < pLength) {
                    if (pi.nextInt() != si.nextInt()) { break; }
                    match++;
                }
                // If we reached the end of p it's a match
                if (match == pLength) {
                    // Grab what came before the match.
                    Object before = wrap(buffer.take());
                    // Now consume (the known length) after the match.
                    buffer = new IntArrayBuilder(lastPos - pos + 1);
                    buffer.append(si);
                    Object after = wrap(buffer.take());
                    // Return a result tuple
                    return Py.tuple(before, sep, after);
                }
                // We stopped on a mismatch: reset si and pi
                sPos.restore();
                pMark.restore();
            }
            // If we didn't return a result, consume one character
            buffer.append(sChar);
        }
        // If we didn't return a result, there was no match
        return null;
    }
    /**
     * Python {@code str.rpartition()}, splits the {@code str} at the
     * last occurrence of {@code sep}. Return a {@code tuple} containing
     * the part before the separator, the separator itself, and the part
     * after the separator.
     *
     * @param sep on which to split the string
     * @return tuple of parts
     */

    /*
    @ExposedMethod(doc = BuiltinDocs.unicode_rpartition_doc)
    */
    PyTuple rpartition(Object sep) {
        PyTuple r;
        r = rpartition(delegate, sep);
        return r != null ? r : Py.tuple(this, "", "");
    }

    static PyTuple rpartition(String self, Object sep) {
        PyTuple r = rpartition(adapt(self), sep);
        return r != null ? r : Py.tuple(self, "", "");
    }

    /**
     * Helper to Python {@code str.rpartition()}. Return a {@code tuple}
     * of the split result {@code (before, sep, after)}, or {@code null}
     * if there was no match.
     *
     * @param s to be split
     * @param sep the separator to look for
     * @return tuple of parts or {@code null}
     */
    private static PyTuple rpartition(CodepointDelegate s, Object sep) {
        /*
         * Create an iterator for p (the needle string) and pick up the
         * last character p[-1] we are seeking. We reset the iterator to
         * that position (pChar is still valid) when a match to p is
         * begun but proves partial.
         */
        CodepointDelegate p = adaptSeparator("rpartition", sep);
        CodepointIterator pi = p.iteratorLast();
        int sChar, pChar = pi.previousInt(), pLength = p.length();
        CodepointIterator.Mark pMark = pi.mark();
        assert pLength > 0;

        // Counting in pos avoids hasNext() calls. Start at the end.
        int pos = s.length(), firstPos = pLength - 1;

        // An iterator on s, the string being split.
        CodepointIterator si = s.iterator(pos);
        IntArrayReverseBuilder buffer = new IntArrayReverseBuilder();

        while (--pos >= firstPos) {
            if ((sChar = si.previousInt()) == pChar) {
                /*
                 * s[pos] matched p[-1]: divert into matching the rest
                 * of p (still in reverse). Leave a mark in s where we
                 * shall resume if this is not a full match with p.
                 */
                CodepointIterator.Mark sPos = si.mark();
                int match = 1;
                while (match < pLength) {
                    if (pi.previousInt() != si.previousInt()) { break; }
                    match++;
                }
                // If we reached the end of p it's a match
                if (match == pLength) {
                    // Grab what came after the match.
                    Object after = wrap(buffer.take());
                    // Now consume (the known length) before the match.
                    buffer = new IntArrayReverseBuilder(si.nextIndex());
                    buffer.prepend(si);
                    Object before = wrap(buffer.take());
                    // Return a result
                    return Py.tuple(before, sep, after);
                }
                // We stopped on a mismatch: reset si and pi
                sPos.restore();
                pMark.restore();
            }
            // If we didn't return a result, consume one character
            buffer.prepend(sChar);
        }
        // If we didn't return a result, there was no match
        return null;
    }

    /**
     * Python {@code str.split([sep [, maxsplit]])} returning a
     * {@link PyList} of {@code str}. The target {@code self} will be
     * split at each occurrence of {@code sep}. If {@code sep == null},
     * whitespace will be used as the criterion. If {@code sep} has zero
     * length, a Python {@code ValueError} is raised. If
     * {@code maxsplit} &gt;=0 and there are more feasible splits than
     * {@code maxsplit} the last element of the list contains what is
     * left over after the last split.
     *
     * @param sep string to use as separator (or {@code null} if to
     *     split on whitespace)
     * @param maxsplit maximum number of splits to make (there may be
     *     {@code maxsplit+1} parts) or {@code -1} for all possible.
     * @return list(str) result
     */
    // split(self, /, sep=None, maxsplit=-1)
    @PythonMethod(positionalOnly = false)
    PyList split(@Default("None") Object sep, @Default("-1") int maxsplit) {
        return split(delegate, sep, maxsplit);
    }

    @PythonMethod(primary = false)
    static PyList split(String self, Object sep, int maxsplit) {
        return split(adapt(self), sep, maxsplit);
    }

    private static PyList split(CodepointDelegate s, Object sep,
            int maxsplit) {
        if (sep == null || sep == Py.None) {
            // Split on runs of whitespace
            return splitAtSpaces(s, maxsplit);
        } else if (maxsplit == 0) {
            // Easy case: a list containing self.
            PyList list = new PyList();
            list.add(s.principal());
            return list;
        } else {
            // Split on specified (non-empty) string
            CodepointDelegate p = adaptSeparator("split", sep);
            return split(s, p, maxsplit);
        }
    }

    /**
     * Implementation of {@code str.split} splitting on white space and
     * returning a list of the separated parts. If there are more than
     * {@code maxsplit} feasible splits the last element of the list is
     * the remainder of the original ({@code self}) string.
     *
     * @param s delegate presenting self as code points
     * @param maxsplit limit on the number of splits (if &gt;=0)
     * @return {@code PyList} of split sections
     */
    private static PyList splitAtSpaces(CodepointDelegate s,
            int maxsplit) {
        /*
         * Result built here is a list of split parts, exactly as
         * required for s.split(None, maxsplit). If there are to be n
         * splits, there will be n+1 elements in L.
         */
        PyList list = new PyList();

        // -1 means make all possible splits, at most:
        if (maxsplit < 0) { maxsplit = s.length(); }

        // An iterator on s, the string being searched
        CodepointIterator si = s.iterator(0);
        IntArrayBuilder segment = new IntArrayBuilder();

        while (si.hasNext()) {
            // We are currently scanning space characters
            while (si.hasNext()) {
                int c;
                if (!isPythonSpace(c = si.nextInt())) {
                    // Just read a non-space: start a segment
                    segment.append(c);
                    break;
                }
            }

            /*
             * Either s ran out while we were scanning space characters,
             * or we have started a new segment. If s ran out, we'll
             * burn past the next loop. If s didn't run out, the next
             * loop accumulates the segment until the next space (or s
             * runs out).
             */

            // We are currently building a non-space segment
            while (si.hasNext()) {
                int c = si.nextInt();
                // Twist: if we've run out of splits, append c anyway.
                if (maxsplit > 0 && isPythonSpace(c)) {
                    // Just read a space: end the segment
                    break;
                } else {
                    // Non-space, or last allowed segment
                    segment.append(c);
                }
            }

            /*
             * Either s ran out while we were scanning space characters,
             * or we have created a new segment. (It is possible s ran
             * out while we created the segment, but that's ok.)
             */
            if (segment.length() > 0) {
                // We created a segment.
                --maxsplit;
                list.add(wrap(segment.take()));
            }
        }
        return list;
    }

    /**
     * Implementation of Python {@code str.split}, returning a list of
     * the separated parts. If there are more than {@code maxsplit}
     * occurrences of {@code sep} the last element of the list is the
     * remainder of the original ({@code self}) string.
     *
     * @param s delegate presenting self as code points
     * @param p at occurrences of which {@code s} should be split
     * @param maxsplit limit on the number of splits (if not &lt;=0)
     * @return {@code PyList} of split sections
     */
    private static PyList split(CodepointDelegate s,
            CodepointDelegate p, int maxsplit) {
        /*
         * The structure of split() resembles that of count() in that
         * after a match we keep going. And it resembles partition() in
         * that, between matches, we are accumulating characters into a
         * segment buffer.
         */

        // -1 means make all possible splits, at most:
        if (maxsplit < 0) { maxsplit = s.length(); }

        // An iterator on p, the string sought.
        CodepointIterator pi = p.iterator(0);
        int pChar = pi.nextInt(), pLength = p.length();
        CodepointIterator.Mark pMark = pi.mark();
        assert pLength > 0;

        // Counting in pos avoids hasNext() calls.
        int pos = 0, lastPos = s.length() - pLength, sChar;

        // An iterator on s, the string being searched.
        CodepointIterator si = s.iterator(pos);

        // Result built here is a list of split segments
        PyList list = new PyList();
        IntArrayBuilder segment = new IntArrayBuilder();

        while (si.hasNext()) {

            if (pos++ > lastPos || maxsplit <= 0) {
                /*
                 * We are too close to the end for a match now, or in
                 * our final segment (according to maxsplit==0).
                 * Everything that is left belongs to this segment.
                 */
                segment.append(si);

            } else if ((sChar = si.nextInt()) == pChar) {
                /*
                 * s[pos] matched p[0]: divert into matching the rest of
                 * p. Leave a mark in s where we shall resume if this is
                 * not a full match with p.
                 */
                CodepointIterator.Mark sPos = si.mark();
                int match = 1;
                while (match < pLength) {
                    if (pi.nextInt() != si.nextInt()) { break; }
                    match++;
                }

                if (match == pLength) {
                    /*
                     * We reached the end of p: it's a match. Emit the
                     * segment we have been accumulating, start a new
                     * one, and count a split.
                     */
                    list.add(wrap(segment.take()));
                    --maxsplit;
                    // Catch pos up with si (matches do not overlap).
                    pos = si.nextIndex();
                } else {
                    /*
                     * We stopped on a mismatch: reset si to pos. The
                     * character that matched pChar is part of the
                     * current segment.
                     */
                    sPos.restore();
                    segment.append(sChar);
                }
                // In either case, reset pi to p[1].
                pMark.restore();

            } else {
                /*
                 * The character that wasn't part of a match with p is
                 * part of the current segment.
                 */
                segment.append(sChar);
            }
        }

        /*
         * Add the segment we were building when s ran out, even if it
         * is empty.
         */
        list.add(wrap(segment.take()));
        return list;
    }

    /**
     * Python {@code str.rsplit([sep [, maxsplit]])} returning a
     * {@link PyList} of {@code str}. The target {@code self} will be
     * split at each occurrence of {@code sep}. If {@code sep == null},
     * whitespace will be used as the criterion. If {@code sep} has zero
     * length, a Python {@code ValueError} is raised. If
     * {@code maxsplit} &gt;=0 and there are more feasible splits than
     * {@code maxsplit} the last element of the list contains what is
     * left over after the last split.
     *
     * @param sep string to use as separator (or {@code null} if to
     *     split on whitespace)
     * @param maxsplit maximum number of splits to make (there may be
     *     {@code maxsplit+1} parts) or {@code -1} for all possible.
     * @return list(str) result
     */
    /*
    @ExposedMethod(defaults = {"null", "-1"}, doc = BuiltinDocs.unicode_rsplit_doc)
    */
    PyList rsplit(Object sep, int maxsplit) {
        return rsplit(delegate, sep, maxsplit);
    }

    static PyList rsplit(String self, Object sep, int maxsplit) {
        return rsplit(adapt(self), sep, maxsplit);
    }

    private static PyList rsplit(CodepointDelegate s, Object sep,
            int maxsplit) {
        if (sep == null || sep == Py.None) {
            // Split on runs of whitespace
            return rsplitAtSpaces(s, maxsplit);
        } else if (maxsplit == 0) {
            // Easy case: a list containing self.
            PyList list = new PyList();
            list.add(s.principal());
            return list;
        } else {
            // Split on specified (non-empty) string
            CodepointDelegate p = adaptSeparator("rsplit", sep);
            return rsplit(s, p, maxsplit);
        }
    }

    /**
     * Implementation of {@code str.rsplit} splitting on white space and
     * returning a list of the separated parts. If there are more than
     * {@code maxsplit} feasible splits the last element of the list is
     * the remainder of the original ({@code self}) string.
     *
     * @param s delegate presenting self as code points
     * @param maxsplit limit on the number of splits (if &gt;=0)
     * @return {@code PyList} of split sections
     */
    private static PyList rsplitAtSpaces(CodepointDelegate s,
            int maxsplit) {
        /*
         * Result built here is a list of split parts, exactly as
         * required for s.rsplit(None, maxsplit). If there are to be n
         * splits, there will be n+1 elements in L.
         */
        PyList list = new PyList();

        // -1 means make all possible splits, at most:
        if (maxsplit < 0) { maxsplit = s.length(); }

        // A reverse iterator on s, the string being searched
        CodepointIterator si = s.iteratorLast();
        IntArrayReverseBuilder segment = new IntArrayReverseBuilder();

        while (si.hasPrevious()) {
            // We are currently scanning space characters
            while (si.hasPrevious()) {
                int c;
                if (!isPythonSpace(c = si.previousInt())) {
                    // Just read a non-space: start a segment
                    segment.prepend(c);
                    break;
                }
            }

            /*
             * Either s ran out while we were scanning space characters,
             * or we have started a new segment. If s ran out, we'll
             * burn past the next loop. If s didn't run out, the next
             * loop accumulates the segment until the next space (or s
             * runs out).
             */

            // We are currently building a non-space segment
            while (si.hasPrevious()) {
                int c = si.previousInt();
                // Twist: if we've run out of splits, prepend c anyway.
                if (maxsplit > 0 && isPythonSpace(c)) {
                    // Just read a space: end the segment
                    break;
                } else {
                    // Non-space, or last allowed segment
                    segment.prepend(c);
                }
            }

            /*
             * Either s ran out while we were scanning space characters,
             * or we have created a new segment. (It is possible s ran
             * out while we created the segment, but that's ok.)
             */
            if (segment.length() > 0) {
                // We created a segment.
                --maxsplit;
                list.add(wrap(segment.take()));
            }
        }

        // We built the list backwards, so reverse it.
        list.reverse();
        return list;
    }

    /**
     * Implementation of Python {@code str.rsplit}, returning a list of
     * the separated parts. If there are more than {@code maxsplit}
     * occurrences of {@code sep} the last element of the list is the
     * remainder of the original ({@code self}) string.
     *
     * @param s delegate presenting self as code points
     * @param p at occurrences of which {@code s} should be split
     * @param maxsplit limit on the number of splits (if not &lt;=0)
     * @return {@code PyList} of split sections
     */
    private static PyList rsplit(CodepointDelegate s,
            CodepointDelegate p, int maxsplit) {
        /*
         * The structure of rsplit() resembles that of count() in that
         * after a match we keep going. And it resembles rpartition() in
         * that, between matches, we are accumulating characters into a
         * segment buffer, and we are working backwards from the end.
         */

        // -1 means make all possible splits, at most:
        if (maxsplit < 0) { maxsplit = s.length(); }

        // A reverse iterator on p, the string sought.
        CodepointIterator pi = p.iteratorLast();
        int pChar = pi.previousInt(), pLength = p.length();
        CodepointIterator.Mark pMark = pi.mark();
        assert pLength > 0;

        /*
         * Counting backwards in pos we recognise when there can be no
         * further matches.
         */
        int pos = s.length(), firstPos = pLength - 1, sChar;

        // An iterator on s, the string being searched.
        CodepointIterator si = s.iterator(pos);

        // Result built here is a list of split segments
        PyList list = new PyList();
        IntArrayReverseBuilder segment = new IntArrayReverseBuilder();

        while (si.hasPrevious()) {
            if (--pos < firstPos || maxsplit <= 0) {
                /*
                 * We are too close to the start for a match now, or in
                 * our final segment (according to maxsplit==0).
                 * Everything that is left belongs to this segment.
                 */
                segment.prepend(si);
            } else if ((sChar = si.previousInt()) == pChar) {
                /*
                 * s[pos] matched p[-1]: divert into matching the rest
                 * of p. Leave a mark in s where we shall resume if this
                 * is not a full match with p.
                 */
                CodepointIterator.Mark sPos = si.mark();
                int match = 1;
                while (match < pLength) {
                    if (pi.previousInt() != si.previousInt()) { break; }
                    match++;
                }

                if (match == pLength) {
                    /*
                     * We reached the start of p: it's a match. Emit the
                     * segment we have been accumulating, start a new
                     * one, and count a split.
                     */
                    list.add(wrap(segment.take()));
                    --maxsplit;
                    // Catch pos up with si (matches do not overlap).
                    pos = si.nextIndex();
                } else {
                    /*
                     * We stopped on a mismatch: reset si to pos. The
                     * character that matched pChar is part of the
                     * current segment.
                     */
                    sPos.restore();
                    segment.prepend(sChar);
                }
                // In either case, reset pi to p[1].
                pMark.restore();

            } else {
                /*
                 * The character that wasn't part of a match with p is
                 * part of the current segment.
                 */
                segment.prepend(sChar);
            }
        }

        /*
         * Add the segment we were building when s ran out, even if it
         * is empty. Note the list is backwards and we must reverse it.
         */
        list.add(wrap(segment.take()));
        list.reverse();
        return list;
    }

    /**
     * Python {@code str.splitlines([keepends])} returning a list of the
     * lines in the string, breaking at line boundaries. Line breaks are
     * not included in the resulting list unless {@code keepends} is
     * given and true.
     * <p>
     * This method splits on the following line boundaries: LF="\n",
     * VT="\u000b", FF="\f", CR="\r", FS="\u001c", GS="\u001d",
     * RS="\u001e", NEL="\u0085", LSEP="\u2028", PSEP="\u2029" and
     * CR-LF="\r\n". In this last case, the sequence "\r\n" is treated
     * as one line separator.
     *
     * @param keepends the lines in the list retain the separator that
     *     caused the split
     * @return the list of lines
     */
    /*
    @ExposedMethod(defaults = "false", doc = BuiltinDocs.unicode_splitlines_doc)
    */
    PyList splitlines(boolean keepends) {
        return splitlines(delegate, keepends);
    }

    static PyList splitlines(String self, boolean keepends) {
        return splitlines(adapt(self), keepends);
    }

    private static PyList splitlines(CodepointDelegate s,
            boolean keepends) {
        /*
         * The structure of splitlines() resembles that of split() for
         * explicit strings, except that the criteria for recognising
         * the "needle" are implicit.
         */
        // An iterator on s, the string being searched.
        CodepointIterator si = s.iterator(0);

        // Result built here is a list of split segments
        PyList list = new PyList();
        IntArrayBuilder line = new IntArrayBuilder();

        /*
         * We scan the input string looking for characters that mark
         * line endings, and appending to the line buffer as we go. Each
         * detected ending makes a PyUnicode to add t5o list.
         */
        while (si.hasNext()) {

            int c = si.nextInt();

            if (isPythonLineSeparator(c)) {
                // Check for a possible CR-LF combination
                if (c == '\r' && si.hasNext()) {
                    // Might be ... have to peek ahead
                    int c2 = si.nextInt();
                    if (c2 == '\n') {
                        // We're processing CR-LF
                        if (keepends) { line.append(c); }
                        // Leave the \n for the main path to deal with
                        c = c2;
                    } else {
                        // There was no \n following \r: undo the read
                        si.previousInt();
                    }
                }
                // Optionally append the (single) line separator c
                if (keepends) { line.append(c); }
                // Emit the line (and start another)
                list.add(wrap(line.take()));

            } else {
                // c is part of the current line.
                line.append(c);
            }
        }

        /*
         * Add the segment we were building when s ran out, but not if
         * it is empty.
         */
        if (line.length() > 0) { list.add(wrap(line.take())); }

        return list;
    }

    /**
     * As {@link #find(Object, Object, Object)}, but throws
     * {@link ValueError} if the substring is not found.
     *
     * @param sub substring to find.
     * @param start start of slice.
     * @param end end of slice.
     * @return index of {@code sub} in this object or -1 if not found.
     * @throws ValueError if {@code sub} is not found
     */
    /*
    @ExposedMethod(defaults = {"null", "null"}, doc = BuiltinDocs.unicode_index_doc)
    */
    int index(Object sub, Object start, Object end) throws ValueError {
        return checkIndexReturn(find(delegate, sub, start, end));
    }

    static int index(String self, Object sub, Object start,
            Object end) {
        return checkIndexReturn(find(adapt(self), sub, start, end));
    }

    /**
     * As {@link #rfind(Object, Object, Object)}, but throws
     * {@link ValueError} if the substring is not found.
     *
     * @param sub substring to find.
     * @param start start of slice.
     * @param end end of slice.
     * @return index of {@code sub} in this object or -1 if not found.
     * @throws ValueError if {@code sub} is not found
     */
    /*
    @ExposedMethod(defaults = {"null", "null"}, doc = BuiltinDocs.unicode_index_doc)
    */
    int rindex(Object sub, Object start, Object end) throws ValueError {
        return checkIndexReturn(rfind(delegate, sub, start, end));
    }

    static int rindex(String self, Object sub, Object start,
            Object end) {
        return checkIndexReturn(rfind(adapt(self), sub, start, end));
    }

    /**
     * Return the number of non-overlapping occurrences of substring
     * {@code sub} in the range {@code [start:end]}. Optional arguments
     * {@code start} and {@code end} are interpreted as in slice
     * notation.
     *
     * @param sub substring to find.
     * @param start start of slice.
     * @param end end of slice.
     * @return count of occurrences.
     */
    /*
    @ExposedMethod(defaults = {"null", "null"}, doc = BuiltinDocs.unicode_count_doc)
    */
    int count(Object sub, Object start, Object end) {
        return count(delegate, sub, start, end);
    }

    static int count(String self, Object sub, Object start,
            Object end) {
        return count(adapt(self), sub, start, end);
    }

    private static int count(CodepointDelegate s, Object sub,
            Object start, Object end) {
        CodepointDelegate p = adaptSub("count", sub);
        PySlice.Indices slice = getSliceIndices(s, start, end);
        if (p.length() == 0)
            return slice.slicelength + 1;
        else
            return count(s, p, slice);
    }

    /**
     * The inner implementation of {@code str.count}, returning the
     * number of occurrences of a substring. It accepts slice-like
     * arguments, which may be {@code None} or end-relative (negative).
     *
     * @param sub substring to find.
     * @param startObj start of slice.
     * @param endObj end of slice.
     * @return count of occurrences
     */
    private static int count(CodepointDelegate s, CodepointDelegate p,
            PySlice.Indices slice) {
        /*
         * count() uses the same pattern as find(), with the difference
         * that it keeps going rather than returning on the first match.
         */
        // An iterator on p, the string sought.
        CodepointIterator pi = p.iterator(0);
        int pChar = pi.nextInt(), pLength = p.length();
        CodepointIterator.Mark pMark = pi.mark();
        assert pLength > 0;

        // Counting in pos avoids hasNext() calls.
        int pos = slice.start, lastPos = slice.stop - pLength;

        // An iterator on s[start:end], the string being searched.
        CodepointIterator si = s.iterator(pos, slice.start, slice.stop);
        int count = 0;

        while (pos++ <= lastPos) {
            if (si.nextInt() == pChar) {
                /*
                 * s[pos] matched p[0]: divert into matching the rest of
                 * p. Leave a mark in s where we shall resume if this is
                 * not a full match with p.
                 */
                CodepointIterator.Mark sPos = si.mark();
                int match = 1;
                while (match < pLength) {
                    if (pi.nextInt() != si.nextInt()) { break; }
                    match++;
                }
                if (match == pLength) {
                    // We reached the end of p: it's a match.
                    count++;
                    // Catch pos up with si (matches do not overlap).
                    pos = si.nextIndex();
                } else {
                    // We stopped on a mismatch: reset si to pos.
                    sPos.restore();
                }
                // In either case, reset pi to p[1].
                pMark.restore();
            }
        }
        return count;
    }

    /**
     * Python {@code str.replace(old, new[, count])}, returning a copy
     * of the string with all occurrences of substring {@code old}
     * replaced by {@code rep}. If argument {@code count} is
     * nonnegative, only the first {@code count} occurrences are
     * replaced.
     *
     * @param old to replace where found.
     * @param rep replacement text.
     * @param count maximum number of replacements to make, or -1
     *     meaning all of them.
     * @return {@code self} string after replacements.
     */
    @PythonMethod
    Object replace(Object old, @Name("new") Object rep, @Default("-1") int count) {
        return replace(delegate, old, rep, count);
    }

    @PythonMethod(primary = false)
    static Object replace(String self, Object old, Object rep, int count) {
        return replace(adapt(self), old, rep, count);
    }

    private static Object replace(CodepointDelegate s, Object old, Object rep, int count) {
        // Convert arguments to their delegates or error
        CodepointDelegate p = adaptSub("replace", old);
        CodepointDelegate n = adaptRep("replace", rep);
        if (p.length() == 0) {
            return replace(s, n, count);
        } else {
            return replace(s, p, n, count);
        }
    }

    /**
     * Implementation of Python {@code str.replace} in the case where
     * the substring to find has zero length. This must result in the
     * insertion of the replacement string at the start if the result
     * and after every character copied from s, up to the limit imposed
     * by {@code count}. For example {@code 'hello'.replace('', '-')}
     * returns {@code '-h-e-l-l-o-'}. This is {@code N+1} replacements,
     * where {@code N = s.length()}, or as limited by {@code count}.
     *
     * @param s delegate presenting self as code points
     * @param r delegate representing the replacement string
     * @param count limit on the number of replacements
     * @return string interleaved with the replacement
     */
    private static Object replace(CodepointDelegate s,
            CodepointDelegate r, int count) {

        // -1 means make all replacements, which is exactly:
        if (count < 0) {
            count = s.length() + 1;
        } else if (count == 0) {
            // Zero replacements: short-cut return the original
            return s.principal();
        }

        CodepointIterator si = s.iterator(0);

        // The result will be this size exactly
        // 'hello'.replace('', '-', 3) == '-h-e-llo'
        IntArrayBuilder result =
                new IntArrayBuilder(s.length() + r.length() * count);

        // Start with the a copy of the replacement
        result.append(r);

        // Put another copy of after each of count-1 characters of s
        for (int i = 1; i < count; i++) {
            assert si.hasNext();
            result.append(si.nextInt()).append(r);
        }

        // Now copy any remaining characters of s
        result.append(si);
        return wrap(result.take());
    }

    /**
     * Implementation of Python {@code str.replace} in the case where
     * the substring to find has non-zero length, up to the limit
     * imposed by {@code count}.
     *
     * @param s delegate presenting self as code points
     * @param p delegate representing the string to replace
     * @param r delegate representing the replacement string
     * @param count limit on the number of replacements
     * @return string with the replacements
     */
    private static Object replace(CodepointDelegate s,
            CodepointDelegate p, CodepointDelegate r, int count) {

        // -1 means make all replacements, but cannot exceed:
        if (count < 0) {
            count = s.length() + 1;
        } else if (count == 0) {
            // Zero replacements: short-cut return the original
            return s.principal();
        }

        /*
         * The structure of replace is a lot like that of split(), in
         * that we iterate over s, copying as we go. The difference is
         * the action we take upon encountering and instance of the
         * "needle" string, which here is to emit the replacement into
         * the result, rather than start a new segment.
         */

        // An iterator on p, the string sought.
        CodepointIterator pi = p.iterator(0);
        int pChar = pi.nextInt(), pLength = p.length();
        CodepointIterator.Mark pMark = pi.mark();
        assert pLength > 0;

        // An iterator on r, the replacement string.
        CodepointIterator ri = r.iterator(0);
        CodepointIterator.Mark rMark = ri.mark();

        // Counting in pos avoids hasNext() calls.
        int pos = 0, lastPos = s.length() - pLength, sChar;

        // An iterator on s, the string being searched.
        CodepointIterator si = s.iterator(pos);

        // Result built here
        IntArrayBuilder result = new IntArrayBuilder();

        while (si.hasNext()) {

            if (pos++ > lastPos || count <= 0) {
                /*
                 * We are too close to the end for a match now, or we
                 * have run out of permission to make (according to
                 * count==0). Everything that is left may be added to
                 * the result.
                 */
                result.append(si);

            } else if ((sChar = si.nextInt()) == pChar) {
                /*
                 * s[pos] matched p[0]: divert into matching the rest of
                 * p. Leave a mark in s where we shall resume if this is
                 * not a full match with p.
                 */
                CodepointIterator.Mark sPos = si.mark();
                int match = 1;
                while (match < pLength) {
                    if (pi.nextInt() != si.nextInt()) { break; }
                    match++;
                }

                if (match == pLength) {
                    /*
                     * We reached the end of p: it's a match. Emit the
                     * replacement string to the result and lose a life.
                     */
                    result.append(ri);
                    rMark.restore();
                    --count;
                    // Catch pos up with si (matches do not overlap).
                    pos = si.nextIndex();
                } else {
                    /*
                     * We stopped on a mismatch: reset si to pos. The
                     * character that matched pChar is part of the
                     * result.
                     */
                    sPos.restore();
                    result.append(sChar);
                }
                // In either case, reset pi to p[1].
                pMark.restore();

            } else {
                /*
                 * The character that wasn't part of a match with p is
                 * part of the result.
                 */
                result.append(sChar);
            }
        }

        return wrap(result.take());
    }

    // @formatter:off

    // Transformation methods -----------------------------------------

    /*
     * We group here methods that are simple transformation functions of
     * the string, based on tests of character properties, for example
     * str.strip() and str.title().
     */

    /*
    @ExposedMethod(doc = BuiltinDocs.unicode_lower_doc)
    */
    PyUnicode lower() { return mapChars(Character::toLowerCase); }

    static String lower(String self) {
        return mapChars(self, Character::toLowerCase);
    }

    @PythonMethod
    PyUnicode upper() { return mapChars(Character::toUpperCase); }

    @PythonMethod(primary = false)
    static String upper(String self) {
        return mapChars(self, Character::toUpperCase);
    }

    /*
    @ExposedMethod(doc = BuiltinDocs.unicode_title_doc)
    */
    PyUnicode title() { return title(delegate); }

    static PyUnicode title(String self) { return title(adapt(self)); }

    private static PyUnicode title(PySequence.OfInt s) {
        IntArrayBuilder buffer = new IntArrayBuilder(s.length());
        boolean previousCased = false;
        for (int c : s) {
            if (previousCased) {
                buffer.append(Character.toLowerCase(c));
            } else {
                buffer.append(Character.toTitleCase(c));
            }
            previousCased =
                    Character.isLowerCase(c) || Character.isUpperCase(c)
                            || Character.isTitleCase(c);
        }
        return wrap(buffer.take());
    }

    /*
    @ExposedMethod(doc = BuiltinDocs.unicode_swapcase_doc)
    */
    PyUnicode swapcase() { return mapChars(PyUnicode::swapcase); }

    static String swapcase(String self) {
        return mapChars(self, PyUnicode::swapcase);
    }

    private static int swapcase(int c) {
        if (Character.isUpperCase(c)) {
            return Character.toLowerCase(c);
        } else if (Character.isLowerCase(c)) {
            return Character.toUpperCase(c);
        } else {
            return c;
        }
    }

    /*
    @ExposedMethod(defaults = "null", doc = BuiltinDocs.unicode_ljust_doc)
    */
    Object ljust(int width, Object fillchar) {
        return pad(false, delegate, true, width,
                adaptFill("ljust", fillchar));
    }

    static Object ljust(String self, int width, Object fillchar) {
        return pad(false, adapt(self), true, width,
                adaptFill("ljust", fillchar));
    }

    /*
    @ExposedMethod(defaults = "null", doc = BuiltinDocs.unicode__doc)
    */
    Object rjust(int width, Object fillchar) {
        return pad(true, delegate, false, width,
                adaptFill("rjust", fillchar));
    }

    static Object rjust(String self, int width, Object fillchar) {
        return pad(true, adapt(self), false, width,
                adaptFill("rjust", fillchar));
    }

    /*
    @ExposedMethod(defaults = "null", doc = BuiltinDocs.unicode_rjust_doc)
    */
    Object center(int width, Object fillchar) {
        return pad(true, delegate, true, width,
                adaptFill("center", fillchar));
    }

    static Object center(String self, int width, Object fillchar) {
        return pad(true, adapt(self), true, width,
                adaptFill("center", fillchar));
    }

    /**
     * Common code for {@link #ljust(int, Object) ljust},
     * {@link #rjust(int, Object) rjust} and {@link #center(int, Object)
     * center}.
     *
     * @param left whether to pad at the left
     * @param s the {@code self} string
     * @param right whether to pad at the right
     * @param width the minimum width to attain
     * @param fill the code point value to use as the fill
     * @return the padded string (or {@code s.principal()})
     */
    private static Object pad(boolean left, CodepointDelegate s,
            boolean right, int width, int fill) {
        // Work out how much (or whether) to pad at the left and right.
        int L = s.length(), pad = Math.max(width, L) - L;
        if (pad == 0) { return s.principal(); }

        // It suits us to assume all right padding to begin with.
        int leftPad = 0, rightPad = pad;
        if (left) {
            if (!right) {
                // It is all on the left
                leftPad = pad;
                rightPad = 0;
            } else {
                // But sometimes you have to be Dutch
                leftPad = pad / 2 + (pad & width & 1);
                rightPad = width - leftPad;
            }
        }

        // Now, use a builder to create the result
        IntArrayBuilder buf = new IntArrayBuilder(width);

        for (int i = 0; i < leftPad; i++) { buf.append(fill); }
        buf.append(s);
        for (int i = 0; i < rightPad; i++) { buf.append(fill); }
        return wrap(buf.take());
    }

    /*
    @ExposedMethod(doc = BuiltinDocs.unicode_zfill_doc)
    */
    Object zfill(int width) {
        return zfill(delegate, width);
    }

    static Object zfill(String self, int width) {
        return zfill(adapt(self), width);
    }

    /**
     * Inner implementation of {@link #zfill(int) zfill}
     *
     * @param s the {@code self} string
     * @param width the achieve by inserting zeros
     * @return the filled string
     */
    private static Object zfill(CodepointDelegate s, int width) {
        // Work out how much to pad.
        int L = s.length(), pad = Math.max(width, L) - L;
        if (pad == 0) { return s.principal(); }

        // Now, use a builder to create the result of the padded width
        IntArrayBuilder buf = new IntArrayBuilder(width);
        CodepointIterator si = s.iterator(0);

        // Possible sign goes first
        if (si.hasNext()) {
            int c = si.nextInt();
            if (c == '+' || c == '-') {
                buf.append(c);
            } else {
                si.previousInt();
            }
        }

        // Now the computed number of zeros
        for (int i = 0; i < pad; i++) { buf.append('0'); }
        buf.append(si);
        return wrap(buf.take());
    }

    /*
    @ExposedMethod(defaults = "8", doc = BuiltinDocs.str_expandtabs_doc)
    */
    Object expandtabs(int tabsize) {
        return expandtabs(delegate, tabsize);
    }

    static Object expandtabs(String self, int tabsize) {
        return expandtabs( adapt(self), tabsize);
    }

    /**
     * Inner implementation of {@link #expandtabs() expandtabs}
     *
     * @param s the {@code self} string
     * @param tabsize number of spaces to tab to
     * @return tab-expanded string
     */
    private static Object expandtabs(CodepointDelegate s, int tabsize) {
        // Build the result in buf. It can be multi-line.
        IntArrayBuilder buf = new IntArrayBuilder(s.length());
        // Iterate through s, keeping track of position on line.
        CodepointIterator si = s.iterator(0);
        int pos = 0;
        while (si.hasNext()) {
            int c = si.nextInt();
            if (c == '\t') {
                int spaces = tabsize - pos % tabsize;
                while (spaces-- > 0) { buf.append(' '); }
                pos += spaces;
            } else {
                if (c == '\n' || c == '\r') { pos = -1; }
                buf.append(c);
                pos++;
            }
        }
        return wrap(buf.take());
    }

    /*
    @ExposedMethod(doc = BuiltinDocs.unicode_capitalize_doc)
    */
    Object capitalize() { return capitalize(delegate); }

    static Object capitalize(String self) {
        return capitalize(adapt(self));
    }

    /**
     * Inner implementation of {@link #capitalize() capitalize}
     *
     * @param s the {@code self} string
     * @return capitalised string
     */
    private static Object capitalize(CodepointDelegate s) {
        // Iterate through s
        CodepointIterator si = s.iterator(0);
        if (si.hasNext()) {
            // Build the result in buf.
            IntArrayBuilder buf = new IntArrayBuilder(s.length());
            // Uppercase the first character
            buf.append(Character.toUpperCase(si.nextInt()));
            // Lowercase the rest
            while (si.hasNext()) {
                buf.append(Character.toLowerCase(si.nextInt()));
            }
            return wrap(buf.take());
        } else {
            // String is empty
            return "";
        }
    }

    // @formatter:off

    /*
    @ExposedMethod(doc = BuiltinDocs.str_join_doc)
    */
    Object join(Object iterable) throws TypeError, Throwable {
        return join(delegate, iterable);
    }

    static Object join(String self, Object iterable)
            throws TypeError, Throwable {
        return join(adapt(self), iterable);
    }

    /**
     * Inner implementation of {@link #join() join}.
     *
     * @param s the {@code self} string (separator)
     * @param iterable of strings
     * @return capitalised string
     * @throws TypeError if {@code iterable} isn't
     * @throws Throwable from errors iterating {@code iterable}
     */
    private static Object join(CodepointDelegate s, Object iterable)
            throws TypeError, Throwable {
        /*
         * The argument is supposed to be a Python iterable: present it
         * as a Java List.
         */
        List<Object> parts = PySequence.fastList(iterable,
                () -> Abstract.argumentTypeError("join", "", "iterable",
                        iterable));

        /*
         * It is safe assume L is constant since either seq is a
         * well-behaved built-in, or we made a copy.
         */
        final int L = parts.size();

        // If empty sequence, return ""
        if (L == 0) {
            return "";
        } else if (L == 1) {
            // One-element sequence: return that element (if a str).
            Object item = parts.get(0);
            if (TYPE.checkExact(item)) { return item; }
        }

        /*
         * There are at least two parts to join, or one and it isn't a
         * str exactly. Do a pre-pass to figure out the total amount of
         * space we'll need, and check that every element is str-like.
         */
        int sepLen = s.length();
        // Start with the length contributed for by L-1 separators
        long size = (L - 1) * sepLen;

        for (int i = 0; i < L; i++) {

            // Accumulate the length of the item according to type
            Object item = parts.get(i);
            if (item instanceof PyUnicode) {
                size += ((PyUnicode)item).__len__();
            } else if (item instanceof String) {
                /*
                 * If non-BMP, this will over-estimate. We assume this
                 * is preferable to counting characters properly.
                 */
                size += ((String)item).length();
            } else {
                // If neither, then it's not a str
                throw joinArgumentTypeError(item, i);
            }

            if (size > Integer.MAX_VALUE) {
                throw new OverflowError(
                        "join() result is too long for a Python string");
            }
        }

        // Build the result here
        IntArrayBuilder buf = new IntArrayBuilder((int)size);

        // Concatenate the parts and separators
        for (int i = 0; i < L; i++) {
            // Separator
            if (i != 0) { buf.append(s); }
            // item from the iterable
            Object item = parts.get(i);
            try {
                buf.append(adapt(item));
            } catch (NoConversion e) {
                // This can't really happen here, given checks above
                throw joinArgumentTypeError(item, i);
            }
        }

        return wrap(buf.take());
    }

    private static TypeError joinArgumentTypeError(Object item, int i) {
        return new TypeError(
                "sequence item %d: expected str, %.80s found", i,
                PyType.of(item).getName());
    }

    // Doc copied from PyString
    /**
     * Equivalent to the Python {@code str.startswith} method, testing
     * whether a string starts with a specified prefix, where a
     * sub-range is specified by {@code [start:end]}. Arguments
     * {@code start} and {@code end} are interpreted as in slice
     * notation, with null or {@link Py#None} representing "missing".
     * {@code prefix} can also be a tuple of prefixes to look for.
     *
     * @param prefix string to check for (or a {@code PyTuple} of them).
     * @param start start of slice.
     * @param end end of slice.
     * @return {@code true} if this string slice starts with a specified
     *     prefix, otherwise {@code false}.
     */
    /*
    @ExposedMethod(defaults = {"null", "null"}, doc = BuiltinDocs.unicode_startswith_doc)
    */
    Object startswith(Object prefix, Object start, Object end) {
        return startswith(delegate, prefix, start, end);
    }

    static Object startswith(String self, Object prefix, Object start,
            Object end) {
        return startswith(adapt(self), prefix, start, end);
    }

    private static boolean startswith(CodepointDelegate s,
            Object prefixObj, Object start, Object end) {

        PySlice.Indices slice = getSliceIndices(s, start, end);

        if (prefixObj instanceof PyTuple) {
            /*
             * Loop will return true if this slice starts with any
             * prefix in the tuple
             */
            for (Object prefix : (PyTuple)prefixObj) {
                // It ought to be a str.
                CodepointDelegate p = adaptSub("startswith", prefix);
                if (startswith(s, p, slice)) { return true; }
            }
            // None matched
            return false;
        } else {
            // It ought to be a str.
            CodepointDelegate p = adaptSub("startswith", prefixObj);
            return startswith(s, p, slice);
        }
    }

    private static boolean startswith(CodepointDelegate s,
            CodepointDelegate p, PySlice.Indices slice) {
        // If p is too long, it can't start s
        if (p.length() > s.length()) { return false; }
        CodepointIterator si = s.iterator(0, slice.start, slice.stop);
        CodepointIterator pi = p.iterator(0);
        // We know that p is no longer than s so only count in p
        while (pi.hasNext()) {
            if (pi.nextInt() != si.nextInt()) { return false; }
        }
        return true;
    }

    // Doc copied from PyString
    /**
     * Equivalent to the Python {@code str.endswith} method, testing
     * whether a string ends with a specified suffix, where a sub-range
     * is specified by {@code [start:end]}. Arguments {@code start} and
     * {@code end} are interpreted as in slice notation, with null or
     * {@link Py#None} representing "missing". {@code suffix} can also
     * be a tuple of suffixes to look for.
     *
     * @param suffix string to check for (or a {@code PyTuple} of them).
     * @param start start of slice.
     * @param end end of slice.
     * @return {@code true} if this string slice ends with a specified
     *     suffix, otherwise {@code false}.
     */
    /*
    @ExposedMethod(defaults = {"null", "null"}, doc = BuiltinDocs.unicode_endswith_doc)
    */
    Object endswith(Object suffix, Object start, Object end) {
        return endswith(delegate, suffix, start, end);
    }

    static Object endswith(String self, Object suffix, Object start,
            Object end) {
        return endswith(adapt(self), suffix, start, end);
    }

    private static boolean endswith(CodepointDelegate s,
            Object suffixObj, Object start, Object end) {

        PySlice.Indices slice = getSliceIndices(s, start, end);

        if (suffixObj instanceof PyTuple) {
            /*
             * Loop will return true if this slice ends with any
             * prefix in the tuple
             */
            for (Object prefix : (PyTuple)suffixObj) {
                // It ought to be a str.
                CodepointDelegate p = adaptSub("endswith", prefix);
                if (endswith(s, p, slice)) { return true; }
            }
            // None matched
            return false;
        } else {
            // It ought to be a str.
            CodepointDelegate p = adaptSub("endswith", suffixObj);
            return endswith(s, p, slice);
        }
    }

    private static boolean endswith(CodepointDelegate s,
            CodepointDelegate p, PySlice.Indices slice) {
        // If p is too long, it can't end s
        if (p.length() > s.length()) { return false; }
        CodepointIterator si = s.iterator(slice.stop, slice.start, slice.stop);
        CodepointIterator pi = p.iteratorLast();
        // We know that p is no longer than s so only count in p
        while (pi.hasPrevious()) {
            if (pi.previousInt() != si.previousInt()) { return false; }
        }
        return true;
    }

    /*
    @ExposedMethod(doc = BuiltinDocs.unicode_translate_doc)
    */
    final Object translate(Object table) {
        return translateCharmap(this, "ignore", table);
    }

    // Copied from PyString
    /**
     * Helper common to the Python and Java API implementing {@code str.translate} returning a
     * copy of this string where all characters  occurring in the argument
     * {@code deletechars} are removed (if it is not {@code null}), and the remaining
     * characters have been mapped through the translation {@code table}, which must be
     * equivalent to a string of length 256 (if it is not {@code null}).
     *
     * @param table of character  translations (or {@code null})
     * @param deletechars set of characters to remove (or {@code null})
     * @return transformed  string
     */
    private final String _translate(String table, String deletechars) {

        if (table != null && table.length() != 256) {
            throw new ValueError("translation table must be 256 characters long");
        }

        StringBuilder buf = new StringBuilder(asString().length());

        for (int i = 0; i < asString().length(); i++) {
            char c = asString().charAt(i);
            if (deletechars != null && deletechars.indexOf(c) >= 0) {
                continue;
            }
            if (table == null) {
                buf.append(c);
            } else {
                try {
                    buf.append(table.charAt(c));
                } catch (IndexOutOfBoundsException e) {
                    throw new TypeError("translate() only works for 8-bit character strings");
                }
            }
        }
        return buf.toString();
    }

    // Predicate methods ----------------------------------------------

    /*
     * We group here methods that are boolean functions of the string,
     * based on tests of character properties, for example
     * str.isascii(). They have a common pattern.
     */

    // @formatter:off
    /*
    @ExposedMethod(doc = BuiltinDocs.unicode_islower_doc)
    */
    boolean islower() { return islower(delegate); }

    static boolean islower(String s) { return islower(adapt(s)); }

    private static boolean islower(PySequence.OfInt s) {
        boolean cased = false;
        for (int codepoint : s) {;
            if (Character.isUpperCase(codepoint) || Character.isTitleCase(codepoint)) {
                return false;
            } else if (!cased && Character.isLowerCase(codepoint)) {
                cased = true;
            }
        }
        return cased;
    }

    /*
    @ExposedMethod(doc = BuiltinDocs.unicode_isupper_doc)
    */
    final boolean isupper() { return isupper(delegate); }

    static boolean isupper(String s) { return isupper(adapt(s)); }

    private static boolean isupper(PySequence.OfInt s) {
        boolean cased = false;
        for (int codepoint : s) {;
            if (Character.isLowerCase(codepoint) || Character.isTitleCase(codepoint)) {
                return false;
            } else if (!cased && Character.isUpperCase(codepoint)) {
                cased = true;
            }
        }
        return cased;
    }

    /*
    @ExposedMethod(doc = BuiltinDocs.unicode_isalpha_doc)
    */
    final boolean isalpha() { return isalpha(delegate); }

    static boolean isalpha(String s) { return isalpha(adapt(s)); }

    private static boolean isalpha(PySequence.OfInt s) {
        if (s.length() == 0) {
            return false;
        }
        for (int codepoint : s) {
            if (!Character.isLetter(codepoint)) {
                return false;
            }
        }
        return true;
    }

    /*
    @ExposedMethod(doc = BuiltinDocs.unicode_isalnum_doc)
    */
    final boolean isalnum() { return isalnum(delegate); }

    static boolean isalnum(String s) { return isalnum(adapt(s)); }

    private static boolean isalnum(PySequence.OfInt s) {
        if (s.length() == 0) {
            return false;
        }
        for (int codepoint : s) {;
            if (!(Character.isLetterOrDigit(codepoint) || //
                    Character.getType(codepoint) == Character.LETTER_NUMBER)) {
                return false;
            }
        }
        return true;
    }

    @PythonMethod(primary = false)
    public boolean isascii() {
        for (int c : value) { if (c >>> 7 != 0) { return false; } }
        return true;
    }

    @PythonMethod
    public static boolean isascii(String self) {
        // We can test chars since any surrogate will fail.
        return self.chars().dropWhile(c -> c >>> 7 == 0)
                .findFirst().isEmpty();
    }

    /*
    @ExposedMethod(doc = BuiltinDocs.unicode_isdecimal_doc)
    */
    final boolean isdecimal() { return isdecimal(delegate); }

    static boolean isdecimal(String s) { return isdecimal(adapt(s)); }

    private static boolean isdecimal(PySequence.OfInt s) {
        if (s.length() == 0) {
            return false;
        }
        for (int codepoint : s) {;
            if (Character.getType(codepoint) != Character.DECIMAL_DIGIT_NUMBER) {
                return false;
            }
        }
        return true;
    }

    /*
    @ExposedMethod(doc = BuiltinDocs.unicode_isdigit_doc)
    */
    final boolean isdigit() { return isdigit(delegate); }

    static boolean isdigit(String s) { return isdigit(adapt(s)); }

    private static boolean isdigit(PySequence.OfInt s) {
        if (s.length() == 0) {
            return false;
        }
        for (int codepoint : s) {;
            if (!Character.isDigit(codepoint)) {
                return false;
            }
        }
        return true;
    }

    /*
    @ExposedMethod(doc = BuiltinDocs.unicode_isnumeric_doc)
    */
    final boolean isnumeric() { return isnumeric(delegate); }

    static boolean isnumeric(String s) { return isnumeric(adapt(s)); }

    private static boolean isnumeric(PySequence.OfInt s) {
        if (s.length() == 0) {
            return false;
        }
        for (int codepoint : s) {;
            int type = Character.getType(codepoint);
            if (type != Character.DECIMAL_DIGIT_NUMBER && type != Character.LETTER_NUMBER
                    && type != Character.OTHER_NUMBER) {
                return false;
            }
        }
        return true;
    }

    /*
    @ExposedMethod(doc = BuiltinDocs.unicode_istitle_doc)
    */
    final boolean istitle() { return istitle(delegate); }

    static boolean istitle(String s) { return istitle(adapt(s)); }

    private static boolean istitle(PySequence.OfInt s) {
        if (s.length() == 0) {
            return false;
        }
        boolean cased = false;
        boolean previous_is_cased = false;
        for (int codepoint : s) {;
            if (Character.isUpperCase(codepoint) || Character.isTitleCase(codepoint)) {
                if (previous_is_cased) {
                    return false;
                }
                previous_is_cased = true;
                cased = true;
            } else if (Character.isLowerCase(codepoint)) {
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

    /*
    @ExposedMethod(doc = BuiltinDocs.unicode_isspace_doc)
    */
    final boolean isspace() { return isspace(delegate); }

    static boolean isspace(String s) { return isspace(adapt(s)); }

    private static boolean isspace(PySequence.OfInt s) {
        if (s.length() == 0) {
            return false;
        }
        for (int codepoint : s) {;
            if (!isPythonSpace(codepoint)) {
                return false;
            }
        }
        return true;
    }

    /*
    @ExposedMethod(doc = BuiltinDocs.unicode_encode_doc)
    */
    Object encode(Object[] args, String[] keywords) {
        return encode(delegate, args, keywords);
    }

    Object encode(String self, Object[] args, String[] keywords) {
        return encode(adapt(self), args, keywords);
    }

    Object encode(CodepointDelegate s, Object[] args, String[] keywords) {
        throw new MissingFeature("bytes, codecs, encoding ...");
    }

    /*
    @ExposedMethod(doc = BuiltinDocs.unicode___getnewargs___doc)
    */
    private PyTuple __getnewargs__() {
        /* This may be a sub-class but it should still be safe to share the value array. (I think.) */
        return new PyTuple(wrap(value));
    }

    private static PyTuple __getnewargs__(String self) {
        return new PyTuple(self);
    }

    /*
    @ExposedMethod(doc = BuiltinDocs.unicode___format___doc)
    */
    static final Object __format__(Object self, Object formatSpec) {

        String stringFormatSpec = coerceToString(formatSpec,
                () -> Abstract.argumentTypeError("__format__",
                        "specification", "str", formatSpec));

        try {
            // Parse the specification
            Spec spec = InternalFormat.fromText(stringFormatSpec);

            // Get a formatter for the specification
            TextFormatter f = new StrFormatter(spec);

            /*
             * Format, pad and return a result according to as the
             * specification argument.
             */
            return f.format(self).pad().getResult();

        } catch (FormatOverflow fe) {
            throw new OverflowError(fe.getMessage());
        } catch (FormatError fe) {
            throw new ValueError(fe.getMessage());
        } catch (NoConversion e) {
            throw Abstract.impossibleArgumentError(TYPE.name, self);
        }
    }

    /**
     * Implementation of {@code _string.formatter_parser}. Return an
     * iterable that contains {@code tuple}s of the form:
     * {@code (literal_text, field_name, format_spec, conversion)}.
     * <p>
     * For example, the iterator <code>formatter_parser("x={2:6.3f}
     * y={y!r:&gt;7s}.")</code> yields successively<pre>
     * ('x=', '2', '6.3f', None)
     * (' y=', 'y', '&gt;7s', 'r')
     * ('.', None, None, None)
     * </pre> {@code literal_text} can be zero length, and
     * {@code field_name} can be {@code None}, in which case there's no
     * object to format and output. If {@code field_name} is not
     * {@code None}, it is looked up, formatted with {@code format_spec}
     * and {@code conversion} and then used.
     *
     * @param formatString to parse
     * @return an iterator of format {@code tuple}s
     */
    // Compare CPython formatter_parser in unicode_formatter.h
    /*
    @ExposedMethod(doc = BuiltinDocs.unicode__formatter_parser_doc)
    */
    // XXX belongs to the _string module, but where does that belong?
    static Object formatter_parser(Object formatString) {
        return new MarkupIterator(asString(formatString));
    }

    /**
     * Implementation of {@code _string.formatter_field_name_split}.
     *
     * @param fieldName to split into components
     * @return a tuple of the first field name component and the rest
     */
    // Compare CPython formatter_field_name_split in unicode_formatter.h
    /*
    @ExposedMethod(doc = BuiltinDocs.unicode__formatter_field_name_split_doc)
    */
    // XXX belongs to the _string module, but where does that belong?
    static PyTuple formatter_field_name_split(Object fieldName) {
        FieldNameIterator iterator = new FieldNameIterator(asString(fieldName));
        return new PyTuple(iterator.head(), iterator);
    }

    /*
    @ExposedMethod(doc = BuiltinDocs.unicode_format_doc)
    */
    final Object format(Object[] args, String[] keywords) throws TypeError, Throwable {
        try {
            return buildFormattedString(args, keywords, null, null);
        } catch (IllegalArgumentException e) {
            throw new ValueError(e.getMessage());
        }
    }

    // @formatter:off

    // Codec support -------------------------------------------------

    // Copied from PyString
    private static char[] hexdigit = "0123456789abcdef".toCharArray();

    // Copied from PyString
    public static String encode_UnicodeEscape(String str, boolean use_quotes) {
        char quote = use_quotes ? '?' : 0;
        return encode_UnicodeEscape(str, quote);
    }

    // Copied from PyString
    /**
     * The inner logic of the string __repr__ producing an ASCII representation of the target
     * string, optionally in quotations. The caller can determine whether the returned string will
     * be wrapped in quotation marks, and whether Python rules are used to choose them through
     * {@code quote}.
     *
     * @param str to process
     * @param quote '"' or '\'' use that, '?' = let Python choose, 0 or anything = no quotes
     * @return encoded string (possibly the same string if unchanged)
     */
    static String encode_UnicodeEscape(String str, char quote) {
        /*
         * XXX consider re-work. The quotation logic is useful for repr,
         * but not escaping all the non-ascii characters: modern Python
         * does not assume the console is acsii. OTOH we do need a couple
         * of unicode escape encodings elsewhere. Calls to this in Jython 2
         * are essentially PyUnicode.__repr__ (filenames mostly).
         */

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

    // Copied from PyString
    private static ucnhashAPI pucnHash = null;

    // Copied from PyString
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
                        // class org.python.modules.ucnhash
//                        Object mod = imp.importName("ucnhash", true);
//                        mod = mod.__call__();
//                        pucnHash = (ucnhashAPI) mod.__tojava__(Object.class);
                        if (pucnHash.getCchMax() < 0) {
                            throw new UnicodeError("Unicode names not loaded");
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

    // Copied from PyString
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

    // Copied from PyString
    /* pass in an int since this can be a UCS-4 character */
    private static boolean storeUnicodeCharacter(int value, StringBuilder partialDecode) {
        if (value < 0 || (value >= 0xD800 && value <= 0xDFFF)) {
            return false;
        } else if (value <= Character.MAX_CODE_POINT) {
            partialDecode.appendCodePoint(value);
            return true;
        }
        return false;
    }

    // Java-only API --------------------------------------------------

    // @formatter:on

    private static final int HIGH_SURROGATE_OFFSET =
            Character.MIN_HIGH_SURROGATE - (Character.MIN_SUPPLEMENTARY_CODE_POINT >>> 10);

    /**
     * The code points of this PyUnicode as a {@link PySequence.OfInt}.
     * This interface will allow the code points to be streamed or
     * iterated (but not modified, obviously).
     *
     * @return the code point sequence
     */
    public PySequence.OfInt asSequence() { return delegate; }

    /**
     * The hash of a {@link PyUnicode} is the same as that of a Java
     * {@code String} equal to it. This is so that a given Python
     * {@code str} may be found as a match in hashed data structures,
     * whichever representation is used for the key or query.
     */
    @Override
    public int hashCode() throws PyException { return PyDict.pythonHash(this); }

    /**
     * Compare for equality with another Python {@code str}, or a
     * {@link PyDict.Key} containing a {@code str}. If the other object
     * is not a {@code str}, or a {@code Key} containing a {@code str},
     * return {@code false}. If it is such an object, compare for
     * equality of the code points.
     */
    @Override
    public boolean equals(Object obj) { return PyDict.pythonEquals(this, obj); }

    /**
     * Create a {@code str} from a format and arguments. Note Java
     * {@code String.format} semantics are applied, not the CPython
     * ones.
     *
     * @param fmt format string (Java semantics)
     * @param args arguments
     * @return formatted string
     */
    @Deprecated // XXX possibly want a version with Python semantics
    static PyUnicode fromFormat(String fmt, Object... args) {
        return new PyUnicode(TYPE, String.format(fmt, args));
    }

    /**
     * Represent the `str` value in readable form, escaping lone
     * surrogates. The {@code PyUnicode.toString()} is intended to
     * produce a readable output, not always the closest Java
     * {@code String}, for which {@link #asString()} is a better choice.
     */
    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        for (int c : value) {
            if (c >= Character.MIN_SURROGATE && c <= Character.MAX_SURROGATE) {
                // This is a lone surrogate: show the code
                b.append(String.format("\\u%04x", c));
            } else {
                b.appendCodePoint(c);
            }
        }
        return b.toString();
    }

    /**
     * Present a Python {@code str} as a Java {@code String} value or
     * raise a {@link TypeError}. This is for use when the argument is
     * expected to be a Python {@code str} or a sub-class of it.
     *
     * @param v claimed {@code str}
     * @return {@code String} value
     * @throws TypeError if {@code v} is not a Python {@code str}
     */
    public static String asString(Object v) throws TypeError {
        return asString(v, o -> Abstract.requiredTypeError("a str", o));
    }

    /**
     * Present a qualifying object {@code v} as a Java {@code String}
     * value or throw {@code E}. This is for use when the argument is
     * expected to be a Python {@code str} or a sub-class of it.
     * <p>
     * The detailed form of exception is communicated in a
     * lambda-function {@code exc} that will be called (if necessary)
     * with {@code v} as argument. We use a {@code Function} to avoid
     * binding a variable {@code v} at the call site.
     *
     * @param <E> type of exception to throw
     * @param v claimed {@code str}
     * @param exc to supply the exception to throw wrapping {@code v}
     * @return {@code String} value
     * @throws E if {@code v} is not a Python {@code str}
     */
    public static <E extends PyException> String asString(Object v,
            Function<Object, E> exc) throws PyException {
        if (v instanceof String)
            return (String)v;
        else if (v instanceof PyUnicode)
            return ((PyUnicode)v).asString();
        throw exc.apply(v);
    }

    // Plumbing ------------------------------------------------------

    // @formatter:on

    /**
     * Convert a Python {@code str} to a Java {@code str} (or throw
     * {@link NoConversion}). This is suitable for use where a method
     * argument should be (exactly) a {@code str}, or an alternate path
     * taken.
     * <p>
     * If the method throws the special exception {@link NoConversion},
     * the caller must deal with it by throwing an appropriate Python
     * exception or taking an alternative course of action.
     *
     * @param v to convert
     * @return converted to {@code String}
     * @throws NoConversion v is not a {@code str}
     */
    static String convertToString(Object v) throws NoConversion {
        if (v instanceof String)
            return (String)v;
        else if (v instanceof PyUnicode)
            return ((PyUnicode)v).asString();
        throw PyObjectUtil.NO_CONVERSION;
    }

    /**
     * Coerce a Python {@code str} to a Java String, or raise a
     * specified exception. This is suitable for use where a method
     * argument should be (exactly) a {@code str}, or a context specific
     * exception has to be raised.
     *
     * @param <E> type of exception to throw
     * @param arg to coerce
     * @param exc supplier for actual exception
     * @return {@code arg} as a {@code String}
     */
    static <E extends PyException> String coerceToString(Object arg, Supplier<E> exc) {
        if (arg instanceof String) {
            return (String)arg;
        } else if (arg instanceof PyUnicode) {
            return ((PyUnicode)arg).asString();
        } else {
            throw exc.get();
        }
    }

    /**
     * Convert this {@code PyUnicode} to a Java {@code String} built
     * from its code point values. If the code point value of a
     * character in Python is a lone surrogate, it will become that
     * UTF-16 unit in the result.
     *
     * @return this {@code PyUnicode} as a Java {@code String}
     */
    String asString() {
        StringBuilder b = new StringBuilder();
        for (int c : delegate) { b.appendCodePoint(c); }
        return b.toString();
    }

    /**
     * Test whether a string contains no characters above the BMP range,
     * that is, any characters that require surrogate pairs to represent
     * them. The method returns {@code true} if and only if the string
     * consists entirely of BMP characters or is empty.
     *
     * @param s the string to test
     * @return whether contains no non-BMP characters
     */
    private static boolean isBMP(String s) {
        return s.codePoints().dropWhile(Character::isBmpCodePoint).findFirst().isEmpty();
    }

    /**
     * Define what characters are to be treated as a space according to
     * Python 3.
     */
    private static boolean isPythonSpace(int cp) {
        // Use the Java built-in methods as far as possible
        return Character.isWhitespace(cp) // ASCII spaces and some
                // remaining Unicode spaces
                || Character.isSpaceChar(cp)
                // NEXT LINE (not a space in Java or Unicode)
                || cp == 0x0085;
    }

    /**
     * Define what characters are to be treated as a line separator
     * according to Python 3. In {@code splitlines} we treat these as
     * separators, but also give singular treatment to the sequence
     * CR-LF.
     */
    private static boolean isPythonLineSeparator(int cp) {
        // Bit i is set if code point i is a line break (i<32).
        final int EOL = 0b0111_0000_0000_0000_0011_1100_0000_0000;
        if (cp >>> 5 == 0) {
            // cp < 32: use the little look-up table
            return ((EOL >>> cp) & 1) != 0;
        } else if (cp >>> 7 == 0) {
            // 32 <= cp < 128 : the rest of ASCII
            return false;
        } else {
            // NEL, L-SEP, P-SEP
            return cp == 0x85 || cp == 0x2028 || cp == 0x2029;
        }
    }

    /**
     * A base class for the delegate of either a {@code String} or a
     * {@code PyUnicode}, implementing {@code __getitem__} and other
     * index-related operations. The class is a
     * {@link PySequence.Delegate}, an iterable of {@code Integer},
     * comparable with other instances of the same base, and is able to
     * supply point codes as a stream.
     */
    static abstract class CodepointDelegate extends PySequence.Delegate<Integer, Object>
            implements PySequence.OfInt {
        /**
         * A bidirectional iterator on the sequence of code points between
         * two indices.
         *
         * @param index starting position (code point index)
         * @param start index of first element to include.
         * @param end index of first element not to include.
         * @return the iterator
         */
        abstract CodepointIterator iterator(int index, int start, int end);

        /**
         * A bidirectional iterator on the sequence of code points.
         *
         * @param index starting position (code point index)
         * @return the iterator
         */
        CodepointIterator iterator(int index) { return iterator(index, 0, length()); }

        /**
         * A bidirectional iterator on the sequence of code points,
         * positioned initially one beyond the end of the sequence, so that
         * the first call to {@code previous()} returns the last element.
         *
         * @return the iterator
         */
        CodepointIterator iteratorLast() { return iterator(length()); }

        @Override
        public Iterator<Integer> iterator() { return iterator(0); }

        /**
         * Return the object of which this is the delegate.
         *
         * @return the object of which this is the delegate
         */
        abstract Object principal();

        // Re-declared here to remove throws clause
        @Override
        public abstract Object getItem(int i);

        // Re-declared here to remove throws clause
        @Override
        public abstract Object getSlice(Indices slice);

        @Override
        public String toString() {
            StringBuilder b = new StringBuilder("adapter(\"");
            for (Integer c : this) { b.appendCodePoint(c); }
            return b.append("\")").toString();
        }
    }

    /**
     * A {@code ListIterator} working bidirectionally in code point
     * indices.
     */
    interface CodepointIterator extends ListIterator<Integer>, PrimitiveIterator.OfInt {

        @Override
        default Integer next() { return nextInt(); }

        /**
         * Returns {@code true} if this list iterator has the given number
         * of elements when traversing the list in the forward direction.
         *
         * @param n number of elements needed
         * @return {@code true} if has a further {@code n} elements going
         *     forwards
         */
        boolean hasNext(int n);

        /**
         * Equivalent to {@code n} calls to {@link #nextInt()} returning the
         * last result.
         *
         * @param n the number of advances
         * @return the {@code n}th next {@code int}
         */
        int nextInt(int n);

        @Override
        default Integer previous() { return previousInt(); }

        /**
         * Returns {@code true} if this list iterator has the given number
         * of elements when traversing the list in the reverse direction.
         *
         * @param n number of elements needed
         * @return {@code true} if has a further {@code n} elements going
         *     backwards
         */
        boolean hasPrevious(int n);

        /**
         * Returns the previous {@code int} element in the iteration. This
         * is just previous specialised to a primitive {@code int}.
         *
         * @return the previous {@code int}
         */
        int previousInt();

        /**
         * Equivalent to {@code n} calls to {@link #previousInt()} returning
         * the last result.
         *
         * @param n the number of steps to take (in reverse)
         * @return the {@code n}th previous {@code int}
         */
        int previousInt(int n);

        // Unsupported operations -----------------------------

        @Override
        default void remove() { throw new UnsupportedOperationException(); }

        @Override
        default void set(Integer o) { throw new UnsupportedOperationException(); }

        @Override
        default void add(Integer o) { throw new UnsupportedOperationException(); }

        // Iterator mark and restore --------------------------

        /**
         * Set a mark (a saved state) to which the iterator may be restored
         * later.
         *
         * @return the mark
         */
        Mark mark();

        /**
         * An opaque object to hold and restore the position of a particular
         * {@link CodepointIterator}.
         */
        interface Mark {
            /**
             * Restore the position of the iterator from which this {@code Mark}
             * was obtained, to the position it had at the time.
             */
            void restore();
        }
    }

    /**
     * Wrap a Java {@code String} as a {@link PySequence.Delegate}, that
     * is also an iterable of {@code Integer}. If the {@code String}
     * includes surrogate pairs of {@code char}s, these are interpreted
     * as a single Python code point.
     */
    static class StringAdapter extends CodepointDelegate {

        /** Value of the str encoded as a Java {@code String}. */
        private final String s;
        /** Length in code points deduced from the {@code String}. */
        private final int length;

        /**
         * Adapt a String so we can iterate or stream its code points.
         *
         * @param s to adapt
         */
        StringAdapter(String s) {
            this.s = s;
            length = s.codePointCount(0, s.length());
        }

        /**
         * Return {@code true} iff the string contains only basic plane
         * characters or, possibly, isolated surrogates. All {@code char}s
         * may be treated as code points.
         *
         * @return contains only BMP characters or isolated surrogates
         */
        private boolean isBMP() { return length == s.length(); }

        @Override
        public int length() { return length; };

        @Override
        public int getInt(int i) {
            if (isBMP()) {
                // No surrogate pairs.
                return s.charAt(i);
            } else {
                // We have to count from the start
                int k = toCharIndex(i);
                return s.codePointAt(k);
            }
        }

        @Override
        public PyType getType() { return TYPE; }

        @Override
        public String getTypeName() { return "string"; }

        @Override
        Object principal() { return s; }

        @Override
        public Object getItem(int i) {
            if (isBMP()) {
                // No surrogate pairs.
                return String.valueOf(s.charAt(i));
            } else {
                return PyUnicode.fromCodePoint(getInt(i));
            }
        }

        /**
         * Translate a (valid) code point index into a {@code char} index
         * into {@code s}, when s contains surrogate pairs. A call is
         * normally guarded by {@link #isBMP()}, since when that is
         * {@code true} we can avoid the work.
         *
         * @param cpIndex code point index
         * @return {@code char} index into {@code s}
         */
        private int toCharIndex(int cpIndex) {
            int L = s.length();
            if (cpIndex == length) {
                // Avoid counting to the end
                return L;
            } else {
                int i = 0, cpCount = 0;
                while (i < L && cpCount < cpIndex) {
                    char c = s.charAt(i++);
                    cpCount++;
                    if (Character.isHighSurrogate(c) && i < L) {
                        // Expect a low surrogate
                        char d = s.charAt(i);
                        if (Character.isLowSurrogate(d)) { i++; }
                    }
                }
                return i;
            }
        }

        @Override
        public Object getSlice(Indices slice) {
            if (slice.slicelength == 0) {
                return "";
            } else if (slice.step == 1 && isBMP()) {
                return s.substring(slice.start, slice.stop);
            } else {
                /*
                 * If the code points are not all BMP, it is less work in future if
                 * we use a PyUnicode. If step != 1, there is the possibility of
                 * creating an unintended surrogate pair, so only a PyUnicode should
                 * be trusted to represent the result.
                 */
                int L = slice.slicelength, i = slice.start;
                int[] r = new int[L];
                if (isBMP()) {
                    // Treating surrogates as characters
                    for (int j = 0; j < L; j++) {
                        r[j] = s.charAt(i);
                        i += slice.step;
                    }
                } else if (slice.step > 0) {
                    // Work forwards through the sequence
                    ListIterator<Integer> cps = iterator(i);
                    r[0] = cps.next();
                    for (int j = 1; j < L; j++) {
                        for (int k = 1; k < slice.step; k++) { cps.next(); }
                        r[j] = cps.next();
                    }
                } else { // slice.step < 0
                    // Work backwards through the sequence
                    ListIterator<Integer> cps = iterator(i + 1);
                    r[0] = cps.previous();
                    for (int j = 1; j < L; j++) {
                        for (int k = -1; k > slice.step; --k) { cps.previous(); }
                        r[j] = cps.previous();
                    }
                }
                return wrap(r);
            }
        }

        @Override
        Object add(Object ow) throws OutOfMemoryError, NoConversion, Throwable {
            if (ow instanceof String) {
                return PyUnicode.concat(s, (String)ow);
            } else {
                IntStream w = adapt(ow).asIntStream();
                return concatUnicode(s.codePoints(), w);
            }
        }

        @Override
        Object radd(Object ov) throws OutOfMemoryError, NoConversion, Throwable {
            if (ov instanceof String) {
                return PyUnicode.concat((String)ov, s);
            } else {
                IntStream v = adapt(ov).asIntStream();
                return concatUnicode(v, s.codePoints());
            }
        }

        @Override
        Object repeat(int n) throws OutOfMemoryError, Throwable {
            if (n == 0)
                return "";
            else if (n == 1 || length == 0)
                return s;
            else if (Character.isLowSurrogate(s.charAt(0))
                    && Character.isHighSurrogate(s.charAt(length - 1)))
                /*
                 * s ends with a high surrogate and starts with a low surrogate, so
                 * simply concatenated to itself by String.repeat, these would merge
                 * into one character. Only a PyUnicode properly represents the
                 * result.
                 */
                return (new PyUnicode(TYPE, s)).delegate.repeat(n);
            else
                // Java String repeat will do fine
                return s.repeat(n);
        }

        @Override
        public int compareTo(PySequence.Delegate<Integer, Object> other) {
            Iterator<Integer> ia = iterator();
            Iterator<Integer> ib = other.iterator();
            while (ia.hasNext()) {
                if (ib.hasNext()) {
                    int a = ia.next();
                    int b = ib.next();
                    // if a != b, then we've found an answer
                    if (a > b)
                        return 1;
                    else if (a < b)
                        return -1;
                } else
                    // s has not run out, but b has. s wins
                    return 1;
            }
            /*
             * The sequences matched over the length of s. The other is the
             * winner if it still has elements. Otherwise its a tie.
             */
            return ib.hasNext() ? -1 : 0;
        }

        // PySequence.OfInt interface --------------------------------

        @Override
        public Spliterator.OfInt spliterator() { return s.codePoints().spliterator(); }

        @Override
        public IntStream asIntStream() { return s.codePoints(); }

        // ListIterator provision ------------------------------------

        @Override
        public CodepointIterator iterator(final int index, int start, int end) {
            if (isBMP())
                return new BMPIterator(index, start, end);
            else
                return new SMPIterator(index, start, end);
        }

        /**
         * A {@code ListIterator} for use when the string in the surrounding
         * adapter instance contains only basic multilingual plane (BMP)
         * characters or isolated surrogates. {@link SMPIterator} extends
         * this class for supplementary characters.
         */
        class BMPIterator implements CodepointIterator {
            /**
             * Index into {@code s} in code points, which is also its index in
             * {@code s} in chars when {@code s} is a BMP string.
             */
            protected int index;
            /**
             * First index at which {@link #next()} is allowable for the
             * iterator in code points, which is also its index in {@code s} in
             * chars when {@code s} is a BMP string.
             */
            protected final int start;
            /**
             * First index at which {@link #next()} is not allowable for the
             * iterator in code points, which is also its index in {@code s} in
             * chars when {@code s} is a BMP string.
             */
            protected final int end;

            BMPIterator(int index, int start, int end) {
                checkIndexRange(index, start, end, length);
                this.start = start;
                this.end = end;
                this.index = index;
            }

            @Override
            public Mark mark() {
                return new Mark() {
                    final int i = index;

                    @Override
                    public void restore() { index = i; }
                };
            }

            // The forward iterator -------------------------------

            @Override
            public boolean hasNext() { return index < end; }

            @Override
            public boolean hasNext(int n) {
                assert n >= 0;
                return index + n <= end;
            }

            @Override
            public int nextInt() {
                if (index < end)
                    return s.charAt(index++);
                else
                    throw noSuchElement(nextIndex());
            }

            @Override
            public int nextInt(int n) {
                assert n >= 0;
                int i = index + n;
                if (i <= end)
                    return s.charAt((index = i) - 1);
                else
                    throw noSuchElement(i - start);
            }

            @Override
            public int nextIndex() { return index - start; }

            // The reverse iterator -------------------------------

            @Override
            public boolean hasPrevious() { return index > start; }

            @Override
            public boolean hasPrevious(int n) {
                assert n >= 0;
                return index - n >= 0;
            }

            @Override
            public int previousInt() {
                if (index > start)
                    return s.charAt(--index);
                else
                    throw noSuchElement(previousIndex());
            }

            @Override
            public int previousInt(int n) {
                assert n >= 0;
                int i = index - n;
                if (i >= start)
                    return s.charAt(index = i);
                else
                    throw noSuchElement(i);
            }

            @Override
            public int previousIndex() { return index - start - 1; }

            // Diagnostic use -------------------------------------

            @Override
            public String toString() {
                return String.format("[%s|%s]", s.substring(start, index), s.substring(index, end));
            }
        }

        /**
         * A {@code ListIterator} for use when the string in the surrounding
         * adapter instance contains one or more supplementary multilingual
         * plane characters represented by surrogate pairs.
         */
        class SMPIterator extends BMPIterator {

            /**
             * Index of the iterator position in {@code s} in chars. This always
             * moves in synchrony with the base class index
             * {@link BMPIterator#index}, which continues to represent the same
             * position as a code point index. Both reference the same
             * character.
             */
            private int charIndex;
            /**
             * The double of {@link BMPIterator#start} in {@code s} in chars.
             */
            final private int charStart;
            /**
             * The double of {@link BMPIterator#end} in {@code s} in chars.
             */
            final private int charEnd;

            SMPIterator(int index, int start, int end) {
                super(index, start, end);
                // Convert the arguments to character indices
                int p = 0, cp = 0;
                while (p < start) {
                    cp = nextCharIndex(cp);
                    p += 1;
                }
                this.charStart = cp;
                while (p < index) {
                    cp = nextCharIndex(cp);
                    p += 1;
                }
                this.charIndex = cp;
                while (p < end) {
                    cp = nextCharIndex(cp);
                    p += 1;
                }
                this.charEnd = cp;
            }

            /** @return next char index after argument. */
            private int nextCharIndex(int cp) {
                if (Character.isBmpCodePoint(s.codePointAt(cp)))
                    return cp + 1;
                else
                    return cp + 2;
            }

            @Override
            public Mark mark() {
                return new Mark() {
                    // In the SMP iterator, we must save both indices
                    final int i = index, ci = charIndex;

                    @Override
                    public void restore() {
                        index = i;
                        charIndex = ci;
                    }
                };
            }

            // The forward iterator -------------------------------

            @Override
            public int nextInt() {
                if (charIndex < charEnd) {
                    char c = s.charAt(charIndex++);
                    index++;
                    if (Character.isHighSurrogate(c) && charIndex < charEnd) {
                        // Expect a low surrogate
                        char d = s.charAt(charIndex);
                        if (Character.isLowSurrogate(d)) {
                            charIndex++;
                            return Character.toCodePoint(c, d);
                        }
                    }
                    return c;
                } else
                    throw new NoSuchElementException();
            }

            @Override
            public int nextInt(int n) {
                assert n >= 0;
                int i = index + n, indexSaved = index, charIndexSaved = charIndex;
                while (hasNext()) {
                    int c = nextInt();
                    if (index == i) { return c; }
                }
                index = indexSaved;
                charIndex = charIndexSaved;
                throw noSuchElement(i);
            }

            // The reverse iterator -------------------------------

            @Override
            public int previousInt() {
                if (charIndex > charStart) {
                    --index;
                    char d = s.charAt(--charIndex);
                    if (Character.isLowSurrogate(d) && charIndex > charStart) {
                        // Expect a low surrogate
                        char c = s.charAt(--charIndex);
                        if (Character.isHighSurrogate(c)) { return Character.toCodePoint(c, d); }
                        charIndex++;
                    }
                    return d;
                } else
                    throw new NoSuchElementException();
            }

            @Override
            public int previousInt(int n) {
                assert n >= 0;
                int i = index - n, indexSaved = index, charIndexSaved = charIndex;
                while (hasPrevious()) {
                    int c = previousInt();
                    if (index == i) { return c; }
                }
                index = indexSaved;
                charIndex = charIndexSaved;
                throw noSuchElement(i);
            }

            // Diagnostic use -------------------------------------

            @Override
            public String toString() {
                return String.format("[%s|%s]", s.substring(charStart, charIndex),
                        s.substring(charIndex, charEnd));
            }
        }
    }

    /**
     * A class to act as the delegate implementing {@code __getitem__}
     * and other index-related operations. By inheriting {@link Delegate
     * PySequence.Delegate} in this inner class, we obtain boilerplate
     * implementation code for slice translation and range checks. We
     * need only specify the work specific to {@link PyUnicode}
     * instances.
     */
    class UnicodeAdapter extends CodepointDelegate {

        @Override
        public int length() { return value.length; }

        @Override
        public int getInt(int i) { return value[i]; }

        @Override
        public PyType getType() { return TYPE; }

        @Override
        public String getTypeName() { return "string"; }

        @Override
        Object principal() { return PyUnicode.this; }

        @Override
        public Object getItem(int i) { return PyUnicode.fromCodePoint(value[i]); }

        @Override
        public Object getSlice(Indices slice) {
            int[] v;
            if (slice.step == 1)
                v = Arrays.copyOfRange(value, slice.start, slice.stop);
            else {
                v = new int[slice.slicelength];
                int i = slice.start;
                for (int j = 0; j < slice.slicelength; j++) {
                    v[j] = value[i];
                    i += slice.step;
                }
            }
            return wrap(v);
        }

        @Override
        Object add(Object ow) throws OutOfMemoryError, NoConversion, Throwable {
            if (ow instanceof PyUnicode) {
                // Optimisation (or is it?) over concatUnicode
                PyUnicode w = (PyUnicode)ow;
                int L = value.length, M = w.value.length;
                int[] r = new int[L + M];
                System.arraycopy(value, 0, r, 0, L);
                System.arraycopy(w.value, 0, r, L, M);
                return wrap(r);
            } else {
                return concatUnicode(asIntStream(), adapt(ow).asIntStream());
            }
        }

        @Override
        Object radd(Object ov) throws OutOfMemoryError, NoConversion, Throwable {
            if (ov instanceof PyUnicode) {
                // Optimisation (or is it?) over concatUnicode
                PyUnicode v = (PyUnicode)ov;
                int L = v.value.length, M = value.length;
                int[] r = new int[L + M];
                System.arraycopy(v.value, 0, r, 0, L);
                System.arraycopy(value, 0, r, L, M);
                return wrap(r);
            } else {
                return concatUnicode(adapt(ov).asIntStream(), asIntStream());
            }
        }

        @Override
        Object repeat(int n) throws OutOfMemoryError, Throwable {
            int m = value.length;
            if (n == 0)
                return "";
            else if (n == 1 || m == 0)
                return PyUnicode.this;
            else {
                int[] b = new int[n * m];
                for (int i = 0, p = 0; i < n; i++, p += m) { System.arraycopy(value, 0, b, p, m); }
                return wrap(b);
            }
        }

        @Override
        public int compareTo(PySequence.Delegate<Integer, Object> other) {
            Iterator<Integer> ib = other.iterator();
            for (int a : value) {
                if (ib.hasNext()) {
                    int b = ib.next();
                    // if a != b, then we've found an answer
                    if (a > b)
                        return 1;
                    else if (a < b)
                        return -1;
                } else
                    // value has not run out, but other has. We win.
                    return 1;
            }
            /*
             * The sequences matched over the length of value. The other is the
             * winner if it still has elements. Otherwise its a tie.
             */
            return ib.hasNext() ? -1 : 0;
        }

        // PySequence.OfInt interface --------------------------------

        @Override
        public Spliterator.OfInt spliterator() {
            final int flags = Spliterator.IMMUTABLE | Spliterator.SIZED | Spliterator.ORDERED;
            return Spliterators.spliterator(value, flags);
        }

        @Override
        public IntStream asIntStream() {
            int flags = Spliterator.IMMUTABLE | Spliterator.SIZED;
            Spliterator.OfInt s = Spliterators.spliterator(value, flags);
            return StreamSupport.intStream(s, false);
        }

        // ListIterator provision ------------------------------------

        @Override
        public CodepointIterator iterator(final int index, int start, int end) {
            return new UnicodeIterator(index, start, end);
        }

        /**
         * A {@code ListIterator} for use when the string in the surrounding
         * adapter instance contains only basic multilingual plane
         * characters or isolated surrogates.
         */
        class UnicodeIterator implements CodepointIterator {

            private int index;
            private final int start, end;

            UnicodeIterator(int index, int start, int end) {
                checkIndexRange(index, start, end, value.length);
                this.start = start;
                this.end = end;
                this.index = index;
            }

            @Override
            public Mark mark() {
                return new Mark() {
                    final int i = index;

                    @Override
                    public void restore() { index = i; }
                };
            }

            // The forward iterator -------------------------------

            @Override
            public boolean hasNext() { return index < value.length; }

            @Override
            public boolean hasNext(int n) {
                assert n >= 0;
                return index + n <= value.length;
            }

            @Override
            public int nextInt() {
                if (index < end)
                    return value[index++];
                else
                    throw noSuchElement(nextIndex());
            }

            @Override
            public int nextInt(int n) {
                assert n >= 0;
                int i = index + n;
                if (i <= end)
                    return value[(index = i) - 1];
                else
                    throw noSuchElement(i - start);
            }

            @Override
            public int nextIndex() { return index - start; }

            // The reverse iterator -------------------------------

            @Override
            public boolean hasPrevious() { return index > start; }

            @Override
            public boolean hasPrevious(int n) {
                assert n >= 0;
                return index - n >= 0;
            }

            @Override
            public int previousInt() {
                if (index > start)
                    return value[--index];
                else
                    throw noSuchElement(previousIndex());
            }

            @Override
            public int previousInt(int n) {
                assert n >= 0;
                int i = index - n;
                if (i >= start)
                    return value[index = i];
                else
                    throw noSuchElement(i);
            }

            @Override
            public int previousIndex() { return index - start - 1; }

            // Diagnostic use -------------------------------------

            @Override
            public String toString() {
                return String.format("[%s|%s]", new String(value, start, index - start),
                        new String(value, index, end - index));
            }
        }
    }

    /**
     * Adapt a Python {@code str} to a sequence of Java {@code int}
     * values or throw an exception. If the method throws the special
     * exception {@link NoConversion}, the caller must catch it and deal
     * with it, perhaps by throwing a {@link TypeError}. A binary
     * operation will normally return {@link Py#NotImplemented} in that
     * case.
     * <p>
     * Note that implementing {@link PySequence.OfInt} is not enough,
     * which other types may, but be incompatible in Python.
     *
     * @param v to wrap or return
     * @return adapted to a sequence
     * @throws NoConversion if {@code v} is not a Python {@code str}
     */
    static CodepointDelegate adapt(Object v) throws NoConversion {
        // Check against supported types, most likely first
        if (v instanceof String)
            return new StringAdapter((String)v);
        else if (v instanceof PyUnicode)
            return ((PyUnicode)v).delegate;
        throw PyObjectUtil.NO_CONVERSION;
    }

    /**
     * Short-cut {@link #adapt(Object)} when type statically known.
     *
     * @param v to wrap
     * @return new StringAdapter(v)
     */
    static StringAdapter adapt(String v) { return new StringAdapter(v); }

    /**
     * Short-cut {@link #adapt(Object)} when type statically known.
     *
     * @return the delegate for sequence operations on this {@code str}
     */
    UnicodeAdapter adapt() { return delegate; }

    /**
     * Adapt a Python {@code str}, as by {@link #adapt(Object)}, that is
     * intended as a substring to find, in {@code str.find()} or
     * {@code str.replace()}, for example. If the argument cannot be
     * adapted as a {@code str}, a {@code TypeError} will be raised,
     * with message like "METHOD(): string to find must be str not T",
     * where {@code T} is the type of the errant argument.
     *
     * @param method in which encountered
     * @param sub alleged string
     * @return adapted to a sequence
     * @throws TypeError if {@code sub} cannot be wrapped as a delegate
     */
    static CodepointDelegate adaptSub(String method, Object sub) throws TypeError {
        try {
            return adapt(sub);
        } catch (NoConversion nc) {
            throw Abstract.argumentTypeError(method, "string to find", "str", sub);
        }
    }

    /**
     * Adapt a Python {@code str}, as by {@link #adapt(Object)}, that is
     * intended as a replacement substring in {@code str.replace()}, for
     * example.
     *
     * @param method in which encountered
     * @param replacement alleged string
     * @return adapted to a sequence
     * @throws TypeError if {@code sub} cannot be wrapped as a delegate
     */
    static CodepointDelegate adaptRep(String method, Object replacement) throws TypeError {
        try {
            return adapt(replacement);
        } catch (NoConversion nc) {
            throw Abstract.argumentTypeError(method, "replacement", "str", replacement);
        }
    }

    /**
     * Adapt a Python {@code str} intended as a separator, as by
     * {@link #adapt(Object)}.
     *
     * @param method in which encountered
     * @param sep alleged separator
     * @return adapted to a sequence
     * @throws TypeError if {@code sep} cannot be wrapped as a delegate
     * @throws ValueError if {@code sep} is the empty string
     */
    static CodepointDelegate adaptSeparator(String method, Object sep)
            throws TypeError, ValueError {
        try {
            CodepointDelegate p = adapt(sep);
            if (p.length() == 0) { throw new ValueError("%s(): empty separator", method); }
            return p;
        } catch (NoConversion nc) {
            throw Abstract.argumentTypeError(method, "separator", "str or None", sep);
        }
    }

    /**
     * Adapt a Python {@code str} intended as a fill character in
     * justification and centring operations. The behaviour is quite
     * like {@link #adapt(Object)}, but it returns a single code point.
     * A null argument returns the default choice, a space.
     *
     * @param method in which encountered
     * @param fill alleged fill character (or {@code null})
     * @return fill as a code point
     * @throws TypeError if {@code fill} is not a one-character string
     */
    private static int adaptFill(String method, Object fill) {
        if (fill == null) {
            return ' ';
        } else if (fill instanceof String) {
            String s = (String)fill;
            if (s.codePointCount(0, s.length()) != 1)
                throw new TypeError(BAD_FILLCHAR);
            return s.codePointAt(0);
        } else if (fill instanceof PyUnicode) {
            PyUnicode u = (PyUnicode)fill;
            if (u.value.length != 1)
                throw new TypeError(BAD_FILLCHAR);
            return u.value[0];
        } else {
            throw Abstract.argumentTypeError(method, "fill", "a character", fill);
        }
    }

    private static String BAD_FILLCHAR = "the fill character must be exactly one character long";

    /**
     * Adapt a Python {@code str}, intended as a list of characters to
     * strip, as by {@link #adapt(Object)} then conversion to a set.
     *
     * @param method in which encountered
     * @param chars characters defining the set (or {@code None} or
     *     {@code null})
     * @return {@code null} or characters adapted to a set
     * @throws TypeError if {@code sep} cannot be wrapped as a delegate
     */
    static Set<Integer> adaptStripSet(String method, Object chars) throws TypeError, ValueError {
        if (chars == null || chars == Py.None) {
            return null;
        } else {
            try {
                return adapt(chars).asStream().collect(Collectors.toCollection(HashSet::new));
            } catch (NoConversion nc) {
                throw Abstract.argumentTypeError(method, "chars", "str or None", chars);
            }
        }
    }

    /**
     * Convert slice end indices to a {@link PySlice.Indices} object.
     *
     * @param s sequence being sliced
     * @param start first index included
     * @param end first index not included
     * @return indices of the slice
     * @throws TypeError if {@code start} or {@code end} cannot be
     *     considered an index
     */
    private static PySlice.Indices getSliceIndices(CodepointDelegate s, Object start, Object end)
            throws TypeError {
        try {
            return (new PySlice(start, end)).getIndices(s.length());
        } catch (PyException pye) {
            throw pye;
        } catch (Throwable t) {
            throw new InterpreterError(t, "non-python exception)");
        }
    }

    /**
     * Concatenate two {@code String} representations of {@code str}.
     * This method almost always calls {@code String.concat(v, w)} and
     * almost always returns a {@code String}. There is a delicate case
     * where {@code v} ends with a high surrogate and {@code w} starts
     * with a low surrogate. Simply concatenated, these merge into one
     * character. Only a {@code PyUnicode} properly represents the
     * result in that case.
     *
     * @param v first string to concatenate
     * @param w second string to concatenate
     * @return the concatenation {@code v + w}
     */
    private static Object concat(String v, String w) throws OutOfMemoryError {
        /*
         * Since we have to guard against empty strings, we may as well take
         * the optimisation these paths invite.
         */
        int vlen = v.length();
        if (vlen == 0)
            return w;
        else if (w.length() == 0)
            return v;
        else if (Character.isLowSurrogate(w.charAt(0))
                && Character.isHighSurrogate(v.charAt(vlen - 1)))
            // Only a PyUnicode properly represents the result
            return concatUnicode(v.codePoints(), w.codePoints());
        else {
            // Java String concatenation will do fine
            return v.concat(w);
        }
    }

    /**
     * Concatenate two streams of code points into a {@code PyUnicode}.
     *
     * @param v first string to concatenate
     * @param w second string to concatenate
     * @return the concatenation {@code v + w}
     * @throws OutOfMemoryError when the concatenated string is too long
     */
    private static PyUnicode concatUnicode(IntStream v, IntStream w) throws OutOfMemoryError {
        return wrap(IntStream.concat(v, w).toArray());
    }

    /**
     * Apply a unary operation to every character of a string and return
     * them as a string. This supports transformations like
     * {@link #upper() str.upper()}.
     *
     * @param op the operation
     * @return transformed string
     */
    private PyUnicode mapChars(IntUnaryOperator op) {
        return wrap(delegate.asIntStream().map(op).toArray());
    }

    /**
     * Apply a unary operation to every character of a string and return
     * them as a string. This supports transformations like
     * {@link #upper() str.upper()}.
     *
     * @param op the operation
     * @return transformed string
     */
    private static String mapChars(String s, IntUnaryOperator op) {
        int[] v = s.codePoints().map(op).toArray();
        return new String(v, 0, v.length);
    }

    /** A {@code NoSuchElementException} identifying the index. */
    private static NoSuchElementException noSuchElement(int k) {
        return new NoSuchElementException(Integer.toString(k));
    }

    /**
     * Assert that <i>0 &le; start &le;index &le; end &le; len</i> or if
     * not, throw an exception.
     *
     * @param index e.g. the start position of na iterator.
     * @param start first in range
     * @param end first beyond range (i.e. non-inclusive bound)
     * @param len of sequence
     * @throws IndexOutOfBoundsException if the condition is violated
     */
    private static void checkIndexRange(int index, int start, int end, int len)
            throws IndexOutOfBoundsException {
        if ((0 <= start && start <= end && end <= len) == false)
            throw new IndexOutOfBoundsException(
                    String.format("start=%d, end=%d, len=%d", start, end, len));
        else if (index < start)
            throw new IndexOutOfBoundsException("before start");
        else if (index > end)
            throw new IndexOutOfBoundsException("beyond end");
    }

    /**
     * A little helper for converting str.find to str.index that will
     * raise {@code ValueError("substring not found")} if the argument
     * is negative, otherwise passes the argument through.
     *
     * @param index to check
     * @return {@code index} if non-negative
     * @throws ValueError if argument is negative
     */
    private static final int checkIndexReturn(int index) throws ValueError {
        if (index >= 0) {
            return index;
        } else {
            throw new ValueError("substring not found");
        }
    }

    // Plumbing (Jython 2) -------------------------------------------

    // @formatter:off

    public int atoi(int base) {
        return atoi(encodeDecimal(), base);
    }

    public PyLong atol(int base) {
        return atol(encodeDecimal(), base);
    }

    public double atof() {
        return atof(encodeDecimal());
    }

    /**
     * Encode unicode into a valid decimal String. Throws a UnicodeEncodeError on invalid
     * characters.
     *
     * @return a valid decimal as an encoded String
     */
    @Deprecated // See _PyUnicode_TransformDecimalAndSpaceToASCII
    private String encodeDecimal() {

        // XXX This all has a has a Jython 2 smell: bytes/str confusion.
        // XXX Also, String and PyUnicode implementations are needed.
        // XXX Follow CPython _PyUnicode_TransformDecimalAndSpaceToASCII

        int digit;
        StringBuilder sb = new StringBuilder();

        for (CodepointIterator si = delegate.iterator(0); si.hasNext();) {
            int codePoint = si.nextInt();
            if (isPythonSpace(codePoint)) {
                sb.append(' ');
                continue;
            }
            digit = Character.digit(codePoint, 10);
            if (digit >= 0) {
                sb.append(digit);
                continue;
            }
            if (0 < codePoint && codePoint < 256) {
                sb.appendCodePoint(codePoint);
                continue;
            }
            // All other characters are considered unencodable
            int i = si.previousIndex();
            // Signature has a Jython 2 smell: String->String?
            codecs.encoding_error("strict", "decimal", asString(), i, i + 1,
                    "invalid decimal Unicode string");
        }
        return sb.toString();
    }

    /**
     * Encode unicode in the basic plane into a valid decimal String. Throws a UnicodeEncodeError on
     * invalid characters.
     *
     * @return a valid decimal as an encoded String
     */
    private String encodeDecimalBasic() {
        int digit;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < asString().length(); i++) {
            char ch = asString().charAt(i);
            if (isPythonSpace(ch)) {
                sb.append(' ');
                continue;
            }
            digit = Character.digit(ch, 10);
            if (digit >= 0) {
                sb.append(digit);
                continue;
            }
            if (0 < ch && ch < 256) {
                sb.append(ch);
                continue;
            }
            // All other characters are considered unencodable
            codecs.encoding_error("strict", "decimal", asString(), i, i + 1,
                    "invalid decimal Unicode string");
        }
        return sb.toString();
    }

    // Copied from PyString
    /**
     * A little helper for converting str.find to str.index that will raise
     * {@code ValueError("substring not found")} if the argument is negative, otherwise passes
     * the argument through.
     *
     * @param index to check
     * @return {@code index} if non-negative
     * @throws PyException {@code ValueError} if not found
     */
    protected final int checkIndex(int index) throws PyException {
        if (index >= 0) {
            return index;
        } else {
            throw new ValueError("substring not found");
        }
    }

    // Copied from PyString with this -> self
    /**
     * Convert a {@code String} to a floating-point value according to Python rules.
     *
     * @param self to convert
     * @return the value
     */
    public static double atof(String self) {
        double x = 0.0;
        Matcher m = getFloatPattern().matcher(self);
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
            throw new ValueError(String.format(fmt, self.trim()));
        }
    }

    // Copied from PyString
    /**
     * Regular expression for an unsigned Python float, accepting also any sequence of the letters
     * that belong to "NaN" or "Infinity" in whatever case. This is used within the regular
     * expression patterns that define a priori acceptable strings in the float and complex
     * constructors. The expression contributes no capture groups.
     */
    private static final String UF_RE =
            "(?:(?:(?:\\d+\\.?|\\.\\d)\\d*(?:[eE][+-]?\\d+)?)|[infatyINFATY]+)";

    // Copied from PyString
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

    // Copied from PyString
    /** Access only through {@link #getFloatPattern()}. */
    private static Pattern floatPattern = null;

    // Copied from PyString
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

    // Copied from PyString
    /** Access only through {@link #getComplexPattern()} */
    private static Pattern complexPattern = null;

    // Copied from PyString
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

    // Copied from PyString
    /**
     * Convert this PyString to a complex value according to Python rules.
     *
     * @return the value
     */
    private PyComplex atocx() {
        double x = 0.0, y = 0.0;
        Matcher m = getComplexPattern().matcher(asString());
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
            throw new ValueError(String.format(fmt, asString().trim()));
        }

    }

    // @formatter:off

    // Copied from PyString
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

    // Copied from PyString with this -> self
    private static BigInteger asciiToBigInteger(String self, int base, boolean isLong) {

        int b = 0;
        int e = self.length();

        while (b < e && Character.isWhitespace(self.charAt(b))) {
            b++;
        }

        while (e > b && Character.isWhitespace(self.charAt(e - 1))) {
            e--;
        }

        char sign = 0;
        if (b < e) {
            sign = self.charAt(b);
            if (sign == '-' || sign == '+') {
                b++;
                while (b < e && Character.isWhitespace(self.charAt(b))) {
                    b++;
                }
            }

            if (base == 16) {
                if (self.charAt(b) == '0') {
                    if (b < e - 1 && Character.toUpperCase(self.charAt(b + 1)) == 'X') {
                        b += 2;
                    }
                }
            } else if (base == 0) {
                if (self.charAt(b) == '0') {
                    if (b < e - 1 && Character.toUpperCase(self.charAt(b + 1)) == 'X') {
                        base = 16;
                        b += 2;
                    } else if (b < e - 1 && Character.toUpperCase(self.charAt(b + 1)) == 'O') {
                        base = 8;
                        b += 2;
                    } else if (b < e - 1 && Character.toUpperCase(self.charAt(b + 1)) == 'B') {
                        base = 2;
                        b += 2;
                    } else {
                        base = 8;
                    }
                }
            } else if (base == 8) {
                if (b < e - 1 && Character.toUpperCase(self.charAt(b + 1)) == 'O') {
                    b += 2;
                }
            } else if (base == 2) {
                if (b < e - 1 && Character.toUpperCase(self.charAt(b + 1)) == 'B') {
                    b += 2;
                }
            }
        }

        if (base == 0) {
            base = 10;
        }

        // if the base >= 22, then an 'l' or 'L' is a digit!
        if (isLong && base < 22 && e > b
                && (self.charAt(e - 1) == 'L' || self.charAt(e - 1) == 'l')) {
            e--;
        }

        String s = self;
        if (b > 0 || e < self.length()) {
            s = self.substring(b, e);
        }

        BigInteger bi;
        if (sign == '-') {
            bi = new BigInteger("-" + s, base);
        } else {
            bi = new BigInteger(s, base);
        }
        return bi;
    }

    // Copied from PyString
    public int atoi() {
        return atoi(10);
    }

    // Copied from PyString with this -> self
    public static int atoi(String self, int base) {
        if ((base != 0 && base < 2) || (base > 36)) {
            throw new ValueError("invalid base for atoi()");
        }

        try {
            BigInteger bi = asciiToBigInteger(self, base, false);
            if (bi.compareTo(PyLong.MAX_INT) > 0 || bi.compareTo(PyLong.MIN_INT) < 0) {
                throw new OverflowError("long int too large to convert to int");
            }
            return bi.intValue();
        } catch (NumberFormatException exc) {
            throw new ValueError(
                    "invalid literal for int() with base " + base + ": '" + self + "'");
        } catch (StringIndexOutOfBoundsException exc) {
            throw new ValueError(
                    "invalid literal for int() with base " + base + ": '" + self + "'");
        }
    }

    // Copied from PyString
    public PyLong atol() {
        return atol(10);
    }

    // Copied from PyString with this -> self
    public static PyLong atol(String self, int base) {
        // XXX Likely this belongs in PyLong
        if ((base != 0 && base < 2) || (base > 36)) {
            throw new ValueError("invalid base for long literal:" + base);
        }

        try {
            BigInteger bi = asciiToBigInteger(self, base, true);
            return new PyLong(PyLong.TYPE, bi); // XXX should return Object bi
        } catch (NumberFormatException | StringIndexOutOfBoundsException exc) {
            throw new ValueError(
                    "invalid literal for long() with base " + base + ": '" + self + "'");
        }
    }

    // Copied from PyString
    /**
     * Implements PEP-3101 {}-formatting method {@code str.format()}.
     * When called with {@code enclosingIterator == null}, this
     * method takes this object as its formatting string. The method is also called (calls itself)
     * to deal with nested formatting specifications. In that case, {@code enclosingIterator}
     * is a {@link MarkupIterator} on this object and {@code value} is a substring of this
     * object needing recursive translation.
     *
     * @param args to be interpolated into the string
     * @param keywords for the trailing args
     * @param enclosingIterator when used nested, null if subject is this {@code PyString}
     * @param value the format string when {@code enclosingIterator} is not null
     * @return the formatted string based on the arguments
     * @throws TypeError if {@code __repr__} or {@code __str__} conversions returned a non-string.
     * @throws Throwable from other errors in {@code __repr__} or {@code __str__}
     */
    // XXX make this support format(String) too
    private String buildFormattedString(Object[] args, String[] keywords,
            MarkupIterator enclosingIterator, String value) throws TypeError, Throwable {

        MarkupIterator it;
        if (enclosingIterator == null) {
            // Top-level call acts on this object.
            it = new MarkupIterator(this.asString());
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
                Object fieldObj = getFieldObject(chunk.fieldName, it.isBytes(), args, keywords);
                if (fieldObj == null) {
                    continue;
                }

                // The conversion specifier is s = __str__ or r = __repr__.
                if ("r".equals(chunk.conversion)) {
                    fieldObj = Abstract.repr(fieldObj);
                } else if ("s".equals(chunk.conversion)) {
                    fieldObj = Abstract.str(fieldObj);
                } else if (chunk.conversion != null) {
                    throw new ValueError("Unknown conversion specifier %s", chunk.conversion);
                }

                // The format_spec may be simple, or contained nested replacement fields.
                String formatSpec = chunk.formatSpec;
                if (chunk.formatSpecNeedsExpanding) {
                    if (enclosingIterator != null) {
                        // PEP 3101 says only 2 levels
                        throw new ValueError("Max string recursion exceeded");
                    }
                    // Recursively interpolate further args into chunk.formatSpec
                    formatSpec = buildFormattedString(args, keywords, it, formatSpec);
                }
                renderField(fieldObj, formatSpec, result);
            }
        }
        return result.toString();
    }

    // Copied from PyString
    /**
     * Return the object referenced by a given field name, interpreted in the context of the given
     * argument list, containing positional and keyword arguments.
     *
     * @param fieldName to interpret.
     * @param bytes true if the field name is from a PyString, false for PyUnicode.
     * @param args argument list (positional then keyword arguments).
     * @param keywords naming the keyword arguments.
     * @return the object designated or {@code null}.
     * @throws Throwable from errors accessing referenced fields
     */
    private Object getFieldObject(String fieldName, boolean bytes, Object[] args,
            String[] keywords) throws Throwable {
        FieldNameIterator iterator = new FieldNameIterator(fieldName, bytes);
        Object head = iterator.head();
        Object obj = null;
        int positionalCount = args.length - keywords.length;

        if (PyNumber.indexCheck(head)) {
            // The field name begins with an integer argument index (not a [n]-type index).
            int index = PyNumber.asSize(head, null);
            if (index >= positionalCount) {
                throw new IndexError("tuple index out of range");
            }
            obj = args[index];

        } else {
            // The field name begins with keyword.
            for (int i = 0; i < keywords.length; i++) {
                if (Abstract.richCompareBool(obj, keywords[i], Comparison.EQ)) {
                    obj = args[positionalCount + i];
                    break;
                }
            }
            // And if we don't find it, that's an error
            if (obj == null) {
                // throw new KeyError(head);
                throw new MissingFeature("dictionary");
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
                // key must be an attribute name
                obj = Abstract.getAttr(obj, key);
            } else {
                // obj = PySequence.getItem(obj, key);
                throw new MissingFeature("dictionary");
            }
        }

        return obj;
    }

    // Copied from PyString
    /**
     * Append to a formatting result, the presentation of one object, according to a given format
     * specification and the object's {@code __format__} method.
     *
     * @param fieldObj to format.
     * @param formatSpec specification to apply.
     * @param result to which the result will be appended.
     */
    private void renderField(Object fieldObj, String formatSpec, StringBuilder result) {
        String formatSpecStr = formatSpec == null ? "" : formatSpec;
        //result.append(fieldObj.__format__(formatSpecStr).asString());
        throw new MissingFeature("String formatting");
    }

    // @formatter:on

    /**
     * A {@link AbstractFormatter}, constructed from a {@link Spec},
     * with specific validations for {@code str.__format__}.
     */
    private static class StrFormatter extends TextFormatter {

        /**
         * Prepare a {@link TextFormatter} in support of
         * {@link PyUnicode#__format__(Object, Object) str.__format__}.
         *
         * @param spec a parsed PEP-3101 format specification.
         * @return a formatter ready to use.
         * @throws FormatOverflow if a value is out of range (including the
         *     precision)
         * @throws FormatError if an unsupported format character is
         *     encountered
         */
        StrFormatter(Spec spec) throws FormatError { super(validated(spec)); }

        @Override
        public TextFormatter format(Object self) throws NoConversion {
            return format(convertToString(self));
        }

        private static Spec validated(Spec spec) throws FormatError {
            String type = TYPE.name;
            switch (spec.type) {

                case Spec.NONE:
                case 's':
                    // Check for disallowed parts of the specification
                    if (spec.grouping) {
                        throw notAllowed("Grouping", type, spec.type);
                    } else if (Spec.specified(spec.sign)) {
                        throw signNotAllowed(type, '\0');
                    } else if (spec.alternate) {
                        throw alternateFormNotAllowed(type);
                    } else if (spec.align == '=') { throw alignmentNotAllowed('=', type); }
                    // Passed (whew!)
                    break;

                default:
                    // The type code was not recognised
                    throw unknownFormat(spec.type, type);
            }

            /*
             * spec may be incomplete. The defaults are those commonly used for
             * string formats.
             */
            return spec.withDefaults(Spec.STRING);
        }
    }

    // @formatter:off
    // Copied from _codecs
    // parallel to CPython's PyUnicode_TranslateCharmap
    static Object translateCharmap(PyUnicode str, String errors, Object mapping) {

        throw new MissingFeature("str.translate");
        /*
        StringBuilder buf = new StringBuilder(str.toString().length());

        for (Iterator<Integer> iter = str.newSubsequenceIterator(); iter.hasNext();) {
            int codePoint = iter.next();
            Object result = mapping.__finditem__(Py.newInteger(codePoint));
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
                throw new TypeError("character mapping must return integer, None or unicode");
            }
        }
        return new PyUnicode(buf.toString());
        */
    }
}
