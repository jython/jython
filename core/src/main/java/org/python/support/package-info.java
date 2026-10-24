/**
 * The {@code base} package contains API classes that support the
 * interpreter without requiring it to be initialised. (Specifically,
 * they may be used before the Python type system is in working order,
 * and without causing it to initialise.)
 * <p>
 * Classes {@code public} in this package are accessible to a client
 * application that {@code requires} the module in its module
 * declaration.
 */
package org.python.support;
