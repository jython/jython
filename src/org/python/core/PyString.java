/// Copyright (c) Corporation for National Research Initiatives
package org.python.core;

import org.python.core.stringlib.FieldNameIterator;
import org.python.core.stringlib.InternalFormatSpec;
import org.python.core.stringlib.InternalFormatSpecParser;
import org.python.core.stringlib.MarkupIterator;
import org.python.core.util.ExtraMath;
import org.python.core.util.StringUtil;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedType;
import org.python.expose.MethodType;

import java.math.BigInteger;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

/**
 * A builtin python string.
 */
@ExposedType(name = "str", doc = BuiltinDocs.str_doc)
public class PyString extends PyBaseString implements MemoryViewProtocol
{
    public static final PyType TYPE = PyType.fromClass(PyString.class);
    protected String string; // cannot make final because of Python intern support
    protected transient boolean interned=false;

    public String getString() {
        return string;
    }

    // for PyJavaClass.init()
    public PyString() {
        this(TYPE, "");
    }

    public PyString(PyType subType, String string) {
        super(subType);
        if (string == null) {
            throw new IllegalArgumentException(
                            "Cannot create PyString from null!");
        }
        this.string = string;
    }

    public PyString(String string) {
        this(TYPE, string);
    }

    public PyString(char c) {
        this(TYPE,String.valueOf(c));
    }

    PyString(StringBuilder buffer) {
        this(TYPE, new String(buffer));
    }
    
    /**
     * Creates a PyString from an already interned String. Just means it won't
     * be reinterned if used in a place that requires interned Strings.
     */
    public static PyString fromInterned(String interned) {
        PyString str = new PyString(TYPE, interned);
        str.interned = true;
        return str;
    }

    @ExposedNew
    static PyObject str_new(PyNewWrapper new_, boolean init, PyType subtype,
            PyObject[] args, String[] keywords) {
        ArgParser ap = new ArgParser("str", args, keywords, new String[] { "object" }, 0);
        PyObject S = ap.getPyObject(0, null);
        if(new_.for_type == subtype) {
            if(S == null) {
                return new PyString("");
            }
            return new PyString(S.__str__().toString());
        } else {
            if (S == null) {
                return new PyStringDerived(subtype, "");
            }
            return new PyStringDerived(subtype, S.__str__().toString());
        }
    }

    public int[] toCodePoints() {
        int n = getString().length();
        int[] codePoints = new int[n];
        for (int i = 0; i < n; i++) {
            codePoints[i] = getString().charAt(i);
        }
        return codePoints;
    }

    public MemoryView getMemoryView() {
        return new MemoryView() {
            // beginning of support
            public String get_format() {
                 return "B";
            }
            public int get_itemsize() {
                return 2;
            }
            public PyTuple get_shape() {
                return new PyTuple(Py.newInteger(getString().length()));
            }
            public int get_ndim() {
                return 1;
            }
            public PyTuple get_strides() {
                return new PyTuple(Py.newInteger(1));
            }
            public boolean get_readonly() {
                return true;
            }
        };
    }

    public String substring(int start, int end) {
        return getString().substring(start, end);
    }
    
    @Override
    public PyString __str__() {
        return str___str__();
    }

    public

    @ExposedMethod(doc = BuiltinDocs.str___str___doc) 
    final PyString str___str__() {
        if (getClass() == PyString.class) {
            return this;
        }
        return new PyString(getString());
    }

    @Override
    public PyUnicode __unicode__() {
        return new PyUnicode(this);
    }

    @Override
    public int __len__() {
        return str___len__();
    }

    @ExposedMethod(doc = BuiltinDocs.str___len___doc)
    final int str___len__() {
        return getString().length();
    }

    @Override
    public String toString() {
        return getString();
    }

    public String internedString() {
        if (interned)
            return getString();
        else {
            string = getString().intern();
            interned = true;
            return getString();
        }
    }

    @Override
    public PyString __repr__() {
        return str___repr__();
    }

    @ExposedMethod(doc = BuiltinDocs.str___repr___doc)
    final PyString str___repr__() {
        return new PyString(encode_UnicodeEscape(getString(), true));
    }

    private static char[] hexdigit = "0123456789abcdef".toCharArray();

    public static String encode_UnicodeEscape(String str,
                                              boolean use_quotes)
    {
        int size = str.length();
        StringBuilder v = new StringBuilder(str.length());

        char quote = 0;

        if (use_quotes) {
            quote = str.indexOf('\'') >= 0 &&
                             str.indexOf('"') == -1 ? '"' : '\'';
            v.append(quote);
        }

        for (int i = 0; size-- > 0; ) {
            int ch = str.charAt(i++);
            /* Escape quotes */
            if ((use_quotes && ch == quote) || ch == '\\') {
                v.append('\\');
                v.append((char) ch);
                continue;
            }
                /* Map UTF-16 surrogate pairs to Unicode \UXXXXXXXX escapes */
                else if (ch >= 0xD800 && ch < 0xDC00) {
                    char ch2 = str.charAt(i++);
                    size--;
                    if (ch2 >= 0xDC00 && ch2 <= 0xDFFF) {
                    int ucs = (((ch & 0x03FF) << 10) | (ch2 & 0x03FF)) + 0x00010000;
                    v.append('\\');
                    v.append('U');
                    v.append(hexdigit[(ucs >> 28) & 0xf]);
                    v.append(hexdigit[(ucs >> 24) & 0xf]);
                    v.append(hexdigit[(ucs >> 20) & 0xf]);
                    v.append(hexdigit[(ucs >> 16) & 0xf]);
                    v.append(hexdigit[(ucs >> 12) & 0xf]);
                    v.append(hexdigit[(ucs >> 8) & 0xf]);
                    v.append(hexdigit[(ucs >> 4) & 0xf]);
                    v.append(hexdigit[ucs & 0xf]);
                    continue;
                    }
                    /* Fall through: isolated surrogates are copied as-is */
                    i--;
                    size++;
                }
            /* Map 16-bit characters to '\\uxxxx' */
             if (ch >= 256) {
                v.append('\\');
                v.append('u');
                v.append(hexdigit[(ch >> 12) & 0xf]);
                v.append(hexdigit[(ch >> 8) & 0xf]);
                v.append(hexdigit[(ch >> 4) & 0xf]);
                v.append(hexdigit[ch & 15]);
            }
             /* Map special whitespace to '\t', \n', '\r' */
            else if (ch == '\t') v.append("\\t");
            else if (ch == '\n') v.append("\\n");
            else if (ch == '\r') v.append("\\r");
            /* Map non-printable US ASCII to '\ooo' */
            else if (ch < ' ' || ch >= 127) {
                v.append('\\');
                v.append('x');
                v.append(hexdigit[(ch >> 4) & 0xf]);
                v.append(hexdigit[ch & 0xf]);
            }
            /* Copy everything else as-is */
            else
                v.append((char) ch);
        }
        if (use_quotes)
            v.append(quote);
        return v.toString();
    }

    private static ucnhashAPI pucnHash = null;

    
    public static String decode_UnicodeEscape(String str,
                                              int start,
                                              int end,
                                              String errors,
                                              boolean unicode) {
        StringBuilder v = new StringBuilder(end - start);
        for(int s = start; s < end;) {
            char ch = str.charAt(s);
            /* Non-escape characters are interpreted as Unicode ordinals */
            if(ch != '\\') {
                v.append(ch);
                s++;
                continue;
            }
            int loopStart = s;
            /* \ - Escapes */
            s++;
            if(s == end) {
                s = codecs.insertReplacementAndGetResume(v,
                                                         errors,
                                                         "unicodeescape",
                                                         str,
                                                         loopStart,
                                                         s + 1,
                                                         "\\ at end of string");
                continue;
            }
            ch = str.charAt(s++);
            switch(ch){
                /* \x escapes */
                case '\n':
                    break;
                case '\\':
                    v.append('\\');
                    break;
                case '\'':
                    v.append('\'');
                    break;
                case '\"':
                    v.append('\"');
                    break;
                case 'b':
                    v.append('\b');
                    break;
                case 'f':
                    v.append('\014');
                    break; /* FF */
                case 't':
                    v.append('\t');
                    break;
                case 'n':
                    v.append('\n');
                    break;
                case 'r':
                    v.append('\r');
                    break;
                case 'v':
                    v.append('\013');
                    break; /* VT */
                case 'a':
                    v.append('\007');
                    break; /* BEL, not classic C */
                /* \OOO (octal) escapes */
                case '0':
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                    int x = Character.digit(ch, 8);
                    for(int j = 0; j < 2 && s < end; j++, s++) {
                        ch = str.charAt(s);
                        if(ch < '0' || ch > '7')
                            break;
                        x = (x << 3) + Character.digit(ch, 8);
                    }
                    v.append((char)x);
                    break;
                case 'x':
                    s = hexescape(v, errors, 2, s, str, end, "truncated \\xXX");
                    break;
                case 'u':
                    if(!unicode) {
                        v.append('\\');
                        v.append('u');
                        break;
                    }
                    s = hexescape(v,
                                  errors,
                                  4,
                                  s,
                                  str,
                                  end,
                                  "truncated \\uXXXX");
                    break;
                case 'U':
                    if(!unicode) {
                        v.append('\\');
                        v.append('U');
                        break;
                    }
                    s = hexescape(v,
                                  errors,
                                  8,
                                  s,
                                  str,
                                  end,
                                  "truncated \\UXXXXXXXX");
                    break;
                case 'N':
                    if(!unicode) {
                        v.append('\\');
                        v.append('N');
                        break;
                    }
                    /*
                     * Ok, we need to deal with Unicode Character Names now,
                     * make sure we've imported the hash table data...
                     */
                    if(pucnHash == null) {
                        PyObject mod = imp.importName("ucnhash", true);
                        mod = mod.__call__();
                        pucnHash = (ucnhashAPI)mod.__tojava__(Object.class);
                        if(pucnHash.getCchMax() < 0)
                            throw Py.UnicodeError("Unicode names not loaded");
                    }
                    if(str.charAt(s) == '{') {
                        int startName = s + 1;
                        int endBrace = startName;
                        /*
                         * look for either the closing brace, or we exceed the
                         * maximum length of the unicode character names
                         */
                        int maxLen = pucnHash.getCchMax();
                        while(endBrace < end && str.charAt(endBrace) != '}'
                                && (endBrace - startName) <= maxLen) {
                            endBrace++;
                        }
                        if(endBrace != end && str.charAt(endBrace) == '}') {
                            int value = pucnHash.getValue(str,
                                                          startName,
                                                          endBrace);
                            if(storeUnicodeCharacter(value, v)) {
                                s = endBrace + 1;
                            } else {
                                s = codecs.insertReplacementAndGetResume(v,
                                                                         errors,
                                                                         "unicodeescape",
                                                                         str,
                                                                         loopStart,
                                                                         endBrace + 1,
                                                                         "illegal Unicode character");
                            }
                        } else {
                            s = codecs.insertReplacementAndGetResume(v,
                                                                     errors,
                                                                     "unicodeescape",
                                                                     str,
                                                                     loopStart,
                                                                     endBrace,
                                                                     "malformed \\N character escape");
                        }
                        break;
                    } else {
                        s = codecs.insertReplacementAndGetResume(v,
                                                                 errors,
                                                                 "unicodeescape",
                                                                 str,
                                                                 loopStart,
                                                                 s + 1,
                                                                 "malformed \\N character escape");
                    }
                    break;
                default:
                    v.append('\\');
                    v.append(str.charAt(s - 1));
                    break;
            }
        }
        return v.toString();
    }

    private static int hexescape(StringBuilder partialDecode,
                                 String errors,
                                 int digits,
                                 int hexDigitStart,
                                 String str,
                                 int size,
                                 String errorMessage) {
        if(hexDigitStart + digits > size) {
            return codecs.insertReplacementAndGetResume(partialDecode,
                                                        errors,
                                                        "unicodeescape",
                                                        str,
                                                        hexDigitStart - 2,
                                                        size,
                                                        errorMessage);
        }
        int i = 0;
        int x = 0;
        for(; i < digits; ++i) {
            char c = str.charAt(hexDigitStart + i);
            int d = Character.digit(c, 16);
            if(d == -1) {
                return codecs.insertReplacementAndGetResume(partialDecode,
                                                            errors,
                                                            "unicodeescape",
                                                            str,
                                                            hexDigitStart - 2,
                                                            hexDigitStart + i + 1,
                                                            errorMessage);
            }
            x = (x << 4) & ~0xF;
            if(c >= '0' && c <= '9')
                x += c - '0';
            else if(c >= 'a' && c <= 'f')
                x += 10 + c - 'a';
            else
                x += 10 + c - 'A';
        }
        if(storeUnicodeCharacter(x, partialDecode)) {
            return hexDigitStart + i;
        } else {
            return codecs.insertReplacementAndGetResume(partialDecode,
                                                        errors,
                                                        "unicodeescape",
                                                        str,
                                                        hexDigitStart - 2,
                                                        hexDigitStart + i + 1,
                                                        "illegal Unicode character");
        }
    }

    /*pass in an int since this can be a UCS-4 character */
    private static boolean storeUnicodeCharacter(int value,
            StringBuilder partialDecode) {
        if (value < 0 || (value >= 0xD800 && value <= 0xDFFF)) {
            return false;
        } else if (value <= PySystemState.maxunicode) {
            partialDecode.appendCodePoint(value);
            return true;
        }
        return false;
    }

    @ExposedMethod(doc = BuiltinDocs.str___getitem___doc)
    final PyObject str___getitem__(PyObject index) {
        PyObject ret = seq___finditem__(index);
        if (ret == null) {
            throw Py.IndexError("string index out of range");
        }
        return ret;
    }
    
    //XXX: need doc
    @ExposedMethod(defaults = "null")
    final PyObject str___getslice__(PyObject start, PyObject stop, PyObject step) {
        return seq___getslice__(start, stop, step);
    }

    @Override
    public int __cmp__(PyObject other) {
        return str___cmp__(other);
    }

    @ExposedMethod(type = MethodType.CMP)
    final int str___cmp__(PyObject other) {
        if (!(other instanceof PyString))
            return -2;

        int c = getString().compareTo(((PyString) other).getString());
        return c < 0 ? -1 : c > 0 ? 1 : 0;
    }

    @Override
    public PyObject __eq__(PyObject other) {
        return str___eq__(other);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.str___eq___doc)
    final PyObject str___eq__(PyObject other) {
        String s = coerce(other);
        if (s == null)
            return null;
        return getString().equals(s) ? Py.True : Py.False;
    }

    @Override
    public PyObject __ne__(PyObject other) {
        return str___ne__(other);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.str___ne___doc)
    final PyObject str___ne__(PyObject other) {
        String s = coerce(other);
        if (s == null)
            return null;
        return getString().equals(s) ? Py.False : Py.True;
    }
    
    @Override
    public PyObject __lt__(PyObject other) {
        return str___lt__(other);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.str___lt___doc)
    final PyObject str___lt__(PyObject other){
        String s = coerce(other);
        if (s == null)
            return null;
        return getString().compareTo(s) < 0 ? Py.True : Py.False;
    }

    @Override
    public PyObject __le__(PyObject other) {
        return str___le__(other);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.str___le___doc)
    final PyObject str___le__(PyObject other){
        String s = coerce(other);
        if (s == null)
            return null;
        return getString().compareTo(s) <= 0 ? Py.True : Py.False;
    }

    @Override
    public PyObject __gt__(PyObject other) {
        return str___gt__(other);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.str___gt___doc)
    final PyObject str___gt__(PyObject other){
        String s = coerce(other);
        if (s == null)
            return null;
        return getString().compareTo(s) > 0 ? Py.True : Py.False;
    }

    @Override
    public PyObject __ge__(PyObject other) {
        return str___ge__(other);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.str___ge___doc)
    final PyObject str___ge__(PyObject other){
        String s = coerce(other);
        if (s == null)
            return null;
        return getString().compareTo(s) >= 0 ? Py.True : Py.False;
    }

    private static String coerce(PyObject o) {
        if (o instanceof PyString)
            return o.toString();
        return null;
    }

    @Override
    public int hashCode() {
        return str___hash__();
    }

    @ExposedMethod(doc = BuiltinDocs.str___hash___doc)
    final int str___hash__() {
        return getString().hashCode();
    }

    /**
     * @return a byte array with one byte for each char in this object's
     *         underlying String. Each byte contains the low-order bits of its
     *         corresponding char.
     */
    public byte[] toBytes() {
        return StringUtil.toBytes(getString());
    }

    @Override
    public Object __tojava__(Class<?> c) {
        if (c.isAssignableFrom(String.class)) {
            return getString();
        }

        if (c == Character.TYPE || c == Character.class)
            if (getString().length() == 1)
                return new Character(getString().charAt(0));

        if (c.isArray()) {
            if (c.getComponentType() == Byte.TYPE)
                return toBytes();
            if (c.getComponentType() == Character.TYPE)
                return getString().toCharArray();
        }

        if (c.isInstance(this))
            return this;

        return Py.NoConversion;
    }

    protected PyObject pyget(int i) {
        return Py.newString(getString().charAt(i));
    }

    protected PyObject getslice(int start, int stop, int step) {
        if (step > 0 && stop < start)
            stop = start;
        if (step == 1)
            return fromSubstring(start, stop);
        else {
            int n = sliceLength(start, stop, step);
            char new_chars[] = new char[n];
            int j = 0;
            for (int i=start; j<n; i+=step)
                new_chars[j++] = getString().charAt(i);

            return createInstance(new String(new_chars), true);
        }
    }

    public PyString createInstance(String str) {
        return new PyString(str);
    }

    protected PyString createInstance(String str, boolean isBasic) {
        // ignore isBasic, doesn't apply to PyString, just PyUnicode
        return new PyString(str);
    } 
    
    @Override
    public boolean __contains__(PyObject o) {
        return str___contains__(o);
    }

    @ExposedMethod(doc = BuiltinDocs.str___contains___doc)
    final boolean str___contains__(PyObject o) {
        if (!(o instanceof PyString))
            throw Py.TypeError("'in <string>' requires string as left operand");
        PyString other = (PyString) o;
        return getString().indexOf(other.getString()) >= 0;
    }

    protected PyObject repeat(int count) {
        if(count < 0) {
            count = 0;
        }
        int s = getString().length();
        if((long)s * count > Integer.MAX_VALUE) {
            // Since Strings store their data in an array, we can't make one
            // longer than Integer.MAX_VALUE. Without this check we get
            // NegativeArraySize exceptions when we create the array on the
            // line with a wrapped int.
            throw Py.OverflowError("max str len is " + Integer.MAX_VALUE);
        }
        char new_chars[] = new char[s * count];
        for(int i = 0; i < count; i++) {
            getString().getChars(0, s, new_chars, i * s);
        }
        return createInstance(new String(new_chars));
    }

    @Override
    public PyObject __mul__(PyObject o) {
        return str___mul__(o);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.str___mul___doc)
    final PyObject str___mul__(PyObject o) {
        if (!o.isIndex()) {
            return null;
        }
        return repeat(o.asIndex(Py.OverflowError));
    }
    
    @Override
    public PyObject __rmul__(PyObject o) {
        return str___rmul__(o);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.str___rmul___doc)
    final PyObject str___rmul__(PyObject o) {
        if (!o.isIndex()) {
            return null;
        }
        return repeat(o.asIndex(Py.OverflowError));
    }

    @Override
    public PyObject __add__(PyObject other) {
        return str___add__(other);
    }
    
    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.str___add___doc)
    final PyObject str___add__(PyObject other) {
        if (other instanceof PyUnicode) {
            return decode().__add__(other);
        }
        if (other instanceof PyString) {
            PyString otherStr = (PyString)other;
            return new PyString(getString().concat(otherStr.getString()));
        }
        return null;
    }

    @ExposedMethod(doc = BuiltinDocs.str___getnewargs___doc)
    final PyTuple str___getnewargs__() {
        return new PyTuple(new PyString(this.getString()));
    }

    @Override
    public PyTuple __getnewargs__() {
        return str___getnewargs__();
    }

    @Override
    public PyObject __mod__(PyObject other) {
        return str___mod__(other);
    }
    
    @ExposedMethod(doc = BuiltinDocs.str___mod___doc)
    public PyObject str___mod__(PyObject other){
        StringFormatter fmt = new StringFormatter(getString(), false);
        return fmt.format(other);
    }

    @Override
    public PyObject __int__() {
        try
        {
            return Py.newInteger(atoi(10));
        } catch (PyException e) {
            if (e.match(Py.OverflowError)) {
                return atol(10);
            }
            throw e;
        }
    }

    @Override
    public PyObject __long__() {
        return atol(10);
    }

    @Override
    public PyFloat __float__() {
        return new PyFloat(atof());
    }

    @Override
    public PyObject __pos__() {
      throw Py.TypeError("bad operand type for unary +");
    }

    @Override
    public PyObject __neg__() {
      throw Py.TypeError("bad operand type for unary -");
    }

    @Override
    public PyObject __invert__() {
      throw Py.TypeError("bad operand type for unary ~");
    }

    @SuppressWarnings("fallthrough")
    @Override
    public PyComplex __complex__() {
        boolean got_re = false;
        boolean got_im = false;
        boolean done = false;
        boolean sw_error = false;

        int s = 0;
        int n = getString().length();
        while (s < n && Character.isSpaceChar(getString().charAt(s)))
            s++;

        if (s == n) {
            throw Py.ValueError("empty string for complex()");
        }

        double z = -1.0;
        double x = 0.0;
        double y = 0.0;

        int sign = 1;
        do {
            char c = getString().charAt(s);
            switch (c) {
            case '-':
                sign = -1;
                /* Fallthrough */
            case '+':
                if (done || s+1 == n) {
                    sw_error = true;
                    break;
                }
                //  a character is guaranteed, but it better be a digit
                //  or J or j
                c = getString().charAt(++s);  //  eat the sign character
                                         //  and check the next
                if  (!Character.isDigit(c) && c!='J' && c!='j')
                    sw_error = true;
                break;

            case 'J':
            case 'j':
                if (got_im || done) {
                    sw_error = true;
                    break;
                }
                if  (z < 0.0) {
                    y = sign;
                } else {
                    y = sign * z;
                }
                got_im = true;
                done = got_re;
                sign = 1;
                s++; // eat the J or j
                break;

            case ' ':
                while (s < n && Character.isSpaceChar(getString().charAt(s)))
                    s++;
                if (s != n)
                    sw_error = true;
                break;

            default:
                boolean digit_or_dot = (c == '.' || Character.isDigit(c));
                if (!digit_or_dot) {
                    sw_error = true;
                    break;
                }
                int end = endDouble(getString(),s);
                z = Double.valueOf(getString().substring(s, end)).doubleValue();
                if (z == Double.POSITIVE_INFINITY) {
                    throw Py.ValueError(String.format("float() out of range: %.150s", getString()));
                }

                s=end;
                if (s < n) {
                    c = getString().charAt(s);
                    if  (c == 'J' || c == 'j') {
                        break;
                    }
                }
                if  (got_re) {
                   sw_error = true;
                   break;
                }

                /* accept a real part */
                x = sign * z;
                got_re = true;
                done = got_im;
                z = -1.0;
                sign = 1;
                break;

             }  /* end of switch  */

        } while (s < n && !sw_error);

        if (sw_error) {
            throw Py.ValueError("malformed string for complex() " +
                                getString().substring(s));
        }

        return new PyComplex(x,y);
    }

    private int endDouble(String string, int s) {
        int n = string.length();
        while (s < n) {
            char c = string.charAt(s++);
            if (Character.isDigit(c))
                continue;
            if (c == '.')
                continue;
            if (c == 'e' || c == 'E') {
                if (s < n) {
                    c = string.charAt(s);
                    if (c == '+' || c == '-')
                        s++;
                    continue;
                }
            }
            return s-1;
        }
        return s;
    }

    // Add in methods from string module
    public String lower() {
        return str_lower();
    }
    
    @ExposedMethod(doc = BuiltinDocs.str_lower_doc)
    final String str_lower() {
        return getString().toLowerCase();
    }

    public String upper() {
        return str_upper();
    }

    @ExposedMethod(doc = BuiltinDocs.str_upper_doc)
    final String str_upper() {
        return getString().toUpperCase();
    }

    public String title() {
        return str_title();
    }

    @ExposedMethod(doc = BuiltinDocs.str_title_doc)
    final String str_title() {
        char[] chars = getString().toCharArray();
        int n = chars.length;

        boolean previous_is_cased = false;
        for (int i = 0; i < n; i++) {
            char ch = chars[i];
            if (previous_is_cased)
                chars[i] = Character.toLowerCase(ch);
            else
                chars[i] = Character.toTitleCase(ch);

            if (Character.isLowerCase(ch) ||
                   Character.isUpperCase(ch) ||
                   Character.isTitleCase(ch))
                previous_is_cased = true;
            else
                previous_is_cased = false;
        }
        return new String(chars);
    }

    public String swapcase() {
        return str_swapcase();
    }

    @ExposedMethod(doc = BuiltinDocs.str_swapcase_doc)
    final String str_swapcase() {
        char[] chars = getString().toCharArray();
        int n=chars.length;
        for (int i=0; i<n; i++) {
            char c = chars[i];
            if (Character.isUpperCase(c)) {
                chars[i] = Character.toLowerCase(c);
            }
            else if (Character.isLowerCase(c)) {
                chars[i] = Character.toUpperCase(c);
            }
        }
        return new String(chars);
    }

    public String strip() {
        return str_strip(null);
    }

    public String strip(String sep) {
        return str_strip(sep);
    }

    @ExposedMethod(defaults = "null", doc = BuiltinDocs.str_strip_doc)
    final String str_strip(String sep) {
        char[] chars = getString().toCharArray();
        int n=chars.length;
        int start=0;
        if (sep == null)
            while (start < n && Character.isWhitespace(chars[start]))
                start++;
        else
            while (start < n && sep.indexOf(chars[start]) >= 0)
                start++;

        int end=n-1;
        if (sep == null)
            while (end >= 0 && Character.isWhitespace(chars[end]))
                end--;
        else
            while (end >= 0 && sep.indexOf(chars[end]) >= 0)
                end--;

        if (end >= start) {
            return (end < n-1 || start > 0)
                ? getString().substring(start, end+1) : getString();
        } else {
            return "";
        }
    }

    public String lstrip() {
        return str_lstrip(null);
    }
    
    public String lstrip(String sep) {
        return str_lstrip(sep);
    }

    @ExposedMethod(defaults = "null", doc = BuiltinDocs.str_lstrip_doc)
    final String str_lstrip(String sep) {
        char[] chars = getString().toCharArray();
        int n=chars.length;
        int start=0;
        if (sep == null)
            while (start < n && Character.isWhitespace(chars[start]))
                start++;
        else
            while (start < n && sep.indexOf(chars[start]) >= 0)
                start++;

        return (start > 0) ? getString().substring(start, n) : getString();
    }

    public String rstrip(String sep) {
        return str_rstrip(sep);
    }
    
    @ExposedMethod(defaults = "null", doc = BuiltinDocs.str_rstrip_doc)
    final String str_rstrip(String sep) {
        char[] chars = getString().toCharArray();
        int n=chars.length;
        int end=n-1;
        if (sep == null)
            while (end >= 0 && Character.isWhitespace(chars[end]))
                end--;
        else
            while (end >= 0 && sep.indexOf(chars[end]) >= 0)
                end--;

        return (end < n-1) ? getString().substring(0, end+1) : getString();
    }


    public PyList split() {
        return str_split(null, -1);
    }

    public PyList split(String sep) {
        return str_split(sep, -1);
    }

    public PyList split(String sep, int maxsplit) {
        return str_split(sep, maxsplit);
    }

    @ExposedMethod(defaults = {"null", "-1"}, doc = BuiltinDocs.str_split_doc)
    final PyList str_split(String sep, int maxsplit) {
        if (sep != null) {
            if (sep.length() == 0) {
                throw Py.ValueError("empty separator");
            }
            return splitfields(sep, maxsplit);
        }

        PyList list = new PyList();

        char[] chars = getString().toCharArray();
        int n=chars.length;

        if (maxsplit < 0)
            maxsplit = n;

        int splits=0;
        int index=0;
        while (index < n && splits < maxsplit) {
            while (index < n && Character.isWhitespace(chars[index]))
                index++;
            if (index == n)
                break;
            int start = index;

            while (index < n && !Character.isWhitespace(chars[index]))
                index++;
            list.append(fromSubstring(start, index));
            splits++;
        }
        while (index < n && Character.isWhitespace(chars[index]))
            index++;
        if (index < n) {
            list.append(fromSubstring(index, n));
        }
        return list;
    }

    public PyList rsplit() {
        return str_rsplit(null, -1);
    }

    public PyList rsplit(String sep) {
        return str_rsplit(sep, -1);
    }

    public PyList rsplit(String sep, int maxsplit) {
        return str_rsplit(sep, maxsplit);
    }

    @ExposedMethod(defaults = {"null", "-1"}, doc = BuiltinDocs.str_rsplit_doc)
    final PyList str_rsplit(String sep, int maxsplit) {
        if (sep != null) {
            if (sep.length() == 0) {
                throw Py.ValueError("empty separator");
            }
            PyList list = rsplitfields(sep, maxsplit);
            list.reverse();
            return list;
        }

        PyList list = new PyList();
        char[] chars = getString().toCharArray();

        if (maxsplit < 0) {
            maxsplit = chars.length;
        }

        int splits = 0;
        int i = chars.length - 1;

        while (i > -1 && Character.isWhitespace(chars[i])) {
            i--;
        }
        if (i == -1) {
            return list;
        }

        while (splits < maxsplit) {
            while (i > -1 && Character.isWhitespace(chars[i])) {
                i--;
            }
            if (i == -1) {
                break;
            }

            int nextWsChar = i;
            while (nextWsChar > -1 && !Character.isWhitespace(chars[nextWsChar])) {
                nextWsChar--;
            }
            if (nextWsChar == -1) {
                break;
            }

            splits++;
            list.add(fromSubstring(nextWsChar + 1, i + 1));
            i = nextWsChar;
        }
        while (i > -1 && Character.isWhitespace(chars[i])) {
            i--;
        }
        if (i > -1) {
            list.add(fromSubstring(0,i+1));
        }
        list.reverse();
        return list;
    }

    public PyTuple partition(PyObject sepObj) {
        return str_partition(sepObj);
    }

    @ExposedMethod(doc = BuiltinDocs.str_partition_doc)
    final PyTuple str_partition(PyObject sepObj) {
        String sep;

        if (sepObj instanceof PyUnicode) {
            return unicodePartition(sepObj);
        } else if (sepObj instanceof PyString) {
            sep = ((PyString) sepObj).getString();
        } else {
            throw Py.TypeError("expected a character buffer object");
        }

        if (sep.length() == 0) {
            throw Py.ValueError("empty separator");
        }

        int index = getString().indexOf(sep);
        if (index != -1) {
            return new PyTuple(fromSubstring(0, index), sepObj,
                               fromSubstring(index + sep.length(), getString().length()));
        } else {
            return new PyTuple(this, Py.EmptyString, Py.EmptyString);
        }
    }

    final PyTuple unicodePartition(PyObject sepObj) {
        PyUnicode strObj = __unicode__();
        String str = strObj.getString();

        // Will throw a TypeError if not a basestring
        String sep = sepObj.asString();
        sepObj = sepObj.__unicode__();

        if (sep.length() == 0) {
            throw Py.ValueError("empty separator");
        }

        int index = str.indexOf(sep);
        if (index != -1) {
            return new PyTuple(strObj.fromSubstring(0, index), sepObj,
                               strObj.fromSubstring(index + sep.length(), str.length()));
        } else {
            PyUnicode emptyUnicode = Py.newUnicode("");
            return new PyTuple(this, emptyUnicode, emptyUnicode);
        }
    }

    public PyTuple rpartition(PyObject sepObj) {
        return str_rpartition(sepObj);
    }

    @ExposedMethod(doc = BuiltinDocs.str_rpartition_doc)
    final PyTuple str_rpartition(PyObject sepObj) {
        String sep;

        if (sepObj instanceof PyUnicode) {
            return unicodeRpartition(sepObj);
        } else if (sepObj instanceof PyString) {
            sep = ((PyString) sepObj).getString();
        } else {
            throw Py.TypeError("expected a character buffer object");
        }

        if (sep.length() == 0) {
            throw Py.ValueError("empty separator");
        }

        int index = getString().lastIndexOf(sep);
        if (index != -1) {
            return new PyTuple(fromSubstring(0, index), sepObj,
                               fromSubstring(index + sep.length(), getString().length()));
        } else {
            return new PyTuple(Py.EmptyString, Py.EmptyString, this);
        }
    }

    final PyTuple unicodeRpartition(PyObject sepObj) {
        PyUnicode strObj = __unicode__();
        String str = strObj.getString();

        // Will throw a TypeError if not a basestring
        String sep = sepObj.asString();
        sepObj = sepObj.__unicode__();

        if (sep.length() == 0) {
            throw Py.ValueError("empty separator");
        }

        int index = str.lastIndexOf(sep);
        if (index != -1) {
            return new PyTuple(strObj.fromSubstring(0, index), sepObj,
                               strObj.fromSubstring(index + sep.length(), str.length()));
        } else {
            PyUnicode emptyUnicode = Py.newUnicode("");
            return new PyTuple(emptyUnicode, emptyUnicode, this);
        }
    }

    private PyList splitfields(String sep, int maxsplit) {
        PyList list = new PyList();

        int length = getString().length();
        if (maxsplit < 0)
            maxsplit = length + 1;

        int lastbreak = 0;
        int splits = 0;
        int sepLength = sep.length();
        int index;
        if((sep.length() == 0) && (maxsplit != 0)) {
            index = getString().indexOf(sep, lastbreak);
            list.append(fromSubstring(lastbreak, index));
            splits++;
        }
        while (splits < maxsplit) {
            index = getString().indexOf(sep, lastbreak);
            if (index == -1)
                break;
            if(sep.length() == 0)
                index++;
            splits += 1;
            list.append(fromSubstring(lastbreak, index));
            lastbreak = index + sepLength;
        }
        if (lastbreak <= length) {
            list.append(fromSubstring(lastbreak, length));
        }
        return list;
    }

    private PyList rsplitfields(String sep, int maxsplit) {
        PyList list = new PyList();

        int length = getString().length();
        if (maxsplit < 0) {
            maxsplit = length + 1;
        }

        int lastbreak = length;
        int splits = 0;
        int index = length;
        int sepLength = sep.length();

        while (index > 0 && splits < maxsplit) {
            int i = getString().lastIndexOf(sep, index - sepLength);
            if (i == index) {
                i -= sepLength;
            }
            if (i < 0) {
                break;
            }
            splits++;
            list.append(fromSubstring(i + sepLength, lastbreak));
            lastbreak = i;
            index = i;

        }
        list.append(fromSubstring(0, lastbreak));
        return list;
    }

    public PyList splitlines() {
        return str_splitlines(false);
    }

    public PyList splitlines(boolean keepends) {
        return str_splitlines(keepends);
    }

    @ExposedMethod(defaults = "false", doc = BuiltinDocs.str_splitlines_doc)
    final PyList str_splitlines(boolean keepends) {
        PyList list = new PyList();

        char[] chars = getString().toCharArray();
        int n=chars.length;

        int j = 0;
        for (int i = 0; i < n; ) {
            /* Find a line and append it */
            while (i < n && chars[i] != '\n' && chars[i] != '\r' &&
                    Character.getType(chars[i]) != Character.LINE_SEPARATOR)
                i++;

            /* Skip the line break reading CRLF as one line break */
            int eol = i;
            if (i < n) {
                if (chars[i] == '\r' && i + 1 < n && chars[i+1] == '\n')
                    i += 2;
                else
                    i++;
                if (keepends)
                    eol = i;
            }
            list.append(fromSubstring(j, eol));
            j = i;
        }
        if (j < n) {
            list.append(fromSubstring(j, n));
        }
        return list;
    }

    protected PyString fromSubstring(int begin, int end) {
        return createInstance(getString().substring(begin, end), true);
    }

    public int index(String sub) {
        return str_index(sub, null, null);
    }

    public int index(String sub, PyObject start) {
        return str_index(sub, start, null);
    }

    public int index(String sub, PyObject start, PyObject end) {
        return str_index(sub, start, end);
    }

    @ExposedMethod(defaults = {"null", "null"}, doc = BuiltinDocs.str_index_doc)
    final int str_index(String sub, PyObject start, PyObject end) {
        int index = str_find(sub, start, end);
        if (index == -1)
            throw Py.ValueError("substring not found in string.index");
        return index;
    }

    public int rindex(String sub) {
        return str_rindex(sub, null, null);
    }

    public int rindex(String sub, PyObject start) {
        return str_rindex(sub, start, null);
    }

    public int rindex(String sub, PyObject start, PyObject end) {
        return str_rindex(sub, start, end);
    }

    @ExposedMethod(defaults = {"null", "null"}, doc = BuiltinDocs.str_rindex_doc)
    final int str_rindex(String sub, PyObject start, PyObject end) {
        int index = str_rfind(sub, start, end);
        if(index == -1)
            throw Py.ValueError("substring not found in string.rindex");
        return index;
    }

    public int count(String sub) {
        return str_count(sub, null, null);
    }

    public int count(String sub, PyObject start) {
        return str_count(sub, start, null);
    }

    public int count(String sub, PyObject start, PyObject end) {
        return str_count(sub, start, end);
    }
    
    @ExposedMethod(defaults = {"null", "null"}, doc = BuiltinDocs.str_count_doc)
    final int str_count(String sub, PyObject start, PyObject end) {
        if (sub == null) {
            throw Py.TypeError("count() takes at least 1 argument (0 given)");
        }
        int[] indices = translateIndices(start, end);
        int n = sub.length();
        if(n == 0) {
            if (indices[2] > getString().length()) {
                return 0;
            }
            return indices[1] - indices[0] + 1;
        }
        int count = 0;
        while(true){
            int index = getString().indexOf(sub, indices[0]);
            indices[0] = index + n;
            if(indices[0] > indices[1] || index == -1) {
                break;
            }
            count++;
        }
        return count;
    }

    public int find(String sub) {
        return str_find(sub, null, null);
    }

    public int find(String sub, PyObject start) {
        return str_find(sub, start, null);
    }

    public int find(String sub, PyObject start, PyObject end) {
        return str_find(sub, start, end);
    }

    @ExposedMethod(defaults = {"null", "null"}, doc = BuiltinDocs.str_find_doc)
    final int str_find(String sub, PyObject start, PyObject end) {
        int[] indices = translateIndices(start, end);
        int index = getString().indexOf(sub, indices[0]);
        if (index < indices[2] || index > indices[1]) {
            return -1;
        }
        return index;
    }

    public int rfind(String sub) {
        return str_rfind(sub, null, null);
    }

    public int rfind(String sub, PyObject start) {
        return str_rfind(sub, start, null);
    }

    public int rfind(String sub, PyObject start, PyObject end) {
        return str_rfind(sub, start, end);
    }

    @ExposedMethod(defaults = {"null", "null"}, doc = BuiltinDocs.str_rfind_doc)
    final int str_rfind(String sub, PyObject start, PyObject end) {
        int[] indices = translateIndices(start, end);
        int index = getString().lastIndexOf(sub, indices[1] - sub.length());
        if (index < indices[2]) {
            return -1;
        }
        return index;
    }

    public double atof() {
        StringBuilder s = null;
        int n = getString().length();
        for (int i = 0; i < n; i++) {
            char ch = getString().charAt(i);
            if (ch == '\u0000') {
                throw Py.ValueError("null byte in argument for float()");
            }
            if (Character.isDigit(ch)) {
                if (s == null)
                    s = new StringBuilder(getString());
                int val = Character.digit(ch, 10);
                s.setCharAt(i, Character.forDigit(val, 10));
            }
        }
        String sval = getString();
        if (s != null)
            sval = s.toString();
        try {
            // Double.valueOf allows format specifier ("d" or "f") at the end
            String lowSval = sval.toLowerCase();
            if (lowSval.equals("nan")) return Double.NaN;
            else if (lowSval.equals("inf")) return Double.POSITIVE_INFINITY;
            else if (lowSval.equals("-inf")) return Double.NEGATIVE_INFINITY;
            
            if (lowSval.endsWith("d") || lowSval.endsWith("f")) {
                throw new NumberFormatException("format specifiers not allowed");
            }
            return Double.valueOf(sval).doubleValue();
        }
        catch (NumberFormatException exc) {
            throw Py.ValueError("invalid literal for __float__: "+getString());
        }
    }

    public int atoi() {
        return atoi(10);
    }

    public int atoi(int base) {
        if ((base != 0 && base < 2) || (base > 36)) {
            throw Py.ValueError("invalid base for atoi()");
        }

        int b = 0;
        int e = getString().length();

        while (b < e && Character.isWhitespace(getString().charAt(b)))
            b++;

        while (e > b && Character.isWhitespace(getString().charAt(e-1)))
            e--;

        char sign = 0;
        if (b < e) {
            sign = getString().charAt(b);
            if (sign == '-' || sign == '+') {
                b++;
                while (b < e && Character.isWhitespace(getString().charAt(b))) b++;
            }

            if (base == 16) {
                if (getString().charAt(b) == '0') {
                    if (b < e-1 &&
                           Character.toUpperCase(getString().charAt(b+1)) == 'X') {
                        b += 2;
                    }
                }
            } else if (base == 0) {
                if (getString().charAt(b) == '0') {
                    if (b < e-1 && Character.toUpperCase(getString().charAt(b+1)) == 'X') {
                        base = 16;
                        b += 2;
                    } else if (b < e-1 && Character.toUpperCase(getString().charAt(b+1)) == 'O') {
                        base = 8;
                        b += 2;
                    } else if (b < e-1 && Character.toUpperCase(getString().charAt(b+1)) == 'B') {
                        base = 2;
                        b += 2;
                    } else {
                        base = 8;
                    }
                }
            } else if (base == 8) {
                if (b < e-1 && Character.toUpperCase(getString().charAt(b+1)) == 'O') {
                    b += 2;
                }
            } else if (base == 2) {
                if (b < e-1 &&
                       Character.toUpperCase(getString().charAt(b+1)) == 'B') {
                    b += 2;
                }
            }
        }

        if (base == 0)
            base = 10;

        String s = getString();
        if (b > 0 || e < getString().length())
            s = getString().substring(b, e);

        try {
            BigInteger bi;
            if (sign == '-') {
                bi = new BigInteger("-" + s, base);
            } else
                bi = new BigInteger(s, base);
            if (bi.compareTo(PyInteger.MAX_INT) > 0 || bi.compareTo(PyInteger.MIN_INT) < 0) {
                throw Py.OverflowError("long int too large to convert to int");
            }
            return bi.intValue();
        } catch (NumberFormatException exc) {
            throw Py.ValueError("invalid literal for int() with base " + base + ": " + getString());
        } catch (StringIndexOutOfBoundsException exc) {
            throw Py.ValueError("invalid literal for int() with base " + base + ": " + getString());
        }
    }

    public PyLong atol() {
        return atol(10);
    }

    public PyLong atol(int base) {
        String str = getString();
        int b = 0;
        int e = str.length();

        while (b < e && Character.isWhitespace(str.charAt(b)))
            b++;

        while (e > b && Character.isWhitespace(str.charAt(e-1)))
            e--;


        char sign = 0;
        if (b < e) {
            sign = getString().charAt(b);
            if (sign == '-' || sign == '+') {
                b++;
                while (b < e && Character.isWhitespace(str.charAt(b))) b++;
            }


            if (base == 0 || base == 16) {
                if (getString().charAt(b) == '0') {
                    if (b < e-1 &&
                           Character.toUpperCase(getString().charAt(b+1)) == 'X') {
                        base = 16;
                        b += 2;
                    } else {
                        if (base == 0)
                            base = 8;
                    }
                }
            }
        }
        if (base == 0)
            base = 10;

        if (base < 2 || base > 36)
            throw Py.ValueError("invalid base for long literal:" + base);

        // if the base >= 22, then an 'l' or 'L' is a digit!
        if (base < 22 && e > b && (str.charAt(e-1) == 'L' || str.charAt(e-1) == 'l'))
            e--;
        
        if (b > 0 || e < str.length())
            str = str.substring(b, e);
        
        try {
            java.math.BigInteger bi = null;
            if (sign == '-')
                bi = new java.math.BigInteger("-" + str, base);
            else
                bi = new java.math.BigInteger(str, base);
            return new PyLong(bi);
        } catch (NumberFormatException exc) {
            if (this instanceof PyUnicode) {
                // TODO: here's a basic issue: do we use the BigInteger constructor
                // above, or add an equivalent to CPython's PyUnicode_EncodeDecimal;
                // we should note that the current error string does not quite match
                // CPython regardless of the codec, that's going to require some more work
                throw Py.UnicodeEncodeError("decimal", "codec can't encode character",
                        0,0, "invalid decimal Unicode string");
            }
            else {
            throw Py.ValueError("invalid literal for long() with base " + base + ": " + getString());
            }
        } catch (StringIndexOutOfBoundsException exc) {
            throw Py.ValueError("invalid literal for long() with base " + base + ": " + getString());
        }
    }

    private static String padding(int n, char pad) {
        char[] chars = new char[n];
        for (int i=0; i<n; i++)
            chars[i] = pad;
        return new String(chars);
    }

    private static char parse_fillchar(String function, String fillchar) {
        if (fillchar == null) { return ' '; }
        if (fillchar.length() != 1) {
            throw Py.TypeError(function + "() argument 2 must be char, not str");
        }
        return fillchar.charAt(0);
    }
    
    public String ljust(int width) {
        return str_ljust(width, null);
    }

    public String ljust(int width, String padding) {
        return str_ljust(width, padding);
    }
    
    @ExposedMethod(defaults="null", doc = BuiltinDocs.str_ljust_doc)
    final String str_ljust(int width, String fillchar) {
        char pad = parse_fillchar("ljust", fillchar);
        int n = width-getString().length();
        if (n <= 0)
            return getString();
        return getString()+padding(n, pad);
    }

    public String rjust(int width) {
        return str_rjust(width, null);
    }

    @ExposedMethod(defaults="null", doc = BuiltinDocs.str_rjust_doc)
    final String str_rjust(int width, String fillchar) {
        char pad = parse_fillchar("rjust", fillchar);
        int n = width-getString().length();
        if (n <= 0)
            return getString();
        return padding(n, pad)+getString();
    }

    public String center(int width) {
        return str_center(width, null);
    }

    @ExposedMethod(defaults="null", doc = BuiltinDocs.str_center_doc)
    final String str_center(int width, String fillchar) {
        char pad = parse_fillchar("center", fillchar);
        int n = width-getString().length();
        if (n <= 0)
            return getString();
        int half = n/2;
        if (n%2 > 0 &&  width%2 > 0)
            half += 1;
        
        return padding(half, pad)+getString()+padding(n-half, pad);
    }

    public String zfill(int width) {
        return str_zfill(width);
    }

    @ExposedMethod(doc = BuiltinDocs.str_zfill_doc)
    final String str_zfill(int width) {
        String s = getString();
        int n = s.length();
        if (n >= width)
            return s;
        char[] chars = new char[width];
        int nzeros = width-n;
        int i=0;
        int sStart=0;
        if (n > 0) {
            char start = s.charAt(0);
            if (start == '+' || start == '-') {
                chars[0] = start;
                i += 1;
                nzeros++;
                sStart=1;
            }
        }
        for(;i<nzeros; i++) {
            chars[i] = '0';
        }
        s.getChars(sStart, s.length(), chars, i);
        return new String(chars);
    }

    public String expandtabs() {
        return str_expandtabs(8);
    }

    public String expandtabs(int tabsize) {
        return str_expandtabs(tabsize);
    }

    @ExposedMethod(defaults = "8", doc = BuiltinDocs.str_expandtabs_doc)
    final String str_expandtabs(int tabsize) {
        String s = getString();
        StringBuilder buf = new StringBuilder((int)(s.length()*1.5));
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
            if (c == '\n' || c == '\r') {
                position = -1;
            }
            buf.append(c);
            position++;
        }
        return buf.toString();
    }

    public String capitalize() {
        return str_capitalize();
    }

    @ExposedMethod(doc = BuiltinDocs.str_capitalize_doc)
    final String str_capitalize() {
        if (getString().length() == 0)
            return getString();
        String first = getString().substring(0,1).toUpperCase();
        return first.concat(getString().substring(1).toLowerCase());
    }

    @ExposedMethod(defaults = "null", doc = BuiltinDocs.str_replace_doc)
    final PyString str_replace(PyObject oldPiece, PyObject newPiece, PyObject maxsplit) {
        if(!(oldPiece instanceof PyString) || !(newPiece instanceof PyString)) {
            throw Py.TypeError("str or unicode required for replace");
        }

        return replace((PyString)oldPiece, (PyString)newPiece, maxsplit == null ? -1 : maxsplit.asInt());
    }
    
    protected PyString replace(PyString oldPiece, PyString newPiece, int maxsplit) {
        int len = getString().length();
        int old_len = oldPiece.getString().length();
        if (len == 0) {
            if (maxsplit == -1 && old_len == 0) {
                return createInstance(newPiece.getString(), true);
            }
            return createInstance(getString(), true);
        }
        
        if (old_len == 0 && newPiece.getString().length() != 0 && maxsplit !=0) {
            // old="" and new != "", interleave new piece with each char in original, taking in effect maxsplit
            StringBuilder buffer = new StringBuilder();
            int i = 0;
            buffer.append(newPiece.getString());
            for (; i < len && (i < maxsplit-1 || maxsplit == -1); i++) {
                buffer.append(getString().charAt(i));
                buffer.append(newPiece.getString());
            }
            buffer.append(getString().substring(i));
            return createInstance(buffer.toString(), true);
        }
       
        if(maxsplit == -1) {
            if(old_len == 0) {
                maxsplit = len + 1;
            } else {
                maxsplit = len;
            }
        }
        
        return newPiece.join(splitfields(oldPiece.getString(), maxsplit));
    }

    public PyString join(PyObject seq) {
        return str_join(seq);
    }

    @ExposedMethod(doc = BuiltinDocs.str_join_doc)
    final PyString str_join(PyObject obj) {
        PySequence seq = fastSequence(obj, "");
        int seqLen = seq.__len__();
        if (seqLen == 0) {
            return Py.EmptyString;
        }

        PyObject item;
        if (seqLen == 1) {
            item = seq.pyget(0);
            if (item.getType() == PyString.TYPE || item.getType() == PyUnicode.TYPE) {
                return (PyString)item;
            }
        }

        // There are at least two things to join, or else we have a subclass of the
        // builtin types in the sequence. Do a pre-pass to figure out the total amount of
        // space we'll need, see whether any argument is absurd, and defer to the Unicode
        // join if appropriate
        int i = 0;
        long size = 0;
        int sepLen = getString().length();
        for (; i < seqLen; i++) {
            item = seq.pyget(i);
            if (!(item instanceof PyString)) {
                throw Py.TypeError(String.format("sequence item %d: expected string, %.80s found",
                                                 i, item.getType().fastGetName()));
            }
            if (item instanceof PyUnicode) {
                // Defer to Unicode join. CAUTION: There's no gurantee that the original
                // sequence can be iterated over again, so we must pass seq here
                return unicodeJoin(seq);
            }

            if (i != 0) {
                size += sepLen;
            }
            size += ((PyString) item).getString().length();
            if (size > Integer.MAX_VALUE) {
                throw Py.OverflowError("join() result is too long for a Python string");
            }
        }

        // Catenate everything
        StringBuilder buf = new StringBuilder((int)size);
        for (i = 0; i < seqLen; i++) {
            item = seq.pyget(i);
            if (i != 0) {
                buf.append(getString());
            }
            buf.append(((PyString) item).getString());
        }
        return new PyString(buf.toString());
    }

    final PyUnicode unicodeJoin(PyObject obj) {
        PySequence seq = fastSequence(obj, "");
        // A codec may be invoked to convert str objects to Unicode, and so it's possible
        // to call back into Python code during PyUnicode_FromObject(), and so it's
        // possible for a sick codec to change the size of fseq (if seq is a list).
        // Therefore we have to keep refetching the size -- can't assume seqlen is
        // invariant.
        int seqLen = seq.__len__();
        // If empty sequence, return u""
        if (seqLen == 0) {
            return new PyUnicode();
        }

        // If singleton sequence with an exact Unicode, return that
        PyObject item;
        if (seqLen == 1) {
            item = seq.pyget(0);
            if (item.getType() == PyUnicode.TYPE) {
                return (PyUnicode)item;
            }
        }

        String sep = null;
        if (seqLen > 1) {
            if (this instanceof PyUnicode) {
                sep = getString();
            } else {
                sep = ((PyUnicode) decode()).getString();
                // In case decode()'s codec mutated seq
                seqLen = seq.__len__();
            }
        }

        // At least two items to join, or one that isn't exact Unicode
        long size = 0;
        int sepLen = getString().length();
        StringBuilder buf = new StringBuilder();
        String itemString;
        for (int i = 0; i < seqLen; i++) {
            item = seq.pyget(i);
            // Convert item to Unicode
            if (!(item instanceof PyString)) {
                throw Py.TypeError(String.format("sequence item %d: expected string or Unicode,"
                                                 + " %.80s found",
                                                 i, item.getType().fastGetName()));
            }
            if (!(item instanceof PyUnicode)) {
                item = ((PyString)item).decode();
                // In case decode()'s codec mutated seq
                seqLen = seq.__len__();
            }
            itemString = ((PyUnicode) item).getString();

            if (i != 0) {
                size += sepLen;
                buf.append(sep);
            }
            size += itemString.length();
            if (size > Integer.MAX_VALUE) {
                throw Py.OverflowError("join() result is too long for a Python string");
            }
            buf.append(itemString);
        }
        return new PyUnicode(buf.toString());
    }

    public boolean startswith(PyObject prefix) {
        return str_startswith(prefix, null, null);
    }

    public boolean startswith(PyObject prefix, PyObject offset) {
        return str_startswith(prefix, offset, null);
    }

    public boolean startswith(PyObject prefix, PyObject start, PyObject end) {
        return str_startswith(prefix, start, end);
    }

    @ExposedMethod(defaults = {"null", "null"}, doc = BuiltinDocs.str_startswith_doc)
    final boolean str_startswith(PyObject prefix, PyObject start, PyObject end) {
        int[] indices = translateIndices(start, end);
        
        if (prefix instanceof PyString) {
            String strPrefix = ((PyString) prefix).getString();
            if (indices[1] - indices[0] < strPrefix.length())
                return false;
            
            return getString().startsWith(strPrefix, indices[0]);
        } else if (prefix instanceof PyTuple) {
            PyObject[] prefixes = ((PyTuple)prefix).getArray();
            
            for (int i = 0 ; i < prefixes.length ; i++) {
                if (!(prefixes[i] instanceof PyString))
                    throw Py.TypeError("expected a character buffer object");

                String strPrefix = ((PyString) prefixes[i]).getString();
                if (indices[1] - indices[0] < strPrefix.length())
                    continue;
                
                if (getString().startsWith(strPrefix, indices[0]))
                    return true;
            }
            return false;
        } else {
            throw Py.TypeError("expected a character buffer object or tuple");
        }
    }

    public boolean endswith(PyObject suffix) {
        return str_endswith(suffix, null, null);
    }

    public boolean endswith(PyObject suffix, PyObject start) {
        return str_endswith(suffix, start, null);
    }

    public boolean endswith(PyObject suffix, PyObject start, PyObject end) {
        return str_endswith(suffix, start, end);
    }

    @ExposedMethod(defaults = {"null", "null"}, doc = BuiltinDocs.str_endswith_doc)
    final boolean str_endswith(PyObject suffix, PyObject start, PyObject end) {
        int[] indices = translateIndices(start, end);

        String substr = getString().substring(indices[0], indices[1]);
        if (suffix instanceof PyString) {
            return substr.endsWith(((PyString) suffix).getString());
        } else if (suffix instanceof PyTuple) {
            PyObject[] suffixes = ((PyTuple)suffix).getArray();
            
            for (int i = 0 ; i < suffixes.length ; i++) {
                if (!(suffixes[i] instanceof PyString))
                    throw Py.TypeError("expected a character buffer object");

                if (substr.endsWith(((PyString) suffixes[i]).getString()))
                    return true;
            }
            return false;
        } else {
            throw Py.TypeError("expected a character buffer object or tuple");
        }
    } 

    /**
     * Turns the possibly negative Python slice start and end into valid indices
     * into this string.
     * 
     * @return a 3 element array of indices into this string describing a
     *         substring from [0] to [1]. [0] <= [1], [0] >= 0 and [1] <=
     *         string.length(). The third element contains the unadjusted 
     *         start value.
     */
    protected int[] translateIndices(PyObject start, PyObject end) {
        int iStart;
        int iStartAdjusted;
        int iEnd;

        if(end == null || end == Py.None) {
            iEnd = getString().length();
        } else {
            iEnd = end.asInt();
        }
        int n = getString().length();
        if(iEnd < 0) {
            iEnd = n + iEnd;
            if(iEnd < 0) {
                iEnd = 0;
            }
        } else if(iEnd > n) {
            iEnd = n;
        }
        if(start == null || start == Py.None) {
            iStart = 0;
        } else {
            iStart = start.asInt();
        }

        iStartAdjusted = iStart;
        if(iStartAdjusted < 0) {
            iStartAdjusted = n + iStartAdjusted;
            if(iStartAdjusted < 0) {
                iStartAdjusted = 0;
            }
        }
        if(iStartAdjusted > iEnd) {
            iStartAdjusted = iEnd;
        }
        return new int[] {iStartAdjusted, iEnd, iStart};
    }

    public String translate(String table) {
        return str_translate(table, null);
    }

    public String translate(String table, String deletechars) {
        return str_translate(table, deletechars);
    }

    @ExposedMethod(defaults = "null", doc = BuiltinDocs.str_translate_doc)
    final String str_translate(String table, String deletechars) {
        if (table.length() != 256)
            throw Py.ValueError(
                "translation table must be 256 characters long");

        StringBuilder buf = new StringBuilder(getString().length());
        for (int i=0; i < getString().length(); i++) {
            char c = getString().charAt(i);
            if (deletechars != null && deletechars.indexOf(c) >= 0)
                continue;
            try {
                buf.append(table.charAt(c));
            }
            catch (IndexOutOfBoundsException e) {
                throw Py.TypeError(
                    "translate() only works for 8-bit character strings");
            }
        }
        return buf.toString();
    }

    //XXX: is this needed?
    public String translate(PyObject table) {
        StringBuilder v = new StringBuilder(getString().length());
        for (int i=0; i < getString().length(); i++) {
            char ch = getString().charAt(i);

            PyObject w = Py.newInteger(ch);
            PyObject x = table.__finditem__(w);
            if (x == null) {
                /* No mapping found: default to 1-1 mapping */
                v.append(ch);
                continue;
            }

            /* Apply mapping */
            if (x instanceof PyInteger) {
                int value = ((PyInteger) x).getValue();
                v.append((char) value);
            } else if (x == Py.None) {
                ;
            } else if (x instanceof PyString) {
                if (x.__len__() != 1) {
                    /* 1-n mapping */
                    throw new PyException(Py.NotImplementedError,
                          "1-n mappings are currently not implemented");
                }
                v.append(x.toString());
            }
            else {
                /* wrong return value */
                throw Py.TypeError(
                     "character mapping must return integer, " +
                     "None or unicode");
            }
        }
        return v.toString();
    }

    public boolean islower() {
        return str_islower();
    }

    @ExposedMethod(doc = BuiltinDocs.str_islower_doc)
    final boolean str_islower() {
        int n = getString().length();

        /* Shortcut for single character strings */
        if (n == 1)
            return Character.isLowerCase(getString().charAt(0));

        boolean cased = false;
        for (int i = 0; i < n; i++) {
            char ch = getString().charAt(i);

            if (Character.isUpperCase(ch) || Character.isTitleCase(ch))
                return false;
            else if (!cased && Character.isLowerCase(ch))
                cased = true;
        }
        return cased;
    }

    public boolean isupper() {
        return str_isupper();
    }

    @ExposedMethod(doc = BuiltinDocs.str_isupper_doc)
    final boolean str_isupper() {
        int n = getString().length();

        /* Shortcut for single character strings */
        if (n == 1)
            return Character.isUpperCase(getString().charAt(0));

        boolean cased = false;
        for (int i = 0; i < n; i++) {
            char ch = getString().charAt(i);

            if (Character.isLowerCase(ch) || Character.isTitleCase(ch))
                return false;
            else if (!cased && Character.isUpperCase(ch))
                cased = true;
        }
        return cased;
    }

    public boolean isalpha() {
        return str_isalpha();
    }

    @ExposedMethod(doc = BuiltinDocs.str_isalpha_doc)
    final boolean str_isalpha() {
        int n = getString().length();

        /* Shortcut for single character strings */
        if (n == 1)
            return Character.isLetter(getString().charAt(0));

        if (n == 0)
            return false;

        for (int i = 0; i < n; i++) {
            char ch = getString().charAt(i);

            if (!Character.isLetter(ch))
                return false;
        }
        return true;
    }

    public boolean isalnum() {
        return str_isalnum();
    }

    @ExposedMethod(doc = BuiltinDocs.str_isalnum_doc)
    final boolean str_isalnum() {
        int n = getString().length();

        /* Shortcut for single character strings */
        if (n == 1)
            return _isalnum(getString().charAt(0));

        if (n == 0)
            return false;

        for (int i = 0; i < n; i++) {
            char ch = getString().charAt(i);

            if (!_isalnum(ch))
                return false;
        }
        return true;
    }

    private boolean _isalnum(char ch) {
        // This can ever be entirely compatible with CPython. In CPython
        // The type is not used, the numeric property is determined from
        // the presense of digit, decimal or numeric fields. These fields
        // are not available in exactly the same way in java.
        return Character.isLetterOrDigit(ch) ||
               Character.getType(ch) == Character.LETTER_NUMBER;
    }

    public boolean isdecimal() {
        return str_isdecimal();
    }

    @ExposedMethod(doc = BuiltinDocs.unicode_isdecimal_doc)
    final boolean str_isdecimal() {
        int n = getString().length();

        /* Shortcut for single character strings */
        if (n == 1) {
            char ch = getString().charAt(0);
            return _isdecimal(ch);
        }

        if (n == 0)
            return false;

        for (int i = 0; i < n; i++) {
            char ch = getString().charAt(i);

            if (!_isdecimal(ch))
                return false;
        }
        return true;
    }

    private boolean _isdecimal(char ch) {
        // See the comment in _isalnum. Here it is even worse.
        return Character.getType(ch) == Character.DECIMAL_DIGIT_NUMBER;
    }

    public boolean isdigit() {
        return str_isdigit();
    }

    @ExposedMethod(doc = BuiltinDocs.str_isdigit_doc)
    final boolean str_isdigit() {
        int n = getString().length();

        /* Shortcut for single character strings */
        if (n == 1)
            return Character.isDigit(getString().charAt(0));

        if (n == 0)
            return false;

        for (int i = 0; i < n; i++) {
            char ch = getString().charAt(i);

            if (!Character.isDigit(ch))
                return false;
        }
        return true;
    }

    public boolean isnumeric() {
        return str_isnumeric();
    }

    @ExposedMethod(doc = BuiltinDocs.unicode_isnumeric_doc)
    final boolean str_isnumeric() {
        int n = getString().length();

        /* Shortcut for single character strings */
        if (n == 1)
            return _isnumeric(getString().charAt(0));

        if (n == 0)
            return false;

        for (int i = 0; i < n; i++) {
            char ch = getString().charAt(i);
            if (!_isnumeric(ch))
                return false;
        }
        return true;
    }

    private boolean _isnumeric(char ch) {
        int type = Character.getType(ch);
        return type == Character.DECIMAL_DIGIT_NUMBER ||
               type == Character.LETTER_NUMBER ||
               type == Character.OTHER_NUMBER;
    }

    public boolean istitle() {
        return str_istitle();
    }

    @ExposedMethod(doc = BuiltinDocs.str_istitle_doc)
    final boolean str_istitle() {
        int n = getString().length();

        /* Shortcut for single character strings */
        if (n == 1)
            return Character.isTitleCase(getString().charAt(0)) ||
                   Character.isUpperCase(getString().charAt(0));

        boolean cased = false;
        boolean previous_is_cased = false;
        for (int i = 0; i < n; i++) {
            char ch = getString().charAt(i);

            if (Character.isUpperCase(ch) || Character.isTitleCase(ch)) {
                if (previous_is_cased)
                    return false;
                previous_is_cased = true;
                cased = true;
            }
            else if (Character.isLowerCase(ch)) {
                if (!previous_is_cased)
                    return false;
                previous_is_cased = true;
                cased = true;
            }
            else
                previous_is_cased = false;
        }
        return cased;
    }

    public boolean isspace() {
        return str_isspace();
    }

    @ExposedMethod(doc = BuiltinDocs.str_isspace_doc)
    final boolean str_isspace() {
        int n = getString().length();

        /* Shortcut for single character strings */
        if (n == 1)
            return Character.isWhitespace(getString().charAt(0));

        if (n == 0)
            return false;

        for (int i = 0; i < n; i++) {
            char ch = getString().charAt(i);

            if (!Character.isWhitespace(ch))
                return false;
        }
        return true;
    }

    public boolean isunicode() {
        return str_isunicode();
    }

    @ExposedMethod(doc = "isunicode is deprecated.")
    final boolean str_isunicode() {
        Py.warning(Py.DeprecationWarning, "isunicode is deprecated.");
        int n = getString().length();
        for (int i = 0; i < n; i++) {
            char ch = getString().charAt(i);
            if (ch > 255)
                return true;
        }
        return false;
    }

    public String encode() {
        return encode(null, null);
    }

    public String encode(String encoding) {
        return encode(encoding, null);
    }

    public String encode(String encoding, String errors) {
        return codecs.encode(this, encoding, errors);
    }

    @ExposedMethod(doc = BuiltinDocs.str_encode_doc)
    final String str_encode(PyObject[] args, String[] keywords) {
        ArgParser ap = new ArgParser("encode", args, keywords, "encoding", "errors");
        String encoding = ap.getString(0, null);
        String errors = ap.getString(1, null);
        return encode(encoding, errors);
    }
    
    public PyObject decode() {
        return decode(null, null);
    }

    public PyObject decode(String encoding) {
        return decode(encoding, null);
    }

    public PyObject decode(String encoding, String errors) {
        return codecs.decode(this, encoding, errors);
    }

    @ExposedMethod(doc = BuiltinDocs.str_decode_doc)
    final PyObject str_decode(PyObject[] args, String[] keywords) {
        ArgParser ap = new ArgParser("decode", args, keywords, "encoding", "errors");
        String encoding = ap.getString(0, null);
        String errors = ap.getString(1, null);
        return decode(encoding, errors);
    }

    @ExposedMethod(doc = BuiltinDocs.str__formatter_parser_doc)
    final PyObject str__formatter_parser() {
        return new MarkupIterator(getString());
    }

    @ExposedMethod(doc = BuiltinDocs.str__formatter_field_name_split_doc)
    final PyObject str__formatter_field_name_split() {
        FieldNameIterator iterator = new FieldNameIterator(getString());
        Object headObj = iterator.head();
        PyObject head = headObj instanceof Integer
                ? new PyInteger((Integer) headObj)
                : new PyString((String) headObj);
        return new PyTuple(head, iterator);
    }

    @ExposedMethod(doc = BuiltinDocs.str_format_doc)
    final PyObject str_format(PyObject[] args, String[] keywords) {
        try {
            return new PyString(buildFormattedString(getString(), args, keywords, null));
        } catch (IllegalArgumentException e) {
            throw Py.ValueError(e.getMessage());
        }
    }

    protected String buildFormattedString(String value, PyObject[] args, String[] keywords, MarkupIterator enclosingIterator) {
        StringBuilder result = new StringBuilder();
        MarkupIterator it = new MarkupIterator(value, enclosingIterator);
        while (true) {
            MarkupIterator.Chunk chunk = it.nextChunk();
            if (chunk == null) {
                break;
            }
            result.append(chunk.literalText);
            if (chunk.fieldName.length() > 0) {
                PyObject fieldObj = getFieldObject(chunk.fieldName, args, keywords);
                if (fieldObj == null) {
                    continue;
                }
                if ("r".equals(chunk.conversion)) {
                    fieldObj = fieldObj.__repr__();
                } else if ("s".equals(chunk.conversion)) {
                    fieldObj = fieldObj.__str__();
                } else if (chunk.conversion != null) {
                    throw Py.ValueError("Unknown conversion specifier " + chunk.conversion);
                }
                String formatSpec = chunk.formatSpec;
                if (chunk.formatSpecNeedsExpanding) {
                    if (enclosingIterator != null) // PEP 3101 says only 2 levels
                        throw Py.ValueError("Max string recursion exceeded");
                    formatSpec = buildFormattedString(formatSpec, args, keywords, it);
                }
                renderField(fieldObj, formatSpec, result);
            }
        }
        return result.toString();
    }

    private PyObject getFieldObject(String fieldName, PyObject[] args, String[] keywords) {
        FieldNameIterator iterator = new FieldNameIterator(fieldName);
        Object head = iterator.head();
        PyObject obj = null;
        int positionalCount = args.length - keywords.length;

        if (head instanceof Integer) {
            int index = (Integer) head;
            if (index >= positionalCount) {
                throw Py.IndexError("tuple index out of range");
            }
            obj = args[index];
        } else {
            for (int i = 0; i < keywords.length; i++) {
                if (keywords[i].equals(head)) {
                    obj = args[positionalCount + i];
                    break;
                }
            }
            if (obj == null) {
                throw Py.KeyError((String) head);
            }
        }
        if (obj != null) {
            while (true) {
                FieldNameIterator.Chunk chunk = iterator.nextChunk();
                if (chunk == null) {
                    break;
                }
                if (chunk.is_attr) {
                    obj = obj.__getattr__((String) chunk.value);
                } else {
                    PyObject key = chunk.value instanceof String
                            ? new PyString((String) chunk.value)
                            : new PyInteger((Integer) chunk.value);
                    obj = obj.__getitem__(key);
                }
                if (obj == null) {
                    break;
                }
            }
        }
        return obj;
    }

    private void renderField(PyObject fieldObj, String formatSpec, StringBuilder result) {
        PyString formatSpecStr = formatSpec == null ? Py.EmptyString : new PyString(formatSpec);
        result.append(fieldObj.__format__(formatSpecStr).asString());
    }

    @Override
    public PyObject __format__(PyObject formatSpec) {
        return str___format__(formatSpec);
    }

    @ExposedMethod(doc = BuiltinDocs.str___format___doc)
    final PyObject str___format__(PyObject formatSpec) {
        if (!(formatSpec instanceof PyString)) {
            throw Py.TypeError("__format__ requires str or unicode");
        }

        PyString formatSpecStr = (PyString) formatSpec;
        String result;
        try {
            String specString = formatSpecStr.getString();
            InternalFormatSpec spec = new InternalFormatSpecParser(specString).parse();
            result = formatString(getString(), spec);
        } catch (IllegalArgumentException e) {
            throw Py.ValueError(e.getMessage());
        }
        return formatSpecStr.createInstance(result);
    }

    /**
     * Internal implementation of str.__format__()
     *
     * @param text the text to format
     * @param spec the PEP 3101 formatting specification
     * @return the result of the formatting
     */
    public static String formatString(String text, InternalFormatSpec spec) {
        if (spec.sign != '\0')
            throw new IllegalArgumentException("Sign not allowed in string format specifier");
        if (spec.alternate)
            throw new IllegalArgumentException("Alternate form (#) not allowed in string format specifier");
        if (spec.align == '=')
            throw new IllegalArgumentException("'=' alignment not allowed in string format specifier");
        if (spec.precision >= 0 && text.length() > spec.precision) {
            text = text.substring(0, spec.precision);
        }
        return spec.pad(text, '<', 0);
    }

    /* arguments' conversion helper */

    @Override
    public String asString(int index) throws PyObject.ConversionException {
        return getString();
    }

    @Override
    public String asString() {
        return getString();
    }

    @Override
    public int asInt() {
        // We have to override asInt/Long/Double because we override __int/long/float__,
        // but generally don't want implicit atoi conversions for the base types. blah
        asNumberCheck("__int__", "an integer");
        return super.asInt();
    }

    @Override
    public long asLong() {
        asNumberCheck("__long__", "an integer");
        return super.asLong();
    }

    @Override
    public double asDouble() {
        asNumberCheck("__float__", "a float");
        return super.asDouble();
    }

    private void asNumberCheck(String methodName, String description) {
        PyType type = getType();
        if (type == PyString.TYPE || type == PyUnicode.TYPE || type.lookup(methodName) == null) {
            throw Py.TypeError(description + " is required");
        }
    }

    @Override
    public String asName(int index) throws PyObject.ConversionException {
        return internedString();
    }

    @Override
    protected String unsupportedopMessage(String op, PyObject o2) {
        if (op.equals("+")) {
            return "cannot concatenate ''{1}'' and ''{2}'' objects";
        }
        return super.unsupportedopMessage(op, o2);
    }
}

final class StringFormatter
{
    int index;
    String format;
    StringBuilder buffer;
    boolean negative;
    int precision;
    int argIndex;
    PyObject args;
    boolean unicodeCoercion;

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
        this(format, false);
    }

    public StringFormatter(String format, boolean unicodeCoercion) {
        index = 0;
        this.format = format;
        this.unicodeCoercion = unicodeCoercion;
        buffer = new StringBuilder(format.length()+100);
    }

    PyObject getarg() {
        PyObject ret = null;
        switch(argIndex) {
            // special index indicating a mapping
        case -3:
            return args;
            // special index indicating a single item that has already been
            // used
        case -2:
            break;
            // special index indicating a single item that has not yet been
            // used
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
            if (o instanceof PyInteger)
                return ((PyInteger)o).getValue();
            throw Py.TypeError("* wants int");
        } else {
            if (Character.isDigit(c)) {
                int numStart = index-1;
                while (Character.isDigit(c = pop()))
                    ;
                index -= 1;
                Integer i = Integer.valueOf(
                                    format.substring(numStart, index));
                return i.intValue();
            }
            index -= 1;
            return 0;
        }
    }

    private void checkPrecision(String type) {
        if(precision > 250) {
            // A magic number. Larger than in CPython.
            throw Py.OverflowError("formatted " + type + " is too long (precision too long?)");
        }
        
    }

    private String formatLong(PyObject arg, char type, boolean altFlag) {
        PyString argAsString;
        switch (type) {
            case 'o':
                argAsString = arg.__oct__();
                break;
            case 'x':
            case 'X':
                argAsString = arg.__hex__();
                break;
            default:
                argAsString = arg.__str__();
                break;
        }
        checkPrecision("long");
        String s = argAsString.toString();
        int end = s.length();
        int ptr = 0;

        int numnondigits = 0;
        if (type == 'x' || type == 'X')
            numnondigits = 2;

        if (s.endsWith("L"))
            end--;

        negative = s.charAt(0) == '-';
        if (negative) {
            ptr++;
        }

        int numdigits = end - numnondigits - ptr;
        if (!altFlag) {
            switch (type) {
            case 'o' :
                if (numdigits > 1) {
                     ++ptr;
                     --numdigits;
                }
                break;
            case 'x' :
            case 'X' :
                ptr += 2;
                numnondigits -= 2;
                break;
            }
        }
        if (precision > numdigits) {
            StringBuilder buf = new StringBuilder();
            for (int i = 0; i < numnondigits; ++i)
                buf.append(s.charAt(ptr++));
            for (int i = 0; i < precision - numdigits; i++)
                buf.append('0');
            for (int i = 0; i < numdigits; i++)
                buf.append(s.charAt(ptr++));
            s = buf.toString();
        } else if (end < s.length() || ptr > 0)
            s = s.substring(ptr, end);

        switch (type) {
        case 'X' :
            s = s.toUpperCase();
            break;
        }
        return s;
    }

    /**
     * Formats arg as an integer, with the specified radix
     *
     * type and altFlag are needed to be passed to {@link #formatLong(PyObject, char, boolean)}
     * in case the result of <code>arg.__int__()</code> is a PyLong.
     */
    private String formatInteger(PyObject arg, int radix, boolean unsigned, char type, boolean altFlag) {
        PyObject argAsInt;
        if (arg instanceof PyInteger || arg instanceof PyLong) {
            argAsInt = arg;
        } else {
            // use __int__ to get an int (or long)
            if (arg instanceof PyFloat) {
                // safe to call __int__:
                argAsInt = arg.__int__();
            } else {
                // Same case noted on formatFloatDecimal:
                // We can't simply call arg.__int__() because PyString implements
                // it without exposing it to python (i.e, str instances has no
                // __int__ attribute). So, we would support strings as arguments
                // for %d format, which is forbidden by CPython tests (on
                // test_format.py).
        try {
                    argAsInt = arg.__getattr__("__int__").__call__();
                } catch (PyException e) {
                    // XXX: Swallow customs AttributeError throws from __float__ methods
                    // No better alternative for the moment
                    if (e.match(Py.AttributeError)) {
            throw Py.TypeError("int argument required");
        }
                    throw e;
        }
    }
        }
        if (argAsInt instanceof PyInteger) {
            return formatInteger(((PyInteger)argAsInt).getValue(), radix, unsigned);
        } else { // must be a PyLong (as per __int__ contract)
            return formatLong(argAsInt, type, altFlag);
        }
    }

    private String formatInteger(long v, int radix, boolean unsigned) {
        checkPrecision("integer");
        if (unsigned) {
            if (v < 0)
                v = 0x100000000l + v;
        } else {
            if (v < 0) {
                negative = true;
                v = -v;
            }
        }
        String s = Long.toString(v, radix);
        while (s.length() < precision) {
            s = "0"+s;
        }
        return s;
    }

    private double asDouble(PyObject obj) {
        try {
            return obj.asDouble();
        } catch (PyException pye) {
            throw !pye.match(Py.TypeError) ? pye : Py.TypeError("float argument required");
        }
    }

    static class DecimalFormatTemplate {
        static DecimalFormat template;
        static {
            template = new DecimalFormat("#,##0.#####");
            DecimalFormatSymbols symbols = template.getDecimalFormatSymbols();
            symbols.setNaN("nan");
            symbols.setInfinity("inf");
            template.setDecimalFormatSymbols(symbols);
            template.setGroupingUsed(false);
        }
    }
    private static final DecimalFormat getDecimalFormat() {
        return (DecimalFormat)DecimalFormatTemplate.template.clone();
    }

    private String formatFloatDecimal(double v, boolean truncate) {
        checkPrecision("decimal");
        int prec = precision;
        if (prec == -1)
            prec = 6;
        if (v < 0) {
            v = -v;
            negative = true;
        }

        DecimalFormat decimalFormat = getDecimalFormat();
        decimalFormat.setMaximumFractionDigits(prec);
        decimalFormat.setMinimumFractionDigits(truncate ? 0 : prec);

        String ret = decimalFormat.format(v);
        return ret;
    }

    private String formatFloatExponential(PyObject arg, char e,
                                         boolean truncate)
    {
        StringBuilder buf = new StringBuilder();
        double v = asDouble(arg);
        boolean isNegative = false;
        if (v < 0) {
            v = -v;
            isNegative = true;
        }
        double power = 0.0;
        if (v > 0)
            power = ExtraMath.closeFloor(Math.log10(v));
        //System.err.println("formatExp: "+v+", "+power);
        int savePrecision = precision;
        precision = 2;

        String exp = formatInteger((long)power, 10, false);
        if (negative) {
            negative = false;
            exp = '-'+exp;
        }
        else {
            exp = '+' + exp;
        }

        precision = savePrecision;

        double base = v/Math.pow(10, power);
        buf.append(formatFloatDecimal(base, truncate));
        buf.append(e);

        buf.append(exp);
        negative = isNegative;

        return buf.toString();
    }

    @SuppressWarnings("fallthrough")
    public PyString format(PyObject args) {
        PyObject dict = null;
        this.args = args;
        boolean needUnicode = unicodeCoercion;
        if (args instanceof PyTuple) {
            argIndex = 0;
        } else {
            // special index indicating a single item rather than a tuple
            argIndex = -1;
            if (args instanceof PyDictionary ||
                args instanceof PyStringMap ||
                (!(args instanceof PySequence) &&
                 args.__findattr__("__getitem__") != null))
            {
                dict = args;
                argIndex = -3;
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
                if (dict == null)
                    throw Py.TypeError("format requires a mapping");
                int parens = 1;
                int keyStart = index;
                while (parens > 0) {
                    c = pop();
                    if (c == ')')
                        parens--;
                    else if (c == '(')
                        parens++;
                }
                String tmp = format.substring(keyStart, index-1);
                this.args = dict.__getitem__(needUnicode ? new PyUnicode(tmp) : new PyString(tmp));
            } else {
                push();
            }
            while (true) {
                switch (c = pop()) {
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
            if (width < 0) {
                width = -width;
                ljustFlag = true;
            }
            c = pop();
            if (c == '.') {
                precision = getNumber();
                if (precision < -1)
                    precision = 0;

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
            char fill = ' ';
            String string=null;
            negative = false;
            if (zeroFlag)
                fill = '0';
            else
                fill = ' ';
            switch(c) {
            case 's':
                if (arg instanceof PyUnicode) {
                    needUnicode = true;
                }
            case 'r':
                fill = ' ';
                if (c == 's')
                    if (needUnicode)
                        string = arg.__unicode__().toString();
                    else
                        string = arg.__str__().toString();
                else
                    string = arg.__repr__().toString();
                if (precision >= 0 && string.length() > precision) {
                    string = string.substring(0, precision);
                }

                break;
            case 'i':
            case 'd':
                if (arg instanceof PyLong)
                    string = formatLong(arg, c, altFlag);
                else
                    string = formatInteger(arg, 10, false, c, altFlag);
                break;
            case 'u':
                if (arg instanceof PyLong)
                    string = formatLong(arg, c, altFlag);
                else if (arg instanceof PyInteger || arg instanceof PyFloat)
                    string = formatInteger(arg, 10, false, c, altFlag);
                else throw Py.TypeError("int argument required");
                break;
            case 'o':
                if (arg instanceof PyLong)
                    string = formatLong(arg, c, altFlag);
                else if (arg instanceof PyInteger || arg instanceof PyFloat) {
                    string = formatInteger(arg, 8, false, c, altFlag);
                    if (altFlag && string.charAt(0) != '0') {
                        string = "0" + string;
                    }
                }
                else throw Py.TypeError("int argument required");
                break;
            case 'x':
                if (arg instanceof PyLong)
                    string = formatLong(arg, c, altFlag);
                else if (arg instanceof PyInteger || arg instanceof PyFloat) {
                    string = formatInteger(arg, 16, false, c, altFlag);
                    string = string.toLowerCase();
                    if (altFlag) {
                        string = "0x" + string;
                    }
                }
                else throw Py.TypeError("int argument required");
                break;
            case 'X':
                if (arg instanceof PyLong)
                    string = formatLong(arg, c, altFlag);
                else if (arg instanceof PyInteger || arg instanceof PyFloat) {
                    string = formatInteger(arg, 16, false, c, altFlag);
                    string = string.toUpperCase();
                    if (altFlag) {
                        string = "0X" + string;
                   }
                }
                else throw Py.TypeError("int argument required");
                break;
            case 'e':
            case 'E':
                string = formatFloatExponential(arg, c, false);
                if (c == 'E') {
                    string = string.toUpperCase();
                }
                break;
            case 'f':
            case 'F':
                string = formatFloatDecimal(asDouble(arg), false);
                if (c == 'F') {
                    string = string.toUpperCase();
                }
                break;
            case 'g':
            case 'G':
                int origPrecision = precision;
                if (precision == -1) {
                    precision = 6;
                }

                double v = asDouble(arg);
                int exponent = (int)ExtraMath.closeFloor(Math.log10(Math.abs(v == 0 ? 1 : v)));
                if (v == Double.POSITIVE_INFINITY) {
                    string = "inf";
                } else if (v == Double.NEGATIVE_INFINITY) {
                    string = "-inf";
                } else if (exponent >= -4 && exponent < precision) {
                    precision -= exponent + 1;
                    string = formatFloatDecimal(v, !altFlag);

                    // XXX: this block may be unnecessary now
                    if (altFlag && string.indexOf('.') == -1) {
                        int zpad = origPrecision - string.length();
                        string += '.';
                        if (zpad > 0) {
                            char zeros[] = new char[zpad];
                            for (int ci=0; ci<zpad; zeros[ci++] = '0')
                                ;
                            string += new String(zeros);
                        }
                    }
                } else {
                    // Exponential precision is the number of digits after the decimal
                    // point, whereas 'g' precision is the number of significant digits --
                    // and exponential always provides one significant digit before the
                    // decimal point
                    precision--;
                    string = formatFloatExponential(arg, (char)(c-2), !altFlag);
                }
                if (c == 'G') {
                    string = string.toUpperCase();
                }
                break;
            case 'c':
                fill = ' ';
                if (arg instanceof PyString) {
                    string = ((PyString)arg).toString();
                    if (string.length() != 1) {
                        throw Py.TypeError("%c requires int or char");
                    }
                    if (arg instanceof PyUnicode) {
                        needUnicode = true;
                    }
                    break;
                }
                int val;
                try {
                    // Explicitly __int__ so we can look for an AttributeError (which is
                    // less invasive to mask than a TypeError)
                    val = arg.__int__().asInt();
                } catch (PyException e){
                    if (e.match(Py.AttributeError)) {
                        throw Py.TypeError("%c requires int or char");
                    }
                    throw e;
                }
                if (!needUnicode) {
                    if (val < 0) {
                        throw Py.OverflowError("unsigned byte integer is less than minimum");
                    } else if (val > 255) {
                        throw Py.OverflowError("unsigned byte integer is greater than maximum");
                    }
                } else if (val < 0 || val > PySystemState.maxunicode) {
                    throw Py.OverflowError("%c arg not in range(0x110000) (wide Python build)");
                }
                string = new String(new int[] {val}, 0, 1);
                break;

            default:
                throw Py.ValueError("unsupported format character '" +
                         codecs.encode(Py.newString(c), null, "replace") +
                         "' (0x" + Integer.toHexString(c) + ") at index " +
                         (index-1));
            }
            int length = string.length();
            int skip = 0;
            String signString = null;
            if (negative) {
                signString = "-";
            } else {
                if (signFlag) {
                    signString = "+";
                } else if (blankFlag) {
                    signString = " ";
                }
            }

            if (width < length)
                width = length;
            if (signString != null) {
                if (fill != ' ')
                    buffer.append(signString);
                if (width > length)
                    width--;
            }
            if (altFlag && (c == 'x' || c == 'X')) {
                if (fill != ' ') {
                    buffer.append('0');
                    buffer.append(c);
                    skip += 2;
                }
                width -= 2;
                if (width < 0)
                    width = 0;
                length -= 2;
            }
            if (width > length && !ljustFlag) {
                do {
                    buffer.append(fill);
                } while (--width > length);
            }
            if (fill == ' ') {
                if (signString != null)
                    buffer.append(signString);
                if (altFlag && (c == 'x' || c == 'X')) {
                    buffer.append('0');
                    buffer.append(c);
                    skip += 2;
                }
            }
            if (skip > 0)
                buffer.append(string.substring(skip));
            else
                buffer.append(string);

            while (--width >= length) {
                buffer.append(' ');
            }
        }
        if (argIndex == -1 ||
            (argIndex >= 0 && args.__finditem__(argIndex) != null))
        {
            throw Py.TypeError("not all arguments converted during string formatting");
        }
        if (needUnicode) {
            return new PyUnicode(buffer);
        }
        return new PyString(buffer);
    }

}
