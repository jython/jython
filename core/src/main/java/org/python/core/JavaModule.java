// Copyright (c)2023 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.core;

/** Common mechanisms for all Python modules defined in Java. */
public abstract class JavaModule extends PyModule {

    final ModuleDef definition;

    /**
     * Construct the base {@code JavaModule}, saving the module
     * definition, which is normally created during static
     * initialisation of the concrete class defining the module. In
     * terms of PEP 489 phases, the constructor performs the
     * {@code Py_mod_create}. We defer filling the module dictionary
     * from the definition and other sources until {@link #exec()} is
     * called.
     *
     * @param definition of the module
     */
    protected JavaModule(ModuleDef definition) {
        super(definition.name);
        this.definition = definition;
    }

    /**
     * {@inheritDoc}
     * <p>
     * In the case of a {@code JavaModule}, the base implementation
     * mines the method definitions from the {@link #definition}. The
     * module should extend this method, that is call
     * {@code super.exec()} to add boilerplate and the methods, then add
     * other definitions (typically constants) to the module namespace
     * with {@link #add(String, Object) #add(String, Object)}. In terms
     * of PEP 489 phases, this is the {@code Py_mod_exec} phase.
     */
    @Override
    void exec() {
        super.exec();
        definition.addMembers(this);
    }
}
