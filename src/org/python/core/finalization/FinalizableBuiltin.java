package org.python.core.finalization;

/**
 * See documentation of {@link FinalizablePyObject}.
 */

public interface FinalizableBuiltin {
    /**
     * {@link #__del_builtin__()} is the built-in's own finalizer, while
     * {@link FinalizablePyObjectDerived#__del_derived__()} refers to an
     * instance's in-dict {@code __del__}.
     * A FinalizeTrigger calls {@link FinalizablePyObjectDerived#__del_derived__()}
     * first and - if existent - {@link #__del_builtin__()} after that. A plain
     * {@link FinalizablePyObject#__del__()}
     * would behave as overridden by
     * {@link FinalizablePyObjectDerived#__del_derived__()}, i.e. won't be called
     * if the type implements {@link FinalizablePyObjectDerived}, while
     * {@link #__del_builtin__()} is called in any case.
     */
    public void __del_builtin__();
}
