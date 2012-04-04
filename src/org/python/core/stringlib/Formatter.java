package org.python.core.stringlib;
import org.python.core.*;
import org.python.core.util.ExtraMath;

import java.math.BigInteger;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;


public class Formatter {
    public static String formatFloat(double value, InternalFormatSpec spec) {
        InternalFormatter f = new InternalFormatter(spec);
        return f.format(value);
    }
}

//Adapted from PyString's StringFormatter class.
final class InternalFormatter {
    InternalFormatSpec spec;
    boolean negative;
    int precision;

    public InternalFormatter(InternalFormatSpec spec) {
        this.spec = spec;
        this.precision = spec.precision;
        if (this.precision == -1)
            this.precision = 6;
    }

    private void checkPrecision(String type) {
        if(precision > 250) {
            // A magic number. Larger than in CPython.
            throw Py.OverflowError("formatted " + type + " is too long (precision too long?)");
        }
        
    }

    private String formatExp(long v, int radix) {
        checkPrecision("integer");
        if (v < 0) {
            negative = true;
            v = -v;
        }
        String s = Long.toString(v, radix);
        while (s.length() < 2) {
            s = "0"+s;
        }
        return s;
    }

    static class DecimalFormatTemplate {
        static DecimalFormat template;
        static {
            template = new DecimalFormat("#,##0.#####");
            DecimalFormatSymbols symbols = template.getDecimalFormatSymbols();
            symbols.setNaN("nan");
            symbols.setInfinity("inf");
            template.setDecimalFormatSymbols(symbols);
            template.setGroupingUsed(false);
        }
    }

    private static final DecimalFormat getDecimalFormat() {
        return (DecimalFormat)DecimalFormatTemplate.template.clone();
    }

    static class PercentageFormatTemplate {
        static DecimalFormat template;
        static {
            template = new DecimalFormat("#,##0.#####%");
            DecimalFormatSymbols symbols = template.getDecimalFormatSymbols();
            symbols.setNaN("nan");
            symbols.setInfinity("inf");
            template.setDecimalFormatSymbols(symbols);
            template.setGroupingUsed(false);
        }
    }

    private static final DecimalFormat getPercentageFormat() {
        return (DecimalFormat)PercentageFormatTemplate.template.clone();
    }

    private String formatFloatDecimal(double v, boolean truncate) {
        checkPrecision("decimal");
        if (v < 0) {
            v = -v;
            negative = true;
        }

        DecimalFormat decimalFormat = getDecimalFormat();
        decimalFormat.setMaximumFractionDigits(precision);
        decimalFormat.setMinimumFractionDigits(truncate ? 0 : precision);

        if (spec.thousands_separators) {
            decimalFormat.setGroupingUsed(true);
        }
        String ret = decimalFormat.format(v);
        return ret;
    }

    private String formatPercentage(double v, boolean truncate) {
        checkPrecision("decimal");
        if (v < 0) {
            v = -v;
            negative = true;
        }

        DecimalFormat decimalFormat = getPercentageFormat();
        decimalFormat.setMaximumFractionDigits(precision);
        decimalFormat.setMinimumFractionDigits(truncate ? 0 : precision);

        String ret = decimalFormat.format(v);
        return ret;
    }

    private String formatFloatExponential(double v, char e, boolean truncate) {
        StringBuilder buf = new StringBuilder();
        boolean isNegative = false;
        if (v < 0) {
            v = -v;
            isNegative = true;
        }
        double power = 0.0;
        if (v > 0)
            power = ExtraMath.closeFloor(Math.log10(v));
        String exp = formatExp((long)power, 10);
        if (negative) {
            negative = false;
            exp = '-'+exp;
        }
        else {
            exp = '+' + exp;
        }

        double base = v/Math.pow(10, power);
        buf.append(formatFloatDecimal(base, truncate));
        buf.append(e);

        buf.append(exp);
        negative = isNegative;

        return buf.toString();
    }

    @SuppressWarnings("fallthrough")
    public String format(double value) {
        String string;

        if (spec.alternate) {
            throw Py.ValueError("Alternate form (#) not allowed in float format specifier");
        }
        int sign = Double.compare(value, 0.0d);

        if (Double.isNaN(value)) {
            if (spec.type == 'E' || spec.type == 'F' || spec.type == 'G') {
                string = "NAN";
            } else {
                string = "nan";
            }
        } else if (Double.isInfinite(value)) {
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

            switch(spec.type) {
            case 'e':
            case 'E':
                string = formatFloatExponential(value, spec.type, false);
                if (spec.type == 'E') {
                    string = string.toUpperCase();
                }
                break;
            case 'f':
            case 'F':
                string = formatFloatDecimal(value, false);
                if (spec.type == 'F') {
                    string = string.toUpperCase();
                }
                break;
            case 'g':
            case 'G':
                int exponent = (int)ExtraMath.closeFloor(Math.log10(Math.abs(value == 0 ? 1 : value)));
                int origPrecision = precision;
                if (exponent >= -4 && exponent < precision) {
                    precision -= exponent + 1;
                    string = formatFloatDecimal(value, !spec.alternate);
                } else {
                    // Exponential precision is the number of digits after the decimal
                    // point, whereas 'g' precision is the number of significant digits --
                    // and exponential always provides one significant digit before the
                    // decimal point
                    precision--;
                    string = formatFloatExponential(value, (char)(spec.type-2), !spec.alternate);
                }
                if (spec.type == 'G') {
                    string = string.toUpperCase();
                }
                precision = origPrecision;
                break;
            case '%':
                string = formatPercentage(value, false);
                break;
            default:
                //Should never get here, since this was checked in PyFloat.
                throw Py.ValueError(String.format("Unknown format code '%c' for object of type 'float'",
                                    spec.type));
            }
        }
        if (sign >= 0) {
            if (spec.sign == '+') {
                string = "+" + string;
            } else if (spec.sign == ' ') {
                string = " " + string;
            }
        }
        if (sign < 0 && string.charAt(0) != '-') {
            string = "-" + string;
        }
        return spec.pad(string, '>', 0);
    }
}
