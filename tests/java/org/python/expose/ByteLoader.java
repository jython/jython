package org.python.expose;

public class ByteLoader extends ClassLoader {

    public Class loadClassFromBytes(String name, byte[] data) {
        Class c = defineClass(name, data, 0, data.length, this.getClass().getProtectionDomain());
        resolveClass(c);
        return c;
    }
}
