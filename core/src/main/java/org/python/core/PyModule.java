// Copyright (c)2026 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.core;

import java.lang.invoke.MethodHandles;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.python.types.TypeSpec;
import org.python.types.WithDict;

/**
 * The Python {@code module} object. A module object, as might result
 * from a Python {@code import} statement, is an instance of a Java
 * subclass of this class. All those subclasses have the same Python
 * type {@code module}.
 * <p>
 * The term "module" is used somewhat carelessly in Python, usually
 * without significant ambiguity. We may refer to the {@code math}
 * "module", meaning the code in the standard library that provides
 * mathematical functions. To be precise, however, we have to
 * distinguish that code from the instance of type {@code module} that
 * exists in the {@code sys.path} of a particular interpreter after
 * {@code import math}. When we need to, we shall refer to the module
 * class and the module instance.
 * <p>
 * Subclasses representing specific modules may define data during their
 * static initialisation, the first time that module class is needed by
 * the run time system. A built-in module class, defined by a class in
 * Java, will do this using the module exposer. The resulting data
 * should be immutable thereafter. Each module instance must then be
 * executed, by a call to {@link #exec()} when it is imported into a
 * specific interpreter. This action will populate the instance with
 * attributes that could be mutable, but whose scope is that single
 * interpreter.
 */
public class PyModule implements WithDict {

    /** Logger for the marshal module. */
    final Logger logger = LoggerFactory.getLogger(PyModule.class);

    /** The type of Python object this class implements. */
    public static final PyType TYPE = PyType.fromSpec( //
            new TypeSpec("module", MethodHandles.lookup()));

    private final PyType type;

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
     * entries to {@link #dict}. These become the members (globals) of the
     * module.
     */
    public void exec() {
        logger.atInfo().setMessage("Executing body of module '{}'.").addArgument(name).log();
    }

    @Override
    public PyType getType() { return type; }

    /**
     * The global dictionary of a module instance. This is always a Python
     * {@code dict} and never {@code null}.
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
    protected void add(PyType t) { dict.put(t.getName(), t); }

    /**
     * Add an object by name to the module dictionary.
     *
     * @param name to use as key
     * @param o value for key
     */
    protected void add(String name, Object o) {
        logger.atTrace().setMessage("Add {}.{} ({})").addArgument(this.name).addArgument(name)
                .addArgument(PyType.of(o)).log();
        dict.put(name, o);
    }
}
