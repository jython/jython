/* Copyright (c) Jython Developers */
package org.python.modules.itertools;
import org.python.core.ArgParser;
import org.python.core.Py;
import org.python.core.PyIterator;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.core.PyTuple;
import org.python.core.PyType;
import org.python.expose.ExposedClassMethod;
import org.python.expose.ExposedGet;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedType;

import java.util.ArrayList;

@ExposedType(name = "itertools.ifilterfalse", base = PyObject.class)
public class ifilterfalse extends PyObject {

    public static final PyType TYPE = PyType.fromClass(ifilterfalse.class);
    private PyIterator iter;

    public ifilterfalse() {
        super();
    }

    public ifilterfalse(PyType subType) {
        super(subType);
    }

    public ifilterfalse(PyObject predicate, PyObject iterable) {
        super();
        ifilterfalse___init__(predicate, iterable);
    }

    @ExposedGet
    public static PyString __doc__ = new PyString(
            "'ifilterfalse(function or None, sequence) --> ifilterfalse object\n\n"
                    + "Return those items of sequence for which function(item) is false.\nIf function is None, "
                    + "return the items that are false.'");

    /**
     * Creates an iterator that returns the items of the iterable for which
     * <code>predicate(item)</code> is <code>false</code>. If <code>predicate</code> is null
     * (None) return the items that are false.
     */
    @ExposedNew
    @ExposedMethod
    final void ifilterfalse___init__(PyObject[] args, String[] kwds) {
        ArgParser ap = new ArgParser("ifilter", args, kwds, new String[] {"predicate", "iterable"}, 2);
        ap.noKeywords();
        PyObject predicate = ap.getPyObject(0);
        PyObject iterable = ap.getPyObject(1);
        ifilterfalse___init__(predicate, iterable);
    }

    public void ifilterfalse___init__(PyObject predicate, PyObject iterable) {
        iter = new itertools.FilterIterator(predicate, iterable, false);
    }

    @ExposedMethod
    public PyObject __iter__() {
        return iter;
    }

    @ExposedMethod
    public PyObject next() {
        return iter.next();
    }

}
