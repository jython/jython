package org.python.util.install;

import org.python.util.install.Installation.JavaVersionInfo;

public class JavaSelectionPageValidator extends AbstractWizardValidator {

    JavaSelectionPage _page;

    JavaSelectionPageValidator(JavaSelectionPage page) {
        super();
        _page = page;
    }

    protected void validate() throws ValidationException {
        JavaVersionInfo javaVersionInfo = new JavaVersionInfo();
        String directory = _page.getJavaHome().getText().trim(); // trim to be sure
        JavaHomeHandler javaHomeHandler = new JavaHomeHandler(directory);
        if(javaHomeHandler.isDeviation()) {
            javaVersionInfo = Installation.getExternalJavaVersion(javaHomeHandler);
            if (javaVersionInfo.getErrorCode() != Installation.NORMAL_RETURN) {
                throw new ValidationException(javaVersionInfo.getReason());
            }
        } else {
            // no experiments if current java is selected
            Installation.fillJavaVersionInfo(javaVersionInfo, System.getProperties());
        }
        FrameInstaller.setJavaHomeHandler(javaHomeHandler);
        FrameInstaller.setJavaVersionInfo(javaVersionInfo);
    }

}
