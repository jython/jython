/* Copyright (c) Jython Developers */
package org.python.modules.itertools;

import org.python.core.Py;
import org.python.core.PyIterator;
import org.python.core.PyObject;
import org.python.core.PyTuple;
import org.python.core.PyType;
import org.python.core.Visitproc;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedType;

@ExposedType(name = "itertools.izip_longest", base = PyObject.class,
    doc = izipLongest.izip_longest_doc)
public class izipLongest extends PyIterator {

    public static final PyType TYPE = PyType.fromClass(izipLongest.class);
    private PyIterator iter;

    public izipLongest() {
        super();
    }

    public izipLongest(PyType subType) {
        super(subType);
    }

    public izipLongest(PyObject[] iterables, PyObject fillvalue) {
        super();
        izipLongest___init__(iterables, fillvalue);
    }

    public static final String izip_longest_doc =
        "izip_longest(iter1 [,iter2 [...]], [fillvalue=None]) --> izip_longest object\n\n" +
        "Return an izip_longest object whose .next() method returns a tuple where\n" +
        "the i-th element comes from the i-th iterable argument.  The .next()\n" +
        "method continues until the longest iterable in the argument sequence\n" +
        "is exhausted and then it raises StopIteration.  When the shorter iterables\n" +
        "are exhausted, the fillvalue is substituted in their place.  The fillvalue\n" +
        "defaults to None or can be specified by a keyword argument.";

    /**
     * Create an iterator that returns items from the iterable while <code>predicate(item)</code>
     * is true. After which iteration is stopped.
     */
    @ExposedNew
    @ExposedMethod
    final void izipLongest___init__(PyObject[] args, String[] kwds) {

        PyObject[] iterables;
        PyObject fillvalue;

        if (kwds.length == 1 && kwds[0] == "fillvalue") {
            fillvalue = args[args.length - 1];
            iterables = new PyObject[args.length - 1];
            System.arraycopy(args, 0, iterables, 0, args.length - 1);
        } else {
            fillvalue = Py.None;
            iterables = args;
        }
        //XXX error checking on args

        izipLongest___init__(iterables, fillvalue);
    }

    private void izipLongest___init__(final PyObject[] iterables, final PyObject fillvalue) {
        final PyObject iterators[] = new PyObject[iterables.length];
        final boolean exhausted[] = new boolean[iterables.length];
        for (int i = 0; i < iterables.length; i++) {
            iterators[i] = iterables[i].__iter__();
            exhausted[i] = false;
        }

        iter = new itertools.ItertoolsIterator() {
            int unexhausted = iterables.length;

            @Override
            public PyObject __iternext__() {
                PyObject item[] = new PyObject[iterables.length];
                for (int i = 0; i < iterables.length; i++) {
                    if (exhausted[i]) {
                        item[i] = fillvalue;
                    } else {
                        PyObject elem = iterators[i].__iternext__();
                        if (elem == null) {
                            unexhausted--;
                            exhausted[i] = true;
                            item[i] = fillvalue;
                        } else {
                            item[i] = elem;
                        }
                    }
                }
                if (unexhausted == 0) {
                    return null;
                } else {
                    return new PyTuple(item);
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
