/* Copyright (c) Jython Developers */
package org.python.modules.itertools;

import org.python.core.ArgParser;
import org.python.core.Py;
import org.python.core.PyIterator;
import org.python.core.PyObject;
import org.python.core.PyTuple;
import org.python.core.PyType;
import org.python.core.PyXRange;
import org.python.core.Visitproc;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedType;

@ExposedType(name = "itertools.groupby", base = PyObject.class, doc = groupby.groupby_doc)
public class groupby extends PyIterator {

    public static final PyType TYPE = PyType.fromClass(groupby.class);
    private PyIterator iter;

    public groupby() {
        super();
    }

    public groupby(PyType subType) {
        super(subType);
    }

    public groupby(PyObject iterable) {
        super();
        groupby___init__(iterable, Py.None);
    }

    public groupby(PyObject iterable, PyObject keyfunc) {
        super();
        groupby___init__(iterable, keyfunc);
    }

    public static final String groupby_doc =
        "groupby(iterable[, keyfunc]) -> create an iterator which returns\n" +
        "(key, sub-iterator) grouped by each value of key(value).";

    /**
     * Creates an iterator that returns the items of the iterable for which
     * <code>predicate(item)</code> is <code>true</code>. If <code>predicate</code> is null
     * (None) return the items that are true.
     */
    @ExposedNew
    @ExposedMethod
    final void groupby___init__(PyObject[] args, String[] kwds) {
        ArgParser ap = new ArgParser("groupby", args, kwds, "iterable", "key");
        if(args.length > 2){
            throw Py.TypeError("groupby takes two arguments, iterable and key");
        }
        PyObject iterable = ap.getPyObject(0);
        PyObject keyfunc = ap.getPyObject(1, Py.None);

        groupby___init__(iterable, keyfunc);
    }

    private void groupby___init__(final PyObject iterable, final PyObject keyfunc) {
        iter = new itertools.ItertoolsIterator() {
            PyObject currentKey;
            PyObject currentValue;
            PyObject iterator = iterable.__iter__();
            PyObject targetKey = currentKey = currentValue = new PyXRange(0);

            public PyObject __iternext__() {
                while (currentKey.equals(targetKey)) {
                    currentValue = nextElement(iterator);
                    if (currentValue == null) {
                        return null;
                    }
                    if (keyfunc == Py.None) {
                        currentKey = currentValue;
                    } else {
                        currentKey = keyfunc.__call__(currentValue);
                    }
                }
                targetKey = currentKey;
                return new PyTuple(currentKey, new GroupByIterator());
            }

            class GroupByIterator extends itertools.ItertoolsIterator {

                private boolean completed = false;

                public PyObject __iternext__() {
                    final PyObject item = currentValue;
                    if (completed) {
                        return null;
                    }
                    currentValue = nextElement(iterator);
                    if (currentValue == null) {
                        completed = true;
                    } else {
                        if (keyfunc == Py.None) {
                            currentKey = currentValue;
                        } else {
                            currentKey = keyfunc.__call__(currentValue);
                        }
                    }
                    if (!currentKey.equals(targetKey)) {
                        completed = true;
                    }
                    return item;
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
