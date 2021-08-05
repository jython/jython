// Copyright (c)2021 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.core;

import java.lang.invoke.MethodHandle;
import java.util.function.Function;

import org.python.core.Slot.EmptyException;

/**
 * Abstract API for operations on numeric types, corresponding to
 * CPython methods defined in {@code abstract.h} and with names
 * like: {@code PyNumber_*}.
 */
public class PyNumber extends Abstract {

    private PyNumber() {} // only static methods here

    /**
     * {@code -v}: unary negative with Python semantics.
     *
     * @param v operand
     * @return {@code -v}
     * @throws Throwable from invoked implementations
     */
    public static Object negative(Object v) throws Throwable {
        try {
            return Operations.of(v).op_neg.invokeExact(v);
        } catch (Slot.EmptyException e) {
            throw operandError(Slot.op_neg, v);
        }
    }

    /**
     * {@code abs(v)}: absolute value with Python semantics.
     *
     * @param v operand
     * @return {@code -v}
     * @throws Throwable from invoked implementations
     */
    public static Object absolute(Object v) throws Throwable {
        try {
            return Operations.of(v).op_abs.invokeExact(v);
        } catch (Slot.EmptyException e) {
            throw operandError(Slot.op_abs, v);
        }
    }

    /**
     * Create a {@code TypeError} for a named unary operation, along the
     * lines "bad operand type for OP: 'T'".
     *
     * @param op operation to report
     * @param v actual operand (only {@code PyType.of(v)} is used)
     * @return exception to throw
     */
    static PyException operandError(Slot op, Object v) {
        return new TypeError("bad operand type for %s: '%.200s'", op.opName,
                PyType.of(v).getName());
    }

    /**
     * {@code v + w} with Python semantics.
     *
     * @param v left operand
     * @param w right operand
     * @return {@code v + w}
     * @throws Throwable from invoked implementations
     */
    public static Object add(Object v, Object w) throws Throwable {
        return binary_op(v, w, Slot.op_add);
    }

    /**
     * {@code v - w} with Python semantics.
     *
     * @param v left operand
     * @param w right operand
     * @return {@code v - w}
     * @throws Throwable from invoked implementations
     */
    public static Object subtract(Object v, Object w) throws Throwable {
        return binary_op(v, w, Slot.op_sub);
    }

    /**
     * {@code v * w} with Python semantics.
     *
     * @param v left operand
     * @param w right operand
     * @return {@code v * w}
     * @throws Throwable from invoked implementations
     */
    public static Object multiply(Object v, Object w) throws Throwable {
        return binary_op(v, w, Slot.op_mul);
    }

    /**
     * {@code v | w} with Python semantics.
     *
     * @param v left operand
     * @param w right operand
     * @return {@code v | w}
     * @throws Throwable from invoked implementations
     */
    static final Object or(Object v, Object w) throws Throwable {
        return binary_op(v, w, Slot.op_or);
    }

    /**
     * {@code v & w} with Python semantics.
     *
     * @param v left operand
     * @param w right operand
     * @return {@code v & w}
     * @throws Throwable from invoked implementations
     */
    static final Object and(Object v, Object w) throws Throwable {
        return binary_op(v, w, Slot.op_and);
    }

    /**
     * {@code v ^ w} with Python semantics.
     *
     * @param v left operand
     * @param w right operand
     * @return {@code v ^ w}
     * @throws Throwable from invoked implementations
     */
    static final Object xor(Object v, Object w) throws Throwable {
        return binary_op(v, w, Slot.op_xor);
    }

    /**
     * Helper for implementing a binary operation that has one,
     * slot-based interpretation.
     *
     * @param v left operand
     * @param w right operand
     * @param binop operation to apply
     * @return result of operation
     * @throws TypeError if neither operand implements the operation
     * @throws Throwable from the implementation of the operation
     */
    private static Object binary_op(Object v, Object w, Slot binop) throws TypeError, Throwable {
        try {
            Object r = binary_op1(v, w, binop);
            if (r != Py.NotImplemented) { return r; }
        } catch (Slot.EmptyException e) {}
        throw operandError(binop, v, w);
    }

    /**
     * Helper for implementing binary operation. If neither the left
     * type nor the right type implements the operation, it will either
     * return {@link Py#NotImplemented} or throw {@link EmptyException}.
     * Both mean the same thing.
     *
     * @param v left operand
     * @param w right operand
     * @param binop operation to apply
     * @return result or {@code Py.NotImplemented}
     * @throws Slot.EmptyException when an empty slot is invoked
     * @throws Throwable from the implementation of the operation
     */
    private static Object binary_op1(Object v, Object w, Slot binop)
            throws Slot.EmptyException, Throwable {

        Operations vOps = Operations.of(v);
        PyType vtype = vOps.type(v);

        Operations wOps = Operations.of(w);
        PyType wtype = wOps.type(w);

        MethodHandle slotv, slotw;

        /*
         * CPython would also test: (slotw = rbinop.getSlot(wtype)) == slotv
         * as an optimisation , but that's never the case since we use
         * distinct binop and rbinop slots.
         */
        if (wtype == vtype) {
            // Same types so only try the binop slot
            slotv = binop.getSlot(vOps);
            return slotv.invokeExact(v, w);

        } else if (!wtype.isSubTypeOf(vtype)) {
            // Ask left (if not empty) then right.
            slotv = binop.getSlot(vOps);
            if (slotv != BINARY_EMPTY) {
                Object r = slotv.invokeExact(v, w);
                if (r != Py.NotImplemented) { return r; }
            }
            slotw = binop.getAltSlot(wOps);
            return slotw.invokeExact(w, v);

        } else {
            // Right is sub-class: ask first (if not empty).
            slotw = binop.getAltSlot(wOps);
            if (slotw != BINARY_EMPTY) {
                Object r = slotw.invokeExact(w, v);
                if (r != Py.NotImplemented) { return r; }
            }
            slotv = binop.getSlot(vOps);
            return slotv.invokeExact(v, w);
        }
    }

    private static final MethodHandle BINARY_EMPTY = Slot.Signature.BINARY.empty;

    /**
     * Return a Python {@code int} (or subclass) from the object
     * {@code o}. Raise {@code TypeError} if the result is not a Python
     * {@code int} subclass, or if the object {@code o} cannot be
     * interpreted as an index (it does not fill {@link Slot#op_index}).
     * This method makes no guarantee about the <i>range</i> of the
     * result.
     *
     * @param o operand
     * @return {@code o} coerced to a Python {@code int}
     * @throws TypeError if {@code o} cannot be interpreted as an
     *     {@code int}
     * @throws Throwable otherwise from invoked implementations
     */
    // Compare with CPython abstract.c :: PyNumber_Index
    static Object index(Object o) throws TypeError, Throwable {

        Operations ops = Operations.of(o);
        Object res;

        if (ops.isIntExact())
            return o;
        else {
            try {
                res = ops.op_index.invokeExact(o);
                // Enforce expectations on the return type
                Operations resOps = Operations.of(res);
                if (resOps.isIntExact())
                    return res;
                else if (resOps.type(res).isSubTypeOf(PyLong.TYPE))
                    return returnDeprecation("__index__", "int", res);
                else
                    throw returnTypeError("__index__", "int", res);
            } catch (EmptyException e) {
                throw typeError(CANNOT_INTERPRET_AS_INT, o);
            }
        }
    }

    /**
     * Returns {@code o} converted to a Java {@code int} if {@code o}
     * can be interpreted as an integer. If the call fails, an exception
     * is raised, which may be a {@link TypeError} or anything thrown by
     * {@code o}'s implementation of {@code __index__}. In the special
     * case of {@link OverflowError}, a replacement may be made where
     * the message is formulated by this method and the type of
     * exception by the caller. (Arcane, but it's what CPython does.) A
     * recommended idiom for this is<pre>
     *      int k = Number.asSize(key, IndexError::new);
     * </pre>
     *
     * @param o the object to convert to an {@code int}
     *
     * @param exc {@code null} or function of {@code String} returning
     *     the exception to use for overflow.
     * @return {@code int} value of {@code o}
     * @throws TypeError if {@code o} cannot be converted to a Python
     *     {@code int}
     * @throws Throwable on other errors
     */
    // Compare with CPython abstract.c :: PyNumber_AsSsize_t
    static int asSize(Object o, Function<String, PyException> exc) throws TypeError, Throwable {

        // Convert to Python int or sub-class. (May raise TypeError.)
        Object value = PyNumber.index(o);

        try {
            // We're done if PyLong.asSize() returns without error.
            return PyLong.asSize(value);
        } catch (OverflowError e) {
            // Caller may replace overflow with own type of exception
            if (exc == null) {
                // No handler: default clipping is sufficient.
                assert PyType.of(value).isSubTypeOf(PyLong.TYPE);
                if (PyLong.signum(value) < 0)
                    return Integer.MIN_VALUE;
                else
                    return Integer.MAX_VALUE;
            } else {
                // Throw an exception of the caller's preferred type.
                String msg = String.format(CANNOT_FIT, PyType.of(o).getName());
                throw exc.apply(msg);
            }
        } catch (TypeError e) {
            // Formally necessary but index() guarantees never reached
            return 0;
        }
    }

    /**
     * Returns the {@code o} converted to an integer object. This is the
     * equivalent of the Python expression {@code int(o)}. It will refer
     * to the {@code __int__}, {@code __index_} and {@code __trunc__}
     * special methods of {@code o}, in that order, an then (if
     * {@code o} is string or bytes-like) attempt a conversion from text
     * assuming decimal base.
     *
     * @param o operand
     * @return {@code int(o)}
     * @throws TypeError if {@code o} cannot be converted to a Python
     *     {@code int}
     * @throws Throwable on other errors
     */
    // Compare with CPython abstract.h :: PyNumber_Long
    static Object asLong(Object o) throws TypeError, Throwable {
        Object result;
        PyType oType = PyType.of(o);

        if (oType == PyLong.TYPE) {
            // Fast path for the case that we already have an int.
            return o;
        }

        else if (Slot.op_int.isDefinedFor(oType)) {
            // XXX Need test of intiness and indexiness?
            // Normalise away subclasses of int
            result = PyLong.fromIntOf(o);
            return PyLong.from(result);
        }

        else if (Slot.op_index.isDefinedFor(oType)) {
            // Normalise away subclasses of int
            result = PyLong.fromIndexOrIntOf(o);
            return PyLong.from(result);
        }

        // XXX Not implemented: else try the __trunc__ method

        if (PyUnicode.TYPE.check(o))
            return PyLong.fromUnicode(o, 10);

        // else if ... support for bytes-like objects
        else
            throw argumentTypeError("int", 0, "a string, a bytes-like object or a number", o);
    }

    private static final String CANNOT_INTERPRET_AS_INT =
            "'%.200s' object cannot be interpreted as an integer";
    private static final String CANNOT_FIT = "cannot fit '%.200s' into an index-sized integer";

    /**
     * Throw a {@code TypeError} for the named binary operation, along
     * the lines "unsupported operand type(s) for OP: 'V' and 'W'".
     *
     * @param op operation to report
     * @param v left operand (only {@code PyType.of(v)} is used)
     * @param w right operand (only {@code PyType.of(w)} is used)
     * @return exception to throw
     */
    // XXX Possibly move to Slot so may bind early.
    static PyException operandError(Slot op, Object v, Object w) {
        return new TypeError(UNSUPPORTED_TYPES, op.opName, PyType.of(v).getName(),
                PyType.of(w).getName());
    }

    private static final String UNSUPPORTED_TYPES =
            "unsupported operand type(s) for %s: '%.100s' and '%.100s'";
}
