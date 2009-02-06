package org.python.modules;

import java.math.BigInteger;
import org.python.core.BaseSet;
import org.python.core.ClassDictInit;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.core.Py;
import org.python.core.PyBytecode;
import org.python.core.PyComplex;
import org.python.core.PyDictionary;
import org.python.core.PyFloat;
import org.python.core.PyFrozenSet;
import org.python.core.PyInteger;
import org.python.core.PyList;
import org.python.core.PyLong;
import org.python.core.PySet;
import org.python.core.PyTuple;
import org.python.core.PyUnicode;

public class _marshal implements ClassDictInit {

    public static void classDictInit(PyObject dict) {
        dict.__setitem__("__name__", Py.newString("_marshal"));
    }
    private final static char TYPE_NULL = '0';
    private final static char TYPE_NONE = 'N';
    private final static char TYPE_FALSE = 'F';
    private final static char TYPE_TRUE = 'T';
    private final static char TYPE_STOPITER = 'S';
    private final static char TYPE_ELLIPSIS = '.';
    private final static char TYPE_INT = 'i';
    private final static char TYPE_INT64 = 'I';
    private final static char TYPE_FLOAT = 'f';
    private final static char TYPE_BINARY_FLOAT = 'g';
    private final static char TYPE_COMPLEX = 'x';
    private final static char TYPE_BINARY_COMPLEX = 'y';
    private final static char TYPE_LONG = 'l';
    private final static char TYPE_STRING = 's';
    private final static char TYPE_INTERNED = 't';
    private final static char TYPE_STRINGREF = 'R';
    private final static char TYPE_TUPLE = '(';
    private final static char TYPE_LIST = '[';
    private final static char TYPE_DICT = '{';
    private final static char TYPE_CODE = 'c';
    private final static char TYPE_UNICODE = 'u';
    private final static char TYPE_UNKNOWN = '?';
    private final static char TYPE_SET = '<';
    private final static char TYPE_FROZENSET = '>';
    private final static int MAX_MARSHAL_STACK_DEPTH = 2000;
    private final static int CURRENT_VERSION = 2;

    public static class Marshaller extends PyObject {

        private final PyIOFile file;
        private final int version;

        public Marshaller(PyObject file) {
            this(file, CURRENT_VERSION);
        }

        public Marshaller(PyObject file, int version) {
            this.file = PyIOFileFactory.createIOFile(file);
            this.version = version;
        }
        private boolean debug = false;

        public void _debug() {
            debug = true;
        }

        public void dump(PyObject obj) {
            write_object(obj, 0);
        }

        private void write_byte(char c) {
            if (debug) {
                System.err.print("[" + (int) c + "]");
            }
            file.write(c);
        }

        private void write_string(String s) {
            file.write(s);
        }

        private void write_strings(String[] some_strings, int depth) {
            PyObject items[] = new PyObject[some_strings.length];
            for (int i = 0; i < some_strings.length; i++) {
                items[i] = Py.newString(some_strings[i]);
            }
            write_object(new PyTuple(items), depth + 1);
        }

        private void write_short(short x) {
            write_byte((char) (x & 0xff));
            write_byte((char) ((x >> 8) & 0xff));
        }

        private void write_int(int x) {
            write_byte((char) (x & 0xff));
            write_byte((char) ((x >> 8) & 0xff));
            write_byte((char) ((x >> 16) & 0xff));
            write_byte((char) ((x >> 24) & 0xff));
        }

        private void write_long64(long x) {
            write_int((int) (x & 0xffffffff));
            write_int((int) ((x >> 32) & 0xffffffff));
        }

        // writes output in 15 bit "digits"
        private void write_long(BigInteger x) {
            int sign = x.signum();
            if (sign < 0) {
                x = x.negate();
            }
            int num_bits = x.bitLength();
            int num_digits = num_bits / 15 + (num_bits % 15 == 0 ? 0 : 1);
            write_int(sign < 0 ? -num_digits : num_digits);
            BigInteger mask = BigInteger.valueOf(0x7FFF);
            for (int i = 0; i < num_digits; i++) {
                write_short(x.and(mask).shortValue());
                x = x.shiftRight(15);
            }
        }

        private void write_float(PyFloat f) {
            write_string(f.__repr__().toString());
        }

        private void write_binary_float(PyFloat f) {
            write_long64(Double.doubleToLongBits(f.getValue()));
        }

        private void write_object(PyObject v, int depth) {
            if (depth >= MAX_MARSHAL_STACK_DEPTH) {
                throw Py.ValueError("Maximum marshal stack depth"); // XXX - fix this exception
            } else if (v == null) {
                write_byte(TYPE_NULL);
            } else if (v == Py.None) {
                write_byte(TYPE_NONE);
            } else if (v == Py.StopIteration) {
                write_byte(TYPE_STOPITER);
            } else if (v == Py.Ellipsis) {
                write_byte(TYPE_ELLIPSIS);
            } else if (v == Py.False) {
                write_byte(TYPE_FALSE);
            } else if (v == Py.True) {
                write_byte(TYPE_TRUE);
            } else if (v instanceof PyInteger) {
                write_byte(TYPE_INT);
                write_int(((PyInteger) v).asInt());
            } else if (v instanceof PyLong) {
                write_byte(TYPE_LONG);
                write_long(((PyLong) v).getValue());
            } else if (v instanceof PyFloat) {
                if (version == CURRENT_VERSION) {
                    write_byte(TYPE_BINARY_FLOAT);
                    write_binary_float((PyFloat) v);
                } else {
                    write_byte(TYPE_FLOAT);
                    write_float((PyFloat) v);
                }
            } else if (v instanceof PyComplex) {
                PyComplex x = (PyComplex) v;
                if (version == CURRENT_VERSION) {
                    write_byte(TYPE_BINARY_COMPLEX);
                    write_binary_float(x.getReal());
                    write_binary_float(x.getImag());
                } else {
                    write_byte(TYPE_COMPLEX);
                    write_float(x.getReal());
                    write_float(x.getImag());
                }
            } else if (v instanceof PyUnicode) {
                write_byte(TYPE_UNICODE);
                String buffer = ((PyUnicode) v).encode("utf-8").toString();
                write_int(buffer.length());
                write_string(buffer);
            } else if (v instanceof PyString) {
                // ignore interning
                write_byte(TYPE_STRING);
                write_int(v.__len__());
                write_string(v.toString());
            } else if (v instanceof PyTuple) {
                write_byte(TYPE_TUPLE);
                PyTuple t = (PyTuple) v;
                int n = t.__len__();
                write_int(n);
                for (int i = 0; i < n; i++) {
                    write_object(t.__getitem__(i), depth + 1);
                }
            } else if (v instanceof PyList) {
                write_byte(TYPE_LIST);
                PyList list = (PyList) v;
                int n = list.__len__();
                write_int(n);
                for (int i = 0; i < n; i++) {
                    write_object(list.__getitem__(i), depth + 1);
                }
            } else if (v instanceof PyDictionary) {
                write_byte(TYPE_DICT);
                PyDictionary dict = (PyDictionary) v;
                for (PyObject item : dict.iteritems().asIterable()) {
                    PyTuple pair = (PyTuple) item;
                    write_object(pair.__getitem__(0), depth + 1);
                    write_object(pair.__getitem__(1), depth + 1);
                }
                write_object(null, depth + 1);
            } else if (v instanceof BaseSet) {
                if (v instanceof PySet) {
                    write_byte(TYPE_SET);
                } else {
                    write_byte(TYPE_FROZENSET);
                }
                int n = v.__len__();
                write_int(n);
                BaseSet set = (BaseSet) v;
                for (PyObject item : set.asIterable()) {
                    write_object(item, depth + 1);
                }
            } else if (v instanceof PyBytecode) {
                PyBytecode code = (PyBytecode) v;
                write_byte(TYPE_CODE);
                write_int(code.co_argcount);
                write_int(code.co_nlocals);
                write_int(code.co_stacksize);
                write_int(code.co_flags);
                write_object(Py.newString(new String(code.co_code)), depth + 1);
                write_object(new PyTuple(code.co_consts), depth + 1);
                write_strings(code.co_names, depth + 1);
                write_strings(code.co_varnames, depth + 1);
                write_strings(code.co_freevars, depth + 1);
                write_strings(code.co_cellvars, depth + 1);
                write_object(Py.newString(code.co_name), depth + 1);
                write_int(code.co_firstlineno);
                write_object(new PyTuple(code.co_lnotab), depth + 1);
            } else {
                write_byte(TYPE_UNKNOWN);
            }

            depth--;

        }
    }

    public static class Unmarshaller extends PyObject {

        private final PyIOFile file;
        private final PyList strings = new PyList();
        private final int version;
        int depth = 0;

        public Unmarshaller(PyObject file) {
            this(file, CURRENT_VERSION);
        }

        public Unmarshaller(PyObject file, int version) {
            this.file = PyIOFileFactory.createIOFile(file);
            this.version = version;
        }
        private boolean debug = false;

        public void _debug() {
            debug = true;
        }

        public PyObject load() {
            try {
                PyObject obj = read_object(0);
                if (obj == null) {
                    throw Py.TypeError("NULL object in marshal data");
                }
                return obj;
            } catch (StringIndexOutOfBoundsException e) {
                // convert from our PyIOFile abstraction to what marshal in CPython returns
                // (although it's really just looking for no bombing)
                throw Py.EOFError("EOF read where object expected");
            }
        }

        private int read_byte() {
            int b = file.read(1).charAt(0);
            if (debug) {
                System.err.print("[" + b + "]");
            }
            return b;
        }

        private String read_string(int n) {
            return file.read(n);
        }

        private int read_short() {
            int x = read_byte();
            x |= read_byte() << 8;
            return x;
        }

        private int read_int() { // cpython calls this r_long
            int x = read_byte();
            x |= read_byte() << 8;
            x |= read_byte() << 16;
            x |= read_byte() << 24;
            return x;
        }

        private long read_long64() { // cpython calls this r_long64
            long lo4 = read_int();
            long hi4 = read_int();
            long x = (hi4 << 32) | (lo4 & 0xFFFFFFFFL);
            return x;
        }

        private BigInteger read_long() {
            int size = read_int();
            int sign = 1;
            if (size < 0) {
                sign = -1;
                size = -size;
            }
            BigInteger result = BigInteger.ZERO;
            for (int i = 0; i < size; i++) {
                String digits = String.valueOf(read_short());
                result = result.or(new BigInteger(digits).shiftLeft(i * 15));
            }
            if (sign < 0) {
                result = result.negate();
            }
            return result;
        }

        private double read_float() {
            int size = read_byte();
            return Py.newString(read_string(size)).atof();
        }

        private double read_binary_float() {
            return Double.longBitsToDouble(read_long64());
        }

        private PyObject read_object_notnull(int depth) {
            PyObject v = read_object(depth);
            if (v == null) {
                throw Py.ValueError("bad marshal data");
            }
            return v;
        }

        private String[] read_strings(int depth) {
            PyTuple t = (PyTuple) read_object_notnull(depth);
            String some_strings[] = new String[t.__len__()];
            int i = 0;
            for (PyObject item : t.asIterable()) {
                some_strings[i++] = item.toString();
            }
            return some_strings;
        }

        private PyObject read_object(int depth) {
            if (depth >= MAX_MARSHAL_STACK_DEPTH) {
                throw Py.ValueError("Maximum marshal stack depth"); // XXX - fix this exception
            }
            int type = read_byte();
            switch (type) {

                case TYPE_NULL:
                    return null;

                case TYPE_NONE:
                    return Py.None;

                case TYPE_STOPITER:
                    return Py.StopIteration;

                case TYPE_ELLIPSIS:
                    return Py.Ellipsis;

                case TYPE_FALSE:
                    return Py.False;

                case TYPE_TRUE:
                    return Py.True;

                case TYPE_INT:
                    return Py.newInteger(read_int());

                case TYPE_INT64:
                    return Py.newInteger(read_long64());

                case TYPE_LONG: {
                    return Py.newLong(read_long());
                }

                case TYPE_FLOAT:
                    return Py.newFloat(read_float());

                case TYPE_BINARY_FLOAT:
                    return Py.newFloat(read_binary_float());

                case TYPE_COMPLEX: {
                    double real = read_float();
                    double imag = read_float();
                    return new PyComplex(real, imag);
                }

                case TYPE_BINARY_COMPLEX: {
                    double real = read_binary_float();
                    double imag = read_binary_float();
                    return new PyComplex(real, imag);
                }

                case TYPE_INTERNED:
                case TYPE_STRING: {
                    int size = read_int();
                    String s = read_string(size);
                    if (type == TYPE_INTERNED) {
                        s.intern(); // do we really honor like this?
                        PyString pys = PyString.fromInterned(s);
                        strings.append(pys);
                        return pys;
                    } else {
                        return Py.newString(s);
                    }
                }

                case TYPE_STRINGREF: {
                    int i = read_int();
                    return strings.__getitem__(i);
                }

                case TYPE_UNICODE: {
                    int n = read_int();
                    PyString buffer = Py.newString(read_string(n));
                    return buffer.decode("utf-8");
                }

                case TYPE_TUPLE: {
                    int n = read_int();
                    if (n < 0) {
                        throw Py.ValueError("bad marshal data");
                    }
                    PyObject items[] = new PyObject[n];
                    for (int i = 0; i < n; i++) {
                        items[i] = read_object_notnull(depth + 1);
                    }
                    return new PyTuple(items);
                }

                case TYPE_LIST: {
                    int n = read_int();
                    if (n < 0) {
                        throw Py.ValueError("bad marshal data");
                    }
                    PyObject items[] = new PyObject[n];
                    for (int i = 0; i < n; i++) {
                        items[i] = read_object_notnull(depth + 1);
                    }
                    return new PyList(items);
                }

                case TYPE_DICT: {
                    PyDictionary d = new PyDictionary();
                    while (true) {
                        PyObject key = read_object(depth + 1);
                        if (key == null) {
                            break;
                        }
                        PyObject value = read_object(depth + 1);
                        if (value != null) {
                            d.__setitem__(key, value);
                        }
                    }
                    return d;
                }

                case TYPE_SET:
                case TYPE_FROZENSET: {
                    int n = read_int();
                    PyObject items[] = new PyObject[n];
                    for (int i = 0; i < n; i++) {
                        items[i] = read_object(depth + 1);
                    }
                    PyTuple v = new PyTuple(items);
                    if (type == TYPE_SET) {
                        return new PySet(v);
                    } else {
                        return new PyFrozenSet(v);
                    }
                }


                case TYPE_CODE: {
                    // XXX - support restricted execution mode? not certain if this is just legacy
                    int argcount = read_int();
                    int nlocals = read_int();
                    int stacksize = read_int();
                    int flags = read_int();
                    String code = read_object_notnull(depth + 1).toString();
                    PyObject consts[] = ((PyTuple) read_object_notnull(depth + 1)).getArray();
                    String names[] = read_strings(depth + 1);
                    String varnames[] = read_strings(depth + 1);
                    String freevars[] = read_strings(depth + 1);
                    String cellvars[] = read_strings(depth + 1);
                    String filename = read_object_notnull(depth + 1).toString();
                    String name = read_object_notnull(depth + 1).toString();
                    int firstlineno = read_int();
                    String lnotab = read_object_notnull(depth + 1).toString();

                    return new PyBytecode(
                            argcount, nlocals, stacksize, flags,
                            code, consts, names, varnames,
                            filename, name, firstlineno, lnotab,
                            cellvars, freevars);
                }

                default:
                    throw Py.ValueError("bad marshal data");
            }
        }
    }
}

