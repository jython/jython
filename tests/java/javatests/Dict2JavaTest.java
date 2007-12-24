package javatests;
import java.util.Map;
import java.util.Set;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

/**
 * This class is used by the test_dict2java.py test script for testing
 * the org.python.core.PyDictionary object's java.util.Map. We verifiy
 * that the Map interface can be seamlessly passed to java code and
 * manipulated in a consistent manner.
 */
public class Dict2JavaTest {
    
    private Map map = null;

    public Dict2JavaTest(Map map) {
        this.map = map;
    }
    
    public Set entrySet() {
        return map.entrySet();
    }

    public Set keySet() {
        return map.keySet();
    }
    
    public Collection values() {
        return map.values();
    }
    
    public Object put(Object key, Object val) {
        return map.put(key, val);
    }
    
    
    public boolean containsKey(Object key) {
        return map.containsKey(key.toString());
    }

    public boolean test_putAll_efg() {
        HashMap hmap = new HashMap();
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
        HashMap hmap = new HashMap();
        hmap.put("b", "y");
        Map.Entry entry = (Map.Entry)hmap.entrySet().iterator().next();
        if (!map.entrySet().contains(entry)) return false;

        // Test a number
        hmap = new HashMap();
        hmap.put("i", new Integer(3));
        entry = (Map.Entry)hmap.entrySet().iterator().next();
        if (!map.entrySet().contains(entry)) return false;

        // test Null
        hmap = new HashMap();
        hmap.put("f", null);
        entry = (Map.Entry)hmap.entrySet().iterator().next();
        if (!map.entrySet().contains(entry)) return false;
        return true;
    }

    // make sure nulls are handled and other object types, nulls
    // should never match anything in the entry set.
    public boolean test_entry_set_nulls() {
        Set set = map.entrySet();
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
