package org.python.core.stringlib;

/**
 * Parser for PEP-3101 field format specifications.
 */
public class InternalFormatSpecParser {
    private String spec;
    private int index;

    public InternalFormatSpecParser(String spec) {
        this.spec = spec;
        this.index = 0;
    }

    private static boolean isAlign(char c) {
        switch(c) {
            case '<':
            case '>':
            case '=':
            case '^':
                return true;
            default:
                return false;
        }
    }

    public InternalFormatSpec parse() {
        InternalFormatSpec result = new InternalFormatSpec();
        if (spec.length() >= 1 && isAlign(spec.charAt(0))) {
            result.align = spec.charAt(index);
            index++;
        } else if (spec.length() >= 2 && isAlign(spec.charAt(1))) {
            result.fill_char = spec.charAt(0);
            result.align = spec.charAt(1);
            index += 2;
        }
        if (isAt("+- ")) {
            result.sign = spec.charAt(index);
            index++;
        }
        if (isAt("#")) {
            result.alternate = true;
            index++;
        }
        if (isAt("0")) {
            result.align = '=';
            result.fill_char = '0';
            index++;
        }
        result.width = getInteger();
        if (isAt(".")) {
            index++;
            result.precision = getInteger();
            if (result.precision == -1) {
                throw new IllegalArgumentException("Format specifier missing precision");
            }
        }
        if (index < spec.length()) {
            result.type = spec.charAt(index);
            if (index + 1 != spec.length()) {
                throw new IllegalArgumentException("Invalid conversion specification");
            }
        }
        return result;
    }

    private int getInteger() {
        int value = 0;
        boolean empty = true;
        while (index < spec.length() && spec.charAt(index) >= '0' && spec.charAt(index) <= '9') {
            value = value * 10 + spec.charAt(index) - '0';
            index++;
            empty = false;
        }
        if (empty) {
            return -1;
        }
        return value;
    }

    private boolean isAt(String chars) {
        return index < spec.length() && chars.indexOf(spec.charAt(index)) >= 0;
    }
}
