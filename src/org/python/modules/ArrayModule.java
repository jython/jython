//Copyright (c) Corporation for National Research Initiatives
package org.python.modules;

import org.python.core.ClassDictInit;
import org.python.core.PyArray;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.core.PyType;

/**
 * The python array module, plus jython extensions from jarray.
 */
public class ArrayModule implements ClassDictInit {
    
    public static PyString __doc__ = new PyString(
        "This module defines a new object type which can efficiently represent\n" +
        "an array of basic values: characters, integers, floating point\n" +
        "numbers.  Arrays are sequence types and behave very much like lists,\n" +
        "except that the type of objects stored in them is constrained.  The\n" +
        "type is specified at object creation time by using a type code, which\n" +
        "is a single character.  The following type codes are defined:\n" +
        "\n" +
        "    Type code   C Type             Minimum size in bytes \n" +
        "    'z'         boolean            1 \n" +
        "    'c'         character          1 \n" +
        "    'b'         signed integer     1 \n" +
        "    'B'         unsigned integer   1 \n" +
        "    'h'         signed integer     2 \n" +
        "    'H'         unsigned integer   2 \n" +
        "    'i'         signed integer     2 \n" +
        "    'I'         unsigned integer   2 \n" +
        "    'l'         signed integer     4 \n" +
        "    'L'         unsigned integer   4 \n" +
        "    'f'         floating point     4 \n" +
        "    'd'         floating point     8 \n" +
        "\n" +
        "Functions:\n" +
        "\n" +
        "array(typecode [, initializer]) -- create a new array\n" +
        "\n" +
        "Special Objects:\n" +
        "\n" +
        "ArrayType -- type object for array objects\n"
    );
    
    public static void classDictInit(PyObject dict){
        dict.__setitem__("array", PyType.fromClass(PyArray.class));
        dict.__setitem__("ArrayType", PyType.fromClass(PyArray.class));
    }
    
    /*
     * These are jython extensions (from jarray module). 
     * Note that the argument order is consistent with
     * python array module, but is reversed from jarray module. 
     */
    public static PyArray zeros(char typecode, int n) {
        return PyArray.zeros(n, typecode);
    }

    public static PyArray zeros(Class type, int n) {
        return PyArray.zeros(n, type);
    }    
}
