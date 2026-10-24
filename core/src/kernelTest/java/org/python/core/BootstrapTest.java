// Copyright (c)2026 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.python.kernel.AdoptiveType;
import org.python.kernel.TypeRegistry;

/**
 * Test that the type system is initialised in a single bootstrap
 * thread. The design intent is that the first thread to ask anything of
 * the type system initialises it fully (as the <i>bootstrap
 * thread</i>), before its request is answered, while all other threads
 * with a request have to wait on {@link TypeSystem}. We are are
 * concerned about these possible failure modes:
 * <ol>
 * <li>Deadlock: a thread waiting for the bootstrap thread holds a lock
 * on objects that the bootstrap thread needs.</li>
 * <li>Incomplete publication: type objects become visible outside the
 * bootstrap thread in an incomplete state.</li>
 * <li>Incomplete bootstrap: the bootstrap thread completes type system
 * creation (and its initiating request) while type objects are still
 * not complete.</li>
 * </ol>
 * The last two look much the same to the test, since it is not aware
 * which thread actually performed the bootstrap.
 * <p>
 * We test this by starting a lot of threads, as close to simultaneously
 * as we can manage, that access {@code TypeSystem} static members in a
 * variety of orders. Each thread records when it started and when it
 * got its first answer. {@code TypeSystem} itself keeps track of when
 * the bootstrap started and finished. The bootstrap should complete
 * before any thread gets its first answer, and (for a satisfactory
 * test) multiple threads should be racing before the bootstrap begins.
 * They should all get the same answers.
 */
@DisplayName("When multiple threads use the type system")
@TestMethodOrder(MethodOrderer.MethodName.class)
class BootstrapTest {

    /** Logger for the test. */
    static final Logger logger = LoggerFactory.getLogger(BootstrapTest.class);

    /** Random (or deterministic) order. */
    static final long seed = System.currentTimeMillis();
    // static final long seed = 1234L; // Somewhat repeatable

    /** If defined, dump the times recorded by threads. */
    static final String DUMP_PROPERTY = "org.python.runtime.BootstrapTest.times";

    /** Threads in total. &gt;&gt; {@code setUpClass()} cases. */
    static final int NTHREADS = 100;

    /**
     * Check this many threads actually concurrent. Ideally
     * &gt;{@code setUpClass().NCASES} but expectation depends on CPUs.
     */
    static int MIN_THREADS = Math.min(Runtime.getRuntime().availableProcessors(), NTHREADS) / 2;

    /** Threads to run. */
    static final List<InitThread> threads = new ArrayList<>();
    /** A barrier they all wait behind. */
    static CyclicBarrier barrier;
    /** Source of random behaviour. */
    static Random random = new Random(seed);
    /** Time the first thread completed its first action. */
    static long refNanoTime;

    /**
     * We create multiple threads for each of several ways the type system
     * might be initiated and run them concurrently. They will make
     * observations of the type system that ought only to be possible
     * definitely after the type system initialises. Each will note the
     * high-resolution time at which it began and was able to
     */
    @BeforeAll
    static void setUpClass() {
        // Create NTHREADS randomly choosing which action comes first.
        for (int i = 0; i < NTHREADS; i++) {
            threads.add(switch (random.nextInt(4)) {

                case 0 -> new InitThread() {
                    @Override
                    void action() { reg = TypeSystem.registry; }
                };

                case 1 -> new InitThread() {
                    @Override
                    void action() { floatType = PyFloat.TYPE; }
                };

                case 2 -> new InitThread() {
                    @Override
                    void action() { objectType = PyObject.TYPE; }
                };

                default -> new InitThread() {
                    @Override
                    void action() throws Throwable { result = PyNumber.multiply(6, 7); }
                };
            });
        }

        // Create a barrier of matching capacity.
        barrier = new CyclicBarrier(threads.size());

        // Start the threads in a shuffled order.
        Collections.shuffle(threads, random);
        logger.info("{} threads prepared.", threads.size());
        for (Thread t : threads) { t.start(); }

        // Wait for the threads to finish.
        for (Thread t : threads) {
            try {
                t.join(1000);
            } catch (InterruptedException e) {
                // Check completion later
            }
        }

        // Make sure they all stop (so the test does).
        boolean allStopped = true;
        for (Thread t : threads) { allStopped &= hasStopped(t); }
        assertTrue(allStopped, "Threads were still running");

        // Now sort them by the time the first action completed
        Comparator<InitThread> byFirst = new Comparator<>() {

            @Override
            public int compare(InitThread t1, InitThread t2) {
                return Long.compare(t1.firstNanoTime, t2.firstNanoTime);
            }
        };
        Collections.sort(threads, byFirst);

        // Make the winning thread the reference time for the test
        refNanoTime = threads.get(0).firstNanoTime;
        for (InitThread it : threads) { it.subtractTime(refNanoTime); }

        // Dump the thread times by start time.
        // TODO Make dump conditional again (once test reliable on CI)
        // if (truthy(DUMP_PROPERTY)) { dumpThreads(); }
        dumpThreads();
    }

    /** Property is defined and nothing like "false". */
    private static boolean truthy(String property) {
        property = System.getProperty(property, "false").toLowerCase();
        return !"false".equals(property);
    }

    /**
     * Dump (relative) bootstrap time and thread times to standard output.
     */
    private static void dumpThreads() {
        double r = (TypeSystem.readyNanoTime - refNanoTime) * 1e-6;
        String fmt = "%42s   =%10.4f  (relative ms)\n";
        System.out.printf(fmt, "Type system ready at", r);
        for (InitThread t : threads) { System.out.println(t); }
    }

    private static boolean hasStopped(Thread t) {
        if (t.isAlive()) {
            logger.warn("Still running {}", t.getName());
            // t.stop();
            return false;
        }
        return true;
    }

    /** All threads completed. */
    @SuppressWarnings("static-method")
    @Test
    @DisplayName("All threads complete")
    void allComplete() {
        long completed = threads.stream().filter(t -> t.finishNanoTime > 0L).count();
        assertTrue(completed == threads.size(),
                () -> String.format("%d threads did not complete.", threads.size() - completed));
    }

    /** Some threads started before the bootstrap started. */
    @SuppressWarnings("static-method")
    @Test
    @DisplayName("A race takes place")
    void aRaceTookPlace() {
        // Enough relative start times should be negative
        long competitors = threads.stream().filter(t -> t.startNanoTime <= 0L).count();
        logger.info("{} threads were racing. (Min {} on this platform.)", competitors, MIN_THREADS);
        assertFalse(competitors < MIN_THREADS,
                () -> String.format("Detect < %d competitors.", MIN_THREADS));
    }

    /** Bootstrap completed before the first action completed. */
    @SuppressWarnings("static-method")
    @Test
    @DisplayName("The bootstrap completes before any action.")
    void bootstrapBeforeAction() {
        // Times on threads are relative to refNanoTime
        final long ready = TypeSystem.readyNanoTime - refNanoTime;
        // All first actions should be after type system ready.
        long hasty = threads.stream().filter(t -> t.firstNanoTime < ready).count();
        logger.info("{} threads failed to wait for the type system.", hasty);
        assertEquals(0L, hasty, () -> String.format("%d threads failed to wait.", hasty));
    }

    /** All the threads see the same type registry. */
    @SuppressWarnings("static-method")
    @Test
    @DisplayName("All threads see the same type registry")
    void sameTypeRegistry() {
        TypeRegistry registry = TypeSystem.registry;
        for (InitThread init : threads) { assertSame(registry, init.reg); }
    }

    /** All the threads see a correct PyFloat.TYPE. */
    @SuppressWarnings("static-method")
    @Test
    @DisplayName("All threads see 'float'")
    void sameFloat() {
        PyType f = PyFloat.TYPE;
        assertInstanceOf(AdoptiveType.class, f);
        for (InitThread init : threads) { assertSame(f, init.floatType); }
    }

    /**
     * A thread that performs actions on the type system, so that we may
     * test the outcome is the same for all threads. We keep track of times
     * in nanoseconds so we can be sure of the order of events. All times
     * are relative to the bootstrap time so that they are reasonably
     * printable.
     */
    static abstract class InitThread extends Thread {
        /** Time this thread started. */
        long startNanoTime;
        /** Time this thread completed line one of {@code action()}. */
        long firstNanoTime;
        /** Time this thread completed {@code otherActions()}. */
        long finishNanoTime;
        /** The reference {@link TypeSystem#registry} when inspected. */
        TypeRegistry reg;
        /** The type {@code object} when inspected. */
        PyType objectType;
        /** The type {@code float} when inspected. */
        PyType floatType;
        /** The result of an operation. */
        Object result;

        /**
         * Each implementation of {@code InitThread} retrieves the same data,
         * but chooses to do one action first by overriding this method.
         * {@link #otherActions()} then completes the work.
         */
        abstract void action() throws Throwable;

        @Override
        public void run() {
            // Wait at the barrier until every thread arrives.
            try {
                barrier.await();
                // sleep(random.nextInt(3) * 1000);
            } catch (InterruptedException | BrokenBarrierException e) {
                // This shouldn't happen.
                fail("A thread was interrupted at the barrier.");
                return;
            }
            // Perform the action: *raw* nanos before and after.
            startNanoTime = System.nanoTime();
            try {
                action();
            } catch (Throwable e) {
                logger.atWarn().setMessage("action() threw {}")
                        .addArgument(e.getClass().getSimpleName()).log();
            }
            firstNanoTime = System.nanoTime();
            otherActions();
            finishNanoTime = System.nanoTime();
        }

        /** The required actions apart from the one already done. */
        void otherActions() {
            if (objectType == null) { objectType = PyObject.TYPE; }
            if (floatType == null) { floatType = PyFloat.TYPE; }
            if (reg == null) { reg = TypeSystem.registry; }
        }

        /**
         * Adjust times backwards by the given amount.
         *
         * @param ref to subtract from each recorded nanosecond time
         */
        void subtractTime(long ref) {
            startNanoTime -= ref;
            firstNanoTime -= ref;
            finishNanoTime -= ref;
        }

        @Override
        public String toString() {
            String fmt = "    start=%10.4f, first=%10.4f, finish=%10.4f (%5dns)";
            return String.format(fmt, startNanoTime * 1e-6, firstNanoTime * 1e-6,
                    finishNanoTime * 1e-6, finishNanoTime - firstNanoTime);
        }
    }
}
