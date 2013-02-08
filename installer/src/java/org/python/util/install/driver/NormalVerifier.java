package org.python.util.install.driver;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.text.MessageFormat;
import java.util.StringTokenizer;

import org.python.util.install.ChildProcess;
import org.python.util.install.FileHelper;
import org.python.util.install.Installation;
import org.python.util.install.JavaHomeHandler;

public class NormalVerifier implements Verifier {

    protected static final String AUTOTEST_PY = "autotest.py";

    protected static final String JYTHON_TEST = "jython_test";

    private static final String BIN = "bin";

    private static final String BAT_EXTENSION = ".bat";

    private static final String JYTHON_UP = "jython up and running!";

    private static final String JYTHON = "jython";

    private static final String TEMPLATE_SUFFIX = ".template";

    private static final String VERIFYING = "verifying";

    private File _targetDir;

    public void setTargetDir(File targetDir) {
        _targetDir = targetDir;
    }

    public File getTargetDir() {
        return _targetDir;
    }

    public void verify() throws DriverException {
        createTestScriptFile(); // create the test .py script
        // verify the most simple start of jython works
        verifyStart(getSimpleCommand());
        if (doShellScriptTests()) {
            // verify more complex versions of starting jython
            verifyStart(getShellScriptTestCommand());
        }
    }

    /**
     * Will be overridden in subclass StandaloneVerifier
     * 
     * @return the command array to start jython with
     * @throws DriverException
     *             if there was a problem getting the target directory path
     */
    protected String[] getSimpleCommand() throws DriverException {
        String parentDirName = null;
        try {
            parentDirName = getTargetDir().getCanonicalPath() + File.separator;
        } catch (IOException ioe) {
            throw new DriverException(ioe);
        }
        String[] command = new String[2];
        if (Installation.isWindows()) {
            command[0] = parentDirName + JYTHON + BAT_EXTENSION;
        } else {
            command[0] = parentDirName + JYTHON;
        }
        command[1] = parentDirName + AUTOTEST_PY;
        return command;
    }

    /**
     * @return The command to test the shell script more deeply
     * @throws DriverException
     */
    protected final String[] getShellScriptTestCommand() throws DriverException {
        // first we have to create the shell script
        File testCommandDir = getShellScriptTestCommandDir();
        if (!testCommandDir.exists()) {
            if (!testCommandDir.mkdirs()) {
                throw new DriverException("unable to create directory "
                        + testCommandDir.getAbsolutePath());
            }
        }
        String commandName = JYTHON_TEST;
        boolean isWindows = Installation.isWindows();
        if (isWindows) {
            commandName = commandName.concat(BAT_EXTENSION);
        }
        File command = new File(testCommandDir, commandName);
        try {
            if (!command.exists()) {
                command.createNewFile();
            }
            FileHelper.write(command, getShellScriptTestContents());
            if (!isWindows) {
                FileHelper.makeExecutable(command);
            }
            return new String[] {command.getCanonicalPath()};
        } catch (Exception e) {
            throw new DriverException(e);
        }
    }

    /**
     * @return The contents of the shell test script
     * @throws DriverException
     */
    protected final String getShellScriptTestContents() throws DriverException {
        String contents = "";
        String templateName = JYTHON_TEST;
        if (Installation.isWindows()) {
            templateName = templateName.concat(BAT_EXTENSION);
        }
        templateName = templateName.concat(TEMPLATE_SUFFIX);
        InputStream inputStream = FileHelper.getRelativeURLAsStream(getClass(), templateName);
        if (inputStream != null) {
            try {
                String template = FileHelper.readAll(inputStream);
                String targetDirPath = getTargetDir().getCanonicalPath();
                String upScriptPath = getSimpleCommand()[1];
                JavaHomeHandler javaHomeHandler = new JavaHomeHandler();
                String javaHomeString = "";
                if (javaHomeHandler.isValidHome()) {
                    javaHomeString = javaHomeHandler.getHome().getAbsolutePath();
                }
                contents = MessageFormat.format(template,
                                                targetDirPath,
                                                upScriptPath,
                                                javaHomeString,
                                                VERIFYING);
            } catch (Exception e) {
                throw new DriverException(e);
            }
        }
        return contents;
    }

    /**
     * @return The directory where to create the shell script test command in.
     * 
     * @throws DriverException
     */
    protected final File getShellScriptTestCommandDir() throws DriverException {
        String dirName;
        try {
            dirName = getTargetDir().getCanonicalPath().concat(File.separator).concat(BIN);
            return new File(dirName);
        } catch (IOException ioe) {
            throw new DriverException(ioe);
        }
    }

    /**
     * Internal method verifying a jython-starting command by capturing the ouptut
     * 
     * @param command
     * 
     * @throws DriverException
     */
    private void verifyStart(String[] command) throws DriverException {
        ChildProcess childProcess = new ChildProcess(command);
        childProcess.setDebug(true);
        ByteArrayOutputStream redirectedErr = new ByteArrayOutputStream();
        ByteArrayOutputStream redirectedOut = new ByteArrayOutputStream();
        int exitValue = 0;
        PrintStream oldErr = System.err;
        PrintStream oldOut = System.out;
        try {
            System.setErr(new PrintStream(redirectedErr));
            System.setOut(new PrintStream(redirectedOut));
            exitValue = childProcess.run();
        } finally {
            System.setErr(oldErr);
            System.setOut(oldOut);
        }
        // verify the output
        String output = null;
        String error = null;
        try {
            redirectedErr.flush();
            redirectedOut.flush();
            String encoding = "US-ASCII";
            output = redirectedOut.toString(encoding);
            error = redirectedErr.toString(encoding);
        } catch (IOException ioe) {
            throw new DriverException(ioe);
        }
        if (exitValue != 0) {
            throw new DriverException("start of jython failed, output:\n" + output + "\nerror:\n"
                    + error);
        }
        verifyError(error);
        verifyOutput(output);
    }

    /**
     * Will be overridden in subclass StandaloneVerifier
     * 
     * @return <code>true</code> if the jython start shell script should be verified (using
     *         different options)
     */
    protected boolean doShellScriptTests() {
        return true;
    }

    private void verifyError(String error) throws DriverException {
        StringTokenizer tokenizer = new StringTokenizer(error, "\n");
        while (tokenizer.hasMoreTokens()) {
            String line = tokenizer.nextToken();
            if (isExpectedError(line)) {
                feedback(line);
            } else {
                throw new DriverException(error);
            }
        }
    }

    private boolean isExpectedError(String line) {
        boolean expected = false;
        if (line.startsWith("*sys-package-mgr*")) {
            expected = true;
        } else if (line.indexOf("32 bit") >= 0 && line.indexOf("64 bit") >= 0) {
            // OS X incompatibility message when using -A -j java1.6.0 from java1.5.0
            expected = true;
        }
        return expected;
    }

    private void verifyOutput(String output) throws DriverException {
        boolean started = false;
        StringTokenizer tokenizer = new StringTokenizer(output, "\n");
        while (tokenizer.hasMoreTokens()) {
            String line = tokenizer.nextToken();
            if (isExpectedOutput(line)) {
                feedback(line);
                if (line.startsWith(JYTHON_UP)) {
                    started = true;
                }
            } else {
                throw new DriverException(output);
            }
        }
        if (!started) {
            throw new DriverException("start of jython failed:\n" + output);
        }
    }

    private boolean isExpectedOutput(String line) {
        boolean expected = false;
        if (line.startsWith("[ChildProcess]") || line.startsWith(VERIFYING)) {
            expected = true;
        } else if (line.startsWith(JYTHON_UP)) {
            expected = true;
        }
        return expected;
    }

    private String getTestScript() {
        StringBuilder b = new StringBuilder(80);
        b.append("import sys\n");
        b.append("import os\n");
        b.append("print '");
        b.append(JYTHON_UP);
        b.append("'\n");
        return b.toString();
    }

    private void createTestScriptFile() throws DriverException {
        File file = new File(getTargetDir(), AUTOTEST_PY);
        try {
            FileHelper.write(file, getTestScript());
        } catch (IOException ioe) {
            throw new DriverException(ioe);
        }
    }

    private void feedback(String line) {
        System.out.println(line);
    }
}
