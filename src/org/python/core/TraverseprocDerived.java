package org.python.core;

/**
 * This is used like Traverseproc, but traverses only the slots[] array
 * of fooDerived classes. This way it is avoided that the traverse
 * method of a traversable PyObject is overwritten by the derived
 * version. The gc module takes care of exploiting both traverse methods.
 *
 */
public interface TraverseprocDerived {
	/**
	 * Traverses all reachable {@code PyObject}s.
	 * Like in CPython, {@code arg} must be passed
	 * unmodified to {@code visit} as its second parameter.
	 */
	public int traverseDerived(Visitproc visit, Object arg);
}
