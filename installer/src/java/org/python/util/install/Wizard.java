package org.python.util.install;

import java.awt.Dimension;
import java.awt.Rectangle;

import javax.swing.JOptionPane;

import org.python.util.install.driver.Autotest;

public class Wizard extends AbstractWizard implements TextKeys {

    public Wizard(JarInfo jarInfo, Autotest autotest) {
        super();

        setTitle(getText(JYTHON_INSTALL));

        LanguagePage languagePage = new LanguagePage(jarInfo);
        LicensePage licensePage = new LicensePage(jarInfo);
        licensePage.setValidator(new LicensePageValidator(licensePage));
        TypePage typePage = new TypePage();
        DirectorySelectionPage directoryPage = new DirectorySelectionPage(jarInfo);
        directoryPage.setValidator(new DirectorySelectionPageValidator(directoryPage));
        // 2.7.0rc2 removed JAVA_HOME support from script generation,
        // due to jython.exe. May consider re-enabling if we have a good
        // strategy for doing so.
//        JavaSelectionPage javaPage = new JavaSelectionPage();
//        javaPage.setValidator(new JavaSelectionPageValidator(javaPage));
        OverviewPage overviewPage = new OverviewPage();
        ProgressPage progressPage = new ProgressPage(jarInfo, autotest);
        ReadmePage readmePage = new ReadmePage(jarInfo);
        SuccessPage successPage = new SuccessPage(jarInfo);

        this.addPage(languagePage);
        this.addPage(licensePage);
        this.addPage(typePage);
        this.addPage(directoryPage);
//        this.addPage(javaPage);
        this.addPage(overviewPage);
        this.addPage(progressPage);
        this.addPage(readmePage);
        this.addPage(successPage);

        setSize(720, 540);
        centerOnScreen();
        validate();
    }

    protected boolean finish() {
        return true;
    }

    protected String getCancelString() {
        return getText(CANCEL);
    }

    protected String getFinishString() {
        return getText(FINISH);
    }

    protected String getNextString() {
        return getText(NEXT);
    }

    protected String getPreviousString() {
        return getText(PREVIOUS);
    }

    public void validationStarted(ValidationEvent event) {
    }

    public void validationFailed(ValidationEvent event, ValidationException exception) {
        JOptionPane.showMessageDialog(this, exception.getMessage(), getText(TextKeys.ERROR), JOptionPane.ERROR_MESSAGE);
    }

    public void validationInformationRequired(ValidationEvent event, ValidationInformationException exception) {
        JOptionPane.showMessageDialog(this, exception.getMessage(), getText(INFORMATION),
                JOptionPane.INFORMATION_MESSAGE);
    }

    public void validationSucceeded(ValidationEvent event) {
    }

    private final String getText(String textKey) {
        return Installation.getText(textKey);
    }

    private void centerOnScreen() {
        Dimension dim = getToolkit().getScreenSize();
        Rectangle rectBounds = getBounds();
        setLocation((dim.width - rectBounds.width) / 2, (dim.height - rectBounds.height) / 2);
    }

}
