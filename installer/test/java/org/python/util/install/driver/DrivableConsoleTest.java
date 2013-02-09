package org.python.util.install.driver;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.python.util.install.driver.ConsoleDriver;
import org.python.util.install.driver.Tunnel;

import junit.framework.TestCase;

public class DrivableConsoleTest extends TestCase {

    private DrivableConsole _console;
    private Tunnel _tunnel;

    protected void setUp() throws IOException {
        _tunnel = new Tunnel();
        _console = new DrivableConsole(_tunnel);
    }

    public void testDrive() throws Exception {
        // sequence matters here (have to fork off the driver thread first
        ConsoleDriver driver = new ConsoleDriver(_tunnel, getAnswers());
        driver.start();
        _console.handleConsoleIO();
    }

    private Collection getAnswers() {
        Collection answers = new ArrayList();
        answers.add("1");
        answers.add("2");
        answers.add("3");
        return answers;
    }

}
