// Copyright (c) Corporation for National Research Initiatives
package org.python.core;

import java.util.Hashtable;

class BuiltinFunctions extends PyBuiltinFunctionSet {

	public BuiltinFunctions(String name, int index, int argcount) {
		this(name, index, argcount, argcount);
	}

	public BuiltinFunctions(String name, int index, int minargs, int maxargs) {
		super(name, index, minargs, maxargs);
	}

	public PyObject __call__() {
		switch (this.index) {
		case 4:
			return __builtin__.globals();
		case 16:
			return __builtin__.dir();
		case 24:
			return __builtin__.input();
		case 28:
			return __builtin__.locals();
		case 34:
			return Py.newString(__builtin__.raw_input());
		case 41:
			return __builtin__.vars();
		default:
			throw info.unexpectedCall(0, false);
		}
	}

	public PyObject __call__(PyObject arg1) {
		switch (this.index) {
		case 0:
			return Py.newString(__builtin__.chr(Py.py2int(arg1, "chr(): 1st arg can't be coerced to int")));
		case 1:
			return Py.newInteger(__builtin__.len(arg1));
		case 2:
			return __builtin__.range(Py.py2int(arg1, "range(): 1st arg can't be coerced to int"));
		case 3:
			return Py.newInteger(__builtin__.ord(Py.py2char(arg1, "ord(): 1st arg can't be coerced to char")));
		case 5:
			return __builtin__.hash(arg1);
		case 6:
			return Py.newUnicode(__builtin__.chr(Py.py2int(arg1, "unichr(): 1st arg can't be coerced to int")));
		case 7:
			return __builtin__.abs(arg1);
		case 8:
			return __builtin__.bool(arg1);
		case 11:
			return Py.newInteger(__builtin__.id(arg1));
		case 12:
			return __builtin__.sum(arg1);
		case 14:
			return Py.newBoolean(__builtin__.callable(arg1));
		case 16:
			return __builtin__.dir(arg1);
		case 18:
			return __builtin__.eval(arg1);
		case 19:
			try {
				__builtin__.execfile(arg1.asString(0));
			} catch (ConversionException e) {
				throw Py.TypeError("execfile's first argument must be str");
			}
			return null;
		case 23:
			return __builtin__.hex(arg1);
		case 24:
			return __builtin__.input(arg1);
		case 25:
			return __builtin__.intern(arg1.__str__());
		case 27:
			return __builtin__.iter(arg1);
		case 32:
			return __builtin__.oct(arg1);
		case 34:
			return Py.newString(__builtin__.raw_input(arg1));
		case 36:
			Object o = arg1.__tojava__(PyModule.class);
			if (o == Py.NoConversion) {
				o = arg1.__tojava__(PyJavaClass.class);
				if (o == Py.NoConversion) {
					Py.TypeError("reload() argument must be a module");
				}
				return __builtin__.reload((PyJavaClass) o);
			}
			return __builtin__.reload((PyModule) o);
		case 37:
			return __builtin__.repr(arg1);
		case 38:
			return __builtin__.round(Py.py2double(arg1));
		case 40:
			return __builtin__.slice(arg1);
		case 41:
			return __builtin__.vars(arg1);
		case 42:
			return __builtin__.xrange(Py.py2int(arg1));
		case 30:
			return fancyCall(new PyObject[] { arg1 });
		case 31:
			return fancyCall(new PyObject[] { arg1 });
		case 43:
			return fancyCall(new PyObject[] { arg1 });
		default:
			throw info.unexpectedCall(1, false);
		}
	}

	public PyObject __call__(PyObject arg1, PyObject arg2) {
		switch (this.index) {
		case 2:
			return __builtin__.range(Py.py2int(arg1, "range(): 1st arg can't be coerced to int"), Py.py2int(arg2,
					"range(): 2nd arg can't be coerced to int"));
		case 6:
			return Py.newInteger(__builtin__.cmp(arg1, arg2));
		case 9:
			return __builtin__.apply(arg1, arg2);
		case 10:
			return Py.newBoolean(__builtin__.isinstance(arg1, arg2));
		case 12:
			return __builtin__.sum(arg1, arg2);
		case 13:
			return __builtin__.coerce(arg1, arg2);
		case 15:
			__builtin__.delattr(arg1, asString(arg2, "delattr(): attribute name must be string"));
			return null;
		case 17:
			return __builtin__.divmod(arg1, arg2);
		case 18:
			return __builtin__.eval(arg1, arg2);
		case 19:
			try {
				__builtin__.execfile(arg1.asString(0), arg2);
			} catch (ConversionException e) {
				throw Py.TypeError("execfile's first argument must be str");
			}
			return null;
		case 20:
			return __builtin__.filter(arg1, arg2);
		case 21:
			return __builtin__.getattr(arg1, asString(arg2, "getattr(): attribute name must be string"));
		case 22:
			return Py.newBoolean(__builtin__.hasattr(arg1, asString(arg2, "hasattr(): attribute name must be string")));
		case 26:
			return Py.newBoolean(__builtin__.issubclass(arg1, arg2));
		case 27:
			return __builtin__.iter(arg1, arg2);
		case 33:
			return __builtin__.pow(arg1, arg2);
		case 35:
			return __builtin__.reduce(arg1, arg2);
		case 38:
			return __builtin__.round(Py.py2double(arg1), Py.py2int(arg2));
		case 40:
			return __builtin__.slice(arg1, arg2);
		case 42:
			return __builtin__.xrange(Py.py2int(arg1), Py.py2int(arg2));
		case 29:
			return fancyCall(new PyObject[] { arg1, arg2 });
		case 30:
			return fancyCall(new PyObject[] { arg1, arg2 });
		case 31:
			return fancyCall(new PyObject[] { arg1, arg2 });
		case 43:
			return fancyCall(new PyObject[] { arg1, arg2 });
		default:
			throw info.unexpectedCall(2, false);
		}
	}

	public PyObject __call__(PyObject arg1, PyObject arg2, PyObject arg3) {
		switch (this.index) {
		case 2:
			return __builtin__.range(Py.py2int(arg1, "range(): 1st arg can't be coerced to int"), Py.py2int(arg2,
					"range(): 2nd arg can't be coerced to int"), Py.py2int(arg3,
					"range(): 3rd arg can't be coerced to int"));
		case 9:
			try {
				if (arg3 instanceof PyStringMap) {
					PyDictionary d = new PyDictionary();
					d.update(arg3);
					arg3 = d;
				}
				// this catches both casts of arg3 to a PyDictionary, and
				// all casts of keys in the dictionary to PyStrings inside
				// apply(PyObject, PyObject, PyDictionary)
				PyDictionary d = (PyDictionary) arg3;
				return __builtin__.apply(arg1, arg2, d);
			} catch (ClassCastException e) {
				throw Py.TypeError("apply() 3rd argument must be a " + "dictionary with string keys");
			}
		case 18:
			return __builtin__.eval(arg1, arg2, arg3);
		case 19:
			__builtin__.execfile(asString(arg1, "execfile's first argument must be str", false), arg2, arg3);
			return null;
		case 21:
			return __builtin__.getattr(arg1, asString(arg2, "getattr(): attribute name must be string"), arg3);
		case 33:
			return __builtin__.pow(arg1, arg2, arg3);
		case 35:
			return __builtin__.reduce(arg1, arg2, arg3);
		case 39:
			__builtin__.setattr(arg1, asString(arg2, "setattr(): attribute name must be string"), arg3);
			return null;
		case 40:
			return __builtin__.slice(arg1, arg2, arg3);
		case 42:
			return __builtin__.xrange(Py.py2int(arg1), Py.py2int(arg2), Py.py2int(arg3));
		case 44:
			return fancyCall(new PyObject[] { arg1, arg2, arg3 });
		case 29:
			return fancyCall(new PyObject[] { arg1, arg2, arg3 });
		case 30:
			return fancyCall(new PyObject[] { arg1, arg2, arg3 });
		case 31:
			return fancyCall(new PyObject[] { arg1, arg2, arg3 });
		case 43:
			return fancyCall(new PyObject[] { arg1, arg2, arg3 });
		default:
			throw info.unexpectedCall(3, false);
		}
	}

	/**
	 * @return arg as an interned String, or throws TypeError with mesage if asString throws a ConversionException
	 */
	private String asString(PyObject arg, String message) {
		return asString(arg, message, true);
	}


	/**
	 * 
	 * @param intern - should the resulting string be interned
	 * @return arg as a String, or throws TypeError with message if asString throws a ConversionException.  
	 */
	private String asString(PyObject arg, String message, boolean intern) {

		try {
			return intern ? arg.asString(0).intern() : arg.asString(0);
		} catch (ConversionException e) {
			throw Py.TypeError(message);
		}
	}

	public PyObject __call__(PyObject arg1, PyObject arg2, PyObject arg3, PyObject arg4) {
		switch (this.index) {
		case 44:
			return fancyCall(new PyObject[] { arg1, arg2, arg3, arg4 });
		case 29:
			return fancyCall(new PyObject[] { arg1, arg2, arg3, arg4 });
		case 30:
			return fancyCall(new PyObject[] { arg1, arg2, arg3, arg4 });
		case 31:
			return fancyCall(new PyObject[] { arg1, arg2, arg3, arg4 });
		case 43:
			return fancyCall(new PyObject[] { arg1, arg2, arg3, arg4 });
		default:
			throw info.unexpectedCall(4, false);
		}
	}

	public PyObject fancyCall(PyObject[] args) {
		switch (this.index) {
		case 44:
			if (args.length > 5) {
				throw info.unexpectedCall(args.length, false);
			}
			int flags = 0;
			if (args.length > 3) {
				flags = Py.py2int(args[3]);
			}
			boolean dont_inherit = false;
			if (args.length > 4) {
				dont_inherit = Py.py2boolean(args[4]);
			}

			return __builtin__.compile(args[0].toString(), args[1].toString(), args[2].toString(), flags, dont_inherit);
		case 29:
			return __builtin__.map(args);
		case 30:
			return __builtin__.max(args);
		case 31:
			return __builtin__.min(args);
		case 43:
			return __builtin__.zip(args);
		default:
			throw info.unexpectedCall(args.length, false);
		}
	}

}

/**
 * The builtin module. All builtin functions are defined here
 */
public class __builtin__  {
	public static void fillWithBuiltins(PyObject dict) {
		/* newstyle */
		dict.__setitem__("object", PyType.fromClass(PyObject.class));
		dict.__setitem__("type", PyType.fromClass(PyType.class));
		dict.__setitem__("int", PyType.fromClass(PyInteger.class));
		dict.__setitem__("enumerate", PyType.fromClass(PyEnumerate.class));
		dict.__setitem__("float", PyType.fromClass(PyFloat.class));
		dict.__setitem__("long", PyType.fromClass(PyLong.class));
		dict.__setitem__("complex", PyType.fromClass(PyComplex.class));
		dict.__setitem__("dict", PyType.fromClass(PyDictionary.class));
		dict.__setitem__("list", PyType.fromClass(PyList.class));
		dict.__setitem__("tuple", PyType.fromClass(PyTuple.class));
	
		dict.__setitem__("property", PyType.fromClass(PyProperty.class));
		dict.__setitem__("staticmethod", PyType.fromClass(PyStaticMethod.class));
		dict.__setitem__("classmethod", PyType.fromClass(PyClassMethod.class));
		dict.__setitem__("super", PyType.fromClass(PySuper.class));
		dict.__setitem__("str", PyType.fromClass(PyString.class));
		dict.__setitem__("unicode", PyType.fromClass(PyUnicode.class));
		dict.__setitem__("basestring", PyType.fromClass(PyBaseString.class));
		dict.__setitem__("file", PyType.fromClass(PyFile.class));
		dict.__setitem__("open", PyType.fromClass(PyFile.class));
	
		/* - */
	
		dict.__setitem__("None", Py.None);
		dict.__setitem__("NotImplemented", Py.NotImplemented);
		dict.__setitem__("Ellipsis", Py.Ellipsis);
		dict.__setitem__("True", Py.One);
		dict.__setitem__("False", Py.Zero);
	
		// Work in debug mode by default
		// Hopefully add -O option in the future to change this
		dict.__setitem__("__debug__", Py.One);
	
		dict.__setitem__("abs", new BuiltinFunctions("abs", 7, 1));
		dict.__setitem__("apply", new BuiltinFunctions("apply", 9, 2, 3));
		dict.__setitem__("bool", new BuiltinFunctions("bool", 8, 1));
		dict.__setitem__("callable", new BuiltinFunctions("callable", 14, 1));
		dict.__setitem__("coerce", new BuiltinFunctions("coerce", 13, 2));
		dict.__setitem__("chr", new BuiltinFunctions("chr", 0, 1));
		dict.__setitem__("cmp", new BuiltinFunctions("cmp", 6, 2));
		dict.__setitem__("globals", new BuiltinFunctions("globals", 4, 0));
		dict.__setitem__("hash", new BuiltinFunctions("hash", 5, 1));
		dict.__setitem__("id", new BuiltinFunctions("id", 11, 1));
		dict.__setitem__("isinstance", new BuiltinFunctions("isinstance", 10, 2));
		dict.__setitem__("len", new BuiltinFunctions("len", 1, 1));
		dict.__setitem__("ord", new BuiltinFunctions("ord", 3, 1));
		dict.__setitem__("range", new BuiltinFunctions("range", 2, 1, 3));
		dict.__setitem__("sum", new BuiltinFunctions("sum", 12, 1, 2));
		dict.__setitem__("unichr", new BuiltinFunctions("unichr", 6, 1));
		dict.__setitem__("compile", new BuiltinFunctions("compile", 44, 3, -1));
		dict.__setitem__("delattr", new BuiltinFunctions("delattr", 15, 2));
		dict.__setitem__("dir", new BuiltinFunctions("dir", 16, 0, 1));
		dict.__setitem__("divmod", new BuiltinFunctions("divmod", 17, 2));
		dict.__setitem__("eval", new BuiltinFunctions("eval", 18, 1, 3));
		dict.__setitem__("execfile", new BuiltinFunctions("execfile", 19, 1, 3));
		dict.__setitem__("filter", new BuiltinFunctions("filter", 20, 2));
		dict.__setitem__("getattr", new BuiltinFunctions("getattr", 21, 2, 3));
		dict.__setitem__("hasattr", new BuiltinFunctions("hasattr", 22, 2));
		dict.__setitem__("hex", new BuiltinFunctions("hex", 23, 1));
		dict.__setitem__("input", new BuiltinFunctions("input", 24, 0, 1));
		dict.__setitem__("intern", new BuiltinFunctions("intern", 25, 1));
		dict.__setitem__("issubclass", new BuiltinFunctions("issubclass", 26, 2));
		dict.__setitem__("iter", new BuiltinFunctions("iter", 27, 1, 2));
		dict.__setitem__("locals", new BuiltinFunctions("locals", 28, 0));
		dict.__setitem__("map", new BuiltinFunctions("map", 29, 2, -1));
		dict.__setitem__("max", new BuiltinFunctions("max", 30, 1, -1));
		dict.__setitem__("min", new BuiltinFunctions("min", 31, 1, -1));
		dict.__setitem__("oct", new BuiltinFunctions("oct", 32, 1));
		dict.__setitem__("pow", new BuiltinFunctions("pow", 33, 2, 3));
		dict.__setitem__("raw_input", new BuiltinFunctions("raw_input", 34, 0, 1));
		dict.__setitem__("reduce", new BuiltinFunctions("reduce", 35, 2, 3));
		dict.__setitem__("reload", new BuiltinFunctions("reload", 36, 1));
		dict.__setitem__("repr", new BuiltinFunctions("repr", 37, 1));
		dict.__setitem__("round", new BuiltinFunctions("round", 38, 1, 2));
		dict.__setitem__("setattr", new BuiltinFunctions("setattr", 39, 3));
		dict.__setitem__("slice", new BuiltinFunctions("slice", 40, 1, 3));
		dict.__setitem__("vars", new BuiltinFunctions("vars", 41, 0, 1));
		dict.__setitem__("xrange", new BuiltinFunctions("xrange", 42, 1, 3));
		dict.__setitem__("zip", new BuiltinFunctions("zip", 43, 1, -1));
	
		dict.__setitem__("__import__", new ImportFunction());
	
	}

	public static PyObject abs(PyObject o) {
		if (o.isNumberType()) {
			return o.__abs__();
		}
		throw Py.TypeError("bad operand type for abs()");
	}

	public static PyObject apply(PyObject o, PyObject args) {
		return o.__call__(Py.make_array(args));
	}

	public static PyObject apply(PyObject o, PyObject args, PyDictionary kws) {
		PyObject[] a;
		String[] kw;
		Hashtable table = kws.table;
		if (table.size() > 0) {
			java.util.Enumeration ek = table.keys();
			java.util.Enumeration ev = table.elements();
			int n = table.size();
			kw = new String[n];
			PyObject[] aargs = Py.make_array(args);
			a = new PyObject[n + aargs.length];
			System.arraycopy(aargs, 0, a, 0, aargs.length);
			int offset = aargs.length;

			for (int i = 0; i < n; i++) {
				kw[i] = ((PyString) ek.nextElement()).internedString();
				a[i + offset] = (PyObject) ev.nextElement();
			}
			return o.__call__(a, kw);
		} else {
			return apply(o, args);
		}
	}

	public static PyObject bool(PyObject o) {
		return (o == null ? Py.Zero : o.__nonzero__() ? Py.One : Py.Zero);
	}

	public static boolean callable(PyObject o) {
		return o.__findattr__("__call__") != null;
	}

	public static char unichr(int i) {
		return chr(i);
	}

	public static char chr(int i) {
		if (i < 0 || i > 65535) {
			throw Py.ValueError("chr() arg not in range(65535)");
		}
		return (char) i;
	}

	public static int cmp(PyObject x, PyObject y) {
		return x._cmp(y);
	}

	public static PyTuple coerce(PyObject o1, PyObject o2) {
		PyObject[] result = o1._coerce(o2);
		if (result != null) {
			return new PyTuple(result);
		}
		throw Py.TypeError("number coercion failed");
	}

	public static PyCode compile(String data, String filename, String type) {
		return Py.compile_flags(data, filename, type, Py.getCompilerFlags());
	}

	public static PyCode compile(String data, String filename, String type, int flags, boolean dont_inherit) {
		if ((flags & ~PyTableCode.CO_ALL_FEATURES) != 0) {
			throw Py.ValueError("compile(): unrecognised flags");
		}
		return Py.compile_flags(data, filename, type, Py.getCompilerFlags(flags, dont_inherit));
	}

	public static void delattr(PyObject o, String n) {
		o.__delattr__(n);
	}

	public static PyObject dir(PyObject o) {
		PyList ret = (PyList) o.__dir__();
		ret.sort();
		return ret;
	}

	public static PyObject dir() {
		PyObject l = locals();
		PyList ret;

		if (l instanceof PyStringMap) {
			ret = ((PyStringMap) l).keys();
		} else if (l instanceof PyDictionary) {
			ret = ((PyDictionary) l).keys();
		}

		ret = (PyList) l.invoke("keys");
		ret.sort();
		return ret;
	}

	public static PyObject divmod(PyObject x, PyObject y) {
		return x._divmod(y);
	}

	public static PyEnumerate enumerate(PyObject seq) {
		return new PyEnumerate(seq);
	}

	public static PyObject eval(PyObject o, PyObject globals, PyObject locals) {
		PyCode code;
		if (o instanceof PyCode) {
			code = (PyCode) o;
		} else {
			if (o instanceof PyString) {
				code = compile(o.toString(), "<string>", "eval");
			} else {
				throw Py.TypeError("eval: argument 1 must be string or code object");
			}
		}
		return Py.runCode(code, locals, globals);
	}

	public static PyObject eval(PyObject o, PyObject globals) {
		return eval(o, globals, globals);
	}

	public static PyObject eval(PyObject o) {
		if (o instanceof PyTableCode && ((PyTableCode) o).hasFreevars()) {
			throw Py.TypeError("code object passed to eval() may not contain free variables");
		}
		return eval(o, null, null);
	}

	public static void execfile(String name, PyObject globals, PyObject locals) {
		execfile_flags(name, globals, locals, Py.getCompilerFlags());
	}

	public static void execfile_flags(String name, PyObject globals, PyObject locals, CompilerFlags cflags) {
		java.io.FileInputStream file;
		try {
			file = new java.io.FileInputStream(name);
		} catch (java.io.FileNotFoundException e) {
			throw Py.IOError(e);
		}
		PyCode code;

		try {
			code = Py.compile_flags(file, name, "exec", cflags);
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
		if (f == Py.None) {
			return s;
		}
		PyObject[] args = new PyObject[1];
		char[] chars = s.toString().toCharArray();
		int i;
		int j;
		int n = chars.length;
		for (i = 0, j = 0; i < n; i++) {
			args[0] = Py.makeCharacter(chars[i]);
			if (!f.__call__(args).__nonzero__()) {
				continue;
			}
			chars[j++] = chars[i];
		}
		return new PyString(new String(chars, 0, j));
	}

	public static PyObject filter(PyObject f, PyObject l) {
		if (l instanceof PyString) {
			return filter(f, (PyString) l);
		}
		PyList list = new PyList();
		PyObject iter = l.__iter__();
		for (PyObject item = null; (item = iter.__iternext__()) != null;) {
			if (f == Py.None) {
				if (!item.__nonzero__()) {
					continue;
				}
			} else if (!f.__call__(item).__nonzero__()) {
				continue;
			}
			list.append(item);
		}
		if (l instanceof PyTuple) {
			return tuple(list);
		}
		return list;
	}

	public static PyObject getattr(PyObject o, String n) {
		return o.__getattr__(n);
	}

	public static PyObject getattr(PyObject o, String n, PyObject def) {
		PyObject val = o.__findattr__(n);
		if (val != null) {
			return val;
		}
		return def;
	}

	public static PyObject globals() {
		return Py.getFrame().f_globals;
	}

	public static boolean hasattr(PyObject o, String n) {
		try {
			return o.__findattr__(n) != null;
		} catch (PyException exc) {
			if (Py.matchException(exc, Py.AttributeError)) {
				return false;
			}
			throw exc;
		}
	}

	public static PyInteger hash(PyObject o) {
		return o.__hash__();
	}

	public static PyString hex(PyObject o) {
		return o.__hex__();
	}

	public static long id(PyObject o) {
		return Py.id(o);
	}

	public static PyObject input(PyObject prompt) {
		String line = raw_input(prompt);
		return eval(new PyString(line));
	}

	public static PyObject input() {
		return input(new PyString(""));
	}

	private static PyStringMap internedStrings;

	public static PyString intern(PyString s) {
		if (internedStrings == null) {
			internedStrings = new PyStringMap();
		}

		String istring = s.internedString();
		PyObject ret = internedStrings.__finditem__(istring);
		if (ret != null) {
			return (PyString) ret;
		}
		if (s instanceof PyStringDerived) {
			s = s.__str__();
		}
		internedStrings.__setitem__(istring, s);
		return s;
	}

	// xxx find where used, modify with more appropriate if necessary
	public static boolean isinstance(PyObject obj, PyObject cls) {
		return Py.isInstance(obj, cls);
	}

	// xxx find where used, modify with more appropriate if necessary
	public static boolean issubclass(PyObject derived, PyObject cls) {
		return Py.isSubClass(derived, cls);
	}

	public static PyObject iter(PyObject obj) {
		return obj.__iter__();
	}

	public static PyObject iter(PyObject callable, PyObject sentinel) {
		return new PyCallIter(callable, sentinel);
	}

	public static int len(PyObject o) {
		try {
			return o.__len__();
		} catch (PyException e) {
			// Make this work like CPython where
			//
			// a = 7; len(a) raises a TypeError,
			// a.__len__() raises an AttributeError
			// and
			// class F: pass
			// f = F(); len(f) also raises an AttributeError
			//
			// Testing the type of o feels unclean though
			if (e.type == Py.AttributeError && !(o instanceof PyInstance)) {
				throw Py.TypeError("len() of unsized object");
			}
			throw e;
		}
	}

	public static PyObject locals() {
		return Py.getFrame().getf_locals();
	}

	public static PyObject map(PyObject[] argstar) {
		int n = argstar.length - 1;
		if (n < 1) {
			throw Py.TypeError("map requires at least two arguments");
		}
		PyObject element;
		PyObject f = argstar[0];
		PyList list = new PyList();
		PyObject[] args = new PyObject[n];
		PyObject[] iters = new PyObject[n];

		for (int j = 0; j < n; j++) {
			iters[j] = Py.iter(argstar[j + 1], "argument " + (j + 1) + " to map() must support iteration");
		}

		while (true) {
			boolean any_items = false;
			for (int j = 0; j < n; j++) {
				if ((element = iters[j].__iternext__()) != null) {
					args[j] = element;
					any_items = true;
				} else {
					args[j] = Py.None;
				}
			}
			if (!any_items) {
				break;
			}
			if (f == Py.None) {
				if (n == 1) {
					list.append(args[0]);
				} else {
					list.append(new PyTuple((PyObject[]) args.clone()));
				}
			} else {
				list.append(f.__call__(args));
			}
		}
		return list;
	}

	// I've never been happy with max and min builtin's...

	public static PyObject max(PyObject[] l) {
		if (l.length == 1) {
			return max(l[0]);
		}
		return max(new PyTuple(l));
	}

	private static PyObject max(PyObject o) {
		PyObject max = null;
		PyObject iter = o.__iter__();
		for (PyObject item; (item = iter.__iternext__()) != null;) {
			if (max == null || item._gt(max).__nonzero__()) {
				max = item;
			}
		}
		if (max == null) {
			throw Py.ValueError("max of empty sequence");
		}
		return max;
	}

	public static PyObject min(PyObject[] l) {
		if (l.length == 1) {
			return min(l[0]);
		}
		return min(new PyTuple(l));
	}

	private static PyObject min(PyObject o) {
		PyObject min = null;
		PyObject iter = o.__iter__();
		for (PyObject item; (item = iter.__iternext__()) != null;) {
			if (min == null || item._lt(min).__nonzero__()) {
				min = item;
			}
		}
		if (min == null) {
			throw Py.ValueError("min of empty sequence");
		}
		return min;
	}

	public static PyString oct(PyObject o) {
		return o.__oct__();
	}

	public static final int ord(char c) {
		return c;
	}

	public static PyObject pow(PyObject x, PyObject y) {
		return x._pow(y);
	}

	private static boolean coerce(PyObject[] objs) {
		PyObject x = objs[0];
		PyObject y = objs[1];
		PyObject[] result;
		result = x._coerce(y);
		if (result != null) {
			objs[0] = result[0];
			objs[1] = result[1];
			return true;
		}
		result = y._coerce(x);
		if (result != null) {
			objs[0] = result[1];
			objs[1] = result[0];
			return true;
		}
		return false;
	}

	public static PyObject pow(PyObject xi, PyObject yi, PyObject zi) {
		PyObject x = xi;
		PyObject y = yi;
		PyObject z = zi;

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
					z = tmp[1];
					y = tmp[0];
				}
			}
		} else {
			tmp[1] = z;
			if (coerce(tmp)) {
				x = tmp[0];
				z = tmp[1];
				tmp[0] = y;
				if (coerce(tmp)) {
					y = tmp[0];
					z = tmp[1];
					tmp[1] = x;
					if (coerce(tmp)) {
						x = tmp[1];
						y = tmp[0];
					}
				}
			}
		}

		if (x.getType() == y.getType() && x.getType() == z.getType()) {
			x = x.__pow__(y, z);
			if (x != null) {
				return x;
			}
		}
		throw Py.TypeError("__pow__ not defined for these operands");
	}

	public static PyObject range(int start, int stop, int step) {
		if (step == 0) {
			throw Py.ValueError("zero step for range()");
		}
		int n;
		if (step > 0) {
			n = (stop - start + step - 1) / step;
		} else {
			n = (stop - start + step + 1) / step;
		}
		if (n <= 0) {
			return new PyList();
		}
		PyObject[] l = new PyObject[n];
		int j = start;
		for (int i = 0; i < n; i++) {
			l[i] = Py.newInteger(j);
			j += step;
		}
		return new PyList(l);
	}

	public static PyObject range(int n) {
		return range(0, n, 1);
	}

	public static PyObject range(int start, int stop) {
		return range(start, stop, 1);
	}

	private static PyString readline(PyObject file) {
		if (file instanceof PyFile) {
			return new PyString(((PyFile) file).readline());
		} else {
			PyObject ret = file.invoke("readline");
			if (!(ret instanceof PyString)) {
				throw Py.TypeError("object.readline() returned non-string");
			}
			return (PyString) ret;
		}
	}

	public static String raw_input(PyObject prompt) {
		Py.print(prompt);
		PyObject stdin = Py.getSystemState().stdin;
		String data = readline(stdin).toString();
		if (data.endsWith("\n")) {
			return data.substring(0, data.length() - 1);
		} else {
			if (data.length() == 0) {
				throw Py.EOFError("raw_input()");
			}
		}
		return data;
	}

	public static String raw_input() {
		return raw_input(new PyString(""));
	}

	public static PyObject reduce(PyObject f, PyObject l, PyObject z) {
		PyObject result = z;
		PyObject iter = Py.iter(l, "reduce() arg 2 must support iteration");

		for (PyObject item; (item = iter.__iternext__()) != null;) {
			if (result == null) {
				result = item;
			} else {
				result = f.__call__(result, item);
			}
		}
		if (result == null) {
			throw Py.TypeError("reduce of empty sequence with no initial value");
		}
		return result;
	}

	public static PyObject reduce(PyObject f, PyObject l) {
		return reduce(f, l, null);
	}

	public static PyObject reload(PyModule o) {
		return imp.reload(o);
	}

	public static PyObject reload(PyJavaClass o) {
		return imp.reload(o);
	}

	public static PyString repr(PyObject o) {
		return o.__repr__();
	}

	// This seems awfully special purpose...
	public static PyFloat round(double f, int digits) {
		boolean neg = f < 0;
		double multiple = Math.pow(10., digits);
		if (neg) {
			f = -f;
		}
		double tmp = Math.floor(f * multiple + 0.5);
		if (neg) {
			tmp = -tmp;
		}
		return new PyFloat(tmp / multiple);
	}

	public static PyFloat round(double f) {
		return round(f, 0);
	}

	public static void setattr(PyObject o, String n, PyObject v) {
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

	public static PyObject sum(PyObject seq, PyObject result) {

		if (result instanceof PyString) {
			throw Py.TypeError("sum() can't sum strings [use ''.join(seq) instead]");
		}

		PyObject item;
		PyObject iter = seq.__iter__();
		while ((item = iter.__iternext__()) != null) {
			result = result._add(item);
		}
		return result;
	}

	public static PyObject sum(PyObject seq) {
		return sum(seq, Py.Zero);
	}

	public static PyTuple tuple(PyObject o) {
		if (o instanceof PyTuple) {
			return (PyTuple) o;
		}
		if (o instanceof PyList) {
			// always make a copy, otherwise the tuple will share the
			// underlying data structure with the list object, which
			// renders the tuple mutable!
			PyList l = (PyList) o;
			PyObject[] a = new PyObject[l.size()];
			System.arraycopy(l.getArray(), 0, a, 0, a.length);
			return new PyTuple(a);
		}
		return new PyTuple(Py.make_array(o));
	}

	public static PyType type(PyObject o) {
		return o.getType();
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
		return xrange(0, n, 1);
	}

	public static PyObject xrange(int start, int stop) {
		return xrange(start, stop, 1);
	}

	public static PyString __doc__zip = new PyString("zip(seq1 [, seq2 [...]]) -> [(seq1[0], seq2[0] ...), (...)]\n"
			+ "\n" + "Return a list of tuples, where each tuple contains the i-th element\n"
			+ "from each of the argument sequences.  The returned list is\n"
			+ "truncated in length to the length of the shortest argument sequence.");

	public static PyObject zip(PyObject[] argstar) {
		int itemsize = argstar.length;
		if (itemsize < 1) {
			throw Py.TypeError("zip requires at least one sequence");
		}

		// Type check the arguments; they must be sequences. Might as well
		// cache the __iter__() methods.
		PyObject[] iters = new PyObject[itemsize];

		for (int j = 0; j < itemsize; j++) {
			PyObject iter = argstar[j].__iter__();
			if (iter == null) {
				throw Py.TypeError("zip argument #" + (j + 1) + " must support iteration");
			}
			iters[j] = iter;
		}

		PyList ret = new PyList();

		for (int i = 0;; i++) {
			PyObject[] next = new PyObject[itemsize];
			PyObject item;

			for (int j = 0; j < itemsize; j++) {
				try {
					item = iters[j].__iternext__();
				} catch (PyException e) {
					if (Py.matchException(e, Py.StopIteration)) {
						return ret;
					}
					throw e;
				}
				if (item == null) {
					return ret;
				}
				next[j] = item;
			}
			ret.append(new PyTuple(next));
		}
	}

	public static PyObject __import__(String name) {
		return __import__(name, null, null, null);
	}

	public static PyObject __import__(String name, PyObject globals) {
		return __import__(name, globals, null, null);
	}

	public static PyObject __import__(String name, PyObject globals, PyObject locals) {
		return __import__(name, globals, locals, null);
	}

	public static PyObject __import__(String name, PyObject globals, PyObject locals, PyObject fromlist) {
		PyFrame frame = Py.getFrame();
		if (frame == null) {
			return null;
		}
		PyObject builtins = frame.f_builtins;
		if (builtins == null) {
			builtins = Py.getSystemState().builtins;
		}

		PyObject __import__ = builtins.__finditem__("__import__");
		if (__import__ == null) {
			return null;
		}

		PyObject module = __import__.__call__(new PyObject[] { Py.newString(name), globals, locals, fromlist });
		return module;
	}

}

class ImportFunction extends PyObject {
	public ImportFunction() {
	}

	public PyObject __call__(PyObject args[], String keywords[]) {
		if (!(args.length < 1 || args[0] instanceof PyString)) {
			throw Py.TypeError("first argument must be a string");
		}
		if (keywords.length > 0) {
			throw Py.TypeError("__import__() takes no keyword arguments");
		}

		int argc = args.length;
		String module = args[0].__str__().toString();

		PyObject globals = (argc > 1 && args[1] != null) ? args[1] : null;
		PyObject fromlist = (argc > 3 && args[3] != null) ? args[3] : Py.EmptyTuple;

		return load(module, globals, fromlist);
	}

	private PyObject load(String module, PyObject globals, PyObject fromlist) {
		PyObject mod = imp.importName(module.intern(), fromlist.__len__() == 0, globals, fromlist);
		return mod;
	}

	public String toString() {
		return "<built-in function __import__>";
	}
}
