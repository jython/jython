// Copyright (c) Corporation for National Research Initiatives

package org.python.core;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import org.python.compiler.AdapterMaker;
import org.python.compiler.JavaMaker;
import org.python.core.util.StringUtil;

class MakeProxies {

    private static Class<?> makeClass(Class<?> referent,
                                      List<Class<?>> secondary,
                                      String name,
                                      ByteArrayOutputStream bytes) {
        List<Class<?>> referents = null;
        if (secondary != null) {
            if (referent != null) {
                secondary.add(0, referent);
            }
            referents = secondary;
        } else if (referent != null) {
            referents = new ArrayList<Class<?>>(1);
            referents.add(referent);
        }

        return BytecodeLoader.makeClass(name, referents, bytes.toByteArray());
    }

    public static Class<?> makeAdapter(Class<?> c) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        String name;
        try {
            name = AdapterMaker.makeAdapter(c, bytes);
        } catch (Exception exc) {
            throw Py.JavaError(exc);
        }

        Py.saveClassFile(name, bytes);

        return makeClass(c, null, name, bytes);
    }

    private static final String proxyPrefix = "org.python.proxies.";

    private static int proxyNumber = 0;

    public static synchronized Class<?> makeProxy(Class<?> superclass,
            List<Class<?>> vinterfaces, String className, String proxyName,
            PyObject dict) {
        Class<?>[] interfaces = vinterfaces.toArray(new Class<?>[vinterfaces.size()]);
        String fullProxyName;
        PyObject customProxyName = dict.__finditem__("__javaname__");
        if (customProxyName != null) {
            fullProxyName = Py.tojava(customProxyName, String.class);
            if (!StringUtil.isJavaClassName(fullProxyName)) {
                throw Py.TypeError(fullProxyName + " isn't a valid Java class name.  Classes " +
                    "must be valid Java identifiers: " +
                    "http://java.sun.com/docs/books/jls/second_edition/html/lexical.doc.html#40625");
            }
        } else {
            fullProxyName = proxyPrefix + proxyName + "$" + proxyNumber++;
        }
        String pythonModuleName;
        PyObject mn = dict.__finditem__("__module__");
        if (mn == null) {
            pythonModuleName = "foo";
        } else {
            pythonModuleName = Py.tojava(mn, String.class);
        }
        JavaMaker jm = new JavaMaker(superclass,
                                     interfaces,
                                     className,
                                     pythonModuleName,
                                     fullProxyName,
                                     dict);
        try {
            jm.build();
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            jm.classfile.write(bytes);
            Py.saveClassFile(fullProxyName, bytes);

            return makeClass(superclass, vinterfaces, jm.myClass, bytes);
        } catch (Exception exc) {
            throw Py.JavaError(exc);
        }
    }
}
