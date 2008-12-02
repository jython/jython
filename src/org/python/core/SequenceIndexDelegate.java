package org.python.core;

/**
 * Handles all the index checking and manipulation for get, set and del operations on a sequence.
 */
public abstract class SequenceIndexDelegate {

    public abstract int len();

    public abstract PyObject getItem(int idx);

    public abstract void setItem(int idx, PyObject value);

    public abstract void delItem(int idx);

    public abstract PyObject getSlice(int start, int stop, int step);

    public abstract void setSlice(int start, int stop, int step, PyObject value);

    public abstract void delItems(int start, int stop);

    public abstract String getTypeName();

    public void checkIdxAndSetItem(PyObject idx, PyObject value) {
        if (idx.isIndex()) {
            checkIdxAndSetItem(idx.asIndex(Py.IndexError), value);
        } else if (idx instanceof PySlice) {
            checkIdxAndSetSlice((PySlice)idx, value);
        } else {
            throw Py.TypeError(getTypeName() + " indices must be integers");
        }
    }

    public void checkIdxAndSetSlice(PySlice slice, PyObject value) {
        int[] indices = slice.indicesEx(len());
        if ((slice.step != Py.None) && value.__len__() != indices[3]) {
            throw Py.ValueError(String.format("attempt to assign sequence of size %d to extended "
                                              + "slice of size %d", value.__len__(), indices[3]));
        }
        setSlice(indices[0], indices[1], indices[2], value);
    }

    public void checkIdxAndSetItem(int idx, PyObject value) {
        setItem(checkIdx(idx), value);
    }

    public void checkIdxAndDelItem(PyObject idx) {
        if (idx.isIndex()) {
            delItem(checkIdx(idx.asIndex(Py.IndexError)));
        } else if (idx instanceof PySlice) {
            int[] indices = ((PySlice)idx).indicesEx(len());
            delSlice(indices[0], indices[1], indices[2]);
        } else {
            throw Py.TypeError(getTypeName() + " indices must be integers");
        }
    }

    public PyObject checkIdxAndGetItem(PyObject idx) {
        PyObject res = checkIdxAndFindItem(idx);
        if (res == null) {
            throw Py.IndexError("index out of range: " + idx);
        }
        return res;
    }

    public PyObject checkIdxAndFindItem(PyObject idx) {
        if (idx.isIndex()) {
            return checkIdxAndFindItem(idx.asIndex(Py.IndexError));
        } else if (idx instanceof PySlice) {
            return getSlice((PySlice)idx);
        } else {
            throw Py.TypeError(getTypeName() + " indices must be integers");
        }
    }

    public PyObject getSlice(PySlice slice) {
        int[] indices = slice.indicesEx(len());
        return getSlice(indices[0], indices[1], indices[2]);
    }

    public PyObject checkIdxAndFindItem(int idx) {
        idx = fixindex(idx);
        if(idx == -1) {
            return null;
        } else {
            return getItem(idx);
        }
    }

    private int checkIdx(int idx) {
        int i = fixindex(idx);
        if (i == -1) {
            throw Py.IndexError(getTypeName() + " assignment index out of range");
        }
        return i;
    }

    int fixindex(int index) {
        int l = len();
        if(index < 0) {
            index += l;
        }
        if(index < 0 || index >= l) {
            return -1;
        } else {
            return index;
        }
    }

    private void delSlice(int start, int stop, int step) {
        if(step == 1) {
            if (stop > start) {
                delItems(start, stop);
            }
        } else if(step > 1) {
            for(int i = start; i < stop; i += step) {
                delItem(i);
                i--;
                stop--;
            }
        } else if(step < 0) {
            for(int i = start; i >= 0 && i >= stop; i += step) {
                delItem(i);
            }
        }
    }
}