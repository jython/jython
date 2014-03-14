/**
 * Copyright 2009, Google Inc.  All rights reserved.
 * Licensed to PSF under a Contributor Agreement.
 */
package org.python.indexer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import junit.framework.TestCase;

import org.python.indexer.types.NType;
import org.python.indexer.types.NUnknownType;

/**
 * Test utilities for {@link IndexerTest}.
 */
public class TestBase extends TestCase {

    // Set this to control logging to the console from the Indexer (mostly at FINER level).
    static protected final Level LOGGING_LEVEL = Level.OFF;

    static protected final String TEST_DATA_DIR;
    static protected final String TEST_LIB_DIR;

    static {
        /*
         * Locate cardinal directories in a way that insulates us from the vagueries of the
         * environment, Ant, IDE and OS.
         */
        String home = System.getProperty("python.home", "dist");
        String test = System.getProperty("python.test.source.dir", "tests/java");
        File source = new File(test, "org/python/indexer"); // corrects to \ where needed.

        // Program actually uses strings, with a trailing slash
        TEST_DATA_DIR = (new File(source, "data")).getAbsolutePath() + File.separator;
        TEST_LIB_DIR = (new File(home, "Lib")).getAbsolutePath() + File.separator;

        // Give the logger used by Indexer an outlet
        setUpLogging();
    }

    // Define a handler for the logger to use
    static private void setUpLogging() {
        // Enable tracing of the operation of the Indexer onto the console
        Logger indexerLogger = Logger.getLogger(Indexer.class.getCanonicalName());
        Handler logHandler = new ConsoleHandler();
        logHandler.setFormatter(new SimpleFormatter());
        logHandler.setLevel(Level.FINEST);
        indexerLogger.addHandler(logHandler);
    }

    protected Indexer idx;

    public TestBase() {
    }

    @Override
    protected void setUp() throws Exception {
        idx = new Indexer();
        idx.getLogger().setLevel(LOGGING_LEVEL);
        idx.enableAggressiveAssertions(true);
        idx.setProjectDir(TEST_DATA_DIR);
        AstCache.get().clearDiskCache();
        AstCache.get().clear();
    }

    /**
     * Call this at the beginning of a test to permit the test code to import
     * modules from the Python standard library.
     */
    protected void includeStandardLibrary() throws Exception {
        idx.addPath(TEST_LIB_DIR);
    }

    protected String abspath(String file) {
        return getTestFilePath(file);
    }

    /**
     * Return absolute path for {@code file}, a relative path under the
     * data/ directory.
     */
    protected String getTestFilePath(String file) {
        return TEST_DATA_DIR + file;
    }

    protected String getSource(String file) throws Exception {
        String path = getTestFilePath(file);
        StringBuilder sb = new StringBuilder();
        BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(path)));
        String line;
        while ((line = in.readLine()) != null) {
            sb.append(line);
            sb.append("\n");
        }
        in.close();  // not overly worried about resource cleanup in unit tests
        return sb.toString();
    }

    /**
     * Construct python source by joining the specified lines.
     */
    protected String makeModule(String... lines) {
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            sb.append(line).append("\n");
        }
        return sb.toString();
    }

    /**
     * Build an index out of the specified file and content lines,
     * and return the resulting module source.
     */
    protected String index(String filename, String... lines) throws Exception {
        String src = makeModule(lines);
        idx.loadString(filename, src);
        idx.ready();
        return src;
    }

    /**
     * Return offset in {@code s} of {@code n}th occurrence of {@code find}.
     * {@code n} is 1-indexed.
     * @throws IllegalArgumentException if the {@code n}th occurrence does not exist
     */
    protected int nthIndexOf(String s, String find, int n) {
        if (n <= 0) {
            throw new IllegalArgumentException();
        }
        int index = -1;
        for (int i = 0; i < n; i++) {
            index = s.indexOf(find, index == -1 ? 0 : index + 1);
            if (index == -1) {
                throw new IllegalArgumentException();
            }
        }
        return index;
    }

    // meta-tests

    public void testHandleExceptionLoggingNulls() throws Exception {
        try {
            idx.enableAggressiveAssertions(false);
            idx.getLogger().setLevel(java.util.logging.Level.OFF);
            idx.handleException(null, new Exception());
            idx.handleException("oops", null);
        } catch (Throwable t) {
            fail("should not have thrown: " + t);
        }
    }

    public void testDataFileFindable() throws Exception {
        assertTrue("Test file not found", new java.io.File(TEST_DATA_DIR).exists());
    }

    public void testLoadDataFile() throws Exception {
        String path = abspath("test-load.txt");
        BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(path)));
        assertEquals(in.readLine().trim(), "hello");
        in.close();
    }

    public void testGetSource() throws Exception {
        String src = getSource("testsrc.txt");
        assertEquals("one\ntwo\n\nthree\n", src);
    }

    public void testStringModule() throws Exception {
        idx.loadString("test-string-module.py", makeModule(
            "def foo():",
            "  pass"));
        idx.ready();
        assertFunctionBinding("test-string-module.foo");
    }

    public void testNthIndexOf() throws Exception {
        String s = "ab a b ab a\nb aab";
        assertEquals(0, nthIndexOf(s, "ab", 1));
        assertEquals(7, nthIndexOf(s, "ab", 2));
        assertEquals(15, nthIndexOf(s, "ab", 3));
        try {
            assertEquals(-1, nthIndexOf(s, "ab", 0));
            assertTrue(false);
        } catch (IllegalArgumentException ix) {
            assertTrue(true);
        }
        try {
            assertEquals(-1, nthIndexOf(s, "ab", 4));
            assertTrue(false);
        } catch (IllegalArgumentException ix) {
            assertTrue(true);
        }
    }

    public void testIndexerDefaults() throws Exception {
        includeStandardLibrary();
        assertEquals("wrong project dir", TEST_DATA_DIR, idx.projDir);
        assertEquals("unexpected load path entries", 1, idx.path.size());
        assertEquals(TEST_LIB_DIR, idx.path.get(0));
    }

    // utilities

    public String buildIndex(String... files) throws Exception {
        for (String f : files) {
            idx.loadFile(abspath(f));
        }
        idx.ready();
        return getSource(files[0]);
    }

    public NBinding getBinding(String qname) throws Exception {
        NBinding b = idx.lookupQname(qname);
        assertNotNull("no binding found for " + qname, b);
        return b;
    }

    public NBinding assertBinding(String qname, NBinding.Kind kind) throws Exception {
        NBinding b = getBinding(qname);
        assertEquals(kind, b.getKind());
        return b;
    }

    public void assertNoBinding(String qname) throws Exception {
        NBinding b = idx.lookupQname(qname);
        assertNull("Should not have found binding for " + qname, b);
    }

    public NBinding assertAttributeBinding(String qname) throws Exception {
        return assertBinding(qname, NBinding.Kind.ATTRIBUTE);
    }

    public NBinding assertBuiltinBinding(String qname) throws Exception {
        NBinding b = getBinding(qname);
        assertTrue(b.isBuiltin());
        return b;
    }

    public NBinding assertClassBinding(String qname) throws Exception {
        return assertBinding(qname, NBinding.Kind.CLASS);
    }

    public NBinding assertConstructorBinding(String qname) throws Exception {
        return assertBinding(qname, NBinding.Kind.CONSTRUCTOR);
    }

    public NBinding assertFunctionBinding(String qname) throws Exception {
        return assertBinding(qname, NBinding.Kind.FUNCTION);
    }

    public NBinding assertMethodBinding(String qname) throws Exception {
        return assertBinding(qname, NBinding.Kind.METHOD);
    }

    public NBinding assertModuleBinding(String qname) throws Exception {
        return assertBinding(qname, NBinding.Kind.MODULE);
    }

    public NBinding assertScopeBinding(String qname) throws Exception {
        return assertBinding(qname, NBinding.Kind.SCOPE);
    }

    public NBinding assertVariableBinding(String qname) throws Exception {
        return assertBinding(qname, NBinding.Kind.VARIABLE);
    }

    public NBinding assertParamBinding(String qname) throws Exception {
        return assertBinding(qname, NBinding.Kind.PARAMETER);
    }

    public void assertStaticSynthetic(NBinding b) {
        assertTrue(b.isStatic());
        assertTrue(b.isSynthetic());
    }

    public Def getDefinition(String qname, int offset, int length) throws Exception {
        NBinding b = getBinding(qname);
        assertNotNull(b.getDefs());
        for (Def def : b.getDefs()) {
            if (offset == def.start() && length == def.end() - def.start()) {
                return def;
            }
        }
        return null;
    }

    public void assertDefinition(String qname, int offset, int length) throws Exception {
        Def def = getDefinition(qname, offset, length);
        if (def == null) {
            fail("No definition for " + qname + " at " + offset + " of len " + length);
        }
    }

    public void assertNoDefinition(String msg, String qname, int pos, int len) throws Exception {
        Def def = getDefinition(qname, pos, len);
        assertNull(msg, def);
    }

    public void assertDefinition(String qname, int offset) throws Exception {
        String[] names = qname.split("[.&@]");
        assertDefinition(qname, offset, names[names.length-1].length());
    }

    public void assertDefinition(String qname, String name, int offset) throws Exception {
        assertDefinition(qname, offset, name.length());
    }

    public Ref getRefOrNull(String qname, int offset, int length) throws Exception {
        NBinding b = getBinding(qname);
        assertNotNull("Null refs list for " + qname, b.getRefs());
        for (Ref ref : b.getRefs()) {
            if (offset == ref.start() && length == ref.length()) {
                return ref;
            }
        }
        return null;
    }

    public Ref getRefOrFail(String qname, int offset, int length) throws Exception {
        Ref ref = getRefOrNull(qname, offset, length);
        assertNotNull("No reference to " + qname + " at offset " + offset + " of length " + length,
                      ref);
        return ref;
    }

    public void assertReference(String qname, int offset, int length) throws Exception {
        assertTrue(getRefOrFail(qname, offset, length).isRef());
    }

    public void assertReference(String qname, int offset, String refname) throws Exception {
        assertReference(qname, offset, refname.length());
    }

    // assume reference to "a.b.c" is called "c" -- the normal case
    public void assertReference(String qname, int offset) throws Exception {
        String[] names = qname.split("[.&@]");
        assertReference(qname, offset, names[names.length-1]);
    }

    public void assertNoReference(String msg, String qname, int pos, int len) throws Exception {
        assertNull(msg, getRefOrNull(qname, pos, len));
    }

    public void assertCall(String qname, int offset, int length) throws Exception {
        assertTrue(getRefOrFail(qname, offset, length).isCall());
    }

    public void assertCall(String qname, int offset, String refname) throws Exception {
        assertCall(qname, offset, refname.length());
    }

    // "a.b.c()" => look for call reference at "c"
    public void assertCall(String qname, int offset) throws Exception {
        String[] names = qname.split("[.&@]");
        assertCall(qname, offset, names[names.length-1]);
    }

    public void assertConstructed(String qname, int offset, int length) throws Exception {
        assertTrue(getRefOrFail(qname, offset, length).isNew());
    }

    public void assertConstructed(String qname, int offset, String refname) throws Exception {
        assertConstructed(qname, offset, refname.length());
    }

    // "a.b.c()" => look for call reference at "c"
    public void assertConstructed(String qname, int offset) throws Exception {
        String[] names = qname.split("[.&@]");
        assertConstructed(qname, offset, names[names.length-1]);
    }

    public NType getTypeBinding(String typeQname) throws Exception {
        NType type = idx.lookupQnameType(typeQname);
        assertNotNull("No recorded type for " + typeQname, type);
        return type;
    }

    // Assert that binding for qname has exactly one type (not a union),
    // and that type has a binding with typeQname.
    public NBinding assertBindingType(String bindingQname, String typeQname) throws Exception {
        NBinding b = getBinding(bindingQname);
        NType expected = getTypeBinding(typeQname);
        assertEquals("Wrong binding type", expected, NUnknownType.follow(b.getType()));
        return b;
    }

    public NBinding assertBindingType(String bindingQname, Class type) throws Exception {
        NBinding b = getBinding(bindingQname);
        NType btype = NUnknownType.follow(b.getType());
        assertTrue("Wrong type: expected " + type + " but was " + btype,
                   type.isInstance(btype));
        return b;
    }

    public void assertListType(String bindingQname) throws Exception {
        assertListType(bindingQname, null);
    }

    /**
     * Asserts that the binding named by {@code bindingQname} exists and
     * its type is a List type.  If {@code eltTypeQname} is non-{@code null},
     * asserts that the List type's element type is an existing binding with
     * {@code eltTypeQname}.
     */
    public void assertListType(String bindingQname, String eltTypeQname) throws Exception {
        NBinding b = getBinding(bindingQname);
        NType btype = b.followType();
        assertTrue(btype.isListType());
        if (eltTypeQname != null) {
            NType eltType = getTypeBinding(eltTypeQname);
            assertEquals(eltType, NUnknownType.follow(btype.asListType().getElementType()));
        }
    }

    public void assertStringType(String bindingQname) throws Exception {
        assertBindingType(bindingQname, "__builtin__.str");
    }

    public void assertNumType(String bindingQname) throws Exception {
        assertBindingType(bindingQname, "__builtin__.float");
    }

    public void assertInstanceType(String bindingQname, String classQname) throws Exception {
        if (true) {
            assertBindingType(bindingQname, classQname);
            return;
        }

        // XXX:  we've disabled support for NInstanceType for now
        NBinding b = getBinding(bindingQname);
        NType btype = b.followType();
        assertTrue(btype.isInstanceType());
        NType ctype = getTypeBinding(classQname);
        assertEquals(btype.asInstanceType().getClassType(), ctype);
    }
}
