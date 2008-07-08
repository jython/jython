/* Copyright (c) Jython Developers */
package org.python.core;

import org.python.expose.ExposedMethod;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedType;

/**
 * The Python builtin enumerate type.
 */
@ExposedType(name = "enumerate", base = PyObject.class)
public class PyEnumerate extends PyIterator {

    public static final PyType TYPE = PyType.fromClass(PyEnumerate.class);

    /** Current index of enumeration. */
    private long index;

    /** Secondary iterator of enumeration. */
    private PyObject sit;

    public PyEnumerate(PyType subType) {
        super(subType);
    }

    public PyEnumerate(PyType subType, PyObject seq) {
        super(subType);
        index = 0;
        sit = seq.__iter__();
    }

    public PyEnumerate(PyObject seq) {
        this(TYPE, seq);
    }

    public PyObject next() {
        return enumerate_next();
    }

    @ExposedMethod
    final PyObject enumerate_next() {
        return doNext(enumerate___iternext__());
    }

    @ExposedMethod
    final PyObject enumerate___iter__() {
        return super.__iter__();
    }

    @ExposedNew
    public final static PyObject enumerate_new(PyNewWrapper new_, boolean init, PyType subtype,
                                               PyObject[] args, String[] keywords) {
        if (args.length != 1) {
            throw PyBuiltinFunction.DefaultInfo.unexpectedCall(args.length, false, "enumerate", 0,
                                                               1);
        }
        if (new_.for_type == subtype) {
            return new PyEnumerate(args[0]);
        } else {
            return new PyEnumerateDerived(subtype, args[0]);
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
