// Copyright (c) Corporation for National Research Initiatives
package org.python.core;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import org.python.core.util.FileUtil;

/**
 * Utility functions for "import" support.
 */
public class imp {
    private static final String IMPORT_LOG = "import";

    private static final String UNKNOWN_SOURCEFILE = "<unknown>";

    public static final int APIVersion = 13;

    /** A non-empty fromlist for __import__'ing sub-modules. */
    private static final PyObject nonEmptyFromlist = new PyTuple(Py.newString("__doc__"));

    /** Synchronizes import operations */
    public static final ReentrantLock importLock = new ReentrantLock();

    private static Object syspathJavaLoaderLock = new Object();

    private static ClassLoader syspathJavaLoader = null;

    public static ClassLoader getSyspathJavaLoader() {
        synchronized (syspathJavaLoaderLock) {
            if (syspathJavaLoader == null) {
                syspathJavaLoader = new SyspathJavaLoader();
            }
        }
        return syspathJavaLoader;
    }

    private imp() {
    }

    /**
     * If the given name is found in sys.modules, the entry from there is
     * returned. Otherwise a new PyModule is created for the name and added to
     * sys.modules
     */
    public static PyModule addModule(String name) {
        name = name.intern();
        PyObject modules = Py.getSystemState().modules;
        PyModule module = (PyModule) modules.__finditem__(name);
        if (module != null) {
            return module;
        }
        module = new PyModule(name, null);
        modules.__setitem__(name, module);
        return module;
    }

    private static byte[] readBytes(InputStream fp) {
        try {
            return FileUtil.readBytes(fp);
        } catch(IOException ioe) {
            throw Py.IOError(ioe);
        } finally {
            try {
                fp.close();
            } catch(IOException e) {
                throw Py.IOError(e);
            }
        }
    }

    private static InputStream makeStream(File file) {
        try {
            return new FileInputStream(file);
        } catch (IOException ioe) {
            throw Py.IOError(ioe);
        }
    }

    static PyObject createFromPyClass(String name, InputStream fp,
            boolean testing, String fileName) {
        byte[] data = unmarshalCode(name, fp, testing);
        if (testing && data == null) {
            return null;
        }
        PyCode code;
        try {
            code = BytecodeLoader.makeCode(name + "$py", data, fileName);
        } catch (Throwable t) {
            if (testing) {
                return null;
            } else {
                throw Py.JavaError(t);
            }
        }

        Py.writeComment(IMPORT_LOG, "'" + name + "' as " + fileName);

        return createFromCode(name, code, fileName);
    }
    
    public static byte[] unmarshalCode(String name, InputStream fp,
            boolean testing) {
        byte[] data = readBytes(fp);
        int n = data.length;

        int api;
        try {
            APIReader ar = new APIReader(data);
            api = ar.getVersion();
        } catch (IOException i) {
            api = -1;
        }
        if (api != APIVersion) {
            if (testing) {
                return null;
            } else {
                throw Py.ImportError("invalid api version(" + api + " != "
                        + APIVersion + ") in: " + name);
            }
        }
        return data;
    }
    
    public static byte[] compileSource(String name, File file, String sourceFilename,
            String compiledFilename) {
        if (sourceFilename == null) {
            sourceFilename = file.toString();
        }
        return compileSource(name, makeStream(file), sourceFilename);
    }

    public static String makeCompiledFilename(String filename) {
        return filename.substring(0, filename.length() - 3) + "$py.class";
    }
    
    /**
     * Stores the bytes in compiledSource in compiledFilename.
     * 
     * If compiledFilename is null it's set to the results of
     * makeCompiledFilename(sourcefileName)
     * 
     * If sourceFilename is null or set to UNKNOWN_SOURCEFILE null is returned
     * 
     * @return the compiledFilename eventually used or null if a
     *         compiledFilename couldn't be determined of if an error was thrown
     *         while writing to the cache file.
     */
    public static String cacheCompiledSource(String sourceFilename,
                                              String compiledFilename,
                                              byte[] compiledSource) {
        if(compiledFilename == null){
            if(sourceFilename == null || sourceFilename.equals(UNKNOWN_SOURCEFILE)){
               return null; 
            }
            compiledFilename = makeCompiledFilename(sourceFilename);
        }
        FileOutputStream fop = null;
        try {
            fop = new FileOutputStream(compiledFilename);
            fop.write(compiledSource);
            fop.close();
            return compiledFilename;
        } catch(IOException exc) {
            // If we can't write the cache file, just log and continue
            Py.writeDebug(IMPORT_LOG, "Unable to write to source cache file '"
                    + compiledFilename + "' due to " + exc);
            return null;
        } finally {
            if(fop != null) {
                try {
                    fop.close();
                } catch(IOException e) {
                    Py.writeDebug(IMPORT_LOG,
                                  "Unable to close source cache file '"
                                          + compiledFilename + "' due to " + e);
                }
            }
        }
    }

    public static byte[] compileSource(String name,
                                InputStream fp,
                                String filename) {
        ByteArrayOutputStream ofp = new ByteArrayOutputStream();
        try {
            if(filename == null) {
                filename = UNKNOWN_SOURCEFILE;
            }
            org.python.antlr.ast.modType node;
            try {
                node = ParserFacade.parse(fp, "exec", filename, new CompilerFlags());
            } finally {
                fp.close();
            }
            org.python.compiler.Module.compile(node,
                                               ofp,
                                               name + "$py",
                                               filename,
                                               true,
                                               false,
                                               true,
                                               null);
            return ofp.toByteArray();
        } catch(Throwable t) {
            throw ParserFacade.fixParseError(null, t, filename);
        }
    }

    public static PyObject createFromSource(String name, InputStream fp,
            String filename) {
        return createFromSource(name, fp, filename, null);
    }
    
    public static PyObject createFromSource(String name, InputStream fp,
            String filename, String outFilename) {
        byte[] bytes = compileSource(name, fp, filename);
        outFilename = cacheCompiledSource(filename, outFilename, bytes);

        Py.writeComment(IMPORT_LOG, "'" + name + "' as " + filename);

        PyCode code = BytecodeLoader.makeCode(name + "$py", bytes, filename);
        return createFromCode(name, code, filename);
    }
    
    /**
     * Returns a module with the given name whose contents are the results of
     * running c. __file__ is set to whatever is in c.
     */
    public static PyObject createFromCode(String name, PyCode c){
        return createFromCode(name, c, null);
    }
    
    /**
     * createFromCode returns a module with the given name whose contents are
     * the results of running c. Sets __file__ on the module to be
     * moduleLocation unless moduleLocation is null. If c comes from a local
     * .py file or compiled $py.class class moduleLocation should be the result
     * of running new File(moduleLocation).getAbsoultePath(). If c comes from a
     * remote file or is a jar moduleLocation should be the full uri for c.
     */
    public static PyObject createFromCode(String name, PyCode c, String moduleLocation) {
        PyModule module = addModule(name);

        PyTableCode code = null;
        if (c instanceof PyTableCode) {
            code = (PyTableCode) c;
        }
        try {
            PyFrame f = new PyFrame(code, module.__dict__, module.__dict__, null);
            code.call(f);
        } catch (RuntimeException t) {
            Py.getSystemState().modules.__delitem__(name.intern());
            throw t;
        }
        if(moduleLocation != null) {
            module.__setattr__("__file__",
                               new PyString(moduleLocation));
        }else{
            Py.writeDebug(IMPORT_LOG, "No fileName known to set __file__ for " + name + ".");
        }
        return module;
    }

    static PyObject createFromClass(String name, Class c) {
        // Two choices. c implements PyRunnable or c is Java package
        if(PyRunnable.class.isAssignableFrom(c)) {
            try {
                return createFromCode(name,
                                      ((PyRunnable)c.newInstance()).getMain());
            } catch(InstantiationException e) {
                throw Py.JavaError(e);
            } catch(IllegalAccessException e) {
                throw Py.JavaError(e);
            }
        }
        return PyJavaClass.lookup(c); // xxx?
    }

    static PyObject getPathImporter(PyObject cache, PyList hooks, PyObject p) {

        // attempt to get an importer for the path
        // use null as default value since Py.None is
        // a valid value in the cache for the default
        // importer
        PyObject importer = cache.__finditem__(p);
        if (importer != null) {
            return importer;
        }

        // nothing in the cache, so check all hooks
        PyObject iter = hooks.__iter__();
        for (PyObject hook; (hook = iter.__iternext__()) != null;) {
            try {
                importer = hook.__call__(p);
                break;
            } catch (PyException e) {
                if (!Py.matchException(e, Py.ImportError)) {
                    throw e;
                }
            }
        }

        importer = (importer == null ? Py.None : importer);
        cache.__setitem__(p, importer);

        return importer;
    }

    static PyObject replacePathItem(PySystemState sys, PyObject path) {
        if (path instanceof SyspathArchive) {
            // already an archive
            return null;
        }

        try {
            // this has the side affect of adding the jar to the PackageManager
            // during the initialization of the SyspathArchive
            return new SyspathArchive(sys.getPath(path.toString()));
        } catch (Exception e) {
            return null;
        }
    }

    static PyObject find_module(String name, String moduleName, PyList path) {

        PyObject loader = Py.None;
        PySystemState sys = Py.getSystemState();
        PyObject metaPath = sys.meta_path;

        /*
         * Needed to convert all entries on the path to SyspathArchives if
         * necessary.
         */
        PyList ppath = path == null ? sys.path : path;
        for (int i = 0; i < ppath.__len__(); i++) {
            PyObject p = ppath.__getitem__(i);
            PyObject q = replacePathItem(sys, p);
            if (q == null) {
                continue;
            }
            ppath.__setitem__(i, q);
        }

        for (PyObject importer : metaPath.asIterable()) {
            PyObject findModule = importer.__getattr__("find_module");
            loader = findModule.__call__(new PyObject[] {
                    new PyString(moduleName), path == null ? Py.None : path });
            if (loader != Py.None) {
                return loadFromLoader(loader, moduleName);
            }
        }

        PyObject ret = loadBuiltin(moduleName);
        if (ret != null) {
            return ret;
        }

        path = path == null ? sys.path : path;
        for (int i = 0; i < path.__len__(); i++) {
            PyObject p = path.__getitem__(i);
            PyObject importer = getPathImporter(sys.path_importer_cache,
                    sys.path_hooks, p);
            if (importer != Py.None) {
                PyObject findModule = importer.__getattr__("find_module");
                loader = findModule.__call__(new PyObject[] { new PyString(
                        moduleName) });
                if (loader != Py.None) {
                    return loadFromLoader(loader, moduleName);
                }
            }
            ret = loadFromSource(sys, name, moduleName, p.__str__().toString());
            if (ret != null) {
                return ret;
            }
        }

        return ret;
    }

    private static PyObject loadBuiltin(String name) {
        if (name == "sys") {
            Py.writeComment(IMPORT_LOG, "'" + name + "' as sys in "
                    + "builtin modules");
            return Py.java2py(Py.getSystemState());
        }
        String mod = PySystemState.getBuiltin(name);
        if (mod != null) {
            Class c = Py.findClassEx(mod, "builtin modules");
            if (c != null) {
                Py.writeComment(IMPORT_LOG, "'" + name + "' as " + mod
                        + " in builtin modules");
                try {
                    if (PyObject.class.isAssignableFrom(c)) { // xxx ok?
                        return PyType.fromClass(c);
                    }
                    return createFromClass(name, c);
                } catch (NoClassDefFoundError e) {
                    throw Py.ImportError("Cannot import " + name
                            + ", missing class " + c.getName());
                }
            }
        }
        return null;
    }

    static PyObject loadFromLoader(PyObject importer, String name) {
        PyObject load_module = importer.__getattr__("load_module");
        return load_module.__call__(new PyObject[] { new PyString(name) });
    }

    public static PyObject loadFromCompiled(String name, InputStream stream,
            String filename) {
        return createFromPyClass(name, stream, false, filename);
    }

    static PyObject loadFromSource(PySystemState sys, String name, String modName, String entry) {

        String sourceName = "__init__.py";
        String compiledName = "__init__$py.class";
        String directoryName = sys.getPath(entry);
        // displayDirName is for identification purposes (e.g.
        // __file__): when null it forces java.io.File to be a
        // relative path (e.g. foo/bar.py instead of /tmp/foo/bar.py)
        String displayDirName = entry.equals("") ? null : entry.toString();

        // First check for packages
        File dir = new File(directoryName, name);
        File sourceFile = new File(dir, sourceName);
        File compiledFile = new File(dir, compiledName);

        boolean pkg = dir.isDirectory() && caseok(dir, name) && (sourceFile.isFile()
                                                                 || compiledFile.isFile());
        if (!pkg) {
            Py.writeDebug(IMPORT_LOG, "trying source " + dir.getPath());
            sourceName = name + ".py";
            compiledName = name + "$py.class";
            sourceFile = new File(directoryName, sourceName);
            compiledFile = new File(directoryName, compiledName);
        } else {
            PyModule m = addModule(modName);
            PyObject filename = new PyString(new File(displayDirName, name).getPath());
            m.__dict__.__setitem__("__path__", new PyList(
                    new PyObject[] { filename }));
            m.__dict__.__setitem__("__file__", filename);
        }

        if (sourceFile.isFile() && caseok(sourceFile, sourceName)) {
            String filename;
            if (pkg) {
                filename = new File(new File(displayDirName, name), sourceName).getPath();
            } else {
                filename = new File(displayDirName, sourceName).getPath();
            }
            if(compiledFile.isFile() && caseok(compiledFile, compiledName)) {
                Py.writeDebug(IMPORT_LOG, "trying precompiled "
                        + compiledFile.getPath());
                long pyTime = sourceFile.lastModified();
                long classTime = compiledFile.lastModified();
                if(classTime >= pyTime) {
                    // XXX: filename should use compiledName here (not
                    // sourceName), but this currently breaks source
                    // code printed out in tracebacks
                    PyObject ret = createFromPyClass(modName, makeStream(compiledFile),
                                                     true, filename);
                    if(ret != null) {
                        return ret;
                    }
                }
            }
            return createFromSource(modName, makeStream(sourceFile), filename,
                                    compiledFile.getPath());
        }
        // If no source, try loading precompiled
        Py.writeDebug(IMPORT_LOG, "trying precompiled with no source "
                + compiledFile.getPath());
        if(compiledFile.isFile() && caseok(compiledFile, compiledName)) {
            String filename = new File(displayDirName, compiledName).getPath();
            return createFromPyClass(modName, makeStream(compiledFile), true, filename);
        }
        return null;
    }

    public static boolean caseok(File file, String filename) {
        if (Options.caseok) {
            return true;
        }
        try {
            File canFile = new File(file.getCanonicalPath());
            return filename.regionMatches(0, canFile.getName(), 0, filename.length());
        } catch (IOException exc) {
            return false;
        }
    }

    /**
     * Load the module by name. Upon loading the module it will be added to
     * sys.modules.
     * 
     * @param name the name of the module to load
     * @return the loaded module
     */
    public static PyObject load(String name) {
        return import_first(name, new StringBuffer());
    }

    /**
     * Find the parent module name for a module. If __name__ does not exist in
     * the module then the parent is null. If __name__ does exist then the
     * __path__ is checked for the parent module. For example, the __name__
     * 'a.b.c' would return 'a.b'.
     * 
     * @param dict the __dict__ of a loaded module
     * @return the parent name for a module
     */
    private static String getParent(PyObject dict) {
        PyObject tmp = dict.__finditem__("__name__");
        if (tmp == null) {
            return null;
        }
        String name = tmp.toString();

        tmp = dict.__finditem__("__path__");
        if (tmp != null && tmp instanceof PyList) {
            return name.intern();
        } else {
            int dot = name.lastIndexOf('.');
            if (dot == -1) {
                return null;
            }
            return name.substring(0, dot).intern();
        }
    }

    /**
     * 
     * @param mod a previously loaded module
     * @param parentNameBuffer
     * @param name the name of the module to load
     * @return null or None
     */
    private static PyObject import_next(PyObject mod,
            StringBuffer parentNameBuffer, String name, String outerFullName, PyObject fromlist) {
        if (parentNameBuffer.length() > 0) {
            parentNameBuffer.append('.');
        }
        parentNameBuffer.append(name);

        String fullName = parentNameBuffer.toString().intern();

        PyObject modules = Py.getSystemState().modules;
        PyObject ret = modules.__finditem__(fullName);
        if (ret != null) {
            return ret;
        }
        if (mod == null) {
            ret = find_module(fullName.intern(), name, null);
        } else {
            ret = mod.impAttr(name.intern());
        }
        if (ret == null || ret == Py.None) {
            if (JavaImportHelper.tryAddPackage(outerFullName, fromlist)) {
                ret = modules.__finditem__(fullName);
            }
            return ret;
        }
        if (modules.__finditem__(fullName) == null) {
            modules.__setitem__(fullName, ret);
        } else {
            ret = modules.__finditem__(fullName);
        }
        return ret;
    }

    // never returns null or None
    private static PyObject import_first(String name,
            StringBuffer parentNameBuffer) {
        PyObject ret = import_next(null, parentNameBuffer, name, null, null);
        if (ret == null || ret == Py.None) {
            throw Py.ImportError("No module named " + name);
        }
        return ret;
    }
    
    
    private static PyObject import_first(String name, StringBuffer parentNameBuffer, String fullName, PyObject fromlist) {
        PyObject ret = import_next(null, parentNameBuffer, name, fullName, fromlist);
        if (ret == null || ret == Py.None) {
            if (JavaImportHelper.tryAddPackage(fullName, fromlist)) {
                ret = import_next(null, parentNameBuffer, name, fullName, fromlist);
            }
        }
        if (ret == null || ret == Py.None) {
            throw Py.ImportError("No module named " + name);
        }
        return ret;
    }
    

    // Hierarchy-recursively search for dotted name in mod;
    // never returns null or None
    // ??pending: check if result is really a module/jpkg/jclass?
    private static PyObject import_logic(PyObject mod,
            StringBuffer parentNameBuffer, String dottedName, String fullName, PyObject fromlist) {
        int dot = 0;
        int last_dot = 0;

        do {
            String name;
            dot = dottedName.indexOf('.', last_dot);
            if (dot == -1) {
                name = dottedName.substring(last_dot);
            } else {
                name = dottedName.substring(last_dot, dot);
            }
            mod = import_next(mod, parentNameBuffer, name, fullName, fromlist);
            if (mod == null || mod == Py.None) {
            	throw Py.ImportError("No module named " + name);
            }
            last_dot = dot + 1;
        } while (dot != -1);

        return mod;
    }

    /**
     * Most similar to import.c:import_module_ex.
     * 
     * @param name
     * @param top
     * @param modDict
     * @return a module
     */
    private static PyObject import_name(String name, boolean top,
            PyObject modDict, PyObject fromlist) {
        if (name.length() == 0) {
            throw Py.ValueError("Empty module name");
        }
        PyObject modules = Py.getSystemState().modules;
        PyObject pkgMod = null;
        String pkgName = null;
        if (modDict != null && !(modDict instanceof PyNone)) {
            pkgName = getParent(modDict);
            pkgMod = modules.__finditem__(pkgName);
            if (pkgMod != null && !(pkgMod instanceof PyModule)) {
                pkgMod = null;
            }
        }
        int dot = name.indexOf('.');
        String firstName;
        if (dot == -1) {
            firstName = name;
        } else {
            firstName = name.substring(0, dot);
        }
        StringBuffer parentNameBuffer = new StringBuffer(pkgMod != null ? pkgName : "");
        PyObject topMod = import_next(pkgMod, parentNameBuffer, firstName, name, fromlist);
        if (topMod == Py.None || topMod == null) {
            // Add None to sys.modules for submodule or subpackage names that aren't found, but 
            // leave top-level entries out.  This allows them to be tried again if another
            // import attempt is made after they've been added to sys.path.
            if (topMod == null && pkgMod != null) {
                modules.__setitem__(parentNameBuffer.toString().intern(), Py.None);
            }
            parentNameBuffer = new StringBuffer("");
            // could throw ImportError
            topMod = import_first(firstName, parentNameBuffer, name, fromlist);
        }
        PyObject mod = topMod;
        if (dot != -1) {
            // could throw ImportError
            mod = import_logic(topMod, parentNameBuffer, name
                    .substring(dot + 1), name, fromlist);
        }
        if (top) {
            return topMod;
        }

        if (fromlist != null && fromlist != Py.None) {
            StringBuffer modNameBuffer = new StringBuffer(name);
            for (PyObject submodName : fromlist.asIterable()) {
                if (mod.__findattr__(submodName.toString()) != null
                    || submodName.toString().equals("*")) {
                    continue;
                }
                String fullName = modNameBuffer.toString() + "." + submodName.toString();
                import_next(mod, modNameBuffer, submodName.toString(), fullName, null);
            }
        }
        return mod;
    }

    /**
     * Import a module by name.
     * 
     * @param name the name of the package to import
     * @param top if true, return the top module in the name, otherwise the last
     * @return an imported module (Java or Python)
     */
    public static PyObject importName(String name, boolean top) {
        return import_name(name, top, null, null);
    }

    /**
     * Import a module by name. This is the default call for
     * __builtin__.__import__.
     * 
     * @param name the name of the package to import
     * @param top if true, return the top module in the name, otherwise the last
     * @param modDict the __dict__ of an already imported module
     * @return an imported module (Java or Python)
     */
    public static PyObject importName(String name, boolean top, 
            PyObject modDict, PyObject fromlist) {
        importLock.lock();
        try {
            return import_name(name, top, modDict, fromlist);
        } finally {
            importLock.unlock();
        }
    }

    /**
     * Called from jython generated code when a statement like "import spam" is
     * executed.
     */
    public static PyObject importOne(String mod, PyFrame frame) {
        PyObject module = __builtin__.__import__(mod, frame.f_globals, frame
                .getLocals(), Py.EmptyTuple);
        /*
         * int dot = mod.indexOf('.'); if (dot != -1) { mod = mod.substring(0,
         * dot).intern(); }
         */
        return module;
    }

    /**
     * Called from jython generated code when a statement like "import spam as
     * foo" is executed.
     */
    public static PyObject importOneAs(String mod, PyFrame frame) {
        PyObject module = __builtin__.__import__(mod, frame.f_globals, frame
                .getLocals(), getStarArg());
        // frame.setlocal(asname, module);
        return module;
    }

    /**
     * Called from jython generated code when a statement like "from spam.eggs
     * import foo, bar" is executed.
     */
    public static PyObject[] importFrom(String mod, String[] names,
            PyFrame frame) {
        return importFromAs(mod, names, null, frame);
    }

    /**
     * Called from jython generated code when a statement like "from spam.eggs
     * import foo as spam" is executed.
     */
    public static PyObject[] importFromAs(String mod, String[] names,
            String[] asnames, PyFrame frame) {
        PyObject[] pyNames = new PyObject[names.length];
        for (int i = 0; i < names.length; i++) {
            pyNames[i] = Py.newString(names[i]);
        }

        PyObject module = __builtin__.__import__(mod, frame.f_globals, frame.getLocals(),
                                                 new PyTuple(pyNames));
        PyObject[] submods = new PyObject[names.length];
        for (int i = 0; i < names.length; i++) {
            PyObject submod = module.__findattr__(names[i]);
            if (submod == null) {
                throw Py.ImportError("cannot import name " + names[i]);
            }
            submods[i] = submod;
        }
        return submods;
    }

    private static PyTuple all = null;

    private synchronized static PyTuple getStarArg() {
        if (all == null) {
            all = new PyTuple(Py.newString('*'));
        }
        return all;
    }

    /**
     * Called from jython generated code when a statement like "from spam.eggs
     * import *" is executed.
     */
    public static void importAll(String mod, PyFrame frame) {
        PyObject module = __builtin__.__import__(mod, frame.f_globals, frame
                .getLocals(), getStarArg());
        PyObject names;
        boolean filter = true;
        if (module instanceof PyJavaPackage) {
            names = ((PyJavaPackage) module).fillDir();
        } else {
            PyObject __all__ = module.__findattr__("__all__");
            if (__all__ != null) {
                names = __all__;
                filter = false;
            } else {
                names = module.__dir__();
            }
        }

        loadNames(names, module, frame.getLocals(), filter);
    }

    /**
     * From a module, load the attributes found in <code>names</code> into
     * locals.
     * 
     * @param filter if true, if the name starts with an underscore '_' do not
     *            add it to locals
     * @param locals the namespace into which names will be loaded
     * @param names the names to load from the module
     * @param module the fully imported module
     */
    private static void loadNames(PyObject names, PyObject module,
            PyObject locals, boolean filter) {
        for (PyObject name : names.asIterable()) {
            String sname = ((PyString) name).internedString();
            if (filter && sname.startsWith("_")) {
                continue;
            } else {
                try {
                    PyObject value = module.__findattr__(sname);
                    if (value == null) {
                        PyObject nameObj = module.__findattr__("__name__");
                        if (nameObj != null) {
                            String submodName = nameObj.__str__().toString() + '.' + sname;
                            value = __builtin__.__import__(submodName, null, null,
                                                           nonEmptyFromlist);
                        }
                    }
                    locals.__setitem__(sname, value);
                } catch (Exception exc) {
                    continue;
                }
            }
        }
    }

    /* Reloading */
    static PyObject reload(PyJavaClass c) {
        // This is a dummy placeholder for the feature that allow
        // reloading of java classes. But this feature does not yet
        // work.
        return c;
    }

    static PyObject reload(PyModule m) {
        String name = m.__getattr__("__name__").toString().intern();

        PyObject modules = Py.getSystemState().modules;
        PyModule nm = (PyModule) modules.__finditem__(name);

        if (nm == null || !nm.__getattr__("__name__").toString().equals(name)) {
            throw Py.ImportError("reload(): module " + name
                    + " not in sys.modules");
        }

        PyList path = Py.getSystemState().path;
        String modName = name;
        int dot = name.lastIndexOf('.');
        if (dot != -1) {
            String iname = name.substring(0, dot).intern();
            PyObject pkg = modules.__finditem__(iname);
            if (pkg == null) {
                throw Py.ImportError("reload(): parent not in sys.modules");
            }
            path = (PyList) pkg.__getattr__("__path__");
            name = name.substring(dot + 1, name.length()).intern();
        }

        // This should be better "protected"
        // ((PyStringMap)nm.__dict__).clear();

        nm.__setattr__("__name__", new PyString(modName));
        PyObject ret = find_module(name, modName, path);
        modules.__setitem__(modName, ret);
        return ret;
    }
}
