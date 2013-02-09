package org.python.util.install;

import java.util.EventObject;

public class ValidationEvent extends EventObject {

    public ValidationEvent(AbstractWizardPage source) {
        super(source);
    }

    public AbstractWizardPage getWizardPage() {
        return (AbstractWizardPage) source;
    }
}