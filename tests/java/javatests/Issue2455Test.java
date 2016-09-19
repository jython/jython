package javatests;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.python.core.PyJavaPackage;
import org.python.core.PyModule;
import org.python.util.PythonInterpreter;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import static org.junit.Assert.*;

/**
 * Test for the Jython bug 2455.
 * @author jsaiz
 */
public class Issue2455Test {

    private static final String NEW_LINE = System.getProperty("line.separator");

    private final PythonInterpreter interpreter = new PythonInterpreter();

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test(timeout = 60000)
    public void testJavaModule() throws IOException, InterruptedException {
        File example1 = temporaryFolder.newFolder("example1");
        File example2 = temporaryFolder.newFolder("example2");

        // Create Java class in example1 and __init__.py in example2
        createJavaFile(example1, "SomeClass");
        createInitFile(example2);

        // Create an interpreter and import the example packages
        interpreter.exec("import sys");
        interpreter.exec("sys.path.append('" + temporaryFolder.getRoot().toString() + "')");
        interpreter.exec("import " + example1.getName());
        interpreter.exec("import " + example2.getName());
        assertTrue(interpreter.eval(example1.getName()) instanceof PyJavaPackage);
        assertTrue(interpreter.eval(example2.getName()) instanceof PyModule);

        // Now add a Java class to example2 (after importing; otherwise example2 might be loaded as a PyJavaPackage)
        createJavaFile(example2, "OtherClass");

        // Both classes should be found
        evaluate(example1.getName() + ".SomeClass");
        evaluate(example2.getName() + ".OtherClass"); // works with 2.5.2 and the patch for 2.7.1, fails with 2.7.0
    }

    private void createJavaFile(File packageFolder, String className) throws IOException, InterruptedException {
        String javaCode = "package " + packageFolder.getName() + ";" + NEW_LINE + "public class " + className + " {}" + NEW_LINE;
        File javaFile = new File(packageFolder, className + ".java");
        createFile(javaFile, javaCode);

        compileJavaFile(javaFile);
    }

    private void compileJavaFile(File javaFile) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);

        Iterable<? extends JavaFileObject> compilationUnits =
                fileManager.getJavaFileObjectsFromFiles(new ArrayList<>(Arrays.asList(javaFile)));
        compiler.getTask(null, fileManager, null, null, null, compilationUnits).call();
    }

    private void createInitFile(File directory) throws IOException {
        File jythonFile = new File(directory, "__init__.py");
        createFile(jythonFile, "print 'within __init__.py'");
    }

    private void createFile(File file, String text) throws IOException {
        file.getParentFile().mkdirs();
        try (FileWriter writer = new FileWriter(file)) {
            writer.append(text);
            writer.flush();
        }
    }

    private void evaluate(String className) {
        assertEquals("<type '" + className + "'>", interpreter.eval(className).toString());
    }
}
