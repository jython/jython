// Copyright (c)2026 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.core;

import static java.math.BigInteger.ONE;
import static java.math.BigInteger.ZERO;
import static org.python.core.ClassShorthand.T;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.math.BigInteger;

import org.python.core.PyUtil.NoConversion;
import org.python.internal.Formats;
import org.python.internal.Formats.AbstractFormatter;
import org.python.internal.Formats.FormatError;
import org.python.internal.Formats.FormatOverflow;
import org.python.internal.Formats.FormatSpec;
import org.python.internal.Formats.FormatUnknown;
import org.python.internal.IntegerFormatter;
import org.python.kernel.Representation;
import org.python.support.InterpreterError;
import org.python.types.Exposed;
import org.python.types.Exposed.DocString;
import org.python.types.Exposed.Default;
import org.python.types.Exposed.PositionalOnly;
import org.python.types.Exposed.PythonMethod;
import org.python.types.Exposed.PythonNewMethod;
import org.python.types.Feature;
import org.python.types.TypeSpec;
import org.python.types.WithClass;

/**
 * A Python {@code int} object may be represented by a
 * {@code java.lang.Integer} or a {@code java.math.BigInteger}. An
 * instance of a Python sub-class of {@code int}, must be represented by
 * an instance of (a Java sub-class of) this class.
 */
// TODO: adopt some more types of int
// TODO: implement PyDict.Key
public class PyLong implements /* PyDict.Key, */ WithClass {

    /** Only referenced during bootstrap by {@link TypeSystem}. */
    static class Spec {
        /** @return the type specification. */
        static TypeSpec get() {
            return new TypeSystem.BootstrapSpec("int", MethodHandles.lookup(), PyLong.class)
                    // .binopImpl(PyLongBinops.class) //
                    .add(Feature.BASETYPE) //
                    .methodImpls(PyLongMethods.class) //
                    .adopt(BigInteger.class, Integer.class) //
                    .accept(Boolean.class);
        }
    }

    /** The Python type of {@code int} objects. */
    public static final PyType TYPE = TypeSystem.TYPE_int;

    /** The value of this Python {@code int}. */
    // Has to be package visible for method implementations to see.
    final BigInteger value;

    /**
     * Constructor for subclass use.
     *
     * @param value of the {@code int}
     */
    protected PyLong(BigInteger value) { this.value = value; }

    @Override
    public PyType getType() { return TYPE; }

    // Constructor from Python ----------------------------------------

    /**
     * @param cls actual sub-type of {@code int} to produce
     * @param x {@code int}-like or {@code str}-like value or {@code None}.
     * @param base number base ({@code x} must be {@code str}-like) or
     *     {@code None}
     * @return an {@code int} or sub-class with the right value
     * @throws Throwable on argument type or other errors
     */
    @PythonNewMethod
    @DocString("""
            int([x]) -> integer
            int(x, base=10) -> integer

            Convert a number or string to an integer, or return 0 if no arguments
            are given.  If x is a number, return x.__int__().  For floating point
            numbers, this truncates towards zero.
            If x is not a number or if base is given, then x must be a string,
            bytes, or bytearray instance representing an integer literal in the
            given base.  The literal can be preceded by '+' or '-' and be surrounded
            by whitespace.  The base defaults to 10.  Valid bases are 0 and 2-36.
            Base 0 means interpret the base from the string as an integer literal.
            >>> int('0b100', base=0)
            4
            """)
    static Object __new__(PyType cls, @Default("None") @PositionalOnly Object x,
            @Default("None") Object base) throws Throwable {
        Object v = intImpl(x, base);
        if (cls == TYPE)
            return v;
        else
            /*
             * We need an instance of a Python subclass cls, which means creating an
             * instance of cls's Java representation.
             */
            try {
                // Look up a constructor with the right parameters
                MethodHandle cons = cls.constructor(T, BigInteger.class).handle();
                return cons.invokeExact(cls, asBigInteger(v));
            } catch (Throwable e) {
                throw PyUtil.cannotConstructInstance(cls, TYPE, e);
            }
    }

    // Legacy Jython-2-ish code follows ...

    // @formatter:off
    /*
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
                throw PyErr.format(PyExc.TypeError, "int() takes at most %d arguments (%d given)", 2, argsLen);
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
     * /
    private static Object __new__impl(PyType subType, Object x, Object obase) throws Throwable {

        if (subType != TYPE) { return longSubtypeNew(subType, x, obase); }

        if (x == null) {
            // Zero-arg int() ... unless invalidly like int(base=10)
            if (obase != null) { throw PyErr.format(PyExc.TypeError, "int() missing string argument"); }
            return 0;
        }

        if (obase == null)
            return PyNumber.asLong(x);
        else {
            int base = PyNumber.asSize(obase, null);
            if (base != 0 && (base < 2 || base > 36))
                throw PyErr.format(PyExc.ValueError, "int() base must be >= 2 and <= 36, or 0");
            else if (PyUnicode.TYPE.check(x))
                return PyLong.fromUnicode(x, base);
            // else if ... support for bytes-like objects
            else
                throw PyErr.format(PyExc.TypeError, NON_STR_EXPLICIT_BASE);
        }
    }

   /**
     * Wimpy, slow approach to {@code __new__} calls for sub-types of
     * {@code int}, that will temporarily create a regular {@code int}
     * from the arguments.
     *
     * @throws Throwable on argument type or other errors
     * /
    private static Object longSubtypeNew(PyType subType, Object x, Object obase) throws Throwable {
        // Create a regular int from whatever arguments we got.
        Object v = __new__impl(TYPE, x, obase);
        // create a sub-type instance from the value in tmp
        return new PyLong.Derived(subType, PyLong.asBigInteger(v));
    }
    */
    // @formatter:on

    /**
     * Create an int from the arguments (not a sub-type).
     *
     * @param x {@code int}-like or {@code str}-like value or {@code None}.
     * @param base number base ({@code x} must be {@code str}-like) or
     *     {@code None}
     * @return an {@code int} with the right value
     * @throws Throwable on argument type or other errors
     */
    private static Object intImpl(Object x, Object base) throws Throwable {

        if (x == Py.None) {
            // Zero-arg int() ... unless invalidly like int(base=10)
            if (base != Py.None) {
                throw PyErr.format(PyExc.TypeError, "int() missing string argument");
            }
            return 0;

        } else if (base == Py.None) {
            return PyNumber.asLong(x);

        } else {
            int b = PyNumber.asSize(base, null);
            if (b != 0 && (b < 2 || b > 36)) {
                throw PyErr.format(PyExc.ValueError, "int() base must be >= 2 and <= 36, or 0");
            } else if (PyUnicode.TYPE.check(x)) {
                return PyLong.fromUnicode(x, b);
                // else if ... support for bytes-like objects
            } else {
                throw PyErr.format(PyExc.TypeError, NON_STR_EXPLICIT_BASE);
            }
        }
    }

    private static final String NON_STR_EXPLICIT_BASE =
            "int() can't convert non-string with explicit base";

    // Special methods ------------------------------------------------

    /*
     * These are exposed to Python (or should be) automatically by name. We
     * make special methods private final, because we can, but the exposer
     * makes them effectively public and non-final to Python.
     */

    @SuppressWarnings("unused")
    private static Object __repr__(Object self) {
        assert TYPE.check(self);
        return asBigInteger(self).toString();
    }

    // __str__: let object.__str__ handle it (calls __repr__)

    // public Object __tojava__(Class<?> c) {
    // // XXX something like this necessary in Jython 3 but what?
    // try {
    // if (c == Boolean.TYPE || c == Boolean.class) {
    // return Boolean
    // .valueOf(!getValue().equals(BigInteger.ZERO));
    // }
    // if (c == Byte.TYPE || c == Byte.class) {
    // return Byte.valueOf(
    // (byte)getLong(Byte.MIN_VALUE, Byte.MAX_VALUE));
    // }
    // if (c == Short.TYPE || c == Short.class) {
    // return Short.valueOf((short)getLong(Short.MIN_VALUE,
    // Short.MAX_VALUE));
    // }
    // if (c == Integer.TYPE || c == Integer.class) {
    // return Integer.valueOf((int)getLong(Integer.MIN_VALUE,
    // Integer.MAX_VALUE));
    // }
    // if (c == Long.TYPE || c == Long.class) {
    // return Long.valueOf(
    // getLong(Long.MIN_VALUE, Long.MAX_VALUE));
    // }
    // if (c == Float.TYPE || c == Double.TYPE || c == Float.class
    // || c == Double.class) {
    // return asDouble(this);
    // }
    // if (c == BigInteger.class || c == Number.class
    // || c == Object.class || c == Serializable.class) {
    // return getValue();
    // }
    // } catch (OverflowError e) {
    // throw new InternalError(e);
    // }
    // throw new MissingFeature("default __tojava__ behaviour for %s",
    // c.getSimpleName());
    // }

    // XXX __coerce__ and __coerce_ex not needed in Jython 3 ?

    // int methods ---------------------------------------------------

    // These are exposed to Python (or should be) via annotations.

    @Exposed.Getter("real")
    public static Object getReal(Object self) { return self; }

    @Exposed.Getter("imag")
    public static Object getImag(Object self) { return 0; }

    @Exposed.Getter("numerator")
    public static Object getNumerator(Object self) { return self; }

    @Exposed.Getter("denominator")
    public static Object getDenominator(Object self) { return 1; }

    @Exposed.PythonMethod
    static Object __trunc__(Object self) { return from(self); }

    @Exposed.PythonMethod
    static Object conjugate(Object self) { return from(self); }

    @Exposed.PythonMethod
    static PyTuple __getnewargs__(Object self) {
        assert TYPE.check(self);
        try {
            return PyTuple.of(convertToBigInteger(self));
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
            AbstractFormatter f;
            if ("efgEFG%".indexOf(spec.type) >= 0) {
                // These are floating-point formats
                f = new PyFloat.Formatter(spec);
            } else {
                f = new PyLong.Formatter(spec);
            }

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
            throw Abstract.impossibleArgumentError("integer", self);
        }
    }

    // Java API -----------------------------------------------------

    /*
     * These members are not exposed to Python but are used from Java in
     * support of other object implementations. (Public members will be
     * client API as this package is exported.)
     *
     * Note that instance methods do not define behaviour for the Python int
     * type in general, just for the PyLong representation (so mostly for
     * subclasses).
     */

    @Override
    public String toString() { return PyUtil.defaultToString(this); }

    // @Override
    // public boolean equals(Object obj) {
    // return PyDict.pythonEquals(this, obj);
    // }
    //
    // @Override
    // public int hashCode() throws PyBaseException {
    // // XXX or return value.hashCode() if not a sub-class?
    // return PyDict.pythonHash(this);
    // }

    /** The minimum Java {@code int} as a {@code BigInteger}. */
    static final BigInteger MIN_INT = BigInteger.valueOf(Integer.MIN_VALUE);
    /** The maximum Java {@code int} as a {@code BigInteger}. */
    static final BigInteger MAX_INT = BigInteger.valueOf(Integer.MAX_VALUE);
    /** The minimum Java {@code long} as a {@code BigInteger}. */
    static final BigInteger MIN_LONG = BigInteger.valueOf(Long.MIN_VALUE);
    /** The maximum Java {@code long} as a {@code BigInteger}. */
    static final BigInteger MAX_LONG = BigInteger.valueOf(Long.MAX_VALUE);

    /*
     * These conversion methods are public API. They convert a Python int,
     * or a specific Java implementation of int, to a specified Java type.
     */

    /**
     * Present the argument as a Java {@code int} when it is expected to be
     * a Python {@code int} or a sub-class of it.
     *
     * @param v claimed {@code int}
     * @return {@code int} value
     * @throws PyBaseException ({@link PyExc#TypeError TypeError}) if
     *     {@code v} is not a Python {@code int}
     * @throws PyBaseException ({@link PyExc#OverflowError OverflowError})
     *     if {@code v} is out of Java range
     */
    public static int asInt(Object v) throws PyBaseException {
        try {
            return convertToInt(v);
        } catch (NoConversion nc) {
            throw Abstract.requiredTypeError("an integer", v);
        }
    }

    /**
     * Present the argument as a Java {@code int} when it is expected to be
     * a Python {@code int} or a sub-class of it.
     *
     * @param v claimed {@code int}
     * @return {@code int} value
     * @throws PyBaseException ({@link PyExc#TypeError TypeError}) if
     *     {@code v} is not a Python {@code int}
     * @throws PyBaseException ({@link PyExc#OverflowError OverflowError})
     *     if {@code v} is out of Java range
     */
    public static int asSize(Object v) throws PyBaseException { return asInt(v); }

    /**
     * Present the argument as a Java {@code BigInteger} when it is expected
     * to be a Python {@code int} or a sub-class of it.
     *
     * @param v claimed {@code int}
     * @return {@code BigInteger} value
     * @throws PyBaseException ({@link PyExc#TypeError TypeError}) if
     *     {@code v} is not a Python {@code int}
     */
    public static BigInteger asBigInteger(Object v) throws PyBaseException {
        try {
            return convertToBigInteger(v);
        } catch (NoConversion nc) {
            throw Abstract.requiredTypeError("an integer", v);
        }
    }

    /**
     * Present the argument as a Java {@code double} when it is expected to
     * be a Python {@code int} or a sub-class of it, using the
     * round-half-to-even rule.
     *
     * @param v to convert
     * @return nearest double
     * @throws PyBaseException ({@link PyExc#TypeError TypeError}) if
     *     {@code v} is not a Python {@code int}
     * @throws PyBaseException ({@link PyExc#OverflowError OverflowError})
     *     if out of double range
     */
    // Compare CPython longobject.c: PyLong_AsDouble
    static double asDouble(Object v) throws PyBaseException {
        try {
            return convertToDouble(v);
        } catch (NoConversion nc) {
            throw Abstract.requiredTypeError("an integer", v);
        }
    }

    /**
     * {@code signum(v)} <i>= -1, 0, 1</i> as <i>v&lt;0</i>, <i>v=0</i>,
     * <i>v&gt;0</i>.
     *
     * @param v of which the sign/sense is required
     * @return {@code signum(v)}
     * @throws PyBaseException ({@link PyExc#TypeError TypeError}) if
     *     {@code v} is not a Python {@code int}
     */
    static int signum(Object v) throws PyBaseException {
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
     * These methods create Python int from other Python objects, or from
     * specific Java types. The methods make use of special methods on the
     * argument and produce Python exceptions when that goes wrong. Note
     * that they never produce a PyLong, but always Java Integer or
     * BigInteger. They often correspond to CPython public or internal API.
     */

    // deleted: fromIntOf ~ _PyLong_FromNbInt gone from v3.11
    // deleted: fromIndexOrIntOf ~ _PyLong_FromNbIndexOrNbInt too

    /**
     * Convert a sequence of Unicode digits in the string u to a Python
     * integer value.
     *
     * @param u string to convert
     * @param base in which to interpret it
     * @return converted value
     * @throws PyBaseException ({@link PyExc#ValueError ValueError}) if
     *     {@code u} is an invalid literal
     * @throws PyBaseException ({@link PyExc#TypeError TypeError}) if
     *     {@code u} is not a Python {@code str}
     */
    // Compare CPython longobject.c :: PyLong_FromUnicodeObject
    static BigInteger fromUnicode(Object u, int base) throws PyBaseException {
        try {
            // XXX maybe check 2<=base<=36 even if Number.asLong does?
            String value = PyUnicode.asString(u);
            return new BigInteger(value, base);
        } catch (NumberFormatException e) {
            throw PyErr.format(PyExc.ValueError, "invalid literal for int() with base %d: %.200s",
                    base, u);
        }
    }

    /**
     * Return a Python {@code int} from a Python {@code int} or subclass. If
     * the value has exactly the Python type {@code int} return it,
     * otherwise construct a new instance of exactly {@code int} type.
     *
     * @param value to represent
     * @return the same value as exactly {@code int}
     * @throws PyBaseException ({@link PyExc#TypeError TypeError}) if not a
     *     Python {@code int} or sub-class
     */
    // Compare CPython longobject.c :: long_long
    static Object from(Object value) throws PyBaseException {
        Representation ops = Abstract.representation(value);
        if (ops.isIntExact())
            return value;
        else if (value instanceof PyLong)
            return ((PyLong)value).value;
        else
            throw Abstract.requiredTypeError("an integer", value);
    }

    /**
     * Create a Python {@code int} from a Java {@code double}.
     *
     * @param value to convert
     * @return BigInteger equivalent.
     * @throws PyBaseException ({@link PyExc#OverflowError OverflowError})
     *     when {@code value} is a floating infinity
     * @throws PyBaseException ({@link PyExc#ValueError ValueError}) when
     *     {@code value} is a floating NaN
     */
    // Compare CPython longobject.c :: PyLong_FromDouble
    static BigInteger fromDouble(double value) {
        // XXX Maybe return Object and Integer if possible
        return PyFloat.bigIntegerFromDouble(value);
    }

    // Plumbing ------------------------------------------------------

    /*
     * These methods are for use internal to the core, including privately
     * here: they may throw non-Python exception like NoConversion on
     * failure, which must be caught by those implementations. They convert
     * a Python int, or a specific Java implementation of int, to a specific
     * Java type.
     */

    /**
     * Convert an {@code int} to a Java {@code double} (or throw
     * {@link NoConversion}), using the round-half-to-even rule. Conversion
     * to a {@code double} may overflow, raising an exception that is
     * propagated to the caller.
     * <p>
     * If the method throws the special exception {@link NoConversion}, the
     * caller must deal with it by throwing an appropriate Python exception
     * or taking an alternative course of action. Binary operations will
     * normally return {@link Py#NotImplemented} in response.
     *
     * @param v to convert
     * @return converted to {@code double}
     * @throws NoConversion v is not an {@code int}
     * @throws PyBaseException ({@link PyExc#OverflowError OverflowError}) v
     *     is too large to be a {@code float}
     */
    // Compare CPython longobject.c: PyLong_AsDouble
    static double convertToDouble(Object v) throws NoConversion, PyBaseException {
        // Check against supported types, most likely first
        if (v instanceof Integer vi)
            // No loss of precision
            return vi.doubleValue();
        else if (v instanceof BigInteger vb)
            // Round half-to-even
            return convertToDouble(vb);
        else if (v instanceof PyLong vl)
            // Round half-to-even
            return convertToDouble(vl.value);
        else if (v instanceof Boolean vb)
            return vb ? 1.0 : 0.0;
        throw PyUtil.NO_CONVERSION;
    }

    /**
     * Convert a {@code BigInteger} to a Java double , using the
     * round-half-to-even rule. Conversion to a double may overflow, raising
     * an exception that is propagated to the caller.
     *
     * @param v to convert
     * @return converted to {@code double}
     * @throws PyBaseException ({@link PyExc#OverflowError OverflowError})
     *     if too large to be a {@code float}
     */
    static double convertToDouble(BigInteger v) throws PyBaseException {
        /*
         * According to the code, BigInteger.doubleValue() rounds half-to-even
         * as required. This differs from conversion from long which rounds to
         * nearest (JLS 3.0 5.1.2).
         */
        double vv = v.doubleValue();
        // On overflow, doubleValue returns ±∞ rather than throwing.
        if (Double.isInfinite(vv))
            throw tooLarge("Python int", "double");
        else
            return vv;
    }

    /**
     * Convert a Python {@code int} to a Java {@code int} (or throw
     * {@link NoConversion}). Conversion to an {@code int} may overflow,
     * raising an exception that is propagated to the caller.
     * <p>
     * If the method throws the special exception {@link NoConversion}, the
     * caller must deal with it by throwing an appropriate Python exception
     * or taking an alternative course of action.
     *
     * @param v to convert
     * @return converted to {@code int}
     * @throws NoConversion v is not an {@code int}
     * @throws PyBaseException ({@link PyExc#OverflowError OverflowError}) v
     *     is too large to be a Java {@code int}
     */
    // Compare CPython longobject.c: PyLong_AsSsize_t
    static int convertToInt(Object v) throws NoConversion, PyBaseException {
        // Check against supported types, most likely first
        if (v instanceof Integer vi)
            return vi.intValue();
        else if (v instanceof BigInteger vb)
            return convertToInt(vb);
        else if (v instanceof PyLong vl)
            return convertToInt(vl.value);
        else if (v instanceof Boolean vb)
            return vb ? 1 : 0;
        throw PyUtil.NO_CONVERSION;
    }

    /**
     * Convert a {@code BigInteger} to a Java {@code int}. Conversion to an
     * {@code int} may overflow, raising an exception that is propagated to
     * the caller.
     *
     * @param v to convert
     * @return converted to {@code int}
     * @throws PyBaseException ({@link PyExc#OverflowError OverflowError})
     *     if too large to be a Java {@code int}
     */
    static int convertToInt(BigInteger v) throws PyBaseException {
        if (v.bitLength() < 32)
            return v.intValue();
        else
            throw tooLarge("Python int", "int");
    }

    /**
     * Convert a Python {@code int} to a Java {@code long} (or throw
     * {@link NoConversion}). Conversion to an {@code long} may overflow,
     * raising an exception that is propagated to the caller.
     * <p>
     * If the method throws the special exception {@link NoConversion}, the
     * caller must deal with it by throwing an appropriate Python exception
     * or taking an alternative course of action.
     *
     * @param v to convert
     * @return converted to {@code long}
     * @throws NoConversion v is not an {@code int}
     * @throws PyBaseException ({@link PyExc#OverflowError OverflowError}) v
     *     is too large to be a Java {@code long}
     */
    // Compare CPython longobject.c: PyLong_AsLong
    static long convertToLong(Object v) throws NoConversion, PyBaseException {
        // Check against supported types, most likely first
        if (v instanceof Integer vi)
            return vi.longValue();
        else if (v instanceof BigInteger vb)
            return convertToLong(vb);
        else if (v instanceof PyLong vl)
            return convertToLong(vl.value);
        else if (v instanceof Boolean vb)
            return vb ? 1L : 0L;
        throw PyUtil.NO_CONVERSION;
    }

    /**
     * Convert a {@code BigInteger} to a Java {@code long}. Conversion to an
     * {@code long} may overflow, raising an exception that is propagated to
     * the caller.
     *
     * @param v to convert
     * @return converted to {@code long}
     * @throws PyBaseException ({@link PyExc#OverflowError OverflowError})
     *     if too large to be a Java {@code long}
     */
    static long convertToLong(BigInteger v) throws PyBaseException {
        if (v.bitLength() < 64)
            return v.longValue();
        else
            throw tooLarge("Python int", "long");
    }

    /**
     * Convert a Python {@code int} to a Java {@code BigInteger} (or throw
     * {@link NoConversion}). Conversion may raise an exception that is
     * propagated to the caller. If the Java type of the {@code int} is
     * declared, generally there is a better option than this method. We
     * only use it for {@code Object} arguments.
     * <p>
     * If the method throws the special exception {@link NoConversion}, the
     * caller must deal with it by throwing an appropriate Python exception
     * or taking an alternative course of action.
     *
     * @param v claimed {@code int}
     * @return converted to {@code BigInteger}
     * @throws NoConversion if {@code v} is not a Python {@code int}
     */
    static BigInteger convertToBigInteger(Object v) throws NoConversion {
        if (v instanceof BigInteger vb)
            return vb;
        else if (v instanceof Integer vi)
            return BigInteger.valueOf(vi.longValue());
        else if (v instanceof PyLong vl)
            return vl.value;
        else if (v instanceof Boolean vb)
            return vb ? ONE : ZERO;
        throw PyUtil.NO_CONVERSION;
    }

    /**
     * Create an {@link PyBaseException OverflowError} with a message along
     * the lines "X too large to convert to Y", where X is {@code from} and
     * Y is {@code to}.
     *
     * @param from description of type to convert from
     * @param to description of type to convert to
     * @return an {@code OverflowError} with that message
     */
    static PyBaseException tooLarge(String from, String to) {
        String msg = String.format(TOO_LARGE, from, to);
        return PyErr.format(PyExc.OverflowError, msg);
    }

    private static final String TOO_LARGE = "%s too large to convert to Java %s";

    /**
     * We received an argument that should be impossible in a correct
     * interpreter. We use this when conversion of an {@code Object self}
     * argument may theoretically fail, but we know that we should only
     * reach that point by paths that guarantee {@code self`} to be some
     * kind on {@code int}.
     *
     * @param o actual argument
     * @return exception to throw
     */
    private static InterpreterError impossible(Object o) {
        return Abstract.impossibleArgumentError("int", o);
    }

    // formatter ------------------------------------------------------

    /**
     * An {@link IntegerFormatter}, constructed from a {@link FormatSpec},
     * with validations customised for {@code int.__format__}.
     */
    private static class Formatter extends IntegerFormatter {

        /**
         * Prepare an {@link IntegerFormatter} in support of
         * {@link PyLong#__format__(Object, Object) int.__format__}.
         *
         * @param spec a parsed PEP-3101 format specification.
         * @throws FormatError if an unsupported format is encountered
         */
        Formatter(FormatSpec spec) throws FormatError { super(validated(spec)); }

        /**
         * Validations and defaults specific to {@code int.__format__}. (Note
         * that %-formatting has slightly different rules.)
         *
         * @param spec to validate
         * @return validated spec with defaults filled
         * @throws FormatError on failure to validate
         */
        private static FormatSpec validated(FormatSpec spec) throws FormatError {
            switch (spec.type) {

                case 'c':
                    // Character data: specific prohibitions.
                    if (FormatSpec.specified(spec.sign)) {
                        throw signNotAllowed("integer", spec.type);
                    } else if (spec.alternate) {
                        throw alternateFormNotAllowed("integer", spec.type);
                    }
                    // $FALL-THROUGH$

                case 'n':
                    if (spec.group > 0) {
                        throw notAllowed("Grouping", spec.grouper, "integer", spec.type);
                    }
                    // $FALL-THROUGH$

                case 'b':
                case 'o':
                case 'x':
                case 'X':
                case FormatSpec.NONE:
                case 'd':
                    // Check for disallowed parts of the specification
                    if (FormatSpec.specified(spec.precision)) {
                        throw precisionNotAllowed("integer");
                    }
                    break;

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
        public IntegerFormatter format(Object o) throws NoConversion, FormatError {
            return format(convertToBigInteger(o));
        }
    }
}
