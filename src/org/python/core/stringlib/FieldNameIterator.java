package org.python.core.stringlib;

import org.python.core.PyBoolean;
import org.python.core.PyInteger;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.core.PyTuple;
import org.python.core.PyType;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedType;

/**
 * Provides an implementation of str._formatter_field_name_split()
 */
@ExposedType(name = "fieldnameiterator", base = PyObject.class, isBaseType = false)
public class FieldNameIterator extends PyObject {

    public static final PyType TYPE = PyType.fromClass(FieldNameIterator.class);

    private String markup;
    private Object head;
    private int index;

    public FieldNameIterator(String markup) {
        this.markup = markup;
        this.index = nextDotOrBracket(markup);
        String headStr = markup.substring(0, index);
        try {
            this.head = Integer.parseInt(headStr);
        } catch (NumberFormatException e) {
            this.head = headStr;
        }
    }

    @Override
    public PyObject __iter__() {
        return fieldnameiterator___iter__();
    }

    @ExposedMethod
    final PyObject fieldnameiterator___iter__() {
        return this;
    }

    @Override
    public PyObject __iternext__() {
        return fieldnameiterator___iternext__();
    }

    @ExposedMethod
    final PyObject fieldnameiterator___iternext__() {
        Chunk chunk = nextChunk();
        if (chunk == null) {
            return null;
        }
        PyObject[] elements = new PyObject[2];
        elements [0] = new PyBoolean(chunk.is_attr);
        if (chunk.value instanceof Integer) {
            elements [1] = new PyInteger((Integer) chunk.value);
        } else {
            elements [1] = new PyString((String) chunk.value);
        }
        return new PyTuple(elements);
    }

    private int nextDotOrBracket(String markup) {
        int dotPos = markup.indexOf('.', index);
        if (dotPos < 0) {
            dotPos = markup.length();
        }
        int bracketPos = markup.indexOf('[', index);
        if (bracketPos < 0) {
            bracketPos = markup.length();
        }
        return Math.min(dotPos, bracketPos);
    }

    public Object head() {
        return head;
    }

    public Chunk nextChunk() {
        if (index == markup.length()) {
            return null;
        }
        Chunk chunk = new Chunk();
        if (markup.charAt(index) == '[') {
            parseItemChunk(chunk);
        } else if (markup.charAt(index) == '.') {
            parseAttrChunk(chunk);
        }
        return chunk;
    }

    private void parseItemChunk(Chunk chunk) {
        chunk.is_attr = false;
        int endBracket = markup.indexOf(']', index+1);
        if (endBracket < 0) {
            throw new IllegalArgumentException("Missing ']' in format string");
        }
        String itemValue = markup.substring(index + 1, endBracket);
        if (itemValue.length() == 0) {
            throw new IllegalArgumentException("Empty attribute in format string");
        }
        try {
            chunk.value = Integer.parseInt(itemValue);
        } catch (NumberFormatException e) {
            chunk.value = itemValue;
        }
        index = endBracket + 1;
    }

    private void parseAttrChunk(Chunk chunk) {
        index++;   // skip dot
        chunk.is_attr = true;
        int pos = nextDotOrBracket(markup);
        if (pos == index) {
            throw new IllegalArgumentException("Empty attribute in format string");
        }
        chunk.value = markup.substring(index, pos);
        index = pos;
    }

    public static class Chunk {
        public boolean is_attr;
        /** Integer or String. */
        public Object value;
    }
}
