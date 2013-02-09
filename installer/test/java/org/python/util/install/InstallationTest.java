package org.python.util.install;

import java.io.File;

import org.python.util.install.Installation.JavaVersionInfo;

import junit.framework.TestCase;

public class InstallationTest extends TestCase {

    public void testGetExternalJavaVersion() {
        JavaHomeHandler javaHomeHandler = new JavaHomeHandler();
        JavaVersionInfo versionInfo = Installation.getExternalJavaVersion(javaHomeHandler);
        assertEquals(Installation.NORMAL_RETURN, versionInfo.getErrorCode());
        assertEquals("", versionInfo.getReason());
        assertTrue(versionInfo.getVersion().length() > 0);
        assertTrue(versionInfo.getSpecificationVersion().length() > 0);
        assertTrue(versionInfo.getVersion().startsWith(versionInfo.getSpecificationVersion()));
        assertNotNull(versionInfo.getVendor());
        assertNotSame("", versionInfo.getVendor());
    }

    public void testGetExternalJavaVersionWithError() {
        JavaHomeHandler javaHomeHandler = new JavaHomeHandler("non_existing/home");
        JavaVersionInfo versionInfo = Installation.getExternalJavaVersion(javaHomeHandler);
        assertEquals(Installation.ERROR_RETURN, versionInfo.getErrorCode());
        String reason = versionInfo.getReason();
        assertTrue(reason.indexOf("invalid") >= 0);
    }

    public void testGetExternalJavaVersionNoBinDirectory() {
        File wrongHome = new File(System.getProperty("user.home"));
        JavaHomeHandler javaHomeHandler = new JavaHomeHandler(wrongHome.getAbsolutePath());
        JavaVersionInfo versionInfo = Installation.getExternalJavaVersion(javaHomeHandler);
        assertEquals(Installation.ERROR_RETURN, versionInfo.getErrorCode());
        String reason = versionInfo.getReason();
        assertTrue(reason.indexOf("invalid") >= 0);
    }

    public void testGetExternalJavaVersionNoJavaInBinDirectory() {
        File wrongHome = new File(System.getProperty("user.home"));
        File binDir = new File(wrongHome, "bin");
        assertFalse(binDir.exists());
        try {
            assertTrue(binDir.mkdirs());
            JavaHomeHandler javaHomeHandler = new JavaHomeHandler(wrongHome.getAbsolutePath());
            JavaVersionInfo versionInfo = Installation.getExternalJavaVersion(javaHomeHandler);
            assertEquals(Installation.ERROR_RETURN, versionInfo.getErrorCode());
            assertTrue(versionInfo.getReason().indexOf("invalid") >= 0);
        } finally {
            if (binDir.exists()) {
                binDir.delete();
            }
        }
    }

    public void testIsValidJavaVersion() {
        JavaVersionInfo javaVersionInfo = new JavaVersionInfo();

        javaVersionInfo.setSpecificationVersion("1.1.9");
        assertFalse(Installation.isValidJava(javaVersionInfo));
        javaVersionInfo.setSpecificationVersion("1.2");
        assertFalse(Installation.isValidJava(javaVersionInfo));
        javaVersionInfo.setSpecificationVersion("1.3");
        assertFalse(Installation.isValidJava(javaVersionInfo));
        javaVersionInfo.setSpecificationVersion("1.4");
        assertFalse(Installation.isValidJava(javaVersionInfo));
        javaVersionInfo.setSpecificationVersion("1.5");
        assertTrue(Installation.isValidJava(javaVersionInfo));
        javaVersionInfo.setSpecificationVersion("1.6");
        assertTrue(Installation.isValidJava(javaVersionInfo));
        javaVersionInfo.setSpecificationVersion("1.7");
        assertTrue(Installation.isValidJava(javaVersionInfo));
    }

    public void testGetJavaSpecificationVersion() {
        String specificationVersion = "1.4.2";
        assertEquals(14, Installation.getJavaSpecificationVersion(specificationVersion));
        specificationVersion = "1.5.0";
        assertEquals(15, Installation.getJavaSpecificationVersion(specificationVersion));
        specificationVersion = "1.6.0";
        assertEquals(16, Installation.getJavaSpecificationVersion(specificationVersion));
    }

    public void testIsGNUJava() {
        assertFalse(Installation.isGNUJava());
        String originalVmName = System.getProperty(Installation.JAVA_VM_NAME);
        try {
            // fake GNU java
            System.setProperty(Installation.JAVA_VM_NAME, "GNU libgcj");
            assertTrue(Installation.isGNUJava());
        } finally {
            System.setProperty(Installation.JAVA_VM_NAME, originalVmName);
            assertFalse(Installation.isGNUJava());
        }
    }
    
    public void testGetDefaultJavaVersion() {
        JavaVersionInfo info = Installation.getDefaultJavaVersion();
        assertNotNull(info);
        assertEquals(Installation.NORMAL_RETURN, info.getErrorCode());
        String specVersion = info.getSpecificationVersion();
        assertNotNull(specVersion);
        assertTrue(specVersion.length() >= 3);
    }

}
