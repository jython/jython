// Copyright © Corporation for National Research Initiatives
package org.python.core;

class ReflectedCallData {
    public Object[] args;
    public int length;
    public Object self;
    public int errArg;

    public ReflectedCallData() {
        args = Py.EmptyObjects;
        length = 0;
        self = null;
        errArg = -2;
    }
    
    public void setLength(int newLength) {
        length = newLength;
        if (newLength <= args.length)
	    return;
        args = new Object[newLength];
    }
    
    public Object[] getArgsArray() {
        if (length == args.length)
            return args;
        Object[] newArgs = new Object[length];
        System.arraycopy(args, 0, newArgs, 0, length);
        args = newArgs;
        return newArgs;
    }
}
