/* Copyright (c) Jython Developers */
package org.python.modules._json;

import org.python.core.ArgParser;
import org.python.core.ClassDictInit;
import org.python.core.Py;
import org.python.core.PyBuiltinFunctionNarrow;
import org.python.core.PyList;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.core.PyTuple;
import org.python.core.PyUnicode;
import org.python.core.codecs;
import org.python.core.Untraversable;
import org.python.expose.ExposedGet;

import java.util.Iterator;

/**
 * This module is a nearly exact line by line port of _json.c to Java. Names and comments  are retained
 * to make it easy to follow, but classes and methods are modified to following Java calling conventions.
 *
 * (Retained comments use the standard commenting convention for C.)
 */
public class _json implements ClassDictInit {

    public static final PyString __doc__ = new PyString("Port of _json C module.");
    public static final PyObject module = Py.newString("_json");

    public static void classDictInit(PyObject dict) {
        dict.__setitem__("__name__", new PyString("_json"));
        dict.__setitem__("__doc__", __doc__);
        dict.__setitem__("encode_basestring_ascii", new EncodeBasestringAsciiFunction());
        dict.__setitem__("make_encoder", Encoder.TYPE);
        dict.__setitem__("make_scanner", Scanner.TYPE);
        dict.__setitem__("scanstring", new ScanstringFunction());
        dict.__setitem__("__module__", new PyString("_json"));

        // ensure __module__ is set properly in these modules,
        // based on how the module name lookups are chained
        Encoder.TYPE.setName("_json.Encoder");
        Scanner.TYPE.setName("_json.Scanner");

        // Hide from Python
        dict.__setitem__("classDictInit", null);
    }

    private static PyObject errmsg_fn;

    private static synchronized PyObject get_errmsg_fn() {
        if (errmsg_fn == null) {
            PyObject json = org.python.core.__builtin__.__import__("json");
            if (json != null) {
                PyObject decoder = json.__findattr__("decoder");
                if (decoder != null) {
                    errmsg_fn = decoder.__findattr__("errmsg");
                }
            }
        }
        return errmsg_fn;
    }

    static void raise_errmsg(String msg, PyObject s) {
        raise_errmsg(msg, s, Py.None, Py.None);
    }

    static void raise_errmsg(String msg, PyObject s, int pos) {
        raise_errmsg(msg, s, Py.newInteger(pos), Py.None);
    }

    static void raise_errmsg(String msg, PyObject s, PyObject pos, PyObject end) {
        /* Use the Python function json.decoder.errmsg to raise a nice
        looking ValueError exception */
        final PyObject errmsg_fn = get_errmsg_fn();
        if (errmsg_fn != null) {
            throw Py.ValueError(errmsg_fn.__call__(Py.newString(msg), s, pos, end).asString());
        } else {
            throw Py.ValueError(msg);
        }
    }

    @Untraversable
    static class ScanstringFunction extends PyBuiltinFunctionNarrow {
        ScanstringFunction() {
            super("scanstring", 2, 4, "scanstring");
        }

        @Override
        public PyObject getModule() {
            return module;
        }


        @Override
        public PyObject __call__(PyObject s, PyObject end) {
            return __call__(s, end, new PyString("utf-8"), Py.True);
        }

        @Override
        public PyObject __call__(PyObject s, PyObject end, PyObject encoding) {
            return __call__(s, end, encoding, Py.True);
        }

        @Override
        public PyObject __call__(PyObject[] args, String[] kwds) {
            ArgParser ap = new ArgParser("scanstring", args, kwds, new String[]{
                    "s", "end", "encoding", "strict"}, 2);
            return __call__(
                    ap.getPyObject(0),
                    ap.getPyObject(1),
                    ap.getPyObject(2, new PyString("utf-8")),
                    ap.getPyObject(3, Py.True));
        }

        @Override
        public PyObject __call__(PyObject s, PyObject end, PyObject encoding, PyObject strict) {
            // but rethrow in case it does work - see the test case for issue 362
            int end_idx = end.asIndex(Py.OverflowError);
            boolean is_strict = strict.__nonzero__();
            if (s instanceof PyString) {
                return scanstring((PyString) s, end_idx,
                        encoding == Py.None ? null : encoding.toString(), is_strict);
            } else {
                throw Py.TypeError(String.format(
                        "first argument must be a string, not %.80s",
                        s.getType().fastGetName()));
            }
        }

    }

    static PyTuple scanstring(PyString pystr, int end, String encoding, boolean strict) {
        int len = pystr.__len__();
        int begin = end - 1;
        if (end < 0 || len <= end) {
            throw Py.ValueError("end is out of bounds");
        }
        int next;
        final PyList chunks = new PyList();
        while (true) {
            /* Find the end of the string or the next escape */
            int c = 0;

            for (next = end; next < len; next++) {
                c = pystr.getInt(next);
                if (c == '"' || c == '\\') {
                    break;
                } else if (strict && c <= 0x1f) {
                    raise_errmsg("Invalid control character at", pystr, next);
                }
            }
            if (!(c == '"' || c == '\\')) {
                raise_errmsg("Unterminated string starting at", pystr, begin);
            }

            /* Pick up this chunk if it's not zero length */
            if (next != end) {
                PyString strchunk = (PyString) pystr.__getslice__(Py.newInteger(end), Py.newInteger(next));
                if (strchunk instanceof PyUnicode) {
                    chunks.append(strchunk);
                } else {
                    chunks.append(codecs.decode(strchunk, encoding, null));
                }
            }
            next++;
            if (c == '"') {
                end = next;
                break;
            }
            if (next == len) {
                raise_errmsg("Unterminated string starting at", pystr, begin);
            }
            c = pystr.getInt(next);
            if (c != 'u') {
                /* Non-unicode backslash escapes */
                end = next + 1;
                switch (c) {
                    case '"':
                        break;
                    case '\\':
                        break;
                    case '/':
                        break;
                    case 'b':
                        c = '\b';
                        break;
                    case 'f':
                        c = '\f';
                        break;
                    case 'n':
                        c = '\n';
                        break;
                    case 'r':
                        c = '\r';
                        break;
                    case 't':
                        c = '\t';
                        break;
                    default:
                        c = 0;
                }
                if (c == 0) {
                    raise_errmsg("Invalid \\escape", pystr, end - 2);
                }
            } else {
                c = 0;
                next++;
                end = next + 4;
                if (end >= len) {
                    raise_errmsg("Invalid \\uXXXX escape", pystr, next - 1);
                }
                /* Decode 4 hex digits */
                for (; next < end; next++) {
                    int digit = pystr.getInt(next);
                    c <<= 4;
                    switch (digit) {
                        case '0':
                        case '1':
                        case '2':
                        case '3':
                        case '4':
                        case '5':
                        case '6':
                        case '7':
                        case '8':
                        case '9':
                            c |= (digit - '0');
                            break;
                        case 'a':
                        case 'b':
                        case 'c':
                        case 'd':
                        case 'e':
                        case 'f':
                            c |= (digit - 'a' + 10);
                            break;
                        case 'A':
                        case 'B':
                        case 'C':
                        case 'D':
                        case 'E':
                        case 'F':
                            c |= (digit - 'A' + 10);
                            break;
                        default:
                            raise_errmsg("Invalid \\uXXXX escape", pystr, end - 5);
                    }
                }
                /* Surrogate pair */
                if ((c & 0xfc00) == 0xd800) {
                    int c2 = 0;
                    if (end + 6 >= len) {
                        raise_errmsg("Unpaired high surrogate", pystr, end - 5);
                    }
                    if (pystr.getInt(next++) != '\\' || pystr.getInt(next++) != 'u') {
                        raise_errmsg("Unpaired high surrogate", pystr, end - 5);
                    }
                    end += 6;
                    /* Decode 4 hex digits */
                    for (; next < end; next++) {
                        int digit = pystr.getInt(next);
                        c2 <<= 4;
                        switch (digit) {
                            case '0':
                            case '1':
                            case '2':
                            case '3':
                            case '4':
                            case '5':
                            case '6':
                            case '7':
                            case '8':
                            case '9':
                                c2 |= (digit - '0');
                                break;
                            case 'a':
                            case 'b':
                            case 'c':
                            case 'd':
                            case 'e':
                            case 'f':
                                c2 |= (digit - 'a' + 10);
                                break;
                            case 'A':
                            case 'B':
                            case 'C':
                            case 'D':
                            case 'E':
                            case 'F':
                                c2 |= (digit - 'A' + 10);
                                break;
                            default:
                                raise_errmsg("Invalid \\uXXXX escape", pystr, end - 5);
                        }
                    }
                    if ((c2 & 0xfc00) != 0xdc00) {
                        raise_errmsg("Unpaired high surrogate", pystr, end - 5);
                    }
                    c = 0x10000 + (((c - 0xd800) << 10) | (c2 - 0xdc00));
                } else if ((c & 0xfc00) == 0xdc00) {
                    raise_errmsg("Unpaired low surrogate", pystr, end - 5);
                }
            }
            chunks.append(new PyUnicode(c));
        }

        return new PyTuple(Py.EmptyUnicode.join(chunks), Py.newInteger(end));
    }

    @Untraversable
    static class EncodeBasestringAsciiFunction extends PyBuiltinFunctionNarrow {
        EncodeBasestringAsciiFunction() {
            super("encode_basestring_ascii", 1, 1, "encode_basestring_ascii");
        }

        @Override
        public PyObject getModule() {
            return module;
        }

        @Override
        public PyObject __call__(PyObject pystr) {
            return encode_basestring_ascii(pystr);
        }
    }

    static PyString encode_basestring_ascii(PyObject pystr) {
        if (pystr instanceof PyUnicode) {
            return ascii_escape((PyUnicode) pystr);
        } else if (pystr instanceof PyString) {
            return ascii_escape((PyString) pystr);
        } else {
            throw Py.TypeError(String.format(
                    "first argument must be a string, not %.80s",
                    pystr.getType().fastGetName()));
        }
    }

    private static PyString ascii_escape(PyUnicode pystr) {
        StringBuilder rval = new StringBuilder(pystr.__len__());
        rval.append("\"");
        for (Iterator<Integer> iter = pystr.newSubsequenceIterator(); iter.hasNext(); ) {
            _write_char(rval, iter.next());
        }
        rval.append("\"");
        return new PyString(rval.toString());
    }

    private static PyString ascii_escape(PyString pystr) {
        int len = pystr.__len__();
        String s = pystr.getString();
        StringBuilder rval = new StringBuilder(len);
        rval.append("\"");
        for (int i = 0; i < len; i++) {
            int c = s.charAt(i);
            if (c > 127) {
                return ascii_escape(new PyUnicode(codecs.PyUnicode_DecodeUTF8(s, null)));
            }
            _write_char(rval, c);
        }
        rval.append("\"");
        return new PyString(rval.toString());
    }

    private static void _write_char(StringBuilder builder, int c) {
        /* Escape unicode code point c to ASCII escape sequences
        in char *output. output must have at least 12 bytes unused to
        accommodate an escaped surrogate pair "\ u XXXX \ u XXXX" */
        if (c >= ' ' && c <= '~' && c != '\\' & c != '"') {
            builder.append((char) c);
        } else {
            _ascii_escape_char(builder, c);
        }
    }

    private static void _write_hexchar(StringBuilder builder, int c) {
        builder.append("0123456789abcdef".charAt(c & 0xf));
    }

    private static void _ascii_escape_char(StringBuilder builder, int c) {
        builder.append('\\');
        switch (c) {
            case '\\':
                builder.append((char) c);
                break;
            case '"':
                builder.append((char) c);
                break;
            case '\b':
                builder.append('b');
                break;
            case '\f':
                builder.append('f');
                break;
            case '\n':
                builder.append('n');
                break;
            case '\r':
                builder.append('r');
                break;
            case '\t':
                builder.append('t');
                break;
            default:
                if (c >= 0x10000) {
                /* UTF-16 surrogate pair */
                    int v = c - 0x10000;
                    c = 0xd800 | ((v >> 10) & 0x3ff);
                    builder.append('u');
                    _write_hexchar(builder, c >> 12);
                    _write_hexchar(builder, c >> 8);
                    _write_hexchar(builder, c >> 4);
                    _write_hexchar(builder, c);
                    c = 0xdc00 | (v & 0x3ff);
                    builder.append('\\');
                }
                builder.append('u');
                _write_hexchar(builder, c >> 12);
                _write_hexchar(builder, c >> 8);
                _write_hexchar(builder, c >> 4);
                _write_hexchar(builder, c);
        }
    }
}
