package org.python.core;

public interface MemoryView {
    // readonly attributes XXX just the boring stuff so far

    public String get_format();
    public int get_itemsize();
    public PyTuple get_shape();
    public int get_ndim();
    public PyTuple get_strides();
    public boolean get_readonly();
}
