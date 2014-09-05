/* Copyright (c) Jython Developers */
package org.python.modules._jythonlib;

import org.python.core.PyDictionary;
import org.python.core.PyObject;
import org.python.core.PyType;

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

public class dict_builder extends PyObject {

    public static final PyType TYPE = PyType.fromClass(dict_builder.class);
    private final PyObject factory;

    public dict_builder(PyObject factory) {
        super();
        this.factory = factory;
    }

    public PyObject __call__(PyObject[] args, String[] keywords) {
        ConcurrentMap map = (ConcurrentMap) (factory.__call__().__tojava__(ConcurrentMap.class));
        PyDictionary dict = new PyDictionary(map, true);
        dict.updateCommon(args, keywords, "dict");
        return dict;
    }

}
