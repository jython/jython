package org.python.util;

import java.io.File;
import java.util.Properties;
import java.util.Set;

import org.apache.tools.ant.BuildException;
import org.python.core.PyException;
import org.python.core.PySystemState;
import org.python.core.imp;
import org.python.modules._py_compile;

/**
 * Compiles all python files in a directory to bytecode, and writes them to another directory,
 * possibly the same one.
 */
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
            compile(src, compiled, name);
        }
    }

    /**
     * Compiles the python file <code>src</code> to bytecode filling in <code>moduleName</code> as
     * its name, and stores it in <code>compiled</code>. This is called by process for every file
     * that's compiled, so subclasses can override this method to affect or track the compilation.
     */
    protected void compile(File src, File compiled, String moduleName) {
        byte[] bytes;
        try {
            bytes = imp.compileSource(moduleName, src);
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

    @Override
    protected String getFrom() {
        return "*.py";
    }

    @Override
    protected String getTo() {
        return "*$py.class";
    }
}
