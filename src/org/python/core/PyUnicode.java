package org.python.core;

import org.python.modules._codecs;

/**
 * a builtin python unicode string.
 */

public class PyUnicode extends PyString {
    public static final Class exposed_base=PyBaseString.class;

    //~ BEGIN GENERATED REGION -- DO NOT EDIT SEE gexpose.py
    /* type info */

    public static final String exposed_name="unicode";

    public static void typeSetup(PyObject dict,PyType.Newstyle marker) {
        class exposed___ne__ extends PyBuiltinFunctionNarrow {

            private PyUnicode self;

            public PyObject getSelf() {
                return self;
            }

            exposed___ne__(PyUnicode self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___ne__((PyUnicode)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=self.unicode___ne__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyUnicode self=(PyUnicode)gself;
                PyObject ret=self.unicode___ne__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__ne__",new PyMethodDescr("__ne__",PyUnicode.class,1,1,new exposed___ne__(null,null)));
        class exposed___eq__ extends PyBuiltinFunctionNarrow {

            private PyUnicode self;

            public PyObject getSelf() {
                return self;
            }

            exposed___eq__(PyUnicode self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___eq__((PyUnicode)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=self.unicode___eq__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyUnicode self=(PyUnicode)gself;
                PyObject ret=self.unicode___eq__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__eq__",new PyMethodDescr("__eq__",PyUnicode.class,1,1,new exposed___eq__(null,null)));
        class exposed___getitem__ extends PyBuiltinFunctionNarrow {

            private PyUnicode self;

            public PyObject getSelf() {
                return self;
            }

            exposed___getitem__(PyUnicode self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___getitem__((PyUnicode)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=self.seq___finditem__(arg0);
                if (ret==null) {
                    throw Py.IndexError("index out of range: "+arg0);
                }
                return ret;
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyUnicode self=(PyUnicode)gself;
                PyObject ret=self.seq___finditem__(arg0);
                if (ret==null) {
                    throw Py.IndexError("index out of range: "+arg0);
                }
                return ret;
            }

        }
        dict.__setitem__("__getitem__",new PyMethodDescr("__getitem__",PyUnicode.class,1,1,new exposed___getitem__(null,null)));
        class exposed___getslice__ extends PyBuiltinFunctionNarrow {

            private PyUnicode self;

            public PyObject getSelf() {
                return self;
            }

            exposed___getslice__(PyUnicode self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___getslice__((PyUnicode)self,info);
            }

            public PyObject __call__(PyObject arg0,PyObject arg1,PyObject arg2) {
                return self.seq___getslice__(arg0,arg1,arg2);
            }

            public PyObject inst_call(PyObject gself,PyObject arg0,PyObject arg1,PyObject arg2) {
                PyUnicode self=(PyUnicode)gself;
                return self.seq___getslice__(arg0,arg1,arg2);
            }

            public PyObject __call__(PyObject arg0,PyObject arg1) {
                return self.seq___getslice__(arg0,arg1);
            }

            public PyObject inst_call(PyObject gself,PyObject arg0,PyObject arg1) {
                PyUnicode self=(PyUnicode)gself;
                return self.seq___getslice__(arg0,arg1);
            }

        }
        dict.__setitem__("__getslice__",new PyMethodDescr("__getslice__",PyUnicode.class,2,3,new exposed___getslice__(null,null)));
        class exposed___contains__ extends PyBuiltinFunctionNarrow {

            private PyUnicode self;

            public PyObject getSelf() {
                return self;
            }

            exposed___contains__(PyUnicode self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___contains__((PyUnicode)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                return Py.newBoolean(self.unicode___contains__(arg0));
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyUnicode self=(PyUnicode)gself;
                return Py.newBoolean(self.unicode___contains__(arg0));
            }

        }
        dict.__setitem__("__contains__",new PyMethodDescr("__contains__",PyUnicode.class,1,1,new exposed___contains__(null,null)));
        class exposed___len__ extends PyBuiltinFunctionNarrow {

            private PyUnicode self;

            public PyObject getSelf() {
                return self;
            }

            exposed___len__(PyUnicode self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___len__((PyUnicode)self,info);
            }

            public PyObject __call__() {
                return Py.newInteger(self.unicode___len__());
            }

            public PyObject inst_call(PyObject gself) {
                PyUnicode self=(PyUnicode)gself;
                return Py.newInteger(self.unicode___len__());
            }

        }
        dict.__setitem__("__len__",new PyMethodDescr("__len__",PyUnicode.class,0,0,new exposed___len__(null,null)));
        class exposed___add__ extends PyBuiltinFunctionNarrow {

            private PyUnicode self;

            public PyObject getSelf() {
                return self;
            }

            exposed___add__(PyUnicode self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___add__((PyUnicode)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                return self.unicode___add__(arg0);
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyUnicode self=(PyUnicode)gself;
                return self.unicode___add__(arg0);
            }

        }
        dict.__setitem__("__add__",new PyMethodDescr("__add__",PyUnicode.class,1,1,new exposed___add__(null,null)));
        class exposed___mul__ extends PyBuiltinFunctionNarrow {

            private PyUnicode self;

            public PyObject getSelf() {
                return self;
            }

            exposed___mul__(PyUnicode self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___mul__((PyUnicode)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                return self.unicode___mul__(arg0);
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyUnicode self=(PyUnicode)gself;
                return self.unicode___mul__(arg0);
            }

        }
        dict.__setitem__("__mul__",new PyMethodDescr("__mul__",PyUnicode.class,1,1,new exposed___mul__(null,null)));
        class exposed___rmul__ extends PyBuiltinFunctionNarrow {

            private PyUnicode self;

            public PyObject getSelf() {
                return self;
            }

            exposed___rmul__(PyUnicode self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___rmul__((PyUnicode)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                return self.unicode___rmul__(arg0);
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyUnicode self=(PyUnicode)gself;
                return self.unicode___rmul__(arg0);
            }

        }
        dict.__setitem__("__rmul__",new PyMethodDescr("__rmul__",PyUnicode.class,1,1,new exposed___rmul__(null,null)));
        class exposed___str__ extends PyBuiltinFunctionNarrow {

            private PyUnicode self;

            public PyObject getSelf() {
                return self;
            }

            exposed___str__(PyUnicode self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___str__((PyUnicode)self,info);
            }

            public PyObject __call__() {
                return self.unicode___str__();
            }

            public PyObject inst_call(PyObject gself) {
                PyUnicode self=(PyUnicode)gself;
                return self.unicode___str__();
            }

        }
        dict.__setitem__("__str__",new PyMethodDescr("__str__",PyUnicode.class,0,0,new exposed___str__(null,null)));
        class exposed___unicode__ extends PyBuiltinFunctionNarrow {

            private PyUnicode self;

            public PyObject getSelf() {
                return self;
            }

            exposed___unicode__(PyUnicode self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___unicode__((PyUnicode)self,info);
            }

            public PyObject __call__() {
                return self.unicode___unicode__();
            }

            public PyObject inst_call(PyObject gself) {
                PyUnicode self=(PyUnicode)gself;
                return self.unicode___unicode__();
            }

        }
        dict.__setitem__("__unicode__",new PyMethodDescr("__unicode__",PyUnicode.class,0,0,new exposed___unicode__(null,null)));
        class exposed___hash__ extends PyBuiltinFunctionNarrow {

            private PyUnicode self;

            public PyObject getSelf() {
                return self;
            }

            exposed___hash__(PyUnicode self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___hash__((PyUnicode)self,info);
            }

            public PyObject __call__() {
                return Py.newInteger(self.unicode_hashCode());
            }

            public PyObject inst_call(PyObject gself) {
                PyUnicode self=(PyUnicode)gself;
                return Py.newInteger(self.unicode_hashCode());
            }

        }
        dict.__setitem__("__hash__",new PyMethodDescr("__hash__",PyUnicode.class,0,0,new exposed___hash__(null,null)));
        class exposed___repr__ extends PyBuiltinFunctionNarrow {

            private PyUnicode self;

            public PyObject getSelf() {
                return self;
            }

            exposed___repr__(PyUnicode self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___repr__((PyUnicode)self,info);
            }

            public PyObject __call__() {
                return new PyString(self.unicode_toString());
            }

            public PyObject inst_call(PyObject gself) {
                PyUnicode self=(PyUnicode)gself;
                return new PyString(self.unicode_toString());
            }

        }
        dict.__setitem__("__repr__",new PyMethodDescr("__repr__",PyUnicode.class,0,0,new exposed___repr__(null,null)));
        class exposed_capitalize extends PyBuiltinFunctionNarrow {

            private PyUnicode self;

            public PyObject getSelf() {
                return self;
            }

            exposed_capitalize(PyUnicode self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed_capitalize((PyUnicode)self,info);
            }

            public PyObject __call__() {
                return new PyUnicode(self.unicode_capitalize());
            }

            public PyObject inst_call(PyObject gself) {
                PyUnicode self=(PyUnicode)gself;
                return new PyUnicode(self.unicode_capitalize());
            }

        }
        dict.__setitem__("capitalize",new PyMethodDescr("capitalize",PyUnicode.class,0,0,new exposed_capitalize(null,null)));
        class exposed_center extends PyBuiltinFunctionNarrow {

            private PyUnicode self;

            public PyObject getSelf() {
                return self;
            }

            exposed_center(PyUnicode self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed_center((PyUnicode)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                try {
                    return new PyUnicode(self.unicode_center(arg0.asInt(0)));
                } catch (PyObject.ConversionException e) {
                    String msg;
                    switch (e.index) {
                    case 0:
                        msg="expected an integer";
                        break;
                    default:
                        msg="xxx";
                    }
                    throw Py.TypeError(msg);
                }
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyUnicode self=(PyUnicode)gself;
                try {
                    return new PyUnicode(self.unicode_center(arg0.asInt(0)));
                } catch (PyObject.ConversionException e) {
                    String msg;
                    switch (e.index) {
                    case 0:
                        msg="expected an integer";
                        break;
                    default:
                        msg="xxx";
                    }
                    throw Py.TypeError(msg);
                }
            }

        }
        dict.__setitem__("center",new PyMethodDescr("center",PyUnicode.class,1,1,new exposed_center(null,null)));
        class exposed_count extends PyBuiltinFunctionNarrow {

            private PyUnicode self;

            public PyObject getSelf() {
                return self;
            }

            exposed_count(PyUnicode self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed_count((PyUnicode)self,info);
            }

            public PyObject __call__(PyObject arg0,PyObject arg1,PyObject arg2) {
                try {
                    return Py.newInteger(self.unicode_count(arg0.asString(0),arg1.asInt(1),arg2.asInt(2)));
                } catch (PyObject.ConversionException e) {
                    String msg;
                    switch (e.index) {
                    case 1:
                    case 2:
                        msg="expected an integer";
                        break;
                    case 0:
                        msg="expected a string";
                        break;
                    default:
                        msg="xxx";
                    }
                    throw Py.TypeError(msg);
                }
            }

            public PyObject inst_call(PyObject gself,PyObject arg0,PyObject arg1,PyObject arg2) {
                PyUnicode self=(PyUnicode)gself;
                try {
                    return Py.newInteger(self.unicode_count(arg0.asString(0),arg1.asInt(1),arg2.asInt(2)));
                } catch (PyObject.ConversionException e) {
                    String msg;
                    switch (e.index) {
                    case 1:
                    case 2:
                        msg="expected an integer";
                        break;
                    case 0:
                        msg="expected a string";
                        break;
                    default:
                        msg="xxx";
                    }
                    throw Py.TypeError(msg);
                }
            }

            public PyObject __call__(PyObject arg0,PyObject arg1) {
                try {
                    return Py.newInteger(self.unicode_count(arg0.asString(0),arg1.asInt(1)));
                } catch (PyObject.ConversionException e) {
                    String msg;
                    switch (e.index) {
                    case 1:
                        msg="expected an integer";
                        break;
                    case 0:
                        msg="expected a string";
                        break;
                    default:
                        msg="xxx";
                    }
                    throw Py.TypeError(msg);
                }
            }

            public PyObject inst_call(PyObject gself,PyObject arg0,PyObject arg1) {
                PyUnicode self=(PyUnicode)gself;
                try {
                    return Py.newInteger(self.unicode_count(arg0.asString(0),arg1.asInt(1)));
                } catch (PyObject.ConversionException e) {
                    String msg;
                    switch (e.index) {
                    case 1:
                        msg="expected an integer";
                        break;
                    case 0:
                        msg="expected a string";
                        break;
                    default:
                        msg="xxx";
                    }
                    throw Py.TypeError(msg);
                }
            }

            public PyObject __call__(PyObject arg0) {
                try {
                    return Py.newInteger(self.unicode_count(arg0.asString(0)));
                } catch (PyObject.ConversionException e) {
                    String msg;
                    switch (e.index) {
                    case 0:
                        msg="expected a string";
                        break;
                    default:
                        msg="xxx";
                    }
                    throw Py.TypeError(msg);
                }
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyUnicode self=(PyUnicode)gself;
                try {
                    return Py.newInteger(self.unicode_count(arg0.asString(0)));
                } catch (PyObject.ConversionException e) {
                    String msg;
                    switch (e.index) {
                    case 0:
                        msg="expected a string";
                        break;
                    default:
                        msg="xxx";
                    }
                    throw Py.TypeError(msg);
                }
            }

        }
        dict.__setitem__("count",new PyMethodDescr("count",PyUnicode.class,1,3,new exposed_count(null,null)));
        class exposed_decode extends PyBuiltinFunctionNarrow {

            private PyUnicode self;

            public PyObject getSelf() {
                return self;
            }

            exposed_decode(PyUnicode self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed_decode((PyUnicode)self,info);
            }

            public PyObject __call__(PyObject arg0,PyObject arg1) {
                try {
                    return new PyUnicode(self.unicode_decode(arg0.asString(0),arg1.asString(1)));
                } catch (PyObject.ConversionException e) {
                    String msg;
                    switch (e.index) {
                    case 0:
                    case 1:
                        msg="expected a string";
                        break;
                    default:
                        msg="xxx";
                    }
                    throw Py.TypeError(msg);
                }
            }

            public PyObject inst_call(PyObject gself,PyObject arg0,PyObject arg1) {
                PyUnicode self=(PyUnicode)gself;
                try {
                    return new PyUnicode(self.unicode_decode(arg0.asString(0),arg1.asString(1)));
                } catch (PyObject.ConversionException e) {
                    String msg;
                    switch (e.index) {
                    case 0:
                    case 1:
                        msg="expected a string";
                        break;
                    default:
                        msg="xxx";
                    }
                    throw Py.TypeError(msg);
                }
            }

            public PyObject __call__(PyObject arg0) {
                try {
                    return new PyUnicode(self.unicode_decode(arg0.asString(0)));
                } catch (PyObject.ConversionException e) {
                    String msg;
                    switch (e.index) {
                    case 0:
                        msg="expected a string";
                        break;
                    default:
                        msg="xxx";
                    }
                    throw Py.TypeError(msg);
                }
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyUnicode self=(PyUnicode)gself;
                try {
                    return new PyUnicode(self.unicode_decode(arg0.asString(0)));
                } catch (PyObject.ConversionException e) {
                    String msg;
                    switch (e.index) {
                    case 0:
                        msg="expected a string";
                        break;
                    default:
                        msg="xxx";
                    }
                    throw Py.TypeError(msg);
                }
            }

            public PyObject __call__() {
                return new PyUnicode(self.unicode_decode());
            }

            public PyObject inst_call(PyObject gself) {
                PyUnicode self=(PyUnicode)gself;
                return new PyUnicode(self.unicode_decode());
            }

        }
        dict.__setitem__("decode",new PyMethodDescr("decode",PyUnicode.class,0,2,new exposed_decode(null,null)));
        class exposed_encode extends PyBuiltinFunctionNarrow {

            private PyUnicode self;

            public PyObject getSelf() {
                return self;
            }

            exposed_encode(PyUnicode self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed_encode((PyUnicode)self,info);
            }

            public PyObject __call__(PyObject arg0,PyObject arg1) {
                try {
                    return new PyUnicode(self.unicode_encode(arg0.asString(0),arg1.asString(1)));
                } catch (PyObject.ConversionException e) {
                    String msg;
                    switch (e.index) {
                    case 0:
                    case 1:
                        msg="expected a string";
                        break;
                    default:
                        msg="xxx";
                    }
                    throw Py.TypeError(msg);
                }
            }

            public PyObject inst_call(PyObject gself,PyObject arg0,PyObject arg1) {
                PyUnicode self=(PyUnicode)gself;
                try {
                    return new PyUnicode(self.unicode_encode(arg0.asString(0),arg1.asString(1)));
                } catch (PyObject.ConversionException e) {
                    String msg;
                    switch (e.index) {
                    case 0:
                    case 1:
                        msg="expected a string";
                        break;
                    default:
                        msg="xxx";
                    }
                    throw Py.TypeError(msg);
                }
            }

            public PyObject __call__(PyObject arg0) {
                try {
                    return new PyUnicode(self.unicode_encode(arg0.asString(0)));
                } catch (PyObject.ConversionException e) {
                    String msg;
                    switch (e.index) {
                    case 0:
                        msg="expected a string";
                        break;
                    default:
                        msg="xxx";
                    }
                    throw Py.TypeError(msg);
                }
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyUnicode self=(PyUnicode)gself;
                try {
                    return new PyUnicode(self.unicode_encode(arg0.asString(0)));
                } catch (PyObject.ConversionException e) {
                    String msg;
                    switch (e.index) {
                    case 0:
                        msg="expected a string";
                        break;
                    default:
                        msg="xxx";
                    }
                    throw Py.TypeError(msg);
                }
            }

            public PyObject __call__() {
                return new PyUnicode(self.unicode_encode());
            }

            public PyObject inst_call(PyObject gself) {
                PyUnicode self=(PyUnicode)gself;
                return new PyUnicode(self.unicode_encode());
            }

        }
        dict.__setitem__("encode",new PyMethodDescr("encode",PyUnicode.class,0,2,new exposed_encode(null,null)));
        class exposed_endswith extends PyBuiltinFunctionNarrow {

            private PyUnicode self;

            public PyObject getSelf() {
                return self;
            }

            exposed_endswith(PyUnicode self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed_endswith((PyUnicode)self,info);
            }

            public PyObject __call__(PyObject arg0,PyObject arg1,PyObject arg2) {
                try {
                    return Py.newBoolean(self.unicode_endswith(arg0.asString(0),arg1.asInt(1),arg2.asInt(2)));
                } catch (PyObject.ConversionException e) {
                    String msg;
                    switch (e.index) {
                    case 1:
                    case 2:
                        msg="expected an integer";
                        break;
                    case 0:
                        msg="expected a string";
                        break;
                    default:
                        msg="xxx";
                    }
                    throw Py.TypeError(msg);
                }
            }

            public PyObject inst_call(PyObject gself,PyObject arg0,PyObject arg1,PyObject arg2) {
                PyUnicode self=(PyUnicode)gself;
                try {
                    return Py.newBoolean(self.unicode_endswith(arg0.asString(0),arg1.asInt(1),arg2.asInt(2)));
                } catch (PyObject.ConversionException e) {
                    String msg;
                    switch (e.index) {
                    case 1:
                    case 2:
                        msg="expected an integer";
                        break;
                    case 0:
                        msg="expected a string";
                        break;
                    default:
                        msg="xxx";
                    }
                    throw Py.TypeError(msg);
                }
            }

            public PyObject __call__(PyObject arg0,PyObject arg1) {
                try {
                    return Py.newBoolean(self.unicode_endswith(arg0.asString(0),arg1.asInt(1)));
                } catch (PyObject.ConversionException e) {
                    String msg;
                    switch (e.index) {
                    case 1:
                        msg="expected an integer";
                        break;
                    case 0:
                        msg="expected a string";
                        break;
                    default:
                        msg="xxx";
                    }
                    throw Py.TypeError(msg);
                }
            }

            public PyObject inst_call(PyObject gself,PyObject arg0,PyObject arg1) {
                PyUnicode self=(PyUnicode)gself;
                try {
                    return Py.newBoolean(self.unicode_endswith(arg0.asString(0),arg1.asInt(1)));
                } catch (PyObject.ConversionException e) {
                    String msg;
                    switch (e.index) {
                    case 1:
                        msg="expected an integer";
                        break;
                    case 0:
                        msg="expected a string";
                        break;
                    default:
                        msg="xxx";
                    }
                    throw Py.TypeError(msg);
                }
            }

            public PyObject __call__(PyObject arg0) {
                try {
                    return Py.newBoolean(self.unicode_endswith(arg0.asString(0)));
                } catch (PyObject.ConversionException e) {
                    String msg;
                    switch (e.index) {
                    case 0:
                        msg="expected a string";
                        break;
                    default:
                        msg="xxx";
                    }
                    throw Py.TypeError(msg);
                }
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyUnicode self=(PyUnicode)gself;
                try {
                    return Py.newBoolean(self.unicode_endswith(arg0.asString(0)));
                } catch (PyObject.ConversionException e) {
                    String msg;
                    switch (e.index) {
                    case 0:
                        msg="expected a string";
                        break;
                    default:
                        msg="xxx";
                    }
                    throw Py.TypeError(msg);
                }
            }

        }
        dict.__setitem__("endswith",new PyMethodDescr("endswith",PyUnicode.class,1,3,new exposed_endswith(null,null)));
        class exposed_expandtabs extends PyBuiltinFunctionNarrow {

            private PyUnicode self;

            public PyObject getSelf() {
                return self;
            }

            exposed_expandtabs(PyUnicode self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed_expandtabs((PyUnicode)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                try {
                    return new PyUnicode(self.unicode_expandtabs(arg0.asInt(0)));
                } catch (PyObject.ConversionException e) {
                    String msg;
                    switch (e.index) {
                    case 0:
                        msg="expected an integer";
                        break;
                    default:
                        msg="xxx";
                    }
                    throw Py.TypeError(msg);
                }
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyUnicode self=(PyUnicode)gself;
                try {
                    return new PyUnicode(self.unicode_expandtabs(arg0.asInt(0)));
                } catch (PyObject.ConversionException e) {
                    String msg;
                    switch (e.index) {
                    case 0:
                        msg="expected an integer";
                        break;
                    default:
                        msg="xxx";
                    }
                    throw Py.TypeError(msg);
                }
            }

            public PyObject __call__() {
                return new PyUnicode(self.unicode_expandtabs());
            }

            public PyObject inst_call(PyObject gself) {
                PyUnicode self=(PyUnicode)gself;
                return new PyUnicode(self.unicode_expandtabs());
            }

        }
        dict.__setitem__("expandtabs",new PyMethodDescr("expandtabs",PyUnicode.class,0,1,new exposed_expandtabs(null,null)));
        class exposed_find extends PyBuiltinFunctionNarrow {

            private PyUnicode self;

            public PyObject getSelf() {
                return self;
            }

            exposed_find(PyUnicode self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed_find((PyUnicode)self,info);
            }

            public PyObject __call__(PyObject arg0,PyObject arg1,PyObject arg2) {
                try {
                    return Py.newInteger(self.unicode_find(arg0.asString(0),arg1.asInt(1),arg2.asInt(2)));
                } catch (PyObject.ConversionException e) {
                    String msg;
                    switch (e.index) {
                    case 1:
                    case 2:
                        msg="expected an integer";
                        break;
                    case 0:
                        msg="expected a string";
                        break;
                    default:
                        msg="xxx";
                    }
                    throw Py.TypeError(msg);
                }
            }

            public PyObject inst_call(PyObject gself,PyObject arg0,PyObject arg1,PyObject arg2) {
                PyUnicode self=(PyUnicode)gself;
                try {
                    return Py.newInteger(self.unicode_find(arg0.asString(0),arg1.asInt(1),arg2.asInt(2)));
                } catch (PyObject.ConversionException e) {
                    String msg;
                    switch (e.index) {
                    case 1:
                    case 2:
                        msg="expected an integer";
                        break;
                    case 0:
                        msg="expected a string";
                        break;
                    default:
                        msg="xxx";
                    }
                    throw Py.TypeError(msg);
                }
            }

            public PyObject __call__(PyObject arg0,PyObject arg1) {
                try {
                    return Py.newInteger(self.unicode_find(arg0.asString(0),arg1.asInt(1)));
                } catch (PyObject.ConversionException e) {
                    String msg;
                    switch (e.index) {
                    case 1:
                        msg="expected an integer";
                        break;
                    case 0:
                        msg="expected a string";
                        break;
                    default:
                        msg="xxx";
                    }
                    throw Py.TypeError(msg);
                }
            }

            public PyObject inst_call(PyObject gself,PyObject arg0,PyObject arg1) {
                PyUnicode self=(PyUnicode)gself;
                try {
                    return Py.newInteger(self.unicode_find(arg0.asString(0),arg1.asInt(1)));
                } catch (PyObject.ConversionException e) {
                    String msg;
                    switch (e.index) {
                    case 1:
                        msg="expected an integer";
                        break;
                    case 0:
                        msg="expected a string";
                        break;
                    default:
                        msg="xxx";
                    }
                    throw Py.TypeError(msg);
                }
            }

            public PyObject __call__(PyObject arg0) {
                try {
                    return Py.newInteger(self.unicode_find(arg0.asString(0)));
                } catch (PyObject.ConversionException e) {
                    String msg;
                    switch (e.index) {
                    case 0:
                        msg="expected a string";
                        break;
                    default:
                        msg="xxx";
                    }
                    throw Py.TypeError(msg);
                }
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyUnicode self=(PyUnicode)gself;
                try {
                    return Py.newInteger(self.unicode_find(arg0.asString(0)));
                } catch (PyObject.ConversionException e) {
                    String msg;
                    switch (e.index) {
                    case 0:
                        msg="expected a string";
                        break;
                    default:
                        msg="xxx";
                    }
                    throw Py.TypeError(msg);
                }
            }

        }
        dict.__setitem__("find",new PyMethodDescr("find",PyUnicode.class,1,3,new exposed_find(null,null)));
        class exposed_index extends PyBuiltinFunctionNarrow {

            private PyUnicode self;

            public PyObject getSelf() {
                return self;
            }

            exposed_index(PyUnicode self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed_index((PyUnicode)self,info);
            }

            public PyObject __call__(PyObject arg0,PyObject arg1,PyObject arg2) {
                try {
                    return Py.newInteger(self.unicode_index(arg0.asString(0),arg1.asInt(1),arg2.asInt(2)));
                } catch (PyObject.ConversionException e) {
                    String msg;
                    switch (e.index) {
                    case 1:
                    case 2:
                        msg="expected an integer";
                        break;
                    case 0:
                        msg="expected a string";
                        break;
                    default:
                        msg="xxx";
                    }
                    throw Py.TypeError(msg);
                }
            }

            public PyObject inst_call(PyObject gself,PyObject arg0,PyObject arg1,PyObject arg2) {
                PyUnicode self=(PyUnicode)gself;
                try {
                    return Py.newInteger(self.unicode_index(arg0.asString(0),arg1.asInt(1),arg2.asInt(2)));
                } catch (PyObject.ConversionException e) {
                    String msg;
                    switch (e.index) {
                    case 1:
                    case 2:
                        msg="expected an integer";
                        break;
                    case 0:
                        msg="expected a string";
                        break;
                    default:
                        msg="xxx";
                    }
                    throw Py.TypeError(msg);
                }
            }

            public PyObject __call__(PyObject arg0,PyObject arg1) {
                try {
                    return Py.newInteger(self.unicode_index(arg0.asString(0),arg1.asInt(1)));
                } catch (PyObject.ConversionException e) {
                    String msg;
                    switch (e.index) {
                    case 1:
                        msg="expected an integer";
                        break;
                    case 0:
                        msg="expected a string";
                        break;
                    default:
                        msg="xxx";
                    }
                    throw Py.TypeError(msg);
                }
            }

            public PyObject inst_call(PyObject gself,PyObject arg0,PyObject arg1) {
                PyUnicode self=(PyUnicode)gself;
                try {
                    return Py.newInteger(self.unicode_index(arg0.asString(0),arg1.asInt(1)));
                } catch (PyObject.ConversionException e) {
                    String msg;
                    switch (e.index) {
                    case 1:
                        msg="expected an integer";
                        break;
                    case 0:
                        msg="expected a string";
                        break;
                    default:
                        msg="xxx";
                    }
                    throw Py.TypeError(msg);
                }
            }

            public PyObject __call__(PyObject arg0) {
                try {
                    return Py.newInteger(self.unicode_index(arg0.asString(0)));
                } catch (PyObject.ConversionException e) {
                    String msg;
                    switch (e.index) {
                    case 0:
                        msg="expected a string";
                        break;
                    default:
                        msg="xxx";
                    }
                    throw Py.TypeError(msg);
                }
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyUnicode self=(PyUnicode)gself;
                try {
                    return Py.newInteger(self.unicode_index(arg0.asString(0)));
                } catch (PyObject.ConversionException e) {
                    String msg;
                    switch (e.index) {
                    case 0:
                        msg="expected a string";
                        break;
                    default:
                        msg="xxx";
                    }
                    throw Py.TypeError(msg);
                }
            }

        }
        dict.__setitem__("index",new PyMethodDescr("index",PyUnicode.class,1,3,new exposed_index(null,null)));
        class exposed_isalnum extends PyBuiltinFunctionNarrow {

            private PyUnicode self;

            public PyObject getSelf() {
                return self;
            }

            exposed_isalnum(PyUnicode self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed_isalnum((PyUnicode)self,info);
            }

            public PyObject __call__() {
                return Py.newBoolean(self.unicode_isalnum());
            }

            public PyObject inst_call(PyObject gself) {
                PyUnicode self=(PyUnicode)gself;
                return Py.newBoolean(self.unicode_isalnum());
            }

        }
        dict.__setitem__("isalnum",new PyMethodDescr("isalnum",PyUnicode.class,0,0,new exposed_isalnum(null,null)));
        class exposed_isalpha extends PyBuiltinFunctionNarrow {

            private PyUnicode self;

            public PyObject getSelf() {
                return self;
            }

            exposed_isalpha(PyUnicode self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed_isalpha((PyUnicode)self,info);
            }

            public PyObject __call__() {
                return Py.newBoolean(self.unicode_isalpha());
            }

            public PyObject inst_call(PyObject gself) {
                PyUnicode self=(PyUnicode)gself;
                return Py.newBoolean(self.unicode_isalpha());
            }

        }
        dict.__setitem__("isalpha",new PyMethodDescr("isalpha",PyUnicode.class,0,0,new exposed_isalpha(null,null)));
        class exposed_isdecimal extends PyBuiltinFunctionNarrow {

            private PyUnicode self;

            public PyObject getSelf() {
                return self;
            }

            exposed_isdecimal(PyUnicode self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed_isdecimal((PyUnicode)self,info);
            }

            public PyObject __call__() {
                return Py.newBoolean(self.unicode_isdecimal());
            }

            public PyObject inst_call(PyObject gself) {
                PyUnicode self=(PyUnicode)gself;
                return Py.newBoolean(self.unicode_isdecimal());
            }

        }
        dict.__setitem__("isdecimal",new PyMethodDescr("isdecimal",PyUnicode.class,0,0,new exposed_isdecimal(null,null)));
        class exposed_isdigit extends PyBuiltinFunctionNarrow {

            private PyUnicode self;

            public PyObject getSelf() {
                return self;
            }

            exposed_isdigit(PyUnicode self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed_isdigit((PyUnicode)self,info);
            }

            public PyObject __call__() {
                return Py.newBoolean(self.unicode_isdigit());
            }

            public PyObject inst_call(PyObject gself) {
                PyUnicode self=(PyUnicode)gself;
                return Py.newBoolean(self.unicode_isdigit());
            }

        }
        dict.__setitem__("isdigit",new PyMethodDescr("isdigit",PyUnicode.class,0,0,new exposed_isdigit(null,null)));
        class exposed_islower extends PyBuiltinFunctionNarrow {

            private PyUnicode self;

            public PyObject getSelf() {
                return self;
            }

            exposed_islower(PyUnicode self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed_islower((PyUnicode)self,info);
            }

            public PyObject __call__() {
                return Py.newBoolean(self.unicode_islower());
            }

            public PyObject inst_call(PyObject gself) {
                PyUnicode self=(PyUnicode)gself;
                return Py.newBoolean(self.unicode_islower());
            }

        }
        dict.__setitem__("islower",new PyMethodDescr("islower",PyUnicode.class,0,0,new exposed_islower(null,null)));
        class exposed_isnumeric extends PyBuiltinFunctionNarrow {

            private PyUnicode self;

            public PyObject getSelf() {
                return self;
            }

            exposed_isnumeric(PyUnicode self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed_isnumeric((PyUnicode)self,info);
            }

            public PyObject __call__() {
                return Py.newBoolean(self.unicode_isnumeric());
            }

            public PyObject inst_call(PyObject gself) {
                PyUnicode self=(PyUnicode)gself;
                return Py.newBoolean(self.unicode_isnumeric());
            }

        }
        dict.__setitem__("isnumeric",new PyMethodDescr("isnumeric",PyUnicode.class,0,0,new exposed_isnumeric(null,null)));
        class exposed_isspace extends PyBuiltinFunctionNarrow {

            private PyUnicode self;

            public PyObject getSelf() {
                return self;
            }

            exposed_isspace(PyUnicode self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed_isspace((PyUnicode)self,info);
            }

            public PyObject __call__() {
                return Py.newBoolean(self.unicode_isspace());
            }

            public PyObject inst_call(PyObject gself) {
                PyUnicode self=(PyUnicode)gself;
                return Py.newBoolean(self.unicode_isspace());
            }

        }
        dict.__setitem__("isspace",new PyMethodDescr("isspace",PyUnicode.class,0,0,new exposed_isspace(null,null)));
        class exposed_istitle extends PyBuiltinFunctionNarrow {

            private PyUnicode self;

            public PyObject getSelf() {
                return self;
            }

            exposed_istitle(PyUnicode self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed_istitle((PyUnicode)self,info);
            }

            public PyObject __call__() {
                return Py.newBoolean(self.unicode_istitle());
            }

            public PyObject inst_call(PyObject gself) {
                PyUnicode self=(PyUnicode)gself;
                return Py.newBoolean(self.unicode_istitle());
            }

        }
        dict.__setitem__("istitle",new PyMethodDescr("istitle",PyUnicode.class,0,0,new exposed_istitle(null,null)));
        class exposed_isunicode extends PyBuiltinFunctionNarrow {

            private PyUnicode self;

            public PyObject getSelf() {
                return self;
            }

            exposed_isunicode(PyUnicode self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed_isunicode((PyUnicode)self,info);
            }

            public PyObject __call__() {
                return Py.newBoolean(self.unicode_isunicode());
            }

            public PyObject inst_call(PyObject gself) {
                PyUnicode self=(PyUnicode)gself;
                return Py.newBoolean(self.unicode_isunicode());
            }

        }
        dict.__setitem__("isunicode",new PyMethodDescr("isunicode",PyUnicode.class,0,0,new exposed_isunicode(null,null)));
        class exposed_isupper extends PyBuiltinFunctionNarrow {

            private PyUnicode self;

            public PyObject getSelf() {
                return self;
            }

            exposed_isupper(PyUnicode self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed_isupper((PyUnicode)self,info);
            }

            public PyObject __call__() {
                return Py.newBoolean(self.unicode_isupper());
            }

            public PyObject inst_call(PyObject gself) {
                PyUnicode self=(PyUnicode)gself;
                return Py.newBoolean(self.unicode_isupper());
            }

        }
        dict.__setitem__("isupper",new PyMethodDescr("isupper",PyUnicode.class,0,0,new exposed_isupper(null,null)));
        class exposed_join extends PyBuiltinFunctionNarrow {

            private PyUnicode self;

            public PyObject getSelf() {
                return self;
            }

            exposed_join(PyUnicode self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed_join((PyUnicode)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                return new PyUnicode(self.unicode_join(arg0));
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyUnicode self=(PyUnicode)gself;
                return new PyUnicode(self.unicode_join(arg0));
            }

        }
        dict.__setitem__("join",new PyMethodDescr("join",PyUnicode.class,1,1,new exposed_join(null,null)));
        class exposed_ljust extends PyBuiltinFunctionNarrow {

            private PyUnicode self;

            public PyObject getSelf() {
                return self;
            }

            exposed_ljust(PyUnicode self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed_ljust((PyUnicode)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                try {
                    return new PyUnicode(self.unicode_ljust(arg0.asInt(0)));
                } catch (PyObject.ConversionException e) {
                    String msg;
                    switch (e.index) {
                    case 0:
                        msg="expected an integer";
                        break;
                    default:
                        msg="xxx";
                    }
                    throw Py.TypeError(msg);
                }
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyUnicode self=(PyUnicode)gself;
                try {
                    return new PyUnicode(self.unicode_ljust(arg0.asInt(0)));
                } catch (PyObject.ConversionException e) {
                    String msg;
                    switch (e.index) {
                    case 0:
                        msg="expected an integer";
                        break;
                    default:
                        msg="xxx";
                    }
                    throw Py.TypeError(msg);
                }
            }

        }
        dict.__setitem__("ljust",new PyMethodDescr("ljust",PyUnicode.class,1,1,new exposed_ljust(null,null)));
        class exposed_lower extends PyBuiltinFunctionNarrow {

            private PyUnicode self;

            public PyObject getSelf() {
                return self;
            }

            exposed_lower(PyUnicode self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed_lower((PyUnicode)self,info);
            }

            public PyObject __call__() {
                return new PyUnicode(self.unicode_lower());
            }

            public PyObject inst_call(PyObject gself) {
                PyUnicode self=(PyUnicode)gself;
                return new PyUnicode(self.unicode_lower());
            }

        }
        dict.__setitem__("lower",new PyMethodDescr("lower",PyUnicode.class,0,0,new exposed_lower(null,null)));
        class exposed_lstrip extends PyBuiltinFunctionNarrow {

            private PyUnicode self;

            public PyObject getSelf() {
                return self;
            }

            exposed_lstrip(PyUnicode self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed_lstrip((PyUnicode)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                try {
                    return new PyUnicode(self.unicode_lstrip(arg0.asStringOrNull(0)));
                } catch (PyObject.ConversionException e) {
                    String msg;
                    switch (e.index) {
                    case 0:
                        msg="expected a string or None";
                        break;
                    default:
                        msg="xxx";
                    }
                    throw Py.TypeError(msg);
                }
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyUnicode self=(PyUnicode)gself;
                try {
                    return new PyUnicode(self.unicode_lstrip(arg0.asStringOrNull(0)));
                } catch (PyObject.ConversionException e) {
                    String msg;
                    switch (e.index) {
                    case 0:
                        msg="expected a string or None";
                        break;
                    default:
                        msg="xxx";
                    }
                    throw Py.TypeError(msg);
                }
            }

            public PyObject __call__() {
                return new PyUnicode(self.unicode_lstrip());
            }

            public PyObject inst_call(PyObject gself) {
                PyUnicode self=(PyUnicode)gself;
                return new PyUnicode(self.unicode_lstrip());
            }

        }
        dict.__setitem__("lstrip",new PyMethodDescr("lstrip",PyUnicode.class,0,1,new exposed_lstrip(null,null)));
        class exposed_replace extends PyBuiltinFunctionNarrow {

            private PyUnicode self;

            public PyObject getSelf() {
                return self;
            }

            exposed_replace(PyUnicode self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed_replace((PyUnicode)self,info);
            }

            public PyObject __call__(PyObject arg0,PyObject arg1,PyObject arg2) {
                try {
                    return new PyUnicode(self.unicode_replace(arg0.asString(0),arg1.asString(1),arg2.asInt(2)));
                } catch (PyObject.ConversionException e) {
                    String msg;
                    switch (e.index) {
                    case 2:
                        msg="expected an integer";
                        break;
                    case 0:
                    case 1:
                        msg="expected a string";
                        break;
                    default:
                        msg="xxx";
                    }
                    throw Py.TypeError(msg);
                }
            }

            public PyObject inst_call(PyObject gself,PyObject arg0,PyObject arg1,PyObject arg2) {
                PyUnicode self=(PyUnicode)gself;
                try {
                    return new PyUnicode(self.unicode_replace(arg0.asString(0),arg1.asString(1),arg2.asInt(2)));
                } catch (PyObject.ConversionException e) {
                    String msg;
                    switch (e.index) {
                    case 2:
                        msg="expected an integer";
                        break;
                    case 0:
                    case 1:
                        msg="expected a string";
                        break;
                    default:
                        msg="xxx";
                    }
                    throw Py.TypeError(msg);
                }
            }

            public PyObject __call__(PyObject arg0,PyObject arg1) {
                try {
                    return new PyUnicode(self.unicode_replace(arg0.asString(0),arg1.asString(1)));
                } catch (PyObject.ConversionException e) {
                    String msg;
                    switch (e.index) {
                    case 0:
                    case 1:
                        msg="expected a string";
                        break;
                    default:
                        msg="xxx";
                    }
                    throw Py.TypeError(msg);
                }
            }

            public PyObject inst_call(PyObject gself,PyObject arg0,PyObject arg1) {
                PyUnicode self=(PyUnicode)gself;
                try {
                    return new PyUnicode(self.unicode_replace(arg0.asString(0),arg1.asString(1)));
                } catch (PyObject.ConversionException e) {
                    String msg;
                    switch (e.index) {
                    case 0:
                    case 1:
                        msg="expected a string";
                        break;
                    default:
                        msg="xxx";
                    }
                    throw Py.TypeError(msg);
                }
            }

        }
        dict.__setitem__("replace",new PyMethodDescr("replace",PyUnicode.class,2,3,new exposed_replace(null,null)));
        class exposed_rfind extends PyBuiltinFunctionNarrow {

            private PyUnicode self;

            public PyObject getSelf() {
                return self;
            }

            exposed_rfind(PyUnicode self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed_rfind((PyUnicode)self,info);
            }

            public PyObject __call__(PyObject arg0,PyObject arg1,PyObject arg2) {
                try {
                    return Py.newInteger(self.unicode_rfind(arg0.asString(0),arg1.asInt(1),arg2.asInt(2)));
                } catch (PyObject.ConversionException e) {
                    String msg;
                    switch (e.index) {
                    case 1:
                    case 2:
                        msg="expected an integer";
                        break;
                    case 0:
                        msg="expected a string";
                        break;
                    default:
                        msg="xxx";
                    }
                    throw Py.TypeError(msg);
                }
            }

            public PyObject inst_call(PyObject gself,PyObject arg0,PyObject arg1,PyObject arg2) {
                PyUnicode self=(PyUnicode)gself;
                try {
                    return Py.newInteger(self.unicode_rfind(arg0.asString(0),arg1.asInt(1),arg2.asInt(2)));
                } catch (PyObject.ConversionException e) {
                    String msg;
                    switch (e.index) {
                    case 1:
                    case 2:
                        msg="expected an integer";
                        break;
                    case 0:
                        msg="expected a string";
                        break;
                    default:
                        msg="xxx";
                    }
                    throw Py.TypeError(msg);
                }
            }

            public PyObject __call__(PyObject arg0,PyObject arg1) {
                try {
                    return Py.newInteger(self.unicode_rfind(arg0.asString(0),arg1.asInt(1)));
                } catch (PyObject.ConversionException e) {
                    String msg;
                    switch (e.index) {
                    case 1:
                        msg="expected an integer";
                        break;
                    case 0:
                        msg="expected a string";
                        break;
                    default:
                        msg="xxx";
                    }
                    throw Py.TypeError(msg);
                }
            }

            public PyObject inst_call(PyObject gself,PyObject arg0,PyObject arg1) {
                PyUnicode self=(PyUnicode)gself;
                try {
                    return Py.newInteger(self.unicode_rfind(arg0.asString(0),arg1.asInt(1)));
                } catch (PyObject.ConversionException e) {
                    String msg;
                    switch (e.index) {
                    case 1:
                        msg="expected an integer";
                        break;
                    case 0:
                        msg="expected a string";
                        break;
                    default:
                        msg="xxx";
                    }
                    throw Py.TypeError(msg);
                }
            }

            public PyObject __call__(PyObject arg0) {
                try {
                    return Py.newInteger(self.unicode_rfind(arg0.asString(0)));
                } catch (PyObject.ConversionException e) {
                    String msg;
                    switch (e.index) {
                    case 0:
                        msg="expected a string";
                        break;
                    default:
                        msg="xxx";
                    }
                    throw Py.TypeError(msg);
                }
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyUnicode self=(PyUnicode)gself;
                try {
                    return Py.newInteger(self.unicode_rfind(arg0.asString(0)));
                } catch (PyObject.ConversionException e) {
                    String msg;
                    switch (e.index) {
                    case 0:
                        msg="expected a string";
                        break;
                    default:
                        msg="xxx";
                    }
                    throw Py.TypeError(msg);
                }
            }

        }
        dict.__setitem__("rfind",new PyMethodDescr("rfind",PyUnicode.class,1,3,new exposed_rfind(null,null)));
        class exposed_rindex extends PyBuiltinFunctionNarrow {

            private PyUnicode self;

            public PyObject getSelf() {
                return self;
            }

            exposed_rindex(PyUnicode self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed_rindex((PyUnicode)self,info);
            }

            public PyObject __call__(PyObject arg0,PyObject arg1,PyObject arg2) {
                try {
                    return Py.newInteger(self.unicode_rindex(arg0.asString(0),arg1.asInt(1),arg2.asInt(2)));
                } catch (PyObject.ConversionException e) {
                    String msg;
                    switch (e.index) {
                    case 1:
                    case 2:
                        msg="expected an integer";
                        break;
                    case 0:
                        msg="expected a string";
                        break;
                    default:
                        msg="xxx";
                    }
                    throw Py.TypeError(msg);
                }
            }

            public PyObject inst_call(PyObject gself,PyObject arg0,PyObject arg1,PyObject arg2) {
                PyUnicode self=(PyUnicode)gself;
                try {
                    return Py.newInteger(self.unicode_rindex(arg0.asString(0),arg1.asInt(1),arg2.asInt(2)));
                } catch (PyObject.ConversionException e) {
                    String msg;
                    switch (e.index) {
                    case 1:
                    case 2:
                        msg="expected an integer";
                        break;
                    case 0:
                        msg="expected a string";
                        break;
                    default:
                        msg="xxx";
                    }
                    throw Py.TypeError(msg);
                }
            }

            public PyObject __call__(PyObject arg0,PyObject arg1) {
                try {
                    return Py.newInteger(self.unicode_rindex(arg0.asString(0),arg1.asInt(1)));
                } catch (PyObject.ConversionException e) {
                    String msg;
                    switch (e.index) {
                    case 1:
                        msg="expected an integer";
                        break;
                    case 0:
                        msg="expected a string";
                        break;
                    default:
                        msg="xxx";
                    }
                    throw Py.TypeError(msg);
                }
            }

            public PyObject inst_call(PyObject gself,PyObject arg0,PyObject arg1) {
                PyUnicode self=(PyUnicode)gself;
                try {
                    return Py.newInteger(self.unicode_rindex(arg0.asString(0),arg1.asInt(1)));
                } catch (PyObject.ConversionException e) {
                    String msg;
                    switch (e.index) {
                    case 1:
                        msg="expected an integer";
                        break;
                    case 0:
                        msg="expected a string";
                        break;
                    default:
                        msg="xxx";
                    }
                    throw Py.TypeError(msg);
                }
            }

            public PyObject __call__(PyObject arg0) {
                try {
                    return Py.newInteger(self.unicode_rindex(arg0.asString(0)));
                } catch (PyObject.ConversionException e) {
                    String msg;
                    switch (e.index) {
                    case 0:
                        msg="expected a string";
                        break;
                    default:
                        msg="xxx";
                    }
                    throw Py.TypeError(msg);
                }
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyUnicode self=(PyUnicode)gself;
                try {
                    return Py.newInteger(self.unicode_rindex(arg0.asString(0)));
                } catch (PyObject.ConversionException e) {
                    String msg;
                    switch (e.index) {
                    case 0:
                        msg="expected a string";
                        break;
                    default:
                        msg="xxx";
                    }
                    throw Py.TypeError(msg);
                }
            }

        }
        dict.__setitem__("rindex",new PyMethodDescr("rindex",PyUnicode.class,1,3,new exposed_rindex(null,null)));
        class exposed_rjust extends PyBuiltinFunctionNarrow {

            private PyUnicode self;

            public PyObject getSelf() {
                return self;
            }

            exposed_rjust(PyUnicode self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed_rjust((PyUnicode)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                try {
                    return new PyUnicode(self.unicode_rjust(arg0.asInt(0)));
                } catch (PyObject.ConversionException e) {
                    String msg;
                    switch (e.index) {
                    case 0:
                        msg="expected an integer";
                        break;
                    default:
                        msg="xxx";
                    }
                    throw Py.TypeError(msg);
                }
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyUnicode self=(PyUnicode)gself;
                try {
                    return new PyUnicode(self.unicode_rjust(arg0.asInt(0)));
                } catch (PyObject.ConversionException e) {
                    String msg;
                    switch (e.index) {
                    case 0:
                        msg="expected an integer";
                        break;
                    default:
                        msg="xxx";
                    }
                    throw Py.TypeError(msg);
                }
            }

        }
        dict.__setitem__("rjust",new PyMethodDescr("rjust",PyUnicode.class,1,1,new exposed_rjust(null,null)));
        class exposed_rstrip extends PyBuiltinFunctionNarrow {

            private PyUnicode self;

            public PyObject getSelf() {
                return self;
            }

            exposed_rstrip(PyUnicode self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed_rstrip((PyUnicode)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                try {
                    return new PyUnicode(self.unicode_rstrip(arg0.asStringOrNull(0)));
                } catch (PyObject.ConversionException e) {
                    String msg;
                    switch (e.index) {
                    case 0:
                        msg="expected a string or None";
                        break;
                    default:
                        msg="xxx";
                    }
                    throw Py.TypeError(msg);
                }
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyUnicode self=(PyUnicode)gself;
                try {
                    return new PyUnicode(self.unicode_rstrip(arg0.asStringOrNull(0)));
                } catch (PyObject.ConversionException e) {
                    String msg;
                    switch (e.index) {
                    case 0:
                        msg="expected a string or None";
                        break;
                    default:
                        msg="xxx";
                    }
                    throw Py.TypeError(msg);
                }
            }

            public PyObject __call__() {
                return new PyUnicode(self.unicode_rstrip());
            }

            public PyObject inst_call(PyObject gself) {
                PyUnicode self=(PyUnicode)gself;
                return new PyUnicode(self.unicode_rstrip());
            }

        }
        dict.__setitem__("rstrip",new PyMethodDescr("rstrip",PyUnicode.class,0,1,new exposed_rstrip(null,null)));
        class exposed_split extends PyBuiltinFunctionNarrow {

            private PyUnicode self;

            public PyObject getSelf() {
                return self;
            }

            exposed_split(PyUnicode self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed_split((PyUnicode)self,info);
            }

            public PyObject __call__(PyObject arg0,PyObject arg1) {
                try {
                    return self.unicode_split(arg0.asStringOrNull(0),arg1.asInt(1));
                } catch (PyObject.ConversionException e) {
                    String msg;
                    switch (e.index) {
                    case 1:
                        msg="expected an integer";
                        break;
                    case 0:
                        msg="expected a string or None";
                        break;
                    default:
                        msg="xxx";
                    }
                    throw Py.TypeError(msg);
                }
            }

            public PyObject inst_call(PyObject gself,PyObject arg0,PyObject arg1) {
                PyUnicode self=(PyUnicode)gself;
                try {
                    return self.unicode_split(arg0.asStringOrNull(0),arg1.asInt(1));
                } catch (PyObject.ConversionException e) {
                    String msg;
                    switch (e.index) {
                    case 1:
                        msg="expected an integer";
                        break;
                    case 0:
                        msg="expected a string or None";
                        break;
                    default:
                        msg="xxx";
                    }
                    throw Py.TypeError(msg);
                }
            }

            public PyObject __call__(PyObject arg0) {
                try {
                    return self.unicode_split(arg0.asStringOrNull(0));
                } catch (PyObject.ConversionException e) {
                    String msg;
                    switch (e.index) {
                    case 0:
                        msg="expected a string or None";
                        break;
                    default:
                        msg="xxx";
                    }
                    throw Py.TypeError(msg);
                }
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyUnicode self=(PyUnicode)gself;
                try {
                    return self.unicode_split(arg0.asStringOrNull(0));
                } catch (PyObject.ConversionException e) {
                    String msg;
                    switch (e.index) {
                    case 0:
                        msg="expected a string or None";
                        break;
                    default:
                        msg="xxx";
                    }
                    throw Py.TypeError(msg);
                }
            }

            public PyObject __call__() {
                return self.unicode_split();
            }

            public PyObject inst_call(PyObject gself) {
                PyUnicode self=(PyUnicode)gself;
                return self.unicode_split();
            }

        }
        dict.__setitem__("split",new PyMethodDescr("split",PyUnicode.class,0,2,new exposed_split(null,null)));
        class exposed_splitlines extends PyBuiltinFunctionNarrow {

            private PyUnicode self;

            public PyObject getSelf() {
                return self;
            }

            exposed_splitlines(PyUnicode self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed_splitlines((PyUnicode)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                return self.unicode_splitlines(arg0.__nonzero__());
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyUnicode self=(PyUnicode)gself;
                return self.unicode_splitlines(arg0.__nonzero__());
            }

            public PyObject __call__() {
                return self.unicode_splitlines();
            }

            public PyObject inst_call(PyObject gself) {
                PyUnicode self=(PyUnicode)gself;
                return self.unicode_splitlines();
            }

        }
        dict.__setitem__("splitlines",new PyMethodDescr("splitlines",PyUnicode.class,0,1,new exposed_splitlines(null,null)));
        class exposed_startswith extends PyBuiltinFunctionNarrow {

            private PyUnicode self;

            public PyObject getSelf() {
                return self;
            }

            exposed_startswith(PyUnicode self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed_startswith((PyUnicode)self,info);
            }

            public PyObject __call__(PyObject arg0,PyObject arg1,PyObject arg2) {
                try {
                    return Py.newBoolean(self.unicode_startswith(arg0.asString(0),arg1.asInt(1),arg2.asInt(2)));
                } catch (PyObject.ConversionException e) {
                    String msg;
                    switch (e.index) {
                    case 1:
                    case 2:
                        msg="expected an integer";
                        break;
                    case 0:
                        msg="expected a string";
                        break;
                    default:
                        msg="xxx";
                    }
                    throw Py.TypeError(msg);
                }
            }

            public PyObject inst_call(PyObject gself,PyObject arg0,PyObject arg1,PyObject arg2) {
                PyUnicode self=(PyUnicode)gself;
                try {
                    return Py.newBoolean(self.unicode_startswith(arg0.asString(0),arg1.asInt(1),arg2.asInt(2)));
                } catch (PyObject.ConversionException e) {
                    String msg;
                    switch (e.index) {
                    case 1:
                    case 2:
                        msg="expected an integer";
                        break;
                    case 0:
                        msg="expected a string";
                        break;
                    default:
                        msg="xxx";
                    }
                    throw Py.TypeError(msg);
                }
            }

            public PyObject __call__(PyObject arg0,PyObject arg1) {
                try {
                    return Py.newBoolean(self.unicode_startswith(arg0.asString(0),arg1.asInt(1)));
                } catch (PyObject.ConversionException e) {
                    String msg;
                    switch (e.index) {
                    case 1:
                        msg="expected an integer";
                        break;
                    case 0:
                        msg="expected a string";
                        break;
                    default:
                        msg="xxx";
                    }
                    throw Py.TypeError(msg);
                }
            }

            public PyObject inst_call(PyObject gself,PyObject arg0,PyObject arg1) {
                PyUnicode self=(PyUnicode)gself;
                try {
                    return Py.newBoolean(self.unicode_startswith(arg0.asString(0),arg1.asInt(1)));
                } catch (PyObject.ConversionException e) {
                    String msg;
                    switch (e.index) {
                    case 1:
                        msg="expected an integer";
                        break;
                    case 0:
                        msg="expected a string";
                        break;
                    default:
                        msg="xxx";
                    }
                    throw Py.TypeError(msg);
                }
            }

            public PyObject __call__(PyObject arg0) {
                try {
                    return Py.newBoolean(self.unicode_startswith(arg0.asString(0)));
                } catch (PyObject.ConversionException e) {
                    String msg;
                    switch (e.index) {
                    case 0:
                        msg="expected a string";
                        break;
                    default:
                        msg="xxx";
                    }
                    throw Py.TypeError(msg);
                }
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyUnicode self=(PyUnicode)gself;
                try {
                    return Py.newBoolean(self.unicode_startswith(arg0.asString(0)));
                } catch (PyObject.ConversionException e) {
                    String msg;
                    switch (e.index) {
                    case 0:
                        msg="expected a string";
                        break;
                    default:
                        msg="xxx";
                    }
                    throw Py.TypeError(msg);
                }
            }

        }
        dict.__setitem__("startswith",new PyMethodDescr("startswith",PyUnicode.class,1,3,new exposed_startswith(null,null)));
        class exposed_strip extends PyBuiltinFunctionNarrow {

            private PyUnicode self;

            public PyObject getSelf() {
                return self;
            }

            exposed_strip(PyUnicode self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed_strip((PyUnicode)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                try {
                    return new PyUnicode(self.unicode_strip(arg0.asStringOrNull(0)));
                } catch (PyObject.ConversionException e) {
                    String msg;
                    switch (e.index) {
                    case 0:
                        msg="expected a string or None";
                        break;
                    default:
                        msg="xxx";
                    }
                    throw Py.TypeError(msg);
                }
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyUnicode self=(PyUnicode)gself;
                try {
                    return new PyUnicode(self.unicode_strip(arg0.asStringOrNull(0)));
                } catch (PyObject.ConversionException e) {
                    String msg;
                    switch (e.index) {
                    case 0:
                        msg="expected a string or None";
                        break;
                    default:
                        msg="xxx";
                    }
                    throw Py.TypeError(msg);
                }
            }

            public PyObject __call__() {
                return new PyUnicode(self.unicode_strip());
            }

            public PyObject inst_call(PyObject gself) {
                PyUnicode self=(PyUnicode)gself;
                return new PyUnicode(self.unicode_strip());
            }

        }
        dict.__setitem__("strip",new PyMethodDescr("strip",PyUnicode.class,0,1,new exposed_strip(null,null)));
        class exposed_swapcase extends PyBuiltinFunctionNarrow {

            private PyUnicode self;

            public PyObject getSelf() {
                return self;
            }

            exposed_swapcase(PyUnicode self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed_swapcase((PyUnicode)self,info);
            }

            public PyObject __call__() {
                return new PyUnicode(self.unicode_swapcase());
            }

            public PyObject inst_call(PyObject gself) {
                PyUnicode self=(PyUnicode)gself;
                return new PyUnicode(self.unicode_swapcase());
            }

        }
        dict.__setitem__("swapcase",new PyMethodDescr("swapcase",PyUnicode.class,0,0,new exposed_swapcase(null,null)));
        class exposed_title extends PyBuiltinFunctionNarrow {

            private PyUnicode self;

            public PyObject getSelf() {
                return self;
            }

            exposed_title(PyUnicode self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed_title((PyUnicode)self,info);
            }

            public PyObject __call__() {
                return new PyUnicode(self.unicode_title());
            }

            public PyObject inst_call(PyObject gself) {
                PyUnicode self=(PyUnicode)gself;
                return new PyUnicode(self.unicode_title());
            }

        }
        dict.__setitem__("title",new PyMethodDescr("title",PyUnicode.class,0,0,new exposed_title(null,null)));
        class exposed_translate extends PyBuiltinFunctionNarrow {

            private PyUnicode self;

            public PyObject getSelf() {
                return self;
            }

            exposed_translate(PyUnicode self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed_translate((PyUnicode)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                return new PyUnicode(self.unicode_translate(arg0));
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyUnicode self=(PyUnicode)gself;
                return new PyUnicode(self.unicode_translate(arg0));
            }

        }
        dict.__setitem__("translate",new PyMethodDescr("translate",PyUnicode.class,1,1,new exposed_translate(null,null)));
        class exposed_upper extends PyBuiltinFunctionNarrow {

            private PyUnicode self;

            public PyObject getSelf() {
                return self;
            }

            exposed_upper(PyUnicode self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed_upper((PyUnicode)self,info);
            }

            public PyObject __call__() {
                return new PyUnicode(self.unicode_upper());
            }

            public PyObject inst_call(PyObject gself) {
                PyUnicode self=(PyUnicode)gself;
                return new PyUnicode(self.unicode_upper());
            }

        }
        dict.__setitem__("upper",new PyMethodDescr("upper",PyUnicode.class,0,0,new exposed_upper(null,null)));
        class exposed_zfill extends PyBuiltinFunctionNarrow {

            private PyUnicode self;

            public PyObject getSelf() {
                return self;
            }

            exposed_zfill(PyUnicode self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed_zfill((PyUnicode)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                try {
                    return new PyUnicode(self.unicode_zfill(arg0.asInt(0)));
                } catch (PyObject.ConversionException e) {
                    String msg;
                    switch (e.index) {
                    case 0:
                        msg="expected an integer";
                        break;
                    default:
                        msg="xxx";
                    }
                    throw Py.TypeError(msg);
                }
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyUnicode self=(PyUnicode)gself;
                try {
                    return new PyUnicode(self.unicode_zfill(arg0.asInt(0)));
                } catch (PyObject.ConversionException e) {
                    String msg;
                    switch (e.index) {
                    case 0:
                        msg="expected an integer";
                        break;
                    default:
                        msg="xxx";
                    }
                    throw Py.TypeError(msg);
                }
            }

        }
        dict.__setitem__("zfill",new PyMethodDescr("zfill",PyUnicode.class,1,1,new exposed_zfill(null,null)));
        dict.__setitem__("__new__",new PyNewWrapper(PyUnicode.class,"__new__",-1,-1) {

                                                                                         public PyObject new_impl(boolean init,PyType subtype,PyObject[]args,String[]keywords) {
                                                                                             return unicode_new(this,init,subtype,args,keywords);
                                                                                         }

                                                                                     });
    }
    //~ END GENERATED REGION -- DO NOT EDIT SEE gexpose.py

    //XXX: probably don't need these.
    //private String string;
    //private transient int cached_hashcode=0;
    //private transient boolean interned=false;

    private static final PyType UNICODETYPE = PyType.fromClass(PyUnicode.class);
    
    // for PyJavaClass.init()
    public PyUnicode() {
        this(UNICODETYPE, "");
    }

    public PyUnicode(String string) {
        this(UNICODETYPE, string);
    }
    
    public PyUnicode(PyType subtype, String string) {
        super(subtype, string);
    }
    
    public PyUnicode(PyString pystring) {
        this(UNICODETYPE, pystring);
    }
    
    public PyUnicode(PyType subtype, PyString pystring) {
        this(subtype, (String)pystring.__tojava__(String.class));
    }


    public PyUnicode(char c) {
        this(UNICODETYPE,String.valueOf(c));
    }

    final static PyObject unicode_new(PyNewWrapper new_, boolean init, PyType subtype,
            PyObject[] args, String[] keywords) {
        ArgParser ap = new ArgParser("unicode",
                                     args,
                                     keywords,
                                     new String[] {"string",
                                                   "encoding",
                                                   "errors"},
                                     0);
        PyObject S = ap.getPyObject(0, null);
        String encoding = ap.getString(1, null);
        String errors = ap.getString(2, null);
        if (new_.for_type == subtype) {
            if (S == null) {
                return new PyUnicode("");
            }
            if (S instanceof PyUnicode) {
                return new PyUnicode( (String)S.__tojava__(String.class) );
            }
            if (S instanceof PyString) {
                return new PyUnicode(codecs.decode((PyString)S, encoding, errors));
            }
            return S.__unicode__();
        } else {
            if (S == null) {
                return new PyUnicodeDerived(subtype, "");
            }
        
            return new PyUnicodeDerived(subtype, (String)((S.__str__()).__tojava__(String.class)));
        }
    }

    /** <i>Internal use only. Do not call this method explicitly.</i> */
    public static void classDictInit(PyObject dict) throws PyIgnoreMethodTag {}

    public String safeRepr() throws PyIgnoreMethodTag {
        return "'unicode' object";
    }
    
    public PyString createInstance(String str){
       return new PyUnicode(str);
    }

    final PyUnicode unicode___unicode__() {
        return str___unicode__();
    }

    public PyString unicode___str__() {
        return str___str__();
    }

    final int unicode___len__() {
        return str___len__();
    }

    public PyString __repr__() {
        return new PyUnicode("u" + encode_UnicodeEscape(string, true));
    }

    public String unicode_toString() {
        return "u" + str_toString();
    }

    final int unicode___cmp__(PyObject other) {
        return str___cmp__(other);
    }

    final PyObject unicode___eq__(PyObject other) {
        return str___eq__(other);
    }

    final PyObject unicode___ne__(PyObject other) {
        return str___ne__(other);
    }

    final int unicode_hashCode() {
        return str_hashCode();
    }

    protected PyObject pyget(int i) {
        return Py.makeCharacter(string.charAt(i), true);
    }

    final boolean unicode___contains__(PyObject o) {
        return str___contains__(o);
    }

    final PyObject unicode___mul__(PyObject o) {
        return str___mul__(o);
    }

    final PyObject unicode___rmul__(PyObject o) {
        return str___rmul__(o);
    }

    final PyObject unicode___add__(PyObject generic_other) {
        return str___add__(generic_other);
    }

    final String unicode_lower() {
        return str_lower();
    }

    final String unicode_upper() {
        return str_upper();
    }

    final String unicode_title() {
        return str_title();
    }

    final String unicode_swapcase() {
        return str_swapcase();
    }

    final String unicode_strip() {
        return str_strip();
    }

    final String unicode_strip(String sep) {
        return str_strip(sep);
    }

    final String unicode_lstrip() {
        return str_lstrip();
    }

    final String unicode_lstrip(String sep) {
        return str_lstrip(sep);
    }

    final String unicode_rstrip() {
        return str_rstrip();
    }

    final String unicode_rstrip(String sep) {
        return str_rstrip(sep);
    }


    final PyList unicode_split() {
        return str_split();
    }

    final PyList unicode_split(String sep) {
        return str_split(sep);
    }

    final PyList unicode_split(String sep, int maxsplit) {
        return str_split(sep, maxsplit);
    }

    final PyList unicode_splitlines() {
        return str_splitlines();
    }

    final PyList unicode_splitlines(boolean keepends) {
        return str_splitlines(keepends);
    }
    
    protected PyString fromSubstring(int begin, int end) {
        return new PyUnicode(string.substring(begin, end));
    }


    final int unicode_index(String sub) {
        return str_index(sub);
    }

    final int unicode_index(String sub, int start) {
        return str_index(sub, start);
    }

    final int unicode_index(String sub, int start, int end) {
        return str_index(sub, start, end);
    }

    final int unicode_rindex(String sub) {
        return str_rindex(sub);
    }

    final int unicode_rindex(String sub, int start) {
        return str_rindex(sub, start);
    }

    final int unicode_rindex(String sub, int start, int end) {
        return str_rindex(sub, start, end);
    }

    final int unicode_count(String sub) {
        return str_count(sub);
    }

    final int unicode_count(String sub, int start) {
        return str_count(sub, start);
    }

    final int unicode_count(String sub, int start, int end) {
        return str_count(sub, start, end);
    }

    final int unicode_find(String sub) {
        return str_find(sub);
    }

    final int unicode_find(String sub, int start) {
        return str_find(sub, start);
    }

    final int unicode_find(String sub, int start, int end) {
        return str_find(sub, start, end);
    }

    final int unicode_rfind(String sub) {
        return str_rfind(sub);
    }

    final int unicode_rfind(String sub, int start) {
        return str_rfind(sub, start);
    }

    final int unicode_rfind(String sub, int start, int end) {
        return str_rfind(sub, start, end);
    }

    final String unicode_ljust(int width) {
        return str_ljust(width);
    }

    final String unicode_rjust(int width) {
        return str_rjust(width);
    }

    final String unicode_center(int width) {
        return str_center(width);
    }

    final String unicode_zfill(int width) {
        return str_zfill(width);
    }

    final String unicode_expandtabs() {
        return str_expandtabs();
    }

    final String unicode_expandtabs(int tabsize) {
        return str_expandtabs(tabsize);
    }

    final String unicode_capitalize() {
        return str_capitalize();
    }

    final String unicode_replace(String oldPiece, String newPiece) {
        return str_replace(oldPiece, newPiece);
    }

    final String unicode_replace(String oldPiece, String newPiece, int maxsplit) {
        return str_replace(oldPiece, newPiece, maxsplit);
    }

    final String unicode_join(PyObject seq) {
        return str_join(seq);
    }

    final boolean unicode_startswith(String prefix) {
        return str_startswith(prefix);
    }

    final boolean unicode_startswith(String prefix, int offset) {
        return str_startswith(prefix, offset);
    }

    final boolean unicode_startswith(String prefix, int start, int end) {
        return str_startswith(prefix, start, end);
    }

    final boolean unicode_endswith(String suffix) {
        return str_endswith(suffix);
    }

    final boolean unicode_endswith(String suffix, int start) {
        return str_endswith(suffix, start);
    }

    final boolean unicode_endswith(String suffix, int start, int end) {
        return str_endswith(suffix, start, end);
    }

    final String unicode_translate(PyObject table) {
        return _codecs.charmap_decode(string, "ignore", table, true).__getitem__(0).toString();
    }

    final boolean unicode_islower() {
        return str_islower();
    }

    final boolean unicode_isupper() {
        return str_isupper();
    }

    final boolean unicode_isalpha() {
        return str_isalpha();
    }

    final boolean unicode_isalnum() {
        return str_isalnum();
    }

    final boolean unicode_isdecimal() {
        return str_isdecimal();
    }

    final boolean unicode_isdigit() {
        return str_isdigit();
    }

    final boolean unicode_isnumeric() {
        return str_isnumeric();
    }

    final boolean unicode_istitle() {
        return str_istitle();
    }

    final boolean unicode_isspace() {
        return str_isspace();
    }

    final boolean unicode_isunicode() {
        return true;
    }

    final String unicode_encode() {
        return str_encode();
    }

    final String unicode_encode(String encoding) {
        return str_encode(encoding);
    }

    final String unicode_encode(String encoding, String errors) {
        return str_encode(encoding, errors);
    }

    final String unicode_decode() {
        return str_decode();
    }

    final String unicode_decode(String encoding) {
        return str_decode(encoding);
    }

    final String unicode_decode(String encoding, String errors) {
        return str_decode(encoding, errors);
    }
    
    final PyTuple unicode___getnewargs__() {
        return new PyTuple(new PyObject[] {new PyUnicode(this.string)});
    }

}
