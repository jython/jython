
package org.python.modules.jffi;

import org.python.core.PyObject;

abstract public class Invoker {
    abstract public PyObject invoke(PyObject[] args);
    abstract public PyObject invoke();
    abstract public PyObject invoke(PyObject arg1);
    abstract public PyObject invoke(PyObject arg1, PyObject arg2);
    abstract public PyObject invoke(PyObject arg1, PyObject arg2, PyObject arg3);
    abstract public PyObject invoke(PyObject arg1, PyObject arg2, PyObject arg3, PyObject arg4);
    abstract public PyObject invoke(PyObject arg1, PyObject arg2, PyObject arg3, PyObject arg4, PyObject arg5);
    abstract public PyObject invoke(PyObject arg1, PyObject arg2, PyObject arg3, PyObject arg4, PyObject arg5, PyObject arg6);
}
