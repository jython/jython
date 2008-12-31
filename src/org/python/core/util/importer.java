package org.python.core.util;

import java.io.InputStream;
import java.util.EnumSet;
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
        return new SearchOrderEntry[] {
              new SearchOrderEntry(getSeparator() + "__init__$py.class",
                  EnumSet.of(EntryType.IS_PACKAGE, EntryType.IS_BYTECODE)),
              new SearchOrderEntry(getSeparator() + "__init__.py",
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
        if (moduleCodeData.ispackage) {
            // add __path__ to the module *before* the code gets executed
            PyList pkgpath = new PyList();
            pkgpath.add(makePackagePath(fullname));
            mod.__dict__.__setitem__("__path__", pkgpath);
        }
        imp.createFromCode(fullname, moduleCodeData.code, moduleCodeData.path);
        Py.writeDebug("import", "import " + fullname + " # loaded from " + moduleCodeData.path);
        return mod;
    }

    /**
     * Bundle is an InputStream, bundled together with a method that can close the input stream and
     * whatever resources are associated with it when the resource is imported.
     */
    protected abstract static class Bundle {
        public InputStream inputStream;

        public Bundle(InputStream inputStream) {
            this.inputStream = inputStream;
        }

        /**
         * Close the underlying resource if necessary. Raises an IOError if a problem occurs.
         */
        public abstract void close();
    }

    protected abstract boolean isAcceptableBytecode(String searchPath, T entry);

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
        String fullPath = makePackagePath(fullname);

        if (path.length() < 0) {
            return null;
        }

        for (SearchOrderEntry entry : searchOrder) {
            String suffix = entry.suffix;
            String searchPath = path + suffix;
            String fullSearchPath = fullPath + suffix;

            Py.writeDebug("import", "# trying " + searchPath);
            T tocEntry = makeEntry(searchPath);
            if (tocEntry == null) {
                continue;
            }

            boolean ispackage = entry.type.contains(EntryType.IS_PACKAGE);
            boolean isbytecode = entry.type.contains(EntryType.IS_BYTECODE);

            if (isbytecode && !isAcceptableBytecode(searchPath, tocEntry)) {
                continue;
            }

            Bundle bundle = makeBundle(searchPath, tocEntry);
            byte[] codeBytes;
            if (isbytecode) {
                codeBytes = imp.readCode(fullname, bundle.inputStream, true);
            } else {
                codeBytes = imp.compileSource(fullname, bundle.inputStream, fullSearchPath);
            }
            bundle.close();

            if (codeBytes == null) {
                // bad magic number or non-matching mtime in byte code, try next
                continue;
            }

            PyCode code = BytecodeLoader.makeCode(fullname + "$py", codeBytes, fullSearchPath);
            return new ModuleCodeData(code, ispackage, fullSearchPath);
        }
        return null;
    }

    /**
     * Container for PyModule code - whether or not it's a package - and its path.
     */
    protected class ModuleCodeData {
        public PyCode code;
        public boolean ispackage;
        public String path;

        public ModuleCodeData(PyCode code, boolean ispackage, String path) {
            this.code = code;
            this.ispackage = ispackage;
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
