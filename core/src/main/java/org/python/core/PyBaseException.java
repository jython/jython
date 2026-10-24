// Copyright (c)2026 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.core;

import static org.python.core.ClassShorthand.T;
import static org.python.core.ClassShorthand.TUPLE;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.Map;
import java.util.StringJoiner;

import org.python.types.Exposed;
import org.python.types.Feature;
import org.python.types.TypeSpec;
import org.python.types.WithClassAssignment;
import org.python.types.WithDict;
import org.python.types.Exposed.KeywordCollector;
import org.python.types.Exposed.PositionalCollector;

/**
 * The Python {@code BaseException}, and many common Python exceptions
 * (for example {@code TypeError}), are represented by instances of this
 * Java class. A Java subclass of {@code PyBaseException} is defined
 * only where a Python exception subclass adds fields to its parent.
 * <p>
 * The Python type of the exception is represented as a field (see
 * {@link #getType()}). The Python type is re-writable through
 * {@link #setType(Object)} with types represented by the same Java
 * class (which obviously cannot change).
 * <p>
 * CPython prohibits class-assignment involving built-in types directly.
 * For example {@code FloatingPointError().__class__ = E} and its
 * converse are not allowed. There seems to be no structural reason to
 * prohibit it, but we do so for compatibility.
 * <p>
 * The implementation follows CPython closely, where the implementation
 * of many exception types is shared with multiple others. This allows
 * multiple inheritance and class assignment amongst user-defined
 * exceptions with diverse built-in bases, in ways that may be
 * surprising. The following is discouraged but valid in Python: <pre>
 * class TE(TypeError): __slots__=()
 * class FPE(FloatingPointError): __slots__=()
 * TE().__class__ = FPE
 * class E(ZeroDivisionError, TypeError): __slots__=()
 * E().__class__ = FPE
 * </pre>In order to meet expectations set by CPython, the Java
 * representation in Java is correspondingly shared. For example
 * {@code TypeError}, {@code FloatingPointError} and
 * {@code ZeroDivisionError} must share a representation. In fact they
 * are defined in this class, to share the representation of
 * {@code BaseException}.
 * <p>
 * It follows that we cannot generally use the exception class in a Java
 * {@code catch} clause to select amongst Python exceptions. We must
 * catch the representation class of the intended Python exception, and
 * re-throw it if it does not match the intended Python type. Method
 * {@link #only(PyType)} is provided to make this simpler.
 *
 * @implNote It would have been convenient, when catching exceptions in
 *     Java, if the different classes of Python exception could have
 *     been distinct classes in Java. This is not possible. User-defined
 *     exceptions extending different built-in exceptions allow class
 *     assignment even when they have distinct bases. It follows that
 *     all built-in exception types where this could happen must have
 *     the same representation class in Java.
 */
// Compare CPython PyBaseExceptionObject in pyerrors.c
public class PyBaseException extends RuntimeException implements WithClassAssignment, WithDict {

    /** Allow the type system package access. */
    private static final MethodHandles.Lookup LOOKUP =
            MethodHandles.lookup().dropLookupMode(MethodHandles.Lookup.PRIVATE);

    /** The type object of Python {@code BaseException} exceptions. */
    public static final PyType TYPE = PyType.fromSpec( //
            new TypeSpec("BaseException", LOOKUP) //
                    .add(Feature.REPLACEABLE, Feature.IMMUTABLE) //
                    .doc("Common base class for all exceptions"));

    /** Python type of the exception. */
    private PyType type;

    /** The dictionary of associated values on the instance. */
    // XXX dictionary required
    Map<Object, Object> dict;

    /**
     * The arguments given to the constructor, which is also the arguments
     * from {@code __new__} or {@code __init__}. Not {@code null}.
     */
    PyTuple args;

    /**
     * A list of the notes added to this exception. Exposed as
     * {@code __notes__}.
     */
    private Object notes;
    /**
     * A writable field that holds the traceback object associated with this
     * exception. Exposed as {@code __traceback__}.
     */
    private Object traceback;
    /**
     * When raising a new exception while another exception is already being
     * handled, the new exception’s {@code __context__} attribute is
     * automatically set to the handled exception.
     */
    private Object context;
    /**
     * The exception following {@code raise ... from}. Exposed as
     * {@code __cause__}.
     */
    private Object cause;
    /**
     * Set to suppress reporting {@code __context__}. Exposed as
     * {@code __suppress_context__}.
     */
    private boolean suppressContext;

    /**
     * Constructor specifying Python type and the argument tuple as the
     * associated value of the exception. We do this for maximum similarity
     * with CPython, where {@code __new__} does no more than allocate an
     * object and all attribute values are decoded by {@code __init__}.
     * <p>
     * The type of an exception may be changed, within limits. The initial
     * {@code type} is checked to see that it shares this class as its
     * representation. E.g. {@code UnboundLocalError} and {@code NameError}
     * share the representation created by {@link PyNameError#TYPE}. Once
     * the object is created, {@link #setType(Object)} makes effectively the
     * same check.
     *
     * @param type Python type of the exception
     * @param args positional arguments
     */
    public PyBaseException(PyType type, PyTuple args) {
        // Ensure Python type is compatible with type family.
        this.type = PyUtil.checkReplaceable(this.getClass(), type);
        this.args = args;
    }

    // WithDict interface --------------------------------------------

    @Override
    public Map<Object, Object> getDict() {
        if (dict == null) { dict = new PyDict(); }
        return dict;
    }

    // WithClassAssignment interface ---------------------------------

    @Override
    public PyType getType() { return type; }

    @Override
    public void setType(Object replacementType) {
        type = PyUtil.checkReplaceable(getType(), replacementType);
    }

    // Exception API -------------------------------------------------

    /**
     * If the Python type of this exception is not the {@code wanted} type,
     * immediately re-throw it, with its original stack trace. This may be
     * used at the top of a catch clause to narrow the caught exception
     * almost as if it were a Java type. For example:<pre>
     *     try {
     *         return PyLong.asSize(value);
     *     } catch (PyBaseException e) {
     *         // Re-raise everything that is not OverflowError
     *         e.only(PyExc.OverflowError);
     *         // ... handle overflow here.
     *     }
     * </pre>
     *
     * @param wanted type for which the method returns normally
     */
    public void only(PyType wanted) {
        // TODO Should be isinstance test, accepting sub-types
        if (type != wanted) { throw this; }
    }

    /**
     * If the Python type of this exception is not one of the {@code wanted}
     * types, immediately re-throw it, with its original stack trace. This
     * may be used at the top of a catch clause to narrow the caught
     * exception almost as if it were a Java type.
     *
     * @param wanted type for which the method returns normally
     */
    public void only(PyType... wanted) {
        // TODO Should be isinstance test, accepting sub-types
        for (PyType w : wanted) {
            // TODO Should be isinstance test, accepting sub-types
            if (type == w) { return; }
        }
        throw this;
    }

    @Override
    public String getMessage() { return args.size() > 0 ? PyUnicode.asString(args.get(0)) : ""; }

    @Override
    public String toString() { return String.format("%s: %s", type.getName(), getMessage()); }

    // Special methods -----------------------------------------------

    // Compare CPython BaseException_* in exceptions.c

    /**
     * Create a new instance of the specified a Python exception class
     * {@code type}, which must be {@code BaseException} or a subclass of
     * it. The returned object is an instance of the Java representation
     * class of {@code type}.
     *
     * @param cls actual Python sub-class being created
     * @param args positional arguments
     * @param kwargs keywords (ignored)
     * @return newly-created object
     */
    @Exposed.PythonNewMethod
    static Object __new__(PyType cls, @PositionalCollector PyTuple args,
            @KeywordCollector PyDict kwargs) {
        assert cls.isSubTypeOf(TYPE);
        try {
            // Look up a constructor with the right parameters
            MethodHandle cons = cls.constructor(T, TUPLE).handle();
            return cons.invokeExact(cls, args);
        } catch (Throwable e) {
            throw PyUtil.cannotConstructInstance(cls, TYPE, e);
        }
    }

    /**
     * Initialise this instance.
     *
     * @param args values to set as the attribute {@code args}
     * @param kwds keywords
     */
    void __init__(Object[] args, String[] kwds) {
        if (kwds == null || kwds.length == 0) {
            this.args = PyTuple.of(args);
        } else {
            throw PyErr.format(PyExc.TypeError, "%s() takes no keyword arguments",
                    getType().getName());
        }
    }

    /**
     * @return {@code str()} of this Python object.
     * @throws Throwable from getting the {@code str()} of {@code args}
     */
    Object __str__() throws Throwable {
        return switch (args.size()) {
            case 0 -> "";
            case 1 -> Abstract.str(args.get(0));
            default -> Abstract.str(args);
        };
    }

    /**
     * @return {@code repr()} of this Python object.
     * @throws Throwable from getting the {@code repr()} of {@code args}
     */
    Object __repr__() throws Throwable {
        String prefix = type.getName() + "(";
        StringJoiner sj = new StringJoiner(",", prefix, ")");
        for (Object o : args) { sj.add(PyUnicode.asString(Abstract.repr(o))); }
        return sj.toString();
    }

    // Python exceptions sharing this representation -----------------

    /**
     * Permit a sub-class to create a type object for a built-in exception
     * that extends a single base, with the addition of no fields or
     * methods, and therefore has the same Java representation as its base.
     *
     * @param excbase the base (parent) exception
     * @param excname the name of the new exception
     * @param excdoc a documentation string for the new exception type
     * @return the type object for the new exception type
     */
    // Compare CPython SimpleExtendsException in exceptions.c
    // ... or (same to us) MiddlingExtendsException
    public static PyType extendsException(PyType excbase, String excname, String excdoc) {
        TypeSpec spec = new TypeSpec(excname, LOOKUP).base(excbase)
                // Share the same Java representation class as base
                .primary(excbase.javaClass())
                // This will be a replaceable type.
                .add(Feature.REPLACEABLE, Feature.IMMUTABLE).doc(excdoc);
        return PyType.fromSpec(spec);
    }

    /** {@code Exception} extends {@link PyBaseException}. */
    static PyType Exception =
            extendsException(TYPE, "Exception", "Common base class for all non-exit exceptions.");
    /** {@code TypeError} extends {@code Exception}. */
    static PyType TypeError =
            extendsException(Exception, "TypeError", "Inappropriate argument type.");
    /** {@code EOFError} extends {@code Exception}. */
    static PyType EOFError = extendsException(Exception, "EOFError", "Read beyond end of file.");
    /** {@code LookupError} extends {@code Exception}. */
    static PyType LookupError =
            extendsException(Exception, "LookupError", "Base class for lookup errors.");
    /** {@code IndexError} extends {@code LookupError}. */
    static PyType IndexError =
            extendsException(LookupError, "IndexError", "Sequence index out of range.");
    /** {@code ValueError} extends {@link Exception}. */
    static PyType ValueError = extendsException(Exception, "ValueError",
            "Inappropriate argument value (of correct type).");

    /** {@code ArithmeticError} extends {@link Exception}. */
    static PyType ArithmeticError =
            extendsException(Exception, "ArithmeticError", "Base class for arithmetic errors.");
    /** {@code FloatingPointError} extends {@link ArithmeticError}. */
    static PyType FloatingPointError = extendsException(ArithmeticError, "FloatingPointError",
            "Floating point operation failed.");
    /** {@code OverflowError} extends {@link ArithmeticError}. */
    static PyType OverflowError = extendsException(ArithmeticError, "OverflowError",
            "Result too large to be represented.");
    /** {@code ZeroDivisionError} extends {@link ArithmeticError}. */
    static PyType ZeroDivisionError = extendsException(ArithmeticError, "ZeroDivisionError",
            "Second argument to a division or modulo operation was zero.");

    /** {@code SystemError} extends {@link Exception}. */
    static PyType SystemError = extendsException(Exception, "SystemError", """
            Internal error in the Python interpreter.
            Please report this to the Python maintainers, along with the traceback,
            the Python version, and the hardware/OS platform and version.""");

    /*
     * Warnings are Exception objects, but do not get thrown (I think),
     * being used as "categories" in the warnings module.
     */
    /** {@code Warning} extends {@link Exception}. */
    static PyType Warning =
            extendsException(Exception, "Warning", "Base class for warning categories.");
    /** {@code DeprecationWarning} extends {@link Warning}. */
    static PyType DeprecationWarning = extendsException(Warning, "DeprecationWarning",
            "Base class for warnings about deprecated features.");
    /** {@code RuntimeWarning} extends {@link Warning}. */
    static PyType RuntimeWarning = extendsException(Warning, "RuntimeWarning",
            "Base class for warnings about dubious runtime behavior.");

    // plumbing ------------------------------------------------------

    private static final long serialVersionUID = 1L;
}
