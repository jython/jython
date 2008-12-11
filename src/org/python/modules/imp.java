
package org.python.modules;

import org.python.core.Py;
import org.python.core.PyFile;
import org.python.core.PyList;
import org.python.core.PyModule;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.core.PySystemState;
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
 * be implemented under Jython.
 */

public class imp {
    public static PyString __doc__ = new PyString(
        "This module provides the components needed to build your own\n"+
        "__import__ function.  Undocumented functions are obsolete.\n"
    );

    public static final int PY_SOURCE = 1;
    public static final int PY_COMPILED = 2;
    public static final int C_EXTENSION = 3;
    public static final int PKG_DIRECTORY = 5;
    public static final int C_BUILTIN = 6;
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

    private static boolean caseok(File file, String filename) {
        return org.python.core.imp.caseok(file, filename);
    }

    /**
     * This needs to be consolidated with the code in (@see org.python.core.imp).
     *
     * @param name module name
     * @param entry a path String
     * @param findingPackage if looking for a package only try to locate __init__
     * @return null if no module found otherwise module information
     */
    static ModuleInfo findFromSource(String name, String entry, boolean findingPackage,
                                     boolean preferSource) {
        String sourceName = "__init__.py";
        String compiledName = "__init__$py.class";
        String directoryName = PySystemState.getPathLazy(entry);
        // displayDirName is for identification purposes: when null it
        // forces java.io.File to be a relative path (e.g. foo/bar.py
        // instead of /tmp/foo/bar.py)
        String displayDirName = entry.equals("") ? null : entry;

        // First check for packages
        File dir = findingPackage ? new File(directoryName) : new File(directoryName, name);
        File sourceFile = new File(dir, sourceName);
        File compiledFile = new File(dir, compiledName);

        boolean pkg = dir.isDirectory() && caseok(dir, name) && (sourceFile.isFile()
                                                                 || compiledFile.isFile());

        if(!findingPackage) {
            if(pkg) {
                return new ModuleInfo(Py.None, new File(displayDirName, name).getPath(),
                                      "", "", PKG_DIRECTORY);
            } else {
                Py.writeDebug("import", "trying source " + dir.getPath());
                sourceName = name + ".py";
                compiledName = name + "$py.class";
                sourceFile = new File(directoryName, sourceName);
                compiledFile = new File(directoryName, compiledName);
            }
        }

        if (sourceFile.isFile() && caseok(sourceFile, sourceName)) {
            if (!preferSource && compiledFile.isFile() && caseok(compiledFile, compiledName)) {
                Py.writeDebug("import", "trying precompiled " + compiledFile.getPath());
                long pyTime = sourceFile.lastModified();
                long classTime = compiledFile.lastModified();
                if (classTime >= pyTime) {
                    return new ModuleInfo(newFile(compiledFile),
                                          new File(displayDirName, compiledName).getPath(),
                                          ".class", "rb", PY_COMPILED);
                }
            }
            return new ModuleInfo(newFile(sourceFile),
                                  new File(displayDirName, sourceName).getPath(),
                                  ".py", "r", PY_SOURCE);
        }

        // If no source, try loading precompiled
        Py.writeDebug("import", "trying " + compiledFile.getPath());
        if (compiledFile.isFile() && caseok(compiledFile, compiledName)) {
            return new ModuleInfo(newFile(compiledFile),
                    new File(displayDirName, compiledName).getPath(),
                                  ".class", "rb", PY_COMPILED);
        }
        return null;
    }

    public static PyObject load_source(String modname, String filename) {
        return load_source(modname, filename, null);
    }

    public static PyObject load_source(String modname, String filename, PyObject file) {
        PyObject mod = Py.None;
        if (file == null) {
            // XXX: This should load the accompanying byte code file
            // instead, if it exists
            file = new PyFile(filename, "r", 1024);
        }
        Object o = file.__tojava__(InputStream.class);
        if (o == Py.NoConversion) {
            throw Py.TypeError("must be a file-like object");
        }
        PySystemState sys = Py.getSystemState();
        String compiledFilename =
                org.python.core.imp.makeCompiledFilename(sys.getPath(filename));
        mod = org.python.core.imp.createFromSource(modname.intern(), (InputStream)o,
                                                   filename, compiledFilename);
        PyObject modules = sys.modules;
        modules.__setitem__(modname.intern(), mod);
        return mod;
    }

    public static PyObject find_module(String name) {
        return find_module(name, Py.None);
    }

    public static PyObject find_module(String name, PyObject path) {
        if (path == Py.None && PySystemState.getBuiltin(name) != null) {
            return new PyTuple(Py.None, Py.newString(name),
                               new PyTuple(Py.EmptyString, Py.EmptyString,
                                           Py.newInteger(C_BUILTIN)));
        }

        if (path == Py.None) {
            path = Py.getSystemState().path;
        }
        for (PyObject p : path.asIterable()) {
            ModuleInfo mi = findFromSource(name, p.toString(), false, true);
            if(mi == null) {
                continue;
            }
            return new PyTuple(mi.file,
                               new PyString(mi.filename),
                               new PyTuple(new PyString(mi.suffix),
                                           new PyString(mi.mode),
                                           Py.newInteger(mi.type)));
        }
        throw Py.ImportError("No module named " + name);
    }

    public static PyObject load_module(String name, PyObject file, PyObject filename, PyTuple data) {
        PyObject mod = Py.None;
        PySystemState sys = Py.getSystemState();
        int type = data.__getitem__(2).asInt();
        while(mod == Py.None) {
            Object o = file.__tojava__(InputStream.class);
            if (o == Py.NoConversion) {
                throw Py.TypeError("must be a file-like object");
            }
            String compiledName;
            switch (type) {
                case PY_SOURCE:
                    // XXX: This should load the accompanying byte
                    // code file instead, if it exists
                    String resolvedFilename = sys.getPath(filename.toString());
                    compiledName = org.python.core.imp.makeCompiledFilename(resolvedFilename);
                    if (name.endsWith(".__init__")) {
                        name = name.substring(0, name.length() - ".__init__".length());
                    } else if (name.equals("__init__")) {
                        name = new File(sys.getCurrentWorkingDir()).getName();
                    }
                    mod = org.python.core.imp.createFromSource(name.intern(),
                                                               (InputStream)o,
                                                               filename.toString(),
                                                               compiledName);
                    break;
                case PY_COMPILED:
                    compiledName = filename.toString();
                    // XXX: Ideally we wouldn't care about sourceName here (see
                    // http://bugs.jython.org/issue1605847 msg3805)
                    String sourceName = compiledName;
                    if (compiledName.endsWith("$py.class")) {
                        sourceName = compiledName.substring(0, compiledName.length() - 9) + ".py";
                    }
                    mod = org.python.core.imp.loadFromCompiled(
                        name.intern(), (InputStream)o, sourceName, filename.toString());
                    break;
                case PKG_DIRECTORY:
                    PyModule m = org.python.core.imp.addModule(name);
                    m.__dict__.__setitem__("__path__",
                        new PyList(new PyObject[] { filename }));
                    m.__dict__.__setitem__("__file__", filename);
                    ModuleInfo mi = findFromSource(name, filename.toString(), true, true);
                    type = mi.type;
                    file = mi.file;
                    filename = new PyString(mi.filename);
                    break;
                default:
                    throw Py.ImportError("No module named " + name);
            }
        }
        PyObject modules = sys.modules;
        modules.__setitem__(name.intern(), mod);
        return mod;
    }

    public static PyObject get_suffixes() {
        return new PyList(new PyObject[] {new PyTuple(new PyString(".py"),
                                                      new PyString("r"),
                                                      Py.newInteger(PY_SOURCE)),
                                          new PyTuple(new PyString(".class"),
                                                      new PyString("rb"),
                                                      Py.newInteger(PY_COMPILED)),});
    }

    public static PyModule new_module(String name) {
        return new PyModule(name, null);
    }

    /**
     * Acquires the interpreter's import lock for the current thread.
     *
     * This lock should be used by import hooks to ensure
     * thread-safety when importing modules.
     *
     */
    public static void acquire_lock() {
        org.python.core.imp.importLock.lock();
    }

    /**
     * Release the interpreter's import lock.
     *
     */
    public static void release_lock() {
        try{
            org.python.core.imp.importLock.unlock();
        }catch(IllegalMonitorStateException e){
            throw Py.RuntimeError("not holding the import lock");
        }
    }

    /**
     * Return true if the import lock is currently held, else false.
     *
     * @return true if the import lock is currently held, else false.
     */
    public static boolean lock_held() {
        return org.python.core.imp.importLock.isHeldByCurrentThread();
    }
}
