// Copyright (c) Corporation for National Research Initiatives
package org.python.core;

import org.python.expose.ExposedMethod;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedType;
import org.python.expose.MethodType;

/**
 * An instance of a classic Python class.
 */
@ExposedType(name = "instance", isBaseType = false)
public class PyInstance extends PyObject {

    public static final PyType TYPE = PyType.fromClass(PyInstance.class);

    // xxx doc, final name
    public transient PyClass instclass;

    /** The namespace of this instance.  Contains all instance attributes. */
    public PyObject __dict__;

    public PyInstance() {
        super(TYPE);
    }

    public PyInstance(PyClass iclass, PyObject dict) {
        super(TYPE);
        instclass = iclass;
        if (dict == null) {
            dict = new PyStringMap();
        }
        __dict__ = dict;
    }

    public PyInstance(PyClass iclass) {
        this(iclass, null);
    }

    @ExposedNew
    public static PyObject instance___new__(PyNewWrapper new_, boolean init, PyType subtype,
                                            PyObject[] args, String[] keywords) {
        ArgParser ap = new ArgParser("instance", args, keywords, "name", "bases", "dict");
        PyClass klass = (PyClass)ap.getPyObjectByType(0, PyClass.TYPE);
        PyObject dict = ap.getPyObject(1, Py.None);
        if (dict == Py.None) {
            dict = null;
        } else if (!(dict instanceof PyStringMap || dict instanceof PyDictionary)) {
            throw Py.TypeError("instance() second arg must be dictionary or None");
        }
        return new PyInstance(klass, dict);
    }

    public PyObject fastGetClass() {
        return instclass;
    }

    /* Override serialization behavior */
    private void readObject(java.io.ObjectInputStream in)
        throws java.io.IOException, ClassNotFoundException
    {
        in.defaultReadObject();

        String module = in.readUTF();
        String name = in.readUTF();

        /* Check for types and missing members here */
        //System.out.println("module: "+module+", "+name);
        PyObject mod = imp.importName(module.intern(), false);
        PyClass pyc = (PyClass)mod.__getattr__(name.intern());

        instclass = pyc;
    }

    private void writeObject(java.io.ObjectOutputStream out)
        throws java.io.IOException
    {
        //System.out.println("writing: "+getClass().getName());
        out.defaultWriteObject();
        PyObject name = instclass.__findattr__("__module__");
        if (!(name instanceof PyString) || name == Py.None) {
            throw Py.ValueError("Can't find module for class: "+
                                instclass.__name__);
        }
        out.writeUTF(name.toString());
        name = instclass.__findattr__("__name__");
        if (!(name instanceof PyString) || name == Py.None) {
            throw Py.ValueError("Can't find module for class with no name");
        }

        out.writeUTF(name.toString());
    }

    @Override
    public Object __tojava__(Class c) {
        if (c.isInstance(this))
            return this;

        if (instclass.__tojava__ != null) {
            // try {
            PyObject ret = instclass.__tojava__.__call__(this, PyType.fromClass(c));

            if (ret == Py.None)
                return Py.NoConversion;
            if (ret != this)
                return ret.__tojava__(c);
            /*} catch (PyException exc) {
              System.err.println("Error in __tojava__ method");
              Py.printException(exc);
              }*/
        }
        return Py.NoConversion;
    }

    public void __init__(PyObject[] args, String[] keywords) {
        // Invoke our own init function
        PyObject init = instclass.lookup("__init__");
        PyObject ret = null;
        if (init != null) {
            ret = init.__call__(this, args, keywords);
        }
        if (ret == null) {
            if (args.length != 0) {
                init = instclass.lookup("__init__");
                if (init != null) {
                    init.__call__(this, args, keywords);
                } else {
                    throw Py.TypeError("this constructor takes no arguments");
                }
            }
        }
        else if (ret != Py.None) {
            throw Py.TypeError("__init__() should return None");
        }
    }

    @Override
    public PyObject __findattr_ex__(String name) {
        PyObject result = ifindlocal(name);
        if (result != null) {
            return result;
        }
        // it wasn't found in the instance, try the class
        result = instclass.lookup(name);
        if (result != null) {
            return result.__get__(this, instclass);
        }
        return ifindfunction(name);
    }

    protected PyObject ifindlocal(String name) {
        if (name == "__dict__") return __dict__;
        if (name == "__class__") return instclass;
        if (__dict__ == null) return null;

        return __dict__.__finditem__(name);
    }

    protected PyObject ifindclass(String name) {
        return instclass.lookup(name);
    }

    protected PyObject ifindfunction(String name) {
        PyObject getter = instclass.__getattr__;
        if (getter == null)
            return null;

        return getter.__call__(this, new PyString(name));
    }

    @Override
    public boolean isCallable() {
        return __findattr__("__call__") != null;
    }

    @Override
    public boolean isIndex() {
        return __findattr__("__index__") != null;
    }

    @Override
    public PyObject invoke(String name) {
        PyObject f = ifindlocal(name);
        if (f == null) {
            f = ifindclass(name);
            if (f != null) {
                if (f instanceof PyFunction) {
                    return f.__call__(this);
                } else {
                    f = f.__get__(this, instclass);
                }
            }
        }
        if (f == null) f = ifindfunction(name);
        if (f == null) noAttributeError(name);
        return f.__call__();
    }

    @Override
    public PyObject invoke(String name, PyObject arg1) {
        PyObject f = ifindlocal(name);
        if (f == null) {
            f = ifindclass(name);
            if (f != null) {
                if (f instanceof PyFunction) {
                    return f.__call__(this, arg1);
                } else {
                    f = f.__get__(this, instclass);
                }
            }
        }
        if (f == null) f = ifindfunction(name);
        if (f == null) noAttributeError(name);
        return f.__call__(arg1);
    }

    @Override
    public PyObject invoke(String name, PyObject arg1, PyObject arg2) {
        PyObject f = ifindlocal(name);
        if (f == null) {
            f = ifindclass(name);
            if (f != null) {
                if (f instanceof PyFunction) {
                    return f.__call__(this, arg1, arg2);
                } else {
                    f = f.__get__(this, instclass);
                }
            }
        }
        if (f == null) f = ifindfunction(name);
        if (f == null) noAttributeError(name);
        return f.__call__(arg1, arg2);
    }

    @Override
    public void noAttributeError(String name) {
        throw Py.AttributeError(String.format("%.50s instance has no attribute '%.400s'",
                                              instclass.__name__, name));
    }


    @Override
    public void __setattr__(String name, PyObject value) {
        instance___setattr__(name, value);
    }

    @ExposedMethod
    final void instance___setattr__(String name, PyObject value) {
        if (name == "__class__") {
            if (value instanceof PyClass) {
                instclass = (PyClass)value;
            } else {
                throw Py.TypeError("__class__ must be set to a class");
            }
            return;
        } else if (name == "__dict__") {
            __dict__ = value;
            return;
        }

        PyObject setter = instclass.__setattr__;
        if (setter != null) {
            setter.__call__(this, new PyString(name), value);
        } else {
            __dict__.__setitem__(name, value);
        }
    }

    protected void noField(String name, PyObject value) {
        __dict__.__setitem__(name, value);
    }

    protected void unassignableField(String name, PyObject value) {
        __dict__.__setitem__(name, value);
    }

    @Override
    public void __delattr__(String name) {
        instance___delattr__(name);
    }

    @ExposedMethod
    final void instance___delattr__(String name) {
        PyObject deller = instclass.__delattr__;
        if (deller != null) {
            deller.__call__(this, new PyString(name));
        } else {
            try {
                __dict__.__delitem__(name);
            } catch (PyException exc) {
                if (exc.match(Py.KeyError))
                    throw Py.AttributeError("class " + instclass.__name__ +
                                        " has no attribute '" + name + "'");
            };
        }
    }

    public PyObject invoke_ex(String name, PyObject[] args, String[] keywords)
    {
        PyObject meth = __findattr__(name);
        if (meth == null)
            return null;
        return meth.__call__(args, keywords);
    }

    public PyObject invoke_ex(String name) {
        PyObject meth = __findattr__(name);
        if (meth == null)
            return null;
        return meth.__call__();
    }
    public PyObject invoke_ex(String name, PyObject arg1) {
        PyObject meth = __findattr__(name);
        if (meth == null)
            return null;
        return meth.__call__(arg1);
    }
    public PyObject invoke_ex(String name, PyObject arg1, PyObject arg2) {
        PyObject meth = __findattr__(name);
        if (meth == null)
            return null;
        return meth.__call__(arg1, arg2);
    }

    @Override
    public PyObject __call__(PyObject args[], String keywords[]) {
        return instance___call__(args, keywords);
    }

    @ExposedMethod
    final PyObject instance___call__(PyObject args[], String keywords[]) {
        ThreadState ts = Py.getThreadState();
        if (ts.recursion_depth++ > ts.systemState.getrecursionlimit())
            throw Py.RuntimeError("maximum __call__ recursion depth exceeded");
        try {
            return invoke("__call__", args, keywords);
        } finally {
            --ts.recursion_depth;
        }
    }

    @Override
    public String toString() {
        return __repr__().toString();
    }

    @Override
    public PyString __repr__() {
        return instance___repr__();
    }

    @ExposedMethod
    final PyString instance___repr__() {
        PyObject ret = invoke_ex("__repr__");
        if (ret == null) {
            return makeDefaultRepr();
        }
        if (!(ret instanceof PyString))
            throw Py.TypeError("__repr__ method must return a string");
        return (PyString)ret;
    }

    /**
     * If a class doesn't define a __repr__ method of its own, the return
     * value from this method is used.
     */
    protected PyString makeDefaultRepr() {
        PyObject mod = instclass.__dict__.__finditem__("__module__");
        String smod;
        if(mod == Py.None) {
            smod = "";
        } else {
            if(mod == null || !(mod instanceof PyString)) {
                smod = "<unknown>.";
            } else {
                smod = ((PyString)mod).toString() + '.';
            }
        }
        return new PyString("<" + smod + instclass.__name__ + " instance at " +
                            Py.idstr(this) + ">");
    }

    @Override
    public PyString __str__() {
        return instance___str__();
    }

    @ExposedMethod
    final PyString instance___str__() {
        PyObject ret = invoke_ex("__str__");
        if (ret == null)
            return __repr__();
        if (!(ret instanceof PyString))
            throw Py.TypeError("__str__ method must return a string");
        return (PyString)ret;
    }

    @Override
    public PyUnicode __unicode__() {
        return instance___unicode__();
    }

    @ExposedMethod
    final PyUnicode instance___unicode__() {
        PyObject ret = invoke_ex("__unicode__");
        if(ret == null) {
            return super.__unicode__();
        } else if(ret instanceof PyUnicode) {
            return (PyUnicode)ret;
        } else if(ret instanceof PyString) {
            return new PyUnicode((PyString)ret);
        } else {
            throw Py.TypeError("__unicode__ must return unicode or str");
        }
    }

    @Override
    public int hashCode() {
        PyObject ret;
        ret = invoke_ex("__hash__");
        if (ret == null) {
            if (__findattr__("__eq__") != null || __findattr__("__cmp__") != null) {
                throw Py.TypeError("unhashable instance");
            }
            return super.hashCode();
        }
        if (ret instanceof PyInteger) {
            return ((PyInteger)ret).getValue();
        }
        else if (ret instanceof PyLong) {
            return ((PyLong)ret).hashCode();
        }
        throw Py.TypeError("__hash__() must really return int" + ret.getType() );
    }

    @Override
    public int __cmp__(PyObject other) {
        return instance___cmp__(other);
    }

    // special case: does all the work
    @ExposedMethod
    final int instance___cmp__(PyObject other) {
        PyObject[] coerced = this._coerce(other);
        PyObject v;
        PyObject w;
        PyObject ret = null;
        if (coerced != null) {
            v = coerced[0];
            w = coerced[1];
            if (!(v instanceof PyInstance) && !(w instanceof PyInstance)) {
                return v._cmp(w);
            }
        } else {
            v = this;
            w = other;
        }
        if (v instanceof PyInstance) {
            ret = ((PyInstance)v).invoke_ex("__cmp__",w);
            if (ret != null) {
                if (ret instanceof PyInteger) {
                    int result = ((PyInteger)ret).getValue();
                    return result < 0 ? -1 : result > 0 ? 1 : 0;
                }
                throw Py.TypeError("__cmp__() must return int");
            }
        }
        if (w instanceof PyInstance) {
            ret = ((PyInstance)w).invoke_ex("__cmp__",v);
            if (ret != null) {
                if (ret instanceof PyInteger) {
                    int result = ((PyInteger)ret).getValue();
                    return -(result < 0 ? -1 : result > 0 ? 1 : 0);
                }
                throw Py.TypeError("__cmp__() must return int");
            }

        }
        return -2;
    }

    private PyObject invoke_ex_richcmp(String name, PyObject o) {
        PyObject ret = invoke_ex(name, o);
        if (ret == Py.NotImplemented)
            return null;
        return ret;
    }

    @Override
    public PyObject __lt__(PyObject o) {
        return instance___lt__(o);
    }

    @ExposedMethod
    final PyObject instance___lt__(PyObject o) {
        return invoke_ex_richcmp("__lt__", o);
    }

    @Override
    public PyObject __le__(PyObject o) {
        return instance___le__(o);
    }

    @ExposedMethod
    final PyObject instance___le__(PyObject o) {
        return invoke_ex_richcmp("__le__", o);
    }

    @Override
    public PyObject __gt__(PyObject o) {
        return instance___gt__(o);
    }

    @ExposedMethod
    final PyObject instance___gt__(PyObject o) {
        return invoke_ex_richcmp("__gt__", o);
    }

    @Override
    public PyObject __ge__(PyObject o) {
        return instance___ge__(o);
    }

    @ExposedMethod
    final PyObject instance___ge__(PyObject o) {
        return invoke_ex_richcmp("__ge__", o);
    }

    @Override
    public PyObject __eq__(PyObject o) {
        return instance___eq__(o);
    }

    @ExposedMethod
    final PyObject instance___eq__(PyObject o) {
        return invoke_ex_richcmp("__eq__", o);
    }

    @Override
    public PyObject __ne__(PyObject o) {
        return instance___ne__(o);
    }

    @ExposedMethod
    final PyObject instance___ne__(PyObject o) {
        return invoke_ex_richcmp("__ne__", o);
    }

    @Override
    public boolean __nonzero__() {
        return instance___nonzero__();
    }

    @ExposedMethod
    final boolean instance___nonzero__() {
        PyObject meth = null;
        try {
            meth = __findattr__("__nonzero__");
        } catch (PyException exc) { }

        if (meth == null) {
            try {
                meth = __findattr__("__len__");
            } catch (PyException exc) { }
            if (meth == null) {
                return true;
            }
        }

        PyObject ret = meth.__call__();
        return ret.__nonzero__();
    }

    @Override
    public int __len__() {
        return instance___len__();
    }

    @ExposedMethod
    final int instance___len__() {
        PyObject ret = invoke("__len__");
        if (ret instanceof PyInteger)
            return ((PyInteger)ret).getValue();
        throw Py.TypeError("__len__() should return an int");
    }

    @Override
    public PyObject __finditem__(int key) {
        return __finditem__(new PyInteger(key));
    }

    private PyObject trySlice(String name, PyObject start, PyObject stop) {
        return trySlice(name, start, stop, null);
    }

    private PyObject trySlice(String name, PyObject start, PyObject stop, PyObject extraArg) {
        PyObject func = __findattr__(name);
        if (func == null) {
            return null;
        }

        PyObject[] indices = PySlice.indices2(this, start, stop);
        start = indices[0];
        stop = indices[1];

        if (extraArg == null) {
            return func.__call__(start, stop);
        } else {
            return func.__call__(start, stop, extraArg);
        }
    }

    @Override
    public PyObject __finditem__(PyObject key) {
        try {
            return invoke("__getitem__", key);
        } catch (PyException e) {
            if (e.match(Py.IndexError))
                return null;
            if (e.match(Py.KeyError))
                return null;
            throw e;
        }
    }

    @Override
    public PyObject __getitem__(PyObject key) {
        return instance___getitem__(key);
    }

    @ExposedMethod
    final PyObject instance___getitem__(PyObject key) {
        return invoke("__getitem__", key);
    }

    @Override
    public void __setitem__(PyObject key, PyObject value) {
        instance___setitem__(key, value);
    }

    @ExposedMethod
    final void instance___setitem__(PyObject key, PyObject value) {
        invoke("__setitem__", key, value);
    }

    @Override
    public void __delitem__(PyObject key) {
        instance___delitem__(key);
    }

    @ExposedMethod
    final void instance___delitem__(PyObject key) {
        invoke("__delitem__", key);
    }

    @Override
    public PyObject __getslice__(PyObject start, PyObject stop, PyObject step) {
        return instance___getslice__(start, stop, step);
    }

    @ExposedMethod
    final PyObject instance___getslice__(PyObject start, PyObject stop, PyObject step) {
        if (step != null) {
            return __getitem__(new PySlice(start, stop, step));
        }
        PyObject ret = trySlice("__getslice__", start, stop);
        if (ret != null) {
            return ret;
        }
        return super.__getslice__(start, stop, step);
    }

    @Override
    public void __setslice__(PyObject start, PyObject stop, PyObject step, PyObject value) {
        instance___setslice__(start, stop, step, value);
    }

    @ExposedMethod
    final void instance___setslice__(PyObject start, PyObject stop, PyObject step, PyObject value) {
        if (step != null) {
            __setitem__(new PySlice(start, stop, step), value);
        } else if (trySlice("__setslice__", start, stop, value) == null) {
            super.__setslice__(start, stop, step, value);
        }
    }

    @Override
    public void __delslice__(PyObject start, PyObject stop, PyObject step) {
        instance___delslice__(start, stop, step);
    }

    @ExposedMethod
    final void instance___delslice__(PyObject start, PyObject stop, PyObject step) {
        if (step != null) {
            __delitem__(new PySlice(start, stop, step));
        } else if (trySlice("__delslice__", start, stop) == null) {
            super.__delslice__(start, stop, step);
        }
    }

    @Override
    public PyObject __iter__() {
        return instance___iter__();
    }

    @ExposedMethod
    final PyObject instance___iter__() {
        PyObject func = __findattr__("__iter__");
        if (func != null)
            return func.__call__();
        func = __findattr__("__getitem__");
        if (func == null) {
            return super.__iter__();
        }
        return new PySequenceIter(this);
    }

    @Override
    public PyObject __iternext__() {
        PyObject func = __findattr__("next");
        if (func != null) {
            try {
                return func.__call__();
            } catch (PyException exc) {
                if (exc.match(Py.StopIteration))
                    return null;
                throw exc;
            }
        }
        throw Py.TypeError("instance has no next() method");
    }

    @Override
    public boolean __contains__(PyObject o) {
        return instance___contains__(o);
    }

    @ExposedMethod
    final boolean instance___contains__(PyObject o) {
        PyObject func = __findattr__("__contains__");
        if (func == null)
           return super.__contains__(o);
        PyObject ret = func.__call__(o);
        return ret.__nonzero__();
    }

    @Override
    public Object __coerce_ex__(PyObject o) {
        PyObject ret = invoke_ex("__coerce__", o);
        if (ret == null || ret == Py.None)
            return ret;
        if (!(ret instanceof PyTuple))
            throw Py.TypeError("coercion should return None or 2-tuple");
        return ((PyTuple)ret).getArray();
    }

    @Override
    public PyObject __index__() {
        return instance___index__();
    }

    /**
     * Implements the __index__ method by looking it up
     * in the instance's dictionary and calling it if it is found.
     **/
    @ExposedMethod
    final PyObject instance___index__() {
        PyObject ret;
        try {
            ret = invoke("__index__");
        } catch (PyException pye) {
            if (!pye.match(Py.AttributeError)) {
                throw pye;
            }
            throw Py.TypeError("object cannot be interpreted as an index");
        }
        if (ret instanceof PyInteger || ret instanceof PyLong) {
            return ret;
        }
        throw Py.TypeError(String.format("__index__ returned non-(int,long) (type %s)",
                                         ret.getType().fastGetName()));
    }

    // Generated by make_binops.py

    // Unary ops

    @Override
    public PyString __hex__() {
        return instance___hex__();
    }

    /**
     * Implements the __hex__ method by looking it up
     * in the instance's dictionary and calling it if it is found.
     **/
    @ExposedMethod
    final PyString instance___hex__() {
        PyObject ret = invoke("__hex__");
        if (ret instanceof PyString)
            return (PyString)ret;
        throw Py.TypeError("__hex__() should return a string");
    }

    @Override
    public PyString __oct__() {
        return instance___oct__();
    }

    /**
     * Implements the __oct__ method by looking it up
     * in the instance's dictionary and calling it if it is found.
     **/
    @ExposedMethod
    final PyString instance___oct__() {
        PyObject ret = invoke("__oct__");
        if (ret instanceof PyString)
            return (PyString)ret;
        throw Py.TypeError("__oct__() should return a string");
    }

    @Override
    public PyObject __int__() {
        return instance___int__();
    }

    /**
     * Implements the __int__ method by looking it up
     * in the instance's dictionary and calling it if it is found.
     **/
    @ExposedMethod
    final PyObject instance___int__() {
        PyObject ret = invoke("__int__");
        if (ret instanceof PyLong || ret instanceof PyInteger)
            return ret;
        throw Py.TypeError("__int__() should return a int");
    }

    @Override
    public PyFloat __float__() {
        return instance___float__();
    }

    /**
     * Implements the __float__ method by looking it up
     * in the instance's dictionary and calling it if it is found.
     **/
    @ExposedMethod
    final PyFloat instance___float__() {
        PyObject ret = invoke("__float__");
        if (ret instanceof PyFloat)
            return (PyFloat)ret;
        throw Py.TypeError("__float__() should return a float");
    }

    @Override
    public PyObject __long__() {
        return instance___long__();
    }

    /**
     * Implements the __long__ method by looking it up
     * in the instance's dictionary and calling it if it is found.
     **/
    @ExposedMethod
    final PyObject instance___long__() {
        PyObject ret = invoke("__long__");
        if (ret instanceof PyLong || ret instanceof PyInteger)
            return ret;
        throw Py.TypeError("__long__() should return a long");
    }

    @Override
    public PyComplex __complex__() {
        return instance___complex__();
    }

    /**
     * Implements the __complex__ method by looking it up
     * in the instance's dictionary and calling it if it is found.
     **/
    @ExposedMethod
    final PyComplex instance___complex__() {
        PyObject ret = invoke("__complex__");
        if (ret instanceof PyComplex)
            return (PyComplex)ret;
        throw Py.TypeError("__complex__() should return a complex");
    }

    @Override
    public PyObject __pos__() {
        return instance___pos__();
    }

    /**
     * Implements the __pos__ method by looking it up
     * in the instance's dictionary and calling it if it is found.
     **/
    @ExposedMethod
    public PyObject instance___pos__() {
        return invoke("__pos__");
    }

    @Override
    public PyObject __neg__() {
        return instance___neg__();
    }

    /**
     * Implements the __neg__ method by looking it up
     * in the instance's dictionary and calling it if it is found.
     **/
    @ExposedMethod
    public PyObject instance___neg__() {
        return invoke("__neg__");
    }

    @Override
    public PyObject __abs__() {
        return instance___abs__();
    }

    /**
     * Implements the __abs__ method by looking it up
     * in the instance's dictionary and calling it if it is found.
     **/
    @ExposedMethod
    public PyObject instance___abs__() {
        return invoke("__abs__");
    }

    @Override
    public PyObject __invert__() {
        return instance___invert__();
    }

    /**
     * Implements the __invert__ method by looking it up
     * in the instance's dictionary and calling it if it is found.
     **/
    @ExposedMethod
    public PyObject instance___invert__() {
        return invoke("__invert__");
    }

    // Binary ops

    @Override
    public PyObject __add__(PyObject o) {
        return instance___add__(o);
    }

    /**
     * Implements the __add__ method by looking it up
     * in the instance's dictionary and calling it if it is found.
     **/
    @ExposedMethod(type = MethodType.BINARY)
    public PyObject instance___add__(PyObject o) {
        Object ctmp = __coerce_ex__(o);
        if (ctmp == null || ctmp == Py.None)
            return invoke_ex("__add__", o);
        else {
            PyObject o1 = ((PyObject[])ctmp)[0];
            PyObject o2 = ((PyObject[])ctmp)[1];
            if (this == o1) {
                // Prevent recusion if __coerce__ return self
                return invoke_ex("__add__", o2);
            }
            else {
                ThreadState ts = Py.getThreadState();
                if (ts.recursion_depth++ > ts.systemState.getrecursionlimit())
                    throw Py.RuntimeError("maximum recursion depth exceeded");
                try {
                    return o1._add(o2);
                } finally {
                    --ts.recursion_depth;
                }
            }
        }
    }

    @Override
    public PyObject __radd__(PyObject o) {
        return instance___radd__(o);
    }

    /**
     * Implements the __radd__ method by looking it up
     * in the instance's dictionary and calling it if it is found.
     **/
    @ExposedMethod(type = MethodType.BINARY)
    public PyObject instance___radd__(PyObject o) {
        Object ctmp = __coerce_ex__(o);
        if (ctmp == null || ctmp == Py.None)
            return invoke_ex("__radd__", o);
        else {
            PyObject o1 = ((PyObject[])ctmp)[0];
            PyObject o2 = ((PyObject[])ctmp)[1];
            if (this == o1) {
                // Prevent recusion if __coerce__ return self
                return invoke_ex("__radd__", o2);
            }
            else {
                ThreadState ts = Py.getThreadState();
                if (ts.recursion_depth++ > ts.systemState.getrecursionlimit())
                    throw Py.RuntimeError("maximum recursion depth exceeded");
                try {
                    return o2._add(o1);
                } finally {
                    --ts.recursion_depth;
                }
            }
        }
    }

    @Override
    public PyObject __iadd__(PyObject o) {
        return instance___iadd__(o);
    }

    /**
     * Implements the __iadd__ method by looking it up
     * in the instance's dictionary and calling it if it is found.
     **/
    @ExposedMethod(type = MethodType.BINARY)
    public PyObject instance___iadd__(PyObject o) {
        PyObject ret = invoke_ex("__iadd__", o);
        if (ret != null)
            return ret;
        return super.__iadd__(o);
    }

    @Override
    public PyObject __sub__(PyObject o) {
        return instance___sub__(o);
    }

    /**
     * Implements the __sub__ method by looking it up
     * in the instance's dictionary and calling it if it is found.
     **/
    @ExposedMethod(type = MethodType.BINARY)
    public PyObject instance___sub__(PyObject o) {
        Object ctmp = __coerce_ex__(o);
        if (ctmp == null || ctmp == Py.None)
            return invoke_ex("__sub__", o);
        else {
            PyObject o1 = ((PyObject[])ctmp)[0];
            PyObject o2 = ((PyObject[])ctmp)[1];
            if (this == o1) {
                // Prevent recusion if __coerce__ return self
                return invoke_ex("__sub__", o2);
            }
            else {
                ThreadState ts = Py.getThreadState();
                if (ts.recursion_depth++ > ts.systemState.getrecursionlimit())
                    throw Py.RuntimeError("maximum recursion depth exceeded");
                try {
                    return o1._sub(o2);
                } finally {
                    --ts.recursion_depth;
                }
            }
        }
    }

    @Override
    public PyObject __rsub__(PyObject o) {
        return instance___rsub__(o);
    }

    /**
     * Implements the __rsub__ method by looking it up
     * in the instance's dictionary and calling it if it is found.
     **/
    @ExposedMethod(type = MethodType.BINARY)
    public PyObject instance___rsub__(PyObject o) {
        Object ctmp = __coerce_ex__(o);
        if (ctmp == null || ctmp == Py.None)
            return invoke_ex("__rsub__", o);
        else {
            PyObject o1 = ((PyObject[])ctmp)[0];
            PyObject o2 = ((PyObject[])ctmp)[1];
            if (this == o1) {
                // Prevent recusion if __coerce__ return self
                return invoke_ex("__rsub__", o2);
            }
            else {
                ThreadState ts = Py.getThreadState();
                if (ts.recursion_depth++ > ts.systemState.getrecursionlimit())
                    throw Py.RuntimeError("maximum recursion depth exceeded");
                try {
                    return o2._sub(o1);
                } finally {
                    --ts.recursion_depth;
                }
            }
        }
    }

    @Override
    public PyObject __isub__(PyObject o) {
        return instance___isub__(o);
    }

    /**
     * Implements the __isub__ method by looking it up
     * in the instance's dictionary and calling it if it is found.
     **/
    @ExposedMethod(type = MethodType.BINARY)
    public PyObject instance___isub__(PyObject o) {
        PyObject ret = invoke_ex("__isub__", o);
        if (ret != null)
            return ret;
        return super.__isub__(o);
    }

    @Override
    public PyObject __mul__(PyObject o) {
        return instance___mul__(o);
    }

    /**
     * Implements the __mul__ method by looking it up
     * in the instance's dictionary and calling it if it is found.
     **/
    @ExposedMethod(type = MethodType.BINARY)
    public PyObject instance___mul__(PyObject o) {
        Object ctmp = __coerce_ex__(o);
        if (ctmp == null || ctmp == Py.None)
            return invoke_ex("__mul__", o);
        else {
            PyObject o1 = ((PyObject[])ctmp)[0];
            PyObject o2 = ((PyObject[])ctmp)[1];
            if (this == o1) {
                // Prevent recusion if __coerce__ return self
                return invoke_ex("__mul__", o2);
            }
            else {
                ThreadState ts = Py.getThreadState();
                if (ts.recursion_depth++ > ts.systemState.getrecursionlimit())
                    throw Py.RuntimeError("maximum recursion depth exceeded");
                try {
                    return o1._mul(o2);
                } finally {
                    --ts.recursion_depth;
                }
            }
        }
    }

    @Override
    public PyObject __rmul__(PyObject o) {
        return instance___rmul__(o);
    }

    /**
     * Implements the __rmul__ method by looking it up
     * in the instance's dictionary and calling it if it is found.
     **/
    @ExposedMethod(type = MethodType.BINARY)
    public PyObject instance___rmul__(PyObject o) {
        Object ctmp = __coerce_ex__(o);
        if (ctmp == null || ctmp == Py.None)
            return invoke_ex("__rmul__", o);
        else {
            PyObject o1 = ((PyObject[])ctmp)[0];
            PyObject o2 = ((PyObject[])ctmp)[1];
            if (this == o1) {
                // Prevent recusion if __coerce__ return self
                return invoke_ex("__rmul__", o2);
            }
            else {
                ThreadState ts = Py.getThreadState();
                if (ts.recursion_depth++ > ts.systemState.getrecursionlimit())
                    throw Py.RuntimeError("maximum recursion depth exceeded");
                try {
                    return o2._mul(o1);
                } finally {
                    --ts.recursion_depth;
                }
            }
        }
    }

    @Override
    public PyObject __imul__(PyObject o) {
        return instance___imul__(o);
    }

    /**
     * Implements the __imul__ method by looking it up
     * in the instance's dictionary and calling it if it is found.
     **/
    @ExposedMethod(type = MethodType.BINARY)
    public PyObject instance___imul__(PyObject o) {
        PyObject ret = invoke_ex("__imul__", o);
        if (ret != null)
            return ret;
        return super.__imul__(o);
    }

    @Override
    public PyObject __div__(PyObject o) {
        return instance___div__(o);
    }

    /**
     * Implements the __div__ method by looking it up
     * in the instance's dictionary and calling it if it is found.
     **/
    @ExposedMethod(type = MethodType.BINARY)
    public PyObject instance___div__(PyObject o) {
        Object ctmp = __coerce_ex__(o);
        if (ctmp == null || ctmp == Py.None)
            return invoke_ex("__div__", o);
        else {
            PyObject o1 = ((PyObject[])ctmp)[0];
            PyObject o2 = ((PyObject[])ctmp)[1];
            if (this == o1) {
                // Prevent recusion if __coerce__ return self
                return invoke_ex("__div__", o2);
            }
            else {
                ThreadState ts = Py.getThreadState();
                if (ts.recursion_depth++ > ts.systemState.getrecursionlimit())
                    throw Py.RuntimeError("maximum recursion depth exceeded");
                try {
                    return o1._div(o2);
                } finally {
                    --ts.recursion_depth;
                }
            }
        }
    }

    @Override
    public PyObject __rdiv__(PyObject o) {
        return instance___rdiv__(o);
    }

    /**
     * Implements the __rdiv__ method by looking it up
     * in the instance's dictionary and calling it if it is found.
     **/
    @ExposedMethod(type = MethodType.BINARY)
    public PyObject instance___rdiv__(PyObject o) {
        Object ctmp = __coerce_ex__(o);
        if (ctmp == null || ctmp == Py.None)
            return invoke_ex("__rdiv__", o);
        else {
            PyObject o1 = ((PyObject[])ctmp)[0];
            PyObject o2 = ((PyObject[])ctmp)[1];
            if (this == o1) {
                // Prevent recusion if __coerce__ return self
                return invoke_ex("__rdiv__", o2);
            }
            else {
                ThreadState ts = Py.getThreadState();
                if (ts.recursion_depth++ > ts.systemState.getrecursionlimit())
                    throw Py.RuntimeError("maximum recursion depth exceeded");
                try {
                    return o2._div(o1);
                } finally {
                    --ts.recursion_depth;
                }
            }
        }
    }

    @Override
    public PyObject __idiv__(PyObject o) {
        return instance___idiv__(o);
    }

    /**
     * Implements the __idiv__ method by looking it up
     * in the instance's dictionary and calling it if it is found.
     **/
    @ExposedMethod(type = MethodType.BINARY)
    public PyObject instance___idiv__(PyObject o) {
        PyObject ret = invoke_ex("__idiv__", o);
        if (ret != null)
            return ret;
        return super.__idiv__(o);
    }

    @Override
    public PyObject __floordiv__(PyObject o) {
        return instance___floordiv__(o);
    }

    /**
     * Implements the __floordiv__ method by looking it up
     * in the instance's dictionary and calling it if it is found.
     **/
    @ExposedMethod(type = MethodType.BINARY)
    public PyObject instance___floordiv__(PyObject o) {
        Object ctmp = __coerce_ex__(o);
        if (ctmp == null || ctmp == Py.None)
            return invoke_ex("__floordiv__", o);
        else {
            PyObject o1 = ((PyObject[])ctmp)[0];
            PyObject o2 = ((PyObject[])ctmp)[1];
            if (this == o1) {
                // Prevent recusion if __coerce__ return self
                return invoke_ex("__floordiv__", o2);
            }
            else {
                ThreadState ts = Py.getThreadState();
                if (ts.recursion_depth++ > ts.systemState.getrecursionlimit())
                    throw Py.RuntimeError("maximum recursion depth exceeded");
                try {
                    return o1._floordiv(o2);
                } finally {
                    --ts.recursion_depth;
                }
            }
        }
    }

    @Override
    public PyObject __rfloordiv__(PyObject o) {
        return instance___rfloordiv__(o);
    }

    /**
     * Implements the __rfloordiv__ method by looking it up
     * in the instance's dictionary and calling it if it is found.
     **/
    @ExposedMethod(type = MethodType.BINARY)
    public PyObject instance___rfloordiv__(PyObject o) {
        Object ctmp = __coerce_ex__(o);
        if (ctmp == null || ctmp == Py.None)
            return invoke_ex("__rfloordiv__", o);
        else {
            PyObject o1 = ((PyObject[])ctmp)[0];
            PyObject o2 = ((PyObject[])ctmp)[1];
            if (this == o1) {
                // Prevent recusion if __coerce__ return self
                return invoke_ex("__rfloordiv__", o2);
            }
            else {
                ThreadState ts = Py.getThreadState();
                if (ts.recursion_depth++ > ts.systemState.getrecursionlimit())
                    throw Py.RuntimeError("maximum recursion depth exceeded");
                try {
                    return o2._floordiv(o1);
                } finally {
                    --ts.recursion_depth;
                }
            }
        }
    }

    @Override
    public PyObject __ifloordiv__(PyObject o) {
        return instance___ifloordiv__(o);
    }

    /**
     * Implements the __ifloordiv__ method by looking it up
     * in the instance's dictionary and calling it if it is found.
     **/
    @ExposedMethod(type = MethodType.BINARY)
    public PyObject instance___ifloordiv__(PyObject o) {
        PyObject ret = invoke_ex("__ifloordiv__", o);
        if (ret != null)
            return ret;
        return super.__ifloordiv__(o);
    }

    @Override
    public PyObject __truediv__(PyObject o) {
        return instance___truediv__(o);
    }

    /**
     * Implements the __truediv__ method by looking it up
     * in the instance's dictionary and calling it if it is found.
     **/
    @ExposedMethod(type = MethodType.BINARY)
    public PyObject instance___truediv__(PyObject o) {
        Object ctmp = __coerce_ex__(o);
        if (ctmp == null || ctmp == Py.None)
            return invoke_ex("__truediv__", o);
        else {
            PyObject o1 = ((PyObject[])ctmp)[0];
            PyObject o2 = ((PyObject[])ctmp)[1];
            if (this == o1) {
                // Prevent recusion if __coerce__ return self
                return invoke_ex("__truediv__", o2);
            }
            else {
                ThreadState ts = Py.getThreadState();
                if (ts.recursion_depth++ > ts.systemState.getrecursionlimit())
                    throw Py.RuntimeError("maximum recursion depth exceeded");
                try {
                    return o1._truediv(o2);
                } finally {
                    --ts.recursion_depth;
                }
            }
        }
    }

    @Override
    public PyObject __rtruediv__(PyObject o) {
        return instance___rtruediv__(o);
    }

    /**
     * Implements the __rtruediv__ method by looking it up
     * in the instance's dictionary and calling it if it is found.
     **/
    @ExposedMethod(type = MethodType.BINARY)
    public PyObject instance___rtruediv__(PyObject o) {
        Object ctmp = __coerce_ex__(o);
        if (ctmp == null || ctmp == Py.None)
            return invoke_ex("__rtruediv__", o);
        else {
            PyObject o1 = ((PyObject[])ctmp)[0];
            PyObject o2 = ((PyObject[])ctmp)[1];
            if (this == o1) {
                // Prevent recusion if __coerce__ return self
                return invoke_ex("__rtruediv__", o2);
            }
            else {
                ThreadState ts = Py.getThreadState();
                if (ts.recursion_depth++ > ts.systemState.getrecursionlimit())
                    throw Py.RuntimeError("maximum recursion depth exceeded");
                try {
                    return o2._truediv(o1);
                } finally {
                    --ts.recursion_depth;
                }
            }
        }
    }

    @Override
    public PyObject __itruediv__(PyObject o) {
        return instance___itruediv__(o);
    }

    /**
     * Implements the __itruediv__ method by looking it up
     * in the instance's dictionary and calling it if it is found.
     **/
    @ExposedMethod(type = MethodType.BINARY)
    public PyObject instance___itruediv__(PyObject o) {
        PyObject ret = invoke_ex("__itruediv__", o);
        if (ret != null)
            return ret;
        return super.__itruediv__(o);
    }

    @Override
    public PyObject __mod__(PyObject o) {
        return instance___mod__(o);
    }

    /**
     * Implements the __mod__ method by looking it up
     * in the instance's dictionary and calling it if it is found.
     **/
    @ExposedMethod(type = MethodType.BINARY)
    public PyObject instance___mod__(PyObject o) {
        Object ctmp = __coerce_ex__(o);
        if (ctmp == null || ctmp == Py.None)
            return invoke_ex("__mod__", o);
        else {
            PyObject o1 = ((PyObject[])ctmp)[0];
            PyObject o2 = ((PyObject[])ctmp)[1];
            if (this == o1) {
                // Prevent recusion if __coerce__ return self
                return invoke_ex("__mod__", o2);
            }
            else {
                ThreadState ts = Py.getThreadState();
                if (ts.recursion_depth++ > ts.systemState.getrecursionlimit())
                    throw Py.RuntimeError("maximum recursion depth exceeded");
                try {
                    return o1._mod(o2);
                } finally {
                    --ts.recursion_depth;
                }
            }
        }
    }

    @Override
    public PyObject __rmod__(PyObject o) {
        return instance___rmod__(o);
    }

    /**
     * Implements the __rmod__ method by looking it up
     * in the instance's dictionary and calling it if it is found.
     **/
    @ExposedMethod(type = MethodType.BINARY)
    public PyObject instance___rmod__(PyObject o) {
        Object ctmp = __coerce_ex__(o);
        if (ctmp == null || ctmp == Py.None)
            return invoke_ex("__rmod__", o);
        else {
            PyObject o1 = ((PyObject[])ctmp)[0];
            PyObject o2 = ((PyObject[])ctmp)[1];
            if (this == o1) {
                // Prevent recusion if __coerce__ return self
                return invoke_ex("__rmod__", o2);
            }
            else {
                ThreadState ts = Py.getThreadState();
                if (ts.recursion_depth++ > ts.systemState.getrecursionlimit())
                    throw Py.RuntimeError("maximum recursion depth exceeded");
                try {
                    return o2._mod(o1);
                } finally {
                    --ts.recursion_depth;
                }
            }
        }
    }

    @Override
    public PyObject __imod__(PyObject o) {
        return instance___imod__(o);
    }

    /**
     * Implements the __imod__ method by looking it up
     * in the instance's dictionary and calling it if it is found.
     **/
    @ExposedMethod(type = MethodType.BINARY)
    public PyObject instance___imod__(PyObject o) {
        PyObject ret = invoke_ex("__imod__", o);
        if (ret != null)
            return ret;
        return super.__imod__(o);
    }

    @Override
    public PyObject __divmod__(PyObject o) {
        return instance___divmod__(o);
    }

    /**
     * Implements the __divmod__ method by looking it up
     * in the instance's dictionary and calling it if it is found.
     **/
    @ExposedMethod(type = MethodType.BINARY)
    public PyObject instance___divmod__(PyObject o) {
        Object ctmp = __coerce_ex__(o);
        if (ctmp == null || ctmp == Py.None)
            return invoke_ex("__divmod__", o);
        else {
            PyObject o1 = ((PyObject[])ctmp)[0];
            PyObject o2 = ((PyObject[])ctmp)[1];
            if (this == o1) {
                // Prevent recusion if __coerce__ return self
                return invoke_ex("__divmod__", o2);
            }
            else {
                ThreadState ts = Py.getThreadState();
                if (ts.recursion_depth++ > ts.systemState.getrecursionlimit())
                    throw Py.RuntimeError("maximum recursion depth exceeded");
                try {
                    return o1._divmod(o2);
                } finally {
                    --ts.recursion_depth;
                }
            }
        }
    }

    @Override
    public PyObject __rdivmod__(PyObject o) {
        return instance___rdivmod__(o);
    }

    /**
     * Implements the __rdivmod__ method by looking it up
     * in the instance's dictionary and calling it if it is found.
     **/
    @ExposedMethod(type = MethodType.BINARY)
    public PyObject instance___rdivmod__(PyObject o) {
        Object ctmp = __coerce_ex__(o);
        if (ctmp == null || ctmp == Py.None)
            return invoke_ex("__rdivmod__", o);
        else {
            PyObject o1 = ((PyObject[])ctmp)[0];
            PyObject o2 = ((PyObject[])ctmp)[1];
            if (this == o1) {
                // Prevent recusion if __coerce__ return self
                return invoke_ex("__rdivmod__", o2);
            }
            else {
                ThreadState ts = Py.getThreadState();
                if (ts.recursion_depth++ > ts.systemState.getrecursionlimit())
                    throw Py.RuntimeError("maximum recursion depth exceeded");
                try {
                    return o2._divmod(o1);
                } finally {
                    --ts.recursion_depth;
                }
            }
        }
    }

    @Override
    public PyObject __pow__(PyObject o) {
        return instance___pow__(o);
    }

    /**
     * Implements the __pow__ method by looking it up
     * in the instance's dictionary and calling it if it is found.
     **/
    @ExposedMethod(type = MethodType.BINARY)
    public PyObject instance___pow__(PyObject o) {
        Object ctmp = __coerce_ex__(o);
        if (ctmp == null || ctmp == Py.None)
            return invoke_ex("__pow__", o);
        else {
            PyObject o1 = ((PyObject[])ctmp)[0];
            PyObject o2 = ((PyObject[])ctmp)[1];
            if (this == o1) {
                // Prevent recusion if __coerce__ return self
                return invoke_ex("__pow__", o2);
            }
            else {
                ThreadState ts = Py.getThreadState();
                if (ts.recursion_depth++ > ts.systemState.getrecursionlimit())
                    throw Py.RuntimeError("maximum recursion depth exceeded");
                try {
                    return o1._pow(o2);
                } finally {
                    --ts.recursion_depth;
                }
            }
        }
    }

    @Override
    public PyObject __rpow__(PyObject o) {
        return instance___rpow__(o);
    }

    /**
     * Implements the __rpow__ method by looking it up
     * in the instance's dictionary and calling it if it is found.
     **/
    @ExposedMethod(type = MethodType.BINARY)
    public PyObject instance___rpow__(PyObject o) {
        Object ctmp = __coerce_ex__(o);
        if (ctmp == null || ctmp == Py.None)
            return invoke_ex("__rpow__", o);
        else {
            PyObject o1 = ((PyObject[])ctmp)[0];
            PyObject o2 = ((PyObject[])ctmp)[1];
            if (this == o1) {
                // Prevent recusion if __coerce__ return self
                return invoke_ex("__rpow__", o2);
            }
            else {
                ThreadState ts = Py.getThreadState();
                if (ts.recursion_depth++ > ts.systemState.getrecursionlimit())
                    throw Py.RuntimeError("maximum recursion depth exceeded");
                try {
                    return o2._pow(o1);
                } finally {
                    --ts.recursion_depth;
                }
            }
        }
    }

    @Override
    public PyObject __ipow__(PyObject o) {
        return instance___ipow__(o);
    }

    /**
     * Implements the __ipow__ method by looking it up
     * in the instance's dictionary and calling it if it is found.
     **/
    @ExposedMethod(type = MethodType.BINARY)
    public PyObject instance___ipow__(PyObject o) {
        PyObject ret = invoke_ex("__ipow__", o);
        if (ret != null)
            return ret;
        return super.__ipow__(o);
    }

    @Override
    public PyObject __lshift__(PyObject o) {
        return instance___lshift__(o);
    }

    /**
     * Implements the __lshift__ method by looking it up
     * in the instance's dictionary and calling it if it is found.
     **/
    @ExposedMethod(type = MethodType.BINARY)
    public PyObject instance___lshift__(PyObject o) {
        Object ctmp = __coerce_ex__(o);
        if (ctmp == null || ctmp == Py.None)
            return invoke_ex("__lshift__", o);
        else {
            PyObject o1 = ((PyObject[])ctmp)[0];
            PyObject o2 = ((PyObject[])ctmp)[1];
            if (this == o1) {
                // Prevent recusion if __coerce__ return self
                return invoke_ex("__lshift__", o2);
            }
            else {
                ThreadState ts = Py.getThreadState();
                if (ts.recursion_depth++ > ts.systemState.getrecursionlimit())
                    throw Py.RuntimeError("maximum recursion depth exceeded");
                try {
                    return o1._lshift(o2);
                } finally {
                    --ts.recursion_depth;
                }
            }
        }
    }

    @Override
    public PyObject __rlshift__(PyObject o) {
        return instance___rlshift__(o);
    }

    /**
     * Implements the __rlshift__ method by looking it up
     * in the instance's dictionary and calling it if it is found.
     **/
    @ExposedMethod(type = MethodType.BINARY)
    public PyObject instance___rlshift__(PyObject o) {
        Object ctmp = __coerce_ex__(o);
        if (ctmp == null || ctmp == Py.None)
            return invoke_ex("__rlshift__", o);
        else {
            PyObject o1 = ((PyObject[])ctmp)[0];
            PyObject o2 = ((PyObject[])ctmp)[1];
            if (this == o1) {
                // Prevent recusion if __coerce__ return self
                return invoke_ex("__rlshift__", o2);
            }
            else {
                ThreadState ts = Py.getThreadState();
                if (ts.recursion_depth++ > ts.systemState.getrecursionlimit())
                    throw Py.RuntimeError("maximum recursion depth exceeded");
                try {
                    return o2._lshift(o1);
                } finally {
                    --ts.recursion_depth;
                }
            }
        }
    }

    @Override
    public PyObject __ilshift__(PyObject o) {
        return instance___ilshift__(o);
    }

    /**
     * Implements the __ilshift__ method by looking it up
     * in the instance's dictionary and calling it if it is found.
     **/
    @ExposedMethod(type = MethodType.BINARY)
    public PyObject instance___ilshift__(PyObject o) {
        PyObject ret = invoke_ex("__ilshift__", o);
        if (ret != null)
            return ret;
        return super.__ilshift__(o);
    }

    @Override
    public PyObject __rshift__(PyObject o) {
        return instance___rshift__(o);
    }

    /**
     * Implements the __rshift__ method by looking it up
     * in the instance's dictionary and calling it if it is found.
     **/
    @ExposedMethod(type = MethodType.BINARY)
    public PyObject instance___rshift__(PyObject o) {
        Object ctmp = __coerce_ex__(o);
        if (ctmp == null || ctmp == Py.None)
            return invoke_ex("__rshift__", o);
        else {
            PyObject o1 = ((PyObject[])ctmp)[0];
            PyObject o2 = ((PyObject[])ctmp)[1];
            if (this == o1) {
                // Prevent recusion if __coerce__ return self
                return invoke_ex("__rshift__", o2);
            }
            else {
                ThreadState ts = Py.getThreadState();
                if (ts.recursion_depth++ > ts.systemState.getrecursionlimit())
                    throw Py.RuntimeError("maximum recursion depth exceeded");
                try {
                    return o1._rshift(o2);
                } finally {
                    --ts.recursion_depth;
                }
            }
        }
    }

    @Override
    public PyObject __rrshift__(PyObject o) {
        return instance___rrshift__(o);
    }

    /**
     * Implements the __rrshift__ method by looking it up
     * in the instance's dictionary and calling it if it is found.
     **/
    @ExposedMethod(type = MethodType.BINARY)
    public PyObject instance___rrshift__(PyObject o) {
        Object ctmp = __coerce_ex__(o);
        if (ctmp == null || ctmp == Py.None)
            return invoke_ex("__rrshift__", o);
        else {
            PyObject o1 = ((PyObject[])ctmp)[0];
            PyObject o2 = ((PyObject[])ctmp)[1];
            if (this == o1) {
                // Prevent recusion if __coerce__ return self
                return invoke_ex("__rrshift__", o2);
            }
            else {
                ThreadState ts = Py.getThreadState();
                if (ts.recursion_depth++ > ts.systemState.getrecursionlimit())
                    throw Py.RuntimeError("maximum recursion depth exceeded");
                try {
                    return o2._rshift(o1);
                } finally {
                    --ts.recursion_depth;
                }
            }
        }
    }

    @Override
    public PyObject __irshift__(PyObject o) {
        return instance___irshift__(o);
    }

    /**
     * Implements the __irshift__ method by looking it up
     * in the instance's dictionary and calling it if it is found.
     **/
    @ExposedMethod(type = MethodType.BINARY)
    public PyObject instance___irshift__(PyObject o) {
        PyObject ret = invoke_ex("__irshift__", o);
        if (ret != null)
            return ret;
        return super.__irshift__(o);
    }

    @Override
    public PyObject __and__(PyObject o) {
        return instance___and__(o);
    }

    /**
     * Implements the __and__ method by looking it up
     * in the instance's dictionary and calling it if it is found.
     **/
    @ExposedMethod(type = MethodType.BINARY)
    public PyObject instance___and__(PyObject o) {
        Object ctmp = __coerce_ex__(o);
        if (ctmp == null || ctmp == Py.None)
            return invoke_ex("__and__", o);
        else {
            PyObject o1 = ((PyObject[])ctmp)[0];
            PyObject o2 = ((PyObject[])ctmp)[1];
            if (this == o1) {
                // Prevent recusion if __coerce__ return self
                return invoke_ex("__and__", o2);
            }
            else {
                ThreadState ts = Py.getThreadState();
                if (ts.recursion_depth++ > ts.systemState.getrecursionlimit())
                    throw Py.RuntimeError("maximum recursion depth exceeded");
                try {
                    return o1._and(o2);
                } finally {
                    --ts.recursion_depth;
                }
            }
        }
    }

    @Override
    public PyObject __rand__(PyObject o) {
        return instance___rand__(o);
    }

    /**
     * Implements the __rand__ method by looking it up
     * in the instance's dictionary and calling it if it is found.
     **/
    @ExposedMethod(type = MethodType.BINARY)
    public PyObject instance___rand__(PyObject o) {
        Object ctmp = __coerce_ex__(o);
        if (ctmp == null || ctmp == Py.None)
            return invoke_ex("__rand__", o);
        else {
            PyObject o1 = ((PyObject[])ctmp)[0];
            PyObject o2 = ((PyObject[])ctmp)[1];
            if (this == o1) {
                // Prevent recusion if __coerce__ return self
                return invoke_ex("__rand__", o2);
            }
            else {
                ThreadState ts = Py.getThreadState();
                if (ts.recursion_depth++ > ts.systemState.getrecursionlimit())
                    throw Py.RuntimeError("maximum recursion depth exceeded");
                try {
                    return o2._and(o1);
                } finally {
                    --ts.recursion_depth;
                }
            }
        }
    }

    @Override
    public PyObject __iand__(PyObject o) {
        return instance___iand__(o);
    }

    /**
     * Implements the __iand__ method by looking it up
     * in the instance's dictionary and calling it if it is found.
     **/
    @ExposedMethod(type = MethodType.BINARY)
    public PyObject instance___iand__(PyObject o) {
        PyObject ret = invoke_ex("__iand__", o);
        if (ret != null)
            return ret;
        return super.__iand__(o);
    }

    @Override
    public PyObject __or__(PyObject o) {
        return instance___or__(o);
    }

    /**
     * Implements the __or__ method by looking it up
     * in the instance's dictionary and calling it if it is found.
     **/
    @ExposedMethod(type = MethodType.BINARY)
    public PyObject instance___or__(PyObject o) {
        Object ctmp = __coerce_ex__(o);
        if (ctmp == null || ctmp == Py.None)
            return invoke_ex("__or__", o);
        else {
            PyObject o1 = ((PyObject[])ctmp)[0];
            PyObject o2 = ((PyObject[])ctmp)[1];
            if (this == o1) {
                // Prevent recusion if __coerce__ return self
                return invoke_ex("__or__", o2);
            }
            else {
                ThreadState ts = Py.getThreadState();
                if (ts.recursion_depth++ > ts.systemState.getrecursionlimit())
                    throw Py.RuntimeError("maximum recursion depth exceeded");
                try {
                    return o1._or(o2);
                } finally {
                    --ts.recursion_depth;
                }
            }
        }
    }

    @Override
    public PyObject __ror__(PyObject o) {
        return instance___ror__(o);
    }

    /**
     * Implements the __ror__ method by looking it up
     * in the instance's dictionary and calling it if it is found.
     **/
    @ExposedMethod(type = MethodType.BINARY)
    public PyObject instance___ror__(PyObject o) {
        Object ctmp = __coerce_ex__(o);
        if (ctmp == null || ctmp == Py.None)
            return invoke_ex("__ror__", o);
        else {
            PyObject o1 = ((PyObject[])ctmp)[0];
            PyObject o2 = ((PyObject[])ctmp)[1];
            if (this == o1) {
                // Prevent recusion if __coerce__ return self
                return invoke_ex("__ror__", o2);
            }
            else {
                ThreadState ts = Py.getThreadState();
                if (ts.recursion_depth++ > ts.systemState.getrecursionlimit())
                    throw Py.RuntimeError("maximum recursion depth exceeded");
                try {
                    return o2._or(o1);
                } finally {
                    --ts.recursion_depth;
                }
            }
        }
    }

    @Override
    public PyObject __ior__(PyObject o) {
        return instance___ior__(o);
    }

    /**
     * Implements the __ior__ method by looking it up
     * in the instance's dictionary and calling it if it is found.
     **/
    @ExposedMethod(type = MethodType.BINARY)
    public PyObject instance___ior__(PyObject o) {
        PyObject ret = invoke_ex("__ior__", o);
        if (ret != null)
            return ret;
        return super.__ior__(o);
    }

    @Override
    public PyObject __xor__(PyObject o) {
        return instance___xor__(o);
    }

    /**
     * Implements the __xor__ method by looking it up
     * in the instance's dictionary and calling it if it is found.
     **/
    @ExposedMethod(type = MethodType.BINARY)
    public PyObject instance___xor__(PyObject o) {
        Object ctmp = __coerce_ex__(o);
        if (ctmp == null || ctmp == Py.None)
            return invoke_ex("__xor__", o);
        else {
            PyObject o1 = ((PyObject[])ctmp)[0];
            PyObject o2 = ((PyObject[])ctmp)[1];
            if (this == o1) {
                // Prevent recusion if __coerce__ return self
                return invoke_ex("__xor__", o2);
            }
            else {
                ThreadState ts = Py.getThreadState();
                if (ts.recursion_depth++ > ts.systemState.getrecursionlimit())
                    throw Py.RuntimeError("maximum recursion depth exceeded");
                try {
                    return o1._xor(o2);
                } finally {
                    --ts.recursion_depth;
                }
            }
        }
    }

    @Override
    public PyObject __rxor__(PyObject o) {
        return instance___rxor__(o);
    }

    /**
     * Implements the __rxor__ method by looking it up
     * in the instance's dictionary and calling it if it is found.
     **/
    @ExposedMethod(type = MethodType.BINARY)
    public PyObject instance___rxor__(PyObject o) {
        Object ctmp = __coerce_ex__(o);
        if (ctmp == null || ctmp == Py.None)
            return invoke_ex("__rxor__", o);
        else {
            PyObject o1 = ((PyObject[])ctmp)[0];
            PyObject o2 = ((PyObject[])ctmp)[1];
            if (this == o1) {
                // Prevent recusion if __coerce__ return self
                return invoke_ex("__rxor__", o2);
            }
            else {
                ThreadState ts = Py.getThreadState();
                if (ts.recursion_depth++ > ts.systemState.getrecursionlimit())
                    throw Py.RuntimeError("maximum recursion depth exceeded");
                try {
                    return o2._xor(o1);
                } finally {
                    --ts.recursion_depth;
                }
            }
        }
    }

    @Override
    public PyObject __ixor__(PyObject o) {
        return instance___ixor__(o);
    }

    /**
     * Implements the __ixor__ method by looking it up
     * in the instance's dictionary and calling it if it is found.
     **/
    @ExposedMethod(type = MethodType.BINARY)
    public PyObject instance___ixor__(PyObject o) {
        PyObject ret = invoke_ex("__ixor__", o);
        if (ret != null)
            return ret;
        return super.__ixor__(o);
    }

}
