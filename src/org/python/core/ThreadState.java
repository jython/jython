// Copyright (c) Corporation for National Research Initiatives
package org.python.core;

// a ThreadState refers to one PySystemState; this weak ref allows for tracking all ThreadState objects
// that refer to a given PySystemState

public class ThreadState {

    public PyFrame frame;

    public PyException exception;

    public int call_depth;

    public boolean tracing;

    public PyList reprStack;

    public int compareStateNesting;

    public TraceFunction tracefunc;

    public TraceFunction profilefunc;

    private PyDictionary compareStateDict;

    private PySystemStateRef systemStateRef;

    public ThreadState(PySystemState systemState) {
        setSystemState(systemState);
    }

    public void setSystemState(PySystemState systemState) {
        if (systemState == null) {
            systemState = Py.defaultSystemState;
        }
        if (systemStateRef == null || systemStateRef.get() != systemState) {
            systemStateRef = new PySystemStateRef(systemState, this);
        }
    }

    public PySystemState getSystemState() {
        PySystemState systemState = systemStateRef == null ? null : systemStateRef.get();
        return systemState == null ? Py.defaultSystemState : systemState;
    }

    public boolean enterRepr(PyObject obj) {
        if (reprStack == null) {
            reprStack = new PyList(new PyObject[] {obj});
            return true;
        }
        for (int i = reprStack.size() - 1; i >= 0; i--) {
            if (obj._is(reprStack.pyget(i)).__nonzero__()) {
                return false;
            }
        }
        reprStack.append(obj);
        return true;
    }

    public void exitRepr(PyObject obj) {
        if (reprStack == null) {
            return;
        }
        for (int i = reprStack.size() - 1; i >= 0; i--) {
            if (obj._is(reprStack.pyget(i)).__nonzero__()) {
                reprStack.delRange(i, reprStack.size());
            }
        }
    }

    public PyDictionary getCompareStateDict() {
        if (compareStateDict == null) {
            compareStateDict = new PyDictionary();
        }
        return compareStateDict;
    }

}
