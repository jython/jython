// Copyright © Corporation for National Research Initiatives
package org.python.core;

import java.io.Serializable;


/**
 * A builtin python float.
 */

public class PyFloat extends PyObject
{
    private double value;

    public PyFloat(double v) {
        value = v;
    }
    public PyFloat(float v) {
        this((double)v);
    }

    protected String safeRepr() {
        return "'float' object";
    }

    public double getValue() {
        return value;
    }

    public String toString() {
        String s = Double.toString(value);
        // this is to work around an apparent bug in Double.toString(0.001)
        // which returns "0.0010"
        if (s.indexOf('E') == -1) {
            while (true) {
                int n = s.length();
                if (n <= 2)
                    break;
                if (s.charAt(n-1) == '0' && s.charAt(n-2) != '.') {
                    s = s.substring(0,n-1);
                    continue;
                }
                break;
            }
        }
        return s;
    }

    public int hashCode() {
        double intPart = Math.floor(value);
        double fractPart = value-intPart;

        if (fractPart == 0) {
            if (intPart <= Integer.MAX_VALUE && intPart >= Integer.MIN_VALUE)
                return (int)value;
            else
                return __long__().hashCode();
        } else {
            long v = Double.doubleToLongBits(value);
            return (int)v ^ (int)(v >> 32);
        }
    }

    public boolean __nonzero__() {
        return value != 0;
    }

    public Object __tojava__(Class c) {
        if (c == Double.TYPE || c == Number.class ||
            c == Double.class || c == Object.class || c == Serializable.class)
        {
            return new Double(value);
        }
        if (c == Float.TYPE || c == Float.class) {
            return new Float(value);
        }
        return super.__tojava__(c);
    }

    public int __cmp__(PyObject other) {
        double v = ((PyFloat)other).value;
        return value < v ? -1 : value > v ? 1 : 0;
    }

    public Object __coerce_ex__(PyObject other) {
        if (other instanceof PyFloat)
            return other;
        else {
            if (other instanceof PyInteger)
                return new PyFloat((double)((PyInteger)other).getValue());
            if (other instanceof PyLong)
                return new PyFloat(((PyLong)other).doubleValue());
            else
                return Py.None;
        }
    }

    private static final boolean canCoerce(PyObject other) {
        return other instanceof PyFloat || other instanceof PyInteger ||
            other instanceof PyLong;
    }

    private static final double coerce(PyObject other) {
        if (other instanceof PyFloat)
            return ((PyFloat) other).value;
        else if (other instanceof PyInteger)
            return ((PyInteger) other).getValue();
        else if (other instanceof PyLong)
            return ((PyLong) other).doubleValue();
        else
            throw Py.TypeError("xxx");
    }


    public PyObject __add__(PyObject right) {
        if (!canCoerce(right))
            return null;
        double rightv = coerce(right);
        return new PyFloat(value + rightv);
    }

    public PyObject __radd__(PyObject left) {
        return __add__(left);
    }

    public PyObject __sub__(PyObject right) {
        if (!canCoerce(right))
            return null;
        double rightv = coerce(right);
        return new PyFloat(value - rightv);
    }

    public PyObject __rsub__(PyObject left) {
        if (!canCoerce(left))
            return null;
        double leftv = coerce(left);
        return new PyFloat(leftv - value);
    }

    public PyObject __mul__(PyObject right) {
        if (!canCoerce(right))
            return null;
        double rightv = coerce(right);
        return new PyFloat(value * rightv);
    }

    public PyObject __rmul__(PyObject left) {
        return __mul__(left);
    }

    public PyObject __div__(PyObject right) {
        if (!canCoerce(right))
            return null;
        double rightv = coerce(right);
        if (rightv == 0)
            throw Py.ZeroDivisionError("float division");
        return new PyFloat(value / rightv);
    }

    public PyObject __rdiv__(PyObject left) {
        if (!canCoerce(left))
            return null;
        double leftv = coerce(left);
        if (value == 0)
            throw Py.ZeroDivisionError("float division");
        return new PyFloat(leftv / value);
    }

    private static double modulo(double x, double y) {
        if (y == 0)
            throw Py.ZeroDivisionError("float modulo");
        double z = Math.IEEEremainder(x, y);
        if (z*y < 0)
            z += y;
        return z;
    }

    public PyObject __mod__(PyObject right) {
        if (!canCoerce(right))
            return null;
        double rightv = coerce(right);
        return new PyFloat(modulo(value, rightv));
    }

    public PyObject __rmod__(PyObject left) {
        if (!canCoerce(left))
            return null;
        double leftv = coerce(left);
        return new PyFloat(modulo(leftv, value));
    }

    public PyObject __divmod__(PyObject right) {
        if (!canCoerce(right))
            return null;
        double rightv = coerce(right);

        if (rightv == 0)
            throw Py.ZeroDivisionError("float division");
        double z = Math.floor(value / rightv);

        return new PyTuple(
            new PyObject[] {new PyFloat(z), new PyFloat(value-z*rightv)}
            );
    }

    public PyObject __rdivmod__(PyObject left) {
        if (!canCoerce(left))
            return null;
        double leftv = coerce(left);

        if (value == 0)
            throw Py.ZeroDivisionError("float division");
        double z = Math.floor(leftv / value);

        return new PyTuple(
            new PyObject[] {new PyFloat(z), new PyFloat(leftv-z*value)}
            );
    }


    public PyObject __pow__(PyObject right, PyObject modulo) {
        if (!canCoerce(right))
            return null;

        if (modulo != null && !canCoerce(modulo))
            return null;

        return _pow(value, coerce(right), modulo);
    }

    public PyObject __rpow__(PyObject left) {
        if (!canCoerce(left))
            return null;

        return _pow(coerce(left), value, null);
    }

    private static PyFloat _pow(double value, double iw, PyObject modulo) {
        // Rely completely on Java's pow function
        if (iw == 0) {
            if (modulo != null)
                return new PyFloat(modulo(1.0, coerce(modulo)));
            return new PyFloat(1.0);
        }
        if (value == 0.0) {
            if (iw < 0.0)
                throw Py.ZeroDivisionError("0.0 cannot be raised to a " +
                                           "negative power");
            return new PyFloat(0);
        }

        double ret = Math.pow(value, iw);
        if (modulo == null) {
            return new PyFloat(ret);
        } else {
            return new PyFloat(modulo(ret, coerce(modulo)));
        }
    }

    public PyObject __neg__() {
        return new PyFloat(-value);
    }

    public PyObject __pos__() {
        return this;
    }

    public PyObject __abs__() {
        if (value >= 0)
            return this;
        else
            return __neg__();
    }

    public PyInteger __int__() {
        if (value <= Integer.MAX_VALUE && value >= Integer.MIN_VALUE) {
            return new PyInteger((int)value);
        }
        throw Py.OverflowError("float too large to convert");
    }

    public PyLong __long__() {
        return new PyLong(value);
    }

    public PyFloat __float__() {
        return this;
    }

    public PyComplex __complex__() {
        return new PyComplex(value, 0.);
    }

    public boolean isMappingType() { return false; }
    public boolean isSequenceType() { return false; }

    // __class__ boilerplate -- see PyObject for details
    public static PyClass __class__;

    protected PyClass getPyClass() {
        return __class__;
    }
}
