package org.python.compiler.custom_proxymaker;

/* 
 * This test tests that we can create JUnit 4 tests in Python and that JUnit's own 
 * reflection system picks our annotations and runs the underlying code
 */

import org.junit.Test;
import org.junit.runner.JUnitCore;
import org.python.util.ProxyCompiler;

public class JUnitTest {
    @Test
    public void testMethodSignatures() throws Exception {
        ProxyCompiler.compile("tests/python/custom_proxymaker/junit_test.py", "build/classes");
        JUnitCore.runClasses(Class.forName("custom_proxymaker.tests.JUnitTest"));
    }
}
