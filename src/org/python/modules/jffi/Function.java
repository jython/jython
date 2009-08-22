
package org.python.modules.jffi;

import org.python.core.Py;
import org.python.core.PyList;
import org.python.core.PyNewWrapper;
import org.python.core.PyObject;
import org.python.core.PyStringMap;
import org.python.core.PyType;
import org.python.expose.ExposedGet;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedSet;
import org.python.expose.ExposedType;

@ExposedType(name = "jffi.Function", base = Pointer.class)
public class Function extends Pointer {
    public static final PyType TYPE = PyType.fromClass(Function.class);

    private final Pointer pointer;
    private final DynamicLibrary library;

    private final PyStringMap dict = new PyStringMap();
    
    private volatile Type returnType = Type.SINT32;
    private volatile Type[] parameterTypes = null;
    private Invoker invoker = null;

    @ExposedGet
    public final String name;
    
    @ExposedGet
    @ExposedSet
    public PyObject restype;

    Function(PyType type, Pointer address, DirectMemory memory) {
        super(type, address.address, memory);
        this.library = null;
        this.name = "<anonymous>";
        this.pointer = address;
    }

    Function(PyType type, Pointer address) {
        this(type, address, new NativeMemory(address.address));
    }

    Function(PyType type, DynamicLibrary library, String name, long address) {
        super(type, address, new NativeMemory(address));
        this.library = library;
        this.name = name;
        this.pointer = null;
    }

    @ExposedNew
    public static PyObject Function_new(PyNewWrapper new_, boolean init, PyType subtype,
            PyObject[] args, String[] keywords) {

        if (args[0] instanceof Pointer) {
            if (args[0] instanceof DynamicLibrary.Symbol) {
                DynamicLibrary.Symbol sym = (DynamicLibrary.Symbol) args[0];
                return new Function(subtype, sym.library, sym.name, sym.address);
            } else {
                return new Function(subtype, (Pointer) args[0]);
            }
        } else {
            throw Py.TypeError("expected memory address");
        }
    }

    @Override
    public PyObject fastGetDict() {
        return dict;
    }

    @Override
    public PyObject getDict() {
        return dict;
    }

    @Override
    public PyObject __call__(PyObject[] args, String[] keywords) {
        return getInvoker().invoke(args);
    }

    @Override
    public PyObject __call__() {
        return getInvoker().invoke();
    }

    @Override
    public PyObject __call__(PyObject arg0) {
        return getInvoker().invoke(arg0);
    }

    @Override
    public PyObject __call__(PyObject arg0, PyObject arg1) {
        return getInvoker().invoke(arg0, arg1);
    }

    @Override
    public PyObject __call__(PyObject arg0, PyObject arg1, PyObject arg2) {
        return getInvoker().invoke(arg0, arg1, arg2);
    }


    @ExposedGet(name = "_jffi_restype")
    public PyObject getReturnType() {
        return this.returnType;
    }


    @ExposedSet(name = "_jffi_restype")
    public void setReturnType(PyObject returnType) {
        if (!(returnType instanceof Type)) {
            throw Py.TypeError("wrong argument type (expected jffi.Type)");
        }

        this.invoker = null; // invalidate old invoker
        this.returnType = (Type) returnType;
    }
    
    @ExposedGet(name = "_jffi_argtypes")
    public PyObject getParameterTypes() {
        return new PyList(parameterTypes != null ? parameterTypes : new Type[0]);
    }

    @ExposedSet(name = "_jffi_argtypes")
    public void setParameterTypes(PyObject parameterTypes) {
        // Removing the parameter types defaults back to varargs
        if (parameterTypes == Py.None) {
            this.parameterTypes = null;
            return;
        }

        if (!(parameterTypes instanceof PyList)) {
            throw Py.TypeError("wrong argument type (expected list of jffi.Type)");
        }

        Type[] paramTypes = new Type[((PyList) parameterTypes).size()];
        for (int i = 0; i < paramTypes.length; ++i) {
            PyObject t = ((PyList) parameterTypes).pyget(i);
            if (!(t instanceof Type)) {
                throw Py.TypeError(String.format("wrong argument type for parameter %d (expected jffi.Type)", i));
            }
            paramTypes[i] = (Type) t;
        }

        this.invoker = null; // invalidate old invoker
        this.parameterTypes = paramTypes;
    }


    private final Invoker getInvoker() {
        if (invoker != null) {
            return invoker;
        }
        return createInvoker(address, returnType, parameterTypes);
    }

    private synchronized final Invoker createInvoker(long address, Type returnType, Type[] parameterTypes) {
        if (parameterTypes == null) {
            throw Py.NotImplementedError("variadic functions not supported yet;  specify a parameter list");
        }

        com.kenai.jffi.Type jffiReturnType = NativeType.jffiType(returnType.nativeType);
        com.kenai.jffi.Type[] jffiParamTypes = new com.kenai.jffi.Type[parameterTypes.length];
        for (int i = 0; i < jffiParamTypes.length; ++i) {
            jffiParamTypes[i] = NativeType.jffiType(parameterTypes[i].nativeType);
        }
        com.kenai.jffi.Function jffiFunction = new com.kenai.jffi.Function(address, jffiReturnType, jffiParamTypes);

        if (FastIntInvokerFactory.getFactory().isFastIntMethod(returnType, parameterTypes)) {
            invoker = FastIntInvokerFactory.getFactory().createInvoker(jffiFunction, parameterTypes, returnType);
        } else {
            invoker = DefaultInvokerFactory.getFactory().createInvoker(jffiFunction, parameterTypes, returnType);
        }
        
        return invoker;
    }
}
