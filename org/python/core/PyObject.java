// Copyright (c) Corporation for National Research Initiatives
package org.python.core;

/**
 * All objects known to the Jython runtime system are represented
 * by an instance of the class <code>PyObject</code> or one of
 * its subclasses.
 *
 * @author Jim Hugunin - hugunin@python.org
 * @version 1.1, 1/5/98
 * @since JPython 0.0
 **/

public class PyObject implements java.io.Serializable {
    /* xxx needed to change, we need types separated from classic behavior
     * 
     * The Python class of this object.
     * Unlike in CPython, all types have this attribute, even builtins.
     * This should only be set in the constructor, never modified otherwise.
     **/
    //public transient PyClass __class__;

    public static void typeSetup(PyObject dict,PyType.Newstyle marker) {
        dict.__setitem__(
            "__class__",
            new PyGetSetDescr("__class__", PyObject.class, "getType", null));
        class exposed___str__ extends PyBuiltinFunctionNarrow {
            private PyObject self;

            public PyObject getSelf() {
                return self;
            }
            
            exposed___str__(PyObject self, PyBuiltinFunction.Info info) {
                super(info);
                this.self = self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___str__(self, info);
            }

            public PyObject __call__() {
                return self.__repr__();
            }

            public PyObject inst_call(PyObject self) {
                return self.__repr__();
            }

        };

        dict.__setitem__(
            "__str__",
            new PyMethodDescr(
                "__str__",PyObject.class,
                0,
                0,
                new exposed___str__(null, null)));
                
        dict.__setitem__(
            "__new__", new PyNewWrapper(PyObject.class,"__new__",-1,-1) {
                public PyObject new_impl(boolean init,PyType subtype,PyObject[] args,String[] keywords) 
                { return new PyObject(); } // xxx
            });
    }

    // xxx this is likely not the final name/approach,
    // getType may become not necessary   
    private transient PyType objtype;

    public PyType getType() {
        return objtype;
    }

    // xxx
    public PyObject fastGetClass() {
        return objtype;
    }

    /* must instantiate __class__ when de-serializing */
    private void readObject(java.io.ObjectInputStream in)
        throws java.io.IOException, ClassNotFoundException {
        in.defaultReadObject();
        objtype = PyType.fromClass(getClass());
    }

    // A package private constructor used by PyJavaClass
    // xxx will need variants for PyType of PyType and still PyJavaClass of PyJavaClass
    PyObject(boolean dummy) {
        objtype = (PyType) this;
    }

    /**
     * The standard constructor for a <code>PyObject</code>.  It will set
     * the <code>__class__</code> field to correspond to the specific
     * subclass of <code>PyObject</code> being instantiated.
     **/
    public PyObject() {
        // xxx for now no such caching 
        // PyClass c = getPyClass();
        // if (c == null)
        //    c = PyJavaClass.lookup(getClass());
        objtype = PyType.fromClass(getClass());
    }

    /* xxx will be replaced.
     * This method is provided to efficiently initialize the __class__
     * attribute.  If the following boilerplate is added to a subclass of
     * PyObject, the instantiation time for the object will be greatly
     * reduced.
     *
     * <blockquote><pre>
     * // __class__ boilerplate -- see PyObject for details
     * public static PyClass __class__;
     * protected PyClass getPyClass() { return __class__; }
     * </pre></blockquote>
     *
     * With PyIntegers this leads to a 50% faster instantiation time.
     * This replaces the PyObject(PyClass c) constructor which is now
     * deprecated.
     *
    protected PyClass getPyClass() {
        return null;
    } */

    /**
     * Equivalent to the standard Python __repr__ method.  This method
     * should not typically need to be overrriden.  The easiest way to
     * configure the string representation of a <code>PyObject</code> is to
     * override the standard Java <code>toString</code> method.
     **/
    public PyString __repr__() {
        return new PyString(toString());
    }

    public String safeRepr() throws PyIgnoreMethodTag {
        if (getType() == null) {
            return "unknown object";
        }

        String name = getType().fastGetName();
        if (name == null)
            return "unknown object";

        return "'" + name + "' object";
    }

    /**
     * Equivalent to the standard Python __str__ method.  This method
     * should not typically need to be overridden.  The easiest way to
     * configure the string representation of a <code>PyObject</code> is to
     * override the standard Java <code>toString</code> method.
     **/
    public PyString __str__() {
        return __repr__();
    }

    /**
     * Equivalent to the standard Python __hash__ method.  This method can
     * not be overridden.  Instead, you should override the standard Java
     * <code>hashCode</code> method to return an appropriate hash code for
     * the <code>PyObject</code>.
     **/
    public final PyInteger __hash__() {
        return new PyInteger(hashCode());
    }

    public int hashCode() {
        return System.identityHashCode(this);
    }

    /**
     * Should almost never be overridden.
     * If overridden, it is the subclasses responsibility to ensure that
     * <code>a.equals(b) == true</code> iff <code>cmp(a,b) == 0</code>
     **/
    public boolean equals(Object ob_other) {
        return (ob_other instanceof PyObject)
            && _eq((PyObject) ob_other).__nonzero__();
    }

    /**
     * Equivalent to the standard Python __nonzero__ method.
     * Returns whether of not a given <code>PyObject</code> is
     * considered true.
     **/
    public boolean __nonzero__() {
        return true;
    }

    /**
     * Equivalent to the Jython __tojava__ method.
     * Tries to coerce this object to an instance of the requested Java class.
     * Returns the special object <code>Py.NoConversion</code>
     * if this <code>PyObject</code> can not be converted to the
     * desired Java class.
     *
     * @param c the Class to convert this <code>PyObject</code> to.
     **/
    public Object __tojava__(Class c) {
        if (c.isInstance(this))
            return this;
        return Py.NoConversion;
    }

    /**
     * The basic method to override when implementing a callable object.
     *
     * The first len(args)-len(keywords) members of args[] are plain
     * arguments.  The last len(keywords) arguments are the values of the
     * keyword arguments.
     *
     * @param args     all arguments to the function (including
     *                 keyword arguments).
     * @param keywords the keywords used for all keyword arguments.
     **/
    public PyObject __call__(PyObject args[], String keywords[]) {
        throw Py.TypeError("call of non-function (" + safeRepr() + ")");
    }

    /**
     * A variant of the __call__ method with one extra initial argument.
     * This variant is used to allow method invocations to be performed
     * efficiently.
     *
     * The default behavior is to invoke <code>__call__(args,
     * keywords)</code> with the appropriate arguments.  The only reason to
     * override this function would be for improved performance.
     *
     * @param arg1     the first argument to the function.
     * @param args     the last arguments to the function (including
     *                 keyword arguments).
     * @param keywords the keywords used for all keyword arguments.
     **/
    public PyObject __call__(
        PyObject arg1,
        PyObject args[],
        String keywords[]) {
        PyObject[] newArgs = new PyObject[args.length + 1];
        System.arraycopy(args, 0, newArgs, 1, args.length);
        newArgs[0] = arg1;
        return __call__(newArgs, keywords);
    }

    /**
     * A variant of the __call__ method when no keywords are passed.  The
     * default behavior is to invoke <code>__call__(args, keywords)</code>
     * with the appropriate arguments.  The only reason to override this
     * function would be for improved performance.
     *
     * @param args     all arguments to the function.
     **/
    public PyObject __call__(PyObject args[]) {
        return __call__(args, Py.NoKeywords);
    }

    /**
     * A variant of the __call__ method with no arguments.  The default
     * behavior is to invoke <code>__call__(args, keywords)</code> with the
     * appropriate arguments.  The only reason to override this function
     * would be for improved performance.
     **/
    public PyObject __call__() {
        return __call__(Py.EmptyObjects, Py.NoKeywords);
    }

    /**
     * A variant of the __call__ method with one argument.  The default
     * behavior is to invoke <code>__call__(args, keywords)</code> with the
     * appropriate arguments.  The only reason to override this function
     * would be for improved performance.
     *
     * @param arg0     the single argument to the function.
     **/
    public PyObject __call__(PyObject arg0) {
        return __call__(new PyObject[] { arg0 }, Py.NoKeywords);
    }

    /**
     * A variant of the __call__ method with two arguments.  The default
     * behavior is to invoke <code>__call__(args, keywords)</code> with the
     * appropriate arguments.  The only reason to override this function
     * would be for improved performance.
     *
     * @param arg0     the first argument to the function.
     * @param arg1     the second argument to the function.
     **/
    public PyObject __call__(PyObject arg0, PyObject arg1) {
        return __call__(new PyObject[] { arg0, arg1 }, Py.NoKeywords);
    }

    /**
     * A variant of the __call__ method with three arguments.  The default
     * behavior is to invoke <code>__call__(args, keywords)</code> with the
     * appropriate arguments.  The only reason to override this function
     * would be for improved performance.
     *
     * @param arg0     the first argument to the function.
     * @param arg1     the second argument to the function.
     * @param arg2     the third argument to the function.
     **/
    public PyObject __call__(PyObject arg0, PyObject arg1, PyObject arg2) {
        return __call__(new PyObject[] { arg0, arg1, arg2 }, Py.NoKeywords);
    }

    /**
     * A variant of the __call__ method with four arguments.  The default
     * behavior is to invoke <code>__call__(args, keywords)</code> with the
     * appropriate arguments.  The only reason to override this function
     * would be for improved performance.
     *
     * @param arg0     the first argument to the function.
     * @param arg1     the second argument to the function.
     * @param arg2     the third argument to the function.
     * @param arg3     the fourth argument to the function.
     **/
    public PyObject __call__(
        PyObject arg0,
        PyObject arg1,
        PyObject arg2,
        PyObject arg3) {
        return __call__(
            new PyObject[] { arg0, arg1, arg2, arg3 },
            Py.NoKeywords);
    }

    /** @deprecated **/
    public PyObject _callextra( // xxx fix, should work with iterators too
        PyObject[] args,
        String[] keywords,
        PyObject starargs,
        PyObject kwargs) {

        int argslen = args.length;
        int nstar = 0;

        String name = "";
        if (this instanceof PyFunction)
            name = ((PyFunction) this).__name__ + "() ";

        if (kwargs != null) {
            PyObject keys = kwargs.__findattr__("keys");
            if (keys == null)
                throw Py.TypeError(
                    name + "argument after ** must be " + "a dictionary");
            for (int i = 0; i < keywords.length; i++)
                if (kwargs.__finditem__(keywords[i]) != null)
                    throw Py.TypeError(
                        name
                            + "got multiple values for "
                            + "keyword argument '"
                            + keywords[i]
                            + "'");
            argslen += kwargs.__len__();
        }
        if (starargs != null) {
            if (!(starargs instanceof PySequence
                || starargs instanceof PyInstance)) // xxx
                throw Py.TypeError(
                    name + "argument after * must " + "be a sequence");
            nstar = starargs.__len__();
            argslen += nstar;
        }

        PyObject[] newargs = new PyObject[argslen];
        int argidx = args.length - keywords.length;
        System.arraycopy(args, 0, newargs, 0, argidx);

        if (starargs != null) {
            PyObject a;
            for (int i = 0;
                (a = starargs.__finditem__(i)) != null && i < nstar;
                i++) {
                newargs[argidx++] = a;
            }
        }
        System.arraycopy(
            args,
            args.length - keywords.length,
            newargs,
            argidx,
            keywords.length);
        argidx += keywords.length;

        if (kwargs != null) {
            String[] newkeywords =
                new String[keywords.length + kwargs.__len__()];
            System.arraycopy(keywords, 0, newkeywords, 0, keywords.length);

            PyObject keys = kwargs.invoke("keys");
            PyObject key;
            for (int i = 0;(key = keys.__finditem__(i)) != null; i++) {
                if (!(key instanceof PyString))
                    throw Py.TypeError(name + "keywords must be strings");
                newkeywords[keywords.length + i] =
                    ((PyString) key).internedString();
                newargs[argidx++] = kwargs.__finditem__(key);
            }
            keywords = newkeywords;
        }

        if (newargs.length != argidx) {
            args = new PyObject[argidx];
            System.arraycopy(newargs, 0, args, 0, argidx);
        } else
            args = newargs;
        return __call__(args, keywords);
    }

    /* xxx fix these around */

    public boolean isCallable() {
        return __findattr__("__call__") != null;
    }
    public boolean isMappingType() {
        return true;
    }
    public boolean isNumberType() {
        return true;
    }
    public boolean isSequenceType() {
        return true;
    }

    /* . */

    /* The basic functions to implement a mapping */

    /**
     * Equivalent to the standard Python __len__ method.
     * Part of the mapping discipline.
     *
     * @return the length of the object
     **/
    public int __len__() {
        throw Py.AttributeError("__len__");
    }

    /**
     * Very similar to the standard Python __getitem__ method.
     * Instead of throwing a KeyError if the item isn't found,
     * this just returns null.
     *
     * Classes that wish to implement __getitem__ should
     * override this method instead (with the appropriate
     * semantics.
     *
     * @param key the key to lookup in this container
     *
     * @return the value corresponding to key or null if key is not found
     **/
    public PyObject __finditem__(PyObject key) {
        throw Py.AttributeError("__getitem__");
    }

    /**
     * A variant of the __finditem__ method which accepts a primitive
     * <code>int</code> as the key.  By default, this method will call
     * <code>__finditem__(PyObject key)</code> with the appropriate args.
     * The only reason to override this method is for performance.
     *
     * @param key the key to lookup in this sequence.
     * @return the value corresponding to key or null if key is not found.
     *
     * @see #__finditem__(PyObject)
     **/
    public PyObject __finditem__(int key) {
        return __finditem__(new PyInteger(key));
    }

    /**
     * A variant of the __finditem__ method which accepts a Java
     * <code>String</code> as the key.  By default, this method will call
     * <code>__finditem__(PyObject key)</code> with the appropriate args.
     * The only reason to override this method is for performance.
     *
     * <b>Warning: key must be an interned string!!!!!!!!</b>
     *
     * @param key the key to lookup in this sequence -
     *            <b> must be an interned string </b>.
     * @return the value corresponding to key or null if key is not found.
     *
     * @see #__finditem__(PyObject)
     **/
    public PyObject __finditem__(String key) {
        return __finditem__(new PyString(key));
    }

    /**
     * Equivalent to the standard Python __getitem__ method.
     * This variant takes a primitive <code>int</code> as the key.
     * This method should not be overridden.
     * Override the <code>__finditem__</code> method instead.
     *
     * @param key the key to lookup in this container.
     * @return the value corresponding to that key.
     * @exception PyKeyError if the key is not found.
     *
     * @see #__finditem__(int)
     **/
    public PyObject __getitem__(int key) {
        PyObject ret = __finditem__(key);
        if (ret == null)
            throw Py.KeyError("" + key);
        return ret;
    }

    /**
     * Equivalent to the standard Python __getitem__ method.
     * This method should not be overridden.
     * Override the <code>__finditem__</code> method instead.
     *
     * @param key the key to lookup in this container.
     * @return the value corresponding to that key.
     * @exception PyKeyError if the key is not found.
     *
     * @see #__finditem__(PyObject)
     **/
    public PyObject __getitem__(PyObject key) {
        PyObject ret = __finditem__(key);
        if (ret == null)
            throw Py.KeyError(key.toString());
        return ret;
    }

    /**
     * Equivalent to the standard Python __setitem__ method.
     *
     * @param key the key whose value will be set
     * @param value the value to set this key to
     **/
    public void __setitem__(PyObject key, PyObject value) {
        throw Py.AttributeError("__setitem__");
    }

    /**
     * A variant of the __setitem__ method which accepts a String
     * as the key.  <b>This String must be interned</b>.
     * By default, this will call
     * <code>__setitem__(PyObject key, PyObject value)</code>
     * with the appropriate args.
     * The only reason to override this method is for performance.
     *
     * @param key the key whose value will be set -
     *            <b> must be an interned string </b>.
     * @param value the value to set this key to
     *
     * @see #__setitem__(PyObject, PyObject)
     **/
    public void __setitem__(String key, PyObject value) {
        __setitem__(new PyString(key), value);
    }

    /**
     * A variant of the __setitem__ method which accepts a primitive
     * <code>int</code> as the key.
     * By default, this will call
     * <code>__setitem__(PyObject key, PyObject value)</code>
     * with the appropriate args.
     * The only reason to override this method is for performance.
     *
     * @param key the key whose value will be set
     * @param value the value to set this key to
     *
     * @see #__setitem__(PyObject, PyObject)
     **/
    public void __setitem__(int key, PyObject value) {
        __setitem__(new PyInteger(key), value);
    }

    /**
     * Equivalent to the standard Python __delitem__ method.
     *
     * @param key the key to be removed from the container
     * @exception PyKeyError if the key is not found in the container
     **/
    public void __delitem__(PyObject key) {
        throw Py.AttributeError("__delitem__");
    }

    /**
     * A variant of the __delitem__ method which accepts a String
     * as the key.  <b>This String must be interned</b>.
     * By default, this will call
     * <code>__delitem__(PyObject key)</code>
     * with the appropriate args.
     * The only reason to override this method is for performance.
     *
     * @param key the key who will be removed -
     *            <b> must be an interned string </b>.
     * @exception PyKeyError if the key is not found in the container
     *
     * @see #__delitem__(PyObject)
     **/
    public void __delitem__(String key) {
        __delitem__(new PyString(key));
    }

    public PyObject __getslice__(
        PyObject s_start,
        PyObject s_stop,
        PyObject s_step) {
        PySlice s = new PySlice(s_start, s_stop, s_step);
        return __getitem__(s);
    }

    public void __setslice__(
        PyObject s_start,
        PyObject s_stop,
        PyObject s_step,
        PyObject value) {
        PySlice s = new PySlice(s_start, s_stop, s_step);
        __setitem__(s, value);
    }

    public void __delslice__(
        PyObject s_start,
        PyObject s_stop,
        PyObject s_step) {
        PySlice s = new PySlice(s_start, s_stop, s_step);
        __delitem__(s);
    }

    public PyObject __getslice__(PyObject start, PyObject stop) {
        return __getslice__(start, stop, Py.One);
    }

    public void __setslice__(PyObject start, PyObject stop, PyObject value) {
        __setslice__(start, stop, Py.One, value);
    }

    public void __delslice__(PyObject start, PyObject stop) {
        __delslice__(start, stop, Py.One);
    }

    /*The basic functions to implement an iterator */

    /**
     * Return an iterator that is used to iterate the element of this
     * sequence.
     * From version 2.2, this method is the primary protocol for looping
     * over sequences.
     * <p>
     * If a PyObject subclass should support iteration based in the
     * __finditem__() method, it must supply an implementation of __iter__()
     * like this:
     * <pre>
     *    public PyObject __iter__() {
     *        return new PySequenceIter(this);
     *    }
     * </pre>
     *
     * When iterating over a python sequence from java code, it should be
     * done with code like this:
     * <pre>
     *    PyObject iter = seq.__iter__();
     *    for (PyObject item; (item = iter.__next__()) != null;  {
     *        // Do somting with item
     *    }
     * </pre>
     *
     * @since 2.2
     */
    public PyObject __iter__() {
        throw Py.TypeError("iteration over non-sequence");
    }

    /**
     * Return the next element of the sequence that this is an iterator
     * for. Returns null when the end of the sequence is reached.
     *
     * @since 2.2
     */
    public PyObject __iternext__() {
        return null;
    }

    /*The basic functions to implement a namespace*/

    /**
     * Very similar to the standard Python __getattr__ method.
     * Instead of throwing a AttributeError if the item isn't found,
     * this just returns null.
     *
     * Classes that wish to implement __getattr__ should
     * override this method instead (with the appropriate
     * semantics.
     *
     * @param name the name to lookup in this namespace
     *
     * @return the value corresponding to name or null if name is not found
     **/
    public final PyObject __findattr__(PyString name) {
        if (name == null) {
            return null;
        }
        return __findattr__(name.internedString());
    }

    /**
     * A variant of the __findattr__ method which accepts a Java
     * <code>String</code> as the name.
     *
     * By default, this method will call <code>__findattr__(PyString
     * name)</code> with the appropriate args.  The only reason to override
     * this method is for performance.
     *
     * <b>Warning: name must be an interned string!!!!!!!!</b>
     *
     * @param name the name to lookup in this namespace
     * <b> must be an interned string </b>.
     * @return the value corresponding to name or null if name is not found
     *
     * @see #__findattr__(PyString)
     **/
    public PyObject __findattr__(String name) { // xxx accelerators/ expose
        /*if (getType() == null)
            return null;
        if (name == "__class__")
            return getType();*/
        /*PyObject ret = getType().lookup(name, false);
        if (ret != null)
            return ret._doget(this);
        return null;*/

        return object__findattr__(name);
    }

    /**
     * Equivalent to the standard Python __getattr__ method.
     * This method can not be overridden.
     * Override the <code>__findattr__</code> method instead.
     *
     * @param name the name to lookup in this namespace
     * @return the value corresponding to name
     * @exception PyAttributeError if the name is not found.
     *
     * @see #__findattr__(PyString)
     **/
    public final PyObject __getattr__(PyString name) {
        PyObject ret = __findattr__(name);
        if (ret == null)
            no_attribute(name.toString());
        return ret;
    }

    /**
     * A variant of the __getattr__ method which accepts a Java
     * <code>String</code> as the name.
     * This method can not be overridden.
     * Override the <code>__findattr__</code> method instead.
     *
     * <b>Warning: name must be an interned string!!!!!!!!</b>
     *
     * @param name the name to lookup in this namespace
     *             <b> must be an interned string </b>.
     * @return the value corresponding to name
     * @exception PyAttributeError if the name is not found.
     *
     * @see #__findattr__(java.lang.String)
     **/
    public final PyObject __getattr__(String name) {
        PyObject ret = __findattr__(name);
        if (ret == null)
            no_attribute(name);
        return ret;
    }

    private void no_attribute(String name) {
        throw Py.AttributeError(
            safeRepr() + " has no attribute '" + name + "'");
    }

    private void readonly_attribute(String name) {
        throw Py.AttributeError(
            safeRepr() + " attribute '" + name + "' is read-only");
    }

    /**
     * Equivalent to the standard Python __setattr__ method.
     * This method can not be overridden.
     *
     * @param name the name to lookup in this namespace
     * @return the value corresponding to name
     * @exception PyAttributeError if the name is not found.
     *
     * @see #__setattr__(java.lang.String, PyObject)
     **/
    public final void __setattr__(PyString name, PyObject value) {
        __setattr__(name.internedString(), value);
    }

    /**
     * A variant of the __setattr__ method which accepts a String
     * as the key.  <b>This String must be interned</b>.
     *
     * @param name  the name whose value will be set -
     *              <b> must be an interned string </b>.
     * @param value the value to set this name to
     *
     * @see #__setattr__(PyString, PyObject)
    **/
    public void __setattr__(String name, PyObject value) {
        object__setattr__(name, value);
    }

    /**
     * Equivalent to the standard Python __delattr__ method.
     * This method can not be overridden.
     *
     * @param name the name to which will be removed
     * @exception PyAttributeError if the name doesn't exist
     *
     * @see #__delattr__(java.lang.String)
     **/
    public final void __delattr__(PyString name) {
        __delattr__(name.internedString());
    }

    /**
     * A variant of the __delattr__ method which accepts a String
     * as the key.  <b>This String must be interned</b>.
     * By default, this will call
     * <code>__delattr__(PyString name)</code>
     * with the appropriate args.
     * The only reason to override this method is for performance.
     *
     * @param name the name which will be removed -
     *             <b> must be an interned string </b>.
     * @exception PyAttributeError if the name doesn't exist
     *
     * @see #__delattr__(PyString)
     **/
    public void __delattr__(String name) {
        object__delattr__(name);
    }

    // Used by import logic.
    protected PyObject impAttr(String name) {
        return __findattr__(name);
    }

    protected void addKeys(PyList ret, String attr) {
        PyObject obj = __findattr__(attr);
        if (obj == null)
            return;
        if (obj instanceof PyDictionary) {
            ret.setslice(
                ret.__len__(),
                ret.__len__(),
                1,
                ((PyDictionary) obj).keys());
        } else if (obj instanceof PyStringMap) {
            ret.setslice(
                ret.__len__(),
                ret.__len__(),
                1,
                ((PyStringMap) obj).keys());
        } else if (obj instanceof PyList) {
            ret.setslice(ret.__len__(), ret.__len__(), 1, (PyList) obj);
        }
    }

    /**
     * Equivalent to the standard Python __dir__ method.
     *
     * @return a list of names defined by this object.
     **/
    public PyObject __dir__() {
        PyList ret = new PyList();

        addKeys(ret, "__dict__");
        addKeys(ret, "__methods__");
        addKeys(ret, "__members__");

        ret.sort();
        return ret;
    }

    public PyObject _doget(PyObject container) {
        return this;
    }

    public PyObject _doget(PyObject container, PyObject wherefound) {
        return _doget(container);
    }

    public boolean _doset(PyObject container, PyObject value) {
        return false;
    }

    public boolean _dodel(PyObject container) {
        return false;
    }

    /* Numeric coercion */

    /**
     * Implements numeric coercion
     *
     * @param o the other object involved in the coercion
     * @return null if no coercion is possible;
     * a single PyObject to use to replace o if this is unchanged;
     * or a PyObject[2] consisting of replacements for this and o.
     **/
    public Object __coerce_ex__(PyObject o) {
        return null;
    }

    /**
     * Equivalent to the standard Python __coerce__ method.
     *
     * This method can not be overridden.
     * To implement __coerce__ functionality, override __coerce_ex__ instead.
     *
     * @param pyo the other object involved in the coercion.
     * @return a tuple of this object and pyo coerced to the same type
     *         or Py.None if no coercion is possible.
     * @see org.python.core.PyObject#__coerce_ex__(org.python.core.PyObject)
     **/
    public final PyObject __coerce__(PyObject pyo) {
        Object o = __coerce_ex__(pyo);
        if (o == null)
            throw Py.AttributeError("__coerce__");
        if (o == Py.None)
            return (PyObject) o;
        if (o instanceof PyObject[])
            return new PyTuple((PyObject[]) o);
        else
            return new PyTuple(new PyObject[] { this, (PyObject) o });
    }

    /* The basic comparision operations */

    /**
     * Equivalent to the standard Python __cmp__ method.
     *
     * @param other the object to compare this with.
     * @return -1 if this < 0; 0 if this == o; +1 if this > o; -2 if no
     * comparison is implemented
     **/
    public int __cmp__(PyObject other) {
        return -2;
    }

    /**
     * Equivalent to the standard Python __eq__ method.
     *
     * @param other the object to compare this with.
     * @return the result of the comparison.
     **/
    public PyObject __eq__(PyObject other) {
        return null;
    }

    /**
     * Equivalent to the standard Python __ne__ method.
     *
     * @param other the object to compare this with.
     * @return the result of the comparison.
     **/
    public PyObject __ne__(PyObject other) {
        return null;
    }

    /**
     * Equivalent to the standard Python __le__ method.
     *
     * @param other the object to compare this with.
     * @return the result of the comparison.
     **/
    public PyObject __le__(PyObject other) {
        return null;
    }

    /**
     * Equivalent to the standard Python __lt__ method.
     *
     * @param other the object to compare this with.
     * @return the result of the comparison.
     **/
    public PyObject __lt__(PyObject other) {
        return null;
    }

    /**
     * Equivalent to the standard Python __ge__ method.
     *
     * @param other the object to compare this with.
     * @return the result of the comparison.
     **/
    public PyObject __ge__(PyObject other) {
        return null;
    }

    /**
     * Equivalent to the standard Python __gt__ method.
     *
     * @param other the object to compare this with.
     * @return the result of the comparison.
     **/
    public PyObject __gt__(PyObject other) {
        return null;
    }

    /**
     * Implements cmp(this, other)
     *
     * @param other the object to compare this with.
     * @return -1 if this < 0; 0 if this == o; +1 if this > o
     **/
    public final int _cmp(PyObject o) {
        PyObject token = null;
        ThreadState ts = Py.getThreadState();
        try {
            if (++ts.compareStateNesting > 500) {
                if ((token = check_recursion(ts, this, o)) == null)
                    return 0;
            }

            PyObject r;
            r = __eq__(o);
            if (r != null && r.__nonzero__())
                return 0;
            r = o.__eq__(this);
            if (r != null && r.__nonzero__())
                return 0;

            r = __lt__(o);
            if (r != null && r.__nonzero__())
                return -1;
            r = o.__gt__(this);
            if (r != null && r.__nonzero__())
                return -1;

            r = __gt__(o);
            if (r != null && r.__nonzero__())
                return 1;
            r = o.__lt__(this);
            if (r != null && r.__nonzero__())
                return 1;

            return _cmp_unsafe(o);
        } finally {
            delete_token(ts, token);
            ts.compareStateNesting--;
        }
    }

    private PyObject make_pair(PyObject o) {
        if (System.identityHashCode(this) < System.identityHashCode(o))
            return new PyIdentityTuple(new PyObject[] { this, o });
        else
            return new PyIdentityTuple(new PyObject[] { o, this });
    }

    private final int _cmp_unsafe(PyObject o2_in) {
        // Shortcut for equal objects
        if (this == o2_in)
            return 0;

        PyObject o2 = o2_in;
        PyObject o1 = this;
        int itmp;
        Object ctmp;
        if (o1.getType() != o2.getType()) {
            ctmp = o1.__coerce_ex__(o2);
            if (ctmp != null) {
                if (ctmp instanceof PyObject[]) {
                    o1 = ((PyObject[]) ctmp)[0];
                    o2 = ((PyObject[]) ctmp)[1];
                } else {
                    o2 = (PyObject) ctmp;
                }
            }
        } else
            ctmp = null;
        if (ctmp != Py.None && (itmp = o1.__cmp__(o2)) != -2)
            return itmp;

        o1 = this;
        o2 = o2_in;
        if (o1.getType() != o2.getType()) {
            ctmp = o2.__coerce_ex__(o1);
            if (ctmp != null) {
                if (ctmp instanceof PyObject[]) {
                    o2 = ((PyObject[]) ctmp)[0];
                    o1 = ((PyObject[]) ctmp)[1];
                } else {
                    o1 = (PyObject) ctmp;
                }
            }
        }
        if (ctmp != Py.None && (itmp = o2.__cmp__(o1)) != -2)
            return -itmp;

        if (this == o2_in)
            return 0;

        /* None is smaller than anything */
        if (this == Py.None)
            return -1;
        if (o2_in == Py.None)
            return 1;

        // No rational way to compare these, so ask their classes to compare
        itmp = this.getType().__cmp__(o2_in.getType());

        if (itmp == 0)
            return System.identityHashCode(this)
                < System.identityHashCode(o2_in)
                ? -1
                : 1;
        if (itmp != -2)
            return itmp;
        return System.identityHashCode(this.getType())
            < System.identityHashCode(o2_in.getType())
            ? -1
            : 1;
    }

    private final static PyObject check_recursion(
        ThreadState ts,
        PyObject o1,
        PyObject o2) {
        PyDictionary stateDict = ts.getCompareStateDict();

        PyObject pair = o1.make_pair(o2);

        if (stateDict.__finditem__(pair) != null)
            return null;

        stateDict.__setitem__(pair, pair);
        return pair;
    }

    private final static void delete_token(ThreadState ts, PyObject token) {
        if (token == null)
            return;
        PyDictionary stateDict = ts.getCompareStateDict();

        stateDict.__delitem__(token);
    }

    /**
     * Implements the Python expression <code>this == other</code>.
     *
     * @param other the object to compare this with.
     * @return the result of the comparison
     **/
    public final PyObject _eq(PyObject o) {
        PyObject token = null;

        ThreadState ts = Py.getThreadState();
        try {
            if (++ts.compareStateNesting > 10) {
                if ((token = check_recursion(ts, this, o)) == null)
                    return Py.One;
            }
            PyObject res = __eq__(o);
            if (res != null)
                return res;
            res = o.__eq__(this);
            if (res != null)
                return res;
            return _cmp_unsafe(o) == 0 ? Py.One : Py.Zero;
        } finally {
            delete_token(ts, token);
            ts.compareStateNesting--;
        }
    }

    /**
     * Implements the Python expression <code>this != other</code>.
     *
     * @param other the object to compare this with.
     * @return the result of the comparison
     **/
    public final PyObject _ne(PyObject o) {
        PyObject token = null;

        ThreadState ts = Py.getThreadState();
        try {
            if (++ts.compareStateNesting > 10) {
                if ((token = check_recursion(ts, this, o)) == null)
                    return Py.Zero;
            }
            PyObject res = __ne__(o);
            if (res != null)
                return res;
            res = o.__ne__(this);
            if (res != null)
                return res;
            return _cmp_unsafe(o) != 0 ? Py.One : Py.Zero;
        } finally {
            delete_token(ts, token);
            ts.compareStateNesting--;
        }
    }

    /**
     * Implements the Python expression <code>this &lt;= other</code>.
     *
     * @param other the object to compare this with.
     * @return the result of the comparison
     **/
    public final PyObject _le(PyObject o) {
        PyObject token = null;

        ThreadState ts = Py.getThreadState();
        try {
            if (++ts.compareStateNesting > 10) {
                if ((token = check_recursion(ts, this, o)) == null)
                    throw Py.ValueError("can't order recursive values");
            }
            PyObject res = __le__(o);
            if (res != null)
                return res;
            res = o.__ge__(this);
            if (res != null)
                return res;
            return _cmp_unsafe(o) <= 0 ? Py.One : Py.Zero;
        } finally {
            delete_token(ts, token);
            ts.compareStateNesting--;
        }
    }

    /**
     * Implements the Python expression <code>this &lt; other</code>.
     *
     * @param other the object to compare this with.
     * @return the result of the comparison
     **/
    public final PyObject _lt(PyObject o) {
        PyObject token = null;

        ThreadState ts = Py.getThreadState();
        try {
            if (++ts.compareStateNesting > 10) {
                if ((token = check_recursion(ts, this, o)) == null)
                    throw Py.ValueError("can't order recursive values");
            }
            PyObject res = __lt__(o);
            if (res != null)
                return res;
            res = o.__gt__(this);
            if (res != null)
                return res;
            return _cmp_unsafe(o) < 0 ? Py.One : Py.Zero;
        } finally {
            delete_token(ts, token);
            ts.compareStateNesting--;
        }
    }

    /**
     * Implements the Python expression <code>this &gt;= other</code>.
     *
     * @param other the object to compare this with.
     * @return the result of the comparison
     **/
    public final PyObject _ge(PyObject o) {
        PyObject token = null;

        ThreadState ts = Py.getThreadState();
        try {
            if (++ts.compareStateNesting > 10) {
                if ((token = check_recursion(ts, this, o)) == null)
                    throw Py.ValueError("can't order recursive values");
            }
            PyObject res = __ge__(o);
            if (res != null)
                return res;
            res = o.__le__(this);
            if (res != null)
                return res;
            return _cmp_unsafe(o) >= 0 ? Py.One : Py.Zero;
        } finally {
            delete_token(ts, token);
            ts.compareStateNesting--;
        }
    }

    /**
     * Implements the Python expression <code>this &gt; other</code>.
     *
     * @param other the object to compare this with.
     * @return the result of the comparison
     **/
    public final PyObject _gt(PyObject o) {
        PyObject token = null;

        ThreadState ts = Py.getThreadState();
        try {
            if (++ts.compareStateNesting > 10) {
                if ((token = check_recursion(ts, this, o)) == null)
                    throw Py.ValueError("can't order recursive values");
            }
            PyObject res = __gt__(o);
            if (res != null)
                return res;
            res = o.__lt__(this);
            if (res != null)
                return res;
            return _cmp_unsafe(o) > 0 ? Py.One : Py.Zero;
        } finally {
            delete_token(ts, token);
            ts.compareStateNesting--;
        }

    }

    /**
     * Implements <code>is</code> operator.
     *
     * @param other the object to compare this with.
     * @return the result of the comparison
     **/
    public PyObject _is(PyObject o) {
        return this == o ? Py.One : Py.Zero;
    }

    /**
     * Implements <code>is not</code> operator.
     *
     * @param other the object to compare this with.
     * @return the result of the comparison
     **/
    public PyObject _isnot(PyObject o) {
        return this != o ? Py.One : Py.Zero;
    }

    /**
     * Implements <code>in</code> operator.
     *
     * @param other the container to search for this element.
     * @return the result of the search.
     **/
    public final PyObject _in(PyObject o) {
        return Py.newBoolean(o.__contains__(this));
    }

    /**
     * Implements <code>not in</code> operator.
     *
     * @param other the container to search for this element.
     * @return the result of the search.
     **/
    public final PyObject _notin(PyObject o) {
        return Py.newBoolean(!o.__contains__(this));
    }

    /**
     * Equivalent to the standard Python __contains__ method.
     *
     * @param o the element to search for in this container.
     * @return the result of the search.
     **/
    public boolean __contains__(PyObject o) {
        PyObject iter = __iter__();
        for (PyObject item = null;(item = iter.__iternext__()) != null;) {
            if (o._eq(item).__nonzero__())
                return true;
        }
        return false;
    }

    /**
     * Implements boolean not
     *
     * @return not this.
     **/
    public PyObject __not__() {
        return __nonzero__() ? Py.Zero : Py.One;
    }

    /* The basic numeric operations */

    /**
     * Equivalent to the standard Python __hex__ method
     * Should only be overridden by numeric objects that can be
     * reasonably represented as a hexadecimal string.
     *
     * @return a string representing this object as a hexadecimal number.
     **/
    public PyString __hex__() {
        throw Py.AttributeError("__hex__");
    }

    /**
     * Equivalent to the standard Python __oct__ method.
     * Should only be overridden by numeric objects that can be
     * reasonably represented as an octal string.
     *
     * @return a string representing this object as an octal number.
     **/
    public PyString __oct__() {
        throw Py.AttributeError("__oct__");
    }

    /**
     * Equivalent to the standard Python __int__ method.
     * Should only be overridden by numeric objects that can be
     * reasonably coerced into an integer.
     *
     * @return an integer corresponding to the value of this object.
     **/
    public PyInteger __int__() {
        throw Py.AttributeError("__int__");
    }

    /**
     * Equivalent to the standard Python __long__ method.
     * Should only be overridden by numeric objects that can be
     * reasonably coerced into a python long.
     *
     * @return a PyLong corresponding to the value of this object.
     **/
    public PyLong __long__() {
        throw Py.AttributeError("__long__");
    }

    /**
     * Equivalent to the standard Python __float__ method.
     * Should only be overridden by numeric objects that can be
     * reasonably coerced into a python float.
     *
     * @return a float corresponding to the value of this object.
     **/
    public PyFloat __float__() {
        throw Py.AttributeError("__float__");
    }

    /**
     * Equivalent to the standard Python __complex__ method.
     * Should only be overridden by numeric objects that can be
     * reasonably coerced into a python complex number.
     *
     * @return a complex number corresponding to the value of this object.
     **/
    public PyComplex __complex__() {
        throw Py.AttributeError("__complex__");
    }

    /**
     * Equivalent to the standard Python __pos__ method.
     *
     * @return +this.
     **/
    public PyObject __pos__() {
        throw Py.AttributeError("__pos__");
    }

    /**
     * Equivalent to the standard Python __neg__ method.
     *
     * @return -this.
     **/
    public PyObject __neg__() {
        throw Py.AttributeError("__neg__");
    }

    /**
     * Equivalent to the standard Python __abs__ method.
     *
     * @return abs(this).
     **/
    public PyObject __abs__() {
        throw Py.AttributeError("__abs__");
    }

    /**
     * Equivalent to the standard Python __invert__ method.
     *
     * @return ~this.
     **/
    public PyObject __invert__() {
        throw Py.AttributeError("__invert__");
    }

    /**
     * Implements the three argument power function.
     *
     * @param o2 the power to raise this number to.
     * @param o3 the modulus to perform this operation in or null if no
     *           modulo is to be used
     * @return this object raised to the given power in the given modulus
     **/
    public PyObject __pow__(PyObject o2, PyObject o3) {
        return null;
    }

    // Generated by make_binops.py (Begin)

    /**
     * Equivalent to the standard Python __add__ method
     * @param     other the object to perform this binary operation with
     *            (the right-hand operand).
     * @return    the result of the add, or null if this operation
     *            is not defined
     **/
    public PyObject __add__(PyObject other) {
        return null;
    }

    /**
     * Equivalent to the standard Python __radd__ method
     * @param     other the object to perform this binary operation with
     *            (the left-hand operand).
     * @return    the result of the add, or null if this operation
     *            is not defined.
     **/
    public PyObject __radd__(PyObject other) {
        return null;
    }

    /**
     * Equivalent to the standard Python __iadd__ method
     * @param     other the object to perform this binary operation with
     *            (the right-hand operand).
     * @return    the result of the add, or null if this operation
     *            is not defined
     **/
    public PyObject __iadd__(PyObject other) {
        return _add(other);
    }

    /**
     * Implements the Python expression <code>this + other</code>
     * @param     other the object to perform this binary operation with.
     * @return    the result of the add.
     * @exception PyTypeError if this operation can't be performed
     *            with these operands.
     **/
    public final PyObject _add(PyObject o2) {
        PyObject x = __add__(o2);
        if (x != null)
            return x;
        x = o2.__radd__(this);
        if (x != null)
            return x;
        throw Py.TypeError("__add__ nor __radd__ defined for these operands");
    }

    /**
     * Equivalent to the standard Python __sub__ method
     * @param     other the object to perform this binary operation with
     *            (the right-hand operand).
     * @return    the result of the sub, or null if this operation
     *            is not defined
     **/
    public PyObject __sub__(PyObject other) {
        return null;
    }

    /**
     * Equivalent to the standard Python __rsub__ method
     * @param     other the object to perform this binary operation with
     *            (the left-hand operand).
     * @return    the result of the sub, or null if this operation
     *            is not defined.
     **/
    public PyObject __rsub__(PyObject other) {
        return null;
    }

    /**
     * Equivalent to the standard Python __isub__ method
     * @param     other the object to perform this binary operation with
     *            (the right-hand operand).
     * @return    the result of the sub, or null if this operation
     *            is not defined
     **/
    public PyObject __isub__(PyObject other) {
        return _sub(other);
    }

    /**
     * Implements the Python expression <code>this - other</code>
     * @param     other the object to perform this binary operation with.
     * @return    the result of the sub.
     * @exception PyTypeError if this operation can't be performed
     *            with these operands.
     **/
    public final PyObject _sub(PyObject o2) {
        PyObject x = __sub__(o2);
        if (x != null)
            return x;
        x = o2.__rsub__(this);
        if (x != null)
            return x;
        throw Py.TypeError("__sub__ nor __rsub__ defined for these operands");
    }

    /**
     * Equivalent to the standard Python __mul__ method
     * @param     other the object to perform this binary operation with
     *            (the right-hand operand).
     * @return    the result of the mul, or null if this operation
     *            is not defined
     **/
    public PyObject __mul__(PyObject other) {
        return null;
    }

    /**
     * Equivalent to the standard Python __rmul__ method
     * @param     other the object to perform this binary operation with
     *            (the left-hand operand).
     * @return    the result of the mul, or null if this operation
     *            is not defined.
     **/
    public PyObject __rmul__(PyObject other) {
        return null;
    }

    /**
     * Equivalent to the standard Python __imul__ method
     * @param     other the object to perform this binary operation with
     *            (the right-hand operand).
     * @return    the result of the mul, or null if this operation
     *            is not defined
     **/
    public PyObject __imul__(PyObject other) {
        return _mul(other);
    }

    /**
     * Implements the Python expression <code>this * other</code>
     * @param     other the object to perform this binary operation with.
     * @return    the result of the mul.
     * @exception PyTypeError if this operation can't be performed
     *            with these operands.
     **/
    public final PyObject _mul(PyObject o2) {
        PyObject x = __mul__(o2);
        if (x != null)
            return x;
        x = o2.__rmul__(this);
        if (x != null)
            return x;
        throw Py.TypeError("__mul__ nor __rmul__ defined for these operands");
    }

    /**
     * Equivalent to the standard Python __div__ method
     * @param     other the object to perform this binary operation with
     *            (the right-hand operand).
     * @return    the result of the div, or null if this operation
     *            is not defined
     **/
    public PyObject __div__(PyObject other) {
        return null;
    }

    /**
     * Equivalent to the standard Python __rdiv__ method
     * @param     other the object to perform this binary operation with
     *            (the left-hand operand).
     * @return    the result of the div, or null if this operation
     *            is not defined.
     **/
    public PyObject __rdiv__(PyObject other) {
        return null;
    }

    /**
     * Equivalent to the standard Python __idiv__ method
     * @param     other the object to perform this binary operation with
     *            (the right-hand operand).
     * @return    the result of the div, or null if this operation
     *            is not defined
     **/
    public PyObject __idiv__(PyObject other) {
        return _div(other);
    }

    /**
     * Implements the Python expression <code>this / other</code>
     * @param     other the object to perform this binary operation with.
     * @return    the result of the div.
     * @exception PyTypeError if this operation can't be performed
     *            with these operands.
     **/
    public final PyObject _div(PyObject o2) {
        if (Options.Qnew)
            return _truediv(o2);
        PyObject x = __div__(o2);
        if (x != null)
            return x;
        x = o2.__rdiv__(this);
        if (x != null)
            return x;
        throw Py.TypeError("__div__ nor __rdiv__ defined for these operands");
    }

    /**
     * Equivalent to the standard Python __floordiv__ method
     * @param     other the object to perform this binary operation with
     *            (the right-hand operand).
     * @return    the result of the floordiv, or null if this operation
     *            is not defined
     **/
    public PyObject __floordiv__(PyObject other) {
        return null;
    }

    /**
     * Equivalent to the standard Python __rfloordiv__ method
     * @param     other the object to perform this binary operation with
     *            (the left-hand operand).
     * @return    the result of the floordiv, or null if this operation
     *            is not defined.
     **/
    public PyObject __rfloordiv__(PyObject other) {
        return null;
    }

    /**
     * Equivalent to the standard Python __ifloordiv__ method
     * @param     other the object to perform this binary operation with
     *            (the right-hand operand).
     * @return    the result of the floordiv, or null if this operation
     *            is not defined
     **/
    public PyObject __ifloordiv__(PyObject other) {
        return _floordiv(other);
    }

    /**
     * Implements the Python expression <code>this // other</code>
     * @param     other the object to perform this binary operation with.
     * @return    the result of the floordiv.
     * @exception PyTypeError if this operation can't be performed
     *            with these operands.
     **/
    public final PyObject _floordiv(PyObject o2) {
        PyObject x = __floordiv__(o2);
        if (x != null)
            return x;
        x = o2.__rfloordiv__(this);
        if (x != null)
            return x;
        throw Py.TypeError(
            "__floordiv__ nor __rfloordiv__ defined for these operands");
    }

    /**
     * Equivalent to the standard Python __truediv__ method
     * @param     other the object to perform this binary operation with
     *            (the right-hand operand).
     * @return    the result of the truediv, or null if this operation
     *            is not defined
     **/
    public PyObject __truediv__(PyObject other) {
        return null;
    }

    /**
     * Equivalent to the standard Python __rtruediv__ method
     * @param     other the object to perform this binary operation with
     *            (the left-hand operand).
     * @return    the result of the truediv, or null if this operation
     *            is not defined.
     **/
    public PyObject __rtruediv__(PyObject other) {
        return null;
    }

    /**
     * Equivalent to the standard Python __itruediv__ method
     * @param     other the object to perform this binary operation with
     *            (the right-hand operand).
     * @return    the result of the truediv, or null if this operation
     *            is not defined
     **/
    public PyObject __itruediv__(PyObject other) {
        return _truediv(other);
    }

    /**
     * Implements the Python expression <code>this / other</code>
     * @param     other the object to perform this binary operation with.
     * @return    the result of the truediv.
     * @exception PyTypeError if this operation can't be performed
     *            with these operands.
     **/
    public final PyObject _truediv(PyObject o2) {
        PyObject x = __truediv__(o2);
        if (x != null)
            return x;
        x = o2.__rtruediv__(this);
        if (x != null)
            return x;
        throw Py.TypeError(
            "__truediv__ nor __rtruediv__ defined for these operands");
    }

    /**
     * Equivalent to the standard Python __mod__ method
     * @param     other the object to perform this binary operation with
     *            (the right-hand operand).
     * @return    the result of the mod, or null if this operation
     *            is not defined
     **/
    public PyObject __mod__(PyObject other) {
        return null;
    }

    /**
     * Equivalent to the standard Python __rmod__ method
     * @param     other the object to perform this binary operation with
     *            (the left-hand operand).
     * @return    the result of the mod, or null if this operation
     *            is not defined.
     **/
    public PyObject __rmod__(PyObject other) {
        return null;
    }

    /**
     * Equivalent to the standard Python __imod__ method
     * @param     other the object to perform this binary operation with
     *            (the right-hand operand).
     * @return    the result of the mod, or null if this operation
     *            is not defined
     **/
    public PyObject __imod__(PyObject other) {
        return _mod(other);
    }

    /**
     * Implements the Python expression <code>this % other</code>
     * @param     other the object to perform this binary operation with.
     * @return    the result of the mod.
     * @exception PyTypeError if this operation can't be performed
     *            with these operands.
     **/
    public final PyObject _mod(PyObject o2) {
        PyObject x = __mod__(o2);
        if (x != null)
            return x;
        x = o2.__rmod__(this);
        if (x != null)
            return x;
        throw Py.TypeError("__mod__ nor __rmod__ defined for these operands");
    }

    /**
     * Equivalent to the standard Python __divmod__ method
     * @param     other the object to perform this binary operation with
     *            (the right-hand operand).
     * @return    the result of the divmod, or null if this operation
     *            is not defined
     **/
    public PyObject __divmod__(PyObject other) {
        return null;
    }

    /**
     * Equivalent to the standard Python __rdivmod__ method
     * @param     other the object to perform this binary operation with
     *            (the left-hand operand).
     * @return    the result of the divmod, or null if this operation
     *            is not defined.
     **/
    public PyObject __rdivmod__(PyObject other) {
        return null;
    }

    /**
     * Equivalent to the standard Python __idivmod__ method
     * @param     other the object to perform this binary operation with
     *            (the right-hand operand).
     * @return    the result of the divmod, or null if this operation
     *            is not defined
     **/
    public PyObject __idivmod__(PyObject other) {
        return _divmod(other);
    }

    /**
     * Implements the Python expression <code>this divmod other</code>
     * @param     other the object to perform this binary operation with.
     * @return    the result of the divmod.
     * @exception PyTypeError if this operation can't be performed
     *            with these operands.
     **/
    public final PyObject _divmod(PyObject o2) {
        PyObject x = __divmod__(o2);
        if (x != null)
            return x;
        x = o2.__rdivmod__(this);
        if (x != null)
            return x;
        throw Py.TypeError(
            "__divmod__ nor __rdivmod__ defined for these operands");
    }

    /**
     * Equivalent to the standard Python __pow__ method
     * @param     other the object to perform this binary operation with
     *            (the right-hand operand).
     * @return    the result of the pow, or null if this operation
     *            is not defined
     **/
    public PyObject __pow__(PyObject other) {
        return __pow__(other, null);
    }

    /**
     * Equivalent to the standard Python __rpow__ method
     * @param     other the object to perform this binary operation with
     *            (the left-hand operand).
     * @return    the result of the pow, or null if this operation
     *            is not defined.
     **/
    public PyObject __rpow__(PyObject other) {
        return null;
    }

    /**
     * Equivalent to the standard Python __ipow__ method
     * @param     other the object to perform this binary operation with
     *            (the right-hand operand).
     * @return    the result of the pow, or null if this operation
     *            is not defined
     **/
    public PyObject __ipow__(PyObject other) {
        return _pow(other);
    }

    /**
     * Implements the Python expression <code>this ** other</code>
     * @param     other the object to perform this binary operation with.
     * @return    the result of the pow.
     * @exception PyTypeError if this operation can't be performed
     *            with these operands.
     **/
    public final PyObject _pow(PyObject o2) {
        PyObject x = __pow__(o2);
        if (x != null)
            return x;
        x = o2.__rpow__(this);
        if (x != null)
            return x;
        throw Py.TypeError("__pow__ nor __rpow__ defined for these operands");
    }

    /**
     * Equivalent to the standard Python __lshift__ method
     * @param     other the object to perform this binary operation with
     *            (the right-hand operand).
     * @return    the result of the lshift, or null if this operation
     *            is not defined
     **/
    public PyObject __lshift__(PyObject other) {
        return null;
    }

    /**
     * Equivalent to the standard Python __rlshift__ method
     * @param     other the object to perform this binary operation with
     *            (the left-hand operand).
     * @return    the result of the lshift, or null if this operation
     *            is not defined.
     **/
    public PyObject __rlshift__(PyObject other) {
        return null;
    }

    /**
     * Equivalent to the standard Python __ilshift__ method
     * @param     other the object to perform this binary operation with
     *            (the right-hand operand).
     * @return    the result of the lshift, or null if this operation
     *            is not defined
     **/
    public PyObject __ilshift__(PyObject other) {
        return _lshift(other);
    }

    /**
     * Implements the Python expression <code>this << other</code>
     * @param     other the object to perform this binary operation with.
     * @return    the result of the lshift.
     * @exception PyTypeError if this operation can't be performed
     *            with these operands.
     **/
    public final PyObject _lshift(PyObject o2) {
        PyObject x = __lshift__(o2);
        if (x != null)
            return x;
        x = o2.__rlshift__(this);
        if (x != null)
            return x;
        throw Py.TypeError(
            "__lshift__ nor __rlshift__ defined for these operands");
    }

    /**
     * Equivalent to the standard Python __rshift__ method
     * @param     other the object to perform this binary operation with
     *            (the right-hand operand).
     * @return    the result of the rshift, or null if this operation
     *            is not defined
     **/
    public PyObject __rshift__(PyObject other) {
        return null;
    }

    /**
     * Equivalent to the standard Python __rrshift__ method
     * @param     other the object to perform this binary operation with
     *            (the left-hand operand).
     * @return    the result of the rshift, or null if this operation
     *            is not defined.
     **/
    public PyObject __rrshift__(PyObject other) {
        return null;
    }

    /**
     * Equivalent to the standard Python __irshift__ method
     * @param     other the object to perform this binary operation with
     *            (the right-hand operand).
     * @return    the result of the rshift, or null if this operation
     *            is not defined
     **/
    public PyObject __irshift__(PyObject other) {
        return _rshift(other);
    }

    /**
     * Implements the Python expression <code>this >> other</code>
     * @param     other the object to perform this binary operation with.
     * @return    the result of the rshift.
     * @exception PyTypeError if this operation can't be performed
     *            with these operands.
     **/
    public final PyObject _rshift(PyObject o2) {
        PyObject x = __rshift__(o2);
        if (x != null)
            return x;
        x = o2.__rrshift__(this);
        if (x != null)
            return x;
        throw Py.TypeError(
            "__rshift__ nor __rrshift__ defined for these operands");
    }

    /**
     * Equivalent to the standard Python __and__ method
     * @param     other the object to perform this binary operation with
     *            (the right-hand operand).
     * @return    the result of the and, or null if this operation
     *            is not defined
     **/
    public PyObject __and__(PyObject other) {
        return null;
    }

    /**
     * Equivalent to the standard Python __rand__ method
     * @param     other the object to perform this binary operation with
     *            (the left-hand operand).
     * @return    the result of the and, or null if this operation
     *            is not defined.
     **/
    public PyObject __rand__(PyObject other) {
        return null;
    }

    /**
     * Equivalent to the standard Python __iand__ method
     * @param     other the object to perform this binary operation with
     *            (the right-hand operand).
     * @return    the result of the and, or null if this operation
     *            is not defined
     **/
    public PyObject __iand__(PyObject other) {
        return _and(other);
    }

    /**
     * Implements the Python expression <code>this & other</code>
     * @param     other the object to perform this binary operation with.
     * @return    the result of the and.
     * @exception PyTypeError if this operation can't be performed
     *            with these operands.
     **/
    public final PyObject _and(PyObject o2) {
        PyObject x = __and__(o2);
        if (x != null)
            return x;
        x = o2.__rand__(this);
        if (x != null)
            return x;
        throw Py.TypeError("__and__ nor __rand__ defined for these operands");
    }

    /**
     * Equivalent to the standard Python __or__ method
     * @param     other the object to perform this binary operation with
     *            (the right-hand operand).
     * @return    the result of the or, or null if this operation
     *            is not defined
     **/
    public PyObject __or__(PyObject other) {
        return null;
    }

    /**
     * Equivalent to the standard Python __ror__ method
     * @param     other the object to perform this binary operation with
     *            (the left-hand operand).
     * @return    the result of the or, or null if this operation
     *            is not defined.
     **/
    public PyObject __ror__(PyObject other) {
        return null;
    }

    /**
     * Equivalent to the standard Python __ior__ method
     * @param     other the object to perform this binary operation with
     *            (the right-hand operand).
     * @return    the result of the or, or null if this operation
     *            is not defined
     **/
    public PyObject __ior__(PyObject other) {
        return _or(other);
    }

    /**
     * Implements the Python expression <code>this | other</code>
     * @param     other the object to perform this binary operation with.
     * @return    the result of the or.
     * @exception PyTypeError if this operation can't be performed
     *            with these operands.
     **/
    public final PyObject _or(PyObject o2) {
        PyObject x = __or__(o2);
        if (x != null)
            return x;
        x = o2.__ror__(this);
        if (x != null)
            return x;
        throw Py.TypeError("__or__ nor __ror__ defined for these operands");
    }

    /**
     * Equivalent to the standard Python __xor__ method
     * @param     other the object to perform this binary operation with
     *            (the right-hand operand).
     * @return    the result of the xor, or null if this operation
     *            is not defined
     **/
    public PyObject __xor__(PyObject other) {
        return null;
    }

    /**
     * Equivalent to the standard Python __rxor__ method
     * @param     other the object to perform this binary operation with
     *            (the left-hand operand).
     * @return    the result of the xor, or null if this operation
     *            is not defined.
     **/
    public PyObject __rxor__(PyObject other) {
        return null;
    }

    /**
     * Equivalent to the standard Python __ixor__ method
     * @param     other the object to perform this binary operation with
     *            (the right-hand operand).
     * @return    the result of the xor, or null if this operation
     *            is not defined
     **/
    public PyObject __ixor__(PyObject other) {
        return _xor(other);
    }

    /**
     * Implements the Python expression <code>this ^ other</code>
     * @param     other the object to perform this binary operation with.
     * @return    the result of the xor.
     * @exception PyTypeError if this operation can't be performed
     *            with these operands.
     **/
    public final PyObject _xor(PyObject o2) {
        PyObject x = __xor__(o2);
        if (x != null)
            return x;
        x = o2.__rxor__(this);
        if (x != null)
            return x;
        throw Py.TypeError("__xor__ nor __rxor__ defined for these operands");
    }

    // Generated by make_binops.py (End)

    /* A convenience function for PyProxy's */
    // Possibly add _jcall(), _jcall(Object, ...) as future optimization
    /**
     * A convenience function for PyProxy's.
     * @param args call arguments.
     * @exception Throwable
     */
    public PyObject _jcallexc(Object[] args) throws Throwable {
        PyObject[] pargs = new PyObject[args.length];
        try {
            int n = args.length;
            for (int i = 0; i < n; i++)
                pargs[i] = Py.java2py(args[i]);
            return __call__(pargs);
        } catch (PyException e) {
            if (e.value instanceof PyJavaInstance) {
                Object t = e.value.__tojava__(Throwable.class);
                if (t != null && t != Py.NoConversion) {
                    throw (Throwable) t;
                }
            } else {
                ThreadState ts = Py.getThreadState();
                if (ts.frame == null) {
                    Py.maybeSystemExit(e);
                }
                if (Options.showPythonProxyExceptions) {
                    Py.stderr.println(
                        "Exception in Python proxy returning to Java:");
                    Py.printException(e);
                }
            }
            throw e;
        }
    }

    public void _jthrow(Throwable t) {
        if (t instanceof RuntimeException)
            throw (RuntimeException) t;
        if (t instanceof Error)
            throw (Error) t;
        throw Py.JavaError(t);
    }

    public PyObject _jcall(Object[] args) {
        try {
            return _jcallexc(args);
        } catch (Throwable t) {
            _jthrow(t);
            return null;
        }
    }

    /* Shortcut methods for calling methods from Java */

    /**
     * Shortcut for calling a method on a PyObject from Java.
     * This form is equivalent to o.__getattr__(name).__call__(args, keywords)
     *
     * @param name the name of the method to call.  This must be an
     *             interned string!
     * @param args an array of the arguments to the call.
     * @param keywords the keywords to use in the call.
     * @return the result of calling the method name with args and keywords.
     **/
    public PyObject invoke(String name, PyObject[] args, String[] keywords) {
        PyObject f = __getattr__(name);
        return f.__call__(args, keywords);
    }

    public PyObject invoke(String name, PyObject[] args) {
        PyObject f = __getattr__(name);
        return f.__call__(args);
    }

    /**
     * Shortcut for calling a method on a PyObject with no args.
     *
     * @param name the name of the method to call.  This must be an
     * interned string!
     * @return the result of calling the method name with no args
     **/
    public PyObject invoke(String name) {
        PyObject f = __getattr__(name);
        return f.__call__();
    }

    /**
     * Shortcut for calling a method on a PyObject with one arg.
     *
     * @param name the name of the method to call.  This must be an
     * interned string!
     * @param arg1 the one argument of the method.
     * @return the result of calling the method name with arg1
     **/
    public PyObject invoke(String name, PyObject arg1) {
        PyObject f = __getattr__(name);
        return f.__call__(arg1);
    }

    /**
     * Shortcut for calling a method on a PyObject with two args.
     *
     * @param name the name of the method to call.  This must be an
     *        interned string!
     * @param arg1 the first argument of the method.
     * @param arg2 the second argument of the method.
     * @return the result of calling the method name with arg1 and arg2
     **/
    public PyObject invoke(String name, PyObject arg1, PyObject arg2) {
        PyObject f = __getattr__(name);
        return f.__call__(arg1, arg2);
    }

    /* descriptors and lookup protocols */

    /** xxx implements where meaningful
     * @return __dict__ or null
     */
    public PyObject getDict() {
        return null;
    }

    public boolean implementsDescrSet() {
        return objtype.has_set;
    }

    public boolean implementsDescrDelete() {
        return objtype.has_delete;
    }

    public boolean isDataDescr() { // implements either __set__ or __delete__
        return objtype.has_set || objtype.has_delete;
    }

    // doc & xxx ok this way?
    // can return null meaning set-only or throw exception

    // backward comp impls.
    public PyObject __get__(PyObject obj, PyObject type) {
        return _doget(obj, type);
    }

    public void __set__(PyObject obj, PyObject value) {
        _doset(obj, value);
    }

    public void __delete__(PyObject obj) {
        _dodel(obj);
    }

    // name must be interned
    final PyObject object__findattr__(String name) {

        PyObject descr = objtype.lookup(name);
        PyObject res;

        if (descr != null) {
            if (descr.isDataDescr()) {
                res = descr.__get__(this, objtype);
                if (res != null)
                    return res;
            }
        }

        PyObject obj_dict = getDict();
        if (obj_dict != null) {
            res = obj_dict.__finditem__(name);
            if (res != null)
                return res;
        }

        if (descr != null) {
            return descr.__get__(this, objtype);
        }

        return null;
    }

    final void object__setattr__(String name, PyObject value) {
        PyObject descr = objtype.lookup(name);

        boolean set = false;

        if (descr != null) {
            set = descr.implementsDescrSet();
            if (set && descr.isDataDescr()) {
                descr.__set__(this, value);
                return;
            }
        }

        PyObject obj_dict = getDict();
        if (obj_dict != null) {
            obj_dict.__setitem__(name, value);
            return;
        }

        if (set) {
            descr.__set__(this, value);
        }

        if (descr != null)
            readonly_attribute(name);

        no_attribute(name);
    }

    final void object__delattr__(String name) {
        PyObject descr = objtype.lookup(name);

        boolean delete = false;

        if (descr != null) {
            delete = descr.implementsDescrDelete();
            if (delete && descr.isDataDescr()) {
                descr.__delete__(this);
                return;
            }
        }

        PyObject obj_dict = getDict();
        if (obj_dict != null) {
            obj_dict.__delitem__(name);
            // xxx convert KeyError into Attribute error
            return;
        }

        if (delete) {
            descr.__delete__(this);
        }

        if (descr != null)
            readonly_attribute(name);

        no_attribute(name);
    }

}

/*
 * A very specialized tuple-like class used when detecting cycles during
 * object comparisons. This classes is different from an normal tuple
 * by hashing and comparing its elements by identity.
 */

class PyIdentityTuple extends PyObject {

    PyObject[] list;

    public PyIdentityTuple(PyObject elements[]) {
        list = elements;
    }

    public int hashCode() {
        int x, y;
        int len = list.length;
        x = 0x345678;

        for (len--; len >= 0; len--) {
            y = System.identityHashCode(list[len]);
            x = (x + x + x) ^ y;
        }
        x ^= list.length;
        return x;
    }

    public boolean equals(Object o) {
        if (!(o instanceof PyIdentityTuple))
            return false;
        PyIdentityTuple that = (PyIdentityTuple) o;
        if (list.length != that.list.length)
            return false;
        for (int i = 0; i < list.length; i++) {
            if (list[i] != that.list[i])
                return false;
        }
        return true;
    }

}
