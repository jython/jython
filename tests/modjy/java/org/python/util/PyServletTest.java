package org.python.util;

import javax.servlet.ServletException;

import org.python.core.PyFile;
import com.mockrunner.base.NestedApplicationException;
import com.mockrunner.mock.web.MockServletConfig;
import com.mockrunner.mock.web.MockServletContext;
import com.mockrunner.mock.web.WebMockObjectFactory;
import com.mockrunner.servlet.BasicServletTestCaseAdapter;

public class PyServletTest extends BasicServletTestCaseAdapter {
    public void testGet() {
        assertEquals("Basic text response", doGetAndRead("basic"));
    }

    public void testNoCallable() {
        try {
            doGetAndRead("empty");
            fail("Using an empty file for PyServlet should raise a ServletException");
        } catch (NestedApplicationException e) {
            assertTrue(e.getRootCause() instanceof ServletException);
        }
    }

    public void testReload() {
        String originalBasic = readTestFile("basic");
        try {
            testGet();
            writeToTestFile("basic", readTestFile("updated_basic"));
            assertEquals("Updated text response", doGetAndRead("basic"));
        } finally {
            writeToTestFile("basic", originalBasic);
        }
    }

    public void testInstanceCaching() {
        assertEquals("1", doGetAndRead("increment"));
        assertEquals("2", doGetAndRead("increment"));
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        MockServletConfig cfg = getWebMockObjectFactory().getMockServletConfig();
        cfg.setInitParameter("python.home", System.getProperty("JYTHON_HOME"));
        createServlet(PyServlet.class);
    }

    protected String doGetAndRead(String testName) {
        getWebMockObjectFactory().getMockRequest().setServletPath(getTestPath(testName));
        doGet();
        String result = getOutput();
        clearOutput();
        return result;
    }

    protected String getTestPath(String testName) {
        return "/test/pyservlet/" + testName + ".py";
    }

    private String readTestFile(String testName) {
        PyFile in = new PyFile(basePath + getTestPath(testName), "r", 4192);
        String result = in.read().toString();
        return result;
    }

    private void writeToTestFile(String testName, String newContents) {
        PyFile out = new PyFile(basePath + getTestPath(testName), "w", 4192);
        out.write(newContents);
        out.close();
    }

    @Override
    protected WebMockObjectFactory createWebMockObjectFactory() {
        return new WebMockObjectFactory() {
            @Override public MockServletContext createMockServletContext() {
                return new MockServletContext() {
                    @Override public synchronized String getRealPath(String path) {
                        return basePath + path;
                    }
                };
            }
        };
    }

    protected String basePath = System.getProperty("JYTHON_HOME") + "/Lib";
}
