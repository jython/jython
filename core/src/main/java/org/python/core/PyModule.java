// Copyright (c)2023 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.core;

import java.lang.invoke.MethodHandles;

/** The Python {@code module} object. */
public class PyModule implements CraftedPyObject, DictPyObject {

    /** The type of Python object this class implements. */
    public static final PyType TYPE = PyType.fromSpec( //
            new PyType.Spec("module", MethodHandles.lookup()));

    protected final PyType type;

    /** Name of this module. Not {@code null}. **/
    final String name;

    /** Dictionary (globals) of this module. Not {@code null}. **/
    final PyDict dict;

    /**
     * As {@link #PyModule(String)} for Python sub-class specifying
     * {@link #type}.
     *
     * @param type actual Python sub-class to being created
     * @param name of module
     */
    PyModule(PyType type, String name) {
        this.type = type;
        this.name = name;
        this.dict = new PyDict();
    }

    /**
     * Construct an instance of the named module.
     *
     * @param name of module
     */
    PyModule(String name) { this(TYPE, name); }

    /**
     * Initialise the module instance. The main action will be to add
     * entries to {@link #dict}. These become the members (globals) of
     * the module.
     */
    void exec() {}

    @Override
    public PyType getType() { return type; }

    /**
     * The global dictionary of a module instance. This is always a
     * Python {@code dict} and never {@code null}.
     *
     * @return The globals of this module
     */
    @Override
    public PyDict getDict() { return dict; }

    @Override
    public String toString() { return String.format("<module '%s'>", name); }

    /**
     * Add a type by name to the dictionary.
     *
     * @param t the type
     */
    void add(PyType t) { dict.put(t.getName(), t); }

    /**
     * Add an object by name to the module dictionary.
     *
     * @param name to use as key
     * @param o value for key
     */
    void add(String name, Object o) { dict.put(name, o); }
}
