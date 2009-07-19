package org.python.util;

import java.io.File;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.tools.ant.BuildException;
import org.python.core.Py;
import org.python.core.PySystemState;

/**
 * Compiles the Java proxies for Python classes in the Python modules in a given directory tree to
 * another directory.
 */
public class CompileProxiesTask extends JycompileAntTask {

    @Override
    public void process(Set<File> toCompile) throws BuildException {
        // Run our superclass' compile first to check that everything has valid syntax before
        // attempting to import it and to keep the imports from generating class files in the source
        // directory
        super.process(toCompile);
        Properties props = new Properties();
        props.setProperty(PySystemState.PYTHON_CACHEDIR_SKIP, "true");
        PySystemState.initialize(props, null);
        PySystemState sys = Py.getSystemState();
        // Part 2 of not spewing compilation in the source directory: import our compiled files
        sys.path.insert(0, Py.newString(destDir.getAbsolutePath()));
        sys.javaproxy_dir = destDir.getAbsolutePath();
        PythonInterpreter interp = new PythonInterpreter();
        for (String module : compiledModuleNames) {
            try {
                interp.exec("import " + module);
            } catch (RuntimeException t) {
                // We didn't get to import any of these files, so their compiled form can't hang
                // around or we won't pick them up as needing compilation next time.
                for (File f : compiledModuleFiles) {
                    f.delete();
                }
                throw t;
            }
            // This module was successfully imported, so its compiled file can hang around
            compiledModuleFiles.remove(0);
        }
    }

    @Override
    protected void compile(File src, File compiled, String moduleName) {
        try {
            super.compile(src, compiled, moduleName);
        } catch (BuildException ex) {
            // This depends on the modtime of the source being newer than that of the compiled file
            // to decide to do the import in process, so even though these files compiled properly,
            // they need to be deleted to allow them to be imported in process next time around.
            for (File f : compiledModuleFiles) {
                f.delete();
            }
            throw ex;
        }
        compiledModuleNames.add(moduleName);
        compiledModuleFiles.add(compiled);
    }

    private List<String> compiledModuleNames = Generic.list();

    private List<File> compiledModuleFiles = Generic.list();
}
