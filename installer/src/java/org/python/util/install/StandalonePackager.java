package org.python.util.install;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

public class StandalonePackager {

    private final static int BUF_SIZE = 1024;

    private File _jarFile;
    private Manifest _manifest;
    private JarOutputStream _jarOut;

    /**
     * Helper class to pack stuff into a single .jar file.
     * <p>
     * If a .jar file has to be added, this should be the first add (because of the MANIFEST).
     */
    public StandalonePackager(File jarFile) {
        _jarFile = jarFile;
    }

    /**
     * add a file, in given parent dir (<code>null</code> = top)
     * 
     * @param file to write to jar
     * @param parentDir as String
     * 
     * @throws IOException
     */
    public void addFile(File file, String parentDir) throws IOException {
        byte[] buffer = new byte[BUF_SIZE];
        FileInputStream inputStream = null;
        try {
            inputStream = new FileInputStream(file);
            String jarEntryName = null;
            if (parentDir != null && parentDir.length() > 0) {
                jarEntryName = parentDir + "/" + file.getName();
            } else {
                jarEntryName = file.getName();
            }
            getJarOutputStream().putNextEntry(new JarEntry(jarEntryName));
            for (int read = 0; read != -1; read = inputStream.read(buffer)) {
                getJarOutputStream().write(buffer, 0, read);
            }
            getJarOutputStream().closeEntry();
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
        }
    }

    /**
     * add a full directory
     * 
     * @param directory
     * @throws IOException
     */
    public void addFullDirectory(File directory) throws IOException {
        addDirectory(directory, null);
    }

    /**
     * add the contents of a given jar file
     * 
     * @param jarFile to add
     * @throws IOException
     * @throws FileNotFoundException
     */
    public void addJarFile(File jarFile) throws FileNotFoundException, IOException {
        JarFile jarJarFile = new JarFile(jarFile);
        try {
            _manifest = jarJarFile.getManifest();
        } finally {
            jarJarFile.close();
        }

        JarInputStream inputStr = null;
        try {
            inputStr = new JarInputStream(new FileInputStream(jarFile));
            JarEntry entry = inputStr.getNextJarEntry();
            while (entry != null) {
                getJarOutputStream().putNextEntry(entry);
                byte[] buffer = new byte[BUF_SIZE];
                int len;
                while ((len = inputStr.read(buffer)) > 0) {
                    getJarOutputStream().write(buffer, 0, len);
                }
                getJarOutputStream().closeEntry();
                entry = inputStr.getNextJarEntry();
            }
        } finally {
            if (inputStr != null) {
                try {
                    inputStr.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void close() throws FileNotFoundException, IOException {
        getJarOutputStream().close();
    }

    /**
     * removes all files in directory dir (and subdirectories), except excludeFile.
     * 
     * @param dir
     * @param excludeFile
     */
    public static void emptyDirectory(File dir, File excludeFile) {
        File[] files = dir.listFiles();
        for (int i = 0; i < files.length; i++) {
            if (!files[i].equals(excludeFile)) {
                if (files[i].isDirectory()) {
                    emptyDirectory(files[i], excludeFile);
                } else {
                    files[i].delete();
                }
            }
        }
        if (dir.listFiles().length == 0) {
            dir.delete();
        }
    }

    /**
     * add the given directory to the jar, including all subdirectories, in given parent dir (<code>null</code> =
     * top)
     * 
     * @param dir to add to the jar
     * @param parentDir to save in jar
     * @throws IOException
     */
    private void addDirectory(File dir, String parentDir) throws IOException {
        if (!dir.isDirectory())
            return;
        File[] filesInDir = dir.listFiles();
        for (int i = 0; i < filesInDir.length; i++) {
            File currentFile = filesInDir[i];
            if (currentFile.isFile()) {
                if (parentDir != null && parentDir.length() > 0) {
                    addFile(currentFile, parentDir + "/" + dir.getName());
                } else {
                    addFile(currentFile, dir.getName());
                }
            } else {
                String newParentDir = null;
                if (parentDir != null && parentDir.length() > 0) {
                    newParentDir = parentDir + "/" + dir.getName();
                } else {
                    newParentDir = dir.getName();
                }
                addDirectory(currentFile, newParentDir);
            }
        }
    }

    /**
     * @return the jar output stream
     */
    private JarOutputStream getJarOutputStream() throws FileNotFoundException, IOException {
        if (_jarOut == null) {
            if (_manifest != null) {
                _jarOut = new JarOutputStream(new FileOutputStream(_jarFile), _manifest);
            } else {
                _jarOut = new JarOutputStream(new FileOutputStream(_jarFile));
            }
        }
        return _jarOut;
    }

}
