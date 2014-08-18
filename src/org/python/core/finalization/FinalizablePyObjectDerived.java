package org.python.core.finalization;

/**
 * This interface should never be used directly in any hand-written code.
 * It should only appear in automatically generated {@code fooDerived}-classes.
 * 
 * To use finalizers in hand-written classes read the instructions at
 * {@link org.python.core.finalization.FinalizablePyObject}.
 *
 */
public interface FinalizablePyObjectDerived extends HasFinalizeTrigger {
	
	/**
	 * {@code __del__Builtin} is the built-in's own finalizer, while
	 * {@code __del__Derived} refers to an instance's in-dict {@code __del__}.
	 * A FinalizeTrigger calls {@code __del__Derived} first and
     * - if existent - {@code __del__Builtin} after that. A plain {@code __del__}
     * would behave as overwritten by {@code __del__Derived}, i.e. won't be called
     * if the type implements {@code FinalizablePyObjectDerived} while
     * {@code __del__Builtin} is called in any case.
	 */
	public void __del__Derived();
}
