// Copyright © Corporation for National Research Initiatives
package org.python.core;

import java.util.*;
import java.io.File;

public class PyJavaPackage extends PyObject {
    public String __name__;
    public PyObject __dict__;
    public String _unparsedAll;
    public String __file__;
    //public PyList __all__;

    public static PyClass __class__;
    public PyJavaPackage(String name) {
        this(name, null, null);
    }
    
    protected PyJavaPackage __parent__ = null;    
    public PyJavaPackage(String name, String jarfile) {
        this(name, null, jarfile);
    }
    
    public PyJavaPackage(String name, PyJavaPackage parent, String jarfile) {
        super(__class__);
        __parent__ = parent;
        if (jarfile == null && parent != null)
	    jarfile = __parent__.__file__;
        __file__ = jarfile;
        __name__ = name;
        __dict__ = new PyStringMap();
        __dict__.__setitem__("__name__", new PyString(__name__));
    }

    public PyJavaPackage addPackage(String name) {
        return addPackage(name, null);
    }
    
    public PyJavaPackage addPackage(String name, String jarfile) {
        int dot = name.indexOf('.');
        String firstName=name;
        String lastName=null;
        if (dot != -1) {
            firstName = name.substring(0,dot);
            lastName = name.substring(dot+1, name.length());
        }
        if (jarfile != null) {
            if (!jarfile.equals(__file__)) {
                __file__ = null;
            }
        }
        firstName = firstName.intern();
        PyJavaPackage p = (PyJavaPackage)__dict__.__finditem__(firstName);
        if (p == null) {
            p = new PyJavaPackage(__name__+'.'+firstName, this, jarfile);
        }
        __dict__.__setitem__(firstName, p);
        if (lastName != null)
	    return p.addPackage(lastName, jarfile);
        else
	    return p;
    }

    private PyObject __path__ = null;
    protected PyObject getPath() {
        if (__path__ != null)
	    return __path__;
        //System.err.println("finding path for: "+__name__);
        
        if (__name__.length() == 0) {
            __path__ = Py.getSystemState().path;
            return __path__;
        }
        
        PyJavaPackage parent = __parent__;
        PyList rootPath;
        if (parent == null) {
            rootPath = Py.getSystemState().path;
        } else {
            PyObject tmp = __parent__.getPath();
            if (tmp == Py.None) {
                __path__ = Py.None;
                return __path__;
            }
            rootPath = (PyList)tmp;
        }
        
        PyList path = new PyList();
        
        if (Py.frozen) {
            __path__ = path;
            return __path__;
        }
        
        // Search through directories in rootPath looking for appropriate
        // subdir
        int n = rootPath.__len__();

        String name = __name__;
        int dot = name.lastIndexOf('.');
        if (dot != -1)
	    name = name.substring(dot+1, name.length());

        for (int i=0; i<n; i++) {
            String dirName = rootPath.get(i).toString();
            File dir = new File(dirName, name);
            //System.err.println("pkg: "+__name__+", looking for: "+dir);
            if (dir.isDirectory()) {
                //System.err.println("found: "+dir.getPath());
                path.append(new PyString(dir.getPath()));
            }
        }
        //Py.println(path);
        if (path.__len__() == 0) {
            __path__ = Py.None;
        } else {
            __path__ = path;
        }
        return __path__;
    }
    
    private PyObject __jdir__;
    private boolean jdirinit = false;

    public PyObject getDirPackage() {
        if (jdirinit)
	    return __jdir__;
        jdirinit = true;
        
        if (__name__.length() == 0) {
            __jdir__ = PySystemState.packageManager.topDirectoryPackage;
            return __jdir__;
        }
        
        PyJavaPackage parent = __parent__;
        PyObject ppkg;
        if (parent == null) {
            ppkg = PySystemState.packageManager.topDirectoryPackage;
        } else {
            ppkg = parent.getDirPackage();
        }
        
        if (ppkg == null) {
            __jdir__ = null;
            return null;
        }

        String name = __name__;
        int dot = name.lastIndexOf('.');
        if (dot != -1)
	    name = name.substring(dot+1, name.length()).intern();
        __jdir__ = ppkg.__findattr__(name);
        return __jdir__;
    }
 
    private void initialize() {
        if (__dict__ == null)
	    __dict__ = new PyStringMap();
        
        if (_unparsedAll != null) {
            //__all__ = new PyList();
            StringTokenizer tok = new StringTokenizer(_unparsedAll, ",");
            while  (tok.hasMoreTokens())  {
                String p = tok.nextToken();
                String cname = p.trim().intern();
                __dict__.__setitem__(cname, PyJavaClass.lookup(__name__+'.'+
							       cname, null));
                //__all__.append(new PyString(p.trim()));
            }
            _unparsedAll = null;
        }
    }

    public PyObject __dir__() {
        initialize();
        if (__dict__ instanceof PyStringMap) {
            return ((PyStringMap)__dict__).keys();
        } else {
            return __dict__.invoke("keys");
        }
    }


    public PyObject __findattr__(String name) {
        if (__dict__ == null)
	    __dict__ = new PyStringMap();
                    
        PyObject ret = __dict__.__finditem__(name);
        if (ret != null)
	    return ret;
        Class c;
        if (__name__.length()>0)
	    c = Py.findClass(__name__+'.'+name);
        else
	    c = Py.findClass(name);

        if (c != null) {
            ret = PyJavaClass.lookup(c);
            __dict__.__setitem__(name, ret);
            return ret;
        }
        if (name == "__name__") return new PyString(__name__);
        if (name == "__dict__") return __dict__;
        if (name == "__file__" && __file__ != null)
	    return new PyString(__file__);
                
        // Name not found - check for Java dir package
        PyObject pyjd = getDirPackage();
        if (pyjd != null)
	    ret = pyjd.__findattr__(name);
        
        if (ret == null) {
            // Name not found - check for a Python package/module
            PyObject path = getPath();
            if (path == Py.None)
		return null;
            ret = imp.loadFromPath(name, (__name__+'.'+name).intern(),
				   (PyList)path);
        }
        
        if (ret != null)
	    __dict__.__setitem__(name, ret);
        return ret;
        //return super.__findattr__(name);
    }

    public String toString()  {
        return "<java package "+__name__+" at "+Py.id(this)+">";
    }
}
