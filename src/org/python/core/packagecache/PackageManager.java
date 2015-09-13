// Copyright (c) Corporation for National Research Initiatives
// Copyright 2000 Samuele Pedroni

package org.python.core.packagecache;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;
import org.python.core.Py;
import org.python.core.PyJavaPackage;
import org.python.core.PyList;
import org.python.core.PyObject;
import org.python.core.PyStringMap;
import org.python.core.util.FileUtil;

import java.io.IOException;
import java.io.InputStream;

/**
 * Abstract package manager.
 */
public abstract class PackageManager extends Object {

    public PyJavaPackage topLevelPackage;

    public PackageManager() {
        this.topLevelPackage = new PyJavaPackage("", this);
    }

    abstract public Class findClass(String pkg, String name, String reason);

    public Class findClass(String pkg, String name) {
        return findClass(pkg, name, "java class");
    }

    public void notifyPackageImport(String pkg, String name) {
    }

    /**
     * Dynamically check if pkg.name exists as java pkg in the controlled
     * hierarchy. Should be overriden.
     *
     * @param pkg parent pkg name
     * @param name candidate name
     * @return true if pkg exists
     */
    public abstract boolean packageExists(String pkg, String name);

    /**
     * Reports the specified package content names. Should be overriden. Used by
     * {@link PyJavaPackage#__dir__} and {@link PyJavaPackage#fillDir}.
     *
     * @return resulting list of names (PyList of PyString)
     * @param jpkg queried package
     * @param instantiate if true then instatiate reported names in package dict
     * @param exclpkgs exclude packages (just when instantiate is false)
     */
    public abstract PyList doDir(PyJavaPackage jpkg, boolean instantiate,
            boolean exclpkgs);

    /**
     * Append a directory to the list of directories searched for java packages
     * and java classes.
     *
     * @param dir A directory.
     */
    public abstract void addDirectory(java.io.File dir);

    /**
     * Append a directory to the list of directories searched for java packages
     * and java classes.
     *
     * @param dir A directory name.
     */
    public abstract void addJarDir(String dir, boolean cache);

    /**
     * Append a jar file to the list of locations searched for java packages and
     * java classes.
     *
     * @param jarfile A directory name.
     */
    public abstract void addJar(String jarfile, boolean cache);

    /**
     * Basic helper implementation of {@link #doDir}. It merges information
     * from jpkg {@link PyJavaPackage#clsSet} and {@link PyJavaPackage#__dict__}.
     */
    protected PyList basicDoDir(PyJavaPackage jpkg, boolean instantiate,
            boolean exclpkgs) {
        PyStringMap dict = jpkg.__dict__;
        PyStringMap cls = jpkg.clsSet;

        if (!instantiate) {
            PyList ret = cls.keys();
            PyList dictKeys = dict.keys();

            for (PyObject name : dictKeys.asIterable()) {
                if (!cls.has_key(name)) {
                    if (exclpkgs && dict.get(name) instanceof PyJavaPackage) {
                        continue;
                    }
                    ret.append(name);
                }
            }

            return ret;
        }

        for (PyObject pyname : cls.keys().asIterable()) {
            if (!dict.has_key(pyname)) {
                String name = pyname.toString();
                jpkg.addClass(name, Py.findClass(jpkg.__name__ + "." + name));
            }
        }

        return dict.keys();
    }

    /**
     * Helper merging list2 into list1. Returns list1.
     */
    protected PyList merge(PyList list1, PyList list2) {
        for (PyObject name : list2.asIterable()) {
            list1.append(name);
        }

        return list1;
    }

    public PyObject lookupName(String name) {
        PyObject top = this.topLevelPackage;
        do {
            int dot = name.indexOf('.');
            String firstName = name;
            String lastName = null;
            if (dot != -1) {
                firstName = name.substring(0, dot);
                lastName = name.substring(dot + 1, name.length());
            }
            firstName = firstName.intern();
            top = top.__findattr__(firstName);
            if (top == null) {
                return null;
            }
            // ??pending: test for jpkg/jclass?
            name = lastName;
        } while (name != null);
        return top;
    }

    /**
     * Creates package/updates statically known classes info. Uses
     * {@link PyJavaPackage#addPackage(java.lang.String, java.lang.String) },
     * {@link PyJavaPackage#addPlaceholders}.
     *
     * @param name package name
     * @param classes comma-separated string
     * @param jarfile involved jarfile; can be null
     * @return created/updated package
     */
    public PyJavaPackage makeJavaPackage(String name, String classes,
            String jarfile) {
        PyJavaPackage p = this.topLevelPackage;
        if (name.length() != 0) {
            p = p.addPackage(name, jarfile);
        }

        if (classes != null) {
            p.addPlaceholders(classes);
        }

        return p;
    }


    private static class AccessVisitor extends ClassVisitor {

        private int class_access;

        public AccessVisitor() throws IOException {
            super(Opcodes.ASM5);
        }

        @Override
        public void visit(int version, int access, String name,
                          String signature, String superName, String[] interfaces) {
            class_access = access;
        }

        public int getClassAccess() {
            return class_access;
        }
    }

    /**
     * Check that a given stream is a valid Java .class file. And return its
     * access permissions as an int.
     */
    static protected int checkAccess(java.io.InputStream cstream)
            throws IOException {
        try {
            ClassReader reader = new ClassReader(cstream);
            AccessVisitor visitor = new AccessVisitor();
            reader.accept(visitor, 0);
            return visitor.getClassAccess();
        } catch (RuntimeException e) {
            return -1;
        }
    }

}
