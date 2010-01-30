
package org.python.modules.jffi;

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

    private volatile PyObject restype = Py.None;
    private volatile PyObject[] argtypes = null;
    private Invoker invoker = null;

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


    @ExposedGet(name = "restype")
    public PyObject getResultType() {
        return this.restype;
    }

    @ExposedSet(name = "restype")
    public void setResultType(PyObject restype) {
        this.invoker = null; // invalidate old invoker
        this.restype = restype;
    }

    @ExposedGet(name = "argtypes")
    public PyObject getArgTypes() {
        return new PyList(argtypes != null ? argtypes : new PyObject[0]);
    }

    @ExposedSet(name = "argtypes")
    public void setArgTypes(PyObject parameterTypes) {
        this.invoker = null; // invalidate old invoker

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
        this.invoker = null; // invalidate old invoker
        this.errcheck = errcheck;
    }
    @Override
    public boolean __nonzero__() {
        return !getMemory().isNull();
    }

    private final Invoker getInvoker() {
        if (invoker != null) {
            return invoker;
        }
        return createInvoker();
    }

    private synchronized final Invoker createInvoker() {
        if (argtypes == null) {
            throw Py.NotImplementedError("variadic functions not supported yet;  specify a parameter list");
        }

        com.kenai.jffi.Type jffiReturnType = Util.jffiType(CType.typeOf(restype));
        com.kenai.jffi.Type[] jffiParamTypes = new com.kenai.jffi.Type[argtypes.length];
        for (int i = 0; i < jffiParamTypes.length; ++i) {
            jffiParamTypes[i] = Util.jffiType(CType.typeOf(argtypes[i]));
        }
        com.kenai.jffi.Function jffiFunction = new com.kenai.jffi.Function(getMemory().getAddress(), jffiReturnType, jffiParamTypes);

        Invoker i;
        if (FastIntInvokerFactory.getFactory().isFastIntMethod(restype, argtypes)) {
            i = FastIntInvokerFactory.getFactory().createInvoker(jffiFunction, restype, argtypes);
        } else {
            i = DefaultInvokerFactory.getFactory().createInvoker(jffiFunction, restype, argtypes);
        }

        return invoker = errcheck != Py.None ? new ErrCheckInvoker(i, errcheck) : i;
    }

    private static final class ErrCheckInvoker implements Invoker {
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
    }
}
