// Copyright (c) Corporation for National Research Initiatives
package org.python.core;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;

import org.antlr.runtime.ANTLRReaderStream;
import org.antlr.runtime.CharStream;
import org.antlr.runtime.RecognitionException;
import org.python.antlr.ExpressionParser;
import org.python.antlr.LeadingSpaceSkippingStream;
import org.python.antlr.PythonGrammar;
import org.python.core.util.StringUtil;
import org.python.antlr.IParserHost;
import org.python.antlr.PythonTree;
import org.python.antlr.ast.modType;

/**
 * Facade for the classes in the org.python.antlr package.
 */

public class antlr {
    
    private static IParserHost literalMkrForParser = new LiteralMakerForParser2();

    private antlr() {}

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
            try {
                reader.reset();
            } catch (IOException e) {
                reader = null;
            }
        }
        
        if (t instanceof RecognitionException) {
            RecognitionException e = (RecognitionException)t;
            //FJW Token tok = e.currentToken;
            int col=0;
            int line=0;
            //FJW if (tok != null && tok.next != null) {
            //FJW     col = tok.next.beginColumn;
            //FJW     line = tok.next.beginLine;
            //FJW }
            String text=getLine(reader, line);
            return new PySyntaxError(e.getMessage(), line, col,
                                     text, filename);
        }
        /* FJW FJW FJW
        if (t instanceof TokenMgrError) {
            TokenMgrError e = (TokenMgrError)t;
            boolean eofSeen = e.EOFSeen;

            int col = e.errorColumn;
            int line = e.errorLine;
            String text = getLine(reader, line);
            if (eofSeen)
                col -= 1;
            return new PySyntaxError(e.getMessage(), line, col,
                                     text, filename);
        }
        */
        else return Py.JavaError(t);
    }

    public static PythonTree parse(String string, String kind) {
        return parse(new ByteArrayInputStream(StringUtil.toBytes(string)),
                     kind, "<string>", null);
    }

    public static modType parse(InputStream istream, String kind,
                                 String filename, CompilerFlags cflags) 
    {
        CharStream cs = null;
        //FIXME: definite NPE potential here -- do we even need prepBufreader
        //       now?
        BufferedReader bufreader = null;
        modType node = null;
        try {
            if (kind.equals("eval")) {
                bufreader = prepBufreader(new LeadingSpaceSkippingStream(istream), cflags);
                cs = new ANTLRReaderStream(bufreader);
                ExpressionParser e = new ExpressionParser(cs);
                node = e.parse();
            } else {
                bufreader = prepBufreader(istream, cflags);
                cs = new ANTLRReaderStream(bufreader);
                PythonGrammar g = new PythonGrammar(cs);//FJW, literalMkrForParser);
                node = doparse(kind, cflags, g);
            }
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
        BufferedReader bufreader = prepBufreader(new ByteArrayInputStream(StringUtil.toBytes(string)),
                                                 cflags);

        CharStream cs = null;
        try {
            cs = new ANTLRReaderStream(bufreader);
        } catch (IOException io){
            //FIXME:
            System.err.println("FIXME: Don't eat exceptions.");
        }
        PythonGrammar g = new PythonGrammar(cs, true);
        //FJW g.token_source.partial = true;
        //FJW g.token_source.stdprompt = stdprompt;

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
            
            //FJW if (g.partial_valid_sentence(t)) {
            //FJW    return null;
            //FJW }            
            throw fixParseError(bufreader, t, filename);
        }
        return node;
    }

    private static modType doparse(String kind, CompilerFlags cflags, 
                                   PythonGrammar g) throws /*Parse*/Exception //FJW
    {
        modType node = null;
               
        //FJW if (cflags != null)
        //FJW    g.token_source.generator_allowed = cflags.generator_allowed;
        
        if (kind.equals("exec")) {
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
        Reader reader;
        if(cflags != null && cflags.encoding != null) {
            try {
                reader = new InputStreamReader(istream, cflags.encoding);
            } catch(UnsupportedEncodingException exc) {
                throw Py.SystemError("python.console.encoding, " + cflags.encoding
                        + ", isn't supported by this JVM so we can't parse this data.");
            }
        } else {
            try {
                // Use ISO-8859-1 to get bytes off the input stream since it leaves their values alone.
                reader = new InputStreamReader(istream, "ISO-8859-1");
            } catch(UnsupportedEncodingException e) {
                // This JVM is whacked, it doesn't even have iso-8859-1
                throw Py.SystemError("Java couldn't find the ISO-8859-1 encoding");
            }
        }
        
        BufferedReader bufreader = new BufferedReader(reader);
        
        try {
            bufreader.mark(nbytes);
        } catch (IOException exc) { }
        return bufreader;
    }
}

class LiteralMakerForParser2 implements IParserHost {

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
