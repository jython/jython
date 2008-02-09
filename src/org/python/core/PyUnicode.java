package org.python.core;

import org.python.expose.ExposedMethod;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedType;
import org.python.expose.MethodType;
import org.python.modules._codecs;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

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

    PyUnicode(StringBuilder buffer) {
        this(TYPE, new String(buffer));
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
    public String safeRepr() throws PyIgnoreMethodTag {
        return "'unicode' object";
    }

    @Override
    public PyString createInstance(String str) {
        return new PyUnicode(str);
    }

    @Override
    public PyObject __mod__(PyObject other) {
        return unicode___mod__(other);
    }

    @ExposedMethod
    final PyObject unicode___mod__(PyObject other) {
        StringFormatter fmt = new StringFormatter(string, true);
        return fmt.format(other).__unicode__();
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

    // TODO; this method does not appear to be called currently!
    // something is wrong with MRO, it seems
    @ExposedMethod
    final int unicode___len__() {
        return getCodePointCount();
    }

    @Override
    public PyString __repr__() {
        return new PyString("u" + encode_UnicodeEscape(string, true));
    }

    @ExposedMethod(names = "__repr__")
    public String unicode_toString() {
        return "u'" + encode_UnicodeEscape(string, false) + "'";
    }

    @ExposedMethod
    public PyObject unicode___getitem__(PyObject index) {
        return seq___finditem__(index);
    }

    @ExposedMethod(defaults = "null")
    public PyObject unicode___getslice__(PyObject start, PyObject stop, PyObject step) {
        return seq___getslice__(start, stop, step);
    }

    @Override
    protected PyObject getslice(int start, int stop, int step) {
        if (step > 0 && stop < start) {
            stop = start;
        }

        StringBuilder buffer = new StringBuilder(sliceLength(start, stop, step));
        for (Iterator<Integer> iter = new SubsequenceIterator(start, stop, step); iter.hasNext();) {
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
                int W2 = string.charAt(k + 1);
                k += 2;
            } else {
                k += 1;
            }
            i--;
        }
        int codepoint = string.codePointAt(k);
        return Py.makeCharacter(codepoint, true);
    }

    // TODO: need to support negative steps;
    // presumably this is via  a reversed iterator
    private class SubsequenceIterator implements Iterator {

        private int current,  k,  start,  stop,  step;

        SubsequenceIterator(int start, int stop, int step) {
            k = 0;
            current = start;
            this.start = start;
            this.stop = stop;
            this.step = step;
            if (start > 0) {
                advance(start);
            }
        }

        SubsequenceIterator() {
            this(0, getCodePointCount(), 1);
        }

        public boolean hasNext() {
            return current + step <= stop;
        }

        public Object next() {
            current += step;
            return advance(step);
        }

        private int advance() {
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
        
        private int advance(int i) {
            int U = advance();
            while (i-- > 1) {
                advance();
            }
            return U;
        }

        public void remove() {
            throw new UnsupportedOperationException("Not supported on PyUnicode objects (immutable)");
        }
    }

    public Iterator newSubsequenceIterator() {
        return new SubsequenceIterator();
    }

    public Iterator newSubsequenceIterator(int start, int stop, int step) {
        return new SubsequenceIterator(start, stop, step);
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
        return new PyUnicode(str_title());
    }

    @ExposedMethod
    final PyObject unicode_swapcase() {
        return new PyUnicode(str_swapcase());
    }

    @ExposedMethod
    final PyObject unicode_strip(PyObject[] args, String[] kws) {
        int nargs = args.length;
        if (nargs == 0) {
            return new PyUnicode(str_strip(null));
        }
        if (nargs > 1) {
            throw Py.TypeError("strip() takes at most 1 argument");
        }
        PyObject sep = args[0];
        if (sep == Py.None) {
            return new PyUnicode(str_strip(null));
        } else if (sep instanceof PyUnicode) {
            return new PyUnicode(str_strip(sep.toString()));
        } else if (sep instanceof PyString) {
            return new PyUnicode(str_strip(((PyString) sep).decode().toString()));
        }
        throw Py.TypeError("strip arg must be None, unicode or str");
    }

    @ExposedMethod(defaults = "null")
    final PyObject unicode_lstrip(String sep) {
        return new PyUnicode(str_lstrip(sep));
    }

    @ExposedMethod(defaults = "null")
    final PyObject unicode_rstrip(String sep) {
        return new PyUnicode(str_rstrip(sep));
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
        PeekIterator(Iterator<T> iter) {
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

    private class LineSplitIterator implements Iterator {
        private final PeekIterator<Integer> iter = new PeekIterator(new SubsequenceIterator());
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
                    }
                    else {
                        iter.next();
                    }
                    break;
                }
                else if (codepoint == '\n' || codepoint == '\r' ||
                    Character.getType(codepoint) == Character.LINE_SEPARATOR) {
                    if (keepends) {
                        buffer.appendCodePoint(codepoint);
                    }
                    break;
                }
                else {
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
                // what I don't like about this is we are initing a new subsequence iter
                // for every mismatch; why not cache the first codepoint? yes, add that
                // complexity, it makes sense
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

    @ExposedMethod(defaults = {"null", "-1"})
    final PyList unicode_split(PyObject sep, int maxsplit) {
        // return str_split(sep, maxsplit); -- it will be interested to compare efficiency of implementations
        // for BMP and this new support
        return new PyList(newSplitIterator(coerceToUnicode(sep), maxsplit));
    }

    @ExposedMethod(defaults = "false")
    final PyList unicode_splitlines(boolean keepends) {
        return new PyList(new LineSplitIterator(keepends));
    // return str_splitlines(keepends);
    }

    // this is with respect to codeunits, and is used only by code that assumes
    // the plane is basic
    @Override
    protected PyString fromSubstring(int begin, int end) {
        throw new UnsupportedOperationException();
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

    // need to be utf-16 aware
    @ExposedMethod
    final PyObject unicode_ljust(int width) {
        return new PyUnicode(str_ljust(width));
    }

    @ExposedMethod
    final PyObject unicode_rjust(int width) {
        return new PyUnicode(str_rjust(width));
    }

    @ExposedMethod
    final PyObject unicode_center(int width) {
        return new PyUnicode(str_center(width));
    }

    @ExposedMethod
    final PyObject unicode_zfill(int width) {
        return new PyUnicode(str_zfill(width));
    }

    @ExposedMethod(defaults = "8")
    final PyObject unicode_expandtabs(int tabsize) {
        return new PyUnicode(str_expandtabs(tabsize));
    }

    @ExposedMethod
    final PyObject unicode_capitalize() {
        return new PyUnicode(str_capitalize());
    }

    @ExposedMethod(defaults = "-1")
    final PyObject unicode_replace(PyObject oldPieceObj, PyObject newPieceObj, int maxsplit) {
        StringBuilder buffer = new StringBuilder();
        PyUnicode newPiece = coerceToUnicode(newPieceObj);
        PyUnicode oldPiece = coerceToUnicode(oldPieceObj);

        if (oldPiece.getCodePointCount() == 0) {
            SubsequenceIterator iter = new SubsequenceIterator();
            for (int i = 1; (maxsplit == -1 || i < maxsplit) && iter.hasNext(); i++) {
                if (i == 1) {
                    buffer.append(newPiece.string);
                }
                buffer.appendCodePoint((Integer) iter.next());
                buffer.append(newPiece.string);
            }
            while (iter.hasNext()) {
                buffer.appendCodePoint((Integer) iter.next());
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
    final boolean unicode_startswith(String prefix, int start, PyObject end) {
        return str_startswith(prefix, start, end);
    }

    @ExposedMethod(defaults = {"0", "null"})
    final boolean unicode_endswith(String suffix, int start, PyObject end) {
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
        for (Iterator<Integer> iter = new SubsequenceIterator(); iter.hasNext();) {
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
        for (Iterator<Integer> iter = new SubsequenceIterator(); iter.hasNext();) {
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
        return str_isalpha();
    }

    @ExposedMethod
    final boolean unicode_isalnum() {
        return str_isalnum();
    }

    @ExposedMethod
    final boolean unicode_isdecimal() {
        return str_isdecimal();
    }

    @ExposedMethod
    final boolean unicode_isdigit() {
        if (isBasicPlane()) {
            return str_isdigit();
        }
        for (Iterator<Integer> iter = new SubsequenceIterator(); iter.hasNext();) {
            if (!Character.isDigit(iter.next())) {
                return false;
            }
        }
        return true;
    }

    @ExposedMethod
    final boolean unicode_isnumeric() {
        return str_isnumeric();
    }

    @ExposedMethod
    final boolean unicode_istitle() {
        return str_istitle();
    }

    @ExposedMethod
    final boolean unicode_isspace() {
        return str_isspace();
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

    final PyTuple unicode___getnewargs__() {
        return new PyTuple(new PyUnicode(this.string));
    }

    public Iterator<Integer> iterator() {
        return newSubsequenceIterator();
    }
}
