package org.python.core;

/**
 * A common interface for bytecode loaders. Jython 2.0 have two loaders, a
 * standard loader and a Java2 SecureClassLoader. Both loader implements this
 * interface.
 */

public interface Loader {
    /**
     * Turn java byte codes into a class.
     */
    public Class loadClassFromBytes(String name, byte[] data);

    /**
     * Add another classloader as a parent loader. Dependent classes will
     * searched in these loaders.
     */
    public void addParent(ClassLoader referent);
}
