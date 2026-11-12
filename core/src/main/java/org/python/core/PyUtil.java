// Copyright (c)2026 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.core;

import java.lang.invoke.MethodHandle;
import java.util.function.Function;
import java.util.function.Supplier;

import org.python.internal.EmptyException;
import org.python.kernel.BaseType;
import org.python.kernel.ReplaceableType;
import org.python.kernel.Representation;
import org.python.types.WithClassAssignment;

/**
 * Miscellaneous static helpers commonly needed to implement Python
 * objects in Java. Methods from this class rely on the Python type
 * system, so should be used only after the core types are Java-ready
 * (or with great care before that).
 */
public class PyUtil {

    private PyUtil() {} // no instances

    /**
     * Convenient default {@code toString} implementation that normally
     * calls {@code __str__}, to provide a familiar-looking output. The
     * values we see in Java debugging are often the {@code toString()} of
     * an object, so we produce a {@code String}, even from broken or
     * partly-ready types. Use as:<pre>
     * public String toString() {
     *     return PyUtil.defaultToString(this);
     * }</pre>
     * <p>
     * When using this method to define {@code toString()}, do not also
     * define {@code __str__} or {@code __repr__} in terms of
     * {@code toString()}, as this will create a loop.
     * <p>
     * The following sequence of delegations operates in Python, when the
     * built in function {@code str()} is called:
     * <ol>
     * <li>{@code str(x)} returns {@code x} if {@code X=type(x)} is exactly
     * {@code str}.</li>
     * <li>{@code str(x)} calls {@code X.__str__}, if it is not empty, or
     * defaults to {@code repr(x)}.</li>
     * <li>{@code X.__str__} may be the inherited
     * {@code object.__str__}.</li>
     * <li>{@code object.__str__(x)} calls {@code X.__repr__(x)}, if it is
     * not empty, or defaults to {@code object.__repr__(x)}.</li>
     * </ol>
     * The following sequence of delegations operates in Python, when the
     * built in function {@code repr()} is called:
     * <ol>
     * <li>{@code repr(x)} calls {@code X.__repr__(x)}, where
     * {@code X=type(x)}, if it is not empty, or defaults to a formula
     * {@code "<X object at A>"} formed from the type and the hex identity
     * of {@code x}.</li>
     * <li>{@code X.__repr__} may be the inherited
     * {@code object.__repr__}.</li>
     * <li>{@code object.__repr__(x)} returns a string like
     * {@code "<M.X object at A>"} formed from the module (if any and not
     * {@code builtins}), the type and the hex identity of {@code x}.</li>
     * </ol>
     * When we write that a special method is "empty" here, we mean that it
     * throws {@link EmptyException} when called. In CPython, the type slot
     * would be {@code NULL}, but we don't use that convention.
     *
     * @param o object to represent
     * @return a string representation
     */
    public static String defaultToString(Object o) {

        if (o == null) { return "<null>"; }

        String name = null;
        if (TypeSystem.systemReady()) {
            Representation rep = null;
            try {
                rep = Abstract.representation(o);
                MethodHandle str = rep.op_str();
                Object r = str.invokeExact(o);
                return r.toString();
            } catch (Throwable e) {}

            // o.__str__ not working. Emulate object.__repr__.
            try {
                // Has o a Python type at all?
                name = rep.pythonType(o).getName();
            } catch (Throwable e) {}
        }

        if (name == null) {
            // Maybe type system broken or not ready. Use Java name.
            Class<?> c = o.getClass();
            if (c.isAnonymousClass()) {
                name = c.getName();
            } else {
                name = c.getSimpleName();
            }
        }

        // Produce something like object.__repr__(o)
        return String.format("<%s object at %#x>", name, Py.id(o));
    }

    /**
     * Return an arbitrary throwable wrapped (if necessary) as a Python
     * exception, in the context of an implementation of {@code __new__}.
     * The special method {@code base.__new__(cls, ...)} has been called,
     * that is, the type {@code base} is requested to create an instance of
     * type {@code cls}, and this has failed for the reason given as a
     * {@code Throwable}.
     * <p>
     * This is a convenience method when implementing an exposed
     * {@code __new__} in a built-in (or extension) type. If the reason is
     * not already a Python exception, we return a {@link PyBaseException
     * TypeError} with a message along the lines
     * {@code "Cannot construct instance of 'CLS' in %BASE.__new__ "}, and a
     * the cause embedded with {@link Throwable#initCause(Throwable)}.
     *
     * @param cls of which an instance is required
     * @param base that provides the {@code __new__} method.
     * @param cause resulting from the attempt to construct the object
     * @return a Python exception reporting the failure.
     */
    public static PyBaseException cannotConstructInstance(PyType cls, PyType base,
            Throwable cause) {
        if (cause instanceof PyBaseException e) {
            // Usually signals no matching constructor
            return e;
        } else {
            // Failed while finding/invoking constructor
            PyBaseException err = PyErr.format(PyExc.TypeError, CANNOT_CONSTRUCT_INSTANCE,
                    cls.getName(), base.getName());
            err.initCause(cause);
            return err;
        }
    }

    private static final String CANNOT_CONSTRUCT_INSTANCE =
            "Cannot construct instance of '%s' in %s.__new__ ";

    // Some singleton exceptions --------------------------------------

    /**
     * The type of exception thrown when an attempt to convert an object to
     * a common data type fails. This type of exception carries no stack
     * context, since it is used only as a sort of "alternative return
     * value".
     */
    public static class NoConversion extends Exception {
        private static final long serialVersionUID = 1L;

        private NoConversion() { super(null, null, false, false); }
    }

    /**
     * A statically allocated {@link NoConversion} used in conversion
     * methods to signal "cannot convert". No stack context is preserved in
     * the exception.
     */
    public static final NoConversion NO_CONVERSION = new NoConversion();

    // Helpers for methods and attributes -----------------------------

    /**
     * Return a default value if {@code v} is {@code null}. This may be used
     * a wrapper on an expression typically to return a field during
     * attribute access when "not set" should be represented to Python.
     *
     * @param <T> type of {@code v}
     * @param v to return if not {@code null}
     * @param defaultValue to return if {@code v} is {@code null}
     * @return {@code v} or {@code defaultValue}
     */
    static <T> T defaultIfNull(T v, T defaultValue) { return v != null ? v : defaultValue; }

    /**
     * Return the argument if it is not {@code null}, or {@code None} if it
     * is.
     *
     * @param o object to return is not {@code null}
     * @return {@code o} or {@code None} if {@code o} was {@code null}.
     */
    static Object noneIfNull(Object o) { return o == null ? Py.None : o; }

    /**
     * Throw an exception if {@code v} is {@code null}.
     *
     * @param <T> type of {@code v}
     * @param <E> type of exception to throw
     * @param v to return if not {@code null}
     * @param exc supplier of exception to throw
     * @return {@code v}
     * @throws E if {@code v} is {@code null}
     */
    static <T, E extends PyBaseException> T errorIfNull(T v, Supplier<E> exc) throws E {
        if (v != null) { return v; }
        throw exc.get();
    }

    /**
     * Present an array as a tuple, or if the expression variable is
     * {@code null}, as a Python {@code None}.
     *
     * @param <E> element type of the array
     * @param a array providing elements or {@code null}
     * @return tuple from argument array or {@code None} if the array was
     *     Java {@code null}.
     */
    public static <E> Object tupleOrNone(E[] a) { return a == null ? Py.None : new PyTuple(a); }

    /**
     * Return {@code v} if it is of the expected Python type, otherwise
     * throw supplied exception.
     *
     * @param <T> type of {@code v}
     * @param <E> type of exception to throw
     * @param v to return if of expected type
     * @param type expected
     * @param exc supplier of exception to throw
     * @return {@code v}
     * @throws E if {@code v} is not of expected type
     */
    public static <T, E extends PyBaseException> T typeChecked(T v, PyType type,
            Function<T, E> exc) {
        if (type.check(v)) { return v; }
        throw exc.apply(v);
    }

    /**
     * A basic check for use during {@code __class__} assignment (that is,
     * during the implementation of
     * {@link WithClassAssignment#setType(Object)}), or in a constructor,
     * that the type being assigned could be acceptable as the Python type.
     * It is also the default implementation of
     * {@link WithClassAssignment#checkClassAssignment(Object)}, which
     * passes the existing type as first argument.
     * <p>
     * This method checks that the proposed replacement type and existing
     * type specify the same representation class for their instances, and
     * will raise a TypeError with a helpful message otherwise.
     *
     * @param type of the target of the change of Python class
     * @param replacementType intended new type
     * @return replacement type cast to {@link ReplaceableType}
     * @throws PyBaseException if replacement is unacceptable
     */
    public static PyType checkReplaceable(PyType type, Object replacementType)
            throws PyBaseException {
        if (replacementType instanceof ReplaceableType t) {
            // Require same primary class for old and new types
            if (type.javaClass() == t.javaClass()) { return t; }
        }
        // Failing to assign for some reason we will now work out
        throw classAssignmentError(type, replacementType);
    }

    /**
     * A basic check for use during initial {@code __class__} assignment
     * (that is, in a constructor), that the type being assigned could be
     * acceptable as the Python type. It is part of the default
     * implementation of
     * {@link WithClassAssignment#checkClassAssignment(Object)}, which
     * passes the target class as first argument.
     * <p>
     * During initial assignment (construction), the existing type may be
     * undefined, but we shall have found a {@link Representation} from the
     * class of the target. This method checks that the proposed replacement
     * type and target have the same primary class, and will raise a
     * TypeError with a helpful message otherwise.
     *
     * @param c class of the target of the change of Python class
     * @param replacementType intended new type
     * @return replacement type cast to {@link ReplaceableType}
     * @throws PyBaseException if replacement is unacceptable
     */
    public static PyType checkReplaceable(Class<?> c, Object replacementType)
            throws PyBaseException {
        // c may be a subclass representation: need primary
        Representation rep = TypeSystem.registry.get(c);
        if (replacementType instanceof ReplaceableType t) {
            // Require same primary class for old and new types
            if (rep.javaClass() == t.javaClass()) { return t; }
        }
        // Failing to assign for some reason we will now work out
        throw classAssignmentError(rep, replacementType);
    }

    /**
     * Helper to {@code checkClassAssignment} returning a {@code TypeError}
     * to throw.
     *
     * @param type of the target of the change of Python class
     * @param replacementType intended new type
     * @return {@code TypeError} to throw
     */
    private static PyBaseException classAssignmentError(PyType type, Object replacementType) {
        String msg;
        if (replacementType == null) {
            msg = "__class__ attribute cannot be deleted";
        } else if (replacementType instanceof BaseType t) {
            // but type.javaClass() != t.javaClass()
            msg = String.format(
                    "__class__ assignment: '%s' representation differs from that of '%s'",
                    t.getName(), type.getName());
        } else {
            // Possibly PyType but doesn't count if not made by us.
            msg = String.format("__class__ must be set to a class, not a '%s' object",
                    PyType.of(replacementType).getName());
        }
        return PyErr.format(PyExc.TypeError, msg);
    }

    /**
     * Helper to {@code checkClassAssignment} returning a {@code TypeError}
     * to throw. During initial assignment (construction), the existing type
     * may be undefined, but we shall have found a {@link Representation}
     * from the class of the target.
     *
     * @param rep representation or type of the target of the change of
     *     Python class
     * @param replacementType intended new type
     * @return {@code TypeError} to throw
     */
    private static PyBaseException classAssignmentError(Representation rep,
            Object replacementType) {
        String msg;
        if (replacementType == null) {
            msg = "__class__ attribute cannot be deleted";
        } else if (replacementType instanceof BaseType t) {
            // but rep.javaClass() != t.javaClass()
            String name;
            if (rep instanceof PyType type) {
                name = type.getName();
            } else {
                name = rep.toString();
            }
            msg = String.format(
                    "__class__ assignment: '%s' representation differs from that of '%s'",
                    t.getName(), name);
        } else {
            // Possibly PyType but doesn't count if not made by us.
            msg = String.format("__class__ must be set to a class, not a '%s' object",
                    PyType.of(replacementType).getName());
        }
        return PyErr.format(PyExc.TypeError, msg);
    }
}
