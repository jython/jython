package org.python.modules._json;

import org.python.core.ArgParser;
import org.python.core.Py;
import org.python.core.PyDictionary;
import org.python.core.PyList;
import org.python.core.PyNewWrapper;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.core.PyTuple;
import org.python.core.PyType;
import org.python.core.codecs;
import org.python.core.Traverseproc;
import org.python.core.Visitproc;
import org.python.expose.ExposedGet;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedType;
import org.python.modules._io.OpenMode;
import org.python.modules._io.PyFileIO;
import org.python.modules._io.PyFileIODerived;


@ExposedType(name = "_json.scanner", base = PyObject.class)
public class Scanner extends PyObject implements Traverseproc {

    public static final PyType TYPE = PyType.fromClass(Scanner.class);

    @ExposedGet
    public final String __module__ = "_json";

    final String encoding;
    final boolean strict;
    final PyObject object_hook;
    final PyObject pairs_hook;
    final PyObject parse_float;
    final PyObject parse_int;
    final PyObject parse_constant;

    public Scanner(PyObject context) {
       this(TYPE, context); 
    }

    public Scanner(PyType subtype, PyObject context) {
        super(subtype);
        encoding = _castString(context.__getattr__("encoding"), "utf-8");
        strict = context.__getattr__("strict").__nonzero__();
        object_hook = context.__getattr__("object_hook");
        pairs_hook = context.__getattr__("object_pairs_hook");
        parse_float = context.__getattr__("parse_float");
        parse_int = context.__getattr__("parse_int");
        parse_constant = context.__getattr__("parse_constant");
    }

    public PyObject __call__(PyObject string, PyObject idx) {
        return _scan_once((PyString)string, idx.asInt());
    }

    private static final String[] newArgs = {"context"};

    @ExposedNew
    static PyObject Scanner___new__(PyNewWrapper new_, boolean init, PyType subtype,
            PyObject[] args, String[] keywords) {

        ArgParser ap = new ArgParser("scanner", args, keywords, newArgs, 1);
        PyObject context = ap.getPyObject(0);

        if (subtype == TYPE) {
            return new Scanner(context);
        } else {
            return new ScannerDerived(subtype, context);
        }
    }

    private static boolean IS_WHITESPACE(int c) {
        return (c == ' ') || (c == '\t') || (c == '\n') || (c == '\r');
    }

    private static String _castString(PyObject pystr, String defaultValue) {
        // Jython used to treat String as equivalent to PyString, or maybe PyUnicode, as
        // it made sense. We need to be more careful now! Insert this cast check as necessary
        // to ensure the appropriate compliance.
        if (pystr == Py.None) {
            return defaultValue;
        }
        if (!(pystr instanceof PyString)) {
            throw Py.TypeError("encoding is not a string");
        }
        String s = pystr.toString();
        return codecs.PyUnicode_EncodeASCII(s, s.length(), null);
    }

    static PyTuple valIndex(PyObject obj, int i) {
        return new PyTuple(obj, Py.newInteger(i));
    }

    public PyTuple _parse_object(PyString pystr, int idx) { // }, Py_ssize_t *next_idx_ptr) {
        /* Read a JSON object from PyString pystr.
        idx is the index of the first character after the opening curly brace.

        Returns a new PyTuple of a PyObject (usually a dict, but object_hook can change that)
        and the next_idx to the first character after
        the closing curly brace.
        */
        PyString str = pystr;
        int end_idx = pystr.__len__() - 1;
        PyList pairs = new PyList();
        PyObject key;
        PyObject val;

        /* skip whitespace after { */
        while (idx <= end_idx && IS_WHITESPACE(str.getInt(idx))) idx++;

        /* only loop if the object is non-empty */
        if (idx <= end_idx && str.getInt(idx) != '}') {
            while (idx <= end_idx) {
                /* read key */
                if (str.getInt(idx) != '"') {
                    _json.raise_errmsg("Expecting property name", pystr, idx);
                }
                PyTuple key_idx = _json.scanstring(pystr, idx + 1, encoding, strict);
                key = key_idx.pyget(0);
                idx = key_idx.pyget(1).asInt();

                /* skip whitespace between key and : delimiter, read :, skip whitespace */
                while (idx <= end_idx && IS_WHITESPACE(str.getInt(idx))) idx++;
                if (idx > end_idx || str.getInt(idx) != ':') {
                    _json.raise_errmsg("Expecting : delimiter", pystr, idx);
                }
                idx++;
                while (idx <= end_idx && IS_WHITESPACE(str.getInt(idx))) idx++;

                /* read any JSON data type */
                PyTuple val_idx = _scan_once(pystr, idx);
                val = val_idx.pyget(0);
                idx = val_idx.pyget(1).asInt();
                pairs.append(new PyTuple(key, val));

                /* skip whitespace before } or , */
                while (idx <= end_idx && IS_WHITESPACE(str.getInt(idx))) idx++;

                /* bail if the object is closed or we didn't get the , delimiter */
                if (idx > end_idx) break;
                if (str.getInt(idx) == '}') {
                    break;
                } else if (str.getInt(idx) != ',') {
                    _json.raise_errmsg("Expecting , delimiter", pystr, idx);
                }
                idx++;

                /* skip whitespace after , delimiter */
                while (idx <= end_idx && IS_WHITESPACE(str.getInt(idx))) idx++;
            }
        }
        /* verify that idx < end_idx, str[idx] should be '}' */
        if (idx > end_idx || str.getInt(idx) != '}') {
            _json.raise_errmsg("Expecting object", pystr, end_idx);
        }

        /* if pairs_hook is not None: rval = object_pairs_hook(pairs) */
        if (pairs_hook != Py.None) {
            return valIndex(pairs_hook.__call__(pairs), idx + 1);
        }

        PyObject rval = new PyDictionary();
        ((PyDictionary)rval).update(pairs);

        /* if object_hook is not None: rval = object_hook(rval) */
        if (object_hook != Py.None) {
            rval = object_hook.__call__(rval);
        }

        return valIndex(rval, idx + 1);
    }

    public PyTuple _parse_array(PyString pystr, int idx) {
        /* Read a JSON array from PyString pystr.


        Returns a new PyTuple of a PyList and next_idx (first character after
        the closing brace.)
        */
        PyString str = pystr;
        int end_idx = pystr.__len__() - 1;
        PyList rval = new PyList();

        /* skip whitespace after [ */
        while (idx <= end_idx && IS_WHITESPACE(str.getInt(idx))) idx++;

        /* only loop if the array is non-empty */
        if (idx <= end_idx && str.getInt(idx) != ']') {
            while (idx <= end_idx) {

                /* read any JSON term and de-tuplefy the (rval, idx) */
                PyTuple val_idx = _scan_once(pystr, idx);
                PyObject val = val_idx.pyget(0);
                idx = val_idx.pyget(1).asInt();
                rval.append(val);

                /* skip whitespace between term and , */
                while (idx <= end_idx && IS_WHITESPACE(str.getInt(idx))) idx++;

                /* bail if the array is closed or we didn't get the , delimiter */
                if (idx > end_idx) break;
                if (str.getInt(idx) == ']') {
                    break;
                } else if (str.getInt(idx) != ',') {
                    _json.raise_errmsg("Expecting , delimiter", pystr, idx);
                }
                idx++;

                /* skip whitespace after , */
                while (idx <= end_idx && IS_WHITESPACE(str.getInt(idx))) idx++;
            }
        }

        /* verify that idx < end_idx, str[idx] should be ']' */
        if (idx > end_idx || str.getInt(idx) != ']') {
            _json.raise_errmsg("Expecting object", pystr, end_idx);
        }
        return valIndex(rval, idx + 1);
    }


    public PyTuple _scan_once(PyString pystr, int idx) {
        /* Read one JSON term (of any kind) from PyString pystr.
        idx is the index of the first character of the term

        Returns a new PyTuple of a PyObject representation of the term along
        with the next_idx
        */
        PyString str = pystr;
        int length = pystr.__len__();
        if (idx >= length) {
            throw Py.StopIteration("");
        }
        switch (str.getInt(idx)) {
            case '"':
                /* string */
                return _json.scanstring(pystr, idx + 1, encoding, strict);
            case '{':
                /* object */
                return _parse_object(pystr, idx + 1);
            case '[':
                /* array */
                return _parse_array(pystr, idx + 1);
            case 'n':
                /* null */
                if ((idx + 3 < length) && str.getInt(idx + 1) == 'u' && str.getInt(idx + 2) == 'l' && str.getInt(idx + 3) == 'l') {
                    return valIndex(Py.None, idx + 4);
                }
                break;
            case 't':
                /* true */
                if ((idx + 3 < length) && str.getInt(idx + 1) == 'r' && str.getInt(idx + 2) == 'u' && str.getInt(idx + 3) == 'e') {
                    return valIndex(Py.True, idx + 4);
                }
                break;
            case 'f':
                /* false */
                if ((idx + 4 < length) && str.getInt(idx + 1) == 'a' && str.getInt(idx + 2) == 'l' && str.getInt(idx + 3) == 's' && str.getInt(idx + 4) == 'e') {
                    return valIndex(Py.False, idx + 5);
                }
                break;
            case 'N':
                /* NaN */
                if ((idx + 2 < length) && str.getInt(idx + 1) == 'a' && str.getInt(idx + 2) == 'N') {
                    return _parse_constant("NaN", idx + 3);
                }
                break;
            case 'I':
                /* Infinity */
                if ((idx + 7 < length) && str.getInt(idx + 1) == 'n' && str.getInt(idx + 2) == 'f' && str.getInt(idx + 3) == 'i' && str.getInt(idx + 4) == 'n' && str.getInt(idx + 5) == 'i' && str.getInt(idx + 6) == 't' && str.getInt(idx + 7) == 'y') {
                    return _parse_constant("Infinity", idx + 8);
                }
                break;
            case '-':
                /* -Infinity */
                if ((idx + 8 < length) && str.getInt(idx + 1) == 'I' && str.getInt(idx + 2) == 'n' && str.getInt(idx + 3) == 'f' && str.getInt(idx + 4) == 'i' && str.getInt(idx + 5) == 'n' && str.getInt(idx + 6) == 'i' && str.getInt(idx + 7) == 't' && str.getInt(idx + 8) == 'y') {
                    return _parse_constant("-Infinity", idx + 9);
                }
                break;
        }
        /* Didn't find a string, object, array, or named constant. Look for a number. */
        return _match_number(pystr, idx);
    }

    public PyTuple _parse_constant(String constant, int idx) {
        return valIndex(parse_constant.__call__(Py.newString(constant)), idx);
    }

    public PyTuple _match_number(PyString pystr, int start) {
        /* Read a JSON number from PyString pystr.
        idx is the index of the first character of the number

        Returns a new PyObject representation of that number:
        PyInt, PyLong, or PyFloat.
        May return other types if parse_int or parse_float are set
        along with index to the first character after
        the number.
        */
        PyString str = pystr;
        int end_idx = pystr.__len__() - 1;
        int idx = start;
        boolean is_float = false;

        /* read a sign if it's there, make sure it's not the end of the string */
        if (str.getInt(idx) == '-') {
            idx++;
            if (idx > end_idx) {
                throw Py.StopIteration("");
            }
        }

        /* read as many integer digits as we find as long as it doesn't start with 0 */
        if (str.getInt(idx) >= '1' && str.getInt(idx) <= '9') {
            idx++;
            while (idx <= end_idx && str.getInt(idx) >= '0' && str.getInt(idx) <= '9') idx++;
        }
        /* if it starts with 0 we only expect one integer digit */
        else if (str.getInt(idx) == '0') {
            idx++;
        }
        /* no integer digits, error */
        else {
            throw Py.StopIteration("");
        }

        /* if the next char is '.' followed by a digit then read all float digits */
        if (idx < end_idx && str.getInt(idx) == '.' && str.getInt(idx + 1) >= '0' && str.getInt(idx + 1) <= '9') {
            is_float = true;
            idx += 2;
            while (idx <= end_idx && str.getInt(idx) >= '0' && str.getInt(idx) <= '9') idx++;
        }

        /* if the next char is 'e' or 'E' then maybe read the exponent (or backtrack) */
        if (idx < end_idx && (str.getInt(idx) == 'e' || str.getInt(idx) == 'E')) {

            /* save the index of the 'e' or 'E' just in case we need to backtrack */
            int e_start = idx;
            idx++;

            /* read an exponent sign if present */
            if (idx < end_idx && (str.getInt(idx) == '-' || str.getInt(idx) == '+')) idx++;

            /* read all digits */
            while (idx <= end_idx && str.getInt(idx) >= '0' && str.getInt(idx) <= '9') idx++;

            /* if we got a digit, then parse as float. if not, backtrack */
            if (str.getInt(idx - 1) >= '0' && str.getInt(idx - 1) <= '9') {
                is_float = true;
            } else {
                idx = e_start;
            }
        }

        /* copy the section we determined to be a number */
        PyString numstr = (PyString) str.__getslice__(Py.newInteger(start), Py.newInteger(idx));
        if (is_float) {
            /* parse as a float using a fast path if available, otherwise call user defined method */
            return valIndex(parse_float.__call__(numstr), idx);
        } else {
            /* parse as an int using a fast path if available, otherwise call user defined method */
            return valIndex(parse_int.__call__(numstr), idx);
        }
    }


    /* Traverseproc implementation */
    @Override
    public int traverse(Visitproc visit, Object arg) {
        int retVal;
        if (object_hook != null) {
            retVal = visit.visit(object_hook, arg);
            if (retVal != 0) {
                return retVal;
            }
        }
        if (pairs_hook != null) {
            retVal = visit.visit(pairs_hook, arg);
            if (retVal != 0) {
                return retVal;
            }
        }
        if (parse_float != null) {
            retVal = visit.visit(parse_float, arg);
            if (retVal != 0) {
                return retVal;
            }
        }
        if (parse_int != null) {
            retVal = visit.visit(parse_int, arg);
            if (retVal != 0) {
                return retVal;
            }
        }
        return parse_constant != null ? visit.visit(parse_constant, arg) : 0;
    }

    @Override
    public boolean refersDirectlyTo(PyObject ob) {
        return ob != null && (ob == object_hook || ob == pairs_hook
            || ob == parse_float || ob == parse_int || ob == parse_constant);
    }
}
