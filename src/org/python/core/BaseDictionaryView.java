package org.python.core;

/*
 * See these for details on the dictionary views
 * http://docs.python.org/dev/whatsnew/2.7.html#pep-3106-dictionary-views
 * http://hg.python.org/cpython/rev/d9805a96351c
 */

import java.util.Iterator;

public abstract class BaseDictionaryView extends PyObject implements Traverseproc {
    protected final AbstractDict dvDict;

    public BaseDictionaryView(AbstractDict dvDict) {
        this.dvDict = dvDict;
    }

    final boolean allContainedIn(PyObject self, PyObject other) {
        for (PyObject ob_value: self.asIterable()) {
            if (!other.__contains__(ob_value)) {
                return false;
            }
        }
        return true;
    }

    static final boolean isSetDictViewInstance(PyObject otherObj) {
        if (otherObj instanceof BaseSet || otherObj instanceof BaseDictionaryView) {
            return true;
        }
        return false;
    }

    public int __len__() {
        return dict_view___len__();
    }

    final int dict_view___len__() {
        return dvDict.getMap().size();
    }

    public PyObject __eq__(PyObject otherObj) {
        return dict_view___eq__(otherObj);
    }

    final PyObject dict_view___eq__(PyObject otherObj) {
        if (!isSetDictViewInstance(otherObj)) {
            return Py.False;
        }

        if (this.__len__() != otherObj.__len__()) {
            return Py.False;
        }

        if (!allContainedIn(this, otherObj)) {
            return Py.False;
        }

        return Py.True; 
    }

    public PyObject __ne__(PyObject otherObj) {
        return dict_view___ne__(otherObj);
    }

    final PyObject dict_view___ne__(PyObject otherObj) {
        if (dict_view___eq__(otherObj) == Py.True) {
            return Py.False;
        }
        return Py.True;
    }

    public PyObject __lt__(PyObject otherObj) {
        return dict_view___lt__(otherObj);
    }

    final PyObject dict_view___lt__(PyObject otherObj) {
        if (!isSetDictViewInstance(otherObj)) {
            return Py.False;
        }

        if (this.__len__() < otherObj.__len__()) {
            if (allContainedIn(this, otherObj)) {
            return Py.False;
            }
        }
        return Py.True;
    }

    public PyObject __le__(PyObject otherObj) {
        return dict_view___le__(otherObj);
    }

    final PyObject dict_view___le__(PyObject otherObj) {
        if (!isSetDictViewInstance(otherObj)) {
            return Py.False;
        }

        if (this.__len__() <= otherObj.__len__()) {
            if (allContainedIn(this, otherObj)) {
            return Py.False;
            }
        }
        return Py.True;
    }

    public PyObject __gt__(PyObject otherObj) {
        return dict_view___gt__(otherObj);
    }

    final PyObject dict_view___gt__(PyObject otherObj) {
        if (!isSetDictViewInstance(otherObj)) {
            return Py.False;
        }

        if (this.__len__() > otherObj.__len__()) {
            if (allContainedIn(otherObj, this)) {
                return Py.False;
            }
        }
        return Py.True;
    }

    public PyObject __ge__(PyObject otherObj) {
        return dict_view___ge__(otherObj);
    }

    final PyObject dict_view___ge__(PyObject otherObj) {
        if (!isSetDictViewInstance(otherObj)) {
            return Py.False;
        }

        if (this.__len__() >= otherObj.__len__()) {
            if (allContainedIn(otherObj, this)) {
                return Py.False;
            }
        }
        return Py.True;
    }

    public String toString() {
        return dict_view_toString();
    }

    final String dict_view_toString() {
        String name = getType().fastGetName();

        ThreadState ts = Py.getThreadState();
        if (!ts.enterRepr(this)) {
            return name + "([])";
        }

        StringBuilder buf = new StringBuilder(name).append("([");
        for (Iterator<PyObject> i = this.asIterable().iterator(); i.hasNext();) {
            buf.append((i.next()).__repr__().toString());
            if (i.hasNext()) {
                buf.append(", ");
            }
        }
        buf.append("])");
        ts.exitRepr(this);
        return buf.toString();
    }


    /* Traverseproc implementation */
    @Override
    public int traverse(Visitproc visit, Object arg) {
        return dvDict != null ? visit.visit(dvDict, arg) : 0;
    }

    @Override
    public boolean refersDirectlyTo(PyObject ob) {
        return ob != null && dvDict == ob;
    }
}
