package org.python.core;

public class PyComplex extends PyObject {
	public double real, imag;

    static PyComplex J = new PyComplex(0, 1.);

    public static PyClass __class__;
	public PyComplex(double r, double i) {
	    super(__class__);
	    real = r;
	    imag = i;
	}
	
	/*public double getReal() {
	    return real;
	}
	
	public double getImag() {
	    return image;
	}*/

    public static String toString(double value) {
        if (value == Math.floor(value)) {
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
            long v = Double.doubleToLongBits(real) ^ Double.doubleToLongBits(imag);
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
		if (real == oreal && imag == oimag) return 0;
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


	public PyObject __add__(PyObject o) {
	    PyComplex c = (PyComplex)o;
		return new PyComplex(real+c.real, imag+c.imag);
	}

	public PyObject __sub__(PyObject o) {
	    PyComplex c = (PyComplex)o;
		return new PyComplex(real-c.real, imag-c.imag);
	}

    public PyObject __mul__(PyObject o) {
	    PyComplex c = (PyComplex)o;
		return new PyComplex(real*c.real-imag*c.imag, real*c.imag+imag*c.real);
	}

	public PyObject __div__(PyObject o) {
	    PyComplex c = (PyComplex)o;
	    double denom = c.real*c.real+c.imag*c.imag;
	    if (denom == 0) throw Py.ZeroDivisionError("complex division");
		return new PyComplex((real*c.real + imag*c.imag)/denom,
		                     (imag*c.real - real*c.imag)/denom);
	}


	public PyObject __mod__(PyObject o) {
	    PyComplex z = (PyComplex)__div__(o);

	    z.real = Math.floor(z.real);
	    z.imag = 0.0;

	    return __sub__(z.__mul__(o));
	}

	public PyObject __divmod__(PyObject o) {
	    PyComplex z = (PyComplex)__div__(o);

	    z.real = Math.floor(z.real);
	    z.imag = 0.0;

	    return new PyTuple(new PyObject[] {z, __sub__(z.__mul__(o))});
	}


    private PyObject ipow(int iexp) {
        int pow = iexp;
        if (pow < 0) pow = -pow;

        double xr = real;
        double xi = imag;

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
	        if (pow == 0) break;
	        tmp = xr*xr - xi*xi;
	        xi = xr*xi*2;
	        xr = tmp;
	    }

	    PyComplex ret = new PyComplex(zr, zi);

	    if (iexp < 0) return new PyComplex(1,0).__div__(ret);
	    return ret;
    }

	public PyObject __pow__(PyObject right, PyObject modulo) {
	    if (modulo != null) {
	        throw Py.ValueError("complex modulo");
	    }

        double xr = real;
        double xi = imag;
        double yr = ((PyComplex)right).real;
        double yi = ((PyComplex)right).imag;

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
            return ipow(iexp);
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
	    throw Py.TypeError("can't convert complex to int; use e.g. int(abs(z))");
	}

	public PyLong __long__() {
	    throw Py.TypeError("can't convert complex to long; use e.g. long(abs(z))");
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
}
