/* Copyright (c) 2012 Jython Developers */
package org.python.modules.itertools;

import org.python.core.Py;
import org.python.core.PyIterator;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.core.PyTuple;
import org.python.core.PyType;
import org.python.expose.ExposedGet;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedType;

@ExposedType(name = "itertools.product", base = PyObject.class)
public class product extends PyObject {

    public static final PyType TYPE = PyType.fromClass(product.class);
    private PyIterator iter;

    @ExposedGet
    public static PyString __doc__ = new PyString(
        "product(*iterables) --> product object\n\n" +
            "Cartesian product of input iterables.  Equivalent to nested for-loops.\n\n" +
            "For example, product(A, B) returns the same as:  ((x,y) for x in A for y in B).\n" +
            "The leftmost iterators are in the outermost for-loop, so the output tuples\n" +
            "cycle in a manner similar to an odometer (with the rightmost element changing\n" +
            "on every iteration).\n\n" +
            "To compute the product of an iterable with itself, specify the number\n" +
            "of repetitions with the optional repeat keyword argument. For example,\n" +
            "product(A, repeat=4) means the same as product(A, A, A, A).\n\n" +
            "product('ab', range(3)) --> ('a',0) ('a',1) ('a',2) ('b',0) ('b',1) ('b',2)\n" +
            "product((0,1), (0,1), (0,1)) --> (0,0,0) (0,0,1) (0,1,0) (0,1,1) (1,0,0) ...");

    public product() {
        super();
    }

    public product(PyType subType) {
        super(subType);
    }

    public product(PyTuple[] tuples, int repeat) {
        super();
        product___init__(tuples, repeat);
    }

    @ExposedNew
    @ExposedMethod
    final void product___init__(PyObject[] args, String[] kws) {
        final int repeat;
        final int num_iterables;
        if (kws.length == 1 && kws[0] == "repeat") {
            repeat = args[args.length -1].asInt();
            if (repeat < 0) {
                throw Py.ValueError("repeat argument cannot be negative");
            }
            num_iterables = args.length - 1;
        } else {
            repeat = 1;
            num_iterables = args.length;
        }
        final PyTuple tuples[] = new PyTuple[num_iterables];
        for (int i = 0; i < num_iterables; i++) {
            tuples[i] = PyTuple.fromIterable(args[i]);
        }
        product___init__(tuples, repeat);
    }

    private void product___init__(PyTuple[] tuples, int repeat) {
        // Make repeat duplicates, in order
        final int num_pools = tuples.length * repeat;
        final PyTuple pools[] = new PyTuple[num_pools];
        for (int r = 0; r < repeat; r++) {
            System.arraycopy(tuples, 0, pools, r * tuples.length, tuples.length);
        }
        final int indices[] = new int[num_pools];

        iter = new itertools.ItertoolsIterator() {
            boolean firstthru = true;

            @Override
            public PyObject __iternext__() {
                if (firstthru) {
                    for (PyTuple pool : pools) {
                        if (pool.__len__() == 0) {
                            return null;
                        }
                    }
                    firstthru = false;
                    return makeTuple();
                }
                for (int i = num_pools - 1; i >= 0; i--) {
                    indices[i]++;

                    if (indices[i] == pools[i].__len__()) {
                        indices[i] = 0;
                    } else {
                        return makeTuple();
                    }
                }
                return null;
            }

            private PyTuple makeTuple() {
                PyObject items[] = new PyObject[num_pools];
                for (int i = 0; i < num_pools; i++) {
                    items[i] = pools[i].__getitem__(indices[i]);
                }
                return new PyTuple(items);
            }

        };
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
