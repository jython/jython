package org.python.util;

import java.lang.reflect.Method;
import java.util.Properties;

import org.python.core.PySystemState;

import junit.framework.TestCase;

/**
 * Tests for creating the right interactive console.
 */
public class jythonTest extends TestCase {

    private static final String PYTHON_CONSOLE = "python.console";

    private Properties _originalRegistry;

    @Override
    protected void setUp() throws Exception {
        _originalRegistry = PySystemState.registry;
        Properties registry;
        if (_originalRegistry != null) {
            registry = new Properties(_originalRegistry);
        } else {
            registry = new Properties();
        }
        PySystemState.registry = registry;
    }

    @Override
    protected void tearDown() throws Exception {
        PySystemState.registry = _originalRegistry;
    }

    /**
     * test the default behavior
     * 
     * @throws Exception
     */
    public void testNewInterpreter() throws Exception {
        assertEquals(JLineConsole.class, invokeNewInterpreter(true).getClass());
    }

    /**
     * test registry override
     * 
     * @throws Exception
     */
    public void testNewInterpreter_registry() throws Exception {
        PySystemState.registry.setProperty(PYTHON_CONSOLE, "org.python.util.InteractiveConsole");
        assertEquals(InteractiveConsole.class, invokeNewInterpreter(true).getClass());
    }

    /**
     * test fallback in case of an invalid registry value
     * 
     * @throws Exception
     */
    public void testNewInterpreter_unknown() throws Exception {
        PySystemState.registry.setProperty(PYTHON_CONSOLE, "foo.bar.NoConsole");
        assertEquals(JLineConsole.class, invokeNewInterpreter(true).getClass());
    }

    /**
     * test non-interactive fallback to legacy console
     * 
     * @throws Exception
     */
    public void testNewInterpreter_NonInteractive() throws Exception {
        assertEquals(InteractiveConsole.class, invokeNewInterpreter(false).getClass());
    }

    /**
     * Invoke the private static method 'newInterpreter(boolean)' on jython.class
     * 
     * @throws Exception
     */
    private InteractiveConsole invokeNewInterpreter(boolean interactiveStdin) throws Exception {
        Method method = jython.class.getDeclaredMethod("newInterpreter", Boolean.TYPE);
        assertNotNull(method);
        method.setAccessible(true);
        Object result = method.invoke(null, interactiveStdin);
        assertNotNull(result);
        assertTrue(result instanceof InteractiveConsole);
        return (InteractiveConsole)result;
    }
}
