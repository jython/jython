// Copyright (c) Corporation for National Research Initiatives

package org.python.core;

import java.io.ByteArrayOutputStream;
import java.util.Vector;

import org.python.compiler.AdapterMaker;
import org.python.compiler.JavaMaker;

class MakeProxies {
    private static Class makeClass(Class referent, Vector secondary,
            String name, ByteArrayOutputStream bytes) {
        Vector referents = null;

        if (secondary != null) {
            if (referent != null) {
                secondary.insertElementAt(referent, 0);
            }
            referents = secondary;
        } else {
            if (referent != null) {
                referents = new Vector();
                referents.addElement(referent);
            }
        }

        return BytecodeLoader.makeClass(name, referents, bytes.toByteArray());
    }

    public static Class makeAdapter(Class c) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        String name;
        try {
            name = AdapterMaker.makeAdapter(c, bytes);
        } catch (Exception exc) {
            throw Py.JavaError(exc);
        }

        Py.saveClassFile(name, bytes);

        Class pc = makeClass(c, null, name, bytes);
        return pc;
    }

    private static final String proxyPrefix = "org.python.proxies.";

    private static int proxyNumber = 0;

    public static synchronized Class makeProxy(Class superclass,
            Vector vinterfaces, String className, String proxyName,
            PyObject dict) {
        Class[] interfaces = new Class[vinterfaces.size()];

        for (int i = 0; i < vinterfaces.size(); i++) {
            interfaces[i] = (Class) vinterfaces.elementAt(i);
        }
        String fullProxyName = proxyPrefix + proxyName + "$" + proxyNumber++;
        String pythonModuleName;
        PyObject mn = dict.__finditem__("__module__");
        if (mn == null) {
            pythonModuleName = "foo";
        } else {
            pythonModuleName = (String) mn.__tojava__(String.class);
        }
        JavaMaker jm = new JavaMaker(superclass, interfaces, className,
                pythonModuleName, fullProxyName, dict);
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
