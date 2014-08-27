package org.python.core.finalization;

/**
 * See documentation of {@link FinalizablePyObject}.
 */

public interface FinalizableBuiltin extends HasFinalizeTrigger {
	/**
	 * {@code __del_builtin__} is the built-in's own finalizer, while
	 * {@code __del_derived__} refers to an instance's in-dict {@code __del__}.
	 * A FinalizeTrigger calls {@code __del_derived__} first and
     * - if existent - {@code __del_builtin__} after that. A plain {@code __del__}
     * would behave as overwritten by {@code __del__Derived}, i.e. won't be called
     * if the type implements {@code FinalizablePyObjectDerived} while
     * {@code __del_builtin__} is called in any case.
	 */
	public void __del_builtin__();
}
