package org.python.util;
import org.python.core.*;

public class jpython {
    private static String usage = "jpython [-i] [filename | -] [args]*";

    public static void runJar(String filename) {
        return;
        /*
        try {
            ZipFile zip = new ZipFile(filename);

            ZipEntry runit = zip.getEntry("__run__.py");
            if (runit == null) throw Py.ValueError("jar file missing '__run__.py'");

    		PyDictionary locals = new PyDictionary();
    		locals.__setitem__(new PyString("__name__"), new PyString(filename));
    		locals.__setitem__(new PyString("zipfile"), Py.java2py(zip));

		    InputStream file = zip.getInputStream(runit);
            PyCode code;
		    try {
		        code = Py.compile(file, "__run__", "exec");
		    } finally {
			    file.close();
			}
			Py.runCode(code, locals, locals);
		} catch (java.io.IOException e) {
			throw Py.IOError(e);
		}*/
	}

    public static void main(String[] args) {
	PythonInterpreter interp = new PythonInterpreter();
	//System.err.println("new interp created");
        PyModule mod = imp.addModule("__main__");
        interp.setLocals(mod.__dict__);

        CommandLineOptions opts = new CommandLineOptions();

        if (!opts.parse(args)) {
            System.err.println(usage);
            System.exit(-1);
        }

	if (opts.filename != null) {
	    String path = new java.io.File(opts.filename).getParent();
	    if (path == null) path = "";
            Py.getSystemState().path.insert(0, new PyString(path));
            if (opts.jar) {
                runJar(opts.filename);
		/*} else if (opts.filename.equals("-")) {
		  try {
		  PyCode code = Py.compile(System.in, "<stdin>", "exec");
		  Py.runCode(code, locals, locals);
		  } catch (Throwable t) {
		  Py.printException(t);
		  }*/
            } else {
		try {
		    interp.execfile(opts.filename);
		} catch (Throwable t) {
		    Py.printException(t);
		}
	    }
	}

	if (opts.interactive) {
	    interp.interact(opts.notice ? null : "");
	}
    }
}

class CommandLineOptions {
    public String filename;
    public boolean jar, interactive, notice;
    private boolean fixInteractive;
    public PyList argv;
    private java.util.Properties registry;

    public CommandLineOptions() {
        filename=null;
        jar = fixInteractive = false;
        interactive = notice = true;
        registry = Py.getSystemState().registry;
    }

    public void setProperty(String key, String value) {
        registry.put(key, value);
    }

    public boolean parse(String[] args) {
        int index=0;
        while (index < args.length && args[index].startsWith("-")) {
            String arg = args[index];
            if (arg.equals("-")) {
                if (!fixInteractive) interactive = false;
                filename = "-";
            } else if (arg.equals("-i")) {
                fixInteractive = true;
                interactive = true;
            } else if (arg.equals("-jar")) {
                jar = true;
                if (!fixInteractive) interactive = false;
            } else if (arg.startsWith("-D")) {
                String key = null; 
                String value = null;
                int equals = arg.indexOf("=");
                if (equals == -1) {
                    String arg2 = args[++index];
                    /*if (!arg2.startsWith("=")) {
		      System.err.println("-D option with no '=': "+args[index-1]+"::"+arg2);
		      return false;
		      }*/
                    key = arg.substring(2, arg.length());
                    value = arg2; //.substring(1, arg2.length());
                } else {
                    key = arg.substring(2, equals);
                    value = arg.substring(equals+1, arg.length());
                }
                setProperty(key, value);
            } else {
                System.err.println("Unknown option: "+args[index]);
                return false;
            }
            index += 1;
        }
        notice = interactive;
        if (filename == null && index < args.length) {
            filename = args[index++];
            if (!fixInteractive) interactive = false;
            notice = false;
        }

        argv = new PyList();
        //new String[args.length-index+1];
        if (filename != null) argv.append(new PyString(filename));
        else argv.append(new PyString(""));

        for(int i=0; index<args.length; i++, index++) {
            argv.append(new PyString(args[index]));
        }

        Py.getSystemState().setOptionsFromRegistry();
        Py.getSystemState().argv = argv;

        return true;
    }
}

