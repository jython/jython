package org.python.core;

import java.lang.invoke.MethodHandles;

import org.python.core.PyType.Flag;



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
class PyMethodWrapper extends AbstractPyObject implements FastCall {

    static final PyType TYPE = PyType.fromSpec(
            new PyType.Spec("method-wrapper", MethodHandles.lookup())
                    .flagNot(Flag.BASETYPE));

    // No subclasses so always this type
    @Override
    public PyType getType() { return TYPE; }

    /** Descriptor for the method being bound. */
    PyWrapperDescr descr;

    /**
     * The target object of the method call that results when
     * {@link #__call__(Object[], String[]) __call__} is invoked on this
     * object. This is exposed to Python as {@code __self__}.
     */
    final Object self;

    /**
     * Bind a slot wrapper descriptor to its target. The result is a
     * callable object e.g. in {@code bark = "Woof!".__mul__},
     * {@code bark} will be an instance of this class, {@code "Woof!}"
     * is {@code self} and {@code str.__mul__} is the descriptor.
     *
     * @param descr for the special method to bind
     * @param self to which this method call is bound
     */
    PyMethodWrapper(PyWrapperDescr descr, Object self) {
        super(TYPE);
        this.descr = descr;
        this.self = self;
    }



    // Special methods ------------------------------------------------

    // Compare CPython wrapper_repr in descrobject.c
    protected Object __repr__() {
        return String.format("<method-wrapper '%s' of %s>",
                descr.slot.methodName, PyObjectUtil.toAt(self));
    }

    // Compare CPython wrapper_richcompare in descrobject.c
    protected Object __eq__(Object b) {
        // Both arguments should be exactly PyMethodWrapper
        if (b instanceof PyMethodWrapper) {
            PyMethodWrapper wb = (PyMethodWrapper) b;
            return descr == wb.descr && self == wb.self;
        }
        return Py.NotImplemented;
    }

    // Compare CPython wrapper_richcompare in descrobject.c
    protected Object __ne__(Object b) {
        // Both arguments should be exactly PyMethodWrapper
        if (b instanceof PyMethodWrapper) {
            PyMethodWrapper wb = (PyMethodWrapper) b;
            return descr != wb.descr || self != wb.self;
        }
        return Py.NotImplemented;
    }

    // Compare CPython wrapper_hash in descrobject.c
    protected int __hash__() {
        int x = self.hashCode() ^ descr.hashCode();
        return x == -1 ? -2 : x;
    }

    // protected PyObject __reduce__(PyObject ignored) {
    // return Py_BuildValue("N(OO)",
    // _PyEval_GetBuiltinId(ID.getattr), self,
    // PyDescr_NAME(descr));
    // }

    // Compare CPython wrapper_call in descrobject.c
    @Override
    public Object __call__(Object[] args, String[] names)
            throws Throwable {
        return descr.callWrapped(self, args, names);
    }
}
