
import org.python.util.PythonInterpreter;
import org.python.core.*;

public class test123j {
    public static void main(String args[]) {
        for(int i=0; i<10; i++) {
            PyThread p1 = new PyThread();
            Thread t1 = new Thread(p1);
            t1.start();
        }
    }

    public static class PyThread implements Runnable {
        public void run() {
            PythonInterpreter interp = new PythonInterpreter();
            interp.exec("import sys");
            interp.set("a", new PyInteger(41));
            //interp.exec("print a");
            interp.exec("x = 2+2");
            PyObject x = interp.get("x");
        }
    }
}