// Copyright (c) Corporation for National Research Initiatives
package org.python.core;

/**
 * A builtin python complex number
 */

public class PyComplex extends PyObject {
    public double real, imag;

    static PyComplex J = new PyComplex(0, 1.);

    //~ BEGIN GENERATED REGION -- DO NOT EDIT SEE gexpose.py
    /* type info */

    public static final String exposed_name="complex";

    public static void typeSetup(PyObject dict,PyType.Newstyle marker) {
        dict.__setitem__("imag",new PyGetSetDescr("imag",PyComplex.class,"getImag",null));
        dict.__setitem__("real",new PyGetSetDescr("real",PyComplex.class,"getReal",null));
        class exposed___abs__ extends PyBuiltinFunctionNarrow {

            private PyComplex self;

            public PyObject getSelf() {
                return self;
            }

            exposed___abs__(PyComplex self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___abs__((PyComplex)self,info);
            }

            public PyObject __call__() {
                return self.complex___abs__();
            }

            public PyObject inst_call(PyObject gself) {
                PyComplex self=(PyComplex)gself;
                return self.complex___abs__();
            }

        }
        dict.__setitem__("__abs__",new PyMethodDescr("__abs__",PyComplex.class,0,0,new exposed___abs__(null,null)));
        class exposed___float__ extends PyBuiltinFunctionNarrow {

            private PyComplex self;

            public PyObject getSelf() {
                return self;
            }

            exposed___float__(PyComplex self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___float__((PyComplex)self,info);
            }

            public PyObject __call__() {
                return self.complex___float__();
            }

            public PyObject inst_call(PyObject gself) {
                PyComplex self=(PyComplex)gself;
                return self.complex___float__();
            }

        }
        dict.__setitem__("__float__",new PyMethodDescr("__float__",PyComplex.class,0,0,new exposed___float__(null,null)));
        class exposed___int__ extends PyBuiltinFunctionNarrow {

            private PyComplex self;

            public PyObject getSelf() {
                return self;
            }

            exposed___int__(PyComplex self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___int__((PyComplex)self,info);
            }

            public PyObject __call__() {
                return self.complex___int__();
            }

            public PyObject inst_call(PyObject gself) {
                PyComplex self=(PyComplex)gself;
                return self.complex___int__();
            }

        }
        dict.__setitem__("__int__",new PyMethodDescr("__int__",PyComplex.class,0,0,new exposed___int__(null,null)));
        class exposed___long__ extends PyBuiltinFunctionNarrow {

            private PyComplex self;

            public PyObject getSelf() {
                return self;
            }

            exposed___long__(PyComplex self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___long__((PyComplex)self,info);
            }

            public PyObject __call__() {
                return self.complex___long__();
            }

            public PyObject inst_call(PyObject gself) {
                PyComplex self=(PyComplex)gself;
                return self.complex___long__();
            }

        }
        dict.__setitem__("__long__",new PyMethodDescr("__long__",PyComplex.class,0,0,new exposed___long__(null,null)));
        class exposed___neg__ extends PyBuiltinFunctionNarrow {

            private PyComplex self;

            public PyObject getSelf() {
                return self;
            }

            exposed___neg__(PyComplex self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___neg__((PyComplex)self,info);
            }

            public PyObject __call__() {
                return self.complex___neg__();
            }

            public PyObject inst_call(PyObject gself) {
                PyComplex self=(PyComplex)gself;
                return self.complex___neg__();
            }

        }
        dict.__setitem__("__neg__",new PyMethodDescr("__neg__",PyComplex.class,0,0,new exposed___neg__(null,null)));
        class exposed___pos__ extends PyBuiltinFunctionNarrow {

            private PyComplex self;

            public PyObject getSelf() {
                return self;
            }

            exposed___pos__(PyComplex self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___pos__((PyComplex)self,info);
            }

            public PyObject __call__() {
                return self.complex___pos__();
            }

            public PyObject inst_call(PyObject gself) {
                PyComplex self=(PyComplex)gself;
                return self.complex___pos__();
            }

        }
        dict.__setitem__("__pos__",new PyMethodDescr("__pos__",PyComplex.class,0,0,new exposed___pos__(null,null)));
        class exposed___add__ extends PyBuiltinFunctionNarrow {

            private PyComplex self;

            public PyObject getSelf() {
                return self;
            }

            exposed___add__(PyComplex self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___add__((PyComplex)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=self.complex___add__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyComplex self=(PyComplex)gself;
                PyObject ret=self.complex___add__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__add__",new PyMethodDescr("__add__",PyComplex.class,1,1,new exposed___add__(null,null)));
        class exposed___div__ extends PyBuiltinFunctionNarrow {

            private PyComplex self;

            public PyObject getSelf() {
                return self;
            }

            exposed___div__(PyComplex self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___div__((PyComplex)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=self.complex___div__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyComplex self=(PyComplex)gself;
                PyObject ret=self.complex___div__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__div__",new PyMethodDescr("__div__",PyComplex.class,1,1,new exposed___div__(null,null)));
        class exposed___divmod__ extends PyBuiltinFunctionNarrow {

            private PyComplex self;

            public PyObject getSelf() {
                return self;
            }

            exposed___divmod__(PyComplex self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___divmod__((PyComplex)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=self.complex___divmod__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyComplex self=(PyComplex)gself;
                PyObject ret=self.complex___divmod__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__divmod__",new PyMethodDescr("__divmod__",PyComplex.class,1,1,new exposed___divmod__(null,null)));
        class exposed___floordiv__ extends PyBuiltinFunctionNarrow {

            private PyComplex self;

            public PyObject getSelf() {
                return self;
            }

            exposed___floordiv__(PyComplex self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___floordiv__((PyComplex)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=self.complex___floordiv__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyComplex self=(PyComplex)gself;
                PyObject ret=self.complex___floordiv__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__floordiv__",new PyMethodDescr("__floordiv__",PyComplex.class,1,1,new exposed___floordiv__(null,null)));
        class exposed___mod__ extends PyBuiltinFunctionNarrow {

            private PyComplex self;

            public PyObject getSelf() {
                return self;
            }

            exposed___mod__(PyComplex self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___mod__((PyComplex)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=self.complex___mod__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyComplex self=(PyComplex)gself;
                PyObject ret=self.complex___mod__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__mod__",new PyMethodDescr("__mod__",PyComplex.class,1,1,new exposed___mod__(null,null)));
        class exposed___mul__ extends PyBuiltinFunctionNarrow {

            private PyComplex self;

            public PyObject getSelf() {
                return self;
            }

            exposed___mul__(PyComplex self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___mul__((PyComplex)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=self.complex___mul__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyComplex self=(PyComplex)gself;
                PyObject ret=self.complex___mul__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__mul__",new PyMethodDescr("__mul__",PyComplex.class,1,1,new exposed___mul__(null,null)));
        class exposed___radd__ extends PyBuiltinFunctionNarrow {

            private PyComplex self;

            public PyObject getSelf() {
                return self;
            }

            exposed___radd__(PyComplex self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___radd__((PyComplex)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=self.complex___radd__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyComplex self=(PyComplex)gself;
                PyObject ret=self.complex___radd__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__radd__",new PyMethodDescr("__radd__",PyComplex.class,1,1,new exposed___radd__(null,null)));
        class exposed___rdiv__ extends PyBuiltinFunctionNarrow {

            private PyComplex self;

            public PyObject getSelf() {
                return self;
            }

            exposed___rdiv__(PyComplex self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___rdiv__((PyComplex)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=self.complex___rdiv__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyComplex self=(PyComplex)gself;
                PyObject ret=self.complex___rdiv__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__rdiv__",new PyMethodDescr("__rdiv__",PyComplex.class,1,1,new exposed___rdiv__(null,null)));
        class exposed___rdivmod__ extends PyBuiltinFunctionNarrow {

            private PyComplex self;

            public PyObject getSelf() {
                return self;
            }

            exposed___rdivmod__(PyComplex self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___rdivmod__((PyComplex)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=self.complex___rdivmod__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyComplex self=(PyComplex)gself;
                PyObject ret=self.complex___rdivmod__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__rdivmod__",new PyMethodDescr("__rdivmod__",PyComplex.class,1,1,new exposed___rdivmod__(null,null)));
        class exposed___rfloordiv__ extends PyBuiltinFunctionNarrow {

            private PyComplex self;

            public PyObject getSelf() {
                return self;
            }

            exposed___rfloordiv__(PyComplex self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___rfloordiv__((PyComplex)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=self.complex___rfloordiv__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyComplex self=(PyComplex)gself;
                PyObject ret=self.complex___rfloordiv__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__rfloordiv__",new PyMethodDescr("__rfloordiv__",PyComplex.class,1,1,new exposed___rfloordiv__(null,null)));
        class exposed___rmod__ extends PyBuiltinFunctionNarrow {

            private PyComplex self;

            public PyObject getSelf() {
                return self;
            }

            exposed___rmod__(PyComplex self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___rmod__((PyComplex)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=self.complex___rmod__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyComplex self=(PyComplex)gself;
                PyObject ret=self.complex___rmod__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__rmod__",new PyMethodDescr("__rmod__",PyComplex.class,1,1,new exposed___rmod__(null,null)));
        class exposed___rmul__ extends PyBuiltinFunctionNarrow {

            private PyComplex self;

            public PyObject getSelf() {
                return self;
            }

            exposed___rmul__(PyComplex self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___rmul__((PyComplex)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=self.complex___rmul__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyComplex self=(PyComplex)gself;
                PyObject ret=self.complex___rmul__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__rmul__",new PyMethodDescr("__rmul__",PyComplex.class,1,1,new exposed___rmul__(null,null)));
        class exposed___rpow__ extends PyBuiltinFunctionNarrow {

            private PyComplex self;

            public PyObject getSelf() {
                return self;
            }

            exposed___rpow__(PyComplex self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___rpow__((PyComplex)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=self.complex___rpow__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyComplex self=(PyComplex)gself;
                PyObject ret=self.complex___rpow__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__rpow__",new PyMethodDescr("__rpow__",PyComplex.class,1,1,new exposed___rpow__(null,null)));
        class exposed___rsub__ extends PyBuiltinFunctionNarrow {

            private PyComplex self;

            public PyObject getSelf() {
                return self;
            }

            exposed___rsub__(PyComplex self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___rsub__((PyComplex)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=self.complex___rsub__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyComplex self=(PyComplex)gself;
                PyObject ret=self.complex___rsub__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__rsub__",new PyMethodDescr("__rsub__",PyComplex.class,1,1,new exposed___rsub__(null,null)));
        class exposed___rtruediv__ extends PyBuiltinFunctionNarrow {

            private PyComplex self;

            public PyObject getSelf() {
                return self;
            }

            exposed___rtruediv__(PyComplex self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___rtruediv__((PyComplex)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=self.complex___rtruediv__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyComplex self=(PyComplex)gself;
                PyObject ret=self.complex___rtruediv__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__rtruediv__",new PyMethodDescr("__rtruediv__",PyComplex.class,1,1,new exposed___rtruediv__(null,null)));
        class exposed___sub__ extends PyBuiltinFunctionNarrow {

            private PyComplex self;

            public PyObject getSelf() {
                return self;
            }

            exposed___sub__(PyComplex self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___sub__((PyComplex)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=self.complex___sub__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyComplex self=(PyComplex)gself;
                PyObject ret=self.complex___sub__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__sub__",new PyMethodDescr("__sub__",PyComplex.class,1,1,new exposed___sub__(null,null)));
        class exposed___truediv__ extends PyBuiltinFunctionNarrow {

            private PyComplex self;

            public PyObject getSelf() {
                return self;
            }

            exposed___truediv__(PyComplex self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___truediv__((PyComplex)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=self.complex___truediv__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyComplex self=(PyComplex)gself;
                PyObject ret=self.complex___truediv__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__truediv__",new PyMethodDescr("__truediv__",PyComplex.class,1,1,new exposed___truediv__(null,null)));
        class exposed___pow__ extends PyBuiltinFunctionNarrow {

            private PyComplex self;

            public PyObject getSelf() {
                return self;
            }

            exposed___pow__(PyComplex self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___pow__((PyComplex)self,info);
            }

            public PyObject __call__(PyObject arg0,PyObject arg1) {
                PyObject ret=self.complex___pow__(arg0,arg1);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

            public PyObject inst_call(PyObject gself,PyObject arg0,PyObject arg1) {
                PyComplex self=(PyComplex)gself;
                PyObject ret=self.complex___pow__(arg0,arg1);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=self.complex___pow__(arg0,null);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyComplex self=(PyComplex)gself;
                PyObject ret=self.complex___pow__(arg0,null);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__pow__",new PyMethodDescr("__pow__",PyComplex.class,1,2,new exposed___pow__(null,null)));
        class exposed_conjugate extends PyBuiltinFunctionNarrow {

            private PyComplex self;

            public PyObject getSelf() {
                return self;
            }

            exposed_conjugate(PyComplex self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed_conjugate((PyComplex)self,info);
            }

            public PyObject __call__() {
                return self.complex_conjugate();
            }

            public PyObject inst_call(PyObject gself) {
                PyComplex self=(PyComplex)gself;
                return self.complex_conjugate();
            }

        }
        dict.__setitem__("conjugate",new PyMethodDescr("conjugate",PyComplex.class,0,0,new exposed_conjugate(null,null)));
        class exposed___nonzero__ extends PyBuiltinFunctionNarrow {

            private PyComplex self;

            public PyObject getSelf() {
                return self;
            }

            exposed___nonzero__(PyComplex self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___nonzero__((PyComplex)self,info);
            }

            public PyObject __call__() {
                return Py.newBoolean(self.complex___nonzero__());
            }

            public PyObject inst_call(PyObject gself) {
                PyComplex self=(PyComplex)gself;
                return Py.newBoolean(self.complex___nonzero__());
            }

        }
        dict.__setitem__("__nonzero__",new PyMethodDescr("__nonzero__",PyComplex.class,0,0,new exposed___nonzero__(null,null)));
        class exposed___repr__ extends PyBuiltinFunctionNarrow {

            private PyComplex self;

            public PyObject getSelf() {
                return self;
            }

            exposed___repr__(PyComplex self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___repr__((PyComplex)self,info);
            }

            public PyObject __call__() {
                return new PyString(self.complex_toString());
            }

            public PyObject inst_call(PyObject gself) {
                PyComplex self=(PyComplex)gself;
                return new PyString(self.complex_toString());
            }

        }
        dict.__setitem__("__repr__",new PyMethodDescr("__repr__",PyComplex.class,0,0,new exposed___repr__(null,null)));
        class exposed___str__ extends PyBuiltinFunctionNarrow {

            private PyComplex self;

            public PyObject getSelf() {
                return self;
            }

            exposed___str__(PyComplex self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___str__((PyComplex)self,info);
            }

            public PyObject __call__() {
                return new PyString(self.complex_toString());
            }

            public PyObject inst_call(PyObject gself) {
                PyComplex self=(PyComplex)gself;
                return new PyString(self.complex_toString());
            }

        }
        dict.__setitem__("__str__",new PyMethodDescr("__str__",PyComplex.class,0,0,new exposed___str__(null,null)));
        class exposed___hash__ extends PyBuiltinFunctionNarrow {

            private PyComplex self;

            public PyObject getSelf() {
                return self;
            }

            exposed___hash__(PyComplex self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___hash__((PyComplex)self,info);
            }

            public PyObject __call__() {
                return Py.newInteger(self.complex_hashCode());
            }

            public PyObject inst_call(PyObject gself) {
                PyComplex self=(PyComplex)gself;
                return Py.newInteger(self.complex_hashCode());
            }

        }
        dict.__setitem__("__hash__",new PyMethodDescr("__hash__",PyComplex.class,0,0,new exposed___hash__(null,null)));
        dict.__setitem__("__new__",new PyNewWrapper(PyComplex.class,"__new__",-1,-1) {

                                                                                         public PyObject new_impl(boolean init,PyType subtype,PyObject[]args,String[]keywords) {
                                                                                             return complex_new(this,init,subtype,args,keywords);
                                                                                         }

                                                                                     });
    }
    //~ END GENERATED REGION -- DO NOT EDIT SEE gexpose.py

    public static PyObject complex_new(PyNewWrapper new_, boolean init, PyType subtype,
            PyObject[] args, String[] keywords) {
        if (args.length == 0) {
            if (new_.for_type == subtype) {
                return new PyComplex(0, 0);
            }
            return new PyComplexDerived(subtype, 0, 0);
        }

        if (args.length > 2)
            throw Py.TypeError("complex() "+"takes at most 2 arguments (" +
                               args.length + " given)");

        // optimize complex(int, int) here?

        ArgParser ap = new ArgParser("complex", args, keywords, "real", "imag");
        PyObject real = ap.getPyObject(0, Py.Zero);
        PyObject imag = ap.getPyObject(1, null);

        if (imag != null) {
            if (real instanceof PyString)
                throw Py.TypeError("complex() " +
                        "can't take second arg if first is a string");
            if (imag instanceof PyString)
                throw Py.TypeError("complex() " +
                        "second arg can't be a string");
        }

        PyComplex ret = null;
        try {
            ret = real.__complex__();
        } catch (PyException pye) {
            // i.e PyString.__complex__ throws ValueError
            if (!(Py.matchException(pye, Py.AttributeError))) throw pye;
        }

        try {
            if (ret == null)
                ret = new PyComplex(real.__float__().getValue(), 0);
            if (imag != null) {
                if (ret == real)
                    ret = new PyComplex(ret.real, ret.imag);
                if (imag instanceof PyComplex) {
                    // optimize away __mul__()
                    // IMO only allowed on pure PyComplex objects, but CPython
                    // does it on all complex subtypes, so I do too.
                    PyComplex c = (PyComplex) imag;
                    ret.real -= c.imag;
                    ret.imag += c.real;
                } else {
                    // CPython doesn't call __complex__ on second argument
                    ret.imag += imag.__float__().getValue();
                }
            }
            if (new_.for_type == subtype) {
                return ret;
            } else {
                return new PyComplexDerived(subtype, ret.real, ret.imag);
            }
        } catch (PyException pye) {
            // convert all AttributeErrors except on PyInstance to TypeError
            if (Py.matchException(pye, Py.AttributeError)) {
                Object o = (ret == null ? real : imag);
                if (!(o instanceof PyInstance))
                    throw Py.TypeError("complex() " +
                            "argument must be a string or a number");
            }
            throw pye;
        }
    }

    private static final PyType COMPLEXTYPE = PyType.fromClass(PyComplex.class);

    public PyComplex(PyType subtype, double r, double i) {
        super(subtype);
        real = r;
        imag = i;
    }

    public PyComplex(double r, double i) {
        this(COMPLEXTYPE, r, i);
    }

    public String safeRepr() throws PyIgnoreMethodTag {
        return "'complex' object";
    }

    public final PyFloat getReal() {
        return Py.newFloat(real);
    }

    public final PyFloat getImag() {
        return Py.newFloat(imag);
    }

    public static String toString(double value) {
        if (value == Math.floor(value) &&
               value <= Long.MAX_VALUE && value >= Long.MIN_VALUE) {
            return Long.toString((long)value);
        } else {
            return Double.toString(value);
        }
    }

    public String toString() {
        return complex_toString();
    }

    final String complex_toString() {
        if (real == 0.) {
            return toString(imag)+"j";
        } else {
            if (imag >= 0) {
                return "("+toString(real)+"+"+toString(imag)+"j)";
            } else {
                return "("+toString(real)+"-"+toString(-imag)+"j)";
            }
        }
    }

    public int hashCode() {
        return complex_hashCode();
    }

    final int complex_hashCode() {
        if (imag == 0) {
            return new PyFloat(real).hashCode();
        } else {
            long v = Double.doubleToLongBits(real) ^
                Double.doubleToLongBits(imag);
            return (int)v ^ (int)(v >> 32);
        }
    }

    public boolean __nonzero__() {
        return complex___nonzero__();
    }

    final boolean complex___nonzero__() {
        return real != 0 && imag != 0;
    }

    /*public Object __tojava__(Class c) {
      return super.__tojava__(c);
      }*/

    public int __cmp__(PyObject other) {
        return complex___cmp__(other);
    }

    final int complex___cmp__(PyObject other) {
        if (!canCoerce(other))
            return -2;
        PyComplex c = coerce(other);
        double oreal = c.real;
        double oimag = c.imag;
        if (real == oreal && imag == oimag)
            return 0;
        if (real != oreal) {
            return real < oreal ? -1 : 1;
        } else {
            return imag < oimag ? -1 : 1;
        }
    }

    /*
     * @see org.python.core.PyObject#__eq__(org.python.core.PyObject)
     */
    public PyObject __eq__(PyObject other) {
        return complex___eq__(other);
    }

    final PyObject complex___eq__(PyObject other) {
        if (!canCoerce(other))
            return null;
        PyComplex c = coerce(other);
        return Py.newBoolean(real == c.real && imag == c.imag);
    }

    /*
     * @see org.python.core.PyObject#__ne__(org.python.core.PyObject)
     */
    public PyObject __ne__(PyObject other) {
        return complex___ne__(other);
    }

    final PyObject complex___ne__(PyObject other) {
        if (!canCoerce(other))
            return null;
        PyComplex c = coerce(other);
        return Py.newBoolean(real != c.real || imag != c.imag);
    }

    private PyObject unsupported_comparison(PyObject other) {
        if (!canCoerce(other))
            return null;
        throw Py.TypeError("cannot compare complex numbers using <, <=, >, >=");
    }

    public PyObject __ge__(PyObject other) {
        return complex___ge__(other);
    }

    final PyObject complex___ge__(PyObject other) {
        return unsupported_comparison(other);
    }

    public PyObject __gt__(PyObject other) {
        return complex___gt__(other);
    }

    final PyObject complex___gt__(PyObject other) {
        return unsupported_comparison(other);
    }

    public PyObject __le__(PyObject other) {
        return complex___le__(other);
    }

    final PyObject complex___le__(PyObject other) {
        return unsupported_comparison(other);
    }

    public PyObject __lt__(PyObject other) {
        return complex___lt__(other);
    }

    final PyObject complex___lt__(PyObject other) {
        return unsupported_comparison(other);
    }


    public Object __coerce_ex__(PyObject other) {
        if (other instanceof PyComplex)
            return other;
        if (other instanceof PyFloat)
            return new PyComplex(((PyFloat)other).getValue(), 0);
        if (other instanceof PyInteger)
            return new PyComplex((double)((PyInteger)other).getValue(), 0);
        if (other instanceof PyLong)
            return new PyComplex(((PyLong)other).doubleValue(), 0);
        return Py.None;
    }

    private final boolean canCoerce(PyObject other) {
        return other instanceof PyComplex ||
               other instanceof PyFloat ||
               other instanceof PyInteger ||
               other instanceof PyLong;
    }

    private final PyComplex coerce(PyObject other) {
        if (other instanceof PyComplex)
            return (PyComplex) other;
        if (other instanceof PyFloat)
            return new PyComplex(((PyFloat)other).getValue(), 0);
        if (other instanceof PyInteger)
            return new PyComplex((double)((PyInteger)other).getValue(), 0);
        if (other instanceof PyLong)
            return new PyComplex(((PyLong)other).doubleValue(), 0);
        throw Py.TypeError("xxx");
    }

    public PyObject __add__(PyObject right) {
        return complex___add__(right);
    }

    final PyObject complex___add__(PyObject right) {
        if (!canCoerce(right))
            return null;
        PyComplex c = coerce(right);
        return new PyComplex(real+c.real, imag+c.imag);
    }

    public PyObject __radd__(PyObject left) {
        return complex___radd__(left);
    }

    final PyObject complex___radd__(PyObject left) {
        return __add__(left);
    }

    private final static PyObject _sub(PyComplex o1, PyComplex o2) {
        return new PyComplex(o1.real-o2.real, o1.imag-o2.imag);
    }

    public PyObject __sub__(PyObject right) {
        return complex___sub__(right);
    }

    final PyObject complex___sub__(PyObject right) {
        if (!canCoerce(right))
            return null;
        return _sub(this, coerce(right));
    }

    public PyObject __rsub__(PyObject left) {
        return complex___rsub__(left);
    }

    final PyObject complex___rsub__(PyObject left) {
        if (!canCoerce(left))
            return null;
        return _sub(coerce(left), this);
    }

    private final static PyObject _mul(PyComplex o1, PyComplex o2) {
        return new PyComplex(o1.real*o2.real-o1.imag*o2.imag,
                             o1.real*o2.imag+o1.imag*o2.real);
    }

    public PyObject __mul__(PyObject right) {
        return complex___mul__(right);
    }

    final PyObject complex___mul__(PyObject right) {
        if (!canCoerce(right))
            return null;
        return _mul(this, coerce(right));
    }

    public PyObject __rmul__(PyObject left) {
        return complex___rmul__(left);
    }

    final PyObject complex___rmul__(PyObject left) {
        if (!canCoerce(left))
            return null;
        return _mul(coerce(left), this);
    }

    private final static PyObject _div(PyComplex a, PyComplex b) {
        double abs_breal = b.real < 0 ? -b.real : b.real;
        double abs_bimag = b.imag < 0 ? -b.imag : b.imag;
        if (abs_breal >= abs_bimag) {
            // Divide tops and bottom by b.real
            if (abs_breal == 0.0) {
                throw Py.ZeroDivisionError("complex division");
            }
            double ratio = b.imag / b.real;
            double denom = b.real + b.imag * ratio;
            return new PyComplex((a.real + a.imag * ratio) / denom,
                                 (a.imag - a.real * ratio) / denom);
        } else {
            /* divide tops and bottom by b.imag */
            double ratio = b.real / b.imag;
            double denom = b.real * ratio + b.imag;
            return new PyComplex((a.real * ratio + a.imag) / denom,
                                 (a.imag * ratio - a.real) / denom);
        }
    }

    public PyObject __div__(PyObject right) {
        return complex___div__(right);
    }

    final PyObject complex___div__(PyObject right) {
        if (!canCoerce(right))
            return null;
        if (Options.divisionWarning >= 2)
            Py.warning(Py.DeprecationWarning, "classic complex division");
        return _div(this, coerce(right));
    }

    public PyObject __rdiv__(PyObject left) {
        return complex___rdiv__(left);
    }

    final PyObject complex___rdiv__(PyObject left) {
        if (!canCoerce(left))
            return null;
        if (Options.divisionWarning >= 2)
            Py.warning(Py.DeprecationWarning, "classic complex division");
        return _div(coerce(left), this);
    }

    public PyObject __floordiv__(PyObject right) {
        return complex___floordiv__(right);
    }

    final PyObject complex___floordiv__(PyObject right) {
        if (!canCoerce(right))
            return null;
        return _divmod(this, coerce(right)).__finditem__(0);
    }

    public PyObject __rfloordiv__(PyObject left) {
        return complex___floordiv__(left);
    }

    final PyObject complex___rfloordiv__(PyObject left) {
        if (!canCoerce(left))
            return null;
        return _divmod(coerce(left), this).__finditem__(0);
    }

    public PyObject __truediv__(PyObject right) {
        return complex___truediv__(right);
    }

    final PyObject complex___truediv__(PyObject right) {
        if (!canCoerce(right))
            return null;
        return _div(this, coerce(right));
    }

    public PyObject __rtruediv__(PyObject left) {
        return complex___rtruediv__(left);
    }

    final PyObject complex___rtruediv__(PyObject left) {
        if (!canCoerce(left))
            return null;
        return _div(coerce(left), this);
    }

    public PyObject __mod__(PyObject right) {
        return complex___mod__(right);
    }

    final PyObject complex___mod__(PyObject right) {
        if (!canCoerce(right))
            return null;
        return _mod(this, coerce(right));
    }

    public PyObject __rmod__(PyObject left) {
        return complex___rmod__(left);
    }

    final PyObject complex___rmod__(PyObject left) {
        if (!canCoerce(left))
            return null;
        return _mod(coerce(left), this);
    }

    private static PyObject _mod(PyComplex value, PyComplex right) {
        PyComplex z = (PyComplex) _div(value, right);

        z.real = Math.floor(z.real);
        z.imag = 0.0;

        return value.__sub__(z.__mul__(right));
    }

    public PyObject __divmod__(PyObject right) {
        return complex___divmod__(right);
    }

    final PyObject complex___divmod__(PyObject right) {
        if (!canCoerce(right))
            return null;
        return _divmod(this, coerce(right));
    }

    public PyObject __rdivmod__(PyObject left) {
        return complex___rdivmod__(left);
    }

    final PyObject complex___rdivmod__(PyObject left) {
        if (!canCoerce(left))
            return null;
        return _divmod(coerce(left), this);
    }

    private static PyObject _divmod(PyComplex value, PyComplex right) {
        PyComplex z = (PyComplex) _div(value, right);

        z.real = Math.floor(z.real);
        z.imag = 0.0;

        return new PyTuple(new PyObject[]
               { z, value.__sub__(z.__mul__(right))});
    }


    private static PyObject ipow(PyComplex value, int iexp) {
        int pow = iexp;
        if (pow < 0) pow = -pow;

        double xr = value.real;
        double xi = value.imag;

        double zr = 1;
        double zi = 0;

        double tmp;

        while (pow > 0) {
            if ((pow & 0x1) != 0) {
                tmp = zr*xr - zi*xi;
                zi = zi*xr + zr*xi;
                zr = tmp;
            }
            pow >>= 1;
            if (pow == 0)
                break;
            tmp = xr*xr - xi*xi;
            xi = xr*xi*2;
            xr = tmp;
        }

        PyComplex ret = new PyComplex(zr, zi);

        if (iexp < 0)
            return new PyComplex(1,0).__div__(ret);
        return ret;
    }

    public PyObject __pow__(PyObject right, PyObject modulo) {
        return complex___pow__(right, modulo);
    }

    final PyObject complex___pow__(PyObject right, PyObject modulo) {
        if (modulo != null) {
            throw Py.ValueError("complex modulo");
        }
        if (!canCoerce(right))
            return null;
        return _pow(this, coerce(right));
    }

    public PyObject __rpow__(PyObject left) {
        return complex___rpow__(left);
    }

    final PyObject complex___rpow__(PyObject left) {
        if (!canCoerce(left))
            return null;
        return _pow(coerce(left), this);
    }

    public static PyObject _pow(PyComplex value, PyComplex right) {
        double xr = value.real;
        double xi = value.imag;
        double yr = right.real;
        double yi = right.imag;

        if (yr == 0 && yi == 0) {
            return new PyComplex(1, 0);
        }

        if (xr == 0 && xi == 0) {
            if (yi != 0 || yr < 0) {
                throw Py.ValueError("0.0 to a negative or complex power");
            }
        }

        // Check for integral powers
        int iexp = (int)yr;
        if (yi == 0 && yr == (double)iexp && iexp >= -128 && iexp <= 128) {
            return ipow(value, iexp);
        }

        double abs = ExtraMath.hypot(xr, xi);
        double len = Math.pow(abs, yr);

        double at = Math.atan2(xi, xr);
        double phase = at*yr;
        if (yi != 0) {
            len /= Math.exp(at*yi);
            phase += yi*Math.log(abs);
        }
        return new PyComplex(len*Math.cos(phase), len*Math.sin(phase));
    }

    public PyObject __neg__() {
        return complex___neg__();
    }

    final PyObject complex___neg__() {
        return new PyComplex(-real, -imag);
    }

    public PyObject __pos__() {
        return complex___pos__();
    }

    final PyObject complex___pos__() {
        return this;
    }

    public PyObject __invert__() {
      throw Py.TypeError("bad operand type for unary ~");
    }

    public PyObject __abs__() {
        return complex___abs__();
    }

    final PyObject complex___abs__() {
        return new PyFloat(ExtraMath.hypot(real, imag));
    }

    public PyObject __int__() {
        return complex___int__();
    }

    final PyInteger complex___int__() {
        throw Py.TypeError(
            "can't convert complex to int; use e.g. int(abs(z))");
    }

    public PyLong __long__() {
        return complex___long__();
    }

    final PyLong complex___long__() {
        throw Py.TypeError(
            "can't convert complex to long; use e.g. long(abs(z))");
    }

    public PyFloat __float__() {
        return complex___float__();
    }

    final PyFloat complex___float__() {
        throw Py.TypeError("can't convert complex to float; use e.g. abs(z)");
    }

    public PyComplex __complex__() {
        return this;
    }

    public PyComplex conjugate() {
        return complex_conjugate();
    }

    final PyComplex complex_conjugate() {
        return new PyComplex(real, -imag);
    }

    public boolean isMappingType() { return false; }
    public boolean isSequenceType() { return false; }
}
