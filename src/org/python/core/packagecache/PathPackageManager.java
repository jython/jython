// Copyright (c) Corporation for National Research Initiatives
// Copyright 2000 Samuele Pedroni

package org.python.core.packagecache;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.logging.Level;

import org.python.core.Py;
import org.python.core.PyException;
import org.python.core.PyJavaPackage;
import org.python.core.PyList;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.core.imp;
import org.python.core.util.RelativeFile;

/**
 * Path package manager. Gathering classes info dynamically from a set of directories in path
 * {@link #searchPath}, and statically from a set of jars, like {@link CachedJarsPackageManager}.
 */
public abstract class PathPackageManager extends CachedJarsPackageManager {

    public PyList searchPath;

    public PathPackageManager() {
        this.searchPath = new PyList();
    }

    /**
     * Helper for {@link #packageExists(String,String)}. Scans the directories in the given path for
     * package pkg.name. A directory with a matching name is considered to define a package if it
     * contains no Python (source or compiled), or contains a Java .class file (not compiled from
     * Python).
     */
    protected static boolean packageExists(PyList path, String pkg, String name) {
        String child = pkg.replace('.', File.separatorChar) + File.separator + name;

        for (int i = 0; i < path.__len__(); i++) {

            PyObject entry = path.pyget(i);

            // Each entry in the path may be byte-encoded or unicode
            String dir = imp.fileSystemDecode(entry, false);
            if (dir != null) {
                File f = new RelativeFile(dir, child);
                try {
                    if (f.isDirectory() && imp.caseok(f, name)) {
                        /*
                         * f is a directory matching the package name. This directory is considered
                         * to define a package if it contains no Python (source or compiled), or
                         * contains a Java .class file (not compiled from Python).
                         */
                        PackageExistsFileFilter m = new PackageExistsFileFilter();
                        f.listFiles(m);
                        boolean exists = m.packageExists();
                        if (exists) {
                            logger.log(Level.CONFIG, "# trying {0}", f.getAbsolutePath());
                        }
                        return exists;
                    }
                } catch (SecurityException se) {
                    return false;
                }
            }
        }
        return false;
    }

    private static class PackageExistsFileFilter implements FilenameFilter {

        private boolean java;
        private boolean python;

        @Override
        public boolean accept(File dir, String name) {
            if (name.endsWith(".py") || name.endsWith("$py.class")
                    || name.endsWith("$_PyInner.class")) {
                python = true;
            } else if (name.endsWith(".class")) {
                java = true;
            }
            return false;
        }

        public boolean packageExists() {
            return !python || java;
        }
    }

    /**
     * Helper for {@link #doDir(PyJavaPackage,boolean,boolean)}. Scans for package jpkg content over
     * the directories in path. Add to ret the found classes/pkgs. Filter out classes using
     * {@link #filterByName},{@link #filterByAccess}.
     */
    protected void doDir(PyList path, PyList ret, PyJavaPackage jpkg, boolean instantiate,
            boolean exclpkgs) {

        String child = jpkg.__name__.replace('.', File.separatorChar);

        for (int i = 0; i < path.__len__(); i++) {
            // Each entry in the path may be byte-encoded or unicode
            String dir = Py.fileSystemDecode(path.pyget(i));
            if (dir.length() == 0) {
                dir = null;
            }

            File childFile = new File(dir, child);
            String[] list = childFile.list();
            if (list == null) {
                continue;
            }

            doList : for (int j = 0; j < list.length; j++) {
                String jname = list[j];
                File cand = new File(childFile, jname);
                int jlen = jname.length();
                boolean pkgCand = false;

                if (cand.isDirectory()) {
                    if (!instantiate && exclpkgs) {
                        continue;
                    }
                    pkgCand = true;
                } else {
                    if (!jname.endsWith(".class")) {
                        continue;
                    }
                    jlen -= 6;
                }

                jname = jname.substring(0, jlen);
                PyString name = Py.newStringOrUnicode(jname);

                if (filterByName(jname, pkgCand)) {
                    continue;
                }

                // for opt maybe we should some hash-set for ret
                if (jpkg.__dict__.has_key(name) || jpkg.clsSet.has_key(name)
                        || ret.__contains__(name)) {
                    continue;
                }

                if (!Character.isJavaIdentifierStart(jname.charAt(0))) {
                    continue;
                }

                for (int k = 1; k < jlen; k++) {
                    if (!Character.isJavaIdentifierPart(jname.charAt(k))) {
                        continue doList;
                    }
                }

                if (!pkgCand) {
                    try {
                        int acc = checkAccess(new BufferedInputStream(new FileInputStream(cand)));
                        if ((acc == -1) || filterByAccess(jname, acc)) {
                            continue;
                        }
                    } catch (IOException e) {
                        continue;
                    }
                }

                if (instantiate) {
                    if (pkgCand) {
                        jpkg.addPackage(jname);
                    } else {
                        jpkg.addClass(jname, Py.findClass(jpkg.__name__ + "." + jname));
                    }
                }

                ret.append(name);
            }
        }
    }

    /** Add directory dir (if exists) to {@link #searchPath}. */
    @Override
    public void addDirectory(File dir) {
        try {
            if (dir.getPath().length() == 0) {
                this.searchPath.append(Py.EmptyString);
            } else {
                this.searchPath.append(Py.newStringOrUnicode(dir.getCanonicalPath()));
            }
        } catch (IOException e) {
            warning("# skipping bad directory {0} ({1})", dir, e.getMessage());
        }
    }

    /**
     * Scan a Java class-path that may be a mixture of directory and JAR specifiers, and within each
     * path entry index the packages. Calls {@link #addDirectory} if a path entry refers to a dir,
     * {@link #addJarToPackages(java.io.File, boolean)} with param cache true if the path entry
     * refers to a jar.
     */
    public void addClassPath(String path) {
        String[] entries = path.split(java.io.File.pathSeparator);
        for (String entry : entries) {
            entry = entry.trim();
            if (entry.endsWith(".jar") || entry.endsWith(".zip")) {
                addJarToPackages(new File(entry), true);
            } else {
                File dir = new File(entry);
                if (entry.length() == 0 || dir.isDirectory()) {
                    addDirectory(dir);
                }
            }
        }
    }

    @Override
    public PyList doDir(PyJavaPackage jpkg, boolean instantiate, boolean exclpkgs) {
        PyList basic = basicDoDir(jpkg, instantiate, exclpkgs);
        PyList ret = new PyList();
        doDir(this.searchPath, ret, jpkg, instantiate, exclpkgs);
        return merge(basic, ret);
    }

    @Override
    public boolean packageExists(String pkg, String name) {
        return packageExists(this.searchPath, pkg, name);
    }

}
