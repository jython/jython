package org.python.core;

import static org.junit.Assert.assertArrayEquals;

import junit.framework.TestCase;

/**
 * Tests for ReflectedArgs, paying particular attention to the processing of arguments to match Java
 * methods where the signature accepts varargs ({@code T...}) and a sequence type is passed. Recall
 * that {@code T...} is just syntactic sugar for an array argument.
 * 
 * <p>
 * If a Java method that accepts varargs is called from Jython, the arguments are boxed into a
 * {@code PyList} first. This should be skipped only if there are already the correct number of
 * arguments and the final argument is a sequence type.
 * 
 * It is not only the final argument that must be checked for being a sequence. Otherwise, we shall
 * miss valid calls matching the underlying method.
 * 
 * E.g. in Java<pre>
 *  void foo(Object... args) {}
 * </pre>should be callable from Jython as<pre>
 *  foo(1, 2, [3, 4])
 * </pre>where the list and the numbers are all objects and passed as a single array to the varargs
 * method.
 * 
 * <p>
 * Similarly, if there are too few arguments, a java method such as<pre>
 * void bar(int a, int[] b, int... c) {}
 * </pre>should be callable from Jython as<pre>
 *  bar(1, [2, 3])
 * </pre>where the vararg argument is empty and [2, 3] is passed as b.
 * 
 * <p>
 * The only remaining ambiguity is where a vararg parameter can take a sequence type as an element,
 * e.g.<pre>
 *  void fizz(Object... args) {}
 * </pre>calling from Jython as<pre>
 *  fizz([1,2,3])
 * </pre>would be valid as either args being {@code [1,2,3]} or {@code [[1, 2, 3]]}. This ambiguity
 * is also present in Java but behaves as if it was the former.
 */
public class ReflectedArgsTest extends TestCase {

    /** Example methods for varargs processing. */
    static class Demo {

        public static void foo(Object... args) {}

        public static void bar(int i, int[] j, int... rest) {}

        public static void baz(Integer i, Integer[] j, Integer... rest) {}
    }

    /** {@code static void foo(Object... args)} */
    private static final ReflectedArgs FOO_SIGNATURE =
            new ReflectedArgs(null, new Class<?>[] {Object[].class}, Demo.class, true, true);

    private static void fooCheck(Object[] args, Object... expected) {
        // Expect one element that is an array of the expected values
        assertEquals(1, args.length);
        Object[] v = (Object[]) args[0];
        assertEquals(expected.length, v.length);
        for (int k = 0; k < expected.length; k++) {
            assertEquals(expected[k], v[k]);
        }
    }

    /** {@code static void bar(int i, int[] j, int... rest)} */
    private static final ReflectedArgs BAR_SIGNATURE = new ReflectedArgs(null,
            new Class<?>[] {int.class, int[].class, int[].class}, Demo.class, true, true);

    private static void barCheck(Object[] args, int i, int[] j, int... rest) {
        // Expect three elements conformant to the method arguments
        assertEquals(3, args.length);
        assertEquals(i, (int) args[0]);
        assertArrayEquals(j, (int[]) args[1]);
        assertArrayEquals(rest, (int[]) args[2]);
    }

    /** {@code static void baz(Integer i, Integer[] j, Integer... rest)} */
    private static final ReflectedArgs BAZ_SIGNATURE = new ReflectedArgs(null,
            new Class<?>[] {Integer.class, Integer[].class, Integer[].class}, Demo.class, true,
            true);

    private static void bazCheck(Object[] args, Integer i, Integer[] j, Integer... rest) {
        // Expect three elements conformant to the method arguments
        assertEquals(3, args.length);
        assertEquals(i, (Integer) args[0]);
        assertArrayEquals(j, (Integer[]) args[1]);
        assertArrayEquals(rest, (Integer[]) args[2]);
    }

    /*
     * If a function accepts Object Varargs (or Collection/array etc), the arguments should be boxed
     * even if the final arg is a PySequenceList if the number of arguments not equal to the
     * expected number of arguments.
     *
     * eg bar(1, [1, 2, 3]) should be valid for a call to void bar(int i, int[] j, int... rest) but
     * should be boxed as bar(1, [1, 2, 3], [])
     */

    /**
     * Calling {@code foo(Object... args)} as Python {@code foo(3, 4, ["bar"])} calls Java
     * {@code foo(3, 4, strlist)} where {@code strlist} is the representation of Python
     * {@code ["bar"]}.
     */
    public void testVarargsBoxedWithTooManyArgs() {
        // calling foo(Object... args) as foo(3, 4, ["bar"])
        PyList strlist = new PyList(new PyObject[] {Py.newString("bar")});
        PyObject[] pyArgs = {Py.newInteger(3), Py.newInteger(4), strlist};
        ReflectedCallData callData = new ReflectedCallData();
        assertTrue(FOO_SIGNATURE.matches(null, pyArgs, Py.NoKeywords, callData));
        fooCheck(callData.args, new Object[] {3, 4, strlist});
    }

    /**
     * Calling {@code bar(int a, int[] b, int... c)} as Python {@code bar(1, [2, 3])} calls Java
     * {@code bar(1, new int[]{2, 3})}.
     */
    public void testPrimitiveVarargsBoxedWithTooFewArguments() {
        // calling bar(int a, int[] b, int... c) as bar(1, [2, 3])
        PyObject[] ints = {Py.newInteger(2), Py.newInteger(3)};
        PyObject[] pyArgs = {Py.newInteger(1), new PyList(ints)};
        ReflectedCallData callData = new ReflectedCallData();
        assertTrue(BAR_SIGNATURE.matches(null, pyArgs, Py.NoKeywords, callData));
        barCheck(callData.args, 1, new int[] {2, 3});
    }

    /**
     * Calling {@code baz(Integer a, Integer[] b, Integer... c)} as Python {@code baz(1, [2, 3])}
     * calls Java {@code baz(1, new Integer[]{2, 3})}.
     */
    public void testVarargsBoxedWithTooFewArguments() {
        // calling baz(Integer a, Integer[] b, Integer... c) as bar(1, [2, 3])
        PyObject[] ints = {Py.newInteger(2), Py.newInteger(3)};
        PyObject[] pyArgs = {Py.newInteger(1), new PyList(ints)};
        ReflectedCallData callData = new ReflectedCallData();
        assertTrue(BAZ_SIGNATURE.matches(null, pyArgs, Py.NoKeywords, callData));
        bazCheck(callData.args, 1, new Integer[] {2, 3});
    }

    /**
     * Calling {@code foo(Object... args)} as Python {@code foo([1,2,3])} calls Java {@code foo(new
     * Object[]{1,2,3})}.
     */
    public void testVarargsNotBoxedWithCorrectArgs() {
        // calling foo(Object... args) as foo([1,2,3])
        PyObject[] ints = {Py.newInteger(1), Py.newInteger(2), Py.newInteger(3)};
        PyObject[] pyArgs = {new PyList(ints)};
        ReflectedCallData callData = new ReflectedCallData();
        assertTrue(FOO_SIGNATURE.matches(null, pyArgs, Py.NoKeywords, callData));
        fooCheck(callData.args, 1, 2, 3);
    }

    /**
     * Calling {@code foo(Object... args)} as Python {@code foo("foo", "bar")} calls Java
     * {@code foo("foo", "bar")}.
     */
    public void testVarargsBoxedWithNoSequences() {
        // calling foo(Object... args) as foo("foo", "bar")
        PyObject[] pyArgs = {Py.newString("foo"), Py.newString("bar")};
        ReflectedCallData callData = new ReflectedCallData();
        assertTrue(FOO_SIGNATURE.matches(null, pyArgs, Py.NoKeywords, callData));
        fooCheck(callData.args, "foo", "bar");
    }
}
