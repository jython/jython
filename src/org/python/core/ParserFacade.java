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
import org.antlr.runtime.*;
import org.python.antlr.ExpressionParser;
import org.python.antlr.InteractiveParser;
import org.python.antlr.LeadingSpaceSkippingStream;
import org.python.antlr.ParseException;
import org.python.antlr.ModuleParser;
import org.python.antlr.PythonParser;
import org.python.antlr.PythonTree;
import org.python.core.util.StringUtil;
import org.python.antlr.IParserHost;
import org.python.antlr.PythonTree;
import org.python.antlr.ast.modType;

/**
 * Facade for the classes in the org.python.antlr package.
 */

public class ParserFacade {
    
    private static IParserHost literalMkrForParser = new LiteralMakerForParser2();

    private ParserFacade() {}

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
        
        if (t instanceof ParseException) {
            ParseException e = (ParseException)t;
            PythonTree node = (PythonTree)e.node;
            int line=0;
            int col=0;
            if (node != null) {
                line = node.getLine();
                col = node.getCharPositionInLine();
            }
            String text=getLine(reader, line);
            return new PySyntaxError(e.getMessage(), line, col,
                                     text, filename);
        }
        else return Py.JavaError(t);
    }

    public static PythonTree parse(String string, String kind) {
        return parse(new ByteArrayInputStream(StringUtil.toBytes(string)),
                     kind, "<string>", null);
    }

    public static modType parse(InputStream istream,
                                String kind,
                                String filename,
                                CompilerFlags cflags) {
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
            } else if (kind.equals("single")) {
                bufreader = prepBufreader(istream, cflags);
                cs = new ANTLRReaderStream(bufreader);
                InteractiveParser i = new InteractiveParser(cs);
                node = i.partialParse();
            } else if (kind.equals("exec")) {
                bufreader = prepBufreader(istream, cflags);
                cs = new ANTLRReaderStream(bufreader);
                ModuleParser g = new ModuleParser(cs);
                node = g.file_input();
            } else {
               throw Py.ValueError("parse kind must be eval, exec, " + "or single");
            }
        } catch (Throwable t) {
            throw fixParseError(bufreader, t, filename);
        }
        return node;
    }

    public static modType partialParse(String string,
                                       String kind,
                                       String filename,
                                       CompilerFlags cflags,
                                       boolean stdprompt) {
        CharStream cs = null;
        //FIXME: definite NPE potential here -- do we even need prepBufreader
        //       now?
        BufferedReader bufreader = null;
        modType node = null;
        if (kind.equals("single")) {
            ByteArrayInputStream bi = new ByteArrayInputStream(
                    StringUtil.toBytes(string));
            bufreader = prepBufreader(bi, cflags);
            try {
                cs = new ANTLRReaderStream(bufreader);
            } catch (IOException io) {
                //FIXME:
            }
            InteractiveParser i = new InteractiveParser(cs);
            node = i.partialParse();
        } else {
            throw Py.ValueError("parse kind must be eval, exec, " + "or single");
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
