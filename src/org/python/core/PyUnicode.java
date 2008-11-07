package org.python.core;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.python.expose.ExposedMethod;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedType;
import org.python.expose.MethodType;
import org.python.modules._codecs;
import org.python.util.Generic;

/**
 * a builtin python unicode string.
 */
@ExposedType(name = "unicode", base = PyBaseString.class)
public class PyUnicode extends PyString implements Iterable {

    private enum Plane {

        UNKNOWN, BASIC, ASTRAL
    }
    private volatile Plane plane = Plane.UNKNOWN;
    private volatile int codePointCount = -1;
    public static final PyType TYPE = PyType.fromClass(PyUnicode.class);

    // for PyJavaClass.init()
    public PyUnicode() {
        this(TYPE, "");
    }

    public PyUnicode(String string) {
        this(TYPE, string);
    }

    public PyUnicode(String string, boolean isBasic) {
        this(TYPE, string);
        plane = isBasic ? Plane.BASIC : Plane.UNKNOWN;
    }

    public PyUnicode(PyType subtype, String string) {
        super(subtype, string);
    }

    public PyUnicode(PyString pystring) {
        this(TYPE, pystring);
    }

    public PyUnicode(PyType subtype, PyString pystring) {
        this(subtype, pystring instanceof PyUnicode ? pystring.string : pystring.decode().toString());
    }

    public PyUnicode(char c) {
        this(TYPE, String.valueOf(c));
    }

    public PyUnicode(int codepoint) {
        this(TYPE, new String(new int[]{codepoint}, 0, 1));
    }

    public PyUnicode(int[] codepoints) {
        this(new String(codepoints, 0, codepoints.length));
    }

    PyUnicode(StringBuilder buffer) {
        this(TYPE, new String(buffer));
    }

    private static StringBuilder fromCodePoints(Iterator<Integer> iter) {
        StringBuilder buffer = new StringBuilder();
        while (iter.hasNext()) {
            buffer.appendCodePoint(iter.next());
        }
        return buffer;
    }

    PyUnicode(Iterator<Integer> iter) {
        this(fromCodePoints(iter));
    }

    PyUnicode(Collection<Integer> ucs4) {
        this(ucs4.iterator());
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

    // modified to know something about codepoints; we just need to return the
    // corresponding substring; darn UTF16!
    // TODO: we could avoid doing this unnecessary copy
    @Override
    public String substring(int start, int end) {
        if (isBasicPlane()) {
            return super.substring(start, end);
        }
        return new PyUnicode(newSubsequenceIterator(start, end, 1)).string;
    }

    /**
     * Creates a PyUnicode from an already interned String. Just means it won't
     * be reinterned if used in a place that requires interned Strings.
     */
    public static PyUnicode fromInterned(String interned) {
        PyUnicode uni = new PyUnicode(TYPE, interned);
        uni.interned = true;
        return uni;
    }

    public boolean isBasicPlane() {
        if (plane == Plane.BASIC) {
            return true;
        } else if (plane == Plane.UNKNOWN) {
            plane = (string.length() == getCodePointCount()) ? Plane.BASIC : Plane.ASTRAL;
        }
        return plane == Plane.BASIC;
    }

// RETAIN THE BELOW CODE, it facilitates testing astral support more completely

//    public boolean isBasicPlane() {
//        return false;
//    }

// END RETAIN

    public int getCodePointCount() {
        if (codePointCount >= 0) {
            return codePointCount;
        }
        codePointCount = string.codePointCount(0, string.length());
        return codePointCount;
    }

    @ExposedNew
    final static PyObject unicode_new(PyNewWrapper new_, boolean init, PyType subtype,
            PyObject[] args, String[] keywords) {
        ArgParser ap = new ArgParser("unicode",
                args,
                keywords,
                new String[]{"string",
            "encoding",
            "errors"
        },
                0);
        PyObject S = ap.getPyObject(0, null);
        String encoding = ap.getString(1, null);
        String errors = ap.getString(2, null);
        if (new_.for_type == subtype) {
            if (S == null) {
                return new PyUnicode("");
            }
            if (S instanceof PyUnicode) {
                return new PyUnicode(((PyUnicode) S).string);
            }
            if (S instanceof PyString) {
                if (S.getType() != PyString.TYPE && encoding == null && errors == null) {
                    return S.__unicode__();
                }
                PyObject decoded = codecs.decode((PyString) S, encoding, errors);
                if (decoded instanceof PyUnicode) {
                    return new PyUnicode((PyUnicode) decoded);
                } else {
                    throw Py.TypeError("decoder did not return an unicode object (type=" +
                            decoded.getType().fastGetName() + ")");
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

    // Unicode ops consisting of basic strings can only produce basic strings;
    // this may not be the case for astral ones - they also might be basic, in
    // case of deletes. So optimize by providing a tainting mechanism.
    @Override
    protected PyString createInstance(String str, boolean isBasic) {
        return new PyUnicode(str, isBasic);
    }

    @Override
    public PyObject __mod__(PyObject other) {
        return unicode___mod__(other);
    }

    @ExposedMethod
    final PyObject unicode___mod__(PyObject other) {
        StringFormatter fmt = new StringFormatter(string, true);
        return fmt.format(other);
    }

    @ExposedMethod
    final PyUnicode unicode___unicode__() {
        return str___unicode__();
    }

    @Override
    public PyString __str__() {
        return unicode___str__();
    }

    @ExposedMethod
    final PyString unicode___str__() {
        return new PyString(encode());
    }

    @Override
    public int __len__() {
        return unicode___len__();
    }

    @ExposedMethod
    final int unicode___len__() {
        return getCodePointCount();
    }

    @Override
    public PyString __repr__() {
        return unicode___repr__();
    }

    @ExposedMethod
    final PyString unicode___repr__() {
        return new PyString("u" + encode_UnicodeEscape(string, true));
    }

    @ExposedMethod
    final PyObject unicode___getitem__(PyObject index) {
        return str___getitem__(index);
    }

    @ExposedMethod(defaults = "null")
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
        return createInstance(new String(buffer));
    }

    @ExposedMethod(type = MethodType.CMP)
    final int unicode___cmp__(PyObject other) {
        return str___cmp__(other);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject unicode___eq__(PyObject other) {
        return str___eq__(other);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject unicode___ne__(PyObject other) {
        return str___ne__(other);
    }

    @ExposedMethod
    final int unicode___hash__() {
        return str___hash__();
    }

    @Override
    protected PyObject pyget(int i) {
        if (isBasicPlane()) {
            return Py.makeCharacter(string.charAt(i), true);
        }

        int k = 0;
        while (i > 0) {
            int W1 = string.charAt(k);
            if (W1 >= 0xD800 && W1 < 0xDC00) {
                k += 2;
            } else {
                k += 1;
            }
            i--;
        }
        int codepoint = string.codePointAt(k);
        return Py.makeCharacter(codepoint, true);
    }

    private class SubsequenceIteratorImpl implements Iterator {

        private int current,  k,  start,  stop,  step;

        SubsequenceIteratorImpl(int start, int stop, int step) {
            k = 0;
            current = start;
            this.start = start;
            this.stop = stop;
            this.step = step;
            for (int i = 0; i < start; i++) {
                nextCodePoint();
            }
        }

        SubsequenceIteratorImpl() {
            this(0, getCodePointCount(), 1);
        }

        public boolean hasNext() {
            return current < stop;
        }

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
            int W1 = string.charAt(k);
            if (W1 >= 0xD800 && W1 < 0xDC00) {
                int W2 = string.charAt(k + 1);
                U = (((W1 & 0x3FF) << 10) | (W2 & 0x3FF)) + 0x10000;
                k += 2;
            } else {
                U = W1;
                k += 1;
            }
            return U;
        }

        public void remove() {
            throw new UnsupportedOperationException("Not supported on PyUnicode objects (immutable)");
        }
    }

    private class SteppedIterator<T> implements Iterator {

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

        public boolean hasNext() {
            return lookahead != null;
        }

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

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    public Iterator newSubsequenceIterator() {
        return new SubsequenceIteratorImpl();
    }

    public Iterator newSubsequenceIterator(int start, int stop, int step) {
        if (step < 0) {
            return new SteppedIterator(step * -1, new ReversedIterator(new SubsequenceIteratorImpl(stop + 1, start + 1, 1)));
        } else {
            return new SubsequenceIteratorImpl(start, stop, step);
        }
    }

    private PyUnicode coerceToUnicode(PyObject o) {
        if (o == null) {
            return null;
        } else if (o instanceof PyUnicode) {
            return (PyUnicode) o;
        } else if (o instanceof PyString) {
            return new PyUnicode(o.toString());
        } else if (o == Py.None) {
            return null;
        } else {
            throw Py.TypeError("coercing to Unicode: need string or buffer, " +
                    o.getType().fastGetName() + "found");
        }

    }

    @ExposedMethod
    final boolean unicode___contains__(PyObject o) {
        return str___contains__(o);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject unicode___mul__(PyObject o) {
        return str___mul__(o);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject unicode___rmul__(PyObject o) {
        return str___rmul__(o);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject unicode___add__(PyObject generic_other) {
        return str___add__(generic_other);
    }

    @ExposedMethod
    final PyObject unicode_lower() {
        return new PyUnicode(str_lower());
    }

    @ExposedMethod
    final PyObject unicode_upper() {
        return new PyUnicode(str_upper());
    }

    @ExposedMethod
    final PyObject unicode_title() {
        if (isBasicPlane()) {
            return new PyUnicode(str_title());
        }
        StringBuilder buffer = new StringBuilder(string.length());
        boolean previous_is_cased = false;
        for (Iterator<Integer> iter = newSubsequenceIterator(); iter.hasNext();) {
            int codePoint = iter.next();
            if (previous_is_cased) {
                buffer.appendCodePoint(Character.toLowerCase(codePoint));
            } else {
                buffer.appendCodePoint(Character.toTitleCase(codePoint));
            }

            if (Character.isLowerCase(codePoint) ||
                    Character.isUpperCase(codePoint) ||
                    Character.isTitleCase(codePoint)) {
                previous_is_cased = true;
            } else {
                previous_is_cased = false;
            }
        }
        return new PyUnicode(buffer);
    }

    @ExposedMethod
    final PyObject unicode_swapcase() {
        if (isBasicPlane()) {
            return new PyUnicode(str_swapcase());
        }
        StringBuilder buffer = new StringBuilder(string.length());
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

    private class StripIterator implements Iterator {

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
                    if (!Character.isWhitespace(codePoint)) {
                        lookahead = codePoint;
                        return;
                    }
                }
            }
        }

        public boolean hasNext() {
            return lookahead != -1;
        }

        public Object next() {
            int old = lookahead;
            if (iter.hasNext()) {
                lookahead = iter.next();
            } else {
                lookahead = -1;
            }
            return old;
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    // compliance requires that we need to support a bit of inconsistency
    // compared to other coercion used
    private PyUnicode coerceStripSepToUnicode(PyObject o) {
        if (o == null) {
            return null;
        } else if (o instanceof PyUnicode) {
            return (PyUnicode) o;
        } else if (o instanceof PyString) {
            return new PyUnicode(((PyString) o).decode().toString());
        } else if (o == Py.None) {
            return null;
        } else {
            throw Py.TypeError("strip arg must be None, unicode or str");
        }
    }

    @ExposedMethod(defaults = "null")
    final PyObject unicode_strip(PyObject sepObj) {
        PyUnicode sep = coerceStripSepToUnicode(sepObj);
        if (isBasicPlane() && (sep == null || sep.isBasicPlane())) {
            if (sep == null) {
                return new PyUnicode(str_strip(null));
            } else {
                return new PyUnicode(str_strip(sep.string));
            }
        }
        return new PyUnicode(new ReversedIterator(new StripIterator(sep,
                new ReversedIterator(new StripIterator(sep, newSubsequenceIterator())))));
    }

    @ExposedMethod(defaults = "null")
    final PyObject unicode_lstrip(PyObject sepObj) {
        PyUnicode sep = coerceStripSepToUnicode(sepObj);
        if (isBasicPlane() && (sep == null || sep.isBasicPlane())) {
            if (sep == null) {
                return new PyUnicode(str_lstrip(null));
            } else {
                return new PyUnicode(str_lstrip(sep.string));
            }
        }
        return new PyUnicode(new StripIterator(sep, newSubsequenceIterator()));
    }

    @ExposedMethod(defaults = "null")
    final PyObject unicode_rstrip(PyObject sepObj) {
        PyUnicode sep = coerceStripSepToUnicode(sepObj);
        if (isBasicPlane() && (sep == null || sep.isBasicPlane())) {
            if (sep == null) {
                return new PyUnicode(str_rstrip(null));
            } else {
                return new PyUnicode(str_rstrip(sep.string));
            }
        }
        return new PyUnicode(new ReversedIterator(new StripIterator(sep,
                new ReversedIterator(newSubsequenceIterator()))));
    }

    @ExposedMethod
    final PyTuple unicode_partition(PyObject sep) {
        return unicodePartition(sep);
    }

    private abstract class SplitIterator implements Iterator {
        protected final int maxsplit;
        protected final Iterator<Integer> iter = newSubsequenceIterator();
        protected final LinkedList<Integer> lookahead = new LinkedList<Integer>();
        protected int numSplits = 0;
        protected boolean completeSeparator = false;

        SplitIterator(int maxsplit) {
            this.maxsplit = maxsplit;
        }

        public boolean hasNext() {
            return lookahead.peek() != null ||
                    (iter.hasNext() && (maxsplit == -1 || numSplits <= maxsplit));
        }

        protected void addLookahead(StringBuilder buffer) {
            for (int codepoint : lookahead) {
                buffer.appendCodePoint(codepoint);
            }
            lookahead.clear();
        }

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
                if (Character.isWhitespace(codepoint)) {
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

    private class PeekIterator<T> implements Iterator {

        private T lookahead = null;
        private final Iterator<T> iter;

        public PeekIterator(Iterator<T> iter) {
            this.iter = iter;
            next();
        }

        public T peek() {
            return lookahead;
        }

        public boolean hasNext() {
            return lookahead != null;
        }

        public T next() {
            T peeked = lookahead;
            lookahead = iter.hasNext() ? iter.next() : null;
            return peeked;
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    private class ReversedIterator<T> implements Iterator {

        private final List<T> reversed = Generic.list();
        private final Iterator<T> iter;

        ReversedIterator(Iterator<T> iter) {
            while (iter.hasNext()) {
                reversed.add(iter.next());
            }
            Collections.reverse(reversed);
            this.iter = reversed.iterator();
        }

        public boolean hasNext() {
            return iter.hasNext();
        }

        public T next() {
            return iter.next();
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    private class LineSplitIterator implements Iterator {

        private final PeekIterator<Integer> iter = new PeekIterator(newSubsequenceIterator());
        private final boolean keepends;

        LineSplitIterator(boolean keepends) {
            this.keepends = keepends;
        }

        public boolean hasNext() {
            return iter.hasNext();
        }

        public Object next() {
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
                } else if (codepoint == '\n' || codepoint == '\r' ||
                        Character.getType(codepoint) == Character.LINE_SEPARATOR) {
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
                for (Iterator<Integer> sepIter = sep.newSubsequenceIterator();
                        sepIter.hasNext();) {
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

    @ExposedMethod
    final PyTuple unicode_rpartition(PyObject sep) {
        return unicodeRpartition(sep);
    }

    @ExposedMethod(defaults = {"null", "-1"})
    final PyList unicode_split(PyObject sepObj, int maxsplit) {
        PyUnicode sep = coerceToUnicode(sepObj);
        if (sep != null) {
            return str_split(sep.string, maxsplit);
        } else {
            return str_split(null, maxsplit);
        }
    }

    @ExposedMethod(defaults = {"null", "-1"})
    final PyList unicode_rsplit(PyObject sepObj, int maxsplit) {
        PyUnicode sep = coerceToUnicode(sepObj);
        if (sep != null) {
            return str_rsplit(sep.string, maxsplit);
        } else {
            return str_rsplit(null, maxsplit);
        }
    }

    @ExposedMethod(defaults = "false")
    final PyList unicode_splitlines(boolean keepends) {
        if (isBasicPlane()) {
            return str_splitlines(keepends);
        }
        return new PyList(new LineSplitIterator(keepends));

    }

    @Override
    protected PyString fromSubstring(int begin, int end) {
        assert(isBasicPlane()); // can only be used on a codepath from str_ equivalents
        return new PyUnicode(string.substring(begin, end));
    }

    @ExposedMethod(defaults = {"0", "null"})
    final int unicode_index(String sub, int start, PyObject end) {
        return str_index(sub, start, end);
    }

    @ExposedMethod(defaults = {"0", "null"})
    final int unicode_rindex(String sub, int start, PyObject end) {
        return str_rindex(sub, start, end);
    }

    @ExposedMethod(defaults = {"0", "null"})
    final int unicode_count(PyObject subObj, int start, PyObject end) {
        final PyUnicode sub = coerceToUnicode(subObj);
        if (isBasicPlane()) {
            return str_count(sub.string, start, end);
        }
        int[] indices = translateIndices(start, end);
        int count = 0;
        for (Iterator<Integer> mainIter = newSubsequenceIterator(indices[0], indices[1], 1);
                mainIter.hasNext();) {
            int matched = sub.getCodePointCount();
            for (Iterator<Integer> subIter = sub.newSubsequenceIterator();
                    mainIter.hasNext() && subIter.hasNext();) {
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

    @ExposedMethod(defaults = {"0", "null"})
    final int unicode_find(String sub, int start, PyObject end) {
        return str_find(sub, start, end);
    }

    @ExposedMethod(defaults = {"0", "null"})
    final int unicode_rfind(String sub, int start, PyObject end) {
        return str_rfind(sub, start, end);
    }

    private static String padding(int n, int pad) {
        StringBuilder buffer = new StringBuilder(n);
        for (int i=0; i<n; i++)
            buffer.appendCodePoint(pad);
        return buffer.toString();
    }

    private static int parse_fillchar(String function, String fillchar) {
        if (fillchar == null) { return ' '; }
        if (fillchar.codePointCount(0, fillchar.length()) != 1) {
            throw Py.TypeError(function + "() argument 2 must be char, not str");
        }
        return fillchar.codePointAt(0);
    }

    @ExposedMethod(defaults="null")
    final PyObject unicode_ljust(int width, String padding) {
        int n = width - getCodePointCount();
        if (n <= 0) {
            return new PyUnicode(string);
        } else {
            return new PyUnicode(string + padding(n, parse_fillchar("ljust", padding)));
        }
    }

    @ExposedMethod(defaults="null")
    final PyObject unicode_rjust(int width, String padding) {
        int n = width - getCodePointCount();
        if (n <= 0) {
            return new PyUnicode(string);
        } else {
            return new PyUnicode(padding(n, parse_fillchar("ljust", padding)) + string);
        }
    }

    @ExposedMethod(defaults="null")
    final PyObject unicode_center(int width, String padding) {
        int n = width - getCodePointCount();
        if (n <= 0) {
            return new PyUnicode(string);
        }
        int half = n / 2;
        if (n % 2 > 0 && width % 2 > 0) {
            half += 1;
        }
        int pad =  parse_fillchar("center", padding);
        return new PyUnicode(padding(half, pad) + string + padding(n - half, pad));
    }

    @ExposedMethod
    final PyObject unicode_zfill(int width) {
        int n = getCodePointCount();
        if (n >= width) {
            return new PyUnicode(string);
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

    @ExposedMethod(defaults = "8")
    final PyObject unicode_expandtabs(int tabsize) {
        return new PyUnicode(str_expandtabs(tabsize));
    }

    @ExposedMethod
    final PyObject unicode_capitalize() {
        if (string.length() == 0) {
            return this;
        }
        if (isBasicPlane()) {
            return new PyUnicode(str_capitalize());
        }
        StringBuilder buffer = new StringBuilder(string.length());
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

    @ExposedMethod(defaults = "-1")
    final PyObject unicode_replace(PyObject oldPieceObj, PyObject newPieceObj, int maxsplit) {
        PyUnicode newPiece = coerceToUnicode(newPieceObj);
        PyUnicode oldPiece = coerceToUnicode(oldPieceObj);
        if (isBasicPlane() && newPiece.isBasicPlane() && oldPiece.isBasicPlane()) {
            return replace(oldPiece, newPiece, maxsplit);
        }

        StringBuilder buffer = new StringBuilder();

        if (oldPiece.getCodePointCount() == 0) {
            Iterator<Integer> iter = newSubsequenceIterator();
            for (int i = 1; (maxsplit == -1 || i < maxsplit) && iter.hasNext(); i++) {
                if (i == 1) {
                    buffer.append(newPiece.string);
                }
                buffer.appendCodePoint(iter.next());
                buffer.append(newPiece.string);
            }
            while (iter.hasNext()) {
                buffer.appendCodePoint(iter.next());
            }
            return new PyUnicode(buffer);
        } else {
            SplitIterator iter = newSplitIterator(oldPiece, maxsplit);
            int numSplits = 0;
            while (iter.hasNext()) {
                buffer.append(((PyUnicode) iter.next()).string);
                if (iter.hasNext()) {
                    buffer.append(newPiece.string);
                }
                numSplits++;
            }
            if (iter.getEndsWithSeparator() && (maxsplit == -1 || numSplits <= maxsplit)) {
                buffer.append(newPiece.string);
            }
            return new PyUnicode(buffer);
        }
    }

    // end utf-16 aware
    @ExposedMethod
    final PyString unicode_join(PyObject seq) {
        return str_join(seq);
    }

    @ExposedMethod(defaults = {"0", "null"})
    final boolean unicode_startswith(PyObject prefix, int start, PyObject end) {
        return str_startswith(prefix, start, end);
    }

    @ExposedMethod(defaults = {"0", "null"})
    final boolean unicode_endswith(PyObject suffix, int start, PyObject end) {
        return str_endswith(suffix, start, end);
    }

    @ExposedMethod
    final PyObject unicode_translate(PyObject table) {
        String trans = _codecs.translate_charmap(string, "ignore", table, true).__getitem__(0).toString();
        return new PyUnicode(trans);
    }

    // these tests need to be UTF-16 aware because they are character-by-character tests,
    // so we can only use equivalent str_XXX tests if we are in basic plane
    @ExposedMethod
    final boolean unicode_islower() {
        if (isBasicPlane()) {
            return str_islower();
        }
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

    @ExposedMethod
    final boolean unicode_isupper() {
        if (isBasicPlane()) {
            return str_isupper();
        }
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

    @ExposedMethod
    final boolean unicode_isalpha() {
        if (isBasicPlane()) {
            return str_isalpha();
        }
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

    @ExposedMethod
    final boolean unicode_isalnum() {
        if (isBasicPlane()) {
            return str_isalnum();
        }
        if (getCodePointCount() == 0) {
            return false;
        }
        for (Iterator<Integer> iter = newSubsequenceIterator(); iter.hasNext();) {
            int codePoint = iter.next();
            if (!(Character.isLetterOrDigit(codePoint) ||
                    Character.getType(codePoint) == Character.LETTER_NUMBER)) {
                return false;
            }
        }
        return true;
    }

    @ExposedMethod
    final boolean unicode_isdecimal() {
        if (isBasicPlane()) {
            return str_isdecimal();
        }
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

    @ExposedMethod
    final boolean unicode_isdigit() {
        if (isBasicPlane()) {
            return str_isdigit();
        }
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

    @ExposedMethod
    final boolean unicode_isnumeric() {
        if (isBasicPlane()) {
            return str_isnumeric();
        }
        if (getCodePointCount() == 0) {
            return false;
        }
        for (Iterator<Integer> iter = newSubsequenceIterator(); iter.hasNext();) {
            int type = Character.getType(iter.next());
            if (type != Character.DECIMAL_DIGIT_NUMBER &&
                    type != Character.LETTER_NUMBER &&
                    type != Character.OTHER_NUMBER) {
                return false;
            }
        }
        return true;
    }

    @ExposedMethod
    final boolean unicode_istitle() {
        if (isBasicPlane()) {
            return str_istitle();
        }
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

    @ExposedMethod
    final boolean unicode_isspace() {
        if (isBasicPlane()) {
            return str_isspace();
        }
        if (getCodePointCount() == 0) {
            return false;
        }
        for (Iterator<Integer> iter = newSubsequenceIterator(); iter.hasNext();) {
            if (!Character.isWhitespace(iter.next())) {
                return false;
            }
        }
        return true;
    }
    // end utf-16 aware
    @ExposedMethod
    final boolean unicode_isunicode() {
        return true;
    }

    @ExposedMethod(defaults = {"null", "null"})
    final String unicode_encode(String encoding, String errors) {
        return str_encode(encoding, errors);
    }

    @ExposedMethod(defaults = {"null", "null"})
    final PyObject unicode_decode(String encoding, String errors) {
        return str_decode(encoding, errors);
    }

    @ExposedMethod
    final PyTuple unicode___getnewargs__() {
        return new PyTuple(new PyUnicode(this.string));
    }

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
            if (Character.isWhitespace(codePoint)) {
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
            codecs.encoding_error("strict", "decimal", string, i, i + 1,
                                  "invalid decimal Unicode string");
        }
        return sb.toString();
    }

    /**
     * Encode unicode in the basic plane into a valid decimal String. Throws a
     * UnicodeEncodeError on invalid characters.
     *
     * @return a valid decimal as an encoded String
     */
    private String encodeDecimalBasic() {
        int digit;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < string.length(); i++) {
            char ch = string.charAt(i);
            if (Character.isWhitespace(ch)) {
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
            codecs.encoding_error("strict", "decimal", string, i, i + 1,
                                  "invalid decimal Unicode string");
        }
        return sb.toString();
    }

    @ExposedMethod
    final String unicode_toString() {
        return toString();
    }
}
