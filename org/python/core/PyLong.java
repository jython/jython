// Copyright (c) Corporation for National Research Initiatives
package org.python.core;

import java.math.BigInteger;
import java.io.Serializable;

/**
 * A builtin python long. This is implemented as a
 * java.math.BigInteger.
 */

public class PyLong extends PyObject
{
    public static final BigInteger minLong =
        BigInteger.valueOf(Long.MIN_VALUE);
    public static final BigInteger maxLong =
        BigInteger.valueOf(Long.MAX_VALUE);
    public static final BigInteger maxULong =
        BigInteger.valueOf(1).shiftLeft(64).subtract(BigInteger.valueOf(1));

    /* type info */

    public static final String exposed_name="long";

    public static void typeSetup(PyObject dict,PyType.Newstyle marker) {
        class exposed___abs__ extends PyBuiltinFunctionNarrow {

            private PyLong self;

            public PyObject getSelf() {
                return self;
            }

            exposed___abs__(PyLong self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___abs__((PyLong)self,info);
            }

            public PyObject __call__() {
                return self.long___abs__();
            }

            public PyObject inst_call(PyObject gself) {
                PyLong self=(PyLong)gself;
                return self.long___abs__();
            }

        }
        dict.__setitem__("__abs__",new PyMethodDescr("__abs__",PyLong.class,0,0,new exposed___abs__(null,null)));
        class exposed___float__ extends PyBuiltinFunctionNarrow {

            private PyLong self;

            public PyObject getSelf() {
                return self;
            }

            exposed___float__(PyLong self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___float__((PyLong)self,info);
            }

            public PyObject __call__() {
                return self.long___float__();
            }

            public PyObject inst_call(PyObject gself) {
                PyLong self=(PyLong)gself;
                return self.long___float__();
            }

        }
        dict.__setitem__("__float__",new PyMethodDescr("__float__",PyLong.class,0,0,new exposed___float__(null,null)));
        class exposed___hex__ extends PyBuiltinFunctionNarrow {

            private PyLong self;

            public PyObject getSelf() {
                return self;
            }

            exposed___hex__(PyLong self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___hex__((PyLong)self,info);
            }

            public PyObject __call__() {
                return self.long___hex__();
            }

            public PyObject inst_call(PyObject gself) {
                PyLong self=(PyLong)gself;
                return self.long___hex__();
            }

        }
        dict.__setitem__("__hex__",new PyMethodDescr("__hex__",PyLong.class,0,0,new exposed___hex__(null,null)));
        class exposed___int__ extends PyBuiltinFunctionNarrow {

            private PyLong self;

            public PyObject getSelf() {
                return self;
            }

            exposed___int__(PyLong self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___int__((PyLong)self,info);
            }

            public PyObject __call__() {
                return self.long___int__();
            }

            public PyObject inst_call(PyObject gself) {
                PyLong self=(PyLong)gself;
                return self.long___int__();
            }

        }
        dict.__setitem__("__int__",new PyMethodDescr("__int__",PyLong.class,0,0,new exposed___int__(null,null)));
        class exposed___invert__ extends PyBuiltinFunctionNarrow {

            private PyLong self;

            public PyObject getSelf() {
                return self;
            }

            exposed___invert__(PyLong self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___invert__((PyLong)self,info);
            }

            public PyObject __call__() {
                return self.long___invert__();
            }

            public PyObject inst_call(PyObject gself) {
                PyLong self=(PyLong)gself;
                return self.long___invert__();
            }

        }
        dict.__setitem__("__invert__",new PyMethodDescr("__invert__",PyLong.class,0,0,new exposed___invert__(null,null)));
        class exposed___long__ extends PyBuiltinFunctionNarrow {

            private PyLong self;

            public PyObject getSelf() {
                return self;
            }

            exposed___long__(PyLong self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___long__((PyLong)self,info);
            }

            public PyObject __call__() {
                return self.long___long__();
            }

            public PyObject inst_call(PyObject gself) {
                PyLong self=(PyLong)gself;
                return self.long___long__();
            }

        }
        dict.__setitem__("__long__",new PyMethodDescr("__long__",PyLong.class,0,0,new exposed___long__(null,null)));
        class exposed___neg__ extends PyBuiltinFunctionNarrow {

            private PyLong self;

            public PyObject getSelf() {
                return self;
            }

            exposed___neg__(PyLong self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___neg__((PyLong)self,info);
            }

            public PyObject __call__() {
                return self.long___neg__();
            }

            public PyObject inst_call(PyObject gself) {
                PyLong self=(PyLong)gself;
                return self.long___neg__();
            }

        }
        dict.__setitem__("__neg__",new PyMethodDescr("__neg__",PyLong.class,0,0,new exposed___neg__(null,null)));
        class exposed___oct__ extends PyBuiltinFunctionNarrow {

            private PyLong self;

            public PyObject getSelf() {
                return self;
            }

            exposed___oct__(PyLong self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___oct__((PyLong)self,info);
            }

            public PyObject __call__() {
                return self.long___oct__();
            }

            public PyObject inst_call(PyObject gself) {
                PyLong self=(PyLong)gself;
                return self.long___oct__();
            }

        }
        dict.__setitem__("__oct__",new PyMethodDescr("__oct__",PyLong.class,0,0,new exposed___oct__(null,null)));
        class exposed___pos__ extends PyBuiltinFunctionNarrow {

            private PyLong self;

            public PyObject getSelf() {
                return self;
            }

            exposed___pos__(PyLong self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___pos__((PyLong)self,info);
            }

            public PyObject __call__() {
                return self.long___pos__();
            }

            public PyObject inst_call(PyObject gself) {
                PyLong self=(PyLong)gself;
                return self.long___pos__();
            }

        }
        dict.__setitem__("__pos__",new PyMethodDescr("__pos__",PyLong.class,0,0,new exposed___pos__(null,null)));
        class exposed___add__ extends PyBuiltinFunctionNarrow {

            private PyLong self;

            public PyObject getSelf() {
                return self;
            }

            exposed___add__(PyLong self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___add__((PyLong)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=self.long___add__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyLong self=(PyLong)gself;
                PyObject ret=self.long___add__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__add__",new PyMethodDescr("__add__",PyLong.class,1,1,new exposed___add__(null,null)));
        class exposed___and__ extends PyBuiltinFunctionNarrow {

            private PyLong self;

            public PyObject getSelf() {
                return self;
            }

            exposed___and__(PyLong self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___and__((PyLong)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=self.long___and__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyLong self=(PyLong)gself;
                PyObject ret=self.long___and__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__and__",new PyMethodDescr("__and__",PyLong.class,1,1,new exposed___and__(null,null)));
        class exposed___div__ extends PyBuiltinFunctionNarrow {

            private PyLong self;

            public PyObject getSelf() {
                return self;
            }

            exposed___div__(PyLong self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___div__((PyLong)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=self.long___div__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyLong self=(PyLong)gself;
                PyObject ret=self.long___div__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__div__",new PyMethodDescr("__div__",PyLong.class,1,1,new exposed___div__(null,null)));
        class exposed___divmod__ extends PyBuiltinFunctionNarrow {

            private PyLong self;

            public PyObject getSelf() {
                return self;
            }

            exposed___divmod__(PyLong self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___divmod__((PyLong)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=self.long___divmod__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyLong self=(PyLong)gself;
                PyObject ret=self.long___divmod__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__divmod__",new PyMethodDescr("__divmod__",PyLong.class,1,1,new exposed___divmod__(null,null)));
        class exposed___floordiv__ extends PyBuiltinFunctionNarrow {

            private PyLong self;

            public PyObject getSelf() {
                return self;
            }

            exposed___floordiv__(PyLong self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___floordiv__((PyLong)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=self.long___floordiv__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyLong self=(PyLong)gself;
                PyObject ret=self.long___floordiv__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__floordiv__",new PyMethodDescr("__floordiv__",PyLong.class,1,1,new exposed___floordiv__(null,null)));
        class exposed___lshift__ extends PyBuiltinFunctionNarrow {

            private PyLong self;

            public PyObject getSelf() {
                return self;
            }

            exposed___lshift__(PyLong self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___lshift__((PyLong)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=self.long___lshift__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyLong self=(PyLong)gself;
                PyObject ret=self.long___lshift__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__lshift__",new PyMethodDescr("__lshift__",PyLong.class,1,1,new exposed___lshift__(null,null)));
        class exposed___mod__ extends PyBuiltinFunctionNarrow {

            private PyLong self;

            public PyObject getSelf() {
                return self;
            }

            exposed___mod__(PyLong self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___mod__((PyLong)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=self.long___mod__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyLong self=(PyLong)gself;
                PyObject ret=self.long___mod__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__mod__",new PyMethodDescr("__mod__",PyLong.class,1,1,new exposed___mod__(null,null)));
        class exposed___mul__ extends PyBuiltinFunctionNarrow {

            private PyLong self;

            public PyObject getSelf() {
                return self;
            }

            exposed___mul__(PyLong self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___mul__((PyLong)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=self.long___mul__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyLong self=(PyLong)gself;
                PyObject ret=self.long___mul__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__mul__",new PyMethodDescr("__mul__",PyLong.class,1,1,new exposed___mul__(null,null)));
        class exposed___or__ extends PyBuiltinFunctionNarrow {

            private PyLong self;

            public PyObject getSelf() {
                return self;
            }

            exposed___or__(PyLong self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___or__((PyLong)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=self.long___or__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyLong self=(PyLong)gself;
                PyObject ret=self.long___or__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__or__",new PyMethodDescr("__or__",PyLong.class,1,1,new exposed___or__(null,null)));
        class exposed___radd__ extends PyBuiltinFunctionNarrow {

            private PyLong self;

            public PyObject getSelf() {
                return self;
            }

            exposed___radd__(PyLong self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___radd__((PyLong)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=self.long___radd__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyLong self=(PyLong)gself;
                PyObject ret=self.long___radd__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__radd__",new PyMethodDescr("__radd__",PyLong.class,1,1,new exposed___radd__(null,null)));
        class exposed___rdiv__ extends PyBuiltinFunctionNarrow {

            private PyLong self;

            public PyObject getSelf() {
                return self;
            }

            exposed___rdiv__(PyLong self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___rdiv__((PyLong)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=self.long___rdiv__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyLong self=(PyLong)gself;
                PyObject ret=self.long___rdiv__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__rdiv__",new PyMethodDescr("__rdiv__",PyLong.class,1,1,new exposed___rdiv__(null,null)));
        class exposed___rfloordiv__ extends PyBuiltinFunctionNarrow {

            private PyLong self;

            public PyObject getSelf() {
                return self;
            }

            exposed___rfloordiv__(PyLong self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___rfloordiv__((PyLong)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=self.long___rfloordiv__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyLong self=(PyLong)gself;
                PyObject ret=self.long___rfloordiv__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__rfloordiv__",new PyMethodDescr("__rfloordiv__",PyLong.class,1,1,new exposed___rfloordiv__(null,null)));
        class exposed___rmod__ extends PyBuiltinFunctionNarrow {

            private PyLong self;

            public PyObject getSelf() {
                return self;
            }

            exposed___rmod__(PyLong self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___rmod__((PyLong)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=self.long___rmod__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyLong self=(PyLong)gself;
                PyObject ret=self.long___rmod__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__rmod__",new PyMethodDescr("__rmod__",PyLong.class,1,1,new exposed___rmod__(null,null)));
        class exposed___rmul__ extends PyBuiltinFunctionNarrow {

            private PyLong self;

            public PyObject getSelf() {
                return self;
            }

            exposed___rmul__(PyLong self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___rmul__((PyLong)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=self.long___rmul__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyLong self=(PyLong)gself;
                PyObject ret=self.long___rmul__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__rmul__",new PyMethodDescr("__rmul__",PyLong.class,1,1,new exposed___rmul__(null,null)));
        class exposed___rshift__ extends PyBuiltinFunctionNarrow {

            private PyLong self;

            public PyObject getSelf() {
                return self;
            }

            exposed___rshift__(PyLong self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___rshift__((PyLong)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=self.long___rshift__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyLong self=(PyLong)gself;
                PyObject ret=self.long___rshift__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__rshift__",new PyMethodDescr("__rshift__",PyLong.class,1,1,new exposed___rshift__(null,null)));
        class exposed___rsub__ extends PyBuiltinFunctionNarrow {

            private PyLong self;

            public PyObject getSelf() {
                return self;
            }

            exposed___rsub__(PyLong self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___rsub__((PyLong)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=self.long___rsub__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyLong self=(PyLong)gself;
                PyObject ret=self.long___rsub__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__rsub__",new PyMethodDescr("__rsub__",PyLong.class,1,1,new exposed___rsub__(null,null)));
        class exposed___rtruediv__ extends PyBuiltinFunctionNarrow {

            private PyLong self;

            public PyObject getSelf() {
                return self;
            }

            exposed___rtruediv__(PyLong self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___rtruediv__((PyLong)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=self.long___rtruediv__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyLong self=(PyLong)gself;
                PyObject ret=self.long___rtruediv__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__rtruediv__",new PyMethodDescr("__rtruediv__",PyLong.class,1,1,new exposed___rtruediv__(null,null)));
        class exposed___sub__ extends PyBuiltinFunctionNarrow {

            private PyLong self;

            public PyObject getSelf() {
                return self;
            }

            exposed___sub__(PyLong self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___sub__((PyLong)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=self.long___sub__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyLong self=(PyLong)gself;
                PyObject ret=self.long___sub__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__sub__",new PyMethodDescr("__sub__",PyLong.class,1,1,new exposed___sub__(null,null)));
        class exposed___truediv__ extends PyBuiltinFunctionNarrow {

            private PyLong self;

            public PyObject getSelf() {
                return self;
            }

            exposed___truediv__(PyLong self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___truediv__((PyLong)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=self.long___truediv__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyLong self=(PyLong)gself;
                PyObject ret=self.long___truediv__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__truediv__",new PyMethodDescr("__truediv__",PyLong.class,1,1,new exposed___truediv__(null,null)));
        class exposed___xor__ extends PyBuiltinFunctionNarrow {

            private PyLong self;

            public PyObject getSelf() {
                return self;
            }

            exposed___xor__(PyLong self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___xor__((PyLong)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=self.long___xor__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyLong self=(PyLong)gself;
                PyObject ret=self.long___xor__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__xor__",new PyMethodDescr("__xor__",PyLong.class,1,1,new exposed___xor__(null,null)));
        class exposed___cmp__ extends PyBuiltinFunctionNarrow {

            private PyLong self;

            public PyObject getSelf() {
                return self;
            }

            exposed___cmp__(PyLong self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___cmp__((PyLong)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                int ret=self.long___cmp__(arg0);
                if (ret==-2) {
                    throw Py.TypeError("long"+".__cmp__(x,y) requires y to be '"+"long"+"', not a '"+(arg0).getType().fastGetName()+"'");
                }
                return Py.newInteger(ret);
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyLong self=(PyLong)gself;
                int ret=self.long___cmp__(arg0);
                if (ret==-2) {
                    throw Py.TypeError("long"+".__cmp__(x,y) requires y to be '"+"long"+"', not a '"+(arg0).getType().fastGetName()+"'");
                }
                return Py.newInteger(ret);
            }

        }
        dict.__setitem__("__cmp__",new PyMethodDescr("__cmp__",PyLong.class,1,1,new exposed___cmp__(null,null)));
        class exposed___pow__ extends PyBuiltinFunctionNarrow {

            private PyLong self;

            public PyObject getSelf() {
                return self;
            }

            exposed___pow__(PyLong self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___pow__((PyLong)self,info);
            }

            public PyObject __call__(PyObject arg0,PyObject arg1) {
                PyObject ret=self.long___pow__(arg0,arg1);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

            public PyObject inst_call(PyObject gself,PyObject arg0,PyObject arg1) {
                PyLong self=(PyLong)gself;
                PyObject ret=self.long___pow__(arg0,arg1);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=self.long___pow__(arg0,null);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyLong self=(PyLong)gself;
                PyObject ret=self.long___pow__(arg0,null);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__pow__",new PyMethodDescr("__pow__",PyLong.class,1,2,new exposed___pow__(null,null)));
        class exposed___repr__ extends PyBuiltinFunctionNarrow {

            private PyLong self;

            public PyObject getSelf() {
                return self;
            }

            exposed___repr__(PyLong self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___repr__((PyLong)self,info);
            }

            public PyObject __call__() {
                return new PyString(self.long_toString());
            }

            public PyObject inst_call(PyObject gself) {
                PyLong self=(PyLong)gself;
                return new PyString(self.long_toString());
            }

        }
        dict.__setitem__("__repr__",new PyMethodDescr("__repr__",PyLong.class,0,0,new exposed___repr__(null,null)));
        class exposed___str__ extends PyBuiltinFunctionNarrow {

            private PyLong self;

            public PyObject getSelf() {
                return self;
            }

            exposed___str__(PyLong self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___str__((PyLong)self,info);
            }

            public PyObject __call__() {
                return new PyString(self.long_toString());
            }

            public PyObject inst_call(PyObject gself) {
                PyLong self=(PyLong)gself;
                return new PyString(self.long_toString());
            }

        }
        dict.__setitem__("__str__",new PyMethodDescr("__str__",PyLong.class,0,0,new exposed___str__(null,null)));
        class exposed___hash__ extends PyBuiltinFunctionNarrow {

            private PyLong self;

            public PyObject getSelf() {
                return self;
            }

            exposed___hash__(PyLong self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___hash__((PyLong)self,info);
            }

            public PyObject __call__() {
                return Py.newInteger(self.long_hashCode());
            }

            public PyObject inst_call(PyObject gself) {
                PyLong self=(PyLong)gself;
                return Py.newInteger(self.long_hashCode());
            }

        }
        dict.__setitem__("__hash__",new PyMethodDescr("__hash__",PyLong.class,0,0,new exposed___hash__(null,null)));
        dict.__setitem__("__new__",new PyNewWrapper(PyLong.class,"__new__",-1,-1) {
            public PyObject new_impl(boolean init,PyType subtype,PyObject[]args,String[]keywords) {
              return long_new(this,init,subtype,args,keywords);
            }
        });
    }

    private BigInteger value;

    public static PyObject long_new(PyNewWrapper new_, boolean init, PyType subtype,
            PyObject[] args, String[] keywords) {

        ArgParser ap = new ArgParser(exposed_name, args, keywords,
            new String[] { "x", "base" }, 0);

        PyObject x = ap.getPyObject(0, null);
        int base = ap.getInt(1, -909);
        if (new_.for_type == subtype) {
            if (x == null) {
                return Py.Zero;
            }

            Object o = x.__tojava__(BigInteger.class);
            if(o != Py.NoConversion) {
                return new PyLong((BigInteger)o);
            }

            if (base == -909) {
                return x.__long__();
            }

            if (!(x instanceof PyString)) {
                throw Py.TypeError("long: can't convert non-string with explicit base");
            }

            return ((PyString) x).atol(base);
        } else {
            if (x == null) {
                return new PyLongDerived(subtype, BigInteger.valueOf(0));
            }
            Object o = x.__tojava__(BigInteger.class);
            if(o != Py.NoConversion) {
                return new PyLongDerived(subtype, (BigInteger)o);
            }

            if (base == -909) {
                return new PyLongDerived(subtype, x.__long__().getValue());
            }

            if (!(x instanceof PyString)) {
                throw Py.TypeError("long: can't convert non-string with explicit base");
            }

            return new PyLongDerived(subtype, (((PyString) x).atol(base)).getValue());
        }
    } // xxx
    
    private static final PyType LONGTYPE = PyType.fromClass(PyLong.class);

    public PyLong(PyType subType, BigInteger v) {
        super(subType);
        value = v;
    }
    public PyLong(BigInteger v) {
        this(LONGTYPE, v);
    }

    public PyLong(double v) {
        this(new java.math.BigDecimal(v).toBigInteger());
    }
    public PyLong(long v) {
        this(BigInteger.valueOf(v));
    }
    public PyLong(String s) {
        this(new BigInteger(s));
    }
    
    public BigInteger getValue() {
        return value;
    }

    public String toString() {
        return long_toString();
    }

    final String long_toString() {
        return value.toString()+"L";
    }

    public int hashCode() {
        return long_hashCode();
    }

    final int long_hashCode() {
        // Probably won't work well for some classes of keys...
        return value.intValue();
    }

    public boolean __nonzero__() {
        return !value.equals(BigInteger.valueOf(0));
    }

    public double doubleValue() {
        double v = value.doubleValue();
        if (v == Double.NEGATIVE_INFINITY || v == Double.POSITIVE_INFINITY) {
            throw Py.OverflowError("long int too long to convert");
        }
        return v;
    }

    private static final double scaledDoubleValue(BigInteger val, int[] exp){
        double x = 0;
        int signum = val.signum();
        byte[] digits;

        if (signum >= 0) {
            digits = val.toByteArray();
        } else {
            digits = val.negate().toByteArray();
        }

        int count = 8;
        int i = 0;

        if (digits[0] == 0) {
            i++;
            count++;
        }
        count = count <= digits.length?count:digits.length;

        while (i < count) {
            x = x * 256 + (digits[i] & 0xff);
            i++;
        }
        exp[0] = digits.length - i;
        return signum*x;
    }

    public double scaledDoubleValue(int[] exp){
        return scaledDoubleValue(value,exp);
    }


    private long getLong(long min, long max) {
        if (value.compareTo(maxLong) <= 0 && value.compareTo(minLong) >= 0) {
            long v = value.longValue();
            if (v >= min && v <= max)
                return v;
        }
        throw Py.OverflowError("long int too long to convert");
    }

    public long asLong(int index) {
        return getLong(Long.MIN_VALUE, Long.MAX_VALUE);
    }

    public Object __tojava__(Class c) {
        try {
            if (c == Byte.TYPE || c == Byte.class) {
                return new Byte((byte)getLong(Byte.MIN_VALUE,
                                              Byte.MAX_VALUE));
            }
            if (c == Short.TYPE || c == Short.class) {
                return new Short((short)getLong(Short.MIN_VALUE,
                                                Short.MAX_VALUE));
            }
            if (c == Integer.TYPE || c == Integer.class) {
                return new Integer((int)getLong(Integer.MIN_VALUE,
                                                Integer.MAX_VALUE));
            }
            if (c == Long.TYPE || c == Long.class) {
                return new Long(getLong(Long.MIN_VALUE,
                                        Long.MAX_VALUE));
            }
            if (c == Float.TYPE || c == Double.TYPE || c == Float.class ||
                c == Double.class)
            {
                return __float__().__tojava__(c);
            }
            if (c == BigInteger.class || c == Number.class ||
                c == Object.class || c == Serializable.class)
            {
                return value;
            }
        } catch (PyException e) {
            return Py.NoConversion;
        }
        return super.__tojava__(c);
    }

    public int __cmp__(PyObject other) {
        return long___cmp__(other);
    }

    final int long___cmp__(PyObject other) {
        if (!canCoerce(other))
            return -2;
        return value.compareTo(coerce(other));
    }

    public Object __coerce_ex__(PyObject other) {
        if (other instanceof PyLong)
            return other;
        else
            if (other instanceof PyInteger) {
                return new PyLong(((PyInteger)other).getValue());
            } else {
                return Py.None;
            }
    }

    private static final boolean canCoerce(PyObject other) {
        return other instanceof PyLong || other instanceof PyInteger;
    }

    private static final BigInteger coerce(PyObject other) {
        if (other instanceof PyLong)
            return ((PyLong) other).value;
        else if (other instanceof PyInteger)
            return BigInteger.valueOf(
                   ((PyInteger) other).getValue());
        else
            throw Py.TypeError("xxx");
    }

    public PyObject __add__(PyObject right) {
        return long___add__(right);
    }

    final PyObject long___add__(PyObject right) {
        if (!canCoerce(right))
            return null;
        return new PyLong(value.add(coerce(right)));
    }

    public PyObject __radd__(PyObject left) {
        return long___radd__(left);
    }

    final PyObject long___radd__(PyObject left) {
        return __add__(left);
    }

    public PyObject __sub__(PyObject right) {
        return long___sub__(right);
    }

    final PyObject long___sub__(PyObject right) {
        if (!canCoerce(right))
            return null;
        return new PyLong(value.subtract(coerce(right)));
    }

    public PyObject __rsub__(PyObject left) {
        return long___rsub__(left);
    }

    final PyObject long___rsub__(PyObject left) {
        return new PyLong(coerce(left).subtract(value));
    }

    public PyObject __mul__(PyObject right) {
        return long___mul__(right);
    }

    final PyObject long___mul__(PyObject right) {
        if (right instanceof PySequence)
            return ((PySequence) right).repeat(coerceInt(this));

        if (!canCoerce(right))
            return null;
        return new PyLong(value.multiply(coerce(right)));
    }

    public PyObject __rmul__(PyObject left) {
        return long___rmul__(left);
    }

    final PyObject long___rmul__(PyObject left) {
        if (left instanceof PySequence)
            return ((PySequence) left).repeat(coerceInt(this));
        if (!canCoerce(left))
            return null;
        return new PyLong(coerce(left).multiply(value));
    }

    // Getting signs correct for integer division
    // This convention makes sense when you consider it in tandem with modulo
    private BigInteger divide(BigInteger x, BigInteger y) {
        BigInteger zero = BigInteger.valueOf(0);
        if (y.equals(zero))
            throw Py.ZeroDivisionError("long division or modulo");

        if (y.compareTo(zero) < 0) {
            if (x.compareTo(zero) > 0)
                return (x.subtract(y).subtract(
                                      BigInteger.valueOf(1))).divide(y);
        } else {
            if (x.compareTo(zero) < 0)
                return (x.subtract(y).add(BigInteger.valueOf(1))).divide(y);
        }
        return x.divide(y);
    }

    public PyObject __div__(PyObject right) {
        return long___div__(right);
    }

    final PyObject long___div__(PyObject right) {
        if (!canCoerce(right))
            return null;
        if (Options.divisionWarning > 0)
            Py.warning(Py.DeprecationWarning, "classic long division");
        return new PyLong(divide(value, coerce(right)));
    }

    public PyObject __rdiv__(PyObject left) {
        return long___rdiv__(left);
    }

    final PyObject long___rdiv__(PyObject left) {
        if (!canCoerce(left))
            return null;
        if (Options.divisionWarning > 0)
            Py.warning(Py.DeprecationWarning, "classic long division");
        return new PyLong(divide(coerce(left), value));
    }

    public PyObject __floordiv__(PyObject right) {
        return long___floordiv__(right);
    }

    final PyObject long___floordiv__(PyObject right) {
        if (!canCoerce(right))
            return null;
        return new PyLong(divide(value, coerce(right)));
    }

    public PyObject __rfloordiv__(PyObject left) {
        return long___rfloordiv__(left);
    }

    final PyObject long___rfloordiv__(PyObject left) {
        if (!canCoerce(left))
            return null;
        return new PyLong(divide(coerce(left), value));
    }

    private static final PyFloat true_divide(BigInteger a,BigInteger b) {
        int[] ae = new int[1];
        int[] be = new int[1];
        double ad,bd;

        ad = scaledDoubleValue(a,ae);
        bd = scaledDoubleValue(b,be);

        if (bd == 0 ) throw Py.ZeroDivisionError("long division or modulo");

        ad /= bd;
        int aexp = ae[0]-be[0];

        if (aexp > Integer.MAX_VALUE/8) {
            throw Py.OverflowError("long/long too large for a float");
        } else if ( aexp < -(Integer.MAX_VALUE/8)) {
            return new PyFloat(0.0);
        }

        ad = ad * Math.pow(2.0, aexp*8);

        if (Double.isInfinite(ad)) {
            throw Py.OverflowError("long/long too large for a float");
        }

        return new PyFloat(ad);
    }

    public PyObject __truediv__(PyObject right) {
        return long___truediv__(right);
    }

    final PyObject long___truediv__(PyObject right) {
        if (!canCoerce(right))
            return null;
        return true_divide(this.value,coerce(right));
    }

    public PyObject __rtruediv__(PyObject left) {
        return long___rtruediv__(left);
    }

    final PyObject long___rtruediv__(PyObject left) {
        if (!canCoerce(left))
            return null;
        return true_divide(coerce(left),this.value);
    }

    private BigInteger modulo(BigInteger x, BigInteger y, BigInteger xdivy) {
        return x.subtract(xdivy.multiply(y));
    }

    public PyObject __mod__(PyObject right) {
        return long___mod__(right);
    }

    final PyObject long___mod__(PyObject right) {
        if (!canCoerce(right))
            return null;
        BigInteger rightv = coerce(right);
        return new PyLong(modulo(value, rightv, divide(value, rightv)));
    }

    public PyObject __rmod__(PyObject left) {
        return long___rmod__(left);
    }

    final PyObject long___rmod__(PyObject left) {
        if (!canCoerce(left))
            return null;
        BigInteger leftv = coerce(left);
        return new PyLong(modulo(leftv, value, divide(leftv, value)));
    }

    public PyObject __divmod__(PyObject right) {
        return long___divmod__(right);
    }

    final PyObject long___divmod__(PyObject right) {
        if (!canCoerce(right))
            return null;
        BigInteger rightv = coerce(right);

        BigInteger xdivy = divide(value, rightv);
        return new PyTuple(new PyObject[] {
            new PyLong(xdivy),
            new PyLong(modulo(value, rightv, xdivy))
        });
    }

    public PyObject __rdivmod__(PyObject left) {
        return long___rdivmod__(left);
    }

    final PyObject long___rdivmod__(PyObject left) {
        if (!canCoerce(left))
            return null;
        BigInteger leftv = coerce(left);

        BigInteger xdivy = divide(leftv, value);
        return new PyTuple(new PyObject[] {
            new PyLong(xdivy),
            new PyLong(modulo(leftv, value, xdivy))
        });
    }

    public PyObject __pow__(PyObject right, PyObject modulo) {
        return long___pow__(right, modulo);
    }
    final PyObject long___pow__(PyObject right, PyObject modulo) {
        if (!canCoerce(right))
            return null;

        if (modulo != null && !canCoerce(right))
            return null;
        return _pow(value, coerce(right), modulo, this, right);
    }

    public PyObject __rpow__(PyObject left) {
        return long___rpow__(left);
    }

    final PyObject long___rpow__(PyObject left) {
        if (!canCoerce(left))
            return null;
        return _pow(coerce(left), value, null, left, this);
    }

    public static PyObject _pow(BigInteger value, BigInteger y,
                                PyObject modulo, PyObject left, PyObject right)
    {
        if (y.compareTo(BigInteger.valueOf(0)) < 0) {
            if (value.compareTo(BigInteger.valueOf(0)) != 0)
                return left.__float__().__pow__(right, modulo);
            else
                throw Py.ZeroDivisionError("zero to a negative power");
        }
        if (modulo == null)
            return new PyLong(value.pow(y.intValue()));
        else {
            // This whole thing can be trivially rewritten after bugs
            // in modPow are fixed by SUN

            BigInteger z = coerce(modulo);
            int zi = z.intValue();
            // Clear up some special cases right away
            if (zi == 0)
                throw Py.ValueError("pow(x, y, z) with z == 0");
            if (zi == 1 || zi == -1)
                return new PyLong(0);

            if (z.compareTo(BigInteger.valueOf(0)) <= 0) {
                // Handle negative modulo's specially
                /*if (z.compareTo(BigInteger.valueOf(0)) == 0) {
                  throw Py.ValueError("pow(x, y, z) with z == 0");
                  }*/
                y = value.modPow(y, z.negate());
                if (y.compareTo(BigInteger.valueOf(0)) > 0) {
                    return new PyLong(z.add(y));
                } else {
                    return new PyLong(y);
                }
                //return __pow__(right).__mod__(modulo);
            } else {
                // This is buggy in SUN's jdk1.1.5
                // Extra __mod__ improves things slightly
                return new PyLong(value.modPow(y, z));
                //return __pow__(right).__mod__(modulo);
            }
        }
    }

    private static final int coerceInt(PyObject other) {
        if (other instanceof PyLong)
            return (int) ((PyLong) other).getLong(
                          Integer.MIN_VALUE, Integer.MAX_VALUE);
        else if (other instanceof PyInteger)
            return ((PyInteger) other).getValue();
        else
            throw Py.TypeError("xxx");
    }

    public PyObject __lshift__(PyObject right) {
        return long___lshift__(right);
    }

    final PyObject long___lshift__(PyObject right) {
        if (!canCoerce(right))
            return null;
        int rightv = coerceInt(right);
        if(rightv < 0)
            throw Py.ValueError("negative shift count");
        return new PyLong(value.shiftLeft(rightv));
    }

    public PyObject __rshift__(PyObject right) {
        return long___rshift__(right);
    }

    final PyObject long___rshift__(PyObject right) {
        if (!canCoerce(right))
            return null;
        int rightv = coerceInt(right);
        if(rightv < 0)
            throw Py.ValueError("negative shift count");
        return new PyLong(value.shiftRight(rightv));
    }

    public PyObject __and__(PyObject right) {
        return long___and__(right);
    }

    final PyObject long___and__(PyObject right) {
        if (!canCoerce(right))
            return null;
        return new PyLong(value.and(coerce(right)));
    }

    public PyObject __rand__(PyObject left) {
        return long___rand__(left);
    }

    final PyObject long___rand__(PyObject left) {
        if (!canCoerce(left))
            return null;
        return new PyLong(coerce(left).and(value));
    }

    public PyObject __xor__(PyObject right) {
        return long___xor__(right);
    }

    final PyObject long___xor__(PyObject right) {
        if (!canCoerce(right))
            return null;
        return new PyLong(value.xor(coerce(right)));
    }

    public PyObject __rxor__(PyObject left) {
        return long___rxor__(left);
    }

    final PyObject long___rxor__(PyObject left) {
        if (!canCoerce(left))
            return null;
        return new PyLong(coerce(left).xor(value));
    }

    public PyObject __or__(PyObject right) {
        return long___or__(right);
    }

    final PyObject long___or__(PyObject right) {
        if (!canCoerce(right))
            return null;
        return new PyLong(value.or(coerce(right)));
    }

    public PyObject __ror__(PyObject left) {
        return long___ror__(left);
    }

    final PyObject long___ror__(PyObject left) {
        if (!canCoerce(left))
            return null;
        return new PyLong(coerce(left).or(value));
    }

    public PyObject __neg__() {
        return long___neg__();
    }

    final PyObject long___neg__() {
        return new PyLong(value.negate());
    }

    public PyObject __pos__() {
        return long___pos__();
    }

    final PyObject long___pos__() {
        return this;
    }

    public PyObject __abs__() {
        return long___abs__();
    }
    final PyObject long___abs__() {
        return new PyLong(value.abs());
    }

    public PyObject __invert__() {
        return long___invert__();
    }

    final PyObject long___invert__() {
        return new PyLong(value.not());
    }

    public PyObject __int__() {
        return long___int__();
    }

    final PyObject long___int__() {
        long v = value.longValue();
        if (v < Integer.MIN_VALUE || v > Integer.MAX_VALUE) {
            return this;
        }
        return new PyInteger((int)getLong(Integer.MIN_VALUE,
                                          Integer.MAX_VALUE));
    }


    public PyLong __long__() {
        return long___long__();
    }

    final PyLong long___long__() {
        return this;
    }

    public PyFloat __float__() {
        return long___float__();
    }

    final PyFloat long___float__() {
        return new PyFloat(doubleValue());
    }

    public PyComplex __complex__() {
        return long___complex__();
    }

    final PyComplex long___complex__() {
        return new PyComplex(doubleValue(), 0.);
    }

    public PyString __oct__() {
        return long___oct__();
    }

    final PyString long___oct__() {
        String s = value.toString(8);
        if (s.startsWith("-"))
            return new PyString("-0"+s.substring(1, s.length())+"L");
        else
            if (s.startsWith("0"))
                return new PyString(s+"L");
            else
                return new PyString("0"+s+"L");
    }

    public PyString __hex__() {
        return long___hex__();
    }

    final PyString long___hex__() {
        String s = value.toString(16).toUpperCase();
        if (s.startsWith("-"))
            return new PyString("-0x"+s.substring(1, s.length())+"L");
        else
            return new PyString("0x"+s+"L");
    }

    public PyString __str__() {
        return Py.newString(value.toString());
    }
    
    public PyUnicode __unicode__() {
        return new PyUnicode(value.toString());
    }


    public boolean isMappingType() { return false; }
    public boolean isSequenceType() { return false; }

}
