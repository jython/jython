package org.python.core;

/**
The abstract superclass of PyObjects that implements a Sequence.
Minimize the work in creating such objects.

Method names are designed to make it possible for PySequence
to implement java.util.List interface when JDK 1.2 is ubiquitous.

All subclasses must implements get, getslice, and repeat methods.

Subclasses that are mutable should also implement:
set, setslice, del, and delRange.
**/

abstract public class PySequence extends PyObject {
    /**
    This constructor is used to pass on an __class__ attribute.
    **/
    public PySequence(PyClass c) { super(c); }
    public PySequence() { ; }

	/*These methods must be defined for any sequence*/
	
	/**
	@param index index of element to return.
	@return the element at the given position in the list.
	**/
	abstract protected PyObject get(int index);
	
	/**
	Returns a range of elements from the sequence.
	
	@param start the position of the first element.
	@param stop one more than the position of the last element.
	@param step the step size.
	@return a sequence corresponding the the given range of elements.
	**/
	abstract protected PyObject getslice(int start, int stop, int step);
	
	/**
	Repeats the given sequence.
	
	@param count the number of times to repeat the sequence.
	@return this sequence repeated count times.
	**/
	abstract protected PyObject repeat(int count);

	/* These methods only apply to writeable sequences */
	/**
	Sets the given element of the sequence.
	
	@param index index of the element to set.
	@param value the value to set this element to.
	**/
	protected void set(int index, PyObject value) {
		throw Py.TypeError("can't assign to immutable object");
	}
	
	/**
	Sets the given range of elements.
	**/
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
		return __len__() != 0;
	}

	public int __cmp__(PyObject ob_other) {
		if (ob_other.__class__ != __class__) return -2;
		
		PySequence other = (PySequence)ob_other;
		int s1 = __len__();
		int s2 = other.__len__();
		int len = (s1 > s2) ? s2 : s1;

		for(int i=0; i<len; i++) {
			int c = get(i)._cmp(other.get(i));
			if (c != 0) return c;
		}
		return s1 < s2 ? -1 : (s1 > s2 ? 1: 0);
	}

    protected static final int sliceLength(int start, int stop, int step) {
        if (step > 0) {
            return (stop-start+step-1)/step;
        } else {
		    return (stop-start+step+1)/step;
		}
	}

    private static final int getIndex(PyObject index, int defaultValue) {
        if (index == Py.None) return defaultValue;
        if (!(index instanceof PyInteger)) throw Py.TypeError("slice index must be int");
        return ((PyInteger)index).getValue();
    }

	private static final int[] slice_indices(PySlice s, int length) {
		int start, stop, step;

        step = getIndex(s.step, 1);
        if (step == 0) throw Py.TypeError("slice step of zero not allowed");
        
        if (step > 0) {
            start = getIndex(s.start, 0);
            stop = getIndex(s.stop, length);
        } else {
            start = getIndex(s.stop, 0);
            stop = getIndex(s.start, length);
        }
		
		if (start < 0) {
		    start = length+start;
		    if (start < 0) start = 0;
		} else if (start > length) {
		    start = length;
		}
		if (stop < 0) {
		    stop = length+stop;
		    if (stop < 0) stop = 0;
		} else if (stop > length) {
		    stop = length;
		}
		
		if (stop < start) stop = start;
		
		if (step > 0) {
		    return new int[] {start, stop, step};
		} else {
		    return new int[] {stop, start, step};
		}
	}

	protected int fixindex(int index) {
		int l = __len__();
		if (index < 0) index += l;
		if (index < 0 || index >= l) return -1;
		//throw Py.IndexError("index out of range");
		else return index;
	}


	public PyObject __finditem__(int index) {
	    index = fixindex(index);
	    if (index == -1) return null;
	    else return get(index);
	}

	public PyObject __finditem__(PyObject index) {
		if (index instanceof PyInteger) {
			return __finditem__(((PyInteger)index).getValue());
		} else {
			if (index instanceof PySlice) {
				int[] s = slice_indices((PySlice)index, __len__());
				return getslice(s[0], s[1], s[2]);
			} else {
				throw Py.TypeError("sequence subscript must be integer or slice");
			}
		}
	}

	public PyObject __getitem__(PyObject index) {
	    PyObject ret = __finditem__(index);
	    if (ret == null) {
	        throw Py.IndexError("index out of range: "+index);
	    }
	    return ret;
	}

	public void __setitem__(int index, PyObject value) {
		int i = fixindex(index);
		if (i == -1) throw Py.IndexError("index out of range: "+i);
	    set(i, value);
	}

	public void __setitem__(PyObject index, PyObject value) {
		if (index instanceof PyInteger) {
		    __setitem__(((PyInteger)index).getValue(), value);

		} else {
			if (index instanceof PySlice) {
				int[] s = slice_indices((PySlice)index, __len__());
				setslice(s[0], s[1], s[2], value);
			} else {
				throw Py.TypeError("sequence subscript must be integer or slice");
			}
		}
	}

	public void __delitem__(PyObject index) {
		if (index instanceof PyInteger) {
		    int i = fixindex(((PyInteger)index).getValue());
		    if (i == -1) throw Py.IndexError("index out of range: "+i);
			del(i);
		} else {
			if (index instanceof PySlice) {
				int[] s = slice_indices((PySlice)index, __len__());
				delRange(s[0], s[1], s[2]);
			} else {
				throw Py.TypeError("sequence subscript must be integer or slice");
			}
		}
	}

	public PyObject __mul__(PyObject count) {
	    int repeats = Py.py2int(count, "can't multiply sequence with non-int");
	    return repeat(repeats >= 0 ? repeats : 0);
	}

	public PyObject __rmul__(PyObject count) {
		return __mul__(count);
	}

	public Object __tojava__(Class c) {
		if (c.isArray()) {
			Class component = c.getComponentType();
			//System.out.println("getting: "+component);
			try {
				int n = __len__();
				PyArray array = new PyArray(component, n);
				for(int i=0; i<n; i++) {
					PyObject o = get(i);
					array.set(i, o);
				}
		        //System.out.println("getting: "+component+", "+array.data);
				return array.data;
			} catch (Throwable t) {
				;//System.out.println("failed to get: "+component.getName());
			}
		}
		return super.__tojava__(c);
	}

}
