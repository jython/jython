// Copyright © Corporation for National Research Initiatives
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
        
    public PyObject __call__(PyObject[] args, String[] keywords) {
        if (im_self instanceof PyClass)
            // unbound method
            // TBD: verify that first arg is class instance?
            return im_func.__call__(args, keywords);
        else
            // bound method
            return im_func.__call__(im_self, args, keywords);
    }

    public int __cmp__(PyObject other) {
        if (other instanceof PyMethod) {
            PyMethod mother = (PyMethod)other;
            if (im_self == mother.im_self &&
                im_func == mother.im_func)
            {
                return 0;
            }
        }
        return -1;
    }

    public String toString() {
        if (im_self instanceof PyClass)
            // this is an unbound method
            return "<unbound method " + ((PyClass)im_self).__name__ + "." +
                __name__ + ">";
        else {
            // this is a bound method
            String classname = im_self.__class__.__name__;
            return "<method " + classname + "." +
                __name__ + " of " + classname + " instance at " +
                Py.id(im_func) + ">";
        }
    }
}
