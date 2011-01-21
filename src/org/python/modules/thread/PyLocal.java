/* Copyright (c) Jython Developers */
package org.python.modules.thread;

import org.python.core.Py;
import org.python.core.PyDictionary;
import org.python.core.PyNewWrapper;
import org.python.core.PyObject;
import org.python.core.PyType;
import org.python.expose.ExposedGet;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedSet;
import org.python.expose.ExposedType;

@ExposedType(name = "thread._local")
public class PyLocal extends PyObject {

    public static final PyType TYPE = PyType.fromClass(PyLocal.class);

    private ThreadLocal<PyDictionary> tdict = new ThreadLocal<PyDictionary>();

    private PyObject args[];

    private String keywords[];

    public PyLocal() {
        this(TYPE);
    }

    public PyLocal(PyType subType) {
        super(subType);
        // Don't lazy load the underlying dict in the insantiating thread; that would call
        // __init__ a the second time
        tdict.set(new PyDictionary());
    }

    @ExposedNew
    final static PyObject _local___new__(PyNewWrapper new_,
                                        boolean init,
                                        PyType subtype,
                                        PyObject[] args,
                                        String[] keywords) {
        PyObject[] where = new PyObject[1];
        subtype.lookup_where("__init__", where);
        if (where[0] == PyObject.TYPE && args.length > 0) {
            throw Py.TypeError("Initialization arguments are not supported");
        }

        PyLocal newobj;
        if (new_.getWrappedType() == subtype) {
            newobj = new PyLocal();
        } else {
            newobj = new PyLocalDerived(subtype);
        }
        newobj.args = args;
        newobj.keywords = keywords;

        return newobj;
    }

    @Override
    @ExposedGet(name = "__dict__")
    public PyObject getDict() {
        return fastGetDict();
    }

    @Override
    @ExposedSet(name = "__dict__")
    public void setDict(PyObject dict) {
        super.setDict(dict);
    }

    @Override
    public PyObject fastGetDict() {
        PyDictionary ldict = tdict.get();
        if (ldict == null) {
            ldict = new PyDictionary();
            tdict.set(ldict);
            dispatch__init__(args, keywords);
        }
        return ldict;
    }
}
