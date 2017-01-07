package org.python.core.finalization;

/**
 * This interface should never be used directly in any hand-written code
 * (except in FinalizeTrigger.java).
 * It should only appear in automatically generated {@code fooDerived}-classes.
 * 
 * To use finalizers in hand-written classes read the instructions at
 * {@link org.python.core.finalization.FinalizablePyObject}.
 *
 */
public interface FinalizablePyObjectDerived {
    
    /**
     * {@code __del_builtin__} is the built-in's own finalizer, while
     * {@code __del_derived__} refers to an instance's in-dict {@code __del__}.
     * A FinalizeTrigger calls {@code __del_derived__} first and
     * - if existent - {@code __del_builtin__} after that. A plain {@code __del__}
     * would behave as overridden by {@code __del_derived__}, i.e. won't be called
     * if the type implements {@code FinalizablePyObjectDerived} while
     * {@code __del_builtin__} is called in any case.
     */
    public void __del_derived__();
}
