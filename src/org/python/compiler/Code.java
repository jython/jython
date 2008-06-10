package org.python.compiler;

import java.util.BitSet;
import java.util.Vector;

import org.python.objectweb.asm.AnnotationVisitor;
import org.python.objectweb.asm.Attribute;
import org.python.objectweb.asm.Label;
import org.python.objectweb.asm.MethodVisitor;
import org.python.objectweb.asm.Opcodes;

class Code implements MethodVisitor, Opcodes {
    MethodVisitor mv;
    String sig;
    String locals[];
    int nlocals;
    int argcount;
    int returnLocal;
    BitSet finallyLocals = new java.util.BitSet();
    
    //XXX: I'd really like to get sig and access out of here since MethodVistitor
    //     should already have this information.
    public Code(MethodVisitor mv, String sig, int access) {
	this.mv = mv;
	this.sig = sig;
	nlocals = -sigSize(sig, false);
	if ((access & ACC_STATIC) != ACC_STATIC) nlocals = nlocals+1;
	argcount = nlocals;
	locals = new String[nlocals+128];
    }
    
    public int getLocal(String type) {
        //Could optimize this to skip arguments?
        for(int l = argcount; l<nlocals; l++) {
            if (locals[l] == null) {
                locals[l] = type;
                return l;
            }
        }
        if (nlocals >= locals.length) {
            String[] new_locals = new String[locals.length*2];
            System.arraycopy(locals, 0, new_locals, 0, locals.length);
            locals = new_locals;
        }
        locals[nlocals] = type;
        nlocals += 1;
        return nlocals-1;
    }

    public void freeLocal(int l) {
        if (locals[l] == null) {
            System.out.println("Double free:" + l);
        }
        locals[l] = null;
    }


    public int getFinallyLocal(String type) {
        int l = getLocal(type);
        finallyLocals.set(l);
        return l;
    }

    public void freeFinallyLocal(int l) {
        finallyLocals.clear(l);
        freeLocal(l);
    }

    public int getReturnLocal() {
        if (returnLocal == 0)
            returnLocal = getLocal("return");
        return returnLocal;
    }

    public Vector getActiveLocals() {
        Vector ret = new Vector();
        ret.setSize(nlocals);
        for (int l = argcount; l<nlocals; l++) {
            if (l == returnLocal || finallyLocals.get(l))
                continue;
            ret.setElementAt(locals[l], l);
        }
        return ret;
    }

    public AnnotationVisitor visitAnnotation(String arg0, boolean arg1) {
        return mv.visitAnnotation(arg0, arg1);
    }

    public AnnotationVisitor visitAnnotationDefault() {
        return mv.visitAnnotationDefault();
    }

    public void visitAttribute(Attribute arg0) {
        mv.visitAttribute(arg0);
    }

    public void visitCode() {
        mv.visitCode();
    }

    public void visitEnd() {
        mv.visitEnd();
    }

    public void visitFieldInsn(int arg0, String arg1, String arg2, String arg3) {
        mv.visitFieldInsn(arg0, arg1, arg2, arg3);
    }

    public void visitFrame(int arg0, int arg1, Object[] arg2, int arg3, Object[] arg4) {
        mv.visitFrame(arg0, arg1, arg2, arg3, arg4);
    }

    public void visitIincInsn(int arg0, int arg1) {
        mv.visitIincInsn(arg0, arg1);
    }

    public void visitInsn(int arg0) {
        mv.visitInsn(arg0);
    }

    public void visitIntInsn(int arg0, int arg1) {
        mv.visitIntInsn(arg0, arg1);
    }

    public void visitJumpInsn(int arg0, Label arg1) {
        mv.visitJumpInsn(arg0, arg1);
    }

    public void visitLabel(Label arg0) {
        mv.visitLabel(arg0);
    }

    public void visitLdcInsn(Object arg0) {
        mv.visitLdcInsn(arg0);
    }

    public void visitLineNumber(int arg0, Label arg1) {
        mv.visitLineNumber(arg0, arg1);
    }

    public void visitLocalVariable(String arg0, String arg1, String arg2, Label arg3, Label arg4, int arg5) {
        mv.visitLocalVariable(arg0, arg1, arg2, arg3, arg4, arg5);
    }

    public void visitLookupSwitchInsn(Label arg0, int[] arg1, Label[] arg2) {
        mv.visitLookupSwitchInsn(arg0, arg1, arg2);
    }

    public void visitMaxs(int arg0, int arg1) {
        mv.visitMaxs(arg0, arg1);
    }

    public void visitMethodInsn(int arg0, String arg1, String arg2, String arg3) {
        mv.visitMethodInsn(arg0, arg1, arg2, arg3);
    }

    public void visitMultiANewArrayInsn(String arg0, int arg1) {
        mv.visitMultiANewArrayInsn(arg0, arg1);
    }

    public AnnotationVisitor visitParameterAnnotation(int arg0, String arg1, boolean arg2) {
        return mv.visitParameterAnnotation(arg0, arg1, arg2);
    }

    public void visitTableSwitchInsn(int arg0, int arg1, Label arg2, Label[] arg3) {
        mv.visitTableSwitchInsn(arg0, arg1, arg2, arg3);
    }

    public void visitTryCatchBlock(Label arg0, Label arg1, Label arg2, String arg3) {
        mv.visitTryCatchBlock(arg0, arg1, arg2, arg3);
    }

    public void visitTypeInsn(int arg0, String arg1) {
        mv.visitTypeInsn(arg0, arg1);
    }

    public void visitVarInsn(int arg0, int arg1) {
        mv.visitVarInsn(arg0, arg1);
    }

    private int sigSize(String sig, boolean includeReturn) {
        int stack = 0;
        int i = 0;
        char[] c = sig.toCharArray();
        int n = c.length;
        boolean ret=false;
        boolean array=false;

        while (++i<n) {
            switch (c[i]) {
            case ')':
                if (!includeReturn)
                    return stack;
                ret=true;
                continue;
            case '[':
                array=true;
                continue;
            case 'V':
                continue;
            case 'D':
            case 'J':
                if (array) {
                    if (ret) stack += 1;
                    else stack -=1;
                    array = false;
                } else {
                    if (ret) stack += 2;
                    else stack -=2;
                }
                break;
            case 'L':
                while (c[++i] != ';') {;}
            default:
                if (ret) stack++;
                else stack--;
                array = false;
            }
        }
        return stack;
    }

    public void aaload() {
        mv.visitInsn(AALOAD);
    }
    
    public void aastore() {
        mv.visitInsn(AASTORE);
    }

    public void aconst_null() {
        mv.visitInsn(ACONST_NULL);
    }

    public void aload(int index) {
        mv.visitVarInsn(ALOAD, index);
    }

    public void anewarray(String index) {
        mv.visitTypeInsn(ANEWARRAY, index);
    }

    public void areturn() {
        mv.visitInsn(ARETURN);
    }

    public void arraylength() {
        mv.visitInsn(ARRAYLENGTH);
    }

    public void astore(int index) {
        mv.visitVarInsn(ASTORE, index);
    }

    public void athrow() {
        mv.visitInsn(ATHROW);
    }

    public void baload() {
        mv.visitInsn(BALOAD);
    }

    public void bastore() {
        mv.visitInsn(BASTORE);
    }

    public void bipush(int value) {
        mv.visitIntInsn(BIPUSH, value);
    }

    public void checkcast(String type) {
        mv.visitTypeInsn(CHECKCAST, type);
    }

    public void dconst_0() {
        mv.visitInsn(DCONST_0);
    }

    public void dload(int index) {
        mv.visitVarInsn(DLOAD, index);
    }

    public void dreturn() {
        mv.visitInsn(DRETURN);
    }

    public void dup() {
        mv.visitInsn(DUP);
    }

    public void dup2() {
        mv.visitInsn(DUP2);
    }
 
    public void dup_x1() {
        mv.visitInsn(DUP_X1);
    }

    public void dup_x2() {
        mv.visitInsn(DUP_X2);
    }

    public void dup2_x1() {
        mv.visitInsn(DUP2_X1);
    }

    public void dup2_x2() {
        mv.visitInsn(DUP2_X2);
    }

    public void fconst_0() {
        mv.visitInsn(FCONST_0);
    }
 
    public void fload(int index) {
        mv.visitVarInsn(FLOAD, index);
    }

    public void freturn() {
        mv.visitInsn(FRETURN);
    }

    public void getfield(String owner, String name, String type) {
        mv.visitFieldInsn(GETFIELD, owner, name, type);
    }

    public void getstatic(String owner, String name, String type) {
        mv.visitFieldInsn(GETSTATIC, owner, name, type);
    }

    public void goto_(Label label) {
        mv.visitJumpInsn(GOTO, label);
    }
  
    public void iconst(int value) {
        if (value <= Byte.MAX_VALUE && value >= Byte.MIN_VALUE) {
            switch (value) {
            case -1:
                iconst_m1();
                break;
            case 0:
                iconst_0();
                break;
            case 1:
                iconst_1();
                break;
            case 2:
                iconst_2();
                break;
            case 3:
                iconst_3();
                break;
            case 4:
                iconst_4();
                break;
            case 5:
                iconst_5();
                break;
            default:
                bipush(value);
                break;
            }
        } else if (value <= Short.MAX_VALUE && value >= Short.MIN_VALUE) {
            sipush(value);
        } else {
            ldc(value);
        }
    }

    public void iconst_m1() {
        mv.visitInsn(ICONST_M1);
    }
    
    public void iconst_0() {
        mv.visitInsn(ICONST_0);
    }
    
    public void iconst_1() {
        mv.visitInsn(ICONST_1);
    }
    
    public void iconst_2() {
        mv.visitInsn(ICONST_2);
    }
    
    public void iconst_3() {
        mv.visitInsn(ICONST_3);
    }
    
    public void iconst_4() {
        mv.visitInsn(ICONST_4);
    }
    
    public void iconst_5() {
        mv.visitInsn(ICONST_5);
    }
    
    public void ifeq(Label label) {
        mv.visitJumpInsn(IFEQ, label);
    }

    public void ifle(Label label) {
        mv.visitJumpInsn(IFLE, label);
    }
     
    public void ifne(Label label) {
        mv.visitJumpInsn(IFNE, label);
    }

    public void ifnull(Label label) {
        mv.visitJumpInsn(IFNULL, label);
    }

    public void ifnonnull(Label label) {
        mv.visitJumpInsn(IFNONNULL, label);
    }
     
    public void if_acmpne(Label label) {
        mv.visitJumpInsn(IF_ACMPNE, label);
    }
    
    public void if_acmpeq(Label label) {
        mv.visitJumpInsn(IF_ACMPEQ, label);
    }
    
    public void if_icmple(Label label) {
        mv.visitJumpInsn(IF_ICMPLE, label);
    }
    
    public void if_icmpgt(Label label) {
        mv.visitJumpInsn(IF_ICMPGT, label);
    }
    
    public void if_icmplt(Label label) {
        mv.visitJumpInsn(IF_ICMPLT, label);
    }
    
    public void if_icmpne(Label label) {
        mv.visitJumpInsn(IF_ICMPNE, label);
    }
    
    public void if_icmpeq(Label label) {
        mv.visitJumpInsn(IF_ICMPEQ, label);
    }

    public void iadd() {
        mv.visitInsn(IADD);
    }

    public void iaload() {
        mv.visitInsn(IALOAD);
    }

    /* XXX: different API from old iinc */
    public void iinc() {
        mv.visitInsn(IINC);
    }
  
    /* XXX: old API for iinc
    public void iinc(int i, int increment) throws IOException {
        code.writeByte(132);
        code.writeByte(i);
        code.writeByte(increment);
    }

    public void iinc(int i) throws IOException {
        iinc(i, 1);
    }
    */

    public void iload(int index) {
        mv.visitVarInsn(ILOAD, index);
    }

    public void instanceof_(String type) {
        mv.visitTypeInsn(INSTANCEOF, type);
    }

    public void invokeinterface(String owner, String name, String type) {
        mv.visitMethodInsn(INVOKEINTERFACE, owner, name, type);
    }

    public void invokespecial(String owner, String name, String type) {
        mv.visitMethodInsn(INVOKESPECIAL, owner, name, type);
    }

    public void invokestatic(String owner, String name, String type) {
        mv.visitMethodInsn(INVOKESTATIC, owner, name, type);
    }
    
    public void invokevirtual(String owner, String name, String type) {
        mv.visitMethodInsn(INVOKEVIRTUAL, owner, name, type);
    }
    
    public void ireturn() {
        mv.visitInsn(IRETURN);
    }
 
    public void istore(int index) {
        mv.visitVarInsn(ISTORE, index);
    }

    public void isub() {
        mv.visitInsn(ISUB);
    }

    /* XXX: needed?
    public void jsr(Label label) throws IOException {
        //push(-1);
        int offset = size();
        code.writeByte(168);
        label.setBranch(offset, 2);
        label.setStack(stack+1);
    }
    */

    public void label(Label label) {
        mv.visitLabel(label);
    }

    public void lconst_0() {
        mv.visitInsn(LCONST_0);
    }

    public void ldc(Object cst) {
        mv.visitLdcInsn(cst);
    }

    public void lload(int index) {
        mv.visitVarInsn(LLOAD, index);
    }

    public void lreturn() {
        mv.visitInsn(LRETURN);
    }

    public void newarray(int atype) {
        mv.visitIntInsn(NEWARRAY, atype);
    }

    public void new_(String type) {
        mv.visitTypeInsn(NEW, type);
    }

    public void nop() {
        mv.visitInsn(NOP);
    }

    public void pop() {
        mv.visitInsn(POP);
    }
    
    public void pop2() {
        mv.visitInsn(POP2);
    }

    public void putstatic(String owner, String name, String type) {
        mv.visitFieldInsn(PUTSTATIC, owner, name, type);
    }
    
    public void putfield(String owner, String name, String type) {
        mv.visitFieldInsn(PUTFIELD, owner, name, type);
    }
 
    public void ret(int index) {
        mv.visitVarInsn(RET, index);
    }

    void return_() {
        mv.visitInsn(RETURN);
    }

    public void sipush(int value) {
        mv.visitIntInsn(SIPUSH, value);
    }

    public void swap() {
        mv.visitInsn(SWAP);
    }
 
    public void swap2() {
        dup2_x2();
        pop2();
    }

    public void tableswitch(int arg0, int arg1, Label arg2, Label[] arg3) {
        mv.visitTableSwitchInsn(arg0, arg1, arg2, arg3);
    }

    public void trycatch(Label start, Label end, Label handlerStart, String type) {
        mv.visitTryCatchBlock(start, end, handlerStart, type);
    }
    
    public void setline(int line) {
        mv.visitLineNumber(line, new Label());
    }
}
