package org.python.expose.generate;

import java.io.IOException;
import java.io.InputStream;

import junit.framework.TestCase;

import org.python.core.BytecodeLoader;
import org.python.core.PyBuiltinFunction;
import org.python.core.PyObject;
import org.python.expose.generate.ExposedTypeProcessor;
import org.python.expose.generate.MethodExposer;

public class ExposedTypeProcessorTest extends TestCase {

    public void testDetectType() throws Exception {
        InputStream in = getClass().getClassLoader()
                .getResourceAsStream("org/python/expose/generate/SimpleExposed.class");
        ExposedTypeProcessor ice = new ExposedTypeProcessor(in);
        assertEquals("simpleexposed", ice.getName());
        assertEquals(10, ice.getMethodExposers().size());
        assertNotNull(ice.getNewExposer());
        assertEquals(1, ice.getDescriptorExposers().size());
        assertEquals("simpleexposed", ice.getTypeExposer().getName());
        BytecodeLoader.Loader loader = new BytecodeLoader.Loader();
        Class simple_method = null;
        for(MethodExposer exposer : ice.getMethodExposers()) {
            if(exposer.getNames()[0].equals("invisible")) {
                simple_method = exposer.load(loader);
            } else {
                exposer.load(loader);
            }
        }
        for(DescriptorExposer exposer : ice.getDescriptorExposers()) {
            exposer.load(loader);
        }
        ice.getNewExposer().load(loader);
        ice.getTypeExposer().load(loader);
        Class doctoredSimple = loader.loadClassFromBytes("org.python.expose.generate.SimpleExposed",
                                                         ice.getBytecode());
        PyObject simp = (PyObject)doctoredSimple.newInstance();
        PyBuiltinFunction func = MethodExposerTest.instantiate(simple_method, "invisible");
        PyBuiltinFunction bound = func.bind(simp);
        bound.__call__();
    }

    public void testNoAnnotationType() throws IOException {
        InputStream in = getClass().getClassLoader()
                .getResourceAsStream("org/python/expose/generate/ExposedTypeProcessorTest.class");
        try {
            new ExposedTypeProcessor(in);
            fail("Shouldn't be able to create an InnerClassExposer with a class without ExposedType");
        } catch(IllegalArgumentException iae) {
            // Expected since there is no @ExposedType on InnerClassExposerTest
        }
    }
}
