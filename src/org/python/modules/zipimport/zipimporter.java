/* Copyright (c) 2007 Jython Developers */
package org.python.modules.zipimport;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.python.core.ArgParser;
import org.python.core.Py;
import org.python.core.PyDictionary;
import org.python.core.PyInteger;
import org.python.core.PyLong;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.core.PySystemState;
import org.python.core.PyTuple;
import org.python.core.PyType;
import org.python.core.util.FileUtil;
import org.python.core.util.StringUtil;
import org.python.core.util.importer;
import org.python.expose.ExposedGet;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedType;

/**
 * Import Python modules and packages from ZIP-format archives.
 *
 * @author Philip Jenvey
 */
@ExposedType(name = "zipimport.zipimporter", base = PyObject.class)
public class zipimporter extends importer<PyObject> {

    public static final PyType TYPE = PyType.fromClass(zipimporter.class);

    @ExposedGet
    public static final PyString __doc__ = new PyString(
        "zipimporter(archivepath) -> zipimporter object\n" +
        "\n" +
        "Create a new zipimporter instance. 'archivepath' must be a path to\n" +
        "a zipfile. ZipImportError is raised if 'archivepath' doesn't point to\n" +
        "a valid Zip archive.");

    /** Pathname of the Zip archive */
    @ExposedGet
    public String archive;

    /** File prefix: "a/sub/directory/" */
    @ExposedGet
    public String prefix;

    /** Dict with file info {path: tocEntry} */
    @ExposedGet(name = "_files")
    public PyObject files;

    /** The PySystemState this zipimporter is associated with */
    private PySystemState sys;

    public zipimporter() {
        super();
    }

    public zipimporter(PyType subType) {
        super(subType);
    }

    public zipimporter(String path) {
        super();
        zipimporter___init__(path);
    }

    @ExposedNew
    @ExposedMethod
    final void zipimporter___init__(PyObject[] args, String[] kwds) {
        ArgParser ap = new ArgParser("__init__", args, kwds, new String[] {"path"});
        String path = ap.getString(0);
        zipimporter___init__(path);
    }

    private void zipimporter___init__(String path) {
        if (path == null || path.length() == 0) {
            throw zipimport.ZipImportError("archive path is empty");
        }

        File pathFile = new File(path);
        sys = Py.getSystemState();
        prefix = "";
        while (true) {
            File fullPathFile = new File(sys.getPath(pathFile.getPath()));
            if (fullPathFile.exists()) {
                if (fullPathFile.isFile()) {
                    archive = pathFile.getPath();
                }
                break;
            }

            // back up one path element
            File parentFile = pathFile.getParentFile();
            if (parentFile == null) {
                break;
            }

            prefix = pathFile.getName() + File.separator + prefix;
            pathFile = parentFile;
        }
        if (archive != null) {
            files = zipimport._zip_directory_cache.__finditem__(archive);
            if (files == null) {
                files = readDirectory(archive);
                zipimport._zip_directory_cache.__setitem__(archive, files);
            }
        } else {
            throw zipimport.ZipImportError("not a Zip file");
        }

        if (prefix != "" && !prefix.endsWith(File.separator)) {
            prefix += File.separator;
        }
    }

    public PyObject find_module(String fullname) {
        return zipimporter_find_module(fullname, null);
    }

    public PyObject find_module(String fullname, String path) {
        return zipimporter_find_module(fullname, path);
    }

    /**
     * Find the module for the fully qualified name.
     *
     * @param fullname the fully qualified name of the module
     * @param path if not installed on the meta-path None or a module path
     * @return a loader instance if this importer can load the module, None
     *         otherwise
     */
    @ExposedMethod(defaults = "null")
    final PyObject zipimporter_find_module(String fullname, String path) {
        return importer_find_module(fullname, path);
    }

    public PyObject load_module(String fullname) {
        return zipimporter_load_module(fullname);
    }

    /**
     * Load a module for the fully qualified name.
     *
     * @param fullname the fully qualified name of the module
     * @return a loaded PyModule
     */
    @ExposedMethod
    final PyObject zipimporter_load_module(String fullname) {
        return importer_load_module(fullname);
    }

    public String get_data(String path) {
        return zipimporter_get_data(path);
    }

    /**
     * Return the uncompressed data for the file at the specified path
     * as a String.
     *
     * @param path a String path name within the archive
     * @return a String of data in binary mode (no CRLF)
     */
    @ExposedMethod
    final String zipimporter_get_data(String path) {
        int len = archive.length();
        if (len < path.length() && path.startsWith(archive + File.separator)) {
            path = path.substring(len + 1);
        }

        PyObject tocEntry = files.__finditem__(path);
        if (tocEntry == null) {
            throw Py.IOError(path);
        }

        Bundle zipBundle = makeBundle(path, tocEntry);
        byte[] data;
        try {
            data = FileUtil.readBytes(zipBundle.inputStream);
        }
        catch (IOException ioe) {
            throw Py.IOError(ioe);
        }
        finally {
            zipBundle.close();
        }
        return StringUtil.fromBytes(data);
    }

    public boolean is_package(String fullname) {
        return zipimporter_is_package(fullname);
    }

    /**
     * Return a boolean signifying whether the module is a package or
     * not.
     *
     * @param fullname the fully qualified name of the module
     * @return a boolean describing if the module is a package
     */
    @ExposedMethod
    final boolean zipimporter_is_package(String fullname) {
        ModuleInfo moduleInfo = getModuleInfo(fullname);
        if (moduleInfo == ModuleInfo.NOT_FOUND) {
            throw zipimport.ZipImportError("can't find module '" + fullname + "'");
        }
        return moduleInfo == ModuleInfo.PACKAGE;
    }

    public PyObject get_code(String fullname) {
        return zipimporter_get_code(fullname);
    }

    /**
     * Return the code object associated with the module.
     *
     * @param fullname the fully qualified name of the module
     * @return the module's PyCode object or None
     */
    @ExposedMethod
    final PyObject zipimporter_get_code(String fullname) {
        ModuleCodeData moduleCodeData = getModuleCode(fullname);
        if (moduleCodeData != null) {
            return moduleCodeData.code;
        }
        return Py.None;
    }

    public String get_source(String fullname) {
        return zipimporter_get_source(fullname);
    }

    /**
     * Return the source code for the module as a string (using
     * newline characters for line endings)
     *
     * @param fullname the fully qualified name of the module
     * @return a String of the module's source code or null
     */
    @ExposedMethod
    final String zipimporter_get_source(String fullname) {
        ModuleInfo moduleInfo = getModuleInfo(fullname);

        if (moduleInfo == ModuleInfo.ERROR) {
            return null;
        }
        if (moduleInfo == ModuleInfo.NOT_FOUND) {
            throw zipimport.ZipImportError("can't find module '" + fullname + "'");
        }

        String path = makeFilename(fullname);
        if (moduleInfo == ModuleInfo.PACKAGE) {
            path += File.separator + "__init__.py";
        }
        else {
            path += ".py";
        }

        PyObject tocEntry = files.__finditem__(path);
        if (tocEntry != null) {
            return get_data(path);
        }

        // we have the module, but no source
        return null;
    }

    /**
     * Given a path to a compressed file in the archive, return the
     * file's (uncompressed) data stream in a ZipBundle.
     *
     * @param datapath file's filename inside of the archive
     * @return a ZipBundle with an InputStream to the file's
     * uncompressed data
     */
    public ZipBundle makeBundle(String datapath, PyObject entry) {
        datapath = datapath.replace(File.separatorChar, '/');
        ZipFile zipArchive;
        try {
            zipArchive = new ZipFile(new File(sys.getPath(archive)));
        }
        catch (IOException ioe) {
            throw zipimport.ZipImportError("zipimport: can not open file: " + archive);
        }

        ZipEntry dataEntry = zipArchive.getEntry(datapath);
        try {
            return new ZipBundle(zipArchive, zipArchive.getInputStream(dataEntry));
        }
        catch (IOException ioe) {
            Py.writeDebug("import", "zipimporter.getDataStream exception: " + ioe.toString());
            throw zipimport.ZipImportError("zipimport: can not open file: " + archive);
        }
    }

    /**
     * Determine if the byte code at path with the specified toc entry has a modification time
     * greater than its accompanying source code's.
     *
     * @param path
     *            a String path to the byte code
     * @param tocEntry
     *            the byte code's PyObject toc entry
     * @return boolean whether or not the byte code is older
     */
    protected boolean isAcceptableBytecode(String path, PyObject tocEntry) {
        String sourcePath = path.substring(0, path.length() - 9) + ".py";
        PyObject sourceTocEntry = files.__finditem__(sourcePath);
        if (sourceTocEntry == null) {
            return true;// If there is no source, assume the bytecode is ok
        }
        try {
            long bytecodeTime = dosTimeToEpoch(tocEntry.__finditem__(5).asInt(0),
                                             tocEntry.__finditem__(6).asInt(0));
            long sourceTime = dosTimeToEpoch(sourceTocEntry.__finditem__(5).asInt(0),
                                             sourceTocEntry.__finditem__(6).asInt(0));
            return bytecodeTime < sourceTime;
        }
        catch (PyObject.ConversionException ce) {
            return false;
        }
    }

    /**
     * readDirectory(archive) -> files dict (new reference)
     *
     * Given a path to a Zip archive, build a dict, mapping file names
     * (local to the archive, using SEP as a separator) to toc entries.
     *
     * A tocEntry is a tuple:
     *
     *     (__file__,      # value to use for __file__, available for all files
     *     compress,      # compression kind; 0 for uncompressed
     *     data_size,     # size of compressed data on disk
     *     file_size,     # size of decompressed data
     *     file_offset,   # offset of file header from start of archive (or -1 in Jython)
     *     time,          # mod time of file (in dos format)
     *     date,          # mod data of file (in dos format)
     *     crc,           # crc checksum of the data
     *     )
     *
     * Directories can be recognized by the trailing SEP in the name,
     * data_size and file_offset are 0.
     *
     * @param archive PyString path to the archive
     * @return a PyDictionary of tocEntrys
     */
    private PyObject readDirectory(String archive) {
        File file = new File(sys.getPath(archive));
        if (!file.canRead()) {
            throw zipimport.ZipImportError("can't open Zip file: '" + archive + "'");
        }

        ZipFile zipFile;
        try {
            zipFile = new ZipFile(file);
        }
        catch (IOException ioe) {
            throw zipimport.ZipImportError("can't read Zip file: '" + archive + "'");
        }

        PyObject files = new PyDictionary();
        for (Enumeration<? extends ZipEntry> zipEntries = zipFile.entries();
             zipEntries.hasMoreElements();) {
            ZipEntry zipEntry = zipEntries.nextElement();
            String name = zipEntry.getName().replace('/', File.separatorChar);

            PyObject __file__ = new PyString(archive + File.separator + name);
            PyObject compress = new PyInteger(zipEntry.getMethod());
            PyObject data_size = new PyLong(zipEntry.getCompressedSize());
            PyObject file_size = new PyLong(zipEntry.getSize());
            // file_offset is a CPython optimization; it's used to
            // seek directly to the file when reading it later. Jython
            // doesn't do this nor is the offset available
            PyObject file_offset = new PyInteger(-1);
            PyObject time = new PyInteger(epochToDosTime(zipEntry.getTime()));
            PyObject date = new PyInteger(epochToDosDate(zipEntry.getTime()));
            PyObject crc = new PyLong(zipEntry.getCrc());

            PyTuple entry = new PyTuple(__file__, compress, data_size, file_size, file_offset,
                                        time, date, crc);
            files.__setitem__(new PyString(name), entry);
        }

        try {
            zipFile.close();
        }
        catch (IOException ioe) {
            throw Py.IOError(ioe);
        }

        return files;
    }

    protected String getSeparator() {
        return File.separator;
    }

    /**
     * Given a full module name, return the potential file path in the archive (without extension).
     *
     * @param prefix
     *            a String value
     * @param name
     *            a String modulename value
     * @return the file path String value
     */
    protected String makeFilename(String fullname) {
        return prefix + getSubname(fullname).replace('.', File.separatorChar);
    }

    @Override
    protected String makePackagePath(String fullname) {
        return archive + File.separator + prefix + getSubname(fullname);
    }

    @Override
    protected String makeFilePath(String fullname) {
        return makePackagePath(fullname);
    }

    @Override
    protected PyObject makeEntry(String fullFilename) {
        return files.__finditem__(fullFilename);
    }

    /**
     * Return fullname.split(".")[-1].
     *
     * @param fullname
     *            a String value
     * @return a split(".")[-1] String value
     */
    protected String getSubname(String fullname) {
        int i = fullname.lastIndexOf(".");
        if (i >= 0) {
            return fullname.substring(i + 1);
        }
        return fullname;
    }

    /**
     * Convert a time in milliseconds since epoch to DOS date format
     *
     * @param time in milliseconds, a long value
     * @return an int, dos style date value
     */
    private int epochToDosDate(long time) {
        Date d = new Date(time);
        int year = d.getYear() + 1900;
        if (year < 1980) {
            return (1 << 21) | (1 << 16);
        }
        return (year - 1980) << 9 | (d.getMonth() + 1) << 5 | d.getDate() << 0;
    }

    /**
     * Convert a time in milliseconds since epoch to DOS time format
     *
     * @param time in milliseconds, a long value
     * @return an int, dos style time value
     */
    private int epochToDosTime(long time) {
        Date d = new Date(time);
        return d.getHours() << 11 | d.getMinutes() << 5 | d.getSeconds() >> 1;
    }

    /**
     * Convert the date/time values found in the Zip archive to a long
     * time (in milliseconds) value.
     *
     * @param dostime a dos style timestamp (only time) integer
     * @param dosdate a dos style date integer
     * @return a long time (in milliseconds) value
     */
    private long dosTimeToEpoch(int dosTime, int dosDate) {
        Date d = new Date(((dosDate >> 9) & 0x7f) + 80,
                          ((dosDate >> 5) & 0x0f) - 1,
                          dosDate & 0x1f,
                          (dosTime >> 11) & 0x1f,
                          (dosTime >> 5) & 0x3f,
                          (dosTime & 0x1f) * 2);
        return d.getTime();
    }

    public String toString() {
        return zipimporter_toString();
    }

    @ExposedMethod(names = "__repr__")
    final String zipimporter_toString() {
        String displayArchive = archive != null ? archive : "???";
        if (prefix != null && !"".equals(prefix)) {
            return String.format("<zipimporter object \"%.300s%c%.150s\">",
                                 displayArchive, File.separatorChar, prefix);
        }
        return String.format("<zipimporter object \"%.300s\">", displayArchive);
    }

    /**
     * ZipBundle is a ZipFile and one of its InputStreams, bundled together so the ZipFile can be
     * closed when finished with its InputStream.
     */
    private class ZipBundle extends Bundle {
        ZipFile zipFile;

        public ZipBundle(ZipFile zipFile, InputStream inputStream) {
            super(inputStream);
            this.zipFile = zipFile;
        }

        /**
         * Close the ZipFile; implicitly closes all of its InputStreams.
         *
         * Raises an IOError if a problem occurred.
         */
        public void close() {
            try {
                zipFile.close();
            }
            catch (IOException ioe) {
                throw Py.IOError(ioe);
            }
        }
    }
}
