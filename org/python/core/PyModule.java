// Copyright © Corporation for National Research Initiatives
package org.python.core;

public class PyModule extends PyObject {
    public PyObject __dict__;

    public PyModule(String name, PyObject dict) {
	if (dict == null)
	    __dict__ = new PyStringMap();
	else
	    __dict__ = dict;
	__dict__.__setitem__("__name__", new PyString(name));
    }

    public PyObject __findattr__(String attr) {
	PyObject ret;
	ret = __dict__.__finditem__(attr);
	if (ret == null) {
	    PyObject path = __dict__.__finditem__("__path__");
	    if (path != null) {
		PyObject pyname = __dict__.__finditem__("__name__");
		        
		if (pyname != null) {
		    String name = pyname.__str__().toString();
    		        
		    if (path == Py.None) {
			ret = imp.loadFromClassLoader(
			    (name+'.'+attr).intern(),
			    Py.getSystemState().getClassLoader());
		    }
		    else if (path instanceof PyList) {
			ret = imp.loadFromPath(attr, (name+'.'+attr).intern(),
					       (PyList)path);
		    }
		    else {
			throw Py.TypeError("__path__ must be list or None");
		    }
    		        
		    if (ret == null) {
			ret = PySystemState.packageManager.lookupName(
			    name+'.'+attr);
			/*Class c = Py.findClass(name+'.'+attr);
			  if (c != null)
			  ret = PyJavaClass.lookup(c);*/
		    }
                	    
		    if (ret != null) {
			// Allow a package component to change its own meaning
			PyObject tmp = __dict__.__finditem__(attr);
			if (tmp != null)
			    ret = tmp;
			__dict__.__setitem__(attr, ret);
			return ret;
		    }
		}
		// Not found in package
	    }
	    return super.__findattr__(attr);
	}
	return ret; //ret._doget(this);
    }

    public void __setattr__(String attr, PyObject value) throws PyException {
	__dict__.__setitem__(attr, value);
    }

    public void __delattr__(String attr) throws PyException {
	__dict__.__delitem__(attr);
    }

    public String toString()  {
	return "<module "+__dict__.__finditem__("__name__")+" at "+
	    Py.id(this)+">";
    }
}
