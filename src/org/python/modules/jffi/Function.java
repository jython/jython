
package org.python.modules.jffi;

import com.kenai.jffi.CallingConvention;
import org.python.core.Py;
import org.python.core.PyList;
import org.python.core.PyNewWrapper;
import org.python.core.PyObject;
import org.python.core.PySequenceList;
import org.python.core.PyStringMap;
import org.python.core.PyType;
import org.python.expose.ExposedGet;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedSet;
import org.python.expose.ExposedType;

@ExposedType(name = "jffi.Function", base = PyObject.class)
public class Function extends BasePointer implements Pointer {
    public static final PyType TYPE = PyType.fromClass(Function.class);

    private final Pointer pointer;
    private final DynamicLibrary library;

    private final PyStringMap dict = new PyStringMap();

    private volatile PyObject restype = CType.INT;
    private volatile PyObject[] argtypes = null;
    private Invoker defaultInvoker;
    private Invoker compiledInvoker;
    private volatile JITHandle jitHandle;
    private volatile com.kenai.jffi.Function jffiFunction;

    @ExposedGet
    public PyObject errcheck = Py.None;

    @ExposedGet
    public final String name;
    
    Function(PyType type, Pointer address) {
        super(type);
        this.library = null;
        this.name = "<anonymous>";
        this.pointer = address;
        this.restype = type.__getattr__("_restype");
    }

    Function(PyType type, DynamicLibrary.Symbol sym) {
        super(type);
        this.library = sym.library;
        this.name = sym.name;
        this.pointer = sym;
        this.restype = type.__getattr__("_restype");
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

    @Override
    public PyObject __call__(PyObject arg0, PyObject arg1, PyObject arg2, PyObject arg3) {
        return getInvoker().invoke(arg0, arg1, arg2, arg3);
    }


    @ExposedGet(name = "restype")
    public PyObject getResultType() {
        return this.restype;
    }

    @ExposedSet(name = "restype")
    public void setResultType(PyObject restype) {
        invalidateInvoker();
        this.restype = restype;
    }

    @ExposedGet(name = "argtypes")
    public PyObject getArgTypes() {
        return new PyList(argtypes != null ? argtypes : new PyObject[0]);
    }

    @ExposedSet(name = "argtypes")
    public void setArgTypes(PyObject parameterTypes) {
        invalidateInvoker();

        // Removing the parameter types defaults back to varargs
        if (parameterTypes == Py.None) {
            this.argtypes = null;
            return;
        }

        if (!(parameterTypes instanceof PySequenceList)) {
            throw Py.TypeError("wrong argument type (expected list or tuple)");
        }

        PySequenceList paramList = (PySequenceList) parameterTypes;
        argtypes = new PyObject[paramList.size()];
        for (int i = 0; i < argtypes.length; ++i) {
            argtypes[i] = paramList.pyget(i);
        }
    }

    @ExposedSet(name = "errcheck")
    public void errcheck(PyObject errcheck) {
        invalidateInvoker();
        this.errcheck = errcheck;
    }
    @Override
    public boolean __nonzero__() {
        return !getMemory().isNull();
    }

    protected final Invoker getInvoker() {
        return compiledInvoker != null ? compiledInvoker : tryCompilation();
    }

    private synchronized Invoker tryCompilation() {
        if (compiledInvoker != null) {
            return compiledInvoker;
        }

        if (argtypes == null) {
            throw Py.NotImplementedError("variadic functions not supported yet;  specify a parameter list");
        }

        CType cResultType = CType.typeOf(restype);
        CType[] cParameterTypes = new CType[argtypes.length];
        for (int i = 0; i < cParameterTypes.length; i++) {
            cParameterTypes[i] = CType.typeOf(argtypes[i]);
        }

        if (jitHandle == null) {
            jitHandle = JITCompiler.getInstance().getHandle(cResultType, cParameterTypes, CallingConvention.DEFAULT, false);
        }

        if (jffiFunction == null) {
            com.kenai.jffi.Type jffiReturnType = Util.jffiType(cResultType);
            com.kenai.jffi.Type[] jffiParamTypes = new com.kenai.jffi.Type[argtypes.length];

            for (int i = 0; i < jffiParamTypes.length; ++i) {
                jffiParamTypes[i] = Util.jffiType(cParameterTypes[i]);
            }

            jffiFunction = new com.kenai.jffi.Function(getMemory().getAddress(), jffiReturnType, jffiParamTypes);
        }

        if (defaultInvoker == null) {
            Invoker invoker = DefaultInvokerFactory.getFactory().createInvoker(jffiFunction, restype, argtypes);
            defaultInvoker = errcheck != Py.None ? new ErrCheckInvoker(invoker, errcheck) : invoker;
        }

        Invoker invoker = jitHandle.compile(jffiFunction, null, new NativeDataConverter[0]);
        if (invoker != null) {
            return compiledInvoker = errcheck != Py.None ? new ErrCheckInvoker(invoker, errcheck) : invoker;
        }

        //
        // Once compilation has failed, always fallback to the default invoker
        //
        if (jitHandle.compilationFailed()) {
            compiledInvoker = defaultInvoker;
        }

        return defaultInvoker;
    }

    private synchronized void invalidateInvoker() {
        // null out the invoker - it will be regenerated on next invocation
        this.defaultInvoker = null;
        this.compiledInvoker = null;
        this.jitHandle = null;
        this.jffiFunction = null;
    }

    private static final class ErrCheckInvoker extends Invoker {
        private final Invoker invoker;
        private final PyObject errcheck;

        public ErrCheckInvoker(Invoker invoker, PyObject errcheck) {
            this.invoker = invoker;
            this.errcheck = errcheck;
        }

        public PyObject invoke(PyObject[] args) {
            return errcheck.__call__(invoker.invoke(args));
        }

        public PyObject invoke() {
            return errcheck.__call__(invoker.invoke());
        }

        public PyObject invoke(PyObject arg1) {
            return errcheck.__call__(invoker.invoke(arg1));
        }

        public PyObject invoke(PyObject arg1, PyObject arg2) {
            return errcheck.__call__(invoker.invoke(arg1, arg2));
        }

        public PyObject invoke(PyObject arg1, PyObject arg2, PyObject arg3) {
            return errcheck.__call__(invoker.invoke(arg1, arg2, arg3));
        }

        public PyObject invoke(PyObject arg1, PyObject arg2, PyObject arg3, PyObject arg4) {
            return errcheck.__call__(invoker.invoke(arg1, arg2, arg3, arg4));
        }

        public PyObject invoke(PyObject arg1, PyObject arg2, PyObject arg3, PyObject arg4, PyObject arg5) {
            return errcheck.__call__(invoker.invoke(arg1, arg2, arg3, arg4, arg5));
        }

        public PyObject invoke(PyObject arg1, PyObject arg2, PyObject arg3, PyObject arg4, PyObject arg5, PyObject arg6) {
            return errcheck.__call__(invoker.invoke(arg1, arg2, arg3, arg4, arg5, arg6));
        }
    }
}
