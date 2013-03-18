package org.python.util.install;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.StringTokenizer;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

public class JarInfo {
    private static final String JAR_URL_PREFIX = "jar:file:";
    private static final String JAR_SEPARATOR = "!";
    private static final String JYTHON = "Jython";
    private static final String VERSION_ATTRIBUTE = "version";
    private static final String EXCLUDE_DIRS_ATTRIBUTE = "exclude-dirs";
    private static final String EXCLUDE_DIRS_DELIM = ";";

    private File _jarFile;
    private int _numberOfEntries;
    private Manifest _manifest;
    private String _licenseText;
    private String _readmeText;

    public JarInfo() {
        _jarFile = null;
        _numberOfEntries = 0;
        _manifest = null;

        try {
            readJarInfo();
        } catch (IOException ioe) {
            throw new InstallerException(Installation.getText(TextKeys.ERROR_ACCESS_JARFILE), ioe);
        }
    }

    public String getVersion() {
        String version = "<unknown>";
        try {
            Attributes jythonAttributes = getManifest().getAttributes(JYTHON);
            if (jythonAttributes != null) {
                version = jythonAttributes.getValue(VERSION_ATTRIBUTE); // do
                // not
                // use
                // containsKey
            }
        } catch (IOException ioe) {
        }
        return version;
    }

    public File getJarFile() throws IOException {
        if (_jarFile == null)
            readJarInfo();
        return _jarFile;
    }

    public Manifest getManifest() throws IOException {
        if (_manifest == null)
            readJarInfo();
        return _manifest;
    }

    public int getNumberOfEntries() throws IOException {
        if (_numberOfEntries == 0)
            readJarInfo();
        return _numberOfEntries;
    }

    public List<String> getExcludeDirs() throws IOException {
        List<String> excludeDirs = new ArrayList<String>();
        Attributes jythonAttributes = getManifest().getAttributes(JYTHON);
        if (jythonAttributes != null) {
            // do not use containsKey
            String excludeDirsString = jythonAttributes.getValue(EXCLUDE_DIRS_ATTRIBUTE);
            if (excludeDirsString != null && excludeDirsString.length() > 0) {
                StringTokenizer tokenizer = new StringTokenizer(excludeDirsString, EXCLUDE_DIRS_DELIM);
                while (tokenizer.hasMoreTokens()) {
                    excludeDirs.add(tokenizer.nextToken());
                }
            }
        }
        return excludeDirs;
    }

    public String getLicenseText() throws IOException {
        if (_licenseText == null) {
            readJarInfo();
        }
        return _licenseText;
    }

    public String getReadmeText() throws IOException {
        if (_readmeText == null) {
            readJarInfo();
        }
        return _readmeText;
    }

    private void readJarInfo() throws IOException {
        String fullClassName = getClass().getName();
        String className = fullClassName.substring(fullClassName.lastIndexOf(".") + 1);
        URL url = getClass().getResource(className + ".class");
        // we expect an URL like:
        // jar:file:/C:/stuff/jython21i.jar!/org/python/util/install/JarInfo.class
        // escape plus signs, since the URLDecoder would turn them into spaces
        final String plus = "\\+";
        final String escapedPlus = "__ppluss__";
        String rawUrl = url.toString();
        rawUrl = rawUrl.replaceAll(plus, escapedPlus);
        String urlString = URLDecoder.decode(rawUrl, "UTF-8");
        urlString = urlString.replaceAll(escapedPlus, plus);
        int jarSeparatorIndex = urlString.lastIndexOf(JAR_SEPARATOR);
        if (!urlString.startsWith(JAR_URL_PREFIX) || jarSeparatorIndex <= 0) {
            throw new InstallerException(Installation.getText(TextKeys.UNEXPECTED_URL, urlString));
        }
        String jarFileName = urlString.substring(JAR_URL_PREFIX.length(), jarSeparatorIndex);
        _jarFile = new File(jarFileName);
        if (!_jarFile.exists()) {
            throw new InstallerException(Installation.getText(TextKeys.JAR_NOT_FOUND, _jarFile.getAbsolutePath()));
        }
        JarFile jarFile = new JarFile(_jarFile);
        Enumeration<JarEntry> entries = jarFile.entries();
        _numberOfEntries = 0;
        while (entries.hasMoreElements()) {
            JarEntry entry = (JarEntry) entries.nextElement();
            if ("LICENSE.txt".equals(entry.getName())) {
                _licenseText = readTextFile(entry, jarFile);
            }
            if ("README.txt".equals(entry.getName())) {
                _readmeText = readTextFile(entry, jarFile);
            }
            _numberOfEntries++;
        }
        _manifest = jarFile.getManifest();
        if (_manifest == null) {
            throw new InstallerException(Installation.getText(TextKeys.NO_MANIFEST, _jarFile.getAbsolutePath()));
        }
        jarFile.close();
    }

    /**
     * Read the text file with the most appropriate Charset.
     * 
     * @param entry
     * @param jarFile
     * 
     * @return the contents of the text file
     * 
     * @throws IOException
     */
    private String readTextFile(JarEntry entry, JarFile jarFile) throws IOException {
        String contents = readTextFileWithCharset(jarFile, entry, "US-ASCII"); // expected to run on most platforms
        if (contents == null) {
            contents = readTextFileWithCharset(jarFile, entry, "ISO-8859-1");
        }
        if (contents == null) {
            contents = readTextFileWithDefaultCharset(jarFile, entry);
        }
        return contents;
    }

    /**
     * Try to read the text file (jarEntry) from the jarFile, using a given <code>charsetName</code>.
     * 
     * @param jarFile
     * @param entry
     * @param charsetName the name of the Charset
     * 
     * @return the contents of the text file as String (if reading was successful), <code>null</code> otherwise.<br>
     * No exception is thrown
     */
    private String readTextFileWithCharset(JarFile jarFile, JarEntry entry, String charsetName) {
        String contents = null;
        if (Charset.isSupported(charsetName)) {
            BufferedReader reader = null;
            try {
                StringBuffer buffer = new StringBuffer(1000);
                reader = new BufferedReader(new InputStreamReader(jarFile.getInputStream(entry), Charset
                        .forName(charsetName)));
                buffer = new StringBuffer(1000);
                for (String s; (s = reader.readLine()) != null;) {
                    buffer.append(s);
                    buffer.append("\n");
                }
                contents = buffer.toString();
            } catch (IOException ioe) {
            } finally {
                if (reader != null)
                    try {
                        reader.close();
                    } catch (IOException e) {
                    }
            }
        }
        return contents;
    }

    /**
     * Read the text file (jarEntry) from the jarFile, using the platform default Charset.
     * 
     * @param jarFile
     * @param entry
     * 
     * @return the contents of the text file as String.
     * 
     * @throws IOException if a problem occurs
     */
    private String readTextFileWithDefaultCharset(JarFile jarFile, JarEntry entry) throws IOException {
        String contents = null;
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(jarFile.getInputStream(entry)));
            StringBuffer buffer = new StringBuffer(1000);
            for (String s; (s = reader.readLine()) != null;) {
                buffer.append(s);
                buffer.append("\n");
            }
            contents = buffer.toString();
        } finally {
            if (reader != null)
                reader.close();
        }
        return contents;
    }

}