package org.python.util.install.driver;

import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.python.util.install.ConsoleInstaller;
import org.python.util.install.Installation;
import org.python.util.install.InstallerCommandLine;
import org.python.util.install.JavaHomeHandler;

public class InstallationDriver {
    private SilentAutotest[] _silentTests;
    private ConsoleAutotest[] _consoleTests;
    private GuiAutotest[] _guiTests;
    private InstallerCommandLine _commandLine;

    /**
     * construct the driver
     * 
     * @param commandLine the console arguments
     * 
     * @throws IOException
     * @throws DriverException
     */
    public InstallationDriver(InstallerCommandLine commandLine) throws DriverException {
        _commandLine = commandLine;
        try {
            buildSilentTests();
            buildConsoleTests();
            buildGuiTests();
        } catch (IOException ioe) {
            throw new DriverException(ioe);
        }
    }

    /**
     * execute all the automatic tests
     * 
     * @throws DriverException
     */
    public void drive() throws DriverException {
        try {
            // silent tests
            for (int i = 0; i < _silentTests.length; i++) {
                driveSilentTest(_silentTests[i]);
            }
            // console tests
            for (int i = 0; i < _consoleTests.length; i++) {
                driveConsoleTest(_consoleTests[i]);
            }
            // gui tests
            for (int i = 0; i < _guiTests.length; i++) {
                driveGuiTest(_guiTests[i]);
            }
        } catch (IOException ioe) {
            throw new DriverException(ioe);
        }
    }

    /**
     * execute a single console test
     * 
     * @param consoleTest
     * @throws DriverException
     * @throws IOException
     * @throws IOException
     */
    private void driveConsoleTest(ConsoleAutotest consoleTest) throws DriverException, IOException {
        Tunnel _tunnel;
        _tunnel = new Tunnel();
        // have to fork off the driver thread first
        ConsoleDriver driver = new ConsoleDriver(_tunnel, consoleTest.getAnswers());
        driver.start();
        // now do the installation
        Installation.driverMain(consoleTest.getCommandLineArgs(), consoleTest, _tunnel);
        _tunnel.close();
        validate(consoleTest);
    }

    /**
     * execute a single silent test
     * 
     * @param silentTest
     * @throws DriverException
     */
    private void driveSilentTest(SilentAutotest silentTest) throws DriverException {
        Installation.driverMain(silentTest.getCommandLineArgs(), silentTest, null); // only a thin wrapper
        validate(silentTest);
    }

    /**
     * execute a single gui test
     * 
     * @param guiTest
     * @throws DriverException
     */
    private void driveGuiTest(GuiAutotest guiTest) throws DriverException {
        Installation.driverMain(guiTest.getCommandLineArgs(), guiTest, null); // only a thin wrapper
        guiTest.execute();
        validate(guiTest);
    }

    /**
     * perform validations after the test was run
     * 
     * @param autoTest
     * @throws DriverException
     */
    private void validate(Autotest autoTest) throws DriverException {
        autoTest.assertTargetDirNotEmpty();
        if (autoTest.getVerifier() != null) {
            System.out.println("verifying installation - this can take a while ...");
            autoTest.getVerifier().verify();
            System.out.println("... installation ok.\n");
        }
    }

    /**
     * @return the command line the autotest session was started with
     */
    private InstallerCommandLine getOriginalCommandLine() {
        return _commandLine;
    }

    /**
     * build all the silent tests
     * 
     * @throws IOException
     * @throws DriverException
     */
    private void buildSilentTests() throws IOException, DriverException {
        List<SilentAutotest> silentTests = new ArrayList<SilentAutotest>(50);

        SilentAutotest test1 = new SilentAutotest(getOriginalCommandLine());
        String[] arguments = new String[] { "-s" };
        test1.setCommandLineArgs(arguments);
        test1.addAdditionalArguments(); // this also adds target directory
        test1.setVerifier(new NormalVerifier());
        silentTests.add(test1);

        SilentAutotest test2 = new SilentAutotest(getOriginalCommandLine());
        arguments = new String[] { "-s", "-t", "minimum" };
        test2.setCommandLineArgs(arguments);
        test2.addAdditionalArguments();
        test2.setVerifier(new NormalVerifier());
        silentTests.add(test2);

        SilentAutotest test3 = new SilentAutotest(getOriginalCommandLine());
        arguments = new String[] { "-s", "-t", "standalone" };
        test3.setCommandLineArgs(arguments);
        test3.addAdditionalArguments();
        test3.setVerifier(new StandaloneVerifier());
        silentTests.add(test3);

        // build array
        int size = silentTests.size();
        _silentTests = new SilentAutotest[size];
        Iterator<SilentAutotest> silentIterator = silentTests.iterator();
        for (int i = 0; i < size; i++) {
            _silentTests[i] = silentIterator.next();
        }
    }

    /**
     * build all the console tests
     * 
     * @throws IOException
     * @throws DriverException
     */
    private void buildConsoleTests() throws IOException, DriverException {
        List<ConsoleAutotest> consoleTests = new ArrayList<ConsoleAutotest>(5);
        final String[] arguments;
        if (getOriginalCommandLine().hasVerboseOption()) {
            arguments = new String[] { "-c", "-v" };
        } else {
            arguments = new String[] { "-c" };
        }
        // do NOT call addAdditionalArguments()

        ConsoleAutotest test1 = new ConsoleAutotest(getOriginalCommandLine());
        test1.setCommandLineArgs(arguments);
        test1.addAnswer("e"); // language
        test1.addAnswer("n"); // no read of license
        test1.addAnswer("y"); // accept license
        test1.addAnswer("3"); // type: minimum
        test1.addAnswer("n"); // include: nothing
        test1.addAnswer(test1.getTargetDir().getAbsolutePath()); // target directory
        addJavaAndOSAnswers(test1);
        test1.addAnswer("y"); // confirm copying
        test1.addAnswer("n"); // no readme
        test1.setVerifier(new NormalVerifier());
        consoleTests.add(test1);

        ConsoleAutotest test2 = new ConsoleAutotest(getOriginalCommandLine());
        test2.setCommandLineArgs(arguments);
        test2.addAnswer("e"); // language
        test2.addAnswer("n"); // no read of license
        test2.addAnswer("y"); // accept license
        test2.addAnswer("3"); // type: minimum
        test2.addAnswer("y"); // include
        test2.addAnswer("mod"); // include
        test2.addAnswer("demo"); // include
        test2.addAnswer("src"); // include
        test2.addAnswer("n"); // no further includes
        test2.addAnswer("y"); // exclude
        test2.addAnswer("demo"); // exclude
        test2.addAnswer("wrongAnswer"); // wrong answer
        test2.addAnswer("n"); // no further excludes
        test2.addAnswer(test2.getTargetDir().getAbsolutePath()); // target directory
        addJavaAndOSAnswers(test2);
        test2.addAnswer("y"); // confirm copying
        test2.addAnswer("n"); // no readme
        test2.setVerifier(new NormalVerifier());
        consoleTests.add(test2);

        ConsoleAutotest test3 = new ConsoleAutotest(getOriginalCommandLine());
        test3.setCommandLineArgs(arguments);
        test3.addAnswer("e"); // language
        test3.addAnswer("n"); // no read of license
        test3.addAnswer("y"); // accept license
        test3.addAnswer("9"); // type: standalone
        test3.addAnswer(test3.getTargetDir().getAbsolutePath()); // target directory
        addJavaAndOSAnswers(test3);
        test3.addAnswer("y"); // confirm copying
        test3.addAnswer("n"); // no readme
        test3.setVerifier(new StandaloneVerifier());
        consoleTests.add(test3);
        
        // test for bug 1783960
        ConsoleAutotest test4 = new ConsoleAutotest(getOriginalCommandLine());
        test4.setCommandLineArgs(arguments);
        test4.addAnswer("e"); // language
        test4.addAnswer("n"); // no read of license
        test4.addAnswer("y"); // accept license
        test4.addAnswer("2"); // type: standard
        test4.addAnswer("n"); // no includes
        test4.addAnswer("y"); // exclude
        test4.addAnswer("n"); // no further excludes
        test4.addAnswer(test4.getTargetDir().getAbsolutePath()); // target directory
        addJavaAndOSAnswers(test4);
        test4.addAnswer("y"); // confirm copying
        test4.addAnswer("n"); // no readme
        test4.setVerifier(new NormalVerifier());
        consoleTests.add(test4);
        
        // build array
        int size = consoleTests.size();
        _consoleTests = new ConsoleAutotest[size];
        Iterator<ConsoleAutotest> consoleIterator = consoleTests.iterator();
        for (int i = 0; i < size; i++) {
            _consoleTests[i] = consoleIterator.next();
        }
    }

    private void addJavaAndOSAnswers(ConsoleAutotest test) {
        JavaHomeHandler javaHomeHandler = test.getJavaHomeHandler();
        if (javaHomeHandler.isDeviation() && javaHomeHandler.isValidHome()) {
            test.addAnswer(javaHomeHandler.getHome().getAbsolutePath()); // different jre
        } else {
            test.addAnswer(ConsoleInstaller.CURRENT_JRE);
        }
        if (!Installation.isValidOs()) {
            test.addAnswer(""); // enter to proceed anyway
        }
    }

    /**
     * build all the gui tests
     * 
     * @throws IOException
     * @throws DriverException
     */
    private void buildGuiTests() throws IOException, DriverException {
        List<GuiAutotest> guiTests = new ArrayList<GuiAutotest>(50);

        if (Installation.isGuiAllowed()) {
            GuiAutotest guiTest1 = new GuiAutotest(getOriginalCommandLine());
            buildLanguageAndLicensePage(guiTest1);
            // type page - use 'Standard'
            guiTest1.addKeyAction(KeyEvent.VK_TAB);
            guiTest1.addKeyAction(KeyEvent.VK_TAB);
            guiTest1.addKeyAction(KeyEvent.VK_TAB);
            guiTest1.addKeyAction(KeyEvent.VK_TAB);
            guiTest1.addKeyAction(KeyEvent.VK_TAB);
            guiTest1.addKeyAction(KeyEvent.VK_TAB);
            guiTest1.addKeyAction(KeyEvent.VK_SPACE);
            buildRestOfGuiPages(guiTest1);
            guiTest1.setVerifier(new NormalVerifier());
            guiTests.add(guiTest1);

            GuiAutotest guiTest2 = new GuiAutotest(getOriginalCommandLine());
            buildLanguageAndLicensePage(guiTest2);
            // type page - use 'All'
            guiTest2.addKeyAction(KeyEvent.VK_TAB);
            guiTest2.addKeyAction(KeyEvent.VK_TAB);
            guiTest2.addKeyAction(KeyEvent.VK_TAB);
            guiTest2.addKeyAction(KeyEvent.VK_TAB);
            guiTest2.addKeyAction(KeyEvent.VK_TAB);
            guiTest2.addKeyAction(KeyEvent.VK_TAB);
            guiTest2.addKeyAction(KeyEvent.VK_TAB);
            guiTest2.addKeyAction(KeyEvent.VK_SPACE); // select 'All'
            guiTest2.addKeyAction(KeyEvent.VK_TAB);
            guiTest2.addKeyAction(KeyEvent.VK_TAB);
            guiTest2.addKeyAction(KeyEvent.VK_TAB);
            guiTest2.addKeyAction(KeyEvent.VK_TAB);
            guiTest2.addKeyAction(KeyEvent.VK_TAB);
            guiTest2.addKeyAction(KeyEvent.VK_TAB);
            guiTest2.addKeyAction(KeyEvent.VK_TAB);
            guiTest2.addKeyAction(KeyEvent.VK_SPACE);
            buildRestOfGuiPages(guiTest2);
            guiTest2.setVerifier(new NormalVerifier());
            guiTests.add(guiTest2);

            GuiAutotest guiTest3 = new GuiAutotest(getOriginalCommandLine());
            buildLanguageAndLicensePage(guiTest3);
            // type page - use 'Custom'
            guiTest3.addKeyAction(KeyEvent.VK_TAB);
            guiTest3.addKeyAction(KeyEvent.VK_TAB);
            guiTest3.addKeyAction(KeyEvent.VK_TAB);
            guiTest3.addKeyAction(KeyEvent.VK_SPACE); // select 'Custom'
            guiTest3.addKeyAction(KeyEvent.VK_TAB);
            guiTest3.addKeyAction(KeyEvent.VK_TAB);
            guiTest3.addKeyAction(KeyEvent.VK_SPACE); // deselect 'Demos and Examples'
            guiTest3.addKeyAction(KeyEvent.VK_TAB);
            guiTest3.addKeyAction(KeyEvent.VK_SPACE); // deselect 'Documentation'
            guiTest3.addKeyAction(KeyEvent.VK_TAB);
            guiTest3.addKeyAction(KeyEvent.VK_SPACE); // select 'Sources'
            guiTest3.addKeyAction(KeyEvent.VK_TAB);
            guiTest3.addKeyAction(KeyEvent.VK_TAB);
            guiTest3.addKeyAction(KeyEvent.VK_TAB);
            guiTest3.addKeyAction(KeyEvent.VK_SPACE);
            buildRestOfGuiPages(guiTest3);
            guiTest3.setVerifier(new NormalVerifier());
            guiTests.add(guiTest3);

            GuiAutotest guiTest4 = new GuiAutotest(getOriginalCommandLine());
            buildLanguageAndLicensePage(guiTest4);
            // type page - use 'Standalone'
            guiTest4.addKeyAction(KeyEvent.VK_TAB);
            guiTest4.addKeyAction(KeyEvent.VK_TAB);
            guiTest4.addKeyAction(KeyEvent.VK_SPACE); // select 'Standalone'
            guiTest4.addKeyAction(KeyEvent.VK_TAB);
            guiTest4.addKeyAction(KeyEvent.VK_TAB);
            guiTest4.addKeyAction(KeyEvent.VK_TAB);
            guiTest4.addKeyAction(KeyEvent.VK_TAB);
            guiTest4.addKeyAction(KeyEvent.VK_SPACE);
            buildRestOfGuiPages(guiTest4);
            guiTest4.setVerifier(new StandaloneVerifier());
            guiTests.add(guiTest4);
        }

        // build array
        int size = guiTests.size();
        _guiTests = new GuiAutotest[size];
        Iterator<GuiAutotest> guiIterator = guiTests.iterator();
        for (int i = 0; i < size; i++) {
            _guiTests[i] = guiIterator.next();
        }
    }

    private void buildLanguageAndLicensePage(GuiAutotest guiTest) {
        // language page
        guiTest.addKeyAction(KeyEvent.VK_E);
        guiTest.addKeyAction(KeyEvent.VK_TAB);
        guiTest.addKeyAction(KeyEvent.VK_TAB);
        guiTest.addKeyAction(KeyEvent.VK_SPACE);
        // license page
        guiTest.addKeyAction(KeyEvent.VK_TAB);
        guiTest.addKeyAction(KeyEvent.VK_TAB);
        guiTest.addKeyAction(KeyEvent.VK_TAB);
        guiTest.addKeyAction(KeyEvent.VK_TAB);
        guiTest.addKeyAction(KeyEvent.VK_TAB);
        guiTest.addKeyAction(KeyEvent.VK_SPACE); // select "i accept"
        guiTest.addKeyAction(KeyEvent.VK_TAB);
        guiTest.addKeyAction(KeyEvent.VK_TAB);
        guiTest.addKeyAction(KeyEvent.VK_TAB);
        guiTest.addKeyAction(KeyEvent.VK_TAB);
        guiTest.addKeyAction(KeyEvent.VK_SPACE);
    }

    private void buildRestOfGuiPages(GuiAutotest guiTest) {
        // directory page
        guiTest.addKeyAction(KeyEvent.VK_TAB);
        guiTest.addKeyAction(KeyEvent.VK_TAB);
        guiTest.addKeyAction(KeyEvent.VK_TAB);
        guiTest.addKeyAction(KeyEvent.VK_TAB);
        guiTest.addKeyAction(KeyEvent.VK_SPACE);
        // java selection page
        guiTest.addKeyAction(KeyEvent.VK_TAB);
        guiTest.addKeyAction(KeyEvent.VK_TAB);
        guiTest.addKeyAction(KeyEvent.VK_TAB);
        guiTest.addKeyAction(KeyEvent.VK_TAB);
        JavaHomeHandler javaHomeHandler = guiTest.getJavaHomeHandler();
        boolean isValidDeviation = javaHomeHandler.isDeviation() && javaHomeHandler.isValidHome(); 
        if (isValidDeviation) { // need 2 more tabs
            guiTest.addKeyAction(KeyEvent.VK_TAB);
            guiTest.addKeyAction(KeyEvent.VK_TAB);
        }
        guiTest.addKeyAction(KeyEvent.VK_SPACE);
        // overview page
        if (isValidDeviation) {
            guiTest.addKeyAction(KeyEvent.VK_SPACE, 3000); // enough time to check the java version
        } else {
            guiTest.addKeyAction(KeyEvent.VK_SPACE);
        }
        // installation page (skipped)
        // readme page
        guiTest.addWaitingKeyAction(KeyEvent.VK_TAB); // wait for the installation to finish
        guiTest.addKeyAction(KeyEvent.VK_SPACE);
        // success page
        guiTest.addKeyAction(KeyEvent.VK_SPACE);
    }
}
