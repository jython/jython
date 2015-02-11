package org.python.modules.itertools;

import org.python.core.ArgParser;
import org.python.core.Py;
import org.python.core.PyInteger;
import org.python.core.PyIterator;
import org.python.core.PyNone;
import org.python.core.PyObject;
import org.python.core.PyType;
import org.python.core.Visitproc;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedType;

@ExposedType(name = "itertools.islice", base = PyObject.class, doc = islice.islice_doc)
public class islice extends PyIterator {

    public static final PyType TYPE = PyType.fromClass(islice.class);
    private itertools.ItertoolsIterator iter;

    public static final String islice_doc =
        "islice(iterable, [start,] stop [, step]) --> islice object\n\n" +
        "Return an iterator whose next() method returns selected values from an\n" +
        "iterable.  If start is specified, will skip all preceding elements;\n" +
        "otherwise, start defaults to zero.  Step defaults to one.  If\n" +
        "specified as another value, step determines how many values are \n" +
        "skipped between successive calls.  Works like a slice() on a list\n" +
        "but returns an iterator.";

    public islice() {
        super();
    }

    public islice(PyType subType) {
        super(subType);
    }

    /**
     * @see #islice___init__(PyObject, PyObject, PyObject, PyObject) startObj defaults to 0 and stepObj to 1
     */
    public islice(PyObject iterable, PyObject stopObj) {
        super();
        islice___init__(iterable, new PyInteger(0), stopObj, new PyInteger(1));
    }

    /**
     * @see #islice___init__(PyObject, PyObject, PyObject, PyObject) stepObj defaults to 1
     */
    public islice(PyObject iterable, PyObject start,
                                    PyObject stopObj) {
        super();
        islice___init__(iterable, start, stopObj, new PyInteger(1));
    }

    @ExposedNew
    @ExposedMethod
    final void islice___init__(PyObject[] args, String[] kwds) {
        ArgParser ap = new ArgParser("islice", args, kwds, new String[] {
                "iterable", "start", "stop", "step"}, 2);

        PyObject iterable = ap.getPyObject(0);
        if (args.length == 2) {
            PyObject stopObj = ap.getPyObject(1);
            islice___init__(iterable, new PyInteger(0), stopObj, new PyInteger(1));
        }
        else {
            PyObject startObj = ap.getPyObject(1);
            PyObject stopObj = ap.getPyObject(2);
            if (args.length == 3) {
                islice___init__(iterable, startObj, stopObj, new PyInteger(1));
            }
            else {
                PyObject stepObj = ap.getPyObject(3);
                islice___init__(iterable, startObj, stopObj, stepObj);
            }
        }
    }

    /**
     * Creates an iterator that returns selected values from an iterable.
     *
     * @param startObj
     *            the index of where in the iterable to start returning values
     * @param stopObj
     *            the index of where in the iterable to stop returning values
     * @param stepObj
     *            the number of steps to take between each call to <code>next()</code>
     */
    private void islice___init__(final PyObject iterable, PyObject startObj,
                                       PyObject stopObj, PyObject stepObj) {
        final int start = itertools.py2int(startObj, 0, "Start argument must be a non-negative integer or None");
        final int step = itertools.py2int(stepObj, 1, "Step argument must be a non-negative integer or None");
        final int stopArg = itertools.py2int(stopObj, 0, "Stop argument must be a non-negative integer or None");
        final int stop = stopObj instanceof PyNone ? Integer.MAX_VALUE : stopArg;

        if (start < 0 || step < 0 || stop < 0) {
            throw Py.ValueError("Indices for islice() must be non-negative integers");
        }

        if (step == 0) {
            throw Py.ValueError("Step must be one or larger for islice()");
        }

        iter = new itertools.ItertoolsIterator() {
            int counter = start;

            int lastCount = 0;

            PyObject iter = iterable.__iter__();

            public PyObject __iternext__() {
                PyObject result = null;

                // ensure we never move the underlying iterator past 'stop'
                while (lastCount < Math.min(counter + 1, stop)) {
                    result = nextElement(iter);
                    lastCount++;
                }

                if (lastCount - 1 == counter) {
                    counter += step;
                    return result;
                }

                return null;
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
