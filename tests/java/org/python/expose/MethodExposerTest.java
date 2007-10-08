package org.python.expose;

import junit.framework.TestCase;

import org.python.core.BytecodeLoader;
import org.python.core.Py;
import org.python.core.PyBuiltinFunction;
import org.python.core.PyBuiltinMethod;
import org.python.core.PySystemState;

public class MethodExposerTest extends TestCase {

    public void setUp() {
        System.setProperty(PySystemState.PYTHON_CACHEDIR_SKIP, "true");
        PySystemState.initialize();
    }
    
    public PyBuiltinFunction createBound(MethodExposer me) throws InstantiationException, IllegalAccessException {
        Class descriptor = me.load(new BytecodeLoader.Loader());
        PyBuiltinMethod instance = (PyBuiltinMethod)descriptor.newInstance();
        return instance.bind(new SimpleExposed());
    }
    
    public void testSimpleMethod() throws SecurityException, NoSuchMethodException,
            InstantiationException, IllegalAccessException {
        MethodExposer mp = new MethodExposer(SimpleExposed.class.getMethod("simple_method"));
        assertEquals("simple_method", mp.getName());
        assertEquals(SimpleExposed.class, mp.getMethodClass());
        assertEquals("org/python/expose/SimpleExposed$exposed_simple_method", mp.getInternalName());
        assertEquals("org.python.expose.SimpleExposed$exposed_simple_method", mp.getClassName());
        Class descriptor = mp.load(new BytecodeLoader.Loader());
        PyBuiltinMethod instance = (PyBuiltinMethod)descriptor.newInstance();
        assertSame("simple_method", instance.__getattr__("__name__").toString());
        SimpleExposed simpleExposed = new SimpleExposed();
        PyBuiltinFunction bound = instance.bind(simpleExposed);
        assertEquals(instance.getClass(), bound.getClass());
        assertEquals(Py.None, bound.__call__());
        assertEquals(1, simpleExposed.timesCalled);
    }

    public void testPrefixing() throws SecurityException, NoSuchMethodException {
        MethodExposer mp = new MethodExposer(SimpleExposed.class.getMethod("simpleexposed_prefixed"),
                                             "simpleexposed_");
        assertEquals("prefixed", mp.getName());
    }

    public void testStringReturn() throws SecurityException, NoSuchMethodException,
            InstantiationException, IllegalAccessException {
        MethodExposer me = new MethodExposer(SimpleExposed.class.getMethod("toString"));
        assertEquals(Py.newString(SimpleExposed.TO_STRING_RETURN), createBound(me).__call__());
    }
    
    public void testBooleanReturn() throws SecurityException, NoSuchMethodException,
            InstantiationException, IllegalAccessException {
        MethodExposer me = new MethodExposer(SimpleExposed.class.getMethod("__nonzero__"));
        assertEquals(Py.False, createBound(me).__call__());
        
    }
}
