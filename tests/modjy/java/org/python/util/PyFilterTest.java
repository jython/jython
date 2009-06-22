package org.python.util;

import javax.servlet.http.HttpServletResponse;

import com.mockrunner.mock.web.MockFilterConfig;

public class PyFilterTest extends PyServletTest  {
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        setDoChain(true);
    }

    public void testFilter() {
        getWebMockObjectFactory().getMockRequest().setServletPath(getTestPath("basic"));
        doGet();
        assertFalse(((HttpServletResponse)getFilteredResponse()).containsHeader("X-LookMaNoJava"));
        clearOutput();
        MockFilterConfig cfg = getWebMockObjectFactory().getMockFilterConfig();
        cfg.setInitParameter(PyFilter.FILTER_PATH_PARAM, getTestPath("filter"));
        cfg.setInitParameter("header", "X-LookMaNoJava");
        // Set that PyServlet initialization has run as mockrunner's context doesn't indicate that
        // it happened.
        getWebMockObjectFactory().getMockServletContext().setAttribute(PyServlet.INIT_ATTR, "true");
        createFilter(PyFilter.class);
        doGet();
        HttpServletResponse resp = (HttpServletResponse)getFilteredResponse();
        assertTrue(resp.containsHeader("X-LookMaNoJava"));
    }
}
