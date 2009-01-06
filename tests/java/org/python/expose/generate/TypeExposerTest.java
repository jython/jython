package org.python.expose.generate;

import java.io.IOException;

import junit.framework.TestCase;

import org.objectweb.asm.Type;
import org.python.core.Py;
import org.python.core.PyNewWrapper;
import org.python.core.PyObject;
import org.python.core.PyType;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedType;
import org.python.expose.TypeBuilder;

public class TypeExposerTest extends InterpTestCase {

    public void testMakeBuilder() throws Exception {
        ExposedTypeProcessor etp = new ExposedTypeProcessor(getClass().getClassLoader()
                .getResourceAsStream("org/python/expose/generate/SimpleExposed.class"));
        TypeBuilder t = etp.getTypeExposer().makeBuilder();
        assertEquals("simpleexposed", t.getName());
        assertEquals(SimpleExposed.class, t.getTypeClass());
        assertEquals(false, t.getIsBaseType());
        PyType type = PyType.fromClass(SimpleExposed.class);
        PyObject dict = t.getDict(type);
        assertNotNull(dict.__finditem__("simple_method"));
        assertNotNull(dict.__finditem__("prefixed"));
        assertNotNull(dict.__finditem__("__str__"));
        assertNotNull(dict.__finditem__("__repr__"));
        assertNotNull(dict.__finditem__("tostring"));
        dict.__finditem__("tostring").__get__(new SimpleExposed(), type);
    }

    public void testGoodNew() throws IOException {
        ExposedTypeProcessor etp = new ExposedTypeProcessor(getClass().getClassLoader()
                .getResourceAsStream("org/python/expose/generate/TypeExposerTest$SimplestNew.class"));
        TypeBuilder te = etp.getTypeExposer().makeBuilder();
        assertEquals(true, te.getIsBaseType());
        PyObject new_ = te.getDict(PyType.fromClass(SimplestNew.class)).__finditem__("__new__");
        assertEquals(Py.One, new_.__call__(PyType.fromClass(SimplestNew.class)));
    }

    public void testCatchingDupes() throws IOException {
        try {
            new ExposedTypeProcessor(getClass().getClassLoader()
                    .getResourceAsStream("org/python/expose/generate/TypeExposerTest$DupeMethodNames.class"));
            fail("Shouldn't be able to create a type with identical names in the dict");
        } catch(InvalidExposingException ite) {}
    }

    @ExposedType
    public static class DupeMethodNames {

        @ExposedMethod
        public void blah() {}

        @ExposedMethod(names = "blah")
        public void bleh() {}
    }

    @ExposedType
    public class NonstaticNew {

        @ExposedNew
        public PyObject __new__(PyNewWrapper new_,
                                boolean init,
                                PyType subtype,
                                PyObject[] args,
                                String[] keywords) {
            return null;
        }
    }

    @ExposedType
    public static class SimplestNew {

        @ExposedNew
        public static PyObject __new__(PyNewWrapper new_,
                                       boolean init,
                                       PyType subtype,
                                       PyObject[] args,
                                       String[] keywords) {
            return Py.One;
        }
    }
}
