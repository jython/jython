package org.python.modules.sets;

import org.python.core.Py;
import org.python.core.PyException;
import org.python.core.PyObject;
import org.python.core.PyType;
import org.python.core.PyBuiltinFunctionNarrow;
import org.python.core.PyBuiltinFunction;
import org.python.core.PyMethodDescr;
import org.python.core.PyString;
import org.python.core.PyBuiltinFunctionWide;
import org.python.core.PyNewWrapper;

import java.util.Iterator;

public class PySet extends BaseSet {

    public PySet() {
        super();
    }

    public PySet(PyType type) {
        super(type);
    }

    public PySet(PyObject data) {
        super(data);

    }

    /* type info */

    public static final String exposed_name="Set";

    public static final Class exposed_base=PyObject.class;

    public static void typeSetup(PyObject dict,PyType.Newstyle marker) {
        class exposed___ne__ extends PyBuiltinFunctionNarrow {

            private PySet self;

            public PyObject getSelf() {
                return self;
            }

            exposed___ne__(PySet self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___ne__((PySet)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=self.baseset___ne__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PySet self=(PySet)gself;
                PyObject ret=self.baseset___ne__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__ne__",new PyMethodDescr("__ne__",PySet.class,1,1,new exposed___ne__(null,null)));
        class exposed___eq__ extends PyBuiltinFunctionNarrow {

            private PySet self;

            public PyObject getSelf() {
                return self;
            }

            exposed___eq__(PySet self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___eq__((PySet)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=self.baseset___eq__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PySet self=(PySet)gself;
                PyObject ret=self.baseset___eq__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__eq__",new PyMethodDescr("__eq__",PySet.class,1,1,new exposed___eq__(null,null)));
        class exposed___or__ extends PyBuiltinFunctionNarrow {

            private PySet self;

            public PyObject getSelf() {
                return self;
            }

            exposed___or__(PySet self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___or__((PySet)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=self.baseset___or__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PySet self=(PySet)gself;
                PyObject ret=self.baseset___or__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__or__",new PyMethodDescr("__or__",PySet.class,1,1,new exposed___or__(null,null)));
        class exposed___xor__ extends PyBuiltinFunctionNarrow {

            private PySet self;

            public PyObject getSelf() {
                return self;
            }

            exposed___xor__(PySet self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___xor__((PySet)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=self.baseset___xor__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PySet self=(PySet)gself;
                PyObject ret=self.baseset___xor__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__xor__",new PyMethodDescr("__xor__",PySet.class,1,1,new exposed___xor__(null,null)));
        class exposed___sub__ extends PyBuiltinFunctionNarrow {

            private PySet self;

            public PyObject getSelf() {
                return self;
            }

            exposed___sub__(PySet self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___sub__((PySet)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=self.baseset___sub__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PySet self=(PySet)gself;
                PyObject ret=self.baseset___sub__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__sub__",new PyMethodDescr("__sub__",PySet.class,1,1,new exposed___sub__(null,null)));
        class exposed___and__ extends PyBuiltinFunctionNarrow {

            private PySet self;

            public PyObject getSelf() {
                return self;
            }

            exposed___and__(PySet self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___and__((PySet)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=self.baseset___and__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PySet self=(PySet)gself;
                PyObject ret=self.baseset___and__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__and__",new PyMethodDescr("__and__",PySet.class,1,1,new exposed___and__(null,null)));
        class exposed___gt__ extends PyBuiltinFunctionNarrow {

            private PySet self;

            public PyObject getSelf() {
                return self;
            }

            exposed___gt__(PySet self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___gt__((PySet)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=self.baseset___gt__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PySet self=(PySet)gself;
                PyObject ret=self.baseset___gt__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__gt__",new PyMethodDescr("__gt__",PySet.class,1,1,new exposed___gt__(null,null)));
        class exposed___ge__ extends PyBuiltinFunctionNarrow {

            private PySet self;

            public PyObject getSelf() {
                return self;
            }

            exposed___ge__(PySet self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___ge__((PySet)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=self.baseset___ge__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PySet self=(PySet)gself;
                PyObject ret=self.baseset___ge__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__ge__",new PyMethodDescr("__ge__",PySet.class,1,1,new exposed___ge__(null,null)));
        class exposed___le__ extends PyBuiltinFunctionNarrow {

            private PySet self;

            public PyObject getSelf() {
                return self;
            }

            exposed___le__(PySet self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___le__((PySet)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=self.baseset___le__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PySet self=(PySet)gself;
                PyObject ret=self.baseset___le__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__le__",new PyMethodDescr("__le__",PySet.class,1,1,new exposed___le__(null,null)));
        class exposed___lt__ extends PyBuiltinFunctionNarrow {

            private PySet self;

            public PyObject getSelf() {
                return self;
            }

            exposed___lt__(PySet self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___lt__((PySet)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=self.baseset___lt__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PySet self=(PySet)gself;
                PyObject ret=self.baseset___lt__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__lt__",new PyMethodDescr("__lt__",PySet.class,1,1,new exposed___lt__(null,null)));
        class exposed___contains__ extends PyBuiltinFunctionNarrow {

            private PySet self;

            public PyObject getSelf() {
                return self;
            }

            exposed___contains__(PySet self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___contains__((PySet)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                return Py.newBoolean(self.baseset___contains__(arg0));
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PySet self=(PySet)gself;
                return Py.newBoolean(self.baseset___contains__(arg0));
            }

        }
        dict.__setitem__("__contains__",new PyMethodDescr("__contains__",PySet.class,1,1,new exposed___contains__(null,null)));
        class exposed___deepcopy__ extends PyBuiltinFunctionNarrow {

            private PySet self;

            public PyObject getSelf() {
                return self;
            }

            exposed___deepcopy__(PySet self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___deepcopy__((PySet)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                return self.baseset___deepcopy__(arg0);
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PySet self=(PySet)gself;
                return self.baseset___deepcopy__(arg0);
            }

        }
        dict.__setitem__("__deepcopy__",new PyMethodDescr("__deepcopy__",PySet.class,1,1,new exposed___deepcopy__(null,null)));
        class exposed___nonzero__ extends PyBuiltinFunctionNarrow {

            private PySet self;

            public PyObject getSelf() {
                return self;
            }

            exposed___nonzero__(PySet self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___nonzero__((PySet)self,info);
            }

            public PyObject __call__() {
                return Py.newBoolean(self.baseset___nonzero__());
            }

            public PyObject inst_call(PyObject gself) {
                PySet self=(PySet)gself;
                return Py.newBoolean(self.baseset___nonzero__());
            }

        }
        dict.__setitem__("__nonzero__",new PyMethodDescr("__nonzero__",PySet.class,0,0,new exposed___nonzero__(null,null)));
        class exposed_copy extends PyBuiltinFunctionNarrow {

            private PySet self;

            public PyObject getSelf() {
                return self;
            }

            exposed_copy(PySet self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed_copy((PySet)self,info);
            }

            public PyObject __call__() {
                return self.baseset_copy();
            }

            public PyObject inst_call(PyObject gself) {
                PySet self=(PySet)gself;
                return self.baseset_copy();
            }

        }
        dict.__setitem__("copy",new PyMethodDescr("copy",PySet.class,0,0,new exposed_copy(null,null)));
        class exposed_union extends PyBuiltinFunctionNarrow {

            private PySet self;

            public PyObject getSelf() {
                return self;
            }

            exposed_union(PySet self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed_union((PySet)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                return self.baseset_union(arg0);
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PySet self=(PySet)gself;
                return self.baseset_union(arg0);
            }

        }
        dict.__setitem__("union",new PyMethodDescr("union",PySet.class,1,1,new exposed_union(null,null)));
        class exposed_difference extends PyBuiltinFunctionNarrow {

            private PySet self;

            public PyObject getSelf() {
                return self;
            }

            exposed_difference(PySet self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed_difference((PySet)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                return self.baseset_difference(arg0);
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PySet self=(PySet)gself;
                return self.baseset_difference(arg0);
            }

        }
        dict.__setitem__("difference",new PyMethodDescr("difference",PySet.class,1,1,new exposed_difference(null,null)));
        class exposed_symmetric_difference extends PyBuiltinFunctionNarrow {

            private PySet self;

            public PyObject getSelf() {
                return self;
            }

            exposed_symmetric_difference(PySet self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed_symmetric_difference((PySet)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                return self.baseset_symmetric_difference(arg0);
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PySet self=(PySet)gself;
                return self.baseset_symmetric_difference(arg0);
            }

        }
        dict.__setitem__("symmetric_difference",new PyMethodDescr("symmetric_difference",PySet.class,1,1,new exposed_symmetric_difference(null,null)));
        class exposed_intersection extends PyBuiltinFunctionNarrow {

            private PySet self;

            public PyObject getSelf() {
                return self;
            }

            exposed_intersection(PySet self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed_intersection((PySet)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                return self.baseset_intersection(arg0);
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PySet self=(PySet)gself;
                return self.baseset_intersection(arg0);
            }

        }
        dict.__setitem__("intersection",new PyMethodDescr("intersection",PySet.class,1,1,new exposed_intersection(null,null)));
        class exposed_issubset extends PyBuiltinFunctionNarrow {

            private PySet self;

            public PyObject getSelf() {
                return self;
            }

            exposed_issubset(PySet self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed_issubset((PySet)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                return self.baseset_issubset(arg0);
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PySet self=(PySet)gself;
                return self.baseset_issubset(arg0);
            }

        }
        dict.__setitem__("issubset",new PyMethodDescr("issubset",PySet.class,1,1,new exposed_issubset(null,null)));
        class exposed_issuperset extends PyBuiltinFunctionNarrow {

            private PySet self;

            public PyObject getSelf() {
                return self;
            }

            exposed_issuperset(PySet self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed_issuperset((PySet)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                return self.baseset_issuperset(arg0);
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PySet self=(PySet)gself;
                return self.baseset_issuperset(arg0);
            }

        }
        dict.__setitem__("issuperset",new PyMethodDescr("issuperset",PySet.class,1,1,new exposed_issuperset(null,null)));
        class exposed___hash__ extends PyBuiltinFunctionNarrow {

            private PySet self;

            public PyObject getSelf() {
                return self;
            }

            exposed___hash__(PySet self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___hash__((PySet)self,info);
            }

            public PyObject __call__() {
                return Py.newInteger(self.set_hashCode());
            }

            public PyObject inst_call(PyObject gself) {
                PySet self=(PySet)gself;
                return Py.newInteger(self.set_hashCode());
            }

        }
        dict.__setitem__("__hash__",new PyMethodDescr("__hash__",PySet.class,0,0,new exposed___hash__(null,null)));
        class exposed___repr__ extends PyBuiltinFunctionNarrow {

            private PySet self;

            public PyObject getSelf() {
                return self;
            }

            exposed___repr__(PySet self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___repr__((PySet)self,info);
            }

            public PyObject __call__() {
                return new PyString(self.baseset_toString());
            }

            public PyObject inst_call(PyObject gself) {
                PySet self=(PySet)gself;
                return new PyString(self.baseset_toString());
            }

        }
        dict.__setitem__("__repr__",new PyMethodDescr("__repr__",PySet.class,0,0,new exposed___repr__(null,null)));
        class exposed_add extends PyBuiltinFunctionNarrow {

            private PySet self;

            public PyObject getSelf() {
                return self;
            }

            exposed_add(PySet self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed_add((PySet)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                self.set_add(arg0);
                return Py.None;
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PySet self=(PySet)gself;
                self.set_add(arg0);
                return Py.None;
            }

        }
        dict.__setitem__("add",new PyMethodDescr("add",PySet.class,1,1,new exposed_add(null,null)));
        class exposed_remove extends PyBuiltinFunctionNarrow {

            private PySet self;

            public PyObject getSelf() {
                return self;
            }

            exposed_remove(PySet self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed_remove((PySet)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                self.set_remove(arg0);
                return Py.None;
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PySet self=(PySet)gself;
                self.set_remove(arg0);
                return Py.None;
            }

        }
        dict.__setitem__("remove",new PyMethodDescr("remove",PySet.class,1,1,new exposed_remove(null,null)));
        class exposed_discard extends PyBuiltinFunctionNarrow {

            private PySet self;

            public PyObject getSelf() {
                return self;
            }

            exposed_discard(PySet self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed_discard((PySet)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                self.set_discard(arg0);
                return Py.None;
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PySet self=(PySet)gself;
                self.set_discard(arg0);
                return Py.None;
            }

        }
        dict.__setitem__("discard",new PyMethodDescr("discard",PySet.class,1,1,new exposed_discard(null,null)));
        class exposed_pop extends PyBuiltinFunctionNarrow {

            private PySet self;

            public PyObject getSelf() {
                return self;
            }

            exposed_pop(PySet self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed_pop((PySet)self,info);
            }

            public PyObject __call__() {
                return self.set_pop();
            }

            public PyObject inst_call(PyObject gself) {
                PySet self=(PySet)gself;
                return self.set_pop();
            }

        }
        dict.__setitem__("pop",new PyMethodDescr("pop",PySet.class,0,0,new exposed_pop(null,null)));
        class exposed_clear extends PyBuiltinFunctionNarrow {

            private PySet self;

            public PyObject getSelf() {
                return self;
            }

            exposed_clear(PySet self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed_clear((PySet)self,info);
            }

            public PyObject __call__() {
                self.set_clear();
                return Py.None;
            }

            public PyObject inst_call(PyObject gself) {
                PySet self=(PySet)gself;
                self.set_clear();
                return Py.None;
            }

        }
        dict.__setitem__("clear",new PyMethodDescr("clear",PySet.class,0,0,new exposed_clear(null,null)));
        class exposed_update extends PyBuiltinFunctionNarrow {

            private PySet self;

            public PyObject getSelf() {
                return self;
            }

            exposed_update(PySet self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed_update((PySet)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                self.set_update(arg0);
                return Py.None;
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PySet self=(PySet)gself;
                self.set_update(arg0);
                return Py.None;
            }

        }
        dict.__setitem__("update",new PyMethodDescr("update",PySet.class,1,1,new exposed_update(null,null)));
        class exposed_union_update extends PyBuiltinFunctionNarrow {

            private PySet self;

            public PyObject getSelf() {
                return self;
            }

            exposed_union_update(PySet self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed_union_update((PySet)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                self.set_union_update(arg0);
                return Py.None;
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PySet self=(PySet)gself;
                self.set_union_update(arg0);
                return Py.None;
            }

        }
        dict.__setitem__("union_update",new PyMethodDescr("union_update",PySet.class,1,1,new exposed_union_update(null,null)));
        class exposed_intersection_update extends PyBuiltinFunctionNarrow {

            private PySet self;

            public PyObject getSelf() {
                return self;
            }

            exposed_intersection_update(PySet self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed_intersection_update((PySet)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                self.set_intersection_update(arg0);
                return Py.None;
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PySet self=(PySet)gself;
                self.set_intersection_update(arg0);
                return Py.None;
            }

        }
        dict.__setitem__("intersection_update",new PyMethodDescr("intersection_update",PySet.class,1,1,new exposed_intersection_update(null,null)));
        class exposed_symmetric_difference_update extends PyBuiltinFunctionNarrow {

            private PySet self;

            public PyObject getSelf() {
                return self;
            }

            exposed_symmetric_difference_update(PySet self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed_symmetric_difference_update((PySet)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                self.set_symmetric_difference_update(arg0);
                return Py.None;
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PySet self=(PySet)gself;
                self.set_symmetric_difference_update(arg0);
                return Py.None;
            }

        }
        dict.__setitem__("symmetric_difference_update",new PyMethodDescr("symmetric_difference_update",PySet.class,1,1,new exposed_symmetric_difference_update(null,null)));
        class exposed_difference_update extends PyBuiltinFunctionNarrow {

            private PySet self;

            public PyObject getSelf() {
                return self;
            }

            exposed_difference_update(PySet self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed_difference_update((PySet)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                self.set_difference_update(arg0);
                return Py.None;
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PySet self=(PySet)gself;
                self.set_difference_update(arg0);
                return Py.None;
            }

        }
        dict.__setitem__("difference_update",new PyMethodDescr("difference_update",PySet.class,1,1,new exposed_difference_update(null,null)));
        class exposed___init__ extends PyBuiltinFunctionWide {

            private PySet self;

            public PyObject getSelf() {
                return self;
            }

            exposed___init__(PySet self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___init__((PySet)self,info);
            }

            public PyObject inst_call(PyObject self,PyObject[]args) {
                return inst_call(self,args,Py.NoKeywords);
            }

            public PyObject __call__(PyObject[]args) {
                return __call__(args,Py.NoKeywords);
            }

            public PyObject __call__(PyObject[]args,String[]keywords) {
                self.set_init(args,keywords);
                return Py.None;
            }

            public PyObject inst_call(PyObject gself,PyObject[]args,String[]keywords) {
                PySet self=(PySet)gself;
                self.set_init(args,keywords);
                return Py.None;
            }

        }
        dict.__setitem__("__init__",new PyMethodDescr("__init__",PySet.class,-1,-1,new exposed___init__(null,null)));
        dict.__setitem__("__new__",new PyNewWrapper(PySet.class,"__new__",-1,-1) {

             public PyObject new_impl(boolean init,PyType subtype,PyObject[]args,String[]keywords) {
                 PySet newobj;
                 if (for_type==subtype) {
                     newobj=new PySet();
                     if (init)
                         newobj.set_init(args,keywords);
                 } else {
                     //newobj=new PySetDerived(subtype);
                     return Py.NotImplemented;
                 }
                 return newobj;
             }

         });
    }

    final void set_init(PyObject[] args, String[] kwds) {
        int nargs = args.length - kwds.length;
        if (nargs > 1) {
            throw PyBuiltinFunction.DefaultInfo.unexpectedCall(nargs, false, exposed_name, 0, 1);
        }
        if (nargs == 0) {
            return;
        }

        PyObject o = args[0];
        _update(o);
    }

    public PyObject __ior__(PyObject other) {
        return set___ior__(other);
    }

    final PyObject set___ior__(PyObject other) {
        BaseSet bs = this._binary_sanity_check(other);
        this._set.addAll(bs._set);
        return this;
    }

    public PyObject __ixor__(PyObject other) {
        return set___ixor__(other);
    }

    final PyObject set___ixor__(PyObject other) {
        this._binary_sanity_check(other);
        set_symmetric_difference_update(other);
        return this;
    }


    public PyObject __iand__(PyObject other) {
        return set___iand__(other);
    }

    final PyObject set___iand__(PyObject other) {
        BaseSet bs = this._binary_sanity_check(other);
        this._set = ((BaseSet) this.__and__(bs))._set;
        return this;
    }

    public PyObject __isub__(PyObject other) {
        return set___isub__(other);
    }

    final PyObject set___isub__(PyObject other) {
        BaseSet bs = this._binary_sanity_check(other);
        this._set.removeAll(bs._set);
        return this;
    }

    public int hashCode() {
        return set_hashCode();
    }

    final int set_hashCode() {
        throw Py.TypeError("Can't hash a Set, only an ImmutableSet.");
    }

    final void set_add(PyObject o) {
        try {
            this._set.add(o);
        } catch (PyException e) {
            PyObject immutable = this.asImmutable(e, o);
            this._set.add(immutable);
        }
    }

    final void set_remove(PyObject o) {
        boolean b = false;
        try {
            b = this._set.remove(o);
        } catch (PyException e) {
            PyObject immutable = this.asImmutable(e, o);
            b = this._set.remove(immutable);
        }
        if (!b) {
            throw new PyException(Py.LookupError, o.toString());
        }
    }

    final void set_discard(PyObject o) {
        try {
            this._set.remove(o);
        } catch (PyException e) {
            PyObject immutable = this.asImmutable(e, o);
            this._set.remove(immutable);
        }
    }

    final PyObject set_pop() {
        Iterator iterator = this._set.iterator();
        Object first = iterator.next();
        this._set.remove(first);
        return (PyObject) first;
    }

    final void set_clear() {
        this._set.clear();
    }

    final void set_update(PyObject data) {
        this._update(data);
    }

    final void set_union_update(PyObject other) {
        this._update(other);
    }

    final void set_intersection_update(PyObject other) {
        if (other instanceof BaseSet) {
            this.__iand__(other);
        } else {
            BaseSet set = (BaseSet) baseset_intersection(other);
            this._set = set._set;
        }
    }

    final void set_symmetric_difference_update(PyObject other) {
        BaseSet bs = (other instanceof BaseSet) ? (BaseSet) other : new PySet(other);
        for (Iterator iterator = bs._set.iterator(); iterator.hasNext();) {
            Object o = iterator.next();
            if (this._set.contains(o)) {
                this._set.remove(o);
            } else {
                this._set.add(o);
            }
        }
    }

    final void set_difference_update(PyObject other) {
        if (other instanceof BaseSet) {
            this.__isub__(other);
            return;
        }
        PyObject iter = other.__iter__();
        for (PyObject o; (o = iter.__iternext__()) != null;) {
            if (this.__contains__(o)) {
                this._set.remove(o);
            }
        }
    }

    public PyObject _as_immutable() {
        return new PyImmutableSet(this);
    }
}
