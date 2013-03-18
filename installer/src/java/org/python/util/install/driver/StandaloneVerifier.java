package org.python.util.install.driver;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.python.util.install.JavaHomeHandler;

public class StandaloneVerifier extends NormalVerifier {

    public void verify() throws DriverException {
        // make sure only JYTHON_JAR is in the target directory
        if (getTargetDir().listFiles().length > 1) {
            throw new DriverException("more than " + JYTHON_JAR + " installed");
        }
        // make sure JYTHON_JAR contains a MANIFEST and a /Lib directory
        verifyJythonJar();
        // do the jython startup verification from the superclass
        super.verify();
    }

    @Override
    protected String[] getSimpleCommand() throws DriverException {
        String parentDirName = null;
        try {
            parentDirName = getTargetDir().getCanonicalPath() + File.separator;
        } catch (IOException ioe) {
            throw new DriverException(ioe);
        }
        String command[] = new String[4];
        command[0] = new JavaHomeHandler().getExecutableName();
        command[1] = "-jar";
        command[2] = parentDirName + JYTHON_JAR;
        command[3] = parentDirName + AUTOTEST_PY;
        return command;
    }

    @Override
    protected boolean doShellScriptTests() {
        return false;
    }

    private void verifyJythonJar() throws DriverException {
        File jythonJar = getTargetDir().listFiles()[0];
        JarFile jar = null;
        try {
            jar = new JarFile(jythonJar);
            if (jar.getManifest() == null) {
                throw new DriverException(JYTHON_JAR + " contains no MANIFEST");
            }
            boolean hasLibDir = false;
            Enumeration<JarEntry> entries = jar.entries();
            while (!hasLibDir && entries.hasMoreElements()) {
                JarEntry entry = (JarEntry)entries.nextElement();
                if (entry.getName().startsWith("Lib/")) {
                    hasLibDir = true;
                }
            }
            if (!hasLibDir) {
                throw new DriverException(JYTHON_JAR + " contains no /Lib directory");
            }
        } catch (IOException e) {
            throw new DriverException(e);
        } finally {
            if (jar != null) {
                try {
                    jar.close();
                } catch (IOException ioe) {}
            }
        }
    }
}
