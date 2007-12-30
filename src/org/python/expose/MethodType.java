package org.python.expose;

public enum MethodType {
    /** Return this method's value unmolested. */
    DEFAULT, 
    /** If a method returns null, raise a NotImplemented.*/
    BINARY, 
    /** Only for __cmp__ methods.  If it returns -2, raise a TypeError */
    CMP
}
