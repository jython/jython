// Copyright © Corporation for National Research Initiatives
// Copyright 2000 Samuele Pedroni

package org.python.core;
import java.io.*;
import java.util.StringTokenizer;
import java.util.Hashtable;

public class SyspathJavaLoader extends ClassLoader
{

    public InputStream getResourceAsStream(String res) {
        ClassLoader classLoader = Py.getSystemState().getClassLoader();
        if (classLoader != null) return classLoader.getResourceAsStream(res);

        classLoader = this.getClass().getClassLoader();

        InputStream ret;

        if (classLoader != null) ret = classLoader.getResourceAsStream(res);
        else ret = ClassLoader.getSystemResourceAsStream(res);

        if (ret != null) return ret;

        if(res.charAt(0) == '/') res = res.substring(1);

        res.replace('/',File.separatorChar);

        PyList path = Py.getSystemState().path;
        for (int i=0; i < path.__len__(); i++) {
            String dir = path.get(i).__str__().toString();
            if (dir.length() == 0) dir = null;
            try {
                return new BufferedInputStream(
                       new FileInputStream(new File(dir,res)));
            }
            catch (IOException e) {
                continue;
            }
        }

        return null;
    }

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
        try {
          return Class.forName(name);
        }
        catch(ClassNotFoundException e) {
        }

        Class c = findLoadedClass(name);
        if (c != null)
         return c;

        /* previously: if
            Options.extendedClassLoader &&
            // KLUDGE ALERT: without this test, running jpython
            // interactively from the build directory will fail with
            // ClassCircularityError or LinkageError.  There's gotta be a
            // better way.
            !name.startsWith("org.python")
        */
        {
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
                    fis.close();
                    return loadClassFromBytes(name, buffer);
                }
                catch (IOException e) {
                    continue;
                }
            }
        }

        // couldn't find the .class file on sys.path
        throw new ClassNotFoundException(name);
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
            if (dir.length() == 0)
                dir = null;
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

}
