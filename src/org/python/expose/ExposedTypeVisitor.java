package org.python.expose;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Type;

/**
 * Visits an ExposedType annotation and passes the values it gathers to
 * handleResult.
 */
public abstract class ExposedTypeVisitor extends RestrictiveAnnotationVisitor {

    public ExposedTypeVisitor(Type onType) {
        this.onType = onType;
    }

    @Override
    public void visit(String name, Object value) {
        if(name.equals("name")) {
            typeName = (String)value;
        } else {
            super.visit(name, value);
        }
    }

    @Override
    public void visitEnd() {
        if(typeName == null) {
            String name = onType.getClassName();
            typeName = name.substring(name.lastIndexOf(".") + 1);
        }
        handleResult(typeName);
    }

    /**
     * @param name -
     *            the name the type should be exposed as from the annotation.
     */
    public abstract void handleResult(String name);

    private String typeName;

    private Type onType;
}
