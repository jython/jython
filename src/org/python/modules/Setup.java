// Copyright (c) Corporation for National Research Initiatives
package org.python.modules;

import org.python.modules.posix.PosixModule;

// This is sort of analogous to CPython's Modules/Setup file.  Use this to
// specify additional builtin modules.

public class Setup
{
    // Each element of this array is a string naming a builtin module to
    // add to the system.  The string has the following allowable forms:
    //
    // name
    //     The module name is `name' and the class name is
    //     org.python.modules.name
    //
    // name:class
    //     The module name is `name' and the class name is `class' where
    //     class must be a fully qualified Java class name
    //
    // name:null
    //     The module `name' is removed from the list of builtin modules
    //
    // That isn't very useful here, but you can add additional builtin
    // modules by editing the Jython registry file.  See the property
    // python.modules.builtin for details.

    public static String[] builtinModules = {
        "_ast:org.python.antlr.ast.AstModule",
        "_bytecodetools",
        "_codecs",
        "_collections:org.python.modules._collections.Collections",
        "_csv:org.python.modules._csv._csv",
        "_functools:org.python.modules._functools._functools",
        "_hashlib",
        "_jyio:org.python.modules._io._jyio",
        "_json:org.python.modules._json._json",
        "_jythonlib:org.python.modules._jythonlib._jythonlib",
        "_marshal",
        "_py_compile",
        "_random:org.python.modules.random.RandomModule",
        "_sre",
        "_threading:org.python.modules._threading._threading",
        "_weakref:org.python.modules._weakref.WeakrefModule",
        "array:org.python.modules.ArrayModule",
        "binascii",
        "bz2:org.python.modules.bz2.bz2",
        "cPickle",
        "cStringIO",
        "cmath",
        "errno",
        "exceptions:org.python.core.exceptions",
        "gc",
        "_imp:org.python.modules._imp",
        "itertools:org.python.modules.itertools.itertools",
        "jarray",
        "jffi:org.python.modules.jffi.jffi",
        "_locale:org.python.modules._locale._locale",
        "math",
        "operator",
        "struct",
        "synchronize",
        "thread:org.python.modules.thread.thread",
        "time:org.python.modules.time.Time",
        "ucnhash",
        "zipimport:org.python.modules.zipimport.zipimport",
        PosixModule.getOSName() + ":org.python.modules.posix.PosixModule"
    };
}
