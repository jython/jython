package org.python.core;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@Untraversable
public class SyspathArchive extends PyString {
    private ZipFile zipFile;

    public SyspathArchive(String archiveName) throws IOException {
        // As a string-like object (on sys.path) an FS-encoded bytes object is expected
        super(Py.fileSystemEncode(archiveName).getString());
        archiveName = getArchiveName(archiveName);
        if(archiveName == null) {
            throw new IOException("path '" + archiveName + "' not an archive");
        }
        this.zipFile = new ZipFile(new File(archiveName));
        if(PySystemState.isPackageCacheEnabled()) {
            PySystemState.packageManager.addJar(archiveName, true);
        }
    }

    SyspathArchive(ZipFile zipFile, String path) {
        // As a string-like object (on sys.path) an FS-encoded bytes object is expected
        // Equivalent to Py.fileSystemEncode(path) with super instead of PyString
        super(TYPE,
                PyString.charsFitWidth(path, 7) ? path : codecs.PyUnicode_EncodeUTF8(path, null),
                true);
        this.zipFile = zipFile;
    }

    /**
     * Return a {@code SyspathArchive} wrapping the FS-encoded path into a ".zip" or ".jar" file
     * (like "x/y/a.jar/b/c.x") and {@code ZipFile} archive representing the file part ("x/y/a.jar")
     * of that path. This is guaranteed to be bytes-like (all chars &lt; 256). The method throws if
     * the argument is malformed, meaning it does not contain ".zip" or ".jar", (terminally or
     * followed by "/").
     *
     * @param path to represent
     * @return SyspathArchive for the path
     * @throws IOException if the argument is malformed
     */
    static SyspathArchive fromPath(String path) throws IOException {
        // Check the name is properly formed and reduce it to the file part.
        String archiveName = getArchiveName(path);
        if (archiveName == null) {
            throw new IOException(path);
        } else {
            ZipFile zipFile = new ZipFile(new File(archiveName));
            if (PySystemState.isPackageCacheEnabled()) {
                PySystemState.packageManager.addJar(archiveName, true);
            }
            return new SyspathArchive(zipFile, path);
        }
    }

    /**
     * Return the archive name extracted from a path like "x/y/a.jar/b/c.x" as a String, extracted
     * from the path by truncation. Here the archive name would be the "x/y/a.jar" part. The method
     * returns {@code null} if the argument is malformed, meaning it does not contain ".zip" or
     * ".jar", (terminally or followed by "/").
     *
     * @param path to extract from
     * @return archive name or {@code null} if the argument is does not contain ".zip" or ".jar"
     */
    static String getArchiveName(String dir) {
        String lowerName = dir.toLowerCase();
        int idx = lowerName.indexOf(".zip");
        if (idx < 0) {
            idx = lowerName.indexOf(".jar");
        }

        if (idx >= 0) {
            if (idx == dir.length() - 4) {
                // Exactly ends with .zip or .jar
                return dir;
            } else {
                // Continues, but must be /something
                char ch = dir.charAt(idx + 4);
                if (ch == File.separatorChar || ch == '/') {
                    // Return only up toe the .jar/.zip (inclusive)
                    return dir.substring(0, idx + 4);
                }
            }
        }
        // Malformed after all.
        return null;
    }

    public SyspathArchive makeSubfolder(String folder) {
        return new SyspathArchive(this.zipFile, super.toString() + "/" + folder);
    }

    private String makeEntry(String entry) {
        String archive = super.toString();
        String folder = getArchiveName(super.toString());
        if (archive.length() == folder.length()) {
            return entry;
        } else {
            return archive.substring(folder.length()+1) + "/" + entry;
        }
    }

    ZipEntry getEntry(String entryName) {
        return this.zipFile.getEntry(makeEntry(entryName));
    }

    public String asUriCompatibleString() {
    	String result = __str__().toString();
        if (File.separatorChar == '\\') {
            return result.replace(File.separatorChar, '/');
        }
        return result;
    }

    InputStream getInputStream(ZipEntry entry) throws IOException {
        InputStream istream = this.zipFile.getInputStream(entry);

        // Some jdk1.1 VMs have problems with detecting the end of a zip
        // stream correctly. If you read beyond the end, you get a
        // EOFException("Unexpected end of ZLIB input stream"), not a
        // -1 return value.
        // XXX: Since 1.1 is no longer supported, we should review the usefulness
        // of this workaround.
        // As a workaround we read the file fully here, but only getSize()
        // bytes.
        int len = (int) entry.getSize();
        byte[] buffer = new byte[len];
        int off = 0;
        while (len > 0) {
            int l = istream.read(buffer, off, buffer.length - off);
            if (l < 0) {
                return null;
            }
            off += l;
            len -= l;
        }
        istream.close();
        return new ByteArrayInputStream(buffer);
    }

/*
    private static Logger logger = Logger.getLogger("org.python.import");
    protected void finalize() {
        System.out.println("closing zip file " + toString());
        try {
            zipFile.close();
        } catch (IOException e) {
            logger.log(Level.FINE, "closing zipEntry failed");
        }
    }
*/
}
