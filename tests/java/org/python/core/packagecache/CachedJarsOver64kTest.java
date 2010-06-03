package org.python.core.packagecache;

import java.io.File;

import org.python.core.PyJavaPackage;
import org.python.core.PyList;
import org.python.core.packagecache.CachedJarsPackageManager;

import junit.framework.TestCase;

public class CachedJarsOver64kTest extends TestCase {

    private TestCachePackageManager packageManager = null;

    private File jarFile = null;

    @Override
    public void setUp() {
        // Find the jar to use
        packageManager = new TestCachePackageManager(new File(System
                                                              .getProperty("java.io.tmpdir")));
        File cwd = new File(System.getProperty("python.test.source.dir"),
                            getClass().getPackage().getName().replace(".", "/"));
        jarFile = new File(cwd, "vim25-small.jar");
    }

    public void testJarOver64k() {
        assertTrue(jarFile.exists());
        packageManager.addJarToPackages(jarFile, true);
        assertFalse(packageManager.failed);
    }

    private class TestCachePackageManager extends CachedJarsPackageManager {

        public boolean failed;

        public TestCachePackageManager(File cachedir) {
            if (useCacheDir(cachedir)){
                initCache();
            }
        }

        @Override
        protected void warning(String msg){
            failed = true;
        }

        @Override
        public void addDirectory(File dir) {}
        @Override
        public void addJar(String jarfile, boolean cache) {}
        @Override
        public void addJarDir(String dir, boolean cache) {}
        @Override
        public PyList doDir(PyJavaPackage jpkg, boolean instantiate, boolean exclpkgs) {
            return null;
        }
        @Override
        public Class<?> findClass(String pkg, String name, String reason) { return null; }
        @Override
        public boolean packageExists(String pkg, String name) { return false; }
    }
}
