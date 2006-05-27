// Copyright (c) Corporation for National Research Initiatives
package org.python.core;

import java.io.Serializable;

/**
 * A builtin python int.
 */
public class PyInteger extends PyObject {
    //~ BEGIN GENERATED REGION -- DO NOT EDIT SEE gexpose.py
    /* type info */

    public static final String exposed_name="int";

    public static void typeSetup(PyObject dict,PyType.Newstyle marker) {
        class exposed___abs__ extends PyBuiltinFunctionNarrow {

            private PyInteger self;

            public PyObject getSelf() {
                return self;
            }

            exposed___abs__(PyInteger self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___abs__((PyInteger)self,info);
            }

            public PyObject __call__() {
                return self.int___abs__();
            }

            public PyObject inst_call(PyObject gself) {
                PyInteger self=(PyInteger)gself;
                return self.int___abs__();
            }

        }
        dict.__setitem__("__abs__",new PyMethodDescr("__abs__",PyInteger.class,0,0,new exposed___abs__(null,null)));
        class exposed___float__ extends PyBuiltinFunctionNarrow {

            private PyInteger self;

            public PyObject getSelf() {
                return self;
            }

            exposed___float__(PyInteger self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___float__((PyInteger)self,info);
            }

            public PyObject __call__() {
                return self.int___float__();
            }

            public PyObject inst_call(PyObject gself) {
                PyInteger self=(PyInteger)gself;
                return self.int___float__();
            }

        }
        dict.__setitem__("__float__",new PyMethodDescr("__float__",PyInteger.class,0,0,new exposed___float__(null,null)));
        class exposed___hex__ extends PyBuiltinFunctionNarrow {

            private PyInteger self;

            public PyObject getSelf() {
                return self;
            }

            exposed___hex__(PyInteger self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___hex__((PyInteger)self,info);
            }

            public PyObject __call__() {
                return self.int___hex__();
            }

            public PyObject inst_call(PyObject gself) {
                PyInteger self=(PyInteger)gself;
                return self.int___hex__();
            }

        }
        dict.__setitem__("__hex__",new PyMethodDescr("__hex__",PyInteger.class,0,0,new exposed___hex__(null,null)));
        class exposed___int__ extends PyBuiltinFunctionNarrow {

            private PyInteger self;

            public PyObject getSelf() {
                return self;
            }

            exposed___int__(PyInteger self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___int__((PyInteger)self,info);
            }

            public PyObject __call__() {
                return self.int___int__();
            }

            public PyObject inst_call(PyObject gself) {
                PyInteger self=(PyInteger)gself;
                return self.int___int__();
            }

        }
        dict.__setitem__("__int__",new PyMethodDescr("__int__",PyInteger.class,0,0,new exposed___int__(null,null)));
        class exposed___invert__ extends PyBuiltinFunctionNarrow {

            private PyInteger self;

            public PyObject getSelf() {
                return self;
            }

            exposed___invert__(PyInteger self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___invert__((PyInteger)self,info);
            }

            public PyObject __call__() {
                return self.int___invert__();
            }

            public PyObject inst_call(PyObject gself) {
                PyInteger self=(PyInteger)gself;
                return self.int___invert__();
            }

        }
        dict.__setitem__("__invert__",new PyMethodDescr("__invert__",PyInteger.class,0,0,new exposed___invert__(null,null)));
        class exposed___long__ extends PyBuiltinFunctionNarrow {

            private PyInteger self;

            public PyObject getSelf() {
                return self;
            }

            exposed___long__(PyInteger self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___long__((PyInteger)self,info);
            }

            public PyObject __call__() {
                return self.int___long__();
            }

            public PyObject inst_call(PyObject gself) {
                PyInteger self=(PyInteger)gself;
                return self.int___long__();
            }

        }
        dict.__setitem__("__long__",new PyMethodDescr("__long__",PyInteger.class,0,0,new exposed___long__(null,null)));
        class exposed___neg__ extends PyBuiltinFunctionNarrow {

            private PyInteger self;

            public PyObject getSelf() {
                return self;
            }

            exposed___neg__(PyInteger self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___neg__((PyInteger)self,info);
            }

            public PyObject __call__() {
                return self.int___neg__();
            }

            public PyObject inst_call(PyObject gself) {
                PyInteger self=(PyInteger)gself;
                return self.int___neg__();
            }

        }
        dict.__setitem__("__neg__",new PyMethodDescr("__neg__",PyInteger.class,0,0,new exposed___neg__(null,null)));
        class exposed___oct__ extends PyBuiltinFunctionNarrow {

            private PyInteger self;

            public PyObject getSelf() {
                return self;
            }

            exposed___oct__(PyInteger self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___oct__((PyInteger)self,info);
            }

            public PyObject __call__() {
                return self.int___oct__();
            }

            public PyObject inst_call(PyObject gself) {
                PyInteger self=(PyInteger)gself;
                return self.int___oct__();
            }

        }
        dict.__setitem__("__oct__",new PyMethodDescr("__oct__",PyInteger.class,0,0,new exposed___oct__(null,null)));
        class exposed___pos__ extends PyBuiltinFunctionNarrow {

            private PyInteger self;

            public PyObject getSelf() {
                return self;
            }

            exposed___pos__(PyInteger self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___pos__((PyInteger)self,info);
            }

            public PyObject __call__() {
                return self.int___pos__();
            }

            public PyObject inst_call(PyObject gself) {
                PyInteger self=(PyInteger)gself;
                return self.int___pos__();
            }

        }
        dict.__setitem__("__pos__",new PyMethodDescr("__pos__",PyInteger.class,0,0,new exposed___pos__(null,null)));
        class exposed___add__ extends PyBuiltinFunctionNarrow {

            private PyInteger self;

            public PyObject getSelf() {
                return self;
            }

            exposed___add__(PyInteger self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___add__((PyInteger)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=self.int___add__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyInteger self=(PyInteger)gself;
                PyObject ret=self.int___add__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__add__",new PyMethodDescr("__add__",PyInteger.class,1,1,new exposed___add__(null,null)));
        class exposed___and__ extends PyBuiltinFunctionNarrow {

            private PyInteger self;

            public PyObject getSelf() {
                return self;
            }

            exposed___and__(PyInteger self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___and__((PyInteger)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=self.int___and__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyInteger self=(PyInteger)gself;
                PyObject ret=self.int___and__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__and__",new PyMethodDescr("__and__",PyInteger.class,1,1,new exposed___and__(null,null)));
        class exposed___div__ extends PyBuiltinFunctionNarrow {

            private PyInteger self;

            public PyObject getSelf() {
                return self;
            }

            exposed___div__(PyInteger self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___div__((PyInteger)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=self.int___div__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyInteger self=(PyInteger)gself;
                PyObject ret=self.int___div__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__div__",new PyMethodDescr("__div__",PyInteger.class,1,1,new exposed___div__(null,null)));
        class exposed___divmod__ extends PyBuiltinFunctionNarrow {

            private PyInteger self;

            public PyObject getSelf() {
                return self;
            }

            exposed___divmod__(PyInteger self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___divmod__((PyInteger)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=self.int___divmod__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyInteger self=(PyInteger)gself;
                PyObject ret=self.int___divmod__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__divmod__",new PyMethodDescr("__divmod__",PyInteger.class,1,1,new exposed___divmod__(null,null)));
        class exposed___floordiv__ extends PyBuiltinFunctionNarrow {

            private PyInteger self;

            public PyObject getSelf() {
                return self;
            }

            exposed___floordiv__(PyInteger self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___floordiv__((PyInteger)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=self.int___floordiv__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyInteger self=(PyInteger)gself;
                PyObject ret=self.int___floordiv__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__floordiv__",new PyMethodDescr("__floordiv__",PyInteger.class,1,1,new exposed___floordiv__(null,null)));
        class exposed___lshift__ extends PyBuiltinFunctionNarrow {

            private PyInteger self;

            public PyObject getSelf() {
                return self;
            }

            exposed___lshift__(PyInteger self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___lshift__((PyInteger)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=self.int___lshift__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyInteger self=(PyInteger)gself;
                PyObject ret=self.int___lshift__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__lshift__",new PyMethodDescr("__lshift__",PyInteger.class,1,1,new exposed___lshift__(null,null)));
        class exposed___mod__ extends PyBuiltinFunctionNarrow {

            private PyInteger self;

            public PyObject getSelf() {
                return self;
            }

            exposed___mod__(PyInteger self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___mod__((PyInteger)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=self.int___mod__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyInteger self=(PyInteger)gself;
                PyObject ret=self.int___mod__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__mod__",new PyMethodDescr("__mod__",PyInteger.class,1,1,new exposed___mod__(null,null)));
        class exposed___mul__ extends PyBuiltinFunctionNarrow {

            private PyInteger self;

            public PyObject getSelf() {
                return self;
            }

            exposed___mul__(PyInteger self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___mul__((PyInteger)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=self.int___mul__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyInteger self=(PyInteger)gself;
                PyObject ret=self.int___mul__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__mul__",new PyMethodDescr("__mul__",PyInteger.class,1,1,new exposed___mul__(null,null)));
        class exposed___or__ extends PyBuiltinFunctionNarrow {

            private PyInteger self;

            public PyObject getSelf() {
                return self;
            }

            exposed___or__(PyInteger self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___or__((PyInteger)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=self.int___or__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyInteger self=(PyInteger)gself;
                PyObject ret=self.int___or__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__or__",new PyMethodDescr("__or__",PyInteger.class,1,1,new exposed___or__(null,null)));
        class exposed___radd__ extends PyBuiltinFunctionNarrow {

            private PyInteger self;

            public PyObject getSelf() {
                return self;
            }

            exposed___radd__(PyInteger self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___radd__((PyInteger)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=self.int___radd__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyInteger self=(PyInteger)gself;
                PyObject ret=self.int___radd__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__radd__",new PyMethodDescr("__radd__",PyInteger.class,1,1,new exposed___radd__(null,null)));
        class exposed___rdiv__ extends PyBuiltinFunctionNarrow {

            private PyInteger self;

            public PyObject getSelf() {
                return self;
            }

            exposed___rdiv__(PyInteger self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___rdiv__((PyInteger)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=self.int___rdiv__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyInteger self=(PyInteger)gself;
                PyObject ret=self.int___rdiv__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__rdiv__",new PyMethodDescr("__rdiv__",PyInteger.class,1,1,new exposed___rdiv__(null,null)));
        class exposed___rfloordiv__ extends PyBuiltinFunctionNarrow {

            private PyInteger self;

            public PyObject getSelf() {
                return self;
            }

            exposed___rfloordiv__(PyInteger self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___rfloordiv__((PyInteger)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=self.int___rfloordiv__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyInteger self=(PyInteger)gself;
                PyObject ret=self.int___rfloordiv__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__rfloordiv__",new PyMethodDescr("__rfloordiv__",PyInteger.class,1,1,new exposed___rfloordiv__(null,null)));
        class exposed___rmod__ extends PyBuiltinFunctionNarrow {

            private PyInteger self;

            public PyObject getSelf() {
                return self;
            }

            exposed___rmod__(PyInteger self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___rmod__((PyInteger)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=self.int___rmod__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyInteger self=(PyInteger)gself;
                PyObject ret=self.int___rmod__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__rmod__",new PyMethodDescr("__rmod__",PyInteger.class,1,1,new exposed___rmod__(null,null)));
        class exposed___rmul__ extends PyBuiltinFunctionNarrow {

            private PyInteger self;

            public PyObject getSelf() {
                return self;
            }

            exposed___rmul__(PyInteger self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___rmul__((PyInteger)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=self.int___rmul__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyInteger self=(PyInteger)gself;
                PyObject ret=self.int___rmul__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__rmul__",new PyMethodDescr("__rmul__",PyInteger.class,1,1,new exposed___rmul__(null,null)));
        class exposed___rshift__ extends PyBuiltinFunctionNarrow {

            private PyInteger self;

            public PyObject getSelf() {
                return self;
            }

            exposed___rshift__(PyInteger self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___rshift__((PyInteger)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=self.int___rshift__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyInteger self=(PyInteger)gself;
                PyObject ret=self.int___rshift__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__rshift__",new PyMethodDescr("__rshift__",PyInteger.class,1,1,new exposed___rshift__(null,null)));
        class exposed___rsub__ extends PyBuiltinFunctionNarrow {

            private PyInteger self;

            public PyObject getSelf() {
                return self;
            }

            exposed___rsub__(PyInteger self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___rsub__((PyInteger)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=self.int___rsub__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyInteger self=(PyInteger)gself;
                PyObject ret=self.int___rsub__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__rsub__",new PyMethodDescr("__rsub__",PyInteger.class,1,1,new exposed___rsub__(null,null)));
        class exposed___rtruediv__ extends PyBuiltinFunctionNarrow {

            private PyInteger self;

            public PyObject getSelf() {
                return self;
            }

            exposed___rtruediv__(PyInteger self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___rtruediv__((PyInteger)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=self.int___rtruediv__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyInteger self=(PyInteger)gself;
                PyObject ret=self.int___rtruediv__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__rtruediv__",new PyMethodDescr("__rtruediv__",PyInteger.class,1,1,new exposed___rtruediv__(null,null)));
        class exposed___sub__ extends PyBuiltinFunctionNarrow {

            private PyInteger self;

            public PyObject getSelf() {
                return self;
            }

            exposed___sub__(PyInteger self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___sub__((PyInteger)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=self.int___sub__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyInteger self=(PyInteger)gself;
                PyObject ret=self.int___sub__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__sub__",new PyMethodDescr("__sub__",PyInteger.class,1,1,new exposed___sub__(null,null)));
        class exposed___truediv__ extends PyBuiltinFunctionNarrow {

            private PyInteger self;

            public PyObject getSelf() {
                return self;
            }

            exposed___truediv__(PyInteger self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___truediv__((PyInteger)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=self.int___truediv__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyInteger self=(PyInteger)gself;
                PyObject ret=self.int___truediv__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__truediv__",new PyMethodDescr("__truediv__",PyInteger.class,1,1,new exposed___truediv__(null,null)));
        class exposed___xor__ extends PyBuiltinFunctionNarrow {

            private PyInteger self;

            public PyObject getSelf() {
                return self;
            }

            exposed___xor__(PyInteger self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___xor__((PyInteger)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=self.int___xor__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyInteger self=(PyInteger)gself;
                PyObject ret=self.int___xor__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__xor__",new PyMethodDescr("__xor__",PyInteger.class,1,1,new exposed___xor__(null,null)));
        class exposed___cmp__ extends PyBuiltinFunctionNarrow {

            private PyInteger self;

            public PyObject getSelf() {
                return self;
            }

            exposed___cmp__(PyInteger self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___cmp__((PyInteger)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                int ret=self.int___cmp__(arg0);
                if (ret==-2) {
                    throw Py.TypeError("int"+".__cmp__(x,y) requires y to be '"+"int"+"', not a '"+(arg0).getType().fastGetName()+"'");
                }
                return Py.newInteger(ret);
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyInteger self=(PyInteger)gself;
                int ret=self.int___cmp__(arg0);
                if (ret==-2) {
                    throw Py.TypeError("int"+".__cmp__(x,y) requires y to be '"+"int"+"', not a '"+(arg0).getType().fastGetName()+"'");
                }
                return Py.newInteger(ret);
            }

        }
        dict.__setitem__("__cmp__",new PyMethodDescr("__cmp__",PyInteger.class,1,1,new exposed___cmp__(null,null)));
        class exposed___pow__ extends PyBuiltinFunctionNarrow {

            private PyInteger self;

            public PyObject getSelf() {
                return self;
            }

            exposed___pow__(PyInteger self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___pow__((PyInteger)self,info);
            }

            public PyObject __call__(PyObject arg0,PyObject arg1) {
                PyObject ret=self.int___pow__(arg0,arg1);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

            public PyObject inst_call(PyObject gself,PyObject arg0,PyObject arg1) {
                PyInteger self=(PyInteger)gself;
                PyObject ret=self.int___pow__(arg0,arg1);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=self.int___pow__(arg0,null);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyInteger self=(PyInteger)gself;
                PyObject ret=self.int___pow__(arg0,null);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__pow__",new PyMethodDescr("__pow__",PyInteger.class,1,2,new exposed___pow__(null,null)));
        class exposed___nonzero__ extends PyBuiltinFunctionNarrow {

            private PyInteger self;

            public PyObject getSelf() {
                return self;
            }

            exposed___nonzero__(PyInteger self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___nonzero__((PyInteger)self,info);
            }

            public PyObject __call__() {
                return Py.newBoolean(self.int___nonzero__());
            }

            public PyObject inst_call(PyObject gself) {
                PyInteger self=(PyInteger)gself;
                return Py.newBoolean(self.int___nonzero__());
            }

        }
        dict.__setitem__("__nonzero__",new PyMethodDescr("__nonzero__",PyInteger.class,0,0,new exposed___nonzero__(null,null)));
        class exposed___reduce__ extends PyBuiltinFunctionNarrow {

            private PyInteger self;

            public PyObject getSelf() {
                return self;
            }

            exposed___reduce__(PyInteger self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___reduce__((PyInteger)self,info);
            }

            public PyObject __call__() {
                return self.int___reduce__();
            }

            public PyObject inst_call(PyObject gself) {
                PyInteger self=(PyInteger)gself;
                return self.int___reduce__();
            }

        }
        dict.__setitem__("__reduce__",new PyMethodDescr("__reduce__",PyInteger.class,0,0,new exposed___reduce__(null,null)));
        class exposed___repr__ extends PyBuiltinFunctionNarrow {

            private PyInteger self;

            public PyObject getSelf() {
                return self;
            }

            exposed___repr__(PyInteger self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___repr__((PyInteger)self,info);
            }

            public PyObject __call__() {
                return new PyString(self.int_toString());
            }

            public PyObject inst_call(PyObject gself) {
                PyInteger self=(PyInteger)gself;
                return new PyString(self.int_toString());
            }

        }
        dict.__setitem__("__repr__",new PyMethodDescr("__repr__",PyInteger.class,0,0,new exposed___repr__(null,null)));
        class exposed___str__ extends PyBuiltinFunctionNarrow {

            private PyInteger self;

            public PyObject getSelf() {
                return self;
            }

            exposed___str__(PyInteger self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___str__((PyInteger)self,info);
            }

            public PyObject __call__() {
                return new PyString(self.int_toString());
            }

            public PyObject inst_call(PyObject gself) {
                PyInteger self=(PyInteger)gself;
                return new PyString(self.int_toString());
            }

        }
        dict.__setitem__("__str__",new PyMethodDescr("__str__",PyInteger.class,0,0,new exposed___str__(null,null)));
        class exposed___hash__ extends PyBuiltinFunctionNarrow {

            private PyInteger self;

            public PyObject getSelf() {
                return self;
            }

            exposed___hash__(PyInteger self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___hash__((PyInteger)self,info);
            }

            public PyObject __call__() {
                return Py.newInteger(self.int_hashCode());
            }

            public PyObject inst_call(PyObject gself) {
                PyInteger self=(PyInteger)gself;
                return Py.newInteger(self.int_hashCode());
            }

        }
        dict.__setitem__("__hash__",new PyMethodDescr("__hash__",PyInteger.class,0,0,new exposed___hash__(null,null)));
        dict.__setitem__("__new__",new PyNewWrapper(PyInteger.class,"__new__",-1,-1) {

                                                                                         public PyObject new_impl(boolean init,PyType subtype,PyObject[]args,String[]keywords) {
                                                                                             return int_new(this,init,subtype,args,keywords);
                                                                                         }

                                                                                     });
    }
    //~ END GENERATED REGION -- DO NOT EDIT SEE gexpose.py
    
    public static PyObject int_new(PyNewWrapper new_, boolean init, PyType subtype,
            PyObject[] args, String[] keywords) {
        ArgParser ap = new ArgParser("int", args, keywords, new String[] { "x",
                "base" }, 0);
        PyObject x = ap.getPyObject(0, null);
        int base = ap.getInt(1, -909);
        if (new_.for_type == subtype) {
            if (x == null) {
                return Py.Zero;
            }
            if (base == -909) {
                return x.__int__();
            }
            if (!(x instanceof PyString)) {
                throw Py
                        .TypeError("int: can't convert non-string with explicit base");
            }
            return Py.newInteger(((PyString) x).atoi(base));
        } else {
            if (x == null) {
                return new PyIntegerDerived(subtype, 0);
            }
            if (base == -909) {
                PyObject intOrLong = x.__int__();
                if (intOrLong instanceof PyInteger) {
                    return new PyIntegerDerived(subtype, ((PyInteger)intOrLong).getValue());
                }
                else {
                    err_ovf("long int too large to convert to int");
                }
            }
            if (!(x instanceof PyString)) {
                throw Py
                        .TypeError("int: can't convert non-string with explicit base");
            }
            return new PyIntegerDerived(subtype, ((PyString) x).atoi(base));
        }
    } // xxx
    
    private static final PyType INTTYPE = PyType.fromClass(PyInteger.class);
    
    private int value;

    public PyInteger(PyType subType, int v) {
        super(subType);
        value = v;
    }

    public PyInteger(int v) {
        this(INTTYPE, v);
    }

    public int getValue() {
        return value;
    }

    public String safeRepr() throws PyIgnoreMethodTag {
        return "'int' object";
    }

    public String toString() {
        return int_toString();
    }

    final String int_toString() {
        return Integer.toString(value);
    }

    public int hashCode() {
        return int_hashCode();
    }

    final int int_hashCode() {
        return value;
    }

    private static void err_ovf(String msg) {
        try {
            Py.OverflowWarning(msg);
        } catch (PyException exc) {
            if (Py.matchException(exc, Py.OverflowWarning))
                throw Py.OverflowError(msg);
        }
    }

    public boolean __nonzero__() {
        return int___nonzero__();
    }

    final boolean int___nonzero__() {
        return value != 0;
    }

    public Object __tojava__(Class c) {
        if (c == Integer.TYPE || c == Number.class ||
            c == Object.class || c == Integer.class ||
            c == Serializable.class)
        {
            return new Integer(value);
        }

        if (c == Boolean.TYPE || c == Boolean.class)
            return new Boolean(value != 0);
        if (c == Byte.TYPE || c == Byte.class)
            return new Byte((byte)value);
        if (c == Short.TYPE || c == Short.class)
            return new Short((short)value);

        if (c == Long.TYPE || c == Long.class)
            return new Long(value);
        if (c == Float.TYPE || c == Float.class)
            return new Float(value);
        if (c == Double.TYPE || c == Double.class)
            return new Double(value);
        return super.__tojava__(c);
    }

    public int __cmp__(PyObject other) {
        return int___cmp__(other);
    }

    final int int___cmp__(PyObject other) {
        if (!canCoerce(other))
             return -2;
        int v = coerce(other);
        return value < v ? -1 : value > v ? 1 : 0;
    }

    public Object __coerce_ex__(PyObject other) {
        if (other instanceof PyInteger)
            return other;
        else
            return Py.None;
    }

    private static final boolean canCoerce(PyObject other) {
        return other instanceof PyInteger;
    }

    private static final int coerce(PyObject other) {
        if (other instanceof PyInteger)
            return ((PyInteger) other).value;
        else
            throw Py.TypeError("xxx");
    }


    public PyObject __add__(PyObject right) {
        return int___add__(right);
    }

    final PyObject int___add__(PyObject right) {
        if (!canCoerce(right))
            return null;
        int rightv = coerce(right);
        int a = value;
        int b = rightv;
        int x = a + b;
        if ((x^a) >= 0 || (x^b) >= 0)
            return Py.newInteger(x);
        err_ovf("integer addition");
        return new PyLong((long) a + (long)b);
    }

    public PyObject __radd__(PyObject left) {
        return int___radd__(left);
    }

    final PyObject int___radd__(PyObject left) {
        return __add__(left);
    }

    private static PyObject _sub(int a, int b) {
        int x = a - b;
        if ((x^a) >= 0 || (x^~b) >= 0)
            return Py.newInteger(x);
        err_ovf("integer subtraction");
        return new PyLong((long) a - (long)b);
    }

    public PyObject __sub__(PyObject right) {
        return int___sub__(right);
    }

    final PyObject int___sub__(PyObject right) {
        if (!canCoerce(right))
            return null;
        return _sub(value, coerce(right));
    }

    public PyObject __rsub__(PyObject left) {
        return int___rsub__(left);
    }

    final PyObject int___rsub__(PyObject left) {
        if (!canCoerce(left))
            return null;
        return _sub(coerce(left), value);
    }

    public PyObject __mul__(PyObject right) {
        return int___mul__(right);
    }

    final PyObject int___mul__(PyObject right) {
        if (right instanceof PySequence)
            return ((PySequence) right).repeat(value);

        if (!canCoerce(right))
            return null;
        int rightv = coerce(right);

        double x = (double)value;
        x *= rightv;
        //long x = ((long)value)*((PyInteger)right).value;
        //System.out.println("mul: "+this+" * "+right+" = "+x);

        if (x <= Integer.MAX_VALUE && x >= Integer.MIN_VALUE)
            return Py.newInteger((int)x);
        err_ovf("integer multiplication");
        return __long__().__mul__(right);
    }

    public PyObject __rmul__(PyObject left) {
        return int___rmul__(left);
    }

    final PyObject int___rmul__(PyObject left) {
        return __mul__(left);
    }

    // Getting signs correct for integer division
    // This convention makes sense when you consider it in tandem with modulo
    private static int divide(int x, int y) {
        if (y == 0)
            throw Py.ZeroDivisionError("integer division or modulo by zero");

        if (y == -1 && x < 0 && x == -x) {
            err_ovf("integer division: "+x+" + "+y);
        }
        int xdivy = x / y;
        int xmody = x - xdivy * y;
        /* If the signs of x and y differ, and the remainder is non-0,
         * C89 doesn't define whether xdivy is now the floor or the
         * ceiling of the infinitely precise quotient.  We want the floor,
         * and we have it iff the remainder's sign matches y's.
         */
        if (xmody != 0 && ((y ^ xmody) < 0) /* i.e. and signs differ */) {
            xmody += y;
            --xdivy;
            //assert(xmody && ((y ^ xmody) >= 0));
        }
        return xdivy;
    }

    public PyObject __div__(PyObject right) {
        return int___div__(right);
    }

    final PyObject int___div__(PyObject right) {
        if (!canCoerce(right))
            return null;
        if (Options.divisionWarning > 0)
            Py.warning(Py.DeprecationWarning, "classic int division");
        return Py.newInteger(divide(value, coerce(right)));
    }

    public PyObject __rdiv__(PyObject left) {
        return int___rdiv__(left);
    }

    final PyObject int___rdiv__(PyObject left) {
        if (!canCoerce(left))
            return null;
        if (Options.divisionWarning > 0)
            Py.warning(Py.DeprecationWarning, "classic int division");
        return Py.newInteger(divide(coerce(left), value));
    }

    public PyObject __floordiv__(PyObject right) {
        return int___floordiv__(right);
    }

    final PyObject int___floordiv__(PyObject right) {
        if (!canCoerce(right))
            return null;
        return Py.newInteger(divide(value, coerce(right)));
    }

    public PyObject __rfloordiv__(PyObject left) {
        return int___rfloordiv__(left);
    }

    final PyObject int___rfloordiv__(PyObject left) {
        if (!canCoerce(left))
            return null;
        return Py.newInteger(divide(coerce(left), value));
    }

    public PyObject __truediv__(PyObject right) {
        return int___truediv__(right);
    }

    final PyObject int___truediv__(PyObject right) {
        if (right instanceof PyInteger)
            return __float__().__truediv__(right);
        return null;
    }

    public PyObject __rtruediv__(PyObject left) {
        return int___rtruediv__(left);
    }

    final PyObject int___rtruediv__(PyObject left) {
        if (left instanceof PyInteger)
            return left.__float__().__truediv__(this);
        return null;
    }

    private static int modulo(int x, int y, int xdivy) {
        return x - xdivy*y;
    }

    public PyObject __mod__(PyObject right) {
        return int___mod__(right);
    }

    final PyObject int___mod__(PyObject right) {
        if (!canCoerce(right))
            return null;
        int rightv = coerce(right);
        return Py.newInteger(modulo(value, rightv, divide(value, rightv)));
    }

    public PyObject __rmod__(PyObject left) {
        return int___rmod__(left);
    }

    final PyObject int___rmod__(PyObject left) {
        if (!canCoerce(left))
            return null;
        int leftv = coerce(left);
        return Py.newInteger(modulo(leftv, value, divide(leftv, value)));
    }

    public PyObject __divmod__(PyObject right) {
        return int___divmod__(right);
    }

    final PyObject int___divmod__(PyObject right) {
        if (!canCoerce(right))
            return null;
        int rightv = coerce(right);

        int xdivy = divide(value, rightv);
        return new PyTuple(new PyObject[] {
            new PyInteger(xdivy),
            new PyInteger(modulo(value, rightv, xdivy))
        });
    }

    public PyObject __pow__(PyObject right, PyObject modulo) {
        return int___pow__(right,modulo);
    }

    final PyObject int___pow__(PyObject right, PyObject modulo) {
        if (!canCoerce(right))
            return null;

        if (modulo != null && !canCoerce(modulo))
            return null;

        return _pow(value, coerce(right), modulo, this, right);
    }

    public PyObject __rpow__(PyObject left, PyObject modulo) {
        if (!canCoerce(left))
            return null;

        if (modulo != null && !canCoerce(modulo))
            return null;

        return _pow(coerce(left), value, modulo, left, this);
    }

    private static PyObject _pow(int value, int pow, PyObject modulo,
                                 PyObject left, PyObject right) {
        int mod = 0;
        long tmp = value;
        boolean neg = false;
        if (tmp < 0) {
            tmp = -tmp;
            neg = (pow & 0x1) != 0;
        }
        long result = 1;

        if (pow < 0) {
            if (value != 0)
                return left.__float__().__pow__(right, modulo);
            else
                throw Py.ZeroDivisionError("cannot raise 0 to a " +
                                           "negative power");
        }

        if (modulo != null) {
            mod = coerce(modulo);
            if (mod == 0) {
                throw Py.ValueError("pow(x, y, z) with z==0");
            }
        }

        // Standard O(ln(N)) exponentiation code
        while (pow > 0) {
            if ((pow & 0x1) != 0) {
                result *= tmp;
                if (mod != 0) {
                    result %= (long)mod;
                }

                if (result > Integer.MAX_VALUE) {
                    err_ovf("integer exponentiation");
                    return left.__long__().__pow__(right, modulo);
                }
            }
            pow >>= 1;
            if (pow == 0)
                break;
            tmp *= tmp;

            if (mod != 0) {
                tmp %= (long)mod;
            }

            if (tmp > Integer.MAX_VALUE) {
                err_ovf("integer exponentiation");
                return left.__long__().__pow__(right, modulo);
            }
        }

        int ret = (int)result;
        if (neg)
            ret = -ret;

        // Cleanup result of modulo
        if (mod != 0) {
            ret = modulo(ret, mod, divide(ret, mod));
        }
        return Py.newInteger(ret);
    }

    public PyObject __lshift__(PyObject right) {
        return int___lshift__(right);
    }

    final PyObject int___lshift__(PyObject right) {
        int rightv;
        if (right instanceof PyInteger)
             rightv = ((PyInteger)right).value;
        else
             return null;

        if (rightv > 31)
            return new PyInteger(0);
        else if(rightv < 0)
            throw Py.ValueError("negative shift count");
        return Py.newInteger(value << rightv);
    }

    public PyObject __rshift__(PyObject right) {
        return int___rshift__(right);
    }

    final PyObject int___rshift__(PyObject right) {
        int rightv;
        if (right instanceof PyInteger)
             rightv = ((PyInteger)right).value;
        else
             return null;

        if(rightv < 0)
            throw Py.ValueError("negative shift count");

        return Py.newInteger(value >> rightv);
    }

    public PyObject __and__(PyObject right) {
        return int___and__(right);
    }

    final PyObject int___and__(PyObject right) {
        int rightv;
        if (right instanceof PyInteger)
             rightv = ((PyInteger)right).value;
        else
             return null;

        return Py.newInteger(value & rightv);
    }

    public PyObject __xor__(PyObject right) {
        return int___xor__(right);
    }

    final PyObject int___xor__(PyObject right) {
        int rightv;
        if (right instanceof PyInteger)
             rightv = ((PyInteger)right).value;
        else
             return null;

        return Py.newInteger(value ^ rightv);
    }

    public PyObject __or__(PyObject right) {
        return int___or__(right);
    }

    final PyObject int___or__(PyObject right) {
        int rightv;
        if (right instanceof PyInteger)
             rightv = ((PyInteger)right).value;
        else
             return null;

        return Py.newInteger(value | rightv);
    }

    public PyObject __neg__() {
        return int___neg__();
    }

    final PyObject int___neg__() {
        int x = -value;
        if (value < 0 && x < 0)
            err_ovf("integer negation");
        return Py.newInteger(x);
    }

    public PyObject __pos__() {
        return int___pos__();
    }

    final PyObject int___pos__() {
        return this;
    }

    public PyObject __abs__() {
        return int___abs__();
    }

    final PyObject int___abs__() {
        if (value >= 0)
            return this;
        else
            return __neg__();
    }

    public PyObject __invert__() {
        return int___invert__();
    }

    final PyObject int___invert__() {
        return Py.newInteger(~value);
    }

    public PyObject __int__() {
        return int___int__();
    }

    final PyInteger int___int__() {
        return this;
    }

    public PyLong __long__() {
        return int___long__();
    }

    final PyLong int___long__() {
        return new PyLong(value);
    }

    public PyFloat __float__() {
        return int___float__();
    }

    final PyFloat int___float__() {
        return new PyFloat((double)value);
    }

    public PyComplex __complex__() {
        return new PyComplex((double)value, 0.);
    }

    public PyString __oct__() {
        return int___oct__();
    }

    final PyString int___oct__() {
        if (value < 0) {
            return new PyString(
                "0"+Long.toString(0x100000000l+(long)value, 8));
        } else if (value > 0) {
            return new PyString("0"+Integer.toString(value, 8));
        } else
            return new PyString("0");
    }

    public PyString __hex__() {
        return int___hex__();
    }

    final PyString int___hex__() {
        if (value < 0) {
            return new PyString(
                "0x"+Long.toString(0x100000000l+(long)value, 16));
        } else {
            return new PyString("0x"+Integer.toString(value, 16));
        }
    }

    public boolean isMappingType() { return false; }
    public boolean isSequenceType() { return false; }

    public long asLong(int index) throws PyObject.ConversionException {
        return getValue();
    }

    public int asInt(int index) throws PyObject.ConversionException {
        return getValue();
    }

    /**
     * Used for pickling.
     *
     * @return a tuple of (class, (Integer))
     */
    public PyObject __reduce__() {
        return int___reduce__();
    }

    final PyObject int___reduce__() {
        return new PyTuple(new PyObject[]{
            getType(),
            new PyTuple(new PyObject[]{
                new PyInteger(getValue())
            })
        });
    }
}
