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

    static PyException fixParseError(InputStream istream, Throwable t,
				     String filename)
    {
	return fixParseError(istream, t, filename, false);
    }
	
    static PyException fixParseError(InputStream istream, Throwable t,
				     String filename, boolean forceNewline)
    {
	if (t instanceof ParseException) {
	    ParseException e = (ParseException)t;
	    Token tok = e.currentToken;
	    int col=0;
	    int line=0;
	    if (tok != null && tok.next != null) {
		col = tok.next.beginColumn;
		line = tok.next.beginLine;
	    }
	    String text=getLine(istream, line);
	    return new PySyntaxError(e.getMessage(), line, col,
				     text, filename, forceNewline);
	}
	if (t instanceof PythonTokenError) {
	    PythonTokenError e = (PythonTokenError)t;
	    boolean eofSeen = e.EOFSeen;

	    int col = e.errorColumn;
	    int line = e.errorLine;
	    //System.err.println("eof seen: "+eofSeen+", "+e.curChar+", "+col+", "+line);
	    String text = getLine(istream, line);
	    if (eofSeen) col -= 1;
	    return new PySyntaxError(e.getMessage(), line, col,
				     text, filename, forceNewline);
	}
	else return Py.JavaError(t);
    }


    public static Node parse(String string, String kind) {
	return parse(new StringBufferInputStream(string), kind, "<string>");
    }

    public static SimpleNode parse(InputStream istream, String kind,
				   String filename)
    {
	if (!istream.markSupported()) {
	    istream = new BufferedInputStream(istream);
	}
	int nbytes;
	try {
	    nbytes = istream.available();
	}
	catch (IOException ioe1) {
	    nbytes = 10000;
	}
	if (nbytes <= 0)
	    nbytes = 10000;
	if (nbytes > 100000)
	    nbytes = 100000;
	//System.err.println("marking istream");
	istream.mark(nbytes);
	PythonGrammar g = new PythonGrammar(istream);
	SimpleNode node = null;
	try {
	    if (kind.equals("eval")) {
		node = g.eval_input();
	    }
	    else if (kind.equals("exec")) {
		node = g.file_input();
	    }
	    else if (kind.equals("single")) {
		node = g.single_input();
	    }
	    else {
	       throw Py.ValueError("parse kind must be eval, exec, or single");
	    }
	}
	catch (Throwable t) {
	    try {
		//System.err.println("resetting istream");
		istream.reset();
		throw fixParseError(istream, t, filename,
				    g.token_source.forcedNewline);
	    }
	    catch (IOException ioe) {
		throw fixParseError(null, t, filename,
				    g.token_source.forcedNewline);
		//throw Py.IOError(ioe);
	    }
	}
	return node;
    }
	
    public static SimpleNode partialParse(String string, String kind,
					  String filename)
    {
	SimpleNode node = null;
	//System.err.println(new PyString(string).__repr__().toString());
	try {
	    node = parse(new StringBufferInputStream(string), kind, filename);
	}
	catch (PySyntaxError e) {
	    //System.out.println("e: "+e.lineno+", "+e.column+", "+e.forceNewline);
	        
	    try {
		node = parse(new StringBufferInputStream(string+"\n"), kind, filename);
	    }
	    catch (PySyntaxError e1) {
		//System.out.println("e1: "+e1.lineno+", "+e1.column+", "+e1.forceNewline);
		if (e.forceNewline || !e1.forceNewline) throw e;
	    }
	    return null;
	}
	return node;
    }
}
