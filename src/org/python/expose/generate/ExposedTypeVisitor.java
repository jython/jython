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

    private String doc;

    private final AnnotationVisitor passthrough;

    public ExposedTypeVisitor(Type onType, AnnotationVisitor passthrough) {
        this.onType = onType;
        this.passthrough = passthrough;
    }

    @Override
    public void visit(String name, Object value) {
        if (name.equals("name")) {
            typeName = (String)value;
        } else if (name.equals("base")) {
            base = (Type)value;
        } else if (name.equals("isBaseType")) {
            isBaseType = (Boolean)value;
        } else if (name.equals("doc")) {
            doc = (String)value;
        } else {
            super.visit(name, value);
        }
        if (passthrough != null) {
            passthrough.visit(name, value);
        }
    }

    @Override
    public void visitEnd() {
        if (typeName == null) {
            String name = onType.getClassName();
            typeName = name.substring(name.lastIndexOf(".") + 1);
        }
        handleResult(typeName, base, isBaseType, doc);
        if (passthrough != null) {
            passthrough.visitEnd();
        }
    }

    /**
     * @param name the name the type should be exposed as from the annotation
     * @param name the specified base type
     * @param name the value of the isBaseType flag
     * @param name the type's docstring
     */
    public abstract void handleResult(String name, Type base, boolean isBaseType, String doc);
}
