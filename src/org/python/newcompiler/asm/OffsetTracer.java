package org.python.newcompiler.asm;

import java.io.PrintWriter;
import java.util.Formatter;

import org.python.objectweb.asm.ClassVisitor;
import org.python.objectweb.asm.Label;
import org.python.objectweb.asm.util.TraceClassVisitor;
import org.python.objectweb.asm.util.TraceMethodVisitor;

public class OffsetTracer extends TraceClassVisitor {

    public OffsetTracer(ClassVisitor cv, PrintWriter pw) {
        super(cv, pw);
    }

    public OffsetTracer(PrintWriter pw) {
        super(pw);
    }

    protected TraceMethodVisitor createTraceMethodVisitor() {
        return createTMV(2);
    }

    public static TraceMethodVisitor createTMV(final int width) {
        return new TraceMethodVisitor() {

            private int insnCounter = 0;

            private void printInsnCount(int count) {
                buf.setLength(0);
                Formatter formatter = new Formatter(buf);
                formatter.format("insn %" + width + "d:",
                        new Object[] { new Integer(count) });
                text.add(buf.toString());
            }

            private void printInsnCount() {
                printInsnCount(insnCounter++);
            }

            public void visitLabel(Label arg0) {
                printInsnCount();
                super.visitLabel(arg0);
            }

            public void visitFrame(int arg0, int arg1, Object[] arg2, int arg3,
                    Object[] arg4) {
                printInsnCount();
                super.visitFrame(arg0, arg1, arg2, arg3, arg4);
            }

            public void visitLineNumber(int arg0, Label arg1) {
                printInsnCount();
                super.visitLineNumber(arg0, arg1);
            }

            public void visitFieldInsn(int arg0, String arg1, String arg2,
                    String arg3) {
                printInsnCount();
                super.visitFieldInsn(arg0, arg1, arg2, arg3);
            }

            public void visitIincInsn(int arg0, int arg1) {
                printInsnCount();
                super.visitIincInsn(arg0, arg1);
            }

            public void visitInsn(int arg0) {
                printInsnCount();
                super.visitInsn(arg0);
            }

            public void visitIntInsn(int arg0, int arg1) {
                printInsnCount();
                super.visitIntInsn(arg0, arg1);
            }

            public void visitJumpInsn(int arg0, Label arg1) {
                printInsnCount();
                super.visitJumpInsn(arg0, arg1);
            }

            public void visitLdcInsn(Object arg0) {
                printInsnCount();
                super.visitLdcInsn(arg0);
            }

            public void visitLookupSwitchInsn(Label arg0, int[] arg1,
                    Label[] arg2) {
                printInsnCount();
                super.visitLookupSwitchInsn(arg0, arg1, arg2);
            }

            public void visitMethodInsn(int arg0, String arg1, String arg2,
                    String arg3) {
                printInsnCount();
                super.visitMethodInsn(arg0, arg1, arg2, arg3);
            }

            public void visitMultiANewArrayInsn(String arg0, int arg1) {
                printInsnCount();
                super.visitMultiANewArrayInsn(arg0, arg1);
            }

            public void visitTableSwitchInsn(int arg0, int arg1, Label arg2,
                    Label[] arg3) {
                printInsnCount();
                super.visitTableSwitchInsn(arg0, arg1, arg2, arg3);
            }

            public void visitTypeInsn(int arg0, String arg1) {
                printInsnCount();
                super.visitTypeInsn(arg0, arg1);
            }

            public void visitVarInsn(int arg0, int arg1) {
                printInsnCount();
                super.visitVarInsn(arg0, arg1);
            }

        };
    }

}
