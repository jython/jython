package org.python.core;

public class PyFrame extends PyObject {
	public PyFrame f_back;
	public PyTableCode f_code;
	public PyObject f_locals;
	public PyObject f_globals;
	public int f_lineno;
	public PyObject f_builtins;
	public PyObject[] f_fastlocals;
	
	public TraceFunction tracefunc;

    public static PyClass __class__;
	public PyFrame(PyTableCode code, PyObject locals, PyObject globals, PyObject builtins) {
	    super(__class__);
		f_code = code;
		f_locals = locals;
		f_globals = globals;
		f_builtins = builtins;
		// This needs work to be efficient with multiple interpreter states
		
		if (locals == null && code != null && code.co_varnames.length > 0) {
		    f_fastlocals = new PyObject[code.co_varnames.length];
		}
	}
	
	public PyFrame(PyTableCode code, PyObject globals) {
		this(code, null, globals, null);
	}	
	
    public String toString() {
        if (f_code == null) {
            return "<frame (unknown code) at line "+f_lineno+">";
        } else {
            return "<frame in \""+f_code.co_name+"\" at line "+f_lineno+">";
        }
    }

	public PyObject __findattr__(String name) {
        if (name == "f_locals") return getf_locals();
		return super.__findattr__(name);
	}

	public PyObject getf_locals() {
		if (f_locals == null) f_locals = new PyStringMap();
		if (f_fastlocals != null && f_code != null) {
			for(int i=0; i<f_fastlocals.length; i++) {
				PyObject o = f_fastlocals[i];
				if (o != null)
					f_locals.__setitem__(f_code.co_varnames[i], o);
			}
			// This should turn off fast_locals optimization after somebody gets the locals dict
			f_fastlocals = null;
		}
		return f_locals;
	}

	public void setline(int line) {
	    f_lineno = line;
	    if (tracefunc != null) {
	        tracefunc = tracefunc.traceLine(this, line);
	        //System.err.println(f_code.co_name+" : "+line+", "+tracefunc);
	    }
	}

	public int getline() {
		return f_lineno;
	}

	public PyObject getlocal(int index) {
		if (f_fastlocals != null) {
			PyObject ret = f_fastlocals[index];
			if (ret != null) return ret;
			//System.err.println("no local: "+index+", "+f_code.co_varnames[index]);
		}
		return getlocal(f_code.co_varnames[index]);
	}

	public PyObject getlocal(String index) {
		if (f_locals == null) getf_locals();
		PyObject ret = f_locals.__finditem__(index);
		if (ret != null) return ret;

		//throw Py.NameError("local: '"+index+"'");
		return getglobal(index);
	}

	public PyObject getglobal(String index) {
		PyObject ret = f_globals.__finditem__(index);
		if (ret != null) {
			return ret;
		}
		
		// Set up f_builtins if not already set
		if (f_builtins == null) {
		    System.err.println("Ooops, forced to set f_builtins in PyFrame: "+this+", "+index);
		    f_builtins = Py.getSystemState().builtins;
		}
		ret = f_builtins.__finditem__(index);
		if (ret != null) return ret;

		throw Py.NameError(index);
	}

	public void setlocal(int index, PyObject value) {
	    if (f_fastlocals != null) f_fastlocals[index] = value;
	    else setlocal(f_code.co_varnames[index], value);
	}

	public void setlocal(String index, PyObject value) {
		if (f_locals == null) getf_locals();
		f_locals.__setitem__(index, value);
	}

	public void setglobal(String index, PyObject value) {
		f_globals.__setitem__(index, value);
	}

	public void dellocal(int index) {
		if (f_fastlocals != null) f_fastlocals[index] = null;
		else dellocal(f_code.co_varnames[index]);
	}

	public void dellocal(String index) {
	    if (f_locals == null) getf_locals();
		f_locals.__delitem__(index);
	}

	public void delglobal(String index) {
		f_globals.__delitem__(index);
	}
}