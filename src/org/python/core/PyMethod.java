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
@ExposedType(name = "instancemethod", isBaseType = false)
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
        super(TYPE);
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
        PyObject classObj = ap.getPyObject(2, null);

        if (!func.isCallable()) {
            throw Py.TypeError("first argument must be callable");
        }
        if (self == Py.None && classObj == null) {
            throw Py.TypeError("unbound methods must have non-NULL im_class");
        }
        return new PyMethod(func, self, classObj);
    }

    @Override
    public PyObject __findattr_ex__(String name) {
        return instancemethod___findattr_ex__(name);
    }
 
    final PyObject instancemethod___findattr_ex__(String name) {
        PyObject ret = super.__findattr_ex__(name);
        if (ret != null) {
            return ret;
        }
        return im_func.__findattr_ex__(name);
    }
    
    @ExposedMethod(doc = BuiltinDocs.instancemethod___getattribute___doc)
    final PyObject instancemethod___getattribute__(PyObject arg0) {
        String name = asName(arg0);
        PyObject ret = instancemethod___findattr_ex__(name);
        if (ret == null) {
            noAttributeError(name);
        }
        return ret;
    }

    @Override
    public PyObject __get__(PyObject obj, PyObject type) {
        return instancemethod___get__(obj, type);
    }

    @ExposedMethod(defaults = "null", doc = BuiltinDocs.instancemethod___get___doc)
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
    /*// This has been disabled since JavaFunc expects to be called with self + boxed args
    @Override
    public PyObject __call__() {
        return __call__(Py.getThreadState());
    }
    
    @Override
    public PyObject __call__(ThreadState state) {
        PyObject self = checkSelf(null, null);
        if (self == null) {
            return im_func.__call__(state);
        } else {
            return im_func.__call__(state, self);
        }
    }
    
    @Override
    public PyObject __call__(PyObject arg0) {
        return __call__(Py.getThreadState(), arg0);
    }
    
    @Override
    public PyObject __call__(ThreadState state, PyObject arg0) {
        PyObject self = checkSelf(arg0, null);
        if (self == null) {
            return im_func.__call__(state, arg0);
        } else {
            return im_func.__call__(state, self, arg0);
        }
    }
    
    @Override
    public PyObject __call__(PyObject arg0, PyObject arg1) {
        return __call__(Py.getThreadState(), arg0, arg1);
    }
    
    @Override
    public PyObject __call__(ThreadState state, PyObject arg0, PyObject arg1) {
        PyObject self = checkSelf(arg0, null);
        if (self == null) {
            return im_func.__call__(state, arg0, arg1);
        } else {
            return im_func.__call__(state, self, arg0, arg1);
        }
    }
    
    @Override
    public PyObject __call__(PyObject arg0, PyObject arg1, PyObject arg2) {
        return __call__(Py.getThreadState(), arg0, arg1, arg2);
    }
    
    @Override
    public PyObject __call__(ThreadState state, PyObject arg0, PyObject arg1,
            PyObject arg2) {
        PyObject self = checkSelf(arg0, null);
        if (self == null) {
            return im_func.__call__(state, arg0, arg1, arg2);
        } else {
            return im_func.__call__(state, self, arg0, arg1, arg2);
        }
    }
    
    @Override
    public PyObject __call__(PyObject arg0, PyObject arg1, PyObject arg2, PyObject arg3) {
        return __call__(Py.getThreadState(), arg0, arg1, arg2, arg3);
    }
    
    @Override
    public PyObject __call__(ThreadState state, PyObject arg0, PyObject arg1,
            PyObject arg2, PyObject arg3) {
        PyObject self = checkSelf(arg0, null);
        if (self == null) {
            return im_func.__call__(state, arg0, arg1, arg2, arg3);
        } else {
            return im_func.__call__(state, self, new PyObject[]{arg0, arg1, arg2, arg3}, Py.NoKeywords);
        }
    }
    
    
    @Override
    public PyObject __call__(PyObject arg1, PyObject[] args, String[] keywords) {
        return __call__(Py.getThreadState(), arg1, args, keywords);
    }
    
    @Override
    public PyObject __call__(ThreadState state, PyObject arg1, PyObject[] args,
            String[] keywords) {
        PyObject self = checkSelf(arg1, args);
        if (self == null) {
            return im_func.__call__(state, arg1, args, keywords);
        } else {
            PyObject[] new_args = new PyObject[args.length+1];
            System.arraycopy(args, 0, new_args, 1, args.length);
            new_args[0] = arg1;
            return im_func.__call__(state, self, new_args, keywords);
        }
    }
    
    @Override
    public PyObject __call__(PyObject[] args) {
        return __call__(Py.getThreadState(), args);
    }
    
    @Override
    public PyObject __call__(ThreadState state, PyObject[] args) {
        return __call__(state, args, Py.NoKeywords);
    }
    */
    @Override
    public PyObject __call__(PyObject[] args, String[] keywords) {
        return instancemethod___call__(args, keywords);
    }
    
    @Override
    public PyObject __call__(ThreadState state, PyObject[] args, String[] keywords) {
        PyObject self = checkSelf(null, args);
        if (self == null) {
            return im_func.__call__(state, args, keywords);
        } else {
            return im_func.__call__(state, self, args, keywords);
        }
    }
    
    @ExposedMethod(doc = BuiltinDocs.instancemethod___call___doc)
    final PyObject instancemethod___call__(PyObject[] args, String[] keywords) {
        return __call__(Py.getThreadState(), args, keywords);
    }

    private PyObject checkSelf(PyObject arg, PyObject[] args) {
        PyObject self = im_self;
        if (self == null) {
            // Unbound methods must be called with an instance of the
            // class (or a derived class) as first argument
            boolean ok;
            if (arg != null) {
                self = arg;
            } else if (args != null && args.length >= 1) {
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
            return null;
        } else {
            return self;
        }
    }

    @Override
    public int __cmp__(PyObject other) {
        return instancemethod___cmp__(other);
    }

    @ExposedMethod(type = MethodType.CMP, doc = BuiltinDocs.instancemethod___cmp___doc)
    final int instancemethod___cmp__(PyObject other) {
        if (!(other instanceof PyMethod)) {
            return -2;
        }
        PyMethod otherMethod = (PyMethod)other;
        int cmp = im_func._cmp(otherMethod.im_func);
        if (cmp != 0) {
            return cmp;
        }
        if (im_self == otherMethod.im_self) {
            return 0;
        }
        if (im_self == null || otherMethod.im_self == null) {
            return System.identityHashCode(im_self) < System.identityHashCode(otherMethod.im_self)
                    ? -1 : 1;
        } else {
            return im_self._cmp(otherMethod.im_self);
        }
    }

    @Override
    public int hashCode() {
        int hashCode = im_self == null ? Py.None.hashCode() : im_self.hashCode();
        return hashCode ^ im_func.hashCode();
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
        PyObject classObj = inst.__findattr__("__class__");
        if (classObj == null) {
            classObj = inst.getType();
        }
        return getClassName(classObj);
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
