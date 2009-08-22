
package org.python.modules.jffi;

import com.kenai.jffi.Function;
import com.kenai.jffi.Platform;
import org.python.core.Py;
import org.python.core.PyObject;


/**
 * A factory which generates {@link Invoker} instances that are optimized for
 * 32 bit integer and float parameters / result types with 3 or less parameters.
 *
 * Technical background:  Instead of trying to cram all calls down a generic call
 * path, then figuring out how to convert the parameters in the native code on
 * each call, jffi supplies arity and type specific call paths that can be
 * optimized ahead of time by the native code.
 *
 * The downside of this approach is more java code to wire up the functions and
 * call them using the arity+type specific paths, but in the case of int and float
 * parameters, it can result in more than a 100% speed boost over the generic path.
 */
public class FastIntInvokerFactory {
    private static final class SingletonHolder {
        private static final FastIntInvokerFactory INSTANCE = new FastIntInvokerFactory();
    }

    private FastIntInvokerFactory() {}

    public static final FastIntInvokerFactory getFactory() {
        return SingletonHolder.INSTANCE;
    }

    /**
     * Interface used to convert from a python object to a native integer
     */
    private static interface IntParameterConverter {
        int intValue(PyObject value);
    }

    /**
     * Interface used to convert from a native integer to a python object
     */
    private static interface IntResultConverter {
        PyObject pyValue(int value);
    }

    /**
     * Tests if a combination of result and parameter types can be called using
     * an {@link Invoker} created by this factory.
     *
     * @param returnType The return type of the native function.
     * @param parameterTypes The parameter types of the native function.
     * @return <tt>true</tt> if the method can be handled as a fast int method.
     */
    final boolean isFastIntMethod(Type returnType, Type[] parameterTypes) {
        for (int i = 0; i < parameterTypes.length; ++i) {
            if (!isFastIntParam(parameterTypes[i])) {
                return false;
            }
        }
        return parameterTypes.length <= 3 && isFastIntResult(returnType);
    }

    /**
     * Tests if the type can be returned as an integer result.
     *
     * @param type The result type.
     * @return <tt>true</tt> if <tt>type</tt> can be returned as an integer.
     */
    final boolean isFastIntResult(Type type) {
        if (type instanceof Type.Builtin) {
            switch (type.getNativeType()) {
                case VOID:
                case BYTE:
                case UBYTE:
                case SHORT:
                case USHORT:
                case INT:
                case UINT:
                    return true;

                case LONG:
                case ULONG:
                    return Platform.getPlatform().longSize() == 32;

                case FLOAT:
                    return Platform.getPlatform().getCPU() == Platform.CPU.I386
                            || Platform.getPlatform().getCPU() == Platform.CPU.X86_64;
            }
        }
        return false;
    }


    /**
     * Tests if the type can be passed as an integer parameter.
     *
     * @param type The parameter type.
     * @return <tt>true</tt> if <tt>type</tt> can be passed as an integer.
     */
    final boolean isFastIntParam(Type paramType) {
        if (paramType instanceof Type.Builtin) {
            switch (paramType.getNativeType()) {
                case BYTE:
                case UBYTE:
                case SHORT:
                case USHORT:
                case INT:
                case UINT:
                    return true;

                case LONG:
                case ULONG:
                    return Platform.getPlatform().longSize() == 32;

                case FLOAT:
                    return Platform.getPlatform().getCPU() == Platform.CPU.I386
                            || Platform.getPlatform().getCPU() == Platform.CPU.X86_64;
            }
        }
        return false;
    }


    /**
     * Creates a new <tt>Invoker</tt> instance for the given function, with the
     * given parameter types and return type.
     *
     * @param function The JFFI function to wrap
     * @param parameterTypes The parameter types the function will be called with
     * @param returnType The result type the function will return
     * @return A new {@link Invoker} instance.
     */
    final Invoker createInvoker(Function function, Type[] parameterTypes, Type returnType) {
        IntParameterConverter[] parameterConverters = new IntParameterConverter[parameterTypes.length];
        
        for (int i = 0; i < parameterConverters.length; ++i) {
            parameterConverters[i] = getIntParameterConverter(parameterTypes[i]);
        }
        if (returnType.nativeType == NativeType.FLOAT) {
            switch (parameterTypes.length) {
                case 0:
                    return new FastFloatInvokerZero(function, null, parameterConverters);
                case 1:
                    return new FastFloatInvokerOne(function, null, parameterConverters);
                case 2:
                    return new FastFloatInvokerTwo(function, null, parameterConverters);
                case 3:
                    return new FastFloatInvokerThree(function, null, parameterConverters);
            }
        } else {
            IntResultConverter resultConverter = getIntResultConverter(returnType);
            switch (parameterTypes.length) {
                case 0:
                    return new FastIntInvokerZero(function, resultConverter, parameterConverters);
                case 1:
                    return new FastIntInvokerOne(function, resultConverter, parameterConverters);
                case 2:
                    return new FastIntInvokerTwo(function, resultConverter, parameterConverters);
                case 3:
                    return new FastIntInvokerThree(function, resultConverter, parameterConverters);
            }
        }
        throw Py.RuntimeError("Fast Integer invoker does not support functions with arity=" + parameterTypes.length);
    }

    /**
     * Gets a python object to integer parameter converter.
     *
     * @param type The python C type
     * @return An <tt>IntParameterConverter</tt> instance.
     */
    final IntParameterConverter getIntParameterConverter(Type type) {
        if (type instanceof Type.Builtin) {
            return getIntParameterConverter(type.getNativeType());
        }
        throw Py.TypeError("cannot convert objects of type " + type + " to int");
    }


    /**
     * Gets a python object to integer parameter converter.
     *
     * @param type The object type.
     * @return An <tt>IntParameterConverter</tt> instance.
     */
    final IntParameterConverter getIntParameterConverter(NativeType type) {
        switch (type) {
            case BYTE:
                return Signed8ParameterConverter.INSTANCE;
            
            case UBYTE:
                return Unsigned8ParameterConverter.INSTANCE;
            
            case SHORT:
                return Signed16ParameterConverter.INSTANCE;
            
            case USHORT:
                return Unsigned16ParameterConverter.INSTANCE;
            
            case INT:
                return Signed32ParameterConverter.INSTANCE;
            
            case UINT:
                return Unsigned32ParameterConverter.INSTANCE;

            case LONG:
                if (Platform.getPlatform().longSize() == 32) {
                    return Signed32ParameterConverter.INSTANCE;
                }
                break;

            case ULONG:
                if (Platform.getPlatform().longSize() == 32) {
                    return Unsigned32ParameterConverter.INSTANCE;
                }
                break;

            case FLOAT:
                if (Platform.getPlatform().getCPU() == Platform.CPU.I386
                        || Platform.getPlatform().getCPU() == Platform.CPU.X86_64) {
                    return Float32ParameterConverter.INSTANCE;
                }
                break;
            default:
                break;
        }

        throw Py.TypeError("cannot convert objects of type " + type + " to int");
    }
    

    /**
     * Gets a int to python object result converter for the type.
     *
     * @param type The object type.
     * @return An <tt>IntResultConverter</tt> instance.
     */
    final IntResultConverter getIntResultConverter(Type type) {
        return type instanceof Type.Builtin ? getIntResultConverter(type.getNativeType()) : null;
    }

    
    /**
     * Gets a int to python object result converter for the type.
     *
     * @param type The object type.
     * @return An <tt>IntResultConverter</tt> instance.
     */
    final IntResultConverter getIntResultConverter(NativeType type) {
        switch (type) {
            case VOID:
                return VoidResultConverter.INSTANCE;
            
            case BYTE:
                return Signed8ResultConverter.INSTANCE;
            
            case UBYTE:
                return Unsigned8ResultConverter.INSTANCE;
            
            case SHORT:
                return Signed16ResultConverter.INSTANCE;
            
            case USHORT:
                return Unsigned16ResultConverter.INSTANCE;
            
            case INT:
                return Signed32ResultConverter.INSTANCE;
            
            case UINT:
                return Unsigned32ResultConverter.INSTANCE;

            case LONG:
                if (Platform.getPlatform().longSize() == 32) {
                    return Signed32ResultConverter.INSTANCE;
                }
                break;

            case ULONG:
                if (Platform.getPlatform().longSize() == 32) {
                    return Unsigned32ResultConverter.INSTANCE;
                }
                break;

            default:
                break;
        }
        throw new IllegalArgumentException("Cannot convert objects of type " + type + " from int");
    }

    /**
     * Base class for all fast-int {@link Invoker} subclasses
     */
    private static abstract class BaseFastIntInvoker implements Invoker {
        final com.kenai.jffi.Invoker jffiInvoker = com.kenai.jffi.Invoker.getInstance();
        final Function function;
        final IntResultConverter resultConverter;
        final int arity;
        final IntParameterConverter c0, c1, c2;

        BaseFastIntInvoker(Function function, IntResultConverter resultConverter,
                IntParameterConverter[] parameterConverters) {
            this.function = function;
            this.resultConverter = resultConverter;
            this.arity = parameterConverters.length;
            c0 = parameterConverters.length > 0 ? parameterConverters[0] : null;
            c1 = parameterConverters.length > 1 ? parameterConverters[1] : null;
            c2 = parameterConverters.length > 2 ? parameterConverters[2] : null;
        }

        final void checkArity(PyObject[] args) {
            checkArity(args.length);
        }

        final void checkArity(int got) {
            if (got != arity) {
                throw Py.TypeError(String.format("__call__() takes exactly %d arguments (%d given)", arity, got));
            }
        }
        public PyObject invoke(PyObject[] args) {
            checkArity(args);
            switch (arity) {
                case 0:
                    return invoke();
                case 1:
                    return invoke(args[0]);
                case 2:
                    return invoke(args[0], args[1]);
                case 3:
                    return invoke(args[0], args[1], args[2]);
                default:
                    throw Py.RuntimeError("invalid fast-int arity");
            }
        }

        public PyObject invoke() {
            checkArity(0);
            return Py.None;
        }

        public PyObject invoke(PyObject arg1) {
            checkArity(1);
            return Py.None;
        }

        public PyObject invoke(PyObject arg1, PyObject arg2) {
            checkArity(2);
            return Py.None;
        }

        public PyObject invoke(PyObject arg1, PyObject arg2, PyObject arg3) {
            checkArity(3);
            return Py.None;
        }

    }

    /**
     * Fast-int invoker that takes no parameters.
     */
    private static final class FastIntInvokerZero extends BaseFastIntInvoker {

        public FastIntInvokerZero(Function function, IntResultConverter resultConverter,
                IntParameterConverter parameterConverters[]) {
            super(function, resultConverter, parameterConverters);
        }

        @Override
        public final PyObject invoke() {
            return resultConverter.pyValue(jffiInvoker.invokeVrI(function));
        }
    }

    /**
     * Fast-int invoker that takes a single parameter
     */
    private static final class FastIntInvokerOne extends BaseFastIntInvoker {
        public FastIntInvokerOne(Function function, IntResultConverter resultConverter,
                IntParameterConverter parameterConverters[]) {
            super(function, resultConverter, parameterConverters);
            
        }

        @Override
        public final PyObject invoke(PyObject arg0) {
            return resultConverter.pyValue(jffiInvoker.invokeIrI(function,
                    c0.intValue(arg0)));
        }
    }


    /**
     * Fast-int invoker that takes two parameters
     */
    private static final class FastIntInvokerTwo extends BaseFastIntInvoker {
        
        public FastIntInvokerTwo(Function function, IntResultConverter resultConverter,
                IntParameterConverter parameterConverters[]) {
            super(function, resultConverter, parameterConverters);
        }

        @Override
        public PyObject invoke(PyObject arg0, PyObject arg1) {
            return resultConverter.pyValue(jffiInvoker.invokeIIrI(function,
                    c0.intValue(arg0), c1.intValue(arg1)));
        }
    }

    /**
     * Fast-int invoker that takes three parameters
     */
    private static final class FastIntInvokerThree extends BaseFastIntInvoker {
        
        public FastIntInvokerThree(Function function, IntResultConverter resultConverter,
                IntParameterConverter parameterConverters[]) {
            super(function, resultConverter, parameterConverters);
        }

        @Override
        public PyObject invoke(PyObject arg0, PyObject arg1, PyObject arg2) {
            return resultConverter.pyValue(jffiInvoker.invokeIIIrI(function,
                    c0.intValue(arg0), c1.intValue(arg1), c2.intValue(arg2)));
        }
    }

    /**
     * Fast-int invoker that takes no parameters and returns a float
     */
    private static final class FastFloatInvokerZero extends BaseFastIntInvoker {

        public FastFloatInvokerZero(Function function, IntResultConverter resultConverter,
                IntParameterConverter parameterConverters[]) {
            super(function, resultConverter, parameterConverters);
        }

        @Override
        public final PyObject invoke() {
            return Py.newFloat(jffiInvoker.invokeVrF(function));
        }
    }

    /**
     * Fast-int invoker that takes one parameter and returns a float
     */
    private static final class FastFloatInvokerOne extends BaseFastIntInvoker {

        public FastFloatInvokerOne(Function function, IntResultConverter resultConverter,
                IntParameterConverter parameterConverters[]) {
            super(function, resultConverter, parameterConverters);
        }

        @Override
        public final PyObject invoke(PyObject arg0) {
            return Py.newFloat(jffiInvoker.invokeIrF(function,
                    c0.intValue(arg0)));
        }
    }


    /**
     * Fast-int invoker that takes two parameters and returns a float
     */
    private static final class FastFloatInvokerTwo extends BaseFastIntInvoker {

        public FastFloatInvokerTwo(Function function, IntResultConverter resultConverter,
                IntParameterConverter parameterConverters[]) {
            super(function, resultConverter, parameterConverters);
        }

        @Override
        public PyObject invoke(PyObject arg0, PyObject arg1) {
            return Py.newFloat(jffiInvoker.invokeIIrF(function,
                    c0.intValue(arg0), c1.intValue(arg1)));
        }
    }

    /**
     * Fast-int invoker that takes three parameters and returns a float
     */
    private static final class FastFloatInvokerThree extends BaseFastIntInvoker {

        public FastFloatInvokerThree(Function function, IntResultConverter resultConverter,
                IntParameterConverter parameterConverters[]) {
            super(function, resultConverter, parameterConverters);
        }

        @Override
        public PyObject invoke(PyObject arg0, PyObject arg1, PyObject arg2) {
            return Py.newFloat(jffiInvoker.invokeIIIrF(function,
                    c0.intValue(arg0), c1.intValue(arg1), c2.intValue(arg2)));
        }
    }

    /**
     * Base class for all fast-int result converters
     */
    static abstract class BaseResultConverter implements IntResultConverter {
        
    }

    /**
     * Converts a native void result into into a python None instance
     */
    static final class VoidResultConverter extends BaseResultConverter {
        public static final IntResultConverter INSTANCE = new VoidResultConverter();
        public final PyObject pyValue(int value) {
            return Py.None;
        }
    }

    /**
     * Converts a native signed byte result into into a python integer instance
     */
    static final class Signed8ResultConverter extends BaseResultConverter {
        public static final IntResultConverter INSTANCE = new Signed8ResultConverter();
        public final PyObject pyValue(int value) {
            return Util.newSigned8(value);
        }
    }

    /**
     * Converts a native unsigned byte result into into a python integer instance
     */
    static final class Unsigned8ResultConverter extends BaseResultConverter {
        public static final IntResultConverter INSTANCE = new Unsigned8ResultConverter();
        public final PyObject pyValue(int value) {
            return Util.newUnsigned8(value);
        }
    }

    /**
     * Converts a native signed short result into into a python integer instance
     */
    static final class Signed16ResultConverter extends BaseResultConverter {
        public static final IntResultConverter INSTANCE = new Signed16ResultConverter();
        public final PyObject pyValue(int value) {
            return Util.newSigned16(value);
        }
    }

    /**
     * Converts a native unsigned short result into into a python integer instance
     */
    static final class Unsigned16ResultConverter extends BaseResultConverter {
        public static final IntResultConverter INSTANCE = new Unsigned16ResultConverter();
        public final PyObject pyValue(int value) {
            return Util.newUnsigned16(value);
        }
    }

    /**
     * Converts a native signed int result into into a python integer instance
     */
    static final class Signed32ResultConverter extends BaseResultConverter {
        public static final IntResultConverter INSTANCE = new Signed32ResultConverter();
        public final PyObject pyValue(int value) {
            return Util.newSigned32(value);
        }
    }

    /**
     * Converts a native unsigned int result into into a python integer instance
     */
    static final class Unsigned32ResultConverter extends BaseResultConverter {
        public static final IntResultConverter INSTANCE = new Unsigned32ResultConverter();
        public final PyObject pyValue(int value) {
            return Util.newUnsigned32(value);
        }
    }

    /**
     * Base class for all integer parameter converters.
     */
    static abstract class BaseParameterConverter implements IntParameterConverter {
    }

    /**
     * Converter for python signed byte to native int
     */
    static final class Signed8ParameterConverter extends BaseParameterConverter {
        public static final IntParameterConverter INSTANCE = new Signed8ParameterConverter();
        public final int intValue(PyObject obj) {
            return Util.int8Value(obj);
        }
    }

    /**
     * Converter for python unsigned byte to native int
     */
    static final class Unsigned8ParameterConverter extends BaseParameterConverter {
        public static final IntParameterConverter INSTANCE = new Unsigned8ParameterConverter();
        public final int intValue(PyObject obj) {
            return Util.uint8Value(obj);
        }
    }

    /**
     * Converter for python signed short to native int
     */
    static final class Signed16ParameterConverter extends BaseParameterConverter {
        public static final IntParameterConverter INSTANCE = new Signed16ParameterConverter();
        public final int intValue(PyObject obj) {
            return Util.int16Value(obj);
        }
    }

    /**
     * Converter for python unsigned short to native int
     */
    static final class Unsigned16ParameterConverter extends BaseParameterConverter {
        public static final IntParameterConverter INSTANCE = new Unsigned16ParameterConverter();
        public final int intValue(PyObject obj) {
            return Util.uint16Value(obj);
        }
    }

    /**
     * Converter for python signed int to native int
     */
    static final class Signed32ParameterConverter extends BaseParameterConverter {
        public static final IntParameterConverter INSTANCE = new Signed32ParameterConverter();
        public final int intValue(PyObject obj) {
            return Util.int32Value(obj);
        }
    }

    /**
     * Converter for python unsigned int to native int
     */
    static final class Unsigned32ParameterConverter extends BaseParameterConverter {
        public static final IntParameterConverter INSTANCE = new Unsigned32ParameterConverter();
        public final int intValue(PyObject obj) {
            return Util.uint32Value(obj);
        }
    }

    /**
     * Converter for python float to native int parameter
     */
    static final class Float32ParameterConverter extends BaseParameterConverter {
        public static final IntParameterConverter INSTANCE = new Float32ParameterConverter();
        public final int intValue(PyObject obj) {
            return Float.floatToIntBits((float) obj.asDouble());
        }
    }
}
