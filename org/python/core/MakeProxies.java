// Copyright © Corporation for National Research Initiatives

package org.python.core;

import java.lang.reflect.*;
import java.util.Vector;
import java.util.Hashtable;
import java.io.*;
import org.python.compiler.JavaMaker;
import org.python.compiler.AdapterMaker;
import org.python.compiler.ProxyMaker;


class MakeProxies 
{
    private static Class makeClass(Class referent, String name,
                                   ByteArrayOutputStream bytes)
    {
        BytecodeLoader bcl;
        ClassLoader cl;
        // Use the superclass's class loader if it is a BytecodeLoader.
        // referent might be null if we're deriving from an interface.  In
        // that case, this may or may not be the RTTD.
        if (referent != null &&
            (cl = referent.getClassLoader()) != null &&
            cl instanceof BytecodeLoader)
        {
            bcl = (BytecodeLoader)cl;
        }
        else
            bcl = imp.getSyspathJavaLoader();
        return bcl.makeClass(name, bytes.toByteArray());
    }

    public static Class makeAdapter(Class c) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        String name;
        try {
            name = AdapterMaker.makeAdapter(c, bytes);
        }
        catch (Exception exc) {
            throw Py.JavaError(exc);
        }

        Py.saveClassFile(name, bytes);

        Class pc = makeClass(c, name, bytes);
        return pc;
    }

    private static final String proxyPrefix = "org.python.proxies.";
    private static int proxyNumber = 0;

    public static synchronized Class makeProxy(Class superclass,
                                               Vector vinterfaces,
                                               String name,
                                               PyObject dict)
    {
        Class[] interfaces = new Class[vinterfaces.size()];

        for (int i=0; i<vinterfaces.size(); i++) {
            interfaces[i] = (Class)vinterfaces.elementAt(i);
        }
        String proxyName = proxyPrefix + name + "$" + proxyNumber++;
        String pythonModuleName;
        PyObject mn=dict.__finditem__("__module__");
        if (mn==null)
            pythonModuleName = "foo";
        else 
            pythonModuleName = (String)mn.__tojava__(String.class);
         
        JavaMaker jm = new JavaMaker(superclass, interfaces, name,
                                     pythonModuleName, proxyName, dict);
        try {
            jm.build();
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            jm.classfile.write(bytes);
            Py.saveClassFile(proxyName, bytes);

            return makeClass(superclass, jm.myClass, bytes);
        }
        catch (Exception exc) {
            throw Py.JavaError(exc);
        }
    }
}
