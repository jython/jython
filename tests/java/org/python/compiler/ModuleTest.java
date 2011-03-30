package org.python.compiler;

import junit.framework.TestCase;

public class ModuleTest extends TestCase {

    /**
     * In order to get testCopysign() in test_math.py passing, PyFloatConstant needs to distinguish
     * between 0.0 and -0.0:
     * 
     * <pre>
     *   # copysign should let us distinguish signs of zeros
     * </pre>
     */
    public void testPyFloatConstant_Zero() {
        PyFloatConstant positiveZero = new PyFloatConstant(0.0);
        PyFloatConstant negativeZero = new PyFloatConstant(-0.0);
        assertNotSame(positiveZero, negativeZero);
        assertFalse(positiveZero.equals(negativeZero));
    }
}
