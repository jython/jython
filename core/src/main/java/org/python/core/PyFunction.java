// Copyright (c)2026 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.core;

import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.Map;

import org.python.internal._PyUtil;
import org.python.support.InterpreterError;
import org.python.types.Exposed.Getter;
import org.python.types.Exposed.Member;
import org.python.types.Exposed.Setter;
import org.python.types.TypeSpec;
import org.python.types.WithDict;

/**
 * Python {@code function} object as created by a function definition
 * and subsequently called.
 *
 * @param <C> implementing class of {@code code} object
 */
public abstract class PyFunction<C extends PyCode> implements WithDict {

    /** The type of Python object this class implements. */
    static final PyType TYPE = PyType.fromSpec(//
            new TypeSpec("function", MethodHandles.lookup()));

    /**
     * The interpreter that defines the import context. Not {@code null}.
     */
    final Interpreter interpreter;

    /**
     * The {@code __code__} attribute: a code object, which is writable, but
     * only with the right implementation type for the concrete class of the
     * function. Not {@code null}.
     */
    protected C code;

    /**
     * The read-only {@code __globals__} attribute is a {@code dict}: other
     * mappings won't do. Not {@code null}.
     */
    @Member(value = "__globals__", readonly = true)
    final PyDict globals;

    /**
     * The read-only {@code __builtins__} attribute is often a {@code dict}
     * but may be any object. It will be accessed using the Python mapping
     * protocol by the interpreter, at which point an error may be raised.
     * Not {@code null}.
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
     * Create a PyFunction supplying most of the attributes at construction
     * time.
     * <p>
     * The strongly-typed {@code defaults}, {@code kwdefaults},
     * {@code annotations} and {@code closure} may be {@code null} if they
     * would otherwise be empty. {@code annotations} is always exposed as a
     * {@code dict}, but may be presented to the constructor as a
     * {@code dict} or {@code tuple} of keys and values (or {@code null}).
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
     * @param defaults default positional argument values or {@code null}
     * @param kwdefaults default keyword argument values or {@code null}
     * @param annotations type annotations ({@code dict}, {@code null} or
     *     maybe {@code tuple})
     * @param closure variables referenced but not defined here, must be
     *     size expected by code or {@code null} if empty.
     */
    // Compare CPython PyFunction_NewWithQualName in funcobject.c
    PyFunction(Interpreter interpreter, C code, PyDict globals, Object[] defaults,
            PyDict kwdefaults, Object annotations, PyCell[] closure) {
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
        if (annotations != null) { setAnnotations(annotations); }

        // Now we can check the code object against the closure etc.
        this.code = checkFreevars(code);
    }

    /**
     * Look in {@code __globals__} then the {@code interpreter} to find the
     * container of built-in objects.
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
         * Difference from CPython: this is always known and will be used by the
         * frame created by a call, not the builtins of a previous frame.
         */
        return interpreter.builtinsModule.dict;
    }

    /**
     * Create a {@code PyFrame} that will execute this {@code PyFunction}.
     * This frame should be "loose": {@link PyFrame#back} should be
     * {@code null} and it should not be on any thread's stack.
     *
     * @param locals name space to treat as local variables
     * @return the frame
     */
    abstract PyFrame<? extends C> createFrame(Object locals);

    // Attributes ----------------------------------------------------

    /*
     * XXX From Java it would be convenient to have a type-safe signature,
     * possibly returning null, but the signature from Python has to be
     * Object, treating null in Java as Python None (if that's allowed).
     * This would require the get-set descriptor to support conversion in
     * the way member descriptors do for their reference types, probably
     * disabled by "optional=true".
     */

    /**
     * @return the {@code __code__} object of this function.
     */
    @Getter("__code__")
    C getCode() { return code; }

    /**
     * Set the {@code __code__} object of this function.
     *
     * @param code new code object to assign
     */
    @Setter("__code__")
    void setCode(C code) { this.code = checkFreevars(code); }

    /** @return the {@code __name__} attribute. */
    @Getter("__name__")
    String getName() { return name; }

    /**
     * Set the {@code __name__} attribute.
     *
     * @param name for function
     */
    @Setter("__name__")
    void setName(Object name) {
        this.name = PyUnicode.asString(name, v -> _PyUtil.attrMustBeString("__name__", v));
    }

    /** @return the {@code __qualname__}, the qualified name. */
    @Getter("__qualname__")
    String getQualname() { return qualname; }

    /**
     * Set the {@code __qualname__} string.
     *
     * @param qualname for function
     */
    @Setter("__qualname__")
    void setQualname(Object qualname) {
        this.qualname =
                PyUnicode.asString(qualname, v -> _PyUtil.attrMustBeString("__qualname__", v));
    }

    /** @return the positional {@code __defaults__ tuple}. */
    @Getter("__defaults__")
    Object getDefaults() { return tupleOrNone(defaults); }

    /**
     * Set the the positional {@code __defaults__ tuple}.
     *
     * @param defaults to set
     */
    @Setter("__defaults__")
    abstract void setDefaults(PyTuple defaults);

    /**
     * @return {@code __kwdefaults__} or {@code None}.
     */
    @Getter("__kwdefaults__")
    Object getKwdefaults() { return kwdefaults; }

    /**
     * Provide the keyword defaults dictionary. Subsequent changes to the
     * dictionary will affect argument parsing, as required for a Python
     * {@link PyFunction function}. (Concurrent access to the mapping is a
     * client issue.)
     *
     * @param kwdefaults specifying {@code __kwdefaults__}
     */
    @Setter("__kwdefaults__")
    abstract void setKwdefaults(PyDict kwdefaults);

    /**
     * @return the {@code __closure__ tuple} or {@code None}.
     */
    @Getter("__closure__")
    Object getClosure() { return tupleOrNone(closure); }

    /**
     * Set the {@code __closure__} attribute. This is <b>not</b> exposed as
     * a setter method to Python. We use it internally.
     *
     * @param <E> element type
     * @param closure elements with which to populate the closure
     */
    <E> void setClosure(Collection<E> closure) {

        int n = closure == null ? 0 : closure.size();
        int nfree = code.layout().nfreevars();

        if (nfree == 0) {
            if (n == 0)
                this.closure = null;
            else
                throw PyErr.format(PyExc.TypeError, "%s closure must be empty/None", code.name);
        } else {
            if (n == nfree) {
                try {
                    this.closure = closure.toArray(new PyCell[n]);
                } catch (ArrayStoreException e) {
                    // The closure is not a tuple of cells only
                    for (Object o : closure) {
                        if (!(o instanceof PyCell)) {
                            throw Abstract.typeError("closure: expected cell, found %s", o);
                        }
                    }
                    throw new InterpreterError("Failed to make closure from %s", closure);
                }
            } else
                throw PyErr.format(PyExc.ValueError, "%s requires closure of length %d, not %d",
                        code.name, nfree, n);
        }
    }

    /** @return the {@code __dict__} attribute */
    @Getter("__dict__")
    PyDict __dict__() { return dict; }

    /**
     * Set the {@code __dict__} attribute
     *
     * @param dict to set
     */
    @Setter("__dict__")
    void __dict__(PyDict dict) { this.dict = dict; }

    /** @return the {@code __annotations__} attribute */
    @Getter("__annotations__")
    PyDict getAnnotations() {
        if (annotations == null) { annotations = Py.dict(); }
        return annotations;
    }

    /**
     * Set the {@code __annotations__} attribute, which is always exposed as
     * a Python {@code dict}. In certain cases a {@code tuple} may be
     * supplied as the argument.
     *
     * @param anno specifying the annotations.
     */
    @Setter("__annotations__")
    private void setAnnotations(Object anno) {
        if (anno instanceof PyDict) {
            annotations = (PyDict)anno;
        } else if (anno instanceof PyTuple) {
            annotations = ((PyTuple)anno).pairsToDict();
        } else {
            // null or wrong type
            throw _PyUtil.attrMustBe("__annotations__", "a dictionary", anno);
        }
    }

    // Special methods -----------------------------------------------

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
    private Object __repr__() {
        return String.format("<function %.100s at %#x>", qualname, Py.id(this));
    }

    // FastCall support ----------------------------------------------

    // XXX ... is needed.

    // Plumbing ------------------------------------------------------

    @Override
    public Map<Object, Object> getDict() { return dict; }

    @Override
    public PyType getType() { return TYPE; }

    @Override
    // Compare CPython func_repr in funcobject.c
    public String toString() { return PyUtil.defaultToString(this); }

    /**
     * Get the interpreter that defines the import context, which was
     * current when this function was defined. Not {@code null}.
     *
     * @return interpreter that defines the import context
     */
    Interpreter getInterpreter() { return interpreter; }

    /**
     * Check that the number of free variables expected by the given code
     * object matches the length of the existing {@link #closure} (or is
     * zero if {@code closure==null}).
     *
     * @param c object to test (not {@code null}).
     * @return {@code c}
     */
    protected C checkFreevars(C c) {
        PyUtil.errorIfNull(c,
                () -> PyErr.format(PyExc.TypeError, "__code__ must be set to a code object"));
        int nfree = c.layout().nfreevars();
        int nclosure = closure == null ? 0 : closure.length;
        if (nclosure != nfree) {
            throw PyErr.format(PyExc.ValueError, FREE_VARS, name, nclosure, nfree);
        }
        return c;
    }

    private static String FREE_VARS = "%s() requires a code object with %d free vars, not %d";

    /**
     * Present an array as a tuple, or if the expression variable is
     * {@code null}, as a Python {@code None}.
     *
     * @param <E> element type of the array
     * @param a array providing elements or {@code null}
     * @return tuple from argument array or {@code None} if the array was
     *     Java {@code null}.
     */
    protected static <E> Object tupleOrNone(E[] a) {
        return a == null ? Py.None : PyTuple.from(a);
    }
}
