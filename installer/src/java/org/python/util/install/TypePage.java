package org.python.util.install;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

public class TypePage extends AbstractWizardPage {

    private static final String _CUSTOM_ACTION_COMMAND = "custom";

    private JLabel _label;
    private JRadioButton _allButton;
    private JRadioButton _standardButton;
    private JRadioButton _minimumButton;
    private JRadioButton _standaloneButton;
    private JRadioButton _customButton;

    private JCheckBox _core;
    private JCheckBox _mod;
    private JCheckBox _demo;
    private JCheckBox _doc;
    private JCheckBox _src;
    private JCheckBox _ensurepip;

    private boolean _firstTime = true;

    public TypePage() {
        super();
        initComponents();
    }

    private void initComponents() {
        TypeChangeListener typeChangeListener = new TypeChangeListener();

        _label = new JLabel();

        // radio buttons
        _allButton = new JRadioButton();
        _allButton.setActionCommand(Installation.ALL);
        _allButton.addActionListener(typeChangeListener);
        _standardButton = new JRadioButton();
        _standardButton.setActionCommand(Installation.STANDARD);
        _standardButton.addActionListener(typeChangeListener);
        _minimumButton = new JRadioButton();
        _minimumButton.setActionCommand(Installation.MINIMUM);
        _minimumButton.addActionListener(typeChangeListener);
        _standaloneButton = new JRadioButton();
        _standaloneButton.setActionCommand(Installation.STANDALONE);
        _standaloneButton.addActionListener(typeChangeListener);
        _customButton = new JRadioButton();
        _customButton.setActionCommand(_CUSTOM_ACTION_COMMAND);
        _customButton.addActionListener(typeChangeListener);
        ButtonGroup radioButtonGroup = new ButtonGroup();
        radioButtonGroup.add(_allButton);
        radioButtonGroup.add(_standardButton);
        radioButtonGroup.add(_minimumButton);
        radioButtonGroup.add(_standaloneButton);
        radioButtonGroup.add(_customButton);
        JPanel radioPanel = new JPanel(new GridLayout(0, 1));
        radioPanel.add(_allButton);
        radioPanel.add(_standardButton);
        radioPanel.add(_minimumButton);
        radioPanel.add(_standaloneButton);
        radioPanel.add(_customButton);

        // check boxes
        _core = new JCheckBox();
        _core.setEnabled(false);
        _mod = new JCheckBox();
        _mod.setEnabled(true);
        _mod.setActionCommand(InstallerCommandLine.INEXCLUDE_LIBRARY_MODULES);
        _mod.addActionListener(typeChangeListener);
        _demo = new JCheckBox();
        _demo.setEnabled(true);
        _demo.setActionCommand(InstallerCommandLine.INEXCLUDE_DEMOS_AND_EXAMPLES);
        _demo.addActionListener(typeChangeListener);
        _doc = new JCheckBox();
        _doc.setEnabled(true);
        _doc.setActionCommand(InstallerCommandLine.INEXCLUDE_DOCUMENTATION);
        _doc.addActionListener(typeChangeListener);
        _src = new JCheckBox();
        _src.setEnabled(true);
        _src.setActionCommand(InstallerCommandLine.INEXCLUDE_SOURCES);
        _src.addActionListener(typeChangeListener);
        _ensurepip = new JCheckBox();
        _ensurepip.setEnabled(true);
        _ensurepip.setActionCommand(InstallerCommandLine.INEXCLUDE_ENSUREPIP);
        _ensurepip.addActionListener(typeChangeListener);

        JPanel checkboxPanel = new JPanel();
        GridLayout gridLayout = new GridLayout(6, 1);
        checkboxPanel.setLayout(gridLayout);
        checkboxPanel.add(_core);
        checkboxPanel.add(_mod);
        checkboxPanel.add(_demo);
        checkboxPanel.add(_doc);
        checkboxPanel.add(_src);
        checkboxPanel.add(_ensurepip);

        JPanel panel = new JPanel();
        GridBagLayout gridBagLayout = new GridBagLayout();
        panel.setLayout(gridBagLayout);
        GridBagConstraints gridBagConstraints = newGridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        panel.add(_label, gridBagConstraints);
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        panel.add(radioPanel, gridBagConstraints);
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        panel.add(checkboxPanel, gridBagConstraints);

        add(panel);
    }

    protected String getTitle() {
        return getText(INSTALLATION_TYPE);
    }

    protected String getDescription() {
        return getText(INSTALLATION_TYPE_DESCRIPTION);
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
        InstallationType installationType = getInstallationType();
        if (installationType.isAll()) {
            return _allButton;
        } else if (installationType.isMinimum()) {
            return _minimumButton;
        } else if (installationType.isStandalone()) {
            return _standaloneButton;
        } else if (installationType.isStandard()) {
            return _standardButton;
        } else {
            return _customButton;
        }
    }

    protected void activate() {
        _label.setText(getText(SELECT_INSTALLATION_TYPE) + ": ");
        _allButton.setText(getText(ALL));
        _standardButton.setText(getText(STANDARD));
        _minimumButton.setText(getText(MINIMUM));
        _standaloneButton.setText(getText(STANDALONE));
        _customButton.setText(getText(CUSTOM));
        InstallationType installationType = getInstallationType();
        if (installationType.isAll()) {
            _allButton.setSelected(true);
        } else if (installationType.isMinimum()) {
            _minimumButton.setSelected(true);
        } else if (installationType.isStandalone()) {
            _standaloneButton.setSelected(true);
        } else if (installationType.isStandard()) {
            _standardButton.setSelected(true);
        } else {
            _customButton.setSelected(true);
        }
        _core.setText(getText(CORE));
        _mod.setText(getText(LIBRARY_MODULES));
        _demo.setText(getText(DEMOS_EXAMPLES));
        _doc.setText(getText(DOCUMENTATION));
        _src.setText(getText(SOURCES));
        _ensurepip.setText(getText(ENSUREPIP));
        setCheckboxes(installationType);
    }

    protected void passivate() {
    }

    protected void beforeValidate() {
    }

    private InstallationType getInstallationType() {
        InstallationType installationType;
        if (_firstTime) {
            _firstTime = false;
            installationType = new InstallationType();
            installationType.setStandard();
            FrameInstaller.setInstallationType(installationType);
        }
        installationType = FrameInstaller.getInstallationType();
        return installationType;
    }

    private final class TypeChangeListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            InstallationType installationType = FrameInstaller.getInstallationType();
            String actionCommand = e.getActionCommand();
            if (Installation.ALL.equals(actionCommand)) {
                installationType.setAll();
                setCheckboxes(installationType);
            } else if (Installation.STANDARD.equals(actionCommand)) {
                installationType.setStandard();
                setCheckboxes(installationType);
            } else if (Installation.MINIMUM.equals(actionCommand)) {
                installationType.setMinimum();
                setCheckboxes(installationType);
            } else if (Installation.STANDALONE.equals(actionCommand)) {
                installationType.setStandalone();
                setCheckboxes(installationType);
            } else if (_CUSTOM_ACTION_COMMAND.equals(actionCommand)) {
                _mod.setEnabled(true);
                _demo.setEnabled(true);
                _doc.setEnabled(true);
                _src.setEnabled(true);
                _ensurepip.setEnabled(true);
            } else {
                boolean selected = ((JCheckBox) e.getSource()).isSelected();
                if (InstallerCommandLine.INEXCLUDE_LIBRARY_MODULES.equals(actionCommand)) {
                    if (selected) {
                        installationType.addLibraryModules();
                    } else {
                        installationType.removeLibraryModules();
                    }
                } else if (InstallerCommandLine.INEXCLUDE_DEMOS_AND_EXAMPLES.equals(actionCommand)) {
                    if (selected) {
                        installationType.addDemosAndExamples();
                    } else {
                        installationType.removeDemosAndExamples();
                    }
                } else if (InstallerCommandLine.INEXCLUDE_DOCUMENTATION.equals(actionCommand)) {
                    if (selected) {
                        installationType.addDocumentation();
                    } else {
                        installationType.removeDocumentation();
                    }
                } else if (InstallerCommandLine.INEXCLUDE_SOURCES.equals(actionCommand)) {
                    if (selected) {
                        installationType.addSources();
                    } else {
                        installationType.removeSources();
                    }
                } else if (InstallerCommandLine.INEXCLUDE_ENSUREPIP.equals(actionCommand)) {
                    if (selected) {
                        installationType.addEnsurepip();
                    } else {
                        installationType.removeEnsurepip();
                    }
                }
            }
            FrameInstaller.setInstallationType(installationType);
        }
    }

    void setCheckboxes(InstallationType installationType) {
        _core.setSelected(true);
        _mod.setSelected(installationType.installLibraryModules());
        _demo.setSelected(installationType.installDemosAndExamples());
        _doc.setSelected(installationType.installDocumentation());
        _src.setSelected(installationType.installSources());
        _ensurepip.setSelected(installationType.ensurepip());
        _standaloneButton.setSelected(installationType.isStandalone());
        _mod.setEnabled(!installationType.isPredefined());
        _demo.setEnabled(!installationType.isPredefined());
        _doc.setEnabled(!installationType.isPredefined());
        _src.setEnabled(!installationType.isPredefined());
        _ensurepip.setEnabled(!installationType.isPredefined());
    }

}