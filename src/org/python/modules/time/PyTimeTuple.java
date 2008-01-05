package org.python.modules.time;

import org.python.core.ArgParser;
import org.python.core.Py;
import org.python.core.PyBuiltinFunction;
import org.python.core.PyBuiltinMethodNarrow;
import org.python.core.PyGetSetDescr;
import org.python.core.PyInteger;
import org.python.core.PyList;
import org.python.core.PyMethodDescr;
import org.python.core.PyNewWrapper;
import org.python.core.PyObject;
import org.python.core.PySequence;
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

    //~ BEGIN GENERATED REGION -- DO NOT EDIT SEE gexpose.py
    /* type info */

    public static final String exposed_name="struct_time";

    public static final Class exposed_base=PyTuple.class;

    public static void typeSetup(PyObject dict,PyType.Newstyle marker) {
        dict.__setitem__("tm_year",new PyGetSetDescr("tm_year",PyTimeTuple.class,"getYear",null,null));
        dict.__setitem__("tm_mon",new PyGetSetDescr("tm_mon",PyTimeTuple.class,"getMon",null,null));
        dict.__setitem__("tm_mday",new PyGetSetDescr("tm_mday",PyTimeTuple.class,"getMday",null,null));
        dict.__setitem__("tm_hour",new PyGetSetDescr("tm_hour",PyTimeTuple.class,"getHour",null,null));
        dict.__setitem__("tm_min",new PyGetSetDescr("tm_min",PyTimeTuple.class,"getMin",null,null));
        dict.__setitem__("tm_sec",new PyGetSetDescr("tm_sec",PyTimeTuple.class,"getSec",null,null));
        dict.__setitem__("tm_wday",new PyGetSetDescr("tm_wday",PyTimeTuple.class,"getWday",null,null));
        dict.__setitem__("tm_yday",new PyGetSetDescr("tm_yday",PyTimeTuple.class,"getYday",null,null));
        dict.__setitem__("tm_isdst",new PyGetSetDescr("tm_isdst",PyTimeTuple.class,"getIsdst",null,null));
        class exposed___ne__ extends PyBuiltinMethodNarrow {

            exposed___ne__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___ne__(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=((PyTimeTuple)self).struct_time___ne__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__ne__",new PyMethodDescr("__ne__",PyTimeTuple.class,1,1,new exposed___ne__(null,null)));
        class exposed___eq__ extends PyBuiltinMethodNarrow {

            exposed___eq__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___eq__(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=((PyTimeTuple)self).struct_time___eq__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__eq__",new PyMethodDescr("__eq__",PyTimeTuple.class,1,1,new exposed___eq__(null,null)));
        class exposed___reduce__ extends PyBuiltinMethodNarrow {

            exposed___reduce__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___reduce__(self,info);
            }

            public PyObject __call__() {
                return((PyTimeTuple)self).struct_time___reduce__();
            }

        }
        dict.__setitem__("__reduce__",new PyMethodDescr("__reduce__",PyTimeTuple.class,0,0,new exposed___reduce__(null,null)));
        dict.__setitem__("__new__",new PyNewWrapper(PyTimeTuple.class,"__new__",-1,-1) {

                                                                                           public PyObject new_impl(boolean init,PyType subtype,PyObject[]args,String[]keywords) {
                                                                                               return struct_time_new(this,init,subtype,args,keywords);
                                                                                           }

                                                                                       });
    }
    //~ END GENERATED REGION -- DO NOT EDIT SEE gexpose.py

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
        return struct_time___eq__(o);
    }

    final synchronized PyObject struct_time___eq__(PyObject o) {
        if (!(getType() == o.getType()) && !(getType().isSubType(o.getType()))) {
            return null;
        }
        int tl = __len__();
        int ol = o.__len__();
        if (tl != ol) {
            return Py.False;
        }
        int i = cmp(this, tl, o, ol);
        return (i < 0) ? Py.True : Py.False;
    }

    public synchronized PyObject __ne__(PyObject o) {
        return struct_time___ne__(o);
    }

    final synchronized PyObject struct_time___ne__(PyObject o) {
        if (!(getType() == o.getType()) && !(getType().isSubType(o.getType()))) {
            return null;
        }
        int tl = __len__();
        int ol = o.__len__();
        if (tl != ol) {
            return Py.True;
        }
        int i = cmp(this, tl, o, ol);
        return (i < 0) ? Py.False : Py.True;
    }

    /**
     * Used for pickling.
     *
     * @return a tuple of (class, tuple)
     */
    public PyObject __reduce__() {
        return struct_time___reduce__();
    }

    final PyObject struct_time___reduce__() {
        PyTuple newargs = __getnewargs__();
        return new PyTuple(getType(), newargs);
    }

    public PyTuple __getnewargs__() {
        return new PyTuple(new PyList(getArray()));
    }

    private static PyObject struct_time_new(PyNewWrapper wrapper,
                                            boolean init,
                                            PyType subtype,
                                            PyObject[] args,
                                            String[] keywords) {
        ArgParser ap = new ArgParser("struct_time", args, keywords,
                                     new String[] { "tuple" }, 1);
        PyObject obj = ap.getPyObject(0);
        if(obj instanceof PyTuple) {
            if(obj.__len__() != 9){
                throw Py.TypeError("time.struct_time() takes a 9-sequence (1-sequence given)");
            }
            return new PyTimeTuple((PyTuple)obj);
        } else if(obj instanceof PySequence) {
            PySequence seq = (PySequence)obj;
            if(seq.__len__() != 9){
                throw Py.TypeError("time.struct_time() takes a 9-sequence (1-sequence given)");
            }
            return new PyTimeTuple((PyObject[])seq.__tojava__(PyObject[].class));
            
        }
        throw Py.TypeError("constructor requires a sequence");
    }
}
