package org.apache.commons.cli;

import junit.framework.TestCase;

public class PosixParserTest extends TestCase {

    private static final String TEST_SHORT = "t";
    private static final String TEST_LONG = "test";
    private static final String TEST_DESC = "test option";

    private static final String TEST_SHORT_OPTION = "-t";
    private static final String TEST_LONG_OPTION = "--test";
    
    private static final String ARGUMENT = "argument";

    private Options _options;
    private Parser _parser;

    protected void setUp() {
        _parser = new PosixParser();

        _options = new Options();
        Option testOption = new Option(TEST_SHORT, TEST_LONG, false, TEST_DESC);
        _options.addOption(testOption);
    }

    /**
     * test that an unknown single option and a double hyphen option (with or without argument) are treated the same
     */
    public void testFlattenStop() {
        boolean stopAtNonOption = true; // means unallowed tokens should not be added
        String[] args;
        String[] expectedFlattened;

        // unknown single dash option
        args = new String[] { "-u" };
        expectedFlattened = new String[0];
        assertEquals(expectedFlattened, _parser.flatten(_options, args, stopAtNonOption));
        args = new String[] { "-u", TEST_SHORT_OPTION };
        expectedFlattened = new String[] { TEST_SHORT_OPTION };
        assertEquals(expectedFlattened, _parser.flatten(_options, args, stopAtNonOption));

        // unknown double dash option
        args = new String[] { "--unknown" };
        expectedFlattened = new String[0];
        assertEquals(expectedFlattened, _parser.flatten(_options, args, stopAtNonOption));
        args = new String[] { "--unknown", TEST_LONG_OPTION };
        expectedFlattened = new String[] { TEST_LONG_OPTION };
        assertEquals(expectedFlattened, _parser.flatten(_options, args, stopAtNonOption));

        // unknown double dash option with argument after =
        args = new String[] { "--unknown=" + ARGUMENT };
        expectedFlattened = new String[0];
        assertEquals(expectedFlattened, _parser.flatten(_options, args, stopAtNonOption));
        args = new String[] { "--unknown="+ARGUMENT, TEST_LONG_OPTION };
        expectedFlattened = new String[] { TEST_LONG_OPTION };
        assertEquals(expectedFlattened, _parser.flatten(_options, args, stopAtNonOption));

        // unknown double dash option with argument after ' '
        args = new String[] { "--unknown", ARGUMENT };
        expectedFlattened = new String[] { ARGUMENT };
        assertEquals(expectedFlattened, _parser.flatten(_options, args, stopAtNonOption));
        args = new String[] { "--unknown", ARGUMENT, TEST_LONG_OPTION };
        expectedFlattened = new String[] { ARGUMENT, TEST_LONG_OPTION };
        assertEquals(expectedFlattened, _parser.flatten(_options, args, stopAtNonOption));
    }

    /**
     * test that an unknown single option and a double hyphen option (with or without argument) are treated the same
     */
    public void testFlattenNoStop() {
        boolean stopAtNonOption = false; // means every token should be added
        String[] args;
        String[] expectedFlattened;

        // unknown single dash option
        args = new String[] { "-u" };
        expectedFlattened = new String[] { "-u" };
        assertEquals(expectedFlattened, _parser.flatten(_options, args, stopAtNonOption));
        args = new String[] { "-u", TEST_SHORT_OPTION };
        expectedFlattened = new String[] { "-u", TEST_SHORT_OPTION };
        assertEquals(expectedFlattened, _parser.flatten(_options, args, stopAtNonOption));

        // unknown double dash option
        args = new String[] { "--unknown" };
        expectedFlattened = new String[] { "--unknown" };
        assertEquals(expectedFlattened, _parser.flatten(_options, args, stopAtNonOption));
        args = new String[] { "--unknown", TEST_LONG_OPTION };
        expectedFlattened = new String[] { "--unknown", TEST_LONG_OPTION };
        assertEquals(expectedFlattened, _parser.flatten(_options, args, stopAtNonOption));

        // unknown double dash option with argument after =
        args = new String[] { "--unknown=" + ARGUMENT };
        expectedFlattened = new String[] { "--unknown", ARGUMENT };
        assertEquals(expectedFlattened, _parser.flatten(_options, args, stopAtNonOption));
        args = new String[] { "--unknown="+ ARGUMENT, TEST_LONG_OPTION };
        expectedFlattened = new String[] { "--unknown", ARGUMENT, TEST_LONG_OPTION };
        assertEquals(expectedFlattened, _parser.flatten(_options, args, stopAtNonOption));

        // unknown double dash option with argument after ' '
        args = new String[] { "--unknown", ARGUMENT };
        expectedFlattened = new String[] { "--unknown", ARGUMENT };
        assertEquals(expectedFlattened, _parser.flatten(_options, args, stopAtNonOption));
        args = new String[] { "--unknown", ARGUMENT, TEST_LONG_OPTION };
        expectedFlattened = new String[] { "--unknown", ARGUMENT, TEST_LONG_OPTION };
        assertEquals(expectedFlattened, _parser.flatten(_options, args, stopAtNonOption));
    }

    /**
     * test that a misspelled long option (-test instead of --test) is not interpreted as -t est
     */
    public void testMisspelledLongOption() {
        boolean stopAtNonOption = false; // means every token should be added
        String[] args;
        String[] expectedFlattened;

        // unknown single dash long option
        String singleDashLongOption = "-" + TEST_LONG; 
        args = new String[] { singleDashLongOption };
        expectedFlattened = new String[] { singleDashLongOption };
        assertEquals(expectedFlattened, _parser.flatten(_options, args, stopAtNonOption));
    }
    
    //
    // private stuff
    //

    /**
     * Assert that the content of the specified object arrays is equal
     */
    private void assertEquals(Object[] correct, Object[] tested) {
        assertEquals("different array lengths:", correct.length, tested.length);
        for (int i = 0; i < correct.length; i++) {
            assertEquals("position " + i + " of array differs", correct[i], tested[i]);
        }
    }

}
