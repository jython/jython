
package org.python.core;
import java.io.*;
import java.util.zip.*;

public class SyspathArchive extends PyString {
    private ZipFile zipFile;

    public SyspathArchive(String archiveName) throws IOException {
        super(archiveName);
        archiveName = getArchiveName(archiveName);
        zipFile = new ZipFile(new File(archiveName));
        Py.getSystemState().packageManager.addJar(archiveName);
    }

    SyspathArchive(ZipFile zipFile, String archiveName) {
        super(archiveName);
        this.zipFile = zipFile;
    }

    static String getArchiveName(String dir) {
        String lowerName = dir.toLowerCase();
        int idx = lowerName.indexOf(".zip");
        if (idx < 0) {
            idx = lowerName.indexOf(".jar");
        }
        if (idx < 0) {
            return null;
        }

        if (idx == dir.length() - 4) {
            return dir;
        }
        char ch = dir.charAt(idx+4);
        if (ch == File.separatorChar || ch == '/') {
            return dir.substring(0, idx+4);
        }
        return null;
    }

    public SyspathArchive makeSubfolder(String folder) {
        return new SyspathArchive(zipFile, super.toString() + "/" + folder);
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
        return zipFile.getEntry(makeEntry(entryName));
    }

    InputStream getInputStream(ZipEntry entry) throws IOException {
        InputStream istream = zipFile.getInputStream(entry);

        // Some jdk1.1 VMs have problems with detecting the end of a zip
        // stream correctly. If you read beyond the end, you get a
        // EOFException("Unexpected end of ZLIB input stream"), not a
        // -1 return value.
        // As a workaround we read the file fully here, but only getSize()
        // bytes.
        int len = (int) entry.getSize();
        byte[] buffer = new byte[len];
        int off = 0;
        while (len > 0) {
            int l = istream.read(buffer, off, buffer.length - off);
            if (l < 0)
                return null;
            off += l;
            len -= l;
        }
        istream.close();
        return new ByteArrayInputStream(buffer);
    }

/*
    protected void finalize() {
        System.out.println("closing zip file " + toString());
        try {
            zipFile.close();
        } catch (IOException e) {
            Py.writeDebug("import", "closing zipEntry failed");
        }
    }
*/
}
