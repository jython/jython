/* Copyright (c) Jython Developers */
package org.python.modules.itertools;

import org.python.core.ArgParser;
import org.python.core.PyIterator;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.core.PyType;
import org.python.expose.ExposedGet;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedType;

@ExposedType(name = "itertools.dropwhile", base = PyObject.class)
public class dropwhile extends PyObject {

    public static final PyType TYPE = PyType.fromClass(dropwhile.class);
    private PyIterator iter;

    public dropwhile() {
        super();
    }

    public dropwhile(PyType subType) {
        super(subType);
    }

    public dropwhile(PyObject predicate, PyObject iterable) {
        super();
        dropwhile___init__(predicate, iterable);
    }

    @ExposedGet
    public static PyString __doc__ = new PyString(
        "dropwhile(predicate, iterable) --> dropwhile object\n\n"
            + "Drop items from the iterable while predicate(item) is true.\n"
            + "Afterwards, return every element until the iterable is exhausted.");

    /**
     * Create an iterator that drops items from the iterable while <code>predicate(item)</code>
     * equals true. After which every remaining item of the iterable is returned.
     */
    @ExposedNew
    @ExposedMethod
    final void dropwhile___init__(PyObject[] args, String[] kwds) {
        ArgParser ap = new ArgParser("dropwhile", args, kwds, new String[] {"predicate", "iterable"}, 2);
        ap.noKeywords();
        PyObject predicate = ap.getPyObject(0);
        PyObject iterable = ap.getPyObject(1);
        dropwhile___init__(predicate, iterable);
    }

    private void dropwhile___init__(PyObject predicate, PyObject iterable) {
        iter = new itertools.WhileIterator(predicate, iterable, true);
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
