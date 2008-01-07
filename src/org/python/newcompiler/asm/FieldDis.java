package org.python.newcompiler.asm;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.FieldVisitor;
import org.python.newcompiler.DisassemblyDocument;

class FieldDis implements FieldVisitor {

    private FieldVisitor next;
    private DisassemblyDocument debugger;

    FieldDis(FieldVisitor next, DisassemblyDocument debugger) {
        this.next = next;
        this.debugger = debugger;
    }

    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        AnnotationVisitor visitor = next.visitAnnotation(desc, visible);
        DisassemblyDocument preTitle = debugger.newPreTitle();
        preTitle.putTitle(ClassDis.formatAnnotation(desc, visible));
        if (visitor != null) {
            return new AnnotationDis(visitor, preTitle);
        } else {
            return null;
        }
    }

    public void visitAttribute(Attribute attr) {
        next.visitAttribute(attr);
        debugger.putTitle(ClassDis.formatAttribute(attr));
    }

    public void visitEnd() {
        next.visitEnd();
        // TODO Something here?
    }

}
