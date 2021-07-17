// Copyright (c) Jython Developers
package org.python.core.stringlib;

import java.math.BigInteger;

import org.python.core.Py;
import org.python.core.PyInteger;
import org.python.core.PyLong;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.core.PySystemState;
import org.python.core.stringlib.InternalFormat.Spec;

/**
 * A class that provides the implementation of integer formatting. In a limited way, it acts like a
 * StringBuilder to which text and one or more numbers may be appended, formatted according to the
 * format specifier supplied at construction. These are ephemeral objects that are not, on their
 * own, thread safe.
 */
public class IntegerFormatter extends InternalFormat.Formatter {

    /**
     * Construct the formatter from a client-supplied buffer, to which the result will be appended,
     * and a specification. Sets {@link #mark} to the end of the buffer.
     *
     * @param result destination buffer
     * @param spec parsed conversion specification
     */
    public IntegerFormatter(StringBuilder result, Spec spec) {
        super(result, spec);
    }

    /**
     * Construct the formatter from a specification, allocating a buffer internally for the result.
     *
     * @param spec parsed conversion specification
     */
    public IntegerFormatter(Spec spec) {
        // Rule of thumb: big enough for 32-bit binary with base indicator 0b
        this(new StringBuilder(34), spec);
    }

    /*
     * Re-implement the text appends so they return the right type.
     */
    @Override
    public IntegerFormatter append(char c) {
        super.append(c);
        return this;
    }

    @Override
    public IntegerFormatter append(CharSequence csq) {
        super.append(csq);
        return this;
    }

    @Override
    public IntegerFormatter append(CharSequence csq, int start, int end) //
            throws IndexOutOfBoundsException {
        super.append(csq, start, end);
        return this;
    }

    /**
     * Format a {@link BigInteger}, which is the implementation type of Jython <code>long</code>,
     * according to the specification represented by this <code>IntegerFormatter</code>. The
     * conversion type, and flags for grouping or base prefix are dealt with here. At the point this
     * is used, we know the {@link #spec} is one of the integer types.
     *
     * @param value to convert
     * @return this object
     */
    @SuppressWarnings("fallthrough")
    public IntegerFormatter format(BigInteger value) {
        try {
            // Different process for each format type.
            switch (spec.type) {
                case 'd':
                case Spec.NONE:
                case 'u':
                case 'i':
                    // None format or d-format: decimal
                    format_d(value);
                    break;

                case 'x':
                    // hexadecimal.
                    format_x(value, false);
                    break;

                case 'X':
                    // HEXADECIMAL!
                    format_x(value, true);
                    break;

                case 'o':
                    // Octal.
                    format_o(value);
                    break;

                case 'b':
                    // Binary.
                    format_b(value);
                    break;

                case 'c':
                    // Binary.
                    format_c(value);
                    break;

                case 'n':
                    // Locale-sensitive version of d-format should be here.
                    format_d(value);
                    break;

                default:
                    // Should never get here, since this was checked in caller.
                    throw unknownFormat(spec.type, "long");
            }

            // If required to, group the whole-part digits.
            if (spec.grouping) {
                groupDigits(3, ',');
            }

            return this;

        } catch (OutOfMemoryError eme) {
            // Most probably due to excessive precision.
            throw precisionTooLarge("long");
        }
    }

    /**
     * Format the value as decimal (into {@link #result}). The option for mandatory sign is dealt
     * with by reference to the format specification.
     *
     * @param value to convert
     */
    void format_d(BigInteger value) {
        String number;
        if (value.signum() < 0) {
            // Negative value: deal with sign and base, and convert magnitude.
            negativeSign(null);
            number = value.negate().toString();
        } else {
            // Positive value: deal with sign, base and magnitude.
            positiveSign(null);
            number = value.toString();
        }
        appendNumber(number);
    }

    /**
     * Format the value as hexadecimal (into {@link #result}), with the option of using upper-case
     * or lower-case letters. The options for mandatory sign and for the presence of a base-prefix
     * "0x" or "0X" are dealt with by reference to the format specification.
     *
     * @param value to convert
     * @param upper if the hexadecimal should be upper case
     */
    void format_x(BigInteger value, boolean upper) {
        String base = upper ? "0X" : "0x";
        String number;
        if (value.signum() < 0) {
            // Negative value: deal with sign and base, and convert magnitude.
            negativeSign(base);
            number = toHexString(value.negate());
        } else {
            // Positive value: deal with sign, base and magnitude.
            positiveSign(base);
            number = toHexString(value);
        }
        // Append to result, case-shifted if necessary.
        if (upper) {
            number = number.toUpperCase();
        }
        appendNumber(number);
    }

    /**
     * Format the value as octal (into {@link #result}). The options for mandatory sign and for the
     * presence of a base-prefix "0o" are dealt with by reference to the format specification.
     *
     * @param value to convert
     */
    void format_o(BigInteger value) {
        String base = "0o";
        String number;
        if (value.signum() < 0) {
            // Negative value: deal with sign and base, and convert magnitude.
            negativeSign(base);
            number = toOctalString(value.negate());
        } else {
            // Positive value: deal with sign, base and magnitude.
            positiveSign(base);
            number = toOctalString(value);
        }
        // Append to result.
        appendNumber(number);
    }

    /**
     * Format the value as binary (into {@link #result}). The options for mandatory sign and for the
     * presence of a base-prefix "0b" are dealt with by reference to the format specification.
     *
     * @param value to convert
     */
    void format_b(BigInteger value) {
        String base = "0b";
        String number;
        if (value.signum() < 0) {
            // Negative value: deal with sign and base, and convert magnitude.
            negativeSign(base);
            number = toBinaryString(value.negate());
        } else {
            // Positive value: deal with sign, base and magnitude.
            positiveSign(base);
            number = toBinaryString(value);
        }
        // Append to result.
        appendNumber(number);
    }

    /**
     * Format the value as a character (into {@link #result}).
     *
     * @param value to convert
     */
    void format_c(BigInteger value) {
        // Limit is 256 if we're formatting for byte output, unicode range otherwise.
        BigInteger limit = bytes ? LIMIT_BYTE : LIMIT_UNICODE;
        if (value.signum() < 0 || value.compareTo(limit) >= 0) {
            throw Py.OverflowError("%c arg not in range(0x" + toHexString(limit) + ")");
        } else {
            result.appendCodePoint(value.intValue());
        }
    }

    // Limits used in format_c(BigInteger)
    private static final BigInteger LIMIT_UNICODE = BigInteger
            .valueOf(PySystemState.maxunicode + 1);
    private static final BigInteger LIMIT_BYTE = BigInteger.valueOf(256);

    /**
     * Format an integer according to the specification represented by this
     * <code>IntegerFormatter</code>. The conversion type, and flags for grouping or base prefix are
     * dealt with here. At the point this is used, we know the {@link #spec} is one of the integer
     * types.
     *
     * @param value to convert
     * @return this object
     */
    @SuppressWarnings("fallthrough")
    public IntegerFormatter format(int value) {
        try {
            // Scratch all instance variables and start = result.length().
            setStart();

            // Different process for each format type.
            switch (spec.type) {
                case 'd':
                case Spec.NONE:
                case 'u':
                case 'i':
                    // None format or d-format: decimal
                    format_d(value);
                    break;

                case 'x':
                    // hexadecimal.
                    format_x(value, false);
                    break;

                case 'X':
                    // HEXADECIMAL!
                    format_x(value, true);
                    break;

                case 'o':
                    // Octal.
                    format_o(value);
                    break;

                case 'b':
                    // Binary.
                    format_b(value);
                    break;

                case 'c':
                case '%':
                    // Binary.
                    format_c(value);
                    break;

                case 'n':
                    // Locale-sensitive version of d-format should be here.
                    format_d(value);
                    break;

                default:
                    // Should never get here, since this was checked in caller.
                    throw unknownFormat(spec.type, "integer");
            }

            // If required to, group the whole-part digits.
            if (spec.grouping) {
                groupDigits(3, ',');
            }

            return this;
        } catch (OutOfMemoryError eme) {
            // Most probably due to excessive precision.
            throw precisionTooLarge("integer");
        }
    }

    /**
     * Format the value as decimal (into {@link #result}). The option for mandatory sign is dealt
     * with by reference to the format specification.
     *
     * @param value to convert
     */
    void format_d(int value) {
        String number;
        if (value < 0) {
            // Negative value: deal with sign and base, and convert magnitude.
            negativeSign(null);
            // Here there is a special case for int min value due to wrapping, to avoid a double
            // negative sign being added see http://bugs.jython.org/issue2672
            // The string constant here is -Integer.MIN_VALUE
            number = value == Integer.MIN_VALUE ? "2147483648" : Integer.toString(-value);
        } else {
            // Positive value: deal with sign, base and magnitude.
            positiveSign(null);
            number = Integer.toString(value);
        }
        appendNumber(number);
    }

    /**
     * Format the value as hexadecimal (into {@link #result}), with the option of using upper-case
     * or lower-case letters. The options for mandatory sign and for the presence of a base-prefix
     * "0x" or "0X" are dealt with by reference to the format specification.
     *
     * @param value to convert
     * @param upper if the hexadecimal should be upper case
     */
    void format_x(int value, boolean upper) {
        String base = upper ? "0X" : "0x";
        String number;
        if (value < 0) {
            // Negative value: deal with sign and base, and convert magnitude.
            negativeSign(base);
            number = Integer.toHexString(-value);
        } else {
            // Positive value: deal with sign, base and magnitude.
            positiveSign(base);
            number = Integer.toHexString(value);
        }
        // Append to result, case-shifted if necessary.
        if (upper) {
            number = number.toUpperCase();
        }
        appendNumber(number);
    }

    /**
     * Format the value as octal (into {@link #result}). The options for mandatory sign and for the
     * presence of a base-prefix "0o" are dealt with by reference to the format specification.
     *
     * @param value to convert
     */
    void format_o(int value) {
        String base = "0o";
        String number;
        if (value < 0) {
            // Negative value: deal with sign and base, and convert magnitude.
            negativeSign(base);
            number = Integer.toOctalString(-value);
        } else {
            // Positive value: deal with sign, base and magnitude.
            positiveSign(base);
            number = Integer.toOctalString(value);
        }
        // Append to result.
        appendNumber(number);
    }

    /**
     * Format the value as binary (into {@link #result}). The options for mandatory sign and for the
     * presence of a base-prefix "0b" are dealt with by reference to the format specification.
     *
     * @param value to convert
     */
    void format_b(int value) {
        String base = "0b";
        String number;
        if (value < 0) {
            // Negative value: deal with sign and base, and convert magnitude.
            negativeSign(base);
            number = Integer.toBinaryString(-value);
        } else {
            // Positive value: deal with sign, base and magnitude.
            positiveSign(base);
            number = Integer.toBinaryString(value);
        }
        // Append to result.
        appendNumber(number);
    }

    /**
     * Format the value as a character (into {@link #result}).
     *
     * @param value to convert
     */
    void format_c(int value) {
        // Limit is 256 if we're formatting for byte output, unicode range otherwise.
        int limit = bytes ? 256 : PySystemState.maxunicode + 1;
        if (value < 0 || value >= limit) {
            throw Py.OverflowError("%c arg not in range(0x" + Integer.toHexString(limit) + ")");
        } else {
            result.appendCodePoint(value);
        }
    }

    /**
     * Append to {@link #result} buffer a sign (if one is specified for positive numbers) and, in
     * alternate mode, the base marker provided. The sign and base marker are together considered to
     * be the "sign" of the converted number, spanned by {@link #lenSign}. This is relevant when we
     * come to insert padding.
     *
     * @param base marker "0x" or "0X" for hex, "0o" for octal, "0b" for binary, "" or
     *            <code>null</code> for decimal.
     */
    final void positiveSign(String base) {
        // Does the format specify a sign for positive values?
        char sign = spec.sign;
        if (Spec.specified(sign) && sign != '-') {
            append(sign);
            lenSign = 1;
        }
        // Does the format call for a base prefix?
        if (base != null && spec.alternate) {
            append(base);
            lenSign += base.length();
        }
    }

    /**
     * Append to {@link #result} buffer a minus sign and, in alternate mode, the base marker
     * provided. The sign and base marker are together considered to be the "sign" of the converted
     * number, spanned by {@link #lenSign}. This is relevant when we come to insert padding.
     *
     * @param base marker ("0x" or "0X" for hex, "0" for octal, <code>null</code> or "" for decimal.
     */
    final void negativeSign(String base) {
        // Insert a minus sign unconditionally.
        append('-');
        lenSign = 1;
        // Does the format call for a base prefix?
        if (base != null && spec.alternate) {
            append(base);
            lenSign += base.length();
        }
    }

    /**
     * Append a string (number) to {@link #result} and set {@link #lenWhole} to its length .
     *
     * @param number to append
     */
    void appendNumber(String number) {
        lenWhole = number.length();
        append(number);
    }

    // For hex-conversion by lookup
    private static final String LOOKUP = "0123456789abcdef";

    /**
     * A more efficient algorithm for generating a hexadecimal representation of a byte array.
     * {@link BigInteger#toString(int)} is too slow because it generalizes to any radix and,
     * consequently, is implemented using expensive mathematical operations.
     *
     * @param value the value to generate a hexadecimal string from
     * @return the hexadecimal representation of value, with "-" sign prepended if necessary
     */
    private static final String toHexString(BigInteger value) {
        int signum = value.signum();

        // obvious shortcut
        if (signum == 0) {
            return "0";
        }

        // we want to work in absolute numeric value (negative sign is added afterward)
        byte[] input = value.abs().toByteArray();
        StringBuilder sb = new StringBuilder(input.length * 2);

        int b;
        for (int i = 0; i < input.length; i++) {
            b = input[i] & 0xFF;
            sb.append(LOOKUP.charAt(b >> 4));
            sb.append(LOOKUP.charAt(b & 0x0F));
        }

        // before returning the char array as string, remove leading zeroes, but not the last one
        String result = sb.toString().replaceFirst("^0+(?!$)", "");
        return signum < 0 ? "-" + result : result;
    }

    /**
     * A more efficient algorithm for generating an octal representation of a byte array.
     * {@link BigInteger#toString(int)} is too slow because it generalizes to any radix and,
     * consequently, is implemented using expensive mathematical operations.
     *
     * @param value the value to generate an octal string from
     * @return the octal representation of value, with "-" sign prepended if necessary
     */
    private static final String toOctalString(BigInteger value) {
        int signum = value.signum();

        // obvious shortcut
        if (signum == 0) {
            return "0";
        }

        byte[] input = value.abs().toByteArray();
        if (input.length < 3) {
            return value.toString(8);
        }

        StringBuilder sb = new StringBuilder(input.length * 3);

        // working backwards, three bytes at a time
        int threebytes;
        int trip1, trip2, trip3;    // most, middle, and least significant bytes in the triplet
        for (int i = input.length - 1; i >= 0; i -= 3) {
            trip3 = input[i] & 0xFF;
            trip2 = ((i - 1) >= 0) ? (input[i - 1] & 0xFF) : 0x00;
            trip1 = ((i - 2) >= 0) ? (input[i - 2] & 0xFF) : 0x00;
            threebytes = trip3 | (trip2 << 8) | (trip1 << 16);

            // convert the three-byte value into an eight-character octal string
            for (int j = 0; j < 8; j++) {
                sb.append(LOOKUP.charAt((threebytes >> (j * 3)) & 0x000007));
            }
        }

        String result = sb.reverse().toString().replaceFirst("^0+(?!%)", "");
        return signum < 0 ? "-" + result : result;
    }

    /**
     * A more efficient algorithm for generating a binary representation of a byte array.
     * {@link BigInteger#toString(int)} is too slow because it generalizes to any radix and,
     * consequently, is implemented using expensive mathematical operations.
     *
     * @param value the value to generate a binary string from
     * @return the binary representation of value, with "-" sign prepended if necessary
     */
    private static final String toBinaryString(BigInteger value) {
        int signum = value.signum();

        // obvious shortcut
        if (signum == 0) {
            return "0";
        }

        // we want to work in absolute numeric value (negative sign is added afterward)
        byte[] input = value.abs().toByteArray();
        StringBuilder sb = new StringBuilder(value.bitCount());

        int b;
        for (int i = 0; i < input.length; i++) {
            b = input[i] & 0xFF;
            for (int bit = 7; bit >= 0; bit--) {
                sb.append(((b >> bit) & 0x1) > 0 ? "1" : "0");
            }
        }

        // before returning the char array as string, remove leading zeroes, but not the last one
        String result = sb.toString().replaceFirst("^0+(?!$)", "");
        return signum < 0 ? "-" + result : result;
    }

    /** Format specification used by bin(). */
    public static final Spec BIN = InternalFormat.fromText("#b");

    /** Format specification used by oct(). */
    public static final Spec OCT = InternalFormat.fromText("#o");

    /** Format specification used by hex(). */
    public static final Spec HEX = InternalFormat.fromText("#x");

    /**
     * Convert the object to binary according to the conventions of Python built-in
     * <code>bin()</code>. The object's __index__ method is called, and is responsible for raising
     * the appropriate error (which the base {@link PyObject#__index__()} does).
     *
     * @param number to convert
     * @return PyString converted result
     */
    // Follow this pattern in Python 3, where objects no longer have __hex__, __oct__ members.
    public static PyString bin(PyObject number) {
        return formatNumber(number, BIN);
    }

    /**
     * Convert the object according to the conventions of Python built-in <code>hex()</code>, or
     * <code>oct()</code>. The object's <code>__index__</code> method is called, and is responsible
     * for raising the appropriate error (which the base {@link PyObject#__index__()} does).
     *
     * @param number to convert
     * @return PyString converted result
     */
    public static PyString formatNumber(PyObject number, Spec spec) {
        number = number.__index__();
        IntegerFormatter f = new IntegerFormatter(spec);
        if (number instanceof PyInteger) {
            f.format(((PyInteger)number).getValue());
        } else {
            f.format(((PyLong)number).getValue());
        }
        return new PyString(f.getResult());
    }

    /**
     * A minor variation on {@link IntegerFormatter} to handle "traditional" %-formatting. The
     * difference is in support for <code>spec.precision</code>, the formatting octal in "alternate"
     * mode (0 and 0123, not 0o0 and 0o123), and in c-format (in the error logic).
     */
    public static class Traditional extends IntegerFormatter {

        /**
         * Construct the formatter from a client-supplied buffer, to which the result will be
         * appended, and a specification. Sets {@link #mark} to the end of the buffer.
         *
         * @param result destination buffer
         * @param spec parsed conversion specification
         */
        public Traditional(StringBuilder result, Spec spec) {
            super(result, spec);
        }

        /**
         * Construct the formatter from a specification, allocating a buffer internally for the
         * result.
         *
         * @param spec parsed conversion specification
         */
        public Traditional(Spec spec) {
            this(new StringBuilder(), spec);
        }

        /**
         * Format the value as octal (into {@link #result}). The options for mandatory sign and for
         * the presence of a base-prefix "0" are dealt with by reference to the format
         * specification.
         *
         * @param value to convert
         */
        @Override
        void format_o(BigInteger value) {
            String number;
            int signum = value.signum();
            if (signum < 0) {
                // Negative value: deal with sign and base, and convert magnitude.
                negativeSign(null);
                number = toOctalString(value.negate());
            } else {
                // Positive value: deal with sign, base and magnitude.
                positiveSign(null);
                number = toOctalString(value);
            }
            // Append to result.
            appendOctalNumber(number);
        }

        /**
         * Format the value as a character (into {@link #result}).
         *
         * @param value to convert
         */
        @Override
        void format_c(BigInteger value) {
            if (value.signum() < 0) {
                throw Py.OverflowError("unsigned byte integer is less than minimum");
            } else {
                // Limit is 256 if we're formatting for byte output, unicode range otherwise.
                BigInteger limit = bytes ? LIMIT_BYTE : LIMIT_UNICODE;
                if (value.compareTo(limit) >= 0) {
                    throw Py.OverflowError("unsigned byte integer is greater than maximum");
                } else {
                    result.appendCodePoint(value.intValue());
                }
            }
        }

        /**
         * Format the value as octal (into {@link #result}). The options for mandatory sign and for
         * the presence of a base-prefix "0" are dealt with by reference to the format
         * specification.
         *
         * @param value to convert
         */
        @Override
        void format_o(int value) {
            String number;
            if (value < 0) {
                // Negative value: deal with sign and convert magnitude.
                negativeSign(null);
                number = Integer.toOctalString(-value);
            } else {
                // Positive value: deal with sign, base and magnitude.
                positiveSign(null);
                number = Integer.toOctalString(value);
            }
            // Append to result.
            appendOctalNumber(number);
        }

        /**
         * Format the value as a character (into {@link #result}).
         *
         * @param value to convert
         */
        @Override
        void format_c(int value) {
            if (value < 0) {
                throw Py.OverflowError("unsigned byte integer is less than minimum");
            } else {
                // Limit is 256 if we're formatting for byte output, unicode range otherwise.
                int limit = bytes ? 256 : PySystemState.maxunicode + 1;
                if (value >= limit) {
                    throw Py.OverflowError("unsigned byte integer is greater than maximum");
                } else {
                    result.appendCodePoint(value);
                }
            }
        }

        /**
         * Append a string (number) to {@link #result}, but insert leading zeros first in order
         * that, on return, the whole-part length #lenWhole should be no less than the precision.
         *
         * @param number to append
         */
        @Override
        void appendNumber(String number) {
            int n, p = spec.getPrecision(0);
            for (n = number.length(); n < p; n++) {
                result.append('0');
            }
            lenWhole = n;
            append(number);
        }

        /**
         * Append a string (number) to {@link #result}, but insert leading zeros first in order
         * that, on return, the whole-part length #lenWhole should be no less than the precision.
         * Octal numbers must begin with a zero if <code>spec.alternate==true</code>, so if the
         * number passed in does not start with a zero, at least one will be inserted.
         *
         * @param number to append
         */
        void appendOctalNumber(String number) {
            int n = number.length(), p = spec.getPrecision(0);
            if (spec.alternate && number.charAt(0) != '0' && n >= p) {
                p = n + 1;
            }
            for (; n < p; n++) {
                result.append('0');
            }
            lenWhole = n;
            append(number);
        }

    }
}
