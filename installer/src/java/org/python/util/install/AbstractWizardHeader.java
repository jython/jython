package org.python.util.install;

import javax.swing.ImageIcon;
import javax.swing.JPanel;

abstract class AbstractWizardHeader extends JPanel {
    protected abstract void setTitle(String title);

    protected abstract void setDescription(String description);

    protected abstract void setIcon(ImageIcon icon);
}