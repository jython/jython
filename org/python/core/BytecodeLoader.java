// Copyright © Corporation for National Research Initiatives

package org.python.core;
import java.io.*;
import java.util.StringTokenizer;

public class BytecodeLoader extends ClassLoader
{
    // override from abstract base class
    protected Class loadClass(String name, boolean resolve)
        throws ClassNotFoundException
    {
//         System.err.println("loadClass("+name+", "+resolve+")");
        // First, if the Python runtime system has a default class loader,
        // defer to it.
        ClassLoader classLoader = Py.getSystemState().getClassLoader();
        if (classLoader != null)
            return classLoader.loadClass(name);
        // Search the sys.path for a .class file matching the named class.
        // TBD: This registry option is temporary.
        if (Options.extendedClassLoader) {
            PyList path = Py.getSystemState().path;
            for (int i=0; i < path.__len__(); i++) {
                String dir = path.get(i).__str__().toString();
                FileInputStream fis = open(dir, name);
                if (fis == null)
                    continue;
                try {
                    int size = fis.available();
                    byte[] buffer = new byte[size];
                    fis.read(buffer);
                    return loadClassFromBytes(name, buffer);
                }
                catch (IOException e) {
                    continue;
                }
            }
        }
        // couldn't find the .class file on sys.path, so give the system
        // class loader the final shot at it
        return findSystemClass(name);
    }

    private FileInputStream open(String dir, String name) {
        String accum = "";
        boolean first = true;
        for (StringTokenizer t = new StringTokenizer(name, ".");
             t.hasMoreTokens();)
        {
            String token = t.nextToken();
            if (!first)
                accum += File.separator;
            accum += token;
            first = false;
        }
        try {
            if (dir == "")
                dir = ".";
            return new FileInputStream(new File(dir, accum+".class"));
        }
        catch (FileNotFoundException e) {
            return null;
        }
    }

    private Class loadClassFromBytes(String name, byte[] data) {
//         System.err.println("loadClassFromBytes("+name+", byte[])");
        Class c = defineClass(name, data, 0, data.length);
        resolveClass(c);
        // This method has caused much trouble.  Using it breaks jdk1.2rc1
        // Not using it can make SUN's jdk1.1.6 JIT slightly unhappy.
        // Don't use by default, but allow python.options.compileClass to
        // override
        if (!Options.skipCompile) {
//             System.err.println("compile: "+name);
            Compiler.compileClass(c);
        }
        return c;
    }

    public PyCode loadBytes(String name, byte[] data)
        throws InstantiationException, IllegalAccessException
    {
        Class c = loadClassFromBytes(name, data);
        return ((PyRunnable)c.newInstance()).getMain();
    }

    public Class makeClass(String name, byte[] data) {
        return loadClassFromBytes(name, data);
    }

    public static PyCode makeCode(String name, byte[] data)
    {
        try {
            return new BytecodeLoader().loadBytes(name, data);
        }
        catch (Exception e) {
            throw Py.JavaError(e);
        }
    }
}
