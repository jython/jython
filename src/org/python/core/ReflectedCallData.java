// Copyright (c) Corporation for National Research Initiatives
package org.python.core;

class ReflectedCallData {
    public Object[] args;

    public int length;

    public Object self;

    public int errArg;

    public ReflectedCallData() {
        this.args = Py.EmptyObjects;
        this.length = 0;
        this.self = null;
        this.errArg = -2;
    }

    public void setLength(int newLength) {
        this.length = newLength;
        if (newLength <= this.args.length) {
            return;
        }
        this.args = new Object[newLength];
    }

    public Object[] getArgsArray() {
        if (this.length == this.args.length) {
            return this.args;
        }
        Object[] newArgs = new Object[this.length];
        System.arraycopy(this.args, 0, newArgs, 0, this.length);
        this.args = newArgs;
        return newArgs;
    }
}
