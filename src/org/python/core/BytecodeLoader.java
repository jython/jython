// Copyright (c) Corporation for National Research Initiatives
package org.python.core;

import java.security.SecureClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/**
 * Utility class for loading of compiled python modules and java classes defined in python modules.
 */
public class BytecodeLoader {

    /**
     * Turn the java byte code in data into a java class.
     * 
     * @param name
     *            the name of the class
     * @param data
     *            the java byte code.
     * @param referents
     *            superclasses and interfaces that the new class will reference.
     */
    public static Class makeClass(String name, byte[] data, Class... referents) {
        Loader loader = new Loader();
        for (int i = 0; i < referents.length; i++) {
            try {
                ClassLoader cur = referents[i].getClassLoader();
                if (cur != null) {
                    loader.addParent(cur);
                }
            } catch (SecurityException e) {}
        }
        return loader.loadClassFromBytes(name, data);
    }

    /**
     * Turn the java byte code in data into a java class.
     * 
     * @param name
     *            the name of the class
     * @param referents
     *            superclasses and interfaces that the new class will reference.
     * @param data
     *            the java byte code.
     */
    public static Class makeClass(String name, Vector<Class> referents, byte[] data) {
        if (referents != null) {
            return makeClass(name, data, referents.toArray(new Class[0]));
        }
        return makeClass(name, data);
    }

    /**
     * Turn the java byte code for a compiled python module into a java class.
     * 
     * @param name
     *            the name of the class
     * @param data
     *            the java byte code.
     */
    public static PyCode makeCode(String name, byte[] data, String filename) {
        try {
            Class c = makeClass(name, data);
            @SuppressWarnings("unchecked")
            Object o = c.getConstructor(new Class[] {String.class})
                    .newInstance(new Object[] {filename});
            return ((PyRunnable)o).getMain();
        } catch (Exception e) {
            throw Py.JavaError(e);
        }
    }

    public static class Loader extends SecureClassLoader {

        private List<ClassLoader> parents = new ArrayList<ClassLoader>();

        public Loader() {
            parents.add(imp.getSyspathJavaLoader());
        }

        public void addParent(ClassLoader referent) {
            if (!parents.contains(referent)) {
                parents.add(0, referent);
            }
        }

        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            Class c = findLoadedClass(name);
            if (c != null) {
                return c;
            }
            for (ClassLoader loader : parents) {
                try {
                    return loader.loadClass(name);
                } catch (ClassNotFoundException cnfe) {}
            }
            // couldn't find the .class file on sys.path
            throw new ClassNotFoundException(name);
        }

        public Class loadClassFromBytes(String name, byte[] data) {
            Class c = defineClass(name, data, 0, data.length, getClass().getProtectionDomain());
            resolveClass(c);
            Compiler.compileClass(c);
            return c;
        }
    }
}
