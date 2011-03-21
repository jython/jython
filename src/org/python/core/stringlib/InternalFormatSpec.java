package org.python.core.stringlib;

/**
 * Parsed PEP-3101 format specification of a single field.
 */
public final class InternalFormatSpec {
    public char fill_char;
    public char align;
    public boolean alternate;
    public char sign;
    public int width = -1;
    public int precision = -1;
    public char type;

    public String pad(String value, char defaultAlign, int leaveWidth) {
        int remaining = width - value.length() - leaveWidth;
        if (remaining <= 0) {
            return value;
        }
        StringBuilder result = new StringBuilder();
        int leading = remaining;
        int useAlign = align;
        if (useAlign == 0) {
            useAlign = defaultAlign;
        }
        if (useAlign == '^') {
            leading = remaining/2;
        }
        else if (useAlign == '<') {
            leading = 0;
        }
        char fill = fill_char != 0 ? fill_char : ' ';
        for (int i = 0; i < leading; i++) {
            result.append(fill);
        }
        result.append(value);
        for (int i = 0; i < remaining-leading; i++) {
            result.append(fill);
        }
        return result.toString();
    }
}
