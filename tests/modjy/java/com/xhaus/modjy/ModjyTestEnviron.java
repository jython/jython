/*###
#
# Copyright Alan Kennedy.
#
# You may contact the copyright holder at this uri:
#
# http://www.xhaus.com/contact/modjy
#
# The licence under which this code is released is the Apache License v2.0.
#
# The terms and conditions of this license are listed in a file contained
# in the distribution that also contained this file, under the name
# LICENSE.txt.
#
# You may also read a copy of the license at the following web address.
#
# http://modjy.xhaus.com/LICENSE.txt
#
###*/

package com.xhaus.modjy;

import org.python.core.PyDictionary;
import org.python.core.PyInteger;
import org.python.core.PyObject;
import org.python.core.PyString;

public class ModjyTestEnviron extends ModjyTestBase {

    protected void environTestSetUp() throws Exception {
        baseSetUp();
        setAppFile("environ_tests.py");
    }

    public void testEnvIsDict() throws Exception {
        // Spec says that the env MUST be a dict, not a subclass or emulation.
        environTestSetUp();
        setAppName("test_env_is_dict");
        createServlet();
        doGet();
        String result = getOutput();
        assertEquals(result, "true");
    }

    public void testEnvIsMutable() throws Exception {
        // Spec says that the env must be mutable.
        environTestSetUp();
        setAppName("test_env_is_mutable");
        createServlet();
        doGet();
        String result = getOutput();
        assertEquals(result, "true");
    }

    public void doRequestMethodTest(String method) throws Exception {
		// Check that the environment contains the REQUEST_METHOD var
		// REQUEST_METHOD
		environTestSetUp();
		setAppName("test_env_contains_request_method");
		createServlet();
		method = method.toLowerCase();
		// Java needs first class functions
		if		(method == "delete")	doDelete();
		else if (method == "get")		doGet();
		else if (method == "head")		doHead();
		else if (method == "options")	doOptions();
		else if (method == "post")		doPost();
		else if (method == "put")		doPut();
		else if (method == "trace")		doTrace();
    	String result = getOutput();
    	assertEquals(result, method.toUpperCase());
    }

    public void testRequestMethod() throws Exception {
        doRequestMethodTest("delete");
        doRequestMethodTest("get");
        doRequestMethodTest("head");
        doRequestMethodTest("options");
        doRequestMethodTest("post");
        doRequestMethodTest("put");
        doRequestMethodTest("trace");
    }

    // This test doesn't really achieve much, because mockrunner doesn't actually do any computation
    // on the incoming URI: you have to set the servletContextPath(SCRIPT_NAME) and
    // PathInfo(PATH_INFO) yourself.
    // Still, best to include the test.
    // Maybe in the future we can implement a subclass of MockHttpServletRequest
    // which actually does computation on the incoming URI to work out the pathInfo.
    public void doScriptNamePathInfoTest(String contextPath,
                                         String servletPath,
                                         String URI,
                                         String pathInfo,
                                         String expectedScriptName,
                                         String expectedPathInfo,
                                         boolean appImportable) throws Exception {
        environTestSetUp();
        if (appImportable)
            setAppImportable("script_name_path_info.test_env_script_name_path_info");
        else
            setAppName("test_env_script_name_path_info");
        createServlet();
        setServletContextPath(contextPath);
        setServletPath(servletPath);
        setRequestURI(URI);
        setPathInfo(pathInfo);
        doGet();
        String output = getOutput();
        String[] results = output.split(":::");
        assertEquals("ScriptName '" + results[0] + "' != '" + expectedScriptName + "'",
                     expectedScriptName,
                     results[0]);
        String actualPathInfo = "";
        if (results.length > 1) {
            actualPathInfo = results[1];
        }
        assertEquals("PathInfo '" + actualPathInfo + "' != '" + expectedPathInfo + "'",
                     actualPathInfo,
                     expectedPathInfo);
    }

	public void testScriptNamePathInfo ( )
    	throws Exception
	{
		// Check that the environment contains correct values for SCRIPT_NAME and PATH_INFO
		String[][] testData = {
			{"",		"/",		""			},
			{"",		"/",		"info"		},
			{"/ctx",	"/srv",		""			},
			{"/ctx",	"/srv",		"/info"		},
			{"/ctx",	"/srv/sub",	""			},
			{"/ctx",	"/srv/sub",	"/info"		},
		};
        for (String[] element : testData) {
            String contextPath = element[0];
            String servletPath = element[1];
            String pathInfo = element[2];
            String uri = contextPath + servletPath + pathInfo;
            String expectedPathInfo = pathInfo;
            doScriptNamePathInfoTest(contextPath, servletPath, uri, pathInfo, contextPath
                    + servletPath, expectedPathInfo, false);
            doScriptNamePathInfoTest(contextPath, servletPath, uri, pathInfo, contextPath
                    + servletPath, expectedPathInfo, true);
        }
	}

    // Again, this test doesn't achieve a lot under mockrunner, because you have to
    // set the query string yourself.
    public void doQueryStringTest(String actualQString, String expectedQString) throws Exception {
        environTestSetUp();
        setAppName("test_env_query_string");
        createServlet();
        setQueryString(actualQString);
        doGet();
        String result = getOutput();
        assertEquals("QueryString '" + result + "' != '" + expectedQString + "'",
                     result,
                     expectedQString);
    }

    public void testQueryString() throws Exception {
        // Test no query string
        doQueryStringTest(null, "");
        // Test empty query string
        doQueryStringTest("", "");
        // Test variable no value
        doQueryStringTest("name", "name");
        // Test variable and value
        doQueryStringTest("name=value", "name=value");
        // Test two name/value pairs
        doQueryStringTest("n1=v1&n2=v2", "n1=v1&n2=v2");
        // Test no unquoting done
        doQueryStringTest("var=hello+world!", "var=hello+world!");
        // Any illegal values to test?
    }

    public void testContentVars() {
    // Test CONTENT_TYPE and CONTENT_LENGTH
    // Perhaps these should be done with the WSGIInput tests?
    }

    public PyObject doEnvVarTest(String name) throws Exception {
        environTestSetUp();
        setAppName("test_echo_wsgi_env");
        createServlet();
        setQueryString(name);
        doGet();
        String output = getOutput();
        PyDictionary result = (PyDictionary)evalPythonString(output);
        return result.__finditem__(name);
    }

    public void doEnvVarTestCheckReturn(String name, String value) throws Exception {
        PyString envVar = (PyString)doEnvVarTest(name);
        assertTrue("Env var '" + name + "' != '" + value + "', == '" + envVar.toString() + "'",
                   envVar.toString().compareTo(value) == 0);
    }

    public void testServerParams_ServerName() throws Exception {
        String serverName = "ModjyServerTest";
        setServerName(serverName);
        doEnvVarTestCheckReturn("SERVER_NAME", serverName);
    }

    public void testServerParams_ServerPort() throws Exception {
        int serverPort = 54321;
        setServerPort(serverPort);
        doEnvVarTestCheckReturn("SERVER_PORT", String.valueOf(serverPort));
    }

    public void testServerParams_ServerProtocol() throws Exception {
        String serverProtocol = "HTTP/1.1";
        setProtocol(serverProtocol);
        doEnvVarTestCheckReturn("SERVER_PROTOCOL", serverProtocol);
    }

    public void testOtherHttpVars() {
    // Test other vars that begin with HTTP
    }

    public void testMandatoryWSGIVars_WSGIVersion() throws Exception {
        PyObject result = doEnvVarTest("wsgi.version");
        assertTrue("'wsgi.version' != PyTuple", result instanceof org.python.core.PyTuple);
        assertEquals("'wsgi.version[0]' != '1'", ((PyInteger)result.__getitem__(0)).getValue(), 1);
        assertEquals("'wsgi.version[1]' != '0'", ((PyInteger)result.__getitem__(1)).getValue(), 0);
    }

    public void testMandatoryWSGIVars_URLScheme_HTTP() throws Exception {
        String scheme = "http";
        setScheme(scheme);
        doEnvVarTestCheckReturn("wsgi.url_scheme", scheme);
    }

    public void testMandatoryWSGIVars_URLScheme_HTTPS() throws Exception {
        String scheme = "https";
        setScheme(scheme);
        doEnvVarTestCheckReturn("wsgi.url_scheme", scheme);
    }

    public void testMandatoryWSGIVars_Other() throws Exception {
    // These should be checked under input and error handling respectively
    // wsgi.input
    // wsgi.errors
    // These should be checked under app invocation
    // wsgi.multithread
    // wsgi.multiprocess
    // wsgi.run_once
    }

    public void testEnvContainsModjySpecificVars() {
    // Test that the environment contains all of the modjy specific vars
    }

    public void testUserSpecifiedEnv() throws Exception {
        // Set a user specified environment in the servlet config, then test that it is passed
        // through.
        String envName1 = "ENVVAR1";
        String envValue1 = "ENVVAL1";
        String envName2 = "ENVVAR2";
        String envValue2 = "ENVVAL2";
        String initialEnv = "\n" + envName1 + "\t: " + envValue1 + "\n" + envName2 + " :\t"
                + envValue2 + "\n";
        setInitParameter("initial_env", initialEnv);
        doEnvVarTestCheckReturn(envName1, envValue1);
        doEnvVarTestCheckReturn(envName2, envValue2);
    }

    public void testUserConfigVars() {
    // Set some environment vars in the servlet config, then test that they are passed through.
    }

    public void testCgiVarsAreStr() throws Exception {
        // PEP-333 states that all CGI vars must be str, not unicode
        // http://www.python.org/dev/peps/pep-0333/#unicode-issues
        environTestSetUp();
        setAppName("test_cgi_vars_are_str");
        createServlet();
        doGet();
        String output = getOutput();
        assertEquals("pass", output);
    }

    public void testMultipleHeaderValues() throws Exception {
        environTestSetUp();
        setAppName("test_multiple_header_values");
        createServlet();
        addHeader("MULTIPLE", "value 1");
        addHeader("MULTIPLE", "value 2");
        doGet();
        String output = getOutput();
        assertEquals("pass", output);
    }
}
