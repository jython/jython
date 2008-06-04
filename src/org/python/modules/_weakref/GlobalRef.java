/* Copyright (c) Jython Developers */
package org.python.modules._weakref;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import org.python.core.Py;
import org.python.core.PyException;
import org.python.core.PyList;
import org.python.core.PyObject;

public class GlobalRef extends WeakReference {

    int hash;

    /** Whether the hash value was calculated by the underlying object. */
    boolean realHash;

    private Vector references = new Vector();

    private static ReferenceQueue referenceQueue = new ReferenceQueue();

    private static RefReaperThread reaperThread;

    private static Map objects = new HashMap();

    static {
        initReaperThread();
    }

    public GlobalRef(PyObject object) {
        super(object, referenceQueue);
        calcHash(object);
    }

    /**
     * Calculate a hash code to use for this object.  If the PyObject we're
     * referencing implements hashCode, we use that value.  If not, we use
     * System.identityHashCode(refedObject).  This allows this object to be
     * used in a Map while allowing Python ref objects to tell if the
     * hashCode is actually valid for the object.
     */
    private void calcHash(PyObject object) {
        try {
            hash = object.hashCode();
            realHash = true;
        } catch (PyException pye) {
            if (Py.matchException(pye, Py.TypeError)) {
                hash = System.identityHashCode(object);
            } else {
                throw pye;
            }
        }
    }

    public synchronized void add(AbstractReference ref) {
        Reference r = new WeakReference(ref);
        references.addElement(r);
    }

    private final AbstractReference getReferenceAt(int idx) {
        WeakReference wref = (WeakReference)references.elementAt(idx);
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
                references.removeElementAt(i);
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
                references.removeElementAt(i);
            } else {
                r.call();
            }
        }
    }

    synchronized public int count() {
        for (int i = references.size() - 1; i >= 0; i--) {
            AbstractReference r = getReferenceAt(i);
            if (r == null) {
                references.removeElementAt(i);
            }
        }
        return references.size();
    }

    synchronized public PyList refs() {
        Vector list = new Vector();
        for (int i = references.size() - 1; i >= 0; i--) {
            AbstractReference r = getReferenceAt(i);
            if (r == null) {
                references.removeElementAt(i);
            } else {
                list.addElement(r);
            }
        }
        return new PyList(list);
    }

    public static GlobalRef newInstance(PyObject object) {
        GlobalRef ref = (GlobalRef)objects.get(new GlobalRef(object));
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
        return t.equals(u);
    }

    /**
     * Allow GlobalRef's to be used as hashtable keys.
     */
    public int hashCode() {
        return hash;
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
