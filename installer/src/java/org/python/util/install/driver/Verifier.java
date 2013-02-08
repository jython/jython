package org.python.util.install.driver;

import java.io.File;

import org.python.util.install.JarInstaller;

public interface Verifier {

    public static final String JYTHON_JAR = JarInstaller.JYTHON_JAR;

    public void setTargetDir(File targetDir);

    public File getTargetDir();

    public void verify() throws DriverException;

}
