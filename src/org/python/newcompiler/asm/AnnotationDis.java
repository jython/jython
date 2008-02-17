package org.python.newcompiler.asm;

import org.python.objectweb.asm.AnnotationVisitor;
import org.python.newcompiler.DisassemblyDocument;

class AnnotationDis implements AnnotationVisitor {

    private AnnotationVisitor next;
    private DisassemblyDocument debugger;

    AnnotationDis(AnnotationVisitor next, DisassemblyDocument debugger) {
        this.next = next;
        this.debugger = debugger;
    }

    public void visit(String name, Object value) {
        next.visit(name, value);
        debugger.put(name + " = " + value);
    }

    public AnnotationVisitor visitAnnotation(String name, String desc) {
        AnnotationVisitor visitor = next.visitAnnotation(name, desc);
        DisassemblyDocument subAnnotation = debugger.newSubSection();
        subAnnotation.putTitle(name + " "
                + ClassDis.formatAnnotation(desc, true));
        return new AnnotationDis(visitor, subAnnotation);
    }

    public AnnotationVisitor visitArray(String name) {
        AnnotationVisitor visitor = next.visitArray(name);
        debugger.put(name + " is an array");
        if (visitor != null) {
            return new AnnotationDis(visitor, debugger);
        } else {
            return null;
        }
    }

    public void visitEnd() {
        next.visitEnd();
        // Do something here?
    }

    public void visitEnum(String name, String desc, String value) {
        next.visitEnum(name, desc, value);
        debugger.put(desc + " " + name + " = " + value);
    }

}
