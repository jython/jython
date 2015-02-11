/* Copyright (c) Jython Developers */
package org.python.modules.thread;

import org.python.core.Py;
import org.python.core.PyDictionary;
import org.python.core.PyNewWrapper;
import org.python.core.PyObject;
import org.python.core.PyType;
import org.python.core.Traverseproc;
import org.python.core.Visitproc;
import org.python.expose.ExposedGet;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedSet;
import org.python.expose.ExposedType;

@ExposedType(name = "thread._local")
public class PyLocal extends PyObject implements Traverseproc {

    public static final PyType TYPE = PyType.fromClass(PyLocal.class);

    private ThreadLocal<Object[]> tdict = new ThreadLocal() {
        @Override
        protected Object initialValue() {
            return new Object[1];
        }
    };

    private PyObject args[];

    private String keywords[];

    public PyLocal() {
        this(TYPE);
    }

    public PyLocal(PyType subType) {
        super(subType);
        // Don't lazy load the underlying dict in the instantiating thread; that would
        // call __init__ a the second time
        tdict.set(new Object[] { new PyDictionary() });
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
        Object[] local = tdict.get();
        PyDictionary ldict = (PyDictionary)(local[0]);
        if (ldict == null) {
            ldict = new PyDictionary();
            local[0] = ldict;
            dispatch__init__(args, keywords);
        }
        return ldict;
    }


    /* Traverseproc implementation */
    @Override
    public int traverse(Visitproc visit, Object arg) {
        int retVal;
        if (args != null) {
            for (PyObject ob: args) {
                if (ob != null) {
                    retVal = visit.visit(ob, arg);
                    if (retVal != 0) {
                        return retVal;
                    }
                }
                
            }
        }
        Object[] ob0 = tdict.get();
        if (ob0 != null) {
            for (Object obj: ob0) {
                if (obj != null) {
                    if (obj instanceof PyObject) {
                        retVal = visit.visit((PyObject) obj, arg);
                        if (retVal != 0) {
                            return retVal;
                        }
                    } else if (obj instanceof Traverseproc) {
                        retVal = ((Traverseproc) obj).traverse(visit, arg);
                        if (retVal != 0) {
                            return retVal;
                        }
                    }
                }
            }
        }
        return 0;
    }

    @Override
    public boolean refersDirectlyTo(PyObject ob) throws UnsupportedOperationException {
        if (ob == null) {
            return false;
        }
        if (args != null) {
            for (PyObject obj: args) {
                if (obj == ob) {
                    return true;
                }
                
            }
        }
        throw new UnsupportedOperationException();
    }
}
