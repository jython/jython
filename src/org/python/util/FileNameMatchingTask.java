package org.python.util;

import java.io.File;
import java.util.Set;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.MatchingTask;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.util.FileNameMapper;
import org.apache.tools.ant.util.SourceFileScanner;

public abstract class FileNameMatchingTask extends MatchingTask {

    private Path src;

    protected File destDir;

    private Set<File> toProcess = Generic.set();

    /**
     * Set the source directories to find the class files to be exposed.
     */
    public void setSrcdir(Path srcDir) {
        if (src == null) {
            src = srcDir;
        } else {
            src.append(srcDir);
        }
    }

    /**
     * Gets the source dirs to find the class files to be exposed.
     */
    public Path getSrcdir() {
        return src;
    }

    /**
     * Set the destination directory into which the Java source files should be compiled.
     *
     * @param destDir
     *            the destination director
     */
    public void setDestdir(File destDir) {
        this.destDir = destDir;
    }

    /**
     * Gets the destination directory into which the java source files should be compiled.
     *
     * @return the destination directory
     */
    public File getDestdir() {
        return destDir;
    }

    @Override
    public void execute() throws BuildException {
        checkParameters();
        toProcess.clear();
        for (String srcEntry : src.list()) {
            File srcDir = getProject().resolveFile(srcEntry);
            if (!srcDir.exists()) {
                throw new BuildException("srcdir '" + srcDir.getPath() + "' does not exist!",
                                         getLocation());
            }
            String[] files = getDirectoryScanner(srcDir).getIncludedFiles();
            scanDir(srcDir, destDir != null ? destDir : srcDir, files);
        }
        process(toProcess);
    }

    protected abstract void process(Set<File> matches);

    protected abstract FileNameMapper createMapper();

    protected void scanDir(File srcDir, File destDir, String[] files) {
        SourceFileScanner sfs = new SourceFileScanner(this);
        for (File file : sfs.restrictAsFiles(files, srcDir, destDir, createMapper())) {
            toProcess.add(file);
        }
    }
    /**
     * Check that all required attributes have been set and nothing silly has been entered.
     */
    protected void checkParameters() throws BuildException {
        if (src == null || src.size() == 0) {
            throw new BuildException("srcdir attribute must be set!", getLocation());
        }
        if (destDir != null && !destDir.isDirectory()) {
            throw new BuildException("destination directory '" + destDir + "' does not exist "
                    + "or is not a directory", getLocation());
        }
    }
}