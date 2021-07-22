package org.python.core;

/**
 * Support direct calls from Java to the function represented by this
 * object, potentially without constructing an argument array.
 * <p>
 * This is an efficiency mechanism similar to the "fast call" paths in
 * CPython. It may provide a basis for efficient call sites for function
 * and method calling when argument lists are simple.
 */
interface FastCall {

    /**
     * @implSpec An object that is a {@link FastCall} must support the
     *     classic call (and with the same result as the direct call).
     *
     * @param args all arguments given positional then keyword
     * @param names of keyword arguments or {@code null}
     * @return result of the invocation
     * @throws Throwable from the implementation
     */
    Object __call__(Object[] args, String[] names) throws Throwable;

    /**
     * Call the object with arguments given by position.
     *
     * @implSpec The default implementation calls
     *     {@link #__call__(Object[], String[]) __call__(args, null)} .
     * @param args arguments given by position
     * @return result of the invocation
     * @throws Throwable from the implementation
     */
    default Object call(Object... args) throws Throwable {
        return __call__(args, null);
    }

    // The idea is to provide a series of specialisations e.g.
    // default Object call(arg0, arg1, arg2) {  ... };
    // Implementations then override the one they like and __call__.
}
