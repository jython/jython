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

	PyObject ret = __dict__.__finditem__(name);
	if (ret != null) return ret;

	String attrName = name;
	if (__name__.length()>0) attrName = __name__+'.'+name;

        // Search the searchPath for this name
        PyObject item;
        int i = 0;
        while ( (item = __path__.__finditem__(i++)) != null) {
            File testdir = new File(item.toString(), name);
            if (testdir.isDirectory()) {
                ret = new PyJavaDirPackage(attrName, new PyList(
			new PyObject[] {new PyString(testdir.getPath())}));
                __dict__.__setitem__(name, ret);
            }
            // Maybe check for name.class here?
        }

	Class c = Py.findClass(attrName);
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
