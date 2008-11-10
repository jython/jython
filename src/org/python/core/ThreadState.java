// Copyright (c) Corporation for National Research Initiatives
package org.python.core;

import java.util.LinkedList;

public class ThreadState {

    public PySystemState systemState;

    public PyFrame frame;

    public PyException exception;

    public Thread thread;

    public boolean tracing;

    public PyList reprStack;

    public int compareStateNesting;

    public int recursion_depth;

    public TraceFunction tracefunc;

    public TraceFunction profilefunc;

    private LinkedList<PyObject> initializingProxies;

    private PyDictionary compareStateDict;

    public PyObject getInitializingProxy() {
        if (initializingProxies == null) {
            return null;
        }
        return initializingProxies.peek();
    }

    public void pushInitializingProxy(PyObject proxy) {
        if (initializingProxies == null) {
            initializingProxies = new LinkedList<PyObject>();
        }
        initializingProxies.addFirst(proxy);
    }

    public void popInitializingProxy() {
        if (initializingProxies == null || initializingProxies.isEmpty()) {
            throw Py.RuntimeError("invalid initializing proxies state");
        }
        initializingProxies.removeFirst();
    }

    public ThreadState(Thread t, PySystemState systemState) {
        this.systemState = systemState;
        thread = t;
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
                reprStack.delRange(i, reprStack.size(), 1);
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
