package org.python.core.finalization;

public interface FinalizeTriggerFactory {
    
	public FinalizeTrigger makeTrigger(HasFinalizeTrigger toFinalize);
}
