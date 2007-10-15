package org.python.expose;

import java.lang.reflect.Method;

import org.python.core.BytecodeLoader;
import org.python.core.Py;
import org.python.core.PyBuiltinFunction;
import org.python.core.PyException;
import org.python.core.PyObject;

public class MethodExposerTest extends InterpTestCase {

    public PyBuiltinFunction createBound(String methodName, Class... args) throws Exception {
        return createBound(new MethodExposer(SimpleExposed.class.getDeclaredMethod(methodName, args),
                                             "simpleexpose"));
    }

    public PyBuiltinFunction createBound(MethodExposer me) throws Exception {
        Class descriptor = me.load(new BytecodeLoader.Loader());
        return instantiate(descriptor, me.getNames()[0]).bind(new SimpleExposed());
    }

    public PyBuiltinFunction instantiate(Class descriptor, String name) throws Exception {
        return (PyBuiltinFunction)descriptor.getConstructor(String.class).newInstance(name);
    }

    public void testSimpleMethod() throws Exception {
        MethodExposer mp = new MethodExposer(SimpleExposed.class.getMethod("simple_method"));
        assertEquals("simple_method", mp.getNames()[0]);
        assertEquals(SimpleExposed.class, mp.getMethodClass());
        assertEquals("org/python/expose/SimpleExposed$exposed_simple_method", mp.getInternalName());
        assertEquals("org.python.expose.SimpleExposed$exposed_simple_method", mp.getClassName());
        Class descriptor = mp.load(new BytecodeLoader.Loader());
        PyBuiltinFunction instance = instantiate(descriptor, "simple_method");
        assertSame("simple_method", instance.__getattr__("__name__").toString());
        SimpleExposed simpleExposed = new SimpleExposed();
        PyBuiltinFunction bound = instance.bind(simpleExposed);
        assertEquals(instance.getClass(), bound.getClass());
        assertEquals(Py.None, bound.__call__());
        assertEquals(1, simpleExposed.timesCalled);
    }

    public void testPrefixing() throws Exception {
        MethodExposer mp = new MethodExposer(SimpleExposed.class.getMethod("simpleexposed_prefixed"),
                                             "simpleexposed");
        assertEquals("prefixed", mp.getNames()[0]);
    }

    public void testStringReturn() throws Exception {
        assertEquals(Py.newString(SimpleExposed.TO_STRING_RETURN),
                     createBound("toString").__call__());
    }

    public void testBooleanReturn() throws Exception {
        assertEquals(Py.False, createBound("__nonzero__").__call__());
    }

    public void testArgumentPassing() throws Exception {
        PyBuiltinFunction bound = createBound("takesArgument", PyObject.class);
        bound.__call__(Py.None);
        try {
            bound.__call__();
            fail("Need to pass an argument to takesArgument");
        } catch(Exception e) {}
    }

    public void testBinary() throws Exception {
        PyBuiltinFunction bound = createBound("__add__", PyObject.class);
        assertEquals(Py.NotImplemented, bound.__call__(Py.None));
        assertEquals(Py.One, bound.__call__(Py.False));
    }

    public void testCmp() throws Exception {
        PyBuiltinFunction bound = createBound("__cmp__", PyObject.class);
        try {
            bound.__call__(Py.None);
            fail("Returning -2 from __cmp__ should yield a type error");
        } catch(PyException e) {
            if(!Py.matchException(e, Py.TypeError)) {
                fail("Returning -2 from __cmp__ should yield a type error");
            }
        }
        assertEquals(Py.One, bound.__call__(Py.False));
    }

    public void testNoneDefault() throws Exception {
        PyBuiltinFunction bound = createBound("defaultToNone", PyObject.class);
        assertEquals(Py.One, bound.__call__(Py.One));
        assertEquals(Py.None, bound.__call__());
    }

    public void testNullDefault() throws Exception {
        PyBuiltinFunction bound = createBound("defaultToNull", PyObject.class);
        assertEquals(Py.One, bound.__call__(Py.One));
        assertEquals(null, bound.__call__());
    }
}
