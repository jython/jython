/* Copyright (c) Jython Developers */
package org.python.modules._weakref;

import org.python.core.ArgParser;
import org.python.core.Py;
import org.python.core.PyNewWrapper;
import org.python.core.PyObject;
import org.python.core.PyType;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedType;

@ExposedType(name = "weakref")
public class ReferenceType extends AbstractReference {

    public static final PyType TYPE = PyType.fromClass(ReferenceType.class);

    public ReferenceType(PyType subType, GlobalRef gref, PyObject callback) {
        super(subType, gref, callback);
    }

    public ReferenceType(GlobalRef gref, PyObject callback) {
        super(gref, callback);
    }

    @ExposedNew
    static final PyObject weakref___new__(PyNewWrapper new_, boolean init, PyType subtype,
                                          PyObject[] args, String[] keywords) {
        ArgParser ap = new ArgParser("__new__", args, keywords, new String[] {"ob", "callback"},
                                     1);
        ap.noKeywords();
        PyObject ob = ap.getPyObject(0);
        PyObject callback = ap.getPyObject(1, null);
        if (callback == Py.None) {
            callback = null;
        }

        GlobalRef gref = GlobalRef.newInstance(ob);
        if (new_.for_type == subtype) {
            // XXX: Reject types that aren't weak referenceable
            if (callback == null) {
                ReferenceType ret = (ReferenceType)gref.find(ReferenceType.class);
                if (ret != null) {
                    // We can re-use an existing reference.
                    return ret;
                }
            }
            return new ReferenceType(gref, callback);
        } else {
            return new ReferenceTypeDerived(subtype, gref, callback);
        }
    }

    public PyObject __call__() {
        return weakref___call__();
    }

    @ExposedMethod
    final PyObject weakref___call__() {
        return Py.java2py(gref.get());
    }

    public String toString() {
        PyObject obj = (PyObject)gref.get();
        if (obj == Py.None) {
            return String.format("<weakref at %s; dead>", Py.idstr(this));
        }

        PyObject nameObj = obj.__findattr__("__name__");
        if (nameObj != null) {
            return String.format("<weakref at %s; to '%.50s' at %s (%s)>", Py.idstr(this),
                                 obj.getType().fastGetName(), Py.idstr(obj), nameObj);
        }
        return String.format("<weakref at %s; to '%.50s' at %s>", Py.idstr(this),
                             obj.getType().fastGetName(), Py.idstr(obj));
    }
}
