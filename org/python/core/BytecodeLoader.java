// Copyright © Corporation for National Research Initiatives

package org.python.core;
import java.io.*;
import java.util.StringTokenizer;
import java.util.Hashtable;
import java.util.Vector;

public class BytecodeLoader extends ClassLoader
{

    private Vector parents;
    
    public BytecodeLoader() {
        this(null);
    }
    
    public BytecodeLoader(Vector referents) {
        parents = new Vector();
        
        parents.addElement(imp.getSyspathJavaLoader());
        
        if (referents != null) {
            for(int i = 0; i < referents.size(); i++) {
                try {
                    ClassLoader cur = ((Class)referents.elementAt(i)).getClassLoader();
                    if (cur != null && !parents.contains(cur)) {
                        parents.addElement(cur);
                    }
                } catch(SecurityException e) {
                }
            }
        }
        
    }
  
    // override from abstract base class
    protected Class loadClass(String name, boolean resolve)
        throws ClassNotFoundException
    {        
        Class c = findLoadedClass(name);
        if (c != null)
         return c;
        
        for (int i = 0; i < parents.size(); i++) {
            try {
                return ((ClassLoader)parents.elementAt(i)).loadClass(name);
            } catch(ClassNotFoundException e) {
            }
        }

        
        // couldn't find the .class file on sys.path        
        throw new ClassNotFoundException(name);
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
