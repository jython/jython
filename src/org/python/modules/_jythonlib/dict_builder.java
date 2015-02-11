/* Copyright (c) Jython Developers */
package org.python.modules._jythonlib;

import org.python.core.PyDictionary;
import org.python.core.PyDictionaryDerived;
import org.python.core.PyObject;
import org.python.core.PyType;
import org.python.core.Traverseproc;
import org.python.core.Visitproc;

import java.util.concurrent.ConcurrentMap;


/* Support building PyDictionary objects with arbitrary backing ConcurrentMap objects
 * Uses a factory for efficiency.
 * Usage from Python: _threads = dict_builder(MapMaker().weakValues().makeMap)()
 *
 * Such usage avoids problems with converting from boxed Python objects to their unboxed
 * versions, compared to building this in Java and exporting to Python via PyJavaType.
 * So in the above example_threads dict maps from thread ID to JavaThread.
 * But thread ID is a long, which meant that we did not have equality between long/int, as in Python
 * JavaThread also exposes __java__ to convert to the backing thread, but this meant this would
 * also be unboxed this way, so the wrapping thread could not be looked up!
 */

public class dict_builder extends PyObject implements Traverseproc {

    public static final PyType TYPE = PyType.fromClass(dict_builder.class);
    private final PyObject factory;
    private final PyType dict_type;

    public dict_builder(PyObject factory) {
        super();
        this.factory = factory;
	this.dict_type = null;
    }

    public dict_builder(PyObject factory, PyType dict_type) {
	super();
	this.factory = factory;
	this.dict_type = dict_type;
    }

    public PyObject __call__(PyObject[] args, String[] keywords) {
        ConcurrentMap map = (ConcurrentMap) (factory.__call__().__tojava__(ConcurrentMap.class));
	PyDictionary dict;
	if (dict_type == null) {
	    dict = new PyDictionary(map, true);
	} else {
	    dict = new PyDictionaryDerived(dict_type, map, true);
	}
        dict.updateCommon(args, keywords, "dict");
        return dict;
    }


    /* Traverseproc implementation */
    @Override
    public int traverse(Visitproc visit, Object arg) {
        return factory != null ? visit.visit(factory, arg) : 0;
    }

    @Override
    public boolean refersDirectlyTo(PyObject ob) {
        return ob != null && factory == ob;
    }
}
