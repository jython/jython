package org.python.util.install;

import java.io.File;

public class DirectorySelectionPageValidator extends AbstractWizardValidator {

    DirectorySelectionPage _page;

    DirectorySelectionPageValidator(DirectorySelectionPage page) {
        super();
        _page = page;
    }

    protected void validate() throws ValidationException, ValidationInformationException {
        String directory = _page.getDirectory().getText().trim(); // trim to be sure
        if (directory != null && directory.length() > 0) {
            File targetDirectory = new File(directory);
            if (targetDirectory.exists()) {
                if (targetDirectory.isDirectory()) {
                    if (targetDirectory.list().length > 0) {
                        throw new ValidationException(getText(NON_EMPTY_TARGET_DIRECTORY));
                    }
                }
            } else {
                if (targetDirectory.mkdirs()) {
                    throw new ValidationInformationException(Installation.getText(CREATED_DIRECTORY, directory));
                } else {
                    throw new ValidationException(getText(UNABLE_CREATE_DIRECTORY, directory));
                }
            }
        } else {
            throw new ValidationException(getText(EMPTY_TARGET_DIRECTORY));
        }
        FrameInstaller.setTargetDirectory(directory);
    }
}