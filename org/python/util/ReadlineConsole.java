// Copyright (c) Corporation for National Research Initiatives
package org.python.util;

import org.python.core.*;
import org.gnu.readline.*;

// Based on CPython-1.5.2's code module

public class ReadlineConsole extends InteractiveConsole {
    public String filename;

    public ReadlineConsole() {
        this(null, "<console>");
    }
    public ReadlineConsole(PyObject locals) {
        this(locals, "<console>");
    }
    public ReadlineConsole(PyObject locals, String filename) {
        super(locals,filename);
        String backingLib = PySystemState.registry.getProperty(
                                 "python.console.readlinelib", "Editline");
        try {
            Readline.load(ReadlineLibrary.byName(backingLib));
        } catch (RuntimeException e) {
            // Silently ignore errors during load of the native library.
            // Will use a pure java fallback.
        }

        // hook into the builtins table so that clients like cmd.Cmd can
        // also use readline.
        Py.getSystemState().builtins.__setitem__("raw_input",
              Py.newJavaFunc(this.getClass(), "_raw_input"));

        Readline.initReadline("jython");
    }


    /**
     * Write a prompt and read a line.
     *
     * The returned line does not include the trailing newline.  When the
     * user enters the EOF key sequence, EOFError is raised.
     *
     * This subclass implements the functionality using JavaReadline.
     **/
    public String raw_input(PyObject prompt) {
        return _raw_input(new PyObject[] { prompt }, new String[0]);
    }

    /**
     * Central point of dispatch to Readline library for all clients,
     * whether the console itself or others like cmd.Cmd interpreters.
     * Both of these uses come through here.
     *
     * @param prompt the prompt to be displayed at the beginning of line
     * @return the user input
     **/
    public static String _raw_input(PyObject args[], String kws[]) {
        ArgParser ap = new ArgParser("raw_input", args, kws, "prompt");
        PyObject prompt = ap.getPyObject(0, new PyString(""));        
        try {
            String line = Readline.readline(
                            prompt==null ? "" : prompt.toString());
            return (line == null ? "" : line);
        } catch (java.io.EOFException eofe) {
            throw new PyException(Py.EOFError);
        } catch (java.io.IOException e) {
            throw new PyException(Py.IOError);
        }
    }
}
