package org.python.modules.sets;

import org.python.core.PyObject;
import org.python.core.PyFloat;
import org.python.core.PyComplex;
import org.python.core.PyType;
import org.python.core.Py;
import org.python.core.PyString;
import org.python.core.PyException;
import org.python.core.PyLong;
import org.python.core.PyInteger;
import org.python.core.ThreadState;
import org.python.core.PySequenceIter;

public class PySetDerived extends PySet {

    private PyObject dict;

    public PyObject fastGetDict() {
        return dict;
    }

    public PyObject getDict() {
        return dict;
    }

    public PySetDerived(PyType subtype) {
        super(subtype);
        dict=subtype.instDict();
    }

    public PyString __str__() {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__str__");
        if (impl!=null) {
            PyObject res=impl.__get__(this,self_type).__call__();
            if (res instanceof PyString)
                return(PyString)res;
            throw Py.TypeError("__str__"+" should return a "+"string");
        }
        return super.__str__();
    }

    public PyString __repr__() {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__repr__");
        if (impl!=null) {
            PyObject res=impl.__get__(this,self_type).__call__();
            if (res instanceof PyString)
                return(PyString)res;
            throw Py.TypeError("__repr__"+" should return a "+"string");
        }
        return super.__repr__();
    }

    public PyString __hex__() {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__hex__");
        if (impl!=null) {
            PyObject res=impl.__get__(this,self_type).__call__();
            if (res instanceof PyString)
                return(PyString)res;
            throw Py.TypeError("__hex__"+" should return a "+"string");
        }
        return super.__hex__();
    }

    public PyString __oct__() {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__oct__");
        if (impl!=null) {
            PyObject res=impl.__get__(this,self_type).__call__();
            if (res instanceof PyString)
                return(PyString)res;
            throw Py.TypeError("__oct__"+" should return a "+"string");
        }
        return super.__oct__();
    }

    public PyObject __int__() {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__int__");
        if (impl!=null) {
            PyObject res=impl.__get__(this,self_type).__call__();
            if (res instanceof PyInteger)
                return(PyInteger)res;
            throw Py.TypeError("__int__"+" should return a "+"int");
        }
        return super.__int__();
    }

    public PyFloat __float__() {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__float__");
        if (impl!=null) {
            PyObject res=impl.__get__(this,self_type).__call__();
            if (res instanceof PyFloat)
                return(PyFloat)res;
            throw Py.TypeError("__float__"+" should return a "+"float");
        }
        return super.__float__();
    }

    public PyLong __long__() {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__long__");
        if (impl!=null) {
            PyObject res=impl.__get__(this,self_type).__call__();
            if (res instanceof PyLong)
                return(PyLong)res;
            throw Py.TypeError("__long__"+" should return a "+"long");
        }
        return super.__long__();
    }

    public PyComplex __complex__() {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__complex__");
        if (impl!=null) {
            PyObject res=impl.__get__(this,self_type).__call__();
            if (res instanceof PyComplex)
                return(PyComplex)res;
            throw Py.TypeError("__complex__"+" should return a "+"complex");
        }
        return super.__complex__();
    }

    public PyObject __pos__() {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__pos__");
        if (impl!=null)
            return impl.__get__(this,self_type).__call__();
        return super.__pos__();
    }

    public PyObject __neg__() {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__neg__");
        if (impl!=null)
            return impl.__get__(this,self_type).__call__();
        return super.__neg__();
    }

    public PyObject __abs__() {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__abs__");
        if (impl!=null)
            return impl.__get__(this,self_type).__call__();
        return super.__abs__();
    }

    public PyObject __invert__() {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__invert__");
        if (impl!=null)
            return impl.__get__(this,self_type).__call__();
        return super.__invert__();
    }

    public PyObject __add__(PyObject other) {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__add__");
        if (impl!=null) {
            PyObject res=impl.__get__(this,self_type).__call__(other);
            if (res==Py.NotImplemented)
                return null;
            return res;
        }
        return super.__add__(other);
    }

    public PyObject __radd__(PyObject other) {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__radd__");
        if (impl!=null) {
            PyObject res=impl.__get__(this,self_type).__call__(other);
            if (res==Py.NotImplemented)
                return null;
            return res;
        }
        return super.__radd__(other);
    }

    public PyObject __sub__(PyObject other) {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__sub__");
        if (impl!=null) {
            PyObject res=impl.__get__(this,self_type).__call__(other);
            if (res==Py.NotImplemented)
                return null;
            return res;
        }
        return super.__sub__(other);
    }

    public PyObject __rsub__(PyObject other) {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__rsub__");
        if (impl!=null) {
            PyObject res=impl.__get__(this,self_type).__call__(other);
            if (res==Py.NotImplemented)
                return null;
            return res;
        }
        return super.__rsub__(other);
    }

    public PyObject __mul__(PyObject other) {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__mul__");
        if (impl!=null) {
            PyObject res=impl.__get__(this,self_type).__call__(other);
            if (res==Py.NotImplemented)
                return null;
            return res;
        }
        return super.__mul__(other);
    }

    public PyObject __rmul__(PyObject other) {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__rmul__");
        if (impl!=null) {
            PyObject res=impl.__get__(this,self_type).__call__(other);
            if (res==Py.NotImplemented)
                return null;
            return res;
        }
        return super.__rmul__(other);
    }

    public PyObject __div__(PyObject other) {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__div__");
        if (impl!=null) {
            PyObject res=impl.__get__(this,self_type).__call__(other);
            if (res==Py.NotImplemented)
                return null;
            return res;
        }
        return super.__div__(other);
    }

    public PyObject __rdiv__(PyObject other) {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__rdiv__");
        if (impl!=null) {
            PyObject res=impl.__get__(this,self_type).__call__(other);
            if (res==Py.NotImplemented)
                return null;
            return res;
        }
        return super.__rdiv__(other);
    }

    public PyObject __floordiv__(PyObject other) {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__floordiv__");
        if (impl!=null) {
            PyObject res=impl.__get__(this,self_type).__call__(other);
            if (res==Py.NotImplemented)
                return null;
            return res;
        }
        return super.__floordiv__(other);
    }

    public PyObject __rfloordiv__(PyObject other) {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__rfloordiv__");
        if (impl!=null) {
            PyObject res=impl.__get__(this,self_type).__call__(other);
            if (res==Py.NotImplemented)
                return null;
            return res;
        }
        return super.__rfloordiv__(other);
    }

    public PyObject __truediv__(PyObject other) {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__truediv__");
        if (impl!=null) {
            PyObject res=impl.__get__(this,self_type).__call__(other);
            if (res==Py.NotImplemented)
                return null;
            return res;
        }
        return super.__truediv__(other);
    }

    public PyObject __rtruediv__(PyObject other) {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__rtruediv__");
        if (impl!=null) {
            PyObject res=impl.__get__(this,self_type).__call__(other);
            if (res==Py.NotImplemented)
                return null;
            return res;
        }
        return super.__rtruediv__(other);
    }

    public PyObject __mod__(PyObject other) {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__mod__");
        if (impl!=null) {
            PyObject res=impl.__get__(this,self_type).__call__(other);
            if (res==Py.NotImplemented)
                return null;
            return res;
        }
        return super.__mod__(other);
    }

    public PyObject __rmod__(PyObject other) {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__rmod__");
        if (impl!=null) {
            PyObject res=impl.__get__(this,self_type).__call__(other);
            if (res==Py.NotImplemented)
                return null;
            return res;
        }
        return super.__rmod__(other);
    }

    public PyObject __divmod__(PyObject other) {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__divmod__");
        if (impl!=null) {
            PyObject res=impl.__get__(this,self_type).__call__(other);
            if (res==Py.NotImplemented)
                return null;
            return res;
        }
        return super.__divmod__(other);
    }

    public PyObject __rdivmod__(PyObject other) {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__rdivmod__");
        if (impl!=null) {
            PyObject res=impl.__get__(this,self_type).__call__(other);
            if (res==Py.NotImplemented)
                return null;
            return res;
        }
        return super.__rdivmod__(other);
    }

    public PyObject __pow__(PyObject other) {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__pow__");
        if (impl!=null) {
            PyObject res=impl.__get__(this,self_type).__call__(other);
            if (res==Py.NotImplemented)
                return null;
            return res;
        }
        return super.__pow__(other);
    }

    public PyObject __rpow__(PyObject other) {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__rpow__");
        if (impl!=null) {
            PyObject res=impl.__get__(this,self_type).__call__(other);
            if (res==Py.NotImplemented)
                return null;
            return res;
        }
        return super.__rpow__(other);
    }

    public PyObject __lshift__(PyObject other) {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__lshift__");
        if (impl!=null) {
            PyObject res=impl.__get__(this,self_type).__call__(other);
            if (res==Py.NotImplemented)
                return null;
            return res;
        }
        return super.__lshift__(other);
    }

    public PyObject __rlshift__(PyObject other) {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__rlshift__");
        if (impl!=null) {
            PyObject res=impl.__get__(this,self_type).__call__(other);
            if (res==Py.NotImplemented)
                return null;
            return res;
        }
        return super.__rlshift__(other);
    }

    public PyObject __rshift__(PyObject other) {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__rshift__");
        if (impl!=null) {
            PyObject res=impl.__get__(this,self_type).__call__(other);
            if (res==Py.NotImplemented)
                return null;
            return res;
        }
        return super.__rshift__(other);
    }

    public PyObject __rrshift__(PyObject other) {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__rrshift__");
        if (impl!=null) {
            PyObject res=impl.__get__(this,self_type).__call__(other);
            if (res==Py.NotImplemented)
                return null;
            return res;
        }
        return super.__rrshift__(other);
    }

    public PyObject __and__(PyObject other) {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__and__");
        if (impl!=null) {
            PyObject res=impl.__get__(this,self_type).__call__(other);
            if (res==Py.NotImplemented)
                return null;
            return res;
        }
        return super.__and__(other);
    }

    public PyObject __rand__(PyObject other) {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__rand__");
        if (impl!=null) {
            PyObject res=impl.__get__(this,self_type).__call__(other);
            if (res==Py.NotImplemented)
                return null;
            return res;
        }
        return super.__rand__(other);
    }

    public PyObject __or__(PyObject other) {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__or__");
        if (impl!=null) {
            PyObject res=impl.__get__(this,self_type).__call__(other);
            if (res==Py.NotImplemented)
                return null;
            return res;
        }
        return super.__or__(other);
    }

    public PyObject __ror__(PyObject other) {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__ror__");
        if (impl!=null) {
            PyObject res=impl.__get__(this,self_type).__call__(other);
            if (res==Py.NotImplemented)
                return null;
            return res;
        }
        return super.__ror__(other);
    }

    public PyObject __xor__(PyObject other) {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__xor__");
        if (impl!=null) {
            PyObject res=impl.__get__(this,self_type).__call__(other);
            if (res==Py.NotImplemented)
                return null;
            return res;
        }
        return super.__xor__(other);
    }

    public PyObject __rxor__(PyObject other) {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__rxor__");
        if (impl!=null) {
            PyObject res=impl.__get__(this,self_type).__call__(other);
            if (res==Py.NotImplemented)
                return null;
            return res;
        }
        return super.__rxor__(other);
    }

    public PyObject __lt__(PyObject other) {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__lt__");
        if (impl!=null) {
            PyObject res=impl.__get__(this,self_type).__call__(other);
            if (res==Py.NotImplemented)
                return null;
            return res;
        }
        return super.__lt__(other);
    }

    public PyObject __le__(PyObject other) {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__le__");
        if (impl!=null) {
            PyObject res=impl.__get__(this,self_type).__call__(other);
            if (res==Py.NotImplemented)
                return null;
            return res;
        }
        return super.__le__(other);
    }

    public PyObject __gt__(PyObject other) {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__gt__");
        if (impl!=null) {
            PyObject res=impl.__get__(this,self_type).__call__(other);
            if (res==Py.NotImplemented)
                return null;
            return res;
        }
        return super.__gt__(other);
    }

    public PyObject __ge__(PyObject other) {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__ge__");
        if (impl!=null) {
            PyObject res=impl.__get__(this,self_type).__call__(other);
            if (res==Py.NotImplemented)
                return null;
            return res;
        }
        return super.__ge__(other);
    }

    public PyObject __eq__(PyObject other) {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__eq__");
        if (impl!=null) {
            PyObject res=impl.__get__(this,self_type).__call__(other);
            if (res==Py.NotImplemented)
                return null;
            return res;
        }
        return super.__eq__(other);
    }

    public PyObject __ne__(PyObject other) {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__ne__");
        if (impl!=null) {
            PyObject res=impl.__get__(this,self_type).__call__(other);
            if (res==Py.NotImplemented)
                return null;
            return res;
        }
        return super.__ne__(other);
    }

    public PyObject __iadd__(PyObject other) {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__iadd__");
        if (impl!=null)
            return impl.__get__(this,self_type).__call__(other);
        return super.__iadd__(other);
    }

    public PyObject __isub__(PyObject other) {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__isub__");
        if (impl!=null)
            return impl.__get__(this,self_type).__call__(other);
        return super.__isub__(other);
    }

    public PyObject __imul__(PyObject other) {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__imul__");
        if (impl!=null)
            return impl.__get__(this,self_type).__call__(other);
        return super.__imul__(other);
    }

    public PyObject __idiv__(PyObject other) {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__idiv__");
        if (impl!=null)
            return impl.__get__(this,self_type).__call__(other);
        return super.__idiv__(other);
    }

    public PyObject __ifloordiv__(PyObject other) {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__ifloordiv__");
        if (impl!=null)
            return impl.__get__(this,self_type).__call__(other);
        return super.__ifloordiv__(other);
    }

    public PyObject __itruediv__(PyObject other) {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__itruediv__");
        if (impl!=null)
            return impl.__get__(this,self_type).__call__(other);
        return super.__itruediv__(other);
    }

    public PyObject __imod__(PyObject other) {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__imod__");
        if (impl!=null)
            return impl.__get__(this,self_type).__call__(other);
        return super.__imod__(other);
    }

    public PyObject __ipow__(PyObject other) {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__ipow__");
        if (impl!=null)
            return impl.__get__(this,self_type).__call__(other);
        return super.__ipow__(other);
    }

    public PyObject __ilshift__(PyObject other) {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__ilshift__");
        if (impl!=null)
            return impl.__get__(this,self_type).__call__(other);
        return super.__ilshift__(other);
    }

    public PyObject __irshift__(PyObject other) {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__irshift__");
        if (impl!=null)
            return impl.__get__(this,self_type).__call__(other);
        return super.__irshift__(other);
    }

    public PyObject __iand__(PyObject other) {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__iand__");
        if (impl!=null)
            return impl.__get__(this,self_type).__call__(other);
        return super.__iand__(other);
    }

    public PyObject __ior__(PyObject other) {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__ior__");
        if (impl!=null)
            return impl.__get__(this,self_type).__call__(other);
        return super.__ior__(other);
    }

    public PyObject __ixor__(PyObject other) {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__ixor__");
        if (impl!=null)
            return impl.__get__(this,self_type).__call__(other);
        return super.__ixor__(other);
    }

    /*
    public String toString() {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__repr__");
        if (impl!=null) {
            PyObject res=impl.__get__(this,self_type).__call__();
            if (!(res instanceof PyString))
                throw Py.TypeError("__repr__ should return a string");
            return((PyString)res).toString();
        }
        return super.toString();
    }
    */

    public int hashCode() {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__hash__");
        if (impl!=null) {
            PyObject res=impl.__get__(this,self_type).__call__();
            if (res instanceof PyInteger)
                return((PyInteger)res).getValue();
            throw Py.TypeError("__hash__ should return a int");
        }
        if (self_type.lookup("__eq__")!=null||self_type.lookup("__cmp__")!=null)
            throw Py.TypeError("unhashable type");
        return super.hashCode();
    }

    public int __cmp__(PyObject other) {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__cmp__");
        if (impl!=null) {
            PyObject res=impl.__get__(this,self_type).__call__(other);
            if (res instanceof PyInteger) {
                int v=((PyInteger)res).getValue();
                return v<0?-1:v>0?1:0;
            }
            throw Py.TypeError("__cmp__ should return a int");
        }
        return super.__cmp__(other);
    }

    public boolean __nonzero__() {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__nonzero__");
        if (impl==null) {
            impl=self_type.lookup("__len__");
            if (impl==null)
                return super.__nonzero__();
        }
        return impl.__get__(this,self_type).__call__().__nonzero__();
    }

    public boolean __contains__(PyObject o) {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__contains__");
        if (impl==null)
            return super.__contains__(o);
        return impl.__get__(this,self_type).__call__(o).__nonzero__();
    }

    public int __len__() {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__len__");
        if (impl!=null) {
            PyObject res=impl.__get__(this,self_type).__call__();
            if (res instanceof PyInteger)
                return((PyInteger)res).getValue();
            throw Py.TypeError("__len__ should return a int");
        }
        return super.__len__();
    }

    public PyObject __iter__() {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__iter__");
        if (impl!=null)
            return impl.__get__(this,self_type).__call__();
        impl=self_type.lookup("__getitem__");
        if (impl==null)
            return super.__iter__();
        return new PySequenceIter(this);
    }

    public PyObject __iternext__() {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("next");
        if (impl!=null) {
            try {
                return impl.__get__(this,self_type).__call__();
            } catch (PyException exc) {
                if (Py.matchException(exc,Py.StopIteration))
                    return null;
                throw exc;
            }
        }
        return super.__iternext__(); // ???
    }

    public PyObject __finditem__(PyObject key) { // ???
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__getitem__");
        if (impl!=null)
            try {
                return impl.__get__(this,self_type).__call__(key);
            } catch (PyException exc) {
                if (Py.matchException(exc,Py.LookupError))
                    return null;
                throw exc;
            }
        return super.__finditem__(key);
    }

    public void __setitem__(PyObject key,PyObject value) { // ???
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__setitem__");
        if (impl!=null) {
            impl.__get__(this,self_type).__call__(key,value);
            return;
        }
        super.__setitem__(key,value);
    }

    public void __delitem__(PyObject key) { // ???
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__delitem__");
        if (impl!=null) {
            impl.__get__(this,self_type).__call__(key);
            return;
        }
        super.__delitem__(key);
    }

    public PyObject __call__(PyObject args[],String keywords[]) {
        ThreadState ts=Py.getThreadState();
        if (ts.recursion_depth++>ts.systemState.getrecursionlimit())
            throw Py.RuntimeError("maximum __call__ recursion depth exceeded");
        try {
            PyType self_type=getType();
            PyObject impl=self_type.lookup("__call__");
            if (impl!=null)
                return impl.__get__(this,self_type).__call__(args,keywords);
            return super.__call__(args,keywords);
        } finally {
            --ts.recursion_depth;
        }
    }

    public PyObject __findattr__(String name) {
        PyType self_type=getType();
        PyObject getattribute=self_type.lookup("__getattribute__");
        PyString py_name=null;
        try {
            if (getattribute!=null) {
                return getattribute.__get__(this,self_type).__call__(py_name=new PyString(name));
            } else {
                return super.__findattr__(name);
            }
        } catch (PyException e) {
            if (Py.matchException(e,Py.AttributeError)) {
                PyObject getattr=self_type.lookup("__getattr__");
                if (getattr!=null)
                    try {
                        return getattr.__get__(this,self_type).__call__(py_name!=null?py_name:new PyString(name));
                    } catch (PyException e1) {
                        if (!Py.matchException(e1,Py.AttributeError))
                            throw e1;
                    }
                return null;
            }
            throw e;
        }
    }

    public void __setattr__(String name,PyObject value) {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__setattr__");
        if (impl!=null) {
            impl.__get__(this,self_type).__call__(new PyString(name),value);
            return;
        }
        super.__setattr__(name,value);
    }

    public void __delattr__(String name) {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__delattr__");
        if (impl!=null) {
            impl.__get__(this,self_type).__call__(new PyString(name));
            return;
        }
        super.__delattr__(name);
    }

    public PyObject __get__(PyObject obj,PyObject type) {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__get__");
        if (impl!=null) {
            if (obj==null)
                obj=Py.None;
            if (type==null)
                type=Py.None;
            return impl.__get__(this,self_type).__call__(obj,type);
        }
        return super.__get__(obj,type);
    }

    public void __set__(PyObject obj,PyObject value) {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__set__");
        if (impl!=null) {
            impl.__get__(this,self_type).__call__(obj,value);
            return;
        }
        super.__set__(obj,value);
    }

    public void __delete__(PyObject obj) {
        PyType self_type=getType();
        PyObject impl=self_type.lookup("__delete__");
        if (impl!=null) {
            impl.__get__(this,self_type).__call__(obj);
            return;
        }
        super.__delete__(obj);
    }

    public void dispatch__init__(PyType type,PyObject[]args,String[]keywords) {
        PyType self_type=getType();
        if (self_type.isSubType(type)) {
            PyObject impl=self_type.lookup("__init__");
            if (impl!=null)
                impl.__get__(this,self_type).__call__(args,keywords);
        }
    }

}
