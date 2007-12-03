// Copyright (c) Corporation for National Research Initiatives
package org.python.core;

/**
 * The abstract superclass of PyObjects that implements a Sequence.
 * Minimize the work in creating such objects.
 *
 * Method names are designed to make it possible for PySequence to
 * implement java.util.List interface when JDK 1.2 is ubiquitous.
 *
 * Subclasses must also implement get, getslice, and repeat methods.
 *
 * Subclasses that are mutable should also implement: set, setslice, del,
 * and delRange.
 */
public abstract class PySequence extends PyObject {
    /**
     * This constructor is used by PyJavaClass.init()
     */
    public PySequence() {}

    protected PySequence(PyType type) {
        super(type);
    }

    // These methods must be defined for any sequence

    /**
     * @param index index of element to return.
     * @return the element at the given position in the list.
     */
    abstract protected PyObject pyget(int index);

    /**
     * Returns a range of elements from the sequence.
     *
     * @param start the position of the first element.
     * @param stop one more than the position of the last element.
     * @param step the step size.
     * @return a sequence corresponding the the given range of elements.
     */
    abstract protected PyObject getslice(int start, int stop, int step);

    /**
     * Repeats the given sequence.
     *
     * @param count the number of times to repeat the sequence.
     * @return this sequence repeated count times.
     */
    abstract protected PyObject repeat(int count);

    // These methods only apply to mutable sequences

    /**
     * Sets the given element of the sequence.
     *
     * @param index index of the element to set.
     * @param value the value to set this element to.
     */
    protected void set(int index, PyObject value) {
        throw Py.TypeError("can't assign to immutable object");
    }

    /**
     * Sets the given range of elements.
     */
    protected void setslice(int start, int stop, int step, PyObject value) {
        throw Py.TypeError("can't assign to immutable object");
    }

    protected void del(int i) throws PyException {
        throw Py.TypeError("can't remove from immutable object");
    }
    protected void delRange(int start, int stop, int step) {
        throw Py.TypeError("can't remove from immutable object");
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
        if (!(getType() == o.getType()) && !(getType().isSubType(o.getType()))) {
            return null;
        }
        int tl = __len__();
        int ol = o.__len__();
        if (tl != ol)
            return Py.Zero;
        int i = cmp(this, tl, o, ol);
        return (i < 0) ? Py.One : Py.Zero;
    }

    public synchronized PyObject __ne__(PyObject o) {
        return seq___ne__(o);
    }

    final synchronized PyObject seq___ne__(PyObject o) {
        if (!(getType() == o.getType()) && !(getType().isSubType(o.getType()))) {
            return null;
        }
        int tl = __len__();
        int ol = o.__len__();
        if (tl != ol)
            return Py.One;
        int i = cmp(this, tl, o, ol);
        return (i < 0) ? Py.Zero : Py.One;
    }

    public synchronized PyObject __lt__(PyObject o) {
        if (!(getType() == o.getType()) && !(getType().isSubType(o.getType()))) {
            return null;
        }
        int i = cmp(this, -1, o, -1);
        if (i < 0)
            return (i == -1) ? Py.One : Py.Zero;
        return __finditem__(i)._lt(o.__finditem__(i));
    }
    
    final synchronized PyObject seq___lt__(PyObject o){
    	return __lt__(o);
    }

    public synchronized PyObject __le__(PyObject o) {
        if (!(getType() == o.getType()) && !(getType().isSubType(o.getType()))) {
            return null;
        }
        int i = cmp(this, -1, o, -1);
        if (i < 0)
            return (i == -1 || i == -2) ? Py.One : Py.Zero;
        return __finditem__(i)._le(o.__finditem__(i));
    }
    
    final synchronized PyObject seq___le__(PyObject o){
    	return __le__(o);
    }

    public synchronized PyObject __gt__(PyObject o) {
        if (!(getType() == o.getType()) && !(getType().isSubType(o.getType()))) {
            return null;
        }
        int i = cmp(this, -1, o, -1);
        if (i < 0)
            return (i == -3) ? Py.One : Py.Zero;
        return __finditem__(i)._gt(o.__finditem__(i));
    }
    
    final synchronized PyObject seq___gt__(PyObject o){
    	return __gt__(o);
    }

    public synchronized PyObject __ge__(PyObject o) {
        if (!(getType() == o.getType()) && !(getType().isSubType(o.getType()))) {
            return null;
        }
        int i = cmp(this, -1, o, -1);
        if (i < 0)
            return (i == -3 || i == -2) ? Py.One : Py.Zero;
        return __finditem__(i)._ge(o.__finditem__(i));
    }
    
    final synchronized PyObject seq___ge__(PyObject o){
    	return __ge__(o);
    }

    // Return value >= 0 is the index where the sequences differs.
    // -1: reached the end of o1 without a difference
    // -2: reached the end of both seqeunces without a difference
    // -3: reached the end of o2 without a difference
    protected static int cmp(PyObject o1, int ol1, PyObject o2, int ol2) {
        if (ol1 < 0)
            ol1 = o1.__len__();
        if (ol2 < 0)
            ol2 = o2.__len__();
        int i = 0;
        for ( ; i < ol1 && i < ol2; i++) {
            if (!o1.__getitem__(i)._eq(o2.__getitem__(i)).__nonzero__())
                return i;
        }
        if (ol1 == ol2)
            return -2;
        return (ol1 < ol2) ? -1 : -3;
    }


    // Return a copy of a sequence where the __len__() method is
    // telling the thruth.
    protected static PyObject fastSequence(PyObject seq, String msg) {
        if (seq instanceof PyList || seq instanceof PyTuple)
            return seq;

        PyList list = new PyList();
        PyObject iter = Py.iter(seq, msg);
        for (PyObject item = null; (item = iter.__iternext__()) != null; ) {
            list.append(item);
        }
        return list;
    }

    protected static final int sliceLength(int start, int stop, int step) {
        //System.err.println("slice: "+start+", "+stop+", "+step);
        int ret;
        if (step > 0) {
            ret = (stop-start+step-1)/step;
        } else {
            ret = (stop-start+step+1)/step;
        }
        if (ret < 0) return 0;
        return ret;
    }

    private static final int getIndex(PyObject index, int defaultValue) {
        if (index == Py.None || index == null)
            return defaultValue;
        if (index instanceof PyLong) {
            try {
                index = (index.__int__());
            } catch (PyException exc) {
                if (Py.matchException(exc, Py.OverflowError)) {
                    if (new PyLong(0L).__cmp__(index) < 0)
                        return Integer.MAX_VALUE;
                    else
                        return 0;
                }
            }
        }
        if (!(index instanceof PyInteger))
            throw Py.TypeError("slice index must be int");
        return ((PyInteger)index).getValue();
    }

    protected int fixindex(int index) {
        int l = __len__();
        if (index < 0)
            index += l;
        if (index < 0 || index >= l)
            return -1;
        //throw Py.IndexError("index out of range");
        else return index;
    }

    public synchronized PyObject __finditem__(int index) {
        index = fixindex(index);
        if (index == -1)
            return null;
        else
            return pyget(index);
    }

    public PyObject __finditem__(PyObject index) {
        return seq___finditem__(index);
    }

    final PyObject seq___finditem__(PyObject index) {
        if (index instanceof PyInteger)
            return __finditem__(((PyInteger)index).getValue());
        else if (index instanceof PySlice) {
            PySlice s = (PySlice)index;
            return __getslice__(s.start, s.stop, s.step);
        } else if (index instanceof PyLong)
            return __finditem__(((PyInteger)index.__int__()).getValue());
        else
            throw Py.TypeError("sequence subscript must be integer or slice");
    }

    public PyObject __getitem__(PyObject index) {
        return seq___getitem__(index);
    }

    final PyObject seq___getitem__(PyObject index) {
        PyObject ret = __finditem__(index);
        if (ret == null) {
            throw Py.IndexError("index out of range: "+index);
        }
        return ret;
    }

    public boolean isMappingType() throws PyIgnoreMethodTag { return false; }
    public boolean isNumberType() throws PyIgnoreMethodTag { return false; }

    protected static final int getStep(PyObject s_step) {
        int step = getIndex(s_step, 1);
        if (step == 0) {
            throw Py.ValueError("slice step cannot be zero");
        }
        return step;
    }

    protected static final int getStart(PyObject s_start, int step,
                                        int length)
    {
        int start;
        if (step < 0) {
            start = getIndex(s_start, length - 1);
            if (start < 0) start += length;
            if (start < 0) start = -1;
            if (start >= length) start = length - 1;
        } else {
            start = getIndex(s_start, 0);
            if (start < 0) start += length;
            if (start < 0) start = 0;
            if (start >= length) start = length;
        }

        return start;
    }

    protected static final int getStop(PyObject s_stop, int start, int step,
                                       int length)
    {
        int stop;
        if (step < 0) {
            stop = getIndex(s_stop, -1);
            if (stop < -1) stop = length+stop;
            if (stop < -1) stop = -1;
        } else {
            stop = getIndex(s_stop, length);
            if (stop < 0) stop = length+stop;
            if (stop < 0) stop = 0;
        }
        if (stop > length) stop = length;

        return stop;
    }

    public synchronized PyObject __getslice__(PyObject s_start,
                                              PyObject s_stop,
                                              PyObject s_step) {
        return seq___getslice__(s_start,s_stop,s_step);
    }

    final synchronized PyObject seq___getslice__(PyObject s_start,
                                              PyObject s_stop)
    {
        return seq___getslice__(s_start,s_stop,null);
    }

    final synchronized PyObject seq___getslice__(PyObject s_start,
                                              PyObject s_stop,
                                              PyObject s_step)
    {
        int length = __len__();
        int step = getStep(s_step);
        int start = getStart(s_start, step, length);
        int stop = getStop(s_stop, start, step, length);
        return getslice(start, stop, step);
    }

    public synchronized void __setslice__(PyObject s_start, PyObject s_stop,
                                          PyObject s_step, PyObject value) {
        seq___setslice__(s_start,s_stop,s_step,value);
    }
    final synchronized void seq___setslice__(PyObject s_start, PyObject s_stop,
                                          PyObject value)
    {
        seq___setslice__(s_start,s_stop,null,value);
    }
    final synchronized void seq___setslice__(PyObject s_start, PyObject s_stop,
                                          PyObject s_step, PyObject value)
    {
        int length = __len__();
        int step = getStep(s_step);
        int start = getStart(s_start, step, length);
        int stop = getStop(s_stop, start, step, length);
        setslice(start, stop, step, value);
    }

    public synchronized void __delslice__(PyObject s_start, PyObject s_stop,
                                          PyObject s_step) {
        seq___delslice__(s_start,s_stop,s_step);
    }

    final synchronized void seq___delslice__(PyObject s_start, PyObject s_stop,
                                          PyObject s_step)
    {
        int length = __len__();
        int step = getStep(s_step);
        int start = getStart(s_start, step, length);
        int stop = getStop(s_stop, start, step, length);
        delRange(start, stop, step);
    }

    public synchronized void __setitem__(int index, PyObject value) {
        int i = fixindex(index);
        if (i == -1)
            throw Py.IndexError("index out of range: "+i);
        set(i, value);
    }

    public void __setitem__(PyObject index, PyObject value) {
        seq___setitem__(index,value);
    }

    final void seq___setitem__(PyObject index, PyObject value) {
        if (index instanceof PyInteger)
            __setitem__(((PyInteger)index).getValue(), value);
        else {
            if (index instanceof PySlice) {
                PySlice s = (PySlice)index;
                __setslice__(s.start, s.stop, s.step, value);
            } else if (index instanceof PyLong) {
                __setitem__(((PyInteger)index.__int__()).getValue(), value);
            } else {
                throw Py.TypeError(
                    "sequence subscript must be integer or slice");
            }
        }
    }

    public synchronized void __delitem__(PyObject index) {
        seq___delitem__(index);
    }

    final synchronized void seq___delitem__(PyObject index) {
        if (index instanceof PyInteger) {
            int i = fixindex(((PyInteger)index).getValue());
            if (i == -1)
                throw Py.IndexError("index out of range: "+i);
            del(i);
        }
        else {
            if (index instanceof PySlice) {
                PySlice s = (PySlice)index;
                __delslice__(s.start, s.stop, s.step);
            } else if (index instanceof PyLong) {
                int i = fixindex(((PyInteger)index.__int__()).getValue());
                if (i == -1)
                    throw Py.IndexError("index out of range: "+i);
                del(i);
            } else {
                throw Py.TypeError(
                    "sequence subscript must be integer or slice");
            }
        }
    }

    public synchronized Object __tojava__(Class c) throws PyIgnoreMethodTag {
        if (c.isArray()) {
            Class component = c.getComponentType();
            //System.out.println("getting: "+component);
            try {
                int n = __len__();
                PyArray array = new PyArray(component, n);
                for (int i=0; i<n; i++) {
                    PyObject o = pyget(i);
                    array.set(i, o);
                }
                //System.out.println("getting: "+component+", "+array.data);
                return array.getArray();
            } catch (Throwable t) {
                ;//System.out.println("failed to get: "+component.getName());
            }
        }
        return super.__tojava__(c);
    }

    /**
     * Return sequence-specific error messages suitable for substitution.
     *
     * {0} is the op name.
     * {1} is the left operand type.
     * {2} is the right operand type.
     */
    protected String unsupportedopMessage(String op, PyObject o2) {
        if (op.equals("*")) {
            return "can''t multiply sequence by non-int of type ''{2}''";
        }
        return null;
    }
    
    /**
     * Return sequence-specific error messages suitable for substitution.
     *
     * {0} is the op name.
     * {1} is the left operand type.
     * {2} is the right operand type.
     */
    protected String runsupportedopMessage(String op, PyObject o2) {
        if (op.equals("*")) {
            return "can''t multiply sequence by non-int of type ''{1}''";
        }
        return null;
    }
}
