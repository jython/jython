package org.python.core;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Helper class handling the VM specific java package detection.
 */
public class JavaImportHelper {

    private static final String DOT = ".";

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
    protected static boolean tryAddPackage(final String packageName, PyObject fromlist) {
        // make sure we do not turn off the added flag, once it is set
        boolean packageAdded = false;

        if (packageName != null) {
            // check explicit imports first (performance optimization)

            // handle 'from java.net import URL' like explicit imports
            List stringFromlist = getFromListAsStrings(fromlist);
            Iterator fromlistIterator = stringFromlist.iterator();
            while (fromlistIterator.hasNext()) {
                String fromName = (String) fromlistIterator.next();
                if (isJavaClass(packageName, fromName)) {
                    packageAdded = addPackage(packageName, packageAdded);

                }
            }

            // handle 'import java.net.URL' style explicit imports
            int dotPos = packageName.lastIndexOf(DOT);
            if (dotPos > 0) {
                String lastDottedName = packageName.substring(dotPos + 1);
                String packageCand = packageName.substring(0, dotPos);
                if (isJavaClass(packageCand, lastDottedName)) {
                    packageAdded = addPackage(packageCand, packageAdded);
                }
            }

            // if all else fails, check already loaded packages
            if (!packageAdded) {
                // build the actual map with the packages known to the VM
                Map packages = buildLoadedPackages();

                // add known packages
                String parentPackageName = packageName;
                if (isLoadedPackage(packageName, packages)) {
                    packageAdded = addPackage(packageName, packageAdded);
                }
                dotPos = 0;
                do {
                    dotPos = parentPackageName.lastIndexOf(DOT);
                    if (dotPos > 0) {
                        parentPackageName = parentPackageName.substring(0, dotPos);
                        if (isLoadedPackage(parentPackageName, packages)) {
                            packageAdded = addPackage(parentPackageName, packageAdded);
                        }
                    }
                } while (dotPos > 0);

                // handle package imports like 'from java import math'
                fromlistIterator = stringFromlist.iterator();
                while (fromlistIterator.hasNext()) {
                    String fromName = (String) fromlistIterator.next();
                    String fromPackageName = packageName + DOT + fromName;
                    if (isLoadedPackage(fromPackageName, packages)) {
                        packageAdded = addPackage(fromPackageName, packageAdded);
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
     * Convert the fromlist into a java.lang.String based list.
     * <p>
     * Do some sanity checks: filter out '*' and empty tuples, as well as non tuples.
     * 
     * @param fromlist
     * @return a list containing java.lang.String entries
     */
    private static final List getFromListAsStrings(PyObject fromlist) {
        List stringFromlist = new ArrayList();

        if (fromlist != null && fromlist != Py.EmptyTuple && fromlist instanceof PyTuple) {
            Iterator iterator = ((PyTuple) fromlist).iterator();
            while (iterator.hasNext()) {
                Object obj = iterator.next();
                if (obj instanceof String) {
                    String fromName = (String) obj;
                    if (!"*".equals(fromName)) {
                        stringFromlist.add(fromName);
                    }
                }
            }
        }
        return stringFromlist;
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
        boolean isLoaded = false;
        if (javaPackageName != null) {
            isLoaded = packages.containsKey(javaPackageName);
        }
        return isLoaded;
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
                dotPos = packageName.lastIndexOf(DOT);
                if (dotPos > 0) {
                    packageName = packageName.substring(0, dotPos);
                    packageMap.put(packageName, "");
                }
            } while (dotPos > 0);
        }
        return packageMap;
    }

    /**
     * @return <code>true</code> if the java class can be found by the current
     *         Py classloader setup
     */
    private static boolean isJavaClass(String packageName, String className) {
        return className != null && className.length() > 0
                && Py.findClass(packageName + "." + className) != null;
    }

    /**
     * Add a java package to sys.modules, if not already done
     * 
     * @return <code>true</code> if something was really added, <code>false</code> otherwise
     */
    private static boolean addPackage(String packageName, boolean packageAdded) {
        PyObject modules = Py.getSystemState().modules;
        String internedPackageName = packageName.intern();
        PyObject module = modules.__finditem__(internedPackageName);
        // a previously failed import could have created a Py.None entry in sys.modules
        if (module == null || module == Py.None) {
            int dotPos = 0;
            do {
                PyJavaPackage p = PySystemState.add_package(packageName);
                if(dotPos == 0) {
                    modules.__setitem__(internedPackageName, p);
                } else {
                    module = modules.__finditem__(internedPackageName);
                    if (module == null || module == Py.None) {
                        modules.__setitem__(internedPackageName, p);
                    }
                }
                dotPos = packageName.lastIndexOf(DOT);
                if (dotPos > 0) {
                    packageName = packageName.substring(0, dotPos);
                    internedPackageName = packageName.intern();
                }
            } while(dotPos > 0);
            // make sure not to turn off the packageAdded flag
            packageAdded = true;
        }
        return packageAdded;
    }

}
