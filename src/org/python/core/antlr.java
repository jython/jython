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
import org.antlr.runtime.*;
import org.python.antlr.ExpressionParser;
import org.python.antlr.InteractiveParser;
import org.python.antlr.LeadingSpaceSkippingStream;
import org.python.antlr.ParseException;
import org.python.antlr.PythonGrammar;
import org.python.antlr.PythonParser;
import org.python.antlr.PythonTree;
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
        
        if (t instanceof ParseException) {
            ParseException e = (ParseException)t;
            PythonTree tok = e.currentToken;
            int line=0;
            int col=0;
            if (tok != null) {
                line = tok.getLine();
                col = tok.getCharPositionInLine();
            }
            String text=getLine(reader, line);
            return new PySyntaxError(e.getMessage(), line, col,
                                     text, filename);
        }
        else if (t instanceof RecognitionException) {
            RecognitionException e = (RecognitionException)t;
            String msg = e.getMessage();
            String tokenNames[] = PythonParser.tokenNames;
            /* XXX: think the first two are new  in antlr 3.1
            if ( e instanceof UnwantedTokenException ) {
                UnwantedTokenException ute = (UnwantedTokenException)e;
                String tokenName="<unknown>";
                if ( ute.expecting== Token.EOF ) {
                    tokenName = "EOF";
                }
                else {
                    tokenName = tokenNames[ute.expecting];
                }
                msg = "extraneous input "+getTokenErrorDisplay(ute.getUnexpectedToken())+
                    " expecting "+tokenName;
            }
            else if ( e instanceof MissingTokenException ) {
                MissingTokenException mte = (MissingTokenException)e;
                String tokenName="<unknown>";
                if ( mte.expecting== Token.EOF ) {
                    tokenName = "EOF";
                }
                else {
                    tokenName = tokenNames[mte.expecting];
                }
                msg = "missing "+tokenName+" at "+getTokenErrorDisplay(e.token);
            }
            */
            if ( e instanceof MismatchedTokenException ) {
                MismatchedTokenException mte = (MismatchedTokenException)e;
                String tokenName="<unknown>";
                if ( mte.expecting== Token.EOF ) {
                    tokenName = "EOF";
                }
                else {
                    tokenName = tokenNames[mte.expecting];
                }
                msg = "mismatched input "+getTokenErrorDisplay(e.token)+
                    " expecting "+tokenName;
            }
            else if ( e instanceof MismatchedTreeNodeException ) {
                MismatchedTreeNodeException mtne = (MismatchedTreeNodeException)e;
                String tokenName="<unknown>";
                if ( mtne.expecting==Token.EOF ) {
                    tokenName = "EOF";
                }
                else {
                    tokenName = tokenNames[mtne.expecting];
                }
                msg = "mismatched tree node: "+mtne.node+
                    " expecting "+tokenName;
            }
            else if ( e instanceof NoViableAltException ) {
                NoViableAltException nvae = (NoViableAltException)e;
                // for development, can add "decision=<<"+nvae.grammarDecisionDescription+">>"
                // and "(decision="+nvae.decisionNumber+") and
                // "state "+nvae.stateNumber
                msg = "no viable alternative at input "+getTokenErrorDisplay(e.token);
            }
            else if ( e instanceof EarlyExitException ) {
                EarlyExitException eee = (EarlyExitException)e;
                // for development, can add "(decision="+eee.decisionNumber+")"
                msg = "required (...)+ loop did not match anything at input "+
                    getTokenErrorDisplay(e.token);
            }
            else if ( e instanceof MismatchedSetException ) {
                MismatchedSetException mse = (MismatchedSetException)e;
                msg = "mismatched input "+getTokenErrorDisplay(e.token)+
                    " expecting set "+mse.expecting;
            }
            else if ( e instanceof MismatchedNotSetException ) {
                MismatchedNotSetException mse = (MismatchedNotSetException)e;
                msg = "mismatched input "+getTokenErrorDisplay(e.token)+
                    " expecting set "+mse.expecting;
            }
            else if ( e instanceof FailedPredicateException ) {
                FailedPredicateException fpe = (FailedPredicateException)e;
                msg = "rule "+fpe.ruleName+" failed predicate: {"+
                    fpe.predicateText+"}?";
            }
            String text=getLine(reader, e.line);
            return new PySyntaxError(msg, e.line, e.charPositionInLine,
                                     text, filename);
        }
        else return Py.JavaError(t);
    }

    private static String getTokenErrorDisplay(Token t) {
        return t.getText();
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
                PythonGrammar g = new PythonGrammar(cs);
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

    private static modType doparse(String kind, CompilerFlags cflags, 
                                   PythonGrammar g) throws RecognitionException
    {
        modType node = null;
               
        //FJW if (cflags != null)
        //FJW    g.token_source.generator_allowed = cflags.generator_allowed;
        
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
