/* Copyright (c) 2007 Jython Developers */
package org.python.modules.zipimport;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.python.core.imp;
import org.python.core.ArgParser;
import org.python.core.BytecodeLoader;
import org.python.core.FileUtil;
import org.python.core.Py;
import org.python.core.PyBuiltinFunction;
import org.python.core.PyBuiltinMethod;
import org.python.core.PyBuiltinMethodNarrow;
import org.python.core.PyCode;
import org.python.core.PyDictionary;
import org.python.core.PyGetSetDescr;
import org.python.core.PyInteger;
import org.python.core.PyList;
import org.python.core.PyLong;
import org.python.core.PyMethodDescr;
import org.python.core.PyModule;
import org.python.core.PyNewWrapper;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.core.PyTuple;
import org.python.core.PyType;

import java.util.Date;
import java.util.Enumeration;

import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Import Python modules and packages from ZIP-format archives.
 *
 * @author Philip Jenvey
 */
public class zipimporter extends PyObject {

    //~ BEGIN GENERATED REGION -- DO NOT EDIT SEE gexpose.py
    /* type info */

    public static final String exposed_name="zipimporter";

    public static void typeSetup(PyObject dict,PyType.Newstyle marker) {
        dict.__setitem__("archive",new PyGetSetDescr("archive",zipimporter.class,"getArchive",null,null));
        dict.__setitem__("prefix",new PyGetSetDescr("prefix",zipimporter.class,"getPrefix",null,null));
        dict.__setitem__("_files",new PyGetSetDescr("_files",zipimporter.class,"getFiles",null,null));
        dict.__setitem__("__doc__",new PyGetSetDescr("__doc__",zipimporter.class,"getDoc",null,null));
        class exposed_find_module extends PyBuiltinMethodNarrow {

            exposed_find_module(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_find_module(self,info);
            }

            public PyObject __call__(PyObject arg0,PyObject arg1) {
                try {
                    return((zipimporter)self).zipimporter_find_module(arg0.asString(0),arg1.asString(1));
                } catch (PyObject.ConversionException e) {
                    String msg;
                    switch (e.index) {
                    case 0:
                    case 1:
                        msg="expected a string";
                        break;
                    default:
                        msg="xxx";
                    }
                    throw Py.TypeError(msg);
                }
            }

            public PyObject __call__(PyObject arg0) {
                try {
                    return((zipimporter)self).zipimporter_find_module(arg0.asString(0));
                } catch (PyObject.ConversionException e) {
                    String msg;
                    switch (e.index) {
                    case 0:
                        msg="expected a string";
                        break;
                    default:
                        msg="xxx";
                    }
                    throw Py.TypeError(msg);
                }
            }

        }
        dict.__setitem__("find_module",new PyMethodDescr("find_module",zipimporter.class,1,2,new exposed_find_module(null,null)));
        class exposed_load_module extends PyBuiltinMethodNarrow {

            exposed_load_module(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_load_module(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                try {
                    return((zipimporter)self).zipimporter_load_module(arg0.asString(0));
                } catch (PyObject.ConversionException e) {
                    String msg;
                    switch (e.index) {
                    case 0:
                        msg="expected a string";
                        break;
                    default:
                        msg="xxx";
                    }
                    throw Py.TypeError(msg);
                }
            }

        }
        dict.__setitem__("load_module",new PyMethodDescr("load_module",zipimporter.class,1,1,new exposed_load_module(null,null)));
        class exposed_get_data extends PyBuiltinMethodNarrow {

            exposed_get_data(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_get_data(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                try {
                    return new PyString(((zipimporter)self).zipimporter_get_data(arg0.asString(0)));
                } catch (PyObject.ConversionException e) {
                    String msg;
                    switch (e.index) {
                    case 0:
                        msg="expected a string";
                        break;
                    default:
                        msg="xxx";
                    }
                    throw Py.TypeError(msg);
                }
            }

        }
        dict.__setitem__("get_data",new PyMethodDescr("get_data",zipimporter.class,1,1,new exposed_get_data(null,null)));
        class exposed_is_package extends PyBuiltinMethodNarrow {

            exposed_is_package(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_is_package(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                try {
                    return Py.newBoolean(((zipimporter)self).zipimporter_is_package(arg0.asString(0)));
                } catch (PyObject.ConversionException e) {
                    String msg;
                    switch (e.index) {
                    case 0:
                        msg="expected a string";
                        break;
                    default:
                        msg="xxx";
                    }
                    throw Py.TypeError(msg);
                }
            }

        }
        dict.__setitem__("is_package",new PyMethodDescr("is_package",zipimporter.class,1,1,new exposed_is_package(null,null)));
        class exposed_get_code extends PyBuiltinMethodNarrow {

            exposed_get_code(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_get_code(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                try {
                    return((zipimporter)self).zipimporter_get_code(arg0.asString(0));
                } catch (PyObject.ConversionException e) {
                    String msg;
                    switch (e.index) {
                    case 0:
                        msg="expected a string";
                        break;
                    default:
                        msg="xxx";
                    }
                    throw Py.TypeError(msg);
                }
            }

        }
        dict.__setitem__("get_code",new PyMethodDescr("get_code",zipimporter.class,1,1,new exposed_get_code(null,null)));
        class exposed_get_source extends PyBuiltinMethodNarrow {

            exposed_get_source(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_get_source(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                try {
                    return new PyString(((zipimporter)self).zipimporter_get_source(arg0.asString(0)));
                } catch (PyObject.ConversionException e) {
                    String msg;
                    switch (e.index) {
                    case 0:
                        msg="expected a string";
                        break;
                    default:
                        msg="xxx";
                    }
                    throw Py.TypeError(msg);
                }
            }

        }
        dict.__setitem__("get_source",new PyMethodDescr("get_source",zipimporter.class,1,1,new exposed_get_source(null,null)));
        class exposed___repr__ extends PyBuiltinMethodNarrow {

            exposed___repr__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___repr__(self,info);
            }

            public PyObject __call__() {
                return new PyString(((zipimporter)self).zipimporter_toString());
            }

        }
        dict.__setitem__("__repr__",new PyMethodDescr("__repr__",zipimporter.class,0,0,new exposed___repr__(null,null)));
        class exposed___init__ extends PyBuiltinMethod {

            exposed___init__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___init__(self,info);
            }

            public PyObject __call__(PyObject[]args) {
                return __call__(args,Py.NoKeywords);
            }

            public PyObject __call__(PyObject[]args,String[]keywords) {
                ((zipimporter)self).zipimporter_init(args,keywords);
                return Py.None;
            }

        }
        dict.__setitem__("__init__",new PyMethodDescr("__init__",zipimporter.class,-1,-1,new exposed___init__(null,null)));
        dict.__setitem__("__new__",new PyNewWrapper(zipimporter.class,"__new__",-1,-1) {

                                                                                           public PyObject new_impl(boolean init,PyType subtype,PyObject[]args,String[]keywords) {
                                                                                               zipimporter newobj;
                                                                                               if (for_type==subtype) {
                                                                                                   newobj=new zipimporter();
                                                                                                   if (init)
                                                                                                       newobj.zipimporter_init(args,keywords);
                                                                                               } else {
                                                                                                   newobj=new zipimporterDerived(subtype);
                                                                                               }
                                                                                               return newobj;
                                                                                           }

                                                                                       });
    }
    //~ END GENERATED REGION -- DO NOT EDIT SEE gexpose.py

    private static final PyString __doc__ = new PyString(
        "zipimporter(archivepath) -> zipimporter object\n" +
        "\n" +
        "Create a new zipimporter instance. 'archivepath' must be a path to\n" +
        "a zipfile. ZipImportError is raised if 'archivepath' doesn't point to\n" +
        "a valid Zip archive.");

    /** zip_searchorder defines how we search for a module in the Zip
     * archive */
    static final int IS_SOURCE = 0;
    static final int IS_BYTECODE = 1;
    static final int IS_PACKAGE = 2;
    static final SearchOrderEntry[] zip_searchorder = new SearchOrderEntry[] {
        new SearchOrderEntry("/__init__$py.class", IS_PACKAGE | IS_BYTECODE),
        new SearchOrderEntry("/__init__.py", IS_PACKAGE | IS_SOURCE),
        new SearchOrderEntry("$py.class", IS_BYTECODE),
        new SearchOrderEntry(".py", IS_SOURCE),
        new SearchOrderEntry("", 0)
    };

    /** Module information */
    static enum ModuleInfo {ERROR, NOT_FOUND, MODULE, PACKAGE};

    /** Pathname of the Zip archive */
    private String archive;

    /** File prefix: "a/sub/directory/" */
    private String prefix;

    /** Dict with file info {path: tocEntry} */
    private PyObject files;

    public zipimporter() {
        super();
    }

    public zipimporter(PyType subType) {
        super(subType);
    }

    public zipimporter(String path) {
        super();
        zipimporter_init(path);
    }

    final void zipimporter_init(PyObject[] args, String[] kwds) {
        ArgParser ap = new ArgParser("__init__", args, kwds, new String[] {"path"});
        String path = ap.getString(0);
        zipimporter_init(path);
    }

    private void zipimporter_init(String path) {
        if (path == null || path.length() == 0) {
            throw zipimport.ZipImportError("archive path is empty");
        }

        File pathFile = new File(path);
        prefix = "";
        while (true) {
            if (pathFile.isFile()) {
                archive = pathFile.getPath();
                break;
            }

            // back up one path element
            File parentFile = pathFile.getParentFile();
            if (parentFile == null) {
                break;
            }

            String childFile = pathFile.getPath();
            prefix = childFile.substring(childFile.lastIndexOf(File.separator) + 1) +
                    File.separator + prefix;
            pathFile = parentFile;
        }

        if (archive != null) {
            files = zipimport._zip_directory_cache.__finditem__(archive);
            if (files == null) {
                files = readDirectory(archive);
                zipimport._zip_directory_cache.__setitem__(archive, files);
            }
        }
        else {
            throw zipimport.ZipImportError("not a Zip file");
        }

        if (prefix != "" && !prefix.endsWith(File.separator)) {
            prefix += File.separator;
        }
    }

    public PyObject find_module(String fullname) {
        return zipimporter_find_module(fullname);
    }

    /**
     * Find the module for the fully qualified name.
     *
     * @param fullname the fully qualified name of the module
     * @return a loader instance if this importer can load the module, None
     *         otherwise
     */
    final PyObject zipimporter_find_module(String fullname) {
        return find_module(fullname, null);
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
    final PyObject zipimporter_find_module(String fullname, String path) {
        ModuleInfo moduleInfo = getModuleInfo(fullname);
        if (moduleInfo == ModuleInfo.ERROR || moduleInfo == ModuleInfo.NOT_FOUND) {
            return Py.None;
        }
        return this;
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
    final PyObject zipimporter_load_module(String fullname) {
        ModuleCodeData moduleCodeData = getModuleCode(fullname);
        if (moduleCodeData == null) {
            return Py.None;
        }

        // the module *must* be in sys.modules before the loader
        // executes the module code; the module code may (directly or
        // indirectly) import itself
        PyModule mod = imp.addModule(fullname);

        mod.__dict__.__setitem__("__loader__", this);
        if (moduleCodeData.ispackage) {
            // add __path__ to the module *before* the code gets
            // executed
            String fullpath = archive + File.separator + prefix + getSubname(fullname);
            PyList pkgpath = new PyList();
            pkgpath.add(fullpath);
            mod.__dict__.__setitem__("__path__", pkgpath);
        }

        imp.createFromCode(fullname, moduleCodeData.code, moduleCodeData.path);
        Py.writeDebug("import", "import " + fullname + " # loaded from Zip " +
                      moduleCodeData.path);
        return mod;
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
    final String zipimporter_get_data(String path) {
        int len = archive.length();
        if (len < path.length() && path.startsWith(archive + File.separator)) {
            path = path.substring(len + 1);
        }

        PyObject tocEntry = files.__finditem__(path);
        if (tocEntry == null) {
            throw Py.IOError(path);
        }

        InputStream dataStream = getDataStream(path);
        byte[] data;
        try {
            data = FileUtil.readBytes(dataStream);
        }
        catch (IOException ioe) {
            throw Py.IOError(ioe);
        }
        finally {
            try {
                dataStream.close();
            }
            catch (IOException ioe) {
                // continue
            }
        }
        try {
            return new String(data, "iso-8859-1");
        }
        catch (UnsupportedEncodingException uee) {
            // should never happen
            throw new RuntimeException(uee);
        }
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
    final String zipimporter_get_source(String fullname) {
        ModuleInfo moduleInfo = getModuleInfo(fullname);

        if (moduleInfo == ModuleInfo.ERROR) {
            return null;
        }
        if (moduleInfo == ModuleInfo.NOT_FOUND) {
            throw zipimport.ZipImportError("can't find module '" + fullname + "'");
        }

        String path = makeFilename(prefix, getSubname(fullname));
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
     * file's (uncompressed) data stream.
     *
     * @param datapath file's filename inside of the archive
     * @return an InputStream yielding the file's uncompressed data
     */
    public InputStream getDataStream(String datapath) {
        ZipFile zipArchive;
        try {
            zipArchive = new ZipFile(new File(archive));
        }
        catch (IOException ioe) {
            throw zipimport.ZipImportError("zipimport: can not open file: " + archive);
        }

        ZipEntry data = zipArchive.getEntry(datapath);
        try {
            return zipArchive.getInputStream(data);
        }
        catch (IOException ioe) {
            Py.writeDebug("import", "zipimporter.getDataStream exception: " +
                          ioe.toString());
            throw zipimport.ZipImportError("zipimport: can not open file: " + archive);

        }
    }

    /**
     * Return module information for the module with the fully
     * qualified name.
     *
     * @param fullname the fully qualified name of the module
     * @return the module's information
     */
    private ModuleInfo getModuleInfo(String fullname) {
        String path = makeFilename(prefix, getSubname(fullname));

        for (int i = 0; i < zip_searchorder.length; i++) {
            SearchOrderEntry entry = zip_searchorder[i];
            PyObject tocEntry = files.__finditem__(path + entry.suffix);
            if (tocEntry == null)
                continue;

            if ((entry.type & IS_PACKAGE) == IS_PACKAGE) {
                return ModuleInfo.PACKAGE;
            }
            return ModuleInfo.MODULE;
        }
        return ModuleInfo.NOT_FOUND;
    }

    /**
     * Return the code object and its associated data for the module
     * with the fully qualified name.
     *
     * @param fullname the fully qualified name of the module
     * @return the module's ModuleCodeData object
     */
    private ModuleCodeData getModuleCode(String fullname) {
        String path = makeFilename(prefix, getSubname(fullname));

        if (path.length() < 0) {
            return null;
        }

        for (int i = 0; i < zip_searchorder.length; i++) {
            SearchOrderEntry entry = zip_searchorder[i];
            String suffix = entry.suffix;
            String searchPath = path + suffix;

            Py.writeDebug("import", "# trying " + archive + File.separator + path);
            PyObject tocEntry = files.__finditem__(searchPath);
            if (tocEntry == null) {
                continue;
            }

            boolean ispackage = (entry.type & IS_PACKAGE) == IS_PACKAGE;
            boolean isbytecode = (entry.type & IS_BYTECODE) == IS_BYTECODE;

            if (isbytecode && isOutdatedBytecode(searchPath, tocEntry)) {
                continue;
            }

            String pathToEntry = archive + File.separator + searchPath;
            InputStream dataStream = getDataStream(searchPath);
            byte[] codeBytes;
            if (isbytecode) {
                codeBytes = imp.unmarshalCode(fullname, dataStream, true);
            }
            else {
                codeBytes = imp.compileSource(fullname, dataStream, pathToEntry);
            }
            imp.cacheCompiledSource(pathToEntry, null, codeBytes);

            PyCode code;
            code = BytecodeLoader.makeCode(fullname + "$py", codeBytes, pathToEntry);

            if (code == null) {
                continue;
            }
            return new ModuleCodeData(code, ispackage, pathToEntry);
        }
        return null;
    }

    /**
     * Determine if the byte code at path with the specified toc entry
     * has a modification time greater than its accompanying source
     * code's.
     *
     * @param path a String path to the byte code
     * @param tocEntry the byte code's PyObject toc entry
     * @return boolean whether or not the byte code is older
     */
    private boolean isOutdatedBytecode(String path, PyObject tocEntry) {
        String sourcePath = path.substring(0, path.length() - 9) + ".py";
        PyObject sourceTocEntry = files.__finditem__(sourcePath);
        if (sourceTocEntry == null) {
            return false;
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
        File file = new File(archive);
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
        for (Enumeration zipEntries = zipFile.entries(); zipEntries.hasMoreElements();) {
            ZipEntry zipEntry = (ZipEntry)zipEntries.nextElement();

            PyObject __file__ = new PyString(archive + File.separator +
                                             zipEntry.getName());
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

            PyTuple entry = new PyTuple(new PyObject[] {
                    __file__, compress, data_size, file_size, file_offset, time, date,
                    crc});
            files.__setitem__(new PyString(zipEntry.getName()), entry);
        }

        return files;
    }

    /**
     * Return fullname.split(".")[-1].
     *
     * @param fullname a String value
     * @return a split(".")[-1] String value
     */
    private String getSubname(String fullname) {
        int i = fullname.lastIndexOf(".");
        if (i >= 0) {
            return fullname.substring(i + 1);
        }
        return fullname;
    }

    /**
     * Given a (sub)modulename, return the potential file path in the
     * archive (without extension).
     *
     * @param prefix a String value
     * @param name a String modulename value
     * @return the file path String value
     */
    private String makeFilename(String prefix, String name) {
        return prefix + name.replace('.', File.separatorChar);
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

    final String zipimporter_toString() {
        return "<zipimporter object \"" + archive + "\">";
    }

    public String getArchive() { return archive; }
    public String getPrefix() { return prefix; }
    public PyObject getFiles() { return files; }
    public PyObject getDoc() { return __doc__; }

    /**
     * Container for PyModule code, whether or not it's a package and
     * its path.
     *
     */
    private class ModuleCodeData {
        PyCode code;
        boolean ispackage;
        String path;

        public ModuleCodeData(PyCode code, boolean ispackage, String path) {
            this.code = code;
            this.ispackage = ispackage;
            this.path = path;
        }
    }

    /**
     * A step in the module search order: the file suffix and its file
     * type.
     *
     */
    protected static class SearchOrderEntry {
        public String suffix;
        public int type;

        public SearchOrderEntry(String suffix, int type) {
            this.suffix = suffix;
            this.type = type;
        }
    }
}
