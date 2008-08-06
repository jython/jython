/* Copyright (c) Jython Developers */
package org.python.modules._csv;

import org.python.core.ArgParser;
import org.python.core.Py;
import org.python.core.PyBaseString;
import org.python.core.PyInteger;
import org.python.core.PyNewWrapper;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.core.PyType;
import org.python.expose.ExposedDelete;
import org.python.expose.ExposedGet;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedSet;
import org.python.expose.ExposedType;

/**
 * The Python CSV Dialect type.
 */
@ExposedType(name = "_csv.Dialect")
public class PyDialect extends PyObject {

    public static final PyType TYPE = PyType.fromClass(PyDialect.class);

    public PyString __doc__ = Py.newString(
        "CSV dialect\n" +
        "\n" +
        "The Dialect type records CSV parsing and generation options.\n");

    /** Whether " is represented by "". */
    @ExposedGet
    public boolean doublequote;

    /** Field separator. */
    @ExposedGet
    public char delimiter;

    /** Quote character. */
    public char quotechar;

    /** Escape character. */
    public char escapechar;

    /** Ignore spaces following delimiter? */
    @ExposedGet
    public boolean skipinitialspace;

    /** String to write between records. */
    @ExposedGet
    public String lineterminator;

    /** Style of quoting to write. */
    public QuoteStyle quoting;

    /** Whether an exception is raised on bad CSV. */
    @ExposedGet
    public boolean strict;

    public PyDialect() {
        super(TYPE);
    }

    public PyDialect(PyType subType) {
        super(subType);
    }

    @ExposedNew
    final static PyObject Dialect___new__(PyNewWrapper new_, boolean init, PyType subtype,
                                          PyObject[] args, String[] keywords) {
        ArgParser ap = new ArgParser("__new__", args, keywords,
                                     new String[] {"dialect", "delimiter", "doublequote",
                                                   "escapechar", "lineterminator", "quotechar",
                                                   "quoting", "skipinitialspace", "strict"});
        PyObject dialect = ap.getPyObject(0, null);
        PyObject delimiter = ap.getPyObject(1, null);
        PyObject doublequote = ap.getPyObject(2, null);
        PyObject escapechar = ap.getPyObject(3, null);
        PyObject lineterminator = ap.getPyObject(4, null);
        PyObject quotechar = ap.getPyObject(5, null);
        PyObject quoting = ap.getPyObject(6, null);
        PyObject skipinitialspace = ap.getPyObject(7, null);
        PyObject strict = ap.getPyObject(8, null);

        if (dialect instanceof PyString) {
            dialect = _csv.get_dialect_from_registry(dialect);
        }

        // Can we reuse this instance?
        if (dialect instanceof PyDialect && delimiter == null && doublequote == null
            && escapechar == null && lineterminator == null && quotechar == null && quoting == null
            && skipinitialspace == null && strict == null) {
            return dialect;
        }

        if (dialect != null) {
            delimiter = delimiter != null ? delimiter : dialect.__findattr__("delimiter");
            doublequote = doublequote != null
                    ? doublequote : dialect.__findattr__("doublequote");
            escapechar = escapechar != null ? escapechar : dialect.__findattr__("escapechar");
            lineterminator = lineterminator != null
                    ? lineterminator : dialect.__findattr__("lineterminator");
            quotechar = quotechar != null ? quotechar : dialect.__findattr__("quotechar");
            quoting = quoting != null ? quoting : dialect.__findattr__("quoting");
            skipinitialspace = skipinitialspace != null
                    ? skipinitialspace : dialect.__findattr__("skipinitialspace");
            strict = strict != null ? strict : dialect.__findattr__("strict");
        }

        PyDialect self;
        if (new_.for_type == subtype) {
            self = new PyDialect();
        } else {
            self = new PyDialectDerived(subtype);
        }

        // check types and convert to Java values
        int quotingOrdinal;
        self.delimiter = toChar("delimiter", delimiter, ',');
        self.doublequote = toBool("doublequote", doublequote, true);
        self.escapechar = toChar("escapechar", escapechar, '\0');
        self.lineterminator = toStr("lineterminator", lineterminator, "\r\n");
        self.quotechar = toChar("quotechar", quotechar, '"');
        quotingOrdinal = toInt("quoting", quoting, QuoteStyle.QUOTE_MINIMAL.ordinal());
        self.skipinitialspace = toBool("skipinitialspace", skipinitialspace, false);
        self.strict = toBool("strict", strict, false);

        // validate options
        self.quoting = QuoteStyle.fromOrdinal(quotingOrdinal);
        if (self.quoting == null) {
            throw Py.TypeError("bad \"quoting\" value");
        }
        if (self.delimiter == '\0') {
            throw Py.TypeError("delimiter must be set");
        }
        if (quotechar == Py.None && quoting == null) {
            self.quoting = QuoteStyle.QUOTE_NONE;
        }
        if (self.quoting != QuoteStyle.QUOTE_NONE && self.quotechar == '\0') {
            throw Py.TypeError("quotechar must be set if quoting enabled");
        }
        if (self.lineterminator == null) {
            throw Py.TypeError("lineterminator must be set");
        }

        return self;
    }

    private static boolean toBool(String name, PyObject src, boolean dflt) {
        return src == null ? dflt : src.__nonzero__();
    }

    private static char toChar(String name, PyObject src, char dflt) {
        if (src == null) {
            return dflt;
        }
        boolean isStr = Py.isInstance(src, PyString.TYPE);
        if (src == Py.None || isStr && src.__len__() == 0) {
            return '\0';
        } else if (!isStr || src.__len__() != 1) {
            throw Py.TypeError(String.format("\"%s\" must be an 1-character string", name));
        }
        return src.toString().charAt(0);
    }

    private static int toInt(String name, PyObject src, int dflt) {
        if (src == null) {
            return dflt;
        }
        if (!(src instanceof PyInteger)) {
            throw Py.TypeError(String.format("\"%s\" must be an integer", name));
        }
        return src.asInt();
    }

    private static String toStr(String name, PyObject src, String dflt) {
        if (src == null) {
            return dflt;
        }
        if (src == Py.None) {
            return null;
        }
        if (!(src instanceof PyBaseString)) {
            throw Py.TypeError(String.format("\"%s\" must be an string", name));
        }
        return src.toString();
    }

    @ExposedGet(name = "escapechar")
    public PyObject getEscapechar() {
        return escapechar == '\0' ? Py.None : Py.newString(escapechar);
    }

    @ExposedGet(name = "quotechar")
    public PyObject getQuotechar() {
        return quotechar == '\0' ? Py.None : Py.newString(quotechar);
    }

    @ExposedGet(name = "quoting")
    public PyObject getQuoting() {
        return Py.newInteger(quoting.ordinal());
    }

    // XXX: setQuoting and delQuoting are to make test_csv pass (currently we incorrectly
    // throw TypeErrors for get/set AttributeError failures)
    @ExposedSet(name = "quoting")
    public void setQuoting(PyObject obj) {
        throw Py.AttributeError(String.format("attribute '%s' of '%s' objects is not writable",
                                              "quoting", getType().fastGetName()));
    }

    @ExposedDelete(name = "quoting")
    public void delQuoting() {
        throw Py.AttributeError(String.format("attribute '%s' of '%s' objects is not writable",
                                              "quoting", getType().fastGetName()));
    }
}
