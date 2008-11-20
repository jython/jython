package org.python.antlr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import org.python.core.PyObject;

public class ListWrapper<E> implements List<E> {

    private List<E> list;

    public ListWrapper(List<E> list) {
        this.list = list;
    }

    public boolean containsAll(Collection c) {
        return containsAll(c);
    }

    public boolean removeAll(Collection c) {
        return list.removeAll(c);
    }

    public boolean retainAll(Collection c) {
        return list.retainAll(c);
    }

    public boolean add(E e) {
        return list.add(e);
    }

    public void add(int index, E e) {
        list.add(index, e);
    }

    public boolean addAll(Collection c) {
        return list.addAll(c);
    }

    public boolean addAll(int index, Collection c) {
        return list.addAll(index, c);
    }

    public void clear() {
        list.clear();
    }

    public boolean contains(Object elem) {
        return list.contains(elem);
    }

    public E get(int index) {
        return list.get(index);
    }

    public int indexOf(Object elem) {
        return list.indexOf(elem);
    }

    public boolean isEmpty() {
        return list.isEmpty();
    }

    public int lastIndexOf(Object elem) {
        return list.lastIndexOf(elem);
    }

    public E remove(int index) {
        return list.remove(index);
    }

    public boolean remove(Object o) {
        return list.remove(o);
    }

    public E set(int index, E element) {
        return list.set(index, element);
    }

    public int size() {
        return list.size();
    }

    public Object[] toArray() {
        return list.toArray();
    }

    public Object[] toArray(Object[] a) {
        return list.toArray(a);
    }

    public Iterator iterator() {
        return list.iterator();
    }

    public ListIterator listIterator() {
        return list.listIterator();
    }

    public ListIterator listIterator(int index) {
        return list.listIterator(index);
    }

    public List subList(int fromIndex, int toIndex) {
        return list.subList(fromIndex, toIndex);
    }

    public ListWrapper __add__(Object o) {
        List newList = new ArrayList();
        newList.addAll(list);
        newList.add(o);
        return new ListWrapper(newList);
    }

    public void __iadd__(PyObject o) {
        extend(o);
    }

    public int __len__() {
        return list.size();
    }

    public boolean __contains__(Object o) {
        return list.contains(o);
    }

    public PyObject __imul__(PyObject o) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public PyObject __iter__() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public PyObject __mul__(PyObject o) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public PyObject __radd__(PyObject o) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public PyObject __rmul__(PyObject o) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void append(PyObject o) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public int count(PyObject o) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    protected void del(int i) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    protected void delRange(int start, int stop, int step) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void extend(PyObject o) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public int index(PyObject o) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public int index(PyObject o, int start) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public int index(PyObject o, int start, int stop) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void insert(int index, PyObject o) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public PyObject pop() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public PyObject pop(int n) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void remove(PyObject o) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void reverse() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void sort(PyObject compare) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void sort() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void sort(PyObject cmp, PyObject key, PyObject reverse) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
