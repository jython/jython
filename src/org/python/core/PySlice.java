//Copyright (c) Corporation for National Research Initiatives
package org.python.core;

import org.python.expose.ExposedGet;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedType;

/**
 * A python slice object.
 */

@ExposedType(name = "slice")
public class PySlice extends PyObject {
    
    public static final PyType TYPE = PyType.fromClass(PySlice.class);
    
    @ExposedNew
    @ExposedMethod
    final void slice___init__(PyObject[] args, String[] keywords) {
        if(args.length == 0) {
            throw Py.TypeError("slice expected at least 1 arguments, got " + args.length);
        } else if(args.length > 3) {
            throw Py.TypeError("slice expected at most 3 arguments, got " + args.length);
        }
        ArgParser ap = new ArgParser("slice", args, keywords, "start", "stop", "step");
        if(args.length == 1) {
            stop = ap.getPyObject(0);
        } else if(args.length == 2) {
            start = ap.getPyObject(0);
            stop = ap.getPyObject(1);
        } else if(args.length == 3) {
            start = ap.getPyObject(0);
            stop = ap.getPyObject(1);
            step = ap.getPyObject(2);
        }
    }

    public PySlice(PyObject start, PyObject stop, PyObject step) {
        this(TYPE);
        if(start != null) {
            this.start = start;
        }
        if(stop != null) {
            this.stop = stop;
        }
        if(step != null) {
            this.step = step;
        }
    }

    public PySlice(PyType type) {
        super(type);
    }
    
    public PySlice() {
        this(TYPE);
    }

    public final PyObject getStart() {
        return start;
    }

    public final PyObject getStop() {
        return stop;
    }

    public final PyObject getStep() {
        return step;
    }

    public int hashCode() { 
        return slice___hash__();
    }

    @ExposedMethod
    final int slice___hash__() {
        throw Py.TypeError("unhashable type");
    } 

    public PyString __str__() {
        return new PyString(getStart().__repr__() + ":" + getStop().__repr__() + ":" +
                getStep().__repr__());
    }

    public PyString __repr__() {
        return new PyString("slice(" + getStart().__repr__() + ", " +
                getStop().__repr__() + ", " +
                getStep().__repr__() + ")");
    }

    public PyObject __eq__(PyObject o) {
        if(getType() != o.getType() && !(getType().isSubType(o.getType()))) {
            return null;
        }
        if(this == o) {
            return Py.True;
        }
        PySlice oSlice = (PySlice)o;
        if(eq(getStart(), oSlice.getStart()) && eq(getStop(), oSlice.getStop())
                && eq(getStep(), oSlice.getStep())) {
            return Py.True;
        }
        return Py.False;
    }
    
    private static final boolean eq(PyObject o1, PyObject o2) {
        return o1._cmp(o2) == 0;
    }

    public PyObject __ne__(PyObject o) {
        return __eq__(o).__not__();
    }
    
    @ExposedMethod
    public PyObject slice_indices(PyObject len) {
        int ilen;
        try {
            ilen = len.asInt(0);
        } catch(ConversionException e) {
            throw Py.TypeError("length must be an int");
        }
        int[] slice = indices(ilen);
        PyInteger[] pyInts = new PyInteger[slice.length];
        for(int i = 0; i < pyInts.length; i++) {
            pyInts[i] = Py.newInteger(slice[i]);
        }
        return new PyTuple(pyInts);
    }

    private static int calculateSliceIndex(PyObject v) {
        if(v instanceof PyInteger) {
            return ((PyInteger)v).getValue();
        } else if(v instanceof PyLong) {
            try {
                return v.asInt();
            } catch (PyException exc) {
                if (Py.matchException(exc, Py.OverflowError)) {
                    if (new PyLong(0L).__cmp__(v) < 0) {
                        return Integer.MAX_VALUE;
                    }else {
                        return 0;
                    }
                }
            }
        }
        throw Py.TypeError("slice indices must be integers or None");
    }
    
    /**
     * Calculates the actual indices of a slice with this slice's start, stop
     * and step values for a sequence of length <code>len</code>.
     * 
     * @return an array with the start at index 0, stop at index 1 and step and
     *         index 2.
     */
    public int[] indices(int len) {
        int[] slice = new int[3];
        if (getStep() == Py.None) {
            slice[STEP] = 1;
        } else {
            slice[STEP] = calculateSliceIndex(getStep());
            if (slice[STEP] == 0) {
                throw Py.ValueError("slice step cannot be zero");
            }
        }

        if (getStart() == Py.None) {
            slice[START] = slice[STEP] < 0 ? len - 1 : 0;
        } else {
            slice[START] = calculateSliceIndex(getStart());
            if(slice[START] < 0) {
                slice[START] += len;
            }
            if(slice[START] < 0) {
                slice[START] = slice[STEP] < 0 ? -1 : 0;
            }
            if(slice[START] >= len) {
                slice[START] = slice[STEP] < 0 ? len - 1 : len;
            }
        }
        if(getStop() == Py.None) {
            slice[STOP] = slice[STEP] < 0 ? -1 : len;
        } else {
            slice[STOP] = calculateSliceIndex(getStop());
            if(slice[STOP] < 0) {
                slice[STOP] += len;
            }
            if(slice[STOP] < 0) {
                slice[STOP] = -1;
            }
            if(slice[STOP] > len) {
                slice[STOP] = len;
            }
        }
        return slice;
    }
    private static final int START = 0, STOP = 1, STEP = 2;

    @ExposedGet
    public PyObject start = Py.None;

    @ExposedGet
    public PyObject stop = Py.None;

    @ExposedGet
    public PyObject step = Py.None;
}
