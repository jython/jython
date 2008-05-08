package org.python.compiler;

import java.util.BitSet;
import java.util.Vector;

import org.python.objectweb.asm.AnnotationVisitor;
import org.python.objectweb.asm.Attribute;
import org.python.objectweb.asm.Label;
import org.python.objectweb.asm.MethodVisitor;
import org.python.objectweb.asm.Opcodes;

/**
 * XXX: I am betting that once I understand ASM better the need for this class will
 *      may away.
 */
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
    
    // XXX: So far I haven't found an ASM convenience for pushing constant
    // integers, so for now I'm putting one here.  This shouldn't except for
    // variable i's (so if you know you are pushing a 0 just use ICONST_0
    // directly).
    public void iconst(int i) {
         if (i == -1) {
             mv.visitInsn(ICONST_M1);
         } else if (i == 0) {
             mv.visitInsn(ICONST_0);
         } else if (i == 1) {
             mv.visitInsn(ICONST_1);
         } else if (i == 2) {
             mv.visitInsn(ICONST_2);
         } else if (i == 3) {
             mv.visitInsn(ICONST_3);
         } else if (i == 4) {
             mv.visitInsn(ICONST_4);
         } else if (i == 5) {
             mv.visitInsn(ICONST_5);
         } else if (i > -127 && i < 128) {
             mv.visitIntInsn(BIPUSH, i);
         } else if (i > -32767 && i < 32768) {
             mv.visitIntInsn(SIPUSH, i);
         } else {
             mv.visitLdcInsn(new Integer(i));
         }
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


}
