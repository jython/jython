package org.python.expose.generate;

import junit.framework.TestCase;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.EmptyVisitor;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedNew;

public class ExposeMethodFinderTest extends TestCase implements Opcodes, PyTypes {

    private ExposedMethodFinder makeFinder(int access, String descriptor) {
        return new ExposedMethodFinder("simpleexposed",
                                       Type.getType(SimpleExposed.class),
                                       access,
                                       "simpleMethod",
                                       descriptor,
                                       null,
                                       new EmptyVisitor()) {

            @Override
            public void handleResult(MethodExposer exposer) {
                resultantMethExp = exposer;
            }

            @Override
            public void handleResult(NewExposer exposer) {
                resultantNewExp = exposer;
            }
        };
    }

    public void testExposedNew() {
        ExposedMethodFinder visitor = makeFinder(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC,
                                                 NewExposer.NEW_DESCRIPTOR);
        visitor.visitAnnotation(Type.getType(ExposedNew.class).getDescriptor(), true);
        visitor.visitEnd();
        assertNotNull(resultantNewExp);
        assertNull(resultantMethExp);
    }

    public void testSimpleExposedMethod() {
        ExposedMethodFinder visitor = makeFinder(Opcodes.ACC_PRIVATE, "()V");
        visitor.visitAnnotation(Type.getType(ExposedMethod.class).getDescriptor(), true);
        visitor.visitEnd();
        assertNull(resultantNewExp);
        assertNotNull(resultantMethExp);
        assertEquals(1, resultantMethExp.getNames().length);
        assertEquals("simpleMethod", resultantMethExp.getNames()[0]);
    }

    public void testNamesExposedMethod() {
        ExposedMethodFinder visitor = makeFinder(Opcodes.ACC_PRIVATE, "()V");
        AnnotationVisitor methVisitor = visitor.visitAnnotation(Type.getType(ExposedMethod.class)
                .getDescriptor(), true);
        AnnotationVisitor arrayVisitor = methVisitor.visitArray("names");
        arrayVisitor.visit("Should not matter", "first");
        arrayVisitor.visit("can be different", "second");
        arrayVisitor.visitEnd();
        methVisitor.visitEnd();
        visitor.visitEnd();
        assertEquals(0, resultantMethExp.getDefaults().length);
        assertEquals(2, resultantMethExp.getNames().length);
        assertEquals("first", resultantMethExp.getNames()[0]);
        assertEquals("second", resultantMethExp.getNames()[1]);
    }

    public void testDefaultsExposedMethod() {
        ExposedMethodFinder visitor = makeFinder(Opcodes.ACC_PRIVATE, "()V");
        AnnotationVisitor methVisitor = visitor.visitAnnotation(Type.getType(ExposedMethod.class)
                .getDescriptor(), true);
        AnnotationVisitor arrayVisitor = methVisitor.visitArray("defaults");
        arrayVisitor.visit("Should not matter", "Py.None");
        arrayVisitor.visitEnd();
        methVisitor.visitEnd();
        visitor.visitEnd();
        assertEquals(1, resultantMethExp.getDefaults().length);
        assertEquals("Py.None", resultantMethExp.getDefaults()[0]);
    }

    private MethodExposer resultantMethExp;

    private NewExposer resultantNewExp;
}
