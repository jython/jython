package org.python.core;

import org.python.expose.ExposedType;
import java.util.Iterator;

@ExposedType(name = "tupleiterator", base = PyIterator.class, isBaseType = false)
public class PyTupleIterator extends PyIterator {

    public static final PyType TYPE = PyType.fromClass(PyTupleIterator.class);
    private Iterator<PyObject> iterator;

    public PyTupleIterator(PyTuple tuple) {
        iterator = tuple.getList().iterator();
    }

    public PyObject __iternext__() {
        if (!iterator.hasNext()) {
            return null;
        } else {
            return iterator.next();
        }
    }
}
