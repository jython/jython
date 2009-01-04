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
        if (isPackagedProtected(m.getDeclaringClass())) {
            /*
            * Set public methods on package protected classes accessible so that reflected calls to
            * the method in subclasses of the package protected class will succeed. Yes, it's
            * convoluted.
            *
            * This fails when done through reflection due to Sun JVM bug
            * 4071957(http://tinyurl.com/le9vo). 4533479 actually describes the problem we're
            * seeing, but there are a bevy of reflection bugs that stem from 4071957. Supposedly
            * it'll be fixed in Dolphin but it's been promised in every version since Tiger
            * so don't hold your breath.
            */
            try {
                m.setAccessible(true);
            } catch (SecurityException se) {
                // This case is pretty far in the corner, so don't scream if we can't set the method
                // accessible due to a security manager.  Any calls to it will fail with an
                // IllegalAccessException, so it'll become visible there.  This way we don't spam
                // people who aren't calling methods like this from Python with warnings if a
                // library they're using happens to have a method like this.
            }
        }
        addArgs(makeArgs(m));
    }

    public static boolean isPackagedProtected(Class<?> c) {
        int mods = c.getModifiers();
        return !(Modifier.isPublic(mods) || Modifier.isPrivate(mods) || Modifier.isProtected(mods));
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
        ReflectedArgs match = null;
        for (int i = 0; i < nargs && match == null; i++) {
            // System.err.println(rargs.toString());
            if (argslist[i].matches(self, args, keywords, callData)) {
                match = argslist[i];
            }
        }
        if (match == null) {
            throwError(callData.errArg, args.length, self != null, keywords.length != 0);
        }
        Object cself = callData.self;
        Method m = (Method)match.data;

        // If this is a direct call to a Java class instance method with a PyProxy instance as the
        // arg, use the super__ version to actually route this through the method on the class.
        if (self == null && cself != null && cself instanceof PyProxy
                && !__name__.startsWith("super__")
                && match.declaringClass != cself.getClass()) {
            String mname = ("super__" + __name__);
            try {
                m = cself.getClass().getMethod(mname, m.getParameterTypes());
            } catch (Exception e) {
                throw Py.JavaError(e);
            }
        }
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

    private static String niceName(Class<?> arg) {
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
            // This check works almost all the time.
            // I'm still a little worried about non-static methods called with an explicit self...
            if (argslist[i].args.length == nArgs ||
                    (!argslist[i].isStatic && !self && argslist[i].args.length == nArgs - 1)) {
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
