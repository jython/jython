package org.python.core;

import com.google.common.collect.MapMaker;

import java.util.Map;

class ThreadStateMapping {
    private static final Map<Thread, ThreadState> cachedThreadState =
        new MapMaker().weakKeys().weakValues().makeMap();

    public ThreadState getThreadState(PySystemState newSystemState) {
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

        ts = new ThreadState(currentThread, newSystemState);
        cachedThreadState.put(currentThread, ts);
        return ts;
    }
}
