package org.python.core;

public class PyString extends PySequence {
	private String string;
	private transient int cached_hashcode=0;
	private transient boolean interned=false;

    public static PyClass __class__;
	public PyString(String new_string) {
	    super(__class__);
		string = new_string;
	}
	
	public PyString(char c) {
	    this(String.valueOf(c));
	}

	public PyString __str__() {
		return this;
	}
	
	public int __len__() {
	    return string.length();
	}

	public String toString() { return string; }
	
	public String internedString() {
	    if (interned) {
	        return string;
	    } else {
	        string = string.intern();
	        interned = true;
	        return string;
	    }
	}


	// Do I need to do more for Unicode?
	public PyString __repr__() {
		char quote = '"';
		if (string.indexOf('\'') == -1 || string.indexOf('"') != -1) quote = '\'';
		StringBuffer buf = new StringBuffer(string.length()+5);
		buf.append(quote);
		for(int i=0; i<string.length(); i++) {
			char c = string.charAt(i);
			if (c == quote || c == '\\') {
				buf.append('\\');
				buf.append(c);
			} else {
				if (c < ' ' || c > 0177) {
					buf.append('\\');
					String s = Integer.toString(c, 8);
					while (s.length() < 3) s = "0"+s;
					buf.append(s);
				} else {
					buf.append(c);
				}
			}
		}
		buf.append(quote);
		return new PyString(buf.toString());
	}

	public boolean equals(Object other) {
		if (!interned) {
			string = string.intern();
			interned = true;
		}
		if (other instanceof PyString) {
			PyString o = (PyString)other;
			if (!o.interned) {
				o.string = o.string.intern();
				o.interned = true;
			}
			return string == o.string; //string.equals( ((PyString)other).string);
		}
		else return false;
	}

	public int __cmp__(PyObject other) {
		if (!(other instanceof PyString)) return -2;

		int c = string.compareTo(((PyString)other).string);
		return c < 0 ? -1 : c > 0 ? 1 : 0;
	}

	public int hashCode() {
		if (cached_hashcode == 0) cached_hashcode = string.hashCode();
		return cached_hashcode;
	}

	private byte[] getBytes() {
		byte[] buf = new byte[string.length()];
		string.getBytes(0, string.length(), buf, 0);
		return buf;
	}

	public Object __tojava__(Class c) {
		//This is a hack to make almost all Java calls happy
		if (c == String.class || c == Object.class) return string;
		if (c == Character.TYPE)
			if (string.length() == 1) return new Character(string.charAt(0));

		if (c.isArray() && c.getComponentType() == Byte.TYPE) {
			return getBytes();
		}

		if (c.isInstance(this)) return this;
		return Py.NoConversion;
	}

	protected PyObject get(int i) {
		return new PyString(string.substring(i,i+1));
	}

	protected PyObject getslice(int start, int stop, int step) {
		if (step == 1) {
			return new PyString(string.substring(start, stop));
		} else {
			int n = (stop-start)/step;
			char new_chars[] = new char[n];
			int j = 0;
			for(int i=start; j<n; i+=step) new_chars[j++] = string.charAt(i);
			return new PyString(new String(new_chars));
		}
	}

	protected PyObject repeat(int count) {
		int s = string.length();
		char new_chars[] = new char[s*count];
		for(int i=0; i<count; i++) {
			string.getChars(0, s, new_chars, i*s);
		}
		return new PyString(new String(new_chars));
	}

	public PyObject __add__(PyObject generic_other) {
		if (generic_other instanceof PyString) {
			return new PyString(string.concat(((PyString)generic_other).string));
		} else {
			return null;
		}
	}


	public PyObject __mod__(PyObject other) {
        StringFormatter fmt = new StringFormatter(string);
        return new PyString(fmt.format(other));
    }

 	public PyInteger __int__() {
 	    try {
		    return new PyInteger(Integer.valueOf(string).intValue());
		} catch (NumberFormatException exc) {
		    throw Py.ValueError("invalid literal for __int__: "+string);
		}
	}

	public PyLong __long__() {
 	    try {
    		return new PyLong(new java.math.BigInteger(string));
		} catch (NumberFormatException exc) {
		    throw Py.ValueError("invalid literal for __int__: "+string);
		}
	}

	public PyFloat __float__() {
 	    try {
    		return new PyFloat(Double.valueOf(string).doubleValue());
		} catch (NumberFormatException exc) {
		    throw Py.ValueError("invalid literal for __int__: "+string);
		}
	}
}

final class StringFormatter{
    int index;
    String format;
    StringBuffer buffer;
    boolean negative;
    int precision;
    int argIndex;
    PyObject args;

    final char pop() {
        try {
            return format.charAt(index++);
        } catch (StringIndexOutOfBoundsException e) {
            throw Py.ValueError("incomplete format");
        }
    }

    final char peek() {
        return format.charAt(index);
    }

    final void push() {
        index--;
    }

    public StringFormatter(String format) {
        index = 0;
        this.format = format;
        buffer = new StringBuffer(format.length()+100);
    }

    PyObject getarg() {
        PyObject ret = null;
        switch(argIndex) {
            case -3:
                break;
            case -2:
                return args;
            case -1:
                argIndex=-2;
                return args;
            default:
                ret = args.__finditem__(argIndex++);
                break;
        }
        if (ret == null)
            throw Py.ValueError("not enough arguments for format string");
        return ret;
    }

    int getNumber() {
        char c = pop();
        if (c == '*') {
            PyObject o = getarg();
            if (o instanceof PyInteger) return ((PyInteger)o).getValue();
            throw Py.TypeError("* wants int");
        } else {
           if (Character.isDigit(c)) {
                int numStart = index-1;
                while (Character.isDigit(c = pop())) {;}
                index -= 1;
                return Integer.valueOf(format.substring(numStart, index)).intValue();
            }
            index -= 1;
            return -1;
        }
    }

    public String formatInteger(PyObject arg, int radix, boolean unsigned) {
        return formatInteger(arg.__int__().getValue(), radix, unsigned);
    }

    public String formatInteger(long v, int radix, boolean unsigned) {
        if (unsigned) {
            if (v < 0) v = v & 0x7fffffff;
        } else {
            if (v < 0) { negative = true; v = -v; }
        }
        String s = Long.toString(v, radix);
        while (s.length() < precision) {
            s = "0"+s;
        }
        return s;
    }



    public String formatFloatDecimal(PyObject arg, boolean truncate) {
        return formatFloatDecimal(arg.__float__().getValue(), truncate);
    }

    public String formatFloatDecimal(double v, boolean truncate) {
        java.text.NumberFormat format = java.text.NumberFormat.getInstance();
        int prec = precision;
        if (prec == -1) prec = 6;
        format.setMaximumFractionDigits(prec);
        format.setMinimumFractionDigits(truncate ? 0 : prec);
        format.setGroupingUsed(false);

        return format.format(v);
    }

    public String formatFloatExponential(PyObject arg, char e, boolean truncate) {
        StringBuffer buf = new StringBuffer();
        double v = arg.__float__().getValue();
        double power = Math.floor(Math.log(v)/Math.log(10));

        double base = v/Math.pow(10, power);
        buf.append(formatFloatDecimal(base, truncate));
        buf.append(e);
        if (truncate) precision = -1;
        else precision = 3;
        String exp = formatInteger((long)power, 10, false);
        if (negative) { negative = false; buf.append('-'); }
        else {
            if (!truncate) buf.append('+');
        }
        buf.append(exp);

        return buf.toString();
    }

    public String format(PyObject args) {
        PyObject dict = null;
        this.args = args;
        if (args instanceof PyTuple) {
            argIndex = 0;
        } else {
            argIndex = -1;
            if (args instanceof PyDictionary ||
                    args instanceof PyStringMap ||
                    args.__findattr__("__getitem__") != null) {
                dict = args;
                argIndex = -2;
            }
        }

        while (index < format.length()) {
            boolean ljustFlag=false;
            boolean signFlag=false;
            boolean blankFlag=false;
            boolean altFlag=false;
            boolean zeroFlag=false;

            int width = -1;
            precision = -1;

            char c = pop();
            if (c != '%') {
                buffer.append(c);
                continue;
            }
            c = pop();
			if (c == '(') {
			    //System.out.println("( found");
		        if (dict == null)
		            throw Py.TypeError("format requires a mapping");
		        int parens = 1;
		        int keyStart = index;
		        while (parens > 0) {
		            c = pop();
		            if (c == ')') parens--;
		            else if (c == '(') parens++;
		        }
		        this.args = dict.__getitem__(new PyString(format.substring(keyStart, index-1)));
		        //System.out.println("args: "+args+", "+argIndex);
			} else {
			    push();
			}
            while (true) {
                switch(c = pop()) {
                    case '-': ljustFlag=true; continue;
                    case '+': signFlag=true; continue;
                    case ' ': blankFlag=true; continue;
                    case '#': altFlag=true; continue;
                    case '0': zeroFlag=true; continue;
                }
                break;
            }
            push();
            width = getNumber();
            c = pop();
            if (c == '.') {
                precision = getNumber();
                if (precision == -1) precision = 0;
                c = pop();
            }
            if (c == 'h' || c == 'l' || c == 'L') {
                c = pop();
            }
            if (c == '%') {
                buffer.append(c);
                continue;
            }
            PyObject arg = getarg();
            //System.out.println("args: "+args+", "+argIndex+", "+arg);
            char fill = ' ';
            String string=null;
            negative = false;
            if (zeroFlag) fill = '0';
            else fill = ' ';
            switch(c) {
                case 's':
                    fill = ' ';
                    string = arg.__str__().toString();
                    if (precision >= 0 && string.length() > precision) {
                        string = string.substring(0, precision);
                    }
                    break;
                case 'i':
                case 'd':
                    string = formatInteger(arg, 10, false);
                    break;
                case 'u':
                    string = formatInteger(arg, 10, true);
                    break;
                case 'o':
                    string = formatInteger(arg, 16, true);
                    if (altFlag) { string = "0" + string; ; }
                    break;
                case 'x':
                    string = formatInteger(arg, 16, true);
                    if (altFlag) { string = "0x" + string;   }
                    break;
                case 'X':
                    string = formatInteger(arg, 16, true);
                    //Do substitution of caps for lowercase here
                    if (altFlag) { string = "0X" + string; }
                    break;
                case 'e':
                case 'E':
                    string = formatFloatExponential(arg, c, false);
                    break;
                case 'f':
                    string = formatFloatDecimal(arg, false);
                    break;
                case 'g':
                case 'G':
                    int prec = precision;
                    if (prec == -1) prec = 6;
                    double v = arg.__float__().getValue();
                    int digits = (int)Math.ceil(ExtraMath.log10(v));
                    if (digits > 0) {
                        if (digits <= prec) {
                            precision = prec-digits;
                            string = formatFloatDecimal(arg, true);
                        } else {
                            string = formatFloatExponential(arg, (char)(c-2), true);
                        }
                    } else {
                        string = formatFloatDecimal(arg, true);
                    }
                    break;
                case 'c':
                    fill = ' ';
                    if (arg instanceof PyString) {
                        string = ((PyString)arg).toString();
                        if (string.length() != 1)
                            throw Py.TypeError("%c requires int or char");
                        break;
                    }
                    string = new Character((char)arg.__int__().getValue()).toString();
                    break;

                default:
                    throw Py.ValueError("unsupported format character '"+c+"'");
            }
            String signString = "";
            if (negative) {
                signString = "-";
            } else {
                if (signFlag) {
                    signString = "+";
                }
            }

            int length = string.length();
            if (width < length) width = length;
            if (ljustFlag && fill==' ') {
                buffer.append(signString);
                buffer.append(string);
                while (width-- > length) buffer.append(fill);
            } else {
                if (fill != ' ') {
                    buffer.append(signString);
                }
                while (width-- > length) buffer.append(fill);
                if (fill == ' ') {
                    buffer.append(signString);
                }
                buffer.append(string);
            }
        }
        return buffer.toString();
    }
}