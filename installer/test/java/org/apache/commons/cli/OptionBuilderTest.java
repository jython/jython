package org.apache.commons.cli;

import java.math.BigDecimal;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import junit.textui.TestRunner;

public class OptionBuilderTest extends TestCase {

    public OptionBuilderTest( String name ) {
        super( name );
    }

    public static Test suite() { 
        return new TestSuite( OptionBuilderTest.class ); 
    }

    public static void main( String args[] ) { 
        TestRunner.run( suite() );
    }

    public void testCompleteOption( ) {
        OptionBuilder.withLongOpt( "simple option");
        OptionBuilder.hasArg( );
        OptionBuilder.isRequired( );
        OptionBuilder.hasArgs( );
        OptionBuilder.withType( new BigDecimal( "10" ) );
        OptionBuilder.withDescription( "this is a simple option" );
        Option simple = OptionBuilder.create( 's' );

        assertEquals( "s", simple.getOpt() );
        assertEquals( "simple option", simple.getLongOpt() );
        assertEquals( "this is a simple option", simple.getDescription() );
        assertEquals( simple.getType().getClass(), BigDecimal.class );
        assertTrue( simple.hasArg() );
        assertTrue( simple.isRequired() );
        assertTrue( simple.hasArgs() );
    }

    public void testTwoCompleteOptions( ) {
        OptionBuilder.withLongOpt( "simple option");
        OptionBuilder.hasArg( );
        OptionBuilder.isRequired( );
        OptionBuilder.hasArgs( );
        OptionBuilder.withType( new BigDecimal( "10" ) );
        OptionBuilder.withDescription( "this is a simple option" );
        Option simple = OptionBuilder.create( 's' );

        assertEquals( "s", simple.getOpt() );
        assertEquals( "simple option", simple.getLongOpt() );
        assertEquals( "this is a simple option", simple.getDescription() );
        assertEquals( simple.getType().getClass(), BigDecimal.class );
        assertTrue( simple.hasArg() );
        assertTrue( simple.isRequired() );
        assertTrue( simple.hasArgs() );

        OptionBuilder.withLongOpt( "dimple option");
        OptionBuilder.hasArg( );
        OptionBuilder.withDescription( "this is a dimple option" );
        simple = OptionBuilder.create( 'd' );

        assertEquals( "d", simple.getOpt() );
        assertEquals( "dimple option", simple.getLongOpt() );
        assertEquals( "this is a dimple option", simple.getDescription() );
        assertNull( simple.getType() );
        assertTrue( simple.hasArg() );
        assertTrue( !simple.isRequired() );
        assertTrue( !simple.hasArgs() );
    }

    public void testBaseOptionCharOpt() {
        OptionBuilder.withDescription( "option description");
        Option base = OptionBuilder.create( 'o' );

        assertEquals( "o", base.getOpt() );
        assertEquals( "option description", base.getDescription() );
        assertTrue( !base.hasArg() );
    }

    public void testBaseOptionStringOpt() {
        OptionBuilder.withDescription( "option description");
        Option base = OptionBuilder.create( "o" );

        assertEquals( "o", base.getOpt() );
        assertEquals( "option description", base.getDescription() );
        assertTrue( !base.hasArg() );
    }

    public void testSpecialOptChars() {

        // '?'
        try {
            OptionBuilder.withDescription( "help options" );
            Option opt = OptionBuilder.create( '?' );
            assertEquals( "?", opt.getOpt() );
        }
        catch( IllegalArgumentException arg ) {
            fail( "IllegalArgumentException caught" );
        }

        // '@'
        try {
            OptionBuilder.withDescription( "read from stdin" );
            Option opt = OptionBuilder.create( '@' );
            assertEquals( "@", opt.getOpt() );
        }
        catch( IllegalArgumentException arg ) {
            fail( "IllegalArgumentException caught" );
        }
    }

    public void testOptionArgNumbers() {
        OptionBuilder.withDescription( "option description" );
        OptionBuilder.hasArgs( 2 );
        Option opt = OptionBuilder.create( 'o' );
        assertEquals( 2, opt.getArgs() );
    }

    public void testIllegalOptions() {
        // bad single character option
        try {
            OptionBuilder.withDescription( "option description" );
            OptionBuilder.create( '"' );
            fail( "IllegalArgumentException not caught" );
        }
        catch( IllegalArgumentException exp ) {
            // success
        }

        // bad character in option string
        try {
            OptionBuilder.create( "opt`" );
            fail( "IllegalArgumentException not caught" );
        }
        catch( IllegalArgumentException exp ) {
            // success
        }

        // null option
        try {
            OptionBuilder.create( null );
            fail( "IllegalArgumentException not caught" );
        }
        catch( IllegalArgumentException exp ) {
            // success
        }

        // valid option 
        try {
            OptionBuilder.create( "opt" );
            // success
        }
        catch( IllegalArgumentException exp ) {
            fail( "IllegalArgumentException caught" );
        }
    }
}