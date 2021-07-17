// Copyright (c) Corporation for National Research Initiatives
// Copyright (c) Jython Developers

package org.python.core;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;

import org.python.core.stringlib.FloatFormatter;
import org.python.core.stringlib.IntegerFormatter;
import org.python.core.stringlib.InternalFormat;
import org.python.core.stringlib.InternalFormat.Formatter;
import org.python.core.stringlib.InternalFormat.Spec;
import org.python.expose.ExposedGet;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedType;
import org.python.expose.MethodType;

/**
 * A builtin python long. This is implemented as a java.math.BigInteger.
 */
@Untraversable
@ExposedType(name = "long", doc = BuiltinDocs.long_doc)
public class PyLong extends PyObject {

    public static final PyType TYPE = PyType.fromClass(PyLong.class);

    public static final BigInteger MIN_LONG = BigInteger.valueOf(Long.MIN_VALUE);
    public static final BigInteger MAX_LONG = BigInteger.valueOf(Long.MAX_VALUE);
    public static final BigInteger MAX_ULONG = BigInteger.valueOf(1).shiftLeft(64)
            .subtract(BigInteger.valueOf(1));

    /** @deprecated Use MIN_INT instead. */
    @Deprecated
    public static final BigInteger minLong = MIN_LONG;
    /** @deprecated Use MAX_INT instead. */
    @Deprecated
    public static final BigInteger maxLong = MAX_LONG;
    /** @deprecated Use MAX_ULONG instead. */
    @Deprecated
    public static final BigInteger maxULong = MAX_ULONG;

    private final BigInteger value;

    public BigInteger getValue() {
        return value;
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

    @ExposedNew
    public static PyObject long___new__(PyNewWrapper new_, boolean init, PyType subtype,
            PyObject[] args, String[] keywords) {
        if (new_.for_type != subtype) {
            return longSubtypeNew(new_, init, subtype, args, keywords);
        }

        ArgParser ap = new ArgParser("long", args, keywords, new String[] {"x", "base"}, 0);
        PyObject x = ap.getPyObject(0, null);
        if (x != null && x.getJavaProxy() instanceof BigInteger) {
            return new PyLong((BigInteger)x.getJavaProxy());
        }
        int base = ap.getInt(1, -909);

        if (x == null) {
            return new PyLong(0);
        }
        if (base == -909) {
            return asPyLong(x);
        }
        if (!(x instanceof PyString)) {
            throw Py.TypeError("long: can't convert non-string with explicit base");
        }
        return ((PyString)x).atol(base);
    }

    /**
     * @return convert to a long.
     * @throws TypeError and AttributeError.
     */
    private static PyObject asPyLong(PyObject x) {
        try {
            return x.__long__();
        } catch (PyException pye) {
            if (!pye.match(Py.AttributeError)) {
                throw pye;
            }
            try {
                PyObject integral = x.invoke("__trunc__");
                return convertIntegralToLong(integral);
            } catch (PyException pye2) {
                if (!pye2.match(Py.AttributeError)) {
                    throw pye2;
                }
                throw Py.TypeError(String.format(
                        "long() argument must be a string or a number, not '%.200s'", x.getType()
                                .fastGetName()));
            }
        }
    }

    /**
     * @return convert to an int.
     * @throws TypeError and AttributeError.
     */
    private static PyObject convertIntegralToLong(PyObject integral) {
        if (!(integral instanceof PyInteger) && !(integral instanceof PyLong)) {
            PyObject i = integral.invoke("__int__");
            if (!(i instanceof PyInteger) && !(i instanceof PyLong)) {
                throw Py.TypeError(String.format("__trunc__ returned non-Integral (type %.200s)",
                        integral.getType().fastGetName()));
            }
            return i;
        }
        return integral;
    }

    /**
     * Wimpy, slow approach to new calls for subtypes of long.
     *
     * First creates a regular long from whatever arguments we got, then allocates a subtype
     * instance and initializes it from the regular long. The regular long is then thrown away.
     */
    private static PyObject longSubtypeNew(PyNewWrapper new_, boolean init, PyType subtype,
            PyObject[] args, String[] keywords) {
        PyObject tmp = long___new__(new_, init, TYPE, args, keywords);
        if (tmp instanceof PyInteger) {
            int intValue = ((PyInteger)tmp).getValue();
            return new PyLongDerived(subtype, BigInteger.valueOf(intValue));
        } else {
            return new PyLongDerived(subtype, ((PyLong)tmp).getValue());
        }
    }

    /**
     * Convert a double to BigInteger, raising an OverflowError if infinite.
     */
    private static BigInteger toBigInteger(double value) {
        if (Double.isInfinite(value)) {
            throw Py.OverflowError("cannot convert float infinity to long");
        }
        if (Double.isNaN(value)) {
            throw Py.ValueError("cannot convert float NaN to integer");
        }
        return new BigDecimal(value).toBigInteger();
    }

    @ExposedGet(name = "real", doc = BuiltinDocs.long_real_doc)
    public PyObject getReal() {
        return long___long__();
    }

    @ExposedGet(name = "imag", doc = BuiltinDocs.long_imag_doc)
    public PyObject getImag() {
        return Py.newLong(0);
    }

    @ExposedGet(name = "numerator", doc = BuiltinDocs.long_numerator_doc)
    public PyObject getNumerator() {
        return long___long__();
    }

    @ExposedGet(name = "denominator", doc = BuiltinDocs.long_denominator_doc)
    public PyObject getDenominator() {
        return Py.newLong(1);
    }

    @Override
    public String toString() {
        return long_toString();
    }

    @ExposedMethod(names = "__repr__", doc = BuiltinDocs.long___repr___doc)
    final String long_toString() {
        return getValue().toString() + "L";
    }

    @Override
    public int hashCode() {
        return long___hash__();
    }

    @ExposedMethod(doc = BuiltinDocs.long___hash___doc)
    final int long___hash__() {
        return getValue().hashCode();
    }

    @Override
    public boolean __nonzero__() {
        return long___nonzero__();
    }

    @ExposedMethod(doc = BuiltinDocs.long___nonzero___doc)
    public boolean long___nonzero__() {
        return !getValue().equals(BigInteger.ZERO);
    }

    public double doubleValue() {
        double v = getValue().doubleValue();
        if (Double.isInfinite(v)) {
            throw Py.OverflowError("long int too large to convert to float");
        }
        return v;
    }

    private static final double scaledDoubleValue(BigInteger val, int[] exp) {
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
        count = count <= digits.length ? count : digits.length;

        while (i < count) {
            x = x * 256 + (digits[i] & 0xff);
            i++;
        }
        exp[0] = digits.length - i;
        return signum * x;
    }

    public double scaledDoubleValue(int[] exp) {
        return scaledDoubleValue(getValue(), exp);
    }

    public long getLong(long min, long max) {
        return getLong(min, max, "long int too large to convert");
    }

    public long getLong(long min, long max, String overflowMsg) {
        if (getValue().compareTo(MAX_LONG) <= 0 && getValue().compareTo(MIN_LONG) >= 0) {
            long v = getValue().longValue();
            if (v >= min && v <= max) {
                return v;
            }
        }
        throw Py.OverflowError(overflowMsg);
    }

    @Override
    public long asLong(int index) {
        return asLong();
    }

    @Override
    public int asInt(int index) {
        return (int)getLong(Integer.MIN_VALUE, Integer.MAX_VALUE,
                "long int too large to convert to int");
    }

    @Override
    public int asInt() {
        return (int)getLong(Integer.MIN_VALUE, Integer.MAX_VALUE,
                "long int too large to convert to int");
    }

    @Override
    public long asLong() {
        return getLong(Long.MIN_VALUE, Long.MAX_VALUE, "long too big to convert");
    }

    @Override
    public Object __tojava__(Class<?> c) {
        try {
            if (c == Boolean.TYPE || c == Boolean.class) {
                return Boolean.valueOf(!getValue().equals(BigInteger.ZERO));
            }
            if (c == Byte.TYPE || c == Byte.class) {
                return Byte.valueOf((byte)getLong(Byte.MIN_VALUE, Byte.MAX_VALUE));
            }
            if (c == Short.TYPE || c == Short.class) {
                return Short.valueOf((short)getLong(Short.MIN_VALUE, Short.MAX_VALUE));
            }
            if (c == Integer.TYPE || c == Integer.class) {
                return Integer.valueOf((int)getLong(Integer.MIN_VALUE, Integer.MAX_VALUE));
            }
            if (c == Long.TYPE || c == Long.class) {
                return Long.valueOf(getLong(Long.MIN_VALUE, Long.MAX_VALUE));
            }
            if (c == Float.TYPE || c == Double.TYPE || c == Float.class || c == Double.class) {
                return __float__().__tojava__(c);
            }
            if (c == BigInteger.class || c == Number.class || c == Object.class
                    || c == Serializable.class) {
                return getValue();
            }
        } catch (PyException e) {
            return Py.NoConversion;
        }
        return super.__tojava__(c);
    }

    @Override
    public int __cmp__(PyObject other) {
        return long___cmp__(other);
    }

    @ExposedMethod(type = MethodType.CMP, doc = BuiltinDocs.long___cmp___doc)
    final int long___cmp__(PyObject other) {
        if (!canCoerce(other)) {
            return -2;
        }
        return getValue().compareTo(coerce(other));
    }

    @Override
    public Object __coerce_ex__(PyObject other) {
        return long___coerce_ex__(other);
    }

    @ExposedMethod(doc = BuiltinDocs.long___coerce___doc)
    final PyObject long___coerce__(PyObject other) {
        return adaptToCoerceTuple(long___coerce_ex__(other));
    }

    /**
     * Coercion logic for long. Implemented as a final method to avoid invocation of virtual methods
     * from the exposed coerce.
     */
    final Object long___coerce_ex__(PyObject other) {
        if (other instanceof PyLong) {
            return other;
        } else if (other instanceof PyInteger) {
            return Py.newLong(((PyInteger)other).getValue());
        } else {
            return Py.None;
        }
    }

    private static final boolean canCoerce(PyObject other) {
        return other instanceof PyLong || other instanceof PyInteger;
    }

    private static final BigInteger coerce(PyObject other) {
        if (other instanceof PyLong) {
            return ((PyLong)other).getValue();
        } else if (other instanceof PyInteger) {
            return BigInteger.valueOf(((PyInteger)other).getValue());
        } else {
            throw Py.TypeError("xxx");
        }
    }

    @Override
    public PyObject __add__(PyObject right) {
        return long___add__(right);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.long___add___doc)
    final PyObject long___add__(PyObject right) {
        if (!canCoerce(right)) {
            return null;
        }
        return Py.newLong(getValue().add(coerce(right)));
    }

    @Override
    public PyObject __radd__(PyObject left) {
        return long___radd__(left);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.long___radd___doc)
    final PyObject long___radd__(PyObject left) {
        return __add__(left);
    }

    @Override
    public PyObject __sub__(PyObject right) {
        return long___sub__(right);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.long___sub___doc)
    final PyObject long___sub__(PyObject right) {
        if (!canCoerce(right)) {
            return null;
        }
        return Py.newLong(getValue().subtract(coerce(right)));
    }

    @Override
    public PyObject __rsub__(PyObject left) {
        return long___rsub__(left);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.long___rsub___doc)
    final PyObject long___rsub__(PyObject left) {
        return Py.newLong(coerce(left).subtract(getValue()));
    }

    @Override
    public PyObject __mul__(PyObject right) {
        return long___mul__(right);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.long___mul___doc)
    final PyObject long___mul__(PyObject right) {
        if (right instanceof PySequence) {
            return ((PySequence)right).repeat(coerceInt(this));
        }

        if (!canCoerce(right)) {
            return null;
        }
        return Py.newLong(getValue().multiply(coerce(right)));
    }

    @Override
    public PyObject __rmul__(PyObject left) {
        return long___rmul__(left);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.long___rmul___doc)
    final PyObject long___rmul__(PyObject left) {
        if (left instanceof PySequence) {
            return ((PySequence)left).repeat(coerceInt(this));
        }
        if (!canCoerce(left)) {
            return null;
        }
        return Py.newLong(coerce(left).multiply(getValue()));
    }

    // Getting signs correct for integer division
    // This convention makes sense when you consider it in tandem with modulo
    private BigInteger divide(BigInteger x, BigInteger y) {
        BigInteger zero = BigInteger.valueOf(0);
        if (y.equals(zero)) {
            throw Py.ZeroDivisionError("long division or modulo");
        }

        if (y.compareTo(zero) < 0) {
            if (x.compareTo(zero) > 0) {
                return (x.subtract(y).subtract(BigInteger.valueOf(1))).divide(y);
            }
        } else {
            if (x.compareTo(zero) < 0) {
                return (x.subtract(y).add(BigInteger.valueOf(1))).divide(y);
            }
        }
        return x.divide(y);
    }

    @Override
    public PyObject __div__(PyObject right) {
        return long___div__(right);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.long___div___doc)
    final PyObject long___div__(PyObject right) {
        if (!canCoerce(right)) {
            return null;
        }
        if (Options.division_warning > 0) {
            Py.warning(Py.DeprecationWarning, "classic long division");
        }
        return Py.newLong(divide(getValue(), coerce(right)));
    }

    @Override
    public PyObject __rdiv__(PyObject left) {
        return long___rdiv__(left);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.long___rdiv___doc)
    final PyObject long___rdiv__(PyObject left) {
        if (!canCoerce(left)) {
            return null;
        }
        if (Options.division_warning > 0) {
            Py.warning(Py.DeprecationWarning, "classic long division");
        }
        return Py.newLong(divide(coerce(left), getValue()));
    }

    @Override
    public PyObject __floordiv__(PyObject right) {
        return long___floordiv__(right);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.long___floordiv___doc)
    final PyObject long___floordiv__(PyObject right) {
        if (!canCoerce(right)) {
            return null;
        }
        return Py.newLong(divide(getValue(), coerce(right)));
    }

    @Override
    public PyObject __rfloordiv__(PyObject left) {
        return long___rfloordiv__(left);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.long___rfloordiv___doc)
    final PyObject long___rfloordiv__(PyObject left) {
        if (!canCoerce(left)) {
            return null;
        }
        return Py.newLong(divide(coerce(left), getValue()));
    }

    private static final PyFloat true_divide(BigInteger a, BigInteger b) {
        int[] ae = new int[1];
        int[] be = new int[1];
        double ad, bd;

        ad = scaledDoubleValue(a, ae);
        bd = scaledDoubleValue(b, be);

        if (bd == 0) {
            throw Py.ZeroDivisionError("long division or modulo");
        }

        ad /= bd;
        int aexp = ae[0] - be[0];

        if (aexp > Integer.MAX_VALUE / 8) {
            throw Py.OverflowError("long/long too large for a float");
        } else if (aexp < -(Integer.MAX_VALUE / 8)) {
            return PyFloat.ZERO;
        }

        ad = ad * Math.pow(2.0, aexp * 8);

        if (Double.isInfinite(ad)) {
            throw Py.OverflowError("long/long too large for a float");
        }

        return new PyFloat(ad);
    }

    @Override
    public PyObject __truediv__(PyObject right) {
        return long___truediv__(right);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.long___truediv___doc)
    final PyObject long___truediv__(PyObject right) {
        if (!canCoerce(right)) {
            return null;
        }
        return true_divide(this.getValue(), coerce(right));
    }

    @Override
    public PyObject __rtruediv__(PyObject left) {
        return long___rtruediv__(left);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.long___rtruediv___doc)
    final PyObject long___rtruediv__(PyObject left) {
        if (!canCoerce(left)) {
            return null;
        }
        return true_divide(coerce(left), this.getValue());
    }

    private BigInteger modulo(BigInteger x, BigInteger y, BigInteger xdivy) {
        return x.subtract(xdivy.multiply(y));
    }

    @Override
    public PyObject __mod__(PyObject right) {
        return long___mod__(right);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.long___mod___doc)
    final PyObject long___mod__(PyObject right) {
        if (!canCoerce(right)) {
            return null;
        }
        BigInteger rightv = coerce(right);
        return Py.newLong(modulo(getValue(), rightv, divide(getValue(), rightv)));
    }

    @Override
    public PyObject __rmod__(PyObject left) {
        return long___rmod__(left);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.long___rmod___doc)
    final PyObject long___rmod__(PyObject left) {
        if (!canCoerce(left)) {
            return null;
        }
        BigInteger leftv = coerce(left);
        return Py.newLong(modulo(leftv, getValue(), divide(leftv, getValue())));
    }

    @Override
    public PyObject __divmod__(PyObject right) {
        return long___divmod__(right);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.long___divmod___doc)
    final PyObject long___divmod__(PyObject right) {
        if (!canCoerce(right)) {
            return null;
        }
        BigInteger rightv = coerce(right);

        BigInteger xdivy = divide(getValue(), rightv);
        return new PyTuple(Py.newLong(xdivy), Py.newLong(modulo(getValue(), rightv, xdivy)));
    }

    @Override
    public PyObject __rdivmod__(PyObject left) {
        return long___rdivmod__(left);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.long___rdivmod___doc)
    final PyObject long___rdivmod__(PyObject left) {
        if (!canCoerce(left)) {
            return null;
        }
        BigInteger leftv = coerce(left);

        BigInteger xdivy = divide(leftv, getValue());
        return new PyTuple(Py.newLong(xdivy), Py.newLong(modulo(leftv, getValue(), xdivy)));
    }

    @Override
    public PyObject __pow__(PyObject right, PyObject modulo) {
        return long___pow__(right, modulo);
    }

    @ExposedMethod(type = MethodType.BINARY, defaults = {"null"},
            doc = BuiltinDocs.long___pow___doc)
    final PyObject long___pow__(PyObject right, PyObject modulo) {
        if (!canCoerce(right)) {
            return null;
        }

        modulo = (modulo == Py.None) ? null : modulo;
        if (modulo != null && !canCoerce(modulo)) {
            return null;
        }

        return _pow(getValue(), coerce(right), modulo, this, right);
    }

    @Override
    public PyObject __rpow__(PyObject left) {
        return long___rpow__(left);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.long___rpow___doc)
    final PyObject long___rpow__(PyObject left) {
        if (!canCoerce(left)) {
            return null;
        }

        return _pow(coerce(left), getValue(), null, left, this);
    }

    public static PyObject _pow(BigInteger value, BigInteger y, PyObject modulo, PyObject left,
            PyObject right) {
        if (y.compareTo(BigInteger.ZERO) < 0) {
            if (value.compareTo(BigInteger.ZERO) != 0) {
                return left.__float__().__pow__(right, modulo);
            } else {
                throw Py.ZeroDivisionError("zero to a negative power");
            }
        }
        if (modulo == null) {
            return Py.newLong(value.pow(y.intValue()));
        } else {
            // This whole thing can be trivially rewritten after bugs
            // in modPow are fixed by SUN

            BigInteger z = coerce(modulo);
            // Clear up some special cases right away
            if (z.equals(BigInteger.ZERO)) {
                throw Py.ValueError("pow(x, y, z) with z == 0");
            }
            if (z.abs().equals(BigInteger.ONE)) {
                return Py.newLong(0);
            }

            if (z.compareTo(BigInteger.valueOf(0)) <= 0) {
                // Handle negative modulo specially
                // if (z.compareTo(BigInteger.valueOf(0)) == 0) {
                // throw Py.ValueError("pow(x, y, z) with z == 0");
                // }
                y = value.modPow(y, z.negate());
                if (y.compareTo(BigInteger.valueOf(0)) > 0) {
                    return Py.newLong(z.add(y));
                } else {
                    return Py.newLong(y);
                }
                // return __pow__(right).__mod__(modulo);
            } else {
                // XXX: 1.1 no longer supported so review this.
                // This is buggy in SUN's jdk1.1.5
                // Extra __mod__ improves things slightly
                return Py.newLong(value.modPow(y, z));
                // return __pow__(right).__mod__(modulo);
            }
        }
    }

    private static final int coerceInt(PyObject other) {
        if (other instanceof PyLong) {
            return ((PyLong)other).asInt();
        } else if (other instanceof PyInteger) {
            return ((PyInteger)other).getValue();
        } else {
            throw Py.TypeError("xxx");
        }
    }

    @Override
    public PyObject __lshift__(PyObject right) {
        return long___lshift__(right);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.long___lshift___doc)
    final PyObject long___lshift__(PyObject right) {
        if (!canCoerce(right)) {
            return null;
        }
        int rightv = coerceInt(right);
        if (rightv < 0) {
            throw Py.ValueError("negative shift count");
        }
        return Py.newLong(getValue().shiftLeft(rightv));
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.long___rlshift___doc)
    final PyObject long___rlshift__(PyObject left) {
        if (!canCoerce(left)) {
            return null;
        }
        if (getValue().intValue() < 0) {
            throw Py.ValueError("negative shift count");
        }
        return Py.newLong(coerce(left).shiftLeft(coerceInt(this)));
    }

    @Override
    public PyObject __rshift__(PyObject right) {
        return long___rshift__(right);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.long___rshift___doc)
    final PyObject long___rshift__(PyObject right) {
        if (!canCoerce(right)) {
            return null;
        }
        int rightv = coerceInt(right);
        if (rightv < 0) {
            throw Py.ValueError("negative shift count");
        }
        return Py.newLong(getValue().shiftRight(rightv));
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.long___rrshift___doc)
    final PyObject long___rrshift__(PyObject left) {
        if (!canCoerce(left)) {
            return null;
        }
        if (getValue().intValue() < 0) {
            throw Py.ValueError("negative shift count");
        }
        return Py.newLong(coerce(left).shiftRight(coerceInt(this)));
    }

    @Override
    public PyObject __and__(PyObject right) {
        return long___and__(right);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.long___and___doc)
    final PyObject long___and__(PyObject right) {
        if (!canCoerce(right)) {
            return null;
        }
        return Py.newLong(getValue().and(coerce(right)));
    }

    @Override
    public PyObject __rand__(PyObject left) {
        return long___rand__(left);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.long___rand___doc)
    final PyObject long___rand__(PyObject left) {
        if (!canCoerce(left)) {
            return null;
        }
        return Py.newLong(coerce(left).and(getValue()));
    }

    @Override
    public PyObject __xor__(PyObject right) {
        return long___xor__(right);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.long___xor___doc)
    final PyObject long___xor__(PyObject right) {
        if (!canCoerce(right)) {
            return null;
        }
        return Py.newLong(getValue().xor(coerce(right)));
    }

    @Override
    public PyObject __rxor__(PyObject left) {
        return long___rxor__(left);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.long___rxor___doc)
    final PyObject long___rxor__(PyObject left) {
        if (!canCoerce(left)) {
            return null;
        }
        return Py.newLong(coerce(left).xor(getValue()));
    }

    @Override
    public PyObject __or__(PyObject right) {
        return long___or__(right);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.long___or___doc)
    final PyObject long___or__(PyObject right) {
        if (!canCoerce(right)) {
            return null;
        }
        return Py.newLong(getValue().or(coerce(right)));
    }

    @Override
    public PyObject __ror__(PyObject left) {
        return long___ror__(left);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.long___ror___doc)
    final PyObject long___ror__(PyObject left) {
        if (!canCoerce(left)) {
            return null;
        }
        return Py.newLong(coerce(left).or(getValue()));
    }

    @Override
    public PyObject __neg__() {
        return long___neg__();
    }

    @ExposedMethod(doc = BuiltinDocs.long___neg___doc)
    final PyObject long___neg__() {
        return Py.newLong(getValue().negate());
    }

    @Override
    public PyObject __pos__() {
        return long___pos__();
    }

    @ExposedMethod(doc = BuiltinDocs.long___pos___doc)
    final PyObject long___pos__() {
        return long___long__();
    }

    @Override
    public PyObject __abs__() {
        return long___abs__();
    }

    @ExposedMethod(doc = BuiltinDocs.long___abs___doc)
    final PyObject long___abs__() {
        if (getValue().signum() == -1) {
            return long___neg__();
        }
        return long___long__();
    }

    @Override
    public PyObject __invert__() {
        return long___invert__();
    }

    @ExposedMethod(doc = BuiltinDocs.long___invert___doc)
    final PyObject long___invert__() {
        return Py.newLong(getValue().not());
    }

    @Override
    public PyObject __int__() {
        return long___int__();
    }

    @ExposedMethod(doc = BuiltinDocs.long___int___doc)
    final PyObject long___int__() {
        if (getValue().compareTo(PyInteger.MAX_INT) <= 0
                && getValue().compareTo(PyInteger.MIN_INT) >= 0) {
            return Py.newInteger(getValue().intValue());
        }
        return long___long__();
    }

    @Override
    public PyObject __long__() {
        return long___long__();
    }

    @ExposedMethod(doc = BuiltinDocs.long___long___doc)
    final PyObject long___long__() {
        return getType() == TYPE ? this : Py.newLong(getValue());
    }

    @Override
    public PyFloat __float__() {
        return long___float__();
    }

    @ExposedMethod(doc = BuiltinDocs.long___float___doc)
    final PyFloat long___float__() {
        return new PyFloat(doubleValue());
    }

    @Override
    public PyComplex __complex__() {
        return long___complex__();
    }

    final PyComplex long___complex__() {
        return new PyComplex(doubleValue(), 0.);
    }

    @Override
    public PyObject __trunc__() {
        return long___trunc__();
    }

    @ExposedMethod(doc = BuiltinDocs.long___trunc___doc)
    final PyObject long___trunc__() {
        return this;
    }

    @Override
    public PyObject conjugate() {
        return long_conjugate();
    }

    @ExposedMethod(doc = BuiltinDocs.long_conjugate_doc)
    final PyObject long_conjugate() {
        return this;
    }

    @Override
    public PyString __oct__() {
        return long___oct__();
    }

    @ExposedMethod(doc = BuiltinDocs.long___oct___doc)
    final PyString long___oct__() {
        // Use the prepared format specifier for octal.
        return formatImpl(IntegerFormatter.OCT);
    }

    @Override
    public PyString __hex__() {
        return long___hex__();
    }

    @ExposedMethod(doc = BuiltinDocs.long___hex___doc)
    final PyString long___hex__() {
        // Use the prepared format specifier for hexadecimal.
        return formatImpl(IntegerFormatter.HEX);
    }

    /**
     * Common code used by the number-base conversion method __oct__ and __hex__.
     *
     * @param spec prepared format-specifier.
     * @return converted value of this object
     */
    private PyString formatImpl(Spec spec) {
        // Traditional formatter (%-format) because #o means "-0123" not "-0o123".
        IntegerFormatter f = new IntegerFormatter.Traditional(spec);
        f.format(value).append('L');
        return new PyString(f.getResult());
    }

    @ExposedMethod(doc = BuiltinDocs.long___str___doc)
    public PyString long___str__() {
        return Py.newString(getValue().toString());
    }

    @Override
    public PyString __str__() {
        return long___str__();
    }

    @Override
    public PyUnicode __unicode__() {
        return new PyUnicode(getValue().toString());
    }

    @ExposedMethod(doc = BuiltinDocs.long___getnewargs___doc)
    final PyTuple long___getnewargs__() {
        return new PyTuple(new PyLong(this.getValue()));
    }

    @Override
    public PyTuple __getnewargs__() {
        return long___getnewargs__();
    }

    @Override
    public PyObject __index__() {
        return long___index__();
    }

    @ExposedMethod(doc = BuiltinDocs.long___index___doc)
    final PyObject long___index__() {
        return this;
    }

    @Override
    public int bit_length() {
        return long_bit_length();
    }

    @ExposedMethod(doc = BuiltinDocs.long_bit_length_doc)
    final int long_bit_length() {
        BigInteger v = value;
        if (v.compareTo(BigInteger.ZERO) == -1) {
            v = v.negate();
        }
        return v.bitLength();
    }

    @Override
    public PyObject __format__(PyObject formatSpec) {
        return long___format__(formatSpec);
    }

    @ExposedMethod(doc = BuiltinDocs.long___format___doc)
    final PyObject long___format__(PyObject formatSpec) {

        // Parse the specification
        Spec spec = InternalFormat.fromText(formatSpec, "__format__");
        InternalFormat.Formatter f;

        // Try to make an integer formatter from the specification
        IntegerFormatter fi = PyInteger.prepareFormatter(spec);
        if (fi != null) {
            // Bytes mode if formatSpec argument is not unicode.
            fi.setBytes(!(formatSpec instanceof PyUnicode));
            // Convert as per specification.
            fi.format(value);
            f = fi;

        } else {
            // Try to make a float formatter from the specification
            FloatFormatter ff = PyFloat.prepareFormatter(spec);
            if (ff != null) {
                // Bytes mode if formatSpec argument is not unicode.
                ff.setBytes(!(formatSpec instanceof PyUnicode));
                // Convert as per specification.
                ff.format(value.doubleValue());
                f = ff;

            } else {
                // The type code was not recognised in either prepareFormatter
                throw Formatter.unknownFormat(spec.type, "integer");
            }
        }

        // Return a result that has the same type (str or unicode) as the formatSpec argument.
        return f.pad().getPyResult();
    }

    @Override
    public boolean isIndex() {
        return true;
    }

    @Override
    public int asIndex(PyObject err) {
        boolean tooLow = getValue().compareTo(PyInteger.MIN_INT) < 0;
        boolean tooHigh = getValue().compareTo(PyInteger.MAX_INT) > 0;
        if (tooLow || tooHigh) {
            if (err != null) {
                throw new PyException(err, "cannot fit 'long' into an index-sized integer");
            }
            return tooLow ? Integer.MIN_VALUE : Integer.MAX_VALUE;
        }
        return (int)getValue().longValue();
    }

    @Override
    public boolean isMappingType() {
        return false;
    }

    @Override
    public boolean isNumberType() {
        return true;
    }

    @Override
    public boolean isSequenceType() {
        return false;
    }
}
