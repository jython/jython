package org.python.modules.jffi;

import org.python.core.Py;
import org.python.core.PyInteger;
import org.python.core.PyLong;
import org.python.core.PyObject;

import java.math.BigInteger;

/**
 * 
 */
public final class JITRuntime {
    private JITRuntime() {}

    public static int pointerValue32(PyObject ptr) {
        return (int) ((Pointer) ptr).getMemory().getAddress();
    }

    public static long pointerValue64(PyObject ptr) {
        return ((Pointer) ptr).getMemory().getAddress();
    }

    public static int intValue(PyObject parameter) {
        if (parameter instanceof PyInteger) {
            return ((PyInteger) parameter).getValue();

        } else if (parameter instanceof PyLong) {
            return ((PyLong) parameter).getValue().intValue();

        } else {
            return (int) __long__value(parameter);
        }
    }
    public static long longValue(PyObject parameter) {
        if (parameter instanceof PyInteger) {
            return ((PyInteger) parameter).getValue();

        } else if (parameter instanceof PyLong) {
            return ((PyLong) parameter).getValue().longValue();

        } else {
            return __long__value(parameter);
        }
    }

    static final long __long__value(PyObject parameter) {
        PyObject value = parameter.__long__();

        if (value instanceof PyLong) {
            return ((PyLong) value).getValue().longValue();

        } else if (value instanceof PyInteger) {
            return ((PyInteger) value).getValue();
        }

        throw Py.TypeError("invalid __long__() result");
    }

    public static int boolValue32(PyObject parameter) {
        return parameter.__nonzero__() ? 1 : 0;
    }

    public static long boolValue64(PyObject parameter) {
        return parameter.__nonzero__() ? 1L : 0L;
    }

    public static int s8Value32(PyObject parameter) {
        return (byte) intValue(parameter);
    }

    public static long s8Value64(PyObject parameter) {
        return (byte) intValue(parameter);
    }

    public static int u8Value32(PyObject parameter) {
        return intValue(parameter) & 0xff;
    }

    public static long u8Value64(PyObject parameter) {
        return ((long) intValue(parameter)) & 0xffL;
    }

    public static int s16Value32(PyObject parameter) {
        return (short) intValue(parameter);
    }

    public static long s16Value64(PyObject parameter) {
        return (short) intValue(parameter);
    }

    public static int u16Value32(PyObject parameter) {
        return intValue(parameter) & 0xffff;
    }

    public static long u16Value64(PyObject parameter) {
        return ((long) intValue(parameter)) & 0xffffL;
    }


    public static int s32Value32(PyObject parameter) {
        return intValue(parameter);
    }

    public static long s32Value64(PyObject parameter) {
        return intValue(parameter);
    }

    public static int u32Value32(PyObject parameter) {
        return intValue(parameter);
    }

    public static long u32Value64(PyObject parameter) {
        return ((long) intValue(parameter)) & 0xffffffffL;
    }

    public static long s64Value64(PyObject parameter) {
        return longValue(parameter);
    }

    public static long u64Value64(PyObject parameter) {
        return longValue(parameter);
    }

    public static int float2int32(PyObject parameter) {
        return Float.floatToRawIntBits((float) parameter.asDouble());
    }

    public static long float2int64(PyObject parameter) {
        return Float.floatToRawIntBits((float) parameter.asDouble());
    }

    public static long double2long64(PyObject parameter) {
        return Double.doubleToRawLongBits(parameter.asDouble());
    }
    
    
    public static PyObject newSigned8(byte value) {
        return Py.newInteger(value);
    }

    public static PyObject newUnsigned8(byte value) {
        return Py.newInteger(value < 0 ? (long)((value & 0x7FL) + 0x80L) : value);
    }

    public static PyObject newSigned16(short value) {
        return Py.newInteger(value);
    }

    public static PyObject newUnsigned16(short value) {
        return Py.newInteger(value < 0 ? (long)((value & 0x7FFFL) + 0x8000L) : value);
    }

    public static PyObject newSigned32(int value) {
        return Py.newInteger(value);
    }

    public static PyObject newUnsigned32(int value) {
        return Py.newInteger(value < 0 ? (long)((value & 0x7FFFFFFFL) + 0x80000000L) : value);
    }

    public static PyObject newSigned64(long value) {
        return Py.newInteger(value);
    }

    private static final BigInteger UINT64_BASE = BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE);
    public static PyObject newUnsigned64(long value) {
        return value < 0
                ? Py.newLong(BigInteger.valueOf(value & 0x7fffffffffffffffL).add(UINT64_BASE))
                : Py.newInteger(value);
    }

    public static PyObject newFloat32(int value) {
        return Py.newFloat(Float.intBitsToFloat(value));
    }

    public static PyObject newFloat64(long value) {
        return Py.newFloat(Double.longBitsToDouble(value));
    }
}
