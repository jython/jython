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
		boolean didhex = false;
		for(int i=0; i<string.length(); i++) {
			char c = string.charAt(i);
			if (didhex && Character.digit(c, 16) != -1) {
		        buf.append("\\x");
		        buf.append(Integer.toString(c, 16));
		        continue;
			}
			
			didhex = false;
			if (c == quote || c == '\\') {
				buf.append('\\');
				buf.append(c);
			} else {
			    if (c >= ' ' && c <= 0177) {
			        buf.append(c);
			    } 
			    else if (c == '\n') buf.append("\\n");
			    else if (c == '\t') buf.append("\\t");
			    else if (c == '\b') buf.append("\\b");
			    else if (c == '\f') buf.append("\\f");
			    else if (c == '\r') buf.append("\\r");
			    else {
			        buf.append("\\x");
			        buf.append(Integer.toString(c, 16));
			        didhex = true;
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
		    int n = sliceLength(start, stop, step);
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
		    throw Py.ValueError("invalid literal for __long__: "+string);
		}
	}

	public PyFloat __float__() {
 	    try {
    		return new PyFloat(Double.valueOf(string).doubleValue());
		} catch (NumberFormatException exc) {
		    throw Py.ValueError("invalid literal for __float__: "+string);
		}
	}
	
	// Add in methods from string module
    public String lower() {
        return string.toLowerCase();
    }

    public String upper() {
        return string.toUpperCase();
    }

    public String swapcase() {
        char[] chars = string.toCharArray();
        int n=chars.length;
        for(int i=0; i<n; i++) {
            char c = chars[i];
            if (Character.isUpperCase(c)) {
                chars[i] = Character.toLowerCase(c);
            } else if (Character.isLowerCase(c)) {
                chars[i] = Character.toUpperCase(c);
            }
        }
        return new String(chars);
    }

    public String strip() {
        char[] chars = string.toCharArray();
        int n=chars.length;
        int start=0;
        while (start < n && Character.isWhitespace(chars[start])) start++;

        int end=n-1;
        while (end >= 0 && Character.isWhitespace(chars[end])) end--;

        if (end >= start) {
            return (end < n-1 || start > 0) ? string.substring(start, end+1) : string;
        } else {
            return "";
        }
    }

    public String lstrip() {
        char[] chars = string.toCharArray();
        int n=chars.length;
        int start=0;
        while (start < n && Character.isWhitespace(chars[start])) start++;

        return (start > 0) ? string.substring(start, n) : string;
    }

    public String rstrip() {
        char[] chars = string.toCharArray();
        int n=chars.length;
        int end=n-1;
        while (end >= 0 && Character.isWhitespace(chars[end])) end--;

        return (end < n-1) ? string.substring(0, end+1) : string;
    }


    public PyList split(String sep) {
        return split(sep, 0);
    }

    public PyList split() {
        return split(null, 0);
    }

    public PyList split(String sep, int maxsplit) {
        if (sep != null) return splitfields(sep, maxsplit);

        PyList list = new PyList();

        char[] chars = string.toCharArray();
        int n=chars.length;

        if (maxsplit <= 0) maxsplit = n;

        int splits=0;
        int index=0;
        while (index < n && splits < maxsplit) {
            while (index < n && Character.isWhitespace(chars[index])) index++;
            if (index == n) break;
            int start = index;

            while (index < n && !Character.isWhitespace(chars[index])) index++;
            list.append(new PyString(string.substring(start, index)));
            splits++;
        }
        if (index < n) {
            while (index < n && Character.isWhitespace(chars[index])) index++;
            list.append(new PyString(string.substring(index, n)));
        }
        return list;
    }

    private PyList splitfields(String sep, int maxsplit) {        
        if (sep.length() == 0) {
            throw Py.ValueError("empty separator");
        }

        PyList list = new PyList();

        char[] chars = string.toCharArray();
        int n=chars.length;

        if (maxsplit <= 0) maxsplit = n;

        int splits=0;
        int index=0;
        char firstChar = sep.charAt(0);
        int nsep = sep.length();
        int lastbreak;
        while (index < n && splits < maxsplit) {
            lastbreak = index;
            while (index < n) {
                if (chars[index] == firstChar) {
                    if (nsep == 1) break;
                    int j;
                    for(j=1; j<nsep; j++) {
                        if (chars[index+j] != sep.charAt(j)) break;
                    }
                    if (j == nsep) break;
                }
                index++;
            }
            list.append(new PyString(string.substring(lastbreak, index)));
            index += nsep;
            splits++;
        }
        if (index <= n) {
            list.append(new PyString(string.substring(index, n)));
        }
        return list;
    }

    public int index(String sub) {
        return index(sub, 0, string.length());
    }

    public int index(String sub, int start) {
        return index(sub, start, string.length());
    }

    public int index(String sub, int start, int end) {
        int n = string.length();

        if (start < 0) start = n+start;
        if (end < 0) end = n+end;

        int index;
        if (end < n) {
            index = string.substring(start, end).indexOf(sub);
        } else {
            index = string.indexOf(sub, start);
        }
        if (index == -1) throw Py.ValueError("substring not found in string.index");
        return index;
    }

    public int rindex(String sub) {
        return rindex(sub, 0, string.length());
    }

    public int rindex(String sub, int start) {
        return rindex(sub, start, string.length());
    }

    public int rindex(String sub, int start, int end) {
        int n = string.length();

        if (start < 0) start = n+start;
        if (end < 0) end = n+end;

        int index;
        if (start > 0) {
            index = string.substring(start, end).lastIndexOf(sub);
        } else {
            index = string.lastIndexOf(sub, end);
        }
        if (index == -1) throw Py.ValueError("substring not found in string.rindex");
        return index;
    }

    public int count(String sub) {
        return count(sub, 0, string.length());
    }
    public int count(String sub, int start) {
        return count(sub, start, string.length());
    }

    public int count(String sub, int start, int end) {
        int n = string.length();
        if (start < 0) start = n+start;
        if (end < 0) end = n+end;
        if (end > n) end = n;
        if (start > end) start = end;

        int slen = sub.length();
        //end = end-slen;

        int count=0;
        while (start < end) {
            int index = string.indexOf(sub, start);
            if (index >= end || index == -1) break;
            count++;
            start = index+slen;
        }
        return count;
    }
 
    public int find(String sub) {
        return find(sub, 0, string.length());
    }
    
    public int find(String sub, int start) {
        return find(sub, start, string.length());
    }
    
    public int find(String sub, int start, int end) {
        int n = string.length();
        if (start < 0) start = n+start;
        if (end < 0) end = n+end;
        if (end > n) end = n;
        if (start > end) start = end;
        int slen = sub.length();
        end = end-slen;

        int index = string.indexOf(sub, start);
        if (index > end) return -1;
        return index;
    }
    
    public int rfind(String sub) {
        return rfind(sub, 0, string.length());
    }
    
    public int rfind(String sub, int start) {
        return rfind(sub, start, string.length());
    }
    
    public int rfind(String sub, int start, int end) {
        int n = string.length();
        if (start < 0) start = n+start;
        if (end < 0) end = n+end;
        if (end > n) end = n;
        if (start > end) start = end;
        int slen = sub.length();
        end = end-slen;

        int index = string.lastIndexOf(sub, end);
        if (index < start) return -1;
        return index;
    }


    public double atof() {
        try {
            return Double.valueOf(string.trim()).doubleValue();
        } catch (NumberFormatException exc) {
            throw Py.ValueError("non-float argument to string.atof");
        }
    }

    public int atoi() {
        return atoi(10);
    }

    public int atoi(int base) {
        if ((base != 0 && base < 2) || (base > 36)) {
            throw Py.ValueError("invalid base for atoi()");
        }
        
        String s = string.trim();
        boolean neg = false;
        try {
            char sign = s.charAt(0);
            if (sign == '+') {
                s = s.substring(1,s.length()).trim();
            }
            if (base == 0 || base == 16) {
                if (sign == '-') {
                    neg = true;
                    s = s.substring(1, s.length()).trim();
                }
                if (s.charAt(0) == '0') {
                    if (s.charAt(1) == 'x') {
                        if (base == 0) base = 16;
                        s = s.substring(2,s.length());
                    } else {
                        if (base == 0) base = 8;
                    }
                }  
            }
            if (base == 0) base = 10;
        } catch (IndexOutOfBoundsException ex) {
            throw Py.ValueError("non-integer argument to string.atoi");
        }
        
        try {
            int value = Integer.valueOf(s, base).intValue();
            if (neg) return -value;
            else return value;
        } catch (NumberFormatException exc) {
            throw Py.ValueError("non-integer argument to string.atoi");
        }
    }

    public PyLong atol() {
        return atol(10);
    }

    public PyLong atol(int base) {
        if ((base != 0 && base < 2) || (base > 36)) {
            throw Py.ValueError("invalid base for atol()");
        }
        
        String s = string.trim();
        boolean neg = false;
        try {
            char sign = s.charAt(0);
            if (sign == '+') {
                s = s.substring(1,s.length()).trim();
            }
            if (base == 0 || base == 16) {
                if (sign == '-') {
                    neg = true;
                    s = s.substring(1, s.length()).trim();
                }
                if (base == 0) {
                    char lastChar = s.charAt(s.length()-1);                    
                    if (lastChar == 'l' || lastChar == 'L') 
                        s = s.substring(0, s.length()-1);                    
                }
                if (s.charAt(0) == '0') {
                    if (s.charAt(1) == 'x') {
                        if (base == 0) base = 16;
                        s = s.substring(2,s.length());
                    } else {
                        if (base == 0) base = 8;
                    }
                }
            }
            if (base == 0) base = 10;
        } catch (IndexOutOfBoundsException ex) {
            throw Py.ValueError("non-integer argument to string.atol 1");
        }
        try {
            java.math.BigInteger value = new java.math.BigInteger(s, base);
            if (neg) return new PyLong(value.negate());
            else return new PyLong(value);
        } catch (NumberFormatException exc) {
            throw Py.ValueError("non-integer argument to string.atol");
        }
    }

    private static String spaces(int n) {
        char[] chars = new char[n];
        for(int i=0; i<n; i++) chars[i] = ' ';
        return new String(chars);
    }

    public String ljust(int width) {
        int n = width-string.length();
        if (n <= 0) return string;
        return string+spaces(n);
    }

    public String rjust(int width) {
        int n = width-string.length();
        if (n <= 0) return string;
        return spaces(n)+string;
    }

    public String center(int width) {
        int n = width-string.length();
        if (n <= 0) return string;
        int half = n/2;
        if (n%2 > 0 &&  width%2 > 0) half += 1;
        return spaces(half)+string+spaces(n-half);
    }

    public String zfill(int width) {
        String s = string;
        int n = s.length();
        if (n >= width) return s;
        char start = s.charAt(0);
        char[] chars = new char[width];
        int nzeros = width-n;
        int i=0;
        int sStart=0;
        if (start == '+' || start == '-') {
            chars[0] = start;
            i += 1;
            nzeros++;
            sStart=1;
        }
        for(;i<nzeros; i++) {
            chars[i] = '0';
        }
        s.getChars(sStart, s.length(), chars, i);
        return new String(chars);
    }

    public String expandtabs() {
        return expandtabs(8);
    }
    
    public String expandtabs(int tabsize) {
        String s = string;
        StringBuffer buf = new StringBuffer((int)(s.length()*1.5));
        char[] chars = s.toCharArray();
        int n = chars.length;
        int position = 0;
        
        for(int i=0; i<n; i++) {
            char c = chars[i];
            if (c == '\t') {
                int spaces = tabsize-position%tabsize;
                position += spaces;
                while (spaces-- > 0) {
                    buf.append(' ');
                }
                continue;
            } 
            if (c == '\n') {
                position = -1;
            }
            buf.append(c);
            position++;
        }
        return buf.toString();
    }
    
    public String capitalize() {
        if (string.length() == 0) return string;
        String first = string.substring(0,1).toUpperCase();
        return first.concat(string.substring(1,string.length()).toLowerCase());
    }   
    
    public String replace(String oldPiece, String newPiece) {
        return replace(oldPiece, newPiece, string.length());
    }
    
    public String replace(String oldPiece, String newPiece, int maxsplit) {
        return joinfields(split(oldPiece, maxsplit), newPiece);
    }
    
    public static String join(PyObject seq) {
        return join(seq, " ");
    }

    public static String join(PyObject seq, String sep) {
        int i=0;
        StringBuffer buf = new StringBuffer();
        PyObject obj;
        boolean addSep=false;

        while ((obj = seq.__finditem__(i++)) != null) {
            if (addSep) buf.append(sep);
            buf.append(obj.toString());
            addSep = true;
        }
        return buf.toString();
    }

    public static String joinfields(PyObject seq) {
        return join(seq, " ");
    }

    public static String joinfields(PyObject seq, String sep) {
        return join(seq, sep);
    }
    
    public boolean startswith(String prefix) {
        return string.startsWith(prefix);
    }
    
    public boolean endswith(String suffix) {
        return string.endsWith(suffix);
    }
    
    //public static String zfill(PyObject o, int width) {
    //    return zfill(o.toString(), width);
    //}    
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
            // special index indicating a single item that has already been used
            case -2:
                break;
            // special index indicating a single item that has not yet been used
            case -1:
                argIndex=-2;
                return args;
            default:
                ret = args.__finditem__(argIndex++);
                break;
        }
        if (ret == null)
            throw Py.TypeError("not enough arguments for format string");
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
            if (v < 0) v = 0x100000000l + v;
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
        if (v < 0) {
            v = -v;
            negative = true;
        }
        format.setMaximumFractionDigits(prec);
        format.setMinimumFractionDigits(truncate ? 0 : prec);
        format.setGroupingUsed(false);

        //System.err.println("formatFloat: "+v+", "+prec);
        String ret = format.format(v);
        if (ret.indexOf('.') == -1) {
            return ret+'.';
        }
        return ret;
    }

    public String formatFloatExponential(PyObject arg, char e, boolean truncate) {
        StringBuffer buf = new StringBuffer();
        double v = arg.__float__().getValue();
        boolean isNegative = false;
        if (v < 0) {
            v = -v;
            isNegative = true;
        }
        double power = 0.0;
        if (v > 0) power = Math.floor(Math.log(v)/Math.log(10));
        //System.err.println("formatExp: "+v+", "+power);
        int savePrecision = precision;
        
        if (truncate) precision = -1;
        else precision = 3;
        
        String exp = formatInteger((long)power, 10, false);
        if (negative) { negative = false; exp = '-'+exp; }
        else {
            if (!truncate) exp = '+'+exp;
        }
        
        precision = savePrecision;
        
        double base = v/Math.pow(10, power);
        buf.append(formatFloatDecimal(base, truncate));
        buf.append(e);

        buf.append(exp);
        negative = isNegative;

        return buf.toString();
    }

    public String format(PyObject args) {
        PyObject dict = null;
        this.args = args;
        if (args instanceof PyTuple) {
            argIndex = 0;
        } else {
            // special index indicating a single item rather than a tuple
            argIndex = -1;
            if (args instanceof PyDictionary ||
                    args instanceof PyStringMap ||
                    args.__findattr__("__getitem__") != null) {
                dict = args;
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
		        this.argIndex = -1;
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
                    string = formatInteger(arg, 8, true);
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
                } else if (blankFlag) {
		    signString = " ";
		}
            }

            int length = string.length() + signString.length();
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
        if (argIndex == -1 || (argIndex >= 0 && args.__finditem__(argIndex) != null)) {
            throw Py.TypeError("not all arguments converted");
        }
        return buffer.toString();
    }
}
