
package org.python.core;

/**
 * A tagging exception. It is never actually thrown but used
 * only to mark java methods that should not be visible from
 * jython.
 */
public class PyIgnoreMethodTag extends RuntimeException {
}

