/* Copyright (c) Jython Developers */
package org.python.modules;

import java.util.ArrayList;
import java.util.List;

import org.python.core.__builtin__;
import org.python.core.ArgParser;
import org.python.core.ClassDictInit;
import org.python.core.Py;
import org.python.core.PyException;
import org.python.core.PyInteger;
import org.python.core.PyIterator;
import org.python.core.PyNone;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.core.PyTuple;
import org.python.core.PyXRange;

import java.util.Arrays; //XXX

/**
 * Functional tools for creating and using iterators. Java implementation of the CPython module
 * itertools.
 * 
 * @since 2.5
 */
public class itertools implements ClassDictInit {

    public static PyString __doc__ = new PyString(
            "Functional tools for creating and using iterators.\n\nInfinite iterators:\n"
	        + "count([n]) --> n, n+1, n+2, ...\n"
	        + "cycle(p) --> p0, p1, ... plast, p0, p1, ...\n"
	        + "repeat(elem [,n]) --> elem, elem, elem, ... endlessly or up to n times\n\n"
	    
	        + "Iterators terminating on the shortest input sequence:\n"
	        + "chain(p, q, ...) --> p0, p1, ... plast, q0, q1, ...\n"
	        + "compress(data, selectors) --> (d[0] if s[0]), (d[1] if s[1]), ...\n"
	        + "dropwhile(pred, seq) --> seq[n], seq[n+1], starting when pred fails\n"
	        + "groupby(iterable[, keyfunc]) --> sub-iterators grouped by value of keyfunc(v)\n"
	        + "ifilter(pred, seq) --> elements of seq where pred(elem) is True\n"
	        + "ifilterfalse(pred, seq) --> elements of seq where pred(elem) is False\n"
	        + "islice(seq, [start,] stop [, step]) --> elements from seq[start:stop:step]\n"
	        + "imap(fun, p, q, ...) --> fun(p0, q0), fun(p1, q1), ...\n"
	        + "starmap(fun, seq) --> fun(*seq[0]), fun(*seq[1]), ...\n"
	        + "tee(it, n=2) --> (it1, it2 , ... itn) splits one iterator into n\n"
	        + "takewhile(pred, seq) --> seq[0], seq[1], until pred fails\n"
	        + "izip(p, q, ...) --> (p[0], q[0]), (p[1], q[1]), ...\n"
	        + "izip_longest(p, q, ...) --> (p[0], q[0]), (p[1], q[1]), ...\n\n"

	        + "Combinatoric generators:\n"
	        + "product(p, q, ... [repeat=1]) --> cartesian product\n"
	        + "permutations(p[, r])\n"
	        + "combinations(p, r)\n"
	        + "combinations_with_replacement(p, r)");

    /**
     * Iterator base class used by most methods.
     */
    static abstract class ItertoolsIterator extends PyIterator {

        /**
         * Returns the next element from an iterator. If it raises/throws StopIteration just store
         * the Exception and return null according to PyIterator practice.
         */
        protected PyObject nextElement(PyObject pyIter) {
            PyObject element = null;
            try {
                element = pyIter.__iternext__();//next();
            } catch (PyException pyEx) {
                if (pyEx.match(Py.StopIteration)) {
                    // store exception - will be used by PyIterator.next()
                    stopException = pyEx;
                } else {
                    throw pyEx;
                }
            }
            return element;
        }
    }

    public static void classDictInit(PyObject dict) {
    }
    

    public static PyString __doc__count = new PyString(
        "count(start=0, step=1) --> count object\n\n" +
        "Return a count object whose .next() method returns consecutive values.\n" +
        "  Equivalent to:\n" +
        "\n" +
        "      def count(firstval=0, step=1):\n" +
        "      x = firstval\n" +
        "      while 1:\n" +
        "          yield x\n" +
        "          x += step\n");

    public static PyIterator count(final int init) {
        return count(init, 1);
    }

    /**
     * Creates an iterator that returns consecutive integers starting at <code>init</code>.
     */
    public static PyIterator count(final int init, final int step) {
        return new PyIterator() {
            int counter = init;
            int stepper = step;

            public PyObject __iternext__() {
                return new PyInteger(counter+=stepper);
            }
            
            public PyString __repr__() {
                return (PyString)(Py.newString("count(%d, %d)").__mod__(new PyTuple(
                                Py.newInteger(counter), Py.newInteger(stepper))));
            }

        };
    }

    /**
     * Creates an iterator that returns consecutive integers starting at 0.
     */
    public static PyIterator count() {
        return itertools.count(0);
    }

    public static PyString __doc__cycle = new PyString(
            "cycle(iterable) --> cycle object\n\nReturn elements from the iterable "
                    + "until itis exhausted.\nThen repeat the sequence indefinitely.");

    /**
     * Returns an iterator that iterates over an iterable, saving the values for each iteration.
     * When the iterable is exhausted continues to iterate over the saved values indefinitely.
     */
    public static PyIterator cycle(final PyObject sequence) {
        return new ItertoolsIterator() {
            List<PyObject> saved = new ArrayList<PyObject>();
            int counter = 0;
            PyObject iter = sequence.__iter__();

            boolean save = true;

            public PyObject __iternext__() {
                if (save) {
                    PyObject obj = nextElement(iter);
                    if (obj != null) {
                        saved.add(obj);
                        return obj;
                    } else {
                        save = false;
                    }
                }
                if (saved.size() == 0) {
                    return null;
                }
                
                // pick element from saved List
                if (counter >= saved.size()) {
                    // start over again
                    counter = 0;
                }
                return saved.get(counter++);
            }

        };
    }
    
    

    public static PyString __doc__chain = new PyString(
            "chain(*iterables) --> chain object\n\nReturn a chain object "
                    + "whose .next() method returns elements from the\nfirst iterable until it is exhausted, then elements"
                    + " from the next\niterable, until all of the iterables are exhausted.");

    /**
     * Creates an iterator that iterates over a <i>chain</i> of iterables.
     */
    public static PyIterator chain(final PyObject[] iterables) {
        final PyObject[] iterators = new PyObject[iterables.length];
        for (int i = 0; i < iterables.length; i++) {
            iterators[i] = iterables[i].__iter__();
        }

        return new ItertoolsIterator() {
            int iteratorIndex = 0;

            public PyObject __iternext__() {
                PyObject next = null;
                for (; iteratorIndex < iterators.length; iteratorIndex++) {
                    next = nextElement(iterators[iteratorIndex]);
                    if (next != null) {
                        break;
                    }
                }
                return next;
            }

        };
    }

    public static PyString __doc__repeat = new PyString(
            "'repeat(element [,times]) -> create an iterator which returns the element\n"
                    + "for the specified number of times.  If not specified, returns the element\nendlessly.");

    /**
     * Creates an iterator that returns the same object the number of times given by
     * <code>times</code>.
     */
    public static PyIterator repeat(final PyObject object, final int times) {
        return new PyIterator() {
            int counter = times;

            public PyObject __iternext__() {
                if (counter > 0) {
                    counter--;
                    return object;
                }
                return null;
            }
            
            public int __len__() {
               return times; 
            }
            
            public PyString __repr__() {
                return (PyString)(Py.newString("repeat(%r, %d)").
                        __mod__(new PyTuple(object, Py.newInteger(counter))));
            }
        };
    }

    /**
     * Creates an iterator that returns the same object over and over again.
     */
    public static PyIterator repeat(final PyObject object) {
        return new PyIterator() {
            public PyObject __iternext__() {
                return object;
            }
                        
            public PyString __repr__() {
                return (PyString)(Py.newString("repeat(%r)").
                        __mod__(new PyTuple(object)));
            }
        };
    }

    public static PyString __doc__imap = new PyString(
            "'map(func, *iterables) --> imap object\n\nMake an iterator that computes the "
                    + "function using arguments from\neach of the iterables.\tLike map() except that it returns\n"
                    + "an iterator instead of a list and that it stops when the shortest\niterable is exhausted "
                    + "instead of filling in None for shorter\niterables.");

    /**
     * Works as <code>__builtin__.map()</code> but returns an iterator instead of a list. (Code in
     * this method is based on __builtin__.map()).
     */
    public static PyIterator imap(PyObject[] argstar) {
        final int n = argstar.length - 1;
        if (n < 1) {
            throw Py.TypeError("imap requires at least two arguments");
        }

        final PyObject callable = argstar[0];
        final PyObject[] iters = new PyObject[n];

        for (int j = 0; j < n; j++) {
            iters[j] = Py.iter(argstar[j + 1], "argument " + (j + 1)
                    + " to imap() must support iteration");
        }

        return new PyIterator() {
            PyObject[] args = new PyObject[n];

            PyObject element = null;

            public PyObject __iternext__() {

                for (int i = 0; i < n; i++) {
                    if ((element = iters[i].__iternext__()) != null) {
                        // collect the arguments for the callable
                        args[i] = element;
                    } else {
                        // break iteration
                        return null;
                    }
                }
                if (callable == Py.None) {
                    // if None is supplied as callable we just return what's in
                    // the iterable(s)
                    if (n == 1) {
                        return args[0];
                    } else {
                        return new PyTuple(args.clone());
                    }
                } else {
                    return callable.__call__(args);
                }
            }
        };
    }

    public static PyString __doc__islice = new PyString(
            "islice(iterable, [start,] stop [, step]) --> islice object\n"
                    + "\nReturn an iterator whose next() method returns selected values from an\n"
                    + "iterable.  If start is specified, will skip all preceding elements;\notherwise, start defaults to zero."
                    + "Step defaults to one.  If\nspecified as another value, step determines how manyvalues are \n"
                    + "skipped between successive calls.  Works like a slice() on a list\nbut returns an iterator.");

    
    private static int py2int(PyObject obj, int defaultValue, String msg) {
        if (obj instanceof PyNone) {
            return defaultValue;
        } else {
            int value = defaultValue;
            try {
                value = Py.py2int(obj);
            }
            catch (PyException pyEx) {
                if (pyEx.match(Py.TypeError)) {
                    throw Py.ValueError(msg);
                } else {
                    throw pyEx;
                }
            }
            return value;
        }
    }
    /**
     * Creates an iterator that returns selected values from an iterable.
     * 
     * @param startObj
     *            the index of where in the iterable to start returning values
     * @param stopObj
     *            the index of where in the iterable to stop returning values
     * @param stepObj
     *            the number of steps to take beween each call to <code>next()</code>
     */
    public static PyIterator islice(final PyObject iterable, PyObject startObj,
            PyObject stopObj, PyObject stepObj) {
        final int stop = py2int(stopObj, 0, "Stop argument must be a non-negative integer or None");
        final int start = py2int(startObj, 0, "Start argument must be a non-negative integer or None");
        final int step = py2int(stepObj, 1, "Step argument must be a non-negative integer or None");
        final boolean stopNone = stopObj instanceof PyNone;

        if (start < 0 || step < 0 || stop < 0) {
            throw Py.ValueError("Indices for islice() must be non-negative integers");
        }

        if (step == 0) {
            throw Py.ValueError("Step must be one or larger for islice()");
        }

        return new ItertoolsIterator() {
            int counter = start;

            int lastCount = 0;

            PyObject iter = iterable.__iter__();

            public PyObject __iternext__() {
                PyObject result = null;

                if (counter >= stop && !stopNone) {
                    return null;
                }

                while (lastCount <= counter) {
                    result = nextElement(iter);
                    lastCount++;
                }
                counter += step;
                return result;
            }

        };

    }

    /**
     * @see #islice(PyObject, PyObject, PyObject, PyObject) startObj defaults to 0 and stepObj to 1
     */
    public static PyIterator islice(PyObject iterable, PyObject stopObj) {
        return islice(iterable, new PyInteger(0), stopObj, new PyInteger(1));
    }

    /**
     * @see #islice(PyObject, PyObject, PyObject, PyObject) stepObj defaults to 1
     */
    public static PyIterator islice(PyObject iterable, PyObject start,
            PyObject stopObj) {
        return islice(iterable, start, stopObj, new PyInteger(1));
    }

    /**
     * Iterator base class for iterators returned by <code>ifilter</code> and
     * <code>ifilterfalse</code>.
     */
    static class FilterIterator extends ItertoolsIterator {
        private PyObject predicate;

        private PyObject iterator;

        private boolean filterTrue;

        FilterIterator(PyObject predicate, PyObject iterable, boolean filterTrue) {
            if (predicate instanceof PyNone) {
                this.predicate = null;
            } else {
                this.predicate = predicate;
            }
            this.iterator = iterable.__iter__();
            this.filterTrue = filterTrue;
        }

        public PyObject __iternext__() {

            while (true) {
                PyObject element = nextElement(iterator);
                if (element != null) {
                    // the boolean value of calling predicate with the element
                    // or if predicate is null/None of the element itself
                    boolean booleanValue = predicate != null ? predicate
                            .__call__(element).__nonzero__() : element
                            .__nonzero__();
                    if (booleanValue == filterTrue) {
                        // if the boolean value is the same as filterTrue return
                        // the element
                        // for ifilter filterTrue is always true, for
                        // ifilterfalse always false
                        return element;
                    }
                } else {
                    return null;
                }
            }
        }
    }

    public static PyString __doc__ifilter = new PyString(
            "ifilter(function or None, sequence) --> ifilter object\n\n"
                    + "Return those items of sequence for which function(item) is true.\nIf function is None, "
                    + "return the items that are true.");

    /**
     * Creates an iterator that returns the items of the iterable for which
     * <code>predicate(item)</code> is <code>true</code>. If <code>predicate</code> is null
     * (None) return the items that are true.
     */
    public static PyIterator ifilter(PyObject predicate, PyObject iterable) {
        return new FilterIterator(predicate, iterable, true);
    }

    public static PyString __doc__ifilterfalse = new PyString(
            "'ifilterfalse(function or None, sequence) --> ifilterfalse object\n\n"
                    + "Return those items of sequence for which function(item) is false.\nIf function is None, "
                    + "return the items that are false.'");

    /**
     * Creates an iterator that returns the items of the iterable for which
     * <code>predicate(item)</code> is <code>false</code>. If <code>predicate</code> is null
     * (None) return the items that are false.
     */
    public static PyIterator ifilterfalse(PyObject predicate, PyObject iterable) {
        return new FilterIterator(predicate, iterable, false);
    }

    public static PyString __doc__izip = new PyString(
            "izip(iter1 [,iter2 [...]]) --> izip object\n\nReturn an izip object "
                    + "whose .next() method returns a tuple where\nthe i-th element comes from the i-th iterable argument.  "
                    + "The .next()\nmethod continues until the shortest iterable in the argument sequence\nis exhausted and then it "
                    + "raises StopIteration.  Works like the zip()\nfunction but consumes less memory by returning an iterator "
                    + "instead of\na list.");

    /**
     * Create an iterator whose <code>next()</code> method returns a tuple where the i-th element
     * comes from the i-th iterable argument. Continues until the shortest iterable is exhausted.
     * (Code in this method is based on __builtin__.zip()).
     * 
     */
    public static PyIterator izip(PyObject[] argstar) {
        final int itemsize = argstar.length;
        
        if (itemsize == 0) {
            return (PyIterator)(new PyXRange(0).__iter__());            
        }

        // Type check the arguments; they must be sequences.
        final PyObject[] iters = new PyObject[itemsize];

        for (int i = 0; i < itemsize; i++) {
            PyObject iter = argstar[i].__iter__();
            if (iter == null) {
                throw Py.TypeError("izip argument #" + (i + 1)
                        + " must support iteration");
            }
            iters[i] = iter;
        }

        return new ItertoolsIterator() {

            public PyObject __iternext__() {
                if (itemsize == 0)
                    return null;

                PyObject[] next = new PyObject[itemsize];
                PyObject item;

                for (int i = 0; i < itemsize; i++) {

                    item = nextElement(iters[i]);

                    if (item == null) {
                        return null;
                    }
                    next[i] = item;
                }
                return new PyTuple(next);
            }

        };

    }

    public static PyString __doc__starmap = new PyString(
            "starmap(function, sequence) --> starmap object\n\nReturn an "
                    + "iterator whose values are returned from the function evaluated\nwith an argument tuple taken from the "
                    + "given sequence.");

    /**
     * Create an iterator whose <code>next()</code> method returns the result
     * of calling the function (first argument) with a tuple of arguments
     * returned from the iterable (second argument).
     * 
     * @param starargs
     *            [0] = callable function, [1] = iterable with argument tuples
     */
    public static PyIterator starmap(PyObject[] starargs) {
        if (starargs.length != 2) {
            throw Py.TypeError("starmap requires 2 arguments, got "
                    + starargs.length);
        }
        final PyObject callable = starargs[0];
        final PyObject iterator = starargs[1].__iter__();

        return new ItertoolsIterator() {

            public PyObject __iternext__() {
                PyObject args = nextElement(iterator);
                PyObject result = null;

                if (args != null) {
                    if (!args.getClass().isAssignableFrom(PyTuple.class)) {
                        throw Py.TypeError("iterator must return a tuple");
                    }
                    PyTuple argTuple = (PyTuple) args;
                    // convert to array of PyObjects in call to function
                    result = callable.__call__(argTuple.getArray());
                }
                return result;
            }

        };
    }

    /**
     * Iterator base class used by <code>dropwhile()</code> and <code>takewhile</code>.
     */
    static class WhileIterator extends ItertoolsIterator {
        private PyObject iterator;

        private PyObject predicate;

        // flag that indicates if the iterator shoul drop or return arguments "while" the predicate is true
        private boolean drop;

        // flag that is set once the predicate is satisfied
        private boolean predicateSatisfied;

        WhileIterator(PyObject predicate, PyObject iterable, boolean drop) {
            this.predicate = predicate;
            iterator = iterable.__iter__();
            this.drop = drop;
        }

        public PyObject __iternext__() {

            while (true) {
                PyObject element = nextElement(iterator);
                if (element != null) {
                    if (!predicateSatisfied) {
                        // the predicate is not satisfied yet (or still satisfied in the case of drop beeing 
                        // false), so we need to check it
                        if (predicate.__call__(element).__nonzero__() != drop) {
                            predicateSatisfied = drop;
                            return element;
                        }
                        predicateSatisfied = !drop;
                    } else {
                        if (drop) {
                            return element;
                        } else {
                            // end iteration if predicate is false and drop is false
                            return null;
                        }
                    }
                } else {
                    // end iteration
                    return null;
                }

            }
        }
    }

    public static PyString __doc__dropwhile = new PyString(
            "dropwhile(predicate, iterable) --> dropwhile object\n\nDrop items "
                    + "from the iterable while predicate(item) is true.\nAfterwards, return every element until theiterable is exhausted.");

    /**
     * Create an iterator that drops items from the iterable while <code>prdicate(item)</code>
     * equals true. After which every remaining item of the iterable is returned.
     */
    public static PyIterator dropwhile(PyObject predicate, PyObject iterable) {
        return new WhileIterator(predicate, iterable, true);
    }

    public static PyString __doc__takewhile = new PyString(
            "takewhile(predicate, iterable) --> takewhile object\n\nReturn "
                    + "successive entries from an iterable as long as the \npredicate evaluates to true for each entry.");

    /**
     * Create an iterator that returns items from the iterable while <code>predicate(item)</code>
     * is true. After which iteration is stopped.
     */
    public static PyIterator takewhile(PyObject predicate, PyObject iterable) {
        return new WhileIterator(predicate, iterable, false);
    }

    private final static class GroupBy extends ItertoolsIterator {

        private final PyObject iterator;
        private final PyObject keyFunc;
        private PyObject currentKey;
        private PyObject currentValue;
        private PyObject targetKey;

        private GroupBy(PyObject iterable, PyObject key) {
            iterator = iterable.__iter__();
            keyFunc = key;
            targetKey = currentKey = currentValue = new PyXRange(0);
        }

        public PyObject __iternext__() {
            while (currentKey.equals(targetKey)) {
                currentValue = nextElement(iterator);
                if (currentValue == null) {
                    return null;
                }
                if (keyFunc == null) {
                    currentKey = currentValue;
                } else {
                    currentKey = keyFunc.__call__(currentValue);
                }
            }
            targetKey = currentKey;
            return new PyTuple(currentKey, new GroupByIterator());
        }

        private class GroupByIterator extends ItertoolsIterator {

            private boolean completed = false;

            public PyObject __iternext__() {
                final PyObject item = currentValue;
                if (completed) {
                    return null;
                }
                currentValue = nextElement(iterator);
                if (currentValue == null) {
                    completed = true;
                } else {
                    if (keyFunc == null) {
                        currentKey = currentValue;
                    } else {
                        currentKey = keyFunc.__call__(currentValue);
                    }
                }
                if (!currentKey.equals(targetKey)) {
                    completed = true;
                }
                return item;
            }
        }
    }

    public static PyString __doc__groupby = new PyString(
            "groupby(iterable[, keyfunc]) -> create an iterator which returns\n" +
            "(key, sub-iterator) grouped by each value of key(value).");

    /**
     * Create an iterator which returns the pair (key, sub-iterator) grouped by key(value).
     */
    public static PyIterator groupby(PyObject [] args, String [] kws) {
        ArgParser ap = new ArgParser("groupby", args, kws, "iterable", "key");
        if(args.length > 2){
            throw Py.TypeError("groupby takes two arguments, iterable and key");
        }
        PyObject iterable = ap.getPyObject(0);
        PyObject key = ap.getPyObject(1, null);
        return new GroupBy(iterable, key);
    }
    
    public static PyString __doc__tee = new PyString(
            "tee(iterable, n=2) --> tuple of n independent iterators.");

    /**
     * Create a tuple of iterators, each of which is effectively a copy of iterable.
     */
    public static PyTuple tee(PyObject iterable, final int n) {
        return new PyTuple(PyTeeIterator.makeTees(iterable, n));
    }

    /**
     * Create a pair of iterators, each of which is effectively a copy of iterable.
     */
    public static PyTuple tee(PyObject iterable) {
        return tee(iterable, 2);
    }

//chain.from_iterable(iterable)

    private static PyTuple makeIndexedTuple(PyTuple pool, int indices[]) {
        return makeIndexedTuple(pool, indices, indices.length);
    }
    
    private static PyTuple makeIndexedTuple(PyTuple pool, int indices[], int end) {
        PyObject items[] = new PyObject[end];
        for (int i = 0; i < end; i++) {
            items[i] = pool.__getitem__(indices[i]);
        }
        return new PyTuple(items);
    }
    
    public static PyIterator combinations(PyObject iterable, final int r) {
        if (r < 0) throw Py.ValueError("r must be non-negative");
        final PyTuple pool = PyTuple.fromIterable(iterable);
        final int n = pool.__len__();
        final int indices[] = new int[r];
        for (int i = 0; i < r; i++) {
            indices[i] = i;
        }

        return new ItertoolsIterator() {
            boolean firstthru = true;

            @Override
            public PyObject __iternext__() {
                if (r > n) { return null; }
                if (firstthru) {
                    firstthru = false;
                    return makeIndexedTuple(pool, indices);
                }
                int i;
                for (i = r-1; i >= 0 && indices[i] == i+n-r ; i--);
                if (i < 0) return null;
                indices[i]++;
                for (int j = i+1; j < r; j++) {
                    indices[j] = indices[j-1] + 1;
                }
                return makeIndexedTuple(pool, indices);
            }
            

        };
    }

    public static PyIterator combinations_with_replacement(PyObject iterable, final int r) {
        final PyTuple pool = PyTuple.fromIterable(iterable);
        final int n = pool.__len__();
        final int indices[] = new int[r];
        for (int i = 0; i < r; i++) {
            indices[i] = 0;
        }

        return new ItertoolsIterator() {
            boolean firstthru = true;

            @Override
            public PyObject __iternext__() {
                if (n == 0 || r == 0) {
                    return null;
                }
                if (firstthru) {
                    firstthru = false;
                    return makeIndexedTuple(pool, indices);
                }
                int i;
                for (i= r - 1 ; i >= 0 && indices[i] == n - 1; i--);
                if (i < 0) return null;
                indices[i]++;
                for (int j = i + 1; j < r; j++) {
                    indices[j] = indices[j-1];
                }
                return makeIndexedTuple(pool, indices);
            }
        };
    }

    public static PyString __doc__compress = new PyString(
        "compress(data, selectors) --> iterator over selected data\n\n" +
        "Return data elements corresponding to true selector elements.\n" +
        "Forms a shorter iterator from selected data elements using the\n" +
        "selectors to choose the data elements.");
    
    public static PyIterator compress(PyObject [] args, String [] kws) {
        ArgParser ap = new ArgParser("compress", args, kws, "data", "selectors");
        if (args.length > 2) {
            throw Py.TypeError(String.format("compress() takes at most 2 arguments (%s given)", args.length));
        }
        final PyObject data = ap.getPyObject(0).__iter__();
        final PyObject selectors = ap.getPyObject(1, null).__iter__();

        return new ItertoolsIterator() {

            @Override
            public PyObject __iternext__() {
                while (true) {
                    PyObject datum = nextElement(data);
                    if (datum == null) { return null; }
                    PyObject selector = nextElement(selectors);
                    if (selector == null) { return null; }
                    if (selector.__nonzero__()) {
                        return datum;
                        }
                }
            }
            public PyString __repr__() {
                return new PyString(String.format("itertools.compress object at 0x%x", Py.id(this)));
            }

        };
    }

    public static PyIterator izip_longest(PyObject[] args, String[] kws) {
        final int num_iterables;
        final PyObject fillvalue;
        if (kws.length == 1 && kws[0] == "fillvalue") {
            fillvalue = args[args.length - 1];
            num_iterables = args.length - 1;
        } else {
            fillvalue = Py.None;
            num_iterables = args.length;
        }

        //XXX error checking on args
        final PyObject iterators[] = new PyObject[num_iterables];
        final boolean exhausted[] = new boolean[num_iterables];
        for (int i = 0; i < num_iterables; i++) {
            iterators[i] = args[i].__iter__();
            exhausted[i] = false;
        }

        return new ItertoolsIterator() {
            int unexhausted = num_iterables;

            @Override
            public PyObject __iternext__() {
                PyObject item[] = new PyObject[num_iterables];
                for (int i = 0; i < num_iterables; i++) {
                    if (exhausted[i]) {
                        item[i] = fillvalue;
                    } else {
                        PyObject elem = iterators[i].__iternext__();
                        if (elem == null) {
                            unexhausted--;
                            exhausted[i] = true;
                            item[i] = fillvalue;
                        } else {
                            item[i] = elem;
                        }
                    }
                }
                if (unexhausted == 0) {
                    return null;
                } else {
                    return new PyTuple(item);
                }
            }
        };
    }

    public static PyIterator permutations(PyObject iterable, final int r) {
        //XXX keyword args support
        if (r < 0) throw Py.ValueError("r must be non-negative");
        final PyTuple pool = PyTuple.fromIterable(iterable);
        final int n = pool.__len__();
        final int indices[] = new int[n];
        for (int i = 0; i < n; i++) {
            indices[i] = i;
        }
        final int cycles[] = new int[r];
        for (int i = 0; i < r; i++) {
            cycles[i] = n - i;
        }

        return new ItertoolsIterator() {
            boolean firstthru = true;

            @Override
            public PyObject __iternext__() {
                if (r > n) return null;
                if (firstthru) {
                    firstthru = false;
                    return makeIndexedTuple(pool, indices, r);
                }
                for (int i = r - 1; i >= 0; i--) {
                    cycles[i] -= 1;
                    if (cycles[i] == 0) {
                        // rotate indices at the ith position
                        int first = indices[i];
                        for (int j = i; j < n - 1; j++) {
                            indices[j] = indices[j + 1];
                        }
                        indices[n - 1] = first;
                        cycles[i] = n - i;
                    } else {
                        int j = cycles[i];
                        int index = indices[i];
                        indices[i] = indices[n - j];
                        indices[n - j] = index;
                        return makeIndexedTuple(pool, indices, r);
                    }
                }
                return null;
            }
        };
    }

    public static PyIterator product(PyObject [] args, String [] kws) {
        final int repeat;
        final int num_iterables;
        if (kws.length == 1 && kws[0] == "repeat") {
            repeat = args[args.length -1].asInt();
            num_iterables = args.length - 1;
        } else {
            repeat = 1;
            num_iterables = args.length;
        }
        // XXX error checking on args! XXX
        final int num_pools = num_iterables * repeat;
        final PyTuple pools[] = new PyTuple[num_pools];
        for (int i = 0; i < num_iterables; i++) {
            pools[i] = PyTuple.fromIterable(args[i]);
        }
        // Make repeat - 1 duplicates, in order
        for (int r = 1; r < repeat; r++) {
            for (int i = 0; i < num_iterables; i++) {
                pools[r * num_iterables + i] = pools[i];
            }
        }
        final int indices[] = new int[num_pools];
        for (int i = 0; i < num_pools; i++) {
            indices[i] = 0;
        }

        return new ItertoolsIterator() {
            boolean firstthru = true;

            @Override
            public PyObject __iternext__() {
                if (firstthru) {
                    firstthru = false;
                    return makeTuple();
                }
                for (int i = num_pools - 1; i >= 0; i--) {
                    indices[i]++;

                    if (indices[i] == pools[i].__len__()) {
                        indices[i] = 0;
                    } else {
                        return makeTuple();         
                    }
                }
                return null;
            }

            private PyTuple makeTuple() {
                PyObject items[] = new PyObject[num_pools];
                for (int i = 0; i < num_pools; i++) {
                    items[i] = pools[i].__getitem__(indices[i]);
                }
                return new PyTuple(items);
            }

        };
    }

}
