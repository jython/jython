package org.python.util.install;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.File;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class DirectorySelectionPage extends AbstractWizardPage {

    private static final long serialVersionUID = -3672273150338356549L;
    
    private JLabel _label;
    private JTextField _directory;
    private JButton _browse;
    private JarInfo _jarInfo;

    public DirectorySelectionPage(JarInfo jarInfo) {
        super();
        _jarInfo = jarInfo;
        initComponents();
    }

    private void initComponents() {
        // label
        _label = new JLabel();
        // directory
        _directory = new JTextField(40);
        _directory.addFocusListener(new DirectoryFocusListener());
        // browse button
        _browse = new JButton();
        _browse.addActionListener(new BrowseButtonListener());

        JPanel panel = new JPanel();
        GridBagLayout gridBagLayout = new GridBagLayout();
        panel.setLayout(gridBagLayout);
        GridBagConstraints gridBagConstraints = newGridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        panel.add(_label, gridBagConstraints);
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        panel.add(_directory, gridBagConstraints);
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        panel.add(_browse, gridBagConstraints);

        add(panel);
    }

    JTextField getDirectory() {
        return _directory;
    }

    protected String getTitle() {
        return getText(TARGET_DIRECTORY_PROPERTY);
    }

    protected String getDescription() {
        return getText(CHOOSE_LOCATION);
    }

    protected boolean isCancelVisible() {
        return true;
    }

    protected boolean isPreviousVisible() {
        return true;
    }

    protected boolean isNextVisible() {
        return true;
    }

    protected JComponent getFocusField() {
        return _directory;
    }

    protected void activate() {
        _label.setText(getText(SELECT_TARGET_DIRECTORY) + ": ");
        _browse.setText(getText(BROWSE));
        String directory = FrameInstaller.getTargetDirectory();
        if (directory == null || directory.length() <= 0) {
            File defaultDirectory = getDefaultDirectory();
            try {
                directory = defaultDirectory.getCanonicalPath();
            } catch (IOException e) {
                directory = "?";
            }
            FrameInstaller.setTargetDirectory(directory);
        }
        _directory.setText(FrameInstaller.getTargetDirectory());
        _directory.setToolTipText(_directory.getText());
    }

    protected void passivate() {
    }

    protected void beforeValidate() {
    }

    private File getDefaultDirectory() {
        String directory = "";
        File defaultDirectory = null;
        // 1st try (on windows): root
        if (Installation.isWindows()) {
            JavaHomeHandler handler = new JavaHomeHandler();
            if (handler.isValidHome()) {
                directory = handler.getHome().getAbsolutePath();
                if (directory.length() > 2) {
                    directory = directory.substring(0, 2);
                }
            } else {
                directory = "C:";
            }
            defaultDirectory = makeJythonSubDirectory(directory);
        }
        // 2st try: user.home
        if (defaultDirectory == null) {
            directory = System.getProperty("user.home", "");
            if (directory.length() > 0) {
                defaultDirectory = makeJythonSubDirectory(directory);
            }
        }
        // 3rd try: user.dir
        if (defaultDirectory == null) {
            directory = System.getProperty("user.dir", "");
            if (directory.length() > 0) {
                defaultDirectory = makeJythonSubDirectory(directory);
            }
        }
        // 4th try: current directory
        if (defaultDirectory == null) {
            defaultDirectory = makeJythonSubDirectory(new File(new File("dummy").getAbsolutePath()).getParent());
        }
        return defaultDirectory;
    }

    private File makeJythonSubDirectory(String directory) {
        File defaultDirectory = null;
        File parentDirectory = new File(directory);
        if (parentDirectory.exists() && parentDirectory.isDirectory()) {
            String jythonSubDirectoryName = "jython" + (_jarInfo.getVersion()).replaceAll("\\+", "");
            defaultDirectory = new File(parentDirectory, jythonSubDirectoryName);
        }
        return defaultDirectory;
    }

    private class BrowseButtonListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            String directoryName = _directory.getText();
            File directory = new File(directoryName);
            if (directory.exists()) {
                if (!directory.isDirectory()) {
                    // switch to parent directory if user typed the name of a file
                    directory = directory.getParentFile();
                }
            }
            JFileChooser fileChooser = new JFileChooser(directory);
            fileChooser.setDialogTitle(getText(SELECT_TARGET_DIRECTORY));
            // the filter is at the moment only used for the title of the dialog:
            fileChooser.setFileFilter(new DirectoryFilter());
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (fileChooser.isAcceptAllFileFilterUsed()) {
                if (Installation.isMacintosh() && Installation.isJDK141()) {
                    // work around ArrayIndexOutOfBoundsExceptio on Mac OS X, java version 1.4.1
                } else {
                    fileChooser.setAcceptAllFileFilterUsed(false);
                }
            }
            int returnValue = fileChooser.showDialog(_browse, getText(SELECT));
            if (returnValue == JFileChooser.APPROVE_OPTION) {
                _directory.setText(fileChooser.getSelectedFile().getAbsolutePath());
                _directory.setToolTipText(_directory.getText());
                FrameInstaller.setTargetDirectory(_directory.getText());
            }
        }
    }

    private class DirectoryFocusListener implements FocusListener {
        public void focusGained(FocusEvent e) {
        }

        public void focusLost(FocusEvent e) {
            FrameInstaller.setTargetDirectory(_directory.getText());
            _directory.setToolTipText(_directory.getText());
        }
    }

}
