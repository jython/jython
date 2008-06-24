// Copyright (c) Corporation for National Research Initiatives
package org.python.core;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.antlr.runtime.ANTLRReaderStream;
import org.antlr.runtime.CharStream;
import org.antlr.runtime.CommonTokenStream;
import org.python.antlr.ExpressionParser;
import org.python.antlr.InteractiveParser;
import org.python.antlr.LeadingSpaceSkippingStream;
import org.python.antlr.ParseException;
import org.python.antlr.ModuleParser;
import org.python.antlr.NoCloseReaderStream;
import org.python.antlr.PythonParser;
import org.python.antlr.PythonTree;
import org.python.core.util.StringUtil;
import org.python.antlr.IParserHost;
import org.python.antlr.PythonTree;
import org.python.antlr.PythonPartialLexer;
import org.python.antlr.PythonPartialParser;
import org.python.antlr.PythonPartialTokenSource;
import org.python.antlr.ast.modType;

/**
 * Facade for the classes in the org.python.antlr package.
 */

public class ParserFacade {
    
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
            String msg = e.getMessage();
            if (msg == null) {
                msg = "XXX: missing msg";
            }
            if (text == null) {
                text = "XXX: missing text";
            }
            return new PySyntaxError(msg, line, col, text, filename);
        }
        else return Py.JavaError(t);
    }

    public static PythonTree parse(String string, String kind) {
        return parse(new ByteArrayInputStream(StringUtil.toBytes(string)),
                     kind, "<string>", null);
    }

    public static modType parse(InputStream stream,
                                String kind,
                                String filename,
                                CompilerFlags cflags) {
        BufferedInputStream bstream = new BufferedInputStream(stream);
        //FIMXE: npe?
        BufferedReader bufreader = null;
        modType node = null;
        try {
            if (kind.equals("eval")) {
                bufreader = prepBufreader(new LeadingSpaceSkippingStream(bstream), cflags, filename);
                CharStream cs = new ANTLRReaderStream(bufreader);
                ExpressionParser e = new ExpressionParser(cs);
                node = e.parse();
            } else if (kind.equals("single")) {
                bufreader = prepBufreader(bstream, cflags, filename);
                InteractiveParser i = new InteractiveParser(bufreader);
                node = i.parse();
            } else if (kind.equals("exec")) {
                bufreader = prepBufreader(bstream, cflags, filename);
                CharStream cs = new ANTLRReaderStream(bufreader);
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
        modType node = null;
        //FIMXE: npe?
        BufferedReader bufreader = null;
        try {
            if (kind.equals("single")) {
                ByteArrayInputStream bi = new ByteArrayInputStream(
                        StringUtil.toBytes(string));
                BufferedInputStream bstream = bstream = new BufferedInputStream(bi);
                bufreader = prepBufreader(bstream, cflags, filename);
                InteractiveParser i = new InteractiveParser(bufreader);
                node = i.parse();
            } else {
                throw Py.ValueError("parse kind must be eval, exec, " + "or single");
            }
        } catch (Throwable t) {
            PyException p = fixParseError(bufreader, t, filename);
            if (validPartialSentence(bufreader)) {
                return null;
            }
            throw p;
        }
        return node;
    }

    private static boolean validPartialSentence(BufferedReader bufreader) {
        PythonPartialLexer lexer = null;
        try {
            bufreader.reset();
            CharStream cs = new NoCloseReaderStream(bufreader);
            lexer = new InteractiveParser.PPLexer(cs);
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            tokens.discardOffChannelTokens(true);
            PythonPartialTokenSource indentedSource = new PythonPartialTokenSource(tokens);
            tokens = new CommonTokenStream(indentedSource);
            PythonPartialParser parser = new PythonPartialParser(tokens);
            parser.single_input();
        } catch (Exception e) {
            return lexer.eofWhileNested;
        }
        return true;
    }

    private static BufferedReader prepBufreader(InputStream istream,
                                                CompilerFlags cflags,
                                                String filename) throws IOException {
        String encoding = readEncoding(istream);
        if(encoding == null && cflags != null && cflags.encoding != null) {
            encoding = cflags.encoding;
        }

        Reader reader;
        if(encoding != null) {
            try {
                reader = new InputStreamReader(istream, encoding);
            } catch(UnsupportedEncodingException exc) {
                throw new PySyntaxError("Encoding '" + encoding + "' isn't supported by this JVM.", 0, 0, "", filename);
            }
        } else {
            try {
                // Default to ISO-8859-1 to get bytes off the input stream since it leaves their values alone.
                reader = new InputStreamReader(istream, "ISO-8859-1");
            } catch(UnsupportedEncodingException e) {
                // This JVM is whacked, it doesn't even have iso-8859-1
                throw Py.SystemError("Java couldn't find the ISO-8859-1 encoding");
            }
        }
        
        BufferedReader bufreader = new BufferedReader(reader);
        
        bufreader.mark(100000);
        return bufreader;
    }


    private static String readEncoding(InputStream stream) throws IOException {
        stream.mark(10000);
        String encoding = null;
        BufferedReader br = new BufferedReader(new InputStreamReader(stream));
        for (int i = 0; i < 2; i++) {
            String strLine = br.readLine();
            if (strLine == null) {
                break;
            }
            String result = matchEncoding(strLine);
            if (result != null) {
                encoding = result;
                break;
            }
        }
        stream.reset();
        return encodingMap(encoding);
    }

    private static String encodingMap(String encoding) {
        if (encoding == null) {
            return null;
        }
        if (encoding.equals("Latin-1")) {
            return "ISO8859_1";
        }
        //FIXME: I'm not at all sure why utf-8 is breaking test_cookielib.py
        //       but this fixes it on my machine.  I'm hoping it is a Java
        //       default behavior, but I'm afraid it may be a Java on Mac
        //       default behavior.
        if (encoding.equals("utf-8")) {
            return null;
        }
        return encoding;
    }

    private static String matchEncoding(String inputStr) {
        String patternStr = "coding[:=]\\s*([-\\w.]+)";
        Pattern pattern = Pattern.compile(patternStr);
        Matcher matcher = pattern.matcher(inputStr);
        boolean matchFound = matcher.find();

        if (matchFound && matcher.groupCount() == 1) {
            String groupStr = matcher.group(1);
            return groupStr;
        }
        return null;
    }

}
