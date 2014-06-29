package org.python.core;

import java.math.BigInteger;

import junit.framework.TestCase;

import org.python.core.stringlib.FieldNameIterator;
import org.python.core.stringlib.IntegerFormatter;
import org.python.core.stringlib.InternalFormat;
import org.python.core.stringlib.MarkupIterator;
import org.python.core.stringlib.TextFormatter;
import org.python.core.stringlib.InternalFormat.Spec;
import org.python.util.PythonInterpreter;

/**
 * Tests for internal bits and pieces of string.format implementation.
 */
public class StringFormatTest extends TestCase {

    /** Exception-raising seems to need the interpreter to be initialised **/
    PythonInterpreter interp = new PythonInterpreter();

    /** Switches mode in tests that have a shared implementation for bytes and Unicode modes. */
    private boolean useBytes = true;

    public void testInternalFormatSpec() {
        InternalFormat.Spec spec;
        spec = InternalFormat.fromText("x");
        assertFalse(Spec.specified(spec.align));
        assertFalse(Spec.specified(spec.fill));
        assertFalse(Spec.specified(spec.width));
        assertFalse(Spec.specified(spec.precision));
        assertEquals('x', spec.type);

        spec = InternalFormat.fromText("<x");
        assertEquals('<', spec.align);
        assertEquals('x', spec.type);

        spec = InternalFormat.fromText("~<x");
        assertEquals('~', spec.fill);
        assertEquals('<', spec.align);
        assertEquals('x', spec.type);

        spec = InternalFormat.fromText("+x");
        assertEquals('+', spec.sign);
        assertEquals('x', spec.type);

        spec = InternalFormat.fromText("#x");
        assertEquals(true, spec.alternate);

        spec = InternalFormat.fromText("0x");
        assertEquals('=', spec.align);
        assertEquals('0', spec.fill);

        spec = InternalFormat.fromText("123x");
        assertEquals(123, spec.width);

        spec = InternalFormat.fromText("123.456x");
        assertEquals(123, spec.width);
        assertEquals(456, spec.precision);

        assertParseError("123.x", "Format specifier missing precision");

        assertParseError("123xx", "Invalid conversion specification");

        spec = InternalFormat.fromText("");
        assertEquals(Spec.NONE, spec.type);
    }

    private void assertParseError(String spec, String expected) {
        String error = null;
        try {
            InternalFormat.fromText(spec);
        } catch (PyException e) {
            assertEquals(Py.ValueError, e.type);
            error = e.value.toString();
        }
        assertEquals(expected, error);
    }

    /**
     * Test the IntegerFormatter returned by {@link PyInteger#prepareFormat}. This is based on the
     * original <code>testFormatIntOrLong</code> which tested <code>PyInteger.formatIntOrLong</code>
     * .
     */
    public void testPrepareFormatter() {
        int v = 123;
        IntegerFormatter f;
        f = PyInteger.prepareFormatter(InternalFormat.fromText("d"));
        assertEquals("123", f.format(v).pad().getResult());
        f = PyInteger.prepareFormatter(InternalFormat.fromText("o"));
        assertEquals("173", f.format(v).pad().getResult());
        f = PyInteger.prepareFormatter(InternalFormat.fromText("x"));
        assertEquals("7b", f.format(v).pad().getResult());
        f = PyInteger.prepareFormatter(InternalFormat.fromText("X"));
        assertEquals("7B", f.format(v).pad().getResult());
        f = PyInteger.prepareFormatter(InternalFormat.fromText("b"));
        assertEquals("1111011", f.format(v).pad().getResult());

        int v2 = 1234567890;
        f = PyInteger.prepareFormatter(InternalFormat.fromText(",d"));
        assertEquals("1,234,567,890", f.format(v2).pad().getResult());

        f = PyInteger.prepareFormatter(InternalFormat.fromText("#o"));
        assertEquals("0o173", f.format(v).pad().getResult());
        f = PyInteger.prepareFormatter(InternalFormat.fromText("#X"));
        assertEquals("0X7B", f.format(v).pad().getResult());

        f = PyInteger.prepareFormatter(InternalFormat.fromText("c"));
        assertEquals("{", f.format(v).pad().getResult());

        f = PyInteger.prepareFormatter(InternalFormat.fromText("+d"));
        assertEquals("+123", f.format(v).pad().getResult());
        f = PyInteger.prepareFormatter(InternalFormat.fromText(" d"));
        assertEquals(" 123", f.format(v).pad().getResult());

        f = PyInteger.prepareFormatter(InternalFormat.fromText("5"));
        assertEquals("  123", f.format(v).pad().getResult());

        f = PyInteger.prepareFormatter(InternalFormat.fromText("^6"));
        assertEquals(" 123  ", f.format(v).pad().getResult());

        f = PyInteger.prepareFormatter(InternalFormat.fromText("~<5"));
        assertEquals("123~~", f.format(v).pad().getResult());

        f = PyInteger.prepareFormatter(InternalFormat.fromText("0=+6"));
        assertEquals("+00123", f.format(v).pad().getResult());

        assertValueError("0=+6.1", "Precision not allowed in integer format specifier");
        assertValueError("+c", "Sign not allowed with integer format specifier 'c'");

        f = PyInteger.prepareFormatter(InternalFormat.fromText("c"));
        f.setBytes(true);
        assertOverflowError(256, f, "%c arg not in range(0x100)");
        assertOverflowError(-1, f, "%c arg not in range(0x100)");
        assertOverflowError(0x110000, f, "%c arg not in range(0x100)");

        f = PyInteger.prepareFormatter(InternalFormat.fromText("c"));
        assertOverflowError(0x110000, f, "%c arg not in range(0x110000)");
        assertOverflowError(-1, f, "%c arg not in range(0x110000)");
    }

    /**
     * Test the IntegerFormatter returned by {@link PyInteger#prepareFormat}. This is based on the
     * original <code>testFormatIntOrLong</code> which tested <code>PyInteger.formatIntOrLong</code>
     * .
     */
    public void testPrepareFormatterLong() {
        BigInteger v = BigInteger.valueOf(123);
        IntegerFormatter f;
        f = PyInteger.prepareFormatter(InternalFormat.fromText("d"));
        assertEquals("123", f.format(v).pad().getResult());
        f = PyInteger.prepareFormatter(InternalFormat.fromText("o"));
        assertEquals("173", f.format(v).pad().getResult());
        f = PyInteger.prepareFormatter(InternalFormat.fromText("x"));
        assertEquals("7b", f.format(v).pad().getResult());
        f = PyInteger.prepareFormatter(InternalFormat.fromText("X"));
        assertEquals("7B", f.format(v).pad().getResult());
        f = PyInteger.prepareFormatter(InternalFormat.fromText("b"));
        assertEquals("1111011", f.format(v).pad().getResult());

        BigInteger v2 = BigInteger.valueOf(1234567890);
        f = PyInteger.prepareFormatter(InternalFormat.fromText(",d"));
        assertEquals("1,234,567,890", f.format(v2).pad().getResult());

        f = PyInteger.prepareFormatter(InternalFormat.fromText("#o"));
        assertEquals("0o173", f.format(v).pad().getResult());
        f = PyInteger.prepareFormatter(InternalFormat.fromText("#X"));
        assertEquals("0X7B", f.format(v).pad().getResult());

        f = PyInteger.prepareFormatter(InternalFormat.fromText("c"));
        assertEquals("{", f.format(v).pad().getResult());

        f = PyInteger.prepareFormatter(InternalFormat.fromText("+d"));
        assertEquals("+123", f.format(v).pad().getResult());
        f = PyInteger.prepareFormatter(InternalFormat.fromText(" d"));
        assertEquals(" 123", f.format(v).pad().getResult());

        f = PyInteger.prepareFormatter(InternalFormat.fromText("5"));
        assertEquals("  123", f.format(v).pad().getResult());

        f = PyInteger.prepareFormatter(InternalFormat.fromText("^6"));
        assertEquals(" 123  ", f.format(v).pad().getResult());

        f = PyInteger.prepareFormatter(InternalFormat.fromText("~<5"));
        assertEquals("123~~", f.format(v).pad().getResult());

        f = PyInteger.prepareFormatter(InternalFormat.fromText("0=+6"));
        assertEquals("+00123", f.format(v).pad().getResult());

        f = PyInteger.prepareFormatter(InternalFormat.fromText("c"));
        f.setBytes(true);
        assertOverflowError(BigInteger.valueOf(256), f, "%c arg not in range(0x100)");
        assertOverflowError(BigInteger.valueOf(-1), f, "%c arg not in range(0x100)");
        assertOverflowError(BigInteger.valueOf(0x110000), f, "%c arg not in range(0x100)");

        f = PyInteger.prepareFormatter(InternalFormat.fromText("c"));
        assertOverflowError(BigInteger.valueOf(0x110000), f, "%c arg not in range(0x110000)");
        assertOverflowError(BigInteger.valueOf(-1), f, "%c arg not in range(0x110000)");
    }

    private void assertValueError(String formatSpec, String expected) {
        try {
            IntegerFormatter f = PyInteger.prepareFormatter(InternalFormat.fromText(formatSpec));
            // f.format(123).pad().getResult();
            fail("ValueError not thrown, expected: " + expected);
        } catch (PyException pye) {
            assertEquals(expected, pye.value.toString());
        }
    }

    private void assertOverflowError(int v, IntegerFormatter f, String expected) {
        // Test with Java int for PyInteger
        try {
            f.format(v).pad().getResult();
            fail("OverflowError not thrown, expected: " + expected);
        } catch (PyException pye) {
            assertEquals(expected, pye.value.toString());
        }
    }

    private void assertOverflowError(BigInteger v, IntegerFormatter f, String expected) {
        // Test with BigInteger for PyLong
        try {
            f.format(v).pad().getResult();
            fail("OverflowError not thrown, expected: " + expected);
        } catch (PyException pye) {
            assertEquals(expected, pye.value.toString());
        }
    }

    public void testFormatString() {
        String v = "abc";
        TextFormatter f;
        f = PyString.prepareFormatter(InternalFormat.fromText(""));
        assertEquals("abc", f.format(v).pad().getResult());

        String v2 = "abcdef";
        f = PyString.prepareFormatter(InternalFormat.fromText(".3"));
        assertEquals("abc", f.format(v2).pad().getResult());

        f = PyString.prepareFormatter(InternalFormat.fromText("6"));
        assertEquals("abc   ", f.format(v).pad().getResult());
    }

    public void implTestMarkupIterator() {
        MarkupIterator iterator = newMarkupIterator("abc");
        assertEquals("abc", iterator.nextChunk().literalText);
        assertNull(iterator.nextChunk());

        iterator = newMarkupIterator("First, thou shalt count to {0}");
        MarkupIterator.Chunk chunk = iterator.nextChunk();
        assertEquals("First, thou shalt count to ", chunk.literalText);
        assertEquals("0", chunk.fieldName);
        assertNull(iterator.nextChunk());

        iterator = newMarkupIterator("Weight in tons {0.weight!r:s}");
        chunk = iterator.nextChunk();
        assertEquals("Weight in tons ", chunk.literalText);
        assertEquals("0.weight", chunk.fieldName);
        assertEquals("r", chunk.conversion);
        assertEquals("s", chunk.formatSpec);

        chunk = newMarkupIterator("{{").nextChunk();
        assertEquals("{", chunk.literalText);

        chunk = newMarkupIterator("}}").nextChunk();
        assertEquals("}", chunk.literalText);

        chunk = newMarkupIterator("{{}}").nextChunk();
        assertEquals("{}", chunk.literalText);

        chunk = newMarkupIterator("{0:.{1}}").nextChunk();
        assertEquals("0", chunk.fieldName);
        assertEquals(".{1}", chunk.formatSpec);
        assertTrue(chunk.formatSpecNeedsExpanding);

        assertMarkupError("{!}", "end of format while looking for conversion specifier");
        assertMarkupError("{!rrrr}", "expected ':' after conversion specifier");
        assertMarkupError("{", "Single '{' encountered in format string");
        assertMarkupError("}", "Single '}' encountered in format string");
    }

    public void testMarkupIteratorBytes() {
        useBytes = true;
        implTestMarkupIterator();
    }

    public void testMarkupIteratorUnicode() {
        useBytes = false;
        implTestMarkupIterator();
    }

    private MarkupIterator newMarkupIterator(String markup) {
        PyString markupObject = useBytes ? Py.newString(markup) : Py.newUnicode(markup);
        return new MarkupIterator(markupObject);
    }

    private void assertMarkupError(String markup, String expected) {
        MarkupIterator iterator = newMarkupIterator(markup);
        String error = null;
        try {
            iterator.nextChunk();
        } catch (IllegalArgumentException e) {
            error = e.getMessage();
        }
        assertEquals(expected, error);
    }

    public void implTestFieldNameIterator() {
        FieldNameIterator it = newFieldNameIterator("abc");
        assertEquals("abc", it.head());
        assertNull(it.nextChunk());

        it = newFieldNameIterator("3");
        assertEquals(3, it.head());
        assertNull(it.nextChunk());

        it = newFieldNameIterator("abc[0]");
        assertEquals("abc", it.head());
        FieldNameIterator.Chunk chunk = it.nextChunk();
        assertEquals(0, chunk.value);
        assertFalse(chunk.is_attr);
        assertNull(it.nextChunk());

        it = newFieldNameIterator("abc.def");
        assertEquals("abc", it.head());
        chunk = it.nextChunk();
        assertEquals("def", chunk.value);
        assertTrue(chunk.is_attr);
        assertNull(it.nextChunk());
    }

    public void testFieldNameIteratorBytes() {
        useBytes = true;
        implTestFieldNameIterator();
    }

    public void testFieldNameIteratorUnicode() {
        useBytes = false;
        implTestFieldNameIterator();
    }

    private FieldNameIterator newFieldNameIterator(String field) {
        PyString fieldObject = useBytes ? Py.newString(field) : Py.newUnicode(field);
        return new FieldNameIterator(fieldObject);
    }
}
