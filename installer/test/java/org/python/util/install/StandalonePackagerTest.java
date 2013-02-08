package org.python.util.install;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import junit.framework.TestCase;

public class StandalonePackagerTest extends TestCase {
    private File _sourceJarFile;
    private File _targetJarFile;

    private File _contentFile;
    private File _nextContentFile;
    private File _additionalFile;

    private File _contentDir;
    private File _additionalDir;

    private File _jarDir;

    protected void setUp() throws Exception {
        _jarDir = File.createTempFile("jarDir", "");
        assertTrue(FileHelper.createTempDirectory(_jarDir));
        _targetJarFile = new File(_jarDir, "target.jar");
        _sourceJarFile = new File(_jarDir, "source.jar");

        _contentDir = File.createTempFile("content", "");
        assertTrue(FileHelper.createTempDirectory(_contentDir));
        _contentFile = new File(_contentDir, "content.file");
        _contentFile.createNewFile();
        assertTrue(_contentFile.exists());

        createSourceJar();
    }

    protected void tearDown() throws Exception {
        if (_sourceJarFile != null) {
            _sourceJarFile.delete();
        }
        if (_targetJarFile != null) {
            _targetJarFile.delete();
        }
        if (_contentFile != null) {
            _contentFile.delete();
        }
        if (_nextContentFile != null) {
            _nextContentFile.delete();
        }
        if (_additionalFile != null) {
            _additionalFile.delete();
        }
        if (_contentDir != null) {
            _contentDir.delete();
        }
        if (_additionalDir != null) {
            _additionalDir.delete();
        }
        if (_jarDir != null) {
            _jarDir.delete();
        }
    }

    /**
     * test the static method emptyDir()
     */
    public void testEmptyDir() throws Exception {
        File tempContentFile = new File(_contentDir, "temp");
        tempContentFile.createNewFile();
        assertTrue(tempContentFile.exists());
        File tempDir = new File(_contentDir, "tempDir");
        assertTrue(FileHelper.createTempDirectory(tempDir));

        StandalonePackager.emptyDirectory(_contentDir, _contentFile);
        assertTrue(_contentFile.exists());
        assertFalse(tempContentFile.exists());
        assertFalse(tempDir.exists());
        assertEquals(1, _contentDir.list().length);
    }

    /**
     * test adding a jar file, a directory, and another single file
     */
    public void testAdd_Jar_Directory_File() throws IOException {
        createAdditionalDirectory();
        _nextContentFile = File.createTempFile("nextContent.file", "");
        _nextContentFile.createNewFile();
        assertTrue(_nextContentFile.exists());

        StandalonePackager packager = new StandalonePackager(_targetJarFile);
        try {
            packager.addJarFile(_sourceJarFile);
            packager.addFullDirectory(_additionalDir);
            packager.addFile(_nextContentFile, null);
        } finally {
            packager.close();
        }

        assertTrue(_targetJarFile.exists());

        Map<String, String> mandatoryEntries = new HashMap<String, String>(8);
        mandatoryEntries.put(_contentDir.getName(), "dir");
        mandatoryEntries.put(_contentFile.getName(), "file");
        mandatoryEntries.put(_nextContentFile.getName(), "file");
        mandatoryEntries.put(_additionalDir.getName(), "dir");
        mandatoryEntries.put(_additionalFile.getName(), "file");
        mandatoryEntries.put("META-INF", "dir");
        mandatoryEntries.put("MANIFEST.MF", "file");

        JarFile targetJarFile = new JarFile(_targetJarFile);
        try {
            Enumeration<JarEntry> entries = targetJarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = (JarEntry) entries.nextElement();
                String name;
                String entryName = entry.getName();
                int slashIndex = entryName.indexOf("/");
                if (slashIndex >= 0) {
                    // handle directory
                    name = entryName.substring(slashIndex + 1);
                    String dirName = entryName.substring(0, slashIndex);
                    assertTrue(mandatoryEntries.containsKey(dirName));
                    assertEquals("dir", mandatoryEntries.get(dirName));
                    mandatoryEntries.remove(dirName);
                } else {
                    name = entryName;
                }
                if (mandatoryEntries.containsKey(name)) {
                    assertEquals("file", (String) mandatoryEntries.get(name));
                    assertFalse(entry.isDirectory());
                    mandatoryEntries.remove(name);
                }
            }
            assertTrue(mandatoryEntries.size() == 0);
            assertNotNull(targetJarFile.getManifest());
        } finally {
            targetJarFile.close();
        }
    }

    private void createSourceJar() throws FileNotFoundException, IOException {
        Manifest manifest = new Manifest();
        JarOutputStream jarOut = new JarOutputStream(new FileOutputStream(_sourceJarFile), manifest);
        addFile(_contentFile, _contentDir, jarOut);
        jarOut.close();
    }

    private void createAdditionalDirectory() throws IOException {
        _additionalDir = File.createTempFile("additional", "");
        assertTrue(FileHelper.createTempDirectory(_additionalDir));

        _additionalFile = new File(_additionalDir, "additional.file");
        _additionalFile.createNewFile();
        assertTrue(_additionalFile.exists());
    }

    private void addFile(File file, File parentDir, JarOutputStream jarOut) throws IOException {
        byte[] buffer = new byte[1024];
        FileInputStream inputStream = null;
        try {
            inputStream = new FileInputStream(file);
            String jarEntryName = parentDir.getName() + "/" + file.getName();
            jarOut.putNextEntry(new JarEntry(jarEntryName));
            for (int read = 0; read != -1; read = inputStream.read(buffer))
                jarOut.write(buffer, 0, read);
            jarOut.closeEntry();
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
        }
    }

}
