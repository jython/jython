// Copyright © Corporation for National Research Initiatives
package org.python.core;

/**
 * A builtin python comples number
 */

public class PyComplex extends PyObject {
    public double real, imag;

    static PyComplex J = new PyComplex(0, 1.);

    public PyComplex(double r, double i) {
        real = r;
        imag = i;
    }

    public String safeRepr() throws PyIgnoreMethodTag {
        return "'complex' object";
    }

    /*public double getReal() {
      return real;
      }

      public double getImag() {
      return image;
      }*/

    public static String toString(double value) {
        if (value == Math.floor(value) &&
               value <= Long.MAX_VALUE && value >= Long.MIN_VALUE) {
            return Long.toString((long)value);
        } else {
            return Double.toString(value);
        }
    }

    public String toString() {
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
        if (imag == 0) {
            return new PyFloat(real).hashCode();
        } else {
            long v = Double.doubleToLongBits(real) ^
                Double.doubleToLongBits(imag);
            return (int)v ^ (int)(v >> 32);
        }
    }

    public boolean __nonzero__() {
        return real != 0 && imag != 0;
    }

    /*public Object __tojava__(Class c) {
      return super.__tojava__(c);
      }*/

    public int __cmp__(PyObject other) {
        double oreal = ((PyComplex)other).real;
        double oimag = ((PyComplex)other).imag;
        if (real == oreal && imag == oimag)
            return 0;
        if (real != oreal) {
            return real < oreal ? -1 : 1;
        } else {
            return imag < oimag ? -1 : 1;
        }
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
        if (!canCoerce(right))
            return null;
        PyComplex c = coerce(right);
        return new PyComplex(real+c.real, imag+c.imag);
    }

    public PyObject __radd__(PyObject left) {
        return __add__(left);
    }

    private final static PyObject _sub(PyComplex o1, PyComplex o2) {
        return new PyComplex(o1.real-o2.real, o1.imag-o2.imag);
    }

    public PyObject __sub__(PyObject right) {
        if (!canCoerce(right))
            return null;
        return _sub(this, coerce(right));
    }

    public PyObject __rsub__(PyObject left) {
        if (!canCoerce(left))
            return null;
        return _sub(coerce(left), this);
    }

    private final static PyObject _mul(PyComplex o1, PyComplex o2) {
        return new PyComplex(o1.real*o2.real-o1.imag*o2.imag,
                             o1.real*o2.imag+o1.imag*o2.real);
    }

    public PyObject __mul__(PyObject right) {
        if (!canCoerce(right))
            return null;
        return _mul(this, coerce(right));
    }

    public PyObject __rmul__(PyObject left) {
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
        if (!canCoerce(right))
            return null;
        return _div(this, coerce(right));
    }

    public PyObject __rdiv__(PyObject left) {
        if (!canCoerce(left))
            return null;
        return _div(coerce(left), this);
    }


    public PyObject __mod__(PyObject right) {
        if (!canCoerce(right))
            return null;
        return _mod(this, coerce(right));
    }

    public PyObject __rmod__(PyObject left) {
        if (!canCoerce(left))
            return null;
        return _mod(coerce(left), this);
    }

    private static PyObject _mod(PyComplex value, PyComplex right) {
        PyComplex z = (PyComplex)value.__div__(right);

        z.real = Math.floor(z.real);
        z.imag = 0.0;

        return value.__sub__(z.__mul__(right));
    }

    public PyObject __divmod__(PyObject right) {
        if (!canCoerce(right))
            return null;
        return _divmod(this, coerce(right));
    }

    public PyObject __rdivmod__(PyObject left) {
        if (!canCoerce(left))
            return null;
        return _divmod(coerce(left), this);
    }

    private static PyObject _divmod(PyComplex value, PyComplex right) {
        PyComplex z = (PyComplex)value.__div__(right);

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
        if (modulo != null) {
            throw Py.ValueError("complex modulo");
        }
        if (!canCoerce(right))
            return null;
        return _pow(this, coerce(right));
    }

    public PyObject __rpow__(PyObject left) {
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
        return new PyComplex(-real, -imag);
    }

    public PyObject __pos__() {
        return this;
    }

    public PyObject __abs__() {
        return new PyFloat(ExtraMath.hypot(real, imag));
    }

    public PyInteger __int__() {
        throw Py.TypeError(
            "can't convert complex to int; use e.g. int(abs(z))");
    }

    public PyLong __long__() {
        throw Py.TypeError(
            "can't convert complex to long; use e.g. long(abs(z))");
    }

    public PyFloat __float__() {
        throw Py.TypeError("can't convert complex to float; use e.g. abs(z)");
    }
    public PyComplex __complex__() {
        return this;
    }

    public PyComplex conjugate() {
        return new PyComplex(real, -imag);
    }

    public boolean isMappingType() { return false; }
    public boolean isSequenceType() { return false; }

    // __class__ boilerplate -- see PyObject for details
    public static PyClass __class__;

    protected PyClass getPyClass() {
        return __class__;
    }
}
