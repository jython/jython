// Copyright © Corporation for National Research Initiatives
package org.python.core;

public class PyTuple extends PySequence {
    public PyObject list[];

    public static PyClass __class__;

    public PyTuple(PyObject elements[]) {
        super(__class__);
        list = elements;
    }

    protected String safeRepr(PyObject o) {
        if (o == null)
            return "null";
        return o.__repr__().toString();
    }

    public int __len__() { 
        return list.length; 
    }

    protected PyObject get(int i) {
        return list[i];
    }

    protected PyObject getslice(int start, int stop, int step) {
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

    public PyObject __add__(PyObject generic_other) {
        if (generic_other instanceof PyTuple) {
            PyTuple other = (PyTuple)generic_other;
            PyObject new_list[] = new PyObject[list.length+other.list.length];
            System.arraycopy(list, 0, new_list, 0, list.length);
            System.arraycopy(other.list, 0, new_list, list.length,
                             other.list.length);

            return new PyTuple(new_list);
        } else {
            return null;
        }
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
    /*public boolean equals(Object other) {
      if (other instanceof PyTuple && ((PyTuple)other).size() == list.length) {
      Object[] ol = ((PyTuple)other).list;
      for(int i=0; i<list.length; i++) {
      if (!ol[i].equals(list[i])) return false;
      }
      return true;
      }
      return true;
      }*/
        
    public String toString() {
        StringBuffer buf = new StringBuffer("(");
        for (int i=0; i<list.length-1; i++) {
            buf.append(safeRepr(list[i]));
            buf.append(", ");
        }
        if (list.length > 0)
            buf.append(safeRepr(list[list.length-1]));
        if (list.length == 1)
            buf.append(",");
        buf.append(")");

        return buf.toString();
    }
}
