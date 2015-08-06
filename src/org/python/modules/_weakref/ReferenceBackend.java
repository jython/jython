package org.python.modules._weakref;

import org.python.core.PyObject;
import org.python.core.PyList;

public interface ReferenceBackend {
    public PyObject get();
    public void add(AbstractReference ref);
    public boolean isCleared();
    public AbstractReference find(Class<?> cls);
    public int pythonHashCode();
    public PyList refs();
    public void restore(PyObject formerReferent);
    public int count();
}
