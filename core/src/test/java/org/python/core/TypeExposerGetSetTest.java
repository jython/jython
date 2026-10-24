// Copyright (c)2026 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.core;

import static org.junit.jupiter.api.Assertions.*;

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

import org.python.internal._PyUtil;
import org.python.types.Exposed;
import org.python.types.Exposed.Deleter;
import org.python.types.Exposed.DocString;
import org.python.types.Exposed.Getter;
import org.python.types.Exposed.Setter;
import org.python.types.Feature;
import org.python.types.TypeSpec;
import org.python.types.WithClass;

/**
 * Test that get-set attributes exposed by a Python <i>type</i> defined
 * in Java, that is, using methods annotated with
 * {@link Exposed.Getter}, {@link Exposed.Setter} and
 * {@link Exposed.Deleter}, result in data descriptors with
 * characteristics that correspond to the definitions.
 * <p>
 * This gets a bit complicated, but if it works, should cover anything
 * we want to do in real life. The first test object is a Python type
 * {@code OGS} defined by the inner Java class {@link OWithGetSets}. The
 * second test object is a Python type {@code PGS} defined by the inner
 * Java class {@link PWithGetSets}, and adopting a second Java class
 * {@link QWithGetSets}. Instances of either Java type are accepted as
 * Python objects of type {@code PGS}.
 * <p>
 * For simplicity, in the test, all three implementations get most of
 * their definition by inheritance from a common base class
 * {@link BaseGetSets}. Note that implementations of get-set methods
 * operating on the state of a {@code QWithGetSets}, have to reside in
 * the defining class {@code PGS} or the common base.
 * <p>
 * There is a nested test suite for each pattern of characteristics. For
 * test purposes, we mostly mimic the behaviour of identified types of
 * member attribute.
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
     * prepare three classes: a Python object type {@code OGS}, and a pair
     * of classes that jointly define the get-set attributes of a type
     * {@code PGS}.
     * <p>
     * As well as giving us less to type, using a base allows us to show
     * that some of the get-set attribute definitions explored in the tests
     * can be Java-inherited.
     */
    private static abstract class BaseGetSets {

        /** Primitive double attribute (not optional). */
        double x;

        @DocString("My test x")
        @Getter
        Object x() { return x; }

        @Setter
        void x(Object v) throws PyBaseException, Throwable { x = PyFloat.asDouble(v); }

        @Deleter("x")
        void _x() { x = Double.NaN; }

        /**
         * Optional {@code String} attribute that can be properly deleted
         * without popping up as {@code None}.
         */
        String s;

        @Getter
        Object s() { return errorIfNull(s, (o) -> _PyUtil.noAttributeError(this, "s")); }

        @Setter
        void s(Object v) { s = PyUnicode.asString(v); }

        @Deleter("s")
        void _s() {
            errorIfNull(s, (o) -> _PyUtil.noAttributeError(this, "s"));
            s = null;
        }

        /**
         * String with change of name. Deletion leads to a distinctive value.
         */
        String t;

        @Getter("text")
        Object t() { return t; }

        @Setter("text")
        void t(Object v) { t = PyUnicode.asString(v); }

        @Deleter("text")
        void _t() { t = "<deleted>"; }

        /**
         * Read-only double attribute. {@code DocString} after {@code Getter}
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
         * Strongly-typed {@code String} array internally, but {@code tuple} to
         * Python or {@code None} when deleted.
         */
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
         * {@code Object} get-set attribute, acting as a non-optional member.
         * That is {@code null} represents deleted and appears as {@code None}
         * externally.
         */
        Object obj;

        @Getter
        Object obj() { return defaultIfNull(obj, Py.None); }

        @Setter
        void obj(Object v) { obj = v; }

        @Deleter("obj")
        void _obj() { obj = null; }

        /*
         * Attribute with tuple value will be implemented at Java level through
         * this abstract interface.
         */
        /** @return notional field {@code tup}. */
        abstract PyTuple getTup();

        /**
         * Strongly-typed {@code PyTuple} attribute with default value
         * {@code None}. The attribute is defined through a pair of abstract
         * methods {@link #getTup()} and {@link #setTup(PyTuple)}. This allows
         * us to have quite different implementations in the two subclasses
         * while defining the get-set methods in the base.
         */
        @Getter
        Object tup() { return defaultIfNull(getTup(), Py.None); }

        /*
         * Notice the strongly typed argument to the Setter. This makes checks
         * in the method body unnecessary. PyGetSetDescr.__set__ will check the
         * supplied value and report a mismatch in terms of Python types.
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

        BaseGetSets(double value) {
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
        static <T, E extends PyBaseException> T errorIfNull(T v, Function<T, E> exc) throws E {
            if (v != null) { return v; }
            throw exc.apply(v);
        }
    }

    // FIXME: Also test when there is only one self-class.

    /**
     * A Python type definition that exhibits a range of get-set attribute
     * definitions explored in the tests.
     */
    private static class OWithGetSets extends BaseGetSets implements WithClass {

        static PyType TYPE = PyType.fromSpec(new TypeSpec("OGS", MethodHandles.lookup()));

        /** Primitive integer attribute (not optional). */
        int i;

        @Getter
        Integer i() { return i; }

        @Setter
        void i(Object v) { i = PyLong.asInt(v); }

        /** Read-only access. */
        int i2;

        @Getter
        Object i2() { return i2; }

        /**
         * Strongly-typed {@code PyTuple} attribute with default value
         * {@code None}.
         */
        PyTuple tup;

        OWithGetSets(double value) {
            super(value);
            i2 = i = BigInteger.valueOf(Math.round(value)).intValueExact();
            obj = i;
            t2 = t = s = String.format("%d", i);
            tup = PyTuple.of(i, x, t);
        }

        @Override
        public PyType getType() { return TYPE; }

        @Override
        PyTuple getTup() { return tup; }

        @Override
        void setTup(PyTuple tup) { this.tup = tup; }
    }

    /**
     * A Python type definition that adopts another class and exhibits a
     * range of get-set attribute definitions explored in the tests.
     */
    private static class PWithGetSets extends BaseGetSets implements WithClass {

        static PyType TYPE = PyType.fromSpec(new TypeSpec("PGS", MethodHandles.lookup()) //
                .add(Feature.IMMUTABLE).adopt(QWithGetSets.class));

        /** Primitive integer attribute (not optional). */
        int i;

        @Getter
        Integer i() { return i; }

        @Getter
        static BigInteger i(QWithGetSets self) { return self.i; }

        @Setter
        void i(Object v) { i = PyLong.asInt(v); }

        @Setter
        static void i(QWithGetSets self, Object v) { self.i = PyLong.asBigInteger(v); }

        /** Read-only access. */
        int i2;

        @Getter
        Object i2() { return i2; }

        @Getter
        static BigInteger i2(QWithGetSets self) { return self.i2; }

        /**
         * Strongly-typed {@code PyTuple} attribute with default value
         * {@code None}.
         */
        PyTuple tup;

        PWithGetSets(double value) {
            super(value);
            i2 = i = BigInteger.valueOf(Math.round(value)).intValueExact();
            obj = i;
            t2 = t = s = String.format("%d", i);
            tup = PyTuple.of(i, x, t);
        }

        @Override
        public PyType getType() { return TYPE; }

        @Override
        PyTuple getTup() { return tup; }

        @Override
        void setTup(PyTuple tup) { this.tup = tup; }
    }

    /**
     * A class that represents an <i>adopted</i> implementation of the
     * Python class {@code PGS} defined above. Attribute access methods
     * implemented in the common base class {@link BaseGetSets} also apply
     * to this class. This class and the canonical class
     * {@link PWithGetSets} implement certain attributes each in their own
     * way. The attribute access methods for this class are implemented as
     * {@code static} methods in the canonical {@code PWithGetSets}.
     */
    private static class QWithGetSets extends BaseGetSets {

        /** Primitive integer attribute (not optional). */
        BigInteger i;

        /** Read-only access. */
        BigInteger i2;

        /**
         * Strongly-typed {@code PyTuple} attribute with default value
         * {@code None}.
         */
        Object[] aTuple;

        QWithGetSets(double value) {
            super(value);
            i2 = i = BigInteger.valueOf(Math.round(value));
            obj = i;
            t2 = t = s = String.format("%d", i);
            aTuple = new Object[] {i, x, t};
        }

        @Override
        PyTuple getTup() { return aTuple == null ? null : PyTuple.of(aTuple); }

        @Override
        void setTup(PyTuple tup) { this.aTuple = tup == null ? null : tup.toArray(); }
    }

    /**
     * Copy {@code tuple} elements to a new {@code T[]}, raising a
     * {@link PyBaseException TypeError} if any element cannot be assigned
     * to variable of type {@code T}.
     */
    private static <T> T[] fromTuple(Object tuple, Class<? extends T[]> arrayType)
            throws PyBaseException {
        // Loosely based on java.util.Arrays.copyOf
        if (tuple instanceof PyTuple) {
            PyTuple t = (PyTuple)tuple;
            int n = t.size();
            @SuppressWarnings("unchecked")
            T[] copy = (T[])Array.newInstance(arrayType.getComponentType(), n);
            try {
                System.arraycopy(t.toArray(), 0, copy, 0, n);
            } catch (ArrayStoreException ase) {
                PyType dstType = PyType.of(arrayType.getComponentType());
                throw PyErr.format(PyExc.TypeError, "tuple of %s expected", dstType);
            }
            return copy;
        } else {
            throw PyErr.format(PyExc.TypeError, "tuple expected");
        }
    }

    /**
     * Copy tuple elements to a new {@code double[]}, converting them with
     * {@link PyFloat#doubleValue(Object)}.
     *
     * @throws Throwable
     */
    private static double[] doubleFromTuple(Object tuple) throws Throwable {
        if (tuple instanceof PyTuple) {
            PyTuple t = (PyTuple)tuple;
            int n = t.size();
            Object[] value = t.toArray();
            double[] copy = new double[n];
            for (int i = 0; i < n; i++) { copy[i] = PyFloat.asDouble(value[i]); }
            return copy;
        } else {
            throw PyErr.format(PyExc.TypeError, "tuple expected");
        }
    }

    /**
     * Create a {@code tuple} in which the elements are equal to the values
     * in a given array.
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
     * Certain nested test classes implement these as standard. A base class
     * here is just a way to describe the tests once that reappear in each
     * nested case.
     */
    private abstract static class Base {

        // Working variables for the tests
        /** Name of the attribute. */
        String name;
        /** Documentation string. */
        String doc;
        /** The value set by delete. */
        Object deleted;
        /** Unbound OGS descriptor by type access to examine or call. */
        PyGetSetDescr gs1;
        /** The object of simple type on which to attempt access. */
        OWithGetSets o;
        /** Unbound PGS descriptor by type access to examine or call. */
        PyGetSetDescr gs2;
        /**
         * The object of a type with multiple self-classes on which to attempt
         * access.
         */
        PWithGetSets p;
        /**
         * An object of the adopted implementation on which to attempt the same
         * kind of access.
         */
        QWithGetSets q;

        void setup(String name, String doc, Object deleted, double oValue, double pValue,
                double qValue) throws Throwable {
            this.name = name;
            this.doc = doc;
            this.deleted = deleted;
            try {
                this.gs1 = (PyGetSetDescr)OWithGetSets.TYPE.lookup(name);
                this.o = new OWithGetSets(oValue);
                this.gs2 = (PyGetSetDescr)PWithGetSets.TYPE.lookup(name);
                this.p = new PWithGetSets(pValue);
                this.q = new QWithGetSets(qValue);
            } catch (ExceptionInInitializerError eie) {
                // Errors detected by the Exposer get wrapped so:
                Throwable t = eie.getCause();
                throw t == null ? eie : t;
            }
        }

        void setup(String name, String doc, double oValue, double pValue, double qValue)
                throws Throwable {
            setup(name, doc, null, oValue, pValue, qValue);
        }

        void setup(String name, double oValue, double pValue, double qValue) throws Throwable {
            setup(name, null, null, oValue, pValue, qValue);
        }

        /**
         * The attribute is a get-set descriptor that correctly reflects the
         * annotations in the defining class.
         *
         * @throws Throwable unexpectedly
         */
        @Test
        void descr_has_expected_fields() throws Throwable {
            assertEquals(name, gs2.__name__());
            assertEquals(doc, gs2.doc);
            String s = String.format("<attribute '%s' of 'PGS' objects>", name);
            assertEquals(s, gs2.toString());
            assertEquals(s, Abstract.repr(gs2));
        }

        /**
         * The string (repr) describes the type and attribute.
         *
         * @throws Throwable unexpectedly
         */
        void checkToString() throws Throwable {
            String s = String.format("<attribute '%s' of 'PGS' objects>", name);
            assertEquals(s, gs2.toString());
            assertEquals(s, Abstract.repr(gs2));
        }

        /**
         * The get-set descriptor may be used to read the field in an instance
         * of the object.
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
         * The get-set descriptor may be used to set the field in an instance of
         * the object.
         *
         * @throws Throwable unexpectedly
         */
        abstract void descr_set_works() throws Throwable;

        /**
         * {@link Abstract#setAttr(Object, String, Object)} may be used to set
         * the field in an instance of the object.
         *
         * @throws Throwable unexpectedly
         */
        abstract void abstract_setAttr_works() throws Throwable;

        /**
         * The get-set attribute raises {@link PyBaseException TypeError} when
         * supplied a value of unacceptable type.
         */
        abstract void set_detects_TypeError();
    }

    /**
     * Base test of settable attribute that may not be deleted.
     */
    abstract static class BaseSettableIndelible extends BaseSettable {

        /**
         * Attempting to delete the get-set attribute, where it has a setter but
         * no deleter, from an instance of the object, through the get-set
         * descriptor, raises {@link PyBaseException TypeError}.
         */
        @Test
        void rejects_descr_delete() {
            assertRaises(PyExc.TypeError, () -> gs1.__delete__(o));
            assertRaises(PyExc.TypeError, () -> gs1.__set__(o, null));
            assertRaises(PyExc.TypeError, () -> gs2.__delete__(p));
            assertRaises(PyExc.TypeError, () -> gs2.__set__(p, null));
            assertRaises(PyExc.TypeError, () -> gs2.__delete__(q));
            assertRaises(PyExc.TypeError, () -> gs2.__set__(q, null));
        }

        /**
         * Attempting to delete the get-set attribute, where it has a setter but
         * no deleter, from an instance of the object, through
         * {@link Abstract#delAttr(Object, String)}, raises
         * {@link PyExc#TypeError TypeError}.
         */
        @Test
        void rejects_abstract_delAttr() {
            assertRaises(PyExc.TypeError, () -> Abstract.delAttr(o, name));
            assertRaises(PyExc.TypeError, () -> Abstract.setAttr(o, name, null));
            assertRaises(PyExc.TypeError, () -> Abstract.delAttr(p, name));
            assertRaises(PyExc.TypeError, () -> Abstract.setAttr(p, name, null));
            assertRaises(PyExc.TypeError, () -> Abstract.delAttr(q, name));
            assertRaises(PyExc.TypeError, () -> Abstract.setAttr(q, name, null));
        }
    }

    /**
     * Base test of an optional attribute. Instances will raise
     * {@link PyAttributeError AttributeError} on access after deletion.
     */
    abstract static class BaseOptionalReference extends BaseSettable {

        /**
         * The get-set descriptor may be used to delete a field from an instance
         * of the object, causing it to disappear externally.
         *
         * @throws Throwable unexpectedly
         */
        @Test
        void descr_delete_removes() throws Throwable {
            gs1.__delete__(o);
            gs2.__delete__(p);
            gs2.__delete__(q);
            // After deletion, ...
            // ... __get__ raises AttributeError
            assertRaises(PyExc.AttributeError, () -> gs1.__get__(o, null));
            assertRaises(PyExc.AttributeError, () -> gs2.__get__(p, null));
            assertRaises(PyExc.AttributeError, () -> gs2.__get__(q, null));
            // ... __delete__ raises AttributeError
            assertRaises(PyExc.AttributeError, () -> gs1.__delete__(o));
            assertRaises(PyExc.AttributeError, () -> gs2.__delete__(p));
            assertRaises(PyExc.AttributeError, () -> gs2.__delete__(q));
        }

        /**
         * {@link Abstract#delAttr(Object, String)} to delete a field from an
         * instance of the object, causing it to disappear externally.
         *
         * @throws Throwable unexpectedly
         */
        @Test
        void abstract_delAttr_removes() throws Throwable {
            Abstract.delAttr(o, name);
            Abstract.delAttr(p, name);
            Abstract.delAttr(q, name);
            // After deletion, ...
            // ... getAttr and delAttr raise AttributeError
            assertRaises(PyExc.AttributeError, () -> Abstract.getAttr(o, name));
            assertRaises(PyExc.AttributeError, () -> Abstract.getAttr(p, name));
            assertRaises(PyExc.AttributeError, () -> Abstract.getAttr(q, name));
            assertRaises(PyExc.AttributeError, () -> Abstract.delAttr(o, name));
            assertRaises(PyExc.AttributeError, () -> Abstract.delAttr(p, name));
            assertRaises(PyExc.AttributeError, () -> Abstract.delAttr(q, name));
        }
    }

    /**
     * Base test of settable attribute where deletion sets a particular
     * value.
     */
    abstract static class BaseSettableDefault extends BaseSettable {

        /**
         * The get-set descriptor may be used to delete a field from an instance
         * of the object, meaning whatever the {@code Deleter} chooses. For test
         * purposes, we set a distinctive {@code deleted} value.
         *
         * @throws Throwable unexpectedly
         */
        @Test
        void descr_delete_sets_deleted() throws Throwable {
            gs1.__delete__(o);
            assertEquals(deleted, gs1.__get__(o, null));
            // __delete__ is idempotent
            gs1.__delete__(o);
            assertEquals(deleted, gs1.__get__(o, null));

            // And again for the adopting implementation
            gs2.__delete__(p);
            assertEquals(deleted, gs2.__get__(p, null));
            gs2.__delete__(p);
            assertEquals(deleted, gs2.__get__(p, null));

            // And again for the adopted implementation
            gs2.__delete__(q);
            assertEquals(deleted, gs2.__get__(q, null));
            gs2.__delete__(q);
            assertEquals(deleted, gs2.__get__(q, null));
        }

        /**
         * {@link Abstract#delAttr(Object, String)} to delete a field from an
         * instance of the object, meaning whatever the {@code deleter} chooses.
         * For test purposes, we mimic the behaviour of an optional member:
         * ({@code null} internally appears as {@code None} externally.
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

            // And again for the adopting implementation
            Abstract.delAttr(p, name);
            assertEquals(deleted, Abstract.getAttr(p, name));
            Abstract.delAttr(p, name);
            assertEquals(deleted, Abstract.getAttr(p, name));

            // And again for the adopted implementation
            Abstract.delAttr(q, name);
            assertEquals(deleted, Abstract.getAttr(q, name));
            Abstract.delAttr(q, name);
            assertEquals(deleted, Abstract.getAttr(q, name));
        }
    }

    @Nested
    @DisplayName("implemented as an int")
    class TestInt extends BaseSettableIndelible {

        @BeforeEach
        void setup() throws PyAttributeError, Throwable { setup("i", 42, 1969, -1); }

        @Override
        @Test
        void descr_get_works() throws Throwable {
            assertEquals(42, gs1.__get__(o, null));
            assertEquals(1969, gs2.__get__(p, null));
            assertPythonEquals(-1, gs2.__get__(q, null));
        }

        @Override
        @Test
        void abstract_getAttr_works() throws Throwable {
            assertEquals(42, Abstract.getAttr(o, name));
            assertEquals(1969, Abstract.getAttr(p, name));
            assertPythonEquals(-1, Abstract.getAttr(q, name));
        }

        @Override
        @Test
        void descr_set_works() throws Throwable {
            gs1.__set__(o, 43);
            gs2.__set__(p, 44);
            gs2.__set__(q, BigInteger.valueOf(45));
            assertEquals(43, o.i);
            assertEquals(44, p.i);
            assertPythonEquals(45, q.i);
        }

        @Override
        @Test
        void abstract_setAttr_works() throws Throwable {
            Abstract.setAttr(o, name, 43);
            Abstract.setAttr(p, name, 44);
            Abstract.setAttr(q, name, BigInteger.valueOf(45));
            assertEquals(43, o.i);
            assertEquals(44, p.i);
            assertPythonEquals(45, q.i);
        }

        @Override
        @Test
        void set_detects_TypeError() {
            // Things that are not a Python int
            assertRaises(PyExc.TypeError, () -> gs1.__set__(o, "Gumby"));
            assertRaises(PyExc.TypeError, () -> gs2.__set__(p, "Gumby"));
            assertRaises(PyExc.TypeError, () -> Abstract.setAttr(q, name, 1.0));
            assertRaises(PyExc.TypeError, () -> gs1.__set__(o, Py.None));
            assertRaises(PyExc.TypeError, () -> gs2.__set__(p, Py.None));
            assertRaises(PyExc.TypeError, () -> gs2.__set__(q, Py.None));
        }
    }

    @Nested
    @DisplayName("implemented as a double")
    class TestDouble extends BaseSettable {

        @BeforeEach
        void setup() throws PyAttributeError, Throwable { setup("x", "My test x", 42, 1969, -1); }

        @Override
        @Test
        void descr_get_works() throws Throwable {
            assertEquals(42.0, gs1.__get__(o, null));
            assertEquals(1969.0, gs2.__get__(p, null));
            assertEquals(-1.0, gs2.__get__(q, null));
        }

        @Override
        @Test
        void abstract_getAttr_works() throws Throwable {
            assertEquals(42.0, Abstract.getAttr(o, name));
            assertEquals(1969.0, Abstract.getAttr(p, name));
            assertEquals(-1.0, Abstract.getAttr(q, name));
        }

        @Override
        @Test
        void descr_set_works() throws Throwable {
            gs1.__set__(o, 1.125);
            gs2.__set__(p, -1.125);
            gs2.__set__(q, BigInteger.valueOf(111_222_333_444L));
            assertEquals(1.125, o.x);
            assertEquals(-1.125, p.x);
            assertEquals(111222333444.0, q.x);
        }

        @Override
        @Test
        void abstract_setAttr_works() throws Throwable {
            Abstract.setAttr(o, name, 1.125);
            Abstract.setAttr(p, name, -1.125);
            Abstract.setAttr(q, name, BigInteger.valueOf(111_222_333_444L));
            assertEquals(1.125, o.x);
            assertEquals(-1.125, p.x);
            assertEquals(111222333444.0, q.x);
        }

        @Override
        @Test
        void set_detects_TypeError() {
            // Things that are not a Python float
            assertRaises(PyExc.TypeError, () -> gs1.__set__(o, "Gumby"));
            assertRaises(PyExc.TypeError, () -> gs2.__set__(p, "Gumby"));
            assertRaises(PyExc.TypeError, () -> Abstract.setAttr(q, name, "42"));
            assertRaises(PyExc.TypeError, () -> gs1.__set__(o, Py.None));
            assertRaises(PyExc.TypeError, () -> gs2.__set__(p, Py.None));
            assertRaises(PyExc.TypeError, () -> gs2.__set__(q, Py.None));
        }

        /**
         * The get-set descriptor may be used to delete a field from an instance
         * of the object, meaning in this case, set it to {@code NaN}.
         *
         * @throws Throwable unexpectedly
         */
        @Test
        void descr_delete_sets_NaN() throws Throwable {
            gs1.__delete__(o);
            assertEquals(Double.NaN, gs1.__get__(o, null));
            gs1.__delete__(o);
            assertEquals(Double.NaN, gs1.__get__(o, null));

            // And again for the adopting implementation
            gs2.__delete__(p);
            assertEquals(Double.NaN, gs2.__get__(p, null));
            gs2.__delete__(p);
            assertEquals(Double.NaN, gs2.__get__(p, null));

            // And again for the adopted implementation
            gs2.__delete__(q);
            assertEquals(Double.NaN, gs2.__get__(q, null));
            gs2.__delete__(q);
            assertEquals(Double.NaN, gs2.__get__(q, null));
        }

        /**
         * {@link Abstract#delAttr(Object, String)} to delete a field from an
         * instance of the object, meaning in this case, set it to {@code NaN}.
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

            // And again for the adopted implementation
            Abstract.delAttr(q, name);
            assertEquals(Double.NaN, Abstract.getAttr(q, name));
            Abstract.delAttr(q, name);
            assertEquals(Double.NaN, Abstract.getAttr(q, name));
        }
    }

    @Nested
    @DisplayName("implemented as a String")
    class TestString extends BaseSettableDefault {

        @BeforeEach
        void setup() throws PyAttributeError, Throwable {
            setup("text", null, "<deleted>", 42, 1969, -1);
        }

        @Override
        @Test
        void descr_get_works() throws Throwable {
            assertEquals("42", gs1.__get__(o, null));
            assertEquals("1969", gs2.__get__(p, null));
            assertEquals("-1", gs2.__get__(q, null));
        }

        @Override
        @Test
        void abstract_getAttr_works() throws Throwable {
            assertEquals("42", Abstract.getAttr(o, name));
            assertEquals("1969", Abstract.getAttr(p, name));
            assertEquals("-1", Abstract.getAttr(q, name));
        }

        @Override
        @Test
        void descr_set_works() throws Throwable {
            gs1.__set__(o, "Enid");
            gs2.__set__(p, "D.P.");
            gs2.__set__(q, newPyUnicode("Gumby"));
            assertEquals("Enid", o.t);
            assertEquals("D.P.", p.t);
            assertEquals("Gumby", q.t);

            // __set__ works after delete
            gs1.__delete__(o);
            assertEquals(deleted, o.t);
            gs1.__set__(o, "Palin");
            assertEquals("Palin", o.t);

            gs2.__delete__(p);
            assertEquals(deleted, p.t);
            gs2.__set__(p, "Idle");
            assertEquals("Idle", p.t);

            gs2.__delete__(q);
            assertEquals(deleted, q.t);
            gs2.__set__(q, "Cleese");
            assertEquals("Cleese", q.t);
        }

        @Override
        @Test
        void abstract_setAttr_works() throws Throwable {
            Abstract.setAttr(o, name, "Enid");
            Abstract.setAttr(p, name, "D.P.");
            Abstract.setAttr(q, name, "Gumby");
            assertEquals("Enid", o.t);
            assertEquals("D.P.", p.t);
            assertEquals("Gumby", q.t);

            // setAttr works after delete
            Abstract.delAttr(o, name);
            assertEquals(deleted, o.t);
            Abstract.setAttr(o, name, "Palin");
            assertEquals("Palin", o.t);

            Abstract.delAttr(p, name);
            assertEquals(deleted, p.t);
            Abstract.setAttr(p, name, "Idle");
            assertEquals("Idle", p.t);

            // And again for the adopted implementation
            Abstract.delAttr(q, name);
            assertEquals(deleted, q.t);
            Abstract.setAttr(q, name, "Cleese");
            assertEquals("Cleese", q.t);
        }

        @Override
        @Test
        void set_detects_TypeError() {
            // Things that are not a Python str
            assertRaises(PyExc.TypeError, () -> gs1.__set__(o, 1));
            assertRaises(PyExc.TypeError, () -> gs2.__set__(p, 1));
            assertRaises(PyExc.TypeError, () -> Abstract.setAttr(q, name, 10.0));
            assertRaises(PyExc.TypeError, () -> gs1.__set__(o, new Object()));
            assertRaises(PyExc.TypeError, () -> gs2.__set__(p, new Object()));
            assertRaises(PyExc.TypeError, () -> gs2.__set__(q, new Object()));
        }
    }

    @Nested
    @DisplayName("implemented as an optional String")
    class TestOptionalString extends BaseOptionalReference {

        @BeforeEach
        void setup() throws PyAttributeError, Throwable { setup("s", 42, 1969, -1); }

        @Override
        @Test
        void descr_get_works() throws Throwable {
            assertEquals("42", gs1.__get__(o, null));
            assertEquals("1969", gs2.__get__(p, null));
            assertEquals("-1", gs2.__get__(q, null));
        }

        @Override
        @Test
        void abstract_getAttr_works() throws Throwable {
            assertEquals("42", Abstract.getAttr(o, name));
            assertEquals("1969", Abstract.getAttr(p, name));
            assertEquals("-1", Abstract.getAttr(q, name));
        }

        @Override
        @Test
        void descr_set_works() throws Throwable {
            gs1.__set__(o, "Enid");
            gs2.__set__(p, "D.P.");
            gs2.__set__(q, "Gumby");
            assertEquals("Enid", o.s);
            assertEquals("D.P.", p.s);
            assertEquals("Gumby", q.s);

            // __set__ works after delete
            gs1.__delete__(o);
            assertNull(o.s);
            gs1.__set__(o, "Palin");
            assertEquals("Palin", o.s);

            // __set__ works after delete
            gs2.__delete__(p);
            assertNull(p.s);
            gs2.__set__(p, "Idle");
            assertEquals("Idle", p.s);

            // And again for the adopted implementation
            gs2.__delete__(q);
            assertNull(q.s);
            gs2.__set__(q, "Cleese");
            assertEquals("Cleese", q.s);
        }

        @Override
        @Test
        void abstract_setAttr_works() throws Throwable {
            Abstract.setAttr(o, name, "Enid");
            Abstract.setAttr(p, name, "D.P.");
            Abstract.setAttr(q, name, newPyUnicode("Gumby"));
            assertEquals("Enid", o.s);
            assertEquals("D.P.", p.s);
            assertEquals("Gumby", q.s);

            // setAttr works after delete
            Abstract.delAttr(o, name);
            assertNull(o.s);
            Abstract.setAttr(o, name, "Palin");
            assertEquals("Palin", o.s);

            // And again for the adopted implementation
            Abstract.delAttr(p, name);
            assertNull(p.s);
            Abstract.setAttr(p, name, "Idle");
            assertEquals("Idle", p.s);

            // And again for the adopted implementation
            Abstract.delAttr(q, name);
            assertNull(q.s);
            Abstract.setAttr(q, name, "Cleese");
            assertEquals("Cleese", q.s);
        }

        @Override
        @Test
        void set_detects_TypeError() {
            // Things that are not a Python str
            assertRaises(PyExc.TypeError, () -> gs1.__set__(o, 1));
            assertRaises(PyExc.TypeError, () -> gs2.__set__(p, 2));
            assertRaises(PyExc.TypeError, () -> Abstract.setAttr(q, name, 10.0));
            assertRaises(PyExc.TypeError, () -> gs2.__set__(p, new Object()));
        }
    }

    @Nested
    @DisplayName("implemented as an Object")
    class TestObject extends BaseSettableDefault {

        @BeforeEach
        void setup() throws PyAttributeError, Throwable {
            setup("obj", null, Py.None, 42, 1969, -1);
        }

        @Override
        @Test
        void descr_get_works() throws Throwable {
            assertEquals(42, gs1.__get__(o, null));
            assertEquals(1969, gs2.__get__(p, null));
            assertPythonEquals(-1, gs2.__get__(q, null));
        }

        @Override
        @Test
        void abstract_getAttr_works() throws Throwable {
            assertEquals(42, Abstract.getAttr(o, name));
            assertEquals(1969, Abstract.getAttr(p, name));
            assertPythonEquals(-1, Abstract.getAttr(q, name));
        }

        @Override
        @Test
        void descr_set_works() throws Throwable {
            final Object one = BigInteger.ONE, dp = "D.P.", gumby = newPyUnicode("Gumby");
            gs1.__set__(o, one);
            gs2.__set__(p, dp);
            gs2.__set__(q, gumby);
            // Should get the same object
            assertSame(one, o.obj);
            assertSame(dp, p.obj);
            assertSame(gumby, q.obj);

            // __set__ works after delete
            final Object nonPython = new HashMap<String, Integer>();
            gs1.__delete__(o);
            assertNull(o.obj);
            gs1.__set__(o, nonPython);
            assertSame(nonPython, o.obj);

            // And again for the adopting implementation
            gs2.__delete__(p);
            assertNull(p.obj);
            gs2.__set__(p, nonPython);
            assertSame(nonPython, p.obj);

            // And again for the adopted implementation
            gs2.__delete__(q);
            assertNull(q.obj);
            gs2.__set__(q, nonPython);
            assertSame(nonPython, q.obj);
        }

        @Override
        @Test
        void abstract_setAttr_works() throws Throwable {
            final Object one = BigInteger.ONE, dp = "D.P.", gumby = newPyUnicode("Gumby");
            Abstract.setAttr(o, name, one);
            Abstract.setAttr(p, name, dp);
            Abstract.setAttr(q, name, gumby);

            // Should get the same object
            assertSame(one, o.obj);
            assertSame(dp, p.obj);
            assertSame(gumby, q.obj);

            // setAttr works after delete
            final Object palin = "Palin";
            Abstract.delAttr(o, name);
            assertNull(o.obj);
            Abstract.setAttr(o, name, palin);
            assertSame(palin, o.obj);

            // And again for the adopting implementation
            Abstract.delAttr(p, name);
            assertNull(p.obj);
            Abstract.setAttr(p, name, palin);
            assertSame(palin, p.obj);

            // And again for the adopted implementation
            Abstract.delAttr(q, name);
            assertNull(q.obj);
            Abstract.setAttr(q, name, palin);
            assertSame(palin, q.obj);
        }

        @Override
        @Test
        void set_detects_TypeError() {
            // Everything is a Python object (no TypeError)
            final float[] everything = {1, 2, 3};
            assertDoesNotThrow(() -> {
                gs1.__set__(o, everything);
                gs2.__set__(p, everything);
                Abstract.setAttr(q, name, System.err);
            });
            assertSame(everything, o.obj);
            assertSame(everything, p.obj);
            assertSame(System.err, q.obj);
        }
    }

    @Nested
    @DisplayName("implemented as a PyTuple")
    class TestTuple extends BaseSettableDefault {

        PyTuple oRef, pRef, qRef;

        @BeforeEach
        void setup() throws PyAttributeError, Throwable {
            setup("tup", null, Py.None, 42, 1969, -1);
            oRef = PyTuple.of(42, 42.0, "42");
            pRef = PyTuple.of(1969, 1969.0, "1969");
            qRef = PyTuple.of(-1, -1.0, "-1");
        }

        @Override
        @Test
        void descr_get_works() throws Throwable {
            assertEquals(oRef, gs1.__get__(o, null));
            assertEquals(pRef, gs2.__get__(p, null));
            assertEquals(qRef, gs2.__get__(q, null));
        }

        @Override
        @Test
        void abstract_getAttr_works() throws Throwable {
            assertEquals(oRef, Abstract.getAttr(o, name));
            assertEquals(pRef, Abstract.getAttr(p, name));
            assertEquals(qRef, Abstract.getAttr(q, name));
        }

        @Override
        @Test
        void descr_set_works() throws Throwable {
            final Object tup2 = PyTuple.of(2, 3, 4);
            gs2.__set__(p, tup2);
            assertEquals(tup2, p.tup);

            // __set__ works after delete
            final Object[] tup3array = new Object[] {3, 4, 5};
            final Object tup3 = PyTuple.of(tup3array);
            gs1.__delete__(o);
            assertNull(o.tup);
            gs1.__set__(o, tup3);
            assertEquals(tup3, o.tup);

            // And again for the adopting implementation
            gs2.__delete__(p);
            assertNull(p.tup);
            gs2.__set__(p, tup3);
            assertEquals(tup3, p.tup);

            // And again for the adopted implementation
            gs2.__delete__(q);
            assertNull(q.aTuple);
            gs2.__set__(q, tup3);
            assertArrayEquals(tup3array, q.aTuple);
        }

        @Override
        @Test
        void abstract_setAttr_works() throws Throwable {
            final Object gumby = PyTuple.from(List.of("D", "P", "Gumby"));
            Abstract.setAttr(p, name, gumby);
            // Should get the same object
            assertSame(gumby, p.tup);

            // setAttr works after delete
            final Object[] tup3array = new Object[] {3, 4, 5};
            final Object tup3 = PyTuple.of(tup3array);
            Abstract.delAttr(o, name);
            assertNull(o.tup);
            Abstract.setAttr(o, name, tup3);
            assertSame(tup3, o.tup);

            // And again for the adopting implementation
            Abstract.delAttr(p, name);
            assertNull(p.tup);
            Abstract.setAttr(p, name, tup3);
            assertSame(tup3, p.tup);

            // And again for the adopted implementation
            Abstract.delAttr(q, name);
            assertNull(q.aTuple);
            Abstract.setAttr(q, name, tup3);
            assertArrayEquals(tup3array, q.aTuple);
        }

        @Override
        @Test
        void set_detects_TypeError() {
            // Things that are not a Python tuple
            assertRaises(PyExc.TypeError, () -> gs1.__set__(o, 1));
            assertRaises(PyExc.TypeError, () -> gs2.__set__(p, 1));
            assertRaises(PyExc.TypeError, () -> Abstract.setAttr(q, name, ""));
            assertRaises(PyExc.TypeError, () -> gs1.__set__(o, new Object()));
            assertRaises(PyExc.TypeError, () -> gs2.__set__(p, new Object()));
            assertRaises(PyExc.TypeError, () -> gs2.__set__(q, new Object()));
        }
    }

    @Nested
    @DisplayName("providing a double array (as a tuple)")
    class TestDoubleArray extends BaseSettable {

        PyTuple oval, pval, qval, ival, rval;
        double[] ref;

        @BeforeEach
        void setup() throws PyAttributeError, Throwable {
            setup("doubles", 42, 1969, -1);
            oval = PyTuple.of(1., 42., 1764., 74088.);
            pval = PyTuple.of(1., 1969., 3876961., 7633736209.);
            qval = PyTuple.of(1., -1., 1., -1.);
            ival = PyTuple.of(3, 14, 15, 926);
            ref = new double[] {3.0, 14.0, 15.0, 926.0};
            rval = tupleFrom(ref);
        }

        @Override
        @Test
        void descr_get_works() throws Throwable {
            assertEquals(oval, gs1.__get__(o, null));
            assertEquals(pval, gs2.__get__(p, null));
            q.setDoubleArray(ref);
            assertEquals(rval, gs2.__get__(q, null));
        }

        @Override
        @Test
        void abstract_getAttr_works() throws Throwable {
            assertEquals(oval, Abstract.getAttr(o, name));
            assertEquals(pval, Abstract.getAttr(p, name));
            q.setDoubleArray(ref);
            assertEquals(rval, Abstract.getAttr(q, name));
        }

        @Override
        @Test
        void descr_set_works() throws Throwable {
            gs1.__set__(o, ival);
            gs2.__set__(p, ival);
            gs2.__set__(q, qval);
            assertArrayEquals(ref, o.doubleArray);
            assertArrayEquals(ref, p.doubleArray);

            // __set__ works after delete
            gs1.__delete__(o);
            assertEquals(0, o.doubleArray.length);
            gs1.__set__(o, ival);
            assertArrayEquals(ref, o.doubleArray);

            // __set__ works after delete
            gs2.__delete__(p);
            assertEquals(0, p.doubleArray.length);
            gs2.__set__(p, ival);
            assertArrayEquals(ref, p.doubleArray);
        }

        @Override
        void abstract_setAttr_works() throws Throwable {
            Abstract.setAttr(o, name, ival);
            assertArrayEquals(ref, o.doubleArray);
            Abstract.setAttr(p, name, ival);
            assertArrayEquals(ref, p.doubleArray);

            // __set__ works after delete
            Abstract.delAttr(o, name);
            assertEquals(0, o.doubleArray.length);
            Abstract.setAttr(o, name, ival);
            assertArrayEquals(ref, o.doubleArray);

            // __set__ works after delete
            Abstract.delAttr(p, name);
            assertEquals(0, p.doubleArray.length);
            Abstract.setAttr(p, name, ival);
            assertArrayEquals(ref, p.doubleArray);
        }

        @Override
        @Test
        void set_detects_TypeError() {
            // Things that are not a Python tuple
            assertRaises(PyExc.TypeError, () -> gs1.__set__(o, 2.0));
            assertRaises(PyExc.TypeError, () -> gs2.__set__(p, 2.0));
            assertRaises(PyExc.TypeError, () -> Abstract.setAttr(q, name, Py.None));
            assertRaises(PyExc.TypeError, () -> gs2.__set__(p, new double[] {1, 2, 3}));
        }
    }

    @Nested
    @DisplayName("providing a string array (as a tuple)")
    class TestStringArray extends BaseSettable {

        PyTuple twits, gumbys, rval, sval;

        @BeforeEach
        void setup() throws PyAttributeError, Throwable {
            setup("names", 42, 1969, -1);
            twits = new PyTuple(TWITS);
            gumbys = new PyTuple(GUMBYS);
        }

        @Override
        @Test
        void descr_get_works() throws Throwable {
            assertEquals(twits, gs1.__get__(o, null));
            assertEquals(twits, gs2.__get__(p, null));
            q.setNameArray(GUMBYS);
            assertEquals(gumbys, gs2.__get__(q, null));
        }

        @Override
        @Test
        void abstract_getAttr_works() throws Throwable {
            assertEquals(twits, Abstract.getAttr(o, name));
            assertEquals(twits, Abstract.getAttr(p, name));
            q.setNameArray(GUMBYS);
            assertEquals(gumbys, Abstract.getAttr(q, name));
        }

        @Override
        @Test
        void descr_set_works() throws Throwable {
            gs1.__set__(o, gumbys);
            assertArrayEquals(GUMBYS, o.nameArray);
            gs2.__set__(p, gumbys);
            assertArrayEquals(GUMBYS, p.nameArray);
            // __set__ works after delete
            gs1.__delete__(o);
            assertEquals(0, o.nameArray.length);
            gs1.__set__(o, twits);
            assertArrayEquals(TWITS, o.nameArray);
            // __set__ works after delete
            gs2.__delete__(p);
            assertEquals(0, p.nameArray.length);
            gs2.__set__(p, twits);
            assertArrayEquals(TWITS, p.nameArray);
        }

        @Override
        void abstract_setAttr_works() throws Throwable {
            Abstract.setAttr(o, name, gumbys);
            assertArrayEquals(GUMBYS, o.nameArray);
            Abstract.setAttr(p, name, gumbys);
            assertArrayEquals(GUMBYS, p.nameArray);
            // __set__ works after delete
            Abstract.delAttr(o, name);
            assertEquals(0, o.nameArray.length);
            Abstract.setAttr(o, name, twits);
            assertArrayEquals(TWITS, o.nameArray);
            // __set__ works after delete
            Abstract.delAttr(p, name);
            assertEquals(0, p.nameArray.length);
            Abstract.setAttr(p, name, twits);
            assertArrayEquals(TWITS, p.nameArray);
        }

        @Override
        @Test
        void set_detects_TypeError() {
            // Things that are not a Python tuple
            assertRaises(PyExc.TypeError, () -> gs1.__set__(o, 0));
            assertRaises(PyExc.TypeError, () -> gs2.__set__(p, ""));
            assertRaises(PyExc.TypeError, () -> Abstract.setAttr(q, name, Py.None));
            assertRaises(PyExc.TypeError, () -> gs2.__set__(p, new String[] {}));
        }
    }

    /**
     * Base test of read-only attribute tests.
     */
    abstract static class BaseReadonly extends Base {

        /**
         * Raises {@link PyAttributeError AttributeError} when the get-set
         * descriptor is asked to set the field in an instance of the object,
         * even if the type is correct.
         */
        @Test
        void rejects_descr_set() {
            assertRaises(PyExc.AttributeError, () -> gs1.__set__(o, 1234));
            assertRaises(PyExc.AttributeError, () -> gs2.__set__(p, 1234));
            assertRaises(PyExc.AttributeError, () -> gs2.__set__(q, 1.0));
            assertRaises(PyExc.AttributeError, () -> gs1.__set__(o, "Gumby"));
            assertRaises(PyExc.AttributeError, () -> gs2.__set__(p, "Gumby"));
            assertRaises(PyExc.AttributeError, () -> gs2.__set__(q, Py.None));
        }

        /**
         * Raises {@link PyAttributeError AttributeError} when
         * {@link Abstract#setAttr(Object, String, Object)} tries to set the
         * field in an instance of the object, even if the type is correct.
         */
        @Test
        void rejects_abstract_setAttr() {
            assertRaises(PyExc.AttributeError, () -> Abstract.setAttr(o, name, 0));
            assertRaises(PyExc.AttributeError, () -> Abstract.setAttr(p, name, 1234));
            assertRaises(PyExc.AttributeError, () -> Abstract.setAttr(q, name, 1.0));
            assertRaises(PyExc.AttributeError, () -> Abstract.setAttr(o, name, ""));
            assertRaises(PyExc.AttributeError, () -> Abstract.setAttr(p, name, "Gumby"));
            assertRaises(PyExc.AttributeError, () -> Abstract.setAttr(q, name, Py.None));
        }

        /**
         * Attempting to delete a get-set attribute, where it has no setter or
         * deleter (is read-only), from an instance of the object, through the
         * get-set descriptor, raises {@link PyAttributeError AttributeError}.
         */
        @Test
        void rejects_descr_delete() {
            assertRaises(PyExc.AttributeError, () -> gs1.__delete__(o));
            assertRaises(PyExc.AttributeError, () -> gs2.__delete__(p));
            assertRaises(PyExc.AttributeError, () -> gs2.__set__(p, null));
        }

        /**
         * Attempting to delete a get-set attribute, where it has no setter or
         * deleter (is read-only), from an instance of the object, through
         * {@link Abstract#delAttr(Object, String)}, raises
         * {@link PyAttributeError AttributeError }.
         */
        @Test
        void rejects_abstract_delAttr() {
            assertRaises(PyExc.AttributeError, () -> Abstract.delAttr(o, name));
            assertRaises(PyExc.AttributeError, () -> Abstract.delAttr(p, name));
        }
    }

    @Nested
    @DisplayName("implemented as a read-only int")
    class TestIntRO extends BaseReadonly {

        @BeforeEach
        void setup() throws PyAttributeError, Throwable { setup("i2", 42, 1969, -1); }

        @Override
        @Test
        void descr_get_works() throws Throwable {
            assertEquals(42, gs1.__get__(o, null));
            assertEquals(1969, gs2.__get__(p, null));
            assertPythonEquals(-1, gs2.__get__(q, null));
        }

        @Override
        @Test
        void abstract_getAttr_works() throws Throwable {
            assertEquals(42, Abstract.getAttr(o, name));
            assertEquals(1969, Abstract.getAttr(p, name));
            assertPythonEquals(-1, Abstract.getAttr(q, name));
        }
    }

    @Nested
    @DisplayName("implemented as a final double")
    class TestDoubleRO extends BaseReadonly {

        @BeforeEach
        void setup() throws PyAttributeError, Throwable { setup("x2", "Another x", 42, 1969, -1); }

        @Override
        @Test
        void descr_get_works() throws Throwable {
            assertEquals(42.0, gs1.__get__(o, null));
            assertEquals(1969.0, gs2.__get__(p, null));
            assertEquals(-1.0, gs2.__get__(q, null));
        }

        @Override
        @Test
        void abstract_getAttr_works() throws Throwable {
            assertEquals(42.0, Abstract.getAttr(o, name));
            assertEquals(1969.0, Abstract.getAttr(p, name));
            assertEquals(-1.0, Abstract.getAttr(q, name));
        }
    }

    @Nested
    @DisplayName("implemented as a read-only String")
    class TestStringRO extends BaseReadonly {

        @BeforeEach
        void setup() throws PyAttributeError, Throwable { setup("text2", 42, 1969, -1); }

        @Override
        @Test
        void descr_get_works() throws Throwable {
            assertEquals("42", gs1.__get__(o, null));
            assertEquals("1969", gs2.__get__(p, null));
            assertEquals("-1", gs2.__get__(q, null));
        }

        @Override
        @Test
        void abstract_getAttr_works() throws Throwable {
            assertEquals("42", Abstract.getAttr(o, name));
            assertEquals("1969", Abstract.getAttr(p, name));
            assertEquals("-1", Abstract.getAttr(q, name));
        }
    }
}
