// Copyright (c) Corporation for National Research Initiatives
package org.python.core;

public class PyModule extends PyObject
{
    public PyObject __dict__;

    public PyModule(String name, PyObject dict) {
        if (dict == null)
            __dict__ = new PyStringMap();
        else
            __dict__ = dict;
        __dict__.__setitem__("__name__", new PyString(name));
        __dict__.__setitem__("__doc__", Py.None);
    }

    protected PyObject impAttr(String attr) {
        PyObject path = __dict__.__finditem__("__path__");
        PyObject pyname = __dict__.__finditem__("__name__");

        if (path == null || pyname == null) return null;

        String name = pyname.__str__().toString();
        String fullName = (name+'.'+attr).intern();

        PyObject ret = null;
        
        if (path == Py.None) {
            /* disabled:
            ret = imp.loadFromClassLoader(
            (name+'.'+attr).intern(),
            Py.getSystemState().getClassLoader());
             */
        }
        else if (path instanceof PyList) {
            ret = imp.loadFromPath(attr, fullName,(PyList)path);
        }
        else {
            throw Py.TypeError("__path__ must be list or None");
        }

        if (ret == null) {
            ret = PySystemState.packageManager.lookupName(fullName);
        }

        if (ret != null) {
            // Allow a package component to change its own meaning 
            PyObject tmp = Py.getSystemState().modules.__finditem__(fullName);
            if (tmp != null) ret = tmp;
            __dict__.__setitem__(attr, ret);
            return ret;
        }

        return null;

    }

    public PyObject __findattr__(String attr) {
        PyObject ret;
        ret = __dict__.__finditem__(attr);
        if (ret != null) return ret;

        ret = super.__findattr__(attr);
        if (ret != null) return ret;

        PyObject pyname = __dict__.__finditem__("__name__");
        if (pyname == null) return null;

        return impHook(pyname.__str__().toString()+'.'+attr);
    }

    public void __setattr__(String attr, PyObject value) {
        __dict__.__setitem__(attr, value);
    }

    public void __delattr__(String attr) {
        __dict__.__delitem__(attr);
    }

    public String toString()  {
        return "<module "+__dict__.__finditem__("__name__")+" at "+
            Py.id(this)+">";
    }

    static private PyObject silly_list = null;

    private static PyObject impHook(String name) {
        if (silly_list == null) {
            silly_list = new PyTuple(new PyString[] {
                                     Py.newString("__doc__"),});
        }
        try {
            return __builtin__.__import__(name, null, null, silly_list);
        } catch(PyException e) {
            if (Py.matchException(e, Py.ImportError)) {
                return null;
            }
            throw e;
        }
    }

}
