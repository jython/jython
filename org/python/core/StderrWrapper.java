package org.python.core;

public class StderrWrapper extends StdoutWrapper {
    public StderrWrapper() {
        name = "stderr";
    }

    protected PyObject getObject(PySystemState ss) {
        return ss.stderr;
    }
    protected void setObject(PySystemState ss, PyObject obj) {
        ss.stderr = obj;
    }
}
