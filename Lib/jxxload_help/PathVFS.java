// Copyright 2000 Samuele Pedroni

package jxxload_help;

import java.util.Vector;
import java.util.Hashtable;
import java.util.Enumeration;
import java.util.zip.ZipFile;
import java.util.zip.ZipEntry;
import java.io.*;

public class PathVFS extends Object {
    
    public interface VFS {
        
       public InputStream open(String id);
        
    }
        
    public static class JarVFS implements VFS {
        private ZipFile zipfile;
        
        public JarVFS(String fname) throws IOException {
            zipfile = new ZipFile(fname);
        }
        
        public InputStream open(String id) {
            ZipEntry ent = zipfile.getEntry(id);
            if (ent == null) return null;
            try {
                return zipfile.getInputStream(ent);
            } catch(IOException e) {
                return null;
            }
        }
        
    }
    
    public static class DirVFS implements VFS {
        private String prefix;
        
        public DirVFS(String dir) {
            if (dir.length() == 0)
                prefix = null;
            else
                prefix = dir;
        }
        
        public InputStream open(String id) {
            File file = new File(prefix,id.replace('/',File.separatorChar));
            if (file.isFile()) {
                try {
                    return new BufferedInputStream(new FileInputStream(file));
                } catch(IOException e) {
                    return null;
                }
            }           
            return null;
        }
    }
    
    private Vector vfs = new Vector();
    private Hashtable once = new Hashtable();
    
    private final static Object PRESENT = new Object();
    
    public void addVFS(String fname) {
        if (fname.length() == 0) {
            if (!once.containsKey("")) {
                once.put("",PRESENT);
                vfs.addElement(new DirVFS(""));
            }
            return;
        }
        try {
            File file = new File(fname);
            String canon = file.getCanonicalPath().toString();
            if (!once.containsKey(canon)) {
                once.put(canon,PRESENT);
                if (file.isDirectory()) vfs.addElement(new DirVFS(fname));
                else if (file.exists() &&  (fname.endsWith(".jar") || fname.endsWith(".zip"))) {
                    vfs.addElement(new JarVFS(fname));
                }
            }
            
        } catch(IOException e) {}
    }
    
    public InputStream open(String id) {
        for(Enumeration enum = vfs.elements(); enum.hasMoreElements();) {
            VFS v = (VFS)enum.nextElement();
            InputStream stream = v.open(id);
            if (stream != null) return stream;
        }
        return null;
    }
      
}