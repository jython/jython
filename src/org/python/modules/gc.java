package org.python.modules;

import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import org.python.core.JyAttribute;
import org.python.core.Py;
import org.python.core.PyList;
import org.python.core.PyObject;
import org.python.core.PyInstance;
import org.python.core.Traverseproc;
import org.python.core.TraverseprocDerived;
import org.python.core.Visitproc;
import org.python.core.Untraversable;
import org.python.core.finalization.FinalizeTrigger;
import org.python.modules._weakref.GlobalRef;

public class gc {
    /**
     * A constant that can occur as result of {@code gc.collect} and
     * indicates an unknown number of collected cyclic trash.
     * It is intentionally not valued -1 as that value is
     * reserved to indicate an error.
     */
    public static final int UNKNOWN_COUNT = -2;
    
    /* Jython-specific gc-flags: */
    /**
     * Tells every newly created PyObject to register for
     * gc-monitoring. This allows {@code gc.collect} to report the
     * number of collected objects.
     */
    public static final short MONITOR_GLOBAL =                    (1<<0);

    /**
     * CPython prior to 3.4 does not finalize cyclic garbage
     * PyObjects, while Jython does this by default. This flag
     * tells Jython's gc to mimic CPython <3.4 behavior (i.e.
     * add such objects to {@code gc.garbage} list instead).
     */
    public static final short DONT_FINALIZE_CYCLIC_GARBAGE =      (1<<1);

    /**
     * If a PyObject is resurrected during its finalization
     * process and was weakly referenced, Jython breaks the
     * weak references to the resurrected PyObject by default.
     * In CPython these persist, if the object was indirectly
     * resurrected due to resurrection of its owner.
     * This flag tells Jython's gc to preserve weak references
     * to such resurrected PyObjects.
     * It only works if all involved objects implement the
     * traverseproc mechanism properly (see
     * {@link org.python.core.Traverseproc}).
     * Note that this feature comes with some cost as it can
     * delay garbage collection of some weak referenced objects
     * for several gc cycles if activated. So we recommend to
     * use it only for debugging.
     */
    public static final short PRESERVE_WEAKREFS_ON_RESURRECTION = (1<<2);

    /**
     * If in CPython an object is resurrected via its finalizer
     * and contained strong references to other objects, these
     * are also resurrected and not finalized in CPython (as
     * their reference count never drops to zero). In contrast
     * to that, Jython calls finalizers for all objects that
     * were unreachable when gc started (regardless of resurrections
     * and in unpredictable order). This flag emulates CPython
     * behavior in Jython. Note that this emulation comes with a
     * significant cost as it can delay collection of many objects
     * for several gc-cycles. Its main intention is for debugging
     * resurrection-sensitive code.
     */
    public static final short DONT_FINALIZE_RESURRECTED_OBJECTS = (1<<3);

    /**
     * Reflection-based traversion is currently an experimental feature and
     * is deactivated by default for now. This means that
     * {@code DONT_TRAVERSE_BY_REFLECTION} is set by default.
     * Once it is stable, reflection-based traversion will be active by default.
     */
    public static final short DONT_TRAVERSE_BY_REFLECTION =       (1<<4);

    /**
     * <p>
     * If this flag is not set, gc warns whenever an object would be subject to
     * reflection-based traversion.
     * Note that if this flag is not set, the warning will occur even if
     * reflection-based traversion is not active. The purpose of this behavior is
     * to identify objects that don't properly support the traverseproc-mechanism,
     * i.e. instances of PyObject-subclasses that neither implement
     * {@link org.python.core.Traverseproc},
     * nor are annotated with the {@link org.python.core.Untraversable}-annotation.
     * </p>
     * <p>
     * A SUPPRESS-flag was chosen rather than a WARN-flag, so that warning is the
     * default behavior - the user must actively set this flag in order to not to
     * be warned.
     * This is because in an ideal implementation reflection-based traversion never
     * occurs; it is only an inefficient fallback.
     * </p>
     */
    public static final short SUPPRESS_TRAVERSE_BY_REFLECTION_WARNING =    (1<<5);

    /**
     * In Jython one usually uses {@code Py.writeDebug} for debugging output.
     * However that method is only verbose if an appropriate verbose-level
     * was set. In CPython it is enough to set gc-{@code DEBUG} flags to get
     * gc-messages, no matter what overall verbose level is selected.
     * This flag tells Jython to use {@code Py.writeDebug} for debugging output.
     * If it is not set (default-case), gc-debugging output (if gc-{@code VERBOSE}
     * or -{@code DEBUG} flags are set) is directly written to {@code System.err}.  
     */
    public static final short USE_PY_WRITE_DEBUG = (1<<6);

    public static final short VERBOSE_COLLECT =  (1<<7);
    public static final short VERBOSE_WEAKREF =  (1<<8);
    public static final short VERBOSE_DELAYED =  (1<<9);
    public static final short VERBOSE_FINALIZE = (1<<10);
    public static final short VERBOSE =
            VERBOSE_COLLECT | VERBOSE_WEAKREF | VERBOSE_DELAYED | VERBOSE_FINALIZE;

    /* set for debugging information */
    /**
     * print collection statistics
     * (in Jython scoped on monitored objects)
     */
    public static final int DEBUG_STATS         = (1<<0);

    /**
     * print collectable objects
     * (in Jython scoped on monitored objects)
     */
    public static final int DEBUG_COLLECTABLE   = (1<<1);

    /**
     * print uncollectable objects
     * (in Jython scoped on monitored objects)
     */
    public static final int DEBUG_UNCOLLECTABLE = (1<<2);

    /**
     * print instances
     * (in Jython scoped on monitored objects)
     */
    public static final int DEBUG_INSTANCES     = (1<<3);

    /**
     * print other objects
     * (in Jython scoped on monitored objects)
     */
    public static final int DEBUG_OBJECTS       = (1<<4);

    /**
     * save all garbage in gc.garbage
     * (in Jython scoped on monitored objects)
     */
    public static final int DEBUG_SAVEALL       = (1<<5);
    public static final int DEBUG_LEAK = DEBUG_COLLECTABLE |
                                         DEBUG_UNCOLLECTABLE |
                                         DEBUG_INSTANCES |
                                         DEBUG_OBJECTS |
                                         DEBUG_SAVEALL;

    private static short gcFlags = DONT_TRAVERSE_BY_REFLECTION;
    private static int debugFlags = 0;
    private static boolean monitorNonTraversable = false;
    private static boolean waitingForFinalizers = false;
    private static AtomicBoolean gcRunning = new AtomicBoolean(false);
    private static HashSet<WeakReferenceGC> monitoredObjects;
    private static ReferenceQueue<Object> gcTrash;
    private static int finalizeWaitCount = 0;
    private static int initWaitTime = 10, defaultWaitFactor = 2;
    private static long lastRemoveTimeStamp = -1, maxWaitTime = initWaitTime;
    private static int gcMonitoredRunCount = 0;
    public static long gcRecallTime = 4000;
    public static PyList garbage = new PyList();

    //Finalization preprocess/postprocess-related declarations:
    private static List<Runnable> preFinalizationProcess, postFinalizationProcess;
    private static List<Runnable> preFinalizationProcessRemove, postFinalizationProcessRemove;
    private static Thread postFinalizationProcessor;
    private static long postFinalizationTimeOut = 100;
    private static long postFinalizationTimestamp = System.currentTimeMillis()-2*postFinalizationTimeOut;
    private static int openFinalizeCount = 0;
    private static boolean postFinalizationPending = false;
    private static boolean lockPostFinalization = false;

    //Resurrection-safe finalizer- and weakref-related declarations:
    private static IdentityHashMap<PyObject, PyObject> delayedFinalizables, resurrectionCritics;
    private static int abortedCyclicFinalizers = 0;
    //Some modes to control aspects of delayed finalization:
    private static final byte DO_NOTHING_SPECIAL = 0;
    private static final byte MARK_REACHABLE_CRITICS = 1;
    private static final byte NOTIFY_FOR_RERUN = 2;
    private static byte delayedFinalizationMode = DO_NOTHING_SPECIAL;
    private static boolean notifyRerun = false;

    public static final String __doc__ =
            "This module provides access to the garbage collector.\n" +
            "\n" +
            "enable() -- Enable automatic garbage collection (does nothing).\n" +
            "isenabled() -- Returns True because Java garbage collection cannot be disabled.\n" +
            "collect() -- Trigger a Java garbage collection (potentially expensive).\n" +
            "get_debug() -- Get debugging flags (returns 0).\n" +
            "\n" +
            "Other functions raise NotImplementedError because they do not apply to Java.\n";

    public static final String __name__ = "gc";


    public static class CycleMarkAttr {
        private boolean cyclic = false;
        private boolean uncollectable = false;
        public boolean monitored = false;

        CycleMarkAttr() {
        }

        CycleMarkAttr(boolean cyclic, boolean uncollectable) {
            this.cyclic = cyclic;
            this.uncollectable = uncollectable;
        }

        public boolean isCyclic() {
            return cyclic || uncollectable;
        }

        public boolean isUncollectable() {
            return uncollectable;
        }

        public void setFlags(boolean cyclic, boolean uncollectable) {
            this.cyclic = cyclic;
            this.uncollectable = uncollectable;
        }
    }

    private static class WeakReferenceGC extends WeakReference<PyObject> {
        int hashCode;
        public String str = null, inst_str = null;
        public String cls;
        boolean isInstance;
        boolean hasFinalizer = false;
        CycleMarkAttr cycleMark;

        WeakReferenceGC(PyObject referent) {
            super(referent);
            isInstance = referent instanceof PyInstance;
            cycleMark = (CycleMarkAttr)
                    JyAttribute.getAttr(referent, JyAttribute.GC_CYCLE_MARK_ATTR);
            hashCode = System.identityHashCode(referent);
            cls = referent.getClass().getName();
            updateHasFinalizer();
        }

        WeakReferenceGC(PyObject referent, ReferenceQueue<Object> q) {
            super(referent, q);
            isInstance = referent instanceof PyInstance;
            cycleMark = (CycleMarkAttr)
                    JyAttribute.getAttr(referent, JyAttribute.GC_CYCLE_MARK_ATTR);
            hashCode = System.identityHashCode(referent);
            cls = referent.getClass().getName();
            updateHasFinalizer();
        }

        public void updateHasFinalizer() {
            PyObject gt = get();
            Object fn = JyAttribute.getAttr(gt, JyAttribute.FINALIZE_TRIGGER_ATTR);
            hasFinalizer = fn != null && ((FinalizeTrigger) fn).isActive();
        }

        public void initStr(PyObject referent) {
            PyObject ref = referent;
            if (referent == null) {
                ref = get();
            }
            try {
                if (ref instanceof PyInstance) {
                    String name = ((PyInstance) ref).fastGetClass().__name__;
                    if (name == null) {
                        name = "?";
                    }
                    inst_str = String.format("<%.100s instance at %s>",
                            name, Py.idstr(ref));
                }
                str = String.format("<%.100s %s>",
                        ref.getType().getName(), Py.idstr(ref));
            } catch (Exception e) {
                str = "<"+ref.getClass().getSimpleName()+" "
                        +System.identityHashCode(ref)+">";
            }
        }
        
        public String toString() {
            return str;
        }
        
        public int hashCode() {
            return hashCode;
        }

        public boolean equals(Object ob) {
            if (ob instanceof WeakReferenceGC) {
                return ((WeakReferenceGC) ob).get().equals(get())
                    && ((WeakReferenceGC) ob).hashCode() == hashCode();
            } else if (ob instanceof WeakrefGCCompareDummy) {
                return ((WeakrefGCCompareDummy) ob).compare != null
                        && ((WeakrefGCCompareDummy) ob).compare.equals(get());
            } else {
                return false;
            }
        }
    }

    private static class WeakrefGCCompareDummy {
        public static WeakrefGCCompareDummy defaultInstance =
                new WeakrefGCCompareDummy();
        PyObject compare;
        int hashCode;
        
        public void setCompare(PyObject compare) {
            this.compare = compare;
            hashCode = System.identityHashCode(compare);
        }
        
        public int hashCode() {
            return hashCode;
        }
        
        @SuppressWarnings("rawtypes")
        public boolean equals(Object ob) {
            if (ob instanceof Reference) {
                return compare.equals(((Reference) ob).get());
            } else if (ob instanceof WeakrefGCCompareDummy) {
                return compare.equals(((WeakrefGCCompareDummy) ob).compare);
            } else {
                return compare.equals(ob);
            }
        }
    }

    private static class GCSentinel {
        Thread waiting;
        
        public GCSentinel(Thread notifyOnFinalize) {
            waiting = notifyOnFinalize;
        }

        protected void finalize() throws Throwable {
            //TODO: Find out why this would cause test to fail:
            //notifyPreFinalization();
            if ((gcFlags & VERBOSE_COLLECT) != 0) {
                writeDebug("gc", "Sentinel finalizer called...");
            }
            if (lastRemoveTimeStamp != -1) {
                long diff = maxWaitTime*defaultWaitFactor-System.currentTimeMillis()+lastRemoveTimeStamp;
                while (diff > 0) {
                    try {
                        Thread.sleep(diff);
                    } catch (InterruptedException ie) {}
                    diff = maxWaitTime*defaultWaitFactor-System.currentTimeMillis()+lastRemoveTimeStamp;
                }
            }
            if (waiting != null) {
                waiting.interrupt();
            }
            if ((gcFlags & VERBOSE_COLLECT) != 0) {
                writeDebug("gc", "Sentinel finalizer done");
            }
            //notifyPostFinalization();
        }
    }

    private static void writeDebug(String type, String msg) {
        if ((gcFlags & USE_PY_WRITE_DEBUG) != 0) {
            Py.writeDebug(type, msg);
        } else {
            System.err.println(type + ": " + msg);
        }
    }

    //----------delayed finalization section-----------------------------------

    private static class DelayedFinalizationProcess implements Runnable {
        static DelayedFinalizationProcess defaultInstance =
                new DelayedFinalizationProcess();

        private void performFinalization(PyObject del) {
            if ((gcFlags & VERBOSE_DELAYED) != 0) {
                writeDebug("gc", "delayed finalize of "+del);
            }
            FinalizeTrigger ft = (FinalizeTrigger)
                    JyAttribute.getAttr(del, JyAttribute.FINALIZE_TRIGGER_ATTR);
            if (ft != null) {
                ft.performFinalization();
            } else if ((gcFlags & VERBOSE_DELAYED) != 0) {
                writeDebug("gc", "no FinalizeTrigger");
            }
        }

        private void restoreFinalizer(PyObject obj, boolean cyclic) {
            FinalizeTrigger ft =
                    (FinalizeTrigger) JyAttribute.getAttr(obj, JyAttribute.FINALIZE_TRIGGER_ATTR);
            FinalizeTrigger.ensureFinalizer(obj);
            boolean notify = false;
            if (ft != null) {
                ((FinalizeTrigger)
                    JyAttribute.getAttr(obj, JyAttribute.FINALIZE_TRIGGER_ATTR)).flags
                    = ft.flags;
                notify = (ft.flags & FinalizeTrigger.NOTIFY_GC_FLAG) != 0;
            }
            if ((gcFlags & VERBOSE_DELAYED) != 0 || (gcFlags & VERBOSE_FINALIZE) != 0) {
                writeDebug("gc", "restore finalizer of "+obj+";  cyclic? "+cyclic);
            }
            CycleMarkAttr cm = (CycleMarkAttr)
                    JyAttribute.getAttr(obj, JyAttribute.GC_CYCLE_MARK_ATTR);
            if (cm != null && cm.monitored) {
                monitorObject(obj, true);
            }
            if (notify) {
                if ((gcFlags & VERBOSE_DELAYED) != 0 || (gcFlags & VERBOSE_FINALIZE) != 0) {
                    writeDebug("gc", "notify finalizer abort.");
                }
                notifyAbortFinalize(obj, cyclic);
            }
        }

        public void run() {
            if ((gcFlags & VERBOSE_DELAYED) != 0) {
                writeDebug("gc", "run delayed finalization. Index: "+
                        gcMonitoredRunCount);
            }
            Set<PyObject> critics = resurrectionCritics.keySet();
            Set<PyObject> cyclicCritics = removeNonCyclic(critics);
            cyclicCritics.retainAll(critics);
            critics.removeAll(cyclicCritics);
            Set<PyObject> criticReachablePool = findReachables(critics);
            //to avoid concurrent modification:
            ArrayList<PyObject> criticReachables = new ArrayList<>();
            FinalizeTrigger fn;
            if (delayedFinalizationMode == MARK_REACHABLE_CRITICS) {
                for (PyObject obj: criticReachablePool) {
                    fn = (FinalizeTrigger) JyAttribute.getAttr(obj,
                            JyAttribute.FINALIZE_TRIGGER_ATTR);
                    if (fn != null && fn.isActive() && fn.isFinalized()) {
                        criticReachables.add(obj);
                        JyAttribute.setAttr(obj,
                            JyAttribute.GC_DELAYED_FINALIZE_CRITIC_MARK_ATTR,
                            Integer.valueOf(gcMonitoredRunCount));
                    }
                }
            } else {
                for (PyObject obj: criticReachablePool) {
                    fn = (FinalizeTrigger) JyAttribute.getAttr(obj,
                            JyAttribute.FINALIZE_TRIGGER_ATTR);
                    if (fn != null && fn.isActive() && fn.isFinalized()) {
                        criticReachables.add(obj);
                    }
                }
            }
            critics.removeAll(criticReachables);
            if ((gcFlags & PRESERVE_WEAKREFS_ON_RESURRECTION) != 0) {
                if ((gcFlags & VERBOSE_DELAYED) != 0) {
                    writeDebug("gc", "restore potentially resurrected weak references...");
                }
                GlobalRef toRestore;
                for (PyObject rst: criticReachablePool) {
                    toRestore = (GlobalRef)
                            JyAttribute.getAttr(rst, JyAttribute.WEAK_REF_ATTR);
                    if (toRestore != null) {
                        toRestore.restore(rst);
                    }
                }
                GlobalRef.processDelayedCallbacks();
            }
            criticReachablePool.clear();
            if ((gcFlags & DONT_FINALIZE_RESURRECTED_OBJECTS) != 0) {
                //restore all finalizers that might belong to resurrected
                //objects:
                if ((gcFlags & VERBOSE_DELAYED) != 0) {
                    writeDebug("gc", "restore "+criticReachables.size()+
                            " potentially resurrected finalizers...");
                }
                for (PyObject obj: criticReachables) {
                    CycleMarkAttr cm = (CycleMarkAttr)
                            JyAttribute.getAttr(obj, JyAttribute.GC_CYCLE_MARK_ATTR);
                    if (cm != null && cm.isUncollectable()) {
                        restoreFinalizer(obj, true);
                    } else {
                        gc.markCyclicObjects(obj, true);
                        cm = (CycleMarkAttr)
                                JyAttribute.getAttr(obj, JyAttribute.GC_CYCLE_MARK_ATTR);
                        restoreFinalizer(obj, cm != null && cm.isUncollectable());
                    }
                }
            } else {
                if ((gcFlags & VERBOSE_DELAYED) != 0) {
                    writeDebug("gc", "delayed finalization of "+criticReachables.size()+
                            " potentially resurrected finalizers...");
                }
                for (PyObject del: criticReachables) {
                    performFinalization(del);
                }
            }
            cyclicCritics.removeAll(criticReachables);
            if ((gcFlags & VERBOSE_DELAYED) != 0 && !delayedFinalizables.isEmpty()) {
                writeDebug("gc", "process "+delayedFinalizables.size()+
                        " delayed finalizers...");
            }
            for (PyObject del: delayedFinalizables.keySet()) {
                performFinalization(del);
            }
            if ((gcFlags & VERBOSE_DELAYED) != 0 && !cyclicCritics.isEmpty()) {
                writeDebug("gc", "process "+cyclicCritics.size()+" cyclic delayed finalizers...");
            }
            for (PyObject del: cyclicCritics) {
                performFinalization(del);
            }
            if ((gcFlags & VERBOSE_DELAYED) != 0 && !critics.isEmpty()) {
                writeDebug("gc", "calling "+critics.size()+
                        " critic finalizers not reachable by other critic finalizers...");
            }
            if (delayedFinalizationMode == MARK_REACHABLE_CRITICS &&
                    !critics.isEmpty() && !criticReachables.isEmpty()) {
                // This means some critic-reachables might be not critic-reachable any more.
                // In a synchronized gc collection approach System.gc should run again while
                // something like this is found. (Yes, not exactly a cheap task, but since this
                // is for debugging, correctness counts.)
                notifyRerun = true;
            }
            if (delayedFinalizationMode == NOTIFY_FOR_RERUN && !notifyRerun) {
                for (PyObject del: critics) {
                    if (!notifyRerun) {
                        Object m = JyAttribute.getAttr(del,
                                JyAttribute.GC_DELAYED_FINALIZE_CRITIC_MARK_ATTR);
                        if (m != null && ((Integer) m).intValue() == gcMonitoredRunCount) {
                            notifyRerun = true;
                        }
                    }
                    performFinalization(del);
                }
            } else {
                for (PyObject del: critics) {
                    performFinalization(del);
                }
            }
            delayedFinalizables.clear();
            resurrectionCritics.clear();
            if ((gcFlags & VERBOSE_DELAYED) != 0) {
                writeDebug("gc", "delayed finalization run done");
            }
        }
    }

    public static boolean delayedFinalizationEnabled() {
        return (gcFlags & (PRESERVE_WEAKREFS_ON_RESURRECTION |
                DONT_FINALIZE_RESURRECTED_OBJECTS)) != 0;
    }

    private static void updateDelayedFinalizationState() {
        if (delayedFinalizationEnabled()) {
            resumeDelayedFinalization();
        } else if (indexOfPostFinalizationProcess(
                DelayedFinalizationProcess.defaultInstance) != -1) {
            suspendDelayedFinalization();
        }
        if ((gcFlags & PRESERVE_WEAKREFS_ON_RESURRECTION) == 0) {
            if (GlobalRef.hasDelayedCallbacks()) {
                Thread dlcProcess = new Thread() {
                    public void run() {
                        GlobalRef.processDelayedCallbacks();
                    }
                };
                dlcProcess.start();
            }
        }
    }

    private static void resumeDelayedFinalization() {
        if (delayedFinalizables == null) {
            delayedFinalizables = new IdentityHashMap<>();
        }
        if (resurrectionCritics == null) {
            resurrectionCritics = new IdentityHashMap<>();
        }
        //add post-finalization process (and cancel pending suspension process if any)
        try {
            synchronized(postFinalizationProcessRemove) {
                postFinalizationProcessRemove.remove(
                        DelayedFinalizationProcess.defaultInstance);
                if (postFinalizationProcessRemove.isEmpty()) {
                    postFinalizationProcessRemove = null;
                }
            }
        } catch (NullPointerException npe) {}
        if (indexOfPostFinalizationProcess(
                DelayedFinalizationProcess.defaultInstance) == -1) {
            registerPostFinalizationProcess(
                    DelayedFinalizationProcess.defaultInstance);
        }
    }

    private static void suspendDelayedFinalization() {
        unregisterPostFinalizationProcessAfterNextRun(
                DelayedFinalizationProcess.defaultInstance);
    }

    private static boolean isResurrectionCritic(PyObject ob) {
        return (isTraversable(ob))
                && FinalizeTrigger.hasActiveTrigger(ob);
    }

    public static void registerForDelayedFinalization(PyObject ob) {
        if (isResurrectionCritic(ob)) {
            resurrectionCritics.put(ob, ob);
        } else {
            delayedFinalizables.put(ob, ob);
        }
    }
    //----------end of delayed finalization section----------------------------




    //----------Finalization preprocess/postprocess section--------------------

    protected static class PostFinalizationProcessor implements Runnable {
        protected static PostFinalizationProcessor defaultInstance =
                new PostFinalizationProcessor();

        public void run() {
            // We wait until last postFinalizationTimestamp is at least timeOut ago.
            // This should only be measured when openFinalizeCount is zero.
            long current = System.currentTimeMillis();
            while (true) {
                if (!lockPostFinalization && openFinalizeCount == 0
                        && current - postFinalizationTimestamp
                        > postFinalizationTimeOut) {
                    break;
                }
                try {
                    long time = postFinalizationTimeOut - current + postFinalizationTimestamp;
                    if (openFinalizeCount != 0 || lockPostFinalization || time < 0) {
                        time = gcRecallTime;
                    }
                    Thread.sleep(time);
                } catch (InterruptedException ie) {
                }
                current = System.currentTimeMillis();
            }
            postFinalizationProcessor = null;
            postFinalizationProcess();
            synchronized(PostFinalizationProcessor.class) {
                postFinalizationPending = false;
                PostFinalizationProcessor.class.notify();
            }
        }
    }

    /**
     * <p>
     * Registers a process that will be called before any finalization during gc-run
     * takes place ("finalization" refers to Jython-style finalizers ran by
     * {@link org.python.core.finalization.FinalizeTrigger}s;
     * to care for other finalizers these must call
     * {@code gc.notifyPreFinalization()} before anything else is done and
     * {@code gc.notifyPostFinalization()} afterwards; between these calls the finalizer
     * must not terminate by throwing an exception). (Note: Using this for extern
     * finalizers is currently experimental and needs more testing.)
     * This works independently from monitoring, which is mainly needed to allow
     * counting of cyclic garbage in {@code gc.collect}.
     * </p>
     * <p>
     * This feature compensates that Java's gc does not provide any guarantees about
     * finalization order. Java not even guarantees that when a weak reference is
     * added to a reference queue, its finalizer already ran or not yet ran, if any.
     * </p>
     * <p>
     * The only guarantee is that {@link java.lang.ref.PhantomReference}s are enqueued
     * after finalization of their referents, but this happens in another gc-cycle then.
     * </p>
     * <p>
     * Actually there are still situations that can cause pre-finalization process to
     * run again during finalization phase. This can happen if external frameworks use
     * their own finalizers. This can be cured by letting these finalizers call
     * {@code gc.notifyPreFinalization()} before anything else is done and
     * {@code gc.notifyPostFinalization()} right before the finalization method returns.
     * Between these calls the finalizer must not terminate by throwing an exception.
     * (Note: Using this for extern finalizers is currently experimental and needs more testing.)
     * </p>
     * <p>
     * We recommend to use this feature in a way such that false-positive runs are
     * not critically harmful, e.g. use it to enhance performance, but don't let it
     * cause a crash if preprocess is rerun unexpectedly.
     * </p>
     */
    public static void registerPreFinalizationProcess(Runnable process) {
        registerPreFinalizationProcess(process, -1);
    }

    /**
     * See doc of {@core registerPreFinalizationProcess(Runnable process)}.
     */
    public static void registerPreFinalizationProcess(Runnable process, int index) {
        while (true) {
            try {
                synchronized (preFinalizationProcess) {
                    preFinalizationProcess.add(index < 0 ?
                            index+preFinalizationProcess.size()+1 : index, process);
                }
                return;
            } catch (NullPointerException npe) {
                preFinalizationProcess = new ArrayList<>(1);
            }
        }
    }

    public static int indexOfPreFinalizationProcess(Runnable process) {
        try {
            synchronized (preFinalizationProcess) {
                return preFinalizationProcess.indexOf(process);
            }
        } catch (NullPointerException npe) {
            return -1;
        }
    }

    public static boolean unregisterPreFinalizationProcess(Runnable process) {
        try {
            synchronized (preFinalizationProcess) {
                boolean result = preFinalizationProcess.remove(process);
                if (result && preFinalizationProcess.isEmpty()) {
                    preFinalizationProcess = null;
                }
                return result;
            }
        } catch (NullPointerException npe) {
            return false;
        }
    }

    /**
     * Useful if a process wants to remove another one or itself during its execution.
     * This asynchronous unregister method circumvents the synchronized-state on
     * pre-finalization process list.
     */
    public static void unregisterPreFinalizationProcessAfterNextRun(Runnable process) {
        while (true) {
            try {
                synchronized (preFinalizationProcessRemove) {
                    preFinalizationProcessRemove.add(process);
                }
                return;
            } catch (NullPointerException npe) {
                preFinalizationProcessRemove = new ArrayList<>(1);
            }
        }
    }

    /**
     * <p>
     * Registers a process that will be called after all finalization during gc-run
     * is done ("finalization" refers to Jython-style finalizers ran by
     * {@link org.python.core.finalization.FinalizeTrigger}s;
     * to care for other finalizers these must call
     * {@code gc.notifyPreFinalization()} before anything else is done and
     * {@code gc.notifyPostFinalization()} afterwards; between these calls the finalizer
     * must not terminate by throwing an exception). (Note: Using this for extern
     * finalizers is currently experimental and needs more testing.)
     * This works independently from monitoring (which is mainly needed to allow
     * garbage counting in {@code gc.collect}).
     * </p>
     * <p>
     * This feature compensates that Java's gc does not provide any guarantees about
     * finalization order. Java not even guarantees that when a weak reference is
     * added to a reference queue, its finalizer already ran or not yet ran, if any.
     * </p>
     * <p>
     * The only guarantee is that {@link java.lang.ref.PhantomReference}s are
     * enqueued after finalization of the referents, but this
     * happens - however - in another gc-cycle then.
     * </p>
     * <p>
     * There are situations that can cause post finalization process to run
     * already during finalization phase. This can happen if external frameworks use
     * their own finalizers. This can be cured by letting these finalizers call
     * {@code gc.notifyPreFinalization()} before anything else is done and
     * {@code notifyPostFinalization()} right before the finalization method returns.
     * Between these calls the finalizer must not terminate by throwing an exception.
     * (Note: Using this for extern finalizers is currently experimental and needs more testing.)
     * </p>
     * <p>
     * If it runs too early, we can at least guarantee that it will run again after
     * finalization was really done. So we recommend to use this feature in a way
     * such that false-positive runs are not critically harmful.
     * </p>
     */
    public static void registerPostFinalizationProcess(Runnable process) {
        registerPostFinalizationProcess(process, -1);
    }

    /**
     * See doc of {@code registerPostFinalizationProcess(Runnable process)}.
     */
    public static void registerPostFinalizationProcess(Runnable process, int index) {
        while (true) {
            try {
                synchronized (postFinalizationProcess) {
                    postFinalizationProcess.add(index < 0 ?
                            index+postFinalizationProcess.size()+1 : index, process);
                }
                return;
            } catch (NullPointerException npe) {
                postFinalizationProcess = new ArrayList<>(1);
            }
        }
    }

    public static int indexOfPostFinalizationProcess(Runnable process) {
        try {
            synchronized (postFinalizationProcess) {
                return postFinalizationProcess.indexOf(process);
            }
        } catch (NullPointerException npe) {
            return -1;
        }
    }

    public static boolean unregisterPostFinalizationProcess(Runnable process) {
        try {
            synchronized (postFinalizationProcess) {
                boolean result = postFinalizationProcess.remove(process);
                if (result && postFinalizationProcess.isEmpty()) {
                    postFinalizationProcess = null;
                }
                return result;
            }
        } catch (NullPointerException npe) {
            return false;
        }
    }

    /**
     * Useful if a process wants to remove another one or itself during its execution.
     * This asynchronous unregister method circumvents the synchronized-state on
     * post-finalization process list.
     */
    public static void unregisterPostFinalizationProcessAfterNextRun(Runnable process) {
        while (true) {
            try {
                synchronized (postFinalizationProcessRemove) {
                    postFinalizationProcessRemove.add(process);
                }
                return;
            } catch (NullPointerException npe) {
                postFinalizationProcessRemove = new ArrayList<>(1);
            }
        }
    }

    public static void notifyPreFinalization() {
        ++openFinalizeCount;
        if (System.currentTimeMillis() - postFinalizationTimestamp
                < postFinalizationTimeOut) {
            return;
        }
        try {
            synchronized(preFinalizationProcess) {
                for (Runnable r: preFinalizationProcess) {
                    try {
                        r.run();
                    } catch (Exception preProcessError) {
                        Py.writeError("gc", "Finalization preprocess "+r+" caused error: "
                                +preProcessError);
                    }
                }
                try {
                    synchronized (preFinalizationProcessRemove) {
                        preFinalizationProcess.removeAll(preFinalizationProcessRemove);
                        preFinalizationProcessRemove = null;
                    }
                    if (preFinalizationProcess.isEmpty()) {
                        preFinalizationProcess = null;
                    }
                } catch (NullPointerException npe0) {}
            }
        } catch (NullPointerException npe) {
            preFinalizationProcessRemove = null;
        }

        try {
            synchronized(postFinalizationProcess) {
                if (!postFinalizationProcess.isEmpty() &&
                        postFinalizationProcessor == null) {
                    postFinalizationPending = true;
                    postFinalizationProcessor = new Thread(
                            PostFinalizationProcessor.defaultInstance);
                    postFinalizationProcessor.start();
                }
            }
        } catch (NullPointerException npe) {}
    }

    public static void notifyPostFinalization() {
        postFinalizationTimestamp = System.currentTimeMillis();
        --openFinalizeCount;
        if (openFinalizeCount == 0 && postFinalizationProcessor != null) {
            postFinalizationProcessor.interrupt();
        }
    }

    protected static void postFinalizationProcess() {
        try {
            synchronized(postFinalizationProcess) {
                for (Runnable r: postFinalizationProcess) {
                    try {
                        r.run();
                    } catch (Exception postProcessError) {
                        System.err.println("Finalization postprocess "+r+" caused error:");
                        System.err.println(postProcessError);
                    }
                }
                try {
                    synchronized (postFinalizationProcessRemove) {
                        postFinalizationProcess.removeAll(postFinalizationProcessRemove);
                        postFinalizationProcessRemove = null;
                    }
                    if (postFinalizationProcess.isEmpty()) {
                        postFinalizationProcess = null;
                    }
                } catch (NullPointerException npe0) {}
            }
        } catch (NullPointerException npe) {
            postFinalizationProcessRemove = null;
        }
    }
    //----------end of Finalization preprocess/postprocess section-------------




    //----------Monitoring section---------------------------------------------
    
    public static void monitorObject(PyObject ob) {
        monitorObject(ob, false);
    }

    public static void monitorObject(PyObject ob, boolean initString) {
        //Already collected garbage should not be monitored,
        //thus also not the garbage list:
        if (ob == garbage) {
            return;
        }
        if (!monitorNonTraversable && !isTraversable(ob)) {
            if (!JyAttribute.hasAttr(ob, JyAttribute.FINALIZE_TRIGGER_ATTR)) {
                return;
            }
        }
        if (gcTrash == null) {
            gcTrash = new ReferenceQueue<>();
        }
        while (true) {
            try {
                synchronized(monitoredObjects) {
                    if (!isMonitored(ob)) {
                        CycleMarkAttr cm = new CycleMarkAttr();
                        JyAttribute.setAttr(ob, JyAttribute.GC_CYCLE_MARK_ATTR, cm);
                        WeakReferenceGC refPut = new WeakReferenceGC(ob, gcTrash);
                        if (initString) {
                            refPut.initStr(ob);
                        }
                        monitoredObjects.add(refPut);
                        cm.monitored = true;
                    }
                }
                return;
            } catch (NullPointerException npe) {
                monitoredObjects = new HashSet<WeakReferenceGC>();
            }
        }
    }

    /**
     * Avoid to use this method. It is inefficient and no intended purpose of the
     * backing Set of objects. In normal business it should not be needed and only
     * exists for bare debugging purposes.
     */
    public static WeakReferenceGC getMonitorReference(PyObject ob) {
        try {
            synchronized(monitoredObjects) {
                for (WeakReferenceGC ref: monitoredObjects) {
                    if (ref.equals(ob)) {
                        return ref;
                    }
                }
            }
        } catch (NullPointerException npe) {}
        return null;
    }

    public static boolean isMonitoring() {
        try {
            synchronized(monitoredObjects) {
                return !monitoredObjects.isEmpty();
            }
        } catch (NullPointerException npe) {
            return false;
        }
    }

    public static boolean isMonitored(PyObject ob) {
        try {
            synchronized(monitoredObjects) {
                WeakrefGCCompareDummy.defaultInstance.setCompare(ob);
                boolean result = monitoredObjects.contains(
                    WeakrefGCCompareDummy.defaultInstance);
                WeakrefGCCompareDummy.defaultInstance.compare = null;
                return result;
            }
        } catch (NullPointerException npe) {
            return false;
        }
    }

    public static boolean unmonitorObject(PyObject ob) {
        try {
            synchronized(monitoredObjects) {
                WeakrefGCCompareDummy.defaultInstance.setCompare(ob);
                WeakReferenceGC rem = getMonitorReference(ob);
                if (rem != null) {
                    rem.clear();
                }
                boolean result = monitoredObjects.remove(
                    WeakrefGCCompareDummy.defaultInstance);
                WeakrefGCCompareDummy.defaultInstance.compare = null;
                JyAttribute.delAttr(ob, JyAttribute.GC_CYCLE_MARK_ATTR);
                FinalizeTrigger ft = (FinalizeTrigger)
                    JyAttribute.getAttr(ob, JyAttribute.FINALIZE_TRIGGER_ATTR);
                if (ft != null) {
                    ft.flags &= ~FinalizeTrigger.NOTIFY_GC_FLAG;
                }
                return result;
            }
        } catch (NullPointerException npe) {
            return false;
        }
    }

    public static void unmonitorAll() {
        try {
            synchronized(monitoredObjects) {
                FinalizeTrigger ft;
                for (WeakReferenceGC mo: monitoredObjects) {
                    PyObject rfrt = mo.get();
                    if (rfrt != null) {
                        JyAttribute.delAttr(rfrt, JyAttribute.GC_CYCLE_MARK_ATTR);
                        ft = (FinalizeTrigger)
                                JyAttribute.getAttr(rfrt, JyAttribute.FINALIZE_TRIGGER_ATTR);
                        if (ft != null) {
                            ft.flags &= ~FinalizeTrigger.NOTIFY_GC_FLAG;
                        }
                    }
                    mo.clear();
                }
                monitoredObjects.clear();
            }
        } catch (NullPointerException npe) {
        }
    }

    public static void stopMonitoring() {
        setMonitorGlobal(false);
        if (monitoredObjects != null) {
            unmonitorAll();
            monitoredObjects = null;
        }
    }

    public static boolean getMonitorGlobal() {
        return PyObject.gcMonitorGlobal;
    }

    public static void setMonitorGlobal(boolean flag) {
        if (flag) {
            gcFlags |= MONITOR_GLOBAL;
        } else {
            gcFlags &= ~MONITOR_GLOBAL;
        }
        PyObject.gcMonitorGlobal = flag;
    }
    //----------end of Monitoring section--------------------------------------


    public static short getJythonGCFlags() {
        if (((gcFlags & MONITOR_GLOBAL) != 0) != PyObject.gcMonitorGlobal) {
            if (PyObject.gcMonitorGlobal) {
                gcFlags |= MONITOR_GLOBAL;
            } else {
                gcFlags &= ~MONITOR_GLOBAL;
            }
        }
        return gcFlags;
    }

    public static void setJythonGCFlags(short flags) {
        gcFlags = flags;
        PyObject.gcMonitorGlobal = (gcFlags & MONITOR_GLOBAL) != 0;
        updateDelayedFinalizationState();
    }

    /**
     * This is a convenience method to add flags via bitwise or.
     */
    public static void addJythonGCFlags(short flags) {
        gcFlags |= flags;
        PyObject.gcMonitorGlobal = (gcFlags & MONITOR_GLOBAL) != 0;
        updateDelayedFinalizationState();
    }

    /**
     * This is a convenience method to remove flags via bitwise and-not.
     */
    public static void removeJythonGCFlags(short flags) {
        gcFlags &= ~flags;
        PyObject.gcMonitorGlobal = (gcFlags & MONITOR_GLOBAL) != 0;
        updateDelayedFinalizationState();
    }

    /**
     * Do not call this method manually.
     * It should only be called by
     * {@link org.python.core.finalization.FinalizeTrigger}.
     */
    public static void notifyFinalize(PyObject finalized) {
        if (--finalizeWaitCount == 0 && waitingForFinalizers) {
            synchronized(GCSentinel.class) {
                GCSentinel.class.notify();
            }
        }
    }

    /**
     * For now this just calls {@code notifyFinalize}, as the only current
     * purpose is to decrement the open finalizer count.
     */
    private static void notifyAbortFinalize(PyObject abort, boolean cyclic) {
        if (cyclic) {
            ++abortedCyclicFinalizers;
        }
        notifyFinalize(abort);
    }

    public static void enable() {}

    public static void disable() {
        throw Py.NotImplementedError("can't disable Java GC");
    }

    public static boolean isenabled() { return true; }

    /**
     * The generation parameter is only for compatibility with
     * CPython {@code gc.collect} and is ignored.
     * @param generation (ignored)
     * @return Collected monitored cyclic trash-objects or
     * {@code gc.UNKNOWN_COUNT} if nothing is monitored or -1 if
     * an error occurred and collection did not complete.
     */
    public static int collect(int generation) {
        return collect();
    }
 
    private static boolean needsTrashPrinting() {
        return ((debugFlags & DEBUG_COLLECTABLE) != 0 ||
                (debugFlags & DEBUG_UNCOLLECTABLE) != 0) &&
                ((debugFlags & DEBUG_INSTANCES) != 0 ||
                (debugFlags & DEBUG_OBJECTS) != 0);
    }

    private static boolean needsCollectBuffer() {
        return (debugFlags & DEBUG_STATS) != 0 || needsTrashPrinting();
    }

    /**
     * If no objects are monitored, this just delegates to
     * {@code System.gc} and returns {@code gc.UNKNOWN_COUNT} as a
     * non-erroneous default value. If objects are monitored,
     * it emulates a synchronous gc run in the sense that it waits
     * until all collected monitored objects were finalized.
     * 
     * @return Number of collected monitored cyclic trash-objects
     * or {@code gc.UNKNOWN_COUNT} if nothing is monitored or -1
     * if an error occurred and collection did not complete.
     */
    public static int collect() {
        try {
            return collect_intern();
        } catch (java.util.ConcurrentModificationException cme) {
            cme.printStackTrace();
        } catch (NullPointerException npe) {
            npe.printStackTrace();
        } 
        return -1;
    }

    private static int collect_intern() {
        long t1 = 0;
        int result;
        if ((debugFlags & DEBUG_STATS) != 0) {
            t1 = System.currentTimeMillis();
        }
        if (!isMonitoring()) {
            if ((debugFlags & DEBUG_STATS) != 0) {
                writeDebug("gc", "collecting generation x...");
                writeDebug("gc", "objects in each generation: unknown");
            }
            if ((gcFlags & VERBOSE_COLLECT) != 0) {
                writeDebug("gc", "no monitoring; perform ordinary async System.gc...");
            }
            System.gc();
            result = UNKNOWN_COUNT; //indicates unknown result (-1 would indicate error)
        } else {
            if (!gcRunning.compareAndSet(false, true)) {
                if ((gcFlags & VERBOSE_COLLECT) != 0) {
                    writeDebug("gc", "collect already running...");
                }
                //We must fail fast in this case to avoid deadlocks.
                //Deadlock would for instance occur if a finalizer calls
                //gc.collect (like is done in some tests in test_gc). 
                //Former version: throw new IllegalStateException("GC is already running.");
                return -1; //better not throw exception here, as calling code
                           //is usually not prepared for that
            }
            if ((gcFlags & VERBOSE_COLLECT) != 0) {
                writeDebug("gc", "perform monitored sync gc run...");
            }
            if (needsTrashPrinting() || (gcFlags & VERBOSE) != 0) {
                // When the weakrefs are enqueued, the referents won't be available
                // any more to provide their string representations, so we must
                // save the string representations in the weak ref objects while
                // the referents are still alive.
                // We cannot save the string representations in the moment when the
                // objects get monitored, because with monitorGlobal activated
                // the objects get monitored just when they are created and some
                // of them are in an invalid state then and cannot directly obtain
                // their string representation (produce overflow errors and such bad
                // stuff). So we do it here...
                List<WeakReferenceGC> lst = new ArrayList<>();
                for (WeakReferenceGC wr: monitoredObjects) {
                    if (wr.str == null) {
                        lst.add(wr);
                    }
                }
                for (WeakReferenceGC ol: lst) {
                    ol.initStr(null);
                }
                lst.clear();
            }
            ++gcMonitoredRunCount;
            delayedFinalizationMode = MARK_REACHABLE_CRITICS;
            notifyRerun = false;
            
            int[] stat = {0, 0};
            
            syncCollect(stat, (debugFlags & DEBUG_STATS) != 0);
            delayedFinalizationMode = NOTIFY_FOR_RERUN;

            if (notifyRerun) {
                if ((gcFlags & VERBOSE_COLLECT) != 0) {
                    writeDebug("gc", "initial sync collect done.");
                }
                while (notifyRerun) {
                    notifyRerun = false;
                    if ((gcFlags & VERBOSE_COLLECT) != 0) {
                        writeDebug("gc", "rerun gc...");
                    }
                    syncCollect(stat, false);
                }
                if ((gcFlags & VERBOSE_COLLECT) != 0) {
                    writeDebug("gc", "all sync collect runs done.");
                }
            } else if ((gcFlags & VERBOSE_COLLECT) != 0) {
                writeDebug("gc", "sync collect done.");
            }

            delayedFinalizationMode = DO_NOTHING_SPECIAL;
            gcRunning.set(false);
            result = stat[0];
            if ((debugFlags & DEBUG_STATS) != 0) {
                if (result != UNKNOWN_COUNT) {
                    StringBuilder sb = new StringBuilder("done, ");
                    sb.append(stat[0]);
                    sb.append(" unreachable, ");
                    sb.append(stat[1]);
                    sb.append(" uncollectable");
                    if (t1 != 0) {
                        sb.append(", ");
                        sb.append((System.currentTimeMillis()-t1)/1000.0);
                        sb.append("s elapsed");
                    }
                    sb.append(".");
                    writeDebug("gc", sb.toString());
                }
            }
        }
        if ((debugFlags & DEBUG_STATS) != 0 && result == UNKNOWN_COUNT) {
            StringBuilder sb = new StringBuilder("done");
            if (t1 != 0) {
                sb.append(", ");
                sb.append((System.currentTimeMillis()-t1)/1000.0);
                sb.append("s elapsed");
            }
            sb.append(".");
            writeDebug("gc", sb.toString());
        }
        return result;
    }

    private static void syncCollect(int[] stat, boolean debugStat) {
        abortedCyclicFinalizers = 0;
        lockPostFinalization = true;
        Reference<? extends Object> trash;
        try {
            trash = gcTrash.remove(initWaitTime);
            if (trash != null && (gcFlags & VERBOSE_COLLECT) != 0) {
                writeDebug("gc", "monitored objects from previous gc-run found.");
            }
        } catch (InterruptedException ie) {
            trash = null;
        }
        Set<WeakReferenceGC> cyclic;
        IdentityHashMap<PyObject, WeakReferenceGC> cyclicLookup;
        synchronized(monitoredObjects) {
            if (trash != null) {
                while (trash != null) {
                    monitoredObjects.remove(trash);
                    try {
                        trash = gcTrash.remove(initWaitTime);
                    } catch (InterruptedException ie) {
                        trash = null;
                    }
                }
                if ((gcFlags & VERBOSE_COLLECT) != 0) {
                    writeDebug("gc", "cleaned up previous trash.");
                }
            }
            FinalizeTrigger ft;
            for (WeakReferenceGC wrg: monitoredObjects) {
                wrg.updateHasFinalizer();
                if (wrg.hasFinalizer) {
                    ft = (FinalizeTrigger)
                        JyAttribute.getAttr(wrg.get(), JyAttribute.FINALIZE_TRIGGER_ATTR);
                    ft.flags |= FinalizeTrigger.NOTIFY_GC_FLAG;
                    //The NOTIFY_GC_FLAG is needed, because monitor state changes during
                    //collection. So the FinalizeTriggers can't use gc.isMonitored to know
                    //whether gc notification is needed.
                }
            }

            //Typically this line causes a gc-run:
            cyclicLookup = removeNonCyclicWeakRefs(monitoredObjects);
            cyclic = new HashSet<>(cyclicLookup.values());
            if (debugStat) {
                writeDebug("gc", "collecting generation x...");
                writeDebug("gc", "objects in each generation: "+cyclic.size());
            }

            if ((debugFlags & DEBUG_SAVEALL) != 0
                    || (gcFlags & DONT_FINALIZE_RESURRECTED_OBJECTS) != 0) {
                cyclic.retainAll(monitoredObjects);
                for (WeakReferenceGC wrg: cyclic) {
                    if (!wrg.hasFinalizer) {
                        PyObject obj = wrg.get();
                        FinalizeTrigger.ensureFinalizer(obj);
                        wrg.updateHasFinalizer();
                        ft = (FinalizeTrigger)
                            JyAttribute.getAttr(obj, JyAttribute.FINALIZE_TRIGGER_ATTR);
                        ft.flags |= FinalizeTrigger.NOT_FINALIZABLE_FLAG;
                        ft.flags |= FinalizeTrigger.NOTIFY_GC_FLAG;
                    }
                }
            }
        }
        maxWaitTime = initWaitTime;
        WeakReference<GCSentinel> sentRef =
        new WeakReference<>(new GCSentinel(Thread.currentThread()), gcTrash);
        lastRemoveTimeStamp = System.currentTimeMillis();
        if (finalizeWaitCount != 0) {
            System.err.println("Finalize wait count should be initially 0!");
            finalizeWaitCount = 0;
        }
        
        // We tidy up a bit... (Because it is not unlikely that
        // the preparation-stuff done so far has caused a gc-run.)
        // This is not entirely safe as gc could interfere with
        // this process at any time again. Since this is intended
        // for debugging, this solution is sufficient in practice.
        // Maybe we will include more mechanisms to ensure safety
        // in the future.
        
        try {
            trash = gcTrash.remove(initWaitTime);
            if (trash != null && (gcFlags & VERBOSE_COLLECT) != 0) {
                writeDebug("gc", "monitored objects from interferring gc-run found.");
            }
        } catch (InterruptedException ie) {
            trash = null;
        }
        if (trash != null) {
            while (trash != null) {
                monitoredObjects.remove(trash);
                if (cyclic.remove(trash) && (gcFlags & VERBOSE_COLLECT) != 0) {
                    writeDebug("gc", "cyclic interferring trash: "+trash);
                } else if ((gcFlags & VERBOSE_COLLECT) != 0) {
                    writeDebug("gc", "interferring trash: "+trash);
                }
                try {
                    trash = gcTrash.remove(initWaitTime);
                } catch (InterruptedException ie) {
                    trash = null;
                }
            }
            if ((gcFlags & VERBOSE_COLLECT) != 0) {
                writeDebug("gc", "cleaned up interferring trash.");
            }
        }
        if ((gcFlags & VERBOSE_COLLECT) != 0) {
            writeDebug("gc", "call System.gc.");
        }
        cyclicLookup = null;
        System.gc();
        List<WeakReferenceGC> collectBuffer = null;
        if (needsCollectBuffer()) {
            collectBuffer = new ArrayList<>();
        }
        long removeTime;
        try {
            while(true) {
                removeTime = System.currentTimeMillis()-lastRemoveTimeStamp;
                if (removeTime > maxWaitTime) {
                    maxWaitTime = removeTime;
                }
                lastRemoveTimeStamp = System.currentTimeMillis();
                trash = gcTrash.remove(Math.max(gcRecallTime, maxWaitTime*defaultWaitFactor));
                if (trash != null) {
                    if (trash instanceof WeakReferenceGC) {
                        synchronized(monitoredObjects) {
                            monitoredObjects.remove(trash);
                        }
                        //We avoid counting jython-specific objects in order to
                        //obtain CPython-comparable results.
                        if (cyclic.contains(trash) && !((WeakReferenceGC) trash).cls.contains("Java")) {
                            ++stat[0];
                            if (collectBuffer != null) {
                                collectBuffer.add((WeakReferenceGC) trash);
                            }
                            if ((gcFlags & VERBOSE_COLLECT) != 0) {
                                writeDebug("gc", "Collected cyclic object: "+trash);
                            }
                        }
                        if (((WeakReferenceGC) trash).hasFinalizer) {
                            ++finalizeWaitCount;
                            if ((gcFlags & VERBOSE_FINALIZE) != 0) {
                                writeDebug("gc", "Collected finalizable object: "+trash);
                                writeDebug("gc", "New finalizeWaitCount: "+finalizeWaitCount);
                            }
                        }
                    } else if (trash == sentRef && (gcFlags & VERBOSE_COLLECT) != 0) {
                        writeDebug("gc", "Sentinel collected.");
                    }
                } else {
                    System.gc();
                }
            }
        } catch (InterruptedException iex) {}

        if ((gcFlags & VERBOSE_COLLECT) != 0) {
            writeDebug("gc", "all objects from run enqueud in trash queue.");
            writeDebug("gc", "pending finalizers: "+finalizeWaitCount);
        }
        //lockPostFinalization assures that postFinalization process
        //only runs once per syncCollect-call.
        lockPostFinalization = false;
        if (postFinalizationProcessor != null) {
            //abort the remaining wait-time if a postFinalizationProcessor is waiting
            postFinalizationProcessor.interrupt();
        }
        waitingForFinalizers = true;
        if (finalizeWaitCount != 0) {
            if ((gcFlags & VERBOSE_COLLECT) != 0) {
                writeDebug("gc", "waiting for "+finalizeWaitCount+
                        " pending finalizers.");
                if (finalizeWaitCount < 0) {
                    //Maybe even throw exception here?
                    Py.writeError("gc", "There should never be "+
                            "less than zero pending finalizers!");
                }
            }
            // It is important to have the while  *inside* the synchronized block.
            // Otherwise the notify might come just between the check and the wait,
            // causing an endless waiting.
            synchronized(GCSentinel.class) {
                while (finalizeWaitCount != 0) {
                    try {
                        GCSentinel.class.wait();
                    } catch (InterruptedException ie2) {}
                }
            }
            if ((gcFlags & VERBOSE_COLLECT) != 0) {
                writeDebug("gc", "no more finalizers pending.");
            }
        }
        waitingForFinalizers = false;

        if (postFinalizationPending) {
            if ((gcFlags & VERBOSE_COLLECT) != 0) {
                writeDebug("gc",
                        "waiting for pending post-finalization process.");
            }
            // It is important to have the while (which is actually an "if" since the
            // InterruptedException is very unlikely to occur) *inside* the synchronized
            // block. Otherwise the notify might come just between the check and the wait,
            // causing an endless waiting. This is no pure academic consideration, but was
            // actually observed to happen from time to time, especially on faster systems.
            synchronized(PostFinalizationProcessor.class) {
                while (postFinalizationPending) {
                    try {
                        PostFinalizationProcessor.class.wait();
                    } catch (InterruptedException ie3) {}
                }
            }
            if ((gcFlags & VERBOSE_COLLECT) != 0) {
                writeDebug("gc", "post-finalization finished.");
            }
        }
        if (collectBuffer != null) {
            /* There is a little discrepancy in CPython's behavior.
             * The documentation tells that all uncollectable objects
             * are stored in gc.garbage, but it actually stores only
             * those uncollectable objects that have finalizers.
             * In contrast to that the uncollectable counting and
             * listing related to DEBUG_X-flags also counts/lists
             * objects that participate in a cycle with uncollectable
             * finalizable objects.
             * 
             * Comprehension:
             * An object is uncollectable if it is in a ref cycle and
             * has a finalizer.
             * 
             * CPython
             * 
             * - counts and prints the whole uncollectable cycle in context
             * of DEBUG_X-flags.
             * 
             * - stores only those objects from the cycle that actually have
             * finalizers in gc.garbage.
             * 
             * While slightly contradictionary to the doc, we reproduce this
             * behavior here.
             */
            if ((debugFlags & gc.DEBUG_COLLECTABLE) != 0 &&
                    (    (debugFlags & gc.DEBUG_OBJECTS) != 0 ||
                        (debugFlags & gc.DEBUG_INSTANCES) != 0)) {
                //note that all cycleMarks should have been initialized when
                //objects became monitored.

                for (WeakReferenceGC wrg: collectBuffer) {
                    if (!wrg.cycleMark.isUncollectable()) {
                        if (wrg.isInstance) {
                            writeDebug("gc", "collectable "+
                                    ((debugFlags & gc.DEBUG_INSTANCES) != 0 ?
                                    wrg.inst_str : wrg.str));
                        } else if ((debugFlags & gc.DEBUG_OBJECTS) != 0) {
                            writeDebug("gc", "collectable "+wrg.str);
                        }
                    } else {
                        ++stat[1];
                    }
                }
            } else if ((debugFlags & gc.DEBUG_STATS) != 0) {
                for (WeakReferenceGC wrg: collectBuffer) {
                    if (wrg.cycleMark.isUncollectable()) {
                        ++stat[1];
                    }
                }
            }
            if ((debugFlags & gc.DEBUG_UNCOLLECTABLE) != 0 &&
                    (    (debugFlags & gc.DEBUG_OBJECTS) != 0 ||
                        (debugFlags & gc.DEBUG_INSTANCES) != 0)) {
                for (WeakReferenceGC wrg: collectBuffer) {
                    if (wrg.cycleMark.isUncollectable()) {
                        if (wrg.isInstance) {
                            writeDebug("gc", "uncollectable "+
                                    ((debugFlags & gc.DEBUG_INSTANCES) != 0 ?
                                    wrg.inst_str : wrg.str));
                        } else if ((debugFlags & gc.DEBUG_OBJECTS) != 0) {
                            writeDebug("gc", "uncollectable "+wrg.str);
                        }
                    }
                }
            }
        }
        if ((gcFlags & VERBOSE_COLLECT) != 0) {
            writeDebug("gc", abortedCyclicFinalizers+
                    " finalizers aborted.");
        }
        stat[0] -= abortedCyclicFinalizers;
        stat[1] -= abortedCyclicFinalizers;
    }

    public static PyObject get_count() {
        throw Py.NotImplementedError("not applicable to Java GC");
    }

    public static void set_debug(int flags) {
        debugFlags = flags;
    }

    public static int get_debug() {
        return debugFlags;
    }

    public static void set_threshold(PyObject[] args, String[] kwargs) {
        throw Py.NotImplementedError("not applicable to Java GC");
    }

    public static PyObject get_threshold() {
        throw Py.NotImplementedError("not applicable to Java GC");
    }

    public static PyObject get_objects() {
        throw Py.NotImplementedError("not applicable to Java GC");
    }

    /**
     * Only works reliably if {@code monitorGlobal} is active, as it depends on
     * monitored objects to search for referrers. It only finds referrers that
     * properly implement the traverseproc mechanism (unless reflection-based
     * traversion is activated and works stable).
     * Further note that the resulting list will contain referrers in no specific
     * order and may even include duplicates.
     */
    public static PyObject get_referrers(PyObject[] args, String[] kwargs) {
        if (!isMonitoring()) {
            throw Py.NotImplementedError(
                "not applicable in Jython if gc module is not monitoring PyObjects");
        }
        if (args == null) {
            return Py.None;
        }
        PyObject result = new PyList();
        PyObject[] coll = {null, result};
        PyObject src;
        synchronized(monitoredObjects) {
            for (PyObject ob: args) {
                for (WeakReferenceGC src0: monitoredObjects) {
                    src = (PyObject) src0.get(); //Sentinels should not be in monitoredObjects
                    if (src instanceof Traverseproc) {
                        try {
                            if (((Traverseproc) src).refersDirectlyTo(ob)) {
                                result.__add__(src);
                            }
                        } catch (UnsupportedOperationException uoe) {
                            coll[0] = ob;
                            traverse(ob, ReferrerFinder.defaultInstance, coll);
                        }
                    } else if (isTraversable(src)) {
                        coll[0] = ob;
                        traverse(ob, ReferrerFinder.defaultInstance, coll);
                    }
                }
            }
        }
        return result;
    }

    /**
     * Only works reliably if all objects in args properly
     * implement the Traverseproc mechanism (unless reflection-based traversion
     * is activated and works stable).
     * Further note that the resulting list will contain referents in no
     * specific order and may even include duplicates.
     */
    public static PyObject get_referents(PyObject[] args, String[] kwargs) {
        if (args == null) {
            return Py.None;
        }
        PyObject result = new PyList();
        for (PyObject ob: args) {
            traverse(ob, ReferentsFinder.defaultInstance, result);
        }
        return result;
    }

    /**
     * {@code is_tracked} is - in Jython case - interpreted in the sense that
     * {@code gc.collect} will be able to count the object as collected if it
     * participates in a cycle. This mimics CPython behavior and passes
     * the corresponding unit test in {@code test_gc.py}.
     */
    public static PyObject is_tracked(PyObject[] args, String[] kwargs) {
        if (isTraversable(args[0]) &&
            (monitoredObjects == null || isMonitored(args[0]))) {
            return Py.True;
        } else {
            return Py.False;
        }
    }

    /**
     * Returns all objects from {@code pool} that are part of reference cycles as a new set.
     * If a reference-cycle is not entirely contained in {@code pool}, it will be entirely
     * contained in the resulting set, i.e. missing participants will be added.
     * This method completely operates on weak references to ensure that the returned
     * set does not manipulate gc-behavior.
     * 
     * Note that this method is not thread-safe. Within the gc-module it is only used
     * by the collect-method which ensures thread-safety by a synchronized block.
     */
    private static IdentityHashMap<PyObject, WeakReferenceGC>
            removeNonCyclicWeakRefs(Iterable<WeakReferenceGC> pool) {
        @SuppressWarnings("unchecked")
        IdentityHashMap<PyObject, WeakReferenceGC>[] pools = new IdentityHashMap[2];
        
        pools[0] = new IdentityHashMap<PyObject, WeakReferenceGC>();
        pools[1] = new IdentityHashMap<PyObject, WeakReferenceGC>();
        PyObject referent;
        if (monitorNonTraversable) {
            //this means there might be non-traversable objects in the pool we must filter out now
            for (WeakReferenceGC ref: pool) {
                referent = ref.get();
                if (referent != null && isTraversable(referent)) {
                    pools[0].put(referent, ref);
                }
            }
        } else {
            //this means the pool is already entirely traversable
            for (WeakReferenceGC ref: pool) {
                referent = ref.get();
                if (referent != null)
                pools[0].put(referent, ref);
            }
        }
        IdentityHashMap<PyObject, WeakReferenceGC> tmp;
        IdentityHashMap<PyObject, WeakReferenceGC> toProcess = new IdentityHashMap<>();
        //We complete pools[0] with all reachable objects.
        for (WeakReferenceGC ref: pools[0].values()) {
            traverse((PyObject) ref.get(), ReachableFinderWeakRefs.defaultInstance, pools);
        }
        while (!pools[1].isEmpty()) {
            tmp = pools[1];
            pools[1] = toProcess;
            toProcess = tmp;
            pools[0].putAll(toProcess);
            for (WeakReferenceGC ref: toProcess.values()) {
                traverse((PyObject) ref.get(), ReachableFinderWeakRefs.defaultInstance, pools);
            }
            toProcess.clear();
        }
        //pools[0] should now be a closed set in the sense that it contains all PyObjects
        //reachable from pools[0]. Now we are going to remove non-cyclic objects:
        
        boolean done = false;
        while (!done) {
            done = true;
            //After this loop pools[1] contains all objects from pools[0]
            //that some object in pools[0] points to.
            //toRemove will contain all objects from pools[0] that don't
            //point to any object in pools[0]. Removing toRemove from
            //pools[1] and repeating this procedure until nothing changes
            //any more will let only cyclic trash remain.
            for (WeakReferenceGC ref: pools[0].values()) {
                RefInListFinder.defaultInstance.found = false;
                referent = ref.get();
                traverse(referent , RefInListFinder.defaultInstance, pools);
                if (!RefInListFinder.defaultInstance.found) {
                    toProcess.put(referent, ref);
                    done = false;
                }
            }
            for (PyObject ref: toProcess.keySet()) {
                pools[1].remove(ref);
            }
            toProcess.clear();
            done = done && pools[0].size() == pools[1].size();
            tmp = pools[0];
            tmp.clear();
            pools[0] = pools[1];
            pools[1] = tmp;
        }
        return pools[0];
    }

    /**
     * Computes the set of objects reachable from {@code pool}, not necessarily
     * including {@code pool} itself; only those objects from {@code pool} that are
     * reachable from at least one other object in {@code pool} will be included
     * in the result.
     */
    private static Set<PyObject> findReachables(Iterable<PyObject> pool) {
        @SuppressWarnings("unchecked")
        IdentityHashMap<PyObject, PyObject>[] pools = new IdentityHashMap[2];

        pools[0] = new IdentityHashMap<PyObject, PyObject>();
        pools[1] = new IdentityHashMap<PyObject, PyObject>();
        IdentityHashMap<PyObject, PyObject> tmp;
        IdentityHashMap<PyObject, PyObject> toProcess = new IdentityHashMap<>();
        
        //We complete pools[0] with all reachable objects.
        //Note the difference to the implementation in removeNonCyclic.
        //There pools[0] was initialized with the contents of pool and
        //then used here as iteration source. In contrast to that we don't
        //want to have pool contained in the reachable set in any case here.
        for (PyObject obj: pool) {
            if (isTraversable(obj)) {
                traverse(obj, ReachableFinder.defaultInstance, pools);
            }
        }
        while (!pools[1].isEmpty()) {
            tmp = pools[1];
            pools[1] = toProcess;
            toProcess = tmp;
            pools[0].putAll(toProcess);
            for (PyObject obj: toProcess.keySet()) {
                traverse(obj, ReachableFinder.defaultInstance, pools);
            }
            toProcess.clear();
        }
        //pools[0] should now be a closed set in the sense that it contains all PyObjects
        //reachable from pools[0].
        return pools[0].keySet();
    }

    /**
     * Returns all objects from {@code pool} that are part of reference-cycles as a new set.
     * If a reference-cycle is not entirely contained in {@code pool}, it will be entirely
     * contained in the resulting set, i.e. missing participants will be added.
     * This method completely operates on weak references to ensure that the returned
     * set does not manipulate gc-behavior.
     * 
     * Note that this method is not thread-safe. Within the gc-module it is only used
     * by the collect-method which ensures thread-safety by a synchronized block.
     */
    private static Set<PyObject> removeNonCyclic(Iterable<PyObject> pool) {
        @SuppressWarnings("unchecked")
        IdentityHashMap<PyObject, PyObject>[] pools = new IdentityHashMap[2];
        
        pools[0] = new IdentityHashMap<PyObject, PyObject>();
        pools[1] = new IdentityHashMap<PyObject, PyObject>();
        if (monitorNonTraversable) {
            //this means there might be non-traversable objects in the pool we must filter out now
            for (PyObject obj: pool) {
                if (isTraversable(obj)) {
                    pools[0].put(obj, obj);
                }
            }
        } else {
            //this means the pool is already entirely traversable
            for (PyObject obj: pool) {
                pools[0].put(obj, obj);
            }
        }
        IdentityHashMap<PyObject, PyObject> tmp;
        IdentityHashMap<PyObject, PyObject> toProcess = new IdentityHashMap<>();
        
        //We complete pools[0] with all reachable objects.
        for (PyObject obj: pools[0].keySet()) {
            traverse(obj, ReachableFinder.defaultInstance, pools);
        }
        while (!pools[1].isEmpty()) {
            tmp = pools[1];
            pools[1] = toProcess;
            toProcess = tmp;
            pools[0].putAll(toProcess);
            for (PyObject obj: toProcess.keySet()) {
                traverse(obj, ReachableFinder.defaultInstance, pools);
            }
            toProcess.clear();
        }
        //pools[0] now is a closed set in the sense that it contains all PyObjects
        //reachable from pools[0]. Now we are going to remove non-cyclic objects:
        
        boolean done = false;
        while (!done) {
            done = true;
            // After this loop pools[1] contains all objects from pools[0]
            // that some object in pools[0] points to.
            // toRemove will contain all objects from pools[0] that don't
            // point to any object in pools[0]. Removing toRemove from
            // pools[1] and repeating this procedure until nothing changes
            // any more will let only cyclic trash remain.
            for (PyObject obj: pools[0].keySet()) {
                ObjectInListFinder.defaultInstance.found = false;
                traverse(obj, ObjectInListFinder.defaultInstance, pools);
                if (!ObjectInListFinder.defaultInstance.found) {
                    toProcess.put(obj, obj);
                    done = false;
                }
            }
            for (PyObject obj: toProcess.keySet()) {
                pools[1].remove(obj);
            }
            toProcess.clear();
            done = done && pools[0].size() == pools[1].size();
            tmp = pools[0];
            tmp.clear();
            pools[0] = pools[1];
            pools[1] = tmp;
        }
        return pools[0].keySet();
    }

    /**
     * Mark all objects that are reachable from start AND can reach start,
     * thus participate in a cycle with start.
     */
    public static void markCyclicObjects(PyObject start, boolean uncollectable) {
        Set<PyObject> search = findCyclicObjects(start);
        if (search == null) {
            return;
        }
        //Search contains the cyclic objects that participate in a cycle with start,
        //i.e. which are reachable from start AND can reach start. 
        //Mark these...
        CycleMarkAttr cm;
        for (PyObject obj: search) {
            cm = (CycleMarkAttr) JyAttribute.getAttr(obj, JyAttribute.GC_CYCLE_MARK_ATTR);
            if (cm == null) {
                cm = new CycleMarkAttr(true, uncollectable);
                JyAttribute.setAttr(obj,
                        JyAttribute.GC_CYCLE_MARK_ATTR, cm);
            } else {
                cm.setFlags(true, uncollectable);
            }
        }
    }

    /**
     * Return objects that are reachable from start AND can reach start,
     * thus participate in a cycle with start.
     * Returns {@code null} if start does not participate in any cycle.
     */
    public static Set<PyObject> findCyclicObjects(PyObject start) {
        IdentityHashMap<PyObject, PyObject> map =  findCyclicObjectsIntern(start);
        return map == null ? null : map.keySet();
    }

    private static IdentityHashMap<PyObject, PyObject> findCyclicObjectsIntern(PyObject start) {
        if (!isTraversable(start)) {
            return null;
        }
        //first determine the reachable set:
        @SuppressWarnings("unchecked")
        IdentityHashMap<PyObject, PyObject>[] reachSearch =
            (IdentityHashMap<PyObject, PyObject>[]) new IdentityHashMap[2];
        reachSearch[0] = new IdentityHashMap<PyObject, PyObject>();
        reachSearch[1] = new IdentityHashMap<PyObject, PyObject>();
        IdentityHashMap<PyObject, PyObject> tmp, search = new IdentityHashMap<PyObject, PyObject>();
        traverse(start, ReachableFinder.defaultInstance, reachSearch);
        tmp = search;
        search = reachSearch[1];
        tmp.clear();
        reachSearch[1] = tmp;
        while (!search.isEmpty()) {
            reachSearch[0].putAll(search);
            for (PyObject obj: search.keySet()) {
                traverse(obj, ReachableFinder.defaultInstance, reachSearch);
            }
            tmp = search;
            search = reachSearch[1];
            tmp.clear();
            reachSearch[1] = tmp;
        }
        //reachSearch[0] is now the reachable set, but still contains non-cyclic objects
        if (!reachSearch[0].containsKey(start)) {
            return null;
        }
        search.clear();
        search.put(start, start);
        boolean changed = true;
        while (changed) {
            changed = false;
            for (PyObject obj: reachSearch[0].keySet()) {
                if (traverse(obj, RefersToSetFinder.defaultInstance, search.keySet()) == 1) {
                    changed = true;
                    tmp.put(obj, obj);
                }
            }
            //move all objects that can reach start from reachSearch[0] to search
            search.putAll(tmp);
            for (PyObject key: tmp.keySet()) {
                reachSearch[0].remove(key);
            }
            tmp.clear();
        }
        return search;
    }

    public static int traverse(PyObject ob, Visitproc visit, Object arg) {
        int retVal;
        boolean traversed = false;
        if (ob instanceof Traverseproc) {
            retVal = ((Traverseproc) ob).traverse(visit, arg);
            traversed = true;
            if (retVal != 0) return retVal;
        }
        if (ob instanceof TraverseprocDerived) {
            retVal = ((TraverseprocDerived) ob).traverseDerived(visit, arg);
            traversed = true;
            if (retVal != 0) return retVal;
        }
        if ((gcFlags & SUPPRESS_TRAVERSE_BY_REFLECTION_WARNING) == 0) {
            if (! (ob instanceof Traverseproc || ob instanceof TraverseprocDerived ||
                    ob.getClass() == PyObject.class ||
                    ob.getClass().isAnnotationPresent(Untraversable.class)) ) {
                Py.writeWarning("gc", "The PyObject-subclass "+ob.getClass().getName()+"\n" +
                        "should either implement Traverseproc or be marked with the\n" +
                        "@Untraversable annotation. See the instructions\n" +
                        "in javadoc of org.python.core.Traverseproc.java.");
            }
        }
        if ((gcFlags & DONT_TRAVERSE_BY_REFLECTION) != 0) {
            return 0;
        }
        Class<?> cls = ob.getClass();
        if (traversed || cls == PyObject.class ||
                cls.isAnnotationPresent(Untraversable.class)) {
            return 0;
        }
        if ((gcFlags & SUPPRESS_TRAVERSE_BY_REFLECTION_WARNING) == 0) {
            Py.writeWarning("gc", "Traverse by reflection: "+ob.getClass().getName()+"\n" +
                    "This is an inefficient procedure. It is recommended to\n" +
                    "implement the traverseproc mechanism properly.");
        }
        return traverseByReflection(ob, visit, arg);
    }

    /**
     * <p>
     * This method recursively traverses fields of {@code ob}.
     * If a field is a PyObject, it is passed to {@code visit}.
     * and recursion ends in that branch.
     * If a field is an array, the elements are checked whether
     * they are PyObjects. {@code PyObject}-elements are passed to
     * {@code visit}. Elements that are arrays themselves are again
     * processed elementwise and so on.
     * </p>
     * <p>
     * Through the whole search this method fails fast if
     * {@code visit} returns non-zero.
     * </p>
     * <p>
     * Note that we intentionally don't traverse iterables by
     * iterating them. Since we perform recursion, this should reach
     * all contained references anyway - in Java every object
     * can only contain references as fields or arrays.
     * On the one hand, exploiting iterables would ease the
     * access to private fields, but on the other hand during
     * iteration they might change inner state, create
     * new (Py)Objects or obtain objects from native methods.
     * Additionally we might run into concurrent modification issues.
     * So all in all the traversal is cleaner and safer if just
     * fields and arrays are traversed.
     * </p>
     */
    public static int traverseByReflection(Object ob, Visitproc visit, Object arg) {
        IdentityHashMap<Object, Object> alreadyTraversed = new IdentityHashMap<>();
        alreadyTraversed.put(ob, ob);
        return traverseByReflectionIntern(ob, alreadyTraversed, visit, arg);
    }

    private static int traverseByReflectionIntern(Object ob,
            IdentityHashMap<Object, Object> alreadyTraversed, Visitproc visit, Object arg) {
        Class<? extends Object> cls = ob.getClass();
        int result;
        Object element;
        if (cls.isArray() && canLinkToPyObject(cls.getComponentType(), false)) {
            for (int i = 0; i < Array.getLength(ob); ++i) {
                element = Array.get(ob, i);
                if (element != null && !alreadyTraversed.containsKey(element)) {
                    alreadyTraversed.put(element, element);
                    if (element instanceof PyObject) {
                        result = visit.visit((PyObject) element, arg);
                    } else {
                        result = traverseByReflectionIntern(element,
                                alreadyTraversed, visit, arg);
                    }
                    if (result != 0) {
                        return result;
                    }
                }
            }
        } else {
            while (cls != Object.class && cls != PyObject.class) {
                Field[] declFields = cls.getDeclaredFields();
                for (int i = 0; i < declFields.length; ++i) {
                    if (!Modifier.isStatic(declFields[i].getModifiers()) &&
                            !declFields[i].getType().isPrimitive()) {
                        if (!declFields[i].isAccessible()) {
                            declFields[i].setAccessible(true);
                        }
                        if (canLinkToPyObject(declFields[i].getType(), false)) {
                            try {
                                element = declFields[i].get(ob);
                                if (!alreadyTraversed.containsKey(element)) {
                                    alreadyTraversed.put(element, element);
                                    if (element instanceof PyObject) {
                                        result = visit.visit((PyObject) element, arg);
                                    } else {
                                        result = traverseByReflectionIntern(element,
                                                alreadyTraversed, visit, arg);
                                    }
                                    if (result != 0) {
                                        return result;
                                    }
                                }
                            } catch (Exception e) {}
                        }
                    }
                }
                cls = cls.getSuperclass();
            }
        }
        return 0;
    }

    /**
     * <p>
     * This method checks via type-checking-only, whether an object
     * of the given class can in principle hold a ref to a {@code PyObject}.
     * Especially if arrays are involved, this can safe a lot performance.
     * For now, no generic-type info is exploited.
     * </p>
     * <p>
     * If {@code actual} is true, the answer will hold for an object
     * that<b> is </b>an instance of the given class.
     * Otherwise it is assumed that cls is the type of a field holding an
     * object, so cls is considered as upper bound for an objects actual
     * type.
     * </p>
     * <p>
     * One should call with {@code actual == true}, if cls was obtained
     * by {@code ob.getClass()} and with {@code actual == false}, if cls
     * was obtained as a field-type or component-type of an array.
     * </p>
     */
    public static boolean canLinkToPyObject(Class<?> cls, boolean actual) {
        // At first some quick fail fast/succeed fast-checks:
        if (quickCheckCannotLinkToPyObject(cls)) {
            return false;
        }
        if (!actual && (!Modifier.isFinal(cls.getModifiers()))) {
            return true; //a subclass could contain anything
        }
        if (quickCheckCanLinkToPyObject(cls)) {
            return true;
        }
        if (cls.isInterface() || Modifier.isAbstract(cls.getModifiers())) {
            return true;
        }
        if (cls.isArray()) {
            return canLinkToPyObject(cls.getComponentType(), false);
        }
        Class<?> cls2 = cls;
        
        // Fail fast if no fields exist in cls:
        int fieldCount = cls2.getDeclaredFields().length;
        while (fieldCount == 0 && cls2 != Object.class) {
            cls2 = cls2.getSuperclass();
            fieldCount += cls.getDeclaredFields().length;
        }
        if (fieldCount == 0) {
            return false;
        }
        IdentityHashMap<Class<?>, Class<?>> alreadyChecked = new IdentityHashMap<>();
        alreadyChecked.put(cls, cls);
        cls2 = cls;
        Class<?> ft;
        while (cls2 != Object.class) {
            for (Field f: cls2.getDeclaredFields()) {
                if (!Modifier.isStatic(f.getModifiers())) {
                    ft = f.getType();
                    if (!ft.isPrimitive() && !alreadyChecked.containsKey(ft)) {
                        alreadyChecked.put(ft, ft);
                        if (canLinkToPyObjectIntern(ft, alreadyChecked)) {
                            return true;
                        }
                    }
                }
            }
            cls2 = cls2.getSuperclass();
        }
        return false;
    }

    private static boolean quickCheckCanLinkToPyObject(Class<?> cls) {
        if (!Modifier.isFinal(cls.getModifiers())) {
            return true;
        }
        if (cls.isAssignableFrom(PyObject.class)) {
            return true;
        }
        if (PyObject.class.isAssignableFrom(cls)) {
            return true;
        }
        if (cls.isArray()) {
            return quickCheckCanLinkToPyObject(cls.getComponentType());
        }
        return false;
    }

    private static boolean quickCheckCannotLinkToPyObject(Class<?> cls) {
        if (cls.isPrimitive()) {
            return true;
        }
        if (cls == String.class || cls == Class.class ||
                cls == Field.class || cls == java.lang.reflect.Method.class) {
            return true;
        }
        if (cls.isArray()) {
            return quickCheckCannotLinkToPyObject(cls.getComponentType());
        }
        return false;
    }

    private static boolean canLinkToPyObjectIntern(Class<?> cls,
            IdentityHashMap<Class<?>, Class<?>> alreadyChecked) {
        if (quickCheckCanLinkToPyObject(cls)) {
            return true;
        }
        if (quickCheckCannotLinkToPyObject(cls)) {
            return false;
        }
        if (cls.isArray()) {
            return canLinkToPyObjectIntern(cls.getComponentType(), alreadyChecked);
        }
        Class<?> cls2 = cls;
        Class<?> ft;
        while (cls2 != Object.class) {
            for (Field f: cls2.getDeclaredFields()) {
                if (!Modifier.isStatic(f.getModifiers())) {
                    ft = f.getType();
                    if (!ft.isPrimitive() && !alreadyChecked.containsKey(ft)) {
                        alreadyChecked.put(ft, ft);
                        if (canLinkToPyObjectIntern(ft, alreadyChecked)) {
                            return true;
                        }
                    }
                }
            }
            cls2 = cls2.getSuperclass();
        }
        return false;
    }

    public static boolean isTraversable(PyObject ob) {
        if (ob == null) {
            return false;
        }
        if (ob instanceof Traverseproc || ob instanceof TraverseprocDerived) {
            return true;
        }
        if ((gcFlags & DONT_TRAVERSE_BY_REFLECTION) != 0) {
            return false;
        }
        Class<?> cls = ob.getClass();
        return !(cls == PyObject.class ||
                cls.isAnnotationPresent(Untraversable.class));
    }

    //----------Visitproc section----------------------------------------------

    static class ReferentsFinder implements Visitproc {
        public static ReferentsFinder defaultInstance = new ReferentsFinder();

        /**
         * Expects arg to be a list-like {@code PyObject} where the
         * referents will be inserted.
         */
        public int visit(PyObject object, Object arg) {
            ((org.python.core.PySequenceList) arg).pyadd(object);
            return 0;
        }
    }

    /**
     * Helper to find the reachable set of an object. {@code arg} must be a
     * 2-component array of type {@code IdentityHashMap<PyObject, PyObject>[]}.
     * Although these are maps, the components of {@code arg} are conceptually
     * used as sets. {@code arg[0]} shall contain all objects already known to
     * be reachable. The visitproc adds all newly discovered objects to
     * {@code arg[1]}, so the user can later add these to the reachable set and
     * knows they need to be explored further. Only traversable objects are
     * considered by this visitproc.
     */
    static class ReachableFinder implements Visitproc {
        public static ReachableFinder defaultInstance = new ReachableFinder();

        /**
         * Expects arg to be a list-like {@code PyObject} where the
         * referents will be inserted.
         */
        @SuppressWarnings("unchecked")
        public int visit(PyObject object, Object arg) {
            IdentityHashMap<PyObject, PyObject>[] reachSearch =
                (IdentityHashMap<PyObject, PyObject>[]) arg;
            if ((isTraversable(object)) &&
                !reachSearch[0].containsKey(object)) {
                reachSearch[1].put(object, object);
            }
            return 0;
        }
    }

    static class ReachableFinderWeakRefs implements Visitproc {
        public static ReachableFinderWeakRefs defaultInstance = new ReachableFinderWeakRefs();

        @SuppressWarnings("unchecked")
        public int visit(PyObject object, Object arg) {
            if (isTraversable(object)) {
                IdentityHashMap<PyObject, WeakReferenceGC>[] pools = 
                        (IdentityHashMap<PyObject, WeakReferenceGC>[]) arg;
                WeakReferenceGC ref = pools[0].get(object);
                if (ref == null) {
                    ref = new WeakReferenceGC(object);
                    pools[1].put(object, ref);
                }
            }
            return 0;
        }
    }

    static class ReferrerFinder implements Visitproc {
        public static ReferrerFinder defaultInstance = new ReferrerFinder();

        /**
         * Expects {@code arg} to be a 2-component array ({@code PyObject[]})
         * consisting of the {@code PyObject} to be referred to at
         * {@code arg[0]} and the destination list (a list-like {@code PyObject}
         * where the referrers will be inserted) at {@code arg[1]}.
         */
        public int visit(PyObject object, Object arg) {
            if (((PyObject[]) arg)[0].__eq__(object).__nonzero__()) {
                ((org.python.core.PySequenceList) ((PyObject[]) arg)[1]).pyadd(object);
            }
            return 0;
        }
    }

    /**
     * Like {@code RefInListFinder} this visitproc looks whether the traversed object
     * refers to one of the objects in a given set. Here we perform fail-fast
     * behavior. This method is useful if one is not interested in the referrers,
     * but only wants to know (quickly) whether a connection exists or not.
     */
    static class RefersToSetFinder implements Visitproc {
        public static RefersToSetFinder defaultInstance = new RefersToSetFinder();

        @SuppressWarnings("unchecked")
        public int visit(PyObject object, Object arg) {
            return ((Set<PyObject>) arg).contains(object) ? 1 : 0;
        }
    }

    /**
     * This visitproc looks whether an object refers to one of the objects in
     * a given set.<br>
     * {@code arg} must be a 2-component-array of
     * {@code HashMap<Object, WeakReferenceGC>}.
     * These maps are actually used as sets, but resolve the strongref/weakref
     * views to the objects.<br>
     * {@code arg[0]} is the pool we search referrers for. When the traverse method
     * iterates through the referents of a source object, this visitproc checks
     * for each referent, whether it is in {@code arg[0]}. If it is, then it is added
     * to {@code arg[1]} (no double entries here since it is a set-like structure)
     * and {@code found} is set to {@code true}.<br>
     * By repeated use one can collect all objects referring to a given set
     * of objects in another set.
     */
    static class RefInListFinder implements Visitproc {
        public static RefInListFinder defaultInstance = new RefInListFinder();
        public boolean found = false;

        /**
         * Expects {@code arg} to be a 2-component array of
         * {@link java.util.Map}s.
         */
        public int visit(PyObject object, Object arg) {
            @SuppressWarnings("unchecked")
            IdentityHashMap<PyObject, WeakReferenceGC>[] pools =
                    (IdentityHashMap<PyObject, WeakReferenceGC>[]) arg;
            WeakReferenceGC ref = pools[0].get(object);
            if (ref != null) {
                pools[1].put(object, ref);
                found = true;
            }
            return 0;
        }
    }

    static class ObjectInListFinder implements Visitproc {
        public static ObjectInListFinder defaultInstance = new ObjectInListFinder();
        public boolean found = false;

        /**
         * Expects {@code arg} to be a 2-component array of
         * {@link java.util.Map}s.
         */
        public int visit(PyObject object, Object arg) {
            @SuppressWarnings("unchecked")
            IdentityHashMap<PyObject, PyObject>[] pools =
                (IdentityHashMap<PyObject, PyObject>[]) arg;
            if (pools[0].containsKey(object)) {
                pools[1].put(object, object);
                found = true;
            }
            return 0;
        }
    }
    //----------end of Visitproc section---------------------------------------
}
