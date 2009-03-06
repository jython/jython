/* Copyright (c) Jython Developers */
package org.python.modules._weakref;

import org.python.core.ClassDictInit;
import org.python.core.Py;
import org.python.core.PyIgnoreMethodTag;
import org.python.core.PyList;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.core.PyStringMap;
import org.python.core.PyType;

/**
 * The _weakref module.
 */
public class WeakrefModule implements ClassDictInit {

    public static final PyString __doc__ = new PyString("Weak-reference support module.");

    public static void classDictInit(PyObject dict)
    {
        dict.__setitem__("__doc__", __doc__);
        dict.__setitem__("__name__", Py.newString("_weakref"));
        dict.__setitem__("ref", ReferenceType.TYPE);
        dict.__setitem__("ReferenceType", ReferenceType.TYPE);
        dict.__setitem__("ProxyType", ProxyType.TYPE);
        dict.__setitem__("CallableProxyType", CallableProxyType.TYPE);
        // __doc__, functions: getweakrefcount, getweakrefs, proxy

        dict.__setitem__("classDictInit", null);
    }

    public static ProxyType proxy(PyObject object)  {
        GlobalRef gref = GlobalRef.newInstance(object);
        boolean callable = object.isCallable();
        ProxyType ret = (ProxyType)gref.find(callable ? CallableProxyType.class : ProxyType.class);
        if (ret != null) {
            return ret;
        }
        if (callable) {
            return new CallableProxyType(GlobalRef.newInstance(object), null);
        } else {
            return new ProxyType(GlobalRef.newInstance(object), null);
        }
    }

    public static ProxyType proxy(PyObject object, PyObject callback) {
        if (callback == Py.None) {
            return proxy(object);
        }
        if (object.isCallable()) {
            return new CallableProxyType(GlobalRef.newInstance(object), callback);
        } else {
            return new ProxyType(GlobalRef.newInstance(object), callback);
        }
    }

    public static int getweakrefcount(PyObject o) {
        GlobalRef ref = GlobalRef.newInstance(o);
        if (ref == null) {
            return 0;
        }
        return ref.count();
    }

    public static PyList getweakrefs(PyObject o) {
        GlobalRef ref = GlobalRef.newInstance(o);
        if (ref == null) {
            return new PyList();
        }
        return ref.refs();
    }
}


