package org.python.core;

/** A <code>PyObject</code> that provides <code>__enter__</code> and <code>__exit__</code> methods for use in the with-statement.
 *
 * Implementing context managers can then be potentially inlined by the JVM.
 */

public interface ContextManager {
    public PyObject __enter__(ThreadState ts);
    public boolean __exit__(ThreadState ts, PyException exception);
}
