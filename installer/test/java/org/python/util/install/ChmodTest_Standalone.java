package org.python.util.install;

import java.io.File;
import java.io.IOException;

/**
 * Helper class to test of 'chmod' on different platforms
 */
public class ChmodTest_Standalone {

    private static String _mode = "755"; // default mode

    public static void main(String[] args) {
        // get mode from first argument, if present
        if (args.length > 0) {
            _mode = args[0];
        }

        // create an empty test file in the current directory
        String curdir = System.getProperty("user.dir");
        File testFile = new File(curdir, "chmod.test");
        String path = testFile.getAbsolutePath();
        if (!testFile.exists()) {
            try {
                if (!testFile.createNewFile()) {
                    System.err.println(getPrefix() + "unable to create file " + path);
                    System.exit(1);
                }
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(1);
            }
        }

        // apply the chmod command on the test file
        if (!testFile.exists()) {
            System.err.println(getPrefix() + "unable to create file " + path);
            System.exit(1);
        } else {
            String command[] = new String[] {"chmod", _mode, path};
            ChildProcess childProcess = new ChildProcess(command, 3000);
            childProcess.setDebug(true);
            int exitValue = childProcess.run();
            if (exitValue != 0) {
                System.err.println(getPrefix() + "error during chmod");
            } else {
                System.out.println(getPrefix() + "chmod command executed on " + path);
            }
            System.exit(exitValue);
        }
    }

    private static String getPrefix() {
        return "[ChmodTest_Standalone] ";
    }

}
