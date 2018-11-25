// Copyright (c) Corporation for National Research Initiatives
// Copyright 2000 Samuele Pedroni

package org.python.core.packagecache;

import org.python.core.Py;
import org.python.core.PyJavaPackage;
import org.python.core.PyList;
import org.python.core.PySystemState;

import java.io.File;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.ProviderNotFoundException;
import java.util.Properties;
import java.util.Set;
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

    /**
     * Index the contents of every JAR or ZIP in a directory.
     *
     * @param jdir direcory containing some JAR or ZIP files
     * @param cache
     * @param saveCache
     */
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
            // Use the extra flag to defer writing out the cache.
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

        String defaultPaths = "java.class.path";
        String defaultDirectories = "";
        String defaultModules = "java.base,java.desktop,java.logging,java.se,java.sql,java.xml";

        try {
            // Support for the modular JVM (particular packages).
            // XXX This may not be our final approach: maybe enumerate all the packages instead?
            Set<String> modules =
                    split(registry.getProperty("python.packages.modules", defaultModules));
            FileSystem jrtfs = FileSystems.getFileSystem(URI.create("jrt:/"));
            for (String moduleName : modules) {
                Path modulePath = jrtfs.getPath("/modules/" + moduleName);
                addModule(modulePath);
            }
        } catch (ProviderNotFoundException e) {
            // Running on a JVM before Java 9: add boot class path and optional extensions.
            defaultPaths = "java.class.path,sun.boot.class.path";
            defaultDirectories = "java.ext.dirs";
        }

        /*
         * python.packages.paths defines a sequence of property names. Each property is a path
         * string. The default setting causes directories and JARs on the classpath and in the JRE
         * (before Java 9) to be sources of Python packages.
         */
        Set<String> paths = split(registry.getProperty("python.packages.paths", defaultPaths));
        for (String name : paths) {
            // Each property is a path string containing directories
            String path = registry.getProperty(name);
            if (path != null) {
                // Each path may be a mixture of directory and JAR specifiers (source of packages)
                addClassPath(path);
            }
        }

        /*
         * python.packages.directories defines a sequence of property names. Each property name is a
         * path string, in which the elements are directories. Each directory contains JAR/ZIP files
         * that are to be a source of Python packages. By default, these directories are those where
         * the JVM stores its optional packages as JARs (a mechanism withdrawn in Java 9).
         */
        Set<String> directories =
                split(registry.getProperty("python.packages.directories", defaultDirectories));
        for (String name : directories) {
            // Each property defines a path string containing directories
            String path = registry.getProperty(name);
            if (path != null) {
                // Add the JAR/ZIP archives in those directories to the search path for packages
                addJarPath(path);
            }
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

        PySystemState sys = Py.getSystemState();
        if (sys.getClassLoader() == null && packageExists(sys.path, pkg, name)) {
            return true;
        } else {
            return false;
        }
    }

}
