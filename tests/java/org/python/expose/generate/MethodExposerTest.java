package org.python.expose.generate;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.python.core.BytecodeLoader;
import org.python.core.Py;
import org.python.core.PyBuiltinFunction;
import org.python.core.PyException;
import org.python.core.PyObject;
import org.python.expose.MethodType;

public class MethodExposerTest extends InterpTestCase implements Opcodes, PyTypes {

    public static MethodExposer createExposer(String methodName, Type returnType, Type... args) {
        return new MethodExposer(Type.getType(SimpleExposed.class),
                                 Opcodes.ACC_PUBLIC,
                                 methodName,
                                 Type.getMethodDescriptor(returnType, args),
                                 "simpleexposed");
    }

    public static PyBuiltinFunction createBound(String methodName, Type returnType, Type... args)
            throws Exception {
        return createBound(createExposer(methodName, returnType, args));
    }

    public static PyBuiltinFunction createBound(MethodExposer me) throws Exception {
        Class descriptor = me.load(new BytecodeLoader.Loader());
        return instantiate(descriptor, me.getNames()[0]).bind(new SimpleExposed());
    }

    public static PyBuiltinFunction instantiate(Class descriptor, String name) throws Exception {
        return (PyBuiltinFunction)descriptor.getConstructor(String.class).newInstance(name);
    }

    public void testSimpleMethod() throws Exception {
        MethodExposer mp = createExposer("simple_method", VOID);
        assertEquals("simple_method", mp.getNames()[0]);
        assertEquals("org/python/expose/generate/SimpleExposed$simple_method_exposer",
                     mp.getInternalName());
        assertEquals("org.python.expose.generate.SimpleExposed$simple_method_exposer",
                     mp.getClassName());
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
        assertEquals("prefixed", createExposer("simpleexposed_prefixed", VOID).getNames()[0]);
    }

    public void testStringReturn() throws Exception {
        assertEquals(Py.newString(SimpleExposed.TO_STRING_RETURN),
                     createBound("toString", STRING).__call__());
    }

    public void testBooleanReturn() throws Exception {
        assertEquals(Py.False, createBound("__nonzero__", BOOLEAN).__call__());
    }

    public void testArgumentPassing() throws Exception {
        PyBuiltinFunction bound = createBound("takesArgument", VOID, PYOBJ);
        bound.__call__(Py.None);
        try {
            bound.__call__();
            fail("Need to pass an argument to takesArgument");
        } catch(Exception e) {}
    }

    public void testBinary() throws Exception {
        MethodExposer exposer = createExposer("__add__", PYOBJ, PYOBJ);
        exposer.type = MethodType.BINARY;
        PyBuiltinFunction bound = createBound(exposer);
        assertEquals(Py.NotImplemented, bound.__call__(Py.None));
        assertEquals(Py.One, bound.__call__(Py.False));
    }

    public void testCmp() throws Exception {
        MethodExposer exp = createExposer("__cmp__", INT, PYOBJ);
        exp.type = MethodType.CMP;
        PyBuiltinFunction bound = createBound(exp);
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
        MethodExposer exp = createExposer("defaultToNone", PYOBJ, PYOBJ);
        exp.defaults = new String[] {"Py.None"};
        PyBuiltinFunction bound = createBound(exp);
        assertEquals(Py.One, bound.__call__(Py.One));
        assertEquals(Py.None, bound.__call__());
    }

    public void testNullDefault() throws Exception {
        MethodExposer exp = createExposer("defaultToNull", PYOBJ, PYOBJ);
        exp.defaults = new String[] {"null"};
        PyBuiltinFunction bound = createBound(exp);
        assertEquals(Py.One, bound.__call__(Py.One));
        assertEquals(null, bound.__call__());
    }

    public void testIntDefault() throws Exception {
        MethodExposer exp = createExposer("defaultToOne", PYOBJ, INT);
        exp.defaults = new String[] {"1"};
        PyBuiltinFunction bound = createBound(exp);
        assertEquals(Py.Zero, bound.__call__(Py.Zero));
        assertEquals(Py.One, bound.__call__());
        exp.defaults = new String[] {"X"};
        try {
            createBound(exp);
            fail("Shouldn't be able to create the exposer with a non-int default value");
        } catch(NumberFormatException nfe) {}
    }

    public void testFullArguments() throws Exception {
        MethodExposer exp = new MethodExposer(Type.getType(SimpleExposed.class),
                                              Opcodes.ACC_PUBLIC,
                                              "fullArgs",
                                              Type.getMethodDescriptor(PYOBJ, new Type[] {APYOBJ,
                                                                                          ASTRING}),
                                              "simpleexposed");
        PyBuiltinFunction bound = createBound(exp);
        assertEquals(Py.Zero, bound.__call__());
        assertEquals(Py.One, bound.__call__(Py.One));
        exp.defaults = new String[] {"X"};
        try {
            createBound(exp);
            fail("Shouldn't be able to create the exposer with a default value");
        } catch(IllegalStateException ise) {}
    }
}
