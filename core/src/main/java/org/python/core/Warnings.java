// Copyright (c)2026 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.core;

/**
 * Compare CPython {@code warnings.h}: {@code PyErr_*}. At present, this
 * is no more than a home for placeholder methods to satisfy ported
 * code. None of the functionality of the {@code warnings} module is
 * really present.
 */
class Warnings {

    private static Object do_warn(String message, PyType category, int stack_level, Object source) {
        // Work out what code to attribute the warning (exception) to.
        /*
         * setup_context(stack_level, &filename, &lineno, &module, &registry)
         */

        // Submit warning (to filters) with that metadata
        /*
         * res = warn_explicit(category, Py.str(message), filename, lineno,
         * module, registry, null, source);
         */
        System.err.print(message);
        return "";
    }

    /**
     * Submit a warning, which may be thrown as an exception if certain
     * conditions are met. In a proper implementation there would be a
     * careful decision (see Python {@code warnings} module), but here we
     * just print a message.
     *
     * @param category of warning (as a Python {@code type})
     * @param stack_level relative location in the Python stack
     * @param format of the message
     * @param args to the message {@code format}
     * @throws PyBaseException ({@link PyExc#RuntimeWarning RuntimeWarning})
     *     if conditions are met
     */
    // Compare CPython _warnings.c :: PyErr_WarnFormat
    // (also PyErr_WarnEx).
    static void format(PyType category, int stack_level, String format, Object... args)
            throws PyBaseException {
        String message = String.format(format, args);
        if (category == null) { category = PyExc.RuntimeWarning; }
        do_warn(message, category, stack_level, null);
    }
}
