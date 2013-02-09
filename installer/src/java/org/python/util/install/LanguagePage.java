package org.python.util.install;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;

public class LanguagePage extends AbstractWizardPage {

    private static Map _languageIndexes = new HashMap(2);
    static {
        _languageIndexes.put(Locale.ENGLISH, new Integer(0));
        _languageIndexes.put(Locale.GERMAN, new Integer(1));
    }

    private JLabel _label;
    private JComboBox _languageBox;
    private JarInfo _jarInfo;
    private boolean _activated;
    private boolean _stopListening;

    public LanguagePage(JarInfo jarInfo) {
        super();
        _jarInfo = jarInfo;
        _activated = false;
        _stopListening = false;
        initComponents();
    }

    private void initComponents() {
        _label = new JLabel();
        add(_label);
        _languageBox = new JComboBox();
        _languageBox.addActionListener(new LanguageBoxListener());
        add(_languageBox);
    }

    protected String getTitle() {
        return getText(WELCOME_TO_JYTHON);
    }

    protected String getDescription() {
        return getText(VERSION_INFO, _jarInfo.getVersion());
    }

    protected boolean isCancelVisible() {
        return true;
    }

    protected boolean isPreviousVisible() {
        return false;
    }

    protected boolean isNextVisible() {
        return true;
    }

    protected JComponent getFocusField() {
        return _languageBox;
    }

    protected void activate() {
        _label.setText(getText(SELECT_LANGUAGE) + ": ");
        // replace combo box items (localization)
        int itemCount = _languageBox.getItemCount();
        _stopListening = true; // adding and removing fires an action event
        for (int i = 0; i < itemCount; i++) {
            _languageBox.removeItemAt(0);
        }
        _languageBox.addItem(getText(ENGLISH)); // keep indexes here
        _languageBox.addItem(getText(GERMAN));
        _stopListening = false;
        if (!_activated) {
            // preselect German if default looks like German
            if (Locale.getDefault().toString().startsWith(Locale.GERMAN.toString())) {
                _languageBox.setSelectedIndex(getLanguageIndex(Locale.GERMAN));
                FrameInstaller.setLanguage(Locale.GERMAN);
            }
        } else {
            _languageBox.setSelectedIndex(getLanguageIndex(FrameInstaller.getLanguage()));
        }
        _activated = true;
    }

    protected void passivate() {
    }

    protected void beforeValidate() {
    }

    private int getLanguageIndex(Locale locale) {
        return ((Integer) _languageIndexes.get(locale)).intValue();
    }

    private Locale getLanguageFromIndex(int index) {
        Integer indexInteger = new Integer(index);
        Iterator languages = _languageIndexes.entrySet().iterator();
        while (languages.hasNext()) {
            Map.Entry entry = (Map.Entry) languages.next();
            if (entry.getValue().equals(indexInteger)) {
                return (Locale) entry.getKey();
            }
        }
        return null;
    }

    private class LanguageBoxListener implements ActionListener {
        public void actionPerformed(ActionEvent ae) {
            if (!_stopListening) {
                FrameInstaller.setLanguage(getLanguageFromIndex(_languageBox.getSelectedIndex()));
            }
        }
    }

}