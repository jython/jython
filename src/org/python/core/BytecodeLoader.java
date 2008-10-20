// Copyright (c) Corporation for National Research Initiatives
package org.python.core;

import java.security.SecureClassLoader;
import java.util.List;

import org.python.objectweb.asm.ClassReader;
import org.python.util.Generic;

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
    public static Class<?> makeClass(String name, byte[] data, Class<?>... referents) {
        Loader loader = new Loader();
        for (Class<?> referent : referents) {
            try {
                ClassLoader cur = referent.getClassLoader();
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
    public static Class<?> makeClass(String name, List<Class<?>> referents, byte[] data) {
        if (referents != null) {
            return makeClass(name, data, referents.toArray(new Class[referents.size()]));
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
            Class<?> c = makeClass(name, data);
            Object o = c.getConstructor(new Class[] {String.class})
                    .newInstance(new Object[] {filename});
            return ((PyRunnable)o).getMain();
        } catch (Exception e) {
            throw Py.JavaError(e);
        }
    }

    public static class Loader extends SecureClassLoader {

        private List<ClassLoader> parents = Generic.list();

        public Loader() {
            parents.add(imp.getSyspathJavaLoader());
        }

        public void addParent(ClassLoader referent) {
            if (!parents.contains(referent)) {
                parents.add(0, referent);
            }
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            Class<?> c = findLoadedClass(name);
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

        public Class<?> loadClassFromBytes(String name, byte[] data) {
            if (name.endsWith("$py")) {
                try {
                    // Get the real class name: we might request a 'bar'
                    // Jython module that was compiled as 'foo.bar', or
                    // even 'baz.__init__' which is compiled as just 'baz'
                    ClassReader cr = new ClassReader(data);
                    name = cr.getClassName().replace('/', '.');
                } catch (RuntimeException re) {
                    // Probably an invalid .class, fallback to the
                    // specified name
                }
            }
            Class<?> c = defineClass(name, data, 0, data.length, getClass().getProtectionDomain());
            resolveClass(c);
            Compiler.compileClass(c);
            return c;
        }
    }
}
