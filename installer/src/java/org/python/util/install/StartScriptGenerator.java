package org.python.util.install;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;


public class StartScriptGenerator {

    private File _targetDirectory;

    public StartScriptGenerator(File targetDirectory, JavaHomeHandler javaHomeHandler) {
        _targetDirectory = targetDirectory;
    }

    protected String getShebang() {
        String shebang = null;
        try {
            String command[] = new String[]{
                    "/usr/bin/env", "python2.7", "-E",
                    "-c",
                    "import sys; " +
                    "assert sys.version_info.major == 2 and sys.version_info.minor == 7, " +
                    "'Need Python 2.7, got %r' % (sys.version_info,);" +
                    "print sys.executable"};
            long timeout = 3000;
            ChildProcess childProcess = new ChildProcess(command, timeout);
            childProcess.setDebug(false);
            childProcess.setSilent(true);
            int errorCode = childProcess.run();
            if (errorCode == 0) {
                // The whole point of this exercise is that we do not
                // want the launcher to interpret or otherwise intercept
                // any PYTHON environment variables that are being passed through.
                // However, a shebang like /usr/bin/env python2.7 -E
                // with an extra argument (-E) in general does not work,
                // such as on Linux, so we have to replace with a hard-coded
                // path
                shebang = "#!" + childProcess.getStdout().get(0) + " -E";
            }
        } catch (Throwable t) {
        }
        return shebang;
    }

    private final void generateLauncher(String shebang, File infile, File outfile)
            throws IOException {
        try (
                BufferedReader br = new BufferedReader(new FileReader(infile));
                BufferedWriter bw = new BufferedWriter(new FileWriter(outfile))) {
            int i = 0;
            for (String line; (line = br.readLine()) != null; i += 1) {
                if (i == 0) {
                    bw.write(shebang);
                } else {
                    bw.write(line);
                }
                bw.newLine();
            }
        }
    }

    protected final void generateStartScripts() throws IOException {
        Path bindir = _targetDirectory.toPath().resolve("bin");
        if (Installation.isWindows()) {
            Files.delete(bindir.resolve("jython"));
            Files.delete(bindir.resolve("jython.py"));
        }
        else {
            String shebang = getShebang();
            if (shebang != null) {
                generateLauncher(shebang,
                        bindir.resolve("jython.py").toFile(),
                        bindir.resolve("jython").toFile());
            }
            Files.delete(bindir.resolve("jython.py"));
            Files.delete(bindir.resolve("jython.exe"));
            Files.setPosixFilePermissions(bindir.resolve("jython"),
                    PosixFilePermissions.fromString("rwxr-xr-x")); // 0755
        }
    }

}
