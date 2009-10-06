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

public class ModjyTestContentHeaders extends ModjyTestBase {

    // From: http://www.w3.org/Protocols/rfc2616/rfc2616-sec13.html#sec13.5.1
    protected void contentHeaderTestSetUp() throws Exception {
        baseSetUp();
        setAppFile("content_header_tests.py");
    }

    public void doHeaderTest(String appName, String queryString) throws Exception {
        contentHeaderTestSetUp();
        setAppName(appName);
        createServlet();
        if (queryString != null)
            setQueryString(queryString);
        doGet();
    }

    public void doHeaderTest(String appName) throws Exception {
        doHeaderTest(appName, null);
    }

    public void testSetContentLengthHeader() throws Exception {
        doHeaderTest("test_set_content_length");
        assertEquals("Status code != 200: ServerError, =='" + getStatus() + "'", 200, getStatus());
    }

    public void testSetBadContentLengthHeader() throws Exception {
        doHeaderTest("test_set_bad_content_length");
        assertEquals("Status code != 500: ServerError, =='" + getStatus() + "'", 500, getStatus());
        assertTrue("Could not find exception 'BadArgument' in output",
                   getOutput().indexOf("BadArgument") != -1);
    }

    public void testInferredContentLength() throws Exception {
        String appReturn = "Hello World!";
        doHeaderTest("test_inferred_content_length", appReturn);
        assertEquals("Status code != 200: ServerError, =='" + getStatus() + "'", 200, getStatus());
        assertEquals("Content-length != '" + appReturn.length() + "', == '"
                             + getResponse().getHeader("content-length") + "' ",
                     Integer.toString(appReturn.length()),
                     getResponse().getHeader("content-length"));
    }
}
