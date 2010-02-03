
package org.python.modules.jffi;

import org.python.core.PyObject;

public interface Invoker {
    public PyObject invoke(PyObject[] args);
    public PyObject invoke();
    public PyObject invoke(PyObject arg1);
    public PyObject invoke(PyObject arg1, PyObject arg2);
    public PyObject invoke(PyObject arg1, PyObject arg2, PyObject arg3);

}
