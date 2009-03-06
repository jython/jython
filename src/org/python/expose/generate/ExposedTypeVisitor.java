package org.python.expose.generate;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Type;

/**
 * Visits an ExposedType annotation and passes the values it gathers to handleResult.
 */
public abstract class ExposedTypeVisitor extends RestrictiveAnnotationVisitor {

    private String typeName;

    private Type onType;

    private Type base = Type.getType(Object.class);

    private boolean isBaseType = true;

    private final AnnotationVisitor passthrough;

    public ExposedTypeVisitor(Type onType, AnnotationVisitor passthrough) {
        this.onType = onType;
        this.passthrough = passthrough;
    }

    @Override
    public void visit(String name, Object value) {
        if(name.equals("name")) {
            typeName = (String)value;
        } else if(name.equals("base")) {
            base = (Type)value;
        } else if(name.equals("isBaseType")) {
            isBaseType = (Boolean)value;
        } else {
            super.visit(name, value);
        }
        if (passthrough != null) {
            passthrough.visit(name, value);
        }
    }

    @Override
    public void visitEnd() {
        if(typeName == null) {
            String name = onType.getClassName();
            typeName = name.substring(name.lastIndexOf(".") + 1);
        }
        handleResult(typeName);
        handleResult(base);
        handleResult(isBaseType);
        if (passthrough != null) {
            passthrough.visitEnd();
        }
    }

    public abstract void handleResult(Type base);

    public abstract void handleResult(boolean isBaseType);

    /**
     * @param name -
     *            the name the type should be exposed as from the annotation.
     */
    public abstract void handleResult(String name);
}
