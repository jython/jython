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


public class ModjyTestHeaders extends ModjyTestBase {

    // From: http://www.w3.org/Protocols/rfc2616/rfc2616-sec13.html#sec13.5.1
    final static String[] hop_by_hop_headers = new String[] {"connection",
                                                             "keep-alive",
                                                             "proxy-authenticate",
                                                             "proxy-authorization",
                                                             "te",
                                                             "trailers",
                                                             "transfer-encoding",
                                                             "upgrade",};

    protected void headerTestSetUp() throws Exception {
        baseSetUp();
        setAppFile("header_tests.py");
    }

    public void doHeaderTest(String appName, String queryString) throws Exception {
        headerTestSetUp();
        setAppName(appName);
        createServlet();
        if (queryString != null)
            setQueryString(queryString);
        doGet();
    }

    public void doHeaderTest(String appName) throws Exception {
        doHeaderTest(appName, null);
    }

    public void testInvalidStatusCode() throws Exception {
        doHeaderTest("test_invalid_status_code");
        assertEquals("Status code != 500: ServerError, =='" + getStatus() + "'", 500, getStatus());
        assertTrue("Could not find exception 'BadArgument' in output: " + getOutput(),
                   getOutput().indexOf("BadArgument") != -1);
    }

    public void testNonLatin1StatusString() throws Exception {
        // We'll let this one pass:
        // 1. The integer status code can't be anything but an ASCII-encoded integer
        // 2. The reason phrase is discarded on J2EE anyway
        // Modjy takes no action if a non latin-1 status string is passed
        doHeaderTest("test_non_latin1_status_string");
        assertEquals("Status code != 500: ServerError, =='" + getStatus() + "'", 500, getStatus());
        assertTrue("Could not find exception 'BadArgument' in output: " + getOutput(),
                   getOutput().indexOf("BadArgument") != -1);
    }

    public void testLatin1StatusStringWithControlChars() throws Exception {
        // We'll let this one pass:
        // 1. The integer status code can't be anything but an ASCII-encoded integer
        // 2. The reason phrase is discarded on J2EE anyway
        // Modjy takes no action if a status string with control chars is passed
        doHeaderTest("test_control_chars_in_status_string");
        assertEquals("Status code != 500: ServerError, =='" + getStatus() + "'", 500, getStatus());
        assertTrue("Could not find exception 'BadArgument' in output: " + getOutput(),
                   getOutput().indexOf("BadArgument") != -1);
    }

    public void doBadHeadersListTest(String appName) throws Exception {
        for (int i = 1; i < 5; i++) {
            doHeaderTest(appName, String.valueOf(i));
            assertEquals("Status code != 500: ServerError, =='" + getStatus() + "'",
                         500,
                         getStatus());
            String firstLine = getOutput().split("\n")[0];
            assertTrue("Could not find exception 'BadArgument' in output: " + firstLine,
                       firstLine.indexOf("BadArgument") != -1);
        }
    }

    public void testHeadersNotList() throws Exception {
        // Ensure that setting headers to anything other than a list raises an error
        doBadHeadersListTest("test_headers_not_list");
    }

    public void testHeadersListContainsNonTuples() throws Exception {
        // Ensure that setting headers to anything other than a list raises an error
        doBadHeadersListTest("test_headers_list_non_tuples");
    }

    public void testHeadersListContainsWrongLengthTuples() throws Exception {
        // Ensure that setting headers to anything other than a list of 2-tuples raises an error
        doBadHeadersListTest("test_headers_list_wrong_length_tuples");
    }

    public void testHeadersListContainsWrongTypeTuples() throws Exception {
        // Ensure that setting headers to anything other than a list of 2-tuples of strings raises
        // an error
        doBadHeadersListTest("test_headers_list_wrong_types_in_tuples");
    }

    public void testHeadersListContainsNonLatin1Values() throws Exception {
        // Ensure that setting header values to non-latin1 strings raises an error
        doBadHeadersListTest("test_headers_list_contains_non_latin1_values");
    }

    public void testHeadersListContainsValuesWithControlChars() throws Exception {
    // Ensure that setting header values to strings raises an error
    // Disable this test: Modjy doesn't test for control characters.
    // doBadHeadersListTest("test_headers_list_contains_values_with_control_chars");
    }

    public void testHeadersListContainsAccentedLatin1Values() throws Exception {
        // Ensure that setting header values to 8-bit latin1 strings works properly
        String headerName = "x-latin1-header";
        String headerValue = "\u00e1\u00e9\u00ed\u00f3\u00fa";
        String headerQString = headerName + "=" + headerValue;
        doHeaderTest("test_headers_list_contains_accented_latin1_values", headerQString);
        assertEquals("Status code != 200: ServerError, =='" + getStatus() + "'", 200, getStatus());
        assertTrue("Header '" + headerName + "' not returned: ",
                   getResponse().containsHeader(headerName));
        assertEquals("Header '" + headerName + "' != '" + headerValue + "', == '"
                             + getResponse().getHeader(headerName) + "' ",
                     headerValue,
                     getResponse().getHeader(headerName));
    }

    public void testHopByHopHeaders() throws Exception {
        // Test that attempts to set hop-by-hop headers raise exception.
        for (String hopByHopHeader : hop_by_hop_headers) {
            doHeaderTest("test_hop_by_hop", hopByHopHeader);
            assertEquals("Status code != 500: ServerError, =='" + getStatus() + "'",
                         500,
                         getStatus());
            assertTrue("Could not find exception 'HopByHopHeaderSet' in output",
                       getOutput().indexOf("HopByHopHeaderSet") != -1);
        }
    }

    public void testSetHeader() throws Exception {
    // test that we can set headers
    }

    public void testMultilineHeaders() {
    // Need to do some research on this
    }

    public void testRFC2047EncodedHeaders() {
    // Need to do some research on this
    }
}
