// Copyright (c) Corporation for National Research Initiatives
package org.python.core;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;

import org.python.expose.ExposedMethod;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedType;
import org.python.expose.MethodType;

/**
 * A builtin python long. This is implemented as a
 * java.math.BigInteger.
 */
@ExposedType(name = "long")
public class PyLong extends PyObject {

    public static final PyType TYPE = PyType.fromClass(PyLong.class);
    
    public static final BigInteger minLong = BigInteger.valueOf(Long.MIN_VALUE);
    public static final BigInteger maxLong = BigInteger.valueOf(Long.MAX_VALUE);
    public static final BigInteger maxULong =
        BigInteger.valueOf(1).shiftLeft(64).subtract(BigInteger.valueOf(1));

    private BigInteger value;

    @ExposedNew
    public static PyObject long___new__(PyNewWrapper new_, boolean init, PyType subtype,
                                        PyObject[] args, String[] keywords) {
        if (new_.for_type != subtype) {
            return longSubtypeNew(new_, init, subtype, args, keywords);
        }

        ArgParser ap = new ArgParser("long", args, keywords, new String[] {"x", "base"}, 0);
        PyObject x = ap.getPyObject(0, null);
        int base = ap.getInt(1, -909);

        if (x == null) {
            return new PyLong(0);
        }
        if (base == -909) {
            return x.__long__();
        }
        if (!(x instanceof PyString)) {
            throw Py.TypeError("long: can't convert non-string with explicit base");
        }
        return ((PyString)x).atol(base);
    }

    /**
     * Wimpy, slow approach to new calls for subtypes of long.
     *
     * First creates a regular long from whatever arguments we got, then allocates a
     * subtype instance and initializes it from the regular long. The regular long is then
     * thrown away.
     */
    private static PyObject longSubtypeNew(PyNewWrapper new_, boolean init, PyType subtype,
                                           PyObject[] args, String[] keywords) {
        PyObject tmp = long___new__(new_, init, TYPE, args, keywords);
        if (tmp instanceof PyInteger) {
            int intValue = ((PyInteger)tmp).getValue();
            return new PyLongDerived(subtype, BigInteger.valueOf(intValue));
        } else {
            return new PyLongDerived(subtype, ((PyLong)tmp).value);
        }
    }
    
    public PyLong(PyType subType, BigInteger v) {
        super(subType);
        value = v;
    }

    public PyLong(BigInteger v) {
        this(TYPE, v);
    }

    public PyLong(double v) {
        this(toBigInteger(v));
    }

    public PyLong(long v) {
        this(BigInteger.valueOf(v));
    }

    public PyLong(String s) {
        this(new BigInteger(s));
    }

    /**
     * Convert a double to BigInteger, raising an OverflowError if
     * infinite.
     */
    private static BigInteger toBigInteger(double value) {
        if (Double.isInfinite(value)) {
            throw Py.OverflowError("cannot convert float infinity to long");
        }
        if (Double.isNaN(value)) {
            return BigInteger.valueOf(0);
        }
        return new BigDecimal(value).toBigInteger();
    }

    public BigInteger getValue() {
        return value;
    }

    public String toString() {
        return long_toString();
    }

    @ExposedMethod(names = {"__str__", "__repr__"})
    final String long_toString() {
        return value.toString()+"L";
    }

    public int hashCode() {
        return long___hash__();
    }

    @ExposedMethod
    final int long___hash__() {
        return value.intValue();
    }

    public boolean __nonzero__() {
        return !value.equals(BigInteger.valueOf(0));
    }

    @ExposedMethod
    public boolean long___nonzero__() {
        return __nonzero__();
    }

    public double doubleValue() {
        double v = value.doubleValue();
        if (Double.isInfinite(v)) {
            throw Py.OverflowError("long int too large to convert to float");
        }
        return v;
    }

    private static final double scaledDoubleValue(BigInteger val, int[] exp){
        double x = 0;
        int signum = val.signum();
        byte[] digits;

        if (signum >= 0) {
            digits = val.toByteArray();
        } else {
            digits = val.negate().toByteArray();
        }

        int count = 8;
        int i = 0;

        if (digits[0] == 0) {
            i++;
            count++;
        }
        count = count <= digits.length?count:digits.length;

        while (i < count) {
            x = x * 256 + (digits[i] & 0xff);
            i++;
        }
        exp[0] = digits.length - i;
        return signum*x;
    }

    public double scaledDoubleValue(int[] exp){
        return scaledDoubleValue(value,exp);
    }


    private long getLong(long min, long max) {
        return getLong(min, max, "long int too large to convert");
    }

    private long getLong(long min, long max, String overflowMsg) {
        if (value.compareTo(maxLong) <= 0 && value.compareTo(minLong) >= 0) {
            long v = value.longValue();
            if (v >= min && v <= max)
                return v;
        }
        throw Py.OverflowError(overflowMsg);
    }

    public long asLong(int index) {
        return getLong(Long.MIN_VALUE, Long.MAX_VALUE, "long too big to convert");
    }

    public int asInt(int index) {
        return (int)getLong(Integer.MIN_VALUE, Integer.MAX_VALUE,
                            "long int too large to convert to int");
    }

    @Override
    public int asInt() {
        return (int)getLong(Integer.MIN_VALUE, Integer.MAX_VALUE,
                            "long int too large to convert to int");
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
        return long___cmp__(other);
    }

    @ExposedMethod(type = MethodType.CMP)
    final int long___cmp__(PyObject other) {
        if (!canCoerce(other))
            return -2;
        return value.compareTo(coerce(other));
    }

    public Object __coerce_ex__(PyObject other) {
        if (other instanceof PyLong)
            return other;
        else
            if (other instanceof PyInteger) {
                return Py.newLong(((PyInteger)other).getValue());
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
            return BigInteger.valueOf(
                   ((PyInteger) other).getValue());
        else
            throw Py.TypeError("xxx");
    }

    public PyObject __add__(PyObject right) {
        return long___add__(right);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject long___add__(PyObject right) {
        if (!canCoerce(right))
            return null;
        return Py.newLong(value.add(coerce(right)));
    }

    public PyObject __radd__(PyObject left) {
        return long___radd__(left);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject long___radd__(PyObject left) {
        return __add__(left);
    }

    public PyObject __sub__(PyObject right) {
        return long___sub__(right);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject long___sub__(PyObject right) {
        if (!canCoerce(right))
            return null;
        return Py.newLong(value.subtract(coerce(right)));
    }

    public PyObject __rsub__(PyObject left) {
        return long___rsub__(left);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject long___rsub__(PyObject left) {
        return Py.newLong(coerce(left).subtract(value));
    }

    public PyObject __mul__(PyObject right) {
        return long___mul__(right);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject long___mul__(PyObject right) {
        if (right instanceof PySequence)
            return ((PySequence) right).repeat(coerceInt(this));

        if (!canCoerce(right))
            return null;
        return Py.newLong(value.multiply(coerce(right)));
    }

    public PyObject __rmul__(PyObject left) {
        return long___rmul__(left);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject long___rmul__(PyObject left) {
        if (left instanceof PySequence)
            return ((PySequence) left).repeat(coerceInt(this));
        if (!canCoerce(left))
            return null;
        return Py.newLong(coerce(left).multiply(value));
    }

    // Getting signs correct for integer division
    // This convention makes sense when you consider it in tandem with modulo
    private BigInteger divide(BigInteger x, BigInteger y) {
        BigInteger zero = BigInteger.valueOf(0);
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
        return long___div__(right);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject long___div__(PyObject right) {
        if (!canCoerce(right))
            return null;
        if (Options.divisionWarning > 0)
            Py.warning(Py.DeprecationWarning, "classic long division");
        return Py.newLong(divide(value, coerce(right)));
    }

    public PyObject __rdiv__(PyObject left) {
        return long___rdiv__(left);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject long___rdiv__(PyObject left) {
        if (!canCoerce(left))
            return null;
        if (Options.divisionWarning > 0)
            Py.warning(Py.DeprecationWarning, "classic long division");
        return Py.newLong(divide(coerce(left), value));
    }

    public PyObject __floordiv__(PyObject right) {
        return long___floordiv__(right);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject long___floordiv__(PyObject right) {
        if (!canCoerce(right))
            return null;
        return Py.newLong(divide(value, coerce(right)));
    }

    public PyObject __rfloordiv__(PyObject left) {
        return long___rfloordiv__(left);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject long___rfloordiv__(PyObject left) {
        if (!canCoerce(left))
            return null;
        return Py.newLong(divide(coerce(left), value));
    }

    private static final PyFloat true_divide(BigInteger a,BigInteger b) {
        int[] ae = new int[1];
        int[] be = new int[1];
        double ad,bd;

        ad = scaledDoubleValue(a,ae);
        bd = scaledDoubleValue(b,be);

        if (bd == 0 ) throw Py.ZeroDivisionError("long division or modulo");

        ad /= bd;
        int aexp = ae[0]-be[0];

        if (aexp > Integer.MAX_VALUE/8) {
            throw Py.OverflowError("long/long too large for a float");
        } else if ( aexp < -(Integer.MAX_VALUE/8)) {
            return new PyFloat(0.0);
        }

        ad = ad * Math.pow(2.0, aexp*8);

        if (Double.isInfinite(ad)) {
            throw Py.OverflowError("long/long too large for a float");
        }

        return new PyFloat(ad);
    }

    public PyObject __truediv__(PyObject right) {
        return long___truediv__(right);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject long___truediv__(PyObject right) {
        if (!canCoerce(right))
            return null;
        return true_divide(this.value,coerce(right));
    }

    public PyObject __rtruediv__(PyObject left) {
        return long___rtruediv__(left);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject long___rtruediv__(PyObject left) {
        if (!canCoerce(left))
            return null;
        return true_divide(coerce(left),this.value);
    }

    private BigInteger modulo(BigInteger x, BigInteger y, BigInteger xdivy) {
        return x.subtract(xdivy.multiply(y));
    }

    public PyObject __mod__(PyObject right) {
        return long___mod__(right);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject long___mod__(PyObject right) {
        if (!canCoerce(right))
            return null;
        BigInteger rightv = coerce(right);
        return Py.newLong(modulo(value, rightv, divide(value, rightv)));
    }

    public PyObject __rmod__(PyObject left) {
        return long___rmod__(left);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject long___rmod__(PyObject left) {
        if (!canCoerce(left))
            return null;
        BigInteger leftv = coerce(left);
        return Py.newLong(modulo(leftv, value, divide(leftv, value)));
    }

    public PyObject __divmod__(PyObject right) {
        return long___divmod__(right);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject long___divmod__(PyObject right) {
        if (!canCoerce(right))
            return null;
        BigInteger rightv = coerce(right);

        BigInteger xdivy = divide(value, rightv);
        return new PyTuple(Py.newLong(xdivy), Py.newLong(modulo(value, rightv, xdivy)));
    }

    public PyObject __rdivmod__(PyObject left) {
        return long___rdivmod__(left);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject long___rdivmod__(PyObject left) {
        if (!canCoerce(left))
            return null;
        BigInteger leftv = coerce(left);

        BigInteger xdivy = divide(leftv, value);
        return new PyTuple(Py.newLong(xdivy), Py.newLong(modulo(leftv, value, xdivy)));
    }

    public PyObject __pow__(PyObject right, PyObject modulo) {
        return long___pow__(right, modulo);
    }
    
    @ExposedMethod(type = MethodType.BINARY, defaults = {"null"})
    final PyObject long___pow__(PyObject right, PyObject modulo) {
        if (!canCoerce(right))
            return null;

        if (modulo != null && !canCoerce(right))
            return null;
        return _pow(value, coerce(right), modulo, this, right);
    }

    public PyObject __rpow__(PyObject left) {
        return long___rpow__(left);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject long___rpow__(PyObject left) {
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
            return Py.newLong(value.pow(y.intValue()));
        else {
            // This whole thing can be trivially rewritten after bugs
            // in modPow are fixed by SUN

            BigInteger z = coerce(modulo);
            int zi = z.intValue();
            // Clear up some special cases right away
            if (zi == 0)
                throw Py.ValueError("pow(x, y, z) with z == 0");
            if (zi == 1 || zi == -1)
                return Py.newLong(0);

            if (z.compareTo(BigInteger.valueOf(0)) <= 0) {
                // Handle negative modulo's specially
                /*if (z.compareTo(BigInteger.valueOf(0)) == 0) {
                  throw Py.ValueError("pow(x, y, z) with z == 0");
                  }*/
                y = value.modPow(y, z.negate());
                if (y.compareTo(BigInteger.valueOf(0)) > 0) {
                    return Py.newLong(z.add(y));
                } else {
                    return Py.newLong(y);
                }
                //return __pow__(right).__mod__(modulo);
            } else {
                // XXX: 1.1 no longer supported so review this.
                // This is buggy in SUN's jdk1.1.5
                // Extra __mod__ improves things slightly
                return Py.newLong(value.modPow(y, z));
                //return __pow__(right).__mod__(modulo);
            }
        }
    }

    private static final int coerceInt(PyObject other) {
        if (other instanceof PyLong)
            return ((PyLong)other).asInt();
        else if (other instanceof PyInteger)
            return ((PyInteger) other).getValue();
        else
            throw Py.TypeError("xxx");
    }

    public PyObject __lshift__(PyObject right) {
        return long___lshift__(right);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject long___lshift__(PyObject right) {
        if (!canCoerce(right))
            return null;
        int rightv = coerceInt(right);
        if(rightv < 0)
            throw Py.ValueError("negative shift count");
        return Py.newLong(value.shiftLeft(rightv));
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject long___rlshift__(PyObject left) {
        if (!canCoerce(left))
            return null;
        if(value.intValue() < 0)
            throw Py.ValueError("negative shift count");
        return Py.newLong(coerce(left).shiftLeft(coerceInt(this)));
    }

    public PyObject __rshift__(PyObject right) {
        return long___rshift__(right);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject long___rshift__(PyObject right) {
        if (!canCoerce(right))
            return null;
        int rightv = coerceInt(right);
        if(rightv < 0)
            throw Py.ValueError("negative shift count");
        return Py.newLong(value.shiftRight(rightv));
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject long___rrshift__(PyObject left) {
        if (!canCoerce(left))
            return null;
        if(value.intValue() < 0)
            throw Py.ValueError("negative shift count");
        return Py.newLong(coerce(left).shiftRight(coerceInt(this)));
    }

    public PyObject __and__(PyObject right) {
        return long___and__(right);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject long___and__(PyObject right) {
        if (!canCoerce(right))
            return null;
        return Py.newLong(value.and(coerce(right)));
    }

    public PyObject __rand__(PyObject left) {
        return long___rand__(left);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject long___rand__(PyObject left) {
        if (!canCoerce(left))
            return null;
        return Py.newLong(coerce(left).and(value));
    }

    public PyObject __xor__(PyObject right) {
        return long___xor__(right);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject long___xor__(PyObject right) {
        if (!canCoerce(right))
            return null;
        return Py.newLong(value.xor(coerce(right)));
    }

    public PyObject __rxor__(PyObject left) {
        return long___rxor__(left);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject long___rxor__(PyObject left) {
        if (!canCoerce(left))
            return null;
        return Py.newLong(coerce(left).xor(value));
    }

    public PyObject __or__(PyObject right) {
        return long___or__(right);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject long___or__(PyObject right) {
        if (!canCoerce(right))
            return null;
        return Py.newLong(value.or(coerce(right)));
    }

    public PyObject __ror__(PyObject left) {
        return long___ror__(left);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject long___ror__(PyObject left) {
        if (!canCoerce(left))
            return null;
        return Py.newLong(coerce(left).or(value));
    }

    @ExposedMethod
    final PyObject long___coerce__(PyObject other) {
        return __coerce__(other);
    }

    public PyObject __neg__() {
        return long___neg__();
    }

    @ExposedMethod
    final PyObject long___neg__() {
        return Py.newLong(value.negate());
    }

    public PyObject __pos__() {
        return long___pos__();
    }

    @ExposedMethod
    final PyObject long___pos__() {
        return Py.newLong(value);
    }

    public PyObject __abs__() {
        return long___abs__();
    }
    
    @ExposedMethod
    final PyObject long___abs__() {
        return Py.newLong(value.abs());
    }

    public PyObject __invert__() {
        return long___invert__();
    }

    @ExposedMethod
    final PyObject long___invert__() {
        return Py.newLong(value.not());
    }

    public PyObject __int__() {
        return long___int__();
    }

    @ExposedMethod
    final PyObject long___int__() {
        if (value.compareTo(PyInteger.maxInt) <= 0 && value.compareTo(PyInteger.minInt) >= 0) {
            return Py.newInteger(value.intValue());
        }
        return Py.newLong(value);
    }


    public PyLong __long__() {
        return long___long__();
    }

    @ExposedMethod
    final PyLong long___long__() {
        return Py.newLong(value);
    }

    public PyFloat __float__() {
        return long___float__();
    }

    @ExposedMethod
    final PyFloat long___float__() {
        return new PyFloat(doubleValue());
    }

    public PyComplex __complex__() {
        return long___complex__();
    }

    final PyComplex long___complex__() {
        return new PyComplex(doubleValue(), 0.);
    }

    public PyString __oct__() {
        return long___oct__();
    }

    @ExposedMethod
    final PyString long___oct__() {
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
        return long___hex__();
    }

    @ExposedMethod
    final PyString long___hex__() {
        String s = value.toString(16).toUpperCase();
        if (s.startsWith("-"))
            return new PyString("-0x"+s.substring(1, s.length())+"L");
        else
            return new PyString("0x"+s+"L");
    }

    public PyString __str__() {
        return Py.newString(value.toString());
    }
    
    public PyUnicode __unicode__() {
        return new PyUnicode(value.toString());
    }

    @ExposedMethod
    final PyTuple long___getnewargs__() {
        return new PyTuple(new PyLong(this.getValue()));
    }

    public PyTuple __getnewargs__() {
        return long___getnewargs__();
    }


    public boolean isMappingType() { return false; }
    public boolean isSequenceType() { return false; }

}
