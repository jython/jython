// Copyright (c) Corporation for National Research Initiatives
package org.python.core;

import java.io.Serializable;

/**
 * A class representing the singleton Ellipsis <code>...</code>
 * object.
 */
public class PyEllipsis extends PySingleton implements Serializable {
    PyEllipsis() {
        super("Ellipsis");
    }


    private Object writeReplace() {
        return new Py.SingletonResolver("Ellipsis");
    } 
    
    
}
