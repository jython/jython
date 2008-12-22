// Copyright (c) Corporation for National Research Initiatives
package org.python.core;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Set;

import org.python.util.Generic;

public class PyReflectedFunction extends PyObject {

    public String __name__;

    public PyObject __doc__ = Py.None;

    public ReflectedArgs[] argslist = new ReflectedArgs[1];

    public int nargs;

    protected PyReflectedFunction(String name) {
        __name__ = name;
    }

    public PyReflectedFunction(Method... methods) {
        this(methods[0].getName());
        for (Method meth : methods) {
            addMethod(meth);
        }
        if (nargs == 0) {
            String msg = String.format("Attempted to make Java method visible, but it isn't "
                                               + "callable[method=%s, class=%s]",
                                       methods[0].getName(),
                                       methods[0].getDeclaringClass());
            throw Py.SystemError(msg);
        }
    }

    public PyObject _doget(PyObject container) {
        return _doget(container, null);
    }

    public PyObject _doget(PyObject container, PyObject wherefound) {
        if (container == null) {
            return this;
        }
        return new PyMethod(this, container, wherefound);
    }

    public PyObject getDoc() {
        return __doc__;
    }

    private ReflectedArgs makeArgs(Method m) {
        return new ReflectedArgs(m,
                                 m.getParameterTypes(),
                                 m.getDeclaringClass(),
                                 Modifier.isStatic(m.getModifiers()));
    }

    public PyReflectedFunction copy() {
        PyReflectedFunction func = new PyReflectedFunction(__name__);
        func.__doc__ = __doc__;
        func.nargs = nargs;
        func.argslist = new ReflectedArgs[nargs];
        System.arraycopy(argslist, 0, func.argslist, 0, nargs);
        return func;
    }

    public boolean handles(Method method) {
        return handles(makeArgs(method));
    }

    protected boolean handles(ReflectedArgs args) {
        for (int i = 0; i < nargs; i++) {
            int cmp = args.compareTo(argslist[i]);
            if (cmp == 0) {
                return true;
            } else if (cmp == 1) {
                return false;
            }
        }
        return false;
    }

    public void addMethod(Method m) {
        // Only add public methods unless we're overriding
        if (!Modifier.isPublic(m.getModifiers()) && Options.respectJavaAccessibility) {
            return;
        }
        addArgs(makeArgs(m));
    }

    protected void addArgs(ReflectedArgs args) {
        int i;
        for (i = 0; i < nargs; i++) {
            int cmp = args.compareTo(argslist[i]);
            if (cmp == 0) {
                return;
            } else if (cmp == ReflectedArgs.REPLACE) {
                argslist[i] = args;
                return;
            } else if (cmp == -1) {
                break;
            }
        }
        int nn = nargs + 1;
        if (nn > argslist.length) {
            ReflectedArgs[] newargslist = new ReflectedArgs[nn + 2];
            System.arraycopy(argslist, 0, newargslist, 0, nargs);
            argslist = newargslist;
        }
        for (int j = nargs; j > i; j--) {
            argslist[j] = argslist[j - 1];
        }
        argslist[i] = args;
        nargs = nn;
    }

    public PyObject __call__(PyObject self, PyObject[] args, String[] keywords) {
        ReflectedCallData callData = new ReflectedCallData();
        Object method = null;
        ReflectedArgs[] argsl = argslist;
        int n = nargs;
        for (int i = 0; i < n; i++) {
            ReflectedArgs rargs = argsl[i];
            // System.err.println(rargs.toString());
            if (rargs.matches(self, args, keywords, callData)) {
                method = rargs.data;
                break;
            }
        }
        if (method == null) {
            throwError(callData.errArg, args.length, self != null, keywords.length != 0);
        }
        Object cself = callData.self;
        Method m = (Method)method;
        Object o;
        try {
            o = m.invoke(cself, callData.getArgsArray());
        } catch (Throwable t) {
            throw Py.JavaError(t);
        }
        return Py.java2py(o);
    }

    public PyObject __call__(PyObject[] args, String[] keywords) {
        return __call__(null, args, keywords);
    }

    // A bunch of code to make error handling prettier
    protected void throwError(String message) {
        throw Py.TypeError(__name__ + "(): " + message);
    }

    private static void addRange(StringBuilder buf, int min, int max, String sep) {
        if (buf.length() > 0) {
            buf.append(sep);
        }
        if (min < max) {
            buf.append(Integer.toString(min)).append('-').append(max);
        } else {
            buf.append(min);
        }
    }

    protected void throwArgCountError(int nArgs, boolean self) {
        // Assume no argument lengths greater than 40...
        boolean[] legalArgs = new boolean[40];
        int maxArgs = -1;
        int minArgs = 40;
        for (int i = 0; i < nargs; i++) {
            ReflectedArgs rargs = argslist[i];
            int l = rargs.args.length;
            if (!self && !rargs.isStatic) {
                l += 1;
            }
            legalArgs[l] = true;
            if (l > maxArgs) {
                maxArgs = l;
            }
            if (l < minArgs) {
                minArgs = l;
            }
        }
        StringBuilder buf = new StringBuilder();
        int startRange = minArgs;
        int a = minArgs + 1;
        while (a < maxArgs) {
            if (legalArgs[a]) {
                a++;
                continue;
            } else {
                addRange(buf, startRange, a - 1, ", ");
                a++;
                while (a <= maxArgs) {
                    if (legalArgs[a]) {
                        startRange = a;
                        break;
                    }
                    a++;
                }
            }
        }
        addRange(buf, startRange, maxArgs, " or ");
        throwError("expected " + buf + " args; got " + nArgs);
    }

    private static String ordinal(int n) {
        switch(n + 1){
            case 0:
                return "self";
            case 1:
                return "1st";
            case 2:
                return "2nd";
            case 3:
                return "3rd";
            default:
                return Integer.toString(n + 1) + "th";
        }
    }

    private static String niceName(Class arg) {
        if (arg == String.class || arg == PyString.class) {
            return "String";
        }
        if (arg.isArray()) {
            return niceName(arg.getComponentType()) + "[]";
        }
        return arg.getName();
    }

    protected void throwBadArgError(int errArg, int nArgs, boolean self) {
        Set<Class<?>> argTypes = Generic.set();
        for (int i = 0; i < nargs; i++) {
            // if (!args.isStatic && !self) { len = len-1; }
            // This check works almost all the time.
            // I'm still a little worried about non-static methods
            // called with an explict self...
            if (argslist[i].args.length == nArgs) {
                if (errArg == ReflectedCallData.UNABLE_TO_CONVERT_SELF) {
                    argTypes.add(argslist[i].declaringClass);
                } else {
                    argTypes.add(argslist[i].args[errArg]);
                }
            }
        }
        StringBuilder buf = new StringBuilder();
        for (Class<?> arg : argTypes) {
            buf.append(niceName(arg));
            buf.append(", ");
        }
        if (buf.length() > 2) {
            buf.setLength(buf.length() - 2);
        }
        throwError(ordinal(errArg) + " arg can't be coerced to " + buf);
    }

    protected void throwError(int errArg, int nArgs, boolean self, boolean keywords) {
        if (keywords) {
            throwError("takes no keyword arguments");
        } else if (errArg == ReflectedCallData.BAD_ARG_COUNT) {
            throwArgCountError(nArgs, self);
        } else {
            throwBadArgError(errArg, nArgs, self);
        }
    }

    // Included only for debugging purposes...
    public void printArgs() {
        System.err.println("nargs: " + nargs);
        for (int i = 0; i < nargs; i++) {
            ReflectedArgs args = argslist[i];
            System.err.println(args.toString());
        }
    }

    public String toString() {
        // printArgs();
        return "<java function " + __name__ + " " + Py.idstr(this) + ">";
    }
}
