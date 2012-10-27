package org.python.modules.bz2;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.python.core.ArgParser;
import org.python.core.Py;
import org.python.core.PyIterator;
import org.python.core.PyList;
import org.python.core.PyLong;
import org.python.core.PyNone;
import org.python.core.PyObject;
import org.python.core.PySequence;
import org.python.core.PyString;
import org.python.core.PyTuple;
import org.python.core.PyType;
import org.python.expose.ExposedGet;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedType;
import org.python.modules.synchronize;


@ExposedType(name="bz2.BZ2File")
public class PyBZ2File extends PyObject {

	public static final PyType TYPE = PyType.fromClass(PyBZ2File.class);
	
	@ExposedGet
	public PyObject newlines = null;
	
	private byte[] fileData = null;
	private int offset = 0;
	private String fileName = null;
	private String fileMode = "";
	private boolean inIterMode = false;
	private boolean inUniversalNewlineMode = false;
	private ArrayList<String> validNewlines = new ArrayList<String>();
	{	
		validNewlines.add("\n");
		validNewlines.add("\r");
		validNewlines.add("\r\n");
	}
	
	
	private BZip2CompressorOutputStream writeStream = null;

	public PyBZ2File() {
		super(TYPE);
	}

	public PyBZ2File(PyType subType) {
		super(subType);
	}
	

	@Override
	protected void finalize() throws Throwable {
		BZ2File_close();
		super.finalize();
	}

	@ExposedNew
	@ExposedMethod
	final void BZ2File___init__(PyObject[] args, String[] kwds) {
		ArgParser ap = new ArgParser("bz2file", args, kwds, 
				new String[] {"filename", "mode", "buffering", "compresslevel"}, 1);
		
		PyObject filename = ap.getPyObject(0);
		if (!(filename instanceof PyString)) {
			throw Py.TypeError("coercing to Unicode: need string, '" 
								+ filename.getType().fastGetName() + "' type found");
		}
		
		String mode = ap.getString(1, "r");
		int buffering = ap.getInt(2, 0);
		int compresslevel = ap.getInt(3, 9);
		BZ2File___init__((PyString)filename, mode, buffering, compresslevel);
	}
	
	private void BZ2File___init__(PyString inFileName, String mode, int buffering, int compresslevel) {
		try {
			
			fileName = inFileName.asString();
			fileMode = mode;
			
			// check universal newline mode
			if (mode.contains("U")) {
				inUniversalNewlineMode = true;
			}
			
			if (mode.contains("w")) {
				
				File f = new File(fileName);
				if ( ! f.exists() ) {
					f.createNewFile();
				}
				
				writeStream = new BZip2CompressorOutputStream(new FileOutputStream(fileName), 
																compresslevel);
				
			} else {
				
				FileInputStream fin = new FileInputStream(fileName);
				BufferedInputStream bin = new BufferedInputStream(fin);
				BZip2CompressorInputStream bZin = new BZip2CompressorInputStream(bin);
	
				ByteArrayOutputStream buffer = new ByteArrayOutputStream();
				
				final byte[] buf = new byte[100];
				int n = 0;
				while(-1 != (n = bZin.read(buf))) {
					buffer.write(buf, 0, n);
				}
				fileData = buffer.toByteArray();
				
				buffer.close();
				bZin.close();
				bin.close();
				fin.close();
			}
			
		} catch (IOException e) {
			throw Py.IOError("File " + fileName + " not found,");
		}
	}
	
	@ExposedMethod
	public void __del__() {
		BZ2File_close();
	}
	
	@ExposedMethod
	public void BZ2File_close() {
		
		fileData = null;
		
		if (writeStream != null) {
			BZ2File_flush();
			try {
				writeStream.close();
				writeStream = null;
			} catch (IOException e) {
				throw Py.IOError(e.getMessage());
			}
		}
	}
	
	private void BZ2File_flush() {
		
		if (writeStream != null) {			
			try {
				writeStream.flush();
			} catch (IOException e) {
				throw Py.IOError(e.getMessage());
			}
		}
	}
	
	private byte[] peek() {
		
		byte[] buf = new byte[1];
		if (fileData.length > offset) {
			buf[0] = fileData[offset + 1];
		}
		
		return buf;
	}

	@ExposedMethod
	public PyObject BZ2File_read(PyObject[] args, String[] kwds) {
		
		checkInIterMode(); 
		
		ArgParser ap = new ArgParser("read", args, kwds, new String[] {"size"}, 0);
		
		int size = ap.getInt(0, -1);
				
		byte[] buf = _BZ2File_read(size);
		
		return new PyString(new String(buf));
	}
	
	private byte[] _BZ2File_read(int size) {
		
		byte[] buf = null;
		if (size == 0) {
			return new byte[0];
		} else if (size > 0) {
			buf = new byte[size];
		} else {
			buf = new byte[fileData.length - offset];
		}
		
		int readbytes = 0;
		for(int i=offset, j=0; i<fileData.length && j<buf.length; i++, j++) {
			buf[j] = fileData[i];
			
			String possibleNewline = new String(new byte[] {buf[j]});
			if (possibleNewline.equals("\r")) { // handle CRLF
				buf[j] = '\n';
				if (fileData[i+1] == '\n') { // check if next character part of newline
					possibleNewline = possibleNewline + new String(new byte[]{fileData[i+1]});
					buf = Arrays.copyOf(buf, buf.length - 1); // adjust buffer size
					i++;
				}
			}
			if (validNewlines.contains(possibleNewline) ) {
				addNewlineMarker(possibleNewline);
			}
			
			offset++;
			readbytes++;
		}
		
		if (readbytes == 0) {
			return new byte[0];
		}
		
		return buf;
		
	}
	
	@ExposedMethod
	public PyObject BZ2File_next(PyObject[] args, String[] kwds) {
		if (fileData == null) {
			throw Py.ValueError("Cannot call next() on closed file");
		}
		inIterMode = true;
		return null;
	}
	
	@ExposedMethod
	public PyString BZ2File_readline(PyObject[] args, String[] kwds) {
		
		checkInIterMode(); 
		
		ArgParser ap = new ArgParser("read", args, kwds, new String[] {"size"}, 0);
		
		int size = ap.getInt(0, -1);
		
		StringBuilder line = new StringBuilder();

		byte[] buf = null;
		int readSize = 0;
		while( (buf = _BZ2File_read(1)).length > 0 ) {
			line.append(new String(buf));
			// handle newlines
			boolean mustBreak = false;
			if (inUniversalNewlineMode) {
				if ((char)buf[0] == '\r') {
					if ( peek()[0] == '\n' ) {
						buf = _BZ2File_read(1);
						mustBreak = true;
					}
					line.replace(line.length()-1, line.length(), new String("\n"));
					mustBreak = true;
				} else if ((char)buf[0] == '\n' || (size > -1 && (readSize >= size)) ) {
					mustBreak = true;				
				}
				
			} else {	
				if ((char)buf[0] == '\n' || (size > -1 && (readSize >= size)) ) {
					mustBreak = true;
				}
			}
			
			if (mustBreak) {
				break;
			}
		}
		
		return new PyString(line.toString());
	}
	
	private void addNewlineMarker(String newline) {
		
		if (newlines == null) {
			newlines = new PyString(newline);
		} else {
			if (newlines instanceof PyString ) {
				if ( ! newlines.equals(new PyString(newline))) {
					newlines = new PyTuple(newlines, new PyString(newline));
				}
			} else {
				if ( ! newlines.__contains__(new PyString(newline))) {
					newlines = newlines.__add__(new PyTuple(new PyString(newline)));
				}
			}
		}
	}
	
	
	@ExposedMethod
	public PyList BZ2File_readlines(PyObject[] args, String[] kwds) {
		
		checkInIterMode(); 
		
		// make sure file data valid
		if (fileData == null) {
			throw Py.ValueError("Cannot call readlines() on a closed file");
		}
		
		PyList lineList = new PyList();
		
		PyString line = null;
		while( ! (line = BZ2File_readline(args, kwds)).equals(new PyString()) ) {
			lineList.add(line);
		}
		
		return lineList;
	}
	
	private void checkInIterMode() {
		if (fileMode.contains("r")) {	
			if (inIterMode) {
				throw Py.ValueError("Cannot mix iteration and reads");
			}
		}
	}
	
	@ExposedMethod
	public PyList BZ2File_xreadlines() {
		return BZ2File_readlines(new PyObject[0], new String[0]);
	}
	
	@ExposedMethod
	public void BZ2File_seek(PyObject[] args, String[] kwds) {
		ArgParser ap = new ArgParser("seek", args, kwds, new String[] {"offset", "whence"}, 1);
	
		int newOffset = ap.getInt(0);
		int whence = ap.getInt(1, 0);
		
		// normalise offset
		int finalOffset = 0;
		switch(whence) {
		case 0: // offset from start of file
			if (newOffset > fileData.length) {
				finalOffset = fileData.length;
			} else {
				finalOffset = newOffset;
			}
			break;
		case 1: // move relative to current position
			finalOffset = offset + newOffset;
			break;
		case 2: // move relative to end of file
			finalOffset = fileData.length + newOffset;
		}
		
		if (finalOffset < 0) {
			finalOffset = 0;
		} else {
			if (finalOffset > fileData.length) {
				finalOffset = fileData.length;
			}
		}
		
		// seek operation
		offset = finalOffset;
	}
	
	@ExposedMethod
	public PyLong BZ2File_tell() {
		return new PyLong(offset);
	}
	
	@ExposedMethod
	public void BZ2File_write(PyObject[] args, String[] kwds) {
		
		checkFileWritable();
		
		ArgParser ap = new ArgParser("write", args, kwds, new String[] {"data"}, 0);
		
		PyObject data = ap.getPyObject(0);
		if (data.getType() == PyNone.TYPE) {
			throw Py.TypeError("Expecting str argument");
		}
		byte[] buf = ap.getString(0).getBytes();
		
		try {
			synchronized (this) {				
				writeStream.write(buf);
			}
		} catch (IOException e) {
			
			throw Py.IOError(e.getMessage());
		}
	}

	
	@ExposedMethod
	public void BZ2File_writelines(PyObject[] args, String[] kwds) {
		
		checkFileWritable();
		
		ArgParser ap = new ArgParser("writelines", args, kwds, new String[] {"sequence_of_strings"}, 0);
		
		PySequence seq = (PySequence) ap.getPyObject(0);
		for (Iterator<PyObject> iterator = seq.asIterable().iterator(); iterator.hasNext();) {
			PyObject line = iterator.next();
			
			BZ2File_write(new PyObject[]{line}, new String[]{"data"});
			
		}
		
	}
	
	private void checkFileWritable() {
		if (fileMode.contains("r")) {
			throw Py.IOError("File in read-only mode");
		}
		if (writeStream == null) {
			throw Py.ValueError("Stream closed");
		}
	}
	
	
	@Override
	@ExposedMethod
	public PyObject __iter__() {
		return new BZ2FileIterator();
	}
	

	private class BZ2FileIterator extends PyIterator {

		@Override
		public PyObject __iternext__() {
			
			PyString s = BZ2File_readline(new PyObject[0], new String[0]);
			
			if (s.equals(new PyString())) {
				return null;
			} else {
				return s;
			}
		}
		
	}
	
	@ExposedMethod
	public PyObject BZ2File___enter__() {
		if (fileMode.contains("w")){	
			if (writeStream == null) {
				throw Py.ValueError("Stream closed");
			}
		} else if (fileMode.contains("r")) {
			if (fileData == null) {
				throw Py.ValueError("Stream closed");
			}
		}
		
		return this;
	}

	@ExposedMethod
	public boolean BZ2File___exit__(PyObject exc_type, PyObject exc_value, PyObject traceback) {
		BZ2File_close();
		return false;
	}
}
