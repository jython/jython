// Copyright (c) Corporation for National Research Initiatives
package org.python.core;

import org.python.expose.ExposedMethod;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedType;

/**
 * The builtin xrange type.
 */
// XXX: Not subclassable
@ExposedType(name = "xrange", base = PyObject.class)
public class PyXRange extends PySequence {

    public static final PyType TYPE = PyType.fromClass(PyXRange.class);

    private int start;

    private int step;

    private int len;

    public PyXRange(int ihigh) {
        this(0, ihigh, 1);
    }

    public PyXRange(int ilow, int ihigh) {
        this(ilow, ihigh, 1);
    }

    public PyXRange(int ilow, int ihigh, int istep) {
        if (istep == 0) {
            throw Py.ValueError("xrange() arg 3 must not be zero");
        }

        int n;
        if (istep > 0) {
            n = getLenOfRange(ilow, ihigh, istep);
        } else {
            n = getLenOfRange(ihigh, ilow, -istep);
        }
        if (n < 0) {
            throw Py.OverflowError("xrange() result has too many items");
        }

        start = ilow;
        len = n;
        step = istep;
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
    static int getLenOfRange(int lo, int hi, int step) {
        int n = 0;
        if (lo < hi) {
            // the base difference may be > Integer.MAX_VALUE
            long diff = (long)hi - (long)lo - 1;
            // any long > Integer.MAX_VALUE or < Integer.MIN_VALUE gets casted to a
            // negative number
            n = (int)((diff / step) + 1);
        }
        return n;
    }

    @Override
    public int __len__() {
        return xrange___len__();
    }

    @ExposedMethod
    final int xrange___len__() {
        return len;
    }

    @Override
    public PyObject __getitem__(PyObject index) {
        return xrange___getitem__(index);
    }

    @ExposedMethod
    final PyObject xrange___getitem__(PyObject index) {
        PyObject ret = seq___finditem__(index);
        if (ret == null) {
            throw Py.IndexError("xrange object index out of range");
        }
        return ret;
    }

    @ExposedMethod
    public PyObject xrange___iter__() {
        return seq___iter__();
    }

    @Override
    protected PyObject pyget(int i) {
        return Py.newInteger(start + (i % len) * step);
    }

    @Override
    protected PyObject getslice(int start, int stop, int step) {
        // not supported
        return null;
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
        String rtn;
        int stop = start + len * step;
        if (start == 0 && step == 1) {
            rtn = String.format("xrange(%d)", stop);
        } else if (step == 1) {
            rtn = String.format("xrange(%d, %d)", start, stop);
        } else {
            rtn = String.format("xrange(%d, %d, %d)", start, stop, step);
        }
        return rtn;
    }
}
