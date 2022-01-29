// Copyright (c)2021 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.python.core.PyObjectUtil.NO_CONVERSION;

import java.math.BigInteger;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.python.core.PyObjectUtil.NoConversion;
import org.python.core.stringlib.InternalFormat;
import org.python.core.stringlib.InternalFormat.FormatError;
import org.python.core.stringlib.InternalFormat.Spec;
import org.python.core.stringlib.TextFormatter;;

/**
 * Tests of formatting of built-in types to string using their __format__ method (on which built-in method {@code format()} relies)..
 * <p>
 * At present, we only have tests for {@code int.__format__}.
 */
class FormatTest extends UnitTestSupport {

    /** Base of tests that format integers. */
    abstract static class AbstractIntFormatTest {

        /**
         * The values corresponding to the expected results in the
         * stream of examples provided by {@link #intExamples()}. A
         * fragment of Python for generating this array is:<pre>
         * ival = [42, -42, 226, 128013, 2**31-1, -2**31,
         *            False, True, 2**36, -2**36, 7**42]
         *
         * def java_value(v):
         *     if isinstance(v, bool):
         *         return repr(v).lower()
         *     elif 2**31 &gt; v &gt;= -2**31:
         *         return repr(v)
         *     else:
         *         return f'new BigInteger("{v!r:s}")'
         *
         * print('static final Object[] VALUES = {',
         *       ', '.join(java_value(v) for v in ival), '};')
         * print()
         * </pre>
         */
        static final Object[] VALUES = {42, -42, 226, 128013,
                2147483647, -2147483648, false, true,
                new BigInteger("68719476736"),
                new BigInteger("-68719476736"),
                new BigInteger("311973482284542371301330321821976049")};

        /**
         * Provide a stream of examples as parameter sets to the tests.
         * In each example, one format has been used to format all the
         * {@link #VALUES}. A fragment of Python for generating these
         * calls is:<pre>
         * ifmt = ["d", "o", "x", "X", "b",
         *        ",d", "#o", "#X", "+d", " d",
         *        "5", "^6", "~&lt;5", "0=+6" ]
         *
         * def java_values_in_fmt(values, fmt):
         *     args = '", "'.join(format(v, fmt) for v in values)
         *     return f'intExample("{fmt:s}", "{args}"), //'
         *
         * for f in ifmt:
         *     print(java_values_in_fmt(ival, f))
         * </pre>
         *
         * @return the examples for search tests.
         */
        static Stream<Arguments> intExamples() {
            return Stream.of( //
                    intExample("d", "42", "-42", "226", "128013",
                            "2147483647", "-2147483648", "0", "1",
                            "68719476736", "-68719476736",
                            "311973482284542371301330321821976049"), //
                    intExample("o", "52", "-52", "342", "372015",
                            "17777777777", "-20000000000", "0", "1",
                            "1000000000000", "-1000000000000",
                            "1701257274030155626774437006214073142761"), //
                    intExample("x", "2a", "-2a", "e2", "1f40d",
                            "7fffffff", "-80000000", "0", "1",
                            "1000000000", "-1000000000",
                            "3c157af0306dcb7f23e06460ecc5f1"), //
                    intExample("X", "2A", "-2A", "E2", "1F40D",
                            "7FFFFFFF", "-80000000", "0", "1",
                            "1000000000", "-1000000000",
                            "3C157AF0306DCB7F23E06460ECC5F1"), //
                    intExample("b", "101010", "-101010", "11100010",
                            "11111010000001101",
                            "1111111111111111111111111111111",
                            "-10000000000000000000000000000000", "0",
                            "1",
                            "1000000000000000000000000000000000000",
                            "-1000000000000000000000000000000000000",
                            "1111000001010101111010111100000011000001101101110010110111111100100011111000000110010001100000111011001100010111110001"), //
                    intExample(",d", "42", "-42", "226", "128,013",
                            "2,147,483,647", "-2,147,483,648", "0", "1",
                            "68,719,476,736", "-68,719,476,736",
                            "311,973,482,284,542,371,301,330,321,821,976,049"), //
                    intExample("#o", "0o52", "-0o52", "0o342",
                            "0o372015", "0o17777777777",
                            "-0o20000000000", "0o0", "0o1",
                            "0o1000000000000", "-0o1000000000000",
                            "0o1701257274030155626774437006214073142761"), //
                    intExample("#X", "0X2A", "-0X2A", "0XE2", "0X1F40D",
                            "0X7FFFFFFF", "-0X80000000", "0X0", "0X1",
                            "0X1000000000", "-0X1000000000",
                            "0X3C157AF0306DCB7F23E06460ECC5F1"), //
                    intExample("+d", "+42", "-42", "+226", "+128013",
                            "+2147483647", "-2147483648", "+0", "+1",
                            "+68719476736", "-68719476736",
                            "+311973482284542371301330321821976049"), //
                    intExample(" d", " 42", "-42", " 226", " 128013",
                            " 2147483647", "-2147483648", " 0", " 1",
                            " 68719476736", "-68719476736",
                            " 311973482284542371301330321821976049"), //
                    intExample("5", "   42", "  -42", "  226", "128013",
                            "2147483647", "-2147483648", "    0",
                            "    1", "68719476736", "-68719476736",
                            "311973482284542371301330321821976049"), //
                    intExample("^6", "  42  ", " -42  ", " 226  ",
                            "128013", "2147483647", "-2147483648",
                            "  0   ", "  1   ", "68719476736",
                            "-68719476736",
                            "311973482284542371301330321821976049"), //
                    intExample("~<5", "42~~~", "-42~~", "226~~",
                            "128013", "2147483647", "-2147483648",
                            "0~~~~", "1~~~~", "68719476736",
                            "-68719476736",
                            "311973482284542371301330321821976049"), //
                    intExample("0=+6", "+00042", "-00042", "+00226",
                            "+128013", "+2147483647", "-2147483648",
                            "+00000", "+00001", "+68719476736",
                            "-68719476736",
                            "+311973482284542371301330321821976049"), //

                    // char formats (hand-crafted) --------------------
                    intExample("c", new Object[] {42, 226, 128013}, //
                            "*", "â", "🐍"));
        }

        /**
         * Construct a set of test arguments for a single format type
         * and a reference result for each value in {@link #VALUES},
         * provided by the caller. We convert reference results to
         * {@code PyUnicode} to ensure we get Python comparison
         * semantics.
         *
         * @param format to apply
         * @param expected results to expect
         * @return example data for a test
         */
        private static Arguments intExample(String format,
                String... expected) {
            return intExample(format, VALUES, expected);
        }

        /**
         * Construct a set of test arguments for a single format type
         * and a reference result for each value in {@code values},
         * provided by the caller. We convert reference results to
         * {@code PyUnicode} to ensure we get Python comparison
         * semantics.
         *
         * @param format to apply
         * @param values to apply {@code format} to
         * @param expected results to expect
         * @return example data for a test
         */
        private static Arguments intExample(String format,
                Object[] values, String... expected) {
            assert expected.length == values.length;
            PyUnicode[] uExpected = new PyUnicode[expected.length];
            for (int i = 0; i < expected.length; i++) {
                uExpected[i] = newPyUnicode(expected[i]);
            }
            return arguments(format, values, uExpected);
        }
    }

    /**
     * Test formatting an integer
     */
    @Nested
    @DisplayName("int.__format__")
    class IntFormatTest extends AbstractIntFormatTest {

        @DisplayName("int.__format__(int, String)")
        @ParameterizedTest(name = "int.__format__(x, \"{0}\")")
        @MethodSource("intExamples")
        void intFormat(String format, Object[] values,
                PyUnicode[] expected) {
            for (int i = 0; i < values.length; i++) {
                Object r = PyLong.__format__(values[i], format);
                assertEquals(expected[i], r);
            }
        }

        @DisplayName("int.__format__: unknown specifier")
        @ParameterizedTest(name = "int.__format__(x, \"{0}\")")
        @ValueSource(strings = {"z", "#10z"})
        void intFormatUnknown(String format) {
            assertRaises(ValueError.class,
                    () -> PyLong.__format__(0, format),
                    "Unknown format code 'z' for object of type 'int'");
        }

        @DisplayName("int.__format__: precision not allowed")
        @ParameterizedTest(name = "int.__format__(x, \"{0}\")")
        @ValueSource(strings = {"123.456x", ".5d", "0=+6.1"})
        void intFormatWithPrecision(String format) {
            assertRaises(ValueError.class,
                    () -> PyLong.__format__(0, format),
                    "Precision not allowed in integer format specifier");
        }

        @DisplayName("int.__format__: grouping not allowed")
        @ParameterizedTest(name = "int.__format__(x, \"{0}\")")
        @ValueSource(strings = {"10,n", ",n"})
        void intFormatNWithGrouping(String format) {
            assertRaises(ValueError.class,
                    () -> PyLong.__format__(0, format),
                    // CPython: "Cannot specify ',' with 'n'");
                    // But we prefer:
                    "Grouping (,) not allowed with integer format specifier 'n'");
        }

        @DisplayName("int.__format__: sign not allowed")
        @ParameterizedTest(name = "int.__format__(x, \"{0}\")")
        @ValueSource(strings = {"+c", "-c", "+10c"})
        void intFormatCWithSign(String format) {
            assertRaises(ValueError.class,
                    () -> PyLong.__format__(0, format),
                    "Sign not allowed with integer format specifier 'c'");
        }

        @DisplayName("int.__format__: alternate form not allowed")
        @ParameterizedTest(name = "int.__format__(x, \"{0}\")")
        @ValueSource(strings = {"#c", "#10c"})
        void intFormatCWithAltForm(String format) {
            assertRaises(ValueError.class,
                    () -> PyLong.__format__(0, format),
                    "Alternate form (#) not allowed with integer format specifier 'c'");
        }
    }

    public void strFormat() {
        String v = "abc";
        TextFormatter f = newTextFormatter("");
        assertEquals("abc", f.format(v).pad().getResult());

        String v2 = "abcdef";
        f = newTextFormatter(".3");
        assertEquals("abc", f.format(v2).pad().getResult());

        f = newTextFormatter("6");
        assertEquals("abc   ", f.format(v).pad().getResult());
    }

    private static TextFormatter newTextFormatter(String fmt) {
        Spec spec = InternalFormat.fromText(fmt);
        return new TextFormatter(spec) {
            @Override
            public TextFormatter format(Object o)
                    throws FormatError, NoConversion {
                if (o instanceof String) {
                    return format((String)o);
                } else if (o instanceof PyUnicode) {
                    return format(PyUnicode.asString(o));
                } else {
                    throw NO_CONVERSION;
                }
            }
        };
    }
}
