// Copyright 2000 Samuele Pedroni

package jxxload_help;

public class PackageManager extends org.python.core.packagecache.PathPackageManager {
    
    private JavaLoaderFactory factory;
    private ClassLoader loader;
    
    public synchronized ClassLoader getLoader() {
        if (loader == null) loader = factory.makeLoader();
        return loader;
    }
    
    public synchronized  ClassLoader checkLoader() {
        return loader;
    }
    
    public synchronized void resetLoader() {
        loader = null;
    }
    
    // ??pending add cache support?
    public PackageManager(org.python.core.PyList path,JavaLoaderFactory factory) { 
        this.factory = factory;
        
        for (int i = 0; i < path.__len__(); i++) {
            String entry = path.__finditem__(i).toString();
            if (entry.endsWith(".jar") || entry.endsWith(".zip")) {
                addJarToPackages(new java.io.File(entry),false);
            } else {
                java.io.File dir = new java.io.File(entry);
                if (entry.length() == 0 || dir.isDirectory()) addDirectory(dir);
            }
        }
    }

    public Class findClass(String pkg,String name,String reason) {
        if (pkg != null && pkg.length()>0) name = pkg + '.' + name;
        try {
            return getLoader().loadClass(name);
        } 
        catch(ClassNotFoundException e) {
            return null;
        }
        catch (LinkageError e) {
            throw org.python.core.Py.JavaError(e);
        }
    }

    public void addJarDir(String jdir, boolean cache) {
        throw new RuntimeException("addJarDir not supported for reloadable packages");
    }  

    public void addJar(String jdir, boolean cache) {
        throw new RuntimeException("addDir not supported for reloadable packages");
    }  
}
