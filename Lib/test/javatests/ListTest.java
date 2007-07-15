//Copyright (c) Corporation for National Research Initiatives
package javatests;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;

/**
 * @author updikca1
 */
public abstract class ListTest {
    
    public static ListTest getArrayListTest(final boolean makeReadOnly) {
        return new ListTest() {
            public List newInstance(Collection c) {
                List l = null;
                if(c == null) {
                    l = new ArrayList();
                } else {
                    l = new ArrayList(c);
                }
                return (makeReadOnly) 
                		? Collections.unmodifiableList(l)
                		: l;
            }
            public boolean isReadOnly() {
                return makeReadOnly;
            }            
        };
    }
    
    public static void verifyImutability(List l) {
        
        String message = "Expected UnsupportedOperationException.";
        
        try {
            l.add(0, new Integer(0));
            TestSupport.assertThat(false, message);
        } catch (UnsupportedOperationException e) {}
        
        try {
            l.add(new Integer(0)); 
            TestSupport.assertThat(false, message);
        } catch (UnsupportedOperationException e) {}
        
        try {
            l.addAll(null);  
            TestSupport.assertThat(false, message);
        } catch (UnsupportedOperationException e) {}
        
        try {
            l.addAll(0, null); 
            TestSupport.assertThat(false, message);
        } catch (UnsupportedOperationException e) {}
        
        try {
            l.clear();      
            TestSupport.assertThat(false, message);
        } catch (UnsupportedOperationException e) {}
        
        try {
            l.remove(0); 
            TestSupport.assertThat(false, message);
        } catch (UnsupportedOperationException e) {}
        
        try {
            l.remove(new Object());
            TestSupport.assertThat(false, message);
        } catch (UnsupportedOperationException e) {}
        
        try {
            l.removeAll(null); 
            TestSupport.assertThat(false, message);
        } catch (UnsupportedOperationException e) {}
        
        try {
            l.retainAll(null);
            TestSupport.assertThat(false, message);
        } catch (UnsupportedOperationException e) {}
        try {
            l.set(0,new Integer(0));
            TestSupport.assertThat(false, message);
        } catch (UnsupportedOperationException e) {}   
    }
    
    private final List nullList;
    
    protected List defaultList() {
        List l = new ArrayList();
        for (int i = 0; i < 4; i++) {
            l.add(new Integer(i));
        }
        return newInstance(l);
    }
    
    /**
     * Implementations must supply an empty list if the collection is null.
     * @param c Initial collection or null for empty.
     * @return the List instance
     */
    public List newInstance(Collection c) {
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
      
        if(!isReadOnly()) {     
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
        TestSupport.assertThat(defaultList().get(0).equals(new Integer(0)),
        		"get() did not return expected value of Integer(0)");
        try {
            defaultList().get(-1);
            TestSupport.assertThat(false, 
            		"get(<negative index>) did not throw IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {}
        
        try {
            defaultList().get(-1);
            TestSupport.assertThat(false, 
            		"get(<index too big>) did not throw IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {}
    }
    
    /** Tests set(int index, Object element) */
    public void test_set() {
        
        try {
            newInstance(null).set(-1, "spam");
            TestSupport.assertThat(false, 
            		"get(<negative index>) did not throw IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {}
        
        try {
            newInstance(null).set(0, "spam");
            TestSupport.assertThat(false, 
            	"set(<index too big>) did not throw IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {}
        
        List a = defaultList();
        a.set(a.size() - 1 , "spam");
        TestSupport.assertThat(a.get(a.size() - 1).equals("spam"), 
        		"set() object was not retrieved via get()");
    }
    
    /** Tests add(Object o) */ 
    public void test_add() {    
        List a = newInstance(null);
        for (int i = 0; i < 4; i++) {
            a.add(new Integer(i));
        }
        TestSupport.assertEquals(a, defaultList(), "add(Object o) failed");
    }
    
    /** Tests isEmpty() */
    public void test_isEmpty() {
        List a = newInstance(null);
        TestSupport.assertThat(a.isEmpty(), 
                "isEmpty() is false on an emtpy List");
        a.addAll(defaultList());
        TestSupport.assertThat(!a.isEmpty(), 
                "isEmpty() is true on a non-empty List)" );
        a.clear();
        TestSupport.assertThat(a.isEmpty(), 
                "isEmpty() is false on an emtpy List");        
    }
    
    /** Tests size() */
    public void test_size() { 
        List b = newInstance(null);
        TestSupport.assertThat(b.size() == 0, "empty list size was not 0");
        TestSupport.assertThat(defaultList().size() == 4, 
                "default list did not have a size of 4");     
    }
    
    /** Tests  add(int index, Object element) */       
    public void test_add_index() {
        List a = newInstance(null);
        List b = defaultList();
        for (int i = 0; i < b.size(); i++) {
            a.add(i, b.get(i));
        }
        
        try {
            a.add(a.size() + 1, new Integer(a.size() + 1));  
            TestSupport.assertThat(false, "expected IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {}
        
        try {
            a.add(-1, new Integer(-1));  
            TestSupport.assertThat(false, "expected IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {}                           
    }
    
    /** Tests  equals(Object o)*/       
    public void test_equals() {
        TestSupport.assertEquals(defaultList(), defaultList(), 
        		"Identical lists weren't equal()");
        TestSupport.assertNotEquals(newInstance(null), defaultList(), 
        		"Different lists were equal()");
        TestSupport.assertNotEquals(newInstance(null), new Object(), 
        		"List was equal to a non-List type");
    }
    
    /** Tests addAll(Collection c) */
    public void test_addAll() {
        List a = defaultList();
        List b = defaultList();
        
        TestSupport.assertThat(a.addAll(b) == true, 
        		"Mutating addAll(Collection) returned false");
        TestSupport.assertThat(a.addAll(newInstance(null)) == false, 
        		"Idempotent addAll(Collection) returned true");
        TestSupport.assertThat(b.addAll(b) == true,
                "Mutating addAll(Collection) returned false");
        TestSupport.assertEquals(a, b, 
                "Expected equal objects from addAll(collection)");
        TestSupport.assertThat(a.size() == 8, 
        		"Expected List to have size 8 after addAll(Collection)");      
    }
    
    /** Tests indexOf(Object o) */       
    public void indexOf() {
        TestSupport.assertThat(defaultList().indexOf(new Integer(3)) == 3, 
                "indexOf(3) did not return 3"); 
        TestSupport.assertThat(defaultList().indexOf(new Integer(42)) == -1, 
        		"indexOf() non-existing entry did not return -1");
        TestSupport.assertThat(defaultList().indexOf(null) == -1, 
        		"indexOf() non-existing null did not return -1"); 
        
    }
    
    /** Tests contains(Object o) */       
    public void test_contains() { 
        TestSupport.assertThat(defaultList().contains(new Integer(42)) == false, 
        		"contains() returned true for non-existing entry");
        TestSupport.assertThat(defaultList().contains(new Integer(0)) == true, 
        		"contains() returned false for existing entry");
        TestSupport.assertThat(nullList.contains(null) == true, 
        		"contains() returned false for existing null entry");
        TestSupport.assertThat(defaultList().contains(null) == false, 
        		"contains() returned true for non-existing null entry");
    }
    
    /** Tests remove(Object o) */
    public void test_remove() {
        List a = defaultList();
        a.add(null);
        TestSupport.assertThat(a.remove(null) == true, 
        		"remove() existing null entry returned false");
        TestSupport.assertThat(a.remove(null) == false, 
        		"remove() non-existing null entry returned false");
        a.add("spam");
        TestSupport.assertThat(a.remove("spam") == true, 
        		"remove() existing entry returned false");
        TestSupport.assertThat(a.remove("spam") == false, 
        		"remove() non-existing entry returned true");
    }
    
    
    /** Tests remove(int index) */
    public void test_remove_index() {    
        
        List a = defaultList();
        for (int i = 0, n = a.size(); i < n; i++) {
            a.remove(0);
        }
        TestSupport.assertThat(a.size() == 0, 
                "remove()-d all entries but size() not 0");
        
        try {
            a.remove(0);
            TestSupport.assertThat(false, 
            		"removing a non-existing index did not throw exception");
        } catch(IndexOutOfBoundsException e) {}       
    }
    
    /** Tests lastIndexOf(Object o) */
    public void test_lastIndexOf() {
        // avoid calling any mutable methods 
        List l = new ArrayList(defaultList());
        l.add(new Integer(0));
        
        // now get the immutable version
        List a = newInstance(l);
        
        TestSupport.assertThat(a.lastIndexOf(new Integer(0)) == 4, 
                "lastIndexOf() did not return 4");
        TestSupport.assertThat(a.lastIndexOf(new Integer(42)) == -1, 
        		"lastIndexOf() non-existing value did not return -1");    
    }
    
    /** Tests removeAll(Collection c) */       
    public void test_removeAll() {
        List a = defaultList();
        TestSupport.assertThat(a.removeAll(a) == true, 
        		"mutating removeAll() did not return true");
        TestSupport.assertThat(a.removeAll(a) == false, 
        		"idempotent removeAll did not return false");
        TestSupport.assertThat(a.removeAll(nullList) == false, 
        		"idempotent removeAll did not return false");
        
        List yanl = newInstance(null); 
        yanl.addAll(nullList);
        TestSupport.assertThat(yanl.removeAll(nullList) == true, 
        		"mutating removeAll() did not return true");
        TestSupport.assertThat(yanl.size() == 0, 
                "empty list had non-zero size");
        TestSupport.assertThat(yanl.removeAll(newInstance(null)) == false,
        		"idempotent removeAll did not return false");
        
    }
    
    /** Tests addAll(int index, Collection c) */       
    public void test_addAll_index() {
        List a = defaultList();
        List b = newInstance(null);
        TestSupport.assertThat(b.addAll(0,a) == true, 
        		"mutating addAll(index, Collection) did not return true");
        TestSupport.assertEquals(a, b, 
                "addAll(index, Collection) instances failed equals test");
        TestSupport.assertThat(a.addAll(0, newInstance(null)) == false, 
        		"idempotent addAll(index, Collection) did not return false");  
        TestSupport.assertThat(b.addAll(0,b) == true, 
                "mutating addAll(index, Collection) did not return true");
        
        // Since PyObjectList has some specific handling when it detects
        // addAll on a PySequenceList, make sure the general case works.
        b = newInstance(null);
        b.addAll(new ArrayList(defaultList()));
        TestSupport.assertEquals(defaultList(), b,
                "addAll(index, <ArrayList>) failed equals test");
    }

    
    /** Tests  hashCode() */       
    public void test_hashCode() {
        List a = defaultList();
        TestSupport.assertThat(a.hashCode() == defaultList().hashCode(), 
        		"Instances with same internal state have different hashcode");
        TestSupport.assertThat(a.hashCode() != newInstance(null).hashCode(), 
        		"Instances with different internal state have the same hashcode");
        
        if (isReadOnly() == false) {
            List b = newInstance(null);
            b.addAll(a);
            b.remove(0);
            TestSupport.assertThat(a.hashCode()!= b.hashCode(), 
            		"Instances with different internal state have the same hashcode");
        }
        
    }
    
    /** Tests clear() */       
    public void test_clear() {
        List a = defaultList();
        a.clear();
        TestSupport.assertThat(a.size() == 0, 
                "clear()-ed list did not have size of 0");
    }
    
    /** Tests subList(int fromIndex, int toIndex) */       
    public void test_subList() {
        List a = defaultList();
        TestSupport.assertThat((a.subList(0, a.size()) != a), 
                "subList() returned the same instance");
        TestSupport.assertEquals(a.subList(0, a.size()), a, 
                "Complete subList() did not equal original List");
        TestSupport.assertThat(a.subList(0,0).size() == 0, 
                "empty subList had non-zero size");
        
        try {
            a.subList(-1,1);
            TestSupport.assertThat(false, "Expected IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {}
        
        try {
            a.subList(1,0);
            TestSupport.assertThat(false, "Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {}
        
        try {
            a.subList(0,a.size() + 1);
            TestSupport.assertThat(false, "Expected IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {}
        
        if (!isReadOnly()) {
            
            a.subList(0, a.size()).clear();
            TestSupport.assertThat(a.size() == 0, 
                    "clear()-ed sublist did not have zero size");           
            List c = newInstance(null); 
            c.addAll(defaultList());
            List d = c.subList(1,3);
            TestSupport.assertThat(d.size() == 2, 
            		"Expected subList to have size of 2");
            TestSupport.assertThat(c.set(1,"canned").equals(new Integer(1)), 
                    "subList.set() did not return Integer(1) from index 1" +
            		" of defaultList");
            TestSupport.assertThat(d.get(0).equals("canned"),
            		"subList does not update with changes to parent");
            d.set(0,"spam");
            TestSupport.assertThat(c.get(1).equals("spam"), 
            		"parent does not update with changes to subList child");
        } else {
            List b = a.subList(0, a.size());
            verifyImutability(b);
        }
        
    }
    
    /** Tests retainAll(Collection c) */
    public void test_retainAll() { 
        List a = defaultList();
        a.retainAll(defaultList());
        TestSupport.assertEquals(a, defaultList(), 
        		"retainAll(<equal List>) does not equal original list");
        a = defaultList();
        a.retainAll(newInstance(null));
        TestSupport.assertThat(a.size() == 0,
        		"retainAll(<empty List>))does not have size of zero");
        
        a = defaultList();
        a.remove(0);
        a.remove(0);
        a.add(new Integer(4));
        a.add(new Integer(5));
        List b = newInstance(null);
        b.add(new Integer(2));
        b.add(new Integer(3));
        a.retainAll(b);
        TestSupport.assertEquals(a, b,
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
        TestSupport.assertThat(defaultList().containsAll(defaultList().subList(1,3)), 
        		"containsAll(<subList>) was false");
    }
    
    /** Tests iterator() */
    public void test_iterator() {
        
        TestSupport.assertThat(newInstance(null).iterator().hasNext() == false, 
        		"Iterator for empty list thinks it hasNext()");
        try {
            newInstance(null).iterator().next();
            TestSupport.assertThat(false, "expected NoSuchElementException");
        } catch (NoSuchElementException e) {}
        
        List a = defaultList();
        int i = 0;
        for (Iterator iter = a.iterator(); iter.hasNext(); ) {
            TestSupport.assertThat(iter.next() == a.get(i++), 
            		"Iterator next() failed identity test");
        }
        TestSupport.assertThat(i == a.size(), 
                "Iterator did not iterator over entire list");
    }
    
    public void test_listIterator() {
        
        ListIterator li = newInstance(null).listIterator();
        TestSupport.assertThat(li.hasNext() == false, 
        		"ListIterator.hasNext() is true for empty List");
        
        TestSupport.assertThat(li.hasPrevious() == false, 
        		"ListIterator.hasPrevious() is true for empty List");
        
        try {
            li.next();
            TestSupport.assertThat(false, "expected NoSuchElementException");
        } catch (NoSuchElementException e) {}
        
        try {
            li.previous();
            TestSupport.assertThat(false, "expected NoSuchElementException");
        } catch (NoSuchElementException e) {}
        
        int nextIndex = li.nextIndex();
        TestSupport.assertThat(nextIndex == 0, 
        		"ListIterator.nextIndex() on empty List did not return 0");
        
        int prevIndex = li.previousIndex();
        TestSupport.assertThat(prevIndex == -1,
            "ListIterator.previousIndex() on empty List did not return -1");
        
        List l = new ArrayList();
        l.add(new Integer(1)); 
        li = newInstance(l).listIterator();
        TestSupport.assertThat(li.hasPrevious() == false, 
        		"ListIterator.hasPrevious() is true with nothing previous");
        
        TestSupport.assertThat(li.hasNext() == true, 
        		"ListIterator.hasNext() is false with next present");
        TestSupport.assertThat(li.next().equals(new Integer(1)), 
                "ListIterator.next() did not return expected Integer(1)");
       
        if (!isReadOnly()) {
	        li.remove();
	        TestSupport.assertThat(li.hasNext() == false, 
	        		"ListIterator.hasNext() is true for empty List");
	        
	        TestSupport.assertThat(li.hasPrevious() == false, 
	        		"ListIterator.hasPrevious() is true for empty List");
	        try {
	            li.set(new Integer(42));
	            TestSupport.assertThat(false, "expected IllegalStateException");
	        } catch (IllegalStateException e) {}
	        
	        try {
	            li.remove();
	            TestSupport.assertThat(false, "expected IllegalStateException");
	        } catch (IllegalStateException e) {}    
        }
        
        l = new ArrayList();
        l.add(new Integer(0));
        l.add(new Integer(1));
        l.add(new Integer(2));
        
        li = newInstance(l).listIterator();
        
        for (int i = 0, n = l.size(); i < n; i++) {
            TestSupport.assertThat(li.next().equals(new Integer(i)),
            		"ListIterator.previous did not return expected value");
        }
        
        while (!isReadOnly() && li.hasNext()) {
            li.next();
            li.set(new Integer(42));
            TestSupport.assertThat(li.previous().equals(new Integer(42)),
            		"ListIterator.previous() did not return the value that was set()");
            li.remove();
        }
        
        if(isReadOnly()) {
            li = newInstance(null).listIterator();
        }
        
        li = defaultList().listIterator(2);
        TestSupport.assertThat(li.next().equals(new Integer(2)), 
        		"List.listIteraor(index) did not return expected value");
        TestSupport.assertThat(li.next().equals(new Integer(3)), 
        		"List.listIteraor(index) did not return expected value");
        TestSupport.assertThat(li.hasNext() == false, 
        		"listIterator.hasNext() at end of list returned true");    
        
    }
    
    /** Tests toArray() */
    public void test_toArray() {
        Object[] intObjArray = new Integer[] {
                new Integer(0), new Integer(1), new Integer(2), new Integer(3)}; 
        TestSupport.assertThat(Arrays.equals(defaultList().toArray(), intObjArray), 
        		"toArray() did not return the expected Integer[] array");
    }
    
    /** Tests toArray(Object[] a)  */
    public void test_toArray_typed() {
        Object[] intObjArray = new Integer[] {
                new Integer(0), new Integer(1), new Integer(2), new Integer(3)}; 
        TestSupport.assertThat(Arrays.equals(
                defaultList().toArray(new Integer[] {}), intObjArray), 
        		"toArray(Integer[]) did not return the expected Integer[] array");
    }
    
    // Can't test clone since it's not visible, but it is from jython.    
    //    /** Tests clone() */
    //    public void test_clone() {
    //        List a = defaultList();
    //        TestSupport.assertEquals(((Object)a).clone(), a, 
    //        	     "clone() was not equal to original");
    //        TestSupport.assertThat(a.clone() != a, 
    //               "clone() returned same instance");
    //    }
}
