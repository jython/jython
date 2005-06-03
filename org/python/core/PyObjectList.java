//Copyright (c) Corporation for National Research Initiatives
package org.python.core;

import java.io.Serializable;
import java.util.AbstractList;
import java.util.Collection;
import java.util.Iterator;
import java.util.RandomAccess;

/**
 * <CODE>java.util.List</CODE> implementation using an underlying PyObject
 * array for higher performance.  Jython should use the following methods
 * where possible, instead of their <CODE>List</CODE> counterparts:
 * <UL>
 * <LI>pyadd(int, PyObject)</LI>
 * <LI>pyadd(PyObject)</LI>
 * <LI>pyset(PyObject)</LI>
 * <LI>pyget()</LI>
 * </UL>
 * @author Clark Updike
 */
public class PyObjectList
	extends AbstractList implements RandomAccess, Cloneable, Serializable {

    /* Design note:
     * This class let's PySequenceList implement java.util.List by delegating
     * to an instance of this.  The major distinction is that the backing array
     * is PyObject[], not Object[] (as you'd get by delegating to ArrayList).
     * There are 2 major benefits:  1) A lot of casting can be avoided
     * internally (although use of PySequenceList descendants as java
     * collections does involve some casting);  2) PySequenceList descendants
     * can still do bulk array operations, allowing better performance and
     * reuse of much of the pre-collections bulk operation implementation.
     */


    /**
     * Provides mutable operations on a PyObject[] array, including features
     * that help with implementing java.util.List.
     */
    protected PyObjectArray array;

    public PyObjectList() {
        array = new PyObjectArray();
    }

    public PyObjectList(PyObject[] pyObjArr) {
        array = new PyObjectArray(pyObjArr);
        array.baseArray = pyObjArr;
    }

    public PyObjectList(Collection c) {
        array = new PyObjectArray();
        array.appendArray(c.toArray());
    }

    public PyObjectList(int size) {
        array = new PyObjectArray(size);
    }

    /**
     * For internal jython usage, use {@link #pyadd(int, PyObject)}.
     */
    public void add(int index, Object element) {
        array.add(index, Py.java2py(element));
        modCount += array.getModCountIncr();
    }

    public void pyadd(int index, PyObject element) {
        array.add(index, element);
        modCount += array.getModCountIncr();
    }

    /**
     * For internal jython usage, use {@link #pyadd(PyObject)}.
     */
    public boolean add(Object o) {
        array.add(Py.java2py(o));
        modCount += array.getModCountIncr();
        return true;
    }

    public boolean pyadd(PyObject o) {
        array.add(o);
        modCount += array.getModCountIncr();
        return true;
    }

    public Object clone(){
        try {
            PyObjectList tol = (PyObjectList) super.clone();
            tol.array = (PyObjectArray) array.clone();
            modCount = 0;
            return tol;
        } catch (CloneNotSupportedException eCNSE) {
            throw new InternalError("Unexpected CloneNotSupportedException.\n"
              + eCNSE.getMessage());
        }
    }

    public boolean equals(Object o) {
        if(o instanceof PyObjectList) {
            return array.equals(((PyObjectList)o).array);
        }
        return false;
    }

    public int hashCode() {
        return array.hashCode();
    }

    /**
     *  Use {@link #pyget(int)} for internal jython usage,.
     */
    public Object get(int index) {
        PyObject obj = array.get(index);
        return obj.__tojava__(Object.class);
    }

    PyObject pyget(int index) {
        return array.get(index);
    }

    public Object remove(int index) {
        modCount++;
        Object existing = array.get(index);
        array.remove(index);
        return existing;
    }

    public void remove(int start, int stop) {
        modCount++;
        array.remove(start, stop);
    }

    public Object set(int index, Object element) {
        return array.set(index, Py.java2py(element) ).__tojava__(Object.class);
    }

    /**
     * Use {@link #pyset(int, PyObject)} for internal jython usage.
     */
    PyObject pyset(int index, PyObject element) {
        return array.set(index, element);
    }

    public int size() {
        return array.getSize();
    }

    public boolean addAll(Collection c) {
        return addAll(size(), c);
    }

    public boolean addAll(int index, Collection c) {
        if (c instanceof PySequenceList) {
            PySequenceList cList = (PySequenceList)c;
            PyObject[] cArray = cList.getArray();
            int cOrigSize = cList.size();
            array.makeInsertSpace(index, cOrigSize);
            array.replaceSubArray(index, index + cOrigSize, cArray, 0, cOrigSize);
        } else {
            // need to use add to convert anything pulled from a collection
            // into a PyObject
            for (Iterator i = c.iterator(); i.hasNext(); ) {
                add(i.next());
            }
        }
        return c.size() > 0;
    }

	/**
	 * Get the backing array. The array should generally not be modified.
	 * To get a copy of the array, see {@link #toArray()} which returns a copy.
	 *
	 * @return backing array object
	 */
    PyObject[] getArray() {
        return (PyObject[])array.getArray();
    }

	void ensureCapacity(int minCapacity) {
	    array.ensureCapacity(minCapacity);
	}

    void replaceSubArray(int destStart, int destStop, Object srcArray, int srcStart, int srcStop) {
        array.replaceSubArray(destStart, destStop, srcArray, srcStart, srcStop);
    }

    void setSize(int count) {
        array.setSize(count);
    }
}
