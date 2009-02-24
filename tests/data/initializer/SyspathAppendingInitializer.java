import java.util.Properties;
import org.python.core.JythonInitializer;
import org.python.core.Py;
import org.python.core.PySystemState;
import org.python.core.adapter.ExtensiblePyObjectAdapter;

public class SyspathAppendingInitializer implements JythonInitializer {
    public void initialize(Properties preProperties,
                    Properties postProperties,
                    String[] argv,
                    ClassLoader classLoader,
                    ExtensiblePyObjectAdapter adapter) {
        postProperties.put(PySystemState.PYTHON_CACHEDIR_SKIP, "true");
        PySystemState defaultState =
            PySystemState.doInitialize(preProperties, postProperties, argv, classLoader, adapter);
        defaultState.path.append(Py.newString("/from_SyspathAppendingInitializer_with_love"));
    }
}
