package org.python.expose.generate;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Opcodes;

public abstract class ExposedFieldFinder extends FieldVisitor implements PyTypes {

    private String fieldName;

    private FieldVisitor delegate;

    private String doc;

    public ExposedFieldFinder(String name, FieldVisitor delegate) {
        super(Opcodes.ASM4);
        fieldName = name;
        this.delegate = delegate;
    }

    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        if(EXPOSED_GET.getDescriptor().equals(desc)) {
            return new DescriptorVisitor(fieldName) {

                @Override
                public void handleResult(String name, String doc) {
                    exposeAsGet(name, doc);
                }
            };
        } else if(EXPOSED_SET.getDescriptor().equals(desc)) {
            return new DescriptorVisitor(fieldName) {

                @Override
                public void handleResult(String name, String doc) {
                    exposeAsSet(name);
                }
            };
        } else {
            return delegate.visitAnnotation(desc, visible);
        }
    }

    public abstract void exposeAsGet(String name, String doc);

    public abstract void exposeAsSet(String name);

    public void visitAttribute(Attribute attr) {
        delegate.visitAttribute(attr);
    }

    public void visitEnd() {
        delegate.visitEnd();
    }
}
