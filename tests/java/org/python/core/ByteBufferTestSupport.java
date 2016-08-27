package org.python.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Common apparatus for tests involving <code>byte[]</code> and <code>java.nio.ByteBuffer</code>
 * material, in particular the tests of {@link PyBuffer} implementations. A test case that extends
 * this class will be equipped with additional assertion methods and a class to represent
 * <code>byte[]</code> test material in several forms.
 */
public class ByteBufferTestSupport {

    /**
     * Class to hold test material representing the same sequence of values 0..255 in several
     * different ways.
     */
    protected static class ByteMaterial {

        /** Length in bytes (length of every array in this material). */
        final int length;
        /** The byte values. */
        byte[] bytes;
        /** The byte values individually as ints. */
        int[] ints;
        /** The byte values treated as characters (unicode < 256). */
        String string;

        /** Construct from int array. */
        public ByteMaterial(int[] a) {
            ints = a.clone();
            length = replicate();
        }

        /** Construct from String. */
        public ByteMaterial(String s) {
            ints = new int[s.length()];
            for (int i = 0; i < ints.length; i++) {
                ints[i] = 0xff & s.charAt(i);
            }
            length = replicate();
        }

        /** Construct from byte array. */
        public ByteMaterial(byte[] b) {
            ints = new int[b.length];
            for (int i = 0; i < ints.length; i++) {
                ints[i] = 0xff & b[i];
            }
            length = replicate();
        }

        /** Construct from pattern on values (used modulo 256). */
        public ByteMaterial(int start, int count, int inc) {
            ints = new int[count];
            int x = start;
            for (int i = 0; i < count; i++) {
                ints[i] = x;
                x = (x + inc) & 0xff;
            }
            length = replicate();
        }

        /**
         * Once the integer value array {@link #ints} has been initialised, fill the others from it.
         *
         * @return length of (all) arrays in units
         */
        private int replicate() {
            int n = ints.length;
            bytes = new byte[n];
            StringBuilder sb = new StringBuilder(n);

            for (int i = 0; i < n; i++) {
                int x = ints[i];
                bytes[i] = (byte)x;
                sb.appendCodePoint(x);
            }
            string = sb.toString();
            return n;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder(100);
            sb.append("byte[").append(length).append("]={ ");
            for (int i = 0; i < length; i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                if (i >= 5) {
                    sb.append(" ...");
                    break;
                } else {
                    sb.append(ints[i]);
                }
            }
            sb.append(" }");
            return sb.toString();
        }

        /**
         * @return a copy of the bytes array (that the client is allowed to modify)
         */
        byte[] getBytes() {
            return bytes.clone();
        }

        /**
         * @return a buffer on a copy of the bytes array (that the client is allowed to modify)
         */
        ByteBuffer getBuffer() {
            return ByteBuffer.wrap(getBytes());
        }

        /**
         * Create material equivalent to a slice of this material. This may act as a reference
         * result for testing slice operations.
         *
         * @param start first byte-index to include
         * @param length number of items
         * @param stride between byte-indices
         * @return ByteMaterial in which the arrays are a slice of this one
         */
        ByteMaterial slice(int start, int length, int stride) {
            return new ByteMaterial(sliceBytes(bytes, 1, start, length, stride));
        }

        /**
         * Create material equivalent to a slice of this material. This may act as a reference
         * result for testing slice operations.
         *
         * @param start first byte-index to include
         * @param itemsize number of consecutive bytes treated as one item
         * @param length number of items
         * @param stride between byte-indices
         * @return ByteMaterial in which the arrays are a slice of this one
         */
        ByteMaterial slice(int itemsize, int start, int length, int stride) {
            return new ByteMaterial(sliceBytes(bytes, itemsize, start, length, stride));
        }
    }

    /**
     * Create a byte array that is a strided copy of the one passed in. The specifications are
     * assumed correct for the size of that array.
     *
     * @param b source array
     * @param start first index to include
     * @param length number of indices
     * @param stride between indices
     * @return slice of b
     */
    protected static byte[] sliceBytes(byte[] b, int start, int length, int stride) {
        return sliceBytes(b, 1, start, length, stride);
    }

    /**
     * Create a multi-byte item array that is a strided copy of the one passed in. The
     * specifications are assumed correct for the size of that array.
     *
     * @param b source array
     * @param itemsize number of consecutive bytes treated as one item
     * @param start first byte-index to include
     * @param length number of indices to visit (items to copy)
     * @param stride between byte-indices
     * @return slice of b
     */
    protected static byte[] sliceBytes(byte[] b, int itemsize, int start, int length, int stride) {
        byte[] a = new byte[length];
        for (int i = 0, j = start; i < length; i++, j += stride) {
            for (int k = 0; k < itemsize; k++) {
                a[i + k] = b[j + k];
            }
        }
        return a;
    }

    /**
     * Custom assert method comparing the bytes in an NIO {@link ByteBuffer} to those in a byte
     * array, when that <code>ByteBuffer</code> is obtained from a contiguous <code>PyBuffer</code>.
     * Let <code>bb[i]</code> denote <code>bb.get(bb.position()+i)</code>, by analogy with a C
     * pointer. It is required that <code>bb[k] == expected[k]</code>, for every index
     * <code>k</code> in <code>expected</code>. If not, a <code>fail()</code> is declared.
     *
     * @param message to issue on failure
     * @param expected expected byte array
     * @param bb result to test
     */
    static void assertBytesEqual(String message, byte[] expected, ByteBuffer bb) {
        // Use the position-advancing buffer get()
        byte[] actual = new byte[expected.length];
        bb.get(actual);
        assertBytesEqual(message, expected, actual);
    }

    /**
     * Custom assert method comparing the bytes in an NIO {@link ByteBuffer} to those in a byte
     * array, when that <code>ByteBuffer</code> is obtained from a striding <code>PyBuffer</code>.
     * Let <code>bb[i]</code> denote <code>bb.get(bb.position()+i)</code>, by analogy with a C
     * pointer. It is required that <code>bb[k*stride] == expected[k]</code>, for every index
     * <code>k</code> in <code>expected</code>. If not, a <code>fail()</code> is declared.
     *
     * @param message to issue on failure
     * @param expected expected byte array
     * @param bb result to test
     * @param stride in the buffer <code>bb</code>
     */
    static void assertBytesEqual(String message, byte[] expected, ByteBuffer bb, int stride) {
        assertBytesEqual(message, expected, 0, expected.length, bb, stride);
    }

    /**
     * Custom assert method comparing the bytes in an NIO {@link ByteBuffer} to those in a byte
     * array, when that <code>ByteBuffer</code> is obtained from a striding <code>PyBuffer</code>.
     * Let <code>bb[i]</code> denote <code>bb.get(bb.position()+i)</code>, by analogy with a C
     * pointer. It is required that <code>bb[k*stride] == expected[expectedStart+k]</code>, for
     * <code>k=0</code> to <code>n-1</code>. If not, a <code>fail()</code> is declared.
     *
     * @param message to issue on failure
     * @param expected expected byte array
     * @param expectedStart where to start the comparison in <code>expected</code>
     * @param n number of bytes to test
     * @param bb result to test
     * @param stride in the buffer <code>bb</code>
     */
    static void assertBytesEqual(String message, byte[] expected, int expectedStart, int n,
            ByteBuffer bb, int stride) {
        // Note that this approach leaves the buffer position unmodified
        int p = bb.position();
        byte[] actual = new byte[n];
        for (int k = 0; k < n; k++, p += stride) {
            actual[k] = bb.get(p);
        }
        assertBytesEqual(message, expected, expectedStart, n, actual, 0);
    }

    /**
     * Custom assert method comparing byte arrays: values in <code>actual[]</code> must match all
     * those in <code>expected[]</code>, and they must be the same length.
     *
     * @param message to issue on failure
     * @param expected expected byte array
     * @param actual result to test
     */
    static void assertBytesEqual(String message, byte[] expected, byte[] actual) {
        assertEquals(message + " (array size)", expected.length, actual.length);
        assertBytesEqual(message, expected, 0, expected.length, actual, 0, 1);
    }

    /**
     * Custom assert method comparing byte arrays. It is required that
     * <code>actual[k] == expected[k]</code>, for <code>k=0</code> to <code>expected.length-1</code>
     * . If not, a <code>fail()</code> is declared.
     *
     * @param message to issue on failure
     * @param expected expected byte array
     * @param actual result to test
     * @param actualStart where to start the comparison in <code>actual</code>
     */
    static void assertBytesEqual(String message, byte[] expected, byte[] actual, int actualStart) {
        assertBytesEqual(message, expected, 0, expected.length, actual, actualStart, 1);
    }

    /**
     * Custom assert method comparing byte arrays. It is required that
     * <code>actual[actualStart+k] == expected[expectedStart+k]</code>, for <code>k=0</code> to
     * <code>n-1</code>. If not, a <code>fail()</code> is declared.
     *
     * @param message to issue on failure
     * @param expected expected byte array
     * @param expectedStart where to start the comparison in <code>expected</code>
     * @param n number of bytes to test
     * @param actual result to test
     * @param actualStart where to start the comparison in <code>actual</code>
     */
    protected static void assertBytesEqual(String message, byte[] expected, int expectedStart,
            int n, byte[] actual, int actualStart) {
        assertBytesEqual(message, expected, expectedStart, n, actual, actualStart, 1);
    }

    /**
     * Custom assert method comparing byte arrays. It is required that
     * <code>actual[actualStart+k*stride] == expected[expectedStart+k]</code>, for <code>k=0</code>
     * to <code>n-1</code>. If not, a <code>fail()</code> is declared.
     *
     * @param message to issue on failure
     * @param expected expected byte array
     * @param expectedStart where to start the comparison in <code>expected</code>
     * @param n number of bytes to test
     * @param actual result to test
     * @param actualStart where to start the comparison in <code>actual</code>
     * @param stride spacing of bytes in <code>actual</code> array
     */
    static void assertBytesEqual(String message, byte[] expected, int expectedStart, int n,
            byte[] actual, int actualStart, int stride) {

        if (actualStart < 0) {
            fail(message + " (start<0 in result)");

        } else if (expectedStart < 0) {
            fail(message + " (start<0 in expected result): bug in test?");

        } else if (actualStart + (n - 1) * stride + 1 > actual.length) {
            fail(message + " (result too short)");

        } else if (expectedStart + n > expected.length) {
            fail(message + " (expected result too short): bug in test?");

        } else {
            // Should be safe to compare the values
            int i = actualStart, j, jLimit = expectedStart + n;
            for (j = expectedStart; j < jLimit; j++) {
                if (actual[i] != expected[j]) {
                    break;
                }
                i += stride;
            }

            // If we stopped early, diagnose the problem
            if (j < jLimit) {
                byte[] a = Arrays.copyOfRange(actual, actualStart, actualStart + n);
                byte[] e = Arrays.copyOfRange(expected, expectedStart, expectedStart + n);
                System.out.println("  expected:" + Arrays.toString(e));
                System.out.println("    actual:" + Arrays.toString(a));
                System.out.println("  _actual_:" + Arrays.toString(actual));
                fail(message + " (byte at " + j + ")");
            }
        }
    }

    /**
     * Customised assert method comparing a int arrays: values in the actual value starting at
     * actual[offset] must match all those in expected[], and there must be enough of them.
     *
     * @param message to issue on failure
     * @param expected expected array
     * @param actual result to test
     * @param offset where to start the comparison in actual
     */
    static void assertIntsEqual(String message, int[] expected, int[] actual, int offset) {
        int n = expected.length;
        if (offset < 0) {
            fail(message + " (offset<0)");
        } else if (offset + n > actual.length) {
            fail(message + " (too short)");
        } else {
            // Should be safe to compare the values
            int i = offset, j;
            for (j = 0; j < n; j++) {
                if (actual[i++] != expected[j]) {
                    break;
                }
            }
            if (j < n) {
                System.out.println("  expected:" + Arrays.toString(expected));
                System.out.println("    actual:" + Arrays.toString(actual));
                fail(message + " (int at " + j + ")");
            }
        }
    }

    /**
     * Customised assert method comparing a int arrays: int in the actual value must match all those
     * in expected[], and there must be the same number of them.
     *
     * @param message to issue on failure
     * @param expected expected array
     * @param actual result to test
     */
    protected static void assertIntsEqual(String message, int[] expected, int[] actual) {
        int n = expected.length;
        assertEquals(message, n, actual.length);
        // Should be safe to compare the values
        int j;
        for (j = 0; j < n; j++) {
            if (actual[j] != expected[j]) {
                break;
            }
        }
        if (j < n) {
            System.out.println("  expected:" + Arrays.toString(expected));
            System.out.println("    actual:" + Arrays.toString(actual));
            fail(message + " (int at " + j + ")");
        }
    }

    /**
     * Method comparing byte arrays after a read (or view creation) operation involving a slice.
     * <p>
     * The invariant asserted must be explained carefully because of its generality. Suppose there
     * to be three arrays of bytes <i>a</i>, <i>b</i> and <i>c</i>. Let <i>a</i> represent the state
     * of some byte storage of length <i>L</i> before an operation. Let <i>b</i> represent the state
     * of the same storage after an operation. Let <i>c</i> be related as follows.
     * <p>
     * <i>c</i> is the result, representing <i>n</i> blocks of <i>u</i> bytes copied from the
     * storage, the <i>k</i>th block starting at position <i>s+kp</i> in the storage and at
     * <i>t+ku</i> in <i>c</i>. <i>c</i> is of length <i>M&ge;nu</i>, and we assume
     * <i>0&le;s+kp&lt;L</i>. After a read operation, it is required that:
     * <ol>
     * <li><i>c[t+iu+j] = b[s+ip+j]</i> for <i>0&le;i&lt;n</i> and <i>0&le;j&lt;u</i>, and</li>
     * <li><i>a[k] = b[k]</i> for <i>0&le;k&lt;L</i>.
     * </ol>
     * <p>
     *
     * @param a storage state before the operation (typically reference data)
     * @param b storage state after the operation (typically from object under test)
     * @param c bytes read
     * @param t index in <code>c</code> of the start byte of item 0
     * @param n number of items
     * @param u number of consecutive bytes per item
     * @param s index in <code>b</code> of the start byte of item 0
     * @param p the distance in <code>b</code> between the start bytes of successive items
     */
    static void checkReadCorrect(byte[] a, byte[] b, byte[] c, int t, int n, int u, int s, int p) {
        // Check the b is the same as a
        assertEquals("Storage size differs from reference", a.length, b.length);
        for (int k = 0; k < b.length; k++) {
            if (a[k] != b[k]) {
                fail("Stored data changed during read");
            }
        }
        // Check slice read out
        checkEqualInSlice(b, c, t, n, u, s, p);
    }

    /**
     * Method comparing byte arrays where a change operation has taken place involving a slice.
     * <p>
     * The invariant asserted must be explained carefully because of its generality. Suppose there
     * to be three arrays of bytes <i>a</i>, <i>b</i> and <i>c</i>. Let <i>a</i> represent the state
     * of some byte storage of length <i>L</i> before an operation. Let <i>b</i> represent the state
     * of the same storage after an operation. Let <i>c</i> be related as follows.
     * <p>
     * <i>c</i> is the source, contaning at index <i>t</i>, <i>n</i> blocks of <i>u</i> bytes copied
     * to the storage. As before, the <i>k</i>th block starts at position <i>s+kp</i> in the storage
     * and at <i>t+ku</i> in <i>c</i>. <i>c</i> is of length <i>M&ge;t+nu</i>, and we assume
     * <i>0&le;s+kp&lt;L</i>. After a write operation, it is required that:
     * <ol>
     * <li><i>c[t+iu+j] = b[s+ip+j]</i> for <i>0&le;i&lt;n</i> and <i>0&le;j&lt;u</i>, and</li>
     * <li><i>a[k] = b[k]</i> for <i>0&le;k&lt;L</i> and <i>k&ne;s+ip+j</i> for any choice of <i</i>
     * and <i>j</i> where <i>0&le;i&lt;n</i> and <i>0&le;j&lt;u</i>.
     * </ol>
     * Note that the first of these is the same as for a read and the second requires equality
     * "everywhere else".
     *
     * @param a storage state before the operation (typically reference data)
     * @param b storage state after the operation (typically from object under test)
     * @param c bytes written
     * @param t index in <code>c</code> of the start byte of item 0
     * @param n number of items
     * @param u number of consecutive bytes per item
     * @param s index in <code>b</code> of the start byte of item 0
     * @param p the distance in <code>b</code> between the start bytes of successive items
     */
    static void checkWriteCorrect(byte[] a, byte[] b, byte[] c, int t, int n, int u, int s, int p) {
        assertEquals("Stored size has changed", a.length, b.length);
        checkEqualInSlice(b, c, t, n, u, s, p);
        checkUnchangedElsewhere(a, b, n, u, s, p);
    }

    /**
     * Method comparing bytes in a slice pattern of one byte array to bytes taken consecutively in
     * another array. This is needed in testing when bytes have been copied into or out of an array
     * slice.
     * <p>
     * Let <i>b</i> represent the state of the byte storage of length <i>L</i> after the copy
     * operation (the sliced array). Let <i>c</i> be a source or destination array, a section of
     * which at index <i>t</i> represents <i>n</i> blocks of <i>u</i> bytes copied to or from the
     * storage. <i>c</i> is of length at least <i>t+nu</i>. The <i>k</i>th block starts at position
     * <i>s+kp</i> in the storage <i>b</i> and at <i>t+ku</i> in <i>c</i>, and we assume
     * <i>0&le;s+kp&lt;L</i>. After a write operation, it is required that: <i>c[t+iu+j] =
     * b[s+ip+j]</i> for <i>0&le;i&lt;n</i> and <i>0&le;j&lt;u</i>.
     *
     *
     * @param b storage state after the operation (typically from object under test)
     * @param c bytes written
     * @param t index in <code>c</code> of the start byte of item 0
     * @param n number of items
     * @param u number of consecutive bytes per item
     * @param s index in <code>b</code> of the start byte of item 0
     * @param p the distance in <code>b</code> between the start bytes of successive items
     */
    static void checkEqualInSlice(byte[] b, byte[] c, int t, int n, int u, int s, int p) {
        // Check correct use of the test
        checkSliceArgs(b, c, t, n, u, s, p);

        // Check the data in copied units (and p-u bytes following)
        for (int i = 0; i < n; i++) {
            int ci = t + i * u, bi = s + i * p;
            for (int j = 0; j < u; j++, bi++, ci++) {
                // Compare corresponding bytes of this unit in c and b
                if (c[ci] != b[bi]) {
                    fail(String.format("contiguous data at %d not equal to buffer at %d", ci, bi));
                }
            }
        }
    }

    /**
     * Method comparing the before and after state of the parts of a byte array that should be
     * untouched where a change operation has taken place involving a slice.
     * <p>
     * Let <i>a</i> represent the state of some byte storage of length <i>L</i> before an operation.
     * Let <i>b</i> represent the state of the same storage after an operation. After a write
     * operation, it is required that: <i>a[k] = b[k]</i> for <i>0&le;k&lt;L</i> and
     * <i>k&ne;s+ip+j</i> for any choice of <i</i> and <i>j</i> where <i>0&le;i&lt;n</i> and
     * <i>0&le;j&lt;u</i>.
     * <p>
     * Note that requires equality "everywhere else" than in the slice defined by <i>n</i> units of
     * size <i>u</i> starting at <i>s</i>.
     *
     * @param a storage state before the operation (typically reference data)
     * @param b storage state after the operation (typically from object under test)
     * @param n number of items
     * @param u number of consecutive bytes per item
     * @param s index in <code>b</code> of the start byte of item 0
     * @param p the distance in <code>b</code> between the start bytes of successive items
     */
    static void checkUnchangedElsewhere(byte[] a, byte[] b, int n, int u, int s, int p) {
        // Check correct use of the test
        assertEquals("Stored size has changed", a.length, b.length);
        assertFalse("Unit size exceeds spacing", u > p && u + p > 0);
        String bufferChangedAt = "buffer changed at %d (outside slice)";

        int absp, low, high;

        if (n < 1) {
            // No elements: check whole array.
            absp = low = high = 0;
        } else if (p >= 0) {
            // Stride is forwards in the range (easy case)
            absp = p;
            // Lowest byte index in the data is byte 0 of first unit in slice
            low = s;
            // One after highest byte index is just beyond last byte of last unit in slice
            high = s + (n - 1) * p + u;
        } else {
            // p<0: stride is backwards in the range (delicate case)
            absp = -p;
            // Lowest byte index in the data is byte 0 of last unit in slice
            low = s + (n - 1) * p;
            // One after highest byte index is just beyond last byte of first unit in slice
            high = s + u;
        }

        // Visit each copied unit (from low to high byte index) except the highest.
        for (int i = 0; i < n - 1; i++) {
            int bi = low + i * absp + u;
            // Check there was no change to the absp-u bytes following unit in b
            for (int j = u; j < absp; j++, bi++) {
                if (b[bi] != a[bi]) {
                    fail(String.format(bufferChangedAt, bi));
                }
            }
        }

        // Check that b[0:low] is unchanged
        for (int k = 0; k < low; k++) {
            if (b[k] != a[k]) {
                fail(String.format(bufferChangedAt, k));
            }
        }

        // Check that [high:] is unchanged
        for (int k = Math.max(high, 0); k < b.length; k++) {
            if (b[k] != a[k]) {
                fail(String.format(bufferChangedAt, k));
            }
        }
    }

    /** Common code for <code>checkReadCorrect</code> and <code>checkWriteCorrect</code>. */
    private static void checkSliceArgs(byte[] b, byte[] c, int t, int n, int u, int s, int p) {
        // Check correct use of the test
        assertFalse("Slice data less than n units", c.length < t + n * u);
        assertFalse("Slice data exceeds destination", n * u > b.length);
        assertFalse("Unit size exceeds spacing", u > p && u + p > 0);
    }

}