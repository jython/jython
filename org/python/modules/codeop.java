// Copyright © Corporation for National Research Initiatives
package org.python.modules;

import org.python.core.*;

public class codeop {
    public static PyObject compile_command(String string) {
        return compile_command(string, "<input>", "single");
    }

    public static PyObject compile_command(String string, String filename) {
        return compile_command(string, filename, "single");
    }

    public static PyObject compile_command(String string, String filename,
					   String kind)
    {
        org.python.parser.SimpleNode node =
	    parser.partialParse(string+"\n", kind, filename);

        if (node == null)
	    return Py.None;
        return Py.compile(node, Py.getName(), filename, true, true);
    }
}
