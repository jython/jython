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
        Readline.initReadline("jpython");
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
        try {
           return Readline.readline(prompt==null ? "" : prompt.toString());
        } catch (java.io.EOFException eofe) {
           throw new PyException(Py.EOFError);
        } catch (java.io.IOException e) {
           throw new PyException(Py.IOError);
        }
    }
}
