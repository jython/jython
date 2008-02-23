package org.python.expose.generate;

import org.python.objectweb.asm.AnnotationVisitor;
import org.python.objectweb.asm.Attribute;
import org.python.objectweb.asm.FieldVisitor;

public abstract class ExposedFieldFinder implements FieldVisitor, PyTypes {

    private String fieldName;

    private FieldVisitor delegate;

    public ExposedFieldFinder(String name, FieldVisitor delegate) {
        fieldName = name;
        this.delegate = delegate;
    }

    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        if(EXPOSED_GET.getDescriptor().equals(desc)) {
            return new DescriptorVisitor(fieldName) {

                @Override
                public void handleResult(String name) {
                    exposeAsGet(name);
                }
            };
        } else if(EXPOSED_SET.getDescriptor().equals(desc)) {
            return new DescriptorVisitor(fieldName) {

                @Override
                public void handleResult(String name) {
                    exposeAsSet(name);
                }
            };
        } else {
            return delegate.visitAnnotation(desc, visible);
        }
    }

    public abstract void exposeAsGet(String name);

    public abstract void exposeAsSet(String name);

    public void visitAttribute(Attribute attr) {
        delegate.visitAttribute(attr);
    }

    public void visitEnd() {
        delegate.visitEnd();
    }
}
