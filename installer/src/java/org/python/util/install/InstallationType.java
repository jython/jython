package org.python.util.install;

public class InstallationType {

    private boolean _installLibraryModules = true;
    private boolean _installDemosAndExamples = true;
    private boolean _installDocumentation = true;
    private boolean _installSources = false;
    private boolean _ensurepip = true;
    private boolean _isStandalone = false;

    public boolean installLibraryModules() {
        return _installLibraryModules;
    }

    public boolean installDemosAndExamples() {
        return _installDemosAndExamples;
    }

    public boolean installDocumentation() {
        return _installDocumentation;
    }

    public boolean installSources() {
        return _installSources;
    }

    public boolean ensurepip() {
        return _ensurepip;
    }

    public void addLibraryModules() {
        _installLibraryModules = true;
    }

    public void removeLibraryModules() {
        _installLibraryModules = false;
    }

    public void addDemosAndExamples() {
        _installDemosAndExamples = true;
    }

    public void removeDemosAndExamples() {
        _installDemosAndExamples = false;
    }

    public void addDocumentation() {
        _installDocumentation = true;
    }

    public void removeDocumentation() {
        _installDocumentation = false;
    }

    public void addSources() {
        _installSources = true;
    }

    public void removeSources() {
        _installSources = false;
    }

    public void addEnsurepip() {
        _ensurepip = true;
    }

    public void removeEnsurepip() {
        _ensurepip = false;
    }

    public void setStandalone() {
        _isStandalone = true;
        addLibraryModules();
        removeDemosAndExamples();
        removeDocumentation();
        removeSources();
        removeEnsurepip();
    }

    public boolean isStandalone() {
        return _isStandalone;
    }

    public void setAll() {
        addLibraryModules();
        addDemosAndExamples();
        addDocumentation();
        addSources();
        addEnsurepip();
        _isStandalone = false;
    }

    public boolean isAll() {
        return installLibraryModules() && installDemosAndExamples() &&
                installDocumentation() && installSources() && ensurepip();
    }

    public void setStandard() {
        addLibraryModules();
        addDemosAndExamples();
        addDocumentation();
        addEnsurepip();
        removeSources();
        _isStandalone = false;
    }

    public boolean isStandard() {
        return installLibraryModules() && installDemosAndExamples() && ensurepip() &&
                installDocumentation() && !installSources();
    }

    public void setMinimum() {
        removeLibraryModules();
        removeDemosAndExamples();
        removeDocumentation();
        removeSources();
        removeEnsurepip();
        _isStandalone = false;
    }

    public boolean isMinimum() {
        return !installLibraryModules() && !installDemosAndExamples() &&
                !ensurepip() && !installDocumentation() && !installSources();
    }

    /**
     * @return <code>true</code> if current settings reflect one of the predefined settings
     */
    public boolean isPredefined() {
        return isAll() || isStandard() || isMinimum() || isStandalone();
    }

    public String toString() {
        StringBuffer buf = new StringBuffer(30);
        buf.append("mod: " + installDemosAndExamples() + ", demo: " + installDemosAndExamples() + ", doc: "
                + installDocumentation() + ", src: " + installSources());
        return buf.toString();
    }
}
