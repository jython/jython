// Copyright © Corporation for National Research Initiatives
package org.python.core;

import java.util.*;
import java.io.File;

public class PyJavaDirPackage extends PyObject {
    public String __name__;
    public PyObject __dict__;
    public PyObject __path__;
    //public PyList __all__;

    public static PyClass __class__;
    public PyJavaDirPackage(String name, PyObject path) {
        this(name, path, null);
    }

    public PyObject __dir__() {
        throw Py.TypeError("can not dir a java directory package");
    }    

    protected PyObject __parent__ = null;
    public PyJavaDirPackage(String name, PyObject path, PyObject parent) {
        super(__class__);
        __parent__ = parent;
        __name__ = name;
        __dict__ = new PyStringMap();
        __dict__.__setitem__("__name__", new PyString(__name__));
        __path__ = path;
    }

    public PyObject __findattr__(String name) {
        if (__dict__ == null) __dict__ = new PyStringMap();

        if (name == "__dict__") return __dict__;
        if (name == "__path__") return __path__;

        PyObject ret = __dict__.__finditem__(name);
        if (ret != null)
            return ret;

        String attrName = name;
        if (__name__.length() > 0)
            attrName = __name__+'.'+name;

        // Search the searchPath for this name
        PyObject item;
        int i = 0;
        PyList subPath = null;
        while ((item=__path__.__finditem__(i++)) != null) {
            File testdir = new File(item.toString(), name);
            //System.err.println("test: "+testdir+", "+testdir.is);
            if (testdir.isDirectory()) {
                if (subPath == null)
                    subPath = new PyList();
                subPath.append(new PyString(testdir.getPath()));
            }
            // Maybe check for name.class here?            
        }
        if (subPath != null) {
            ret = new PyJavaDirPackage(attrName, subPath);
            __dict__.__setitem__(name, ret);
            return ret;
        }

        Class c = Py.findClassEx(attrName);
        if (c != null) {
            ret = PyJavaClass.lookup(c);
            __dict__.__setitem__(name, ret);
            return ret;
        }
        return null;
    }

    public String toString()  {
        return "<java dir package "+__name__+" at "+Py.id(this)+">";
    }
}
