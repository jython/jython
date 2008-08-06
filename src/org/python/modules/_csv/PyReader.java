/* Copyright (c) Jython Developers */
package org.python.modules._csv;

import org.python.core.Py;
import org.python.core.PyIterator;
import org.python.core.PyList;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.core.PyType;
import org.python.expose.ExposedGet;
import org.python.expose.ExposedType;

/**
 * CSV file reader.
 *
 * Analogous to CPython's _csv.c::ReaderObj struct.
 */
@ExposedType(name = "_csv.reader")
public class PyReader extends PyIterator {

    public static final PyType TYPE = PyType.fromClass(PyReader.class);

    public PyString __doc__ = Py.newString(
        "CSV reader\n" +
        "\n" +
        "Reader objects are responsible for reading and parsing tabular data\n" +
        "in CSV format.\n");

    /** Parsing Dialect. */
    @ExposedGet
    public PyDialect dialect;

    /** The current line number. */
    @ExposedGet
    public int line_num = 0;

    /** The underlying input iterator. */
    private PyObject input_iter;

    /** Current CSV parse state. */
    private ParserState state = ParserState.START_RECORD;

    /** Field list for current record. */
    private PyList fields = new PyList();

    /** Current field builder in here. */
    private StringBuffer field = new StringBuffer(INITIAL_BUILDER_CAPACITY);

    /** Whether the field should be treated as numeric. */
    private boolean numeric_field = false;

    /** Initial capacity of the field StringBuilder. */
    private static final int INITIAL_BUILDER_CAPACITY = 4096;

    public PyReader(PyObject input_iter, PyDialect dialect) {
        this.input_iter = input_iter;
        this.dialect = dialect;
    }

    public PyObject __iternext__() {
        PyObject lineobj;
        PyObject fields;
        String line;
        char c;
        int linelen;

        parse_reset();
        do {
            lineobj = input_iter.__iternext__();
            if (lineobj == null) {
                // End of input OR exception
                if (field.length() != 0) {
                    throw _csv.Error("newline inside string");
                } else {
                    return null;
                }
            }

            line_num++;
            line = lineobj.toString();
            linelen = line.length();

            for (int i = 0; i < linelen; i++) {
                c = line.charAt(i);
                if (c == '\0') {
                    throw _csv.Error("line contains NULL byte");
                }
                parse_process_char(c);
            }
            parse_process_char('\0');
        } while (state != ParserState.START_RECORD);

        fields = this.fields;
        this.fields = new PyList();

        return fields;
    }

    private void parse_process_char(char c) {
        switch (state) {
            case START_RECORD:
                // start of record
                if (c == '\0') {
                    // empty line - return []
                    break;
                } else if (c == '\n' || c == '\r') {
                    state = ParserState.EAT_CRNL;
                    break;
                }
                // normal character - handle as START_FIELD
                state = ParserState.START_FIELD;
                // *** fallthru ***
            case START_FIELD:
                // expecting field
                if (c == '\n' || c == '\r' || c == '\0') {
                    // save empty field - return [fields]
                    parse_save_field();
                    state = c == '\0' ? ParserState.START_RECORD : ParserState.EAT_CRNL;
                } else if (c == dialect.quotechar && dialect.quoting != QuoteStyle.QUOTE_NONE) {
                    // start quoted field
                    state = ParserState.IN_QUOTED_FIELD;
                } else if (c == dialect.escapechar) {
                    // possible escaped character
                    state = ParserState.ESCAPED_CHAR;
                } else if (c == ' ' && dialect.skipinitialspace) {
                    // ignore space at start of field
                    ;
                } else if (c == dialect.delimiter) {
                    // save empty field
                    parse_save_field();
                } else {
                    // begin new unquoted field
                    if (dialect.quoting == QuoteStyle.QUOTE_NONNUMERIC) {
                        numeric_field = true;
                    }
                    parse_add_char(c);
                    state = ParserState.IN_FIELD;
                }
                break;

            case ESCAPED_CHAR:
                if (c == '\0') {
                    c = '\n';
                }
                parse_add_char(c);
                state = ParserState.IN_FIELD;
                break;

            case IN_FIELD:
                // in unquoted field
                if (c == '\n' || c == '\r' || c == '\0') {
                    // end of line - return [fields]
                    parse_save_field();
                    state = c == '\0' ? ParserState.START_RECORD : ParserState.EAT_CRNL;
                } else if (c == dialect.escapechar) {
                    // possible escaped character
                    state = ParserState.ESCAPED_CHAR;
                } else if (c == dialect.delimiter) {
                    // save field - wait for new field
                    parse_save_field();
                    state = ParserState.START_FIELD;
                } else {
                    // normal character - save in field
                    parse_add_char(c);
                }
                break;

            case IN_QUOTED_FIELD:
                // in quoted field
                if (c == '\0') {
                    ;
                } else if (c == dialect.escapechar) {
                    // Possible escape character
                    state = ParserState.ESCAPE_IN_QUOTED_FIELD;
                } else if (c == dialect.quotechar && dialect.quoting != QuoteStyle.QUOTE_NONE) {
                    if (dialect.doublequote) {
                        // doublequote; " represented by ""
                        state = ParserState.QUOTE_IN_QUOTED_FIELD;
                    } else {
                        // end of quote part of field
                        state = ParserState.IN_FIELD;
                    }
                } else {
                    // normal character - save in field
                    parse_add_char(c);
                }
                break;

            case ESCAPE_IN_QUOTED_FIELD:
                if (c == '\0') {
                    c = '\n';
                }
                parse_add_char(c);
                state = ParserState.IN_QUOTED_FIELD;
                break;

            case QUOTE_IN_QUOTED_FIELD:
                // doublequote - seen a quote in an quoted field
                if (dialect.quoting != QuoteStyle.QUOTE_NONE && c == dialect.quotechar) {
                    // save "" as "
                    parse_add_char(c);
                    state = ParserState.IN_QUOTED_FIELD;
                } else if (c == dialect.delimiter) {
                    // save field - wait for new field
                    parse_save_field();
                    state = ParserState.START_FIELD;
                } else if (c == '\n' || c == '\r' || c == '\0') {
                    // end of line - return [fields]
                    parse_save_field();
                    state = c == '\0' ? ParserState.START_RECORD : ParserState.EAT_CRNL;
                } else if (!dialect.strict) {
                    parse_add_char(c);
                    state = ParserState.IN_FIELD;
                } else {
                    // illegal
                    throw _csv.Error(String.format("'%c' expected after '%c'",
                                                   dialect.delimiter, dialect.quotechar));
                }
                break;

            case EAT_CRNL:
                if (c == '\n' || c == '\r') {
                    ;
                } else if (c == '\0') {
                    state = ParserState.START_RECORD;
                } else {
                    String err = "new-line character seen in unquoted field - do you need to "
                            + "open the file in universal-newline mode?";
                    throw _csv.Error(err);
                }
                break;
        }
    }

    private void parse_reset() {
        fields = new PyList();
        state = ParserState.START_RECORD;
        numeric_field = false;
    }

    private void parse_save_field() {
        PyObject field;

        field = new PyString(this.field.toString());
        if (numeric_field) {
            numeric_field = false;
            field = field.__float__();
        }
        fields.append(field);

        // XXX: fastest way to clear StringBuffer?
        this.field = new StringBuffer(INITIAL_BUILDER_CAPACITY);
    }

    private void parse_add_char(char c) {
        int field_len = field.length();
        if (field_len >= _csv.field_limit) {
            throw _csv.Error(String.format("field larger than field limit (%d)",
                                           _csv.field_limit));
        }
        field.append(c);
    }

    /**
     * State of the CSV reader.
     */
    private enum ParserState {
        START_RECORD, START_FIELD, ESCAPED_CHAR, IN_FIELD, IN_QUOTED_FIELD, ESCAPE_IN_QUOTED_FIELD,
        QUOTE_IN_QUOTED_FIELD, EAT_CRNL;
    }
}
