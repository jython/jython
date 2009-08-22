
package org.python.modules.jffi;

import java.math.BigInteger;
import org.python.core.Py;
import org.python.core.PyInteger;
import org.python.core.PyLong;
import org.python.core.PyObject;

final class Util {
    private Util() {}
    
    public static final PyObject newSigned8(int value) {
        value &= 0xff;
        return Py.newInteger(value < 0x80 ? value : -0x80 + (value - 0x80));
    }

    public static final PyObject newUnsigned8(int value) {
        return Py.newInteger(value < 0 ? (long)((value & 0x7FL) + 0x80L) : value);
    }

    public static final PyObject newSigned16(int value) {
        value &= 0xffff;
        return Py.newInteger(value < 0x8000 ? value : -0x8000 + (value - 0x8000));
    }

    public static final PyObject newUnsigned16(int value) {
        return Py.newInteger(value < 0 ? (long)((value & 0x7FFFL) + 0x8000L) : value);
    }

    public static final PyObject newSigned32(int value) {
        return Py.newInteger(value);
    }

    public static final PyObject newUnsigned32(int value) {
        return Py.newInteger(value < 0 ? (long)((value & 0x7FFFFFFFL) + 0x80000000L) : value);
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

    public static final byte int8Value(PyObject parameter) {
        return (byte) parameter.asInt();
    }
    
    public static final byte uint8Value(PyObject parameter) {
        return (byte) parameter.asInt();
    }
    
    public static final short int16Value(PyObject parameter) {
        return (short) parameter.asInt();
    }
    
    public static final short uint16Value(PyObject parameter) {
        return (short) parameter.asInt();
    }

    public static final int int32Value(PyObject parameter) {
        return parameter.asInt();
    }
    
    public static final int uint32Value(PyObject parameter) {
        return parameter.asInt();
    }

    public static final long int64Value(PyObject value) {
        if (value instanceof PyLong) {
            return ((PyLong) value).getLong(Long.MIN_VALUE, Long.MAX_VALUE);
        } else if (value instanceof PyInteger) {
            return value.asInt();
        } else {
            return ((PyLong) value.__long__()).getLong(Long.MIN_VALUE, Long.MAX_VALUE);
        }
    }

    public static final long uint64Value(PyObject value) {
        return int64Value(value);
    }

    public static final float floatValue(PyObject parameter) {
        return (float) parameter.asDouble();
    }

    public static final double doubleValue(PyObject parameter) {
        return parameter.asDouble();
    }

    public static final void checkBounds(long size, long off, long len) {
        if ((off | len | (off + len) | (size - (off + len))) < 0) {
            throw Py.IndexError("Memory access offset="
                    + off + " size=" + len + " is out of bounds");
        }
    }
}
