/* Copyright (c) Jython Developers */
package org.python.modules._weakref;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

import org.python.core.Py;
import org.python.core.PyList;
import org.python.core.PyObject;
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

    private static RefReaperThread reaperThread;

    private static ConcurrentMap<GlobalRef, GlobalRef> objects = Generic.concurrentMap();

    static {
        initReaperThread();
    }

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

    public static GlobalRef newInstance(PyObject object) {
        GlobalRef ref = objects.get(new GlobalRef(object));
        if (ref == null) {
            ref = new GlobalRef(object);
            objects.put(ref, ref);
        }
        return ref;
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
        reaperThread = new RefReaperThread();
        reaperThread.setDaemon(true);
        reaperThread.start();
    }

    private static class RefReaperThread extends Thread {

        RefReaperThread() {
            super("weakref reaper");
        }

        public void collect() throws InterruptedException {
            GlobalRef gr = (GlobalRef)referenceQueue.remove();
            gr.call();
            objects.remove(gr);
            gr = null;
        }

        public void run() {
            while (true) {
                try {
                    collect();
                } catch (InterruptedException exc) {
                    // ok
                }
            }
        }
    }
}
