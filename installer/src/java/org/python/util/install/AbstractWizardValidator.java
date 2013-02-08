package org.python.util.install;

import java.util.ArrayList;
import java.util.Iterator;

public abstract class AbstractWizardValidator implements TextKeys {

    /**
     * The thread that performs the validation
     */
    private class ValidatorThread extends Thread {
        public final void run() {
            try {
                fireValidationStarted();
                validate();
                fireValidationSucceeded();
            } catch (ValidationException e) {
                fireValidationFailed(e);
            } catch (ValidationInformationException vie) {
                fireValidationInformationRequired(vie);
            }
        }
    }

    private ArrayList _listeners;
    private AbstractWizardPage _page;
    private Thread _validatorThread;

    public final void addValidationListener(ValidationListener listener) {
        if (listener == null)
            return;
        if (_listeners == null)
            _listeners = new ArrayList(5);
        if (_listeners.contains(listener))
            return;
        _listeners.add(listener);
    }

    private void fireValidationFailed(ValidationException exception) {
        if (getWizard() != null)
            getWizard().unlock();
        if (_listeners == null || _listeners.isEmpty())
            return;
        ValidationEvent event = new ValidationEvent(_page);
        for (Iterator it = _listeners.iterator(); it.hasNext();) {
            ValidationListener listener = (ValidationListener) it.next();
            listener.validationFailed(event, exception);
        }
    }

    private void fireValidationInformationRequired(ValidationInformationException exception) {
        if (getWizard() != null)
            getWizard().unlock();
        if (_listeners == null || _listeners.isEmpty())
            return;
        ValidationEvent event = new ValidationEvent(_page);
        for (Iterator it = _listeners.iterator(); it.hasNext();) {
            ValidationListener listener = (ValidationListener) it.next();
            listener.validationInformationRequired(event, exception);
        }
    }

    private void fireValidationStarted() {
        if (getWizard() != null)
            getWizard().lock();
        if (_listeners == null || _listeners.isEmpty())
            return;
        ValidationEvent event = new ValidationEvent(_page);
        for (Iterator it = _listeners.iterator(); it.hasNext();) {
            ValidationListener listener = (ValidationListener) it.next();
            listener.validationStarted(event);
        }
    }

    private void fireValidationSucceeded() {
        if (getWizard() != null) {
            getWizard().unlock();
            getWizard().gotoNextPage();
        }
        if (_listeners == null || _listeners.isEmpty())
            return;
        ValidationEvent event = new ValidationEvent(_page);
        for (Iterator it = _listeners.iterator(); it.hasNext();) {
            ValidationListener listener = (ValidationListener) it.next();
            listener.validationSucceeded(event);
        }
    }

    protected final AbstractWizard getWizard() {
        if (_page != null) {
            return _page.getWizard();
        } else {
            return null;
        }
    }

    protected final AbstractWizardPage getWizardPage() {
        return _page;
    }

    public final void removeValidationListener(ValidationListener listener) {
        if (listener == null || _listeners == null || !_listeners.contains(listener))
            return;
        _listeners.remove(listener);
    }

    public final void setWizardPage(AbstractWizardPage page) {
        this._page = page;
    }

    public final void start() {
        _validatorThread = new ValidatorThread();
        _validatorThread.start();
    }

    /**
     * perform the actual validation
     * 
     * @throws ValidationException when the validation failed
     * @throws ValidationInformationException when an information should be displayed
     */
    protected abstract void validate() throws ValidationException, ValidationInformationException;

    final String getText(String textKey) {
        return Installation.getText(textKey);
    }

    final String getText(String textKey, String parameter0) {
        return Installation.getText(textKey, parameter0);
    }

}