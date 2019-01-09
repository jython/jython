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

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.StringTokenizer;

/**
 * Abstract package manager.
 */
public abstract class PackageManager extends Object {

    /** Nominal top-level package of all (Java) packages, containing "java", "com", "org", etc.. */
    public PyJavaPackage topLevelPackage;

    public PackageManager() {
        this.topLevelPackage = new PyJavaPackage("", this);
    }

    abstract public Class findClass(String pkg, String name, String reason);

    public Class findClass(String pkg, String name) {
        return findClass(pkg, name, "java class");
    }

    public void notifyPackageImport(String pkg, String name) {}

    /**
     * Dynamically check if pkg.name exists as java pkg in the controlled hierarchy. Should be
     * overridden.
     *
     * @param pkg parent pkg name
     * @param name candidate name
     * @return true if pkg exists
     */
    public abstract boolean packageExists(String pkg, String name);

    /**
     * Reports the specified package content names. Should be overridden. Used by
     * {@link PyJavaPackage#__dir__} and {@link PyJavaPackage#fillDir}.
     *
     * @return resulting list of names (PyList of PyString)
     * @param jpkg queried package
     * @param instantiate if true then instatiate reported names in package dict
     * @param exclpkgs exclude packages (just when instantiate is false)
     */
    public abstract PyList doDir(PyJavaPackage jpkg, boolean instantiate, boolean exclpkgs);

    /**
     * Append a directory to the list of directories searched for java packages and java classes.
     *
     * @param dir A directory.
     */
    public abstract void addDirectory(java.io.File dir);

    /**
     * Append a directory to the list of directories searched for java packages and java classes.
     *
     * @param dir A directory name.
     */
    public abstract void addJarDir(String dir, boolean cache);

    /**
     * Append a jar file to the list of locations searched for java packages and java classes.
     *
     * @param jarfile A directory name.
     */
    public abstract void addJar(String jarfile, boolean cache);

    /**
     * Basic helper implementation of {@link #doDir}. It merges information from jpkg
     * {@link PyJavaPackage#clsSet} and {@link PyJavaPackage#__dict__}.
     */
    protected PyList basicDoDir(PyJavaPackage jpkg, boolean instantiate, boolean exclpkgs) {
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

    /** Helper merging list2 into list1. Returns list1. */
    protected PyList merge(PyList list1, PyList list2) {
        for (PyObject name : list2.asIterable()) {
            list1.append(name);
        }
        return list1;
    }

    /**
     * Given the (dotted) name of a package, find the {@link PyJavaPackage} corresponding, by
     * navigating from the {@link #topLevelPackage}, successively applying
     * {@link PyObject#__findattr__(String)}. This in fact drives the creation of
     * {@link PyJavaPackage} objects since it indirectly calls
     * {@link #packageExists(String, String)}.
     *
     * @param name (dotted) package name
     * @return the package named
     */
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
     * Create (or ensure we have) a {@link PyJavaPackage} for the named package and add to it the
     * names of classes mentioned here. These classes are added as "place holders" only, so they
     * become members of it, without being instantiated. This method relies on
     * {@link PyJavaPackage#addPackage(java.lang.String, java.lang.String)} and
     * {@link PyJavaPackage#addPlaceholders}.
     *
     * @param name package name
     * @param classes comma or @-sign separated string
     * @param jarfile involved; can be null
     * @return created/updated package
     */
    public PyJavaPackage makeJavaPackage(String name, String classes, String jarfile) {
        PyJavaPackage p = this.topLevelPackage;
        if (name.length() != 0) {
            p = p.addPackage(name, jarfile);
        }
        p.addPlaceholders(split(classes, ",@"));
        return p;
    }

    /** ASM visitor class supporting {@link #checkAccess(InputStream)}. */
    private static class AccessVisitor extends ClassVisitor {

        private int class_access;

        public AccessVisitor() throws IOException {
            super(Opcodes.ASM7);
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName,
                String[] interfaces) {
            class_access = access;
        }

        public int getClassAccess() {
            return class_access;
        }
    }

    /**
     * Check that a given stream is a valid Java .class file, and return its access permissions as
     * an int.
     */
    static protected int checkAccess(InputStream cstream) throws IOException {
        try {
            ClassReader reader = new ClassReader(cstream);
            AccessVisitor visitor = new AccessVisitor();
            reader.accept(visitor, 0);
            return visitor.getClassAccess();
        } catch (RuntimeException e) {
            return -1;
        }
    }

    /**
     * Helper to split a textual list into a list. The semantics are basically those of
     * {@code StringTokenizer}, followed by {@code String.trim()} and duplicate removal.
     *
     * @param target compound string to split into tokens ({@code null} treated as "".
     * @param sep characters, any of which will be treated as a token separator
     * @return set of tokens trimmed of white space
     */
    protected static Set<String> split(String target, String sep) {
        Set<String> result = new LinkedHashSet<>();
        if (target != null) {
            StringTokenizer tok = new StringTokenizer(target, sep);
            while (tok.hasMoreTokens()) {
                String entry = tok.nextToken().trim();
                if (entry.length() > 0) {
                    result.add(entry);
                }
            }
        }
        return result;
    }

    /** Equivalent to {@code split(target, ",")}. See {@link #split(String, String)}. */
    protected static Set<String> split(String target) {
        return split(target, ",");
    }
}
