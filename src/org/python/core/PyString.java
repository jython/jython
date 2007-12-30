/// Copyright (c) Corporation for National Research Initiatives
package org.python.core;

import java.io.UnsupportedEncodingException;

/**
 * A builtin python string.
 */
public class PyString extends PyBaseString implements ClassDictInit
{
    //~ BEGIN GENERATED REGION -- DO NOT EDIT SEE gexpose.py
    /* type info */

    public static final String exposed_name="str";

    public static void typeSetup(PyObject dict,PyType.Newstyle marker) {
        class exposed___ne__ extends PyBuiltinMethodNarrow {

            exposed___ne__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___ne__(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=((PyString)self).str___ne__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__ne__",new PyMethodDescr("__ne__",PyString.class,1,1,new exposed___ne__(null,null)));
        class exposed___eq__ extends PyBuiltinMethodNarrow {

            exposed___eq__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___eq__(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=((PyString)self).str___eq__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__eq__",new PyMethodDescr("__eq__",PyString.class,1,1,new exposed___eq__(null,null)));
        class exposed___lt__ extends PyBuiltinMethodNarrow {

            exposed___lt__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___lt__(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=((PyString)self).str___lt__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__lt__",new PyMethodDescr("__lt__",PyString.class,1,1,new exposed___lt__(null,null)));
        class exposed___le__ extends PyBuiltinMethodNarrow {

            exposed___le__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___le__(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=((PyString)self).str___le__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__le__",new PyMethodDescr("__le__",PyString.class,1,1,new exposed___le__(null,null)));
        class exposed___gt__ extends PyBuiltinMethodNarrow {

            exposed___gt__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___gt__(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=((PyString)self).str___gt__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__gt__",new PyMethodDescr("__gt__",PyString.class,1,1,new exposed___gt__(null,null)));
        class exposed___ge__ extends PyBuiltinMethodNarrow {

            exposed___ge__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___ge__(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=((PyString)self).str___ge__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__ge__",new PyMethodDescr("__ge__",PyString.class,1,1,new exposed___ge__(null,null)));
        class exposed___add__ extends PyBuiltinMethodNarrow {

            exposed___add__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___add__(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=((PyString)self).str___add__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__add__",new PyMethodDescr("__add__",PyString.class,1,1,new exposed___add__(null,null)));
        class exposed___mod__ extends PyBuiltinMethodNarrow {

            exposed___mod__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___mod__(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=((PyString)self).str___mod__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__mod__",new PyMethodDescr("__mod__",PyString.class,1,1,new exposed___mod__(null,null)));
        class exposed___mul__ extends PyBuiltinMethodNarrow {

            exposed___mul__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___mul__(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=((PyString)self).str___mul__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__mul__",new PyMethodDescr("__mul__",PyString.class,1,1,new exposed___mul__(null,null)));
        class exposed___rmul__ extends PyBuiltinMethodNarrow {

            exposed___rmul__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___rmul__(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=((PyString)self).str___rmul__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__rmul__",new PyMethodDescr("__rmul__",PyString.class,1,1,new exposed___rmul__(null,null)));
        class exposed___getitem__ extends PyBuiltinMethodNarrow {

            exposed___getitem__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___getitem__(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=((PyString)self).seq___finditem__(arg0);
                if (ret==null) {
                    throw Py.IndexError("index out of range: "+arg0);
                }
                return ret;
            }

        }
        dict.__setitem__("__getitem__",new PyMethodDescr("__getitem__",PyString.class,1,1,new exposed___getitem__(null,null)));
        class exposed___getslice__ extends PyBuiltinMethodNarrow {

            exposed___getslice__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___getslice__(self,info);
            }

            public PyObject __call__(PyObject arg0,PyObject arg1,PyObject arg2) {
                return((PyString)self).seq___getslice__(arg0,arg1,arg2);
            }

            public PyObject __call__(PyObject arg0,PyObject arg1) {
                return((PyString)self).seq___getslice__(arg0,arg1);
            }

        }
        dict.__setitem__("__getslice__",new PyMethodDescr("__getslice__",PyString.class,2,3,new exposed___getslice__(null,null)));
        class exposed___contains__ extends PyBuiltinMethodNarrow {

            exposed___contains__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___contains__(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                return Py.newBoolean(((PyString)self).str___contains__(arg0));
            }

        }
        dict.__setitem__("__contains__",new PyMethodDescr("__contains__",PyString.class,1,1,new exposed___contains__(null,null)));
        class exposed___len__ extends PyBuiltinMethodNarrow {

            exposed___len__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___len__(self,info);
            }

            public PyObject __call__() {
                return Py.newInteger(((PyString)self).str___len__());
            }

        }
        dict.__setitem__("__len__",new PyMethodDescr("__len__",PyString.class,0,0,new exposed___len__(null,null)));
        class exposed___reduce__ extends PyBuiltinMethodNarrow {

            exposed___reduce__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___reduce__(self,info);
            }

            public PyObject __call__() {
                return((PyString)self).str___reduce__();
            }

        }
        dict.__setitem__("__reduce__",new PyMethodDescr("__reduce__",PyString.class,0,0,new exposed___reduce__(null,null)));
        class exposed___str__ extends PyBuiltinMethodNarrow {

            exposed___str__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___str__(self,info);
            }

            public PyObject __call__() {
                return((PyString)self).str___str__();
            }

        }
        dict.__setitem__("__str__",new PyMethodDescr("__str__",PyString.class,0,0,new exposed___str__(null,null)));
        class exposed___unicode__ extends PyBuiltinMethodNarrow {

            exposed___unicode__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___unicode__(self,info);
            }

            public PyObject __call__() {
                return((PyString)self).str___unicode__();
            }

        }
        dict.__setitem__("__unicode__",new PyMethodDescr("__unicode__",PyString.class,0,0,new exposed___unicode__(null,null)));
        class exposed___hash__ extends PyBuiltinMethodNarrow {

            exposed___hash__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___hash__(self,info);
            }

            public PyObject __call__() {
                return Py.newInteger(((PyString)self).str_hashCode());
            }

        }
        dict.__setitem__("__hash__",new PyMethodDescr("__hash__",PyString.class,0,0,new exposed___hash__(null,null)));
        class exposed___repr__ extends PyBuiltinMethodNarrow {

            exposed___repr__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___repr__(self,info);
            }

            public PyObject __call__() {
                return new PyString(((PyString)self).str_toString());
            }

        }
        dict.__setitem__("__repr__",new PyMethodDescr("__repr__",PyString.class,0,0,new exposed___repr__(null,null)));
        class exposed_capitalize extends PyBuiltinMethodNarrow {

            exposed_capitalize(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_capitalize(self,info);
            }

            public PyObject __call__() {
                return new PyString(((PyString)self).str_capitalize());
            }

        }
        dict.__setitem__("capitalize",new PyMethodDescr("capitalize",PyString.class,0,0,new exposed_capitalize(null,null)));
        class exposed_center extends PyBuiltinMethodNarrow {

            exposed_center(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_center(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                try {
                    return new PyString(((PyString)self).str_center(arg0.asInt(0)));
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
        dict.__setitem__("center",new PyMethodDescr("center",PyString.class,1,1,new exposed_center(null,null)));
        class exposed_count extends PyBuiltinMethodNarrow {

            exposed_count(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_count(self,info);
            }

            public PyObject __call__(PyObject arg0,PyObject arg1,PyObject arg2) {
                try {
                    return Py.newInteger(((PyString)self).str_count(arg0.asString(0),arg1.asInt(1),arg2.asInt(2)));
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
                    return Py.newInteger(((PyString)self).str_count(arg0.asString(0),arg1.asInt(1)));
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
                    return Py.newInteger(((PyString)self).str_count(arg0.asString(0)));
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
        dict.__setitem__("count",new PyMethodDescr("count",PyString.class,1,3,new exposed_count(null,null)));
        class exposed_decode extends PyBuiltinMethodNarrow {

            exposed_decode(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_decode(self,info);
            }

            public PyObject __call__(PyObject arg0,PyObject arg1) {
                try {
                    return new PyUnicode(((PyString)self).str_decode(arg0.asString(0),arg1.asString(1)));
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
                    return new PyUnicode(((PyString)self).str_decode(arg0.asString(0)));
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
                return new PyUnicode(((PyString)self).str_decode());
            }

        }
        dict.__setitem__("decode",new PyMethodDescr("decode",PyString.class,0,2,new exposed_decode(null,null)));
        class exposed_encode extends PyBuiltinMethodNarrow {

            exposed_encode(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_encode(self,info);
            }

            public PyObject __call__(PyObject arg0,PyObject arg1) {
                try {
                    return new PyString(((PyString)self).str_encode(arg0.asString(0),arg1.asString(1)));
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
                    return new PyString(((PyString)self).str_encode(arg0.asString(0)));
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
                return new PyString(((PyString)self).str_encode());
            }

        }
        dict.__setitem__("encode",new PyMethodDescr("encode",PyString.class,0,2,new exposed_encode(null,null)));
        class exposed_endswith extends PyBuiltinMethodNarrow {

            exposed_endswith(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_endswith(self,info);
            }

            public PyObject __call__(PyObject arg0,PyObject arg1,PyObject arg2) {
                try {
                    return Py.newBoolean(((PyString)self).str_endswith(arg0.asString(0),arg1.asInt(1),arg2.asInt(2)));
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
                    return Py.newBoolean(((PyString)self).str_endswith(arg0.asString(0),arg1.asInt(1)));
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
                    return Py.newBoolean(((PyString)self).str_endswith(arg0.asString(0)));
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
        dict.__setitem__("endswith",new PyMethodDescr("endswith",PyString.class,1,3,new exposed_endswith(null,null)));
        class exposed_expandtabs extends PyBuiltinMethodNarrow {

            exposed_expandtabs(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_expandtabs(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                try {
                    return new PyString(((PyString)self).str_expandtabs(arg0.asInt(0)));
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
                return new PyString(((PyString)self).str_expandtabs());
            }

        }
        dict.__setitem__("expandtabs",new PyMethodDescr("expandtabs",PyString.class,0,1,new exposed_expandtabs(null,null)));
        class exposed_find extends PyBuiltinMethodNarrow {

            exposed_find(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_find(self,info);
            }

            public PyObject __call__(PyObject arg0,PyObject arg1,PyObject arg2) {
                try {
                    return Py.newInteger(((PyString)self).str_find(arg0.asString(0),arg1.asInt(1),arg2.asInt(2)));
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
                    return Py.newInteger(((PyString)self).str_find(arg0.asString(0),arg1.asInt(1)));
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
                    return Py.newInteger(((PyString)self).str_find(arg0.asString(0)));
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
        dict.__setitem__("find",new PyMethodDescr("find",PyString.class,1,3,new exposed_find(null,null)));
        class exposed_index extends PyBuiltinMethodNarrow {

            exposed_index(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_index(self,info);
            }

            public PyObject __call__(PyObject arg0,PyObject arg1,PyObject arg2) {
                try {
                    return Py.newInteger(((PyString)self).str_index(arg0.asString(0),arg1.asInt(1),arg2.asInt(2)));
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
                    return Py.newInteger(((PyString)self).str_index(arg0.asString(0),arg1.asInt(1)));
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
                    return Py.newInteger(((PyString)self).str_index(arg0.asString(0)));
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
        dict.__setitem__("index",new PyMethodDescr("index",PyString.class,1,3,new exposed_index(null,null)));
        class exposed_isalnum extends PyBuiltinMethodNarrow {

            exposed_isalnum(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_isalnum(self,info);
            }

            public PyObject __call__() {
                return Py.newBoolean(((PyString)self).str_isalnum());
            }

        }
        dict.__setitem__("isalnum",new PyMethodDescr("isalnum",PyString.class,0,0,new exposed_isalnum(null,null)));
        class exposed_isalpha extends PyBuiltinMethodNarrow {

            exposed_isalpha(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_isalpha(self,info);
            }

            public PyObject __call__() {
                return Py.newBoolean(((PyString)self).str_isalpha());
            }

        }
        dict.__setitem__("isalpha",new PyMethodDescr("isalpha",PyString.class,0,0,new exposed_isalpha(null,null)));
        class exposed_isdecimal extends PyBuiltinMethodNarrow {

            exposed_isdecimal(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_isdecimal(self,info);
            }

            public PyObject __call__() {
                return Py.newBoolean(((PyString)self).str_isdecimal());
            }

        }
        dict.__setitem__("isdecimal",new PyMethodDescr("isdecimal",PyString.class,0,0,new exposed_isdecimal(null,null)));
        class exposed_isdigit extends PyBuiltinMethodNarrow {

            exposed_isdigit(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_isdigit(self,info);
            }

            public PyObject __call__() {
                return Py.newBoolean(((PyString)self).str_isdigit());
            }

        }
        dict.__setitem__("isdigit",new PyMethodDescr("isdigit",PyString.class,0,0,new exposed_isdigit(null,null)));
        class exposed_islower extends PyBuiltinMethodNarrow {

            exposed_islower(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_islower(self,info);
            }

            public PyObject __call__() {
                return Py.newBoolean(((PyString)self).str_islower());
            }

        }
        dict.__setitem__("islower",new PyMethodDescr("islower",PyString.class,0,0,new exposed_islower(null,null)));
        class exposed_isnumeric extends PyBuiltinMethodNarrow {

            exposed_isnumeric(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_isnumeric(self,info);
            }

            public PyObject __call__() {
                return Py.newBoolean(((PyString)self).str_isnumeric());
            }

        }
        dict.__setitem__("isnumeric",new PyMethodDescr("isnumeric",PyString.class,0,0,new exposed_isnumeric(null,null)));
        class exposed_isspace extends PyBuiltinMethodNarrow {

            exposed_isspace(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_isspace(self,info);
            }

            public PyObject __call__() {
                return Py.newBoolean(((PyString)self).str_isspace());
            }

        }
        dict.__setitem__("isspace",new PyMethodDescr("isspace",PyString.class,0,0,new exposed_isspace(null,null)));
        class exposed_istitle extends PyBuiltinMethodNarrow {

            exposed_istitle(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_istitle(self,info);
            }

            public PyObject __call__() {
                return Py.newBoolean(((PyString)self).str_istitle());
            }

        }
        dict.__setitem__("istitle",new PyMethodDescr("istitle",PyString.class,0,0,new exposed_istitle(null,null)));
        class exposed_isunicode extends PyBuiltinMethodNarrow {

            exposed_isunicode(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_isunicode(self,info);
            }

            public PyObject __call__() {
                return Py.newBoolean(((PyString)self).str_isunicode());
            }

        }
        dict.__setitem__("isunicode",new PyMethodDescr("isunicode",PyString.class,0,0,new exposed_isunicode(null,null)));
        class exposed_isupper extends PyBuiltinMethodNarrow {

            exposed_isupper(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_isupper(self,info);
            }

            public PyObject __call__() {
                return Py.newBoolean(((PyString)self).str_isupper());
            }

        }
        dict.__setitem__("isupper",new PyMethodDescr("isupper",PyString.class,0,0,new exposed_isupper(null,null)));
        class exposed_join extends PyBuiltinMethodNarrow {

            exposed_join(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_join(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                return((PyString)self).str_join(arg0);
            }

        }
        dict.__setitem__("join",new PyMethodDescr("join",PyString.class,1,1,new exposed_join(null,null)));
        class exposed_ljust extends PyBuiltinMethodNarrow {

            exposed_ljust(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_ljust(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                try {
                    return new PyString(((PyString)self).str_ljust(arg0.asInt(0)));
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
        dict.__setitem__("ljust",new PyMethodDescr("ljust",PyString.class,1,1,new exposed_ljust(null,null)));
        class exposed_lower extends PyBuiltinMethodNarrow {

            exposed_lower(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_lower(self,info);
            }

            public PyObject __call__() {
                return new PyString(((PyString)self).str_lower());
            }

        }
        dict.__setitem__("lower",new PyMethodDescr("lower",PyString.class,0,0,new exposed_lower(null,null)));
        class exposed_lstrip extends PyBuiltinMethodNarrow {

            exposed_lstrip(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_lstrip(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                try {
                    return new PyString(((PyString)self).str_lstrip(arg0.asStringOrNull(0)));
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
                return new PyString(((PyString)self).str_lstrip());
            }

        }
        dict.__setitem__("lstrip",new PyMethodDescr("lstrip",PyString.class,0,1,new exposed_lstrip(null,null)));
        class exposed_replace extends PyBuiltinMethodNarrow {

            exposed_replace(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_replace(self,info);
            }

            public PyObject __call__(PyObject arg0,PyObject arg1,PyObject arg2) {
                try {
                    return((PyString)self).str_replace(arg0,arg1,arg2.asInt(2));
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
                return((PyString)self).str_replace(arg0,arg1);
            }

        }
        dict.__setitem__("replace",new PyMethodDescr("replace",PyString.class,2,3,new exposed_replace(null,null)));
        class exposed_rfind extends PyBuiltinMethodNarrow {

            exposed_rfind(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_rfind(self,info);
            }

            public PyObject __call__(PyObject arg0,PyObject arg1,PyObject arg2) {
                try {
                    return Py.newInteger(((PyString)self).str_rfind(arg0.asString(0),arg1.asInt(1),arg2.asInt(2)));
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
                    return Py.newInteger(((PyString)self).str_rfind(arg0.asString(0),arg1.asInt(1)));
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
                    return Py.newInteger(((PyString)self).str_rfind(arg0.asString(0)));
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
        dict.__setitem__("rfind",new PyMethodDescr("rfind",PyString.class,1,3,new exposed_rfind(null,null)));
        class exposed_rindex extends PyBuiltinMethodNarrow {

            exposed_rindex(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_rindex(self,info);
            }

            public PyObject __call__(PyObject arg0,PyObject arg1,PyObject arg2) {
                try {
                    return Py.newInteger(((PyString)self).str_rindex(arg0.asString(0),arg1.asInt(1),arg2.asInt(2)));
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
                    return Py.newInteger(((PyString)self).str_rindex(arg0.asString(0),arg1.asInt(1)));
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
                    return Py.newInteger(((PyString)self).str_rindex(arg0.asString(0)));
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
        dict.__setitem__("rindex",new PyMethodDescr("rindex",PyString.class,1,3,new exposed_rindex(null,null)));
        class exposed_rjust extends PyBuiltinMethodNarrow {

            exposed_rjust(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_rjust(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                try {
                    return new PyString(((PyString)self).str_rjust(arg0.asInt(0)));
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
        dict.__setitem__("rjust",new PyMethodDescr("rjust",PyString.class,1,1,new exposed_rjust(null,null)));
        class exposed_rstrip extends PyBuiltinMethodNarrow {

            exposed_rstrip(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_rstrip(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                try {
                    return new PyString(((PyString)self).str_rstrip(arg0.asStringOrNull(0)));
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
                return new PyString(((PyString)self).str_rstrip());
            }

        }
        dict.__setitem__("rstrip",new PyMethodDescr("rstrip",PyString.class,0,1,new exposed_rstrip(null,null)));
        class exposed_split extends PyBuiltinMethodNarrow {

            exposed_split(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_split(self,info);
            }

            public PyObject __call__(PyObject arg0,PyObject arg1) {
                try {
                    return((PyString)self).str_split(arg0.asStringOrNull(0),arg1.asInt(1));
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
                    return((PyString)self).str_split(arg0.asStringOrNull(0));
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
                return((PyString)self).str_split();
            }

        }
        dict.__setitem__("split",new PyMethodDescr("split",PyString.class,0,2,new exposed_split(null,null)));
        class exposed_splitlines extends PyBuiltinMethodNarrow {

            exposed_splitlines(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_splitlines(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                return((PyString)self).str_splitlines(arg0.__nonzero__());
            }

            public PyObject __call__() {
                return((PyString)self).str_splitlines();
            }

        }
        dict.__setitem__("splitlines",new PyMethodDescr("splitlines",PyString.class,0,1,new exposed_splitlines(null,null)));
        class exposed_startswith extends PyBuiltinMethodNarrow {

            exposed_startswith(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_startswith(self,info);
            }

            public PyObject __call__(PyObject arg0,PyObject arg1,PyObject arg2) {
                try {
                    return Py.newBoolean(((PyString)self).str_startswith(arg0.asString(0),arg1.asInt(1),arg2.asInt(2)));
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
                    return Py.newBoolean(((PyString)self).str_startswith(arg0.asString(0),arg1.asInt(1)));
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
                    return Py.newBoolean(((PyString)self).str_startswith(arg0.asString(0)));
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
        dict.__setitem__("startswith",new PyMethodDescr("startswith",PyString.class,1,3,new exposed_startswith(null,null)));
        class exposed_strip extends PyBuiltinMethodNarrow {

            exposed_strip(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_strip(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                try {
                    return new PyString(((PyString)self).str_strip(arg0.asStringOrNull(0)));
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
                return new PyString(((PyString)self).str_strip());
            }

        }
        dict.__setitem__("strip",new PyMethodDescr("strip",PyString.class,0,1,new exposed_strip(null,null)));
        class exposed_swapcase extends PyBuiltinMethodNarrow {

            exposed_swapcase(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_swapcase(self,info);
            }

            public PyObject __call__() {
                return new PyString(((PyString)self).str_swapcase());
            }

        }
        dict.__setitem__("swapcase",new PyMethodDescr("swapcase",PyString.class,0,0,new exposed_swapcase(null,null)));
        class exposed_title extends PyBuiltinMethodNarrow {

            exposed_title(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_title(self,info);
            }

            public PyObject __call__() {
                return new PyString(((PyString)self).str_title());
            }

        }
        dict.__setitem__("title",new PyMethodDescr("title",PyString.class,0,0,new exposed_title(null,null)));
        class exposed_translate extends PyBuiltinMethodNarrow {

            exposed_translate(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_translate(self,info);
            }

            public PyObject __call__(PyObject arg0,PyObject arg1) {
                try {
                    return new PyString(((PyString)self).str_translate(arg0.asString(0),arg1.asString(1)));
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
                    return new PyString(((PyString)self).str_translate(arg0.asString(0)));
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
        dict.__setitem__("translate",new PyMethodDescr("translate",PyString.class,1,2,new exposed_translate(null,null)));
        class exposed_upper extends PyBuiltinMethodNarrow {

            exposed_upper(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_upper(self,info);
            }

            public PyObject __call__() {
                return new PyString(((PyString)self).str_upper());
            }

        }
        dict.__setitem__("upper",new PyMethodDescr("upper",PyString.class,0,0,new exposed_upper(null,null)));
        class exposed_zfill extends PyBuiltinMethodNarrow {

            exposed_zfill(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_zfill(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                try {
                    return new PyString(((PyString)self).str_zfill(arg0.asInt(0)));
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
        dict.__setitem__("zfill",new PyMethodDescr("zfill",PyString.class,1,1,new exposed_zfill(null,null)));
        dict.__setitem__("__new__",new PyNewWrapper(PyString.class,"__new__",-1,-1) {

                                                                                        public PyObject new_impl(boolean init,PyType subtype,PyObject[]args,String[]keywords) {
                                                                                            return str_new(this,init,subtype,args,keywords);
                                                                                        }

                                                                                    });
    }
    //~ END GENERATED REGION -- DO NOT EDIT SEE gexpose.py

    private static final PyType STRTYPE = PyType.fromClass(PyString.class);

    protected String string;
    private transient int cached_hashcode=0;
    private transient boolean interned=false;

    // for PyJavaClass.init()
    public PyString() {
        this(STRTYPE, "");
    }

    public PyString(PyType subType, String string) {
        super(subType);
        if (string == null) {
            throw new IllegalArgumentException(
                            "Cannot create PyString from null!");
        }
        this.string = string;
    }

    public PyString(String string) {
        this(STRTYPE, string);
    }

    public PyString(char c) {
        this(STRTYPE,String.valueOf(c));
    }

    final static PyObject str_new(PyNewWrapper new_, boolean init, PyType subtype,
            PyObject[] args, String[] keywords) {
        ArgParser ap = new ArgParser("str", args, keywords, new String[] { "object" }, 0);
        PyObject S = ap.getPyObject(0, null);
        if(new_.for_type == subtype) {
            if(S == null) {
                return new PyString("");
            }
            return new PyString(S.__str__().toString());
        } else {
            if (S == null) {
                return new PyStringDerived(subtype, "");
            }
            return new PyStringDerived(subtype, S.__str__().toString());
        }
    }

    /** <i>Internal use only. Do not call this method explicit.</i> */
    public static void classDictInit(PyObject dict) throws PyIgnoreMethodTag {}

    public String safeRepr() throws PyIgnoreMethodTag {
        return "'string' object";
    }

    public PyString __str__() {
        return str___str__();
    }

    final PyString str___str__() {
        return this;
    }

    public PyUnicode __unicode__() {
        return str___unicode__();
    }

    final PyUnicode str___unicode__() {
        return new PyUnicode(this);
    }

    public int __len__() {
        return str___len__();
    }

    final int str___len__() {
        return string.length();
    }

    public String toString() {
        return str_toString();
    }

    final String str_toString() {
        return string;
    }

    public String internedString() {
        if (interned)
            return string;
        else {
            string = string.intern();
            interned = true;
            return string;
        }
    }

    public PyString __repr__() {
        return new PyString(encode_UnicodeEscape(string, true));
    }

    private static char[] hexdigit = "0123456789ABCDEF".toCharArray();

    public static String encode_UnicodeEscape(String str,
                                              boolean use_quotes)
    {
        return encode_UnicodeEscape(str, use_quotes, false);
    }
    
    public static String encode_UnicodeEscape(String str,
                                              boolean use_quotes,
                                              boolean unicodeDesignatorPresent)
    {
        int size = str.length();
        StringBuffer v = new StringBuffer(str.length());

        char quote = 0;

        if (use_quotes) {
            quote = str.indexOf('\'') >= 0 &&
                             str.indexOf('"') == -1 ? '"' : '\'';
            v.append(quote);
        }

        for (int i = 0; size-- > 0; ) {
            int ch = str.charAt(i++);
            /* Escape quotes */
            if (use_quotes && (ch == quote || ch == '\\')) {
                v.append('\\');
                v.append((char) ch);
            }
            /* Map 16-bit characters to '\\uxxxx' */
            else if (ch >= 256) {
                if (use_quotes && !unicodeDesignatorPresent) {
                   v.insert(0, 'u');
                   unicodeDesignatorPresent = true;
                }
                v.append('\\');
                v.append('u');
                v.append(hexdigit[(ch >> 12) & 0xf]);
                v.append(hexdigit[(ch >> 8) & 0xf]);
                v.append(hexdigit[(ch >> 4) & 0xf]);
                v.append(hexdigit[ch & 15]);
            }
            /* Map non-printable US ASCII to '\ooo' */
            else if (use_quotes && ch == '\n') v.append("\\n");
            else if (use_quotes && ch == '\t') v.append("\\t");
            else if (use_quotes && ch == '\b') v.append("\\b");
            else if (use_quotes && ch == '\f') v.append("\\f");
            else if (use_quotes && ch == '\r') v.append("\\r");
            else if (ch < ' ' || ch >= 127) {
                v.append("\\x");
                v.append(hexdigit[(ch >> 4) & 0xF]);
                v.append(hexdigit[ch & 0xF]);
            }
            /* Copy everything else as-is */
            else
                v.append((char) ch);
        }
        if (use_quotes)
            v.append(quote);
        return v.toString();
    }

    private static ucnhashAPI pucnHash = null;

    public static String decode_UnicodeEscape(String str, int start, int end,
                                              String errors, boolean unicode)
    {
        StringBuffer v = new StringBuffer(end-start);
        for (int s = start; s < end; ) {
            char ch = str.charAt(s);

            /* Non-escape characters are interpreted as Unicode ordinals */
            if (ch != '\\') {
                v.append(ch);
                s++;
                continue;
            }

            /* \ - Escapes */
            s++;
			if (s == end) {
				codecs.decoding_error("unicode escape", v, errors, "\\ at end of string");
				break;
			}
            ch = str.charAt(s++);
            switch (ch) {

            /* \x escapes */
            case '\n': break;
            case '\\': v.append('\\'); break;
            case '\'': v.append('\''); break;
            case '\"': v.append('\"'); break;
            case 'b': v.append('\b'); break;
            case 'f': v.append('\014'); break; /* FF */
            case 't': v.append('\t'); break;
            case 'n': v.append('\n'); break;
            case 'r': v.append('\r'); break;
            case 'v': v.append('\013'); break; /* VT */
            case 'a': v.append('\007'); break; /* BEL, not classic C */

            /* \OOO (octal) escapes */
            case '0': case '1': case '2': case '3':
            case '4': case '5': case '6': case '7':

                int x = Character.digit(ch, 8);
                for (int j = 0; j < 2 && s < end; j++, s++) {
                    ch = str.charAt(s);
                    if (ch < '0' || ch > '7')
                        break;
                    x = (x<<3) + Character.digit(ch, 8);
                }
                v.append((char) x);
                break;

            case 'x':
                int i;
                for (x = 0, i = 0; i < 2 && s < end; i++) {
                    ch = str.charAt(s + i);
                    int d = Character.digit(ch, 16);
                    if (d == -1) {
                        codecs.decoding_error("unicode escape", v, errors,
                                                     "truncated \\xXX");
                        i++;
                        break;
                    }

                    x = ((x<<4) & ~0xF) + d;
                }
                s += i;
                v.append((char) x);
                break;

            /* \ uXXXX with 4 hex digits */
            case 'u':
                if (!unicode) {
                    v.append('\\');
                    v.append('u');
                    break;
                }
                if (s+4 > end) {
                    codecs.decoding_error("unicode escape", v, errors,
                                              "truncated \\uXXXX");
                    break;
                }
                for (x = 0, i = 0; i < 4; i++) {
                    ch = str.charAt(s + i);
                    int d  = Character.digit(ch, 16);
                    if (d == -1) {
                        codecs.decoding_error("unicode escape", v, errors,
                                              "truncated \\uXXXX");
                        break;
                    }
                    x = ((x<<4) & ~0xF) + d;
                }
                s += i;
                v.append((char) x);
                break;

            case 'N':
                if (!unicode) {
                    v.append('\\');
                    v.append('N');
                    break;
                }
                /* Ok, we need to deal with Unicode Character Names now,
                 * make sure we've imported the hash table data...
                 */
                if (pucnHash == null) {
                     PyObject mod = imp.importName("ucnhash", true);
                     mod = mod.__call__();
                     pucnHash = (ucnhashAPI) mod.__tojava__(Object.class);
                     if (pucnHash.getCchMax() < 0)
                         codecs.decoding_error("unicode escape", v, errors,
                                 "Unicode names not loaded");
                }

                if (str.charAt(s) == '{') {
                    int startName = s + 1;
                    int endBrace = startName;

                    /* look for either the closing brace, or we
                     * exceed the maximum length of the unicode
                     * character names
                     */
                    int maxLen = pucnHash.getCchMax();
                    while (endBrace < end && str.charAt(endBrace) != '}'
                           && (endBrace - startName) <= maxLen) {
                        endBrace++;
                    }
                    if (endBrace != end && str.charAt(endBrace) == '}') {
                         int value = pucnHash.getValue(str, startName,
                                                       endBrace);
                         if (value < 0) {
                             codecs.decoding_error("unicode escape", v,
                                  errors, "Invalid Unicode Character Name");
                         } else {

							if (value < 1 << 16) {
								/* In UCS-2 range, easy solution.. */
								v.append((char) value);
							} else {
								/* Oops, its in UCS-4 space, */
								/* compute and append the two surrogates: */
								/* translate from 10000..10FFFF to 0..FFFFF */
								value -= 0x10000;

								/* high surrogate = top 10 bits added to D800 */
								v.append((char) (0xD800 + (value >> 10)));

								/* low surrogate = bottom 10 bits added to DC00 */
								v.append((char) (0xDC00 + (value & ~0xFC00)));
							}
						}
                        s = endBrace + 1;
                        break;
                    } else {
                         codecs.decoding_error("unicode escape", v, errors,
                              "Unicode name missing closing brace");
                    }
                } else {
					codecs.decoding_error("unicode escape", v, errors, "Missing opening brace for Unicode "
							+ "Character Name escape");
				}
                break;

           default:
               v.append('\\');
               v.append(str.charAt(s-1));
               break;
           }
       }
       return v.toString();
    }

    public int __cmp__(PyObject other) {
        return str___cmp__(other);
    }

    final int str___cmp__(PyObject other) {
        if (!(other instanceof PyString))
            return -2;

        int c = string.compareTo(((PyString)other).string);
        return c < 0 ? -1 : c > 0 ? 1 : 0;
    }

    public PyObject __eq__(PyObject other) {
        return str___eq__(other);
    }

    final PyObject str___eq__(PyObject other) {
        String s = coerce(other);
        if (s == null)
            return null;
        return string.equals(s) ? Py.One : Py.Zero;
    }

    public PyObject __ne__(PyObject other) {
        return str___ne__(other);
    }

    final PyObject str___ne__(PyObject other) {
        String s = coerce(other);
        if (s == null)
            return null;
        return string.equals(s) ? Py.Zero : Py.One;
    }
    
    public PyObject __lt__(PyObject other) {
        String s = coerce(other);
        if (s == null)
            return null;
        return string.compareTo(s) < 0 ? Py.One : Py.Zero;
    }
    
    final PyObject str___lt__(PyObject other){
    	return __lt__(other);
    }

    public PyObject __le__(PyObject other) {
        String s = coerce(other);
        if (s == null)
            return null;
        return string.compareTo(s) <= 0 ? Py.One : Py.Zero;
    }
    
    final PyObject str___le__(PyObject other){
    	return __le__(other);
    }

    public PyObject __gt__(PyObject other) {
        String s = coerce(other);
        if (s == null)
            return null;
        return string.compareTo(s) > 0 ? Py.One : Py.Zero;
    }
    
    final PyObject str___gt__(PyObject other){
    	return __gt__(other);
    }

    public PyObject __ge__(PyObject other) {
        String s = coerce(other);
        if (s == null)
            return null;
        return string.compareTo(s) >= 0 ? Py.One : Py.Zero;
    }
    
    final PyObject str___ge__(PyObject other){
    	return __ge__(other);
    }

    private static String coerce(PyObject o) {
        if (o instanceof PyString)
            return o.toString();
        return null;
    }

    public int hashCode() {
        return str_hashCode();
    }

    final int str_hashCode() {
        if (cached_hashcode == 0)
            cached_hashcode = string.hashCode();
        return cached_hashcode;
    }

    /**
     * @return a byte array with one byte for each char in this object's
     *         underlying String. Each byte contains the low-order bits of its
     *         corresponding char.
     */
    public byte[] toBytes() {
        return to_bytes(string);
    }

    /**
     * @return a byte array with one byte for each char in s. Each byte contains
     *         the low-order bits of its corresponding char.
     */
    public static byte[] to_bytes(String s) {
        try {
            return s.getBytes("ISO-8859-1");
        } catch(UnsupportedEncodingException e) {
            // This JVM is whacked, it doesn't even have iso-8859-1
            throw Py.SystemError("Java couldn't find the ISO-8859-1 encoding");
        }
    }

    /**
     * @return A String with chars corresponding to the bytes in buf
     */
    public static String from_bytes(byte[] buf) {
        return from_bytes(buf, 0, buf.length);
    }
    
    /**
     * @return A String of len buff with chars corresponding to buf from off to
     *         off + len
     */
    public static String from_bytes(byte[] buf, int off, int len) {
        // Yes, I known the method is deprecated, but it is the fastest
        // way of converting between between byte[] and String
        return new String(buf, 0, off, len);
    }

    public Object __tojava__(Class c) {
        if (c.isAssignableFrom(String.class)) {
            return string;
        }

        if (c == Character.TYPE || c == Character.class)
            if (string.length() == 1)
                return new Character(string.charAt(0));

        if (c.isArray()) {
            if (c.getComponentType() == Byte.TYPE)
                return toBytes();
            if (c.getComponentType() == Character.TYPE)
                return string.toCharArray();
        }

        if (c.isInstance(this))
            return this;

        return Py.NoConversion;
    }

    protected PyObject pyget(int i) {
        return Py.newString(string.charAt(i));
    }

    protected PyObject getslice(int start, int stop, int step) {
        if (step > 0 && stop < start)
            stop = start;
        if (step == 1)
            return fromSubstring(start, stop);
        else {
            int n = sliceLength(start, stop, step);
            char new_chars[] = new char[n];
            int j = 0;
            for (int i=start; j<n; i+=step)
                new_chars[j++] = string.charAt(i);

            return createInstance(new String(new_chars));
        }
    }

    public PyString createInstance(String str) {
        return new PyString(str);
    }

    public boolean __contains__(PyObject o) {
        return str___contains__(o);
    }

    final boolean str___contains__(PyObject o) {
        if (!(o instanceof PyString))
            throw Py.TypeError("'in <string>' requires string as left operand");
        PyString other = (PyString) o;
        return string.indexOf(other.string) >= 0;
    }

    protected PyObject repeat(int count) {
        if (count < 0)
            count = 0;
        int s = string.length();
        char new_chars[] = new char[s*count];
        for (int i=0; i<count; i++) {
            string.getChars(0, s, new_chars, i*s);
        }
        return createInstance(new String(new_chars));
    }

    final PyObject str___mul__(PyObject o) {
        if (!(o instanceof PyInteger || o instanceof PyLong))
            return null;
        int count = ((PyInteger)o.__int__()).getValue();
        return repeat(count);
    }

    final PyObject str___rmul__(PyObject o) {
        if (!(o instanceof PyInteger || o instanceof PyLong))
            return null;
        int count = ((PyInteger)o.__int__()).getValue();
        return repeat(count);
    }

    public PyObject __add__(PyObject generic_other) {
        return str___add__(generic_other);
    }

    final PyObject str___add__(PyObject generic_other) {
        if (generic_other instanceof PyString) {
            PyString other = (PyString)generic_other;
            String result = string.concat(other.string);
            if (generic_other instanceof PyUnicode) {
                return new PyUnicode(result);
            }
            return createInstance(result);
        }
        else return null;
    }

    /**
     * Used for pickling.
     *
     * @return a tuple of (class, tuple)
     */
    public PyObject __reduce__() {
        return str___reduce__();
    }

    final PyObject str___reduce__() {
        return object___reduce__();
    }

    public PyTuple __getnewargs__() {
        return new PyTuple(new PyObject[]{
            new PyString(str_toString())
            }
        );
    }

    public PyObject __mod__(PyObject other) {
        return str___mod__(other);
    }
    
    public PyObject str___mod__(PyObject other){
        StringFormatter fmt = new StringFormatter(string);
        return fmt.format(other);
    }

    public PyObject __int__() {
        return Py.newInteger(atoi(10));
    }

    public PyLong __long__() {
        return atol(10);
    }

    public PyFloat __float__() {
        return new PyFloat(atof());
    }

    public PyObject __pos__() {
      throw Py.TypeError("bad operand type for unary +");
    }

    public PyObject __neg__() {
      throw Py.TypeError("bad operand type for unary -");
    }

    public PyObject __invert__() {
      throw Py.TypeError("bad operand type for unary ~");
    }

    public PyComplex __complex__() {
        boolean got_re = false;
        boolean got_im = false;
        boolean done = false;
        boolean sw_error = false;

        int s = 0;
        int n = string.length();
        while (s < n && Character.isSpaceChar(string.charAt(s)))
            s++;

        if (s == n) {
            throw Py.ValueError("empty string for complex()");
        }

        double z = -1.0;
        double x = 0.0;
        double y = 0.0;

        int sign = 1;
        do {
            char c = string.charAt(s);
            switch (c) {
            case '-':
                sign = -1;
                /* Fallthrough */
            case '+':
                if (done || s+1 == n) {
                    sw_error = true;
                    break;
                }
                //  a character is guaranteed, but it better be a digit
                //  or J or j
                c = string.charAt(++s);  //  eat the sign character
                                         //  and check the next
                if  (!Character.isDigit(c) && c!='J' && c!='j')
                    sw_error = true;
                break;

            case 'J':
            case 'j':
                if (got_im || done) {
                    sw_error = true;
                    break;
                }
                if  (z < 0.0) {
                    y = sign;
                } else {
                    y = sign * z;
                }
                got_im = true;
                done = got_re;
                sign = 1;
                s++; // eat the J or j
                break;

            case ' ':
                while (s < n && Character.isSpaceChar(string.charAt(s)))
                    s++;
                if (s != n)
                    sw_error = true;
                break;

            default:
                boolean digit_or_dot = (c == '.' || Character.isDigit(c));
                if (!digit_or_dot) {
                    sw_error = true;
                    break;
                }
                int end = endDouble(string, s);
                z = Double.valueOf(string.substring(s, end)).doubleValue();
                s=end;
                if (s < n) {
                    c = string.charAt(s);
                    if  (c == 'J' || c == 'j') {
                        break;
                    }
                }
                if  (got_re) {
                   sw_error = true;
                   break;
                }

                /* accept a real part */
                x = sign * z;
                got_re = true;
                done = got_im;
                z = -1.0;
                sign = 1;
                break;

             }  /* end of switch  */

        } while (s < n && !sw_error);

        if (sw_error) {
            throw Py.ValueError("malformed string for complex() " +
                                string.substring(s));
        }

        return new PyComplex(x,y);
    }

    private int endDouble(String string, int s) {
        int n = string.length();
        while (s < n) {
            char c = string.charAt(s++);
            if (Character.isDigit(c))
                continue;
            if (c == '.')
                continue;
            if (c == 'e' || c == 'E') {
                if (s < n) {
                    c = string.charAt(s);
                    if (c == '+' || c == '-')
                        s++;
                    continue;
                }
            }
            return s-1;
        }
        return s;
    }

    // Add in methods from string module
    public String lower() {
        return str_lower();
    }

    final String str_lower() {
        return string.toLowerCase();
    }

    public String upper() {
        return str_upper();
    }

    final String str_upper() {
        return string.toUpperCase();
    }

    public String title() {
        return str_title();
    }

    final String str_title() {
        char[] chars = string.toCharArray();
        int n = chars.length;

        boolean previous_is_cased = false;
        for (int i = 0; i < n; i++) {
            char ch = chars[i];
            if (previous_is_cased)
                chars[i] = Character.toLowerCase(ch);
            else
                chars[i] = Character.toTitleCase(ch);

            if (Character.isLowerCase(ch) ||
                   Character.isUpperCase(ch) ||
                   Character.isTitleCase(ch))
                previous_is_cased = true;
            else
                previous_is_cased = false;
        }
        return new String(chars);
    }

    public String swapcase() {
        return str_swapcase();
    }

    final String str_swapcase() {
        char[] chars = string.toCharArray();
        int n=chars.length;
        for (int i=0; i<n; i++) {
            char c = chars[i];
            if (Character.isUpperCase(c)) {
                chars[i] = Character.toLowerCase(c);
            }
            else if (Character.isLowerCase(c)) {
                chars[i] = Character.toUpperCase(c);
            }
        }
        return new String(chars);
    }

    public String strip() {
        return str_strip();
    }

    final String str_strip() {
        return str_strip(null);
    }

    public String strip(String sep) {
        return str_strip(sep);
    }

    final String str_strip(String sep) {
        char[] chars = string.toCharArray();
        int n=chars.length;
        int start=0;
        if (sep == null)
            while (start < n && Character.isWhitespace(chars[start]))
                start++;
        else
            while (start < n && sep.indexOf(chars[start]) >= 0)
                start++;

        int end=n-1;
        if (sep == null)
            while (end >= 0 && Character.isWhitespace(chars[end]))
                end--;
        else
            while (end >= 0 && sep.indexOf(chars[end]) >= 0)
                end--;

        if (end >= start) {
            return (end < n-1 || start > 0)
                ? string.substring(start, end+1) : string;
        } else {
            return "";
        }
    }

    public String lstrip() {
        return str_lstrip();
    }

    final String str_lstrip() {
        return str_lstrip(null);
    }

    public String lstrip(String sep) {
        return str_lstrip(sep);
    }

    final String str_lstrip(String sep) {
        char[] chars = string.toCharArray();
        int n=chars.length;
        int start=0;
        if (sep == null)
            while (start < n && Character.isWhitespace(chars[start]))
                start++;
        else
            while (start < n && sep.indexOf(chars[start]) >= 0)
                start++;

        return (start > 0) ? string.substring(start, n) : string;
    }

    public String rstrip() {
        return str_rstrip();
    }

    final String str_rstrip() {
        return str_rstrip(null);
    }

    public String rstrip(String sep) {
        return str_rstrip(sep);
    }

    final String str_rstrip(String sep) {
        char[] chars = string.toCharArray();
        int n=chars.length;
        int end=n-1;
        if (sep == null)
            while (end >= 0 && Character.isWhitespace(chars[end]))
                end--;
        else
            while (end >= 0 && sep.indexOf(chars[end]) >= 0)
                end--;

        return (end < n-1) ? string.substring(0, end+1) : string;
    }


    public PyList split() {
        return str_split();
    }

    final PyList str_split() {
        return str_split(null, -1);
    }

    public PyList split(String sep) {
        return str_split(sep);
    }

    final PyList str_split(String sep) {
        return str_split(sep, -1);
    }

    public PyList split(String sep, int maxsplit) {
        return str_split(sep, maxsplit);
    }

    final PyList str_split(String sep, int maxsplit) {
        if (sep != null)
            return splitfields(sep, maxsplit);

        PyList list = new PyList();

        char[] chars = string.toCharArray();
        int n=chars.length;

        if (maxsplit < 0)
            maxsplit = n;

        int splits=0;
        int index=0;
        while (index < n && splits < maxsplit) {
            while (index < n && Character.isWhitespace(chars[index]))
                index++;
            if (index == n)
                break;
            int start = index;

            while (index < n && !Character.isWhitespace(chars[index]))
                index++;
            list.append(fromSubstring(start, index));
            splits++;
        }
        while (index < n && Character.isWhitespace(chars[index]))
            index++;
        if (index < n) {
            list.append(fromSubstring(index, n));
        }
        return list;
    }

    private PyList splitfields(String sep, int maxsplit) {
        if (sep.length() == 0) {
            throw Py.ValueError("empty separator");
        }

        PyList list = new PyList();

        int length = string.length();
        if (maxsplit < 0)
            maxsplit = length;

        int lastbreak = 0;
        int splits = 0;
        int sepLength = sep.length();
        while (splits < maxsplit) {
            int index = string.indexOf(sep, lastbreak);
            if (index == -1)
                break;
            splits += 1;
            list.append(fromSubstring(lastbreak, index));
            lastbreak = index + sepLength;
        }
        if (lastbreak <= length) {
            list.append(fromSubstring(lastbreak, length));
        }
        return list;
    }

    public PyList splitlines() {
        return str_splitlines();
    }

    final PyList str_splitlines() {
        return str_splitlines(false);
    }

    public PyList splitlines(boolean keepends) {
        return str_splitlines(keepends);
    }

    final PyList str_splitlines(boolean keepends) {
        PyList list = new PyList();

        char[] chars = string.toCharArray();
        int n=chars.length;

        int j = 0;
        for (int i = 0; i < n; ) {
            /* Find a line and append it */
            while (i < n && chars[i] != '\n' && chars[i] != '\r' &&
                    Character.getType(chars[i]) != Character.LINE_SEPARATOR)
                i++;

            /* Skip the line break reading CRLF as one line break */
            int eol = i;
            if (i < n) {
                if (chars[i] == '\r' && i + 1 < n && chars[i+1] == '\n')
                    i += 2;
                else
                    i++;
                if (keepends)
                    eol = i;
            }
            list.append(fromSubstring(j, eol));
            j = i;
        }
        if (j < n) {
            list.append(fromSubstring(j, n));
        }
        return list;
    }

    protected PyString fromSubstring(int begin, int end) {
        return createInstance(string.substring(begin, end));
    }

    public int index(String sub) {
        return str_index(sub);
    }

    final int str_index(String sub) {
        return str_index(sub, 0, string.length());
    }

    public int index(String sub, int start) {
        return str_index(sub, start);
    }

    final int str_index(String sub, int start) {
        return str_index(sub, start, string.length());
    }

    public int index(String sub, int start, int end) {
        return str_index(sub, start, end);
    }

    final int str_index(String sub, int start, int end) {
        int n = string.length();

        if (start < 0)
            start = n+start;
        if (end < 0)
            end = n+end;

        int index;
        if (end < n) {
            index = string.substring(start, end).indexOf(sub);
        } else {
            index = string.indexOf(sub, start);
        }
        if (index == -1)
            throw Py.ValueError("substring not found in string.index");
        return index;
    }

    public int rindex(String sub) {
        return str_rindex(sub);
    }

    final int str_rindex(String sub) {
        return str_rindex(sub, 0, string.length());
    }

    public int rindex(String sub, int start) {
        return str_rindex(sub, start);
    }

    final int str_rindex(String sub, int start) {
        return str_rindex(sub, start, string.length());
    }

    public int rindex(String sub, int start, int end) {
        return str_rindex(sub, start, end);
    }

    final int str_rindex(String sub, int start, int end) {
        int n = string.length();

        if (start < 0)
            start = n+start;
        if (end < 0)
            end = n+end;

        int index;
        if (start > 0) {
            index = string.substring(start, end).lastIndexOf(sub);
        } else {
            index = string.lastIndexOf(sub, end);
        }
        if (index == -1)
            throw Py.ValueError("substring not found in string.rindex");
        return index;
    }

    public int count(String sub) {
        return str_count(sub);
    }

    final int str_count(String sub) {
        return count(sub, 0, string.length());
    }

    public int count(String sub, int start) {
        return str_count(sub, start);
    }

    final int str_count(String sub, int start) {
        return count(sub, start, string.length());
    }

    public int count(String sub, int start, int end) {
        return str_count(sub, start, end);
    }

    final int str_count(String sub, int start, int end) {
        int len = string.length();
        if (end > len)
            end = len;
        if (end < 0)
            end += len;
        if (end < 0)
            end = 0;
        if (start < 0)
            start += len;
        if (start < 0)
            start = 0;

        int n = sub.length();
        end = end + 1 - n;
        if (n == 0)
            return end-start;

        int count=0;
        while (start < end) {
            int index = string.indexOf(sub, start);
            if (index >= end || index == -1)
                break;
            count++;
            start = index + n;
        }
        return count;
    }

    public int find(String sub) {
        return str_find(sub);
    }

    final int str_find(String sub) {
        return str_find(sub, 0, string.length());
    }

    public int find(String sub, int start) {
        return str_find(sub, start);
    }

    final int str_find(String sub, int start) {
        return str_find(sub, start, string.length());
    }

    public int find(String sub, int start, int end) {
        return str_find(sub, start, end);
    }

    final int str_find(String sub, int start, int end) {
        int n = string.length();
        if (start < 0)
            start = n+start;
        if (end < 0)
            end = n+end;
        if (end > n)
            end = n;
        if (start > end)
            start = end;
        int slen = sub.length();
        end = end-slen;

        int index = string.indexOf(sub, start);
        if (index > end)
            return -1;
        return index;
    }

    public int rfind(String sub) {
        return str_rfind(sub);
    }

    final int str_rfind(String sub) {
        return str_rfind(sub, 0, string.length());
    }

    public int rfind(String sub, int start) {
        return str_rfind(sub, start);
    }

    final int str_rfind(String sub, int start) {
        return str_rfind(sub, start, string.length());
    }

    public int rfind(String sub, int start, int end) {
        return str_rfind(sub, start, end);
    }

    final int str_rfind(String sub, int start, int end) {
        int n = string.length();
        if (start < 0)
            start = n+start;
        if (end < 0)
            end = n+end;
        if (end > n)
            end = n;
        if (start > end)
            start = end;
        int slen = sub.length();
        end = end-slen;

        int index = string.lastIndexOf(sub, end);
        if (index < start)
            return -1;
        return index;
    }

    public double atof() {
        StringBuffer s = null;
        int n = string.length();
        for (int i = 0; i < n; i++) {
            char ch = string.charAt(i);
            if (ch == '\u0000') {
                throw Py.ValueError("null byte in argument for float()");
            }
            if (Character.isDigit(ch)) {
                if (s == null)
                    s = new StringBuffer(string);
                int val = Character.digit(ch, 10);
                s.setCharAt(i, Character.forDigit(val, 10));
            }
        }
        String sval = string;
        if (s != null)
            sval = s.toString();
        try {
            // Double.valueOf allows format specifier ("d" or "f") at the end
            String lowSval = sval.toLowerCase();
            if (lowSval.endsWith("d") || lowSval.endsWith("f")) {
                throw new NumberFormatException("format specifiers not allowed");
            }
            return Double.valueOf(sval).doubleValue();
        }
        catch (NumberFormatException exc) {
            throw Py.ValueError("invalid literal for __float__: "+string);
        }
    }

    public int atoi() {
        return atoi(10);
    }

    public int atoi(int base) {
        if ((base != 0 && base < 2) || (base > 36)) {
            throw Py.ValueError("invalid base for atoi()");
        }

        int b = 0;
        int e = string.length();

        while (b < e && Character.isWhitespace(string.charAt(b)))
            b++;

        while (e > b && Character.isWhitespace(string.charAt(e-1)))
            e--;

        char sign = 0;
        if (b < e) {
            sign = string.charAt(b);
            if (sign == '-' || sign == '+') {
                b++;
                while (b < e && Character.isWhitespace(string.charAt(b))) b++;
            }

            if (base == 0 || base == 16) {
                if (string.charAt(b) == '0') {
                    if (b < e-1 &&
                           Character.toUpperCase(string.charAt(b+1)) == 'X') {
                        base = 16;
                        b += 2;
                    } else {
                        if (base == 0)
                            base = 8;
                    }
                }
            }
        }

        if (base == 0)
            base = 10;

        String s = string;
        if (b > 0 || e < string.length())
            s = string.substring(b, e);

        try {
            long result = Long.parseLong(s, base);
            if (result < 0 && !(sign == '-' && result == -result))
                throw Py.ValueError("invalid literal for __int__: "+string);
            if (sign == '-')
                result = - result;
            if (result < Integer.MIN_VALUE || result > Integer.MAX_VALUE)
                throw Py.ValueError("invalid literal for __int__: "+string);
            return (int) result;
        } catch (NumberFormatException exc) {
            throw Py.ValueError("invalid literal for __int__: "+string);
        } catch (StringIndexOutOfBoundsException exc) {
            throw Py.ValueError("invalid literal for __int__: "+string);
        }
    }

    public PyLong atol() {
        return atol(10);
    }

    public PyLong atol(int base) {
        String str = string;
        int b = 0;
        int e = str.length();

        while (b < e && Character.isWhitespace(str.charAt(b)))
            b++;

        while (e > b && Character.isWhitespace(str.charAt(e-1)))
            e--;
        if (e > b && (str.charAt(e-1) == 'L' || str.charAt(e-1) == 'l'))
            e--;

        char sign = 0;
        if (b < e) {
            sign = string.charAt(b);
            if (sign == '-' || sign == '+') {
                b++;
                while (b < e && Character.isWhitespace(str.charAt(b))) b++;
            }


            if (base == 0 || base == 16) {
                if (string.charAt(b) == '0') {
                    if (b < e-1 &&
                           Character.toUpperCase(string.charAt(b+1)) == 'X') {
                        base = 16;
                        b += 2;
                    } else {
                        if (base == 0)
                            base = 8;
                    }
                }
            }
        }
        if (base == 0)
            base = 10;

        if (base < 2 || base > 36)
            throw Py.ValueError("invalid base for long literal:" + base);

        if (b > 0 || e < str.length())
            str = str.substring(b, e);

        try {
            java.math.BigInteger bi = null;
            if (sign == '-')
                bi = new java.math.BigInteger("-" + str, base);
            else
                bi = new java.math.BigInteger(str, base);
            return new PyLong(bi);
        } catch (NumberFormatException exc) {
            throw Py.ValueError("invalid literal for __long__: "+str);
        } catch (StringIndexOutOfBoundsException exc) {
            throw Py.ValueError("invalid literal for __long__: "+str);
        }
    }


    private static String spaces(int n) {
        char[] chars = new char[n];
        for (int i=0; i<n; i++)
            chars[i] = ' ';
        return new String(chars);
    }

    public String ljust(int width) {
        return str_ljust(width);
    }

    final String str_ljust(int width) {
        int n = width-string.length();
        if (n <= 0)
            return string;
        return string+spaces(n);
    }

    public String rjust(int width) {
        return str_rjust(width);
    }

    final String str_rjust(int width) {
        int n = width-string.length();
        if (n <= 0)
            return string;
        return spaces(n)+string;
    }

    public String center(int width) {
        return str_center(width);
    }

    final String str_center(int width) {
        int n = width-string.length();
        if (n <= 0)
            return string;
        int half = n/2;
        if (n%2 > 0 &&  width%2 > 0)
            half += 1;
        return spaces(half)+string+spaces(n-half);
    }

    public String zfill(int width) {
        return str_zfill(width);
    }

    final String str_zfill(int width) {
        String s = string;
        int n = s.length();
        if (n >= width)
            return s;
        char[] chars = new char[width];
        int nzeros = width-n;
        int i=0;
        int sStart=0;
        if (n > 0) {
            char start = s.charAt(0);
            if (start == '+' || start == '-') {
                chars[0] = start;
                i += 1;
                nzeros++;
                sStart=1;
            }
        }
        for(;i<nzeros; i++) {
            chars[i] = '0';
        }
        s.getChars(sStart, s.length(), chars, i);
        return new String(chars);
    }

    public String expandtabs() {
        return str_expandtabs();
    }

    final String str_expandtabs() {
        return str_expandtabs(8);
    }

    public String expandtabs(int tabsize) {
        return str_expandtabs(tabsize);
    }

    final String str_expandtabs(int tabsize) {
        String s = string;
        StringBuffer buf = new StringBuffer((int)(s.length()*1.5));
        char[] chars = s.toCharArray();
        int n = chars.length;
        int position = 0;

        for(int i=0; i<n; i++) {
            char c = chars[i];
            if (c == '\t') {
                int spaces = tabsize-position%tabsize;
                position += spaces;
                while (spaces-- > 0) {
                    buf.append(' ');
                }
                continue;
            }
            if (c == '\n' || c == '\r') {
                position = -1;
            }
            buf.append(c);
            position++;
        }
        return buf.toString();
    }

    public String capitalize() {
        return str_capitalize();
    }

    final String str_capitalize() {
        if (string.length() == 0)
            return string;
        String first = string.substring(0,1).toUpperCase();
        return first.concat(string.substring(1).toLowerCase());
    }

    final PyString str_replace(PyObject oldPiece, PyObject newPiece) {
        return str_replace(oldPiece, newPiece, string.length());
    }
    
    final PyString str_replace(PyObject oldPiece, PyObject newPiece, int maxsplit) {
        if(!(oldPiece instanceof PyString) || !(newPiece instanceof PyString)){
            throw Py.TypeError("str or unicode required for replace");
        }
        return ((PyString)newPiece).str_join(str_split(((PyString)oldPiece).string, maxsplit));
    }

    public String join(PyObject seq) {
        return str_join(seq).string;
    }

    final PyString str_join(PyObject seq) {
        StringBuffer buf = new StringBuffer();

        PyObject iter = seq.__iter__();
        PyObject obj = null;
        boolean needsUnicode = false;
        for (int i = 0; (obj = iter.__iternext__()) != null; i++) {
            if (!(obj instanceof PyString)){
                 throw Py.TypeError(
                        "sequence item " + i + ": expected string, " +
                        obj.safeRepr() + " found");
            }
            if(obj instanceof PyUnicode){
                needsUnicode = true;
            }
            if (i > 0){
                buf.append(string);
            }
            buf.append(((PyString)obj).string);
        }
        if(needsUnicode || this instanceof PyUnicode){
            return new PyUnicode(buf.toString());
        }
        return new PyString(buf.toString());
    }


    public boolean startswith(String prefix) {
        return str_startswith(prefix);
    }

    final boolean str_startswith(String prefix) {
        return string.startsWith(prefix);
    }

    public boolean startswith(String prefix, int offset) {
        return str_startswith(prefix, offset);
    }

    final boolean str_startswith(String prefix, int offset) {
        return string.startsWith(prefix, offset);
    }

    public boolean startswith(String prefix, int start, int end) {
        return str_startswith(prefix, start, end);
    }

    final boolean str_startswith(String prefix, int start, int end) {
        if (start < 0 || start + prefix.length() > string.length())
            return false;
        if (end > string.length())
            end = string.length();
        String substr = string.substring(start, end);
        return substr.startsWith(prefix);
    }

    public boolean endswith(String suffix) {
        return str_endswith(suffix);
    }

    final boolean str_endswith(String suffix) {
        return string.endsWith(suffix);
    }

    public boolean endswith(String suffix, int start) {
        return str_endswith(suffix, start);
    }

    final boolean str_endswith(String suffix, int start) {
        return str_endswith(suffix, start, string.length());
    }

    public boolean endswith(String suffix, int start, int end) {
        return str_endswith(suffix, start, end);
    }

    final boolean str_endswith(String suffix, int start, int end) {
        int len = string.length();

        if (start < 0 || start > len || suffix.length() > len)
            return false;

        end = (end <= len ? end : len);
        if (end < start)
            return false;

        String substr = string.substring(start, end);
        return substr.endsWith(suffix);
    }

    public String translate(String table) {
        return str_translate(table);
    }

    final String str_translate(String table) {
        return str_translate(table, null);
    }

    public String translate(String table, String deletechars) {
        return str_translate(table, deletechars);
    }

    final String str_translate(String table, String deletechars) {
        if (table.length() != 256)
            throw Py.ValueError(
                "translation table must be 256 characters long");

        StringBuffer buf = new StringBuffer(string.length());
        for (int i=0; i < string.length(); i++) {
            char c = string.charAt(i);
            if (deletechars != null && deletechars.indexOf(c) >= 0)
                continue;
            try {
                buf.append(table.charAt(c));
            }
            catch (IndexOutOfBoundsException e) {
                throw Py.TypeError(
                    "translate() only works for 8-bit character strings");
            }
        }
        return buf.toString();
    }

    //XXX: is this needed?
    public String translate(PyObject table) {
        StringBuffer v = new StringBuffer(string.length());
        for (int i=0; i < string.length(); i++) {
            char ch = string.charAt(i);

            PyObject w = Py.newInteger(ch);
            PyObject x = table.__finditem__(w);
            if (x == null) {
                /* No mapping found: default to 1-1 mapping */
                v.append(ch);
                continue;
            }

            /* Apply mapping */
            if (x instanceof PyInteger) {
                int value = ((PyInteger) x).getValue();
                v.append((char) value);
            } else if (x == Py.None) {
                ;
            } else if (x instanceof PyString) {
                if (x.__len__() != 1) {
                    /* 1-n mapping */
                    throw new PyException(Py.NotImplementedError,
                          "1-n mappings are currently not implemented");
                }
                v.append(x.toString());
            }
            else {
                /* wrong return value */
                throw Py.TypeError(
                     "character mapping must return integer, " +
                     "None or unicode");
            }
        }
        return v.toString();
    }

    public boolean islower() {
        return str_islower();
    }

    final boolean str_islower() {
        int n = string.length();

        /* Shortcut for single character strings */
        if (n == 1)
            return Character.isLowerCase(string.charAt(0));

        boolean cased = false;
        for (int i = 0; i < n; i++) {
            char ch = string.charAt(i);

            if (Character.isUpperCase(ch) || Character.isTitleCase(ch))
                return false;
            else if (!cased && Character.isLowerCase(ch))
                cased = true;
        }
        return cased;
    }

    public boolean isupper() {
        return str_isupper();
    }

    final boolean str_isupper() {
        int n = string.length();

        /* Shortcut for single character strings */
        if (n == 1)
            return Character.isUpperCase(string.charAt(0));

        boolean cased = false;
        for (int i = 0; i < n; i++) {
            char ch = string.charAt(i);

            if (Character.isLowerCase(ch) || Character.isTitleCase(ch))
                return false;
            else if (!cased && Character.isUpperCase(ch))
                cased = true;
        }
        return cased;
    }

    public boolean isalpha() {
        return str_isalpha();
    }

    final boolean str_isalpha() {
        int n = string.length();

        /* Shortcut for single character strings */
        if (n == 1)
            return Character.isLetter(string.charAt(0));

        if (n == 0)
            return false;

        for (int i = 0; i < n; i++) {
            char ch = string.charAt(i);

            if (!Character.isLetter(ch))
                return false;
        }
        return true;
    }

    public boolean isalnum() {
        return str_isalnum();
    }

    final boolean str_isalnum() {
        int n = string.length();

        /* Shortcut for single character strings */
        if (n == 1)
            return _isalnum(string.charAt(0));

        if (n == 0)
            return false;

        for (int i = 0; i < n; i++) {
            char ch = string.charAt(i);

            if (!_isalnum(ch))
                return false;
        }
        return true;
    }

    private boolean _isalnum(char ch) {
        // This can ever be entirely compatible with CPython. In CPython
        // The type is not used, the numeric property is determined from
        // the presense of digit, decimal or numeric fields. These fields
        // are not available in exactly the same way in java.
        return Character.isLetterOrDigit(ch) ||
               Character.getType(ch) == Character.LETTER_NUMBER;
    }

    public boolean isdecimal() {
        return str_isdecimal();
    }

    final boolean str_isdecimal() {
        int n = string.length();

        /* Shortcut for single character strings */
        if (n == 1) {
            char ch = string.charAt(0);
            return _isdecimal(ch);
        }

        if (n == 0)
            return false;

        for (int i = 0; i < n; i++) {
            char ch = string.charAt(i);

            if (!_isdecimal(ch))
                return false;
        }
        return true;
    }

    private boolean _isdecimal(char ch) {
        // See the comment in _isalnum. Here it is even worse.
        return Character.getType(ch) == Character.DECIMAL_DIGIT_NUMBER;
    }

    public boolean isdigit() {
        return str_isdigit();
    }

    final boolean str_isdigit() {
        int n = string.length();

        /* Shortcut for single character strings */
        if (n == 1)
            return Character.isDigit(string.charAt(0));

        if (n == 0)
            return false;

        for (int i = 0; i < n; i++) {
            char ch = string.charAt(i);

            if (!Character.isDigit(ch))
                return false;
        }
        return true;
    }

    public boolean isnumeric() {
        return str_isnumeric();
    }

    final boolean str_isnumeric() {
        int n = string.length();

        /* Shortcut for single character strings */
        if (n == 1)
            return _isnumeric(string.charAt(0));

        if (n == 0)
            return false;

        for (int i = 0; i < n; i++) {
            char ch = string.charAt(i);
            if (!_isnumeric(ch))
                return false;
        }
        return true;
    }

    private boolean _isnumeric(char ch) {
        int type = Character.getType(ch);
        return type == Character.DECIMAL_DIGIT_NUMBER ||
               type == Character.LETTER_NUMBER ||
               type == Character.OTHER_NUMBER;
    }

    public boolean istitle() {
        return str_istitle();
    }

    final boolean str_istitle() {
        int n = string.length();

        /* Shortcut for single character strings */
        if (n == 1)
            return Character.isTitleCase(string.charAt(0)) ||
                   Character.isUpperCase(string.charAt(0));

        boolean cased = false;
        boolean previous_is_cased = false;
        for (int i = 0; i < n; i++) {
            char ch = string.charAt(i);

            if (Character.isUpperCase(ch) || Character.isTitleCase(ch)) {
                if (previous_is_cased)
                    return false;
                previous_is_cased = true;
                cased = true;
            }
            else if (Character.isLowerCase(ch)) {
                if (!previous_is_cased)
                    return false;
                previous_is_cased = true;
                cased = true;
            }
            else
                previous_is_cased = false;
        }
        return cased;
    }

    public boolean isspace() {
        return str_isspace();
    }

    final boolean str_isspace() {
        int n = string.length();

        /* Shortcut for single character strings */
        if (n == 1)
            return Character.isWhitespace(string.charAt(0));

        if (n == 0)
            return false;

        for (int i = 0; i < n; i++) {
            char ch = string.charAt(i);

            if (!Character.isWhitespace(ch))
                return false;
        }
        return true;
    }

    public boolean isunicode() {
        return str_isunicode();
    }

    final boolean str_isunicode() {
        int n = string.length();
        for (int i = 0; i < n; i++) {
            char ch = string.charAt(i);
            if (ch > 255)
                return true;
        }
        return false;
    }

    public String encode() {
        return str_encode();
    }

    final String str_encode() {
        return str_encode(null, null);
    }

    public String encode(String encoding) {
        return str_encode(encoding);
    }

    final String str_encode(String encoding) {
        return str_encode(encoding, null);
    }

    public String encode(String encoding, String errors) {
        return str_encode(encoding, errors);
    }

    final String str_encode(String encoding, String errors) {
        return codecs.encode(this, encoding, errors);
    }

    public String decode() {
        return str_decode();
    }

    final String str_decode() {
        return str_decode(null, null); // xxx
    }

    public String decode(String encoding) {
        return str_decode(encoding);
    }

    final String str_decode(String encoding) {
        return str_decode(encoding, null);
    }

    public String decode(String encoding, String errors) {
        return str_decode(encoding, errors);
    }

    final String str_decode(String encoding, String errors) {
        return codecs.decode(this, encoding, errors);
    }

    /* arguments' conversion helper */

    public String asString(int index) throws PyObject.ConversionException {
        return string;
    }

    public String asName(int index) throws PyObject.ConversionException {
        return internedString();
    }

    protected String unsupportedopMessage(String op, PyObject o2) {
        if (op.equals("+")) {
            return "cannot concatenate ''{1}'' and ''{2}'' objects";
        }
        return super.unsupportedopMessage(op, o2);
    }
}

final class StringFormatter
{
    int index;
    String format;
    StringBuffer buffer;
    boolean negative;
    int precision;
    int argIndex;
    PyObject args;
    boolean unicodeCoercion;

    final char pop() {
        try {
            return format.charAt(index++);
        } catch (StringIndexOutOfBoundsException e) {
            throw Py.ValueError("incomplete format");
        }
    }

    final char peek() {
        return format.charAt(index);
    }

    final void push() {
        index--;
    }

    public StringFormatter(String format) {
        this(format, false);
    }

    public StringFormatter(String format, boolean unicodeCoercion) {
        index = 0;
        this.format = format;
        this.unicodeCoercion = unicodeCoercion;
        buffer = new StringBuffer(format.length()+100);
    }

    PyObject getarg() {
        PyObject ret = null;
        switch(argIndex) {
            // special index indicating a mapping
        case -3:
            return args;
            // special index indicating a single item that has already been
            // used
        case -2:
            break;
            // special index indicating a single item that has not yet been
            // used
        case -1:
            argIndex=-2;
            return args;
        default:
            ret = args.__finditem__(argIndex++);
            break;
        }
        if (ret == null)
            throw Py.TypeError("not enough arguments for format string");
        return ret;
    }

    int getNumber() {
        char c = pop();
        if (c == '*') {
            PyObject o = getarg();
            if (o instanceof PyInteger)
                return ((PyInteger)o).getValue();
            throw Py.TypeError("* wants int");
        } else {
            if (Character.isDigit(c)) {
                int numStart = index-1;
                while (Character.isDigit(c = pop()))
                    ;
                index -= 1;
                Integer i = Integer.valueOf(
                                    format.substring(numStart, index));
                return i.intValue();
            }
            index -= 1;
            return 0;
        }
    }

    private void checkPrecision(String type) {
        if(precision > 250) {
            // A magic number. Larger than in CPython.
            throw Py.OverflowError("formatted " + type + " is too long (precision too long?)");
        }
        
    }

    private String formatLong(PyString arg, char type, boolean altFlag) {
        checkPrecision("long");
        String s = arg.toString();
        int end = s.length();
        int ptr = 0;

        int numnondigits = 0;
        if (type == 'x' || type == 'X')
            numnondigits = 2;

        if (s.endsWith("L"))
            end--;

        negative = s.charAt(0) == '-';
        if (negative) {
            ptr++;
        }

        int numdigits = end - numnondigits - ptr;
        if (!altFlag) {
            switch (type) {
            case 'o' :
                if (numdigits > 1) {
                     ++ptr;
                     --numdigits;
                }
                break;
            case 'x' :
            case 'X' :
                ptr += 2;
                numnondigits -= 2;
                break;
            }
        }
        if (precision > numdigits) {
            StringBuffer buf = new StringBuffer();
            for (int i = 0; i < numnondigits; ++i)
                buf.append(s.charAt(ptr++));
            for (int i = 0; i < precision - numdigits; i++)
                buf.append('0');
            for (int i = 0; i < numdigits; i++)
                buf.append(s.charAt(ptr++));
            s = buf.toString();
        } else if (end < s.length() || ptr > 0)
            s = s.substring(ptr, end);

        switch (type) {
        case 'x' :
            s = s.toLowerCase();
            break;
        }
        return s;
    }

    private String formatInteger(PyObject arg, int radix, boolean unsigned) {
        return formatInteger(((PyInteger)arg.__int__()).getValue(), radix, unsigned);
    }

    private String formatInteger(long v, int radix, boolean unsigned) {
        checkPrecision("integer");
        if (unsigned) {
            if (v < 0)
                v = 0x100000000l + v;
        } else {
            if (v < 0) {
                negative = true;
                v = -v;
            }
        }
        String s = Long.toString(v, radix);
        while (s.length() < precision) {
            s = "0"+s;
        }
        return s;
    }

    private String formatFloatDecimal(PyObject arg, boolean truncate) {
        return formatFloatDecimal(arg.__float__().getValue(), truncate);
    }

    private String formatFloatDecimal(double v, boolean truncate) {
        checkPrecision("decimal");
        java.text.NumberFormat format = java.text.NumberFormat.getInstance(
                                           java.util.Locale.US);
        int prec = precision;
        if (prec == -1)
            prec = 6;
        if (v < 0) {
            v = -v;
            negative = true;
        }
        format.setMaximumFractionDigits(prec);
        format.setMinimumFractionDigits(truncate ? 0 : prec);
        format.setGroupingUsed(false);

        String ret = format.format(v);
//         System.err.println("formatFloat: "+v+", prec="+prec+", ret="+ret);
//         if (ret.indexOf('.') == -1) {
//             return ret+'.';
//         }
        return ret;
    }

    private String formatFloatExponential(PyObject arg, char e,
                                         boolean truncate)
    {
        StringBuffer buf = new StringBuffer();
        double v = arg.__float__().getValue();
        boolean isNegative = false;
        if (v < 0) {
            v = -v;
            isNegative = true;
        }
        double power = 0.0;
        if (v > 0)
            power = ExtraMath.closeFloor(ExtraMath.log10(v));
        //System.err.println("formatExp: "+v+", "+power);
        int savePrecision = precision;

        if (truncate)
            precision = -1;
        else
            precision = 3;

        String exp = formatInteger((long)power, 10, false);
        if (negative) {
            negative = false;
            exp = '-'+exp;
        }
        else {
            if (!truncate)
                exp = '+'+exp;
        }

        precision = savePrecision;

        double base = v/Math.pow(10, power);
        buf.append(formatFloatDecimal(base, truncate));
        buf.append(e);

        buf.append(exp);
        negative = isNegative;

        return buf.toString();
    }

    public PyString format(PyObject args) {
        PyObject dict = null;
        this.args = args;
        boolean needUnicode = false;
        if (args instanceof PyTuple) {
            argIndex = 0;
        } else {
            // special index indicating a single item rather than a tuple
            argIndex = -1;
            if (args instanceof PyDictionary ||
                args instanceof PyStringMap ||
                (!(args instanceof PySequence) &&
                 args.__findattr__("__getitem__") != null))
            {
                dict = args;
                argIndex = -3;
            }
        }

        while (index < format.length()) {
            boolean ljustFlag=false;
            boolean signFlag=false;
            boolean blankFlag=false;
            boolean altFlag=false;
            boolean zeroFlag=false;

            int width = -1;
            precision = -1;

            char c = pop();
            if (c != '%') {
                buffer.append(c);
                continue;
            }
            c = pop();
            if (c == '(') {
                //System.out.println("( found");
                if (dict == null)
                    throw Py.TypeError("format requires a mapping");
                int parens = 1;
                int keyStart = index;
                while (parens > 0) {
                    c = pop();
                    if (c == ')')
                        parens--;
                    else if (c == '(')
                        parens++;
                }
                String tmp = format.substring(keyStart, index-1);
                this.args = dict.__getitem__(new PyString(tmp));
                //System.out.println("args: "+args+", "+argIndex);
            } else {
                push();
            }
            while (true) {
                switch (c = pop()) {
                case '-': ljustFlag=true; continue;
                case '+': signFlag=true; continue;
                case ' ': blankFlag=true; continue;
                case '#': altFlag=true; continue;
                case '0': zeroFlag=true; continue;
                }
                break;
            }
            push();
            width = getNumber();
            if (width < 0) {
                width = -width;
                ljustFlag = true;
            }
            c = pop();
            if (c == '.') {
                precision = getNumber();
                if (precision < -1)
                    precision = 0;

                c = pop();
            }
            if (c == 'h' || c == 'l' || c == 'L') {
                c = pop();
            }
            if (c == '%') {
                buffer.append(c);
                continue;
            }
            PyObject arg = getarg();
            //System.out.println("args: "+args+", "+argIndex+", "+arg);
            char fill = ' ';
            String string=null;
            negative = false;
            if (zeroFlag)
                fill = '0';
            else
                fill = ' ';

            switch(c) {
            case 's':
            case 'r':
                fill = ' ';
                if (c == 's')
                    if (unicodeCoercion)
                        string = arg.__unicode__().toString();
                    else
                        string = arg.__str__().toString();
                else
                    string = arg.__repr__().toString();
                if (precision >= 0 && string.length() > precision) {
                    string = string.substring(0, precision);
                }
                if (arg instanceof PyUnicode) {
                    needUnicode = true;
                }
                break;
            case 'i':
            case 'd':
                if (arg instanceof PyLong)
                    string = formatLong(arg.__str__(), c, altFlag);
                else
                    string = formatInteger(arg, 10, false);
                break;
            case 'u':
                if (arg instanceof PyLong)
                    string = formatLong(arg.__str__(), c, altFlag);
                else
                    string = formatInteger(arg, 10, true);
                break;
            case 'o':
                if (arg instanceof PyLong)
                    string = formatLong(arg.__oct__(), c, altFlag);
                else {
                    string = formatInteger(arg, 8, true);
                    if (altFlag && string.charAt(0) != '0') {
                        string = "0" + string;
                    }
                }
                break;
            case 'x':
                if (arg instanceof PyLong)
                    string = formatLong(arg.__hex__(), c, altFlag);
                else {
                    string = formatInteger(arg, 16, true);
                    string = string.toLowerCase();
                    if (altFlag) {
                        string = "0x" + string;
                    }
                }
                break;
            case 'X':
                if (arg instanceof PyLong)
                    string = formatLong(arg.__hex__(), c, altFlag);
                else {
                    string = formatInteger(arg, 16, true);
                    string = string.toUpperCase();
                    if (altFlag) {
                        string = "0X" + string;
                   }
                }

                break;
            case 'e':
            case 'E':
                string = formatFloatExponential(arg, c, false);
                break;
            case 'f':
                string = formatFloatDecimal(arg, false);
//                 if (altFlag && string.indexOf('.') == -1)
//                     string += '.';
                break;
            case 'g':
            case 'G':
                int prec = precision;
                if (prec == -1)
                    prec = 6;
                double v = arg.__float__().getValue();
                int digits = (int)Math.ceil(ExtraMath.log10(v));
                if (digits > 0) {
                    if (digits <= prec) {
                        precision = prec-digits;
                        string = formatFloatDecimal(arg, true);
                    } else {
                        string = formatFloatExponential(arg, (char)(c-2),
                                                        true);
                    }
                } else {
                    string = formatFloatDecimal(arg, true);
                }
                if (altFlag && string.indexOf('.') == -1) {
                    int zpad = prec - string.length();
                    string += '.';
                    if (zpad > 0) {
                        char zeros[] = new char[zpad];
                        for (int ci=0; ci<zpad; zeros[ci++] = '0')
                            ;
                        string += new String(zeros);
                    }
                }
                break;
            case 'c':
                fill = ' ';
                if (arg instanceof PyString) {
                    string = ((PyString)arg).toString();
                    if (string.length() != 1)
                        throw Py.TypeError("%c requires int or char");
                    if (arg instanceof PyUnicode) {
                        needUnicode = true;
                    }
                    break;
                }
                char tmp = (char)((PyInteger)arg.__int__()).getValue();
                string = new Character(tmp).toString();
                break;

            default:
                throw Py.ValueError("unsupported format character '" +
                         codecs.encode(Py.newString(c), null, "replace") +
                         "' (0x" + Integer.toHexString(c) + ") at index " +
                         (index-1));
            }
            int length = string.length();
            int skip = 0;
            String signString = null;
            if (negative) {
                signString = "-";
            } else {
                if (signFlag) {
                    signString = "+";
                } else if (blankFlag) {
                    signString = " ";
                }
            }

            if (width < length)
                width = length;
            if (signString != null) {
                if (fill != ' ')
                    buffer.append(signString);
                if (width > length)
                    width--;
            }
            if (altFlag && (c == 'x' || c == 'X')) {
                if (fill != ' ') {
                    buffer.append('0');
                    buffer.append(c);
                    skip += 2;
                }
                width -= 2;
                if (width < 0)
                    width = 0;
                length -= 2;
            }
            if (width > length && !ljustFlag) {
                do {
                    buffer.append(fill);
                } while (--width > length);
            }
            if (fill == ' ') {
                if (signString != null)
                    buffer.append(signString);
                if (altFlag && (c == 'x' || c == 'X')) {
                    buffer.append('0');
                    buffer.append(c);
                    skip += 2;
                }
            }
            if (skip > 0)
                buffer.append(string.substring(skip));
            else
                buffer.append(string);

            while (--width >= length) {
                buffer.append(' ');
            }
        }
        if (argIndex == -1 ||
            (argIndex >= 0 && args.__finditem__(argIndex) != null))
        {
            throw Py.TypeError("not all arguments converted");
        }
        if (needUnicode) {
            return new PyUnicode(buffer.toString());
        }
        return new PyString(buffer.toString());
    }

}
