
import java.util.*;
import org.python.core.*;

public class test100j {
    public Vector iterate(PyObject dict) {
	Vector v = new Vector();
	PyObject keys = dict.invoke("keys");
	PyObject key;
	for (int i = 0; (key = keys.__finditem__(i)) != null; i++) {
            v.addElement(key);
            v.addElement(dict.__getitem__(key));
	}
	return v;
    }
}
