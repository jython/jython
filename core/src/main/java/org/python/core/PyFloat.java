// Copyright (c)2021 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.core;

import static org.python.core.PyFloatMethods.toDouble;

import java.lang.invoke.MethodHandles;
import java.math.BigInteger;
import java.util.Map;

import org.python.base.InterpreterError;
import org.python.core.PyObjectUtil.NoConversion;
import org.python.core.stringlib.FloatFormatter;
import org.python.core.stringlib.InternalFormat;
import org.python.core.stringlib.InternalFormat.FormatError;
import org.python.core.stringlib.InternalFormat.FormatOverflow;
import org.python.core.stringlib.InternalFormat.Spec;
import org.python.modules.math;

/** The Python {@code float} object. */
public class PyFloat extends AbstractPyObject {
    /** The type {@code float}. */
    public static final PyType TYPE = PyType.fromSpec( //
            new PyType.Spec("float", MethodHandles.lookup())
                    .adopt(Double.class)
                    .operand(Integer.class, BigInteger.class,
                            PyLong.class, Boolean.class)
                    .methods(PyFloatMethods.class));

    /** Format specification used by repr(). */
    static final Spec SPEC_REPR = InternalFormat.fromText(" >r");
    /** Format specification used by str(). */
    static final Spec SPEC_STR = Spec.NUMERIC;

    /** A constant Python {@code float(0)}. */
    static final Double ZERO = Double.valueOf(0.0);

    /** A constant Python {@code float(1)}. */
    static final Double ONE = Double.valueOf(1.0);

    /** A constant Python {@code float("nan")}. */
    static final Double NAN = Double.NaN;


    /** Value of this {@code float} object. */
    final double value;

    public double getValue() {
        return value;
    }

    /**
     * Constructor for Python sub-class specifying {@link #type}.
     *
     * @param type actual type
     * @param value of the {@code float}
     */
    PyFloat(PyType type, double value) {
        super(type);
        this.value = value;
    }

    // XXX Provide factory from double, but expose no constructor.
    // Is it safe to allow user-defined Java sub-classes of PyFloat?

    public PyFloat(double v) {
        this(TYPE, v);
    }

    public PyFloat(float v) {
        this((double)v);
    }

    // Instance methods on PyFloat -------------------------------------

    @Override
    public String toString() { return Py.defaultToString(this); }

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

    // Constructor from Python ----------------------------------------

    // @formatter:off
    /*
    @ExposedNew
    public static Object float_new(PyNewWrapper new_, boolean init, PyType subtype,
            Object[] args, String[] keywords) {
        ArgParser ap = new ArgParser("float", args, keywords, new String[] {"x"}, 0);
        Object x = ap.getPyObject(0, null);
        if (x == null) {
            if (new_.for_type == subtype) {
                return ZERO;
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
                    // it is not strictly correct (instances of types
                    // that implement the __float__ method are also
                    // valid arguments)
                    throw new TypeError("float() argument must be a string or a number");
                }
                throw e;
            }
            if (new_.for_type == subtype) {
                return floatObject;
            } else {
                return new PyFloatDerived(subtype, floatObject.value);
            }
        }
    }
    */
    // @formatter:on

    /*
    @ExposedGet(name = "real", doc = BuiltinDocs.float_real_doc)
     */
    public Object getReal() {
        return value;
    }

    /*
    @ExposedGet(name = "imag", doc = BuiltinDocs.float_imag_doc)
     */
    public Object getImag() {
        return ZERO;
    }

    /*
    @ExposedClassMethod(doc = BuiltinDocs.float_fromhex_doc)
     */
    public static Object float_fromhex(PyType type, Object o) {
        // XXX: I'm sure this could be shortened/simplified, but Double.parseDouble() takes
        // non-hex strings and requires the form 0xNUMBERpNUMBER for hex input which
        // causes extra complexity here.

        String message = "invalid hexadecimal floating-point string";
        boolean negative = false;

        // XXX Should declare value as String parameter and coerce
        String value = o.toString().trim().toLowerCase();

        if (value.length() == 0) {
            throw new ValueError(message);
        } else if (value.equals("nan") || value.equals("-nan") || value.equals("+nan")) {
            return NAN;
        } else if (value.equals("inf") || value.equals("infinity") || value.equals("+inf")
                || value.equals("+infinity")) {
            return new PyFloat(Double.POSITIVE_INFINITY);
        } else if (value.equals("-inf") || value.equals("-infinity")) {
            return new PyFloat(Double.NEGATIVE_INFINITY);
        }

        // Strip and record + or -
        if (value.charAt(0) == '-') {
            value = value.substring(1);
            negative = true;
        } else if (value.charAt(0) == '+') {
            value = value.substring(1);
        }
        if (value.length() == 0) {
            throw new ValueError(message);
        }

        // Append 0x if not present.
        if (!value.startsWith("0x") && !value.startsWith("0X")) {
            value = "0x" + value;
        }

        // reattach - if needed.
        if (negative) {
            value = "-" + value;
        }

        // Append p if not present.
        if (value.indexOf('p') == -1) {
            value = value + "p0";
        }

        try {
            double d = Double.parseDouble(value);
            if (Double.isInfinite(d)) {
                throw new OverflowError("hexadecimal value too large to represent as a float");
            }
            return new PyFloat(d);
        } catch (NumberFormatException n) {
            throw new ValueError(message);
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
        } else if (java_hex.equals("-0x0.0p0")) {
            return "-0x0.0p+0";
        }

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
                for (int pad = i; pad < padding; pad++) {
                    py_hex.append('0');
                }
                start_exponent = true;
            } else if (start_exponent) {
                if (c != '-') {
                    py_hex.append('+');
                }
                start_exponent = false;
            }
            py_hex.append(c);
        }
        return py_hex.toString();
    }

    /*
    @ExposedMethod(doc = BuiltinDocs.float_hex_doc)
     */
    public Object float_hex() {
        return pyHexString(value);
    }


    // Special methods ------------------------------------------------

    /*
    @ExposedMethod(doc = BuiltinDocs.float___str___doc)
     */
    static final String __str__(Object self) { return formatDouble(doubleValue(self), SPEC_STR); }

    /*
    @ExposedMethod(doc = BuiltinDocs.float___repr___doc)
     */
    static final String __repr__(Object self) { return formatDouble(doubleValue(self), SPEC_REPR); }

    /*
    @ExposedMethod(doc = BuiltinDocs.float___hash___doc)
     */
    final int __hash__() { return __hash__(value); }

    static final int __hash__(Double self) { return __hash__(self.doubleValue()); }

    static final int __hash__(double value) {
        // XXX Essentially copied from Jython 2 but not right for 3
        if (Double.isInfinite(value)) {
            return value < 0 ? -271828 : 314159;
        } else if (Double.isNaN(value)) {
            return 0;
        }

        double intPart = Math.floor(value);
        double fractPart = value - intPart;

        if (fractPart == 0) {
            if (intPart <= Integer.MAX_VALUE && intPart >= Integer.MIN_VALUE) {
                // Yes (short cut)
                return (int)value;
            } else {
                // No, but PyLong is also wrong in this way
                return BigInteger.valueOf((long)intPart).hashCode();
            }
        } else {
            // No, but almost what Java does :/
            long v = Double.doubleToLongBits(value);
            return (int)v ^ (int)(v >> 32);
        }
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
     * Python % operator: y = n*x + z. The modulo operator always yields
     * a result with the same sign as its second operand (or zero).
     * (Compare <code>java.Math.IEEEremainder</code>)
     *
     * @param x dividend
     * @param y divisor
     * @return <code>x % y</code>
     */
    private static double modulo(double x, double y) {
        if (y == 0.0) {
            throw new ZeroDivisionError("float modulo");
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

    /*
    @ExposedMethod(type = MethodType.BINARY, defaults = "null", //
            doc = BuiltinDocs.float___pow___doc)
     */
    static Object __pow__(Object left, Object right, Object modulus) {
        try {
            if (modulus == null || modulus == Py.None) {
                return pow(toDouble(left), toDouble(right));
            } else {
                // Note that we also call __pow__ from PyLong.__pow__
                throw new TypeError(
                        "pow() 3rd argument not allowed unless all arguments are integers");
            }
        } catch (NoConversion e) {
            return Py.NotImplemented;
        }
    }

    /*
    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.float___rpow___doc)
     */
    static Object __rpow__(Object right, Object left) {
        try {
            return pow(toDouble(left), toDouble(right));
        } catch (NoConversion e) {
            return Py.NotImplemented;
        }
    }

       /** Smallest value that cannot be represented as an int */
    private static double INT_LONG_BOUNDARY = -(double)Integer.MIN_VALUE; // 2^31

    /*
    @ExposedMethod(doc = BuiltinDocs.float___int___doc)
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
    @ExposedMethod(doc = BuiltinDocs.float___float___doc)
     */
    Object __float__() { return value; }

    static final Object __float__(Double self) { return self; }

    // Methods --------------------------------------------------------
    // Expose to Python when mechanisms are available

    /*
    @ExposedMethod(doc = BuiltinDocs.float___trunc___doc)
     */
    final Object __trunc__() { return __int__(value); }

    static final Object __trunc__(Double self) { return __int__(self.doubleValue()); }

    /*
    @ExposedMethod(doc = BuiltinDocs.float_conjugate_doc)
     */
    final Object conjugate() { return value; }

    static final Object conjugate(Double self) { return self; }

    /*
    @ExposedMethod(doc = BuiltinDocs.float_is_integer_doc)
     */
    final boolean is_integer() { return is_integer(value); }

    static final boolean is_integer(Double self) { return is_integer(self.doubleValue()); }

    static final boolean is_integer(double self) {
        if (!Double.isFinite(self)) { return false; }
        return Math.floor(self) == self;
    }

    /*
    @ExposedMethod(doc = BuiltinDocs.float___getnewargs___doc)
     */
    final PyTuple __getnewargs__() { return new PyTuple(value); }
    static final PyTuple __getnewargs__(Double self) { return new PyTuple(self); }

    /*
    @ExposedMethod(doc = BuiltinDocs.float___format___doc)
     */
    static final Object __format__(Object self, Object formatSpec) {
        try {
            /*
             * Parse the specification, which must at least sub-class str in
             * Python.
             */
            if (!PyUnicode.TYPE.check(formatSpec)) {
                throw Abstract.argumentTypeError("__format__", 0, "str", formatSpec);
            }

            Spec spec = InternalFormat.fromText(formatSpec.toString());
            return formatDouble(doubleValue(self), spec);
        } catch (IllegalArgumentException iae) {
            // XXX Some format specification errors: why not FormatError?
            throw new ValueError(iae.getMessage());
        }
    }

    /**
     * Format this float according to the specification passed in.
     * Supports {@code __format__}, {@code __str__} and
     * {@code __repr__}.
     *
     * @param value to format
     * @param spec parsed format specification string
     * @return formatted value
     */
    private static String formatDouble(double value, Spec spec) {
        try {
            FloatFormatter f = new Formatter(spec, true);
            return f.format(value).getResult();
        } catch (FormatOverflow fe) {
            throw new OverflowError(fe.getMessage());
        } catch (FormatError fe) {
            throw new ValueError(fe.getMessage());
        }
    }

    /*
    @ExposedMethod(doc = BuiltinDocs.float_as_integer_ratio_doc)
     */
    final PyTuple as_integer_ratio() {return as_integer_ratio(value);}
    static final PyTuple as_integer_ratio(Double self) {return as_integer_ratio(self.doubleValue());}

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
         * CPython comment (not relevant after first sentence):
         * self == float_part * 2**exponent exactly and float_part is integral. If FLT_RADIX != 2,
         * the 300 steps may leave a tiny fractional part to be truncated by PyLong_FromDouble().
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

        return new PyTuple(numerator, denominator);
    }

    // Non-slot API -------------------------------------------------

    /**
     * Present the value as a Java {@code double} when the argument is
     * expected to be a Python {@code float} or a sub-class of it.
     *
     * @param v claimed {@code float}
     * @return {@code double} value
     * @throws TypeError if {@code v} is not a Python {@code float}
     */
    // Compare CPython floatobject.h: PyFloat_AS_DOUBLE
    public static double doubleValue(Object v) throws TypeError {
        if (v instanceof Double)
            return ((Double)v).doubleValue();
        else if (v instanceof PyFloat)
            return ((PyFloat)v).value;
        else
            throw Abstract.requiredTypeError("a float", v);
    }

    /**
     * Convert the argument to a Java {@code double} value. If {@code o}
     * is not a Python {@code float} try the {@code __float__()} method,
     * then {@code __index__()}.
     *
     * @param o to convert
     * @return converted value
     * @throws TypeError if o cannot be interpreted as a {@code float}
     * @throws Throwable from {@code __float__)} or {@code __index__}
     */
    // Compare CPython floatobject.c: PyFloat_AsDouble
    static double asDouble(Object o) throws TypeError, Throwable {
        /*
         * Ever so similar to Number.toFloat, but returns the double
         * value extracted from (potentially) a sub-type of PyFloat, and
         * does not try to convert from strings.
         */

        if (TYPE.check(o)) {
            return doubleValue(o);

        } else {
            Operations ops = Operations.of(o);
            try {
                // Try __float__ (if defined)
                Object res = ops.op_float.invokeExact(o);
                PyType resType = PyType.of(res);
                if (resType == PyFloat.TYPE) // Exact type
                    return doubleValue(res);
                else if (resType.isSubTypeOf(PyFloat.TYPE)) {
                    // Warn about this and make a clean Python float
                    PyFloat.asDouble(Abstract.returnDeprecation(
                            "__float__", "float", res));
                } else
                    // Slot defined but not a Python float at all
                    throw Abstract.returnTypeError("__float__", "float",
                            res);
            } catch (Slot.EmptyException e) {}

            // Fall out here if __float__ was not defined
            if (Slot.op_index.isDefinedFor(ops))
                return PyLong.asDouble(PyNumber.index(o));
            else
                throw Abstract.requiredTypeError("a real number", o);
        }
    }

    // standard singleton issues apply here to __getformat__/__setformat__,
    // but this is what Python demands
    public enum Format {

        UNKNOWN("unknown"), BE("IEEE, big-endian"), LE("IEEE, little-endian");

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

    /*
    @ExposedClassMethod(doc = BuiltinDocs.float___getformat___doc)
     */
    public static String float___getformat__(PyType type, String typestr) {
        if ("double".equals(typestr)) {
            return double_format.format();
        } else if ("float".equals(typestr)) {
            return float_format.format();
        } else {
            throw new ValueError("__getformat__() argument 1 must be 'double' or 'float'");
        }
    }

    /*
    @ExposedClassMethod(doc = BuiltinDocs.float___setformat___doc)
     */
    public static void float___setformat__(PyType type, String typestr, String format) {
        Format new_format = null;
        if (!"double".equals(typestr) && !"float".equals(typestr)) {
            throw new ValueError("__setformat__() argument 1 must be 'double' or 'float'");
        }
        if (Format.LE.format().equals(format)) {
            throw new ValueError(String.format("can only set %s format to 'unknown' or the "
                    + "detected platform value", typestr));
        } else if (Format.BE.format().equals(format)) {
            new_format = Format.BE;
        } else if (Format.UNKNOWN.format().equals(format)) {
            new_format = Format.UNKNOWN;
        } else {
            throw new ValueError("__setformat__() argument 2 must be 'unknown', "
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

    // Java-only API -------------------------------------------------

    /**
     * Convert a Python {@code float}, {@code int} or {@code bool} to a
     * Java {@code double} (or throw {@link NoConversion}). Conversion
     * from an {@code int} may overflow.
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
     * @throws OverflowError if v is an {@code int} out of range
     */
    static double convertToDouble(Object v) throws NoConversion, OverflowError {
        if (v instanceof Double)
            return ((Double)v).doubleValue();
        else if (v instanceof PyUnicode)
            return ((PyFloat)v).value;
        else
            // BigInteger, PyLong, Boolean, etc. or throw
            return PyLong.convertToDouble(v);
    }

    // Python sub-class -----------------------------------------------

    /**
     * Instances in Python of sub-classes of 'float', are represented in
     * Java by instances of this class.
     */
    static class Derived extends PyFloat implements DictPyObject {

        protected Derived(PyType subType, double value) {
            super(subType, value);
        }

        // /** The instance dictionary {@code __dict__}. */
        // protected PyDict dict = new PyDict();

        @Override
        public Map<Object, Object> getDict() { return null; }
    }

    // formatter ------------------------------------------------------

    /**
     * A {@link Formatter}, constructed from a {@link Spec}, with
     * specific validations for {@code int.__format__}.
     */
    static class Formatter extends FloatFormatter {

        /**
         * If {@code true}, give {@code printf}-style meanings to
         * {@link Spec#type}.
         */
        final boolean printf;

        /**
         * Prepare a {@link Formatter} in support of {@code str.__mod__},
         * that is, traditional {@code printf}-style formatting.
         *
         * @param spec a parsed format specification.
         * @param printf f {@code true}, interpret {@code spec}
         *     {@code printf}-style, otherwise as
         *     {@link Formatter#Formatter(Spec) Formatter(Spec)}
         * @throws FormatOverflow if a value is out of range (including the
         *     precision)
         * @throws FormatError if an unsupported format character is
         *     encountered
         */
        Formatter(Spec spec, boolean printf) throws FormatError {
            super(validated(spec, printf));
            this.printf = printf;
        }

        /**
         * Prepare a {@link Formatter} in support of
         * {@link PyFloat#__format__(Object, Object) float.__format__}.
         *
         * @param spec a parsed PEP-3101 format specification.
         * @throws FormatOverflow if a value is out of range (including the
         *     precision)
         * @throws FormatError if an unsupported format character is
         *     encountered
         */
        Formatter(Spec spec) throws FormatError { this(spec, false); }

        /**
         * Validations and defaults specific to {@code float}.
         *
         * @param spec to validate
         * @return validated spec with defaults filled
         * @throws FormatError on failure to validate
         */
        private static Spec validated(Spec spec, boolean printf) throws FormatError {
            String type = TYPE.name;

            switch (spec.type) {

                case 'n':
                    if (spec.grouping) { throw notAllowed("Grouping", type, spec.type); }
                    //$FALL-THROUGH$

                case Spec.NONE:
                case 'e':
                case 'f':
                case 'g':
                case 'E':
                case 'F':
                case 'G':
                case '%':
                    // Check for disallowed parts of the specification
                    if (spec.alternate) { throw alternateFormNotAllowed(type); }
                    break;

                case 'r':
                case 's':
                    // Only allow for printf-style formatting
                    if (printf) { break; }
                    //$FALL-THROUGH$

                default:
                    // The type code was not recognised
                    throw unknownFormat(spec.type, type);
            }

            /*
             * spec may be incomplete. The defaults are those commonly used for
             * numeric formats.
             */
            return spec.withDefaults(Spec.NUMERIC);
        }

        @Override
        public FloatFormatter format(Object o) throws NoConversion, FormatError {
            return format(convertToDouble(o));
        }
    }

    // plumbing ------------------------------------------------------

    /**
     * Convert a Java {@code double} to Java {@code BigInteger} by
     * truncation.
     *
     * @param value to convert
     * @return BigInteger equivalent.
     * @throws OverflowError when this is a floating infinity
     * @throws ValueError when this is a floating NaN
     */
    // Somewhat like CPython longobject.c :: PyLong_FromDouble
    static BigInteger bigIntegerFromDouble(double value) throws OverflowError, ValueError {

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
    private static final int SIGNIFICAND_BITS = 52; // not counting the implied 1
    private static final int EXPONENT_BITS = 11;
    private static final int EXPONENT_BIAS = 1023;

    // Masks derived from the 64-bit floating point parameters
    private static final long IMPLIED_ONE = 1L << SIGNIFICAND_BITS; // = 0x0010000000000000L
    private static final long SIGNIFICAND = IMPLIED_ONE - 1;        // = 0x000fffffffffffffL
    private static final long SIGN = IMPLIED_ONE << EXPONENT_BITS;  // = 0x8000000000000000L;
    private static final long EXPONENT = SIGN - IMPLIED_ONE;        // = 0x7ff0000000000000L;

    private static OverflowError cannotConvertInf(String to) {
        String msg = String.format(CANNOT_CONVERT, "infinity", to);
        return new OverflowError(msg);
    }

    private static ValueError cannotConvertNaN(String to) {
        String msg = String.format(CANNOT_CONVERT, "NaN", to);
        return new ValueError(msg);
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
         * This code was translated from the CPython implementation at
         * v2.7.8 by progressively removing cases that could be delegated to
         * Java. Jython differs from CPython in that where C pow()
         * overflows, Java pow() returns inf (observed on Windows). This is
         * not subject to regression tests, so we take it as an allowable
         * platform dependency. All other differences in Java Math.pow() are
         * trapped below and Python behaviour is enforced.
         */
        if (w == 0) {
            // v**0 is 1, even 0**0 and nan**0
            return ONE;

        } else if (Double.isNaN(v)) {
            // nan**w = nan, unless w == 0
            return NAN;

        } else if (Double.isNaN(w)) {
            // v**nan = nan, unless v == 1; 1**nan = 1
            return v == 1.0 ? ONE : NAN;

        } else if (Double.isInfinite(w)) {
            /*
             * In Java Math pow(1,inf) = pow(-1,inf) = pow(1,-inf) =
             * pow(-1,-inf) = nan, but in Python they are all 1.
             */
            if (v == 1.0 || v == -1.0) { return ONE; }

        } else if (v == 0.0) {
            // 0**w is an error if w is negative.
            if (w < 0.0) {
                throw new ZeroDivisionError("0.0 cannot be raised to a negative power");
            }

        } else if (!Double.isInfinite(v) && v < 0.0) {
            if (w != Math.floor(w)) {
                throw new ValueError("negative number cannot be raised to a fractional power");
            }
        }

        // In all other cases entrust the calculation to Java
        return Math.pow(v, w);
    }


    /** Used as error message text for division by zero. */
    static final String DIV_ZERO = "float division by zero";
    /** Used as error message text for modulo zero. */
    static final String MOD_ZERO = "float modulo zero";

    /**
     * Convenience function to throw a {@link ZeroDivisionError} if the
     * argument is zero. (Java float arithmetic does not throw whatever
     * the arguments.)
     *
     * @param v value to check is not zero
     * @param msg for exception if {@code v==0.0}
     * @return {@code v}
     */
    static double nonzero(double v, String msg) {
        if (v == 0.0) { throw new ZeroDivisionError(msg); }
        return v;
    }

    /**
     * Convenience function to throw a {@link ZeroDivisionError} if the
     * argument is zero. (Java float arithmetic does not throw whatever
     * the arguments.)
     *
     * @param v value to check is not zero
     * @return {@code v}
     */
    static double nonzero(double v) {
        if (v == 0.0) { throw new ZeroDivisionError(DIV_ZERO); }
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
            if (y == 0.) {
                throw new ZeroDivisionError(DIV_ZERO);
            } else {
                return Double.NaN;
            }
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
            if (y == 0.) { throw new ZeroDivisionError(MOD_ZERO); }
            // Otherwise nan is fine
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
        return new PyTuple(floordiv(x, y), mod(x, y));
    }
}
