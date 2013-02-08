package org.python.util.install;

import java.io.File;

/**
 * Helper class to test a java version
 */
public class JavaVersionTester {

    public static final String JAVA_HOME = "java.home";
    protected static final String JAVA_VERSION = "java.version";
    public static final String JAVA_SPECIFICATION_VERSION = "java.specification.version";
    protected static final String JAVA_VENDOR = "java.vendor";
    private static final String NEWLINE = "\n";

    private static final String UNKNOWN = "<unknown>";

    public static void main(String[] args) {
        if (args.length > 0) {
            String tempFilePath = args[0];
            File tempFile = new File(tempFilePath);
            if (tempFile.exists() && tempFile.canWrite()) {
                try {
                    writeTempFile(tempFile);
                } catch (Exception e) {
                    e.printStackTrace();
                    System.exit(1);
                }
            } else {
                if (!tempFile.exists()) {
                    System.err.println("temp file " + tempFilePath + " does not exist");
                } else {
                    System.err.println("cannot write to temp file " + tempFilePath);
                }
                System.exit(1);
            }
        } else {
            System.err.println("no temp file given. usage: JavaVersionTester tempfile");
            System.out.println("exiting with 1");
            System.exit(1);
        }
    }

    private static void writeTempFile(File file) throws Exception {
        FileHelper.write(file, createFileContent());
    }

    private static String createFileContent() {
        StringBuffer sb = new StringBuffer(500);
        String java_home = new JavaHomeHandler().getExecutableName();
        if (File.separatorChar != '/') {
            java_home = java_home.replace(File.separatorChar, '/'); // backslash would be interpreted as escape char
        }
        sb.append(JAVA_HOME + "=" + java_home + NEWLINE);
        sb.append(JAVA_VERSION + "=" + System.getProperty(JAVA_VERSION, UNKNOWN) + NEWLINE);
        sb.append(JAVA_SPECIFICATION_VERSION + "=" + System.getProperty(JAVA_SPECIFICATION_VERSION, UNKNOWN) + NEWLINE);
        sb.append(JAVA_VENDOR + "=" + System.getProperty(JAVA_VENDOR, UNKNOWN) + NEWLINE);
        return sb.toString();
    }

}
