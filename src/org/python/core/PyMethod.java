// Copyright (c) Corporation for National Research Initiatives
package org.python.core;

import org.python.expose.ExposedGet;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedType;
import org.python.expose.MethodType;

/**
 * A Python method.
 */
@ExposedType(name = "instancemethod")
public class PyMethod extends PyObject {

    public static final PyType TYPE = PyType.fromClass(PyMethod.class);

    /** The class associated with a method. */
    @ExposedGet
    public PyObject im_class;

    /** The function (or other callable) implementing a method. */
    @ExposedGet
    public PyObject im_func;

    /** The instance to which a method is bound; None for unbound methods. */
    @ExposedGet
    public PyObject im_self;

    public PyMethod(PyObject function, PyObject self, PyObject type) {
        if (self == Py.None){
            self = null;
        }
        im_func = function;
        im_self = self;
        im_class = type;
    }

    @ExposedNew
    static final PyObject instancemethod___new__(PyNewWrapper new_, boolean init, PyType subtype,
                                                PyObject[] args, String[] keywords) {
        ArgParser ap = new ArgParser("instancemethod", args, keywords, "func");
        ap.noKeywords();
        PyObject func = ap.getPyObject(0);
        PyObject self = ap.getPyObject(1);
        PyObject klass = ap.getPyObject(2);

        if (!__builtin__.callable(func)) {
            throw Py.TypeError("first argument must be callable");
        }
        return new PyMethod(func, self, klass);
    }

    @Override
    public PyObject __findattr__(String name) {
        PyObject ret = super.__findattr__(name);
        if (ret != null) {
            return ret;
        }
        return im_func.__findattr__(name);
    }

    @Override
    public PyObject __get__(PyObject obj, PyObject type) {
        return instancemethod___get__(obj, type);
    }

    @ExposedMethod(defaults = "null")
    final PyObject instancemethod___get__(PyObject obj, PyObject type) {
        // Only if classes are compatible
        if (obj == null || im_self != null) {
            return this;
        } else if (Py.isSubClass(obj.fastGetClass(), im_class)) {
            return new PyMethod(im_func, obj, im_class);
        } else {
            return this;
        }
    }

    @Override
    public PyObject __call__(PyObject[] args, String[] keywords) {
        return instancemethod___call__(args, keywords);
    }

    @ExposedMethod
    final PyObject instancemethod___call__(PyObject[] args, String[] keywords) {
        PyObject self = im_self;

        if (self == null) {
            // Unbound methods must be called with an instance of the
            // class (or a derived class) as first argument
            boolean ok;
            if (args.length >= 1) {
                self = args[0];
            }
            if (self == null) {
                ok = false;
            } else {
                ok = Py.isInstance(self, im_class);
            }
            if (!ok) {
                // XXX: Need equiv. of PyEval_GetFuncDesc instead of
                // hardcoding "()"
                String msg = String.format("unbound method %s%s must be called with %s instance as "
                                           + "first argument (got %s%s instead)",
                                           getFuncName(), "()",
                                           getClassName(im_class), getInstClassName(self),
                                           self == null ? "" : " instance");
                throw Py.TypeError(msg);
            }
            return im_func.__call__(args, keywords);
        } else {
            return im_func.__call__(self, args, keywords);
        }
    }

    @Override
    public int __cmp__(PyObject other) {
        return instancemethod___cmp__(other);
    }

    @ExposedMethod(type = MethodType.CMP)
    final int instancemethod___cmp__(PyObject other) {
        if (!(other instanceof PyMethod)) {
            return -2;
        }
        PyMethod mother = (PyMethod)other;
        if (im_self != mother.im_self) {
            return System.identityHashCode(im_self) <
                    System.identityHashCode(mother.im_self) ? -1 : 1;
        }
        if (im_func != mother.im_func) {
            return System.identityHashCode(im_func) <
                    System.identityHashCode(mother.im_func) ? -1 : 1;
        }
        return 0;
    }

    @Override
    public PyObject getDoc() {
        return im_func.getDoc();
    }

    @Override
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

    private String getClassName(PyObject cls) {
        if (cls instanceof PyClass) {
            return ((PyClass)cls).__name__;
        }
        if (cls instanceof PyType) {
            return ((PyType)cls).fastGetName();
        }
        return "?";
    }

    private String getInstClassName(PyObject inst) {
        if (inst == null) {
            return "nothing";
        }
        PyObject klass = inst.__findattr__("__class__");
        if (klass == null) {
            klass = inst.getType();
        }
        return getClassName(klass);
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
}
