// Copyright (c)2026 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.lang.invoke.MethodHandles;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.python.internal.Util;
import org.python.types.Feature;
import org.python.types.TypeSpec;
import org.python.types.WithClass;
import org.python.types.WithDict;

/**
 * This test is primarily concerned with attribute access. It exercises
 * {@code __getattribute__}, {@code __setattr__} and {@code __delattr__}
 * on a variety of types, by calling the descriptors directly.
 * <p>
 * There are significant differences between access to a {@link PyType}
 * and to any other Python type, and they get separate tests.
 */
class AttrGetSetTest extends UnitTestSupport {

    private static final PyType OBJECT = PyObject.TYPE;

    /** {@code object.__getattribute__} */
    private static final PyWrapperDescr OBJECT_GETATTRIBUTE =
            (PyWrapperDescr)OBJECT.lookup("__getattribute__");
    /** {@code object.__setattr__} */
    private static final PyWrapperDescr OBJECT_SETATTR =
            (PyWrapperDescr)OBJECT.lookup("__setattr__");
    /** {@code object.__delattr__} */
    private static final PyWrapperDescr OBJECT_DELATTR =
            (PyWrapperDescr)OBJECT.lookup("__delattr__");

    /** {@code type.__getattribute__} */
    private static final PyWrapperDescr TYPE_GETATTRIBUTE =
            (PyWrapperDescr)PyType.TYPE().lookup("__getattribute__");
    /** {@code type.__setattr__} */
    private static final PyWrapperDescr TYPE_SETATTR =
            (PyWrapperDescr)PyType.TYPE().lookup("__setattr__");
    /** {@code type.__delattr__} */
    private static final PyWrapperDescr TYPE_DELATTR =
            (PyWrapperDescr)PyType.TYPE().lookup("__delattr__");

    /**
     * This class is effectively a built-in (or extension) Python type,
     * which we create in order to include a Python subclass using the
     * expected pattern, with an instance dictionary.
     */
    static abstract class TestBase implements WithClass {

        static final PyType TYPE = PyType.fromSpec( //
                new TypeSpec("attrGetSetTestBase", MethodHandles.lookup()).add(Feature.BASETYPE));

        protected TestBase() {}

        @Override
        public PyType getType() { return TYPE; }

        /** Python sub-classes extend this class. */
        // FIXME Old inadequate concept of Python subclass
        abstract static class Derived extends TestBase implements WithDict {

            protected PyType type;

            protected Derived(PyType type) { this.type = type; }

            protected PyDict dict = new PyDict();

            @Override
            public Map<Object, Object> getDict() { return dict; }
        }
    }

    /**
     * This class defines a Python subclass of the Python type defined by
     * {@link TestBase}. Instances of this class have a dictionary, which
     * will be consulted in the search for attributes. The type itself is
     * also mutable (allows change to its attributes).
     */
    static class TestSubclass extends TestBase.Derived {

        static final PyType TYPE = PyType.fromSpec( //
                new TypeSpec("attrGetSetTestSubclass", MethodHandles.lookup()) //
                        .base(TestBase.TYPE));

        protected TestSubclass() { super(TYPE); }

        @SuppressWarnings({"unused", "static-method"})
        private Object __str__() { return "a string"; }
    }

    @Nested
    @DisplayName("Get method-wrapper from instance and call it")
    class GetMethodFromInstance {

        /**
         * Get a bound unary method ({@code method-wrapper}) via attribute
         * access on an instance of a built-in type and call it.
         *
         * @throws Throwable unexpectedly
         */
        @Test
        void where_int_unary() throws Throwable {
            // m = (42).__repr__
            PyMethodWrapper m = getattribute(42, "__repr__");
            // "42" == m()
            assertEquals("42", m.__call__(Util.EMPTY_ARRAY, null));
        }

        /**
         * Get a bound binary method ({@code method-wrapper}) via attribute
         * access on an instance of a built-in type and call it.
         *
         * @throws Throwable unexpectedly
         */
        @Test
        void where_int_binary() throws Throwable {
            // m = (51).__sub__
            PyMethodWrapper m = getattribute(51, "__sub__");
            // 42 == m(9)
            assertEquals(42, m.__call__(new Object[] {9}, null));
        }

        /**
         * Get a unary method ({@code method-wrapper}) via attribute access on
         * an instance of a Python subclass and call it.
         *
         * @throws Throwable unexpectedly
         */
        @Test
        void where_python_subclass() throws Throwable {
            // x = attrGetSetTestSubclass()
            TestSubclass x = new TestSubclass();
            // m = x.__str__
            PyMethodWrapper m = getattribute(x, "__str__");
            assertStartsWith("<method-wrapper '__str__'", m.toString());
            // s == m()
            Object s = m.__call__(Util.EMPTY_ARRAY, null);
            assertEquals("a string", s);
        }

        /**
         * Call {@code object.__getattribute__} to retrieve and bind a slot
         * wrapper for the name given. This is dispatched through the
         * slot-wrapper.
         *
         * @param o on which to seek the attribute
         * @param name of the attribute
         * @return {@code o.name}
         * @throws Throwable from the implementation
         */
        private PyMethodWrapper getattribute(Object o, String name) throws Throwable {
            PyMethodWrapper m =
                    (PyMethodWrapper)OBJECT_GETATTRIBUTE.__call__(new Object[] {o, name}, null);
            assertSame(o, m.self);
            // Check same effect with PyUnicode
            PyMethodWrapper m2 = (PyMethodWrapper)OBJECT_GETATTRIBUTE
                    .__call__(new Object[] {o, newPyUnicode(name)}, null);
            assertSame(m.self, m2.self);
            assertSame(m.descr, m2.descr);
            // Return the original
            return m;
        }
    }

    @Nested
    @DisplayName("Get value attribute from instance")
    class GetAttrFromInstance {

        /**
         * Get an object via attribute access on an instance of a Python
         * subclass.
         *
         * @throws Throwable unexpectedly
         */
        @Test
        void where_python_subclass() throws Throwable {
            // x = attrGetSetTestSubclass()
            TestSubclass x = new TestSubclass();
            // x.a = 42 # using Java API
            x.getDict().put("a", 42);
            // r = x.a
            Object r = getattribute(x, "a");
            // r == 42
            assertEquals(42, r);
        }

        /**
         * Call {@code object.__getattribute__} on the object for the name
         * given. This is dispatched through the slot-wrapper.
         *
         * @param o on which to seek the attribute
         * @param name of the attribute
         * @return {@code o.name}
         * @throws Throwable from the implementation
         */
        private Object getattribute(Object o, String name) throws Throwable {
            Object r = OBJECT_GETATTRIBUTE.__call__(new Object[] {o, name}, null);
            // Check same effect with PyUnicode
            Object r2 = OBJECT_GETATTRIBUTE.__call__(new Object[] {o, newPyUnicode(name)}, null);
            assertSame(r, r2);
            // Return the original
            return r;
        }
    }

    @Nested
    @DisplayName("Set value attribute on instance")
    class SetAttrOnInstance {

        /**
         * Set an object via attribute access on an instance of a Python
         * subclass.
         *
         * @throws Throwable unexpectedly
         */
        @Test
        void where_python_subclass() throws Throwable {
            // x = attrGetSetTestSubclass()
            TestSubclass x = new TestSubclass();
            // x.a = 42
            setattr(x, "a", 42);
            // r = x.a # via Java API
            Object r = x.getDict().get("a");
            // r == 42
            assertEquals(42, r);
        }

        /**
         * Call {@code object.__setattr__} on the object for the name given.
         * This is dispatched through the slot-wrapper.
         *
         * @param o on which to set the attribute
         * @param name of the attribute
         * @param value to set
         * @throws Throwable from the implementation
         */
        protected void setattr(Object o, String name, Object value) throws Throwable {
            OBJECT_SETATTR.__call__(new Object[] {o, name, value}, null);
        }
    }

    @Nested
    @DisplayName("Delete value attribute on instance")
    class DelAttrFromInstance {

        /**
         * Delete an object via attribute access on an instance of a Python
         * subclass
         *
         * @throws Throwable unexpectedly
         */
        @Test
        void where_python_subclass() throws Throwable {
            // x = attrGetSetTestSubclass()
            TestSubclass x = new TestSubclass();
            // x.a = 42 # using Java API
            x.getDict().put("a", 42);
            // del x.a
            delattr(x, "a");
            // Verify x.a is missing by Java API
            assertNull(x.getDict().get("a"));
        }

        /**
         * Call {@code type.__delattr__} on the object for the name given. This
         * is dispatched through the slot-wrapper.
         *
         * @param o on which to delete the attribute
         * @param name of the attribute
         * @throws Throwable from the implementation
         */
        protected void delattr(Object o, String name) throws Throwable {
            OBJECT_DELATTR.__call__(new Object[] {o, name}, null);
        }
    }

    @Nested
    @DisplayName("Get value attribute from a type")
    class GetAttrFromType {

        /**
         * Get an object via attribute access on a Python type that allows one
         * to be set.
         *
         * @throws Throwable unexpectedly
         */
        @Test
        void where_python_subclass() throws Throwable {
            // C = attrGetSetTestSubclass
            PyType C = TestSubclass.TYPE;
            // C.a = 42
            TYPE_SETATTR.__call__(new Object[] {C, "a", 42}, null);
            // r = x.a
            Object r = getattribute(C, "a");
            // r == 42
            assertEquals(42, r);
        }

        /**
         * Call {@code type.__getattribute__} on the object for the name given.
         * This is dispatched through the slot-wrapper.
         *
         * @param t on which to seek the attribute
         * @param name of the attribute
         * @return {@code t.name}
         * @throws Throwable from the implementation
         */
        private Object getattribute(PyType t, String name) throws Throwable {
            Object r = TYPE_GETATTRIBUTE.__call__(new Object[] {t, name}, null);
            // Check same effect with PyUnicode
            Object r2 = TYPE_GETATTRIBUTE.__call__(new Object[] {t, newPyUnicode(name)}, null);
            assertSame(r, r2);
            // Return the original
            return r;
        }
    }

    @Nested
    @DisplayName("Set value attribute on a type")
    class SetAttrOnType {

        /**
         * Set an object via attribute access on a Python type that allows it.
         *
         * @throws Throwable unexpectedly
         */
        @Test
        void where_python_subclass() throws Throwable {
            // C = attrGetSetTestSubclass
            PyType C = TestSubclass.TYPE;
            // C.a = 42
            setattr(C, "a", 42);
            // r = x.a
            Object r = C.lookup("a");
            // r == 42
            assertEquals(42, r);
        }

        /**
         * Call {@code type.__setattr__} on the object for the name given. This
         * is dispatched through the slot-wrapper.
         *
         * @param t on which to seek the attribute
         * @param name of the attribute
         * @throws Throwable from the implementation
         */
        private void setattr(PyType t, String name, Object value) throws Throwable {
            TYPE_SETATTR.__call__(new Object[] {t, name, value}, null);
        }
    }

    @Nested
    @DisplayName("Fail to set value attribute on a type as object")
    class SetAttrOnTypeAsObject extends SetAttrOnInstance {
        /*
         * Check object.__setattr__ cannot be applied to a type (the Carlo Verre
         * hack).
         */

        /**
         * It is not possible to set an object via attribute
         * {@code object.__setattr__} access on a Python type (even one that
         * allows it via {@code type.__setattr__}).
         *
         * @throws Throwable unexpectedly
         */
        @Override
        @Test
        void where_python_subclass() throws Throwable {
            // C = attrGetSetTestSubclass
            PyType C = TestSubclass.TYPE;
            // C.a = 42
            assertRaises(PyExc.TypeError, () -> setattr(C, "a", 42));
        }
    }

    @Nested
    @DisplayName("Delete attribute from a type")
    class DelAttrFromType {

        /**
         * Delete an attribute via attribute access on a Python type.
         *
         * @throws Throwable unexpectedly
         */
        @Test
        void where_python_subclass() throws Throwable {
            // C = attrGetSetTestSubclass
            PyType C = TestSubclass.TYPE;
            // C.a = 42
            TYPE_SETATTR.__call__(new Object[] {C, "a", 42}, null);
            // del x.a
            delattr(C, "a");
            // Check attribute absent
            assertNull(C.lookup("a"));
        }

        /**
         * Call {@code type.__delattr__} on the object for the name given. This
         * is dispatched through the slot-wrapper.
         *
         * @param t on which to seek the attribute
         * @param name of the attribute
         * @throws Throwable from the implementation
         */
        private void delattr(PyType t, String name) throws Throwable {
            TYPE_DELATTR.__call__(new Object[] {t, name}, null);
        }
    }

    @Nested
    @DisplayName("Fail to delete attribute from a type as object")
    class DelAttrOnTypeAsObject extends DelAttrFromInstance {
        /*
         * Check object.__delattr__ cannot be applied to a type (the Carlo Verre
         * hack).
         */

        /**
         * It is not possible to delete an object via attribute
         * {@code object.__delattr__} access on a Python type (even one that
         * allows setting via {@code type.__setattr__}).
         *
         * @throws Throwable unexpectedly
         */
        @Override
        @Test
        void where_python_subclass() throws Throwable {
            // C = attrGetSetTestSubclass
            PyType C = TestSubclass.TYPE;
            // C.a = 42
            TYPE_SETATTR.__call__(new Object[] {C, "a", 42}, null);
            // del C.a
            assertRaises(PyExc.TypeError, () -> delattr(C, "a"));
        }
    }

}
