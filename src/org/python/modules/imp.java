
package org.python.modules;

import org.python.core.Py;
import org.python.core.PyFile;
import org.python.core.PyList;
import org.python.core.PyModule;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.core.PyTuple;
import org.python.core.PyInteger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/*
 * A bogus implementation of the CPython builtin module "imp".
 * Only the functions required by IDLE and PMW are implemented.
 * Luckily these function are also the only function that IMO can
 * be implemented under JPython.
 */

public class imp {
    public static PyString __doc__ = new PyString(
        "This module provides the components needed to build your own\n"+
        "__import__ function.  Undocumented functions are obsolete.\n"
    );

    public static final int PY_SOURCE = 1;
    public static final int PY_COMPILED = 2;
    public static final int PKG_DIRECTORY = 5;
    public static final int PY_FROZEN = 7;
    public static final int IMP_HOOK = 9;

    private static class ModuleInfo {
        PyObject file;
        String filename;
        String suffix;
        String mode;
        int type;
        ModuleInfo(PyObject file, String filename, String suffix, String mode, int type) {
            this.file = file;
            this.filename = filename;
            this.suffix = suffix;
            this.mode = mode;
            this.type = type;
        }
    }

    private static PyObject newFile(File file) {
        try {
            return new PyFile(new FileInputStream(file));
        } catch (IOException ioe) {
            throw Py.IOError(ioe);
        }
    }

    private static boolean caseok(File file, String filename, int namelen) {
        return org.python.core.imp.caseok(file, filename, namelen);
    }

    /**
     * This needs to be consolidated with the code in (@see org.python.core.imp).
     *
     * @param name module name
     * @param entry an iterable of paths
     * @param findingPackage if looking for a package only try to locate __init__
     * @return null if no module found otherwise module information
     */
    static ModuleInfo findFromSource(String name, PyObject entry, boolean findingPackage) {
        int nlen = name.length();
        String sourceName = "__init__.py";
        String compiledName = "__init__$py.class";
        String directoryName = entry.toString();

        // The empty string translates into the current working
        // directory, which is usually provided on the system property
        // "user.dir".  Don't rely on File's constructor to provide
        // this correctly.
        if (directoryName.length() == 0) {
            directoryName = null;
        }

        // First check for packages
        File dir = findingPackage ? new File(directoryName) : new File(directoryName, name);
        File sourceFile = new File(dir, sourceName);
        File compiledFile = new File(dir, compiledName);

        boolean pkg = (dir.isDirectory() && caseok(dir, name, nlen)
                && (sourceFile.isFile() || compiledFile.isFile()));

        if(!findingPackage) {
            if(pkg) {
                return new ModuleInfo(Py.None, dir.getPath(), "", "", PKG_DIRECTORY);
            } else {
                Py.writeDebug("import", "trying source " + dir.getPath());
                sourceName = name + ".py";
                compiledName = name + "$py.class";
                sourceFile = new File(directoryName, sourceName);
                compiledFile = new File(directoryName, compiledName);
            }
        }

        if (sourceFile.isFile() && caseok(sourceFile, sourceName, nlen)) {
            if (compiledFile.isFile() && caseok(compiledFile, compiledName, nlen)) {
                Py.writeDebug("import", "trying precompiled " + compiledFile.getPath());
                long pyTime = sourceFile.lastModified();
                long classTime = compiledFile.lastModified();
                if (classTime >= pyTime) {
                    return new ModuleInfo(newFile(compiledFile),
                        compiledFile.getPath(), ".class", "rb", PY_COMPILED);
                }
            }
            return new ModuleInfo(newFile(sourceFile),
                    sourceFile.getPath(), ".py", "r", PY_SOURCE);
        }

        // If no source, try loading precompiled
        Py.writeDebug("import", "trying " + compiledFile.getPath());
        if (compiledFile.isFile() && caseok(compiledFile, compiledName, nlen)) {
            return new ModuleInfo(newFile(compiledFile),
                    compiledFile.getPath(), ".class", "rb", PY_COMPILED);
        }
        return null;
    }

    public static PyObject find_module(String name) {
        return find_module(name, null);
    }

    public static PyObject load_source(String modname, String filename) {
        PyObject mod = Py.None;
        //XXX: bufsize is ignored in PyFile now, but look 3rd arg if this ever changes.
        PyFile file = new PyFile(filename, "r", 1024);
        Object o = file.__tojava__(InputStream.class);
        if (o == Py.NoConversion) {
            throw Py.TypeError("must be a file-like object");
        }
        mod = org.python.core.imp.loadFromSource(
             modname.intern(), (InputStream)o, filename.toString());
        PyObject modules = Py.getSystemState().modules;
        modules.__setitem__(modname.intern(), mod);
        return mod;
    }

    public static PyObject find_module(String name, PyObject path) {
        if (path == null || path == Py.None) {
            path = Py.getSystemState().path;
        }

        PyObject iter = path.__iter__();
        for (PyObject p = null; (p = iter.__iternext__()) != null; ) {
            ModuleInfo mi = findFromSource(name, p, false);
            if(mi == null) {
                continue;
            }
            return new PyTuple(new PyObject[] {
                mi.file,
                new PyString(mi.filename),
                new PyTuple(new PyObject[] {
                    new PyString(mi.suffix),
                    new PyString(mi.mode),
                    Py.newInteger(mi.type)
                }),
            });
        }
        throw Py.ImportError("No module named " + name);
    }

    public static PyObject load_module(String name, PyObject file, PyObject filename, PyTuple data) {
        PyObject mod = Py.None;
        int type = ((PyInteger)data.__getitem__(2).__int__()).getValue();
        while(mod == Py.None) {
            Object o = file.__tojava__(InputStream.class);
            if (o == Py.NoConversion) {
                throw Py.TypeError("must be a file-like object");
            }
            switch (type) {
                case PY_SOURCE:
                    mod = org.python.core.imp.loadFromSource(
                        name.intern(), (InputStream)o, filename.toString());
                    break;
                case PY_COMPILED:
                    mod = org.python.core.imp.loadFromCompiled(
                        name.intern(), (InputStream)o, filename.toString());
                    break;
                case PKG_DIRECTORY:
                    PyModule m = org.python.core.imp.addModule(name);
                    m.__dict__.__setitem__("__path__",
                        new PyList(new PyObject[] { filename }));
                    m.__dict__.__setitem__("__file__", filename);
                    ModuleInfo mi = findFromSource(name, filename, true);
                    type = mi.type;
                    file = mi.file;
                    filename = new PyString(mi.filename);
                    break;
                default:
                    throw Py.ImportError("No module named " + name);
            }
        }
        PyObject modules = Py.getSystemState().modules;
        modules.__setitem__(name.intern(), mod);
        return mod;
    }

    public static PyObject get_suffixes() {
        return new PyList(new PyObject[] {
            new PyTuple(new PyObject[] {
                new PyString(".py"),
                new PyString("r"),
                Py.newInteger(PY_SOURCE),
            }),
            new PyTuple(new PyObject[] {
                new PyString(".class"),
                new PyString("rb"),
                Py.newInteger(PY_COMPILED),
            }),
        });
    }

    public static PyModule new_module(String name) {
        return new PyModule(name, null);
    }
}
