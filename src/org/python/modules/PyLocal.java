package org.python.modules;

import org.python.core.Py;
import org.python.core.PyDictionary;
import org.python.core.PyNewWrapper;
import org.python.core.PyObject;
import org.python.core.PyType;
import org.python.expose.ExposedGet;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedSet;
import org.python.expose.ExposedType;

@ExposedType(name = "local")
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
        // Because the instantiation of a type instance in PyType.invoke_new_
        // calls dispatch__init__, we call tdict.set here so dispatch__init__
        // doesn't get called a second time for a thread in fastGetDict
        tdict.set(new PyDictionary());
    }

    @ExposedNew
    final static PyObject local___new__(PyNewWrapper new_,
                                        boolean init,
                                        PyType subtype,
                                        PyObject[] args,
                                        String[] keywords) {
        PyLocal newobj;
        if (new_.getWrappedType() == subtype) {
            newobj = new PyLocal();
        } else {
            newobj = new PyLocalDerived(subtype);
        }
        if (init) {
            newobj.local___init__(args, keywords);
        }
        return newobj;
    }

    @ExposedMethod
    final void local___init__(PyObject[] args, String[] keywords) {
        PyObject[] where = new PyObject[1];
        getType().lookup_where("__init__", where);
        if (where[0] == TYPE && args.length > 0) {
            throw Py.TypeError("Initialization arguments are not supported");
        }
        this.args = args;
        this.keywords = keywords;
    }

    @Override
    @ExposedGet(name = "__dict__")
    public PyObject getDict() {
        return fastGetDict();
    }

    @Override
    @ExposedSet(name = "__dict__")
    public void setDict(PyObject dict) {
        setDict(dict);
    }

    @Override
    public synchronized PyObject fastGetDict() {
        PyDictionary ldict = tdict.get();
        if (ldict == null) {
            ldict = new PyDictionary();
            tdict.set(ldict);
            dispatch__init__(getType(), args, keywords);
        }
        return ldict;
    }
}
