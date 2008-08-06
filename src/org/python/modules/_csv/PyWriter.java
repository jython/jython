/* Copyright (c) Jython Developers */
package org.python.modules._csv;

import org.python.core.Py;
import org.python.core.PyException;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.core.PyType;
import org.python.expose.ExposedType;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedGet;

/**
 * CSV file writer.
 *
 * Analogous to CPython's _csv.c::WriterObj struct.
 */
@ExposedType(name = "_csv.writer")
public class PyWriter extends PyObject {

    public static final PyType TYPE = PyType.fromClass(PyWriter.class);

    /** Parsing dialect. */
    @ExposedGet
    public PyDialect dialect;

    /** Output lines writer callable. */
    private PyObject writeline;

    /** Buffer for parser.join. */
    private StringBuffer rec;

    /** Length of record. */
    private int rec_len = 0;

    /** Number of fields in record. */
    private int num_fields = 0;

    /** Whether field should be quoted during a join. */
    private boolean quoted = false;

    public PyWriter(PyObject writeline, PyDialect dialect) {
        this.writeline = writeline;
        this.dialect = dialect;
    }

    public static PyString __doc__writerows = Py.newString(
            "writerows(sequence of sequences)\n" +
            "\n" +
            "Construct and write a series of sequences to a csv file.  Non-string\n" +
            "elements will be converted to string.");

    public void writerows(PyObject seqseq) {
        writerows(seqseq);
    }

    @ExposedMethod
    final void writer_writerows(PyObject seqseq) {
        PyObject row_iter;
        PyObject row_obj;
        boolean result;

        row_iter = seqseq.__iter__();
        if (row_iter == null) {
            throw _csv.Error("writerows() argument must be iterable");
        }

        while ((row_obj = row_iter.__iternext__()) != null) {
            result = writerow(row_obj);
            if (!result) {
                break;
            }
        }
    }

    public static PyString __doc__writerow = Py.newString(
            "writerow(sequence)\n" +
            "\n" +
            "Construct and write a CSV record from a sequence of fields.  Non-string\n" +
            "elements will be converted to string."
            );

    public boolean writerow(PyObject seq) {
        return writer_writerow(seq);
    }

    @ExposedMethod
    final boolean writer_writerow(PyObject seq) {
        int len;
        int i;

        if (!seq.isSequenceType()) {
            throw _csv.Error("sequence expected");
        }

        len = seq.__len__();
        if (len < 0) {
            return false;
        }

        // Join all fields in internal buffer.
        join_reset();
        for (i = 0; i < len; i++) {
            PyObject field;
            boolean append_ok;
            quoted = false;

            field = seq.__getitem__(i);
            if (field == null) {
                return false;
            }

            switch (dialect.quoting) {
                case QUOTE_NONNUMERIC:
                    try {
                        field.__float__();
                    } catch (PyException ex) {
                        quoted = true;
                    }
                    break;
                case QUOTE_ALL:
                    quoted = true;
                    break;
                default:
                    quoted = false;
            }

            if (field instanceof PyString) {
                append_ok = join_append(field.toString(), len == 1);
            } else if (field == Py.None) {
                append_ok = join_append("", len == 1);
            } else {
                PyObject str = field.__str__();
                if (str == null) {
                    return false;
                }

                append_ok = join_append(str.toString(), len == 1);
            }
            if (!append_ok) {
                return false;
            }
        }

        // Add line terminator.
        if (!join_append_lineterminator()) {
            return false;
        }

        writeline.__call__(new PyString(rec.toString()));
        return true;
    }

    private void join_reset() {
        rec_len = 0;
        num_fields = 0;
        quoted = false;
        rec = new StringBuffer();
    }

    private boolean join_append_lineterminator() {
        rec.append(dialect.lineterminator);
        return true;
    }

    private boolean join_append(String field, boolean quote_empty) {
        int rec_len;

        rec_len = join_append_data(field, quote_empty, false);
        if (rec_len < 0) {
            return false;
        }

        this.rec_len = join_append_data(field, quote_empty, true);
        num_fields++;

        return true;
    }

    /**
     * This method behaves differently depending on the value of copy_phase: if copy_phase
     * is false, then the method determines the new record length. If copy_phase is true
     * then the new field is appended to the record.
     */
    private int join_append_data(String field, boolean quote_empty, boolean copy_phase) {
        int i;

        // If this is not the first field we need a field separator.
        if (num_fields > 0) {
            addChar(dialect.delimiter, copy_phase);
        }

        // Handle preceding quote
        if (copy_phase && quoted) {
            addChar(dialect.quotechar, copy_phase);
        }

        // parsing below is based on _csv.c which expects all strings to be terminated
        // with a nul byte.
        field += '\0';

        // Copy/count field data.
        for (i = 0;; i++) {
            char c = field.charAt(i);
            boolean want_escape = false;

            if (c == '\0') {
                break;
            }
            if (c == dialect.delimiter || c == dialect.escapechar || c == dialect.quotechar
                || dialect.lineterminator.indexOf(c) > -1) {
                if (dialect.quoting == QuoteStyle.QUOTE_NONE) {
                    want_escape = true;
                } else {
                    if (c == dialect.quotechar) {
                        if (dialect.doublequote) {
                            addChar(dialect.quotechar, copy_phase);
                        } else {
                            want_escape = true;
                        }
                    }
                    if (!want_escape) {
                        quoted = true;
                    }
                }
                if (want_escape) {
                    if (dialect.escapechar == '\0') {
                        throw _csv.Error("need to escape, but no escapechar set");
                    }
                    addChar(dialect.escapechar, copy_phase);
                }
            }

            // Copy field character into record buffer.
            addChar(c, copy_phase);
        }

        // If field is empty check if it needs to be quoted.
        if (i == 0 && quote_empty) {
            if (dialect.quoting == QuoteStyle.QUOTE_NONE) {
                throw _csv.Error("single empty field record must be quoted");
            } else {
                quoted = true;
            }
        }

        // Handle final quote character on field.
        if (quoted) {
            if (copy_phase) {
                addChar(dialect.quotechar, copy_phase);
            } else {
                // Didn't know about leading quote until we found it necessary in field
                // data - compensate for it now.
                rec_len += 2;
            }
        }

        return rec_len;
    }

    private void addChar(char c, boolean copy_phase) {
        if (copy_phase) {
            rec.append(c);
        }
        rec_len++;
    }
}
