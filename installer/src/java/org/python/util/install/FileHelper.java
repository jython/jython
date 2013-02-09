package org.python.util.install;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * Helper methods for file handling during installation / installation verification
 */
public final class FileHelper {

    private final static String EXECUTABLE_MODE = "755";

    /**
     * create a temporary directory with the same name as the passed in File (which may exist as
     * file, not directory)
     * 
     * @param tempDirectory
     * @return <code>true</code> only if the the directory was successfully created (or already
     *         existed)
     */
    public static boolean createTempDirectory(File tempDirectory) {
        boolean success = true;
        if (!tempDirectory.isDirectory()) {
            if (tempDirectory.exists()) {
                success = carryOnResult(tempDirectory.delete(), success);
            }
            if (success) {
                success = tempDirectory.mkdirs();
            }
        }
        return success;
    }

    /**
     * completely remove a directory
     * 
     * @param dir
     * @return <code>true</code> if successful, <code>false</code> otherwise.
     */
    public static boolean rmdir(File dir) {
        boolean success = true;
        if (dir.exists()) {
            File[] files = dir.listFiles();
            for (int i = 0; i < files.length; i++) {
                File file = files[i];
                if (file.isFile()) {
                    success = carryOnResult(file.delete(), success);
                } else {
                    if (file.isDirectory()) {
                        success = carryOnResult(rmdir(file), success);
                    }
                }
            }
            success = carryOnResult(dir.delete(), success);
        }
        return success;
    }

    /**
     * read the contents of a file into a String
     * 
     * @param file
     *            The file has to exist
     * @return The contents of the file as String
     * @throws IOException
     */
    public static String readAll(File file) throws IOException {
        FileReader fileReader = new FileReader(file);
        try {
            StringBuffer sb = new StringBuffer();
            char[] b = new char[8192];
            int n;
            while ((n = fileReader.read(b)) > 0) {
                sb.append(b, 0, n);
            }
            return sb.toString();
        } finally {
            fileReader.close();
        }
    }

    /**
     * read the contents of a stream into a String
     * <p>
     * ATTN: does not handle encodings
     * 
     * @param inputStream
     *            The input stream
     * @return A String representation of the file contents
     * @throws IOException
     */
    public static String readAll(InputStream inputStream) throws IOException {
        try {
            StringBuffer sb = new StringBuffer();
            byte[] b = new byte[8192];
            int n;
            while ((n = inputStream.read(b)) > 0) {
                sb.append(new String(b, 0, n));
            }
            return sb.toString();
        } finally {
            inputStream.close();
        }
    }

    /**
     * Write contents to a file.
     * <p>
     * An existing file would be overwritten.
     * 
     * @param file
     * @param contents
     * 
     * @throws IOException
     */
    public static void write(File file, String contents) throws IOException {
        FileWriter writer = new FileWriter(file);
        writer.write(contents);
        writer.flush();
        writer.close();
    }

    /**
     * determine the url of a file relative to (in the same directory as) the specified .class file<br>
     * can also be used if the .class file resides inside a .jar file
     * 
     * @param clazz
     *            The class next to the file
     * @param fileName
     *            The name of the file
     * 
     * @return The url of the file, can be null
     */
    public static URL getRelativeURL(Class<?> clazz, String fileName) {
        String filePath = getRelativePackagePath(clazz) + "/" + fileName;
        return Thread.currentThread().getContextClassLoader().getResource(filePath);
    }

    /**
     * get the input stream of a file relative to (in the same directory as) the specified .class
     * file<br>
     * can also be used if the .class file resides inside a .jar file
     * 
     * @param clazz
     *            The class next to the file
     * @param fileName
     *            The name of the file
     * 
     * @return The input stream of the file, can be null
     */
    public static InputStream getRelativeURLAsStream(Class<?> clazz, String fileName) {
        String filePath = getRelativePackagePath(clazz) + "/" + fileName;
        return Thread.currentThread().getContextClassLoader().getResourceAsStream(filePath);
    }

    /**
     * do a chmod on the passed file
     * 
     * @param scriptFile
     */
    public static void makeExecutable(File scriptFile) {
        try {
            String command[] = new String[3];
            command[0] = "chmod";
            command[1] = EXECUTABLE_MODE;
            command[2] = scriptFile.getAbsolutePath();
            long timeout = 3000;
            ChildProcess childProcess = new ChildProcess(command, timeout);
            childProcess.run();
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    /**
     * build the package path for the class loader<br>
     * the class loader should be able to load a file appended to this path, if it is in the same
     * directory.
     * 
     * @param clazz
     *            The class
     * 
     * @return The package path
     */
    private static String getRelativePackagePath(Class<?> clazz) {
        String className = clazz.getName();
        String packageName = className.substring(0, className.lastIndexOf("."));
        return packageName.replace('.', '/');
    }

    /**
     * @param newResult
     * @param existingResult
     * @return <code>false</code> if newResult or existingResult are false, <code>true</code>
     *         otherwise.
     */
    private static boolean carryOnResult(boolean newResult, boolean existingResult) {
        if (existingResult) {
            return newResult;
        } else {
            return existingResult;
        }
    }
}
