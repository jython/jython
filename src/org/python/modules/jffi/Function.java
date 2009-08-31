
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

@ExposedType(name = "jffi.Function", base = PyObject.class)
public class Function extends PyObject implements Pointer {
    public static final PyType TYPE = PyType.fromClass(Function.class);

    private final Pointer pointer;
    private final DynamicLibrary library;

    private final PyStringMap dict = new PyStringMap();
    
    private volatile CType returnType = CType.INT;
    private volatile CType[] parameterTypes = null;
    private Invoker invoker = null;

    @ExposedGet
    public final String name;

    @ExposedGet
    public final long address;

    @ExposedGet
    @ExposedSet
    public PyObject restype;

    Function(PyType type, Pointer address) {
        super(type);
        this.address = address.getAddress();
        this.library = null;
        this.name = "<anonymous>";
        this.pointer = address;
    }

    Function(PyType type, DynamicLibrary.Symbol sym) {
        super(type);
        this.library = sym.library;
        this.name = sym.name;
        this.pointer = sym;
        this.address = sym.getAddress();
    }

    @ExposedNew
    public static PyObject Function_new(PyNewWrapper new_, boolean init, PyType subtype,
            PyObject[] args, String[] keywords) {

        if (args[0] instanceof Pointer) {
            if (args[0] instanceof DynamicLibrary.Symbol) {
                return new Function(subtype, (DynamicLibrary.Symbol) args[0]);
            } else {
                return new Function(subtype, (Pointer) args[0]);
            }
        } else {
            throw Py.TypeError("expected memory address");
        }
    }

    public long getAddress() {
        return address;
    }

    public DirectMemory getMemory() {
        return pointer.getMemory();
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
        if (!(returnType instanceof CType)) {
            throw Py.TypeError("wrong argument type (expected jffi.Type)");
        }

        this.invoker = null; // invalidate old invoker
        this.returnType = (CType) returnType;
    }
    
    @ExposedGet(name = "_jffi_argtypes")
    public PyObject getParameterTypes() {
        return new PyList(parameterTypes != null ? parameterTypes : new CType[0]);
    }

    @ExposedSet(name = "_jffi_argtypes")
    public void setParameterTypes(PyObject parameterTypes) {
        this.invoker = null; // invalidate old invoker

        // Removing the parameter types defaults back to varargs
        if (parameterTypes == Py.None) {
            this.parameterTypes = null;
            return;
        }

        if (!(parameterTypes instanceof PyList)) {
            throw Py.TypeError("wrong argument type (expected list of jffi.Type)");
        }

        CType[] paramTypes = new CType[((PyList) parameterTypes).size()];
        for (int i = 0; i < paramTypes.length; ++i) {
            PyObject t = ((PyList) parameterTypes).pyget(i);
            if (!(t instanceof CType)) {
                throw Py.TypeError(String.format("wrong argument type for parameter %d (expected jffi.Type)", i));
            }
            paramTypes[i] = (CType) t;
        }

        this.parameterTypes = paramTypes;
    }

    @Override
    public boolean __nonzero__() {
        return !getMemory().isNull();
    }

    private final Invoker getInvoker() {
        if (invoker != null) {
            return invoker;
        }
        return createInvoker(address, returnType, parameterTypes);
    }

    private synchronized final Invoker createInvoker(long address, CType returnType, CType[] parameterTypes) {
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
