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

import javax.servlet.http.HttpServlet;

public class ModjyTestServletLifecycle extends ModjyTestBase {

    protected void lifecycleTestSetUp() throws Exception {
        baseSetUp();
        setAppFile("lifecycle_tests.py");
    }

    public void testAtExitHandlersCalled() throws Exception {
        System.setProperty("modjy", "here");
        lifecycleTestSetUp();
        createServlet();
        doGet();
        HttpServlet modjyServlet = getServlet();
        modjyServlet.destroy();
        assertEquals("gone", System.getProperty("modjy"));
    }

}
