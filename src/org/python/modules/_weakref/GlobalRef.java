/* Copyright (c) Jython Developers */
package org.python.modules._weakref;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.python.core.Py;
import org.python.core.PyList;
import org.python.core.PyObject;
import org.python.core.PySystemState;
import org.python.util.Generic;

public class GlobalRef extends WeakReference {

    /**
     * This reference's hashCode: the System.identityHashCode of the referent. Only used
     * internally.
     */
    private int hashCode;

    /**
     * The public hashCode for the Python AbstractReference wrapper. Derived from the
     * referent's hashCode.
     */
    private int pythonHashCode;

    /** Whether pythonHashCode was already determined. */
    private boolean havePythonHashCode;

    private List references = new ArrayList();

    private static ReferenceQueue referenceQueue = new ReferenceQueue();

    private static Thread reaperThread;
    private static ReentrantReadWriteLock reaperLock = new ReentrantReadWriteLock();

    private static ConcurrentMap<GlobalRef, GlobalRef> objects = Generic.concurrentMap();

    public GlobalRef(PyObject object) {
        super(object, referenceQueue);
        hashCode = System.identityHashCode(object);
    }

    public synchronized void add(AbstractReference ref) {
        Reference r = new WeakReference(ref);
        references.add(r);
    }

    private final AbstractReference getReferenceAt(int idx) {
        WeakReference wref = (WeakReference)references.get(idx);
        return (AbstractReference)wref.get();
    }

    /**
     * Search for a reusable refrence. To be reused, it must be of the
     * same class and it must not have a callback.
     */
    synchronized AbstractReference find(Class cls) {
        for (int i = references.size() - 1; i >= 0; i--) {
            AbstractReference r = getReferenceAt(i);
            if (r == null) {
                references.remove(i);
            } else if (r.callback == null && r.getClass() == cls) {
                return r;
            }
        }
        return null;
    }

    /**
     * Call each of the registered references.
     */
    synchronized void call() {
        for (int i = references.size() - 1; i >= 0; i--) {
            AbstractReference r = getReferenceAt(i);
            if (r == null) {
                references.remove(i);
            } else {
                r.call();
            }
        }
    }

    synchronized public int count() {
        for (int i = references.size() - 1; i >= 0; i--) {
            AbstractReference r = getReferenceAt(i);
            if (r == null) {
                references.remove(i);
            }
        }
        return references.size();
    }

    synchronized public PyList refs() {
        List list = new ArrayList();
        for (int i = references.size() - 1; i >= 0; i--) {
            AbstractReference r = getReferenceAt(i);
            if (r == null) {
                references.remove(i);
            } else {
                list.add(r);
            }
        }
        return new PyList(list);
    }

    /**
     * Create a new tracked GlobalRef.
     *
     * @param object a PyObject to reference
     * @return a new tracked GlobalRef
     */
    public static GlobalRef newInstance(PyObject object) {
        createReaperThreadIfAbsent();

        GlobalRef newRef = new GlobalRef(object);
        GlobalRef ref = objects.putIfAbsent(newRef, newRef);
        if (ref == null) {
            ref = newRef;
        }
        return ref;
    }

    private static void createReaperThreadIfAbsent() {
        reaperLock.readLock().lock();
        try {
            if (reaperThread == null || !reaperThread.isAlive()) {
                reaperLock.readLock().unlock();
                reaperLock.writeLock().lock();
                if (reaperThread == null || !reaperThread.isAlive()) {
                    try {
                        initReaperThread();
                    } finally {
                        reaperLock.readLock().lock();
                        reaperLock.writeLock().unlock();
                    }
                }
            }
        } finally {
            reaperLock.readLock().unlock();
        }
    }

    /**
     * Return the number of references to the specified PyObject.
     *
     * @param object a PyObject
     * @return an int reference count
     */
    public static int getCount(PyObject object) {
        GlobalRef ref = objects.get(new GlobalRef(object));
        return ref == null ? 0 : ref.count();
    }

    /**
     * Return a list of references to the specified PyObject.
     *
     * @param object a PyObject
     * @return a PyList of references. may be empty
     */
    public static PyList getRefs(PyObject object) {
        GlobalRef ref = objects.get(new GlobalRef(object));
        return ref == null ? new PyList() : ref.refs();
    }

    /**
     * Allow GlobalRef's to be used as hashtable keys.
     */
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof GlobalRef)) {
            return false;
        }
        Object t = this.get();
        Object u = ((GlobalRef)o).get();
        if ((t == null) || (u == null)) {
            return false;
        }
        if (t == u) {
            return true;
        }
        // Don't consult the objects' equals (__eq__) method, it can't be trusted
        return false;
    }

    /**
     * Allows GlobalRef to be used as hashtable keys.
     *
     * @return a hashCode int value
     */
    public int hashCode() {
        return hashCode;
    }

    /**
     * The publicly used hashCode, for the AbstractReference wrapper.
     *
     * @return a hashCode int value
     */
    public int pythonHashCode() {
        if (havePythonHashCode) {
            return pythonHashCode;
        }
        Object referent = get();
        if (referent == null) {
            throw Py.TypeError("weak object has gone away");
        }
        pythonHashCode = referent.hashCode();
        havePythonHashCode = true;
        return pythonHashCode;
    }

    private static void initReaperThread() {
        RefReaper reaper = new RefReaper();
        PySystemState systemState = Py.getSystemState();
        systemState.registerCloser(reaper);

        reaperThread = new Thread(reaper, "weakref reaper");
        reaperThread.setDaemon(true);
        reaperThread.start();
    }

    private static class RefReaper implements Runnable, Callable<Void> {
        private volatile boolean exit = false;
        private Thread thread;

        public void collect() throws InterruptedException {
            GlobalRef gr = (GlobalRef)referenceQueue.remove();
            gr.call();
            objects.remove(gr);
            gr = null;
        }

        public void run() {
            // Store the actual reaper thread so that when PySystemState.cleanup()
            // is called this thread can be interrupted and die.
            this.thread = Thread.currentThread();

            while (true) {
                try {
                    collect();
                } catch (InterruptedException exc) {
                    // Is cleanup time so break out and die.
                    if (exit) {
                        break;
                    }
                }
            }
        }

        @Override
        public Void call() throws Exception {
            this.exit = true;

            if (thread != null && thread.isAlive()) {
                this.thread.interrupt();
                this.thread = null;
            }

            return null;
        }
    }
}
