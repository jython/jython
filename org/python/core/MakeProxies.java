// Copyright © Corporation for National Research Initiatives

package org.python.core;

import java.lang.reflect.*;
import java.util.Vector;
import java.io.*;


class MakeProxies 
{
    public static Class makeAdapter(Class c) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        String name;
        try {
            name = org.python.compiler.AdapterMaker.makeAdapter(
                c.getName(), bytes);
        } catch (Exception exc) {
            throw Py.JavaError(exc);
        }

        Class pc = BytecodeLoader.makeClass(name, bytes.toByteArray());
        String dir = Options.proxyCacheDirectory;
        if (dir != null) {
            try {
                OutputStream file = org.python.compiler.ProxyMaker.getFile(
                    dir, name);
                bytes.writeTo(file);
            } catch (Throwable t) {
                // Allow caching to fail silently
            }
        }
        return pc;
    }

    private static final String proxyPrefix = "org.python.proxies.";
    private static int proxyNumber = 0;

    public static synchronized Class makeProxy(Class c, Vector vinterfaces,
                                               String name, PyObject dict)
    {
        String[] interfaces = new String[vinterfaces.size()];
        for (int i=0; i<vinterfaces.size(); i++) {
            interfaces[i] = ((Class)vinterfaces.elementAt(i)).getName();
//             System.err.println("interface: " + interfaces[i]);
        }
        String proxyName = proxyPrefix + name + "$" + proxyNumber++;

        org.python.compiler.JavaMaker jm =
            new org.python.compiler.JavaMaker(c, interfaces, name,
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
            Class pc = BytecodeLoader.makeClass(jm.myClass,
                                                bytes.toByteArray());
            return pc;
        } catch (Exception exc) {
            throw Py.JavaError(exc);
        }
    }
}
