//Copyright (c) Corporation for National Research Initiatives
package org.python.core;


/**
 * Provides mutable behavior on a PyObject array.  Supports operations for
 * implementing  <CODE>java.util.List</CODE>.
 * @author Clark Updike
 */
public class PyObjectArray extends AbstractArray {

    public void remove(int start, int stop) {
        super.remove(start, stop);
    }

	/**
	 * The underlying array used for storing the data.
	 */
	protected PyObject[] baseArray;

	/**
	 * Create the array with the specified size.
	 */
	public PyObjectArray() {
		super(PyObject.class);
	}

	public PyObjectArray(PyObject[] rawArray) {
	    super(rawArray == null ? 0 : rawArray.length);
	    baseArray = (rawArray == null) ? new PyObject[] {} : rawArray;
	}

	/**
	 * Create the array with the specified size.
	 * @param size number of int values initially allowed in array
	 */
	public PyObjectArray(int size) {
		super(PyObject.class, size);
	}

	/**
     * @param toCopy
     */
    public PyObjectArray(PyObjectArray toCopy) {

        super(toCopy);
        this.baseArray = (PyObject[])toCopy.copyArray();
    }

    /**
	 * Add a value at a specified index in the array.
	 * <P><CODE>AbstractList</CODE> subclasses should update their
	 * <CODE>modCount</CODE> after calling this method.
	 *
	 * @param index index position at which to insert element
	 * @param value value to be inserted into array
	 */
	public void add(int index, PyObject value) {
		makeInsertSpace(index);
		baseArray[index] = value;
	}

	/**
	 * Add a value to the array, appending it after the current values.
	 * <P><CODE>AbstractList</CODE> subclasses should update their
	 * <CODE>modCount</CODE> after calling this method.
	 *
	 * @param value value to be added
	 * @return index number of added element
	 */
	public int add(PyObject value) {
		int index = getAddIndex();
		baseArray[index] = value;
		return index;
	}

	/**
	 * Duplicates the object with the generic call.
	 *
	 * @return a copy of the object
	 */
	public Object clone() {
		return new PyObjectArray(this);
	}

    public boolean equals(Object o) {
        if(o instanceof PyObjectArray) {
            PyObjectArray arr = (PyObjectArray)o;
            if (size != arr.size) return false;
            for (int i = 0; i < size; i++) {
                PyObject thisElem = baseArray[i];
                PyObject otherElem = arr.baseArray[i];
                if (thisElem == null) {
                    if (otherElem == null) continue;
                    return false;
                }
                if (!thisElem.equals(otherElem)) return false;
            }
            return true;
        }
        return false;
    }

    public int hashCode() {
        int x, y;
        int len = size;
        x = 0x345678;

        for (len--; len>=0; len--) {
            y = baseArray[len].hashCode();
            x = (x + x + x) ^ y;
        }
        x ^= size;
        return x;
    }

	/**
	 * Discards values for a range of indices from the array. For the array of
	 * <code>int</code> values, just sets the values to null.
	 *
	 * @param from index of first value to be discarded
	 * @param to index past last value to be discarded
	 */
	protected void discardValues(int from, int to) {
		for (int i = from; i < to; i++) {
			baseArray[i] = null;
		}
	}

	/**
	 * Retrieve the value present at an index position in the array.
	 *
	 * @param index index position for value to be retrieved
	 * @return value from position in the array
	 */
	public PyObject get(int index) {

	    if (index >= 0 && index < size) {
			return baseArray[index];
		}

		String message = (size == 0)
				? "No data was added, unable to get entry at " + index
				: "Index must be between " + 0 + " and " +
				  (size - 1) + ", but was " + index;
		throw new ArrayIndexOutOfBoundsException(message);

	}

	/**
	 * Get the backing array. This method is used by the type-agnostic base
	 * class code to access the array used for type-specific storage.  The array
	 * should generally not be modified.  To get a copy of the array, see
	 * {@link #toArray()} which returns a copy.  Note that 
     * <CODE>getSize()</CODE> should be used to determine the number of elements
     * in the array, not the array's length (which may reflect excess capacity).
     * <CODE>toArray()</CODE> returns an array whose length equals the value
     * returned by <CODE>getSize()</CODE>.
	 *
	 * @return backing array object
	 */
	public Object getArray() {
		return baseArray;
	}

	/**
	 * Set the value at an index position in the array.
	 *
	 * @param index index position to be set
	 * @param value value to be set
	 */
	public PyObject set(int index, PyObject value) {
		if (index >= 0 && index < size) {
		    PyObject existing = baseArray[index];
			baseArray[index] = value;
			return existing;
		}
		throw new ArrayIndexOutOfBoundsException(
				"Index must be between " + 0 + " and " +
				(size - 1) + ", but was " + index);
	}

	/**
	 * Set the backing array. This method is used by the type-agnostic base
	 * class code to set the array used for type-specific storage.
	 *
	 * @param array the backing array object
	 */
	protected void setArray(Object array) {
		baseArray = (PyObject[]) array;
	}

	/**
	 * Constructs and returns a simple array containing the same data as held
	 * in this growable array.  The array's length matches the value returned
     * by <CODE>getSize()</CODE>
	 *
	 * @return array containing a copy of the data
	 */
	public PyObject[] toArray() {
		return (PyObject[]) copyArray();
	}

	public void ensureCapacity(int minCapacity) {
	    super.ensureCapacity(minCapacity);
	}
	
	@Override
	protected PyObject[] createArray(int size) {
        return new PyObject[size];
    }
}

