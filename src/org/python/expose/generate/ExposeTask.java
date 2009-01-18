package org.python.expose.generate;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Set;

import org.apache.tools.ant.BuildException;
import org.objectweb.asm.ClassWriter;
import org.python.util.GlobMatchingTask;

public class ExposeTask extends GlobMatchingTask {

    @Override
    protected String getFrom() {
        return "*.class";
    }

    @Override
    protected String getTo() {
        return "*.class";
    }

    @Override
    public void process(Set<File> toExpose) throws BuildException {
        if (toExpose.size() > 1) {
            log("Exposing " + toExpose.size() + " classes");
        } else if (toExpose.size() == 1) {
            log("Exposing 1 class");
        }
        for (File f : toExpose) {
            ExposedTypeProcessor etp;
            try {
                etp = new ExposedTypeProcessor(new FileInputStream(f));
            } catch (IOException e) {
                throw new BuildException("Unable to read '" + f + "' to expose it", e);
            } catch (InvalidExposingException iee) {
                throw new BuildException(iee.getMessage());
            }
            for (MethodExposer exposer : etp.getMethodExposers()) {
                generate(exposer);
            }
            for (DescriptorExposer exposer : etp.getDescriptorExposers()) {
                generate(exposer);
            }
            if (etp.getNewExposer() != null) {
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
        } catch (IOException e) {
            throw new BuildException("Unable to write to '" + dest + "'", e);
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    // Le sigh...
                }
            }
        }
    }
}
