// Copyright (c) Corporation for National Research Initiatives
// Copyright 2000 Samuele Pedroni

package org.python.core;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.StringTokenizer;
import java.util.zip.ZipEntry;

import org.python.core.util.RelativeFile;

public class SyspathJavaLoader extends ClassLoader {

    private static final char SLASH_CHAR = '/';
    
    public InputStream getResourceAsStream(String res) {
        Py.writeDebug("resource", "trying resource: " + res);
        PySystemState sys = Py.getSystemState();
        ClassLoader classLoader = sys.getClassLoader();
        if (classLoader != null) {
            return classLoader.getResourceAsStream(res);
        }

        classLoader = this.getClass().getClassLoader();

        InputStream ret;

        if (classLoader != null) {
            ret = classLoader.getResourceAsStream(res);
        } else {
            ret = ClassLoader.getSystemResourceAsStream(res);
        }
        if (ret != null) {
            return ret;
        }

        if (res.charAt(0) == SLASH_CHAR) {
            res = res.substring(1);
        }
        String entryRes = res;
        if (File.separatorChar != SLASH_CHAR) {
            res = res.replace(SLASH_CHAR, File.separatorChar);
            entryRes = entryRes.replace(File.separatorChar, SLASH_CHAR);
        }
        
        PyList path = sys.path;
        for (int i = 0; i < path.__len__(); i++) {
            PyObject entry = path.__getitem__(i);
            if (entry instanceof SyspathArchive) {
                SyspathArchive archive = (SyspathArchive) entry;
                ZipEntry ze = archive.getEntry(entryRes);
                if (ze != null) {
                    try {
                        return archive.getInputStream(ze);
                    } catch (IOException e) {
                        ;
                    }
                }
                continue;
            }
            String dir = sys.getPath(entry.__str__().toString());
            try {
                return new BufferedInputStream(new FileInputStream(new File(
                        dir, res)));
            } catch (IOException e) {
                continue;
            }
        }

        return null;
    }

    // override from abstract base class
    protected Class loadClass(String name, boolean resolve)
            throws ClassNotFoundException {
        // First, if the Python runtime system has a default class loader,
        // defer to it.
        PySystemState sys = Py.getSystemState();
        ClassLoader classLoader = sys.getClassLoader();
        if (classLoader != null) {
            return classLoader.loadClass(name);
        }
        
        // Search the sys.path for a .class file matching the named class.
        try {
            return Class.forName(name);
        } catch(ClassNotFoundException e) {}
        
        // The current class loader may be null (e.g., when Jython is loaded
        // from the boot classpath); try the system class loader.
        try {
            return Class.forName(name, true, ClassLoader.getSystemClassLoader());
        } catch(ClassNotFoundException e) {}
        
        Class c = findLoadedClass(name);
        if(c != null) {
            return c;
        }
        
        PyList path = sys.path;
        for(int i = 0; i < path.__len__(); i++) {
            InputStream fis = null;
            File file = null;
            int size = 0;
            PyObject entry = path.__getitem__(i);
            if(entry instanceof SyspathArchive) {
                SyspathArchive archive = (SyspathArchive)entry;
                String entryname = name.replace('.', SLASH_CHAR) + ".class";
                ZipEntry ze = archive.getEntry(entryname);
                if(ze != null) {
                    try {
                        fis = archive.getInputStream(ze);
                        size = (int)ze.getSize();
                    } catch(IOException exc) {
                        ;
                    }
                }
            } else {
                String dir = entry.__str__().toString();
                file = getFile(dir, name);
                if(file != null) {
                    size = (int)file.length();
                    try {
                        fis = new FileInputStream(file);
                    } catch(FileNotFoundException e) {
                        ;
                    }
                }
            }
            if(fis == null) {
                continue;
            }
            try {
                byte[] buffer = new byte[size];
                int nread = 0;
                while(nread < size) {
                    nread += fis.read(buffer, nread, size - nread);
                }
                fis.close();
                return loadClassFromBytes(name, buffer);
            } catch(IOException e) {
                continue;
            } finally {
                try {
                    fis.close();
                } catch(IOException e) {
                    continue;
                }
            }
        }

        // couldn't find the .class file on sys.path
        throw new ClassNotFoundException(name);
    }

    private File getFile(String dir, String name) {
        String accum = "";
        boolean first = true;
        for (StringTokenizer t = new StringTokenizer(name, "."); t
                .hasMoreTokens();) {
            String token = t.nextToken();
            if (!first) {
                accum += File.separator;
            }
            accum += token;
            first = false;
        }
        return new RelativeFile(dir, accum + ".class");
    }

    private Class loadClassFromBytes(String name, byte[] data) {
        // System.err.println("loadClassFromBytes("+name+", byte[])");
        Class c = defineClass(name, data, 0, data.length);
        resolveClass(c);
        Compiler.compileClass(c);
        return c;
    }

}
