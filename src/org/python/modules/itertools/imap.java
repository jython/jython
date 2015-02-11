/* Copyright (c) Jython Developers */
package org.python.modules.itertools;

import org.python.core.Py;
import org.python.core.PyIterator;
import org.python.core.PyObject;
import org.python.core.PyTuple;
import org.python.core.PyType;
import org.python.core.Visitproc;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedType;

@ExposedType(name = "itertools.imap", base = PyObject.class, doc = imap.imap_doc)
public class imap extends PyIterator {

    public static final PyType TYPE = PyType.fromClass(imap.class);
    private PyIterator iter;

    public static final String imap_doc =
        "'map(func, *iterables) --> imap object\n\n" +
        "Make an iterator that computes the function using arguments from\n" +
        "each of the iterables.\tLike map() except that it returns\n" +
        "an iterator instead of a list and that it stops when the shortest\n" +
        "iterable is exhausted instead of filling in None for shorter\n" +
        "iterables.";

    public imap() {
        super();
    }

    public imap(PyType subType) {
        super(subType);
    }

    public imap(PyObject... args) {
        super();
        imap___init__(args);
    }


    /**
     * Works as <code>__builtin__.map()</code> but returns an iterator instead of a list. (Code in
     * this method is based on __builtin__.map()).
     */
    @ExposedNew
    @ExposedMethod
    final void imap___init__(PyObject[] args, String[] kwds) {
        if (kwds.length > 0) {
            throw Py.TypeError(String.format("imap does not take keyword arguments"));
        }
        imap___init__(args);
    }

    private void imap___init__(final PyObject[] argstar) {
        if (argstar.length < 2) {
            throw Py.TypeError("imap requires at least two arguments");
        }
        final int n = argstar.length - 1;
        final PyObject func = argstar[0];

        final PyObject[] iterables = new PyObject[n];
        for (int j = 0; j < n; j++) {
            iterables[j] = Py.iter(argstar[j + 1], "argument " + (j + 1)
                    + " to imap() must support iteration");
        }
        iter = new PyIterator() {
            PyObject[] args = new PyObject[n];

            PyObject element = null;

            public PyObject __iternext__() {

                for (int i = 0; i < n; i++) {
                    if ((element = iterables[i].__iternext__()) != null) {
                        // collect the arguments for the func
                        args[i] = element;
                    } else {
                        // break iteration
                        return null;
                    }
                }
                if (func == Py.None) {
                    // if None is supplied as func we just return what's in
                    // the iterable(s)
                    if (n == 1) {
                        return args[0];
                    } else {
                        return new PyTuple(args.clone());
                    }
                } else {
                    return func.__call__(args);
                }
            }
        };
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
