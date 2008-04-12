// Copyright (c) Corporation for National Research Initiatives
package org.python.core;

/**
 * The builtin xrange type.
 *
 * Significant patches contributed by Jason Orendorff -
 * jorendor@cbu.edu
 * 
 * @author Jim Hugunin - hugunin@python.org
 * @since JPython 0.3
 */
public class PyXRange extends PySequence {

    /** directly from xrange(start, stop, step) */
    public int start, stop, step;

    /** The length of an uncopied xrange */
    int cycleLength;

    /** The number of copies made (used to implement
     * xrange(x,y,z)*n) */
    int copies;

    public PyXRange(int stop) {
        this(0, stop, 1);
    }

    public PyXRange(int start, int stop) {
        this(start, stop, 1);
    }

    public PyXRange(int start, int stop, int step) {
        if (step == 0) {
            throw Py.ValueError("xrange() arg 3 must not be zero");
        }
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
        return cycleLength * copies;
    }

    private int getInt(int i) {
        if (cycleLength == 0) {
            // avoid divide by zero errors
            return start;
        } else {
            return start + (i % cycleLength) * step;
        }
    }

    protected PyObject pyget(int i) {
        return new PyInteger(getInt(i));
    }

    protected PyObject getslice(int start, int stop, int step) {
        return null;
    }

    protected PyObject repeat(int howmany) {
        return null;
    }

    public int hashCode() {
        // Not the greatest hash function
        // but then again hashing xrange's is rather uncommon
        return stop ^ start ^ step;
    }

    public String toString() {
        StringBuffer buf = new StringBuffer("xrange(");
        if (start != 0) {
            buf.append(start);
            buf.append(", ");
        }
        buf.append(__len__() * step + start);
        if (step != 1) {
            buf.append(", ");
            buf.append(step);
        }
        buf.append(")");
        return buf.toString();
    }

    /** {@inheritDoc} */
    protected String unsupportedopMessage(String op, PyObject o2) {
        return null;
    }
}
