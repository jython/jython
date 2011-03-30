package org.python.core;

import junit.framework.TestCase;

public class PyFloatTest extends TestCase {

    private final static double nan = Double.NaN;

    private final static double inf = Double.POSITIVE_INFINITY;

    private final static double ninf = Double.NEGATIVE_INFINITY;

    /**
     * test the basic behavior of java.lang.Double extreme values
     */
    public void test_Double_InfinityAndNaN() {
        assertTrue(Double.NaN != nan); // this is the definition of NaN
        assertTrue(Double.isNaN(nan));
        assertFalse(Double.isInfinite(nan));
        assertTrue(Double.POSITIVE_INFINITY == inf);
        assertFalse(Double.isNaN(inf));
        assertTrue(Double.isInfinite(inf));
        assertTrue(Double.NEGATIVE_INFINITY == ninf);
        assertFalse(Double.isNaN(ninf));
        assertTrue(Double.isInfinite(ninf));
        assertTrue(nan != inf);
        assertTrue(nan != ninf);
        assertTrue(inf != ninf);
    }

    /**
     * test extreme values
     */
    public void testInfinityAndNaN() {
        PyFloat fNan = new PyFloat(Double.NaN);
        PyFloat fInf = new PyFloat(Double.POSITIVE_INFINITY);
        PyFloat fNinf = new PyFloat(Double.NEGATIVE_INFINITY);
        assertTrue(Double.NaN != fNan.getValue()); // this is the definition of NaN
        assertTrue(Double.isNaN(fNan.getValue()));
        assertFalse(Double.isInfinite(fNan.getValue()));
        assertTrue(Double.POSITIVE_INFINITY == fInf.getValue());
        assertFalse(Double.isNaN(fInf.getValue()));
        assertTrue(Double.isInfinite(fInf.getValue()));
        assertTrue(Double.NEGATIVE_INFINITY == fNinf.getValue());
        assertFalse(Double.isNaN(fNinf.getValue()));
        assertTrue(Double.isInfinite(fNinf.getValue()));
        assertTrue(fNan.getValue() != fInf.getValue());
        assertTrue(fNan.getValue() != fNinf.getValue());
        assertTrue(fInf.getValue() != fNinf.getValue());
    }

    /**
     * test formatting of extreme values
     */
    public void testInfinityAndNaN_repr() {
        PyFloat fNan = new PyFloat(Double.NaN);
        PyFloat fInf = new PyFloat(Double.POSITIVE_INFINITY);
        PyFloat fNinf = new PyFloat(Double.NEGATIVE_INFINITY);
        assertEquals("nan", String.valueOf(fNan.__repr__()));
        assertEquals("inf", String.valueOf(fInf.__repr__()));
        assertEquals("-inf", String.valueOf(fNinf.__repr__()));
    }
}
