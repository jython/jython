// Copyright (c)2026 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.types;

import org.python.core.PyBaseException;
import org.python.core.PyType;
import org.python.core.PyUtil;

/**
 * An instance of a class implementing this interface allows assignment
 * to {@code __class__} within certain constraints that the object must
 * enforce.
 */
public interface WithClassAssignment extends WithClass {

    /**
     * Assign a new type to the {@code __class__} attribute provided the
     * type meets certain criteria. The assignment must fail if
     * {@code replacementType} is not a type with this class as its instance
     * representation.
     *
     * @implSpec The necessary check (and cast) should be made with
     *     {@link #checkClassAssignment(Object)}. It doesn't have to succeed
     *     if that test passes, of course: the implementation may enforce
     *     further restrictions of its own.
     * @param replacementType intended new type object
     */
    // @Exposed.Set(name="__class__")
    void setType(Object replacementType);

    /**
     * Called during {@code __class__} assignment (that is, during the
     * implementation of {@link #setType(Object)} or a constructor) to check
     * that the object being assigned is acceptable. It is only acceptable
     * if the replacement type and the existing type specify the same
     * representation class for their instances. The declared type of
     * {@code type} is {@code Object} to simplify exposure to Python via
     * {@code __setattr__} but it has to be a (replaceable) Python
     * {@code type} object.
     *
     * @param replacementType intended new type object
     * @return argument cast to {@link PyType} (if no error raised)
     * @throws PyBaseException (TypeError) if replacement unacceptable
     */
    default PyType checkClassAssignment(Object replacementType) {
        PyType type = getType();
        if (type == null) {
            // Possible when checking initial assignment to __class__
            return PyUtil.checkReplaceable(getClass(), replacementType);
        } else {
            return PyUtil.checkReplaceable(type, replacementType);
        }
    }

}
