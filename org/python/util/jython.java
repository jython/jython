// Copyright © Corporation for National Research Initiatives
package org.python.util;

import org.python.core.*;
import java.util.zip.*;
import java.io.*;

public class jpython
{
    private static String usage =
        "usage: jython [options] [-jar jar | -c cmd | file | -] [args]\n"+
        "Options and arguments:\n"+
        "-i       : inspect interactively after running script, and force\n"+
        "           prompts, even if stdin does not appear to be a terminal\n"+
        "-S       : don't imply `import site' on initialization\n"+
        "-X       : disable class based standard exceptions\n"+
        "-Dprop=v : Set the property `prop' to value `v'\n"+
        "-jar jar : program read from __run__.py in jar file\n"+
        "-c cmd   : program passed in as string (terminates option list)\n"+
        "file     : program read from script file\n"+
        "-        : program read from stdin (default; interactive mode if a "+
        "tty)\n"+
        "--help   : print this usage message and exit\n"+
        "--version: print Jython version number and exit\n"+
        "args     : arguments passed to program in sys.argv[1:]";

    public static void runJar(String filename) {
        // TBD: this is kind of gross because a local called `zipfile' just
        // magically shows up in the module's globals.  Either `zipfile'
        // should be called `__zipfile__' or (preferrably, IMO), __run__.py 
        // should be imported and a main() function extracted.  This
        // function should be called passing zipfile in as an argument.
        //
        // Probably have to keep this code around for backwards
        // compatibility (?)
        try {
            ZipFile zip = new ZipFile(filename);

            ZipEntry runit = zip.getEntry("__run__.py");
            if (runit == null)
                throw Py.ValueError("jar file missing '__run__.py'");

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
            if (opts.version) {
                System.err.println(InteractiveConsole.getDefaultBanner());
                System.exit(0);
            }
            System.err.println(usage);
            int exitcode = opts.help ? 0 : -1;
            System.exit(exitcode);
        }
        
        // Setup the basic python system state from these options
        PySystemState.initialize(System.getProperties(),
                                 opts.properties, opts.argv);
        
        if (opts.notice) {
            System.err.println(InteractiveConsole.getDefaultBanner());
        }
        
        // Now create an interpreter
        InteractiveConsole interp = null;
        try {
            String interpClass = PySystemState.registry.getProperty(
                                    "python.console",
                                    "org.python.util.InteractiveConsole");
            interp = (InteractiveConsole)
                             Class.forName(interpClass).newInstance();
        } catch (Exception e) {
            interp = new InteractiveConsole();
        }

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
 
        // was there a filename on the command line?
        if (opts.filename != null) {
            String path = new java.io.File(opts.filename).getParent();
            if (path == null)
                path = "";
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
                    if (!opts.interactive)
                        System.exit(-1);
                }
            }
        }
        else {
            // if there was no file name on the command line, then "" is
            // the first element on sys.path.  This is here because if
            // there /was/ a filename on the c.l., and say the -i option
            // was given, sys.path[0] will have gotten filled in with the
            // dir of the argument filename.
            Py.getSystemState().path.insert(0, new PyString(""));
        }

        if (opts.interactive) {
            try {
                interp.interact(null);
            } catch (Throwable t) {
                Py.printException(t);
            }
        }
    }
}



class CommandLineOptions
{
    public String filename;
    public boolean jar, interactive, notice;
    private boolean fixInteractive;
    public boolean help, version;
    public String[] argv;
    public java.util.Properties properties;
    public String command;

    public CommandLineOptions() {
        filename = null;
        jar = fixInteractive = false;
        interactive = notice = true;
        properties = new java.util.Properties();
        help = version = false;
    }

    public void setProperty(String key, String value) {
        properties.put(key, value);
        // This only works for Java 1.2.  There appears to be no portable
        // way to support this under Java 1.1
//         try {
//             System.setProperty(key, value);
//         }
//         catch (SecurityException e) {}
    }

    public boolean parse(String[] args) {
        int index=0;
        while (index < args.length && args[index].startsWith("-")) {
            String arg = args[index];
            if (arg.equals("--help")) {
                help = true;
                return false;
            }
            else if (arg.equals("--version")) {
                version = true;
                return false;
            }
            else if (arg.equals("-")) {
                if (!fixInteractive)
                    interactive = false;
                filename = "-";
            }
            else if (arg.equals("-i")) {
                fixInteractive = true;
                interactive = true;
            }
            else if (arg.equals("-jar")) {
                jar = true;
                if (!fixInteractive)
                    interactive = false;
            }
            else if (arg.equals("-X")) {
                Options.classBasedExceptions = false;
            }
            else if (arg.equals("-S")) {
                Options.importSite = false;
            }
            else if (arg.equals("-c")) {
                command = args[++index];
                if (!fixInteractive) interactive = false;              
                break;
            }
            else if (arg.startsWith("-D")) {
                String key = null; 
                String value = null;
                int equals = arg.indexOf("=");
                if (equals == -1) {
                    String arg2 = args[++index];
                    key = arg.substring(2, arg.length());
                    value = arg2;
                }
                else {
                    key = arg.substring(2, equals);
                    value = arg.substring(equals+1, arg.length());
                }
                setProperty(key, value);
            }
            else {
                String opt = args[index];
                if (opt.startsWith("--"))
                    opt = opt.substring(2);
                else if (opt.startsWith("-"))
                    opt = opt.substring(1);
                System.err.println("jython: illegal option -- " + opt);
                return false;
            }
            index += 1;
        }
        notice = interactive;
        if (filename == null && index < args.length && command == null) {
            filename = args[index++];
            if (!fixInteractive)
                interactive = false;
            notice = false;
        }
        if (command != null)
            notice = false;

        int n = args.length-index+1;
        argv = new String[n];
        //new String[args.length-index+1];
        if (filename != null)
            argv[0] = filename;
        else argv[0] = "";

        for(int i=1; i<n; i++, index++) {
            argv[i] = args[index];
        }

        return true;
    }
}
