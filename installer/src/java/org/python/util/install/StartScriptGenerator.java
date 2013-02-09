package org.python.util.install;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Date;

public class StartScriptGenerator {

    protected final static int UNIX_FLAVOUR = 10;

    protected final static int WINDOWS_FLAVOUR = 30;

    protected final static int BOTH_FLAVOUR = 50;

    protected final static String WIN_CR_LF;

    private final static String JAVA_HOME = "JAVA_HOME";

    /** do not hard-wire JYTHON_HOME */
    private final static String JYTHON_HOME_FALLBACK = "JYTHON_HOME_FALLBACK";

    private final static String JYTHON = "jython";

    private final static String JYTHON_BAT = "jython.bat";
    static {
        int dInt = Integer.parseInt("0d", 16);
        int aInt = Integer.parseInt("0a", 16);
        WIN_CR_LF = new String(new char[] {(char)dInt, (char)aInt});
    }

    private File _targetDirectory;

    private JavaHomeHandler _javaHomeHandler;

    private int _flavour;

    public StartScriptGenerator(File targetDirectory, JavaHomeHandler javaHomeHandler) {
        _targetDirectory = targetDirectory;
        _javaHomeHandler = javaHomeHandler;
        if (Installation.isWindows()) {
            setFlavour(WINDOWS_FLAVOUR);
        } else {
            // everything else defaults to unix at the moment
            setFlavour(UNIX_FLAVOUR);
        }
    }

    protected void setFlavour(int flavour) {
        _flavour = flavour;
        if (flavour == WINDOWS_FLAVOUR) {
            // check if we should create unix like scripts, too
            if (hasUnixlikeShell()) {
                _flavour = BOTH_FLAVOUR;
            }
        }
    }

    protected int getFlavour() {
        return _flavour;
    }

    protected boolean hasUnixlikeShell() {
        int errorCode = 0;
        try {
            String command[] = new String[] {"sh", "-c", "env"};
            long timeout = 3000;
            ChildProcess childProcess = new ChildProcess(command, timeout);
            childProcess.setDebug(false);
            childProcess.setSilent(true);
            errorCode = childProcess.run();
        } catch (Throwable t) {
            errorCode = 1;
        }
        return errorCode == 0;
    }

    protected final void generateStartScripts() throws IOException {
        File bin = new File(getTargetDirectory(), "bin");
        File bin_jython = new File(bin, JYTHON);
        switch(getFlavour()){
            case BOTH_FLAVOUR:
                writeToTargetDir(JYTHON_BAT, getJythonScript(WINDOWS_FLAVOUR));
                FileHelper.makeExecutable(writeToTargetDir(JYTHON, getJythonScript(BOTH_FLAVOUR)));
                FileHelper.makeExecutable(bin_jython);
                break;
            case WINDOWS_FLAVOUR:
                writeToTargetDir(JYTHON_BAT, getJythonScript(WINDOWS_FLAVOUR));
                // delete the *nix script in /bin dir
                bin_jython.delete();
                break;
            default:
                FileHelper.makeExecutable(writeToTargetDir(JYTHON, getJythonScript(UNIX_FLAVOUR)));
                FileHelper.makeExecutable(bin_jython);
                // delete the windows script in /bin dir
                File bin_jython_bat = new File(bin, JYTHON_BAT);
                bin_jython_bat.delete();
                break;
        }
    }

    /**
     * only <code>protected</code> for unit test use
     */
    protected final String getJythonScript(int flavour) throws IOException {
        if (flavour == WINDOWS_FLAVOUR) {
            return getStartScript(getWindowsJythonTemplate()) + readFromFile(JYTHON_BAT);
        } else {
            return getStartScript(getUnixJythonTemplate()) + readFromFile(JYTHON);
        }
    }

    /**
     * These placeholders are valid for all private methods:
     * 
     * {0} : current date <br>
     * {1} : user.name <br>
     * {2} : target directory <br>
     */
    private String getStartScript(String template) throws IOException {
        String parameters[] = new String[4];
        parameters[0] = new Date().toString();
        parameters[1] = System.getProperty("user.name");
        parameters[2] = getTargetDirectory().getCanonicalPath();
        return MessageFormat.format(template, (Object[])parameters);
    }

    /**
     * placeholders:
     * 
     * @see getStartScript
     */
    private String getWindowsJythonTemplate() {
        StringBuilder builder = getWindowsHeaderTemplate();
        builder.append("set ");
        builder.append(JAVA_HOME);
        builder.append("=");
        if (_javaHomeHandler.isValidHome()) {
            builder.append("\"");
            builder.append(_javaHomeHandler.getHome().getAbsolutePath());
            builder.append("\"");
        }
        builder.append(WIN_CR_LF);
        builder.append("set ");
        builder.append(JYTHON_HOME_FALLBACK);
        builder.append("=\"{2}\"");
        builder.append(WIN_CR_LF);
        builder.append(WIN_CR_LF);
        return builder.toString();
    }

    /**
     * placeholders:
     * 
     * @see getStartScript
     */
    private StringBuilder getWindowsHeaderTemplate() {
        StringBuilder builder = new StringBuilder(1000);
        builder.append("@echo off");
        builder.append(WIN_CR_LF);
        builder.append("rem This file was generated by the Jython installer");
        builder.append(WIN_CR_LF);
        builder.append("rem Created on {0} by {1}");
        builder.append(WIN_CR_LF);
        builder.append(WIN_CR_LF);
        return builder;
    }

    /**
     * placeholders:
     * 
     * @see getStartScript
     */
    private String getUnixJythonTemplate() {
        StringBuilder builder = getUnixHeaderTemplate();
        builder.append(JAVA_HOME);
        builder.append("=");
        if (_javaHomeHandler.isValidHome()) {
            builder.append("\"");
            builder.append(_javaHomeHandler.getHome().getAbsolutePath());
            builder.append("\"");
        }
        builder.append("\n");
        builder.append(JYTHON_HOME_FALLBACK);
        builder.append("=\"{2}\"\n");
        builder.append("\n");
        return builder.toString();
    }

    /**
     * placeholders:
     * 
     * @see getStartScript
     */
    private StringBuilder getUnixHeaderTemplate() {
        StringBuilder builder = new StringBuilder(1000);
        builder.append("#!/usr/bin/env bash\n");
        builder.append("\n");
        builder.append("# This file was generated by the Jython installer\n");
        builder.append("# Created on {0} by {1}\n");
        builder.append("\n");
        return builder;
    }

    /**
     * @param fileName
     *            The short file name, e.g. JYTHON_BAT
     * 
     * @throws IOException
     */
    private String readFromFile(String fileName) throws IOException {
        // default runtime location
        File targetDirectory = getTargetDirectory();
        File file = new File(new File(targetDirectory, "bin"), fileName);
        if (!file.exists()) {
            // deviation: test time location
            file = new File(targetDirectory, fileName);
        }
        return FileHelper.readAll(file);
    }

    /**
     * Create (or overwrite) the specified file in the target directory
     * 
     * @param fileName
     *            The short file name, e.g. JYTHON_BAT
     * @param contents
     * 
     * @throws IOException
     */
    private File writeToTargetDir(String fileName, String contents) throws IOException {
        File file = new File(getTargetDirectory(), fileName);
        FileHelper.write(file, contents);
        return file;
    }

    private File getTargetDirectory() {
        return _targetDirectory;
    }
}
