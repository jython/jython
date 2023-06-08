// Copyright (c)2023 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.core;

import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.Map;

import org.python.core.Exposed.FrozenArray;
import org.python.core.Exposed.Member;
import org.python.core.PyType.Flag;

/**
 * Python {@code function} object as created by a function
 * definition and subsequently called.
 *
 * @param <C> implementing class of {@code code} object
 */
public abstract class PyFunction<C extends PyCode> extends AbstractPyObject
        implements DictPyObject {

    /** The type of Python object this class implements. */
    static final PyType TYPE = PyType
            .fromSpec(new PyType.Spec("function", MethodHandles.lookup()).flagNot(Flag.BASETYPE));

    /**
     * The interpreter that defines the import context. Not
     * {@code null}.
     */
    final Interpreter interpreter;

    /**
     * Get the interpreter that defines the import context, which was
     * current when this function was defined. Not {@code null}.
     *
     * @return interpreter that defines the import context
     */
    Interpreter getInterpreter() { return interpreter; }

    /**
     * The {@code __code__} attribute: a code object, which is writable,
     * but only with the right implementation type for the concrete
     * class of the function. Not {@code null}.
     */
    protected C code;

    /**
     * The read-only {@code __globals__} attribute is a {@code dict}:
     * other mappings won't do. Not {@code null}.
     */
    @Member(value = "__globals__", readonly = true)
    final PyDict globals;

    /**
     * The read-only {@code __builtins__} attribute is often a
     * {@code dict} but may be any object. It will be accessed using the
     * Python mapping protocol by the interpreter, at which point an
     * error may be raised. Not {@code null}.
     */
    @Member(value = "__builtins__", readonly = true)
    final Object builtins;

    /** The (positional) {@code __defaults__} or {@code null}. */
    protected Object[] defaults;

    /** The {@code __kwdefaults__} or {@code null}. */
    protected PyDict kwdefaults;

    /**
     * The read-only {@code __closure__} attribute, or {@code null}. See
     * {@link #setClosure(Collection) __closure__} access method
     */
    @FrozenArray
    protected PyCell[] closure;

    /**
     * The {@code __doc__} attribute, can be set to anything or
     * {@code null}.
     */
    // (but only a str prints in help)
    @Member("__doc__")
    Object doc;

    /** The function name ({@code __name__} attribute). */
    String name;

    /**
     * The {@code __dict__} attribute, a {@code dict} or {@code null}.
     */
    private PyDict dict;

    /**
     * The {@code __module__} attribute, can be anything or {@code null}
     * meaning {@code None}
     */
    @Member(value = "__module__")
    Object module;

    /**
     * The {@code __annotations__} attribute, a {@code dict} or
     * {@code null}.
     */
    PyDict annotations;

    /** The function qualified name ({@code __qualname__} attribute). */
    private String qualname;

    /**
     * Create a PyFunction supplying most of the attributes at
     * construction time.
     * <p>
     * The strongly-typed {@code defaults}, {@code kwdefaults},
     * {@code annotations} and {@code closure} may be {@code null} if
     * they would otherwise be empty. {@code annotations} is always
     * exposed as a {@code dict}, but may be presented to the
     * constructor as a {@code dict} or {@code tuple} of keys and values
     * (or {@code null}).
     *
     * @implNote We differ from CPython in requiring a reference to the
     *     interpreter as an argument. Also, we favour a constructor in
     *     which the attributes are supplied {@code defaults},
     *     {@code kwdefaults}, {@code annotations} and {@code closure}
     *     rather than added after construction.
     *
     * @param interpreter providing the module context not {@code null}
     * @param code to execute not {@code null}
     * @param globals name space to treat as global variables not
     *     {@code null}
     * @param defaults default positional argument values or
     *     {@code null}
     * @param kwdefaults default keyword argument values or {@code null}
     * @param annotations type annotations ({@code dict}, {@code null}
     *     or maybe {@code tuple})
     * @param closure variables referenced but not defined here, must be
     *     size expected by code or {@code null} if empty.
     */
    // Compare CPython PyFunction_NewWithQualName in funcobject.c
    PyFunction(Interpreter interpreter, C code, PyDict globals, Object[] defaults,
            PyDict kwdefaults, Object annotations, PyCell[] closure) {
        super(TYPE);

        // We differ from CPython in requiring this reference
        this.interpreter = interpreter;
        assert interpreter != null;

        this.globals = globals;
        this.name = code.name;
        this.qualname = code.qualname;

        // Get __doc__ from first constant in code (if str)
        Object doc;
        Object[] consts = code.consts;
        if (consts.length >= 1 && PyUnicode.TYPE.check(doc = consts[0]))
            this.doc = doc;
        else
            this.doc = Py.None;

        // __module__ = globals['__name__'] or null.
        this.module = globals.get("__name__");
        this.builtins = getBuiltinsFromGlobals();

        // We differ from CPython in having these in construction
        this.defaults = defaults;
        this.kwdefaults = kwdefaults;
        this.closure = closure;
        this.annotations = null; // XXX process argument instead
        this.code = code; // XXX validate
    }

    /**
     * Look in {@code __globals__} then the {@code interpreter} to find
     * the container of built-in objects.
     *
     * @return the {@code __builtins__} of the function
     */
    // Compare CPython _PyEval_BuiltinsFromGlobals in frameobject.c
    private Object getBuiltinsFromGlobals() {
        Object builtins = globals.get("__builtins__");
        if (builtins != null) {
            if (PyModule.TYPE.check(builtins)) { return ((PyModule)builtins).getDict(); }
            return builtins;
        }
        /*
         * Difference from CPython: this is always known and will be used by
         * the frame created by a call, not the builtins of a previous
         * frame.
         */
        return interpreter.builtinsModule.dict;
    }

    /**
     * Create a {@code PyFrame} that will execute this
     * {@code PyFunction}. This frame should be "loose":
     * {@link PyFrame#back} should be {@code null} and it should not be
     * on any thread's stack.
     *
     * @param locals name space to treat as local variables
     * @return the frame
     */
    abstract PyFrame<? extends C> createFrame(Object locals);

    // slot methods --------------------------------------------------

    /**
     * Canonical {@code __call__} slot with Jython conventions, making
     * function implementations callable.
     *
     * @param args all the arguments (position then keyword)
     * @param names of the keyword arguments (or {@code null})
     * @return the return from the call
     * @throws Throwable for errors raised in the function
     */
    abstract Object __call__(Object[] args, String[] names) throws Throwable;

    @SuppressWarnings("unused")
    private Object __repr__() { return toString(); }

    @SuppressWarnings("unused")
    private Object __str__() { return toString(); }

    // plumbing ------------------------------------------------------

    @Override
    public Map<Object, Object> getDict() { return dict; }

    @Override
    public PyType getType() { return TYPE; }

    @Override
    // Compare CPython func_repr in funcobject.c
    public String toString() {
        return String.format("<function %.100s at %#x>", qualname, Py.id(this));
    }
}
