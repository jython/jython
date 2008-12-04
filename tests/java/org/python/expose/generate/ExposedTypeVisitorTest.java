package org.python.expose.generate;

import junit.framework.TestCase;

import org.python.objectweb.asm.Type;
import org.python.core.PyObject;
import org.python.expose.generate.ExposedTypeVisitor;

public class ExposedTypeVisitorTest extends TestCase {

    public void setUp() {
        etv = new ExposedTypeVisitor(Type.getType("Lsimpletype;"), null) {

            @Override
            public void handleResult(String name) {
                result = name;
            }

            @Override
            public void handleResult(Type base) {
                baseResult = base;
            }

            @Override
            public void handleResult(boolean isBaseType) {
                isBaseTypeResult = isBaseType;
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
        etv.visit("isBaseType", false);
        etv.visitEnd();
        assertEquals("different", result);
        assertEquals(Type.getType(PyObject.class), baseResult);
        assertEquals(false, isBaseTypeResult);
    }

    ExposedTypeVisitor etv;

    private String result;

    private Type baseResult;

    private boolean isBaseTypeResult;
}
