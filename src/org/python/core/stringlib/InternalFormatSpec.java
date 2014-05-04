package org.python.core.stringlib;

/**
 * Parsed PEP-3101 format specification of a single field. This class holds the several attributes
 * that might be decoded from a format specifier. It provides a method
 * {@link #pad(String, char, int)} for adjusting a string using those attributes related to padding
 * to a string assumed to be the result of formatting to the given precision.
 * <p>
 * This structure is returned by {@link InternalFormatSpecParser#parse()} and having public members
 * is freely used by {@link InternalFormatSpecParser}, and the __format__ methods of client object
 * types.
 * <p>
 * The fields correspond to the elements of a format specification. The grammar of a format
 * specification is:
 *
 * <pre>
 * [[fill]align][sign][#][0][width][,][.precision][type]
 * </pre>
 */
public final class InternalFormatSpec {

    /** The fill specified in the grammar. */
    public char fill_char;
    /** Alignment indicator is 0, or one of {<code>'&lt;', '^', '>', '='</code> . */
    public char align;
    /** The alternative format flag '#' was given. */
    public boolean alternate;
    /** Sign-handling flag, one of <code>'+'</code>, <code>'-'</code>, or <code>' '</code>. */
    public char sign;
    /** Width to which to pad the resault in {@link #pad(String, char, int)}. */
    public int width = -1;
    /** Insert the grouping separator (which in Python always indicates a group-size of 3). */
    public boolean thousands_separators;
    /** Precision decoded from the format. */
    public int precision = -1;
    /** Type key from the format. */
    public char type;

    /**
     * Pad value, using {@link #fill_char} (or <code>' '</code>) before and after, to {@link #width}
     * <code>-leaveWidth</code>, aligned according to {@link #align} (or according to
     * <code>defaultAlign</code>).
     *
     * @param value to pad
     * @param defaultAlign to use if <code>this.align</code>=0 (one of <code>'&lt;'</code>,
     *            <code>'^'</code>, <code>'>'</code>, or <code>'='</code>).
     * @param leaveWidth to reduce effective <code>this.width</code> by
     * @return padded value
     */
    public String pad(String value, char defaultAlign, int leaveWidth) {

        // We'll need this many pad characters (if>0)
        int remaining = width - value.length() - leaveWidth;
        if (remaining <= 0) {
            return value;
        }

        // Use this.align or defaultAlign
        int useAlign = align;
        if (useAlign == 0) {
            useAlign = defaultAlign;
        }

        // By default all padding is leading padding ('<' case or '=')
        int leading = remaining;
        if (useAlign == '^') {
            // Half the padding before
            leading = remaining / 2;
        } else if (useAlign == '<') {
            // All the padding after
            leading = 0;
        }

        // Now build the result
        StringBuilder result = new StringBuilder();
        char fill = fill_char != 0 ? fill_char : ' ';

        for (int i = 0; i < leading; i++) { // before
            result.append(fill);
        }
        result.append(value);
        for (int i = 0; i < remaining - leading; i++) { // after
            result.append(fill);
        }

        return result.toString();
    }
}
