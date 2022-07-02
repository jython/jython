package org.python.core;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.python.core.stringlib.FieldNameIterator;
import org.python.core.stringlib.MarkupIterator;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedType;
import org.python.expose.MethodType;
import org.python.modules._codecs;
import org.python.util.Generic;

import com.google.common.base.CharMatcher;

/**
 * a builtin python unicode string.
 */
@Untraversable
@ExposedType(name = "unicode", base = PyBaseString.class, doc = BuiltinDocs.unicode_doc)
public class PyUnicode extends PyString implements Iterable<Integer> {

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
        this(TYPE, checkedCPString(codepoint));
    }

    public PyUnicode(int[] codepoints) {
        this(TYPE, checkedCPString(codepoints));
    }

    PyUnicode(StringBuilder buffer) {
        this(TYPE, buffer.toString());
    }

    /**
     * Translate a code point to a Java String, guaranteeing validity. (This avoids a Java stack
     * dump.)
     *
     * @param codePoint to translate
     * @return String from codepoint
     * @throws PyException(ValueError) if not a valid Unicode codepoint.
     */
    private static String checkedCPString(int codePoint) throws PyException {
        if (Character.isValidCodePoint(codePoint)) {
            return new String(Character.toChars(codePoint));
        } else {
            throw Py.ValueError(
                    String.format("character U+%08x is not in Unicode range", codePoint));
        }
    }

    /**
     * Translate a code point to a Java String, guaranteeing validity. (This avoids a Java stack
     * dump.)
     *
     * @param codePoints to translate
     * @return String from codepoint
     * @throws PyException(ValueError) if any element is not a valid Unicode codepoint.
     */
    private static String checkedCPString(int[] codePoints) throws PyException {
        try {
            return new String(codePoints, 0, codePoints.length);
        } catch (IllegalArgumentException e) {
            // Scan it again because the other call produces a better error message
            for (int c : codePoints) {
                checkedCPString(c);
            }
            return ""; // never reached in practice
        }
    }

    private static StringBuilder fromCodePoints(Iterator<Integer> iter) {
        StringBuilder buffer = new StringBuilder();
        while (iter.hasNext()) {
            buffer.append(checkedCPString(iter.next()));
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
        super(subtype, "", true);
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

    /**
     * {@code PyUnicode} implements the interface {@link BufferProtocol} technically by inheritance from {@link PyString},
     * but does not provide a buffer (in CPython). We therefore arrange that all calls to {@code getBuffer}
     * raise an error.
     *
     * @return always throws a {@code ClassCastException}
     */
    @Override
    public synchronized PyBuffer getBuffer(int flags) throws ClassCastException {
        throw new ClassCastException("'unicode' does not support the buffer protocol");
    }

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
        return Py.ValueError(msg);
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
    @Override
    protected int[] translateIndices(PyObject start, PyObject end) {
        int[] indices = super.translateIndices(start, end);
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
        if (s == null || CharMatcher.ascii().matchesAllOf(s)) {
            return s;
        }
        return codecs.PyUnicode_EncodeASCII(s, s.length(), null);
    }

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
                    throw Py.TypeError("decoder did not return an unicode object (type="
                            + decoded.getType().fastGetName() + ")");
                }
            }
            return S.__unicode__();
        } else {
            if (S == null) {
                return new PyUnicodeDerived(subtype, Py.EmptyString);
            }
            if (S instanceof PyUnicode) {
                return new PyUnicodeDerived(subtype, (PyUnicode) S);
            } else {
                return new PyUnicodeDerived(subtype, S.__str__());
            }
        }
    }

    @Override
    public PyString createInstance(String str) {
        return new PyUnicode(str);
    }

    /**
     * @param string UTF-16 string encoding the characters (as Java).
     * @param isBasic true if it is known that only BMP characters are present.
     */
    @Override
    protected PyString createInstance(String string, boolean isBasic) {
        return new PyUnicode(string, isBasic);
    }

    @Override
    public PyObject __mod__(PyObject other) {
        return unicode___mod__(other);
    }

    @ExposedMethod(doc = BuiltinDocs.unicode___mod___doc)
    final PyObject unicode___mod__(PyObject other) {
        StringFormatter fmt = new StringFormatter(getString(), true);
        return fmt.format(other);
    }

    @Override
    public PyUnicode __unicode__() {
        return this;
    }

    @Override
    public PyString __str__() {
        return unicode___str__();
    }

    @ExposedMethod(doc = BuiltinDocs.unicode___str___doc)
    final PyString unicode___str__() {
        return new PyString(encode());
    }

    @Override
    public int __len__() {
        return unicode___len__();
    }

    @ExposedMethod(doc = BuiltinDocs.unicode___len___doc)
    final int unicode___len__() {
        return getCodePointCount();
    }

    @Override
    public PyString __repr__() {
        return unicode___repr__();
    }

    @ExposedMethod(doc = BuiltinDocs.unicode___repr___doc)
    final PyString unicode___repr__() {
        return new PyString("u" + encode_UnicodeEscape(getString(), true));
    }

    @ExposedMethod(doc = BuiltinDocs.unicode___getitem___doc)
    final PyObject unicode___getitem__(PyObject index) {
        return str___getitem__(index);
    }

    @ExposedMethod(defaults = "null", doc = BuiltinDocs.unicode___getslice___doc)
    final PyObject unicode___getslice__(PyObject start, PyObject stop, PyObject step) {
        return seq___getslice__(start, stop, step);
    }

    @Override
    protected PyObject getslice(int start, int stop, int step) {
        if (isBasicPlane()) {
            return super.getslice(start, stop, step);
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

    @ExposedMethod(type = MethodType.CMP)
    final int unicode___cmp__(PyObject other) {
        // XXX needs proper coercion like __eq__, then UCS-32 code point order :(
        return str___cmp__(other);
    }

    @Override
    public PyObject __eq__(PyObject other) {
        return unicode___eq__(other);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.unicode___eq___doc)
    final PyObject unicode___eq__(PyObject other) {
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

    @Override
    public PyObject __ne__(PyObject other) {
        return unicode___ne__(other);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.unicode___ne___doc)
    final PyObject unicode___ne__(PyObject other) {
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

    @Override
    public PyObject __lt__(PyObject other) {
        return unicode___lt__(other);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.unicode___lt___doc)
    final PyObject unicode___lt__(PyObject other) {
        String s = coerceForComparison(other);
        if (s == null) {
            return null;
        }
        return getString().compareTo(s) < 0 ? Py.True : Py.False;
    }

    @Override
    public PyObject __le__(PyObject other) {
        return unicode___le__(other);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.unicode___le___doc)
    final PyObject unicode___le__(PyObject other) {
        String s = coerceForComparison(other);
        if (s == null) {
            return null;
        }
        return getString().compareTo(s) <= 0 ? Py.True : Py.False;
    }

    @Override
    public PyObject __gt__(PyObject other) {
        return unicode___gt__(other);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.unicode___gt___doc)
    final PyObject unicode___gt__(PyObject other) {
        String s = coerceForComparison(other);
        if (s == null) {
            return null;
        }
        return getString().compareTo(s) > 0 ? Py.True : Py.False;
    }

    @Override
    public PyObject __ge__(PyObject other) {
        return unicode___ge__(other);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.unicode___ge___doc)
    final PyObject unicode___ge__(PyObject other) {
        String s = coerceForComparison(other);
        if (s == null) {
            return null;
        }
        return getString().compareTo(s) >= 0 ? Py.True : Py.False;
    }

    @ExposedMethod(doc = BuiltinDocs.unicode___hash___doc)
    final int unicode___hash__() {
        return str___hash__();
    }

    @Override
    protected PyObject pyget(int i) {
        int codepoint = getString().codePointAt(translator.utf16Index(i));
        return Py.makeCharacter(codepoint, true);
    }

    @Override
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
        } else if (o instanceof BufferProtocol) {
            // PyByteArray, PyMemoryView, Py2kBuffer ...
            // We ought to be able to call codecs.decode on o but see Issue #2164
            try (PyBuffer buf = ((BufferProtocol) o).getBuffer(PyBUF.FULL_RO)) {
                PyString s = new PyString(buf);
                // For any sensible codec, the return is unicode and toString() is getString().
                return s.decode().toString();
            }
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
        } else if (o instanceof Py2kBuffer) {
            // We ought to be able to call codecs.decode on o but see Issue #2164
            try (PyBuffer buf = ((BufferProtocol) o).getBuffer(PyBUF.FULL_RO)) {
                PyString s = new PyString(buf);
                // For any sensible codec, the return is unicode and toString() is getString().
                return s.decode().toString();
            }
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
        return Py.TypeError("coercing to Unicode: need string or buffer, "
                + (o == null ? Py.None : o).getType().fastGetName() + " found");
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
        } else if (o instanceof BufferProtocol) {
            // PyByteArray, PyMemoryView, Py2kBuffer ...
            // We ought to be able to call codecs.decode on o but see Issue #2164
            try (PyBuffer buf = ((BufferProtocol) o).getBuffer(PyBUF.FULL_RO)) {
                PyString s = new PyString(buf);
                // For any sensible codec, the return is unicode and toString() is getString().
                PyObject u = s.decode();
                return (u instanceof PyUnicode) ? (PyUnicode) u : new PyUnicode(o.toString());
            }
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

    @Override
    public boolean __contains__(PyObject o) {
        return unicode___contains__(o);
    }

    @ExposedMethod(doc = BuiltinDocs.unicode___contains___doc)
    final boolean unicode___contains__(PyObject o) {
        String other = coerceToString(o);
        return getString().indexOf(other) >= 0;
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.unicode___getslice___doc)
    final PyObject unicode___mul__(PyObject o) {
        return str___mul__(o);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.unicode___getslice___doc)
    final PyObject unicode___rmul__(PyObject o) {
        return str___rmul__(o);
    }

    @Override
    public PyObject __add__(PyObject other) {
        return unicode___add__(other);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.unicode___getslice___doc)
    final PyObject unicode___add__(PyObject other) {
        // Interpret other as a Java String
        String s = coerceToStringOrNull(other);
        return s == null ? null : new PyUnicode(getString().concat(s));
    }

    @ExposedMethod(doc = BuiltinDocs.unicode_lower_doc)
    final PyObject unicode_lower() {
        return new PyUnicode(getString().toLowerCase());
    }

    @ExposedMethod(doc = BuiltinDocs.unicode_upper_doc)
    final PyObject unicode_upper() {
        return new PyUnicode(getString().toUpperCase());
    }

    @ExposedMethod(doc = BuiltinDocs.unicode_title_doc)
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

    @ExposedMethod(doc = BuiltinDocs.unicode_swapcase_doc)
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
                Set<Integer> sepSet = Generic.set();
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
            throw Py.TypeError(name + " arg must be None, unicode or str");
        }
    }

    @ExposedMethod(defaults = "null", doc = BuiltinDocs.unicode_strip_doc)
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

    @ExposedMethod(defaults = "null", doc = BuiltinDocs.unicode_lstrip_doc)
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

    @ExposedMethod(defaults = "null", doc = BuiltinDocs.unicode_rstrip_doc)
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

    /** {@inheritDoc} */
    @Override
    protected int _findLeft(int right) {
        String s = getString();
        for (int left = 0; left < right; left++) {
            if (!isPythonSpace(s.charAt(left))) {
                return left;
            }
        }
        return right;
    }

    /** {@inheritDoc} */
    @Override
    protected int _findRight() {
        String s = getString();
        for (int right = s.length(); --right >= 0;) {
            if (!isPythonSpace(s.charAt(right))) {
                return right;
            }
        }
        return -1;
    }

    @Override
    public PyTuple partition(PyObject sep) {
        return unicode_partition(sep);
    }

    @ExposedMethod(doc = BuiltinDocs.unicode_partition_doc)
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

        private final List<T> reversed = Generic.list();
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
            throw Py.ValueError("empty separator");
        } else {
            return new SepSplitIterator(sep, maxsplit);
        }
    }

    @Override
    public PyTuple rpartition(PyObject sep) {
        return unicode_rpartition(sep);
    }

    @ExposedMethod(doc = BuiltinDocs.unicode_rpartition_doc)
    final PyTuple unicode_rpartition(PyObject sep) {
        return unicodeRpartition(coerceToUnicode(sep));
    }

    @ExposedMethod(defaults = {"null", "-1"}, doc = BuiltinDocs.unicode_split_doc)
    final PyList unicode_split(PyObject sepObj, int maxsplit) {
        String sep = coerceToString(sepObj, true);
        if (sep != null) {
            return _split(sep, maxsplit);
        } else {
            return _split(null, maxsplit);
        }
    }

    /**
     * {@inheritDoc} The split sections will be {@link PyUnicode} and use the Python
     * <code>unicode</code> definition of "space".
     */
    @Override
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

    @ExposedMethod(defaults = {"null", "-1"}, doc = BuiltinDocs.unicode_rsplit_doc)
    final PyList unicode_rsplit(PyObject sepObj, int maxsplit) {
        String sep = coerceToString(sepObj, true);
        if (sep != null) {
            return _rsplit(sep, maxsplit);
        } else {
            return _rsplit(null, maxsplit);
        }
    }

    /**
     * {@inheritDoc} The split sections will be {@link PyUnicode} and use the Python
     * <code>unicode</code> definition of "space".
     */
    @Override
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

    @ExposedMethod(defaults = "false", doc = BuiltinDocs.unicode___getslice___doc)
    final PyList unicode_splitlines(boolean keepends) {
        return new PyList(new LineSplitIterator(keepends));
    }

    @Override
    protected PyString fromSubstring(int begin, int end) {
        assert (isBasicPlane()); // can only be used on a codepath from str_ equivalents
        return new PyUnicode(getString().substring(begin, end), true);
    }

    @ExposedMethod(defaults = {"null", "null"}, doc = BuiltinDocs.unicode_index_doc)
    final int unicode_index(PyObject subObj, PyObject start, PyObject end) {
        final String sub = coerceToString(subObj);
        // Now use the mechanics of the PyString on the UTF-16.
        return checkIndex(_find(sub, start, end));
    }

    @ExposedMethod(defaults = {"null", "null"}, doc = BuiltinDocs.unicode_index_doc)
    final int unicode_rindex(PyObject subObj, PyObject start, PyObject end) {
        final String sub = coerceToString(subObj);
        // Now use the mechanics of the PyString on the UTF-16.
        return checkIndex(_rfind(sub, start, end));
    }

    @ExposedMethod(defaults = {"null", "null"}, doc = BuiltinDocs.unicode_count_doc)
    final int unicode_count(PyObject subObj, PyObject start, PyObject end) {
        final PyUnicode sub = coerceToUnicode(subObj);
        if (isBasicPlane()) {
            return _count(sub.getString(), start, end);
        }
        int[] indices = super.translateIndices(start, end); // do not convert to utf-16 indices.
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

    @ExposedMethod(defaults = {"null", "null"}, doc = BuiltinDocs.unicode_find_doc)
    final int unicode_find(PyObject subObj, PyObject start, PyObject end) {
        int found = _find(coerceToString(subObj), start, end);
        return found < 0 ? -1 : translator.codePointIndex(found);
    }

    @ExposedMethod(defaults = {"null", "null"}, doc = BuiltinDocs.unicode_rfind_doc)
    final int unicode_rfind(PyObject subObj, PyObject start, PyObject end) {
        int found = _rfind(coerceToString(subObj), start, end);
        return found < 0 ? -1 : translator.codePointIndex(found);
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
            throw Py.TypeError(function + "() argument 2 must be char, not str");
        }
        return fillchar.codePointAt(0);
    }

    @ExposedMethod(defaults = "null", doc = BuiltinDocs.unicode___getslice___doc)
    final PyObject unicode_ljust(int width, String padding) {
        int n = width - getCodePointCount();
        if (n <= 0) {
            return new PyUnicode(getString());
        } else {
            return new PyUnicode(getString() + padding(n, parse_fillchar("ljust", padding)));
        }
    }

    @ExposedMethod(defaults = "null", doc = BuiltinDocs.unicode___getslice___doc)
    final PyObject unicode_rjust(int width, String padding) {
        int n = width - getCodePointCount();
        if (n <= 0) {
            return new PyUnicode(getString());
        } else {
            return new PyUnicode(padding(n, parse_fillchar("ljust", padding)) + getString());
        }
    }

    @ExposedMethod(defaults = "null", doc = BuiltinDocs.unicode___getslice___doc)
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

    @ExposedMethod(doc = BuiltinDocs.unicode_zfill_doc)
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

    @ExposedMethod(defaults = "8", doc = BuiltinDocs.unicode___getslice___doc)
    final PyObject unicode_expandtabs(int tabsize) {
        return new PyUnicode(str_expandtabs(tabsize));
    }

    @ExposedMethod(doc = BuiltinDocs.unicode_capitalize_doc)
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

    @ExposedMethod(defaults = "-1", doc = BuiltinDocs.unicode_replace_doc)
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
                    buffer.append(iter.next().getString());
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

    // end utf-16 aware
    @Override
    public PyString join(PyObject seq) {
        return unicode_join(seq);
    }

    @ExposedMethod(doc = BuiltinDocs.unicode_join_doc)
    final PyUnicode unicode_join(PyObject seq) {
        return unicodeJoin(seq);
    }

    /**
     * Equivalent to the Python <code>unicode.startswith</code> method, testing whether a string
     * starts with a specified prefix, where a sub-range is specified by <code>[start:end]</code>.
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
    @Override
    public boolean startswith(PyObject prefix, PyObject start, PyObject end) {
        return unicode_startswith(prefix, start, end);
    }

    @ExposedMethod(defaults = {"null", "null"}, doc = BuiltinDocs.unicode_startswith_doc)
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

    /**
     * Equivalent to the Python <code>unicode.endswith</code> method, testing whether a string ends
     * with a specified suffix, where a sub-range is specified by <code>[start:end]</code>.
     * Arguments <code>start</code> and <code>end</code> are interpreted as in slice notation, with
     * null or {@link Py#None} representing "missing". <code>suffix</code> can also be a tuple of
     * suffixes to look for.
     *
     * @param suffix string to check for (or a <code>PyTuple</code> of them).
     * @param start start of slice.
     * @param end end of slice.
     * @return <code>true</code> if this string slice ends with a specified suffix, otherwise
     *         <code>false</code>.
     */
    @Override
    public boolean endswith(PyObject suffix, PyObject start, PyObject end) {
        return unicode_endswith(suffix, start, end);
    }

    @ExposedMethod(defaults = {"null", "null"}, doc = BuiltinDocs.unicode_endswith_doc)
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

    @ExposedMethod(doc = BuiltinDocs.unicode_translate_doc)
    final PyObject unicode_translate(PyObject table) {
        return _codecs.translateCharmap(this, "ignore", table);
    }

    @ExposedMethod(doc = BuiltinDocs.unicode_islower_doc)
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

    @ExposedMethod(doc = BuiltinDocs.unicode_isupper_doc)
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

    @ExposedMethod(doc = BuiltinDocs.unicode_isalpha_doc)
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

    @ExposedMethod(doc = BuiltinDocs.unicode_isalnum_doc)
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

    @ExposedMethod(doc = BuiltinDocs.unicode_isdecimal_doc)
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

    @ExposedMethod(doc = BuiltinDocs.unicode_isdigit_doc)
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

    @ExposedMethod(doc = BuiltinDocs.unicode_isnumeric_doc)
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

    @ExposedMethod(doc = BuiltinDocs.unicode_istitle_doc)
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

    @ExposedMethod(doc = BuiltinDocs.unicode_isspace_doc)
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

    // end utf-16 aware
    @ExposedMethod(doc = "isunicode is deprecated.")
    final boolean unicode_isunicode() {
        Py.warning(Py.DeprecationWarning, "isunicode is deprecated.");
        return true;
    }

    @ExposedMethod(doc = BuiltinDocs.unicode_encode_doc)
    final String unicode_encode(PyObject[] args, String[] keywords) {
        return str_encode(args, keywords);
    }

    @ExposedMethod(doc = BuiltinDocs.unicode_decode_doc)
    final PyObject unicode_decode(PyObject[] args, String[] keywords) {
        return str_decode(args, keywords);
    }

    @ExposedMethod(doc = BuiltinDocs.unicode___getnewargs___doc)
    final PyTuple unicode___getnewargs__() {
        return new PyTuple(new PyUnicode(this.getString()));
    }

    @Override
    public PyObject __format__(PyObject formatSpec) {
        return unicode___format__(formatSpec);
    }

    @ExposedMethod(doc = BuiltinDocs.unicode___format___doc)
    final PyObject unicode___format__(PyObject formatSpec) {
        // Re-use the str implementation, which adapts itself to unicode.
        return str___format__(formatSpec);
    }

    @ExposedMethod(doc = BuiltinDocs.unicode__formatter_parser_doc)
    final PyObject unicode__formatter_parser() {
        return new MarkupIterator(this);
    }

    @ExposedMethod(doc = BuiltinDocs.unicode__formatter_field_name_split_doc)
    final PyObject unicode__formatter_field_name_split() {
        FieldNameIterator iterator = new FieldNameIterator(this);
        return new PyTuple(iterator.pyHead(), iterator);
    }

    @ExposedMethod(doc = BuiltinDocs.unicode_format_doc)
    final PyObject unicode_format(PyObject[] args, String[] keywords) {
        try {
            return new PyUnicode(buildFormattedString(args, keywords, null, null));
        } catch (IllegalArgumentException e) {
            throw Py.ValueError(e.getMessage());
        }
    }

    @Override
    public Iterator<Integer> iterator() {
        return newSubsequenceIterator();
    }

    @Override
    public PyComplex __complex__() {
        return new PyString(encodeDecimal()).__complex__();
    }

    @Override
    public int atoi(int base) {
        return new PyString(encodeDecimal()).atoi(base);
    }

    @Override
    public PyLong atol(int base) {
        return new PyString(encodeDecimal()).atol(base);
    }

    @Override
    public double atof() {
        return new PyString(encodeDecimal()).atof();
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
}
