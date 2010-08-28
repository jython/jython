package org.python.core;

class ThreadStateMapping {
    private static final ThreadLocal<ThreadState[]> cachedThreadState =
            new ThreadLocal<ThreadState[]>() {
                @Override
                protected ThreadState[] initialValue() {
                    return new ThreadState[1];
                }
            };

    public ThreadState getThreadState(PySystemState newSystemState) {

        // usual double checked locking pattern
        ThreadState ts = cachedThreadState.get()[0];

        if (ts != null) {
            return ts;
        }

        synchronized(this) {
            ThreadState[] threadLocal = cachedThreadState.get();
            if(threadLocal[0] != null)
                return (ThreadState)threadLocal[0];

            Thread t = Thread.currentThread();
            if (newSystemState == null) {
                Py.writeDebug("threadstate", "no current system state");
                if (Py.defaultSystemState == null) {
                    PySystemState.initialize();
                }
                newSystemState = Py.defaultSystemState;
            }

            ts = new ThreadState(t, newSystemState);

            newSystemState.registerThreadState(threadLocal, ts);
            return ts;
        }
    }
}
