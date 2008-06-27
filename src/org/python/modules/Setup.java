// Copyright (c) Corporation for National Research Initiatives
package org.python.modules;

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
        "jarray",
        "math",
        "thread:org.python.modules.thread.thread",
        "operator",
        "time:org.python.modules.time.Time",
        "_py_compile",
        "pre:org.python.modules.re",
        "_sre",
        "synchronize",
        "cPickle",
        "cStringIO",
        "struct",
        "binascii",
        "exceptions:org.python.core.exceptions",
        "_codecs",
        "imp",
        "sha",
        "ucnhash",
        "_jython",
        "_new:org.python.modules._newmodule",
        "_weakref:org.python.modules._weakref.WeakrefModule",
        "xreadlines",
        "errno",
        "array:org.python.modules.ArrayModule",
        "_random:org.python.modules.random.RandomModule",
        "cmath",
        "itertools",
        "zipimport:org.python.modules.zipimport.zipimport",
        "collections:org.python.modules.collections.Collections",
        "gc",
        "_hashlib"
    };
}
