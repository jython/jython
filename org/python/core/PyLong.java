// Copyright (c) Corporation for National Research Initiatives
package org.python.core;

import java.math.BigInteger;
import java.io.Serializable;

/**
 * A builtin python long. This is implemented as a
 * java.math.BigInteger.
 */

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
                return new Byte((byte)getLong(Byte.MIN_VALUE,
                                              Byte.MAX_VALUE));
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
                return new Long(getLong(Long.MIN_VALUE,
                                        Long.MAX_VALUE));
            }
            if (c == Float.TYPE || c == Double.TYPE || c == Float.class ||
                c == Double.class)
            {
                return __float__().__tojava__(c);
            }
            if (c == BigInteger.class || c == Number.class ||
                c == Object.class || c == Serializable.class)
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

    private static final boolean canCoerce(PyObject other) {
        return other instanceof PyLong || other instanceof PyInteger;
    }

    private static final BigInteger coerce(PyObject other) {
        if (other instanceof PyLong)
            return ((PyLong) other).value;
        else if (other instanceof PyInteger)
            return java.math.BigInteger.valueOf(
                   ((PyInteger) other).getValue());
        else
            throw Py.TypeError("xxx");
    }


    public PyObject __add__(PyObject right) {
        if (!canCoerce(right))
            return null;
        return new PyLong(value.add(coerce(right)));
    }

    public PyObject __radd__(PyObject left) {
        return __add__(left);
    }

    public PyObject __sub__(PyObject right) {
        if (!canCoerce(right))
            return null;
        return new PyLong(value.subtract(coerce(right)));
    }

    public PyObject __rsub__(PyObject left) {
        return new PyLong(coerce(left).subtract(value));
    }

    public PyObject __mul__(PyObject right) {
        if (right instanceof PySequence)
            return ((PySequence) right).repeat(coerceInt(this));

        if (!canCoerce(right))
            return null;
        return new PyLong(value.multiply(coerce(right)));
    }

    public PyObject __rmul__(PyObject left) {
        if (left instanceof PySequence)
            return ((PySequence) left).repeat(coerceInt(this));
        if (!canCoerce(left))
            return null;
        return new PyLong(coerce(left).multiply(value));
    }

    // Getting signs correct for integer division
    // This convention makes sense when you consider it in tandem with modulo
    private BigInteger divide(BigInteger x, BigInteger y) {
        BigInteger zero = java.math.BigInteger.valueOf(0);
        if (y.equals(zero))
            throw Py.ZeroDivisionError("long division or modulo");

        if (y.compareTo(zero) < 0) {
            if (x.compareTo(zero) > 0)
                return (x.subtract(y).subtract(
                                      BigInteger.valueOf(1))).divide(y);
        } else {
            if (x.compareTo(zero) < 0)
                return (x.subtract(y).add(BigInteger.valueOf(1))).divide(y);
        }
        return x.divide(y);
    }

    public PyObject __div__(PyObject right) {
        if (!canCoerce(right))
            return null;
        if (Options.divisionWarning > 0)
            Py.warning(Py.DeprecationWarning, "classic long division");
        return new PyLong(divide(value, coerce(right)));
    }

    public PyObject __rdiv__(PyObject left) {
        if (!canCoerce(left))
            return null;
        if (Options.divisionWarning > 0)
            Py.warning(Py.DeprecationWarning, "classic long division");
        return new PyLong(divide(coerce(left), value));
    }

    public PyObject __floordiv__(PyObject right) {
        if (!canCoerce(right))
            return null;
        return new PyLong(divide(value, coerce(right)));
    }

    public PyObject __rfloordiv__(PyObject left) {
        if (!canCoerce(left))
            return null;
        return new PyLong(divide(coerce(left), value));
    }

    public PyObject __truediv__(PyObject right) {
        if (!canCoerce(right))
            return null;
        return new PyFloat(doubleValue() / coerce(right).doubleValue());
    }

    public PyObject __rtruediv__(PyObject left) {
        if (!canCoerce(left))
            return null;
        return new PyFloat(coerce(left).doubleValue() / doubleValue());
    }

    private BigInteger modulo(BigInteger x, BigInteger y, BigInteger xdivy) {
        return x.subtract(xdivy.multiply(y));
    }

    public PyObject __mod__(PyObject right) {
        if (!canCoerce(right))
            return null;
        BigInteger rightv = coerce(right);
        return new PyLong(modulo(value, rightv, divide(value, rightv)));
    }

    public PyObject __rmod__(PyObject left) {
        if (!canCoerce(left))
            return null;
        BigInteger leftv = coerce(left);
        return new PyLong(modulo(leftv, value, divide(leftv, value)));
    }

    public PyObject __divmod__(PyObject right) {
        if (!canCoerce(right))
            return null;
        BigInteger rightv = coerce(right);

        BigInteger xdivy = divide(value, rightv);
        return new PyTuple(new PyObject[] {
            new PyLong(xdivy),
            new PyLong(modulo(value, rightv, xdivy))
        });
    }

    public PyObject __rdivmod__(PyObject left) {
        if (!canCoerce(left))
            return null;
        BigInteger leftv = coerce(left);

        BigInteger xdivy = divide(leftv, value);
        return new PyTuple(new PyObject[] {
            new PyLong(xdivy),
            new PyLong(modulo(leftv, value, xdivy))
        });
    }

    public PyObject __pow__(PyObject right, PyObject modulo) {
        if (!canCoerce(right))
            return null;

        if (modulo != null && !canCoerce(right))
            return null;
        return _pow(value, coerce(right), modulo, this, right);
    }

    public PyObject __rpow__(PyObject left) {
        if (!canCoerce(left))
            return null;
        return _pow(coerce(left), value, null, left, this);
    }

    public static PyObject _pow(BigInteger value, BigInteger y,
                                PyObject modulo, PyObject left, PyObject right)
    {
        if (y.compareTo(BigInteger.valueOf(0)) < 0) {
            if (value.compareTo(BigInteger.valueOf(0)) != 0)
                return left.__float__().__pow__(right, modulo);
            else
                throw Py.ZeroDivisionError("zero to a negative power");
        }
        if (modulo == null)
            return new PyLong(value.pow(y.intValue()));
        else {
            // This whole thing can be trivially rewritten after bugs
            // in modPow are fixed by SUN

            BigInteger z = coerce(modulo);
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

    private static final int coerceInt(PyObject other) {
        if (other instanceof PyLong)
            return (int) ((PyLong) other).getLong(
                          Integer.MIN_VALUE, Integer.MAX_VALUE);
        else if (other instanceof PyInteger)
            return ((PyInteger) other).getValue();
        else
            throw Py.TypeError("xxx");
    }

    public PyObject __lshift__(PyObject right) {
        if (!canCoerce(right))
            return null;
        int rightv = coerceInt(right);
        if(rightv < 0)
            throw Py.ValueError("negative shift count");
        return new PyLong(value.shiftLeft(rightv));
    }

    public PyObject __rshift__(PyObject right) {
        if (!canCoerce(right))
            return null;
        int rightv = coerceInt(right);
        if(rightv < 0)
            throw Py.ValueError("negative shift count");
        return new PyLong(value.shiftRight(rightv));
    }

    public PyObject __and__(PyObject right) {
        if (!canCoerce(right))
            return null;
        return new PyLong(value.and(coerce(right)));
    }

    public PyObject __rand__(PyObject left) {
        if (!canCoerce(left))
            return null;
        return new PyLong(coerce(left).and(value));
    }

    public PyObject __xor__(PyObject right) {
        if (!canCoerce(right))
            return null;
        return new PyLong(value.xor(coerce(right)));
    }

    public PyObject __rxor__(PyObject left) {
        if (!canCoerce(left))
            return null;
        return new PyLong(coerce(left).xor(value));
    }

    public PyObject __or__(PyObject right) {
        if (!canCoerce(right))
            return null;
        return new PyLong(value.or(coerce(right)));
    }

    public PyObject __ror__(PyObject left) {
        if (!canCoerce(left))
            return null;
        return new PyLong(coerce(left).or(value));
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
            if (s.startsWith("0"))
                return new PyString(s+"L");
            else
                return new PyString("0"+s+"L");
    }

    public PyString __hex__() {
        String s = value.toString(16).toUpperCase();
        if (s.startsWith("-"))
            return new PyString("-0x"+s.substring(1, s.length())+"L");
        else
            return new PyString("0x"+s+"L");
    }

    public PyString __str__() {
        return Py.newString(value.toString());
    }

    public boolean isMappingType() { return false; }
    public boolean isSequenceType() { return false; }

    // __class__ boilerplate -- see PyObject for details
    public static PyClass __class__;

    protected PyClass getPyClass() {
        return __class__;
    }
}
