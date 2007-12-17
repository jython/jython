package org.python.expose.generate;

import org.objectweb.asm.Label;
import org.objectweb.asm.Type;
import org.python.core.PyOverridableNew;
import org.python.core.PyObject;

public class OverridableNewExposer extends Exposer {

    public OverridableNewExposer(Type onType,
                          Type subtype,
                          int access,
                          String methodName,
                          String descriptor,
                          String[] exceptions) {
        super(PyOverridableNew.class, onType.getClassName() + "$exposed___new__");
        this.onType = onType;
        this.subtype = subtype;
        this.name = methodName;
    }

    @Override
    protected void generate() {
        generateConstructor();
        generateOfType();
        generateOfSubtype();
    }

    private void generateConstructor() {
        startConstructor();
        mv.visitVarInsn(ALOAD, 0);
        superConstructor();
        endConstructor();
    }

    private void generateOfType() {
        startMethod("createOfType", PYOBJ, BOOLEAN, APYOBJ, ASTRING);
        instantiate(onType, new Instantiator(PYTYPE){
            public void pushArgs(){
                get("for_type", PYTYPE);
            }
        });
        mv.visitVarInsn(ASTORE, 4);
        Label regularReturn = new Label();
        mv.visitVarInsn(ILOAD, 1);
        mv.visitJumpInsn(IFEQ, regularReturn);
        mv.visitVarInsn(ALOAD, 4);
        mv.visitVarInsn(ALOAD, 2);
        mv.visitVarInsn(ALOAD, 3);
        mv.visitMethodInsn(INVOKEVIRTUAL, onType.getInternalName(), name, methodDesc(VOID,
                                                                                     APYOBJ,
                                                                                     ASTRING));
        mv.visitLabel(regularReturn);
        mv.visitVarInsn(ALOAD, 4);
        mv.visitInsn(ARETURN);
        endMethod();
    }
    
    private void generateOfSubtype() {
        startMethod("createOfSubtype", PYOBJ, PYTYPE);
        instantiate(subtype, new Instantiator(PYTYPE){
            public void pushArgs(){
                mv.visitVarInsn(ALOAD, 1);
            }
        });
        mv.visitInsn(ARETURN);
        endMethod();
    }
    
    private Type onType, subtype;
    
    private String name;

}
