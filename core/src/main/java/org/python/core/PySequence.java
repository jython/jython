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
     * A delegate that handles index checking and manipulation for get, set and del operations on
     * this sequence in the form of a "pluggable behaviour". Because different types of sequence
     * exhibit subtly different behaviour, there is scope for subclasses to customise the behaviour
     * with their own extension of <code>SequenceIndexDelegate</code>.
     */
    protected SequenceIndexDelegate delegator;

    /**
     * Construct a PySequence for the given sub-type with the default index behaviour.
     *
     * @param type actual (Python) type of sub-class
     */
    protected PySequence(PyType type) {
        super(type);
        delegator = new DefaultIndexDelegate();
    }

    /**
     * Construct a PySequence for the given sub-type with custom index behaviour. In practice,
     * restrictions on the construction of inner classes will mean null has to be passed and the
     * actual delegator assigned later.
     *
     * @param type actual (Python) type of sub-class
     * @param behaviour specific index behaviour (or null)
     */
    protected PySequence(PyType type, SequenceIndexDelegate behaviour) {
        super(type);
        delegator = behaviour;
    }

    // These methods must be defined for any sequence
    /**
     * Returns the element of the sequence at the given index. This is an extension point called by
     * PySequence in its implementation of {@link #__getitem__} It is guaranteed by PySequence that
     * when it calls <code>pyget(int)</code> the index is within the bounds of the array. Any other
     * clients must make the same guarantee.
     *
     * @param index index of element to return.
     * @return the element at the given position in the list.
     */
    protected abstract PyObject pyget(int index);

    /**
     * Returns a range of elements from the sequence.
     *
     * @param start the position of the first element.
     * @param stop one more than the position of the last element.
     * @param step the step size.
     * @return a sequence corresponding the the given range of elements.
     */
    protected abstract PyObject getslice(int start, int stop, int step);

    /**
     * Returns a (concrete subclass of) PySequence that repeats the given sequence, as in the
     * implementation of <code>__mul__</code> for strings.
     *
     * @param count the number of times to repeat the sequence.
     * @return this sequence repeated count times.
     */
    protected abstract PyObject repeat(int count);

    // These methods only apply to mutable sequences
    /**
     * Sets the indexed element of the sequence to the given value. This is an extension point
     * called by PySequence in its implementation of {@link #__setitem__} It is guaranteed by
     * PySequence that when it calls pyset(int) the index is within the bounds of the array. Any
     * other clients must make the same guarantee.
     *
     * @param index index of the element to set.
     * @param value the value to set this element to.
     */
    protected void pyset(int index, PyObject value) {
        throw Py.TypeError("can't assign to immutable object");
    }

    /**
     * Sets the given range of elements according to Python slice assignment semantics. If the step
     * size is one, it is a simple slice and the operation is equivalent to deleting that slice,
     * then inserting the value at that position, regarding the value as a sequence (if possible) or
     * as a single element if it is not a sequence. If the step size is not one, but
     * <code>start==stop</code>, it is equivalent to insertion at that point. If the step size is
     * not one, and <code>start!=stop</code>, the slice defines a certain number of elements to be
     * replaced, and the value must be a sequence of exactly that many elements (or convertible to
     * such a sequence).
     *
     * @param start the position of the first element.
     * @param stop one more than the position of the last element.
     * @param step the step size.
     * @param value an object consistent with the slice assignment
     */
    protected void setslice(int start, int stop, int step, PyObject value) {
        throw Py.TypeError(String.format("'%s' object does not support item assignment", getType()
                .fastGetName()));
    }

    /**
     * Deletes an element from the sequence (and closes up the gap).
     *
     * @param index index of the element to delete.
     */
    protected void del(int index) {
        delslice(index, index, 1, 1); // Raises TypeError (for immutable types).
    }

    /**
     * Deletes a contiguous sub-sequence (and closes up the gap).
     *
     * @param start the position of the first element.
     * @param stop one more than the position of the last element.
     */
    protected void delRange(int start, int stop) {
        delslice(start, stop, 1, Math.abs(stop - start)); // Raises TypeError (for immutable types).
    }

    /**
     * Deletes a simple or extended slice and closes up the gap(s). The slice parameters
     * <code>[start:stop:step]</code> mean what they would in Python, <i>after</i> application of
     * the "end-relative" rules for negative numbers and <code>None</code>. The count <code>n</code>
     * is as supplied by {@link PySlice#indicesEx(int)}. This method is unsafe in that slice
     * parameters are assumed correct.
     *
     * @param start the position of the first element.
     * @param stop beyond the position of the last element (not necessarily just beyond).
     * @param step from one element to the next (positive or negative)
     * @param n number of elements to delete
     */
    protected void delslice(int start, int stop, int step, int n) {
        // Raises TypeError (for immutable types).
        throw Py.TypeError(String.format("'%s' object does not support item deletion", getType()
                .fastGetName()));
    }

    @Override
    public boolean __nonzero__() {
        return seq___nonzero__();
    }

    final boolean seq___nonzero__() {
        return __len__() != 0;
    }

    @Override
    public PyObject __iter__() {
        return seq___iter__();
    }

    final PyObject seq___iter__() {
        return new PySequenceIter(this);
    }

    @Override
    public PyObject __eq__(PyObject o) {
        return seq___eq__(o);
    }

    final PyObject seq___eq__(PyObject o) {
        if (!isSubType(o) || o.getType() == PyObject.TYPE) {
            return null;
        }
        int tl = __len__();
        int ol = o.__len__();
        if (tl != ol) {
            return Py.False;
        }
        int i = cmp(this, tl, o, ol);
        return i < 0 ? Py.True : Py.False;
    }

    @Override
    public PyObject __ne__(PyObject o) {
        return seq___ne__(o);
    }

    final PyObject seq___ne__(PyObject o) {
        if (!isSubType(o) || o.getType() == PyObject.TYPE) {
            return null;
        }
        int tl = __len__();
        int ol = o.__len__();
        if (tl != ol) {
            return Py.True;
        }
        int i = cmp(this, tl, o, ol);
        return i < 0 ? Py.False : Py.True;
    }

    @Override
    public PyObject __lt__(PyObject o) {
        return seq___lt__(o);
    }

    final PyObject seq___lt__(PyObject o) {
        if (!isSubType(o) || o.getType() == PyObject.TYPE) {
            return null;
        }
        int i = cmp(this, -1, o, -1);
        if (i < 0) {
            return i == -1 ? Py.True : Py.False;
        }
        return __finditem__(i)._lt(o.__finditem__(i));
    }

    @Override
    public PyObject __le__(PyObject o) {
        return seq___le__(o);
    }

    final PyObject seq___le__(PyObject o) {
        if (!isSubType(o) || o.getType() == PyObject.TYPE) {
            return null;
        }
        int i = cmp(this, -1, o, -1);
        if (i < 0) {
            return i == -1 || i == -2 ? Py.True : Py.False;
        }
        return __finditem__(i)._le(o.__finditem__(i));
    }

    @Override
    public PyObject __gt__(PyObject o) {
        return seq___gt__(o);
    }

    final PyObject seq___gt__(PyObject o) {
        if (!isSubType(o) || o.getType() == PyObject.TYPE) {
            return null;
        }
        int i = cmp(this, -1, o, -1);
        if (i < 0) {
            return i == -3 ? Py.True : Py.False;
        }
        return __finditem__(i)._gt(o.__finditem__(i));
    }

    @Override
    public PyObject __ge__(PyObject o) {
        return seq___ge__(o);
    }

    final PyObject seq___ge__(PyObject o) {
        if (!isSubType(o) || o.getType() == PyObject.TYPE) {
            return null;
        }
        int i = cmp(this, -1, o, -1);
        if (i < 0) {
            return i == -3 || i == -2 ? Py.True : Py.False;
        }
        return __finditem__(i)._ge(o.__finditem__(i));
    }

    /**
     * isSubType tailored for PySequence binops.
     *
     * @param other PyObject
     * @return true if subclass of other
     */
    protected boolean isSubType(PyObject other) {
        PyType type = getType();
        PyType otherType = other.getType();
        return type == otherType || type.isSubType(otherType);
    }

    /**
     * Compare the specified object/length pairs.
     *
     * @return value &ge; 0 is the index where the sequences differs. -1: reached the end of o1
     *         without a difference -2: reached the end of both sequences without a difference -3:
     *         reached the end of o2 without a difference
     */
    protected static int cmp(PyObject o1, int ol1, PyObject o2, int ol2) {
        if (ol1 < 0) {
            ol1 = o1.__len__();
        }
        if (ol2 < 0) {
            ol2 = o2.__len__();
        }
        for (int i = 0; i < ol1 && i < ol2; i++) {
            if (!o1.__getitem__(i).equals(o2.__getitem__(i))) {
                return i;
            }
        }
        if (ol1 == ol2) {
            return -2;
        }
        return ol1 < ol2 ? -1 : -3;
    }

    /**
     * Return a copy of a sequence where the __len__() method is telling the truth.
     */
    protected static PySequence fastSequence(PyObject seq, String msg) {
        if (seq instanceof PySequence) {
            return (PySequence)seq;
        }
        PyList list = new PyList();
        PyObject iter = Py.iter(seq, msg);
        for (PyObject item = null; (item = iter.__iternext__()) != null;) {
            list.append(item);
        }
        return list;
    }

    /**
     * Make step a long in case adding the start, stop and step together overflows an int.
     */
    protected static final int sliceLength(int start, int stop, long step) {
        int ret;
        if (step > 0) {
            ret = (int)((stop - start + step - 1) / step);
        } else {
            ret = (int)((stop - start + step + 1) / step);
        }
        if (ret < 0) {
            return 0;
        }
        return ret;
    }

    /**
     * Adjusts <code>index</code> such that it's &ge;0 and &le; __len__. If <code>index</code>
     * starts off negative, it's treated as an index from the end of the sequence going back to the
     * start.
     */
    protected int boundToSequence(int index) {
        int length = __len__();
        if (index < 0) {
            index += length;
            if (index < 0) {
                index = 0;
            }
        } else if (index > length) {
            index = length;
        }
        return index;
    }

    @Override
    public PyObject __finditem__(int index) {
        return seq___finditem__(index);
    }

    final PyObject seq___finditem__(int index) {
        return delegator.checkIdxAndFindItem(index);
    }

    @Override
    public PyObject __finditem__(PyObject index) {
        return seq___finditem__(index);
    }

    final PyObject seq___finditem__(PyObject index) {
        return delegator.checkIdxAndFindItem(index);
    }

    @Override
    public PyObject __getitem__(PyObject index) {
        return seq___getitem__(index);
    }

    final PyObject seq___getitem__(PyObject index) {
        return delegator.checkIdxAndGetItem(index);
    }

    @Override
    public boolean isMappingType() throws PyIgnoreMethodTag {
        return false;
    }

    @Override
    public boolean isNumberType() throws PyIgnoreMethodTag {
        return false;
    }

    @Override
    public PyObject __getslice__(PyObject start, PyObject stop, PyObject step) {
        return seq___getslice__(start, stop, step);
    }

    final PyObject seq___getslice__(PyObject start, PyObject stop, PyObject step) {
        return delegator.getSlice(new PySlice(start, stop, step));
    }

    @Override
    public void __setslice__(PyObject start, PyObject stop, PyObject step, PyObject value) {
        seq___setslice__(start, stop, step, value);
    }

    final void seq___setslice__(PyObject start, PyObject stop, PyObject step, PyObject value) {
        if (value == null) {
            value = step;
            step = null;
        }
        delegator.checkIdxAndSetSlice(new PySlice(start, stop, step), value);
    }

    @Override
    public void __delslice__(PyObject start, PyObject stop, PyObject step) {
        seq___delslice__(start, stop, step);
    }

    final void seq___delslice__(PyObject start, PyObject stop, PyObject step) {
        delegator.checkIdxAndDelItem(new PySlice(start, stop, step));
    }

    @Override
    public void __setitem__(int index, PyObject value) {
        delegator.checkIdxAndSetItem(index, value);
    }

    @Override
    public void __setitem__(PyObject index, PyObject value) {
        seq___setitem__(index, value);
    }

    final void seq___setitem__(PyObject index, PyObject value) {
        delegator.checkIdxAndSetItem(index, value);
    }

    @Override
    public void __delitem__(PyObject index) {
        seq___delitem__(index);
    }

    final void seq___delitem__(PyObject index) {
        delegator.checkIdxAndDelItem(index);
    }

    @Override
    public synchronized Object __tojava__(Class<?> c) throws PyIgnoreMethodTag {
        if (c.isArray()) {
            Class<?> component = c.getComponentType();
            try {
                int n = __len__();
                PyArray array = new PyArray(component, n);
                for (int i = 0; i < n; i++) {
                    PyObject o = pyget(i);
                    array.set(i, o);
                }
                return array.getArray();
            } catch (Throwable t) {
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
    @Override
    protected String unsupportedopMessage(String op, PyObject o2) {
        if (op.equals("*")) {
            return "can''t multiply sequence by non-int of type ''{2}''";
        }
        return null;
    }

    /**
     * Return sequence-specific error messages suitable for substitution.
     *
     * {0} is the op name. {1} is the left operand type. {2} is the right operand type.
     */
    @Override
    protected String runsupportedopMessage(String op, PyObject o2) {
        if (op.equals("*")) {
            return "can''t multiply sequence by non-int of type ''{1}''";
        }
        return null;
    }

    @Override
    public boolean isSequenceType() {
        return true;
    }

    /**
     * Class defining the default behaviour of sequences with respect to slice assignment, etc.,
     * which is the one correct for <code>list</code>.
     */
    protected class DefaultIndexDelegate extends SequenceIndexDelegate {

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
