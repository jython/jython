package org.python.core.finalization;

import java.lang.reflect.Field;
import java.lang.ref.WeakReference;
import java.lang.ref.SoftReference;
import java.lang.ref.Reference;
import org.python.core.PyObject;

/**
 * To use finalizers on {@code PyObject}s, read the documentation of
 * {@link org.python.core.finalization.FinalizablePyObject}.
 */
public class FinalizeTrigger {

    /**
     * This optional factory hook allows to replace the
     * default {@code FinalizeTrigger}. It is f.i. needed by JyNI.
     */
    public static FinalizeTriggerFactory factory;

    public static FinalizeTrigger makeTrigger(HasFinalizeTrigger toFinalize) {
        if (factory != null) {
            return factory.makeTrigger(toFinalize);
        } else {
            return new FinalizeTrigger(toFinalize);
        }
    }

    /*
    public static FinalizeTrigger makeTriggerDerived(FinalizablePyObjectDerived toFinalize) {
        if (factory != null) {
            return factory.makeTriggerDerived(toFinalize);
        } else {
            return new FinalizeTriggerDerived(toFinalize);
        }
    }
    */

    /**
     * Recreates the {@code FinalizeTrigger} of the given object. This makes sure that
     * once the resurrected object is gc'ed again, its {@code __del__}-method will be
     * called again.
     */
    public static void ensureFinalizer(HasFinalizeTrigger resurrect) {
    	FinalizeTrigger trigger = makeTrigger(resurrect);
    	setFinalizeTrigger(resurrect, trigger);
    }

    /*
    public static void ensureFinalizerDerived(FinalizablePyObjectDerived resurrect) {
        setFinalizeTrigger(resurrect, makeTriggerDerived(resurrect));
    }
    */

    public static void setFinalizeTrigger(HasFinalizeTrigger toFinalize, FinalizeTrigger trigger) {
        Field triggerField;
        try {
            triggerField = toFinalize.getClass().getDeclaredField("finalizeTrigger");
        } catch (NoSuchFieldException nfe) {
            throw new IllegalArgumentException(toFinalize.getClass()+" must have a field finalizeTrigger.");
        }
        try {
            triggerField.set(toFinalize, trigger);
        } catch (IllegalAccessException iae) {
            try {
                triggerField.setAccessible(true);
                triggerField.set(toFinalize, trigger);
            } catch (Exception e) {
                throw new IllegalArgumentException("finalizeTrigger in "+toFinalize.getClass()+" must be accessible.");
            }
        }
    }

    public static FinalizeTrigger getFinalizeTrigger(HasFinalizeTrigger toFinalize) {
        Field triggerField;
        try {
            triggerField = toFinalize.getClass().getDeclaredField("finalizeTrigger");
        } catch (NoSuchFieldException nfe) {
            throw new IllegalArgumentException(toFinalize.getClass()+" must have a field finalizeTrigger.");
        }
        try {
            return (FinalizeTrigger) triggerField.get(toFinalize);
        } catch (IllegalAccessException iae) {
            try {
                triggerField.setAccessible(true);
                return (FinalizeTrigger) triggerField.get(toFinalize);
            } catch (Exception e) {
                throw new IllegalArgumentException("finalizeTrigger in "+toFinalize.getClass()+" must be accessible.");
            }
        }
    }


    protected HasFinalizeTrigger toFinalize;
    public void clear() {
        toFinalize = null;
    }

    public void trigger(HasFinalizeTrigger toFinalize)
    {
        this.toFinalize = toFinalize;
    }

    protected FinalizeTrigger(HasFinalizeTrigger toFinalize) {
        this.toFinalize = toFinalize;
    }

    protected void finalize() throws Throwable {
        if (toFinalize != null) {
        	if (toFinalize instanceof FinalizablePyObjectDerived) {
        		((FinalizablePyObjectDerived) toFinalize).__del__Derived();
        	} else if (toFinalize instanceof FinalizablePyObject) {
        		((FinalizablePyObject) toFinalize).__del__();
        	}
        	if (toFinalize instanceof FinalizableBuiltin) {
        		((FinalizableBuiltin) toFinalize).__del__Builtin();
        	}
        }
    }


    /*
     * A FinalizeTrigger variant that only calls __del__Derived, but not the
     * built-in's finalizer __del__. It can be used to control finalization
     * behavior of resurrected objects in more detail.
     */
    /*protected static class FinalizeTriggerDerived extends FinalizeTrigger {
    	protected FinalizeTriggerDerived(FinalizablePyObjectDerived toFinalize) {
            super(toFinalize);
        }

        protected void finalize() throws Throwable {
            if (toFinalize != null) {
            	((FinalizablePyObjectDerived) toFinalize).__del__Derived();
            }
        }
    }*/
}
