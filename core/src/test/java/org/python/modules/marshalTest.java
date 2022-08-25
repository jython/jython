// Copyright (c)2022 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.modules;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.python.core.Py;
import org.python.core.PyBytes;
import org.python.core.PyDict;
import org.python.core.PyList;
import org.python.core.PySequence;
import org.python.core.PyTuple;
import org.python.core.PyType;
import org.python.core.StopIteration;
import org.python.core.UnitTestSupport;
import org.python.modules.marshal.BytesReader;
import org.python.modules.marshal.BytesWriter;
import org.python.modules.marshal.Reader;
import org.python.modules.marshal.StreamReader;
import org.python.modules.marshal.StreamWriter;
import org.python.modules.marshal.Writer;
import org.python.core.stringlib.ByteArrayBuilder;

/**
 * Test reading (and to some extent writing) objects using the
 * marshal module. We are interested for now only in reading code
 * objects and their reference results.
 *
 * We test the Java API only, consistent with our interest in
 * reading code for execution.
 */
@DisplayName("Read and write objects with marshal")
class marshalTest extends UnitTestSupport {

    /**
     * Base of tests that read or write elementary values where a
     * reference is available serialised by CPython.
     */
    abstract static class AbstractElementTest {

        /**
         * Test cases for serialising 16-bit ints.
         *
         * @return the examples
         */
        static Stream<Arguments> int16() {
            return Stream.of( //
                    intArguments(0, bytes(0x00, 0x00)), //
                    intArguments(1, bytes(0x01, 0x00)), //
                    intArguments(-42, bytes(0xd6, 0xff)),
                    intArguments(Short.MAX_VALUE, bytes(0xff, 0x7f)));
        }

        /**
         * Test cases for serialising 32-bit ints.
         *
         * @return the examples
         */
        static Stream<Arguments> int32() {
            return Stream.of( //
                    intArguments(0, bytes(0x00, 0x00, 0x00, 0x00)),
                    intArguments(1, bytes(0x01, 0x00, 0x00, 0x00)),
                    intArguments(-42, bytes(0xd6, 0xff, 0xff, 0xff)),
                    intArguments(Integer.MAX_VALUE, bytes(0xff, 0xff, 0xff, 0x7f)));
        }

        /**
         * Test cases for serialising 64-bit ints.
         *
         * @return the examples
         */
        static Stream<Arguments> int64() {
            return Stream.of( //
                    longArguments(0, bytes(0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00)),
                    longArguments(1, bytes(0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00)),
                    longArguments(-42, bytes(0xd6, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff)),
                    longArguments(7450580596923828125L,
                            bytes(0x9d, 0x07, 0x10, 0xfa, 0x93, 0xc7, 0x65, 0x67)),
                    longArguments(Long.MAX_VALUE,
                            bytes(0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0x7f)));
        }

        /**
         * Test cases for serialising {@code BigInteger}s.
         *
         * @return the examples
         */
        static Stream<Arguments> bigint() {
            return Stream.of( //
                    arguments(new BigInteger("17557851463681"), //
                            bytes(0x03, 0x00, 0x00, 0x00, 0x01, 0x60, 0xff, 0x02, 0xe0, 0x3f)),
                    arguments(new BigInteger("35184372088832"), //
                            bytes(0x04, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01,
                                    0x00)),
                    arguments(new BigInteger("-2232232135326160725639168"), //
                            bytes(0xfa, 0xff, 0xff, 0xff, 0x00, 0x00, 0xfd, 0x20, 0xa7, 0x39, 0x4b,
                                    0x5f, 0x18, 0x0b, 0x3b, 0x00)));
        }

        /**
         * Wrap a {@code byte}, {@code short} or {@code int} expected value
         * and its marshalled form as a arguments for a test.
         *
         * @param expected result
         * @param bytes containing value to decode
         * @return arguments for the test
         */
        private static Arguments intArguments(int expected, byte[] bytes) {
            return arguments(expected, bytes);
        }

        /**
         * Wrap a {@code long} expected value and its marshalled form as a
         * arguments for a test.
         *
         * @param expected result
         * @param bytes containing value to decode
         * @return arguments for the test
         */
        private static Arguments longArguments(long expected, byte[] bytes) {
            assert bytes.length == 8;
            return arguments(expected, bytes);
        }
    }

    /**
     * Tests reading from a {@code ByteBuffer}, which is also how we
     * shall address objects with the Python buffer protocol
     * ({@link PyBytes} etc.), and native {@code byte[]}.
     */
    @Nested
    @DisplayName("Read elementary values from bytes")
    class ReadBytesElementary extends AbstractElementTest {

        @DisplayName("r.readShort()")
        @ParameterizedTest(name = "r.readShort() = {0}")
        @MethodSource("int16")
        void int16read(Integer expected, byte[] b) {
            Reader r = new BytesReader(b);
            assertEquals(expected, r.readShort());
        }

        @DisplayName("r.readInt()")
        @ParameterizedTest(name = "r.readInt() = {0}")
        @MethodSource("int32")
        void int32read(Integer expected, byte[] b) {
            Reader r = new BytesReader(b);
            assertEquals(expected, r.readInt());
        }

        @DisplayName("r.readLong()")
        @ParameterizedTest(name = "r.readInt() = {0}")
        @MethodSource("int64")
        void int64read(Long expected, byte[] b) {
            Reader r = new BytesReader(b);
            assertEquals(expected, r.readLong());
        }

        @DisplayName("r.readBigInteger()")
        @ParameterizedTest(name = "r.readBigInteger() = {0}")
        @MethodSource("bigint")
        void bigintread(BigInteger expected, byte[] b) {
            Reader r = new BytesReader(b);
            assertEquals(expected, r.readBigInteger());
        }
    }

    /**
     * Tests reading elementary values from an {@code InputStream},
     * which is also how we shall address file-like objects in Python,
     * and native Java input streams.
     */
    @Nested
    @DisplayName("Read elementary values from a stream")
    class ReadStreamElementary extends AbstractElementTest {

        @DisplayName("r.readShort()")
        @ParameterizedTest(name = "r.readShort() = {0}")
        @MethodSource("int16")
        void int16read(Integer expected, byte[] b) {
            Reader r = new StreamReader(new ByteArrayInputStream(b));
            assertEquals(expected, r.readShort());
        }

        @DisplayName("r.readInt()")
        @ParameterizedTest(name = "r.readInt() = {0}")
        @MethodSource("int32")
        void int32read(Integer expected, byte[] b) {
            Reader r = new StreamReader(new ByteArrayInputStream(b));
            assertEquals(expected, r.readInt());
        }

        @DisplayName("r.readLong()")
        @ParameterizedTest(name = "r.readInt() = {0}")
        @MethodSource("int64")
        void int64read(Long expected, byte[] b) {
            Reader r = new StreamReader(new ByteArrayInputStream(b));
            assertEquals(expected, r.readLong());
        }

        @DisplayName("r.readBigInteger()")
        @ParameterizedTest(name = "r.readBigInteger() = {0}")
        @MethodSource("bigint")
        void bigintread(BigInteger expected, byte[] b) {
            Reader r = new StreamReader(new ByteArrayInputStream(b));
            assertEquals(expected, r.readBigInteger());
        }
    }

    /**
     * Tests writing to a {@code ByteArrayBuilder}, which is how we
     * create a {@link PyBytes} serialising an object. In the test, we
     * recover a native {@code byte[]} to compare with the expected
     * bytes.
     */
    @Nested
    @DisplayName("Write elementary values to bytes")
    class WriteBytesElementary extends AbstractElementTest {

        @DisplayName("w.writeShort()")
        @ParameterizedTest(name = "w.writeShort({0})")
        @MethodSource("int16")
        void int16write(Integer v, byte[] expected) {
            ByteArrayBuilder b = new ByteArrayBuilder(2);
            Writer w = new BytesWriter(b, 4);
            w.writeShort(v);
            assertArrayEquals(expected, b.take());
        }

        @DisplayName("w.writeInt()")
        @ParameterizedTest(name = "w.writeInt({0})")
        @MethodSource("int32")
        void int32write(Integer v, byte[] expected) {
            ByteArrayBuilder b = new ByteArrayBuilder(4);
            Writer w = new BytesWriter(b, 4);
            w.writeInt(v);
            assertArrayEquals(expected, b.take());
        }

        @DisplayName("w.writeLong()")
        @ParameterizedTest(name = "w.writeInt({0})")
        @MethodSource("int64")
        void int64write(Long v, byte[] expected) {
            ByteArrayBuilder b = new ByteArrayBuilder(8);
            Writer w = new BytesWriter(b, 4);
            w.writeLong(v);
            assertArrayEquals(expected, b.take());
        }

        @DisplayName("w.writeBigInteger()")
        @ParameterizedTest(name = "w.writeBigInteger({0})")
        @MethodSource("bigint")
        void bigintwrite(BigInteger v, byte[] expected) {
            ByteArrayBuilder b = new ByteArrayBuilder();
            Writer w = new BytesWriter(b, 4);
            w.writeBigInteger(v);
            assertArrayEquals(expected, b.take());
        }
    }

    /**
     * Tests writing elementary values to an {@code OutputStream}, which
     * is also how we shall address file-like objects in Python, and
     * native Java input streams. In the test, we write to a
     * {@link ByteArrayOutputStream} and recover a native {@code byte[]}
     * to compare with the expected bytes.
     */
    @Nested
    @DisplayName("Write elementary values to a stream")
    class WriteStreamElementary extends AbstractElementTest {

        @DisplayName("w.writeShort()")
        @ParameterizedTest(name = "w.writeShort({0})")
        @MethodSource("int16")
        void int16write(Integer v, byte[] expected) {
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            Writer w = new StreamWriter(b, 4);
            w.writeShort(v);
            assertArrayEquals(expected, b.toByteArray());
        }

        @DisplayName("w.writeInt()")
        @ParameterizedTest(name = "w.writeInt({0})")
        @MethodSource("int32")
        void int32write(Integer v, byte[] expected) {
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            Writer w = new StreamWriter(b, 4);
            w.writeInt(v);
            assertArrayEquals(expected, b.toByteArray());
        }

        @DisplayName("w.writeLong()")
        @ParameterizedTest(name = "w.writeInt({0})")
        @MethodSource("int64")
        void int64write(Long v, byte[] expected) {
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            Writer w = new StreamWriter(b, 4);
            w.writeLong(v);
            assertArrayEquals(expected, b.toByteArray());
        }

        @DisplayName("w.writeBigInteger()")
        @ParameterizedTest(name = "w.writeBigInteger({0})")
        @MethodSource("bigint")
        void bigintwrite(BigInteger v, byte[] expected) {
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            Writer w = new StreamWriter(b, 4);
            w.writeBigInteger(v);
            assertArrayEquals(expected, b.toByteArray());
        }
    }

    /** Base of tests that read objects serialised by CPython. */
    abstract static class AbstractLoadTest {

        /**
         * Provide a stream of examples as parameter sets to the tests. In
         * each example, the expression is given (as documentation only)
         * that was originally evaluated by CPython, and the serialisation
         * of the result as bytes. The final argument is an equivalent
         * expression within this implementation of Python. Deserialising
         * the bytes should be equal to this argument.
         * <p>
         * The examples were generated programmatically from a list of the
         * expressions using the script at
         * {@code ~/build-tools/python/tool/marshal_test.py}.
         *
         * @return the examples for object loading tests.
         */
        static Stream<Arguments> objectLoadExamples() {
            return Stream.of( //
                    loadExample("None", // tc='N'
                            bytes(0x4e), Py.None),
                    loadExample("False", // tc='F'
                            bytes(0x46), false),
                    loadExample("True", // tc='T'
                            bytes(0x54), true),
                    loadExample("0", // tc='i'
                            bytes(0xe9, 0x00, 0x00, 0x00, 0x00), 0),
                    loadExample("1", // tc='i'
                            bytes(0xe9, 0x01, 0x00, 0x00, 0x00), 1),
                    loadExample("-42", // tc='i'
                            bytes(0xe9, 0xd6, 0xff, 0xff, 0xff), -42),
                    loadExample("2**31-1", // tc='i'
                            bytes(0xe9, 0xff, 0xff, 0xff, 0x7f), 2147483647),
                    loadExample("2047**4", // tc='l'
                            bytes(0xec, 0x03, 0x00, 0x00, 0x00, 0x01, 0x60, 0xff, 0x02, 0xe0, 0x3f),
                            new BigInteger("17557851463681")),
                    loadExample("2**45", // tc='l'
                            bytes(0xec, 0x04, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                                    0x01, 0x00),
                            new BigInteger("35184372088832")),
                    loadExample("-42**15", // tc='l'
                            bytes(0xec, 0xfa, 0xff, 0xff, 0xff, 0x00, 0x00, 0xfd, 0x20, 0xa7, 0x39,
                                    0x4b, 0x5f, 0x18, 0x0b, 0x3b, 0x00),
                            new BigInteger("-2232232135326160725639168")),
                    loadExample("0.", // tc='g'
                            bytes(0xe7, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00), 0x0.0p+0),
                    loadExample("1.", // tc='g'
                            bytes(0xe7, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0xf0, 0x3f),
                            0x1.0000000000000p+0),
                    loadExample("-42.", // tc='g'
                            bytes(0xe7, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x45, 0xc0),
                            -0x1.5000000000000p+5),
                    loadExample("1e42", // tc='g'
                            bytes(0xe7, 0x61, 0xa0, 0xe0, 0xc4, 0x78, 0xf5, 0xa6, 0x48),
                            0x1.6f578c4e0a061p+139),
                    loadExample("1.8e300", // tc='g'
                            bytes(0xe7, 0xa6, 0x36, 0xcd, 0xe0, 0x9c, 0x80, 0x45, 0x7e),
                            0x1.5809ce0cd36a6p+997),
                    loadExample("1.12e-308", // tc='g'
                            bytes(0xe7, 0xd7, 0xb2, 0x64, 0x01, 0xbd, 0x0d, 0x08, 0x00),
                            0x0.80dbd0164b2d7p-1022),
                    loadExample("float.fromhex('0x1.fffffffffffffp1023')", // tc='g'
                            bytes(0xe7, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xef, 0x7f),
                            0x1.fffffffffffffp+1023),
                    loadExample("float.fromhex('-0x1.p-1022')", // tc='g'
                            bytes(0xe7, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x10, 0x80),
                            -0x1.0000000000000p-1022),
                    loadExample("float('inf')", // tc='g'
                            bytes(0xe7, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0xf0, 0x7f),
                            Double.POSITIVE_INFINITY),
                    loadExample("float('-inf')", // tc='g'
                            bytes(0xe7, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0xf0, 0xff),
                            Double.NEGATIVE_INFINITY),
                    loadExample("float('nan')", // tc='g'
                            bytes(0xe7, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0xf8, 0x7f),
                            Double.NaN),
                    loadExample("'hello'", // tc='Z'
                            bytes(0xda, 0x05, 0x68, 0x65, 0x6c, 0x6c, 0x6f), "hello"),
                    loadExample("'sæll'", // tc='t'
                            bytes(0x74, 0x05, 0x00, 0x00, 0x00, 0x73, 0xc3, 0xa6, 0x6c, 0x6c),
                            "sæll"),
                    loadExample("'🐍'", // tc='t'
                            bytes(0x74, 0x04, 0x00, 0x00, 0x00, 0xf0, 0x9f, 0x90, 0x8d), "🐍"),
                    loadExample("()", // tc=')'
                            bytes(0xa9, 0x00), Py.tuple()),
                    loadExample("(sa,sa,sa)", // tc=')'
                            bytes(0xa9, 0x03, 0xda, 0x05, 0x68, 0x65, 0x6c, 0x6c, 0x6f, 0x72, 0x01,
                                    0x00, 0x00, 0x00, 0x72, 0x01, 0x00, 0x00, 0x00),
                            Py.tuple("hello", "hello", "hello")),
                    loadExample("(sb,sb,t,t)", // tc=')'
                            bytes(0xa9, 0x04, 0xf5, 0x05, 0x00, 0x00, 0x00, 0x73, 0xc3, 0xa6, 0x6c,
                                    0x6c, 0x72, 0x01, 0x00, 0x00, 0x00, 0xa9, 0x03, 0xe9, 0x01,
                                    0x00, 0x00, 0x00, 0xe9, 0x02, 0x00, 0x00, 0x00, 0xe9, 0x03,
                                    0x00, 0x00, 0x00, 0x72, 0x02, 0x00, 0x00, 0x00),
                            Py.tuple("sæll", "sæll", Py.tuple(1, 2, 3), Py.tuple(1, 2, 3))),
                    loadExample("[]", // tc='['
                            bytes(0xdb, 0x00, 0x00, 0x00, 0x00), new PyList(List.of())),
                    loadExample("[sa]", // tc='['
                            bytes(0xdb, 0x01, 0x00, 0x00, 0x00, 0xda, 0x05, 0x68, 0x65, 0x6c, 0x6c,
                                    0x6f),
                            new PyList(List.of("hello"))),
                    loadExample("[sa, 2, t]", // tc='['
                            bytes(0xdb, 0x03, 0x00, 0x00, 0x00, 0xda, 0x05, 0x68, 0x65, 0x6c, 0x6c,
                                    0x6f, 0xe9, 0x02, 0x00, 0x00, 0x00, 0xa9, 0x03, 0xe9, 0x01,
                                    0x00, 0x00, 0x00, 0x72, 0x02, 0x00, 0x00, 0x00, 0xe9, 0x03,
                                    0x00, 0x00, 0x00),
                            new PyList(List.of("hello", 2, Py.tuple(1, 2, 3)))),
                    loadExample("{}", // tc='{'
                            bytes(0xfb, 0x30), PyDict.fromKeyValuePairs()),
                    loadExample("{sa:sb}", // tc='{'
                            bytes(0xfb, 0xda, 0x05, 0x68, 0x65, 0x6c, 0x6c, 0x6f, 0xf5, 0x05, 0x00,
                                    0x00, 0x00, 0x73, 0xc3, 0xa6, 0x6c, 0x6c, 0x30),
                            PyDict.fromKeyValuePairs(Py.tuple("hello", "sæll"))),
                    loadExample("dict(python=su)", // tc='{'
                            bytes(0xfb, 0xda, 0x06, 0x70, 0x79, 0x74, 0x68, 0x6f, 0x6e, 0xf5, 0x04,
                                    0x00, 0x00, 0x00, 0xf0, 0x9f, 0x90, 0x8d, 0x30),
                            PyDict.fromKeyValuePairs(Py.tuple("python", "🐍"))),
                    loadExample("{sa:1, sb:2, su:t}", // tc='{'
                            bytes(0xfb, 0xda, 0x05, 0x68, 0x65, 0x6c, 0x6c, 0x6f, 0xe9, 0x01, 0x00,
                                    0x00, 0x00, 0xf5, 0x05, 0x00, 0x00, 0x00, 0x73, 0xc3, 0xa6,
                                    0x6c, 0x6c, 0xe9, 0x02, 0x00, 0x00, 0x00, 0xf5, 0x04, 0x00,
                                    0x00, 0x00, 0xf0, 0x9f, 0x90, 0x8d, 0xa9, 0x03, 0x72, 0x02,
                                    0x00, 0x00, 0x00, 0x72, 0x04, 0x00, 0x00, 0x00, 0xe9, 0x03,
                                    0x00, 0x00, 0x00, 0x30),
                            PyDict.fromKeyValuePairs(Py.tuple("hello", 1), Py.tuple("sæll", 2),
                                    Py.tuple("🐍", Py.tuple(1, 2, 3)))),

                    // Hand-generated examples

                    loadExample("StopIteration", // tc='S'
                            bytes('S'), StopIteration.TYPE));

        }

        /**
         * A list referring to itself: {@code listself = [1, listself, 3]},
         * used for tesing the marshalling of a self-referential list.
         */
        static final PyList LISTSELF = listself();

        /** @return {@code listself = [1, listself, 3]} */
        private static PyList listself() {
            PyList list = new PyList(List.of(1, 2, 3));
            list.set(1, list);
            return list;
        }

        /** The result of marshalling {@code (listself,4)}. */
        static final byte[] LISTSELF_BYTES = bytes( //
                0xa9, 0x02, 0xdb, 0x03, 0x00, 0x00, 0x00, 0xe9, 0x01, 0x00, 0x00, 0x00, 0x72, 0x01,
                0x00, 0x00, 0x00, 0xe9, 0x03, 0x00, 0x00, 0x00, 0xe9, 0x04, 0x00, 0x00, 0x00);

        /**
         * Construct a set of test arguments for a single test of load, and
         * a reference result provided by the caller.
         *
         * @param expression to identify the test
         * @param bytes to deserialise
         * @param expected results to expect
         */
        private static Arguments loadExample(String name, byte[] bytes, Object expected) {
            return arguments(name, bytes, expected);
        }
    }

    /**
     * Tests reading a complete object from a {@code PyBytes}, using the
     * Python buffer protocol.
     */
    @Nested
    @DisplayName("Read object from bytes-like")
    class MarshalLoadBytesTest extends AbstractLoadTest {

        @DisplayName("loads(b)")
        @ParameterizedTest(name = "loads(b) = {0}")
        @MethodSource("objectLoadExamples")
        void loadsTest(String name, byte[] bytes, Object expected) {
            Object r = marshal.loads(new PyBytes(bytes));
            assertPythonType(PyType.of(expected), r);
            assertPythonEquals(expected, r);
        }

        @DisplayName("loads((listself,4))")
        @Test
        void loadsListSelf() throws Throwable {
            Object r = marshal.loads(new PyBytes(LISTSELF_BYTES));
            assertPythonType(PyTuple.TYPE, r);
            // We can't simply compare values, r with expected
            // assertPythonEquals(expected, r);
            PyList list = (PyList)PySequence.getItem(r, 0);
            // Item 1 of this list should be the list itself
            PyList list1 = (PyList)PySequence.getItem(list, 1);
            assertSame(list, list1);
            assertPythonEquals(4, PySequence.getItem(r, 1));
        }
    }

    /**
     * Tests reading a complete object from a {@code byte[]}, wrapping
     * it as a stream.
     */
    @Nested
    @DisplayName("Read object from a stream")
    class MarshalLoadStreamTest extends AbstractLoadTest {

        @DisplayName("load(f)")
        @ParameterizedTest(name = "load(f) = {0}")
        @MethodSource("objectLoadExamples")
        void loadsTest(String name, byte[] b, Object expected) {
            Object r = marshal.load(new ByteArrayInputStream(b));
            assertPythonType(PyType.of(expected), r);
            assertPythonEquals(expected, r);
        }

        @DisplayName("loads((listself,4))")
        @Test
        void loadsListSelf() throws Throwable {
            Object r = marshal.load(new ByteArrayInputStream(LISTSELF_BYTES));
            assertPythonType(PyTuple.TYPE, r);
            // We can't simply compare values, r with expected
            // assertPythonEquals(expected, r);
            PyList list = (PyList)PySequence.getItem(r, 0);
            // Item 1 of this list should be the list itself
            PyList list1 = (PyList)PySequence.getItem(list, 1);
            assertSame(list, list1);
            assertPythonEquals(4, PySequence.getItem(r, 1));
        }
    }

    // Support methods ------------------------------------------------

    /**
     * Copy values to a new {@code byte[]} casting each to a
     * {@code byte}.
     *
     * @param v to convert to {@code byte}
     * @return the byte array of cast values
     */
    private static byte[] bytes(int... v) {
        byte[] b = new byte[v.length];
        for (int i = 0; i < b.length; i++) { b[i] = (byte)v[i]; }
        return b;
    }
}
