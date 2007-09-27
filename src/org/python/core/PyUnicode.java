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
        class exposed___ne__ extends PyBuiltinMethodNarrow {

            exposed___ne__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___ne__(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=((PyUnicode)self).unicode___ne__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__ne__",new PyMethodDescr("__ne__",PyUnicode.class,1,1,new exposed___ne__(null,null)));
        class exposed___eq__ extends PyBuiltinMethodNarrow {

            exposed___eq__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___eq__(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=((PyUnicode)self).unicode___eq__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__eq__",new PyMethodDescr("__eq__",PyUnicode.class,1,1,new exposed___eq__(null,null)));
        class exposed___add__ extends PyBuiltinMethodNarrow {

            exposed___add__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___add__(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=((PyUnicode)self).unicode___add__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__add__",new PyMethodDescr("__add__",PyUnicode.class,1,1,new exposed___add__(null,null)));
        class exposed___mul__ extends PyBuiltinMethodNarrow {

            exposed___mul__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___mul__(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=((PyUnicode)self).unicode___mul__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__mul__",new PyMethodDescr("__mul__",PyUnicode.class,1,1,new exposed___mul__(null,null)));
        class exposed___rmul__ extends PyBuiltinMethodNarrow {

            exposed___rmul__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___rmul__(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=((PyUnicode)self).unicode___rmul__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__rmul__",new PyMethodDescr("__rmul__",PyUnicode.class,1,1,new exposed___rmul__(null,null)));
        class exposed___mod__ extends PyBuiltinMethodNarrow {

            exposed___mod__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___mod__(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=((PyUnicode)self).unicode___mod__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__mod__",new PyMethodDescr("__mod__",PyUnicode.class,1,1,new exposed___mod__(null,null)));
        class exposed___getitem__ extends PyBuiltinMethodNarrow {

            exposed___getitem__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___getitem__(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=((PyUnicode)self).seq___finditem__(arg0);
                if (ret==null) {
                    throw Py.IndexError("index out of range: "+arg0);
                }
                return ret;
            }

        }
        dict.__setitem__("__getitem__",new PyMethodDescr("__getitem__",PyUnicode.class,1,1,new exposed___getitem__(null,null)));
        class exposed___getslice__ extends PyBuiltinMethodNarrow {

            exposed___getslice__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___getslice__(self,info);
            }

            public PyObject __call__(PyObject arg0,PyObject arg1,PyObject arg2) {
                return((PyUnicode)self).seq___getslice__(arg0,arg1,arg2);
            }

            public PyObject __call__(PyObject arg0,PyObject arg1) {
                return((PyUnicode)self).seq___getslice__(arg0,arg1);
            }

        }
        dict.__setitem__("__getslice__",new PyMethodDescr("__getslice__",PyUnicode.class,2,3,new exposed___getslice__(null,null)));
        class exposed___contains__ extends PyBuiltinMethodNarrow {

            exposed___contains__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___contains__(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                return Py.newBoolean(((PyUnicode)self).unicode___contains__(arg0));
            }

        }
        dict.__setitem__("__contains__",new PyMethodDescr("__contains__",PyUnicode.class,1,1,new exposed___contains__(null,null)));
        class exposed___len__ extends PyBuiltinMethodNarrow {

            exposed___len__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___len__(self,info);
            }

            public PyObject __call__() {
                return Py.newInteger(((PyUnicode)self).unicode___len__());
            }

        }
        dict.__setitem__("__len__",new PyMethodDescr("__len__",PyUnicode.class,0,0,new exposed___len__(null,null)));
        class exposed___str__ extends PyBuiltinMethodNarrow {

            exposed___str__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___str__(self,info);
            }

            public PyObject __call__() {
                return((PyUnicode)self).unicode___str__();
            }

        }
        dict.__setitem__("__str__",new PyMethodDescr("__str__",PyUnicode.class,0,0,new exposed___str__(null,null)));
        class exposed___unicode__ extends PyBuiltinMethodNarrow {

            exposed___unicode__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___unicode__(self,info);
            }

            public PyObject __call__() {
                return((PyUnicode)self).unicode___unicode__();
            }

        }
        dict.__setitem__("__unicode__",new PyMethodDescr("__unicode__",PyUnicode.class,0,0,new exposed___unicode__(null,null)));
        class exposed___hash__ extends PyBuiltinMethodNarrow {

            exposed___hash__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___hash__(self,info);
            }

            public PyObject __call__() {
                return Py.newInteger(((PyUnicode)self).unicode_hashCode());
            }

        }
        dict.__setitem__("__hash__",new PyMethodDescr("__hash__",PyUnicode.class,0,0,new exposed___hash__(null,null)));
        class exposed___repr__ extends PyBuiltinMethodNarrow {

            exposed___repr__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___repr__(self,info);
            }

            public PyObject __call__() {
                return new PyString(((PyUnicode)self).unicode_toString());
            }

        }
        dict.__setitem__("__repr__",new PyMethodDescr("__repr__",PyUnicode.class,0,0,new exposed___repr__(null,null)));
        class exposed_capitalize extends PyBuiltinMethodNarrow {

            exposed_capitalize(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_capitalize(self,info);
            }

            public PyObject __call__() {
                return new PyUnicode(((PyUnicode)self).unicode_capitalize());
            }

        }
        dict.__setitem__("capitalize",new PyMethodDescr("capitalize",PyUnicode.class,0,0,new exposed_capitalize(null,null)));
        class exposed_center extends PyBuiltinMethodNarrow {

            exposed_center(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_center(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                try {
                    return new PyUnicode(((PyUnicode)self).unicode_center(arg0.asInt(0)));
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
        class exposed_count extends PyBuiltinMethodNarrow {

            exposed_count(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_count(self,info);
            }

            public PyObject __call__(PyObject arg0,PyObject arg1,PyObject arg2) {
                try {
                    return Py.newInteger(((PyUnicode)self).unicode_count(arg0.asString(0),arg1.asInt(1),arg2.asInt(2)));
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
                    return Py.newInteger(((PyUnicode)self).unicode_count(arg0.asString(0),arg1.asInt(1)));
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
                    return Py.newInteger(((PyUnicode)self).unicode_count(arg0.asString(0)));
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
        class exposed_decode extends PyBuiltinMethodNarrow {

            exposed_decode(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_decode(self,info);
            }

            public PyObject __call__(PyObject arg0,PyObject arg1) {
                try {
                    return((PyUnicode)self).unicode_decode(arg0.asString(0),arg1.asString(1));
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
                    return((PyUnicode)self).unicode_decode(arg0.asString(0));
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
                return((PyUnicode)self).unicode_decode();
            }

        }
        dict.__setitem__("decode",new PyMethodDescr("decode",PyUnicode.class,0,2,new exposed_decode(null,null)));
        class exposed_encode extends PyBuiltinMethodNarrow {

            exposed_encode(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_encode(self,info);
            }

            public PyObject __call__(PyObject arg0,PyObject arg1) {
                try {
                    return new PyString(((PyUnicode)self).unicode_encode(arg0.asString(0),arg1.asString(1)));
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
                    return new PyString(((PyUnicode)self).unicode_encode(arg0.asString(0)));
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
                return new PyString(((PyUnicode)self).unicode_encode());
            }

        }
        dict.__setitem__("encode",new PyMethodDescr("encode",PyUnicode.class,0,2,new exposed_encode(null,null)));
        class exposed_endswith extends PyBuiltinMethodNarrow {

            exposed_endswith(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_endswith(self,info);
            }

            public PyObject __call__(PyObject arg0,PyObject arg1,PyObject arg2) {
                try {
                    return Py.newBoolean(((PyUnicode)self).unicode_endswith(arg0.asString(0),arg1.asInt(1),arg2.asInt(2)));
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
                    return Py.newBoolean(((PyUnicode)self).unicode_endswith(arg0.asString(0),arg1.asInt(1)));
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
                    return Py.newBoolean(((PyUnicode)self).unicode_endswith(arg0.asString(0)));
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
        class exposed_expandtabs extends PyBuiltinMethodNarrow {

            exposed_expandtabs(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_expandtabs(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                try {
                    return new PyUnicode(((PyUnicode)self).unicode_expandtabs(arg0.asInt(0)));
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
                return new PyUnicode(((PyUnicode)self).unicode_expandtabs());
            }

        }
        dict.__setitem__("expandtabs",new PyMethodDescr("expandtabs",PyUnicode.class,0,1,new exposed_expandtabs(null,null)));
        class exposed_find extends PyBuiltinMethodNarrow {

            exposed_find(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_find(self,info);
            }

            public PyObject __call__(PyObject arg0,PyObject arg1,PyObject arg2) {
                try {
                    return Py.newInteger(((PyUnicode)self).unicode_find(arg0.asString(0),arg1.asInt(1),arg2.asInt(2)));
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
                    return Py.newInteger(((PyUnicode)self).unicode_find(arg0.asString(0),arg1.asInt(1)));
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
                    return Py.newInteger(((PyUnicode)self).unicode_find(arg0.asString(0)));
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
        class exposed_index extends PyBuiltinMethodNarrow {

            exposed_index(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_index(self,info);
            }

            public PyObject __call__(PyObject arg0,PyObject arg1,PyObject arg2) {
                try {
                    return Py.newInteger(((PyUnicode)self).unicode_index(arg0.asString(0),arg1.asInt(1),arg2.asInt(2)));
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
                    return Py.newInteger(((PyUnicode)self).unicode_index(arg0.asString(0),arg1.asInt(1)));
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
                    return Py.newInteger(((PyUnicode)self).unicode_index(arg0.asString(0)));
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
        class exposed_isalnum extends PyBuiltinMethodNarrow {

            exposed_isalnum(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_isalnum(self,info);
            }

            public PyObject __call__() {
                return Py.newBoolean(((PyUnicode)self).unicode_isalnum());
            }

        }
        dict.__setitem__("isalnum",new PyMethodDescr("isalnum",PyUnicode.class,0,0,new exposed_isalnum(null,null)));
        class exposed_isalpha extends PyBuiltinMethodNarrow {

            exposed_isalpha(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_isalpha(self,info);
            }

            public PyObject __call__() {
                return Py.newBoolean(((PyUnicode)self).unicode_isalpha());
            }

        }
        dict.__setitem__("isalpha",new PyMethodDescr("isalpha",PyUnicode.class,0,0,new exposed_isalpha(null,null)));
        class exposed_isdecimal extends PyBuiltinMethodNarrow {

            exposed_isdecimal(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_isdecimal(self,info);
            }

            public PyObject __call__() {
                return Py.newBoolean(((PyUnicode)self).unicode_isdecimal());
            }

        }
        dict.__setitem__("isdecimal",new PyMethodDescr("isdecimal",PyUnicode.class,0,0,new exposed_isdecimal(null,null)));
        class exposed_isdigit extends PyBuiltinMethodNarrow {

            exposed_isdigit(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_isdigit(self,info);
            }

            public PyObject __call__() {
                return Py.newBoolean(((PyUnicode)self).unicode_isdigit());
            }

        }
        dict.__setitem__("isdigit",new PyMethodDescr("isdigit",PyUnicode.class,0,0,new exposed_isdigit(null,null)));
        class exposed_islower extends PyBuiltinMethodNarrow {

            exposed_islower(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_islower(self,info);
            }

            public PyObject __call__() {
                return Py.newBoolean(((PyUnicode)self).unicode_islower());
            }

        }
        dict.__setitem__("islower",new PyMethodDescr("islower",PyUnicode.class,0,0,new exposed_islower(null,null)));
        class exposed_isnumeric extends PyBuiltinMethodNarrow {

            exposed_isnumeric(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_isnumeric(self,info);
            }

            public PyObject __call__() {
                return Py.newBoolean(((PyUnicode)self).unicode_isnumeric());
            }

        }
        dict.__setitem__("isnumeric",new PyMethodDescr("isnumeric",PyUnicode.class,0,0,new exposed_isnumeric(null,null)));
        class exposed_isspace extends PyBuiltinMethodNarrow {

            exposed_isspace(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_isspace(self,info);
            }

            public PyObject __call__() {
                return Py.newBoolean(((PyUnicode)self).unicode_isspace());
            }

        }
        dict.__setitem__("isspace",new PyMethodDescr("isspace",PyUnicode.class,0,0,new exposed_isspace(null,null)));
        class exposed_istitle extends PyBuiltinMethodNarrow {

            exposed_istitle(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_istitle(self,info);
            }

            public PyObject __call__() {
                return Py.newBoolean(((PyUnicode)self).unicode_istitle());
            }

        }
        dict.__setitem__("istitle",new PyMethodDescr("istitle",PyUnicode.class,0,0,new exposed_istitle(null,null)));
        class exposed_isunicode extends PyBuiltinMethodNarrow {

            exposed_isunicode(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_isunicode(self,info);
            }

            public PyObject __call__() {
                return Py.newBoolean(((PyUnicode)self).unicode_isunicode());
            }

        }
        dict.__setitem__("isunicode",new PyMethodDescr("isunicode",PyUnicode.class,0,0,new exposed_isunicode(null,null)));
        class exposed_isupper extends PyBuiltinMethodNarrow {

            exposed_isupper(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_isupper(self,info);
            }

            public PyObject __call__() {
                return Py.newBoolean(((PyUnicode)self).unicode_isupper());
            }

        }
        dict.__setitem__("isupper",new PyMethodDescr("isupper",PyUnicode.class,0,0,new exposed_isupper(null,null)));
        class exposed_join extends PyBuiltinMethodNarrow {

            exposed_join(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_join(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                return((PyUnicode)self).unicode_join(arg0);
            }

        }
        dict.__setitem__("join",new PyMethodDescr("join",PyUnicode.class,1,1,new exposed_join(null,null)));
        class exposed_ljust extends PyBuiltinMethodNarrow {

            exposed_ljust(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_ljust(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                try {
                    return new PyUnicode(((PyUnicode)self).unicode_ljust(arg0.asInt(0)));
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
        class exposed_lower extends PyBuiltinMethodNarrow {

            exposed_lower(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_lower(self,info);
            }

            public PyObject __call__() {
                return new PyUnicode(((PyUnicode)self).unicode_lower());
            }

        }
        dict.__setitem__("lower",new PyMethodDescr("lower",PyUnicode.class,0,0,new exposed_lower(null,null)));
        class exposed_lstrip extends PyBuiltinMethodNarrow {

            exposed_lstrip(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_lstrip(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                try {
                    return new PyUnicode(((PyUnicode)self).unicode_lstrip(arg0.asStringOrNull(0)));
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
                return new PyUnicode(((PyUnicode)self).unicode_lstrip());
            }

        }
        dict.__setitem__("lstrip",new PyMethodDescr("lstrip",PyUnicode.class,0,1,new exposed_lstrip(null,null)));
        class exposed_replace extends PyBuiltinMethodNarrow {

            exposed_replace(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_replace(self,info);
            }

            public PyObject __call__(PyObject arg0,PyObject arg1,PyObject arg2) {
                try {
                    return((PyUnicode)self).unicode_replace(arg0,arg1,arg2.asInt(2));
                } catch (PyObject.ConversionException e) {
                    String msg;
                    switch (e.index) {
                    case 2:
                        msg="expected an integer";
                        break;
                    default:
                        msg="xxx";
                    }
                    throw Py.TypeError(msg);
                }
            }

            public PyObject __call__(PyObject arg0,PyObject arg1) {
                return((PyUnicode)self).unicode_replace(arg0,arg1);
            }

        }
        dict.__setitem__("replace",new PyMethodDescr("replace",PyUnicode.class,2,3,new exposed_replace(null,null)));
        class exposed_rfind extends PyBuiltinMethodNarrow {

            exposed_rfind(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_rfind(self,info);
            }

            public PyObject __call__(PyObject arg0,PyObject arg1,PyObject arg2) {
                try {
                    return Py.newInteger(((PyUnicode)self).unicode_rfind(arg0.asString(0),arg1.asInt(1),arg2.asInt(2)));
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
                    return Py.newInteger(((PyUnicode)self).unicode_rfind(arg0.asString(0),arg1.asInt(1)));
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
                    return Py.newInteger(((PyUnicode)self).unicode_rfind(arg0.asString(0)));
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
        class exposed_rindex extends PyBuiltinMethodNarrow {

            exposed_rindex(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_rindex(self,info);
            }

            public PyObject __call__(PyObject arg0,PyObject arg1,PyObject arg2) {
                try {
                    return Py.newInteger(((PyUnicode)self).unicode_rindex(arg0.asString(0),arg1.asInt(1),arg2.asInt(2)));
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
                    return Py.newInteger(((PyUnicode)self).unicode_rindex(arg0.asString(0),arg1.asInt(1)));
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
                    return Py.newInteger(((PyUnicode)self).unicode_rindex(arg0.asString(0)));
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
        class exposed_rjust extends PyBuiltinMethodNarrow {

            exposed_rjust(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_rjust(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                try {
                    return new PyUnicode(((PyUnicode)self).unicode_rjust(arg0.asInt(0)));
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
        class exposed_rstrip extends PyBuiltinMethodNarrow {

            exposed_rstrip(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_rstrip(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                try {
                    return new PyUnicode(((PyUnicode)self).unicode_rstrip(arg0.asStringOrNull(0)));
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
                return new PyUnicode(((PyUnicode)self).unicode_rstrip());
            }

        }
        dict.__setitem__("rstrip",new PyMethodDescr("rstrip",PyUnicode.class,0,1,new exposed_rstrip(null,null)));
        class exposed_split extends PyBuiltinMethodNarrow {

            exposed_split(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_split(self,info);
            }

            public PyObject __call__(PyObject arg0,PyObject arg1) {
                try {
                    return((PyUnicode)self).unicode_split(arg0.asStringOrNull(0),arg1.asInt(1));
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
                    return((PyUnicode)self).unicode_split(arg0.asStringOrNull(0));
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
                return((PyUnicode)self).unicode_split();
            }

        }
        dict.__setitem__("split",new PyMethodDescr("split",PyUnicode.class,0,2,new exposed_split(null,null)));
        class exposed_splitlines extends PyBuiltinMethodNarrow {

            exposed_splitlines(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_splitlines(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                return((PyUnicode)self).unicode_splitlines(arg0.__nonzero__());
            }

            public PyObject __call__() {
                return((PyUnicode)self).unicode_splitlines();
            }

        }
        dict.__setitem__("splitlines",new PyMethodDescr("splitlines",PyUnicode.class,0,1,new exposed_splitlines(null,null)));
        class exposed_startswith extends PyBuiltinMethodNarrow {

            exposed_startswith(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_startswith(self,info);
            }

            public PyObject __call__(PyObject arg0,PyObject arg1,PyObject arg2) {
                try {
                    return Py.newBoolean(((PyUnicode)self).unicode_startswith(arg0.asString(0),arg1.asInt(1),arg2.asInt(2)));
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
                    return Py.newBoolean(((PyUnicode)self).unicode_startswith(arg0.asString(0),arg1.asInt(1)));
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
                    return Py.newBoolean(((PyUnicode)self).unicode_startswith(arg0.asString(0)));
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
        class exposed_strip extends PyBuiltinMethodNarrow {

            exposed_strip(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_strip(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                try {
                    return new PyUnicode(((PyUnicode)self).unicode_strip(arg0.asStringOrNull(0)));
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
                return new PyUnicode(((PyUnicode)self).unicode_strip());
            }

        }
        dict.__setitem__("strip",new PyMethodDescr("strip",PyUnicode.class,0,1,new exposed_strip(null,null)));
        class exposed_swapcase extends PyBuiltinMethodNarrow {

            exposed_swapcase(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_swapcase(self,info);
            }

            public PyObject __call__() {
                return new PyUnicode(((PyUnicode)self).unicode_swapcase());
            }

        }
        dict.__setitem__("swapcase",new PyMethodDescr("swapcase",PyUnicode.class,0,0,new exposed_swapcase(null,null)));
        class exposed_title extends PyBuiltinMethodNarrow {

            exposed_title(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_title(self,info);
            }

            public PyObject __call__() {
                return new PyUnicode(((PyUnicode)self).unicode_title());
            }

        }
        dict.__setitem__("title",new PyMethodDescr("title",PyUnicode.class,0,0,new exposed_title(null,null)));
        class exposed_translate extends PyBuiltinMethodNarrow {

            exposed_translate(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_translate(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                return new PyUnicode(((PyUnicode)self).unicode_translate(arg0));
            }

        }
        dict.__setitem__("translate",new PyMethodDescr("translate",PyUnicode.class,1,1,new exposed_translate(null,null)));
        class exposed_upper extends PyBuiltinMethodNarrow {

            exposed_upper(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_upper(self,info);
            }

            public PyObject __call__() {
                return new PyUnicode(((PyUnicode)self).unicode_upper());
            }

        }
        dict.__setitem__("upper",new PyMethodDescr("upper",PyUnicode.class,0,0,new exposed_upper(null,null)));
        class exposed_zfill extends PyBuiltinMethodNarrow {

            exposed_zfill(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_zfill(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                try {
                    return new PyUnicode(((PyUnicode)self).unicode_zfill(arg0.asInt(0)));
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
        this(subtype, pystring.decode().toString());
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
                return new PyUnicode(((PyUnicode)S).string);
            }
            if (S instanceof PyString) {
                return new PyUnicode(codecs.decode((PyString)S, encoding, errors).toString());
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

    public PyObject __mod__(PyObject other) {
        return unicode___mod__(other);
    }

    final PyObject unicode___mod__(PyObject other){
        StringFormatter fmt = new StringFormatter(string, true);
        return fmt.format(other).__unicode__();
    }

    final PyUnicode unicode___unicode__() {
        return str___unicode__();
    }

    public PyString __str__() {
        return unicode___str__();
    }

    public PyString unicode___str__() {
        return new PyString(encode());
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

    final PyObject unicode_replace(PyObject oldPiece, PyObject newPiece) {
        return str_replace(oldPiece, newPiece);
    }

    final PyObject unicode_replace(PyObject oldPiece, PyObject newPiece, int maxsplit) {
        return str_replace(oldPiece, newPiece, maxsplit);
    }

    final PyString unicode_join(PyObject seq) {
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

    final PyObject unicode_decode() {
        return str_decode();
    }

    final PyObject unicode_decode(String encoding) {
        return str_decode(encoding);
    }

    final PyObject unicode_decode(String encoding, String errors) {
        return str_decode(encoding, errors);
    }
    
    final PyTuple unicode___getnewargs__() {
        return new PyTuple(new PyObject[] {new PyUnicode(this.string)});
    }

}
