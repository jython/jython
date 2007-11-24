package org.python.expose;

import java.lang.reflect.Method;
import java.util.List;

import junit.framework.TestCase;

import org.python.core.Py;
import org.python.core.PyNewWrapper;
import org.python.core.PyObject;
import org.python.core.PyType;

public class TypeExposerTest extends TestCase {

    public void testGetName() {
        assertEquals("simpleexposed", new TypeExposer(SimpleExposed.class).getName());
        assertEquals("somethingcompletelydifferent", new TypeExposer(Rename.class).getName());
    }

    public void testNoExposed() {
        try {
            new TypeExposer(Unexposed.class);
            fail("Passing a class without @Exposed to ExposedClassProcessor should throw an IllegalArgumentException");
        } catch(IllegalArgumentException iae) {}
    }

    public void testMakeBuilder() throws InstantiationException, IllegalAccessException {
        TypeBuilder t = new TypeExposer(SimpleExposed.class).makeBuilder();
        assertEquals("simpleexposed", t.getName());
        assertEquals(SimpleExposed.class, t.getTypeClass());
        PyType type = PyType.fromClass(SimpleExposed.class);
        PyObject dict = t.getDict(type);
        assertNotNull(dict.__finditem__("simple_method"));
        assertNotNull(dict.__finditem__("prefixed"));
        assertNotNull(dict.__finditem__("__str__"));
        assertNotNull(dict.__finditem__("__repr__"));
        assertEquals(Py.One, type.__call__());
    }

    public void testBadNews() {
        for(Class cls : new Class[] {NonstaticNew.class, NoreturnNew.class}) {
            try {
                new TypeExposer(cls);
                fail("Passing a malformed constructor, as on " + cls
                        + ", should raise an IllegalArgumentException");
            } catch(IllegalArgumentException iae) {}
        }
    }

    public void testGoodNew() {
        TypeBuilder te = new TypeExposer(SimplestNew.class).makeBuilder();
        PyObject new_ = te.getDict(PyType.fromClass(SimplestNew.class)).__finditem__("__new__");
        assertEquals(Py.One, new_.__call__(PyType.fromClass(SimplestNew.class)));
    }

    public class Unexposed {}

    @ExposedType(name = "somethingcompletelydifferent")
    public class Rename {}

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
    public static class NoreturnNew {

        @ExposedNew
        public static void __new__(PyNewWrapper new_,
                                   boolean init,
                                   PyType subtype,
                                   PyObject[] args,
                                   String[] keywords) {}
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
