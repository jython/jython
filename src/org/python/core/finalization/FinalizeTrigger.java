package org.python.core.finalization;

import org.python.core.PyObject;
import org.python.core.JyAttribute;
import org.python.core.PySystemState;
import org.python.modules.gc;

/**
 * To use finalizers on {@code PyObject}s, read the documentation of
 * {@link org.python.core.finalization.FinalizablePyObject}.
 */
public class FinalizeTrigger {
    /**
     * This flag tells the finalize trigger to call
     * {@link gc#notifyFinalize(PyObject)} after it called the finalizer.
     */
    public static final byte NOTIFY_GC_FLAG =           (1<<0);

    /**
     * Indicates that the underlying PyObject was never intended to be finalized.
     * It is actually not finalizable and the trigger only exists to notify
     * {@link org.python.modules.gc} that the underlying object was finalized.
     * This is needed for some advanced gc-functionality.
     */
    public static final byte NOT_FINALIZABLE_FLAG = (1<<3);

    /**
     * Indicates that only
     * {@link org.python.core.finalization.FinalizableBuiltin}
     * shall be called.
     */
    public static final byte ONLY_BUILTIN_FLAG = (1<<4);

    /**
     * Indicates that this trigger was already finalized.
     */
    public static final byte FINALIZED_FLAG = (1<<5);

    /**
     * This factory hook is reserved for use by JyNI.
     * It allows to replace the default {@code FinalizeTrigger}.
     * JyNI needs it to support garbage collection.
     */
    public static FinalizeTriggerFactory factory;

    public static FinalizeTrigger makeTrigger(PyObject toFinalize) {
        if (factory != null) {
            return factory.makeTrigger(toFinalize);
        } else {
            return new FinalizeTrigger(toFinalize);
        }
    }

    public static boolean hasActiveTrigger(PyObject obj) {
        Object fn = JyAttribute.getAttr(obj, JyAttribute.FINALIZE_TRIGGER_ATTR);
        return fn != null && ((FinalizeTrigger) fn).isActive();
    }

    public static boolean isFinalizable(PyObject obj) {
        return obj instanceof FinalizablePyObject || obj instanceof FinalizableBuiltin
                || obj instanceof FinalizablePyObjectDerived;
    }

    /**
     * Recreates the {@code FinalizeTrigger} of the given object. This makes sure that
     * once the resurrected object is gc'ed again, its {@code __del__}-method will be
     * called again.
     */
    public static void ensureFinalizer(PyObject resurrect) {
        JyAttribute.setAttr(resurrect, JyAttribute.FINALIZE_TRIGGER_ATTR,
            makeTrigger(resurrect));
    }

    public static void runFinalizer(PyObject toFinalize) {
        runFinalizer(toFinalize, false);
    }

    public static void runFinalizer(PyObject toFinalize, boolean runBuiltinOnly) {
        if (!runBuiltinOnly) {
            if (toFinalize instanceof FinalizablePyObjectDerived) {
                try {
                    ((FinalizablePyObjectDerived) toFinalize).__del_derived__();
                } catch (Exception e) {
                }
            } else if (toFinalize instanceof FinalizablePyObject) {
                try {
                    ((FinalizablePyObject) toFinalize).__del__();
                } catch (Exception e) {
                }
            }
        }
        if (toFinalize instanceof FinalizableBuiltin) {
            try {
                ((FinalizableBuiltin) toFinalize).__del_builtin__();
            } catch (Exception e) {
            }
        }
    }

    public static void appendFinalizeTriggerForBuiltin(PyObject obj) {
        if (obj instanceof FinalizableBuiltin) {
            FinalizeTrigger ft = makeTrigger(obj);
            ft.flags = ONLY_BUILTIN_FLAG;
            JyAttribute.setAttr(obj, JyAttribute.FINALIZE_TRIGGER_ATTR, ft);
        } else {
            JyAttribute.delAttr(obj, JyAttribute.FINALIZE_TRIGGER_ATTR);
        }
    }

    protected PyObject toFinalize;
    public byte flags = 0;

    public void clear() {
        toFinalize = null;
    }

    public void trigger(PyObject toFinalize)
    {
        this.toFinalize = toFinalize;
    }

    public boolean isActive() {
        return toFinalize != null;
    }

    protected FinalizeTrigger(PyObject toFinalize) {
        this.toFinalize = toFinalize;
    }

    protected boolean isCyclic() {
        gc.CycleMarkAttr cm = (gc.CycleMarkAttr)
                JyAttribute.getAttr(toFinalize, JyAttribute.GC_CYCLE_MARK_ATTR);
        if (cm != null && cm.isCyclic()) {
            return true;
        } else {
            gc.markCyclicObjects(toFinalize, (flags & NOT_FINALIZABLE_FLAG) == 0);
            cm = (gc.CycleMarkAttr)
                    JyAttribute.getAttr(toFinalize, JyAttribute.GC_CYCLE_MARK_ATTR);
            return cm != null && cm.isCyclic();
        }
    }

    protected boolean isUncollectable() {
        gc.CycleMarkAttr cm = (gc.CycleMarkAttr)
                JyAttribute.getAttr(toFinalize, JyAttribute.GC_CYCLE_MARK_ATTR);
        if (cm != null && cm.isUncollectable()) {
            return true;
        } else {
            gc.markCyclicObjects(toFinalize, (flags & NOT_FINALIZABLE_FLAG) == 0);
            cm = (gc.CycleMarkAttr)
                    JyAttribute.getAttr(toFinalize, JyAttribute.GC_CYCLE_MARK_ATTR);
            return cm != null && cm.isUncollectable();
        }
    }

    public void performFinalization() {
        if (toFinalize != null) {
            byte saveGarbage = 0;
            if ((gc.getJythonGCFlags() & gc.DONT_FINALIZE_CYCLIC_GARBAGE) != 0) {
                if (isUncollectable()) {
                    saveGarbage = 1;
                } else if (!isCyclic()) {
                    saveGarbage = -1;
                    runFinalizer(toFinalize, (flags & ONLY_BUILTIN_FLAG) != 0);
                }
            } else {
                if ((flags & NOT_FINALIZABLE_FLAG) == 0) {
                    runFinalizer(toFinalize, (flags & ONLY_BUILTIN_FLAG) != 0);
                }
            }
            if ((gc.getJythonGCFlags() & gc.VERBOSE_FINALIZE) != 0) {
                gc.writeDebug("gc", "finalization of "+toFinalize);
            }
            if (saveGarbage == 1 || (saveGarbage == 0 &&
                    (gc.get_debug() & gc.DEBUG_SAVEALL) != 0 && isCyclic())) {
                if ((flags & NOT_FINALIZABLE_FLAG) == 0) {
                    //Finalizable objects in gc.garbage get a special FinalizeTrigger
                    //that only runs the builtin finalizer. This is needed because
                    //from Python the user can't call the builtin-part of the
                    //finalizer by hand.
                    appendFinalizeTriggerForBuiltin(toFinalize);
                }
                gc.garbage.add(toFinalize);
                if ((gc.getJythonGCFlags() & gc.VERBOSE_FINALIZE) != 0) {
                    gc.writeDebug("gc", toFinalize+" added to garbage.");
                }
            }
        }
        if ((flags & NOTIFY_GC_FLAG) != 0) {
            if ((gc.getJythonGCFlags() & gc.VERBOSE_FINALIZE) != 0) {
                gc.writeDebug("gc", "notify finalization of "+toFinalize);
            }
            gc.notifyFinalize(toFinalize);
            flags &= ~NOTIFY_GC_FLAG;
        }
    }

    protected void finalize() throws Throwable {
        flags |= FINALIZED_FLAG;
        gc.notifyPreFinalization();
        if (gc.delayedFinalizationEnabled() && toFinalize != null) {
            if ((gc.getJythonGCFlags() & gc.VERBOSE_FINALIZE) != 0) {
                gc.writeDebug("gc", "delayed finalization for "+toFinalize);
            }
            gc.registerForDelayedFinalization(toFinalize);
        } else {
            performFinalization();
        }
        gc.notifyPostFinalization();
    }

    public boolean isFinalized() {
        return (flags & FINALIZED_FLAG) != 0;
    }
}
