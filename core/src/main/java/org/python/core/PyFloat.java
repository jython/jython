// Copyright (c)2026 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.core;

import static org.python.core.Abstract.returnDeprecation;
import static org.python.core.Abstract.returnTypeError;
import static org.python.core.ClassShorthand.T;
import static org.python.core.PyFloatMethods.toDouble;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.math.BigInteger;

import org.python.core.PyUtil.NoConversion;
import org.python.internal.EmptyException;
import org.python.internal.FloatFormatter;
import org.python.internal.Formats;
import org.python.internal.Formats.FormatError;
import org.python.internal.Formats.FormatOverflow;
import org.python.internal.Formats.FormatSpec;
import org.python.internal.Formats.FormatUnknown;
import org.python.kernel.KernelTypeFlag;
import org.python.kernel.Representation;
import org.python.modules.math;
import org.python.types.Exposed;
import org.python.types.Exposed.PythonMethod;
import org.python.types.Feature;
import org.python.types.TypeSpec;
import org.python.types.WithClass;

/**
 * The implementation of the Python {@code float} type.
 * <p>
 * We only actually need instances of this class as a base for Python
 * subclasses of {@code float}. Actual float values are represented by
 * {@code double} or {@code java.lang.Double} when boxed as an object.
 */
public class PyFloat implements WithClass {

    // TODO Make 'float' adopt Float.class
    /** Only referenced during bootstrap by {@link TypeSystem}. */
    static class Spec {
        /** @return the type specification. */
        static TypeSpec get() {
            return new TypeSystem.BootstrapSpec("float", MethodHandles.lookup(), PyFloat.class)
                    // .binopImpl(PyFloatBinops.class) //
                    .add(Feature.BASETYPE) //
                    .methodImpls(PyFloatMethods.class) //
                    .adopt(Double.class);
        }
    }

    /** The Python type of {@code float} objects. */
    // Bootstrap type so ask the type system to resolve it.
    public static final PyType TYPE = TypeSystem.typeOf(0.0);

    /** Value of this Python {@code float} as a Java primitive. */
    final double value;

    /**
     * Construct from primitive.
     *
     * @param value of the {@code float}
     */
    protected PyFloat(double value) { this.value = value; }

    @Override
    public PyType getType() { return TYPE; }

    // Constructor from Python ----------------------------------------

    /**
     * Create a new instance of Python {@code float}, or of a subclass.
     *
     * @param cls actual Python sub-class being created
     * @param x the value
     * @return object with that type and value
     */
    @Exposed.PythonNewMethod
    public static Object __new__(PyType cls, double x) {
        /*
         * We normally arrive here from PyType.__call__, where this/self is the
         * the type we're asked to construct, and gets passed here as the 'cls'
         * argument.
         */
        if (cls == TYPE) {
            // A basic float can be represented by a java.lang.Double
            return Double.valueOf(x);

        } else {
            /*
             * We need an instance of a Python subclass cls, which means creating an
             * instance of cls's Java representation.
             */
            try {
                // Look up a constructor with the right parameters
                MethodHandle cons = cls.constructor(T, double.class).handle();
                return cons.invokeExact(cls, x);
            } catch (Throwable e) {
                throw PyUtil.cannotConstructInstance(cls, TYPE, e);
            }
        }
    }

    // Special methods -----------------------------------------------

    /*
     * These are exposed to Python (or should be) automatically by name. We
     * make special methods private final, because we can, but the exposer
     * makes them effectively public and non-final to Python.
     */
    @SuppressWarnings("unused")
    private static final String __repr__(Object self) {
        assert TYPE.check(self);
        return formatDouble(doubleValue(self), SPEC_REPR);
    }

    /** Format specification used by repr(). */
    private static final Formats.FormatSpec SPEC_REPR = Formats.fromText(">r");

    private final int __hash__() { return PyFloat.__hash__(value); }

    private static final int __hash__(Double self) {
        /*
         * The hash should be independent of the representation, so we make it
         * agree with Java Double.hashCode.
         */
        return self.hashCode();
    }

    // @formatter:off
    /*
    public Object __tojava__(Class<?> c) {
        if (c == Double.TYPE || c == Number.class || c == Double.class || c == Object.class
                || c == Serializable.class) {
            return Double.valueOf(value);
        } else if (c == Float.TYPE || c == Float.class) {
            return Float.valueOf((float) value);
        }
        return super.__tojava__(c);
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
     * Coercion logic for float. Implemented as a final method to avoid invocation of virtual
     * methods from the exposed coerce.
     * /
    final Object float___coerce_ex__(PyObject other) {
        if (other instanceof PyFloat) {
            return other;
        } else if (other instanceof PyInteger) {
            return new PyFloat((double)((PyInteger)other).getValue());
        } else if (other instanceof PyLong) {
            return new PyFloat(((PyLong)other).doubleValue());
        } else {
            return Py.None;
        }
    }

    private static boolean canCoerce(PyObject other) {
        return other instanceof PyFloat || other instanceof PyInteger || other instanceof PyLong;
    }

    private static double coerce(PyObject other) {
        if (other instanceof PyFloat) {
            return ((PyFloat)other).getValue();
        } else if (other instanceof PyInteger) {
            return ((PyInteger)other).getValue();
        } else if (other instanceof PyLong) {
            return ((PyLong)other).doubleValue();
        } else {
            throw Py.TypeError("xxx");
        }
    }
    */
    // @formatter:on

    /**
     * Ternary special method {@code __pow__}.
     *
     * @param x base
     * @param y exponent
     * @param z modulus
     * @return <i>x<sup>y</sup></i>mod <i>z</i> or <i>x<sup>y</sup></i>
     */
    static Object __pow__(Object x, Object y, Object z) {
        try {
            if (z == null || z == Py.None) {
                return pow(toDouble(x), toDouble(y));
            } else {
                // Note that we also call __pow__ from PyLong.__pow__
                throw PyErr.format(PyExc.TypeError, POW_3RD_ARGUMENT);
            }
        } catch (NoConversion e) {
            return Py.NotImplemented;
        }
    }

    @SuppressWarnings("unused")
    private static Object __rpow__(Object y, Object x) {
        try {
            return pow(toDouble(x), toDouble(y));
        } catch (NoConversion e) {
            return Py.NotImplemented;
        }
    }

    private static final String POW_3RD_ARGUMENT =
            "pow() 3rd argument not allowed " + "unless all arguments are integers";

    /** Smallest value that cannot be represented as an int */
    private static double INT_LONG_BOUNDARY = -(double)Integer.MIN_VALUE; // 2^31

    /*
     * @ExposedMethod(doc = BuiltinDocs.float___int___doc)
     */
    final Object __int__() { return __int__(value); }

    static final Object __int__(Double self) { return __int__(self.doubleValue()); }

    private static final Object __int__(double v) {
        if (v < INT_LONG_BOUNDARY && v > -(INT_LONG_BOUNDARY + 1.0)) {
            // v will fit into an int (when rounded towards zero).
            return (int)v;
        } else {
            return bigIntegerFromDouble(v);
        }
    }

    /*
     * @ExposedMethod(doc = BuiltinDocs.float___float___doc)
     */
    Object __float__() { return value; }

    static final Object __float__(Double self) { return self; }

    // float methods -------------------------------------------------

    // These are exposed to Python (or should be) via annotations.

    /*
     * @ExposedClassMethod(doc = BuiltinDocs.float_fromhex_doc)
     */
    public static Object float_fromhex(PyType type, Object o) {
        // XXX: I'm sure this could be shortened/simplified, but
        // Double.parseDouble() takes
        // non-hex strings and requires the form 0xNUMBERpNUMBER for hex input
        // which
        // causes extra complexity here.

        String message = "invalid hexadecimal floating-point string";
        boolean negative = false;

        // XXX Should declare value as String parameter and coerce
        String value = o.toString().trim().toLowerCase();

        if (value.length() == 0) {
            throw PyErr.format(PyExc.ValueError, message);
        } else if (value.equals("nan") || value.equals("-nan") || value.equals("+nan")) {
            return Double.NaN;
        } else if (value.equals("inf") || value.equals("infinity") || value.equals("+inf")
                || value.equals("+infinity")) {
            return Double.POSITIVE_INFINITY;
        } else if (value.equals("-inf") || value.equals("-infinity")) {
            return Double.NEGATIVE_INFINITY;
        }

        // Strip and record + or -
        if (value.charAt(0) == '-') {
            value = value.substring(1);
            negative = true;
        } else if (value.charAt(0) == '+') { value = value.substring(1); }
        if (value.length() == 0) { throw PyErr.format(PyExc.ValueError, message); }

        // Append 0x if not present.
        if (!value.startsWith("0x") && !value.startsWith("0X")) { value = "0x" + value; }

        // reattach - if needed.
        if (negative) { value = "-" + value; }

        // Append p if not present.
        if (value.indexOf('p') == -1) { value = value + "p0"; }

        try {
            double d = Double.parseDouble(value);
            if (Double.isInfinite(d)) {
                throw PyErr.format(PyExc.OverflowError,
                        "hexadecimal value too large to represent as a float");
            }
            return new PyFloat(d);
        } catch (NumberFormatException n) {
            throw PyErr.format(PyExc.ValueError, message);
        }
    }

    /*
     * @ExposedMethod(doc = BuiltinDocs.float_hex_doc)
     */
    public Object float_hex() { return pyHexString(value); }

    /*
     * @ExposedMethod(doc = BuiltinDocs.float___trunc___doc)
     */
    final Object __trunc__() { return __int__(value); }

    static final Object __trunc__(Double self) { return __int__(self.doubleValue()); }

    /*
     * @ExposedGet(name = "real", doc = BuiltinDocs.float_real_doc)
     */
    public Object getReal() { return value; }

    /*
     * @ExposedGet(name = "imag", doc = BuiltinDocs.float_imag_doc)
     */
    public Object getImag() { return 0.0; }

    /*
     * @ExposedMethod(doc = BuiltinDocs.float_conjugate_doc)
     */
    final Object conjugate() { return value; }

    static final Object conjugate(Double self) { return self; }

    /*
     * @ExposedMethod(doc = BuiltinDocs.float_is_integer_doc)
     */
    final boolean is_integer() { return is_integer(value); }

    static final boolean is_integer(Double self) { return is_integer(self.doubleValue()); }

    static final boolean is_integer(double self) {
        if (!Double.isFinite(self)) { return false; }
        return Math.floor(self) == self;
    }

    /*
     * @ExposedMethod(doc = BuiltinDocs.float___getnewargs___doc)
     */
    final PyTuple __getnewargs__() { return PyTuple.of(value); }

    static final PyTuple __getnewargs__(Double self) { return PyTuple.of(self); }

    // standard singleton issues apply here to __getformat__/__setformat__,
    // but this is what Python demands
    // TODO investigate this comment (and use of Format enum)
    public enum Format {

        UNKNOWN("unknown"), BE("IEEE, big-endian"), LE("IEEE, little-endian");

        private final String format;

        Format(String format) { this.format = format; }

        public String format() { return format; }
    }

    // subset of IEEE-754, the JVM is big-endian
    public static volatile Format double_format = Format.BE;
    public static volatile Format float_format = Format.BE;

    /*
     * @ExposedClassMethod(doc = BuiltinDocs.float___getformat___doc)
     */
    public static String float___getformat__(PyType type, String typestr) {
        if ("double".equals(typestr)) {
            return double_format.format();
        } else if ("float".equals(typestr)) {
            return float_format.format();
        } else {
            throw PyErr.format(PyExc.ValueError,
                    "__getformat__() argument 1 must be 'double' or 'float'");
        }
    }

    /*
     * @ExposedClassMethod(doc = BuiltinDocs.float___setformat___doc)
     */
    public static void float___setformat__(PyType type, String typestr, String format) {
        Format new_format = null;
        if (!"double".equals(typestr) && !"float".equals(typestr)) {
            throw PyErr.format(PyExc.ValueError,
                    "__setformat__() argument 1 must be 'double' or 'float'");
        }
        if (Format.LE.format().equals(format)) {
            throw PyErr.format(PyExc.ValueError, String.format(
                    "can only set %s format to 'unknown' or the " + "detected platform value",
                    typestr));
        } else if (Format.BE.format().equals(format)) {
            new_format = Format.BE;
        } else if (Format.UNKNOWN.format().equals(format)) {
            new_format = Format.UNKNOWN;
        } else {
            throw PyErr.format(PyExc.ValueError, "__setformat__() argument 2 must be 'unknown', "
                    + "'IEEE, little-endian' or 'IEEE, big-endian'");
        }
        if (new_format != null) {
            if ("double".equals(typestr)) {
                double_format = new_format;
            } else {
                float_format = new_format;
            }
        }
    }

    /**
     * Format the {@code self} object.
     *
     * @param self to format
     * @param formatSpec specification
     * @return the formatted string from {@code self}
     */
    @PythonMethod
    static final Object __format__(Object self, Object formatSpec) {

        String stringFormatSpec = PyUnicode.asString(formatSpec,
                o -> Abstract.argumentTypeError("__format__", "specification", "str", o));

        try {
            // Parse the specification
            FormatSpec spec = Formats.fromText(stringFormatSpec);

            // Get a formatter for the specification
            Formatter f = new Formatter(spec);

            /*
             * Format, pad and return a result according to as the specification
             * argument.
             */
            return f.format(self).pad().getResult();

        } catch (FormatOverflow fe) {
            throw PyErr.format(PyExc.OverflowError, fe.getMessage());
        } catch (FormatUnknown fe) {
            throw PyErr.format(PyExc.ValueError, "%s for object of type '%s'", fe.getMessage(),
                    PyType.of(self).getName());
        } catch (FormatError fe) {
            throw PyErr.format(PyExc.ValueError, fe.getMessage());
        } catch (NoConversion e) {
            throw Abstract.impossibleArgumentError("float", self);
        }
    }

    /**
     * Format this float according to the specification passed in. Supports
     * {@code __format__}, {@code __str__} and {@code __repr__}.
     *
     * @param value to format
     * @param spec parsed format specification string
     * @return formatted value
     */
    private static String formatDouble(double value, Formats.FormatSpec spec) {
        try {
            FloatFormatter f = new Formatter(spec, true);
            return f.format(value).getResult();
        } catch (FormatOverflow fe) {
            throw PyErr.format(PyExc.OverflowError, fe.getMessage());
        } catch (FormatError fe) {
            throw PyErr.format(PyExc.ValueError, fe.getMessage());
        }
    }

    /*
     * @ExposedMethod(doc = BuiltinDocs.float_as_integer_ratio_doc)
     */
    final PyTuple as_integer_ratio() { return as_integer_ratio(value); }

    static final PyTuple as_integer_ratio(Double self) {
        return as_integer_ratio(self.doubleValue());
    }

    // Java API -----------------------------------------------------

    /*
     * These methods are not exposed to Python but are used from Java in
     * support of other object implementations. (Public members will be
     * client API as this package is exported.)
     *
     * Note that instance methods do not define behaviour for the Python
     * float type in general, just for the PyFloat representation (so mostly
     * for subclasses).
     */

    @Override
    public String toString() { return PyUtil.defaultToString(this); }

    @Override
    public boolean equals(Object obj) {
        // XXX Use Dict.pythonEquals when available
        if (obj instanceof PyFloat) {
            PyFloat other = (PyFloat)obj;
            return other.value == this.value;
        } else
            // XXX should try more accepted types. Or __eq__?
            return false;
    }

    @Override
    public int hashCode() { return __hash__(); }

    // Representations of the value -----------------------------------

    /*
     * These conversion methods are public API. They convert a Python float,
     * or a Java float-like, to a specified Java type.
     */

    /**
     * Present the value as a Java {@code double} when the argument is
     * expected to be exactly an instance of Python {@code float} or of a
     * consistent sub-class of it.
     *
     * @param v claimed {@code float}
     * @return {@code double} value
     * @throws PyBaseException ({@link PyExc#TypeError TypeError}) if
     *     {@code v} is not a Python {@code float}
     */
    // Compare CPython floatobject.h: PyFloat_AS_DOUBLE
    public static double doubleValue(Object v) throws PyBaseException {
        if (v instanceof Double)
            return ((Double)v).doubleValue();
        else if (v instanceof PyFloat)
            return ((PyFloat)v).value;
        else
            throw Abstract.requiredTypeError("a float", v);
    }

    /**
     * Convert the argument to a Java {@code double} value. If {@code o} is
     * not a Python {@code float} try the {@code __float__()} method, then
     * {@code __index__()}.
     *
     * @param o to convert
     * @return converted value
     * @throws PyBaseException ({@link PyExc#TypeError TypeError}) if o
     *     cannot be interpreted as a {@code float}
     * @throws Throwable from {@code __float__)} or {@code __index__}
     */
    // Compare CPython floatobject.c: PyFloat_AsDouble
    public static double asDouble(Object o) throws PyBaseException, Throwable {
        /*
         * Ever so similar to PyNumber.toFloat, but returns the double value
         * extracted from (potentially) a sub-type of PyFloat, and does not try
         * to convert from strings.
         */
        Representation rep = Abstract.representation(o);

        if (rep.isFloatExact()) { return doubleValue(o); }

        // Try __float__ (if defined)
        try {
            Object flt = rep.op_float().invokeExact(o);
            Representation resRep = Abstract.representation(flt);
            if (!resRep.isFloatExact()) {
                PyType resType = resRep.pythonType(flt);
                if (resType.isSubTypeOf(PyFloat.TYPE))
                    // Warn about this and return value field
                    returnDeprecation("__float__", "float", flt);
                else
                    // Not a float at all.
                    throw returnTypeError("__float__", "float", flt);
            }
            return doubleValue(flt);
        } catch (EmptyException e) {}

        // o.__float__ was not defined try o.__index__
        if (rep.hasFeature(o, KernelTypeFlag.HAS_INDEX))
            return PyLong.asDouble(PyNumber.index(o));
        else
            throw Abstract.requiredTypeError("a real number", o);
    }

    // Plumbing ------------------------------------------------------

    /*
     * These methods are for use internal to the core, including privately
     * here: they may throw non-Python exception like NoConversion on
     * failure, which must be caught by those implementations. Some convert
     * a Python float, or a specific Java float-like, to a specified Java
     * type, or convert from a range of inputs to a Python float.
     */

    /**
     * Convert a Python {@code float}, {@code int} or {@code bool} to a Java
     * {@code double} (or throw {@link NoConversion}). Conversion from an
     * {@code int} may overflow.
     * <p>
     * If the method throws the special non-Python exception
     * {@link NoConversion}, the caller must deal with it by throwing an
     * appropriate Python exception or taking an alternative course of
     * action. OverlowError could be allowed to propagate since it is a
     * Python exception.
     *
     * @param v to convert
     * @return converted to {@code double}
     * @throws NoConversion if v is not a {@code float}, {@code int} or
     *     {@code bool}
     * @throws PyBaseException ({@link PyExc#OverflowError OverflowError})
     *     if v is an {@code int} out of range
     */
    static double convertToDouble(Object v) throws NoConversion, PyBaseException {
        if (v instanceof Double)
            return ((Double)v).doubleValue();
        else if (v instanceof PyUnicode)
            return ((PyFloat)v).value;
        else
            // BigInteger, PyLong, Boolean, etc. or throw
            return PyLong.convertToDouble(v);
    }

    /**
     * Convert a string-like object to a Python {@code float}.
     *
     * @param v to convert
     * @return converted value
     */
    // Compare CPython floatobject.c :: PyFloat_FromString
    static Double fromString(Object v) {
        // Keep it simple (a lot simpler than in CPython)
        return Double.valueOf(v.toString());
    }

    /**
     * Convert a Java {@code double} to Java {@code BigInteger} by
     * truncation.
     *
     * @param value to convert
     * @return BigInteger equivalent.
     * @throws PyBaseException ({@link PyExc#OverflowError OverflowError})
     *     when this is a floating infinity
     * @throws PyBaseException ({@link PyExc#ValueError ValueError}) when
     *     this is a floating NaN
     */
    // Somewhat like CPython longobject.c :: PyLong_FromDouble
    static BigInteger bigIntegerFromDouble(double value) throws PyBaseException {

        long raw = Double.doubleToRawLongBits(value);
        long e = (raw & EXPONENT) >>> SIGNIFICAND_BITS;
        int exponent = ((int)e) - EXPONENT_BIAS;

        if (exponent < 63)
            // Give the job to the hardware.
            return BigInteger.valueOf((long)value);

        else if (exponent > 1023) {
            // raw exponent was 0x7ff
            if ((raw & SIGNIFICAND) == 0)
                throw cannotConvertInf("integer");
            else
                throw cannotConvertNaN("integer");

        } else {
            // Get the signed version of the significand
            long significand = IMPLIED_ONE | raw & SIGNIFICAND;
            long v = (raw & SIGN) == 0L ? significand : -significand;
            // Shift (left or right) according to the exponent
            return BigInteger.valueOf(v).shiftLeft(exponent - SIGNIFICAND_BITS);
        }
    }

    // IEE-754 64-bit floating point parameters
    private static final int SIGNIFICAND_BITS = 52; // exc. implied 1
    private static final int EXPONENT_BITS = 11;
    private static final int EXPONENT_BIAS = 1023;

    // Masks derived from the 64-bit floating point parameters
    private static final long IMPLIED_ONE = 1L << SIGNIFICAND_BITS; // = 0x0010000000000000L
    private static final long SIGNIFICAND = IMPLIED_ONE - 1;        // = 0x000fffffffffffffL
    private static final long SIGN = IMPLIED_ONE << EXPONENT_BITS;  // = 0x8000000000000000L;
    private static final long EXPONENT = SIGN - IMPLIED_ONE;        // = 0x7ff0000000000000L;

    private static PyBaseException cannotConvertInf(String to) {
        String msg = String.format(CANNOT_CONVERT, "infinity", to);
        return PyErr.format(PyExc.OverflowError, msg);
    }

    private static PyBaseException cannotConvertNaN(String to) {
        String msg = String.format(CANNOT_CONVERT, "NaN", to);
        return PyErr.format(PyExc.ValueError, msg);
    }

    private static final String CANNOT_CONVERT = "cannot convert float %s to %s";

    /**
     * Exponentiation with Python semantics.
     *
     * @param v base value
     * @param w exponent
     * @return {@code v ** w}
     */
    static double pow(double v, double w) {
        /*
         * This code was translated from the CPython implementation at v2.7.8 by
         * progressively removing cases that could be delegated to Java. Jython
         * differs from CPython in that where C pow() overflows, Java pow()
         * returns inf (observed on Windows). This is not subject to regression
         * tests, so we take it as an allowable platform dependency. All other
         * differences in Java Math.pow() are trapped below and Python behaviour
         * is enforced.
         */
        if (w == 0) {
            // v**0 is 1, even 0**0 and nan**0
            return 1.0;

        } else if (Double.isNaN(v)) {
            // nan**w = nan, unless w == 0
            return Double.NaN;

        } else if (Double.isNaN(w)) {
            // v**nan = nan, unless v == 1; 1**nan = 1
            return v == 1.0 ? v : Double.NaN;

        } else if (Double.isInfinite(w)) {
            /*
             * In Java Math pow(1,inf) = pow(-1,inf) = pow(1,-inf) = pow(-1,-inf) =
             * nan, but in Python they are all 1.
             */
            if (v == 1.0 || v == -1.0) { return 1.0; }

        } else if (v == 0.0) {
            // 0**w is an error if w is negative.
            if (w < 0.0) {
                throw PyErr.format(PyExc.ZeroDivisionError,
                        "0.0 cannot be raised to a negative power");
            }

        } else if (!Double.isInfinite(v) && v < 0.0) {
            if (w != Math.floor(w)) {
                throw PyErr.format(PyExc.ValueError,
                        "negative number cannot be raised to a fractional power");
            }
        }

        // In all other cases we can entrust the calculation to Java.
        return Math.pow(v, w);
    }

    /** Used as error message text for division by zero. */
    static final String DIV_ZERO = "float division by zero";
    /** Used as error message text for modulo zero. */
    static final String MOD_ZERO = "float modulo zero";

    /**
     * Convenience function to raise a {@link PyExc#ZeroDivisionError
     * ZeroDivisionError} if the argument is zero. (Java float arithmetic
     * does not throw whatever the arguments.)
     *
     * @param v value to check is not zero
     * @param msg for exception if {@code v==0.0}
     * @return {@code v}
     */
    static double nonzero(double v, String msg) {
        if (v == 0.0) { throw PyErr.format(PyExc.ZeroDivisionError, msg); }
        return v;
    }

    /**
     * Convenience function to raise a {@link PyExc#ZeroDivisionError
     * ZeroDivisionError} if the argument is zero. (Java float arithmetic
     * does not throw whatever the arguments.)
     *
     * @param v value to check is not zero
     * @return {@code v}
     */
    static double nonzero(double v) {
        if (v == 0.0) { throw PyErr.format(PyExc.ZeroDivisionError, DIV_ZERO); }
        return v;
    }

    /**
     * Test that two {@code double}s have the same sign.
     *
     * @param u a double
     * @param v another double
     * @return if signs equal (works for signed zeros, etc.)
     */
    private static boolean sameSign(double u, double v) {
        long uBits = Double.doubleToRawLongBits(u);
        long vBits = Double.doubleToRawLongBits(v);
        return ((uBits ^ vBits) & SIGN) == 0L;
    }

    /**
     * Inner method for {@code __floordiv__} and {@code __rfloordiv__}.
     *
     * @param x operand
     * @param y operand
     * @return {@code x//y}
     */
    static final double floordiv(double x, double y) {
        // Java and Python agree a lot of the time (after floor()).
        // Also, Java / never throws: it just returns nan or inf.
        // So we ask Java first, then adjust the answer.
        double z = x / y;
        if (Double.isFinite(z)) {
            // Finite result: only need floor ...
            if (Double.isInfinite(y) && x != 0.0 && !sameSign(x, y))
                // ... except in this messy corner case :(
                return -1.;
            return Math.floor(z);
        } else {
            // Non-finite result: Java & Python differ
            nonzero(y);
            return Double.NaN;
        }
    }

    /**
     * Inner method for {@code __mod__} and {@code __rmod__}.
     *
     * @param x operand
     * @param y operand
     * @return {@code x%y}
     */
    static final double mod(double x, double y) {
        // Java and Python agree a lot of the time.
        // Also, Java % never throws: it just returns nan.
        // So we ask Java first, then adjust the answer.
        double z = x % y;
        if (Double.isNaN(z)) {
            nonzero(y, MOD_ZERO);   // Otherwise nan is fine
        } else if (!sameSign(z, y)) {
            // z is finite (and x), but only correct if signs match
            if (z == 0.) {
                z = Math.copySign(z, y);
            } else {
                z = z + y;
            }
        }
        return z;
    }

    /**
     * Inner method for {@code __divmod__} and {@code __rdivmod__}.
     *
     * @param x operand
     * @param y operand
     * @return {@code tuple} of {@code (x//y, x%y)}
     */
    static final PyTuple divmod(double x, double y) {
        // Possibly not the most efficient
        return PyTuple.of(floordiv(x, y), mod(x, y));
    }

    private static PyTuple as_integer_ratio(double value) {
        if (Double.isInfinite(value)) { throw cannotConvertInf("integer ratio"); }
        if (Double.isNaN(value)) { throw cannotConvertNaN("integer ratio"); }
        // XXX This is potty: use similar logic to bigIntegerFromDouble.
        // Long.numberOfTrailingZeros​ on significand adjusts the exponent.
        PyTuple frexp = math.frexp(value);
        double float_part = ((Double)frexp.get(0)).doubleValue();
        int exponent = ((Integer)frexp.get(1)).intValue();
        for (int i = 0; i < 300 && float_part != Math.floor(float_part); i++) {
            float_part *= 2.0;
            exponent--;
        }
        /*
         * CPython comment (not relevant after first sentence): self ==
         * float_part * 2**exponent exactly and float_part is integral. If
         * FLT_RADIX != 2, the 300 steps may leave a tiny fractional part to be
         * truncated by PyLong_FromDouble().
         */

        // value == m * 2**exponent exactly and m is integral.
        BigInteger numerator = bigIntegerFromDouble(float_part);
        Object denominator = 1;

        // Factor to apply to numerator or denominator

        if (exponent >= 0) {
            // Scale the numerator by 2**exponent
            numerator = numerator.shiftLeft(exponent);
        } else {
            // exponent<0: make the denominator 2**-exponent
            denominator = BigInteger.ONE.shiftLeft(-exponent);
        }

        return PyTuple.of(numerator, denominator);
    }

    /**
     * Python % operator: y = n*x + z. The modulo operator always yields a
     * result with the same sign as its second operand (or zero). (Compare
     * <code>java.Math.IEEEremainder</code>)
     *
     * @param x dividend
     * @param y divisor
     * @return <code>x % y</code>
     */
    private static double modulo(double x, double y) {
        // FIXME: Dead code?
        if (y == 0.0) {
            throw PyErr.format(PyExc.ZeroDivisionError, "float modulo");
        } else {
            double z = x % y;
            if (z == 0.0) {
                // Has to be same sign as y (even when zero).
                return Math.copySign(z, y);
            } else if ((z > 0.0) == (y > 0.0)) {
                // z has same sign as y, as it must.
                return z;
            } else {
                // Note abs(z) < abs(y) and opposite sign.
                return z + y;
            }
        }
    }

    private String pyHexString(Double f) {
        // Simply rewrite Java hex repr to expected Python values; not
        // the most efficient, but we don't expect this to be a hot
        // spot in our code either
        String java_hex = Double.toHexString(value);
        if (java_hex.equals("Infinity")) {
            return "inf";
        } else if (java_hex.equals("-Infinity")) {
            return "-inf";
        } else if (java_hex.equals("NaN")) {
            return "nan";
        } else if (java_hex.equals("0x0.0p0")) {
            return "0x0.0p+0";
        } else if (java_hex.equals("-0x0.0p0")) { return "-0x0.0p+0"; }

        // replace hex rep of MpE to conform with Python such that
        // 1. M is padded to 16 digits (ignoring a leading -)
        // 2. Mp+E if E>=0
        // example: result of 42.0.hex() is translated from
        // 0x1.5p5 to 0x1.5000000000000p+5
        int len = java_hex.length();
        boolean start_exponent = false;
        StringBuilder py_hex = new StringBuilder(len + 1);
        int padding = f > 0 ? 17 : 18;
        for (int i = 0; i < len; i++) {
            char c = java_hex.charAt(i);
            if (c == 'p') {
                for (int pad = i; pad < padding; pad++) { py_hex.append('0'); }
                start_exponent = true;
            } else if (start_exponent) {
                if (c != '-') { py_hex.append('+'); }
                start_exponent = false;
            }
            py_hex.append(c);
        }
        return py_hex.toString();
    }

    // formatter ------------------------------------------------------

    /**
     * A {@link Formatter}, constructed from a {@link FormatSpec}, with
     * specific validations for {@code int.__format__}.
     */
    static class Formatter extends FloatFormatter {

        /**
         * If {@code true}, give old-style %-formatting meanings to
         * {@link FormatSpec#type}.
         */
        final boolean oldfmt;

        /**
         * Prepare a {@link Formatter} in support of {@code str.__mod__}, that
         * is, old-style %-formatting.
         *
         * @param spec a parsed format specification.
         * @param oldfmt f {@code true}, interpret {@code spec} old-style
         *     %-formatting, otherwise as {@link Formatter#Formatter(FormatSpec)
         *     Formatter(FormatSpec)}
         * @throws FormatOverflow if a value is out of range (including the
         *     precision)
         * @throws FormatError if an unsupported format character is encountered
         */
        Formatter(FormatSpec spec, boolean oldfmt) throws FormatError {
            super(validated(spec, oldfmt));
            this.oldfmt = oldfmt;
        }

        /**
         * Prepare a {@link Formatter} in support of
         * {@link PyFloat#__format__(Object, Object) float.__format__}.
         *
         * @param spec a parsed PEP-3101 format specification.
         * @throws FormatError if an unsupported format is encountered
         */
        Formatter(FormatSpec spec) throws FormatError { this(spec, false); }

        /**
         * Validations and defaults specific to {@code float.__format__}. (Note
         * that %-formatting has slightly different rules.)
         *
         * @param spec to validate
         * @return validated spec with defaults filled
         * @throws FormatError on failure to validate
         */
        private static FormatSpec validated(FormatSpec spec, boolean oldfmt) throws FormatError {

            switch (spec.type) {

                case 'n':
                    if (spec.group > 0) {
                        throw notAllowed("Grouping", spec.grouper, TYPE.getName(), spec.type);
                    }
                    //$FALL-THROUGH$

                case FormatSpec.NONE:
                case 'e':
                case 'f':
                case 'g':
                case 'E':
                case 'F':
                case 'G':
                case '%':
                    // TODO Alternate forms allowed in Python 3
                    // Check for disallowed parts of the specification
                    if (spec.alternate) { throw alternateFormNotAllowed(TYPE.getName()); }
                    break;

                case 'r':
                case 's':
                    // Only allow for old-style %-formatting
                    if (oldfmt) { break; }
                    //$FALL-THROUGH$

                default:
                    // The type code was not recognised
                    throw new FormatUnknown(spec.type);
            }

            /*
             * spec may be incomplete. The defaults are those commonly used for
             * numeric formats.
             */
            return spec.withDefaults(FormatSpec.NUMERIC);
        }

        @Override
        public FloatFormatter format(Object o) throws NoConversion, FormatError {
            return format(convertToDouble(o));
        }
    }
}
