package org.python.core;

/**
All objects known to the JPython runtime system are represented
by an instance of the class <code>PyObject</code> or one of
its subclasses.

@author Jim Hugunin - hugunin@python.org
@version 1.0, 3/24/98
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
    private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, ClassNotFoundException {
        in.defaultReadObject();
        __class__ = PyJavaClass.lookup(getClass());
    }

    // A package private constructor used by PyJavaClass
    PyObject(boolean fakeArgument) {
        __class__ = (PyClass)this;
    }


    /**
    The standard constructor for a <code>PyObject</code>.
    It will set the <code>__class__</code> field to correspond to the specific subclass
    of <code>PyObject</code> being instantiated.
    **/
    public PyObject() {
        __class__ = PyJavaClass.lookup(getClass());
    }

    /**
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
    
    @param c a <code>PyClass</code> instance giving the <code>__class__</code>
                of the new <code>PyObject</code>
    **/
    public PyObject(PyClass c) {
        if (c == null) {
            c = PyJavaClass.lookup(getClass());
        }
        __class__ = c;
    }

    /**
    Equivalent to the standard Python __repr__ method.
    This method should not typically need to be overrriden.
    The easiest way to configure the string representation of a <code>PyObject</code>
    is to override the standard Java <code>toString</code> method.
    **/
	public PyString __repr__() {
		return new PyString(toString());
	}
	
	String safeRepr() {
	    if (__class__ == null) {
	        return "unknown object";
	    }
	    String name = __class__.__name__;
	    if (name == null) return "unknown object";
	    
	    if ((name.equals("org.python.core.PyClass") || name.equals("org.python.core.PyJavaClass"))
	      && (this instanceof PyClass)) {
	        name = ((PyClass)this).__name__;
	        if (name == null) return "unknown class";
	        return "class '"+name+"'";
	    }
	    
	    return "instance of '"+name+"'";
	}
	
	/**
	Equivalent to the standard Python __str__ method.
	This method should not typically need to be overridden.
    The easiest way to configure the string representation of a <code>PyObject</code>
    is to override the standard Java <code>toString</code> method.
    **/
	public PyString __str__() {
		return __repr__();
	}

	/**
	Equivalent to the standard Python __hash__ method.
	This method can not be overridden.
	Instead, you should override the standard Java <code>hashCode</code> method
	to return an appropriate hash code for the <code>PyObject</code>.
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
		return _cmp((PyObject)ob_other) == 0;
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
		if (c.isInstance(this)) return this;
		return Py.NoConversion;
	}

    /**
    The basic method to override when implementing a callable object.
    
    The first len(args)-len(keywords) members of args[] are plain arguments.
    The last len(keywords) arguments are the values of the keyword arguments.
    
    @param args     all arguments to the function (including keyword arguments).
    @param keywords the keywords used for all keyword arguments.
    **/
	public PyObject __call__(PyObject args[], String keywords[]) {
		throw Py.AttributeError("__call__ not implemented by "+safeRepr());
	}

    /**
    A variant of the __call__ method with one extra initial argument.
    This variant is used to allow method invocations to be performed efficiently.
    
    The default behavior is to invoke <code>__call__(args, keywords)</code>
    with the appropriate arguments.
    The only reason to override this function would be for improved performance.
    
    @param arg1     the first argument to the function.
    @param args     the last arguments to the function (including keyword arguments).
    @param keywords the keywords used for all keyword arguments.    
    **/
	public PyObject __call__(PyObject arg1, PyObject args[], String keywords[]) {
	    PyObject[] newArgs = new PyObject[args.length+1];
	    System.arraycopy(args, 0, newArgs, 1, args.length);
	    newArgs[0] = arg1;
		return __call__(newArgs, keywords);
	}

    /**
    A variant of the __call__ method when no keywords are passed.
    The default behavior is to invoke <code>__call__(args, keywords)</code>
    with the appropriate arguments.
    The only reason to override this function would be for improved performance.
    
    @param args     all arguments to the function.
    **/
	public PyObject __call__(PyObject args[]) {
		return __call__(args, Py.NoKeywords);
	}

    /**
    A variant of the __call__ method with no arguments.
    The default behavior is to invoke <code>__call__(args, keywords)</code>
    with the appropriate arguments.
    The only reason to override this function would be for improved performance.
    **/
	public PyObject __call__() {
		return __call__(Py.EmptyObjects, Py.NoKeywords);
	}

    /**
    A variant of the __call__ method with one argument.
    The default behavior is to invoke <code>__call__(args, keywords)</code>
    with the appropriate arguments.
    The only reason to override this function would be for improved performance.
    
    @param arg0     the single argument to the function.
    **/
	public PyObject __call__(PyObject arg0) {
		return __call__(new PyObject[] {arg0}, Py.NoKeywords);
	}

    /**
    A variant of the __call__ method with two arguments.
    The default behavior is to invoke <code>__call__(args, keywords)</code>
    with the appropriate arguments.
    The only reason to override this function would be for improved performance.
    
    @param arg0     the first argument to the function.
    @param arg1     the second argument to the function.
    **/
    public PyObject __call__(PyObject arg0, PyObject arg1) {
		return __call__(new PyObject[] {arg0, arg1}, Py.NoKeywords);
	}

    /**
    A variant of the __call__ method with three arguments.
    The default behavior is to invoke <code>__call__(args, keywords)</code>
    with the appropriate arguments.
    The only reason to override this function would be for improved performance.
    
    @param arg0     the first argument to the function.
    @param arg1     the second argument to the function.
    @param arg2     the third argument to the function.
    **/
    public PyObject __call__(PyObject arg0, PyObject arg1, PyObject arg2) {
		return __call__(new PyObject[] {arg0, arg1, arg2}, Py.NoKeywords);
	}

    /**
    A variant of the __call__ method with four arguments.
    The default behavior is to invoke <code>__call__(args, keywords)</code>
    with the appropriate arguments.
    The only reason to override this function would be for improved performance.
    
    @param arg0     the first argument to the function.
    @param arg1     the second argument to the function.
    @param arg2     the third argument to the function.
    @param arg3     the fourth argument to the function.
    **/
    public PyObject __call__(PyObject arg0, PyObject arg1, PyObject arg2, PyObject arg3) {
		return __call__(new PyObject[] {arg0, arg1, arg2, arg3}, Py.NoKeywords);
	}
	
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
    <code>int</code> as the key.
    By default, this method will call <code>__finditem__(PyObject key)</code>
    with the appropriate args.
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
    <code>String</code> as the key.
    By default, this method will call <code>__finditem__(PyObject key)</code>
    with the appropriate args.
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
		if (ret == null) throw Py.KeyError(""+key);
		return ret;
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
		if (ret == null) throw Py.KeyError(key.toString());
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
    By default, this method will call <code>__findattr__(PyString name)</code>
    with the appropriate args.
    The only reason to override this method is for performance.
    
    <b>Warning: name must be an interned string!!!!!!!!</b>
    
    @param name the name to lookup in this namespace
                    <b> must be an interned string </b>.
    @return the value corresponding to name or null if name is not found
    
    
    @see org.python.core.PyObject#__findattr__(org.python.core.PyString)
    **/	
	public PyObject __findattr__(String name) {
		if (__class__ == null) return null;
		if (name == "__class__") return __class__;
		PyObject ret = __class__.lookup(name, false);
		if (ret != null) return ret._doget(this);
		return null;
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
		if (ret == null) throw Py.AttributeError(safeRepr()+" has no attribute '"+name+"'");
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
		if (ret == null) throw Py.AttributeError(safeRepr()+" has no attribute '"+name+"'");
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
	public void __setattr__(String name, PyObject value) throws PyException {
		throw Py.TypeError("readonly class");
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


    public PyObject _doget(PyObject container) {
        return this;
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
		if (o == null) throw Py.AttributeError("__coerce__");
		if (o == Py.None) return (PyObject)o;
		if (o instanceof PyObject[]) return new PyTuple((PyObject[])o);
		else return new PyTuple(new PyObject[] {this, (PyObject)o});
	}

	/* The basic comparision operations */
	/**
	Equivalent to the standard Python __cmp__ method.
	
	@param other the object to compare this with.
	@return -1 if this < 0; 0 if this == o; +1 if this > o; -2 if no comparison is implemented
	**/
	public int __cmp__(PyObject other) {
		return -2;
	}

    /**
    Implements cmp(this, other)
    
    @param other the object to compare this with.
    @return -1 if this < 0; 0 if this == o; +1 if this > o
    **/
	public final int _cmp(PyObject o2_in) {
		PyObject o2 = o2_in;
		PyObject o1 = this;
		int itmp;
		Object ctmp;
		if (o1.__class__ != o2.__class__) {
			ctmp=o1.__coerce_ex__(o2);
			if (ctmp != null) {
				if (ctmp instanceof PyObject[]) { o1 = ((PyObject[])ctmp)[0]; o2 = ((PyObject[])ctmp)[1];
				} else { o2 = (PyObject)ctmp; }
			}
		} else ctmp = null;
		if (ctmp != Py.None && (itmp = o1.__cmp__(o2)) != -2) return itmp;
		o1 = this;
		o2 = o2_in;
		if (o1.__class__ != o2.__class__) {
			ctmp=o2.__coerce_ex__(o1);
			if (ctmp != null) {
				if (ctmp instanceof PyObject[]) { o2 = ((PyObject[])ctmp)[0]; o1 = ((PyObject[])ctmp)[1];
				} else { o1 = (PyObject)ctmp; }
			}
		}
		if (ctmp != Py.None && (itmp = o2.__cmp__(o1)) != -2) return -itmp;
		else {
			if (this == o2_in) return 0;
			else {
			    return Py.id(this) < Py.id(o2_in) ? -1 : 1;
			}
		}
	}

	public final PyObject _eq(PyObject o) {
		return _cmp(o) == 0 ? Py.One : Py.Zero;
	}
	public final PyObject _ne(PyObject o) {
		return _cmp(o) != 0 ? Py.One : Py.Zero;
	}
	public final PyObject _le(PyObject o) {
		return _cmp(o) <= 0 ? Py.One : Py.Zero;
	}
	public final PyObject _lt(PyObject o) {
		return _cmp(o) < 0 ? Py.One : Py.Zero;
	}
	public final PyObject _ge(PyObject o) {
		return _cmp(o) >= 0 ? Py.One : Py.Zero;
	}
	public final PyObject _gt(PyObject o) {
		return _cmp(o) > 0 ? Py.One : Py.Zero;
	}

	public final PyObject _is(PyObject o) {
		return this == o ? Py.One : Py.Zero;
	}
	public final PyObject _isnot(PyObject o) {
		return this != o ? Py.One : Py.Zero;
	}

	public final PyObject _in(PyObject o) {
		PyObject tmp;
		int i = 0;

		while ((tmp = o.__finditem__(i++)) != null) {
			if (_eq(tmp).__nonzero__()) return Py.One;
		}

		return Py.Zero;
	}

	public final PyObject _notin(PyObject o) {
	    return _in(o).__not__();
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
    @param o3 the modulus to perform this operation in or null if no modulo is to be used
    @return this object raised to the given power in the given modulus
    **/
	public PyObject __pow__(PyObject o2, PyObject o3) throws PyException { return null; }

    /* This code is generated by make_binops.py */
	/**Equivalent to the standard Python __add__ method
	@param other the object to perform this binary operation with (the right-hand operand).
	@return the result of the add, or null if this operation is not defined
	**/
	public PyObject __add__(PyObject other) { return null; }
	
	/**Equivalent to the standard Python __radd__ method
	@param other the object to perform this binary operation with (the left-hand operand).
	@return the result of the add, or null if this operation is not defined.
	**/
	public PyObject __radd__(PyObject other) { return null; }

	/**Implements the Python expression <code>this + other</code>
	@param other the object to perform this binary operation with.
	@return the result of the add.
	@exception PyTypeError if this operation can't be performed with these operands.
	**/
	public final PyObject _add(PyObject o2_in) {
		PyObject o2 = o2_in;
		PyObject o1 = this;
		Object ctmp;
		if (o1.__class__ != o2.__class__) {
			ctmp=o1.__coerce_ex__(o2);
			if (ctmp != null) {
				if (ctmp instanceof PyObject[]) { o1 = ((PyObject[])ctmp)[0]; o2 = ((PyObject[])ctmp)[1];
				} else { o2 = (PyObject)ctmp; }
			}
		} else ctmp = null;
		if (ctmp != Py.None && (o1 = o1.__add__(o2)) != null) return o1;
		o1 = this;
		o2 = o2_in;
		if (o1.__class__ != o2.__class__) {
			ctmp=o2.__coerce_ex__(o1);
			if (ctmp != null) {
				if (ctmp instanceof PyObject[]) { o2 = ((PyObject[])ctmp)[0]; o1 = ((PyObject[])ctmp)[1];
				} else { o1 = (PyObject)ctmp; }
			}
		}
		if (ctmp != Py.None) {
			if (o1.__class__ == o2.__class__) o1 = o1.__add__(o2);
			else o1 = o2.__radd__(o1);
			if (o1 != null) return o1;
		}
		throw Py.TypeError("__add__ nor __radd__ defined for these operands");
	}

	/**Equivalent to the standard Python __sub__ method
	@param other the object to perform this binary operation with (the right-hand operand).
	@return the result of the sub, or null if this operation is not defined
	**/
	public PyObject __sub__(PyObject other) { return null; }
	
	/**Equivalent to the standard Python __rsub__ method
	@param other the object to perform this binary operation with (the left-hand operand).
	@return the result of the sub, or null if this operation is not defined.
	**/
	public PyObject __rsub__(PyObject other) { return null; }

	/**Implements the Python expression <code>this - other</code>
	@param other the object to perform this binary operation with.
	@return the result of the sub.
	@exception PyTypeError if this operation can't be performed with these operands.
	**/
	public final PyObject _sub(PyObject o2_in) {
		PyObject o2 = o2_in;
		PyObject o1 = this;
		Object ctmp;
		if (o1.__class__ != o2.__class__) {
			ctmp=o1.__coerce_ex__(o2);
			if (ctmp != null) {
				if (ctmp instanceof PyObject[]) { o1 = ((PyObject[])ctmp)[0]; o2 = ((PyObject[])ctmp)[1];
				} else { o2 = (PyObject)ctmp; }
			}
		} else ctmp = null;
		if (ctmp != Py.None && (o1 = o1.__sub__(o2)) != null) return o1;
		o1 = this;
		o2 = o2_in;
		if (o1.__class__ != o2.__class__) {
			ctmp=o2.__coerce_ex__(o1);
			if (ctmp != null) {
				if (ctmp instanceof PyObject[]) { o2 = ((PyObject[])ctmp)[0]; o1 = ((PyObject[])ctmp)[1];
				} else { o1 = (PyObject)ctmp; }
			}
		}
		if (ctmp != Py.None) {
			if (o1.__class__ == o2.__class__) o1 = o1.__sub__(o2);
			else o1 = o2.__rsub__(o1);
			if (o1 != null) return o1;
		}
		throw Py.TypeError("__sub__ nor __rsub__ defined for these operands");
	}

	/**Equivalent to the standard Python __mul__ method
	@param other the object to perform this binary operation with (the right-hand operand).
	@return the result of the mul, or null if this operation is not defined
	**/
	public PyObject __mul__(PyObject other) { return null; }
	
	/**Equivalent to the standard Python __rmul__ method
	@param other the object to perform this binary operation with (the left-hand operand).
	@return the result of the mul, or null if this operation is not defined.
	**/
	public PyObject __rmul__(PyObject other) { return null; }

	/**Implements the Python expression <code>this * other</code>
	@param other the object to perform this binary operation with.
	@return the result of the mul.
	@exception PyTypeError if this operation can't be performed with these operands.
	**/
	public final PyObject _mul(PyObject o2_in) {
		PyObject o2 = o2_in;
		PyObject o1 = this;
		Object ctmp;
		if (o1.__class__ != o2.__class__) {
			ctmp=o1.__coerce_ex__(o2);
			if (ctmp != null) {
				if (ctmp instanceof PyObject[]) { o1 = ((PyObject[])ctmp)[0]; o2 = ((PyObject[])ctmp)[1];
				} else { o2 = (PyObject)ctmp; }
			}
		} else ctmp = null;
		if (ctmp != Py.None && (o1 = o1.__mul__(o2)) != null) return o1;
		o1 = this;
		o2 = o2_in;
		if (o1.__class__ != o2.__class__) {
			ctmp=o2.__coerce_ex__(o1);
			if (ctmp != null) {
				if (ctmp instanceof PyObject[]) { o2 = ((PyObject[])ctmp)[0]; o1 = ((PyObject[])ctmp)[1];
				} else { o1 = (PyObject)ctmp; }
			}
		}
		if (ctmp != Py.None) {
			if (o1.__class__ == o2.__class__) o1 = o1.__mul__(o2);
			else o1 = o2.__rmul__(o1);
			if (o1 != null) return o1;
		}
		throw Py.TypeError("__mul__ nor __rmul__ defined for these operands");
	}

	/**Equivalent to the standard Python __div__ method
	@param other the object to perform this binary operation with (the right-hand operand).
	@return the result of the div, or null if this operation is not defined
	**/
	public PyObject __div__(PyObject other) { return null; }
	
	/**Equivalent to the standard Python __rdiv__ method
	@param other the object to perform this binary operation with (the left-hand operand).
	@return the result of the div, or null if this operation is not defined.
	**/
	public PyObject __rdiv__(PyObject other) { return null; }

	/**Implements the Python expression <code>this / other</code>
	@param other the object to perform this binary operation with.
	@return the result of the div.
	@exception PyTypeError if this operation can't be performed with these operands.
	**/
	public final PyObject _div(PyObject o2_in) {
		PyObject o2 = o2_in;
		PyObject o1 = this;
		Object ctmp;
		if (o1.__class__ != o2.__class__) {
			ctmp=o1.__coerce_ex__(o2);
			if (ctmp != null) {
				if (ctmp instanceof PyObject[]) { o1 = ((PyObject[])ctmp)[0]; o2 = ((PyObject[])ctmp)[1];
				} else { o2 = (PyObject)ctmp; }
			}
		} else ctmp = null;
		if (ctmp != Py.None && (o1 = o1.__div__(o2)) != null) return o1;
		o1 = this;
		o2 = o2_in;
		if (o1.__class__ != o2.__class__) {
			ctmp=o2.__coerce_ex__(o1);
			if (ctmp != null) {
				if (ctmp instanceof PyObject[]) { o2 = ((PyObject[])ctmp)[0]; o1 = ((PyObject[])ctmp)[1];
				} else { o1 = (PyObject)ctmp; }
			}
		}
		if (ctmp != Py.None) {
			if (o1.__class__ == o2.__class__) o1 = o1.__div__(o2);
			else o1 = o2.__rdiv__(o1);
			if (o1 != null) return o1;
		}
		throw Py.TypeError("__div__ nor __rdiv__ defined for these operands");
	}

	/**Equivalent to the standard Python __mod__ method
	@param other the object to perform this binary operation with (the right-hand operand).
	@return the result of the mod, or null if this operation is not defined
	**/
	public PyObject __mod__(PyObject other) { return null; }
	
	/**Equivalent to the standard Python __rmod__ method
	@param other the object to perform this binary operation with (the left-hand operand).
	@return the result of the mod, or null if this operation is not defined.
	**/
	public PyObject __rmod__(PyObject other) { return null; }

	/**Implements the Python expression <code>this % other</code>
	@param other the object to perform this binary operation with.
	@return the result of the mod.
	@exception PyTypeError if this operation can't be performed with these operands.
	**/
	public final PyObject _mod(PyObject o2_in) {
		PyObject o2 = o2_in;
		PyObject o1 = this;
		Object ctmp;
		if (o1.__class__ != o2.__class__) {
			ctmp=o1.__coerce_ex__(o2);
			if (ctmp != null) {
				if (ctmp instanceof PyObject[]) { o1 = ((PyObject[])ctmp)[0]; o2 = ((PyObject[])ctmp)[1];
				} else { o2 = (PyObject)ctmp; }
			}
		} else ctmp = null;
		if (ctmp != Py.None && (o1 = o1.__mod__(o2)) != null) return o1;
		o1 = this;
		o2 = o2_in;
		if (o1.__class__ != o2.__class__) {
			ctmp=o2.__coerce_ex__(o1);
			if (ctmp != null) {
				if (ctmp instanceof PyObject[]) { o2 = ((PyObject[])ctmp)[0]; o1 = ((PyObject[])ctmp)[1];
				} else { o1 = (PyObject)ctmp; }
			}
		}
		if (ctmp != Py.None) {
			if (o1.__class__ == o2.__class__) o1 = o1.__mod__(o2);
			else o1 = o2.__rmod__(o1);
			if (o1 != null) return o1;
		}
		throw Py.TypeError("__mod__ nor __rmod__ defined for these operands");
	}

	/**Equivalent to the standard Python __divmod__ method
	@param other the object to perform this binary operation with (the right-hand operand).
	@return the result of the divmod, or null if this operation is not defined
	**/
	public PyObject __divmod__(PyObject other) { return null; }
	
	/**Equivalent to the standard Python __rdivmod__ method
	@param other the object to perform this binary operation with (the left-hand operand).
	@return the result of the divmod, or null if this operation is not defined.
	**/
	public PyObject __rdivmod__(PyObject other) { return null; }

	/**Implements the Python expression <code>this divmod other</code>
	@param other the object to perform this binary operation with.
	@return the result of the divmod.
	@exception PyTypeError if this operation can't be performed with these operands.
	**/
	public final PyObject _divmod(PyObject o2_in) {
		PyObject o2 = o2_in;
		PyObject o1 = this;
		Object ctmp;
		if (o1.__class__ != o2.__class__) {
			ctmp=o1.__coerce_ex__(o2);
			if (ctmp != null) {
				if (ctmp instanceof PyObject[]) { o1 = ((PyObject[])ctmp)[0]; o2 = ((PyObject[])ctmp)[1];
				} else { o2 = (PyObject)ctmp; }
			}
		} else ctmp = null;
		if (ctmp != Py.None && (o1 = o1.__divmod__(o2)) != null) return o1;
		o1 = this;
		o2 = o2_in;
		if (o1.__class__ != o2.__class__) {
			ctmp=o2.__coerce_ex__(o1);
			if (ctmp != null) {
				if (ctmp instanceof PyObject[]) { o2 = ((PyObject[])ctmp)[0]; o1 = ((PyObject[])ctmp)[1];
				} else { o1 = (PyObject)ctmp; }
			}
		}
		if (ctmp != Py.None) {
			if (o1.__class__ == o2.__class__) o1 = o1.__divmod__(o2);
			else o1 = o2.__rdivmod__(o1);
			if (o1 != null) return o1;
		}
		throw Py.TypeError("__divmod__ nor __rdivmod__ defined for these operands");
	}

	/**Equivalent to the standard Python __pow__ method
	@param other the object to perform this binary operation with (the right-hand operand).
	@return the result of the pow, or null if this operation is not defined
	**/
	public PyObject __pow__(PyObject other) { return __pow__(other, null); }
	
	/**Equivalent to the standard Python __rpow__ method
	@param other the object to perform this binary operation with (the left-hand operand).
	@return the result of the pow, or null if this operation is not defined.
	**/
	public PyObject __rpow__(PyObject other) { return null; }

	/**Implements the Python expression <code>this ** other</code>
	@param other the object to perform this binary operation with.
	@return the result of the pow.
	@exception PyTypeError if this operation can't be performed with these operands.
	**/
	public final PyObject _pow(PyObject o2_in) {
		PyObject o2 = o2_in;
		PyObject o1 = this;
		Object ctmp;
		if (o1.__class__ != o2.__class__) {
			ctmp=o1.__coerce_ex__(o2);
			if (ctmp != null) {
				if (ctmp instanceof PyObject[]) { o1 = ((PyObject[])ctmp)[0]; o2 = ((PyObject[])ctmp)[1];
				} else { o2 = (PyObject)ctmp; }
			}
		} else ctmp = null;
		if (ctmp != Py.None && (o1 = o1.__pow__(o2)) != null) return o1;
		o1 = this;
		o2 = o2_in;
		if (o1.__class__ != o2.__class__) {
			ctmp=o2.__coerce_ex__(o1);
			if (ctmp != null) {
				if (ctmp instanceof PyObject[]) { o2 = ((PyObject[])ctmp)[0]; o1 = ((PyObject[])ctmp)[1];
				} else { o1 = (PyObject)ctmp; }
			}
		}
		if (ctmp != Py.None) {
			if (o1.__class__ == o2.__class__) o1 = o1.__pow__(o2);
			else o1 = o2.__rpow__(o1);
			if (o1 != null) return o1;
		}
		throw Py.TypeError("__pow__ nor __rpow__ defined for these operands");
	}

	/**Equivalent to the standard Python __lshift__ method
	@param other the object to perform this binary operation with (the right-hand operand).
	@return the result of the lshift, or null if this operation is not defined
	**/
	public PyObject __lshift__(PyObject other) { return null; }
	
	/**Equivalent to the standard Python __rlshift__ method
	@param other the object to perform this binary operation with (the left-hand operand).
	@return the result of the lshift, or null if this operation is not defined.
	**/
	public PyObject __rlshift__(PyObject other) { return null; }

	/**Implements the Python expression <code>this << other</code>
	@param other the object to perform this binary operation with.
	@return the result of the lshift.
	@exception PyTypeError if this operation can't be performed with these operands.
	**/
	public final PyObject _lshift(PyObject o2_in) {
		PyObject o2 = o2_in;
		PyObject o1 = this;
		Object ctmp;
		if (o1.__class__ != o2.__class__) {
			ctmp=o1.__coerce_ex__(o2);
			if (ctmp != null) {
				if (ctmp instanceof PyObject[]) { o1 = ((PyObject[])ctmp)[0]; o2 = ((PyObject[])ctmp)[1];
				} else { o2 = (PyObject)ctmp; }
			}
		} else ctmp = null;
		if (ctmp != Py.None && (o1 = o1.__lshift__(o2)) != null) return o1;
		o1 = this;
		o2 = o2_in;
		if (o1.__class__ != o2.__class__) {
			ctmp=o2.__coerce_ex__(o1);
			if (ctmp != null) {
				if (ctmp instanceof PyObject[]) { o2 = ((PyObject[])ctmp)[0]; o1 = ((PyObject[])ctmp)[1];
				} else { o1 = (PyObject)ctmp; }
			}
		}
		if (ctmp != Py.None) {
			if (o1.__class__ == o2.__class__) o1 = o1.__lshift__(o2);
			else o1 = o2.__rlshift__(o1);
			if (o1 != null) return o1;
		}
		throw Py.TypeError("__lshift__ nor __rlshift__ defined for these operands");
	}

	/**Equivalent to the standard Python __rshift__ method
	@param other the object to perform this binary operation with (the right-hand operand).
	@return the result of the rshift, or null if this operation is not defined
	**/
	public PyObject __rshift__(PyObject other) { return null; }
	
	/**Equivalent to the standard Python __rrshift__ method
	@param other the object to perform this binary operation with (the left-hand operand).
	@return the result of the rshift, or null if this operation is not defined.
	**/
	public PyObject __rrshift__(PyObject other) { return null; }

	/**Implements the Python expression <code>this >> other</code>
	@param other the object to perform this binary operation with.
	@return the result of the rshift.
	@exception PyTypeError if this operation can't be performed with these operands.
	**/
	public final PyObject _rshift(PyObject o2_in) {
		PyObject o2 = o2_in;
		PyObject o1 = this;
		Object ctmp;
		if (o1.__class__ != o2.__class__) {
			ctmp=o1.__coerce_ex__(o2);
			if (ctmp != null) {
				if (ctmp instanceof PyObject[]) { o1 = ((PyObject[])ctmp)[0]; o2 = ((PyObject[])ctmp)[1];
				} else { o2 = (PyObject)ctmp; }
			}
		} else ctmp = null;
		if (ctmp != Py.None && (o1 = o1.__rshift__(o2)) != null) return o1;
		o1 = this;
		o2 = o2_in;
		if (o1.__class__ != o2.__class__) {
			ctmp=o2.__coerce_ex__(o1);
			if (ctmp != null) {
				if (ctmp instanceof PyObject[]) { o2 = ((PyObject[])ctmp)[0]; o1 = ((PyObject[])ctmp)[1];
				} else { o1 = (PyObject)ctmp; }
			}
		}
		if (ctmp != Py.None) {
			if (o1.__class__ == o2.__class__) o1 = o1.__rshift__(o2);
			else o1 = o2.__rrshift__(o1);
			if (o1 != null) return o1;
		}
		throw Py.TypeError("__rshift__ nor __rrshift__ defined for these operands");
	}

	/**Equivalent to the standard Python __and__ method
	@param other the object to perform this binary operation with (the right-hand operand).
	@return the result of the and, or null if this operation is not defined
	**/
	public PyObject __and__(PyObject other) { return null; }
	
	/**Equivalent to the standard Python __rand__ method
	@param other the object to perform this binary operation with (the left-hand operand).
	@return the result of the and, or null if this operation is not defined.
	**/
	public PyObject __rand__(PyObject other) { return null; }

	/**Implements the Python expression <code>this & other</code>
	@param other the object to perform this binary operation with.
	@return the result of the and.
	@exception PyTypeError if this operation can't be performed with these operands.
	**/
	public final PyObject _and(PyObject o2_in) {
		PyObject o2 = o2_in;
		PyObject o1 = this;
		Object ctmp;
		if (o1.__class__ != o2.__class__) {
			ctmp=o1.__coerce_ex__(o2);
			if (ctmp != null) {
				if (ctmp instanceof PyObject[]) { o1 = ((PyObject[])ctmp)[0]; o2 = ((PyObject[])ctmp)[1];
				} else { o2 = (PyObject)ctmp; }
			}
		} else ctmp = null;
		if (ctmp != Py.None && (o1 = o1.__and__(o2)) != null) return o1;
		o1 = this;
		o2 = o2_in;
		if (o1.__class__ != o2.__class__) {
			ctmp=o2.__coerce_ex__(o1);
			if (ctmp != null) {
				if (ctmp instanceof PyObject[]) { o2 = ((PyObject[])ctmp)[0]; o1 = ((PyObject[])ctmp)[1];
				} else { o1 = (PyObject)ctmp; }
			}
		}
		if (ctmp != Py.None) {
			if (o1.__class__ == o2.__class__) o1 = o1.__and__(o2);
			else o1 = o2.__rand__(o1);
			if (o1 != null) return o1;
		}
		throw Py.TypeError("__and__ nor __rand__ defined for these operands");
	}

	/**Equivalent to the standard Python __or__ method
	@param other the object to perform this binary operation with (the right-hand operand).
	@return the result of the or, or null if this operation is not defined
	**/
	public PyObject __or__(PyObject other) { return null; }
	
	/**Equivalent to the standard Python __ror__ method
	@param other the object to perform this binary operation with (the left-hand operand).
	@return the result of the or, or null if this operation is not defined.
	**/
	public PyObject __ror__(PyObject other) { return null; }

	/**Implements the Python expression <code>this | other</code>
	@param other the object to perform this binary operation with.
	@return the result of the or.
	@exception PyTypeError if this operation can't be performed with these operands.
	**/
	public final PyObject _or(PyObject o2_in) {
		PyObject o2 = o2_in;
		PyObject o1 = this;
		Object ctmp;
		if (o1.__class__ != o2.__class__) {
			ctmp=o1.__coerce_ex__(o2);
			if (ctmp != null) {
				if (ctmp instanceof PyObject[]) { o1 = ((PyObject[])ctmp)[0]; o2 = ((PyObject[])ctmp)[1];
				} else { o2 = (PyObject)ctmp; }
			}
		} else ctmp = null;
		if (ctmp != Py.None && (o1 = o1.__or__(o2)) != null) return o1;
		o1 = this;
		o2 = o2_in;
		if (o1.__class__ != o2.__class__) {
			ctmp=o2.__coerce_ex__(o1);
			if (ctmp != null) {
				if (ctmp instanceof PyObject[]) { o2 = ((PyObject[])ctmp)[0]; o1 = ((PyObject[])ctmp)[1];
				} else { o1 = (PyObject)ctmp; }
			}
		}
		if (ctmp != Py.None) {
			if (o1.__class__ == o2.__class__) o1 = o1.__or__(o2);
			else o1 = o2.__ror__(o1);
			if (o1 != null) return o1;
		}
		throw Py.TypeError("__or__ nor __ror__ defined for these operands");
	}

	/**Equivalent to the standard Python __xor__ method
	@param other the object to perform this binary operation with (the right-hand operand).
	@return the result of the xor, or null if this operation is not defined
	**/
	public PyObject __xor__(PyObject other) { return null; }
	
	/**Equivalent to the standard Python __rxor__ method
	@param other the object to perform this binary operation with (the left-hand operand).
	@return the result of the xor, or null if this operation is not defined.
	**/
	public PyObject __rxor__(PyObject other) { return null; }

	/**Implements the Python expression <code>this ^ other</code>
	@param other the object to perform this binary operation with.
	@return the result of the xor.
	@exception PyTypeError if this operation can't be performed with these operands.
	**/
	public final PyObject _xor(PyObject o2_in) {
		PyObject o2 = o2_in;
		PyObject o1 = this;
		Object ctmp;
		if (o1.__class__ != o2.__class__) {
			ctmp=o1.__coerce_ex__(o2);
			if (ctmp != null) {
				if (ctmp instanceof PyObject[]) { o1 = ((PyObject[])ctmp)[0]; o2 = ((PyObject[])ctmp)[1];
				} else { o2 = (PyObject)ctmp; }
			}
		} else ctmp = null;
		if (ctmp != Py.None && (o1 = o1.__xor__(o2)) != null) return o1;
		o1 = this;
		o2 = o2_in;
		if (o1.__class__ != o2.__class__) {
			ctmp=o2.__coerce_ex__(o1);
			if (ctmp != null) {
				if (ctmp instanceof PyObject[]) { o2 = ((PyObject[])ctmp)[0]; o1 = ((PyObject[])ctmp)[1];
				} else { o1 = (PyObject)ctmp; }
			}
		}
		if (ctmp != Py.None) {
			if (o1.__class__ == o2.__class__) o1 = o1.__xor__(o2);
			else o1 = o2.__rxor__(o1);
			if (o1 != null) return o1;
		}
		throw Py.TypeError("__xor__ nor __rxor__ defined for these operands");
	}
	
    /* A convenience function for PyProxy's */
    // Possibly add _jcall(), _jcall(Object, ...) as future optimization
    public PyObject _jcall(Object[] args) throws Throwable {
		PyObject[] pargs = new PyObject[args.length];
		try {
		    int n = args.length;
			for(int i=0; i<n; i++) pargs[i] = Py.java2py(args[i]);
			return __call__(pargs);
		} catch (PyException e) {
		    if (e.value instanceof PyJavaInstance) {
		        Throwable t = (Throwable)e.value.__tojava__(Throwable.class);
		        if (t != null) throw t;
		    } else {
		        ThreadState ts = Py.getThreadState();
		        if (ts.frame == null) {
		            Py.maybeSystemExit(e);
		        }
		            
		        //System.err.println("frame: "+ts.frame); //+", "+ts.frame.f_back);
                PyObject obj = ts.interp.sysdict.__finditem__("printJCallExceptions");
                if (obj != null && obj.__nonzero__()) {
                    Py.stderr.println("Caught python exception in jcall:");
                    Py.printException(e);
                }
            }
		    throw e;
		}
    }
	
	/* Shortcut methods for calling methods from Java */
	
	/** Shortcut for calling a method on a PyObject from Java.
	This form is equivalent to o.__getattr__(name).__call__(args, keywords)
	
	@param name the name of the method to call.  This must be an interned string!
	@param args an array of the arguments to the call.
	@param keywords the keywords to use in the call.
	@return the result of calling the method name with args and keywords.
	**/
	public PyObject invoke(String name, PyObject[] args, String[] keywords) {
	    PyObject f = __getattr__(name);
	    return f.__call__(args, keywords);
	}
	
	/**
	Shortcut for calling a method on a PyObject with no args.
	
	@param name the name of the method to call.  This must be an interned string!
	@return the result of calling the method name with no args
	**/
	public PyObject invoke(String name) {
	    PyObject f = __getattr__(name);
	    return f.__call__();
	}
	
	/**
	Shortcut for calling a method on a PyObject with one arg.
	
	@param name the name of the method to call.  This must be an interned string!
	@param arg1 the one argument of the method.
	@return the result of calling the method name with arg1
	**/
	public PyObject invoke(String name, PyObject arg1) {
	    PyObject f = __getattr__(name);
	    return f.__call__(arg1);
	}
	
	/**
	Shortcut for calling a method on a PyObject with two args.
	
	@param name the name of the method to call.  This must be an interned string!
	@param arg1 the first argument of the method.
	@param arg2 the second argument of the method.
	@return the result of calling the method name with arg1 and arg2
	**/
	public PyObject invoke(String name, PyObject arg1, PyObject arg2) {
	    PyObject f = __getattr__(name);
	    return f.__call__(arg1, arg2);
	}
}

