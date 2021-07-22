package org.python.core;

/**
 * Abstract base class for the descriptor of a method defined in Java.
 * This class provides some common behaviour and support methods that
 * would otherwise be duplicated.
 */
abstract class MethodDescriptor extends Descriptor implements FastCall {

    MethodDescriptor(PyType descrtype, PyType objclass, String name) {
        super(descrtype, objclass, name);
    }

    /**
     * Translate a problem with the number and pattern of arguments, in
     * a failed attempt to call the wrapped method, to a Python
     * {@link TypeError}.
     *
     * @param ae expressing the problem
     * @param args positional arguments (only the number will matter)
     * @return a {@code TypeError} to throw
     */
    protected TypeError typeError(ArgumentError ae, Object[] args) {
        int n = args.length;
        switch (ae.mode) {
        case NOARGS:
        case NUMARGS:
        case MINMAXARGS:
            return new TypeError("%s() %s (%d given)", name, ae, n);
        case NOKWARGS:
        default:
            return new TypeError("%s() %s", name, ae);
        }
    }
}
