// Copyright (c) Corporation for National Research Initiatives
// Copyright 2000 Samuele Pedroni

package org.python.core.packagecache;

import org.python.core.Options;
import org.python.core.PyJavaPackage;
import org.python.core.util.FileUtil;
import org.python.util.Generic;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Abstract package manager that gathers info about statically known classes from a set of jars and
 * the Java runtime. This info can be cached, eventually. Off-the-shelf this class offers a local
 * file-system based cache implementation.
 */
public abstract class CachedJarsPackageManager extends PackageManager {

    protected static Logger logger = Logger.getLogger("org.python.import");

    /**
     * Message log method - hook. This default implementation does nothing.
     *
     * @param msg message template (see java.text.MessageFormat)
     * @param params parameters to insert
     */
    protected void message(String msg, Object... params) {}

    /**
     * Warning log method - hook. This default implementation does nothing.
     *
     * @param msg message template (see java.text.MessageFormat)
     * @param params parameters to insert
     */
    protected void warning(String msg, Object... params) {}

    /**
     * Comment log method - hook. This default implementation does nothing.
     *
     * @param msg message template (see java.text.MessageFormat)
     * @param params parameters to insert
     */
    protected void comment(String msg, Object... params) {}

    /**
     * Debug log method - hook. This default implementation does nothing.
     *
     * @param msg message template (see java.text.MessageFormat)
     * @param params parameters to insert
     */
    protected void debug(String msg, Object... params) {}

    /**
     * Filter class/pkg by name helper method - hook. The default implementation is used by
     * {@link #addJarToPackages} in order to filter out classes whose name contains '$' (e.g. inner
     * classes). Should be used or overridden by derived classes too. Also to be used in
     * {@link #doDir}.
     *
     * @param name class/pkg name
     * @param pkg if true, name refers to a pkg
     * @return {@code true} if name should be filtered out
     */
    protected boolean filterByName(String name, boolean pkg) {
        return name.indexOf('$') != -1;
    }

    /**
     * Filter class by access perms helper method - hook. The default implementation is used by
     * {@link #addJarToPackages} in order to filter out non-public classes. Should be used or
     * overridden by derived classes too. Also to be used in {@link #doDir}. Access permissions can
     * be read with {@link #checkAccess}.
     *
     * @param name class name
     * @param acc class access permissions as int
     * @return {@code true} if name should be filtered out
     */
    protected boolean filterByAccess(String name, int acc) {
        return (acc & Modifier.PUBLIC) != Modifier.PUBLIC;
    }

    /**
     * Set {@code true} whenever the cache index is modified (needs ultimately to be re-written).
     */
    private boolean indexModified;

    /**
     * Map from some source of class definitions to the name of a file used to cache lists of
     * classes in that source. {@code null} if cache is not operating. (This source must have a time
     * last modified so we may check that the cache is valid).
     */
    private Map<String, JarXEntry> index;

    /**
     * Process one entry from a JAR/ZIP, and if the entry is a (qualifying) Java class, add its name
     * to that package's entry in the map passed in. A class is added only if
     * {@link #filterByName(String, boolean)} returns {@code true}. We add it to the accessible or
     * inaccessible list according to whether {@link #filterByAccess(String, int)} returns
     * {@code true} for the name and permissions of that class.
     *
     * @param zipPackages map (to update) from package name to class names by accessibility
     * @param entry possibly representing a class
     * @param zip the JAR or ZIP to which the entry belongs
     * @throws IOException
     */
    private void addZipEntry(Map<String, ClassList> zipPackages, ZipEntry entry, ZipInputStream zip)
            throws IOException {

        String name = entry.getName();
        // System.err.println("entry: "+name);
        if (name.endsWith(".class")) {

            // Split off the bare class name
            char sep = '/';
            int slash = name.lastIndexOf(sep);
            if (slash == -1) {
                if ((slash = name.lastIndexOf('\\')) >= 0) {
                    sep = '\\'; // Shouldn't be necessary according to standards, but is.
                }
            }
            String className = name.substring(slash + 1, name.length() - 6);

            // Check acceptable name: in practice, this is used to ignore inner classes.
            if (!filterByName(className, false)) {
                // File this class by name against the package
                String packageName = slash == -1 ? "" : name.substring(0, slash).replace(sep, '.');
                ClassList classes = zipPackages.get(packageName);

                if (classes == null) {
                    // It wasn't in the map so add it
                    classes = new ClassList();
                    zipPackages.put(packageName, classes);
                }

                // Put the class on the right list
                int access = checkAccess(zip);
                if ((access != -1) && !filterByAccess(name, access)) {
                    classes.accessible.add(className);
                } else {
                    classes.inaccessible.add(className);
                }
            }
        }
    }

    /** Used when representing the classes in a particular package. */
    private static class ClassList {

        /** Class names of accessible classes */
        List<String> accessible = new ArrayList<String>();
        /** Class names of inaccessible classes */
        List<String> inaccessible = new ArrayList<String>();

        /** Retrieve the two lists in "cache file" format {@code A,B,C[@D,E]}. */
        @Override
        public String toString() {
            StringBuilder buf = new StringBuilder();
            appendList(buf, accessible);
            if (inaccessible.size() > 0) {
                buf.append('@');
                appendList(buf, inaccessible);
            }
            return buf.toString();
        }

        private static void appendList(StringBuilder buf, List<String> names) {
            if (names.size() > 0) {
                for (String n : names) {
                    buf.append(n).append(',');
                }
                // Deal with surplus comma
                buf.deleteCharAt(buf.length() - 1);
            }
        }
    }

    /**
     * Detect all of the packages in an open JAR/ZIP file, that contain any classes for which
     * {@link #filterByName(String, boolean)} returns {@code true}. For each such package, list the
     * (surviving) classes in two lists: accessible and inaccessible, which is judged according to
     * {@link #filterByAccess(String, int)}. The returned map is from package to these two lists,
     * now comma-separated lists, with an '@' between them.
     *
     * @param jarin an open JAR/ZIP file
     * @return map from package name to comma/@-separated list of class names
     * @throws IOException
     */
    private Map<String, String> getZipPackages(InputStream jarin) throws IOException {

        Map<String, ClassList> zipPackages = Generic.map();
        ZipInputStream zip = new ZipInputStream(jarin);
        ZipEntry entry;

        while ((entry = zip.getNextEntry()) != null) {
            addZipEntry(zipPackages, entry, zip);
            zip.closeEntry();
        }

        // Turn each ClassList into a comma/@-separated String
        Map<String, String> transformed = Generic.map();
        for (Entry<String, ClassList> kv : zipPackages.entrySet()) {
            transformed.put(kv.getKey(), kv.getValue().toString());
        }

        return transformed;
    }

    /**
     * Gathers classes info from jar specified by a URL. Eventually just using previously cached
     * info. Eventually updated info is not cached. Persistent cache storage access goes through
     * inOpenCacheFile() and outCreateCacheFile().
     */
    public void addJarToPackages(java.net.URL jarurl) {
        addJarToPackages(jarurl, null, false);
    }

    /**
     * Gathers classes info from jar specified by URL. Eventually just using previously cached info.
     * Eventually updated info is (re-)cached if param cache is true. Persistent cache storage
     * access goes through inOpenCacheFile() and outCreateCacheFile().
     */
    public void addJarToPackages(URL jarurl, boolean cache) {
        addJarToPackages(jarurl, null, cache);
    }

    /**
     * Gathers classes info from jar specified by File jarfile. Eventually just using previously
     * cached info. Eventually updated info is not cached. Persistent cache storage access goes
     * through inOpenCacheFile() and outCreateCacheFile().
     */
    public void addJarToPackages(File jarfile) {
        addJarToPackages(null, jarfile, false);
    }

    /**
     * Gathers package and class lists from a jar specified by a {@code File}. Eventually just using
     * previously cached info. Eventually updated info is (re-)cached if param cache is true.
     * Persistent cache storage access goes through inOpenCacheFile() and outCreateCacheFile().
     */
    public void addJarToPackages(File jarfile, boolean cache) {
        addJarToPackages(null, jarfile, cache);
    }

    /**
     * Create (or ensure we have) a {@link PyJavaPackage}, descending from
     * {@link PackageManager#topLevelPackage} in this {@link PackageManager} instance, for each
     * package in a jar specified by a file or URL. Ensure that the class list in each package is
     * updated with the classes this JAR supplies to it.
     *
     * The information concerning packages in the JAR and the classes they contain, may be read from
     * from a previously cached account of the JAR, if the last-modified time of the JAR matches a
     * cached value. If it is not read from a cache, it will be obtained by inspecting the JAR, and
     * a new cache will be written (if requested).
     *
     * Access to persistent cache storage goes through {@link #inOpenCacheFile(String)} and
     * {@link #outCreateCacheFile(JarXEntry, boolean)}.
     *
     * @param jarurl identifying the JAR if {@code jarfile} is {@code null}
     * @param jarfile identifying the JAR
     * @param writeCache re-write the cache if it was out of date (and caching is active).
     */
    private void addJarToPackages(URL jarurl, File jarfile, boolean writeCache) {
        try {
            // We try to read the cache (for this jar) if caching is in operation.
            boolean readCache = this.index != null;
            // We write a cache if caching is in operation AND writing has been requested.
            writeCache &= readCache;

            URLConnection jarconn = null;
            boolean localfile = true;

            // If a local JAR file was not given directly in jarfile, try to find one from the URL.
            if (jarfile == null) {
                // We were not given a File, so the URL must be reliable (but may not be a file)
                jarconn = jarurl.openConnection();
                // The following comment may be out of date. Also 2 reasons or just the bug?
                /*
                 * This is necessary because 'file:' url-connections always return 0 through
                 * getLastModified (bug?). And in order to handle localfiles (from urls too)
                 * uniformly.
                 */
                if (jarconn.getURL().getProtocol().equals("file")) {
                    // Although given as a URL, this *is* a file.
                    String jarfilename = jarurl.getFile();
                    jarfilename = jarfilename.replace('/', File.separatorChar);
                    jarfile = new File(jarfilename);
                } else {
                    // We can't find a local file
                    localfile = false;
                }
            }

            if (localfile && !jarfile.exists()) {
                // Local JAR file claimed or deduced does not exist. Silently ignore.
                return;
            }

            // The classes to discover are in a local JAR file. They go in this map:
            Map<String, String> zipPackages = null;

            long mtime = 0;
            String jarcanon = null;
            JarXEntry entry = null;
            boolean brandNew = false;

            if (readCache) {

                // Get the name and last modified time of the actual JAR on disk.
                if (localfile) {
                    mtime = jarfile.lastModified();
                    jarcanon = jarfile.getCanonicalPath();
                } else {
                    mtime = jarconn.getLastModified();
                    jarcanon = jarurl.toString();
                }

                // The canonical name is our key in the (in memory) index to the cache file.
                entry = this.index.get(jarcanon);

                if (writeCache && (entry == null || !(new File(entry.cachefile).exists()))) {
                    // We intend to use a cache but there is no valid existing file.
                    comment("processing new jar ''{0}''", jarcanon);

                    // Create a base-name for the cache file
                    String jarname;
                    if (localfile) {
                        jarname = jarfile.getName();
                    } else {
                        jarname = jarurl.getFile();
                        int slash = jarname.lastIndexOf('/');
                        if (slash != -1) {
                            jarname = jarname.substring(slash + 1);
                        }
                    }
                    jarname = jarname.substring(0, jarname.length() - 4);

                    // Create a new (or replacement) index entry. Remember to create the file.
                    entry = new JarXEntry(jarname);
                    this.index.put(jarcanon, entry);
                    brandNew = true;
                }

                // If the source has a date and the cache matches, create the map we need from it.
                if (entry != null && mtime != 0 && entry.mtime == mtime) {
                    zipPackages = readCacheFile(entry, jarcanon);
                }
            }

            /*
             * At this point, we will have the package map from the cache if it was valid, and a
             * cache is in use generally in this package manager.
             */

            if (zipPackages == null) {
                // We'll have to read the actual JAR file

                if (writeCache) {
                    // Update the index entry for the cache file we shall eventually write.
                    this.indexModified = true;
                    if (entry.mtime != 0) {
                        comment("processing modified jar ''{0}''", jarcanon);
                    }
                    entry.mtime = mtime;
                }

                // Create the package-to-class mapping from whatever stream.
                InputStream jarin = null;
                try {
                    if (jarconn == null) {
                        jarin = new BufferedInputStream(new FileInputStream(jarfile));
                    } else {
                        // We were given a URL originally so use that.
                        jarin = jarconn.getInputStream();
                    }

                    zipPackages = getZipPackages(jarin);

                } finally {
                    if (jarin != null) {
                        jarin.close();
                    }
                }

                if (writeCache) {
                    // Write what we created out to a cache file (new or updated)
                    writeCacheFile(entry, jarcanon, zipPackages, brandNew);
                }
            }

            /*
             * We now have the package map we need (from a cache or by construction). Now create or
             * update corresponding package objects with the discovered classes (named, but not as
             * PyObjects).
             */
            addPackages(zipPackages, jarcanon);

        } catch (IOException ioe) {
            // Skip the bad JAR with a message
            warning("skipping bad jar ''{0}''", (jarfile != null ? jarfile : jarurl).toString());
        }
    }

    /**
     * From a map of package name to comma/@-separated list of classes therein, relating to a
     * particular JAR file, create a {@link PyJavaPackage} for each package.
     *
     * @param packageToClasses source of mappings
     * @param jarfile becomes the __file__ attribute of the {@link PyJavaPackage}
     */
    private void addPackages(Map<String, String> packageToClasses, String jarfile) {
        for (Entry<String, String> entry : packageToClasses.entrySet()) {
            String pkg = entry.getKey();
            String classes = entry.getValue();

            int idx = classes.indexOf('@');
            if (idx >= 0 && Options.respectJavaAccessibility) {
                classes = classes.substring(0, idx);
            }

            makeJavaPackage(pkg, classes, jarfile);
        }
    }

    /**
     * Read in cache file storing package info for a single jar. Return null and delete this cache
     * file if it is invalid.
     */
    @SuppressWarnings("empty-statement")
    private Map<String, String> readCacheFile(JarXEntry entry, String jarcanon) {
        String cachefile = entry.cachefile;
        long mtime = entry.mtime;

        debug("reading cache of ''{0}''", jarcanon);

        DataInputStream istream = null;
        try {
            istream = inOpenCacheFile(cachefile);
            String old_jarcanon = istream.readUTF();
            long old_mtime = istream.readLong();
            if ((!old_jarcanon.equals(jarcanon)) || (old_mtime != mtime)) {
                comment("invalid cache file: {0} for new:{1}({3}), old:{2}({4})", cachefile, jarcanon,
                        old_jarcanon, mtime, old_mtime);
                deleteCacheFile(cachefile);
                return null;
            }
            Map<String, String> packs = Generic.map();
            try {
                while (true) {
                    String packageName = istream.readUTF();
                    String classes = istream.readUTF();
                    // XXX: Handle multiple chunks of classes and concatenate them
                    // together. Multiple chunks were added in 2.5.2 (for #1595) in this
                    // way to maintain compatibility with the pre 2.5.2 format. In the
                    // future we should consider changing the cache format to prepend a
                    // count of chunks to avoid this check
                    if (packs.containsKey(packageName)) {
                        classes = packs.get(packageName) + classes;
                    }
                    packs.put(packageName, classes);
                }
            } catch (EOFException eof) {
                // ignore
            }

            return packs;
        } catch (IOException ioe) {
            // if (cachefile.exists()) cachefile.delete();
            return null;
        } finally {
            if (istream != null) {
                try {
                    istream.close();
                } catch (IOException ignore) {
                    // ignore
                }
            }
        }
    }

    /**
     * Write a cache file storing package info for a single JAR. *
     *
     * @param entry in {@link #index} corresponding to the JAR
     * @param jarcanon canonical name of the JAR (used as key into {@link #index})
     * @param zipPackages a map from package name to class names for that pacjkage in the JAR
     * @param brandNew if the cache file must be created (vs. being re-written)
     */
    private void writeCacheFile(JarXEntry entry, String jarcanon, Map<String, String> zipPackages,
            boolean brandNew) {
        DataOutputStream ostream = null;
        try {
            ostream = outCreateCacheFile(entry, brandNew);
            ostream.writeUTF(jarcanon);
            ostream.writeLong(entry.mtime);
            comment("rewriting cache for ''{0}''", jarcanon);

            for (Entry<String, String> kv : zipPackages.entrySet()) {
                String classes = kv.getValue();
                // Make sure each package is not larger than 64k
                for (String part : splitString(classes, 65535)) {
                    // For each chunk, write the package name followed by the classes.
                    ostream.writeUTF(kv.getKey());
                    ostream.writeUTF(part);
                }
            }
        } catch (IOException ioe) {
            warning("failed to write cache for ''{0}'' ({1})", jarcanon, ioe.getMessage());
        } finally {
            if (ostream != null) {
                try {
                    ostream.close();
                } catch (IOException ignore) {
                    // ignore
                }
            }
        }
    }

    /** Scan a Java module, creating package objects. */
    protected void addModuleToPackages(Path modulePath) {
        try {
            comment("reading packages from ''{0}''", modulePath);
            Map<String, String> packages = getModularPackages(modulePath);
            addPackages(packages, modulePath.toUri().toString());
        } catch (IOException ioe) {
            warning("skipping bad module ''{0}'' ({1})", modulePath, ioe.getMessage());
        }
    }

    /**
     * Detect all of the packages in a single module, that contain any classes for which
     * {@link #filterByName(String, boolean)} returns {@code true}. For each such package, list the
     * (surviving) classes in two lists: accessible and inaccessible, which is judged according to
     * {@link #filterByAccess(String, int)}. The returned map is from package to these two lists,
     * now comma-separated lists, with an '@' between them.
     *
     * @param modulePath up to and including the name of the module
     * @return map from packages to classes
     * @throws IOException
     */
    private Map<String, String> getModularPackages(Path modulePath) throws IOException {

        final int M = modulePath.getNameCount();
        final Map<String, ClassList> modPackages = Generic.map();

        FileVisitor<Path> visitor = new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                    throws IOException {
                //System.out.println("    visitFile:" + file);

                // file starts with modulePath, then has package & class: / ... /[name].class
                int n = file.getNameCount();
                // Apply name and access tests and conditionally add to modPackages
                String fileName = file.getFileName().toString();
                if (fileName.endsWith(".class") && n > M + 1) {
                    // Split off the bare class name
                    String className = fileName.substring(0, fileName.length() - 6);

                    // Check acceptable name: in practice, this is used to ignore inner classes.
                    if (!filterByName(className, false)) {
                        // Parts M to n-1 define the package of this class
                        String packageName = file.subpath(M, n - 1).toString().replace('/', '.');
                        ClassList classes = modPackages.get(packageName);

                        if (classes == null) {
                            // It wasn't in the map so add it
                            classes = new ClassList();
                            modPackages.put(packageName, classes);
                        }

                        // Put the class on the accessible or inaccessible list
                        try (InputStream c = Files.newInputStream(file, StandardOpenOption.READ)) {
                            int access = checkAccess(c);
                            if ((access != -1) && !filterByAccess(fileName, access)) {
                                classes.accessible.add(className);
                            } else {
                                classes.inaccessible.add(className);
                            }
                        }
                    }
                }
                return FileVisitResult.CONTINUE;
            }
        };

        Files.walkFileTree(modulePath, visitor);

        // Turn each ClassList into a comma/@-separated String
        Map<String, String> transformed = Generic.map();
        for (Entry<String, ClassList> kv : modPackages.entrySet()) {
            transformed.put(kv.getKey(), kv.getValue().toString());
        }

        return transformed;
    }

    /**
     * Split up a string into several chunks based on a certain size
     *
     * The writeCacheFile method will use the writeUTF method on a DataOutputStream which only
     * allows writing 64k chunks, so use this utility method to split it up
     *
     * @param str - The string to split up into chunks
     * @param maxLength - The max size a string should be
     * @return - An array of strings, each of which will not be larger than maxLength
     */
    protected static String[] splitString(String str, int maxLength) {
        if (str == null) {
            return null;
        }

        int len = str.length();
        if (len <= maxLength) {
            return new String[] {str};
        }

        int chunkCount = (int) Math.ceil((float) len / maxLength);
        String[] chunks = new String[chunkCount];
        for (int i = 0; i < chunkCount; i++) {
            chunks[i] = str.substring(i * maxLength, Math.min(i * maxLength + maxLength, len));
        }
        return chunks;
    }

    /**
     * Initialise the cache by reading the index from storage, through {@link #inOpenIndex()}, or by
     * creating a new empty one.
     */
    protected void initCache() {
        this.indexModified = false;
        this.index = Generic.map();

        DataInputStream istream = null;
        try {
            istream = inOpenIndex();
            if (istream == null) {
                return;
            }

            try {
                while (true) {
                    String jarcanon = istream.readUTF();
                    String cachefile = istream.readUTF();
                    long mtime = istream.readLong();
                    this.index.put(jarcanon, new JarXEntry(cachefile, mtime));
                }
            } catch (EOFException eof) {
                // ignore
            }
        } catch (IOException ioe) {
            warning("invalid index file");
        } finally {
            if (istream != null) {
                try {
                    istream.close();
                } catch (IOException ignore) {
                    // ignore
                }
            }
        }
    }

    /**
     * Write back cache <b>index</b>. The index is a mapping from each source of class definitions
     * to the file where the cache for that source is held. This list is accessed through
     * outOpenIndex().
     */
    public void saveCache() {

        if (index == null || !indexModified) {
            return;
        }

        indexModified = false;

        comment("writing modified index file");

        DataOutputStream ostream = null;
        try {
            ostream = outOpenIndex();
            for (Entry<String, JarXEntry> entry : index.entrySet()) {
                String jarcanon = entry.getKey();
                JarXEntry xentry = entry.getValue();
                ostream.writeUTF(jarcanon);
                ostream.writeUTF(xentry.cachefile);
                ostream.writeLong(xentry.mtime);
            }
        } catch (IOException ioe) {
            warning("failed to write index file ({0})", ioe.getMessage());
        } finally {
            if (ostream != null) {
                try {
                    ostream.close();
                } catch (IOException ignore) {
                    // ignore
                }
            }
        }
    }

    // hooks for changing cache storage

    /**
     * Class of object used to represent a cache file and last modification time, internally and to
     * {@link CachedJarsPackageManager#outCreateCacheFile}. When caching, a {@code JarXEntry} is
     * created for each JAR processed for classes, and corresponds to a file in the package cache
     * directory. The name is based on the name of the JAR.
     */
    public static class JarXEntry extends Object {

        /** Specifies the actual cache file once that is created or opened. */
        public String cachefile;

        public long mtime;

        public JarXEntry(String cachefile) {
            this.cachefile = cachefile;
        }

        public JarXEntry(String cachefile, long mtime) {
            this.cachefile = cachefile;
            this.mtime = mtime;
        }

    }

    /**
     * Open cache index for reading from persistent storage &ndash; hook. Must Return null if this
     * is absent. This default implementation is part of the off-the-shelf local file-system cache
     * implementation. Can be overridden.
     */
    protected DataInputStream inOpenIndex() throws IOException {
        File indexFile = new File(this.cachedir, "packages.idx");
        if (!indexFile.exists()) {
            return null;
        } else {
            FileInputStream istream = new FileInputStream(indexFile);
            return new DataInputStream(new BufferedInputStream(istream));
        }
    }

    /**
     * Open cache index for writing back to persistent storage &ndash; hook. This default
     * implementation is part of the off-the-shelf local file-system cache implementation. Can be
     * overridden.
     */
    protected DataOutputStream outOpenIndex() throws IOException {
        File indexFile = FileUtil.makePrivateRW(new File(this.cachedir, "packages.idx"));
        FileOutputStream ostream = new FileOutputStream(indexFile);
        return new DataOutputStream(new BufferedOutputStream(ostream));
    }

    /**
     * Open a particular cache file for reading from persistent storage. This default implementation
     * is part of the off-the-shelf local file-system cache implementation. Can be overridden.
     */
    protected DataInputStream inOpenCacheFile(String cachefile) throws IOException {
        return new DataInputStream(new BufferedInputStream(new FileInputStream(cachefile)));
    }

    /**
     * Delete (invalidated) cache file from persistent storage - hook. This default implementation
     * is part of the off-the-shelf local file-system cache implementation. Can be overridden.
     */
    protected void deleteCacheFile(String cachefile) {
        new File(cachefile).delete();
    }

    /**
     * Create/open cache file for rewriting back to persistent storage &ndash; hook. If
     * {@code create} is {@code false}, the cache file is supposed to exist at
     * {@code entry.cachefile} and will be opened for rewriting. If {@code create} is {@code true},
     * {@code entry.cachefile} is the base name (e.g. JAR or module name) for a cache to be created,
     * and the full name will be put in {@code entry.cachefile} on exit. This default implementation
     * is part of the off-the-shelf local file-system cache implementation. It may be overridden to
     * provide a different cache medium and use of {@code entry.cachefile}.
     *
     * @param entry cache file description
     * @param create new or use existing file named in {@code entry.cachefile}
     * @return stream on which to represent the package to class-list textually
     * @throws IOException
     */
    protected DataOutputStream outCreateCacheFile(JarXEntry entry, boolean create)
            throws IOException {

        File file;

        if (create) {
            // Create a new cache file with a name based on the initial value
            String jarname = entry.cachefile;
            file = new File(this.cachedir, jarname + ".pkc");
            for (int index = 1; file.exists(); index++) {
                // That name is in use: make up another one.
                file = new File(this.cachedir, jarname + "$" + index + ".pkc");
            }
            file = FileUtil.makePrivateRW(file);
            entry.cachefile = file.getCanonicalPath();

        } else {
            // Use an existing cache file named in the entry
            file = new File(entry.cachefile);
        }

        return new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
    }

    /** Directory in which cache files are stored. */
    private File cachedir;

    /**
     * Initialize off-the-shelf (default) local file-system cache implementation. Must be called
     * before {@link #initCache}. cachedir is the cache repository directory, this is eventually
     * created. Returns true if dir works.
     */
    protected boolean useCacheDir(File cachedir) {
        try {
            if (cachedir != null) {
                if (cachedir.isDirectory() || cachedir.mkdirs()) {
                    this.cachedir = cachedir;
                    return true;
                } else {
                    warning("failed to create cache dir ''{0}''", cachedir);
                }
            }
        } catch (AccessControlException ace) {
            warning("Not permitted to access cache ''{0}'' ({1})", cachedir, ace.getMessage());
        }
        return false;
    }

}
