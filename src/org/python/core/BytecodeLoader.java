// Copyright (c) Corporation for National Research Initiatives

package org.python.core;

import java.security.SecureClassLoader;
import java.util.Vector;

/**
 * Utility class for loading of compiled python modules and java classes defined
 * in python modules.
 */
public class BytecodeLoader {

    static Vector init() {
        Vector parents = new Vector();
        parents.addElement(imp.getSyspathJavaLoader());
        return parents;
    }

    static Class findParentClass(Vector parents, String name)
            throws ClassNotFoundException {
        for (int i = 0; i < parents.size(); i++) {
            try {
                return ((ClassLoader) parents.elementAt(i)).loadClass(name);
            } catch (ClassNotFoundException e) {
            }
        }
        // couldn't find the .class file on sys.path
        throw new ClassNotFoundException(name);
    }

    static void compileClass (Class c) {
        Compiler.compileClass(c);
    }

    private static Loader makeLoader() {
            return new Loader();
    }

    /**
     * Turn the java byte code in data into a java class.
     * 
     * @param name the name of the class
     * @param referents a list of superclass and interfaces that the new class
     *            will reference.
     * @param data the java byte code.
     */
    public static Class makeClass(String name, Vector referents, byte[] data) {
        Loader loader = makeLoader();

        if (referents != null) {
            for (int i = 0; i < referents.size(); i++) {
                try {
                    Class cls = (Class) referents.elementAt(i);
                    ClassLoader cur = cls.getClassLoader();
                    if (cur != null) {
                        loader.addParent(cur);
                    }
                } catch (SecurityException e) {
                }
            }
        }
        return loader.loadClassFromBytes(name, data);
    }

    /**
     * Turn the java byte code for a compiled python module into a java class.
     * 
     * @param name the name of the class
     * @param data the java byte code.
     */
    public static PyCode makeCode(String name, byte[] data, String filename) {
        try {
            Class c = makeClass(name, null, data);
            Object o = c.getConstructor(new Class[] {String.class})
                    .newInstance(new Object[] {filename});
            return ((PyRunnable)o).getMain();
        } catch(Exception e) {
            throw Py.JavaError(e);
        }
    }
    
    private static class Loader extends SecureClassLoader {
        private Vector parents;

        public Loader() {
            this.parents = BytecodeLoader.init();
        }

        public void addParent(ClassLoader referent) {
            if (!this.parents.contains(referent)) {
                this.parents.add(0, referent);
            }
        }

        // override from abstract base class
        protected Class loadClass(String name, boolean resolve)
                throws ClassNotFoundException {
            Class c = findLoadedClass(name);
            if (c != null) {
                return c;
            }
            return BytecodeLoader.findParentClass(this.parents, name);
        }

        public Class loadClassFromBytes(String name, byte[] data) {
            Class c = defineClass(name, data, 0, data.length, this.getClass()
                    .getProtectionDomain());
            resolveClass(c);
            BytecodeLoader.compileClass(c);
            return c;
        }
    }
}
