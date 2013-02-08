package org.python.util.install;

import java.util.EventObject;

public class WizardEvent extends EventObject {
    public WizardEvent(AbstractWizard source) {
        super(source);
    }

    public AbstractWizard getWizard() {
        return (AbstractWizard) source;
    }
}