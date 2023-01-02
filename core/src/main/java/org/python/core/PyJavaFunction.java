// Copyright (c)2022 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.core;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.List;

import org.python.base.InterpreterError;
import org.python.base.MethodKind;
import org.python.core.ArgumentError.Mode;
import org.python.core.Exposed.Getter;
import org.python.core.Exposed.Member;

/**
 * The Python {@code builtin_function_or_method} object. Java
 * sub-classes represent either a built-in function or a built-in
 * method bound to a particular object.
 */
public abstract class PyJavaFunction implements CraftedPyObject, FastCall {

    /** The type of Python object this class implements. */
    static final PyType TYPE = PyType.fromSpec( //
            new PyType.Spec("builtin_function_or_method", MethodHandles.lookup()));

    @Override
    public PyType getType() { return TYPE; }

    /** Name of the containing module (or {@code null}). */
    final String module;

    /**
     * The object to which this is bound as target (or {@code null}).
     * Conventions (adopted from CPython) around this field are that it
     * should be {@code null} when representing a static method of a
     * built-in class, and otherwise contain the bound target
     * ({@code object} or {@code type}). A function obtained from a
     * module may be a method bound to an instance of that module.
     */
    @Member("__self__")
    final Object self;

    /**
     * A Java {@code MethodHandle} that implements the function or bound
     * method. The type of this handle varies according to the sub-class
     * of {@code PyJavaFunction}, but it is definitely "prepared" to
     * accept {@code Object.class} instances or arrays, not the actual
     * parameter types of the method definition in Java.
     */
    final MethodHandle handle;

    /**
     * An argument parser supplied to this {@code PyJavaFunction} at
     * construction, from Java reflection of the definition in Java and
     * from annotations on it. Full information on the signature is
     * available from this structure, and it is available to parse the
     * arguments to {@link #__call__(Object[], String[])}.
     */
    final ArgParser argParser;

    /**
     * Construct a Python {@code builtin_function_or_method} object,
     * optionally bound to a particular "self" object, specifying the
     * prepared method handle. The {@code self} object to which this is
     * bound should be {@code null} if the method is Python static in a
     * type. Otherwise, we will create a method bound to {@code self} as
     * target. This may be any {@code object} in the case of an instance
     * method, is a {@code type} in the case of a class method, and is a
     * {@code module} in the case of a function in a module (whether the
     * Java signature is Java static or not).
     *
     * @param argParser parser defining the method
     * @param handle a prepared prepared to the method defined
     * @param self object to which bound (or {@code null} if a static
     *     method)
     * @param module name of the module supplying the definition
     */
    protected PyJavaFunction(ArgParser argParser, MethodHandle handle, Object self, String module) {
        this.argParser = argParser;
        this.handle = handle;
        this.self = self;
        this.module = module;
    }

    /**
     * Construct a {@code PyJavaFunction} from an {@link ArgParser} and
     * {@code MethodHandle} for the implementation method. The arguments
     * described by the parser do not include "self". This is the
     * factory we use to create a function in a module.
     *
     * @param ap argument parser (provides name etc.)
     * @param method raw handle to the method defined
     * @param self object to which bound (or {@code null} if a static
     *     method)
     * @param module name of the module supplying the definition (or
     *     {@code null} if representing a bound method of a type)
     * @return A method descriptor supporting the signature
     */
    // Compare CPython PyCFunction_NewEx in methodobject.c
    static PyJavaFunction fromParser(ArgParser ap, MethodHandle method, Object self,
            String module) {
        /*
         * Note this is a recommendation on the assumption all optimisations
         * are supported. The actual choice is made in the switch statement.
         */
        MethodSignature sig = MethodSignature.fromParser(ap);

        assert ap.methodKind != MethodKind.CLASS;

        /*
         * In each case, we must prepare a method handle of the chosen
         * shape.
         */
        switch (sig) {
            case NOARGS:
                method = MethodSignature.NOARGS.prepareBound(ap, method, self);
                return new NoArgs(ap, method, self, module);
            case O1:
                method = MethodSignature.O1.prepareBound(ap, method, self);
                return new O1(ap, method, self, module);
            case O2:
                method = MethodSignature.O2.prepareBound(ap, method, self);
                return new O2(ap, method, self, module);
            case O3:
                method = MethodSignature.O3.prepareBound(ap, method, self);
                return new O3(ap, method, self, module);
            case POSITIONAL:
                method = MethodSignature.POSITIONAL.prepareBound(ap, method, self);
                return new Positional(ap, method, self, module);
            default:
                method = MethodSignature.GENERAL.prepareBound(ap, method, self);
                return new General(ap, method, self, module);
        }
    }

    /**
     * Construct a {@code PyJavaFunction} from a {@link PyMethodDescr}
     * and optional object to bind. The {@link PyMethodDescr} provides
     * the parser and unbound prepared {@code MethodHandle}. The
     * arguments described by the parser do not include "self". This is
     * the factory that supports descriptor {@code __get__}.
     *
     * @param descr descriptor being bound
     * @param self object to which bound (or {@code null} if a static
     *     method)
     * @return a Java method object supporting the signature
     * @throws TypeError if {@code self} is not compatible with
     *     {@code descr}
     * @throws Throwable on other errors while chasing the MRO
     */
    // Compare CPython PyCFunction_NewEx in methodobject.c
    static PyJavaFunction from(PyMethodDescr descr, Object self) throws TypeError, Throwable {
        ArgParser ap = descr.argParser;
        assert ap.methodKind == MethodKind.INSTANCE;
        MethodHandle handle = descr.getHandle(self).bindTo(self);
        // We must support the same optimisations as PyMethodDescr
        switch (descr.signature) {
            case NOARGS:
                return new NoArgs(ap, handle, self, null);
            case O1:
                return new O1(ap, handle, self, null);
            case O2:
                return new O2(ap, handle, self, null);
            case O3:
                return new O3(ap, handle, self, null);
            case POSITIONAL:
                return new Positional(ap, handle, self, null);
            case GENERAL:
                return new General(ap, handle, self, null);
            default:
                throw new InterpreterError("Optimisation not supported: %s", descr.signature);
        }
    }

    // slot functions -------------------------------------------------

    protected Object __repr__() throws Throwable {
        if (self == null || self instanceof PyModule)
            return String.format("<built-in function %s>", __name__());
        else
            return String.format("<built-in method %s of %s>", __name__(), PyObjectUtil.toAt(self));
    }

    Object __call__(Object[] args, String[] names) throws TypeError, Throwable {
        try {
            // It is *not* worth unpacking the array here
            return call(args, names);
        } catch (ArgumentError ae) {
            throw typeError(ae, args, names);
        }
    }

    // exposed methods -----------------------------------------------

    /** @return name of the function or method */
    // Compare CPython meth_get__name__ in methodobject.c
    @Getter
    String __name__() { return argParser.name; }

    // plumbing ------------------------------------------------------

    @Override
    public String toString() { return Py.defaultToString(this); }

    /**
     * Translate a problem with the number and pattern of arguments, in
     * a failed attempt to call the wrapped method, to a Python
     * {@link TypeError}.
     *
     * @param ae expressing the problem
     * @param args positional arguments (only the number will matter)
     * @return a {@code TypeError} to throw
     */
    // XXX Compare MethodDescriptor.typeError : unify?
    @Override
    @SuppressWarnings("fallthrough")
    public TypeError typeError(ArgumentError ae, Object[] args, String[] names) {
        int n = args.length;
        switch (ae.mode) {
            case NOARGS:
            case NUMARGS:
            case MINMAXARGS:
                return new TypeError("%s() %s (%d given)", __name__(), ae, n);
            case NOKWARGS:
                assert names != null && names.length > 0;
            default:
                return new TypeError("%s() %s", __name__(), ae);
        }
    }

    /**
     * The implementation may have any signature allowed by
     * {@link ArgParser}.
     */
    private static class General extends PyJavaFunction {

        /**
         * Construct a method object, identifying the implementation by a
         * parser and a method handle.
         *
         * @param argParser describing the signature of the method
         * @param handle a prepared prepared to the method defined
         * @param self object to which bound (or {@code null} if a static
         *     method)
         * @param module name of the module supplying the definition (or
         *     {@code null} if representing a bound method of a type)
         */
        General(ArgParser argParser, MethodHandle handle, Object self, String module) {
            super(argParser, handle, self, module);
            assert handle.type() == MethodSignature.GENERAL.boundType;
        }

        @Override
        public Object call(Object[] args, String[] names) throws TypeError, Throwable {
            /*
             * The method handle type is {@code (O[])O}. The parser will make an
             * array of the args, and where allowed, gather excess arguments
             * into a tuple or dict, and fill missing ones from defaults.
             */
            Object[] frame = argParser.parse(args, names);
            return handle.invokeExact(frame);
        }
    }

    /**
     * Base class for methods that accept between defined maximum and
     * minimum numbers of arguments, that must be given by position.
     * Maximum and minimum may be equal to a single acceptable number.
     * <p>
     * Arguments may not be given by keyword. There is no excess
     * argument (varargs) collector.
     * <p>
     * The number of arguments required by the wrapped Java method sets
     * a maximum allowable number of arguments. Fewer arguments than
     * this may be given, to the extent that defaults specified by the
     * parser make up the difference. The number of available defaults
     * determines the minimum number of arguments to be supplied.
     *
     * @ImplNote Sub-classes must define {@link #call(Object[])}: the
     *     default definition in {@link FastCall} is not enough.
     */
    private static abstract class AbstractPositional extends PyJavaFunction {

        /** Default values of the trailing arguments. */
        protected final Object[] d;

        /** Minimum number of positional arguments in a call. */
        protected final int min;

        /** Maximum number of positional arguments in a call. */
        protected final int max;

        /**
         * Construct a method descriptor, identifying the implementation by
         * a parser and a method handle.
         */
        // Compare CPython PyDescr_NewMethod in descrobject.c
        AbstractPositional(ArgParser argParser, MethodHandle handle, Object self, String module) {
            super(argParser, handle, self, module);
            assert !argParser.hasVarArgs();
            // Cardinal values for positional argument processing
            this.d = argParser.getDefaults();
            this.max = argParser.argcount;
            this.min = argParser.argcount - d.length;
        }

        @Override
        public Object call(Object[] args, String[] names) throws TypeError, Throwable {
            if (names == null || names.length == 0) {
                return call(args);
            } else {
                throw new ArgumentError(Mode.NOKWARGS);
            }
        }

        @Override
        public Object call(Object[] args) throws TypeError, Throwable {
            // Make sure we find out if this is missing
            throw new InterpreterError(
                    "Sub-classes of AbstractPositional must define call(Object[])");
        }

        // Save some indirection by specialising to positional
        @Override
        Object __call__(Object[] args, String[] names) throws TypeError, Throwable {
            try {
                if (names == null || names.length == 0) {
                    // It is *not* worth unpacking the array here
                    return call(args);
                } else {
                    throw new ArgumentError(Mode.NOKWARGS);
                }
            } catch (ArgumentError ae) {
                throw typeError(ae, args, names);
            }
        }
    }

    /** The implementation signature accepts no arguments. */
    private static class NoArgs extends AbstractPositional {

        /**
         * Construct a method object, identifying the implementation by a
         * parser and a prepared method handle.
         *
         * @param argParser describing the signature of the method
         * @param handle a prepared prepared to the method defined
         * @param self object to which bound (or {@code null} if a static
         *     method)
         * @param module name of the module supplying the definition (or
         *     {@code null} if representing a bound method of a type)
         */
        NoArgs(ArgParser argParser, MethodHandle handle, Object self, String module) {
            super(argParser, handle, self, module);
            assert handle.type() == MethodSignature.NOARGS.boundType;
        }

        @Override
        public Object call(Object[] a) throws Throwable {
            // The method handle type is {@code ()O}.
            if (a.length == 0) { return handle.invokeExact(); }
            // n < min || n > max
            throw new ArgumentError(min, max);
        }
    }

    /**
     * The implementation signature requires one argument, which may be
     * supplied by {@link ArgParser#getDefaults()}.
     */
    private static class O1 extends AbstractPositional {

        /**
         * Construct a method object, identifying the implementation by a
         * parser and a method handle.
         *
         * @param argParser describing the signature of the method
         * @param handle a prepared prepared to the method defined
         * @param self object to which bound (or {@code null} if a static
         *     method)
         * @param module name of the module supplying the definition (or
         *     {@code null} if representing a bound method of a type)
         */
        O1(ArgParser argParser, MethodHandle handle, Object self, String module) {
            super(argParser, handle, self, module);
            assert handle.type() == MethodSignature.O1.boundType;
        }

        @Override
        public Object call(Object[] a) throws TypeError, Throwable {
            // The method handle type is {@code (O)O}.
            int n = a.length;
            if (n == 1) {
                // Number of arguments matches number of parameters
                return handle.invokeExact(a[0]);
            } else if (n == min) {
                // Since min<=max, max==1 and n!=1, we have n==min==0
                return handle.invokeExact(d[0]);
            }
            // n < min || n > max
            throw new ArgumentError(min, max);
        }
    }

    /**
     * The implementation signature requires two arguments, which may be
     * supplied by {@link ArgParser#getDefaults()}.
     */
    private static class O2 extends AbstractPositional {

        /**
         * Construct a method descriptor, identifying the implementation by
         * a parser and a method handle.
         *
         * @param objclass the class declaring the method
         * @param argParser describing the signature of the method
         * @param method handle to invoke the wrapped method or {@code null}
         *     signifying a matching empty handle.
         */
        // Compare CPython PyDescr_NewMethod in descrobject.c
        O2(ArgParser argParser, MethodHandle handle, Object self, String module) {
            super(argParser, handle, self, module);
            assert handle.type() == MethodSignature.O2.boundType;
            assert max == 2;
            assert max - min == d.length;
        }

        @Override
        public Object call(Object[] a) throws ArgumentError, TypeError, Throwable {
            // The method handle type is (O,O)O.
            int n = a.length, k;
            if (n == 2) {
                // Number of arguments matches number of parameters
                return handle.invokeExact(a[0], a[1]);
            } else if ((k = n - min) >= 0) {
                if (n == 1) {
                    return handle.invokeExact(a[0], d[k]);
                } else if (n == 0)
                    return handle.invokeExact(d[k++], d[k]);
            }
            // n < min || n > max
            throw new ArgumentError(min, max);
        }

        @Override
        public Object call() throws Throwable {
            if (min == 0) { return handle.invokeExact(d[0], d[1]); }
            throw new ArgumentError(min, max);
        }

        @Override
        public Object call(Object a0) throws Throwable {
            int k = 1 - min;
            if (k >= 0) { return handle.invokeExact(a0, d[k]); }
            throw new ArgumentError(min, max);
        }

        @Override
        public Object call(Object self, Object a0, Object a1) throws Throwable {
            return handle.invokeExact(self, a0, a1);
        }
    }

    /**
     * The implementation signature requires three arguments, which may
     * be supplied by {@link ArgParser#getDefaults()}.
     */
    private static class O3 extends AbstractPositional {

        /**
         * Construct a method descriptor, identifying the implementation by
         * a parser and a method handle.
         *
         * @param objclass the class declaring the method
         * @param argParser describing the signature of the method
         * @param method handle to invoke the wrapped method or {@code null}
         *     signifying a matching empty handle.
         */
        // Compare CPython PyDescr_NewMethod in descrobject.c
        O3(ArgParser argParser, MethodHandle handle, Object self, String module) {
            super(argParser, handle, self, module);
            assert handle.type() == MethodSignature.O3.boundType;
            assert max == 3;
            assert max - min == d.length;
        }

        @Override
        public Object call(Object[] a) throws ArgumentError, TypeError, Throwable {
            // The method handle type is (O,O)O.
            int n = a.length, k;
            if (n == 3) {
                // Number of arguments matches number of parameters
                return handle.invokeExact(a[0], a[1], a[2]);
            } else if ((k = n - min) >= 0) {
                if (n == 2) {
                    return handle.invokeExact(a[0], a[1], d[k]);
                } else if (n == 1) {
                    return handle.invokeExact(a[0], d[k++], d[k]);
                } else {
                    return handle.invokeExact(d[k++], d[k++], d[k]);
                }
            }
            // n < min || n > max
            throw new ArgumentError(min, max);
        }

        @Override
        public Object call() throws Throwable {
            if (min == 0) { return handle.invokeExact(d[0], d[1], d[2]); }
            throw new ArgumentError(min, max);
        }

        @Override
        public Object call(Object a0) throws Throwable {
            int k = 1 - min;
            if (k >= 0) { return handle.invokeExact(a0, d[k++], d[k]); }
            throw new ArgumentError(min, max);
        }

        @Override
        public Object call(Object a0, Object a1) throws Throwable {
            int k = 2 - min;
            if (k >= 0) { return handle.invokeExact(a0, a1, d[k]); }
            throw new ArgumentError(min, max);
        }

        @Override
        public Object call(Object a0, Object a1, Object a2) throws Throwable {
            return handle.invokeExact(a0, a1, a2);
        }
    }

    /**
     * A method represented by {@code Positional} only accepts arguments
     * given by position. The constraints detailed for
     * {@link AbstractPositional} apply.
     * <p>
     * {@link #fromParser(PyType, ArgParser, List) fromParser()} will
     * only choose a {@code Positional} (or sub-class) representation of
     * the method when these conditions apply.
     */
    private static class Positional extends AbstractPositional {

        /**
         * Construct a method object, identifying the implementation by a
         * parser and a method handle.
         *
         * @param argParser describing the signature of the method
         * @param handle a prepared prepared to the method defined
         * @param self object to which bound (or {@code null} if a static
         *     method)
         * @param module name of the module supplying the definition (or
         *     {@code null} if representing a bound method of a type)
         */
        // XXX Compare CPython XXX in XXX
        Positional(ArgParser argParser, MethodHandle handle, Object self, String module) {
            super(argParser, handle, self, module);
            assert handle.type() == MethodSignature.POSITIONAL.boundType;
            assert max == argParser.argcount;
            assert max - min == d.length;
        }

        @Override
        public Object call(Object[] args) throws TypeError, Throwable {
            // The method handle type is {@code (O[])O}.
            int n = args.length, k;
            if (n == max) {
                // Number of arguments matches number of parameters
                return handle.invokeExact(args);
            } else if ((k = n - min) >= 0) {
                // Concatenate args[:] and defaults[k:]
                Object[] frame = new Object[max];
                System.arraycopy(args, 0, frame, 0, n);
                System.arraycopy(d, k, frame, n, max - n);
                return handle.invokeExact(frame);
            }
            // n < min || n > max
            throw new ArgumentError(min, max);
        }
    }
}
