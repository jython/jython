package org.python.core;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Array;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.python.core.Exposed.Deleter;
import org.python.core.Exposed.DocString;
import org.python.core.Exposed.FrozenArray;
import org.python.core.Exposed.Getter;
import org.python.core.Exposed.Setter;

/**
 * Test that get-set attributes exposed by a Python <i>type</i>
 * defined in Java, that is, using methods annotated with
 * {@link Exposed.Getter}, {@link Exposed.Setter} and
 * {@link Exposed.Deleter}, result in data descriptors with
 * characteristics that correspond to the definitions.
 * <p>
 * This gets a bit complicated, but if it works, should cover
 * anything we want to do in real life. The test object is a Python
 * type {@code ObjectWithGetSets} defined by the inner Java class
 * {@link ObjectWithGetSets}. This definition adopts a second Java
 * class {@link AdoptedWithGetSets}, so that instances of either
 * Java type are accepted as Python objects of type
 * {@code ObjectWithGetSets}.
 * <p>
 * For simplicity, in the test both implementations get most of
 * their definition by inheritance from a common base class
 * {@link BaseGetSets}. Note that implementations of get-set methods
 * operating on the state of an {@code AdoptedWithGetSets}, have to
 * reside in the defining class {@code ObjectWithGetSets} or the
 * base.
 * <p>
 * There is a nested test suite for each pattern of characteristics.
 * For test purposes, we mostly mimic the behaviour of identified
 * types of member attribute.
 *
 * <table class="lined">
 * <caption>Patterns of get-set behaviour</caption>
 * <tr>
 * <th style="border-style: none;"></th>
 * <th>get</th>
 * <th>set</th>
 * <th>delete</th>
 * <th>get after delete</th>
 * </tr>
 * <tr>
 * <td class="row-label">readonly</td>
 * <td>yes</td>
 * <td>AttributeError</td>
 * <td>AttributeError</td>
 * <td>n/a</td>
 * </tr>
 * <tr>
 * <td class="row-label">settable</td>
 * <td>yes</td>
 * <td>yes</td>
 * <td>TypeError</td>
 * <td>n/a</td>
 * </tr>
 * <tr>
 * <td class="row-label">optional</td>
 * <td>yes</td>
 * <td>yes</td>
 * <td>removes</td>
 * <td>AttributeError</td>
 * </tr>
 * <tr>
 * <td class="row-label">not optional</td>
 * <td>yes</td>
 * <td>yes</td>
 * <td>sets default</td>
 * <td>gets default</td>
 * </tr>
 * </table>
 */
@DisplayName("For an attribute exposed by a type")
class TypeExposerGetSetTest extends UnitTestSupport {

    static final String[] GUMBYS = {"Prof L.R.Gumby", "Prof Enid Gumby", "D.P.Gumby"};
    static final String[] TWITS = {"Vivian Smith-Smythe-Smith", "Simon Zinc-Trumpet-Harris",
            "Nigel Incubator-Jones", "Gervaise Brook-Hampster", "Oliver St. John-Mollusc"};

    /**
     * Java base class of a Python type definition. We use this class to
     * prepare two classes that jointly define the get-set attributes of
     * a type {@code ObjectWithGetSets}.
     * <p>
     * As well as giving us less to type, using a base allows us to show
     * that some of the get-set attribute definitions explored in the
     * tests can be Java-inherited.
     */
    private static abstract class BaseGetSets {

        /** The actual Python type */
        protected PyType type;

        /** Primitive double attribute (not optional). */
        double x;

        @DocString("My test x")
        @Getter
        Object x() { return x; }

        @Setter
        void x(Object v) throws TypeError, Throwable { x = PyFloat.asDouble(v); }

        @Deleter("x")
        void _x() { x = Double.NaN; }

        /**
         * Optional {@code String} attribute that can be properly deleted
         * without popping up as {@code None}.
         */
        String s;

        @Getter
        Object s() { return errorIfNull(s, (o) -> Abstract.noAttributeError(this, "s")); }

        @Setter
        void s(Object v) { s = PyUnicode.asString(v); }

        @Deleter("s")
        void _s() {
            errorIfNull(s, (o) -> Abstract.noAttributeError(this, "s"));
            s = null;
        }

        /**
         * String with change of name. Deletion leads to a distinctive
         * value.
         */
        String t;

        @Getter("text")
        Object t() { return t; }

        @Setter("text")
        void t(Object v) { t = PyUnicode.asString(v); }

        @Deleter("text")
        void _t() { t = "<deleted>"; }

        /**
         * Read-only double attribute. {@code DocString} after
         * {@code Getter}
         */
        final double x2;

        @Getter
        @DocString("Another x")
        Object x2() { return x2; }

        /** Read-only {@code String} attribute. */
        String t2;

        @Getter("text2")
        Object t2() { return t2; }

        /**
         * Strongly-typed primitive ({@code double}) array internally, but
         * {@code tuple} to Python.
         */
        @FrozenArray
        double[] doubleArray;

        @Getter
        Object doubles() {
            PyTuple.Builder tb = new PyTuple.Builder(doubleArray.length);
            for (double d : doubleArray) { tb.append(d); }
            return tb.take();
        }

        @Setter
        void doubles(Object v) throws Throwable { doubleArray = doubleFromTuple(v); }

        @Deleter("doubles")
        void _doubles() { doubleArray = new double[0]; }

        /**
         * Strongly-typed {@code String} array internally, but {@code tuple}
         * to Python or {@code None} when deleted.
         */
        @FrozenArray
        String[] nameArray;

        @Getter
        Object names() { return new PyTuple(nameArray); }

        @Setter
        void names(Object v) { nameArray = fromTuple(v, String[].class); }

        @Deleter("names")
        void _names() { nameArray = new String[0]; }

        /**
         * Create new array value for {@link #nameArray}.
         *
         * @param v new value
         */
        void setNameArray(String[] v) { nameArray = Arrays.copyOf(v, v.length); }

        /**
         * Create new array value for {@link #doubleArray}.
         *
         * @param v new value
         */
        void setDoubleArray(double[] v) { doubleArray = Arrays.copyOf(v, v.length); }

        /**
         * {@code Object} get-set attribute, acting as a non-optional
         * member. That is {@code null} represents deleted and appears as
         * {@code None} externally.
         */
        Object obj;

        @Getter
        Object obj() { return defaultIfNull(obj, Py.None); }

        @Setter
        void obj(Object v) { obj = v; }

        @Deleter("obj")
        void _obj() { obj = null; }

        /*
         * Attribute with tuple value will be implemented at Java level
         * through this abstract interface.
         */
        /** @return notional field {@code tup}. */
        abstract PyTuple getTup();

        /**
         * Strongly-typed {@code PyTuple} attribute with default value
         * {@code None}. The attribute is defined through a pair of abstract
         * methods {@link #getTup()} and {@link #setTup(PyTuple)}. This
         * allows us to have quite different implementations in the two
         * subclasses while defining the get-set methods in the base.
         */
        @Getter
        Object tup() { return defaultIfNull(getTup(), Py.None); }

        /*
         * Notice the strongly typed argument to the Setter. This makes
         * checks in the method body unnecessary. PyGetSetDescr.__set__ will
         * check the supplied value and report a mismatch in terms of Python
         * types.
         *
         */
        @Setter
        void tup(PyTuple v) { setTup(v); }

        @Deleter("tup")
        void _tup() { setTup(null); }

        /**
         * Assign or delete notional field {@code tup}.
         *
         * @param tup new value ({@code null} for delete)
         */
        abstract void setTup(PyTuple tup);

        BaseGetSets(PyType type, double value) {
            this.type = type;
            x2 = x = value;
            doubleArray = new double[] {1, x, x * x, x * x * x};
            nameArray = TWITS.clone();
        }

        /**
         * Return a default value if {@code v} is {@code null}.
         *
         * @param <T> type of {@code v}
         * @param v to return if not {@code null}
         * @param defaultValue to return if {@code v} is {@code null}
         * @return {@code v} or {@code defaultValue}
         */
        static <T> T defaultIfNull(T v, T defaultValue) { return v != null ? v : defaultValue; }

        /**
         * Throw an exception if {@code v} is {@code null}.
         *
         * @param <T> type of {@code v}
         * @param <E> type of exception to throw
         * @param v to return if not {@code null}
         * @param exc supplier of exception to throw
         * @return {@code v}
         * @throws E if {@code v} is {@code null}
         */
        static <T, E extends PyException> T errorIfNull(T v, Function<T, E> exc) throws E {
            if (v != null) { return v; }
            throw exc.apply(v);
        }
    }

    /**
     * A Python type definition that exhibits a range of get-set
     * attribute definitions explored in the tests.
     */
    private static class ObjectWithGetSets extends BaseGetSets {

        static PyType TYPE =
                PyType.fromSpec(new PyType.Spec("ObjectWithGetSets", MethodHandles.lookup())
                        .adopt(AdoptedWithGetSets.class));

        /** Primitive integer attribute (not optional). */
        int i;

        @Getter
        Integer i() { return i; }

        @Getter
        static BigInteger i(AdoptedWithGetSets self) { return self.i; }

        @Setter
        void i(Object v) { i = PyLong.asInt(v); }

        @Setter
        static void i(AdoptedWithGetSets self, Object v) { self.i = PyLong.asBigInteger(v); }

        /** Read-only access. */
        int i2;

        @Getter
        Object i2() { return i2; }

        @Getter
        static BigInteger i2(AdoptedWithGetSets self) { return self.i2; }

        /**
         * Strongly-typed {@code PyTuple} attribute with default value
         * {@code None}.
         */
        PyTuple tup;

        ObjectWithGetSets(PyType type, double value) {
            super(type, value);
            i2 = i = BigInteger.valueOf(Math.round(value)).intValueExact();
            obj = i;
            t2 = t = s = String.format("%d", i);
            tup = new PyTuple(i, x, t);
        }

        ObjectWithGetSets(double value) { this(TYPE, value); }

        @Override
        PyTuple getTup() { return tup; }

        @Override
        void setTup(PyTuple tup) { this.tup = tup; }
    }

    /**
     * A class that represents an <i>adopted</i> implementation of the
     * Python class {@code ObjectWithGetSets} defined above. Attribute
     * access methods implemented in the common base class
     * {@link BaseGetSets} also apply to this class. This class and the
     * canonical class {@link ObjectWithGetSets} implement certain
     * attributes each in their own way. The attribute access methods
     * for this class are implemented as {@code static} methods in the
     * canonical {@code ObjectWithGetSets}.
     */
    private static class AdoptedWithGetSets extends BaseGetSets {
        // FIXME not used in tests

        /** Primitive integer attribute (not optional). */
        BigInteger i;

        /** Read-only access. */
        BigInteger i2;

        /**
         * Strongly-typed {@code PyTuple} attribute with default value
         * {@code None}.
         */
        Object[] aTuple;

        AdoptedWithGetSets(double value) {
            super(ObjectWithGetSets.TYPE, value);
            i2 = i = BigInteger.valueOf(Math.round(value));
            obj = i;
            t2 = t = s = String.format("%d", i);
            aTuple = new Object[] {i, x, t};
        }

        @Override
        PyTuple getTup() { return aTuple == null ? null : PyTuple.from(aTuple); }

        @Override
        void setTup(PyTuple tup) { this.aTuple = tup == null ? null : tup.toArray(); }
    }

    /**
     * Copy {@code tuple} elements to a new {@code T[]}, raising a
     * {@link TypeError} if any element cannot be assigned to variable
     * of type {@code T}.
     */
    private static <T> T[] fromTuple(Object tuple, Class<? extends T[]> arrayType)
            throws TypeError {
        // Loosely based on java.util.Arrays.copyOf
        if (tuple instanceof PyTuple) {
            PyTuple t = (PyTuple)tuple;
            int n = t.size();
            @SuppressWarnings("unchecked")
            T[] copy = (T[])Array.newInstance(arrayType.getComponentType(), n);
            try {
                System.arraycopy(t.value, 0, copy, 0, n);
            } catch (ArrayStoreException ase) {
                PyType dstType = PyType.of(arrayType.getComponentType());
                throw new TypeError("tuple of %s expected", dstType);
            }
            return copy;
        } else {
            throw new TypeError("tuple expected");
        }
    }

    /**
     * Copy tuple elements to a new {@code double[]}, converting them
     * with {@link PyFloat#doubleValue(Object)}.
     *
     * @throws Throwable
     */
    private static double[] doubleFromTuple(Object tuple) throws Throwable {
        if (tuple instanceof PyTuple) {
            PyTuple t = (PyTuple)tuple;
            int n = t.size();
            Object[] value = t.value;
            double[] copy = new double[n];
            for (int i = 0; i < n; i++) { copy[i] = PyFloat.asDouble(value[i]); }
            return copy;
        } else {
            throw new TypeError("tuple expected");
        }
    }

    /**
     * Create a {@code tuple} in which the elements are equal to the
     * values in a given array.
     *
     * @param a values
     * @return tuple of those values
     */
    private static PyTuple tupleFrom(double[] a) {
        int n = a.length;
        Double[] oa = new Double[n];
        for (int i = 0; i < n; i++) { oa[i] = a[i]; }
        return new PyTuple(oa);
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
        /** The value set by delete. */
        Object deleted;
        /** Unbound descriptor by type access to examine or call. */
        PyGetSetDescr gsd;
        /** The object on which to attempt access. */
        ObjectWithGetSets o;
        /**
         * An object of the adopted implementation on which to attempt the
         * same kind of access (in case we are getting instances mixed up).
         */
        AdoptedWithGetSets p;

        void setup(String name, String doc, Object deleted, double oValue, double pValue)
                throws Throwable {
            this.name = name;
            this.doc = doc;
            this.deleted = deleted;
            try {
                this.gsd = (PyGetSetDescr)ObjectWithGetSets.TYPE.lookup(name);
                this.o = new ObjectWithGetSets(oValue);
                this.p = new AdoptedWithGetSets(pValue);
            } catch (ExceptionInInitializerError eie) {
                // Errors detected by the Exposer get wrapped so:
                Throwable t = eie.getCause();
                throw t == null ? eie : t;
            }
        }

        void setup(String name, String doc, double oValue, double pValue) throws Throwable {
            setup(name, doc, null, oValue, pValue);
        }

        void setup(String name, double oValue, double pValue) throws Throwable {
            setup(name, null, null, oValue, pValue);
        }

        /**
         * The attribute is a get-set descriptor that correctly reflects the
         * annotations in the defining class.
         *
         * @throws Throwable unexpectedly
         */
        @Test
        void descr_has_expected_fields() throws Throwable {
            assertEquals(name, gsd.name);
            assertEquals(doc, gsd.doc);
            String s = String.format("<attribute '%s' of 'ObjectWithGetSets' objects>", name);
            assertEquals(s, gsd.toString());
            assertEquals(s, Abstract.repr(gsd));
        }

        /**
         * The string (repr) describes the type and attribute.
         *
         * @throws Throwable unexpectedly
         */
        void checkToString() throws Throwable {
            String s = String.format("<attribute '%s' of 'ObjectWithGetSets' objects>", name);
            assertEquals(s, gsd.toString());
            assertEquals(s, Abstract.repr(gsd));
        }

        /**
         * The get-set descriptor may be used to read the field in an
         * instance of the object.
         *
         * @throws Throwable unexpectedly
         */
        abstract void descr_get_works() throws Throwable;

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
         * The get-set descriptor may be used to set the field in an
         * instance of the object.
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
         * The get-set attribute raises {@link TypeError} when supplied a
         * value of unacceptable type.
         */
        abstract void set_detects_TypeError();
    }

    /**
     * Base test of settable attribute that may not be deleted.
     */
    abstract static class BaseSettableIndelible extends BaseSettable {

        /**
         * Attempting to delete the get-set attribute, where it has a setter
         * but no deleter, from an instance of the object, through the
         * get-set descriptor, raises {@link TypeError}.
         */
        @Test
        void rejects_descr_delete() {
            assertThrows(TypeError.class, () -> gsd.__delete__(o));
            assertThrows(TypeError.class, () -> gsd.__set__(o, null));
            assertThrows(TypeError.class, () -> gsd.__delete__(p));
            assertThrows(TypeError.class, () -> gsd.__set__(p, null));
        }

        /**
         * Attempting to delete the get-set attribute, where it has a setter
         * but no deleter, from an instance of the object, through
         * {@link Abstract#delAttr(Object, String)}, raises
         * {@link TypeError}.
         */
        @Test
        void rejects_abstract_delAttr() {
            assertThrows(TypeError.class, () -> Abstract.delAttr(o, name));
            assertThrows(TypeError.class, () -> Abstract.setAttr(o, name, null));
            assertThrows(TypeError.class, () -> Abstract.delAttr(p, name));
            assertThrows(TypeError.class, () -> Abstract.setAttr(p, name, null));
        }
    }

    /**
     * Base test of an optional attribute. Instances will raise
     * {@link AttributeError} on access after deletion.
     */
    abstract static class BaseOptionalReference extends BaseSettable {

        /**
         * The get-set descriptor may be used to delete a field from an
         * instance of the object, causing it to disappear externally.
         *
         * @throws Throwable unexpectedly
         */
        @Test
        void descr_delete_removes() throws Throwable {
            gsd.__delete__(o);
            gsd.__delete__(p);
            // After deletion, ...
            // ... __get__ raises AttributeError
            assertThrows(AttributeError.class, () -> gsd.__get__(o, null));
            assertThrows(AttributeError.class, () -> gsd.__get__(p, null));
            // ... __delete__ raises AttributeError
            assertThrows(AttributeError.class, () -> gsd.__delete__(o));
            assertThrows(AttributeError.class, () -> gsd.__delete__(p));
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
            Abstract.delAttr(p, name);
            // After deletion, ...
            // ... getAttr and delAttr raise AttributeError
            assertThrows(AttributeError.class, () -> Abstract.getAttr(o, name));
            assertThrows(AttributeError.class, () -> Abstract.getAttr(p, name));
            assertThrows(AttributeError.class, () -> Abstract.delAttr(o, name));
            assertThrows(AttributeError.class, () -> Abstract.delAttr(p, name));
        }
    }

    /**
     * Base test of settable attribute where deletion sets a particular
     * value.
     */
    abstract static class BaseSettableDefault extends BaseSettable {

        /**
         * The get-set descriptor may be used to delete a field from an
         * instance of the object, meaning whatever the {@code Deleter}
         * chooses. For test purposes, we set a distinctive {@code deleted}
         * value.
         *
         * @throws Throwable unexpectedly
         */
        @Test
        void descr_delete_sets_deleted() throws Throwable {
            gsd.__delete__(o);
            assertEquals(deleted, gsd.__get__(o, null));
            // __delete__ is idempotent
            gsd.__delete__(o);
            assertEquals(deleted, gsd.__get__(o, null));
            // And again for the adopted implementation
            gsd.__delete__(p);
            assertEquals(deleted, gsd.__get__(p, null));
            gsd.__delete__(p);
            assertEquals(deleted, gsd.__get__(p, null));
        }

        /**
         * {@link Abstract#delAttr(Object, String)} to delete a field from
         * an instance of the object, meaning whatever the {@code deleter}
         * chooses. For test purposes, we mimic the behaviour of an optional
         * member: ({@code null} internally appears as {@code None}
         * externally.
         *
         * @throws Throwable unexpectedly
         */
        @Test
        void abstract_delAttr_sets_deleted() throws Throwable {
            Abstract.delAttr(o, name);
            assertEquals(deleted, Abstract.getAttr(o, name));
            // delAttr is idempotent
            Abstract.delAttr(o, name);
            assertEquals(deleted, Abstract.getAttr(o, name));
            // And again for the adopted implementation
            Abstract.delAttr(p, name);
            assertEquals(deleted, Abstract.getAttr(p, name));
            Abstract.delAttr(p, name);
            assertEquals(deleted, Abstract.getAttr(p, name));
        }
    }

    @Nested
    @DisplayName("implemented as an int")
    class TestInt extends BaseSettableIndelible {

        @BeforeEach
        void setup() throws AttributeError, Throwable { setup("i", 42, -1); }

        @Override
        @Test
        void descr_get_works() throws Throwable {
            assertEquals(42, gsd.__get__(o, null));
            assertPythonEquals(-1, gsd.__get__(p, null));
        }

        @Override
        @Test
        void abstract_getAttr_works() throws Throwable {
            assertEquals(42, Abstract.getAttr(o, name));
            assertPythonEquals(-1, Abstract.getAttr(p, name));
        }

        @Override
        @Test
        void descr_set_works() throws Throwable {
            gsd.__set__(o, 43);
            gsd.__set__(p, BigInteger.valueOf(44));
            assertEquals(43, o.i);
            assertPythonEquals(44, p.i);
        }

        @Override
        @Test
        void abstract_setAttr_works() throws Throwable {
            Abstract.setAttr(o, name, 43);
            Abstract.setAttr(p, name, BigInteger.valueOf(44));
            assertEquals(43, o.i);
            assertPythonEquals(44, p.i);
        }

        @Override
        @Test
        void set_detects_TypeError() {
            // Things that are not a Python int
            assertThrows(TypeError.class, () -> gsd.__set__(o, "Gumby"));
            assertThrows(TypeError.class, () -> Abstract.setAttr(p, name, 1.0));
            assertThrows(TypeError.class, () -> gsd.__set__(o, Py.None));
            assertThrows(TypeError.class, () -> gsd.__set__(p, Py.None));
        }
    }

    @Nested
    @DisplayName("implemented as a double")
    class TestDouble extends BaseSettable {

        @BeforeEach
        void setup() throws AttributeError, Throwable { setup("x", "My test x", 42, -1); }

        @Override
        @Test
        void descr_get_works() throws Throwable {
            assertEquals(42.0, gsd.__get__(o, null));
            assertEquals(-1.0, gsd.__get__(p, null));
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
            gsd.__set__(o, 1.125);
            gsd.__set__(p, BigInteger.valueOf(111_222_333_444L));
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
        void set_detects_TypeError() {
            // Things that are not a Python float
            assertThrows(TypeError.class, () -> gsd.__set__(o, "Gumby"));
            assertThrows(TypeError.class, () -> Abstract.setAttr(p, name, "42"));
            assertThrows(TypeError.class, () -> gsd.__set__(o, Py.None));
            assertThrows(TypeError.class, () -> gsd.__set__(p, Py.None));
        }

        /**
         * The get-set descriptor may be used to delete a field from an
         * instance of the object, meaning in this case, set it to
         * {@code NaN}.
         *
         * @throws Throwable unexpectedly
         */
        @Test
        void descr_delete_sets_NaN() throws Throwable {
            gsd.__delete__(o);
            assertEquals(Double.NaN, gsd.__get__(o, null));
            // __delete__ is idempotent
            gsd.__delete__(o);
            assertEquals(Double.NaN, gsd.__get__(o, null));

            // And again for the adopted implementation
            gsd.__delete__(p);
            assertEquals(Double.NaN, gsd.__get__(p, null));
            gsd.__delete__(p);
            assertEquals(Double.NaN, gsd.__get__(p, null));
        }

        /**
         * {@link Abstract#delAttr(Object, String)} to delete a field from
         * an instance of the object, meaning in this case, set it to
         * {@code NaN}.
         *
         * @throws Throwable unexpectedly
         */
        @Test
        void abstract_delAttr_sets_NaN() throws Throwable {
            Abstract.delAttr(o, name);
            assertEquals(Double.NaN, Abstract.getAttr(o, name));
            // delAttr is idempotent
            Abstract.delAttr(o, name);
            assertEquals(Double.NaN, Abstract.getAttr(o, name));
            // And again for the adopted implementation
            Abstract.delAttr(p, name);
            assertEquals(Double.NaN, Abstract.getAttr(p, name));
            Abstract.delAttr(p, name);
            assertEquals(Double.NaN, Abstract.getAttr(p, name));
        }
    }

    @Nested
    @DisplayName("implemented as a String")
    class TestString extends BaseSettableDefault {

        @BeforeEach
        void setup() throws AttributeError, Throwable { setup("text", null, "<deleted>", 42, -1); }

        @Override
        @Test
        void descr_get_works() throws Throwable {
            assertEquals("42", gsd.__get__(o, null));
            assertEquals("-1", gsd.__get__(p, null));
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
            gsd.__set__(o, "D.P.");
            gsd.__set__(p, newPyUnicode("Gumby"));
            assertEquals("D.P.", o.t);
            assertEquals("Gumby", p.t);
            // __set__ works after delete
            gsd.__delete__(o);
            assertEquals(deleted, o.t);
            gsd.__set__(o, "Palin");
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
            assertEquals(deleted, o.t);
            Abstract.setAttr(o, name, "Palin");
            assertEquals("Palin", o.t);
            // And again for the adopted implementation
            Abstract.delAttr(p, name);
            assertEquals(deleted, p.t);
            Abstract.setAttr(p, name, "Palin");
            assertEquals("Palin", p.t);
        }

        @Override
        @Test
        void set_detects_TypeError() {
            // Things that are not a Python str
            assertThrows(TypeError.class, () -> gsd.__set__(o, 1));
            assertThrows(TypeError.class, () -> Abstract.setAttr(p, name, 10.0));
            assertThrows(TypeError.class, () -> gsd.__set__(o, new Object()));
            assertThrows(TypeError.class, () -> gsd.__set__(p, new Object()));
        }
    }

    @Nested
    @DisplayName("implemented as an optional String")
    class TestOptionalString extends BaseOptionalReference {

        @BeforeEach
        void setup() throws AttributeError, Throwable { setup("s", 42, -1); }

        @Override
        @Test
        void descr_get_works() throws Throwable {
            assertEquals("42", gsd.__get__(o, null));
            assertEquals("-1", gsd.__get__(p, null));
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
            gsd.__set__(o, "D.P.");
            gsd.__set__(p, "Gumby");
            assertEquals("D.P.", o.s);
            assertEquals("Gumby", p.s);
            // __set__ works after delete
            gsd.__delete__(o);
            assertNull(o.s);
            gsd.__set__(o, "Palin");
            assertEquals("Palin", o.s);
            // And again for the adopted implementation
            gsd.__delete__(p);
            assertNull(p.s);
            gsd.__set__(p, "Palin");
            assertEquals("Palin", p.s);
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
            // And again for the adopted implementation
            Abstract.delAttr(p, name);
            assertNull(p.s);
            Abstract.setAttr(p, name, "Palin");
            assertEquals("Palin", p.s);
        }

        @Override
        @Test
        void set_detects_TypeError() {
            // Things that are not a Python str
            assertThrows(TypeError.class, () -> gsd.__set__(o, 1));
            assertThrows(TypeError.class, () -> Abstract.setAttr(p, name, 10.0));
            assertThrows(TypeError.class, () -> gsd.__set__(o, new Object()));
        }
    }

    @Nested
    @DisplayName("implemented as an Object")
    class TestObject extends BaseSettableDefault {

        @BeforeEach
        void setup() throws AttributeError, Throwable { setup("obj", null, Py.None, 42, -1); }

        @Override
        @Test
        void descr_get_works() throws Throwable {
            assertEquals(42, gsd.__get__(o, null));
            assertPythonEquals(-1, gsd.__get__(p, null));
        }

        @Override
        @Test
        void abstract_getAttr_works() throws Throwable {
            assertEquals(42, Abstract.getAttr(o, name));
            assertPythonEquals(-1, Abstract.getAttr(p, name));
        }

        @Override
        @Test
        void descr_set_works() throws Throwable {
            final Object dp = "D.P.", gumby = newPyUnicode("Gumby");
            gsd.__set__(o, dp);
            gsd.__set__(p, gumby);
            // Should get the same object
            assertSame(dp, o.obj);
            assertSame(gumby, p.obj);
            // __set__ works after delete
            gsd.__delete__(o);
            assertNull(o.obj);
            final Object nonPython = new HashMap<String, Integer>();
            gsd.__set__(o, nonPython);
            assertSame(nonPython, o.obj);
            // And again for the adopted implementation
            gsd.__delete__(p);
            assertNull(p.obj);
            gsd.__set__(p, nonPython);
            assertSame(nonPython, p.obj);
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
        void set_detects_TypeError() {
            // Everything is a Python object (no TypeError)
            final float[] everything = {1, 2, 3};
            assertDoesNotThrow(
                    () -> { gsd.__set__(o, everything); Abstract.setAttr(p, name, System.err); });
            assertSame(everything, o.obj);
            assertSame(System.err, p.obj);
        }
    }

    @Nested
    @DisplayName("implemented as a PyTuple")
    class TestTuple extends BaseSettableDefault {

        PyTuple oRef, pRef;

        @BeforeEach
        void setup() throws AttributeError, Throwable {
            setup("tup", null, Py.None, 42, -1);
            oRef = new PyTuple(42, 42.0, "42");
            pRef = new PyTuple(-1, -1.0, "-1");
        }

        @Override
        @Test
        void descr_get_works() throws Throwable {
            assertEquals(oRef, gsd.__get__(o, null));
            assertEquals(pRef, gsd.__get__(p, null));
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
            gsd.__set__(o, tup2);
            assertEquals(tup2, o.tup);
            // __set__ works after delete
            final Object[] tup3array = new Object[] {3, 4, 5};
            final Object tup3 = PyTuple.from(tup3array);
            gsd.__delete__(o);
            assertNull(o.tup);
            gsd.__set__(o, tup3);
            assertEquals(tup3, o.tup);
            // And again for the adopted implementation
            gsd.__delete__(p);
            assertNull(p.aTuple);
            gsd.__set__(p, tup3);
            assertArrayEquals(tup3array, p.aTuple);
        }

        @Override
        @Test
        void abstract_setAttr_works() throws Throwable {
            final Object gumby = PyTuple.from(List.of("D", "P", "Gumby"));
            Abstract.setAttr(o, name, gumby);
            // Should get the same object
            assertSame(gumby, o.tup);
            // setAttr works after delete
            final Object[] tup3array = new Object[] {3, 4, 5};
            final Object tup3 = PyTuple.from(tup3array);
            Abstract.delAttr(o, name);
            assertNull(o.tup);
            Abstract.setAttr(o, name, tup3);
            assertSame(tup3, o.tup);
            // And again for the adopted implementation
            Abstract.delAttr(p, name);
            assertNull(p.aTuple);
            Abstract.setAttr(p, name, tup3);
            assertArrayEquals(tup3array, p.aTuple);
        }

        @Override
        @Test
        void set_detects_TypeError() {
            // Things that are not a Python tuple
            assertThrows(TypeError.class, () -> gsd.__set__(o, 1));
            assertThrows(TypeError.class, () -> Abstract.setAttr(p, name, ""));
            assertThrows(TypeError.class, () -> gsd.__set__(o, new Object()));
            assertThrows(TypeError.class, () -> gsd.__set__(p, new Object()));
        }
    }

    @Nested
    @DisplayName("providing a double array (as a tuple)")
    class TestDoubleArray extends BaseSettable {

        PyTuple oval, pval, ival, rval;
        double[] ref;

        @BeforeEach
        void setup() throws AttributeError, Throwable {
            setup("doubles", 42, -1);
            oval = Py.tuple(1., 42., 1764., 74088.);
            pval = Py.tuple(1., -1., 1., -1.);
            ival = Py.tuple(3, 14, 15, 926);
            ref = new double[] {3.0, 14.0, 15.0, 926.0};
            rval = tupleFrom(ref);
        }

        @Override
        @Test
        void descr_get_works() throws Throwable {
            assertEquals(oval, gsd.__get__(o, null));
            p.setDoubleArray(ref);
            assertEquals(rval, gsd.__get__(p, null));
        }

        @Override
        @Test
        void abstract_getAttr_works() throws Throwable {
            assertEquals(oval, Abstract.getAttr(o, name));
            p.setDoubleArray(ref);
            assertEquals(rval, Abstract.getAttr(p, name));
        }

        @Override
        @Test
        void descr_set_works() throws Throwable {
            gsd.__set__(o, ival);
            gsd.__set__(p, pval);
            assertArrayEquals(ref, o.doubleArray);
            // __set__ works after delete
            gsd.__delete__(o);
            assertEquals(0, o.doubleArray.length);
            gsd.__set__(o, ival);
            assertArrayEquals(ref, o.doubleArray);
        }

        @Override
        void abstract_setAttr_works() throws Throwable {
            Abstract.setAttr(o, name, ival);
            assertArrayEquals(ref, o.doubleArray);
            // __set__ works after delete
            Abstract.delAttr(o, name);
            assertEquals(0, o.doubleArray.length);
            Abstract.setAttr(o, name, ival);
            assertArrayEquals(ref, o.doubleArray);
        }

        @Override
        @Test
        void set_detects_TypeError() {
            // Things that are not a Python tuple
            assertThrows(TypeError.class, () -> gsd.__set__(o, 2.0));
            assertThrows(TypeError.class, () -> Abstract.setAttr(p, name, Py.None));
            assertThrows(TypeError.class, () -> gsd.__set__(o, new double[] {1, 2, 3}));
        }
    }

    @Nested
    @DisplayName("providing a string array (as a tuple)")
    class TestStringArray extends BaseSettable {

        PyTuple twits, gumbys, rval, sval;

        @BeforeEach
        void setup() throws AttributeError, Throwable {
            setup("names", 42, -1);
            twits = PyTuple.from(TWITS);
            gumbys = PyTuple.from(GUMBYS);
        }

        @Override
        @Test
        void descr_get_works() throws Throwable {
            assertEquals(twits, gsd.__get__(o, null));
            p.setNameArray(GUMBYS);
            assertEquals(gumbys, gsd.__get__(p, null));
        }

        @Override
        @Test
        void abstract_getAttr_works() throws Throwable {
            assertEquals(twits, Abstract.getAttr(o, name));
            p.setNameArray(GUMBYS);
            assertEquals(gumbys, Abstract.getAttr(p, name));
        }

        @Override
        @Test
        void descr_set_works() throws Throwable {
            gsd.__set__(o, gumbys);
            assertArrayEquals(GUMBYS, o.nameArray);
            // __set__ works after delete
            gsd.__delete__(o);
            assertEquals(0, o.nameArray.length);
            gsd.__set__(o, twits);
            assertArrayEquals(TWITS, o.nameArray);
        }

        @Override
        void abstract_setAttr_works() throws Throwable {
            Abstract.setAttr(o, name, gumbys);
            assertArrayEquals(GUMBYS, o.nameArray);
            // __set__ works after delete
            Abstract.delAttr(o, name);
            assertEquals(0, o.nameArray.length);
            Abstract.setAttr(o, name, twits);
            assertArrayEquals(TWITS, o.nameArray);
        }

        @Override
        @Test
        void set_detects_TypeError() {
            // Things that are not a Python tuple
            assertThrows(TypeError.class, () -> gsd.__set__(o, ""));
            assertThrows(TypeError.class, () -> Abstract.setAttr(p, name, Py.None));
            assertThrows(TypeError.class, () -> gsd.__set__(o, new String[] {}));
        }
    }

    /**
     * Base test of read-only attribute tests.
     */
    abstract static class BaseReadonly extends Base {

        /**
         * Raises {@link AttributeError} when the get-set descriptor is
         * asked to set the field in an instance of the object, even if the
         * type is correct.
         */
        @Test
        void rejects_descr_set() {
            assertThrows(AttributeError.class, () -> gsd.__set__(o, 1234));
            assertThrows(AttributeError.class, () -> gsd.__set__(p, 1.0));
            assertThrows(AttributeError.class, () -> gsd.__set__(o, "Gumby"));
            assertThrows(AttributeError.class, () -> gsd.__set__(p, Py.None));
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
         * Attempting to delete a get-set attribute, where it has no setter
         * or deleter (is read-only), from an instance of the object,
         * through the get-set descriptor, raises {@link AttributeError}.
         */
        @Test
        void rejects_descr_delete() {
            assertThrows(AttributeError.class, () -> gsd.__delete__(o));
            assertThrows(AttributeError.class, () -> gsd.__set__(o, null));
        }

        /**
         * Attempting to delete a get-set attribute, where it has no setter
         * or deleter (is read-only), from an instance of the object,
         * through {@link Abstract#delAttr(Object, String)}, raises
         * {@link AttributeError}.
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
        void descr_get_works() throws Throwable {
            assertEquals(42, gsd.__get__(o, null));
            assertPythonEquals(-1, gsd.__get__(p, null));
        }

        @Override
        @Test
        void abstract_getAttr_works() throws Throwable {
            assertEquals(42, Abstract.getAttr(o, name));
            assertPythonEquals(-1, Abstract.getAttr(p, name));
        }
    }

    @Nested
    @DisplayName("implemented as a final double")
    class TestDoubleRO extends BaseReadonly {

        @BeforeEach
        void setup() throws AttributeError, Throwable { setup("x2", "Another x", 42, -1); }

        @Override
        @Test
        void descr_get_works() throws Throwable {
            assertEquals(42.0, gsd.__get__(o, null));
            assertEquals(-1.0, gsd.__get__(p, null));
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
        void descr_get_works() throws Throwable {
            assertEquals("42", gsd.__get__(o, null));
            assertEquals("-1", gsd.__get__(p, null));
        }

        @Override
        @Test
        void abstract_getAttr_works() throws Throwable {
            assertEquals("42", Abstract.getAttr(o, name));
            assertEquals("-1", Abstract.getAttr(p, name));
        }
    }
}
