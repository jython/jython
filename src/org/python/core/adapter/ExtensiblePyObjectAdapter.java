package org.python.core.adapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.python.core.PyObject;

/**
 * A PyObjectAdapter attempts to adapt a Java Object with three user fillable
 * groups of adapters: preClass, class and postClass.
 * 
 */
public class ExtensiblePyObjectAdapter implements PyObjectAdapter {

	/**
	 * @return true if a preClass, postClass or class adapter can handle this
	 */
	public boolean canAdapt(Object o) {
		return findAdapter(preClassAdapters, o) != null || classAdapters.containsKey(o.getClass())
				|| findAdapter(postClassAdapters, o) != null;
	}

	/**
	 * Attempts to adapt o using the preClass, class and postClass adapters.
	 * 
	 * First each of the preClass adapters is asked in the order of addition if
	 * they can adapt o. If so, they adapt it. Otherwise, if o.getClass() is
	 * equal to one of the classes from the added ClassAdapters, that class
	 * adapter is used. Finally, each of the post class adapters are asked in
	 * turn if they can adapt o. If so, that adapter handles it. If none can,
	 * null is returned.
	 */
	public PyObject adapt(Object o) {
		PyObjectAdapter adapter = findAdapter(preClassAdapters, o);
		if (adapter != null) {
			return adapter.adapt(o);
		}

		adapter = (PyObjectAdapter) classAdapters.get(o.getClass());
		if (adapter != null) {
			return adapter.adapt(o);
		}

		adapter = findAdapter(postClassAdapters, o);
		if (adapter != null) {
			return adapter.adapt(o);
		}
		return null;
	}

	/**
	 * Adds an adapter to the list of adapters to be tried before the
	 * ClassAdapters.
	 */
	public void addPreClass(PyObjectAdapter adapter) {
		preClassAdapters.add(adapter);
	}

	/**
	 * Adds a Class handling adapter that will adapt any objects of its Class if
	 * that object hasn't already been handled by one of the pre class adapters.
	 */
	public void add(ClassAdapter adapter) {
		classAdapters.put(adapter.getAdaptedClass(), adapter);
	}

	/**
	 * Adds an adapter to the list of adapters to be tried after the
	 * ClassAdapters.
	 */
	public void addPostClass(PyObjectAdapter converter) {
		postClassAdapters.add(converter);
	}

	private static PyObjectAdapter findAdapter(List l, Object o) {
		for (Iterator iter = l.iterator(); iter.hasNext();) {
			PyObjectAdapter adapter = (PyObjectAdapter) iter.next();
			if (adapter.canAdapt(o)) {
				return adapter;
			}
		}
		return null;
	}

	private List preClassAdapters = new ArrayList();

	private List postClassAdapters = new ArrayList();

	private Map classAdapters = new HashMap();

}
