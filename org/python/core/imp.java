package org.python.core;

import java.io.*;
import java.util.Hashtable;
import java.util.Properties;

public class imp {
    private static String[] builtinNames = new String[] {
        "jarray", "math", "thread", "operator", "time",
        "os", "types", "py_compile", "codeop", "re", "code",
        "synchronize", "cPickle", "cStringIO",
    };
    
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

    private static PyObject createFromPyClass(String name, InputStream fp, boolean testing) {
        byte[] data = readBytes(fp);
        int n = data.length;
        //System.err.println("data: "+data[n-1]+", "+data[n-2]+", "+data[n-3]+", "+data[n-4]);
        int api = (data[n-4]<<24)+(data[n-3]<<16)+(data[n-2]<<8)+data[n-1];
        if (api != APIVersion) {
            if (testing) {
                return null;
            } else { 
                throw Py.ImportError("invalid api version("+api+" != "+APIVersion+") in: "+name);
            }
        }
        //System.err.println("APIVersion: "+api);
        PyCode code;
        try {
            code = BytecodeLoader.makeCode(name+"$py", data);
        } catch (Throwable t) {
            BytecodeLoader.clearLoader();
            if (testing) return null;
            else throw Py.JavaError(t);
        }
        return createFromCode(name, code);
    }

    public static byte[] compileSource(String name, File file) {
        return compileSource(name, file, null, null);
    }
    
    public static byte[] compileSource(String name, File file, String filename, String outFilename) {
        if (filename == null) {
            filename = file.toString();
        }
        
        if (outFilename == null) {
            outFilename = filename.substring(0,filename.length()-3)+"$py.class";
        }
        
        return compileSource(name, makeStream(file), filename, outFilename);
    }

    static byte[] compileSource(String name, InputStream fp, String filename) {
        String outFilename = null;
        if (filename != null) {
            outFilename = filename.substring(0,filename.length()-3)+"$py.class";
        }
        return compileSource(name, fp, filename, outFilename);
    }
    
    static byte[] compileSource(String name, InputStream fp, String filename, String outFilename) {
	try {
	    ByteArrayOutputStream ofp = new ByteArrayOutputStream();
    
            if (filename == null) filename = "<unknown>";
	    org.python.parser.SimpleNode node;
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
	} catch (Exception e) {
	    throw Py.JavaError(e);
	}
    }

    private static PyObject createFromSource(String name, InputStream fp, String filename) {
	byte[] bytes = compileSource(name, fp, filename);
	PyCode code = BytecodeLoader.makeCode(name+"$py", bytes);
	return createFromCode(name, code);
    }

    static PyObject createFromCode(String name, PyCode c) {
	PyModule module = addModule(name);

	PyTableCode code = null;
	if (c instanceof PyTableCode) code = (PyTableCode)c;
	PyFrame f = new PyFrame(code, module.__dict__, module.__dict__, null);
	code.call(f);

	return module;
    }


    private static PyObject createFromClass(String name, InputStream fp) {
        return createFromClass(name, BytecodeLoader.makeClass(name, readBytes(fp)));
    }

    private static PyObject createFromClass(String name, Class c) {
	//Two choices.  c implements PyRunnable or c is Java package
	Class interfaces[] = c.getInterfaces();
	for (int i=0; i<interfaces.length; i++) {
	    if (interfaces[i] == PyRunnable.class) {
		try {
		    PyObject o = createFromCode(name, ((PyRunnable)c.newInstance()).getMain());
		    if (o.__findattr__("__path__") != null) {
			Class ci = Py.findClass(name+".__init__$py");
			if (ci == null) return null;
			createFromCode(name, ((PyRunnable)ci.newInstance()).getMain());
		    }
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
	
    private static Hashtable builtins;
    private static String getBuiltin(String name) {
        if (builtins == null) {
            Hashtable t = new Hashtable();
            t.put("__builtin__", "org.python.core.__builtin__");
            t.put("sys", "org.python.core.sys");
            for(int i=0; i<builtinNames.length; i++) {
                t.put(builtinNames[i], "org.python.modules."+builtinNames[i]);
            }
            builtins = t;
        }

        return (String)builtins.get(name);
    }

    private static PyObject loadBuiltin(String name, PyList path) {
	if (name == "sys") {
	    return Py.java2py(Py.getSystemState());
	}
	String mod = getBuiltin(name);
	if (mod != null) {
            Class c = Py.findClass(mod);
            if (c != null)
		return createFromClass(name, c);
	}
	return null;
    }

    private static PyObject loadPrecompiled(String name, String modName, PyList path) {
        //System.out.println("precomp: "+name+", "+modName);
        Class c = Py.findClass(modName+"$py");
        if (c == null) return null;
        return createFromClass(modName, c);
    }

    static PyObject loadFromPath(String name, PyList path) {
        return loadFromPath(name, name, path);
    }

    static PyObject loadFromPath(String name, String modName, PyList path) {
	if (Py.frozen) return loadPrecompiled(name, modName, path);

	String pyName = name+".py";
	String className = name+"$py.class";
	String javaName = name+".class";

	int n = path.__len__();

	for (int i=0; i<n; i++) {
	    String dirName = path.get(i).toString();
	    if (dirName.endsWith(".jar") || dirName.endsWith(".zip")) {
		// Handle .jar and .zip files on the path
		// Sometime in the future
		continue;
	    }

	    // First check for packages
	    File dir = new File(dirName, name);
	    if (dir.isDirectory() && 
		(new File(dir, "__init__.py").isFile() || 
		 new File(dir, "__init__$py.class").isFile())) {
		PyList pkgPath = new PyList();
		PyModule m = addModule(modName);
		pkgPath.append(new PyString(dir.toString()));
		m.__dict__.__setitem__("__path__", pkgPath);
		PyObject o = loadFromPath("__init__", modName, pkgPath);
		if (o == null) continue;

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
			PyObject ret = createFromPyClass(modName, makeStream(classFile), true);
			if (ret != null) return ret;
		    }
		}
		return createFromSource(modName, makeStream(pyFile), pyFile.getAbsolutePath());
	    }

	    // If no source, try loading precompiled
	    if (classFile.isFile()) {
		return createFromPyClass(modName, makeStream(classFile), false);
	    }
			
	    File javaFile = new File(dirName, javaName);
	    if (javaFile.isFile()) {
		try {
		    return createFromClass(modName, makeStream(javaFile));
		} catch (ClassFormatError exc) {
		    throw Py.ImportError("bad java class file in: "+javaFile.toString());
		}
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
	//System.out.println("load: "+name);
	PyObject ret = loadBuiltin(name, path);
	if (ret != null) return ret;

        ClassLoader classLoader=null;
        if (!Py.frozen) classLoader = Py.getSystemState().getClassLoader();
	//System.out.println("load1: "+classLoader);

        if (classLoader != null) {
            try {
                ret = loadFromClassLoader(name, classLoader);
                if (ret != null) return ret;
            } catch (Throwable t) {
                t.printStackTrace(System.err);
            }
        }
	//System.out.println("load2: ");

	ret = loadFromPath(name, path);
	if (ret != null) return ret;
	//System.out.println("load3: ");

        ret = PySystemState.packageManager.findName(name);
        if (ret != null) return ret;

        //Class c = Py.findClass(name);
        //if (c != null) return createFromClass(name, c);

	throw Py.ImportError("no module named "+name);
    }

    public static PyObject load(String name) {
	PyObject modules = Py.getSystemState().modules;
	PyObject ret = modules.__finditem__(name);
	if (ret != null) return ret;

	ret = load(name, Py.getSystemState().path);
	modules.__setitem__(name, ret);
	return ret;
    }

    public static PyObject importName(String name, boolean top) {
	// Is this really needed any more?
	/*if (sys.registry == null) sys.registry = sys.initRegistry();*/

	int dot = name.indexOf('.');
	if (dot != -1) {
	    PyObject modules = Py.getSystemState().modules;		
	    PyObject mod = modules.__finditem__(name);
	    if (mod != null && !top) return mod;

	    int last_dot = dot;
	    String firstName = name.substring(0,dot).intern();
	    PyObject pkg = load(firstName);

	    if (mod == null) {
		mod = pkg;
		while (dot != -1) {
		    String tmpName;
		    dot = name.indexOf('.', last_dot+1);
		    if (dot == -1) {
			tmpName = name.substring(last_dot+1, name.length()).intern();
		    } else {
			tmpName = name.substring(last_dot+1, dot).intern();
		    }
		    mod = mod.__getattr__(tmpName);
		    last_dot = dot;
		}
	    }
	    modules.__setitem__(name, mod);
	    if (top) return pkg;
	    else return mod;
	} else {
	    return load(name);
	}
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

    public synchronized static PyObject importName(String name, boolean top, PyObject modDict) {
	//System.err.println("importName: "+name);
	String pkgName = getParent(modDict);
	PyObject ret;

	if (pkgName != null) {
	    PyObject modules = Py.getSystemState().modules;		
	    String newName = (pkgName+'.'+name).intern();
	    ret = modules.__finditem__(newName);
	    if (ret != null) return ret;

	    PyObject pkg = modules.__finditem__(pkgName);
	    if (pkg != null) {
		ret = pkg.__findattr__(name);
		if (ret != null) return ret;
	    }

	    ret = importName(name, top);
	    modules.__setitem__(newName, ret);
	    return ret;
	}
	//System.err.println("done importName: "+name);
	return importName(name, top);
    }

    public static void importOne(String mod, PyFrame frame) {
	PyObject module = importName(mod, true, frame.f_globals);
	int dot = mod.indexOf('.');
	if (dot != -1) {
	    mod = mod.substring(0, dot).intern();
	}
	//System.err.println("mod: "+mod+", "+dot);
	frame.setlocal(mod, module);
    }
	
    public static void importFrom(String mod, String[] names, PyFrame frame) {
	PyObject module = importName(mod, false, frame.f_globals);
	for(int i=0; i<names.length; i++) {
	    frame.setlocal(names[i], module.__getattr__(names[i]));
	}
    }	

    private static void loadNames(PyObject names, PyObject module, PyObject locals) {
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
		

    public static void importAll(String mod, PyFrame frame) {
	PyObject module = importName(mod, false, frame.f_globals);
	PyObject locals = frame.getf_locals();

	loadNames(module.__dir__(), module, frame.getf_locals());
    }


    static PyObject reload(PyJavaClass c) {
        PyObject modules = Py.getSystemState().modules;		
        modules.__delitem__(c.__name__);
        // Should delete from package if in one
        String name = c.__name__;
        int dot = name.lastIndexOf('.');
        if (dot != -1) {
            PyObject pkg = modules.__finditem__(name.substring(0, dot).intern());
            if (pkg == null) {
                throw Py.ImportError("reload(): parent not in sys.modules");
            }
            name = name.substring(dot+1, name.length()).intern();
            pkg.__delattr__(name);
        }        
        BytecodeLoader.clearLoader();
        PyObject nc = importName(c.__name__, false, null);
        return nc;
    }

    static PyObject reload(PyModule m) {
        String name = m.__getattr__("__name__").toString().intern();

        PyObject modules = Py.getSystemState().modules;		
        PyModule nm = (PyModule)modules.__finditem__(name);

        if (!nm.__getattr__("__name__").toString().equals(name)) {
            throw Py.ImportError("reload(): module "+name+" not in sys.modules");
        }

        PyList path = Py.getSystemState().path;
        String modName = name;
        int dot = name.lastIndexOf('.');
        if (dot != -1) {
            PyObject pkg = modules.__finditem__(name.substring(0, dot).intern());
            if (pkg == null) {
                throw Py.ImportError("reload(): parent not in sys.modules");
            }
            path = (PyList)pkg.__getattr__("__path__");
            name = name.substring(dot+1, name.length()).intern();
        }
        
        // This should be better "protected"
        ((PyStringMap)nm.__dict__).clear();
        
        nm.__setattr__("__name__", new PyString(name));
        BytecodeLoader.clearLoader();

	PyObject ret = loadFromPath(name, modName, path);

	modules.__setitem__(name, ret);
	return ret;
    }
}
