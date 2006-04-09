
package org.python.modules.sets;

import org.python.core.Py;
import org.python.core.PyBuiltinFunction;
import org.python.core.PyBuiltinFunctionNarrow;
import org.python.core.PyBuiltinFunctionWide;
import org.python.core.PyMethodDescr;
import org.python.core.PyNewWrapper;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.core.PyType;

public class PyImmutableSet extends BaseSet {

    /* type info */

    public static final String exposed_name="ImmutableSet";

    public static final Class exposed_base=PyObject.class;

    public static void typeSetup(PyObject dict,PyType.Newstyle marker) {
        class exposed___ne__ extends PyBuiltinFunctionNarrow {

            private PyImmutableSet self;

            public PyObject getSelf() {
                return self;
            }

            exposed___ne__(PyImmutableSet self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___ne__((PyImmutableSet)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=self.baseset___ne__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyImmutableSet self=(PyImmutableSet)gself;
                PyObject ret=self.baseset___ne__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__ne__",new PyMethodDescr("__ne__",PyImmutableSet.class,1,1,new exposed___ne__(null,null)));
        class exposed___eq__ extends PyBuiltinFunctionNarrow {

            private PyImmutableSet self;

            public PyObject getSelf() {
                return self;
            }

            exposed___eq__(PyImmutableSet self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___eq__((PyImmutableSet)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=self.baseset___eq__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyImmutableSet self=(PyImmutableSet)gself;
                PyObject ret=self.baseset___eq__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__eq__",new PyMethodDescr("__eq__",PyImmutableSet.class,1,1,new exposed___eq__(null,null)));
        class exposed___or__ extends PyBuiltinFunctionNarrow {

            private PyImmutableSet self;

            public PyObject getSelf() {
                return self;
            }

            exposed___or__(PyImmutableSet self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___or__((PyImmutableSet)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=self.baseset___or__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyImmutableSet self=(PyImmutableSet)gself;
                PyObject ret=self.baseset___or__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__or__",new PyMethodDescr("__or__",PyImmutableSet.class,1,1,new exposed___or__(null,null)));
        class exposed___xor__ extends PyBuiltinFunctionNarrow {

            private PyImmutableSet self;

            public PyObject getSelf() {
                return self;
            }

            exposed___xor__(PyImmutableSet self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___xor__((PyImmutableSet)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=self.baseset___xor__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyImmutableSet self=(PyImmutableSet)gself;
                PyObject ret=self.baseset___xor__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__xor__",new PyMethodDescr("__xor__",PyImmutableSet.class,1,1,new exposed___xor__(null,null)));
        class exposed___sub__ extends PyBuiltinFunctionNarrow {

            private PyImmutableSet self;

            public PyObject getSelf() {
                return self;
            }

            exposed___sub__(PyImmutableSet self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___sub__((PyImmutableSet)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=self.baseset___sub__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyImmutableSet self=(PyImmutableSet)gself;
                PyObject ret=self.baseset___sub__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__sub__",new PyMethodDescr("__sub__",PyImmutableSet.class,1,1,new exposed___sub__(null,null)));
        class exposed___and__ extends PyBuiltinFunctionNarrow {

            private PyImmutableSet self;

            public PyObject getSelf() {
                return self;
            }

            exposed___and__(PyImmutableSet self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___and__((PyImmutableSet)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=self.baseset___and__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyImmutableSet self=(PyImmutableSet)gself;
                PyObject ret=self.baseset___and__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__and__",new PyMethodDescr("__and__",PyImmutableSet.class,1,1,new exposed___and__(null,null)));
        class exposed___gt__ extends PyBuiltinFunctionNarrow {

            private PyImmutableSet self;

            public PyObject getSelf() {
                return self;
            }

            exposed___gt__(PyImmutableSet self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___gt__((PyImmutableSet)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=self.baseset___gt__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyImmutableSet self=(PyImmutableSet)gself;
                PyObject ret=self.baseset___gt__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__gt__",new PyMethodDescr("__gt__",PyImmutableSet.class,1,1,new exposed___gt__(null,null)));
        class exposed___ge__ extends PyBuiltinFunctionNarrow {

            private PyImmutableSet self;

            public PyObject getSelf() {
                return self;
            }

            exposed___ge__(PyImmutableSet self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___ge__((PyImmutableSet)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=self.baseset___ge__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyImmutableSet self=(PyImmutableSet)gself;
                PyObject ret=self.baseset___ge__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__ge__",new PyMethodDescr("__ge__",PyImmutableSet.class,1,1,new exposed___ge__(null,null)));
        class exposed___le__ extends PyBuiltinFunctionNarrow {

            private PyImmutableSet self;

            public PyObject getSelf() {
                return self;
            }

            exposed___le__(PyImmutableSet self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___le__((PyImmutableSet)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=self.baseset___le__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyImmutableSet self=(PyImmutableSet)gself;
                PyObject ret=self.baseset___le__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__le__",new PyMethodDescr("__le__",PyImmutableSet.class,1,1,new exposed___le__(null,null)));
        class exposed___lt__ extends PyBuiltinFunctionNarrow {

            private PyImmutableSet self;

            public PyObject getSelf() {
                return self;
            }

            exposed___lt__(PyImmutableSet self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___lt__((PyImmutableSet)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=self.baseset___lt__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyImmutableSet self=(PyImmutableSet)gself;
                PyObject ret=self.baseset___lt__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__lt__",new PyMethodDescr("__lt__",PyImmutableSet.class,1,1,new exposed___lt__(null,null)));
        class exposed___contains__ extends PyBuiltinFunctionNarrow {

            private PyImmutableSet self;

            public PyObject getSelf() {
                return self;
            }

            exposed___contains__(PyImmutableSet self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___contains__((PyImmutableSet)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                return Py.newBoolean(self.baseset___contains__(arg0));
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyImmutableSet self=(PyImmutableSet)gself;
                return Py.newBoolean(self.baseset___contains__(arg0));
            }

        }
        dict.__setitem__("__contains__",new PyMethodDescr("__contains__",PyImmutableSet.class,1,1,new exposed___contains__(null,null)));
        class exposed___deepcopy__ extends PyBuiltinFunctionNarrow {

            private PyImmutableSet self;

            public PyObject getSelf() {
                return self;
            }

            exposed___deepcopy__(PyImmutableSet self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___deepcopy__((PyImmutableSet)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                return self.baseset___deepcopy__(arg0);
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyImmutableSet self=(PyImmutableSet)gself;
                return self.baseset___deepcopy__(arg0);
            }

        }
        dict.__setitem__("__deepcopy__",new PyMethodDescr("__deepcopy__",PyImmutableSet.class,1,1,new exposed___deepcopy__(null,null)));
        class exposed___nonzero__ extends PyBuiltinFunctionNarrow {

            private PyImmutableSet self;

            public PyObject getSelf() {
                return self;
            }

            exposed___nonzero__(PyImmutableSet self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___nonzero__((PyImmutableSet)self,info);
            }

            public PyObject __call__() {
                return Py.newBoolean(self.baseset___nonzero__());
            }

            public PyObject inst_call(PyObject gself) {
                PyImmutableSet self=(PyImmutableSet)gself;
                return Py.newBoolean(self.baseset___nonzero__());
            }

        }
        dict.__setitem__("__nonzero__",new PyMethodDescr("__nonzero__",PyImmutableSet.class,0,0,new exposed___nonzero__(null,null)));
        class exposed_copy extends PyBuiltinFunctionNarrow {

            private PyImmutableSet self;

            public PyObject getSelf() {
                return self;
            }

            exposed_copy(PyImmutableSet self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed_copy((PyImmutableSet)self,info);
            }

            public PyObject __call__() {
                return self.baseset_copy();
            }

            public PyObject inst_call(PyObject gself) {
                PyImmutableSet self=(PyImmutableSet)gself;
                return self.baseset_copy();
            }

        }
        dict.__setitem__("copy",new PyMethodDescr("copy",PyImmutableSet.class,0,0,new exposed_copy(null,null)));
        class exposed_union extends PyBuiltinFunctionNarrow {

            private PyImmutableSet self;

            public PyObject getSelf() {
                return self;
            }

            exposed_union(PyImmutableSet self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed_union((PyImmutableSet)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                return self.baseset_union(arg0);
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyImmutableSet self=(PyImmutableSet)gself;
                return self.baseset_union(arg0);
            }

        }
        dict.__setitem__("union",new PyMethodDescr("union",PyImmutableSet.class,1,1,new exposed_union(null,null)));
        class exposed_difference extends PyBuiltinFunctionNarrow {

            private PyImmutableSet self;

            public PyObject getSelf() {
                return self;
            }

            exposed_difference(PyImmutableSet self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed_difference((PyImmutableSet)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                return self.baseset_difference(arg0);
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyImmutableSet self=(PyImmutableSet)gself;
                return self.baseset_difference(arg0);
            }

        }
        dict.__setitem__("difference",new PyMethodDescr("difference",PyImmutableSet.class,1,1,new exposed_difference(null,null)));
        class exposed_symmetric_difference extends PyBuiltinFunctionNarrow {

            private PyImmutableSet self;

            public PyObject getSelf() {
                return self;
            }

            exposed_symmetric_difference(PyImmutableSet self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed_symmetric_difference((PyImmutableSet)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                return self.baseset_symmetric_difference(arg0);
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyImmutableSet self=(PyImmutableSet)gself;
                return self.baseset_symmetric_difference(arg0);
            }

        }
        dict.__setitem__("symmetric_difference",new PyMethodDescr("symmetric_difference",PyImmutableSet.class,1,1,new exposed_symmetric_difference(null,null)));
        class exposed_intersection extends PyBuiltinFunctionNarrow {

            private PyImmutableSet self;

            public PyObject getSelf() {
                return self;
            }

            exposed_intersection(PyImmutableSet self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed_intersection((PyImmutableSet)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                return self.baseset_intersection(arg0);
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyImmutableSet self=(PyImmutableSet)gself;
                return self.baseset_intersection(arg0);
            }

        }
        dict.__setitem__("intersection",new PyMethodDescr("intersection",PyImmutableSet.class,1,1,new exposed_intersection(null,null)));
        class exposed_issubset extends PyBuiltinFunctionNarrow {

            private PyImmutableSet self;

            public PyObject getSelf() {
                return self;
            }

            exposed_issubset(PyImmutableSet self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed_issubset((PyImmutableSet)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                return self.baseset_issubset(arg0);
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyImmutableSet self=(PyImmutableSet)gself;
                return self.baseset_issubset(arg0);
            }

        }
        dict.__setitem__("issubset",new PyMethodDescr("issubset",PyImmutableSet.class,1,1,new exposed_issubset(null,null)));
        class exposed_issuperset extends PyBuiltinFunctionNarrow {

            private PyImmutableSet self;

            public PyObject getSelf() {
                return self;
            }

            exposed_issuperset(PyImmutableSet self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed_issuperset((PyImmutableSet)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                return self.baseset_issuperset(arg0);
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyImmutableSet self=(PyImmutableSet)gself;
                return self.baseset_issuperset(arg0);
            }

        }
        dict.__setitem__("issuperset",new PyMethodDescr("issuperset",PyImmutableSet.class,1,1,new exposed_issuperset(null,null)));
        class exposed___len__ extends PyBuiltinFunctionNarrow {

            private PyImmutableSet self;

            public PyObject getSelf() {
                return self;
            }

            exposed___len__(PyImmutableSet self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___len__((PyImmutableSet)self,info);
            }

            public PyObject __call__() {
                return Py.newInteger(self.baseset___len__());
            }

            public PyObject inst_call(PyObject gself) {
                PyImmutableSet self=(PyImmutableSet)gself;
                return Py.newInteger(self.baseset___len__());
            }

        }
        dict.__setitem__("__len__",new PyMethodDescr("__len__",PyImmutableSet.class,0,0,new exposed___len__(null,null)));
        class exposed___hash__ extends PyBuiltinFunctionNarrow {

            private PyImmutableSet self;

            public PyObject getSelf() {
                return self;
            }

            exposed___hash__(PyImmutableSet self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___hash__((PyImmutableSet)self,info);
            }

            public PyObject __call__() {
                return Py.newInteger(self.immutableset_hashCode());
            }

            public PyObject inst_call(PyObject gself) {
                PyImmutableSet self=(PyImmutableSet)gself;
                return Py.newInteger(self.immutableset_hashCode());
            }

        }
        dict.__setitem__("__hash__",new PyMethodDescr("__hash__",PyImmutableSet.class,0,0,new exposed___hash__(null,null)));
        class exposed___repr__ extends PyBuiltinFunctionNarrow {

            private PyImmutableSet self;

            public PyObject getSelf() {
                return self;
            }

            exposed___repr__(PyImmutableSet self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___repr__((PyImmutableSet)self,info);
            }

            public PyObject __call__() {
                return new PyString(self.baseset_toString());
            }

            public PyObject inst_call(PyObject gself) {
                PyImmutableSet self=(PyImmutableSet)gself;
                return new PyString(self.baseset_toString());
            }

        }
        dict.__setitem__("__repr__",new PyMethodDescr("__repr__",PyImmutableSet.class,0,0,new exposed___repr__(null,null)));
        class exposed___init__ extends PyBuiltinFunctionWide {

            private PyImmutableSet self;

            public PyObject getSelf() {
                return self;
            }

            exposed___init__(PyImmutableSet self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___init__((PyImmutableSet)self,info);
            }

            public PyObject inst_call(PyObject self,PyObject[]args) {
                return inst_call(self,args,Py.NoKeywords);
            }

            public PyObject __call__(PyObject[]args) {
                return __call__(args,Py.NoKeywords);
            }

            public PyObject __call__(PyObject[]args,String[]keywords) {
                self.immutableset_init(args,keywords);
                return Py.None;
            }

            public PyObject inst_call(PyObject gself,PyObject[]args,String[]keywords) {
                PyImmutableSet self=(PyImmutableSet)gself;
                self.immutableset_init(args,keywords);
                return Py.None;
            }

        }
        dict.__setitem__("__init__",new PyMethodDescr("__init__",PyImmutableSet.class,-1,-1,new exposed___init__(null,null)));
        dict.__setitem__("__new__",new PyNewWrapper(PyImmutableSet.class,"__new__",-1,-1) {

          public PyObject new_impl(boolean init,PyType subtype,PyObject[]args,String[]keywords) {
              PyImmutableSet newobj;
              if (for_type==subtype) {
                  newobj=new PyImmutableSet();
                  if (init)
                      newobj.immutableset_init(args,keywords);
              } else {
                  //newobj=new PyImmutableSetDerived(subtype);
                  return Py.NotImplemented;
              }
              return newobj;
          }

      });
    }

    public PyImmutableSet() {
        super();
    }

    public PyImmutableSet(PyType type) {
        super(type);
    }

    public PyImmutableSet(PyObject data) {
        super(data);
    }

    final void immutableset_init(PyObject[] args, String[] kwds) {
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

    final int immutableset_hashCode() {
        return hashCode();
    }

    public int hashCode() {
        return this._set.hashCode();
    }

    public PyObject _as_immutable() {
        return this;
    }

//    public void clear() {
//        throw new UnsupportedOperationException();
//    }
//
//    public boolean add(Object o) {
//        throw new UnsupportedOperationException();
//    }
//
//    public boolean remove(Object o) {
//        throw new UnsupportedOperationException();
//    }
//
//    public boolean addAll(Collection c) {
//        throw new UnsupportedOperationException();
//    }
//
//    public boolean removeAll(Collection c) {
//        throw new UnsupportedOperationException();
//    }
//
//    public boolean retainAll(Collection c) {
//        throw new UnsupportedOperationException();
//    }
}
