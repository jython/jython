package javatests;
import org.python.core.PyObject;

public class Issue1833 {
    public PyObject target;

    public void setValue(PyObject value) {
        target.__setattr__("attribute", value);
    }
}
