// Copyright (c) Corporation for National Research Initiatives
package org.python.core;

import org.python.expose.ExposedMethod;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * The builtin xrange type.
 */
@Untraversable
@ExposedType(name = "xrange", base = PyObject.class, isBaseType = false,
             doc = BuiltinDocs.xrange_doc)
public class PyXRange extends PySequence {

    public static final PyType TYPE = PyType.fromClass(PyXRange.class);

    private final long start;
    private final long step;
    private final long stop;
    private final long len;

    public PyXRange(int ihigh) {
        this(0, ihigh, 1);
    }

    public PyXRange(int ilow, int ihigh) {
        this(ilow, ihigh, 1);
    }

    public PyXRange(int ilow, int ihigh, int istep) {
        super(TYPE);

        if (istep == 0) {
            throw Py.ValueError("xrange() arg 3 must not be zero");
        }

        int n;
        long listep = istep;
        if (listep > 0) {
            n = getLenOfRange(ilow, ihigh, listep);
        } else {
            n = getLenOfRange(ihigh, ilow, -listep);
        }
        if (n < 0) {
            throw Py.OverflowError("xrange() result has too many items");
        }
        start = ilow;
        len = n;
        step = istep;
        stop = ihigh;
    }

    @ExposedNew
    static final PyObject xrange___new__(PyNewWrapper new_, boolean init, PyType subtype,
                                         PyObject[] args, String[] keywords) {
        ArgParser ap = new ArgParser("xrange", args, keywords,
                                     new String[] {"ilow", "ihigh", "istep"}, 1);
        ap.noKeywords();

        int ilow = 0;
        int ihigh;
        int istep = 1;
        if (args.length == 1) {
            ihigh = ap.getInt(0);
        } else {
            ilow = ap.getInt(0);
            ihigh = ap.getInt(1);
            istep = ap.getInt(2, 1);
        }
        return new PyXRange(ilow, ihigh, istep);
    }

    /**
     * Return number of items in range/xrange (lo, hi, step).  step > 0 required.  Return
     * a value < 0 if & only if the true value is too large to fit in a Java int.
     *
     * @param lo int value
     * @param hi int value
     * @param step int value (> 0)
     * @return int length of range
     */
    static int getLenOfRange(long lo, long hi, long step) {
        if (lo < hi) {
            // the base difference may be > Integer.MAX_VALUE
            long diff = hi - lo - 1;
            // any long > Integer.MAX_VALUE or < Integer.MIN_VALUE gets cast to a
            // negative number
            return (int)((diff / step) + 1);
        } else {
            return 0;
        }
    }

    @Override
    public int __len__() {
        return xrange___len__();
    }

    @ExposedMethod(doc = BuiltinDocs.xrange___len___doc)
    final int xrange___len__() {
        return (int)len;
    }

    @Override
    public PyObject __getitem__(PyObject index) {
        return xrange___getitem__(index);
    }

    @ExposedMethod(doc = BuiltinDocs.xrange___getitem___doc)
    final PyObject xrange___getitem__(PyObject index) {
        PyObject ret = seq___finditem__(index);
        if (ret == null) {
            throw Py.IndexError("xrange object index out of range");
        }
        return ret;
    }

    @Override
    public PyObject __iter__() {
        return xrange___iter__();
    }

    @ExposedMethod(doc = BuiltinDocs.xrange___iter___doc)
    public PyObject xrange___iter__() {
        return range_iter();
    }

    @ExposedMethod(doc = BuiltinDocs.xrange___reversed___doc)
    public PyObject xrange___reversed__() {
        return range_reverse();
    }

    private final PyXRangeIter range_iter() {
        return new PyXRangeIter(0, (long)start, (long)step, (long)len);
    }

    private final PyXRangeIter range_reverse() {
        return new PyXRangeIter(0,
                (start + (len - 1) * step),   // start
                (0 - step),                   // step (negative value)
                len);
    }

    @ExposedMethod
    public PyObject xrange___reduce__() {
        return new PyTuple(getType(),
                new PyTuple(Py.newInteger(start), Py.newInteger(stop), Py.newInteger(step)));
    }

    @Override
    public PyObject __reduce__() {
        return xrange___reduce__();
    }

    @Override
    protected PyObject pyget(int i) {
        return Py.newInteger(start + (i % len) * step);
    }

    @Override
    protected PyObject getslice(int start, int stop, int step) {
        throw Py.TypeError("xrange index must be integer, not 'slice'");
    }

    @Override
    protected PyObject repeat(int howmany) {
        // not supported
        return null;
    }

    @Override
    protected String unsupportedopMessage(String op, PyObject o2) {
        // always return the default unsupported messages instead of PySequence's
        return null;
    }

    @Override
    public String toString() {
        long lstop = start + len * step;
        if (lstop > PySystemState.maxint) { lstop = PySystemState.maxint; }
        else if (lstop < PySystemState.minint) { lstop = PySystemState.minint; }
        int stop = (int)lstop;
        if (start == 0 && step == 1) {
            return String.format("xrange(%d)", stop);
        } else if (step == 1) {
            return String.format("xrange(%d, %d)", start, stop);
        } else {
            return String.format("xrange(%d, %d, %d)", start, stop, step);
        }
    }

    @Override
    public Object __tojava__(Class<?> c) {
        if (c.isAssignableFrom(Iterable.class)) {
            return new JavaIterator(range_iter());
        }
        if (c.isAssignableFrom(Iterator.class)) {
            return (new JavaIterator(range_iter())).iterator();
        }
        if (c.isAssignableFrom(Collection.class)) {
            List<Object> list = new ArrayList();
            for (Object obj : new JavaIterator(range_iter())) {
                list.add(obj);
            }
            return list;
        }
        if (c.isArray()) {
            PyArray array = new PyArray(c.getComponentType(), this);
            return array.__tojava__(c);
        }
        return super.__tojava__(c);
    }
}
