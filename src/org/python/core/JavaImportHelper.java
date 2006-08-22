package org.python.core;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

/**
 * Helper class handling the VM specific java package detection.
 */
public class JavaImportHelper {

    /**
     * Try to add the java package.
     * <p>
     * This is handy in cases where the package scan cannot run, or when the initial classpath does not contain all .jar
     * files (such as in J2EE containers).
     * <p>
     * There is some self-healing in the sense that a correct, explicit import of a java class will succeed even if
     * sys.modules already contains a Py.None entry for the corresponding java package.
     * 
     * @param packageName The dotted name of the java package
     * @param fromlist A tuple with the from names to import. Can be null or empty.
     * 
     * @return <code>true</code> if a java package was doubtlessly identified and added, <code>false</code>
     * otherwise.
     */
    protected static boolean tryAddPackage(String packageName, PyObject fromlist) {
        // build the actual map with the packages known to the VM
        Map packages = buildLoadedPackages();

        // make sure we do not turn off the added flag, once it is set
        boolean packageAdded = false;

        // handle package name
        if (isLoadedPackage(packageName, packages)) {
            packageAdded = addPackage(packageName);
        }
        String parentPackageName = packageName;
        int dotPos = 0;
        do {
            dotPos = parentPackageName.lastIndexOf(".");
            if (dotPos > 0) {
                parentPackageName = parentPackageName.substring(0, dotPos);
                if (isLoadedPackage(parentPackageName, packages)) {
                    boolean parentAdded = addPackage(parentPackageName);
                    if (parentAdded) {
                        packageAdded = true;
                    }
                }
            }
        } while (dotPos > 0);

        // handle fromlist
        if (fromlist != null && fromlist != Py.EmptyTuple && fromlist instanceof PyTuple) {
            Iterator iterator = ((PyTuple) fromlist).iterator();
            while (iterator.hasNext()) {
                Object obj = iterator.next();
                if (obj instanceof String) {
                    String fromName = (String) obj;
                    if (!"*".equals(fromName)) {
                        boolean fromAdded = false;
                        if (isJavaClass(packageName, fromName)) {
                            fromAdded = addPackage(packageName);
                            if (fromAdded) {
                                packageAdded = true;
                            }
                        } else {
                            // handle cases like: from java import math
                            String fromPackageName = packageName + "." + fromName;
                            if (isLoadedPackage(fromPackageName, packages)) {
                                fromAdded = addPackage(fromPackageName);
                                if (fromAdded) {
                                    packageAdded = true;
                                }
                            }
                        }
                    }
                }
            }
        }
        return packageAdded;
    }

    /**
     * Check if a java package is already known to the VM.
     * <p>
     * May return <code>false</code> even if the given package name is a valid java package !
     * 
     * @param packageName
     * 
     * @return <code>true</code> if the package with the given name is already loaded by the VM, <code>false</code>
     * otherwise.
     */
    protected static boolean isLoadedPackage(String packageName) {
        return isLoadedPackage(packageName, buildLoadedPackages());
    }

    /**
     * Faster way to check if a java package is already known to the VM.
     * <p>
     * May return <code>false</code> even if the given package name is a valid java package !
     * 
     * @param packageName
     * @param packages A Map containing all packages actually known to the VM. Such a Map can be obtained using
     * {@link JavaImportHelper.buildLoadedPackagesTree()}
     * 
     * @return <code>true</code> if the package with the given name is already loaded by the VM, <code>false</code>
     * otherwise.
     */
    private static boolean isLoadedPackage(String javaPackageName, Map packages) {
        return packages.containsKey(javaPackageName);
    }

    /**
     * Build a <code>Map</code> of the currently known packages to the VM.
     * <p>
     * All parent packages appear as single entries like python modules, e.g. <code>java</code>,
     * <code>java.lang</code>, <code>java.lang.reflect</code>,
     */
    private static Map buildLoadedPackages() {
        TreeMap packageMap = new TreeMap();
        Package[] packages = Package.getPackages();
        for (int i = 0; i < packages.length; i++) {
            String packageName = packages[i].getName();
            packageMap.put(packageName, "");
            int dotPos = 0;
            do {
                dotPos = packageName.lastIndexOf(".");
                if (dotPos > 0) {
                    packageName = packageName.substring(0, dotPos);
                    packageMap.put(packageName, "");
                }
            } while (dotPos > 0);
        }
        return packageMap;
    }

    /**
     * Check a java class on VM level.
     * 
     * @param packageName
     * @param className
     * 
     * @return <code>true</code> if the java class can be doubtlessly identified, <code>false</code> otherwise.
     */
    private static boolean isJavaClass(String packageName, String className) {
        if (className != null && className.length() > 0) {
            className = packageName.replace('.', '/') + "/" + className + ".class";
            return Thread.currentThread().getContextClassLoader().getResource(className) != null;
        } else {
            return false;
        }
    }

    /**
     * Add a java package to sys.modules, if not already done
     * 
     * @return <code>true</code> if something was really added, <code>false</code> otherwise
     */
    private static boolean addPackage(String packageName) {
        boolean added = false;
        PyObject module = Py.getSystemState().modules.__finditem__(packageName.intern());
        if (module == null || module == Py.None) {
            PyObject modules = Py.getSystemState().modules;
            int dotPos;
            do {
                String internedPackageName = packageName.intern();
                if (modules.__finditem__(internedPackageName) == Py.None) {
                    // a previously failed import could have created a Py.None entry in sys.modules
                    modules.__delitem__(internedPackageName);
                }
                PyJavaPackage p = PySystemState.add_package(packageName);
                Py.getSystemState().modules.__setitem__(internedPackageName, p);
                added = true;
                dotPos = packageName.lastIndexOf(".");
                if (dotPos > 0) {
                    packageName = packageName.substring(0, dotPos);
                }
            } while (dotPos > 0);
        }
        return added;
    }

}
