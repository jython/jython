// Copyright (c)2026 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.core;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Supplier;

import org.python.types.WithClass;

/** Abstract base class for defining Python iterators. */
abstract class AbstractPyIterator implements WithClass, Iterator<Object> {

    /**
     * {@inheritDoc}
     * <p>
     * {@code hasNext()} may perform more than a simple test. Where this
     * iterator wraps a Python object, it may call the {@code __next__} or
     * {@code __getitem__} special method of that wrapped object to
     * determine whether there is a value to return in the next call to
     * {@link #next()}. In that case, implementations must cache that value
     * so that {@link #next()} does not call {@code __next__} a second time.
     */
    @Override
    public abstract boolean hasNext();

    /**
     * Inner implementation of both {@link #next()} and {@link #__next__()}.
     * In cases where {@link #hasNext()} has to advance a wrapped generator,
     * {@code next()} need only consume that cached value.
     *
     * @param <E> type of exception
     * @param exc specified exception
     * @return value of {@code next()} or {@code __next__()}
     * @throws E when there is no next element
     */
    abstract <E extends RuntimeException> Object next(Supplier<E> exc) throws E;

    @Override
    public Object next() { return next(NoSuchElementException::new); }

    // special methods -----------------------------------------------

    /**
     * Get the iterator itself. A Python iterator {@code __iter__} is
     * required to return its {@code self}. This is required to allow both
     * containers and iterators to be used with the {@code for} and
     * {@code in} statements. It has to be defined in the Python
     * implementation class to get exposed to Python.
     *
     * @return this iterator
     */
    final Object __iter__() { return this; }

    /**
     * Get the next item from the iteration. Each concrete sub-class must
     * implement the special function {@code __next__} in its own way.
     *
     * @implNote Exhaustion of the iterator is signalled by an exception as
     *     is standard for Python objects. This exception does not need to
     *     carry any context, since it will be caught by the surrounding
     *     loop, in an idiom like:<pre>
     * try {
     *     for (;;) { list.add(next.invokeExact()); }
     * } catch (PyStopIteration si) {}
     * </pre>
     * @return the next object
     * @throws PyBaseException (StopIteration) signifying no more items
     */
    Object __next__() { return next(PyStopIteration::new); }

    @Override
    public String toString() { return PyUtil.defaultToString(this); }
}
