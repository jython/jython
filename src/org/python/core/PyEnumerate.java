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

    public static final PyType TYPE = PyType.fromClass(PyEnumerate.class);

    /** Current index of enumeration. */
    private long index;

    /** Secondary iterator of enumeration. */
    private PyObject sit;

    public PyEnumerate(PyType subType) {
        super(subType);
    }

    public PyEnumerate(PyType subType, PyObject seq, long start) {
        super(subType);
        index = start;
        sit = seq.__iter__();
    }

    public PyEnumerate(PyObject seq, long start) {
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
            throw PyBuiltinCallable.DefaultInfo.unexpectedCall(args.length, false, "enumerate", 0,
                                                               1);
        }

        ArgParser ap = new ArgParser("enumerate", args, keywords, new String[] {"sequence", "start"});
        PyObject seq = ap.getPyObject(0);
        long start = (long) ap.getInt(1, 0);

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

        return new PyTuple(new PyInteger((int)index++), nextItem);
    }
}
