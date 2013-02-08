package org.python.util.install;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JSeparator;

public class WizardHeader extends AbstractWizardHeader {
    private static final Dimension _iconSize = new Dimension(100, 60);

    private JLabel _descriptionLabel;
    private JSeparator _headerSeparator;
    private JLabel _iconLabel;
    private JLabel _titleLabel;

    WizardHeader() {
        super();
        initComponents();
    }

    private void initComponents() {
        GridBagConstraints gridBagConstraints;

        _titleLabel = new JLabel();
        _descriptionLabel = new JLabel();
        _iconLabel = new JLabel();
        _headerSeparator = new JSeparator();

        setLayout(new GridBagLayout());

        setBackground(new Color(255, 255, 255));
        _titleLabel.setFont(_titleLabel.getFont().deriveFont(Font.BOLD, 14f));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new Insets(2, 2, 2, 2);
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        add(_titleLabel, gridBagConstraints);

        _descriptionLabel.setFont(_descriptionLabel.getFont().deriveFont(Font.PLAIN));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new Insets(2, 7, 2, 2);
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        add(_descriptionLabel, gridBagConstraints);

        _iconLabel.setMinimumSize(_iconSize);
        _iconLabel.setMaximumSize(_iconSize);
        _iconLabel.setPreferredSize(_iconSize);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.insets = new Insets(2, 2, 2, 2);
        gridBagConstraints.anchor = GridBagConstraints.NORTHEAST;
        add(_iconLabel, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        add(_headerSeparator, gridBagConstraints);
    }

    protected void setDescription(String description) {
        _descriptionLabel.setText(description);
    }

    protected void setIcon(ImageIcon icon) {
        _iconLabel.setIcon(icon);
    }

    protected void setTitle(String title) {
        _titleLabel.setText(title);
    }
}