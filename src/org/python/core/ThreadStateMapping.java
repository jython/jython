package org.python.core;

class ThreadStateMapping {
    private static final ThreadLocal<ThreadState[]> cachedThreadState =
            new ThreadLocal<ThreadState[]>() {
                @Override
                protected ThreadState[] initialValue() {
                    return new ThreadState[1];
                }
            };

    public synchronized ThreadState getThreadState(PySystemState newSystemState) {
        ThreadState[] threadLocal = cachedThreadState.get();
        if (threadLocal[0] != null)
            return threadLocal[0];

        Thread t = Thread.currentThread();
        if (newSystemState == null) {
            Py.writeDebug("threadstate", "no current system state");
            if (Py.defaultSystemState == null) {
                PySystemState.initialize();
            }
            newSystemState = Py.defaultSystemState;
        }

        ThreadState ts = new ThreadState(t, newSystemState);
        newSystemState.registerThreadState(threadLocal, ts);
        return ts;
    }
}
