// Copyright © Corporation for National Research Initiatives
package org.python.core;

public class PyMethod extends PyObject {
    public PyObject im_self;
    public PyObject im_func;
    public PyObject im_class;
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
        
    public void _setClass(PyObject myclass) {
        im_class = myclass;
    }

    /*private final boolean isBound() {
      return im_self != null && !(im_self instanceof PyClass);
      }*/
        
    public PyObject __call__(PyObject[] args, String[] keywords) {
        if (im_self != null)
            // bound method
            return im_func.__call__(im_self, args, keywords);
        // unbound method.  TBD: Verify that the first argument is a class
        // instance.
        System.err.println("unbound check");
        return im_func.__call__(args, keywords);
    }

    public int __cmp__(PyObject other) {
        if (other instanceof PyMethod) {
            PyMethod mother = (PyMethod)other;
            if (im_self != mother.im_self)
                return Py.id(im_self) < Py.id(mother.im_self) ? -1 : 1;
            if (im_func != mother.im_func)
                return Py.id(im_func) < Py.id(mother.im_func) ? -1 : 1;
            return 0;
        }
        return -2;
    }

    public String toString() {
        String classname = "?";
        if (im_class != null && im_class instanceof PyClass)
            classname = ((PyClass)im_class).__name__;
        if (im_self == null)
            // this is an unbound method
            return "<unbound method " + classname + "." + __name__ + ">";
        else
            return "<method " + classname + "." +
                __name__ + " of " + classname + " instance at " +
                Py.id(im_func) + ">";
    }
}
