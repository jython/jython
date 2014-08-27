package org.python.core.finalization;

import org.python.core.PyObject;;

/**
 * For detailed intructions how to use finalizers on PyObjects,
 * read the documentation of {@link org.python.core.finalization.FinalizablePyObject}.
 */
public abstract class PyFinalizableObject extends PyObject implements FinalizablePyObject {
    
    public FinalizeTrigger finalizeTrigger;
    
    public PyFinalizableObject() {
        super();
        finalizeTrigger = FinalizeTrigger.makeTrigger(this);
    }
}
