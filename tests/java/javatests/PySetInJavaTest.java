package javatests;

import java.util.Iterator;
import java.util.Random;
import java.util.Set;

import org.python.core.PySet;
import org.python.core.PyTuple;
import org.python.util.Generic;

public class PySetInJavaTest {

    @SuppressWarnings("unchecked")
    public static Set<Object> createPySetContainingJavaObjects() {
        PySet s = new PySet();
        s.add("value");
        s.add(new Random());
        return s;
    }

    public static void testPySetAsJavaSet() {
        PySet s = new PySet();
        String v = "value";
        check(s.add(v));// Add a String as it should be wrapped in PyString
        check(!s.add(v));
        String[] asArray = (String[])s.toArray(new String[0]);// The array type should be the same
        // and it should be resized properly
        check(asArray.length == 1);
        check(asArray[0] == v);
        Object[] naiveArray = s.toArray();
        check(naiveArray.length == 1);
        check(naiveArray[0] == v);
        // Add a Random as it should be wrapped in a generic PyObject; go through addAll to give it
        // a little exercise
        Random rand = new Random();
        check(s.addAll(Generic.list(rand)));
        check(!s.addAll(Generic.list(rand, v)));
        naiveArray = s.toArray();
        check(naiveArray.length == 2);
        for (Object object : naiveArray) {
            if (object instanceof String) {
                check(object == v);
            } else {
                check(object == rand, "Should be 'value' or rand, not " + object);
            }
        }
        check(!s.remove(new Random()), "The Random in the set shouldn't match a new Random");
        check(s.remove(rand));
        check(s.removeAll(Generic.list(rand, v)),
              "The set should contain v and indicate it removed it");
        check(s.isEmpty());
        check(s.addAll(Generic.list(rand, v)));
        check(2 == s.size(), "There should be 2 items, not " + s.size());
        check(s.containsAll(Generic.list(rand, v)));
        check(!s.containsAll(Generic.list(rand, v, "other")));
        check(s.retainAll(Generic.list(rand)));
        check(!s.retainAll(Generic.list(rand)));
        check(s.addAll(Generic.list(rand, v)));
        check(2 == s.size(), "There should be 2 items, not " + s.size());
        check(!s.addAll(Generic.list(rand, v)));
        check(2 == s.size(), "There should be 2 items, not " + s.size());
    }

    public static void accessAndRemovePySetItems(Set<Object> items) {
        check(items instanceof PySet, "The set shouldn't be __tojava'd into " + items.getClass());
        check(items.size() == 3, "Should be 3 items, not " + items.size());
        check(items.contains("value"), "The set from Python should contain 'value'");
        check(!items.contains(new Random()), "The set contains a particular Random");
        Iterator<Object> it = items.iterator();
        while (it.hasNext()) {
            Object object = it.next();
            check(items.contains(object), "The set should contain all items from its iterator");
            if (object instanceof String) {
                check(object.equals("value"), "The string should be 'value', not '" + object + "'");
            } else {
                check(object instanceof Random || object instanceof PyTuple,
                      "The objects in the set should be a String, a Random or a PyTuple, not a "
                              + object.getClass());
            }
            it.remove(); // Tests that removing on the iterator works
        }
    }

    private static void check(boolean testVal) {
        check(testVal, "");
    }

    private static void check(boolean testVal, String failMsg) {
        if (!testVal) {
            throw new RuntimeException(failMsg);
        }
    }
}
