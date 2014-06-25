// Copyright (c) Corporation for National Research Initiatives
package org.python.core;

import java.util.LinkedList;

public class ThreadState {

    public PySystemState systemState;

    public PyFrame frame;

    public PyException exception;

    public int call_depth;

    public boolean tracing;

    public PyList reprStack;

    public int compareStateNesting;

    public TraceFunction tracefunc;

    public TraceFunction profilefunc;

    private PyDictionary compareStateDict;

    public ThreadState(PySystemState systemState) {
        this.systemState = systemState;
    }

    public boolean enterRepr(PyObject obj) {
        if (reprStack == null) {
            reprStack = new PyList(new PyObject[] {obj});
            return true;
        }
        for (int i = reprStack.size() - 1; i >= 0; i--) {
            if (obj == reprStack.pyget(i)) {
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
            if (reprStack.pyget(i) == obj) {
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
