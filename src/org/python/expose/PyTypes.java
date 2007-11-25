package org.python.expose;

import org.objectweb.asm.Type;
import org.python.core.Py;
import org.python.core.PyBoolean;
import org.python.core.PyBuiltinFunction;
import org.python.core.PyBuiltinMethod;
import org.python.core.PyBuiltinMethodNarrow;
import org.python.core.PyException;
import org.python.core.PyInteger;
import org.python.core.PyNewWrapper;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.core.PyType;

/** 
 * Type objects used by exposed generation.
 */
public interface PyTypes {

    // Core Jython types
    public static final Type PYOBJ = Type.getType(PyObject.class);

    public static final Type APYOBJ = Type.getType(PyObject[].class);

    public static final Type PYTYPE = Type.getType(PyType.class);

    public static final Type PYEXCEPTION = Type.getType(PyException.class);

    public static final Type PY = Type.getType(Py.class);

    public static final Type PYSTR = Type.getType(PyString.class);

    public static final Type PYBOOLEAN = Type.getType(PyBoolean.class);

    public static final Type PYINTEGER = Type.getType(PyInteger.class);

    public static final Type PYNEWWRAPPER = Type.getType(PyNewWrapper.class);

    public static final Type BUILTIN_METHOD = Type.getType(PyBuiltinMethod.class);

    public static final Type BUILTIN_METHOD_NARROW = Type.getType(PyBuiltinMethodNarrow.class);

    public static final Type BUILTIN_FUNCTION = Type.getType(PyBuiltinFunction.class);

    public static final Type ABUILTIN_FUNCTION = Type.getType(PyBuiltinFunction[].class);

    public static final Type BUILTIN_INFO = Type.getType(PyBuiltinFunction.Info.class);

    // Exposer Jython types
    public static final Type EXPOSED_TYPE = Type.getType(ExposedType.class);

    public static final Type EXPOSED_METHOD = Type.getType(ExposedMethod.class);

    public static final Type EXPOSED_NEW = Type.getType(ExposedNew.class);

    public static final Type TYPEBUILDER = Type.getType(TypeBuilder.class);
    
    // Java types
    public static final Type STRING = Type.getType(String.class);

    public static final Type ASTRING = Type.getType(String[].class);

    public static final Type STRING_BUILDER = Type.getType(StringBuilder.class);

    public static final Type CLASS = Type.getType(Class.class);
    
    // Primitives
    public static final Type INT = Type.INT_TYPE;

    public static final Type VOID = Type.VOID_TYPE;
    
    public static final Type BOOLEAN = Type.BOOLEAN_TYPE;
}
