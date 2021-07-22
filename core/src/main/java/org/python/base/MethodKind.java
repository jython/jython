package org.python.base;

/**
 * Enum describing whether a method is an instance, static or class
 * method (in Python).
 */
public enum MethodKind {
    /**
     * The method must be defined by a Java static method. An initial
     * self or module argument is not expected. (If the writer attempts
     * to declare one it will simply be the first parameter.) In a call
     * from Python that uses dot notation, which is attribute lookup,
     * the target object (or its type) is used to find the method, but
     * is not bound to the first parameter.
     */
    /*
     * In CPython STATIC cannot be used for functions in modules, but we
     * find it useful to expose Java static methods that way.
     */
    STATIC,

    /**
     * The first argument is self or a module. The method must be
     * defined either by a Java instance method or by a static method in
     * which an initial self or module argument is declared. In a call
     * from Python that uses dot notation, which is attribute lookup,
     * the target object (or module) is used to find the method, and is
     * bound to the first parameter.
     */
    INSTANCE,

    /**
     * The first argument is the Python type of the target. The method
     * must be defined either by a Java static method in which an
     * initial type argument is declared. In a call from Python that
     * uses dot notation, which is attribute lookup, the target object's
     * type is used to find the method, and is bound to the first
     * parameter.
     */
    // CLASS cannot be used for functions in modules.
    CLASS
}
