// Copyright (c) Corporation for National Research Initiatives

package org.python.core;
import java.io.*;
import java.util.*;

/**
 * A java1 classloader for loading compiled python modules.
 */
class BytecodeLoader1 extends ClassLoader implements Loader {
    private Vector parents;

    public BytecodeLoader1() {
        parents = BytecodeLoader.init();
    }

    public void addParent(ClassLoader referent) {
        if (!parents.contains(referent))
            parents.addElement(referent);
    }

    // override from abstract base class
    protected Class loadClass(String name, boolean resolve)
        throws ClassNotFoundException
    {
        Class c = findLoadedClass(name);
        if (c != null)
            return c;
        return BytecodeLoader.findParentClass(parents, name);
    }


    public Class loadClassFromBytes(String name, byte[] data) {
        Class c = defineClass(name, data, 0, data.length);
        resolveClass(c);
        BytecodeLoader.compileClass(c);
        return c;
    }
}
