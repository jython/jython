package org.python.core;

import java.util.*;
import java.io.*;
import java.util.zip.*;

class JarEntry implements Serializable {
    public long mtime;
    public String cachefile;
    public String toString() {
        return "JarEntry("+cachefile+":"+mtime+")";
    }
}

public class PackageCache {
    public String cachedir;
    private boolean indexModified;
    public Hashtable jarfiles;
    
	public Hashtable packages;

    public PyStringMap topLevelPackages;
    public PyList searchPath;
    public PyObject topPackage;

    public PackageCache() {
        searchPath = new PyList();
        packages = new Hashtable();
        topLevelPackages = new PyStringMap();
        topPackage = new PyJavaDirPackage("", searchPath);
    }

    public PyObject findName(String name) {
        // First look for this as a top-level package
        PyObject pkg = topLevelPackages.__finditem__(name);
        if (pkg != null) return pkg;
        
        return topPackage.__findattr__(name);
        /*
        // Next search the searchPath for this name
        PyObject item;
        int i = 0;
        while ( (item = searchPath.__finditem__(i++)) != null) {
            File testdir = new File(item.toString(), name);
            if (testdir.isDirectory()) {
                return new PyJavaDirPackage(testdir.getCanonicalPath());
            }
            // Maybe check for name.class here?
        }
        return null;
        // Assume that Class.forName will be tried after this is all over...
        */
    }

    public static int checkAccess(DataInputStream istream) throws IOException {
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

    public static void addEntry(Hashtable packages, ZipEntry entry, ZipFile zf) throws IOException {
        String name = entry.getName();
        if (!name.endsWith(".class")) return;
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
        InputStream istream = zf.getInputStream(entry);
        int access = checkAccess(new DataInputStream(new BufferedInputStream(istream)));
        if ((access == -1) || ((access & 0x01) != 1)) return;

        Vector vec = (Vector)packages.get(packageName);
        if (vec == null) {
            vec = new Vector();
            packages.put(packageName, vec);
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
    public static String vectorToString(Vector vec) {
        int n = vec.size();
        StringBuffer ret = new StringBuffer();
        for(int i=0; i<n; i++) {
            ret.append((String)vec.elementAt(i));
            if (i<n-1) ret.append(",");
        }
        return ret.toString();
    }

    public static Hashtable getPackages(File jarfile) throws IOException {
        Hashtable packages = new Hashtable();
        ZipFile zf = new ZipFile(jarfile);
        for (Enumeration e = zf.entries() ; e.hasMoreElements() ;) {
            ZipEntry entry = (ZipEntry)e.nextElement();
            addEntry(packages, entry, zf);
        }
        // Turn each vector into a String
        for (Enumeration e = packages.keys() ; e.hasMoreElements() ;) {
            Object key = e.nextElement();
            Vector vec = (Vector)packages.get(key);
            packages.put(key, vectorToString(vec));
        }
        return packages;
    }

    public String findCacheFile(File jarfile) {
        //File jfile = new File(jarfile);
        String name = jarfile.getName();
        return name.substring(0,name.length()-4)+".cache";
    }

    public static void writeCacheFile(File cachefile, long mtime,
      String cpath, Hashtable packs) throws IOException {
	    DataOutputStream ostream = new DataOutputStream(
              new BufferedOutputStream(new FileOutputStream(cachefile)));
        ostream.writeUTF(cpath);
        ostream.writeLong(mtime);
        if (Options.verbosePackageCache) {
            System.err.println("packageCache: rewriting cache file for \""+cpath+"\"");
        }
        for (Enumeration e = packs.keys() ; e.hasMoreElements() ;) {
            String packageName = (String)e.nextElement();
            String classes = (String)packs.get(packageName);
            ostream.writeUTF(packageName);
            ostream.writeUTF(classes);
        }
        ostream.close();
      }

    public static Hashtable readCacheFile(File cachefile, long mtime, String cpath)
    throws IOException {
	    DataInputStream istream = new DataInputStream(
              new BufferedInputStream(new FileInputStream(cachefile)));
	    String old_cpath = istream.readUTF();
	    long old_mtime = istream.readLong();
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
    }

    public void addDirectory(File directory) throws IOException {
        if (!directory.isDirectory()) return;
        
        searchPath.append(new PyString(directory.getCanonicalPath()));
    }


    public void addJar(File jarfile) throws IOException {
        if (!jarfile.exists()) return;

        
        long mtime = jarfile.lastModified();
        String cpath = jarfile.getCanonicalPath();
        JarEntry jarEntry = (JarEntry)jarfiles.get(cpath);

        //if (Options.verbosePackageCache) {
        //    System.err.println("packageCache: adding jar: \""+cpath+"\"");
        //}

        if (jarEntry == null) {
            if (Options.verbosePackageCache) {
                System.err.println("packageCache: new jar found in \""+cpath+"\"");
            }
            jarEntry = new JarEntry();
            jarEntry.mtime = 0;
            jarEntry.cachefile = null;
            jarfiles.put(cpath, jarEntry);
        }

        if (jarEntry.mtime != mtime) {
            indexModified = true;
            if (Options.verbosePackageCache) {
                System.err.println("packageCache: modified jar found in \""+cpath+"\"");
            }
            jarEntry.mtime = mtime;
            if (jarEntry.cachefile == null) {
                jarEntry.cachefile = findCacheFile(jarfile);
            }
            Hashtable packages = getPackages(jarfile);
            // Write the cache file
            writeCacheFile(new File(cachedir, jarEntry.cachefile), mtime, cpath, packages);

            addPackages(packages);
        } else {
            Hashtable packages = readCacheFile(new File(cachedir, jarEntry.cachefile), mtime, cpath);
            //System.err.println("packs from: "+new File(cachedir, jarEntry.cachefile));
            addPackages(packages);
        }
    }

    public void addPackages(Hashtable packs) {
        for (Enumeration e = packs.keys() ; e.hasMoreElements() ;) {
            Object key = e.nextElement();
            //System.err.println("addPackages: "+key);
            Object value = packs.get(key);
            packages.put(key, value);
        }
    }

    public void findPackages(Properties registry) throws IOException {
        String paths = registry.getProperty("python.packages.paths", 
            "java.class.path,sun.boot.class.path");
        String directories = registry.getProperty("python.packages.directories", 
            "java.ext.dirs");
        
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
	}


	public PyJavaPackage doAddPackage(String name, String classes) {
	    //System.out.println("add package: "+name);
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
			p = new PyJavaPackage(firstName);
		    topLevelPackages.__setitem__(firstName, p);
		}
		PyJavaPackage ret = p;
		if (lastName != null) ret = p.addPackage(lastName);
		ret._unparsedAll = classes;
		return ret;
	}


    public void addSysPackages() {
        //PySystemState ss = Py.getSystemState();
        
        try {
            cachedir = new File(PySystemState.prefix, "pkgcache").getCanonicalPath();
            loadIndex();
            findPackages(PySystemState.registry);
            saveIndex();

            Hashtable packs = packages;

            for (Enumeration e = packs.keys() ; e.hasMoreElements() ;) {
                String key = (String)e.nextElement();
                String value = (String)packs.get(key);
                //System.err.println("add package: "+key);
                doAddPackage(key, value);
            }
        } catch (IOException ioe) {
            ;
        }
    }

    public void addJarDir(String jdir) throws IOException {
        File file = new File(jdir);
        if (!file.isDirectory()) return;
        String[] files = file.list();
        for(int i=0; i<files.length; i++) {
            String entry = files[i];
            if (entry.endsWith(".jar") || entry.endsWith(".zip")) {
                addJar(new File(jdir, entry));
            }
        }
    }

    public void addJarPath(String path) throws IOException {
		StringTokenizer tok = new StringTokenizer(path, java.io.File.pathSeparator);
		while  (tok.hasMoreTokens())  {
			String entry = tok.nextToken();
			addJarDir(entry);
		}
	}
	
	public void addClassPath(String path) throws IOException {
		StringTokenizer tok = new StringTokenizer(path, java.io.File.pathSeparator);
		while  (tok.hasMoreTokens())  {
			String entry = tok.nextToken().trim();
			if (entry.endsWith(".jar") || entry.endsWith(".zip")) {
			    addJar(new File(entry));
			} else {
			    addDirectory(new File(entry));
			}
		}
	}
	public void loadIndex() throws IOException {
	    indexModified = false;

	    File indexFile = new File(cachedir, "index.cache");
	    jarfiles = new Hashtable();
	    if (!indexFile.exists()) return;

	    DataInputStream istream = new DataInputStream(
              new BufferedInputStream(new FileInputStream(indexFile)));
	    try {
	        while (true) {
	            String jarfile = istream.readUTF();
	            JarEntry je = new JarEntry();
	            je.cachefile = istream.readUTF();
	            je.mtime = istream.readLong();
	            jarfiles.put(jarfile, je);
	        }
	    } catch (EOFException eof) {
	        ;
	    }
	    istream.close();
	}

	public void saveIndex() throws IOException {
	    if (!indexModified) return;
	    indexModified = false;

        if (Options.verbosePackageCache) {
            System.err.println("packageCache: writing modified index file");
        }

	    File indexFile = new File(cachedir, "index.cache");
	    DataOutputStream ostream = new DataOutputStream(
              new BufferedOutputStream(new FileOutputStream(indexFile)));
        for (Enumeration e = jarfiles.keys() ; e.hasMoreElements() ;) {
            String jarfile = (String)e.nextElement();
            JarEntry je = (JarEntry)jarfiles.get(jarfile);
            ostream.writeUTF(jarfile);
            ostream.writeUTF(je.cachefile);
            ostream.writeLong(je.mtime);
        }
        ostream.close();
	}

    /*public static void main(String[] args) throws IOException {
        System.out.println("start");
        long t0 = System.currentTimeMillis();
        loadIndex();
        addJarPath(System.getProperty("java.ext.dirs"));
        addJar(new File("d:\\jdk1.2\\jre\\lib\\rt.jar"));

        //Hashtable packages = getPackages(new File("d:\\jdk1.2\\jre\\lib\\rt.jar")); //loadCacheIndex(new File(new File(cachedir), "index").toString());
        saveIndex();
        addSysPackages();
        long t1 = System.currentTimeMillis();

        //System.out.println("packages: "+packages);
        System.out.println("jarfiles: "+jarfiles);


        System.out.println("end: "+(t1-t0)/1000.);
        //System.out.println("packages: "+packages);
    }*/
}