/* Copyright (c) 2012 Jython Developers */
package org.python.modules.itertools;

import org.python.core.ArgParser;
import org.python.core.Py;
import org.python.core.PyInteger;
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

@ExposedType(name = "itertools.count", base = PyObject.class)
public class count extends PyObject {

    public static final PyType TYPE = PyType.fromClass(count.class);
    private PyIterator iter;
    private int counter;
    private int stepper;

    @ExposedGet
    public static PyString __doc__ = new PyString(
        "count(start=0, step=1) --> count object\n\n" +
        "Return a count object whose .next() method returns consecutive values.\n" +
        "  Equivalent to:\n" +
        "\n" +
        "      def count(firstval=0, step=1):\n" +
        "      x = firstval\n" +
        "      while 1:\n" +
        "          yield x\n" +
        "          x += step\n");

    public count(PyType subType) {
        super(subType);
    }

    /**
     * Creates an iterator that returns consecutive integers starting at 0.
     */
    public count() {
        super();
        count___init__(0, 1);
    }

    /**
     * Creates an iterator that returns consecutive integers starting at <code>start</code>.
     */
    public count(final int start) {
        super();
        count___init__(start, 1);
    }

    /**
     * Creates an iterator that returns consecutive integers starting at <code>start</code> with <code>step</code> step.
     */
    public count(final int start, final int step) {
        super();
        count___init__(start, step);
    }

    @ExposedNew
    @ExposedMethod
    final void count___init__(final PyObject[] args, String[] kwds) {
        ArgParser ap = new ArgParser("count", args, kwds, new String[] {"start", "step"}, 0);

        int start = ap.getInt(0, 0);
        int step = ap.getInt(1, 1);
        count___init__(start, step);
    }

    private void count___init__(final int start, final int step) {
        counter = start;
        stepper = step;

        iter = new PyIterator() {

            public PyObject __iternext__() {
                int result = counter;
                counter += stepper;
                return new PyInteger(result);
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

    @ExposedMethod
    public PyString __repr__() {
        if (stepper == 1) {
            return (PyString)(Py.newString("count(%d)").__mod__(Py.newInteger(counter)));
        }
        else {
            return (PyString)(Py.newString("count(%d, %d)").__mod__(new PyTuple(
                    Py.newInteger(counter), Py.newInteger(stepper))));
        }
    }

}
