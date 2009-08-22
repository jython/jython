package org.python.modules.jffi;

import com.kenai.jffi.ArrayFlags;
import com.kenai.jffi.Function;
import com.kenai.jffi.HeapInvocationBuffer;
import com.kenai.jffi.Platform;
import org.python.core.Py;
import org.python.core.PyNone;
import org.python.core.PyObject;

class DefaultInvokerFactory {
    private static final class SingletonHolder {
        public static final DefaultInvokerFactory INSTANCE = new DefaultInvokerFactory();
    }

    public static final DefaultInvokerFactory getFactory() {
        return SingletonHolder.INSTANCE;
    }

    private DefaultInvokerFactory() {}

    final Invoker createInvoker(com.kenai.jffi.Function function, Type[] parameterTypes, Type returnType) {
        ParameterMarshaller[] marshallers = new ParameterMarshaller[parameterTypes.length];
        
        for (int i = 0; i < marshallers.length; ++i) {
            marshallers[i] = getMarshaller(parameterTypes[i]);
        }


        if (returnType instanceof Type.Builtin) {
            switch (returnType.getNativeType()) {
                case VOID:
                    return new VoidInvoker(function, marshallers);

                case BYTE:
                    return new Signed8Invoker(function, marshallers);

                case UBYTE:
                    return new Unsigned8Invoker(function, marshallers);

                case SHORT:
                    return new Signed16Invoker(function, marshallers);

                case USHORT:
                    return new Unsigned16Invoker(function, marshallers);

                case INT:
                    return new Signed32Invoker(function, marshallers);

                case UINT:
                    return new Unsigned32Invoker(function, marshallers);

                case LONGLONG:
                    return new Signed64Invoker(function, marshallers);

                case ULONGLONG:
                    return new Unsigned64Invoker(function, marshallers);

                case LONG:
                    return Platform.getPlatform().longSize() == 32
                            ? new Signed32Invoker(function, marshallers)
                            : new Signed64Invoker(function, marshallers);

                case ULONG:
                    return Platform.getPlatform().longSize() == 32
                            ? new Unsigned32Invoker(function, marshallers)
                            : new Unsigned64Invoker(function, marshallers);
                case FLOAT:
                    return new FloatInvoker(function, marshallers);

                case DOUBLE:
                    return new DoubleInvoker(function, marshallers);

                case POINTER:
                    return new PointerInvoker(function, marshallers);

                default:
                    break;
            }
        }
        throw Py.RuntimeError("Unsupported return type: " + returnType);
        
    }

    private static final ParameterMarshaller getMarshaller(NativeType type) {
        switch (type) {

            case BYTE:
                return Signed8Marshaller.INSTANCE;
            case UBYTE:
                return Unsigned8Marshaller.INSTANCE;

            case SHORT:
                return Signed16Marshaller.INSTANCE;
            case USHORT:
                return Unsigned16Marshaller.INSTANCE;

            case INT:
                return Signed32Marshaller.INSTANCE;
            case UINT:
                return Unsigned32Marshaller.INSTANCE;

            case LONGLONG:
                return Signed64Marshaller.INSTANCE;
            case ULONGLONG:
                return Unsigned64Marshaller.INSTANCE;

            case LONG:
                return Platform.getPlatform().longSize() == 32
                        ? Signed32Marshaller.INSTANCE : Signed64Marshaller.INSTANCE;
            case ULONG:
                return Platform.getPlatform().longSize() == 32
                        ? Unsigned32Marshaller.INSTANCE : Unsigned64Marshaller.INSTANCE;

            case FLOAT:
                return FloatMarshaller.INSTANCE;

            case DOUBLE:
                return DoubleMarshaller.INSTANCE;

            case POINTER:
                return PointerMarshaller.INSTANCE;

            case STRING:
                return StringMarshaller.INSTANCE;
            default:
                throw Py.RuntimeError("Unsupported parameter type: " + type);
        }
    }

    private static final ParameterMarshaller getMarshaller(Type type) {
        if (type instanceof Type.Builtin) {
            return getMarshaller(type.getNativeType());
        } else {
            throw Py.RuntimeError("Unsupported parameter type: " + type);
        }
    }

    private static interface ParameterMarshaller {
        void marshal(HeapInvocationBuffer buffer, PyObject arg);
    }

    private static abstract class BaseInvoker implements Invoker {
        final Function jffiFunction;
        final com.kenai.jffi.Invoker jffiInvoker = com.kenai.jffi.Invoker.getInstance();
        final ParameterMarshaller[] marshallers;
        final int arity;

        public BaseInvoker(Function function, ParameterMarshaller[] marshallers) {
            this.jffiFunction= function;
            this.marshallers = marshallers;
            this.arity = marshallers.length;
        }

        final HeapInvocationBuffer convertArguments(PyObject[] args) {
            checkArity(args);
            HeapInvocationBuffer buffer = new HeapInvocationBuffer(jffiFunction);

            for (int i = 0; i < marshallers.length; ++i) {
                marshallers[i].marshal(buffer, args[i]);
            }

            return buffer;
        }

        public final PyObject invoke() {
            return invoke(new PyObject[0]);
        }

        public final PyObject invoke(PyObject arg0) {
            return invoke(new PyObject[] { arg0 });
        }

        public final PyObject invoke(PyObject arg0, PyObject arg1) {
            return invoke(new PyObject[] { arg0, arg1 });
        }

        public final PyObject invoke(PyObject arg0, PyObject arg1, PyObject arg2) {
            return invoke(new PyObject[] { arg0, arg1, arg2 });
        }

        final void checkArity(PyObject[] args) {
            checkArity(args.length);
        }

        final void checkArity(int got) {
            if (got != arity) {
                throw Py.TypeError(String.format("expected %d args; got %d", arity, got));
            }
        }
    }

    private static final class VoidInvoker extends BaseInvoker {

        public VoidInvoker(Function function, ParameterMarshaller[] marshallers) {
            super(function, marshallers);
        }

        public final PyObject invoke(PyObject[] args) {
            jffiInvoker.invokeInt(jffiFunction, convertArguments(args));

            return Py.None;
        }

    }
    private static final class Signed8Invoker extends BaseInvoker {

        public Signed8Invoker(Function function, ParameterMarshaller[] marshallers) {
            super(function, marshallers);
        }

        public final PyObject invoke(PyObject[] args) {
            return Util.newSigned8(jffiInvoker.invokeInt(jffiFunction, convertArguments(args)));
        }
    }

    private static final class Unsigned8Invoker extends BaseInvoker {

        public Unsigned8Invoker(Function function, ParameterMarshaller[] marshallers) {
            super(function, marshallers);
        }

        public final PyObject invoke(PyObject[] args) {
            return Util.newUnsigned8(jffiInvoker.invokeInt(jffiFunction, convertArguments(args)));
        }
    }

    private static final class Signed16Invoker extends BaseInvoker {

        public Signed16Invoker(Function function, ParameterMarshaller[] marshallers) {
            super(function, marshallers);
        }

        public final PyObject invoke(PyObject[] args) {
            return Util.newSigned16(jffiInvoker.invokeInt(jffiFunction, convertArguments(args)));
        }
    }

    private static final class Unsigned16Invoker extends BaseInvoker {

        public Unsigned16Invoker(Function function, ParameterMarshaller[] marshallers) {
            super(function, marshallers);
        }

        public final PyObject invoke(PyObject[] args) {
            return Util.newUnsigned16(jffiInvoker.invokeInt(jffiFunction, convertArguments(args)));
        }
    }

    private static final class Signed32Invoker extends BaseInvoker {

        public Signed32Invoker(Function function, ParameterMarshaller[] marshallers) {
            super(function, marshallers);
        }

        public final PyObject invoke(PyObject[] args) {
            return Util.newSigned32(jffiInvoker.invokeInt(jffiFunction, convertArguments(args)));
        }
    }

    private static final class Unsigned32Invoker extends BaseInvoker {

        public Unsigned32Invoker(Function function, ParameterMarshaller[] marshallers) {
            super(function, marshallers);
        }

        public final PyObject invoke(PyObject[] args) {
            return Util.newUnsigned32(jffiInvoker.invokeInt(jffiFunction, convertArguments(args)));
        }
    }

    private static final class Signed64Invoker extends BaseInvoker {

        public Signed64Invoker(Function function, ParameterMarshaller[] marshallers) {
            super(function, marshallers);
        }

        public final PyObject invoke(PyObject[] args) {
            return Util.newSigned64(jffiInvoker.invokeLong(jffiFunction, convertArguments(args)));
        }
    }

    private static final class Unsigned64Invoker extends BaseInvoker {

        public Unsigned64Invoker(Function function, ParameterMarshaller[] marshallers) {
            super(function, marshallers);
        }

        public final PyObject invoke(PyObject[] args) {
            return Util.newUnsigned64(jffiInvoker.invokeLong(jffiFunction, convertArguments(args)));
        }
    }

    private static final class FloatInvoker extends BaseInvoker {

        public FloatInvoker(Function function, ParameterMarshaller[] marshallers) {
            super(function, marshallers);
        }

        public final PyObject invoke(PyObject[] args) {
            return Py.newFloat(jffiInvoker.invokeFloat(jffiFunction, convertArguments(args)));
        }
    }

    private static final class DoubleInvoker extends BaseInvoker {

        public DoubleInvoker(Function function, ParameterMarshaller[] marshallers) {
            super(function, marshallers);
        }

        public final PyObject invoke(PyObject[] args) {
            return Py.newFloat(jffiInvoker.invokeDouble(jffiFunction, convertArguments(args)));
        }
    }

    private static final class PointerInvoker extends BaseInvoker {

        public PointerInvoker(Function function, ParameterMarshaller[] marshallers) {
            super(function, marshallers);
        }

        public final PyObject invoke(PyObject[] args) {
            return Py.newLong(jffiInvoker.invokeAddress(jffiFunction, convertArguments(args)));
        }
    }
    
    /*------------------------------------------------------------------------*/
    static abstract class BaseMarshaller implements ParameterMarshaller {
    }
    
    private static class Signed8Marshaller extends BaseMarshaller {
        public static final ParameterMarshaller INSTANCE = new Signed8Marshaller();

        public void marshal(HeapInvocationBuffer buffer, PyObject arg) {
            buffer.putByte(Util.int8Value(arg));
        }
    }

    private static class Unsigned8Marshaller extends BaseMarshaller {
        public static final ParameterMarshaller INSTANCE = new Unsigned8Marshaller();

        public void marshal(HeapInvocationBuffer buffer, PyObject arg) {
            buffer.putByte((byte) Util.uint8Value(arg));
        }
    }

    private static class Signed16Marshaller extends BaseMarshaller {
        public static final ParameterMarshaller INSTANCE = new Signed16Marshaller();

        public void marshal(HeapInvocationBuffer buffer, PyObject arg) {
            buffer.putShort(Util.int16Value(arg));
        }
    }

    private static class Unsigned16Marshaller extends BaseMarshaller {
        public static final ParameterMarshaller INSTANCE = new Unsigned16Marshaller();

        public void marshal(HeapInvocationBuffer buffer, PyObject arg) {
            buffer.putShort((short) Util.uint16Value(arg));
        }
    }

    private static class Signed32Marshaller extends BaseMarshaller {
        public static final ParameterMarshaller INSTANCE = new Signed32Marshaller();

        public void marshal(HeapInvocationBuffer buffer, PyObject arg) {
            buffer.putInt(Util.int32Value(arg));
        }
    }

    private static class Unsigned32Marshaller extends BaseMarshaller {
        public static final ParameterMarshaller INSTANCE = new Unsigned32Marshaller();

        public void marshal(HeapInvocationBuffer buffer, PyObject arg) {
            buffer.putInt((int) Util.int32Value(arg));
        }
    }

    private static class Signed64Marshaller extends BaseMarshaller {
        public static final ParameterMarshaller INSTANCE = new Signed64Marshaller();

        public void marshal(HeapInvocationBuffer buffer, PyObject arg) {
            buffer.putLong(Util.int64Value(arg));
        }
    }

    private static class Unsigned64Marshaller extends BaseMarshaller {
        public static final ParameterMarshaller INSTANCE = new Unsigned64Marshaller();

        public void marshal(HeapInvocationBuffer buffer, PyObject arg) {
            buffer.putLong(Util.uint64Value(arg));
        }
    }

    private static class FloatMarshaller extends BaseMarshaller {
        public static final ParameterMarshaller INSTANCE = new FloatMarshaller();

        public void marshal(HeapInvocationBuffer buffer, PyObject arg) {
            buffer.putFloat((float) arg.asDouble());
        }
    }

    private static class DoubleMarshaller extends BaseMarshaller {
        public static final ParameterMarshaller INSTANCE = new DoubleMarshaller();

        public void marshal(HeapInvocationBuffer buffer, PyObject arg) {
            buffer.putDouble(arg.asDouble());
        }
    }

    private static class StringMarshaller extends BaseMarshaller {
        public static final ParameterMarshaller INSTANCE = new StringMarshaller();

        public void marshal(HeapInvocationBuffer buffer, PyObject parameter) {
            if (parameter instanceof PyNone) {
                buffer.putAddress(0);
            } else {
                byte[] bytes = parameter.toString().getBytes();
                buffer.putArray(bytes, 0, bytes.length,
                        ArrayFlags.IN | ArrayFlags.NULTERMINATE);
            }
        }
    }

    private static class PointerMarshaller extends BaseMarshaller {
        public static final ParameterMarshaller INSTANCE = new PointerMarshaller();

        public void marshal(HeapInvocationBuffer buffer, PyObject parameter) {
            throw Py.NotImplementedError("POINTER parameters not implemented");
        }
    }

}
