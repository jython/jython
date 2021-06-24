package org.python.core;

import junit.framework.TestCase;

/**
 * Tests for ReflectedArgs.
 */
public class ReflectedArgsTest extends TestCase {
    static class Demo {
        public static void foo(Object... args) {}
        public static void bar(int i, int[] j, int... rest) {}
    }

    /*
     * If a function accepts Object Varargs (or Collection/array etc), the arguments
     * should be boxed even if the final arg is a PySequenceList if the number of arguments
     * not equal to the expected number of arguments.
     *
     * eg foo(1, [1, 2, 3]) should be valid for a call to
     * void foo(int i, int[] j, int... rest)
     * but should be boxed as foo(1, [1, 2, 3], [])
     */

    public void testVarargsBoxedWithTooManyArgs() {
        // calling foo(Object... args) as foo(3, 4, ["bar"])
        ReflectedArgs args = new ReflectedArgs(null, new Class<?>[] {Object[].class}, Demo.class, true, true);
        PyObject[] varargs = new PyObject[] {Py.newString("foo"), Py.newString("bar") };
        PyObject[] argList = new PyObject[] {Py.newInteger(3), Py.newInteger(4), new PyList(varargs)};
        assertTrue(args.matches(null, argList, new String[0], new ReflectedCallData()));
    }

    public void testVarargsBoxedWithTooFewArguments() {
        // calling bar(int a, int[] b, int... c) as bar(1, [2, 3])
        ReflectedArgs args = new ReflectedArgs(null, new Class<?>[] {Integer.class, Integer[].class, Integer[].class}, Demo.class, true, true);
        PyObject[] ints = new PyObject[] {Py.newInteger(3), Py.newInteger(4)};
        assertTrue(args.matches(null, new PyObject[] {Py.newInteger(1), new PyList(ints)}, new String[0], new ReflectedCallData()));
    }

    public void testVarargsNotBoxedWithCorrectArgs() {
        // calling foo(Object... args) as foo([1,2,3])
        ReflectedArgs args = new ReflectedArgs(null, new Class<?>[] {Object[].class}, Demo.class, true, true);
        PyObject[] varargs = new PyObject[] {Py.newString("foo"), Py.newString("bar") };
        PyObject[] argList = new PyObject[] {new PyList(varargs)};
        assertTrue(args.matches(null, argList, new String[0], new ReflectedCallData()));
    }

    public void testVarargsBoxedWithNoSequences() {
        // calling foo(Object... args) as foo("foo", "bar")
        ReflectedArgs args = new ReflectedArgs(null, new Class<?>[] {Object[].class}, Demo.class, true, true);
        PyObject[] varargs = new PyObject[] {Py.newString("foo"), Py.newString("bar") };
        assertTrue(args.matches(null, varargs, new String[0], new ReflectedCallData()));
    }
}
