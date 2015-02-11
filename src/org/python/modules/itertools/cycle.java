package org.python.modules.itertools;

import org.python.core.ArgParser;
import org.python.core.PyIterator;
import org.python.core.PyObject;
import org.python.core.PyType;
import org.python.core.Visitproc;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedType;

import java.util.ArrayList;
import java.util.List;

@ExposedType(name = "itertools.count", base = PyObject.class, doc = cycle.cycle_doc)
public class cycle extends PyIterator {

    public static final PyType TYPE = PyType.fromClass(cycle.class);
    private PyIterator iter;

    public static final String cycle_doc =
        "cycle(iterable) --> cycle object\n\n" +
        "Return elements from the iterable until it is exhausted.\n" +
        "Then repeat the sequence indefinitely.";

    public cycle() {
        super();
    }

    public cycle(PyType subType) {
        super(subType);
    }

    /**
     * Creates an iterator that iterates over an iterable, saving the values for each iteration.
     * When the iterable is exhausted continues to iterate over the saved values indefinitely.
     */
    public cycle(PyObject sequence) {
        super();
        cycle___init__(sequence);
    }

    @ExposedNew
    @ExposedMethod
    final void cycle___init__(final PyObject[] args, String[] kwds) {
        ArgParser ap = new ArgParser("cycle", args, kwds, new String[] {"iterable"}, 1);
        ap.noKeywords();
        cycle___init__(ap.getPyObject(0));
    }

    private void cycle___init__(final PyObject sequence) {
        iter = new itertools.ItertoolsIterator() {
            List<PyObject> saved = new ArrayList<PyObject>();
            int counter = 0;
            PyObject iterator = sequence.__iter__();

            boolean save = true;

            public PyObject __iternext__() {
                if (save) {
                    PyObject obj = nextElement(iterator);
                    if (obj != null) {
                        saved.add(obj);
                        return obj;
                    } else {
                        save = false;
                    }
                }
                if (saved.size() == 0) {
                    return null;
                }

                // pick element from saved List
                if (counter >= saved.size()) {
                    // start over again
                    counter = 0;
                }
                return saved.get(counter++);
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

