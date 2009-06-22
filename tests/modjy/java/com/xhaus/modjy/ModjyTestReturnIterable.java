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

public class ModjyTestReturnIterable extends ModjyTestBase {

    protected void returnTestSetUp() throws Exception {
        baseSetUp();
        setAppFile("return_tests.py");
    }

    public void doReturnTest(String appName, String queryString) throws Exception {
        returnTestSetUp();
        setAppName(appName);
        createServlet();
        if (queryString != null)
            setQueryString(queryString);
        doGet();
    }

    public void doReturnTest(String appName) throws Exception {
        doReturnTest(appName, null);
    }

    public void doWrongReturnTypeTest(String appName, String typeString) throws Exception {
        // Try to return not-iterables
        doReturnTest(appName, typeString);
        assertEquals("Status code != 500: ServerError, =='" + getStatus() + "'", 500, getStatus());
        String firstLine = getOutput().split("\n")[0];
        assertTrue("Could not find exception 'ReturnNotIterable' in output: " + firstLine,
                   firstLine.indexOf("ReturnNotIterable") != -1);
    }

    public void testReturnString() throws Exception {
        doWrongReturnTypeTest("test_non_iterable_return", "str");
    }

    public void testReturnUnicode() throws Exception {
        doWrongReturnTypeTest("test_non_iterable_return", "unicode");
    }

    public void testReturnInt() throws Exception {
        doWrongReturnTypeTest("test_non_iterable_return", "int");
    }

    public void testReturnFloat() throws Exception {
        doWrongReturnTypeTest("test_non_iterable_return", "float");
    }

    public void testReturnNone() throws Exception {
        doWrongReturnTypeTest("test_non_iterable_return", "none");
    }

    public void doIterableContainsWrongReturnTypeTest(String appName, String typeString)
            throws Exception {
        // Try to return not-iterables
        doReturnTest(appName, typeString);
        assertEquals("Status code != 500: ServerError, =='" + getStatus() + "'", 500, getStatus());
        String firstLine = getOutput().split("\n")[0];
        assertTrue("Could not find exception 'NonStringOutput' in output: " + firstLine,
                   firstLine.indexOf("NonStringOutput") != -1);
    }

    public void testReturnIterableContainingInt() throws Exception {
        doIterableContainsWrongReturnTypeTest("test_iterable_containing_non_strings_return", "int");
    }

    public void testReturnIterableContainingFloat() throws Exception {
        doIterableContainsWrongReturnTypeTest("test_iterable_containing_non_strings_return",
                                              "float");
    }

    public void testReturnIterableContainingNone() throws Exception {
        doIterableContainsWrongReturnTypeTest("test_iterable_containing_non_strings_return", "none");
    }

    public void testStartResponseNotCalled() throws Exception {
        doReturnTest("test_start_response_not_called");
        assertEquals("Status code != 500: ServerError, =='" + getStatus() + "'", 500, getStatus());
        String firstLine = getOutput().split("\n")[0];
        assertTrue("Could not find exception 'StartResponseNotCalled' in output: " + firstLine,
                   firstLine.indexOf("StartResponseNotCalled") != -1);
    }

    public void testBadLengthIterator() throws Exception {
        doReturnTest("test_bad_length_iterator");
        assertEquals("Status code != 500: ServerError, =='" + getStatus() + "'", 500, getStatus());
        assertTrue("Could not find exception 'WrongLength' in output",
                   getOutput().indexOf("WrongLength") != -1);
    }

    String testData = "Drifting breeze-blown clouds:\r\n" + "Shadows glide across the grass..\r\n"
            + "Apple blossom falls.\r\n";

    public void doCorrectIterableTest(String appName) throws Exception {
        // Try to return not-iterables
        doReturnTest(appName, testData);
        assertEquals("Status code != 200: ServerError, =='" + getStatus() + "'", 200, getStatus());
        String output = getOutput();
        assertEquals(output, testData);
    }

    public void testReturnListOfString() throws Exception {
        doCorrectIterableTest("test_return_list_strings");
    }

    public void testReturnGenerator() throws Exception {
        doCorrectIterableTest("test_return_generator");
    }

    public void testReturnFileLike() throws Exception {
        doCorrectIterableTest("test_return_file_like");
    }

    public void testLineEndsNotTranslated() throws Exception {
        doCorrectIterableTest("test_return_list_strings");
    }

    public void testIterableInstance() throws Exception {
        doCorrectIterableTest("test_iterable_instance");
    }
}
