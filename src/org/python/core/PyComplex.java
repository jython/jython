// Copyright (c) Corporation for National Research Initiatives
package org.python.core;

import org.python.expose.ExposedGet;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedType;
import org.python.expose.MethodType;

/**
 * A builtin python complex number
 */
@ExposedType(name = "complex")
public class PyComplex extends PyObject {

    public static final PyType TYPE = PyType.fromClass(PyComplex.class);

    @ExposedGet
    public double real, imag;

    static PyComplex J = new PyComplex(0, 1.);

    @ExposedNew
    public static PyObject complex_new(PyNewWrapper new_, boolean init, PyType subtype,
                                       PyObject[] args, String[] keywords) {
        ArgParser ap = new ArgParser("complex", args, keywords, "real", "imag");
        PyObject real = ap.getPyObject(0, Py.Zero);
        PyObject imag = ap.getPyObject(1, null);

        // Special-case for single argument that is already complex
        if (real.getType() == TYPE && new_.for_type == subtype && imag == null) {
            return real;
        }
        if (real instanceof PyString) {
            if (imag != null) {
                throw Py.TypeError("complex() can't take second arg if first is a string");
            }
            return real.__complex__();
        }
        if (imag != null && imag instanceof PyString) {
            throw Py.TypeError("complex() second arg can't be a string");
        }

        try {
            real = real.__complex__();
        } catch (PyException pye) {
            if (!Py.matchException(pye, Py.AttributeError)) {
                // __complex__ not supported
                throw pye;
            }
            // otherwise try other means
        }

        PyComplex complexReal;
        PyComplex complexImag;
        PyFloat toFloat = null;
        if (real instanceof PyComplex) {
            complexReal = (PyComplex)real;
        } else {
            try {
                toFloat = real.__float__();
            } catch (PyException pye) {
                if (Py.matchException(pye, Py.AttributeError)) {
                    // __float__ not supported
                    throw Py.TypeError("complex() argument must be a string or a number");
                }
                throw pye;
            }
            complexReal = new PyComplex(toFloat.getValue());
        }

        if (imag == null) {
            complexImag = new PyComplex(0.0);
        } else if (imag instanceof PyComplex) {
            complexImag = (PyComplex)imag;
        } else {
            toFloat = null;
            try {
                toFloat = imag.__float__();
            } catch (PyException pye) {
                if (Py.matchException(pye, Py.AttributeError)) {
                    // __float__ not supported
                    throw Py.TypeError("complex() argument must be a string or a number");
                }
                throw pye;
            }
            complexImag = new PyComplex(toFloat.getValue());
        }

        complexReal.real -= complexImag.imag;
        complexReal.imag += complexImag.real;
        if (new_.for_type != subtype) {
            complexReal = new PyComplexDerived(subtype, complexReal.real, complexReal.imag);
        }
        return complexReal;
    }

    public PyComplex(PyType subtype, double r, double i) {
        super(subtype);
        real = r;
        imag = i;
    }

    public PyComplex(double r, double i) {
        this(TYPE, r, i);
    }

    public PyComplex(double r) {
        this(r, 0.0);
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

    @ExposedMethod(names = {"__repr__", "__str__"})
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
        return complex___hash__();
    }

    @ExposedMethod
    final int complex___hash__() {
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

    @ExposedMethod
    final boolean complex___nonzero__() {
        return real != 0 && imag != 0;
    }

    /*public Object __tojava__(Class c) {
      return super.__tojava__(c);
      }*/

    public int __cmp__(PyObject other) {
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

    @ExposedMethod(type = MethodType.BINARY)
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

    @ExposedMethod(type = MethodType.BINARY)
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

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject complex___ge__(PyObject other) {
        return unsupported_comparison(other);
    }

    public PyObject __gt__(PyObject other) {
        return complex___gt__(other);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject complex___gt__(PyObject other) {
        return unsupported_comparison(other);
    }

    public PyObject __le__(PyObject other) {
        return complex___le__(other);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject complex___le__(PyObject other) {
        return unsupported_comparison(other);
    }

    public PyObject __lt__(PyObject other) {
        return complex___lt__(other);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject complex___lt__(PyObject other) {
        return unsupported_comparison(other);
    }

    public Object __coerce_ex__(PyObject other) {
        return complex___coerce_ex__(other);
    }
    
    @ExposedMethod
    final PyObject complex___coerce__(PyObject other) {
        return adaptToCoerceTuple(complex___coerce_ex__(other));
    }

    /** 
     * Coercion logic for complex. Implemented as a final method to avoid
     * invocation of virtual methods from the exposed coerce. 
     */ 
    final PyObject complex___coerce_ex__(PyObject other) {
        if (other instanceof PyComplex)
            return other;
        if (other instanceof PyFloat)
            return new PyComplex(((PyFloat)other).getValue(), 0);
        if (other instanceof PyInteger)
            return new PyComplex(((PyInteger)other).getValue(), 0);
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
            return new PyComplex(((PyInteger)other).getValue(), 0);
        if (other instanceof PyLong)
            return new PyComplex(((PyLong)other).doubleValue(), 0);
        throw Py.TypeError("xxx");
    }

    public PyObject __add__(PyObject right) {
        return complex___add__(right);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject complex___add__(PyObject right) {
        if (!canCoerce(right))
            return null;
        PyComplex c = coerce(right);
        return new PyComplex(real+c.real, imag+c.imag);
    }

    public PyObject __radd__(PyObject left) {
        return complex___radd__(left);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject complex___radd__(PyObject left) {
        return __add__(left);
    }

    private final static PyObject _sub(PyComplex o1, PyComplex o2) {
        return new PyComplex(o1.real-o2.real, o1.imag-o2.imag);
    }

    public PyObject __sub__(PyObject right) {
        return complex___sub__(right);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject complex___sub__(PyObject right) {
        if (!canCoerce(right))
            return null;
        return _sub(this, coerce(right));
    }

    public PyObject __rsub__(PyObject left) {
        return complex___rsub__(left);
    }

    @ExposedMethod(type = MethodType.BINARY)
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

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject complex___mul__(PyObject right) {
        if (!canCoerce(right))
            return null;
        return _mul(this, coerce(right));
    }

    public PyObject __rmul__(PyObject left) {
        return complex___rmul__(left);
    }

    @ExposedMethod(type = MethodType.BINARY)
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

    @ExposedMethod(type = MethodType.BINARY)
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

    @ExposedMethod(type = MethodType.BINARY)
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

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject complex___floordiv__(PyObject right) {
        if (!canCoerce(right))
            return null;
        return _divmod(this, coerce(right)).__finditem__(0);
    }

    public PyObject __rfloordiv__(PyObject left) {
        return complex___rfloordiv__(left);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject complex___rfloordiv__(PyObject left) {
        if (!canCoerce(left))
            return null;
        return _divmod(coerce(left), this).__finditem__(0);
    }

    public PyObject __truediv__(PyObject right) {
        return complex___truediv__(right);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject complex___truediv__(PyObject right) {
        if (!canCoerce(right))
            return null;
        return _div(this, coerce(right));
    }

    public PyObject __rtruediv__(PyObject left) {
        return complex___rtruediv__(left);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject complex___rtruediv__(PyObject left) {
        if (!canCoerce(left))
            return null;
        return _div(coerce(left), this);
    }

    public PyObject __mod__(PyObject right) {
        return complex___mod__(right);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject complex___mod__(PyObject right) {
        if (!canCoerce(right))
            return null;
        return _mod(this, coerce(right));
    }

    public PyObject __rmod__(PyObject left) {
        return complex___rmod__(left);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject complex___rmod__(PyObject left) {
        if (!canCoerce(left))
            return null;
        return _mod(coerce(left), this);
    }

    private static PyObject _mod(PyComplex value, PyComplex right) {
        Py.warning(Py.DeprecationWarning, "complex divmod(), // and % are deprecated");
        PyComplex z = (PyComplex) _div(value, right);

        z.real = Math.floor(z.real);
        z.imag = 0.0;

        return value.__sub__(z.__mul__(right));
    }

    public PyObject __divmod__(PyObject right) {
        return complex___divmod__(right);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject complex___divmod__(PyObject right) {
        if (!canCoerce(right))
            return null;
        return _divmod(this, coerce(right));
    }

    public PyObject __rdivmod__(PyObject left) {
        return complex___rdivmod__(left);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject complex___rdivmod__(PyObject left) {
        if (!canCoerce(left))
            return null;
        return _divmod(coerce(left), this);
    }

    private static PyObject _divmod(PyComplex value, PyComplex right) {
        Py.warning(Py.DeprecationWarning, "complex divmod(), // and % are deprecated");
        PyComplex z = (PyComplex) _div(value, right);

        z.real = Math.floor(z.real);
        z.imag = 0.0;

        return new PyTuple(z, value.__sub__(z.__mul__(right)));
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

    @ExposedMethod(defaults = "null")
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

    @ExposedMethod(type = MethodType.BINARY)
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
                throw Py.ZeroDivisionError("0.0 to a negative or complex power");
            }
        }

        // Check for integral powers
        int iexp = (int)yr;
        if (yi == 0 && yr == iexp && iexp >= -128 && iexp <= 128) {
            return ipow(value, iexp);
        }

        double abs = Math.hypot(xr, xi);
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

    @ExposedMethod
    final PyObject complex___neg__() {
        return new PyComplex(-real, -imag);
    }

    public PyObject __pos__() {
        return complex___pos__();
    }

    @ExposedMethod
    final PyObject complex___pos__() {
        return new PyComplex(real, imag);
    }

    public PyObject __invert__() {
      throw Py.TypeError("bad operand type for unary ~");
    }

    public PyObject __abs__() {
        return complex___abs__();
    }

    @ExposedMethod
    final PyObject complex___abs__() {
        return new PyFloat(Math.hypot(real, imag));
    }

    public PyObject __int__() {
        return complex___int__();
    }

    @ExposedMethod
    final PyInteger complex___int__() {
        throw Py.TypeError(
            "can't convert complex to int; use e.g. int(abs(z))");
    }

    public PyObject __long__() {
        return complex___long__();
    }

    @ExposedMethod
    final PyObject complex___long__() {
        throw Py.TypeError(
            "can't convert complex to long; use e.g. long(abs(z))");
    }

    public PyFloat __float__() {
        return complex___float__();
    }

    @ExposedMethod
    final PyFloat complex___float__() {
        throw Py.TypeError("can't convert complex to float; use e.g. abs(z)");
    }

    public PyComplex __complex__() {
        return new PyComplex(real, imag);
    }

    public PyComplex conjugate() {
        return complex_conjugate();
    }

    @ExposedMethod
    final PyComplex complex_conjugate() {
        return new PyComplex(real, -imag);
    }

    @ExposedMethod
    final PyTuple complex___getnewargs__() {
        return new PyTuple(new PyComplex(real, imag));
    }

    public PyTuple __getnewargs__() {
        return complex___getnewargs__();
    }

    public boolean isMappingType() { return false; }
    public boolean isSequenceType() { return false; }
}
