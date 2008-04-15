package org.python.core;

import org.python.expose.ExposedMethod;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedType;
import org.python.expose.MethodType;
import org.python.modules._codecs;

/**
 * a builtin python unicode string.
 */
@ExposedType(name = "unicode", base = PyBaseString.class)
public class PyUnicode extends PyString {

    public static final PyType TYPE = PyType.fromClass(PyUnicode.class);
    
    // for PyJavaClass.init()
    public PyUnicode() {
        this(TYPE, "");
    }

    public PyUnicode(String string) {
        this(TYPE, string);
    }
    
    public PyUnicode(PyType subtype, String string) {
        super(subtype, string);
    }
    
    public PyUnicode(PyString pystring) {
        this(TYPE, pystring);
    }

    public PyUnicode(PyType subtype, PyString pystring) {
        this(subtype, pystring instanceof PyUnicode ? pystring.string : pystring.decode()
                .toString());
    }

    public PyUnicode(char c) {
        this(TYPE,String.valueOf(c));
    }

    /**
     * Creates a PyUnicode from an already interned String. Just means it won't
     * be reinterned if used in a place that requires interned Strings.
     */
    public static PyUnicode fromInterned(String interned) {
        PyUnicode uni = new PyUnicode(TYPE, interned);
        uni.interned = true;
        return uni;
    }

    @ExposedNew
    final static PyObject unicode_new(PyNewWrapper new_, boolean init, PyType subtype,
            PyObject[] args, String[] keywords) {
        ArgParser ap = new ArgParser("unicode",
                                     args,
                                     keywords,
                                     new String[] {"string",
                                                   "encoding",
                                                   "errors"},
                                     0);
        PyObject S = ap.getPyObject(0, null);
        String encoding = ap.getString(1, null);
        String errors = ap.getString(2, null);
        if (new_.for_type == subtype) {
            if (S == null) {
                return new PyUnicode("");
            }
            if (S instanceof PyUnicode) {
                return new PyUnicode(((PyUnicode)S).string);
            }
            if (S instanceof PyString) {
                return new PyUnicode(codecs.decode((PyString)S, encoding, errors).toString());
            }
            return S.__unicode__();
        } else {
            if (S == null) {
                return new PyUnicodeDerived(subtype, Py.EmptyString);
            }
            if (S instanceof PyUnicode) {
                return new PyUnicodeDerived(subtype, (PyUnicode)S);
            } else {
                return new PyUnicodeDerived(subtype, S.__str__());
            }
        }
    }

    public PyString createInstance(String str){
       return new PyUnicode(str);
    }

    public PyObject __mod__(PyObject other) {
        return unicode___mod__(other);
    }

    @ExposedMethod
    final PyObject unicode___mod__(PyObject other){
        StringFormatter fmt = new StringFormatter(string, true);
        return fmt.format(other).__unicode__();
    }

    @ExposedMethod
    final PyUnicode unicode___unicode__() {
        return str___unicode__();
    }

    public PyString __str__() {
        return unicode___str__();
    }

    @ExposedMethod
    final PyString unicode___str__() {
        return new PyString(encode());
    }

    final int unicode___len__() {
        return str___len__();
    }

    public PyString __repr__() {
        return unicode___repr__();
    }

    @ExposedMethod
    final PyString unicode___repr__() {
        return new PyString("u'" + encode_UnicodeEscape(string, false) + "'");
    }
    
    @ExposedMethod(type = MethodType.CMP)
    final int unicode___cmp__(PyObject other) {
        return str___cmp__(other);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject unicode___eq__(PyObject other) {
        return str___eq__(other);
    }
    
    @ExposedMethod(type = MethodType.BINARY)
    final PyObject unicode___ne__(PyObject other) {
        return str___ne__(other);
    }

    @ExposedMethod
    final int unicode___hash__() {
        return str___hash__();
    }

    protected PyObject pyget(int i) {
        return Py.makeCharacter(string.charAt(i), true);
    }

    @ExposedMethod
    final boolean unicode___contains__(PyObject o) {
        return str___contains__(o);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject unicode___mul__(PyObject o) {
        return str___mul__(o);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject unicode___rmul__(PyObject o) {
        return str___rmul__(o);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final PyObject unicode___add__(PyObject generic_other) {
        return str___add__(generic_other);
    }

    @ExposedMethod
    final PyObject unicode_lower() {
        return new PyUnicode(str_lower());
    }

    @ExposedMethod
    final PyObject unicode_upper() {
        return new PyUnicode(str_upper());
    }

    @ExposedMethod
    final PyObject unicode_title() {
        return new PyUnicode(str_title());
    }

    @ExposedMethod
    final PyObject unicode_swapcase() {
        return new PyUnicode(str_swapcase());
    }

    @ExposedMethod(defaults = "null")
    final PyObject unicode_strip(String sep) {
        return new PyUnicode(str_strip(sep));
    }
    
    @ExposedMethod(defaults = "null")
    final PyObject unicode_lstrip(String sep) {
        return new PyUnicode(str_lstrip(sep));
    }

    @ExposedMethod(defaults = "null")
    final PyObject unicode_rstrip(String sep) {
        return new PyUnicode(str_rstrip(sep));
    }


    @ExposedMethod(defaults = {"null", "-1"})
    final PyList unicode_split(String sep, int maxsplit) {
        return str_split(sep, maxsplit);
    }

    @ExposedMethod(defaults = "false")
    final PyList unicode_splitlines(boolean keepends) {
        return str_splitlines(keepends);
    }
    
    protected PyString fromSubstring(int begin, int end) {
        return new PyUnicode(string.substring(begin, end));
    }

    @ExposedMethod(defaults = {"0", "null"})
    final int unicode_index(String sub, int start, PyObject end) {
        return str_index(sub, start, end);
    }

    @ExposedMethod(defaults = {"0", "null"})
    final int unicode_rindex(String sub, int start, PyObject end) {
        return str_rindex(sub, start, end);
    }
    
    @ExposedMethod(defaults = {"0", "null"})
    final int unicode_count(String sub, int start, PyObject end) {
        return str_count(sub, start, end);
    }

    @ExposedMethod(defaults = {"0", "null"})
    final int unicode_find(String sub, int start, PyObject end) {
        return str_find(sub, start, end);
    }

    @ExposedMethod(defaults = {"0", "null"})
    final int unicode_rfind(String sub, int start, PyObject end) {
        return str_rfind(sub, start, end);
    }

    @ExposedMethod
    final PyObject unicode_ljust(int width) {
        return new PyUnicode(str_ljust(width));
    }

    @ExposedMethod
    final PyObject unicode_rjust(int width) {
        return new PyUnicode(str_rjust(width));
    }

    @ExposedMethod
    final PyObject unicode_center(int width) {
        return new PyUnicode(str_center(width));
    }

    @ExposedMethod
    final PyObject unicode_zfill(int width) {
        return new PyUnicode(str_zfill(width));
    }

    @ExposedMethod(defaults = "8")
    final PyObject unicode_expandtabs(int tabsize) {
        return new PyUnicode(str_expandtabs(tabsize));
    }

    @ExposedMethod
    final PyObject unicode_capitalize() {
        return new PyUnicode(str_capitalize());
    }

    @ExposedMethod(defaults = "null")
    final PyObject unicode_replace(PyObject oldPiece, PyObject newPiece, PyObject maxsplit) {
        return str_replace(oldPiece, newPiece, maxsplit);
    }

    @ExposedMethod
    final PyString unicode_join(PyObject seq) {
        return str_join(seq);
    }

    @ExposedMethod(defaults = {"0", "null"})
    final boolean unicode_startswith(String prefix, int start, PyObject end) {
        return str_startswith(prefix, start, end);
    }
    
    @ExposedMethod(defaults = {"0", "null"})
    final boolean unicode_endswith(String suffix, int start, PyObject end) {
        return str_endswith(suffix, start, end);
    }

    @ExposedMethod
    final PyObject unicode_translate(PyObject table) {
        String trans = _codecs.charmap_decode(string, "ignore", table, true).__getitem__(0).toString();
        return new PyUnicode(trans);
    }
    
    @ExposedMethod
    final boolean unicode_islower() {
        return str_islower();
    }

    @ExposedMethod
    final boolean unicode_isupper() {
        return str_isupper();
    }

    @ExposedMethod
    final boolean unicode_isalpha() {
        return str_isalpha();
    }

    @ExposedMethod
    final boolean unicode_isalnum() {
        return str_isalnum();
    }

    @ExposedMethod
    final boolean unicode_isdecimal() {
        return str_isdecimal();
    }

    @ExposedMethod
    final boolean unicode_isdigit() {
        return str_isdigit();
    }

    @ExposedMethod
    final boolean unicode_isnumeric() {
        return str_isnumeric();
    }

    @ExposedMethod
    final boolean unicode_istitle() {
        return str_istitle();
    }

    @ExposedMethod
    final boolean unicode_isspace() {
        return str_isspace();
    }

    @ExposedMethod
    final boolean unicode_isunicode() {
        return true;
    }

    @ExposedMethod(defaults = {"null", "null"})
    final String unicode_encode(String encoding, String errors) {
        return str_encode(encoding, errors);
    }

    @ExposedMethod(defaults = {"null", "null"})
    final PyObject unicode_decode(String encoding, String errors) {
        return str_decode(encoding, errors);
    }
    
    @ExposedMethod
    final PyTuple unicode___getnewargs__() {
        return new PyTuple(new PyUnicode(this.string));
    }

}
