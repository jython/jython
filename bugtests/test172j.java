
import org.python.core.*;

public class test172j {
    public static String foo(PyObject[] args) {
       return "foo called with " + args.length + " arguments";
    }

    public String bar(PyObject[] args) {
       return "bar called with " + args.length + " arguments";
    }
}
