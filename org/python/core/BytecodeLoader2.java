// Copyright © Corporation for National Research Initiatives

package org.python.core;
import java.io.*;
import java.util.*;
import java.security.*;

public class BytecodeLoader2 extends SecureClassLoader implements Loader {
    private Vector parents;

    public BytecodeLoader2() {
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
        Class c = defineClass(name, data, 0, data.length,
                              this.getClass().getProtectionDomain());
        resolveClass(c);
        BytecodeLoader.compileClass(c);
        return c;
    }

}
