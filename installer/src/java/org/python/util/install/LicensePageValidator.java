package org.python.util.install;

public class LicensePageValidator extends AbstractWizardValidator {

    LicensePage _page;

    LicensePageValidator(LicensePage page) {
        super();
        _page = page;
    }

    protected void validate() throws ValidationException {
        if (!_page.isAccept()) {
            throw new ValidationException(getText(PLEASE_ACCEPT_LICENSE));
        }
    }
}