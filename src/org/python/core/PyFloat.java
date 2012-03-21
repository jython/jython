/*
 * Copyright (c) Corporation for National Research Initiatives
 * Copyright (c) Jython Developers
 */
package org.python.core;

import java.io.Serializable;
import java.math.BigDecimal;

import org.python.expose.ExposedClassMethod;
import org.python.expose.ExposedGet;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedType;
import org.python.expose.MethodType;

/**
 * A builtin python float.
 */
@ExposedType(name = "float", doc = BuiltinDocs.float_doc)
public class PyFloat extends PyObject {

    public static final PyType TYPE = PyType.fromClass(PyFloat.class);

    /** Precisions used by repr() and str(), respectively. */
    private static final int PREC_REPR = 17;
    private static final int PREC_STR = 12;

    private final double value;

    public double getValue() {
        return value;
    }

    public PyFloat(PyType subtype, double v) {
        super(subtype);
        value = v;
    }

    public PyFloat(double v) {
        this(TYPE, v);
    }

    public PyFloat(float v) {
        this((double) v);
    }

    @ExposedNew
    public static PyObject float_new(PyNewWrapper new_, boolean init, PyType subtype,
                                     PyObject[] args, String[] keywords) {
        ArgParser ap = new ArgParser("float", args, keywords, new String[] {"x"}, 0);
        PyObject x = ap.getPyObject(0, null);
        if (x == null) {
            if (new_.for_type == subtype) {
                return new PyFloat(0.0);
            } else {
                return new PyFloatDerived(subtype, 0.0);
            }
        } else {
            PyFloat floatObject = null;
            try {
                floatObject = x.__float__();
            } catch (PyException e) {
                if (e.match(Py.AttributeError)) {
                    // Translate AttributeError to TypeError
                    // XXX: We are using the same message as CPython, even if
                    //      it is not strictly correct (instances of types
                    //      that implement the __float__ method are also
                    //      valid arguments)
                    throw Py.TypeError("float() argument must be a string or a number");
                }
                throw e;
            }
            if (new_.for_type == subtype) {
                return floatObject;
            } else {
                return new PyFloatDerived(subtype, floatObject.getValue());
            }
        }
    }

    @ExposedGet(name = "real", doc = BuiltinDocs.float_real_doc)
    public PyObject getReal() {
        return float___float__();
    }

    @ExposedGet(name = "imag", doc = BuiltinDocs.float_imag_doc)
    public PyObject getImag() {
        return Py.newFloat(0.0);
    }

    /**
     * Determine if this float is not infinity, nor NaN.
     */
    public boolean isFinite() {
        return !Double.isInfinite(getValue()) && !Double.isNaN(getValue());
    }

    @Override
    public String toString() {
        return __str__().toString();
    }

    @Override
    public PyString __str__() {
        return float___str__();
    }

    @ExposedMethod(doc = BuiltinDocs.float___str___doc)
    final PyString float___str__() {
        return Py.newString(formatDouble(PREC_STR));
    }

    @Override
    public PyString __repr__() {
        return float___repr__();
    }

    @ExposedMethod(doc = BuiltinDocs.float___repr___doc)
    final PyString float___repr__() {
        return Py.newString(formatDouble(PREC_REPR));
    }

    private String formatDouble(int precision) {
        if (Double.isNaN(value))
            return "nan";
        else if (value == Double.NEGATIVE_INFINITY)
            return "-inf";
        else if (value == Double.POSITIVE_INFINITY)
            return "inf";

        String result = String.format("%%.%dg", precision);
        result = Py.newString(result).__mod__(this).toString();

        int i = 0;
        if (result.startsWith("-")) {
            i++;
        }
        for (; i < result.length(); i++) {
            if (!Character.isDigit(result.charAt(i))) {
                break;
            }
        }
        if (i == result.length()) {
            result += ".0";
        }
        return result;
    }

    @Override
    public int hashCode() {
        return float___hash__();
    }

    @ExposedMethod(doc = BuiltinDocs.float___hash___doc)
    final int float___hash__() {
        double intPart = Math.floor(getValue());
        double fractPart = getValue() - intPart;

        if (fractPart == 0) {
            if (intPart <= Integer.MAX_VALUE && intPart >= Integer.MIN_VALUE) {
                return (int) getValue();
            } else {
                return __long__().hashCode();
            }
        } else {
            long v = Double.doubleToLongBits(getValue());
            return (int) v ^ (int) (v >> 32);
        }
    }

    @Override
    public boolean __nonzero__() {
        return float___nonzero__();
    }

    @ExposedMethod(doc = BuiltinDocs.float___nonzero___doc)
    final boolean float___nonzero__() {
        return getValue() != 0;
    }

    @Override
    public Object __tojava__(Class<?> c) {
        if (c == Double.TYPE || c == Number.class || c == Double.class || c == Object.class
            || c == Serializable.class) {
            return new Double(getValue());
        }
        if (c == Float.TYPE || c == Float.class) {
            return new Float(getValue());
        }
        return super.__tojava__(c);
    }

    @Override
    public PyObject __eq__(PyObject other) {
        // preclude _cmp_unsafe's this == other shortcut because NaN != anything, even
        // itself
        if (Double.isNaN(getValue())) {
            return Py.False;
        }
        return null;
    }

    @Override
    public PyObject __ne__(PyObject other) {
        if (Double.isNaN(getValue())) {
            return Py.True;
        }
        return null;
    }

    @Override
    public int __cmp__(PyObject other) {
        return float___cmp__(other);
    }

    // XXX: needs __doc__
    @ExposedMethod(type = MethodType.CMP)
    final int float___cmp__(PyObject other) {
        double i = getValue();
        double j;

        if (other instanceof PyFloat) {
            j = ((PyFloat) other).getValue();
        } else if (!isFinite()) {
            // we're infinity: our magnitude exceeds any finite
            // integer, so it doesn't matter which int we compare i
            // with. If NaN, similarly.
            if (other instanceof PyInteger || other instanceof PyLong) {
                j = 0.0;
            } else {
                return -2;
            }
        } else if (other instanceof PyInteger) {
            j = ((PyInteger) other).getValue();
        } else if (other instanceof PyLong) {
            BigDecimal v = new BigDecimal(getValue());
            BigDecimal w = new BigDecimal(((PyLong) other).getValue());
            return v.compareTo(w);
        } else {
            return -2;
        }

        if (i < j) {
            return -1;
        } else if (i > j) {
            return 1;
        } else if (i == j) {
            return 0;
        } else {
            // at least one side is NaN
            return Double.isNaN(i) ? (Double.isNaN(j) ? 1 : -1) : 1;
        }
    }

    @Override
    public Object __coerce_ex__(PyObject other) {
        return float___coerce_ex__(other);
    }

    @ExposedMethod(doc = BuiltinDocs.float___coerce___doc)
    final PyObject float___coerce__(PyObject other) {
        return adaptToCoerceTuple(float___coerce_ex__(other));
    }

    /**
     * Coercion logic for float. Implemented as a final method to avoid
     * invocation of virtual methods from the exposed coerce.
     */
    final Object float___coerce_ex__(PyObject other) {
        if (other instanceof PyFloat) {
            return other;
        } else {
            if (other instanceof PyInteger) {
                return new PyFloat((double) ((PyInteger) other).getValue());
            }
            if (other instanceof PyLong) {
                return new PyFloat(((PyLong) other).doubleValue());
            } else {
                return Py.None;
            }
        }
    }

    private static boolean canCoerce(PyObject other) {
        return other instanceof PyFloat || other instanceof PyInteger || other instanceof PyLong;
    }

    private static double coerce(PyObject other) {
        if (other instanceof PyFloat) {
            return ((PyFloat) other).getValue();
        } else if (other instanceof PyInteger) {
            return ((PyInteger) other).getValue();
        } else if (other instanceof PyLong) {
            return ((PyLong) other).doubleValue();
        } else {
            throw Py.TypeError("xxx");
        }
    }

    @Override
    public PyObject __add__(PyObject right) {
        return float___add__(right);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.float___add___doc)
    final PyObject float___add__(PyObject right) {
        if (!canCoerce(right)) {
            return null;
        }
        double rightv = coerce(right);
        return new PyFloat(getValue() + rightv);
    }

    @Override
    public PyObject __radd__(PyObject left) {
        return float___radd__(left);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.float___radd___doc)
    final PyObject float___radd__(PyObject left) {
        return __add__(left);
    }

    @Override
    public PyObject __sub__(PyObject right) {
        return float___sub__(right);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.float___sub___doc)
    final PyObject float___sub__(PyObject right) {
        if (!canCoerce(right)) {
            return null;
        }
        double rightv = coerce(right);
        return new PyFloat(getValue() - rightv);
    }

    @Override
    public PyObject __rsub__(PyObject left) {
        return float___rsub__(left);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.float___rsub___doc)
    final PyObject float___rsub__(PyObject left) {
        if (!canCoerce(left)) {
            return null;
        }
        double leftv = coerce(left);
        return new PyFloat(leftv - getValue());
    }

    @Override
    public PyObject __mul__(PyObject right) {
        return float___mul__(right);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.float___mul___doc)
    final PyObject float___mul__(PyObject right) {
        if (!canCoerce(right)) {
            return null;
        }
        double rightv = coerce(right);
        return new PyFloat(getValue() * rightv);
    }

    @Override
    public PyObject __rmul__(PyObject left) {
        return float___rmul__(left);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.float___rmul___doc)
    final PyObject float___rmul__(PyObject left) {
        return __mul__(left);
    }

    @Override
    public PyObject __div__(PyObject right) {
        return float___div__(right);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.float___div___doc)
    final PyObject float___div__(PyObject right) {
        if (!canCoerce(right)) {
            return null;
        }
        if (Options.division_warning >= 2) {
            Py.warning(Py.DeprecationWarning, "classic float division");
        }

        double rightv = coerce(right);
        if (rightv == 0) {
            throw Py.ZeroDivisionError("float division");
        }
        return new PyFloat(getValue() / rightv);
    }

    @Override
    public PyObject __rdiv__(PyObject left) {
        return float___rdiv__(left);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.float___rdiv___doc)
    final PyObject float___rdiv__(PyObject left) {
        if (!canCoerce(left)) {
            return null;
        }
        if (Options.division_warning >= 2) {
            Py.warning(Py.DeprecationWarning, "classic float division");
        }

        double leftv = coerce(left);
        if (getValue() == 0) {
            throw Py.ZeroDivisionError("float division");
        }
        return new PyFloat(leftv / getValue());
    }

    @Override
    public PyObject __floordiv__(PyObject right) {
        return float___floordiv__(right);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.float___floordiv___doc)
    final PyObject float___floordiv__(PyObject right) {
        if (!canCoerce(right)) {
            return null;
        }
        double rightv = coerce(right);
        if (rightv == 0) {
            throw Py.ZeroDivisionError("float division");
        }
        return new PyFloat(Math.floor(getValue() / rightv));
    }

    @Override
    public PyObject __rfloordiv__(PyObject left) {
        return float___rfloordiv__(left);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.float___rfloordiv___doc)
    final PyObject float___rfloordiv__(PyObject left) {
        if (!canCoerce(left)) {
            return null;
        }
        double leftv = coerce(left);
        if (getValue() == 0) {
            throw Py.ZeroDivisionError("float division");
        }
        return new PyFloat(Math.floor(leftv / getValue()));
    }

    @Override
    public PyObject __truediv__(PyObject right) {
        return float___truediv__(right);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.float___truediv___doc)
    final PyObject float___truediv__(PyObject right) {
        if (!canCoerce(right)) {
            return null;
        }
        double rightv = coerce(right);
        if (rightv == 0) {
            throw Py.ZeroDivisionError("float division");
        }
        return new PyFloat(getValue() / rightv);
    }

    @Override
    public PyObject __rtruediv__(PyObject left) {
        return float___rtruediv__(left);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.float___rtruediv___doc)
    final PyObject float___rtruediv__(PyObject left) {
        if (!canCoerce(left)) {
            return null;
        }
        double leftv = coerce(left);
        if (getValue() == 0) {
            throw Py.ZeroDivisionError("float division");
        }
        return new PyFloat(leftv / getValue());
    }

    private static double modulo(double x, double y) {
        if (y == 0) {
            throw Py.ZeroDivisionError("float modulo");
        }
        double z = Math.IEEEremainder(x, y);
        if (z * y < 0) {
            z += y;
        }
        return z;
    }

    @Override
    public PyObject __mod__(PyObject right) {
        return float___mod__(right);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.float___mod___doc)
    final PyObject float___mod__(PyObject right) {
        if (!canCoerce(right)) {
            return null;
        }
        double rightv = coerce(right);
        return new PyFloat(modulo(getValue(),rightv));
    }

    @Override
    public PyObject __rmod__(PyObject left) {
        return float___rmod__(left);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.float___rmod___doc)
    final PyObject float___rmod__(PyObject left) {
        if (!canCoerce(left)) {
            return null;
        }
        double leftv = coerce(left);
        return new PyFloat(modulo(leftv, getValue()));
    }

    @Override
    public PyObject __divmod__(PyObject right) {
        return float___divmod__(right);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.float___divmod___doc)
    final PyObject float___divmod__(PyObject right) {
        if (!canCoerce(right)) {
            return null;
        }
        double rightv = coerce(right);

        if (rightv == 0) {
            throw Py.ZeroDivisionError("float division");
        }
        double z = Math.floor(getValue() / rightv);

        return new PyTuple(new PyFloat(z), new PyFloat(getValue() - z * rightv));
    }

    @Override
    public PyObject __rdivmod__(PyObject left) {
        if (!canCoerce(left)) {
            return null;
        }
        double leftv = coerce(left);

        if (getValue() == 0) {
            throw Py.ZeroDivisionError("float division");
        }
        double z = Math.floor(leftv / getValue());

        return new PyTuple(new PyFloat(z), new PyFloat(leftv - z * getValue()));
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.float___rdivmod___doc)
    final PyObject float___rdivmod__(PyObject left) {
        return __rdivmod__(left);
    }

    @Override
    public PyObject __pow__(PyObject right, PyObject modulo) {
        return float___pow__(right, modulo);
    }

    @ExposedMethod(type = MethodType.BINARY, defaults = "null",
                   doc = BuiltinDocs.float___pow___doc)
    final PyObject float___pow__(PyObject right, PyObject modulo) {
        if (!canCoerce(right)) {
            return null;
        }

        if (modulo != null) {
            throw Py.TypeError("pow() 3rd argument not allowed unless all arguments are integers");
        }

        return _pow( getValue(), coerce(right), modulo);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.float___rpow___doc)
    final PyObject float___rpow__(PyObject left) {
        return __rpow__(left);
    }

    @Override
    public PyObject __rpow__(PyObject left) {
        if (!canCoerce(left)) {
            return null;
        }

        return _pow(coerce(left), getValue(), null);
    }

    private static PyFloat _pow(double value, double iw, PyObject modulo) {
        // Rely completely on Java's pow function
        if (iw == 0) {
            if (modulo != null) {
                return new PyFloat(modulo(1.0, coerce(modulo)));
            }
            return new PyFloat(1.0);
        }
        if (value == 0.0) {
            if (iw < 0.0) {
                throw Py.ZeroDivisionError("0.0 cannot be raised to a negative power");
            }
            return new PyFloat(0);
        }

        if (value < 0 && iw != Math.floor(iw)) {
            throw Py.ValueError("negative number cannot be raised to a fractional power");
        }

        double ret = Math.pow(value, iw);
        return new PyFloat(modulo == null ? ret : modulo(ret, coerce(modulo)));
    }

    @Override
    public PyObject __neg__() {
        return float___neg__();
    }

    @ExposedMethod(doc = BuiltinDocs.float___neg___doc)
    final PyObject float___neg__() {
        return new PyFloat(-getValue());
    }

    @Override
    public PyObject __pos__() {
        return float___pos__();
    }

    @ExposedMethod(doc = BuiltinDocs.float___pos___doc)
    final PyObject float___pos__() {
        return float___float__();
    }

    @Override
    public PyObject __invert__() {
        throw Py.TypeError("bad operand type for unary ~");
    }

    @Override
    public PyObject __abs__() {
        return float___abs__();
    }

    @ExposedMethod(doc = BuiltinDocs.float___abs___doc)
    final PyObject float___abs__() {
        if (getValue() < 0) {
            return float___neg__();
        }
        return float___float__();
    }

    @Override
    public PyObject __int__() {
        return float___int__();
    }

    @ExposedMethod(doc = BuiltinDocs.float___int___doc)
    final PyObject float___int__() {
        if (getValue() <= Integer.MAX_VALUE && getValue() >= Integer.MIN_VALUE) {
            return new PyInteger((int) getValue());
        }
        return __long__();
    }

    @Override
    public PyObject __long__() {
        return float___long__();
    }

    @ExposedMethod(doc = BuiltinDocs.float___long___doc)
    final PyObject float___long__() {
        return new PyLong(getValue());
    }

    @Override
    public PyFloat __float__() {
        return float___float__();
    }

    @ExposedMethod(doc = BuiltinDocs.float___float___doc)
    final PyFloat float___float__() {
        return getType() == TYPE ? this : Py.newFloat(getValue());
    }

    @Override
    public PyComplex __complex__() {
        return new PyComplex(getValue(), 0.);
    }

    @ExposedMethod(doc = BuiltinDocs.float___getnewargs___doc)
    final PyTuple float___getnewargs__() {
        return new PyTuple(new PyObject[] {new PyFloat(getValue())});
    }

    @Override
    public PyTuple __getnewargs__() {
        return float___getnewargs__();
    }

    @Override
    public double asDouble() {
        return getValue();
    }

    @Override
    public boolean isNumberType() {
        return true;
    }

    // standard singleton issues apply here to __getformat__/__setformat__,
    // but this is what Python demands
    public enum Format {

        UNKNOWN("unknown"),
        BE("IEEE, big-endian"),
        LE("IEEE, little-endian");

        private final String format;

        Format(String format) {
            this.format = format;
        }

        public String format() {
            return format;
        }
    }
    // subset of IEEE-754, the JVM is big-endian
    public static volatile Format double_format = Format.BE;
    public static volatile Format float_format = Format.BE;

    @ExposedClassMethod(doc = BuiltinDocs.float___getformat___doc)
    public static String float___getformat__(PyType type, String typestr) {
        if ("double".equals(typestr)) {
            return double_format.format();
        } else if ("float".equals(typestr)) {
            return float_format.format();
        } else {
            throw Py.ValueError("__getformat__() argument 1 must be 'double' or 'float'");
        }
    }

    @ExposedClassMethod(doc = BuiltinDocs.float___setformat___doc)
    public static void float___setformat__(PyType type, String typestr, String format) {
        Format new_format = null;
        if (!"double".equals(typestr) && !"float".equals(typestr)) {
            throw Py.ValueError("__setformat__() argument 1 must be 'double' or 'float'");
        }
        if (Format.LE.format().equals(format)) {
            throw Py.ValueError(String.format("can only set %s format to 'unknown' or the "
                                              + "detected platform value", typestr));
        } else if (Format.BE.format().equals(format)) {
            new_format = Format.BE;
        } else if (Format.UNKNOWN.format().equals(format)) {
            new_format = Format.UNKNOWN;
        } else {
            throw Py.ValueError("__setformat__() argument 2 must be 'unknown', " +
                                "'IEEE, little-endian' or 'IEEE, big-endian'");
        }
        if (new_format != null) {
            if ("double".equals(typestr)) {
                double_format = new_format;
            } else {
                float_format = new_format;
            }
        }
    }
}
