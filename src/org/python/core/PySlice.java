//Copyright (c) Corporation for National Research Initiatives
package org.python.core;

import org.python.expose.ExposedGet;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedType;

/**
 * The Python slice object.
 */
@ExposedType(name = "slice", isBaseType = false)
public class PySlice extends PyObject {

    public static final PyType TYPE = PyType.fromClass(PySlice.class);

    @ExposedGet
    public PyObject start = Py.None;

    @ExposedGet
    public PyObject stop = Py.None;

    @ExposedGet
    public PyObject step = Py.None;

    public PySlice() {
        this(TYPE);
    }

    public PySlice(PyType type) {
        super(type);
    }

    public PySlice(PyObject start, PyObject stop, PyObject step) {
        this(TYPE);
        if (start != null) {
            this.start = start;
        }
        if (stop != null) {
            this.stop = stop;
        }
        if (step != null) {
            this.step = step;
        }
    }

    @ExposedNew
    @ExposedMethod
    final void slice___init__(PyObject[] args, String[] keywords) {
        if (args.length == 0) {
            throw Py.TypeError("slice expected at least 1 arguments, got " + args.length);
        } else if (args.length > 3) {
            throw Py.TypeError("slice expected at most 3 arguments, got " + args.length);
        }
        ArgParser ap = new ArgParser("slice", args, keywords, "start", "stop", "step");
        if (args.length == 1) {
            stop = ap.getPyObject(0);
        } else if (args.length == 2) {
            start = ap.getPyObject(0);
            stop = ap.getPyObject(1);
        } else if (args.length == 3) {
            start = ap.getPyObject(0);
            stop = ap.getPyObject(1);
            step = ap.getPyObject(2);
        }
    }

    public int hashCode() {
        return slice___hash__();
    }

    @ExposedMethod
    final int slice___hash__() {
        throw Py.TypeError(String.format("unhashable type: '%.200s'", getType().fastGetName()));
    }

    public PyString __str__() {
        return new PyString(getStart().__repr__() + ":" + getStop().__repr__() + ":"
                            + getStep().__repr__());
    }

    public PyString __repr__() {
        return new PyString("slice(" + getStart().__repr__() + ", " + getStop().__repr__() + ", "
                            + getStep().__repr__() + ")");
    }

    public PyObject __eq__(PyObject o) {
        if (getType() != o.getType() && !(getType().isSubType(o.getType()))) {
            return null;
        }
        if (this == o) {
            return Py.True;
        }
        PySlice oSlice = (PySlice)o;
        if (eq(getStart(), oSlice.getStart()) && eq(getStop(), oSlice.getStop())
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

    public PyObject indices(PyObject len) {
        return slice_indices(len);
    }

    @ExposedMethod
    final PyObject slice_indices(PyObject len) {
        int[] indices = indicesEx(len.asIndex(Py.OverflowError));
        return new PyTuple(Py.newInteger(indices[0]), Py.newInteger(indices[1]),
                           Py.newInteger(indices[2]));
    }

    /**
     * Calculates the actual indices of a slice with this slice's start, stop, step and
     * slicelength values for a sequence of length <code>len</code>.
     *
     * @return an array with the start at index 0, stop at index 1, step at index 2 and
     *         slicelength at index 3
     */
    public int[] indicesEx(int len) {
        int start;
        int stop;
        int step;
        int slicelength;

        if (getStep() == Py.None) {
            step = 1;
        } else {
            step = calculateSliceIndex(getStep());
            if (step == 0) {
                throw Py.ValueError("slice step cannot be zero");
            }
        }

        if (getStart() == Py.None) {
            start = step < 0 ? len - 1 : 0;
        } else {
            start = calculateSliceIndex(getStart());
            if (start < 0) {
                start += len;
            }
            if (start < 0) {
                start = step < 0 ? -1 : 0;
            }
            if (start >= len) {
                start = step < 0 ? len - 1 : len;
            }
        }

        if (getStop() == Py.None) {
            stop = step < 0 ? -1 : len;
        } else {
            stop = calculateSliceIndex(getStop());
            if (stop < 0) {
                stop += len;
            }
            if (stop < 0) {
                stop = -1;
            }
            if (stop > len) {
                stop = len;
            }
        }

        if ((step < 0 && stop >= start) || (step > 0 && start >= stop)) {
            slicelength = 0;
        } else if (step < 0) {
            slicelength = (stop - start + 1) / (step) + 1;
        } else {
            slicelength = (stop - start - 1) / (step) + 1;
        }

        return new int[] {start, stop, step, slicelength};
    }

    /**
     * Calculate indices for the deprecated __get/set/delslice__ methods.
     *
     * @param obj the object being sliced
     * @param start the slice operation's start
     * @param stop the slice operation's stop
     * @return an array with start at index 0 and stop at index 1
     */
    public static PyObject[] indices2(PyObject obj, PyObject start, PyObject stop) {
        PyObject[] indices = new PyObject[2];
        int istart = (start == null || start == Py.None) ? 0 : calculateSliceIndex(start);
        int istop = (stop == null || stop == Py.None)
                ? PySystemState.maxint : calculateSliceIndex(stop);
        if (istart < 0 || istop < 0) {
            try {
                int len = obj.__len__();
                if (istart < 0) {
                    istart += len;
                }
                if (istop < 0) {
                    istop += len;
                }
            } catch (PyException pye) {
                if (!Py.matchException(pye, Py.AttributeError)) {
                    throw pye;
                }
            }
        }
        indices[0] = Py.newInteger(istart);
        indices[1] = Py.newInteger(istop);
        return indices;
    }

    public static int calculateSliceIndex(PyObject v) {
        if (v.isIndex()) {
            return v.asIndex();
        }
        throw Py.TypeError("slice indices must be integers or None or have an __index__ method");
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
}
