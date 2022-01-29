// Copyright (c)2021 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.core.stringlib;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.python.core.PyType;
import org.python.core.stringlib.InternalFormat.Spec;

/**
 * Tests of {@code stringlib} support for formatting. These facilities
 * lie behind the {@code __format__} methods of built-in types, and
 * methods exposed by Python module {@code _string}, for example.
 */
class FormatParsingTest {

    /**
     * Test constructing a specification {@link Spec} from a format
     * string. Note that a parsed specification is not a Python type so
     * it reports errors usiung Java-only exceptions that clients may
     * convert to {@code ValueError}.
     */
    @Nested
    @DisplayName("An InternalFormat.Spec correctly")
    class InternalFormatSpecTest {

        @Test
        @DisplayName("interprets ''")
        public void fromEmpty() {
            InternalFormat.Spec spec = InternalFormat.fromText("");
            assertEquals(Spec.NONE, spec.type);
        }

        @Test
        @DisplayName("interprets 'x'")
        public void fromPlain() {
            InternalFormat.Spec spec = InternalFormat.fromText("x");
            assertFalse(Spec.specified(spec.align));
            assertFalse(Spec.specified(spec.fill));
            assertFalse(Spec.specified(spec.width));
            assertFalse(Spec.specified(spec.precision));
            assertEquals('x', spec.type);
        }

        @Test
        @DisplayName("interprets '<x'")
        public void fromPLeftAlign() {
            InternalFormat.Spec spec = InternalFormat.fromText("<x");
            assertEquals('<', spec.align);
            assertEquals('x', spec.type);
        }

        @Test
        @DisplayName("interprets '~<x'")
        public void fromFilled() {
            InternalFormat.Spec spec = InternalFormat.fromText("~<x");
            assertEquals('~', spec.fill);
            assertEquals('<', spec.align);
            assertEquals('x', spec.type);
        }

        @Test
        @DisplayName("interprets '+x'")
        public void fromMandatorySign() {
            InternalFormat.Spec spec = InternalFormat.fromText("+x");
            assertEquals('+', spec.sign);
            assertEquals('x', spec.type);
        }

        @Test
        @DisplayName("interprets '#x'")
        public void fromAlternate() {
            InternalFormat.Spec spec = InternalFormat.fromText("#x");
            assertEquals(true, spec.alternate);

        }

        @Test
        @DisplayName("interprets '0x'")
        public void fromLeadingZero() {
            InternalFormat.Spec spec = InternalFormat.fromText("0x");
            assertEquals('=', spec.align);
            assertEquals('0', spec.fill);
        }

        @Test
        @DisplayName("interprets '123x'")
        public void fromWidth() {
            InternalFormat.Spec spec = InternalFormat.fromText("123x");
            assertEquals(123, spec.width);

        }

        @Test
        @DisplayName("interprets '123.456x'")
        public void fromWidthPrecision() {
            InternalFormat.Spec spec =
                    InternalFormat.fromText("123.456x");
            assertEquals(123, spec.width);
            assertEquals(456, spec.precision);
        }

        @Test
        @DisplayName("rejects '123.x'")
        public void rejectMissingPrecision() {
            assertParseError(IllegalArgumentException.class, "123.x",
                    "Format specifier missing precision");
        }

        @Test
        @DisplayName("rejects '123xx'")
        public void rejectTrailing() {
            assertParseError(IllegalArgumentException.class, "123xx",
                    "Invalid format specifier");
        }

        /**
         * Parse a specification expected to be erroneous and check the
         * message.
         *
         * @param <E> type of exception
         * @param expected type of exception
         * @param spec to parse
         * @param expectedMessage expected message text
         * @return what was thrown
         */
        private <T extends Throwable> T assertParseError(
                Class<T> expected, String spec,
                String expectedMessage) {
            T t = assertThrows(expected,
                    () -> InternalFormat.fromText(spec));
            assertEquals(expectedMessage, t.getMessage());
            return t;
        }
    }

    /**
     * Test that the correct "chunks" are parsed from a format string,
     * and presented as a {@link MarkupIterator}
     * ({@code formatteriterator} in Python). We make this test using
     * the Java API here, because it is statically-typed, and Python
     * iteration need not be working. In Python it produces
     * {@code tuple}s.
     */
    @Nested
    @DisplayName("A stringlib.MarkupIterator")
    class MarkupIteratorTest {

        @Test
        @DisplayName("is a Python formatteriterator")
        void hasPythonType() {
            MarkupIterator it = new MarkupIterator("abc");
            PyType type = it.getType();
            assertEquals("formatteriterator", type.getName());
        }

        @Test
        @DisplayName("parses the literal 'abc'")
        void parsesLiteral() {
            MarkupIterator it = new MarkupIterator("abc");
            assertEquals("abc", it.nextChunk().literalText);
            assertNull(it.nextChunk());
        }

        @Test
        @DisplayName("parses 'First, thou shalt count to {0}'")
        void parsesTerminalIndex() {
            MarkupIterator it = new MarkupIterator(
                    "First, thou shalt count to {0}");
            MarkupIterator.Chunk chunk = it.nextChunk();
            assertEquals("First, thou shalt count to ",
                    chunk.literalText);
            assertEquals("0", chunk.fieldName);
            assertNull(it.nextChunk());
        }

        @Test
        @DisplayName("parses 'Weight in tons {0.weight!r:s}'")
        void parsesTerminalAttribute() {
            MarkupIterator it =
                    new MarkupIterator("Weight in tons {0.weight!r:s}");
            MarkupIterator.Chunk chunk = it.nextChunk();
            assertEquals("Weight in tons ", chunk.literalText);
            assertEquals("0.weight", chunk.fieldName);
            assertEquals("r", chunk.conversion);
            assertEquals("s", chunk.formatSpec);
        }

        @Test
        @DisplayName("parses '{{' and '}}'")
        void parsesEscapedBraces() {
            MarkupIterator.Chunk chunk =
                    (new MarkupIterator("{{")).nextChunk();
            assertEquals("{", chunk.literalText);

            chunk = (new MarkupIterator("}}")).nextChunk();
            assertEquals("}", chunk.literalText);

            chunk = (new MarkupIterator("{{}}")).nextChunk();
            assertEquals("{}", chunk.literalText);
        }

        @Test
        @DisplayName("parses '{0:.{1}}'")
        void parsesNestedReference() {
            MarkupIterator.Chunk chunk =
                    (new MarkupIterator("{0:.{1}}")).nextChunk();
            assertEquals("0", chunk.fieldName);
            assertEquals(".{1}", chunk.formatSpec);
            assertTrue(chunk.formatSpecNeedsExpanding);
        }

        @Test
        @DisplayName("rejects conversions '{!}' and '{!rrrr}'")
        void rejectsBadConversion() {
            assertMarkupError("{!}",
                    "end of format while looking for conversion specifier");
            assertMarkupError("{!rrrr}",
                    "expected ':' after conversion specifier");
        }

        @Test
        @DisplayName("rejects unbalanced '{' and '}'")
        void rejectsUnbalencedBraces() {
            assertMarkupError("{",
                    "Single '{' encountered in format string");
            assertMarkupError("}",
                    "Single '}' encountered in format string");
        }

        private void assertMarkupError(String markup, String expected) {
            MarkupIterator it = new MarkupIterator(markup);
            Exception e = assertThrows(IllegalArgumentException.class,
                    () -> it.nextChunk());
            assertEquals(expected, e.getMessage());
        }
    }

    /**
     * Test that field names as they might appear in format statements
     * are correctly broken up by a {@link FieldNameIterator}
     * ({@code fieldnameiterator} in Python).
     *
     * We make this test using the Java API here, because it is
     * statically-typed, and Python iteration need not be working. In
     * Python it produces a {@code tuple} containing a first element and
     * iterator of succeeding elements, each described by a tuple.
     */
    @Nested
    @DisplayName("A stringlib.FieldNameIterator")
    class FieldNameIteratorTest {

        @Test
        @DisplayName("is a Python fieldnameiterator")
        void hasPythonType() {
            FieldNameIterator it = new FieldNameIterator("abc");
            PyType type = it.getType();
            assertEquals("fieldnameiterator", type.getName());
        }

        @Test
        @DisplayName("parses a simple name")
        void simpleName() {
            FieldNameIterator it = new FieldNameIterator("abc");
            assertEquals("abc", it.head());
            assertNull(it.nextChunk());
        }

        @Test
        @DisplayName("parses a simple number")
        void simpleNumber() {
            FieldNameIterator it = new FieldNameIterator("3");
            assertEquals(3, it.head());
            assertNull(it.nextChunk());
        }

        @Test
        @DisplayName("parses 'abc[0]'")
        void nameIndex() {
            FieldNameIterator it = new FieldNameIterator("abc[0]");
            assertEquals("abc", it.head());
            FieldNameIterator.Chunk chunk = it.nextChunk();
            assertEquals(0, chunk.value);
            assertFalse(chunk.is_attr);
            assertNull(it.nextChunk());
        }

        @Test
        @DisplayName("parses 'abc.def'")
        void nameDotName() {
            FieldNameIterator it = new FieldNameIterator("abc.def");
            assertEquals("abc", it.head());
            FieldNameIterator.Chunk chunk = it.nextChunk();
            assertEquals("def", chunk.value);
            assertTrue(chunk.is_attr);
            assertNull(it.nextChunk());
        }

        @Test
        @DisplayName("parses 'a[2].b'")
        void nameIndexDotName() {
            FieldNameIterator it = new FieldNameIterator("a[2].b");
            FieldNameIterator.Chunk chunk;
            assertEquals("a", it.head());
            chunk = it.nextChunk();
            assertEquals(2, chunk.value);
            assertFalse(chunk.is_attr);
            chunk = it.nextChunk();
            assertEquals("b", chunk.value);
            assertTrue(chunk.is_attr);
            assertNull(it.nextChunk());
        }

        @Test
        @DisplayName("parses '1.a[2].b[3]'")
        void numberDotNameIndexDotNameIndex() {
            FieldNameIterator it = new FieldNameIterator("1.a[2].b[3]");
            FieldNameIterator.Chunk chunk;
            assertEquals(1, it.head());
            chunk = it.nextChunk();
            assertEquals("a", chunk.value);
            chunk = it.nextChunk();
            assertEquals(2, chunk.value);
            assertFalse(chunk.is_attr);
            chunk = it.nextChunk();
            assertEquals("b", chunk.value);
            assertTrue(chunk.is_attr);
            chunk = it.nextChunk();
            assertEquals(3, chunk.value);
            assertFalse(chunk.is_attr);
            assertNull(it.nextChunk());
        }
    }
}
