package org.python.util.install.driver;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

import org.python.util.install.ChildProcess;
import org.python.util.install.FileHelper;

public class NormalVerifier implements Verifier {

    protected static final String AUTOTEST_PY = "autotest.py";

    private static final String BIN = "bin";

    private static final String JYTHON_UP = "jython up and running!";

    private static final String JYTHON = "jython";

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
    }

    /**
     * Will be overridden in subclass StandaloneVerifier
     * 
     * @return the command array to start jython with
     * @throws DriverException
     *             if there was a problem getting the target directory path
     */
    protected String[] getSimpleCommand() throws DriverException {
        return new String[] {
                Paths.get(BIN).resolve(JYTHON).toString(),
                _targetDir.toPath().resolve(AUTOTEST_PY).toString() };
    }

    /**
     * @return The directory where to create the shell script test command in.
     * 
     * @throws DriverException
     */
    protected final File getShellScriptTestCommandDir() throws DriverException {
        return _targetDir.toPath().resolve(BIN).toFile();
    }

    /**
     * Internal method verifying a jython-starting command by capturing the output
     * 
     * @param command
     * 
     * @throws DriverException
     */
    private void verifyStart(String[] command) throws DriverException {
        ChildProcess p = new ChildProcess(command);
        p.setDebug(true);
        p.setCWD(_targetDir.toPath());
        System.err.println("Verify start: command=" + Arrays.toString(command) + ", cwd="  + p.getCWD());
        int exitValue = p.run();
//        if (exitValue != 0) {
//            throw new DriverException("start of jython failed\n"
//                    + "command: " + Arrays.toString(command)
//                    + "\ncwd: " + p.getCWD()
//                    + "\nexit value: " + exitValue
//                    + "\nstdout: " + p.getStdout()
//                    + "\nstderr: " + p.getStderr());
//        }
        verifyError(p.getStderr());
        verifyOutput(p.getStdout());
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

    private void verifyError(List<String> stderr) throws DriverException {
        for (String line : stderr) {
            if (isExpectedError(line)) {
                feedback(line);
            } else {
                throw new DriverException(stderr.toString());
            }
        }
    }

    private boolean isExpectedError(String line) {
        boolean expected = false;
        if (line.startsWith("*sys-package-mgr*")) {
            expected = true;
        }
        return expected;
    }

    private void verifyOutput(List<String> stdout) throws DriverException {
        boolean started = false;
        for (String line : stdout) {
            if (isExpectedOutput(line)) {
                feedback(line);
                if (line.startsWith(JYTHON_UP)) {
                    started = true;
                }
            } else {
                throw new DriverException(stdout.toString());
            }
        }
        if (!started) {
            throw new DriverException("start of jython failed:\n" + stdout.toString());
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
        System.out.println("feedback " + line);
    }
}
