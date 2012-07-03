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

@ExposedType(name = "itertools.repeat", base = PyObject.class)
public class repeat extends PyObject {

    public static final PyType TYPE = PyType.fromClass(repeat.class);
    private PyIterator iter;
    private PyObject object;
    private int counter;

    @ExposedGet
    public static PyString __doc__ = new PyString(
        "'repeat(element [,times]) -> create an iterator which returns the element\n"
            + "for the specified number of times.  If not specified, returns the element\nendlessly.");

    public repeat() {
        super();
    }

    public repeat(PyType subType) {
        super(subType);
    }

    public repeat(PyObject object) {
        super();
        repeat___init__(object);
    }

    public repeat(PyObject object, int times) {
        super();
        repeat___init__(object, times);
    }

    @ExposedNew
    @ExposedMethod
    final void repeat___init__(final PyObject[] args, String[] kwds) {
        ArgParser ap = new ArgParser("repeat", args, kwds, new String[] {"object", "times"}, 1);

        PyObject object = ap.getPyObject(0);
        if (args.length == 1) {
            repeat___init__(object);
        }
        else {
            int times = ap.getInt(1);
            repeat___init__(object, times);
        }
    }

    /**
     * Creates an iterator that returns the same object the number of times given by
     * <code>times</code>.
     */
    private void repeat___init__(final PyObject object, final int times) {
        this.object = object;
        if (times < 0) {
            counter = 0;
        }
        else {
            counter = times;
        }
        iter = new PyIterator() {

            public PyObject __iternext__() {
                if (counter > 0) {
                    counter--;
                    return object;
                }
                return null;
            }

        };
    }

    /**
     * Creates an iterator that returns the same object over and over again.
     */
    private void repeat___init__(final PyObject object) {
        this.object = object;
        counter = -1;
        iter = new PyIterator() {
            public PyObject __iternext__() {
                return object;
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
    public int __len__() {
        if (counter < 0) {
            throw Py.TypeError("object of type 'itertools.repeat' has no len()");
        }
        return counter;
    }

    @ExposedMethod
    public PyString __repr__() {
        if (counter >= 0) {
            return (PyString)(Py.newString("repeat(%r, %d)").
                    __mod__(new PyTuple(object, Py.newInteger(counter))));
        }
        else {
            return (PyString)(Py.newString("repeat(%r)").
                    __mod__(new PyTuple(object)));
        }
    }
}
