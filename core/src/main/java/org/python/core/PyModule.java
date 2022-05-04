package org.python.core;

import java.lang.invoke.MethodHandles;
import java.util.Map;

/**
 * The Python {@code module} object.
 * <p>
 * Stop-gap implementation to satisfy use elsewhere in the project.
 */
public class PyModule implements CraftedPyObject, DictPyObject {

    /** The type of Python object this class implements. */
    public static final PyType TYPE = PyType.fromSpec( //
            new PyType.Spec("module", MethodHandles.lookup()));

    protected final PyType type;

    /** Name of this module. **/
    final String name;

    /** Dictionary (globals) of this module. **/
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

    @Override
    public PyType getType() { return type; }

    @Override
    public Map<Object, Object> getDict() { return dict; }

    @Override
    public String toString() { return String.format("<module '%s'>", name); }
}
