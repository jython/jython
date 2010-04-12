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

public class ModjyTestPostData extends ModjyTestBase {

    protected void postDataTestSetUp() throws Exception {
        baseSetUp();
        setAppFile("post_data_tests.py");
    }

    public void doHeaderTest(String appName, String postData) throws Exception {
        postDataTestSetUp();
        setMethod("POST");
        setAppName(appName);
        createServlet();
        if (postData != null)
            setBodyContent(postData);
        doPost();
    }

    public void testPostDataLineEndsNotTranslated() throws Exception {
    	String testData = "this\r\ndata\r\ncontains\r\ncarriage\r\nreturns\r\n";
    	String expectedData = "'"+testData.replace("\r", "\\r").replace("\n", "\\n")+"'";
        doHeaderTest("test_return_post_data", testData);
        assertEquals("Wrong post data returned >>" + getOutput() + "<< != >>"+expectedData+"<<", expectedData, getOutput());
    }

}
