
package org.python.modules;
import org.python.core.*;

public class types implements InitModule {
    public void initModule(PyObject dict) {
		dict.__setitem__("ArrayType", PyJavaClass.lookup(PyArray.class));
		dict.__setitem__("BuiltinFunctionType", PyJavaClass.lookup(PyReflectedFunction.class));
		dict.__setitem__("BuiltinMethodType", PyJavaClass.lookup(PyMethod.class));
		dict.__setitem__("ClassType", PyJavaClass.lookup(PyClass.class));
		dict.__setitem__("CodeType", PyJavaClass.lookup(PyCode.class));
		dict.__setitem__("ComplexType", PyJavaClass.lookup(PyComplex.class));
		dict.__setitem__("DictType", PyJavaClass.lookup(PyDictionary.class));
		dict.__setitem__("DictionaryType", PyJavaClass.lookup(PyDictionary.class));
		dict.__setitem__("EllipsisType", PyJavaClass.lookup(PyEllipsis.class));
		dict.__setitem__("FileType", PyJavaClass.lookup(PyFile.class));
		dict.__setitem__("FloatType", PyJavaClass.lookup(PyFloat.class));
		dict.__setitem__("FrameType", PyJavaClass.lookup(PyFrame.class));
		dict.__setitem__("FunctionType", PyJavaClass.lookup(PyFunction.class));
		dict.__setitem__("InstanceType", PyJavaClass.lookup(PyInstance.class));
		dict.__setitem__("IntType", PyJavaClass.lookup(PyInteger.class));
		dict.__setitem__("LambdaType", PyJavaClass.lookup(PyFunction.class));
		dict.__setitem__("ListType", PyJavaClass.lookup(PyList.class));
		dict.__setitem__("LongType", PyJavaClass.lookup(PyLong.class));
		dict.__setitem__("MethodType", PyJavaClass.lookup(PyMethod.class));
		dict.__setitem__("ModuleType", PyJavaClass.lookup(PyModule.class));
		dict.__setitem__("NoneType", PyJavaClass.lookup(PyNone.class));
		dict.__setitem__("SliceType", PyJavaClass.lookup(PySlice.class));
		dict.__setitem__("StringType", PyJavaClass.lookup(PyString.class));
		dict.__setitem__("TracebackType", PyJavaClass.lookup(PyTraceback.class));
		dict.__setitem__("TupleType", PyJavaClass.lookup(PyTuple.class));
		dict.__setitem__("TypeType", PyJavaClass.lookup(PyJavaClass.class));
		dict.__setitem__("UnboundMethodType", PyJavaClass.lookup(PyMethod.class));
		dict.__setitem__("XRangeType", PyJavaClass.lookup(PyXRange.class));
    }
}