// Copyright (c) Corporation for National Research Initiatives
package org.python.modules;
import org.python.core.*;

public class types implements ClassDictInit {
    public static PyString __doc__ = new PyString(
        "Define names for all type symbols known in the standard "+
                "interpreter.\n"+
        "\n"+
        "Types that are part of optional modules (e.g. array) "+
                "are not listed.\n"
    );

    // xxx change some of these
    public static void classDictInit(PyObject dict) {
        dict.__setitem__("ArrayType", PyType.fromClass(PyArray.class));
        dict.__setitem__("BooleanType", PyType.fromClass(PyBoolean.class));
        dict.__setitem__("BuiltinFunctionType",
                         PyType.fromClass(PyReflectedFunction.class));
        dict.__setitem__("BuiltinMethodType",
                         PyType.fromClass(PyMethod.class));
        dict.__setitem__("ClassType", PyType.fromClass(PyClass.class));
        dict.__setitem__("CodeType", PyType.fromClass(PyCode.class));
        dict.__setitem__("ComplexType", PyType.fromClass(PyComplex.class));
        dict.__setitem__("DictType", PyType.fromClass(PyDictionary.class));
        dict.__setitem__("DictionaryType",
                         PyType.fromClass(PyDictionary.class));
        dict.__setitem__("EllipsisType",
                         PyType.fromClass(PyEllipsis.class));
        dict.__setitem__("FileType", PyType.fromClass(PyFile.class));
        dict.__setitem__("FloatType", PyType.fromClass(PyFloat.class));
        dict.__setitem__("FrameType", PyType.fromClass(PyFrame.class));
        dict.__setitem__("FunctionType",
                         PyType.fromClass(PyFunction.class));
        dict.__setitem__("GeneratorType",
                         PyType.fromClass(PyGenerator.class));
        dict.__setitem__("InstanceType",
                         PyType.fromClass(PyInstance.class));
        dict.__setitem__("IntType", PyType.fromClass(PyInteger.class));
        dict.__setitem__("LambdaType", PyType.fromClass(PyFunction.class));
        dict.__setitem__("ListType", PyType.fromClass(PyList.class));
        dict.__setitem__("LongType", PyType.fromClass(PyLong.class));
        dict.__setitem__("MethodType", PyType.fromClass(PyMethod.class));
        dict.__setitem__("ModuleType", PyType.fromClass(PyModule.class));
        dict.__setitem__("NoneType", PyType.fromClass(PyNone.class));
        dict.__setitem__("SliceType", PyType.fromClass(PySlice.class));
        dict.__setitem__("StringType", PyType.fromClass(PyString.class));
        dict.__setitem__("TracebackType",
                         PyType.fromClass(PyTraceback.class));
        dict.__setitem__("TupleType", PyType.fromClass(PyTuple.class));
        dict.__setitem__("TypeType", PyType.fromClass(PyJavaClass.class));
        dict.__setitem__("UnboundMethodType",
                         PyType.fromClass(PyMethod.class));
        dict.__setitem__("UnicodeType", PyType.fromClass(PyString.class));
        dict.__setitem__("XRangeType", PyType.fromClass(PyXRange.class));

        dict.__setitem__("StringTypes", new PyTuple(new PyObject[] {
                PyType.fromClass(PyString.class), PyType.fromClass(PyUnicode.class)
        }));
    }
}
