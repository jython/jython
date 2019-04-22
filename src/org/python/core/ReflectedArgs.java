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
        if (lastArg instanceof PySequenceList || lastArg instanceof PyArray
                || lastArg instanceof PyXRange || lastArg instanceof PyIterator) {
            // NOTE that the check is against PySequenceList, not PySequence,
            // because certain Java <=> Python semantics currently require this
            // additional strictness. Perhaps this can be relaxed.

            // Empirically this list is exhaustive against the Jython runtime,
            // excluding only PyBaseString, PyMemoryView, Py2kBuffer, BaseBytes,
            // and AstList, many/most of which seem likely to be problematic for
            // varargs usage.

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
