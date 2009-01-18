package org.python.util;

import java.io.File;
import java.util.Properties;
import java.util.Set;

import org.apache.tools.ant.BuildException;
import org.python.core.PyException;
import org.python.core.PySystemState;
import org.python.core.imp;
import org.python.modules._py_compile;

public class JycompileAntTask extends GlobMatchingTask {

    @Override
    public void process(Set<File> toCompile) throws BuildException {
        if (toCompile.size() == 0) {
            return;
        } else if (toCompile.size() > 1) {
            log("Compiling " + toCompile.size() + " files");
        } else if (toCompile.size() == 1) {
            log("Compiling 1 file");
        }
        Properties props = new Properties();
        props.setProperty(PySystemState.PYTHON_CACHEDIR_SKIP, "true");
        PySystemState.initialize(System.getProperties(), props);
        for (File src : toCompile) {
            String name = _py_compile.getModuleName(src);
            String compiledFilePath = name.replace('.', '/');
            if (src.getName().endsWith("__init__.py")) {
                compiledFilePath += "/__init__";
            }
            File compiled = new File(destDir, compiledFilePath + "$py.class");
            byte[] bytes;
            try {
                bytes = imp.compileSource(name, src);
            } catch (PyException pye) {
                pye.printStackTrace();
                throw new BuildException("Compile failed; see the compiler error output for details.");
            }
            File dir = compiled.getParentFile();
            if (!dir.exists() && !compiled.getParentFile().mkdirs()) {
                throw new BuildException("Unable to make directory for compiled file: " + compiled);
            }
            imp.cacheCompiledSource(src.getAbsolutePath(), compiled.getAbsolutePath(), bytes);
        }
    }

    protected String getFrom() {
        return "*.py";
    }

    protected String getTo() {
        return "*$py.class";
    }
}
