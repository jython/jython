// Copyright (c)2021 Jython Developers.
// Licensed to PSF under a contributor agreement.
// @formatter:off
package org.python.core;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.python.base.MissingFeature;
import org.python.core.PySlice.Indices;
import org.python.core.stringlib.FieldNameIterator;
import org.python.core.stringlib.InternalFormat;
import org.python.core.stringlib.MarkupIterator;
import org.python.core.stringlib.TextFormatter;
import org.python.core.stringlib.InternalFormat.Formatter;
import org.python.core.stringlib.InternalFormat.Spec;
import org.python.modules.ucnhashAPI;

/**
 * The Python {@code str} object is implemented by both
 * {@code PyUnicode} and {@code String}. Most strings used as names
 * (keys) and text are quite satisfactorily represented by Java
 * {@code String}. All operations will produce the same result for
 * Python, whichever representation is used.
 * <p>
 * Java {@code String}s are compact, but where they contain non-BMP
 * characters, these are represented by a pair of code units. This
 * makes certain operations (such as indexing) expensive.
 * By contrast, a {@code PyUnicode} representation is
 * time-efficient, but each character occupies one {@code int}.
 */
/*
@ExposedType(name = "unicode", base = PyBaseString.class, doc = BuiltinDocs.unicode_doc)
*/
public class PyUnicode extends PyString implements CraftedPyObject, Iterable<Integer>, CharSequence {

    /**
     * Nearly every significant method comes in two versions: one applicable when the string
     * contains only basic plane characters, and one that is correct when supplementary characters
     * are also present. Set this constant <code>true</code> to treat all strings as containing
     * supplementary characters, so that these versions will be exercised in tests.
     */
    private static final boolean DEBUG_NON_BMP_METHODS = false;

    public static final PyType TYPE = PyType.fromClass(PyUnicode.class);

    // for PyJavaClass.init()
    public PyUnicode() {
        this(TYPE, "", true);
    }

    /**
     * Construct a PyUnicode interpreting the Java String argument as UTF-16.
     *
     * @param string UTF-16 string encoding the characters (as Java).
     */
    public PyUnicode(String string) {
        this(TYPE, string, false);
    }

    /**
     * Construct a PyUnicode interpreting the Java String argument as UTF-16. If it is known that
     * the string contains no supplementary characters, argument isBasic may be set true by the
     * caller. If it is false, the PyUnicode will scan the string to find out.
     *
     * @param string UTF-16 string encoding the characters (as Java).
     * @param isBasic true if it is known that only BMP characters are present.
     */
    public PyUnicode(String string, boolean isBasic) {
        this(TYPE, string, isBasic);
    }

    public PyUnicode(PyType subtype, String string) {
        this(subtype, string, false);
    }

    public PyUnicode(PyString pystring) {
        this(TYPE, pystring);
    }

    public PyUnicode(PyType subtype, PyString pystring) {
        this(subtype, //
                pystring instanceof PyUnicode ? pystring.string : pystring.decode().toString(), //
                pystring.isBasicPlane());
    }

    public PyUnicode(char c) {
        this(TYPE, String.valueOf(c), true);
    }

    public PyUnicode(int codepoint) {
        this(TYPE, new String(new int[] {codepoint}, 0, 1));
    }

    public PyUnicode(int[] codepoints) {
        this(new String(codepoints, 0, codepoints.length));
    }

    PyUnicode(StringBuilder buffer) {
        this(TYPE, buffer.toString());
    }

    private static StringBuilder fromCodePoints(Iterator<Integer> iter) {
        StringBuilder buffer = new StringBuilder();
        while (iter.hasNext()) {
            buffer.appendCodePoint(iter.next());
        }
        return buffer;
    }

    public PyUnicode(Iterator<Integer> iter) {
        this(fromCodePoints(iter));
    }

    public PyUnicode(Collection<Integer> ucs4) {
        this(ucs4.iterator());
    }

    /**
     * Fundamental all-features constructor on which the others depend. If it is known that the
     * string contains no supplementary characters, argument isBasic may be set true by the caller.
     * If it is false, the PyUnicode will scan the string to find out.
     *
     * @param subtype actual type to create.
     * @param string UTF-16 string encoding the characters (as Java).
     * @param isBasic true if it is known that only BMP characters are present.
     */
    private PyUnicode(PyType subtype, String string, boolean isBasic) {
        super(subtype, "");
        this.string = string;
        translator = isBasic ? BASIC : this.chooseIndexTranslator();
    }

    @Override
    public int[] toCodePoints() {
        int n = getCodePointCount();
        int[] codePoints = new int[n];
        int i = 0;
        for (Iterator<Integer> iter = newSubsequenceIterator(); iter.hasNext(); i++) {
            codePoints[i] = iter.next();
        }
        return codePoints;
    }

    public PyType getType() { return TYPE; }

    // Copied from PyString
    public String asString() { return getString(); }


    // ------------------------------------------------------------------------------------------
    // Index translation for Unicode beyond the BMP
    // ------------------------------------------------------------------------------------------

    /**
     * Index translation between code point index (as seen by Python) and UTF-16 index (as used in
     * the Java String.
     */
    private interface IndexTranslator extends Serializable {

        /** Number of supplementary characters (hence point code length may be found). */
        public int suppCount();

        /** Translate a UTF-16 code unit index to its equivalent code point index. */
        public int codePointIndex(int utf16Index);

        /** Translate a code point index to its equivalent UTF-16 code unit index. */
        public int utf16Index(int codePointIndex);
    }

    /**
     * The instance of index translation in use in this string. It will be set to either
     * {@link #BASIC} or an instance of {@link PyUnicode.Supplementary}.
     */
    private final IndexTranslator translator;

    /**
     * A singleton provides the translation service (which is a pass-through) for all BMP strings.
     */
    static final IndexTranslator BASIC = new IndexTranslator() {

        @Override
        public int suppCount() {
            return 0;
        }

        @Override
        public int codePointIndex(int u) {
            return u;
        }

        @Override
        public int utf16Index(int i) {
            return i;
        }
    };

    /**
     * A class of index translation that uses the cumulative count so far of supplementary
     * characters, tabulated in blocks of a standard size. The count is then used as an offset
     * between the code point index and the corresponding point in the UTF-16 representation.
     */
    private final class Supplementary implements IndexTranslator {

        /** Tabulates cumulative count so far of supplementary characters, by blocks of size M. */
        final int[] count;

        /** Configure the block size M, as this power of 2. */
        static final int LOG2M = 4;
        /** The block size used for indexing (power of 2). */
        static final int M = 1 << LOG2M;
        /** A mask used to separate the block number and offset in the block. */
        static final int MASK = M - 1;

        /**
         * The constructor works on a count array prepared by
         * {@link PyUnicode#getSupplementaryCounts(String)}.
         */
        Supplementary(int[] count) {
            this.count = count;
        }

        @Override
        public int codePointIndex(int u) {
            /*
             * Let the desired result be j such that utf16Index(j) = u. As we have only a forward
             * index of the string, we have to conduct a search. In principle, we bound j by a pair
             * of values (j1,j2) such that j1<=j<j2, and in successive iterations, we shorten the
             * range until a unique j is found. In the first part of the search, we work in terms of
             * block numbers, that is, indexes into the count array. We have j<u, and so j<k2*M
             * where:
             */
            int k2 = (u >> LOG2M) + 1;
            // The count of supplementary characters before the start of block k2 is:
            int c2 = count[k2 - 1];
            /*
             * Since the count array is non-decreasing, and j < k2*M, we have u-j <= count[k2-1].
             * That is, j >= k1*M, where:
             */
            int k1 = Math.max(0, u - c2) >> LOG2M;
            // The count of supplementary characters before the start of block k1 is:
            int c1 = (k1 == 0) ? 0 : count[k1 - 1];

            /*
             * Now, j (to be found) is in an unknown block k, where k1<=k<k2. We make a binary
             * search, maintaining the inequalities, but moving the end points. When k2=k1+1, we
             * know that j must be in block k1.
             */
            while (true) {
                if (c2 == c1) {
                    // We can stop: the region contains no supplementary characters so j is:
                    return u - c1;
                }
                // Choose a candidate k in the middle of the range
                int k = (k1 + k2) / 2;
                if (k == k1) {
                    // We must have k2=k1+1, so j is in block k1
                    break;
                } else {
                    // kx divides the range: is j<kx*M?
                    int c = count[k - 1];
                    if ((k << LOG2M) + c > u) {
                        // k*M+c > u therefore j is not in block k but to its left.
                        k2 = k;
                        c2 = c;
                    } else {
                        // k*M+c <= u therefore j must be in block k, or to its right.
                        k1 = k;
                        c1 = c;
                    }
                }
            }

            /*
             * At this point, j is known to be in block k1 (and k2=k1+1). c1 is the number of
             * supplementary characters to the left of code point index k1*M and c2 is the number of
             * supplementary characters to the left of code point index (k1+1)*M. We have to search
             * this block sequentially. The current position in the UTF-16 is:
             */
            int p = (k1 << LOG2M) + c1;
            while (p < u) {
                if (Character.isHighSurrogate(string.charAt(p++))) {
                    // c1 tracks the number of supplementary characters to the left of p
                    c1 += 1;
                    if (c1 == c2) {
                        // We have found all supplementary characters in the block.
                        break;
                    }
                    // Skip the trailing surrogate.
                    p++;
                }
            }
            // c1 is the number of supplementary characters to the left of u, so the result j is:
            return u - c1;
        }

        @Override
        public int utf16Index(int i) {
            // The code point index i lies in the k-th block where:
            int k = i >> LOG2M;
            // The offset for the code point index k*M is exactly
            int d = (k == 0) ? 0 : count[k - 1];
            // The offset for the code point index (k+1)*M is exactly
            int e = count[k];
            if (d == e) {
                /*
                 * The offset for the code point index (k+1)*M is the same, and since this is a
                 * non-decreasing function of k, it is also the value for i.
                 */
                return i + d;
            } else {
                /*
                 * The offset for the code point index (k+1)*M is different (higher). We must scan
                 * along until we have found all the supplementary characters that precede i,
                 * starting the scan at code point index k*M.
                 */
                for (int q = i & ~MASK; q < i; q++) {
                    if (Character.isHighSurrogate(string.charAt(q + d))) {
                        d += 1;
                        if (d == e) {
                            /*
                             * We have found all the supplementary characters in this block, so we
                             * must have found all those to the left of i.
                             */
                            break;
                        }
                    }
                }

                // d counts all the supplementary characters to the left of i.
                return i + d;
            }
        }

        @Override
        public int suppCount() {
            // The last element of the count array is the total number of supplementary characters.
            return count[count.length - 1];
        }
    }

    /**
     * Generate the table that is used by the class {@link Supplementary} to accelerate access to
     * the the implementation string. The method returns <code>null</code> if the string passed
     * contains no surrogate pairs, in which case we'll use {@link #BASIC} as the translator. This
     * method is sensitive to {@link #DEBUG_NON_BMP_METHODS} which if true will prevent it returning
     * null, hance we will always use a {@link Supplementary} {@link #translator}.
     *
     * @param string to index
     * @return the index (counts) or null if basic plane
     */
    private static int[] getSupplementaryCounts(final String string) {

        final int n = string.length();
        int p; // Index of the current UTF-16 code unit.

        /*
         * We scan to the first surrogate code unit, in a simple loop. If we hit the end before we
         * find one, no count array will be necessary and we'll use BASIC. If we find a surrogate it
         * may be half a supplementary character, or a lone surrogate: we'll find out later.
         */
        for (p = 0; p < n; p++) {
            if (Character.isSurrogate(string.charAt(p))) {
                break;
            }
        }

        if (p == n && !DEBUG_NON_BMP_METHODS) {
            // There are no supplementary characters so the 1:1 translator is fine.
            return null;

        } else {
            /*
             * We have to do this properly, using a scheme in which code point indexes are
             * efficiently translatable to UTF-16 indexes through a table called here count[]. In
             * this array, count[k] contains the total number of supplementary characters up to the
             * end of the k.th block, that is, to the left of code point (k+1)M. We have to fill
             * this array by scanning the string.
             */
            int q = p; // The current code point index (q = p+s).
            int k = q >> Supplementary.LOG2M; // The block number k = q/M.

            /*
             * When addressing with a code point index q<=L (the length in code points) we will
             * index the count array with k = q/M. We have q<=L<=n, therefore q/M <= n/M, the
             * maximum valid k is 1 + n/M. A q>=L should raise IndexOutOfBoundsException, but it
             * doesn't matter whether that's from indexing this array, or the string later.
             */
            int[] count = new int[1 + (n >> Supplementary.LOG2M)];

            /*
             * To get the generation of count[] going efficiently, we need to advance the next whole
             * block. The next loop will complete processing of the block containing the first
             * supplementary character. Note that in all these loops, if we exit because p reaches a
             * limit, the count for the last partial block is known from p-q and we take care of
             * that right at the end of this method. The limit of these loops is n-1, so if we spot
             * a lead surrogate, the we may access the low-surrogate confident that p+1<n.
             */
            while (p < n - 1) {

                // Catch supplementary characters and lone surrogate code units.
                p += calcAdvance(string, p);
                // Advance the code point index
                q += 1;

                // Was that the last in a block?
                if ((q & Supplementary.MASK) == 0) {
                    count[k++] = p - q;
                    break;
                }
            }

            /*
             * If the string is long enough, we can work in whole blocks of M, and there are fewer
             * things to track. We can't know the number of blocks in advance, but we know there is
             * at least one whole block to go when p+2*M<n.
             */
            while (p + 2 * Supplementary.M < n) {

                for (int i = 0; i < Supplementary.M; i++) {
                    // Catch supplementary characters and lone surrogate code units.
                    p += calcAdvance(string, p);
                }

                // Advance the code point index one whole block
                q += Supplementary.M;

                // The number of supplementary characters to the left of code point index k*M is:
                count[k++] = p - q;
            }

            /*
             * Process the remaining UTF-16 code units, except possibly the last.
             */
            while (p < n - 1) {

                // Catch supplementary characters and lone surrogate code units.
                p += calcAdvance(string, p);
                // Advance the code point index
                q += 1;

                // Was that the last in a block?
                if ((q & Supplementary.MASK) == 0) {
                    count[k++] = p - q;
                }
            }

            /*
             * There may be just one UTF-16 unit left (if the last thing processed was not a
             * surrogate pair).
             */
            if (p < n) {
                // We are at the last UTF-16 unit in string. Any surrogate here is an error.
                char c = string.charAt(p++);
                if (Character.isSurrogate(c)) {
                    throw unpairedSurrogate(p - 1, c);
                }
                // Advance the code point index
                q += 1;
            }

            /*
             * There may still be some elements of count[] we haven't set, so we fill to the end
             * with the total count. This also takes care of an incomplete final block.
             */
            int total = p - q;
            while (k < count.length) {
                count[k++] = total;
            }

            return count;
        }
    }

    /**
     * Called at each code point index, returns 2 if this is a surrogate pair, 1 otherwise, and
     * detects lone surrogates as an error. The return is the amount to advance the UTF-16 index. An
     * exception is raised if at <code>p</code> we find a lead surrogate without a trailing one
     * following, or a trailing surrogate directly. It should not be called on the final code unit,
     * when <code>p==string.length()-1</code>, since it may check the next code unit as well.
     *
     * @param string of UTF-16 code units
     * @param p index into that string
     * @return 2 if a surrogate pair stands at <code>p</code>, 1 if not
     * @throws PyException {@code ValueError} if a lone surrogate stands at <code>p</code>.
     */
    private static int calcAdvance(String string, int p) throws PyException {

        // Catch supplementary characters and lone surrogate code units.
        char c = string.charAt(p);

        if (c >= Character.MIN_SURROGATE) {
            if (c < Character.MIN_LOW_SURROGATE) {
                // This is a lead surrogate.
                if (Character.isLowSurrogate(string.charAt(p + 1))) {
                    // Required trailing surrogate follows, so step over both.
                    return 2;
                } else {
                    // Required trailing surrogate missing.
                    throw unpairedSurrogate(p, c);
                }

            } else if (c <= Character.MAX_SURROGATE) {
                // This is a lone trailing surrogate
                throw unpairedSurrogate(p, c);

            } // else this is a private use or special character in 0xE000 to 0xFFFF.

        }
        return 1;
    }

    /**
     * Return a ready-to-throw exception indicating an unpaired surrogate.
     *
     * @param p index within that sequence of the problematic code unit
     * @param c the code unit
     * @return an exception
     */
    private static PyException unpairedSurrogate(int p, int c) {
        String fmt = "unpaired surrogate %#4x at code unit %d";
        String msg = String.format(fmt, c, p);
        return new ValueError(msg);
    }

    /**
     * Choose an {@link IndexTranslator} implementation for efficient working, according to the
     * contents of the {@link PyString#string}.
     *
     * @return chosen <code>IndexTranslator</code>
     */
    private IndexTranslator chooseIndexTranslator() {
        int[] count = getSupplementaryCounts(string);
        if (DEBUG_NON_BMP_METHODS) {
            return new Supplementary(count);
        } else {
            return count == null ? BASIC : new Supplementary(count);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * In the <code>PyUnicode</code> version, the arguments are code point indices, such as are
     * received from the Python caller, while the first two elements of the returned array have been
     * translated to UTF-16 indices in the implementation string.
     */
    protected int[] translateIndices(PyObject start, PyObject end) {
        int[] indices = translateIndices_PyString(start, end);
        indices[0] = translator.utf16Index(indices[0]);
        indices[1] = translator.utf16Index(indices[1]);
        // indices[2] and [3] remain Unicode indices (and may be out of bounds) relative to len()
        return indices;
    }

    // ------------------------------------------------------------------------------------------

    /**
     * {@inheritDoc} The indices are code point indices, not UTF-16 (<code>char</code>) indices. For
     * example:
     *
     * <pre>
     * PyUnicode u = new PyUnicode("..\ud800\udc02\ud800\udc03...");
     * // (Python) u = u'..\U00010002\U00010003...'
     *
     * String s = u.substring(2, 4);  // = "\ud800\udc02\ud800\udc03" (Java)
     * </pre>
     */
    @Override
    public String substring(int start, int end) {
        return super.substring(translator.utf16Index(start), translator.utf16Index(end));
    }

    /**
     * Creates a PyUnicode from an already interned String. Just means it won't be reinterned if
     * used in a place that requires interned Strings.
     */
    public static PyUnicode fromInterned(String interned) {
        PyUnicode uni = new PyUnicode(TYPE, interned);
        uni.interned = true;
        return uni;
    }

    /**
     * {@inheritDoc}
     *
     * @return true if the string consists only of BMP characters
     */
    @Override
    public boolean isBasicPlane() {
        return translator == BASIC;
    }

    public int getCodePointCount() {
        return string.length() - translator.suppCount();
    }

    public static String checkEncoding(String s) {
        if (s == null || s.chars().allMatch(c->c<128)) {
            return s;
        }
        return codecs.PyUnicode_EncodeASCII(s, s.length(), null);
    }

    // @formatter:off
    /*
    @ExposedNew
    final static PyObject unicode_new(PyNewWrapper new_, boolean init, PyType subtype,
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

    // XXX Eventually remove entirely
    /**
     * Create an instance of the same type as this object, from the Java String given as argument.
     * This is to be overridden in a subclass to return its own type.
     *
     * @param str to wrap
     * @return instance wrapping {@code str}
     */
    public PyUnicode createInstance(String str) {
        return new PyUnicode(str);
    }

    // XXX Return PyUnicode or remove entirely
    /**
     * Create an instance of the same type as this object, from the Java String given as argument.
     * This is to be overridden in a subclass to return its own type.
     *
     * @param str Java string representing the characters (as Java UTF-16).
     * @param isBasic is ignored in <code>PyString</code> (effectively true).
     * @return instance wrapping {@code str}
     */
    protected PyString createInstance(String string, boolean isBasic) {
        return new PyUnicode(string, isBasic);
    }


    // Special methods -----------------------------------------------

    public PyObject __mod__(PyObject other) {
        return unicode___mod__(other);
    }

    /*
    @ExposedMethod(doc = BuiltinDocs.unicode___mod___doc)
    */
    final PyObject unicode___mod__(PyObject other) {
        StringFormatter fmt = new StringFormatter(getString(), true);
        return fmt.format(other);
    }

    public PyUnicode __unicode__() {
        return this;
    }

    public PyString __str__() {
        return unicode___str__();
    }

    /*
    @ExposedMethod(doc = BuiltinDocs.unicode___str___doc)
    */
    final PyString unicode___str__() {
        return new PyString(encode());
    }

    public int __len__() {
        return unicode___len__();
    }

    /*
    @ExposedMethod(doc = BuiltinDocs.unicode___len___doc)
    */
    final int unicode___len__() {
        return getCodePointCount();
    }

    public PyString __repr__() {
        return unicode___repr__();
    }

    /*
    @ExposedMethod(doc = BuiltinDocs.unicode___repr___doc)
    */
    final PyString unicode___repr__() {
        return new PyString("u" + encode_UnicodeEscape(getString(), true));
    }

    /*
    @ExposedMethod(doc = BuiltinDocs.unicode___getitem___doc)
    */
    final PyObject unicode___getitem__(PyObject index) {
        return str___getitem__(index);
    }

    // Copied from PyString
    /*
    @ExposedMethod(doc = BuiltinDocs.str___getitem___doc)
    */
    final PyObject str___getitem__(PyObject index) {
        PyObject ret = seq___finditem__(index);
        if (ret == null) {
            throw new IndexError("string index out of range");
        }
        return ret;
    }

    // Temporary to satisfy references
    private PyObject seq___finditem__(PyObject index) {
        return null;
    }

    /*
    @ExposedMethod(defaults = "null", doc = BuiltinDocs.unicode___getslice___doc)
    */
    final PyObject unicode___getslice__(PyObject start, PyObject stop, PyObject step) {
        return seq___getslice__(start, stop, step);
    }

    // Temporary to satisfy references
    private PyObject seq___getslice__(PyObject start, PyObject stop, PyObject step) {
        return null;
    }

    protected PyObject getslice(int start, int stop, int step) {
        if (isBasicPlane()) {
            return getslice_PyString(start, stop, step);
        }
        if (step > 0 && stop < start) {
            stop = start;
        }

        StringBuilder buffer = new StringBuilder(sliceLength(start, stop, step));
        for (Iterator<Integer> iter = newSubsequenceIterator(start, stop, step); iter.hasNext();) {
            buffer.appendCodePoint(iter.next());
        }
        return createInstance(buffer.toString());
    }

    // Copied from PyString
    protected PyObject getslice_PyString(int start, int stop, int step) {
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

    // Temporary to satisfy references
    private int sliceLength(int start, int stop, int step) {
       Indices i;
        try {
            i = (new  PySlice(start, stop, step)).getIndices(__len__());
            return i.slicelength;
        } catch (Throwable e) {
            return 0;
        }
    }

    /*
    @ExposedMethod(type = MethodType.CMP)
    */
    final int unicode___cmp__(PyObject other) {
        // XXX needs proper coercion like __eq__, then UCS-32 code point order :(
        return str___cmp__(other);
    }

    // Copied from PyString
    /*
    @ExposedMethod(type = MethodType.CMP)
    */
    final int str___cmp__(PyObject other) {
        if (!(other instanceof PyString)) {
            return -2;
        }

        int c = getString().compareTo(((PyString) other).getString());
        return c < 0 ? -1 : c > 0 ? 1 : 0;
    }

    /*
    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.unicode___eq___doc)
    */
    final Object unicode___eq__(PyObject other) {
        try {
            String s = coerceForComparison(other);
            if (s == null) {
                return null;
            }
            return getString().equals(s) ? Py.True : Py.False;
        } catch (PyException e) {
            // Decoding failed: treat as unequal
            return Py.False;
        }
    }

    /*
    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.unicode___ne___doc)
    */
    final Object unicode___ne__(PyObject other) {
        try {
            String s = coerceForComparison(other);
            if (s == null) {
                return null;
            }
            return getString().equals(s) ? Py.False : Py.True;
        } catch (PyException e) {
            // Decoding failed: treat as unequal
            return Py.True;
        }
    }

    /*
    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.unicode___lt___doc)
    */
    final Object unicode___lt__(PyObject other) {
        String s = coerceForComparison(other);
        if (s == null) {
            return null;
        }
        return getString().compareTo(s) < 0 ? Py.True : Py.False;
    }

    /*
    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.unicode___le___doc)
    */
    final Object unicode___le__(PyObject other) {
        String s = coerceForComparison(other);
        if (s == null) {
            return null;
        }
        return getString().compareTo(s) <= 0 ? Py.True : Py.False;
    }

    /*
    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.unicode___gt___doc)
    */
    final Object unicode___gt__(PyObject other) {
        String s = coerceForComparison(other);
        if (s == null) {
            return null;
        }
        return getString().compareTo(s) > 0 ? Py.True : Py.False;
    }

    /*
    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.unicode___ge___doc)
    */
    final Object unicode___ge__(PyObject other) {
        String s = coerceForComparison(other);
        if (s == null) {
            return null;
        }
        return getString().compareTo(s) >= 0 ? Py.True : Py.False;
    }

    /*
    @ExposedMethod(doc = BuiltinDocs.unicode___hash___doc)
    */
    final int unicode___hash__() {
        return getString().hashCode();
    }

    // Copied from PyString
    public Object __tojava__(Class<?> c) {
        // XXX something like this necessary in Jython 3 but not used yet
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
//            if (c.getComponentType() == Byte.TYPE) {
//                return toBytes();
//            }
            if (c.getComponentType() == Character.TYPE) {
                return getString().toCharArray();
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

    protected PyObject pyget(int i) {
        int codepoint = getString().codePointAt(translator.utf16Index(i));
        return new PyUnicode(codepoint);
    }

    public int getInt(int i) {
        return getString().codePointAt(translator.utf16Index(i));
    }

    /**
     * An iterator returning code points from this array, for use when not basic plane.
     */
    private class SubsequenceIteratorImpl extends SubsequenceIteratorBasic {

        private int k; // UTF-16 index (of current)

        SubsequenceIteratorImpl(int start, int stop, int step) {
            super(start, stop, step);
            k = translator.utf16Index(current);
        }

        SubsequenceIteratorImpl() {
            this(0, getCodePointCount(), 1);
        }

        @Override
        protected int nextCodePoint() {
            int U;
            int W1 = getString().charAt(k);
            if (W1 >= 0xD800 && W1 < 0xDC00) {
                int W2 = getString().charAt(k + 1);
                U = (((W1 & 0x3FF) << 10) | (W2 & 0x3FF)) + 0x10000;
                k += 2;
            } else {
                U = W1;
                k += 1;
            }
            current += 1;
            return U;
        }
    }

    /**
     * An iterator returning code points from this array, for use when basic plane.
     */
    private class SubsequenceIteratorBasic implements Iterator<Integer> {

        protected int current, stop, step; // Character indexes

        SubsequenceIteratorBasic(int start, int stop, int step) {
            current = start;
            this.stop = stop;
            this.step = step;
        }

        SubsequenceIteratorBasic() {
            this(0, getCodePointCount(), 1);
        }

        @Override
        public boolean hasNext() {
            return current < stop;
        }

        @Override
        public Integer next() {
            int codePoint = nextCodePoint();
            for (int j = 1; j < step && hasNext(); j++) {
                nextCodePoint();
            }
            return codePoint;
        }

        protected int nextCodePoint() {
            return getString().charAt(current++);
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException(
                    "Not supported on PyUnicode objects (immutable)");
        }
    }

    private static class SteppedIterator<T> implements Iterator<T> {

        private final Iterator<T> iter;
        private final int step;
        private T lookahead = null;

        public SteppedIterator(int step, Iterator<T> iter) {
            this.iter = iter;
            this.step = step;
            lookahead = advance();
        }

        private T advance() {
            if (iter.hasNext()) {
                T elem = iter.next();
                for (int i = 1; i < step && iter.hasNext(); i++) {
                    iter.next();
                }
                return elem;
            } else {
                return null;
            }
        }

        @Override
        public boolean hasNext() {
            return lookahead != null;
        }

        @Override
        public T next() {
            T old = lookahead;
            if (iter.hasNext()) {
                lookahead = iter.next();
                for (int i = 1; i < step && iter.hasNext(); i++) {
                    iter.next();
                }
            } else {
                lookahead = null;
            }
            return old;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    // XXX: Parameterize SubsequenceIteratorImpl and friends (and make them Iterable)
    /** Get an iterator over the code point sequence. */
    public Iterator<Integer> newSubsequenceIterator() {
        if (isBasicPlane()) {
            return new SubsequenceIteratorBasic();
        } else {
            return new SubsequenceIteratorImpl();
        }
    }

    /** Get an iterator over a slice of the code point sequence. */
    public Iterator<Integer> newSubsequenceIterator(int start, int stop, int step) {
        if (isBasicPlane()) {
            if (step < 0) {
                return new SteppedIterator<Integer>(step * -1, new ReversedIterator<Integer>(
                        new SubsequenceIteratorBasic(stop + 1, start + 1, 1)));
            } else {
                return new SubsequenceIteratorBasic(start, stop, step);
            }
        } else {
            if (step < 0) {
                return new SteppedIterator<Integer>(step * -1, new ReversedIterator<Integer>(
                        new SubsequenceIteratorImpl(stop + 1, start + 1, 1)));
            } else {
                return new SubsequenceIteratorImpl(start, stop, step);
            }
        }
    }

    /**
     * Interpret the object as a Java <code>String</code> representing characters as UTF-16, or
     * return <code>null</code> if the type does not admit this conversion. From a
     * <code>PyUnicode</code> we return its internal string. A byte argument is decoded with the
     * default encoding.
     *
     * @param o the object to coerce
     * @return an equivalent <code>String</code>
     */
    private static String coerceToStringOrNull(PyObject o) {
        if (o instanceof PyUnicode) {
            return ((PyUnicode) o).getString();
        } else if (o instanceof PyString) {
            return ((PyString) o).decode().toString();
//        } else if (o instanceof BufferProtocol) {
//            // PyByteArray, PyMemoryView, Py2kBuffer ...
//            // We ought to be able to call codecs.decode on o but see Issue #2164
//            try (PyBuffer buf = ((BufferProtocol) o).getBuffer(PyBUF.FULL_RO)) {
//                PyString s = new PyString(buf);
//                // For any sensible codec, the return is unicode and toString() is getString().
//                return s.decode().toString();
//            }
        } else {
            // o is some type not allowed:
            return null;
        }
    }

    /**
     * Interpret the object as a Java <code>String</code> for use in comparison. The return
     * represents characters as UTF-16. From a <code>PyUnicode</code> we return its internal string.
     * A <code>str</code> and <code>buffer</code> argument is decoded with the default encoding.
     * <p>
     * This method could be replaced by {@link #coerceToStringOrNull(PyObject)} if we were content
     * to allowing a wider range of types to be supported in comparison operations than (C)Python
     * <code>unicode.__eq__</code>.
     *
     * @param o the object to coerce
     * @return an equivalent <code>String</code>
     */
    private static String coerceForComparison(PyObject o) {
        if (o instanceof PyUnicode) {
            return ((PyUnicode) o).getString();
        } else if (o instanceof PyString) {
            return ((PyString) o).decode().toString();
//        } else if (o instanceof Py2kBuffer) {
//            // We ought to be able to call codecs.decode on o but see Issue #2164
//            try (PyBuffer buf = ((BufferProtocol) o).getBuffer(PyBUF.FULL_RO)) {
//                PyString s = new PyString(buf);
//                // For any sensible codec, the return is unicode and toString() is getString().
//                return s.decode().toString();
//            }
        } else {
            // o is some type not allowed:
            return null;
        }
    }

    /**
     * Interpret the object as a Java <code>String</code> representing characters as UTF-16, or
     * raise an error if the type does not admit this conversion. A byte argument is decoded with
     * the default encoding.
     *
     * @param o the object to coerce
     * @return an equivalent <code>String</code> (and never <code>null</code>)
     */
    private static String coerceToString(PyObject o) {
        String s = coerceToStringOrNull(o);
        if (s == null) {
            throw errorCoercingToUnicode(o);
        }
        return s;
    }

    /**
     * Interpret the object as a Java <code>String</code> representing characters as UTF-16, or
     * optionally as <code>null</code> (for a <code>null</code> or <code>None</code> argument if the
     * second argument is <code>true</code>). Raise an error if the type does not admit this
     * conversion.
     *
     * @param o the object to coerce
     * @param allowNullArgument iff <code>true</code> allow a null or <code>none</code> argument
     * @return an equivalent <code>String</code> or <code>null</code>
     */
    private static String coerceToString(PyObject o, boolean allowNullArgument) {
        if (allowNullArgument && (o == null || o == Py.None)) {
            return null;
        } else {
            return coerceToString(o);
        }
    }

    /** Construct exception "coercing to Unicode: ..." */
    private static PyException errorCoercingToUnicode(PyObject o) {
        return Abstract.requiredTypeError("coercing to Unicode: a string or buffer",
                o == null ? Py.None : o);
    }

    /**
     * Interpret the object as a <code>PyUnicode</code>, or return <code>null</code> if the type
     * does not admit this conversion. From a <code>PyUnicode</code> we return itself. A byte
     * argument is decoded with the default encoding.
     *
     * @param o the object to coerce
     * @return an equivalent <code>PyUnicode</code> (or o itself)
     */
    private static PyUnicode coerceToUnicodeOrNull(PyObject o) {
        if (o instanceof PyUnicode) {
            return (PyUnicode) o;
        } else if (o instanceof PyString) {
            // For any sensible codec, the return here is unicode.
            PyObject u = ((PyString) o).decode();
            return (u instanceof PyUnicode) ? (PyUnicode) u : new PyUnicode(o.toString());
//        } else if (o instanceof BufferProtocol) {
//            // PyByteArray, PyMemoryView, Py2kBuffer ...
//            // We ought to be able to call codecs.decode on o but see Issue #2164
//            try (PyBuffer buf = ((BufferProtocol) o).getBuffer(PyBUF.FULL_RO)) {
//                PyString s = new PyString(buf);
//                // For any sensible codec, the return is unicode and toString() is getString().
//                PyObject u = s.decode();
//                return (u instanceof PyUnicode) ? (PyUnicode) u : new PyUnicode(o.toString());
//            }
        } else {
            // o is some type not allowed:
            return null;
        }
    }

    /**
     * Interpret the object as a <code>PyUnicode</code>, or raise a <code>TypeError</code> if the
     * type does not admit this conversion. From a <code>PyUnicode</code> we return itself. A byte
     * argument is decoded with the default encoding.
     *
     * @param o the object to coerce
     * @return an equivalent <code>PyUnicode</code> (or o itself)
     */
    private static PyUnicode coerceToUnicode(PyObject o) {
        PyUnicode u = coerceToUnicodeOrNull(o);
        if (u == null) {
            throw errorCoercingToUnicode(o);
        }
        return u;
    }

    public boolean __contains__(PyObject o) {
        return unicode___contains__(o);
    }

    /*
    @ExposedMethod(doc = BuiltinDocs.unicode___contains___doc)
    */
    final boolean unicode___contains__(PyObject o) {
        String other = coerceToString(o);
        return getString().indexOf(other) >= 0;
    }

    // Copied from PyString
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
            throw new OverflowError("max str len is " + Integer.MAX_VALUE);
        }
        char new_chars[] = new char[s * count];
        for (int i = 0; i < count; i++) {
            getString().getChars(0, s, new_chars, i * s);
        }
        return createInstance(new String(new_chars));
    }

    /*
    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.unicode___mul___doc)
    */
    final PyObject unicode___mul__(PyObject o) {
        return str___mul__(o);
    }

    // Copied from PyString
    /*
    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.str___mul___doc)
    */
    final PyObject str___mul__(PyObject o) {
        if (!o.isIndex()) {
            return null;
        }
        return repeat(o.asIndex(Py.OverflowError));
    }

    /*
    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.unicode___rmul___doc)
    */
    final PyObject unicode___rmul__(PyObject o) {
        return str___rmul__(o);
    }

    // Copied from PyString
    /*
    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.str___rmul___doc)
    */
    final PyObject str___rmul__(PyObject o) {
        if (!o.isIndex()) {
            return null;
        }
        return repeat(o.asIndex(Py.OverflowError));
    }

    /**
     * For a <code>str</code> addition means concatenation and returns a
     * <code>str</code> ({@link PyString}) result, except when a {@link PyUnicode} argument is
     * given, when a <code>PyUnicode</code> results.
     */
    public PyObject __add__(PyObject other) {
        return unicode___add__(other);
    }

    /*
    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.unicode___add___doc)
    */
    final PyObject unicode___add__(PyObject other) {
        // Interpret other as a Java String
        String s = coerceToStringOrNull(other);
        return s == null ? null : new PyUnicode(getString().concat(s));
    }

    // Copied from PyString
    /*
    @ExposedMethod(doc = BuiltinDocs.str___getnewargs___doc)
    */
    final PyTuple str___getnewargs__() {
        return new PyTuple(new PyString(this.getString()));
    }

    // Copied from PyString
    public PyTuple __getnewargs__() {
        return str___getnewargs__();
    }

    // Copied from PyString
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

    // Copied from PyString
    public PyFloat __float__() {
        return new PyFloat(atof());
    }

    // Copied from PyString
    public PyObject __pos__() {
        throw new TypeError("bad operand type for unary +");
    }

    // Copied from PyString
    public PyObject __neg__() {
        throw new TypeError("bad operand type for unary -");
    }

    // Copied from PyString
    public PyObject __invert__() {
        throw new TypeError("bad operand type for unary ~");
    }

    // Copied from PyString
    public PyComplex __complex__() {
        return atocx(this);
    }

    /*
    @ExposedMethod(doc = BuiltinDocs.unicode_lower_doc)
    */
    final PyObject unicode_lower() {
        return new PyUnicode(getString().toLowerCase());
    }

    /*
    @ExposedMethod(doc = BuiltinDocs.unicode_upper_doc)
    */
    final PyObject unicode_upper() {
        return new PyUnicode(getString().toUpperCase());
    }

    // Copied from PyString
    // Bit to twiddle (XOR) for lowercase letter to uppercase and vice-versa.
    private static final int SWAP_CASE = 0x20;

    /*
    @ExposedMethod(doc = BuiltinDocs.unicode_title_doc)
    */
    final PyObject unicode_title() {
        StringBuilder buffer = new StringBuilder(getString().length());
        boolean previous_is_cased = false;
        for (Iterator<Integer> iter = newSubsequenceIterator(); iter.hasNext();) {
            int codePoint = iter.next();
            if (previous_is_cased) {
                buffer.appendCodePoint(Character.toLowerCase(codePoint));
            } else {
                buffer.appendCodePoint(Character.toTitleCase(codePoint));
            }

            if (Character.isLowerCase(codePoint) || Character.isUpperCase(codePoint)
                    || Character.isTitleCase(codePoint)) {
                previous_is_cased = true;
            } else {
                previous_is_cased = false;
            }
        }
        return new PyUnicode(buffer);
    }

    /*
    @ExposedMethod(doc = BuiltinDocs.unicode_swapcase_doc)
    */
    final PyObject unicode_swapcase() {
        StringBuilder buffer = new StringBuilder(getString().length());
        for (Iterator<Integer> iter = newSubsequenceIterator(); iter.hasNext();) {
            int codePoint = iter.next();
            if (Character.isUpperCase(codePoint)) {
                buffer.appendCodePoint(Character.toLowerCase(codePoint));
            } else if (Character.isLowerCase(codePoint)) {
                buffer.appendCodePoint(Character.toUpperCase(codePoint));
            } else {
                buffer.appendCodePoint(codePoint);
            }
        }
        return new PyUnicode(buffer);
    }

    /** Define what characters are to be treated as a space according to Python 2. */
    private static boolean isPythonSpace(int ch) {
        // Use the Java built-in methods as far as possible
        return Character.isWhitespace(ch)    // catches the ASCII spaces and some others
                || Character.isSpaceChar(ch) // catches remaining Unicode spaces
                || ch == 0x0085  // NEXT LINE (not a space in Java)
                || ch == 0x180e; // MONGOLIAN VOWEL SEPARATOR (not a space in Java 9+ or Python 3)
    }

    private static class StripIterator implements Iterator<Integer> {

        private final Iterator<Integer> iter;
        private int lookahead = -1;

        public StripIterator(PyUnicode sep, Iterator<Integer> iter) {
            this.iter = iter;
            if (sep != null) {
                Set<Integer> sepSet = new HashSet<>();
                for (Iterator<Integer> sepIter = sep.newSubsequenceIterator(); sepIter.hasNext();) {
                    sepSet.add(sepIter.next());
                }
                while (iter.hasNext()) {
                    int codePoint = iter.next();
                    if (!sepSet.contains(codePoint)) {
                        lookahead = codePoint;
                        return;
                    }
                }
            } else {
                while (iter.hasNext()) {
                    int codePoint = iter.next();
                    if (!isPythonSpace(codePoint)) {
                        lookahead = codePoint;
                        return;
                    }
                }
            }
        }

        @Override
        public boolean hasNext() {
            return lookahead != -1;
        }

        @Override
        public Integer next() {
            int old = lookahead;
            if (iter.hasNext()) {
                lookahead = iter.next();
            } else {
                lookahead = -1;
            }
            return old;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    // Compliance requires a bit of inconsistency with other coercions used.
    /**
     * Helper used in <code>.strip()</code> to "coerce" a method argument into a
     * <code>PyUnicode</code> (which it may already be). A <code>null</code> argument or a
     * <code>PyNone</code> causes <code>null</code> to be returned. A buffer type is not acceptable
     * to (Unicode) <code>.strip()</code>. This is the difference from
     * {@link #coerceToUnicode(PyObject, boolean)}.
     *
     * @param o the object to coerce
     * @param name of method
     * @return an equivalent <code>PyUnicode</code> (or o itself, or <code>null</code>)
     */
    private static PyUnicode coerceStripSepToUnicode(PyObject o, String name) {
        if (o == null) {
            return null;
        } else if (o instanceof PyUnicode) {
            return (PyUnicode) o;
        } else if (o instanceof PyString) {
            PyObject u = ((PyString) o).decode();
            return (u instanceof PyUnicode) ? (PyUnicode) u : new PyUnicode(u.toString());
        } else if (o == Py.None) {
            return null;
        } else {
            throw new TypeError(name + " arg must be None, unicode or str");
        }
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
    /*
    @ExposedMethod(defaults = "null", doc = BuiltinDocs.unicode_strip_doc)
    */
    final PyObject unicode_strip(PyObject sepObj) {

        PyUnicode sep = coerceStripSepToUnicode(sepObj, "strip");

        if (isBasicPlane()) {
            // this contains only basic plane characters
            if (sep == null) {
                // And we're stripping whitespace, so use the PyString implementation
                return new PyUnicode(_strip());
            } else if (sep.isBasicPlane()) {
                // And the strip characters are basic plane too, so use the PyString implementation
                return new PyUnicode(_strip(sep.getString()));
            }
        }

        // Not basic plane: have to do real Unicode
        return new PyUnicode(new ReversedIterator<Integer>(new StripIterator(sep,
                new ReversedIterator<>(new StripIterator(sep, newSubsequenceIterator())))));
    }

    // Copied from PyString
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

    // Copied from PyString
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

    // Copied from PyString
    /**
     * Helper for <code>strip</code>, <code>lstrip</code> implementation, when stripping whitespace.
     *
     * @param right rightmost extent of string search
     * @return index of leftmost non-whitespace character or <code>right</code> if they all are.
     */
    protected int _findLeft_PyString(int right) {
        String s = getString();
        for (int left = 0; left < right; left++) {
            if (!BaseBytes.isspace((byte) s.charAt(left))) {
                return left;
            }
        }
        return right;
    }

    // Copied from PyString
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

    // Copied from PyString
    /**
     * Helper for <code>strip</code>, <code>rstrip</code> implementation, when stripping whitespace.
     *
     * @return index of rightmost non-whitespace character or -1 if they all are.
     */
    protected int _findRight_PyString() {
        String s = getString();
        for (int right = s.length(); --right >= 0;) {
            if (!BaseBytes.isspace((byte) s.charAt(right))) {
                return right;
            }
        }
        return -1;
    }

    // Copied from PyString
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

    /*
    @ExposedMethod(defaults = "null", doc = BuiltinDocs.unicode_lstrip_doc)
    */
    final PyObject unicode_lstrip(PyObject sepObj) {

        PyUnicode sep = coerceStripSepToUnicode(sepObj, "lstrip");

        if (isBasicPlane()) {
            // this contains only basic plane characters
            if (sep == null) {
                // And we're stripping whitespace, so use the PyString implementation
                return new PyUnicode(_lstrip());
            } else if (sep.isBasicPlane()) {
                // And the strip characters are basic plane too, so use the PyString implementation
                return new PyUnicode(_lstrip(sep.getString()));
            }
        }

        // Not basic plane: have to do real Unicode
        return new PyUnicode(new StripIterator(sep, newSubsequenceIterator()));
    }

    // Copied from PyString
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

    // Copied from PyString
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

    /*
    @ExposedMethod(defaults = "null", doc = BuiltinDocs.unicode_rstrip_doc)
    */
    final PyObject unicode_rstrip(PyObject sepObj) {

        PyUnicode sep = coerceStripSepToUnicode(sepObj, "rstrip");

        if (isBasicPlane()) {
            // this contains only basic plane characters
            if (sep == null) {
                // And we're stripping whitespace, so use the PyString implementation
                return new PyUnicode(_rstrip());
            } else if (sep.isBasicPlane()) {
                // And the strip characters are basic plane too, so use the PyString implementation
                return new PyUnicode(_rstrip(sep.getString()));
            }
        }

        // Not basic plane: have to do real Unicode
        return new PyUnicode(new ReversedIterator<Integer>(
                new StripIterator(sep, new ReversedIterator<>(newSubsequenceIterator()))));
    }

    // Copied from PyString
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

    // Copied from PyString
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

    // Docs copied from PyString
    /**
     * Helper for <code>strip</code>, <code>lstrip</code> implementation, when stripping whitespace.
     *
     * @param right rightmost extent of string search
     * @return index of leftmost non-whitespace character or <code>right</code> if they all are.
     */
    protected int _findLeft(int right) {
        String s = getString();
        for (int left = 0; left < right; left++) {
            if (!isPythonSpace(s.charAt(left))) {
                return left;
            }
        }
        return right;
    }

    // Docs copied from PyString
    /**
     * Helper for <code>strip</code>, <code>rstrip</code> implementation, when stripping whitespace.
     *
     * @return index of rightmost non-whitespace character or -1 if they all are.
     */
    protected int _findRight() {
        String s = getString();
        for (int right = s.length(); --right >= 0;) {
            if (!isPythonSpace(s.charAt(right))) {
                return right;
            }
        }
        return -1;
    }

    public PyTuple partition(PyObject sep) {
        return unicode_partition(sep);
    }

    // Copied from PyString
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

    // Copied from PyString
    /*
    @ExposedMethod(doc = BuiltinDocs.str_partition_doc)
    */
    final PyTuple str_partition(PyObject sepObj) {

        if (sepObj instanceof PyUnicode) {
            // Deal with Unicode separately
            return unicodePartition(sepObj);

        } else {
            // It ought to be some kind of bytes with the buffer API.
            String sep = asU16BytesOrError(sepObj);

            if (sep.length() == 0) {
                throw new ValueError("empty separator");
            }

            int index = getString().indexOf(sep);
            if (index != -1) {
                return new PyTuple(fromSubstring(0, index), sepObj,
                        fromSubstring(index + sep.length(), getString().length()));
            } else {
                return new PyTuple(this, new PyString(""), new PyString(""));
            }
        }
    }

    // Copied from PyString
    final PyTuple unicodePartition(PyObject sepObj) {
        PyUnicode strObj = __unicode__();
        String str = strObj.getString();

        // Will throw a TypeError if not a basestring
        String sep = sepObj.asString();
        sepObj = sepObj.__unicode__();

        if (sep.length() == 0) {
            throw new ValueError("empty separator");
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

    /*
    @ExposedMethod(doc = BuiltinDocs.unicode_partition_doc)
    */
    final PyTuple unicode_partition(PyObject sep) {
        return unicodePartition(coerceToUnicode(sep));
    }

    private abstract class SplitIterator implements Iterator<PyUnicode> {

        protected final int maxsplit;
        protected final Iterator<Integer> iter = newSubsequenceIterator();
        protected final LinkedList<Integer> lookahead = new LinkedList<Integer>();
        protected int numSplits = 0;
        protected boolean completeSeparator = false;

        SplitIterator(int maxsplit) {
            this.maxsplit = maxsplit;
        }

        @Override
        public boolean hasNext() {
            return lookahead.peek() != null
                    || (iter.hasNext() && (maxsplit == -1 || numSplits <= maxsplit));
        }

        protected void addLookahead(StringBuilder buffer) {
            for (int codepoint : lookahead) {
                buffer.appendCodePoint(codepoint);
            }
            lookahead.clear();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        public boolean getEndsWithSeparator() {
            return completeSeparator && !hasNext();
        }
    }

    private class WhitespaceSplitIterator extends SplitIterator {

        WhitespaceSplitIterator(int maxsplit) {
            super(maxsplit);
        }

        @Override
        public PyUnicode next() {
            StringBuilder buffer = new StringBuilder();

            addLookahead(buffer);
            if (numSplits == maxsplit) {
                while (iter.hasNext()) {
                    buffer.appendCodePoint(iter.next());
                }
                return new PyUnicode(buffer);
            }

            boolean inSeparator = false;
            boolean atBeginning = numSplits == 0;

            while (iter.hasNext()) {
                int codepoint = iter.next();
                if (isPythonSpace(codepoint)) {
                    completeSeparator = true;
                    if (!atBeginning) {
                        inSeparator = true;
                    }
                } else if (!inSeparator) {
                    completeSeparator = false;
                    buffer.appendCodePoint(codepoint);
                } else {
                    completeSeparator = false;
                    lookahead.add(codepoint);
                    break;
                }
                atBeginning = false;
            }
            numSplits++;
            return new PyUnicode(buffer);
        }
    }

    private static class PeekIterator<T> implements Iterator<T> {

        private T lookahead = null;
        private final Iterator<T> iter;

        public PeekIterator(Iterator<T> iter) {
            this.iter = iter;
            next();
        }

        public T peek() {
            return lookahead;
        }

        @Override
        public boolean hasNext() {
            return lookahead != null;
        }

        @Override
        public T next() {
            T peeked = lookahead;
            lookahead = iter.hasNext() ? iter.next() : null;
            return peeked;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    private static class ReversedIterator<T> implements Iterator<T> {

        private final List<T> reversed = new LinkedList<>();
        private final Iterator<T> iter;

        ReversedIterator(Iterator<T> iter) {
            while (iter.hasNext()) {
                reversed.add(iter.next());
            }
            Collections.reverse(reversed);
            this.iter = reversed.iterator();
        }

        @Override
        public boolean hasNext() {
            return iter.hasNext();
        }

        @Override
        public T next() {
            return iter.next();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    private class LineSplitIterator implements Iterator<PyObject> {

        private final PeekIterator<Integer> iter = new PeekIterator<>(newSubsequenceIterator());
        private final boolean keepends;

        LineSplitIterator(boolean keepends) {
            this.keepends = keepends;
        }

        @Override
        public boolean hasNext() {
            return iter.hasNext();
        }

        @Override
        public PyObject next() {
            StringBuilder buffer = new StringBuilder();
            while (iter.hasNext()) {
                int codepoint = iter.next();
                if (codepoint == '\r' && iter.peek() != null && iter.peek() == '\n') {
                    if (keepends) {
                        buffer.appendCodePoint(codepoint);
                        buffer.appendCodePoint(iter.next());
                    } else {
                        iter.next();
                    }
                    break;
                } else if (codepoint == '\n' || codepoint == '\r'
                        || Character.getType(codepoint) == Character.LINE_SEPARATOR) {
                    if (keepends) {
                        buffer.appendCodePoint(codepoint);
                    }
                    break;
                } else {
                    buffer.appendCodePoint(codepoint);
                }
            }
            return new PyUnicode(buffer);
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    private class SepSplitIterator extends SplitIterator {

        private final PyUnicode sep;

        SepSplitIterator(PyUnicode sep, int maxsplit) {
            super(maxsplit);
            this.sep = sep;
        }

        @Override
        public PyUnicode next() {
            StringBuilder buffer = new StringBuilder();

            addLookahead(buffer);
            if (numSplits == maxsplit) {
                while (iter.hasNext()) {
                    buffer.appendCodePoint(iter.next());
                }
                return new PyUnicode(buffer);
            }

            boolean inSeparator = true;
            while (iter.hasNext()) {
                // TODO: should cache the first codepoint
                inSeparator = true;
                for (Iterator<Integer> sepIter = sep.newSubsequenceIterator(); sepIter.hasNext();) {
                    int codepoint = iter.next();
                    if (codepoint != sepIter.next()) {
                        addLookahead(buffer);
                        buffer.appendCodePoint(codepoint);
                        inSeparator = false;
                        break;
                    } else {
                        lookahead.add(codepoint);
                    }
                }

                if (inSeparator) {
                    lookahead.clear();
                    break;
                }
            }

            numSplits++;
            completeSeparator = inSeparator;
            return new PyUnicode(buffer);
        }
    }

    private SplitIterator newSplitIterator(PyUnicode sep, int maxsplit) {
        if (sep == null) {
            return new WhitespaceSplitIterator(maxsplit);
        } else if (sep.getCodePointCount() == 0) {
            throw new ValueError("empty separator");
        } else {
            return new SepSplitIterator(sep, maxsplit);
        }
    }

    /*
    @ExposedMethod(doc = BuiltinDocs.unicode_rpartition_doc)
    */
    final PyTuple unicode_rpartition(PyObject sep) {
        return unicodeRpartition(coerceToUnicode(sep));
    }

    // Copied from PyString
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

    // Copied from PyString
    /*
    @ExposedMethod(doc = BuiltinDocs.str_rpartition_doc)
    */
    final PyTuple str_rpartition(PyObject sepObj) {

        if (sepObj instanceof PyUnicode) {
            // Deal with Unicode separately
            return unicodeRpartition(sepObj);

        } else {
            // It ought to be some kind of bytes with the buffer API.
            String sep = asU16BytesOrError(sepObj);

            if (sep.length() == 0) {
                throw new ValueError("empty separator");
            }

            int index = getString().lastIndexOf(sep);
            if (index != -1) {
                return new PyTuple(fromSubstring(0, index), sepObj,
                        fromSubstring(index + sep.length(), getString().length()));
            } else {
                return new PyTuple(new PyString(""), new PyString(""), this);
            }
        }
    }

    // Copied from PyString
    /**
     * Helper to Python <code>str.rpartition()</code>, splits the <code>PyString</code> at the
     * last occurrence of <code>sepObj</code> returning a {@link PyTuple} containing the part before
     * the separator, the separator itself, and the part after the separator.
     *
     * @param sepObj str, unicode or object implementing {@link BufferProtocol}
     * @return tuple of parts
     */
    final PyTuple unicodeRpartition(PyObject sepObj) {
        PyUnicode strObj = __unicode__();
        String str = strObj.getString();

        // Will throw a TypeError if not a basestring
        String sep = sepObj.asString();
        sepObj = sepObj.__unicode__();

        if (sep.length() == 0) {
            throw new ValueError("empty separator");
        }

        int index = str.lastIndexOf(sep);
        if (index != -1) {
            return new PyTuple(strObj.fromSubstring(0, index), sepObj,
                    strObj.fromSubstring(index + sep.length(), str.length()));
        } else {
            PyUnicode emptyUnicode = new PyUnicode("");
            return new PyTuple(emptyUnicode, emptyUnicode, this);
        }
    }

    /*
    @ExposedMethod(defaults = {"null", "-1"}, doc = BuiltinDocs.unicode_split_doc)
    */
    final PyList unicode_split(PyObject sepObj, int maxsplit) {
        String sep = coerceToString(sepObj, true);
        if (sep != null) {
            return _split(sep, maxsplit);
        } else {
            return _split(null, maxsplit);
        }
    }

    // Copied from PyString
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
            throw new ValueError("empty separator");
        } else {
            // Split on specified (non-empty) string
            return splitfields(sep, maxsplit);
        }
    }

    // Docs copied from PyString
    /**
     * Helper function for <code>.split</code>, in <code>str</code> and (when overridden) in
     * <code>unicode</code>, splitting on white space and returning a list of the separated parts.
     * If there are more than <code>maxsplit</code> feasible splits the last element of the list is
     * the remainder of the original (this) string.
     * <p> The split sections will be {@link PyUnicode} and use the Python
     * <code>unicode</code> definition of "space".
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
                if (!isPythonSpace(s.charAt(start))) {
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
                    if (isPythonSpace(s.charAt(index))) {
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

    // Copied from PyString
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

    /*
    @ExposedMethod(defaults = {"null", "-1"}, doc = BuiltinDocs.unicode_rsplit_doc)
    */
    final PyList unicode_rsplit(PyObject sepObj, int maxsplit) {
        String sep = coerceToString(sepObj, true);
        if (sep != null) {
            return _rsplit(sep, maxsplit);
        } else {
            return _rsplit(null, maxsplit);
        }
    }

    // Docs copied from PyString
    /**
     * Helper function for <code>.rsplit</code>, in <code>str</code> and (when overridden) in
     * <code>unicode</code>, splitting on white space and returning a list of the separated parts.
     * If there are more than <code>maxsplit</code> feasible splits the first element of the list is
     * the remainder of the original (this) string.
     * The split sections will be {@link PyUnicode} and use the Python
     * <code>unicode</code> definition of "space".
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
                if (!isPythonSpace(s.charAt(end))) {
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
                    if (isPythonSpace(s.charAt(index))) {
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

    // Copied from PyString
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
            throw new ValueError("empty separator");
        } else {
            // Split on specified (non-empty) string
            return rsplitfields(sep, maxsplit);
        }
    }

    // Copied from PyString
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
            throw new ValueError("empty separator");

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

    /*
    @ExposedMethod(defaults = "false", doc = BuiltinDocs.unicode_splitlines_doc)
    */
    final PyList unicode_splitlines(boolean keepends) {
        return new PyList(new LineSplitIterator(keepends));
    }

    // Doc copied from PyString
    /**
     * Return a new object <em>of the same type as this one</em> equal to the slice
     * <code>[begin:end]</code>. (Python end-relative indexes etc. are not supported.) Subclasses (
     * {@link PyUnicode#fromSubstring(int, int)}) override this to return their own type.)
     *
     * @param begin first included character.
     * @param end first excluded character.
     * @return new object.
     */
    @Override
    protected PyString fromSubstring(int begin, int end) {
        assert (isBasicPlane()); // can only be used on a codepath from str_ equivalents
        return new PyUnicode(getString().substring(begin, end), true);
    }

    // Doc copied from PyString
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
    /*
    @ExposedMethod(defaults = {"null", "null"}, doc = BuiltinDocs.unicode_index_doc)
    */
    final int unicode_index(PyObject subObj, PyObject start, PyObject end) {
        final String sub = coerceToString(subObj);
        // Now use the mechanics of the PyString on the UTF-16.
        return checkIndex(_find(sub, start, end));
    }

    // Doc copied from PyString
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
    /*
    @ExposedMethod(defaults = {"null", "null"}, doc = BuiltinDocs.unicode_index_doc)
    */
    final int unicode_rindex(PyObject subObj, PyObject start, PyObject end) {
        final String sub = coerceToString(subObj);
        // Now use the mechanics of the PyString on the UTF-16.
        return checkIndex(_rfind(sub, start, end));
    }

    // Doc copied from PyString
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
    /*
    @ExposedMethod(defaults = {"null", "null"}, doc = BuiltinDocs.unicode_count_doc)
    */
    final int unicode_count(PyObject subObj, PyObject start, PyObject end) {
        final PyUnicode sub = coerceToUnicode(subObj);
        if (isBasicPlane()) {
            return _count(sub.getString(), start, end);
        }
        int[] indices = translateIndices_PyString(start, end); // do not convert to utf-16 indices.
        int count = 0;
        for (Iterator<Integer> mainIter =
                newSubsequenceIterator(indices[0], indices[1], 1); mainIter.hasNext();) {
            int matched = sub.getCodePointCount();
            for (Iterator<Integer> subIter = sub.newSubsequenceIterator(); mainIter.hasNext()
                    && subIter.hasNext();) {
                if (mainIter.next() != subIter.next()) {
                    break;
                }
                matched--;
            }
            if (matched == 0) {
                count++;
            }
        }
        return count;
    }

    // Copied from PyString
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

    // Doc copied from PyString
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
    /*
    @ExposedMethod(defaults = {"null", "null"}, doc = BuiltinDocs.unicode_find_doc)
    */
    final int unicode_find(PyObject subObj, PyObject start, PyObject end) {
        int found = _find(coerceToString(subObj), start, end);
        return found < 0 ? -1 : translator.codePointIndex(found);
    }

    // Copied from PyString
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

    // Doc copied from PyString
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
    /*
    @ExposedMethod(defaults = {"null", "null"}, doc = BuiltinDocs.unicode_rfind_doc)
    */
    final int unicode_rfind(PyObject subObj, PyObject start, PyObject end) {
        int found = _rfind(coerceToString(subObj), start, end);
        return found < 0 ? -1 : translator.codePointIndex(found);
    }

    // Copied from PyString
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

    private static String padding(int n, int pad) {
        StringBuilder buffer = new StringBuilder(n);
        for (int i = 0; i < n; i++) {
            buffer.appendCodePoint(pad);
        }
        return buffer.toString();
    }

    private static int parse_fillchar(String function, String fillchar) {
        if (fillchar == null) {
            return ' ';
        }
        if (fillchar.codePointCount(0, fillchar.length()) != 1) {
            throw new TypeError(function + "() argument 2 must be char, not str");
        }
        return fillchar.codePointAt(0);
    }

    /*
    @ExposedMethod(defaults = "null", doc = BuiltinDocs.unicode_ljust_doc)
    */
    final PyObject unicode_ljust(int width, String padding) {
        int n = width - getCodePointCount();
        if (n <= 0) {
            return new PyUnicode(getString());
        } else {
            return new PyUnicode(getString() + padding(n, parse_fillchar("ljust", padding)));
        }
    }

    /*
    @ExposedMethod(defaults = "null", doc = BuiltinDocs.unicode__doc)
    */
    final PyObject unicode_rjust(int width, String padding) {
        int n = width - getCodePointCount();
        if (n <= 0) {
            return new PyUnicode(getString());
        } else {
            return new PyUnicode(padding(n, parse_fillchar("ljust", padding)) + getString());
        }
    }

    /*
    @ExposedMethod(defaults = "null", doc = BuiltinDocs.unicode_rjust_doc)
    */
    final PyObject unicode_center(int width, String padding) {
        int n = width - getCodePointCount();
        if (n <= 0) {
            return new PyUnicode(getString());
        }
        int half = n / 2;
        if (n % 2 > 0 && width % 2 > 0) {
            half += 1;
        }
        int pad = parse_fillchar("center", padding);
        return new PyUnicode(padding(half, pad) + getString() + padding(n - half, pad));
    }

    /*
    @ExposedMethod(doc = BuiltinDocs.unicode_zfill_doc)
    */
    final PyObject unicode_zfill(int width) {
        int n = getCodePointCount();
        if (n >= width) {
            return new PyUnicode(getString());
        }
        if (isBasicPlane()) {
            return new PyUnicode(str_zfill(width));
        }
        StringBuilder buffer = new StringBuilder(width);
        int nzeros = width - n;
        boolean first = true;
        boolean leadingSign = false;
        for (Iterator<Integer> iter = newSubsequenceIterator(); iter.hasNext();) {
            int codePoint = iter.next();
            if (first) {
                first = false;
                if (codePoint == '+' || codePoint == '-') {
                    buffer.appendCodePoint(codePoint);
                    leadingSign = true;
                }
                for (int i = 0; i < nzeros; i++) {
                    buffer.appendCodePoint('0');
                }
                if (!leadingSign) {
                    buffer.appendCodePoint(codePoint);
                }
            } else {
                buffer.appendCodePoint(codePoint);
            }
        }
        if (first) {
            for (int i = 0; i < nzeros; i++) {
                buffer.appendCodePoint('0');
            }
        }
        return new PyUnicode(buffer);
    }

    // Copied from PyString
    /*
    @ExposedMethod(doc = BuiltinDocs.str_zfill_doc)
    */
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

    // Copied from PyString
    /*
    @ExposedMethod(defaults = "8", doc = BuiltinDocs.str_expandtabs_doc)
    */
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

    /*
    @ExposedMethod(defaults = "8", doc = BuiltinDocs.unicode_expandtabs_doc)
    */
    final PyObject unicode_expandtabs(int tabsize) {
        return new PyUnicode(str_expandtabs(tabsize));
    }

    /*
    @ExposedMethod(doc = BuiltinDocs.unicode_capitalize_doc)
    */
    final PyObject unicode_capitalize() {
        if (getString().length() == 0) {
            return this;
        }
        StringBuilder buffer = new StringBuilder(getString().length());
        boolean first = true;
        for (Iterator<Integer> iter = newSubsequenceIterator(); iter.hasNext();) {
            if (first) {
                buffer.appendCodePoint(Character.toUpperCase(iter.next()));
                first = false;
            } else {
                buffer.appendCodePoint(Character.toLowerCase(iter.next()));
            }
        }
        return new PyUnicode(buffer);
    }

    // Doc copied from PyString
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
    /*
    @ExposedMethod(defaults = "-1", doc = BuiltinDocs.unicode_replace_doc)
    */
    final PyString unicode_replace(PyObject oldPieceObj, PyObject newPieceObj, int count) {

        // Convert other argument types to PyUnicode (or error)
        PyUnicode newPiece = coerceToUnicode(newPieceObj);
        PyUnicode oldPiece = coerceToUnicode(oldPieceObj);

        if (isBasicPlane() && newPiece.isBasicPlane() && oldPiece.isBasicPlane()) {
            // Use the mechanics of PyString, since all is basic plane
            return _replace(oldPiece.getString(), newPiece.getString(), count);

        } else {
            // A Unicode-specific implementation is needed working in code points
            StringBuilder buffer = new StringBuilder();

            if (oldPiece.getCodePointCount() == 0) {
                Iterator<Integer> iter = newSubsequenceIterator();
                for (int i = 1; (count == -1 || i < count) && iter.hasNext(); i++) {
                    if (i == 1) {
                        buffer.append(newPiece.getString());
                    }
                    buffer.appendCodePoint(iter.next());
                    buffer.append(newPiece.getString());
                }
                while (iter.hasNext()) {
                    buffer.appendCodePoint(iter.next());
                }
                return new PyUnicode(buffer);

            } else {
                SplitIterator iter = newSplitIterator(oldPiece, count);
                int numSplits = 0;
                while (iter.hasNext()) {
                    buffer.append(((PyUnicode) iter.next()).getString());
                    if (iter.hasNext()) {
                        buffer.append(newPiece.getString());
                    }
                    numSplits++;
                }
                if (iter.getEndsWithSeparator() && (count == -1 || numSplits <= count)) {
                    buffer.append(newPiece.getString());
                }
                return new PyUnicode(buffer);
            }
        }
    }

    // Copied from PyString
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
            return createInstance(newPiece).str_join(splitfields(oldPiece, count));
        }
    }

    // Copied from PyString
    /*
    @ExposedMethod(doc = BuiltinDocs.str_join_doc)
    */
    final PyString str_join(PyObject obj) {
        PySequence seq = fastSequence(obj, "");
        int seqLen = seq.__len__();
        if (seqLen == 0) {
            return new PyString("");
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
                throw new TypeError(String.format("sequence item %d: expected string, %.80s found",
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
                throw new OverflowError("join() result is too long for a Python string");
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

    // Copied from PyString
    PyUnicode unicodeJoin(PyObject obj) {
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
                throw new TypeError(String.format(
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
                throw new OverflowError("join() result is too long for a Python string");
            }
            buf.append(itemString);
        }
        return new PyUnicode(buf.toString());
    }

    @Override
    public PyString join(PyObject seq) {
        return unicode_join(seq);
    }

    /*
    @ExposedMethod(doc = BuiltinDocs.unicode_join_doc)
    */
    final PyUnicode unicode_join(PyObject seq) {
        return unicodeJoin(seq);
    }

    // Doc copied from PyString
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
    /*
    @ExposedMethod(defaults = {"null", "null"}, doc = BuiltinDocs.unicode_startswith_doc)
    */
    final boolean unicode_startswith(PyObject prefix, PyObject startObj, PyObject endObj) {
        int[] indices = translateIndices(startObj, endObj);
        int start = indices[0];
        int sliceLen = indices[1] - start;

        if (!(prefix instanceof PyTuple)) {
            // It ought to be PyUnicode or some kind of bytes with the buffer API to decode.
            String s = coerceToString(prefix);
            return sliceLen >= s.length() && getString().startsWith(s, start);

        } else {
            // Loop will return true if this slice starts with any prefix in the tuple
            for (PyObject prefixObj : ((PyTuple) prefix).getArray()) {
                // It ought to be PyUnicode or some kind of bytes with the buffer API.
                String s = coerceToString(prefixObj);
                if (sliceLen >= s.length() && getString().startsWith(s, start)) {
                    return true;
                }
            }
            // None matched
            return false;
        }
    }

    // Doc copied from PyString
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
    /*
    @ExposedMethod(defaults = {"null", "null"}, doc = BuiltinDocs.unicode_endswith_doc)
    */
    final boolean unicode_endswith(PyObject suffix, PyObject startObj, PyObject endObj) {
        int[] indices = translateIndices(startObj, endObj);
        String substr = getString().substring(indices[0], indices[1]);

        if (!(suffix instanceof PyTuple)) {
            // It ought to be PyUnicode or some kind of bytes with the buffer API.
            String s = coerceToString(suffix);
            return substr.endsWith(s);

        } else {
            // Loop will return true if this slice ends with any suffix in the tuple
            for (PyObject suffixObj : ((PyTuple) suffix).getArray()) {
                // It ought to be PyUnicode or some kind of bytes with the buffer API.
                String s = coerceToString(suffixObj);
                if (substr.endsWith(s)) {
                    return true;
                }
            }
            // None matched
            return false;
        }
    }

    // Doc copied from PyString
    /*
    @ExposedMethod(doc = BuiltinDocs.unicode_translate_doc)
    */
    final PyObject unicode_translate(PyObject table) {
        return translateCharmap(this, "ignore", table);
    }

    // Copied from PyString
    // Note that unicode.translate, therefore Python 3 str.translate, differs a lot
    /*
    @ExposedMethod(defaults = {"null", "null"}, doc = BuiltinDocs.str_translate_doc)
    */
    final String str_translate(PyObject tableObj, PyObject deletecharsObj) {
        // Accept anything with the buffer API or null
        String table = asU16BytesNullOrError(tableObj, null);
        String deletechars = asU16BytesNullOrError(deletecharsObj, null);
        return _translate(table, deletechars);
    }

    // Copied from PyString
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
            throw new ValueError("translation table must be 256 characters long");
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
                    throw new TypeError("translate() only works for 8-bit character strings");
                }
            }
        }
        return buf.toString();
    }

    /*
    @ExposedMethod(doc = BuiltinDocs.unicode_islower_doc)
    */
    final boolean unicode_islower() {
        boolean cased = false;
        for (Iterator<Integer> iter = newSubsequenceIterator(); iter.hasNext();) {
            int codepoint = iter.next();
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
    final boolean unicode_isupper() {
        boolean cased = false;
        for (Iterator<Integer> iter = newSubsequenceIterator(); iter.hasNext();) {
            int codepoint = iter.next();
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
    final boolean unicode_isalpha() {
        if (getCodePointCount() == 0) {
            return false;
        }
        for (Iterator<Integer> iter = newSubsequenceIterator(); iter.hasNext();) {
            if (!Character.isLetter(iter.next())) {
                return false;
            }
        }
        return true;
    }

    /*
    @ExposedMethod(doc = BuiltinDocs.unicode_isalnum_doc)
    */
    final boolean unicode_isalnum() {
        if (getCodePointCount() == 0) {
            return false;
        }
        for (Iterator<Integer> iter = newSubsequenceIterator(); iter.hasNext();) {
            int codePoint = iter.next();
            if (!(Character.isLetterOrDigit(codePoint) || //
                    Character.getType(codePoint) == Character.LETTER_NUMBER)) {
                return false;
            }
        }
        return true;
    }

    /*
    @ExposedMethod(doc = BuiltinDocs.unicode_isdecimal_doc)
    */
    final boolean unicode_isdecimal() {
        if (getCodePointCount() == 0) {
            return false;
        }
        for (Iterator<Integer> iter = newSubsequenceIterator(); iter.hasNext();) {
            if (Character.getType(iter.next()) != Character.DECIMAL_DIGIT_NUMBER) {
                return false;
            }
        }
        return true;
    }

    /*
    @ExposedMethod(doc = BuiltinDocs.unicode_isdigit_doc)
    */
    final boolean unicode_isdigit() {
        if (getCodePointCount() == 0) {
            return false;
        }
        for (Iterator<Integer> iter = newSubsequenceIterator(); iter.hasNext();) {
            if (!Character.isDigit(iter.next())) {
                return false;
            }
        }
        return true;
    }

    /*
    @ExposedMethod(doc = BuiltinDocs.unicode_isnumeric_doc)
    */
    final boolean unicode_isnumeric() {
        if (getCodePointCount() == 0) {
            return false;
        }
        for (Iterator<Integer> iter = newSubsequenceIterator(); iter.hasNext();) {
            int type = Character.getType(iter.next());
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
    final boolean unicode_istitle() {
        if (getCodePointCount() == 0) {
            return false;
        }
        boolean cased = false;
        boolean previous_is_cased = false;
        for (Iterator<Integer> iter = newSubsequenceIterator(); iter.hasNext();) {
            int codePoint = iter.next();
            if (Character.isUpperCase(codePoint) || Character.isTitleCase(codePoint)) {
                if (previous_is_cased) {
                    return false;
                }
                previous_is_cased = true;
                cased = true;
            } else if (Character.isLowerCase(codePoint)) {
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
    final boolean unicode_isspace() {
        if (getCodePointCount() == 0) {
            return false;
        }
        for (Iterator<Integer> iter = newSubsequenceIterator(); iter.hasNext();) {
            if (!isPythonSpace(iter.next())) {
                return false;
            }
        }
        return true;
    }

    /*
    @ExposedMethod(doc = BuiltinDocs.unicode_encode_doc)
    */
    final String unicode_encode(PyObject[] args, String[] keywords) {
        return str_encode(args, keywords);
    }

    // Copied from PyString
    public String encode() {
        return encode(null, null);
    }

    // Copied from PyString
    public String encode(String encoding, String errors) {
        return codecs.encode(this, encoding, errors);
    }

    // Copied from PyString
    /*
    @ExposedMethod(doc = BuiltinDocs.str_encode_doc)
    */
    final String str_encode(PyObject[] args, String[] keywords) {
        ArgParser ap = new ArgParser("encode", args, keywords, "encoding", "errors");
        String encoding = ap.getString(0, null);
        String errors = ap.getString(1, null);
        return encode(encoding, errors);
    }

    /*
    @ExposedMethod(doc = BuiltinDocs.unicode___getnewargs___doc)
    */
    final PyTuple unicode___getnewargs__() {
        return new PyTuple(new PyUnicode(this.getString()));
    }

    @Override
    public PyObject __format__(PyObject formatSpec) {
        return unicode___format__(formatSpec);
    }

    /*
    @ExposedMethod(doc = BuiltinDocs.unicode___format___doc)
    */
    final PyObject unicode___format__(PyObject formatSpec) {
        // Re-use the str implementation, which adapts itself to unicode.
        return str___format__(formatSpec);
    }

    /*
    @ExposedMethod(doc = BuiltinDocs.unicode__formatter_parser_doc)
    */
    final PyObject unicode__formatter_parser() {
        return new MarkupIterator(this);
    }

    /*
    @ExposedMethod(doc = BuiltinDocs.unicode__formatter_field_name_split_doc)
    */
    final PyObject unicode__formatter_field_name_split() {
        FieldNameIterator iterator = new FieldNameIterator(this);
        return new PyTuple(iterator.pyHead(), iterator);
    }

    /*
    @ExposedMethod(doc = BuiltinDocs.unicode_format_doc)
    */
    final PyObject unicode_format(PyObject[] args, String[] keywords) {
        try {
            return new PyUnicode(buildFormattedString(args, keywords, null, null));
        } catch (IllegalArgumentException e) {
            throw new ValueError(e.getMessage());
        }
    }

    // CharSequence interface ----------------------------------------

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
//                        PyObject mod = imp.importName("ucnhash", true);
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
        } else if (value <= PySystemState.maxunicode) {
            partialDecode.appendCodePoint(value);
            return true;
        }
        return false;
    }

    // Plumbing ------------------------------------------------------

    @Override
    public Iterator<Integer> iterator() {
        return newSubsequenceIterator();
    }

    public PyComplex __complex__() {
        return new PyString(encodeDecimal()).__complex__();
    }

    public int atoi(int base) {
        return atoi(new PyString(encodeDecimal()), base);
    }

    public PyLong atol(int base) {
        return atol(new PyString(encodeDecimal()), base);
    }

    public double atof() {
        return atof(new PyString(encodeDecimal()));
    }

    /**
     * Encode unicode into a valid decimal String. Throws a UnicodeEncodeError on invalid
     * characters.
     *
     * @return a valid decimal as an encoded String
     */
    private String encodeDecimal() {
        if (isBasicPlane()) {
            return encodeDecimalBasic();
        }

        int digit;
        StringBuilder sb = new StringBuilder();
        int i = 0;
        for (Iterator<Integer> iter = newSubsequenceIterator(); iter.hasNext(); i++) {
            int codePoint = iter.next();
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
            codecs.encoding_error("strict", "decimal", getString(), i, i + 1,
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
        for (int i = 0; i < getString().length(); i++) {
            char ch = getString().charAt(i);
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
            codecs.encoding_error("strict", "decimal", getString(), i, i + 1,
                    "invalid decimal Unicode string");
        }
        return sb.toString();
    }

    // Copied from PyString
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
            throw new ValueError("substring not found");
        }
    }

    // Copied from PyString with this -> self
    /**
     * Convert this PyString to a floating-point value according to Python rules.
     *
     * @return the value
     */
    public static double atof(PyString self) {
        double x = 0.0;
        Matcher m = getFloatPattern().matcher(self.getString());
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
            throw new ValueError(String.format(fmt, self.getString().trim()));
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
            throw new ValueError(String.format(fmt, getString().trim()));
        }

    }

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
    private static BigInteger asciiToBigInteger(PyString self, int base, boolean isLong) {
        String str = self.getString();

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

    // Copied from PyString
    public int atoi() {
        return atoi(10);
    }

    // Copied from PyString with this -> self
    public static int atoi(PyString self, int base) {
        if ((base != 0 && base < 2) || (base > 36)) {
            throw new ValueError("invalid base for atoi()");
        }

        try {
            BigInteger bi = asciiToBigInteger(self, base, false);
            if (bi.compareTo(PyInteger.MAX_INT) > 0 || bi.compareTo(PyInteger.MIN_INT) < 0) {
                throw new OverflowError("long int too large to convert to int");
            }
            return bi.intValue();
        } catch (NumberFormatException exc) {
            throw new ValueError(
                    "invalid literal for int() with base " + base + ": '" + self.getString() + "'");
        } catch (StringIndexOutOfBoundsException exc) {
            throw new ValueError(
                    "invalid literal for int() with base " + base + ": '" + self.getString() + "'");
        }
    }

    // Copied from PyString
    public PyLong atol() {
        return atol(10);
    }

    // Copied from PyString with this -> self
    public static PyLong atol(PyString self, int base) {
        if ((base != 0 && base < 2) || (base > 36)) {
            throw new ValueError("invalid base for long literal:" + base);
        }

        try {
            BigInteger bi = asciiToBigInteger(self, base, true);
            return new PyLong(bi);
        } catch (NumberFormatException exc) {
            if (self instanceof PyUnicode) {
                // TODO: here's a basic issue: do we use the BigInteger constructor
                // above, or add an equivalent to CPython's PyUnicode_EncodeDecimal;
                // we should note that the current error string does not quite match
                // CPython regardless of the codec, that's going to require some more work
                throw new UnicodeEncodeError("decimal", "codec can't encode character", 0, 0,
                        "invalid decimal Unicode string");
            } else {
                throw new ValueError(
                        "invalid literal for long() with base " + base + ": '" + self.getString() + "'");
            }
        } catch (StringIndexOutOfBoundsException exc) {
            throw new ValueError(
                    "invalid literal for long() with base " + base + ": '" + self.getString() + "'");
        }
    }

    // Copied from PyString
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
                    throw new ValueError("Unknown conversion specifier " + chunk.conversion);
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
                throw new IndexError("tuple index out of range");
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
                throw new KeyError(head);
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

    // Copied from PyString
    /**
     * Append to a formatting result, the presentation of one object, according to a given format
     * specification and the object's <code>__format__</code> method.
     *
     * @param fieldObj to format.
     * @param formatSpec specification to apply.
     * @param result to which the result will be appended.
     */
    private void renderField(PyObject fieldObj, String formatSpec, StringBuilder result) {
        PyString formatSpecStr = formatSpec == null ? new PyString("") : new PyString(formatSpec);
        //result.append(fieldObj.__format__(formatSpecStr).asString());
        throw new MissingFeature("String formatting");
    }

    // Copied from PyString
    /*
    @ExposedMethod(doc = BuiltinDocs.str___format___doc)
    */
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

    // Copied from PyString
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
                // XXX API of InternalFormat has changed from Jython 2
                // See org.python.core.PyFloat.formatDouble(double, Spec) for a clue.
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

    // Copied from _codecs
    // parallel to CPython's PyUnicode_TranslateCharmap
    static PyObject translateCharmap(PyUnicode str, String errors, PyObject mapping) {
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
                throw new TypeError("character mapping must return integer, None or unicode");
            }
        }
        return new PyUnicode(buf.toString());
    }

    // Copied from PyString
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
//        } else if (obj instanceof BufferProtocol) {
//            // Other object with buffer API: briefly access the buffer
//            try (PyBuffer buf = ((BufferProtocol) obj).getBuffer(PyBUF.FULL_RO)) {
//                return buf.toString();
//            }
        } else {
            return null;
        }
    }

    // Copied from PyString
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
            throw new TypeError("expected str, bytearray or other buffer compatible object");
        }
    }

    // Copied from PyString
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
                throw new TypeError("expected None, str or buffer compatible object");
            } else {
                // Tuned for .strip and its relations, which supply their name
                throw new TypeError(name + " arg must be None, str or buffer compatible object");
            }
        }
    }

    // Copied from PyString
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
    // XXX Probably superseded by the capabilities of PySequence.Delegate
    protected int[] translateIndices_PyString(PyObject startObj, PyObject endObj) {
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

}
