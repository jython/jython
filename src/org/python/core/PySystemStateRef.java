package org.python.core;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

/**
 * A weak reference that allows to keep track of PySystemState
 * within Jython core runtime without leaking: as soon as it
 * gets garbage collected, we can clear the places where we have
 * associated data stored.
 */
public class PySystemStateRef extends WeakReference<PySystemState> {
    static final ReferenceQueue<PySystemState> referenceQueue = new ReferenceQueue<>();
    private ThreadState threadStateBackReference;

    public PySystemStateRef(PySystemState referent, ThreadState threadState) {
        super(referent, referenceQueue);
        threadStateBackReference = threadState;
    }

    public ThreadState getThreadState() {
        return threadStateBackReference;
    }
}
