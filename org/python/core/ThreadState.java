// Copyright (c) Corporation for National Research Initiatives
package org.python.core;

import java.util.Stack;

public class ThreadState {
    // public InterpreterState interp;
    public PySystemState systemState;

    public PyFrame frame;

    // public PyObject curexc_type, curexc_value, curexc_traceback;
    // public PyObject exc_type, exc_value, exc_traceback;
    public PyException exception;

    public Thread thread;

    public boolean tracing;

    public PyList reprStack = null;

    // public PyInstance initializingProxy = null;
    private Stack initializingProxies = null;

    public int compareStateNesting = 0;

    private PyDictionary compareStateDict;

    public int recursion_depth = 0;

    public PyInstance getInitializingProxy() {
        if (this.initializingProxies == null
                || this.initializingProxies.empty()) {
            return null;
        }
        return (PyInstance) this.initializingProxies.peek();
    }

    public void pushInitializingProxy(PyInstance proxy) {
        if (this.initializingProxies == null) {
            this.initializingProxies = new Stack();
        }
        this.initializingProxies.push(proxy);
    }

    public void popInitializingProxy() {
        if (this.initializingProxies == null
                || this.initializingProxies.empty()) {
            throw Py.RuntimeError("invalid initializing proxies state");
        }
        this.initializingProxies.pop();
    }

    public ThreadState(Thread t, PySystemState systemState) {
        this.thread = t;
        // Fake multiple interpreter states for now
        /*
         * if (Py.interp == null) { Py.interp =
         * InterpreterState.getInterpreterState(); }
         */
        this.systemState = systemState;
        // interp = Py.interp;
        this.tracing = false;
        // System.out.println("new thread state");
    }

    public boolean enterRepr(PyObject obj) {
        // if (reprStack == null) System.err.println("reprStack: null");
        // else System.err.println("reprStack: "+reprStack.__len__());
        if (this.reprStack == null) {
            this.reprStack = new PyList(new PyObject[] { obj });
            return true;
        }
        for (int i = this.reprStack.size() - 1; i >= 0; i--) {
            if (obj == this.reprStack.pyget(i)) {
                return false;
            }
        }
        this.reprStack.append(obj);
        return true;
    }

    public void exitRepr(PyObject obj) {
        if (this.reprStack == null) {
            return;
        }

        for (int i = this.reprStack.size() - 1; i >= 0; i--) {
            if (this.reprStack.pyget(i) == obj) {
                this.reprStack.delRange(i, this.reprStack.size(), 1);
            }
        }
    }

    public PyDictionary getCompareStateDict() {
        if (this.compareStateDict == null) {
            this.compareStateDict = new PyDictionary();
        }
        return this.compareStateDict;
    }
}
