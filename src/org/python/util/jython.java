// Copyright (c) Corporation for National Research Initiatives
package org.python.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.python.Version;
import org.python.core.BytecodeLoader;
import org.python.core.CompileMode;
import org.python.core.Options;
import org.python.core.PrePy;
import org.python.core.Py;
import org.python.core.PyCode;
import org.python.core.PyException;
import org.python.core.PyFile;
import org.python.core.PyList;
import org.python.core.PyNullImporter;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.core.PyStringMap;
import org.python.core.PySystemState;
import org.python.core.RegistryKey;
import org.python.core.imp;

public class jython {

    /** Exit status: must have {@code OK.ordinal()==0} */
    private enum Status {
        OK, ERROR, NOT_RUN, NO_FILE
    }

    /** The root of the Jython Logger hierarchy, named "org.python". */
    public static final Logger logger;// = Logger.getLogger("org.python");

    /**
     * The default format for console log messages in the command-line Jython. See
     * {@code java.util.logging.SimpleFormatter} for an explanation of the syntax.
     * <p>
     * This format is used in the absence of other logging preferences. Jython tests for definitions
     * in the system properties of {@code java.util.logging.config.class},
     * {@code java.util.logging.config.file}, and {@code java.util.logging.SimpleFormatter.format}
     * and if none of these is defined, it sets the last of them to this value.
     * <p>
     * You can choose something else, for example to log with millisecond time stamps, launch Jython
     * as: <pre>
     * jython -vv -J-Djava.util.logging.SimpleFormatter.format="[%1$tT.%1$tL] %3$s: (%4$s) %5$s%n"
     * </pre> Depending on your shell, the argument may need quoting or escaping.
     */
    public static final String CONSOLE_LOG_FORMAT = "%3$s %4$s %5$s%n";

    static {
        SecurityException exception = null;
        try {
            // Jython console messages (-v option) are emitted using SimpleFormatter
            configureSimpleFormatter(CONSOLE_LOG_FORMAT);
        } catch (SecurityException se) {
            // Unable to access the necessary system properties. Give up on custom logging.
            exception = se;
        }

        // Whether we can configure it or not, we can still _use_ logging.
        logger = Logger.getLogger("org.python");

        if (exception == null) {
            try {
                // Make our "org.python" logger do its own output and not propagate to root.
                setConsoleHandler(logger);
            } catch (SecurityException se) {
                // This probably means no logging finer than INFO (so none enabled by -v)
                exception = se;
            }
        }

        if (exception != null) {
            logger.log(Level.WARNING, "Unable to format console messages: {0}",
                    exception.getMessage());
        }
    }

    // An instance of this class will provide the console (python.console) by default.
    private static final String PYTHON_CONSOLE_CLASS = "org.python.util.JLineConsole";

    private static final String COPYRIGHT =
            "Type \"help\", \"copyright\", \"credits\" or \"license\" for more information.";

    /** The message output when reporting command-line errors and when asked for help. */
    static final String usageHeader =
            "usage: jython [option] ... [-c cmd | -m mod | file | -] [arg] ...\n";

    /** The message additional to {@link #usageHeader} output when asked for help. */
    // @formatter:off
    static final String usageBody =
            "Options and arguments:\n"
            // + "(and corresponding environment variables):\n"
            + "-B       : don't write bytecode files on import\n"
            // + "also PYTHONDONTWRITEBYTECODE=x\n" +
            + "-c cmd   : program passed in as string (terminates option list)\n"
            // + "-d       : debug output from parser (also PYTHONDEBUG=x)\n"
            + "-Dprop=v : Set the property `prop' to value `v'\n"
            + "-E       : ignore environment variables (such as JYTHONPATH)\n"
            + "-h       : print this help message and exit (also --help)\n"
            + "-i       : inspect interactively after running script; forces a prompt even\n"
            + "           if stdin does not appear to be a terminal; also PYTHONINSPECT=x\n"
            + "-jar jar : program read from __run__.py in jar file. Deprecated: instead,\n"
            + "           name the archive as the file argument (runs __main__.py).\n"
            + "-m mod   : run library module as a script (terminates option list)\n"
            // + "-O       : optimize generated bytecode (a tad; also PYTHONOPTIMIZE=x)\n"
            // + "-OO      : remove doc-strings in addition to the -O optimizations\n"
            + "-Q arg   : division options: -Qold (default), -Qwarn, -Qwarnall, -Qnew\n"
            + "-s       : don't add user site directory to sys.path;\n"
            // + "also PYTHONNOUSERSITE\n"
            + "-S       : don't imply 'import site' on initialization\n"
            // + "-t       : issue warnings about inconsistent tab usage (-tt: issue errors)\n"
            + "-u       : unbuffered binary stdout and stderr\n"
            // + "(also PYTHONUNBUFFERED=x)\n"
            // + "           see man page for details on internal buffering relating to '-u'\n"
            + "-v       : verbose (emit more \"org.python\" log messages)\n"
            // + "(also PYTHONVERBOSE=x)\n"
            + "           can be supplied multiple times to increase verbosity\n"
            + "-V       : print the Python version number and exit (also --version)\n"
            + "-W arg   : warning control (arg is action:message:category:module:lineno)\n"
            // + "-x       : skip first line of source, allowing use of non-Unix forms of #!cmd\n"
            + "-3       : warn about Python 3.x incompatibilities that 2to3 cannot trivially fix\n"
            + "file     : program read from script file\n"
            + "-        : program read from stdin (default; interactive mode if a tty)\n"
            + "arg ...  : arguments passed to program in sys.argv[1:]\n" + "\n"
            + "Other environment variables:\n" //
            + "JYTHONSTARTUP: file executed on interactive startup (no default)\n"
            + "JYTHONPATH   : '" + File.pathSeparator
            + "'-separated list of directories prefixed to the default module\n"
            + "               search path.  The result is sys.path.\n"
            + "PYTHONIOENCODING: Encoding[:errors] used for stdin/stdout/stderr.";
    // @formatter:on

    /**
     * Print a full usage message onto {@code System.out} or a brief usage message onto
     * {@code System.err}.
     *
     * @param status if {@code == 0} full help version on {@code System.out}.
     * @return the status given as the argument.
     */
    static Status usage(Status status) {
        boolean fullHelp = (status == Status.OK);
        PrintStream f = fullHelp ? System.out : System.err;
        f.printf(usageHeader);
        if (fullHelp) {
            f.printf(usageBody);
        } else {
            f.println("Try 'jython -h' for more information.");
        }
        return status;
    }

    /**
     * Try to set the format for SimpleFormatter if no other mechanism has been provided, and
     * security allows it. Note that the absolute fall-back format is:
     * {@code "%1$tb %1$td, %1$tY %1$tl:%1$tM:%1$tS %1$Tp %2$s%n%4$s: %5$s%6$s%n"},
     * defined ultimately in {@code sun.util.logging.LoggingSupport}.
     *
     * @param format to set for {@code java.util.logging.SimpleFormatter}
     * @throws SecurityException if not allowed to read or set necessary properties.
     */
    private static void configureSimpleFormatter(String format) throws SecurityException {
        final String CLASS_KEY = "java.util.logging.config.class";
        String className = System.getProperty(CLASS_KEY);
        if (className == null) {
            final String FILE_KEY = "java.util.logging.config.file";
            String fileName = System.getProperty(FILE_KEY);
            if (fileName == null) {
                final String FORMAT_KEY = "java.util.logging.SimpleFormatter.format";
                String currentFormat = System.getProperty(FORMAT_KEY);
                if (currentFormat == null) {
                    // Note that this sets the format for _all_ console logging
                    System.setProperty(FORMAT_KEY, format);
                }
            }
        }
    }

    /**
     * Customise the logger so that it does not propagate to its parent and has its own
     * {@code Handler} accepting all messages. The level set on the logger alone therefore controls
     * whether messages are emitted to the console.
     *
     * @param logger to adjust (always "python.org")
     * @throws SecurityException if no permission to adjust logging
     */
    private static void setConsoleHandler(Logger logger) throws SecurityException {
        logger.setUseParentHandlers(false);
        Handler handler = new ConsoleHandler();
        handler.setLevel(Level.ALL);
        logger.addHandler(handler);
    }

    /**
     * Runs a JAR file, by executing the code found in the file __run__.py, which should be in the
     * root of the JAR archive. Note that {@code __name__} is set to the base name of the JAR file
     * and not to "__main__" (for historical reasons). This method does <b>not</b> handle
     * exceptions. the caller <b>should</b> handle any {@code (Py)Exceptions} thrown by the code.
     *
     * @param filename The path to the filename to run.
     * @return {@code 0} on normal termination (otherwise throws {@code PyException}).
     */
    public static int runJar(String filename) {
        // TBD: this is kind of gross because a local called `zipfile' just magically
        // shows up in the module's globals. Either `zipfile' should be called
        // `__zipfile__' or (preferably, IMO), __run__.py should be imported and a main()
        // function extracted. This function should be called passing zipfile in as an
        // argument.
        //
        // Probably have to keep this code around for backwards compatibility (?)
        try (ZipFile zip = new ZipFile(filename)) {

            ZipEntry runit = zip.getEntry("__run__.py");
            if (runit == null) {
                throw Py.ValueError("can't find '__run__.py' in '" + filename + "'");
            }

            /*
             * Stripping the stuff before the last File.separator fixes Bug #931129 by keeping
             * illegal characters out of the generated proxy class name. Mostly.
             */
            int beginIndex = filename.lastIndexOf(File.separator);
            if (beginIndex >= 0) {
                filename = filename.substring(beginIndex + 1);
            }

            PyStringMap locals = Py.newStringMap();
            locals.__setitem__("__name__", Py.fileSystemEncode(filename));
            locals.__setitem__("zipfile", Py.java2py(zip));

            InputStream file = zip.getInputStream(runit); // closed when zip is closed

            PyCode code = Py.compile(file, "__run__", CompileMode.exec);
            Py.runCode(code, locals, locals);

        } catch (IOException e) {
            throw Py.IOError(e);
        }

        return Status.OK.ordinal();
    }

    /** Now equivalent to {@link #main(String[])}, which is to be preferred. */
    @Deprecated
    public static void run(String[] args) {
        main(args);
    }

    /** Exit Jython with status (converted to an integer). */
    private static void exit(Status status) {
        System.exit(status.ordinal());
    }

    /**
     * Append options from the environment variable {@code PYTHONWARNINGS}, respecting the -E
     * option.
     *
     * @param opts the list to which the options are appended.
     */
    private static void addWarnOptionsFromEnv(PyList opts) {
        String envVar = getenv("PYTHONWARNINGS", "");
        for (String opt : envVar.split(",")) {
            opt = opt.trim();
            if (opt.length() > 0) {
                opts.add(Py.fileSystemEncode(opt));
            }
        }
    }

    /**
     * Attempt to run a module as the {@code __main__} module, via a call to
     * {@code runpy._run_module_as_main}. Exceptions raised by the imported module, including
     * {@code SystemExit}, if not handled by {@code runpy} itself, will propagate out of this
     * method. Note that if {@code runpy} cannot import the module it calls {@code sys.exit} with a
     * message, which will raise {@code SystemExit} from this method.
     *
     * @param moduleName to run
     * @param set_argv0 replace {@code sys.argv[0]} with the file name of the module source
     *            {@code runpy._run_module_as_main} option.
     * @return {@code Status.OK} on normal termination.
     */
    private static Status runModule(String moduleName, boolean set_argv0) {
        // PEP 338 - Execute module as a script
        PyObject runpy = imp.importName("runpy", true);
        PyObject runmodule = runpy.__findattr__("_run_module_as_main");
        // May raise SystemExit (with message)
        runmodule.__call__(Py.fileSystemEncode(moduleName), Py.newBoolean(set_argv0));
        return Status.OK;
    }

    /**
     * Attempt to treat a file as a source of imports, import a module {@code __main__}, and run it.
     * If the file is suitable (e.g. it's a directory or a ZIP archive) the method places the file
     * first on {@code sys.path}, so that {@code __main__} and its packaged dependencies may be
     * found in it. This permits a zip file containing Python source to be run when given as a first
     * argument on the command line. It may be that the file is not of a type that can be imported,
     * in which case the return indicates this.
     *
     * @param archive to run (FS encoded name)
     * @return {@code Status.OK} or {@code Status.NOT_RUN} (if the file was not an archive).
     */
    private static Status runMainFromImporter(PyString archive) {
        PyObject importer = imp.getImporter(archive);
        if (!(importer instanceof PyNullImporter)) {
            // filename is usable as an import source, so put it in sys.path[0] and import __main__
            Py.getSystemState().path.insert(0, archive);
            return runModule("__main__", false);
        }
        return Status.NOT_RUN;
    }

    /**
     * Execute the stream {@code fp} as a file, in the given interpreter, as {@code __main__}. The
     * file name provided must correspond to the stream. In particular, the file name extension
     * "$py.class" will cause the stream to be interpreted as compiled code. For streams that are
     * not really files, this name may be a conventional one like {@code "<stdin>"}, however this
     * method will not treat a console stream as interactive.
     *
     * @param fp Python source code
     * @param filename appears FS-encoded in variable {@code __file__} and in error messages
     * @param interp to do the work
     * @return {@code Status.OK} on normal termination.
     */
    // This is roughly equivalent to CPython PyRun_SimpleFileExFlags
    private static Status runSimpleFile(InputStream fp, String filename, PythonInterpreter interp) {

        // Reflect the current name in the module's __file__, compare PyRun_SimpleFileExFlags.
        final String __file__ = "__file__";
        PyObject globals = interp.globals;
        PyObject previousFilename = globals.__finditem__(__file__);
        if (previousFilename == null) {
            globals.__setitem__(__file__,
                    // Note that __file__ is widely expected to be encoded bytes
                    Py.fileSystemEncode(filename));
        }

        // Allow for already-compiled target, but for us it's a $py.class not a .pyc.
        if (filename.endsWith("$py.class")) {
            // Jython compiled file.
            String name = filename.substring(0, filename.length() - 6); // = - ".class"
            try {
                byte[] codeBytes = imp.readCode(filename, fp, false, imp.NO_MTIME);
                File file = new File(filename);
                PyCode code = BytecodeLoader.makeCode(name, codeBytes, file.getParent());
                interp.exec(code);
            } catch (IOException e) {
                throw Py.IOError(e);
            }
        } else {
            // Assume Python source file: run in the interpreter
            interp.execfile(fp, filename);
        }

        // Delete __file__ variable, previously non-existent. Compare PyRun_SimpleFileExFlags.
        if (previousFilename == null) {
            globals.__delitem__(__file__);
        }
        return Status.OK;
    }

    /**
     * Execute the stream {@code fp} in the given interpreter. If {@code fp} refers to a stream
     * associated with an interactive device (console or terminal input), execute Python source
     * statements from the stream in the interpreter as {@code __main__}. Otherwise, the file name
     * provided must correspond to the stream, as in
     * {@link #runSimpleFile(InputStream, String, PythonInterpreter)}.
     *
     * @param fp Python source code
     * @param filename the name of the file or {@code null} meaning "???"
     * @param interp to do the work
     * @return {@code Status.OK} on normal termination.
     */
    // This is roughly equivalent to CPython PyRun_AnyFileExFlags
    private static Status runStream(InputStream fp, String filename, InteractiveConsole interp) {
        // Following CPython PyRun_AnyFileExFlags here, blindly, concerning null name.
        filename = filename != null ? filename : "???";
        // Run the contents in the interpreter
        if (PrePy.isInteractive(fp, filename)) {
            // __file__ not defined
            interp.interact(null, new PyFile(fp));
        } else {
            // __file__ will be defined
            runSimpleFile(fp, filename, interp);
        }
        return Status.OK;
    }

    /**
     * Attempt to open the named file and execute it in the interpreter as {@code __main__}, as in
     * {@link #runStream(InputStream, String, InteractiveConsole)}. This may raise a Python
     * exception, including {@code SystemExit}. If the file cannot be opened, or using it throws a
     * Java {@code IOException} that is not converted to a {@code PyException} (i.e. not within the
     * executing code), it is reported via {@link #printError(String, Object...)}, and reflected in
     * the return status. If the file can be opened, its parent directory will be inserted at
     * {@code sys.argv[0]}.
     *
     * @param filename the name of the file or {@code null} meaning "???"
     * @param interp to do the work
     * @return {@code Status.OK} on normal termination, {@code Status.NO_FILE} if the file cannot be
     *         read, or {@code Status.ERROR} on other {@code IOException}s.
     */
    private static Status runFile(String filename, InteractiveConsole interp) {
        File file = new File(filename);
        try (InputStream is = new FileInputStream(file)) {
            String parent = file.getAbsoluteFile().getParent();
            interp.getSystemState().path.insert(0, Py.fileSystemEncode(parent));
            // May raise exceptions, (including SystemExit)
            return runStream(is, filename, interp);
        } catch (FileNotFoundException fnfe) {
            // Couldn't open it. No point in going interactive, even if -i given.
            printError("can't open file '%s': %s", filename, fnfe);
            return Status.NO_FILE;
        } catch (IOException ioe) {
            // This may happen on the automatically-generated close()
            printError("error closing '%s': %s", filename, ioe);
            return Status.ERROR;
        }
    }

    /**
     * Attempt to execute the file named in the registry entry {@code python.startup}, which may
     * also have been set via the environment variable {@code JYTHONSTARTUP}. This may raise a
     * Python exception, including {@code SystemExit} that propagates to the caller. If the file
     * cannot be opened, or using it throws a Java {@code IOException} that is not converted to a
     * {@code PyException} (i.e. not within the executing code), it is reported via
     * {@link #printError(String, Object...)}.
     *
     * @param interp to do the work
     */
    private static void runStartupFile(InteractiveConsole interp) {
        String filename = PySystemState.registry.getProperty(RegistryKey.PYTHON_STARTUP, null);
        if (filename != null) {
            try (InputStream fp = new FileInputStream(filename)) {
                // May raise exceptions, (including SystemExit)
                // RunStreamOrThrow(fp, filename, interp);
                runSimpleFile(fp, filename, interp);
            } catch (FileNotFoundException fnfe) {
                // Couldn't open it. No point in going interactive, even if -i given.
                printError("Could not open startup file '%s'", filename);
            } catch (IOException ioe) {
                // This may happen on the automatically-generated close()
                printError("error closing '%s': %s", filename, ioe);
            }
        }
    }

    /**
     * Main Jython program, following the structure and logic of CPython {@code main.c} to produce
     * the same behaviour. The argument to the method is the argument list supplied after the class
     * name in the {@code java} command. Arguments up to the executable script name are options for
     * Jython; arguments after the executable script are supplied in {@code sys.argv}. "Executable
     * script" here means a Python source file name, a module name (following the {@code -m}
     * option), a literal command (following the {@code -c} option), or a JAR file name (following
     * the {@code -jar} option). As a special case of the file name, "-" is allowed, meaning take
     * the script from standard input.
     * <p>
     * The main difference for the caller stems from a difference between C and Java: in C, the
     * argument list {@code (argv)} begins with the program name, while in Java all elements of
     * {@code (args)} are arguments to the program.
     *
     * @param args arguments to the program.
     */
    public static void main(String[] args) {
        // Parse the command line options
        CommandLineOptions opts = CommandLineOptions.parse(args);

        // Choose the basic action
        switch (opts.action) {
            case VERSION:
                System.err.printf("Jython %s\n", Version.PY_VERSION);
                exit(Status.OK);
            case HELP:
                exit(usage(Status.OK));
            case ERROR:
                System.err.println(opts.message);
                exit(usage(Status.ERROR));
            case RUN:
                // Let's run some Python! ...
        }

        // Adjust relative to the level set by java.util.logging.
        PrePy.increaseLoggingLevel(opts.verbosity);

        // Get system properties (or empty set if we're prevented from accessing them)
        Properties preProperties = PrePy.getSystemProperties();
        addDefaultsFromEnvironment(preProperties);

        // Treat the apparent filename "-" as no filename
        boolean haveDash = "-".equals(opts.filename);
        if (haveDash) {
            opts.filename = null;
        }

        // Sense whether the console is interactive, or we have been told to consider it so.
        boolean stdinIsInteractive = PrePy.isInteractive(System.in, null);

        // Shorthand
        boolean haveScript = opts.command != null || opts.filename != null || opts.module != null;

        if (Options.inspect || !haveScript) {
            // We'll be going interactive eventually. condition an interactive console.
            if (PrePy.haveConsole()) {
                // Set the default console type if nothing else has
                addDefault(preProperties, RegistryKey.PYTHON_CONSOLE, PYTHON_CONSOLE_CLASS);
            }
        }

        /*
         * Set up the executable-wide state from the options, environment and registry, and create
         * the first instance of a sys module. We try to leave to this initialisation the things
         * necessary to an embedded interpreter, and to do in the present method only that which
         * belongs only to command line application.
         *
         * (Jython partitions system and interpreter state differently from modern CPython, which is
         * able explicitly to create a PyInterpreterState first, after which sys and the thread
         * state are created to hang from it.)
         */
        // The Jython type system will spring into existence here. This may take a while.
        PySystemState.initialize(preProperties, opts.properties);
        // Now we can use PyObjects safely.
        PySystemState sys = Py.getSystemState();

        /*
         * Jython initialisation does not load the "warnings" module. Rather we defer it to here,
         * where we may safely prepare sys.warnoptions from the -W arguments and the contents of
         * PYTHONWARNINGS (compare PEP 432).
         */
        addFSEncoded(opts.warnoptions, sys.warnoptions);
        addWarnOptionsFromEnv(sys.warnoptions);
        if (!sys.warnoptions.isEmpty()) {
            // The warnings module validates (and may complain about) the warning options.
            imp.load("warnings");
        }

        /*
         * Create the interpreter that we will use as a name space in which to execute the script or
         * interactive session. We run site.py as part of interpreter initialisation (as CPython).
         */
        InteractiveConsole interp = new InteractiveConsole();

        if (opts.verbosity > 0 || (!haveScript && stdinIsInteractive)) {
            // Verbose or going interactive immediately: produce sign on messages.
            System.err.println(InteractiveConsole.getDefaultBanner());
            if (Options.importSite) {
                System.err.println(COPYRIGHT);
            }
        }

        /*
         * We currently have sys.argv = PySystemState.defaultArgv = ['']. Python has a special use
         * for sys.argv[0] according to the source of the script (-m, -c, etc.), but the rest of it
         * comes from the unparsed part of the command line.
         */
        addFSEncoded(opts.argv, sys.argv);

        /*
         * At last, we are ready to execute something. This has two parts: execute the script or
         * console and (if we didn't execute the console) optionally start an interactive console
         * session. The sys.path needs to be prepared in a slightly different way for each case.
         */
        Status sts = Status.NOT_RUN;

        try {
            if (opts.command != null) {
                // The script is an immediate command -c "..."
                sys.argv.set(0, Py.newString("-c"));
                sys.path.insert(0, Py.EmptyString);
                interp.exec(opts.command);
                sts = Status.OK;

            } else if (opts.module != null) {
                // The script is a module
                sys.argv.set(0, Py.newString("-m"));
                sts = runModule(opts.module, true);

            } else if (opts.filename != null) {
                // The script is designated by file (or directory) name.
                PyString pyFileName = Py.fileSystemEncode(opts.filename);
                sys.argv.set(0, pyFileName);

                if (opts.jar) {
                    // The filename was given with the -jar option.
                    sys.path.insert(0, pyFileName);
                    runJar(opts.filename);
                    sts = Status.OK;

                } else {
                    /*
                     * The filename was given as the leading argument after the options. Our first
                     * approach is to treat it as an archive (or directory) in which to find a
                     * __main__.py (as per PEP 338). The handler for this inserts the module at
                     * sys.path[0] if it runs. It may raise exceptions, but only SystemExit as runpy
                     * deals with the others.
                     */
                    sts = runMainFromImporter(pyFileName);

                    if (sts == Status.NOT_RUN) {
                        /*
                         * The filename was not a suitable source for import, so treat it as a file
                         * to execute. The handler for this inserts the parent of the file at
                         * sys.path[0].
                         */
                        sts = runFile(opts.filename, interp);
                        // If we really had no script, do not go interactive at the end.
                        haveScript = sts != Status.NO_FILE;
                    }
                }

            } else { // filename == null
                // There is no script. (No argument or it was "-".)
                if (haveDash) {
                    sys.argv.set(0, Py.newString('-'));
                }
                sys.path.insert(0, Py.EmptyString);

                // Genuinely interactive, or just interpreting piped instructions?
                if (stdinIsInteractive) {
                    // If genuinely interactive, SystemExit should mean exit the application.
                    Options.inspect = false;
                    // If genuinely interactive, run a start-up file if one is specified.
                    runStartupFile(interp);
                }

                // Run from console: exceptions other than SystemExit are handled in the REPL.
                sts = runStream(System.in, "<stdin>", interp);
            }

        } catch (PyException pye) {
            // Whatever the mode of execution an uncaught PyException lands here.
            // If pye was SystemExit *and* Options.inspect==false, this will exit the JVM:
            Py.printException(pye);
            // It was an exception other than SystemExit or Options.inspect==true.
            sts = Status.ERROR;
        }

        /*
         * Check this environment variable at the end, to give programs the opportunity to set it
         * from Python.
         */
        if (!Options.inspect) {
            // If set from Python, the value will be in os.environ, not Java System.getenv.
            Options.inspect = Py.getenv("PYTHONINSPECT", "").length() > 0;
        }

        if (Options.inspect && stdinIsInteractive && haveScript) {
            /*
             * The inspect flag is set (-i option) so we've been asked to end with an interactive
             * session: the console is interactive, and we have just executed some kind of script
             * (i.e. it wasn't already an interactive session).
             */
            try {
                // Ensure that this time SystemExit means exit.
                Options.inspect = false;
                // Run from console: exceptions other than SystemExit are handled in the REPL.
                sts = runStream(System.in, "<stdin>", interp);
            } catch (PyException pye) {
                // Exception from the execution of Python code.
                Py.printException(pye); // SystemExit will exit the JVM here.
                sts = Status.ERROR;
            }
        }

        /*
         * If we arrive here then we ran some Python code. It is possible that threads we started
         * are still running, so if the status is currently good, just return into the JVM. (This
         * exits with good status if nothing goes wrong subsequently.)
         */
        if (sts != Status.OK) {
            // Something went wrong running Python code: shut down in a tidy way.
            interp.cleanup();
            exit(sts);
        }
    }

    /**
     * If the key is not currently present and the passed value is not <code>null</code>, sets the
     * <code>key</code> to the <code>value</code> in the given <code>Properties</code> object. Thus,
     * it provides a default value for a subsequent <code>getProperty()</code>.
     *
     * @param registry to be (possibly) updated
     * @param key at which to set value
     * @param value to set (or <code>null</code> for no setting)
     * @return true iff a value was set
     */
    private static boolean addDefault(Properties registry, String key, String value) {
        // Set value at key if nothing else has set it
        if (value == null || registry.containsKey(key)) {
            return false;
        } else {
            registry.setProperty(key, value);
            return true;
        }
    }

    /**
     * Provides default registry entries from particular supported environment variables, obtained
     * by calls to {@link #getenv(String)}. If a corresponding entry already exists in the
     * properties passed, it takes precedence.
     *
     * @param registry to be (possibly) updated
     */
    private static void addDefaultsFromEnvironment(Properties registry) {

        // Pick up the path from the environment
        addDefault(registry, "python.path", getenv("JYTHONPATH"));

        // Runs at the start of each (wholly) interactive session.
        addDefault(registry, "python.startup", getenv("JYTHONSTARTUP"));
        // Go interactive after script. (PYTHONINSPECT because Python scripts may set it.)
        addDefault(registry, "python.inspect", getenv("PYTHONINSPECT"));

        // Read environment variable PYTHONIOENCODING into properties (registry)
        String pythonIoEncoding = getenv("PYTHONIOENCODING");
        if (pythonIoEncoding != null) {
            String[] spec = pythonIoEncoding.split(":", 2);
            // Note that if encoding or errors is blank (=null), the registry value wins.
            addDefault(registry, RegistryKey.PYTHON_IO_ENCODING, spec[0]);
            if (spec.length > 1) {
                addDefault(registry, RegistryKey.PYTHON_IO_ERRORS, spec[1]);
            }
        }
    }

    /** The same as {@code getenv(name, null)}. */
    private static String getenv(String name) {
        return getenv(name, null);
    }

    /**
     * Get the value of an environment variable, respecting {@link Options#ignore_environment} (the
     * -E option), or return the given default if the variable is undefined or the security
     * environment prevents access. An empty string value from the environment is treated as
     * undefined.
     * <p>
     * This accesses the read-only Java copy of the system environment directly, not
     * {@code os.environ} so that it is safe to use before Python types are available.
     *
     * @param name to access in the environment (if allowed by
     *            {@link Options#ignore_environment}=={@code false}).
     * @param defaultValue to return if {@code name} is not defined or "" or access is forbidden.
     * @return the corresponding value or <code>defaultValue</code>.
     */
    private static String getenv(String name, String defaultValue) {
        if (!Options.ignore_environment) {
            try {
                String value = System.getenv(name);
                return (value != null && value.length() > 0) ? value : defaultValue;
            } catch (SecurityException e) {
                // We're not allowed to access them after all
                Options.ignore_environment = true;
            }
        }
        return defaultValue;
    }

    /** Non-fatal error message when ignoring unsupported option (usually one valid for CPython). */
    private static void optionNotSupported(char option) {
        printError("Option -%c is not supported", option);
    }

    /**
     * Print {@code "jython: <formatted args>"} on {@code System.err} as one line.
     *
     * @param format suitable to use in {@code String.format(format, args)}
     * @param args zero or more args
     */
    private static void printError(String format, Object... args) {
        System.err.println(String.format("jython: " + format, args));
    }

    /**
     * Append strings to a PyList as {@code bytes/str} objects. These might come from the command
     * line, or any source with the possibility of non-ascii values.
     *
     * @param source of {@code String}s
     * @param destination list
     */
    private static void addFSEncoded(Iterable<String> source, PyList destination) {
        for (String s : source) {
            destination.add(Py.fileSystemEncode(s));
        }
    }

    /**
     * Class providing a parser for Jython command line options. Many of the allowable options set
     * values directly in the static {@link Options} as the parser runs, while others set values in
     * (an instance) of this class.
     */
    static class CommandLineOptions {

        /** Possible actions to take after processing the options. */
        enum Action {
            RUN, ERROR, HELP, VERSION
        };

        /** The action to take after processing the options. */
        Action action = Action.RUN;
        /** Set informatively when {@link #action}{@code ==ERROR}. */
        String message = "";

        /** Argument to the -c option. */
        String command;
        /** First argument that is not an option (therefore the executable file). */
        String filename;
        /** Argument to the -m option. */
        String module;

        /** -h or --help option. */
        boolean help = false;
        /** -V or --version option. */
        boolean version = false;
        /** -jar option. */
        boolean jar = false;
        /** Count of -v options. */
        int verbosity = 0;

        /** Collects definitions made with the -D option directly to Jython (not java -D). */
        Properties properties = new Properties();

        /** Arguments after the first non-option, therefore arguments to the executable program. */
        List<String> argv = new LinkedList<String>();
        /** Arguments collected from succesive -W options. */
        List<String> warnoptions = new LinkedList<String>();

        /** Valid single character options. ':' means expect an argument following. */
        // XJD are extra to CPython. X and J are sanctioned while D may one day clash.
        static final String PROGRAM_OPTS = "3bBc:dEhim:OQ:RsStuUvVW:x?" + "XJD:";

        /** Valid long-name options. */
        static final char JAR_OPTION = '\u2615';
        static final OptionScanner.LongSpec[] PROGRAM_LONG_OPTS =
                {new OptionScanner.LongSpec("--", OptionScanner.DONE),
                        new OptionScanner.LongSpec("--help", 'h'),
                        new OptionScanner.LongSpec("--version", 'V'),
                        new OptionScanner.LongSpec("-jar", JAR_OPTION, true), // Yes, just one dash.
                };

        /**
         * Parse the arguments into the static {@link Options} and a returned instance of this
         * class.
         *
         * @param args from program invocation.
         * @return
         */
        static CommandLineOptions parse(String args[]) {
            CommandLineOptions opts = new CommandLineOptions();
            opts._parse(args);
            return opts;
        }

        /** Parser implementation. Do not call this twice on the same instance. */
        private void _parse(String args[]) {
            // Create a scanner with the right tables for Python/Jython
            OptionScanner scanner = new OptionScanner(args, PROGRAM_OPTS, PROGRAM_LONG_OPTS);
            _parse(scanner, args);
            if (action == Action.RUN) {
                // Squirrel away the unprocessed arguments
                while (scanner.countRemainingArguments() > 0) {
                    argv.add(scanner.getWholeArgument());
                }
            }
        }

        /**
         * Parse options into object state, until we encounter the first argument. This is a helper
         * to {@link #_parse(String[])}.
         */
        private void _parse(OptionScanner scanner, String args[]) {
            char c;
            /*
             * The default action is RUN, taken when we all the options have been processed, and
             * either we have run out of arguments (we'll start an interactive session) or
             * encountered a non-option argument, which ought to name the file to execute.
             * Executable options (like -m and -c) cause a return with action==RUN from their case.
             * Any errors, and some special options like --help and -V, return set some other action
             * than RUN, ending the loop.
             */
            while (action == Action.RUN && (c = scanner.getOption()) != OptionScanner.DONE) {

                switch (c) {
                    /*
                     * The first 4 cases all terminate the options in with a RUN action, meaning
                     * that this option defines the executable script and the arguments following
                     * will be passed to the script.
                     */
                    case 'c':
                        /*
                         * -c is the last option; following arguments that look like options are
                         * left for the command to interpret.
                         */
                        command = scanner.getOptionArgument() + "\n";
                        return;

                    case 'm':
                        /*
                         * -m is the last option; following arguments that look like options are
                         * left for the module to interpret.
                         */
                        module = scanner.getOptionArgument();
                        return;

                    case JAR_OPTION:
                        /*
                         * -jar is the last option; following arguments that look like options are
                         * left for __run__.py to interpret.
                         */
                        jar = true;
                        filename = scanner.getOptionArgument();
                        return;

                    case OptionScanner.ARGUMENT:
                        /*
                         * This should be a file name (or "-", meaning stdin); following arguments
                         * that look like options are left for the code it contains to interpret.
                         */
                        filename = scanner.getWholeArgument();
                        return;

                    // Options that don't terminate option processing (mostly).

                    case 'b':
                    case 'd':
                        optionNotSupported(c);
                        break;

                    case '3':
                        Options.py3k_warning = true;
                        if (Options.division_warning == 0) {
                            Options.division_warning = 1;
                        }
                        break;

                    case 'Q':
                        switch (scanner.getOptionArgument()) {
                            case "old":
                                Options.division_warning = 0;
                                break;
                            case "warn":
                                Options.division_warning = 1;
                                break;
                            case "warnall":
                                Options.division_warning = 2;
                                break;
                            case "new":
                                Options.Qnew = true;
                            default:
                                error("-Q option should be `-Qold', "
                                        + "`-Qwarn', `-Qwarnall', or `-Qnew' only");
                        }
                        break;

                    case 'i':
                        Options.inspect = Options.interactive = true;
                        break;

                    case 'O':
                        Options.optimize++;
                        break;

                    case 'B':
                        Options.dont_write_bytecode = true;
                        break;

                    case 's':
                        Options.no_user_site = true;
                        break;

                    case 'S':
                        Options.no_site = true;
                        Options.importSite = false;
                        break;

                    case 'E':
                        Options.ignore_environment = true;
                        break;

                    case 't':
                        optionNotSupported(c);
                        // Py_TabcheckFlag++;
                        break;

                    case 'u':
                        Options.unbuffered = true;
                        break;

                    case 'v':
                        verbosity++;
                        break;

                    case 'x':
                        optionNotSupported(c);
                        // skipfirstline = true;
                        break;

                    // case 'X': reserved for implementation-specific arguments

                    case 'U':
                        optionNotSupported(c);
                        // Py_UnicodeFlag++;
                        break;

                    case 'W':
                        warnoptions.add(scanner.getOptionArgument());
                        break;

                    case 'R':
                        optionNotSupported(c);
                        break;

                    case 'D':
                        // Definitions made on the command line: -Dprop=v
                        try {
                            optionD(scanner);
                        } catch (SecurityException e) {
                            // Prevented by security policy.
                        }
                        break;

                    // Options that terminate option processing with something other than RUN.

                    case 'h':
                    case '?':
                        action = Action.HELP;
                        break;

                    case 'V':
                        action = Action.VERSION;
                        break;

                    case 'J':
                        /*
                         * This shouldn't happen because the launcher should have recognised this
                         * and converted it to an option or argument to the java command. If it
                         * shows up here, maybe it was supplied outside the loader or the context
                         * has confused the launcher.
                         */
                        error("-J is only valid when using the Jython launcher. "
                                + "In a complex command, put the -J options early.");
                        break;

                    case OptionScanner.ERROR:
                        error(scanner.getMessage());
                        break;

                    default:
                        // Acceptable to the scanner, but missing from the case statement?
                        error("parser did not recognise option -%c \\u%04x", c, c);
                        break;
                }
            }
        }

        /**
         * Helper for option {@code -Dprop=v}, adding to the "post-properties". (This is a
         * clash-in-waiting with Python.) The effect is slightly different from {@code -J-Dprop=v},
         * which contributes to the "pre-properties".
         */
        private void optionD(OptionScanner scanner) throws SecurityException {
            String[] kv = scanner.getOptionArgument().split("=", 2);
            String prop = kv[0].trim();
            if (kv.length > 1) {
                properties.put(prop, kv[1]);
            } else {
                properties.put(prop, "");
            }
        }

        /**
         * Set the error message as {@code String.format(message, args)} and set the action to
         * {@link Action#ERROR}.
         */
        private void error(String message, Object... args) {
            this.message = args.length == 0 ? message : String.format(message, args);
            action = Action.ERROR;
        }
    }
}
