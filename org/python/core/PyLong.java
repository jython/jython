// Copyright © Corporation for National Research Initiatives
package org.python.core;

import java.math.BigInteger;


public class PyLong extends PyObject
{
    private static final BigInteger minLong =
        BigInteger.valueOf(Long.MIN_VALUE);
    private static final BigInteger maxLong =
        BigInteger.valueOf(Long.MAX_VALUE);

    private static final BigInteger minDouble =
        new java.math.BigDecimal(Double.MIN_VALUE).toBigInteger();
    private static final BigInteger maxDouble =
        new java.math.BigDecimal(Double.MAX_VALUE).toBigInteger();

    private java.math.BigInteger value;

    public PyLong(java.math.BigInteger v) {
        value = v;
    }

    public PyLong(double v) {
        this(new java.math.BigDecimal(v).toBigInteger());
    }
    public PyLong(long v) {
        this(java.math.BigInteger.valueOf(v));
    }
    public PyLong(String s) {
        this(new java.math.BigInteger(s));
    }
    public String toString() {
        return value.toString()+"L";
    }

    public int hashCode() {
        // Probably won't work well for some classes of keys...
        return value.intValue();
    }

    public boolean __nonzero__() {
        return !value.equals(java.math.BigInteger.valueOf(0));
    }

    public double doubleValue() {
        double v = value.doubleValue();
        if (v == Double.NEGATIVE_INFINITY || v == Double.POSITIVE_INFINITY) {
            throw Py.OverflowError("long int too long to convert");
        }
        return v;
    }

    private long getLong(long min, long max) {
        if (value.compareTo(maxLong) <= 0 && value.compareTo(minLong) >= 0) {
            long v = value.longValue();
            if (v >= min && v <= max)
                return v;
        }
        throw Py.OverflowError("long int too long to convert");
    }

    public Object __tojava__(Class c) {
        try {
            if (c == Byte.TYPE || c == Byte.class) {
                return new Byte((byte)getLong(Byte.MIN_VALUE, Byte.MAX_VALUE));
            }
            if (c == Short.TYPE || c == Short.class) {
                return new Short((short)getLong(Short.MIN_VALUE,
                                                Short.MAX_VALUE));
            }
            if (c == Integer.TYPE || c == Integer.class) {
                return new Integer((int)getLong(Integer.MIN_VALUE,
                                                Integer.MAX_VALUE));
            }
            if (c == Long.TYPE || c == Long.class) {
                return new Long(getLong(Long.MIN_VALUE, Long.MAX_VALUE));
            }
            if (c == Float.TYPE || c == Double.TYPE || c == Float.class ||
                c == Double.class)
            {
                __float__().__tojava__(c);
            }
            if (c == BigInteger.class || c == Number.class ||
                c == Object.class)
            {
                return value;
            }
        } catch (PyException e) {
            return Py.NoConversion;
        }
        return super.__tojava__(c);
    }

    public int __cmp__(PyObject other) {
        return value.compareTo(((PyLong)other).value);
    }

    public Object __coerce_ex__(PyObject other) {
        if (other instanceof PyLong)
            return other;
        else
            if (other instanceof PyInteger) {
                return new PyLong(((PyInteger)other).getValue());
            } else {
                return Py.None;
            }
    }

    public PyObject __add__(PyObject right) {
        return new PyLong(value.add(((PyLong)right).value));
    }

    public PyObject __sub__(PyObject right) {
        return new PyLong(value.subtract(((PyLong)right).value));
    }

    public PyObject __mul__(PyObject right) {
        return new PyLong(value.multiply(((PyLong)right).value));
    }

    // Getting signs correct for integer division
    // This convention makes sense when you consider it in tandem with modulo
    private BigInteger divide(BigInteger x, BigInteger y) {
        BigInteger zero = java.math.BigInteger.valueOf(0);
        if (y.equals(zero))
            throw Py.ZeroDivisionError("long division or modulo");

        if (y.compareTo(zero) < 0) {
            if (x.compareTo(zero) > 0)
                return (x.subtract(y).subtract(BigInteger.valueOf(1))).divide(y);
        } else {
            if (x.compareTo(zero) < 0)
                return (x.subtract(y).add(BigInteger.valueOf(1))).divide(y);
        }
        return x.divide(y);
    }

    public PyObject __div__(PyObject right) {
        return new PyLong(divide(value, ((PyLong)right).value));
    }

    private BigInteger modulo(BigInteger x, BigInteger y, BigInteger xdivy) {
        return x.subtract(xdivy.multiply(y));
    }

    public PyObject __mod__(PyObject right) {
        BigInteger y = ((PyLong)right).value;
        return new PyLong(modulo(value, y, divide(value, y)));
    }

    public PyObject __divmod__(PyObject right) {
        BigInteger y = ((PyLong)right).value;
        BigInteger xdivy = divide(value, y);
        return new PyTuple(new PyObject[] {
            new PyLong(xdivy),
            new PyLong(modulo(value, y, xdivy))
        });
    }

    public PyObject __pow__(PyObject right, PyObject modulo) {
        BigInteger y = ((PyLong)right).value;
        if (y.compareTo(BigInteger.valueOf(0)) < 0) {
            throw Py.ValueError("long to negative power");
        }
        if (modulo == null)
            return new PyLong(value.pow(y.intValue()));
        else {
            // This whole thing can be trivially rewritten after bugs in modPow
            // are fixed by SUN

            BigInteger z = ((PyLong)modulo).value;
            int zi = z.intValue();
            // Clear up some special cases right away
            if (zi == 0)
                throw Py.ValueError("pow(x, y, z) with z == 0");
            if (zi == 1 || zi == -1)
                return new PyLong(0);

            if (z.compareTo(BigInteger.valueOf(0)) <= 0) {
                // Handle negative modulo's specially
                /*if (z.compareTo(BigInteger.valueOf(0)) == 0) {
                  throw Py.ValueError("pow(x, y, z) with z == 0");
                  }*/
                y = value.modPow(y, z.negate());
                if (y.compareTo(BigInteger.valueOf(0)) > 0) {
                    return new PyLong(z.add(y));
                } else {
                    return new PyLong(y);
                }
                //return __pow__(right).__mod__(modulo);
            } else {
                // This is buggy in SUN's jdk1.1.5
                // Extra __mod__ improves things slightly
                return new PyLong(value.modPow(y, z));
                //return __pow__(right).__mod__(modulo);
            }
        }
    }

    public PyObject __lshift__(PyObject right) {
        return new PyLong(value.shiftLeft(((PyLong)right).value.intValue()));
    }

    public PyObject __rshift__(PyObject right) {
        return new PyLong(value.shiftRight(((PyLong)right).value.intValue()));
    }

    public PyObject __and__(PyObject right) {
        return new PyLong(value.and(((PyLong)right).value));
    }

    public PyObject __xor__(PyObject right) {
        return new PyLong(value.xor(((PyLong)right).value));
    }

    public PyObject __or__(PyObject right) {
        return new PyLong(value.or(((PyLong)right).value));
    }

    public PyObject __neg__() {
        return new PyLong(value.negate());
    }

    public PyObject __pos__() {
        return this;
    }

    public PyObject __abs__() {
        return new PyLong(value.abs());
    }

    public PyObject __invert__() {
        return new PyLong(value.not());
    }

    public PyInteger __int__() {
        return new PyInteger((int)getLong(Integer.MIN_VALUE,
                                          Integer.MAX_VALUE));
    }
    public PyLong __long__() {
        return this;
    }
    public PyFloat __float__() {
        return new PyFloat(doubleValue());
    }

    public PyComplex __complex__() {
        return new PyComplex(doubleValue(), 0.);
    }

    public PyString __oct__() {
        String s = value.toString(8);
        if (s.startsWith("-"))
            return new PyString("-0"+s.substring(1, s.length())+"L");
        else
            return new PyString("0"+s+"L");
    }

    public PyString __hex__() {
        String s = value.toString(16);
        if (s.startsWith("-"))
            return new PyString("-0x"+s.substring(1, s.length())+"L");
        else
            return new PyString("0x"+s+"L");
    }
}
