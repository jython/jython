import org.python.core.*;
import org.python.util.PythonInterpreter;

public class embed {
    public static void main(String[] args) {
        // Create a new Python Interpreter
        PythonInterpreter interp = new PythonInterpreter();

        // Add a variable to its namespace
        interp.set("x", new PyInteger(2));

        String expr = "x+5";

        // Evaluate some arbitrary strings
        System.out.println(expr+" = "+interp.eval(expr));     
        interp.exec("y = 'foo'");

        // Get a string back from the namespace
        Object y = Py.tojava(interp.get("y"), Object.class);

        System.out.println("y = "+y);
        
        // Create an instance of a Python defined class (FooRunner.py)
        Runnable foo = new FooRunner();
        
        // Call a method on this class as if it was a Java object
        foo.run();
    }
}