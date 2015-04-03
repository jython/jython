package org.python.util.install;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.python.util.install.Installation.JavaVersionInfo;
import org.python.util.install.driver.Tunnel;

public class ConsoleInstaller implements ProgressListener, TextKeys {

    public static final String CURRENT_JRE = "=";

    private static final String _CANCEL = "c";

    private static final String _PROMPT = ">>>";

    private static final String _BEGIN_ANSWERS = "[";

    private static final String _END_ANSWERS = "]";


    private InstallerCommandLine _commandLine;

    private JarInstaller _jarInstaller;

    private JarInfo _jarInfo;

    private Tunnel _tunnel;

    public ConsoleInstaller(InstallerCommandLine commandLine, JarInfo jarInfo) {
        _commandLine = commandLine;
        _jarInfo = jarInfo;
        _jarInstaller = new JarInstaller(this, jarInfo);
    }

    public void setTunnel(Tunnel tunnel) {
        _tunnel = tunnel;
    }

    public void install() {
        File targetDirectory = null;
        JavaHomeHandler javaHomeHandler = null;
        if (_commandLine.hasConsoleOption()) {
            welcome();
            selectLanguage();
            acceptLicense();
            InstallationType installationType = selectInstallationType();
            targetDirectory = determineTargetDirectory();
            javaHomeHandler = checkVersion(determineJavaHome());
            promptForCopying(targetDirectory, installationType, javaHomeHandler);
            _jarInstaller.inflate(targetDirectory, installationType, javaHomeHandler);
            showReadme(targetDirectory);
            success(targetDirectory);
        } else if (_commandLine.hasSilentOption()) {
            message(getText(C_SILENT_INSTALLATION));
            targetDirectory = _commandLine.getTargetDirectory();
            checkTargetDirectorySilent(targetDirectory);
            javaHomeHandler = checkVersionSilent(_commandLine.getJavaHomeHandler());
            _jarInstaller.inflate(targetDirectory,
                                  _commandLine.getInstallationType(),
                                  javaHomeHandler);
            success(targetDirectory);
        }
    }

    protected final static void message(String message) {
        System.out.println(message); // this System.out.println is intended
    }

    private void welcome() {
        message(getText(C_WELCOME_TO_JYTHON));
        message(getText(C_VERSION_INFO, _jarInfo.getVersion()));
        message(getText(C_AT_ANY_TIME_CANCEL, _CANCEL));
    }

    private String question(String question) {
        return question(question, null, false, null);
    }

    private String question(String question, boolean answerRequired) {
        return question(question, null, answerRequired, null);
    }

    private String question(String question, List<String> answers, String defaultAnswer) {
        return question(question, answers, true, defaultAnswer);
    }

    /**
     * question and answer
     * 
     * @param question
     * @param answers
     *            Possible answers (may be null)
     * @param answerRequired
     * @param defaultAnswer
     *            (may be null)
     * 
     * @return (chosen) answer
     */
    private String question(String question,
                            List<String> answers,
                            boolean answerRequired,
                            String defaultAnswer) {
        try {
            if (answers != null && answers.size() > 0) {
                question = question + " " + _BEGIN_ANSWERS;
                Iterator<String> answersAsIterator = answers.iterator();
                while (answersAsIterator.hasNext()) {
                    if (!question.endsWith(_BEGIN_ANSWERS))
                        question = question + "/";
                    String possibleAnswer = answersAsIterator.next();
                    if (possibleAnswer.equalsIgnoreCase(defaultAnswer)) {
                        if (Character.isDigit(possibleAnswer.charAt(0))) {
                            question = question.concat(" ").concat(possibleAnswer).concat(" ");
                        } else {
                            question = question + possibleAnswer.toUpperCase();
                        }
                    } else {
                        question = question + possibleAnswer;
                    }
                }
                question = question + _END_ANSWERS;
            }
            question = question + " " + _PROMPT + " ";
            boolean match = false;
            String answer = "";
            while (!match && !_CANCEL.equalsIgnoreCase(answer)) {
                // output to normal System.out
                System.out.print(question); // intended print, not println (!)
                answer = readLine();
                if ("".equals(answer) && answerRequired) {
                    // check default answer
                    if (defaultAnswer != null) {
                        match = true;
                        answer = defaultAnswer;
                    }
                } else {
                    if (answers != null && answers.size() > 0) {
                        Iterator<String> answersAsIterator = answers.iterator();
                        while (answersAsIterator.hasNext()) {
                            if (answer.equalsIgnoreCase(answersAsIterator.next())) {
                                match = true;
                            }
                        }
                    } else {
                        match = true;
                    }
                    if (!match && !_CANCEL.equalsIgnoreCase(answer)) {
                        message(getText(C_INVALID_ANSWER, answer));
                    }
                }
            }
            if (_CANCEL.equalsIgnoreCase(answer)) {
                throw new InstallationCancelledException();
            }
            return answer;
        } catch (IOException ioe) {
            throw new InstallerException(ioe);
        }
    }

    /**
     * Send a signal through the tunnel, and then wait for the answer from the other side.
     * 
     * <pre>
     *            (2)  [Driver]   receives question  [Tunnel]   sends question   [Console]  (1)
     *            (3)  [Driver]   sends answer       [Tunnel]   receives answer  [Console]  (4)
     * </pre>
     */
    private String readLine() throws IOException {
        InputStream inputStream;
        String line = "";
        if (_tunnel == null) {
            inputStream = System.in;
        } else {
            inputStream = _tunnel.getAnswerReceiverStream();
            _tunnel.getQuestionSenderStream().write(Tunnel.NEW_LINE.getBytes());
            _tunnel.getQuestionSenderStream().flush();
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        line = reader.readLine();
        return line;
    }

    private void selectLanguage() {
        List<String> availableLanguages = new ArrayList<String>(2);
        availableLanguages.add(getText(C_ENGLISH));
        availableLanguages.add(getText(C_GERMAN)); // 1 == German
        List<String> answers = new ArrayList<String>(availableLanguages.size());
        String languages = "";
        String defaultAnswer = null;
        for (Iterator<String> iterator = availableLanguages.iterator(); iterator.hasNext();) {
            String language = iterator.next();
            String possibleAnswer = language.substring(0, 1);
            if (defaultAnswer == null) {
                defaultAnswer = possibleAnswer;
            }
            languages = languages + language + ", ";
            answers.add(possibleAnswer.toLowerCase());
        }
        languages = languages.substring(0, languages.length() - 2);
        message(getText(C_AVAILABLE_LANGUAGES, languages));
        String answer = question(getText(C_SELECT_LANGUAGE), answers, defaultAnswer);
        if (answer.equalsIgnoreCase(answers.get(1))) {
            Installation.setLanguage(Locale.GERMAN);
        } else {
            Installation.setLanguage(Locale.ENGLISH);
        }
    }

    private InstallationType selectInstallationType() {
        InstallationType installationType = new InstallationType();
        String no = getText(C_NO);
        String yes = getText(C_YES);
        message(getText(C_INSTALL_TYPES));
        message("  " + Installation.ALL + ". " + getText(C_ALL));
        message("  " + Installation.STANDARD + ". " + getText(C_STANDARD));
        message("  " + Installation.MINIMUM + ". " + getText(C_MINIMUM));
        message("  " + Installation.STANDALONE + ". " + getText(C_STANDALONE));
        String answer = question(getText(C_SELECT_INSTALL_TYPE), getTypeAnswers(), Installation.ALL);
        if (Installation.ALL.equals(answer)) {
            installationType.setAll();
        } else if (Installation.STANDARD.equals(answer)) {
            installationType.setStandard();
        } else if (Installation.MINIMUM.equals(answer)) {
            installationType.setMinimum();
        } else if (Installation.STANDALONE.equals(answer)) {
            installationType.setStandalone();
        }
        if (!installationType.isStandalone()) {
            // include parts ?
            if (!installationType.isAll()) {
                answer = question(getText(C_INCLUDE), getYNAnswers(), no);
                if (yes.equals(answer)) {
                    do {
                        answer = question(getText(C_INEXCLUDE_PARTS, no), getInExcludeAnswers(), no);
                        if (InstallerCommandLine.INEXCLUDE_LIBRARY_MODULES.equals(answer)) {
                            installationType.addLibraryModules();
                        } else if (InstallerCommandLine.INEXCLUDE_DEMOS_AND_EXAMPLES.equals(answer)) {
                            installationType.addDemosAndExamples();
                        } else if (InstallerCommandLine.INEXCLUDE_DOCUMENTATION.equals(answer)) {
                            installationType.addDocumentation();
                        } else if (InstallerCommandLine.INEXCLUDE_SOURCES.equals(answer)) {
                            installationType.addSources();
                        } else if (InstallerCommandLine.INEXCLUDE_ENSUREPIP.equals(answer)) {
                            installationType.addEnsurepip();
                        }
                        if (!no.equals(answer)) {
                            message(getText(C_SCHEDULED, answer));
                        }
                    } while (!no.equals(answer));
                }
            }
            // exclude parts ?
            if (!installationType.isMinimum()) {
                answer = question(getText(C_EXCLUDE), getYNAnswers(), no);
                if (yes.equals(answer)) {
                    do {
                        answer = question(getText(C_INEXCLUDE_PARTS, no), getInExcludeAnswers(), no);
                        if (InstallerCommandLine.INEXCLUDE_LIBRARY_MODULES.equals(answer)) {
                            installationType.removeLibraryModules();
                        } else if (InstallerCommandLine.INEXCLUDE_DEMOS_AND_EXAMPLES.equals(answer)) {
                            installationType.removeDemosAndExamples();
                        } else if (InstallerCommandLine.INEXCLUDE_DOCUMENTATION.equals(answer)) {
                            installationType.removeDocumentation();
                        } else if (InstallerCommandLine.INEXCLUDE_SOURCES.equals(answer)) {
                            installationType.removeSources();
                        } else if (InstallerCommandLine.INEXCLUDE_ENSUREPIP.equals(answer)) {
                            installationType.removeEnsurepip();
                        }
                        if (!no.equals(answer)) {
                            message(getText(C_UNSCHEDULED, answer));
                        }
                    } while (!no.equals(answer));
                }
            }
        }
        return installationType;
    }

    private JavaHomeHandler checkVersion(JavaHomeHandler javaHomeHandler) {
        // handle target java version
        JavaInfo javaInfo = verifyTargetJava(javaHomeHandler);
        message(getText(C_JAVA_VERSION,
                        javaInfo.getJavaVersionInfo().getVendor(),
                        javaInfo.getJavaVersionInfo().getVersion()));
        if (!Installation.isValidJava(javaInfo.getJavaVersionInfo())) {
            message(getText(C_UNSUPPORTED_JAVA));
            question(getText(C_PROCEED_ANYWAY));
        }
        // handle OS
        String osName = System.getProperty(Installation.OS_NAME);
        String osVersion = System.getProperty(Installation.OS_VERSION);
        message(getText(C_OS_VERSION, osName, osVersion));
        if (!Installation.isValidOs()) {
            message(getText(C_UNSUPPORTED_OS));
            question(getText(C_PROCEED_ANYWAY));
        }
        return javaInfo.getJavaHomeHandler();
    }

    private JavaHomeHandler checkVersionSilent(JavaHomeHandler javaHomeHandler) {
        // check target java version
        JavaInfo javaInfo = verifyTargetJava(javaHomeHandler);
        if (!Installation.isValidJava(javaInfo.getJavaVersionInfo())) {
            message(getText(C_UNSUPPORTED_JAVA));
        }
        // check OS
        if (!Installation.isValidOs()) {
            message(getText(C_UNSUPPORTED_OS));
        }
        return javaInfo.getJavaHomeHandler();
    }

    private JavaInfo verifyTargetJava(JavaHomeHandler javaHomeHandler) {
        JavaVersionInfo javaVersionInfo = new JavaVersionInfo();
        Installation.fillJavaVersionInfo(javaVersionInfo, System.getProperties()); // a priori
        if (javaHomeHandler.isDeviation()) {
            javaVersionInfo = Installation.getExternalJavaVersion(javaHomeHandler);
            if (javaVersionInfo.getErrorCode() != Installation.NORMAL_RETURN) {
                // switch back to current if an error occurred
                message(getText(C_TO_CURRENT_JAVA, javaVersionInfo.getReason()));
                javaHomeHandler = new JavaHomeHandler();
            }
        }
        JavaInfo javaInfo = new JavaInfo();
        javaInfo.setJavaHomeHandler(javaHomeHandler);
        javaInfo.setJavaVersionInfo(javaVersionInfo);
        return javaInfo;
    }

    private void acceptLicense() {
        String no = getText(C_NO);
        String yes = getText(C_YES);
        String read = question(getText(C_READ_LICENSE), getYNAnswers(), no);
        if (read.equalsIgnoreCase(getText(C_YES))) {
            String licenseText = "n/a";
            try {
                licenseText = _jarInfo.getLicenseText();
                message(licenseText);
            } catch (IOException ioe) {
                throw new InstallerException(ioe);
            }
        }
        String accept = question(getText(C_ACCEPT), getYNAnswers(), yes);
        if (!accept.equalsIgnoreCase(getText(C_YES))) {
            throw new InstallationCancelledException();
        }
    }

    private File determineTargetDirectory() {
        String no = getText(C_NO);
        String yes = getText(C_YES);
        File targetDirectory = null;
        try {
            do {
                targetDirectory = new File(question(getText(C_ENTER_TARGET_DIRECTORY), true));
                if (targetDirectory.exists()) {
                    if (!targetDirectory.isDirectory()) {
                        message(getText(C_NOT_A_DIRECTORY, targetDirectory.getCanonicalPath()));
                    } else {
                        if (targetDirectory.list().length > 0) {
                            String overwrite = question(getText(C_OVERWRITE_DIRECTORY,
                                                                targetDirectory.getCanonicalPath()),
                                                        getYNAnswers(),
                                                        no);
                            if (overwrite.equalsIgnoreCase(getText(C_YES))) {
                                String clear = question(getText(C_CLEAR_DIRECTORY,
                                                                targetDirectory.getCanonicalPath()),
                                                        getYNAnswers(),
                                                        yes);
                                if (clear.equalsIgnoreCase(getText(C_YES))) {
                                    clearDirectory(targetDirectory);
                                }
                            }
                        }
                    }
                } else {
                    String create = question(getText(C_CREATE_DIRECTORY,
                                                     targetDirectory.getCanonicalPath()),
                                             getYNAnswers(),
                                             yes);
                    if (create.equalsIgnoreCase(getText(C_YES))) {
                        if (!targetDirectory.mkdirs()) {
                            throw new InstallerException(getText(C_UNABLE_CREATE_DIRECTORY,
                                                                 targetDirectory.getCanonicalPath()));
                        }
                    }
                }
            } while (!targetDirectory.exists() || !targetDirectory.isDirectory()
                    || targetDirectory.list().length > 0);
        } catch (IOException ioe) {
            throw new InstallerException(ioe);
        }
        return targetDirectory;
    }

    private JavaHomeHandler determineJavaHome() {
        return new JavaHomeHandler();
    }

    private void checkTargetDirectorySilent(File targetDirectory) {
        try {
            if (!targetDirectory.exists()) {
                // create directory
                if (!targetDirectory.mkdirs()) {
                    throw new InstallerException(getText(C_UNABLE_CREATE_DIRECTORY,
                                                         targetDirectory.getCanonicalPath()));
                }
            } else {
                // assert it is an empty directory
                if (!targetDirectory.isDirectory()) {
                    throw new InstallerException(getText(C_NOT_A_DIRECTORY,
                                                         targetDirectory.getCanonicalPath()));
                } else {
                    if (targetDirectory.list().length > 0) {
                        throw new InstallerException(getText(C_NON_EMPTY_TARGET_DIRECTORY,
                                                             targetDirectory.getCanonicalPath()));
                    }
                }
            }
        } catch (IOException ioe) {
            throw new InstallerException(ioe);
        }
    }

    private void showReadme(final File targetDirectory) {
        String no = getText(C_NO);
        String read = question(getText(C_READ_README), getYNAnswers(), no);
        if (read.equalsIgnoreCase(getText(C_YES))) {
            try {
                message(_jarInfo.getReadmeText());
                question(getText(C_PROCEED));
            } catch (IOException ioe) {
                throw new InstallerException(ioe);
            }
        }
    }

    private void clearDirectory(File targetDirectory) {
        File files[] = targetDirectory.listFiles();
        for (int i = 0; i < files.length; i++) {
            if (files[i].isDirectory()) {
                clearDirectory(files[i]);
            }
            if (!files[i].delete()) {
                throw new InstallerException(getText(C_UNABLE_TO_DELETE, files[i].getAbsolutePath()));
            }
        }
    }

    private void promptForCopying(final File targetDirectory,
                                  final InstallationType installationType,
                                  final JavaHomeHandler javaHomeHandler) {
        try {
            message(getText(C_SUMMARY));
            if (installationType.isStandalone()) {
                message(" - " + InstallerCommandLine.TYPE_STANDALONE);
            } else {
                message("  - " + InstallerCommandLine.INEXCLUDE_LIBRARY_MODULES + ": "
                        + installationType.installLibraryModules());
                message("  - " + InstallerCommandLine.INEXCLUDE_DEMOS_AND_EXAMPLES + ": "
                        + installationType.installDemosAndExamples());
                message("  - " + InstallerCommandLine.INEXCLUDE_DOCUMENTATION + ": "
                        + installationType.installDocumentation());
                message("  - " + InstallerCommandLine.INEXCLUDE_SOURCES + ": "
                        + installationType.installSources());
                message("  - " + InstallerCommandLine.INEXCLUDE_ENSUREPIP + ": "
                        + installationType.ensurepip());
                if (javaHomeHandler.isValidHome()) {
                    message("  - JRE: " + javaHomeHandler.getHome().getAbsolutePath());
                } else {
                    message("  - java");
                }
            }
            String proceed = question(getText(C_CONFIRM_TARGET, targetDirectory.getCanonicalPath()),
                                      getYNAnswers(), getText(C_YES));
            if (!proceed.equalsIgnoreCase(getText(C_YES))) {
                throw new InstallationCancelledException();
            }
        } catch (IOException ioe) {
            throw new InstallerException(ioe); // catch for the compiler
        }
    }

    private void success(final File targetDirectory) {
        try {
            message(getText(C_CONGRATULATIONS) + " "
                    + getText(C_SUCCESS, _jarInfo.getVersion(), targetDirectory.getCanonicalPath()));
        } catch (IOException ioe) {
            throw new InstallerException(ioe); // catch for the compiler
        }
    }

    private List<String> getTypeAnswers() {
        List<String> answers = new ArrayList<String>(4);
        answers.add(Installation.ALL);
        answers.add(Installation.STANDARD);
        answers.add(Installation.MINIMUM);
        answers.add(Installation.STANDALONE);
        return answers;
    }

    private List<String> getYNAnswers() {
        List<String> answers = new ArrayList<String>(2);
        answers.add(getText(C_YES));
        answers.add(getText(C_NO));
        return answers;
    }

    private List<String> getInExcludeAnswers() {
        List<String> answers = new ArrayList<String>(5);
        answers.add(InstallerCommandLine.INEXCLUDE_LIBRARY_MODULES);
        answers.add(InstallerCommandLine.INEXCLUDE_DEMOS_AND_EXAMPLES);
        answers.add(InstallerCommandLine.INEXCLUDE_DOCUMENTATION);
        answers.add(InstallerCommandLine.INEXCLUDE_SOURCES);
        answers.add(InstallerCommandLine.INEXCLUDE_ENSUREPIP);
        answers.add(getText(C_NO));
        return answers;
    }

    private void progressMessage(int percentage) {
        message(" " + percentage + " %");
    }

    private String getText(String textKey) {
        return Installation.getText(textKey);
    }

    private String getText(String textKey, String parameter0) {
        return Installation.getText(textKey, parameter0);
    }

    private String getText(String textKey, String parameter0, String parameter1) {
        return Installation.getText(textKey, parameter0, parameter1);
    }

    private static class JavaInfo {

        private JavaVersionInfo _javaVersionInfo;

        private JavaHomeHandler _javaHomeHandler;

        void setJavaHomeHandler(JavaHomeHandler javaHomeHandler) {
            _javaHomeHandler = javaHomeHandler;
        }

        JavaHomeHandler getJavaHomeHandler() {
            return _javaHomeHandler;
        }

        void setJavaVersionInfo(JavaVersionInfo javaVersionInfo) {
            _javaVersionInfo = javaVersionInfo;
        }

        JavaVersionInfo getJavaVersionInfo() {
            return _javaVersionInfo;
        }
    }

    //
    // interface ProgressListener
    //
    public void progressChanged(int newPercentage) {
        progressMessage(newPercentage);
    }

    public int getInterval() {
        return 10; // fixed interval for console installer
    }

    public void progressFinished() {
        progressMessage(100);
    }

    public void progressEntry(String entry) {
    // ignore the single entries - only used in gui mode
    }

    public void progressStartScripts() {
        message(getText(C_GENERATING_START_SCRIPTS));
    }

    public void progressStandalone() {
        message(getText(C_PACKING_STANDALONE_JAR));
    }

    public void progressEnsurepip() { message(getText(C_ENSUREPIP)); }
}