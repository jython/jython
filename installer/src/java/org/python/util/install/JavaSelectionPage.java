package org.python.util.install;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.File;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

public class JavaSelectionPage extends AbstractWizardPage {

    private static final long serialVersionUID = 2871052924519223110L;

    private final static String _CURRENT_ACTION_COMMAND = "current";
    private final static String _OTHER_ACTION_COMMAND = "other";

    private JRadioButton _currentButton;
    private JRadioButton _otherButton;

    private JLabel _label;
    private JTextField _javaHome;
    private JButton _browse;

    public JavaSelectionPage() {
        super();
        initComponents();
    }

    private void initComponents() {
        // label for java home
        _label = new JLabel();

        // radio buttons
        RadioButtonListener radioButtonListener = new RadioButtonListener();
        _currentButton = new JRadioButton();
        _currentButton.setActionCommand(_CURRENT_ACTION_COMMAND);
        _currentButton.addActionListener(radioButtonListener);
        _otherButton = new JRadioButton();
        _otherButton.setActionCommand(_OTHER_ACTION_COMMAND);
        _otherButton.addActionListener(radioButtonListener);
        ButtonGroup radioButtonGroup = new ButtonGroup();
        radioButtonGroup.add(_currentButton);
        radioButtonGroup.add(_otherButton);
        JPanel radioPanel = new JPanel(new GridLayout(0, 1));
        radioPanel.add(_currentButton);
        radioPanel.add(_otherButton);

        // directory for java home
        _javaHome = new JTextField(40);
        _javaHome.addFocusListener(new JavaFocusListener());
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
        panel.add(radioPanel, gridBagConstraints);
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        panel.add(_javaHome, gridBagConstraints);
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        panel.add(_browse, gridBagConstraints);

        add(panel);
    }

    JTextField getJavaHome() {
        return _javaHome;
    }

    protected String getTitle() {
        return getText(TARGET_JAVA_HOME_PROPERTY);
    }

    protected String getDescription() {
        return getText(CHOOSE_JRE);
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
        return _currentButton;
    }

    protected void activate() {
        _label.setText(getText(SELECT_JAVA_HOME) + ": ");
        _currentButton.setText(getText(CURRENT));
        _otherButton.setText(getText(OTHER));
        _browse.setText(getText(BROWSE));
        setValues();
    }

    protected void passivate() {
    }

    protected void beforeValidate() {
    }

    private void setValues() {
        boolean current = true;
        JavaHomeHandler javaHomeHandler = FrameInstaller.getJavaHomeHandler();
        if (javaHomeHandler.isDeviation()) {
            current = false;
        }
        setCurrent(current);
    }

    private void setCurrent(boolean current) {
        if (current) {
            FrameInstaller.setJavaHomeHandler(new JavaHomeHandler());
            _currentButton.setSelected(true);
            _otherButton.setSelected(false);
            _javaHome.setEnabled(false);
            _browse.setEnabled(false);
        } else {
            _currentButton.setSelected(false);
            _otherButton.setSelected(true);
            _javaHome.setEnabled(true);
            _browse.setEnabled(true);
        }
        JavaHomeHandler javaHomeHandler = FrameInstaller.getJavaHomeHandler();
        if (javaHomeHandler.isValidHome()) {
            _javaHome.setText(javaHomeHandler.getHome().getAbsolutePath());
        } else {
            _javaHome.setText("");
        }
        _javaHome.setToolTipText(_javaHome.getText());
    }

    private final class BrowseButtonListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            JFileChooser fileChooser = new JFileChooser(new File(_javaHome.getText()));
            fileChooser.setDialogTitle(getText(SELECT_JAVA_HOME));
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
                FrameInstaller.setJavaHomeHandler(new JavaHomeHandler(fileChooser.getSelectedFile().getAbsolutePath()));
                setValues();
            }
        }
    }

    private final class RadioButtonListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            String actionCommand = e.getActionCommand();
            setCurrent(_CURRENT_ACTION_COMMAND.equals(actionCommand));
        }
    }

    private final class JavaFocusListener implements FocusListener {
        public void focusGained(FocusEvent e) {
        }

        public void focusLost(FocusEvent e) {
            String javaHome = _javaHome.getText();
            FrameInstaller.setJavaHomeHandler(new JavaHomeHandler(javaHome));
            _javaHome.setToolTipText(javaHome);
        }
    }

}