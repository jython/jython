import org.python.util.PythonInterpreter;
import org.python.core.*;

public class SimpleEmbedded {
    public static void main(String []args) throws PyException {
        PythonInterpreter interp = new PythonInterpreter();

        System.out.println("Hello, brave new world");
        interp.exec("import sys");
        interp.exec("print sys");

        interp.set("a", new PyInteger(42));
        interp.exec("print a");
        interp.exec("x = 2+2");
        PyObject x = interp.get("x");

        System.out.println("x: "+x);
        System.out.println("Goodbye, cruel world");
    }
}