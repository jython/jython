//Copyright (c) Corporation for National Research Initiatives
package org.python.core;

import org.python.expose.ExposedGet;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedType;

/**
 * The Python slice object.
 */
@ExposedType(name = "slice", isBaseType = false, doc = BuiltinDocs.slice_doc)
public class PySlice extends PyObject implements Traverseproc {

    public static final PyType TYPE = PyType.fromClass(PySlice.class);

    @ExposedGet(doc = BuiltinDocs.slice_start_doc)
    public PyObject start = Py.None;

    @ExposedGet(doc = BuiltinDocs.slice_stop_doc)
    public PyObject stop = Py.None;

    @ExposedGet(doc = BuiltinDocs.slice_step_doc)
    public PyObject step = Py.None;

    public PySlice() {
        super(TYPE);
    }

    public PySlice(PyObject start, PyObject stop, PyObject step) {
        super(TYPE);
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
    static PyObject slice_new(PyNewWrapper new_, boolean init, PyType subtype, PyObject[] args,
                              String[] keywords) {
        if (args.length == 0) {
            throw Py.TypeError("slice expected at least 1 arguments, got " + args.length);
        } else if (args.length > 3) {
            throw Py.TypeError("slice expected at most 3 arguments, got " + args.length);
        }
        ArgParser ap = new ArgParser("slice", args, keywords, "start", "stop", "step");
        PySlice slice = new PySlice();
        if (args.length == 1) {
            slice.stop = ap.getPyObject(0);
        } else if (args.length == 2) {
            slice.start = ap.getPyObject(0);
            slice.stop = ap.getPyObject(1);
        } else if (args.length == 3) {
            slice.start = ap.getPyObject(0);
            slice.stop = ap.getPyObject(1);
            slice.step = ap.getPyObject(2);
        }
        return slice;
    }

    @Override
    public int hashCode() {
        return slice___hash__();
    }

    @ExposedMethod(doc = BuiltinDocs.slice___hash___doc)
    final int slice___hash__() {
        throw Py.TypeError(String.format("unhashable type: '%.200s'", getType().fastGetName()));
    }

    @Override
    public PyObject __eq__(PyObject o) {
        if (getType() != o.getType() && !(getType().isSubType(o.getType()))) {
            return null;
        }
        if (this == o) {
            return Py.True;
        }
        PySlice oSlice = (PySlice)o;
        return Py.newBoolean(eq(getStart(), oSlice.getStart()) && eq(getStop(), oSlice.getStop())
                             && eq(getStep(), oSlice.getStep()));
    }

    private static final boolean eq(PyObject o1, PyObject o2) {
        return o1._cmp(o2) == 0;
    }

    @Override
    public PyObject __ne__(PyObject o) {
        return __eq__(o).__not__();
    }

    public PyObject indices(PyObject len) {
        return slice_indices(len);
    }

    @ExposedMethod(doc = BuiltinDocs.slice_indices_doc)
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
    public int[] indicesEx(int length) {
        /* The corresponding C code (PySlice_GetIndicesEx) states:
        *  "this is harder to get right than you might think"
        *  As a consequence, I have chosen to copy the code and translate to Java.
        *  Note *rstart, etc., become result_start - the usual changes we need
        *  when going from pointers to corresponding Java.
        */

        int defstart, defstop;
        int result_start, result_stop, result_step, result_slicelength;

        if (step == Py.None) {
            result_step = 1;
        } else {
            result_step = calculateSliceIndex(step);
            if (result_step == 0) {
                throw Py.ValueError("slice step cannot be zero");
            }
        }

        defstart = result_step < 0 ? length - 1 : 0;
        defstop = result_step < 0 ? -1 : length;

        if (start == Py.None) {
            result_start = defstart;
        } else {
            result_start = calculateSliceIndex(start);
            if (result_start < 0) result_start += length;
            if (result_start < 0) result_start = (result_step < 0) ? -1 : 0;
            if (result_start >= length) {
                result_start = (result_step < 0) ? length - 1 : length;
            }
        }

        if (stop == Py.None) {
            result_stop = defstop;
        } else {
            result_stop = calculateSliceIndex(stop);
            if (result_stop < 0) result_stop += length;
            if (result_stop < 0) result_stop = (result_step < 0) ? -1 : 0;
            if (result_stop >= length) {
                result_stop = (result_step < 0) ? length - 1 : length;
            }
        }

        if ((result_step < 0 && result_stop >= result_start)
                || (result_step > 0 && result_start >= result_stop)) {
            result_slicelength = 0;
        } else if (result_step < 0) {
            result_slicelength = (result_stop - result_start + 1) / (result_step) + 1;
        } else {
            result_slicelength = (result_stop - result_start - 1) / (result_step) + 1;
        }

        return new int[]{result_start, result_stop, result_step, result_slicelength};
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
                if (!pye.match(Py.TypeError)) {
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

    @Override
    public String toString() {
        return slice_toString();
    }

    @ExposedMethod(names = "__repr__", doc = BuiltinDocs.slice___repr___doc)
    final String slice_toString() {
        return String.format("slice(%s, %s, %s)", getStart(), getStop(), getStep());
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

    @ExposedMethod
    final PyObject slice___reduce__() {
        return new PyTuple(getType(), new PyTuple(start, stop, step));
    }

    @ExposedMethod(defaults = "Py.None")
    final PyObject slice___reduce_ex__(PyObject protocol) {
        return new PyTuple(getType(), new PyTuple(start, stop, step));
    }


    /* Traverseproc implementation */
    @Override
    public int traverse(Visitproc visit, Object arg) {
        //start, stop, step cannot be null
        int retVal = visit.visit(start, arg);
        if (retVal != 0) {
            return retVal;
        }
        retVal = visit.visit(stop, arg);
        if (retVal != 0) {
            return retVal;
        }
        return visit.visit(step, arg);
    }

    @Override
    public boolean refersDirectlyTo(PyObject ob) {
        //start, stop, step cannot be null
        return ob == start || ob == stop || ob == step;
    }
}
