// Copyright (c) Corporation for National Research Initiatives
package org.python.util;

import java.io.EOFException;
import java.io.IOException;

import org.gnu.readline.Readline;
import org.gnu.readline.ReadlineLibrary;

import org.python.core.Py;
import org.python.core.PyException;
import org.python.core.PyObject;
import org.python.core.PySystemState;

/**
 * Uses: <a href="http://java-readline.sourceforge.net/">Java Readline</a> <p/>
 * 
 * Based on CPython-1.5.2's code module
 * 
 */
public class ReadlineConsole extends InteractiveConsole {

    public String filename;

    public ReadlineConsole() {
        this(null, CONSOLE_FILENAME);
    }

    public ReadlineConsole(PyObject locals) {
        this(locals, CONSOLE_FILENAME);
    }

    public ReadlineConsole(PyObject locals, String filename) {
        super(locals, filename, true);
        String backingLib = PySystemState.registry.getProperty("python.console.readlinelib",
                                                               "Editline");
        try {
            Readline.load(ReadlineLibrary.byName(backingLib));
        } catch(RuntimeException e) {
            // Silently ignore errors during load of the native library.
            // Will use a pure java fallback.
        }
        Readline.initReadline("jython");

        // Force rebind of TAB to insert a tab instead of complete
        Readline.parseAndBind("tab: tab-insert");
    }

    /**
     * Write a prompt and read a line.
     * 
     * The returned line does not include the trailing newline. When the user
     * enters the EOF key sequence, EOFError is raised.
     * 
     * This subclass implements the functionality using JavaReadline.
     */
    public String raw_input(PyObject prompt) {
        try {
            String line = Readline.readline(prompt == null ? "" : prompt.toString());
            return (line == null ? "" : line);
        } catch(EOFException eofe) {
            throw new PyException(Py.EOFError);
        } catch(IOException ioe) {
            throw new PyException(Py.IOError);
        }
    }
}
