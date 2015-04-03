package org.python.util.install;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Locale;
import java.util.Properties;

import javax.swing.UIManager;

import org.python.util.install.Installation.JavaVersionInfo;
import org.python.util.install.driver.Autotest;

public class FrameInstaller {
    private static final String TRUE = "1";
    private static final String FALSE = "0";

    private static final String JAVA_VERSION_PROPERTY = "FrameInstaller.Version";
    private static final String JAVA_VENDOR_PROPERTY = "FrameInstaller.Vendor";
    private static final String JAVA_SPEC_VERSION_PROPERTY = "FrameInstaller.SpecVersion";

    private static final String INEX_MOD_PROPERTY = "FrameInstaller.mod";
    private static final String INEX_DEMO_PROPERTY = "FrameInstaller.demo";
    private static final String INEX_DOC_PROPERTY = "FrameInstaller.doc";
    private static final String INEX_SRC_PROPERTY = "FrameInstaller.src";
    private static final String INEX_ENSUREPIP_PROPERTY = "FrameInstaller.ensurepip";
    private static final String STANDALONE_PROPERTY = "FrameInstaller.standalone";

    private static Properties _properties = new Properties();

    private static JavaHomeHandler _javaHomeHandler;

    protected FrameInstaller(InstallerCommandLine commandLine, JarInfo jarInfo, Autotest autotest) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
        }
        // clear all properties
        _properties.clear();
        // set the default for the target directory
        if (commandLine.hasDirectoryOption()) {
            setTargetDirectory(commandLine.getTargetDirectory().getAbsolutePath());
        }
        setJavaHomeHandler(commandLine.getJavaHomeHandler());
        initDefaultJava();
        Wizard wizard = new Wizard(jarInfo, autotest);
        wizard.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent event) {
                if (!Installation.isAutotesting()) {
                    System.exit(0);
                }
            }
        });
        wizard.addWizardListener(new SimpleWizardListener());
        wizard.setVisible(true);
    }

    protected static void setProperty(String key, String value) {
        _properties.setProperty(key, value);
    }

    protected static String getProperty(String key) {
        return _properties.getProperty(key);
    }

    protected static String getProperty(String key, String defaultValue) {
        return _properties.getProperty(key, defaultValue);
    }

    protected static void setTargetDirectory(String targetDirectory) {
        setProperty(TextKeys.TARGET_DIRECTORY_PROPERTY, targetDirectory.trim());
    }

    protected static String getTargetDirectory() {
        return getProperty(TextKeys.TARGET_DIRECTORY_PROPERTY);
    }

    protected static void setJavaHomeHandler(JavaHomeHandler javaHomeHandler) {
        _javaHomeHandler = javaHomeHandler;
    }

    protected static JavaHomeHandler getJavaHomeHandler() {
        if (_javaHomeHandler == null) {
            _javaHomeHandler = new JavaHomeHandler();
        }
        return _javaHomeHandler;
    }

    protected static void setLanguage(Locale locale) {
        setProperty(TextKeys.LANGUAGE_PROPERTY, locale.toString());
        Installation.setLanguage(locale);
    }

    protected static Locale getLanguage() {
        return new Locale(getProperty(TextKeys.LANGUAGE_PROPERTY));
    }

    protected static InstallationType getInstallationType() {
        InstallationType installationType = new InstallationType();
        if (Boolean.valueOf(getProperty(STANDALONE_PROPERTY))) {
            installationType.setStandalone();
        }
        if (Boolean.valueOf(getProperty(INEX_MOD_PROPERTY))) {
            installationType.addLibraryModules();
        } else {
            installationType.removeLibraryModules();
        }
        if (Boolean.valueOf(getProperty(INEX_DEMO_PROPERTY))) {
            installationType.addDemosAndExamples();
        } else {
            installationType.removeDemosAndExamples();
        }
        if (Boolean.valueOf(getProperty(INEX_DOC_PROPERTY))) {
            installationType.addDocumentation();
        } else {
            installationType.removeDocumentation();
        }
        if (Boolean.valueOf(getProperty(INEX_SRC_PROPERTY))) {
            installationType.addSources();
        } else {
            installationType.removeSources();
        }
        if (Boolean.valueOf(getProperty(INEX_ENSUREPIP_PROPERTY))) {
            installationType.addEnsurepip();
        } else {
            installationType.removeEnsurepip();
        }
        return installationType;
    }

    protected static void setInstallationType(InstallationType installationType) {
        setProperty(STANDALONE_PROPERTY, Boolean.toString(installationType.isStandalone()));
        setProperty(INEX_MOD_PROPERTY, Boolean.toString(installationType.installLibraryModules()));
        setProperty(INEX_DEMO_PROPERTY, Boolean.toString(installationType.installDemosAndExamples()));
        setProperty(INEX_DOC_PROPERTY, Boolean.toString(installationType.installDocumentation()));
        setProperty(INEX_SRC_PROPERTY, Boolean.toString(installationType.installSources()));
        setProperty(INEX_ENSUREPIP_PROPERTY, Boolean.toString(installationType.ensurepip()));
    }

    protected static JavaVersionInfo getJavaVersionInfo() {
        JavaVersionInfo javaVersionInfo = new JavaVersionInfo();
        javaVersionInfo.setVersion(getProperty(JAVA_VERSION_PROPERTY));
        javaVersionInfo.setVendor(getProperty(JAVA_VENDOR_PROPERTY));
        javaVersionInfo.setSpecificationVersion(getProperty(JAVA_SPEC_VERSION_PROPERTY));
        return javaVersionInfo;
    }

    protected static void setJavaVersionInfo(JavaVersionInfo javaVersionInfo) {
        setProperty(JAVA_VERSION_PROPERTY, javaVersionInfo.getVersion());
        setProperty(JAVA_VENDOR_PROPERTY, javaVersionInfo.getVendor());
        setProperty(JAVA_SPEC_VERSION_PROPERTY, javaVersionInfo.getSpecificationVersion());
    }

    protected static void setAccept(boolean accept) {
        if (accept) {
            setProperty(TextKeys.ACCEPT_PROPERTY, TRUE);
        } else {
            setProperty(TextKeys.ACCEPT_PROPERTY, FALSE);
        }
    }

    protected static boolean isAccept() {
        return TRUE.equals(getProperty(TextKeys.ACCEPT_PROPERTY, FALSE));
    }

    protected static void initDefaultJava() {
        JavaVersionInfo javaVersionInfo = new JavaVersionInfo();
        Installation.fillJavaVersionInfo(javaVersionInfo, System.getProperties());
        FrameInstaller.setJavaVersionInfo(javaVersionInfo);
    }

    private class SimpleWizardListener implements WizardListener {
        public void wizardStarted(WizardEvent event) {
        }

        public void wizardFinished(WizardEvent event) {
            if (!Installation.isAutotesting()) {
                System.exit(0);
            }
        }

        public void wizardCancelled(WizardEvent event) {
            System.exit(1);
        }

        public void wizardNext(WizardEvent event) {
        }

        public void wizardPrevious(WizardEvent event) {
        }
    }
}