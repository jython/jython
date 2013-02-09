package org.python.util.install.driver;

import java.io.IOException;

import org.python.util.install.InstallerCommandLine;

public class SilentAutotest extends Autotest {

    protected SilentAutotest(InstallerCommandLine commandLine) throws IOException, DriverException {
        super(commandLine);
    }

    protected String getNameSuffix() {
        return "silentTest";
    }

    //
    // interface InstallationListener
    //

    public void progressFinished() {
        // ignored
    }

}
