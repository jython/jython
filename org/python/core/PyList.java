// Copyright (c) Corporation for National Research Initiatives

// Implementation of the standard Python list objects

package org.python.core;
import java.util.Vector;



class ListFunctions extends PyBuiltinFunctionSet
{
    ListFunctions(String name, int index, int argcount) {
        super(name, index, argcount, argcount, true, null);
    }

    ListFunctions(String name, int index, int minargs, int maxargs) {
        super(name, index, minargs, maxargs, true, null);
    }

    public PyObject __call__() {
        PyList list = (PyList)__self__;
        switch (index) {
        case 1:
            list.reverse();
            return Py.None;
        case 2:
            list.sort(null);
            return Py.None;
        case 3:
            return new PyInteger(list.__len__());
        default:
            throw argCountError(0);
        }
    }

    public PyObject __call__(PyObject arg) {
        PyList list = (PyList)__self__;
        switch (index) {
        case 2:
            list.sort(arg);
            return Py.None;
        case 10:
            list.append(arg);
            return Py.None;
        case 11:
            return new PyInteger(list.count(arg));
        case 12:
            return new PyInteger(list.index(arg));
        case 13:
            list.remove(arg);
            return Py.None;
        case 14:
            list.extend(arg);
            return Py.None;
        case 15:
            return list.__add__(arg);
        default:
            throw argCountError(1);
        }
    }

    public PyObject __call__(PyObject arg1, PyObject arg2) {
        PyList list = (PyList)__self__;
        switch (index) {
        case 20:
            if (!(arg1 instanceof PyInteger)) {
                throw Py.TypeError(
                    "illegal argument type for built-in operation");
            }
            int index = ((PyInteger)arg1).getValue();
            list.insert(index, arg2);
            return Py.None;
        default:
            throw argCountError(2);
        }
    }
}



/**
 * A builtin python list.
 */

public class PyList extends PySequence implements ClassDictInit
{
    protected PyObject[] list;
    protected int length;
    protected static PyObject __methods__;

    static {
        PyList list = new PyList();
        String[] methods = {
            "append", "count", "extend", "index", "insert", "pop",
            "remove", "reverse", "sort"};
        for (int i = 0; i < methods.length; i++)
            list.append(new PyString(methods[i]));
        __methods__ = list;
    }

    /** <i>Internal use only. Do not call this method explicit.</i> */
    public static void classDictInit(PyObject dict) {
        PySequence.classDictInit(dict);
        dict.__setitem__("reverse", new ListFunctions("reverse", 1, 0));
        dict.__setitem__("sort", new ListFunctions("sort", 2, 0, 1));
        dict.__setitem__("__len__", new ListFunctions("__len__", 3, 0));
        dict.__setitem__("append", new ListFunctions("append", 10, 1));
        dict.__setitem__("count", new ListFunctions("count", 11, 1));
        dict.__setitem__("index", new ListFunctions("index", 12, 1));
        dict.__setitem__("remove", new ListFunctions("remove", 13, 1));
        dict.__setitem__("extend", new ListFunctions("extend", 14, 1));
        dict.__setitem__("__add__", new ListFunctions("__add__", 15, 1));
        dict.__setitem__("insert", new ListFunctions("insert", 20, 2));
        // hide these from Python!
        dict.__setitem__("initModule", null);
        dict.__setitem__("toString", null);
        dict.__setitem__("hashCode", null);
    }

    public PyList() {
        this(Py.EmptyObjects);
    }

    public PyList(Vector ilist) {
        this(new PyObject[ilist.size()]);
        for (int i=0; i<ilist.size(); i++) {
            list[i] = (PyObject)ilist.elementAt(i);
        }
    }

    public PyList(PyObject elements[]) {
        list = elements;
        length = elements.length;
    }

    public String safeRepr() throws PyIgnoreMethodTag {
        return "'list' object";
    }

    public int __len__() {
        return length;
    }

    public PyObject __findattr__(String name) {
        if (name.equals("__methods__")) {
            PyList mlist = (PyList)__methods__;
            PyString methods[] = new PyString[mlist.length];
            for (int i = 0; i < mlist.length; i++)
                methods[i] = (PyString)mlist.list[i];
            return new PyList(methods);
        }
        return super.__findattr__(name);
    }

    protected PyObject get(int i) {
        return list[i];
    }

    protected PyObject getslice(int start, int stop, int step) {
        if (step > 0 && stop < start)
            stop = start;
        int n = sliceLength(start, stop, step);
        PyObject[] newList = new PyObject[n];

        if (step == 1) {
            System.arraycopy(list, start, newList, 0, stop-start);
            return new PyList(newList);
        }
        int j = 0;
        for (int i=start; j<n; i+=step) {
            newList[j] = list[i];
            j++;
        }
        return new PyList(newList);
    }

    protected void del(int i) {
        length = length-1;
        System.arraycopy(list, i+1, list, i, length-i);
        list[length] = null;
    }

    protected void delRange(int start, int stop, int step) {
        if (step != 1)
            throw Py.ValueError("step size must be 1 for deleting "+
                                "list slice");

        System.arraycopy(list, stop, list, start, length-stop);
        int newLength = length-(stop-start);
        int oldLength = length;

        for(int i = newLength; i < oldLength; i++)
            list[i] = null;
        length = newLength;
    }

    protected void set(int i, PyObject value) {
        list[i] = value;
    }

    protected void setslice(int start, int stop, int step, PyObject value) {
        if (!(value instanceof PySequence))
            throw Py.TypeError("rhs of setslice must be a sequence");

        if (step != 1)
            throw Py.ValueError("step size must be 1 for setting list slice");
        if (stop < start)
            stop = start;

        PySequence seq = (PySequence)value;

        // Hack to make recursive setslices work correctly.
        // Could avoid the copy if we wanted to be more clever about
        // the code to do the moving

        int n = seq.__len__();
        int i;
        int length = this.length;
        int newLength = length-(stop-start)+n;

        if (newLength > length || newLength < length) {
            resize(newLength);
            System.arraycopy(list, stop, list, stop+(newLength-length),
                             length-stop);
            if (newLength < length) {
                for (i = newLength; i < length; i++)
                    list[i] = null;
            }
        }
//         else if (newLength < length) {
//             System.arraycopy(list, stop, list, stop+(newLength-length),
//                              length-stop);
//             this.length = newLength;
//         }

        PyObject[] otherList = null;

        if (value instanceof PyTuple)
            otherList = ((PyTuple)value).list;
        if (value instanceof PyList) {
            otherList = ((PyList)value).list;
            if (otherList == list)
                otherList = (PyObject[])otherList.clone();
        }
        if (otherList != null) {
            System.arraycopy(otherList, 0, list, start, n);
        }
        else {
            for(i=0; i<n; i++) {
                list[i+start] = seq.get(i);
            }
        }
    }

    protected PyObject repeat(int count) {
        int l = length;
        PyObject[] newList = new PyObject[l*count];
        for (int i=0; i<count; i++) {
            System.arraycopy(list, 0, newList, i*l, l);
        }
        return new PyList(newList);
    }

    public PyObject __imul__(PyObject o) {
        if (!(o instanceof PyInteger || o instanceof PyLong))
            throw Py.TypeError("can't multiply sequence to non-int");
        int l = length;
        int count = o.__int__().getValue();

        resize(l * count);

        for (int i=0; i<count; i++) {
            System.arraycopy(list, 0, list, i*l, l);
        }
        return this;
    }

    public PyObject __add__(PyObject genericOther) {
        if (genericOther instanceof PyList) {
            PyList other = (PyList)genericOther;

            PyObject[] newList = new PyObject[length+other.length];
            System.arraycopy(list, 0, newList, 0, length);
            System.arraycopy(other.list, 0, newList, length, other.length);

            return new PyList(newList);
        } else {
            return null;
        }
    }

    public String toString() {
        ThreadState ts = Py.getThreadState();
        if (!ts.enterRepr(this)) {
            return "[...]";
        }

        StringBuffer buf = new StringBuffer("[");
        for (int i=0; i<length-1; i++) {
            buf.append(((PyObject)list[i]).__repr__().toString());
            buf.append(", ");
        }
        if (length > 0)
            buf.append(((PyObject)list[length-1]).__repr__().toString());
        buf.append("]");

        ts.exitRepr(this);
        return buf.toString();
    }

    protected void resize(int n) {
        if (list.length < n) {
            PyObject[] newList = new PyObject[(int)(n*1.5)];
            System.arraycopy(list, 0, newList, 0, length);
            list = newList;
        }
        length = n;
    }

    /**
     * Add a single element to the end of list.
     *
     * @param o the element to add.
     */
    public void append(PyObject o) {
        resize(length+1);
        list[length-1] = o;
    }

    /**
     * Return the number elements in the list that equals the argument.
     *
     * @param o the argument to test for. Testing is done with
     *          the <code>==</code> operator.
     */
    public int count(PyObject o) {
        int count = 0;
        int n = length;
        PyObject[] list = this.list;
        for (int i=0; i<n; i++) {
            if (list[i].equals(o))
                count++;
        }
        return count;
    }

    /**
     * return smallest index where an element in the list equals
     * the argument.
     *
     * @param o the argument to test for. Testing is done with
     *          the <code>==</code> operator.
     */
    public int index(PyObject o) {
        return _index(o, "list.index(x): x not in list");
    }

    private int _index(PyObject o, String message) {
        int n = length;
        PyObject[] list = this.list;
        int i=0;
        for (; i<n; i++) {
            if (list[i].equals(o))
                break;
        }
        if (i == n)
            throw Py.ValueError(message);
        return i;
    }

    /**
     * Insert the argument element into the list at the specified 
     * index.
     * <br>
     * Same as <code>s[index:index] = [o] if index &gt;= 0</code>.
     *
     * @param index the position where the element will be inserted.
     * @param o     the element to insert.
     */
    public void insert(int index, PyObject o) {
        if (index < 0)
            index = 0;
        if (index > length)
            index = length;
        resize(length+1);
        System.arraycopy(list, index, list, index+1, length-index-1);
        list[index] = o;
    }

    /**
     * Remove the first occurence of the argument from the list.
     * The elements arecompared with the <code>==</code> operator.
     * <br>
     * Same as <code>del s[s.index(x)]</code>
     *
     * @param o     the element to search for and remove.
     */
    public void remove(PyObject o) {
        del(_index(o, "list.remove(x): x not in list"));
    }

    /**
     * Reverses the items of s in place.
     * The reverse() methods modify the list in place for economy 
     * of space when reversing a large list. It doesn't return the
     * reversed list to remind you of this side effect.
     */
    public void reverse() {
        PyObject tmp;
        int n = length;
        PyObject[] list = this.list;
        int j = n-1;
        for (int i=0; i<n/2; i++, j--) {
            tmp = list[i];
            list[i] = list[j];
            list[j] = tmp;
        }
    }

    /**
     * Removes and return the last element in the list.
     */
    public PyObject pop() {
        return pop(-1);
    }

    /**
     * Removes and return the <code>n</code> indexed element in the
     * list.
     *
     * @param n the index of the element to remove and return.
     */
    public PyObject pop(int n) {
        if (length==0) {
            throw Py.IndexError("pop from empty list");
        }
        if (n < 0)
            n += length;
        if (n < 0 || n >= length)
            throw Py.IndexError("pop index out of range");
        PyObject v = list[n];

        setslice(n, n+1, 1, Py.EmptyTuple);
        return v;
    }


    /**
     * Append the elements in the argument sequence to the end of the list.
     * <br>
     * Same as <code>s[len(s):len(s)] = o</code>.
     *
     * @param o the sequence of items to append to the list.
     */
    public void extend(PyObject o) {
        setslice(length, length, 1, o);
    }

    public PyObject __iadd__(PyObject o) {
        extend(fastSequence(o, "argument to += must be a sequence"));
        return this;
    }

    /* Implementation is taken from Python 1.5 as written by Guido van
     * Rossum Port to Java is by Jim Hugunin.  This function will almost
     * certainly go away with the builtin sorting provided by JDK 1.2
     */

    /* New quicksort implementation for arrays of object pointers.  Thanks
     * to discussions with Tim Peters.
     */

    /* Comparison function.  Takes care of calling a user-supplied
     * comparison function (any callable Python object).  Calls the
     * standard comparison function, cmpobject(), if the user-supplied
     * function is NULL.
     */
    private static int docompare(PyObject x, PyObject y,
                                 PyObject compare, String cmpop) {
        if (compare == null) {
            /* NOTE: we rely on the fact here that the sorting algorithm
               only ever checks whether k<0, i.e., whether x<y.  So we
               invoke the rich comparison function with _lt ('<'), and
               return -1 when it returns true and 0 when it returns
               false. */
            if (cmpop == "<")
                return x._lt(y).__nonzero__() ? -1 : 0;
            if (cmpop == "<=")
                return x._le(y).__nonzero__() ? -1 : 1;
        }

        PyObject ret = compare.__call__(new PyObject[] {x, y});

        if (ret instanceof PyInteger) {
            int v = ((PyInteger)ret).getValue();
            return v < 0 ? -1 : v > 0 ? 1 : 0;
        }
        throw Py.TypeError("comparision function must return int");
    }

    /* Straight insertion sort.  More efficient for sorting small arrays. */
    private static void insertionsort(PyObject[] array, int off, int size,
                                      PyObject compare)
    {
        int end = off+size;
        for (int i=off+1; i<end; i++) {
            PyObject key = array[i];
            int j = i;
            while (--j >= off) {
                PyObject q = array[j];
                if (docompare(q, key, compare, "<=") <= 0)
                    break;
                array[j+1] = q;
                array[j] = key;
            }
        }
    }

    /* MINSIZE is the smallest array we care to partition; smaller arrays
     * are sorted using a straight insertion sort (above).  It must be at
     * least 2 for the quicksort implementation to work.  Assuming that
     * comparisons are more expensive than everything else (and this is a
     * good assumption for Python), it should be 10, which is the cutoff
     * point: quicksort requires more comparisons than insertion sort for
     * smaller arrays.
     */
    private static final int MINSIZE = 10;

    /* STACKSIZE is the size of our work stack.  A rough estimate is that
     * this allows us to sort arrays of MINSIZE * 2**STACKSIZE, or large
     * enough.  (Because of the way we push the biggest partition first,
     * the worst case occurs when all subarrays are always partitioned
     * exactly in two.)
     */
    private static final int STACKSIZE = 64;

    /* Quicksort algorithm.  If an exception occurred; in this
     * case we leave the array partly sorted but otherwise in good health
     * (i.e. no items have been removed or duplicated).
     */
    private static void quicksort(PyObject[] array, int off, int size,
                                  PyObject compare)
    {
        PyObject tmp, pivot, left, right;
        int lo, hi, l, r;
        int top, k, n, n2;
        int[] lostack = new int[STACKSIZE];
        int[] histack = new int[STACKSIZE];

        /* Start out with the whole array on the work stack */
        lostack[0] = off;
        histack[0] = off+size;
        top = 1;

        /* Repeat until the work stack is empty */
        while (--top >= 0) {
            lo = lostack[top];
            hi = histack[top];

            /* If it's a small one, use straight insertion sort */
            n = hi - lo;
            if (n < MINSIZE) {
                /*
                 * skip it.  The insertion sort at the end will catch these
                 */
                continue;
            }

            /* Choose median of first, middle and last item as pivot */
            l = lo + (n>>1);                 /* Middle */
            r = hi - 1;                      /* Last */

            left = array[l]; right = array[lo];
            if (docompare(left, right, compare, "<") < 0) {
                array[lo] = left; array[l] = right;
            }

            left = array[r]; right = array[l];
            if (docompare(left, right, compare, "<") < 0) {
                array[r] = left; array[l] = right;
            }

            left = array[l]; right = array[lo];
            if (docompare(left, right, compare, "<") < 0) {
                array[lo] = left; array[l] = right;
            }
            pivot = array[l];

            /* Partition the array */
            l = lo+1;
            r = hi-2;
            for (;;) {
                /* Move left index to element > pivot */
                while (l < hi) {
                    if (docompare(array[l], pivot, compare, "<") >= 0)
                        break;
                    l++;
                }
                /* Move right index to element < pivot */
                while (r >= lo) {
                    if (docompare(pivot, array[r], compare, "<") >= 0)
                        break;
                    r--;
                }

                /* If they met, we're through */
                if (l < r) {
                    /* Swap elements and continue */
                    tmp = array[l]; array[l] = array[r]; array[r] = tmp;
                    l++; r--;
                }
                else if (l == r) {
                    l++; r--;
                    break;
                }

                if (l > r)
                    break;
            }

            /* We have now reached the following conditions:
               lo <= r < l <= hi
               all x in [lo,r) are <= pivot
               all x in [r,l)  are == pivot
               all x in [l,hi) are >= pivot
               The partitions are [lo,r) and [l,hi)
            */

            /* Push biggest partition first */
            n = r - lo;
            n2 = hi - l;
            if (n > n2) {
                /* First one is bigger */
                if (n > MINSIZE) {
                    lostack[top] = lo;
                    histack[top++] = r;
                    if (n2 > MINSIZE) {
                        lostack[top] = l;
                        histack[top++] = hi;
                    }
                }
            } else {
                /* Second one is bigger */
                if (n2 > MINSIZE) {
                    lostack[top] = l;
                    histack[top++] = hi;
                    if (n > MINSIZE) {
                        lostack[top] = lo;
                        histack[top++] = r;
                    }
                }
            }
            /* Should assert top < STACKSIZE-1 */
        }

        /* Ouch - even if I screwed up the quicksort above, the
         * insertionsort below will cover up the problem - just a
         * performance hit would be noticable.
         */

        /* insertionsort is pretty fast on the partially sorted list */
        insertionsort(array, off, size, compare);
    }

    /**
     * Sort the items of the list in place. The compare argument is a
     * function of two arguments (list items) which should return
     * -1, 0 or 1 depending on whether the first argument is 
     * considered smaller than, equal to, or larger than the second 
     * argument. Note that this slows the sorting process down
     * considerably; e.g. to sort a list in reverse order it is much
     * faster to use calls to the methods sort() and reverse() than 
     * to use the built-in function sort() with a comparison function
     * that reverses the ordering of the elements. 
     *
     * @param compare the comparison function.
     */
    public synchronized void sort(PyObject compare) {
        quicksort(list, 0, length, compare);
    }

    /**
     * Sort the items of the list in place. Items is compared with the
     * normal relative comparison operators.
     */
    public void sort() {
        sort(null);
    }

    // __class__ boilerplate -- see PyObject for details
    public static PyClass __class__;

    protected PyClass getPyClass() {
        return __class__;
    }

    public int hashCode() {
        throw Py.TypeError("unhashable type");
    }
}
