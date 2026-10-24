// Copyright (c)2026 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.core;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.lang.invoke.MethodHandles;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.python.support.InterpreterError;
import org.python.types.Feature;
import org.python.types.TypeSpec;
import org.python.types.Exposed.PythonMethod;

/**
 * This test modifies the attributes of some (mutable) type objects and
 * reads those attributes, or performs operations dependent on them,
 * concurrently from distinct threads. It is a collection of nested
 * tests. Each will look for (and fail on) outcomes we deem impossible.
 * <p>
 * Unusually for JUnit tests, the intense processing takes place during
 * the static initialisation method (annotated &#64;{@link BeforeAll})
 * of each test, during which we collect a list of violations. The
 * individual JUnit tests are predicates on that list. If the list is
 * empty, that's a pass.
 * <p>
 * Java guarantees "sequentially consistent" execution of correctly
 * synchronised programs, but the tests here are (deliberately)
 * <b>not</b> correctly synchronised. The hypothesis we attempt to
 * disprove by testing is that unsynchronised use of Python objects we
 * provide leads to behaviour not expected of Python.
 * <p>
 * It is challenging to define the allowed or forbidden outcomes. Python
 * has only recently started to allow true concurrency without the GIL
 * and the required behaviours are not yet clearly stated (2025).
 * <p>
 * This test is influenced by the literature on the "litmus tests", used
 * to test or complement assertions made for a processor architecture
 * and to validate language memory models. Listing 16.1 in <i>Java
 * Concurrency in Practice</i>, Peierls, Goetz, et al. is an example of
 * the latter.
 */
@DisplayName("During concurrent use of type objects ...")
@TestMethodOrder(MethodOrderer.MethodName.class)
class TypeConcurrencyTest {

    /** Logger for the tests. */
    static final Logger logger = LoggerFactory.getLogger(TypeConcurrencyTest.class);

    /** Threads in each batch. */
    static final int BATCH_SIZE = 25; // ~ 10s

    /** Number of batches. */
    static final int BATCH_COUNT = 1_000; // ~ 1000s

    /** Curtail a test once we have found this many failures. */
    static final int FAILURE_LIMIT = BATCH_SIZE + 5;

    /**
     * A test fixture that runs two threads defined by the methods
     * {@link #t1()} and {@link #t2()} in a race. The test object itself
     * implements {@link Runnable} so that we can run many copies in
     * parallel (for efficiency and to stress the processor), but it is the
     * pair of threads in each case that constitute the effective "litmus
     * test program".
     * <p>
     * In order to use this class, a subclass defines {@link #t1()} and
     * {@link #t2()} to define the activity, and {@link #allowed()} or
     * {@link #forbidden()} (not usually both) to and determine whether the
     * outcome is allowed (a pass) for not (a fail).
     */
    abstract static class LitmusTest implements Runnable {

        private CyclicBarrier barrier = new CyclicBarrier(2);

        /** Any exception thrown in the corresponding method. */
        Throwable exc1, exc2;

        /** Define the actions of the first thread. */
        abstract void t1() throws Throwable;

        /** Define the actions of the second thread. */
        abstract void t2() throws Throwable;

        /**
         * Test whether the result of the pair of threads is allowed behaviour
         * (a pass) or not (a fail). Returns {@code true} by default.
         *
         * @return {@code false} if a fail
         */
        @SuppressWarnings("static-method")
        boolean allowed() { return true; }

        /**
         * Test whether the result of the pair of threads is forbidden behaviour
         * (a fail) or not (a pass). Returns {@code false} by default.
         *
         * @return {@code true} if a fail
         */
        @SuppressWarnings("static-method")
        boolean forbidden() { return false; }

        /**
         * Create and run two threads, one to call {@link #t1()} and the other
         * {@link #t2()}. Both threads wait at a barrier of capacity two,
         * meaning the first waits until the second arrives there, then both
         * proceed to call their respective methods in a race. Afterwards,
         * {@link #allowed()} (must be {@code true}) {@link #forbidden()} (must
         * be {@code false}) determine whether the state reached is permitted.
         */
        @Override
        public void run() {

            // This thread calls t1()
            Thread call_t1 = new Thread(() -> {
                try {
                    barrier.await();
                    t1();
                } catch (Throwable t) {
                    exc1 = t;
                }
            });

            // At the same time, this thread calls t2()
            Thread call_t2 = new Thread(() -> {
                try {
                    barrier.await();
                    t2();
                } catch (Throwable t) {
                    exc2 = t;
                }
            });

            Thread[] threads = {call_t1, call_t2};

            // Start both threads. (They sync at the barrier.)
            for (Thread t : threads) { t.start(); }
            // Wait for the threads to finish.
            for (Thread t : threads) {
                try {
                    t.join(1000);
                } catch (InterruptedException e) {
                    // Check completion later
                    exc1 = exc2 = e;
                }
            }
        }

        /**
         * Return {@code true} if there was an exception during {@link #t1()} or
         * {@link #t2()}, if {@link #allowed()} is {@code false} or if
         * {@link #forbidden()} is {@code true}.
         *
         * @return {@code true} on failure
         */
        boolean failed() { return exc1 != null || exc2 != null || !allowed() || forbidden(); }

        /**
         * Return the test to initial conditions to save creating a fresh one.
         * Only re-define this if that can be validly done, in which case it
         * should return {@code true}. The default return value is
         * {@code false}, which indicates to the caller it should create a new
         * test object.
         */
        @SuppressWarnings("static-method")
        boolean reset() { return false; }
    }

    /**
     * A base class for tests that run multiple litmus tests in parallel and
     * examine the outcomes.
     */
    abstract static class Examination {

        /** Failing litmus tests */
        List<? extends LitmusTest> failures = new LinkedList<>();

        /**
         * This method must be called from the static set-up of each (annotated
         * &#64;{@link BeforeAll}) individual test class, and the returned list
         * be in the (instance) field {@code failures} by a
         * &#64;{@link BeforeEach} method. It is given a factory method
         * ({@code new}) for a litmus test object of the specific kind needed by
         * the test.
         * <p>
         * This is the method that performs most of the work. It returns a list
         * of failing cases (as determined by {@link LitmusTest#failed()}).
         *
         * @param <T> Specific type of litmus test
         * @param newInstance new method of that type (or factory)
         * @return instances where LitmusTest#failed
         * @throws Throwable on uncaught errors (not failed tests)
         */
        static <T extends LitmusTest> List<T> race(Supplier<T> newInstance) throws Throwable {

            // Failing litmus tests
            List<T> failures = new LinkedList<>();

            // Batch of tests to run concurrently.
            ArrayList<T> tests = new ArrayList<>(BATCH_SIZE);
            Thread[] threads = new Thread[BATCH_SIZE];

            for (int i = 0; i < BATCH_SIZE; i++) { tests.add(i, newInstance.get()); }
            String name = tests.get(0).getClass().getSimpleName();

            logger.atInfo().setMessage("Race begun ({})").addArgument(name).log();

            // We use that storage repeatedly with new tests.
            for (int batch = 0; batch < BATCH_COUNT; batch++) {
                // Create threads: each will run a pair of racing
                // threads.
                for (int i = 0; i < BATCH_SIZE; i++) { threads[i] = new Thread(tests.get(i)); }
                // Start them all close together in time
                for (Thread t : threads) { t.start(); }

                // Wait for the threads to finish.
                for (int i = 0; i < BATCH_SIZE; i++) {
                    try {
                        threads[i].join(1000);
                    } catch (InterruptedException e) {
                        // Check completion later
                    }
                    // Collect failures in a list.
                    T test = tests.get(i);
                    if (test.failed()) {
                        // Collect failed example and replace in tests.
                        failures.add(test);
                        tests.add(i, newInstance.get());
                    } else if (!test.reset()) {
                        // We could not reset the test object.
                        tests.add(i, newInstance.get());
                    }
                }

                // Make sure they all stop (so the test does).
                boolean allStopped = true;
                for (Thread t : threads) { allStopped &= hasStopped(t); }
                assertTrue(allStopped, "Threads were still running");

                // Early exit if ample evidence.
                if (failures.size() >= FAILURE_LIMIT) { break; }
            }
            logger.atInfo().setMessage("Race ended").log();

            return failures;
        }

        /**
         * Dry-run components of the test in sequence without threading for
         * debugging purposes. This is like {@link #race(Supplier)}, but without
         * threads and only one instance.
         */
        static <T extends LitmusTest> void dryRun(Supplier<T> newInstance) {
            // Ensure the type system exists
            assertTrue(TypeSystem.bootstrapNanoTime > 0L, "Check type system booted");
            T test = newInstance.get();

            // Run both halves to completion in sequence
            try {
                test.t1();
                test.t2();
            } catch (Throwable t) {
                t.printStackTrace();
            }

            logger.atDebug().setMessage("Dry run (%s) allowed()=%s forbidden()=%s")
                    .addArgument(test.getClass().getSimpleName()).addArgument(test.allowed())
                    .addArgument(test.forbidden()).log();

            // Reset and run again the other way around
            if (!test.reset()) { test = newInstance.get(); }
            try {
                test.t2();
                test.t1();
            } catch (Throwable t) {
                t.printStackTrace();
            }
            // This may fail if reset() is buggy.
            assertTrue(test.allowed() && !test.forbidden());
        }

        /**
         * Publish failing litmus tests to this test instance.
         *
         * @param failures resulting from the racing threads
         */
        void setFailures(List<? extends LitmusTest> failures) { this.failures = failures; }

        private static boolean hasStopped(Thread t) {
            if (t.isAlive()) { logger.warn("Still running {}", t.getName()); return false; }
            return true;
        }

        /** Check for tests that ended with an exceptions. */
        @Test
        @DisplayName("No exceptions were thrown")
        void checkException() {
            for (LitmusTest t : failures) { assertNull(t.exc1); assertNull(t.exc2); }
        }

        /** Check the outcome was allowed. */
        @Test
        @DisplayName("Only allowed states were reached")
        void checkAllowed() throws PyAttributeError, Throwable {
            for (LitmusTest t : failures) { if (!t.allowed()) { fail(t.toString()); } }
        }
    }

    /**
     * Extend {@link LitmusTest} for a single type object name space as the
     * "store". The subclass should add attributes "X" and "Y" in a race and
     * expect a sequentially consistent result.
     */
    static abstract class DirectXY extends LitmusTest {
        /** Type object to use as a namespace. */
        PyType type;
        /** Variables set by the racing threads (registers). */
        Object r1, r2;

        DirectXY() {
            String name = this.getClass().getSimpleName();
            try {
                this.type = (PyType)PyType.TYPE().call(name, Py.tuple(PyObject.TYPE), Py.dict());
            } catch (Throwable e) {
                fail("Constructing a type");
            }
        }

        @Override
        boolean reset() {
            r1 = r2 = null;
            exc1 = exc2 = null;
            try {
                if (Abstract.lookupAttr(type, "X") != null) { Abstract.delAttr(type, "X"); }
                if (Abstract.lookupAttr(type, "Y") != null) { Abstract.delAttr(type, "Y"); }
            } catch (Throwable e) {
                fail("Unable to reset() type attributes");
            }
            return true;
        }

        @Override
        public String toString() {
            Object x = null, y = null;
            try {
                x = Abstract.lookupAttr(type, "X");
                y = Abstract.lookupAttr(type, "Y");
            } catch (Throwable t) {}
            return String.format("[t=%s, r1=%s, r2=%s, t.X=%s, t.Y=%s]", type, r1, r2, x, y);
        }
    }

    /**
     * The Load Buffering litmus test interpreted for direct attribute
     * access on a single type object as the "store".
     */
    @Nested
    @DisplayName("Load Buffering (direct LB)")
    class ExamDirectXY_LB extends Examination {

        /** Failing litmus tests */
        static List<? extends LitmusTest> failures;

        /** Run the set-up and collect failing cases. */
        @BeforeAll
        static void setUpClass() throws Throwable {
            dryRun(DirectXY_LB::new);
            failures = Examination.race(DirectXY_LB::new);
        }

        @BeforeEach
        void getFailures() { setFailures(failures); }

        /**
         * Define LB for type object name space as the "store".
         */
        static class DirectXY_LB extends DirectXY {

            @Override
            void t1() throws Throwable {
                r1 = Abstract.lookupAttr(type, "X");
                Abstract.setAttr(type, "Y", 2);
            }

            @Override
            void t2() throws Throwable {
                r2 = Abstract.lookupAttr(type, "Y");
                Abstract.setAttr(type, "X", 1);
            }

            @Override
            boolean allowed() {
                // Sequentially consistent answer.
                return r1 == null && r2 == null  //
                        || r1 == null && r2.equals(2)  //
                        || r2 == null && r1.equals(1);
            }
        }
    }

    /**
     * The Storage Buffering litmus test interpreted for direct attribute
     * access on a single type object as the "store".
     */
    @Nested
    @DisplayName("Store Buffering (direct SB)")
    class ExamDirectXY_SB extends Examination {

        /** Failing litmus tests */
        static List<? extends LitmusTest> failures;

        /** Run the set-up and collect failing cases. */
        @BeforeAll
        static void setUpClass() throws Throwable {
            dryRun(DirectXY_SB::new);
            failures = Examination.race(DirectXY_SB::new);
        }

        @BeforeEach
        void getFailures() { setFailures(failures); }

        /**
         * Define SB for a type object name space as the "store".
         */
        static class DirectXY_SB extends DirectXY {
            @Override
            void t1() throws Throwable {
                Abstract.setAttr(type, "X", 2);
                r1 = Abstract.lookupAttr(type, "Y");
            }

            @Override
            void t2() throws Throwable {
                Abstract.setAttr(type, "Y", 1);
                r2 = Abstract.lookupAttr(type, "X");
            }

            @Override
            boolean forbidden() {
                // r1, r2 cannot both be unset.
                return r1 == null && r2 == null;
            }
        }
    }

    /**
     * The Message Passing litmus test interpreted for direct attribute
     * access on a single type object as the "store".
     */
    @Nested
    @DisplayName("Message Passing (direct MP)")
    class ExamDirectXY_MP extends Examination {

        /** Failing litmus tests */
        static List<? extends LitmusTest> failures;

        /** Run the set-up and collect failing cases. */
        @BeforeAll
        static void setUpClass() throws Throwable {
            dryRun(DirectXY_MP::new);
            failures = Examination.race(DirectXY_MP::new);
        }

        @BeforeEach
        void getFailures() { setFailures(failures); }

        /**
         * Define MP for a type object name space as the "store".
         */
        static class DirectXY_MP extends DirectXY {
            @Override
            void t1() throws Throwable {
                Abstract.setAttr(type, "X", 1);
                Abstract.setAttr(type, "Y", 2);
            }

            @Override
            void t2() throws Throwable {
                r1 = Abstract.lookupAttr(type, "Y");
                r2 = Abstract.lookupAttr(type, "X");
            }

            @Override
            boolean forbidden() {
                // If r1=t.Y is set, r2=t.X cannot be unset.
                return r1 != null && r1.equals(2) && r2 == null;
            }
        }
    }

    /**
     * A Python type (defined in Java) from which we may lift the methods
     * for the purpose of assigning them to a sub-type. This has to be
     * public in Java because a representation of its Python subclass
     * (created in a package belonging to the run-time) must extend this
     * class.
     */
    public static class DummyMethods extends PyLong {
        static PyType TYPE = PyType.fromSpec(new TypeSpec("DummyMethods", MethodHandles.lookup())
                .base(PyLong.TYPE).add(Feature.BASETYPE, Feature.IMMUTABLE));

        /** Construct from integer. */
        public DummyMethods(BigInteger i) { super(i); }

        @PythonMethod
        static Object neg(DummyMethods self) { return 42; }

        @PythonMethod
        static String str(DummyMethods self) { return "forty-two"; }
    }

    /**
     * Extend {@link LitmusTest} for a single type object name space as the
     * "store", but defining and calling special methods. The type is a
     * sub-class of {@code int}. The subclass should add and call attributes
     * {@code __neg__} and {@code __str__} in a race and expect a
     * sequentially consistent result.
     */
    static abstract class DirectSM extends LitmusTest {
        /** Type object to use as a namespace. */
        final PyType type;
        /** Object on which to call special method. */
        final Object instance;
        /** Variables set by the racing threads (registers). */
        Object r1, r2;
        /** Values to assign as special methods */
        static MethodDescriptor NEG, STR;
        static {
            try {
                NEG = (MethodDescriptor)Abstract.lookupAttr(DummyMethods.TYPE, "neg");
                STR = (MethodDescriptor)Abstract.lookupAttr(DummyMethods.TYPE, "str");
            } catch (Throwable t) {
                fail("Accessing DummyMethods for test");
            }
        }

        DirectSM() {
            String name = this.getClass().getSimpleName();
            PyType t = null;
            Object i = null;
            try {
                t = (PyType)PyType.TYPE().call(name, Py.tuple(DummyMethods.TYPE), Py.dict());
                i = t.call(1); // int(1)
            } catch (Throwable e) {
                e.printStackTrace();
                fail("Constructing a type and an instance");
            }
            this.type = t;
            this.instance = i;
        }

        @Override
        boolean reset() {
            try {
                MethodDescriptor neg = (MethodDescriptor)Abstract.lookupAttr(type, "__neg__");
                MethodDescriptor str = (MethodDescriptor)Abstract.lookupAttr(type, "__str__");
                if (neg == NEG) { Abstract.delAttr(type, "__neg__"); }
                if (str == STR) { Abstract.delAttr(type, "__str__"); }
            } catch (Throwable e) {
                fail("Unable to reset() type special methods");
            }
            r1 = r2 = null;
            exc1 = exc2 = null;
            return true;
        }

        @Override
        public String toString() {
            Object neg = null, str = null;
            try {
                neg = Abstract.lookupAttr(type, "__neg__");
                str = Abstract.lookupAttr(type, "__str__");
            } catch (Throwable t) {}
            return String.format("[t=%s, r1=%s, r2=%s, t.__neg__=%s, t.__str__=%s]", type, r1, r2,
                    neg, str);
        }
    }

    /**
     * The Load Buffering litmus test interpreted for special method access
     * on a single type object as the "store".
     */
    @Nested
    @DisplayName("Load Buffering (special method LB)")
    class ExamSpecialMethod_LB extends Examination {

        /** Failing litmus tests */
        static List<? extends LitmusTest> failures;

        /** Run the set-up and collect failing cases. */
        @BeforeAll
        static void setUpClass() throws Throwable {
            dryRun(DirectSM_LB::new);
            failures = Examination.race(DirectSM_LB::new);
        }

        @BeforeEach
        void getFailures() { setFailures(failures); }

        /**
         * Define LB for type object name space as the "store".
         */
        static class DirectSM_LB extends DirectSM {

            @Override
            void t1() throws Throwable {
                r1 = PyNumber.negative(instance);
                Abstract.setAttr(type, "__str__", STR);
            }

            @Override
            void t2() throws Throwable {
                r2 = Abstract.str(instance);
                Abstract.setAttr(type, "__neg__", NEG);
            }

            @Override
            boolean allowed() {
                if (pythonEquals(-1, r1) && pythonEquals("1", r2)) {
                    // Both reads occurred before either redefinition.
                    return true;
                } else if (pythonEquals("forty-two", r2)) {
                    // __str__ was called after it was redefined
                    // => __neg__ was redefined after it was called.
                    return pythonEquals(-1, r1);
                } else if (pythonEquals(42, r1)) {
                    // __neg__ was called after it was redefined
                    // => __str__ was redefined after it was called
                    return pythonEquals("1", r2);
                }
                return false;
            }
        }
    }

    /**
     * The Storage Buffering litmus test interpreted for special method
     * access on a single type object as the "store".
     */
    @Nested
    @DisplayName("Storage Buffering (special method SB)")
    class ExamSpecialMethod_SB extends Examination {

        /** Failing litmus tests */
        static List<? extends LitmusTest> failures;

        /** Run the set-up and collect failing cases. */
        @BeforeAll
        static void setUpClass() throws Throwable {
            dryRun(DirectSM_SB::new);
            failures = Examination.race(DirectSM_SB::new);
        }

        @BeforeEach
        void getFailures() { setFailures(failures); }

        /**
         * Define SB for type object name space as the "store".
         */
        static class DirectSM_SB extends DirectSM {

            @Override
            void t1() throws Throwable {
                Abstract.setAttr(type, "__str__", STR);
                r1 = PyNumber.negative(instance);
            }

            @Override
            void t2() throws Throwable {
                Abstract.setAttr(type, "__neg__", NEG);
                r2 = Abstract.str(instance);
            }

            @Override
            boolean forbidden() {
                // r1, r2 cannot both be from 'int' methods.
                return pythonEquals(-1, r1) && pythonEquals("1", r2);
            }
        }
    }

    /**
     * The Message Passing litmus test interpreted for special method access
     * on a single type object as the "store".
     */
    @Nested
    @DisplayName("Message Passing (special method MP)")
    class ExamSpecialMethod_MP extends Examination {

        /** Failing litmus tests */
        static List<? extends LitmusTest> failures;

        /** Run the set-up and collect failing cases. */
        @BeforeAll
        static void setUpClass() throws Throwable {
            dryRun(DirectSM_MP::new);
            failures = Examination.race(DirectSM_MP::new);
        }

        @BeforeEach
        void getFailures() { setFailures(failures); }

        /**
         * Define LB for type object name space as the "store".
         */
        static class DirectSM_MP extends DirectSM {

            @Override
            void t1() throws Throwable {
                Abstract.setAttr(type, "__str__", STR);
                Abstract.setAttr(type, "__neg__", NEG);
            }

            @Override
            void t2() throws Throwable {
                r1 = PyNumber.negative(instance);
                r2 = Abstract.str(instance);
            }

            @Override
            boolean allowed() {
                // Did r1 see the modified __neg__?
                if (pythonEquals(42, r1)) {
                    // Then r2 must see the modified __str__.
                    return pythonEquals("forty-two", r2);
                }
                // Otherwise ok
                return true;
            }
        }
    }

    /**
     * Test whether the object {@code o} is equal to the expected value
     * according to Python (e.g. {@code True == 1} and strings may be equal
     * even if one is a {@link PyUnicode}. An unchecked exception may be
     * thrown if the comparison goes badly enough.
     *
     * @param x value expected
     * @param o to test
     * @return whether equal in Python
     */
    static boolean pythonEquals(Object x, Object o) {
        try {
            return Abstract.richCompareBool(x, o, Comparison.EQ);
        } catch (RuntimeException | Error e) {
            // Let unchecked exception fly
            throw e;
        } catch (Throwable t) {
            // Wrap checked exception
            throw new InterpreterError(t);
        }
    }

}
