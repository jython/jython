package org.python.util.install;

import java.awt.BorderLayout;
import java.io.File;
import java.io.IOException;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

import org.python.util.install.driver.Autotest;

public class ProgressPage extends AbstractWizardPage implements ProgressListener {

    private static final long serialVersionUID = 9013748834030994976L;

    private JarInfo _jarInfo;
    private JLabel _label;
    private JProgressBar _progressBar;
    private JLabel _progressEntry;
    private Autotest _autotest;

    public ProgressPage(JarInfo jarInfo, Autotest autotest) {
        super();
        _jarInfo = jarInfo;
        _autotest = autotest;
        initComponents();
    }

    private void initComponents() {
        JPanel northPanel = new JPanel();
        _label = new JLabel();
        northPanel.add(_label);
        _progressBar = new JProgressBar();
        northPanel.add(_progressBar);
        JPanel centerPanel = new JPanel();
        _progressEntry = new JLabel();
        centerPanel.add(_progressEntry);
        setLayout(new BorderLayout(0, 5));
        add(northPanel, BorderLayout.NORTH);
        add(centerPanel, BorderLayout.CENTER);
    }

    protected String getTitle() {
        return getText(INSTALLATION_IN_PROGRESS);
    }

    protected String getDescription() {
        return getText(PLEASE_WAIT);
    }

    protected boolean isCancelVisible() {
        return true;
    }

    protected boolean isPreviousVisible() {
        return false;
    }

    protected boolean isNextVisible() {
        return false;
    }

    protected JComponent getFocusField() {
        return null;
    }

    protected void activate() {
        _label.setText(getText(PROGRESS) + ": ");
        _progressBar.setValue(0);
        _progressBar.setStringPainted(true);
        try {
            _progressEntry.setText(getText(INFLATING, _jarInfo.getJarFile().getName()));
        } catch (IOException e) {
            // should not happen
        }
        JarInstaller jarInstaller = new JarInstaller(this, _jarInfo);
        if (_autotest != null) {
            jarInstaller.addInstallationListener(_autotest);
        }
        File targetDirectory = new File(FrameInstaller.getTargetDirectory());
        JavaHomeHandler javaHomeHandler = FrameInstaller.getJavaHomeHandler();
        jarInstaller.inflate(targetDirectory, FrameInstaller.getInstallationType(), javaHomeHandler);
    }

    protected void passivate() {
    }

    protected void beforeValidate() {
    }

    //
    // interface ProgressListener
    //

    public void progressChanged(int newPercentage) {
        _progressBar.setValue(newPercentage);
    }

    public int getInterval() {
        return 1;
    }

    public void progressFinished() {
        _progressBar.setValue(100);
        getWizard().gotoNextPage();
    }

    public void progressEntry(String entry) {
        _progressEntry.setText(getText(INFLATING, entry));
    }

    public void progressStartScripts() {
        _progressEntry.setText(getText(GENERATING_START_SCRIPTS));
    }

    public void progressStandalone() {
        _progressEntry.setText(getText(PACKING_STANDALONE_JAR));
    }

    public void progressEnsurepip() { _progressEntry.setText(getText(C_ENSUREPIP)); }

}