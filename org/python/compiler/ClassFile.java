// Copyright © Corporation for National Research Initiatives

package org.python.compiler;
import java.util.*;
import java.io.*;


class Method
{
    int access, name, type;
    Attribute[] atts;

    public Method(int name, int type, int access, Attribute[] atts) {
        this.name = name;
        this.type = type;
        this.access = access;
        this.atts = atts;
    }

    public void write(DataOutputStream stream) throws IOException {
        stream.writeShort(access);
        stream.writeShort(name);
        stream.writeShort(type);
        ClassFile.writeAttributes(stream, atts);
    }

}



public class ClassFile
{
    ConstantPool pool;
    int access;
    public String name;
    String superclass;
    int[] interfaces;
    Vector methods;
    Vector fields;
    Vector attributes;

    public final static int PUBLIC = 0x1;
    public final static int PRIVATE = 0x2;
    public final static int PROTECTED = 0x4;
    public final static int STATIC = 0x8;
    public final static int FINAL = 0x10;
    public final static int SYNCHRONIZED = 0x20;
    public final static int NATIVE = 0x100;
    public final static int ABSTRACT = 0x400;

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
        this(name, "java/lang/Object", SYNCHRONIZED | PUBLIC);
    }

    public ClassFile(String name, String superclass, int access) {
        this.name = fixName(name);
        this.superclass = fixName(superclass);
        this.interfaces = new int[0];
        this.access = access;

        pool = new ConstantPool();
        methods = new Vector();
        fields = new Vector();
        attributes = new Vector();
    }

    public void addInterface(String name) throws IOException {
        int[] new_interfaces = new int[interfaces.length+1];
        System.arraycopy(interfaces, 0, new_interfaces, 0, interfaces.length);
        new_interfaces[interfaces.length] = pool.Class(name);
        interfaces = new_interfaces;
    }

    public Code addMethod(String name, String type, int access)
        throws IOException
    {
        Code code = new Code(type, pool, (access & STATIC) == STATIC);
        Method m = new Method(pool.UTF8(name), pool.UTF8(type), access,
                              new Attribute[] {code});
        methods.addElement(m);
        return code;
    }

    public void addField(String name, String type, int access)
        throws IOException
    {
        Method m = new Method(pool.UTF8(name), pool.UTF8(type), access,
                              new Attribute[0]);
        fields.addElement(m);
    }

    public static void writeAttributes(DataOutputStream stream,
                                       Attribute[] atts)
        throws IOException
    {
        stream.writeShort(atts.length);
        for (int i=0; i<atts.length; i++) {
            atts[i].write(stream);
        }
    }

    public void writeMethods(DataOutputStream stream, Vector methods)
        throws IOException
    {
        stream.writeShort(methods.size());
        for (int i=0; i<methods.size(); i++) {
            Method m = (Method)methods.elementAt(i);
            m.write(stream);
        }
    }
        
    public void addAttribute(Attribute attr) throws IOException {
        attributes.addElement(attr);
    }

    public void write(DataOutputStream stream) throws IOException {
        //Write Header
        int thisclass = pool.Class(name);
        int superclass = pool.Class(this.superclass); 

        stream.writeInt(0xcafebabe);
        stream.writeShort(0x3);
        stream.writeShort(0x2d);

        pool.write(stream);

        stream.writeShort(access);
        stream.writeShort(thisclass);
        stream.writeShort(superclass);

        //write out interfaces
        stream.writeShort(interfaces.length);
        for (int i=0; i<interfaces.length; i++)
            stream.writeShort(interfaces[i]);

        writeMethods(stream, fields);
        writeMethods(stream, methods);

        //write out class attributes
        int n = attributes.size();
        stream.writeShort(n);
                
        for (int i=0; i<n; i++) {
            ((Attribute)attributes.elementAt(i)).write(stream);
        }
    }

    public void write(OutputStream stream) throws IOException {
        write(new DataOutputStream(stream));
    }
}
