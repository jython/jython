// Copyright (c) Corporation for National Research Initiatives
package org.python.core;

import org.python.expose.ExposedDelete;
import org.python.expose.ExposedGet;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedSet;
import org.python.expose.ExposedType;

@ExposedType(name="module")
public class PyModule extends PyObject
{
    private final PyObject module_doc = new PyString(
      "module(name[, doc])\n" +
      "\n" +
      "Create a module object.\n" + 
      "The name must be a string; the optional doc argument can have any type.");

    public PyObject __dict__;

    public PyModule() {
        super();
    }

    public PyModule(PyType subType) {
        super(subType);
    }

    public PyModule(PyType subType, String name) {
        super(subType);
        module_init(new PyString(name), Py.None);
    }

    public PyModule(String name) {
        this(name, null);
    }

    public PyModule(String name, PyObject dict) {
        super();
        __dict__ = dict;
        module_init(new PyString(name), Py.None);
    }

    final void module_init(PyObject name, PyObject doc) {
        ensureDict();
        __dict__.__setitem__("__name__", name);
        __dict__.__setitem__("__doc__", doc);
    }

    @ExposedNew
    @ExposedMethod
    final void module___init__(PyObject[] args, String[] keywords) {
      ArgParser ap = new ArgParser("__init__", args, keywords, new String[] {"name",
                                                                             "doc"});
      PyObject name = ap.getPyObject(0);
      PyObject docs = ap.getPyObject(1, Py.None);
      module_init(name, docs);
    }

    public PyObject fastGetDict() {
        return __dict__;
    }

    @ExposedGet(name="__dict__")
    public PyObject getDict() {
        if (__dict__ == null)
            return Py.None;
        return __dict__;
    }
  
    @ExposedSet(name="__dict__")
    public void setDict(PyObject newDict) {
        throw Py.TypeError("readonly attribute");
    }

    @ExposedDelete(name="__dict__")
    public void delDict() {
        throw Py.TypeError("readonly attribute");
    }

    @ExposedGet(name="__doc__")
    public PyObject getDoc() {
        PyObject d = fastGetDict();
        if (d != null) {
            PyObject doc = d.__finditem__("__doc__");
            if (doc != null) {
                return doc;
            }
        }
        return module_doc;
    }

    protected PyObject impAttr(String attr) {
        PyObject path = __dict__.__finditem__("__path__");
        PyObject pyname = __dict__.__finditem__("__name__");

        if (path == null || pyname == null) return null;

        String name = pyname.__str__().toString();
        String fullName = (name+'.'+attr).intern();

        PyObject ret = null;

        //System.err.println("PyModule.impAttr " + attr + " " + name + " " + fullName);
        if (path == Py.None) {
            /* disabled:
            ret = imp.loadFromClassLoader(
            (name+'.'+attr).intern(),
            Py.getSystemState().getClassLoader());
             */
        }
        else if (path instanceof PyList) {
            ret = imp.find_module(attr, fullName, (PyList)path);
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
        return module___findattr__(attr);
    }

    final PyObject module___findattr__(String attr) {
        PyObject ret;

        if (__dict__ != null) {
          ret = __dict__.__finditem__(attr);
          if (ret != null) return ret;
        }

        ret = super.__findattr__(attr);
        if (ret != null) return ret;

        if (__dict__ == null) {
            return null;
        }

        PyObject pyname = __dict__.__finditem__("__name__");
        if (pyname == null) return null;

        return impHook(pyname.__str__().toString()+'.'+attr);
    }

    public void __setattr__(String attr, PyObject value) {
        module___setattr__(attr, value);
    }

    @ExposedMethod
    final void module___setattr__(String attr, PyObject value) {
        if (attr != "__dict__")
            ensureDict();
        super.__setattr__(attr, value);
    }

    public void __delattr__(String attr) {
        module___delattr__(attr);
    }

    @ExposedMethod
    final void module___delattr__(String attr) {
        super.__delattr__(attr);
    }

    public String toString()  {
        return module_toString();
    }

    @ExposedMethod(names={"__repr__"})
    final String module_toString()  {
        PyObject name = null;
        PyObject filename = null;
        if (__dict__ != null) {
          name = __dict__.__finditem__("__name__");
          filename = __dict__.__finditem__("__file__");
        }
        if (name == null)
            name = new PyString("?");
        if (filename == null)
            filename = new PyString("(built-in)");
        else
            filename = new PyString("from '" + filename + "'");
        return "<module '" + name + "' " + filename + ">";
    }

    public PyObject __dir__() {
        if (__dict__ == null)
            throw Py.TypeError("module.__dict__ is not a dictionary");
        return __dict__.invoke("keys");
    }

    private void ensureDict() {
        if (__dict__ == null)
            __dict__ = new PyStringMap();
    }

    static private PyObject silly_list = null;

    private static PyObject impHook(String name) {
        if (silly_list == null) {
            silly_list = new PyTuple(Py.newString("__doc__"));
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
