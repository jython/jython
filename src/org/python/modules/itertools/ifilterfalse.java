/* Copyright (c) Jython Developers */
package org.python.modules.itertools;
import org.python.core.ArgParser;
import org.python.core.PyIterator;
import org.python.core.PyObject;
import org.python.core.PyType;
import org.python.core.Visitproc;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedType;

@ExposedType(name = "itertools.ifilterfalse", base = PyObject.class,
    doc = ifilterfalse.ifilterfalse_doc)
public class ifilterfalse extends PyIterator {

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

    public static final String ifilterfalse_doc =
        "'ifilterfalse(function or None, sequence) --> ifilterfalse object\n\n" +
        "Return those items of sequence for which function(item) is false.\n" +
        "If function is None, return the items that are false.'";

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

    public PyObject __iternext__() {
        return iter.__iternext__();
    }

    @ExposedMethod
    @Override
    public PyObject next() {
        return doNext(__iternext__());
    }


    /* Traverseproc implementation */
    @Override
    public int traverse(Visitproc visit, Object arg) {
        int retVal = super.traverse(visit, arg);
        if (retVal != 0) {
            return retVal;
        }
        return iter != null ? visit.visit(iter, arg) : 0;
    }

    @Override
    public boolean refersDirectlyTo(PyObject ob) {
        return ob != null && (iter == ob || super.refersDirectlyTo(ob));
    }
}
