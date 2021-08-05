// Copyright (c) Corporation for National Research Initiatives
// Copyright (c)2021 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.core;

import static java.math.BigInteger.ONE;
import static java.math.BigInteger.ZERO;

import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.math.BigInteger;
import java.util.Map;

import org.python.base.InterpreterError;
import org.python.base.MissingFeature;
import org.python.core.PyObjectUtil.NoConversion;
import org.python.core.Slot.EmptyException;
import org.python.core.stringlib.FloatFormatter;
import org.python.core.stringlib.IntegerFormatter;
import org.python.core.stringlib.InternalFormat;
import org.python.core.stringlib.InternalFormat.FormatError;
import org.python.core.stringlib.InternalFormat.FormatOverflow;
import org.python.core.stringlib.InternalFormat.Formatter;
import org.python.core.stringlib.InternalFormat.Spec;

/**
 * A Python {@code int} object may be represented by a
 * {@code java.lang.Integer} or a {@code java.math.BigInteger}. An
 * instance of a Python sub-class of {@code int}, must be
 * represented by an instance of a Java sub-class of) this class.
 */
// @Untraversable
// @ExposedType(name = "long", doc = BuiltinDocs.long_doc)
public class PyLong extends AbstractPyObject {

    /** The type {@code int}. */
    public static final PyType TYPE = PyType.fromSpec( //
            new PyType.Spec("int", MethodHandles.lookup()) //
                    .adopt(BigInteger.class, Integer.class) //
                    .accept(Boolean.class) //
                    .methods(PyLongMethods.class));

    public static final BigInteger MIN_LONG = BigInteger.valueOf(Long.MIN_VALUE);
    public static final BigInteger MAX_LONG = BigInteger.valueOf(Long.MAX_VALUE);
    public static final BigInteger MAX_ULONG = ONE.shiftLeft(64).subtract(ONE);

    /**
     * The value of this Python {@code int} (in sub-class instances).
     */
    // Has to be package visible for method implementations.
    final BigInteger value;

    /**
     * The value of this Python {@code int} (in sub-class instances).
     *
     * @return value
     */
    public BigInteger getValue() { return value; }

    /**
     * Constructor for Python sub-class specifying {@link #type}.
     *
     * @param subType actual Python sub-class being created
     * @param v of the {@code int}
     */
    private PyLong(PyType subType, BigInteger v) {
        super(subType);
        value = v;
    }

    // Instance methods on PyLong -------------------------------------

    @Override
    public boolean equals(Object obj) {
        try {
            // XXX Use Dict.pythonEquals when available
            return getValue().equals(asBigInteger(convertToInt(obj)));
        } catch (Throwable e) {
            return false;
        }
    }

    // Constructor from Python ----------------------------------------

    @SuppressWarnings("fallthrough")
    static Object __new__(PyType subType, Object[] args, String[] kwnames) throws Throwable {
        Object x = null, obase = null;
        int argsLen = args.length;
        switch (argsLen) {
            case 2:
                obase = args[1]; // fall through
            case 1:
                x = args[0]; // fall through
            case 0:
                break;
            default:
                throw new TypeError("int() takes at most %d arguments (%d given)", 2, argsLen);
        }
        return __new__impl(subType, x, obase);
    }

    /**
     * Implementation of {@code __new__} with classic arguments
     * unpacked.
     *
     * @param subType actual sub-type of int to produce
     * @param x {@code int}-like or {@code str}-like value or
     *     {@code null}.
     * @param obase number base ({@code x} must be {@code str}-like)
     * @return an {@code int} or sub-class with the right value
     * @throws Throwable on argument type or other errors
     */
    private static Object __new__impl(PyType subType, Object x, Object obase) throws Throwable {

        if (subType != TYPE) { return longSubtypeNew(subType, x, obase); }

        if (x == null) {
            // Zero-arg int() ... unless invalidly like int(base=10)
            if (obase != null) { throw new TypeError("int() missing string argument"); }
            return 0;
        }

        if (obase == null)
            return PyNumber.asLong(x);
        else {
            int base = PyNumber.asSize(obase, null);
            if (base != 0 && (base < 2 || base > 36))
                throw new ValueError("int() base must be >= 2 and <= 36, or 0");
            else if (PyUnicode.TYPE.check(x))
                return PyLong.fromUnicode(x, base);
            // else if ... support for bytes-like objects
            else
                throw new TypeError(NON_STR_EXPLICIT_BASE);
        }
    }

    private static final String NON_STR_EXPLICIT_BASE =
            "int() can't convert non-string with explicit base";

    /**
     * Wimpy, slow approach to {@code __new__} calls for sub-types of
     * {@code int}, that will temporarily create a regular {@code int}
     * from the arguments.
     *
     * @throws Throwable on argument type or other errors
     */
    private static Object longSubtypeNew(PyType subType, Object x, Object obase) throws Throwable {
        // Create a regular int from whatever arguments we got.
        Object v = __new__impl(TYPE, x, obase);
        // create a sub-type instance from the value in tmp
        return new PyLong.Derived(subType, PyLong.asBigInteger(v));
    }

    // Representations of the value -----------------------------------

    /**
     * Present the value as a Java {@code int} when the argument is
     * expected to be a Python {@code int} or a sub-class of it.
     *
     * @param v claimed {@code int}
     * @return {@code int} value
     * @throws TypeError if {@code v} is not a Python {@code int}
     * @throws OverflowError if {@code v} is out of Java range
     */
    static int asInt(Object v) throws TypeError, OverflowError {
        try {
            return convertToInt(v);
        } catch (NoConversion nc) {
            throw Abstract.requiredTypeError("an integer", v);
        }
    }

    /**
     * Present the value as a Java {@code int} when the argument is
     * expected to be a Python {@code int} or a sub-class of it.
     *
     * @param v claimed {@code int}
     * @return {@code int} value
     * @throws TypeError if {@code v} is not a Python {@code int}
     * @throws OverflowError if {@code v} is out of Java range
     */
    static int asSize(Object v) throws TypeError, OverflowError { return asInt(v); }

    /**
     * Present the value as a Java {@code BigInteger} when the argument
     * is expected to be a Python {@code int} or a sub-class of it.
     *
     * @param v claimed {@code int}
     * @return {@code BigInteger} value
     * @throws TypeError if {@code v} is not a Python {@code int}
     */
    static BigInteger asBigInteger(Object v) throws TypeError {
        try {
            return convertToBigInteger(v);
        } catch (NoConversion nc) {
            throw Abstract.requiredTypeError("an integer", v);
        }
    }

    /**
     * Value as a Java {@code double} using the round-half-to-even rule.
     *
     * @param v to convert
     * @return nearest double
     * @throws OverflowError if out of double range
     */
    // Compare CPython longobject.c: PyLong_AsDouble
    static double asDouble(Object v) {
        try {
            return convertToDouble(v);
        } catch (NoConversion nc) {
            throw Abstract.requiredTypeError("an integer", v);
        }
    }

    static int signum(Object v) throws TypeError {
        if (v instanceof BigInteger)
            return ((BigInteger)v).signum();
        else if (v instanceof Integer)
            return Integer.signum((Integer)v);
        else if (v instanceof PyLong)
            return ((PyLong)v).value.signum();
        else if (v instanceof Boolean)
            return (Boolean)v ? 1 : 0;
        else
            throw Abstract.requiredTypeError("an integer", v);
    }

    // Factories ------------------------------------------------------

    /*
     * These methods create Python int from other Python objects, or
     * from specific Java types. The methods make use of special methods
     * on the argument and produce Python exceptions when that goes
     * wrong. Note that they never produce a PyLong, but always Java
     * Integer or BigInteger. The often correspond to CPython public or
     * internal API.
     */
    /**
     * Convert the given object to a Python {@code int} using the
     * {@code op_int} slot, if available. Raise {@code TypeError} if
     * either the {@code op_int} slot is not available or the result of
     * the call to {@code op_int} returns something not of type
     * {@code int}.
     * <p>
     * The return is not always exactly an {@code int}.
     * {@code integral.__int__}, which this method wraps, may return any
     * type: Python sub-classes of {@code int} are tolerated, but with a
     * deprecation warning. Returns not even a sub-class type
     * {@code int} raise {@link TypeError}.
     *
     * @param integral to convert to {@code int}
     * @return integer value of argument
     * @throws TypeError if {@code integral} seems not to be
     * @throws Throwable from the supporting implementation
     */
    // Compare CPython longobject.c::_PyLong_FromNbInt
    static Object fromIntOf(Object integral) throws TypeError, Throwable {
        Operations ops = Operations.of(integral);

        if (ops.isIntExact()) {
            // Fast path for the case that we already have an int.
            return integral;
        }

        else
            try {
                /*
                 * Convert using the op_int slot, which should return something of
                 * exact type int.
                 */
                Object r = ops.op_int.invokeExact(integral);
                if (PyLong.TYPE.checkExact(r)) {
                    return r;
                } else if (PyLong.TYPE.check(r)) {
                    // Result not of exact type int but is a subclass
                    Abstract.returnDeprecation("__int__", "int", r);
                    return r;
                } else
                    throw Abstract.returnTypeError("__int__", "int", r);
            } catch (EmptyException e) {
                // __int__ is not defined for t
                throw Abstract.requiredTypeError("an integer", integral);
            }
    }

    /**
     * Convert the given object to a {@code int} using the
     * {@code __index__} or {@code __int__} special methods, if
     * available (the latter is deprecated).
     * <p>
     * The return is not always exactly an {@code int}.
     * {@code integral.__index__} or {@code integral.__int__}, which
     * this method wraps, may return any type: Python sub-classes of
     * {@code int} are tolerated, but with a deprecation warning.
     * Returns not even a sub-class type {@code int} raise
     * {@link TypeError}. This method should be replaced with
     * {@link PyNumber#index(Object)} after the end of the deprecation
     * period.
     *
     * @param integral to convert to {@code int}
     * @return integer value of argument
     * @throws TypeError if {@code integral} seems not to be
     * @throws Throwable from the supporting implementation
     */
    // Compare CPython longobject.c :: _PyLong_FromNbIndexOrNbInt
    static Object fromIndexOrIntOf(Object integral) throws TypeError, Throwable {
        Operations ops = Operations.of(integral);
        ;

        if (ops.isIntExact())
            // Fast path for the case that we already have an int.
            return integral;

        try {
            // Normally, the op_index slot will do the job
            Object r = ops.op_index.invokeExact(integral);
            if (Operations.of(r).isIntExact())
                return r;
            else if (PyLong.TYPE.check(r)) {
                // 'result' not of exact type int but is a subclass
                Abstract.returnDeprecation("__index__", "int", r);
                return r;
            } else
                throw Abstract.returnTypeError("__index__", "int", r);
        } catch (EmptyException e) {}

        // We're here because op_index was empty. Try op_int.
        if (Slot.op_int.isDefinedFor(ops)) {
            Object r = fromIntOf(integral);
            // ... but grumble about it.
            // Warnings.format(DeprecationWarning.TYPE, 1,
            // "an integer is required (got type %.200s). "
            // + "Implicit conversion to integers "
            // + "using __int__ is deprecated, and may be "
            // + "removed in a future version of Python.",
            // ops.type(integral).name);
            return r;
        } else
            throw Abstract.requiredTypeError("an integer", integral);
    }

    /**
     * Convert a sequence of Unicode digits in the string u to a Python
     * integer value.
     *
     * @param u string to convert
     * @param base in which to interpret it
     * @return converted value
     * @throws ValueError if {@code u} is an invalid literal
     * @throws TypeError if {@code u} is not a Python {@code str}
     */
    // Compare CPython longobject.c :: PyLong_FromUnicodeObject
    static BigInteger fromUnicode(Object u, int base) throws ValueError, TypeError {
        try {
            // XXX maybe check 2<=base<=36 even if Number.asLong does?
            // XXX Should allow for only string types, but for now ...
            String value = u.toString();
            // String value = PyUnicode.asString(u);
            return new BigInteger(value, base);
        } catch (NumberFormatException e) {
            throw new ValueError("invalid literal for int() with base %d: %.200s", base, u);
        }
    }

    /**
     * Return a Python {@code int} from a Python {@code int} or
     * subclass. If the value has exactly Python type {@code int} return
     * it, otherwise construct a new instance of exactly {@code int}
     * type.
     *
     * @param value to represent
     * @return the same value as exactly {@code int}
     * @throws TypeError if not a Python {@code int} or sub-class
     */
    // Compare CPython longobject.c :: long_long
    static Object from(Object value) throws TypeError {
        Operations ops = Operations.of(value);
        if (ops.isIntExact())
            return value;
        else if (value instanceof PyLong)
            return ((PyLong)value).value;
        else
            throw Abstract.requiredTypeError("an integer", value);
    }

    // ----------------------------------------------------------------

    public long getLong(long min, long max) throws OverflowError {
        return getLong(min, max, "long int too large to convert");
    }

    public long getLong(long min, long max, String overflowMsg) throws OverflowError {
        if (getValue().compareTo(MAX_LONG) <= 0 && getValue().compareTo(MIN_LONG) >= 0) {
            long v = getValue().longValue();
            if (v >= min && v <= max) { return v; }
        }
        throw new OverflowError(overflowMsg);
    }

    public long asLong() {
        return getLong(Long.MIN_VALUE, Long.MAX_VALUE, "long too big to convert");
    }

    public Object __tojava__(Class<?> c) {
        // XXX something like this necessary in Jython 3 but what?
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
                return asDouble(this);
            }
            if (c == BigInteger.class || c == Number.class || c == Object.class
                    || c == Serializable.class) {
                return getValue();
            }
        } catch (OverflowError e) {
            throw new InternalError(e);
        }
        throw new MissingFeature("default __tojava__ behaviour for %s", c.getSimpleName());
    }

    // XXX __coerce__ and __coerce_ex not needed in Jython 3 ?

    // special methods ------------------------------------------------

    @SuppressWarnings("unused")
    private static Object __repr__(Object self) {
        assert TYPE.check(self);
        return asBigInteger(self).toString();
    }

    // __str__: let object.__str__ handle it (calls __repr__)

    // Methods --------------------------------------------------------
    // Expose to Python when mechanisms are available

    // @ExposedGet(name = "real", doc = BuiltinDocs.long_real_doc)
    public static Object getReal(Object self) { return self; }

    // @ExposedGet(name = "imag", doc = BuiltinDocs.long_imag_doc)
    public static Object getImag(Object self) { return 0; }

    // @ExposedGet(name = "numerator", doc =
    // BuiltinDocs.long_numerator_doc)
    public static Object getNumerator(Object self) { return self; }

    // @ExposedGet(name = "denominator", doc =
    // BuiltinDocs.long_denominator_doc)
    public static Object getDenominator(Object self) { return 1; }

    // @ExposedMethod(doc = BuiltinDocs.long___trunc___doc)
    static Object __trunc__(Object self) { return from(self); }

    // @ExposedMethod(doc = BuiltinDocs.long_conjugate_doc)
    static Object conjugate(Object self) { return from(self); }

    // @ExposedMethod(doc = BuiltinDocs.long___getnewargs___doc)
    static PyTuple __getnewargs__(Object self) {
        assert TYPE.check(self);
        try {
            return new PyTuple(convertToBigInteger(self));
        } catch (NoConversion e) {
            throw impossible(self);
        }
    }

    // @ExposedMethod(doc = BuiltinDocs.long_bit_length_doc)
    static int bit_length(Object self) {
        try {
            BigInteger v = convertToBigInteger(self);
            if (v.signum() == -1) { v = v.negate(); }
            return v.bitLength();
        } catch (NoConversion e) {
            throw impossible(self);
        }
    }

    // @ExposedMethod(doc = BuiltinDocs.long___format___doc)
    static Object __format__(Object self, Object formatSpec) {
        try {
            // Parse the specification, which must at least sub-class str in
            // Python
            if (!PyUnicode.TYPE.check(formatSpec)) {
                throw Abstract.argumentTypeError("__format__", 0, "str", formatSpec);
            }

            BigInteger value = convertToBigInteger(self);
            Spec spec = InternalFormat.fromText(formatSpec.toString());
            InternalFormat.Formatter f;

            // Try to make an integer formatter from the specification
            IntegerFormatter fi = IntegerFormatter.prepareFormatter(spec);
            if (fi != null) {
                // Bytes mode if formatSpec argument is not unicode.
                fi.setBytes(!(formatSpec instanceof PyUnicode));
                // Convert as per specification.
                fi.format(value);
                f = fi;

            } else {
                // Try to make a float formatter from the specification
                FloatFormatter ff = FloatFormatter.prepareFormatter(spec);
                if (ff != null) {
                    // Bytes mode if formatSpec argument is not str.
                    ff.setBytes(!(formatSpec instanceof PyUnicode));
                    // Convert as per specification.
                    ff.format(value.doubleValue());
                    f = ff;

                } else {
                    // The type code was not recognised in either prepareFormatter
                    throw Formatter.unknownFormat(spec.type, "integer");
                }
            }

            /*
             * Return a result that has the same type (str or unicode) as the
             * formatSpec argument.
             */
            return f.pad().getResult();

        } catch (FormatOverflow fe) {
            throw new OverflowError(fe.getMessage());
        } catch (FormatError fe) {
            throw new ValueError(fe.getMessage());
        } catch (NoConversion e) {
            throw impossible(self);
        }
    }

    // Python sub-class -----------------------------------------------

    /**
     * Instances in Python of sub-classes of 'int', are represented in
     * Java by instances of this class.
     */
    static class Derived extends PyLong implements DictPyObject {

        protected Derived(PyType subType, BigInteger value) { super(subType, value); }

        // /** The instance dictionary {@code __dict__}. */
        // protected PyDict dict = new PyDict();

        @Override
        public Map<Object, Object> getDict() { return null; }
    }

    // plumbing -------------------------------------------------------

    // Convert from int (core use) ------------------------------------

    /*
     * These methods are for use internal to the core, in the
     * implementation of special functions: they may throw NoConversion
     * of failure, which must be caught by those implementations. They
     * convert a Python int, or a specific Java implementation of int,
     * to a specific Java type.
     */

    /**
     * Convert an {@code int} to a Java {@code double} (or throw
     * {@link NoConversion}), using the round-half-to-even rule.
     * Conversion to a {@code double} may overflow, raising an exception
     * that is propagated to the caller.
     * <p>
     * If the method throws the special exception {@link NoConversion},
     * the caller must deal with it by throwing an appropriate Python
     * exception or taking an alternative course of action. Binary
     * operations will normally return {@link Py#NotImplemented} in
     * response.
     *
     * @param v to convert
     * @return converted to {@code double}
     * @throws NoConversion v is not an {@code int}
     * @throws OverflowError v is too large to be a {@code float}
     */
    // Compare CPython longobject.c: PyLong_AsDouble
    static double convertToDouble(Object v) throws NoConversion, OverflowError {
        // Check against supported types, most likely first
        if (v instanceof Integer)
            // No loss of precision
            return ((Integer)v).doubleValue();
        else if (v instanceof BigInteger)
            // Round half-to-even
            return convertToDouble((BigInteger)v);
        else if (v instanceof PyLong)
            // Round half-to-even
            return convertToDouble(((PyLong)v).value);
        else if (v instanceof Boolean)
            return (Boolean)v ? 1.0 : 0.0;
        throw PyObjectUtil.NO_CONVERSION;
    }

    /**
     * Convert a {@code BigInteger} to a Java double , using the
     * round-half-to-even rule. Conversion to a double may overflow,
     * raising an exception that is propagated to the caller.
     *
     * @param v to convert
     * @return converted to {@code double}
     * @throws OverflowError if too large to be a {@code float}
     */
    static double convertToDouble(BigInteger v) throws OverflowError {
        /*
         * According to the code, BigInteger.doubleValue() rounds
         * half-to-even as required. This differs from conversion from long
         * which rounds to nearest (JLS 3.0 5.1.2).
         */
        double vv = v.doubleValue();
        // On overflow, doubleValue returns ±∞ rather than throwing.
        if (Double.isInfinite(vv))
            throw tooLarge("Python int", "float");
        else
            return vv;
    }

    /**
     * Convert a Python {@code int} to a Java {@code int} (or throw
     * {@link NoConversion}). Conversion to an {@code int} may overflow,
     * raising an exception that is propagated to the caller.
     * <p>
     * If the method throws the special exception {@link NoConversion},
     * the caller must deal with it by throwing an appropriate Python
     * exception or taking an alternative course of action.
     *
     * @param v to convert
     * @return converted to {@code int}
     * @throws NoConversion v is not an {@code int}
     * @throws OverflowError v is too large to be a Java {@code int}
     */
    // Compare CPython longobject.c: PyLong_AsSsize_t
    static int convertToInt(Object v) throws NoConversion, OverflowError {
        // Check against supported types, most likely first
        if (v instanceof Integer)
            return ((Integer)v).intValue();
        else if (v instanceof BigInteger)
            return convertToInt((BigInteger)v);
        else if (v instanceof PyLong)
            return convertToInt(((PyLong)v).value);
        else if (v instanceof Boolean)
            return (Boolean)v ? 1 : 0;
        throw PyObjectUtil.NO_CONVERSION;
    }

    /**
     * Convert a {@code BigInteger} to a Java {@code int}. Conversion to
     * an {@code int} may overflow, raising an exception that is
     * propagated to the caller.
     *
     * @param v to convert
     * @return converted to {@code int}
     * @throws OverflowError if too large to be a Java {@code int}
     */
    static int convertToInt(BigInteger v) throws OverflowError {
        if (v.bitLength() < 32)
            return v.intValue();
        else
            throw tooLarge("Python int", "int");
    }

    /**
     * Convert a Python {@code int} to a Java {@code BigInteger} (or
     * throw {@link NoConversion}). Conversion may raise an exception
     * that is propagated to the caller. If the Java type of the
     * {@code int} is declared, generally there is a better option than
     * this method. We only use it for {@code Object} arguments.
     * <p>
     * If the method throws the special exception {@link NoConversion},
     * the caller must deal with it by throwing an appropriate Python
     * exception or taking an alternative course of action.
     *
     * @param v claimed {@code int}
     * @return converted to {@code BigInteger}
     * @throws NoConversion if {@code v} is not a Python {@code int}
     */
    static BigInteger convertToBigInteger(Object v) throws NoConversion {
        if (v instanceof BigInteger)
            return (BigInteger)v;
        else if (v instanceof Integer)
            return BigInteger.valueOf(((Integer)v).longValue());
        else if (v instanceof PyLong)
            return ((PyLong)v).value;
        else if (v instanceof Boolean)
            return (Boolean)v ? ONE : ZERO;
        throw PyObjectUtil.NO_CONVERSION;
    }

    /**
     * Create an OverflowError with a message along the lines "X too
     * large to convert to Y", where X is {@code from} and Y is
     * {@code to}.
     *
     * @param from description of type to convert from
     * @param to description of type to convert to
     * @return an {@link OverflowError} with that message
     */
    static OverflowError tooLarge(String from, String to) {
        String msg = String.format(TOO_LARGE, from, to);
        return new OverflowError(msg);
    }

    private static final String TOO_LARGE = "%s too large to convert to %s";

    /**
     * We received an argument that should be impossible in a correct
     * interpreter. We use this when conversion of an
     * {@code Object self} argument may theoretically fail, but we know
     * that we should only reach that point by paths that guarantee
     * {@code self`} to be some kind on {@code int}.
     *
     * @param o actual argument
     * @return exception to throw
     */
    private static InterpreterError impossible(Object o) {
        return Abstract.impossibleArgumentError("int", o);
    }
}
