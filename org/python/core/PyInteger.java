package org.python.core;

public class PyInteger extends PyObject {
    private int value; 
    public PyInteger(int v) {
	value = (int)v;
    }

    public int getValue() {
        return value;
    }

    public String toString() {
	return Integer.toString(value);
    }

    public int hashCode() {
	return value;
    }

    public boolean __nonzero__() {
	return value != 0;
    }

    public Object __tojava__(Class c) {
        if (c == Integer.TYPE || c == Number.class || 
	    c == Object.class || c == Integer.class) {
            return new Integer(value);
        }
        
	if (c == Boolean.TYPE || c == Boolean.class) return new Boolean(value != 0);
	if (c == Byte.TYPE || c == Byte.class) return new Byte((byte)value);
	if (c == Short.TYPE || c == Short.class) return new Short((short)value);
		
	if (c == Long.TYPE || c == Long.class) return new Long(value);
	if (c == Float.TYPE || c == Float.class) return new Float(value);
	if (c == Double.TYPE || c == Double.class) return new Double(value);
	return super.__tojava__(c);
    }

    public int __cmp__(PyObject other) {
	int v = ((PyInteger)other).value;
	return value < v ? -1 : value > v ? 1 : 0;
    }

    public Object __coerce_ex__(PyObject other) {
	if (other instanceof PyInteger) return other;
	else return Py.None;
    }

    public PyObject __add__(PyObject right) {
	int a = value;
	int b = ((PyInteger)right).value;
	int x = a + b;
	if ((x^a) < 0 && (x^b) < 0)
	    throw Py.OverflowError("integer addition: "+this+" + "+right);
	return Py.newInteger(x); //new PyInteger(x);
    }

    public PyObject __sub__(PyObject right) {
	int a = value;
	int b = ((PyInteger)right).value;
	int x = a - b;
	if ((x^a) < 0 && (x^~b) < 0)
	    throw Py.OverflowError("integer subtraction: "+this+" - "+right);
	return Py.newInteger(x);
    }

    public PyObject __mul__(PyObject right) {
	double x = (double)value;
	x *= ((PyInteger)right).value;
	//long x = ((long)value)*((PyInteger)right).value;
	//System.out.println("mul: "+this+" * "+right+" = "+x);
		
	if (x > Integer.MAX_VALUE || x < Integer.MIN_VALUE)
	    throw Py.OverflowError("integer multiplication: "+this+" * "+right);
	return Py.newInteger((int)x);
    }

    // Getting signs correct for integer division
    // This convention makes sense when you consider it in tandem with modulo
    private int divide(int x, int y) {
	if (y == 0) throw Py.ZeroDivisionError("integer division or modulo");

        if (y < 0) {
	    if (x > 0)
		return (x-y-1) / y;
	} else {
	    if (x < 0)
		return (x-y+1) / y;
	}
	return x / y;
    }

    public PyObject __div__(PyObject right) {
	return Py.newInteger(divide(value, ((PyInteger)right).value));
    }

    private int modulo(int x, int y, int xdivy) {
	return x - xdivy*y;
    }

    public PyObject __mod__(PyObject right) {
	int x = ((PyInteger)right).value;
	return Py.newInteger(modulo(value, x, divide(value, x)));
    }

    public PyObject __divmod__(PyObject right) {
	int x = ((PyInteger)right).value;
	int xdivy = divide(value, x);
	return new PyTuple(new PyObject[] {new PyInteger(xdivy),
					       new PyInteger(modulo(value, x, xdivy)) } );
    }

    public PyObject __pow__(PyObject right, PyObject modulo) {
	int mod = 0;
	int pow = ((PyInteger)right).value;
	long tmp = value;
	boolean neg = false;
	if (tmp < 0) {
	    tmp = -tmp;
	    neg = (pow & 0x1) != 0;
	}
	long result = 1;

	if (pow < 0) {
	    throw Py.ValueError("integer to the negative power");
	}

	if (modulo != null) {
	    mod = ((PyInteger)modulo).value;
	    if (mod == 0) {
		throw Py.ValueError("pow(x, y, z) with z==0");
	    }
	}


	// Standard O(ln(N)) exponentiation code
	while (pow > 0) {
	    if ((pow & 0x1) != 0) {
		result *= tmp;
		if (mod != 0) {
		    result %= (long)mod;
		}

		if (result > Integer.MAX_VALUE) {
		    throw Py.OverflowError("integer pow()");
		}
	    }
	    pow >>= 1;
	    if (pow == 0) break;
	    tmp *= tmp;

	    if (mod != 0) {
		tmp %= (long)mod;

	    }

	    if (tmp > Integer.MAX_VALUE) {
                throw Py.OverflowError("integer pow()");
            }
	}

	int ret = (int)result;
	if (neg) ret = -ret;

	// Cleanup result of modulo
	if (mod != 0) {
	    ret = modulo(ret, mod, divide(ret, mod));
	}
	return Py.newInteger(ret);
    }

    public PyObject __lshift__(PyObject right) {
        int shift = ((PyInteger)right).value;
        if (shift > 31) return new PyInteger(0);
	return Py.newInteger(value << shift);
    }

    public PyObject __rshift__(PyObject right) {
	return Py.newInteger(value >>> ((PyInteger)right).value);
    }

    public PyObject __and__(PyObject right) {
	return Py.newInteger(value & ((PyInteger)right).value);
    }

    public PyObject __xor__(PyObject right) {
	return Py.newInteger(value ^ ((PyInteger)right).value);
    }

    public PyObject __or__(PyObject right) {
	return Py.newInteger(value | ((PyInteger)right).value);
    }

    public PyObject __neg__() {
	int x = -value;
	if (value < 0 && x < 0)
	    throw Py.OverflowError("integer negation");
	return Py.newInteger(x);
    }

    public PyObject __pos__() {
	return this;
    }

    public PyObject __abs__() {
	if (value >= 0)
	    return this;
	else
	    return __neg__();
    }

    public PyObject __invert__() {
	return Py.newInteger(~value);
    }

    public PyInteger __int__() {
	return this;
    }

    public PyLong __long__() {
	return new PyLong(value);
    }

    public PyFloat __float__() {
	return new PyFloat((double)value);
    }

    public PyComplex __complex__() {
	return new PyComplex((double)value, 0.);
    }

    public PyString __oct__() {
	if (value < 0) {
	    return new PyString("0"+Long.toString(0x100000000l+(long)value, 8));
	} else {
	    return new PyString("0"+Integer.toString(value, 8));
	}
    }

    public PyString __hex__() {
	if (value < 0) {
	    return new PyString("0x"+Long.toString(0x100000000l+(long)value, 16));
	} else {
	    return new PyString("0x"+Integer.toString(value, 16));
        }
    }

    // __class__ boilerplate -- see PyObject for details
    public static PyClass __class__;
    protected PyClass getPyClass() { return __class__; }
}
