// Copyright (c) Corporation for National Research Initiatives
package org.python.core;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Set;

import org.python.util.Generic;


public class PyReflectedFunction extends PyObject
{
    public String __name__;
    public PyObject __doc__ = Py.None;
    public ReflectedArgs[] argslist;
    public int nargs;

    public PyReflectedFunction(String name) {
        __name__ = name;
        argslist = new ReflectedArgs[1];
        nargs = 0;
    }

    public PyReflectedFunction(Method method) {
        this(method.getName());
        addMethod(method);
    }

    public PyObject _doget(PyObject container) {
        return _doget(container, null);
    }

    public PyObject _doget(PyObject container, PyObject wherefound) {
        if (container == null)
            return this;
        return new PyMethod(this, container, wherefound);
    }

    public boolean _doset(PyObject container) {
        throw Py.TypeError("java function not settable: "+__name__);
    }

    public PyObject getDoc() {
        return __doc__;
    }

    private ReflectedArgs makeArgs(Method m) {
        return new ReflectedArgs(m, m.getParameterTypes(),
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
        ReflectedArgs[] argsl = argslist;
        int n = nargs;
        for(int i=0; i<n; i++) {
            int cmp = args.compareTo(argsl[i]);
            if (cmp == 0)
                return true;
            if (cmp == +1)
                return false;
        }
        return false;
    }

    public void addMethod(Method m) {
        int mods = m.getModifiers();
        // Only add public methods unless we're overriding
        if (!Modifier.isPublic(mods) && Options.respectJavaAccessibility)
            return;
        addArgs(makeArgs(m));
    }

    protected void addArgs(ReflectedArgs args) {
        ReflectedArgs[] argsl = argslist;
        int n = nargs;
        int i;
        for(i=0; i<n; i++) {
            int cmp = args.compareTo(argsl[i]);
            if (cmp == 0)
                return;
            if (cmp == ReflectedArgs.REPLACE) {
                argsl[i] = args;
                return;
            }
            if (cmp == -1)
                break;
        }

        int nn = n+1;
        if (nn > argsl.length) {
            argsl = new ReflectedArgs[nn+2];
            System.arraycopy(argslist, 0, argsl, 0, n);
            argslist = argsl;
        }

        for(int j=n; j>i; j--) {
            argsl[j] = argsl[j-1];
        }

        argsl[i] = args;
        nargs = nn;
    }

    public PyObject __call__(PyObject self, PyObject[] args,
                             String[] keywords)
    {
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
            throwError(callData.errArg, args.length, self != null,
                       keywords.length != 0);
        }

        Object cself = callData.self;
        Method m = (Method)method;
        // Check to see if we should be using a super__ method instead
        // This is probably a bit inefficient...
        if (self == null && cself != null && cself instanceof PyProxy &&
                   !__name__.startsWith("super__")) {
            PyInstance iself = ((PyProxy)cself)._getPyInstance();
            if (argslist[0].declaringClass != iself.instclass.proxyClass) {
                String mname = ("super__"+__name__);
                // xxx experimental
                Method[] super__methods = (Method[])iself.instclass.super__methods.get(mname);
                if (super__methods != null) {
                    Class[] msig = m.getParameterTypes();
                    for (Method super__method : super__methods) {
                        if (java.util.Arrays.equals(msig,super__method.getParameterTypes())) {
                            m = super__method;
                            break;
                        }
                    }
                }
            }
        }
        try {

            Object o = m.invoke(cself, callData.getArgsArray());
            return Py.java2py(o);
        } catch (Throwable t) {
            throw Py.JavaError(t);
        }
    }

    public PyObject __call__(PyObject[] args, String[] keywords) {
        return __call__(null, args, keywords);
    }

    // A bunch of code to make error handling prettier


    protected void throwError(String message) {
        throw Py.TypeError(__name__+"(): "+message);
    }

    private static void addRange(StringBuffer buf, int min, int max,
                                 String sep)
    {
        if (buf.length() > 0) {
            buf.append(sep);
        }
        if (min < max) {
            buf.append(Integer.toString(min)+"-"+max);
        } else {
            buf.append(min);
        }
    }


    protected void throwArgCountError(int nArgs, boolean self) {
        // Assume no argument lengths greater than 40...
        boolean[] legalArgs = new boolean[40];
        int maxArgs = -1;
        int minArgs = 40;

        ReflectedArgs[] argsl = argslist;
        int n = nargs;
        for (int i=0; i<n; i++) {
            ReflectedArgs rargs = argsl[i];

            int l = rargs.args.length;
            if (!self && !rargs.isStatic) {
                l += 1;
            }

            legalArgs[l] = true;
            if (l > maxArgs)
                maxArgs = l;
            if (l < minArgs)
                minArgs = l;
        }

        StringBuffer buf = new StringBuffer();

        int startRange = minArgs;
        int a = minArgs+1;
        while (a < maxArgs) {
            if (legalArgs[a]) {
                a++;
                continue;
            } else {
                addRange(buf, startRange, a-1, ", ");
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
        throwError("expected "+buf+" args; got "+nArgs);
    }

    private static String ordinal(int n) {
        switch(n+1) {
        case 0:
            return "self";
        case 1:
            return "1st";
        case 2:
            return "2nd";
        case 3:
            return "3rd";
        default:
            return Integer.toString(n+1)+"th";
        }
    }

    private static String niceName(Class arg) {
        if (arg == String.class || arg == PyString.class) {
            return "String";
        }
        if (arg.isArray()) {
            return niceName(arg.getComponentType())+"[]";
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
        if(buf.length() > 2) {
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
        System.err.println("nargs: "+nargs);
        for(int i=0; i<nargs; i++) {
            ReflectedArgs args = argslist[i];
            System.err.println(args.toString());
        }
    }


    public String toString() {
        //printArgs();
        return "<java function "+__name__+" "+Py.idstr(this)+">";
    }
}
