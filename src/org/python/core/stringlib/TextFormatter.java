// Copyright (c) Jython Developers
package org.python.core.stringlib;

import org.python.core.stringlib.InternalFormat.Spec;

/**
 * A class that provides the implementation of <code>str</code> and <code>unicode</code> formatting.
 * In a limited way, it acts like a StringBuilder to which text, formatted according to the format
 * specifier supplied at construction. These are ephemeral objects that are not, on their own,
 * thread safe.
 */
public class TextFormatter extends InternalFormat.Formatter {

    /**
     * Construct the formatter from a specification and guess the initial buffer capacity. A
     * reference is held to this specification.
     *
     * @param spec parsed conversion specification
     */
    public TextFormatter(Spec spec) {
        // No right answer here for the buffer size, especially as non-BMP Unicode possible.
        super(spec, Math.max(spec.width, spec.getPrecision(10)) + 6);
    }

    /*
     * Re-implement the text appends so they return the right type.
     */
    @Override
    public TextFormatter append(char c) {
        super.append(c);
        return this;
    }

    @Override
    public TextFormatter append(CharSequence csq) {
        super.append(csq);
        return this;
    }

    @Override
    public TextFormatter append(CharSequence csq, int start, int end) //
            throws IndexOutOfBoundsException {
        super.append(csq, start, end);
        return this;
    }

    /**
     * Format the given <code>String</code> into the <code>result</code> buffer. Largely, this is a
     * matter of copying the value of the argument, but a subtlety arises when the string contains
     * supplementary (non-BMP) Unicode characters, which are represented as surrogate pairs. The
     * precision specified in the format relates to a count of Unicode characters (code points), not
     * Java <code>char</code>s. The method deals with this correctly, essentially by not counting
     * the high-surrogates in the allowance. The final value of {@link #lenWhole} counts the UTF-16
     * units added.
     *
     * @param value to format
     * @return this <code>TextFormatter</code> object
     */
    public TextFormatter format(String value) {
        this.reset();
        int p = spec.precision, n = value.length();

        if (Spec.specified(p) && p < n) {
            /*
             * A precision p was specified less than the length: we may have to truncate. Note we
             * compared p with the UTF-16 length, even though it is the code point length that
             * matters. But the code point length cannot be greater than n.
             */
            int count = 0;
            while (count < p) {
                // count is the number of UTF-16 chars.
                char c = value.charAt(count++);
                result.append(c);
                // A high-surrogate will always be followed by a low, so doesn't count.
                if (Character.isHighSurrogate(c) && p < n) {
                    // Accomplish "not counting" by bumping the limit p, within the array bounds.
                    p += 1;
                }
            }
            // Record the UTF-16 count as the length in buffer
            lenWhole = count;

        } else {
            // We definitely don't need to truncate. Append the whole string.
            lenWhole = n;
            result.append(value);
        }

        return this;
    }

    /**
     * Pad the result according to the specification, dealing correctly with Unicode.
     */
    @Override
    public TextFormatter pad() {
        // We'll need this many pad characters (if>0). Note Spec.UNDEFINED<0.
        int n = spec.width - result.codePointCount(0, result.length());
        if (n > 0) {
            pad(0, n);
        }
        return this;
    }

}
