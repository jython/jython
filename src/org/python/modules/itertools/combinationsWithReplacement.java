/* Copyright (c) 2012 Jython Developers */
package org.python.modules.itertools;

import org.python.core.ArgParser;
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

@ExposedType(name = "itertools.combinations_with_replacement", base = PyObject.class)
public class combinationsWithReplacement extends PyObject {

    public static final PyType TYPE = PyType.fromClass(combinationsWithReplacement.class);
    private itertools.ItertoolsIterator iter;

    @ExposedGet
    public static PyString __doc__ = new PyString(
        "combinations_with_replacement(iterable, r) --> combinations_with_replacement object\n\n" +
            "Return successive r-length combinations of elements in the iterable\n" +
            "allowing individual elements to have successive repeats.\n" +
            "combinations_with_replacement('ABC', 2) --> AA AB AC BB BC CC");

    public combinationsWithReplacement() {
        super();
    }

    public combinationsWithReplacement(PyType subType) {
        super(subType);
    }

    public combinationsWithReplacement(PyObject iterable, int r) {
        super();
        combinationsWithReplacement___init__(iterable, r);
    }

    @ExposedNew
    @ExposedMethod
    final void combinationsWithReplacement___init__(PyObject[] args, String[] kwds) {
        if (args.length > 2) {
            throw Py.TypeError("combinations_with_replacement() takes at most 2 arguments (3 given)");
        }
        ArgParser ap = new ArgParser("combinations_with_replacement", args, kwds, "iterable", "r");
        PyObject iterable = ap.getPyObject(0);
        int r = ap.getInt(1);
        if (r < 0) {
            throw Py.ValueError("r must be non-negative");
        }
        combinationsWithReplacement___init__(iterable, r);
    }

    private void combinationsWithReplacement___init__(PyObject iterable, final int r) {
        final PyTuple pool = PyTuple.fromIterable(iterable);
        final int n = pool.__len__();
        final int indices[] = new int[r];
        for (int i = 0; i < r; i++) {
            indices[i] = 0;
        }

        iter = new itertools.ItertoolsIterator() {
            boolean firstthru = true;

            @Override
            public PyObject __iternext__() {
                if (firstthru) {
                    firstthru = false;
                    if (n == 0 && r > 0) {
                        return null;
                    }
                    return itertools.makeIndexedTuple(pool, indices);
                }
                int i;
                for (i = r - 1 ; i >= 0 && indices[i] == n - 1; i--);
                if (i < 0) return null;
                indices[i]++;
                for (int j = i + 1; j < r; j++) {
                    indices[j] = indices[j-1];
                }
                return itertools.makeIndexedTuple(pool, indices);
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
