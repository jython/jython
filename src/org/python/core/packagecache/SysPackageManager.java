// Copyright (c) Corporation for National Research Initiatives
// Copyright 2000 Samuele Pedroni

package org.python.core.packagecache;

import org.python.core.Py;
import org.python.core.PyJavaPackage;
import org.python.core.PyList;
import org.python.core.PySystemState;
import org.python.core.RegistryKey;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.ProviderNotFoundException;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
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
     * @param jdir directory containing some JAR or ZIP files
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
     * Index the packages in every module in a directory. Entries in the directory that are not modules
     * (do not contain a {@code module-info.class}) are ignored. Only modules exploded on the file system of this path are (currently) supported,
     * and at the time of writing, we only use this method on the {@code jrt:} file system.
     *
     * @param moduleDir directory containing some modules
     */
    private void addModuleDir(final Path moduleDir) {
        try {
            // Walk the directory tree with this visitor
            FileVisitor<Path> visitor = new SimpleFileVisitor<Path>() {

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    // System.out.println(dir);
                    if (dir.equals(moduleDir)) {
                        // Ignore this, it's just the root)
                    } else if (Files.exists(dir.resolve("module-info.class"))) {
                        // dir is a module: scan packages from it.
                        addModuleToPackages(dir);
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return FileVisitResult.CONTINUE;
                }
            };

            Files.walkFileTree(moduleDir, visitor);

        } catch (IOException e) {
            warning("error enumerating Java modules in " + moduleDir + ": " + e.getMessage());
        }
    }

    /**
     * Walk the packages found in paths specified indirectly through the given {@code Properties}
     * object.
     *
     * @param registry in practice, the Jython registry
     */
    private void findAllPackages(Properties registry) {

        /*
         * Packages in the Java runtime environment are enumerated in the jrt file system (from Java
         * 9 onwards), or in JARs and directories designated by the properties
         * sun.boot.class.path and java.ext.dirs (up to Java 8).
         */
        String defaultClassPaths, defaultDirectories;
        try {
            // Support for the modular JVM (particular packages).
            FileSystem jrtfs = FileSystems.getFileSystem(URI.create("jrt:/"));
            addModuleDir(jrtfs.getPath("/modules/"));
            defaultClassPaths = "java.class.path";
            defaultDirectories = "";
        } catch (ProviderNotFoundException e) {
            // Running on a JVM before Java 9: add boot class path and optional extensions.
            defaultClassPaths = "java.class.path,sun.boot.class.path";
            defaultDirectories = "java.ext.dirs";
        }

        Set<String> cps = split(registry.getProperty(
                    RegistryKey.PYTHON_PACKAGES_PATHS, defaultClassPaths));
        for (String name : cps) {
            // Each property is a class-path string containing JARS and directories
            String classPath = registry.getProperty(name);
            if (classPath != null) {
                // Each path may be a mixture of directory and JAR specifiers (source of packages)
                addClassPath(classPath);
            }
        }

        Set<String> directories =
                split(registry.getProperty(
                            RegistryKey.PYTHON_PACKAGES_DIRECTORIES, 
                            defaultDirectories));
        for (String name : directories) {
            // Each property defines a path string containing directories
            String path = registry.getProperty(name);
            if (path != null) {
                // Add the JAR/ZIP archives in those directories to the search path for packages
                addJarPath(path);
            }
        }

        String fakepath = registry.getProperty(
                RegistryKey.PYTHON_PACKAGES_FAKEPATH, null);
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
        } else {
            PySystemState sys = Py.getSystemState();
            return sys.getClassLoader() == null && packageExists(sys.path, pkg, name);
        }
    }
}
