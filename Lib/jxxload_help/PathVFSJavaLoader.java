// Copyright 2000 Samuele Pedroni

package jxxload_help;

public class PathVFSJavaLoader extends ClassLoader {
    private ClassLoader parent;
   
    private PathVFS vfs;
    
    public java.util.Vector interfaces = new java.util.Vector();
    
    public PathVFSJavaLoader(PathVFS vfs,ClassLoader parent) {
        this.vfs = vfs;
        this.parent = parent;
    }
    
    protected Class loadClass(String name, boolean resolve) throws ClassNotFoundException {
        Class c;
        
        c = findLoadedClass(name);
        if (c != null) return c;
        
        try {
            if (parent != null) return parent.loadClass(name);
        } catch(ClassNotFoundException e) {
        }
        
        java.io.InputStream in = vfs.open(name.replace('.','/')+".class");
        if (in == null) throw new ClassNotFoundException(name);
        try {
	    byte[] buf = org.python.core.FileUtil.readBytes( in );
            in.close();
            return loadClassFromBytes(name,buf);
        } catch(java.io.IOException e) {
            throw new ClassNotFoundException(name);
        }
    }

    private Class loadClassFromBytes(String name, byte[] data) {
        Class c = defineClass(name, data, 0, data.length);
        resolveClass(c);
        if (c.isInterface()) interfaces.addElement(c);
        if (!org.python.core.Options.skipCompile) {
            Compiler.compileClass(c);
        }
        return c;
    }
  
}
