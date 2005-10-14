package org.python.modules.time;

import org.python.core.Py;
import org.python.core.PyBuiltinFunction;
import org.python.core.PyBuiltinFunctionNarrow;
import org.python.core.PyGetSetDescr;
import org.python.core.PyMethodDescr;
import org.python.core.PyInteger;
import org.python.core.PyObject;
import org.python.core.PyTuple;
import org.python.core.PyType;

class PyTimeTupleSetup {
    public static void typeSetup(PyObject dict,PyType.Newstyle marker) {
        dict.__setitem__("tm_year",new PyGetSetDescr("tm_year",PyTimeTuple.class,"getYear",null));
        dict.__setitem__("tm_mon",new PyGetSetDescr("tm_mon",PyTimeTuple.class,"getMon",null));
        dict.__setitem__("tm_mday",new PyGetSetDescr("tm_mday",PyTimeTuple.class,"getMday",null));
        dict.__setitem__("tm_hour",new PyGetSetDescr("tm_hour",PyTimeTuple.class,"getHour",null));
        dict.__setitem__("tm_min",new PyGetSetDescr("tm_min",PyTimeTuple.class,"getMin",null));
        dict.__setitem__("tm_sec",new PyGetSetDescr("tm_sec",PyTimeTuple.class,"getSec",null));
        dict.__setitem__("tm_wday",new PyGetSetDescr("tm_wday",PyTimeTuple.class,"getWday",null));
        dict.__setitem__("tm_yday",new PyGetSetDescr("tm_yday",PyTimeTuple.class,"getYday",null));
        dict.__setitem__("tm_isdst",new PyGetSetDescr("tm_isdst",PyTimeTuple.class,"getIsdst",null));
        class exposed___ne__ extends PyBuiltinFunctionNarrow {

            private PyTimeTuple self;

            public PyObject getSelf() {
                return self;
            }

            exposed___ne__(PyTimeTuple self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___ne__((PyTimeTuple)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=self.timetuple___ne__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyTimeTuple self=(PyTimeTuple)gself;
                PyObject ret=self.timetuple___ne__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__ne__",new PyMethodDescr("__ne__",PyTimeTuple.class,1,1,new exposed___ne__(null,null)));
        class exposed___eq__ extends PyBuiltinFunctionNarrow {

            private PyTimeTuple self;

            public PyObject getSelf() {
                return self;
            }

            exposed___eq__(PyTimeTuple self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___eq__((PyTimeTuple)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=self.timetuple___eq__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyTimeTuple self=(PyTimeTuple)gself;
                PyObject ret=self.timetuple___eq__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__eq__",new PyMethodDescr("__eq__",PyTimeTuple.class,1,1,new exposed___eq__(null,null)));
    }
}
