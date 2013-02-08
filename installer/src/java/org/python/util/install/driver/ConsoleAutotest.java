package org.python.util.install.driver;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.python.util.install.InstallerCommandLine;

public class ConsoleAutotest extends SilentAutotest {

    private Collection _answers;

    protected ConsoleAutotest(InstallerCommandLine commandLine) throws IOException, DriverException {
        super(commandLine);
        _answers = new ArrayList(50);
    }

    protected void addAnswer(String answer) {
        _answers.add(answer);
    }

    protected Collection getAnswers() {
        return _answers;
    }

    protected String getNameSuffix() {
        return "consoleTest";
    }

}
