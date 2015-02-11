/* Copyright (c) 2012 Jython Developers */
package org.python.modules.itertools;

import org.python.core.ArgParser;
import org.python.core.Py;
import org.python.core.PyException;
import org.python.core.PyInteger;
import org.python.core.PyIterator;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.core.PyTuple;
import org.python.core.PyType;
import org.python.core.__builtin__;
import org.python.core.Visitproc;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedType;

@ExposedType(name = "itertools.count", base = PyObject.class, doc = count.count_doc)
public class count extends PyIterator {

    public static final PyType TYPE = PyType.fromClass(count.class);
    private PyIterator iter;
    private PyObject counter;
    private PyObject stepper;

    private static PyObject NumberClass;
    private static synchronized PyObject getNumberClass() {
        if (NumberClass == null) {
            NumberClass = __builtin__.__import__("numbers").__getattr__("Number");
        }
        return NumberClass;
    }

    public static final String count_doc =
        "count(start=0, step=1) --> count object\n\n" +
        "Return a count object whose .next() method returns consecutive values.\n" +
        "  Equivalent to:\n" +
        "\n" +
        "      def count(firstval=0, step=1):\n" +
        "      x = firstval\n" +
        "      while 1:\n" +
        "          yield x\n" +
        "          x += step\n";

    public count(PyType subType) {
        super(subType);
    }

    /**
     * Creates an iterator that returns consecutive numbers starting at 0.
     */
    public count() {
        super();
        count___init__(Py.Zero, Py.One);
    }

    /**
     * Creates an iterator that returns consecutive numbers starting at <code>start</code>.
     */
    public count(final PyObject start) {
        super();
        count___init__(start, Py.One);
    }

    /**
     * Creates an iterator that returns consecutive numbers starting at <code>start</code> with <code>step</code> step.
     */
    public count(final PyObject start, final PyObject step) {
        super();
        count___init__(start, step);
    }

    // TODO: move into Py, although NumberClass import time resolution becomes
    // TODO: a bit trickier
    private static PyObject getNumber(PyObject obj) {
        if (Py.isInstance(obj, getNumberClass())) {
            return obj;
        }
        try {
            PyObject intObj = obj.__int__();
            if (Py.isInstance(obj, getNumberClass())) {
                return intObj;
            }
            throw Py.TypeError("a number is required");
        } catch (PyException exc) {
            if (exc.match(Py.ValueError)) {
                throw Py.TypeError("a number is required");
            }
            throw exc;
        }
    }

    @ExposedNew
    @ExposedMethod
    final void count___init__(final PyObject[] args, String[] kwds) {
        ArgParser ap = new ArgParser("count", args, kwds, new String[] {"start", "step"}, 0);
        PyObject start = getNumber(ap.getPyObject(0, Py.Zero));
        PyObject step = getNumber(ap.getPyObject(1, Py.One));
        count___init__(start, step);
    }

    private void count___init__(final PyObject start, final PyObject step) {
        counter = start;
        stepper = step;

        iter = new PyIterator() {

            public PyObject __iternext__() {
                PyObject result = counter;
                counter = counter._add(stepper);
                return result;
            }

        };
    }

    @ExposedMethod
    public PyObject count___copy__() {
        return new count(counter, stepper);
    }

    @ExposedMethod
    final PyObject count___reduce_ex__(PyObject protocol) {
        return __reduce_ex__(protocol);
    }

    @ExposedMethod
    final PyObject count___reduce__() {
        return __reduce_ex__(Py.Zero);
    }


    public PyObject __reduce_ex__(PyObject protocol) {
        if (stepper == Py.One) {
            return new PyTuple(getType(), new PyTuple(counter));
        } else {
            return new PyTuple(getType(), new PyTuple(counter, stepper));
        }
    }

    @ExposedMethod
    public PyString __repr__() {
        if (stepper instanceof PyInteger && stepper._cmp(Py.One) == 0) {
            return Py.newString(String.format("count(%s)", counter));
        }
        else {
            return Py.newString(String.format("count(%s, %s)", counter, stepper));
        }
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
        int retVal;
        if (iter != null) {
            retVal = visit.visit(iter, arg);
            if (retVal != 0) {
                return retVal;
            }
        }
        if (counter != null) {
            retVal = visit.visit(counter, arg);
            if (retVal != 0) {
                return retVal;
            }
        }
        if (stepper != null) {
            retVal = visit.visit(stepper, arg);
            if (retVal != 0) {
                return retVal;
            }
        }
        return super.traverse(visit, arg);
    }

    @Override
    public boolean refersDirectlyTo(PyObject ob) {
        return ob != null && (iter == ob || counter == ob
            || stepper == ob || super.refersDirectlyTo(ob));
    }
}
