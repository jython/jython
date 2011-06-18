package org.python.modules.jffi;

import org.python.core.Py;
import org.python.core.PyObject;

import java.math.BigInteger;

/**
 * 
 */
public final class JITRuntime {
    private static final com.kenai.jffi.MemoryIO IO = com.kenai.jffi.MemoryIO.getInstance();

    private JITRuntime() {}

    public static int pointerValue32(PyObject ptr) {
        return (int) ((Pointer) ptr).getMemory().getAddress();
    }

    public static long pointerValue64(PyObject ptr) {
        return ((Pointer) ptr).getMemory().getAddress();
    }

    public static int boolValue32(PyObject parameter) {
        return parameter.__nonzero__() ? 1 : 0;
    }

    public static long boolValue64(PyObject parameter) {
        return parameter.__nonzero__() ? 1L : 0L;
    }

    public static int s8Value32(PyObject parameter) {
        return (byte) Util.intValue(parameter);
    }

    public static long s8Value64(PyObject parameter) {
        return (byte) Util.intValue(parameter);
    }

    public static int u8Value32(PyObject parameter) {
        return Util.intValue(parameter) & 0xff;
    }

    public static long u8Value64(PyObject parameter) {
        return Util.intValue(parameter) & 0xff;
    }

    public static int s16Value32(PyObject parameter) {
        return (short) Util.intValue(parameter);
    }

    public static long s16Value64(PyObject parameter) {
        return (short) Util.intValue(parameter);
    }

    public static int u16Value32(PyObject parameter) {
        return Util.intValue(parameter) & 0xffff;
    }

    public static long u16Value64(PyObject parameter) {
        return Util.intValue(parameter) & 0xffff;
    }


    public static int s32Value32(PyObject parameter) {
        return Util.intValue(parameter);
    }

    public static long s32Value64(PyObject parameter) {
        return Util.intValue(parameter);
    }

    public static int u32Value32(PyObject parameter) {
        return Util.intValue(parameter);
    }

    public static long u32Value64(PyObject parameter) {
        return Util.intValue(parameter) & 0xffffffffL;
    }

    public static long s64Value64(PyObject parameter) {
        return Util.longValue(parameter);
    }

    public static long u64Value64(PyObject parameter) {
        return Util.longValue(parameter);
    }

    public static int f32Value32(PyObject parameter) {
        return Float.floatToRawIntBits((float) parameter.asDouble());
    }

    public static long f32Value64(PyObject parameter) {
        return Float.floatToRawIntBits((float) parameter.asDouble());
    }

    public static long f64Value64(PyObject parameter) {
        return Double.doubleToRawLongBits(parameter.asDouble());
    }
    
    
    public static PyObject newSigned8(int value) {
        return Py.newInteger((byte) value);
    }

    public static PyObject newSigned8(long value) {
        return Py.newInteger((byte) value);
    }

    public static PyObject newUnsigned8(int value) {
        int n = (byte) value; // sign-extend the low 8 bits to 32
        return Py.newInteger(n < 0 ? ((n & 0x7F) + 0x80) : n);
    }

    public static PyObject newUnsigned8(long value) {
        int n = (byte) value; // sign-extend the low 8 bits to 32
        return Py.newInteger(n < 0 ? ((n & 0x7F) + 0x80) : n);
    }

    public static PyObject newSigned16(int value) {
        return Py.newInteger((short) value);
    }

    public static PyObject newSigned16(long value) {
        return Py.newInteger((short) value);
    }

    public static PyObject newUnsigned16(int value) {
        int n = (short) value; // sign-extend the low 16 bits to 32
        return Py.newInteger(n < 0 ? ((n & 0x7FFF) + 0x8000) : n);
    }

    public static PyObject newUnsigned16(long value) {
        int n = (short) value; // sign-extend the low 16 bits to 32
        return Py.newInteger(n < 0 ? ((n & 0x7FFF) + 0x8000) : n);
    }

    public static PyObject newSigned32(int value) {
        return Py.newInteger(value);
    }

    public static PyObject newSigned32(long value) {
        return Py.newInteger((int) value);
    }

    public static PyObject newUnsigned32(int value) {
        int n = value;
        return n < 0 ? Py.newInteger(((n & 0x7FFFFFFFL) + 0x80000000L)) : Py.newInteger(n);
    }

    public static PyObject newUnsigned32(long value) {
        long n = (int) value; // only keep the low 32 bits
        return n < 0 ? Py.newInteger(((n & 0x7FFFFFFFL) + 0x80000000L)) : Py.newInteger(n);
    }

    public static PyObject newSigned64(int value) {
        return Py.newInteger(value);
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

    public static PyObject newFloat32(long value) {
        return Py.newFloat(Float.intBitsToFloat((int) value));
    }

    public static PyObject newFloat64(long value) {
        return Py.newFloat(Double.longBitsToDouble(value));
    }

    public static PyObject newBoolean(int value) {
        return (value & 0x1) != 0 ? Py.True : Py.False;
    }

    public static PyObject newBoolean(long value) {
        return (value & 0x1) != 0 ? Py.True : Py.False;
    }

    public static PyObject newNone(int unused) {
        return Py.None;
    }

    public static PyObject newNone(long unused) {
        return Py.None;
    }

    public static PyObject newPointer32(int value) {
        return Py.newLong(value);
    }

    public static PyObject newPointer32(long value) {
        return Py.newLong(value & 0xffffffffL);
    }

    public static PyObject newPointer64(long value) {
        return Py.newLong(value);
    }

    public static PyObject newString(int address) {
        return address != 0
                ? Py.newString(new String(IO.getZeroTerminatedByteArray(address)))
                : Py.None;
    }

    public static PyObject newString(long address) {
        return address != 0L
                ? Py.newString(new String(IO.getZeroTerminatedByteArray(address)))
                : Py.None;
    }
}
