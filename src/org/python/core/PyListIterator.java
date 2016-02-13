package org.python.core;

import org.python.expose.ExposedType;

@ExposedType(name = "listiterator", base = PyIterator.class, isBaseType = false)
public class PyListIterator extends PyIterator {

    public static final PyType TYPE = PyType.fromClass(PyListIterator.class);
    private PyList list;
    private boolean stopped = false;
    private int index = 0;

    public PyListIterator(PyList list) {
        this.list = list;
    }

    public PyObject __iternext__() {
        synchronized (list) {
            if (stopped) {
                // Need to prevent the iteration from restarting, even after a StopIteration,
                // due to the list subsequently growing.
                // Keeping track of this flag ensures that next(it) will throw StopIteration
                // exceptions on all subsequent invocations.
                return null;
            } else if (index >= list.size()) {
                stopped = true;
                return null;
            } else {
                return list.pyget(index++);
            }
        }
    }

    /* Traverseproc implementation */
    @Override
    public int traverse(Visitproc visit, Object arg) {
        int retValue = super.traverse(visit, arg);
        if (retValue != 0) {
            return retValue;
        }
        return list == null ? 0 : visit.visit(list, arg);
    }

    @Override
    public boolean refersDirectlyTo(PyObject ob) {
        return ob != null && (ob == list || super.refersDirectlyTo(ob));
    }

}
