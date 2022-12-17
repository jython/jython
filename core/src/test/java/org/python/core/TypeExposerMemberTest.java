package org.python.core;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.invoke.MethodHandles;
import java.math.BigInteger;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.python.core.Exposed.DocString;
import org.python.core.Exposed.Member;

/**
 * Test that members exposed by a Python <b>type</b> defined in
 * Java, using the annotation defined in {@link Exposed.Member} on
 * fields implementing them, results in data descriptors with
 * characteristics that correspond to the definition.
 * <p>
 * There is a nested test suite for each pattern of characteristics.
 */
@DisplayName("For a member exposed by a type")
class TypeExposerMemberTest extends UnitTestSupport {

    /**
     * Java base class of a Python type definition showing that some of
     * the member definitions explored in the tests can be
     * Java-inherited.
     */
    private static class BaseMembers {
        @Member
        int i;

        /** String with change of name. */
        @Member("text")
        String t;
    }

    /**
     * A Python type definition that exhibits a range of member
     * definitions explored in the tests.
     */
    private static class ObjectWithMembers extends BaseMembers {

        static PyType TYPE =
                PyType.fromSpec(new PyType.Spec("ObjectWithMembers", MethodHandles.lookup())
                        .adopt(DerivedWithMembers.class));

        @Member
        @DocString("My test x")
        double x;

        /** String can be properly deleted without popping up as None */
        @Member(optional = true)
        String s;

        /** {@code Object} member (not optional) */
        @Member
        Object obj;

        /** Read-only access. */
        @Member(readonly = true)
        int i2;

        /** Read-only access since final. */
        @Member
        final double x2;

        /** Read-only access given first. */
        @Member(readonly = true, value = "text2")
        String t2;

        /** {@code PyTuple} member. */
        @Member
        PyTuple tup;

        /** {@code PyUnicode} member: not practical to allow set. */
        @Member(readonly = true)
        PyUnicode strhex;

        /**
         * Give all the members values based on a single "seed"
         *
         * @param value starting value for all the members
         */
        ObjectWithMembers(double value) {
            x2 = x = value;
            i2 = i = Math.round((float)value);
            t2 = t = s = String.format("%d", i);
            obj = i;
            tup = new PyTuple(i, x, t);
            strhex = newPyUnicode(Integer.toString(i, 16));
        }
    }

    /**
     * A class that extends the above, with the same Python type. We
     * want to check that what we're doing to reflect on the parent
     * produces descriptors we can apply to a sub-class.
     */
    private static class DerivedWithMembers extends ObjectWithMembers {
        DerivedWithMembers(double value) { super(value); }
    }

    /**
     * Certain nested test classes implement these as standard. A base
     * class here is just a way to describe the tests once that reappear
     * in each nested case.
     */
    abstract static class Base {

        // Working variables for the tests
        /** Name of the attribute. */
        String name;
        /** Documentation string. */
        String doc;
        /** Unbound descriptor by type access to examine or call. */
        PyMemberDescr md;
        /** The object on which to attempt access. */
        ObjectWithMembers o;
        /**
         * Another object on which to attempt access (in case we are getting
         * instances mixed up).
         */
        ObjectWithMembers p;

        void setup(String name, String doc, double oValue, double pValue) throws Throwable {
            this.name = name;
            this.doc = doc;
            try {
                this.md = (PyMemberDescr)ObjectWithMembers.TYPE.lookup(name);
                this.o = new ObjectWithMembers(oValue);
                this.p = new ObjectWithMembers(pValue);
            } catch (ExceptionInInitializerError eie) {
                // Errors detected by the Exposer get wrapped so:
                Throwable t = eie.getCause();
                throw t == null ? eie : t;
            }
        }

        void setup(String name, double oValue, double pValue) throws Throwable {
            setup(name, null, oValue, pValue);
        }

        /**
         * The attribute is a member descriptor that correctly reflects the
         * annotations in the defining class.
         *
         * @throws Throwable unexpectedly
         */
        @Test
        void descr_has_expected_fields() throws Throwable {
            assertEquals(name, md.name);
            assertEquals(doc, md.doc);
            String s = String.format("<member '%s' of 'ObjectWithMembers' objects>", name);
            assertEquals(s, md.toString());
            assertEquals(s, Abstract.repr(md));
        }

        /**
         * The string (repr) describes the type and attribute.
         *
         * @throws Throwable unexpectedly
         */
        void checkToString() throws Throwable {
            String s = String.format("<member '%s' of 'ObjectWithMembers' objects>", name);
            assertEquals(s, md.toString());
            assertEquals(s, Abstract.repr(md));
        }

        /**
         * The member descriptor may be used to read the field in an
         * instance of the object.
         */
        abstract void descr_get_works();

        /**
         * {@link Abstract#getAttr(Object, String)} may be used to read the
         * field in an instance of the object.
         *
         * @throws Throwable unexpectedly
         */
        abstract void abstract_getAttr_works() throws Throwable;
    }

    /**
     * Add tests of setting values to the base tests.
     */
    abstract static class BaseSettable extends Base {

        /**
         * The member descriptor may be used to set the field in an instance
         * of the object.
         *
         * @throws Throwable unexpectedly
         */
        abstract void descr_set_works() throws Throwable;

        /**
         * {@link Abstract#setAttr(Object, String, Object)} may be used to
         * set the field in an instance of the object.
         *
         * @throws Throwable unexpectedly
         */
        abstract void abstract_setAttr_works() throws Throwable;

        /**
         * The member raises {@link TypeError} when supplied a value of
         * unacceptable type.
         *
         * @throws Throwable unexpectedly
         */
        abstract void set_detects_TypeError() throws Throwable;
    }

    /**
     * Base test of settable attribute with primitive implementation.
     */
    abstract static class BaseSettablePrimitive extends BaseSettable {

        /**
         * Attempting to delete the member implemented by a primitive raises
         * {@link TypeError}.
         */
        @Test
        void rejects_descr_delete() {
            assertThrows(TypeError.class, () -> md.__delete__(o));
            assertThrows(TypeError.class, () -> md.__set__(o, null));
        }

        /**
         * Attempting to delete the member implemented by a primitive raises
         * {@link TypeError}.
         */
        @Test
        void rejects_abstract_delAttr() {
            assertThrows(TypeError.class, () -> Abstract.delAttr(o, name));
            assertThrows(TypeError.class, () -> Abstract.setAttr(o, name, null));
        }
    }

    @Nested
    @DisplayName("implemented as an int")
    class TestInt extends BaseSettablePrimitive {

        @BeforeEach
        void setup() throws AttributeError, Throwable { setup("i", 42, -1); }

        @Override
        @Test
        void descr_get_works() {
            assertEquals(42, md.__get__(o, null));
            assertEquals(-1, md.__get__(p, null));
        }

        @Override
        @Test
        void abstract_getAttr_works() throws Throwable {
            assertEquals(42, Abstract.getAttr(o, name));
            assertEquals(-1, Abstract.getAttr(p, name));
        }

        @Override
        @Test
        void descr_set_works() throws Throwable {
            md.__set__(o, 43);
            md.__set__(p, BigInteger.valueOf(44));
            assertEquals(43, o.i);
            assertEquals(44, p.i);
        }

        @Override
        @Test
        void abstract_setAttr_works() throws Throwable {
            Abstract.setAttr(o, name, 43);
            Abstract.setAttr(p, name, BigInteger.valueOf(44));
            assertEquals(43, o.i);
            assertEquals(44, p.i);
        }

        @Override
        @Test
        void set_detects_TypeError() throws Throwable {
            // Things that are not a Python int
            assertThrows(TypeError.class, () -> md.__set__(o, "Gumby"));
            assertThrows(TypeError.class, () -> Abstract.setAttr(p, name, 1.0));
            assertThrows(TypeError.class, () -> md.__set__(o, Py.None));
        }

    }

    @Nested
    @DisplayName("implemented as a double")
    class TestDouble extends BaseSettablePrimitive {

        @BeforeEach
        void setup() throws AttributeError, Throwable { setup("x", "My test x", 42, -1); }

        @Override
        @Test
        void descr_get_works() {
            assertEquals(42.0, md.__get__(o, null));
            assertEquals(-1.0, md.__get__(p, null));
        }

        @Override
        @Test
        void abstract_getAttr_works() throws Throwable {
            assertEquals(42.0, Abstract.getAttr(o, name));
            assertEquals(-1.0, Abstract.getAttr(p, name));
        }

        @Override
        @Test
        void descr_set_works() throws Throwable {
            md.__set__(o, 1.125);
            md.__set__(p, BigInteger.valueOf(111_222_333_444L));
            assertEquals(1.125, o.x);
            assertEquals(111222333444.0, p.x);
        }

        @Override
        @Test
        void abstract_setAttr_works() throws Throwable {
            Abstract.setAttr(o, name, 1.125);
            Abstract.setAttr(p, name, BigInteger.valueOf(111_222_333_444L));
            assertEquals(1.125, o.x);
            assertEquals(111222333444.0, p.x);
        }

        @Override
        @Test
        void set_detects_TypeError() throws Throwable {
            // Things that are not a Python float
            assertThrows(TypeError.class, () -> md.__set__(o, "Gumby"));
            assertThrows(TypeError.class, () -> Abstract.setAttr(p, name, "42"));
            assertThrows(TypeError.class, () -> md.__set__(o, Py.None));
        }

    }

    /**
     * Base test of settable attribute with object reference
     * implementation.
     */
    abstract static class BaseSettableReference extends BaseSettable {

        /**
         * The member descriptor may be used to delete a field from an
         * instance of the object, meaning set it to {@code null}
         * internally, appearing as {@code None} externally.
         *
         * @throws Throwable unexpectedly
         */
        @Test
        void descr_delete_sets_None() throws Throwable {
            md.__delete__(o);
            assertEquals(Py.None, md.__get__(o, null));
            // __delete__ is idempotent
            md.__delete__(o);
            assertEquals(Py.None, md.__get__(o, null));
        }

        /**
         * {@link Abstract#delAttr(Object, String)} to delete a field from
         * an instance of the object, meaning set it to {@code null}
         * internally, appearing as {@code None} externally.
         *
         * @throws Throwable unexpectedly
         */
        @Test
        void abstract_delAttr_sets_None() throws Throwable {
            Abstract.delAttr(o, name);
            assertEquals(Py.None, Abstract.getAttr(o, name));
            // delAttr is idempotent
            Abstract.delAttr(o, name);
            assertEquals(Py.None, Abstract.getAttr(o, name));
        }
    }

    @Nested
    @DisplayName("implemented as a String")
    class TestString extends BaseSettableReference {

        @BeforeEach
        void setup() throws AttributeError, Throwable { setup("text", 42, -1); }

        @Override
        @Test
        void descr_get_works() {
            assertEquals("42", md.__get__(o, null));
            assertEquals("-1", md.__get__(p, null));
        }

        @Override
        @Test
        void abstract_getAttr_works() throws Throwable {
            assertEquals("42", Abstract.getAttr(o, name));
            assertEquals("-1", Abstract.getAttr(p, name));
        }

        @Override
        @Test
        void descr_set_works() throws Throwable {
            md.__set__(o, "D.P.");
            md.__set__(p, newPyUnicode("Gumby"));
            assertEquals("D.P.", o.t);
            assertEquals("Gumby", p.t);
            // __set__ works after delete
            md.__delete__(o);
            assertNull(o.t);
            md.__set__(o, "Palin");
            assertEquals("Palin", o.t);
        }

        @Override
        @Test
        void abstract_setAttr_works() throws Throwable {
            Abstract.setAttr(o, name, "D.P.");
            Abstract.setAttr(p, name, "Gumby");
            assertEquals("D.P.", o.t);
            assertEquals("Gumby", p.t);
            // setAttr works after delete
            Abstract.delAttr(o, name);
            assertNull(o.t);
            Abstract.setAttr(o, name, "Palin");
            assertEquals("Palin", o.t);
        }

        @Override
        @Test
        void set_detects_TypeError() throws Throwable {
            // Things that are not a Python str
            assertThrows(TypeError.class, () -> md.__set__(o, 1));
            assertThrows(TypeError.class, () -> Abstract.setAttr(p, name, 10.0));
            assertThrows(TypeError.class, () -> md.__set__(o, new Object()));
        }
    }

    /**
     * Base test of an optional attribute, necessarily with object
     * reference implementation.
     */
    abstract static class BaseOptionalReference extends BaseSettable {

        /**
         * The member descriptor may be used to delete a field from an
         * instance of the object, causing it to disappear externally.
         *
         * @throws Throwable unexpectedly
         */
        @Test
        void descr_delete_removes() throws Throwable {
            md.__delete__(o);
            // After deletion, ...
            // ... __get__ raises AttributeError
            assertThrows(AttributeError.class, () -> md.__get__(o, null));
            // ... __delete__ raises AttributeError
            assertThrows(AttributeError.class, () -> md.__delete__(o));
        }

        /**
         * {@link Abstract#delAttr(Object, String)} to delete a field from
         * an instance of the object, causing it to disappear externally.
         *
         * @throws Throwable unexpectedly
         */
        @Test
        void abstract_delAttr_removes() throws Throwable {
            Abstract.delAttr(o, name);
            // After deletion, ...
            // ... getAttr and delAttr raise AttributeError
            assertThrows(AttributeError.class, () -> Abstract.getAttr(o, name));
            assertThrows(AttributeError.class, () -> Abstract.delAttr(o, name));
        }

    }

    @Nested
    @DisplayName("implemented as an optional String")
    class TestOptionalString extends BaseOptionalReference {

        @BeforeEach
        void setup() throws AttributeError, Throwable { setup("s", 42, -1); }

        @Override
        @Test
        void descr_get_works() {
            assertEquals("42", md.__get__(o, null));
            assertEquals("-1", md.__get__(p, null));
        }

        @Override
        @Test
        void abstract_getAttr_works() throws Throwable {
            assertEquals("42", Abstract.getAttr(o, name));
            assertEquals("-1", Abstract.getAttr(p, name));
        }

        @Override
        @Test
        void descr_set_works() throws Throwable {
            md.__set__(o, "D.P.");
            md.__set__(p, "Gumby");
            assertEquals("D.P.", o.s);
            assertEquals("Gumby", p.s);
            // __set__ works after delete
            md.__delete__(o);
            assertNull(o.s);
            md.__set__(o, "Palin");
            assertEquals("Palin", o.s);
        }

        @Override
        @Test
        void abstract_setAttr_works() throws Throwable {
            Abstract.setAttr(o, name, "D.P.");
            Abstract.setAttr(p, name, newPyUnicode("Gumby"));
            assertEquals("D.P.", o.s);
            assertEquals("Gumby", p.s);
            // setAttr works after delete
            Abstract.delAttr(o, name);
            assertNull(o.s);
            Abstract.setAttr(o, name, "Palin");
            assertEquals("Palin", o.s);
        }

        @Override
        @Test
        void set_detects_TypeError() throws Throwable {
            // Things that are not a Python str
            assertThrows(TypeError.class, () -> md.__set__(o, 1));
            assertThrows(TypeError.class, () -> Abstract.setAttr(p, name, 10.0));
            assertThrows(TypeError.class, () -> md.__set__(o, new Object()));
        }
    }

    @Nested
    @DisplayName("implemented as an Object")
    class TestObject extends BaseSettableReference {

        @BeforeEach
        void setup() throws AttributeError, Throwable { setup("obj", 42, -1); }

        @Override
        @Test
        void descr_get_works() {
            assertEquals(42, md.__get__(o, null));
            assertEquals(-1, md.__get__(p, null));
        }

        @Override
        @Test
        void abstract_getAttr_works() throws Throwable {
            assertEquals(42, Abstract.getAttr(o, name));
            assertEquals(-1, Abstract.getAttr(p, name));
        }

        @Override
        @Test
        void descr_set_works() throws Throwable {
            final Object dp = "D.P.", gumby = newPyUnicode("Gumby");
            md.__set__(o, dp);
            md.__set__(p, gumby);
            // Should get the same object
            assertSame(dp, o.obj);
            assertSame(gumby, p.obj);
            // __set__ works after delete
            md.__delete__(o);
            assertNull(o.obj);
            final Object palin = "Palin";
            md.__set__(o, palin);
            assertSame(palin, o.obj);
        }

        @Override
        @Test
        void abstract_setAttr_works() throws Throwable {
            final Object dp = "D.P.", gumby = newPyUnicode("Gumby");
            Abstract.setAttr(o, name, dp);
            Abstract.setAttr(p, name, gumby);
            // Should get the same object
            assertSame(dp, o.obj);
            assertSame(gumby, p.obj);
            // setAttr works after delete
            Abstract.delAttr(o, name);
            assertNull(o.obj);
            final Object palin = "Palin";
            Abstract.setAttr(o, name, palin);
            assertSame(palin, o.obj);
        }

        @Override
        @Test
        void set_detects_TypeError() throws Throwable {
            // Everything is a Python object (no TypeError)
            final float[] everything = {1, 2, 3};
            assertDoesNotThrow(
                    () -> { md.__set__(o, everything); Abstract.setAttr(p, name, System.err); });
            assertSame(everything, o.obj);
            assertSame(System.err, p.obj);
        }
    }

    @Nested
    @DisplayName("implemented as a PyTuple")
    class TestTuple extends BaseSettableReference {

        PyTuple oRef, pRef;

        @BeforeEach
        void setup() throws AttributeError, Throwable {
            setup("tup", 42, -1);
            oRef = new PyTuple(42, 42.0, "42");
            pRef = new PyTuple(-1, -1.0, "-1");
        }

        @Override
        @Test
        void descr_get_works() {
            assertEquals(oRef, md.__get__(o, null));
            assertEquals(pRef, md.__get__(p, null));
        }

        @Override
        @Test
        void abstract_getAttr_works() throws Throwable {
            assertEquals(oRef, Abstract.getAttr(o, name));
            assertEquals(pRef, Abstract.getAttr(p, name));
        }

        @Override
        @Test
        void descr_set_works() throws Throwable {
            final Object tup2 = new PyTuple(2, 3, 4);
            md.__set__(o, tup2);
            assertEquals(tup2, o.tup);
            // __set__ works after delete
            md.__delete__(o);
            assertNull(o.tup);
            final Object tup3 = new PyTuple(3, 4, 5);
            md.__set__(o, tup3);
            assertEquals(tup3, o.tup);
        }

        @Override
        @Test
        void abstract_setAttr_works() throws Throwable {
            final Object gumby = PyTuple.from(List.of("D", "P", "Gumby"));
            Abstract.setAttr(o, name, gumby);
            // Should get the same object
            assertSame(gumby, o.tup);
            // setAttr works after delete
            Abstract.delAttr(o, name);
            assertNull(o.tup);
            final Object empty = PyTuple.EMPTY;
            Abstract.setAttr(o, name, empty);
            assertSame(empty, o.tup);
        }

        @Override
        @Test
        void set_detects_TypeError() throws Throwable {
            // Things that are not a Python tuple
            assertThrows(TypeError.class, () -> md.__set__(o, 1));
            assertThrows(TypeError.class, () -> Abstract.setAttr(p, name, ""));
            assertThrows(TypeError.class, () -> md.__set__(o, new Object()));
        }
    }

    /**
     * Base test of read-only attribute tests.
     */
    abstract static class BaseReadonly extends Base {

        /**
         * Raises {@link AttributeError} when the member descriptor is asked
         * to set the field in an instance of the object, even if the type
         * is correct.
         */
        @Test
        void rejects_descr_set() {
            assertThrows(AttributeError.class, () -> md.__set__(o, 1234));
            assertThrows(AttributeError.class, () -> md.__set__(p, 1.0));
            assertThrows(AttributeError.class, () -> md.__set__(o, "Gumby"));
            assertThrows(AttributeError.class, () -> md.__set__(p, Py.None));
        }

        /**
         * Raises {@link AttributeError} when
         * {@link Abstract#setAttr(Object, String, Object)} tries to set the
         * field in an instance of the object, even if the type is correct.
         */
        @Test
        void rejects_abstract_setAttr() {
            assertThrows(AttributeError.class, () -> Abstract.setAttr(o, name, 1234));
            assertThrows(AttributeError.class, () -> Abstract.setAttr(p, name, 1.0));
            assertThrows(AttributeError.class, () -> Abstract.setAttr(o, name, "Gumby"));
            assertThrows(AttributeError.class, () -> Abstract.setAttr(p, name, Py.None));
        }

        /**
         * Raises {@link AttributeError} when the member descriptor is asked
         * to delete the field in an instance of the object.
         */
        @Test
        void rejects_descr_delete() {
            assertThrows(AttributeError.class, () -> md.__delete__(o));
            assertThrows(AttributeError.class, () -> md.__set__(o, null));
        }

        /**
         * Raises {@link AttributeError} when
         * {@link Abstract#delAttr(Object, String)} tries to delete the
         * field from an instance of the object.
         */
        @Test
        void rejects_abstract_delAttr() {
            assertThrows(AttributeError.class, () -> Abstract.delAttr(o, name));
        }
    }

    @Nested
    @DisplayName("implemented as a read-only int")
    class TestIntRO extends BaseReadonly {

        @BeforeEach
        void setup() throws AttributeError, Throwable { setup("i2", 42, -1); }

        @Override
        @Test
        void descr_get_works() {
            assertEquals(42, md.__get__(o, null));
            assertEquals(-1, md.__get__(p, null));
        }

        @Override
        @Test
        void abstract_getAttr_works() throws Throwable {
            assertEquals(42, Abstract.getAttr(o, name));
            assertEquals(-1, Abstract.getAttr(p, name));
        }
    }

    @Nested
    @DisplayName("implemented as a final double")
    class TestDoubleRO extends BaseReadonly {

        @BeforeEach
        void setup() throws AttributeError, Throwable { setup("x2", 42, -1); }

        @Override
        @Test
        void descr_get_works() {
            assertEquals(42.0, md.__get__(o, null));
            assertEquals(-1.0, md.__get__(p, null));
        }

        @Override
        @Test
        void abstract_getAttr_works() throws Throwable {
            assertEquals(42.0, Abstract.getAttr(o, name));
            assertEquals(-1.0, Abstract.getAttr(p, name));
        }
    }

    @Nested
    @DisplayName("implemented as a read-only String")
    class TestStringRO extends BaseReadonly {

        @BeforeEach
        void setup() throws AttributeError, Throwable { setup("text2", 42, -1); }

        @Override
        @Test
        void descr_get_works() {
            assertEquals("42", md.__get__(o, null));
            assertEquals("-1", md.__get__(p, null));
        }

        @Override
        @Test
        void abstract_getAttr_works() throws Throwable {
            assertEquals("42", Abstract.getAttr(o, name));
            assertEquals("-1", Abstract.getAttr(p, name));
        }
    }

    @Nested
    @DisplayName("implemented as a PyUnicode (read-only)")
    class TestPyUnicodeRO extends BaseReadonly {

        @BeforeEach
        void setup() throws AttributeError, Throwable { setup("strhex", 42, -1); }

        @Override
        @Test
        void descr_get_works() {
            assertEquals(newPyUnicode("2a"), md.__get__(o, null));
            assertEquals(newPyUnicode("-1"), md.__get__(p, null));
        }

        @Override
        @Test
        void abstract_getAttr_works() throws Throwable {
            assertEquals(newPyUnicode("2a"), Abstract.getAttr(o, name));
            assertEquals(newPyUnicode("-1"), Abstract.getAttr(p, name));
        }
    }
}
