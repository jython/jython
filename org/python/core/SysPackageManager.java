// Copyright (c) Corporation for National Research Initiatives
// Copyright 2000 Samuele Pedroni

package org.python.core;

import java.util.Properties;
import java.util.StringTokenizer;
import java.io.*;

/** System package manager.
 * Used by org.python.core.PySystemState.
 */
public class SysPackageManager extends PathPackageManager {

    protected void message(String msg) {
        Py.writeMessage("*sys-package-mgr*",msg);
    }

    protected void warning(String warn) {
        Py.writeWarning("*sys-package-mgr*",warn);
    }

    protected void comment(String msg) {
        Py.writeComment("*sys-package-mgr*",msg);
    }

    protected void debug(String msg) {
        Py.writeDebug("*sys-package-mgr*",msg);
    }

    public SysPackageManager(File cachedir, Properties registry) {
        if(useCacheDir(cachedir)) {
            initCache();
            findAllPackages(registry);
            saveCache();
        }
    }

    public void addJar(String jarfile) {
        addJarToPackages(new File(jarfile), false);
    }

    public void addJarDir(String jdir) {
        File file = new File(jdir);
        if (!file.isDirectory()) return;
        String[] files = file.list();
        for(int i=0; i<files.length; i++) {
            String entry = files[i];
            if (entry.endsWith(".jar") || entry.endsWith(".zip")) {
                addJarToPackages(new File(jdir,entry),true);
            }
        }
    }

    private void addJarPath(String path) {
        StringTokenizer tok =
        new StringTokenizer(path, java.io.File.pathSeparator);
        while  (tok.hasMoreTokens())  {
            // ??pending: do jvms trim? how is interpreted entry=""?
            String entry = tok.nextToken();
            addJarDir(entry);
        }
    }

    private void findAllPackages(Properties registry) {
        String paths = registry.getProperty(
                              "python.packages.paths",
                              "java.class.path,sun.boot.class.path");
        String directories = registry.getProperty(
                              "python.packages.directories",
                              "java.ext.dirs");
        String fakepath = registry.getProperty(
                              "python.packages.fakepath", null);
        StringTokenizer tok = new StringTokenizer(paths, ",");
        while  (tok.hasMoreTokens())  {
            String entry = tok.nextToken().trim();
            String tmp = registry.getProperty(entry);
            if (tmp == null) continue;
            addClassPath(tmp);
        }

        tok = new StringTokenizer(directories, ",");
        while  (tok.hasMoreTokens())  {
            String entry = tok.nextToken().trim();
            String tmp = registry.getProperty(entry);
            if (tmp == null) continue;
            addJarPath(tmp);
        }

        if (fakepath != null) addClassPath(fakepath);
    }

    public void notifyPackageImport(String pkg, String name) {
        if (pkg != null && pkg.length()>0) name = pkg + '.' + name;
        Py.writeComment("import","'"+name+"' as java package");
    }

    public Class findClass(String pkg,String name) {
        Class c = super.findClass(pkg,name);
        if (c != null)
            Py.writeComment("import","'"+name+"' as java class");
        return c;
    }

    public Class findClass(String pkg, String name, String reason) {
        if (pkg != null && pkg.length()>0) name = pkg + '.' + name;
        return Py.findClassEx(name,reason);
    }

    public PyList doDir(PyJavaPackage jpkg, boolean instantiate,
                        boolean exclpkgs)
    {
        PyList basic = basicDoDir(jpkg, instantiate, exclpkgs);
        PyList ret = new PyList();

        doDir(searchPath, ret, jpkg, instantiate, exclpkgs);

        PySystemState system = Py.getSystemState();

        if (system.getClassLoader() == null)
            doDir(system.path, ret, jpkg, instantiate, exclpkgs);

        return merge(basic,ret);
    }

    public boolean packageExists(String pkg, String name) {
        if (packageExists(searchPath, pkg, name)) return true;

        PySystemState system = Py.getSystemState();

        if (system.getClassLoader() == null &&
                 packageExists(Py.getSystemState().path,pkg,name)) {
            return true;
        }

        return false;
    }

}
