
import org.python.core.*;

public class test292j {
    public static void main(String[] args) {
        PySystemState.initialize();
        Py.getSystemState().path = new PyList();
        Py.getSystemState().path.append(new PyString("."));

        try {
            __builtin__.__import__("test292j1");
        } catch (PyException exc) {
            if (!Py.matchException(exc, Py.ImportError))
                throw exc;
        }
    }
}

