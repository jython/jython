// Copyright (c) Corporation for National Research Initiatives
package org.python.core;

import java.io.Serializable;

/**
 * A builtin python float.
 */

public class PyFloat extends PyObject
{
    //~ BEGIN GENERATED REGION -- DO NOT EDIT SEE gexpose.py
    /* type info */

    public static final String exposed_name="float";

    public static void typeSetup(PyObject dict,PyType.Newstyle marker) {
        class exposed___abs__ extends PyBuiltinMethodNarrow {

            exposed___abs__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___abs__(self,info);
            }

            public PyObject __call__() {
                return((PyFloat)self).float___abs__();
            }

        }
        dict.__setitem__("__abs__",new PyMethodDescr("__abs__",PyFloat.class,0,0,new exposed___abs__(null,null)));
        class exposed___float__ extends PyBuiltinMethodNarrow {

            exposed___float__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___float__(self,info);
            }

            public PyObject __call__() {
                return((PyFloat)self).float___float__();
            }

        }
        dict.__setitem__("__float__",new PyMethodDescr("__float__",PyFloat.class,0,0,new exposed___float__(null,null)));
        class exposed___int__ extends PyBuiltinMethodNarrow {

            exposed___int__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___int__(self,info);
            }

            public PyObject __call__() {
                return((PyFloat)self).float___int__();
            }

        }
        dict.__setitem__("__int__",new PyMethodDescr("__int__",PyFloat.class,0,0,new exposed___int__(null,null)));
        class exposed___long__ extends PyBuiltinMethodNarrow {

            exposed___long__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___long__(self,info);
            }

            public PyObject __call__() {
                return((PyFloat)self).float___long__();
            }

        }
        dict.__setitem__("__long__",new PyMethodDescr("__long__",PyFloat.class,0,0,new exposed___long__(null,null)));
        class exposed___neg__ extends PyBuiltinMethodNarrow {

            exposed___neg__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___neg__(self,info);
            }

            public PyObject __call__() {
                return((PyFloat)self).float___neg__();
            }

        }
        dict.__setitem__("__neg__",new PyMethodDescr("__neg__",PyFloat.class,0,0,new exposed___neg__(null,null)));
        class exposed___pos__ extends PyBuiltinMethodNarrow {

            exposed___pos__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___pos__(self,info);
            }

            public PyObject __call__() {
                return((PyFloat)self).float___pos__();
            }

        }
        dict.__setitem__("__pos__",new PyMethodDescr("__pos__",PyFloat.class,0,0,new exposed___pos__(null,null)));
        class exposed___add__ extends PyBuiltinMethodNarrow {

            exposed___add__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___add__(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=((PyFloat)self).float___add__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__add__",new PyMethodDescr("__add__",PyFloat.class,1,1,new exposed___add__(null,null)));
        class exposed___div__ extends PyBuiltinMethodNarrow {

            exposed___div__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___div__(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=((PyFloat)self).float___div__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__div__",new PyMethodDescr("__div__",PyFloat.class,1,1,new exposed___div__(null,null)));
        class exposed___divmod__ extends PyBuiltinMethodNarrow {

            exposed___divmod__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___divmod__(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=((PyFloat)self).float___divmod__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__divmod__",new PyMethodDescr("__divmod__",PyFloat.class,1,1,new exposed___divmod__(null,null)));
        class exposed___floordiv__ extends PyBuiltinMethodNarrow {

            exposed___floordiv__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___floordiv__(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=((PyFloat)self).float___floordiv__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__floordiv__",new PyMethodDescr("__floordiv__",PyFloat.class,1,1,new exposed___floordiv__(null,null)));
        class exposed___mod__ extends PyBuiltinMethodNarrow {

            exposed___mod__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___mod__(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=((PyFloat)self).float___mod__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__mod__",new PyMethodDescr("__mod__",PyFloat.class,1,1,new exposed___mod__(null,null)));
        class exposed___mul__ extends PyBuiltinMethodNarrow {

            exposed___mul__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___mul__(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=((PyFloat)self).float___mul__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__mul__",new PyMethodDescr("__mul__",PyFloat.class,1,1,new exposed___mul__(null,null)));
        class exposed___radd__ extends PyBuiltinMethodNarrow {

            exposed___radd__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___radd__(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=((PyFloat)self).float___radd__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__radd__",new PyMethodDescr("__radd__",PyFloat.class,1,1,new exposed___radd__(null,null)));
        class exposed___rdiv__ extends PyBuiltinMethodNarrow {

            exposed___rdiv__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___rdiv__(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=((PyFloat)self).float___rdiv__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__rdiv__",new PyMethodDescr("__rdiv__",PyFloat.class,1,1,new exposed___rdiv__(null,null)));
        class exposed___rfloordiv__ extends PyBuiltinMethodNarrow {

            exposed___rfloordiv__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___rfloordiv__(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=((PyFloat)self).float___rfloordiv__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__rfloordiv__",new PyMethodDescr("__rfloordiv__",PyFloat.class,1,1,new exposed___rfloordiv__(null,null)));
        class exposed___rmod__ extends PyBuiltinMethodNarrow {

            exposed___rmod__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___rmod__(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=((PyFloat)self).float___rmod__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__rmod__",new PyMethodDescr("__rmod__",PyFloat.class,1,1,new exposed___rmod__(null,null)));
        class exposed___rmul__ extends PyBuiltinMethodNarrow {

            exposed___rmul__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___rmul__(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=((PyFloat)self).float___rmul__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__rmul__",new PyMethodDescr("__rmul__",PyFloat.class,1,1,new exposed___rmul__(null,null)));
        class exposed___rsub__ extends PyBuiltinMethodNarrow {

            exposed___rsub__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___rsub__(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=((PyFloat)self).float___rsub__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__rsub__",new PyMethodDescr("__rsub__",PyFloat.class,1,1,new exposed___rsub__(null,null)));
        class exposed___rtruediv__ extends PyBuiltinMethodNarrow {

            exposed___rtruediv__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___rtruediv__(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=((PyFloat)self).float___rtruediv__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__rtruediv__",new PyMethodDescr("__rtruediv__",PyFloat.class,1,1,new exposed___rtruediv__(null,null)));
        class exposed___sub__ extends PyBuiltinMethodNarrow {

            exposed___sub__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___sub__(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=((PyFloat)self).float___sub__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__sub__",new PyMethodDescr("__sub__",PyFloat.class,1,1,new exposed___sub__(null,null)));
        class exposed___truediv__ extends PyBuiltinMethodNarrow {

            exposed___truediv__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___truediv__(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=((PyFloat)self).float___truediv__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__truediv__",new PyMethodDescr("__truediv__",PyFloat.class,1,1,new exposed___truediv__(null,null)));
        class exposed___rdivmod__ extends PyBuiltinMethodNarrow {

            exposed___rdivmod__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___rdivmod__(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=((PyFloat)self).float___rdivmod__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__rdivmod__",new PyMethodDescr("__rdivmod__",PyFloat.class,1,1,new exposed___rdivmod__(null,null)));
        class exposed___rpow__ extends PyBuiltinMethodNarrow {

            exposed___rpow__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___rpow__(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=((PyFloat)self).float___rpow__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__rpow__",new PyMethodDescr("__rpow__",PyFloat.class,1,1,new exposed___rpow__(null,null)));
        class exposed___cmp__ extends PyBuiltinMethodNarrow {

            exposed___cmp__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___cmp__(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                int ret=((PyFloat)self).float___cmp__(arg0);
                if (ret==-2) {
                    throw Py.TypeError("float"+".__cmp__(x,y) requires y to be '"+"float"+"', not a '"+(arg0).getType().fastGetName()+"'");
                }
                return Py.newInteger(ret);
            }

        }
        dict.__setitem__("__cmp__",new PyMethodDescr("__cmp__",PyFloat.class,1,1,new exposed___cmp__(null,null)));
        class exposed___pow__ extends PyBuiltinMethodNarrow {

            exposed___pow__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___pow__(self,info);
            }

            public PyObject __call__(PyObject arg0,PyObject arg1) {
                PyObject ret=((PyFloat)self).float___pow__(arg0,arg1);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=((PyFloat)self).float___pow__(arg0,null);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__pow__",new PyMethodDescr("__pow__",PyFloat.class,1,2,new exposed___pow__(null,null)));
        class exposed___nonzero__ extends PyBuiltinMethodNarrow {

            exposed___nonzero__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___nonzero__(self,info);
            }

            public PyObject __call__() {
                return Py.newBoolean(((PyFloat)self).float___nonzero__());
            }

        }
        dict.__setitem__("__nonzero__",new PyMethodDescr("__nonzero__",PyFloat.class,0,0,new exposed___nonzero__(null,null)));
        class exposed___repr__ extends PyBuiltinMethodNarrow {

            exposed___repr__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___repr__(self,info);
            }

            public PyObject __call__() {
                return new PyString(((PyFloat)self).float_toString());
            }

        }
        dict.__setitem__("__repr__",new PyMethodDescr("__repr__",PyFloat.class,0,0,new exposed___repr__(null,null)));
        class exposed___str__ extends PyBuiltinMethodNarrow {

            exposed___str__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___str__(self,info);
            }

            public PyObject __call__() {
                return new PyString(((PyFloat)self).float_toString());
            }

        }
        dict.__setitem__("__str__",new PyMethodDescr("__str__",PyFloat.class,0,0,new exposed___str__(null,null)));
        class exposed___getnewargs__ extends PyBuiltinMethodNarrow {

            exposed___getnewargs__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___getnewargs__(self,info);
            }

            public PyObject __call__() {
                return((PyFloat)self).float___getnewargs__();
            }

        }
        dict.__setitem__("__getnewargs__",new PyMethodDescr("__getnewargs__",PyFloat.class,0,0,new exposed___getnewargs__(null,null)));
        class exposed___hash__ extends PyBuiltinMethodNarrow {

            exposed___hash__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___hash__(self,info);
            }

            public PyObject __call__() {
                return Py.newInteger(((PyFloat)self).float_hashCode());
            }

        }
        dict.__setitem__("__hash__",new PyMethodDescr("__hash__",PyFloat.class,0,0,new exposed___hash__(null,null)));
        dict.__setitem__("__new__",new PyNewWrapper(PyFloat.class,"__new__",-1,-1) {

                                                                                       public PyObject new_impl(boolean init,PyType subtype,PyObject[]args,String[]keywords) {
                                                                                           return float_new(this,init,subtype,args,keywords);
                                                                                       }

                                                                                   });
    }
    //~ END GENERATED REGION -- DO NOT EDIT SEE gexpose.py

    public static PyObject float_new(PyNewWrapper new_, boolean init, PyType subtype,
            PyObject[] args, String[] keywords) {
        ArgParser ap = new ArgParser("float", args, keywords, new String[] { "x" }, 0);
        PyObject x = ap.getPyObject(0, null);
        if (new_.for_type == subtype) {
            if (x == null) {
                return new PyFloat(0.0);
            }
            return x.__float__();
        } else {
            if (x == null) {
                return new PyFloatDerived(subtype, 0.0);
            }
            return new PyFloatDerived(subtype, x.__float__().getValue());
        }
    } // xxx

    private static final PyType FLOATTYPE = PyType.fromClass(PyFloat.class);

    private double value;

    public PyFloat(PyType subtype, double v) {
        super(subtype);
        value = v;
    }
    
    public PyFloat(double v) {
        this(FLOATTYPE, v);
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

    final PyObject float___rdivmod__(PyObject left) {
    	return __rdivmod__(left);
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
    
    final PyObject float___rpow__(PyObject left) {
    	return __rpow__(left);
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

        if (value < 0 && iw != Math.floor(iw))
            throw Py.ValueError("negative number cannot be raised to a fractional power");
            
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
        return Py.newFloat(value);
    }

    public PyObject __invert__() {
        throw Py.TypeError("bad operand type for unary ~");
    }

    public PyObject __abs__() {
        return float___abs__();
    }

    final PyObject float___abs__() {
        if (value >= 0)
            return Py.newFloat(value);
        else
            return __neg__();
    }

    public PyObject __int__() {
        return float___int__();
    }

    final PyObject float___int__() {
        if (value <= Integer.MAX_VALUE && value >= Integer.MIN_VALUE) {
            return new PyInteger((int)value);
        }
        return __long__();
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
        return Py.newFloat(value);
    }
    public PyComplex __complex__() {
        return new PyComplex(value, 0.);
    }

    final PyTuple float___getnewargs__() {
        return new PyTuple(new PyObject[] {new PyFloat(getValue())});
    }

    public PyTuple __getnewargs__() {
        return float___getnewargs__();
    }

    public boolean isMappingType() throws PyIgnoreMethodTag { return false; }
    public boolean isSequenceType() throws PyIgnoreMethodTag { return false; }

}
