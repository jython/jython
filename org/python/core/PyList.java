package org.python.core;
import java.util.Vector;
/* TODO: Change to implementation based on PyObject[] */
public class PyList extends PySequence {
	public Vector list;

	public PyList() {
		this(new Vector());
	}

    public static PyClass __class__;
	public PyList(Vector ilist) {
	    super(__class__);
		list = ilist;
	}

	public PyList(PyObject elements[]) {
		this(new Vector(elements.length));
		for(int i=0; i<elements.length; i++) {
			list.addElement(elements[i]);
		}
	}

	public int __len__() { 
	    return list.size(); 
	}

	protected PyObject get(int i) {
		return (PyObject)list.elementAt(i);
	}

	protected PyObject getslice(int start, int stop, int step) {
		int n = sliceLength(start, stop, step);
		Vector new_list = new java.util.Vector(n);
		int j = 0;
		for(int i=start; j<n; i+=step) {
			new_list.addElement(list.elementAt(i));
			j++;
		}
		return new PyList(new_list);
	}

	protected void del(int i) {
		list.removeElementAt(i);
	}

	protected void delRange(int start, int stop, int step) {
		int n = sliceLength(start, stop, step);
		int j = 0;
		for(int i=start; j<n; i+=step-1) {
			list.removeElementAt(i);
			j++;
		}
	}

	protected void set(int i, PyObject value) {
		list.setElementAt(value, i);
	}

	protected void setslice(int start, int stop, int step, PyObject value) {
		if (!(value instanceof PySequence))
		    throw Py.TypeError("rhs of setslice must be a sequence");

		if (step != 1)
		    throw Py.ValueError("step size must be 1 for setting list slice");
		PySequence seq = (PySequence)value;
		
		// Hack to make recursive setslices work correctly.
		// Could avoid the copy if we wanted to be more clever about
		// the code to do the moving
		if (seq == this) {
		    seq = new PyList((Vector)this.list.clone());
		}

		int n = seq.__len__();
		int i;
		int size = __len__();
		int newSize = size-(stop-start)+n;


		if (newSize > size) {
		    list.setSize(newSize);
		    int offset = newSize-size;
		    for(i=size-1; i>=stop; i--) {
		        list.setElementAt(list.elementAt(i), i+offset);
		    }
		} else {
		    if (newSize < size) {
    		    int offset = newSize-size;
    		    for(i=stop; i<size; i++) {
    		        list.setElementAt(list.elementAt(i), i+offset);
    		    }
    		}
    		list.setSize(newSize);
    	}

	    for(i=0; i<n; i++) {
	        list.setElementAt(seq.get(i), i+start);
	    }
	}

	protected PyObject repeat(int count) {
		int l = __len__();
		java.util.Vector new_list = new java.util.Vector(l*count);
		for(int i=0; i<count; i++) {
			for(int j=0; j<l; j++) {
				new_list.addElement(list.elementAt(j));
			}
		}
		return new PyList(new_list);
	}

	public PyObject __add__(PyObject generic_other) {
		if (generic_other instanceof PyList) {
			PyList other = (PyList)generic_other;
			java.util.Vector new_list = new java.util.Vector(__len__()+other.__len__());
			int l = __len__();
			for(int i=0; i<l; i++) new_list.addElement(list.elementAt(i));
			l = other.__len__();
			for(int i=0; i<l; i++) new_list.addElement(other.list.elementAt(i));
			return new PyList(new_list);
		} else {
			return null;
		}
	}

	public PyString __repr__() {
		StringBuffer buf = new StringBuffer("[");
		for(int i=0; i<__len__()-1; i++) {
			buf.append(((PyObject)list.elementAt(i)).__repr__().toString());
			buf.append(", ");
		}
		if (__len__() > 0) buf.append(((PyObject)list.elementAt(__len__()-1)).__repr__().toString());
		buf.append("]");

		return new PyString(buf.toString());
	}

	public void append(PyObject o) {
		list.addElement(o);
	}

	public int count(PyObject o) {
		int n=0;
		for(int i=0; i<__len__(); i++) {
			if (list.elementAt(i).equals(o)) n++;
		}
		return n;
	}

	public int index(PyObject o) {
		int i = list.indexOf(o);
		if (i == -1) throw Py.ValueError("item not found in list");
		return i;
	}

	public void insert(int index, PyObject o) {
		list.insertElementAt(o, index);
	}

	public void remove(PyObject o) {
		del(index(o));
	}

	public void reverse() {
		Object tmp;
		int n = __len__();
		for(int i=0; i<n/2; i++) {
			tmp = list.elementAt(i);
			list.setElementAt(list.elementAt(n-1-i), i);
			list.setElementAt(tmp, n-1-i);
		}
	}


/* Implementation is taken from Python 1.5 as written by Guido van Rossum
   Port to Java is by Jim Hugunin
   This function will almost certainly go away with the builtin sorting
   provided by JDK 1.2 */


/* New quicksort implementation for arrays of object pointers.
   Thanks to discussions with Tim Peters. */


    /* Comparison function.  Takes care of calling a user-supplied
       comparison function (any callable Python object).  Calls the
       standard comparison function, cmpobject(), if the user-supplied
       function is NULL. */

    private static int docompare(PyObject x, PyObject y, PyObject compare) {
        if (compare == null) return x._cmp(y);

        PyObject ret = compare.__call__(new PyObject[] {x, y});

        if (ret instanceof PyInteger) {
			int v = ((PyInteger)ret).getValue();
			return v < 0 ? -1 : v > 0 ? 1 : 0;
		}
		throw Py.TypeError("comparision function must return int");
	}

    /* Straight insertion sort.  More efficient for sorting small arrays. */
    private static void insertionsort(PyObject[] array, int off, int size, PyObject compare) {
        int end = off+size;
        for(int i=off+1; i<end; i++) {
            PyObject key = array[i];
            int j = i;
            while (--j >= off) {
                PyObject q = array[j];
                if (docompare(q, key, compare) <= 0) break;
                array[j+1] = q;
                array[j] = key;
            }
        }
    }

    /* MINSIZE is the smallest array we care to partition; smaller arrays
       are sorted using a straight insertion sort (above).  It must be at
       least 2 for the quicksort implementation to work.  Assuming that
       comparisons are more expensive than everything else (and this is a
       good assumption for Python), it should be 10, which is the cutoff
       point: quicksort requires more comparisons than insertion sort for
       smaller arrays. */
    private static final int MINSIZE = 10;

    /* STACKSIZE is the size of our work stack.  A rough estimate is that
       this allows us to sort arrays of MINSIZE * 2**STACKSIZE, or large
       enough.  (Because of the way we push the biggest partition first,
       the worst case occurs when all subarrays are always partitioned
       exactly in two.) */
    private static final int STACKSIZE = 64;

    /* Quicksort algorithm.  If an exception occurred; in this
       case we leave the array partly sorted but otherwise in good health
       (i.e. no items have been removed or duplicated). */

    private static void quicksort(PyObject[] array, int off, int size, PyObject compare) {
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
    		         * skip it.  The insertion sort at the end will
    			 * catch these
    			 */
    			continue;
    		}

    		/* Choose median of first, middle and last item as pivot */

    		l = lo + (n>>1); /* Middle */
    		r = hi - 1;	/* Last */

            left = array[l]; right = array[lo];
    		if (docompare(left, right, compare) < 0)
    			{ array[lo] = left; array[l] = right; }

            left = array[r]; right = array[l];
    		if (docompare(left, right, compare) < 0)
    			{ array[r] = left; array[l] = right; }

            left = array[l]; right = array[lo];
    		if (docompare(left, right, compare) < 0)
    			{ array[lo] = left; array[l] = right; }
    		pivot = array[l];

    		/* Partition the array */
    		l = lo+1;
    		r = hi-2;
    		for (;;) {
    			/* Move left index to element > pivot */
    			while (l < hi) {
    				if (docompare(array[l], pivot, compare) >= 0)
    					break;
    				l++;
    			}
    			/* Move right index to element < pivot */
    			while (r >= lo) {
    				if (docompare(pivot, array[r], compare) >= 0)
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

    	/*
    	 * Ouch - even if I screwed up the quicksort above, the
    	 * insertionsort below will cover up the problem - just a
    	 * performance hit would be noticable.
    	 */

    	/* insertionsort is pretty fast on the partially sorted list */

    	insertionsort(array, off, size, compare);
    }

    // Future PyLists will probably have their own PyObject[]
    // For now, we must copy one out of and back into the vector
	public void sort(PyObject compare) {
	    int n = __len__();
	    PyObject[] array = new PyObject[n];
	    int i;
	    for(i=0; i<n; i++)
	        array[i] = (PyObject)list.elementAt(i);

	    quicksort(array, 0, n, compare);

	    for(i=0; i<n; i++)
	        list.setElementAt(array[i], i);
	}

	public void sort() {
	    sort(null);
	}
}
