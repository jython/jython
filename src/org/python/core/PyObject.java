// Copyright (c) Corporation for National Research Initiatives
package org.python.core;

import java.io.Serializable;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.python.expose.ExposedDelete;
import org.python.expose.ExposedGet;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedSet;
import org.python.expose.ExposedType;

/**
 * All objects known to the Jython runtime system are represented by an instance
 * of the class <code>PyObject</code> or one of its subclasses.
 */
@ExposedType(name = "object")
public class PyObject implements Serializable {

    public static final PyType TYPE = PyType.fromClass(PyObject.class);

    @ExposedNew
    @ExposedMethod
    final void object___init__(PyObject[] args, String[] keywords) {
// XXX: attempted fix for object(foo=1), etc
// XXX: this doesn't work for metaclasses, for some reason
//        if (args.length > 0) {
//            throw Py.TypeError("default __new__ takes no parameters");
//        }

    }

    // This field is only filled on Python wrappers for Java objects and for Python subclasses of
    // Java classes.
    Object javaProxy;

    private PyType objtype;

    @ExposedGet(name = "__class__")
    public PyType getType() {
        return objtype;
    }

    @ExposedSet(name = "__class__")
    public void setType(PyType type) {
        if (type.builtin || getType().builtin) {
            throw Py.TypeError("__class__ assignment: only for heap types");
        }
        type.compatibleForAssignment(getType(), "__class__");
        objtype = type;
    }

    @ExposedDelete(name = "__class__")
    public void delType() {
        throw Py.TypeError("can't delete __class__ attribute");
    }

    // xxx
    public PyObject fastGetClass() {
        return objtype;
    }

    @ExposedGet(name = "__doc__")
    public PyObject getDoc() {
        PyObject d = fastGetDict();
        if (d != null) {
            PyObject doc = d.__finditem__("__doc__");
            if(doc != null) {
                return doc;
            }
        }
        return Py.None;
    }

    public PyObject(PyType objtype) {
        this.objtype = objtype;
    }

    /**
     * Creates the PyObject for the base type. The argument only exists to make the constructor
     * distinct.
     */
    PyObject(boolean ignored) {
        objtype = (PyType)this;
    }

    /**
     * The standard constructor for a <code>PyObject</code>. It will set the <code>objtype</code>
     * field to correspond to the specific subclass of <code>PyObject</code> being instantiated.
     **/
    public PyObject() {
        objtype = PyType.fromClass(getClass());
    }

    /**
     * Dispatch __init__ behavior
     */
    public void dispatch__init__(PyType type,PyObject[] args,String[] keywords) {
    }


    /**
     * Equivalent to the standard Python __repr__ method.  This method
     * should not typically need to be overrriden.  The easiest way to
     * configure the string representation of a <code>PyObject</code> is to
     * override the standard Java <code>toString</code> method.
     **/
    @ExposedMethod(names = "__str__")
    public PyString __repr__() {
        return new PyString(toString());
    }

    public String toString() {
        return object_toString();
    }

    @ExposedMethod(names = "__repr__")
    final String object_toString() {
        if (getType() == null) {
            return "unknown object";
        }

        String name = getType().getName();
        if (name == null) {
            return "unknown object";
        }

        PyObject module = getType().getModule();
        if (module instanceof PyString && !module.toString().equals("__builtin__")) {
            return String.format("<%s.%s object at %s>", module.toString(), name, Py.idstr(this));
        }
        return String.format("<%s object at %s>", name, Py.idstr(this));
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

    public PyUnicode __unicode__() {
        return new PyUnicode(__str__());
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
        return object___hash__();
    }

    @ExposedMethod
    final int object___hash__() {
        return System.identityHashCode(this);
    }

    /**
     * Should almost never be overridden.
     * If overridden, it is the subclasses responsibility to ensure that
     * <code>a.equals(b) == true</code> iff <code>cmp(a,b) == 0</code>
     **/
    public boolean equals(Object ob_other) {
        if(ob_other == this) {
            return true;
        }
        return (ob_other instanceof PyObject) && _eq((PyObject)ob_other).__nonzero__();
    }

    /**
     * Equivalent to the standard Python __nonzero__ method. Returns whether of
     * not a given <code>PyObject</code> is considered true.
     */
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
    public Object __tojava__(Class<?> c) {
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
        throw Py.TypeError(String.format("'%s' object is not callable", getType().fastGetName()));
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
    public PyObject _callextra(PyObject[] args,
                               String[] keywords,
                               PyObject starargs,
                               PyObject kwargs) {

        int argslen = args.length;

        String name;
        if (this instanceof PyFunction) {
            name = ((PyFunction) this).__name__ + "() ";
        } else if (this instanceof PyBuiltinCallable) {
            name = ((PyBuiltinCallable)this).fastGetName().toString() + "() ";
        } else {
            name = getType().fastGetName() + " ";
        }
        if (kwargs != null) {
            PyObject keys = kwargs.__findattr__("keys");
            if(keys == null)
                throw Py.TypeError(name
                        + "argument after ** must be a mapping");
            for (String keyword : keywords)
                if (kwargs.__finditem__(keyword) != null)
                    throw Py.TypeError(
                        name
                            + "got multiple values for "
                            + "keyword argument '"
                            + keyword
                            + "'");
            argslen += kwargs.__len__();
        }
        List<PyObject> starObjs = null;
        if (starargs != null) {
            starObjs = new ArrayList<PyObject>();
            PyObject iter = Py.iter(starargs, name + "argument after * must be a sequence");
            for (PyObject cur = null; ((cur = iter.__iternext__()) != null); ) {
                starObjs.add(cur);
            }
            argslen += starObjs.size();
        }
        PyObject[] newargs = new PyObject[argslen];
        int argidx = args.length - keywords.length;
        System.arraycopy(args, 0, newargs, 0, argidx);
        if(starObjs != null) {
            Iterator<PyObject> it = starObjs.iterator();
            while(it.hasNext()) {
                newargs[argidx++] = it.next();
            }
        }
        System.arraycopy(args,
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
        return getType().lookup("__call__") != null;
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

    /**
     * Determine if this object can act as an index (implements __index__).
     *
     * @return true if the object can act as an index
     */
    public boolean isIndex() {
        return getType().lookup("__index__") != null;
    }

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
        throw Py.TypeError(String.format("'%.200s' object is unsubscriptable",
                                         getType().fastGetName()));
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
     * @exception Py.KeyError if the key is not found.
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
     * @exception Py.KeyError if the key is not found.
     *
     * @see #__finditem__(PyObject)
     **/
    public PyObject __getitem__(PyObject key) {
        PyObject ret = __finditem__(key);
        if (ret == null) {
            throw Py.KeyError(key);
        }
        return ret;
    }

    /**
     * Equivalent to the standard Python __setitem__ method.
     *
     * @param key the key whose value will be set
     * @param value the value to set this key to
     **/
    public void __setitem__(PyObject key, PyObject value) {
        throw Py.TypeError(String.format("'%.200s' object does not support item assignment",
                                         getType().fastGetName()));
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
     * @exception Py.KeyError if the key is not found in the container
     **/
    public void __delitem__(PyObject key) {
        throw Py.TypeError(String.format("'%.200s' object doesn't support item deletion",
                                         getType().fastGetName()));
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
     * @exception Py.KeyError if the key is not found in the container
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
        return __getslice__(start, stop, null);
    }

    public void __setslice__(PyObject start, PyObject stop, PyObject value) {
        __setslice__(start, stop, null, value);
    }

    public void __delslice__(PyObject start, PyObject stop) {
        __delslice__(start, stop, null);
    }

    /*The basic functions to implement an iterator */

    /**
     * Return an iterator that is used to iterate the element of this sequence. From version 2.2,
     * this method is the primary protocol for looping over sequences.
     * <p>
     * If a PyObject subclass should support iteration based in the __finditem__() method, it must
     * supply an implementation of __iter__() like this:
     *
     * <pre>
     * public PyObject __iter__() {
     *     return new PySequenceIter(this);
     * }
     * </pre>
     *
     * When iterating over a python sequence from java code, it should be done with code like this:
     *
     * <pre>
     * for (PyObject item : seq.asIterable()) {
     *     // Do somting with item
     * }
     * </pre>
     *
     * @since 2.2
     */
    public PyObject __iter__() {
        throw Py.TypeError(String.format("'%.200s' object is not iterable",
                                         getType().fastGetName()));
    }

    /**
     * @return an Iterable over the Python iterator returned by __iter__ on this object. If this
     *         object doesn't support __iter__, a TypeException will be raised when iterator is
     *         called on the returned Iterable.
     */
    public Iterable<PyObject> asIterable() {
        return new Iterable<PyObject>() {

            public Iterator<PyObject> iterator() {
                return new Iterator<PyObject>() {

                    private PyObject iter = __iter__();

                    private PyObject next;

                    private boolean checkedForNext;

                    public boolean hasNext() {
                        if (!checkedForNext) {
                            next = iter.__iternext__();
                            checkedForNext = true;
                        }
                        return next != null;
                    }

                    public PyObject next() {
                        if (!hasNext()) {
                            throw new NoSuchElementException("End of the line, bub");
                        }
                        PyObject toReturn = next;
                        checkedForNext = false;
                        next = null;
                        return toReturn;
                    }

                    public void remove() {
                        throw new UnsupportedOperationException("Can't remove from a Python iterator");
                    }
                };
            }
        };
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
     * Very similar to the standard Python __getattr__ method. Instead of
     * throwing a AttributeError if the item isn't found, this just returns
     * null.
     *
     * By default, this method will call
     * <code>__findattr__(name.internedString)</code> with the appropriate
     * args.
     *
     * @param name the name to lookup in this namespace
     *
     * @return the value corresponding to name or null if name is not found
     */
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
     * <b>Warning: name must be an interned string!</b>
     *
     * @param name the name to lookup in this namespace
     * <b> must be an interned string </b>.
     * @return the value corresponding to name or null if name is not found
     **/
    public final PyObject __findattr__(String name) {
        try {
            return  __findattr_ex__(name);
        } catch (PyException exc) {
            if (Py.matchException(exc, Py.AttributeError)) {
                return null;
            }
            throw exc;
        }
    }

    /**
     * Attribute lookup hook. If the attribute is not found, null may be
     * returned or a Py.AttributeError can be thrown, whatever is more
     * correct, efficient and/or convenient for the implementing class.
     *
     * Client code should use {@link #__getattr__(String)} or
     * {@link #__findattr__(String)}. Both methods have a clear policy for
     * failed lookups.
     *
     * @return The looked up value. May return null if the attribute is not found
     * @throws PyException(AttributeError) if the attribute is not found. This
     * is not mandatory, null can be returned if it fits the implementation
     * better, or for performance reasons.
     */
    public PyObject __findattr_ex__(String name) {
        return object___findattr__(name);
    }

    /**
     * Equivalent to the standard Python __getattr__ method.
     *
     * By default, this method will call
     * <code>__getattr__(name.internedString)</code> with the appropriate
     * args.
     *
     * @param name the name to lookup in this namespace
     * @return the value corresponding to name
     * @exception Py.AttributeError if the name is not found.
     *
     * @see #__findattr_ex__(PyString)
     **/
    public final PyObject __getattr__(PyString name) {
        return __getattr__(name.internedString());
    }

    /**
     * A variant of the __getattr__ method which accepts a Java
     * <code>String</code> as the name.
     * This method can not be overridden.
     * Override the <code>__findattr_ex__</code> method instead.
     *
     * <b>Warning: name must be an interned string!!!!!!!!</b>
     *
     * @param name the name to lookup in this namespace
     *             <b> must be an interned string </b>.
     * @return the value corresponding to name
     * @exception Py.AttributeError if the name is not found.
     *
     * @see #__findattr__(java.lang.String)
     **/
    public final PyObject __getattr__(String name) {
        PyObject ret = __findattr_ex__(name);
        if (ret == null)
            noAttributeError(name);
        return ret;
    }

    public void noAttributeError(String name) {
        throw Py.AttributeError(String.format("'%.50s' object has no attribute '%.400s'",
                                              getType().fastGetName(), name));
    }

    public void readonlyAttributeError(String name) {
        // XXX: Should be an AttributeError but CPython throws TypeError for read only
        // member descriptors (in structmember.c::PyMember_SetOne), which is expected by a
        // few tests. fixed in py3k: http://bugs.python.org/issue1687163
        throw Py.TypeError("readonly attribute");
    }

    /**
     * Equivalent to the standard Python __setattr__ method.
     * This method can not be overridden.
     *
     * @param name the name to lookup in this namespace
     * @exception Py.AttributeError if the name is not found.
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
        object___setattr__(name, value);
    }

    /**
     * Equivalent to the standard Python __delattr__ method.
     * This method can not be overridden.
     *
     * @param name the name to which will be removed
     * @exception Py.AttributeError if the name doesn't exist
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
     * @exception Py.AttributeError if the name doesn't exist
     *
     * @see #__delattr__(PyString)
     **/
    public void __delattr__(String name) {
        object___delattr__(name);
    }

    // Used by import logic.
    protected PyObject impAttr(String name) {
        return __findattr__(name);
    }

    protected void addKeys(PyDictionary accum, String attr) {
        PyObject obj = __findattr__(attr);
        if (obj == null)
            return;
        if (obj instanceof PyList) {
            for (PyObject name : obj.asIterable()) {
                accum.__setitem__(name, Py.None);
            }
        } else {
            accum.update(obj);
        }
    }

    protected void __rawdir__(PyDictionary accum) {
        addKeys(accum, "__dict__");
        addKeys(accum, "__methods__");
        addKeys(accum, "__members__");
        fastGetClass().__rawdir__(accum);
    }

    /**
     * Equivalent to the standard Python __dir__ method.
     *
     * @return a list of names defined by this object.
     **/
    public PyObject __dir__() {
        PyDictionary accum = new PyDictionary();
        __rawdir__(accum);
        PyList ret = accum.keys();
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

    boolean jtryset(PyObject container, PyObject value) {
        return _doset(container, value);
    }

    boolean jdontdel() {
        return false;
    }

    /* Numeric coercion */

    /**
     * Implements numeric coercion
     *
     * @param o the other object involved in the coercion
     * @return null if coercion is not implemented
     *         Py.None if coercion was not possible
     *         a single PyObject to use to replace o if this is unchanged;
     *         or a PyObject[2] consisting of replacements for this and o.
     **/
    public Object __coerce_ex__(PyObject o) {
        return null;
    }

    /**
     * Implements coerce(this,other), result as PyObject[]
     * @param other
     * @return PyObject[]
     */
    PyObject[] _coerce(PyObject other) {
        Object result;
        if (this.getType() == other.getType() &&
            !(this instanceof PyInstance)) {
            return new PyObject[] {this, other};
        }
        result = this.__coerce_ex__(other);
        if (result != null && result != Py.None) {
            if (result instanceof PyObject[]) {
                return (PyObject[])result;
            } else {
                return new PyObject[] {this, (PyObject)result};
            }
        }
        result = other.__coerce_ex__(this);
        if (result != null && result != Py.None) {
            if (result instanceof PyObject[]) {
                return (PyObject[])result;
            } else {
                return new PyObject[] {(PyObject)result, other};
            }
        }
        return null;

    }

    /**
     * Equivalent to the standard Python __coerce__ method.
     *
     * This method can not be overridden.
     * To implement __coerce__ functionality, override __coerce_ex__ instead.
     *
     * Also, <b>do not</b> call this method from exposed 'coerce' methods.
     * Instead, Use adaptToCoerceTuple over the result of the overriden
     * __coerce_ex__.
     *
     * @param pyo the other object involved in the coercion.
     * @return a tuple of this object and pyo coerced to the same type
     *         or Py.NotImplemented if no coercion is possible.
     * @see org.python.core.PyObject#__coerce_ex__(org.python.core.PyObject)
     **/
    public final PyObject __coerce__(PyObject pyo) {
        Object o = __coerce_ex__(pyo);
        if (o == null) {
            throw Py.AttributeError("__coerce__");
        }
        return adaptToCoerceTuple(o);
    }

    /**
     * Adapts the result of __coerce_ex__ to a tuple of two elements, with the
     * resulting coerced values, or to Py.NotImplemented, if o is Py.None.
     *
     * This is safe to be used from subclasses exposing '__coerce__'
     * (as opposed to {@link #__coerce__(PyObject)}, which calls the virtual
     * method {@link #__coerce_ex__(PyObject)})
     *
     * @param o either a PyObject[2] or a PyObject, as given by
     *        {@link #__coerce_ex__(PyObject)}.
     */
    protected final PyObject adaptToCoerceTuple(Object o) {
        if (o == Py.None) {
            return Py.NotImplemented;
        }
        if (o instanceof PyObject[]) {
            return new PyTuple((PyObject[]) o);
        } else {
            return new PyTuple(this, (PyObject) o );
        }
    }

    /* The basic comparision operations */

    /**
     * Equivalent to the standard Python __cmp__ method.
     *
     * @param other the object to compare this with.
     * @return -1 if this < o; 0 if this == o; +1 if this > o; -2 if no
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
     * @param o the object to compare this with.
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

    private final int _default_cmp(PyObject other) {
        int result;
        if (this._is(other).__nonzero__())
            return 0;

        /* None is smaller than anything */
        if (this == Py.None)
            return -1;
        if (other == Py.None)
            return 1;

        // No rational way to compare these, so ask their classes to compare
        PyType this_type = this.getType();
        PyType other_type = other.getType();
        if (this_type == other_type) {
            return Py.id(this) < Py.id(other)? -1: 1;
        }
        result = this_type.fastGetName().compareTo(other_type.fastGetName());
        if (result == 0)
            return Py.id(this_type)<Py.id(other_type)? -1: 1;
        return result < 0? -1: 1;
    }

    private final int _cmp_unsafe(PyObject other) {
        // Shortcut for equal objects
        if (this == other)
            return 0;

        int result = _try__cmp__(other);
        if (result != -2) {
            return result;
        }
        return this._default_cmp(other);
    }

    /*
     *  Like _cmp_unsafe but limited to ==/!= as 0/!=0,
     *  thus it avoids to invoke _default_cmp.
     */
    private final int _cmpeq_unsafe(PyObject other) {
        // Shortcut for equal objects
        if (this == other)
            return 0;

        int result = _try__cmp__(other);
        if (result != -2) {
            return result;
        }

        return this._is(other).__nonzero__()?0:1;
    }

    /**
     * Tries a 3-way comparison, using __cmp__. It tries the following
     * operations until one of them succeed:<ul>
     *  <li>this.__cmp__(other)
     *  <li>other.__cmp__(this)
     *  <li>this._coerce(other) followed by coerced_this.__cmp__(coerced_other)</ul>
     *
     * @return -1, 0, -1 or -2, according to the {@link #__cmp__} protocol.
     */
    private int _try__cmp__(PyObject other) {
        int result;
        result = this.__cmp__(other);
        if (result != -2)
            return result;

        if (!(this instanceof PyInstance)) {
            result = other.__cmp__(this);
            if (result != -2)
                return -result;
        }
        // Final attempt: coerce both arguments and compare that. We are doing
        // this the same point where CPython 2.5 does. (See
        // <http://svn.python.org/projects/python/tags/r252/Objects/object.c> at
        // the end of try_3way_compare).
        //
        // This is not exactly was is specified on
        // <http://docs.python.org/ref/coercion-rules.html>, where coercion is
        // supposed to happen before trying __cmp__.

        PyObject[] coerced = _coerce(other);
        if (coerced != null) {
            result = coerced[0].__cmp__(coerced[1]);
            if (result != -2) {
                return result;
            }
        }
        return -2;
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
     * @param o the object to compare this with.
     * @return the result of the comparison
     **/
    public final PyObject _eq(PyObject o) {
        PyObject token = null;
        PyType t1 = this.getType();
        PyType t2 = o.getType();

        if (t1 != t2 && t2.isSubType(t1)) {
            return o._eq(this);
        }

        ThreadState ts = Py.getThreadState();
        try {
            if (++ts.compareStateNesting > 10) {
                if ((token = check_recursion(ts, this, o)) == null)
                    return Py.True;
            }
            PyObject res = __eq__(o);
            if (res != null)
                return res;
            res = o.__eq__(this);
            if (res != null)
                return res;
            return _cmpeq_unsafe(o) == 0 ? Py.True : Py.False;
        } catch (PyException e) {
            if (Py.matchException(e, Py.AttributeError)) {
                return Py.False;
            }
            throw e;
        } finally {
            delete_token(ts, token);
            ts.compareStateNesting--;
        }
    }

    /**
     * Implements the Python expression <code>this != other</code>.
     *
     * @param o the object to compare this with.
     * @return the result of the comparison
     **/
    public final PyObject _ne(PyObject o) {
        PyObject token = null;
        PyType t1 = this.getType();
        PyType t2 = o.getType();

        if (t1 != t2 && t2.isSubType(t1)) {
            return o._ne(this);
        }

        ThreadState ts = Py.getThreadState();
        try {
            if (++ts.compareStateNesting > 10) {
                if ((token = check_recursion(ts, this, o)) == null)
                    return Py.False;
            }
            PyObject res = __ne__(o);
            if (res != null)
                return res;
            res = o.__ne__(this);
            if (res != null)
                return res;
            return _cmpeq_unsafe(o) != 0 ? Py.True : Py.False;
        } finally {
            delete_token(ts, token);
            ts.compareStateNesting--;
        }
    }

    /**
     * Implements the Python expression <code>this &lt;= other</code>.
     *
     * @param o the object to compare this with.
     * @return the result of the comparison
     **/
    public final PyObject _le(PyObject o) {
        PyObject token = null;
        PyType t1 = this.getType();
        PyType t2 = o.getType();

        if (t1 != t2 && t2.isSubType(t1)) {
            return o._ge(this);
        }

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
            return _cmp_unsafe(o) <= 0 ? Py.True : Py.False;
        } finally {
            delete_token(ts, token);
            ts.compareStateNesting--;
        }
    }

    /**
     * Implements the Python expression <code>this &lt; other</code>.
     *
     * @param o the object to compare this with.
     * @return the result of the comparison
     **/
    public final PyObject _lt(PyObject o) {
        PyObject token = null;
        PyType t1 = this.getType();
        PyType t2 = o.getType();

        if (t1 != t2 && t2.isSubType(t1)) {
            return o._gt(this);
        }

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
            return _cmp_unsafe(o) < 0 ? Py.True : Py.False;
        } finally {
            delete_token(ts, token);
            ts.compareStateNesting--;
        }
    }

    /**
     * Implements the Python expression <code>this &gt;= other</code>.
     *
     * @param o the object to compare this with.
     * @return the result of the comparison
     **/
    public final PyObject _ge(PyObject o) {
        PyObject token = null;
        PyType t1 = this.getType();
        PyType t2 = o.getType();

        if (t1 != t2 && t2.isSubType(t1)) {
            return o._le(this);
        }

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
            return _cmp_unsafe(o) >= 0 ? Py.True : Py.False;
        } finally {
            delete_token(ts, token);
            ts.compareStateNesting--;
        }
    }

    /**
     * Implements the Python expression <code>this &gt; other</code>.
     *
     * @param o the object to compare this with.
     * @return the result of the comparison
     **/
    public final PyObject _gt(PyObject o) {
        PyObject token = null;
        PyType t1 = this.getType();
        PyType t2 = o.getType();

        if (t1 != t2 && t2.isSubType(t1)) {
            return o._lt(this);
        }

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
            return _cmp_unsafe(o) > 0 ? Py.True : Py.False;
        } finally {
            delete_token(ts, token);
            ts.compareStateNesting--;
        }

    }

    /**
     * Implements <code>is</code> operator.
     *
     * @param o the object to compare this with.
     * @return the result of the comparison
     **/
    public PyObject _is(PyObject o) {
        return this == o ? Py.True : Py.False;
    }

    /**
     * Implements <code>is not</code> operator.
     *
     * @param o the object to compare this with.
     * @return the result of the comparison
     **/
    public PyObject _isnot(PyObject o) {
        return this != o ? Py.True : Py.False;
    }

    /**
     * Implements <code>in</code> operator.
     *
     * @param o the container to search for this element.
     * @return the result of the search.
     **/
    public final PyObject _in(PyObject o) {
        return Py.newBoolean(o.__contains__(this));
    }

    /**
     * Implements <code>not in</code> operator.
     *
     * @param o the container to search for this element.
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
        return object___contains__(o);
    }

    final boolean object___contains__(PyObject o) {
        for (PyObject item : asIterable()) {
            if (o._eq(item).__nonzero__()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Implements boolean not
     *
     * @return not this.
     **/
    public PyObject __not__() {
        return __nonzero__() ? Py.False : Py.True;
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
        throw Py.TypeError("hex() argument can't be converted to hex");
    }

    /**
     * Equivalent to the standard Python __oct__ method.
     * Should only be overridden by numeric objects that can be
     * reasonably represented as an octal string.
     *
     * @return a string representing this object as an octal number.
     **/
    public PyString __oct__() {
        throw Py.TypeError("oct() argument can't be converted to oct");
    }

    /**
     * Equivalent to the standard Python __int__ method.
     * Should only be overridden by numeric objects that can be
     * reasonably coerced into an integer.
     *
     * @return an integer corresponding to the value of this object.
     **/
    public PyObject __int__() {
        throw Py.AttributeError("__int__");
    }

    /**
     * Equivalent to the standard Python __long__ method.
     * Should only be overridden by numeric objects that can be
     * reasonably coerced into a python long.
     *
     * @return a PyLong or PyInteger corresponding to the value of this object.
     **/
    public PyObject __long__() {
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
        throw Py.TypeError(String.format("bad operand type for unary +: '%.200s'",
                                         getType().fastGetName()));
    }

    /**
     * Equivalent to the standard Python __neg__ method.
     *
     * @return -this.
     **/
    public PyObject __neg__() {
        throw Py.TypeError(String.format("bad operand type for unary -: '%.200s'",
                                         getType().fastGetName()));
    }

    /**
     * Equivalent to the standard Python __abs__ method.
     *
     * @return abs(this).
     **/
    public PyObject __abs__() {
        throw Py.TypeError(String.format("bad operand type for abs(): '%.200s'",
                                         getType().fastGetName()));
    }

    /**
     * Equivalent to the standard Python __invert__ method.
     *
     * @return ~this.
     **/
    public PyObject __invert__() {
        throw Py.TypeError(String.format("bad operand type for unary ~: '%.200s'",
                                         getType().fastGetName()));
    }

    /**
     * Equivalent to the standard Python __index__ method.
     *
     * @return a PyInteger or PyLong
     * @throws a Py.TypeError if not supported
     **/
    public PyObject __index__() {
        throw Py.TypeError(String.format("'%.200s' object cannot be interpreted as an index",
                                         getType().fastGetName()));
    }

    /**
     * @param op the String form of the op (e.g. "+")
     * @param o2 the right operand
     */
    protected final String _unsupportedop(String op, PyObject o2) {
        Object[] args = {op, getType().fastGetName(), o2.getType().fastGetName()};
        String msg = unsupportedopMessage(op, o2);
        if (msg == null) {
            msg = o2.runsupportedopMessage(op, o2);
        }
        if (msg == null) {
            msg = "unsupported operand type(s) for {0}: ''{1}'' and ''{2}''";
        }
        return MessageFormat.format(msg, args);
    }

    /**
     * Should return an error message suitable for substitution where.
     *
     * {0} is the op name.
     * {1} is the left operand type.
     * {2} is the right operand type.
     */
    protected String unsupportedopMessage(String op, PyObject o2) {
        return null;
    }

    /**
     * Should return an error message suitable for substitution where.
     *
     * {0} is the op name.
     * {1} is the left operand type.
     * {2} is the right operand type.
     */
    protected String runsupportedopMessage(String op, PyObject o2) {
        return null;
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

    /**
     * Determine if the binary op on types t1 and t2 is an add
     * operation dealing with a str/unicode and a str/unicode
     * subclass.
     *
     * This operation is special cased in _binop_rule to match
     * CPython's handling; CPython uses tp_as_number and
     * tp_as_sequence to allow string/unicode subclasses to override
     * the left side's __add__ when that left side is an actual str or
     * unicode object (see test_concat_jy for examples).
     *
     * @param t1 left side PyType
     * @param t2 right side PyType
     * @param op the binary operation's String
     * @return true if this is a special case
     */
    private boolean isStrUnicodeSpecialCase(PyType t1, PyType t2, String op) {
        // XXX: We may need to generalize this rule to apply to other
        // situations
        // XXX: This method isn't expensive but could (and maybe
        // should?) be optimized for worst case scenarios
        return op == "+" && (t1 == PyString.TYPE || t1 == PyUnicode.TYPE) &&
                (t2.isSubType(PyString.TYPE) || t2.isSubType(PyUnicode.TYPE));
    }

    private PyObject _binop_rule(PyType t1, PyObject o2, PyType t2,
            String left, String right, String op) {
        /*
         * this is the general rule for binary operation dispatching try first
         * __xxx__ with this and then __rxxx__ with o2 unless o2 is an instance
         * of subclass of the type of this, and further __xxx__ and __rxxx__ are
         * unrelated ( checked here by looking at where in the hierarchy they
         * are defined), in that case try them in the reverse order. This is the
         * same formulation as used by PyPy, see also
         * test_descr.subclass_right_op.
         */
        PyObject o1 = this;
        PyObject[] where = new PyObject[1];
        PyObject where1 = null, where2 = null;
        PyObject impl1 = t1.lookup_where(left, where);
        where1 = where[0];
        PyObject impl2 = t2.lookup_where(right, where);
        where2 = where[0];
        if (impl2 != null && where1 != where2 && (t2.isSubType(t1) ||
                                                  isStrUnicodeSpecialCase(t1, t2, op))) {
            PyObject tmp = o1;
            o1 = o2;
            o2 = tmp;
            tmp = impl1;
            impl1 = impl2;
            impl2 = tmp;
            PyType ttmp;
            ttmp = t1;
            t1 = t2;
            t2 = ttmp;
        }
        PyObject res = null;
        if (impl1 != null) {
            res = impl1.__get__(o1, t1).__call__(o2);
            if (res != Py.NotImplemented) {
                return res;
            }
        }
        if (impl2 != null) {
            res = impl2.__get__(o2, t2).__call__(o1);
            if (res != Py.NotImplemented) {
                return res;
            }
        }
        throw Py.TypeError(_unsupportedop(op, o2));
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
     * @return    the result of the iadd, or null if this operation
     *            is not defined
     **/
    public PyObject __iadd__(PyObject other) {
        return null;
    }

    /**
      * Implements the Python expression <code>this + o2</code>
      * @param     o2 the object to perform this binary operation with.
      * @return    the result of the add.
      * @exception Py.TypeError if this operation can't be performed
      *            with these operands.
      **/
    public final PyObject _add(PyObject o2) {
        PyType t1=this.getType();
        PyType t2=o2.getType();
        if (t1==t2||t1.builtin&&t2.builtin) {
            return this._basic_add(o2);
        }
        return _binop_rule(t1,o2,t2,"__add__","__radd__","+");
    }

    /**
     * Implements the Python expression <code>this + o2</code>
     * when this and o2 have the same type or are builtin types.
     * @param     o2 the object to perform this binary operation with.
     * @return    the result of the add.
     * @exception Py.TypeError if this operation can't be performed
     *            with these operands.
     **/
    final PyObject _basic_add(PyObject o2) {
        PyObject x=__add__(o2);
        if (x!=null) {
            return x;
        }
        x=o2.__radd__(this);
        if (x!=null) {
            return x;
        }
        throw Py.TypeError(_unsupportedop("+",o2));
    }

    /**
      * Implements the Python expression <code>this += o2</code>
      * @param     o2 the object to perform this inplace binary
      *            operation with.
      * @return    the result of the iadd.
      * @exception Py.TypeError if this operation can't be performed
      *            with these operands.
      **/
    public final PyObject _iadd(PyObject o2) {
        PyType t1=this.getType();
        PyType t2=o2.getType();
        if (t1==t2||t1.builtin&&t2.builtin) {
            return this._basic_iadd(o2);
        }
        PyObject impl=t1.lookup("__iadd__");
        if (impl!=null) {
            PyObject res=impl.__get__(this,t1).__call__(o2);
            if (res!=Py.NotImplemented) {
                return res;
            }
        }
        return _binop_rule(t1,o2,t2,"__add__","__radd__","+");
    }

    /**
     * Implements the Python expression <code>this += o2</code>
     * when this and o2 have the same type or are builtin types.
     * @param     o2 the object to perform this inplace binary
     *            operation with.
     * @return    the result of the iadd.
     * @exception Py.TypeError if this operation can't be performed
     *            with these operands.
     **/
    final PyObject _basic_iadd(PyObject o2) {
        PyObject x=__iadd__(o2);
        if (x!=null) {
            return x;
        }
        return this._basic_add(o2);
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
     * @return    the result of the isub, or null if this operation
     *            is not defined
     **/
    public PyObject __isub__(PyObject other) {
        return null;
    }

    /**
      * Implements the Python expression <code>this - o2</code>
      * @param     o2 the object to perform this binary operation with.
      * @return    the result of the sub.
      * @exception Py.TypeError if this operation can't be performed
      *            with these operands.
      **/
    public final PyObject _sub(PyObject o2) {
        PyType t1=this.getType();
        PyType t2=o2.getType();
        if (t1==t2||t1.builtin&&t2.builtin) {
            return this._basic_sub(o2);
        }
        return _binop_rule(t1,o2,t2,"__sub__","__rsub__","-");
    }

    /**
     * Implements the Python expression <code>this - o2</code>
     * when this and o2 have the same type or are builtin types.
     * @param     o2 the object to perform this binary operation with.
     * @return    the result of the sub.
     * @exception Py.TypeError if this operation can't be performed
     *            with these operands.
     **/
    final PyObject _basic_sub(PyObject o2) {
        PyObject x=__sub__(o2);
        if (x!=null) {
            return x;
        }
        x=o2.__rsub__(this);
        if (x!=null) {
            return x;
        }
        throw Py.TypeError(_unsupportedop("-",o2));
    }

    /**
      * Implements the Python expression <code>this -= o2</code>
      * @param     o2 the object to perform this inplace binary
      *            operation with.
      * @return    the result of the isub.
      * @exception Py.TypeError if this operation can't be performed
      *            with these operands.
      **/
    public final PyObject _isub(PyObject o2) {
        PyType t1=this.getType();
        PyType t2=o2.getType();
        if (t1==t2||t1.builtin&&t2.builtin) {
            return this._basic_isub(o2);
        }
        PyObject impl=t1.lookup("__isub__");
        if (impl!=null) {
            PyObject res=impl.__get__(this,t1).__call__(o2);
            if (res!=Py.NotImplemented) {
                return res;
            }
        }
        return _binop_rule(t1,o2,t2,"__sub__","__rsub__","-");
    }

    /**
     * Implements the Python expression <code>this -= o2</code>
     * when this and o2 have the same type or are builtin types.
     * @param     o2 the object to perform this inplace binary
     *            operation with.
     * @return    the result of the isub.
     * @exception Py.TypeError if this operation can't be performed
     *            with these operands.
     **/
    final PyObject _basic_isub(PyObject o2) {
        PyObject x=__isub__(o2);
        if (x!=null) {
            return x;
        }
        return this._basic_sub(o2);
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
     * @return    the result of the imul, or null if this operation
     *            is not defined
     **/
    public PyObject __imul__(PyObject other) {
        return null;
    }

    /**
      * Implements the Python expression <code>this * o2</code>
      * @param     o2 the object to perform this binary operation with.
      * @return    the result of the mul.
      * @exception Py.TypeError if this operation can't be performed
      *            with these operands.
      **/
    public final PyObject _mul(PyObject o2) {
        PyType t1=this.getType();
        PyType t2=o2.getType();
        if (t1==t2||t1.builtin&&t2.builtin) {
            return this._basic_mul(o2);
        }
        return _binop_rule(t1,o2,t2,"__mul__","__rmul__","*");
    }

    /**
     * Implements the Python expression <code>this * o2</code>
     * when this and o2 have the same type or are builtin types.
     * @param     o2 the object to perform this binary operation with.
     * @return    the result of the mul.
     * @exception Py.TypeError if this operation can't be performed
     *            with these operands.
     **/
    final PyObject _basic_mul(PyObject o2) {
        PyObject x=__mul__(o2);
        if (x!=null) {
            return x;
        }
        x=o2.__rmul__(this);
        if (x!=null) {
            return x;
        }
        throw Py.TypeError(_unsupportedop("*",o2));
    }

    /**
      * Implements the Python expression <code>this *= o2</code>
      * @param     o2 the object to perform this inplace binary
      *            operation with.
      * @return    the result of the imul.
      * @exception Py.TypeError if this operation can't be performed
      *            with these operands.
      **/
    public final PyObject _imul(PyObject o2) {
        PyType t1=this.getType();
        PyType t2=o2.getType();
        if (t1==t2||t1.builtin&&t2.builtin) {
            return this._basic_imul(o2);
        }
        PyObject impl=t1.lookup("__imul__");
        if (impl!=null) {
            PyObject res=impl.__get__(this,t1).__call__(o2);
            if (res!=Py.NotImplemented) {
                return res;
            }
        }
        return _binop_rule(t1,o2,t2,"__mul__","__rmul__","*");
    }

    /**
     * Implements the Python expression <code>this *= o2</code>
     * when this and o2 have the same type or are builtin types.
     * @param     o2 the object to perform this inplace binary
     *            operation with.
     * @return    the result of the imul.
     * @exception Py.TypeError if this operation can't be performed
     *            with these operands.
     **/
    final PyObject _basic_imul(PyObject o2) {
        PyObject x=__imul__(o2);
        if (x!=null) {
            return x;
        }
        return this._basic_mul(o2);
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
     * @return    the result of the idiv, or null if this operation
     *            is not defined
     **/
    public PyObject __idiv__(PyObject other) {
        return null;
    }

    /**
      * Implements the Python expression <code>this / o2</code>
      * @param     o2 the object to perform this binary operation with.
      * @return    the result of the div.
      * @exception Py.TypeError if this operation can't be performed
      *            with these operands.
      **/
    public final PyObject _div(PyObject o2) {
        if (Options.Qnew)
            return _truediv(o2);
        PyType t1=this.getType();
        PyType t2=o2.getType();
        if (t1==t2||t1.builtin&&t2.builtin) {
            return this._basic_div(o2);
        }
        return _binop_rule(t1,o2,t2,"__div__","__rdiv__","/");
    }

    /**
     * Implements the Python expression <code>this / o2</code>
     * when this and o2 have the same type or are builtin types.
     * @param     o2 the object to perform this binary operation with.
     * @return    the result of the div.
     * @exception Py.TypeError if this operation can't be performed
     *            with these operands.
     **/
    final PyObject _basic_div(PyObject o2) {
        PyObject x=__div__(o2);
        if (x!=null) {
            return x;
        }
        x=o2.__rdiv__(this);
        if (x!=null) {
            return x;
        }
        throw Py.TypeError(_unsupportedop("/",o2));
    }

    /**
      * Implements the Python expression <code>this /= o2</code>
      * @param     o2 the object to perform this inplace binary
      *            operation with.
      * @return    the result of the idiv.
      * @exception Py.TypeError if this operation can't be performed
      *            with these operands.
      **/
    public final PyObject _idiv(PyObject o2) {
        if (Options.Qnew)
            return _itruediv(o2);
        PyType t1=this.getType();
        PyType t2=o2.getType();
        if (t1==t2||t1.builtin&&t2.builtin) {
            return this._basic_idiv(o2);
        }
        PyObject impl=t1.lookup("__idiv__");
        if (impl!=null) {
            PyObject res=impl.__get__(this,t1).__call__(o2);
            if (res!=Py.NotImplemented) {
                return res;
            }
        }
        return _binop_rule(t1,o2,t2,"__div__","__rdiv__","/");
    }

    /**
     * Implements the Python expression <code>this /= o2</code>
     * when this and o2 have the same type or are builtin types.
     * @param     o2 the object to perform this inplace binary
     *            operation with.
     * @return    the result of the idiv.
     * @exception Py.TypeError if this operation can't be performed
     *            with these operands.
     **/
    final PyObject _basic_idiv(PyObject o2) {
        PyObject x=__idiv__(o2);
        if (x!=null) {
            return x;
        }
        return this._basic_div(o2);
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
     * @return    the result of the ifloordiv, or null if this operation
     *            is not defined
     **/
    public PyObject __ifloordiv__(PyObject other) {
        return null;
    }

    /**
      * Implements the Python expression <code>this // o2</code>
      * @param     o2 the object to perform this binary operation with.
      * @return    the result of the floordiv.
      * @exception Py.TypeError if this operation can't be performed
      *            with these operands.
      **/
    public final PyObject _floordiv(PyObject o2) {
        PyType t1=this.getType();
        PyType t2=o2.getType();
        if (t1==t2||t1.builtin&&t2.builtin) {
            return this._basic_floordiv(o2);
        }
        return _binop_rule(t1,o2,t2,"__floordiv__","__rfloordiv__","//");
    }

    /**
     * Implements the Python expression <code>this // o2</code>
     * when this and o2 have the same type or are builtin types.
     * @param     o2 the object to perform this binary operation with.
     * @return    the result of the floordiv.
     * @exception Py.TypeError if this operation can't be performed
     *            with these operands.
     **/
    final PyObject _basic_floordiv(PyObject o2) {
        PyObject x=__floordiv__(o2);
        if (x!=null) {
            return x;
        }
        x=o2.__rfloordiv__(this);
        if (x!=null) {
            return x;
        }
        throw Py.TypeError(_unsupportedop("//",o2));
    }

    /**
      * Implements the Python expression <code>this //= o2</code>
      * @param     o2 the object to perform this inplace binary
      *            operation with.
      * @return    the result of the ifloordiv.
      * @exception Py.TypeError if this operation can't be performed
      *            with these operands.
      **/
    public final PyObject _ifloordiv(PyObject o2) {
        PyType t1=this.getType();
        PyType t2=o2.getType();
        if (t1==t2||t1.builtin&&t2.builtin) {
            return this._basic_ifloordiv(o2);
        }
        PyObject impl=t1.lookup("__ifloordiv__");
        if (impl!=null) {
            PyObject res=impl.__get__(this,t1).__call__(o2);
            if (res!=Py.NotImplemented) {
                return res;
            }
        }
        return _binop_rule(t1,o2,t2,"__floordiv__","__rfloordiv__","//");
    }

    /**
     * Implements the Python expression <code>this //= o2</code>
     * when this and o2 have the same type or are builtin types.
     * @param     o2 the object to perform this inplace binary
     *            operation with.
     * @return    the result of the ifloordiv.
     * @exception Py.TypeError if this operation can't be performed
     *            with these operands.
     **/
    final PyObject _basic_ifloordiv(PyObject o2) {
        PyObject x=__ifloordiv__(o2);
        if (x!=null) {
            return x;
        }
        return this._basic_floordiv(o2);
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
     * @return    the result of the itruediv, or null if this operation
     *            is not defined
     **/
    public PyObject __itruediv__(PyObject other) {
        return null;
    }

    /**
      * Implements the Python expression <code>this / o2</code>
      * @param     o2 the object to perform this binary operation with.
      * @return    the result of the truediv.
      * @exception Py.TypeError if this operation can't be performed
      *            with these operands.
      **/
    public final PyObject _truediv(PyObject o2) {
        PyType t1=this.getType();
        PyType t2=o2.getType();
        if (t1==t2||t1.builtin&&t2.builtin) {
            return this._basic_truediv(o2);
        }
        return _binop_rule(t1,o2,t2,"__truediv__","__rtruediv__","/");
    }

    /**
     * Implements the Python expression <code>this / o2</code>
     * when this and o2 have the same type or are builtin types.
     * @param     o2 the object to perform this binary operation with.
     * @return    the result of the truediv.
     * @exception Py.TypeError if this operation can't be performed
     *            with these operands.
     **/
    final PyObject _basic_truediv(PyObject o2) {
        PyObject x=__truediv__(o2);
        if (x!=null) {
            return x;
        }
        x=o2.__rtruediv__(this);
        if (x!=null) {
            return x;
        }
        throw Py.TypeError(_unsupportedop("/",o2));
    }

    /**
      * Implements the Python expression <code>this /= o2</code>
      * @param     o2 the object to perform this inplace binary
      *            operation with.
      * @return    the result of the itruediv.
      * @exception Py.TypeError if this operation can't be performed
      *            with these operands.
      **/
    public final PyObject _itruediv(PyObject o2) {
        PyType t1=this.getType();
        PyType t2=o2.getType();
        if (t1==t2||t1.builtin&&t2.builtin) {
            return this._basic_itruediv(o2);
        }
        PyObject impl=t1.lookup("__itruediv__");
        if (impl!=null) {
            PyObject res=impl.__get__(this,t1).__call__(o2);
            if (res!=Py.NotImplemented) {
                return res;
            }
        }
        return _binop_rule(t1,o2,t2,"__truediv__","__rtruediv__","/");
    }

    /**
     * Implements the Python expression <code>this /= o2</code>
     * when this and o2 have the same type or are builtin types.
     * @param     o2 the object to perform this inplace binary
     *            operation with.
     * @return    the result of the itruediv.
     * @exception Py.TypeError if this operation can't be performed
     *            with these operands.
     **/
    final PyObject _basic_itruediv(PyObject o2) {
        PyObject x=__itruediv__(o2);
        if (x!=null) {
            return x;
        }
        return this._basic_truediv(o2);
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
     * @return    the result of the imod, or null if this operation
     *            is not defined
     **/
    public PyObject __imod__(PyObject other) {
        return null;
    }

    /**
      * Implements the Python expression <code>this % o2</code>
      * @param     o2 the object to perform this binary operation with.
      * @return    the result of the mod.
      * @exception Py.TypeError if this operation can't be performed
      *            with these operands.
      **/
    public final PyObject _mod(PyObject o2) {
        PyType t1=this.getType();
        PyType t2=o2.getType();
        if (t1==t2||t1.builtin&&t2.builtin) {
            return this._basic_mod(o2);
        }
        return _binop_rule(t1,o2,t2,"__mod__","__rmod__","%");
    }

    /**
     * Implements the Python expression <code>this % o2</code>
     * when this and o2 have the same type or are builtin types.
     * @param     o2 the object to perform this binary operation with.
     * @return    the result of the mod.
     * @exception Py.TypeError if this operation can't be performed
     *            with these operands.
     **/
    final PyObject _basic_mod(PyObject o2) {
        PyObject x=__mod__(o2);
        if (x!=null) {
            return x;
        }
        x=o2.__rmod__(this);
        if (x!=null) {
            return x;
        }
        throw Py.TypeError(_unsupportedop("%",o2));
    }

    /**
      * Implements the Python expression <code>this %= o2</code>
      * @param     o2 the object to perform this inplace binary
      *            operation with.
      * @return    the result of the imod.
      * @exception Py.TypeError if this operation can't be performed
      *            with these operands.
      **/
    public final PyObject _imod(PyObject o2) {
        PyType t1=this.getType();
        PyType t2=o2.getType();
        if (t1==t2||t1.builtin&&t2.builtin) {
            return this._basic_imod(o2);
        }
        PyObject impl=t1.lookup("__imod__");
        if (impl!=null) {
            PyObject res=impl.__get__(this,t1).__call__(o2);
            if (res!=Py.NotImplemented) {
                return res;
            }
        }
        return _binop_rule(t1,o2,t2,"__mod__","__rmod__","%");
    }

    /**
     * Implements the Python expression <code>this %= o2</code>
     * when this and o2 have the same type or are builtin types.
     * @param     o2 the object to perform this inplace binary
     *            operation with.
     * @return    the result of the imod.
     * @exception Py.TypeError if this operation can't be performed
     *            with these operands.
     **/
    final PyObject _basic_imod(PyObject o2) {
        PyObject x=__imod__(o2);
        if (x!=null) {
            return x;
        }
        return this._basic_mod(o2);
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
     * @return    the result of the idivmod, or null if this operation
     *            is not defined
     **/
    public PyObject __idivmod__(PyObject other) {
        return null;
    }

    /**
      * Implements the Python expression <code>this divmod o2</code>
      * @param     o2 the object to perform this binary operation with.
      * @return    the result of the divmod.
      * @exception Py.TypeError if this operation can't be performed
      *            with these operands.
      **/
    public final PyObject _divmod(PyObject o2) {
        PyType t1=this.getType();
        PyType t2=o2.getType();
        if (t1==t2||t1.builtin&&t2.builtin) {
            return this._basic_divmod(o2);
        }
        return _binop_rule(t1,o2,t2,"__divmod__","__rdivmod__","divmod");
    }

    /**
     * Implements the Python expression <code>this divmod o2</code>
     * when this and o2 have the same type or are builtin types.
     * @param     o2 the object to perform this binary operation with.
     * @return    the result of the divmod.
     * @exception Py.TypeError if this operation can't be performed
     *            with these operands.
     **/
    final PyObject _basic_divmod(PyObject o2) {
        PyObject x=__divmod__(o2);
        if (x!=null) {
            return x;
        }
        x=o2.__rdivmod__(this);
        if (x!=null) {
            return x;
        }
        throw Py.TypeError(_unsupportedop("divmod",o2));
    }

    /**
      * Implements the Python expression <code>this divmod= o2</code>
      * @param     o2 the object to perform this inplace binary
      *            operation with.
      * @return    the result of the idivmod.
      * @exception Py.TypeError if this operation can't be performed
      *            with these operands.
      **/
    public final PyObject _idivmod(PyObject o2) {
        PyType t1=this.getType();
        PyType t2=o2.getType();
        if (t1==t2||t1.builtin&&t2.builtin) {
            return this._basic_idivmod(o2);
        }
        PyObject impl=t1.lookup("__idivmod__");
        if (impl!=null) {
            PyObject res=impl.__get__(this,t1).__call__(o2);
            if (res!=Py.NotImplemented) {
                return res;
            }
        }
        return _binop_rule(t1,o2,t2,"__divmod__","__rdivmod__","divmod");
    }

    /**
     * Implements the Python expression <code>this divmod= o2</code>
     * when this and o2 have the same type or are builtin types.
     * @param     o2 the object to perform this inplace binary
     *            operation with.
     * @return    the result of the idivmod.
     * @exception Py.TypeError if this operation can't be performed
     *            with these operands.
     **/
    final PyObject _basic_idivmod(PyObject o2) {
        PyObject x=__idivmod__(o2);
        if (x!=null) {
            return x;
        }
        return this._basic_divmod(o2);
    }

    /**
     * Equivalent to the standard Python __pow__ method
     * @param     other the object to perform this binary operation with
     *            (the right-hand operand).
     * @return    the result of the pow, or null if this operation
     *            is not defined
     **/
    public PyObject __pow__(PyObject other) {
        return __pow__(other,null);
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
     * @return    the result of the ipow, or null if this operation
     *            is not defined
     **/
    public PyObject __ipow__(PyObject other) {
        return null;
    }

    /**
      * Implements the Python expression <code>this ** o2</code>
      * @param     o2 the object to perform this binary operation with.
      * @return    the result of the pow.
      * @exception Py.TypeError if this operation can't be performed
      *            with these operands.
      **/
    public final PyObject _pow(PyObject o2) {
        PyType t1=this.getType();
        PyType t2=o2.getType();
        if (t1==t2||t1.builtin&&t2.builtin) {
            return this._basic_pow(o2);
        }
        return _binop_rule(t1,o2,t2,"__pow__","__rpow__","**");
    }

    /**
     * Implements the Python expression <code>this ** o2</code>
     * when this and o2 have the same type or are builtin types.
     * @param     o2 the object to perform this binary operation with.
     * @return    the result of the pow.
     * @exception Py.TypeError if this operation can't be performed
     *            with these operands.
     **/
    final PyObject _basic_pow(PyObject o2) {
        PyObject x=__pow__(o2);
        if (x!=null) {
            return x;
        }
        x=o2.__rpow__(this);
        if (x!=null) {
            return x;
        }
        throw Py.TypeError(_unsupportedop("**",o2));
    }

    /**
      * Implements the Python expression <code>this **= o2</code>
      * @param     o2 the object to perform this inplace binary
      *            operation with.
      * @return    the result of the ipow.
      * @exception Py.TypeError if this operation can't be performed
      *            with these operands.
      **/
    public final PyObject _ipow(PyObject o2) {
        PyType t1=this.getType();
        PyType t2=o2.getType();
        if (t1==t2||t1.builtin&&t2.builtin) {
            return this._basic_ipow(o2);
        }
        PyObject impl=t1.lookup("__ipow__");
        if (impl!=null) {
            PyObject res=impl.__get__(this,t1).__call__(o2);
            if (res!=Py.NotImplemented) {
                return res;
            }
        }
        return _binop_rule(t1,o2,t2,"__pow__","__rpow__","**");
    }

    /**
     * Implements the Python expression <code>this **= o2</code>
     * when this and o2 have the same type or are builtin types.
     * @param     o2 the object to perform this inplace binary
     *            operation with.
     * @return    the result of the ipow.
     * @exception Py.TypeError if this operation can't be performed
     *            with these operands.
     **/
    final PyObject _basic_ipow(PyObject o2) {
        PyObject x=__ipow__(o2);
        if (x!=null) {
            return x;
        }
        return this._basic_pow(o2);
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
     * @return    the result of the ilshift, or null if this operation
     *            is not defined
     **/
    public PyObject __ilshift__(PyObject other) {
        return null;
    }

    /**
      * Implements the Python expression <code>this << o2</code>
      * @param     o2 the object to perform this binary operation with.
      * @return    the result of the lshift.
      * @exception Py.TypeError if this operation can't be performed
      *            with these operands.
      **/
    public final PyObject _lshift(PyObject o2) {
        PyType t1=this.getType();
        PyType t2=o2.getType();
        if (t1==t2||t1.builtin&&t2.builtin) {
            return this._basic_lshift(o2);
        }
        return _binop_rule(t1,o2,t2,"__lshift__","__rlshift__","<<");
    }

    /**
     * Implements the Python expression <code>this << o2</code>
     * when this and o2 have the same type or are builtin types.
     * @param     o2 the object to perform this binary operation with.
     * @return    the result of the lshift.
     * @exception Py.TypeError if this operation can't be performed
     *            with these operands.
     **/
    final PyObject _basic_lshift(PyObject o2) {
        PyObject x=__lshift__(o2);
        if (x!=null) {
            return x;
        }
        x=o2.__rlshift__(this);
        if (x!=null) {
            return x;
        }
        throw Py.TypeError(_unsupportedop("<<",o2));
    }

    /**
      * Implements the Python expression <code>this <<= o2</code>
      * @param     o2 the object to perform this inplace binary
      *            operation with.
      * @return    the result of the ilshift.
      * @exception Py.TypeError if this operation can't be performed
      *            with these operands.
      **/
    public final PyObject _ilshift(PyObject o2) {
        PyType t1=this.getType();
        PyType t2=o2.getType();
        if (t1==t2||t1.builtin&&t2.builtin) {
            return this._basic_ilshift(o2);
        }
        PyObject impl=t1.lookup("__ilshift__");
        if (impl!=null) {
            PyObject res=impl.__get__(this,t1).__call__(o2);
            if (res!=Py.NotImplemented) {
                return res;
            }
        }
        return _binop_rule(t1,o2,t2,"__lshift__","__rlshift__","<<");
    }

    /**
     * Implements the Python expression <code>this <<= o2</code>
     * when this and o2 have the same type or are builtin types.
     * @param     o2 the object to perform this inplace binary
     *            operation with.
     * @return    the result of the ilshift.
     * @exception Py.TypeError if this operation can't be performed
     *            with these operands.
     **/
    final PyObject _basic_ilshift(PyObject o2) {
        PyObject x=__ilshift__(o2);
        if (x!=null) {
            return x;
        }
        return this._basic_lshift(o2);
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
     * @return    the result of the irshift, or null if this operation
     *            is not defined
     **/
    public PyObject __irshift__(PyObject other) {
        return null;
    }

    /**
      * Implements the Python expression <code>this >> o2</code>
      * @param     o2 the object to perform this binary operation with.
      * @return    the result of the rshift.
      * @exception Py.TypeError if this operation can't be performed
      *            with these operands.
      **/
    public final PyObject _rshift(PyObject o2) {
        PyType t1=this.getType();
        PyType t2=o2.getType();
        if (t1==t2||t1.builtin&&t2.builtin) {
            return this._basic_rshift(o2);
        }
        return _binop_rule(t1,o2,t2,"__rshift__","__rrshift__",">>");
    }

    /**
     * Implements the Python expression <code>this >> o2</code>
     * when this and o2 have the same type or are builtin types.
     * @param     o2 the object to perform this binary operation with.
     * @return    the result of the rshift.
     * @exception Py.TypeError if this operation can't be performed
     *            with these operands.
     **/
    final PyObject _basic_rshift(PyObject o2) {
        PyObject x=__rshift__(o2);
        if (x!=null) {
            return x;
        }
        x=o2.__rrshift__(this);
        if (x!=null) {
            return x;
        }
        throw Py.TypeError(_unsupportedop(">>",o2));
    }

    /**
      * Implements the Python expression <code>this >>= o2</code>
      * @param     o2 the object to perform this inplace binary
      *            operation with.
      * @return    the result of the irshift.
      * @exception Py.TypeError if this operation can't be performed
      *            with these operands.
      **/
    public final PyObject _irshift(PyObject o2) {
        PyType t1=this.getType();
        PyType t2=o2.getType();
        if (t1==t2||t1.builtin&&t2.builtin) {
            return this._basic_irshift(o2);
        }
        PyObject impl=t1.lookup("__irshift__");
        if (impl!=null) {
            PyObject res=impl.__get__(this,t1).__call__(o2);
            if (res!=Py.NotImplemented) {
                return res;
            }
        }
        return _binop_rule(t1,o2,t2,"__rshift__","__rrshift__",">>");
    }

    /**
     * Implements the Python expression <code>this >>= o2</code>
     * when this and o2 have the same type or are builtin types.
     * @param     o2 the object to perform this inplace binary
     *            operation with.
     * @return    the result of the irshift.
     * @exception Py.TypeError if this operation can't be performed
     *            with these operands.
     **/
    final PyObject _basic_irshift(PyObject o2) {
        PyObject x=__irshift__(o2);
        if (x!=null) {
            return x;
        }
        return this._basic_rshift(o2);
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
     * @return    the result of the iand, or null if this operation
     *            is not defined
     **/
    public PyObject __iand__(PyObject other) {
        return null;
    }

    /**
      * Implements the Python expression <code>this & o2</code>
      * @param     o2 the object to perform this binary operation with.
      * @return    the result of the and.
      * @exception Py.TypeError if this operation can't be performed
      *            with these operands.
      **/
    public final PyObject _and(PyObject o2) {
        PyType t1=this.getType();
        PyType t2=o2.getType();
        if (t1==t2||t1.builtin&&t2.builtin) {
            return this._basic_and(o2);
        }
        return _binop_rule(t1,o2,t2,"__and__","__rand__","&");
    }

    /**
     * Implements the Python expression <code>this & o2</code>
     * when this and o2 have the same type or are builtin types.
     * @param     o2 the object to perform this binary operation with.
     * @return    the result of the and.
     * @exception Py.TypeError if this operation can't be performed
     *            with these operands.
     **/
    final PyObject _basic_and(PyObject o2) {
        PyObject x=__and__(o2);
        if (x!=null) {
            return x;
        }
        x=o2.__rand__(this);
        if (x!=null) {
            return x;
        }
        throw Py.TypeError(_unsupportedop("&",o2));
    }

    /**
      * Implements the Python expression <code>this &= o2</code>
      * @param     o2 the object to perform this inplace binary
      *            operation with.
      * @return    the result of the iand.
      * @exception Py.TypeError if this operation can't be performed
      *            with these operands.
      **/
    public final PyObject _iand(PyObject o2) {
        PyType t1=this.getType();
        PyType t2=o2.getType();
        if (t1==t2||t1.builtin&&t2.builtin) {
            return this._basic_iand(o2);
        }
        PyObject impl=t1.lookup("__iand__");
        if (impl!=null) {
            PyObject res=impl.__get__(this,t1).__call__(o2);
            if (res!=Py.NotImplemented) {
                return res;
            }
        }
        return _binop_rule(t1,o2,t2,"__and__","__rand__","&");
    }

    /**
     * Implements the Python expression <code>this &= o2</code>
     * when this and o2 have the same type or are builtin types.
     * @param     o2 the object to perform this inplace binary
     *            operation with.
     * @return    the result of the iand.
     * @exception Py.TypeError if this operation can't be performed
     *            with these operands.
     **/
    final PyObject _basic_iand(PyObject o2) {
        PyObject x=__iand__(o2);
        if (x!=null) {
            return x;
        }
        return this._basic_and(o2);
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
     * @return    the result of the ior, or null if this operation
     *            is not defined
     **/
    public PyObject __ior__(PyObject other) {
        return null;
    }

    /**
      * Implements the Python expression <code>this | o2</code>
      * @param     o2 the object to perform this binary operation with.
      * @return    the result of the or.
      * @exception Py.TypeError if this operation can't be performed
      *            with these operands.
      **/
    public final PyObject _or(PyObject o2) {
        PyType t1=this.getType();
        PyType t2=o2.getType();
        if (t1==t2||t1.builtin&&t2.builtin) {
            return this._basic_or(o2);
        }
        return _binop_rule(t1,o2,t2,"__or__","__ror__","|");
    }

    /**
     * Implements the Python expression <code>this | o2</code>
     * when this and o2 have the same type or are builtin types.
     * @param     o2 the object to perform this binary operation with.
     * @return    the result of the or.
     * @exception Py.TypeError if this operation can't be performed
     *            with these operands.
     **/
    final PyObject _basic_or(PyObject o2) {
        PyObject x=__or__(o2);
        if (x!=null) {
            return x;
        }
        x=o2.__ror__(this);
        if (x!=null) {
            return x;
        }
        throw Py.TypeError(_unsupportedop("|",o2));
    }

    /**
      * Implements the Python expression <code>this |= o2</code>
      * @param     o2 the object to perform this inplace binary
      *            operation with.
      * @return    the result of the ior.
      * @exception Py.TypeError if this operation can't be performed
      *            with these operands.
      **/
    public final PyObject _ior(PyObject o2) {
        PyType t1=this.getType();
        PyType t2=o2.getType();
        if (t1==t2||t1.builtin&&t2.builtin) {
            return this._basic_ior(o2);
        }
        PyObject impl=t1.lookup("__ior__");
        if (impl!=null) {
            PyObject res=impl.__get__(this,t1).__call__(o2);
            if (res!=Py.NotImplemented) {
                return res;
            }
        }
        return _binop_rule(t1,o2,t2,"__or__","__ror__","|");
    }

    /**
     * Implements the Python expression <code>this |= o2</code>
     * when this and o2 have the same type or are builtin types.
     * @param     o2 the object to perform this inplace binary
     *            operation with.
     * @return    the result of the ior.
     * @exception Py.TypeError if this operation can't be performed
     *            with these operands.
     **/
    final PyObject _basic_ior(PyObject o2) {
        PyObject x=__ior__(o2);
        if (x!=null) {
            return x;
        }
        return this._basic_or(o2);
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
     * @return    the result of the ixor, or null if this operation
     *            is not defined
     **/
    public PyObject __ixor__(PyObject other) {
        return null;
    }

    /**
      * Implements the Python expression <code>this ^ o2</code>
      * @param     o2 the object to perform this binary operation with.
      * @return    the result of the xor.
      * @exception Py.TypeError if this operation can't be performed
      *            with these operands.
      **/
    public final PyObject _xor(PyObject o2) {
        PyType t1=this.getType();
        PyType t2=o2.getType();
        if (t1==t2||t1.builtin&&t2.builtin) {
            return this._basic_xor(o2);
        }
        return _binop_rule(t1,o2,t2,"__xor__","__rxor__","^");
    }

    /**
     * Implements the Python expression <code>this ^ o2</code>
     * when this and o2 have the same type or are builtin types.
     * @param     o2 the object to perform this binary operation with.
     * @return    the result of the xor.
     * @exception Py.TypeError if this operation can't be performed
     *            with these operands.
     **/
    final PyObject _basic_xor(PyObject o2) {
        PyObject x=__xor__(o2);
        if (x!=null) {
            return x;
        }
        x=o2.__rxor__(this);
        if (x!=null) {
            return x;
        }
        throw Py.TypeError(_unsupportedop("^",o2));
    }

    /**
      * Implements the Python expression <code>this ^= o2</code>
      * @param     o2 the object to perform this inplace binary
      *            operation with.
      * @return    the result of the ixor.
      * @exception Py.TypeError if this operation can't be performed
      *            with these operands.
      **/
    public final PyObject _ixor(PyObject o2) {
        PyType t1=this.getType();
        PyType t2=o2.getType();
        if (t1==t2||t1.builtin&&t2.builtin) {
            return this._basic_ixor(o2);
        }
        PyObject impl=t1.lookup("__ixor__");
        if (impl!=null) {
            PyObject res=impl.__get__(this,t1).__call__(o2);
            if (res!=Py.NotImplemented) {
                return res;
            }
        }
        return _binop_rule(t1,o2,t2,"__xor__","__rxor__","^");
    }

    /**
     * Implements the Python expression <code>this ^= o2</code>
     * when this and o2 have the same type or are builtin types.
     * @param     o2 the object to perform this inplace binary
     *            operation with.
     * @return    the result of the ixor.
     * @exception Py.TypeError if this operation can't be performed
     *            with these operands.
     **/
    final PyObject _basic_ixor(PyObject o2) {
        PyObject x=__ixor__(o2);
        if (x!=null) {
            return x;
        }
        return this._basic_xor(o2);
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

    /**
     * Shortcut for calling a method on a PyObject with one extra
     * initial argument.
     *
     * @param name the name of the method to call.  This must be an
     *        interned string!
     * @param arg1 the first argument of the method.
     * @param args an array of the arguments to the call.
     * @param keywords the keywords to use in the call.
     * @return the result of calling the method name with arg1 args
     * and keywords
     **/
    public PyObject invoke(String name, PyObject arg1, PyObject[] args, String[] keywords) {
        PyObject f = __getattr__(name);
        return f.__call__(arg1, args, keywords);
    }

    /* descriptors and lookup protocols */

    /** xxx implements where meaningful
     * @return internal object per instance dict or null
     */
    public PyObject fastGetDict() {
        return null;
    }

    /** xxx implements where meaningful
     * @return internal object __dict__ or null
     */
    public PyObject getDict() {
        return null;
    }

    public void setDict(PyObject newDict) {
        // fallback if setDict not implemented in subclass
        throw Py.TypeError("can't set attribute '__dict__' of instance of " + getType().fastGetName());
    }

    public void delDict() {
        // fallback to error
        throw Py.TypeError("can't delete attribute '__dict__' of instance of '" + getType().fastGetName()+ "'");
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
    /**
     * Get descriptor for this PyObject.
     *
     * @param obj -
     *            the instance accessing this descriptor. Can be null if this is
     *            being accessed by a type.
     * @param type -
     *            the type accessing this descriptor. Will be null if obj exists
     *            as obj is of the type accessing the descriptor.
     * @return - the object defined for this descriptor for the given obj and
     *         type.
     */
    public PyObject __get__(PyObject obj, PyObject type) {
        return _doget(obj, type);
    }

    public void __set__(PyObject obj, PyObject value) {
        if (!_doset(obj, value)) {
            throw Py.AttributeError("object internal __set__ impl is abstract");
        }
    }

    public void __delete__(PyObject obj) {
        throw Py.AttributeError("object internal __delete__ impl is abstract");
    }

    @ExposedMethod
    final PyObject object___getattribute__(PyObject arg0) {
        String name = asName(arg0);
        PyObject ret = object___findattr__(name);
        if(ret == null)
            noAttributeError(name);
        return ret;
    }

    // name must be interned
    final PyObject object___findattr__(String name) {

        PyObject descr = objtype.lookup(name);
        PyObject res;

        if (descr != null) {
            if (descr.isDataDescr()) {
                res = descr.__get__(this, objtype);
                if (res != null)
                    return res;
            }
        }

        PyObject obj_dict = fastGetDict();
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

    @ExposedMethod
    final void object___setattr__(PyObject name, PyObject value) {
        object___setattr__(asName(name), value);
    }

    private final void object___setattr__(String name, PyObject value) {
        PyObject descr = objtype.lookup(name);

        boolean set = false;

        if (descr != null) {
            set = descr.implementsDescrSet();
            if (set && descr.isDataDescr()) {
                descr.__set__(this, value);
                return;
            }
        }

        PyObject obj_dict = fastGetDict();
        if (obj_dict != null) {
            obj_dict.__setitem__(name, value);
            return;
        }

        if (set) {
            descr.__set__(this, value);
        }

        if (descr != null) {
            readonlyAttributeError(name);
        }

        noAttributeError(name);
    }

    @ExposedMethod
    final void object___delattr__(PyObject name) {
        object___delattr__(asName(name));
    }

    public static final String asName(PyObject obj) {
        try {
            return obj.asName(0);
        } catch(PyObject.ConversionException e) {
            throw Py.TypeError("attribute name must be a string");
        }
    }

    final void object___delattr__(String name) {
        PyObject descr = objtype.lookup(name);

        boolean delete = false;

        if (descr != null) {
            delete = descr.implementsDescrDelete();
            if (delete && descr.isDataDescr()) {
                descr.__delete__(this);
                return;
            }
        }

        PyObject obj_dict = fastGetDict();
        if (obj_dict != null) {
            try {
                obj_dict.__delitem__(name);
            } catch (PyException exc) {
                if (Py.matchException(exc, Py.KeyError))
                    noAttributeError(name);
                else
                    throw exc;
            }
            return;
        }

        if (delete) {
            descr.__delete__(this);
        }

        if (descr != null) {
            readonlyAttributeError(name);
        }

        noAttributeError(name);
    }

    /**
     * Used for pickling.  Default implementation calls object___reduce__.
     *
     * @return a tuple of (class, tuple)
     */
    public PyObject __reduce__() {
        return object___reduce__();
    }

    @ExposedMethod
    final PyObject object___reduce__() {
        return object___reduce_ex__(0);
    }

    /** Used for pickling.  If the subclass specifies __reduce__, it will
     * override __reduce_ex__ in the base-class, even if __reduce_ex__ was
     * called with an argument.
     *
     * @param arg PyInteger specifying reduce algorithm (method without this
     * argument defaults to 0).
     * @return a tuple of (class, tuple)
     */
    public PyObject __reduce_ex__(int arg) {
        return object___reduce_ex__(arg);
    }
    public PyObject __reduce_ex__() {
        return object___reduce_ex__(0);
    }

    @ExposedMethod(defaults = "0")
    final PyObject object___reduce_ex__(int arg) {
        PyObject res;

        PyObject clsreduce = this.getType().__findattr__("__reduce__");
        PyObject objreduce = (new PyObject()).getType().__findattr__("__reduce__");

        if (clsreduce != objreduce) {
            res = this.__reduce__();
        } else if (arg >= 2) {
            res = reduce_2();
        } else {
            PyObject copyreg = __builtin__.__import__("copy_reg", null, null, Py.EmptyTuple);
            PyObject copyreg_reduce = copyreg.__findattr__("_reduce_ex");
            res = copyreg_reduce.__call__(this, new PyInteger(arg));
        }
        return res;
    }

    private static PyObject slotnames(PyObject cls) {
        PyObject slotnames;

        slotnames = cls.fastGetDict().__finditem__("__slotnames__");
        if (null != slotnames) {
            return slotnames;
        }

        PyObject copyreg = __builtin__.__import__("copy_reg", null, null, Py.EmptyTuple);
        PyObject copyreg_slotnames = copyreg.__findattr__("_slotnames");
        slotnames = copyreg_slotnames.__call__(cls);
        if (null != slotnames && Py.None != slotnames && (!(slotnames instanceof PyList))) {
            throw Py.TypeError("copy_reg._slotnames didn't return a list or None");
        }

        return slotnames;
    }

    private PyObject reduce_2() {
        PyObject args, state;
        PyObject res = null;
        int n,i;

        PyObject cls = this.__findattr__("__class__");

        PyObject getnewargs = this.__findattr__("__getnewargs__");
        if (null != getnewargs) {
            args = getnewargs.__call__();
            if (null != args && !(args instanceof PyTuple)) {
                throw Py.TypeError("__getnewargs__ should return a tuple");
            }
        } else {
            args = Py.EmptyTuple;
        }

        PyObject getstate = this.__findattr__("__getstate__");
        if (null != getstate) {
            state = getstate.__call__();
            if (null == state) {
                return res;
            }
        } else {
            state = this.__findattr__("__dict__");
            if (null == state) {
                state = Py.None;
            }

            PyObject names = slotnames(cls);
            if (null == names) {
                return res;
            }

            if (names != Py.None) {
                if (!(names instanceof PyList)) {
                    throw Py.AssertionError("slots not a list");
                }
                PyObject slots = new PyDictionary();

                n = 0;
                for (i = 0; i < ((PyList)names).size(); i++) {
                    PyObject name = ((PyList)names).pyget(i);
                    PyObject value = this.__findattr__(name.toString());
                    if (null == value) {
                        // do nothing
                    } else {
                        slots.__setitem__(name, value);
                        n++;
                    }
                }
                if (n > 0) {
                    state = new PyTuple(state, slots);
                }
            }
        }
        PyObject listitems;
        PyObject dictitems;
        if (!(this instanceof PyList)) {
            listitems = Py.None;
        } else {
            listitems = ((PyList)this).__iter__();
        }
        if (!(this instanceof PyDictionary)) {
            dictitems = Py.None;
        } else {
            dictitems = ((PyDictionary)this).iteritems();
        }

        PyObject copyreg = __builtin__.__import__("copy_reg", null, null, Py.EmptyTuple);
        PyObject newobj = copyreg.__findattr__("__newobj__");

        n = ((PyTuple)args).size();
        PyObject args2[] = new PyObject[n+1];
        args2[0] = cls;
        for(i = 0; i < n; i++) {
            args2[i+1] = ((PyTuple)args).pyget(i);
        }

        return new PyTuple(newobj, new PyTuple(args2), state, listitems, dictitems);
    }

    public PyTuple __getnewargs__() {
        //default is empty tuple
        return new PyTuple();
    }

    /* arguments' conversion helpers */

    public static class ConversionException extends Exception {

        public int index;

        public ConversionException(int index) {
            this.index = index;
        }

    }

    public String asString(int index) throws ConversionException {
        throw new ConversionException(index);
    }

    public String asString(){
        throw Py.TypeError("expected a str");
    }

    public String asStringOrNull(int index) throws ConversionException {
       return asString(index);
    }

    public String asStringOrNull(){
        return asString();
    }

    // TODO - remove when all asName users are moved to the @Exposed annotation
    public String asName(int index) throws ConversionException {
        throw new ConversionException(index);
    }

    // TODO - remove when all generated users are migrated to @Exposed and asInt()
    public int asInt(int index) throws ConversionException {
        throw new ConversionException(index);
    }

    public int asInt() {
        PyObject intObj;
        try {
            intObj = __int__();
        } catch (PyException pye) {
            if (Py.matchException(pye, Py.AttributeError)) {
                throw Py.TypeError("an integer is required");
            }
            throw pye;
        }
        if (!(intObj instanceof PyInteger) && !(intObj instanceof PyLong)) {
            // Shouldn't happen except with buggy builtin types
            throw Py.TypeError("nb_int should return int object");
        }
        return intObj.asInt();
    }

    public long asLong(int index) throws ConversionException {
        throw new ConversionException(index);
    }

    /**
     * Convert this object into a double. Throws a PyException on failure.
     *
     * @return a double value
     */
    public double asDouble() {
        PyFloat floatObj;
        try {
            floatObj = __float__();
        } catch (PyException pye) {
            if (Py.matchException(pye, Py.AttributeError)) {
                throw Py.TypeError("a float is required");
            }
            throw pye;
        }
        return floatObj.asDouble();
    }

    /**
     * Convert this object into an index-sized integer. Throws a PyException on failure.
     *
     * @return an index-sized int
     */
    public int asIndex() {
        return asIndex(null);
    }

    /**
     * Convert this object into an index-sized integer.
     *
     * Throws a Python exception on Overflow if specified an exception type for err.
     *
     * @param err the Python exception to raise on OverflowErrors
     * @return an index-sized int
     */
    public int asIndex(PyObject err) {
        // OverflowErrors are handled in PyLong.asIndex
        return __index__().asInt();
    }

    static {
        for (Class unbootstrapped : Py.BOOTSTRAP_TYPES) {
            Py.writeWarning("init", "Bootstrap type wasn't encountered in bootstrapping[class="
                    + unbootstrapped + "]");
        }
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
