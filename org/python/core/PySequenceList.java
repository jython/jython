/*                           JHU/APL PROPRIETARY
 * Copyright © 2005 The Johns Hopkins University/Applied Physics Laboratory.
 * All rights reserved.  This software is for internal JHU/APL use only and
 * is not to be distributed outside of JHU/APL.  For all other permissions,
 * please contact the RMIS Development Team at JHU/APL.
 *
 * Created: Apr 18, 2005
 * By: updikca1
 */
package org.python.core;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Arrays;


/**
 * @author updikca1
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public abstract class PySequenceList extends PySequence implements List {

    protected PyObjectList list;

    public PySequenceList() {
        list = new PyObjectList();
    }

    protected PySequenceList(PyType type) {
        super(type);
        list = new PyObjectList();
    }

    /**
     * Creates an instance directly backed by the array of PyObject elements.
     * @param elements
     */
    public PySequenceList(PyObject[] elements) {
        list = new PyObjectList(elements);
    }

    public PySequenceList(Collection c) {
        list = new PyObjectList(c);
    }

    public void add(int index, Object element) {
        list.add(index, element);
    }
    public boolean add(Object o) {
        return list.add(o);
    }
    public boolean addAll(int index, Collection c) {
        return list.addAll(index, c);
    }
    public boolean addAll(Collection c) {
        return list.addAll(c);
    }
    public void clear() {
        list.clear();
    }
    public boolean contains(Object o) {
        return list.contains(o);
    }
    public boolean containsAll(Collection c) {
        return list.containsAll(c);
    }
    public Object get(int index) {
        return list.get(index);
    }
    public int indexOf(Object o) {
        return list.indexOf(o);
    }
    public boolean isEmpty() {
        return list.isEmpty();
    }
    public Iterator iterator() {
        return list.iterator();
    }
    public int lastIndexOf(Object o) {
        return list.lastIndexOf(o);
    }
    public ListIterator listIterator() {
        return list.listIterator();
    }
    public ListIterator listIterator(int index) {
        return list.listIterator(index);
    }
    public void pyadd(int index, PyObject element) {
        list.pyadd(index, element);
    }
    public PyObject pyget(int index) {
        return list.pyget(index);
    }
    public PyObject pyset(int index, PyObject element) {
        return list.pyset(index, element);
    }
    public Object remove(int index) {
        return list.remove(index);
    }
    public void remove(int start, int stop) {
        list.remove(start, stop);
    }
    public boolean remove(Object o) {
        return list.remove(o);
    }
    public boolean removeAll(Collection c) {
        return list.removeAll(c);
    }
    public boolean retainAll(Collection c) {
        return list.retainAll(c);
    }
    public Object set(int index, Object element) {
        return list.set(index, element);
    }
    public int size() {
        return list.size();
    }
    public List subList(int fromIndex, int toIndex) {
        return list.subList(fromIndex, toIndex);
    }
    public Object[] toArray() {
        return list.toArray();
    }
    public Object[] toArray(Object[] a) {
        return list.toArray(a);
    }
    public String toString() {
        return list.toString();
    }
    public boolean pyadd(PyObject o) {
        return list.pyadd(o);
    }

    public boolean equals(Object o) {
        if(o instanceof PySequenceList) {
            return list.equals(((PySequenceList)o).list);
        }
        return false;
    }

    public int hashCode() {
        return list.hashCode();
    }

//    /**
//     * @param list The list to set.
//     */
//    public void setList(PyObjectList list) {
//        this.list = list;
//    }

	/**
	 * Get the backing array. The array should not be modified.
	 * To get a copy of the array, see {@link #toArray()}.
	 *
	 * @return backing array object
	 */
    public PyObject[] getArray() {
        return list.getArray();
    }
}
