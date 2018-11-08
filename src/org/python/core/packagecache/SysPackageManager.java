// Copyright (c) Corporation for National Research Initiatives
// Copyright 2000 Samuele Pedroni

package org.python.core.packagecache;

import org.python.core.Py;
import org.python.core.PyJavaPackage;
import org.python.core.PyList;
import org.python.core.PySystemState;

import java.io.File;
import java.util.Properties;
import java.util.StringTokenizer;

/**
 * System package manager. Used by org.python.core.PySystemState.
 */
public class SysPackageManager extends PathPackageManager {

    @Override
    protected void message(String msg) {
        Py.writeMessage("*sys-package-mgr*", msg);
    }

    @Override
    protected void warning(String warn) {
        Py.writeWarning("*sys-package-mgr*", warn);
    }

    @Override
    protected void comment(String msg) {
        Py.writeComment("*sys-package-mgr*", msg);
    }

    @Override
    protected void debug(String msg) {
        Py.writeDebug("*sys-package-mgr*", msg);
    }

    public SysPackageManager(File cachedir, Properties registry) {
        if (useCacheDir(cachedir)) {
            initCache();
            findAllPackages(registry);
            saveCache();
        }
    }

    @Override
    public void addJar(String jarfile, boolean cache) {
        addJarToPackages(new File(jarfile), cache);
        if (cache) {
            saveCache();
        }
    }

    @Override
    public void addJarDir(String jdir, boolean cache) {
        addJarDir(jdir, cache, cache);
    }

    /** Index the contents of every JAR or ZIP in a directory. */
    private void addJarDir(String jdir, boolean cache, boolean saveCache) {

        File file = new File(jdir);
        String[] files = file.list();

        if (files != null) {
            // jdir is a directory, enumerated in the array files
            for (int i = 0; i < files.length; i++) {
                String entry = files[i];
                if (entry.endsWith(".jar") || entry.endsWith(".zip")) {
                    addJarToPackages(new File(jdir, entry), cache);
                }
            }

            if (saveCache) {
                saveCache();
            }
        }
    }

    /**
     * Scan a path that contains directory specifiers, and within each directory, find every JAR or
     * ZIP archive and add it to the list of archives searched for packages. (This means, index it.)
     * Non-directory entries are ignored.
     */
    private void addJarPath(String path) {
        StringTokenizer tok = new StringTokenizer(path, java.io.File.pathSeparator);
        while (tok.hasMoreTokens()) {
            // ??pending: do jvms trim? how is interpreted entry=""?
            String entry = tok.nextToken();
            addJarDir(entry, true, false);
        }
    }

    /**
     * Walk the packages found in paths specified indirectly through the given {@code Properties}
     * object, which in practice is the Jython registry.
     *
     * @param registry
     */
    private void findAllPackages(Properties registry) {
        /*
         * python.packages.directories defines a sequence of property names. Each property name is a
         * path string. The default setting causes directories and JARs on the classpath and in the
         * JRE (before Java 9) to be sources of Python packages.
         */
        String defaultPaths = "java.class.path,sun.boot.class.path";
        String paths = registry.getProperty("python.packages.paths", defaultPaths);
        StringTokenizer tok = new StringTokenizer(paths, ",");
        while (tok.hasMoreTokens()) {
            // Each property name is a path string containing directories
            String entry = tok.nextToken().trim();
            String tmp = registry.getProperty(entry);
            if (tmp == null) {
                continue;
            }
            // Each path may be a mixture of directory and JAR specifiers (source of packages)
            addClassPath(tmp);
        }

        /*
         * python.packages.directories defines a sequence of property names. Each property name is a
         * path string, in which the elements are directories. Each directory contains JAR/ZIP files
         * that are to be a source of Python packages. By default, these directories are those where
         * the JVM stores its optional packages as JARs (a mechanism withdrawn in Java 9).
         */
        String directories = registry.getProperty("python.packages.directories", "java.ext.dirs");
        tok = new StringTokenizer(directories, ",");
        while (tok.hasMoreTokens()) {
            // Each property name is a path string containing directories
            String entry = tok.nextToken().trim();
            String tmp = registry.getProperty(entry);
            if (tmp == null) {
                continue;
            }
            // Add the JAR/ZIP archives found in those directories to the search path for packages
            addJarPath(tmp);
        }

        /*
         * python.packages.fakepath defines a sequence of directories and JARs that are to be
         * sources of Python packages.
         */
        String fakepath = registry.getProperty("python.packages.fakepath", null);
        if (fakepath != null) {
            addClassPath(fakepath);
        }
    }

    @Override
    public void notifyPackageImport(String pkg, String name) {
        if (pkg != null && pkg.length() > 0) {
            name = pkg + '.' + name;
        }
        Py.writeComment("import", "'" + name + "' as java package");
    }

    @Override
    public Class findClass(String pkg, String name) {
        Class c = super.findClass(pkg, name);
        if (c != null) {
            Py.writeComment("import", "'" + name + "' as java class");
        }
        return c;
    }

    @Override
    public Class findClass(String pkg, String name, String reason) {
        if (pkg != null && pkg.length() > 0) {
            name = pkg + '.' + name;
        }
        return Py.findClassEx(name, reason);
    }

    @Override
    public PyList doDir(PyJavaPackage jpkg, boolean instantiate, boolean exclpkgs) {
        PyList basic = basicDoDir(jpkg, instantiate, exclpkgs);
        PyList ret = new PyList();

        doDir(this.searchPath, ret, jpkg, instantiate, exclpkgs);

        PySystemState system = Py.getSystemState();
        if (system.getClassLoader() == null) {
            doDir(system.path, ret, jpkg, instantiate, exclpkgs);
        }

        return merge(basic, ret);
    }

    @Override
    public boolean packageExists(String pkg, String name) {
        if (packageExists(this.searchPath, pkg, name)) {
            return true;
        }

        PySystemState system = Py.getSystemState();
        if (system.getClassLoader() == null && packageExists(Py.getSystemState().path, pkg, name)) {
            return true;
        } else {
            return false;
        }
    }

}
