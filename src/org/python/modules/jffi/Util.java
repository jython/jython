
package org.python.modules.jffi;

import java.math.BigInteger;

import org.python.core.Py;
import org.python.core.PyInteger;
import org.python.core.PyLong;
import org.python.core.PyObject;

final class Util {
    private static final com.kenai.jffi.MemoryIO IO = com.kenai.jffi.MemoryIO.getInstance();

    private Util() {}

    public static final PyObject newSigned8(int value) {
        return Py.newInteger((byte) value);
    }

    public static final PyObject newUnsigned8(int value) {
        int n = (byte) value; // sign-extend the low 8 bits to 32
        return Py.newInteger(n < 0 ? ((n & 0x7F) + 0x80) : n);
    }

    public static final PyObject newSigned16(int value) {
        return Py.newInteger((short) value);
    }

    public static final PyObject newUnsigned16(int value) {
        int n = (short) value; // sign-extend the low 16 bits to 32
        return Py.newInteger(n < 0 ? ((n & 0x7FFF) + 0x8000) : n);
    }

    public static final PyObject newSigned32(int value) {
        return Py.newInteger(value);
    }

    public static final PyObject newUnsigned32(int value) {
        int n = value;
        return n < 0 ? Py.newInteger(((n & 0x7FFFFFFFL) + 0x80000000L)) : Py.newInteger(n);
    }

    public static final PyObject newSigned64(long value) {
        return Py.newInteger(value);
    }

    private static final BigInteger UINT64_BASE = BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE);
    public static final PyObject newUnsigned64(long value) {
        return value < 0
                    ? Py.newLong(BigInteger.valueOf(value & 0x7fffffffffffffffL).add(UINT64_BASE))
                    : Py.newInteger(value);
    }

    public static final PyObject newString(long address) {
        return address != 0
                    ? Py.newString(new String(IO.getZeroTerminatedByteArray(address)))
                    : Py.None;
    }

    public static final byte int8Value(PyObject parameter) {
        return (byte) intValue(parameter);
    }
    
    public static final byte uint8Value(PyObject parameter) {
        return (byte) intValue(parameter);
    }
    
    public static final short int16Value(PyObject parameter) {
        return (short) intValue(parameter);
    }
    
    public static final short uint16Value(PyObject parameter) {
        return (short) intValue(parameter);
    }

    public static final int int32Value(PyObject parameter) {
        return intValue(parameter);
    }
    
    public static final int uint32Value(PyObject parameter) {
        return intValue(parameter);
    }

    public static final long int64Value(PyObject value) {
        return longValue(value);
    }

    public static final long uint64Value(PyObject value) {
        return longValue(value);
    }

    public static final float floatValue(PyObject parameter) {
        return (float) parameter.asDouble();
    }

    public static final double doubleValue(PyObject parameter) {
        return parameter.asDouble();
    }

    private static final long __long__value(PyObject value) {
        PyObject l = value.__long__();
        if (l instanceof PyLong) {
            return ((PyLong) l).getValue().longValue();

        } else if (l instanceof PyInteger) {
            return ((PyInteger) l).getValue();
        }

        throw Py.TypeError("invalid __long__() result");
    }

    public static final void checkBounds(long size, long off, long len) {
        if ((off | len | (off + len) | (size - (off + len))) < 0) {
            throw Py.IndexError("Memory access offset="
                    + off + " size=" + len + " is out of bounds");
        }
    }

    static final DirectMemory getMemoryForAddress(PyObject address) {
        if (address instanceof Pointer) {
            return ((Pointer) address).getMemory();
        } else if (address instanceof PyInteger) {
            return new NativeMemory(address.asInt());
        } else if (address instanceof PyLong) {
            return new NativeMemory(((PyLong) address).getValue().longValue());
        }
        throw Py.TypeError("invalid address");
    }

    static final com.kenai.jffi.Type jffiType(CType type) {
        return (com.kenai.jffi.Type) type.jffiType();
    }

    public static int intValue(PyObject parameter) {
        if (parameter instanceof PyInteger) {
            return ((PyInteger) parameter).getValue();

        } else if (parameter instanceof PyLong) {
            return ((PyLong) parameter).getValue().intValue();

        } else if (parameter instanceof ScalarCData) {
            return intValue(((ScalarCData) parameter).getValue());

        } else {
            return (int) __long__value(parameter);
        }
    }

    public static long longValue(PyObject parameter) {
        if (parameter instanceof PyInteger) {
            return ((PyInteger) parameter).getValue();

        } else if (parameter instanceof PyLong) {
            return ((PyLong) parameter).getValue().longValue();

        } else if (parameter instanceof ScalarCData) {
            return longValue(((ScalarCData) parameter).getValue());

        } else {
            return __long__value(parameter);
        }
    }
}
