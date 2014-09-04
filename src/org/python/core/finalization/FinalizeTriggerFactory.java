package org.python.core.finalization;

/**
 * Reserved for use by JyNI.
 */
public interface FinalizeTriggerFactory {

    public FinalizeTrigger makeTrigger(HasFinalizeTrigger toFinalize);
}
