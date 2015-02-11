/* Copyright (c) Jython Developers */
package org.python.core;

import org.python.expose.ExposedMethod;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedType;

/**
 * The Python builtin enumerate type.
 */
@ExposedType(name = "enumerate", base = PyObject.class, doc = BuiltinDocs.enumerate_doc)
public class PyEnumerate extends PyIterator {
    //note: Already implements Traverseproc, inheriting it from PyIterator

    public static final PyType TYPE = PyType.fromClass(PyEnumerate.class);

    /** Current index of enumeration. */
    private PyObject index;     // using PyObject so we are not limited to sys.maxint or Integer.MAX_VALUE

    /** Secondary iterator of enumeration. */
    private PyObject sit;

    public PyEnumerate(PyType subType) {
        super(subType);
    }

    public PyEnumerate(PyType subType, PyObject seq, PyObject start) {
        super(subType);
        index = start;
        sit = seq.__iter__();
    }

    public PyEnumerate(PyObject seq, PyObject start) {
        this(TYPE, seq, start);
    }

    public PyObject next() {
        return enumerate_next();
    }

    @ExposedMethod(doc = BuiltinDocs.enumerate_next_doc)
    final PyObject enumerate_next() {
        return doNext(enumerate___iternext__());
    }

    @ExposedMethod(doc = BuiltinDocs.enumerate___iter___doc)
    final PyObject enumerate___iter__() {
        return super.__iter__();
    }

    @ExposedNew
    public final static PyObject enumerate_new(PyNewWrapper new_, boolean init, PyType subtype,
                                               PyObject[] args, String[] keywords) {
        if (args.length > 2 || args.length <= 0) {
            throw PyBuiltinCallable.DefaultInfo.unexpectedCall(args.length, true, "enumerate", 1, 2);
        }

        ArgParser ap = new ArgParser("enumerate", args, keywords, new String[] {"sequence", "start"});
        PyObject seq = ap.getPyObject(0);
        PyObject start = ap.getPyObject(1, Py.newInteger(0));
        if (!start.isIndex()) {
            throw Py.TypeError("an integer is required");
        }

        if (new_.for_type == subtype) {
            return new PyEnumerate(seq, start);
        } else {
            return new PyEnumerateDerived(subtype, seq, start);
        }
    }

    public PyObject __iternext__() {
        return enumerate___iternext__();
    }

    final PyObject enumerate___iternext__() {
        PyObject nextItem;

        nextItem = sit.__iternext__();
        if (nextItem == null) {
            if (sit instanceof PyIterator && ((PyIterator)sit).stopException != null) {
                stopException = ((PyIterator)sit).stopException;
            }
            return null;
        }

        PyObject next = new PyTuple(index, nextItem);
        index = index.__radd__(Py.newInteger(1));

        return next;
    }


    /* Traverseproc implementation */
    @Override
    public int traverse(Visitproc visit, Object arg) {
        int retValue = super.traverse(visit, arg);
        if (retValue != 0) {
            return retValue;
        }
        if (index != null) {
            retValue = visit.visit(index, arg);
            if (retValue != 0) {
                return retValue;
            }
        }
        return sit == null ? 0 : visit.visit(sit, arg);
    }

    @Override
    public boolean refersDirectlyTo(PyObject ob) {
        return ob != null && (ob == index || ob == sit || super.refersDirectlyTo(ob));
    }
}
