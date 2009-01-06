/*
 * Copyright (c) 2008 Jython Developers
 * Licensed to PSF under a Contributor Agreement.
 */
package org.python.core;

import java.io.IOException;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.commons.EmptyVisitor;

/**
 * This class reads a classfile from a byte array and pulls out the value of the class annotation
 * for APIVersion, which can then be retrieved by a call to getVersion().
 *
 * Hopefully the use of ClassReader in this implementation is not too expensive. I suspect it is not
 * since EmptyVisitor is just a bag of empty methods so shouldn't cost too much. If it turns out to
 * cost too much, we will want to implement a special purpose ClassReader that only reads out the
 * APIVersion annotation I think.
 */
public class APIReader extends EmptyVisitor {

    private boolean nextVisitIsVersion = false;

    private int version = -1;

    /**
     * Reads the classfile bytecode in data and to extract the version.
     * @throws IOException - if the classfile is malformed.
     */
    public APIReader(byte[] data) throws IOException {
        ClassReader r;
        try {
            r = new ClassReader(data);
        } catch (ArrayIndexOutOfBoundsException e) {
            IOException ioe = new IOException("Malformed bytecode: not enough data");
            ioe.initCause(e);// IOException didn't grow a constructor that could take a cause till
                             // 1.6, so do it the old fashioned way
            throw ioe;
        }
        r.accept(this, 0);
    }

    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        nextVisitIsVersion = desc.equals("Lorg/python/compiler/APIVersion;");
        return this;
    }

    public void visit(String name, Object value) {
        if (nextVisitIsVersion) {
            version = (Integer)value;
            nextVisitIsVersion = false;
        }
    }

    public int getVersion() {
        return version;
    }
}
