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
import org.python.core.PyString;

public class ModjyTestWSGIStreams extends ModjyTestBase {

    protected void streamTestSetUp() throws Exception {
        baseSetUp();
        setAppFile("stream_tests.py");
    }

    // Tests in here are really container independent: no guarantee
    // that mockrunner correctly simulates container behaviour
    public void doInputTest(String appName,
                            String bodyContent,
                            String expectedContent,
                            int expectedLength,
                            int readSize) throws Exception {
        streamTestSetUp();
        setAppName(appName);
        createServlet();
        // createServlet sets a default content string of "", so must override after, not before
        setBodyContent(bodyContent);
        // If there is a read length, set it in the query string
        if (readSize != 0) {
            setQueryString("readsize=" + String.valueOf(readSize));
        }
        doGet();
        String app_output = getOutput();
        PyDictionary result = (PyDictionary)evalPythonString(app_output);
        String instream_contents = ((PyString)result.__finditem__("data")).toString();
        assertEquals("Application output length != " + expectedLength + ", =='"
                + instream_contents.length() + "'", instream_contents.length(), expectedLength);
        assertEquals("Application output != '" + expectedContent + "', =='" + instream_contents
                + "'", instream_contents, expectedContent);
    }

    public void doInputTest(String appName,
                            String bodyContent,
                            String expectedContent,
                            int expectedLength) throws Exception {
        doInputTest(appName, bodyContent, expectedContent, expectedLength, 0);
    }

    protected String buildStringWithContents(int count, String contents) {
    	StringBuilder builder = new StringBuilder();
    	for (int i = 0 ; i < count ; i++) {
            builder.append(contents);
        };
        return builder.toString();
    }

    public void testEmptyInput() throws Exception {
        doInputTest("test_read_input_stream", "", "", 0);
    }

    public void testEmptyInputWithReadSize() throws Exception {
        doInputTest("test_read_input_stream", "", "", 0, 1024);
    }

    public void testAsciiInput() throws Exception {
        doInputTest("test_read_input_stream", "Hello World!", "Hello World!", 12);
    }

    public void testAsciiInputWithReadSize() throws Exception {
        for (int i = 0; i < 14; i++) {
            doInputTest("test_read_input_stream", "Hello World!", "Hello World!", 12, i);
        }
    }

    public void testAsciiInputWithReadSizeLargeInput() throws Exception {
        String one_k = buildStringWithContents(1024, "X");
        String testData = buildStringWithContents(64, one_k);
        for (int i = 64; i <= 65536; i=i*2) {
            doInputTest("test_read_input_stream", testData, testData, testData.length(), i);
        }
    }

    public void testAsciiInputReadline() throws Exception {
        doInputTest("test_readline_input_stream", "Hello\nWorld!\n", "Hello\n", 6);
        doInputTest("test_readline_input_stream", "Hello", "Hello", 5);
    }

    public void testAsciiInputReadlineWithSize() throws Exception {
        // Let's test this: although PEP-333 says it's not supported, modjy can do it
        doInputTest("test_readline_input_stream", "Hello\nWorld!\n", "Hello\n", 6, 6);
    }

    public void testAsciiInputReadlineWithSizeLessThanOneLine() throws Exception {
        // Test where the size given is too small to actually reach a line end and
        // a truncated line should be returned.
        doInputTest("test_readline_input_stream", "Hello\nWorld!\n", "Hel", 3, 3);
    }

    public void testAsciiInputReadlineWithSizeLongerThanOneLine() throws Exception {
        // Test where the size given is long enough to take in a whole line 
        // and part of the next line.
        doInputTest("test_readline_input_stream", "Hello\nWorld!\n", "Hello\n", 6, 10);
    }

    public void testAsciiInputReadlineWithSizeLongerThanTwoLines() throws Exception {
        // Test where the size given is long enough to take in a whole line 
        // and part of the next line.
        doInputTest("test_readline_input_stream", "Hello\nWorld!\n", "Hello\n", 6, 32);
    }

    public void testAsciiInputWithReadlines() throws Exception {
        doInputTest("test_readlines_input_stream", "Hello", "Hello", 5);
        doInputTest("test_readlines_input_stream", "Hello\n", "Hello\n", 6);
        doInputTest("test_readlines_input_stream", "Hello\nWorld!\n", "Hello\n$World!\n", 14);
    }

    public void testAsciiInputWithReadlinesWithHint() throws Exception {
        doInputTest("test_readlines_input_stream", "Hello\nWorld!\n", "Hello\n", 6, 5);
        doInputTest("test_readlines_input_stream", "Hello\nWorld!\n", "Hello\n$World!\n", 14, 32);
    }

    public void testInputIterator() throws Exception {
        doInputTest("test_iter_input_stream", "Hello\nWorld!\n", "Hello\n$World!\n", 14);
    }

    public void testError() throws Exception {
        // For now, just check the requisite methods exist and are callable
        streamTestSetUp();
        setAppName("test_error_stream");
        createServlet();
        doGet();
        String app_output = getOutput();
        assertEquals("Application output != 'success', =='" + app_output + "'",
                     "success",
                     app_output);
    }

    public void testContentType() {
    // This probably needs several tests
    }

    public void testContentLength() {
    // This probably needs several tests
    }
}
