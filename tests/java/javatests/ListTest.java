// Copyright (c) Corporation for National Research Initiatives
package javatests;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;

import org.python.util.Generic;

/**
 * @author updikca1
 */
public abstract class ListTest {

    public static ListTest getArrayListTest(final boolean makeReadOnly) {
        return new ListTest() {

            public List<Object> newInstance(Collection<Object> c) {
                List<Object> l = null;
                if (c == null) {
                    l = Generic.list();
                } else {
                    l = new ArrayList<Object>(c);
                }
                return (makeReadOnly) ? Collections.unmodifiableList(l) : l;
            }

            public boolean isReadOnly() {
                return makeReadOnly;
            }
        };
    }

    public static void verifyImutability(List<Object> l) {
        String message = "Expected UnsupportedOperationException.";
        try {
            l.add(0, 0);
            TestSupport.fail(message);
        } catch (UnsupportedOperationException e) {}
        try {
            l.add(0);
            TestSupport.fail(message);
        } catch (UnsupportedOperationException e) {}
        try {
            l.addAll(null);
            TestSupport.fail(message);
        } catch (UnsupportedOperationException e) {}
        try {
            l.addAll(0, null);
            TestSupport.fail(message);
        } catch (UnsupportedOperationException e) {}
        try {
            l.clear();
            TestSupport.fail(message);
        } catch (UnsupportedOperationException e) {}
        try {
            l.remove(0);
            TestSupport.fail(message);
        } catch (UnsupportedOperationException e) {}
        try {
            l.remove(new Object());
            TestSupport.fail(message);
        } catch (UnsupportedOperationException e) {}
        try {
            l.removeAll(null);
            TestSupport.fail(message);
        } catch (UnsupportedOperationException e) {}
        try {
            l.retainAll(null);
            TestSupport.fail(message);
        } catch (UnsupportedOperationException e) {}
        try {
            l.set(0, 0);
            TestSupport.fail(message);
        } catch (UnsupportedOperationException e) {}
    }

    private final List<Object> nullList;

    protected List<Object> defaultList() {
        List<Object> l = Generic.list();
        for (int i = 0; i < 4; i++) {
            l.add(i);
        }
        return newInstance(l);
    }

    /**
     * Implementations must supply an empty list if the collection is null.
     *
     * @param c
     *            Initial collection or null for empty.
     * @return the List instance
     */
    public List<Object> newInstance(Collection<Object> c) {
        throw new UnsupportedOperationException("This method must be overridden");
    }

    /**
     * @return true if the list is read-only (like PyTuple)
     */
    public boolean isReadOnly() {
        throw new UnsupportedOperationException("This method must be overridden");
    }

    {
        nullList = newInstance(Arrays.asList(new Object[] {null}));
    }

    public void testAll() {
        test_get();
        test_equals();
        test_size();
        test_contains();
        test_containsAll();
        try {
            defaultList().hashCode();
            test_hashCode();
        } catch (Exception e) {
            // skip unhashable types
        }
        test_subList();
        test_lastIndexOf();
        test_listIterator();
        test_toArray();
        test_toArray_typed();
        if (!isReadOnly()) {
            test_add();
            test_add_index();
            test_set();
            test_clear();
            test_addAll();
            test_addAll_index();
            test_remove();
            test_remove_index();
            test_removeAll();
            test_retainAll();
        } else {
            verifyImutability(newInstance(null));
        }
    }

    /** Tests get(int index) */
    public void test_get() {
        List<Object> l = defaultList();
        TestSupport.assertThat(l.get(0).equals(0),
                               "get() did not return expected value of Integer(0)");
        try {
            l.get(-1);
            TestSupport.fail("get(<negative index>) did not throw IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {}
        try {
            l.get(l.size());
            TestSupport.fail("get(<index too big>) did not throw IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {}
    }

    /** Tests set(int index, Object element) */
    public void test_set() {
        try {
            newInstance(null).set(-1, "spam");
            TestSupport.fail("get(<negative index>) did not throw IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {}
        try {
            newInstance(null).set(0, "spam");
            TestSupport.fail("set(<index too big>) did not throw IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {}
        List<Object> a = defaultList();
        a.set(a.size() - 1, "spam");
        TestSupport.assertThat(a.get(a.size() - 1).equals("spam"),
                               "set() object was not retrieved via get()");
    }

    /** Tests add(Object o) */
    public void test_add() {
        List<Object> a = newInstance(null);
        for (int i = 0; i < 4; i++) {
            a.add(i);
        }
        TestSupport.assertEquals(a, defaultList(), "add(Object o) failed");
    }

    /** Tests isEmpty() */
    public void test_isEmpty() {
        List<Object> a = newInstance(null);
        TestSupport.assertThat(a.isEmpty(), "isEmpty() is false on an emtpy List");
        a.addAll(defaultList());
        TestSupport.assertThat(!a.isEmpty(), "isEmpty() is true on a non-empty List)");
        a.clear();
        TestSupport.assertThat(a.isEmpty(), "isEmpty() is false on an emtpy List");
    }

    /** Tests size() */
    public void test_size() {
        List<Object> b = newInstance(null);
        TestSupport.assertThat(b.size() == 0, "empty list size was not 0");
        TestSupport.assertThat(defaultList().size() == 4, "default list did not have a size of 4");
    }

    /** Tests add(int index, Object element) */
    public void test_add_index() {
        List<Object> a = newInstance(null);
        List<Object> b = defaultList();
        for (int i = 0; i < b.size(); i++) {
            a.add(i, b.get(i));
        }
        try {
            a.add(a.size() + 1, new Integer(a.size() + 1));
            TestSupport.fail("expected IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {}
        try {
            a.add(-1, new Integer(-1));
            TestSupport.fail("expected IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {}
    }

    /** Tests equals(Object o) */
    public void test_equals() {
        TestSupport.assertEquals(defaultList(), defaultList(), "Identical lists weren't equal()");
        TestSupport.assertNotEquals(newInstance(null),
                                    defaultList(),
                                    "Different lists were equal()");
        TestSupport.assertNotEquals(newInstance(null),
                                    new Object(),
                                    "List was equal to a non-List type");
    }

    /** Tests addAll(Collection c) */
    public void test_addAll() {
        List<Object> a = defaultList();
        List<Object> b = defaultList();
        TestSupport.assertThat(a.addAll(b) == true, "Mutating addAll(Collection) returned false");
        TestSupport.assertThat(a.addAll(newInstance(null)) == false,
                               "Idempotent addAll(Collection) returned true");
        TestSupport.assertThat(b.addAll(b) == true, "Mutating addAll(Collection) returned false");
        TestSupport.assertEquals(a, b, "Expected equal objects from addAll(collection)");
        TestSupport.assertThat(a.size() == 8,
                               "Expected List to have size 8 after addAll(Collection)");
    }

    /** Tests indexOf(Object o) */
    public void indexOf() {
        TestSupport.assertThat(defaultList().indexOf(3) == 3, "indexOf(3) did not return 3");
        TestSupport.assertThat(defaultList().indexOf(42) == -1,
                               "indexOf() non-existing entry did not return -1");
        TestSupport.assertThat(defaultList().indexOf(null) == -1,
                               "indexOf() non-existing null did not return -1");
    }

    /** Tests contains(Object o) */
    public void test_contains() {
        TestSupport.assertThat(defaultList().contains(42) == false,
                               "contains() returned true for non-existing entry");
        TestSupport.assertThat(defaultList().contains(0) == true,
                               "contains() returned false for existing entry");
        TestSupport.assertThat(nullList.contains(null) == true,
                               "contains() returned false for existing null entry");
        TestSupport.assertThat(defaultList().contains(null) == false,
                               "contains() returned true for non-existing null entry");
    }

    /** Tests remove(Object o) */
    public void test_remove() {
        List<Object> a = defaultList();
        a.add(null);
        TestSupport.assertThat(a.remove(null) == true,
                               "remove() existing null entry returned false");
        TestSupport.assertThat(a.remove(null) == false,
                               "remove() non-existing null entry returned false");
        a.add("spam");
        TestSupport.assertThat(a.remove("spam") == true, "remove() existing entry returned false");
        TestSupport.assertThat(a.remove("spam") == false,
                               "remove() non-existing entry returned true");
    }

    /** Tests remove(int index) */
    public void test_remove_index() {
        List<Object> a = defaultList();
        for (int i = 0, n = a.size(); i < n; i++) {
            a.remove(0);
        }
        TestSupport.assertThat(a.size() == 0, "remove()-d all entries but size() not 0");
        try {
            a.remove(0);
            TestSupport.fail("removing a non-existing index did not throw exception");
        } catch (IndexOutOfBoundsException e) {}
    }

    /** Tests lastIndexOf(Object o) */
    public void test_lastIndexOf() {
        // avoid calling any mutable methods
        List<Object> l = new ArrayList<Object>(defaultList());
        l.add(0);
        // now get the immutable version
        List<Object> a = newInstance(l);
        TestSupport.assertThat(a.lastIndexOf(0) == 4, "lastIndexOf() did not return 4");
        TestSupport.assertThat(a.lastIndexOf(42) == -1,
                               "lastIndexOf() non-existing value did not return -1");
    }

    /** Tests removeAll(Collection c) */
    public void test_removeAll() {
        List<Object> a = defaultList();
        TestSupport.assertThat(a.removeAll(a) == true, "mutating removeAll() did not return true");
        TestSupport.assertThat(a.removeAll(a) == false, "idempotent removeAll did not return false");
        TestSupport.assertThat(a.removeAll(nullList) == false,
                               "idempotent removeAll did not return false");
        List<Object> yanl = newInstance(null);
        yanl.addAll(nullList);
        TestSupport.assertThat(yanl.removeAll(nullList) == true,
                               "mutating removeAll() did not return true");
        TestSupport.assertThat(yanl.size() == 0, "empty list had non-zero size");
        TestSupport.assertThat(yanl.removeAll(newInstance(null)) == false,
                               "idempotent removeAll did not return false");
    }

    /** Tests addAll(int index, Collection c) */
    public void test_addAll_index() {
        List<Object> a = defaultList();
        List<Object> b = newInstance(null);
        TestSupport.assertThat(b.addAll(0, a) == true,
                               "mutating addAll(index, Collection) did not return true");
        TestSupport.assertEquals(a, b, "addAll(index, Collection) instances failed equals test");
        TestSupport.assertThat(a.addAll(0, newInstance(null)) == false,
                               "idempotent addAll(index, Collection) did not return false");
        TestSupport.assertThat(b.addAll(0, b) == true,
                               "mutating addAll(index, Collection) did not return true");
        // Since PyObjectList has some specific handling when it detects
        // addAll on a PySequenceList, make sure the general case works.
        b = newInstance(null);
        b.addAll(new ArrayList<Object>(defaultList()));
        TestSupport.assertEquals(defaultList(), b, "addAll(index, <ArrayList>) failed equals test");
    }

    /** Tests hashCode() */
    public void test_hashCode() {
        List<Object> a = defaultList();
        TestSupport.assertThat(a.hashCode() == defaultList().hashCode(),
                               "Instances with same internal state have different hashcode");
        TestSupport.assertThat(a.hashCode() != newInstance(null).hashCode(),
                               "Instances with different internal state have the same hashcode");
        if (isReadOnly() == false) {
            List<Object> b = newInstance(null);
            b.addAll(a);
            b.remove(0);
            TestSupport.assertThat(a.hashCode() != b.hashCode(),
                                   "Instances with different internal state have the same hashcode");
        }
    }

    /** Tests clear() */
    public void test_clear() {
        List<Object> a = defaultList();
        a.clear();
        TestSupport.assertThat(a.size() == 0, "clear()-ed list did not have size of 0");
    }

    /** Tests subList(int fromIndex, int toIndex) */
    public void test_subList() {
        List<Object> a = defaultList();
        TestSupport.assertThat((a.subList(0, a.size()) != a),
                               "subList() returned the same instance");
        TestSupport.assertEquals(a.subList(0, a.size()),
                                 a,
                                 "Complete subList() did not equal original List");
        TestSupport.assertThat(a.subList(0, 0).size() == 0, "empty subList had non-zero size");
        try {
            a.subList(-1, 1);
            TestSupport.fail("Expected IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {}
        try {
            a.subList(1, 0);
            TestSupport.fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {}
        try {
            a.subList(0, a.size() + 1);
            TestSupport.fail("Expected IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {}
        if (!isReadOnly()) {
            a.subList(0, a.size()).clear();
            TestSupport.assertThat(a.size() == 0, "clear()-ed sublist did not have zero size");
            List<Object> c = newInstance(null);
            c.addAll(defaultList());
            List<Object> d = c.subList(1, 3);
            TestSupport.assertThat(d.size() == 2, "Expected subList to have size of 2");
            TestSupport.assertThat(c.set(1, "canned").equals(1),
                                   "subList.set() did not return Integer(1) from index 1"
                                           + " of defaultList");
            TestSupport.assertThat(d.get(0).equals("canned"),
                                   "subList does not update with changes to parent");
            d.set(0, "spam");
            TestSupport.assertThat(c.get(1).equals("spam"),
                                   "parent does not update with changes to subList child");
        } else {
            List<Object> b = a.subList(0, a.size());
            verifyImutability(b);
        }
    }

    /** Tests retainAll(Collection c) */
    public void test_retainAll() {
        List<Object> a = defaultList();
        a.retainAll(defaultList());
        TestSupport.assertEquals(a,
                                 defaultList(),
                                 "retainAll(<equal List>) does not equal original list");
        a = defaultList();
        a.retainAll(newInstance(null));
        TestSupport.assertThat(a.size() == 0, "retainAll(<empty List>))does not have size of zero");
        a = defaultList();
        a.remove(0);
        a.remove(0);
        a.add(4);
        a.add(5);
        List<Object> b = newInstance(null);
        b.add(2);
        b.add(3);
        a.retainAll(b);
        TestSupport.assertEquals(a,
                                 b,
                                 "retainAll() on overlap of indices [2,3] did not return that List");
    }

    /** Tests containsAll(Collection c) */
    public void test_containsAll() {
        TestSupport.assertThat(defaultList().containsAll(defaultList()),
                               "containsAll(<identical List> was false");
        TestSupport.assertThat(defaultList().containsAll(newInstance(null)),
                               "containsAll(<empty List>) was false");
        TestSupport.assertThat(newInstance(null).containsAll(defaultList()) == false,
                               "containsAll(<disjoint List>) returned true");
        TestSupport.assertThat(defaultList().containsAll(defaultList().subList(1, 3)),
                               "containsAll(<subList>) was false");
    }

    /** Tests iterator() */
    public void test_iterator() {
        TestSupport.assertThat(newInstance(null).iterator().hasNext() == false,
                               "Iterator for empty list thinks it hasNext()");
        try {
            newInstance(null).iterator().next();
            TestSupport.fail("expected NoSuchElementException");
        } catch (NoSuchElementException e) {}
        List<Object> a = defaultList();
        int i = 0;
        for (Object element : a) {
            TestSupport.assertThat(element == a.get(i++), "Iterator next() failed identity test");
        }
        TestSupport.assertThat(i == a.size(), "Iterator did not iterator over entire list");
    }

    public void test_listIterator() {
        ListIterator<Object> li = newInstance(null).listIterator();
        TestSupport.assertThat(!li.hasNext(), "ListIterator.hasNext() is true for empty List");
        TestSupport.assertThat(!li.hasPrevious(),
                               "ListIterator.hasPrevious() is true for empty List");
        try {
            li.next();
            TestSupport.fail("expected NoSuchElementException");
        } catch (NoSuchElementException e) {}
        try {
            li.previous();
            TestSupport.fail("expected NoSuchElementException");
        } catch (NoSuchElementException e) {}
        int nextIndex = li.nextIndex();
        TestSupport.assertThat(nextIndex == 0,
                               "ListIterator.nextIndex() on empty List did not return 0");
        int prevIndex = li.previousIndex();
        TestSupport.assertThat(prevIndex == -1,
                               "ListIterator.previousIndex() on empty List did not return -1");
        List<Object> l = Generic.list();
        l.add(1);
        li = newInstance(l).listIterator();
        TestSupport.assertThat(!li.hasPrevious(),
                               "ListIterator.hasPrevious() is true with nothing previous");
        TestSupport.assertThat(li.hasNext(), "ListIterator.hasNext() is false with next present");
        TestSupport.assertThat(li.next().equals(1),
                               "ListIterator.next() did not return expected Integer(1)");
        if (!isReadOnly()) {
            li.remove();
            TestSupport.assertThat(!li.hasNext(), "ListIterator.hasNext() is true for empty List");
            TestSupport.assertThat(!li.hasPrevious(),
                                   "ListIterator.hasPrevious() is true for empty List");
            try {
                li.set(42);
                TestSupport.fail("expected IllegalStateException");
            } catch (IllegalStateException e) {}
            try {
                li.remove();
                TestSupport.fail("expected IllegalStateException");
            } catch (IllegalStateException e) {}
        }
        l = Generic.list(new Object[]{0, 1, 2});
        li = newInstance(l).listIterator();
        for (int i = 0, n = l.size(); i < n; i++) {
            TestSupport.assertThat(li.next().equals(i),
                                   "ListIterator.previous did not return expected value");
        }
        while (!isReadOnly() && li.hasNext()) {
            li.next();
            li.set(42);
            TestSupport.assertThat(li.previous().equals(42),
                                   "ListIterator.previous() did not return the value that was set()");
            li.remove();
        }
        if (isReadOnly()) {
            li = newInstance(null).listIterator();
        }
        li = defaultList().listIterator(2);
        TestSupport.assertThat(li.next().equals(2),
                               "List.listIteraor(index) did not return expected value");
        TestSupport.assertThat(li.next().equals(3),
                               "List.listIteraor(index) did not return expected value");
        TestSupport.assertThat(!li.hasNext(), "listIterator.hasNext() at end of list returned true");
    }

    /** Tests toArray() */
    public void test_toArray() {
        Object[] intObjArray = {0, 1, 2, 3};
        TestSupport.assertThat(Arrays.equals(defaultList().toArray(), intObjArray),
                               "toArray() did not return the expected Integer[] array");
    }

    /** Tests toArray(Object[] a) */
    public void test_toArray_typed() {
        Object[] intObjArray = {0, 1, 2, 3};
        TestSupport.assertThat(Arrays.equals(defaultList().toArray(new Integer[0]), intObjArray),
                               "toArray(Integer[]) did not return the expected Integer[] array");
    }
    // Can't test clone since it's not visible, but it is from jython.
    // /** Tests clone() */
    // public void test_clone() {
    // List a = defaultList();
    // TestSupport.assertEquals(((Object)a).clone(), a,
    // "clone() was not equal to original");
    // TestSupport.assertThat(a.clone() != a,
    // "clone() returned same instance");
    // }
}
