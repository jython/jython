package org.python.util.install;

import junit.framework.TestCase;

// test checkin
public class InstallationTypeTest extends TestCase {

    private InstallationType _type;

    protected void setUp() {
        _type = new InstallationType();
    }

    public void testConstruction() {
        assertTrue(_type.isStandard());
        assertFalse(_type.isMinimum());
        assertFalse(_type.isAll());
        assertFalse(_type.isStandalone());
        assertTrue(_type.isPredefined());
    }

    public void testStandard() {
        _type.setStandard();
        assertTrue(_type.isStandard());
        assertTrue(_type.installLibraryModules());
        assertTrue(_type.installDemosAndExamples());
        assertTrue(_type.installDocumentation());
        assertFalse(_type.installSources());
        assertFalse(_type.isStandalone());
        assertTrue(_type.isPredefined());
    }

    public void testMinimum() {
        assertFalse(_type.isMinimum());
        _type.setMinimum();
        assertTrue(_type.isMinimum());
        assertFalse(_type.installLibraryModules());
        assertFalse(_type.installDemosAndExamples());
        assertFalse(_type.installDocumentation());
        assertFalse(_type.installSources());
        assertFalse(_type.isStandalone());
        assertTrue(_type.isPredefined());
    }

    public void testAll() {
        assertFalse(_type.isAll());
        _type.setAll();
        assertTrue(_type.isAll());
        assertTrue(_type.installLibraryModules());
        assertTrue(_type.installDemosAndExamples());
        assertTrue(_type.installDocumentation());
        assertTrue(_type.installSources());
        assertFalse(_type.isStandalone());
        assertTrue(_type.isPredefined());
    }

    public void testStandalone() {
        assertFalse(_type.isStandalone());
        _type.setStandalone();
        assertTrue(_type.isStandalone());
        assertFalse(_type.isMinimum());
        assertFalse(_type.isStandard());
        assertFalse(_type.isAll());
        assertTrue(_type.isPredefined());

        // sure to handle this as follows?
        assertTrue(_type.installLibraryModules());
        assertFalse(_type.installDemosAndExamples());
        assertFalse(_type.installDocumentation());
        assertFalse(_type.installSources());
    }

    public void testAddRemove() {
        _type.removeDocumentation();
        assertTrue(_type.installLibraryModules());
        assertTrue(_type.installDemosAndExamples());
        assertFalse(_type.installDocumentation());
        assertFalse(_type.installSources());
        assertFalse(_type.isMinimum());
        assertFalse(_type.isStandard());
        assertFalse(_type.isAll());
        assertFalse(_type.isStandalone());
        assertFalse(_type.isPredefined());

        _type.removeDemosAndExamples();
        assertTrue(_type.installLibraryModules());
        assertFalse(_type.installDemosAndExamples());
        assertFalse(_type.installDocumentation());
        assertFalse(_type.installSources());
        assertFalse(_type.isMinimum());
        assertFalse(_type.isStandard());
        assertFalse(_type.isAll());
        assertFalse(_type.isStandalone());
        assertFalse(_type.isPredefined());

        _type.addSources();
        assertTrue(_type.installLibraryModules());
        assertFalse(_type.installDemosAndExamples());
        assertFalse(_type.installDocumentation());
        assertTrue(_type.installSources());
        assertFalse(_type.isMinimum());
        assertFalse(_type.isStandard());
        assertFalse(_type.isAll());
        assertFalse(_type.isStandalone());
        assertFalse(_type.isPredefined());

        _type.addDocumentation();
        assertTrue(_type.installLibraryModules());
        assertFalse(_type.installDemosAndExamples());
        assertTrue(_type.installDocumentation());
        assertTrue(_type.installSources());
        assertFalse(_type.isMinimum());
        assertFalse(_type.isStandard());
        assertFalse(_type.isAll());
        assertFalse(_type.isStandalone());
        assertFalse(_type.isPredefined());
    }

}
