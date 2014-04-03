package org.python.core.stringlib;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

import org.python.core.Py;
import org.python.core.util.ExtraMath;

/**
 * This class provides an approximate equivalent to corresponding parts of CPython's
 * "~/Objects/stringlib/formatter.h", by concentrating in one place the formatting capabilities of
 * built-in numeric types float and complex.
 */
public class Formatter {

    /**
     * Format a floating-point value according to a conversion specification (created by
     * {@link InternalFormatSpecParser#parse()}), the type of which must be one of
     * <code>{efgEFG%}</code>, including padding to width.
     *
     * @param value to convert
     * @param spec for a floating-point conversion
     * @return formatted result
     */
    public static String formatFloat(double value, InternalFormatSpec spec) {
        InternalFormatter f = new InternalFormatter(spec);
        String string = f.format(value);
        return spec.pad(string, '>', 0);
    }

    /**
     * Format a complex value according to a conversion specification (created by
     * {@link InternalFormatSpecParser#parse()}), the type of which must be one of
     * <code>{efgEFG}</code>, including padding to width. The operation is effectively the
     * application of the floating-point format to the real an imaginary parts, then the addition of
     * padding once.
     *
     * @param value to convert
     * @param spec for a floating-point conversion
     * @return formatted result
     */
    public static String formatComplex(double real, double imag, InternalFormatSpec spec) {
        String string;
        InternalFormatter f = new InternalFormatter(spec);
        String r = f.format(real);
        String i = f.format(imag);
        if (i.charAt(0) == '-') {
            string = r + i + "j";
        } else {
            string = r + "+" + i + "j";
        }
        return spec.pad(string, '>', 0);
    }
}


/**
 * A class that provides the implementation of floating-point formatting, and holds a conversion
 * specification (created by {@link InternalFormatSpecParser#parse()}), a derived precision, and the
 * sign of the number being converted.
 */
// Adapted from PyString's StringFormatter class.
final class InternalFormatter {

    InternalFormatSpec spec;
    boolean negative;
    int precision;

    /**
     * Construct the formatter from a specification: default missing {@link #precision} to 6.
     *
     * @param spec parsed conversion specification
     */
    public InternalFormatter(InternalFormatSpec spec) {
        this.spec = spec;
        this.precision = spec.precision;
        if (this.precision == -1) {
            this.precision = 6;
        }
    }

    /**
     * If {@link #precision} exceeds an implementation limit, raise {@link Py#OverflowError}.
     *
     * @param type to name as the type being formatted
     */
    private void checkPrecision(String type) {
        if (precision > 250) {
            // A magic number. Larger than in CPython.
            throw Py.OverflowError("formatted " + type + " is too long (precision too long?)");
        }

    }

    /**
     * Format <code>abs(e)</code> (in the given radix) with zero-padding to 2 decimal places, and
     * store <code>sgn(e)</code> in {@link #negative}.
     *
     * @param e to convert
     * @param radix in which to express
     * @return string value of <code>abs(e)</code> base <code>radix</code>.
     */
    private String formatExp(long e, int radix) {
        checkPrecision("integer");
        if (e < 0) {
            negative = true;
            e = -e;
        }
        String s = Long.toString(e, radix);
        while (s.length() < 2) {
            s = "0" + s;
        }
        return s;
    }

    /**
     * Holds in its {@link #template} member, a {@link DecimalFormat} initialised for fixed point
     * float formatting.
     */
    static class DecimalFormatTemplate {

        static DecimalFormat template;
        static {
            template =
                    new DecimalFormat("#,##0.#####", new DecimalFormatSymbols(java.util.Locale.US));
            DecimalFormatSymbols symbols = template.getDecimalFormatSymbols();
            symbols.setNaN("nan");
            symbols.setInfinity("inf");
            template.setDecimalFormatSymbols(symbols);
            template.setGroupingUsed(false);
        }
    }

    /**
     * Return a copy of the pre-configured {@link DecimalFormatTemplate#template}, which may be
     * further customised by the client.
     *
     * @return the template
     */
    private static final DecimalFormat getDecimalFormat() {
        return (DecimalFormat)DecimalFormatTemplate.template.clone();
    }

    /**
     * Holds in its {@link #template} member, a {@link DecimalFormat} initialised for fixed point
     * float formatting with percentage scaling and furniture.
     */
    static class PercentageFormatTemplate {

        static DecimalFormat template;
        static {
            template =
                    new DecimalFormat("#,##0.#####%", new DecimalFormatSymbols(java.util.Locale.US));
            DecimalFormatSymbols symbols = template.getDecimalFormatSymbols();
            symbols.setNaN("nan");
            symbols.setInfinity("inf");
            template.setDecimalFormatSymbols(symbols);
            template.setGroupingUsed(false);
        }
    }

    /**
     * Return a copy of the pre-configured {@link PercentageFormatTemplate#template}, which may be
     * further customised by the client.
     *
     * @return the template
     */
    private static final DecimalFormat getPercentageFormat() {
        return (DecimalFormat)PercentageFormatTemplate.template.clone();
    }

    /**
     * Format <code>abs(v)</code> in <code>'{f}'</code> format to {@link #precision} (places after
     * decimal point), and store <code>sgn(v)</code> in {@link #negative}. Truncation is provided
     * for that will remove trailing zeros and the decimal point (e.g. <code>1.200</code> becomes
     * <code>1.2</code>, and <code>4.000</code> becomes <code>4</code>. This treatment is to support
     * <code>'{g}'</code> format. (Also potentially <code>'%g'</code> format.) Truncation is not
     * used (cannot validly be specified) for <code>'{f}'</code> format.
     *
     * @param v to convert
     * @param truncate if <code>true</code> strip trailing zeros and decimal point
     * @return converted value
     */
    private String formatFloatDecimal(double v, boolean truncate) {

        checkPrecision("decimal");

        // Separate the sign from v
        if (v < 0) {
            v = -v;
            negative = true;
        }

        // Configure a DecimalFormat: express truncation via minimumFractionDigits
        DecimalFormat decimalFormat = getDecimalFormat();
        decimalFormat.setMaximumFractionDigits(precision);
        decimalFormat.setMinimumFractionDigits(truncate ? 0 : precision);

        // The DecimalFormat is already configured to group by comma at group size 3.
        if (spec.thousands_separators) {
            decimalFormat.setGroupingUsed(true);
        }

        String ret = decimalFormat.format(v);
        return ret;
    }

    /**
     * Format <code>100*abs(v)</code> to {@link #precision} (places after decimal point), with a '%'
     * (percent) sign following, and store <code>sgn(v)</code> in {@link #negative}.
     *
     * @param v to convert
     * @param truncate if <code>true</code> strip trailing zeros
     * @return converted value
     */
    private String formatPercentage(double v, boolean truncate) {

        checkPrecision("decimal");

        // Separate the sign from v
        if (v < 0) {
            v = -v;
            negative = true;
        }

        // Configure a DecimalFormat: express truncation via minimumFractionDigits
        // XXX but truncation cannot be specified with % format!
        DecimalFormat decimalFormat = getPercentageFormat();
        decimalFormat.setMaximumFractionDigits(precision);
        decimalFormat.setMinimumFractionDigits(truncate ? 0 : precision);

        String ret = decimalFormat.format(v);
        return ret;
    }

    /**
     * Format <code>abs(v)</code> in <code>'{e}'</code> format to {@link #precision} (places after
     * decimal point), and store <code>sgn(v)</code> in {@link #negative}. Truncation is provided
     * for that will remove trailing zeros and the decimal point before the exponential part (e.g.
     * <code>1.200e+04</code> becomes <code>1.2e+04</code>, and <code>4.000e+05</code> becomes
     * <code>4e+05</code>. This treatment is to support <code>'{g}'</code> format. (Also potentially
     * <code>'%g'</code> format.) Truncation is not used (cannot validly be specified) for
     * <code>'{e}'</code> format.
     *
     * @param v to convert
     * @param truncate if <code>true</code> strip trailing zeros and decimal point
     * @return converted value
     */
    private String formatFloatExponential(double v, char e, boolean truncate) {

        // Separate the sign from v
        boolean isNegative = false;
        if (v < 0) {
            v = -v;
            isNegative = true;
        }

        /*
         * Separate treatment is given to the exponent (I think) because java.text.DecimalFormat
         * will insert a sign in a positive exponent, as in 1.234e+45 where Java writes 1.234E45.
         */

        // Power of 10 that will be the exponent.
        double power = 0.0;
        if (v > 0) {
            // That is, if not zero (or NaN)
            power = ExtraMath.closeFloor(Math.log10(v));
        }

        // Get exponent (as text)
        String exp = formatExp((long)power, 10);
        if (negative) {
            // This is the sign of the power-of-ten *exponent*
            negative = false;
            exp = '-' + exp;
        } else {
            exp = '+' + exp;
        }

        // Format the mantissa as a fixed point number
        double base = v / Math.pow(10, power);
        StringBuilder buf = new StringBuilder();
        buf.append(formatFloatDecimal(base, truncate));
        buf.append(e);

        buf.append(exp);
        negative = isNegative;

        return buf.toString();
    }

    /**
     * Format a floating-point number according to the specification represented by this
     * <code>InternalFormatter</code>. The conversion type, precision, and flags for grouping or
     * percentage are dealt with here. At the point this is used, we know the {@link #spec} has type
     * in <code>{efgEFG}</code>.
     *
     * @param value to convert
     * @return formatted version
     */
    @SuppressWarnings("fallthrough")
    public String format(double value) {

        // XXX Possible duplication in handling NaN and upper/lower case here when methiods
        // floatFormatDecimal, formatFloatExponential, etc. appear to do those things.

        String string;  // return value

        if (spec.alternate) {
            // XXX in %g, but not {:g} alternate form means always include a decimal point
            throw Py.ValueError("Alternate form (#) not allowed in float format specifier");
        }

        int sign = Double.compare(value, 0.0d);

        if (Double.isNaN(value)) {
            // Express NaN cased according to the conversion type.
            if (spec.type == 'E' || spec.type == 'F' || spec.type == 'G') {
                string = "NAN";
            } else {
                string = "nan";
            }

        } else if (Double.isInfinite(value)) {
            // Express signed infinity cased according to the conversion type.
            if (spec.type == 'E' || spec.type == 'F' || spec.type == 'G') {
                if (value > 0) {
                    string = "INF";
                } else {
                    string = "-INF";
                }
            } else {
                if (value > 0) {
                    string = "inf";
                } else {
                    string = "-inf";
                }
            }

        } else {

            switch (spec.type) {
                case 'e':
                case 'E':
                    // Exponential case: 1.23e-45
                    string = formatFloatExponential(value, spec.type, false);
                    if (spec.type == 'E') {
                        string = string.toUpperCase();
                    }
                    break;

                case 'f':
                case 'F':
                    // Fixed case: 123.45
                    string = formatFloatDecimal(value, false);
                    if (spec.type == 'F') {
                        string = string.toUpperCase();
                    }
                    break;

                case 'g':
                case 'G':
                    // Mixed "general" case: e or f format according to exponent.
                    // XXX technique not wholly effective, for example on 0.0000999999999999995.
                    int exponent =
                            (int)ExtraMath.closeFloor(Math.log10(Math.abs(value == 0 ? 1 : value)));
                    int origPrecision = precision;
                    /*
                     * (Python docs) Suppose formatting with presentation type 'e' and precision p-1
                     * would give exponent exp. Then if -4 <= exp < p, ...
                     */
                    if (exponent >= -4 && exponent < precision) {
                        /*
                         * ... the number is formatted with presentation type 'f' and precision
                         * p-1-exp.
                         */
                        precision -= exponent + 1;
                        string = formatFloatDecimal(value, !spec.alternate);
                    } else {
                        /*
                         * ... Otherwise, the number is formatted with presentation type 'e' and
                         * precision p-1.
                         */
                        precision--;
                        string =
                                formatFloatExponential(value, (char)(spec.type - 2),
                                        !spec.alternate);
                    }
                    if (spec.type == 'G') {
                        string = string.toUpperCase();
                    }
                    precision = origPrecision;
                    break;

                case '%':
                    // Multiplies by 100 and displays in f-format, followed by a percent sign.
                    string = formatPercentage(value, false);
                    break;

                default:
                    // Should never get here, since this was checked in PyFloat.
                    throw Py.ValueError(String.format(
                            "Unknown format code '%c' for object of type 'float'", spec.type));
            }
        }

        // If positive, deal with mandatory sign, or mandatory space.
        if (sign >= 0) {
            if (spec.sign == '+') {
                string = "+" + string;
            } else if (spec.sign == ' ') {
                string = " " + string;
            }
        }

        // If negative, insert a minus sign where needed, and we haven't already (e.g. "-inf").
        if (sign < 0 && string.charAt(0) != '-') {
            string = "-" + string;
        }
        return string;
    }
}
