/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 * 
 * $Id: ParseRequiredTest.java 3134 2007-03-02 07:20:08Z otmarhumbel $
 */

package org.apache.commons.cli;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * @author John Keyes (john at integralsource.com)
 * @version $Revision: 3134 $
 */
public class ParseRequiredTest extends TestCase
{

    private Options _options = null;
    private CommandLineParser parser = new PosixParser();

    public static Test suite() { 
        return new TestSuite(ParseRequiredTest.class); 
    }

    public ParseRequiredTest(String name)
    {
        super(name);
    }

    public void setUp()
    {
        OptionBuilder.withLongOpt( "bfile" );
        OptionBuilder.hasArg();
        OptionBuilder.isRequired();
        OptionBuilder.withDescription( "set the value of [b]" );
        Option opt2 = OptionBuilder.create( 'b' );
        _options = new Options()
            .addOption("a",
                       "enable-a",
                       false,
                       "turn [a] on or off")
            .addOption( opt2 );
    }

    public void tearDown()
    {

    }

    public void testWithRequiredOption()
    {
        String[] args = new String[] {  "-b", "file" };

        try
        {
            CommandLine cl = parser.parse(_options,args);
            
            assertTrue( "Confirm -a is NOT set", !cl.hasOption("a") );
            assertTrue( "Confirm -b is set", cl.hasOption("b") );
            assertTrue( "Confirm arg of -b", cl.getOptionValue("b").equals("file") );
            assertTrue( "Confirm NO of extra args", cl.getArgList().size() == 0);
        }
        catch (ParseException e)
        {
            fail( e.toString() );
        }
    }

    public void testOptionAndRequiredOption()
    {
        String[] args = new String[] {  "-a", "-b", "file" };

        try
        {
            CommandLine cl = parser.parse(_options,args);

            assertTrue( "Confirm -a is set", cl.hasOption("a") );
            assertTrue( "Confirm -b is set", cl.hasOption("b") );
            assertTrue( "Confirm arg of -b", cl.getOptionValue("b").equals("file") );
            assertTrue( "Confirm NO of extra args", cl.getArgList().size() == 0);
        }
        catch (ParseException e)
        {
            fail( e.toString() );
        }
    }

    public void testMissingRequiredOption()
    {
        String[] args = new String[] { "-a" };

        CommandLine cl = null;
        try
        {
            cl = parser.parse(_options,args);
            fail( "exception should have been thrown" );
        }
        catch (ParseException e)
        {
            if( !( e instanceof MissingOptionException ) )
            {
                fail( "expected to catch MissingOptionException in " + cl );
            }
        }
    }

}
