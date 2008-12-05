package org.python.core;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.Arrays;

/**
 * Abstract class that manages bulk structural and data operations
 * on arrays, defering type-specific element-wise operations to the
 * subclass.  Subclasses supply the underlying array and the
 * type-specific operations--greatly reducing the need for casting
 * (thus achieving array-like performances with collection-like
 * flexibility).  Also includes
 * functionality to support integration with the the jdk's
 * collections (via methods that return a modification increment).<P>
 * Subclasses will want to provide the following methods (which are
 * not declared in this class since subclasses should specify the
 * explicit return type):
 * <UL>
 * <LI><CODE>&lt;type&gt; get(int)</CODE></LI>
 * <LI><CODE>void set(int, &lt;type&gt;)</CODE></LI>
 * <LI><CODE>void add(&lt;type&gt;)</CODE></LI>
 * <LI><CODE>void add(int, &lt;type&gt;)</CODE></LI>
 * <LI><CODE>&lt;type&gt;[] toArray()</CODE></LI>
 * </UL><P>
 * Clone cannot be supported since the array is not held locally.
 * But the @link #AbstractArray(AbstractArray) constructor can be used
 * for suclasses that need to support clone.
 * <P>
 * This "type-specific collections" approach was originally developed
 * by Dennis Sosnoski, who provides a more complete library at the
 * referenced URL.  Sosnoski's library does not integrate with the
 * jdk collection classes but provides collection-like classes.
 *
 * @author Clark Updike
 * @see <A href="http://www.sosnoski.com/opensrc/tclib/index.html">
 *      Sosnoski's Type-Specific Collection Library</A>
 */
public abstract class AbstractArray implements Serializable{

    /**
     * Size of the current array, which can be larger than the
     * <CODE>size</CODE> field.
     */
    protected int capacity;

    /**
     * The number of values currently present in the array.
     */
    protected int size;

    /**
     * The modification count increment indicates if a structural change
     * occured as a result of an operation that would make concurrent iteration
     * over the array invalid.  It is typically used by subclasses that
     * extend <CODE>AbstractList</CODE>, by adding the value to
     * <CODE>AbstractList.modCount</CODE> after performing a potentially
     * structure-altering operation.  A value of 0 indicates that
     * it is still valid to iterate over the array.  A value of 1
     * indicates it is no longer valid to iterate over the range.<P>
     * This class uses a somewhat stricter semantic for <CODE>modCount</CODE>.
     * Namely, <CODE>modCountIncr</CODE> is only set to 1 if a structural
     * change occurred.  The jdk collections generally increment
     * <CODE>modCount</CODE> if a potentially structure-altering method
     * is called, regardless of whether or not a change actually occurred.
     *
     * <b>See also:</b> <code>java.util.AbstractList#modCount</code>
     */
    protected int modCountIncr;

    /**
     * Since AbstractArray can support a clone method, this facilitates
     * sublcasses that want to implement clone (poor man's cloning).
     * Sublclasses can then do this:
     * <PRE>
     * public MyManagedArray(MyManagedArray toCopy) {
     * super(this);
     * this.baseArray = (<my array type>)toCopy.copyArray();
     * this.someProp = toCopy.someProp;
     * <etc>
     * }
     * <p/>
     * public Object clone() {
     * return new MyManagedArray(this);
     * }
     * </PRE>
     *
     * @param toCopy
     */
    public AbstractArray(AbstractArray toCopy) {
        this.capacity = toCopy.capacity;
        // let modCountIncr default to 0
        this.size = toCopy.size;
    }

    /**
     * Use when the subclass has a preexisting array.
     *
     * @param size the initial size of the array
     */
    public AbstractArray(int size) {
        this.size = size;
        this.capacity = size;
    }

    /**
     * Creates the managed array with a default size of 10.
     *
     * @param type array element type (primitive type or object class)
     */
    public AbstractArray(Class type) {
        this(type, 10);
    }

    /**
     * Construtor for multi-dimensional array types.
     * For example, <CODE>char[][]</CODE>.  This class only manages the
     * top level dimension of the array.  For single dimension
     * arrays (the more typical usage), use the other constructors.<P>
     *
     * @param type       Array element type (primitive type or object class).
     * @param dimensions An int array specifying the dimensions.  For
     *                   a 2D array, something like <CODE>new int[] {10,0}</CODE> to
     *                   create 10 elements each of which can hold an reference to an
     *                   array of the same type.
     * @see Array#newInstance(java.lang.Class, int[])
     */
    public AbstractArray(Class type, int[] dimensions) {
        Object array = Array.newInstance(type, dimensions);
        this.capacity = dimensions[0];
        setArray(array);
    }

    /**
     * Creates the managed array with the specified size.
     *
     * @param type array element type (primitive type or object class)
     * @param size number of elements initially allowed in array
     */
    public AbstractArray(Class type, int size) {
        Object array = Array.newInstance(type, size);
        this.capacity = Math.max(size, 10);
        setArray(array);
    }

    /**
     * Appends the supplied array, which must be an array of the same
     * type as <CODE>this</CODE>, to the end of <CODE>this</CODE>.
     * <P><CODE>AbstractList</CODE> subclasses should update their
     * <CODE>modCount</CODE> after calling this method.
     *
     * @param ofArrayType the array to append
     */
    public void appendArray(Object ofArrayType) {
        replaceSubArray(ofArrayType, this.size);
    }

    /**
     * Set the array to the empty state, clearing all the data out and
     * nulling objects (or "zero-ing" primitives).
     * <P>Note: This method does not set <CODE>modCountIncr</CODE> to
     * <CODE>1</CODE> even though <CODE>java.util.ArrayList</CODE>
     * would.
     * <p/>
     * <P><CODE>AbstractList</CODE> subclasses should update their
     * <CODE>modCount</CODE> after calling this method.
     */
    public void clear() {
        this.modCountIncr = 0;
        if (this.size != 0) {
            this.modCountIncr = 1;
            clearRange(0, this.size);  
            setSize(0);
        }

    }


    /**
     * Clears out the values in the specified range.  For object arrays,
     * the cleared range is nullified.  For primitve arrays, it is
     * "zero-ed" out.
     * <P>Note: This method does not set <CODE>modCountIncr</CODE> to
     * <CODE>1</CODE> even though <CODE>java.util.ArrayList</CODE>
     * would.
     *
     * @param start the start index, inclusive
     * @param stop  the stop index, exclusive
     */
    protected void clearRange(int start, int stop) {

        if (start < stop && start >= 0 && stop <= this.size) {
            clearRangeInternal(start, stop);
        } else {
            if (start == stop && start >= 0 && stop <= this.size) {
                return;
            }

            throw new ArrayIndexOutOfBoundsException("start and stop must follow: 0 <= start <= stop <= " +
                    (this.size) + ", but found start= " + start + " and stop=" + stop);
        }
    }

    /**
     * Used internally, no bounds checking.
     *
     * @param start the start index, inclusive
     * @param stop  the stop index, exclusive
     */
    private void clearRangeInternal(int start, int stop) {

        Object base = getArray();
        Class arrayType = base.getClass().getComponentType();
        if (arrayType.isPrimitive()) {
            if (arrayType == Boolean.TYPE) {
                Arrays.fill((boolean[]) base, start, stop, false);
            } else if (arrayType == Character.TYPE) {
                Arrays.fill((char[]) base, start, stop, '\u0000');
            } else if (arrayType == Byte.TYPE) {
                Arrays.fill((byte[]) base, start, stop, (byte) 0);
            } else if (arrayType == Short.TYPE) {
                Arrays.fill((short[]) base, start, stop, (short) 0);
            } else if (arrayType == Integer.TYPE) {
                Arrays.fill((int[]) base, start, stop, 0);
            } else if (arrayType == Long.TYPE) {
                Arrays.fill((long[]) base, start, stop, 0);
            } else if (arrayType == Float.TYPE) {
                Arrays.fill((float[]) base, start, stop, 0.f);
            } else if (arrayType == Double.TYPE) {
                Arrays.fill((double[]) base, start, stop, 0.);
            }
        } else {
            Arrays.fill((Object[]) base, start, stop, null);
        }

    }

    /**
     * Constructs and returns a simple array containing the same data as held
     * in this growable array.
     *
     * @return array containing a shallow copy of the data.
     */
    public Object copyArray() {
        Object copy = createArray(this.size);
        System.arraycopy(getArray(), 0, copy, 0, this.size);
        return copy;
    }

    /**
     * Ensures that the base array has at least the specified
     * minimum capacity.
     * <P><CODE>AbstractList</CODE> subclasses should update their
     * <CODE>modCount</CODE> after calling this method.
     *
     * @param minCapacity new minimum size required
     */
    protected void ensureCapacity(int minCapacity) {
        // ArrayList always increments the mod count, even if no
        // structural change is made (not sure why).
        // This only indicates a mod count change if a change is made.
        this.modCountIncr = 0;
        if (minCapacity > this.capacity) {
            this.modCountIncr = 1;
            int newCapacity = (this.capacity * 2) + 1;
            newCapacity = (newCapacity < minCapacity)
                    ? minCapacity
                    : newCapacity;
            setNewBase(newCapacity);
            this.capacity = newCapacity;
        }
    }

    /**
     * Gets the next add position for appending a value to those in the array.
     * If the underlying array is full, it is grown by the appropriate size
     * increment so that the index value returned is always valid for the
     * array in use by the time of the return.
     * <P><CODE>AbstractList</CODE> subclasses should update their
     * <CODE>modCount</CODE> after calling this method.
     *
     * @return index position for next added element
     */
    protected int getAddIndex() {
        int index = this.size++;
        if (this.size > this.capacity) {
            ensureCapacity(this.size);
        }
        return index;
    }

    /**
     * Get the backing array. This method is used by the type-agnostic base
     * class code to access the array used for type-specific storage by the
     * child class.
     *
     * @return backing array object
     */
    protected abstract Object getArray();

    protected boolean isEmpty() {
        return this.size == 0;
    }

    /**
     * Makes room to insert a value at a specified index in the array.
     * <P><CODE>AbstractList</CODE> subclasses should update their
     * <CODE>modCount</CODE> after calling this method.  Does not change
     * the <CODE>size</CODE> property of the array.
     *
     * @param index index position at which to insert element
     */
    protected void makeInsertSpace(int index) {
        makeInsertSpace(index, 1);
    }

    protected void makeInsertSpace(int index, int length) {

        this.modCountIncr = 0;
        if (index >= 0 && index <= this.size) {
            int toCopy = this.size - index;
            this.size = this.size + length;
            // First increase array size if needed
            if (this.size > this.capacity) {
                ensureCapacity(this.size);
            }
            if (index < this.size - 1) {
                this.modCountIncr = 1;
                Object array = getArray();
                System.arraycopy(array, index, array, index + length, toCopy);
            }
        } else {
            throw new ArrayIndexOutOfBoundsException("Index must be between 0 and " +
                    this.size + ", but was " + index);
        }
    }

    /**
     * Remove a value from the array. All values above the index removed
     * are moved down one index position.
     * <P><CODE>AbstractList</CODE> subclasses should always increment
     * their <CODE>modCount</CODE> method after calling this, as
     * <CODE>remove</CODE> always causes a structural modification.
     *
     * @param index index number of value to be removed
     */
    public void remove(int index) {
        if (index >= 0 && index < this.size) {
            this.size = this.size - 1;
            if (index < this.size) {
                Object base = getArray();
                System.arraycopy(base, index + 1, base, index, this.size - index);
                clearRangeInternal(this.size, this.size);
            }

        } else {
            if (this.size == 0) {
                throw new IllegalStateException("Cannot remove data from an empty array");
            }
            throw new IndexOutOfBoundsException("Index must be between 0 and " +
                    (this.size - 1) + ", but was " + index);

        }
    }

    /**
     * Removes a range from the array at the specified indices.
     * @param start inclusive
     * @param stop exclusive
     */
    public void remove(int start, int stop) {
        if (start >= 0 && stop <= this.size && start <= stop) {
            Object base = getArray();
            int nRemove = stop - start;
            if (nRemove == 0) {
                return;
            }
            System.arraycopy(base, stop, base, start, this.size - stop);
            this.size = this.size - nRemove;
            clearRangeInternal(this.size, this.size + nRemove);
            setArray(base);
            return;
        }

        throw new IndexOutOfBoundsException("start and stop must follow: 0 <= start <= stop <= " +
                this.size + ", but found start= " + start + " and stop=" + stop);
    }

    /**
     * Allows an array type to overwrite a segment of the array.
     * Will expand the array if <code>(atIndex + 1) + ofArrayType</code>'s length
     * is greater than the current length.
     * <P><CODE>AbstractList</CODE> subclasses should update their
     * <CODE>modCount</CODE> after calling this method.
     *
     * @param array
     * @param atIndex
     */
    public void replaceSubArray(Object array, int atIndex) {
        int arrayLen = Array.getLength(array);
        replaceSubArray(atIndex, Math.min(this.size, atIndex + arrayLen), array, 0, arrayLen);
    }
    
    /**
     * Replace a range of this array with another subarray.
     * @param thisStart the start index (inclusive) of the subarray in this 
     * array to be replaced
     * @param thisStop the stop index (exclusive) of the subarray in this 
     * array to be replaced
     * @param srcArray the source array from which to copy
     * @param srcStart the start index (inclusive) of the replacement subarray
     * @param srcStop the stop index (exclusive)  of the replacement subarray
     */
    public void replaceSubArray(int thisStart, int thisStop, Object srcArray, 
            int srcStart, int srcStop) {
    
        this.modCountIncr = 0;
        if (!srcArray.getClass().isArray()) {
            throw new IllegalArgumentException("'array' must be an array type");
        }
    
        int replacedLen = thisStop - thisStart;
         if (thisStart < 0 || replacedLen < 0 || thisStop > this.size) {
            String message = null;
            if (thisStart < 0) {
                message = "thisStart < 0 (thisStart = " + thisStart + ")";
            } else if (replacedLen < 0) {
                message = "thisStart > thistStop (thisStart = " + thisStart + 
                                ", thisStop = " + thisStop + ")";
            } else if (thisStop > this.size) {
                message = "thisStop > size (thisStop = " + thisStop + 
                                ", size = " + this.size + ")";
            } else {
                throw new InternalError("Incorrect validation logic");
            }
    
            throw new ArrayIndexOutOfBoundsException(message);
        }
    
        int srcLen = Array.getLength(srcArray);
        int replacementLen = srcStop - srcStart;
        if (srcStart < 0 || replacementLen < 0 || srcStop > srcLen) {
            String message = null;
            if (srcStart < 0) {
                message = "srcStart < 0 (srcStart = " + srcStart +")";
            } else if (replacementLen < 0) {
                message = "srcStart > srcStop (srcStart = " + srcStart + 
                                ", srcStop = " + srcStop + ")";
            } else if (srcStop > srcLen) {
                message = "srcStop > srcArray length (srcStop = " + srcStop + 
                                ", srcArray length = " +srcLen + ")";
            } else {
                throw new InternalError("Incorrect validation logic");
            }
            
            throw new IllegalArgumentException("start, stop and array must follow:\n\t"
                    + "0 <= start <= stop <= array length\nBut found\n\t" +
                    message);
        }
    
        int lengthChange = replacementLen - replacedLen;
        
        // Adjust array size if needed.
        if (lengthChange < 0) {
            remove(thisStop + lengthChange, thisStop);
        } else if (lengthChange > 0) {
            makeInsertSpace(thisStop, lengthChange);
        }
    
        try {
            this.modCountIncr = 1;
            System.arraycopy(srcArray, srcStart, getArray(), thisStart, replacementLen);
        } catch (ArrayStoreException e) {
            throw new IllegalArgumentException("'ofArrayType' must be compatible with existing array type of " +
                    getArray().getClass().getName() + "\tsee java.lang.Class.getName().");
        }
    }
    
    /**
     * Set the backing array. This method is used by the type-agnostic base
     * class code to set the array used for type-specific storage by the
     * child class.
     *
     * @param array the backing array object
     */
    protected abstract void setArray(Object array);

    /**
     * Replaces the existing base array in the subclass with a new
     * base array resized to the specified capacity.
     *
     * @param newCapacity
     */
    private void setNewBase(int newCapacity) {
        this.modCountIncr = 1;
        Object base = getArray();
        Object newBase = createArray(newCapacity);
        System.arraycopy(base, 0, newBase, 0, capacity > newCapacity ? newCapacity : capacity);
        setArray(newBase);
    }

    /**
     * Sets the number of values currently present in the array. If the new
     * size is greater than the current size, the added values are initialized
     * to the default values. If the new size is less than the current size,
     * all values dropped from the array are discarded.
     * <P><CODE>AbstractList</CODE> subclasses should update their
     * <CODE>modCount</CODE> after calling this method.
     *
     * @param count number of values to be set
     */
    public void setSize(int count) {
        if (count > this.capacity) {
            ensureCapacity(count);
        } else if (count < this.size) {
            clearRange(count, this.size);  
        }
        this.size = count;
    }

    /**
     * Get the number of values currently present in the array.
     *
     * @return count of values present
     */
    public int getSize() {
        return this.size;
    }

    /**
     * Provides a default comma-delimited representation of array.
     *
     * @see java.lang.Object#toString()
     */
    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append("[");

        Object base = getArray();
        Class arrayType = base.getClass().getComponentType();
        int n = this.size - 1;
        if (arrayType.isPrimitive()) {
            for (int i = 0; i < n; i++) {
                buf.append(Array.get(base, i)).append(", ");
            }
            if (n >= 0) buf.append(Array.get(base, n));
        } else {
            Object[] objects = (Object[]) base;
            for (int i = 0; i < n; i++) {
                buf.append(objects[i]).append(", ");
            }
            if (n >= 0) {
                buf.append(objects[n]);
            }
        }
        buf.append("]");
        return buf.toString();
    }


    /**
     * Removes any excess capacity in the backing array so it is
     * just big enough to hold the amount of data actually in the array.
     */
    protected void trimToSize() {
        // Don't need to adjust modCountIncr since AbstractList subclasses
        // should only ever see up to the size (and not the capacity--which
        // is encapsulated).
        if (this.size < this.capacity) {
            setNewBase(this.size);
        }
    }


    /**
     * Returns the modification count increment, which is used by
     * <CODE>AbstractList</CODE> subclasses to adjust <CODE>modCount</CODE>
     * <CODE>AbstractList</CODE> uses it's <CODE>modCount</CODE> field
     * to invalidate concurrent operations (like iteration) that should
     * fail if the underlying array changes structurally during the
     * operation.
     *
     * @return the modification count increment (0 if no change, 1 if changed)
     */
    public int getModCountIncr() {
        return this.modCountIncr;
    }
    
    /**
     * @return an array of the given size for the type used by this abstract array.
     */ 
    protected abstract Object createArray(int size);
}
