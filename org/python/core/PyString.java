// Copyright © Corporation for National Research Initiatives
package org.python.core;

import java.io.Serializable;


class StringFuncs extends PyBuiltinFunctionSet
{
    StringFuncs(String name, int index, int argcount) {
        super(name, index, argcount, argcount, true, null);
    }

    StringFuncs(String name, int index, int mincount, int maxcount) {
        super(name, index, mincount, maxcount, true, null);
    }

    private String tostring(PyObject o) {
        if (o == Py.None)
            return null;
        if (o instanceof PyString)
            return ((PyString)o).toString();
        throw Py.TypeError("1st arg can't be coerced to string");
    }

    private String tostring(PyObject o, String which) {
        if (o instanceof PyString)
            return ((PyString)o).toString();
        throw Py.TypeError(which + " arg can't be coerced to string");
    }

    private int toint(PyObject o) {
        if (o instanceof PyInteger)
            return ((PyInteger)o).getValue();
        throw Py.TypeError("2nd arg can't be coerced to int");
    }

    private int toint(PyObject o, String which) {
        if (o instanceof PyInteger)
            return ((PyInteger)o).getValue();
        throw Py.TypeError(which + " arg can't be coerced to int");
    }

    public PyObject __call__() {
        PyString s = (PyString)__self__;
        switch (index) {
        case 1:
            return s.__str__();
        case 2:
            return new PyInteger(s.__len__());
        case 3:
            return s.__repr__();
        case 4:
            return Py.newBoolean(s.islower());
        case 5:
            return Py.newBoolean(s.isalpha());
        case 6:
            return Py.newBoolean(s.isdigit());
        case 7:
            return Py.newBoolean(s.isupper());
        case 8:
            return Py.newBoolean(s.isspace());
        case 9:
            return Py.newBoolean(s.istitle());
        case 10:
            return Py.newBoolean(s.isnumeric());
        case 101:
            return new PyString(s.lower());
        case 102:
            return new PyString(s.upper());
        case 103:
            return new PyString(s.swapcase());
        case 104:
            return new PyString(s.strip());
        case 105:
            return new PyString(s.lstrip());
        case 106:
            return new PyString(s.rstrip());
        case 107:
            return s.split();
        case 113:
            return new PyString(s.capitalize());
        default:
            throw argCountError(0);
        }
    }

    public PyObject __call__(PyObject arg) {
        PyString s = (PyString)__self__;
        switch (index) {
        case 11:
            return new PyInteger(s.__cmp__(arg));
        case 12:
            return s.__add__(arg);
        case 13:
            return s.__mod__(arg);
        case 107:
            return s.split(tostring(arg));
        case 108:
            return new PyInteger(s.index(tostring(arg)));
        case 109:
            return new PyInteger(s.rindex(tostring(arg)));
        case 110:
            return new PyInteger(s.count(tostring(arg)));
        case 111:
            return new PyInteger(s.find(tostring(arg)));
        case 112:
            return new PyInteger(s.rfind(tostring(arg)));
        case 114:
            return new PyInteger(s.endswith(tostring(arg)) ? 1 : 0);
        case 115:
            return new PyString(s.join(arg));
        case 117:
            return new PyInteger(s.startswith(tostring(arg)) ? 1 : 0);
        case 118:
            if (arg instanceof PyString)
                return new PyString(s.translate(tostring(arg)));
            else
                return new PyString(s.translate(arg));
        default:
            throw argCountError(1);
        }
    }

    public PyObject __call__(PyObject arg1, PyObject arg2) {
        PyString s = (PyString)__self__;
        String args, args1, args2;
        int argi;
        switch (index) {
        case 107:
            args = tostring(arg1);
            argi = toint(arg2);
            return s.split(args, argi);
        case 108:
            args = tostring(arg1);
            argi = toint(arg2);
            return new PyInteger(s.index(args, argi));
        case 109:
            args = tostring(arg1);
            argi = toint(arg2);
            return new PyInteger(s.rindex(args, argi));
        case 110:
            args = tostring(arg1);
            argi = toint(arg2);
            return new PyInteger(s.count(args, argi));
        case 111:
            args = tostring(arg1);
            argi = toint(arg2);
            return new PyInteger(s.find(args, argi));
        case 112:
            args = tostring(arg1);
            argi = toint(arg2);
            return new PyInteger(s.rfind(args, argi));
        case 114:
            args = tostring(arg1);
            argi = toint(arg2);
            return new PyInteger(s.endswith(args, argi) ? 1 : 0);
        case 116:
            args1 = tostring(arg1);
            args2 = tostring(arg2, "2nd");
            return new PyString(s.replace(args1, args2));
        case 117:
            args = tostring(arg1);
            argi = toint(arg2);
            return new PyInteger(s.startswith(args, argi) ? 1 : 0);
        case 118:
            args1 = tostring(arg1);
            args2 = tostring(arg2, "2nd");
            return new PyString(s.translate(args1, args2));
        default:
            throw argCountError(2);
        }
    }

    public PyObject __call__(PyObject arg1, PyObject arg2, PyObject arg3) {
        PyString s = (PyString)__self__;
        String args, args1, args2;
        int argi, argi2, argi3;
        switch (index) {
        case 108:
            args = tostring(arg1);
            argi2 = toint(arg2);
            argi3 = toint(arg3, "3rd");
            return new PyInteger(s.index(args, argi2, argi3));
        case 109:
            args = tostring(arg1);
            argi2 = toint(arg2);
            argi3 = toint(arg3, "3rd");
            return new PyInteger(s.rindex(args, argi2, argi3));
        case 110:
            args = tostring(arg1);
            argi2 = toint(arg2);
            argi3 = toint(arg3, "3rd");
            return new PyInteger(s.count(args, argi2, argi3));
        case 111:
            args = tostring(arg1);
            argi2 = toint(arg2);
            argi3 = toint(arg3, "3rd");
            return new PyInteger(s.find(args, argi2, argi3));
        case 112:
            args = tostring(arg1);
            argi2 = toint(arg2);
            argi3 = toint(arg3, "3rd");
            return new PyInteger(s.rfind(args, argi2, argi3));
        case 114:
            args = tostring(arg1);
            argi2 = toint(arg2);
            argi3 = toint(arg3, "3rd");
            return new PyInteger(s.endswith(args, argi2, argi3) ? 1 : 0);
        case 116:
            args1 = tostring(arg1);
            args2 = tostring(arg2, "2nd");
            argi = toint(arg3, "3rd");
            return new PyString(s.replace(args1, args2, argi));
        case 117:
            args = tostring(arg1);
            argi2 = toint(arg2);
            argi3 = toint(arg3, "3rd");
            return new PyInteger(s.startswith(args, argi2, argi3) ? 1 : 0);
        default:
            throw argCountError(3);
        }
    }
}




/**
 * A builtin python string.
 */
public class PyString extends PySequence implements ClassDictInit
{
    private String string;
    private transient int cached_hashcode=0;
    private transient boolean interned=false;

    // for PyJavaClass.init()
    public PyString() {
        string = "";
    }

    public PyString(String string) {
        this.string = string;
    }

    public PyString(char c) {
        this(String.valueOf(c));
    }

    /** <i>Internal use only. Do not call this method explicit.</i> */
    public static void classDictInit(PyObject dict) {
        dict.__setitem__("__str__", new StringFuncs("__str__", 1, 0));
        dict.__setitem__("__len__", new StringFuncs("__len__", 2, 0));
        dict.__setitem__("__repr__", new StringFuncs("__repr__", 3, 0));
        dict.__setitem__("islower", new StringFuncs("islower", 4, 0));
        dict.__setitem__("isalpha", new StringFuncs("isalpha", 5, 0));
        dict.__setitem__("isdigit", new StringFuncs("isdigit", 6, 0));
        dict.__setitem__("isupper", new StringFuncs("isupper", 7, 0));
        dict.__setitem__("isspace", new StringFuncs("isspace", 8, 0));
        dict.__setitem__("istitle", new StringFuncs("istitle", 9, 0));
        dict.__setitem__("isnumeric", new StringFuncs("isnumeric", 10, 0));
        dict.__setitem__("__cmp__", new StringFuncs("__cmp__", 11, 1));
        dict.__setitem__("__add__", new StringFuncs("__add__", 12, 1));
        dict.__setitem__("__mod__", new StringFuncs("__mod__", 13, 1));
        // new experimental string methods
        dict.__setitem__("lower", new StringFuncs("lower", 101, 0));
        dict.__setitem__("upper", new StringFuncs("upper", 102, 0));
        dict.__setitem__("swapcase", new StringFuncs("swapcase", 103, 0));
        dict.__setitem__("strip", new StringFuncs("strip", 104, 0));
        dict.__setitem__("lstrip", new StringFuncs("lstrip", 105, 0));
        dict.__setitem__("rstrip", new StringFuncs("rstrip", 106, 0));
        dict.__setitem__("split", new StringFuncs("split", 107, 0, 2));
        dict.__setitem__("index", new StringFuncs("index", 108, 1, 3));
        dict.__setitem__("rindex", new StringFuncs("rindex", 109, 1, 3));
        dict.__setitem__("count", new StringFuncs("count", 110, 1, 3));
        dict.__setitem__("find", new StringFuncs("find", 111, 1, 3));
        dict.__setitem__("rfind", new StringFuncs("rfind", 112, 1, 3));
        dict.__setitem__("capitalize", new StringFuncs("capitalize", 113, 0));
        dict.__setitem__("endswith", new StringFuncs("endswith", 114, 1, 3));
        dict.__setitem__("join", new StringFuncs("join", 115, 1));
        dict.__setitem__("replace", new StringFuncs("replace", 116, 2, 3));
        dict.__setitem__("startswith",
                         new StringFuncs("startswith", 117, 1, 3));
        dict.__setitem__("translate",
                         new StringFuncs("translate", 118, 1, 2));
        // TBD: CPython currently doesn't have these as string methods, but
        // they'd be easy to add for JPython.  For now, compatibility says
        // to access these only through the string module
        //
        // ljust()
        // rjust()
        // center()
        // zfill()
        // expandtabs()
        // capwords()
        //
        // Hide these from Python!
        dict.__setitem__("toString", null);
        dict.__setitem__("internedString", null);
        dict.__setitem__("hashCode", null);
        dict.__setitem__("equals", null);
        dict.__setitem__("__int__", null);
        dict.__setitem__("__long__", null);
        dict.__setitem__("__float__", null);
        dict.__setitem__("__tojava__", null);
        dict.__setitem__("atof", null);
        dict.__setitem__("atoi", null);
        dict.__setitem__("atol", null);
        dict.__setitem__("encode_UnicodeEscape", null);
        dict.__setitem__("decode_UnicodeEscape", null);
    }

    protected String safeRepr() {
        return "'string' object";
    }

    public PyString __str__() {
        return this;
    }

    public int __len__() {
        return string.length();
    }

    public String toString() {
        return string;
    }

    public String internedString() {
        if (interned)
            return string;
        else {
            string = string.intern();
            interned = true;
            return string;
        }
    }

    public PyString __repr__() {
        return new PyString(encode_UnicodeEscape(string, true));
    }

    private static char[] hexdigit = "0123456789ABCDEF".toCharArray();

    public static String encode_UnicodeEscape(String str,
                                              boolean use_quotes)
    {
        int size = str.length();
        StringBuffer v = new StringBuffer(str.length());

        char quote = 0;
        boolean unicode = false;

        if (use_quotes) {
            quote = str.indexOf('\'') >= 0 &&
                             str.indexOf('"') == -1 ? '"' : '\'';
            v.append(quote);
        }

        for (int i = 0; size-- > 0; ) {
            int ch = str.charAt(i++);
            /* Escape quotes */
            if (use_quotes && (ch == quote || ch == '\\')) {
                v.append('\\');
                v.append((char) ch);
            }
            /* Map 16-bit characters to '\\uxxxx' */
            else if (ch >= 256) {
                if (use_quotes && !unicode) {
                   v.insert(0, 'u');
                   unicode = true;
                }
                v.append('\\');
                v.append('u');
                v.append(hexdigit[(ch >> 12) & 0xf]);
                v.append(hexdigit[(ch >> 8) & 0xf]);
                v.append(hexdigit[(ch >> 4) & 0xf]);
                v.append(hexdigit[ch & 15]);
            }
            /* Map non-printable US ASCII to '\ooo' */
            else if (use_quotes && ch == '\n') v.append("\\n");
            else if (use_quotes && ch == '\t') v.append("\\t");
            else if (use_quotes && ch == '\b') v.append("\\b");
            else if (use_quotes && ch == '\f') v.append("\\f");
            else if (use_quotes && ch == '\r') v.append("\\r");
            else if (ch < ' ' || ch >= 128) {
                v.append('\\');
                v.append(hexdigit[(ch >> 6) & 7]);
                v.append(hexdigit[(ch >> 3) & 7]);
                v.append(hexdigit[ch & 7]);
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

    public static String decode_UnicodeEscape(String str, int start, int end,
                                              String errors, boolean unicode)
    {
        StringBuffer v = new StringBuffer(end-start);
        for (int s = start; s < end; ) {
            char ch = str.charAt(s);

            /* Non-escape characters are interpreted as Unicode ordinals */
            if (ch != '\\') {
                v.append(ch);
                s++;
                continue;
            }

            /* \ - Escapes */
            s++;
            ch = str.charAt(s++);
            switch (ch) {

            /* \x escapes */
            case '\n': break;
            case '\\': v.append('\\'); break;
            case '\'': v.append('\''); break;
            case '\"': v.append('\"'); break;
            case 'b': v.append('\b'); break;
            case 'f': v.append('\014'); break; /* FF */
            case 't': v.append('\t'); break;
            case 'n': v.append('\n'); break;
            case 'r': v.append('\r'); break;
            case 'v': v.append('\013'); break; /* VT */
            case 'a': v.append('\007'); break; /* BEL, not classic C */

            /* \OOO (octal) escapes */
            case '0': case '1': case '2': case '3':
            case '4': case '5': case '6': case '7':

                int x = Character.digit(ch, 8);
                for (int j = 0; j < 2 && s < end; j++, s++) {
                    ch = str.charAt(s);
                    if (ch < '0' || ch > '7')
                        break;
                    x = (x<<3) + Character.digit(ch, 8);
                }
                v.append((char) x);
                break;

            case 'x':
                int i;
                for (x = 0, i = 0; i < 2 && s < end; i++) {
                    ch = str.charAt(s + i);
                    int d = Character.digit(ch, 16);
                    if (d == -1) {
                        codecs.decoding_error("unicode escape", v, errors,
                                                     "truncated \\xXX");
                        i++;
                        break;
                    }

                    x = ((x<<4) & ~0xF) + d;
                }
                s += i;
                v.append((char) x);
                break;

            /* \ uXXXX with 4 hex digits */
            case 'u':
                if (!unicode) {
                    v.append('\\');
                    v.append('u');
                    break;
                }
                if (s+4 > end) {
                    codecs.decoding_error("unicode escape", v, errors,
                                              "truncated \\uXXXX");
                    break;
                }
                for (x = 0, i = 0; i < 4; i++) {
                    ch = str.charAt(s + i);
                    int d  = Character.digit(ch, 16);
                    if (d == -1) {
                        codecs.decoding_error("unicode escape", v, errors,
                                              "truncated \\uXXXX");
                        break;
                    }
                    x = ((x<<4) & ~0xF) + d;
                }
                s += i;
                v.append((char) x);
                break;

            case 'N':
                if (!unicode) {
                    v.append('\\');
                    v.append('N');
                    break;
                }
                /* Ok, we need to deal with Unicode Character Names now,
                 * make sure we've imported the hash table data...
                 */
                if (pucnHash == null) {
                     PyObject mod = imp.importName("ucnhash", true);
                     mod = mod.__call__();
                     pucnHash = (ucnhashAPI) mod.__tojava__(Object.class);
                     if (pucnHash.getCchMax() < 0)
                         codecs.decoding_error("unicode escape", v, errors,
                                 "Unicode names not loaded");
                }

                if (str.charAt(s) == '{') {
                    int startName = s + 1;
                    int endBrace = startName;

                    /* look for either the closing brace, or we
                     * exceed the maximum length of the unicode
                     * character names
                     */
                    int maxLen = pucnHash.getCchMax();
                    while (endBrace < end && str.charAt(endBrace) != '}'
                           && (endBrace - startName) <= maxLen) {
                        endBrace++;
                    }
                    if (endBrace != end && str.charAt(endBrace) == '}') {
                         int value = pucnHash.getValue(str, startName,
                                                       endBrace);
                         if (value < 0) {
                             codecs.decoding_error("unicode escape", v,
                                  errors, "Invalid Unicode Character Name");
                             v.append('\\');
                             v.append(str.charAt(s-1));
                             break;
                         }

                         if (value < 1<<16) {
                             /* In UCS-2 range, easy solution.. */
                             v.append((char) value);
                         } else {
                             /* Oops, its in UCS-4 space, */
                             /*  compute and append the two surrogates: */
                             /*  translate from 10000..10FFFF to 0..FFFFF */
                             value -= 0x10000;

                             /* high surrogate = top 10 bits added to D800 */
                             v.append((char) (0xD800 + (value >> 10)));

                             /* low surrogate = bottom 10 bits added to DC00*/
                             v.append((char) (0xDC00 + (value & ~0xFC00)));
                        }
                        s = endBrace + 1;
                    } else {
                         codecs.decoding_error("unicode escape", v, errors,
                              "Unicode name missing closing brace");
                         v.append('\\');
                         v.append(str.charAt(s-1));
                         break;
                    }
                    break;
                }
                codecs.decoding_error("unicode escape", v, errors,
                                      "Missing opening brace for Unicode " +
                                      "Character Name escape");

                /* fall through on purpose */
           default:
               v.append('\\');
               v.append(str.charAt(s-1));
               break;
           }
       }
       return v.toString();
    }

    public boolean equals(Object other) {
        if (!(other instanceof PyString))
            return false;

        PyString o = (PyString)other;
        if (interned && o.interned)
            return string == o.string;

        return string.equals(o.string);
    }

    public int __cmp__(PyObject other) {
        if (!(other instanceof PyString))
            return -2;

        int c = string.compareTo(((PyString)other).string);
        return c < 0 ? -1 : c > 0 ? 1 : 0;
    }

    public PyObject __eq__(PyObject other) {
        String s = coerce(other);
        if (s == null)
            return null;
        return string.equals(s) ? Py.One : Py.Zero;
    }

    public PyObject __ne__(PyObject other) {
        String s = coerce(other);
        if (s == null)
            return null;
        return string.equals(s) ? Py.Zero : Py.One;
    }

    public PyObject __lt__(PyObject other) {
        String s = coerce(other);
        if (s == null)
            return null;
        return string.compareTo(s) < 0 ? Py.One : Py.Zero;
    }

    public PyObject __le__(PyObject other) {
        String s = coerce(other);
        if (s == null)
            return null;
        return string.compareTo(s) <= 0 ? Py.One : Py.Zero;
    }

    public PyObject __gt__(PyObject other) {
        String s = coerce(other);
        if (s == null)
            return null;
        return string.compareTo(s) > 0 ? Py.One : Py.Zero;
    }

    public PyObject __ge__(PyObject other) {
        String s = coerce(other);
        if (s == null)
            return null;
        return string.compareTo(s) >= 0 ? Py.One : Py.Zero;
    }

    private static String coerce(PyObject o) {
        if (o instanceof PyString)
            return o.toString();
        return null;
    }

    public int hashCode() {
        if (cached_hashcode == 0)
            cached_hashcode = string.hashCode();
        return cached_hashcode;
    }

    private byte[] getBytes() {
        byte[] buf = new byte[string.length()];
        string.getBytes(0, string.length(), buf, 0);
        return buf;
    }

    public Object __tojava__(Class c) {
        //This is a hack to make almost all Java calls happy
        if (c == String.class || c == Object.class || c == Serializable.class)
            return string;
        if (c == Character.TYPE)
            if (string.length() == 1)
                return new Character(string.charAt(0));

        if (c.isArray()) {
            if (c.getComponentType() == Byte.TYPE)
                return getBytes();
            if (c.getComponentType() == Character.TYPE)
                return string.toCharArray();
        }

        if (c.isInstance(this))
            return this;

        return Py.NoConversion;
    }

    protected PyObject get(int i) {
        return Py.newString(string.charAt(i));
    }

    protected PyObject getslice(int start, int stop, int step) {
        if (step > 0 && stop < start)
            stop = start;
        if (step == 1)
            return new PyString(string.substring(start, stop));
        else {
            int n = sliceLength(start, stop, step);
            char new_chars[] = new char[n];
            int j = 0;
            for (int i=start; j<n; i+=step)
                new_chars[j++] = string.charAt(i);

            return new PyString(new String(new_chars));
        }
    }

    public boolean __contains__(PyObject o) {
        if (!(o instanceof PyString) || o.__len__() != 1)
            throw Py.TypeError("string member test needs char left operand");
        PyString other = (PyString) o;
        return string.indexOf(other.string) >= 0;
    }

    protected PyObject repeat(int count) {
        int s = string.length();
        char new_chars[] = new char[s*count];
        for (int i=0; i<count; i++) {
            string.getChars(0, s, new_chars, i*s);
        }
        return new PyString(new String(new_chars));
    }

    public PyObject __add__(PyObject generic_other) {
        if (generic_other instanceof PyString) {
            PyString other = (PyString)generic_other;
            return new PyString(string.concat(other.string));
        }
        else return null;
    }


    public PyObject __mod__(PyObject other) {
        StringFormatter fmt = new StringFormatter(string);
        return new PyString(fmt.format(other));
    }

    public PyInteger __int__() {
        return Py.newInteger(atoi(10));
    }

    public PyLong __long__() {
        return atol(10);
    }

    public PyFloat __float__() {
        return new PyFloat(atof());
    }

    public PyComplex __complex__() {
        boolean got_re = false;
        boolean got_im = false;
        boolean done = false;
        boolean sw_error = false;

        int s = 0;
        int n = string.length();
        while (s < n && Character.isSpaceChar(string.charAt(s)))
            s++;

        if (s == n) {
            throw Py.ValueError("empty string for complex()");
        }

        double z = -1.0;
        double x = 0.0;
        double y = 0.0;

        int sign = 1;
        do {
            char c = string.charAt(s);
            switch (c) {
            case '\0':
                if (s != n)
                    throw Py.ValueError("null byte in argument for " +
                                        "complex()");
                if (!done)
                    sw_error = true;
                break;

            case '-':
                sign = -1;
                /* Fallthrough */
            case '+':
                if (done)
                    sw_error = true;
                s++;
                c = string.charAt(s);
                if  (c == '\0' || c == '+' || c == '-' ||
                           Character.isSpaceChar(c))
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
                s++;
                c = string.charAt(s);

                if  (c != '+' && c != '-')
                    done = true;
                break;

            default:
                if (Character.isSpaceChar(c)) {
                    while (s < n && Character.isSpaceChar(string.charAt(s)))
                        s++;
                    if (s != n)
                        sw_error = true;
                    else
                        done = true;
                    break;
                }
                c = string.charAt(s);
                boolean digit_or_dot = (c == '.' || Character.isDigit(c));
                if (done || !digit_or_dot) {
                    sw_error = true;
                    break;
                }
                int end = endDouble(string, s);
                z = Double.valueOf(string.substring(s, end)).doubleValue();
                s=end;
                c = string.charAt(s);
                if  (c == 'J' || c == 'j') {
                    break;
                }
                if  (got_re) {
                   sw_error = true;
                   break;
                }

                /* accept a real part */
                x = sign * z;
                got_re = true;
                if (got_im)
                    done = true;
                z = -1.0;
                sign = 1;
                break;

             }  /* end of switch  */

        } while (s < n && !sw_error);

        if (sw_error) {
            throw Py.ValueError("malformed string for complex() " +
                                string.substring(s));
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
                    c = string.charAt(s++);
                    if (c == '+' || c == '-')
                        continue;
                }
            }
            break;
        }
        return s-1;
    }

    // Add in methods from string module
    public String lower() {
        return string.toLowerCase();
    }

    public String upper() {
        return string.toUpperCase();
    }

    public String title() {
        char[] chars = string.toCharArray();
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
        char[] chars = string.toCharArray();
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
        char[] chars = string.toCharArray();
        int n=chars.length;
        int start=0;
        while (start < n && Character.isWhitespace(chars[start]))
            start++;

        int end=n-1;
        while (end >= 0 && Character.isWhitespace(chars[end]))
            end--;

        if (end >= start) {
            return (end < n-1 || start > 0)
                ? string.substring(start, end+1) : string;
        } else {
            return "";
        }
    }

    public String lstrip() {
        char[] chars = string.toCharArray();
        int n=chars.length;
        int start=0;
        while (start < n && Character.isWhitespace(chars[start]))
            start++;

        return (start > 0) ? string.substring(start, n) : string;
    }

    public String rstrip() {
        char[] chars = string.toCharArray();
        int n=chars.length;
        int end=n-1;
        while (end >= 0 && Character.isWhitespace(chars[end]))
            end--;

        return (end < n-1) ? string.substring(0, end+1) : string;
    }


    public PyList split() {
        return split(null, -1);
    }

    public PyList split(String sep) {
        return split(sep, -1);
    }

    public PyList split(String sep, int maxsplit) {
        if (sep != null)
            return splitfields(sep, maxsplit);

        PyList list = new PyList();

        char[] chars = string.toCharArray();
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
            list.append(new PyString(string.substring(start, index)));
            splits++;
        }
        if (index < n) {
            while (index < n && Character.isWhitespace(chars[index]))
                index++;
            list.append(new PyString(string.substring(index, n)));
        }
        return list;
    }

    private PyList splitfields(String sep, int maxsplit) {
        if (sep.length() == 0) {
            throw Py.ValueError("empty separator");
        }

        PyList list = new PyList();

        int length = string.length();
        if (maxsplit < 0)
            maxsplit = length;

        int lastbreak = 0;
        int splits = 0;
        int sepLength = sep.length();
        while (splits < maxsplit) {
            int index = string.indexOf(sep, lastbreak);
            if (index == -1)
                break;
            splits += 1;
            list.append(new PyString(string.substring(lastbreak, index)));
            lastbreak = index + sepLength;
        }
        if (lastbreak <= length) {
            list.append(new PyString(string.substring(lastbreak, length)));
        }
        return list;
    }

    public PyList splitlines() {
        return splitlines(false);
    }

    public PyList splitlines(boolean keepends) {
        PyList list = new PyList();

        char[] chars = string.toCharArray();
        int n=chars.length;

        int j = 0;
        for (int i = 0; i < n; ) {
            /* Find a line and append it */
            while (i < n && (Character.getType(chars[i]) &
                                   Character.LINE_SEPARATOR) == 0)
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
            list.append(new PyString(string.substring(j, eol)));
            j = i;
        }
        if (j < n) {
            list.append(new PyString(string.substring(j, n)));
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

        if (start < 0)
            start = n+start;
        if (end < 0)
            end = n+end;

        int index;
        if (end < n) {
            index = string.substring(start, end).indexOf(sub);
        } else {
            index = string.indexOf(sub, start);
        }
        if (index == -1)
            throw Py.ValueError("substring not found in string.index");
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

        if (start < 0)
            start = n+start;
        if (end < 0)
            end = n+end;

        int index;
        if (start > 0) {
            index = string.substring(start, end).lastIndexOf(sub);
        } else {
            index = string.lastIndexOf(sub, end);
        }
        if (index == -1)
            throw Py.ValueError("substring not found in string.rindex");
        return index;
    }

    public int count(String sub) {
        return count(sub, 0, string.length());
    }
    public int count(String sub, int start) {
        return count(sub, start, string.length());
    }

    public int count(String sub, int start, int end) {
        int len = string.length();
        if (end > len)
            end = len;
        if (end < 0)
            end += len;
        if (end < 0)
            end = 0;
        if (start < 0)
            start += len;
        if (start < 0)
            start = 0;

        int n = sub.length();
        end = end + 1 - n;
        if (n == 0)
            return end-start;

        int count=0;
        while (start < end) {
            int index = string.indexOf(sub, start);
            if (index >= end || index == -1)
                break;
            count++;
            start = index + n;
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
        if (start < 0)
            start = n+start;
        if (end < 0)
            end = n+end;
        if (end > n)
            end = n;
        if (start > end)
            start = end;
        int slen = sub.length();
        end = end-slen;

        int index = string.indexOf(sub, start);
        if (index > end)
            return -1;
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
        if (start < 0)
            start = n+start;
        if (end < 0)
            end = n+end;
        if (end > n)
            end = n;
        if (start > end)
            start = end;
        int slen = sub.length();
        end = end-slen;

        int index = string.lastIndexOf(sub, end);
        if (index < start)
            return -1;
        return index;
    }

    public double atof() {
        StringBuffer s = null;
        int n = string.length();
        for (int i = 0; i < n; i++) {
            char ch = string.charAt(i);
            if (Character.isDigit(ch)) {
                if (s == null)
                    s = new StringBuffer(string);
                int val = Character.digit(ch, 10);
                s.setCharAt(i, Character.forDigit(val, 10));
            }
        }
        String sval = string;
        if (s != null)
            sval = s.toString();
        try {
            return Double.valueOf(sval).doubleValue();
        }
        catch (NumberFormatException exc) {
            throw Py.ValueError("invalid literal for __float__: "+string);
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
        int e = string.length();

        while (b < e && Character.isWhitespace(string.charAt(b)))
            b++;

        while (e > b && Character.isWhitespace(string.charAt(e-1)))
            e--;

        char sign = 0;
        if (b < e) {
            sign = string.charAt(b);
            if (sign == '-' || sign == '+') {
                b++;
                while (b < e && Character.isWhitespace(string.charAt(b)))
                    b++;
            }
        }

        if (base == 0 || base == 16) {
            if (string.charAt(b) == '0') {
                if (b < e-1 && string.charAt(b+1) == 'x') {
                    if (base == 0)
                        base = 16;
                    b += 2;
                } else {
                    if (base == 0)
                        base = 8;
                }
            }
        }

        if (base == 0)
            base = 10;

        String s = string;
        if (b > 0 || e < string.length())
            s = string.substring(b, e);

        try {
            long result = Long.parseLong(s, base);
            if (result < 0 && !(sign == '-' && result == -result))
                throw Py.ValueError("invalid literal for __int__: "+string);
            if (sign == '-')
                result = - result;
            if (result < Integer.MIN_VALUE || result > Integer.MAX_VALUE)
                throw Py.ValueError("invalid literal for __int__: "+string);
            return (int) result;
        } catch (NumberFormatException exc) {
            throw Py.ValueError("invalid literal for __int__: "+string);
        } catch (StringIndexOutOfBoundsException exc) {
            throw Py.ValueError("invalid literal for __int__: "+string);
        }
    }

    public PyLong atol() {
        return atol(10);
    }

    public PyLong atol(int base) {
        String str = string;
        int b = 0;
        int e = str.length();

        while (b < e && Character.isWhitespace(str.charAt(b)))
            b++;

        while (e > b && Character.isWhitespace(str.charAt(e-1)))
            e--;
        if (e > b && (str.charAt(e-1) == 'L' || str.charAt(e-1) == 'l'))
            e--;

        char sign = 0;
        if (b < e) {
            sign = string.charAt(b);
            if (sign == '-' || sign == '+') {
                b++;
                while (b < e && Character.isWhitespace(str.charAt(b)))
                    b++;
            }
        }

        if (base == 0) {
            if (str.charAt(b) != '0')
                base = 10;
            else if (str.charAt(b+1) == 'x' || str.charAt(b+1) == 'X') {
                base = 16;
                b += 2;
            } else
                base = 8;
        }
        if (base < 2 || base > 36)
            throw Py.ValueError("invalid base for long literal:" + base);

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
            throw Py.ValueError("invalid literal for __int__: "+str);
        } catch (StringIndexOutOfBoundsException exc) {
            throw Py.ValueError("invalid literal for __int__: "+str);
        }
    }


    private static String spaces(int n) {
        char[] chars = new char[n];
        for (int i=0; i<n; i++)
            chars[i] = ' ';
        return new String(chars);
    }

    public String ljust(int width) {
        int n = width-string.length();
        if (n <= 0)
            return string;
        return string+spaces(n);
    }

    public String rjust(int width) {
        int n = width-string.length();
        if (n <= 0)
            return string;
        return spaces(n)+string;
    }

    public String center(int width) {
        int n = width-string.length();
        if (n <= 0)
            return string;
        int half = n/2;
        if (n%2 > 0 &&  width%2 > 0)
            half += 1;
        return spaces(half)+string+spaces(n-half);
    }

    public String zfill(int width) {
        String s = string;
        int n = s.length();
        if (n >= width)
            return s;
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
            if (c == '\n' || c == '\r') {
                position = -1;
            }
            buf.append(c);
            position++;
        }
        return buf.toString();
    }

    public String capitalize() {
        if (string.length() == 0)
            return string;
        String first = string.substring(0,1).toUpperCase();
        return first.concat(string.substring(1).toLowerCase());
    }

    public String replace(String oldPiece, String newPiece) {
        return replace(oldPiece, newPiece, string.length());
    }

    public String replace(String oldPiece, String newPiece, int maxsplit) {
        PyString newstr = new PyString(newPiece);
        return newstr.join(split(oldPiece, maxsplit));
    }

    public String join(PyObject seq) {
        // trigger same TypeError as CPython if seq is not a sequence
        int seqlen = __builtin__.len(seq);
        StringBuffer buf = new StringBuffer();

        PyObject obj;
        for (int i=0; (obj = seq.__finditem__(i)) != null; i++) {
            if (!(obj instanceof PyString))
                 throw Py.TypeError(
                        "sequence item " + i + ": expected string, " +
                        obj.safeRepr() + " found");
            if (i > 0)
                buf.append(string);
            buf.append(obj.__str__());
        }
        return buf.toString();
    }


    public boolean startswith(String prefix) {
        return string.startsWith(prefix);
    }

    public boolean startswith(String prefix, int offset) {
        return string.startsWith(prefix, offset);
    }

    public boolean startswith(String prefix, int start, int end) {
        if (start < 0 || start + prefix.length() > string.length())
            return false;
        String substr = string.substring(start, end);
        return substr.startsWith(prefix);
    }

    public boolean endswith(String suffix) {
        return string.endsWith(suffix);
    }

    public boolean endswith(String suffix, int start) {
        return endswith(suffix, start, string.length());
    }

    public boolean endswith(String suffix, int start, int end) {
        int len = string.length();

        if (start < 0 || start > len || suffix.length() > len)
            return false;

        end = (end <= len ? end : len);
        if (end < start)
            return false;

        String substr = string.substring(start, end);
        return substr.endsWith(suffix);
    }

    //public static String zfill(PyObject o, int width) {
    //    return zfill(o.toString(), width);
    //}

    public String translate(String table) {
        return translate(table, null);
    }

    public String translate(String table, String deletechars) {
        if (table.length() != 256)
            throw Py.ValueError(
                "translation table must be 256 characters long");

        StringBuffer buf = new StringBuffer(string.length());
        for (int i=0; i < string.length(); i++) {
            char c = string.charAt(i);
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

    public String translate(PyObject table) {
        StringBuffer v = new StringBuffer(string.length());
        for (int i=0; i < string.length(); i++) {
            char ch = string.charAt(i);

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
        int n = string.length();

        /* Shortcut for single character strings */
        if (n == 1)
            return Character.isLowerCase(string.charAt(0));

        boolean cased = false;
        for (int i = 0; i < n; i++) {
            char ch = string.charAt(i);

            if (Character.isUpperCase(ch) || Character.isTitleCase(ch))
                return false;
            else if (!cased && Character.isLowerCase(ch))
                cased = true;
        }
        return cased;
    }

    public boolean isupper() {
        int n = string.length();

        /* Shortcut for single character strings */
        if (n == 1)
            return Character.isUpperCase(string.charAt(0));

        boolean cased = false;
        for (int i = 0; i < n; i++) {
            char ch = string.charAt(i);

            if (Character.isLowerCase(ch) || Character.isTitleCase(ch))
                return false;
            else if (!cased && Character.isUpperCase(ch))
                cased = true;
        }
        return cased;
    }

    public boolean isalpha() {
        int n = string.length();

        /* Shortcut for single character strings */
        if (n == 1)
            return Character.isLetter(string.charAt(0));

        if (n == 0)
            return false;

        for (int i = 0; i < n; i++) {
            char ch = string.charAt(i);

            if (!Character.isLetter(ch))
                return false;
        }
        return true;
    }

    public boolean isalnum() {
        int n = string.length();

        /* Shortcut for single character strings */
        if (n == 1)
            return _isalnum(string.charAt(0));

        if (n == 0)
            return false;

        for (int i = 0; i < n; i++) {
            char ch = string.charAt(i);

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
        int n = string.length();

        /* Shortcut for single character strings */
        if (n == 1) {
            char ch = string.charAt(0);
            return _isdecimal(ch);
        }

        if (n == 0)
            return false;

        for (int i = 0; i < n; i++) {
            char ch = string.charAt(i);

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
        int n = string.length();

        /* Shortcut for single character strings */
        if (n == 1)
            return Character.isDigit(string.charAt(0));

        if (n == 0)
            return false;

        for (int i = 0; i < n; i++) {
            char ch = string.charAt(i);

            if (!Character.isDigit(ch))
                return false;
        }
        return true;
    }

    public boolean isnumeric() {
        int n = string.length();

        /* Shortcut for single character strings */
        if (n == 1)
            return _isnumeric(string.charAt(0));

        if (n == 0)
            return false;

        for (int i = 0; i < n; i++) {
            char ch = string.charAt(i);
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
        int n = string.length();

        /* Shortcut for single character strings */
        if (n == 1)
            return Character.isTitleCase(string.charAt(0)) ||
                   Character.isUpperCase(string.charAt(0));

        boolean cased = false;
        boolean previous_is_cased = false;
        for (int i = 0; i < n; i++) {
            char ch = string.charAt(i);

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
        int n = string.length();

        /* Shortcut for single character strings */
        if (n == 1)
            return Character.isWhitespace(string.charAt(0));

        if (n == 0)
            return false;

        for (int i = 0; i < n; i++) {
            char ch = string.charAt(i);

            if (!Character.isWhitespace(ch))
                return false;
        }
        return true;
    }

    public boolean isunicode() {
        int n = string.length();
        for (int i = 0; i < n; i++) {
            char ch = string.charAt(i);
            if (ch > 255)
                return true;
        }
        return false;
    }

    public PyString encode() {
        return encode(null, null);
    }

    public PyString encode(String encoding) {
        return encode(encoding, null);
    }

    public PyString encode(String encoding, String errors) {
        return codecs.encode(this, encoding, errors);
    }
}



final class StringFormatter
{
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
            return -1;
        }
    }

    public String formatLong(PyString arg, char type, boolean altFlag) {
        String s = arg.toString();
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
            StringBuffer buf = new StringBuffer();
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
        case 'x' :
            s = s.toLowerCase();
            break;
        }
        return s;
    }

    public String formatInteger(PyObject arg, int radix, boolean unsigned) {
        return formatInteger(arg.__int__().getValue(), radix, unsigned);
    }

    public String formatInteger(long v, int radix, boolean unsigned) {
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

    public String formatFloatDecimal(PyObject arg, boolean truncate) {
        return formatFloatDecimal(arg.__float__().getValue(), truncate);
    }

    public String formatFloatDecimal(double v, boolean truncate) {
        java.text.NumberFormat format = java.text.NumberFormat.getInstance(
                                           java.util.Locale.US);
        int prec = precision;
        if (prec == -1)
            prec = 6;
        if (v < 0) {
            v = -v;
            negative = true;
        }
        format.setMaximumFractionDigits(prec);
        format.setMinimumFractionDigits(truncate ? 0 : prec);
        format.setGroupingUsed(false);

        String ret = format.format(v);
//         System.err.println("formatFloat: "+v+", prec="+prec+", ret="+ret);
//         if (ret.indexOf('.') == -1) {
//             return ret+'.';
//         }
        return ret;
    }

    public String formatFloatExponential(PyObject arg, char e,
                                         boolean truncate)
    {
        StringBuffer buf = new StringBuffer();
        double v = arg.__float__().getValue();
        boolean isNegative = false;
        if (v < 0) {
            v = -v;
            isNegative = true;
        }
        double power = 0.0;
        if (v > 0)
            power = Math.floor(Math.log(v)/Math.log(10));
        //System.err.println("formatExp: "+v+", "+power);
        int savePrecision = precision;

        if (truncate)
            precision = -1;
        else
            precision = 3;

        String exp = formatInteger((long)power, 10, false);
        if (negative) {
            negative = false;
            exp = '-'+exp;
        }
        else {
            if (!truncate)
                exp = '+'+exp;
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
                //System.out.println("( found");
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
                this.args = dict.__getitem__(new PyString(tmp));
                //System.out.println("args: "+args+", "+argIndex);
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
            c = pop();
            if (c == '.') {
                precision = getNumber();
                if (precision == -1)
                    precision = 0;
                if (precision > 250) {
                    // A magic number. Larger than in CPython.
                    throw Py.OverflowError(
                         "formatted float is too long (precision too long?)");
                }

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
            if (zeroFlag)
                fill = '0';
            else
                fill = ' ';

            switch(c) {
            case 's':
            case 'r':
                fill = ' ';
                if (c == 's')
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
                    string = formatLong(arg.__str__(), c, altFlag);
                else
                    string = formatInteger(arg, 10, false);
                break;
            case 'u':
                if (arg instanceof PyLong)
                    string = formatLong(arg.__str__(), c, altFlag);
                else
                    string = formatInteger(arg, 10, true);
                break;
            case 'o':
                if (arg instanceof PyLong)
                    string = formatLong(arg.__oct__(), c, altFlag);
                else {
                    string = formatInteger(arg, 8, true);
                    if (altFlag) {
                        string = "0" + string;
                    }
                }
                break;
            case 'x':
                if (arg instanceof PyLong)
                    string = formatLong(arg.__hex__(), c, altFlag);
                else {
                    string = formatInteger(arg, 16, true);
                    string = string.toLowerCase();
                    if (altFlag) {
                        string = "0x" + string;
                    }
                }
                break;
            case 'X':
                if (arg instanceof PyLong)
                    string = formatLong(arg.__hex__(), c, altFlag);
                else {
                    string = formatInteger(arg, 16, true);
                    string = string.toUpperCase();
                    if (altFlag) {
                        string = "0X" + string;
                   }
                }

                break;
            case 'e':
            case 'E':
                string = formatFloatExponential(arg, c, false);
                break;
            case 'f':
                string = formatFloatDecimal(arg, false);
//                 if (altFlag && string.indexOf('.') == -1)
//                     string += '.';
                break;
            case 'g':
            case 'G':
                int prec = precision;
                if (prec == -1)
                    prec = 6;
                double v = arg.__float__().getValue();
                int digits = (int)Math.ceil(ExtraMath.log10(v));
                if (digits > 0) {
                    if (digits <= prec) {
                        precision = prec-digits;
                        string = formatFloatDecimal(arg, true);
                    } else {
                        string = formatFloatExponential(arg, (char)(c-2),
                                                        true);
                    }
                } else {
                    string = formatFloatDecimal(arg, true);
                }
                if (altFlag && string.indexOf('.') == -1) {
                    int zpad = prec - string.length();
                    string += '.';
                    if (zpad > 0) {
                        char zeros[] = new char[zpad];
                        for (int ci=0; ci<zpad; zeros[ci++] = '0')
                            ;
                        string += new String(zeros);
                    }
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
                char tmp = (char)arg.__int__().getValue();
                string = new Character(tmp).toString();
                break;

            default:
                throw Py.ValueError("unsupported format character '"+c+"'");
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
            throw Py.TypeError("not all arguments converted");
        }
        return buffer.toString();
    }
}
