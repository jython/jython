// Copyright (c) Corporation for National Research Initiatives
// Copyright (c)2019 Jython Developers.
// Licensed to PSF under a Contributor Agreement.
package org.python.core;

import java.lang.reflect.Member;

/** Map the signature of a method to the {@code Method} itself, within the context of a given simple name. This is used in support of signature polymorphism in Java methods and constructors reflected into Python. **/
public class ReflectedArgs {

    /** The types of arguments defining this signature (key) */
    public Class<?>[] args;

    /** The specific method (or constructor). */
    public Member method;

    public Class<?> declaringClass;

    public boolean isStatic;

    public boolean isVarArgs;

    public int flags;

    public static final int StandardCall = 0;

    public static final int PyArgsCall = 1;

    public static final int PyArgsKeywordsCall = 2;

    public ReflectedArgs(Member method, Class<?>[] args, Class<?> declaringClass, boolean isStatic) {
        this(method, args, declaringClass, isStatic, false);
    }

    public ReflectedArgs(Member method, Class<?>[] args, Class<?> declaringClass, boolean isStatic,
            boolean isVarArgs) {
        this.method = method;
        this.args = args;
        this.declaringClass = declaringClass;
        this.isStatic = isStatic;
        // only used for varargs matching; it should be added after the unboxed form
        this.isVarArgs = isVarArgs;
        if (args.length == 1 && args[0] == PyObject[].class) {
            this.flags = PyArgsCall;
        } else if (args.length == 2 && args[0] == PyObject[].class && args[1] == String[].class) {
            this.flags = PyArgsKeywordsCall;
        } else {
            this.flags = StandardCall;
        }
    }

    public boolean matches(PyObject self, PyObject[] pyArgs, String[] keywords,
            ReflectedCallData callData) {
        if (this.flags != PyArgsKeywordsCall) {
            if (keywords != null && keywords.length != 0) {
                return false;
            }
        }

        // if (isStatic ? self != null : self == null) return Py.NoConversion;
        /* Ugly code to handle mismatch in static vs. instance functions... */
        /*
         * Will be very inefficient in cases where static and instance functions both exist with
         * same names and number of args
         */
        if (this.isStatic) {
            if (self != null) {
                self = null;
            }
        } else {
            if (self == null) {
                if (pyArgs.length == 0) {
                    return false;
                }
                self = pyArgs[0];
                PyObject[] newArgs = new PyObject[pyArgs.length - 1];
                System.arraycopy(pyArgs, 1, newArgs, 0, newArgs.length);
                pyArgs = newArgs;
            }
        }

        if (this.flags == PyArgsKeywordsCall) { // foo(PyObject[], String[])
            callData.setLength(2);
            callData.args[0] = pyArgs;
            callData.args[1] = keywords;
            callData.self = self;
            if (self != null) {
                Object tmp = self.__tojava__(this.declaringClass);
                if (tmp != Py.NoConversion) {
                    callData.self = tmp;
                }
            }
            return true;
        } else if (this.flags == PyArgsCall) { // foo(PyObject[])
            callData.setLength(1);
            callData.args[0] = pyArgs;
            callData.self = self;
            if (self != null) {
                Object tmp = self.__tojava__(this.declaringClass);
                if (tmp != Py.NoConversion) {
                    callData.self = tmp;
                }
            }
            return true;
        }

        int n = this.args.length;

        // if we have a varargs method AND the last PyObject is not a list/tuple
        // we need to do box (wrap with an array) the last pyArgs.length - n args
        // (which might be empty)
        //
        // examples:
        // test(String... x)
        // test(List... x)
        //
        // in this last example, don't worry if someone is overly clever in calling this,
        // they can always write their own version of PyReflectedFunction and put it in the proxy
        // if that's what they need to do ;)

        if (isVarArgs) {
            pyArgs = ensureBoxedVarargs(pyArgs, n);
        }

        if (pyArgs.length != n) {
            return false;
        }

        // Make error messages clearer
        callData.errArg = ReflectedCallData.UNABLE_TO_CONVERT_SELF;

        if (self != null) {
            Object tmp = self.__tojava__(this.declaringClass);
            if (tmp == Py.NoConversion) {
                return false;
            }
            callData.self = tmp;
        } else {
            callData.self = null;
        }

        callData.setLength(n);
        Object[] javaArgs = callData.args;

        for (int i = 0; i < n; i++) {
            PyObject pyArg = pyArgs[i];
            Class<?> targetClass = this.args[i];
            Object javaArg = pyArg.__tojava__(targetClass);
            javaArgs[i] = javaArg;
            if (javaArg == Py.NoConversion) {
                if (i > callData.errArg) {
                    callData.errArg = i;
                }
                return false;
            }
        }
        return true;
    }

    /* Boxes argument in the varargs position if not already boxed */
    private PyObject[] ensureBoxedVarargs(PyObject[] pyArgs, int n) {
        if (pyArgs.length == 0) {
            // If there are no args return an empty list
            return new PyObject[] {new PyList()};
        }
        PyObject lastArg = pyArgs[pyArgs.length - 1];
        if (pyArgs.length == n && isSequenceVararg(lastArg)) {
            // This is only relevant if the number of arguments would be correct if a final sequence
            // argument was treated as a vararg argument. eg, if two lists are passed to a function
            // that accepts (Object...), they should be boxed and passed as a single list, even though
            // the final arg is a sequence.

            // FIXME also check if lastArg is sequence-like
            return pyArgs; // will be boxed in an array once __tojava__ is called
        }
        int non_varargs_len = n - 1;
        if (pyArgs.length < non_varargs_len) {
            return pyArgs;
        }
        PyObject[] boxedPyArgs = new PyObject[n];
        for (int i = 0; i < non_varargs_len; i++) {
            boxedPyArgs[i] = pyArgs[i];
        }
        int varargs_len = pyArgs.length - non_varargs_len;
        PyObject[] varargs = new PyObject[varargs_len];
        for (int i = 0; i < varargs_len; i++) {
            varargs[i] = pyArgs[non_varargs_len + i];
        }
        boxedPyArgs[non_varargs_len] = new PyList(varargs);
        return boxedPyArgs;
    }

    private int fixedArgCount() {
        return isVarArgs ? args.length - 1 : args.length;
    }

    /**
     * Compare matching varargs signatures for the same Python call.
     *
     * <p>The normal signature ordering is intentionally independent of a particular call. For
     * overloaded varargs methods we need one more step because several signatures may match after
     * the Python arguments are packed into the final array. Prefer the signature whose Java
     * parameter types are a better fit for the actual Python objects, then prefer the signature
     * with more fixed parameters, and finally keep the existing signature ordering as a stable
     * fallback.
     */
    boolean betterVarargsMatchThan(ReflectedArgs other, PyObject self, PyObject[] pyArgs) {
        int thisCost = varargsCallCost(methodPyArgs(self, pyArgs));
        int otherCost = other.varargsCallCost(other.methodPyArgs(self, pyArgs));
        if (thisCost != otherCost) {
            return thisCost < otherCost;
        }

        int thisFixed = fixedArgCount();
        int otherFixed = other.fixedArgCount();
        if (thisFixed != otherFixed) {
            return thisFixed > otherFixed;
        }

        return compareTo(other) > 0;
    }

    private PyObject[] methodPyArgs(PyObject self, PyObject[] pyArgs) {
        if (!isStatic && self == null && pyArgs.length > 0) {
            PyObject[] newArgs = new PyObject[pyArgs.length - 1];
            System.arraycopy(pyArgs, 1, newArgs, 0, newArgs.length);
            return newArgs;
        }
        return pyArgs;
    }

    private int varargsCallCost(PyObject[] pyArgs) {
        int cost = 0;
        int fixed = fixedArgCount();
        for (int i = 0; i < fixed && i < pyArgs.length; i++) {
            cost += conversionCost(pyArgs[i], args[i]);
        }

        Class<?> componentType = args[args.length - 1].getComponentType();
        if (pyArgs.length == args.length && pyArgs.length > 0
                && isSequenceVararg(pyArgs[pyArgs.length - 1])) {
            return cost + arrayConversionCost(pyArgs[pyArgs.length - 1], componentType);
        }
        for (int i = fixed; i < pyArgs.length; i++) {
            cost += conversionCost(pyArgs[i], componentType);
        }
        return cost;
    }

    private static boolean isSequenceVararg(PyObject pyArg) {
        // NOTE that the check is against PySequenceList, not PySequence,
        // because certain Java <=> Python semantics currently require this
        // additional strictness. Perhaps this can be relaxed.

        // Empirically this list is exhaustive against the Jython runtime,
        // excluding only PyBaseString, PyMemoryView, Py2kBuffer, BaseBytes,
        // and AstList, many/most of which seem likely to be problematic for
        // varargs usage.
        return pyArg instanceof PySequenceList || pyArg instanceof PyArray || pyArg instanceof PyXRange
                || pyArg instanceof PyIterator;
    }

    /**
     * Return a relative cost for converting {@code pyArg} to {@code target}.
     *
     * <p>These costs are used only to choose among overloads that have already matched, so the
     * absolute values are arbitrary but their broad ranges are intentional:
     *
     * <ul>
     * <li>0 is an exact or preferred Python-to-Java mapping, such as {@code PyBoolean} to
     * {@code boolean} or {@code PyString} to {@code String}.
     * <li>1-9 are close numeric conversions, such as widening or narrowing between numeric
     * primitive types.
     * <li>10-19 are compatible but less specific reference conversions.
     * <li>20-49 are permissive conversions that should lose to type-specific matches, notably
     * boolean truthiness of non-boolean values.
     * <li>50 and above are broad fallback matches, with {@code Object} deliberately very expensive.
     * </ul>
     */
    private static int conversionCost(PyObject pyArg, Class<?> target) {
        target = boxedClass(target);

        Object proxy = pyArg.getJavaProxy();
        if (proxy != null) {
            Class<?> proxyClass = proxy.getClass();
            if (target == proxyClass) {
                return 0;
            } else if (target.isAssignableFrom(proxyClass)) {
                return target == Object.class ? 100 : 10;
            }
        }

        if (pyArg instanceof PyBoolean) {
            if (target == Boolean.class) {
                return 0;
            } else if (target == Integer.class || target == Long.class || target == Short.class
                    || target == Byte.class || target == Float.class || target == Double.class
                    || target == Number.class) {
                return 20;
            }
        } else if (pyArg instanceof PyInteger) {
            if (target == Integer.class) {
                return 0;
            } else if (target == Long.class) {
                return 1;
            } else if (target == Short.class || target == Byte.class || target == Float.class
                    || target == Double.class || target == Number.class) {
                return 2;
            } else if (target == Boolean.class) {
                return 20;
            }
        } else if (pyArg instanceof PyFloat) {
            if (target == Double.class) {
                return 0;
            } else if (target == Float.class || target == Number.class) {
                return 1;
            }
        } else if (pyArg instanceof PyString) {
            if (target == String.class) {
                return 0;
            } else if (target == Character.class) {
                return pyArg.__len__() == 1 ? 1 : 1000;
            }
        }

        if (target.isArray()) {
            return arrayConversionCost(pyArg, target.getComponentType());
        } else if (target.isInstance(pyArg)) {
            return 0;
        } else if (target == Object.class) {
            return 3000;
        } else {
            return 50 + precedence(target);
        }
    }

    private static int arrayConversionCost(PyObject pyArg, Class<?> componentType) {
        componentType = boxedClass(componentType);
        int cost = 5;
        try {
            int n = pyArg.__len__();
            for (int i = 0; i < n; i++) {
                cost += conversionCost(pyArg.__getitem__(i), componentType);
            }
            return cost;
        } catch (Throwable t) {
            return 1000;
        }
    }

    private static Class<?> boxedClass(Class<?> c) {
        if (!c.isPrimitive()) {
            return c;
        } else if (c == Boolean.TYPE) {
            return Boolean.class;
        } else if (c == Byte.TYPE) {
            return Byte.class;
        } else if (c == Short.TYPE) {
            return Short.class;
        } else if (c == Integer.TYPE) {
            return Integer.class;
        } else if (c == Long.TYPE) {
            return Long.class;
        } else if (c == Float.TYPE) {
            return Float.class;
        } else if (c == Double.TYPE) {
            return Double.class;
        } else if (c == Character.TYPE) {
            return Character.class;
        } else {
            return c;
        }
    }

    public static int precedence(Class<?> arg) {
        if (arg == Object.class) {
            return 3000;
        }
        if (arg.isPrimitive()) {
            if (arg == Long.TYPE) {
                return 10;
            }
            if (arg == Integer.TYPE) {
                return 11;
            }
            if (arg == Short.TYPE) {
                return 12;
            }
            if (arg == Character.TYPE) {
                return 13;
            }
            if (arg == Byte.TYPE) {
                return 14;
            }
            if (arg == Double.TYPE) {
                return 20;
            }
            if (arg == Float.TYPE) {
                return 21;
            }
            if (arg == Boolean.TYPE) {
                return 30;
            }
        }
        // Consider Strings a primitive type
        // This makes them higher priority than byte[]
        if (arg == String.class) {
            return 40;
        }

        if (arg.isArray()) {
            Class<?> componentType = arg.getComponentType();
            if (componentType == Object.class) {
                return 2500;
            }
            return 100 + precedence(componentType);
        }
        return 2000;
    }

    /*
     * Returns 0 iff arg1 == arg2 Returns +/-1 iff arg1 and arg2 are unimportantly different Returns
     * +/-2 iff arg1 and arg2 are significantly different
     */
    public static int compare(Class<?> arg1, Class<?> arg2) {
        int p1 = precedence(arg1);
        int p2 = precedence(arg2);
        // Special code if they are both nonprimitives
        // Superclasses/superinterfaces are considered greater than sub's
        if (p1 >= 2000 && p2 >= 2000) {
            if (arg1.isAssignableFrom(arg2)) {
                if (arg2.isAssignableFrom(arg1)) {
                    return 0;
                } else {
                    return +2;
                }
            } else {
                if (arg2.isAssignableFrom(arg1)) {
                    return -2;
                } else {
                    int cmp = arg1.getName().compareTo(arg2.getName());
                    return cmp > 0 ? +1 : -1;
                }
            }
        }
        return p1 > p2 ? +2 : (p1 == p2 ? 0 : -2);
    }

    public static final int REPLACE = 1998;

    public int compareTo(ReflectedArgs other) {
        Class<?>[] oargs = other.args;

        // First decision based on flags
        if (other.flags != this.flags) {
            return other.flags < this.flags ? -1 : +1;
        }

        // Decision based on number of args
        int n = this.args.length;
        if (n < oargs.length) {
            return -1;
        }
        if (n > oargs.length) {
            return +1;
        }

        // Decide based on static/non-static
        if (this.isStatic && !other.isStatic) {
            return +1;
        }
        if (!this.isStatic && other.isStatic) {
            return -1;
        }
        // Compare the arg lists
        int cmp = 0;
        for (int i = 0; i < n; i++) {
            int tmp = compare(this.args[i], oargs[i]);
            if (tmp == +2 || tmp == -2) {
                cmp = tmp;
            }
            if (cmp == 0) {
                cmp = tmp;
            }
        }

        if (cmp != 0) {
            return cmp > 0 ? +1 : -1;
        }

        // If arg lists are equivalent, look at declaring classes
        boolean replace = other.declaringClass.isAssignableFrom(this.declaringClass);

        // For static methods, use the child's version
        // For instance methods, use the parent's version
        if (!this.isStatic) {
            replace = !replace;
        }

        return replace ? REPLACE : 0;
    }

    @Override
    public String toString() {
        String s = declaringClass + ", static=" + isStatic + ", varargs=" + isVarArgs + ",flags="
                + flags + ", " + method + "\n";
        s = s + "\t(";
        for (Class<?> arg : args) {
            s += arg.getName() + ", ";
        }
        s += ")";
        return s;
    }
}
