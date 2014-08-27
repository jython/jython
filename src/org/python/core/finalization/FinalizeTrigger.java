package org.python.core.finalization;

import java.lang.reflect.Field;

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

    /**
     * Recreates the {@code FinalizeTrigger} of the given object. This makes sure that
     * once the resurrected object is gc'ed again, its {@code __del__}-method will be
     * called again.
     */
    public static void ensureFinalizer(HasFinalizeTrigger resurrect) {
    	FinalizeTrigger trigger = makeTrigger(resurrect);
    	setFinalizeTrigger(resurrect, trigger);
    }

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
        		((FinalizablePyObjectDerived) toFinalize).__del_derived__();
        	} else if (toFinalize instanceof FinalizablePyObject) {
        		((FinalizablePyObject) toFinalize).__del__();
        	}
        	if (toFinalize instanceof FinalizableBuiltin) {
        		((FinalizableBuiltin) toFinalize).__del_builtin__();
        	}
        }
    }
}
