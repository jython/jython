package org.python.util.install.driver;

import java.io.File;
import java.io.IOException;

import org.python.util.install.FileHelper;
import org.python.util.install.Installation;
import org.python.util.install.InstallationListener;
import org.python.util.install.InstallerCommandLine;
import org.python.util.install.JavaHomeHandler;
import org.python.util.install.JavaVersionTester;
import org.python.util.install.Installation.JavaVersionInfo;

public abstract class Autotest implements InstallationListener {

    private static final String _DIR_SUFFIX = "_dir";

    private static int _count = 0; // unique test number
    private static File _rootDirectory = null;
    private static JavaVersionInfo _systemDefaultJavaVersion;

    private String _name;
    private File _targetDir;
    private JavaHomeHandler _javaHomeHandler;
    private boolean _verbose;
    private String[] _commandLineArgs;
    private Verifier _verifier;

    /**
     * constructor
     * 
     * @throws IOException
     * @throws DriverException
     */
    protected Autotest(InstallerCommandLine commandLine) throws IOException, DriverException {
        _count++;
        buildName();
        if (_rootDirectory == null) {
            createRootDirectory();
        }
        createTargetDirectory();
        setCommandLineArgs(new String[0]); // a priori value
        _verbose = commandLine.hasVerboseOption();
        _javaHomeHandler = commandLine.getJavaHomeHandler();
    }

    /**
     * @return the root directory for all test installations
     */
    protected static File getRootDir() {
        return _rootDirectory;
    }

    /**
     * @return the target directory of this test
     */
    protected File getTargetDir() {
        return _targetDir;
    }

    /**
     * @return the name of this test
     */
    protected String getName() {
        return _name;
    }

    /**
     * @return the array of command line arguments
     */
    protected String[] getCommandLineArgs() {
        return _commandLineArgs;
    }

    /**
     * set the array of command line arguments
     * 
     * @param commandLineArgs
     */
    protected void setCommandLineArgs(String[] commandLineArgs) {
        _commandLineArgs = commandLineArgs;
    }

    /**
     * @return the java home handler, can be asked for deviation using <code>isDeviation()</code>.
     */
    protected JavaHomeHandler getJavaHomeHandler() {
        return _javaHomeHandler;
    }

    /**
     * @return <code>true</code> if this test should be verbose
     */
    protected boolean isVerbose() {
        return _verbose;
    }

    /**
     * @return the name suffix for this test
     */
    protected abstract String getNameSuffix();

    /**
     * @throws DriverException if the target directory does not exist or is empty (installation failed)
     */
    protected void assertTargetDirNotEmpty() throws DriverException {
        File targetDir = getTargetDir();
        if (targetDir != null) {
            if (targetDir.exists() && targetDir.isDirectory()) {
                if (targetDir.listFiles().length > 0) {
                    return;
                }
            }
        }
        throw new DriverException("installation failed for " + targetDir.getAbsolutePath());
    }

    /**
     * Convenience method to add the additional arguments, if specified behind the -A option
     * <p>
     * This adds (if present):
     * <ul>
     * <li> target directory (should always be present)
     * <li> verbose
     * <li> jre
     * </ul>
     */
    protected void addAdditionalArguments() {
        if (getTargetDir() != null) {
            addArgument("-d");
            addArgument(getTargetDir().getAbsolutePath());
        }
        if (isVerbose()) {
            addArgument("-v");
        }
        JavaHomeHandler javaHomeHandler = getJavaHomeHandler();
        if (javaHomeHandler.isDeviation() && javaHomeHandler.isValidHome()) {
            addArgument("-j");
            addArgument(javaHomeHandler.getHome().getAbsolutePath());
        }
    }

    /**
     * Add an additional String argument to the command line arguments
     * 
     * @param newArgument
     */
    protected void addArgument(String newArgument) {
        setCommandLineArgs(addArgument(newArgument, getCommandLineArgs()));
    }

    /**
     * set the verifier
     */
    protected void setVerifier(Verifier verifier) {
        _verifier = verifier;
        _verifier.setTargetDir(getTargetDir());
    }

    protected Verifier getVerifier() {
        return _verifier;
    }

    //
    // private stuff
    //

    /**
     * build a test name containing some special characters (which will be used to create the target
     * directory), and store it into <code>_name</code>.
     */
    private void buildName() {
        StringBuilder b = new StringBuilder(24);
        if (_count <= 99) {
            b.append('0');
        }
        if (_count <= 9) {
            b.append('0');
        }
        b.append(_count);
        // explicitly use a blank, to nail down some platform specific problems
        b.append(' ');
        // add an exclamation mark if possible (see issue #1208)
        if(canHandleExclamationMarks()) {
            b.append('!');
        }
        b.append(getNameSuffix());
        b.append('_');
        _name = b.toString();
    }

    /**
     * Add the new argument to the args array
     * 
     * @param newArgument
     * @param args
     * 
     * @return the new String array, with size increased by 1
     */
    private String[] addArgument(String newArgument, String[] args) {
        String[] newArgs = new String[args.length + 1];
        for (int i = 0; i < args.length; i++) {
            newArgs[i] = args[i];
        }
        newArgs[args.length] = newArgument;
        return newArgs;
    }

    /**
     * create the root directory for all automatic installations
     * <p>
     * assumed to be a subdirectory of java.io.tmpdir
     * 
     * @throws IOException
     * @throws DriverException
     */
    private void createRootDirectory() throws IOException, DriverException {
        File tmpFile = File.createTempFile("jython.autoinstall.root_", _DIR_SUFFIX);
        if (FileHelper.createTempDirectory(tmpFile)) {
            _rootDirectory = tmpFile;
        } else {
            throw new DriverException("unable to create root temporary directory");
        }
    }

    /**
     * create a target directory for a test installation
     * 
     * @throws IOException
     * @throws DriverException
     */
    private void createTargetDirectory() throws IOException, DriverException {
        File tmpFile = File.createTempFile(getName(), _DIR_SUFFIX, _rootDirectory);
        if (FileHelper.createTempDirectory(tmpFile)) {
            _targetDir = tmpFile;
        } else {
            throw new DriverException("unable to create temporary target directory");
        }
    }

    /**
     * Determine if the target directory may contain an exclamation mark (see also issue #1208).
     * <p>
     * Autotests can handle exclamation marks, if both the running and the system default java
     * specification versions are 1.6 or higher. Class.getResource() was fixed for JDK 1.6, but only if the directory name does not end with '!'...
     * <p>
     * Currently there is no way on windows, because the enabledelayedexpansion in jython.bat cannot
     * handle exclamation marks in variable names.
     * 
     * @return <code>true</code> if we can handle exclamation marks, <code>false</code> otherwise
     */
    private boolean canHandleExclamationMarks() {
        boolean exclamation = false;
        if (!Installation.isWindows()) {
            // get the running java specification version
            String specificationVersion = System.getProperty(JavaVersionTester.JAVA_SPECIFICATION_VERSION,
                                                             "");
            if (Installation.getJavaSpecificationVersion(specificationVersion) > 15) {
                // get the system default java version
                if (_systemDefaultJavaVersion == null) {
                    _systemDefaultJavaVersion = Installation.getDefaultJavaVersion();
                }
                if (_systemDefaultJavaVersion.getErrorCode() == Installation.NORMAL_RETURN) {
                    specificationVersion = _systemDefaultJavaVersion.getSpecificationVersion();
                    if (Installation.getJavaSpecificationVersion(specificationVersion) > 15) {
                        exclamation = true;
                    }
                }
            }
        }
        return exclamation;
    }

}
