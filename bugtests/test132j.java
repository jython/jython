import org.python.core.*;
import org.python.util.PythonInterpreter;

public class test132j {
    public static void main(String[] args) {
        PythonInterpreter interp = new PythonInterpreter();
        interp.execfile("test132m.py");
        PyObject FooClass = interp.get("Foo");
        PyObject fooInstance = FooClass.__call__(new PyInteger(42));
        for (int i=0; i<4; i++) {
            fooInstance.invoke("execute");
        }
    }
}