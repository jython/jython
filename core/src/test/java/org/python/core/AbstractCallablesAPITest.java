// Copyright (c)2026 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.python.types.FastCall;
import org.python.types.TypeSpec;
import org.python.types.WithClass;

/**
 * Test the {@link Callables} API class on a variety of types. We are
 * looking for correct behaviour in the cases attempted but mostly
 * testing the invocation of special method {@code __call__}.
 * <p>
 * Built-in descriptor types have extensive tests elsewhere in which we
 * call them in a variety of ways. Those objects implement the
 * {@link FastCall} interface. Here we test that callable objects not
 * implementing that interface are correctly called.
 */
@DisplayName("When called through the Abstract API ...")
class AbstractCallablesAPITest extends UnitTestSupport {

    /**
     * This abstract base forms a check-list of methods we mean to test.
     */
    abstract static class CallTests {

        final Object obj;
        static final Object[] a = {0, 1, 2, 3, 4, 5, 6, 7};

        /**
         * Construct test from callable object.
         *
         * @param obj the callable object
         */
        CallTests(Object obj) { this.obj = obj; }

        /**
         * A holder for the unpacked result of the call, which is always a pair
         * of a {@code tuple} and a {@code dict}.
         */
        record ArgsKwargs(Object[] pos, Map<Object, Object> kwargs) {}

        /**
         * Unpack a result to an {@link ArgsKwargs}.
         *
         * @param result a pair of a {@code tuple} and a {@code dict}
         * @return holding result as an array and a {@link Map}.
         */
        static ArgsKwargs unpack(Object result) {
            PyTuple r = (PyTuple)result, pos = (PyTuple)r.get(0);
            PyDict kwargs = (PyDict)r.get(1);
            return new ArgsKwargs(pos.toArray(), kwargs);
        }

        /**
         * Check the result of a call against expectations and the contents of
         * {@link #a}.
         *
         * @param result to check
         * @param np the number of positional parameters
         * @param names the names of keyword parameters (in order)
         */
        static void check(Object result, int np, String... names) {
            ArgsKwargs r = unpack(result);
            // The positional arguments must match a[:np]
            Object[] p = r.pos();
            assertEquals(p.length, np);
            for (int i = 0; i < np; i++) { assertEquals(a[i], p[i]); }
            // The arguments given by keyword must match a[np:np+nk]
            int nk = names == null ? 0 : names.length, k = np;
            Map<Object, Object> kwargs = r.kwargs();
            assertEquals(kwargs.size(), nk);
            for (String key : names) { assertPythonEquals(kwargs.get(key), a[k++]); }
        }

        /**
         * Call with no arguments.
         *
         * @throws Throwable unexpectedly.
         */
        @Test
        @DisplayName("obj.call()")
        void check_0p() throws Throwable {
            Object result = Callables.call(obj);
            check(result, 0);
        }

        /**
         * Call with 1 positional argument.
         *
         * @throws Throwable unexpectedly.
         */
        @Test
        @DisplayName("obj.call(a0)")
        void check_1p() throws Throwable {
            Object result = Callables.call(obj, a[0]);
            check(result, 1);
        }

        /**
         * Call with 2 positional arguments.
         *
         * @throws Throwable unexpectedly.
         */
        @Test
        @DisplayName("obj.call(a0, a1)")
        void check_2p() throws Throwable {
            Object result = Callables.call(obj, a[0], a[1]);
            check(result, 2);
        }

        /**
         * Call with 3 positional arguments.
         *
         * @throws Throwable unexpectedly.
         */
        @Test
        @DisplayName("obj.call(a0, a1, a2)")
        void check_3p() throws Throwable {
            Object result = Callables.call(obj, a[0], a[1], a[2]);
            check(result, 3);
        }

        /**
         * Call with 4 positional arguments.
         *
         * @throws Throwable unexpectedly.
         */
        @Test
        @DisplayName("obj.call(a0, a1, a2, a3)")
        void check_4p() throws Throwable {
            Object result = Callables.call(obj, a[0], a[1], a[2], a[3]);
            check(result, 4);
        }

        /**
         * Call with 5 positional arguments.
         *
         * @throws Throwable unexpectedly.
         */
        @Test
        @DisplayName("obj.call(a0, a1, a2, a3, a4)")
        void check_5p() throws Throwable {
            Object result = Callables.call(obj, a[0], a[1], a[2], a[3], a[4]);
            check(result, 5);
        }

        /**
         * Call with only keyword arguments.
         *
         * @throws Throwable unexpectedly.
         */
        @Test
        @DisplayName("obj.call(x=a0, y=a1)")
        void check_0p2k() throws Throwable {
            Object[] args = {a[0], a[1]};
            String[] names = {"x", "y"};
            Object result = Callables.call(obj, args, names);
            check(result, 0, names);
        }

        /**
         * Call with 1 positional argument and 3 keyword arguments.
         *
         * @throws Throwable unexpectedly.
         */
        @Test
        @DisplayName("obj.call(a0, x=a1, y=a2, z=a3)")
        void check_1p3k() throws Throwable {
            Object[] args = Arrays.copyOf(a, 4);
            String[] names = {"x", "y", "z"};
            Object result = Callables.call(obj, args, names);
            check(result, 1, names);
        }

        /**
         * Call with 3 positional argument and 3 keyword arguments.
         *
         * @throws Throwable unexpectedly.
         */
        @Test
        @DisplayName("obj.call(a0, a1, a2, x=a3, y=a4, z=a5)")
        void check_3p3k() throws Throwable {
            Object[] args = Arrays.copyOf(a, 6);
            String[] names = {"x", "y", "z"};
            Object result = Callables.call(obj, args, names);
            check(result, 3, names);
        }
    }

    /**
     * Call a Python type implementing {@code __call__} in Java via the
     * {@link Callables} API.
     */
    @Nested
    @DisplayName("an object defining '__call__' implements ...")
    class SlowCallableTest extends CallTests {
        SlowCallableTest() { super(new SlowCallable()); }
    }

    /**
     * Call a Python type implementing {@link FastCall} via the
     * {@link Callables} API. Technically it implements {@code FastCall},
     * but really it just delegates to {@link SlowCallable}.
     */
    @Nested
    @DisplayName("an object with the FastCall interface implements ...")
    class FastCallSlowTest extends CallTests {
        FastCallSlowTest() { super(new FastCall.Slow(new SlowCallable())); }
    }

    /**
     * A Python type implemented in Java that defines __call__, but doesn't
     * implement FastCall.
     */
    static class SlowCallable implements WithClass {
        static PyType TYPE = PyType.fromSpec(new TypeSpec("SlowCallable", MethodHandles.lookup()));

        @Override
        public PyType getType() { return TYPE; }

        /**
         * {@code __class__} simply plays back its arguments, positional
         * ({@code pos}) and keyword ({@code kwargs}).
         *
         * @param args all arguments
         * @param names keywords matching trailing args
         * @return Python {@code (pos:tuple, kwargs:dict))}
         */
        @SuppressWarnings("static-method")
        Object __call__(Object[] args, String[] names) {
            int k = names == null ? 0 : names.length;
            int n = args.length - k;
            // Make a tuple of the positional arguments
            PyTuple pos = new PyTuple(args, 0, n);

            // Load a dictionary from the keyword arguments
            PyDict kwargs = Py.dict();
            for (int i = 0, j = n; i < k; i++, j++) { kwargs.put(names[i], args[j]); }
            // Return the two as a pair
            return Py.tuple(pos, kwargs);
        }
    }
}
