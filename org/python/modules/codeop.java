// Copyright (c) Corporation for National Research Initiatives
package org.python.modules;

import org.python.core.*;

public class codeop implements ClassDictInit {
    public static PyString __doc__ = new PyString(
        "Utility to compile possibly incomplete Python source code.\n"
    );

    public static void classDictInit(PyObject dict)
    {
        dict.__delitem__("compile_command_flags");
        dict.__delitem__("classDictInit");
    }

    public static PyList __all__ = new PyList(new PyString[] {
        new PyString("compile_command")
    });

    public static PyObject compile_command(String string) {
        return compile_command(string, "<input>", "single");
    }

    public static PyObject compile_command(String string, String filename) {
        return compile_command(string, filename, "single");
    }

    public static PyObject compile_command(String string, String filename,
                                           String kind)
    {
        return compile_command_flags(string,filename,kind,null);
    }

    public static PyObject compile_command_flags(String string,
                    String filename, String kind, CompilerFlags cflags)
    {
        org.python.parser.SimpleNode node =
            parser.partialParse(string+"\n", kind, filename, cflags);

        if (node == null)
            return Py.None;
        return Py.compile_flags(node, Py.getName(), filename, true, true,
                                cflags);
    }

}
