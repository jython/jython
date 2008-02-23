package org.python.newcompiler.asm;

import java.util.HashMap;
import java.util.Map;

import org.python.objectweb.asm.AnnotationVisitor;
import org.python.objectweb.asm.Attribute;
import org.python.objectweb.asm.Label;
import org.python.objectweb.asm.MethodAdapter;
import org.python.objectweb.asm.MethodVisitor;
import org.python.objectweb.asm.Opcodes;
import org.python.newcompiler.DisassemblyDocument;

class MethodDis extends MethodAdapter {

    private DisassemblyDocument debugger;
    private int insnCounter;

    MethodDis(MethodVisitor next, DisassemblyDocument debugger) {
        super(next);
        this.debugger = debugger;
    }

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        AnnotationVisitor next = super.visitAnnotation(desc, visible);
        DisassemblyDocument preTitle = debugger.newPreTitle();
        preTitle.putTitle(ClassDis.formatAnnotation(desc, visible));
        if (next != null) {
            return new AnnotationDis(next, preTitle);
        } else {
            return null;
        }
    }

    @Override
    public AnnotationVisitor visitAnnotationDefault() {
        AnnotationVisitor next = super.visitAnnotationDefault();
        if (next != null) {
            return new AnnotationDis(next, debugger.newPreTitle());
        } else {
            return null;
        }
    }

    @Override
    public AnnotationVisitor visitParameterAnnotation(int parameter,
            String desc, boolean visible) {
        AnnotationVisitor next = super.visitParameterAnnotation(parameter,
                desc, visible);
        DisassemblyDocument subSection = debugger.newSubSection();
        subSection.putTitle("Local variable " + parameter + " "
                + ClassDis.formatAnnotation(desc, visible));
        if (next != null) {
            return new AnnotationDis(next, subSection);
        } else {
            return null;
        }
    }

    @Override
    public void visitAttribute(Attribute attr) {
        super.visitAttribute(attr);
        debugger.putTitle(ClassDis.formatAttribute(attr));
    }

    @Override
    public void visitCode() {
        super.visitCode();
        // TODO something here?
    }

    @Override
    public void visitEnd() {
        super.visitEnd();
        // TODO something here?
    }

    private static Map<Integer, String> instructionNames = new HashMap<Integer, String>();
    static {
        instructionNames.put(Opcodes.NOP, "NOP");
        instructionNames.put(Opcodes.ACONST_NULL, "ACONST_NULL");
        instructionNames.put(Opcodes.ICONST_M1, "ICONST_M1");
        instructionNames.put(Opcodes.ICONST_0, "ICONST_0");
        instructionNames.put(Opcodes.ICONST_1, "ICONST_1");
        instructionNames.put(Opcodes.ICONST_2, "ICONST_2");
        instructionNames.put(Opcodes.ICONST_3, "ICONST_3");
        instructionNames.put(Opcodes.ICONST_4, "ICONST_4");
        instructionNames.put(Opcodes.ICONST_5, "ICONST_5");
        instructionNames.put(Opcodes.LCONST_0, "LCONST_0");
        instructionNames.put(Opcodes.LCONST_1, "LCONST_1");
        instructionNames.put(Opcodes.FCONST_0, "FCONST_0");
        instructionNames.put(Opcodes.FCONST_1, "FCONST_1");
        instructionNames.put(Opcodes.FCONST_2, "FCONST_2");
        instructionNames.put(Opcodes.DCONST_0, "DCONST_0");
        instructionNames.put(Opcodes.DCONST_1, "DCONST_1");
        instructionNames.put(Opcodes.IALOAD, "IALOAD");
        instructionNames.put(Opcodes.LALOAD, "LALOAD");
        instructionNames.put(Opcodes.FALOAD, "FALOAD");
        instructionNames.put(Opcodes.DALOAD, "DALOAD");
        instructionNames.put(Opcodes.AALOAD, "AALOAD");
        instructionNames.put(Opcodes.BALOAD, "BALOAD");
        instructionNames.put(Opcodes.CALOAD, "CALOAD");
        instructionNames.put(Opcodes.SALOAD, "SALOAD");
        instructionNames.put(Opcodes.IASTORE, "IASTORE");
        instructionNames.put(Opcodes.LASTORE, "LASTORE");
        instructionNames.put(Opcodes.FASTORE, "FASTORE");
        instructionNames.put(Opcodes.DASTORE, "DASTORE");
        instructionNames.put(Opcodes.AASTORE, "AASTORE");
        instructionNames.put(Opcodes.BASTORE, "BASTORE");
        instructionNames.put(Opcodes.CASTORE, "CASTORE");
        instructionNames.put(Opcodes.SASTORE, "SASTORE");
        instructionNames.put(Opcodes.POP, "POP");
        instructionNames.put(Opcodes.POP2, "POP2");
        instructionNames.put(Opcodes.DUP, "DUP");
        instructionNames.put(Opcodes.DUP_X1, "DUP_X1");
        instructionNames.put(Opcodes.DUP_X2, "DUP_X2");
        instructionNames.put(Opcodes.DUP2, "DUP2");
        instructionNames.put(Opcodes.DUP2_X1, "DUP2_X1");
        instructionNames.put(Opcodes.DUP2_X2, "DUP2_X2");
        instructionNames.put(Opcodes.SWAP, "SWAP");
        instructionNames.put(Opcodes.IADD, "IADD");
        instructionNames.put(Opcodes.LADD, "LADD");
        instructionNames.put(Opcodes.FADD, "FADD");
        instructionNames.put(Opcodes.DADD, "DADD");
        instructionNames.put(Opcodes.ISUB, "ISUB");
        instructionNames.put(Opcodes.LSUB, "LSUB");
        instructionNames.put(Opcodes.FSUB, "FSUB");
        instructionNames.put(Opcodes.DSUB, "DSUB");
        instructionNames.put(Opcodes.IMUL, "IMUL");
        instructionNames.put(Opcodes.LMUL, "LMUL");
        instructionNames.put(Opcodes.FMUL, "FMUL");
        instructionNames.put(Opcodes.DMUL, "DMUL");
        instructionNames.put(Opcodes.IDIV, "IDIV");
        instructionNames.put(Opcodes.LDIV, "LDIV");
        instructionNames.put(Opcodes.FDIV, "FDIV");
        instructionNames.put(Opcodes.DDIV, "DDIV");
        instructionNames.put(Opcodes.IREM, "IREM");
        instructionNames.put(Opcodes.LREM, "LREM");
        instructionNames.put(Opcodes.FREM, "FREM");
        instructionNames.put(Opcodes.DREM, "DREM");
        instructionNames.put(Opcodes.INEG, "INEG");
        instructionNames.put(Opcodes.LNEG, "LNEG");
        instructionNames.put(Opcodes.FNEG, "FNEG");
        instructionNames.put(Opcodes.DNEG, "DNEG");
        instructionNames.put(Opcodes.ISHL, "ISHL");
        instructionNames.put(Opcodes.LSHL, "LSHL");
        instructionNames.put(Opcodes.ISHR, "ISHR");
        instructionNames.put(Opcodes.LSHR, "LSHR");
        instructionNames.put(Opcodes.IUSHR, "IUSHR");
        instructionNames.put(Opcodes.LUSHR, "LUSHR");
        instructionNames.put(Opcodes.IAND, "IAND");
        instructionNames.put(Opcodes.LAND, "LAND");
        instructionNames.put(Opcodes.IOR, "IOR");
        instructionNames.put(Opcodes.LOR, "LOR");
        instructionNames.put(Opcodes.IXOR, "IXOR");
        instructionNames.put(Opcodes.LXOR, "LXOR");
        instructionNames.put(Opcodes.I2L, "I2L");
        instructionNames.put(Opcodes.I2F, "I2F");
        instructionNames.put(Opcodes.I2D, "I2D");
        instructionNames.put(Opcodes.L2I, "L2I");
        instructionNames.put(Opcodes.L2F, "L2F");
        instructionNames.put(Opcodes.L2D, "L2D");
        instructionNames.put(Opcodes.F2I, "F2I");
        instructionNames.put(Opcodes.F2L, "F2L");
        instructionNames.put(Opcodes.F2D, "F2D");
        instructionNames.put(Opcodes.D2I, "D2I");
        instructionNames.put(Opcodes.D2L, "D2L");
        instructionNames.put(Opcodes.D2F, "D2F");
        instructionNames.put(Opcodes.I2B, "I2B");
        instructionNames.put(Opcodes.I2C, "I2C");
        instructionNames.put(Opcodes.I2S, "I2S");
        instructionNames.put(Opcodes.LCMP, "LCMP");
        instructionNames.put(Opcodes.FCMPL, "FCMPL");
        instructionNames.put(Opcodes.FCMPG, "FCMPG");
        instructionNames.put(Opcodes.DCMPL, "DCMPL");
        instructionNames.put(Opcodes.DCMPG, "DCMPG");
        instructionNames.put(Opcodes.IRETURN, "IRETURN");
        instructionNames.put(Opcodes.LRETURN, "LRETURN");
        instructionNames.put(Opcodes.FRETURN, "FRETURN");
        instructionNames.put(Opcodes.DRETURN, "DRETURN");
        instructionNames.put(Opcodes.ARETURN, "ARETURN");
        instructionNames.put(Opcodes.RETURN, "RETURN");
        instructionNames.put(Opcodes.ARRAYLENGTH, "ARRAYLENGTH");
        instructionNames.put(Opcodes.ATHROW, "ATHROW");
        instructionNames.put(Opcodes.MONITORENTER, "MONITORENTER");
        instructionNames.put(Opcodes.MONITOREXIT, "MONITOREXIT");
        instructionNames.put(Opcodes.BIPUSH, "BIPUSH");
        instructionNames.put(Opcodes.SIPUSH, "SIPUSH");
        instructionNames.put(Opcodes.NEWARRAY, "NEWARRAY");
        instructionNames.put(Opcodes.ILOAD, "ILOAD");
        instructionNames.put(Opcodes.LLOAD, "LLOAD");
        instructionNames.put(Opcodes.FLOAD, "FLOAD");
        instructionNames.put(Opcodes.DLOAD, "DLOAD");
        instructionNames.put(Opcodes.ALOAD, "ALOAD");
        instructionNames.put(Opcodes.ISTORE, "ISTORE");
        instructionNames.put(Opcodes.LSTORE, "LSTORE");
        instructionNames.put(Opcodes.FSTORE, "FSTORE");
        instructionNames.put(Opcodes.DSTORE, "DSTORE");
        instructionNames.put(Opcodes.ASTORE, "ASTORE");
        instructionNames.put(Opcodes.RET, "RET");
        instructionNames.put(Opcodes.NEW, "NEW");
        instructionNames.put(Opcodes.ANEWARRAY, "ANEWARRAY");
        instructionNames.put(Opcodes.CHECKCAST, "CHECKCAST");
        instructionNames.put(Opcodes.INSTANCEOF, "INSTANCEOF");
        instructionNames.put(Opcodes.GETSTATIC, "GETSTATIC");
        instructionNames.put(Opcodes.PUTSTATIC, "PUTSTATIC");
        instructionNames.put(Opcodes.GETFIELD, "GETFIELD");
        instructionNames.put(Opcodes.PUTFIELD, "PUTFIELD");
        instructionNames.put(Opcodes.INVOKEVIRTUAL, "INVOKEVIRTUAL");
        instructionNames.put(Opcodes.INVOKESPECIAL, "INVOKESPECIAL");
        instructionNames.put(Opcodes.INVOKESTATIC, "INVOKESTATIC");
        instructionNames.put(Opcodes.INVOKEINTERFACE, "INVOKEINTERFACE");
        instructionNames.put(Opcodes.IFEQ, "IFEQ");
        instructionNames.put(Opcodes.IFNE, "IFNE");
        instructionNames.put(Opcodes.IFLT, "IFLT");
        instructionNames.put(Opcodes.IFGE, "IFGE");
        instructionNames.put(Opcodes.IFGT, "IFGT");
        instructionNames.put(Opcodes.IFLE, "IFLE");
        instructionNames.put(Opcodes.IF_ICMPEQ, "IF_ICMPEQ");
        instructionNames.put(Opcodes.IF_ICMPNE, "IF_ICMPNE");
        instructionNames.put(Opcodes.IF_ICMPLT, "IF_ICMPLT");
        instructionNames.put(Opcodes.IF_ICMPGE, "IF_ICMPGE");
        instructionNames.put(Opcodes.IF_ICMPGT, "IF_ICMPGT");
        instructionNames.put(Opcodes.IF_ICMPLE, "IF_ICMPLE");
        instructionNames.put(Opcodes.IF_ACMPEQ, "IF_ACMPEQ");
        instructionNames.put(Opcodes.IF_ACMPNE, "IF_ACMPNE");
        instructionNames.put(Opcodes.GOTO, "GOTO");
        instructionNames.put(Opcodes.JSR, "JSR");
        instructionNames.put(Opcodes.IFNULL, "IFNULL");
        instructionNames.put(Opcodes.IFNONNULL, "IFNONNULL");
    }

    private void write(String string) {
        debugger.put(insnCounter++, string);
    }

    private void write(int opcode, String params) {
        write(instructionNames.get(opcode) + " " + params);
    }

    private void formatVariables(Object[] variables, DisassemblyDocument section) {
        for (Object variable : variables) {
            if (Opcodes.TOP.equals(variable)) {
            } else if (Opcodes.INTEGER.equals(variable)) {
                section.put("int");
            } else if (Opcodes.FLOAT.equals(variable)) {
                section.put("float");
            } else if (Opcodes.DOUBLE.equals(variable)) {
                section.put("double");
            } else if (Opcodes.LONG.equals(variable)) {
                section.put("long");
            } else if (Opcodes.NULL.equals(variable)) {
                section.put("null");
            } else if (Opcodes.UNINITIALIZED_THIS.equals(variable)) {
                section.put("this");
            } else if (variable instanceof String) {
                String type = (String) variable;
                section.put(type);
            } else if (variable instanceof Label) {
                Label initialization = (Label) variable;
                section.put("Object created at " + initialization);
            } else {
                section.put("UNKNOWN TYPE");
            }
        }
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name,
            String desc) {
        super.visitFieldInsn(opcode, owner, name, desc);
        write(opcode, owner + "." + name + " : " + desc);
    }

    @Override
    public void visitFrame(int type, int nLocal, Object[] local, int nStack,
            Object[] stack) {
        super.visitFrame(type, nLocal, local, nStack, stack);
        switch (type) {
        case Opcodes.F_NEW:
            write("NEW Frame: " + nLocal + " locals, " + nStack
                    + " stack elements.");
            break;
        case Opcodes.F_SAME:
            write("SAME Frame: " + nLocal + " locals, " + nStack
                    + " stack elements.");
            break;
        case Opcodes.F_SAME1:
            write("SAME Frame + 1 stack element: " + nLocal + " locals, "
                    + nStack + " stack elements.");
            break;
        case Opcodes.F_APPEND:
            write("APPENDED Frame: " + nLocal + " locals added, " + nStack
                    + " stack elements.");
            break;
        case Opcodes.F_CHOP:
            write("CHOPPED Frame: " + nLocal
                    + " locals removed, stack emptied, " + nStack
                    + " stack elements.");
            break;
        case Opcodes.F_FULL:
            write("FULL Frame: " + nLocal + " locals, " + nStack
                    + " stack elements.");
            break;

        default:
            break;
        }
        DisassemblyDocument localSection = debugger.newSubSection();
        localSection.putTitle("Frame Locals:");
        formatVariables(local, localSection);
        DisassemblyDocument stackSection = debugger.newSubSection();
        stackSection.putTitle("Frame Stack elements:");
        formatVariables(stack, stackSection);
    }

    @Override
    public void visitIincInsn(int var, int increment) {
        super.visitIincInsn(var, increment);
        write("Iinc " + var + " " + increment);
    }

    @Override
    public void visitInsn(int opcode) {
        super.visitInsn(opcode);
        write(opcode, "");
    }

    @Override
    public void visitIntInsn(int opcode, int operand) {
        super.visitIntInsn(opcode, operand);
        write(opcode, "" + operand);
    }

    @Override
    public void visitJumpInsn(int opcode, Label label) {
        super.visitJumpInsn(opcode, label);
        write(opcode, label.toString());
    }

    @Override
    public void visitLabel(Label label) {
        super.visitLabel(label);
        debugger.putLabel(label.toString());
    }

    @Override
    public void visitLdcInsn(Object cst) {
        super.visitLdcInsn(cst);
        write("Load Constant " + cst);
    }

    @Override
    public void visitLineNumber(int line, Label start) {
        super.visitLineNumber(line, start);
        write("Line number: " + line + " at " + start);
    }

    @Override
    public void visitLocalVariable(String name, String desc, String signature,
            Label start, Label end, int index) {
        super.visitLocalVariable(name, desc, signature, start, end, index);
        debugger.put(desc + (signature != null ? "<" + signature + ">" : "")
                + " " + name + " : " + index + " from " + start + " to " + end);
    }

    @Override
    public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
        super.visitLookupSwitchInsn(dflt, keys, labels);
        write("Lookup Switch");
        DisassemblyDocument lookup = debugger.newSubSection();
        for (int i = 0; i < keys.length; i++) {
            lookup.put("case " + keys[i] + ": " + labels[i]);
        }
        lookup.put("default: " + dflt);
    }

    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
        super.visitMaxs(maxStack, maxLocals);
        debugger.put("Max Stack: " + maxStack);
        debugger.put("Max Locals: " + maxLocals);
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name,
            String desc) {
        super.visitMethodInsn(opcode, owner, name, desc);
        write(opcode, desc + " " + owner + "." + name);
    }

    @Override
    public void visitMultiANewArrayInsn(String desc, int dims) {
        super.visitMultiANewArrayInsn(desc, dims);
        write("MultiANewArray " + desc + " " + dims + " dimensions");
    }

    @Override
    public void visitTableSwitchInsn(int min, int max, Label dflt,
            Label[] labels) {
        super.visitTableSwitchInsn(min, max, dflt, labels);
        write("Table Switch");
        DisassemblyDocument lookup = debugger.newSubSection();
        for (int i = 0; min + i <= max; i++) {
            lookup.put("case " + (min + i) + ": " + labels[i]);
        }
        lookup.put("default: " + dflt);
    }

    @Override
    public void visitTryCatchBlock(Label start, Label end, Label handler,
            String type) {
        super.visitTryCatchBlock(start, end, handler, type);
        debugger.put("TryCatchBlock of " + type + " from " + start + " to "
                + end + " handled by: " + handler);
    }

    @Override
    public void visitTypeInsn(int opcode, String type) {
        super.visitTypeInsn(opcode, type);
        write(opcode, type);
    }

    @Override
    public void visitVarInsn(int opcode, int var) {
        super.visitVarInsn(opcode, var);
        write(opcode, "" + var);
    }

}
