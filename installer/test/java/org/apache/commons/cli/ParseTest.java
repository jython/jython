/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 * 
 * $Id: ParseTest.java 3134 2007-03-02 07:20:08Z otmarhumbel $
 */

package org.apache.commons.cli;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class ParseTest extends TestCase
{

    private Options _options = null;
    private CommandLineParser _parser = null;

    public static Test suite() { 
        return new TestSuite(ParseTest.class); 
    }

    public ParseTest(String name)
    {
        super(name);
    }

    public void setUp()
    {
        _options = new Options()
            .addOption("a",
                       "enable-a",
                       false,
                       "turn [a] on or off")
            .addOption("b",
                       "bfile",
                       true,
                       "set the value of [b]")
            .addOption("c",
                       "copt",
                       false,
                       "turn [c] on or off");

        _parser = new PosixParser();
    }

    public void tearDown()
    {

    }

    public void testSimpleShort()
    {
        String[] args = new String[] { "-a",
                                       "-b", "toast",
                                       "foo", "bar" };

        try
        {
            CommandLine cl = _parser.parse(_options, args);
            
            assertTrue( "Confirm -a is set", cl.hasOption("a") );
            assertTrue( "Confirm -b is set", cl.hasOption("b") );
            assertTrue( "Confirm arg of -b", cl.getOptionValue("b").equals("toast") );
            assertTrue( "Confirm size of extra args", cl.getArgList().size() == 2);
        }
        catch (ParseException e)
        {
            fail( e.toString() );
        }
    }

    public void testSimpleLong()
    {
        String[] args = new String[] { "--enable-a",
                                       "--bfile", "toast",
                                       "foo", "bar" };

        try
        {
            CommandLine cl = _parser.parse(_options, args);
            
            assertTrue( "Confirm -a is set", cl.hasOption("a") );
            assertTrue( "Confirm -b is set", cl.hasOption("b") );
            assertTrue( "Confirm arg of -b", cl.getOptionValue("b").equals("toast") );
            assertTrue( "Confirm size of extra args", cl.getArgList().size() == 2);
        } 
        catch (ParseException e)
        {
            fail( e.toString() );
        }
    }

    public void testComplexShort()
    {
        String[] args = new String[] { "-acbtoast",
                                       "foo", "bar" };

        try
        {
            CommandLine cl = _parser.parse(_options, args);
            
            assertTrue( "Confirm -a is set", cl.hasOption("a") );
            assertTrue( "Confirm -b is set", cl.hasOption("b") );
            assertTrue( "Confirm -c is set", cl.hasOption("c") );
            assertTrue( "Confirm arg of -b", cl.getOptionValue("b").equals("toast") );
            assertTrue( "Confirm size of extra args", cl.getArgList().size() == 2);
        }
        catch (ParseException e)
        {
            fail( e.toString() );
        }
    }

    public void testExtraOption()
    {
        String[] args = new String[] { "-adbtoast",
                                       "foo", "bar" };

        boolean caught = false;

        try
        {
            CommandLine cl = _parser.parse(_options, args);
            
            assertTrue( "Confirm -a is set", cl.hasOption("a") );
            assertTrue( "Confirm -b is set", cl.hasOption("b") );
            assertTrue( "confirm arg of -b", cl.getOptionValue("b").equals("toast") );
            assertTrue( "Confirm size of extra args", cl.getArgList().size() == 3);
        }
        catch (UnrecognizedOptionException e)
        {
            caught = true;
        }
        catch (ParseException e)
        {
            fail( e.toString() );
        }
        assertTrue( "Confirm UnrecognizedOptionException caught", caught );
    }

    public void testMissingArg()
    {

        String[] args = new String[] { "-acb" };

        boolean caught = false;

        CommandLine cl = null;
        try
        {
            cl = _parser.parse(_options, args);
        }
        catch (MissingArgumentException e)
        {
            caught = true;
        }
        catch (ParseException e)
        {
            fail( e.toString() );
        }

        assertTrue( "Confirm MissingArgumentException caught " + cl, caught );
    }

    public void testStop()
    {
        String[] args = new String[] { "-c",
                                       "foober",
                                       "-btoast" };

        try
        {
            CommandLine cl = _parser.parse(_options, args, true);
            assertTrue( "Confirm -c is set", cl.hasOption("c") );
            assertTrue( "Confirm  2 extra args: " + cl.getArgList().size(), cl.getArgList().size() == 2);
        }
        catch (ParseException e)
        {
            fail( e.toString() );
        }
    }

    public void testMultiple()
    {
        String[] args = new String[] { "-c",
                                       "foobar",
                                       "-btoast" };

        try
        {
            CommandLine cl = _parser.parse(_options, args, true);
            assertTrue( "Confirm -c is set", cl.hasOption("c") );
            assertTrue( "Confirm  2 extra args: " + cl.getArgList().size(), cl.getArgList().size() == 2);

            cl = _parser.parse(_options, cl.getArgs() );

            assertTrue( "Confirm -c is not set", ! cl.hasOption("c") );
            assertTrue( "Confirm -b is set", cl.hasOption("b") );
            assertTrue( "Confirm arg of -b", cl.getOptionValue("b").equals("toast") );
            assertTrue( "Confirm  1 extra arg: " + cl.getArgList().size(), cl.getArgList().size() == 1);
            assertTrue( "Confirm  value of extra arg: " + cl.getArgList().get(0), cl.getArgList().get(0).equals("foobar") );
        }
        catch (ParseException e)
        {
            fail( e.toString() );
        }
    }

    public void testMultipleWithLong()
    {
        String[] args = new String[] { "--copt",
                                       "foobar",
                                       "--bfile", "toast" };

        try
        {
            CommandLine cl = _parser.parse(_options,args,
                                            true);
            assertTrue( "Confirm -c is set", cl.hasOption("c") );
            assertTrue( "Confirm  3 extra args: " + cl.getArgList().size(), cl.getArgList().size() == 3);

            cl = _parser.parse(_options, cl.getArgs() );

            assertTrue( "Confirm -c is not set", ! cl.hasOption("c") );
            assertTrue( "Confirm -b is set", cl.hasOption("b") );
            assertTrue( "Confirm arg of -b", cl.getOptionValue("b").equals("toast") );
            assertTrue( "Confirm  1 extra arg: " + cl.getArgList().size(), cl.getArgList().size() == 1);
            assertTrue( "Confirm  value of extra arg: " + cl.getArgList().get(0), cl.getArgList().get(0).equals("foobar") );
        }
        catch (ParseException e)
        {
            fail( e.toString() );
        }
    }

    public void testDoubleDash()
    {
        String[] args = new String[] { "--copt",
                                       "--",
                                       "-b", "toast" };

        try
        {
            CommandLine cl = _parser.parse(_options, args);

            assertTrue( "Confirm -c is set", cl.hasOption("c") );
            assertTrue( "Confirm -b is not set", ! cl.hasOption("b") );
            assertTrue( "Confirm 2 extra args: " + cl.getArgList().size(), cl.getArgList().size() == 2);

        }
        catch (ParseException e)
        {
            fail( e.toString() );
        }
    }

    public void testSingleDash()
    {
        String[] args = new String[] { "--copt",
                                       "-b", "-",
                                       "-a",
                                       "-" };

        try
        {
            CommandLine cl = _parser.parse(_options, args);

            assertTrue( "Confirm -a is set", cl.hasOption("a") );
            assertTrue( "Confirm -b is set", cl.hasOption("b") );
            assertTrue( "Confirm arg of -b", cl.getOptionValue("b").equals("-") );
            assertTrue( "Confirm 1 extra arg: " + cl.getArgList().size(), cl.getArgList().size() == 1);
            assertTrue( "Confirm value of extra arg: " + cl.getArgList().get(0), cl.getArgList().get(0).equals("-") );
        }
        catch (ParseException e)
        {
            fail( e.toString() );
        }
        
    }
}
