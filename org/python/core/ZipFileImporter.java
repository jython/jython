package org.python.core;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;

/**
 * Load python source from jar or zip files.
 */
public class ZipFileImporter extends PyObject {

    private SyspathArchive archive;

    /**
     * If this path is not an archive (.zip or .jar) then raise
     * an ImportError, otherwise this instance will handle this
     * path.
     * @param path the path to check for modules
     */
    public ZipFileImporter(PyObject path) {
        if(!(path instanceof SyspathArchive)) {
            throw Py.ImportError(path.toString());
        }
        this.archive = (SyspathArchive)path;
    }

    /**
     * Find the module for the fully qualified name.
     * @param name the fully qualified name of the module
     * @return a loader instance if this importer can load the module, None otherwise
     */
    public PyObject find_module(String name) {
        return find_module(name, Py.None);
    }

    /**
     * Find the module for the fully qualified name.
     * @param name the fully qualified name of the module
     * @param path if installed on the meta-path None or a module path
     * @return a loader instance if this importer can load the module, None otherwise
     */
    public PyObject find_module(String name, PyObject path) {
        ZipModuleInfo zip = getModuleInfo(name, this.archive);
        return zip == null ? Py.None : this;
    }

    /**
     * A loaded module for the fully qualified name.
     * @param name the fully qualified name
     * @return a loaded module (added to sys.path)
     */
    public PyObject load_module(String name) {
        ZipModuleInfo zip = getModuleInfo(name, this.archive);
        if(zip == null) {
            throw Py.ImportError("error loading " + name);
        }
        return loadFromZipFile(name, zip);
    }

    /**
     * Returns the last part of a fully qualified name.  For example, the name
     * <p>
     * <code>
     * a.b.c
     * </code>
     * </p>
     * would return <code>c</code>.
     * @param name a fully qualified name
     * @return the last part of a fully qualified name
     */
    private String getSubName(String name) {
        int x = name.lastIndexOf(".");
        if (x >= 0) {
            return name.substring(x+1);
        }
        return name;
    }

    /**
     * Find the module name starting at the zipArchive root.  This method will
     * look for both package and non-package names in the archive.  If the name
     * is not found null will be returned.
     * @param name the fully qualified module name
     * @param zipArchive the root of the path to begin looking
     * @return null if the module is not found, a ZipModuleInfo instance otherwise
     */
    private ZipModuleInfo getModuleInfo(String name, SyspathArchive zipArchive) {
        String entryName = getSubName(name);

        String sourceName = entryName + "/__init__.py";
        String compiledName = entryName + "/__init__$py.class";
        ZipEntry sourceEntry = zipArchive.getEntry(sourceName);
        ZipEntry compiledEntry = zipArchive.getEntry(compiledName);

        boolean pkg = (sourceEntry != null || compiledEntry != null);
        if(!pkg) {
            sourceName = entryName + ".py";
            compiledName = entryName + "$py.class";
            sourceEntry = zipArchive.getEntry(sourceName);
            compiledEntry = zipArchive.getEntry(compiledName);
        } else {
           zipArchive = zipArchive.makeSubfolder(entryName);
        }

        ZipModuleInfo info = null;
        if (sourceEntry != null) {
            Py.writeDebug("import", "trying source entry: " + sourceName
                    + " from jar/zip file " + zipArchive);
            if (compiledEntry != null) {
                Py.writeDebug("import", "trying precompiled entry "
                        + compiledName + " from jar/zip file " + zipArchive);
                long pyTime = sourceEntry.getTime();
                long classTime = compiledEntry.getTime();
                if (classTime >= pyTime) {
                    info = new ZipModuleInfo(zipArchive, compiledEntry, true);
                }
            }
            info = new ZipModuleInfo(zipArchive, sourceEntry, false);
        }

        if(pkg && info != null) {
            info.path = new PyList(new PyObject[]{zipArchive});
        }

        return info;
    }

    /**
     * Load the module with the information provided in the info.
     * @param moduleName the module name to add to sys.path
     * @param info the information about the module to load
     * @return the loaded module
     */
    private PyObject loadFromZipFile(String moduleName, ZipModuleInfo info) {

        PyModule m = null;
        if(info.path != null) {
            m = imp.addModule(moduleName);
            m.__dict__.__setitem__("__path__", info.path);
            m.__dict__.__setitem__("__loader__", this);
        }

        InputStream is = null; // should this be closed?
        ZipEntry entry = info.zipEntry;
        try {
            is = info.archive.getInputStream(entry);
        } catch (IOException e) {
            Py.writeDebug("import", "loadFromZipFile exception: " + e.toString());
            throw Py.ImportError("error loading from zipfile");
        }
        if (info.compiled) {
            PyObject o = imp.createFromPyClass(moduleName, is, true, entry.getName());
            return (m == null) ? o : m;
        } else {
            PyObject o = imp.createFromSource(moduleName, is, entry.getName(), null);
            return (m == null) ? o : m;
        }
    }

    private class ZipModuleInfo {
        /** The path of the package if it is a package. */
        public PyObject path;
        /** Whether the code is already compiled. */
        public boolean compiled;
        /** The zip entry for the file to load. */
        public ZipEntry zipEntry;
        /** The archive in which the zip entry resides. */
        public SyspathArchive archive;

        public ZipModuleInfo(SyspathArchive archive, ZipEntry zipEntry, boolean compiled) {
            this(archive, zipEntry, compiled, null);
        }

        public ZipModuleInfo(SyspathArchive archive, ZipEntry zipEntry, boolean compiled, PyObject path) {
            this.path = path;
            this.archive = archive;
            this.zipEntry = zipEntry;
            this.compiled = compiled;
        }
    }
}

