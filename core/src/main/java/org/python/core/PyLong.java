// Copyright (c) Corporation for National Research Initiatives
// Copyright (c) Jython Developers

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

    /** The value of this Python {@code int} (in sub-class instances). */
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

        if (subType != TYPE) {
            return longSubtypeNew(subType, x, obase);
        }

        if (x == null) {
            // Zero-arg int() ... unless invalidly like int(base=10)
            if (obase != null) {
                throw new TypeError("int() missing string argument");
            }
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
    static Object from(Object value) throws TypeError {
        Operations ops = Operations.of(value);
        if (ops.isIntExact())
            return value;
        else if (value instanceof PyLong)
            return ((PyLong)value).value;
        else
            throw Abstract.requiredTypeError("an integer", value);
    }


    // Methods --------------------------------------------------------
    // Expose to Python when mechanisms are available

    /*

    @ExposedGet(name = "real", doc = BuiltinDocs.long_real_doc)
     */
    public static Object getReal(Object self) { return self; }

    /*

    @ExposedGet(name = "imag", doc = BuiltinDocs.long_imag_doc)
     */
    public static Object getImag(Object self) { return 0; }

    /*

    @ExposedGet(name = "numerator", doc = BuiltinDocs.long_numerator_doc)
     */
    public static Object getNumerator(Object self) { return self; }

    /*

    @ExposedGet(name = "denominator", doc = BuiltinDocs.long_denominator_doc)
     */
    public static Object getDenominator(Object self) { return 1; }

    // ----------------------------------------------------------------

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

    public double scaledDoubleValue(int[] exp) { return scaledDoubleValue(getValue(), exp); }

    // ----------------------------------------------------------------

    public long getLong(long min, long max) throws OverflowError {
        return getLong(min, max, "long int too large to convert");
    }

    public long getLong(long min, long max, String overflowMsg) throws OverflowError {
        if (getValue().compareTo(MAX_LONG) <= 0 && getValue().compareTo(MIN_LONG) >= 0) {
            long v = getValue().longValue();
            if (v >= min && v <= max) {
                return v;
            }
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
                return __float__(this);
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

    // XXX __coerce__ and __coerce_ex not needed in Jython 3 (reasonably
    // certain).

    /**
     * Convert an {@code int} or its sub-class to a Java
     * {@code BigInteger}. Conversion may raise an exception that is
     * propagated to the caller. If the Java type of the {@code int} is
     * declared, generally there is a better option than this method. We
     * only use it for {@code Object} arguments. If the method throws
     * the special exception {@link NoConversion}, the caller must catch
     * it, and will normally return {@link Py#NotImplemented}.
     *
     * @param v to convert
     * @return converted to {@code BigInteger}
     * @throws NoConversion v is not an {@code int}
     */
    private static BigInteger toBig(Object v) throws NoConversion {
        // Check against supported types, most likely first
        if (v instanceof Integer)
            return BigInteger.valueOf(((Integer)v).longValue());
        else if (v instanceof BigInteger)
            return (BigInteger)v;
        else if (v instanceof PyLong)
            return ((PyLong)v).value;
        else if (v instanceof Boolean)
            return (Boolean)v ? ONE : ZERO;

        throw PyObjectUtil.NO_CONVERSION;
    }

    /**
     * Reduce a {@code BigInteger} result to {@code Integer} if
     * possible. This makes it more likely the next operation will be
     * 32-bit.
     *
     * @param r to reduce
     * @return equal value
     */
    static Object toInt(BigInteger r) {
        /*
         * Implementation note: r.intValueExact() is for exactly this
         * purpose, but building the ArithmeticException is a huge cost.
         * (2900ns is added to a 100ns __add__.) The compiler (as tested in
         * JDK 11.0.9) doesn't recognise that it can be optimised to a jump.
         * This version of toInt() adds around 5ns.
         */
        if (r.bitLength() < 32)
            return r.intValue();
        else
            return r;
    }

    /**
     * Convert a Python {@code object} to a Java {@code int} suitable as
     * a shift distance. Negative values are a {@link ValueError}, while
     * positive values too large to convert are clipped to the maximum
     * Java {@code int} value.
     *
     * @param shift to interpret as an {@code int} shift
     * @return {@code min(v, Integer.MAX_VALUE)}
     * @throws NoConversion for values not convertible to a Python
     *     {@code int}
     * @throws ValueError when the argument is negative
     */
    private static final int toShift(Object shift) throws NoConversion, ValueError {
        BigInteger s = toBig(shift); // implicitly: check its an int
        if (s.signum() < 0) {
            throw new ValueError("negative shift count");
        } else if (s.bitLength() < 32) {
            return s.intValue();
        } else {
            return Integer.MAX_VALUE;
        }
    }

    // special methods ------------------------------------------------

    /*

    @ExposedMethod(type = MethodType.BINARY, defaults = {"null"},
            doc = BuiltinDocs.long___pow___doc)
     */
    static Object __pow__(Object self, Object right, Object modulo) {
        try {
            modulo = (modulo == Py.None) ? null : modulo;
            return _pow(toBig(self), toBig(right), modulo, self, right);
        } catch (NoConversion e) {
            return Py.NotImplemented;
        }
    }

    /*

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.long___rpow___doc)
     */
    static Object __rpow__(Object self, Object left) {
        try {
            return _pow(toBig(left), toBig(self), null, left, self);
        } catch (NoConversion e) {
            return Py.NotImplemented;
        }
    }

    private static Object _pow(BigInteger x, BigInteger y, Object modulo, Object left, Object right)
            throws NoConversion {

        // For negative exponent, resort to float calculation
        // XXX This is the only reason we pass left, right. Lift to caller.
        if (y.signum() < 0) {
            if (x.signum() != 0) {
                // return PyFloat.__pow__(__float__(left), right, modulo);
                throw new MissingFeature("float.__pow__");
            } else {
                throw new ZeroDivisionError("zero to a negative power");
            }
        }

        if (modulo == null || modulo == Py.None) {
            return toInt(x.pow(y.intValue()));

        } else {
            /*
             * XXX The original contains comments about alleged bugs in Sun Java
             * 1.1. These must be very old. The bugs may have been fixed and the
             * work-around removed. Or is may not have been a bug since Java
             * definitions of % differs from Python's.
             */
            // This whole thing can be trivially rewritten after bugs
            // in modPow are fixed by SUN
            BigInteger z = toBig(modulo);
            // Identify some special cases for quick treatment
            if (z.signum() == 0) {
                throw new ValueError("pow(x, y, z) with z == 0");
            } else if (z.abs().equals(ONE)) {
                return 0;
            } else if (z.signum() < 0) {
                // Handle negative modulo specially
                y = x.modPow(y, z.negate());
                if (y.signum() > 0) {
                    return toInt(z.add(y));
                } else {
                    return toInt(y);
                }
                // return __pow__(right).__mod__(modulo);
            } else {
                // XXX: 1.1 no longer supported so review this.
                // This is buggy in SUN's jdk1.1.5
                // Extra __mod__ improves things slightly
                return toInt(x.modPow(y, z));
                // return __pow__(right).__mod__(modulo);
            }
        }
    }

    /*

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.long___lshift___doc)
     */
    static Object __lshift__(Object self, Object right) {
        try {
            return toInt(toBig(self).shiftLeft(toShift(right)));
        } catch (NoConversion e) {
            return Py.NotImplemented;
        }
    }

    /*

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.long___rlshift___doc)
     */
    static Object __rlshift__(Object self, Object left) {
        try {
            return toInt(toBig(left).shiftLeft(toShift(self)));
        } catch (NoConversion e) {
            return Py.NotImplemented;
        }
    }

    /*

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.long___rshift___doc)
     */
    static Object __rshift__(Object self, Object right) {
        try {
            return toInt(toBig(self).shiftRight(toShift(right)));
        } catch (NoConversion e) {
            return Py.NotImplemented;
        }
    }

    /*

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.long___rrshift___doc)
     */
    static Object __rrshift__(Object self, Object left) {
        try {
            return toInt(toBig(left).shiftRight(toShift(self)));
        } catch (NoConversion e) {
            return Py.NotImplemented;
        }
    }

    /*

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.long___and___doc)
     */
    static Object __and__(Object self, Object right) {
        try {
            return toInt(toBig(self).and(toBig(right)));
        } catch (NoConversion e) {
            return Py.NotImplemented;
        }
    }

    /*

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.long___rand___doc)
     */
    static Object __rand__(Object self, Object left) {
        try {
            return toInt(toBig(left).and(toBig(self)));
        } catch (NoConversion e) {
            return Py.NotImplemented;
        }
    }

    /*

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.long___xor___doc)
     */
    static Object __xor__(Object self, Object right) {
        try {
            return toInt(toBig(self).xor(toBig(right)));
        } catch (NoConversion e) {
            return Py.NotImplemented;
        }
    }

    /*

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.long___rxor___doc)
     */
    static Object __rxor__(Object self, Object left) {
        try {
            return toInt(toBig(left).xor(toBig(self)));
        } catch (NoConversion e) {
            return Py.NotImplemented;
        }
    }

    /*

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.long___or___doc)
     */
    static Object __or__(Object self, Object right) {
        try {
            return toInt(toBig(self).or(toBig(right)));
        } catch (NoConversion e) {
            return Py.NotImplemented;
        }
    }

    /*

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.long___ror___doc)
     */
    static Object __ror__(Object self, Object left) {
        try {
            return toInt(toBig(left).or(toBig(self)));
        } catch (NoConversion e) {
            return Py.NotImplemented;
        }
    }

    /*

    @ExposedMethod(doc = BuiltinDocs.long___neg___doc)
     */
    static Object __neg__(Object self) {
        try {
            return toInt(toBig(self).negate());
        } catch (NoConversion e) {
            throw impossible(self);
        }
    }

    /*

    @ExposedMethod(doc = BuiltinDocs.long___pos___doc)
     */
    static Object __pos__(Object self) { return __int__(self); }

    /*

    @ExposedMethod(doc = BuiltinDocs.long___abs___doc)
     */
    static Object __abs__(Object self) {
        try {
            return toInt(toBig(self).abs());
        } catch (NoConversion e) {
            throw impossible(self);
        }
    }

    /*

    @ExposedMethod(doc = BuiltinDocs.long___invert___doc)
     */
    static Object __invert__(Object self) {
        try {
            return toInt(toBig(self).not());
        } catch (NoConversion e) {
            throw impossible(self);
        }
    }

    /*

    @ExposedMethod(doc = BuiltinDocs.long___int___doc)
     */
    static Object __int__(Object self) {
        try {
            // Guarantee type is exactly int
            return PyType.of(self) == TYPE ? self : toBig(self);
        } catch (NoConversion e) {
            throw impossible(self);
        }
    }

    /*

    @ExposedMethod(doc = BuiltinDocs.long___float___doc)
     */
    static Object __float__(Object self) {
        try {
            return convertToDouble(self);
        } catch (NoConversion e) {
            throw impossible(self);
        }
    }

    /*

    @ExposedMethod(doc = BuiltinDocs.long___trunc___doc)
     */
    static Object __trunc__(Object self) { return __int__(self); }

    /*

    @ExposedMethod(doc = BuiltinDocs.long_conjugate_doc)
     */
    static Object conjugate(Object self) { return self; }

    static PyComplex __complex__(Object self) {
        try {
            return new PyComplex(convertToDouble(self), 0.);
        } catch (NoConversion e) {
            throw impossible(self);
        }
    }

    /*

    @ExposedMethod(doc = BuiltinDocs.long___str___doc)
     */
    static Object __str__(Object self) {
        try {
            return toBig(self).toString();
        } catch (NoConversion e) {
            throw impossible(self);
        }
    }

    static Object __repr__(Object self) {
        assert TYPE.check(self);
        return asBigInteger(self).toString();
    }

    /*

    @ExposedMethod(doc = BuiltinDocs.long___getnewargs___doc)
     */
    static PyTuple __getnewargs__(Object self) {
        try {
            return new PyTuple(toBig(self));
        } catch (NoConversion e) {
            throw impossible(self);
        }
    }

    /*

    @ExposedMethod(doc = BuiltinDocs.long___index___doc)
     */
    static Object __index__(Object self) { return __int__(self); }

    /*

    @ExposedMethod(doc = BuiltinDocs.long_bit_length_doc)
     */
    static int bit_length(Object self) {
        try {
            BigInteger v = toBig(self);
            if (v.signum() == -1) {
                v = v.negate();
            }
            return v.bitLength();
        } catch (NoConversion e) {
            throw impossible(self);
        }

    }

    /*

    @ExposedMethod(doc = BuiltinDocs.long___format___doc)
     */
    static Object __format__(Object self, Object formatSpec) {
        try {
            // Parse the specification, which must at least sub-class str in
            // Python
            if (!PyUnicode.TYPE.check(formatSpec)) {
                throw Abstract.argumentTypeError("__format__", 0, "str", formatSpec);
            }

            BigInteger value = toBig(self);
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
     * throw {@link NoConversion}).
     * <p>
     * If the method throws the special exception {@link NoConversion},
     * the caller must deal with it by throwing an appropriate Python
     * exception or taking an alternative course of action.
     *
     * @param v claimed {@code int}
     * @return converted to {@code BigInteger}
     * @throws NoConversion if {@code v} is not a Python {@code int}
     */
    static BigInteger convertToBigInteger(Object v)
            throws NoConversion {
        if (v instanceof BigInteger)
            return (BigInteger)v;
        else if (v instanceof Integer)
            return BigInteger.valueOf(((Integer)v).longValue());
        else if (v instanceof PyLong)
            return ((PyLong)v).value;
        else if (v instanceof Boolean)
            return (Boolean)v ? BigInteger.ONE : BigInteger.ZERO;
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

    private static InterpreterError impossible(Object self) {
        return Abstract.impossibleArgumentError("int", self);
    }
}
