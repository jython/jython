import org.python.util.*;

public class test343j implements Runnable {
    public static void main(String[] args) {
        new Thread(new test343j()).start();
        new Thread(new test343j()).start();
        new Thread(new test343j()).start();
        new Thread(new test343j()).start();
        new Thread(new test343j()).start();
    }

    public void run() {
        new PythonInterpreter();
        new PythonInterpreter();
    }
}
