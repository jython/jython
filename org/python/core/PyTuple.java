// Copyright © Corporation for National Research Initiatives
package org.python.core;



class TupleFunctions extends PyBuiltinFunctionSet 
{
    TupleFunctions(String name, int index, int argcount) {
        super(name, index, argcount, argcount, true, null);
    }

    public PyObject __call__() {
        PyTuple tuple = (PyTuple)__self__;
        switch (index) {
        case 3:
            return new PyInteger(tuple.__len__());
        default:
            throw argCountError(0);
        }
    }

    public PyObject __call__(PyObject arg) {
        PyTuple tuple = (PyTuple)__self__;
        switch (index) {
        case 15:
            return tuple.__add__(arg);
        default:
            throw argCountError(1);
        }
    }
}



public class PyTuple extends PySequence implements ClassDictInit
{
    // TBD: this should not be public, but it is required to be public by
    // many classes, including the thread.java and PyClass.java classes.
    // URG!  This should be fixed.
    public PyObject[] list;

    public static void classDictInit(PyObject dict) {
        PySequence.classDictInit(dict);
        dict.__setitem__("__len__", new TupleFunctions("__len__", 3, 0));
        dict.__setitem__("__add__", new TupleFunctions("__add__", 15, 1));
        // hide these from Python!
        dict.__setitem__("classDictInit", null);
        dict.__setitem__("toString", null);
        dict.__setitem__("hashCode", null);
    }

    public PyTuple() {
        this(Py.EmptyObjects);
    }

    public PyTuple(PyObject elements[]) {
        list = elements;
    }

    protected String safeRepr() {
        return "'tuple' object";
    }

    protected PyObject get(int i) {
        return list[i];
    }

    protected PyObject getslice(int start, int stop, int step) {
        if (step > 0 && stop < start)
            stop = start;
        int n = sliceLength(start, stop, step);
        PyObject[] newList = new PyObject[n];
                
        if (step == 1) {
            System.arraycopy(list, start, newList, 0, stop-start);
            return new PyTuple(newList);
        }
        int j = 0;
        for (int i=start; j<n; i+=step) {
            newList[j] = list[i];
            j++;
        }
        return new PyTuple(newList);
    }

    protected PyObject repeat(int count) {
        int l = list.length;
        PyObject[] newList = new PyObject[l*count];
        for (int i=0; i<count; i++) {
            System.arraycopy(list, 0, newList, i*l, l);
        }
        return new PyTuple(newList);
    }

    public int __len__() { 
        return list.length; 
    }

    public PyObject __add__(PyObject generic_other) {
        if (generic_other instanceof PyTuple) {
            PyTuple other = (PyTuple)generic_other;
            PyObject new_list[] = new PyObject[list.length+other.list.length];
            System.arraycopy(list, 0, new_list, 0, list.length);
            System.arraycopy(other.list, 0, new_list, list.length,
                             other.list.length);

            return new PyTuple(new_list);
        } else
            return null;
    }

    public int hashCode() {
        int x, y;
        int len = list.length;
        x = 0x345678;

        for (len--; len>=0; len--) {
            y = list[len].hashCode();
            x = (x + x + x) ^ y;
        }
        x ^= list.length;
        return x;
    }


    // Should go away when compare works properly
//     public boolean equals(Object other) {
//         if (other instanceof PyTuple &&
//             ((PyTuple)other).size() == list.length)
//         {
//             Object[] ol = ((PyTuple)other).list;
//             for(int i=0; i<list.length; i++) {
//                 if (!ol[i].equals(list[i])) return false;
//             }
//             return true;
//         }
//         return true;
//     }
        
    private String subobjRepr(PyObject o) {
        if (o == null)
            return "null";
        return o.__repr__().toString();
    }

    public String toString() {
        StringBuffer buf = new StringBuffer("(");
        for (int i = 0; i < list.length-1; i++) {
            buf.append(subobjRepr(list[i]));
            buf.append(", ");
        }
        if (list.length > 0)
            buf.append(subobjRepr(list[list.length-1]));
        if (list.length == 1)
            buf.append(",");
        buf.append(")");
        return buf.toString();
    }
}
