package org.python.util;
import org.python.core.*;
import java.util.zip.*;
import java.io.*;

public class jpython {
    private static String usage = "jpython [-i] [filename | -] [args]*";

    public static void runJar(String filename) {
        try {
            ZipFile zip = new ZipFile(filename);

            ZipEntry runit = zip.getEntry("__run__.py");
            if (runit == null) throw Py.ValueError("jar file missing '__run__.py'");

                PyStringMap locals = new PyStringMap();
                locals.__setitem__("__name__", new PyString(filename));
                locals.__setitem__("zipfile", Py.java2py(zip));

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
         }
    }

    public static void main(String[] args) {
        // Parse the command line options
        CommandLineOptions opts = new CommandLineOptions();
        if (!opts.parse(args)) {
            System.err.println(usage);
            System.exit(-1);
        }
        
        // Setup the basic python system state from these options
        PySystemState.initialize(System.getProperties(), opts.properties, opts.argv);
        
        if (opts.notice) {
            System.err.println(InteractiveConsole.getDefaultBanner());
        }
        
        // Now create an interpreter
        InteractiveConsole interp = new InteractiveConsole();
        //System.err.println("interp");
        PyModule mod = imp.addModule("__main__");
        interp.setLocals(mod.__dict__);
        //System.err.println("imp");

        if (Options.importSite) {
            try {
                imp.load("site");
            } catch (PyException pye) {
                if (!Py.matchException(pye, Py.ImportError)) {
                    System.err.println("error importing site");
                    Py.printException(pye);
                    System.exit(-1);
                }
            }
        }
 
        if (opts.command != null) {
            try {
              interp.exec(opts.command);
            } catch (Throwable t) {
              Py.printException(t);
            }
        }
 
        if (opts.filename != null) {
            String path = new java.io.File(opts.filename).getParent();
            if (path == null) path = "";
            Py.getSystemState().path.insert(0, new PyString(path));
            if (opts.jar) {
                runJar(opts.filename);
            } else if (opts.filename.equals("-")) {
                try {
                  interp.execfile(System.in, "<stdin>");
                } catch (Throwable t) {
                  Py.printException(t);
                }
            } else {
                try {
                    interp.execfile(opts.filename);
                } catch (Throwable t) {
                    Py.printException(t);
                }
            }
        }

        if (opts.interactive) {
            interp.interact(null);
        }
    }
}

class CommandLineOptions {
    public String filename;
    public boolean jar, interactive, notice;
    private boolean fixInteractive;
    public String[] argv;
    public java.util.Properties properties;
    public String command;

    public CommandLineOptions() {
        filename=null;
        jar = fixInteractive = false;
        interactive = notice = true;
        properties = new java.util.Properties();
    }

    public void setProperty(String key, String value) {
        properties.put(key, value);
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
            } else if (arg.equals("-X")) {
                Options.classBasedExceptions = false;
            } else if (arg.equals("-S")) {
                Options.importSite = false;
            } else if (arg.equals("-c")) {
                command = args[++index];
                if (!fixInteractive) interactive = false;              
                break;
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
        if (filename == null && index < args.length && command == null) {
            filename = args[index++];
            if (!fixInteractive) interactive = false;
            notice = false;
        }
        if (command != null) notice = false;

        int n = args.length-index+1;
        argv = new String[n];
        //new String[args.length-index+1];
        if (filename != null) argv[0] = filename;
        else argv[0] = "";

        for(int i=1; i<n; i++, index++) {
            argv[i] = args[index];
        }

        return true;
    }
}

