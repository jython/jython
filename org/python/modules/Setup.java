// Copyright © Corporation for National Research Initiatives
package org.python.modules;

// This is sort of analogous to CPython's Modules/Setup file.  Use this to
// specify additional builtin modules.

public class Setup 
{
    // Each element of this array is a string naming the builtin module.
    // The string can be of the form name:module in which case the class
    // name of the builtin module is module.name.  The string can also be
    // of the form name:null in which case name is removed from the list of 
    // builtin modules.
    //
    // That isn't very useful here, but you can add additional builtin
    // modules by editing the JPython registry file.  See the property
    // python.modules.builtin for details.

    public static String[] builtinModules = {
        "jarray",
        "math",
        "thread",
        "operator",
        "time",
        "os",
        "types",
        "py_compile",
        "codeop",
        "re",
        "code",
        "synchronize",
        "cPickle",
        "cStringIO",
        "struct",
        "binascii",
        "__builtin__:org.python.core"
    };
}
