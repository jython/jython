
/*
 * Jython Database Specification API 2.0
 *
 * $Id$
 *
 * Copyright (c) 2001 brian zimmer <bzimmer@ziclix.com>
 *
 */
package com.ziclix.python.sql.pipe.csv;

import java.io.*;
import org.python.core.*;
import com.ziclix.python.sql.pipe.Sink;

/**
 * The CSVSink writes data out in a Comma Seperated Format.
 *
 * @author brian zimmer
 * @version $Revision$
 */
public class CSVSink implements Sink {

	/** Field header */
	protected boolean header;

	/** Field delimiter */
	protected String delimiter;

	/** Field writer */
	protected PrintWriter writer;

	/** Field converters */
	protected PyObject converters;

	/**
	 * All data will be written to the given PrintWriter.
	 *
	 * @param writer the PrintWriter to which data will be written
	 */
	public CSVSink(PrintWriter writer) {
		this(writer, Py.None);
	}

	/**
	 * All data will be written to the given PrintWriter.  If
	 * the converters param is not None, then an attempt will
	 * be made to convert the object using the given converter.
	 *
	 * @param writer the PrintWriter to which data will be written
	 * @param converters an indexed dictionary of callable objects used for converting objects to strings
	 */
	public CSVSink(PrintWriter writer, PyObject converters) {

		this.header = false;
		this.writer = writer;
		this.converters = converters;
		this.delimiter = ",";
	}

	/**
	 * Handle the data callback and write the row out.
	 */
	public void row(PyObject row) {

		String[] values = new String[row.__len__()];

		if (this.header) {
			for (int i = 0; i < row.__len__(); i++) {
				values[i] = this.convert(Py.newInteger(i), row.__getitem__(i));
			}
		} else {
			for (int i = 0; i < row.__len__(); i++) {
				values[i] = row.__getitem__(i).__getitem__(0).toString();
			}

			this.header = true;
		}

		this.println(values);
	}

	/**
	 * Convert the object at index to a String.
	 */
	protected String convert(PyObject index, PyObject object) {

		if (this.converters != Py.None) {
			PyObject converter = this.converters.__finditem__(index);

			if (converter != Py.None) {
				object = converter.__call__(object);
			}
		}

		if ((object == Py.None) || (object == null)) {
			return "";
		}

		return CSVString.toCSV(object.toString());
	}

	/**
	 * Print the row of Strings.
	 */
	protected void println(String[] row) {

		for (int i = 0; i < row.length - 1; i++) {
			this.writer.print(row[i]);
			this.writer.print(this.delimiter);
		}

		this.writer.println(row[row.length - 1]);
	}

	/**
	 * Method start
	 *
	 */
	public void start() {}

	/**
	 * Method end
	 *
	 */
	public void end() {}
}
