package org.python.compiler.custom_proxymaker;

/*
 * Test support for Python class annotations
 */

import static org.junit.Assert.*;

import org.junit.*;
import org.python.util.ProxyCompiler;

public class ClassAnnotationTest {
    
    Class<?> proxy;
    
    @Before
    public void setUp() throws Exception {
        ProxyCompiler.compile("tests/python/custom_proxymaker/annotated_class.py", "build/classes");
        proxy = Class.forName("custom_proxymaker.tests.AnnotatedInputStream");
    }
    
    @Test
    public void hasClassAnnotation() {
        // Just by "finding" it we satisfy the test.
        @SuppressWarnings("unused")
        Deprecated deprecatedAnnotation = proxy.getAnnotation(Deprecated.class);
    }
    
    @Test
    public void hasCustomAnnotationWithFields() throws Exception {
        CustomAnnotation customAnnotation = proxy.getAnnotation(CustomAnnotation.class);
        assertEquals("Darusik", customAnnotation.createdBy());
        assertEquals(CustomAnnotation.Priority.LOW, customAnnotation.priority());
        assertArrayEquals(new String[] {"Darjus", "Darjunia"}, customAnnotation.changedBy());
    }
}
