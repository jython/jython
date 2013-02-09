/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 * 
 * $Id: BugsTest.java 3134 2007-03-02 07:20:08Z otmarhumbel $
 */

package org.apache.commons.cli;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class BugsTest extends TestCase
{

    public static Test suite() { 
        return new TestSuite( BugsTest.class );
    }

    public BugsTest( String name )
    {
        super( name );
    }

    public void setUp()
    {
    }

    public void tearDown()
    {
    }

    public void test11457() {
        Options options = new Options();
        OptionBuilder.withLongOpt( "verbose" );
        options.addOption( OptionBuilder.create() );
        String[] args = new String[] { "--verbose" };

        CommandLineParser parser = new PosixParser();

        try {
            CommandLine cmd = parser.parse( options, args );
            assertTrue( cmd.hasOption( "verbose" ) );
        }        
        catch( ParseException exp ) {
            exp.printStackTrace();
            fail( "Unexpected Exception: " + exp.getMessage() );
        }
    }

    public void test11458()
    {
        Options options = new Options();
        OptionBuilder.withValueSeparator( '=' );
        OptionBuilder.hasArgs();
        options.addOption(OptionBuilder.create( 'D' ) );
        OptionBuilder.withValueSeparator( ':' );
        OptionBuilder.hasArgs();
        options.addOption( OptionBuilder.create( 'p' ) );
        String[] args = new String[] { "-DJAVA_HOME=/opt/java" ,
        "-pfile1:file2:file3" };

        CommandLineParser parser = new PosixParser();

        try {
            CommandLine cmd = parser.parse( options, args );

            String[] values = cmd.getOptionValues( 'D' );

            assertEquals( values[0], "JAVA_HOME" );
            assertEquals( values[1], "/opt/java" );

            values = cmd.getOptionValues( 'p' );

            assertEquals( values[0], "file1" );
            assertEquals( values[1], "file2" );
            assertEquals( values[2], "file3" );

            java.util.Iterator iter = cmd.iterator();
            while( iter.hasNext() ) {
                Option opt = (Option)iter.next();
                switch( opt.getId() ) {
                    case 'D':
                        assertEquals( opt.getValue( 0 ), "JAVA_HOME" );
                        assertEquals( opt.getValue( 1 ), "/opt/java" );
                        break;
                    case 'p':
                        assertEquals( opt.getValue( 0 ), "file1" );
                        assertEquals( opt.getValue( 1 ), "file2" );
                        assertEquals( opt.getValue( 2 ), "file3" );
                        break;
                    default:
                        fail( "-D option not found" );
                }
            }
        }
        catch( ParseException exp ) {
            fail( "Unexpected Exception:\nMessage:" + exp.getMessage() 
                  + "Type: " + exp.getClass().getName() );
        }
    }

    public void test11680()
    {
        Options options = new Options();
        options.addOption("f", true, "foobar");
	options.addOption("m", true, "missing");
        String[] args = new String[] { "-f" , "foo" };

        CommandLineParser parser = new PosixParser();

        try {
            CommandLine cmd = parser.parse( options, args );

            try {
                cmd.getOptionValue( "f", "default f");
                cmd.getOptionValue( "m", "default m");
            }
            catch( NullPointerException exp ) {
                fail( "NullPointer caught: " + exp.getMessage() );
            }
        }
        catch( ParseException exp ) {
            fail( "Unexpected Exception: " + exp.getMessage() );
        }
    }

    public void test11456()
    {
        // Posix 
        Options options = new Options();
        OptionBuilder.hasOptionalArg();
        options.addOption( OptionBuilder.create( 'a' ) );
        OptionBuilder.hasArg();
        options.addOption( OptionBuilder.create( 'b' ) );
        String[] args = new String[] { "-a", "-bvalue" };

        CommandLineParser parser = new PosixParser();

        try {
            CommandLine cmd = parser.parse( options, args );
            assertEquals( cmd.getOptionValue( 'b' ), "value" );
        }
        catch( ParseException exp ) {
            fail( "Unexpected Exception: " + exp.getMessage() );
        }

        // GNU
        options = new Options();
        OptionBuilder.hasOptionalArg();
        options.addOption( OptionBuilder.create( 'a' ) );
        OptionBuilder.hasArg();
        options.addOption( OptionBuilder.create( 'b' ) );
        args = new String[] { "-a", "-b", "value" };

        parser = new GnuParser();

        try {
            CommandLine cmd = parser.parse( options, args );
            assertEquals( cmd.getOptionValue( 'b' ), "value" );
        }
        catch( ParseException exp ) {
            fail( "Unexpected Exception: " + exp.getMessage() );
        }

    }

    public void test12210() {
        // create the main options object which will handle the first parameter
        Options mainOptions = new Options();
        // There can be 2 main exclusive options:  -exec|-rep

        // Therefore, place them in an option group

        String[] argv = new String[] { "-exec", "-exec_opt1", "-exec_opt2" };
        OptionGroup grp = new OptionGroup();

        grp.addOption(new Option("exec",false,"description for this option"));

        grp.addOption(new Option("rep",false,"description for this option"));

        mainOptions.addOptionGroup(grp);

        // for the exec option, there are 2 options...
        Options execOptions = new Options();
        execOptions.addOption("exec_opt1",false," desc");
        execOptions.addOption("exec_opt2",false," desc");

        // similarly, for rep there are 2 options...
        Options repOptions = new Options();
        repOptions.addOption("repopto",false,"desc");
        repOptions.addOption("repoptt",false,"desc");

        // create the parser
        GnuParser parser = new GnuParser();

        // finally, parse the arguments:

        // first parse the main options to see what the user has specified
        // We set stopAtNonOption to true so it does not touch the remaining
        // options
        try {
            CommandLine cmd = parser.parse(mainOptions,argv,true);
            // get the remaining options...
            argv = cmd.getArgs();

            if(cmd.hasOption("exec")){
                cmd = parser.parse(execOptions,argv,false);
                // process the exec_op1 and exec_opt2...
                assertTrue( cmd.hasOption("exec_opt1") );
                assertTrue( cmd.hasOption("exec_opt2") );
            }
            else if(cmd.hasOption("rep")){
                cmd = parser.parse(repOptions,argv,false);
                // process the rep_op1 and rep_opt2...
            }
            else {
                fail( "exec option not found" );
            }
        }
        catch( ParseException exp ) {
            fail( "Unexpected exception: " + exp.getMessage() );
        }
    }

    public void test13425() {
        Options options = new Options();
        OptionBuilder.withLongOpt( "old-password" );
        OptionBuilder.withDescription( "Use this option to specify the old password" );
        OptionBuilder.hasArg();
        Option oldpass = OptionBuilder.create( 'o' );
        OptionBuilder.withLongOpt( "new-password" );
        OptionBuilder.withDescription( "Use this option to specify the new password" );
        OptionBuilder.hasArg();
        Option newpass = OptionBuilder.create( 'n' );

        String[] args = { 
            "-o", 
            "-n", 
            "newpassword" 
        };

        options.addOption( oldpass );
        options.addOption( newpass );

        Parser parser = new PosixParser();

        CommandLine line = null;
        try {
            line = parser.parse( options, args );
        }
        // catch the exception and leave the method
        catch( Exception exp ) {
            assertTrue( exp != null );
            return;
        }
        fail( "MissingArgumentException not caught." + line);
    }

    public void test13666() {
        Options options = new Options();
        OptionBuilder.withDescription( "dir" );
        OptionBuilder.hasArg();
        Option dir = OptionBuilder.create( 'd' );
        options.addOption( dir );
        try {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp( "dir", options );
        }
        catch( Exception exp ) {
            fail( "Unexpected Exception: " + exp.getMessage() );
        }
    }

    public void test13935() {
        OptionGroup directions = new OptionGroup();

        Option left = new Option( "l", "left", false, "go left" );
        Option right = new Option( "r", "right", false, "go right" );
        Option straight = new Option( "s", "straight", false, "go straight" );
        Option forward = new Option( "f", "forward", false, "go forward" );
        forward.setRequired( true );

        directions.addOption( left );
        directions.addOption( right );
        directions.setRequired( true );

        Options opts = new Options();
        opts.addOptionGroup( directions );
        opts.addOption( straight );

        CommandLineParser parser = new PosixParser();
        boolean exception = false;

        CommandLine line = null;
        String[] args = new String[] {  };
        try {
            line = parser.parse( opts, args );
        }
        catch( ParseException exp ) {
            exception = true;
        }

        if( !exception ) {
            fail( "Expected exception not caught.");
        }

        exception = false;

        args = new String[] { "-s" };
        try {
            line = parser.parse( opts, args );
        }
        catch( ParseException exp ) {
            exception = true;
        }

        if( !exception ) {
            fail( "Expected exception not caught.");
        }

        exception = false;

        args = new String[] { "-s", "-l" };
        try {
            line = parser.parse( opts, args );
        }
        catch( ParseException exp ) {
            fail( "Unexpected exception: " + exp.getMessage() );
        }

        opts.addOption( forward );
        args = new String[] { "-s", "-l", "-f" };
        try {
            line = parser.parse( opts, args );
        }
        catch( ParseException exp ) {
            fail( "Unexpected exception: " + exp.getMessage() );
        }
        if(line != null) {
            line = null;
        }
    }
}
