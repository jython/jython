// Copyright © Corporation for National Research Initiatives
// Copyright 2000 Samuele Pedroni

package org.python.core;

import java.io.*;
import java.lang.reflect.Modifier;

/** Path package manager. Gathering classes info dynamically
 * from a set of directories in path {@link #searchPath}, and
 * statically from a set of jars, like {@link CachedJarsPackageManager}.
 */
public abstract class PathPackageManager extends CachedJarsPackageManager {

    public PyList searchPath;

    public PathPackageManager() {
        searchPath = new PyList();
    }

    /** Helper for {@link #packageExists(java.lang.String,java.lang.String)}.
     * Scans for package pkg.name the directories in path.
     *
     */
    protected boolean packageExists(PyList path,String pkg,String name) {
        String child = pkg.replace('.',File.separatorChar) +
                       File.separator + name;

        for (int i=0; i < path.__len__(); i++) {
            String dir = path.get(i).__str__().toString();
            if (dir.length() == 0) dir = null;

            if (new File(dir,child).isDirectory()) return true;
        }
        return false;
    }

    /** Helper for {@link #doDir(PyJavaPackage,boolean,boolean)}.
     * Scans for package jpkg content over the directories in path.
     * Add to ret the founded classes/pkgs.
     * Filter out classes using {@link #filterByName},{@link #filterByAccess}.
     */
    protected void doDir(PyList path, PyList ret, PyJavaPackage jpkg,
                         boolean instantiate,boolean exclpkgs)
    {
        String child=jpkg.__name__.replace('.',File.separatorChar);

        for (int i=0; i < path.__len__(); i++) {
            String dir = path.get(i).__str__().toString();
            if (dir.length() == 0) dir = null;

            File childFile = new File(dir,child);

            String[] list=childFile.list();
            if(list == null) continue;

            doList:
            for (int j=0; j < list.length; j++) {
                String jname = list[j];

                File cand = new File(childFile, jname);

                int jlen = jname.length();

                boolean pkgCand=false;

                if (cand.isDirectory()) {
                    if (!instantiate && exclpkgs) continue;
                    pkgCand = true;
                } else {
                    if(!jname.endsWith(".class")) continue;
                    jlen-=6;
                }

                jname = jname.substring(0,jlen);
                PyString name = new PyString(jname);

                if (filterByName(jname,pkgCand)) continue;

                // for opt maybe we should some hash-set for ret
                if (jpkg.__dict__.has_key(name) ||
                      jpkg.clsSet.has_key(name) ||
                      ret.__contains__(name)) {
                    continue;
                }

                if (!Character.isJavaIdentifierStart(jname.charAt(0)))
                    continue;
                for (int k = 1; k < jlen; k++) {
                    if (!Character.isJavaIdentifierPart(jname.charAt(k)))
                        continue doList;
                }

                if(!pkgCand) {
                    try {
                        int acc = checkAccess(new BufferedInputStream(
                                              new FileInputStream(cand)));
                        if ((acc == -1) || filterByAccess(jname, acc))
                            continue;
                    } catch(IOException e) {
                        continue;
                    }
                }

                if (instantiate) {
                    if (pkgCand) jpkg.addPackage(jname);
                    else jpkg.addLazyClass(jname);
                }

                ret.append(name);

            }
        }

    }

    /** Add directory dir (if exists) to {@link #searchPath}.
     */
    public void addDirectory(File dir) {
        try {
            if (dir.getPath().length() == 0)
                searchPath.append(Py.EmptyString);
            else
                searchPath.append(new PyString(dir.getCanonicalPath()));
        } catch(IOException e) {
            warning("skipping bad directory, '" +dir+ "'");
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

    /** Adds "classpath" entry. Calls {@link #addDirectory} if path
     * refers to a dir, {@link #addJarToPackages(java.io.File, boolean)}
     * with param cache true if path refers to a jar.
     */
    public void addClassPath(String path) {
        PyList paths = new PyString(path).split(java.io.File.pathSeparator);

        for (int i = 0; i < paths.__len__(); i++) {
            String entry = paths.get(i).toString();
            if (entry.endsWith(".jar") || entry.endsWith(".zip")) {
                addJarToPackages(new File(entry),true);
            } else {
                File dir = new File(entry);
                if (entry.length() == 0 || dir.isDirectory())
                    addDirectory(dir);
            }
        }
    }

    public PyList doDir(PyJavaPackage jpkg, boolean instantiate,
                        boolean exclpkgs)
    {
        PyList basic = basicDoDir(jpkg,instantiate,exclpkgs);
        PyList ret = new PyList();

        doDir(searchPath,ret,jpkg,instantiate,exclpkgs);

        return merge(basic,ret);
    }

    public boolean packageExists(String pkg,String name) {
        return packageExists(searchPath,pkg,name);
    }

}
