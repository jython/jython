// Copyright (c) Corporation for National Research Initiatives
package org.python.core;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FilterReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;

import org.python.parser.IParserHost;
import org.python.parser.Node;
import org.python.parser.ParseException;
import org.python.parser.PythonGrammar;
import org.python.parser.ReaderCharStream;
import org.python.parser.Token;
import org.python.parser.TokenMgrError;
import org.python.parser.ast.modType;

/**
 * Facade for the classes in the org.python.parser package.
 */

public class parser {
    
    private static IParserHost literalMkrForParser = new LiteralMakerForParser();

    private parser() { ; }

    static String getLine(BufferedReader reader, int line) {
        if (reader == null)
            return "";
        try {
            String text=null;
            for(int i=0; i < line; i++) {
                text = reader.readLine();
            }
            return text;
        } catch (IOException ioe) {
            return null;
        }
    }


    // if reader != null, reset it
    public static PyException fixParseError(BufferedReader reader, Throwable t,
                                     String filename)
    {
        if (reader != null) {
            // System.err.println("resetting istream");
            try {
                reader.reset();
            } catch (IOException e) {
                reader = null;
            }
        }
        
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
                                     text, filename);
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
                                     text, filename);
        }
        else return Py.JavaError(t);
    }


    public static Node parse(String string, String kind) {
        return parse(new ByteArrayInputStream(PyString.to_bytes(string)),
                     kind, "<string>", null);
    }

    public static modType parse(InputStream istream, String kind,
                                 String filename, CompilerFlags cflags) 
    {
        BufferedReader bufreader = prepBufreader(istream, cflags);
        
        PythonGrammar g = new PythonGrammar(new ReaderCharStream(bufreader),
                                            literalMkrForParser);

        modType node = null;
        try {
            node = doparse(kind, cflags, g);
        }
        catch (Throwable t) {
            throw fixParseError(bufreader, t, filename);
        }
        return node;
    }

    public static modType partialParse(String string, String kind,
                                       String filename, CompilerFlags cflags,boolean stdprompt)
    {
        modType node = null;        
        //System.err.println(new PyString(string).__repr__().toString());

        BufferedReader bufreader = prepBufreader(new ByteArrayInputStream(PyString.to_bytes(string)),
                                                 cflags);
        
        PythonGrammar g = new PythonGrammar(new ReaderCharStream(bufreader),
                                            literalMkrForParser);
        
        g.token_source.partial = true;
        g.token_source.stdprompt = stdprompt;

        try {
            node = doparse(kind, cflags, g);
        }
        catch (Throwable t) {
            /*
             CPython codeop exploits that with CPython parser adding newlines
             to a partial valid sentence move the reported error position,
             this is not true for our parser, so we need a different approach:
             we check whether all sentence tokens have been consumed or
             the remaining ones fullfill lookahead expectations. See:
             PythonGrammar.partial_valid_sentence (def in python.jjt)
            */
            
            if (g.partial_valid_sentence(t)) {
                return null;
            }            
            throw fixParseError(bufreader, t, filename);
        }
        return node;
        
        
//        try {
//            node = parse(new StringBufferInputStream(string),
//                         kind, filename, cflags, true);
//        }
//        catch (PySyntaxError e) {
//            //System.out.println("e: "+e.lineno+", "+e.column+", "+
//            //                   e.forceNewline);
//            try {
//                node = parse(new StringBufferInputStream(string+"\n"),
//                             kind, filename, cflags, true);
//            }
//            catch (PySyntaxError e1) {
//                //System.out.println("e1: "+e1.lineno+", "+e1.column+
//                //                   ", "+e1.forceNewline);
//                if (e.forceNewline || !e1.forceNewline) throw e;
//            }
//            return null;
//        }
//        return node;
    }

    private static modType doparse(String kind, CompilerFlags cflags, 
                                   PythonGrammar g) throws ParseException
    {
        modType node = null;
               
        if (cflags != null)
            g.token_source.generator_allowed = cflags.generator_allowed;
        
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
        return node;
    }

    private static BufferedReader prepBufreader(InputStream istream,
                                                CompilerFlags cflags) {
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
        return bufreader;
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

class LiteralMakerForParser implements IParserHost {

       public Object newLong(String s) {
               return Py.newLong(s);
       }

       public Object newLong(java.math.BigInteger i) {
               return Py.newLong(i);
       }

       public Object newFloat(double v) {
               return Py.newFloat(v);
       }

       public Object newImaginary(double v) {
               return Py.newImaginary(v);
       }

       public Object newInteger(int i) {
               return Py.newInteger(i);
       }

       public String decode_UnicodeEscape(
               String str, int start, int end, String errors, boolean unicode) {
                       return PyString.decode_UnicodeEscape(str, start, end, errors, unicode);
       }

}
