package org.python.expose.generate;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Opcodes;

/**
 * An Annotation visitor that throws an IllegalArgumentException if it visits anything other than
 * visitEnd. Should be subclasses by something interested in only certain events.
 */
public class RestrictiveAnnotationVisitor extends AnnotationVisitor {

    public RestrictiveAnnotationVisitor(int arg0) {
        super(arg0);
    }

    public RestrictiveAnnotationVisitor() {
        this(Opcodes.ASM4);
    }

    public AnnotationVisitor visitAnnotation(String name, String desc) {
        throw new IllegalArgumentException("Unknown annotation field '" + name + "'");
    }

    public void visitEnd() {}

    public AnnotationVisitor visitArray(String name) {
        throw new IllegalArgumentException("Unknown annotation field '" + name + "'");
    }

    public void visitEnum(String name, String desc, String value) {
        throw new IllegalArgumentException("Unknown annotation field '" + name + "'");
    }

    public void visit(String name, Object value) {
        throw new IllegalArgumentException("Unknown annotation field '" + name + "'");
    }
}
