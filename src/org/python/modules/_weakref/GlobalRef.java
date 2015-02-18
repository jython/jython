/* Copyright (c) Jython Developers */
package org.python.modules._weakref;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.python.core.JyAttribute;
import org.python.core.Py;
import org.python.core.PyList;
import org.python.core.PyObject;
import org.python.core.PySystemState;
import org.python.util.Generic;
import org.python.modules.gc;

public class GlobalRef extends WeakReference<PyObject> {

    /**
     * This reference's hashCode: The {@code System.identityHashCode} of the referent.
     * Only used internally.
     */
    private int hashCode;

    /**
     * The public hashCode for the Python AbstractReference wrapper. Derived from the
     * referent's hashCode.
     */
    private int pythonHashCode;

    /** Whether {@link #pythonHashCode} was already determined. */
    private boolean havePythonHashCode;

    /**
     * This boolean is set {@code true} when the callback is processed.
     * If the reference is cleared it might potentially be restored until
     * this boolean is set true. If weak reference restoring is activated (c.f.
     * {@link gc#PRESERVE_WEAKREFS_ON_RESURRECTION}), {@link AbstractReference#get()}
     * would block until a consistent state is reached (i.e. referent is
     * non-{@code null} or {@code cleared == true}).
     *
     * @see gc#PRESERVE_WEAKREFS_ON_RESURRECTION
     * @see AbstractReference#get()
     */
    protected boolean cleared = false;

    private List<WeakReference<AbstractReference>> references = new ArrayList<>();

    private static ReferenceQueue<PyObject> referenceQueue = new ReferenceQueue<>();

    private static Thread reaperThread;
    private static ReentrantReadWriteLock reaperLock = new ReentrantReadWriteLock();

    private static ConcurrentMap<GlobalRef, GlobalRef> objects = Generic.concurrentMap();
    private static List<GlobalRef> delayedCallbacks;

    public GlobalRef(PyObject object) {
        super(object, referenceQueue);
        hashCode = System.identityHashCode(object);
    }

    public synchronized void add(AbstractReference ref) {
        WeakReference<AbstractReference> r = new WeakReference<>(ref);
        references.add(r);
    }

    private final AbstractReference getReferenceAt(int idx) {
        WeakReference<AbstractReference> wref = references.get(idx);
        return wref.get();
    }

    /**
     * Search for a reusable reference. To be reused, it must be of the
     * same class and it must not have a callback.
     */
    synchronized AbstractReference find(Class<?> cls) {
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
        if (!cleared) {
            cleared = true;
            for (int i = references.size() - 1; i >= 0; i--) {
                AbstractReference r = getReferenceAt(i);
                if (r == null) {
                    references.remove(i);
                } else {
                    Thread pendingGet = (Thread) JyAttribute.getAttr(
                            r, JyAttribute.WEAKREF_PENDING_GET_ATTR);
                    if (pendingGet != null) {
                        pendingGet.interrupt();
                    }
                    r.call();
                }
            }
        }
    }

    /**
     * Call all callbacks that were enqueued via
     * {@link #delayedCallback(GlobalRef)} method.
     *
     * @see #delayedCallback(GlobalRef)
     */
    public static void processDelayedCallbacks() {
        if (delayedCallbacks != null) {
            synchronized (delayedCallbacks) {
                for (GlobalRef gref: delayedCallbacks) {
                    gref.call();
                }
                delayedCallbacks.clear();
            }
        }
    }

    /**
     * Stores the callback for later processing. This is needed if
     * weak reference restoration (c.f.
     * {@link gc#PRESERVE_WEAKREFS_ON_RESURRECTION})
     * is active. In this case the callback is delayed until it was
     * determined whether a resurrection restored the reference.
     *
     * @see gc#PRESERVE_WEAKREFS_ON_RESURRECTION
     */
    private static void delayedCallback(GlobalRef cl) {
        if (delayedCallbacks == null) {
            delayedCallbacks = new ArrayList<>();
        }
        synchronized (delayedCallbacks) {
            delayedCallbacks.add(cl);
        }
    }

    public static boolean hasDelayedCallbacks() {
        return delayedCallbacks != null && !delayedCallbacks.isEmpty();
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
        List<AbstractReference> list = new ArrayList<>();
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
     * Create a new tracked {@code GlobalRef}.
     *
     * @param object a {@link org.python.core.PyObject} to reference
     * @return a new tracked {@code GlobalRef}
     */
    public static GlobalRef newInstance(PyObject object) {
        createReaperThreadIfAbsent();

        GlobalRef newRef = new GlobalRef(object);
        GlobalRef ref = objects.putIfAbsent(newRef, newRef);
        if (ref == null) {
            ref = newRef;
            JyAttribute.setAttr(object, JyAttribute.WEAK_REF_ATTR, ref);
        } else {
            // We clear the not-needed Global ref so that it won't
            // pop up in ref-reaper thread's activity.
            newRef.clear();
            newRef.cleared = true;
        }
        return ref;
    }

    /**
     * Restores this weak reference to its former referent.
     * This actually means that a fresh {@code GlobalRef} is created
     * and inserted into all adjacent
     * {@link org.python.modules._weakref.AbstractReference}s. The
     * current {@code GlobalRef} is disbanded.
     * If the given {@link org.python.core.PyObject} is not the former
     * referent of this weak reference, an
     * {@link java.lang.IllegalArgumentException} is thrown.
     *
     * @throws java.lang.IllegalArgumentException if {@code formerReferent} is not
     * the actual former referent.
     */
    public void restore(PyObject formerReferent) {
        if (JyAttribute.getAttr(formerReferent, JyAttribute.WEAK_REF_ATTR) != this) {
            throw new IllegalArgumentException(
                    "Argument is not former referent of this GlobalRef.");
        }
        if (delayedCallbacks != null) {
            synchronized (delayedCallbacks) {
                delayedCallbacks.remove(this);
            }
        }
        clear();
        createReaperThreadIfAbsent();
        GlobalRef restore = new GlobalRef(formerReferent);
        restore.references = references;
        objects.remove(this);
        objects.put(restore, restore);
        AbstractReference aref;
        for (int i = references.size() - 1; i >= 0; i--) {
            aref = getReferenceAt(i);
            if (aref == null) {
                references.remove(i);
            } else {
                aref.gref = restore;
                Thread pendingGet = (Thread) JyAttribute.getAttr(
                        aref, JyAttribute.WEAKREF_PENDING_GET_ATTR);
                if (pendingGet != null) {
                    pendingGet.interrupt();
                }
            }
        }
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
     * Return the number of references to the specified
     * {@link org.python.core.PyObject}.
     *
     * @param object a PyObject
     * @return an int reference count
     */
    public static int getCount(PyObject object) {
        GlobalRef ref = objects.get(new GlobalRef(object));
        return ref == null ? 0 : ref.count();
    }

    /**
     * Return a list of references to the specified
     * {@link org.python.core.PyObject}.
     *
     * @param object a {@link org.python.core.PyObject}
     * @return a {@link org.python.core.PyList} of references. May be empty.
     */
    public static PyList getRefs(PyObject object) {
        GlobalRef ref = objects.get(new GlobalRef(object));
        return ref == null ? new PyList() : ref.refs();
    }

    /**
     * Allow {@code GlobalRef}s to be used as hashtable-keys.
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
     * Allows {@code GlobalRef} to be used as hashtable-keys.
     *
     * @return a hashCode {@code int}-value
     */
    public int hashCode() {
        return hashCode;
    }

    /**
     * The publicly used {@code hashCode}, for the
     * {@link org.python.modules._weakref.AbstractReference}
     * wrapper.
     *
     * @return a hashCode {@code int}-value
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
            GlobalRef gr = (GlobalRef) referenceQueue.remove();
            if ((gc.getJythonGCFlags() & gc.PRESERVE_WEAKREFS_ON_RESURRECTION) == 0) {
                gr.call();
            } else {
                delayedCallback(gr);
            }
            objects.remove(gr);
            gr = null;
        }

        @Override
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
