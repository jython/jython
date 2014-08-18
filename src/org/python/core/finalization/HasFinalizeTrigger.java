package org.python.core.finalization;

/**
 * This is a pure marker-interface to indicate that a
 * {@link org.python.core.PyObject} has a field declaration
 * {@code FinalizeTrigger finalizeTrigger;}
 * and thus can be treated by Jython's finalization API.
 * 
 * For detailed instructions how to use finalizers in Jython, see
 * {@link org.python.core.finalization.FinalizablePyObject}.
 */
public interface HasFinalizeTrigger {
}
