// Copyright © Corporation for National Research Initiatives
package org.python.core;

import java.io.Serializable;

/**
 * A builtin python int.
 */

public class PyInteger extends PyObject
{
    private int value;

    public PyInteger(int v) {
        value = (int)v;
    }

    public int getValue() {
        return value;
    }

    protected String safeRepr() {
        return "'int' object";
    }

    public String toString() {
        return Integer.toString(value);
    }

    public int hashCode() {
        return value;
    }

    public boolean __nonzero__() {
        return value != 0;
    }

    public Object __tojava__(Class c) {
        if (c == Integer.TYPE || c == Number.class ||
            c == Object.class || c == Integer.class ||
            c == Serializable.class)
        {
            return new Integer(value);
        }

        if (c == Boolean.TYPE || c == Boolean.class)
            return new Boolean(value != 0);
        if (c == Byte.TYPE || c == Byte.class)
            return new Byte((byte)value);
        if (c == Short.TYPE || c == Short.class)
            return new Short((short)value);

        if (c == Long.TYPE || c == Long.class)
            return new Long(value);
        if (c == Float.TYPE || c == Float.class)
            return new Float(value);
        if (c == Double.TYPE || c == Double.class)
            return new Double(value);
        return super.__tojava__(c);
    }

    public int __cmp__(PyObject other) {
        int v = ((PyInteger)other).value;
        return value < v ? -1 : value > v ? 1 : 0;
    }

    public Object __coerce_ex__(PyObject other) {
        if (other instanceof PyInteger)
            return other;
        else
            return Py.None;
    }

    private static final boolean canCoerce(PyObject other) {
        return other instanceof PyInteger;
    }

    private static final int coerce(PyObject other) {
        if (other instanceof PyInteger)
            return ((PyInteger) other).value;
        else
            throw Py.TypeError("xxx");
    }


    public PyObject __add__(PyObject right) {
        if (!canCoerce(right))
            return null;
        int rightv = coerce(right);
        int a = value;
        int b = rightv;
        int x = a + b;
        if ((x^a) < 0 && (x^b) < 0)
            throw Py.OverflowError("integer addition: "+this+" + "+right);
        return Py.newInteger(x);
    }
    public PyObject __radd__(PyObject left) {
        return __add__(left);
    }

    private static PyInteger _sub(int a, int b) {
        int x = a - b;
        if ((x^a) < 0 && (x^~b) < 0)
            throw Py.OverflowError("integer subtraction: "+a+" - "+b);
        return Py.newInteger(x);
    }

    public PyObject __sub__(PyObject right) {
        if (!canCoerce(right))
            return null;
        return _sub(value, coerce(right));
    }

    public PyObject __rsub__(PyObject left) {
        if (!canCoerce(left))
            return null;
        return _sub(coerce(left), value);
    }

    public PyObject __mul__(PyObject right) {
        if (right instanceof PySequence)
            return ((PySequence) right).repeat(value);

        if (!canCoerce(right))
            return null;
        int rightv = coerce(right);

        double x = (double)value;
        x *= rightv;
        //long x = ((long)value)*((PyInteger)right).value;
        //System.out.println("mul: "+this+" * "+right+" = "+x);

        if (x > Integer.MAX_VALUE || x < Integer.MIN_VALUE)
            throw Py.OverflowError("integer multiplication: "+this+
                                   " * "+right);
        return Py.newInteger((int)x);
    }

    public PyObject __rmul__(PyObject left) {
        return __mul__(left);
    }

    // Getting signs correct for integer division
    // This convention makes sense when you consider it in tandem with modulo
    private static int divide(int x, int y) {
        if (y == 0)
            throw Py.ZeroDivisionError("integer division or modulo");

        if (y == -1 && x < 0 && x == -x) {
            throw Py.OverflowError("integer division: "+x+" + "+y);
        }
        int xdivy = x / y;
        int xmody = x - xdivy * y;
        /* If the signs of x and y differ, and the remainder is non-0,
         * C89 doesn't define whether xdivy is now the floor or the
         * ceiling of the infinitely precise quotient.  We want the floor,
         * and we have it iff the remainder's sign matches y's.
         */
        if (xmody != 0 && ((y ^ xmody) < 0) /* i.e. and signs differ */) {
            xmody += y;
            --xdivy;
            //assert(xmody && ((y ^ xmody) >= 0));
        }
        return xdivy;
    }

    public PyObject __div__(PyObject right) {
        if (!canCoerce(right))
            return null;
        return Py.newInteger(divide(value, coerce(right)));
    }

    public PyObject __rdiv__(PyObject left) {
        if (!canCoerce(left))
            return null;
        return Py.newInteger(divide(coerce(left), value));
    }

    private static int modulo(int x, int y, int xdivy) {
        return x - xdivy*y;
    }

    public PyObject __mod__(PyObject right) {
        if (!canCoerce(right))
            return null;
        int rightv = coerce(right);
        return Py.newInteger(modulo(value, rightv, divide(value, rightv)));
    }

    public PyObject __rmod__(PyObject left) {
        if (!canCoerce(left))
            return null;
        int leftv = coerce(left);
        return Py.newInteger(modulo(leftv, value, divide(leftv, value)));
    }

    public PyObject __divmod__(PyObject right) {
        if (!canCoerce(right))
            return null;
        int rightv = coerce(right);

        int xdivy = divide(value, rightv);
        return new PyTuple(new PyObject[] {
            new PyInteger(xdivy),
            new PyInteger(modulo(value, rightv, xdivy))
        });
    }

    public PyObject __pow__(PyObject right, PyObject modulo) {
        if (!canCoerce(right))
            return null;

        if (modulo != null && !canCoerce(modulo))
            return null;

        return _pow(value, coerce(right), modulo);
    }

    public PyObject __rpow__(PyObject left, PyObject modulo) {
        if (!canCoerce(left))
            return null;

        if (modulo != null && !canCoerce(modulo))
            return null;

        return _pow(coerce(left), value, modulo);
    }

    private static PyInteger _pow(int value, int pow, PyObject modulo) {
        int mod = 0;
        long tmp = value;
        boolean neg = false;
        if (tmp < 0) {
            tmp = -tmp;
            neg = (pow & 0x1) != 0;
        }
        long result = 1;

        if (pow < 0) {
            if (value != 0)
                throw Py.ValueError("cannot raise integer to a " +
                                    "negative power");
            else
                throw Py.ZeroDivisionError("cannot raise 0 to a " +
                                           "negative power");
        }

        if (modulo != null) {
            mod = coerce(modulo);
            if (mod == 0) {
                throw Py.ValueError("pow(x, y, z) with z==0");
            }
        }

        // Standard O(ln(N)) exponentiation code
        while (pow > 0) {
            if ((pow & 0x1) != 0) {
                result *= tmp;
                if (mod != 0) {
                    result %= (long)mod;
                }

                if (result > Integer.MAX_VALUE) {
                    throw Py.OverflowError("integer pow()");
                }
            }
            pow >>= 1;
            if (pow == 0)
                break;
            tmp *= tmp;

            if (mod != 0) {
                tmp %= (long)mod;
            }

            if (tmp > Integer.MAX_VALUE) {
                throw Py.OverflowError("integer pow()");
            }
        }

        int ret = (int)result;
        if (neg)
            ret = -ret;

        // Cleanup result of modulo
        if (mod != 0) {
            ret = modulo(ret, mod, divide(ret, mod));
        }
        return Py.newInteger(ret);
    }

    public PyObject __lshift__(PyObject right) {
        int rightv;
        if (right instanceof PyInteger)
             rightv = ((PyInteger)right).value;
        else
             return null;

        if (rightv > 31)
            return new PyInteger(0);
        return Py.newInteger(value << rightv);
    }

    public PyObject __rshift__(PyObject right) {
        int rightv;
        if (right instanceof PyInteger)
             rightv = ((PyInteger)right).value;
        else
             return null;

        return Py.newInteger(value >> rightv);
    }

    public PyObject __and__(PyObject right) {
        int rightv;
        if (right instanceof PyInteger)
             rightv = ((PyInteger)right).value;
        else
             return null;

        return Py.newInteger(value & rightv);
    }

    public PyObject __xor__(PyObject right) {
        int rightv;
        if (right instanceof PyInteger)
             rightv = ((PyInteger)right).value;
        else
             return null;

        return Py.newInteger(value ^ rightv);
    }

    public PyObject __or__(PyObject right) {
        int rightv;
        if (right instanceof PyInteger)
             rightv = ((PyInteger)right).value;
        else
             return null;

        return Py.newInteger(value | rightv);
    }

    public PyObject __neg__() {
        int x = -value;
        if (value < 0 && x < 0)
            throw Py.OverflowError("integer negation");
        return Py.newInteger(x);
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

    public PyObject __invert__() {
        return Py.newInteger(~value);
    }

    public PyInteger __int__() {
        return this;
    }

    public PyLong __long__() {
        return new PyLong(value);
    }

    public PyFloat __float__() {
        return new PyFloat((double)value);
    }

    public PyComplex __complex__() {
        return new PyComplex((double)value, 0.);
    }

    public PyString __oct__() {
        if (value < 0) {
            return new PyString(
                "0"+Long.toString(0x100000000l+(long)value, 8));
        } else if (value > 0) {
            return new PyString("0"+Integer.toString(value, 8));
        } else
            return new PyString("0");
    }

    public PyString __hex__() {
        if (value < 0) {
            return new PyString(
                "0x"+Long.toString(0x100000000l+(long)value, 16));
        } else {
            return new PyString("0x"+Integer.toString(value, 16));
        }
    }

    public boolean isMappingType() { return false; }
    public boolean isSequenceType() { return false; }

    // __class__ boilerplate -- see PyObject for details
    public static PyClass __class__;

    protected PyClass getPyClass() {
        return __class__;
    }
}
