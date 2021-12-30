// Copyright (c)2021 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.core.stringlib;

import java.lang.invoke.MethodHandles;

import org.python.base.MissingFeature;
import org.python.core.CraftedPyObject;
import org.python.core.PyTuple;
import org.python.core.PyType;


/**
 * This class is an implementation of the iterator object returned
 * by {@code string.formatter_field_name_split()}. The function
 * {@code formatter_field_name_split()} returns a pair (tuple)
 * consisting of a head element and an instance of this iterator.
 * The constructor of this class effectively implements that
 * function, since as well as being the iterator (second member),
 * the object has an extra method {@link #head()} to return the
 * required first member of the pair.
 */
public class FieldNameIterator implements CraftedPyObject {

    /** The Python type {@code fieldnameiterator} of this class. */
    public static final PyType TYPE = PyType.fromSpec( //
            new PyType.Spec("fieldnameiterator", MethodHandles.lookup())
                    .flagNot(PyType.Flag.BASETYPE));

    /** The UTF-16 string from which elements are being returned. */
    private final String markup;
    /**
     * True if originally given a PyString (so must return PyString not
     * PyUnicode).
     */
    // XXX re-think for Jython 3 when we have PyBytes
    private final boolean bytes;
    /** How far along that string we are. */
    private int index;
    private Object head;

    /**
     * Create an iterator for the parts of this field name (and extract
     * the head name field, which may be an empty string). According to
     * the Python Standard Library documentation, a replacement field
     * name has the structure:
     *
     * <pre>
     * field_name        ::=  arg_name ("." attribute_name | "[" element_index "]")*
     * arg_name          ::=  [identifier | integer]
     * attribute_name    ::=  identifier
     * element_index     ::=  integer | index_string
     * </pre>
     *
     * The object is used from PyUnicode and from PyString, and we have
     * to signal which it is, so that returned values may match in type.
     *
     * @param fieldName the field name as UTF-16
     * @param bytes true if elements returned should be bytes
     */
    public FieldNameIterator(String fieldName, boolean bytes) {
        this.markup = fieldName;
        this.bytes = bytes;
        this.index = nextDotOrBracket(fieldName);
        String headStr = fieldName.substring(0, index);
        try {
            this.head = Integer.parseInt(headStr);
        } catch (NumberFormatException e) {
            this.head = headStr;
        }
    }

    /**
     * Create an iterator for the parts of this field name (and extract
     * the head name field, which may be an empty string).
     *
     * @param fieldName to parse
     */
    public FieldNameIterator(String fieldName) {
        /*
         * XXX Extract UTF-16 string but remember whether str or bytes
         * should result.
         */
        this(fieldName, false);
    }

    @Override
    public PyType getType() { return TYPE; }

    @SuppressWarnings("unused")
    private final Object __iter__() { return this; }

    @SuppressWarnings("unused")
    private final Object __next__() {
        Chunk chunk = nextChunk();
        if (chunk == null) { throw new MissingFeature("StopIteration"); }
        return new PyTuple(chunk.is_attr, chunk.value);
    }

    private int nextDotOrBracket(String markup) {
        int dotPos = markup.indexOf('.', index);
        if (dotPos < 0) { dotPos = markup.length(); }
        int bracketPos = markup.indexOf('[', index);
        if (bracketPos < 0) { bracketPos = markup.length(); }
        return Math.min(dotPos, bracketPos);
    }

    /**
     * Return the head object from the field name, as {@code int} or
     * {@code str}.
     *
     * @return the isolated head object from the field name.
     */
    public Object head() { return head; }

    /**
     * Return the next "chunk" of the field name (or return null if
     * ended). A chunk is a 2-tuple describing:
     * <ol start=0>
     * <li>whether the chunk is an attribute name,</li>
     * <li>the name or number (as a String or Integer) for accessing the
     * value.</li>
     * </ol>
     *
     * @return next element of the field name
     */
    public Chunk nextChunk() {
        if (index == markup.length()) { return null; }
        Chunk chunk = new Chunk();
        if (markup.charAt(index) == '[') {
            parseItemChunk(chunk);
        } else if (markup.charAt(index) == '.') {
            parseAttrChunk(chunk);
        } else {
            throw new IllegalArgumentException(
                    "Only '.' or '[' may follow ']' in format field specifier");
        }
        return chunk;
    }

    private void parseItemChunk(Chunk chunk) {
        chunk.is_attr = false;
        int endBracket = markup.indexOf(']', index + 1);
        if (endBracket < 0) { throw new IllegalArgumentException("Missing ']' in format string"); }
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
