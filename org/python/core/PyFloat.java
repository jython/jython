// Copyright (c) Corporation for National Research Initiatives
package org.python.core;

import java.io.Serializable;

/**
 * A builtin python float.
 */

public class PyFloat extends PyObject
{
    /* type info */

    public static final String exposed_name="float";

    public static void typeSetup(PyObject dict,PyType.Newstyle marker) {
        class exposed___abs__ extends PyBuiltinFunctionNarrow {

            private PyFloat self;

            public PyObject getSelf() {
                return self;
            }

            exposed___abs__(PyFloat self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___abs__((PyFloat)self,info);
            }

            public PyObject __call__() {
                return self.float___abs__();
            }

            public PyObject inst_call(PyObject gself) {
                PyFloat self=(PyFloat)gself;
                return self.float___abs__();
            }

        }
        dict.__setitem__("__abs__",new PyMethodDescr("__abs__",PyFloat.class,0,0,new exposed___abs__(null,null)));
        class exposed___float__ extends PyBuiltinFunctionNarrow {

            private PyFloat self;

            public PyObject getSelf() {
                return self;
            }

            exposed___float__(PyFloat self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___float__((PyFloat)self,info);
            }

            public PyObject __call__() {
                return self.float___float__();
            }

            public PyObject inst_call(PyObject gself) {
                PyFloat self=(PyFloat)gself;
                return self.float___float__();
            }

        }
        dict.__setitem__("__float__",new PyMethodDescr("__float__",PyFloat.class,0,0,new exposed___float__(null,null)));
        class exposed___int__ extends PyBuiltinFunctionNarrow {

            private PyFloat self;

            public PyObject getSelf() {
                return self;
            }

            exposed___int__(PyFloat self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___int__((PyFloat)self,info);
            }

            public PyObject __call__() {
                return self.float___int__();
            }

            public PyObject inst_call(PyObject gself) {
                PyFloat self=(PyFloat)gself;
                return self.float___int__();
            }

        }
        dict.__setitem__("__int__",new PyMethodDescr("__int__",PyFloat.class,0,0,new exposed___int__(null,null)));
        class exposed___long__ extends PyBuiltinFunctionNarrow {

            private PyFloat self;

            public PyObject getSelf() {
                return self;
            }

            exposed___long__(PyFloat self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___long__((PyFloat)self,info);
            }

            public PyObject __call__() {
                return self.float___long__();
            }

            public PyObject inst_call(PyObject gself) {
                PyFloat self=(PyFloat)gself;
                return self.float___long__();
            }

        }
        dict.__setitem__("__long__",new PyMethodDescr("__long__",PyFloat.class,0,0,new exposed___long__(null,null)));
        class exposed___neg__ extends PyBuiltinFunctionNarrow {

            private PyFloat self;

            public PyObject getSelf() {
                return self;
            }

            exposed___neg__(PyFloat self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___neg__((PyFloat)self,info);
            }

            public PyObject __call__() {
                return self.float___neg__();
            }

            public PyObject inst_call(PyObject gself) {
                PyFloat self=(PyFloat)gself;
                return self.float___neg__();
            }

        }
        dict.__setitem__("__neg__",new PyMethodDescr("__neg__",PyFloat.class,0,0,new exposed___neg__(null,null)));
        class exposed___pos__ extends PyBuiltinFunctionNarrow {

            private PyFloat self;

            public PyObject getSelf() {
                return self;
            }

            exposed___pos__(PyFloat self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___pos__((PyFloat)self,info);
            }

            public PyObject __call__() {
                return self.float___pos__();
            }

            public PyObject inst_call(PyObject gself) {
                PyFloat self=(PyFloat)gself;
                return self.float___pos__();
            }

        }
        dict.__setitem__("__pos__",new PyMethodDescr("__pos__",PyFloat.class,0,0,new exposed___pos__(null,null)));
        class exposed___add__ extends PyBuiltinFunctionNarrow {

            private PyFloat self;

            public PyObject getSelf() {
                return self;
            }

            exposed___add__(PyFloat self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___add__((PyFloat)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=self.float___add__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyFloat self=(PyFloat)gself;
                PyObject ret=self.float___add__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__add__",new PyMethodDescr("__add__",PyFloat.class,1,1,new exposed___add__(null,null)));
        class exposed___div__ extends PyBuiltinFunctionNarrow {

            private PyFloat self;

            public PyObject getSelf() {
                return self;
            }

            exposed___div__(PyFloat self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___div__((PyFloat)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=self.float___div__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyFloat self=(PyFloat)gself;
                PyObject ret=self.float___div__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__div__",new PyMethodDescr("__div__",PyFloat.class,1,1,new exposed___div__(null,null)));
        class exposed___divmod__ extends PyBuiltinFunctionNarrow {

            private PyFloat self;

            public PyObject getSelf() {
                return self;
            }

            exposed___divmod__(PyFloat self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___divmod__((PyFloat)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=self.float___divmod__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyFloat self=(PyFloat)gself;
                PyObject ret=self.float___divmod__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__divmod__",new PyMethodDescr("__divmod__",PyFloat.class,1,1,new exposed___divmod__(null,null)));
        class exposed___floordiv__ extends PyBuiltinFunctionNarrow {

            private PyFloat self;

            public PyObject getSelf() {
                return self;
            }

            exposed___floordiv__(PyFloat self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___floordiv__((PyFloat)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=self.float___floordiv__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyFloat self=(PyFloat)gself;
                PyObject ret=self.float___floordiv__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__floordiv__",new PyMethodDescr("__floordiv__",PyFloat.class,1,1,new exposed___floordiv__(null,null)));
        class exposed___mod__ extends PyBuiltinFunctionNarrow {

            private PyFloat self;

            public PyObject getSelf() {
                return self;
            }

            exposed___mod__(PyFloat self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___mod__((PyFloat)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=self.float___mod__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyFloat self=(PyFloat)gself;
                PyObject ret=self.float___mod__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__mod__",new PyMethodDescr("__mod__",PyFloat.class,1,1,new exposed___mod__(null,null)));
        class exposed___mul__ extends PyBuiltinFunctionNarrow {

            private PyFloat self;

            public PyObject getSelf() {
                return self;
            }

            exposed___mul__(PyFloat self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___mul__((PyFloat)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=self.float___mul__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyFloat self=(PyFloat)gself;
                PyObject ret=self.float___mul__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__mul__",new PyMethodDescr("__mul__",PyFloat.class,1,1,new exposed___mul__(null,null)));
        class exposed___radd__ extends PyBuiltinFunctionNarrow {

            private PyFloat self;

            public PyObject getSelf() {
                return self;
            }

            exposed___radd__(PyFloat self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___radd__((PyFloat)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=self.float___radd__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyFloat self=(PyFloat)gself;
                PyObject ret=self.float___radd__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__radd__",new PyMethodDescr("__radd__",PyFloat.class,1,1,new exposed___radd__(null,null)));
        class exposed___rdiv__ extends PyBuiltinFunctionNarrow {

            private PyFloat self;

            public PyObject getSelf() {
                return self;
            }

            exposed___rdiv__(PyFloat self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___rdiv__((PyFloat)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=self.float___rdiv__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyFloat self=(PyFloat)gself;
                PyObject ret=self.float___rdiv__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__rdiv__",new PyMethodDescr("__rdiv__",PyFloat.class,1,1,new exposed___rdiv__(null,null)));
        class exposed___rfloordiv__ extends PyBuiltinFunctionNarrow {

            private PyFloat self;

            public PyObject getSelf() {
                return self;
            }

            exposed___rfloordiv__(PyFloat self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___rfloordiv__((PyFloat)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=self.float___rfloordiv__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyFloat self=(PyFloat)gself;
                PyObject ret=self.float___rfloordiv__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__rfloordiv__",new PyMethodDescr("__rfloordiv__",PyFloat.class,1,1,new exposed___rfloordiv__(null,null)));
        class exposed___rmod__ extends PyBuiltinFunctionNarrow {

            private PyFloat self;

            public PyObject getSelf() {
                return self;
            }

            exposed___rmod__(PyFloat self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___rmod__((PyFloat)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=self.float___rmod__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyFloat self=(PyFloat)gself;
                PyObject ret=self.float___rmod__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__rmod__",new PyMethodDescr("__rmod__",PyFloat.class,1,1,new exposed___rmod__(null,null)));
        class exposed___rmul__ extends PyBuiltinFunctionNarrow {

            private PyFloat self;

            public PyObject getSelf() {
                return self;
            }

            exposed___rmul__(PyFloat self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___rmul__((PyFloat)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=self.float___rmul__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyFloat self=(PyFloat)gself;
                PyObject ret=self.float___rmul__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__rmul__",new PyMethodDescr("__rmul__",PyFloat.class,1,1,new exposed___rmul__(null,null)));
        class exposed___rsub__ extends PyBuiltinFunctionNarrow {

            private PyFloat self;

            public PyObject getSelf() {
                return self;
            }

            exposed___rsub__(PyFloat self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___rsub__((PyFloat)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=self.float___rsub__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyFloat self=(PyFloat)gself;
                PyObject ret=self.float___rsub__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__rsub__",new PyMethodDescr("__rsub__",PyFloat.class,1,1,new exposed___rsub__(null,null)));
        class exposed___rtruediv__ extends PyBuiltinFunctionNarrow {

            private PyFloat self;

            public PyObject getSelf() {
                return self;
            }

            exposed___rtruediv__(PyFloat self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___rtruediv__((PyFloat)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=self.float___rtruediv__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyFloat self=(PyFloat)gself;
                PyObject ret=self.float___rtruediv__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__rtruediv__",new PyMethodDescr("__rtruediv__",PyFloat.class,1,1,new exposed___rtruediv__(null,null)));
        class exposed___sub__ extends PyBuiltinFunctionNarrow {

            private PyFloat self;

            public PyObject getSelf() {
                return self;
            }

            exposed___sub__(PyFloat self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___sub__((PyFloat)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=self.float___sub__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyFloat self=(PyFloat)gself;
                PyObject ret=self.float___sub__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__sub__",new PyMethodDescr("__sub__",PyFloat.class,1,1,new exposed___sub__(null,null)));
        class exposed___truediv__ extends PyBuiltinFunctionNarrow {

            private PyFloat self;

            public PyObject getSelf() {
                return self;
            }

            exposed___truediv__(PyFloat self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___truediv__((PyFloat)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=self.float___truediv__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyFloat self=(PyFloat)gself;
                PyObject ret=self.float___truediv__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__truediv__",new PyMethodDescr("__truediv__",PyFloat.class,1,1,new exposed___truediv__(null,null)));
        class exposed___cmp__ extends PyBuiltinFunctionNarrow {

            private PyFloat self;

            public PyObject getSelf() {
                return self;
            }

            exposed___cmp__(PyFloat self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___cmp__((PyFloat)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                int ret=self.float___cmp__(arg0);
                if (ret==-2) {
                    throw Py.TypeError("float"+".__cmp__(x,y) requires y to be '"+"float"+"', not a '"+(arg0).getType().fastGetName()+"'");
                }
                return Py.newInteger(ret);
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyFloat self=(PyFloat)gself;
                int ret=self.float___cmp__(arg0);
                if (ret==-2) {
                    throw Py.TypeError("float"+".__cmp__(x,y) requires y to be '"+"float"+"', not a '"+(arg0).getType().fastGetName()+"'");
                }
                return Py.newInteger(ret);
            }

        }
        dict.__setitem__("__cmp__",new PyMethodDescr("__cmp__",PyFloat.class,1,1,new exposed___cmp__(null,null)));
        class exposed___pow__ extends PyBuiltinFunctionNarrow {

            private PyFloat self;

            public PyObject getSelf() {
                return self;
            }

            exposed___pow__(PyFloat self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___pow__((PyFloat)self,info);
            }

            public PyObject __call__(PyObject arg0,PyObject arg1) {
                PyObject ret=self.float___pow__(arg0,arg1);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

            public PyObject inst_call(PyObject gself,PyObject arg0,PyObject arg1) {
                PyFloat self=(PyFloat)gself;
                PyObject ret=self.float___pow__(arg0,arg1);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=self.float___pow__(arg0,null);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyFloat self=(PyFloat)gself;
                PyObject ret=self.float___pow__(arg0,null);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__pow__",new PyMethodDescr("__pow__",PyFloat.class,1,2,new exposed___pow__(null,null)));
        class exposed___nonzero__ extends PyBuiltinFunctionNarrow {

            private PyFloat self;

            public PyObject getSelf() {
                return self;
            }

            exposed___nonzero__(PyFloat self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___nonzero__((PyFloat)self,info);
            }

            public PyObject __call__() {
                return Py.newBoolean(self.float___nonzero__());
            }

            public PyObject inst_call(PyObject gself) {
                PyFloat self=(PyFloat)gself;
                return Py.newBoolean(self.float___nonzero__());
            }

        }
        dict.__setitem__("__nonzero__",new PyMethodDescr("__nonzero__",PyFloat.class,0,0,new exposed___nonzero__(null,null)));
        class exposed___repr__ extends PyBuiltinFunctionNarrow {

            private PyFloat self;

            public PyObject getSelf() {
                return self;
            }

            exposed___repr__(PyFloat self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___repr__((PyFloat)self,info);
            }

            public PyObject __call__() {
                return new PyString(self.float_toString());
            }

            public PyObject inst_call(PyObject gself) {
                PyFloat self=(PyFloat)gself;
                return new PyString(self.float_toString());
            }

        }
        dict.__setitem__("__repr__",new PyMethodDescr("__repr__",PyFloat.class,0,0,new exposed___repr__(null,null)));
        class exposed___str__ extends PyBuiltinFunctionNarrow {

            private PyFloat self;

            public PyObject getSelf() {
                return self;
            }

            exposed___str__(PyFloat self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___str__((PyFloat)self,info);
            }

            public PyObject __call__() {
                return new PyString(self.float_toString());
            }

            public PyObject inst_call(PyObject gself) {
                PyFloat self=(PyFloat)gself;
                return new PyString(self.float_toString());
            }

        }
        dict.__setitem__("__str__",new PyMethodDescr("__str__",PyFloat.class,0,0,new exposed___str__(null,null)));
        class exposed___hash__ extends PyBuiltinFunctionNarrow {

            private PyFloat self;

            public PyObject getSelf() {
                return self;
            }

            exposed___hash__(PyFloat self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___hash__((PyFloat)self,info);
            }

            public PyObject __call__() {
                return Py.newFloat(self.float_hashCode());
            }

            public PyObject inst_call(PyObject gself) {
                PyFloat self=(PyFloat)gself;
                return Py.newFloat(self.float_hashCode());
            }

        }
        dict.__setitem__("__hash__",new PyMethodDescr("__hash__",PyFloat.class,0,0,new exposed___hash__(null,null)));
        dict.__setitem__("__new__",new PyNewWrapper(PyFloat.class,"__new__",-1,-1) {
               public PyObject new_impl(boolean init,PyType subtype,PyObject[]args,String[]keywords) {
                   return float_new(this,init,subtype,args,keywords);
               }
        });
    }

    public static PyObject float_new(PyObject new_, boolean init, PyType subtype,
            PyObject[] args, String[] keywords) {
        ArgParser ap = new ArgParser("float", args, keywords, new String[] { "x" }, 0);
        PyObject x = ap.getPyObject(0, null);
        if (x == null)
            return new PyFloat(0.0);
        return x.__float__();
    } // xxx

    private static final PyType FLOATTYPE = PyType.fromClass(PyFloat.class);

    private double value;

    public PyFloat(double v) {
	    super(FLOATTYPE);
        value = v;
    }

    public PyFloat(float v) {
        this((double)v);
    }

    public String safeRepr() throws PyIgnoreMethodTag {
        return "'float' object";
    }

    public double getValue() {
        return value;
    }

    public String toString() {
        return float_toString();
    }

    final String float_toString() {
        String s = Double.toString(value);
        // this is to work around an apparent bug in Double.toString(0.001)
        // which returns "0.0010"
        if (s.indexOf('E') == -1) {
            while (true) {
                int n = s.length();
                if (n <= 2)
                    break;
                if (s.charAt(n-1) == '0' && s.charAt(n-2) != '.') {
                    s = s.substring(0,n-1);
                    continue;
                }
                break;
            }
        }
        return s;
    }

    public int hashCode() {
        return float_hashCode();
    }

    final int float_hashCode() {
        double intPart = Math.floor(value);
        double fractPart = value-intPart;

        if (fractPart == 0) {
            if (intPart <= Integer.MAX_VALUE && intPart >= Integer.MIN_VALUE)
                return (int)value;
            else
                return __long__().hashCode();
        } else {
            long v = Double.doubleToLongBits(value);
            return (int)v ^ (int)(v >> 32);
        }
    }

    public boolean __nonzero__() {
        return float___nonzero__();
    }

    final boolean float___nonzero__() {
        return value != 0;
    }

    public Object __tojava__(Class c) {
        if (c == Double.TYPE || c == Number.class ||
            c == Double.class || c == Object.class || c == Serializable.class)
        {
            return new Double(value);
        }
        if (c == Float.TYPE || c == Float.class) {
            return new Float(value);
        }
        return super.__tojava__(c);
    }

    public int __cmp__(PyObject other) {
        return float___cmp__(other);
    }

    final int float___cmp__(PyObject other) {
        if (!canCoerce(other))
             return -2;
        double v = coerce(other);
        return value < v ? -1 : value > v ? 1 : 0;
    }

    public Object __coerce_ex__(PyObject other) {
        if (other instanceof PyFloat)
            return other;
        else {
            if (other instanceof PyInteger)
                return new PyFloat((double)((PyInteger)other).getValue());
            if (other instanceof PyLong)
                return new PyFloat(((PyLong)other).doubleValue());
            else
                return Py.None;
        }
    }

    private static final boolean canCoerce(PyObject other) {
        return other instanceof PyFloat || other instanceof PyInteger ||
            other instanceof PyLong;
    }

    private static final double coerce(PyObject other) {
        if (other instanceof PyFloat)
            return ((PyFloat) other).value;
        else if (other instanceof PyInteger)
            return ((PyInteger) other).getValue();
        else if (other instanceof PyLong)
            return ((PyLong) other).doubleValue();
        else
            throw Py.TypeError("xxx");
    }


    public PyObject __add__(PyObject right) {
        return float___add__(right);
    }

    final PyObject float___add__(PyObject right) {
        if (!canCoerce(right))
            return null;
        double rightv = coerce(right);
        return new PyFloat(value + rightv);
    }

    public PyObject __radd__(PyObject left) {
        return float___radd__(left);
    }

    final PyObject float___radd__(PyObject left) {
        return __add__(left);
    }

    public PyObject __sub__(PyObject right) {
        return float___sub__(right);
    }

    final PyObject float___sub__(PyObject right) {
        if (!canCoerce(right))
            return null;
        double rightv = coerce(right);
        return new PyFloat(value - rightv);
    }

    public PyObject __rsub__(PyObject left) {
        return float___rsub__(left);
    }

    final PyObject float___rsub__(PyObject left) {
        if (!canCoerce(left))
            return null;
        double leftv = coerce(left);
        return new PyFloat(leftv - value);
    }

    public PyObject __mul__(PyObject right) {
        return float___mul__(right);
    }

    final PyObject float___mul__(PyObject right) {
        if (!canCoerce(right))
            return null;
        double rightv = coerce(right);
        return new PyFloat(value * rightv);
    }

    public PyObject __rmul__(PyObject left) {
        return float___rmul__(left);
    }

    final PyObject float___rmul__(PyObject left) {
        return __mul__(left);
    }

    public PyObject __div__(PyObject right) {
        return float___div__(right);
    }

    final PyObject float___div__(PyObject right) {
        if (!canCoerce(right))
            return null;
        if (Options.divisionWarning >= 2)
            Py.warning(Py.DeprecationWarning, "classic float division");
        double rightv = coerce(right);
        if (rightv == 0)
            throw Py.ZeroDivisionError("float division");
        return new PyFloat(value / rightv);
    }

    public PyObject __rdiv__(PyObject left) {
        return float___rdiv__(left);
    }

    final PyObject float___rdiv__(PyObject left) {
        if (!canCoerce(left))
            return null;
        if (Options.divisionWarning >= 2)
            Py.warning(Py.DeprecationWarning, "classic float division");
        double leftv = coerce(left);
        if (value == 0)
            throw Py.ZeroDivisionError("float division");
        return new PyFloat(leftv / value);
    }

    public PyObject __floordiv__(PyObject right) {
        return float___floordiv__(right);
    }

    final PyObject float___floordiv__(PyObject right) {
        if (!canCoerce(right))
            return null;
        double rightv = coerce(right);
        if (rightv == 0)
            throw Py.ZeroDivisionError("float division");
        return new PyFloat(Math.floor(value / rightv));
    }

    public PyObject __rfloordiv__(PyObject left) {
        return float___rfloordiv__(left);
    }

    final PyObject float___rfloordiv__(PyObject left) {
        if (!canCoerce(left))
            return null;
        double leftv = coerce(left);
        if (value == 0)
            throw Py.ZeroDivisionError("float division");
        return new PyFloat(Math.floor(leftv / value));
    }

    public PyObject __truediv__(PyObject right) {
        return float___truediv__(right);
    }

    final PyObject float___truediv__(PyObject right) {
        if (!canCoerce(right))
            return null;
        double rightv = coerce(right);
        if (rightv == 0)
            throw Py.ZeroDivisionError("float division");
        return new PyFloat(value / rightv);
    }

    public PyObject __rtruediv__(PyObject left) {
        return float___rtruediv__(left);
    }

    final PyObject float___rtruediv__(PyObject left) {
        if (!canCoerce(left))
            return null;
        double leftv = coerce(left);
        if (value == 0)
            throw Py.ZeroDivisionError("float division");
        return new PyFloat(leftv / value);
    }

    private static double modulo(double x, double y) {
        if (y == 0)
            throw Py.ZeroDivisionError("float modulo");
        double z = Math.IEEEremainder(x, y);
        if (z*y < 0)
            z += y;
        return z;
    }

    public PyObject __mod__(PyObject right) {
        return float___mod__(right);
    }

    final PyObject float___mod__(PyObject right) {
        if (!canCoerce(right))
            return null;
        double rightv = coerce(right);
        return new PyFloat(modulo(value, rightv));
    }

    public PyObject __rmod__(PyObject left) {
        return float___rmod__(left);
    }

    final PyObject float___rmod__(PyObject left) {
        if (!canCoerce(left))
            return null;
        double leftv = coerce(left);
        return new PyFloat(modulo(leftv, value));
    }

    public PyObject __divmod__(PyObject right) {
        return float___divmod__(right);
    }

    final PyObject float___divmod__(PyObject right) {
        if (!canCoerce(right))
            return null;
        double rightv = coerce(right);

        if (rightv == 0)
            throw Py.ZeroDivisionError("float division");
        double z = Math.floor(value / rightv);

        return new PyTuple(
            new PyObject[] {new PyFloat(z), new PyFloat(value-z*rightv)}
            );
    }

    public PyObject __rdivmod__(PyObject left) {
        if (!canCoerce(left))
            return null;
        double leftv = coerce(left);

        if (value == 0)
            throw Py.ZeroDivisionError("float division");
        double z = Math.floor(leftv / value);

        return new PyTuple(
            new PyObject[] {new PyFloat(z), new PyFloat(leftv-z*value)}
            );
    }


    public PyObject __pow__(PyObject right, PyObject modulo) {
        return float___pow__(right, modulo);
    }

    final PyObject float___pow__(PyObject right, PyObject modulo) {
        if (!canCoerce(right))
            return null;

        if (modulo != null) {
            throw Py.TypeError("pow() 3rd argument not allowed " +
                               "unless all arguments are integers");
        }

        return _pow(value, coerce(right), modulo);
    }

    public PyObject __rpow__(PyObject left) {
        if (!canCoerce(left))
            return null;

        return _pow(coerce(left), value, null);
    }

    private static PyFloat _pow(double value, double iw, PyObject modulo) {
        // Rely completely on Java's pow function
        if (iw == 0) {
            if (modulo != null)
                return new PyFloat(modulo(1.0, coerce(modulo)));
            return new PyFloat(1.0);
        }
        if (value == 0.0) {
            if (iw < 0.0)
                throw Py.ZeroDivisionError("0.0 cannot be raised to a " +
                                           "negative power");
            return new PyFloat(0);
        }

        double ret = Math.pow(value, iw);
        if (modulo == null) {
            return new PyFloat(ret);
        } else {
            return new PyFloat(modulo(ret, coerce(modulo)));
        }
    }

    public PyObject __neg__() {
        return float___neg__();
    }

    final PyObject float___neg__() {
        return new PyFloat(-value);
    }

    public PyObject __pos__() {
        return float___pos__();
    }

    final PyObject float___pos__() {
        return this;
    }

    public PyObject __abs__() {
        return float___abs__();
    }

    final PyObject float___abs__() {
        if (value >= 0)
            return this;
        else
            return __neg__();
    }

    public PyObject __int__() {
        return float___int__();
    }

    final PyInteger float___int__() {
        if (value <= Integer.MAX_VALUE && value >= Integer.MIN_VALUE) {
            return new PyInteger((int)value);
        }
        throw Py.OverflowError("float too large to convert");
    }

    public PyLong __long__() {
        return float___long__();
    }

    final PyLong float___long__() {
        return new PyLong(value);
    }

    public PyFloat __float__() {
        return float___float__();
    }

    final PyFloat float___float__() {
        return this;
    }
    public PyComplex __complex__() {
        return new PyComplex(value, 0.);
    }

    public boolean isMappingType() throws PyIgnoreMethodTag { return false; }
    public boolean isSequenceType() throws PyIgnoreMethodTag { return false; }

}
