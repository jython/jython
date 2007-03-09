package org.python.modules.sets;

import org.python.core.Py;
import org.python.core.PyBuiltinFunction;
import org.python.core.PyBuiltinMethod;
import org.python.core.PyBuiltinMethodNarrow;
import org.python.core.PyMethodDescr;
import org.python.core.PyNewWrapper;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.core.PyType;

public class PyImmutableSet extends BaseSet {
    //~ BEGIN GENERATED REGION -- DO NOT EDIT SEE gexpose.py
    /* type info */

    public static final String exposed_name="ImmutableSet";

    public static final Class exposed_base=PyObject.class;

    public static void typeSetup(PyObject dict,PyType.Newstyle marker) {
        class exposed___ne__ extends PyBuiltinMethodNarrow {

            exposed___ne__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___ne__(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=((PyImmutableSet)self).baseset___ne__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__ne__",new PyMethodDescr("__ne__",PyImmutableSet.class,1,1,new exposed___ne__(null,null)));
        class exposed___eq__ extends PyBuiltinMethodNarrow {

            exposed___eq__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___eq__(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=((PyImmutableSet)self).baseset___eq__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__eq__",new PyMethodDescr("__eq__",PyImmutableSet.class,1,1,new exposed___eq__(null,null)));
        class exposed___or__ extends PyBuiltinMethodNarrow {

            exposed___or__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___or__(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=((PyImmutableSet)self).baseset___or__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__or__",new PyMethodDescr("__or__",PyImmutableSet.class,1,1,new exposed___or__(null,null)));
        class exposed___xor__ extends PyBuiltinMethodNarrow {

            exposed___xor__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___xor__(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=((PyImmutableSet)self).baseset___xor__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__xor__",new PyMethodDescr("__xor__",PyImmutableSet.class,1,1,new exposed___xor__(null,null)));
        class exposed___sub__ extends PyBuiltinMethodNarrow {

            exposed___sub__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___sub__(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=((PyImmutableSet)self).baseset___sub__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__sub__",new PyMethodDescr("__sub__",PyImmutableSet.class,1,1,new exposed___sub__(null,null)));
        class exposed___and__ extends PyBuiltinMethodNarrow {

            exposed___and__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___and__(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=((PyImmutableSet)self).baseset___and__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__and__",new PyMethodDescr("__and__",PyImmutableSet.class,1,1,new exposed___and__(null,null)));
        class exposed___gt__ extends PyBuiltinMethodNarrow {

            exposed___gt__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___gt__(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=((PyImmutableSet)self).baseset___gt__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__gt__",new PyMethodDescr("__gt__",PyImmutableSet.class,1,1,new exposed___gt__(null,null)));
        class exposed___ge__ extends PyBuiltinMethodNarrow {

            exposed___ge__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___ge__(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=((PyImmutableSet)self).baseset___ge__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__ge__",new PyMethodDescr("__ge__",PyImmutableSet.class,1,1,new exposed___ge__(null,null)));
        class exposed___le__ extends PyBuiltinMethodNarrow {

            exposed___le__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___le__(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=((PyImmutableSet)self).baseset___le__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__le__",new PyMethodDescr("__le__",PyImmutableSet.class,1,1,new exposed___le__(null,null)));
        class exposed___lt__ extends PyBuiltinMethodNarrow {

            exposed___lt__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___lt__(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=((PyImmutableSet)self).baseset___lt__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__lt__",new PyMethodDescr("__lt__",PyImmutableSet.class,1,1,new exposed___lt__(null,null)));
        class exposed___contains__ extends PyBuiltinMethodNarrow {

            exposed___contains__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___contains__(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                return Py.newBoolean(((PyImmutableSet)self).baseset___contains__(arg0));
            }

        }
        dict.__setitem__("__contains__",new PyMethodDescr("__contains__",PyImmutableSet.class,1,1,new exposed___contains__(null,null)));
        class exposed___deepcopy__ extends PyBuiltinMethodNarrow {

            exposed___deepcopy__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___deepcopy__(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                return((PyImmutableSet)self).baseset___deepcopy__(arg0);
            }

        }
        dict.__setitem__("__deepcopy__",new PyMethodDescr("__deepcopy__",PyImmutableSet.class,1,1,new exposed___deepcopy__(null,null)));
        class exposed___nonzero__ extends PyBuiltinMethodNarrow {

            exposed___nonzero__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___nonzero__(self,info);
            }

            public PyObject __call__() {
                return Py.newBoolean(((PyImmutableSet)self).baseset___nonzero__());
            }

        }
        dict.__setitem__("__nonzero__",new PyMethodDescr("__nonzero__",PyImmutableSet.class,0,0,new exposed___nonzero__(null,null)));
        class exposed_copy extends PyBuiltinMethodNarrow {

            exposed_copy(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_copy(self,info);
            }

            public PyObject __call__() {
                return((PyImmutableSet)self).baseset_copy();
            }

        }
        dict.__setitem__("copy",new PyMethodDescr("copy",PyImmutableSet.class,0,0,new exposed_copy(null,null)));
        class exposed_union extends PyBuiltinMethodNarrow {

            exposed_union(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_union(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                return((PyImmutableSet)self).baseset_union(arg0);
            }

        }
        dict.__setitem__("union",new PyMethodDescr("union",PyImmutableSet.class,1,1,new exposed_union(null,null)));
        class exposed_difference extends PyBuiltinMethodNarrow {

            exposed_difference(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_difference(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                return((PyImmutableSet)self).baseset_difference(arg0);
            }

        }
        dict.__setitem__("difference",new PyMethodDescr("difference",PyImmutableSet.class,1,1,new exposed_difference(null,null)));
        class exposed_symmetric_difference extends PyBuiltinMethodNarrow {

            exposed_symmetric_difference(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_symmetric_difference(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                return((PyImmutableSet)self).baseset_symmetric_difference(arg0);
            }

        }
        dict.__setitem__("symmetric_difference",new PyMethodDescr("symmetric_difference",PyImmutableSet.class,1,1,new exposed_symmetric_difference(null,null)));
        class exposed_intersection extends PyBuiltinMethodNarrow {

            exposed_intersection(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_intersection(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                return((PyImmutableSet)self).baseset_intersection(arg0);
            }

        }
        dict.__setitem__("intersection",new PyMethodDescr("intersection",PyImmutableSet.class,1,1,new exposed_intersection(null,null)));
        class exposed_issubset extends PyBuiltinMethodNarrow {

            exposed_issubset(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_issubset(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                return((PyImmutableSet)self).baseset_issubset(arg0);
            }

        }
        dict.__setitem__("issubset",new PyMethodDescr("issubset",PyImmutableSet.class,1,1,new exposed_issubset(null,null)));
        class exposed_issuperset extends PyBuiltinMethodNarrow {

            exposed_issuperset(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_issuperset(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                return((PyImmutableSet)self).baseset_issuperset(arg0);
            }

        }
        dict.__setitem__("issuperset",new PyMethodDescr("issuperset",PyImmutableSet.class,1,1,new exposed_issuperset(null,null)));
        class exposed___len__ extends PyBuiltinMethodNarrow {

            exposed___len__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___len__(self,info);
            }

            public PyObject __call__() {
                return Py.newInteger(((PyImmutableSet)self).baseset___len__());
            }

        }
        dict.__setitem__("__len__",new PyMethodDescr("__len__",PyImmutableSet.class,0,0,new exposed___len__(null,null)));
        class exposed___reduce__ extends PyBuiltinMethodNarrow {

            exposed___reduce__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___reduce__(self,info);
            }

            public PyObject __call__() {
                return((PyImmutableSet)self).baseset___reduce__();
            }

        }
        dict.__setitem__("__reduce__",new PyMethodDescr("__reduce__",PyImmutableSet.class,0,0,new exposed___reduce__(null,null)));
        class exposed___hash__ extends PyBuiltinMethodNarrow {

            exposed___hash__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___hash__(self,info);
            }

            public PyObject __call__() {
                return Py.newInteger(((PyImmutableSet)self).ImmutableSet_hashCode());
            }

        }
        dict.__setitem__("__hash__",new PyMethodDescr("__hash__",PyImmutableSet.class,0,0,new exposed___hash__(null,null)));
        class exposed___repr__ extends PyBuiltinMethodNarrow {

            exposed___repr__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___repr__(self,info);
            }

            public PyObject __call__() {
                return new PyString(((PyImmutableSet)self).baseset_toString());
            }

        }
        dict.__setitem__("__repr__",new PyMethodDescr("__repr__",PyImmutableSet.class,0,0,new exposed___repr__(null,null)));
        class exposed___init__ extends PyBuiltinMethod {

            exposed___init__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___init__(self,info);
            }

            public PyObject __call__(PyObject[]args) {
                return __call__(args,Py.NoKeywords);
            }

            public PyObject __call__(PyObject[]args,String[]keywords) {
                ((PyImmutableSet)self).ImmutableSet_init(args,keywords);
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
                                                                                                          newobj.ImmutableSet_init(args,keywords);
                                                                                                  } else {
                                                                                                      newobj=new PyImmutableSetDerived(subtype);
                                                                                                  }
                                                                                                  return newobj;
                                                                                              }

                                                                                          });
    }
    //~ END GENERATED REGION -- DO NOT EDIT SEE gexpose.py

    public PyImmutableSet() {
        super();
    }

    public PyImmutableSet(PyType type) {
        super(type);
    }

    public PyImmutableSet(PyObject data) {
        super(data);
    }

    final void ImmutableSet_init(PyObject[] args, String[] kwds) {
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

    final int ImmutableSet_hashCode() {
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
