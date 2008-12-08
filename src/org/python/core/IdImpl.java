package org.python.core;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.HashMap;

public class IdImpl {

    public static class WeakIdentityMap {
        
        private transient ReferenceQueue refqueue = new ReferenceQueue();
        private HashMap hashmap = new HashMap();
        
        private void cleanup() {
            Object k;
            while ((k = this.refqueue.poll()) != null) {
                this.hashmap.remove(k);
            }
        }
                
        private class WeakIdKey extends WeakReference {
            private int hashcode;
            
            WeakIdKey(Object obj) {
                super(obj,WeakIdentityMap.this.refqueue);
                this.hashcode = System.identityHashCode(obj);                
            }
            
            public int hashCode() {
                return this.hashcode;
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
            return this.hashmap.size();
        }
        
        public void put(Object key,Object val) {
            cleanup();
            this.hashmap.put(new WeakIdKey(key),val);
        }
        
        public Object get(Object key) {
            cleanup();
            return this.hashmap.get(new WeakIdKey(key));
        }

        public void remove(Object key) {
            cleanup();
            this.hashmap.remove(new WeakIdKey(key));        
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

    public String idstr(PyObject o) {
        return String.format("0x%x", id(o));
    }

    public synchronized long java_obj_id(Object o) {
        Long cand = (Long)this.id_map.get(o);
        if (cand == null) {
            this.sequential_id++;
            long new_id = this.sequential_id;           
            this.id_map.put(o,new Long(new_id));
            return new_id;            
        }
        return cand.longValue();
    }
}
