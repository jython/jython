// Copyright (c)2026 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.core;

import java.lang.invoke.MethodHandles.Lookup;

import org.python.core.ModuleDef.MethodDef;
import org.python.support.InterpreterError;
import org.python.support.ScopeKind;

/**
 * A {@code ModuleExposer} provides access to the attributes of a module
 * defined in Java (a built-in or extension module). These are primarily
 * the {@link MethodDef}s derived from annotated methods in the defining
 * class. {@link ModuleDef} uses an instance as it fills the module
 * dictionary.
 */
class ModuleExposer extends Exposer {

    /** The name of the module (mainly for logging and debug). */
    final String name;

    /**
     * Construct the {@code ModuleExposer} instance for a particular module.
     *
     * @param name of the module
     */
    ModuleExposer(String name) { this.name = name; }

    /**
     * Build the result from the defining class.
     *
     * @param definingClass to scan for definitions
     */
    void expose(Class<?> definingClass) {
        // Scan the defining class for definitions
        scanJavaMethods(definingClass);
        // XXX ... and for fields.
        // XXX ... and for types defined in the module maybe? :o
    }

    @Override
    ScopeKind kind() { return ScopeKind.MODULE; }

    /**
     * From the methods discovered by introspection of the class, return an
     * array of {@link MethodDef}s. This array will normally be part of a
     * {@link ModuleDef} from which the dictionary of each instance of the
     * module will be created.
     * <p>
     * A {@link MethodDef} relies on {@code MethodHandle}, so a lookup
     * object must be provided with the necessary access to the defining
     * class.
     *
     * @param lookup authorisation to access methods
     * @return method definitions
     * @throws InterpreterError on lookup prohibited
     */
    MethodDef[] getMethodDefs(Lookup lookup) throws InterpreterError {
        MethodDef[] a = new MethodDef[methodSpecs.size()];
        int i = 0;
        for (CallableSpec ms : methodSpecs) { a[i++] = ms.getMethodDef(lookup); }
        return a;
    }
}
