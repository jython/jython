package org.python.expose.generate;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.MatchingTask;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.util.GlobPatternMapper;
import org.apache.tools.ant.util.SourceFileScanner;
import org.objectweb.asm.ClassWriter;

public class ExposeTask extends MatchingTask {

    /**
     * Set the source directories to find the class files to be exposed.
     */
    public void setSrcdir(Path srcDir) {
        if(src == null) {
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
     * Set the destination directory into which the Java source files should be
     * compiled.
     * 
     * @param destDir
     *            the destination director
     */
    public void setDestdir(File destDir) {
        this.destDir = destDir;
    }

    /**
     * Gets the destination directory into which the java source files should be
     * compiled.
     * 
     * @return the destination directory
     */
    public File getDestdir() {
        return destDir;
    }

    @Override
    // documentation inherited
    public void execute() throws BuildException {
        checkParameters();
        toExpose.clear();
        for(String srcEntry : src.list()) {
            File srcDir = getProject().resolveFile(srcEntry);
            if(!srcDir.exists()) {
                throw new BuildException("srcdir '" + srcDir.getPath() + "' does not exist!",
                                         getLocation());
            }
            String[] files = getDirectoryScanner(srcDir).getIncludedFiles();
            scanDir(srcDir, destDir != null ? destDir : srcDir, files);
        }
        if(toExpose.size() > 1) {
            log("Exposing " + toExpose.size() + " classes");
        } else if(toExpose.size() == 1) {
            log("Exposing 1 class");
        }
        for(File f : toExpose) {
            ExposedTypeProcessor etp;
            try {
                etp = new ExposedTypeProcessor(new FileInputStream(f));// TODO - complain about non-exposed type
            } catch(IOException e) {
                throw new BuildException("Unable to read '" + f + "' to expose it", e);
            }
            for(MethodExposer exposer : etp.getMethodExposers()) {
                generate(exposer);
            }
            for(DescriptorExposer exposer : etp.getDescriptorExposers()) {
                generate(exposer);
            }
            if(etp.getNewExposer() != null) {
                generate(etp.getNewExposer());
            }
            generate(etp.getTypeExposer());
            write(etp.getExposedClassName(), etp.getBytecode());
        }
    }

    private void generate(Exposer exposer) {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        exposer.generate(writer);
        write(exposer.getClassName(), writer.toByteArray());
    }

    private void write(String destClass, byte[] newClassfile) {
        File dest = new File(destDir, destClass.replace('.', '/') + ".class");
        dest.getParentFile().mkdirs();// TODO - check for success
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(dest);
            out.write(newClassfile);
        } catch(IOException e) {
            throw new BuildException("Unable to write to '" + dest + "'", e);
        } finally {
            if(out != null) {
                try {
                    out.close();
                } catch(IOException e) {
                    // Le sigh...
                }
            }
        }
    }

    protected void scanDir(File srcDir, File destDir, String[] files) {
        GlobPatternMapper m = new GlobPatternMapper();
        m.setFrom("*.class");
        m.setTo("*.class");
        SourceFileScanner sfs = new SourceFileScanner(this);
        for(File file : sfs.restrictAsFiles(files, srcDir, destDir, m)) {
            toExpose.add(file);
        }
    }

    /**
     * Check that all required attributes have been set and nothing silly has
     * been entered.
     */
    protected void checkParameters() throws BuildException {
        if(src == null || src.size() == 0) {
            throw new BuildException("srcdir attribute must be set!", getLocation());
        }
        if(destDir != null && !destDir.isDirectory()) {
            throw new BuildException("destination directory '" + destDir + "' does not exist "
                    + "or is not a directory", getLocation());
        }
    }

    private Path src;

    private File destDir;

    private Set<File> toExpose = new HashSet<File>();
}
