// Copyright =A9 Corporation for National Research Initiatives
// These are just like normal instances, except that their classes included
// a definition for __del__(), i.e. Python's finalizer.  These two instance
// types have to be separated due to Java performance issues.

package org.python.core;

public class PyFinalizableInstance extends PyInstance
{
    public PyFinalizableInstance(PyClass iclass) {
        super(iclass);
    }

    // __del__ method is invoked upon object finalization.
    protected void finalize() {
        __class__.__del__.__call__(this);
    }
}