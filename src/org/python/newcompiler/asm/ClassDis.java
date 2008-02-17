// (C) Copyright 2007 Tobias Ivarsson
package org.python.newcompiler.asm;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.python.objectweb.asm.AnnotationVisitor;
import org.python.objectweb.asm.Attribute;
import org.python.objectweb.asm.ClassAdapter;
import org.python.objectweb.asm.ClassVisitor;
import org.python.objectweb.asm.FieldVisitor;
import org.python.objectweb.asm.MethodVisitor;
import org.python.objectweb.asm.Opcodes;
import org.python.newcompiler.DisassemblyDocument;

/**
 * A Java class disassembler.
 * 
 * @author Tobias Ivarsson
 */
public class ClassDis extends ClassAdapter {
    /**
     * @param sep
     * @param strings
     * @return the strings joined by the separator
     */
    public static String join(String sep, String[] strings) {
        String result = "";
        String addSep = "";
        for (String string : strings) {
            result += addSep + string;
            addSep = sep;
        }
        return result;
    }

    /**
     * @param sep
     * @param strings
     * @return the strings joined by the separator
     */
    public static String join(String sep, Iterable<String> strings) {
        String result = "";
        String addSep = "";
        for (String string : strings) {
            result += addSep + string;
            addSep = sep;
        }
        return result;
    }

    /**
     * @param desc
     * @param visible
     * @return output formatted annotation
     */
    public static String formatAnnotation(String desc, boolean visible) {
        return "@" + desc
                + (visible ? "" : " /* only visible at compile time */");
    }

    /**
     * @param attr
     * @return output formatted attribute
     */
    public static String formatAttribute(Attribute attr) {
        return "/* ATTRIBUTE: type=" + attr.type + " */";
    }

    private static Map<Integer, String> accessModes = new HashMap<Integer, String>();
    static {
        accessModes.put(0, null);
        accessModes.put(Opcodes.ACC_ABSTRACT, "abstract");
        accessModes.put(Opcodes.ACC_ANNOTATION, "annotation");
        accessModes.put(Opcodes.ACC_BRIDGE, "bridge");
        accessModes.put(Opcodes.ACC_DEPRECATED, "deprecated");
        accessModes.put(Opcodes.ACC_FINAL, "final");
        accessModes.put(Opcodes.ACC_NATIVE, "native");
        accessModes.put(Opcodes.ACC_PRIVATE, "private");
        accessModes.put(Opcodes.ACC_PROTECTED, "protected");
        accessModes.put(Opcodes.ACC_PUBLIC, "public");
        accessModes.put(Opcodes.ACC_STATIC, "static");
        accessModes.put(Opcodes.ACC_STRICT, "strict");
        accessModes.put(Opcodes.ACC_SUPER, "super");
        accessModes.put(Opcodes.ACC_SYNCHRONIZED, "synchronized");
        accessModes.put(Opcodes.ACC_SYNTHETIC, "synthetic");
        accessModes.put(Opcodes.ACC_TRANSIENT, "transient");
        accessModes.put(Opcodes.ACC_VARARGS, "varargs");
        accessModes.put(Opcodes.ACC_VOLATILE, "volatile");
    }

    /**
     * @param access
     * @return A string representation of the access modifiers
     */
    public static String accessToString(int access) {
        List<String> modes = new LinkedList<String>();
        for (int mode : accessModes.keySet()) {
            String modeString = accessModes.get(access & mode);
            if (modeString != null) {
                modes.add(modeString);
            }
        }
        return join(" ", modes);
    }

    private DisassemblyDocument debugger;

    /**
     * @param next The next {@link ClassVisitor} in the chain.
     * @param debugger The debugger used for handling events.
     */
    public ClassDis(ClassVisitor next, DisassemblyDocument debugger) {
        super(next);
        this.debugger = debugger;
    }

    @Override
    public void visit(int version, int access, String name, String signature,
            String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
        if (signature == null) {
            signature = "";
        }
        debugger.putTitle(accessToString(access) + " class " + name + signature
                + " extends " + superName + " implements "
                + join(", ", interfaces));
    }

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        AnnotationVisitor next = super.visitAnnotation(desc, visible);
        DisassemblyDocument preTitle = debugger.newPreTitle();
        preTitle.putTitle(formatAnnotation(desc, visible));
        if (next != null) {
            return new AnnotationDis(next, preTitle);
        } else {
            return null;
        }
    }

    @Override
    public void visitAttribute(Attribute attr) {
        super.visitAttribute(attr);
        debugger.putTitle(formatAttribute(attr));
    }

    @Override
    public void visitEnd() {
        super.visitEnd();
        // TODO: Something here?
    }

    @Override
    public FieldVisitor visitField(int access, String name, String desc,
            String signature, Object value) {
        FieldVisitor next = super.visitField(access, name, desc, signature,
                value);
        DisassemblyDocument fieldSection = debugger.newSubSection();
        String valueString;
        if (value != null) {
            valueString = " = " + value.toString();
        } else {
            valueString = "";
        }
        fieldSection.putTitle(accessToString(access) + " " + desc + " " + name
                + valueString);
        if (next != null) {
            return new FieldDis(next, fieldSection);
        } else {
            return null;
        }
    }

    @Override
    public void visitInnerClass(String name, String outerName,
            String innerName, int access) {
        super.visitInnerClass(name, outerName, innerName, access);
        // FIXME: Implements this when needed.
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc,
            String signature, String[] exceptions) {
        MethodVisitor next = super.visitMethod(access, name, desc, signature,
                exceptions);
        DisassemblyDocument methodSection = debugger.newSubSection();
        // TODO: something
        return new MethodDis(next, methodSection);
    }

    @Override
    public void visitOuterClass(String owner, String name, String desc) {
        super.visitOuterClass(owner, name, desc);
        // FIXME: Implement this when needed.
    }

    @Override
    public void visitSource(String source, String debug) {
        super.visitSource(source, debug);
        debugger.putTitle("Source: " + source);
    }

}
