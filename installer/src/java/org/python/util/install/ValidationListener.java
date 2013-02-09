package org.python.util.install;

public interface ValidationListener {
    public void validationFailed(ValidationEvent event, ValidationException exception);

    public void validationInformationRequired(ValidationEvent event, ValidationInformationException exception);

    public void validationStarted(ValidationEvent event);

    public void validationSucceeded(ValidationEvent event);
}