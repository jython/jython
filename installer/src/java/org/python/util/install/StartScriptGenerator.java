package org.python.util.install;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermissions;


public class StartScriptGenerator {

    private File _targetDirectory;

    public StartScriptGenerator(File targetDirectory, JavaHomeHandler javaHomeHandler) {
        _targetDirectory = targetDirectory;
    }

    protected boolean hasCPython27() {
        int errorCode = 0;
        try {
            String command[] = new String[]{
                    "/usr/bin/env", "python2.7", "-E",
                    "-c",
                    "import sys; " +
                    "assert sys.version_info.major == 2 and sys.version_info.minor == 7, " +
                    "'Need Python 2.7, got %r' % (sys.version_info,)"};
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
        Path bindir = _targetDirectory.toPath().resolve("bin");
        if (Installation.isWindows()) {
            Files.delete(bindir.resolve("jython"));
            Files.delete(bindir.resolve("jython.py"));
        }
        else {
            if (hasCPython27()) {
                Files.move(bindir.resolve("jython.py"), bindir.resolve("jython"),
                        StandardCopyOption.REPLACE_EXISTING);
            } else {
                Files.delete(bindir.resolve("jython.py"));
            }
            Files.delete(bindir.resolve("jython.exe"));
            Files.delete(bindir.resolve("python27.dll"));
            Files.setPosixFilePermissions(bindir.resolve("jython"),
                    PosixFilePermissions.fromString("rwxr-xr-x")); // 0755
        }
    }

}
