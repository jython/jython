// Copyright (c) Corporation for National Research Initiatives
package org.python.core;

/**
 * The abstract superclass of PyObjects that implements a Sequence. Minimize the work in creating
 * such objects.
 *
 * Method names are designed to make it possible for subclasses of PySequence to implement
 * java.util.List.
 *
 * Subclasses must also implement get, getslice, and repeat methods.
 *
 * Subclasses that are mutable should also implement: set, setslice, del, and delRange.
 */
public abstract class PySequence extends PyObject {

    /**
     * This constructor is used by PyJavaClass.init()
     */
    public PySequence() {}
    public int gListAllocatedStatus = -1;

    protected PySequence(PyType type) {
        super(type);
    }

    // These methods must be defined for any sequence
    /**
     * @param index
     *            index of element to return.
     * @return the element at the given position in the list.
     */
    abstract protected PyObject pyget(int index);

    /**
     * Returns a range of elements from the sequence.
     *
     * @param start
     *            the position of the first element.
     * @param stop
     *            one more than the position of the last element.
     * @param step
     *            the step size.
     * @return a sequence corresponding the the given range of elements.
     */
    abstract protected PyObject getslice(int start, int stop, int step);

    /**
     * Repeats the given sequence.
     *
     * @param count
     *            the number of times to repeat the sequence.
     * @return this sequence repeated count times.
     */
    abstract protected PyObject repeat(int count);

    // These methods only apply to mutable sequences
    /**
     * Sets the given element of the sequence.
     *
     * @param index
     *            index of the element to set.
     * @param value
     *            the value to set this element to.
     */
    protected void pyset(int index, PyObject value) {
        throw Py.TypeError("can't assign to immutable object");
    }

    /**
     * Sets the given range of elements.
     */
    protected void setslice(int start, int stop, int step, PyObject value) {
        throw Py.TypeError(String.format("'%s' object does not support item assignment",
                                         getType().fastGetName()));
    }

    protected void del(int i) throws PyException {
        throw Py.TypeError(String.format("'%s' object does not support item deletion",
                                         getType().fastGetName()));
    }

    protected void delRange(int start, int stop) {
        throw Py.TypeError(String.format("'%s' object does not support item deletion",
                                         getType().fastGetName()));

    }

    public boolean __nonzero__() {
        return seq___nonzero__();
    }

    final boolean seq___nonzero__() {
        return __len__() != 0;
    }

    public PyObject __iter__() {
        return seq___iter__();
    }

    final PyObject seq___iter__() {
        return new PySequenceIter(this);
    }

    public synchronized PyObject __eq__(PyObject o) {
        return seq___eq__(o);
    }

    final synchronized PyObject seq___eq__(PyObject o) {
        if(!(getType() == o.getType()) && !(getType().isSubType(o.getType()))) {
            return null;
        }
        int tl = __len__();
        int ol = o.__len__();
        if(tl != ol) {
            return Py.False;
        }
        int i = cmp(this, tl, o, ol);
        return (i < 0) ? Py.True : Py.False;
    }

    public synchronized PyObject __ne__(PyObject o) {
        return seq___ne__(o);
    }

    final synchronized PyObject seq___ne__(PyObject o) {
        if(!(getType() == o.getType()) && !(getType().isSubType(o.getType()))) {
            return null;
        }
        int tl = __len__();
        int ol = o.__len__();
        if(tl != ol) {
            return Py.True;
        }
        int i = cmp(this, tl, o, ol);
        return (i < 0) ? Py.False : Py.True;
    }

    public synchronized PyObject __lt__(PyObject o) {
        return seq___lt__(o);
    }

    final synchronized PyObject seq___lt__(PyObject o) {
        if(!(getType() == o.getType()) && !(getType().isSubType(o.getType()))) {
            return null;
        }
        int i = cmp(this, -1, o, -1);
        if(i < 0) {
            return (i == -1) ? Py.True : Py.False;
        }
        return __finditem__(i)._lt(o.__finditem__(i));
    }

    public synchronized PyObject __le__(PyObject o) {
        return seq___le__(o);
    }

    final synchronized PyObject seq___le__(PyObject o) {
        if(!(getType() == o.getType()) && !(getType().isSubType(o.getType()))) {
            return null;
        }
        int i = cmp(this, -1, o, -1);
        if(i < 0) {
            return (i == -1 || i == -2) ? Py.True : Py.False;
        }
        return __finditem__(i)._le(o.__finditem__(i));
    }

    public synchronized PyObject __gt__(PyObject o) {
        return seq___gt__(o);
    }

    final synchronized PyObject seq___gt__(PyObject o) {
        if(!(getType() == o.getType()) && !(getType().isSubType(o.getType()))) {
            return null;
        }
        int i = cmp(this, -1, o, -1);
        if(i < 0)
            return (i == -3) ? Py.True : Py.False;
        return __finditem__(i)._gt(o.__finditem__(i));
    }

    public synchronized PyObject __ge__(PyObject o) {
        return seq___ge__(o);
    }

    final synchronized PyObject seq___ge__(PyObject o) {
        if(!(getType() == o.getType()) && !(getType().isSubType(o.getType()))) {
            return null;
        }
        int i = cmp(this, -1, o, -1);
        if(i < 0) {
            return (i == -3 || i == -2) ? Py.True : Py.False;
        }
        return __finditem__(i)._ge(o.__finditem__(i));
    }

    // Return value >= 0 is the index where the sequences differs.
    // -1: reached the end of o1 without a difference
    // -2: reached the end of both seqeunces without a difference
    // -3: reached the end of o2 without a difference
    protected static int cmp(PyObject o1, int ol1, PyObject o2, int ol2) {
        if(ol1 < 0) {
            ol1 = o1.__len__();
        }
        if(ol2 < 0) {
            ol2 = o2.__len__();
        }
        for(int i = 0; i < ol1 && i < ol2; i++) {
            if(!o1.__getitem__(i)._eq(o2.__getitem__(i)).__nonzero__()) {
                return i;
            }
        }
        if(ol1 == ol2) {
            return -2;
        }
        return (ol1 < ol2) ? -1 : -3;
    }

    // Return a copy of a sequence where the __len__() method is
    // telling the truth.
    protected static PySequence fastSequence(PyObject seq, String msg) {
        if (seq instanceof PySequence) {
            return (PySequence)seq;
        }
        PyList list = new PyList();
        PyObject iter = Py.iter(seq, msg);
        for(PyObject item = null; (item = iter.__iternext__()) != null;) {
            list.append(item);
        }
        return list;
    }

    // make step a long in case adding the start, stop and step together overflows an int
    protected static final int sliceLength(int start, int stop, long step) {
        int ret;
        if(step > 0) {
            ret = (int)((stop - start + step - 1) / step);
        } else {
            ret = (int)((stop - start + step + 1) / step);
        }
        if(ret < 0) {
            return 0;
        }
        return ret;
    }

    /**
     * Adjusts <code>index</code> such that it's >= 0 and <= __len__. If <code>index</code> starts
     * off negative, it's treated as an index from the end of the sequence going back to the start.
     */
    protected int boundToSequence(int index) {
        int length = __len__();
        if(index < 0) {
            index = index += length;
            if(index < 0) {
                index = 0;
            }
        } else if(index > length) {
            index = length;
        }
        return index;
    }

    public PyObject __finditem__(int index) {
        return seq___finditem__(index);
    }

    final synchronized PyObject seq___finditem__(int index) {
        return delegator.checkIdxAndFindItem(index);
    }

    public PyObject __finditem__(PyObject index) {
        return seq___finditem__(index);
    }

    final PyObject seq___finditem__(PyObject index) {
        return delegator.checkIdxAndFindItem(index);
    }

    public PyObject __getitem__(PyObject index) {
        return seq___getitem__(index);
    }

    final PyObject seq___getitem__(PyObject index) {
        return delegator.checkIdxAndGetItem(index);
    }

    public boolean isMappingType() throws PyIgnoreMethodTag {
        return false;
    }

    public boolean isNumberType() throws PyIgnoreMethodTag {
        return false;
    }

    public synchronized PyObject __getslice__(PyObject start, PyObject stop, PyObject step) {
        return seq___getslice__(start, stop, step);
    }

    final synchronized PyObject seq___getslice__(PyObject start, PyObject stop, PyObject step) {
        return delegator.getSlice(new PySlice(start, stop, step));
    }

    public synchronized void __setslice__(PyObject start,
                                          PyObject stop,
                                          PyObject step,
                                          PyObject value) {
        seq___setslice__(start, stop, step, value);
    }

    final synchronized void seq___setslice__(PyObject start,
                                             PyObject stop,
                                             PyObject step,
                                             PyObject value) {
        if (value == null) {
            value = step;
            step = null;
        }
        delegator.checkIdxAndSetSlice(new PySlice(start, stop, step), value);
    }

    public synchronized void __delslice__(PyObject start, PyObject stop, PyObject step) {
        seq___delslice__(start, stop, step);
    }

    final synchronized void seq___delslice__(PyObject start, PyObject stop, PyObject step) {
        delegator.checkIdxAndDelItem(new PySlice(start, stop, step));
    }

    public synchronized void __setitem__(int index, PyObject value) {
        delegator.checkIdxAndSetItem(index, value);
    }

    public void __setitem__(PyObject index, PyObject value) {
        seq___setitem__(index, value);
    }

    final void seq___setitem__(PyObject index, PyObject value) {
        delegator.checkIdxAndSetItem(index, value);
    }

    public synchronized void __delitem__(PyObject index) {
        seq___delitem__(index);
    }

    final synchronized void seq___delitem__(PyObject index) {
        delegator.checkIdxAndDelItem(index);
    }

    public synchronized Object __tojava__(Class c) throws PyIgnoreMethodTag {
        if(c.isArray()) {
            Class component = c.getComponentType();
            try {
                int n = __len__();
                PyArray array = new PyArray(component, n);
                for(int i = 0; i < n; i++) {
                    PyObject o = pyget(i);
                    array.set(i, o);
                }
                return array.getArray();
            } catch(Throwable t) {
                // ok
            }
        }
        return super.__tojava__(c);
    }

    /**
     * Return sequence-specific error messages suitable for substitution.
     *
     * {0} is the op name. {1} is the left operand type. {2} is the right operand type.
     */
    protected String unsupportedopMessage(String op, PyObject o2) {
        if(op.equals("*")) {
            return "can''t multiply sequence by non-int of type ''{2}''";
        }
        return null;
    }

    /**
     * Return sequence-specific error messages suitable for substitution.
     *
     * {0} is the op name. {1} is the left operand type. {2} is the right operand type.
     */
    protected String runsupportedopMessage(String op, PyObject o2) {
        if(op.equals("*")) {
            return "can''t multiply sequence by non-int of type ''{1}''";
        }
        return null;
    }

    protected final SequenceIndexDelegate delegator = new SequenceIndexDelegate() {

        @Override
        public String getTypeName() {
            return getType().fastGetName();
        }

        @Override
        public void setItem(int idx, PyObject value) {
            pyset(idx, value);
        }

        @Override
        public void setSlice(int start, int stop, int step, PyObject value) {
            setslice(start, stop, step, value);
        }

        @Override
        public int len() {
            return __len__();
        }

        @Override
        public void delItem(int idx) {
            del(idx);
        }

        @Override
        public void delItems(int start, int stop) {
            delRange(start, stop);
        }


        @Override
        public PyObject getItem(int idx) {
            return pyget(idx);
        }

        @Override
        public PyObject getSlice(int start, int stop, int step) {
            return getslice(start, stop, step);
        }
    };
}
