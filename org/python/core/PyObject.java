// Copyright © Corporation for National Research Initiatives
package org.python.core;

/**
All objects known to the JPython runtime system are represented
by an instance of the class <code>PyObject</code> or one of
its subclasses.

@author Jim Hugunin - hugunin@python.org
@version 1.1, 1/5/98
@since JPython 0.0
**/



public class PyObject implements java.io.Serializable {
    /**
       The Python class of this object.
       Unlike in CPython, all types have this attribute, even builtins.
       This should only be set in the constructor, never modified otherwise.
    **/
    public transient PyClass __class__;

    /* must instantiate __class__ when de-serializing */
    private void readObject(java.io.ObjectInputStream in)
        throws java.io.IOException, ClassNotFoundException
    {
        in.defaultReadObject();
        __class__ = PyJavaClass.lookup(getClass());
    }

    // A package private constructor used by PyJavaClass
    PyObject(boolean fakeArgument) {
        __class__ = (PyClass)this;
    }


    /**
       The standard constructor for a <code>PyObject</code>.  It will set
       the <code>__class__</code> field to correspond to the specific
       subclass of <code>PyObject</code> being instantiated.
    **/
    public PyObject() {
        PyClass c = getPyClass();
        if (c == null)
            c = PyJavaClass.lookup(getClass());
        __class__ = c;
    }

    /** This method is provided to efficiently initialize the __class__
        attribute.  If the following boilerplate is added to a subclass of
        PyObject, the instantiation time for the object will be greatly
        reduced.

        <blockquote><pre>
        // __class__ boilerplate -- see PyObject for details
        public static PyClass __class__;
        protected PyClass getPyClass() { return __class__; }
        </pre></blockquote>

        With PyIntegers this leads to a 50% faster instantiation time.
        This replaces the PyObject(PyClass c) constructor which is now
        deprecated.
    **/

    protected PyClass getPyClass() {
        return null;
    }

    /**
       #### This method is now deprecated and will go away in the future ####
       A more sophisticated constructor for a <code>PyObject</code>.
       Can be more efficient as it allows the subclass of PyObject to cache its
       known <code>__class__</code>.

       The common idiom for using this constructor is shown as used for the
       PyInteger class:
       <blockquote><pre>
       public static PyClass __class__;
       public PyInteger(int v) {
       super(__class__);
       ...
       </pre></blockquote>

       @param c a <code>PyClass</code> instance giving the
       <code>__class__</code> of the new <code>PyObject</code> @deprecated
       see get PyClass for details
    **/
    public PyObject(PyClass c) {
        if (c == null) {
            c = PyJavaClass.lookup(getClass());
        }
        __class__ = c;
    }

    /**
       Equivalent to the standard Python __repr__ method.  This method
       should not typically need to be overrriden.  The easiest way to
       configure the string representation of a <code>PyObject</code> is to
       override the standard Java <code>toString</code> method.
    **/
    public PyString __repr__() {
        return new PyString(toString());
    }

    // TBD: safeRepr() is protected so that it's not accessible from
    // Python.  This is bogus; arbitrary Java code should be able to get
    // safeRepr but we still want to hide it from Python.  There should be
    // another way to hide Java methods from Python.
    protected String safeRepr() {
        if (__class__ == null) {
            return "unknown object";
        }
        String name = __class__.__name__;
        PyObject tmp;
        if (name == null)
            return "unknown object";

        if ((name.equals("org.python.core.PyClass") ||
             name.equals("org.python.core.PyJavaClass")) &&
            (this instanceof PyClass))
        {
            name = ((PyClass)this).__name__;
            if (name == null)
                return "unknown class";
            return "class '"+name+"'";
        }

        if (name.equals("org.python.core.PyModule")) {
            tmp = this.__findattr__("__name__");
            if (tmp == null)
                return "unnamed module";
            return "module '"+tmp+"'";
        }
        if (name.equals("org.python.core.PyJavaPackage") &&
            (this instanceof PyJavaPackage))
        {
            name = ((PyJavaPackage)this).__name__;
            if (name == null)
                return "unnamed java package";
            return "java package '"+name+"'";
        }
        return "instance of '"+name+"'";
    }

    /**
       Equivalent to the standard Python __str__ method.  This method
       should not typically need to be overridden.  The easiest way to
       configure the string representation of a <code>PyObject</code> is to
       override the standard Java <code>toString</code> method.
    **/
    public PyString __str__() {
        return __repr__();
    }

    /**
       Equivalent to the standard Python __hash__ method.  This method can
       not be overridden.  Instead, you should override the standard Java
       <code>hashCode</code> method to return an appropriate hash code for
       the <code>PyObject</code>.
    **/
    public final PyInteger __hash__() {
        return new PyInteger(hashCode());
    }

    public int hashCode() {
        return Py.id(this);
    }

    /**
       Should almost never be overridden.
       If overridden, it is the subclasses responsibility to ensure that
       <code>a.equals(b) == true</code> iff <code>cmp(a,b) == 0</code>
    **/
    public boolean equals(Object ob_other) {
        return (ob_other instanceof PyObject) &&
            _eq((PyObject)ob_other).__nonzero__();
    }

    /**
       Equivalent to the standard Python __nonzero__ method.
       Returns whether of not a given <code>PyObject</code> is considered true.
    **/
    public boolean __nonzero__() {
        return true;
    }

    /**
       Equivalent to the JPython __tojava__ method.
       Tries to coerce this object to an instance of the requested Java class.
       Returns the special object <code>Py.NoConversion</code>
       if this <code>PyObject</code> can not be converted to the
       desired Java class.

       @param c the Class to convert this <code>PyObject</code> to.
    **/
    public Object __tojava__(Class c) {
        if (c.isInstance(this))
            return this;
        return Py.NoConversion;
    }

    /**
       The basic method to override when implementing a callable object.

       The first len(args)-len(keywords) members of args[] are plain
       arguments.  The last len(keywords) arguments are the values of the
       keyword arguments.

       @param args     all arguments to the function (including
                       keyword arguments).
       @param keywords the keywords used for all keyword arguments.
    **/
    public PyObject __call__(PyObject args[], String keywords[]) {
        throw Py.TypeError("call of non-function (" + safeRepr() + ")");
    }

    /**
       A variant of the __call__ method with one extra initial argument.
       This variant is used to allow method invocations to be performed
       efficiently.

       The default behavior is to invoke <code>__call__(args,
       keywords)</code> with the appropriate arguments.  The only reason to
       override this function would be for improved performance.

       @param arg1     the first argument to the function.
       @param args     the last arguments to the function (including
                       keyword arguments).
       @param keywords the keywords used for all keyword arguments.
    **/
    public PyObject __call__(PyObject arg1, PyObject args[],
                             String keywords[])
    {
        PyObject[] newArgs = new PyObject[args.length+1];
        System.arraycopy(args, 0, newArgs, 1, args.length);
        newArgs[0] = arg1;
        return __call__(newArgs, keywords);
    }

    /**
       A variant of the __call__ method when no keywords are passed.  The
       default behavior is to invoke <code>__call__(args, keywords)</code>
       with the appropriate arguments.  The only reason to override this
       function would be for improved performance.

       @param args     all arguments to the function.
    **/
    public PyObject __call__(PyObject args[]) {
        return __call__(args, Py.NoKeywords);
    }

    /**
       A variant of the __call__ method with no arguments.  The default
       behavior is to invoke <code>__call__(args, keywords)</code> with the
       appropriate arguments.  The only reason to override this function
       would be for improved performance.
    **/
    public PyObject __call__() {
        return __call__(Py.EmptyObjects, Py.NoKeywords);
    }

    /**
       A variant of the __call__ method with one argument.  The default
       behavior is to invoke <code>__call__(args, keywords)</code> with the
       appropriate arguments.  The only reason to override this function
       would be for improved performance.

       @param arg0     the single argument to the function.
    **/
    public PyObject __call__(PyObject arg0) {
        return __call__(new PyObject[] {arg0}, Py.NoKeywords);
    }

    /**
       A variant of the __call__ method with two arguments.  The default
       behavior is to invoke <code>__call__(args, keywords)</code> with the
       appropriate arguments.  The only reason to override this function
       would be for improved performance.

       @param arg0     the first argument to the function.
       @param arg1     the second argument to the function.
    **/
    public PyObject __call__(PyObject arg0, PyObject arg1) {
        return __call__(new PyObject[] {arg0, arg1}, Py.NoKeywords);
    }

    /**
       A variant of the __call__ method with three arguments.  The default
       behavior is to invoke <code>__call__(args, keywords)</code> with the
       appropriate arguments.  The only reason to override this function
       would be for improved performance.

       @param arg0     the first argument to the function.
       @param arg1     the second argument to the function.
       @param arg2     the third argument to the function.
    **/
    public PyObject __call__(PyObject arg0, PyObject arg1, PyObject arg2) {
        return __call__(new PyObject[] {arg0, arg1, arg2}, Py.NoKeywords);
    }

    /**
       A variant of the __call__ method with four arguments.  The default
       behavior is to invoke <code>__call__(args, keywords)</code> with the
       appropriate arguments.  The only reason to override this function
       would be for improved performance.

       @param arg0     the first argument to the function.
       @param arg1     the second argument to the function.
       @param arg2     the third argument to the function.
       @param arg3     the fourth argument to the function.
    **/
    public PyObject __call__(PyObject arg0, PyObject arg1,
                             PyObject arg2, PyObject arg3)
    {
        return __call__(new PyObject[] {arg0, arg1, arg2, arg3},
                        Py.NoKeywords);
    }

    /** @deprecated **/
    public PyObject _callextra(PyObject[] args, String[] keywords,
            PyObject starargs, PyObject kwargs) {

       int argslen = args.length;
       int nstar = 0;

       String name = "";
       if (this instanceof PyFunction)
           name = ((PyFunction) this).__name__ + "() ";

       if (kwargs != null) {
           PyObject keys = kwargs.__findattr__("keys");
           if (keys == null)
               throw Py.TypeError(name + "argument after ** must be a dictionary");
           for (int i = 0; i < keywords.length; i++)
               if (kwargs.__finditem__(keywords[i]) != null)
                   throw Py.TypeError(name +
                          "got multiple values for keyword argument '" + keywords[i] + "'");
           argslen += kwargs.__len__();
       }
       if (starargs != null) {
           if (!(starargs instanceof PySequence ||
                       starargs instanceof PyInstance))
               throw Py.TypeError(name + "argument after * must be a sequence");
           nstar = starargs.__len__();
           argslen += nstar;
       }

       PyObject[] newargs = new PyObject[argslen];
       int argidx = args.length - keywords.length;
       System.arraycopy(args, 0, newargs, 0, argidx);

       if (starargs != null) {
           PyObject a;
           for (int i = 0; (a = starargs.__finditem__(i)) != null &&
                                              i < nstar; i++) {
               newargs[argidx++] = a;
           }
       }
       System.arraycopy(args, args.length - keywords.length,
                        newargs, argidx, keywords.length);
       argidx += keywords.length;

       if (kwargs != null) {
           String[] newkeywords =
                       new String[keywords.length + kwargs.__len__()];
           System.arraycopy(keywords, 0, newkeywords, 0, keywords.length);

           PyObject keys = kwargs.invoke("keys");
           PyObject key;
           for (int i = 0; (key = keys.__finditem__(i)) != null; i++) {
               if (!(key instanceof PyString))
                   throw Py.TypeError(name +
                       "keywords must be strings");
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



    public boolean isCallable() { return __findattr__("__call__") != null; }
    public boolean isMappingType() { return true; }
    public boolean isNumberType() { return true; }
    public boolean isSequenceType() { return true; }

    /* The basic functions to implement a mapping */

    /**
       Equivalent to the standard Python __len__ method.
       Part of the mapping discipline.

       @return the length of the object
    **/
    public int __len__() {
        throw Py.AttributeError("__len__");
    }

    /**
       Very similar to the standard Python __getitem__ method.
       Instead of throwing a KeyError if the item isn't found,
       this just returns null.

       Classes that wish to implement __getitem__ should
       override this method instead (with the appropriate
       semantics.

       @param key the key to lookup in this container

       @return the value corresponding to key or null if key is not found
    **/
    public PyObject __finditem__(PyObject key) {
        throw Py.AttributeError("__getitem__");
    }

    /**
       A variant of the __finditem__ method which accepts a primitive
       <code>int</code> as the key.  By default, this method will call
       <code>__finditem__(PyObject key)</code> with the appropriate args.
       The only reason to override this method is for performance.

       @param key the key to lookup in this sequence.
       @return the value corresponding to key or null if key is not found.

       @see org.python.core.PyObject#__finditem__(org.python.core.PyObject)
    **/
    public PyObject __finditem__(int key) {
        return __finditem__(new PyInteger(key));
    }

    /**
       A variant of the __finditem__ method which accepts a Java
       <code>String</code> as the key.  By default, this method will call
       <code>__finditem__(PyObject key)</code> with the appropriate args.
       The only reason to override this method is for performance.

       <b>Warning: key must be an interned string!!!!!!!!</b>

       @param key the key to lookup in this sequence -
       <b> must be an interned string </b>.
       @return the value corresponding to key or null if key is not found.

       @see org.python.core.PyObject#__finditem__(org.python.core.PyObject)
    **/

    public PyObject __finditem__(String key) {
        return __finditem__(new PyString(key));
    }


    public PyObject __getitem__(int key) {
        PyObject ret = __finditem__(key);
        if (ret == null)
            throw Py.KeyError(""+key);
        return ret;
    }

    public PyObject __getslice__(PyObject s_start, PyObject s_stop,
                                 PyObject s_step)
    {
        PySlice s = new PySlice(s_start, s_stop, s_step);
        return __getitem__(s);
    }

    public void __setslice__(PyObject s_start, PyObject s_stop,
                             PyObject s_step, PyObject value)
    {
        PySlice s = new PySlice(s_start, s_stop, s_step);
        __setitem__(s, value);
    }

    public void __delslice__(PyObject s_start, PyObject s_stop,
                             PyObject s_step)
    {
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

    /**
       Equivalent to the standard Python __getitem__ method.
       This method should not be overridden.
       Override the <code>__finditem__</code> method instead.

       @param key the key to lookup in this container.
       @return the value corresponding to that key.
       @exception PyKeyError if the key is not found.

       @see org.python.core.PyObject#__finditem__(org.python.core.PyObject)
    **/
    public PyObject __getitem__(PyObject key) {
        PyObject ret = __finditem__(key);
        if (ret == null)
            throw Py.KeyError(key.toString());
        return ret;
    }

    /**
       Equivalent to the standard Python __setitem__ method.

       @param key the key whose value will be set
       @param value the value to set this key to
    **/
    public void __setitem__(PyObject key, PyObject value) {
        throw Py.AttributeError("__setitem__");
    }

    /**
       A variant of the __setitem__ method which accepts a String
       as the key.  <b>This String must be interned</b>.
       By default, this will call
       <code>__setitem__(PyObject key, PyObject value)</code>
       with the appropriate args.
       The only reason to override this method is for performance.

       @param key the key whose value will be set -
       <b> must be an interned string </b>.
       @param value the value to set this key to
       @see org.python.core.PyObject#__setitem__(org.python.core.PyObject,
       org.python.core.PyObject)
    **/
    public void __setitem__(String key, PyObject value) {
        __setitem__(new PyString(key), value);
    }

    public void __setitem__(int key, PyObject value) {
        __setitem__(new PyInteger(key), value);
    }

    /**
       Equivalent to the standard Python __delitem__ method.

       @param key the key to be removed from the container
       @exception PyKeyError if the key is not found in the container
    **/
    public void __delitem__(PyObject key) {
        throw Py.AttributeError("__delitem__");
    }

    /**
       A variant of the __delitem__ method which accepts a String
       as the key.  <b>This String must be interned</b>.
       By default, this will call
       <code>__delitem__(PyObject key)</code>
       with the appropriate args.
       The only reason to override this method is for performance.

       @param key the key who will be removed -
       <b> must be an interned string </b>.
       @exception PyKeyError if the key is not found in the container
       @see org.python.core.PyObject#__delitem__(org.python.core.PyObject)
    **/
    public void __delitem__(String key) {
        __delitem__(new PyString(key));
    }


    /*The basic functions to implement a namespace*/

    public PyObject __findattr__(PyString name) {
        return __findattr__(name.internedString());
    }

    public final void __setattr__(PyString name, PyObject value) {
        __setattr__(name.internedString(), value);
    }

    public final void __delattr__(PyString name) {
        __delattr__(name.internedString());
    }

    /**
       Very similar to the standard Python __getattr__ method.
       Instead of throwing a AttributeError if the item isn't found,
       this just returns null.

       Classes that wish to implement __getitem__ should
       override this method instead (with the appropriate
       semantics.

       @param name the name to lookup in this namespace

       @return the value corresponding to name or null if name is not found
    **/

    /**
       A variant of the __findattr__ method which accepts a Java
       <code>String</code> as the name.

       By default, this method will call <code>__findattr__(PyString
       name)</code> with the appropriate args.  The only reason to override
       this method is for performance.

       <b>Warning: name must be an interned string!!!!!!!!</b>

       @param name the name to lookup in this namespace
       <b> must be an interned string </b>.
       @return the value corresponding to name or null if name is not found


       @see org.python.core.PyObject#__findattr__(org.python.core.PyString)
    **/
    public PyObject __findattr__(String name) {
        if (__class__ == null)
            return null;
        if (name == "__class__")
            return __class__;
        PyObject ret = __class__.lookup(name, false);
        if (ret != null)
            return ret._doget(this);
        return null;
    }

    // Used by import logic.
    protected PyObject impAttr(String name) {
        return __findattr__(name);
    }

    /**
       Equivalent to the standard Python __getattr__ method.
       This method can not be overridden.
       Override the <code>__findattr__</code> method instead.

       @param name the name to lookup in this namespace
       @return the value corresponding to name
       @exception PyAttributeError if the name is not found.

       @see org.python.core.PyObject#__findattr__(org.python.core.PyString)
    **/
    public final PyObject __getattr__(PyString name) {
        PyObject ret = __findattr__(name);
        if (ret == null)
            throw Py.AttributeError(safeRepr()+" has no attribute '"+name+"'");
        return ret;
    }

    /**
       A variant of the __getattr__ method which accepts a Java
       <code>String</code> as the name.
       This method can not be overridden.
       Override the <code>__findattr__</code> method instead.

       <b>Warning: name must be an interned string!!!!!!!!</b>

       @param name the name to lookup in this namespace
       <b> must be an interned string </b>.
       @return the value corresponding to name
       @exception PyAttributeError if the name is not found.

       @see org.python.core.PyObject#__findattr__(java.lang.String)
    **/
    public final PyObject __getattr__(String name) {
        PyObject ret = __findattr__(name);
        if (ret == null)
            throw Py.AttributeError(safeRepr()+" has no attribute '"+name+"'");
        return ret;
    }

    /**
       Equivalent to the standard Python __setattr__ method.

       @param name the name whose value will be set
       @param value the value to set this name to
    **/
    /*public void __setattr__(PyString name, PyObject value) {
      }*/

    /**
       A variant of the __setattr__ method which accepts a String
       as the key.  <b>This String must be interned</b>.
       By default, this will call
       <code>__setattr__(PyString name, PyObject value)</code>
       with the appropriate args.
       The only reason to override this method is for performance.

       @param name the name whose value will be set -
       <b> must be an interned string </b>.
       @param value the value to set this name to
       @see org.python.core.PyObject#__setattr__(org.python.core.PyString,
       org.python.core.PyObject)
    **/
    public void __setattr__(String name, PyObject value) {
        throw Py.TypeError("readonly class or attribute: " + name);
    }

    /**
       Equivalent to the standard Python __delattr__ method.

       @param name the name to be removed from this namespace
       @exception PyAttributeError if the name doesn't exist
    **/
    /*public void __delattr__(PyString name) throws PyAttributeError {
      }*/

    /**
       A variant of the __delattr__ method which accepts a String
       as the key.  <b>This String must be interned</b>.
       By default, this will call
       <code>__delattr__(PyString name)</code>
       with the appropriate args.
       The only reason to override this method is for performance.

       @param name the name which will be removed -
       <b> must be an interned string </b>.
       @exception PyAttributeError if the name doesn't exist
       @see org.python.core.PyObject#__delattr__(org.python.core.PyString)
    **/
    public void __delattr__(String name) {
        throw Py.TypeError("readonly class");
    }

    protected void addKeys(PyList ret, String attr) {
        PyObject obj = __findattr__(attr);
        if (obj == null)
            return;
        if (obj instanceof PyDictionary) {
            ret.setslice(ret.__len__(), ret.__len__(), 1,
                         ((PyDictionary)obj).keys());
        }
        else if (obj instanceof PyStringMap) {
            ret.setslice(ret.__len__(), ret.__len__(), 1,
                         ((PyStringMap)obj).keys());
        }
        else if (obj instanceof PyList) {
            ret.setslice(ret.__len__(), ret.__len__(), 1, (PyList)obj);
        }
    }

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
       Implements numeric coercion

       @param o the other object involved in the coercion
       @return null if no coercion is possible;
       a single PyObject to use to replace o if this is unchanged;
       or a PyObject[2] consisting of replacements for this and o.
    **/
    public Object __coerce_ex__(PyObject o) throws PyException {
        return null;
    }

    /**
       Equivalent to the standard Python __coerce__ method.

       This method can not be overridden.
       To implement __coerce__ functionality, override __coerce_ex__ instead.

       @param pyo the other object involved in the coercion.
       @return a tuple of this object and pyo coerced to the same type
       or Py.None if no coercion is possible.
       @see org.python.core.PyObject#__coerce_ex__(org.python.core.PyObject)
    **/
    public final PyObject __coerce__(PyObject pyo) {
        Object o = __coerce_ex__(pyo);
        if (o == null)
            throw Py.AttributeError("__coerce__");
        if (o == Py.None)
            return (PyObject)o;
        if (o instanceof PyObject[])
            return new PyTuple((PyObject[])o);
        else
            return new PyTuple(new PyObject[] {this, (PyObject)o});
    }

    /* The basic comparision operations */
    /**
       Equivalent to the standard Python __cmp__ method.

       @param other the object to compare this with.
       @return -1 if this < 0; 0 if this == o; +1 if this > o; -2 if no
       comparison is implemented
    **/
    public int __cmp__(PyObject other) {
        return -2;
    }


    public PyObject __eq__(PyObject other) { return null; }

    public PyObject __ne__(PyObject other) { return null; }

    public PyObject __le__(PyObject other) { return null; }

    public PyObject __lt__(PyObject other) { return null; }

    public PyObject __ge__(PyObject other) { return null; }

    public PyObject __gt__(PyObject other) { return null; }


    /**
       Implements cmp(this, other)

       @param other the object to compare this with.
       @return -1 if this < 0; 0 if this == o; +1 if this > o
    **/
    public final int _cmp(PyObject o2_in) {
        ThreadState ts = Py.getThreadState();
        try {
            if (++ts.compareStateNesting > 500) {
                PyDictionary stateDict = ts.getCompareStateDict();

                PyObject pair = make_pair(o2_in);

                if (stateDict.__finditem__(pair) != null) {
                    // already comparing these objects.  assume
                    // they're equal until shown otherwise */
                    return 0;
                }
                stateDict.__setitem__(pair, pair);
                int res = _cmp_unsafe(o2_in);
                stateDict.__delitem__(pair);
                return res;
            }
            return _cmp_unsafe(o2_in);
        } finally {
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
        if (o1.__class__ != o2.__class__) {
            ctmp = o1.__coerce_ex__(o2);
            if (ctmp != null) {
                if (ctmp instanceof PyObject[]) {
                    o1 = ((PyObject[])ctmp)[0];
                    o2 = ((PyObject[])ctmp)[1];
                }
                else {
                    o2 = (PyObject)ctmp;
                }
            }
        }
        else ctmp = null;

        if (ctmp != Py.None && (itmp = o1.__cmp__(o2)) != -2)
            return itmp;

        o1 = this;
        o2 = o2_in;
        if (o1.__class__ != o2.__class__) {
            ctmp = o2.__coerce_ex__(o1);
            if (ctmp != null) {
                if (ctmp instanceof PyObject[]) {
                    o2 = ((PyObject[])ctmp)[0];
                    o1 = ((PyObject[])ctmp)[1];
                }
                else {
                    o1 = (PyObject)ctmp;
                }
            }
        }
        if (ctmp != Py.None && (itmp = o2.__cmp__(o1)) != -2)
            return -itmp;

        if (this == o2_in)
            return 0;

        // No rational way to compare these, so ask their classes to compare
        itmp = this.__class__.__cmp__(o2_in.__class__);

        if (itmp == 0)
            return Py.id(this) < Py.id(o2_in) ? -1 : 1;
        if (itmp != -2)
            return itmp;
        return
            Py.id(this.__class__) < Py.id(o2_in.__class__) ? -1 : 1;
    }


    private final static PyObject check_recursion(ThreadState ts, PyObject o1, PyObject o2) {
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

    public PyObject _is(PyObject o) {
        return this == o ? Py.One : Py.Zero;
    }

    public PyObject _isnot(PyObject o) {
        return this != o ? Py.One : Py.Zero;
    }

    public final PyObject _in(PyObject o) {
        return Py.newBoolean(o.__contains__(this));
    }

    public boolean __contains__(PyObject o) {
        PyObject tmp;
        int i = 0;

        while ((tmp = __finditem__(i++)) != null) {
            if (o._eq(tmp).__nonzero__())
                return true;
        }
        return false;
    }

    public final PyObject _notin(PyObject o) {
        return Py.newBoolean(!o.__contains__(this));
    }

    /**
       Implements boolean not

       @return not this.
    **/
    public PyObject __not__() {
        return __nonzero__() ? Py.Zero : Py.One;
    }

    /* The basic numeric operations */

    /**
       Equivalent to the standard Python __hex__ method
       Should only be overridden by numeric objects that can be
       reasonably represented as a hexadecimal string.

       @return a string representing this object as a hexadecimal number.
    **/
    public PyString __hex__() {
        throw Py.AttributeError("__hex__");
    }

    /**
       Equivalent to the standard Python __oct__ method
       Should only be overridden by numeric objects that can be
       reasonably represented as an octal string.

       @return a string representing this object as an octal number.
    **/
    public PyString __oct__() {
        throw Py.AttributeError("__oct__");
    }

    /**
       Equivalent to the standard Python __int__ method
       Should only be overridden by numeric objects that can be
       reasonably coerced into an integer.

       @return an integer corresponding to the value of this object.
    **/
    public PyInteger __int__() {
        throw Py.AttributeError("__int__");
    }

    /**
       Equivalent to the standard Python __long__ method
       Should only be overridden by numeric objects that can be
       reasonably coerced into a python long.

       @return a PyLong corresponding to the value of this object.
    **/
    public PyLong __long__() {
        throw Py.AttributeError("__long__");
    }

    /**
       Equivalent to the standard Python __float__ method
       Should only be overridden by numeric objects that can be
       reasonably coerced into a python float.

       @return a float corresponding to the value of this object.
    **/
    public PyFloat __float__() {
        throw Py.AttributeError("__float__");
    }

    /**
       Equivalent to the standard Python __complex__ method
       Should only be overridden by numeric objects that can be
       reasonably coerced into a python complex number.

       @return a complex number corresponding to the value of this object.
    **/
    public PyComplex __complex__() {
        throw Py.AttributeError("__complex__");
    }

    /**
       Equivalent to the standard Python __pos__ method

       @return +this.
    **/
    public PyObject __pos__() {
        throw Py.AttributeError("__pos__");
    }

    /**
       Equivalent to the standard Python __neg__ method

       @return -this.
    **/
    public PyObject __neg__() {
        throw Py.AttributeError("__neg__");
    }

    /**
       Equivalent to the standard Python __abs__ method

       @return abs(this).
    **/
    public PyObject __abs__() {
        throw Py.AttributeError("__abs__");
    }

    /**
       Equivalent to the standard Python __invert__ method

       @return ~this.
    **/
    public PyObject __invert__() {
        throw Py.AttributeError("__invert__");
    }

    /**
       Implements the three argument power function

       @param o2 the power to raise this number to.
       @param o3 the modulus to perform this operation in or null if no
       modulo is to be used
       @return this object raised to the given power in the given modulus
    **/
    public PyObject __pow__(PyObject o2, PyObject o3) throws PyException { return null; }



    // Generated by make_binops.py (Begin)

    /**
     * Equivalent to the standard Python __add__ method
     * @param     other the object to perform this binary operation with
     *            (the right-hand operand).
     * @return    the result of the add, or null if this operation
     *            is not defined
     **/
    public PyObject __add__(PyObject other) { return null; }

    /**
     * Equivalent to the standard Python __radd__ method
     * @param     other the object to perform this binary operation with
     *            (the left-hand operand).
     * @return    the result of the add, or null if this operation
     *            is not defined.
     **/
    public PyObject __radd__(PyObject other) { return null; }

    /**
     * Equivalent to the standard Python __iadd__ method
     * @param     other the object to perform this binary operation with
     *            (the right-hand operand).
     * @return    the result of the add, or null if this operation
     *            is not defined
     **/
    public PyObject __iadd__(PyObject other) { return _add(other); }

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
        throw Py.TypeError(
                 "__add__ nor __radd__ defined for these operands");
    }

    /**
     * Equivalent to the standard Python __sub__ method
     * @param     other the object to perform this binary operation with
     *            (the right-hand operand).
     * @return    the result of the sub, or null if this operation
     *            is not defined
     **/
    public PyObject __sub__(PyObject other) { return null; }

    /**
     * Equivalent to the standard Python __rsub__ method
     * @param     other the object to perform this binary operation with
     *            (the left-hand operand).
     * @return    the result of the sub, or null if this operation
     *            is not defined.
     **/
    public PyObject __rsub__(PyObject other) { return null; }

    /**
     * Equivalent to the standard Python __isub__ method
     * @param     other the object to perform this binary operation with
     *            (the right-hand operand).
     * @return    the result of the sub, or null if this operation
     *            is not defined
     **/
    public PyObject __isub__(PyObject other) { return _sub(other); }

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
        throw Py.TypeError(
                 "__sub__ nor __rsub__ defined for these operands");
    }

    /**
     * Equivalent to the standard Python __mul__ method
     * @param     other the object to perform this binary operation with
     *            (the right-hand operand).
     * @return    the result of the mul, or null if this operation
     *            is not defined
     **/
    public PyObject __mul__(PyObject other) { return null; }

    /**
     * Equivalent to the standard Python __rmul__ method
     * @param     other the object to perform this binary operation with
     *            (the left-hand operand).
     * @return    the result of the mul, or null if this operation
     *            is not defined.
     **/
    public PyObject __rmul__(PyObject other) { return null; }

    /**
     * Equivalent to the standard Python __imul__ method
     * @param     other the object to perform this binary operation with
     *            (the right-hand operand).
     * @return    the result of the mul, or null if this operation
     *            is not defined
     **/
    public PyObject __imul__(PyObject other) { return _mul(other); }

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
        throw Py.TypeError(
                 "__mul__ nor __rmul__ defined for these operands");
    }

    /**
     * Equivalent to the standard Python __div__ method
     * @param     other the object to perform this binary operation with
     *            (the right-hand operand).
     * @return    the result of the div, or null if this operation
     *            is not defined
     **/
    public PyObject __div__(PyObject other) { return null; }

    /**
     * Equivalent to the standard Python __rdiv__ method
     * @param     other the object to perform this binary operation with
     *            (the left-hand operand).
     * @return    the result of the div, or null if this operation
     *            is not defined.
     **/
    public PyObject __rdiv__(PyObject other) { return null; }

    /**
     * Equivalent to the standard Python __idiv__ method
     * @param     other the object to perform this binary operation with
     *            (the right-hand operand).
     * @return    the result of the div, or null if this operation
     *            is not defined
     **/
    public PyObject __idiv__(PyObject other) { return _div(other); }

    /**
     * Implements the Python expression <code>this / other</code>
     * @param     other the object to perform this binary operation with.
     * @return    the result of the div.
     * @exception PyTypeError if this operation can't be performed
     *            with these operands.
     **/
    public final PyObject _div(PyObject o2) {
        PyObject x = __div__(o2);
        if (x != null)
            return x;
        x = o2.__rdiv__(this);
        if (x != null)
            return x;
        throw Py.TypeError(
                 "__div__ nor __rdiv__ defined for these operands");
    }

    /**
     * Equivalent to the standard Python __mod__ method
     * @param     other the object to perform this binary operation with
     *            (the right-hand operand).
     * @return    the result of the mod, or null if this operation
     *            is not defined
     **/
    public PyObject __mod__(PyObject other) { return null; }

    /**
     * Equivalent to the standard Python __rmod__ method
     * @param     other the object to perform this binary operation with
     *            (the left-hand operand).
     * @return    the result of the mod, or null if this operation
     *            is not defined.
     **/
    public PyObject __rmod__(PyObject other) { return null; }

    /**
     * Equivalent to the standard Python __imod__ method
     * @param     other the object to perform this binary operation with
     *            (the right-hand operand).
     * @return    the result of the mod, or null if this operation
     *            is not defined
     **/
    public PyObject __imod__(PyObject other) { return _mod(other); }

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
        throw Py.TypeError(
                 "__mod__ nor __rmod__ defined for these operands");
    }

    /**
     * Equivalent to the standard Python __divmod__ method
     * @param     other the object to perform this binary operation with
     *            (the right-hand operand).
     * @return    the result of the divmod, or null if this operation
     *            is not defined
     **/
    public PyObject __divmod__(PyObject other) { return null; }

    /**
     * Equivalent to the standard Python __rdivmod__ method
     * @param     other the object to perform this binary operation with
     *            (the left-hand operand).
     * @return    the result of the divmod, or null if this operation
     *            is not defined.
     **/
    public PyObject __rdivmod__(PyObject other) { return null; }

    /**
     * Equivalent to the standard Python __idivmod__ method
     * @param     other the object to perform this binary operation with
     *            (the right-hand operand).
     * @return    the result of the divmod, or null if this operation
     *            is not defined
     **/
    public PyObject __idivmod__(PyObject other) { return _divmod(other); }

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
    public PyObject __pow__(PyObject other) { return __pow__(other, null); }

    /**
     * Equivalent to the standard Python __rpow__ method
     * @param     other the object to perform this binary operation with
     *            (the left-hand operand).
     * @return    the result of the pow, or null if this operation
     *            is not defined.
     **/
    public PyObject __rpow__(PyObject other) { return null; }

    /**
     * Equivalent to the standard Python __ipow__ method
     * @param     other the object to perform this binary operation with
     *            (the right-hand operand).
     * @return    the result of the pow, or null if this operation
     *            is not defined
     **/
    public PyObject __ipow__(PyObject other) { return _pow(other); }

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
        throw Py.TypeError(
                 "__pow__ nor __rpow__ defined for these operands");
    }

    /**
     * Equivalent to the standard Python __lshift__ method
     * @param     other the object to perform this binary operation with
     *            (the right-hand operand).
     * @return    the result of the lshift, or null if this operation
     *            is not defined
     **/
    public PyObject __lshift__(PyObject other) { return null; }

    /**
     * Equivalent to the standard Python __rlshift__ method
     * @param     other the object to perform this binary operation with
     *            (the left-hand operand).
     * @return    the result of the lshift, or null if this operation
     *            is not defined.
     **/
    public PyObject __rlshift__(PyObject other) { return null; }

    /**
     * Equivalent to the standard Python __ilshift__ method
     * @param     other the object to perform this binary operation with
     *            (the right-hand operand).
     * @return    the result of the lshift, or null if this operation
     *            is not defined
     **/
    public PyObject __ilshift__(PyObject other) { return _lshift(other); }

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
    public PyObject __rshift__(PyObject other) { return null; }

    /**
     * Equivalent to the standard Python __rrshift__ method
     * @param     other the object to perform this binary operation with
     *            (the left-hand operand).
     * @return    the result of the rshift, or null if this operation
     *            is not defined.
     **/
    public PyObject __rrshift__(PyObject other) { return null; }

    /**
     * Equivalent to the standard Python __irshift__ method
     * @param     other the object to perform this binary operation with
     *            (the right-hand operand).
     * @return    the result of the rshift, or null if this operation
     *            is not defined
     **/
    public PyObject __irshift__(PyObject other) { return _rshift(other); }

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
    public PyObject __and__(PyObject other) { return null; }

    /**
     * Equivalent to the standard Python __rand__ method
     * @param     other the object to perform this binary operation with
     *            (the left-hand operand).
     * @return    the result of the and, or null if this operation
     *            is not defined.
     **/
    public PyObject __rand__(PyObject other) { return null; }

    /**
     * Equivalent to the standard Python __iand__ method
     * @param     other the object to perform this binary operation with
     *            (the right-hand operand).
     * @return    the result of the and, or null if this operation
     *            is not defined
     **/
    public PyObject __iand__(PyObject other) { return _and(other); }

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
        throw Py.TypeError(
                 "__and__ nor __rand__ defined for these operands");
    }

    /**
     * Equivalent to the standard Python __or__ method
     * @param     other the object to perform this binary operation with
     *            (the right-hand operand).
     * @return    the result of the or, or null if this operation
     *            is not defined
     **/
    public PyObject __or__(PyObject other) { return null; }

    /**
     * Equivalent to the standard Python __ror__ method
     * @param     other the object to perform this binary operation with
     *            (the left-hand operand).
     * @return    the result of the or, or null if this operation
     *            is not defined.
     **/
    public PyObject __ror__(PyObject other) { return null; }

    /**
     * Equivalent to the standard Python __ior__ method
     * @param     other the object to perform this binary operation with
     *            (the right-hand operand).
     * @return    the result of the or, or null if this operation
     *            is not defined
     **/
    public PyObject __ior__(PyObject other) { return _or(other); }

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
        throw Py.TypeError(
                 "__or__ nor __ror__ defined for these operands");
    }

    /**
     * Equivalent to the standard Python __xor__ method
     * @param     other the object to perform this binary operation with
     *            (the right-hand operand).
     * @return    the result of the xor, or null if this operation
     *            is not defined
     **/
    public PyObject __xor__(PyObject other) { return null; }

    /**
     * Equivalent to the standard Python __rxor__ method
     * @param     other the object to perform this binary operation with
     *            (the left-hand operand).
     * @return    the result of the xor, or null if this operation
     *            is not defined.
     **/
    public PyObject __rxor__(PyObject other) { return null; }

    /**
     * Equivalent to the standard Python __ixor__ method
     * @param     other the object to perform this binary operation with
     *            (the right-hand operand).
     * @return    the result of the xor, or null if this operation
     *            is not defined
     **/
    public PyObject __ixor__(PyObject other) { return _xor(other); }

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
        throw Py.TypeError(
                 "__xor__ nor __rxor__ defined for these operands");
    }

    // Generated by make_binops.py (End)



    /* A convenience function for PyProxy's */
    // Possibly add _jcall(), _jcall(Object, ...) as future optimization
    public PyObject _jcallexc(Object[] args) throws Throwable {
        PyObject[] pargs = new PyObject[args.length];
        try {
            int n = args.length;
            for (int i=0; i<n; i++)
                pargs[i] = Py.java2py(args[i]);
            return __call__(pargs);
        } catch (PyException e) {
            if (e.value instanceof PyJavaInstance) {
                Object t = e.value.__tojava__(Throwable.class);
                if (t != null && t != Py.NoConversion) {
                    throw (Throwable)t;
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
            throw (RuntimeException)t;
        if (t instanceof Error)
            throw (Error)t;
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
       Shortcut for calling a method on a PyObject with no args.

       @param name the name of the method to call.  This must be an
       interned string!
       @return the result of calling the method name with no args
    **/
    public PyObject invoke(String name) {
        PyObject f = __getattr__(name);
        return f.__call__();
    }

    /**
       Shortcut for calling a method on a PyObject with one arg.

       @param name the name of the method to call.  This must be an
       interned string!
       @param arg1 the one argument of the method.
       @return the result of calling the method name with arg1
    **/
    public PyObject invoke(String name, PyObject arg1) {
        PyObject f = __getattr__(name);
        return f.__call__(arg1);
    }

    /**
       Shortcut for calling a method on a PyObject with two args.

       @param name the name of the method to call.  This must be an
       interned string!
       @param arg1 the first argument of the method.
       @param arg2 the second argument of the method.
       @return the result of calling the method name with arg1 and arg2
    **/
    public PyObject invoke(String name, PyObject arg1, PyObject arg2) {
        PyObject f = __getattr__(name);
        return f.__call__(arg1, arg2);
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

        for (len--; len>=0; len--) {
            y = System.identityHashCode(list[len]);
            x = (x + x + x) ^ y;
        }
        x ^= list.length;
        return x;
    }

    public boolean equals(Object o) {
        if (!(o instanceof PyIdentityTuple))
            return false;
        PyIdentityTuple that = (PyIdentityTuple)o;
        if (list.length != that.list.length)
            return false;
        for (int i = 0; i < list.length; i++) {
            if (list[i] != that.list[i])
                return false;
        }
        return true;
    }

    // __class__ boilerplate -- see PyObject for details
    public static PyClass __class__;
    protected PyClass getPyClass() { return __class__; }
}
