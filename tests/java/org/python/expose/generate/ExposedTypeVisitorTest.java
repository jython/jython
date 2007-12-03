package org.python.expose.generate;

import junit.framework.TestCase;

import org.objectweb.asm.Type;
import org.python.core.PyObject;
import org.python.expose.generate.ExposedTypeVisitor;

public class ExposedTypeVisitorTest extends TestCase {

    public void setUp() {
        etv = new ExposedTypeVisitor(Type.getType("Lsimpletype;")) {

            @Override
            public void handleResult(String name) {
                result = name;
            }

            @Override
            public void handleResult(Type base) {
                baseResult = base;
            }
        };
    }

    public void testSimpleType() {
        etv.visitEnd();
        assertEquals("simpletype", result);
        assertEquals(Type.getType(Object.class), baseResult);
    }

    public void testNamedType() {
        etv.visit("name", "different");
        etv.visit("base", Type.getType(PyObject.class));
        etv.visitEnd();
        assertEquals("different", result);
        assertEquals(Type.getType(PyObject.class), baseResult);
    }

    ExposedTypeVisitor etv;

    private String result;
    
    private Type baseResult;
}
