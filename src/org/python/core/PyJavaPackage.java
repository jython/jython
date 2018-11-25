// Copyright (c) Corporation for National Research Initiatives
// Copyright 2000 Samuele Pedroni

package org.python.core;

import org.python.core.packagecache.PackageManager;

import java.util.Collection;

/** A representation of java package. */
public class PyJavaPackage extends PyObject implements Traverseproc {

    public String __name__;

    public PyStringMap __dict__;

    /** Its keys are the names of statically known classes. E.g. from jars pre-scan. */
    public PyStringMap clsSet;
    public String __file__;

    /** (Control) package manager whose hierarchy contains this java pkg */
    public PackageManager __mgr__;

    public PyJavaPackage(String name) {
        this(name, null, null);
    }

    public PyJavaPackage(String name, String jarfile) {
        this(name, null, jarfile);
    }

    public PyJavaPackage(String name, PackageManager mgr) {
        this(name, mgr, null);
    }

    public PyJavaPackage(String name, PackageManager mgr, String jarfile) {
        __file__ = jarfile;
        __name__ = name;
        __mgr__ = (mgr != null) ? mgr : PySystemState.packageManager;

        clsSet = new PyStringMap();

        __dict__ = new PyStringMap();
        __dict__.__setitem__("__name__", new PyString(__name__));
    }

    public PyJavaPackage addPackage(String name) {
        return addPackage(name, null);
    }

    /**
     * From a dotted package name {@code a.b.c} interpreted relative to this package {@code t},
     * ensure that {@code a} is in the dictionary of {@code t} and then recursively process the
     * remainder {@code b.c} relative to {@code a}, finally returning the {@link #PyJavaPackage} of
     * {@code t.a.b.c}. In the case where the initial package name is just {@code a}, no dots, the
     * method simply ensures {@code a} is entered in {@code t}, and returns the
     * {@link PyJavaPackage} of {@code t.a}.
     *
     * @param name a package name
     * @param jarfile becomes the {@code __file__} attribute
     * @return the {@link PyJavaPackage} of the package named
     */
    public PyJavaPackage addPackage(String name, String jarfile) {
        int dot = name.indexOf('.');
        String firstName = name;
        String remainder = null;
        if (dot != -1) {
            firstName = name.substring(0, dot);
            remainder = name.substring(dot + 1, name.length());
        }
        firstName = firstName.intern();
        PyJavaPackage p = (PyJavaPackage) __dict__.__finditem__(firstName);
        if (p == null) {
            String pname = __name__.length() == 0 ? firstName : __name__ + '.' + firstName;
            p = new PyJavaPackage(pname, __mgr__, jarfile);
            __dict__.__setitem__(firstName, p);
        } else {
            // this code is ok here, because this is not needed for a top level package
            if (jarfile == null || !jarfile.equals(p.__file__)) {
                p.__file__ = null;
            }
        }
        return remainder != null ? p.addPackage(remainder, jarfile) : p;
    }

    public PyObject addClass(String name, Class<?> c) {
        PyObject ret = Py.java2py(c);
        __dict__.__setitem__(name.intern(), ret);
        return ret;
    }

    /**
     * Add the classes named to this package, but with only a placeholder value. These names are
     * statically known, typically from processing JAR files on the class path.
     *
     * @param classes the names as strings
     */
    public void addPlaceholders(Collection<String> classes) {
        for (String name : classes) {
            name = name.intern();
            if (clsSet.__finditem__(name) == null) {
                clsSet.__setitem__(name, Py.One);
            }
        }
    }

    @Override
    public PyObject __dir__() {
        return __mgr__.doDir(this, false, false);
    }

    /**
     * Used for 'from xyz import *', dynamically dir pkg filling up __dict__. It uses
     * {@link PackageManager#doDir} implementation furnished by the control package manager with
     * instantiate true. The package manager should load classes with {@link #addClass} in the
     * package.
     *
     * @return list of member names
     */
    public PyObject fillDir() {
        return __mgr__.doDir(this, true, false);
    }

    @Override
    public PyObject __findattr_ex__(String name) {

        PyObject ret = __dict__.__finditem__(name);

        if (ret != null) {
            return ret;

        } else if (__mgr__.packageExists(__name__, name)) {
            __mgr__.notifyPackageImport(__name__, name);
            return addPackage(name);

        } else {
            Class<?> c = __mgr__.findClass(__name__, name);
            if (c != null) {
                return addClass(name, c);
            } else if (name == "__name__") {
                return new PyString(__name__);
            } else if (name == "__dict__") {
                return __dict__;
            } else if (name == "__mgr__") {
                return Py.java2py(__mgr__);
            } else if (name == "__file__") {
                // Stored as UTF-16 for Java but expected as bytes in Python
                return __file__ == null ? Py.None : Py.fileSystemEncode(__file__);
            } else {
                return null;
            }
        }
    }

    @Override
    public void __setattr__(String attr, PyObject value) {
        if (attr == "__mgr__") {
            PackageManager newMgr = Py.tojava(value, PackageManager.class);
            if (newMgr == null) {
                throw Py.TypeError("cannot set java package __mgr__ to None");
            }
            __mgr__ = newMgr;
        } else if (attr == "__file__") {
            // Stored as UTF-16 for Java but presented as bytes from Python
            __file__ = Py.fileSystemDecode(value);
        } else {
            super.__setattr__(attr, value);
        }
    }

    @Override
    public String toString() {
        return "<java package " + __name__ + " " + Py.idstr(this) + ">";
    }

    /* Traverseproc implementation */
    @Override
    public int traverse(Visitproc visit, Object arg) {
        // __dict__ cannot be null
        int retVal = visit.visit(__dict__, arg);
        if (retVal != 0) {
            return retVal;
        } else {
            // clsSet cannot be null
            retVal = visit.visit(clsSet, arg);
            if (retVal != 0) {
                return retVal;
            } else {
                // __mgr__ and __mgr__.topLevelPackage cannot be null
                return visit.visit(__mgr__.topLevelPackage, arg);
            }
        }
    }

    @Override
    public boolean refersDirectlyTo(PyObject ob) {
        return ob != null && (ob == __dict__ || ob == clsSet || ob == __mgr__.topLevelPackage);
    }
}
