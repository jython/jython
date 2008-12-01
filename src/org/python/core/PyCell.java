/* Copyright (c) Jython Developers */
package org.python.core;

import org.python.expose.ExposedGet;
import org.python.expose.ExposedType;

/**
 * The Python cell type.
 * 
 * Cells are used to implement variables referenced by multiple
 * scopes.
 */
@ExposedType(name = "cell", isBaseType = false)
public class PyCell extends PyObject {

    /** The underlying content of the cell, or null. */
    public PyObject ob_ref;

    @ExposedGet(name = "cell_contents")
    public PyObject getCellContents() {
        if (ob_ref == null) {
            throw Py.ValueError("Cell is empty");
        }
        return ob_ref;
    }

    @Override
    public String toString() {
        if (ob_ref == null) {
            return String.format("<cell at %s: empty>", Py.idstr(this));
        }
        return String.format("<cell at %s: %.80s object at %s>", Py.idstr(this),
                             ob_ref.getType().getName(), Py.idstr(ob_ref));
    }
}
