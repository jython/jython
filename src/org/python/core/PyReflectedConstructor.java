// Copyright (c) Corporation for National Research Initiatives
package org.python.core;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.lang.reflect.InvocationTargetException;
import java.lang.InstantiationException;

public class PyReflectedConstructor extends PyReflectedFunction {

    public PyReflectedConstructor(String name) {
        super(name);
        __name__ = name;
        argslist = new ReflectedArgs[1];
        nargs = 0;
    }

    public PyReflectedConstructor(Constructor c) {
        this(c.getDeclaringClass().getName());
        addConstructor(c);
    }

    private ReflectedArgs makeArgs(Constructor m) {
        return new ReflectedArgs(m, m.getParameterTypes(), m.getDeclaringClass(), true);
    }

    public void addConstructor(Constructor m) {
        int mods = m.getModifiers();
        // Only add public methods unless we're overriding
        if (!Modifier.isPublic(mods) && Options.respectJavaAccessibility)
            return;
        addArgs(makeArgs(m));
    }

    // xxx temporary solution, type ctr will go through __new__ ...
    PyObject make(PyObject[] args, String[] keywords) {
        ReflectedArgs[] argsl = argslist;
        ReflectedCallData callData = new ReflectedCallData();
        Object method = null;
        boolean consumes_keywords = false;
        int nkeywords = keywords.length;
        PyObject[] allArgs = null;
        // Check for a matching constructor to call
        if (nargs > 0) { // PyArgsKeywordsCall signature, if present, is the first
            if (argsl[0].matches(null, args, keywords, callData)) {
                method = argsl[0].data;
                consumes_keywords = argsl[0].flags == ReflectedArgs.PyArgsKeywordsCall;
            } else {
                allArgs = args;
                int i = 1;
                if (nkeywords > 0) {
                    args = new PyObject[allArgs.length - nkeywords];
                    System.arraycopy(allArgs, 0, args, 0, args.length);
                    i = 0;
                }
                for (; i < nargs; i++) {
                    ReflectedArgs rargs = argsl[i];
                    if (rargs.matches(null, args, Py.NoKeywords, callData)) {
                        method = rargs.data;
                        break;
                    }
                }
            }
        }
        // Throw an error if no valid set of arguments
        if (method == null) {
            throwError(callData.errArg, args.length, true /* xxx? */, false);
        }
        // Do the actual constructor call
        PyObject obj = null;
        try {
            obj = (PyObject)((Constructor<?>)method).newInstance(callData.getArgsArray());
        } catch (Throwable t) {
            throw Py.JavaError(t);
        }
        if (!consumes_keywords) {
            int offset = args.length;
            for (int i = 0; i < nkeywords; i++) {
                obj.__setattr__(keywords[i], allArgs[i + offset]);
            }
        }
        return obj;
    }

    public PyObject __call__(PyObject self, PyObject[] args, String[] keywords) {
        if (self == null) {
            throw Py.TypeError("invalid self argument to constructor");
        }
        Class<?> javaClass = self.getType().getProxyType();

        Class<?> declaringClass = argslist[0].declaringClass;
        if (!declaringClass.isAssignableFrom(javaClass)) {
            throw Py.TypeError("self invalid - must implement: " + declaringClass.getName());
        }

        if (self.javaProxy != null) {
            Class<?> sup = javaClass;
            if (PyProxy.class.isAssignableFrom(sup)) {
                sup = sup.getSuperclass();
            }
            throw Py.TypeError("instance already instantiated for " + sup.getName());
        }
        ReflectedCallData callData = new ReflectedCallData();
        Object method = null;
        // Remove keyword args
        int nkeywords = keywords.length;
        PyObject[] allArgs = args;
        if (nkeywords > 0) {
            args = new PyObject[allArgs.length - nkeywords];
            System.arraycopy(allArgs, 0, args, 0, args.length);
        }
        // Check for a matching constructor to call
        int n = nargs;
        for (int i = 0; i < n; i++) {
            ReflectedArgs rargs = argslist[i];
            if (rargs.matches(null, args, Py.NoKeywords, callData)) {
                method = rargs.data;
                break;
            }
        }
        // Throw an error if no valid set of arguments
        if (method == null) {
            throwError(callData.errArg, args.length, false, false);
        }
        // Do the actual constructor call
        constructProxy(self, (Constructor<?>)method, callData.getArgsArray(), javaClass);

        // Do setattr's for keyword args
        int offset = args.length;
        for (int i = 0; i < nkeywords; i++) {
            self.__setattr__(keywords[i], allArgs[i + offset]);
        }
        return Py.None;
    }

    public PyObject __call__(PyObject[] args, String[] keywords) {
        if (args.length < 1) {
            throw Py.TypeError("constructor requires self argument");
        }
        PyObject[] newArgs = new PyObject[args.length - 1];
        System.arraycopy(args, 1, newArgs, 0, newArgs.length);
        return __call__(args[0], newArgs, keywords);
    }

    protected void constructProxy(PyObject obj, Constructor<?> ctor, Object[] args, Class<?> proxy) {
        // Do the actual constructor call
        Object jself = null;
        ThreadState ts = Py.getThreadState();
        try {
            ts.pushInitializingProxy(obj);
            try {
                jself = ctor.newInstance(args);
            } catch (InvocationTargetException e) {
                if (e.getTargetException() instanceof InstantiationException) {
                    Class<?> sup = proxy.getSuperclass();
                    String msg = "Constructor failed for Java superclass";
                    if (sup != null) {
                        msg += " " + sup.getName();
                    }
                    throw Py.TypeError(msg);
                } else
                    throw Py.JavaError(e);
            } catch (Throwable t) {
                throw Py.JavaError(t);
            }
        } finally {
            ts.popInitializingProxy();
        }
        obj.javaProxy = jself;
    }

    public String toString() {
        // printArgs();
        return "<java constructor " + __name__ + " " + Py.idstr(this) + ">";
    }
}
