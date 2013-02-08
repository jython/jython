package org.python.util.install;

import java.io.File;

import javax.swing.filechooser.FileFilter;

/**
 * Filters all directories, and sets the description in the file chooser
 */
public class DirectoryFilter extends FileFilter {

    public boolean accept(File f) {
        if (f.isDirectory()) {
            return true;
        } else {
            return false;
        }
    }

    public String getDescription() {
        return Installation.getText(TextKeys.DIRECTORIES_ONLY);
    }
}