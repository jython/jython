/* Copyright (c) Jython Developers */
package org.python.core.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.EnumSet;
import java.util.logging.Logger;
import java.util.logging.Level;

import org.python.core.BytecodeLoader;
import org.python.core.Py;
import org.python.core.PyCode;
import org.python.core.PyList;
import org.python.core.PyModule;
import org.python.core.PyObject;
import org.python.core.PyType;
import org.python.core.imp;

/**
 * A base class for PEP-302 path hooks. Handles looking through source, compiled, package and module
 * items in the right order, and creating and filling in modules.
 */
public abstract class importer<T> extends PyObject {

    protected static Logger logger = Logger.getLogger("org.python.import");

    static enum EntryType {
        IS_SOURCE, IS_BYTECODE, IS_PACKAGE
    };
    /** SearchOrder defines how we search for a module. */
    final SearchOrderEntry[] searchOrder;

    /** Module information */
    protected static enum ModuleInfo {
        ERROR, NOT_FOUND, MODULE, PACKAGE
    };

    public importer(PyType subType) {
        super(subType);
        searchOrder = makeSearchOrder();
    }

    public importer() {
        searchOrder = makeSearchOrder();
    }

    /**
     * Return the bytes for the data located at <code>path</code>.
     */
    public abstract String get_data(String path);

    /**
     * Returns the separator between directories and files used by this type of importer.
     */
    protected abstract String getSeparator();

    /**
     * Returns the value to fill in __path__ on a module with the given full module name created by
     * this importer.
     */
    protected abstract String makePackagePath(String fullname);

    /**
     * Given a full module name, return the potential file path in the archive (without extension).
     */
    protected abstract String makeFilename(String fullname);

    /**
     * Given a full module name, return the potential file path including the archive (without
     * extension).
     */
    protected abstract String makeFilePath(String fullname);

    /**
     * Returns an entry for a filename from makeFilename with a potential suffix such that this
     * importer can make a bundle with it, or null if fullFilename doesn't exist in this importer.
     */
    protected abstract T makeEntry(String filenameAndSuffix);

    /**
     * Returns a Bundle for fullFilename and entry, the result from a makeEntry call for
     * fullFilename.
     */
    protected abstract Bundle makeBundle(String filenameAndSuffix, T entry);

    private SearchOrderEntry[] makeSearchOrder(){
        String initName = "__init__.py";
        return new SearchOrderEntry[] {
            new SearchOrderEntry(getSeparator() + imp.makeCompiledFilename(initName),
                                 EnumSet.of(EntryType.IS_PACKAGE, EntryType.IS_BYTECODE)),
            new SearchOrderEntry(getSeparator() + initName,
                                 EnumSet.of(EntryType.IS_PACKAGE, EntryType.IS_SOURCE)),
            new SearchOrderEntry("$py.class", EnumSet.of(EntryType.IS_BYTECODE)),
            new SearchOrderEntry(".py", EnumSet.of(EntryType.IS_SOURCE)),};
    }

    protected final PyObject importer_find_module(String fullname, String path) {
        ModuleInfo moduleInfo = getModuleInfo(fullname);
        if (moduleInfo == ModuleInfo.ERROR || moduleInfo == ModuleInfo.NOT_FOUND) {
            return Py.None;
        }
        return this;
    }

    protected final PyObject importer_load_module(String fullname) {
        ModuleCodeData moduleCodeData = getModuleCode(fullname);
        if (moduleCodeData == null) {
            return Py.None;
        }
        // the module *must* be in sys.modules before the loader executes the module code; the
        // module code may (directly or indirectly) import itself
        PyModule mod = imp.addModule(fullname);
        mod.__dict__.__setitem__("__loader__", this);
        if (moduleCodeData.isPackage) {
            // add __path__ to the module *before* the code gets executed
            PyList pkgpath = new PyList();
            pkgpath.add(makePackagePath(fullname));
            mod.__dict__.__setitem__("__path__", pkgpath);
        }
        imp.createFromCode(fullname, moduleCodeData.code, moduleCodeData.path);
        logger.log(Level.FINE, "import {0} # loaded from {1}",
                new Object[] {fullname, moduleCodeData.path});
        return mod;
    }

    /**
     * @param fullname
     *            the fully qualified name of the module
     * @return whether the module is a package
     */
    protected final boolean importer_is_package(String fullname) {
        ModuleInfo info = getModuleInfo(fullname);
        return info == ModuleInfo.PACKAGE;
    }

    /**
     * Bundle is an InputStream, bundled together with a method that can close the input stream and
     * whatever resources are associated with it when the resource is imported.
     */
    protected abstract static class Bundle implements AutoCloseable {
        public InputStream inputStream;

        public Bundle(InputStream inputStream) {
            this.inputStream = inputStream;
        }

        /**
         * Close the underlying resource if necessary. Raises an IOError if a problem occurs.
         */
        @Override
        public abstract void close();
    }

    /**
     * Given a path to a compiled file in the archive, return the modification time of the
     * matching .py file.
     *
     * @param path to the compiled file
     * @return long mtime of the .py, or -1 if no source is available
     */
    protected abstract long getSourceMtime(String path);

    /**
     * Return module information for the module with the fully qualified name.
     *
     * @param fullname
     *            the fully qualified name of the module
     * @return the module's information
     */
    protected final ModuleInfo getModuleInfo(String fullname) {
        String path = makeFilename(fullname);

        for (SearchOrderEntry entry : searchOrder) {
            T importEntry = makeEntry(path + entry.suffix);
            if (importEntry == null) {
                continue;
            }

            if (entry.type.contains(EntryType.IS_PACKAGE)) {
                return ModuleInfo.PACKAGE;
            }
            return ModuleInfo.MODULE;
        }
        return ModuleInfo.NOT_FOUND;
    }

    /**
     * Return the code object and its associated data for the module with the fully qualified name.
     *
     * @param fullname
     *            the fully qualified name of the module
     * @return the module's ModuleCodeData object
     */
    protected final ModuleCodeData getModuleCode(String fullname) {
        String path = makeFilename(fullname);
        String fullPath = makeFilePath(fullname);

        if (path.length() == 0) {
            return null;
        }

        for (SearchOrderEntry entry : searchOrder) {
            String suffix = entry.suffix;
            String searchPath = path + suffix;
            String fullSearchPath = fullPath + suffix;

            logger.log(Level.FINE, "# trying {0}", searchPath);
            T tocEntry = makeEntry(searchPath);
            if (tocEntry == null) {
                continue;
            }

            boolean isPackage = entry.type.contains(EntryType.IS_PACKAGE);
            boolean isBytecode = entry.type.contains(EntryType.IS_BYTECODE);
            long mtime = -1;
            if (isBytecode) {
                mtime = getSourceMtime(searchPath);
            }

            Bundle bundle = makeBundle(searchPath, tocEntry);
            byte[] codeBytes;
            try {
                if (isBytecode) {
                    try {
                        codeBytes = imp.readCode(fullname, bundle.inputStream, true, mtime);
                    } catch (IOException ioe) {
                        throw Py.ImportError(ioe.getMessage() + "[path=" + fullSearchPath + "]");
                    }
                    if (codeBytes == null) {
                        // bad magic number or non-matching mtime in byte code, try next
                        continue;
                    }
                } else {
                    codeBytes = imp.compileSource(fullname, bundle.inputStream, fullSearchPath);
                }
            } finally {
                bundle.close();
            }

            PyCode code = BytecodeLoader.makeCode(fullname + "$py", codeBytes, fullSearchPath);
            return new ModuleCodeData(code, isPackage, fullSearchPath);
        }
        return null;
    }

    /**
     * Container for PyModule code - whether or not it's a package - and its path.
     */
    protected class ModuleCodeData {
        public PyCode code;
        public boolean isPackage;
        public String path;

        public ModuleCodeData(PyCode code, boolean isPackage, String path) {
            this.code = code;
            this.isPackage = isPackage;
            this.path = path;
        }
    }

    /**
     * A step in the module search order: the file suffix and its entry type.
     */
    protected static class SearchOrderEntry {
        public String suffix;
        public EnumSet<EntryType> type;

        public SearchOrderEntry(String suffix, EnumSet<EntryType> type) {
            this.suffix = suffix;
            this.type = type;
        }
    }

}
