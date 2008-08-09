// Copyright (c) Corporation for National Research Initiatives
package org.python.core;

import java.io.Serializable;
import java.math.BigDecimal;

import org.python.expose.ExposedClassMethod;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedType;
import org.python.expose.MethodType;

/**
 * A builtin python float.
 */
@ExposedType(name = "float")
public class PyFloat extends PyObject
{
    /** Precisions used by repr() and str(), respectively. */
    private static final int PREC_REPR = 17;
    private static final int PREC_STR = 12;

    @ExposedNew
    public static PyObject float_new(PyNewWrapper new_, boolean init, PyType subtype,
            PyObject[] args, String[] keywords) {
        ArgParser ap = new ArgParser("float", args, keywords, new String[] { "x" }, 0);
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
                if (Py.matchException(e, Py.AttributeError)) {
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

    public static final PyType TYPE = PyType.fromClass(PyFloat.class);

    private double value;

    public PyFloat(PyType subtype, double v) {
        super(subtype);
        value = v;
    }
    
    public PyFloat(double v) {
        this(TYPE, v);
    }

    public PyFloat(float v) {
        this((double)v);
    }

    /**
     * Determine if this float is not infinity, nor NaN.
     */
    public boolean isFinite() {
        return !Double.isInfinite(value) && !Double.isNaN(value);
    }

    public double getValue() {
        return value;
    }

    public String toString() {
        return __str__().toString();
    }

    public PyString __str__() {
        return float___str__();
    }

    @ExposedMethod
    final PyString float___str__() {
        return Py.newString(formatDouble(PREC_STR));
    }

    public PyString __repr__() {
        return float___repr__();
    }

    @ExposedMethod
    final PyString float___repr__() {
        return Py.newString(formatDouble(PREC_REPR));
    }

    private String formatDouble(int precision) {
        if (Double.isNaN(value)) {
            return "nan";
        }
        
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

    public int hashCode() {
        return float___hash__();
    }

    @ExposedMethod
    final int float___hash__() {
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
        return float___nonzero__();
    }

    @ExposedMethod
    final boolean float___nonzero__() {
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
        return float___cmp__(other);
    }

    @ExposedMethod(type = MethodType.CMP)
    final int float___cmp__(PyObject other) {
        double i = value;
        double j;

        if (other instanceof PyFloat) {
            j = ((PyFloat)other).value;
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
            j = ((PyInteger)other).getValue();
        } else if (other instanceof PyLong) {
            BigDecimal v = new BigDecimal(value);
            BigDecimal w = new BigDecimal(((PyLong)other).getValue());
            return v.compareTo(w);
        } else {
            return -2;
        }
        return i < j ? -1 : i > j ? 1 : 0;
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
        return other instanceof PyFloat || other instanceof PyInteger || other instanceof PyLong;
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
        return float___add__(right);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject float___add__(PyObject right) {
        if (!canCoerce(right))
            return null;
        double rightv = coerce(right);
        return new PyFloat(value + rightv);
    }

    public PyObject __radd__(PyObject left) {
        return float___radd__(left);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject float___radd__(PyObject left) {
        return __add__(left);
    }

    public PyObject __sub__(PyObject right) {
        return float___sub__(right);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject float___sub__(PyObject right) {
        if (!canCoerce(right))
            return null;
        double rightv = coerce(right);
        return new PyFloat(value - rightv);
    }

    public PyObject __rsub__(PyObject left) {
        return float___rsub__(left);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject float___rsub__(PyObject left) {
        if (!canCoerce(left))
            return null;
        double leftv = coerce(left);
        return new PyFloat(leftv - value);
    }

    public PyObject __mul__(PyObject right) {
        return float___mul__(right);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject float___mul__(PyObject right) {
        if (!canCoerce(right))
            return null;
        double rightv = coerce(right);
        return new PyFloat(value * rightv);
    }

    public PyObject __rmul__(PyObject left) {
        return float___rmul__(left);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject float___rmul__(PyObject left) {
        return __mul__(left);
    }

    public PyObject __div__(PyObject right) {
        return float___div__(right);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject float___div__(PyObject right) {
        if (!canCoerce(right))
            return null;
        if (Options.divisionWarning >= 2)
            Py.warning(Py.DeprecationWarning, "classic float division");
        double rightv = coerce(right);
        if (rightv == 0)
            throw Py.ZeroDivisionError("float division");
        return new PyFloat(value / rightv);
    }

    public PyObject __rdiv__(PyObject left) {
        return float___rdiv__(left);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject float___rdiv__(PyObject left) {
        if (!canCoerce(left))
            return null;
        if (Options.divisionWarning >= 2)
            Py.warning(Py.DeprecationWarning, "classic float division");
        double leftv = coerce(left);
        if (value == 0)
            throw Py.ZeroDivisionError("float division");
        return new PyFloat(leftv / value);
    }

    public PyObject __floordiv__(PyObject right) {
        return float___floordiv__(right);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject float___floordiv__(PyObject right) {
        if (!canCoerce(right))
            return null;
        double rightv = coerce(right);
        if (rightv == 0)
            throw Py.ZeroDivisionError("float division");
        return new PyFloat(Math.floor(value / rightv));
    }

    public PyObject __rfloordiv__(PyObject left) {
        return float___rfloordiv__(left);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject float___rfloordiv__(PyObject left) {
        if (!canCoerce(left))
            return null;
        double leftv = coerce(left);
        if (value == 0)
            throw Py.ZeroDivisionError("float division");
        return new PyFloat(Math.floor(leftv / value));
    }

    public PyObject __truediv__(PyObject right) {
        return float___truediv__(right);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject float___truediv__(PyObject right) {
        if (!canCoerce(right))
            return null;
        double rightv = coerce(right);
        if (rightv == 0)
            throw Py.ZeroDivisionError("float division");
        return new PyFloat(value / rightv);
    }

    public PyObject __rtruediv__(PyObject left) {
        return float___rtruediv__(left);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject float___rtruediv__(PyObject left) {
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
        return float___mod__(right);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject float___mod__(PyObject right) {
        if (!canCoerce(right))
            return null;
        double rightv = coerce(right);
        return new PyFloat(modulo(value, rightv));
    }

    public PyObject __rmod__(PyObject left) {
        return float___rmod__(left);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject float___rmod__(PyObject left) {
        if (!canCoerce(left))
            return null;
        double leftv = coerce(left);
        return new PyFloat(modulo(leftv, value));
    }

    public PyObject __divmod__(PyObject right) {
        return float___divmod__(right);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject float___divmod__(PyObject right) {
        if (!canCoerce(right))
            return null;
        double rightv = coerce(right);

        if (rightv == 0)
            throw Py.ZeroDivisionError("float division");
        double z = Math.floor(value / rightv);

        return new PyTuple(new PyFloat(z), new PyFloat(value - z * rightv));
    }

    public PyObject __rdivmod__(PyObject left) {
        if (!canCoerce(left))
            return null;
        double leftv = coerce(left);

        if (value == 0)
            throw Py.ZeroDivisionError("float division");
        double z = Math.floor(leftv / value);

        return new PyTuple(new PyFloat(z), new PyFloat(leftv - z * value));
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject float___rdivmod__(PyObject left) {
    	return __rdivmod__(left);
    }


    public PyObject __pow__(PyObject right, PyObject modulo) {
        return float___pow__(right, modulo);
    }

    @ExposedMethod(defaults = "null")
    final PyObject float___pow__(PyObject right, PyObject modulo) {
        if (!canCoerce(right))
            return null;

        if (modulo != null) {
            throw Py.TypeError("pow() 3rd argument not allowed " +
                               "unless all arguments are integers");
        }

        return _pow(value, coerce(right), modulo);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject float___rpow__(PyObject left) {
    	return __rpow__(left);
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

        if (value < 0 && iw != Math.floor(iw))
            throw Py.ValueError("negative number cannot be raised to a fractional power");
            
        double ret = Math.pow(value, iw);
        if (modulo == null) {
            return new PyFloat(ret);
        } else {
            return new PyFloat(modulo(ret, coerce(modulo)));
        }
    }

    @ExposedMethod
    final PyObject float___coerce__(PyObject other) {
        return __coerce__(other);
    }

    public PyObject __neg__() {
        return float___neg__();
    }

    @ExposedMethod
    final PyObject float___neg__() {
        return new PyFloat(-value);
    }

    public PyObject __pos__() {
        return float___pos__();
    }

    @ExposedMethod
    final PyObject float___pos__() {
        return Py.newFloat(value);
    }

    public PyObject __invert__() {
        throw Py.TypeError("bad operand type for unary ~");
    }

    public PyObject __abs__() {
        return float___abs__();
    }

    @ExposedMethod
    final PyObject float___abs__() {
        if (value >= 0)
            return Py.newFloat(value);
        else
            return __neg__();
    }

    public PyObject __int__() {
        return float___int__();
    }

    @ExposedMethod
    final PyObject float___int__() {
        if (value <= Integer.MAX_VALUE && value >= Integer.MIN_VALUE) {
            return new PyInteger((int)value);
        }
        return __long__();
    }

    public PyObject __long__() {
        return float___long__();
    }

    @ExposedMethod
    final PyObject float___long__() {
        return new PyLong(value);
    }

    public PyFloat __float__() {
        return float___float__();
    }

    @ExposedMethod
    final PyFloat float___float__() {
        return Py.newFloat(value);
    }
    public PyComplex __complex__() {
        return new PyComplex(value, 0.);
    }

    @ExposedMethod
    final PyTuple float___getnewargs__() {
        return new PyTuple(new PyObject[] {new PyFloat(getValue())});
    }

    public PyTuple __getnewargs__() {
        return float___getnewargs__();
    }

    // standard singleton issues apply here to __getformat__/__setformat__,
    // but this is what Python demands
    
    public enum Format {
        UNKNOWN ("unknown"),
        BE ("IEEE, big-endian"),
        LE ("IEEE, little-endian");
        
        private final String format;
        Format(String format) {
            this.format = format;
        }
        public String format() { return format; } 
    }
    
    // subset of IEEE-754, the JVM is big-endian 
    public static volatile Format double_format = Format.BE;
    public static volatile Format float_format = Format.BE;
    
    @ExposedClassMethod
    public static String float___getformat__(PyType type, String typestr) {
        if ("double".equals(typestr)) {
            return double_format.format();
        } else if ("float".equals(typestr)) {
            return float_format.format();
        } else {
            throw Py.ValueError("__getformat__() argument 1 must be 'double' or 'float'");
        }
    }
    
    @ExposedClassMethod
    public static void float___setformat__(PyType type, String typestr, String format) {
        Format new_format = null;
        if (!"double".equals(typestr) && !"float".equals(typestr)) {
            throw Py.ValueError("__setformat__() argument 1 must be 'double' or 'float'");
        }
        if (Format.LE.format().equals(format)) {
            throw Py.ValueError(String.format("can only set %s format to 'unknown' or the " + "detected platform value", typestr));
        } else if (Format.BE.format().equals(format)) {
            new_format = Format.BE;
        } else if (Format.UNKNOWN.format().equals(format)) {
            new_format = Format.UNKNOWN;
        } else {
            throw Py.ValueError("__setformat__() argument 2 must be 'unknown', " + "'IEEE, little-endian' or 'IEEE, big-endian'");
        }
        if (new_format != null) {
            if ("double".equals(typestr)) {
                double_format = new_format;
            } else {
                float_format = new_format;
            }
        }
    }

    public boolean isMappingType() { return false; }
    public boolean isSequenceType() { return false; }

}
