package org.python.util.install;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Working horse extracting the contents of the installation .jar to the file system. <br>
 * The directory stucture is preserved, but there is the possibility to exclude some entries
 * (directories at the moment).
 */
public class JarInstaller {

    public static final String JYTHON_JAR = "jython.jar";

    private static final String PATH_SEPARATOR = "/";

    private static final String LIB_NAME_SEP = "Lib" + PATH_SEPARATOR;

    private static final String LIB_PAWT_SEP = LIB_NAME_SEP + "pawt" + PATH_SEPARATOR;

    private static final int BUFFER_SIZE = 1024;

    private ProgressListener _progressListener;

    private JarInfo _jarInfo;

    private List<InstallationListener> _installationListeners;

    public JarInstaller(ProgressListener progressListener, JarInfo jarInfo) {
        _progressListener = progressListener;
        _jarInfo = jarInfo;
        _installationListeners = new ArrayList<InstallationListener>();
    }

    /**
     * Do the physical installation:
     * <ul>
     * <li>unzip the files
     * <li>generate the start scripts
     * <li>run ensurepip if selected
     * </ul>
     *
     * @param targetDirectory
     * @param installationType
     */
    public void inflate(final File targetDirectory, InstallationType installationType, JavaHomeHandler javaHomeHandler) {
        try {
            // has to correspond with build.xml
            // has to correspond with build.Lib.include.properties
            List<String> excludeDirs = _jarInfo.getExcludeDirs();
            List<String> coreLibFiles = new ArrayList<String>();
            if (!installationType.installSources()) {
                excludeDirs.add("src");
                excludeDirs.add("grammar");
                excludeDirs.add("extlibs");
            }
            if (!installationType.installDocumentation()) {
                excludeDirs.add("Doc");
            }
            if (!installationType.installDemosAndExamples()) {
                excludeDirs.add("Demo");
            }
            if (!installationType.installLibraryModules()) {
                excludeDirs.add(LIB_NAME_SEP + "email");
                excludeDirs.add(LIB_NAME_SEP + "encodings");
                excludeDirs.add(LIB_NAME_SEP + "test");
                excludeDirs.add(LIB_NAME_SEP + "jxxload_help");
                coreLibFiles = getCoreLibFiles();
            }
            if (installationType.isStandalone()) {
                excludeDirs.add("Tools");
            }
            int count = 0;
            int percent = 0;
            int numberOfIntervals = 100 / _progressListener.getInterval();
            int numberOfEntries = approximateNumberOfEntries(installationType);
            int threshold = numberOfEntries / numberOfIntervals + 1; // +1 = pessimistic
            boolean coreExclusionReported = false;
            // unzip
            ZipInputStream zipInput = new ZipInputStream(new BufferedInputStream(new FileInputStream(_jarInfo.getJarFile()),
                                                                                 BUFFER_SIZE));
            ZipEntry zipEntry = zipInput.getNextEntry();
            while (zipEntry != null) {
                String zipEntryName = zipEntry.getName();
                boolean exclude = false;
                // handle exclusion of directories
                Iterator<String> excludeDirsAsIterator = excludeDirs.iterator();
                while (excludeDirsAsIterator.hasNext()) {
                    if (zipEntryName.startsWith(excludeDirsAsIterator.next()
                            + PATH_SEPARATOR)) {
                        exclude = true;
                    }
                }
                // exclude build.xml when not installing source
                if (!installationType.installSources() && zipEntryName.equals("build.xml")) {
                    exclude = true;
                }
                // handle exclusion of core Lib files
                if (!exclude) {
                    exclude = shouldExcludeFile(installationType,
                                                coreLibFiles,
                                                zipEntry,
                                                zipEntryName);
                    if (Installation.isVerbose() && !coreExclusionReported && exclude) {
                        ConsoleInstaller.message("excluding some .py files, like " + zipEntryName);
                        coreExclusionReported = true;
                    }
                }
                if (exclude) {
                    if (Installation.isVerbose() && zipEntry.isDirectory()) {
                        ConsoleInstaller.message("excluding directory " + zipEntryName);
                    }
                } else {
                    count++;
                    if (count % threshold == 0) {
                        percent = percent + _progressListener.getInterval();
                        _progressListener.progressChanged(percent);
                    }
                    createDirectories(targetDirectory, zipEntryName);
                    if (!zipEntry.isDirectory()) {
                        File file = createFile(targetDirectory, zipEntryName);
                        _progressListener.progressEntry(file.getAbsolutePath());
                        FileOutputStream output = new FileOutputStream(file);
                        byte[] buffer = new byte[BUFFER_SIZE];
                        int len;
                        while ((len = zipInput.read(buffer)) > 0) {
                            output.write(buffer, 0, len);
                        }
                        output.close();
                        file.setLastModified(zipEntry.getTime());
                    }
                }
                zipInput.closeEntry();
                zipEntry = zipInput.getNextEntry();
            }
            if (!installationType.isStandalone()) {
                // generate start scripts
                _progressListener.progressStartScripts();
                StartScriptGenerator generator = new StartScriptGenerator(targetDirectory, javaHomeHandler);
                generator.generateStartScripts();
                if (installationType.ensurepip()) {
                    _progressListener.progressEnsurepip();
                    _progressListener.progressChanged(90); // approx
                    ensurepip(targetDirectory.toPath().resolve("bin"));
                }
            } else {
                _progressListener.progressStandalone();
                File jythonJar = new File(targetDirectory, JYTHON_JAR);
                File jythonPlainJar = new File(targetDirectory, "plain_" + JYTHON_JAR);
                jythonJar.renameTo(jythonPlainJar);
                File libDir = new File(targetDirectory, "Lib");
                StandalonePackager packager = new StandalonePackager(jythonJar);
                packager.addJarFile(jythonPlainJar);
                _progressListener.progressChanged(90); // approx
                packager.addFullDirectory(libDir);
                packager.close();
                // TODO:Oti move to FileHelper
                StandalonePackager.emptyDirectory(targetDirectory, jythonJar);
            }
            // finish: inform listeners
            _progressListener.progressFinished();
            Iterator<InstallationListener> installationListenersIterator = _installationListeners.iterator();
            while (installationListenersIterator.hasNext()) {
                installationListenersIterator.next().progressFinished();
            }
        } catch (IOException ioe) {
            throw new InstallerException(Installation.getText(TextKeys.ERROR_ACCESS_JARFILE), ioe);
        }
    }

    private int ensurepip(Path bindir) {
        int errorCode = 0;
        try {
            String command[];
            if (Installation.isWindows()) {
                command = new String[] {bindir.resolve("jython.exe").toString(), "-m", "ensurepip"};
            } else {
                command = new String[] {Paths.get(".", "jython").toString(), "-m", "ensurepip"};
            }
            ChildProcess childProcess = new ChildProcess(command);
            childProcess.setCWD(bindir);
            // JYTHON_HOME will be wrong if set: see https://bugs.jython.org/issue2345
            childProcess.putEnvironment("JYTHON_HOME", null);
            errorCode = childProcess.run();
        } catch (Throwable t) {
            errorCode = 1;
        }
        return errorCode;
    }

    public void addInstallationListener(InstallationListener installationListener) {
        if (installationListener != null) {
            _installationListeners.add(installationListener);
        }
    }

    private int approximateNumberOfEntries(InstallationType installationType) {
        int numberOfEntries = 200; // core (minimum)
        if (installationType.installLibraryModules()) {
            if (installationType.isStandalone()) {
                numberOfEntries += 450;
            } else {
                numberOfEntries += 1300;
            }
        }
        if (installationType.installDemosAndExamples()) {
            numberOfEntries += 70;
        }
        if (installationType.installDocumentation()) {
            numberOfEntries += 500;
        }
        if (installationType.installSources()) {
            numberOfEntries += 1000;
        }
        if (installationType.ensurepip()) {
            numberOfEntries += 2000;
        }
        return numberOfEntries;
    }

    private void createDirectories(final File targetDirectory, final String zipEntryName) {
        int lastSepIndex = zipEntryName.lastIndexOf(PATH_SEPARATOR);
        if (lastSepIndex > 0) {
            File directory = new File(targetDirectory, zipEntryName.substring(0, lastSepIndex));
            if (directory.exists() && directory.isDirectory()) {} else {
                if (!directory.mkdirs()) {
                    throw new InstallerException(Installation.getText(TextKeys.UNABLE_CREATE_DIRECTORY,
                                                                      directory.getAbsolutePath()));
                }
            }
        }
    }

    private File createFile(final File targetDirectory, final String zipEntryName)
            throws IOException {
        File file = new File(targetDirectory, zipEntryName);
        if (file.exists() && file.isFile()) {} else {
            if (!file.createNewFile()) {
                throw new InstallerException(Installation.getText(TextKeys.UNABLE_CREATE_FILE,
                                                                  file.getCanonicalPath()));
            }
        }
        return file;
    }

    private List<String> getCoreLibFiles() {
        List<String> coreLibFiles = new ArrayList<String>();
        coreLibFiles.add("__future__.py");
        coreLibFiles.add("copy.py");
        coreLibFiles.add("copy_reg.py");
        coreLibFiles.add("dbexts.py");
        coreLibFiles.add("imaplib.py");
        coreLibFiles.add("isql.py");
        coreLibFiles.add("javaos.py");
        coreLibFiles.add("javapath.py");
        coreLibFiles.add("jreload.py");
        coreLibFiles.add("linecache.py");
        coreLibFiles.add("marshal.py");
        coreLibFiles.add("ntpath.py");
        coreLibFiles.add("os.py");
        coreLibFiles.add("popen2.py");
        coreLibFiles.add("posixpath.py");
        coreLibFiles.add("random.py");
        coreLibFiles.add("re.py");
        coreLibFiles.add("site.py");
        coreLibFiles.add("socket.py");
        coreLibFiles.add("sre.py");
        coreLibFiles.add("sre_compile.py");
        coreLibFiles.add("sre_constants.py");
        coreLibFiles.add("sre_parse.py");
        coreLibFiles.add("stat.py");
        coreLibFiles.add("string.py");
        coreLibFiles.add("sysconfig.py");
        coreLibFiles.add("threading.py");
        coreLibFiles.add("traceback.py");
        coreLibFiles.add("types.py");
        coreLibFiles.add("UserDict.py");
        coreLibFiles.add("zipfile.py");
        coreLibFiles.add("zlib.py");
        return coreLibFiles;
    }

    private boolean shouldExcludeFile(InstallationType installationType,
                                      List<String> coreLibFiles,
                                      ZipEntry zipEntry,
                                      String zipEntryName) {
        boolean exclude = false;
        if (!installationType.installLibraryModules()) {
            // handle files in Lib
            if (!zipEntry.isDirectory() && zipEntryName.startsWith(LIB_NAME_SEP)) {
                // include all files in /pawt subdirectory
                if (!zipEntryName.startsWith(LIB_PAWT_SEP)) {
                    if (zipEntryName.endsWith(".py")) { // only compare *.py files
                        exclude = true;
                        Iterator<String> coreLibFilesAsIterator = coreLibFiles.iterator();
                        while (coreLibFilesAsIterator.hasNext()) {
                            String coreFileName = coreLibFilesAsIterator.next();
                            if (zipEntryName.endsWith(PATH_SEPARATOR + coreFileName)) {
                                exclude = false;
                            }
                        }
                    }
                }
            }
        }
        return exclude;
    }
}
