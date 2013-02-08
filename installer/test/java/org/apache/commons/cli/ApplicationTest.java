package org.apache.commons.cli;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * <p>
 * This is a collection of tests that test real world
 * applications command lines.
 * </p>
 * 
 * <p>
 * The following are the applications that are tested:
 * <ul>
 * <li>Ant</li>
 * </ul>
 * </p>
 *
 * @author John Keyes (john at integralsource.com)
 */
public class ApplicationTest extends TestCase {

    public static Test suite() { 
        return new TestSuite(ApplicationTest.class); 
    }

    public ApplicationTest(String name)
    {
        super(name);
    }
    
    /**
     *	
     */
    public void testLs() {
        // create the command line parser
        CommandLineParser parser = new PosixParser();
        Options options = new Options();
        options.addOption( "a", "all", false, "do not hide entries starting with ." );
        options.addOption( "A", "almost-all", false, "do not list implied . and .." );
        options.addOption( "b", "escape", false, "print octal escapes for nongraphic characters" );
        OptionBuilder.withLongOpt( "block-size" );
        OptionBuilder.withDescription( "use SIZE-byte blocks" );
        OptionBuilder.withValueSeparator( '=' );
        OptionBuilder.hasArg();
        options.addOption(OptionBuilder.create());
//        options.addOption( OptionBuilder.withLongOpt( "block-size" )
//                .withDescription( "use SIZE-byte blocks" )
//                .withValueSeparator( '=' )
//                .hasArg()
//                .create() );
        options.addOption( "B", "ignore-backups", false, "do not list implied entried ending with ~");
        options.addOption( "c", false, "with -lt: sort by, and show, ctime (time of last modification of file status information) with -l:show ctime and sort by name otherwise: sort by ctime" );
        options.addOption( "C", false, "list entries by columns" );

        String[] args = new String[]{ "--block-size=10" };

        try {
            CommandLine line = parser.parse( options, args );
            assertTrue( line.hasOption( "block-size" ) );
            assertEquals( line.getOptionValue( "block-size" ), "10" );
        }
        catch( ParseException exp ) {
            fail( "Unexpected exception:" + exp.getMessage() );
        }
    }

    /**
     * Ant test
     */
    public void testAnt() {
        // use the GNU parser
        CommandLineParser parser = new GnuParser( );
        Options options = new Options();
        options.addOption( "help", false, "print this message" );
        options.addOption( "projecthelp", false, "print project help information" );
        options.addOption( "version", false, "print the version information and exit" );
        options.addOption( "quiet", false, "be extra quiet" );
        options.addOption( "verbose", false, "be extra verbose" );
        options.addOption( "debug", false, "print debug information" );
        options.addOption( "version", false, "produce logging information without adornments" );
        options.addOption( "logfile", true, "use given file for log" );
        options.addOption( "logger", true, "the class which is to perform the logging" );
        options.addOption( "listener", true, "add an instance of a class as a project listener" );
        options.addOption( "buildfile", true, "use given buildfile" );
        OptionBuilder.withDescription( "use value for given property" );
        OptionBuilder.hasArgs();
        OptionBuilder.withValueSeparator();
        options.addOption( OptionBuilder.create( 'D' ) );
                           //, null, true, , false, true );
        options.addOption( "find", true, "search for buildfile towards the root of the filesystem and use it" );

        String[] args = new String[]{ "-buildfile", "mybuild.xml",
            "-Dproperty=value", "-Dproperty1=value1",
            "-projecthelp" };

        try {
            CommandLine line = parser.parse( options, args );

            // check multiple values
            String[] opts = line.getOptionValues( "D" );
            assertEquals( "property", opts[0] );
            assertEquals( "value", opts[1] );
            assertEquals( "property1", opts[2] );
            assertEquals( "value1", opts[3] );

            // check single value
            assertEquals( line.getOptionValue( "buildfile"), "mybuild.xml" );

            // check option
            assertTrue( line.hasOption( "projecthelp") );
        }
        catch( ParseException exp ) {
            fail( "Unexpected exception:" + exp.getMessage() );
        }

    }

}