// Copyright (c) Corporation for National Research Initiatives

package org.python.compiler;

import java.io.*;
import java.util.Vector;

class ExceptionLabel
{
    public Label start, end, handler;
    public int exc;

    public ExceptionLabel(Label start, Label end, Label handler, int exc) {
        this.start = start;
        this.end = end;
        this.handler = handler;
        this.exc = exc;
    }
}


public class Code extends Attribute
{
    ConstantPool pool;
    public int stack;
    int max_stack;
    public DataOutputStream code;
    ByteArrayOutputStream stream;
    String sig;
    boolean locals[];
    int nlocals;
    int att_name;
    Vector labels, exceptions;
    LineNumberTable linenumbers;

    public Label getLabel() {
        Label l = new Label(this);
        addLabel(l);
        return l;
    }

    public void addLabel(Label l) {
        labels.addElement(l);
    }

    public int size() {
        return stream.size();
    }

    public Code(String sig, ConstantPool pool, boolean isStatic) {
        this.sig = sig;
        max_stack = 2;
        stack = 0;
        this.pool = pool;
        stream = new ByteArrayOutputStream();
        code = new DataOutputStream(stream);
        nlocals = -ConstantPool.sigSize(sig, false);
        if (!isStatic) nlocals = nlocals+1;
        locals = new boolean[nlocals+128];
        for(int i=0; i<nlocals; i++) {
            locals[i] = true;
        }
        labels = new Vector();
        exceptions = new Vector();
        try {
            att_name = pool.UTF8("Code");
        } catch (IOException e) {
            att_name=0;
        }
    }

    public int getLocal() {
        //Could optimize this to skip arguments?
        for(int l = 0; l<nlocals; l++) {
            if (!locals[l]) {
                locals[l] = true;
                return l;
            }
        }
        if (nlocals >= locals.length) {
            boolean[] new_locals = new boolean[locals.length*2];
            System.arraycopy(locals, 0, new_locals, 0, locals.length);
            locals = new_locals;
        }
        locals[nlocals] = true;
        nlocals += 1;
        return nlocals-1;
    }

    public void freeLocal(int l) {
        locals[l] = false;
    }


    public void addExceptionHandler(Label begin, Label end,
                                    Label handler, int exc)
    {
        exceptions.addElement(new ExceptionLabel(begin, end, handler, exc));
    }

    /*
      cl = self.code_length()
      self.length = cl+12+8*len(self.exc_table)
      cw.put2(self.name)
      cw.put4(self.length)
      cw.put2(self.max_stack)
      cw.put2(len(self.locals))
      cw.put4(cl)
      self.dump_code(cw)
      cw.put2(len(self.exc_table))
      for start, end, handler, exc in self.exc_table:
      cw.put2(self.labels[start])
      cw.put2(self.labels[end])
      cw.put2(self.labels[handler])
      cw.put2(exc)
      cw.dump_attributes(self.attributes)
    */

    public void fixLabels(byte[] bytes) throws IOException {
        for(int i=0; i<labels.size(); i++) {
            ((Label)labels.elementAt(i)).fix(bytes);
        }
    }

    public void write(DataOutputStream stream) throws IOException {
        byte[] bytes = this.stream.toByteArray();

        fixLabels(bytes);

        int n = exceptions.size();
        int length = bytes.length+12+8*n;;
        if (linenumbers != null)
            length += linenumbers.length();
        stream.writeShort(att_name);
        stream.writeInt(length);
        stream.writeShort(max_stack);
        stream.writeShort(nlocals);
        stream.writeInt(bytes.length);
        stream.write(bytes);

        //No Exceptions for now
        stream.writeShort(n);
        for(int i=0; i<n; i++) {
            ExceptionLabel e = (ExceptionLabel)exceptions.elementAt(i);
            stream.writeShort(e.start.getPosition());
            stream.writeShort(e.end.getPosition());
            stream.writeShort(e.handler.getPosition());
            stream.writeShort(e.exc);
        }
        if (linenumbers != null)
            ClassFile.writeAttributes(stream,
                                      new Attribute[] { linenumbers });
        else
            ClassFile.writeAttributes(stream, new Attribute[0]);
    }

    public void push(int i) {
        //System.out.println("push: "+i+" : "+stack);
        stack = stack+i;
        if (stack > max_stack) max_stack = stack;
        if (stack < 0)
            throw new InternalError("stack < 0: "+stack);
    }

    public void branch(int b, Label label) throws IOException {
        int offset = size();
        code.writeByte(b);
        label.setBranch(offset, 2);
        label.setStack(stack);
    }

    public void print(String s) throws IOException {
        getstatic("java/lang/System", "out", "Ljava/io/PrintStream;");
        ldc(s);
        invokevirtual("java/io/PrintStream", "println",
                      "(Ljava/lang/String;)V");
    }


    public void aaload() throws IOException {
        code.writeByte(50);
        push(-1);
    }

    public void aastore() throws IOException {
        code.writeByte(83);
        push(-3);
    }

    public void aconst_null() throws IOException {
        code.writeByte(1);
        push(1);
    }

    public void aload(int i) throws IOException {
        if (i >= 0 && i < 4) {
            code.writeByte(42+i);
        } else {
            code.writeByte(25);
            code.writeByte(i);
        }
        push(1);
    }

    public void anewarray(int c) throws IOException {
        code.writeByte(189);
        code.writeShort(c);
        //push(-1); push(1);
    }

    public void areturn() throws IOException {
        code.writeByte(176);
        push(-1);
    }

    public void arraylength() throws IOException {
        code.writeByte(190);
        //push(-1); push(1);
    }

    public void astore(int i) throws IOException {
        if (i >= 0 && i < 4) {
            code.writeByte(75+i);
        } else {
            code.writeByte(58);
            code.writeByte(i);
        }
        push(-1);
    }

    public void athrow() throws IOException {
        code.writeByte(191);
        push(-1);
    }

    public void checkcast(int c) throws IOException {
        code.writeByte(192);
        code.writeShort(c);
    }

    public void dload(int i) throws IOException {
        if (i >= 0 && i < 4) {
            code.writeByte(38+i);
        } else {
            code.writeByte(24);
            code.writeByte(i);
        }
        push(2);
    }

    public void dreturn() throws IOException {
        code.writeByte(175);
        push(-2);
    }

    public void dup() throws IOException {
        code.writeByte(89);
        push(1);
    }

    public void dup_x1() throws IOException {
        code.writeByte(90);
        push(1);
    }

    public void fload(int i) throws IOException {
        if (i >= 0 && i < 4) {
            code.writeByte(34+i);
        } else {
            code.writeByte(23);
            code.writeByte(i);
        }
        push(1);
    }

    public void freturn() throws IOException {
        code.writeByte(174);
        push(-1);
    }

    public void getfield(int c) throws IOException {
        code.writeByte(180);
        code.writeShort(c);
        push(pool.sizes[c]-1);
    }

    public void getfield(String c, String name, String type)
        throws IOException
    {
        getfield(pool.Fieldref(c, name, type));
    }

    public void getstatic(int c) throws IOException {
        code.writeByte(178);
        code.writeShort(c);
        push(pool.sizes[c]);
    }

    public void getstatic(String c, String name, String type)
        throws IOException
    {
        getstatic(pool.Fieldref(c, name, type));
    }

    public void goto_(Label label) throws IOException {
        branch(167, label);
    }

    public void iconst(int i) throws IOException {
        if (i >= -1 && i <= 5) {
            code.writeByte(3+i);
        } else {
            if (i > -127 && i < 128) {
                code.writeByte(16);
                if (i < 0) i = 256+i;
                code.writeByte(i);
            } else {
                if (i > -32767 && i < 32768) {
                    code.writeByte(17);
                    if (i < 0) i = i+65536;
                    code.writeShort(i);
                } else {
                    ldc(pool.Integer(i));
                }
            }
        }
        push(1);
    }

    public void if_icmpne(Label label) throws IOException {
        push(-2);
        branch(160, label);
    }

    public void ifeq(Label label) throws IOException {
        push(-1);
        branch(153, label);
    }

    public void ifne(Label label) throws IOException {
        push(-1);
        branch(154, label);
    }

    public void ifnonnull(Label label) throws IOException {
        push(-1);
        branch(199, label);
    }

    public void ifnull(Label label) throws IOException {
        push(-1);
        branch(198, label);
    }

    public void iinc(int i, int increment) throws IOException {
        code.writeByte(132);
        code.writeByte(i);
        code.writeByte(increment);
    }

    public void iinc(int i) throws IOException {
        iinc(i, 1);
    }

    public void iload(int i) throws IOException {
        if (i >= 0 && i < 4) {
            code.writeByte(26+i);
        } else {
            code.writeByte(21);
            code.writeByte(i);
        }
        push(1);
    }

    public void invokespecial(int c) throws IOException {
        code.writeByte(183);
        code.writeShort(c);
        push(pool.sizes[c]-1);
    }

    public void invokestatic(int c) throws IOException {
        code.writeByte(184);
        code.writeShort(c);
        push(pool.sizes[c]);
    }

    public void invokevirtual(int c) throws IOException {
        code.writeByte(182);
        code.writeShort(c);
        push(pool.sizes[c]-1);
    }

    public void invokevirtual(String c, String name, String type)
        throws IOException
    {
        invokevirtual(pool.Methodref(c, name, type));
    }

    public void ireturn() throws IOException {
        code.writeByte(172);
        push(-1);
    }

    public void istore(int i) throws IOException {
        if (i >= 0 && i < 4) {
            code.writeByte(59+i);
        } else {
            code.writeByte(54);
            code.writeByte(i);
        }
        push(-1);
    }

    public void jsr(Label label) throws IOException {
        //push(-1);
        int offset = size();
        code.writeByte(168);
        label.setBranch(offset, 2);
        label.setStack(stack+1);
    }

    public void ldc(int c) throws IOException {
        int size = pool.sizes[c];
        if (size == 1) {
            if (c < 256) {
                code.writeByte(18);
                code.writeByte(c);
            } else {
                code.writeByte(19);
                code.writeShort(c);
            }
        } else {
            code.writeByte(20);
            code.writeShort(c);
        }

        push(pool.sizes[c]);
    }

    public void ldc(String s) throws IOException {
        ldc(pool.String(s));
    }

    public void lload(int i) throws IOException {
        if (i >= 0 && i < 4) {
            code.writeByte(30+i);
        } else {
            code.writeByte(22);
            code.writeByte(i);
        }
        push(2);
    }

    public void lreturn() throws IOException {
        code.writeByte(173);
        push(-2);
    }

    public void new_(int c) throws IOException {
            code.writeByte(187);
            code.writeShort(c);
            push(1);
        }

    public void pop() throws IOException {
        code.writeByte(87);
        push(-1);
    }

    public void putfield(int c) throws IOException {
        code.writeByte(181);
        code.writeShort(c);
        push(-pool.sizes[c]-1);
    }

    public void putfield(String c, String name, String type)
        throws IOException
    {
        putfield(pool.Fieldref(c, name, type));
    }

    public void putstatic(int c) throws IOException {
        code.writeByte(179);
        code.writeShort(c);
        push(-pool.sizes[c]);
    }

    public void putstatic(String c, String name, String type)
        throws IOException
    {
        putstatic(pool.Fieldref(c, name, type));
    }

    public void return_() throws IOException {
        code.writeByte(177);
    }

    public void ret(int index) throws IOException {
        code.writeByte(169);
        code.writeByte(index);
    }

    public void swap() throws IOException {
        code.writeByte(95);
    }

    public void tableswitch(Label def, int low, Label[] labels)
        throws IOException
    {
        int position = size();
        push(-1);
        code.writeByte(170);
        for(int j=0; j<((3-position)%4); j++) code.writeByte(0);
        def.setBranch(position, 4);
        code.writeInt(low);
        code.writeInt(labels.length-1);
        for(int i=0; i<labels.length; i++) {
            labels[i].setBranch(position, 4);
        }
    }

    public void setline(int line) throws IOException {
        if (linenumbers == null)
            linenumbers = new LineNumberTable(pool);
        linenumbers.addLine(size(), line);
    }
}
