// Copyright (c) Corporation for National Research Initiatives
package org.python.core;

/**
 * A python method.
 */

public class PyMethod extends PyObject
{
    public PyObject im_self;
    public PyObject im_func;
    public PyObject im_class;

    public PyMethod(PyObject self, PyObject f, PyObject wherefound) {
        if(self == Py.None){
            self = null;
        }
        im_func = f;
        im_self = self;
        im_class = wherefound;
    }

    public PyMethod(PyObject self, PyFunction f, PyObject wherefound) {
        this(self, (PyObject)f, wherefound);
    }

    public PyMethod(PyObject self, PyReflectedFunction f, PyObject wherefound)
    {
        this(self, (PyObject)f, wherefound);
    }

    private static final String[] __members__ = {
        "im_self", "im_func", "im_class",
        "__doc__", "__dict__",
    };

    // TBD: this should be unnecessary
    public PyObject __dir__() {
        PyString members[] = new PyString[__members__.length];
        for (int i = 0; i < __members__.length; i++)
            members[i] = new PyString(__members__[i]);
        PyList ret = new PyList(members);
        PyObject k = im_func.__getattr__("__dict__").invoke("keys");
        ret.extend(k);
        return ret;
    }

    private void throwReadonly(String name) {
        for (int i = 0; i < __members__.length; i++)
            if (__members__[i] == name)
                throw Py.TypeError("readonly attribute");
        throw Py.AttributeError(name);
    }

    public PyObject __findattr__(String name) {
        PyObject ret = super.__findattr__(name);
        if (ret != null) {
            return ret;
        }
        return im_func.__findattr__(name);
    }

    public void __delattr__(String name) {
        if (name == "__doc__") {
            throwReadonly(name);
        }
        im_func.__delattr__(name);
    }

    public PyObject getDoc() {
        return im_func.getDoc();
    }

    public PyObject _doget(PyObject container) {
        return _doget(container, null);
    }

    public PyObject _doget(PyObject container, PyObject wherefound) {
        /* Only if classes are compatible */
        if(container == null || im_self != null) {
            return this;
        } else if(__builtin__.issubclass(container.fastGetClass(), im_class)) {
           if(im_func instanceof PyFunction) {
                return new PyMethod(container, (PyFunction)im_func, im_class);
            } else if(im_func instanceof PyReflectedFunction) {
                return new PyMethod(container,
                                    (PyReflectedFunction)im_func,
                                    im_class);
            } else {
                return new PyMethod(container, im_func, im_class);
            }
        } else {
            return this;
        }
    }


    public PyObject __call__(PyObject[] args, String[] keywords) {
        if (im_self != null)
            // bound method
            return im_func.__call__(im_self, args, keywords);
        // unbound method.
        boolean badcall = false;
        if (im_class == null)
            // TBD: An example of this is running any function defined in
            // the os module.  If you "import os", you'll find it's a
            // jclass object instead of a module object.  Still unclear
            // whether that's wrong, but it's definitely not easily fixed
            // right now.  Running, e.g. os.getcwd() creates an unbound
            // method with im_class == null.  For backwards compatibility,
            // let this pass the call test
            ;
        else if (args.length < 1)
            badcall = true;
        else // xxx can be faster?
            // first argument must be an instance who's class is im_class
            // or a subclass of im_class
            badcall = ! __builtin__.issubclass(args[0].fastGetClass(),im_class);
        if (badcall) {
            String got ="nothing";
            if (args.length>=1)
                got = getClassName(args[0].fastGetClass())+" instance";
            throw Py.TypeError("unbound method " + getFuncName() + "() must be " +
                               "called with "+getClassName(im_class)+ " instance as first argument"+
                               " (got "+got+" instead)");
        }
        else
            return im_func.__call__(args, keywords);
    }

    public int __cmp__(PyObject other) {
        if (other instanceof PyMethod) {
            PyMethod mother = (PyMethod)other;
            if (im_self != mother.im_self)
                return System.identityHashCode(im_self) < 
                       System.identityHashCode(mother.im_self) ? -1 : 1;
            if (im_func != mother.im_func)
                return System.identityHashCode(im_func) < 
                       System.identityHashCode(mother.im_func) ? -1 : 1;
            return 0;
        }
        return -2;
    }

    private String getClassName(PyObject cls) {
        if (cls instanceof PyClass)
           return ((PyClass)cls).__name__;
        if (cls instanceof PyType)
            return ((PyType)cls).fastGetName();
        return "?";
    }

    private String getFuncName() {
        PyObject funcName = null;
        try {
            funcName = im_func.__findattr__("__name__");
        } catch (PyException pye) {
            // continue
        }
        if (funcName == null) {
            return "?";
        }
        return funcName.toString();
    }

    public String toString() {
        String className = "?";
        if (im_class != null) {
            className = getClassName(im_class);
        }
        if (im_self == null) {
            return String.format("<unbound method %s.%s>", className, getFuncName());
        } else {
            return String.format("<bound method %s.%s of %s>", className, getFuncName(),
                                 im_self.__str__());
        }
    }
}
