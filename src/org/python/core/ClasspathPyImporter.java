/* Copyright (c) Jython Developers */
package org.python.core;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.logging.Level;

import org.python.core.util.FileUtil;
import org.python.core.util.StringUtil;
import org.python.core.util.importer;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedType;
import org.python.util.Generic;

@Untraversable
@ExposedType(name="ClasspathPyImporter")
public class ClasspathPyImporter extends importer<String> {

    public static final String PYCLASSPATH_PREFIX = "__pyclasspath__/";
    public static final PyType TYPE = PyType.fromClass(ClasspathPyImporter.class);

    public ClasspathPyImporter(PyType subType) {
        super(subType);
    }

    public ClasspathPyImporter() {
        super();
    }

    @ExposedNew
    @ExposedMethod
    final void ClasspathPyImporter___init__(PyObject[] args, String[] kwds) {
        ArgParser ap = new ArgParser("__init__", args, kwds, new String[] {"path"});
        String path = ap.getString(0);
        if (path == null || !path.startsWith(PYCLASSPATH_PREFIX)) {
            throw Py.ImportError("path isn't for classpath importer");
        }
        if (!path.endsWith("/")) {
            path += "/";
        }
        this.path = path;
    }

    /**
     * Return the contents of the jarred file at the specified path
     * as bytes.
     *
     * @param path a String path name within the archive
     * @return a String of data in binary mode (no CRLF)
     */
    @Override
    public String get_data(String path) {
        return ClasspathPyImporter_get_data(path);
    }

    @ExposedMethod
    final String ClasspathPyImporter_get_data(String path) {
        // Strip any leading occurrence of the hook string
        int len = PYCLASSPATH_PREFIX.length();
        if (len < path.length() && path.startsWith(PYCLASSPATH_PREFIX)) {
            path = path.substring(len);
        }

        // Bundle wraps the stream together with a close operation
        try (Bundle bundle = makeBundle(path, makeEntry(path))) {
            byte[] data = FileUtil.readBytes(bundle.inputStream);
            return StringUtil.fromBytes(data);
        } catch (IOException ioe) {
            throw Py.IOError(ioe);
        }
    }

    /**
     * Return the source code for the module as a string (using
     * newline characters for line endings)
     *
     * @param fullname the fully qualified name of the module
     * @return a String of the module's source code or null
     */
    public String get_source(String fullname) {
        return ClasspathPyImporter_get_source(fullname);
    }

    @ExposedMethod
    final String ClasspathPyImporter_get_source(String fullname) {

        ModuleInfo moduleInfo = getModuleInfo(fullname);

        if (moduleInfo == ModuleInfo.ERROR) {
            return null;

        } else if (moduleInfo == ModuleInfo.NOT_FOUND) {
            throw Py.ImportError(String.format("can't find module '%s'", fullname));

        } else {
            // Turn the module name into a source file name
            String path = makeFilename(fullname);
            if (moduleInfo == ModuleInfo.PACKAGE) {
                path += File.separator + "__init__.py";
            } else {
                path += ".py";
            }

            // Bundle wraps the stream together with a close operation
            try (Bundle bundle = makeBundle(path, makeEntry(path))) {
                InputStream is = bundle.inputStream;
                if (is != null) {
                    byte[] data = FileUtil.readBytes(is);
                    return StringUtil.fromBytes(data);
                } else {
                    // we have the module, but no source
                    return null;
                }
            } catch (IOException ioe) {
                throw Py.IOError(ioe);
            }
        }

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
    final PyObject ClasspathPyImporter_find_module(String fullname, String path) {
        return importer_find_module(fullname, path);
    }

    /**
     * Determine whether a module is a package.
     *
     * @param fullname the fully qualified name of the module
     * @return whether the module is a package
     */
    @ExposedMethod
    final boolean ClasspathPyImporter_is_package(String fullname) {
        return importer_is_package(fullname);
    }

    /**
     * Return the code object associated with the module.
     *
     * @param fullname the fully qualified name of the module
     * @return the module's PyCode object or None
     */
    @ExposedMethod
    final PyObject ClasspathPyImporter_get_code(String fullname) {
        ModuleCodeData moduleCodeData = getModuleCode(fullname);
        if (moduleCodeData != null) {
            return moduleCodeData.code;
        }
        return Py.None;
    }

    /**
     * Load a module for the fully qualified name.
     *
     * @param fullname the fully qualified name of the module
     * @return a loaded PyModule
     */
    @ExposedMethod
    final PyObject ClasspathPyImporter_load_module(String fullname) {
        return importer_load_module(fullname);
    }

    @Override
    protected long getSourceMtime(String path) {
        // Can't determine this easily
        return -1;
    }

    @Override
    protected Bundle makeBundle(String fullFilename, String entry) {
        InputStream is = entries.remove(entry);
        return new Bundle(is) {
            @Override
            public void close() {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    throw Py.JavaError(e);
                }
            }
        };
    }

    @Override
    protected String makeEntry(String filename) {
        // In some contexts, the resource string arrives as from os.path.join(*parts)
        if (!getSeparator().equals(File.separator)) {
            filename = filename.replace(File.separator, getSeparator());
        }
        if (entries.containsKey(filename)) {
            return filename;
        }
        InputStream is;
        if (Py.getSystemState().getClassLoader() != null) {
            is = tryClassLoader(filename, Py.getSystemState().getClassLoader(), "sys");
        } else {
            is = tryClassLoader(filename, imp.getParentClassLoader(), "parent");
        }
        if (is != null) {
            entries.put(filename, is);
            return filename;
        }
        return null;
    }

    private InputStream tryClassLoader(String fullFilename, ClassLoader loader, String place) {
        if (loader != null) {
            logger.log(Level.FINE, "# trying {0} in {1} class loader",
                    new Object[] {fullFilename, place});
            return loader.getResourceAsStream(fullFilename);
        }
        return null;
    }

    @Override
    protected String makeFilename(String fullname) {
        return path.replace(PYCLASSPATH_PREFIX, "") + fullname.replace('.', '/');
    }

    @Override
    protected String makeFilePath(String fullname) {
        return path + fullname.replace('.', '/');
    }

    @Override
    protected String makePackagePath(String fullname) {
        return path;
    }

    @Override
    protected String getSeparator() {
        return "/";
    }

    private Map<String, InputStream> entries = Generic.map();

    private String path;
}
