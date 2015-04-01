package org.python.util.install;

import java.util.ListResourceBundle;

public class TextConstants extends ListResourceBundle implements TextKeys {

    static final Object[][] contents = {
        // LOCALIZE THIS

        { ACCEPT, "I accept" }, // license
        { ALL, "All (everything, including sources)" }, // installation type
        { BROWSE, "Browse..." }, // button (open the JFileChooser)
        { CANCEL, "Cancel" }, // button
        { CHOOSE_LOCATION, "Choose the location where you want Jython to be installed to" }, // selection
        { CHOOSE_JRE, "Choose the java version (JRE/JDK) to run Jython with" }, // selection
        { CONFIRM_START, "Please press {0} to start the installation" }, // overview
        { CONGRATULATIONS, "Congratulations!" }, // congratulations
        { CORE, "Core" }, // installation type
        { CREATED_DIRECTORY, "Created directory {0}" }, // directory
        { CURRENT, "Current" }, // directory
        { CUSTOM, "Custom" }, // installation type
        { DEMOS_EXAMPLES, "Demos and examples" }, // installation type
        { DO_NOT_ACCEPT, "I do not accept" }, // license
        { DIRECTORIES_ONLY, "Directories only" }, // file chooser
        { DOCUMENTATION, "Documentation" }, // installation type
        { EMPTY_TARGET_DIRECTORY, "Target directory must not be empty" }, // error
        { ENGLISH, "English" }, // language
        { ENSUREPIP, "Install pip and setuptools"},
        { ERROR, "Error" }, // error
        { ERROR_ACCESS_JARFILE, "Error accessing jar file" }, // error
        { FINISH, "Finish" }, // button
        { GENERATING_START_SCRIPTS, "Generating start scripts ..." }, // progress
        { GERMAN, "German" }, // language
        { INFLATING, "Inflating {0}" }, // progress
        { INFORMATION, "Information" }, // information
        { INSTALLATION_CANCELLED, "Installation cancelled." }, // final
        { INSTALLATION_IN_PROGRESS, "The installation is now in progress" }, // progress
        { INSTALLATION_TYPE_DESCRIPTION, "The following installation types are available" }, // installation type
        { INSTALLATION_TYPE, "Installation type" }, // installation type
        { JAR_NOT_FOUND, "Unable to find jar file {0}." }, // error
        { JAVA_INFO, "Java vendor / version" }, // version
        { JYTHON_INSTALL, "Jython Installation" }, // title
        { LANGUAGE_PROPERTY, "Language" }, // language
        { LIBRARY_MODULES, "Library modules" }, // installation type
        { LICENSE, "License agreement" }, // license
        { MAYBE_NOT_SUPPORTED, "Maybe not supported" }, // version
        { MINIMUM, "Minimum (core)" }, // installation type
        { NEXT, "Next" }, // button
        { NON_EMPTY_TARGET_DIRECTORY, "Target directory is not empty" }, // error
        { NO_MANIFEST, "No manifest found in jar file {0}." }, // error
        { NOT_OK, "Not ok !" }, // version
        { OK, "Ok" }, // version
        { OS_INFO, "OS name / version" }, // version
        { OTHER, "Other" }, // directory
        { OVERVIEW_DESCRIPTION, "The installation will be done using the following options" }, // overview
        { OVERVIEW_TITLE, "Overview (summary of options)" }, // overview
        { PACKING_STANDALONE_JAR, "Packing standalone " + JarInstaller.JYTHON_JAR + " ..." }, // progress
        { PLEASE_ACCEPT_LICENSE, "Please read and accept the license agreement" }, // license
        { PLEASE_README, "Please read the following information" }, // readme
        { PLEASE_READ_LICENSE, "Please read the license agreement carefully" }, // license
        { PLEASE_WAIT, "Please stand by, this may take a few seconds ..." }, // progress
        { PRESS_FINISH, "Please press {0} to exit the installation." }, // finish
        { PREVIOUS, "Previous" }, // button
        { PROGRESS, "Progress" }, // progress
        { README, "README" }, // readme
        { SELECT, "Select" }, // button (approval in JFileChooser)
        { SELECT_INSTALLATION_TYPE, "Please select the installation type" }, // installation type
        { SELECT_JAVA_HOME, "Please select the java home directory" }, // directory
        { SELECT_LANGUAGE, "Please select your language" }, // language
        { SELECT_TARGET_DIRECTORY, "Please select the target directory" }, // directory
        { SOURCES, "Sources" }, // installation type
        { STANDARD, "Standard (core, library modules, demos, examples, documentation)" }, // installation type
        { STANDALONE, "Standalone (a callable .jar file)" }, // installation type
        { SUCCESS, "You successfully installed Jython {0} to directory {1}." }, // success
        { TARGET_DIRECTORY_PROPERTY, "Target directory" }, // property as title
        { TARGET_JAVA_HOME_PROPERTY, "Target java home" }, // property as title
        { UNABLE_CREATE_DIRECTORY, "Unable to create directory {0}." }, // error
        { UNABLE_CREATE_FILE, "Unable to create file {0}." }, // error
        { UNABLE_TO_DELETE, "Unable to delete {0}" }, // error
        { UNEXPECTED_URL, "Unexpected URL {0} found for installation jar file." }, // error
        { VERSION_INFO, "You are about to install Jython version {0}" }, // version
        { WELCOME_TO_JYTHON, "Welcome to Jython !" }, // welcome
        { ZIP_ENTRY_SIZE, "Size of zip entry {0} unknown." }, // error
        { ZIP_ENTRY_TOO_BIG, "Zip entry {0} too big." }, // error

        // console texts (C_*) should not contain special characters (like e.g. &uuml;)
        { C_ACCEPT, "Do you accept the license agreement ?" }, // license
        { C_ALL, "All (everything, including sources)" }, // installation type
        { C_AT_ANY_TIME_CANCEL, "(at any time, answer {0} to cancel the installation)" }, // console
        { C_AVAILABLE_LANGUAGES, "For the installation process, the following languages are available: {0}" }, // console
        { C_CHECK_JAVA_VERSION, "Checking java version ..." }, // progress
        { C_CLEAR_DIRECTORY, "Contents of directory {0} will be deleted now! Are you sure to proceed ?" }, //console
        { C_CONFIRM_TARGET, "Please confirm copying of files to directory {0}" }, // console
        { C_CONGRATULATIONS, "Congratulations!" }, // congratulations
        { C_CREATE_DIRECTORY, "Unable to find directory {0}, create it ?" }, // console
        { C_ENTER_TARGET_DIRECTORY, "Please enter the target directory" }, // console
        { C_ENTER_JAVA_HOME, "Please enter the java home directory (empty for using the current java runtime)" }, // console
        { C_ENGLISH, "English" }, // language
        { C_ENSUREPIP, "Installing pip and setuptools"},
        { C_EXCLUDE, "Do you want to exclude parts from the installation ?" }, // installation type
        { C_GENERATING_START_SCRIPTS, "Generating start scripts ..." }, // progress
        { C_GERMAN, "German" }, // language
        { C_INCLUDE, "Do you want to install additional parts ?" }, // installation type
        { C_INEXCLUDE_PARTS, "The following parts are selectable ({0} = no more)" }, // installation type
        { C_INSTALL_TYPES, "The following installation types are available:" }, // installation type
        { C_INVALID_ANSWER, "Answer {0} is not valid here" }, // error
        { C_JAVA_VERSION, "Your java version to start Jython is: {0} / {1}" }, // version
        { C_MINIMUM, "Minimum (core)" }, // installation type
        { C_NO, "n" }, // answer
        { C_NO_BIN_DIRECTORY, "There is no /bin directory below {0}." }, // error
        { C_NO_JAVA_EXECUTABLE, "No java executable found in {0}." }, // error
        { C_NO_VALID_JAVA, "No valid java found in {0}." }, // error
        { C_NON_EMPTY_TARGET_DIRECTORY, "Target directory {0} is not empty" }, // error
        { C_NOT_A_DIRECTORY, "{0} is not a directory. " }, // error
        { C_NOT_FOUND, "{0} not found. " }, // error
        { C_OS_VERSION, "Your operating system version is: {0} / {1}" }, // version
        { C_OVERWRITE_DIRECTORY, "Directory {0} is not empty - ok to overwrite contents ?" }, // console
        { C_PACKING_STANDALONE_JAR, "Packing standalone " + JarInstaller.JYTHON_JAR + " ..." }, // progress
        { C_PROCEED, "Please press Enter to proceed" }, // console
        { C_PROCEED_ANYWAY, "Please press Enter to proceed anyway" }, // console
        { C_READ_LICENSE, "Do you want to read the license agreement now ?" }, // license
        { C_READ_README, "Do you want to show the contents of README ?" }, // readme
        { C_SCHEDULED, "{0} scheduled for installation" }, // installation type
        { C_SELECT_INSTALL_TYPE, "Please select the installation type" }, // installation type
        { C_SELECT_LANGUAGE, "Please select your language" }, // language
        { C_SILENT_INSTALLATION, "Performing silent installation" }, // installation mode
        { C_STANDALONE, "Standalone (a single, executable .jar)" }, //installation mode
        { C_STANDARD, "Standard (core, library modules, demos and examples, documentation)" }, // installation type
        { C_SUCCESS, "You successfully installed Jython {0} to directory {1}." }, // success
        { C_SUMMARY, "Summary:" }, // summary
        { C_TO_CURRENT_JAVA, "Warning: switching back to current JDK due to error: {0}." }, // warning
        { C_UNABLE_CREATE_DIRECTORY, "Unable to create directory {0}." }, // error
        { C_UNABLE_CREATE_TMPFILE, "Unable to create temp file {0}." }, // error
        { C_UNSCHEDULED, "{0} excluded from installation" }, // installation type
        { C_UNABLE_TO_DELETE, "Unable to delete {0}" }, // error
        { C_UNSUPPORTED_JAVA, "This java version is not supported." }, // version
        { C_UNSUPPORTED_OS, "This operating system might not be fully supported." }, // version
        { C_USING_TYPE, "Using installation type {0}" }, // installation type
        { C_VERSION_INFO, "You are about to install Jython version {0}" }, // version
        { C_WELCOME_TO_JYTHON, "Welcome to Jython !" }, // welcome
        { C_YES, "y" }, // answer
        
        // END OF MATERIAL TO LOCALIZE
    };

    public Object[][] getContents() {
        return contents;
    }

}