// Copyright (c) Corporation for National Research Initiatives
package org.python.core;

/**
Used to implement the builtin xrange function.

Significant patches contributed by Jason Orendorff - jorendor@cbu.edu

@author Jim Hugunin - hugunin@python.org
@since JPython 0.3
**/

public class PyXRange extends PySequence {
    public int start, stop, step; // directly from xrange(start, stop, step)
    int cycleLength;       // The length of an uncopied xrange
    int copies;            // The number of copies made (used to implement
                           // xrange(x,y,z)*n)

    public PyXRange(int start, int stop, int step) {
        if (step == 0)
            throw Py.ValueError("zero step for xrange()");
        this.start = start;
        this.stop = stop;
        this.step = step;
        int oneLessThanStep = step + (step > 0 ? -1 : 1);
        cycleLength = (stop - start + oneLessThanStep) / step;
        if (cycleLength < 0) {
            cycleLength = 0;
        }
        this.stop = start + cycleLength*step;
        copies = 1;
    }

    public int __len__() {
        return cycleLength*copies;
    }

    private int getInt(int i) {
        if (cycleLength == 0) { // avoid divide by zero errors
            return start;
        } else {
            return start + (i % cycleLength)*step;
        }
    }

    protected PyObject get(int i) {
        return new PyInteger(getInt(i));
    }

    protected PyObject getslice(int start, int stop, int step) {
        Py.DeprecationWarning("xrange object slicing is deprecated; " +
	                      "convert to list instead");
        if (copies != 1) {
            throw Py.TypeError("cannot slice a replicated range");
        }
        int len = sliceLength(start, stop, step);
        int xslice_start = getInt(start);
        int xslice_step = this.step * step;
        int xslice_stop = xslice_start + xslice_step * len;
        return new PyXRange(xslice_start, xslice_stop, xslice_step);
    }


    protected PyObject repeat(int howmany) {
        Py.DeprecationWarning("xrange object multiplication is deprecated; " +
	                      "convert to list instead");
        PyXRange x = new PyXRange(start, stop, step);
        x.copies = copies*howmany;
        return x;
    }

    public PyObject __add__(PyObject generic_other) {
        throw Py.TypeError("cannot concatenate xrange objects");
    }

    public PyObject __findattr__(String name) {
        String msg = "xrange object's 'start', 'stop' and 'step' " +
		     "attributes are deprecated";
        if (name == "start") {
            Py.DeprecationWarning(msg);
            return Py.newInteger(start);
        } else if (name == "stop") {
            Py.DeprecationWarning(msg);
            return Py.newInteger(stop);
        } else if (name == "step") {
            Py.DeprecationWarning(msg);
            return Py.newInteger(step);
        } else {
            return super.__findattr__(name);
        }
    }

    public int hashCode() {
        // Not the greatest hash function
        // but then again hashing xrange's is rather uncommon
        return stop^start^step;
    }

    public String toString() {
        StringBuffer buf = new StringBuffer("(");
        int count = __len__();
        for (int i=0; i<(count-1); i++) {
            buf.append(get(i).__repr__().toString());
            buf.append(", ");
        }
        if (count > 0)
            buf.append(get(count-1).__repr__().toString());
        if (count == 1)
            buf.append(",");
        buf.append(")");

        return buf.toString();
    }

    public PyList tolist() {
        Py.DeprecationWarning("xrange.tolist() is deprecated; " +
                              "use list(xrange) instead");
        PyList list = new PyList();
        int count = __len__();
        for (int i=0; i<count; i++) {
            list.append(get(i));
        }
        return list;
    }
}
