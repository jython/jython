// Copyright (c) Corporation for National Research Initiatives
package org.python.core;

class ReflectedCallData {

    /** errArg value if the self passed to a call wasn't compatible with the type of the method.  */
    public static final int UNABLE_TO_CONVERT_SELF = -1;

    /** errArg value if the method has no forms that take the number of arguments given. */
    public static final int BAD_ARG_COUNT = -2;

    public Object[] args = Py.EmptyObjects;

    public int length;

    public Object self;

    /**
     * Either {@link #BAD_ARG_COUNT}, {@link #UNABLE_TO_CONVERT_SELF}, or the index of the
     * unconvertible argument in args.
     */
    public int errArg = BAD_ARG_COUNT;

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
