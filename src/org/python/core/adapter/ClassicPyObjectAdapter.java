package org.python.core.adapter;

import java.math.BigInteger;

import org.python.core.Py;
import org.python.core.PyArray;
import org.python.core.PyFloat;
import org.python.core.PyInteger;
import org.python.core.PyJavaType;
import org.python.core.PyLong;
import org.python.core.PyObject;
import org.python.core.PyProxy;
import org.python.core.PyType;
import org.python.core.PyUnicode;

/**
 * Implements the algorithm originally used in {@link Py#java2py} to adapt objects.
 *
 * Pre-class adapters are added to handle instances of PyObject, PyProxy and null values. Class
 * adapters are added to handle builtin Java classes: String, Integer, Float, Double, Byte, Long,
 * Short, Character, Class and Boolean. An adapter is added to the post-class adapters to handle
 * wrapping arrays properly. Finally, if all of the added adapters can handle an object, it's
 * wrapped in a PyJavaInstance.
 *
 */
public class ClassicPyObjectAdapter extends ExtensiblePyObjectAdapter {

    public ClassicPyObjectAdapter() {
        addPreClass(new PyObjectAdapter() {

            @Override
            public PyObject adapt(Object o) {
                return (PyObject)o;
            }

            @Override
            public boolean canAdapt(Object o) {
                return o instanceof PyObject;
            }
        });
        addPreClass(new PyObjectAdapter() {

            @Override
            public PyObject adapt(Object o) {
                return ((PyProxy)o)._getPyInstance();
            }

            @Override
            public boolean canAdapt(Object o) {
                return o instanceof PyProxy;
            }
        });
        addPreClass(new PyObjectAdapter() {

            @Override
            public boolean canAdapt(Object o) {
                return o == null;
            }

            @Override
            public PyObject adapt(Object o) {
                return Py.None;
            }
        });
        add(new ClassAdapter(String.class) {

            @Override
            public PyObject adapt(Object o) {
                return new PyUnicode((String)o);
            }

        });
        add(new ClassAdapter(Character.class) {

            @Override
            public PyObject adapt(Object o) {
                return PyUnicode.from((Character)o);
            }

        });
        add(new ClassAdapter(Class.class) {

            @Override
            public PyObject adapt(Object o) {
                return PyType.fromClass((Class<?>)o, false);
            }

        });
        add(new NumberToPyFloat(Double.class));
        add(new NumberToPyFloat(Float.class));
        add(new NumberToPyInteger(Integer.class));
        add(new NumberToPyInteger(Byte.class));
        add(new NumberToPyInteger(Short.class));
        add(new ClassAdapter(Long.class) {

            @Override
            public PyObject adapt(Object o) {
                return new PyLong(((Number)o).longValue());
            }

        });

        add(new ClassAdapter(BigInteger.class) {

            @Override
            public PyObject adapt(Object o) {
                return new PyLong((BigInteger)o);
            }

        });

        add(new ClassAdapter(Boolean.class) {

            @Override
            public PyObject adapt(Object o) {
                return ((Boolean)o).booleanValue() ? Py.True : Py.False;
            }

        });

        addPostClass(new PyObjectAdapter() {

            @Override
            public PyObject adapt(Object o) {
                return new PyArray(o.getClass().getComponentType(), o);
            }

            @Override
            public boolean canAdapt(Object o) {
                return o.getClass().isArray();
            }
        });
    }

    /**
     * Always returns true as we just return new PyJavaInstance(o) if the adapters added to the
     * superclass can't handle o.
     */
    @Override
    public boolean canAdapt(Object o) {
        return true;
    }

    @Override
    public PyObject adapt(Object o) {
        PyObject result = super.adapt(o);
        if (result != null) {
            return result;
        }
        return PyJavaType.wrapJavaObject(o);
    }

    private static class NumberToPyInteger extends ClassAdapter {

        public NumberToPyInteger(Class c) {
            super(c);
        }

        @Override
        public PyObject adapt(Object o) {
            return new PyInteger(((Number)o).intValue());
        }

    }

    private static class NumberToPyFloat extends ClassAdapter {

        public NumberToPyFloat(Class c) {
            super(c);
        }

        @Override
        public PyObject adapt(Object o) {
            return new PyFloat(((Number)o).doubleValue());
        }

    }
}
