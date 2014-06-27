package org.python.core.stringlib;

import org.python.core.Py;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.core.PyTuple;
import org.python.core.PyType;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedType;

/**
 * Provides an implementation of the object that <code>str._formatter_parser()</code> returns, which
 * is an iterator returning successive 4-tuples, the sequence being equivalent to the original
 * string.
 */
@ExposedType(name = "formatteriterator", base = PyObject.class, isBaseType = false)
public class MarkupIterator extends PyObject {

    public static final PyType TYPE = PyType.fromClass(MarkupIterator.class);

    /** The string from which elements are being returned. */
    private final String markup;
    /** How far along that string we are. */
    private int index;
    /** A counter used to auto-number fields when not explicitly numbered in the format. */
    private final FieldNumbering numbering;

    /** Constructor used at top-level to enumerate a format. */
    public MarkupIterator(String markup) {
        this(markup, null);
    }

    /** Variant constructor used when formats are nested. */
    public MarkupIterator(String markup, MarkupIterator enclosingIterator) {
        this.markup = markup;
        if (enclosingIterator != null) {
            numbering = enclosingIterator.numbering;
        } else {
            numbering = new FieldNumbering();
        }
    }

    @Override
    public PyObject __iter__() {
        return formatteriterator___iter__();
    }

    @ExposedMethod
    final PyObject formatteriterator___iter__() {
        return this;
    }

    @Override
    public PyObject __iternext__() {
        return formatteriterator___iternext__();
    }

    /**
     * Return the next "chunk" of the format (or return null if ended). A chunk is a 4-tuple
     * describing
     * <ol start=0>
     * <li>the text leading up to the next format field,</li>
     * <li>the field name or number (as a string) for accessing the value,</li>
     * <li>the format specifier such as <code>"#12x"</code>, and</li>
     * <li>any conversion that should be applied (the <code>'s'</code> or <code>'r'</code> codes for
     * <code>str()</code> and <code>repr()</code>)</li>
     * </ol>
     * Elements 1-3 are None if this chunk contains no format specifier. Elements 0-2 are
     * zero-length strings if missing from the format, while element 3 will be None if missing.
     * 
     * @return <code>PyTuple</code> chunk or <code>null</code>
     */
    @ExposedMethod
    final PyObject formatteriterator___iternext__() {
        Chunk chunk;
        try {
            chunk = nextChunk();
        } catch (IllegalArgumentException e) {
            throw Py.ValueError(e.getMessage());
        }
        if (chunk == null) {
            return null;
        }
        PyObject[] elements = new PyObject[4];

        // Literal text is used verbatim.
        elements[0] = new PyString(chunk.literalText);

        // A field name is empty only if there was no format at all.
        elements[1] = chunk.fieldName.length() == 0 ? Py.None : new PyString(chunk.fieldName);
        if (chunk.fieldName.length() > 0) {
            elements[2] =
                    chunk.formatSpec == null ? Py.EmptyString : new PyString(chunk.formatSpec);
        } else {
            elements[2] = Py.None;
        }

        // There may have been a conversion specifier.
        elements[3] = chunk.conversion == null ? Py.None : new PyString(chunk.conversion);

        // And those make up the next answer.
        return new PyTuple(elements);
    }

    public Chunk nextChunk() {
        if (index == markup.length()) {
            return null;
        }
        Chunk result = new Chunk();

        // pos = index is the index of the first text not already chunked
        int pos = index;

        // Advance pos to the first '{' that is not a "{{" (escaped brace), or pos<0 if none such.
        while (true) {
            pos = indexOfFirst(markup, pos, '{', '}');
            if (pos >= 0 && pos < markup.length() - 1
                    && markup.charAt(pos + 1) == markup.charAt(pos)) {
                // skip escaped bracket
                pos += 2;
            } else if (pos >= 0 && markup.charAt(pos) == '}') {
                // Un-escaped '}' is a syntax error
                throw new IllegalArgumentException("Single '}' encountered in format string");
            } else {
                // pos is at an un-escaped '{'
                break;
            }
        }

        // markup[index:pos] is the literal part of this chunk.
        if (pos < 0) {
            // ... except pos<0, and there is no further format specifier, only literal text.
            result.literalText = unescapeBraces(markup.substring(index));
            result.fieldName = "";
            index = markup.length();

        } else {
            // Grab the literal text, dealing with escaped braces.
            result.literalText = unescapeBraces(markup.substring(index, pos));
            // Scan through the contents of the format spec, between the braces. Skip one '{'.
            pos++;
            int fieldStart = pos;
            int count = 1;
            while (pos < markup.length()) {
                if (markup.charAt(pos) == '{') {
                    // This means the spec we are gathering itself contains nested specifiers.
                    count++;
                    result.formatSpecNeedsExpanding = true;
                } else if (markup.charAt(pos) == '}') {
                    // And here is a '}' matching one we already counted.
                    count--;
                    if (count == 0) {
                        // ... matching the one we began with: parse the replacement field.
                        parseField(result, markup.substring(fieldStart, pos));
                        pos++;
                        break;
                    }
                }
                pos++;
            }
            if (count > 0) {
                // Must be end of string without matching '}'.
                throw new IllegalArgumentException("Single '{' encountered in format string");
            }
            index = pos;
        }
        return result;
    }

    private String unescapeBraces(String substring) {
        return substring.replace("{{", "{").replace("}}", "}");
    }

    /**
     * Parse a "replacement field" consisting of a name, conversion and format specification.
     * According to the Python Standard Library documentation, a replacement field has the
     * structure:
     * 
     * <pre>
     * replacement_field ::=  "{" [field_name] ["!" conversion] [":" format_spec] "}"
     * field_name        ::=  arg_name ("." attribute_name | "[" element_index "]")*
     * arg_name          ::=  [identifier | integer]
     * attribute_name    ::=  identifier
     * element_index     ::=  integer | index_string
     * </pre>
     * 
     * except at this point, we have already discarded the outer braces.
     * 
     * @param result destination chunk
     * @param fieldMarkup specifying a replacement field, possibly with nesting
     */
    private void parseField(Chunk result, String fieldMarkup) {
        int pos = indexOfFirst(fieldMarkup, 0, '!', ':');
        if (pos >= 0) {
            // There's a '!' or a ':', so what precedes the first of them is a field name.
            result.fieldName = fieldMarkup.substring(0, pos);
            if (fieldMarkup.charAt(pos) == '!') {
                // There's a conversion specifier
                if (pos == fieldMarkup.length() - 1) {
                    throw new IllegalArgumentException("end of format while "
                            + "looking for conversion specifier");
                }
                result.conversion = fieldMarkup.substring(pos + 1, pos + 2);
                pos += 2;
                // And if that's not the end, there ought to be a ':' now.
                if (pos < fieldMarkup.length()) {
                    if (fieldMarkup.charAt(pos) != ':') {
                        throw new IllegalArgumentException("expected ':' "
                                + "after conversion specifier");
                    }
                    // So the format specifier is from the ':' to the end.
                    result.formatSpec = fieldMarkup.substring(pos + 1);
                }
            } else {
                // No '!', so the format specifier is from the ':' to the end. Or empty.
                result.formatSpec = fieldMarkup.substring(pos + 1);
            }
        } else {
            // Neither a '!' nor a ':', the whole thing is a name.
            result.fieldName = fieldMarkup;
        }

        if (result.fieldName.isEmpty()) {
            // The field was empty, so generate a number automatically.
            result.fieldName = numbering.nextAutomaticFieldNumber();
            return;
        }

        // Automatic numbers must also work when there is an .attribute or [index]
        char c = result.fieldName.charAt(0);
        if (c == '.' || c == '[') {
            result.fieldName = numbering.nextAutomaticFieldNumber() + result.fieldName;
            return;
        }

        // Finally, remember the argument number was specified (perhaps complain of mixed use)
        if (Character.isDigit(c)) {
            numbering.useManualFieldNumbering();
        }
    }

    /** Find the first of two characters, or return -1. */
    private int indexOfFirst(String s, int start, char c1, char c2) {
        int i1 = s.indexOf(c1, start);
        int i2 = s.indexOf(c2, start);
        if (i1 == -1) {
            return i2;
        }
        if (i2 == -1) {
            return i1;
        }
        return Math.min(i1, i2);
    }

    /**
     * Class used locally to assign indexes to the automatically-numbered arguments (see String
     * Formatting section of the Python Standard Library).
     */
    static final class FieldNumbering {

        private boolean manualFieldNumberSpecified;
        private int automaticFieldNumber = 0;

        /**
         * Generate a numeric argument index automatically, or raise an error if already started
         * numbering manually.
         * 
         * @return index as string
         */
        String nextAutomaticFieldNumber() {
            if (manualFieldNumberSpecified) {
                throw new IllegalArgumentException(
                        "cannot switch from manual field specification to automatic field numbering");
            }
            return Integer.toString(automaticFieldNumber++);
        }

        /**
         * Remember we are numbering manually, and raise an error if already started numbering
         * automatically.
         */
        void useManualFieldNumbering() {
            if (manualFieldNumberSpecified) {
                return;
            }
            if (automaticFieldNumber != 0) {
                throw new IllegalArgumentException(
                        "cannot switch from automatic field numbering to manual field specification");
            }
            manualFieldNumberSpecified = true;
        }
    }

    public static final class Chunk {

        /** The text leading up to the next format field. */
        public String literalText;
        /** The field name or number (as a string) for accessing the value. */
        public String fieldName;
        /** The format specifier such as <code>"#12x"</code>. */
        public String formatSpec;
        /** Conversion to be applied, e.g. <code>'r'</code> for <code>repr()</code>. */
        public String conversion;
        /** Signals the <code>formatSpec</code> needs expanding recursively. */
        public boolean formatSpecNeedsExpanding;
    }
}
