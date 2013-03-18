package org.python.util.install;

import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.net.URL;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JPanel;

public abstract class AbstractWizardPage extends JPanel implements TextKeys {
    private static final long serialVersionUID = -5233805023557214279L;

    private static final String _ICON_FILE_NAME = "jython_small_c.png";

    private static ImageIcon _imageIcon = null;
    private AbstractWizardValidator _validator = null;
    private AbstractWizard _wizard;

    public AbstractWizardPage() {
        super();
        setValidator(null);
    }

    /**
     * This method is called when the wizard page is activated, after Wizard.next();
     */
    protected abstract void activate();

    /**
     * This method is called right before validation of the page
     */
    protected abstract void beforeValidate();

    /**
     * called from Wizard, right after this page is set visible
     */
    final void doActivate() {
        if (getFocusField() != null)
            this.getFocusField().grabFocus();
        activate();
    }

    /**
     * @return the description of this page, which will be displayed in the wizard header
     */
    protected abstract String getDescription();

    /**
     * @return the input field on the page that should grab the focus when the page is activated
     */
    protected abstract JComponent getFocusField();

    /**
     * @return the icon that should be displayed in the header in all steps
     */
    protected ImageIcon getIcon() {
        if (_imageIcon == null) {
            URL iconURL = FileHelper.getRelativeURL(getClass(), _ICON_FILE_NAME);
            if (iconURL != null) {
                _imageIcon = new ImageIcon(iconURL);
            }
        }
        return _imageIcon;
    }

    /**
     * @return the title of this page, which will be displayed in the wizard header
     */
    protected abstract String getTitle();

    /**
     * @return the wizard this page belongs to
     */
    public final AbstractWizard getWizard() {
        return _wizard;
    }

    /**
     * @return whether the <i>cancel </i> button is visible
     */
    protected abstract boolean isCancelVisible();

    /**
     * @return whether the <i>next </i> button is visible
     */
    protected abstract boolean isNextVisible();

    /**
     * @return whether the <i>previous </i> button is visible
     */
    protected abstract boolean isPreviousVisible();

    /**
     * this method is called right before the page is hidden, but after the validation
     */
    protected abstract void passivate();

    /**
     * Set the validator for this page. The validator is called when the <i>next </i> button is clicked.
     * 
     * @param validator the validator for this page. If this is null, a EmptyValidator is assigned
     */
    public final void setValidator(AbstractWizardValidator validator) {
        if (validator == null)
            this._validator = new EmptyValidator();
        else
            this._validator = validator;
        this._validator.setWizardPage(this);
        this._validator.addValidationListener(_wizard);
    }

    /**
     * @param wizard the wizard this page belongs to
     */
    final void setWizard(AbstractWizard wizard) {
        this._wizard = wizard;
        _validator.addValidationListener(wizard);
    }

    /**
     * perform the validation of this page, using the assigned WizardValidator
     */
    final void validateInput() {
        beforeValidate();
        if (_validator == null)
            return;
        _validator.start();
    }

    /**
     * @return default grid bag constraints
     */
    protected GridBagConstraints newGridBagConstraints() {
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        return gridBagConstraints;
    }

    final String getText(String textKey) {
        return Installation.getText(textKey);
    }

    final String getText(String textKey, String parameter0) {
        return Installation.getText(textKey, parameter0);
    }

    final String getText(String textKey, String parameter0, String parameter1) {
        return Installation.getText(textKey, parameter0, parameter1);
    }

}