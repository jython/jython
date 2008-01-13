package org.python.expose.generate;

import org.objectweb.asm.Type;
import org.python.core.PyDataDescr;

/**
 * Generates a class to expose a descriptor on Python type. One of addMethodGetter or addFieldGetter
 * must be called, and possibly one of addMethodSetter and addFieldSetter if this is a settable
 * descriptor. If this is a deletable descriptor, addMethodDeleter may be called. There is no
 * addFieldDeleter since there's no defined behavior to 'delete' a field.
 */
public class DescriptorExposer extends Exposer {

    private Type onType, ofType;

    private String name;

    private String getterMethodName, getterFieldName, setterMethodName, setterFieldName,
            deleterMethodName;

    /**
     * Creates an exposer that will work on type and have <code>descrName</code> as its name in
     * the type's dict.
     */
    public DescriptorExposer(Type onType, String descrName) {
        super(PyDataDescr.class, onType.getClassName() + "$" + descrName + "_descriptor");
        this.onType = onType;
        name = descrName;
    }

    /**
     * @return - the name this descriptor will be exposed as in its type's dict
     */
    public String getName() {
        return name;
    }

    public void addMethodGetter(String methodName, String desc) {
        if(hasGetter()) {
            error("Descriptor can only have one getter");
        }
        if(Type.getArgumentTypes(desc).length > 0) {
            error("Getter can't take arguments");
        }
        setOfType(Type.getReturnType(desc));
        getterMethodName = methodName;
    }

    public void addFieldGetter(String fieldName, Type fieldType) {
        if(hasGetter()) {
            error("Can only have one getter for a descriptor");
        }
        setOfType(fieldType);
        getterFieldName = fieldName;
    }

    public boolean hasGetter() {
        return getterMethodName != null || getterFieldName != null;
    }

    private void setOfType(Type type) {
        if(ofType == null) {
            ofType = type;
        } else if(!ofType.equals(type)) {
            error("Types of the getter and setter must agree");
        }
    }

    public void addMethodSetter(String methodName, String desc) {
        if(hasSetter()) {
            error("Descriptor can only have one setter");
        }
        Type[] args = Type.getArgumentTypes(desc);
        if(args.length > 1) {
            error("Setter can only take one argument");
        }
        setOfType(args[0]);
        setterMethodName = methodName;
    }

    public void addFieldSetter(String fieldName, Type fieldType) {
        if(hasSetter()) {
            error("Descriptor can only have one setter");
        }
        setOfType(fieldType);
        setterFieldName = fieldName;
    }

    public boolean hasSetter() {
        return setterMethodName != null || setterFieldName != null;
    }

    public void addMethodDeleter(String methodName, String desc) {
        deleterMethodName = methodName;
    }

    public String toString() {
        return "DescriptorExposer[class=" + onType.getClassName() + ", name=" + name + "]";
    }

    @Override
    protected void generate() {
        if(!hasGetter()) {
            error("A descriptor requires at least a get");
        }
        generateConstructor();
        if(getterMethodName != null) {
            generateMethodGetter();
        } else {
            generateFieldGetter();
        }
        if(setterMethodName != null) {
            generateMethodSetter();
        } else if(setterFieldName != null) {
            generateFieldSetter();
        } else {
            generateDoesntImplement("Set");
        }
        if(deleterMethodName != null) {
            generateMethodDeleter();
        } else {
            generateDoesntImplement("Delete");
        }
    }

    private void generateConstructor() {
        startConstructor();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitLdcInsn(onType);
        mv.visitLdcInsn(name);
        if(PRIMITIVES.containsKey(ofType)) {
            mv.visitLdcInsn(PRIMITIVES.get(ofType));
        } else {
            mv.visitLdcInsn(ofType);
        }
        superConstructor(CLASS, STRING, CLASS);
        endConstructor();
    }

    private void generateMethodGetter() {
        startMethod("invokeGet", OBJECT, PYOBJ);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitTypeInsn(CHECKCAST, onType.getInternalName());
        call(onType, getterMethodName, ofType);
        if(PRIMITIVES.containsKey(ofType)) {
            toPy(ofType);
        }
        endMethod(ARETURN);
    }

    private void generateFieldGetter() {
        startMethod("invokeGet", OBJECT, PYOBJ);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitTypeInsn(CHECKCAST, onType.getInternalName());
        mv.visitFieldInsn(GETFIELD,
                          onType.getInternalName(),
                          getterFieldName,
                          ofType.getDescriptor());
        if(PRIMITIVES.containsKey(ofType)) {
            toPy(ofType);
        }
        endMethod(ARETURN);
    }

    private void generateMethodSetter() {
        startMethod("invokeSet", VOID, PYOBJ, OBJECT);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitTypeInsn(CHECKCAST, onType.getInternalName());
        mv.visitVarInsn(ALOAD, 2);
        if(PRIMITIVES.containsKey(ofType)) {
            mv.visitTypeInsn(CHECKCAST, PRIMITIVES.get(ofType).getInternalName());
            call(PRIMITIVES.get(ofType), ofType.getClassName() + "Value", ofType);
        } else {
            mv.visitTypeInsn(CHECKCAST, ofType.getInternalName());
        }
        call(onType, setterMethodName, VOID, ofType);
        endMethod(RETURN);
    }

    private void generateFieldSetter() {
        startMethod("invokeSet", VOID, PYOBJ, OBJECT);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitTypeInsn(CHECKCAST, onType.getInternalName());
        mv.visitVarInsn(ALOAD, 2);
        if(PRIMITIVES.containsKey(ofType)) {
            mv.visitTypeInsn(CHECKCAST, PRIMITIVES.get(ofType).getInternalName());
            call(PRIMITIVES.get(ofType), ofType.getClassName() + "Value", ofType);
        } else {
            mv.visitTypeInsn(CHECKCAST, ofType.getInternalName());
        }
        mv.visitFieldInsn(PUTFIELD,
                          onType.getInternalName(),
                          setterFieldName,
                          ofType.getDescriptor());
        endMethod(RETURN);
    }

    private void generateMethodDeleter() {
        startMethod("invokeDelete", VOID, PYOBJ);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitTypeInsn(CHECKCAST, onType.getInternalName());
        call(onType, deleterMethodName, VOID);
        endMethod(RETURN);
    }

    private void generateDoesntImplement(String setOrDelete) {
        startMethod("implementsDescr" + setOrDelete, BOOLEAN);
        mv.visitInsn(ICONST_0);
        endMethod(IRETURN);
    }

    private void error(String reason) {
        throw new InvalidExposingException(reason + "[class=" + onType.getClassName() + ", name="
                + name + "]");
    }
}
