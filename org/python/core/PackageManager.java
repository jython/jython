package org.python.core;

import java.util.*;
import java.io.*;
import java.util.zip.*;

class JarEntry implements Serializable {
    public long mtime;
    public File cachefile;
    public JarEntry(File cachefile, long mtime) {
        this.mtime = mtime;
        this.cachefile = cachefile;
    }
    public String toString() {
        return "JarEntry("+cachefile+":"+mtime+")";
    }
}

class JarPackage {
    public String name;
    public String classes;
    public String filename;
    public JarPackage(String name, String classes, String filename) {
        this.name = name;
        this.classes = classes;
        this.filename = filename;
    }
}


public class PackageManager {
    public File cachedir;
    private boolean indexModified;
    public Hashtable jarfiles;

    public Hashtable packages;

    public PyStringMap topLevelPackages;
    public PyList searchPath;
    public PyObject topDirectoryPackage;

    public PackageManager(File cachedir, Properties registry) {
        searchPath = new PyList();
        packages = new Hashtable();
        topLevelPackages = new PyStringMap();
        topDirectoryPackage = new PyJavaDirPackage("", searchPath);
        this.cachedir = cachedir;

        initPackages(registry);
    }

    public void initPackages(Properties registry) {
        if (cachedir == null) return;
        
        if (!cachedir.isDirectory() && cachedir.mkdirs() == false) {
            Py.writeWarning("packageManager", "can't create package cache dir, '"+cachedir+"'");
            return;
        }
        
        loadIndexFile();
        findAllPackages(registry);
        saveIndexFile();

        Hashtable packs = packages;

        for (Enumeration e = packs.elements(); e.hasMoreElements(); ) {
            JarPackage jp = (JarPackage)e.nextElement();
            makeJavaPackage(jp.name, jp.classes, jp.filename);
        }
    }
    
    public PyObject lookupName(String name) {
        int dot = name.indexOf('.');
        String firstName=name;
        String lastName=null;
        if (dot != -1) {
            firstName = name.substring(0,dot);
            lastName = name.substring(dot+1, name.length());
        }
        firstName = firstName.intern();
        PyObject top = findName(firstName);
        if (top == null) return null;
        name = lastName;
        while (name != null) {
            dot = lastName.indexOf('.');
            firstName = name;
            lastName = null;
            if (dot != -1) {
                firstName = name.substring(0,dot);
                lastName = name.substring(dot+1, name.length());
            }
            firstName = firstName.intern();
            top = top.__findattr__(firstName);
            if (top == null) return null;
            name = lastName;
        }
        return top;
    }
        

    public PyObject jarFindName(String name) {
        return topLevelPackages.__finditem__(name);
    }
    
    public PyObject dirFindName(String name) {
        return topDirectoryPackage.__findattr__(name);
    }

    /**
        Find a top-level package by name (a top-level package is one without
        any '.' in its name).

       @param name the name of the top-level package to find -
       <b> must be an interned string </b>.
       @return a package object or null if name is not found.
    **/
    public PyObject findName(String name) {
        // First look for this as a top-level .jar package
        PyObject pkg = jarFindName(name);
        if (pkg != null) return pkg;

        // Then check for it in the directory packages (this is slower)
        return dirFindName(name);
    }

    // Check that a given stream is a valid Java .class file
    // And return its access permissions as an int
    static int checkAccess(DataInputStream istream) throws IOException {
        int magic = istream.readInt();
        int minor = istream.readShort();
        int major = istream.readShort();
        if (magic != 0xcafebabe) return -1;
        // Check versions???
        //System.out.println("magic: "+magic+", "+major+", "+minor);
        int nconstants = istream.readShort();
        for(int i=1; i<nconstants; i++) {
            int cid = istream.readByte();
            //System.out.println(""+i+" : "+cid);
            switch (cid) {
            case 7: istream.skipBytes(2); break;
            case 9:
            case 10:
            case 11: istream.skipBytes(4); break;
            case 8: istream.skipBytes(2); break;
            case 3:
            case 4: istream.skipBytes(4); break;
            case 5:
            case 6: istream.skipBytes(8); i++; break;
            case 12: istream.skipBytes(4); break;
            case 1:
                //System.out.println("utf: "+istream.readUTF()+";");
                int slength = istream.readShort();
                istream.skipBytes(slength);
                break;
            default:
                //System.err.println("unexpected cid: "+cid+", "+i+", "+nconstants);
                //for (int j=0; j<10; j++) { System.err.print(", "+istream.readByte()); }
                //System.err.println();
                return -1;
            }
        }
        return istream.readShort();
    }

    // Add a single class from zipFile to zipPackages
    // Only add valid, public classes
    static void addZipEntry(Hashtable zipPackages, ZipEntry entry, ZipFile zipFile)
    throws IOException {
        String name = entry.getName();
        //System.err.println("entry: "+name);
        if (!name.endsWith(".class")) return;
        // Ignore inner classes (at least for now)
        if (name.indexOf("$") != -1) return;
        char sep = '/';
        int breakPoint = name.lastIndexOf(sep);
        if (breakPoint == -1) {
            breakPoint = name.lastIndexOf('\\');
            sep = '\\';
            // Ignore entries in top-level package
            if (breakPoint == -1) return;
        }

        String packageName = name.substring(0,breakPoint).replace(sep, '.');
        String className = name.substring(breakPoint+1, name.length()-6);

        // An extra careful test, maybe should be ignored????
        InputStream istream = zipFile.getInputStream(entry);
        int access = checkAccess(new DataInputStream(new BufferedInputStream(istream)));
        if ((access == -1) || ((access & 0x01) != 1)) return;

        Vector vec = (Vector)zipPackages.get(packageName);
        if (vec == null) {
            vec = new Vector();
            zipPackages.put(packageName, vec);
        }
        vec.addElement(className);
    }

    /*public static String[] vectorToStrings(Vector vec) {
      int n = vec.size();
      String[] ret = new String[n];
      for(int i=0; i<n; i++) {
      ret[i] = (String)vec.elementAt(i);
      }
      return ret;
      }*/
    static String vectorToString(Vector vec) {
        int n = vec.size();
        StringBuffer ret = new StringBuffer();
        for(int i=0; i<n; i++) {
            ret.append((String)vec.elementAt(i));
            if (i<n-1) ret.append(",");
        }
        return ret.toString();
    }


    // Extract all of the packages in a single jarfile
    static Hashtable getZipPackages(File jarfile) throws IOException {
        Hashtable zipPackages = new Hashtable();
        ZipFile zf = new ZipFile(jarfile);
        for (Enumeration e = zf.entries() ; e.hasMoreElements() ;) {
            ZipEntry entry = (ZipEntry)e.nextElement();
            addZipEntry(zipPackages, entry, zf);
        }
        // Turn each vector into a comma-separated String
        for (Enumeration e = zipPackages.keys() ; e.hasMoreElements() ;) {
            Object key = e.nextElement();
            Vector vec = (Vector)zipPackages.get(key);
            zipPackages.put(key, vectorToString(vec));
        }
        return zipPackages;
    }


    public void addDirectoryToPackages(File directory) {
        try {
            if (!directory.isDirectory()) return;

            searchPath.append(new PyString(directory.getCanonicalPath()));
        } catch (IOException ioe) {
            // silently skip any bad directories
            Py.writeWarning("packageManager",
            "skipping bad directory, '" + directory.toString() + "'");
        }
    }


    public void addJarToPackages(File jarfile) {
        try {
            if (!jarfile.exists()) return;

            long mtime = jarfile.lastModified();
            String canonicalJarfile = jarfile.getCanonicalPath();
            JarEntry jarEntry = (JarEntry)jarfiles.get(canonicalJarfile);

            if (jarEntry == null) {
                Py.writeMessage("packageManager", "processing new jar, \""+canonicalJarfile+"\"");

                jarEntry = new JarEntry(findJarCacheFile(jarfile), 0);
                jarfiles.put(canonicalJarfile, jarEntry);
            }

            Hashtable zipPackages = null;
            if (jarEntry.mtime == mtime) {
                zipPackages = readCacheFile(jarEntry.cachefile, mtime, canonicalJarfile);
            }

            if (zipPackages == null) {
                indexModified = true;
                if (jarEntry.mtime != 0) {
                    Py.writeMessage("packageManager", "processing modified jar, \""+canonicalJarfile+"\"");
                }
                jarEntry.mtime = mtime;

                zipPackages = getZipPackages(jarfile);
                // Write the cache file
                writeCacheFile(jarEntry.cachefile, mtime, canonicalJarfile, zipPackages);
            }

            addPackages(zipPackages, canonicalJarfile);
        } catch (IOException ioe) {
            // silently skip any bad directories
            Py.writeWarning("packageManager",
            "skipping bad jarfile, '" + jarfile.toString() + "'");
        }
    }

    void addPackages(Hashtable zipPackages, String jarfile) {
        for (Enumeration e = zipPackages.keys() ; e.hasMoreElements() ;) {
            Object key = e.nextElement();
            JarPackage value = new JarPackage((String)key, (String)zipPackages.get(key), jarfile);
            //Object value = zipPackages.get(key);
            packages.put(key, value);
        }
    }


    // Find a file in the current cachedir to hold cache for this jar
    File findJarCacheFile(File jarfile) {
        String jname = jarfile.getName();
        jname = jname.substring(0,jname.length()-4);

        int index = 1;
        String suffix = "";
        File cachefile = null;
        while (true) {
            cachefile = new File(cachedir, jname+suffix+".pkc");
            //System.err.println("try cachefile: "+cachefile);
            if (!cachefile.exists()) break;
            suffix = "$"+index;
            index += 1;
        }
        return cachefile;
    }

    // Write a cache file storing package info for a single .jar
    public static void writeCacheFile(File cachefile, long mtime, String canonicalJarfile,
    Hashtable zipPackages) {
        try {
            DataOutputStream ostream = new DataOutputStream(
                    new BufferedOutputStream(new FileOutputStream(cachefile)));
            ostream.writeUTF(canonicalJarfile);
            ostream.writeLong(mtime);
            Py.writeComment("packageManager", "rewriting cachefile for '"+canonicalJarfile+"'");

            for (Enumeration e = zipPackages.keys() ; e.hasMoreElements() ;) {
                String packageName = (String)e.nextElement();
                String classes = (String)zipPackages.get(packageName);
                ostream.writeUTF(packageName);
                ostream.writeUTF(classes);
            }
            ostream.close();
        } catch (IOException ioe) {
            Py.writeWarning("packageManager", "can't write cache file to, '"+cachefile+"'");
        }
    }

    // Read in cache file storing package info for a single .jar
    // Return null and delete this cachefile if it is invalid
    public static Hashtable readCacheFile(File cachefile, long mtime, String canonicalJarfile) {
        Py.writeDebug("packageManager", "reading cache, '"+canonicalJarfile+"'");
        try {
            DataInputStream istream = new DataInputStream(
                    new BufferedInputStream(new FileInputStream(cachefile)));
            String old_jarfile = istream.readUTF();
            long old_mtime = istream.readLong();
            if ((!old_jarfile.equals(canonicalJarfile)) || (old_mtime != mtime)) {
                Py.writeComment("packageManager",
                  "invalid cache file: "+cachefile+", "+canonicalJarfile+":"+old_jarfile+", "+mtime+":"+old_mtime);
                cachefile.delete();
                return null;
            }
            Hashtable packs = new Hashtable();
            try {
                while (true) {
                    String packageName = istream.readUTF();
                    String classes = istream.readUTF();
                    packs.put(packageName, classes);
                }
            } catch (EOFException eof) {
                ;
            }
            istream.close();

            return packs;
        } catch (IOException ioe) {
            if (cachefile.exists()) cachefile.delete();
            return null;
        }
    }


    public void findAllPackages(Properties registry) {
        String paths = registry.getProperty("python.packages.paths",
                                            "java.class.path,sun.boot.class.path");
        String directories = registry.getProperty("python.packages.directories",
                                                  "java.ext.dirs");
        String fakepath = registry.getProperty("python.packages.fakepath", "");

        StringTokenizer tok = new StringTokenizer(paths, ",");
        while  (tok.hasMoreTokens())  {
            String entry = tok.nextToken().trim();
            String tmp = registry.getProperty(entry);
            if (tmp == null) continue;
            addClassPath(tmp);
        }

        tok = new StringTokenizer(directories, ",");
        while  (tok.hasMoreTokens())  {
            String entry = tok.nextToken().trim();
            String tmp = registry.getProperty(entry);
            if (tmp == null) continue;
            addJarPath(tmp);
        }

        addClassPath(fakepath);
    }


    public PyJavaPackage makeJavaPackage(String name, String classes, String jarfile) {
        int dot = name.indexOf('.');
        String firstName=name;
        String lastName=null;
        if (dot != -1) {
            firstName = name.substring(0,dot);
            lastName = name.substring(dot+1, name.length());
        }

        firstName = firstName.intern();
        PyJavaPackage p = (PyJavaPackage)topLevelPackages.__finditem__(firstName);
        if (p == null) {
            p = new PyJavaPackage(firstName, jarfile);
            topLevelPackages.__setitem__(firstName, p);
        }
        PyJavaPackage ret = p;
        if (lastName != null) ret = p.addPackage(lastName, jarfile);
        ret._unparsedAll = classes;
        return ret;
    }

    public void addJarDir(String jdir) {
        File file = new File(jdir);
        if (!file.isDirectory()) return;
        String[] files = file.list();
        for(int i=0; i<files.length; i++) {
            String entry = files[i];
            if (entry.endsWith(".jar") || entry.endsWith(".zip")) {
                addJarToPackages(new File(jdir, entry));
            }
        }
        /*} catch (IOException ioe) {
            Py.writeWarning("packageManager", "bad jar directory, '"+jdir+"'");
        }*/
    }

    public void addJarPath(String path) {
        StringTokenizer tok = new StringTokenizer(path, java.io.File.pathSeparator);
        while  (tok.hasMoreTokens())  {
            String entry = tok.nextToken();
            addJarDir(entry);
        }
    }

    public void addClassPath(String path) {
        StringTokenizer tok = new StringTokenizer(path, java.io.File.pathSeparator);
        while  (tok.hasMoreTokens())  {
            String entry = tok.nextToken().trim();
            if (entry.endsWith(".jar") || entry.endsWith(".zip")) {
                addJarToPackages(new File(entry));
            } else {
                addDirectoryToPackages(new File(entry));
            }
        }
    }

    public void loadIndexFile() {
        indexModified = false;
        jarfiles = new Hashtable();        
        File indexFile = new File(cachedir, "packages.idx");

        try {
            if (!indexFile.exists()) return;

            DataInputStream istream = new DataInputStream(
                    new BufferedInputStream(new FileInputStream(indexFile)));
            try {
                while (true) {
                    String jarfile = istream.readUTF();
                    String cachefile = istream.readUTF();
                    long mtime = istream.readLong();
                    jarfiles.put(jarfile, new JarEntry(new File(cachefile), mtime));
                }
            } catch (EOFException eof) {
                ;
            }
            istream.close();
        } catch (IOException ioe) {
            Py.writeWarning("packageManager", "invalid index file, '"+indexFile+"'");
        }
    }

    public void saveIndexFile() {
        if (!indexModified) return;
        indexModified = false;

        Py.writeComment("packageManager", "writing modified index file");
        File indexFile = new File(cachedir, "packages.idx");

        try {
            DataOutputStream ostream = new DataOutputStream(
                    new BufferedOutputStream(new FileOutputStream(indexFile)));
            for (Enumeration e = jarfiles.keys(); e.hasMoreElements();) {
                String jarfile = (String)e.nextElement();
                JarEntry je = (JarEntry)jarfiles.get(jarfile);
                ostream.writeUTF(jarfile);
                ostream.writeUTF(je.cachefile.getCanonicalPath());
                ostream.writeLong(je.mtime);
            }
            ostream.close();
        } catch (IOException ioe) {
            Py.writeWarning("packageManager", "can't write index file, '"+indexFile+"'");
        }
    }
}