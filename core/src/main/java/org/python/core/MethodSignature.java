// Copyright (c)2022 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.core;

import static java.lang.invoke.MethodHandles.filterArguments;
import static java.lang.invoke.MethodHandles.filterReturnValue;
import static org.python.core.ClassShorthand.O;
import static org.python.core.ClassShorthand.OA;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import org.python.base.MethodKind;
import org.python.core.Slot.EmptyException;

/**
 * The {@code enum MethodSignature} enumerates the method signatures
 * for which an optimised implementation is possible. Sub-classes of
 * {@link PyJavaFunction} and {@link PyMethodDescr} correspond to
 * these values. It is not required that each value have a distinct
 * optimised sub-class. This {@code enum} is used internally to
 * choose between these sub-classes.
 */
// Compare CPython METH_* constants in methodobject.h
enum MethodSignature {
    // Constructors describe the parameters after self
    /** No arguments allowed after {@code self}. */
    NOARGS(),       // METH_NOARGS
    /** One argument allowed, possibly after {@code self}. */
    O1(O),          // METH_O
    /** Two arguments allowed, possibly after {@code self}. */
    O2(O, O),
    /** Three arguments allowed, possibly after {@code self}. */
    O3(O, O, O),
    /**
     * Only positional arguments allowed, after {@code self} when
     * describing an unbound method.
     */
    POSITIONAL(OA),
    /**
     * Full generality of ArgParser allowed, after {@code self} when
     * describing an unbound method.
     */
    GENERAL(OA);

    /**
     * The type of method handles matching this method signature when it
     * describes a bound or static method. For {@code POSITIONAL} this
     * is the type {@code (O[])O}.
     */
    final MethodType boundType;

    /**
     * The type of method handles matching this method signature when it
     * describes an instance method. This differs from
     * {@link #boundType} by a preceding {@code O}. For
     * {@code POSITIONAL} this is the type {@code (O, O[])O}.
     */
    final MethodType instanceType;

    /**
     * Handle to throw a {@link Slot.EmptyException}, and having the
     * signature {@code instanceType} for this {@code MethodSignature}.
     */
    final MethodHandle empty;

    /** The second parameter is an object array. */
    private final boolean useArray;

    private MethodSignature(Class<?>... ptypes) {
        this.boundType = MethodType.methodType(O, ptypes);
        this.instanceType = boundType.insertParameterTypes(0, O);
        this.empty =
                MethodHandles.dropArguments(Util.THROW_EMPTY, 0, instanceType.parameterArray());
        this.useArray = ptypes.length >= 1 && ptypes[0] == OA;
    }

    /** Handle utilities, supporting signature creation. */
    private static class Util {

        /** Single re-used instance of {@link Slot.EmptyException} */
        private static final EmptyException EMPTY = new EmptyException();

        /**
         * A handle with signature {@code ()O} that throws a single re-used
         * instance of {@code Slot.EmptyException}. We use this in sub-class
         * constructors when given a {@code null} raw method handle, to
         * ensure it is always safe to invoke {@link PyMethodDescr#method}.
         * If the signature is to be believed, {@code EMPTY} returns
         * {@code Object}, although it never actually returns at all.
         */
        static final MethodHandle THROW_EMPTY =
                MethodHandles.throwException(O, Slot.EmptyException.class).bindTo(EMPTY);
    }

    /**
     * Choose a {@code MethodSignature} based on the argument parser.
     * Note that in a {@link PyMethodDescr}, the {@link ArgParser}
     * describes the arguments after {@code self}, even if the
     * implementation is declared {@code static} in Java, so that the
     * {@code self} argument is explicit.
     *
     * @param ap argument parser describing the method
     * @return a chosen {@code MethodSignature}
     */
    static MethodSignature fromParser(ArgParser ap) {
        if (ap.hasVarArgs() || ap.hasVarKeywords()) {
            /*
             * Signatures that have collector parameters for excess arguments
             * given by position or keyword are not worth optimising (we
             * assume).
             */
            return GENERAL;
        } else if (ap.posonlyargcount < ap.regargcount) {
            /*
             * Signatures that allow keyword arguments are too difficult to
             * optimise (we assume).
             */
            return GENERAL;
        } else {
            // Arguments may only be given by position
            return positional(ap.regargcount);
        }
    }

    /**
     * Choose a {@code MethodSignature} based on a {@code MethodType}.
     *
     * @param mt to look for
     * @return a chosen {@code MethodSignature}
     */
    static MethodSignature from(MethodType mt) {
        for (MethodSignature ms : MethodSignature.values()) {
            if (ms.empty.type().equals(mt)) { return ms; }
        }
        return GENERAL;
    }

    /**
     * Select a (potential) optimisation for a method that accepts
     * arguments only by position. Signatures that allow only positional
     * arguments (optionally with default values for trailing parameters
     * not filled by the argument) may be optimised if the number is not
     * too great.
     *
     * @param n number of arguments
     * @return chosen method signature
     */
    private static MethodSignature positional(int n) {
        switch (n) {
            case 0:
                return NOARGS;
            case 1:
                return O1;
            case 2:
                return O2;
            case 3:
                return O3;
            default:
                return POSITIONAL;
        }
    }

    /**
     * Prepare a raw method handle, consistent with this
     * {@code MethodSignature}, so that it matches the type implied by
     * the parser, and may be called in an optimised way.
     *
     * @param ap to which the handle is made to conform
     * @param raw handle representing the Java implementation
     * @return handle consistent with this {@code MethodSignature}
     */
    MethodHandle prepare(ArgParser ap, MethodHandle raw) {
        assert raw != null;
        MethodHandle mh;
        if (ap.methodKind == MethodKind.STATIC) {
            // No self parameter: start at zero
            mh = adapt(raw, 0);
            // Discard the self argument that we pass
            mh = MethodHandles.dropArguments(mh, 0, O);
        } else {
            // Skip self parameter: start at one
            mh = adapt(raw, 1);
        }
        if (useArray) {
            // We will present the last n args as an array
            int n = ap.argnames.length;
            mh = mh.asSpreader(OA, n);
        }
        return mh.asType(instanceType);
    }

    /**
     * Prepare and bind a provided raw method handle, consistent with
     * this {@code MethodSignature}, so that it matches the type implied
     * by the parser, and may be called in an optimised way. This has
     * the right semantics for methods in a {@link JavaModule}, where
     * {@code ap.methodKind==STATIC} means there is no {@code module}
     * argument to bind.
     *
     * @param ap to which the handle is made to conform
     * @param raw handle representing the Java implementation
     * @param self to bind as the first argument if not Python static
     * @return handle consistent with this {@code MethodSignature}
     */
    MethodHandle prepareBound(ArgParser ap, MethodHandle raw, Object self) {
        assert raw != null;
        assert ap.methodKind != MethodKind.CLASS;
        if (ap.methodKind != MethodKind.STATIC) {
            // The type must match here
            raw = raw.bindTo(self);
        }
        MethodHandle mh = adapt(raw, 0);
        if (useArray) {
            // We will present the last n args as an array
            int n = ap.argnames.length;
            mh = mh.asSpreader(OA, n);
        }
        return mh.asType(boundType);
    }

    /**
     * Adapt an arbitrary method handle to one that expects arguments
     * from a given position onwards to be {@code Object}, and returns
     * {@code Object}, using the conversions defined in {@link Clinic}.
     *
     * @param raw the handle to be prepared (or null for empty)
     * @param pos index in the type at which to start.
     * @return handle compatible with {@code methodDef}
     */
    static final MethodHandle adapt(MethodHandle raw, int pos) {
        /*
         * To begin with, adapt the arguments after self to expect a
         * java.lang.Object, if Clinic knows how to convert them.
         */
        MethodType mt = raw.type();
        MethodHandle[] af = Clinic.argumentFilter(mt, pos);
        MethodHandle mh = filterArguments(raw, pos, af);
        MethodHandle rf = Clinic.returnFilter(mt);
        if (rf != null) { mh = filterReturnValue(mh, rf); }
        /*
         * Let the method definition enforce specific constraints and
         * conversions on the handle.
         */
        return mh;
    }
}
