package org.python.core;

import java.lang.ref.WeakReference;
import java.lang.ref.ReferenceQueue;
import java.util.HashMap;

public class IdImpl2 extends IdImpl {
    
    public static class WeakIdentityMap {
        
        private ReferenceQueue refqueue = new ReferenceQueue();
        private HashMap hashmap = new HashMap();
        
        private void cleanup() {
            Object k;
            while ((k = refqueue.poll()) != null) {
                hashmap.remove(k);
            }
        }
                
        private class WeakIdKey extends WeakReference {
            private int hashcode;
            
            WeakIdKey(Object obj) {
                super(obj,refqueue);
                hashcode = System.identityHashCode(obj);                
            }
            
            public int hashCode() {
                return hashcode;
            }
            
            public boolean equals(Object other) {
                Object obj = this.get();
                if (obj != null) {
                    return obj == ((WeakIdKey)other).get();
                } else {
                    return this == other;
                }                
            }
        }
        
        public int _internal_map_size() {
            return hashmap.size();
        }
        
        public void put(Object key,Object val) {
            cleanup();
            hashmap.put(new WeakIdKey(key),val);
        }
        
        public Object get(Object key) {
            cleanup();
            return hashmap.get(new WeakIdKey(key));
        }

        public void remove(Object key) {
            cleanup();
            hashmap.remove(new WeakIdKey(key));        
        }

    }

    private WeakIdentityMap id_map = new WeakIdentityMap();
    private long sequential_id = 0; 

    public long id(PyObject o) {
        if (o instanceof PyJavaInstance) {
            return java_obj_id(((PyJavaInstance)o).javaProxy);
        } else {
            return java_obj_id(o);
        }            
    }

    // XXX maybe should display both this id and identityHashCode
    // XXX preserve the old "at ###" style?
    public String idstr(PyObject o) {
        return Long.toString(id(o));
    }

    public synchronized long java_obj_id(Object o) {
        Long cand = (Long)id_map.get(o);
        if (cand == null) {
            sequential_id++;
            long new_id = sequential_id;           
            id_map.put(o,new Long(new_id));
            return new_id;            
        }
        return cand.longValue();
    }
    
}
