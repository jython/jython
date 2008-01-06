package org.python.modules.sets;

import java.util.Iterator;
import org.python.core.Py;
import org.python.core.PyBuiltinFunction;
import org.python.core.PyBuiltinMethod;
import org.python.core.PyBuiltinMethodNarrow;
import org.python.core.PyException;
import org.python.core.PyMethodDescr;
import org.python.core.PyNewWrapper;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.core.PyType;

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

    //~ BEGIN GENERATED REGION -- DO NOT EDIT SEE gexpose.py
    /* type info */

    public static final String exposed_name="Set";

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
                PyObject ret=((PySet)self).baseset___ne__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__ne__",new PyMethodDescr("__ne__",PySet.class,1,1,new exposed___ne__(null,null)));
        class exposed___eq__ extends PyBuiltinMethodNarrow {

            exposed___eq__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___eq__(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=((PySet)self).baseset___eq__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__eq__",new PyMethodDescr("__eq__",PySet.class,1,1,new exposed___eq__(null,null)));
        class exposed___or__ extends PyBuiltinMethodNarrow {

            exposed___or__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___or__(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=((PySet)self).baseset___or__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__or__",new PyMethodDescr("__or__",PySet.class,1,1,new exposed___or__(null,null)));
        class exposed___xor__ extends PyBuiltinMethodNarrow {

            exposed___xor__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___xor__(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=((PySet)self).baseset___xor__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__xor__",new PyMethodDescr("__xor__",PySet.class,1,1,new exposed___xor__(null,null)));
        class exposed___sub__ extends PyBuiltinMethodNarrow {

            exposed___sub__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___sub__(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=((PySet)self).baseset___sub__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__sub__",new PyMethodDescr("__sub__",PySet.class,1,1,new exposed___sub__(null,null)));
        class exposed___and__ extends PyBuiltinMethodNarrow {

            exposed___and__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___and__(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=((PySet)self).baseset___and__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__and__",new PyMethodDescr("__and__",PySet.class,1,1,new exposed___and__(null,null)));
        class exposed___gt__ extends PyBuiltinMethodNarrow {

            exposed___gt__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___gt__(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=((PySet)self).baseset___gt__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__gt__",new PyMethodDescr("__gt__",PySet.class,1,1,new exposed___gt__(null,null)));
        class exposed___ge__ extends PyBuiltinMethodNarrow {

            exposed___ge__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___ge__(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=((PySet)self).baseset___ge__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__ge__",new PyMethodDescr("__ge__",PySet.class,1,1,new exposed___ge__(null,null)));
        class exposed___le__ extends PyBuiltinMethodNarrow {

            exposed___le__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___le__(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=((PySet)self).baseset___le__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__le__",new PyMethodDescr("__le__",PySet.class,1,1,new exposed___le__(null,null)));
        class exposed___lt__ extends PyBuiltinMethodNarrow {

            exposed___lt__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___lt__(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=((PySet)self).baseset___lt__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__lt__",new PyMethodDescr("__lt__",PySet.class,1,1,new exposed___lt__(null,null)));
        class exposed___contains__ extends PyBuiltinMethodNarrow {

            exposed___contains__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___contains__(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                return Py.newBoolean(((PySet)self).baseset___contains__(arg0));
            }

        }
        dict.__setitem__("__contains__",new PyMethodDescr("__contains__",PySet.class,1,1,new exposed___contains__(null,null)));
        class exposed___deepcopy__ extends PyBuiltinMethodNarrow {

            exposed___deepcopy__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___deepcopy__(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                return((PySet)self).baseset___deepcopy__(arg0);
            }

        }
        dict.__setitem__("__deepcopy__",new PyMethodDescr("__deepcopy__",PySet.class,1,1,new exposed___deepcopy__(null,null)));
        class exposed___nonzero__ extends PyBuiltinMethodNarrow {

            exposed___nonzero__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___nonzero__(self,info);
            }

            public PyObject __call__() {
                return Py.newBoolean(((PySet)self).baseset___nonzero__());
            }

        }
        dict.__setitem__("__nonzero__",new PyMethodDescr("__nonzero__",PySet.class,0,0,new exposed___nonzero__(null,null)));
        class exposed_copy extends PyBuiltinMethodNarrow {

            exposed_copy(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_copy(self,info);
            }

            public PyObject __call__() {
                return((PySet)self).baseset_copy();
            }

        }
        dict.__setitem__("copy",new PyMethodDescr("copy",PySet.class,0,0,new exposed_copy(null,null)));
        class exposed_union extends PyBuiltinMethodNarrow {

            exposed_union(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_union(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                return((PySet)self).baseset_union(arg0);
            }

        }
        dict.__setitem__("union",new PyMethodDescr("union",PySet.class,1,1,new exposed_union(null,null)));
        class exposed_difference extends PyBuiltinMethodNarrow {

            exposed_difference(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_difference(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                return((PySet)self).baseset_difference(arg0);
            }

        }
        dict.__setitem__("difference",new PyMethodDescr("difference",PySet.class,1,1,new exposed_difference(null,null)));
        class exposed_symmetric_difference extends PyBuiltinMethodNarrow {

            exposed_symmetric_difference(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_symmetric_difference(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                return((PySet)self).baseset_symmetric_difference(arg0);
            }

        }
        dict.__setitem__("symmetric_difference",new PyMethodDescr("symmetric_difference",PySet.class,1,1,new exposed_symmetric_difference(null,null)));
        class exposed_intersection extends PyBuiltinMethodNarrow {

            exposed_intersection(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_intersection(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                return((PySet)self).baseset_intersection(arg0);
            }

        }
        dict.__setitem__("intersection",new PyMethodDescr("intersection",PySet.class,1,1,new exposed_intersection(null,null)));
        class exposed_issubset extends PyBuiltinMethodNarrow {

            exposed_issubset(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_issubset(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                return((PySet)self).baseset_issubset(arg0);
            }

        }
        dict.__setitem__("issubset",new PyMethodDescr("issubset",PySet.class,1,1,new exposed_issubset(null,null)));
        class exposed_issuperset extends PyBuiltinMethodNarrow {

            exposed_issuperset(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_issuperset(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                return((PySet)self).baseset_issuperset(arg0);
            }

        }
        dict.__setitem__("issuperset",new PyMethodDescr("issuperset",PySet.class,1,1,new exposed_issuperset(null,null)));
        class exposed___len__ extends PyBuiltinMethodNarrow {

            exposed___len__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___len__(self,info);
            }

            public PyObject __call__() {
                return Py.newInteger(((PySet)self).baseset___len__());
            }

        }
        dict.__setitem__("__len__",new PyMethodDescr("__len__",PySet.class,0,0,new exposed___len__(null,null)));
        class exposed___reduce__ extends PyBuiltinMethodNarrow {

            exposed___reduce__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___reduce__(self,info);
            }

            public PyObject __call__() {
                return((PySet)self).baseset___reduce__();
            }

        }
        dict.__setitem__("__reduce__",new PyMethodDescr("__reduce__",PySet.class,0,0,new exposed___reduce__(null,null)));
        class exposed___hash__ extends PyBuiltinMethodNarrow {

            exposed___hash__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___hash__(self,info);
            }

            public PyObject __call__() {
                return Py.newInteger(((PySet)self).Set_hashCode());
            }

        }
        dict.__setitem__("__hash__",new PyMethodDescr("__hash__",PySet.class,0,0,new exposed___hash__(null,null)));
        class exposed___repr__ extends PyBuiltinMethodNarrow {

            exposed___repr__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___repr__(self,info);
            }

            public PyObject __call__() {
                return new PyString(((PySet)self).baseset_toString());
            }

        }
        dict.__setitem__("__repr__",new PyMethodDescr("__repr__",PySet.class,0,0,new exposed___repr__(null,null)));
        class exposed_add extends PyBuiltinMethodNarrow {

            exposed_add(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_add(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                ((PySet)self).Set_add(arg0);
                return Py.None;
            }

        }
        dict.__setitem__("add",new PyMethodDescr("add",PySet.class,1,1,new exposed_add(null,null)));
        class exposed_remove extends PyBuiltinMethodNarrow {

            exposed_remove(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_remove(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                ((PySet)self).Set_remove(arg0);
                return Py.None;
            }

        }
        dict.__setitem__("remove",new PyMethodDescr("remove",PySet.class,1,1,new exposed_remove(null,null)));
        class exposed_discard extends PyBuiltinMethodNarrow {

            exposed_discard(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_discard(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                ((PySet)self).Set_discard(arg0);
                return Py.None;
            }

        }
        dict.__setitem__("discard",new PyMethodDescr("discard",PySet.class,1,1,new exposed_discard(null,null)));
        class exposed_pop extends PyBuiltinMethodNarrow {

            exposed_pop(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_pop(self,info);
            }

            public PyObject __call__() {
                return((PySet)self).Set_pop();
            }

        }
        dict.__setitem__("pop",new PyMethodDescr("pop",PySet.class,0,0,new exposed_pop(null,null)));
        class exposed_clear extends PyBuiltinMethodNarrow {

            exposed_clear(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_clear(self,info);
            }

            public PyObject __call__() {
                ((PySet)self).Set_clear();
                return Py.None;
            }

        }
        dict.__setitem__("clear",new PyMethodDescr("clear",PySet.class,0,0,new exposed_clear(null,null)));
        class exposed_update extends PyBuiltinMethodNarrow {

            exposed_update(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_update(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                ((PySet)self).Set_update(arg0);
                return Py.None;
            }

        }
        dict.__setitem__("update",new PyMethodDescr("update",PySet.class,1,1,new exposed_update(null,null)));
        class exposed_union_update extends PyBuiltinMethodNarrow {

            exposed_union_update(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_union_update(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                ((PySet)self).Set_union_update(arg0);
                return Py.None;
            }

        }
        dict.__setitem__("union_update",new PyMethodDescr("union_update",PySet.class,1,1,new exposed_union_update(null,null)));
        class exposed_intersection_update extends PyBuiltinMethodNarrow {

            exposed_intersection_update(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_intersection_update(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                ((PySet)self).Set_intersection_update(arg0);
                return Py.None;
            }

        }
        dict.__setitem__("intersection_update",new PyMethodDescr("intersection_update",PySet.class,1,1,new exposed_intersection_update(null,null)));
        class exposed_symmetric_difference_update extends PyBuiltinMethodNarrow {

            exposed_symmetric_difference_update(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_symmetric_difference_update(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                ((PySet)self).Set_symmetric_difference_update(arg0);
                return Py.None;
            }

        }
        dict.__setitem__("symmetric_difference_update",new PyMethodDescr("symmetric_difference_update",PySet.class,1,1,new exposed_symmetric_difference_update(null,null)));
        class exposed_difference_update extends PyBuiltinMethodNarrow {

            exposed_difference_update(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_difference_update(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                ((PySet)self).Set_difference_update(arg0);
                return Py.None;
            }

        }
        dict.__setitem__("difference_update",new PyMethodDescr("difference_update",PySet.class,1,1,new exposed_difference_update(null,null)));
        class exposed__as_immutable extends PyBuiltinMethodNarrow {

            exposed__as_immutable(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed__as_immutable(self,info);
            }

            public PyObject __call__() {
                return((PySet)self).Set__as_immutable();
            }

        }
        dict.__setitem__("_as_immutable",new PyMethodDescr("_as_immutable",PySet.class,0,0,new exposed__as_immutable(null,null)));
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
                ((PySet)self).Set_init(args,keywords);
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
                                                                                                 newobj.Set_init(args,keywords);
                                                                                         } else {
                                                                                             newobj=new PySetDerived(subtype);
                                                                                         }
                                                                                         return newobj;
                                                                                     }

                                                                                 });
    }
    //~ END GENERATED REGION -- DO NOT EDIT SEE gexpose.py

    final void Set_init(PyObject[] args, String[] kwds) {
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
        return Set___ior__(other);
    }

    final PyObject Set___ior__(PyObject other) {
        BaseSet bs = this._binary_sanity_check(other);
        this._set.addAll(bs._set);
        return this;
    }

    public PyObject __ixor__(PyObject other) {
        return Set___ixor__(other);
    }

    final PyObject Set___ixor__(PyObject other) {
        this._binary_sanity_check(other);
        Set_symmetric_difference_update(other);
        return this;
    }

    public PyObject __iand__(PyObject other) {
        return Set___iand__(other);
    }

    final PyObject Set___iand__(PyObject other) {
        BaseSet bs = this._binary_sanity_check(other);
        this._set = ((BaseSet) this.__and__(bs))._set;
        return this;
    }

    public PyObject __isub__(PyObject other) {
        return Set___isub__(other);
    }

    final PyObject Set___isub__(PyObject other) {
        BaseSet bs = this._binary_sanity_check(other);
        this._set.removeAll(bs._set);
        return this;
    }

    public int hashCode() {
        return Set_hashCode();
    }

    final int Set_hashCode() {
        throw Py.TypeError("Can't hash a Set, only an ImmutableSet.");
    }

    final void Set_add(PyObject o) {
        try {
            this._set.add(o);
        } catch (PyException e) {
            PyObject immutable = this.asImmutable(e, o);
            this._set.add(immutable);
        }
    }

    final void Set_remove(PyObject o) {
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

    final void Set_discard(PyObject o) {
        try {
            this._set.remove(o);
        } catch (PyException e) {
            PyObject immutable = this.asImmutable(e, o);
            this._set.remove(immutable);
        }
    }

    final PyObject Set_pop() {
        Iterator iterator = this._set.iterator();
        Object first = iterator.next();
        this._set.remove(first);
        return (PyObject) first;
    }

    final void Set_clear() {
        this._set.clear();
    }

    final void Set_update(PyObject data) {
        this._update(data);
    }

    final void Set_union_update(PyObject other) {
        this._update(other);
    }

    final void Set_intersection_update(PyObject other) {
        if (other instanceof BaseSet) {
            this.__iand__(other);
        } else {
            BaseSet set = (BaseSet) baseset_intersection(other);
            this._set = set._set;
        }
    }

    final void Set_symmetric_difference_update(PyObject other) {
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

    final void Set_difference_update(PyObject other) {
        if (other instanceof BaseSet) {
            this.__isub__(other);
            return;
        }
        for (PyObject o : other.asIterable()) {
            if (this.__contains__(o)) {
                this._set.remove(o);
            }
        }
    }

    final PyObject Set__as_immutable() {
        return new PyImmutableSet(this);
    }
}
