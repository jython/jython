package org.python.core.stringlib;

import org.python.core.Py;
import org.python.core.PyBoolean;
import org.python.core.PyInteger;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.core.PyTuple;
import org.python.core.PyType;
import org.python.core.PyUnicode;
import org.python.core.Traverseproc;
import org.python.core.Visitproc;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedType;
import org.python.modules.gc;

/**
 * This class is an implementation of the iterator object returned by
 * <code>str._formatter_field_name_split()</code> and
 * <code>unicode._formatter_field_name_split()</code>. The function
 * <code>_formatter_field_name_split()</code> returns a pair (tuple) consisting of a head element
 * and an instance of this iterator. The constructor of this class effectively implements that
 * function, since as well as "being" the iterator, the object has an extra method {@link #head()}
 * to return the required first member of the pair.
 */

@ExposedType(name = "fieldnameiterator", base = PyObject.class, isBaseType = false)
public class FieldNameIterator extends PyObject implements Traverseproc {

    public static final PyType TYPE = PyType.fromClass(FieldNameIterator.class);

    /** The UTF-16 string from which elements are being returned. */
    private final String markup;
    /** True if originally given a PyString (so must return PyString not PyUnicode). */
    private final boolean bytes;
    /** How far along that string we are. */
    private int index;
    private Object head;

    /**
     * Create an iterator for the parts of this field name (and extract the head name field, which
     * may be an empty string). According to the Python Standard Library documentation, a
     * replacement field name has the structure:
     *
     * <pre>
     * field_name        ::=  arg_name ("." attribute_name | "[" element_index "]")*
     * arg_name          ::=  [identifier | integer]
     * attribute_name    ::=  identifier
     * element_index     ::=  integer | index_string
     * </pre>
     *
     * The object is used from PyUnicode and from PyString, and we have to signal which it is, so
     * that returned values may match in type.
     *
     * @param fieldName the field name as UTF-16
     * @param bytes true if elements returned should be PyString, else PyUnicode
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
     * Create an iterator for the parts of this field name (and extract the head name field, which
     * may be an empty string).
     *
     * @param fieldNameObject
     */
    public FieldNameIterator(PyString fieldNameObject) {
        // Extract UTF-16 string but remember whether PyString or PyUnicode shouyld result.
        this(fieldNameObject.getString(), !(fieldNameObject instanceof PyUnicode));
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
        return new PyTuple(Py.newBoolean(chunk.is_attr), wrap(chunk.value));
    }

    /**
     * Convenience method to wrap a value as a PyInteger, if it is an Integer, or as
     * <code>PyString</code> or <code>PyUnicode</code> according to the type of the original field
     * name string. These objects are being used as field specifiers in navigating arguments to a
     * format statement.
     *
     * @param value to wrap as a PyObject.
     * @return PyObject equivalent field specifier
     */
    private PyObject wrap(Object value) {
        if (value instanceof Integer) {
            return Py.newInteger(((Integer)value).intValue());
        } else {
            // It can only be a String (but if not, at least we see it).
            String s = value.toString();
            if (s.length() == 0) {
                // This is frequent so avoid the constructor
                return bytes ? Py.EmptyString : Py.EmptyUnicode;
            } else {
                return bytes ? Py.newString(s) : Py.newUnicode(s);
            }
        }
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

    /** @return the isolated head object from the field name. */
    public Object head() {
        return head;
    }

    /**
     * Return the head object from the field name, as <code>PyInteger</code>, <code>PyString</code>
     * or <code>PyUnicode</code>.
     *
     * @return the isolated head object from the field name.
     */
    public PyObject pyHead() {
        return wrap(head());
    }

    /**
     * If originally given a PyString, the iterator must return PyString not PyUnicode.
     *
     * @return true if originally given a PyString
     */
    public final boolean isBytes() {
        return bytes;
    }

    /**
     * Return the next "chunk" of the field name (or return null if ended). A chunk is a 2-tuple
     * describing:
     * <ol start=0>
     * <li>whether the chunk is an attribute name,</li>
     * <li>the name or number (as a String or Integer) for accessing the value.</li>
     * </ol>
     *
     * @return next element of the field name
     */
    public Chunk nextChunk() {
        if (index == markup.length()) {
            return null;
        }
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


    /* Traverseproc implementation */
    @Override
    public int traverse(Visitproc visit, Object arg) {
        if (head == null || !gc.canLinkToPyObject(head.getClass(), true)) {
        	return 0;
        }
        return gc.traverseByReflection(head, visit, arg);
    }

    @Override
    public boolean refersDirectlyTo(PyObject ob)
            throws UnsupportedOperationException {
        if (ob != null && ob == head) {
        	return true;
        }
        if (!gc.canLinkToPyObject(head.getClass(), true)) {
        	return false;
        }
        throw new UnsupportedOperationException();
    }
}
