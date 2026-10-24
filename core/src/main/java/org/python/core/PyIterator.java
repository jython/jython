// Copyright (c)2026 Jython Developers.
// Licensed to PSF under a contributor agreement.
package org.python.core;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.function.Supplier;

import org.python.internal.Util;
import org.python.kernel.KernelTypeFlag;
import org.python.kernel.Representation;
import org.python.kernel.SpecialMethod;
import org.python.types.Feature;
import org.python.types.TypeSpec;

/**
 * The Python {@code iterator} type provides iteration over any Python
 * <em>sequence</em>. We make this object equally a Java
 * {@code Iterator<Object>} to make implementation more readable. See
 * also {@link Abstract#getIterator(Object)}. It has two concrete
 * subclasses: for types that implement {@code __getitem__} and
 * {@code __iter__} respectively.
 * <p>
 * Note that (in CPython) well-known built-in sequence types define
 * specialised iterator types, leaving this type mostly as an iterator
 * on user-defined sequence types (with {@code __getitem__} but not
 * {@code __iter__}).
 */
abstract class PyIterator extends AbstractPyIterator {
    /** The type {@code iterator}. */
    static final PyType TYPE = PyType.fromSpec(
            new TypeSpec("iterator", MethodHandles.lookup()).remove(Feature.INSTANTIABLE));

    /**
     * The next item to return or {@code null} if we must call
     * {@code __next__} to fill it.
     */
    Object waiting;

    @Override
    public PyType getType() { return TYPE; }

    /**
     * Call {@link #hasNext()}, and if that finds or places the next value
     * of the iterable in {@link #waiting}, return it. If that is not
     * possible ({@code hasNext()} returns {@code false}), throw the
     * specified exception.
     *
     * @param <E> type of exception
     * @param exc specified exception
     * @return value of {@code next()} or {@code __next__()}
     * @throws E when there is no next element
     */
    @Override
    <E extends RuntimeException> Object next(Supplier<E> exc) throws E {
        if (hasNext()) {
            // There is definitely a waiting object now.
            Object nextObject = waiting;
            waiting = null;
            return nextObject;
        } else {
            // hasNext() was unable to get a new waiting object.
            throw exc.get();
        }
    }

    /**
     * A Python and Java iterator relying only on {@code __getitem__}
     * accepting an integer index.
     */
    static class GetItem extends PyIterator {
        /**
         * Index of the next item {@code __next__} will return. -1 if exhausted.
         */
        private int index;

        /**
         * Method handle (bound to the sequence) that will retrieve an element.
         * Signature is {@code O(O)}.
         */
        private final MethodHandle getitem;

        /**
         * Construct an instance of {@code PyIterator}, a Python
         * {@code iterator}, from a given Python sequence (defining
         * {@code __getitem__}).
         *
         * @param seq on which this is an iterator
         */
        public GetItem(Object seq) {
            Representation rep = Abstract.representation(seq);
            if (rep.hasFeature(seq, KernelTypeFlag.HAS_GETITEM)) {
                this.getitem = rep.op_getitem().bindTo(seq);
            } else {
                throw new IllegalArgumentException(SpecialMethod.op_getitem.methodName);
            }
        }

        /**
         * {@inheritDoc}
         * <p>
         * Calling {@code hasNext()} may call the {@code __getitem__} special
         * method of the underlying object to determine a value to return in the
         * next call to {@link #next()}.
         */
        @Override
        public boolean hasNext() {
            if (waiting != null)
                return true;
            else if (index >= 0) {
                // Put the next object in waiting (or return false)
                try {
                    waiting = getitem.invokeExact((Object)index++);
                    return true;
                } catch (PyStopIteration e) {
                    // Signal in index that we reached the end
                    index = -1;
                } catch (PyBaseException e) {
                    e.only(PyExc.IndexError);
                    // Signal in index that we reached the end
                    index = -1;
                } catch (Throwable t) {
                    throw Util.asUnchecked(t);
                }
            }
            // The iterator is exhausted
            assert waiting == null;
            return false;
        }

    }

    /**
     * A Python and Java iterator relying on the {@code __next__} method of
     * a Python iterator.
     */
    static class Next extends PyIterator {

        /**
         * Whether there is a next item {@code __next__} will return.
         * {@code true} if exhausted.
         */
        private boolean exhausted;

        /**
         * Method handle (bound to the sequence) that will retrieve an element.
         * Signature is {@code O()}.
         */
        private final MethodHandle next;

        /**
         * Construct an instance of {@code PyIterator} from a given Python
         * iterator (defining {@code __next__}). Note that while {@link GetItem}
         * is constructed on the sequence itself, {@code Next} is constructed on
         * the iterator returned by calling {@code __iter__}. This asymmetry is
         * intentional, supporting an optimisation when {@code iter} may be used
         * already as in {@link Abstract#getIterator(Object, Supplier)}.
         *
         * @param <E> the type of exception to throw
         * @param iter Python iterator to wrap
         * @param exc for the exception (e.g. lambda expression)
         * @throws E if {@code iter.__next__} is not defined
         */
        public <E extends PyBaseException> Next(Object iter, Supplier<E> exc) {
            Representation rep = Abstract.representation(iter);
            if (rep.hasFeature(iter, KernelTypeFlag.HAS_NEXT)) {
                this.next = rep.op_next().bindTo(iter);
            } else {
                throw exc.get();
            }
        }

        /**
         * {@inheritDoc}
         * <p>
         * Calling {@code hasNext()} may call the {@code __getitem__} special
         * method of the underlying object to determine a value to return in the
         * next call to {@link #next()}.
         */
        @Override
        public boolean hasNext() {
            if (waiting != null)
                return true;
            else if (!exhausted) {
                // Put the next object in waiting (or return false)
                try {
                    waiting = next.invokeExact();
                    return true;
                } catch (PyStopIteration e) {
                    // Signal in index that we reached the end
                    exhausted = true;
                } catch (PyBaseException e) {
                    e.only(PyExc.IndexError);
                    // Signal in index that we reached the end
                    exhausted = true;
                } catch (Throwable t) {
                    throw Util.asUnchecked(t);
                }
            }
            // The iterator is exhausted
            assert waiting == null;
            return false;
        }
    }
}
