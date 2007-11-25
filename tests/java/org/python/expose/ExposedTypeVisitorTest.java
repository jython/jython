package org.python.expose;

import junit.framework.TestCase;

import org.objectweb.asm.Type;

public class ExposedTypeVisitorTest extends TestCase {

    public void setUp() {
        etv = new ExposedTypeVisitor(Type.getType("Lsimpletype;")) {

            @Override
            public void handleResult(String name) {
                result = name;
            }
        };
    }

    public void testSimpleType() {
        etv.visitEnd();
        assertEquals("simpletype", result);
    }

    public void testNamedType() {
        etv.visit("name", "different");
        etv.visitEnd();
        assertEquals("different", result);
    }

    ExposedTypeVisitor etv;

    private String result;
}
