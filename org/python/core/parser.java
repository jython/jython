// Copyright (c) Corporation for National Research Initiatives
package org.python.core;

import org.python.parser.*;
import java.io.*;

/**
 * Facade for the classes in the org.python.parser package.
 */

public class parser {

    private parser() { ; }

    static String getLine(BufferedReader reader, int line) {
        if (reader == null)
            return "";
        try {
            String text=null;
            for(int i=0; i      <line; i++) {
                text = reader.readLine();
            }
            return text;
        } catch (IOException ioe) {
            return null;
        }
    }

    static public PyException fixParseError(BufferedReader reader,
                                            Throwable t,
                                            String filename)
    {
        return fixParseError(reader, t, filename, false);
    }

    static PyException fixParseError(BufferedReader reader, Throwable t,
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
            String text=getLine(reader, line);
            return new PySyntaxError(e.getMessage(), line, col,
                                     text, filename, forceNewline);
        }
        if (t instanceof TokenMgrError) {
            TokenMgrError e = (TokenMgrError)t;
            boolean eofSeen = e.EOFSeen;

            int col = e.errorColumn;
            int line = e.errorLine;
            //System.err.println("eof seen: "+eofSeen+", "+e.curChar+", "+col+
            //                   ", "+line);
            String text = getLine(reader, line);
            if (eofSeen)
                col -= 1;
            return new PySyntaxError(e.getMessage(), line, col,
                                     text, filename, forceNewline);
        }
        else return Py.JavaError(t);
    }


    public static Node parse(String string, String kind) {
        return parse(new StringBufferInputStream(string),
                     kind, "<string>", null);
    }


    public static SimpleNode parse(InputStream istream, String kind,
                                   String filename, CompilerFlags cflags)
    {
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

        Reader reader = null;
        try {
            if (cflags != null && cflags.encoding != null) {
                reader = new InputStreamReader(istream, cflags.encoding);
            }
        } catch (UnsupportedEncodingException exc) { ; }
        if (reader == null) {
            reader = new InputStreamReader(istream);
        }

        //if (Options.fixMacReaderBug);
        reader = new FixMacReaderBug(reader);

        BufferedReader bufreader = new BufferedReader(reader);

        try {
            bufreader.mark(nbytes);
        } catch (IOException exc) { }

        PythonGrammar g = new PythonGrammar(new ReaderCharStream(bufreader));
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
               throw Py.ValueError("parse kind must be eval, exec, " +
                                   "or single");
            }
        }
        catch (Throwable t) {
            try {
                //System.err.println("resetting istream");
                bufreader.reset();
                throw fixParseError(bufreader, t, filename,
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
                                          String filename,
                                          CompilerFlags cflags)
    {
        SimpleNode node = null;
        //System.err.println(new PyString(string).__repr__().toString());
        try {
            node = parse(new StringBufferInputStream(string),
                         kind, filename, cflags);
        }
        catch (PySyntaxError e) {
            //System.out.println("e: "+e.lineno+", "+e.column+", "+
            //                   e.forceNewline);
            try {
                node = parse(new StringBufferInputStream(string+"\n"),
                             kind, filename, cflags);
            }
            catch (PySyntaxError e1) {
                //System.out.println("e1: "+e1.lineno+", "+e1.column+
                //                   ", "+e1.forceNewline);
                if (e.forceNewline || !e1.forceNewline) throw e;
            }
            return null;
        }
        return node;
    }
}


/**
 * A workaround for a bug in MRJ2.2's FileReader, where the value returned
 * from read(b, o, l) sometimes are wrong.
 */
class FixMacReaderBug extends FilterReader {
    public FixMacReaderBug(Reader in) {
        super(in);
    }

    public int read(char b[], int off, int len) throws IOException {
        int l = super.read(b, off, len);
        if (l < -1)
            l += off;
        return l;
    }
}
