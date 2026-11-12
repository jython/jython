// Copyright (c)2026 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.core;

import java.lang.invoke.MethodHandles;

import org.python.internal._PyUtil;
import org.python.types.Exposed;
import org.python.types.FastCall;
import org.python.types.Feature;
import org.python.types.TypeSpec;
import org.python.types.WithClass;

/**
 * The Python type {@code types.MethodWrapperType} (seen also as
 * {@code <class 'method-wrapper'>}) represents a special method bound
 * to a particular target "self" object. It is part of the mechanism
 * which allows a special method defined in Java to be called from
 * Python using its name as an attribute.
 * <p>
 * An example from the wild is to evaluate: {@code (42).__add__},
 * binding the method {@code __add__} to the target {@code 42}, to
 * produce a callable equivalent to <i>λ x . 42+x</i>. An instance of
 * this class results from a call to
 * {@link PyWrapperDescr#__get__(Object, PyType)}.
 */
// Compare CPython wrapperobject in descrobject.c
// and _PyMethodWrapper_Type in descrobject.c
public class PyMethodWrapper implements WithClass, FastCall {

    /** Only referenced during bootstrap by {@link TypeSystem}. */
    static class Spec {
        /** @return the type specification. */
        static TypeSpec get() {
            return new TypeSystem.BootstrapSpec("method-wrapper", MethodHandles.lookup(),
                    PyMethodWrapper.class).remove(Feature.INSTANTIABLE);
        }
    }

    /**
     * Return the Python type of {@code method-wrapper} objects,
     * {@code types.MethodWrapperType}.
     *
     * @return {@code <class 'method-wrapper'>}
     */
    public static final PyType TYPE() { return TypeSystem.TYPE_method_wrapper; }

    // No subclasses so always this type
    @Override
    public PyType getType() { return TYPE(); }

    /** Descriptor for the method being bound. */
    @Exposed.Member
    PyWrapperDescr descr;

    /**
     * The target object of the method call that results when
     * {@link #__call__(Object[], String[]) __call__} is invoked on this
     * object. This is exposed to Python as {@code __self__}.
     */
    @Exposed.Member("__self__")
    final Object self;

    /**
     * Bind a slot wrapper descriptor to its target. The result is a
     * callable object e.g. in {@code bark = "Woof!".__mul__}, {@code bark}
     * will be an instance of this class, {@code "Woof!}" is {@code self}
     * and {@code str.__mul__} is the descriptor.
     *
     * @param descr for the special method to bind
     * @param self to which this method call is bound
     */
    PyMethodWrapper(PyWrapperDescr descr, Object self) {
        this.descr = descr;
        this.self = self;
    }

    // Exposed attributes --------------------------------------------

    /*
     * While superficially identical to members that other classes inherit
     * from Descriptor, note that __objclass__ and __name__ are here
     * read-only *attributes* (getter methods) rather than *members* so that
     * we can get them from the descriptor of the wrapped special method.
     * This the same in CPython.
     */

    /**
     * Return the class where this method was defined. This is exposed to
     * Python as {@code __objclass__}. It is the type of the object that is
     * expected as the first (that is, {@code self}) argument, and that is
     * bound (as {@code obj}) when {@code __get__} is called.
     *
     * @return the class where this object was defined
     */
    @Exposed.Getter
    // Compare CPython wrapper_objclass in descrobject.c
    public Object __objclass__() { return descr.__objclass__(); }

    /**
     * Return the name of the member, attribute or method described. For
     * example it is {@code "__add__"} or {@code "__str__"}. This is exposed
     * to Python as {@code __name__}.
     *
     * @return plain name of the (special) method.
     */
    @Exposed.Getter
    // Compare CPython wrapper_name in descrobject.c
    private Object __name__() { return descr.sm.methodName; }

    /** @return documentation string formatted for external reader. */
    @Exposed.Getter
    // Compare CPython wrapper_doc in descrobject.c
    private Object __doc__() {
        return Descriptor.getDocFromInternalDoc(descr.sm.methodName, descr.sm.doc);
    }

    /** @return signature string based on internal documentation. */
    @Exposed.Getter
    // Compare CPython wrapper_text_signature in descrobject.c
    private Object __text_signature__() {
        return Descriptor.getTextSignatureFromInternalDoc(descr.sm.methodName, descr.sm.doc);
    }

    /**
     * Return the qualified name attribute.
     *
     * @return qualified name of the method.
     * @throws PyAttributeError if the attribute does not exist
     * @throws Throwable from other implementation errors
     */
    @Exposed.Getter
    // Compare CPython wrapper_qualname in descrobject.c
    private Object __qualname__() throws PyAttributeError, Throwable {
        return descr.__qualname__();
    }

    // Special methods ------------------------------------------------

    // Compare CPython wrapper_repr in descrobject.c
    @SuppressWarnings("unused")
    private Object __repr__() {
        return String.format("<method-wrapper '%s' of %s>", descr.sm.methodName,
                _PyUtil.toAt(self));
    }

    // Compare CPython wrapper_richcompare in descrobject.c
    @SuppressWarnings("unused")
    private Object __eq__(Object b) {
        // Both arguments should be exactly PyMethodWrapper
        if (b instanceof PyMethodWrapper) {
            PyMethodWrapper wb = (PyMethodWrapper)b;
            return descr == wb.descr && self == wb.self;
        }
        return Py.NotImplemented;
    }

    // Compare CPython wrapper_richcompare in descrobject.c
    @SuppressWarnings("unused")
    private Object __ne__(Object b) {
        // Both arguments should be exactly PyMethodWrapper
        if (b instanceof PyMethodWrapper) {
            PyMethodWrapper wb = (PyMethodWrapper)b;
            return descr != wb.descr || self != wb.self;
        }
        return Py.NotImplemented;
    }

    // Compare CPython wrapper_hash in descrobject.c
    @SuppressWarnings("unused")
    private int __hash__() {
        int x = self.hashCode() ^ descr.hashCode();
        return x == -1 ? -2 : x;
    }

    // protected PyObject __reduce__(PyObject ignored) {
    // return Py_BuildValue("N(OO)",
    // _PyEval_GetBuiltinId(ID.getattr), self,
    // PyDescr_NAME(descr));
    // }

    /**
     * Invoke the associated method descriptor {@link #descr} the standard
     * {@code __call__} arguments supplied, providing the bound object
     * {@link #self} as the first argument.
     *
     * @param args all arguments beginning with {@code self}
     * @param names of keyword arguments
     * @return result of calling the bound method
     * @throws PyBaseException (TypeError) if {@code self} is of the wrong
     *     type or the pattern of arguments is unacceptable (number, keyword
     *     use).
     * @throws Throwable from the implementation of the method
     */
    // Compare CPython wrapper_call in descrobject.c
    Object __call__(Object[] args, String[] names) throws Throwable {
        try {
            return call(args, names);
        } catch (ArgumentError ae) {
            /*
             * FastCall.call() methods may encode a TypeError as an ArgumentError
             * with limited context.
             */
            throw typeError(ae, args, names);
        }
    }

    @Override
    public Object call(Object[] args, String[] names) throws Throwable {
        return descr.call(self, args, names);
    }

    @Override
    public Object call() throws ArgumentError, Throwable { return descr.call(self); }

    @Override
    public Object call(Object a1) throws ArgumentError, Throwable { return descr.call(self, a1); }

    @Override
    public Object call(Object a1, Object a2) throws ArgumentError, Throwable {
        return descr.call(self, a1, a2);
    }

    @Override
    public PyBaseException typeError(ArgumentError ae, Object[] args, String[] names) {
        return descr.typeError(ae, args, names);
    }

    @Override
    public String toString() { return PyUtil.defaultToString(this); }

}
