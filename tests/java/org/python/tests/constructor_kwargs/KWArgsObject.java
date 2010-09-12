package org.python.tests.constructor_kwargs;

import java.util.HashMap;
import java.util.Arrays;

import org.python.core.PyObject;

public class KWArgsObject {
	private HashMap<String, PyObject> data = new HashMap<String, PyObject>();

    public KWArgsObject(PyObject[] values, String[] names) {
		int offset = values.length-names.length;
		for (int i = 0; i<names.length; i++) {
			data.put(names[i], values[offset+i]);
		}
    }
    
    public PyObject getValue(String key) {
		return data.get(key);
    }
}
