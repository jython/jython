// Copyright © Corporation for National Research Initiatives

package org.python.core;

import java.lang.reflect.*;
import java.util.Vector;
import java.util.Hashtable;
import java.io.*;
import org.python.compiler.*;


class MakeProxies 
{
    private static Class makeClass(Class referent, String name,
                                   ByteArrayOutputStream bytes)
    {
        BytecodeLoader bcl;
        ClassLoader cl = referent.getClassLoader();
        if (cl != null && cl instanceof BytecodeLoader)
            bcl = (BytecodeLoader)cl;
        else
            bcl = new BytecodeLoader();
        return bcl.makeClass(name, bytes.toByteArray());
    }

    public static Class makeAdapter(Class c) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        String name;
        try {
            name = AdapterMaker.makeAdapter(c.getName(), bytes);
        }
        catch (Exception exc) {
            throw Py.JavaError(exc);
        }

        Class pc = makeClass(c, name, bytes);
        String dir = Options.proxyCacheDirectory;

        if (dir != null) {
            try {
                OutputStream file = ProxyMaker.getFile(dir, name);
                bytes.writeTo(file);
            }
            // Allow caching to fail silently
            catch (Throwable t) {}
        }
        return pc;
    }

    private static final String proxyPrefix = "org.python.proxies.";
    private static int proxyNumber = 0;

    public static synchronized Class makeProxy(Class superclass,
                                               Vector vinterfaces,
                                               String name,
                                               PyObject dict)
    {
        String[] interfaces = new String[vinterfaces.size()];

        for (int i=0; i<vinterfaces.size(); i++) {
            interfaces[i] = ((Class)vinterfaces.elementAt(i)).getName();
//             System.err.println("interface: " + interfaces[i]);
        }
        String proxyName = proxyPrefix + name + "$" + proxyNumber++;
        JavaMaker jm = new JavaMaker(superclass, interfaces, name,
                                     "foo", proxyName, dict);
        try {
            jm.build();
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            jm.classfile.write(bytes);
            // debugging
//             String filename = "/tmp/jpython/test/"+proxyName+".class";
//             System.err.println("filename: "+filename);
//             bytes.writeTo(new java.io.FileOutputStream(filename));
            // end debugging
            return makeClass(superclass, jm.myClass, bytes);
        }
        catch (Exception exc) {
            throw Py.JavaError(exc);
        }
    }
}
