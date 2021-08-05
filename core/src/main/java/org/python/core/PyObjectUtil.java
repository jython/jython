// Copyright (c)2021 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.core;

/**
 * Miscellaneous static helpers commonly needed to implement Python objects in
 * Java.
 */
class PyObjectUtil {

    private PyObjectUtil() {} // no instances

    /**
     * A string along the lines "T object at 0xhhh", where T is the type of
     * {@code o}. This is for creating default {@code __repr__} implementations seen
     * around the code base and containing this form. By implementing it here, we
     * encapsulate the problem of qualified type name and what "address" or
     * "identity" should mean.
     *
     * @param o the object (not its type)
     * @return string denoting {@code o}
     */
    static String toAt(Object o) {
        // For the time being identity means:
        int id = System.identityHashCode(o);
        // For the time being type name means:
        String typeName = PyType.of(o).name;
        return String.format("%s object at %#x", typeName, id);
    }

    /**
     * The type of exception thrown when an attempt to convert an object to a common
     * data type fails. This type of exception carries no stack context, since it is
     * used only as a sort of "alternative return value".
     */
    static class NoConversion extends Exception {
        private static final long serialVersionUID = 1L;

        private NoConversion() { super(null, null, false, false); }
    }

    /**
     * A statically allocated {@link NoConversion} used in conversion methods to
     * signal "cannot convert". No stack context is preserved in the exception.
     */
    static final NoConversion NO_CONVERSION = new NoConversion();
}
