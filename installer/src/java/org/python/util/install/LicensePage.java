package org.python.util.install;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

public class LicensePage extends AbstractWizardPage {

    private static final String _ACCEPT_ACTION_COMMAND = "1";
    private static final String _DO_NOT_ACCEPT_ACTION_COMMAND = "0";

    private JRadioButton _acceptButton;
    private JRadioButton _doNotAcceptButton;

    public LicensePage(JarInfo jarInfo) {
        super();
        initComponents(jarInfo);
    }

    private void initComponents(JarInfo jarInfo) {
        String licenseText = "n/a";
        try {
            licenseText = jarInfo.getLicenseText();
        } catch (IOException e) {
            e.printStackTrace();
        }
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new GridLayout(1, 1, 10, 10));
        JTextArea textArea = new JTextArea(13, 80);
        JScrollPane scrollPane = new JScrollPane(textArea, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        textArea.setEditable(false);
        textArea.setText(licenseText);
        centerPanel.add(scrollPane);

        // radio buttons
        JPanel southPanel = new JPanel();
        RadioButtonListener radioButtonListener = new RadioButtonListener();
        _acceptButton = new JRadioButton();
        _acceptButton.setActionCommand(_ACCEPT_ACTION_COMMAND);
        _acceptButton.addActionListener(radioButtonListener);
        _doNotAcceptButton = new JRadioButton();
        _doNotAcceptButton.setActionCommand(_DO_NOT_ACCEPT_ACTION_COMMAND);
        _doNotAcceptButton.addActionListener(radioButtonListener);
        ButtonGroup radioButtonGroup = new ButtonGroup();
        radioButtonGroup.add(_acceptButton);
        radioButtonGroup.add(_doNotAcceptButton);
        JPanel radioPanel = new JPanel(new GridLayout(1, 0));
        radioPanel.add(_acceptButton);
        radioPanel.add(_doNotAcceptButton);
        southPanel.add(radioPanel);

        setLayout(new BorderLayout(0, 5));
        add(centerPanel, BorderLayout.CENTER);
        add(southPanel, BorderLayout.SOUTH);
    }

    protected String getTitle() {
        return getText(LICENSE);
    }

    protected String getDescription() {
        return getText(PLEASE_READ_LICENSE);
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
        return _doNotAcceptButton;
    }

    protected void activate() {
        _acceptButton.setText(getText(ACCEPT));
        _doNotAcceptButton.setText(getText(DO_NOT_ACCEPT));
        boolean accept = FrameInstaller.isAccept();
        _acceptButton.setSelected(accept);
        _doNotAcceptButton.setSelected(!accept);
    }

    protected void passivate() {
    }

    protected void beforeValidate() {
    }

    protected boolean isAccept() {
        return _acceptButton.isSelected() && !_doNotAcceptButton.isSelected();
    }

    private final static class RadioButtonListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            String actionCommand = e.getActionCommand();
            if (actionCommand.equals(_ACCEPT_ACTION_COMMAND)) {
                FrameInstaller.setAccept(true);
            } else {
                FrameInstaller.setAccept(false);
            }
        }
    }

}