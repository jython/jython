package org.python.core;

public class PyEnumerate extends PyIterator {

    private long en_index;          /* current index of enumeration */
	private PyObject en_sit;        /* secondary iterator of enumeration */
	private PyTuple en_result;      /* result tuple  */
    protected static PyObject __methods__;

    static {
        PyList list = new PyList();
        String[] methods = {"next"};
        list.append(new PyString(methods[0]));
        __methods__ = list;
    }

    public PyEnumerate(PyObject seq) {
        en_index = 0;
        en_sit = seq.__iter__();
    }

    public PyObject __iternext__() {
        PyObject next_item;
        PyObject next_index;

        next_item = en_sit.__iternext__();
        if(next_item == null)
            return null;
        next_index = new PyInteger((int)en_index);
        en_index++;

        en_result = new PyTuple(new PyObject[] {next_index, next_item});
        return en_result;
    }

    public PyObject __findattr__(String name) {
        if (name.equals("__methods__")) {
            return __methods__;
        }
        return super.__findattr__(name);
    }

    public String toString() {
        return "<enumerate object "+Py.idstr(this)+">";
    }
}
