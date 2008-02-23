package org.python.expose.generate;

import org.python.objectweb.asm.AnnotationVisitor;

/**
 * An Annotation visitor that throws an IllegalArgumentException if it visits anything other than
 * visitEnd. Should be subclasses by something interested in only certain events.
 */
public class RestrictiveAnnotationVisitor implements AnnotationVisitor {

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
