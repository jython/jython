// Copyright 2000 Samuele Pedroni
 
package org.python.core;

import java.lang.ref.*;
import java.util.*;

public class SoftIInternalTables extends AutoInternalTables {
    
    private class Ref extends SoftReference {
        Object key;
        short type;
        
        Ref(short type,Object key, Object obj) {
            super(obj,queue);
            this.type=type;
            this.key=key;
        }
    }
    
    protected Reference newAutoRef(short type,Object key, Object obj) {
        return new Ref(type,key,obj);
    }
    
    protected short getAutoRefType(Reference ref) {
        return ((Ref)ref).type;
    }
    
    
    protected Object getAutoRefKey(Reference ref) {
        return ((Ref)ref).key;
    }

}