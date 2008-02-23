package org.python.expose.generate;

import org.python.objectweb.asm.Type;

/**
 * Visits an ExposedType annotation and passes the values it gathers to handleResult.
 */
public abstract class ExposedTypeVisitor extends RestrictiveAnnotationVisitor {

    private String typeName;

    private Type onType;

    private Type base = Type.getType(Object.class);

    public ExposedTypeVisitor(Type onType) {
        this.onType = onType;
    }

    @Override
    public void visit(String name, Object value) {
        if(name.equals("name")) {
            typeName = (String)value;
        } else if(name.equals("base")) {
            base = (Type)value;
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
        handleResult(base);
    }

    public abstract void handleResult(Type base);

    /**
     * @param name -
     *            the name the type should be exposed as from the annotation.
     */
    public abstract void handleResult(String name);
}
