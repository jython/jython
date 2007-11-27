package org.python.expose.generate;

import org.objectweb.asm.Type;
import org.python.core.BytecodeLoader;
import org.python.core.Py;
import org.python.core.PyDataDescr;
import org.python.core.PyObject;
import org.python.core.PyType;

public class DescriptorExposerTest extends InterpTestCase implements PyTypes {

    public void testNoDescriptors() throws Exception {
        DescriptorExposer de = new DescriptorExposer(Type.getType(SimpleExposed.class), "desc");
        try {
            de.load(new BytecodeLoader.Loader());
            fail("Should not be able to generate a descriptor with no getter");
        } catch(IllegalArgumentException ise) {
            // Should be thrown when a getter isn't added.
        }
    }

    public void testMethodGetter() throws Exception {
        DescriptorExposer de = new DescriptorExposer(Type.getType(SimpleExposed.class), "desc");
        de.addMethodGetter("toString", RETURN_STRING_DESCRIPTOR);
        Class descriptor = de.load(new BytecodeLoader.Loader());
        PyDataDescr instance = (PyDataDescr)descriptor.newInstance();
        assertEquals(SimpleExposed.TO_STRING_RETURN,
                     instance.__get__(new SimpleExposed(), PyType.fromClass(SimpleExposed.class))
                             .toString());
        assertFalse(instance.implementsDescrSet());
        assertFalse(instance.implementsDescrDelete());
    }

    public void testFieldGetter() throws Exception {
        DescriptorExposer de = new DescriptorExposer(Type.getType(SimpleExposed.class), "desc");
        de.addFieldGetter("toStringVal", STRING);
        Class descriptor = de.load(new BytecodeLoader.Loader());
        PyDataDescr instance = (PyDataDescr)descriptor.newInstance();
        assertEquals(SimpleExposed.TO_STRING_RETURN,
                     instance.__get__(new SimpleExposed(), PyType.fromClass(SimpleExposed.class))
                             .toString());
        assertFalse(instance.implementsDescrSet());
        assertFalse(instance.implementsDescrDelete());
    }

    public void testMethodSetter() throws Exception {
        DescriptorExposer de = new DescriptorExposer(Type.getType(SimpleExposed.class), "desc");
        de.addMethodGetter("toString", RETURN_STRING_DESCRIPTOR);
        de.addMethodSetter("setToString", "(Ljava/lang/String;)V");
        String newVal = "This is not what was there before";
        Class descriptor = de.load(new BytecodeLoader.Loader());
        PyDataDescr instance = (PyDataDescr)descriptor.newInstance();
        SimpleExposed se = new SimpleExposed();
        instance.__set__(se, Py.newString(newVal));
        assertEquals(newVal, se.toString());
        assertTrue(instance.implementsDescrSet());
        assertFalse(instance.implementsDescrDelete());
    }

    public void testFieldSetter() throws Exception {
        DescriptorExposer de = new DescriptorExposer(Type.getType(SimpleExposed.class), "desc");
        de.addFieldGetter("toStringVal", STRING);
        de.addFieldSetter("toStringVal", STRING);
        String newVal = "This is not what was there before";
        Class descriptor = de.load(new BytecodeLoader.Loader());
        PyDataDescr instance = (PyDataDescr)descriptor.newInstance();
        SimpleExposed se = new SimpleExposed();
        instance.__set__(se, Py.newString(newVal));
        assertEquals(newVal, se.toString());
        assertTrue(instance.implementsDescrSet());
        assertFalse(instance.implementsDescrDelete());
    }

    public void testMethodDel() throws Exception {
        DescriptorExposer de = new DescriptorExposer(Type.getType(SimpleExposed.class), "desc");
        de.addMethodGetter("toString", RETURN_STRING_DESCRIPTOR);
        de.addMethodDeleter("deleteToString", "()V");
        Class descriptor = de.load(new BytecodeLoader.Loader());
        PyDataDescr instance = (PyDataDescr)descriptor.newInstance();
        SimpleExposed se = new SimpleExposed();
        instance.__delete__(se);
        assertNull(se.toString());
        assertTrue(instance.implementsDescrDelete());
    }

    private static final String RETURN_STRING_DESCRIPTOR = Type.getMethodDescriptor(STRING,
                                                                                    new Type[0]);
}
