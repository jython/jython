// Copyright (c) Corporation for National Research Initiatives

package org.python.compiler;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.python.objectweb.asm.Attribute;
import org.python.objectweb.asm.ClassWriter;
import org.python.objectweb.asm.FieldVisitor;
import org.python.objectweb.asm.MethodVisitor;
import org.python.objectweb.asm.Opcodes;

public class ClassFile
{
    ClassWriter cw;
    int access;
    public String name;
    String superclass;
    String sfilename;
    String[] interfaces;
    List<MethodVisitor> methodVisitors;
    List<FieldVisitor> fieldVisitors;
    List<Attribute> attributes;

    public static String fixName(String n) {
        if (n.indexOf('.') == -1)
            return n;
        char[] c = n.toCharArray();
        for(int i=0; i<c.length; i++) {
            if (c[i] == '.') c[i] = '/';
        }
        return new String(c);
    }

    public ClassFile(String name) {
        this(name, "java/lang/Object", Opcodes.ACC_SYNCHRONIZED | Opcodes.ACC_PUBLIC);
    }

    public ClassFile(String name, String superclass, int access) {
        this.name = fixName(name);
        this.superclass = fixName(superclass);
        this.interfaces = new String[0];
        this.access = access;
        
        //XXX: can we do better than ASM for computing MAXS?
        cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);

        methodVisitors = Collections.synchronizedList(new ArrayList());
        fieldVisitors = Collections.synchronizedList(new ArrayList());
        attributes = Collections.synchronizedList(new ArrayList());
    }

    public void setSource(String name) {
        sfilename = name;
    }

    public void addInterface(String name) throws IOException {
        String[] new_interfaces = new String[interfaces.length+1];
        System.arraycopy(interfaces, 0, new_interfaces, 0, interfaces.length);
        new_interfaces[interfaces.length] = name;
        interfaces = new_interfaces;
    }

    //FIXME: Should really return a MethodVisitor
    public Code addMethod(String name, String type, int access)
        throws IOException
    {
        MethodVisitor mv = cw.visitMethod(access, name, type, null, null);
        Code pmv = new Code(mv, type, access);
        methodVisitors.add(pmv);
        return pmv;
    }

    public void addField(String name, String type, int access)
        throws IOException
    {
        FieldVisitor fv = cw.visitField(access, name, type, null, null);
        fieldVisitors.add(fv);
    }

    public void endAttributes()
        throws IOException
    {
        for (Attribute attr : attributes) {
            cw.visitAttribute(attr);
        }
    }

    public void endFields()
        throws IOException
    {
        for (FieldVisitor fv : fieldVisitors) {
            fv.visitEnd();
        }
    }
    
    public void endMethods()
        throws IOException
    {
        for (int i=0; i<methodVisitors.size(); i++) {
            MethodVisitor mv = (MethodVisitor)methodVisitors.get(i);
            mv.visitMaxs(0,0);
            mv.visitEnd();
        }
    }

    public void addAttribute(Attribute attr) throws IOException {
        //FIXME: Do nothing for now.
        //attributes.add(attr);
    }

    public void write(OutputStream stream) throws IOException {
        cw.visit(Opcodes.V1_5, Opcodes.ACC_PUBLIC + Opcodes.ACC_SUPER, this.name, null, this.superclass, interfaces);
        if (sfilename != null) {
            cw.visitSource(sfilename, null);
        }
        endAttributes();
        endFields();
        endMethods();

        byte[] ba = cw.toByteArray();
        //fos = io.FileOutputStream("%s.class" % self.name)
        ByteArrayOutputStream baos = new ByteArrayOutputStream(ba.length);
        baos.write(ba, 0, ba.length);
        baos.writeTo(stream);
        debug(baos);
        baos.close();
    }
    
    //XXX: this should go away when things stabilize.
    private void debug(ByteArrayOutputStream baos) throws IOException {
        FileOutputStream fos = new FileOutputStream("DEBUG.class");
        baos.writeTo(fos);
        fos.close();
    }
}
