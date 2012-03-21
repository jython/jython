package org.python.core.stringlib;

import org.python.core.Py;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.core.PyTuple;
import org.python.core.PyType;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedType;

/**
 * Provides an implementation of str._formatter_parser()
 */
@ExposedType(name = "formatteriterator", base = PyObject.class, isBaseType = false)
public class MarkupIterator extends PyObject {

    public static final PyType TYPE = PyType.fromClass(MarkupIterator.class);

    private final String markup;
    private int index;
    private final FieldNumbering numbering;

    public MarkupIterator(String markup) {
        this(markup, null);
    }

    public MarkupIterator(String markup, MarkupIterator enclosingIterator) {
        this.markup = markup;
        if (enclosingIterator != null)
            numbering = enclosingIterator.numbering;
        else
            numbering = new FieldNumbering();
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
        elements[0] = new PyString(chunk.literalText);
        elements[1] = new PyString(chunk.fieldName);
        if (chunk.fieldName.length() > 0) {
            elements[2] = chunk.formatSpec == null
                    ? Py.EmptyString : new PyString(chunk.formatSpec);
        } else {
            elements[2] = Py.None;
        }
        elements[3] = chunk.conversion == null ? Py.None : new PyString(chunk.conversion);
        return new PyTuple(elements);
    }

    public Chunk nextChunk() {
        if (index == markup.length()) {
            return null;
        }
        Chunk result = new Chunk();
        int pos = index;
        while (true) {
            pos = indexOfFirst(markup, pos, '{', '}');
            if (pos >= 0 && pos < markup.length() - 1
                && markup.charAt(pos + 1) == markup.charAt(pos)) {
                // skip escaped bracket
                pos += 2;
            } else if (pos >= 0 && markup.charAt(pos) == '}') {
                throw new IllegalArgumentException("Single '}' encountered in format string");
            } else {
                break;
            }
        }
        if (pos < 0) {
            result.literalText = unescapeBraces(markup.substring(index));
            result.fieldName = "";
            index = markup.length();
        }
        else {
            result.literalText = unescapeBraces(markup.substring(index, pos));
            pos++;
            int fieldStart = pos;
            int count = 1;
            while (pos < markup.length()) {
                if (markup.charAt(pos) == '{') {
                    count++;
                    result.formatSpecNeedsExpanding = true;
                } else if (markup.charAt(pos) == '}') {
                    count--;
                    if (count == 0) {
                        parseField(result, markup.substring(fieldStart, pos));
                        pos++;
                        break;
                    }
                }
                pos++;
            }
            if (count > 0) {
                throw new IllegalArgumentException("Single '{' encountered in format string");
            }
            index = pos;
        }
        return result;
    }

    private String unescapeBraces(String substring) {
        return substring.replace("{{", "{").replace("}}", "}");
    }

    private void parseField(Chunk result, String fieldMarkup) {
        int pos = indexOfFirst(fieldMarkup, 0, '!', ':');
        if (pos >= 0) {
            result.fieldName = fieldMarkup.substring(0, pos);
            if (fieldMarkup.charAt(pos) == '!') {
                if (pos == fieldMarkup.length() - 1) {
                    throw new IllegalArgumentException("end of format while " +
                            "looking for conversion specifier");
                }
                result.conversion = fieldMarkup.substring(pos + 1, pos + 2);
                pos += 2;
                if (pos < fieldMarkup.length()) {
                    if (fieldMarkup.charAt(pos) != ':') {
                        throw new IllegalArgumentException("expected ':' " +
                                "after conversion specifier");
                    }
                    result.formatSpec = fieldMarkup.substring(pos + 1);
                }
            } else {
                result.formatSpec = fieldMarkup.substring(pos + 1);
            }
        } else {
            result.fieldName = fieldMarkup;
        }
        if (result.fieldName.isEmpty()) {
            result.fieldName = numbering.nextAutomaticFieldNumber();
            return;
        }
        char c = result.fieldName.charAt(0);
        if (c == '.' || c == '[') {
            result.fieldName = numbering.nextAutomaticFieldNumber() + result.fieldName;
            return;
        }
        if (Character.isDigit(c))
            numbering.useManualFieldNumbering();
    }

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

    static final class FieldNumbering {
        private boolean manualFieldNumberSpecified;
        private int automaticFieldNumber = 0;

        String nextAutomaticFieldNumber() {
            if (manualFieldNumberSpecified)
                throw new IllegalArgumentException("cannot switch from manual field specification to automatic field numbering");
            return Integer.toString(automaticFieldNumber++);
        }
        void useManualFieldNumbering() {
            if (manualFieldNumberSpecified)
                return;
            if (automaticFieldNumber != 0)
                throw new IllegalArgumentException("cannot switch from automatic field numbering to manual field specification");
            manualFieldNumberSpecified = true;
        }
    }

    public static final class Chunk {
        public String literalText;
        public String fieldName;
        public String formatSpec;
        public String conversion;
        public boolean formatSpecNeedsExpanding;
    }
}
