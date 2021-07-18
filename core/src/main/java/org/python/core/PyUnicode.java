package org.python.core;

import java.lang.invoke.MethodHandles;

/**
 * This is a placeholder to satisfy references in the implementation before we
 * have a proper {@code str} type.
 */
class PyUnicode implements CraftedPyObject {

    static final PyType TYPE = PyType.fromSpec( //
            new PyType.Spec("str", MethodHandles.lookup()) //
                    .adopt(String.class));

    @Override
    public PyType getType() { return TYPE; }
}
