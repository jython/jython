// Copyright (c) Corporation for National Research Initiatives

package org.python.core;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import org.python.compiler.APIVersion;
import org.python.compiler.AdapterMaker;
import org.python.compiler.CustomMaker;
import org.python.compiler.JavaMaker;

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
        AdapterMaker maker = new AdapterMaker(proxyPrefix + c.getName(), c);
        try {
            maker.build(bytes);
        } catch (Exception exc) {
            throw Py.JavaError(exc);
        }

        Py.saveClassFile(maker.myClass, bytes);

        return makeClass(c, null, maker.myClass, bytes);
    }

    private static final String proxyPrefix = "org.python.proxies.";

    private static int proxyNumber = 0;

    public static synchronized Class<?> makeProxy(Class<?> superclass,
            List<Class<?>> vinterfaces, String className, String proxyName,
            PyObject dict) {
        JavaMaker javaMaker = null;
        
        Class<?>[] interfaces = vinterfaces.toArray(new Class<?>[vinterfaces.size()]);
        String fullProxyName = proxyPrefix + proxyName + "$" + proxyNumber++;
        String pythonModuleName;
        PyObject module = dict.__finditem__("__module__");
        if (module == null) {
            pythonModuleName = "foo"; // FIXME Really, module name foo?
        } else {
            pythonModuleName = (String) module.__tojava__(String.class);
        }
        
        // Grab the proxy maker from the class if it exists, and if it does, use the proxy class
        // name from the maker
        PyObject userDefinedProxyMaker = dict.__finditem__("__proxymaker__");
        if (userDefinedProxyMaker != null) {
            if (module == null) {
                throw Py.TypeError("Classes using __proxymaker__ must define __module__");
            }
            PyObject[] args = Py.javas2pys(superclass, interfaces, className, pythonModuleName, fullProxyName, dict);
            javaMaker = Py.tojava(userDefinedProxyMaker.__call__(args), JavaMaker.class);
            if (javaMaker instanceof CustomMaker) {
                // This hook hack is necessary because of the divergent behavior of how classes
                // are made - we want to allow CustomMaker complete freedom in constructing the
                // class, including saving any bytes.
                CustomMaker customMaker = (CustomMaker) javaMaker;
                return customMaker.makeClass();
            }
        }
        if (javaMaker == null) {
            javaMaker = new JavaMaker(superclass,
                        interfaces,
                        className,
                        pythonModuleName,
                        fullProxyName,
                        dict);
        }
        
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            javaMaker.build(bytes);
            Py.saveClassFile(javaMaker.myClass, bytes);

            return makeClass(superclass, vinterfaces, javaMaker.myClass, bytes);
        } catch (Exception exc) {
            throw Py.JavaError(exc);
        }
    }
}
