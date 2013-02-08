/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 * 
 * $Id: ValueTest.java 3134 2007-03-02 07:20:08Z otmarhumbel $
 */

package org.apache.commons.cli;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class ValueTest extends TestCase
{

    public static Test suite() { 
        return new TestSuite(ValueTest.class); 
    }

    private CommandLine _cl = null;
    private Options opts = new Options();

    public ValueTest(String name)
    {
        super(name);
    }

    public void setUp()
    {
        opts.addOption("a",
                       false,
                       "toggle -a");

        opts.addOption("b",
                       true,
                       "set -b");

        opts.addOption("c",
                       "c",
                       false,
                       "toggle -c");

        opts.addOption("d",
                       "d",
                       true,
                       "set -d");

        OptionBuilder.hasOptionalArg();
        opts.addOption( OptionBuilder.create( 'e') );

        OptionBuilder.hasOptionalArg();
        OptionBuilder.withLongOpt( "fish" );
        opts.addOption( OptionBuilder.create( ) );

        OptionBuilder.hasOptionalArgs();
        OptionBuilder.withLongOpt( "gravy" );
        opts.addOption( OptionBuilder.create( ) );

        OptionBuilder.hasOptionalArgs( 2 );
        OptionBuilder.withLongOpt( "hide" );
        opts.addOption( OptionBuilder.create( ) );

        OptionBuilder.hasOptionalArgs( 2 );
        opts.addOption( OptionBuilder.create( 'i' ) );

        OptionBuilder.hasOptionalArgs( );
        opts.addOption( OptionBuilder.create( 'j' ) );

        String[] args = new String[] { "-a",
            "-b", "foo",
            "--c",
            "--d", "bar" 
        };

        try
        {
            CommandLineParser parser = new PosixParser();
            _cl = parser.parse(opts,args);
        }
        catch (ParseException e)
        {
            fail("Cannot setUp() CommandLine: " + e.toString());
        }
    }

    public void tearDown()
    {

    }

    public void testShortNoArg()
    {
        assertTrue( _cl.hasOption("a") );
        assertNull( _cl.getOptionValue("a") );
    }

    public void testShortWithArg()
    {
        assertTrue( _cl.hasOption("b") );
        assertNotNull( _cl.getOptionValue("b") );
        assertEquals( _cl.getOptionValue("b"), "foo");
    }

    public void testLongNoArg()
    {
        assertTrue( _cl.hasOption("c") );
        assertNull( _cl.getOptionValue("c") );
    }

    public void testLongWithArg()
    {
        assertTrue( _cl.hasOption("d") );
        assertNotNull( _cl.getOptionValue("d") );
        assertEquals( _cl.getOptionValue("d"), "bar");
    }

    public void testShortOptionalArgNoValue()
    {
        String[] args = new String[] { "-e"
        };
        try
        {
            CommandLineParser parser = new PosixParser();
            CommandLine cmd = parser.parse(opts,args);
            assertTrue( cmd.hasOption("e") );
            assertNull( cmd.getOptionValue("e") );
        }
        catch (ParseException e)
        {
            fail("Cannot setUp() CommandLine: " + e.toString());
        }
    }

    public void testShortOptionalArgValue()
    {
        String[] args = new String[] { "-e", "everything"
        };
        try
        {
            CommandLineParser parser = new PosixParser();
            CommandLine cmd = parser.parse(opts,args);
            assertTrue( cmd.hasOption("e") );
            assertEquals( "everything", cmd.getOptionValue("e") );
        }
        catch (ParseException e)
        {
            fail("Cannot setUp() CommandLine: " + e.toString());
        }
    }

    public void testLongOptionalNoValue()
    {
        String[] args = new String[] { "--fish"
        };
        try
        {
            CommandLineParser parser = new PosixParser();
            CommandLine cmd = parser.parse(opts,args);
            assertTrue( cmd.hasOption("fish") );
            assertNull( cmd.getOptionValue("fish") );
        }
        catch (ParseException e)
        {
            fail("Cannot setUp() CommandLine: " + e.toString());
        }
    }

    public void testLongOptionalArgValue()
    {
        String[] args = new String[] { "--fish", "face"
        };
        try
        {
            CommandLineParser parser = new PosixParser();
            CommandLine cmd = parser.parse(opts,args);
            assertTrue( cmd.hasOption("fish") );
            assertEquals( "face", cmd.getOptionValue("fish") );
        }
        catch (ParseException e)
        {
            fail("Cannot setUp() CommandLine: " + e.toString());
        }
    }

    public void testShortOptionalArgValues()
    {
        String[] args = new String[] { "-j", "ink", "idea"
        };
        try
        {
            CommandLineParser parser = new PosixParser();
            CommandLine cmd = parser.parse(opts,args);
            assertTrue( cmd.hasOption("j") );
            assertEquals( "ink", cmd.getOptionValue("j") );
            assertEquals( "ink", cmd.getOptionValues("j")[0] );
            assertEquals( "idea", cmd.getOptionValues("j")[1] );
            assertEquals( cmd.getArgs().length, 0 );
        }
        catch (ParseException e)
        {
            fail("Cannot setUp() CommandLine: " + e.toString());
        }
    }

    public void testLongOptionalArgValues()
    {
        String[] args = new String[] { "--gravy", "gold", "garden"
        };
        try
        {
            CommandLineParser parser = new PosixParser();
            CommandLine cmd = parser.parse(opts,args);
            assertTrue( cmd.hasOption("gravy") );
            assertEquals( "gold", cmd.getOptionValue("gravy") );
            assertEquals( "gold", cmd.getOptionValues("gravy")[0] );
            assertEquals( "garden", cmd.getOptionValues("gravy")[1] );
            assertEquals( cmd.getArgs().length, 0 );
        }
        catch (ParseException e)
        {
            fail("Cannot setUp() CommandLine: " + e.toString());
        }
    }

    public void testShortOptionalNArgValues()
    {
        String[] args = new String[] { "-i", "ink", "idea", "isotope", "ice"
        };
        try
        {
            CommandLineParser parser = new PosixParser();
            CommandLine cmd = parser.parse(opts,args);
            assertTrue( cmd.hasOption("i") );
            assertEquals( "ink", cmd.getOptionValue("i") );
            assertEquals( "ink", cmd.getOptionValues("i")[0] );
            assertEquals( "idea", cmd.getOptionValues("i")[1] );
            assertEquals( cmd.getArgs().length, 2 );
            assertEquals( "isotope", cmd.getArgs()[0] );
            assertEquals( "ice", cmd.getArgs()[1] );
        }
        catch (ParseException e)
        {
            fail("Cannot setUp() CommandLine: " + e.toString());
        }
    }

    public void testLongOptionalNArgValues()
    {
        String[] args = new String[] { "--hide", "house", "hair", "head"
        };
        try
        {
            CommandLineParser parser = new PosixParser();
            CommandLine cmd = parser.parse(opts,args);
            assertTrue( cmd.hasOption("hide") );
            assertEquals( "house", cmd.getOptionValue("hide") );
            assertEquals( "house", cmd.getOptionValues("hide")[0] );
            assertEquals( "hair", cmd.getOptionValues("hide")[1] );
            assertEquals( cmd.getArgs().length, 1 );
            assertEquals( "head", cmd.getArgs()[0] );
        }
        catch (ParseException e)
        {
            fail("Cannot setUp() CommandLine: " + e.toString());
        }
    }
}
