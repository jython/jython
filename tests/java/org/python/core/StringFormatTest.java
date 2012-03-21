package org.python.core;

import junit.framework.TestCase;
import org.python.core.stringlib.FieldNameIterator;
import org.python.core.stringlib.InternalFormatSpec;
import org.python.core.stringlib.InternalFormatSpecParser;
import org.python.core.stringlib.MarkupIterator;

/**
 * Tests for internal bits and pieces of string.format implementation.
 */
public class StringFormatTest extends TestCase {
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

    public void testFormatIntOrLong() {
        InternalFormatSpec spec = new InternalFormatSpec();
        spec.type = 'd';
        assertEquals("123", PyInteger.formatIntOrLong(123, spec));
        spec.type = 'o';
        assertEquals("173", PyInteger.formatIntOrLong(123, spec));
        spec.type = 'x';
        assertEquals("7b", PyInteger.formatIntOrLong(123, spec));
        spec.type = 'X';
        assertEquals("7B", PyInteger.formatIntOrLong(123, spec));
        spec.type = 'b';
        assertEquals("1111011", PyInteger.formatIntOrLong(123, spec));

        spec.thousands_separators = true;
        spec.type = 'd';
        assertEquals("1,234", PyInteger.formatIntOrLong(1234, spec));
        spec.thousands_separators = false;

        spec.alternate = true;
        spec.type = 'o';
        assertEquals("0o173", PyInteger.formatIntOrLong(123, spec));
        spec.type = 'X';
        assertEquals("0X7B", PyInteger.formatIntOrLong(123, spec));
        spec.alternate = false;

        spec.type = 'c';
        assertEquals("{", PyInteger.formatIntOrLong(123, spec));

        spec.type = 'd';
        spec.sign = '+';
        assertEquals("+123", PyInteger.formatIntOrLong(123, spec));
        spec.sign = ' ';
        assertEquals(" 123", PyInteger.formatIntOrLong(123, spec));

        spec.sign = 0;
        spec.width = 5;
        assertEquals("  123", PyInteger.formatIntOrLong(123, spec));

        spec.align = '^';
        spec.width = 6;
        assertEquals(" 123  ", PyInteger.formatIntOrLong(123, spec));

        spec.align = '<';
        spec.width = 5;
        spec.fill_char = '~';
        assertEquals("123~~", PyInteger.formatIntOrLong(123, spec));

        spec.align = '=';
        spec.width = 6;
        spec.fill_char = '0';
        spec.sign = '+';
        assertEquals("+00123", PyInteger.formatIntOrLong(123, spec));

        spec.precision = 1;
        assertFormatError(123, spec, "Precision not allowed in integer format specifier");

        spec.precision = -1;
        spec.sign = '+';
        spec.type = 'c';
        assertFormatError(123, spec, "Sign not allowed with integer format specifier 'c'");

        spec.sign = 0;
        assertFormatError(0x11111, spec, "%c arg not in range(0x10000)");
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

    private void assertFormatError(int value, InternalFormatSpec spec, String expected) {
        String error = null;
        try {
            PyInteger.formatIntOrLong(value, spec);
        } catch (IllegalArgumentException e) {
            error = e.getMessage();
        }
        assertEquals(expected, error);
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
