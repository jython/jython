package org.python.core;
import org.python.parser.SimpleNode;
import java.util.Hashtable;

/*class PyOrdFunction extends PyObject {
    public PyObject __call__(PyObject arg) {
        if (arg instanceof PyString) {
            return new PyInteger(__builtin__.ord(arg.toString()));
        }
        throw Py.TypeError("arg must be a string");
    }
}

class PyChrFunction extends PyObject {
    public PyObject __call__(PyObject arg) {
        return __builtin__.chr(Py.py2int(arg));
    }
}*/

public class __builtin__ implements InitModule {
    public void initModule(PyObject dict) {
        dict.__setitem__("None", Py.None);
        dict.__setitem__("Ellipsis", Py.Ellipsis);
        
		dict.__setitem__("StandardError", Py.StandardError);
		dict.__setitem__("KeyboardInterrupt", Py.KeyboardInterrupt);
		dict.__setitem__("ImportError", Py.ImportError);
		dict.__setitem__("SystemError", Py.SystemError);
		dict.__setitem__("AttributeError", Py.AttributeError);
		dict.__setitem__("ArithmeticError", Py.ArithmeticError);
		dict.__setitem__("RuntimeError", Py.RuntimeError);
		dict.__setitem__("EOFError", Py.EOFError);
		dict.__setitem__("AssertionError", Py.AssertionError);
		dict.__setitem__("FloatingPointError", Py.FloatingPointError);
		dict.__setitem__("IndexError", Py.IndexError);
		dict.__setitem__("NameError", Py.NameError);
		dict.__setitem__("KeyError", Py.KeyError);
		dict.__setitem__("TypeError", Py.TypeError);
		dict.__setitem__("LookupError", Py.LookupError);
		dict.__setitem__("Exception", Py.Exception);
		dict.__setitem__("IOError", Py.IOError);
		dict.__setitem__("SyntaxError", Py.SyntaxError);
		dict.__setitem__("ValueError", Py.ValueError);
		dict.__setitem__("SystemExit", Py.SystemExit);
		dict.__setitem__("ZeroDivisionError", Py.ZeroDivisionError);
		dict.__setitem__("MemoryError", Py.MemoryError);
		dict.__setitem__("OverflowError", Py.OverflowError);
		
		// Work in debug mode by default
		// Hopefully add -O option in the future to change this
		dict.__setitem__("__debug__", Py.One);
		//dict.__setitem__("ord", new PyOrdFunction() );
		//dict.__setitem__("chr", new PyChrFunction() );

    }

	public static PyObject abs(PyObject o) {
		return o.__abs__();
	}

	public static PyObject apply(PyObject o, PyTuple args) {
		return o.__call__(args.list);
	}

	public static PyObject apply(PyObject o, PyTuple args, PyDictionary kws) {
		PyObject[] a;
		String[] kw;
		Hashtable table = kws.table;
		if (table.size() > 0) {
			java.util.Enumeration ek = table.keys();
    		java.util.Enumeration ev = table.elements();
    		int n = table.size();
    		kw = new String[n];
    		a = new PyObject[n+args.list.length];
    		System.arraycopy(args.list, 0, a, 0, args.list.length);
    		int offset = args.list.length;

    		for(int i=0; i<n; i++) {
    		    kw[i] = ((PyString)ek.nextElement()).internedString();
    		    a[i+offset] = (PyObject)ev.nextElement();
    		}

    		return o.__call__(a, kw);
		} else {
			return apply(o, args);
		}
	}

    public static boolean callable(PyObject o) {
        return o.__findattr__("__call__") != null;
    }

	private static PyString[] letters=null;

	public static PyString chr(int i) {
		if (i < 0 || i > 65535) throw Py.ValueError("chr() arg not in range(65535)");
		if (i > 255) {
		    return new PyString(new String(new char[] {(char)i}));
		}
		// Cache the 8-bit characters for performance
		if (letters == null) {
			letters = new PyString[256];
			for(int j=0; j<256; j++) {
				letters[j] = new PyString(new String(new char[] {(char)j}));
			}
		}
		return letters[i];
	}

	public static int cmp(PyObject x, PyObject y) {
		return x._cmp(y);
	}

	public static PyTuple coerce(PyObject o1, PyObject o2) {
		Object ctmp;
		PyTuple ret;
		if (o1.__class__ == o2.__class__) {
		    return new PyTuple(new PyObject[] {o1, o2});
		}
		ctmp=o1.__coerce_ex__(o2);
		if (ctmp != null && ctmp != Py.None) {
		    if (ctmp instanceof PyObject[]) {
		        return new PyTuple((PyObject[])ctmp);
		    } else {
		        return new PyTuple(new PyObject[] {o1, (PyObject)ctmp});
		    }
		}

    	ctmp=o2.__coerce_ex__(o1);
		if (ctmp != null && ctmp != Py.None) {
		    if (ctmp instanceof PyObject[]) {
		        return new PyTuple((PyObject[])ctmp);
		    } else {
		        return new PyTuple(new PyObject[] {(PyObject)ctmp, o2});
		    }
		}
		return new PyTuple(new PyObject[] {o1, o2});
	}

	public static PyCode compile(String data, String filename, String type) {
		return Py.compile(new java.io.StringBufferInputStream(data+"\n\n"), filename, type);
	}

	public static PyComplex complex(PyObject real, PyObject imag) {
	    return (PyComplex)real.__complex__().__add__(imag.__complex__().__mul__(PyComplex.J));
	}

	public static PyComplex complex(PyObject real) {
	    return real.__complex__();
	}


	public static void delattr(PyObject o, PyString n) {
		o.__delattr__(n);
	}

    private static void addKeys(PyList ret, PyObject o, String attr) {
	    PyObject obj = o.__findattr__(attr);
	    if (obj == null) return;
	    if (obj instanceof PyDictionary) {
	        ret.setslice(ret.__len__(), ret.__len__(), 1, ((PyDictionary)obj).keys());
	    } else if (obj instanceof PyStringMap) {
	        ret.setslice(ret.__len__(), ret.__len__(), 1, ((PyStringMap)obj).keys());
	    } else if (obj instanceof PyList) {
	        ret.setslice(ret.__len__(), ret.__len__(), 1, (PyList)obj);
	    }
    }
    
	public static PyObject dir(PyObject o) {
	    PyList ret = new PyList();
	    
	    addKeys(ret, o, "__dict__");
	    addKeys(ret, o, "__methods__");
	    addKeys(ret, o, "__members__");
	    
		ret.sort();
		return ret;
	}

	public static PyObject dir() {
	    PyObject l = locals();
	    PyList ret;
	    
	    if (l instanceof PyStringMap) ret = ((PyStringMap)l).keys();
	    if (l instanceof PyDictionary) ret = ((PyDictionary)l).keys();
	    
	    ret = (PyList)l.invoke("keys");
	    ret.sort();
	    return ret;
	}

	public static PyObject divmod(PyObject x, PyObject y) {
		return x._divmod(y);
	}

	public static PyObject eval(PyObject o, PyObject globals, PyObject locals) {
		PyCode code;
		if (o instanceof PyCode) code = (PyCode)o;
		else {
			if (o instanceof PyString)
				code = __builtin__.compile(((PyString)o).toString(), "<string>", "eval");
			else
				throw Py.TypeError("eval: argument 1 must be string or code object");
		}
		return Py.runCode(code, locals, globals);
	}

	public static PyObject eval(PyObject o, PyObject globals) {
		return eval(o, globals, globals);
	}

	public static PyObject eval(PyObject o) {
		return eval(o, null, null);
	}

	public static void execfile(String name, PyObject globals, PyObject locals) {
		java.io.FileInputStream file;
		try {
			file = new java.io.FileInputStream(name);
		} catch (java.io.FileNotFoundException e) {
			throw Py.IOError(e);
		}

		PyCode code;
		
		try {
		    code = Py.compile(file, name, "exec");
		} finally {
    		try {
    			file.close();
    		} catch (java.io.IOException e) {
    			throw Py.IOError(e);
    		}
    	}

		Py.runCode(code, locals, globals);
	}

	public static void execfile(String name, PyObject globals) {
		execfile(name, globals, globals);
	}
	public static void execfile(String name) {
		execfile(name, null, null);
	}

	public static PyObject filter(PyObject f, PyString s) {
	    if (f == Py.None) return s;
	    PyObject[] args = new PyObject[1];
	    char[] chars = s.toString().toCharArray();
		int i;
		int j;
		int n = chars.length;
		for(i=0, j=0; i<n; i++) {
		    args[0] = chr(chars[i]);
		    if (!f.__call__(args).__nonzero__()) continue;
		    chars[j++] = chars[i];
		}
		return new PyString(new String(chars, 0, j));
	}


	public static PyObject filter(PyObject f, PyObject l) {
		int i=0;
		PyObject element;
		PyList list = new PyList();
		while ((element = l.__finditem__(i++)) != null) {
			if (f == Py.None) {
				if (!element.__nonzero__()) continue;
			} else {
				if (!f.__call__(element).__nonzero__()) continue;
			}
			list.append(element);
		}
		return list;
	}

	public static PyFloat float$(PyObject o) {
		return o.__float__();
	}

	public static PyObject getattr(PyObject o, PyString n) {
		return o.__getattr__(n);
	}

	public static PyObject globals() {
		return Py.getFrame().f_globals;
	}

	public static boolean hasattr(PyObject o, PyString n) {
		return o.__findattr__(n) != null;
	}

	public static PyInteger hash(PyObject o) {
		return o.__hash__();
	}

	public static PyString hex(PyObject o) {
		return o.__hex__();
	}

	public static int id(PyObject o) {
		return Py.id(o);
	}

	/*
	public static PyObject input(PyString prompt) throws PyException, java.io.IOException {
		String line = raw_input(prompt);

		org.python.parser.Node node = parser.parse(line, "single", true);
		if (node == null) {
			while (true) {
				String input = raw_input(sys.ps2);
				if (node != null && input.equals("")) break;
				line = line+"\n"+input;
				node = parser.parse(line, "single", true);
			}
		}
		//System.out.println("line: "+line);
		PyCode code = compile(node, "<stdin>", null);
		return code;
		//return Py.runCode(code, locals, globals);
	}

	public static PyObject input() throws PyException, java.io.IOException {
		return input(sys.ps1);
	}
	*/

	public static PyInteger int$(PyObject o) {
		return o.__int__();
	}

	//Need to implement string interning

	public static boolean isinstance(PyObject obj, PyClass myClass) {
	    return issubclass(obj.__class__, myClass);
	}


	public static boolean issubclass(PyClass subClass, PyClass superClass) {
	    if (subClass == superClass) return true;
	    PyObject[] bases = subClass.__bases__.list;
	    int n = bases.length;
	    for(int i=0; i<n; i++) {
	        PyClass c = (PyClass)bases[i];
	        if (issubclass(c, superClass)) return true;
	    }
	    return false;
	}


	public static int len(PyObject o) {
		return o.__len__();
	}

	public static PyList list(PyObject o) {
		if (o instanceof PyList) return (PyList)o;
		return new PyList(make_array(o));
	}

	public static PyObject locals() {
		return Py.getFrame().getf_locals();
	}

	public static PyLong long$(PyObject o) {
		return o.__long__();
	}

	public static PyObject map(PyObject[] argstar) {
		int i=0;
		int n = argstar.length-1;
		if (n < 1) throw Py.TypeError("map requires at least two arguments");
		PyObject element;
		PyObject f = argstar[0];
		PyList list = new PyList();
		PyObject[] args = new PyObject[n];
		while (true) {
			boolean any_items = false;
			for(int j=0; j<n; j++) {
				if ((element = argstar[j+1].__finditem__(i)) != null) {
					args[j] = element;
					any_items = true;
				} else {
					args[j] = Py.None;
				}
			}
			if (!any_items) break;
			if (f == Py.None) {
				if (n == 1) {
					list.append(args[0]);
				} else {
					list.append(new PyTuple((PyObject[])args.clone()));
				}
			} else {
				list.append(f.__call__(args));
			}
			i = i+1;
		}
		return list;
	}

	// I've never been happy with max and min builtin's...

	public static PyObject max(PyObject[] l) {
		if (l.length == 1) return max(l[0]);
		else return max(new PyTuple(l));
	}

	private static PyObject max(PyObject o) {
		PyObject max = o.__finditem__(0);
		if (max == null) throw Py.TypeError("max of empty sequence");
		PyObject element;
		int i=1;
		while ((element = o.__finditem__(i++)) != null) {
			if (element._gt(max).__nonzero__()) max = element;
		}
		return max;
	}

	public static PyObject min(PyObject[] l) {
		if (l.length == 1) return min(l[0]);
		else return min(new PyTuple(l));
	}

	private static PyObject min(PyObject o) {
		PyObject min = o.__finditem__(0);
		if (min == null) throw Py.TypeError("min of empty sequence");
		PyObject element;
		int i=1;
		while ((element = o.__finditem__(i++)) != null) {
			if (element._lt(min).__nonzero__()) min = element;
		}
		return min;
	}

	public static PyString oct(PyObject o) {
		return o.__oct__();
	}

    public static PyFile open(String name) throws java.io.IOException {
        return new PyFile(name, "r", -1);
    }

    public static PyFile open(String name, String mode) throws java.io.IOException {
        return new PyFile(name, mode, -1);
    }

    public static PyFile open(String name, String mode, int bufsize) throws java.io.IOException {
        return new PyFile(name, mode, bufsize);
    }

	public static int ord(String s) {
		if (s.length() != 1) throw Py.TypeError("expected 1-length string");
		return (int)(s.charAt(0));
	}

	public static PyObject pow(PyObject x, PyObject y) {
		return x._pow(y);
	}

    private static boolean coerce(PyObject[] objs) {
        PyObject x = objs[0];
        PyObject y = objs[1];
        if (x.__class__ == y.__class__) return true;
        Object ctmp = x.__coerce_ex__(y);
        if (ctmp != null && ctmp != Py.None) {
    		if (ctmp instanceof PyObject[]) {
    		    x = ((PyObject[])ctmp)[0]; y = ((PyObject[])ctmp)[1];
    		} else {
    		    y = (PyObject)ctmp;
    		}
    	}
		objs[0] = x; objs[1] = y;
    	if (x.__class__ == y.__class__) return true;
        ctmp = y.__coerce_ex__(x);
        if (ctmp != null && ctmp != Py.None) {
    		if (ctmp instanceof PyObject[]) {
    		    y = ((PyObject[])ctmp)[0]; x = ((PyObject[])ctmp)[1];
    		} else {
    		    x = (PyObject)ctmp;
    		}
    	}
		objs[0] = x; objs[1] = y;
		//System.out.println(""+x.__class__+" : "+y.__class__);
		return x.__class__ == y.__class__;
	}

	public static PyObject pow(PyObject xi, PyObject yi, PyObject zi) {
		PyObject x=xi;
		PyObject y=yi;
		PyObject z=zi;

		boolean doit=false;

        PyObject[] tmp = new PyObject[2];

        tmp[0] = x;
        tmp[1] = y;
        if (coerce(tmp)) {
            x = tmp[0];
            y = tmp[1];
            tmp[1] = z;
            if (coerce(tmp)) {
                x = tmp[0];
                z = tmp[1];
                tmp[0] = y;
                if (coerce(tmp)) {
                    z = tmp[1]; y = tmp[0];
                    doit=true;
                }
            }
        } else {
            tmp[1] = z;
            if (coerce(tmp)) {
                x=tmp[0]; z=tmp[1];
                tmp[0] = y;
                if (coerce(tmp)) {
                    y=tmp[0]; z = tmp[1];
                    tmp[1] = x;
                    if (coerce(tmp)) {
                        x=tmp[1]; y=tmp[0];
                        doit = true;
                    }
                }
            }
        }

        if (x.__class__ == y.__class__ && x.__class__ == z.__class__) {
            x = x.__pow__(y, z);
            if (x != null) return x;
        }

		throw Py.TypeError("__pow__ not defined for these operands");
	}

	public static PyObject range(int start, int stop, int step) {
	    if (step == 0)
	        throw Py.ValueError("zero step for range()");
		int n;
		if (step > 0) n = (stop-start+step-1)/step;
		else n = (stop-start+step+1)/step;
		
		if (n <= 0) return new PyList();
		PyObject[] l = new PyObject[n];
		int j=start;
		for (int i=0; i<n; i++) {
			l[i] = new PyInteger(j);
			j+= step;
		}
		return new PyList(l);
	}

	public static PyObject range(int n) {
		return range(0,n,1);
	}

	public static PyObject range(int start, int stop) {
		return range(start,stop,1);
	}

	public static String raw_input(PyObject prompt) throws java.io.IOException {
		Py.print(prompt);
		StringBuffer buf = new StringBuffer();

		while (true) {
			int i = System.in.read();
			if (i == -1) throw Py.EOFError("raw_input()");
			char c = (char)i;
			if (c == '\r') continue;
			if (c == '\n') break;
			buf.append(c);
		}
		return buf.toString();
	}

	public static String raw_input() throws java.io.IOException {
		return raw_input(new PyString(""));
	}

	public static PyObject reduce(PyObject f, PyObject l, PyObject z) {
		int i=0;
		PyObject element, result;
		result = z;
		if (result == null) {
		    result = l.__finditem__(i++);
		    if (result == null) {
		        throw Py.TypeError("reduce of empty sequence with no initial value");
		    }
		}

		while ((element = l.__finditem__(i++)) != null) {
			result = f.__call__(result, element);
		}
		return result;
	}

	public static PyObject reduce(PyObject f, PyObject l) {
		return reduce(f, l, null);
	}

	public static PyObject reload(PyModule o) {
        return imp.reload(o);
	}
	public static PyObject reload(PyJavaClass o) throws PyException {
        return imp.reload(o);
	}

	public static PyString repr(PyObject o) throws PyException {
		return o.__repr__();
	}

	//This seems awfully special purpose...
	public static PyFloat round(double f, int digits) throws PyException {
	    boolean neg = f < 0;
		double multiple = Math.pow(10., digits);
		if (neg) f = -f;
		double tmp = Math.floor(f*multiple+0.5);
		if (neg) tmp = -tmp;
		return new PyFloat(tmp/multiple);
	}

	public static PyFloat round(double f) throws PyException {
		return round(f, 0);
	}

	public static void setattr(PyObject o, PyString n, PyObject v) {
		o.__setattr__(n, v);
	}

	public static PySlice slice(PyObject start, PyObject stop, PyObject step) {
		return new PySlice(start, stop, step);
	}

	public static PySlice slice(PyObject start, PyObject stop) {
		return slice(start, stop, Py.None);
	}

	public static PySlice slice(PyObject stop) {
		return slice(Py.None, stop, Py.None);
	}


	public static PyString str(PyObject o) {
		return o.__str__();
	}

	public static PyTuple tuple(PyObject o) {
		if (o instanceof PyTuple) return (PyTuple)o;
		return new PyTuple(make_array(o));
	}

	public static PyClass type(PyObject o) {
	    if (o instanceof PyInstance) {
	        return PyJavaClass.lookup(PyInstance.class);
	    } else {
		    return o.__class__;
		}
	}

	public static PyObject vars(PyObject o) {
		return o.__getattr__("__dict__");
	}


	public static PyObject vars() {
		return locals();
	}

	public static PyObject xrange(int start, int stop, int step) {
	    return new PyXRange(start, stop, step);
	}

	public static PyObject xrange(int n) {
		return xrange(0,n,1);
	}

	public static PyObject xrange(int start, int stop) {
		return xrange(start,stop,1);
	}

    public static synchronized PyObject __import__(PyString name) {
        return imp.importName(name.internedString(), true);
    }

	private static PyObject[] make_array(PyObject o) {
		if (o instanceof PyTuple) return ((PyTuple)o).list;

		int n = o.__len__();
		PyObject[] objs= new PyObject[n];

		for(int i=0; i<n; i++) {
			objs[i] = o.__finditem__(i);
		}
		return objs;
	}
}
