package org.python.core;

public class PyMethod extends PyObject {
	public PyObject im_self;
	public PyObject im_func;
	public String __name__;
	public PyObject __doc__;

    public static PyClass __class__;
	public PyMethod(PyObject self, PyObject f) {
	    super(__class__);
		im_func = f;
		im_self = self;
	}
	
	public PyMethod(PyObject self, PyFunction f) {
	    this(self, (PyObject)f);
		__name__ = f.__name__;
		__doc__ = f.__doc__;
	}
	
	public PyMethod(PyObject self, PyReflectedFunction f) {
	    this(self, (PyObject)f);
		__name__ = f.__name__;
		__doc__ = f.__doc__;
	}
	
	/*private final boolean isBound() {
	    return im_self != null && !(im_self instanceof PyClass);
	}*/
	
	public PyObject __call__(PyObject arg1) {
	    if (im_self != null) {
	        return im_func.__call__(im_self, arg1);
	    } else {
	        return im_func.__call__(arg1);
	    }
	}
	
	public PyObject __call__(PyObject[] args, String[] keywords) {
		if (im_self != null) {
		    return im_func.__call__(im_self, args, keywords);
		} else {
		    // Verify that first arg is class instance?
			return im_func.__call__(args, keywords);
		}
	}

	public String toString() {
		if (im_self != null) {
			return "<method "+__name__+" at "+Py.id(this)+">";
		} else {
			return "<unbound method "+__name__+" at "+Py.id(this)+">";
		}
	}
}