package org.python.core;
import org.python.parser.*;
import java.io.*;

public class parser {
	public static String getLine(InputStream istream, int line) {
	    if (istream == null) return "";
		try {
			String text=null;
			DataInputStream s = new DataInputStream(istream);
			for(int i=0; i	<line; i++) {
				text = s.readLine();
			}
			return text;
		} catch (IOException ioe) {
			return null;
		}
	}

	static PyException fixParseError(InputStream istream, Throwable t, String filename) {
		if (t instanceof ParseException) {
			ParseException e = (ParseException)t;
			Token tok = e.currentToken;
			int col = tok.next.beginColumn;
			int line = tok.next.beginLine;
			String text=getLine(istream, line);
			return new PySyntaxError("invalid syntax", line, col, text, filename);
		}
		if (t instanceof TokenMgrError) {
			TokenMgrError e = (TokenMgrError)t;
			int col = e.errorColumn;
			int line = e.errorLine;
			String text = getLine(istream, line);
			return new PySyntaxError(e.getMessage(), line, col, text, filename);
		}
		else return Py.JavaError(t);
	}


	public static Node parse(String string, String kind) {
		return parse(new StringBufferInputStream(string), kind, "<string>");
	}

	public static SimpleNode parse(InputStream istream, String kind, String filename) {
		if (!istream.markSupported()) {
			istream = new BufferedInputStream(istream);
		}
		int nbytes;
		try { nbytes = istream.available(); }
		catch (IOException ioe1) { nbytes = 10000; }
		if (nbytes <= 0) nbytes = 10000;
		if (nbytes > 100000) nbytes = 100000;
		//System.err.println("marking istream");
		istream.mark(nbytes);
		PythonGrammar g = new PythonGrammar(istream);
		SimpleNode node = null;
		try {
			if (kind.equals("eval")) {
				node = g.eval_input();
			} else {
				if (kind.equals("exec")) {
					node = g.file_input();
				} else {
					if (kind.equals("single")) {
						node = g.single_input();
					} else {
						throw Py.ValueError("parse kind must be eval, exec, or single");
					}
				}
			}
		} catch (Throwable t) {
			try {
			    //System.err.println("resetting istream");
				istream.reset();
				throw fixParseError(istream, t, filename);
			} catch (IOException ioe) {
				throw Py.IOError(ioe);
			}
		}
		return node;
	}
	
	public static SimpleNode partialParse(String string, String kind, String filename) {
	    SimpleNode node = null;
	    try {
	        node = parse(new StringBufferInputStream(string), kind, filename);
	    } catch (PySyntaxError e) {
	        try {
	            node = parse(new StringBufferInputStream(string+"\n"), kind, filename);
	        } catch (PySyntaxError e1) {
	            if (e.lineno == e1.lineno && e.column == e1.column)
	                throw e;
	        }
	        return null;
	    }
	    return node;
	}
}