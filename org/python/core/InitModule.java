// Copyright © Corporation for National Research Initiatives

package org.python.core;

/**
 * An deprecated interface that can be used if a java class
 * want control over the class dict initialization.
 *
 * @deprecated. See ClassDictInit for a replacement.
 *
 * @see ClassDictInit
 */


public interface InitModule
{
    public abstract void initModule(PyObject dict);
}
