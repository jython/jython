package org.python.modules.time;

import org.python.core.Py;
import org.python.core.PyInteger;
import org.python.core.PyObject;
import org.python.core.PyTuple;
import org.python.core.PyType;

public class PyTimeTuple extends PyTuple {
    private PyInteger tm_year;
    private PyInteger tm_mon;
    private PyInteger tm_mday;
    private PyInteger tm_hour;
    private PyInteger tm_min;
    private PyInteger tm_sec;
    private PyInteger tm_wday;
    private PyInteger tm_yday;
    private PyInteger tm_isdst;

    /* type info */

    public static final String exposed_name="timetuple";

    public static final Class exposed_base=PyTuple.class;

    public static void typeSetup(PyObject dict,PyType.Newstyle marker) {
        PyTimeTupleSetup.typeSetup(dict, marker);
    }

    private static final PyType TIMETUPLETYPE = PyType.fromClass(PyTimeTuple.class);

    PyTimeTuple(PyObject[] vals) {
        super(TIMETUPLETYPE, vals);
        tm_year = (PyInteger)vals[0];
        tm_mon = (PyInteger)vals[1];
        tm_mday = (PyInteger)vals[2];
        tm_hour = (PyInteger)vals[3];
        tm_min = (PyInteger)vals[4];
        tm_sec = (PyInteger)vals[5];
        tm_wday = (PyInteger)vals[6];
        tm_yday = (PyInteger)vals[7];
        tm_isdst = (PyInteger)vals[8];
    }

    PyTimeTuple(PyTuple vals) {
        super(TIMETUPLETYPE, new PyObject[] {
            vals.pyget(0),
            vals.pyget(1),
            vals.pyget(2),
            vals.pyget(3),
            vals.pyget(4),
            vals.pyget(5),
            vals.pyget(6),
            vals.pyget(7),
            vals.pyget(8)
        });
        tm_year = (PyInteger)vals.pyget(0);
        tm_mon = (PyInteger)vals.pyget(1);
        tm_mday = (PyInteger)vals.pyget(2);
        tm_hour = (PyInteger)vals.pyget(3);
        tm_min = (PyInteger)vals.pyget(4);
        tm_sec = (PyInteger)vals.pyget(5);
        tm_wday = (PyInteger)vals.pyget(6);
        tm_yday = (PyInteger)vals.pyget(7);
        tm_isdst = (PyInteger)vals.pyget(8);
    }
    
    public PyInteger getYear() {
        return tm_year;
    }

    public PyInteger getMon() {
        return tm_mon;
    }

    public PyInteger getMday() {
        return tm_mday;
    }

    public PyInteger getHour() {
        return tm_hour;
    }

    public PyInteger getMin() {
        return tm_min;
    }

    public PyInteger getSec() {
        return tm_sec;
    }

    public PyInteger getWday() {
        return tm_wday;
    }

    public PyInteger getYday() {
        return tm_yday;
    }

    public PyInteger getIsdst() {
        return tm_isdst;
    }

    public synchronized PyObject __eq__(PyObject o) {
        return timetuple___eq__(o);
    }

    final synchronized PyObject timetuple___eq__(PyObject o) {
        if (!(getType() == o.getType()) && !(getType().isSubType(o.getType()))) {
            return null;
        }
        int tl = __len__();
        int ol = o.__len__();
        if (tl != ol) {
            return Py.Zero;
        }
        int i = cmp(this, tl, o, ol);
        return (i < 0) ? Py.One : Py.Zero;
    }

    public synchronized PyObject __ne__(PyObject o) {
        return timetuple___ne__(o);
    }

    final synchronized PyObject timetuple___ne__(PyObject o) {
        if (!(getType() == o.getType()) && !(getType().isSubType(o.getType()))) {
            return null;
        }
        int tl = __len__();
        int ol = o.__len__();
        if (tl != ol) {
            return Py.One;
        }
        int i = cmp(this, tl, o, ol);
        return (i < 0) ? Py.Zero : Py.One;
    }
}
