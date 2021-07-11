package org.python.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.fail;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.python.base.InterpreterError;
import org.python.core.Operations.Clash;
import org.python.core.PyType.Spec;

/**
 * An {@link Operations} object contains information about a Java class
 * considered as a Python object. There are several patterns to explore.
 * <p>
 * These tests can't work unless parts of {@link PyType} formation also work, so
 * there is a bit of overlap. In fact many of the {@code Operations} objects
 * involved are {@code PyType}s. We do not test that the {@code PyType}s
 * encountered are fully-working as a Python {@code type}.
 */
@DisplayName("The Operations object of")
class OperationsFormationTest {

    /**
     * A built-in Python type something like: <pre>
     * class A:
     *     pass
     * </pre>
     */
    static class A implements CraftedPyObject {
        static PyType TYPE = PyType.fromSpec(new Spec("A", MethodHandles.lookup()));
        private PyType type;

        A(PyType type) { this.type = type; }

        A() { this(TYPE); }

        @Override
        public PyType getType() { return type; }

        static class Derived extends A implements DerivedPyObject, DictPyObject {
            protected Map<Object, Object> __dict__;

            Derived(PyType type) {
                super(type);
                this.__dict__ = new HashMap<>();
            }

            @Override
            public Map<Object, Object> getDict() { return __dict__; }
        }

    }

    /**
     * A built-in Python type something like: <pre>
     * class B(A):
     *     pass
     * </pre>
     */
    static class B {
        static PyType TYPE = PyType.fromSpec(new Spec("B", MethodHandles.lookup()) //
                .base(A.TYPE));
    }

    /**
     * Built-in Python {@code class C} that has adopted implementations.
     */
    static class C implements CraftedPyObject {
        static PyType TYPE = PyType.fromSpec(new Spec("C", MethodHandles.lookup())
                .adopt(C1.class, C2.class).flagNot(PyType.Flag.BASETYPE));

        @Override
        public PyType getType() { return TYPE; }
    }

    /** An adopted implementation of Python class {@code C}. */
    static class C1 {}

    /** An adopted implementation of Python class {@code C}. */
    static class C2 {}

    /** Built-in Python {@code class BadC} identical to C. */
    static class BadC implements CraftedPyObject {
        static PyType TYPE = PyType.fromSpec(new Spec("BadC", MethodHandles.lookup())
                .adopt(BadC2.class).flagNot(PyType.Flag.BASETYPE));

        @Override
        public PyType getType() { return TYPE; }
    }

    /** An adopted implementation of Python class {@code BadC}. */
    static class BadC2 {}

    /**
     * A pure Java class (a found class in the tests)
     * {@code uk.co.farowl.vsj3.evo1.OperationsFormationTest.J}.
     */
    static class J {}

    /**
     * A Java class simulating one generated when we extend a found Java class
     * {@link J} in Python. There is no {@link PyType} corresponding directly to
     * this class (unless "found").
     */
    static class JDerived extends J implements DerivedPyObject, DictPyObject {

        /** The Python type of this instance. */
        private PyType type;
        protected Map<Object, Object> __dict__;

        JDerived(PyType type) {
            this.type = type;
            this.__dict__ = new HashMap<>();
        }

        @Override
        public Map<Object, Object> getDict() { return __dict__; }

        @Override
        public PyType getType() { return type; }
    }

    /**
     * Certain nested test classes implement these as standard. A base class here is
     * just a way to describe the tests once that reappear in each nested case.
     */
    abstract static class Base {

        // Working variables for the tests
        Operations ops;
        String repr;
        Class<?> javaClass;

        void setup(Class<?> javaClass, String repr) throws Throwable {
            this.javaClass = javaClass;
            this.repr = repr;
            this.ops = Operations.fromClass(javaClass);
        }

        /**
         * The {@link Operations} object finds the expected {@link PyType}, given the
         * target class definition.
         *
         * @throws Throwable unexpectedly
         */
        @Test
        void finds_expected_type() throws Throwable { fail("Not yet implemented"); }

        /**
         * The toString (repr) describes the Operations.
         *
         * @throws Throwable unexpectedly
         */
        @Test
        void has_expected_toString() throws Throwable { assertEquals(repr, ops.toString()); }

    }

    @Nested
    @DisplayName("a built-in type")
    class BuiltInTest extends Base {
        @BeforeEach
        void setup() throws Throwable { setup(A.class, "<class 'A'>"); }

        @Override
        @Test
        void finds_expected_type() {
            A a = new A();
            PyType t = ops.type(a);
            assertSame(A.TYPE, t);
        }
    }

    @Nested
    @DisplayName("a built-in type adopting classes")
    class BuiltInAdoptiveTest extends Base {
        @BeforeEach
        void setup() throws Throwable { setup(C.class, "<class 'C'>"); }

        @Override
        @Test
        void finds_expected_type() {
            /*
             * The Operations object of the canonical implementation is the type itself.
             * (Python must touch C before C2.)
             */
            assertSame(C.TYPE, ops);
            C c = new C();
            assertSame(C.TYPE, ops.type(c));
            /*
             * An instance of the adopted implementation has the adopting type.
             */
            C2 c2 = new C2();
            Operations ops2 = Operations.of(c2);
            assertNotSame(C.TYPE, ops2);
            assertSame(C.TYPE, ops2.type(c2));
        }

        @Override
        @Test
        void has_expected_toString() throws Throwable {
            super.has_expected_toString();
            // Only seen in debugging:
            Operations ops2 = Operations.fromClass(C2.class);
            assertEquals("C2 as <class 'C'>", ops2.toString());
        }

        /**
         * This is an un-feature. The test is like {@link #finds_expected_type()} but
         * the adopted class gets handled as a Python object before its adopting class
         * can create its {@link PyType}. This causes an unintended binding that
         * prevents {@link BadC} initialising correctly.The problem seems unavoidable,
         * and the requirement is to detect it.
         */
        @Test
        void is_sensitive_to_order_of_use() {
            /*
             * An instance of the adopted implementation has the fails to have the adopting
             * type if it treated as a Python object before that type.
             */
            BadC2 c2 = new BadC2(); // ok
            Operations ops2 = Operations.of(c2);
            // That created a PyType but not for BadC
            assertNotSame(BadC.class, ops2.type(c2).definingClass);
            /*
             * The Operations object of the canonical implementation is the type itself.
             * BadC will try to adopt BadC2 and this is detected as a clash.
             */
            try {
                Operations.fromClass(BadC.class);
                fail("Exception not raised when " + "adoped class is exposed prematurely");
            } catch (ExceptionInInitializerError e) {
                Throwable cause = e.getCause();
                if (!(cause instanceof InterpreterError))
                    throw e;
                cause = cause.getCause();
                if (!(cause instanceof Clash))
                    throw e;
            }
        }
    }

    @Nested
    @DisplayName("a Python sub-class of a built-in type")
    class PythonSubBuiltInTest extends Base {

        @BeforeEach
        void setup() throws Throwable {
            // toString is seen only in debugging
            setup(A.Derived.class, "Derived");
        }

        /**
         * Simulating a Python sub-class of a built-in Python type something like: <pre>
         * class MyA(A):
         *     pass
         * </pre> {@code MyA} must be a Java sub-class of {@code A} in order that
         * methods defined in {@code A} in Java be applicable to instances of
         * {@code MyA}.
         */
        @Override
        @Test
        void finds_expected_type() {
            // Define a new type
            // XXX cheating by short-cutting type.__new__
            Spec specMyA = new Spec("MyA", A.Derived.class).base(A.TYPE);
            PyType typeMyA = PyType.fromSpec(specMyA);

            // Define an object of that type
            // XXX again, cheating by short-cutting type.__call__
            Object obj = new A.Derived(typeMyA);
            Operations ops = Operations.of(obj);

            // This ops is not the type of an A or a MyA
            assertNotSame(A.TYPE, ops);
            assertNotSame(typeMyA, ops);

            // However, the type of an instance is MyA
            assertSame(typeMyA, ops.type(obj));
        }
    }

    @Nested
    @DisplayName("a found Java class")
    class FoundTest extends Base {
        @BeforeEach
        void setup() throws Throwable { setup(J.class, "<class 'J'>"); }

        @Override
        @Test
        void finds_expected_type() {
            /*
             * The Operations object is the type itself. Even if J has initialised,
             * Operations.Registry.computeValue will not find it in opsMap.
             */
            PyType type = ops.uniqueType();
            assertSame(type, ops);

            J obj = new J();
            assertSame(type, ops.type(obj));

            // We probably expect the simple name "J". Probably.
            String expectedName = J.class.getSimpleName();
            assertEquals(expectedName, type.getName());
        }
    }

    @Nested
    @DisplayName("a Python sub-class of a found Java class")
    class PythonSubFoundTest extends Base {
        @BeforeEach
        void setup() throws Throwable {
            // toString is seen only in debugging
            setup(JDerived.class, "Derived");
        }

        @Override
        @Test
        void finds_expected_type() throws Throwable {
            // Define a new type
            // XXX cheating by short-cutting type.__new__
            PyType JTYPE = PyType.fromClass(J.class);
            Spec specMyJ = new Spec("MyJ", JDerived.class).base(JTYPE);
            PyType typeMyJ = PyType.fromSpec(specMyJ);

            // XXX cheating by short-cutting type.__call__
            Object obj = new JDerived(typeMyJ);
            Operations ops = Operations.of(obj);

            // This ops is not the type of a J or a MyJ
            assertNotSame(JTYPE, ops);
            assertNotSame(typeMyJ, ops);

            // However, the type of an instance is MyA
            assertSame(typeMyJ, ops.type(obj));
        }
    }
}
