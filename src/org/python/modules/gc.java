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
import org.python.core.PyException;
import org.python.core.PyList;
import org.python.core.PyObject;
import org.python.core.PyInstance;
import org.python.core.PyString;
import org.python.core.Traverseproc;
import org.python.core.TraverseprocDerived;
import org.python.core.Visitproc;
import org.python.core.Untraversable;
import org.python.core.finalization.FinalizeTrigger;
import org.python.modules._weakref.GlobalRef;
import org.python.modules._weakref.ReferenceBackend;

//These imports belong to the out commented section on MXBean-based
//gc sync far below. That section is kept to document this failed
//approach and allow easy reproduction of this failure.
//import java.lang.management.*;
//import javax.management.*;
//import javax.management.openmbean.*;

/**
 * In Jython, the gc module notably differs from that in CPython. This comes from the different ways
 * Jython and CPython perform garbage collection. While CPython's garbage collection is based on
 * <a href="http://en.wikipedia.org/wiki/Reference_counting" target="_blank"> reference
 * counting</a>, Jython is backed by Java's gc, which is based on a
 * <a href="http://en.wikipedia.org/wiki/Tracing_garbage_collection" target="_blank"> mark-and-sweep
 * approach</a>.
 * <p>
 * This difference becomes most notable if finalizers are involved that perform resurrection. While
 * the resurrected object itself behaves rather similar between Jython and CPython, things are more
 * delicate with objects that are reachable (i.e. strongly referenced) via the resurrected object
 * exclusively. While in CPython such objects do not get their finalizers called, Jython/Java would
 * call all their finalizers. That is because Java detects the whole unreachable subgraph as garbage
 * and thus calls all their finalizers without any chance of direct intervention. CPython instead
 * detects the unreachable object and calls its finalizer, which makes the object reachable again.
 * Then all other objects are reachable from it and CPython does not treat them as garbage and does
 * not call their finalizers at all. This further means that in Jython weak references to such
 * indirectly resurrected objects break, while these persist in CPython.
 * <p>
 * As of Jython 2.7, the gc module offers some options to emulate CPython behavior. Especially see
 * the flags {@link #PRESERVE_WEAKREFS_ON_RESURRECTION}, {@link #DONT_FINALIZE_RESURRECTED_OBJECTS}
 * and {@link #DONT_FINALIZE_CYCLIC_GARBAGE} for this.
 * <p>
 * Another difference is that CPython's gc module offers some debug features like counting of
 * collected cyclic trash, which are hard to support by Jython. As of Jython 2.7 the introduction of
 * a traverseproc mechanism (c.f. {@link org.python.core.Traverseproc}) made support of these
 * features feasible. As support of these features comes with a significant emulation cost, one must
 * explicitly tell gc to perform this. To make objects subject to cyclic trash counting, these
 * objects must be gc-monitored in Jython. See {@link #monitorObject(PyObject)},
 * {@link #unmonitorObject(PyObject)}, {@link #MONITOR_GLOBAL} and {@link #stopMonitoring()} for
 * this.
 * <p>
 * If at least one object is gc-monitored, {@link #collect()} works synchronously in the sense that
 * it blocks until all gc-monitored objects that are garbage actually have been collected and had
 * their finalizers called and completed. {@link #collect()} will report the number of collected
 * objects in the same manner as in CPython, i.e. counts only those that participate in reference
 * cycles. This allows a unified test implementation across Jython and CPython (which applies to
 * most tests in test_gc.py). If not any object is gc-monitored, {@link #collect()} just delegates
 * to {@link java.lang.System#gc()}, runs asynchronously (i.e. non-blocking) and returns
 * {@link #UNKNOWN_COUNT}. See also {@link #DEBUG_SAVEALL} for a useful gc debugging feature that is
 * supported by Jython from version 2.7 onwards.
 * <p>
 * Implementing all these features in Jython involved a lot of synchronization logic. While care was
 * taken to implement this without using timeouts as far as possible and rely on locks, states and
 * system/hardware independent synchronization techniques, this was not entirely feasible.<br>
 * The aspects that were only feasible using a timeout are waiting for gc to enqueue all collected
 * objects (i.e. weak references to monitored objects that were gc'ed) to the reference queue and
 * waiting for gc to run all PyObject finalizers.
 * <p>
 * Waiting for trash could in theory be strictly synchronized by using {@code MXBean}s, i.e.
 * <a href=
 * "https://docs.oracle.com/javase/7/docs/jre/api/management/extension/index.html?com/sun/management/GcInfo.html"
 * target="_blank">GarbageCollectionNotificationInfo</a> and related API. However, experiments
 * showed that the arising gc notifications do not reliably indicate when enqueuing was done for a
 * specific gc run. We kept the experimental implementation in source code comments to allow easy
 * reproducibility of this issue. (Note that out commented code contradicts Jython styleguide, but
 * this one - however - is needed to document this infeasible approach and is explicitly declared
 * accordingly).
 * <p>
 * But how <b>is</b> sync done now? We insert a sentinel before running gc and wait until this
 * sentinel was collected. Timestamps are taken to give us an idea at which time scales the gc of
 * the current JVM performs. We then wait until twice the measured time (i.e. duration from call to
 * {@link java.lang.System#gc()} until the sentinel reference was enqueued) has passed after the
 * last reference was enqueued by gc. While this approach is not entirely safe in theory, it passes
 * all tests on various systems and machines we had available for testing so far. We consider it
 * more robust than a fixed-length timeout and regard it the best known feasible compromise to
 * emulate synchronous gc runs in Java.
 * <p>
 * The other timing-based synchronization issue - waiting for finalizers to run - is solved as
 * follows. Since PyObject finalizers are based on
 * {@link org.python.core.finalization.FinalizeTrigger}s, Jython has full control about these
 * finalization process from a central point. Before such a finalizer runs, it calls
 * {@link #notifyPreFinalization()} and when it is done, it calls {@link #notifyPostFinalization()}.
 * While processing of a finalizer can be of arbitrary duration, it widely holds that Java's gc
 * thread calls the next finalizer almost instantaneously after the former. That means that a
 * timestamp taken in {@link #notifyPreFinalization()} is usually delayed only few milliseconds -
 * often even reported as 0 milliseconds - after the last taken timestamp in
 * {@link #notifyPostFinalization()} (i.e. that was called by the previous finalizer). Jython's gc
 * module assumes the end of Java's finalization process if {@link #postFinalizationTimeOut}
 * milliseconds passed after a call of {@link #notifyPostFinalization()} without another call to
 * {@link #notifyPreFinalization()} in that time. The default value of
 * {@link #postFinalizationTimeOut} is {@code 100}, which is far larger than the usual almost-zero
 * duration between finalizer calls.<br>
 * This process can be disturbed by third-party finalizers of non-PyObjects brought into the process
 * by external libraries. If these finalizers are of short duration (which applies to typical
 * finalizers), one can deal with this by adjusting {@link #postFinalizationTimeOut}, which was
 * declared {@code public} for exactly this purpose. However if the external framework causing the
 * issue is Jython aware, a cleaner solution would be to let its finalizers call
 * {@link #notifyPreFinalization()} and {@link #notifyPostFinalization()} appropriately. In that
 * case these finalizers must not terminate by throwing an exception before
 * {@link #notifyPostFinalization()} was called. This is a strict requirement, since a deadlock can
 * be caused otherwise.
 * <p>
 * Note that the management API (c.f. <a href=
 * "https://docs.oracle.com/javase/7/docs/jre/api/management/extension/index.html?com/sun/management/GcInfo.html"
 * target="_blank">com.sun.management.GarbageCollectionNotificationInfo</a>) does not emit any
 * notifications that allow to detect the end of the finalization phase. So this API provides no
 * alternative to the described technique.
 * <p>
 * Usually Java's gc provides hardly any guarantee about its collection and finalization process. It
 * not even guarantees that finalizers are called at all (c.f.
 * <a href="http://howtodoinjava.com/2012/10/31/why-not-to-use-finalize-method-in-java" target=
 * "_blank">http://howtodoinjava.com/2012/10/31/why-not-to-use-finalize-method-in-java</a>). While
 * at least the most common JVM implementations usually <b>do</b> call finalizers reliably under
 * normal conditions, there still is no specific finalization order guaranteed (one might reasonably
 * expect that this would be related to reference connection graph topology, but this appears not to
 * be the case). However Jython now offers some functionality to compensate this situation. Via
 * {@link #registerPreFinalizationProcess(Runnable)} and
 * {@link #registerPostFinalizationProcess(Runnable)} and related methods one can now listen to
 * beginning and end of the finalization process. Note that this functionality relies on the
 * technique described in the former paragraph (i.e. based on calls to
 * {@link #notifyPreFinalization()} and {@link #notifyPostFinalization()}) and thus underlies its
 * unsafety, if third-party finalizers are involved. Such finalizers can cause false-positive runs
 * of registered (pre/post) finalization processes, so this feature should be used with some care.
 * It is recommended to use it only in such a way that false-positive runs would not cause serious
 * harm, but only some loss in performance or so.
 */
public class gc {
    /**
     * A constant that can occur as result of {@link #collect()} and
     * indicates an unknown number of collected cyclic trash.
     * It is intentionally not valued -1 as that value is
     * reserved to indicate an error.
     */
    public static final int UNKNOWN_COUNT = -2;

    /* Jython-specific gc flags: */
    /**
     * This flag tells every newly created PyObject to register for
     * gc monitoring. This allows {@link #collect()} to report the
     * number of collected objects.
     *
     * @see #setJythonGCFlags(short)
     * @see #getJythonGCFlags()
     * @see #addJythonGCFlags(short)
     * @see #removeJythonGCFlags(short)
     */
    public static final short MONITOR_GLOBAL =                    (1<<0);

    /**
     * CPython prior to 3.4 does not finalize cyclic garbage
     * PyObjects, while Jython does this by default. This flag
     * tells Jython's gc to mimic CPython &lt;3.4 behavior (i.e.
     * add such objects to {@code gc.garbage} list instead).
     *
     * @see #setJythonGCFlags(short)
     * @see #getJythonGCFlags()
     * @see #addJythonGCFlags(short)
     * @see #removeJythonGCFlags(short)
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
     *
     * @see #setJythonGCFlags(short)
     * @see #getJythonGCFlags()
     * @see #addJythonGCFlags(short)
     * @see #removeJythonGCFlags(short)
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
     * for several gc cycles. Its main intention is for debugging
     * resurrection-sensitive code.
     *
     * @see #setJythonGCFlags(short)
     * @see #getJythonGCFlags()
     * @see #addJythonGCFlags(short)
     * @see #removeJythonGCFlags(short)
     */
    public static final short DONT_FINALIZE_RESURRECTED_OBJECTS = (1<<3);

    public static final short FORCE_DELAYED_FINALIZATION = (1<<4);
    public static final short FORCE_DELAYED_WEAKREF_CALLBACKS = (1<<5);

    /**
     * <p>
     * Reflection-based traversal is an inefficient fallback method to
     * traverse PyObject subtypes that don't implement
     * {@link org.python.core.Traverseproc} and
     * are not marked as {@link org.python.core.Untraversable}.
     * Such a situation indicates that the programmer was not aware of
     * Jython's traverseproc mechanism and reflection is used to
     * compensate this.
     * </p>
     * <p>
     * This flag allows to inhibit reflection-based traversal. If it is
     * activated, objects that don't implement
     * {@link org.python.core.Traverseproc}
     * are always treated as if they were marked as
     * {@link org.python.core.Untraversable}.
     * </p>
     * <p>
     * Note that reflection-based traversal fallback is performed by
     * default. Further note that Jython emits warning messages if
     * reflection-based traversal occurs or if an object is encountered
     * that neither implements {@link org.python.core.Traverseproc}
     * nor is marked as {@link org.python.core.Untraversable} (even if
     * reflection-based traversal is inhibited). See
     * {@link #SUPPRESS_TRAVERSE_BY_REFLECTION_WARNING} and
     * {@link #INSTANCE_TRAVERSE_BY_REFLECTION_WARNING} to control
     * these warning messages.
     * </p>
     *
     * @see #setJythonGCFlags(short)
     * @see #getJythonGCFlags()
     * @see #addJythonGCFlags(short)
     * @see #removeJythonGCFlags(short)
     * @see #SUPPRESS_TRAVERSE_BY_REFLECTION_WARNING
     * @see #INSTANCE_TRAVERSE_BY_REFLECTION_WARNING
     */
    public static final short DONT_TRAVERSE_BY_REFLECTION =       (1<<6);

    /**
     * <p>
     * If this flag is not set, gc warns whenever an object would be subject to
     * reflection-based traversal.
     * Note that if this flag is not set, the warning will occur even if
     * reflection-based traversal is not active. The purpose of this behavior is
     * to identify objects that don't properly support the traverseproc mechanism,
     * i.e. instances of PyObject subclasses that neither implement
     * {@link org.python.core.Traverseproc},
     * nor are annotated with the {@link org.python.core.Untraversable} annotation.
     * </p>
     * <p>
     * A SUPPRESS flag was chosen rather than a WARN flag, so that warning is the
     * default behavior - the user must actively set this flag in order to not to
     * be warned.
     * This is because in an ideal implementation reflection-based traversal never
     * occurs; it is only an inefficient fallback.
     * </p>
     *
     * @see #setJythonGCFlags(short)
     * @see #getJythonGCFlags()
     * @see #addJythonGCFlags(short)
     * @see #removeJythonGCFlags(short)
     * @see #INSTANCE_TRAVERSE_BY_REFLECTION_WARNING
     */
    public static final short SUPPRESS_TRAVERSE_BY_REFLECTION_WARNING =    (1<<7);

    /**
     * Makes gc emit reflection-based traversal warning for every traversed
     * object instead of only once per class.
     * A potential reflection-based traversal occurs whenever an object is
     * traversed that neither implements {@link org.python.core.Traverseproc},
     * nor is annotated with the {@link org.python.core.Untraversable} annotation.
     *
     * @see #setJythonGCFlags(short)
     * @see #getJythonGCFlags()
     * @see #addJythonGCFlags(short)
     * @see #removeJythonGCFlags(short)
     * @see #SUPPRESS_TRAVERSE_BY_REFLECTION_WARNING
     */
    public static final short INSTANCE_TRAVERSE_BY_REFLECTION_WARNING =    (1<<8);

    /**
     * In Jython one usually uses {@code Py.writeDebug} for debugging output.
     * However that method is only verbose if an appropriate verbose level
     * was set. In CPython it is enough to set gc {@code DEBUG} flags to get
     * gc messages, no matter what overall verbose level is selected.
     * This flag tells Jython to use {@code Py.writeDebug} for debugging output.
     * If it is not set (default case), gc debugging output (if gc {@code VERBOSE}
     * or {@code DEBUG} flags are set) is directly written to {@code System.err}.
     *
     * @see #setJythonGCFlags(short)
     * @see #getJythonGCFlags()
     * @see #addJythonGCFlags(short)
     * @see #removeJythonGCFlags(short)
     */
    public static final short USE_PY_WRITE_DEBUG = (1<<9);

    /**
     * Enables collection-related verbose output.
     *
     * @see #setJythonGCFlags(short)
     * @see #getJythonGCFlags()
     * @see #addJythonGCFlags(short)
     * @see #removeJythonGCFlags(short)
     */
    public static final short VERBOSE_COLLECT =  (1<<10);

    /**
     * Enables weakref-related verbose output.
     *
     * @see #setJythonGCFlags(short)
     * @see #getJythonGCFlags()
     * @see #addJythonGCFlags(short)
     * @see #removeJythonGCFlags(short)
     */
    public static final short VERBOSE_WEAKREF =  (1<<11);

    /**
     * Enables delayed finalization related verbose output.
     *
     * @see #setJythonGCFlags(short)
     * @see #getJythonGCFlags()
     * @see #addJythonGCFlags(short)
     * @see #removeJythonGCFlags(short)
     */
    public static final short VERBOSE_DELAYED =  (1<<12);

    /**
     * Enables finalization-related verbose output.
     *
     * @see #setJythonGCFlags(short)
     * @see #getJythonGCFlags()
     * @see #addJythonGCFlags(short)
     * @see #removeJythonGCFlags(short)
     */
    public static final short VERBOSE_FINALIZE = (1<<13);

    /**
     * Bit combination of the flags {@link #VERBOSE_COLLECT},
     * {@link #VERBOSE_WEAKREF}, {@link #VERBOSE_DELAYED},
     * {@link #VERBOSE_FINALIZE}.
     *
     * @see #setJythonGCFlags(short)
     * @see #getJythonGCFlags()
     * @see #addJythonGCFlags(short)
     * @see #removeJythonGCFlags(short)
     */
    public static final short VERBOSE =
            VERBOSE_COLLECT | VERBOSE_WEAKREF | VERBOSE_DELAYED | VERBOSE_FINALIZE;

    /* set for debugging information */
    /**
     * print collection statistics
     * (in Jython scoped on monitored objects)
     *
     * @see #set_debug(int)
     * @see #get_debug()
     */
    public static final int DEBUG_STATS         = (1<<0);

    /**
     * print collectable objects
     * (in Jython scoped on monitored objects)
     *
     * @see #set_debug(int)
     * @see #get_debug()
     */
    public static final int DEBUG_COLLECTABLE   = (1<<1);

    /**
     * print uncollectable objects
     * (in Jython scoped on monitored objects)
     *
     * @see #set_debug(int)
     * @see #get_debug()
     */
    public static final int DEBUG_UNCOLLECTABLE = (1<<2);

    /**
     * print instances
     * (in Jython scoped on monitored objects)
     *
     * @see #set_debug(int)
     * @see #get_debug()
     */
    public static final int DEBUG_INSTANCES     = (1<<3);

    /**
     * print other objects
     * (in Jython scoped on monitored objects)
     *
     * @see #set_debug(int)
     * @see #get_debug()
     */
    public static final int DEBUG_OBJECTS       = (1<<4);

    /**
     * save all garbage in gc.garbage
     * (in Jython scoped on monitored objects)
     *
     * @see #set_debug(int)
     * @see #get_debug()
     */
    public static final int DEBUG_SAVEALL       = (1<<5);

    /**
     * Bit combination of the flags {@link #DEBUG_COLLECTABLE},
     * {@link #DEBUG_UNCOLLECTABLE}, {@link #DEBUG_INSTANCES},
     * {@link #DEBUG_OBJECTS}, {@link #DEBUG_SAVEALL}.
     *
     * @see #set_debug(int)
     * @see #get_debug()
     */
    public static final int DEBUG_LEAK = DEBUG_COLLECTABLE |
                                         DEBUG_UNCOLLECTABLE |
                                         DEBUG_INSTANCES |
                                         DEBUG_OBJECTS |
                                         DEBUG_SAVEALL;

    private static short gcFlags = DONT_TRAVERSE_BY_REFLECTION;
    private static int debugFlags = 0;
    private static boolean monitorNonTraversable = false;
    private static boolean waitingForFinalizers = false;
    private static final AtomicBoolean gcRunning = new AtomicBoolean(false);
    private static final Set<WeakReferenceGC> monitoredObjects = new HashSet<>();
    private static HashSet<Class<? extends PyObject>> reflectionWarnedClasses;
    private static ReferenceQueue<Object> gcTrash;
    private static int finalizeWaitCount = 0;
    private static int initWaitTime = 10, defaultWaitFactor = 2;
    private static long lastRemoveTimeStamp = -1, maxWaitTime = initWaitTime;
    private static int gcMonitoredRunCount = 0;
    public static long gcRecallTime = 4000;

    /**
     * list of uncollectable objects
     */
    public static PyList garbage = new PyList();

    /* Finalization preprocess/postprocess-related declarations: */
    private static final List<Runnable> preFinalizationProcess = new ArrayList<>();
    private static final List<Runnable> postFinalizationProcess = new ArrayList<>();
    private static final List<Runnable> preFinalizationProcessRemove = new ArrayList<>();
    private static final List<Runnable> postFinalizationProcessRemove = new ArrayList<>();
    private static Thread postFinalizationProcessor;
    public static long postFinalizationTimeOut = 100;
    private static long postFinalizationTimestamp = System.currentTimeMillis()-2*postFinalizationTimeOut;
    private static int openFinalizeCount = 0;
    private static boolean postFinalizationPending = false;
    private static boolean lockPostFinalization = false;

    /* Resurrection-safe finalizer- and weakref-related declarations: */
    private static IdentityHashMap<PyObject, PyObject> delayedFinalizables, resurrectionCriticals;
    private static int abortedCyclicFinalizers = 0;
    /* Some modes to control aspects of delayed finalization: */
    private static final byte DO_NOTHING_SPECIAL = 0;
    private static final byte MARK_REACHABLE_CRITICALS = 1;
    private static final byte NOTIFY_FOR_RERUN = 2;
    private static byte delayedFinalizationMode = DO_NOTHING_SPECIAL;
    private static boolean notifyRerun = false;

    public static final String __doc__ =
            "This module provides access to the garbage collector for reference cycles.\n" +
            "\n" +
            "enable() -- Enable automatic garbage collection (does nothing in Jython).\n" +
            "disable() -- Disable automatic garbage collection (raises NotImplementedError in Jython).\n" +
            "isenabled() -- Returns True because Java garbage collection cannot be disabled.\n" +
            "collect() -- Do a full collection right now (potentially expensive).\n" +
            "get_count() -- Return the current collection counts (raises NotImplementedError in Jython).\n" +
            "set_debug() -- Set debugging flags.\n" +
            "get_debug() -- Get debugging flags.\n" +
            "set_threshold() -- Set the collection thresholds (raise NotImplementedError in Jython).\n" +
            "get_threshold() -- Return the current the collection thresholds (raise NotImplementedError in Jython).\n" +
            "get_objects() -- Return a list of all objects tracked by the collector (raises NotImplementedError in Jython).\n" +
            "is_tracked() -- Returns true if a given object is tracked (i.e. monitored in Jython).\n" +
            "get_referrers() -- Return the list of objects that refer to an object (only finds monitored referrers in Jython).\n" +
            "get_referents() -- Return the list of objects that an object refers to.\n";

    public static final String __name__ = "gc";

    public static final PyString __doc__enable = new PyString(
            "enable() -> None\n" +
            "\n" +
            "Enable automatic garbage collection.\n" +
            "(does nothing in Jython)\n");

    public static final PyString __doc__disable = new PyString(
            "disable() -> None\n" +
            "\n" +
            "Disable automatic garbage collection.\n" +
            "(raises NotImplementedError in Jython)\n");

    public static final PyString __doc__isenabled = new PyString(
            "isenabled() -> status\n" +
            "\n" +
            "Returns true if automatic garbage collection is enabled.\n");

    public static final PyString __doc__collect = new PyString(
            "collect([generation]) -> n\n" +
            "\n" +
            "With no arguments, run a full collection.  The optional argument\n" +
            "may be an integer specifying which generation to collect.  A ValueError\n" +
            "is raised if the generation number is invalid.\n\n" +
            "The number of unreachable objects is returned.\n" +
            "(Jython emulates CPython cyclic trash counting if objects are monitored.\n" +
            "If no objects are monitored, returns -2\n");

    public static final PyString __doc__get_count = new PyString(
            "get_count() -> (count0, count1, count2)\n" +
            "\n" +
            "Return the current collection counts\n" +
            "(raises NotImplementedError in Jython)\n");

    public static final PyString __doc__set_debug = new PyString(
            "set_debug(flags) -> None\n" +
            "\n" +
            "Set the garbage collection debugging flags. Debugging information is\n" +
            "written to sys.stderr.\n" +
            "\n" +
            "flags is an integer and can have the following bits turned on:\n" +
            "\n" +
            "  DEBUG_STATS - Print statistics during collection.\n" +
            "  DEBUG_COLLECTABLE - Print collectable objects found.\n" +
            "  DEBUG_UNCOLLECTABLE - Print unreachable but uncollectable objects found.\n" +
            "  DEBUG_INSTANCES - Print instance objects.\n" +
            "  DEBUG_OBJECTS - Print objects other than instances.\n" +
            "  DEBUG_SAVEALL - Save objects to gc.garbage rather than freeing them.\n" +
            "  DEBUG_LEAK - Debug leaking programs (everything but STATS).\n");

    public static final PyString __doc__get_debug = new PyString(
            "get_debug() -> flags\n" +
            "\n" +
            "Get the garbage collection debugging flags.\n");

    public static final PyString __doc__set_thresh = new PyString(
            "set_threshold(threshold0, [threshold1, threshold2]) -> None\n" +
            "\n" +
            "Sets the collection thresholds.  Setting threshold0 to zero disables\n" +
            "collection.\n" +
            "(raises NotImplementedError in Jython)\n");

    public static final PyString __doc__get_thresh = new PyString(
            "get_threshold() -> (threshold0, threshold1, threshold2)\n" +
            "\n" +
            "Return the current collection thresholds\n" +
            "(raises NotImplementedError in Jython)\n");

    public static final PyString __doc__get_objects = new PyString(
            "get_objects() -> [...]\n" +
            "\n" +
            "Return a list of objects tracked by the collector (excluding the list\n" +
            "returned).\n" +
            "(raises NotImplementedError in Jython)\n");

    public static final PyString __doc__is_tracked = new PyString(
            "is_tracked(obj) -> bool\n" +
            "\n" +
            "Returns true if the object is tracked by the garbage collector.\n" +
            "(i.e. monitored in Jython)\n");

    public static final PyString __doc__get_referrers = new PyString(
            "get_referrers(*objs) -> list\n" +
            "Return the list of objects that directly refer to any of objs.\n" +
            "(only finds monitored referrers in Jython)");

    public static final PyString __doc__get_referents = new PyString(
            "get_referents(*objs) -> list\n" +
            "Return the list of objects that are directly referred to by objs.");


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
        int hashCode = 0;
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

        @Override
        public String toString() {
            return str;
        }

        @Override
        public int hashCode() {
            return hashCode;
        }

        @Override
        public boolean equals(Object ob) {
            Object ownReferent = get();
            if (ob instanceof WeakReferenceGC) {
                Object otherReferent = ((WeakReferenceGC) ob).get();
                if (ownReferent == null || otherReferent == null) {
                    return ownReferent == otherReferent &&
                /* We compare the cached hash codes in order to get an idea
                 * whether in the both-null-case the referent was equal once.
                 */
                            hashCode == ((WeakReferenceGC) ob).hashCode;
                } else {
                    return otherReferent.equals(ownReferent)
                /* Here the hash codes are only compared as a consistency check. */
                            && ((WeakReferenceGC) ob).hashCode == hashCode;
                }
            } else if (ob instanceof WeakrefGCCompareDummy) {
                if (ownReferent == null ||
                        ((WeakrefGCCompareDummy) ob).compare == null) {
                    return ownReferent ==
                            ((WeakrefGCCompareDummy) ob).compare &&
                /* We compare the cached hash codes in order to get an idea
                 * whether in the both-null-case the referent was equal once.
                 */
                            hashCode == ((WeakrefGCCompareDummy) ob).hashCode;
                } else {
                    return ownReferent.equals(((WeakrefGCCompareDummy) ob).compare)
                /* Here the hash codes are only compared as a consistency check. */
                            && hashCode == ((WeakrefGCCompareDummy) ob).hashCode;
                }
            } else {
                return false;
            }
        }
    }

    private static class WeakrefGCCompareDummy {
        public static WeakrefGCCompareDummy defaultInstance =
                new WeakrefGCCompareDummy();
        protected PyObject compare;
        int hashCode = 0;

        public void setCompare(PyObject compare) {
            this.compare = compare;
            hashCode = System.identityHashCode(compare);
        }

        public void clearCompare() {
            compare = null;
            hashCode = 0;
        }

        @Override
        public int hashCode() {
            return hashCode;
        }

        @Override
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

        @Override
        protected void finalize() throws Throwable {
            notifyPreFinalization();
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
            notifyPostFinalization();
        }
    }

    /**
     * Works like {@link org.python.core.Py#writeDebug(String, String)},
     * but prints to {@link org.python.core.Py#writeDebug(String, String)}
     * (i.e. subject to Jython's verbose level)
     * or directly to {@code System.err}, according to
     * {@link #USE_PY_WRITE_DEBUG}.
     *
     * @see #USE_PY_WRITE_DEBUG
     * @see org.python.core.Py#writeDebug(String, String)
     */
    public static void writeDebug(String type, String msg) {
        if ((gcFlags & USE_PY_WRITE_DEBUG) != 0) {
            Py.writeDebug(type, msg);
        } else {
            System.err.println(type + ": " + msg);
        }
    }

//--------------delayed finalization section-----------------------------------

    /**
     * In addition to what
     * {@link org.python.core.finalization.FinalizeTrigger#ensureFinalizer(PyObject)}
     * does, this method also restores the finalizer's
     * {@link org.python.core.finalization.FinalizeTrigger}'s flags by taking the
     * values from the former finalizer. On the other hand - in contrast to
     * {@link org.python.core.finalization.FinalizeTrigger#ensureFinalizer(PyObject)} -
     * this method would not create a {@link org.python.core.finalization.FinalizeTrigger}
     * for an object that did not have one before (i.e. the method checks for an old
     * (dead) trigger before it creates a new one. <br><br>
     * If a new finalizer is needed due to an
     * ordinary resurrection (i.e. the object's finalizer was called),
     * {@link org.python.core.finalization.FinalizeTrigger#ensureFinalizer(PyObject)}
     * is the right choice. If a finalization was vetoed in context of delayed
     * finalization (i.e. a resurrection that pretends not to be one and didn't run
     * the finalizer), this method is the better choice as it helps to make the new
     * {@link org.python.core.finalization.FinalizeTrigger} look exactly like the
     * old one regarding flags etc.
     * E.g. this method is called by {@link #abortDelayedFinalization(PyObject)}.
     *
     * @see #abortDelayedFinalization(PyObject)
     */
    public static void restoreFinalizer(PyObject obj) {
        FinalizeTrigger ft =
                (FinalizeTrigger) JyAttribute.getAttr(obj, JyAttribute.FINALIZE_TRIGGER_ATTR);
        boolean notify = false;
        if (ft != null) {
            FinalizeTrigger.ensureFinalizer(obj);
            /* ensure that the old finalize won't run in any case */
            ft.clear();
            ((FinalizeTrigger) JyAttribute.getAttr(obj,
                    JyAttribute.FINALIZE_TRIGGER_ATTR)).flags = ft.flags;
            notify = (ft.flags & FinalizeTrigger.NOTIFY_GC_FLAG) != 0;
        }
        if ((gcFlags & VERBOSE_DELAYED) != 0 || (gcFlags & VERBOSE_FINALIZE) != 0) {
            writeDebug("gc", "restore finalizer of "+obj);
        }
        CycleMarkAttr cm = (CycleMarkAttr)
                JyAttribute.getAttr(obj, JyAttribute.GC_CYCLE_MARK_ATTR);
        if (cm != null && cm.monitored) {
            monitorObject(obj, true);
        }
        if (notify) {

            boolean cyclic;
            if (cm != null && cm.isUncollectable()) {
                cyclic = true;
            } else {
                markCyclicObjects(obj, true);
                cm = (CycleMarkAttr) JyAttribute.getAttr(obj, JyAttribute.GC_CYCLE_MARK_ATTR);
                cyclic = cm != null && cm.isUncollectable();
            }

            if ((gcFlags & VERBOSE_DELAYED) != 0 || (gcFlags & VERBOSE_FINALIZE) != 0) {
                writeDebug("gc", "notify finalizer abort;  cyclic? "+cyclic);
            }
            notifyAbortFinalize(obj, cyclic);
        }
    }

    /**
     * Restores weak references pointing to {@code rst}. Note that
     * this does not prevent callbacks, unless it is called during
     * finalization phase (e.g. by a finalizer) and
     * {@link #delayedWeakrefCallbacksEnabled()} returns {@code true}.
     * In a manual fashion, one can enforce this by using the gc flag
     * {@link #FORCE_DELAYED_WEAKREF_CALLBACKS}. Alternatively, one can
     * use the automatic way via the gc flag
     * {@link #PRESERVE_WEAKREFS_ON_RESURRECTION}, but then one would
     * not need to call this method anyway. The manual way has better
     * performance, but also brings more responsibilies.
     *
     * @see #delayedWeakrefCallbacksEnabled()
     * @see #FORCE_DELAYED_WEAKREF_CALLBACKS
     * @see #PRESERVE_WEAKREFS_ON_RESURRECTION
     */
    public static void restoreWeakReferences(PyObject rst) {
        ReferenceBackend toRestore = (ReferenceBackend)
                JyAttribute.getAttr(rst, JyAttribute.WEAK_REF_ATTR);
        if (toRestore != null) {
            toRestore.restore(rst);
        }
    }

    private static class DelayedFinalizationProcess implements Runnable {
        static DelayedFinalizationProcess defaultInstance =
                new DelayedFinalizationProcess();

        private static void performFinalization(PyObject del) {
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

        @Override
        public void run() {
            if ((gcFlags & VERBOSE_DELAYED) != 0) {
                writeDebug("gc", "run delayed finalization. Index: "+
                        gcMonitoredRunCount);
            }
            Set<PyObject> criticals = resurrectionCriticals.keySet();
            if (delayedFinalizationMode == DO_NOTHING_SPECIAL &&
                    (gcFlags & (PRESERVE_WEAKREFS_ON_RESURRECTION |
                    DONT_FINALIZE_RESURRECTED_OBJECTS)) == 0) {
                /* In this case we can do a cheap variant... */
                if ((gcFlags & FORCE_DELAYED_WEAKREF_CALLBACKS) != 0) {
                    if ((gcFlags & VERBOSE_DELAYED) != 0) {
                        writeDebug("gc", "process delayed callbacks (force-branch)");
                    }
                    GlobalRef.processDelayedCallbacks();
                }
                if ((gcFlags & FORCE_DELAYED_FINALIZATION) != 0) {
                    if ((gcFlags & VERBOSE_DELAYED) != 0) {
                        writeDebug("gc", "process delayed finalizers (force-branch)");
                    }
                    for (PyObject del: delayedFinalizables.keySet()) {
                        performFinalization(del);
                    }
                    for (PyObject cr: criticals) {
                        performFinalization(cr);
                    }
                    delayedFinalizables.clear();
                    resurrectionCriticals.clear();
                }
                if ((gcFlags & VERBOSE_DELAYED) != 0) {
                    writeDebug("gc", "forced delayed finalization run done");
                }
                return;
            }

            Set<PyObject> cyclicCriticals = removeNonCyclic(criticals);
            cyclicCriticals.retainAll(criticals);
            criticals.removeAll(cyclicCriticals);
            Set<PyObject> criticalReachablePool = findReachables(criticals);
            /* to avoid concurrent modification: */
            ArrayList<PyObject> criticalReachables = new ArrayList<>();
            FinalizeTrigger fn;
            if (delayedFinalizationMode == MARK_REACHABLE_CRITICALS) {
                for (PyObject obj: criticalReachablePool) {
                    fn = (FinalizeTrigger) JyAttribute.getAttr(obj,
                            JyAttribute.FINALIZE_TRIGGER_ATTR);
                    if (fn != null && fn.isActive() && fn.isFinalized()) {
                        criticalReachables.add(obj);
                        JyAttribute.setAttr(obj,
                            JyAttribute.GC_DELAYED_FINALIZE_CRITICAL_MARK_ATTR,
                            Integer.valueOf(gcMonitoredRunCount));
                    }
                }
            } else {
                for (PyObject obj: criticalReachablePool) {
                    fn = (FinalizeTrigger) JyAttribute.getAttr(obj,
                            JyAttribute.FINALIZE_TRIGGER_ATTR);
                    if (fn != null && fn.isActive() && fn.isFinalized()) {
                        criticalReachables.add(obj);
                    }
                }
            }
            criticals.removeAll(criticalReachables);
            if ((gcFlags & PRESERVE_WEAKREFS_ON_RESURRECTION) != 0) {
                if ((gcFlags & VERBOSE_DELAYED) != 0) {
                    writeDebug("gc", "restore potentially resurrected weak references...");
                }
                for (PyObject rst: criticalReachablePool) {
                    restoreWeakReferences(rst);
                }
                GlobalRef.processDelayedCallbacks();
            }
            criticalReachablePool.clear();
            if ((gcFlags & DONT_FINALIZE_RESURRECTED_OBJECTS) != 0) {
                /* restore all finalizers that might belong to resurrected objects: */
                if ((gcFlags & VERBOSE_DELAYED) != 0) {
                    writeDebug("gc", "restore "+criticalReachables.size()+
                            " potentially resurrected finalizers...");
                }
                for (PyObject obj: criticalReachables) {
                    restoreFinalizer(obj);
                }
            } else {
                if ((gcFlags & VERBOSE_DELAYED) != 0) {
                    writeDebug("gc", "delayed finalization of "+criticalReachables.size()+
                            " potentially resurrected finalizers...");
                }
                for (PyObject del: criticalReachables) {
                    performFinalization(del);
                }
            }
            cyclicCriticals.removeAll(criticalReachables);
            if ((gcFlags & VERBOSE_DELAYED) != 0 && !delayedFinalizables.isEmpty()) {
                writeDebug("gc", "process "+delayedFinalizables.size()+
                        " delayed finalizers...");
            }
            for (PyObject del: delayedFinalizables.keySet()) {
                performFinalization(del);
            }
            if ((gcFlags & VERBOSE_DELAYED) != 0 && !cyclicCriticals.isEmpty()) {
                writeDebug("gc", "process "+cyclicCriticals.size()+" cyclic delayed finalizers...");
            }
            for (PyObject del: cyclicCriticals) {
                performFinalization(del);
            }
            if ((gcFlags & VERBOSE_DELAYED) != 0 && !criticals.isEmpty()) {
                writeDebug("gc", "calling "+criticals.size()+
                        " critical finalizers not reachable by other critical finalizers...");
            }
            if (delayedFinalizationMode == MARK_REACHABLE_CRITICALS &&
                    !criticals.isEmpty() && !criticalReachables.isEmpty()) {
                /* This means some critical reachables might be not critical-reachable any more.
                 * In a synchronized gc collection approach System.gc should run again while
                 * something like this is found. (Yes, not exactly a cheap task, but since this
                 * is for debugging, correctness counts.)
                 */
                notifyRerun = true;
            }
            if (delayedFinalizationMode == NOTIFY_FOR_RERUN && !notifyRerun) {
                for (PyObject del: criticals) {
                    if (!notifyRerun) {
                        Object m = JyAttribute.getAttr(del,
                                JyAttribute.GC_DELAYED_FINALIZE_CRITICAL_MARK_ATTR);
                        if (m != null && ((Integer) m).intValue() == gcMonitoredRunCount) {
                            notifyRerun = true;
                        }
                    }
                    performFinalization(del);
                }
            } else {
                for (PyObject del: criticals) {
                    performFinalization(del);
                }
            }
            delayedFinalizables.clear();
            resurrectionCriticals.clear();
            if ((gcFlags & VERBOSE_DELAYED) != 0) {
                writeDebug("gc", "delayed finalization run done");
            }
        }
    }

    public static boolean delayedWeakrefCallbacksEnabled() {
        return (gcFlags & (PRESERVE_WEAKREFS_ON_RESURRECTION |
                FORCE_DELAYED_WEAKREF_CALLBACKS)) != 0;
    }

    public static boolean delayedFinalizationEnabled() {
        return (gcFlags & (PRESERVE_WEAKREFS_ON_RESURRECTION |
                DONT_FINALIZE_RESURRECTED_OBJECTS |
                FORCE_DELAYED_FINALIZATION)) != 0;
    }

    private static void updateDelayedFinalizationState() {
        /*
         * There might be the case where delayed weakref callbacks are enabled,
         * but not delayed finalization. We still register a DelayedFinalizationProcess
         * then. That process detects the situation by checking the flags and only
         * performs GlobalRef.processDelayedCallbacks() then.
         */
        if (delayedFinalizationEnabled() || delayedWeakrefCallbacksEnabled()) {
            resumeDelayedFinalization();
        } else if (indexOfPostFinalizationProcess(
                DelayedFinalizationProcess.defaultInstance) != -1) {
            suspendDelayedFinalization();
        }
        if (!delayedWeakrefCallbacksEnabled() &&
                GlobalRef.hasDelayedCallbacks()) {
            // If delayed callbacks were turned off, we process remaining
            // queued callbacks immediately (but in a new thread though):
            Thread dlcProcess = new Thread() {
                @Override
                public void run() {
                    GlobalRef.processDelayedCallbacks();
                }
            };
            dlcProcess.start();
        }
    }

    private static void resumeDelayedFinalization() {
        if (delayedFinalizables == null) {
            delayedFinalizables = new IdentityHashMap<>();
        }
        if (resurrectionCriticals == null) {
            resurrectionCriticals = new IdentityHashMap<>();
        }
        /* add post finalization process (and cancel pending suspension process if any) */
        synchronized (postFinalizationProcessRemove) {
            postFinalizationProcessRemove.remove(DelayedFinalizationProcess.defaultInstance);
        }
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

    private static boolean isResurrectionCritical(PyObject ob) {
        return (isTraversable(ob))
                && FinalizeTrigger.hasActiveTrigger(ob);
    }

    public static void registerForDelayedFinalization(PyObject ob) {
        if (isResurrectionCritical(ob)) {
            resurrectionCriticals.put(ob, ob);
        } else {
            delayedFinalizables.put(ob, ob);
        }
    }

    public static void abortDelayedFinalization(PyObject ob) {
        resurrectionCriticals.remove(ob);
        delayedFinalizables.remove(ob);
        if ((gcFlags & VERBOSE_DELAYED) != 0 || (gcFlags & VERBOSE_FINALIZE) != 0) {
            writeDebug("gc", "abort delayed finalization of "+ob);
        }
        restoreFinalizer(ob);
    }
//--------------end of delayed finalization section----------------------------




//--------------Finalization preprocess/postprocess section--------------------

    private static class PostFinalizationProcessor implements Runnable {
        protected static PostFinalizationProcessor defaultInstance =
                new PostFinalizationProcessor();

        @Override
        public void run() {
            /* We wait until last postFinalizationTimestamp is at least timeOut ago.
             * This should only be measured when openFinalizeCount is zero.
             */
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
     * Registers a process that will be called before any finalization during gc run
     * takes place ("finalization" refers to Jython style finalizers ran by
     * {@link org.python.core.finalization.FinalizeTrigger}s;
     * to care for other finalizers these must call
     * {@code gc.notifyPreFinalization()} before anything else is done and
     * {@code gc.notifyPostFinalization()} afterwards; between these calls the finalizer
     * must not terminate by throwing an exception).
     * This works independently from monitoring, which is mainly needed to allow
     * counting of cyclic garbage in {@link #collect()}.
     * </p>
     * <p>
     * This feature compensates that Java's gc does not provide any guarantees about
     * finalization order. Java not even guarantees that when a weak reference is
     * added to a reference queue, its finalizer already ran or not yet ran, if any.
     * </p>
     * <p>
     * The only guarantee is that {@link java.lang.ref.PhantomReference}s are enqueued
     * after finalization of their referents, but this happens in another gc cycle then.
     * </p>
     * <p>
     * Actually there are still situations that can cause pre finalization process to
     * run again during finalization phase. This can happen if external frameworks use
     * their own finalizers. This can be cured by letting these finalizers call
     * {@code gc.notifyPreFinalization()} before anything else is done and
     * {@code gc.notifyPostFinalization()} right before the finalization method returns.
     * Between these calls the finalizer must not terminate by throwing an exception.
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
     * See doc of {@link #registerPreFinalizationProcess(Runnable)}.
     */
    public static void registerPreFinalizationProcess(Runnable process, int index) {
        while (true) {
            synchronized (preFinalizationProcess) {
                preFinalizationProcess.add(index < 0 ? index + preFinalizationProcess.size() + 1 : index, process);
            }
            return;
        }
    }

    public static int indexOfPreFinalizationProcess(Runnable process) {
        synchronized (preFinalizationProcess) {
            return preFinalizationProcess.indexOf(process);
        }
    }

    public static boolean unregisterPreFinalizationProcess(Runnable process) {
        synchronized (preFinalizationProcess) {
            return preFinalizationProcess.remove(process);
        }
    }

    /**
     * Useful if a process wants to remove another one or itself during its execution.
     * This asynchronous unregister method circumvents the synchronized state on
     * pre finalization process list.
     */
    public static void unregisterPreFinalizationProcessAfterNextRun(Runnable process) {
        synchronized (preFinalizationProcessRemove) {
            preFinalizationProcessRemove.add(process);
        }
        return;
    }

    /**
     * <p>
     * Registers a process that will be called after all finalization during gc run
     * is done ("finalization" refers to Jython style finalizers ran by
     * {@link org.python.core.finalization.FinalizeTrigger}s;
     * to care for other finalizers these must call
     * {@code gc.notifyPreFinalization()} before anything else is done and
     * {@code gc.notifyPostFinalization()} afterwards; between these calls the finalizer
     * must not terminate by throwing an exception).
     * This works independently from monitoring (which is mainly needed to allow
     * garbage counting in {@link #collect()}).
     * </p>
     * <p>
     * This feature compensates that Java's gc does not provide any guarantees about
     * finalization order. Java not even guarantees that when a weak reference is
     * added to a reference queue, its finalizer already ran or not yet ran, if any.
     * </p>
     * <p>
     * The only guarantee is that {@link java.lang.ref.PhantomReference}s are
     * enqueued after finalization of the referents, but this
     * happens - however - in another gc cycle then.
     * </p>
     * <p>
     * There are situations that can cause post finalization process to run
     * already during finalization phase. This can happen if external frameworks use
     * their own finalizers. This can be cured by letting these finalizers call
     * {@code gc.notifyPreFinalization()} before anything else is done and
     * {@code gc.notifyPostFinalization()} right before the finalization method returns.
     * Between these calls the finalizer must not terminate by throwing an exception.
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
     * See doc of {@link #registerPostFinalizationProcess(Runnable)}.
     */
    public static void registerPostFinalizationProcess(Runnable process, int index) {
        synchronized (postFinalizationProcess) {
            postFinalizationProcess.add(index < 0 ? index + postFinalizationProcess.size() + 1 : index, process);
        }
    }

    public static int indexOfPostFinalizationProcess(Runnable process) {
        synchronized (postFinalizationProcess) {
            return postFinalizationProcess.indexOf(process);
        }
    }

    public static boolean unregisterPostFinalizationProcess(Runnable process) {
        synchronized (postFinalizationProcess) {
            return postFinalizationProcess.remove(process);
        }
    }

    /**
     * Useful if a process wants to remove another one or itself during its execution.
     * This asynchronous unregister method circumvents the synchronized state on
     * post finalization process list.
     */
    public static void unregisterPostFinalizationProcessAfterNextRun(Runnable process) {
        synchronized (postFinalizationProcessRemove) {
            postFinalizationProcessRemove.add(process);
        }
    }

    public static void notifyPreFinalization() {
        long callTime = System.currentTimeMillis();
/*
 * This section is experimental and kept for further investigation. In theory, it can
 * prevent potential problems in JyNI gc, if a gc run overlaps the previous run's
 * post finalization phase. However it currently breaks gc tests, so is out commented
 * so far. In practical sense, JyNI's gc support also works fine without it so far.
 */
//        if (postFinalizationPending) {
//            if ((gcFlags & VERBOSE_COLLECT) != 0) {
//                writeDebug("gc", "waiting for pending post finalization process.");
//            }
//            /* It is important to have the while (which is actually an "if" since the
//             * InterruptedException is very unlikely to occur) *inside* the synchronized
//             * block. Otherwise the notification might come just between the check and the wait,
//             * causing an endless waiting. This is no pure academic consideration, but was
//             * actually observed to happen from time to time, especially on faster systems.
//             */
//            synchronized(PostFinalizationProcessor.class) {
//                while (postFinalizationPending) {
//                    try {
//                        PostFinalizationProcessor.class.wait();
//                    } catch (InterruptedException ie3) {}
//                }
//            }
//            if ((gcFlags & VERBOSE_COLLECT) != 0) {
//                writeDebug("gc", "post finalization finished.");
//            }
//        }
//        /*
//         * Increment of openFinalizeCount must not happen before waiting for pending
//         * post finalization process is done. Otherwise PostFinalizationProcessor can
//         * be waiting for a neutral openFinalizeCount, causing a deadlock.
//         */
        ++openFinalizeCount;
        if (callTime - postFinalizationTimestamp
                < postFinalizationTimeOut) {
            return;
        }
            synchronized(preFinalizationProcess) {
                for (Runnable r: preFinalizationProcess) {
                    try {
                        r.run();
                    } catch (Exception preProcessError) {
                        Py.writeError("gc", "Finalization preprocess "+r+" caused error: "
                                +preProcessError);
                    }
                }
                    synchronized (preFinalizationProcessRemove) {
                        preFinalizationProcess.removeAll(preFinalizationProcessRemove);
                        preFinalizationProcessRemove.clear();
                    }
            }

            synchronized(postFinalizationProcess) {
                if (!postFinalizationProcess.isEmpty() &&
                        postFinalizationProcessor == null) {
                    postFinalizationPending = true;
                    postFinalizationProcessor = new Thread(
                            PostFinalizationProcessor.defaultInstance);
                    postFinalizationProcessor.start();
                }
            }
    }

    public static void notifyPostFinalization() {
        postFinalizationTimestamp = System.currentTimeMillis();
        --openFinalizeCount;
        if (openFinalizeCount == 0 && postFinalizationProcessor != null) {
            postFinalizationProcessor.interrupt();
        }
    }

    protected static void postFinalizationProcess() {
        synchronized (postFinalizationProcess) {
            for (Runnable r : postFinalizationProcess) {
                try {
                    r.run();
                } catch (Exception postProcessError) {
                    System.err.println("Finalization postprocess " + r + " caused error:");
                    System.err.println(postProcessError);
                }
            }
            synchronized (postFinalizationProcessRemove) {
                postFinalizationProcess.removeAll(postFinalizationProcessRemove);
                postFinalizationProcessRemove.clear();
            }
        }
    }
//--------------end of Finalization preprocess/postprocess section-------------




//--------------Monitoring section---------------------------------------------

    public static void monitorObject(PyObject ob) {
        monitorObject(ob, false);
    }

    public static void monitorObject(PyObject ob, boolean initString) {
        /* Already collected garbage should not be monitored,
         * thus also not the garbage list:
         */
        if (ob == null || ob == garbage) {
            return;
        }

        /* In contrast to isTraversable(ob) we don't look for DONT_TRAVERSE_BY_REFLECTION
         * here. Objects not explicitly marked as Untraversable get monitored so we are
         * able to print out warnings in Traverseproc.
         */
        if (!monitorNonTraversable &&
                !(ob instanceof Traverseproc || ob instanceof TraverseprocDerived)) {
            if (ob.getClass().isAnnotationPresent(Untraversable.class) &&
                    !JyAttribute.hasAttr(ob, JyAttribute.FINALIZE_TRIGGER_ATTR)) {
                return;
            }
        }
        if (gcTrash == null) {
            gcTrash = new ReferenceQueue<>();
        }
        while (true) {
            synchronized (monitoredObjects) {
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
        }
    }

    /**
     * Avoid to use this method. It is inefficient and no intended purpose of the
     * backing Set of objects. In normal business it should not be needed and only
     * exists for bare debugging purposes.
     */
    public static WeakReferenceGC getMonitorReference(PyObject ob) {
        synchronized (monitoredObjects) {
            for (WeakReferenceGC ref : monitoredObjects) {
                if (ref.equals(ob)) {
                    return ref;
                }
            }
        }
        return null;
    }

    public static boolean isMonitoring() {
        synchronized (monitoredObjects) {
            return !monitoredObjects.isEmpty();
        }
    }

    public static boolean isMonitored(PyObject ob) {
        synchronized (monitoredObjects) {
            WeakrefGCCompareDummy.defaultInstance.setCompare(ob);
            boolean result = monitoredObjects.contains(WeakrefGCCompareDummy.defaultInstance);
            WeakrefGCCompareDummy.defaultInstance.clearCompare();
            return result;
        }
    }

    public static boolean unmonitorObject(PyObject ob) {
            synchronized(monitoredObjects) {
                WeakrefGCCompareDummy.defaultInstance.setCompare(ob);
                WeakReferenceGC rem = getMonitorReference(ob);
                if (rem != null) {
                    rem.clear();
                }
                boolean result = monitoredObjects.remove(
                    WeakrefGCCompareDummy.defaultInstance);
                WeakrefGCCompareDummy.defaultInstance.clearCompare();
                JyAttribute.delAttr(ob, JyAttribute.GC_CYCLE_MARK_ATTR);
                FinalizeTrigger ft = (FinalizeTrigger)
                    JyAttribute.getAttr(ob, JyAttribute.FINALIZE_TRIGGER_ATTR);
                if (ft != null) {
                    ft.flags &= ~FinalizeTrigger.NOTIFY_GC_FLAG;
                }
                return result;
            }
    }

    public static void unmonitorAll() {
        synchronized (monitoredObjects) {
            FinalizeTrigger ft;
            for (WeakReferenceGC mo : monitoredObjects) {
                PyObject rfrt = mo.get();
                if (rfrt != null) {
                    JyAttribute.delAttr(rfrt, JyAttribute.GC_CYCLE_MARK_ATTR);
                    ft = (FinalizeTrigger) JyAttribute.getAttr(rfrt, JyAttribute.FINALIZE_TRIGGER_ATTR);
                    if (ft != null) {
                        ft.flags &= ~FinalizeTrigger.NOTIFY_GC_FLAG;
                    }
                }
                mo.clear();
            }
            monitoredObjects.clear();
        }
    }

    public static void stopMonitoring() {
        setMonitorGlobal(false);
        unmonitorAll();
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
//--------------end of Monitoring section--------------------------------------


    /**
     * Gets the current Jython specific gc flags.
     *
     * @see #MONITOR_GLOBAL
     * @see #DONT_FINALIZE_CYCLIC_GARBAGE
     * @see #PRESERVE_WEAKREFS_ON_RESURRECTION
     * @see #DONT_FINALIZE_RESURRECTED_OBJECTS
     * @see #DONT_TRAVERSE_BY_REFLECTION
     * @see #SUPPRESS_TRAVERSE_BY_REFLECTION_WARNING
     * @see #INSTANCE_TRAVERSE_BY_REFLECTION_WARNING
     * @see #USE_PY_WRITE_DEBUG
     * @see #VERBOSE_COLLECT
     * @see #VERBOSE_WEAKREF
     * @see #VERBOSE_DELAYED
     * @see #VERBOSE_FINALIZE
     * @see #VERBOSE
     * @see #setJythonGCFlags(short)
     * @see #addJythonGCFlags(short)
     * @see #removeJythonGCFlags(short)
     */
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

    /**
     * Sets the current Jython specific gc flags.
     * <br>
     * {@code flags} is a {@code short} and can have the following bits turned on:<br>
     * <br>
     * {@link #MONITOR_GLOBAL} - Automatically monitors all PyObjects created from now on.<br>
     * {@link #DONT_FINALIZE_CYCLIC_GARBAGE} - Adds cyclic finalizable PyObjects to {@link #garbage}.<br>
     * {@link #PRESERVE_WEAKREFS_ON_RESURRECTION} - Keeps weak references alive if the referent is resurrected.<br>
     * {@link #DONT_FINALIZE_RESURRECTED_OBJECTS} -
     * Emulates CPython behavior regarding resurrected objects and finalization.<br>
     * {@link #DONT_TRAVERSE_BY_REFLECTION} - Inhibits reflection-based traversal.<br>
     * {@link #SUPPRESS_TRAVERSE_BY_REFLECTION_WARNING} -
     * Suppress warnings for PyObjects that neither implement {@link org.python.core.Traverseproc} nor
     * are marked as {@link org.python.core.Untraversable}.<br>
     * {@link #USE_PY_WRITE_DEBUG} - uses {@link org.python.core.Py#writeDebug(String, String)} for
     * debugging output instead of directly writing to {@link java.lang.System#err}.<br>
     * {@link #VERBOSE_COLLECT} - Enable collection-related verbose output.<br>
     * {@link #VERBOSE_WEAKREF} - Enable weakref-related verbose output.<br>
     * {@link #VERBOSE_DELAYED} - Enable delayed finalization-related verbose output.<br>
     * {@link #VERBOSE_FINALIZE} - Enable finalization-related verbose output.<br>
     * {@link #VERBOSE} - All previous verbose-flags combined.
     *
     * @see #MONITOR_GLOBAL
     * @see #DONT_FINALIZE_CYCLIC_GARBAGE
     * @see #PRESERVE_WEAKREFS_ON_RESURRECTION
     * @see #DONT_FINALIZE_RESURRECTED_OBJECTS
     * @see #DONT_TRAVERSE_BY_REFLECTION
     * @see #SUPPRESS_TRAVERSE_BY_REFLECTION_WARNING
     * @see #INSTANCE_TRAVERSE_BY_REFLECTION_WARNING
     * @see #USE_PY_WRITE_DEBUG
     * @see #VERBOSE_COLLECT
     * @see #VERBOSE_WEAKREF
     * @see #VERBOSE_DELAYED
     * @see #VERBOSE_FINALIZE
     * @see #VERBOSE
     * @see #getJythonGCFlags()
     * @see #addJythonGCFlags(short)
     * @see #removeJythonGCFlags(short)
     */
    public static void setJythonGCFlags(short flags) {
        gcFlags = flags;
        PyObject.gcMonitorGlobal = (gcFlags & MONITOR_GLOBAL) != 0;
        updateDelayedFinalizationState();
    }

    /**
     * This is a convenience method to add flags via bitwise or.
     *
     * @see #MONITOR_GLOBAL
     * @see #DONT_FINALIZE_CYCLIC_GARBAGE
     * @see #PRESERVE_WEAKREFS_ON_RESURRECTION
     * @see #DONT_FINALIZE_RESURRECTED_OBJECTS
     * @see #DONT_TRAVERSE_BY_REFLECTION
     * @see #SUPPRESS_TRAVERSE_BY_REFLECTION_WARNING
     * @see #INSTANCE_TRAVERSE_BY_REFLECTION_WARNING
     * @see #USE_PY_WRITE_DEBUG
     * @see #VERBOSE_COLLECT
     * @see #VERBOSE_WEAKREF
     * @see #VERBOSE_DELAYED
     * @see #VERBOSE_FINALIZE
     * @see #VERBOSE
     * @see #getJythonGCFlags()
     * @see #setJythonGCFlags(short)
     * @see #removeJythonGCFlags(short)
     */
    public static void addJythonGCFlags(short flags) {
        gcFlags |= flags;
        PyObject.gcMonitorGlobal = (gcFlags & MONITOR_GLOBAL) != 0;
        updateDelayedFinalizationState();
    }

    /**
     * This is a convenience method to remove flags via bitwise and-not.
     *
     * @see #MONITOR_GLOBAL
     * @see #DONT_FINALIZE_CYCLIC_GARBAGE
     * @see #PRESERVE_WEAKREFS_ON_RESURRECTION
     * @see #DONT_FINALIZE_RESURRECTED_OBJECTS
     * @see #DONT_TRAVERSE_BY_REFLECTION
     * @see #SUPPRESS_TRAVERSE_BY_REFLECTION_WARNING
     * @see #INSTANCE_TRAVERSE_BY_REFLECTION_WARNING
     * @see #USE_PY_WRITE_DEBUG
     * @see #VERBOSE_COLLECT
     * @see #VERBOSE_WEAKREF
     * @see #VERBOSE_DELAYED
     * @see #VERBOSE_FINALIZE
     * @see #VERBOSE
     * @see #getJythonGCFlags()
     * @see #setJythonGCFlags(short)
     * @see #addJythonGCFlags(short)
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

    /**
     * Does nothing in Jython as Java gc is always enabled.
     */
    public static void enable() {}

    /**
     * Not supported by Jython.
     *
     * @throws PyException {@code NotImplementedError}
     */
    public static void disable() {
        throw Py.NotImplementedError("can't disable Java GC");
    }

    /**
     * Always returns {@code true} in Jython.
     */
    public static boolean isenabled() { return true; }

    /**
     * The generation parameter is only for compatibility with
     * CPython {@link #collect()} and is ignored.
     * @param generation (ignored)
     * @return Collected monitored cyclic trash objects or
     * {@code gc.UNKNOWN_COUNT} if nothing is monitored or -1 if
     * an error occurred and collection did not complete.
     * @see #collect()
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
     * {@code System.gc()} and returns {@link #UNKNOWN_COUNT} as a
     * non-erroneous default value. If objects are monitored,
     * it emulates a synchronous gc run in the sense that it waits
     * until all collected monitored objects were finalized.
     *
     * @return Number of collected monitored cyclic trash objects
     * or {@link #UNKNOWN_COUNT} if nothing is monitored or -1
     * if an error occurred and collection did not complete.
     * @see #UNKNOWN_COUNT
     * @see #collect(int)
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
            result = UNKNOWN_COUNT; /* indicates unknown result (-1 would indicate error) */
        } else {
            if (!gcRunning.compareAndSet(false, true)) {
                if ((gcFlags & VERBOSE_COLLECT) != 0) {
                    writeDebug("gc", "collect already running...");
                }
                /* We must fail fast in this case to avoid deadlocks.
                 * Deadlock would for instance occur if a finalizer calls
                 * gc.collect (like is done in some tests in test_gc).
                 * Former version: throw new IllegalStateException("GC is already running.");
                 */
                return -1; /* better not throw exception here, as calling code
                            * is usually not prepared for that
                            */
            }
            if ((gcFlags & VERBOSE_COLLECT) != 0) {
                writeDebug("gc", "perform monitored sync gc run...");
            }
            if (needsTrashPrinting() || (gcFlags & VERBOSE) != 0) {
                /* When the weakrefs are enqueued, the referents won't be available
                 * any more to provide their string representations, so we must
                 * save the string representations in the weak ref objects while
                 * the referents are still alive.
                 * We cannot save the string representations in the moment when the
                 * objects get monitored, because with monitorGlobal activated
                 * the objects get monitored just when they are created and some
                 * of them are in an invalid state then and cannot directly obtain
                 * their string representation (would produce overflow errors and
                 * such bad stuff). So we do it here...
                 */
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
            delayedFinalizationMode = MARK_REACHABLE_CRITICALS;
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
                    /* The NOTIFY_GC_FLAG is needed, because monitor state changes during
                     * collection. So the FinalizeTriggers can't use gc.isMonitored to know
                     * whether gc notification is needed.
                     */
                }
            }

            /* Typically this line causes a gc run: */
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
        lastRemoveTimeStamp = System.currentTimeMillis();
        if (finalizeWaitCount != 0) {
            System.err.println("Finalize wait count should be initially 0!");
            finalizeWaitCount = 0;
        }

        /* We tidy up a bit... (Because it is not unlikely that
         * the preparation stuff done so far has caused a gc run.)
         * This is not entirely safe as gc could interfere with
         * this process at any time again. Since this is intended
         * for debugging, this solution is sufficient in practice.
         * Maybe we will include more mechanisms to ensure safety
         * in the future.
         */

        try {
            trash = gcTrash.remove(initWaitTime);
            if (trash != null && (gcFlags & VERBOSE_COLLECT) != 0) {
                writeDebug("gc", "monitored objects from interferring gc run found.");
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
        List<WeakReferenceGC> collectBuffer;

        /* The following out commented block is a nice idea to sync gc in a more
         * elegant way. Unfortunately it proved infeasible because MXBean appears
         * to be no reliable measurement for gc to have finished enqueueing trash.
         * We leave it here to document this failed approach for future generations,
         * so nobody needs to waste time on this again/can quickly reproduce how
         * it fails.

        // Yes, Errors should not be caught, but in this case we have a very good
        // reason for it.
        // collectSyncViaMXBean uses inofficial API, i.e. classes from com.sun.management.
        // In case that a JVM does not provide this API, we have a less elegant fallback
        // at hand, which is based on a sentinel and timeout technique.
        try {
            collectBuffer = collectSyncViaMXBean(stat, cyclic);
        } catch (NoClassDefFoundError ncdfe) {
            collectBuffer = collectSyncViaSentinel(stat, cyclic);
        }
        if ((gcFlags & VERBOSE_COLLECT) != 0) {
            writeDebug("gc", "all objects from run enqueud in trash queue.");
            writeDebug("gc", "pending finalizers: "+finalizeWaitCount);
        }*/

        collectBuffer = collectSyncViaSentinel(stat, cyclic);
        //lockPostFinalization assures that postFinalization process
        //only runs once per syncCollect call.
        lockPostFinalization = false;
        if (postFinalizationProcessor != null) {
            //abort the remaining wait time if a postFinalizationProcessor is waiting
            postFinalizationProcessor.interrupt();
        }
        waitingForFinalizers = true;
        waitForFinalizers();
        waitingForFinalizers = false;
        if (postFinalizationPending) {
            if ((gcFlags & VERBOSE_COLLECT) != 0) {
                writeDebug("gc", "waiting for pending post-finalization process.");
            }
            /* It is important to have the while (which is actually an "if" since the
             * InterruptedException is very unlikely to occur) *inside* the synchronized
             * block. Otherwise the notification might come just between the check and the wait,
             * causing an endless waiting. This is no pure academic consideration, but was
             * actually observed to happen from time to time, especially on faster systems.
             */
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
             * listing related to DEBUG_X flags also counts/lists
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
             * of DEBUG_X flags.
             *
             * - stores only those objects from the cycle that actually have
             * finalizers in gc.garbage.
             *
             * While slightly contradictory to the doc, we reproduce this
             * behavior here.
             */
            if ((debugFlags & gc.DEBUG_COLLECTABLE) != 0 &&
                    (    (debugFlags & gc.DEBUG_OBJECTS) != 0 ||
                        (debugFlags & gc.DEBUG_INSTANCES) != 0)) {
                /* note that all cycleMarks should have been initialized when
                 * objects became monitored.
                 */
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

    /*
     * The following out commented section belongs to the out commented
     * block on MXBean based GC sync somewhere above.
     * We keep it here to document this failed approach and to enable
     * future developers to quickly reproduce and analyse this failure.

    private static class GCListener implements NotificationListener {
        private int index;
        private Thread toNotify;

        public GCListener(int index, Thread toNotify) {
            this.index = index;
            this.toNotify = toNotify;
        }

        public void handleNotification(Notification notif, Object handback) {
            if (waitForGCNotification[index]) {
                String notifType = notif.getType();
                if (notifType.equals(
                        com.sun.management.GarbageCollectionNotificationInfo.GARBAGE_COLLECTION_NOTIFICATION)) {
                    // retrieve the garbage collection notification information
                    CompositeData cd = (CompositeData) notif.getUserData();
                    com.sun.management.GarbageCollectionNotificationInfo info =
                            com.sun.management.GarbageCollectionNotificationInfo.from(cd);
                    if (info.getGcCause().equals("System.gc()") &&
                            info.getGcAction().startsWith("end of ") &&
                            info.getGcAction().endsWith(" GC")) {
                        synchronized (GCListener.class) {
                            --outstandingNotifications;
                            if (toNotify != null && waitingForTrash && outstandingNotifications == 0) {
                                toNotify.interrupt();
                                toNotify = null;
                            }
                        }
                    }
                }
            }
        }
    }

    private static boolean[] waitForGCNotification;
    private static int outstandingNotifications = 0;
    private static List<GarbageCollectorMXBean> currentGCBeans;
    private static String failFast = null;
    private static boolean waitingForTrash = false;
    private static List<WeakReferenceGC> collectSyncViaMXBean(int[] stat, Set<WeakReferenceGC> cyclic) {
        // This step should be done first in order to fail fast,
        // if com.sun.management is not available.
        if (failFast == null) {
            failFast =
                com.sun.management.GarbageCollectionNotificationInfo.GARBAGE_COLLECTION_NOTIFICATION;
        }
        //Reaching this line successfully means that com.sun.management exists.
        Reference<? extends Object> trash;
        currentGCBeans = ManagementFactory.getGarbageCollectorMXBeans();
        long[] initialGCCounts = new long[currentGCBeans.size()];
        GCListener[] listeners = new GCListener[initialGCCounts.length];
        int i = 0;
        for (GarbageCollectorMXBean gcb: currentGCBeans) {
            listeners[i] = new GCListener(i, Thread.currentThread());
            ((NotificationBroadcaster) gcb).addNotificationListener(
                    listeners[i], null, null);
            initialGCCounts[i++] = gcb.getCollectionCount();
        }
        if (waitForGCNotification == null || waitForGCNotification.length != initialGCCounts.length) {
            waitForGCNotification = new boolean[initialGCCounts.length];
        }
        synchronized (GCListener.class) {
            boolean gcRunDetected = false;
            outstandingNotifications = 0;
            while (!gcRunDetected) {
                System.gc();
                for (i = 0; i < waitForGCNotification.length; ++i) {
                    waitForGCNotification[i] =
                            currentGCBeans.get(i).getCollectionCount() > initialGCCounts[i];
                    ++outstandingNotifications;
                    if (waitForGCNotification[i]) {
                        // at least one counter should change if a gc run occurred.
                        gcRunDetected = true;
                    }
                }
            }
        }
        List<WeakReferenceGC> collectBuffer = null;
        if (needsCollectBuffer()) {
            collectBuffer = new ArrayList<>();
        }
        while (outstandingNotifications > 0) {
            try {
                waitingForTrash = true;
                Thread.sleep(2000);
            } catch (InterruptedException ie)
            {
            }
            waitingForTrash = false;
        }
        trash = gcTrash.poll();
        while (trash != null) {
            if (trash instanceof WeakReferenceGC) {
                synchronized(monitoredObjects) {
                    monitoredObjects.remove(trash);
                }
                //We avoid counting Jython-specific objects in order to
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
            }
            trash = gcTrash.poll();
        }
        try {
            Thread.sleep(200);
        } catch (InterruptedException ie) {
        }
        trash = gcTrash.poll();
        if (trash != null) {
            //This should not happen, but actually does.
            //So MXBean-based sync is not reliable!
            System.out.println("Late trash: "+trash);
            System.out.println("Late trash: "+trash.getClass());
            System.out.println("Late trash: "+System.identityHashCode(trash));
            int count = 0;
            while (trash != null) {
                System.out.println("Late trash "+count+": "+trash);
                ++count;
                trash = gcTrash.poll();
            }
            System.out.println("Late trash count: "+count);
        }
        i = 0;
        for (GarbageCollectorMXBean gcb: currentGCBeans) {
            try {
                ((NotificationBroadcaster) gcb).removeNotificationListener(
                        listeners[i++]);
            } catch (ListenerNotFoundException lnfe) {
            }
        }
        return collectBuffer;
    }
    */

    private static List<WeakReferenceGC> collectSyncViaSentinel(int[] stat, Set<WeakReferenceGC> cyclic) {
        WeakReference<GCSentinel> sentRef =
                new WeakReference<>(new GCSentinel(Thread.currentThread()), gcTrash);
        Reference<? extends Object> trash;
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
                        /* We avoid counting Jython-specific objects in order to
                         * obtain CPython-comparable results.
                         */
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
        return collectBuffer;
    }

    private static void waitForFinalizers() {
        if (finalizeWaitCount != 0) {
            if ((gcFlags & VERBOSE_COLLECT) != 0) {
                writeDebug("gc", "waiting for "+finalizeWaitCount+
                        " pending finalizers.");
                if (finalizeWaitCount < 0) {
                    /* Maybe even throw exception here? */
                    Py.writeError("gc", "There should never be "+
                            "less than zero pending finalizers!");
                }
            }
            /* It is important to have the while *inside* the synchronized block.
             * Otherwise the notify might come just between the check and the wait,
             * causing an endless waiting.
             */
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
        /* Can the following block be skipped if monitor global is active?
         * No, because there could already be unmonitored finalizable objects!
         */
        while (openFinalizeCount > 0 || System.currentTimeMillis() - postFinalizationTimestamp
                < postFinalizationTimeOut) {
            try {
                Thread.sleep(postFinalizationTimeOut);
            } catch (InterruptedException ie) {}
        }
    }

    /**
     * Not supported by Jython.
     *
     * @throws PyException {@code NotImplementedError}
     */
    public static PyObject get_count() {
        throw Py.NotImplementedError("not applicable to Java GC");
    }

    /**
     * Copied from CPython doc:<br>
     * <br>
     * Set the garbage collection debugging flags. Debugging information is
     * written to {@code System.err}.<br>
     * <br>
     * {@code flags} flags is an {@code int}eger and can have the following bits turned on:<br>
     * <br>
     * {@link #DEBUG_STATS} - Print statistics during collection.<br>
     * {@link #DEBUG_COLLECTABLE} - Print collectable objects found.<br>
     * {@link #DEBUG_UNCOLLECTABLE} - Print unreachable but uncollectable objects found.<br>
     * {@link #DEBUG_INSTANCES} - Print instance objects.<br>
     * {@link #DEBUG_OBJECTS} - Print objects other than instances.<br>
     * {@link #DEBUG_SAVEALL} - Save objects to gc.garbage rather than freeing them.<br>
     * {@link #DEBUG_LEAK} - Debug leaking programs (everything but STATS).
     *
     * @see #DEBUG_STATS
     * @see #DEBUG_COLLECTABLE
     * @see #DEBUG_UNCOLLECTABLE
     * @see #DEBUG_INSTANCES
     * @see #DEBUG_OBJECTS
     * @see #DEBUG_SAVEALL
     * @see #DEBUG_LEAK
     */
    public static void set_debug(int flags) {
        debugFlags = flags;
    }

    /**
     * Copied from CPython doc:<br>
     * <br>
     * Get the garbage collection debugging flags.
     */
    public static int get_debug() {
        return debugFlags;
    }

    /**
     * Not supported by Jython.
     *
     * @throws PyException {@code NotImplementedError}
     */
    public static void set_threshold(PyObject[] args, String[] kwargs) {
        throw Py.NotImplementedError("not applicable to Java GC");
    }

    /**
     * Not supported by Jython.
     *
     * @throws PyException {@code NotImplementedError}
     */
    public static PyObject get_threshold() {
        throw Py.NotImplementedError("not applicable to Java GC");
    }

    /**
     * Only works reliably if {@code monitorGlobal} is active, as it depends on
     * monitored objects to search for referrers. It only finds referrers that
     * properly implement the traverseproc mechanism (unless reflection-based
     * traversal is activated and works stable).
     *
     * @throws PyException {@code NotImplementedError}
     */
    public static PyObject get_objects() {
        if (!isMonitoring()) {
            throw Py.NotImplementedError(
                    "not applicable in Jython if gc module is not monitoring PyObjects");
        }
        ArrayList<PyObject> resultList = new ArrayList<>(monitoredObjects.size());
        synchronized (monitoredObjects) {
            for (WeakReferenceGC src: monitoredObjects) {
                PyObject obj = src.get();
                if (isTraversable(obj)) {
                    resultList.add(obj);
                }
            }
        }
        resultList.trimToSize();
        return new PyList(resultList);
    }

    /**
     * Only works reliably if {@code monitorGlobal} is active, as it depends on
     * monitored objects to search for referrers. It only finds referrers that
     * properly implement the traverseproc mechanism (unless reflection-based
     * traversal is activated and works stable).
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
                    src = (PyObject) src0.get(); /* Sentinels should not be in monitoredObjects */
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
     * implement the Traverseproc mechanism (unless reflection-based traversal
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
     * If a reference cycle is not entirely contained in {@code pool}, it will be entirely
     * contained in the resulting set, i.e. missing participants will be added.
     * This method completely operates on weak references to ensure that the returned
     * set does not manipulate gc behavior.
     *
     * Note that this method is not threadsafe. Within the gc module it is only used
     * by the collect method, which ensures threadsafety by a synchronized block.
     */
    private static IdentityHashMap<PyObject, WeakReferenceGC>
            removeNonCyclicWeakRefs(Iterable<WeakReferenceGC> pool) {
        @SuppressWarnings("unchecked")
        IdentityHashMap<PyObject, WeakReferenceGC>[] pools = new IdentityHashMap[2];

        pools[0] = new IdentityHashMap<PyObject, WeakReferenceGC>();
        pools[1] = new IdentityHashMap<PyObject, WeakReferenceGC>();
        PyObject referent;
        if (monitorNonTraversable) {
            /* this means there might be non-traversable objects in
             * the pool we must filter out now
             */
            for (WeakReferenceGC ref: pool) {
                referent = ref.get();
                if (referent != null && isTraversable(referent)) {
                    pools[0].put(referent, ref);
                }
            }
        } else {
            /* this means the pool is already entirely traversable */
            for (WeakReferenceGC ref: pool) {
                referent = ref.get();
                if (referent != null) {
                    pools[0].put(referent, ref);
                }
            }
        }
        IdentityHashMap<PyObject, WeakReferenceGC> tmp;
        IdentityHashMap<PyObject, WeakReferenceGC> toProcess = new IdentityHashMap<>();
        /* We complete pools[0] with all reachable objects. */
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
        /* pools[0] should now be a closed set in the sense that it contains all PyObjects
         * reachable from pools[0]. Now we are going to remove non-cyclic objects:
         */
        boolean done = false;
        while (!done) {
            done = true;
            /* After this loop pools[1] contains all objects from pools[0]
             * that some object in pools[0] points to.
             * toRemove will contain all objects from pools[0] that don't
             * point to any object in pools[0]. Removing toRemove from
             * pools[1] and repeating this procedure until nothing changes
             * any more will let only cyclic trash remain.
             */
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

        /* We complete pools[0] with all reachable objects.
         * Note the difference to the implementation in removeNonCyclic.
         * There pools[0] was initialized with the contents of pool and
         * then used here as iteration source. In contrast to that we don't
         * want to have pool contained in the reachable set in any case here.
         */
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
        /* pools[0] should now be a closed set in the sense that it contains
         * all PyObjects reachable from pools[0].
         */
        return pools[0].keySet();
    }

    /**
     * Returns all objects from {@code pool} that are part of reference cycles as a new set.
     * If a reference cycle is not entirely contained in {@code pool}, it will be entirely
     * contained in the resulting set, i.e. missing participants will be added.
     * This method completely operates on weak references to ensure that the returned
     * set does not manipulate gc behavior.
     *
     * Note that this method is not threadsafe. Within the gc module it is only used
     * by the collect method which ensures threadsafety by a synchronized block.
     */
    private static Set<PyObject> removeNonCyclic(Iterable<PyObject> pool) {
        @SuppressWarnings("unchecked")
        IdentityHashMap<PyObject, PyObject>[] pools = new IdentityHashMap[2];

        pools[0] = new IdentityHashMap<PyObject, PyObject>();
        pools[1] = new IdentityHashMap<PyObject, PyObject>();
        if (monitorNonTraversable) {
            /* this means there might be non-traversable objects in
             * the pool we must filter out now
             */
            for (PyObject obj: pool) {
                if (isTraversable(obj)) {
                    pools[0].put(obj, obj);
                }
            }
        } else {
            /* this means the pool is already entirely traversable */
            for (PyObject obj: pool) {
                pools[0].put(obj, obj);
            }
        }
        IdentityHashMap<PyObject, PyObject> tmp;
        IdentityHashMap<PyObject, PyObject> toProcess = new IdentityHashMap<>();

        /* We complete pools[0] with all reachable objects. */
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
        /* pools[0] now is a closed set in the sense that it contains all PyObjects
         * reachable from pools[0]. Now we are going to remove non-cyclic objects:
         */
        boolean done = false;
        while (!done) {
            done = true;
            /* After this loop pools[1] contains all objects from pools[0]
             * that some object in pools[0] points to.
             * toRemove will contain all objects from pools[0] that don't
             * point to any object in pools[0]. Removing toRemove from
             * pools[1] and repeating this procedure until nothing changes
             * any more will let only cyclic trash remain.
             */
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
        /* Search contains the cyclic objects that participate in a cycle with start,
         * i.e. which are reachable from start AND can reach start.
         * Mark these...
         */
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
        /* first determine the reachable set: */
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
        /* reachSearch[0] is now the reachable set, but still contains non-cyclic objects */
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
            /* move all objects that can reach start from reachSearch[0] to search */
            search.putAll(tmp);
            for (PyObject key: tmp.keySet()) {
                reachSearch[0].remove(key);
            }
            tmp.clear();
        }
        return search;
    }

    /**
     * Does its best to traverse the given {@link org.python.core.PyObject}
     * {@code ob}. It exploits both
     * {@link org.python.core.Traverseproc#traverse(Visitproc, Object)} and
     * {@link org.python.core.TraverseprocDerived#traverseDerived(Visitproc, Object)}.
     * If {@code ob} neither implements {@link org.python.core.Traverseproc} nor
     * {@link org.python.core.Traverseproc} and is not annotated with
     * {@link org.python.core.Untraversable}, reflection-based traversal via
     * {@link #traverseByReflection(Object, Visitproc, Object)} may be attempted
     * according to {@link #DONT_TRAVERSE_BY_REFLECTION}.
     *
     * @see org.python.core.Traverseproc#traverse(Visitproc, Object)
     * @see org.python.core.TraverseprocDerived#traverseDerived(Visitproc, Object)
     * @see #DONT_TRAVERSE_BY_REFLECTION
     * @see org.python.core.Untraversable
     * @see #traverseByReflection(Object, Visitproc, Object)
     */
    public static int traverse(PyObject ob, Visitproc visit, Object arg) {
        int retVal;
        boolean traversed = false;
        if (ob instanceof Traverseproc) {
            retVal = ((Traverseproc) ob).traverse(visit, arg);
            traversed = true;
            if (retVal != 0) {
                return retVal;
            }
        }
        if (ob instanceof TraverseprocDerived) {
            retVal = ((TraverseprocDerived) ob).traverseDerived(visit, arg);
            traversed = true;
            if (retVal != 0) {
                return retVal;
            }
        }
        boolean justAddedWarning = false;
        if ((gcFlags & SUPPRESS_TRAVERSE_BY_REFLECTION_WARNING) == 0) {
            if (! (ob instanceof Traverseproc || ob instanceof TraverseprocDerived ||
                    ob.getClass() == PyObject.class ||
                    ob.getClass().isAnnotationPresent(Untraversable.class)) ) {
                if (((gcFlags & INSTANCE_TRAVERSE_BY_REFLECTION_WARNING) != 0) ||
                        reflectionWarnedClasses == null ||
                        !reflectionWarnedClasses.contains(ob.getClass())) {
                    if ((gcFlags & INSTANCE_TRAVERSE_BY_REFLECTION_WARNING) == 0) {
                        if (reflectionWarnedClasses == null) {
                            reflectionWarnedClasses = new HashSet<>();
                        }
                        reflectionWarnedClasses.add(ob.getClass());
                        justAddedWarning = true;
                    }
                    Py.writeWarning("gc", "The PyObject subclass "+ob.getClass().getName()+"\n" +
                            "should either implement Traverseproc or be marked with the\n" +
                            "@Untraversable annotation. See instructions in\n" +
                            "javadoc of org.python.core.Traverseproc.java.");
                }
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
            if (((gcFlags & INSTANCE_TRAVERSE_BY_REFLECTION_WARNING) != 0) ||
                    justAddedWarning ||
                    reflectionWarnedClasses == null ||
                    !reflectionWarnedClasses.contains(ob.getClass())) {
                if ((gcFlags & INSTANCE_TRAVERSE_BY_REFLECTION_WARNING) == 0 &&
                        !justAddedWarning) {
                    if (reflectionWarnedClasses == null) {
                        reflectionWarnedClasses = new HashSet<>();
                    }
                    reflectionWarnedClasses.add(ob.getClass());
                }
                Py.writeWarning("gc", "Traverse by reflection: "+ob.getClass().getName()+"\n" +
                        "This is an inefficient procedure. It is recommended to\n" +
                        "implement the traverseproc mechanism properly.");
            }
        }
        return traverseByReflection(ob, visit, arg);
    }

    /**
     * <p>
     * This method recursively traverses fields of {@code ob}.
     * If a field is a PyObject, it is passed to {@code visit}.
     * and recursion ends in that branch.
     * If a field is an array, the elements are checked whether
     * they are PyObjects. {@code PyObject} elements are passed to
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
        int result = 0;
        Object element;
        if (cls.isArray() && canLinkToPyObject(cls.getComponentType(), false)) {
            for (int i = 0; i < Array.getLength(ob); ++i) {
                element = Array.get(ob, i);
                if (element != null) {
                    if (element instanceof PyObject) {
                        result = visit.visit((PyObject) element, arg);
                    } else if (!alreadyTraversed.containsKey(element)) {
                        alreadyTraversed.put(element, element);
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
                                if (element != null) {
                                    if (element instanceof PyObject) {
                                        result = visit.visit((PyObject) element, arg);
                                    } else if (!alreadyTraversed.containsKey(element)) {
                                        alreadyTraversed.put(element, element);
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
     * For now, no generic type info is exploited.
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
     * was obtained as a field type or component type of an array.
     * </p>
     */
    public static boolean canLinkToPyObject(Class<?> cls, boolean actual) {
        /* At first some quick (fail fast/succeed fast) checks: */
        if (quickCheckCannotLinkToPyObject(cls)) {
            return false;
        }
        if (!actual && (!Modifier.isFinal(cls.getModifiers()))) {
            return true; /* a subclass could contain anything */
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

        /* Fail fast if no fields exist in cls: */
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

//--------------Visitproc section----------------------------------------------

    private static class ReferentsFinder implements Visitproc {
        public static ReferentsFinder defaultInstance = new ReferentsFinder();

        /**
         * Expects arg to be a list-like {@code PyObject} where the
         * referents will be inserted.
         */
        @Override
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
    private static class ReachableFinder implements Visitproc {
        public static ReachableFinder defaultInstance = new ReachableFinder();

        /**
         * Expects arg to be a list-like {@code PyObject} where the
         * referents will be inserted.
         */
        @Override
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

    private static class ReachableFinderWeakRefs implements Visitproc {
        public static ReachableFinderWeakRefs defaultInstance = new ReachableFinderWeakRefs();

        @Override
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

    private static class ReferrerFinder implements Visitproc {
        public static ReferrerFinder defaultInstance = new ReferrerFinder();

        /**
         * Expects {@code arg} to be a 2-component array ({@code PyObject[]})
         * consisting of the {@code PyObject} to be referred to at
         * {@code arg[0]} and the destination list (a list-like {@code PyObject}
         * where the referrers will be inserted) at {@code arg[1]}.
         */
        @Override
        public int visit(PyObject object, Object arg) {
            if (((PyObject[]) arg)[0].__eq__(object).__nonzero__()) {
                ((org.python.core.PySequenceList) ((PyObject[]) arg)[1]).pyadd(object);
            }
            return 0;
        }
    }

    /**
     * Like {@code RefInListFinder} this visitproc looks whether the traversed object
     * refers to one of the objects in a given set. Here we perform fail fast
     * behavior. This method is useful if one is not interested in the referrers,
     * but only wants to know (quickly) whether a connection exists or not.
     */
    private static class RefersToSetFinder implements Visitproc {
        public static RefersToSetFinder defaultInstance = new RefersToSetFinder();

        @Override
        @SuppressWarnings("unchecked")
        public int visit(PyObject object, Object arg) {
            return ((Set<PyObject>) arg).contains(object) ? 1 : 0;
        }
    }

    /**
     * This visitproc looks whether an object refers to one of the objects in
     * a given set.<br>
     * {@code arg} must be a 2-component array of
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
    private static class RefInListFinder implements Visitproc {
        public static RefInListFinder defaultInstance = new RefInListFinder();
        public boolean found = false;

        /**
         * Expects {@code arg} to be a 2-component array of
         * {@link java.util.Map}s.
         */
        @Override
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

    private static class ObjectInListFinder implements Visitproc {
        public static ObjectInListFinder defaultInstance = new ObjectInListFinder();
        public boolean found = false;

        /**
         * Expects {@code arg} to be a 2-component array of
         * {@link java.util.Map}s.
         */
        @Override
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
//--------------end of Visitproc section---------------------------------------
}
