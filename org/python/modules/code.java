// Copyright © Corporation for National Research Initiatives
package org.python.modules;

import org.python.core.*;

public class code {
    public static PyObject compile_command(String string) {
        return codeop.compile_command(string, "<input>", "single");
    }
    
    public static PyObject compile_command(String string, String filename) {
        return codeop.compile_command(string, filename, "single");
    }
    
    public static PyObject compile_command(String string, String filename,
					   String kind)
    {
        return codeop.compile_command(string, filename, kind);
    }
}
