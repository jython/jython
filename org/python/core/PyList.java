package org.python.core;
import java.util.Vector;

class ListFunctions extends PyBuiltinFunctionSet {
    /*public PyObject __call__() {
        switch(index) {
            case 4:
                return __builtin__.globals();
            default:
                throw argCountError(0);
        }
    }    
    public PyObject __call__(PyObject arg1) {
        switch(index) {
            case 0:
                return Py.newString(__builtin__.chr(Py.py2int(arg1, "chr(): 1st arg can't be coerced to int")));
            case 1:
                return Py.newInteger(__builtin__.len(arg1));
            case 2:
                return __builtin__.range(Py.py2int(arg1, "range(): 1st arg can't be coerced to int"));
            case 3:
                return Py.newInteger(__builtin__.ord(Py.py2char(arg1, "ord(): 1st arg can't be coerced to char")));
            case 5:
                return __builtin__.hash(arg1);
            case 7:
                return __builtin__.list(arg1);
            case 8:
                return __builtin__.tuple(arg1);
            default:
                throw argCountError(1);
        }
    }*/
    public PyObject __call__(PyObject arg1, PyObject arg2) {
        switch(index) {
            case 0:
                ((PyList)arg1).append(arg2);
                return Py.None;
            default:
                throw argCountError(2);
        }
    }
    /*public PyObject __call__(PyObject arg1, PyObject arg2, PyObject arg3) {
        switch(index) {
            case 2:
                return __builtin__.range(
                    Py.py2int(arg1, "range(): 1st arg can't be coerced to int"),
                    Py.py2int(arg2, "range(): 2nd arg can't be coerced to int"),
                    Py.py2int(arg3, "range(): 3rd arg can't be coerced to int"));
            default:
                throw argCountError(3);
        }
    }*/
}



public class PyList extends PySequence implements InitModule {
    public void initModule(PyObject dict) {
		//dict.__setitem__("append", new ListFunctions().init("append", 0, 2, true) );
	}
    
    public PyObject[] list;
    public int length;
    
	public PyList() {
		this(Py.EmptyObjects);
	}

	public PyList(Vector ilist) {
	    this(new PyObject[ilist.size()]);
	    for(int i=0; i<ilist.size(); i++) {
	        list[i] = (PyObject)ilist.elementAt(i);
	    }
	}

	public PyList(PyObject elements[]) {
	    list = elements;
	    length = elements.length;
	}

	public int __len__() { 
	    return length; 
	}

	protected PyObject get(int i) {
	    return list[i];
	}

	protected PyObject getslice(int start, int stop, int step) {
		int n = sliceLength(start, stop, step);
		PyObject[] newList = new PyObject[n];
		
		if (step == 1) {
		    System.arraycopy(list, start, newList, 0, stop-start);
		    return new PyList(newList);
		}
		int j = 0;
		for(int i=start; j<n; i+=step) {
			newList[j] = list[i];
			j++;
		}
		return new PyList(newList);
	}

	protected void del(int i) {
	    length = length-1;
	    System.arraycopy(list, i+1, list, i, length-i);
	}

	protected void delRange(int start, int stop, int step) {
		if (step != 1)
		    throw Py.ValueError("step size must be 1 for deleting list slice");
		    
	    System.arraycopy(list, stop, list, start, length-stop);
	    length = length-(stop-start);
	}

	protected void set(int i, PyObject value) {
	    list[i] = value;
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


		int n = seq.__len__();
		int i;
		int length = this.length;
		int newLength = length-(stop-start)+n;

		if (newLength > length || newLength < length) {
		    resize(newLength);
		    System.arraycopy(list, stop, list, stop+(newLength-length), length-stop);
		} /*else if (newLength < length) {
		    System.arraycopy(list, stop, list, stop+(newLength-length), length-stop);
		    this.length = newLength;
    	}*/

		PyObject[] otherList = null;
		if (value instanceof PyTuple) otherList = ((PyTuple)value).list;
		if (value instanceof PyList) {
		    otherList = ((PyList)value).list;
		    if (otherList == list) otherList = (PyObject[])otherList.clone();;
		}
		
		if (otherList != null) {
		    System.arraycopy(otherList, 0, list, start, n);
		} else {
	        for(i=0; i<n; i++) {
	           list[i+start] = seq.get(i);
	        }
	    }
	}

	public PyObject repeat(int count) {
		int l = length;
		PyObject[] newList = new PyObject[l*count];
		for(int i=0; i<count; i++) {
		    System.arraycopy(list, 0, newList, i*l, l);
		}
		return new PyList(newList);
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
		for(int i=0; i<length-1; i++) {
			buf.append(((PyObject)list[i]).__repr__().toString());
			buf.append(", ");
		}
		if (length > 0) buf.append(((PyObject)list[length-1]).__repr__().toString());
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

	public void append(PyObject o) {
	    resize(length+1);
	    list[length-1] = o;
	}

	public int count(PyObject o) {
		int count=0;
		int n = length;
		PyObject[] list = this.list;
		for(int i=0; i<n; i++) {
			if (list[i].equals(o)) count++;
		}
		return count;
	}

	public int index(PyObject o) {
		int n = length;
		PyObject[] list = this.list;
		int i=0;
		for(; i<n; i++) {
			if (list[i].equals(o)) break;
		}
		if (i == n) throw Py.ValueError("item not found in list");
		return i;
	}

	public void insert(int index, PyObject o) {
	    resize(length+1);
	    System.arraycopy(list, index, list, index+1, length-index-1);
	    list[index] = o;
	}

	public void remove(PyObject o) {
		del(index(o));
	}

	public void reverse() {
		PyObject tmp;
		int n = length;
		PyObject[] list = this.list;
		int j = n-1;
		for(int i=0; i<n/2; i++, j--) {
			tmp = list[i];
			list[i] = list[j];
			list[j] = tmp;
		}
	}

    public PyObject pop() {
        if (length==0) {
            throw Py.IndexError("pop from empty list");
        }
        length -= 1;
        return list[length];
    }
    
    public void extend(PyObject o) {
        setslice(length, length, 1, o);
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
	public synchronized void sort(PyObject compare) {
	    /*
	    int n = __len__();
	    PyObject[] array = new PyObject[n];
	    int i;
	    for(i=0; i<n; i++)
	        array[i] = (PyObject)list.elementAt(i);
        */
	    quicksort(list, 0, length, compare);

	    /*for(i=0; i<n; i++)
	        list.setElementAt(array[i], i);*/
	}

	public void sort() {
	    sort(null);
	}
	
    // __class__ boilerplate -- see PyObject for details
    public static PyClass __class__;
    protected PyClass getPyClass() { return __class__; }
}
