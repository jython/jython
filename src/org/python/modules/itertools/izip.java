/* Copyright (c) Jython Developers */
package org.python.modules.itertools;

import org.python.core.Py;
import org.python.core.PyIterator;
import org.python.core.PyObject;
import org.python.core.PyTuple;
import org.python.core.PyType;
import org.python.core.PyXRange;
import org.python.core.Visitproc;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedType;

@ExposedType(name = "itertools.izip", base = PyObject.class, doc = izip.izip_doc)
public class izip extends PyIterator {

    public static final PyType TYPE = PyType.fromClass(izip.class);
    private PyIterator iter;

    public static final String izip_doc =
        "izip(iter1 [,iter2 [...]]) --> izip object\n\n" +
        "Return an izip object whose .next() method returns a tuple where\n" +
        "the i-th element comes from the i-th iterable argument.  The .next()\n" +
        "method continues until the shortest iterable in the argument sequence\n" +
        "is exhausted and then it raises StopIteration.  Works like the zip()\n" +
        "function but consumes less memory by returning an iterator instead of\n" +
        "a list.";

    public izip() {
        super();
    }

    public izip(PyType subType) {
        super(subType);
    }

    public izip(PyObject... args) {
        super();
        izip___init__(args);
    }


    /**
     * Create an iterator whose <code>next()</code> method returns a tuple where the i-th element
     * comes from the i-th iterable argument. Continues until the shortest iterable is exhausted.
     * (Code in this method is based on __builtin__.zip()).
     * 
     */
    @ExposedNew
    @ExposedMethod
    final void izip___init__(PyObject[] args, String[] kwds) {
        if (kwds.length > 0) {
            throw Py.TypeError(String.format("izip does not take keyword arguments"));
        }
        izip___init__(args);
    }

    private void izip___init__(final PyObject[] argstar) {

        final int itemsize = argstar.length;
        
        if (itemsize == 0) {
            iter = (PyIterator)(new PyXRange(0).__iter__());
            return;
        }

        // Type check the arguments; they must be sequences.
        final PyObject[] iters = new PyObject[itemsize];

        for (int i = 0; i < itemsize; i++) {
            PyObject iter = argstar[i].__iter__();
            if (iter == null) {
                throw Py.TypeError("izip argument #" + (i + 1)
                        + " must support iteration");
            }
            iters[i] = iter;
        }

        iter = new itertools.ItertoolsIterator() {

            public PyObject __iternext__() {
                if (itemsize == 0)
                    return null;

                PyObject[] next = new PyObject[itemsize];
                PyObject item;

                for (int i = 0; i < itemsize; i++) {

                    item = nextElement(iters[i]);

                    if (item == null) {
                        return null;
                    }
                    next[i] = item;
                }
                return new PyTuple(next);
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
