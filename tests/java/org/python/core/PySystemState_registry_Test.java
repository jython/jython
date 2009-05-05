package org.python.core;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Properties;

import junit.framework.TestCase;

/**
 * Tests for property handling (pre, post and registry files)
 */
public class PySystemState_registry_Test extends TestCase {

    /** The name of the installed registry file */
    private static final String REGISTRY = "registry";

    /** The name of the user registry file */
    private static final String USER_REGISTRY = ".jython";
    
    /** name of the dist (root) dir */
    private static final String DIST = "dist";

    /** property name */
    private final static String FIRST_PROP = "first.test.property";

    /** property name */
    private final static String SECOND_PROP = "second.test.property";

    /** property name */
    private final static String USER_HOME = "user.home";

    /** property name */
    private static final String PYTHON_HOME = "python.home";

    /** property value */
    private final static String ANY = "any";

    /** property value */
    private final static String PRE = "pre";

    /** property value */
    private final static String POST = "post";

    /** property value */
    private final static String INSTALLED = "installed";

    /** property value */
    private final static String USER = "user";

    private String _originalUserHome;

    private File _root;

    private String _originalRegistryContent;

    private Properties _originalRegistry;

    private File _tmpDir;

    @Override
    protected void setUp() throws Exception {
        findRoot();
        storeOriginals();
        uninitialize();
    }

    @Override
    protected void tearDown() throws Exception {
        restoreOriginals();
        cleanup();
    }

    /**
     * make sure post properties override pre properties
     * 
     * @throws Exception
     */
    public void testInitialize_PrePostProperties() throws Exception {
        Properties preProps = createPreProperties();
        preProps.setProperty(FIRST_PROP, PRE);
        preProps.setProperty(SECOND_PROP, PRE);
        Properties postProps = createPostProperties();
        postProps.setProperty(SECOND_PROP, POST);
        PySystemState.initialize(preProps, postProps);
        Properties registry = PySystemState.registry;
        String first = registry.getProperty(FIRST_PROP, ANY);
        String second = registry.getProperty(SECOND_PROP, ANY);
        assertEquals(PRE, first);
        assertEquals(POST, second);
    }

    /**
     * make sure a property from the user registry overrides the same property from the installed
     * registry
     * 
     * @throws Exception
     */
    public void testInitialize_Registry_User() throws Exception {
        // prepare both registry files
        String installedContent = FIRST_PROP.concat("=").concat(INSTALLED);
        appendToInstalledRegistry(installedContent);
        String userContent = FIRST_PROP.concat("=").concat(USER);
        addUserRegistry(userContent);
        // test with empty pre an post properties
        Properties preProps = createPreProperties();
        Properties postProps = createPostProperties();
        PySystemState.initialize(preProps, postProps);
        Properties registry = PySystemState.registry;
        String first = registry.getProperty(FIRST_PROP, ANY);
        assertEquals(USER, first);
    }

    /**
     * make sure a pre property overrides any registry properties with the same name
     * 
     * @throws Exception
     */
    public void testInitialize_Pre_Registries() throws Exception {
        // prepare both registry files
        String contentToAppend = FIRST_PROP.concat("=").concat(INSTALLED);
        appendToInstalledRegistry(contentToAppend);
        String userContent = FIRST_PROP.concat("=").concat(USER);
        addUserRegistry(userContent);
        // set same property on pre properties
        Properties preProps = createPreProperties();
        preProps.setProperty(FIRST_PROP, PRE);
        Properties postProps = createPostProperties();
        PySystemState.initialize(preProps, postProps);
        Properties registry = PySystemState.registry;
        String first = registry.getProperty(FIRST_PROP, ANY);
        assertEquals(PRE, first);
    }

    /**
     * make sure a post property overrides any registry properties with the same name, and the pre
     * property as well
     * 
     * @throws Exception
     */
    public void testInitialize_Post_Registries() throws Exception {
        // prepare both registry files
        String contentToAppend = FIRST_PROP.concat("=").concat(INSTALLED);
        appendToInstalledRegistry(contentToAppend);
        String userContent = FIRST_PROP.concat("=").concat(USER);
        addUserRegistry(userContent);
        // set same property on pre properties
        Properties preProps = createPreProperties();
        preProps.setProperty(FIRST_PROP, PRE);
        // set same property on post properties
        Properties postProps = createPostProperties();
        postProps.setProperty(FIRST_PROP, POST);
        PySystemState.initialize(preProps, postProps);
        Properties registry = PySystemState.registry;
        String first = registry.getProperty(FIRST_PROP, ANY);
        assertEquals(POST, first);
    }

    /**
     * determine the installation root (the /dist directory)
     * @throws Exception 
     */
    private void findRoot() throws Exception {
        Class<? extends PySystemState_registry_Test> thisClass = getClass();
        String classFileName = "/".concat(thisClass.getName().replace('.', '/')).concat(".class");
        URL url = thisClass.getResource(classFileName);
        assertNotNull(url);
        String path = URLDecoder.decode(url.getPath(), "UTF-8");
        assertTrue(path.endsWith(classFileName));
        String classesDirName = path.substring(0, path.length() - classFileName.length());
        File classesDir = new File(classesDirName);
        assertTrue(classesDir.exists());
        assertTrue(classesDir.isDirectory());
        _root = new File(classesDir.getParentFile().getParentFile(), DIST);
        assertTrue(_root.exists());
        assertTrue(_root.isDirectory());
    }

    /**
     * force a new initialization
     * <p>
     * set private static field PySystemState.initialized to false <br>
     * clear {@link PySystemState#registry}
     * 
     * @throws Exception
     */
    private void uninitialize() throws Exception {
        Field field = PySystemState.class.getDeclaredField("initialized");
        field.setAccessible(true);
        field.set(null, false);
        PySystemState.registry = null;
    }

    /**
     * keep track of original values, to be able to restore them in tearDown()
     * 
     * @throws Exception
     */
    private void storeOriginals() throws Exception {
        _originalRegistry = PySystemState.registry;
        _originalUserHome = System.getProperty(USER_HOME);
        File installedRegistry = new File(getRoot(), REGISTRY);
        assertTrue(installedRegistry.exists());
        _originalRegistryContent = readAll(installedRegistry);
    }

    /**
     * restore the original values
     * 
     * @throws Exception
     */
    private void restoreOriginals() throws Exception {
        PySystemState.registry = _originalRegistry;
        String originalUserHome = getOriginalUserHome();
        if (originalUserHome != null) {
            System.setProperty(USER_HOME, originalUserHome);
        }
        writeInstalledRegistry(getOriginalRegistryContent());
    }

    /**
     * overwrite the registry file in the installation root
     * 
     * @param content
     * @throws IOException
     */
    private void writeInstalledRegistry(String content) throws IOException {
        if (content != null && content.length() > 0) {
            File installedRegistry = new File(getRoot(), REGISTRY);
            assertTrue(installedRegistry.exists());
            write(installedRegistry, content);
        }
    }

    /**
     * append to the registry file in the installation root
     * 
     * @param contentToAppend
     * @throws IOException
     */
    private void appendToInstalledRegistry(String contentToAppend) throws IOException {
        if (contentToAppend != null && contentToAppend.length() > 0) {
            String content = getOriginalRegistryContent().concat(contentToAppend);
            writeInstalledRegistry(content);
        }
    }

    /**
     * override user.home and add a user registry there
     * 
     * @param content
     * @throws Exception
     */
    private void addUserRegistry(String content) throws Exception {
        File tmpDir = createTempDir();
        System.setProperty(USER_HOME, tmpDir.getCanonicalPath());
        File userRegistry = new File(tmpDir, USER_REGISTRY);
        write(userRegistry, content);
        assertTrue(userRegistry.exists());
        assertTrue(userRegistry.isFile());
    }

    /**
     * @return pre properties, based by System.properties
     */
    private Properties createPreProperties() {
        return new Properties(System.getProperties());
    }

    /**
     * @return post properties, containing python home
     * @throws Exception
     */
    private Properties createPostProperties() throws Exception {
        Properties postProps = new Properties();
        postProps.setProperty(PYTHON_HOME, getRoot().getCanonicalPath());
        return postProps;
    }

    /**
     * create a temp directory, which is removed in cleanup()
     * <p>
     * should only be called once per test method
     * 
     * @return the temp directory
     * @throws Exception
     */
    private File createTempDir() throws Exception {
        String name = getClass().getSimpleName();
        File tmpFile = File.createTempFile(name, "");
        assertTrue(tmpFile.exists());
        assertTrue(tmpFile.isFile());
        File parent = tmpFile.getParentFile();
        assertTrue(parent.exists());
        assertTrue(parent.isDirectory());
        assertTrue(tmpFile.delete());
        File tmpDir = new File(parent, name);
        assertTrue(tmpDir.mkdir());
        assertTrue(tmpDir.exists());
        assertTrue(tmpDir.isDirectory());
        _tmpDir = tmpDir;
        return tmpDir;
    }

    /**
     * delete created files
     */
    private void cleanup() {
        File tmpDir = getTmpDir();
        if (tmpDir != null && tmpDir.exists()) {
            assertTrue(rmdir(tmpDir));
        }
        _tmpDir = null;
    }

    /**
     * read the contents of a file into a String
     * 
     * @param file
     *            The file has to exist
     * @return The contents of the file as String
     * @throws IOException
     */
    private String readAll(File file) throws IOException {
        FileReader fileReader = new FileReader(file);
        try {
            StringBuffer sb = new StringBuffer();
            char[] b = new char[8192];
            int n;
            while ((n = fileReader.read(b)) > 0) {
                sb.append(b, 0, n);
            }
            return sb.toString();
        } finally {
            fileReader.close();
        }
    }

    /**
     * Write contents to a file.
     * <p>
     * An existing file would be overwritten.
     * 
     * @param file
     * @param content
     * 
     * @throws IOException
     */
    private void write(File file, String content) throws IOException {
        FileWriter writer = new FileWriter(file);
        writer.write(content);
        writer.flush();
        writer.close();
    }

    /**
     * completely remove a directory
     * 
     * @param dir
     * @return <code>true</code> if successful, <code>false</code> otherwise.
     */
    private boolean rmdir(File dir) {
        boolean success = true;
        if (dir.exists()) {
            File[] files = dir.listFiles();
            for (int i = 0; i < files.length; i++) {
                File file = files[i];
                if (file.isFile()) {
                    success = carryOnResult(file.delete(), success);
                } else {
                    if (file.isDirectory()) {
                        success = carryOnResult(rmdir(file), success);
                    }
                }
            }
            success = carryOnResult(dir.delete(), success);
        }
        return success;
    }

    /**
     * @param newResult
     * @param existingResult
     * @return <code>false</code> if newResult or existingResult are false, <code>true</code>
     *         otherwise.
     */
    private boolean carryOnResult(boolean newResult, boolean existingResult) {
        if (existingResult) {
            return newResult;
        } else {
            return existingResult;
        }
    }

    private File getRoot() {
        return _root;
    }

    private File getTmpDir() {
        return _tmpDir;
    }

    private String getOriginalUserHome() {
        return _originalUserHome;
    }

    private String getOriginalRegistryContent() {
        return _originalRegistryContent;
    }
}
