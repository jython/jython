// Copyright (c) Corporation for National Research Initiatives
package org.python.core;

import java.io.Serializable;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.python.expose.ExposedClassMethod;
import org.python.expose.ExposedDelete;
import org.python.expose.ExposedGet;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedSet;
import org.python.expose.ExposedType;
import org.python.modules.gc;
import org.python.util.Generic;

/**
 * All objects known to the Jython runtime system are represented by an instance of the class
 * {@code PyObject} or one of its subclasses.
 */
@ExposedType(name = "object", doc = BuiltinDocs.object_doc)
public class PyObject implements Serializable {

    public static final PyType TYPE = PyType.fromClass(PyObject.class);

    /**
     * This should have been suited at {@link org.python.modules.gc}, but that would cause a
     * dependency cycle in the init-phases of {@code gc.class} and {@code PyObject.class}. Now this
     * boolean mirrors the presence of the {@link org.python.modules.gc#MONITOR_GLOBAL}-flag in
     * Jython's gc module.<br>
     * <br>
     * <b>Do not change manually.</b>
     */
    public static boolean gcMonitorGlobal = false;

    /** The type of this object. */
    protected PyType objtype;

    /**
     * {@code attributes} is a general purpose linked list of arbitrary Java objects that should be
     * kept alive by this PyObject. These objects can be accessed by the methods and keys in
     * {@link org.python.core.JyAttribute}. A notable attribute is the javaProxy (accessible via
     * {@code JyAttribute.getAttr(this, JyAttribute.JAVA_PROXY_ATTR)}), an underlying Java instance
     * that this object is wrapping or is a subclass of. Anything attempting to use the proxy should
     * go through {@link #getJavaProxy()} which ensures that it's initialized.
     *
     * @see org.python.core.JyAttribute
     * @see org.python.core.JyAttribute#JAVA_PROXY_ATTR
     * @see #getJavaProxy()
     */
    protected Object attributes;

    /** Primitives classes their wrapper classes. */
    private static final Map<Class<?>, Class<?>> primitiveMap = Generic.map();

    static {
        primitiveMap.put(Character.TYPE, Character.class);
        primitiveMap.put(Boolean.TYPE, Boolean.class);
        primitiveMap.put(Byte.TYPE, Byte.class);
        primitiveMap.put(Short.TYPE, Short.class);
        primitiveMap.put(Integer.TYPE, Integer.class);
        primitiveMap.put(Long.TYPE, Long.class);
        primitiveMap.put(Float.TYPE, Float.class);
        primitiveMap.put(Double.TYPE, Double.class);

        PyType.ensureBootstrapped();
    }

    public PyObject(PyType objtype) {
        this.objtype = objtype;
        if (gcMonitorGlobal) {
            gc.monitorObject(this);
        }
    }

    /**
     * The standard constructor for a <code>PyObject</code>. It will set the <code>objtype</code>
     * field to correspond to the specific subclass of <code>PyObject</code> being instantiated.
     **/
    public PyObject() {
        objtype = PyType.fromClass(getClass());
        if (gcMonitorGlobal) {
            gc.monitorObject(this);
        }
    }

    /**
     * Creates the PyObject for the base type. The argument only exists to make the constructor
     * distinct.
     */
    PyObject(boolean ignored) {
        objtype = (PyType) this;
        if (gcMonitorGlobal) {
            gc.monitorObject(this);
        }
    }

    @ExposedNew
    static final PyObject object___new__(PyNewWrapper new_, boolean init, PyType subtype,
            PyObject[] args, String[] keywords) {
        // don't allow arguments if the default object.__init__() is about to be called
        PyObject[] where = new PyObject[1];
        subtype.lookup_where("__init__", where);
        if (where[0] == TYPE && args.length > 0) {
            throw Py.TypeError("object.__new__() takes no parameters");
        }

        if (subtype.isAbstract()) {
            // Compute ", ".join(sorted(type.__abstractmethods__)) into methods
            PyObject sorted = Py.getSystemState().getBuiltins().__getitem__(Py.newString("sorted"));
            PyString methods =
                    Py.newString(", ").join(sorted.__call__(subtype.getAbstractmethods()));
            throw Py.TypeError(String.format(
                    "Can't instantiate abstract class %s with abstract " + "methods %s",
                    subtype.fastGetName(), methods));
        }

        return new_.for_type == subtype ? new PyObject() : new PyObjectDerived(subtype);
    }

    /**
     * From Jython 2.7 on, {@code PyObject}s must not have finalizers directly. If a finalizer,
     * a.k.a. {@code __del__} is needed, follow the instructions in the documentation of
     * {@link org.python.core.finalization.FinalizablePyObject}.
     * <p>
     * Note that this empty finalizer implementation is optimized away by the JVM. (See <a
     * href=http://www.javaspecialists.eu/archive/Issue170.html>Discovering Objects with Non-trivial
     * Finalizers</a>). So {@code PyObject}s are not expensively treated as finalizable objects by
     * the Java-GC. Its single intention is to prevent subclasses from having Java-style finalizers.
     */
    @SuppressWarnings("deprecation") // See the Javadoc
    @Override
    protected final void finalize() throws Throwable {}

    @ExposedMethod(doc = BuiltinDocs.object___init___doc)
    final void object___init__(PyObject[] args, String[] keywords) {}

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

    /** Dispatch __init__ behavior */
    public void dispatch__init__(PyObject[] args, String[] keywords) {}

    /**
     * Attempts to automatically initialize our Java proxy if we have one and it wasn't initialized
     * by our __init__.
     */
    void proxyInit() {
        Class<?> c = getType().getProxyType();
        Object javaProxy = JyAttribute.getAttr(this, JyAttribute.JAVA_PROXY_ATTR);
        if (javaProxy != null || c == null) {
            return;
        }
        if (!PyProxy.class.isAssignableFrom(c)) {
            throw Py.SystemError(
                    "Automatic proxy initialization should only occur on proxy classes");
        }
        PyProxy proxy;
        Object[] previous = ThreadContext.initializingProxy.get();
        ThreadContext.initializingProxy.set(new Object[] {this});
        try {
            try {
                proxy = (PyProxy) c.getDeclaredConstructor().newInstance();
            } catch (InstantiationException | NoSuchMethodException e) {
                Class<?> sup = c.getSuperclass();
                String msg = "Default constructor failed/missing for Java superclass";
                if (sup != null) {
                    msg += " " + sup.getName();
                }
                throw Py.TypeError(msg);
            } catch (Exception exc) {
                throw Py.JavaError(exc);
            }
        } finally {
            ThreadContext.initializingProxy.set(previous);
        }
        javaProxy = JyAttribute.getAttr(this, JyAttribute.JAVA_PROXY_ATTR);
        if (javaProxy != null && javaProxy != proxy) {
            throw Py.TypeError("Proxy instance already initialized");
        }
        PyObject proxyInstance = proxy._getPyInstance();
        if (proxyInstance != null && proxyInstance != this) {
            throw Py.TypeError("Proxy initialized with another instance");
        }
        JyAttribute.setAttr(this, JyAttribute.JAVA_PROXY_ATTR, proxy);
    }

    /**
     * Equivalent to the standard Python <code>__repr__</code> method. Each sub-class of
     * <code>PyObject</code> is likely to re-define this method to provide for its own reproduction.
     **/
    /*
     * The effect of exposing __repr__ as __str__ is that a Python call to o.__str__() will land
     * here. (A Java call to o.__str__() lands here too because __str__ is defined to call
     * __repr__.) This will continue to be true in any sub-class that does not expose a __str__ of
     * its own. (Such a class should override Java __str__ to call the method exposed as Python
     * __str__.) Note that we expose a non-final method, therefore in a class that (Java-)overrides
     * __repr__, Python (and Java) calls like o.__str__() will land on the overridden __repr__.
     *
     * This design, though long-standing, has caused confusion to the implementors of types, and
     * seems to make *Derived.java classes more complicated. We should seek a more transparent
     * design.
     */
    @ExposedMethod(names = "__str__", doc = BuiltinDocs.object___str___doc)
    public PyString __repr__() {
        return Py.newStringOrUnicode(toString());
    }

    @Override
    public String toString() {
        return object_toString();
    }

    @ExposedMethod(names = "__repr__", doc = BuiltinDocs.object___repr___doc)
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
     * Equivalent to the standard Python __str__ method. The default implementation (in
     * <code>PyObject</code>) calls {@link #__repr__()}, making it unnecessary to override
     * <code>__str__</code> in sub-classes of <code>PyObject</code> where both forms are the same. A
     * common choice is to provide the same implementation to <code>__str__</code> and
     * <code>toString</code>, for consistency in the printed form of objects between Python and
     * Java.
     **/
    public PyString __str__() {
        return __repr__();
    }

    /**
     * PyObjects that implement <code>org.python.core.finalization.HasFinalizeTrigger</code> shall
     * implement this method via:<br>
     * <code>FinalizeTrigger.ensureFinalizer(this);</code>
     **/
    @ExposedMethod
    public void __ensure_finalizer__() {}

    public PyUnicode __unicode__() {
        return new PyUnicode(__str__());
    }

    /**
     * Equivalent to the standard Python __hash__ method. This method can not be overridden.
     * Instead, you should override the standard Java <code>hashCode</code> method to return an
     * appropriate hash code for the <code>PyObject</code>.
     **/
    public final PyInteger __hash__() {
        return new PyInteger(hashCode());
    }

    @Override
    public int hashCode() {
        return object___hash__();
    }

    @ExposedMethod(doc = BuiltinDocs.object___hash___doc)
    final int object___hash__() {
        return System.identityHashCode(this);
    }

    /**
     * Should almost never be overridden. If overridden, it is the subclasses responsibility to
     * ensure that <code>a.equals(b) == true</code> iff <code>cmp(a,b) == 0</code>
     **/
    @Override
    public boolean equals(Object ob_other) {
        if (ob_other == this) {
            return true;
        }
        return (ob_other instanceof PyObject) && _eq((PyObject) ob_other).__nonzero__();
    }

    /**
     * Equivalent to the standard Python __nonzero__ method. Returns whether of not a given
     * <code>PyObject</code> is considered true.
     */
    public boolean __nonzero__() {
        return true;
    }

    /**
     * Equivalent to the Jython __tojava__ method. Tries to coerce this object to an instance of the
     * requested Java class. Returns the special object <code>Py.NoConversion</code> if this
     * <code>PyObject</code> can not be converted to the desired Java class.
     *
     * @param c the Class to convert this <code>PyObject</code> to.
     **/
    public Object __tojava__(Class<?> c) {
        Object proxy = getJavaProxy();
        if ((c == Object.class || c == Serializable.class) && proxy != null) {
            return proxy;
        }
        if (c.isInstance(this)) {
            return this;
        }
        if (c.isPrimitive()) {
            Class<?> tmp = primitiveMap.get(c);
            if (tmp != null) {
                c = tmp;
            }
        }
        if (c.isInstance(proxy)) {
            return proxy;
        }

        // convert faux floats
        // XXX: should also convert faux ints, but that breaks test_java_visibility (ReflectedArgs
        // resolution)
        if (c == Double.class || c == Float.class) {
            try {
                return __float__().asDouble();
            } catch (PyException pye) {
                if (!pye.match(Py.AttributeError)) {
                    throw pye;
                }
            }
        }
        if (c.isArray()) {
            Class<?> component = c.getComponentType();
            try {
                int n = __len__();
                PyArray array = new PyArray(component, n);
                for (int i = 0; i < n; i++) {
                    PyObject o = __getitem__(i);
                    array.set(i, o);
                }
                return array.getArray();
            } catch (Throwable t) {
                // ok
            }
        }

        return Py.NoConversion;
    }

    protected Object getJavaProxy() {
        Object ob = JyAttribute.getAttr(this, JyAttribute.JAVA_PROXY_ATTR);
        if (ob == null) {
            synchronized (this) {
                proxyInit();
                ob = JyAttribute.getAttr(this, JyAttribute.JAVA_PROXY_ATTR);
            }
        }
        return ob;
    }

    /**
     * The basic method to override when implementing a callable object.
     *
     * The first len(args)-len(keywords) members of args[] are plain arguments. The last
     * len(keywords) arguments are the values of the keyword arguments.
     *
     * @param args all arguments to the function (including keyword arguments).
     * @param keywords the keywords used for all keyword arguments.
     **/
    public PyObject __call__(PyObject args[], String keywords[]) {
        throw Py.TypeError(String.format("'%s' object is not callable", getType().fastGetName()));
    }

    public PyObject __call__(ThreadState state, PyObject args[], String keywords[]) {
        return __call__(args, keywords);
    }

    /**
     * A variant of the __call__ method with one extra initial argument. This variant is used to
     * allow method invocations to be performed efficiently.
     *
     * The default behavior is to invoke <code>__call__(args, keywords)</code> with the appropriate
     * arguments. The only reason to override this function would be for improved performance.
     *
     * @param arg1 the first argument to the function.
     * @param args the last arguments to the function (including keyword arguments).
     * @param keywords the keywords used for all keyword arguments.
     **/
    public PyObject __call__(PyObject arg1, PyObject args[], String keywords[]) {
        PyObject[] newArgs = new PyObject[args.length + 1];
        System.arraycopy(args, 0, newArgs, 1, args.length);
        newArgs[0] = arg1;
        return __call__(newArgs, keywords);
    }

    public PyObject __call__(ThreadState state, PyObject arg1, PyObject args[], String keywords[]) {
        return __call__(arg1, args, keywords);
    }

    /**
     * A variant of the __call__ method when no keywords are passed. The default behavior is to
     * invoke <code>__call__(args, keywords)</code> with the appropriate arguments. The only reason
     * to override this function would be for improved performance.
     *
     * @param args all arguments to the function.
     **/
    public PyObject __call__(PyObject args[]) {
        return __call__(args, Py.NoKeywords);
    }

    public PyObject __call__(ThreadState state, PyObject args[]) {
        return __call__(args);
    }

    /**
     * A variant of the __call__ method with no arguments. The default behavior is to invoke
     * <code>__call__(args, keywords)</code> with the appropriate arguments. The only reason to
     * override this function would be for improved performance.
     **/
    public PyObject __call__() {
        return __call__(Py.EmptyObjects, Py.NoKeywords);
    }

    public PyObject __call__(ThreadState state) {
        return __call__();
    }

    /**
     * A variant of the __call__ method with one argument. The default behavior is to invoke
     * <code>__call__(args, keywords)</code> with the appropriate arguments. The only reason to
     * override this function would be for improved performance.
     *
     * @param arg0 the single argument to the function.
     **/
    public PyObject __call__(PyObject arg0) {
        return __call__(new PyObject[] {arg0}, Py.NoKeywords);
    }

    public PyObject __call__(ThreadState state, PyObject arg0) {
        return __call__(arg0);
    }

    /**
     * A variant of the __call__ method with two arguments. The default behavior is to invoke
     * <code>__call__(args, keywords)</code> with the appropriate arguments. The only reason to
     * override this function would be for improved performance.
     *
     * @param arg0 the first argument to the function.
     * @param arg1 the second argument to the function.
     **/
    public PyObject __call__(PyObject arg0, PyObject arg1) {
        return __call__(new PyObject[] {arg0, arg1}, Py.NoKeywords);
    }

    public PyObject __call__(ThreadState state, PyObject arg0, PyObject arg1) {
        return __call__(arg0, arg1);
    }

    /**
     * A variant of the __call__ method with three arguments. The default behavior is to invoke
     * <code>__call__(args, keywords)</code> with the appropriate arguments. The only reason to
     * override this function would be for improved performance.
     *
     * @param arg0 the first argument to the function.
     * @param arg1 the second argument to the function.
     * @param arg2 the third argument to the function.
     **/
    public PyObject __call__(PyObject arg0, PyObject arg1, PyObject arg2) {
        return __call__(new PyObject[] {arg0, arg1, arg2}, Py.NoKeywords);
    }

    public PyObject __call__(ThreadState state, PyObject arg0, PyObject arg1, PyObject arg2) {
        return __call__(arg0, arg1, arg2);
    }

    /**
     * A variant of the __call__ method with four arguments. The default behavior is to invoke
     * <code>__call__(args, keywords)</code> with the appropriate arguments. The only reason to
     * override this function would be for improved performance.
     *
     * @param arg0 the first argument to the function.
     * @param arg1 the second argument to the function.
     * @param arg2 the third argument to the function.
     * @param arg3 the fourth argument to the function.
     **/
    public PyObject __call__(PyObject arg0, PyObject arg1, PyObject arg2, PyObject arg3) {
        return __call__(new PyObject[] {arg0, arg1, arg2, arg3}, Py.NoKeywords);
    }

    public PyObject __call__(ThreadState state, PyObject arg0, PyObject arg1, PyObject arg2,
            PyObject arg3) {
        return __call__(arg0, arg1, arg2, arg3);
    }

    public PyObject _callextra(PyObject[] args, String[] keywords, PyObject starargs,
            PyObject kwargs) {

        int argslen = args.length;

        String name;
        if (this instanceof PyFunction) {
            name = ((PyFunction) this).__name__ + "() ";
        } else if (this instanceof PyBuiltinCallable) {
            name = ((PyBuiltinCallable) this).fastGetName().toString() + "() ";
        } else {
            name = getType().fastGetName() + " ";
        }
        if (kwargs != null) {
            PyObject keys = kwargs.__findattr__("keys");
            if (keys == null) {
                throw Py.TypeError(name + "argument after ** must be a mapping");
            }
            for (String keyword : keywords) {
                if (kwargs.__finditem__(keyword) != null) {
                    throw Py.TypeError(
                            name + "got multiple values for keyword argument '" + keyword + "'");
                }
            }
            argslen += kwargs.__len__();
        }
        List<PyObject> starObjs = null;
        if (starargs != null) {
            starObjs = new ArrayList<PyObject>();
            PyObject iter = Py.iter(starargs, name + "argument after * must be a sequence");
            for (PyObject cur = null; ((cur = iter.__iternext__()) != null);) {
                starObjs.add(cur);
            }
            argslen += starObjs.size();
        }
        PyObject[] newargs = new PyObject[argslen];
        int argidx = args.length - keywords.length;
        System.arraycopy(args, 0, newargs, 0, argidx);
        if (starObjs != null) {
            Iterator<PyObject> it = starObjs.iterator();
            while (it.hasNext()) {
                newargs[argidx++] = it.next();
            }
        }
        System.arraycopy(args, args.length - keywords.length, newargs, argidx, keywords.length);
        argidx += keywords.length;

        if (kwargs != null) {
            String[] newkeywords = new String[keywords.length + kwargs.__len__()];
            System.arraycopy(keywords, 0, newkeywords, 0, keywords.length);

            PyObject keys = kwargs.invoke("keys");
            PyObject key;
            for (int i = 0; (key = keys.__finditem__(i)) != null; i++) {
                if (!(key instanceof PyString)) {
                    throw Py.TypeError(name + "keywords must be strings");
                }
                newkeywords[keywords.length + i] = ((PyString) key).internedString();
                newargs[argidx++] = kwargs.__finditem__(key);
            }
            keywords = newkeywords;
        }

        if (newargs.length != argidx) {
            args = new PyObject[argidx];
            System.arraycopy(newargs, 0, args, 0, argidx);
        } else {
            args = newargs;
        }
        return __call__(args, keywords);
    }

    public boolean isCallable() {
        return getType().lookup("__call__") != null;
    }

    public boolean isNumberType() {
        PyType type = getType();
        return type.lookup("__int__") != null || type.lookup("__float__") != null;
    }

    public boolean isMappingType() {
        PyType type = getType();
        return type.lookup("__getitem__") != null
                && !(isSequenceType() && type.lookup("__getslice__") != null);
    }

    public boolean isSequenceType() {
        return getType().lookup("__getitem__") != null;
    }

    /**
     * Determine if this object can act as an int (implements __int__).
     *
     * @return true if the object can act as an int
     */
    public boolean isInteger() {
        return getType().lookup("__int__") != null;
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
     * Equivalent to the standard Python __len__ method. Part of the mapping discipline.
     *
     * @return the length of the object
     **/
    public int __len__() {
        throw Py.TypeError(
                String.format("object of type '%.200s' has no len()", getType().fastGetName()));
    }

    /**
     * Very similar to the standard Python __getitem__ method. Instead of throwing a KeyError if the
     * item isn't found, this just returns null.
     *
     * Classes that wish to implement __getitem__ should override this method instead (with the
     * appropriate semantics.
     *
     * @param key the key to lookup in this container
     *
     * @return the value corresponding to key or null if key is not found
     **/
    public PyObject __finditem__(PyObject key) {
        throw Py.TypeError(
                String.format("'%.200s' object is unsubscriptable", getType().fastGetName()));
    }

    /**
     * A variant of the __finditem__ method which accepts a primitive <code>int</code> as the key.
     * By default, this method will call <code>__finditem__(PyObject key)</code> with the
     * appropriate args. The only reason to override this method is for performance.
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
     * A variant of the __finditem__ method which accepts a Java <code>String</code> as the key. By
     * default, this method will call <code>__finditem__(PyObject key)</code> with the appropriate
     * args. The only reason to override this method is for performance.
     *
     * <b>Warning: key must be an interned string!!!!!!!!</b>
     *
     * @param key the key to lookup in this sequence - <b> must be an interned string </b>.
     * @return the value corresponding to key or null if key is not found.
     *
     * @see #__finditem__(PyObject)
     **/
    public PyObject __finditem__(String key) {
        return __finditem__(new PyString(key));
    }

    /**
     * Equivalent to the standard Python __getitem__ method. This variant takes a primitive
     * <code>int</code> as the key. This method should not be overridden. Override the
     * <code>__finditem__</code> method instead.
     *
     * @param key the key to lookup in this container.
     * @return the value corresponding to that key.
     * @throws PyException {@code KeyError} if the key is not found.
     *
     * @see #__finditem__(int)
     **/
    public PyObject __getitem__(int key) {
        PyObject ret = __finditem__(key);
        if (ret == null) {
            throw Py.KeyError("" + key);
        }
        return ret;
    }

    /**
     * Equivalent to the standard Python __getitem__ method. This method should not be overridden.
     * Override the <code>__finditem__</code> method instead.
     *
     * @param key the key to lookup in this container.
     * @return the value corresponding to that key.
     * @throws PyException {@code KeyError} if the key is not found.
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
     * A variant of the __setitem__ method which accepts a String as the key. <b>This String must be
     * interned</b>. By default, this will call
     * <code>__setitem__(PyObject key, PyObject value)</code> with the appropriate args. The only
     * reason to override this method is for performance.
     *
     * @param key the key whose value will be set - <b> must be an interned string </b>.
     * @param value the value to set this key to
     *
     * @see #__setitem__(PyObject, PyObject)
     **/
    public void __setitem__(String key, PyObject value) {
        __setitem__(new PyString(key), value);
    }

    /**
     * A variant of the __setitem__ method which accepts a primitive <code>int</code> as the key. By
     * default, this will call <code>__setitem__(PyObject key, PyObject value)</code> with the
     * appropriate args. The only reason to override this method is for performance.
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
     * @throws PyException {@code KeyError} if the key is not found in the container
     **/
    public void __delitem__(PyObject key) {
        throw Py.TypeError(String.format("'%.200s' object doesn't support item deletion",
                getType().fastGetName()));
    }

    /**
     * A variant of the __delitem__ method which accepts a String as the key. <b>This String must be
     * interned</b>. By default, this will call <code>__delitem__(PyObject key)</code> with the
     * appropriate args. The only reason to override this method is for performance.
     *
     * @param key the key who will be removed - <b> must be an interned string </b>.
     * @throws PyException {@code KeyError} if the key is not found in the container
     *
     * @see #__delitem__(PyObject)
     **/
    public void __delitem__(String key) {
        __delitem__(new PyString(key));
    }

    public PyObject __getslice__(PyObject s_start, PyObject s_stop, PyObject s_step) {
        PySlice s = new PySlice(s_start, s_stop, s_step);
        return __getitem__(s);
    }

    public void __setslice__(PyObject s_start, PyObject s_stop, PyObject s_step, PyObject value) {
        PySlice s = new PySlice(s_start, s_stop, s_step);
        __setitem__(s, value);
    }

    public void __delslice__(PyObject s_start, PyObject s_stop, PyObject s_step) {
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

    /* The basic functions to implement an iterator */

    /**
     * Return an iterator that is used to iterate the element of this sequence. From version 2.2,
     * this method is the primary protocol for looping over sequences.
     * <p>
     * If a PyObject subclass should support iteration based in the __finditem__() method, it must
     * supply an implementation of __iter__() like this: <pre>
     * public PyObject __iter__() {
     *     return new PySequenceIter(this);
     * }
     * </pre> When iterating over a python sequence from java code, it should be done with code like
     * this: <pre>
     * for (PyObject item : seq.asIterable()) {
     *     // Do something with item
     * }
     * </pre>
     *
     * @since 2.2
     */
    public PyObject __iter__() {
        throw Py.TypeError(
                String.format("'%.200s' object is not iterable", getType().fastGetName()));
    }

    /**
     * Returns an Iterable over the Python iterator returned by __iter__ on this object. If this
     * object doesn't support __iter__, a TypeException will be raised when iterator is called on
     * the returned Iterable.
     */
    public Iterable<PyObject> asIterable() {
        return new Iterable<PyObject>() {

            @Override
            public Iterator<PyObject> iterator() {
                return new WrappedIterIterator<PyObject>(__iter__()) {

                    @Override
                    public PyObject next() {
                        return getNext();
                    }
                };
            }
        };
    }

    /**
     * Return the next element of the sequence that this is an iterator for. Returns null when the
     * end of the sequence is reached.
     *
     * @since 2.2
     */
    public PyObject __iternext__() {
        return null;
    }

    /* The basic functions to implement a namespace */

    /**
     * Very similar to the standard Python __getattr__ method. Instead of throwing a AttributeError
     * if the item isn't found, this just returns null.
     *
     * By default, this method will call <code>__findattr__(name.internedString)</code> with the
     * appropriate args.
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
     * A variant of the __findattr__ method which accepts a Java <code>String</code> as the name.
     *
     * <b>Warning: name must be an interned string!</b>
     *
     * @param name the name to lookup in this namespace <b>must be an interned string</b>.
     * @return the value corresponding to name or null if name is not found
     **/
    public final PyObject __findattr__(String name) {
        try {
            return __findattr_ex__(name);
        } catch (PyException exc) {
            if (exc.match(Py.AttributeError)) {
                return null;
            }
            throw exc;
        }
    }

    /**
     * Attribute lookup hook. If the attribute is not found, null may be returned or a
     * Py.AttributeError can be thrown, whatever is more correct, efficient and/or convenient for
     * the implementing class.
     *
     * Client code should use {@link #__getattr__(String)} or {@link #__findattr__(String)}. Both
     * methods have a clear policy for failed lookups.
     *
     * @return The looked up value. May return null if the attribute is not found
     * @throws PyException {@code AttributeError} if the attribute is not found. This is not
     *             mandatory, null can be returned if it fits the implementation better, or for
     *             performance reasons.
     */
    public PyObject __findattr_ex__(String name) {
        return object___findattr__(name);
    }

    /**
     * Equivalent to the standard Python __getattr__ method.
     *
     * By default, this method will call <code>__getattr__(name.internedString)</code> with the
     * appropriate args.
     *
     * @param name the name to lookup in this namespace
     * @return the value corresponding to name
     * @throws PyException {@code AttributeError} if the name is not found.
     *
     * @see #__findattr_ex__(String)
     **/
    public final PyObject __getattr__(PyString name) {
        return __getattr__(name.internedString());
    }

    /**
     * A variant of the __getattr__ method which accepts a Java <code>String</code> as the name.
     * This method can not be overridden. Override the <code>__findattr_ex__</code> method instead.
     *
     * <b>Warning: name must be an interned string!!!!!!!!</b>
     *
     * @param name the name to lookup in this namespace <b> must be an interned string </b>.
     * @return the value corresponding to name
     * @throws PyException {@code AttributeError} if the name is not found.
     *
     * @see #__findattr__(java.lang.String)
     **/
    public final PyObject __getattr__(String name) {
        PyObject ret = __findattr_ex__(name);
        if (ret == null) {
            noAttributeError(name);
        }
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
     * Equivalent to the standard Python __setattr__ method. This method can not be overridden.
     *
     * @param name the name to lookup in this namespace
     * @throws PyException {@code AttributeError} if the name is not found.
     *
     * @see #__setattr__(java.lang.String, PyObject)
     **/
    public final void __setattr__(PyString name, PyObject value) {
        __setattr__(name.internedString(), value);
    }

    /**
     * A variant of the __setattr__ method which accepts a String as the key. <b>This String must be
     * interned</b>.
     *
     * @param name the name whose value will be set - <b> must be an interned string </b>.
     * @param value the value to set this name to
     *
     * @see #__setattr__(PyString, PyObject)
     **/
    public void __setattr__(String name, PyObject value) {
        object___setattr__(name, value);
    }

    /**
     * Equivalent to the standard Python __delattr__ method. This method can not be overridden.
     *
     * @param name the name to which will be removed
     * @throws PyException {@code AttributeError} if the name doesn't exist
     *
     * @see #__delattr__(java.lang.String)
     **/
    public final void __delattr__(PyString name) {
        __delattr__(name.internedString());
    }

    /**
     * A variant of the __delattr__ method which accepts a String as the key. <b>This String must be
     * interned</b>. By default, this will call <code>__delattr__(PyString name)</code> with the
     * appropriate args. The only reason to override this method is for performance.
     *
     * @param name the name which will be removed - <b> must be an interned string </b>.
     * @throws PyException {@code AttributeError} if the name doesn't exist
     *
     * @see #__delattr__(PyString)
     **/
    public void __delattr__(String name) {
        object___delattr__(name);
    }

    /**
     * This is a hook called during the import mechanism when the target module is (or may be) a
     * sub-module of this object.
     *
     * @param name relative to this object <b>must be an interned string</b>.
     * @return corresponding value (a module or package) or {@code null} if not found
     */
    protected PyObject impAttr(String name) {
        return __findattr__(name);
    }

    protected void mergeListAttr(PyDictionary accum, String attr) {
        PyObject obj = __findattr__(attr);
        if (obj == null) {
            return;
        }
        if (obj instanceof PyList) {
            for (PyObject name : obj.asIterable()) {
                accum.__setitem__(name, Py.None);
            }
        }
    }

    protected void mergeDictAttr(PyDictionary accum, String attr) {
        PyObject obj = __findattr__(attr);
        if (obj == null) {
            return;
        }
        if (obj instanceof AbstractDict || obj instanceof PyDictProxy) {
            accum.update(obj);
        }
    }

    protected void mergeClassDict(PyDictionary accum, PyObject aClass) {
        // Merge in the type's dict (if any)
        aClass.mergeDictAttr(accum, "__dict__");

        // Recursively merge in the base types' (if any) dicts
        PyObject bases = aClass.__findattr__("__bases__");
        if (bases == null) {
            return;
        }
        // We have no guarantee that bases is a real tuple
        int len = bases.__len__();
        for (int i = 0; i < len; i++) {
            mergeClassDict(accum, bases.__getitem__(i));
        }
    }

    protected void __rawdir__(PyDictionary accum) {
        mergeDictAttr(accum, "__dict__");
        mergeListAttr(accum, "__methods__");
        mergeListAttr(accum, "__members__");
        // Class dict is a slower, more manual merge to match CPython
        PyObject itsClass = __findattr__("__class__");
        if (itsClass != null) {
            mergeClassDict(accum, itsClass);
        }
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

    boolean jdontdel() {
        return false;
    }

    /* Numeric coercion */

    /**
     * Implements numeric coercion
     *
     * @param o the other object involved in the coercion
     * @return null if coercion is not implemented Py.None if coercion was not possible a single
     *         PyObject to use to replace o if this is unchanged; or a PyObject[2] consisting of
     *         replacements for this and o.
     **/
    public Object __coerce_ex__(PyObject o) {
        return null;
    }

    /**
     * Implements coerce(this,other), result as PyObject[]
     *
     * @param other
     * @return PyObject[]
     */
    PyObject[] _coerce(PyObject other) {
        Object result;
        if (this.getType() == other.getType() && !(this instanceof PyInstance)) {
            return new PyObject[] {this, other};
        }
        result = this.__coerce_ex__(other);
        if (result != null && result != Py.None) {
            if (result instanceof PyObject[]) {
                return (PyObject[]) result;
            } else {
                return new PyObject[] {this, (PyObject) result};
            }
        }
        result = other.__coerce_ex__(this);
        if (result != null && result != Py.None) {
            if (result instanceof PyObject[]) {
                return (PyObject[]) result;
            } else {
                return new PyObject[] {(PyObject) result, other};
            }
        }
        return null;

    }

    /**
     * Equivalent to the standard Python __coerce__ method.
     *
     * This method can not be overridden. To implement __coerce__ functionality, override
     * __coerce_ex__ instead.
     *
     * Also, <b>do not</b> call this method from exposed 'coerce' methods. Instead, Use
     * adaptToCoerceTuple over the result of the overridden __coerce_ex__.
     *
     * @param pyo the other object involved in the coercion.
     * @return a tuple of this object and pyo coerced to the same type or Py.NotImplemented if no
     *         coercion is possible.
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
     * Adapts the result of __coerce_ex__ to a tuple of two elements, with the resulting coerced
     * values, or to Py.NotImplemented, if o is Py.None.
     *
     * This is safe to be used from subclasses exposing '__coerce__' (as opposed to
     * {@link #__coerce__(PyObject)}, which calls the virtual method
     * {@link #__coerce_ex__(PyObject)})
     *
     * @param o either a PyObject[2] or a PyObject, as given by {@link #__coerce_ex__(PyObject)}.
     */
    protected final PyObject adaptToCoerceTuple(Object o) {
        if (o == Py.None) {
            return Py.NotImplemented;
        }
        if (o instanceof PyObject[]) {
            return new PyTuple((PyObject[]) o);
        } else {
            return new PyTuple(this, (PyObject) o);
        }
    }

    /* The basic comparision operations */

    /**
     * Equivalent to the standard Python __cmp__ method.
     *
     * @param other the object to compare this with.
     * @return -1 if {@code this<o}; 0 if {@code this==o}; +1 if {@code this>o}; -2 if no comparison
     *         is implemented
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
     * @return -1 if {@code this<0}; 0 if {@code this==o}; +1 if {@code this>o}
     **/
    public final int _cmp(PyObject o) {
        if (this == o) {
            return 0;
        }

        PyObject token = null;
        ThreadState ts = Py.getThreadState();
        try {
            if (++ts.compareStateNesting > 500) {
                if ((token = check_recursion(ts, this, o)) == null) {
                    return 0;
                }
            }

            PyObject result;
            result = __eq__(o);
            if (result == null) {
                result = o.__eq__(this);
            }
            if (result != null && result.__nonzero__()) {
                return 0;
            }

            result = __lt__(o);
            if (result == null) {
                result = o.__gt__(this);
            }
            if (result != null && result.__nonzero__()) {
                return -1;
            }

            result = __gt__(o);
            if (result == null) {
                result = o.__lt__(this);
            }
            if (result != null && result.__nonzero__()) {
                return 1;
            }

            return _cmp_unsafe(o);
        } finally {
            delete_token(ts, token);
            ts.compareStateNesting--;
        }
    }

    private PyObject make_pair(PyObject o) {
        if (System.identityHashCode(this) < System.identityHashCode(o)) {
            return new PyIdentityTuple(new PyObject[] {this, o});
        } else {
            return new PyIdentityTuple(new PyObject[] {o, this});
        }
    }

    private final int _default_cmp(PyObject other) {
        int result;
        if (_is(other).__nonzero__()) {
            return 0;
        }

        /* None is smaller than anything */
        if (this == Py.None) {
            return -1;
        }
        if (other == Py.None) {
            return 1;
        }

        // No rational way to compare these, so ask their classes to compare
        PyType type = getType();
        PyType otherType = other.getType();
        if (type == otherType) {
            return Py.id(this) < Py.id(other) ? -1 : 1;
        }

        // different type: compare type names; numbers are smaller
        String typeName = isNumberType() ? "" : type.fastGetName();
        String otherTypeName = other.isNumberType() ? "" : otherType.fastGetName();
        result = typeName.compareTo(otherTypeName);
        if (result == 0) {
            // Same type name, or (more likely) incomparable numeric types
            return Py.id(type) < Py.id(otherType) ? -1 : 1;
        }
        return result < 0 ? -1 : 1;
    }

    private final int _cmp_unsafe(PyObject other) {
        int result = _try__cmp__(other);
        if (result != -2) {
            return result;
        }
        return this._default_cmp(other);
    }

    /*
     * Like _cmp_unsafe but limited to ==/!= as 0/!=0, thus it avoids to invoke _default_cmp.
     */
    private final int _cmpeq_unsafe(PyObject other) {
        int result = _try__cmp__(other);
        if (result != -2) {
            return result;
        }

        return this._is(other).__nonzero__() ? 0 : 1;
    }

    /**
     * Tries a 3-way comparison, using __cmp__. It tries the following operations until one of them
     * succeed:
     * <ul>
     * <li>this.__cmp__(other)
     * <li>other.__cmp__(this)
     * <li>this._coerce(other) followed by coerced_this.__cmp__(coerced_other)
     * </ul>
     *
     * @return -1, 0, -1 or -2, according to the {@link #__cmp__} protocol.
     */
    private int _try__cmp__(PyObject other) {
        int result;
        result = this.__cmp__(other);
        if (result != -2) {
            return result;
        }

        if (!(this instanceof PyInstance)) {
            result = other.__cmp__(this);
            if (result != -2) {
                return -result;
            }
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

    private final static PyObject check_recursion(ThreadState ts, PyObject o1, PyObject o2) {
        PyDictionary stateDict = ts.getCompareStateDict();

        PyObject pair = o1.make_pair(o2);

        if (stateDict.__finditem__(pair) != null) {
            return null;
        }

        stateDict.__setitem__(pair, pair);
        return pair;
    }

    private final static void delete_token(ThreadState ts, PyObject token) {
        if (token == null) {
            return;
        }
        PyDictionary stateDict = ts.getCompareStateDict();

        stateDict.__delitem__(token);
    }

    /**
     * Implements the Python expression {@code this == other}.
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
                if ((token = check_recursion(ts, this, o)) == null) {
                    return Py.True;
                }
            }
            PyObject res = __eq__(o);
            if (res != null) {
                return res;
            }
            res = o.__eq__(this);
            if (res != null) {
                return res;
            }
            return _cmpeq_unsafe(o) == 0 ? Py.True : Py.False;
        } finally {
            delete_token(ts, token);
            ts.compareStateNesting--;
        }
    }

    /**
     * Implements the Python expression {@code this != other}.
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
                if ((token = check_recursion(ts, this, o)) == null) {
                    return Py.False;
                }
            }
            PyObject res = __ne__(o);
            if (res != null) {
                return res;
            }
            res = o.__ne__(this);
            if (res != null) {
                return res;
            }
            return _cmpeq_unsafe(o) != 0 ? Py.True : Py.False;
        } finally {
            delete_token(ts, token);
            ts.compareStateNesting--;
        }
    }

    /**
     * Implements the Python expression {@code this <= other}.
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
                if ((token = check_recursion(ts, this, o)) == null) {
                    throw Py.ValueError("can't order recursive values");
                }
            }
            PyObject res = __le__(o);
            if (res != null) {
                return res;
            }
            res = o.__ge__(this);
            if (res != null) {
                return res;
            }
            return _cmp_unsafe(o) <= 0 ? Py.True : Py.False;
        } finally {
            delete_token(ts, token);
            ts.compareStateNesting--;
        }
    }

    /**
     * Implements the Python expression {@code this < other}.
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
                if ((token = check_recursion(ts, this, o)) == null) {
                    throw Py.ValueError("can't order recursive values");
                }
            }
            PyObject res = __lt__(o);
            if (res != null) {
                return res;
            }
            res = o.__gt__(this);
            if (res != null) {
                return res;
            }
            return _cmp_unsafe(o) < 0 ? Py.True : Py.False;
        } finally {
            delete_token(ts, token);
            ts.compareStateNesting--;
        }
    }

    /**
     * Implements the Python expression {@code this >= other}.
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
                if ((token = check_recursion(ts, this, o)) == null) {
                    throw Py.ValueError("can't order recursive values");
                }
            }
            PyObject res = __ge__(o);
            if (res != null) {
                return res;
            }
            res = o.__le__(this);
            if (res != null) {
                return res;
            }
            return _cmp_unsafe(o) >= 0 ? Py.True : Py.False;
        } finally {
            delete_token(ts, token);
            ts.compareStateNesting--;
        }
    }

    /**
     * Implements the Python expression {@code this > other}.
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
                if ((token = check_recursion(ts, this, o)) == null) {
                    throw Py.ValueError("can't order recursive values");
                }
            }
            PyObject res = __gt__(o);
            if (res != null) {
                return res;
            }
            res = o.__lt__(this);
            if (res != null) {
                return res;
            }
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
        // Access javaProxy directly here as is is for object identity, and at best getJavaProxy
        // will initialize a new object with a different identity
        return this == o || (JyAttribute.hasAttr(this, JyAttribute.JAVA_PROXY_ATTR)
                && JyAttribute.getAttr(this, JyAttribute.JAVA_PROXY_ATTR) == JyAttribute.getAttr(o,
                        JyAttribute.JAVA_PROXY_ATTR)) ? Py.True : Py.False;
    }

    /**
     * Implements <code>is not</code> operator.
     *
     * @param o the object to compare this with.
     * @return the result of the comparison
     **/
    public PyObject _isnot(PyObject o) {
        // Access javaProxy directly here as is is for object identity, and at best getJavaProxy
        // will initialize a new object with a different identity
        return this != o && (!JyAttribute.hasAttr(this, JyAttribute.JAVA_PROXY_ATTR)
                || JyAttribute.getAttr(this, JyAttribute.JAVA_PROXY_ATTR) != JyAttribute.getAttr(o,
                        JyAttribute.JAVA_PROXY_ATTR)) ? Py.True : Py.False;
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
            if (o.equals(item)) {
                return true;
            }
        }
        return false;
    }

    public PyObject __format__(PyObject formatSpec) {
        return object___format__(formatSpec);
    }

    @ExposedMethod(doc = BuiltinDocs.object___format___doc)
    final PyObject object___format__(PyObject formatSpec) {
        if (formatSpec != null && formatSpec instanceof PyString
                && !((PyString) formatSpec).getString().isEmpty()) {
            Py.warning(Py.PendingDeprecationWarning,
                    "object.__format__ with a non-empty format string is deprecated");
        }
        return __str__().__format__(formatSpec);
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
     * Equivalent to the standard Python __hex__ method Should only be overridden by numeric objects
     * that can be reasonably represented as a hexadecimal string.
     *
     * @return a string representing this object as a hexadecimal number.
     **/
    public PyString __hex__() {
        throw Py.TypeError("hex() argument can't be converted to hex");
    }

    /**
     * Equivalent to the standard Python __oct__ method. Should only be overridden by numeric
     * objects that can be reasonably represented as an octal string.
     *
     * @return a string representing this object as an octal number.
     **/
    public PyString __oct__() {
        throw Py.TypeError("oct() argument can't be converted to oct");
    }

    /**
     * Equivalent to the standard Python __int__ method. Should only be overridden by numeric
     * objects that can be reasonably coerced into an integer.
     *
     * @return an integer corresponding to the value of this object.
     **/
    public PyObject __int__() {
        throw Py.AttributeError("__int__");
    }

    /**
     * Equivalent to the standard Python __long__ method. Should only be overridden by numeric
     * objects that can be reasonably coerced into a python long.
     *
     * @return a PyLong or PyInteger corresponding to the value of this object.
     **/
    public PyObject __long__() {
        throw Py.AttributeError("__long__");
    }

    /**
     * Equivalent to the standard Python __float__ method. Should only be overridden by numeric
     * objects that can be reasonably coerced into a python float.
     *
     * @return a float corresponding to the value of this object.
     **/
    public PyFloat __float__() {
        throw Py.AttributeError("__float__");
    }

    /**
     * Equivalent to the standard Python __complex__ method. Should only be overridden by numeric
     * objects that can be reasonably coerced into a python complex number.
     *
     * @return a complex number corresponding to the value of this object.
     **/
    public PyComplex __complex__() {
        throw Py.AttributeError("__complex__");
    }

    /**
     * Equivalent to the standard Python __trunc__ method. Should only be overridden by numeric
     * objects that can reasonably be truncated to an Integral.
     *
     * @return the Integral closest to x between 0 and x.
     **/
    public PyObject __trunc__() {
        throw Py.AttributeError("__trunc__");
    }

    /**
     * Equivalent to the standard Python conjugate method. Should only be overridden by numeric
     * objects that can calculate a complex conjugate.
     *
     * @return the complex conjugate.
     **/
    public PyObject conjugate() {
        throw Py.AttributeError("conjugate");
    }

    /**
     * Equivalent to the standard Python bit_length method. Should only be overridden by numeric
     * objects that can calculate a bit_length.
     *
     * @return the bit_length of this object.
     **/
    public int bit_length() {
        throw Py.AttributeError("bit_length");
    }

    /**
     * Equivalent to the standard Python __pos__ method.
     *
     * @return +this.
     **/
    public PyObject __pos__() {
        throw Py.TypeError(
                String.format("bad operand type for unary +: '%.200s'", getType().fastGetName()));
    }

    /**
     * Equivalent to the standard Python __neg__ method.
     *
     * @return -this.
     **/
    public PyObject __neg__() {
        throw Py.TypeError(
                String.format("bad operand type for unary -: '%.200s'", getType().fastGetName()));
    }

    /**
     * Equivalent to the standard Python __abs__ method.
     *
     * @return abs(this).
     **/
    public PyObject __abs__() {
        throw Py.TypeError(
                String.format("bad operand type for abs(): '%.200s'", getType().fastGetName()));
    }

    /**
     * Equivalent to the standard Python __invert__ method.
     *
     * @return ~this.
     **/
    public PyObject __invert__() {
        throw Py.TypeError(
                String.format("bad operand type for unary ~: '%.200s'", getType().fastGetName()));
    }

    /**
     * Equivalent to the standard Python __index__ method.
     *
     * @return a PyInteger or PyLong
     * @throws PyException {@code TypeError} if not supported
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
     * {0} is the op name. {1} is the left operand type. {2} is the right operand type.
     */
    protected String unsupportedopMessage(String op, PyObject o2) {
        return null;
    }

    /**
     * Should return an error message suitable for substitution where.
     *
     * {0} is the op name. {1} is the left operand type. {2} is the right operand type.
     */
    protected String runsupportedopMessage(String op, PyObject o2) {
        return null;
    }

    /**
     * Implements the three argument power function.
     *
     * @param o2 the power to raise this number to.
     * @param o3 the modulus to perform this operation in or null if no modulo is to be used
     * @return this object raised to the given power in the given modulus
     **/
    public PyObject __pow__(PyObject o2, PyObject o3) {
        return null;
    }

    /**
     * Determine if the binary op on types t1 and t2 is an add operation dealing with a str/unicode
     * and a str/unicode subclass.
     *
     * This operation is special cased in _binop_rule to match CPython's handling; CPython uses
     * tp_as_number and tp_as_sequence to allow string/unicode subclasses to override the left
     * side's __add__ when that left side is an actual str or unicode object (see test_concat_jy for
     * examples).
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
        return (op == "+") && (t1 == PyString.TYPE || t1 == PyUnicode.TYPE)
                && (t2.isSubType(PyString.TYPE) || t2.isSubType(PyUnicode.TYPE));
    }

    private PyObject _binop_rule(PyType t1, PyObject o2, PyType t2, String left, String right,
            String op) {
        /*
         * this is the general rule for binary operation dispatching try first __xxx__ with this and
         * then __rxxx__ with o2 unless o2 is an instance of subclass of the type of this, and
         * further __xxx__ and __rxxx__ are unrelated ( checked here by looking at where in the
         * hierarchy they are defined), in that case try them in the reverse order. This is the same
         * formulation as used by PyPy, see also test_descr.subclass_right_op.
         */
        PyObject o1 = this;
        PyObject[] where = new PyObject[1];
        PyObject where1 = null, where2 = null;
        PyObject impl1 = t1.lookup_where(left, where);
        where1 = where[0];
        PyObject impl2 = t2.lookup_where(right, where);
        where2 = where[0];
        if (impl2 != null && impl1 != null && where1 != where2
                && (t2.isSubType(t1) && !Py.isSubClass(where1, where2) && !Py.isSubClass(t1, where2)
                        || isStrUnicodeSpecialCase(t1, t2, op))) {
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
     * Equivalent to the standard Python __add__ method.
     *
     * @param other the object to perform this binary operation with (the right-hand operand).
     * @return the result of the add, or null if this operation is not defined.
     **/
    public PyObject __add__(PyObject other) {
        return null;
    }

    /**
     * Equivalent to the standard Python __radd__ method.
     *
     * @param other the object to perform this binary operation with (the left-hand operand).
     * @return the result of the add, or null if this operation is not defined.
     **/
    public PyObject __radd__(PyObject other) {
        return null;
    }

    /**
     * Equivalent to the standard Python __iadd__ method.
     *
     * @param other the object to perform this binary operation with (the right-hand operand).
     * @return the result of the iadd, or null if this operation is not defined
     **/
    public PyObject __iadd__(PyObject other) {
        return null;
    }

    /**
     * Implements the Python expression {@code this + o2}.
     *
     * @param o2 the object to perform this binary operation with.
     * @return the result of the add.
     * @throws PyException {@code TypeError} if this operation can't be performed with these
     *             operands.
     **/
    public final PyObject _add(PyObject o2) {
        PyType t1 = this.getType();
        PyType t2 = o2.getType();
        if (t1 == t2 || t1.builtin && t2.builtin) {
            return this._basic_add(o2);
        }
        return _binop_rule(t1, o2, t2, "__add__", "__radd__", "+");
    }

    /**
     * Implements the Python expression {@code this + o2} when this and o2 have the same type or are
     * builtin types.
     *
     * @param o2 the object to perform this binary operation with.
     * @return the result of the add.
     * @throws PyException {@code TypeError} if this operation can't be performed with these
     *             operands.
     **/
    final PyObject _basic_add(PyObject o2) {
        PyObject x = __add__(o2);
        if (x != null) {
            return x;
        }
        x = o2.__radd__(this);
        if (x != null) {
            return x;
        }
        throw Py.TypeError(_unsupportedop("+", o2));
    }

    /**
     * Implements the Python expression {@code this += o2}.
     *
     * @param o2 the object to perform this inplace binary operation with.
     * @return the result of the iadd.
     * @throws PyException {@code TypeError} if this operation can't be performed with these
     *             operands.
     **/
    public final PyObject _iadd(PyObject o2) {
        PyType t1 = this.getType();
        PyType t2 = o2.getType();
        if (t1 == t2 || t1.builtin && t2.builtin) {
            return this._basic_iadd(o2);
        }
        PyObject impl = t1.lookup("__iadd__");
        if (impl != null) {
            PyObject res = impl.__get__(this, t1).__call__(o2);
            if (res != Py.NotImplemented) {
                return res;
            }
        }
        return _binop_rule(t1, o2, t2, "__add__", "__radd__", "+");
    }

    /**
     * Implements the Python expression {@code this += o2} when this and o2 have the same type or
     * are builtin types.
     *
     * @param o2 the object to perform this inplace binary operation with.
     * @return the result of the iadd.
     * @throws PyException {@code TypeError} if this operation can't be performed with these
     *             operands.
     **/
    final PyObject _basic_iadd(PyObject o2) {
        PyObject x = __iadd__(o2);
        if (x != null) {
            return x;
        }
        return this._basic_add(o2);
    }

    /**
     * Equivalent to the standard Python __sub__ method
     *
     * @param other the object to perform this binary operation with (the right-hand operand).
     * @return the result of the sub, or null if this operation is not defined
     **/
    public PyObject __sub__(PyObject other) {
        return null;
    }

    /**
     * Equivalent to the standard Python __rsub__ method
     *
     * @param other the object to perform this binary operation with (the left-hand operand).
     * @return the result of the sub, or null if this operation is not defined.
     **/
    public PyObject __rsub__(PyObject other) {
        return null;
    }

    /**
     * Equivalent to the standard Python __isub__ method
     *
     * @param other the object to perform this binary operation with (the right-hand operand).
     * @return the result of the isub, or null if this operation is not defined
     **/
    public PyObject __isub__(PyObject other) {
        return null;
    }

    /**
     * Implements the Python expression {@code this - o2}
     *
     * @param o2 the object to perform this binary operation with.
     * @return the result of the sub.
     * @throws PyException {@code TypeError} if this operation can't be performed with these
     *             operands.
     **/
    public final PyObject _sub(PyObject o2) {
        PyType t1 = this.getType();
        PyType t2 = o2.getType();
        if (t1 == t2 || t1.builtin && t2.builtin) {
            return this._basic_sub(o2);
        }
        return _binop_rule(t1, o2, t2, "__sub__", "__rsub__", "-");
    }

    /**
     * Implements the Python expression {@code this - o2} when this and o2 have the same type or are
     * builtin types.
     *
     * @param o2 the object to perform this binary operation with.
     * @return the result of the sub.
     * @throws PyException {@code TypeError} if this operation can't be performed with these
     *             operands.
     **/
    final PyObject _basic_sub(PyObject o2) {
        PyObject x = __sub__(o2);
        if (x != null) {
            return x;
        }
        x = o2.__rsub__(this);
        if (x != null) {
            return x;
        }
        throw Py.TypeError(_unsupportedop("-", o2));
    }

    /**
     * Implements the Python expression {@code this -= o2}
     *
     * @param o2 the object to perform this inplace binary operation with.
     * @return the result of the isub.
     * @throws PyException {@code TypeError} if this operation can't be performed with these
     *             operands.
     **/
    public final PyObject _isub(PyObject o2) {
        PyType t1 = this.getType();
        PyType t2 = o2.getType();
        if (t1 == t2 || t1.builtin && t2.builtin) {
            return this._basic_isub(o2);
        }
        PyObject impl = t1.lookup("__isub__");
        if (impl != null) {
            PyObject res = impl.__get__(this, t1).__call__(o2);
            if (res != Py.NotImplemented) {
                return res;
            }
        }
        return _binop_rule(t1, o2, t2, "__sub__", "__rsub__", "-");
    }

    /**
     * Implements the Python expression {@code this -= o2} when this and o2 have the same type or
     * are builtin types.
     *
     * @param o2 the object to perform this inplace binary operation with.
     * @return the result of the isub.
     * @throws PyException {@code TypeError} if this operation can't be performed with these
     *             operands.
     **/
    final PyObject _basic_isub(PyObject o2) {
        PyObject x = __isub__(o2);
        if (x != null) {
            return x;
        }
        return this._basic_sub(o2);
    }

    /**
     * Equivalent to the standard Python __mul__ method.
     *
     * @param other the object to perform this binary operation with (the right-hand operand).
     * @return the result of the mul, or null if this operation is not defined
     **/
    public PyObject __mul__(PyObject other) {
        return null;
    }

    /**
     * Equivalent to the standard Python __rmul__ method.
     *
     * @param other the object to perform this binary operation with (the left-hand operand).
     * @return the result of the mul, or null if this operation is not defined.
     **/
    public PyObject __rmul__(PyObject other) {
        return null;
    }

    /**
     * Equivalent to the standard Python __imul__ method.
     *
     * @param other the object to perform this binary operation with (the right-hand operand).
     * @return the result of the imul, or null if this operation is not defined.
     **/
    public PyObject __imul__(PyObject other) {
        return null;
    }

    /**
     * Implements the Python expression {@code this * o2}.
     *
     * @param o2 the object to perform this binary operation with.
     * @return the result of the mul.
     * @throws PyException {@code TypeError} if this operation can't be performed with these
     *             operands.
     **/
    public final PyObject _mul(PyObject o2) {
        PyType t1 = this.getType();
        PyType t2 = o2.getType();
        if (t1 == t2 || t1.builtin && t2.builtin) {
            return this._basic_mul(o2);
        }
        return _binop_rule(t1, o2, t2, "__mul__", "__rmul__", "*");
    }

    /**
     * Implements the Python expression {@code this * o2} when this and o2 have the same type or are
     * builtin types.
     *
     * @param o2 the object to perform this binary operation with.
     * @return the result of the mul.
     * @throws PyException {@code TypeError} if this operation can't be performed with these
     *             operands.
     **/
    final PyObject _basic_mul(PyObject o2) {
        PyObject x = __mul__(o2);
        if (x != null) {
            return x;
        }
        x = o2.__rmul__(this);
        if (x != null) {
            return x;
        }
        throw Py.TypeError(_unsupportedop("*", o2));
    }

    /**
     * Implements the Python expression {@code this *= o2}.
     *
     * @param o2 the object to perform this inplace binary operation with.
     * @return the result of the imul.
     * @throws PyException {@code TypeError} if this operation can't be performed with these
     *             operands.
     **/
    public final PyObject _imul(PyObject o2) {
        PyType t1 = this.getType();
        PyType t2 = o2.getType();
        if (t1 == t2 || t1.builtin && t2.builtin) {
            return this._basic_imul(o2);
        }
        PyObject impl = t1.lookup("__imul__");
        if (impl != null) {
            PyObject res = impl.__get__(this, t1).__call__(o2);
            if (res != Py.NotImplemented) {
                return res;
            }
        }
        return _binop_rule(t1, o2, t2, "__mul__", "__rmul__", "*");
    }

    /**
     * Implements the Python expression {@code this *= o2} when this and o2 have the same type or
     * are builtin types.
     *
     * @param o2 the object to perform this inplace binary operation with.
     * @return the result of the imul.
     * @throws PyException {@code TypeError} if this operation can't be performed with these
     *             operands.
     **/
    final PyObject _basic_imul(PyObject o2) {
        PyObject x = __imul__(o2);
        if (x != null) {
            return x;
        }
        return this._basic_mul(o2);
    }

    /**
     * Equivalent to the standard Python __div__ method
     *
     * @param other the object to perform this binary operation with (the right-hand operand).
     * @return the result of the div, or null if this operation is not defined
     **/
    public PyObject __div__(PyObject other) {
        return null;
    }

    /**
     * Equivalent to the standard Python __rdiv__ method
     *
     * @param other the object to perform this binary operation with (the left-hand operand).
     * @return the result of the div, or null if this operation is not defined.
     **/
    public PyObject __rdiv__(PyObject other) {
        return null;
    }

    /**
     * Equivalent to the standard Python __idiv__ method
     *
     * @param other the object to perform this binary operation with (the right-hand operand).
     * @return the result of the idiv, or null if this operation is not defined
     **/
    public PyObject __idiv__(PyObject other) {
        return null;
    }

    /**
     * Implements the Python expression {@code this / o2}
     *
     * @param o2 the object to perform this binary operation with.
     * @return the result of the div.
     * @throws PyException {@code TypeError} if this operation can't be performed with these
     *             operands.
     **/
    public final PyObject _div(PyObject o2) {
        if (Options.Qnew) {
            return _truediv(o2);
        }
        PyType t1 = this.getType();
        PyType t2 = o2.getType();
        if (t1 == t2 || t1.builtin && t2.builtin) {
            return this._basic_div(o2);
        }
        return _binop_rule(t1, o2, t2, "__div__", "__rdiv__", "/");
    }

    /**
     * Implements the Python expression {@code this / o2} when this and o2 have the same type or are
     * builtin types.
     *
     * @param o2 the object to perform this binary operation with.
     * @return the result of the div.
     * @throws PyException {@code TypeError} if this operation can't be performed with these
     *             operands.
     **/
    final PyObject _basic_div(PyObject o2) {
        PyObject x = __div__(o2);
        if (x != null) {
            return x;
        }
        x = o2.__rdiv__(this);
        if (x != null) {
            return x;
        }
        throw Py.TypeError(_unsupportedop("/", o2));
    }

    /**
     * Implements the Python expression {@code this /= o2}
     *
     * @param o2 the object to perform this inplace binary operation with.
     * @return the result of the idiv.
     * @throws PyException {@code TypeError} if this operation can't be performed with these
     *             operands.
     **/
    public final PyObject _idiv(PyObject o2) {
        if (Options.Qnew) {
            return _itruediv(o2);
        }
        PyType t1 = this.getType();
        PyType t2 = o2.getType();
        if (t1 == t2 || t1.builtin && t2.builtin) {
            return this._basic_idiv(o2);
        }
        PyObject impl = t1.lookup("__idiv__");
        if (impl != null) {
            PyObject res = impl.__get__(this, t1).__call__(o2);
            if (res != Py.NotImplemented) {
                return res;
            }
        }
        return _binop_rule(t1, o2, t2, "__div__", "__rdiv__", "/");
    }

    /**
     * Implements the Python expression {@code this /= o2} when this and o2 have the same type or
     * are builtin types.
     *
     * @param o2 the object to perform this inplace binary operation with.
     * @return the result of the idiv.
     * @throws PyException {@code TypeError} if this operation can't be performed with these
     *             operands.
     **/
    final PyObject _basic_idiv(PyObject o2) {
        PyObject x = __idiv__(o2);
        if (x != null) {
            return x;
        }
        return this._basic_div(o2);
    }

    /**
     * Equivalent to the standard Python __floordiv__ method
     *
     * @param other the object to perform this binary operation with (the right-hand operand).
     * @return the result of the floordiv, or null if this operation is not defined
     **/
    public PyObject __floordiv__(PyObject other) {
        return null;
    }

    /**
     * Equivalent to the standard Python __rfloordiv__ method
     *
     * @param other the object to perform this binary operation with (the left-hand operand).
     * @return the result of the floordiv, or null if this operation is not defined.
     **/
    public PyObject __rfloordiv__(PyObject other) {
        return null;
    }

    /**
     * Equivalent to the standard Python __ifloordiv__ method
     *
     * @param other the object to perform this binary operation with (the right-hand operand).
     * @return the result of the ifloordiv, or null if this operation is not defined
     **/
    public PyObject __ifloordiv__(PyObject other) {
        return null;
    }

    /**
     * Implements the Python expression {@code this // o2}
     *
     * @param o2 the object to perform this binary operation with.
     * @return the result of the floordiv.
     * @throws PyException {@code TypeError} if this operation can't be performed with these
     *             operands.
     **/
    public final PyObject _floordiv(PyObject o2) {
        PyType t1 = this.getType();
        PyType t2 = o2.getType();
        if (t1 == t2 || t1.builtin && t2.builtin) {
            return this._basic_floordiv(o2);
        }
        return _binop_rule(t1, o2, t2, "__floordiv__", "__rfloordiv__", "//");
    }

    /**
     * Implements the Python expression {@code this // o2} when this and o2 have the same type or
     * are builtin types.
     *
     * @param o2 the object to perform this binary operation with.
     * @return the result of the floordiv.
     * @throws PyException {@code TypeError} if this operation can't be performed with these
     *             operands.
     **/
    final PyObject _basic_floordiv(PyObject o2) {
        PyObject x = __floordiv__(o2);
        if (x != null) {
            return x;
        }
        x = o2.__rfloordiv__(this);
        if (x != null) {
            return x;
        }
        throw Py.TypeError(_unsupportedop("//", o2));
    }

    /**
     * Implements the Python expression {@code this //= o2}
     *
     * @param o2 the object to perform this inplace binary operation with.
     * @return the result of the ifloordiv.
     * @throws PyException {@code TypeError} if this operation can't be performed with these
     *             operands.
     **/
    public final PyObject _ifloordiv(PyObject o2) {
        PyType t1 = this.getType();
        PyType t2 = o2.getType();
        if (t1 == t2 || t1.builtin && t2.builtin) {
            return this._basic_ifloordiv(o2);
        }
        PyObject impl = t1.lookup("__ifloordiv__");
        if (impl != null) {
            PyObject res = impl.__get__(this, t1).__call__(o2);
            if (res != Py.NotImplemented) {
                return res;
            }
        }
        return _binop_rule(t1, o2, t2, "__floordiv__", "__rfloordiv__", "//");
    }

    /**
     * Implements the Python expression {@code this //= o2} when this and o2 have the same type or
     * are builtin types.
     *
     * @param o2 the object to perform this inplace binary operation with.
     * @return the result of the ifloordiv.
     * @throws PyException {@code TypeError} if this operation can't be performed with these
     *             operands.
     **/
    final PyObject _basic_ifloordiv(PyObject o2) {
        PyObject x = __ifloordiv__(o2);
        if (x != null) {
            return x;
        }
        return this._basic_floordiv(o2);
    }

    /**
     * Equivalent to the standard Python __truediv__ method
     *
     * @param other the object to perform this binary operation with (the right-hand operand).
     * @return the result of the truediv, or null if this operation is not defined
     **/
    public PyObject __truediv__(PyObject other) {
        return null;
    }

    /**
     * Equivalent to the standard Python __rtruediv__ method
     *
     * @param other the object to perform this binary operation with (the left-hand operand).
     * @return the result of the truediv, or null if this operation is not defined.
     **/
    public PyObject __rtruediv__(PyObject other) {
        return null;
    }

    /**
     * Equivalent to the standard Python __itruediv__ method
     *
     * @param other the object to perform this binary operation with (the right-hand operand).
     * @return the result of the itruediv, or null if this operation is not defined
     **/
    public PyObject __itruediv__(PyObject other) {
        return null;
    }

    /**
     * Implements the Python expression {@code this / o2}
     *
     * @param o2 the object to perform this binary operation with.
     * @return the result of the truediv.
     * @throws PyException {@code TypeError} if this operation can't be performed with these
     *             operands.
     **/
    public final PyObject _truediv(PyObject o2) {
        PyType t1 = this.getType();
        PyType t2 = o2.getType();
        if (t1 == t2 || t1.builtin && t2.builtin) {
            return this._basic_truediv(o2);
        }
        return _binop_rule(t1, o2, t2, "__truediv__", "__rtruediv__", "/");
    }

    /**
     * Implements the Python expression {@code this / o2} when this and o2 have the same type or are
     * builtin types.
     *
     * @param o2 the object to perform this binary operation with.
     * @return the result of the truediv.
     * @throws PyException {@code TypeError} if this operation can't be performed with these
     *             operands.
     **/
    final PyObject _basic_truediv(PyObject o2) {
        PyObject x = __truediv__(o2);
        if (x != null) {
            return x;
        }
        x = o2.__rtruediv__(this);
        if (x != null) {
            return x;
        }
        throw Py.TypeError(_unsupportedop("/", o2));
    }

    /**
     * Implements the Python expression {@code this /= o2}
     *
     * @param o2 the object to perform this inplace binary operation with.
     * @return the result of the itruediv.
     * @throws PyException {@code TypeError} if this operation can't be performed with these
     *             operands.
     **/
    public final PyObject _itruediv(PyObject o2) {
        PyType t1 = this.getType();
        PyType t2 = o2.getType();
        if (t1 == t2 || t1.builtin && t2.builtin) {
            return this._basic_itruediv(o2);
        }
        PyObject impl = t1.lookup("__itruediv__");
        if (impl != null) {
            PyObject res = impl.__get__(this, t1).__call__(o2);
            if (res != Py.NotImplemented) {
                return res;
            }
        }
        return _binop_rule(t1, o2, t2, "__truediv__", "__rtruediv__", "/");
    }

    /**
     * Implements the Python expression {@code this /= o2} when this and o2 have the same type or
     * are builtin types.
     *
     * @param o2 the object to perform this inplace binary operation with.
     * @return the result of the itruediv.
     * @throws PyException {@code TypeError} if this operation can't be performed with these
     *             operands.
     **/
    final PyObject _basic_itruediv(PyObject o2) {
        PyObject x = __itruediv__(o2);
        if (x != null) {
            return x;
        }
        return this._basic_truediv(o2);
    }

    /**
     * Equivalent to the standard Python __mod__ method
     *
     * @param other the object to perform this binary operation with (the right-hand operand).
     * @return the result of the mod, or null if this operation is not defined
     **/
    public PyObject __mod__(PyObject other) {
        return null;
    }

    /**
     * Equivalent to the standard Python __rmod__ method
     *
     * @param other the object to perform this binary operation with (the left-hand operand).
     * @return the result of the mod, or null if this operation is not defined.
     **/
    public PyObject __rmod__(PyObject other) {
        return null;
    }

    /**
     * Equivalent to the standard Python __imod__ method
     *
     * @param other the object to perform this binary operation with (the right-hand operand).
     * @return the result of the imod, or null if this operation is not defined
     **/
    public PyObject __imod__(PyObject other) {
        return null;
    }

    /**
     * Implements the Python expression {@code this % o2}
     *
     * @param o2 the object to perform this binary operation with.
     * @return the result of the mod.
     * @throws PyException {@code TypeError} if this operation can't be performed with these
     *             operands.
     **/
    public final PyObject _mod(PyObject o2) {
        PyType t1 = this.getType();
        PyType t2 = o2.getType();
        if (t1 == t2 || t1.builtin && t2.builtin) {
            return this._basic_mod(o2);
        }
        return _binop_rule(t1, o2, t2, "__mod__", "__rmod__", "%");
    }

    /**
     * Implements the Python expression {@code this % o2} when this and o2 have the same type or are
     * builtin types.
     *
     * @param o2 the object to perform this binary operation with.
     * @return the result of the mod.
     * @throws PyException {@code TypeError} if this operation can't be performed with these
     *             operands.
     **/
    final PyObject _basic_mod(PyObject o2) {
        PyObject x = __mod__(o2);
        if (x != null) {
            return x;
        }
        x = o2.__rmod__(this);
        if (x != null) {
            return x;
        }
        throw Py.TypeError(_unsupportedop("%", o2));
    }

    /**
     * Implements the Python expression {@code this %= o2}
     *
     * @param o2 the object to perform this inplace binary operation with.
     * @return the result of the imod.
     * @throws PyException {@code TypeError} if this operation can't be performed with these
     *             operands.
     **/
    public final PyObject _imod(PyObject o2) {
        PyType t1 = this.getType();
        PyType t2 = o2.getType();
        if (t1 == t2 || t1.builtin && t2.builtin) {
            return this._basic_imod(o2);
        }
        PyObject impl = t1.lookup("__imod__");
        if (impl != null) {
            PyObject res = impl.__get__(this, t1).__call__(o2);
            if (res != Py.NotImplemented) {
                return res;
            }
        }
        return _binop_rule(t1, o2, t2, "__mod__", "__rmod__", "%");
    }

    /**
     * Implements the Python expression {@code this %= o2} when this and o2 have the same type or
     * are builtin types.
     *
     * @param o2 the object to perform this inplace binary operation with.
     * @return the result of the imod.
     * @throws PyException {@code TypeError} if this operation can't be performed with these
     *             operands.
     **/
    final PyObject _basic_imod(PyObject o2) {
        PyObject x = __imod__(o2);
        if (x != null) {
            return x;
        }
        return this._basic_mod(o2);
    }

    /**
     * Equivalent to the standard Python __divmod__ method
     *
     * @param other the object to perform this binary operation with (the right-hand operand).
     * @return the result of the divmod, or null if this operation is not defined
     **/
    public PyObject __divmod__(PyObject other) {
        return null;
    }

    /**
     * Equivalent to the standard Python __rdivmod__ method
     *
     * @param other the object to perform this binary operation with (the left-hand operand).
     * @return the result of the divmod, or null if this operation is not defined.
     **/
    public PyObject __rdivmod__(PyObject other) {
        return null;
    }

    /**
     * Equivalent to the standard Python __idivmod__ method
     *
     * @param other the object to perform this binary operation with (the right-hand operand).
     * @return the result of the idivmod, or null if this operation is not defined
     **/
    public PyObject __idivmod__(PyObject other) {
        return null;
    }

    /**
     * Implements the Python expression {@code this divmod o2}
     *
     * @param o2 the object to perform this binary operation with.
     * @return the result of the divmod.
     * @throws PyException {@code TypeError} if this operation can't be performed with these
     *             operands.
     **/
    public final PyObject _divmod(PyObject o2) {
        PyType t1 = this.getType();
        PyType t2 = o2.getType();
        if (t1 == t2 || t1.builtin && t2.builtin) {
            return this._basic_divmod(o2);
        }
        return _binop_rule(t1, o2, t2, "__divmod__", "__rdivmod__", "divmod");
    }

    /**
     * Implements the Python expression {@code this divmod o2} when this and o2 have the same type
     * or are builtin types.
     *
     * @param o2 the object to perform this binary operation with.
     * @return the result of the divmod.
     * @throws PyException {@code TypeError} if this operation can't be performed with these
     *             operands.
     **/
    final PyObject _basic_divmod(PyObject o2) {
        PyObject x = __divmod__(o2);
        if (x != null) {
            return x;
        }
        x = o2.__rdivmod__(this);
        if (x != null) {
            return x;
        }
        throw Py.TypeError(_unsupportedop("divmod", o2));
    }

    /**
     * Implements the Python expression {@code this divmod= o2}
     *
     * @param o2 the object to perform this inplace binary operation with.
     * @return the result of the idivmod.
     * @throws PyException {@code TypeError} if this operation can't be performed with these
     *             operands.
     **/
    public final PyObject _idivmod(PyObject o2) {
        PyType t1 = this.getType();
        PyType t2 = o2.getType();
        if (t1 == t2 || t1.builtin && t2.builtin) {
            return this._basic_idivmod(o2);
        }
        PyObject impl = t1.lookup("__idivmod__");
        if (impl != null) {
            PyObject res = impl.__get__(this, t1).__call__(o2);
            if (res != Py.NotImplemented) {
                return res;
            }
        }
        return _binop_rule(t1, o2, t2, "__divmod__", "__rdivmod__", "divmod");
    }

    /**
     * Implements the Python expression {@code this divmod= o2} when this and o2 have the same type
     * or are builtin types.
     *
     * @param o2 the object to perform this inplace binary operation with.
     * @return the result of the idivmod.
     * @throws PyException {@code TypeError} if this operation can't be performed with these
     *             operands.
     **/
    final PyObject _basic_idivmod(PyObject o2) {
        PyObject x = __idivmod__(o2);
        if (x != null) {
            return x;
        }
        return this._basic_divmod(o2);
    }

    /**
     * Equivalent to the standard Python __pow__ method
     *
     * @param other the object to perform this binary operation with (the right-hand operand).
     * @return the result of the pow, or null if this operation is not defined
     **/
    public PyObject __pow__(PyObject other) {
        return __pow__(other, null);
    }

    /**
     * Equivalent to the standard Python __rpow__ method
     *
     * @param other the object to perform this binary operation with (the left-hand operand).
     * @return the result of the pow, or null if this operation is not defined.
     **/
    public PyObject __rpow__(PyObject other) {
        return null;
    }

    /**
     * Equivalent to the standard Python __ipow__ method
     *
     * @param other the object to perform this binary operation with (the right-hand operand).
     * @return the result of the ipow, or null if this operation is not defined
     **/
    public PyObject __ipow__(PyObject other) {
        return null;
    }

    /**
     * Implements the Python expression {@code this ** o2}
     *
     * @param o2 the object to perform this binary operation with.
     * @return the result of the pow.
     * @throws PyException {@code TypeError} if this operation can't be performed with these
     *             operands.
     **/
    public final PyObject _pow(PyObject o2) {
        PyType t1 = this.getType();
        PyType t2 = o2.getType();
        if (t1 == t2 || t1.builtin && t2.builtin) {
            return this._basic_pow(o2);
        }
        return _binop_rule(t1, o2, t2, "__pow__", "__rpow__", "**");
    }

    /**
     * Implements the Python expression {@code this ** o2} when this and o2 have the same type or
     * are builtin types.
     *
     * @param o2 the object to perform this binary operation with.
     * @return the result of the pow.
     * @throws PyException {@code TypeError} if this operation can't be performed with these
     *             operands.
     **/
    final PyObject _basic_pow(PyObject o2) {
        PyObject x = __pow__(o2);
        if (x != null) {
            return x;
        }
        x = o2.__rpow__(this);
        if (x != null) {
            return x;
        }
        throw Py.TypeError(_unsupportedop("**", o2));
    }

    /**
     * Implements the Python expression {@code this **= o2}
     *
     * @param o2 the object to perform this inplace binary operation with.
     * @return the result of the ipow.
     * @throws PyException {@code TypeError} if this operation can't be performed with these
     *             operands.
     **/
    public final PyObject _ipow(PyObject o2) {
        PyType t1 = this.getType();
        PyType t2 = o2.getType();
        if (t1 == t2 || t1.builtin && t2.builtin) {
            return this._basic_ipow(o2);
        }
        PyObject impl = t1.lookup("__ipow__");
        if (impl != null) {
            PyObject res = impl.__get__(this, t1).__call__(o2);
            if (res != Py.NotImplemented) {
                return res;
            }
        }
        return _binop_rule(t1, o2, t2, "__pow__", "__rpow__", "**");
    }

    /**
     * Implements the Python expression {@code this **= o2} when this and o2 have the same type or
     * are builtin types.
     *
     * @param o2 the object to perform this inplace binary operation with.
     * @return the result of the ipow.
     * @throws PyException {@code TypeError} if this operation can't be performed with these
     *             operands.
     **/
    final PyObject _basic_ipow(PyObject o2) {
        PyObject x = __ipow__(o2);
        if (x != null) {
            return x;
        }
        return this._basic_pow(o2);
    }

    /**
     * Equivalent to the standard Python __lshift__ method
     *
     * @param other the object to perform this binary operation with (the right-hand operand).
     * @return the result of the lshift, or null if this operation is not defined
     **/
    public PyObject __lshift__(PyObject other) {
        return null;
    }

    /**
     * Equivalent to the standard Python __rlshift__ method
     *
     * @param other the object to perform this binary operation with (the left-hand operand).
     * @return the result of the lshift, or null if this operation is not defined.
     **/
    public PyObject __rlshift__(PyObject other) {
        return null;
    }

    /**
     * Equivalent to the standard Python __ilshift__ method
     *
     * @param other the object to perform this binary operation with (the right-hand operand).
     * @return the result of the ilshift, or null if this operation is not defined
     **/
    public PyObject __ilshift__(PyObject other) {
        return null;
    }

    /**
     * Implements the Python expression {@code this << o2}
     *
     * @param o2 the object to perform this binary operation with.
     * @return the result of the lshift.
     * @throws PyException {@code TypeError} if this operation can't be performed with these
     *             operands.
     **/
    public final PyObject _lshift(PyObject o2) {
        PyType t1 = this.getType();
        PyType t2 = o2.getType();
        if (t1 == t2 || t1.builtin && t2.builtin) {
            return this._basic_lshift(o2);
        }
        return _binop_rule(t1, o2, t2, "__lshift__", "__rlshift__", "<<");
    }

    /**
     * Implements the Python expression {@code this << o2} when this and o2 have the same type or
     * are builtin types.
     *
     * @param o2 the object to perform this binary operation with.
     * @return the result of the lshift.
     * @throws PyException {@code TypeError} if this operation can't be performed with these
     *             operands.
     **/
    final PyObject _basic_lshift(PyObject o2) {
        PyObject x = __lshift__(o2);
        if (x != null) {
            return x;
        }
        x = o2.__rlshift__(this);
        if (x != null) {
            return x;
        }
        throw Py.TypeError(_unsupportedop("<<", o2));
    }

    /**
     * Implements the Python expression {@code this <<= o2}
     *
     * @param o2 the object to perform this inplace binary operation with.
     * @return the result of the ilshift.
     * @throws PyException {@code TypeError} if this operation can't be performed with these
     *             operands.
     **/
    public final PyObject _ilshift(PyObject o2) {
        PyType t1 = this.getType();
        PyType t2 = o2.getType();
        if (t1 == t2 || t1.builtin && t2.builtin) {
            return this._basic_ilshift(o2);
        }
        PyObject impl = t1.lookup("__ilshift__");
        if (impl != null) {
            PyObject res = impl.__get__(this, t1).__call__(o2);
            if (res != Py.NotImplemented) {
                return res;
            }
        }
        return _binop_rule(t1, o2, t2, "__lshift__", "__rlshift__", "<<");
    }

    /**
     * Implements the Python expression {@code this <<= o2} when this and o2 have the same type or
     * are builtin types.
     *
     * @param o2 the object to perform this inplace binary operation with.
     * @return the result of the ilshift.
     * @throws PyException {@code TypeError} if this operation can't be performed with these
     *             operands.
     **/
    final PyObject _basic_ilshift(PyObject o2) {
        PyObject x = __ilshift__(o2);
        if (x != null) {
            return x;
        }
        return this._basic_lshift(o2);
    }

    /**
     * Equivalent to the standard Python __rshift__ method
     *
     * @param other the object to perform this binary operation with (the right-hand operand).
     * @return the result of the rshift, or null if this operation is not defined
     **/
    public PyObject __rshift__(PyObject other) {
        return null;
    }

    /**
     * Equivalent to the standard Python __rrshift__ method
     *
     * @param other the object to perform this binary operation with (the left-hand operand).
     * @return the result of the rshift, or null if this operation is not defined.
     **/
    public PyObject __rrshift__(PyObject other) {
        return null;
    }

    /**
     * Equivalent to the standard Python __irshift__ method
     *
     * @param other the object to perform this binary operation with (the right-hand operand).
     * @return the result of the irshift, or null if this operation is not defined
     **/
    public PyObject __irshift__(PyObject other) {
        return null;
    }

    /**
     * Implements the Python expression {@code this >> o2}
     *
     * @param o2 the object to perform this binary operation with.
     * @return the result of the rshift.
     * @throws PyException {@code TypeError} if this operation can't be performed with these
     *             operands.
     **/
    public final PyObject _rshift(PyObject o2) {
        PyType t1 = this.getType();
        PyType t2 = o2.getType();
        if (t1 == t2 || t1.builtin && t2.builtin) {
            return this._basic_rshift(o2);
        }
        return _binop_rule(t1, o2, t2, "__rshift__", "__rrshift__", ">>");
    }

    /**
     * Implements the Python expression {@code this >> o2} when this and o2 have the same type or
     * are builtin types.
     *
     * @param o2 the object to perform this binary operation with.
     * @return the result of the rshift.
     * @throws PyException {@code TypeError} if this operation can't be performed with these
     *             operands.
     **/
    final PyObject _basic_rshift(PyObject o2) {
        PyObject x = __rshift__(o2);
        if (x != null) {
            return x;
        }
        x = o2.__rrshift__(this);
        if (x != null) {
            return x;
        }
        throw Py.TypeError(_unsupportedop(">>", o2));
    }

    /**
     * Implements the Python expression {@code this >>= o2}
     *
     * @param o2 the object to perform this inplace binary operation with.
     * @return the result of the irshift.
     * @throws PyException {@code TypeError} if this operation can't be performed with these
     *             operands.
     **/
    public final PyObject _irshift(PyObject o2) {
        PyType t1 = this.getType();
        PyType t2 = o2.getType();
        if (t1 == t2 || t1.builtin && t2.builtin) {
            return this._basic_irshift(o2);
        }
        PyObject impl = t1.lookup("__irshift__");
        if (impl != null) {
            PyObject res = impl.__get__(this, t1).__call__(o2);
            if (res != Py.NotImplemented) {
                return res;
            }
        }
        return _binop_rule(t1, o2, t2, "__rshift__", "__rrshift__", ">>");
    }

    /**
     * Implements the Python expression {@code this >>= o2} when this and o2 have the same type or
     * are builtin types.
     *
     * @param o2 the object to perform this inplace binary operation with.
     * @return the result of the irshift.
     * @throws PyException {@code TypeError} if this operation can't be performed with these
     *             operands.
     **/
    final PyObject _basic_irshift(PyObject o2) {
        PyObject x = __irshift__(o2);
        if (x != null) {
            return x;
        }
        return this._basic_rshift(o2);
    }

    /**
     * Equivalent to the standard Python __and__ method
     *
     * @param other the object to perform this binary operation with (the right-hand operand).
     * @return the result of the and, or null if this operation is not defined
     **/
    public PyObject __and__(PyObject other) {
        return null;
    }

    /**
     * Equivalent to the standard Python __rand__ method
     *
     * @param other the object to perform this binary operation with (the left-hand operand).
     * @return the result of the and, or null if this operation is not defined.
     **/
    public PyObject __rand__(PyObject other) {
        return null;
    }

    /**
     * Equivalent to the standard Python __iand__ method
     *
     * @param other the object to perform this binary operation with (the right-hand operand).
     * @return the result of the iand, or null if this operation is not defined
     **/
    public PyObject __iand__(PyObject other) {
        return null;
    }

    /**
     * Implements the Python expression {@code this & o2}
     *
     * @param o2 the object to perform this binary operation with.
     * @return the result of the and.
     * @throws PyException {@code TypeError} if this operation can't be performed with these
     *             operands.
     **/
    public final PyObject _and(PyObject o2) {
        PyType t1 = this.getType();
        PyType t2 = o2.getType();
        if (t1 == t2 || t1.builtin && t2.builtin) {
            return this._basic_and(o2);
        }
        return _binop_rule(t1, o2, t2, "__and__", "__rand__", "&");
    }

    /**
     * Implements the Python expression {@code this & o2} when this and o2 have the same type or are
     * builtin types.
     *
     * @param o2 the object to perform this binary operation with.
     * @return the result of the and.
     * @throws PyException {@code TypeError} if this operation can't be performed with these
     *             operands.
     **/
    final PyObject _basic_and(PyObject o2) {
        PyObject x = __and__(o2);
        if (x != null) {
            return x;
        }
        x = o2.__rand__(this);
        if (x != null) {
            return x;
        }
        throw Py.TypeError(_unsupportedop("&", o2));
    }

    /**
     * Implements the Python expression {@code this &= o2}
     *
     * @param o2 the object to perform this inplace binary operation with.
     * @return the result of the iand.
     * @throws PyException {@code TypeError} if this operation can't be performed with these
     *             operands.
     **/
    public final PyObject _iand(PyObject o2) {
        PyType t1 = this.getType();
        PyType t2 = o2.getType();
        if (t1 == t2 || t1.builtin && t2.builtin) {
            return this._basic_iand(o2);
        }
        PyObject impl = t1.lookup("__iand__");
        if (impl != null) {
            PyObject res = impl.__get__(this, t1).__call__(o2);
            if (res != Py.NotImplemented) {
                return res;
            }
        }
        return _binop_rule(t1, o2, t2, "__and__", "__rand__", "&");
    }

    /**
     * Implements the Python expression {@code this &= o2} when this and o2 have the same type or
     * are builtin types.
     *
     * @param o2 the object to perform this inplace binary operation with.
     * @return the result of the iand.
     * @throws PyException {@code TypeError} if this operation can't be performed with these
     *             operands.
     **/
    final PyObject _basic_iand(PyObject o2) {
        PyObject x = __iand__(o2);
        if (x != null) {
            return x;
        }
        return this._basic_and(o2);
    }

    /**
     * Equivalent to the standard Python __or__ method
     *
     * @param other the object to perform this binary operation with (the right-hand operand).
     * @return the result of the or, or null if this operation is not defined
     **/
    public PyObject __or__(PyObject other) {
        return null;
    }

    /**
     * Equivalent to the standard Python __ror__ method
     *
     * @param other the object to perform this binary operation with (the left-hand operand).
     * @return the result of the or, or null if this operation is not defined.
     **/
    public PyObject __ror__(PyObject other) {
        return null;
    }

    /**
     * Equivalent to the standard Python __ior__ method
     *
     * @param other the object to perform this binary operation with (the right-hand operand).
     * @return the result of the ior, or null if this operation is not defined
     **/
    public PyObject __ior__(PyObject other) {
        return null;
    }

    /**
     * Implements the Python expression {@code this | o2}
     *
     * @param o2 the object to perform this binary operation with.
     * @return the result of the or.
     * @throws PyException {@code TypeError} if this operation can't be performed with these
     *             operands.
     **/
    public final PyObject _or(PyObject o2) {
        PyType t1 = this.getType();
        PyType t2 = o2.getType();
        if (t1 == t2 || t1.builtin && t2.builtin) {
            return this._basic_or(o2);
        }
        return _binop_rule(t1, o2, t2, "__or__", "__ror__", "|");
    }

    /**
     * Implements the Python expression {@code this | o2} when this and o2 have the same type or are
     * builtin types.
     *
     * @param o2 the object to perform this binary operation with.
     * @return the result of the or.
     * @throws PyException {@code TypeError} if this operation can't be performed with these
     *             operands.
     **/
    final PyObject _basic_or(PyObject o2) {
        PyObject x = __or__(o2);
        if (x != null) {
            return x;
        }
        x = o2.__ror__(this);
        if (x != null) {
            return x;
        }
        throw Py.TypeError(_unsupportedop("|", o2));
    }

    /**
     * Implements the Python expression {@code this |= o2}
     *
     * @param o2 the object to perform this inplace binary operation with.
     * @return the result of the ior.
     * @throws PyException {@code TypeError} if this operation can't be performed with these
     *             operands.
     **/
    public final PyObject _ior(PyObject o2) {
        PyType t1 = this.getType();
        PyType t2 = o2.getType();
        if (t1 == t2 || t1.builtin && t2.builtin) {
            return this._basic_ior(o2);
        }
        PyObject impl = t1.lookup("__ior__");
        if (impl != null) {
            PyObject res = impl.__get__(this, t1).__call__(o2);
            if (res != Py.NotImplemented) {
                return res;
            }
        }
        return _binop_rule(t1, o2, t2, "__or__", "__ror__", "|");
    }

    /**
     * Implements the Python expression {@code this |= o2} when this and o2 have the same type or
     * are builtin types.
     *
     * @param o2 the object to perform this inplace binary operation with.
     * @return the result of the ior.
     * @throws PyException {@code TypeError} if this operation can't be performed with these
     *             operands.
     **/
    final PyObject _basic_ior(PyObject o2) {
        PyObject x = __ior__(o2);
        if (x != null) {
            return x;
        }
        return this._basic_or(o2);
    }

    /**
     * Equivalent to the standard Python __xor__ method
     *
     * @param other the object to perform this binary operation with (the right-hand operand).
     * @return the result of the xor, or null if this operation is not defined
     **/
    public PyObject __xor__(PyObject other) {
        return null;
    }

    /**
     * Equivalent to the standard Python __rxor__ method
     *
     * @param other the object to perform this binary operation with (the left-hand operand).
     * @return the result of the xor, or null if this operation is not defined.
     **/
    public PyObject __rxor__(PyObject other) {
        return null;
    }

    /**
     * Equivalent to the standard Python __ixor__ method
     *
     * @param other the object to perform this binary operation with (the right-hand operand).
     * @return the result of the ixor, or null if this operation is not defined
     **/
    public PyObject __ixor__(PyObject other) {
        return null;
    }

    /**
     * Implements the Python expression {@code this ^ o2}
     *
     * @param o2 the object to perform this binary operation with.
     * @return the result of the xor.
     * @throws PyException {@code TypeError} if this operation can't be performed with these
     *             operands.
     **/
    public final PyObject _xor(PyObject o2) {
        PyType t1 = this.getType();
        PyType t2 = o2.getType();
        if (t1 == t2 || t1.builtin && t2.builtin) {
            return this._basic_xor(o2);
        }
        return _binop_rule(t1, o2, t2, "__xor__", "__rxor__", "^");
    }

    /**
     * Implements the Python expression {@code this ^ o2} when this and o2 have the same type or are
     * builtin types.
     *
     * @param o2 the object to perform this binary operation with.
     * @return the result of the xor.
     * @throws PyException {@code TypeError} if this operation can't be performed with these
     *             operands.
     **/
    final PyObject _basic_xor(PyObject o2) {
        PyObject x = __xor__(o2);
        if (x != null) {
            return x;
        }
        x = o2.__rxor__(this);
        if (x != null) {
            return x;
        }
        throw Py.TypeError(_unsupportedop("^", o2));
    }

    /**
     * Implements the Python expression {@code this ^= o2}
     *
     * @param o2 the object to perform this inplace binary operation with.
     * @return the result of the ixor.
     * @throws PyException {@code TypeError} if this operation can't be performed with these
     *             operands.
     **/
    public final PyObject _ixor(PyObject o2) {
        PyType t1 = this.getType();
        PyType t2 = o2.getType();
        if (t1 == t2 || t1.builtin && t2.builtin) {
            return this._basic_ixor(o2);
        }
        PyObject impl = t1.lookup("__ixor__");
        if (impl != null) {
            PyObject res = impl.__get__(this, t1).__call__(o2);
            if (res != Py.NotImplemented) {
                return res;
            }
        }
        return _binop_rule(t1, o2, t2, "__xor__", "__rxor__", "^");
    }

    /**
     * Implements the Python expression {@code this ^= o2} when this and o2 have the same type or
     * are builtin types.
     *
     * @param o2 the object to perform this inplace binary operation with.
     * @return the result of the ixor.
     * @throws PyException {@code TypeError} if this operation can't be performed with these
     *             operands.
     **/
    final PyObject _basic_ixor(PyObject o2) {
        PyObject x = __ixor__(o2);
        if (x != null) {
            return x;
        }
        return this._basic_xor(o2);
    }

    // Generated by make_binops.py (End)

    /**
     * A convenience function for PyProxys.
     */
    public PyObject _jcallexc(Object[] args) throws Throwable {
        try {
            return __call__(Py.javas2pys(args));
        } catch (PyException e) {
            if (e.value.getJavaProxy() != null) {
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
                    Py.stderr.println("Exception in Python proxy returning to Java:");
                    Py.printException(e);
                }
            }
            throw e;
        }
    }

    public void _jthrow(Throwable t) {
        if (t instanceof RuntimeException) {
            throw (RuntimeException) t;
        }
        if (t instanceof Error) {
            throw (Error) t;
        }
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
     * Shortcut for calling a method on a PyObject from Java. This form is equivalent to
     * o.__getattr__(name).__call__(args, keywords)
     *
     * @param name the name of the method to call. This must be an interned string!
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
     * @param name the name of the method to call. This must be an interned string!
     * @return the result of calling the method name with no args
     **/
    public PyObject invoke(String name) {
        PyObject f = __getattr__(name);
        return f.__call__();
    }

    /**
     * Shortcut for calling a method on a PyObject with one arg.
     *
     * @param name the name of the method to call. This must be an interned string!
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
     * @param name the name of the method to call. This must be an interned string!
     * @param arg1 the first argument of the method.
     * @param arg2 the second argument of the method.
     * @return the result of calling the method name with arg1 and arg2
     **/
    public PyObject invoke(String name, PyObject arg1, PyObject arg2) {
        PyObject f = __getattr__(name);
        return f.__call__(arg1, arg2);
    }

    /**
     * Shortcut for calling a method on a PyObject with one extra initial argument.
     *
     * @param name the name of the method to call. This must be an interned string!
     * @param arg1 the first argument of the method.
     * @param args an array of the arguments to the call.
     * @param keywords the keywords to use in the call.
     * @return the result of calling the method name with arg1 args and keywords
     **/
    public PyObject invoke(String name, PyObject arg1, PyObject[] args, String[] keywords) {
        PyObject f = __getattr__(name);
        return f.__call__(arg1, args, keywords);
    }

    /* descriptors and lookup protocols */

    /**
     * xxx implements where meaningful
     *
     * @return internal object per instance dict or null
     */
    public PyObject fastGetDict() {
        return null;
    }

    /**
     * xxx implements where meaningful
     *
     * @return internal object __dict__ or null
     */
    public PyObject getDict() {
        return null;
    }

    public void setDict(PyObject newDict) {
        // fallback if setDict not implemented in subclass
        throw Py.TypeError(
                "can't set attribute '__dict__' of instance of " + getType().fastGetName());
    }

    public void delDict() {
        // fallback to error
        throw Py.TypeError("can't delete attribute '__dict__' of instance of '"
                + getType().fastGetName() + "'");
    }

    public boolean implementsDescrGet() {
        return objtype.hasGet;
    }

    public boolean implementsDescrSet() {
        return objtype.hasSet;
    }

    public boolean implementsDescrDelete() {
        return objtype.hasDelete;
    }

    public boolean isDataDescr() {
        return objtype.hasSet || objtype.hasDelete;
    }

    /**
     * Get descriptor for this PyObject.
     *
     * @param obj - the instance accessing this descriptor. Can be null if this is being accessed by
     *            a type.
     * @param type - the type accessing this descriptor. Will be null if obj exists as obj is of the
     *            type accessing the descriptor.
     * @return - the object defined for this descriptor for the given obj and type.
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

    @ExposedMethod(doc = BuiltinDocs.object___getattribute___doc)
    final PyObject object___getattribute__(PyObject arg0) {
        String name = asName(arg0);
        PyObject ret = object___findattr__(name);
        if (ret == null) {
            noAttributeError(name);
        }
        return ret;
    }

    // name must be interned
    final PyObject object___findattr__(String name) {
        PyObject descr = objtype.lookup(name);
        PyObject res;
        boolean get = false;

        if (descr != null) {
            get = descr.implementsDescrGet();
            if (get && descr.isDataDescr()) {
                return descr.__get__(this, objtype);
            }
        }

        PyObject obj_dict = fastGetDict();
        if (obj_dict != null) {
            res = obj_dict.__finditem__(name);
            if (res != null) {
                return res;
            }
        }

        if (get) {
            return descr.__get__(this, objtype);
        }

        if (descr != null) {
            return descr;
        }

        return null;
    }

    @ExposedMethod(doc = BuiltinDocs.object___setattr___doc)
    final void object___setattr__(PyObject name, PyObject value) {
        hackCheck("__setattr__");
        object___setattr__(asName(name), value);
    }

    final void object___setattr__(String name, PyObject value) {
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

    @ExposedMethod(doc = BuiltinDocs.object___delattr___doc)
    final void object___delattr__(PyObject name) {
        hackCheck("__delattr__");
        object___delattr__(asName(name));
    }

    public static final String asName(PyObject obj) {
        try {
            return obj.asName(0);
        } catch (PyObject.ConversionException e) {
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
                if (exc.match(Py.KeyError)) {
                    noAttributeError(name);
                } else {
                    throw exc;
                }
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
     * Helper to check for object.__setattr__ or __delattr__ applied to a type (The Carlo Verre
     * hack).
     *
     * @param what String method name to check for
     */
    private void hackCheck(String what) {
        if (this instanceof PyType && ((PyType) this).builtin) {
            throw Py.TypeError(
                    String.format("can't apply this %s to %s object", what, objtype.fastGetName()));
        }
    }

    /**
     * A common helper method, use to prevent infinite recursion when a Python object implements
     * __reduce__ and sometimes calls object.__reduce__. Trying to do it all in __reduce__ex__
     * caused # this problem. See http://bugs.jython.org/issue2323.
     */
    private PyObject commonReduce(int proto) {
        PyObject res;

        if (proto >= 2) {
            res = reduce_2();
        } else {
            PyObject copyreg = __builtin__.__import__("copy_reg", null, null, Py.EmptyTuple);
            PyObject copyreg_reduce = copyreg.__findattr__("_reduce_ex");
            res = copyreg_reduce.__call__(this, new PyInteger(proto));
        }
        return res;
    }

    /**
     * Used for pickling. Default implementation calls object___reduce__.
     *
     * @return a tuple of (class, tuple)
     */
    public PyObject __reduce__() {
        return object___reduce__();
    }

    @ExposedMethod(doc = BuiltinDocs.object___reduce___doc)
    final PyObject object___reduce__() {
        return commonReduce(0);
    }

    /**
     * Used for pickling. If the subclass specifies __reduce__, it will override __reduce_ex__ in
     * the base-class, even if __reduce_ex__ was called with an argument.
     *
     * @param arg PyInteger specifying reduce algorithm (method without this argument defaults to
     *            0).
     * @return a tuple of (class, tuple)
     */
    public PyObject __reduce_ex__(int arg) {
        return object___reduce_ex__(arg);
    }

    public PyObject __reduce_ex__() {
        return object___reduce_ex__(0);
    }

    @ExposedMethod(defaults = "0", doc = BuiltinDocs.object___reduce___doc)
    final PyObject object___reduce_ex__(int arg) {
        PyObject res;

        PyObject clsreduce = this.getType().__findattr__("__reduce__");
        PyObject objreduce = (new PyObject()).getType().__findattr__("__reduce__");

        if (clsreduce != objreduce) {
            res = this.__reduce__();
        } else {
            res = commonReduce(arg);
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
        int n, i;

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
                for (i = 0; i < ((PyList) names).size(); i++) {
                    PyObject name = ((PyList) names).pyget(i);
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
            listitems = ((PyList) this).__iter__();
        }
        if (!(this instanceof PyDictionary)) {
            dictitems = Py.None;
        } else {
            dictitems = invoke("iteritems");
        }

        PyObject copyreg = __builtin__.__import__("copy_reg", null, null, Py.EmptyTuple);
        PyObject newobj = copyreg.__findattr__("__newobj__");

        n = ((PyTuple) args).size();
        PyObject args2[] = new PyObject[n + 1];
        args2[0] = cls;
        for (i = 0; i < n; i++) {
            args2[i + 1] = ((PyTuple) args).pyget(i);
        }

        return new PyTuple(newobj, new PyTuple(args2), state, listitems, dictitems);
    }

    public PyTuple __getnewargs__() {
        // default is empty tuple
        return new PyTuple();
    }

    @ExposedClassMethod(doc = BuiltinDocs.object___subclasshook___doc)
    public static PyObject object___subclasshook__(PyType type, PyObject subclass) {
        return Py.NotImplemented;
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

    public String asString() {
        throw Py.TypeError("expected a str");
    }

    public String asStringOrNull(int index) throws ConversionException {
        return asString(index);
    }

    public String asStringOrNull() {
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

    /**
     * Convert this object into an int. Throws a PyException on failure.
     *
     * @return an int value
     */
    public int asInt() {
        PyObject intObj;
        try {
            intObj = __int__();
        } catch (PyException pye) {
            if (pye.match(Py.AttributeError)) {
                throw Py.TypeError("an integer is required");
            }
            throw pye;
        }
        if (!(intObj instanceof PyInteger || intObj instanceof PyLong)) {
            // Shouldn't happen except with buggy builtin types
            throw Py.TypeError("nb_int should return int object");
        }
        return intObj.asInt();
    }

    public long asLong(int index) throws ConversionException {
        throw new ConversionException(index);
    }

    /**
     * Convert this object into a long. Throws a PyException on failure.
     *
     * @return an long value
     */
    public long asLong() {
        PyObject longObj;
        try {
            longObj = __long__();
        } catch (PyException pye) {
            if (pye.match(Py.AttributeError)) {
                throw Py.TypeError("an integer is required");
            }
            throw pye;
        }
        if (!(longObj instanceof PyLong || longObj instanceof PyInteger)) {
            // Shouldn't happen except with buggy builtin types
            throw Py.TypeError("integer conversion failed");
        }
        return longObj.asLong();
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
            if (pye.match(Py.AttributeError)) {
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
}


/**
 * A very specialized tuple-like class used when detecting cycles during object comparisons. This
 * classes is different from an normal tuple by hashing and comparing its elements by identity.
 */
class PyIdentityTuple extends PyObject implements Traverseproc {

    PyObject[] list;

    public PyIdentityTuple(PyObject elements[]) {
        list = elements;
    }

    @Override
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

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof PyIdentityTuple)) {
            return false;
        }
        PyIdentityTuple that = (PyIdentityTuple) o;
        if (list.length != that.list.length) {
            return false;
        }
        for (int i = 0; i < list.length; i++) {
            if (list[i] != that.list[i]) {
                return false;
            }
        }
        return true;
    }

    /* Traverseproc implementation */
    @Override
    public int traverse(Visitproc visit, Object arg) {
        if (list != null) {
            int retVal;
            for (PyObject ob : list) {
                if (ob != null) {
                    retVal = visit.visit(ob, arg);
                    if (retVal != 0) {
                        return retVal;
                    }
                }
            }
        }
        return 0;
    }

    @Override
    public boolean refersDirectlyTo(PyObject ob) {
        if (ob == null || list == null) {
            return false;
        }
        for (PyObject obj : list) {
            if (ob == obj) {
                return true;
            }
        }
        return false;
    }
}
