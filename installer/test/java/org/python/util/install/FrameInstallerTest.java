package org.python.util.install;

import org.python.util.install.Installation.JavaVersionInfo;

import junit.framework.TestCase;

public class FrameInstallerTest extends TestCase {

    public void testInitDefaultJava() {
        FrameInstaller.initDefaultJava();
        JavaVersionInfo vInfo = FrameInstaller.getJavaVersionInfo();
        assertNotNull(vInfo);
        String version = vInfo.getVersion();
        assertNotNull(version);
        assertTrue(version.length() > 0);
        String specificationVersion = vInfo.getSpecificationVersion();
        assertNotNull(specificationVersion);
        assertTrue(specificationVersion.length() > 0);
        String vendor = vInfo.getVendor();
        assertNotNull(vendor);
        assertTrue(vendor.length() > 0);
    }
    
    public void testJavaVersionInfo() {
        String version = "1;2;3";
        String vendor = "jython [macrosystems]";
        String specificationVersion = "@spec 1,4";

        JavaVersionInfo vInfo = new JavaVersionInfo();
        vInfo.setVersion(version);
        vInfo.setVendor(vendor);
        vInfo.setSpecificationVersion(specificationVersion);

        FrameInstaller.setJavaVersionInfo(vInfo);
        JavaVersionInfo returnedInfo = FrameInstaller.getJavaVersionInfo();

        assertNotNull(returnedInfo);
        assertEquals(version, returnedInfo.getVersion());
        assertEquals(vendor, returnedInfo.getVendor());
        assertEquals(specificationVersion, returnedInfo.getSpecificationVersion());
    }

    public void testInstallationType() {
        InstallationType installationType = new InstallationType();
        installationType.addLibraryModules();
        installationType.removeDemosAndExamples();
        installationType.removeDocumentation();
        installationType.addSources();

        FrameInstaller.setInstallationType(installationType);
        InstallationType returnedType = FrameInstaller.getInstallationType();

        assertNotNull(returnedType);
        assertTrue(returnedType.installLibraryModules());
        assertFalse(returnedType.installDemosAndExamples());
        assertFalse(returnedType.installDocumentation());
        assertTrue(returnedType.installSources());
    }

    public void testStandalone() {
        InstallationType installationType = new InstallationType();
        installationType.setStandalone();
        assertTrue(installationType.installLibraryModules());
        assertFalse(installationType.installDemosAndExamples());
        assertFalse(installationType.installDocumentation());
        assertFalse(installationType.installSources());

        FrameInstaller.setInstallationType(installationType);
        InstallationType returnedType = FrameInstaller.getInstallationType();

        assertNotNull(returnedType);
        assertTrue(returnedType.isStandalone());
        assertTrue(returnedType.installLibraryModules());
        assertFalse(returnedType.installDemosAndExamples());
        assertFalse(returnedType.installDocumentation());
        assertFalse(returnedType.installSources());
    }
    
    public void testSetGetJavaHomeHandler() {
        assertNotNull(FrameInstaller.getJavaHomeHandler());
        JavaHomeHandler handler1 = new JavaHomeHandler();
        JavaHomeHandler handler2 = new JavaHomeHandler("some/dir");
        FrameInstaller.setJavaHomeHandler(handler1);
        assertEquals(handler1, FrameInstaller.getJavaHomeHandler());
        FrameInstaller.setJavaHomeHandler(handler2);
        assertEquals(handler2, FrameInstaller.getJavaHomeHandler());
    }
}
