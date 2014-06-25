package org.python.core;

import com.google.common.collect.MapMaker;

import java.util.Map;

class ThreadStateMapping {
    private static final Map<Thread, ThreadState> cachedThreadState =
        new MapMaker().weakKeys().weakValues().makeMap();

    private static ThreadLocal<Object[]> scopedThreadState= new ThreadLocal<Object[]>() {
        @Override
        protected Object[] initialValue() {
            return new Object[1];
        }
    };

    public ThreadState getThreadState(PySystemState newSystemState) {
        Object scoped = scopedThreadState.get()[0];
        if (scoped != null) {
            return (ThreadState)scoped;
        }
        Thread currentThread = Thread.currentThread();
        ThreadState ts = cachedThreadState.get(currentThread);
        if (ts != null) {
            return ts;
        }

        if (newSystemState == null) {
            Py.writeDebug("threadstate", "no current system state");
            if (Py.defaultSystemState == null) {
                PySystemState.initialize();
            }
            newSystemState = Py.defaultSystemState;
        }

        ts = new ThreadState(newSystemState);
        cachedThreadState.put(currentThread, ts);
        return ts;
    }

    public static void enterCall(ThreadState ts) {
        if (ts.call_depth == 0) {
            scopedThreadState.get()[0] = ts;
//            Thread.currentThread().setContextClassLoader(imp.getSyspathJavaLoader());
        } else if (ts.call_depth > ts.systemState.getrecursionlimit()) {
            throw Py.RuntimeError("maximum recursion depth exceeded");
        }
        ts.call_depth++;
    }

    public static void exitCall(ThreadState ts) {
        ts.call_depth--;
        if (ts.call_depth == 0) {
            scopedThreadState.get()[0] = null;
//            Thread.currentThread().setContextClassLoader(null);
        }
    }
}
