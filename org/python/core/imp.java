// Copyright © Corporation for National Research Initiatives
package org.python.core;

import java.io.*;
import java.util.Hashtable;
import java.util.Properties;

public class imp
{
    public static final int APIVersion = 8;
    
    public static PyModule addModule(String name) {
        PyObject modules = Py.getSystemState().modules;
        PyModule module = (PyModule)modules.__finditem__(name);
        if (module != null) {
            return module;
        }
        module = new PyModule(name, null);
        modules.__setitem__(name, module);
        return module;
    }
    
    // Simplistic implementation
    // Some InputStream's might require multiple read's to get it all...
    private static byte[] readBytes(InputStream fp) {
        try {
            byte[] buf;
            buf = new byte[fp.available()];
            fp.read(buf);
            fp.close();
            return buf;
        } catch (IOException ioe) {
            throw Py.IOError(ioe);
        }
    }
    
    private static InputStream makeStream(File file) {
        try {
            return new FileInputStream(file);
        } catch (IOException ioe) {
            throw Py.IOError(ioe);
        }
    }

    private static PyObject createFromPyClass(String name, InputStream fp,
                                              boolean testing)
    {
        byte[] data = readBytes(fp);
        int n = data.length;
        //System.err.println("data: "+data[n-1]+", "+data[n-2]+", "+data[n-3]+", "+data[n-4]);
        int api = (data[n-4]<<24)+(data[n-3]<<16)+(data[n-2]<<8)+data[n-1];
        if (api != APIVersion) {
            if (testing) {
                return null;
            } else { 
                throw Py.ImportError("invalid api version("+api+" != "+
                                     APIVersion+") in: "+name);
            }
        }
        //System.err.println("APIVersion: "+api);
        PyCode code;
        try {
            code = BytecodeLoader.makeCode(name+"$py", data);
        } catch (Throwable t) {
            if (testing)
                return null;
            else
                throw Py.JavaError(t);
        }
        return createFromCode(name, code);
    }

    public static byte[] compileSource(String name, File file) {
        return compileSource(name, file, null, null);
    }
    
    public static byte[] compileSource(String name, File file, String filename,
                                       String outFilename)
    {
        if (filename == null) {
            filename = file.toString();
        }
        
        if (outFilename == null) {
            outFilename = filename.substring(0,filename.length()-3)+
                "$py.class";
        }
        
        return compileSource(name, makeStream(file), filename, outFilename);
    }

    static byte[] compileSource(String name, InputStream fp, String filename) {
        String outFilename = null;
        if (filename != null) {
            outFilename = filename.substring(0,filename.length()-3)+
                "$py.class";
        }
        return compileSource(name, fp, filename, outFilename);
    }
    
    static byte[] compileSource(String name, InputStream fp, String filename,
                                String outFilename)
    {
        try {
            ByteArrayOutputStream ofp = new ByteArrayOutputStream();
    
            if (filename == null)
                filename = "<unknown>";
            org.python.parser.SimpleNode node = null; //*Forte*
            try {
                node = parser.parse(fp, "exec", filename);
            } finally {
                fp.close();
            }
            org.python.compiler.Module.compile(node, ofp, name+"$py",
                                               filename, true, false, true);

            if (outFilename != null) {
                File classFile = new File(outFilename);
                try {
                    FileOutputStream fop = new FileOutputStream(classFile);
                    ofp.writeTo(fop);
                    fop.close();
                } catch (IOException exc) {
                    // If we can't write the cache file, just fail silently
                }
            }

            return ofp.toByteArray();
        } catch (Throwable t) {
            throw parser.fixParseError(null, t, filename);
        }
    }

    private static PyObject createFromSource(String name, InputStream fp,
                                             String filename)
    {
        byte[] bytes = compileSource(name, fp, filename);
        PyCode code = BytecodeLoader.makeCode(name+"$py", bytes);
        return createFromCode(name, code);
    }

    static PyObject createFromCode(String name, PyCode c) {
        PyModule module = addModule(name);

        PyTableCode code = null;
        if (c instanceof PyTableCode)
            code = (PyTableCode)c;
        PyFrame f = new PyFrame(code, module.__dict__, module.__dict__, null);
        code.call(f);

        return module;
    }

    private static BytecodeLoader syspathJavaLoader = null;

    public static synchronized BytecodeLoader getSyspathJavaLoader() {
        if (syspathJavaLoader == null)
            syspathJavaLoader = new BytecodeLoader();
        return syspathJavaLoader;
    }

    private static PyObject createFromClass(String name, InputStream fp) {
        BytecodeLoader bcl = getSyspathJavaLoader();
        return createFromClass(name, bcl.makeClass(name, readBytes(fp)));
    }

    private static PyObject createFromClass(String name, Class c) {
        //Two choices.  c implements PyRunnable or c is Java package
        //System.err.println("create from class: "+name+", "+c);
        Class interfaces[] = c.getInterfaces();
        for (int i=0; i<interfaces.length; i++) {
            if (interfaces[i] == PyRunnable.class) {
                //System.err.println("is runnable");
                try {
                    PyObject o = createFromCode(
                        name, ((PyRunnable)c.newInstance()).getMain());
                    return o;
                } catch (InstantiationException e) {
                    throw Py.JavaError(e);
                } catch (IllegalAccessException e) {
                    throw Py.JavaError(e);
                }
            }
        }
        return PyJavaClass.lookup(c);
    }

    private static PyObject loadBuiltin(String name, PyList path) {
        if (name == "sys")
            return Py.java2py(Py.getSystemState());
        String mod = PySystemState.getBuiltin(name);
        if (mod != null) {
            Class c = Py.findClassEx(mod);
            if (c != null) {
                try {
                    return createFromClass(name, c);
                }
                catch (NoClassDefFoundError e) {
                    throw Py.ImportError("Cannot import " + name +
                                         ", missing class " +
                                         c.getName());
                }
            }
        }
        return null;
    }

    private static Class findPyClass(String modName) {
        if (Py.frozenPackage != null) {
            modName = Py.frozenPackage+"."+modName;
        }
        return Py.findClassEx(modName+"$_PyInner");
    }

    private static PyObject loadPrecompiled(String name, String modName,
                                            PyList path)
    {
        if (Py.frozenModules != null) {
            //System.out.println("precomp: "+name+", "+modName);
            Class c;

            if (Py.frozenModules.get(modName+".__init__") != null) {
                //System.err.println("trying: "+modName+".__init__$_PyInner");
                c = findPyClass(modName+".__init__");
                if (c == null) return null;
                //System.err.println("found: "+modName+".__init__$_PyInner");
                PyModule m = addModule(modName);
                m.__dict__.__setitem__("__path__", new PyList());
            }
            else if (Py.frozenModules.get(modName) != null) {
                c = findPyClass(modName);
                if (c == null) return null;
            }
            else return null;

            //System.err.println("creating: "+modName+", "+c);
            return createFromClass(modName, c);
        }
        return null;
    }

    static PyObject loadFromPath(String name, PyList path) {
        return loadFromPath(name, name, path);
    }

    static PyObject loadFromPath(String name, String modName, PyList path) {
        //System.err.println("load-from-path:"+name+" "+modName+" "+path); // ?? dbg
        PyObject o = loadPrecompiled(name, modName, path);
        if (o != null) return o;

        String pyName = name+".py";
        String className = name+"$py.class";

        int n = path.__len__();

        for (int i=0; i<n; i++) {
            String dirName = path.get(i).toString();

            // TBD: probably should tie this into -v option a la CPython
            if (dirName.endsWith(".jar") || dirName.endsWith(".zip")) {
                // Handle .jar and .zip files on the path sometime in the
                // future
                continue;
            }

            // The empty string translates into the current working
            // directory, which is usually provided on the system property
            // "user.dir".  Don't rely on File's constructor to provide
            // this correctly.
            if (dirName.length() == 0) {
                dirName = null;
            }

            // First check for packages
            File dir = new File(dirName, name);
            if (dir.isDirectory() && 
                (new File(dir, "__init__.py").isFile() || 
                 new File(dir, "__init__$py.class").isFile()))
            {
                PyList pkgPath = new PyList();
                PyModule m = addModule(modName);
                pkgPath.append(new PyString(dir.toString()));
                m.__dict__.__setitem__("__path__", pkgPath);
                o = loadFromPath("__init__", modName, pkgPath);
                if (o == null)
                    continue;
                return m;
            }

            // Now check for source
            File pyFile = new File(dirName, pyName);
            File classFile = new File(dirName, className);
            if (pyFile.isFile()) {
                if (classFile.isFile()) {
                    long pyTime = pyFile.lastModified();
                    long classTime = classFile.lastModified();
                    if (classTime >= pyTime) {
                        PyObject ret = createFromPyClass(
                            modName, makeStream(classFile), true);
                        if (ret != null)
                            return ret;
                    }
                }
                return createFromSource(modName, makeStream(pyFile),
                                        pyFile.getAbsolutePath());
            }

            // If no source, try loading precompiled
            if (classFile.isFile()) {
                return createFromPyClass(modName, makeStream(classFile),
                                         false);
            }

        }
        return null;
    }

    static PyObject loadFromClassLoader(String name, ClassLoader classLoader) {
        PyObject ret;
        String path = name.replace('.', '/');
        InputStream istream;

        // First check to see if a package exists (look for name/__init__.py)
        boolean loadCompiled = false;
        boolean loadSource = false;

        istream = classLoader.getResourceAsStream(path+"/__init__.py");
        if (istream != null) {
            PyModule m = addModule(name);
            m.__dict__.__setitem__("__path__", Py.None);
            return createFromSource(name, istream, null);
        }

        // Finally, try to load from source
        istream = classLoader.getResourceAsStream(path+".py");
        if (istream != null) return createFromSource(name, istream, null);

        return null;
    }

    private static PyObject load(String name, PyList path) {
        PyObject ret = loadBuiltin(name, path);
        if (ret != null) return ret;
        
        ret = loadFromPath(name, path);
        if (ret != null) return ret;

        ret = PySystemState.packageManager.lookupName(name);
        if (ret != null) return ret;

        throw Py.ImportError("no module named "+name);
    }

    public static PyObject load(String name) {
        PyObject modules = Py.getSystemState().modules;
        PyObject ret = modules.__finditem__(name);
        if (ret != null) return ret;

        ret = load(name, Py.getSystemState().path);
        if (modules.__finditem__(name) == null)
            modules.__setitem__(name, ret);
        else
            ret = modules.__finditem__(name);
        return ret;
    }

    private static String getParent(PyObject dict) {
        PyObject tmp = dict.__finditem__("__name__");
        if (tmp == null) return null;
        String name = tmp.toString();
        
        tmp = dict.__finditem__("__path__");
        if (tmp != null && tmp instanceof PyList) {
            return name.intern();
        } else {
            int dot = name.lastIndexOf('.');
            if (dot == -1) return null;
            return name.substring(0, dot).intern();
        }
    }

    // Hierarchy-recursivly search for dotted name in mod.
    private static PyObject dottedFind(PyObject mod, String name) {
      int dot = 0;
      int last_dot= 0;
      do {
        String tmpName;
        dot = name.indexOf('.', last_dot);
        if (dot == -1) {
          tmpName = name.substring(last_dot).intern();
        } else {
          tmpName = name.substring(last_dot, dot).intern();
        }
        mod = mod.__findattr__(tmpName);
        if (mod == null)
          throw Py.ImportError("No module named " + tmpName);
        last_dot = dot + 1;
      } while (dot != -1);
      return mod;
    }
    
    public static PyObject importName(String name, boolean top) {
        if (name.length() == 0)
            throw Py.ValueError("Empty module name");
        int dot = name.indexOf('.');
        if (dot != -1) {
            PyObject modules = Py.getSystemState().modules;             
            PyObject mod = modules.__finditem__(name);
            if (mod != null && !top)
                return mod;

            int last_dot = dot;
            String firstName = name.substring(0,dot).intern();
            PyObject pkg = load(firstName);

            if (mod == null) {
                mod = pkg;
                if (dot != -1) mod = dottedFind(mod, name.substring(dot+1));
            }
            if (modules.__finditem__(name) == null)
                modules.__setitem__(name, mod);
            else
                mod = modules.__finditem__(name);
            if (top)
                return pkg;
            else
                return mod;
        }
        else return load(name);
    }

    // This version should deal properly with package relative imports.
    // Assumption (runtime enforced):
    // x.y.z key in sys.modules => any subseq (e.g x, x.y) is a present key too.
    // ??pending: check if result is really a module/jpkg/jclass?
    public synchronized static PyObject importName(String name, boolean top,
    PyObject modDict)
    {
        //System.err.println("importName: "+name);
        String pkgName = getParent(modDict);
        PyObject mod;

        if (pkgName != null) {
            PyObject modules = Py.getSystemState().modules;
            int dot = name.indexOf('.');
            String firstName;
            if (dot == -1) firstName = name;
            else firstName = name.substring(0,dot);

            String topNewName = (pkgName+'.'+firstName).intern();

            PyObject topMod = modules.__finditem__(topNewName);

            if (topMod != Py.None) {
                if (dot == -1 && topMod != null) {
                    //System.err.println("refound-1-top: "+topMod); // ?? dbg
                    return topMod;
                }
                String newName = (pkgName+'.'+name).intern();
                mod = modules.__finditem__(newName);
                if (mod != null) {
                    //System.err.println("refound: "+name); // ?? dbg
                    if (!top) return mod;
                    else return topMod;
                }

                PyObject pkg = modules.__finditem__(pkgName);
                if (pkg != null) {
                    topMod = pkg.__findattr__(firstName.intern());
                    if (topMod != null ) {
                        if (dot == -1 ) {
                            //System.err.println("found-1-top: "+topMod); // ?? dbg
                            return topMod;
                        }

                        //System.err.println(".-find: "+topMod+","+name.substring(dot+1)); // ?? dbg
                        mod = dottedFind(topMod,name.substring(dot+1));

                        if(top) return topMod;
                        else return mod;

                    }

                }

                //System.err.println("mark: "+topNewName); // ?? dbg
                modules.__setitem__(topNewName,Py.None);
            }
        }

        return importName(name, top);
}
            
    /**
     * Called from jpython generated code when a statement like "import spam"
     * is executed.
     */
    public static void importOne(String mod, PyFrame frame) {
        //System.out.println("importOne(" + mod + ")");
        PyObject module = getImportFunc(frame).__call__(
            new PyObject[] {
                Py.newString(mod),
                frame.f_globals,
                frame.f_locals,
                Py.EmptyTuple
            });

        int dot = mod.indexOf('.');
        if (dot != -1) {
            mod = mod.substring(0, dot).intern();
        }
        //System.err.println("mod: "+mod+", "+dot);
        frame.setlocal(mod, module);
    }

    /**
     * Called from jpython generated code when a statement like 
     * "import spam as foo" is executed.
     */
    public static void importOneAs(String mod, String asname, PyFrame frame) {
        //System.out.println("importOne(" + mod + ")");
        PyObject module = getImportFunc(frame).__call__(
            new PyObject[] {
                Py.newString(mod),
                frame.f_globals,
                frame.f_locals,
                getStarArg()
            });

        frame.setlocal(asname, module);
    }
        
    /**
     * Called from jpython generated code when a stamenet like 
     * "from spam.eggs import foo, bar" is executed.
     */
    public static void importFrom(String mod, String[] names, PyFrame frame) {
        importFromAs(mod, names, names, frame);
    }

    /**
     * Called from jpython generated code when a stamenet like 
     * "from spam.eggs import foo as spam" is executed.
     */
    public static void importFromAs(String mod, String[] names, String[] asnames,
                         PyFrame frame) {
        //StringBuffer sb = new StringBuffer();
        //for(int i=0; i<names.length; i++)
        //    sb.append(names[i] + " ");
        //System.out.println("importFrom(" + mod + ", [" + sb + "]");

        PyObject[] pynames = new PyObject[names.length];
        for (int i=0; i<names.length; i++)
            pynames[i] = Py.newString(names[i]);

        PyObject module = getImportFunc(frame).__call__(
            new PyObject[] {
                Py.newString(mod),
                frame.f_globals,
                frame.f_locals,
                new PyTuple(pynames)
            });
        for (int i=0; i<names.length; i++) {
            PyObject submod = module.__findattr__(names[i]);
            if (submod == null)
                throw Py.ImportError("cannot import name " + names[i]);
            frame.setlocal(asnames[i], submod);
        }
    }

    private static PyTuple all = null;

    private synchronized static PyTuple getStarArg() {
        if (all == null)
            all = new PyTuple(new PyString[] { Py.newString('*') });
        return all;
    }

    /**
     * Called from jpython generated code when a statement like 
     * "from spam.eggs import *" is executed.
     */
    public static void importAll(String mod, PyFrame frame) {
        //System.out.println("importAll(" + mod + ")");
        PyObject module = getImportFunc(frame).__call__(new PyObject[] {
                Py.newString(mod),
                frame.f_globals,
                frame.f_locals,
                getStarArg() } );
        PyObject names;
        if (module instanceof PyJavaPackage) names = ((PyJavaPackage)module).fillDir();
        else names = module.__dir__();
                          
        loadNames(names, module, frame.getf_locals());
    }

    // if __all__ is present, things work properly under the assumption that names is sorted (__*__ names come first)
    private static void loadNames(PyObject names, PyObject module,
                                  PyObject locals)
    {
        int i=0;
        PyObject name;
        while ((name=names.__finditem__(i++)) != null) {
            String sname = ((PyString)name).internedString();
            if (sname == "__all__") {
                loadNames(module.__findattr__("__all__"), module, locals);
            } else if (sname.startsWith("__")) {
                continue;
            } else {
                try {
                    locals.__setitem__(sname, module.__getattr__(sname));
                } catch (Exception exc) {
                    continue;
                }
            }
        }
    }

    static PyObject reload(PyJavaClass c) {
        // This is a dummy placeholder for the feature that allow
        // reloading of java classes. But this feature does not yet
        // work.
        return c;
    }

    static PyObject reload(PyModule m) {
        String name = m.__getattr__("__name__").toString().intern();

        PyObject modules = Py.getSystemState().modules;         
        PyModule nm = (PyModule)modules.__finditem__(name);

        if (nm == null || !nm.__getattr__("__name__").toString().equals(name)) {
            throw Py.ImportError("reload(): module "+name+
                                 " not in sys.modules");
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
            path = (PyList)pkg.__getattr__("__path__");
            name = name.substring(dot+1, name.length()).intern();
        }
        
        // This should be better "protected"
        //((PyStringMap)nm.__dict__).clear();
        
        nm.__setattr__("__name__", new PyString(modName));
        PyObject ret = loadFromPath(name, modName, path);
        modules.__setitem__(modName, ret);
        return ret;
    }

    private static PyObject __import__ = null;

    private static PyObject getImportFunc(PyFrame frame) {
        if (__import__ == null)
            __import__ = Py.newString("__import__");
        // Set up f_builtins if not already set
        PyObject builtins = frame.f_builtins;
        if (builtins == null)
            builtins = Py.getSystemState().builtins;
        return builtins.__getitem__(__import__);
    }
}
