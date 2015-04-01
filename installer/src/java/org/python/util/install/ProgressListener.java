package org.python.util.install;

public interface ProgressListener extends InstallationListener {

    public int getInterval();

    public void progressChanged(int newPercentage);

    public void progressEntry(String entry);

    public void progressStartScripts();

    public void progressStandalone();

    public void progressEnsurepip();

}