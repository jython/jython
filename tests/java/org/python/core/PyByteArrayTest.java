package org.python.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.python.util.PythonInterpreter;

/**
 * JUnit tests for PyByteArray.
 */
public class PyByteArrayTest extends BaseBytesTest {

    /**
     * Constructor required by JUnit.
     *
     * @param name
     */
    public PyByteArrayTest(String name) {
        super(name);
    }

    /**
     * Generate character codes for in a pattern matching an intended deletion or slice to be
     * replaced. If c="adb", something like b'aaaaaaddddbbbb' where the 'd' characters should be
     * deleted or replaced in the slice operation.
     *
     * @param na number of c.charAt(0) characters
     * @param nd number of c.charAt(1) characters
     * @param nb number of c.charAt(2) characters
     * @param c character codes
     * @return filled array
     */
    public static int[] patternInts(int na, int nd, int nb, String c) {
        int[] r = new int[na + nd + nb];
        int p = 0;
        for (int i = 0; i < na; i++) {
            r[p++] = c.charAt(0);
        }
        for (int i = 0; i < nd; i++) {
            r[p++] = c.charAt(1);
        }
        for (int i = 0; i < nb; i++) {
            r[p++] = c.charAt(2);
        }
        return r;
    }

    /**
     * Generate character codes for 'a', 'D', 'b' in a pattern matching an intended deletion or
     * slice to be replaced. Something like b'aaaaaaddddbbbb' where the 'E' characters should be
     * deleted or replaced in the slice operation.
     *
     * @param na number of a characters
     * @param nd number of D characters
     * @param nb number of b characters
     * @return filled array
     */
    public static int[] adbInts(int na, int nd, int nb) {
        return patternInts(na, nd, nb, "aDb");
    }

    /**
     * Generate character codes for 'a', 'E', 'b' in a pattern matching an intended result of slice
     * replacement. Something like b'aaaaaaEEEbbbb' where the 'E' characters are the replacement in
     * the slice operation.
     *
     * @param na number of a characters
     * @param ne number of E characters
     * @param nb number of b characters
     * @return filled array
     */
    public static int[] aebInts(int na, int ne, int nb) {
        return patternInts(na, ne, nb, "aEb");
    }

    /**
     * Generate a tuple of int arrays at random in the range 0..255 for testing slice operations. In
     * effect, the method generates 4 arrays of random data A, B, D, E and returns an array of three
     * arrays formed thus: { A + D + B, A + E + B, E } where + means concatenation. This can be used
     * to test slice assignment and deletion.
     *
     * @param random the random generator
     * @param na the number of elements in A
     * @param nd the number of elements in D (the deleted material)
     * @param nb the number of elements in B
     * @param ne the number of elements in E (the inserted material, 0 for slice deletion)
     * @return three arrays of length na + nd + nb, na + ne + nb, and ne.
     */
    public static int[][] randomSliceProblem(Random random, int na, int nd, int nb, int ne) {
        int[] adb = new int[na + nd + nb];
        int[] aeb = new int[na + ne + nb];
        int[] e = new int[ne];
        int[][] ret = {adb, aeb, e};
        int p = 0, q = 0;
        // The A values go into adb and aeb
        for (int i = 0; i < na; i++) {
            int a = random.nextInt(256);
            adb[p++] = a;
            aeb[q++] = a;
        }
        // The D values go into adb only
        for (int i = 0; i < nd; i++) {
            int d = random.nextInt(256);
            adb[p++] = d;
        }
        // The E values go into e and aeb
        for (int i = 0; i < ne; i++) {
            int x = random.nextInt(256);
            e[p++] = x;
            aeb[q++] = x;
        }
        // The B values go into adb and aeb
        for (int i = 0; i < nb; i++) {
            int b = random.nextInt(256);
            adb[p++] = b;
            aeb[q++] = b;
        }
        return ret;
    }

    /**
     * Check result of slice operations, synthesised from the elements passed. This method accepts
     * the 'dimensions' of a slice problem and tests whether a resulting byte array contains the
     * correct result. The data elements have come from two existing arrays of (potentially) random
     * data X and Y. Let N=na+nd+nb. The client has generated, in effect, 4 arrays A=X[:na],
     * B=X[-nb:N], D=X[na:nb] and E=Y[:ne], and posed the problem setslice( A + D + B, E ), where +
     * means concatenation in this expression, to which the answer should be A + E + B. This method
     * checks that the result is exactly that.
     *
     * @param na the number of elements in A
     * @param nd the number of elements in D (the deleted material)
     * @param nb the number of elements in B
     * @param ne the number of elements in E (the inserted material, 0 for slice deletion)
     * @param x source of the A, D and B data
     * @param y source of the E data
     * @param result the result to be tested against A+E+B
     */
    public static void
            checkSlice(int na, int nd, int nb, int ne, int[] x, int[] y, BaseBytes result) {
        // Check the size is right
        assertEquals("size", na + ne + nb, result.size());
        // Check that A is preserved
        checkInts(x, 0, result, 0, na);
        // Check that E is inserted
        checkInts(y, 0, result, na, ne);
        // Check that B is preserved
        checkInts(x, na + nd, result, na + ne, nb);
    }

    /**
     * Check result of extended slice operations, synthesised from the elements passed. This method
     * accepts the 'dimensions' of a slice problem and tests whether a resulting byte array contains
     * the correct result. The result array has been filled from (the whole of) array x[], then
     * slice assignment took place from y[k] to element u[start + k*step].
     *
     * @param start
     * @param step
     * @param n number of steps
     * @param x source of the original data
     * @param y source of the assigned data
     * @param u the result to be tested against properly selected elements of x and y
     */
    public static void checkSlice(int start, int step, int n, int[] x, int[] y, BaseBytes u) {
        // Check the size is right
        assertEquals("size", x.length, u.size());

        if (step > 0) {

            // Check before start of slice
            int px = 0, py = 0;
            for (; px < start; px++) {
                assertEquals("before slice", x[px], u.intAt(px));
            }

            // Check slice-affected region at n assignments and n-1 gaps of length step-1.
            if (n > 0) {
                assertEquals("first affected", y[py++], u.intAt(px++));
            }

            for (int i = 1; i < n; i++) {
                for (int j = 1; j < step; j++, px++) {
                    assertEquals("in gap", x[px], u.intAt(px));
                }
                assertEquals("next affected", y[py++], u.intAt(px++));
            }

            // Check after slice-affected region
            for (; px < x.length; px++) {
                assertEquals("after slice", x[px], u.intAt(px));
            }

        } else {
            // Negative step but easier to think about as a positive number
            step = -step;

            // Check after start of slice
            int px = x.length - 1, py = 0;
            for (; px > start; --px) {
                assertEquals("after slice", x[px], u.intAt(px));
            }

            // Check slice-affected region at n assignments and n-1 gaps of length step-1.
            if (n > 0) {
                assertEquals("first affected", y[py++], u.intAt(px--));
            }

            for (int i = 1; i < n; i++) {
                for (int j = 1; j < step; j++, px--) {
                    assertEquals("in gap", x[px], u.intAt(px));
                }
                assertEquals("next affected", y[py++], u.intAt(px--));
            }

            // Check before slice-affected region
            for (; px >= 0; px--) {
                assertEquals("before slice", x[px], u.intAt(px));
            }
        }
    }

    /**
     * Check result of extended slice deletion operations, synthesised from the elements passed.
     * This method accepts the 'dimensions' of a slice deletion problem and tests whether a
     * resulting byte array contains the correct result. The result array has been filled from (the
     * whole of) array x[], then slice deletion took place at original element u[start + k*step].
     *
     * @param start
     * @param step
     * @param n number of steps (deletions)
     * @param x source of the original data
     * @param u the result to be tested against properly selected elements of x
     */
    public static void checkDelSlice(int start, int step, int n, int[] x, BaseBytes u) {
        // Check the size is right
        assertEquals("size", x.length - n, u.size());

        if (step > 0) {

            // Check before start of slice
            int px = 0, pu = 0;
            for (; px < start; px++) {
                assertEquals("before slice", x[px], u.intAt(pu++));
            }

            // Check slice-affected region at n deletions and n-1 gaps of length step-1.
            // px now points to the first element that should be missing from u
            px++;
            for (int i = 1; i < n; i++) {
                for (int j = 1; j < step; j++, px++) {
                    assertEquals("in gap", x[px], u.intAt(pu++));
                }
                // px now points to the i.th element that should be missing from u
                px++;
            }

            // Check after slice-affected region
            for (; px < x.length; px++) {
                assertEquals("after slice", x[px], u.intAt(pu++));
            }

        } else {

            // Negative step but easier to think about as a positive number
            step = -step;

            // Check after start of slice
            int px = x.length - 1, pu = u.size - 1;
            for (; px > start; --px) {
                assertEquals("after slice", x[px], u.intAt(pu--));
            }

            // Check slice-affected region at n assignments and n-1 gaps of length step-1.
            // px now points to the first element that should be missing from u
            px--;
            for (int i = 1; i < n; i++) {
                for (int j = 1; j < step; j++, px--) {
                    assertEquals("in gap", x[px], u.intAt(pu--));
                }
                // px now points to the i.th element that should be missing from u
                px--;
            }

            // Check before slice-affected region
            for (; px >= 0; px--) {
                assertEquals("before slice", x[px], u.intAt(pu--));
            }
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.python.core.BaseBytesTest#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    /**
     * Test method for {@link PyObject#__getslice__(PyObject, PyObject)}.
     *
     * @see BaseBytes#getslice(int, int)
     */
    public void test__getslice__2() {
        int verbose = 0;
        // __getslice__() deals with start, stop values also relative to the end.
        String ver = "L'un a la pourpre de nos âmes";
        final int L = ver.length();
        int[] aRef = toInts(ver);
        BaseBytes a = getInstance(aRef);
        List<PyInteger> bList = new ArrayList<PyInteger>(L);

        // Need interpreter (for Py.None and exceptions)
        interp = new PythonInterpreter();

        final int[] posStart = new int[] {0, 8, 16, L - 5, L - 1};

        for (int start : posStart) {
            PyObject pyStart, pyStop, pyStart_L, pyStop_L;
            pyStart = new PyInteger(start);
            if (start > 0) {
                // The other way of saying [start:stop] is [start-L:stop]
                pyStart_L = new PyInteger(start - L);
            } else {
                // The other way of saying [0:stop] is [:stop]
                pyStart_L = Py.None;
            }

            for (int stop = start; stop <= L; stop++) {

                // Make a reference answer by picking elements of aRef in slice pattern
                bList.clear();
                for (int i = start; i < stop; i++) {
                    if (verbose >= 5) {
                        System.out.printf("    (%d,%d) i=%d\n", start, stop, i);
                    }
                    bList.add(new PyInteger(aRef[i]));
                }

                // PyObject versions of stop
                if (stop < L) {
                    // The other way of saying [start:stop:+s] is [start:stop-L:+s]
                    pyStop_L = new PyInteger(stop - L);
                } else {
                    // The other way of saying [start:L:+s] is [start::+s]
                    pyStop_L = Py.None;
                }
                pyStop = new PyInteger(stop);

                // Generate test result and check it
                doTest__getslice__2(a, pyStart, pyStop, bList, verbose + 2);
                // Repeat same result specifying start relative to end
                doTest__getslice__2(a, pyStart_L, pyStop, bList, verbose);
                // Repeat same result specifying stop relative to end
                doTest__getslice__2(a, pyStart, pyStop_L, bList, verbose);
                // Repeat same result specifying start and stop relative to end
                doTest__getslice__2(a, pyStart_L, pyStop_L, bList, verbose);
            }
        }
    }

    /**
     * Common code to check {@link PyByteArray#__getslice__(PyObject, PyObject)} against a reference
     * answer.
     *
     * @param a object under test
     * @param pyStart
     * @param pyStop
     * @param bList reference answer
     * @param verbose 0..4 control output
     */
    private void doTest__getslice__2(BaseBytes a, PyObject pyStart, PyObject pyStop,
            List<PyInteger> bList, int verbose) {
        if (verbose >= 4) {
            System.out.printf("    __getslice__(%s,%s)\n", pyStart, pyStop);
        }
        PyObject b = a.__getslice__(pyStart, pyStop);
        if (verbose >= 3) {
            System.out.println(toString((BaseBytes)b));
        }
        checkInts(bList, b);
    }

    /**
     * Test method for {@link PyObject#__getslice__(PyObject, PyObject, PyObject)}.
     *
     * @see BaseBytes#getslice(int, int, int)
     */
    public void test__getslice__3() {
        int verbose = 0;
        // __getslice__() deals with start, stop values also relative to the end.
        String ver = "Quand je brûle et que tu t'enflammes ;";
        final int L = ver.length();
        int[] aRef = toInts(ver);
        BaseBytes a = getInstance(aRef);
        List<PyInteger> bList = new ArrayList<PyInteger>(L);

        // Need interpreter (for Py.None and exceptions)
        interp = new PythonInterpreter();

        // Positive step

        final int[] posStart = new int[] {0, 9, 22, L - 11, L - 1};
        // final int[] posStart = new int[] {0, 9, L-1};

        for (int step = 1; step < 4; step++) {
            PyInteger pyStep = new PyInteger(step);
            for (int start : posStart) {
                PyObject pyStart, pyStop, pyStart_L, pyStop_L;
                pyStart = new PyInteger(start);
                if (start > 0) {
                    // The other way of saying [start:stop:+s] is [start-L:stop:+s]
                    pyStart_L = new PyInteger(start - L);
                } else {
                    // The other way of saying [0:stop:+s] is [:stop:+s]
                    pyStart_L = Py.None;
                }

                for (int stop = start; stop <= L; stop++) {

                    // Make a reference answer by picking elements of aRef in slice pattern
                    bList.clear();
                    for (int i = start; i < stop; i += step) {
                        if (verbose >= 5) {
                            System.out.printf("    (%d,%d,%d) i=%d\n", start, stop, step, i);
                        }
                        bList.add(new PyInteger(aRef[i]));
                    }

                    // PyObject versions of stop
                    if (stop < L) {
                        // The other way of saying [start:stop:+s] is [start:stop-L:+s]
                        pyStop_L = new PyInteger(stop - L);
                    } else {
                        // The other way of saying [start:L:+s] is [start::+s]
                        pyStop_L = Py.None;
                    }
                    pyStop = new PyInteger(stop);

                    // Generate test result and check it
                    doTest__getslice__3(a, pyStart, pyStop, pyStep, bList, verbose + 2);
                    // Repeat same result specifying start relative to end
                    doTest__getslice__3(a, pyStart_L, pyStop, pyStep, bList, verbose);
                    // Repeat same result specifying stop relative to end
                    doTest__getslice__3(a, pyStart, pyStop_L, pyStep, bList, verbose);
                    // Repeat same result specifying start and stop relative to end
                    doTest__getslice__3(a, pyStart_L, pyStop_L, pyStep, bList, verbose);
                }
            }
        }

        // Negative step

        final int[] negStart = new int[] {0, 5, 14, L - 10, L - 1};
        // final int[] negStart = new int[] {0, 5, L-1};

        for (int step = -1; step > -4; step--) {
            PyInteger pyStep = new PyInteger(step);

            for (int start : negStart) {
                PyObject pyStart, pyStop, pyStart_L, pyStop_L;
                pyStart = new PyInteger(start);

                if (start < L - 1) {
                    // The other way of saying [start:stop:-s] is [start-L:stop:-s]
                    pyStart_L = new PyInteger(start - L);
                } else {
                    // The other way of saying [L-1:stop:-s] is [:stop:-s]
                    pyStart_L = Py.None;
                }

                for (int stop = start; stop >= -1; stop--) {

                    // Make a reference answer by picking elements of aRef in slice pattern
                    bList.clear();
                    for (int i = start; i > stop; i += step) {
                        if (verbose >= 5) {
                            System.out.printf("    (%d,%d,%d) i=%d\n", start, stop, step, i);
                        }
                        bList.add(new PyInteger(aRef[i]));
                    }

                    // PyObject versions of stop
                    if (stop >= 0) {
                        // The other way of saying [start:stop:-s] is [start:stop-L:-s]
                        pyStop_L = new PyInteger(stop - L);
                    } else {
                        // intended final value is 0, but [start:-1:-s] doesn't mean that
                        stop = -(L + 1);      // This does.
                        pyStop_L = Py.None; // And so does [start::-s]
                    }
                    pyStop = new PyInteger(stop);

                    // Generate test result and check it
                    doTest__getslice__3(a, pyStart, pyStop, pyStep, bList, verbose + 2);
                    // Repeat same result specifying start relative to end
                    doTest__getslice__3(a, pyStart_L, pyStop, pyStep, bList, verbose);
                    // Repeat same result specifying stop relative to end
                    doTest__getslice__3(a, pyStart, pyStop_L, pyStep, bList, verbose);
                    // Repeat same result specifying start and stop relative to end
                    doTest__getslice__3(a, pyStart_L, pyStop_L, pyStep, bList, verbose);
                }
            }
        }
    }

    /**
     * Common code to check {@link PyByteArray#__getslice__(PyObject, PyObject, PyObject)} against a
     * reference answer.
     *
     * @param a answer from method under test
     * @param pyStart
     * @param pyStop
     * @param pyStep
     * @param bList reference answer
     * @param verbose 0..4 control output
     */
    private void doTest__getslice__3(BaseBytes a, PyObject pyStart, PyObject pyStop,
            PyObject pyStep, List<PyInteger> bList, int verbose) {
        if (verbose >= 4) {
            System.out.printf("    __getslice__(%s,%s,%s)\n", pyStart, pyStop, pyStep);
        }
        PyObject b = a.__getslice__(pyStart, pyStop, pyStep);
        if (verbose >= 3) {
            System.out.println(toString((BaseBytes)b));
        }
        checkInts(bList, b);
    }

    /**
     * Test method for {@link PyByteArray#__setitem__(int,PyObject)}, and through it of
     * {@link PyByteArray#pyset(int,PyObject)}.
     */
    @Override
    public void testPyset() {
        int verbose = 0;

        // Need interpreter
        interp = new PythonInterpreter();

        // Fill with random stuff
        int[] aRef = randomInts(random, MEDIUM);
        BaseBytes a = getInstance(aRef);
        for (int i = 0; i < MEDIUM; i++) {
            int b = aRef[i] ^ 0x55; // != a[i]
            PyInteger pyb = new PyInteger(b);
            a.__setitem__(i, pyb);
            int ai = a.pyget(i).asInt();
            if (verbose >= 3) {
                System.out.printf("    __setitem__(%2d,%3d) : a[%2d]=%3d\n", i, b, i, ai);
            }
            assertEquals(b, ai);
        }

        // Check ValueError exceptions generated
        int[] badValue = {256, Integer.MAX_VALUE, -1, -2, -100, -0x10000, Integer.MIN_VALUE};
        for (int i : badValue) {
            PyInteger b = new PyInteger(i);
            try {
                a.__setitem__(0, b);
                fail("Exception not thrown for __setitem__(" + 0 + ", " + b + ")");
            } catch (PyException pye) {
                assertEquals(Py.ValueError, pye.type);
                if (verbose >= 2) {
                    System.out.printf("    Exception: %s\n", pye);
                }
            }
        }

        // Check IndexError exceptions generated
        PyInteger x = new PyInteger(10);
        for (int i : new int[] {-1 - MEDIUM, -100 - MEDIUM, MEDIUM, MEDIUM + 1}) {
            try {
                a.__setitem__(i, x);
                fail("Exception not thrown for __setitem__(" + i + ", x)");
            } catch (PyException pye) {
                assertEquals(Py.IndexError, pye.type);
                if (verbose >= 2) {
                    System.out.printf("    Exception: %s\n", pye);
                }
            }
        }

    }

    /**
     * Test method for {@link org.python.core.PyByteArray#setslice(int,int,int,PyObject)}, when the
     * slice to replace is simple (a contiguous 2-argument slice).
     */
    public void testSetslice2() {
        int verbose = 0;

        // Tests where we transform aaaaaDDDDbbbbb into aaaaaEEEEEEEbbbbb.
        // Lists of the lengths to try, for each of the aaaa, DDDD, bbbb, EEEEEE sections
        int[] naList = {2, 5, 0}; // Interesting cases: slice is at start, or not at start
        int[] ndList = {5, 20, 0}; // Slice to replace is small, large or zero
        int[] nbList = {4, 7, 0}; // Interesting cases: slice is at end, or not at end
        int[] neList = {4, 5, 6, 20, 0}; // Insert smaller, same, large or zero

        for (int ne : neList) {
            int[] eInts = new int[ne];
            Arrays.fill(eInts, 'E');
            PyByteArray e = new PyByteArray(eInts);

            for (int nd : ndList) {
                for (int na : naList) {
                    for (int nb : nbList) {
                        int[] aRef = adbInts(na, nd, nb);
                        int[] bRef = aebInts(na, ne, nb);

                        PyByteArray b = getInstance(aRef);

                        byte[] oldStorage = b.storage;

                        if (verbose >= 2) {
                            System.out.printf("setslice(%d,%d,%d,e[len=%d])\n", na, na + nd, 1, ne);
                            if (verbose >= 3) {
                                System.out.println(toString(b));
                            }
                        }

                        b.setslice(na, na + nd, 1, e);

                        if (verbose >= 2) {
                            boolean avAlloc =
                                    (b.storage != oldStorage) && (bRef.length <= oldStorage.length);
                            if (b.storage.length * 2 < oldStorage.length) {
                                avAlloc = false;
                            }
                            System.out.println(toString(b) + (avAlloc ? " avoidable new" : ""));
                        }
                        checkInts(bRef, b);
                    }
                }
            }
        }

        // Insertions at a range of positions and all sizes with random data

        final int AMAX = SMALL;
        final int BMAX = SMALL;
        final int DMAX = MEDIUM;
        final int EMAX = MEDIUM;

        int[] xInts = randomInts(random, AMAX + DMAX + BMAX, 'u', 'z');
        int[] yInts = randomInts(random, EMAX, 'A', 'H');
        PyByteArray x = getInstance(xInts);
        PyByteArray y = getInstance(yInts);

        int[] nbList2 = {0, 1, BMAX};

        for (int na = 0; na <= AMAX; na++) {
            for (int nb : nbList2) {
                for (int nd = 0; nd < DMAX; nd++) {
                    for (int ne = 0; ne < EMAX; ne++) {
                        PyByteArray u = x.getslice(0, na + nd + nb, 1);
                        PyByteArray e = y.getslice(0, ne, 1);
                        if (verbose >= 2) {
                            System.out.printf("setslice(start=%d, stop=%d, step=%d, e[len=%d])\n",
                                    na, na + nd, 1, ne);
                            if (verbose >= 3) {
                                System.out.println("u = " + toString(u));
                                System.out.println("e = " + toString(e));
                            }
                        }
                        u.setslice(na, na + nd, 1, e);
                        if (verbose >= 1) {
                            System.out.println("u'= " + toString(u));
                        }
                        checkSlice(na, nd, nb, ne, xInts, yInts, u);
                    }
                }
            }
        }
    }

    /**
     * Test method for {@link PyByteArray#__setslice__(PyObject, PyObject, PyObject)}, when the
     * slice to replace is simple (a contiguous 2-argument slice).
     */
    public void test__setslice__2() {
        int verbose = 0;
        // __setslice__() deals with start, stop values also relative to the end.
        String ver = "Cet autre affecte tes langueurs";
        final int L = ver.length();
        int[] uRef = toInts(ver);

        // Need interpreter (for Py.None and exceptions)
        interp = new PythonInterpreter();

        // Source of assigned values.
        int[] eRef = randomInts(random, 2 * SMALL, 'V', 'Z');
        BaseBytes eFull = new BaseBytesTest.MyBytes(eRef);

        final int[] posStart = new int[] {0, 4, 10, 18, L - 9};
        final int[] posStop = new int[] {0, 3, 9, 17, L - 10, L};

        for (int start : posStart) {
            PyObject pyStart, pyStop, pyStart_L, pyStop_L;
            pyStart = new PyInteger(start);
            if (start > 0) {
                // The other way of saying [start:stop] is [start-L:stop]
                pyStart_L = new PyInteger(start - L);
            } else {
                // The other way of saying [0:stop] is [:stop]
                pyStart_L = Py.None;
            }

            for (int stop : posStop) {
                if (stop < start) {
                    continue; // Skip backwards cases
                }

                // PyObject versions of stop
                if (stop < L) {
                    // The other way of saying [start:stop] is [start:stop-L]
                    pyStop_L = new PyInteger(stop - L);
                } else {
                    // The other way of saying [start:L] is [start:]
                    pyStop_L = Py.None;
                }
                pyStop = new PyInteger(stop);

                for (int n = 0; n <= eRef.length; n++) {
                    // Generate test result and check it
                    doTest__setslice__2(uRef, pyStart, pyStop, eFull, n, eRef, start, stop,
                            verbose + 2);
                    // Repeat same result specifying start relative to end
                    doTest__setslice__2(uRef, pyStart_L, pyStop, eFull, n, eRef, start, stop,
                            verbose);
                    // Repeat same result specifying stop relative to end
                    doTest__setslice__2(uRef, pyStart, pyStop_L, eFull, n, eRef, start, stop,
                            verbose);
                    // Repeat same result specifying start and stop relative to end
                    doTest__setslice__2(uRef, pyStart_L, pyStop_L, eFull, n, eRef, start, stop,
                            verbose);
                }
            }
        }
    }

    /**
     * Common code to check {@link PyByteArray#__setslice__(PyObject, PyObject, PyObject)} against a
     * reference answer.
     *
     * @param uRef to initialise the object to test
     * @param pyStart
     * @param pyStop
     * @param eFull byte array from which to take a slice to insert
     * @param n length of the slice to take from eFull
     * @param eRef from which eFull was initialised
     * @param verbose 0..4 control output
     */
    private void doTest__setslice__2(int[] uRef, PyObject pyStart, PyObject pyStop,
            BaseBytes eFull, int n, int[] eRef, int start, int stop, int verbose) {
        PyByteArray u = getInstance(uRef);
        BaseBytes e = eFull.getslice(0, n, 1);
        if (verbose >= 4) {
            System.out.printf("    __setslice__(%s,%s,e[0:%d])\n", pyStart, pyStop, n);
            System.out.println("u = " + toString(u));
            System.out.println("e = " + toString(e));
        }
        // Now do the test
        u.__setslice__(pyStart, pyStop, e);
        if (verbose >= 3) {
            System.out.println("u'= " + toString(u));
        }
        int nd = stop - start;
        int nb = uRef.length - stop;
        checkSlice(start, nd, nb, n, uRef, eRef, u);
    }

    /**
     * Test method for {@link org.python.core.PyByteArray#setslice(int,int,int,PyObject)}, when the
     * slice to replace is extended (3-argument slice and step!=0). Note that PySequence checks and
     * converts arguments first, so we need only test with valid combinations of indices.
     */
    @Override
    public void testSetslice3() {
        int verbose = 0;

        // Need interpreter
        interp = new PythonInterpreter();

        // Source of assigned values.
        int[] eRef = randomInts(random, MEDIUM, 'A', 'H');
        BaseBytes eFull = new BaseBytesTest.MyBytes(eRef);
        int[] uRef = randomInts(random, MEDIUM, 'm', 's');

        // Positive step sizes we will try
        int[] posStep = {2, 3, 5, 8, 25, 100};

        for (int start = 0; start < uRef.length; start++) {
            // Bytes from start to end of array
            int len = uRef.length - start;
            for (int step : posStep) {
                // Allowable number of assignments to end of array at given step size
                int nmax = (len + step - 1) / step;
                for (int n = 1; n <= nmax; n++) {
                    // Location of last i
                    int last = start + step * (n - 1) + 1;
                    // But any stop value in this range results in n assignments
                    for (int stop = last + 1; stop < last + step; stop++) {
                        // Now do the test
                        PyByteArray u = getInstance(uRef);
                        BaseBytes e = eFull.getslice(0, n, 1);
                        if (verbose >= 2) {
                            System.out.printf("setslice(start=%d, stop=%d, step=%d, e[len=%d])\n",
                                    start, stop, step, n);
                            if (verbose >= 3) {
                                System.out.println("u = " + toString(u));
                                System.out.println("e = " + toString(e));
                            }
                        }
                        u.setslice(start, stop, step, e);
                        if (verbose >= 1) {
                            System.out.println("u'= " + toString(u));
                        }
                        checkSlice(start, step, n, uRef, eRef, u);
                    }
                }
            }
        }

        // Negative step sizes we will try
        int[] negStep = {-1, -2, -5, -8, -25, -100};

        for (int start = uRef.length - 1; start >= 0; start--) {
            // Bytes from slice start to start of array
            int len = start + 1;
            for (int step : negStep) {
                // Allowable number of assignments to end of array at given step size
                int nmax = (len + (-step) - 1) / (-step);
                for (int n = 1; n <= nmax; n++) {
                    // Location of last i
                    int last = start + step * (n - 1) - 1;
                    // But any stop value in this range results in n assignments
                    for (int stop = last; stop > last - (-step) && stop >= 0; stop--) {
                        // Now do the test
                        PyByteArray u = getInstance(uRef);
                        BaseBytes e = eFull.getslice(0, n, 1);
                        if (verbose >= 2) {
                            System.out.printf("setslice(start=%d, stop=%d, step=%d, e[len=%d])\n",
                                    start, stop, step, n);
                            if (verbose >= 3) {
                                System.out.println("u = " + toString(u));
                                System.out.println("e = " + toString(e));
                            }
                        }
                        u.setslice(start, stop, step, e);
                        if (verbose >= 1) {
                            System.out.println("u'= " + toString(u));
                        }
                        checkSlice(start, step, n, uRef, eRef, u);
                    }
                }
            }
        }

    }

    /**
     * Test method for {@link org.python.core.PyByteArray#setslice(int,int,int,PyObject)}, when the
     * slice to replace is extended (3-argument slice and step!=0). Note that PySequence checks and
     * converts arguments first, so we need only test with valid combinations of indices.
     */
    public void test__setslice__3() {
        int verbose = 0;

        // Need interpreter
        interp = new PythonInterpreter();

        // Source of assigned values.
        int[] eRef = randomInts(random, MEDIUM, 'A', 'H');
        BaseBytes eFull = new BaseBytesTest.MyBytes(eRef);

        // Forwards cases

        int[] uRef = randomInts(random, MEDIUM, 'm', 's');

        // Positive step sizes we will try
        int[] posStep = {2, 3, 5, 8, 25, 100};

        for (int start = 0; start < uRef.length; start++) {
            PyInteger pyStart = new PyInteger(start);
            // Bytes from start to end of array
            int len = uRef.length - start;
            for (int step : posStep) {
                PyInteger pyStep = new PyInteger(step);
                // Allowable number of assignments to end of array at given step size
                int nmax = (len + step - 1) / step;
                for (int n = 1; n <= nmax; n++) {
                    // Location of last i
                    int last = start + step * (n - 1) + 1;
                    // But any stop value in this range results in n assignments
                    for (int stop = last + 1; stop < last + step; stop++) {
                        PyInteger pyStop = new PyInteger(stop);
                        // Now do the test
                        PyByteArray u = getInstance(uRef);
                        BaseBytes e = eFull.getslice(0, n, 1);
                        if (verbose >= 2) {
                            System.out.printf("setslice(%d,%d,%d, e[len=%d])\n", //
                                    start, stop, step, n);
                            if (verbose >= 3) {
                                System.out.println("u = " + toString(u));
                                System.out.println("e = " + toString(e));
                            }
                        }
                        u.__setslice__(pyStart, pyStop, pyStep, e);
                        if (verbose >= 1) {
                            System.out.println("u'= " + toString(u));
                        }
                        checkSlice(start, step, n, uRef, eRef, u);
                    }
                }
            }
        }

        // Negative step sizes we will try
        int[] negStep = {-1, -2, -5, -8, -25, -100};

        for (int start = uRef.length - 1; start >= 0; start--) {
            PyInteger pyStart = new PyInteger(start);
            // Bytes from slice start to start of array
            int len = start + 1;
            for (int step : negStep) {
                PyInteger pyStep = new PyInteger(step);
                // Allowable number of assignments to end of array at given step size
                int nmax = (len + (-step) - 1) / (-step);
                for (int n = 1; n <= nmax; n++) {
                    // Location of last i
                    int last = start + step * (n - 1) - 1;
                    // But any stop value in this range results in n assignments
                    for (int stop = last; stop > last - (-step) && stop >= 0; stop--) {
                        PyInteger pyStop = new PyInteger(stop);
                        // Now do the test
                        PyByteArray u = getInstance(uRef);
                        BaseBytes e = eFull.getslice(0, n, 1);
                        if (verbose >= 2) {
                            System.out.printf("setslice(%d,%d,%d, e[len=%d])\n", //
                                    start, stop, step, n);
                            if (verbose >= 3) {
                                System.out.println("u = " + toString(u));
                                System.out.println("e = " + toString(e));
                            }
                        }
                        u.__setslice__(pyStart, pyStop, pyStep, e);
                        if (verbose >= 1) {
                            System.out.println("u'= " + toString(u));
                        }
                        checkSlice(start, step, n, uRef, eRef, u);
                    }
                }
            }
        }
    }

    /**
     * Performance for {@link org.python.core.PyByteArray#setslice(int,int,int,PyObject)}, when the
     * slice to replace is simple and contiguous (2-argument slice).
     */
    public void testSetsliceTime() {
        int verbose = 1;
        timeSetslice(50, 100, SMALL, 2 * SMALL, verbose);
        timeSetslice(50, 100, MEDIUM, MEDIUM, verbose);
        timeSetslice(500, 20, LARGE, LARGE / 5, verbose);
        // timeSetslice(1000, 4, HUGE, HUGE/5, verbose);
    }

    /**
     * Tabulate the elapsed time for calls to setslice, for a given array size and maximum slice
     * length to insert arrays of a range of sizes. The aim is to demonstrate benefit from the
     * centring of the occupied storage in the storage array as a whole and catch any drop-off in
     * implementation that while functionally correct (gets the right value) is massively
     * inefficient.
     *
     * @param trials number of trials over which to take minimum "uninterrupted" time
     * @param repeats number of repeat calls in each trial, over which to average
     * @param N of bytearray subjected to the change
     * @param M Size of change (inserted, removed or replaced slice)
     * @param verbose Control level of textual output 1=just the timings, 2=enumerate calls, etc..
     */
    private void timeSetslice(int trials, int repeats, int N, int M, int verbose) {

        // Trials we intend to do: insertion at a variety of points.
        int[] startList = new int[11]; // 11 means 0%, 10%, 20%, ... 100% of N
        for (int i = 0; i < startList.length; i++) {
            startList[i] = N * i / (startList.length - 1);
        }

        // Insertion slice sizes.
        int[] changeList = new int[11]; // 0%, ... 100% of M
        for (int i = 0; i < changeList.length; i++) {
            changeList[i] = M * i / (changeList.length - 1);
        }

        // We are going to tabulate this for each startList and changeList entry.
        long[][] elapsed = new long[startList.length][changeList.length];
        // Initialise the timing record
        for (int row = 0; row < startList.length; row++) {
            for (int col = 0; col < changeList.length; col++) {
                elapsed[row][col] = Long.MAX_VALUE;
            }
        }

        // Create test material as bytearrays
        int[] xRef = randomInts(random, N, 'u', 'z');
        PyByteArray x = getInstance(xRef);
        int[] yRef = randomInts(random, M, 'A', 'H');
        PyByteArray y = getInstance(yRef);

        // We will time repeated calls: need a fresh bytearray each time
        PyByteArray[] u = new PyByteArray[repeats];

        // Now take the shortest of some number of trials in each row and column
        for (int trial = 0; trial < trials; trial++) {
            // Work through the combinations necessary
            for (int irow = 0; irow < startList.length; irow++) {
                int row = (irow + 5 * trial) % startList.length;     // Shuffle order
                int na = startList[row];
                int nd = 0;
                for (int icol = 0; icol < changeList.length; icol++) {
                    int col = (icol + trial) % changeList.length;     // Shuffle order
                    int ne = changeList[col];
                    int start = na;
                    int stop = na + nd;
                    // Data to replace the slice with
                    PyByteArray e = y.getslice(0, ne, 1);

                    if (trial == 0) {
                        // First trial: do once untimed in order ensure classes loaded.
                        doTimeSetslice(u, start, stop, e, x, verbose);
                        checkSlice(na, nd, N - (na + nd), ne, xRef, yRef, u[0]);
                    }

                    // Now do the trial properly
                    long t = doTimeSetslice(u, start, stop, e, x, -1);

                    // Retain the shortest time so far
                    if (t < elapsed[row][col]) {
                        elapsed[row][col] = t;
                    }
                }
            }
        }

        // Tabulate the time for each array size and change size

        if (verbose >= 1) {
            System.out.print("     N  ,     na  ");
            for (int col = 0; col < changeList.length; col++) {
                System.out.printf(", ne=%7d", changeList[col]);
            }
            System.out.println(", elements inserted: time in microseconds.");

            for (int row = 0; row < startList.length; row++) {
                System.out.printf("%8d, %8d", N, startList[row]);
                for (int col = 0; col < changeList.length; col++) {
                    double usPerCall = (1e-3 * elapsed[row][col]) / repeats;
                    System.out.printf(", %10.3f", usPerCall);
                    // System.out.printf(", %10d", elapsed[row][col]);
                }
                System.out.println();
            }
        }
    }

    /**
     * Time trial of {@link PyByteArray#setslice(int,int,int)}. Every element of the array of test
     * objects will be initialised to the same value then the specified slice replacement will take
     * place, with the block of repetitions timed.
     *
     * @param u array of test objects
     * @param start
     * @param stop
     * @param e to insert over [start:stop]
     * @param x value from which to initialise each test object
     * @param verbose amount of output
     * @return elapsed time in nanoseconds for setslice operation on array of objects
     */
    private long doTimeSetslice(PyByteArray[] u, int start, int stop, BaseBytes e, BaseBytes x,
            int verbose) {

        // The call is either to do a time trial (block of test objects) or one test of correctness
        int repeats = 1;
        if (verbose < 0) {
            // We're doing a timed trial on an array of identical objects.
            repeats = u.length;
        }

        // Set up clean bytearray objects
        for (int i = 0; i < repeats; i++) {
            u[i] = new PyByteArray(x);
        }

        // Display effects (if verbose) using first element only.
        PyByteArray v = u[0];
        byte[] oldStorage = v.storage;

        if (verbose >= 3) {
            System.out.printf("setslice(%d,%d,%d,e[%d])\n", start, stop, 1, e.size());
            System.out.println("u = " + toString(v));
            System.out.println("e = " + toString(e));
        }

        // Start the clock
        long beginTime = System.nanoTime();
        // Do the work lots of times
        for (int i = 0; i < repeats; i++) {
            u[i].setslice(start, stop, 1, e);
        }
        // Stop the clock
        long t = System.nanoTime() - beginTime;

        // Diagnostic printout
        if (verbose >= 2) {
            // Was there a reallocation?
            boolean avAlloc = (v.storage != oldStorage);
            // Justified if ...
            if (v.size * 2 <= oldStorage.length) {
                avAlloc = false;
            }
            if (v.size > oldStorage.length) {
                avAlloc = false;
            }
            System.out.println("u'= " + toString(v) + (avAlloc ? " new" : ""));
        }

        return t;
    }

    /**
     * Test method for {@link org.python.core.PyByteArray#delslice(int,int,int)}, when the slice to
     * delete is simple (a contiguous 2-argument slice).
     */
    public void testDelslice2() {
        int verbose = 0;

        // Tests where we transform aaaaaDDDDbbbbb into aaaaabbbbb.
        // Lists of the lengths to try, for each of the aaaa, DDDD, bbbb sections
        int[] naList = {2, 5, 0};   // Interesting cases: slice is at start, or not at start
        int[] ndList = {5, 20, 0};  // Slice to delete is small, large or zero
        int[] nbList = {4, 7, 0};   // Interesting cases: slice is at end, or not at end

        for (int nd : ndList) {
            for (int na : naList) {
                for (int nb : nbList) {
                    int[] aRef = adbInts(na, nd, nb);
                    int[] bRef = aebInts(na, 0, nb);

                    PyByteArray b = getInstance(aRef);

                    byte[] oldStorage = b.storage;

                    if (verbose >= 2) {
                        System.out.printf("delslice(%d,%d,%d,%d)\n", na, na + nd, 1, nd);
                        if (verbose >= 3) {
                            System.out.println(toString(b));
                        }
                    }

                    b.delslice(na, na + nd, 1, nd);

                    if (verbose >= 2) {
                        // Was there a reallocation?
                        boolean avAlloc = (b.storage != oldStorage);
                        // Justified if ...
                        if (bRef.length * 2 <= oldStorage.length) {
                            avAlloc = false;
                        }
                        System.out.println(toString(b) + (avAlloc ? " avoidable new" : ""));
                    }
                    checkInts(bRef, b);
                }
            }
        }

        // Deletions at a range of positions and all sizes with random data

        final int AMAX = SMALL;
        final int BMAX = SMALL;
        final int DMAX = MEDIUM;

        int[] xInts = randomInts(random, AMAX + DMAX + BMAX, 'u', 'z');
        PyByteArray x = getInstance(xInts);
        // Use the checker for assignments, pretending to have assigned a zero length array.
        // int[] yInts = new int[0];

        int[] nbList2 = {0, 1, BMAX};

        for (int na = 0; na <= AMAX; na++) {
            for (int nb : nbList2) {
                for (int nd = 0; nd < DMAX; nd++) {
                    PyByteArray u = x.getslice(0, na + nd + nb, 1);
                    if (verbose >= 2) {
                        System.out.printf("delslice(start=%d, stop=%d, step=%d, n=%d)\n", na, na
                                + nd, 1, nd);
                        if (verbose >= 3) {
                            System.out.println("u = " + toString(u));
                        }
                    }
                    u.delslice(na, na + nd, 1, nd);
                    if (verbose >= 1) {
                        System.out.println("u'= " + toString(u));
                    }
                    checkSlice(na, nd, nb, 0, xInts, null, u);
                }
            }
        }
    }

    /**
     * Test method for {@link PyByteArray#__delslice__(PyObject, PyObject, PyObject)}, when the
     * slice to delete is simple (a contiguous 2-argument slice).
     */
    public void test__delslice__2() {
        int verbose = 0;
        // __delslice__() deals with start, stop values also relative to the end.

        String ver = "Et tes pâleurs, alors que lasse,";
        final int L = ver.length();
        int[] uRef = toInts(ver);

        // Need interpreter (for Py.None and exceptions)
        interp = new PythonInterpreter();

        final int[] posStart = new int[] {0, 3, 7, 16, L - 1};
        final int[] posStop = new int[] {0, 3, 7, 16, L - 6, L};

        for (int start : posStart) {
            PyObject pyStart, pyStop, pyStart_L, pyStop_L;
            pyStart = new PyInteger(start);
            if (start > 0) {
                // The other way of saying [start:stop] is [start-L:stop]
                pyStart_L = new PyInteger(start - L);
            } else {
                // The other way of saying [0:stop] is [:stop]
                pyStart_L = Py.None;
            }

            for (int stop : posStop) {
                if (stop < start) {
                    continue; // Skip backwards cases
                }

                // PyObject versions of stop
                if (stop < L) {
                    // The other way of saying [start:stop] is [start:stop-L]
                    pyStop_L = new PyInteger(stop - L);
                } else {
                    // The other way of saying [start:L] is [start:]
                    pyStop_L = Py.None;
                }
                pyStop = new PyInteger(stop);

                // Generate test result and check it
                doTest__delslice__2(uRef, pyStart, pyStop, start, stop, verbose + 2);
                // Repeat same result specifying start relative to end
                doTest__delslice__2(uRef, pyStart_L, pyStop, start, stop, verbose);
                // Repeat same result specifying stop relative to end
                doTest__delslice__2(uRef, pyStart, pyStop_L, start, stop, verbose);
                // Repeat same result specifying start and stop relative to end
                doTest__delslice__2(uRef, pyStart_L, pyStop_L, start, stop, verbose);
            }
        }
    }

    /**
     * Common code to check {@link PyByteArray#__delslice__(PyObject, PyObject, PyObject)} against a
     * reference answer.
     *
     * @param uRef to initialise the object to test
     * @param pyStart
     * @param pyStop
     * @param verbose 0..4 control output
     */
    private void doTest__delslice__2(int[] uRef, PyObject pyStart, PyObject pyStop, int start,
            int stop, int verbose) {
        PyByteArray u = getInstance(uRef);
        if (verbose >= 4) {
            System.out.printf("    __delslice__(%s,%s,1)\n", pyStart, pyStop);
            System.out.println("u = " + toString(u));
        }
        // Now do the test
        u.__delslice__(pyStart, pyStop);
        if (verbose >= 3) {
            System.out.println("u'= " + toString(u));
        }
        int nd = stop - start;
        int nb = uRef.length - stop;
        checkSlice(start, nd, nb, 0, uRef, null, u);
    }

    /**
     * Test method for {@link org.python.core.PyByteArray#delslice(int,int,int)}, when the slice to
     * delete is extended (3-argument slice and step!=0). Note that PySequence checks and converts
     * arguments first, so we need only test with valid combinations of indices.
     */
    public void test__delslice__3() {
        int verbose = 0;

        // Need interpreter
        interp = new PythonInterpreter();

        // Forwards cases

        int[] uRef = randomInts(random, MEDIUM, 'm', 's');

        // Positive step sizes we will try
        int[] posStep = {2, 3, 5, 8, 25, 100};

        for (int start = 0; start < uRef.length; start++) {
            PyInteger pyStart = new PyInteger(start);
            // Bytes from start to end of array
            int len = uRef.length - start;
            for (int step : posStep) {
                PyInteger pyStep = new PyInteger(step);
                // Allowable number of deletions to end of array at given step size
                int nmax = (len + step - 1) / step;
                for (int n = 1; n <= nmax; n++) {
                    // Location of last i
                    int last = start + step * (n - 1) + 1;
                    // But any stop value in this range results in n deletions
                    for (int stop = last + 1; stop < last + step; stop++) {
                        PyInteger pyStop = new PyInteger(stop);
                        // Now do the test
                        PyByteArray u = getInstance(uRef);
                        if (verbose >= 2) {
                            System.out.printf("__delslice__(%d,%d,%d) (%d deletions)\n", start,
                                    stop, step, n);
                            if (verbose >= 3) {
                                System.out.println("u = " + toString(u));
                            }
                        }
                        u.__delslice__(pyStart, pyStop, pyStep);
                        if (verbose >= 1) {
                            System.out.println("u'= " + toString(u));
                        }
                        checkDelSlice(start, step, n, uRef, u);
                    }
                }
            }
        }

        // Negative step sizes we will try
        int[] negStep = {-1, -2, -5, -8, -25, -100};

        for (int start = uRef.length - 1; start >= 0; start--) {
            PyInteger pyStart = new PyInteger(start);
            // Bytes from slice start to start of array
            int len = start + 1;
            for (int step : negStep) {
                PyInteger pyStep = new PyInteger(step);
                // Allowable number of assignments to end of array at given step size
                int nmax = (len + (-step) - 1) / (-step);
                for (int n = 1; n <= nmax; n++) {
                    // Location of last i
                    int last = start + step * (n - 1) - 1;
                    // But any stop value in this range results in n deletions
                    for (int stop = last; stop > last - (-step) && stop >= 0; stop--) {
                        PyInteger pyStop = new PyInteger(stop);
                        // Now do the test
                        PyByteArray u = getInstance(uRef);
                        if (verbose >= 2) {
                            System.out.printf("__delslice__(%d,%d,%d) (%d deletions)\n", start,
                                    stop, step, n);
                            if (verbose >= 3) {
                                System.out.println("u = " + toString(u));
                            }
                        }
                        u.__delslice__(pyStart, pyStop, pyStep);
                        if (verbose >= 1) {
                            System.out.println("u'= " + toString(u));
                        }
                        checkDelSlice(start, step, n, uRef, u);
                    }
                }
            }
        }

    }

    /**
     * Performance for {@link org.python.core.PyByteArray#delslice(int,int,int,PyObject)}, when the
     * slice to replace is extended (3-argument slice).
     */
    public void testDelsliceTime3() {
        int verbose = 1;
        timeDelslice3(50, 100, SMALL, verbose);
        timeDelslice3(50, 100, MEDIUM, verbose);
        timeDelslice3(20, 4, LARGE, verbose);
        // timeDelslice3(10, 1, HUGE, verbose);
    }

    /**
     * Tabulate the elapsed time for calls to __delslice__, for a given array size and maximum
     * step-size between deleted items length to delete. The aim is to demonstrate benefit from the
     * centring of the occupied storage in the storage array as a whole and catch any drop-off in
     * implementation that while functionally correct (gets the right value) is massively
     * inefficient.
     *
     * @param trials number of trials over which to take minimum "uninterrupted" time
     * @param repeats number of repeat calls in each trial, over which to average
     * @param N of bytearray subjected to the change
     * @param verbose Control level of textual output 1=just the timings, 2=eumerate calls, etc..
     */
    private void timeDelslice3(int trials, int repeats, int N, int verbose) {

        // Trials we intend to do: deletion from a variety of points.
        int[] startList = new int[10]; // 10 means 0%, 10%, 20%, ... 90% of N
        for (int i = 0; i < startList.length; i++) {
            startList[i] = N * i / startList.length;
        }

        // Deletion step sizes. Positive and negative of these will be used
        int[] posStep = {1, 2, 3, 5, 10};
        int[] stepList = new int[2 * posStep.length];
        for (int i = 0; i < posStep.length; i++) {
            stepList[posStep.length - i - 1] = posStep[i];
            stepList[posStep.length + i] = -posStep[i];
        }

        // We are going to tabulate this for each startList and changeList entry.
        long[][] elapsed = new long[startList.length][stepList.length];
        // Initialise the timing record
        for (int row = 0; row < startList.length; row++) {
            for (int col = 0; col < stepList.length; col++) {
                elapsed[row][col] = Long.MAX_VALUE;
            }
        }

        // Create test material as bytearrays
        int[] xRef = randomInts(random, N, 'u', 'z');
        PyByteArray x = getInstance(xRef);

        // We will time repeated calls: need a fresh bytearray each time
        PyByteArray[] u = new PyByteArray[repeats];

        // Now take the shortest of some number of trials in each row and column
        for (int trial = 0; trial < trials; trial++) {

            // Work through the combinations necessary
            for (int irow = 0; irow < startList.length; irow++) {
                int row = (irow + 5 * trial) % startList.length;     // Shuffle order
                int start = startList[row];
                for (int icol = 0; icol < stepList.length; icol++) {
                    int col = (icol + trial) % stepList.length;    // Shuffle order
                    int step = stepList[col];
                    int n;

                    if (step > 0) {
                        // Deletions available from start location to end of array
                        n = (xRef.length - start + step - 1) / step;
                    } else {
                        // Deletions available from start location to beginning of array
                        n = (start + (-step)) / (-step);
                    }

                    // Make objects of the arguments
                    PyInteger pyStart = new PyInteger(start);
                    PyObject pyStop = Py.None;
                    PyInteger pyStep = new PyInteger(step);

                    if (trial == 0) {
                        // First trial: do once untimed in order ensure classes loaded.
                        doTimeDelslice3(u, pyStart, pyStop, pyStep, x, verbose);
                        checkDelSlice(start, step, n, xRef, u[0]);
                    }

                    // Now do the trial properly
                    long t = doTimeDelslice3(u, pyStart, pyStop, pyStep, x, -1);

                    // Retain the shortest time so far
                    if (t < elapsed[row][col]) {
                        elapsed[row][col] = t;
                    }
                }
            }
        }

        // Tabulate the time for each array size and change size

        System.out.print("     N  ,    start");
        for (int col = 0; col < stepList.length; col++) {
            System.out.printf(", step=%5d", stepList[col]);
        }
        System.out.println(", deletion time in microseconds.");

        for (int row = 0; row < startList.length; row++) {
            System.out.printf("%8d, %8d", N, startList[row]);
            for (int col = 0; col < stepList.length; col++) {
                double usPerCall = (1e-3 * elapsed[row][col]) / repeats;
                System.out.printf(", %10.3f", usPerCall);
                // System.out.printf(", %10d", elapsed[row][col]);
            }
            System.out.println();
        }
    }

    /**
     * Time trial of {@link PyByteArray#__delslice__(PyObject,PyObject,PyObject)}. Every element of
     * the array of test objects will be initialised to the same value then the specified slice
     * replacement will take place, with the block of repetitions timed.
     *
     * @param u array of test objects
     * @param pyStart
     * @param pyStop
     * @param pyStep
     * @param x value from which to initialise each test object
     * @param verbose amount of output
     * @return elapsed time in nanoseconds for delslice operation on array of objects
     */
    private long doTimeDelslice3(PyByteArray[] u, PyObject pyStart, PyObject pyStop,
            PyObject pyStep, BaseBytes x, int verbose) {

        // The call is either to do a time trial (block of test objects) or one test of correctness
        int repeats = 1;
        if (verbose < 0) {
            // We're doing a timed trial on an array of identical objects.
            repeats = u.length;
        }

        // Set up clean bytearray objects
        for (int i = 0; i < repeats; i++) {
            u[i] = new PyByteArray(x);
        }

        // Display effects (if verbose) using first element only.
        PyByteArray v = u[0];
        byte[] oldStorage = v.storage;

        if (verbose >= 3) {
            System.out.printf("__delslice__(%s,%s,%s)\n", pyStart, pyStop, pyStep);
            System.out.println("u = " + toString(v));
        }

        // Start the clock
        long beginTime = System.nanoTime();
        // Do the work lots of times
        for (int i = 0; i < repeats; i++) {
            u[i].__delslice__(pyStart, pyStop, pyStep);
        }
        // Stop the clock
        long t = System.nanoTime() - beginTime;

        // Diagnostic printout
        if (verbose >= 2) {
            // Was there a reallocation?
            boolean avAlloc = (v.storage != oldStorage);
            // Justified if ...
            if (v.size * 2 <= oldStorage.length) {
                avAlloc = false;
            }
            if (v.size > oldStorage.length) {
                avAlloc = false;
            }
            System.out.println("u'= " + toString(v) + (avAlloc ? " new" : ""));
        }

        return t;
    }

    /*
     * Note that JUnit test classes extending this one inherit all the test* methods, and they will
     * be run by JUnit. Each test uses getInstance() methods where it might have used a constructor
     * with a similar signature. The idea is to override the getInstance() methods to return an
     * instance of the class actually under test in the derived test.
     */
    @Override
    public PyByteArray getInstance(PyType type) {
        return new PyByteArray(type);
    }

    @Override
    public PyByteArray getInstance() {
        return new PyByteArray();
    }

    @Override
    public PyByteArray getInstance(int size) {
        return new PyByteArray(size);
    }

    @Override
    public PyByteArray getInstance(int[] value) {
        return new PyByteArray(value);
    }

    @Override
    public PyByteArray getInstance(BaseBytes source) {
        return new PyByteArray(source);
    }

    @Override
    public PyByteArray getInstance(Iterable<? extends PyObject> source) {
        return new PyByteArray(source);
    }

    @Override
    public PyByteArray getInstance(PyString arg, PyObject encoding, PyObject errors) {
        return new PyByteArray(arg, encoding, errors);
    }

    @Override
    public PyByteArray getInstance(PyString arg, String encoding, String errors) {
        return new PyByteArray(arg, encoding, errors);
    }

    @Override
    public PyByteArray getInstance(PyObject arg) throws PyException {
        return new PyByteArray(arg);
    }

}
