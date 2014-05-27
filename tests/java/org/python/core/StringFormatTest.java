package org.python.core;

import java.math.BigInteger;

import junit.framework.TestCase;

import org.python.core.stringlib.FieldNameIterator;
import org.python.core.stringlib.IntegerFormatter;
import org.python.core.stringlib.InternalFormat.Formatter;
import org.python.core.stringlib.InternalFormatSpec;
import org.python.core.stringlib.InternalFormatSpecParser;
import org.python.core.stringlib.MarkupIterator;
import org.python.util.PythonInterpreter;

/**
 * Tests for internal bits and pieces of string.format implementation.
 */
public class StringFormatTest extends TestCase {

    /** Exception-raising seems to need the interpreter to be initialised **/
    PythonInterpreter interp = new PythonInterpreter();

    public void testInternalFormatSpec() {
        InternalFormatSpec spec = new InternalFormatSpecParser("x").parse();
        assertEquals('x', spec.type);

        spec = new InternalFormatSpecParser("<x").parse();
        assertEquals('<', spec.align);
        assertEquals('x', spec.type);

        spec = new InternalFormatSpecParser("~<x").parse();
        assertEquals('~', spec.fill_char);
        assertEquals('<', spec.align);
        assertEquals('x', spec.type);

        spec = new InternalFormatSpecParser("+x").parse();
        assertEquals('+', spec.sign);
        assertEquals('x', spec.type);

        spec = new InternalFormatSpecParser("#x").parse();
        assertEquals(true, spec.alternate);

        spec = new InternalFormatSpecParser("0x").parse();
        assertEquals('=', spec.align);
        assertEquals('0', spec.fill_char);

        spec = new InternalFormatSpecParser("123x").parse();
        assertEquals(123, spec.width);

        spec = new InternalFormatSpecParser("123.456x").parse();
        assertEquals(123, spec.width);
        assertEquals(456, spec.precision);

        assertParseError("123.x", "Format specifier missing precision");

        assertParseError("123xx", "Invalid conversion specification");

        spec = new InternalFormatSpecParser("").parse();
        assertEquals(0, spec.type);
    }

    private void assertParseError(String spec, String expected) {
        String error = null;
        try {
            new InternalFormatSpecParser(spec).parse();
        } catch (IllegalArgumentException e) {
            error = e.getMessage();
        }
        assertEquals(expected, error);
    }

    /**
     * Test the IntegerFormatter returned by {@link PyInteger#prepareFormat}. This is based on the original
     * <code>testFormatIntOrLong</code> which tested <code>PyInteger.formatIntOrLong</code>.
     */
    public void testPrepareFormatter() {
        int v = 123;
        IntegerFormatter f;
        f = PyInteger.prepareFormatter(new PyString("d"));
        assertEquals("123", f.format(v).pad().getResult());
        f = PyInteger.prepareFormatter(new PyString("o"));
        assertEquals("173", f.format(v).pad().getResult());
        f = PyInteger.prepareFormatter(new PyString("x"));
        assertEquals("7b", f.format(v).pad().getResult());
        f = PyInteger.prepareFormatter(new PyString("X"));
        assertEquals("7B", f.format(v).pad().getResult());
        f = PyInteger.prepareFormatter(new PyString("b"));
        assertEquals("1111011", f.format(v).pad().getResult());

        int v2 = 1234567890;
        f = PyInteger.prepareFormatter(new PyString(",d"));
        assertEquals("1,234,567,890", f.format(v2).pad().getResult());

        f = PyInteger.prepareFormatter(new PyString("#o"));
        assertEquals("0o173", f.format(v).pad().getResult());
        f = PyInteger.prepareFormatter(new PyString("#X"));
        assertEquals("0X7B", f.format(v).pad().getResult());

        f = PyInteger.prepareFormatter(new PyString("c"));
        assertEquals("{", f.format(v).pad().getResult());

        f = PyInteger.prepareFormatter(new PyString("+d"));
        assertEquals("+123", f.format(v).pad().getResult());
        f = PyInteger.prepareFormatter(new PyString(" d"));
        assertEquals(" 123", f.format(v).pad().getResult());

        f = PyInteger.prepareFormatter(new PyString("5"));
        assertEquals("  123", f.format(v).pad().getResult());

        f = PyInteger.prepareFormatter(new PyString("^6"));
        assertEquals(" 123  ", f.format(v).pad().getResult());

        f = PyInteger.prepareFormatter(new PyString("~<5"));
        assertEquals("123~~", f.format(v).pad().getResult());

        f = PyInteger.prepareFormatter(new PyString("0=+6"));
        assertEquals("+00123", f.format(v).pad().getResult());

        assertValueError("0=+6.1", "Precision not allowed in integer format specifier");
        assertValueError("+c", "Sign not allowed with integer format specifier 'c'");

        f = PyInteger.prepareFormatter(new PyString("c"));
        assertOverflowError(256, f, "%c arg not in range(0x100)");
        assertOverflowError(-1, f, "%c arg not in range(0x100)");
        assertOverflowError(0x110000, f, "%c arg not in range(0x100)");

        f = PyInteger.prepareFormatter(new PyUnicode("c"));
        assertOverflowError(0x110000, f, "%c arg not in range(0x110000)");
        assertOverflowError(-1, f, "%c arg not in range(0x110000)");
    }

    /**
     * Test the IntegerFormatter returned by {@link PyInteger#prepareFormat}. This is based on the original
     * <code>testFormatIntOrLong</code> which tested <code>PyInteger.formatIntOrLong</code>.
     */
    public void testPrepareFormatterLong() {
        BigInteger v = BigInteger.valueOf(123);
        IntegerFormatter f;
        f = PyInteger.prepareFormatter(new PyString("d"));
        assertEquals("123", f.format(v).pad().getResult());
        f = PyInteger.prepareFormatter(new PyString("o"));
        assertEquals("173", f.format(v).pad().getResult());
        f = PyInteger.prepareFormatter(new PyString("x"));
        assertEquals("7b", f.format(v).pad().getResult());
        f = PyInteger.prepareFormatter(new PyString("X"));
        assertEquals("7B", f.format(v).pad().getResult());
        f = PyInteger.prepareFormatter(new PyString("b"));
        assertEquals("1111011", f.format(v).pad().getResult());

        BigInteger v2 = BigInteger.valueOf(1234567890);
        f = PyInteger.prepareFormatter(new PyString(",d"));
        assertEquals("1,234,567,890", f.format(v2).pad().getResult());

        f = PyInteger.prepareFormatter(new PyString("#o"));
        assertEquals("0o173", f.format(v).pad().getResult());
        f = PyInteger.prepareFormatter(new PyString("#X"));
        assertEquals("0X7B", f.format(v).pad().getResult());

        f = PyInteger.prepareFormatter(new PyString("c"));
        assertEquals("{", f.format(v).pad().getResult());

        f = PyInteger.prepareFormatter(new PyString("+d"));
        assertEquals("+123", f.format(v).pad().getResult());
        f = PyInteger.prepareFormatter(new PyString(" d"));
        assertEquals(" 123", f.format(v).pad().getResult());

        f = PyInteger.prepareFormatter(new PyString("5"));
        assertEquals("  123", f.format(v).pad().getResult());

        f = PyInteger.prepareFormatter(new PyString("^6"));
        assertEquals(" 123  ", f.format(v).pad().getResult());

        f = PyInteger.prepareFormatter(new PyString("~<5"));
        assertEquals("123~~", f.format(v).pad().getResult());

        f = PyInteger.prepareFormatter(new PyString("0=+6"));
        assertEquals("+00123", f.format(v).pad().getResult());

        f = PyInteger.prepareFormatter(new PyString("c"));
        assertOverflowError(BigInteger.valueOf(256), f, "%c arg not in range(0x100)");
        assertOverflowError(BigInteger.valueOf(-1), f, "%c arg not in range(0x100)");
        assertOverflowError(BigInteger.valueOf(0x110000), f, "%c arg not in range(0x100)");

        f = PyInteger.prepareFormatter(new PyUnicode("c"));
        assertOverflowError(BigInteger.valueOf(0x110000), f, "%c arg not in range(0x110000)");
        assertOverflowError(BigInteger.valueOf(-1), f, "%c arg not in range(0x110000)");
    }

    private void assertValueError(String formatSpec, String expected) {
        try {
            IntegerFormatter f = PyInteger.prepareFormatter(new PyString(formatSpec));
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
        InternalFormatSpec spec = new InternalFormatSpec();
        assertEquals("abc", PyString.formatString("abc", spec));

        spec.precision = 3;
        assertEquals("abc", PyString.formatString("abcdef", spec));

        spec.precision = -1;
        spec.width = 6;
        assertEquals("abc   ", PyString.formatString("abc", spec));
    }

    public void testMarkupIterator() {
        MarkupIterator iterator = new MarkupIterator("abc");
        assertEquals("abc", iterator.nextChunk().literalText);
        assertNull(iterator.nextChunk());

        iterator = new MarkupIterator("First, thou shalt count to {0}");
        MarkupIterator.Chunk chunk = iterator.nextChunk();
        assertEquals("First, thou shalt count to ", chunk.literalText);
        assertEquals("0", chunk.fieldName);
        assertNull(iterator.nextChunk());

        iterator = new MarkupIterator("Weight in tons {0.weight!r:s}");
        chunk = iterator.nextChunk();
        assertEquals("Weight in tons ", chunk.literalText);
        assertEquals("0.weight", chunk.fieldName);
        assertEquals("r", chunk.conversion);
        assertEquals("s", chunk.formatSpec);

        chunk = new MarkupIterator("{{").nextChunk();
        assertEquals("{", chunk.literalText);

        chunk = new MarkupIterator("}}").nextChunk();
        assertEquals("}", chunk.literalText);

        chunk = new MarkupIterator("{{}}").nextChunk();
        assertEquals("{}", chunk.literalText);

        chunk = new MarkupIterator("{0:.{1}}").nextChunk();
        assertEquals("0", chunk.fieldName);
        assertEquals(".{1}", chunk.formatSpec);
        assertTrue(chunk.formatSpecNeedsExpanding);

        assertMarkupError("{!}", "end of format while looking for conversion specifier");
        assertMarkupError("{!rrrr}", "expected ':' after conversion specifier");
        assertMarkupError("{", "Single '{' encountered in format string");
        assertMarkupError("}", "Single '}' encountered in format string");
    }

    private void assertMarkupError(String markup, String expected) {
        MarkupIterator iterator = new MarkupIterator(markup);
        String error = null;
        try {
            iterator.nextChunk();
        } catch (IllegalArgumentException e) {
            error = e.getMessage();
        }
        assertEquals(expected, error);
    }

    public void testFieldNameIterator() {
        FieldNameIterator it = new FieldNameIterator("abc");
        assertEquals("abc", it.head());
        assertNull(it.nextChunk());

        it = new FieldNameIterator("3");
        assertEquals(3, it.head());
        assertNull(it.nextChunk());

        it = new FieldNameIterator("abc[0]");
        assertEquals("abc", it.head());
        FieldNameIterator.Chunk chunk = it.nextChunk();
        assertEquals(0, chunk.value);
        assertFalse(chunk.is_attr);
        assertNull(it.nextChunk());

        it = new FieldNameIterator("abc.def");
        assertEquals("abc", it.head());
        chunk = it.nextChunk();
        assertEquals("def", chunk.value);
        assertTrue(chunk.is_attr);
        assertNull(it.nextChunk());
    }
}
