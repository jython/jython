// Copyright (c) Corporation for National Research Initiatives
// Copyright 2000 Samuele Pedroni

package org.python.core.packagecache;

import org.python.core.imp;
import org.python.core.Py;
import org.python.core.PyJavaPackage;
import org.python.core.PyList;
import org.python.core.PyString;
import org.python.core.util.RelativeFile;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;

/**
 * Path package manager. Gathering classes info dynamically from a set of
 * directories in path {@link #searchPath}, and statically from a set of jars,
 * like {@link CachedJarsPackageManager}.
 */
public abstract class PathPackageManager extends CachedJarsPackageManager {

    public PyList searchPath;

    public PathPackageManager() {
        this.searchPath = new PyList();
    }

    /**
     * Helper for {@link #packageExists(java.lang.String,java.lang.String)}.
     * Scans for package pkg.name the directories in path.
     */
    protected boolean packageExists(PyList path, String pkg, String name) {
        String child = pkg.replace('.', File.separatorChar) + File.separator
                + name;

        for (int i = 0; i < path.__len__(); i++) {
            String dir = path.pyget(i).__str__().toString();

            File f = new RelativeFile(dir, child);
            if (f.isDirectory() && imp.caseok(f, name)) {
                /*
                 * Figure out if we have a directory a mixture of python and
                 * java or just an empty directory (which means Java) or a
                 * directory with only Python source (which means Python).
                 */
                PackageExistsFileFilter m = new PackageExistsFileFilter();
                f.listFiles(m);
                boolean exists = m.packageExists();
                if (exists) {
                    Py.writeComment("import", "java package as '"
                            + f.getAbsolutePath() + "'");
                }
                return exists;
            }
        }
        return false;
    }

    class PackageExistsFileFilter implements FilenameFilter {
        private boolean java;

        private boolean python;

        public boolean accept(File dir, String name) {
            if(name.endsWith(".py") || name.endsWith("$py.class") || name.endsWith("$_PyInner.class")) {
                python = true;
            }else if (name.endsWith(".class")) {
                java = true;
            }
            return false;
        }

        public boolean packageExists() {
            if (this.python && !this.java) {
                return false;
            }
            return true;
        }
    }

    /**
     * Helper for {@link #doDir(PyJavaPackage,boolean,boolean)}. Scans for
     * package jpkg content over the directories in path. Add to ret the founded
     * classes/pkgs. Filter out classes using {@link #filterByName},{@link #filterByAccess}.
     */
    protected void doDir(PyList path, PyList ret, PyJavaPackage jpkg,
            boolean instantiate, boolean exclpkgs) {
        String child = jpkg.__name__.replace('.', File.separatorChar);

        for (int i = 0; i < path.__len__(); i++) {
            String dir = path.pyget(i).__str__().toString();
            if (dir.length() == 0) {
                dir = null;
            }

            File childFile = new File(dir, child);

            String[] list = childFile.list();
            if (list == null) {
                continue;
            }

            doList: for (int j = 0; j < list.length; j++) {
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
                PyString name = new PyString(jname);

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
                        int acc = checkAccess(new BufferedInputStream(
                                new FileInputStream(cand)));
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
                        jpkg.addClass(jname, Py.findClass(jname));
                    }
                }

                ret.append(name);

            }
        }

    }

    /**
     * Add directory dir (if exists) to {@link #searchPath}.
     */
    public void addDirectory(File dir) {
        try {
            if (dir.getPath().length() == 0) {
                this.searchPath.append(Py.EmptyString);
            } else {
                this.searchPath.append(new PyString(dir.getCanonicalPath()));
            }
        } catch (IOException e) {
            warning("skipping bad directory, '" + dir + "'");
        }
    }

    // ??pending:
    // Uses simply split and not a StringTokenizer+trim to adhere to
    // sun jvm parsing of classpath.
    // E.g. "a;" is parsed by sun jvm as a, ""; the latter is interpreted
    // as cwd. jview trims and cwd is per default in classpath.
    // The logic here should work for both(...). Need to distinguish?
    // This code does not avoid duplicates in searchPath.
    // Should cause no problem (?).

    /**
     * Adds "classpath" entry. Calls {@link #addDirectory} if path refers to a
     * dir, {@link #addJarToPackages(java.io.File, boolean)} with param cache
     * true if path refers to a jar.
     */
    public void addClassPath(String path) {
        PyList paths = new PyString(path).split(java.io.File.pathSeparator);

        for (int i = 0; i < paths.__len__(); i++) {
            String entry = paths.pyget(i).toString();
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

    public PyList doDir(PyJavaPackage jpkg, boolean instantiate,
            boolean exclpkgs) {
        PyList basic = basicDoDir(jpkg, instantiate, exclpkgs);
        PyList ret = new PyList();

        doDir(this.searchPath, ret, jpkg, instantiate, exclpkgs);

        return merge(basic, ret);
    }

    public boolean packageExists(String pkg, String name) {
        return packageExists(this.searchPath, pkg, name);
    }

}
