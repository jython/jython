// Copyright (c) Corporation for National Research Initiatives
// Copyright 2000 Samuele Pedroni

package org.python.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.StringTokenizer;
import java.util.zip.ZipEntry;

import org.python.core.util.RelativeFile;

public class SyspathJavaLoader extends ClassLoader {

    private static final char SLASH_CHAR = '/';

    public SyspathJavaLoader(ClassLoader parent) {
    	super(parent);
    }


    /**
     * Returns a byte[] with the contents read from an InputStream.
     *
     * The stream is closed after reading the bytes.
     *
     * @param input The input stream
     * @param size The number of bytes to read
     *
     * @return an array of byte[size] with the contents read
     * */
    private byte[] getBytesFromInputStream(InputStream input, int size) {
    	try {
	    	byte[] buffer = new byte[size];
	        int nread = 0;
	        while(nread < size) {
	            nread += input.read(buffer, nread, size - nread);
	        }
	        return buffer;
    	} catch (IOException exc) {
            return null;
    	} finally {
            try {
                input.close();
            } catch (IOException e) {
            	// Nothing to do
            }
    	}
    }

    private byte[] getBytesFromDir(String dir, String name) {
    	try {
    		File file = getFile(dir, name);
	        if (file == null) {
	            return null;
	        }
	        return getBytesFromInputStream(new FileInputStream(file), (int)file.length());
        } catch (FileNotFoundException e) {
            return null;
        } catch(SecurityException e) {
            return null;
        }

    }

    private byte[] getBytesFromArchive(SyspathArchive archive, String name) {
        String entryname = name.replace('.', SLASH_CHAR) + ".class";
        ZipEntry ze = archive.getEntry(entryname);
        if (ze == null) {
            return null;
        }
        try {
			return getBytesFromInputStream(archive.getInputStream(ze),
					                       (int)ze.getSize());
		} catch (IOException e) {
			return null;
		}
    }

    protected Package definePackageForClass(String name) {
	    int lastDotIndex = name.lastIndexOf('.');
	    if (lastDotIndex < 0) {
	    	return null;
	    }
        String pkgname = name.substring(0, lastDotIndex);
        Package pkg = getPackage(pkgname);
        if (pkg == null) {
            pkg = definePackage(pkgname, null, null, null, null, null, null, null);
        }
        return pkg;
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
    	PySystemState sys = Py.getSystemState();
    	ClassLoader sysClassLoader = sys.getClassLoader();
    	if (sysClassLoader != null) {
    		// sys.classLoader overrides this class loader!
    		return sysClassLoader.loadClass(name);
    	}
        // Search the sys.path for a .class file matching the named class.
    	PyList path = sys.path;
        for (int i = 0; i < path.__len__(); i++) {
        	byte[] buffer;
            PyObject entry = replacePathItem(sys, i, path);
            if (entry instanceof SyspathArchive) {
                SyspathArchive archive = (SyspathArchive) entry;
                buffer = getBytesFromArchive(archive, name);
            } else {
                String dir = imp.fileSystemDecode(entry, false);
                buffer = dir != null ? getBytesFromDir(dir, name) : null;
            }
            if (buffer != null) {
            	definePackageForClass(name);
            	return defineClass(name, buffer, 0, buffer.length);
            }
        }
        // couldn't find the .class file on sys.path
        throw new ClassNotFoundException(name);
    }

    @Override
    protected URL findResource(String res) {
    	PySystemState sys = Py.getSystemState();

        res = deslashResource(res);
        String entryRes = res;
        if (File.separatorChar != SLASH_CHAR) {
            res = res.replace(SLASH_CHAR, File.separatorChar);
            entryRes = entryRes.replace(File.separatorChar, SLASH_CHAR);
        }

        PyList path = sys.path;
        for (int i = 0; i < path.__len__(); i++) {
            PyObject entry = replacePathItem(sys, i, path);
            if (entry instanceof SyspathArchive) {
                SyspathArchive archive = (SyspathArchive) entry;
                ZipEntry ze = archive.getEntry(entryRes);
                if (ze != null) {
                	try {
						return new URL("jar:file:" + archive.asUriCompatibleString() + "!/" + entryRes);
					} catch (MalformedURLException e) {
						throw new RuntimeException(e);
					}
                }
                continue;
            }
            String dir = sys.getPath(Py.fileSystemDecode(entry));
            try {
				File resource = new File(dir, res);
				if (!resource.exists()) {
					continue;
				}
				return resource.toURI().toURL();
			} catch (MalformedURLException e) {
				throw new RuntimeException(e);
			}
        }
        return null;
    }

    @Override
    protected Enumeration<URL> findResources(String res)
        throws IOException
    {
        List<URL> resources = new ArrayList<URL>();

        PySystemState sys = Py.getSystemState();

        res = deslashResource(res);
        String entryRes = res;
        if (File.separatorChar != SLASH_CHAR) {
            res = res.replace(SLASH_CHAR, File.separatorChar);
            entryRes = entryRes.replace(File.separatorChar, SLASH_CHAR);
        }

        PyList path = sys.path;
        for (int i = 0; i < path.__len__(); i++) {
            PyObject entry = replacePathItem(sys, i, path);
            if (entry instanceof SyspathArchive) {
                SyspathArchive archive = (SyspathArchive) entry;
                ZipEntry ze = archive.getEntry(entryRes);
                if (ze != null) {
                    try {
                        resources.add(new URL("jar:file:" + archive.asUriCompatibleString() + "!/" + entryRes));
                    } catch (MalformedURLException e) {
                        throw new RuntimeException(e);
                    }
                }
                continue;
            }
            String dir = sys.getPath(Py.fileSystemDecode(entry));
            try {
                File resource = new File(dir, res);
                if (!resource.exists()) {
                    continue;
                }
                resources.add(resource.toURI().toURL());
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }
        return Collections.enumeration(resources);
    }

    static PyObject replacePathItem(PySystemState sys, int idx, PyList paths) {
        PyObject path = paths.__getitem__(idx);
        if (path instanceof SyspathArchive) {
            // already an archive
            return path;
        }

        try {
            // this has the side effect of adding the jar to the PackageManager during the
            // initialization of the SyspathArchive
            path = new SyspathArchive(sys.getPath(Py.fileSystemDecode(path)));
        } catch (Exception e) {
            return path;
        }
        paths.__setitem__(idx, path);
        return path;
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

    private static String deslashResource(String res) {
        if (!res.isEmpty() && res.charAt(0) == SLASH_CHAR) {
            res = res.substring(1);
        }
        return res;
    }

}
