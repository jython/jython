package org.python.modules.collections;

import org.python.core.ClassDictInit;
import org.python.core.Py;
import org.python.core.PyObject;

/**
 * Collections - This module adds the ability to use high performance data 
 *               structures.
 *               - deque:  ordered collection accessible from endpoints only
 *               - defaultdict:  dict subclass with a default value factory
 * 
 * @author    Mehendran T (mehendran@gmail.com)
 *            Novell Software Development (I) Pvt. Ltd              
 * @created   Fri 07-Sep-2007 18:50:20
 */
public class Collections implements ClassDictInit {

    public static void classDictInit(PyObject dict) {
        dict.__setitem__("deque", Py.java2py(PyDeque.class));  
        dict.__setitem__("defaultdict", Py.java2py(PyDefaultDict.class));
    }
}
