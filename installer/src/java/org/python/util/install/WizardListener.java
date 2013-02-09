package org.python.util.install;

public interface WizardListener {
    public void wizardCancelled(WizardEvent event);

    public void wizardFinished(WizardEvent event);

    public void wizardNext(WizardEvent event);

    public void wizardPrevious(WizardEvent event);

    public void wizardStarted(WizardEvent event);
}