package org.python.modules.jffi;

import org.python.core.PyObject;

/**
 * 
 */
abstract public class JITInvoker1 extends JITInvoker {
    public JITInvoker1() {
        super(1);
    }

    public final PyObject invoke() {
        return invalidArity(0);
    }

    public final PyObject invoke(PyObject arg1, PyObject arg2) {
        return invalidArity(2);
    }

    public final PyObject invoke(PyObject arg1, PyObject arg2, PyObject arg3) {
        return invalidArity(3);
    }

    public final PyObject invoke(PyObject arg1, PyObject arg2, PyObject arg3, PyObject arg4) {
        return invalidArity(4);
    }

    public final PyObject invoke(PyObject arg1, PyObject arg2, PyObject arg3, PyObject arg4, PyObject arg5) {
        return invalidArity(5);
    }

    public final PyObject invoke(PyObject arg1, PyObject arg2, PyObject arg3, PyObject arg4, PyObject arg5, PyObject arg6) {
        return invalidArity(6);
    }
}
