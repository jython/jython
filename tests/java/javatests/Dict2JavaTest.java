/* Copyright (c) Jython Developers */
package javatests;

import java.util.Map;
import java.util.Set;
import java.util.Collection;
import java.util.HashMap;

/**
 * This class is used by the test_dict2java.py test script for testing
 * the org.python.core.PyDictionary object's java.util.Map. We verifiy
 * that the Map interface can be seamlessly passed to java code and
 * manipulated in a consistent manner.
 */
public class Dict2JavaTest {
    
    private Map<Object, Object> map;

    public Dict2JavaTest(Map<Object, Object> map) {
        this.map = map;
    }

    public Set<Map.Entry<Object, Object>> entrySet() {
        return map.entrySet();
    }

    public Set<Object> keySet() {
        return map.keySet();
    }
    
    public Collection<Object> values() {
        return map.values();
    }
    
    public Object put(Object key, Object val) {
        return map.put(key, val);
    }
    
    
    public boolean containsKey(Object key) {
        return map.containsKey(key.toString());
    }

    public boolean test_putAll_efg() {
        HashMap<String, String> hmap = new HashMap<String, String>();
        hmap.put("e", "1");
        hmap.put("f", null);
        hmap.put("g", "2");
        map.putAll(hmap);
        return true;
    }

    public boolean test_remove_ac() {
        Object val1 = map.remove("a");
        Object val2 = map.remove("c");
        Object val3 = map.remove("bar");
        return val1.equals("x") && val2.equals("z") && val3 == null;
    }

    public boolean test_get_gd() {
        return map.get("b").equals("y") && map.get("d") == null
          && map.get(null).equals("foo");
    }

    public boolean test_put_hig() {
        map.put("h", null);
        map.put("i", new Integer(3));
        Object val = map.put("g", "3");
        return val.equals("2");
    }

    public boolean test_java_mapentry() {
        // created outside of Jython with non PyOjects
        HashMap<String, Object> hmap = new HashMap<String, Object>();
        hmap.put("b", "y");
        Map.Entry<String, Object> entry = hmap.entrySet().iterator().next();
        if (!map.entrySet().contains(entry)) return false;

        // Test a number
        hmap = new HashMap<String, Object>();
        hmap.put("i", new Integer(3));
        entry = hmap.entrySet().iterator().next();
        if (!map.entrySet().contains(entry)) return false;

        // test Null
        hmap = new HashMap<String, Object>();
        hmap.put("f", null);
        entry = hmap.entrySet().iterator().next();
        if (!map.entrySet().contains(entry)) return false;
        return true;
    }

    // make sure nulls are handled and other object types, nulls
    // should never match anything in the entry set.
    public boolean test_entry_set_nulls() {
        Set<Map.Entry<Object, Object>> set = map.entrySet();
        return set.contains(null) == false  && set.remove(null) == false &&
          set.contains(new Boolean(true)) == false && set.remove(new String("")) == false;
    }
  
    
    public void remove(Object key) {
        // toString so we insure there are no PyObject influences
        map.remove(key.toString());
    }

    public int size() {
        return map.size();
    }
    
}
