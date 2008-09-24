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

import org.python.antlr.BaseParser;
import org.python.antlr.ExpressionParser;
import org.python.antlr.InteractiveParser;
import org.python.antlr.ParseException;
import org.python.antlr.ModuleParser;
import org.python.antlr.NoCloseReaderStream;
import org.python.antlr.PythonParser;
import org.python.antlr.PythonTree;
import org.python.antlr.PythonTree;
import org.python.antlr.PythonLexer;
import org.python.antlr.PythonPartial;
import org.python.antlr.PythonTokenSource;
import org.python.antlr.ast.modType;
import org.python.core.io.StreamIO;
import org.python.core.io.TextIOInputStream;
import org.python.core.io.UniversalIOWrapper;
import org.python.core.util.StringUtil;

/**
 * Facade for the classes in the org.python.antlr package.
 */

public class ParserFacade {
    
    private static int MARK_LIMIT = 100000;

    private ParserFacade() {}

    static String getLine(BufferedReader reader, int line) {
        if (reader == null) {
            return "";
        }
        String text = null;
        try {
            for(int i=0; i < line; i++) {
                text = reader.readLine();
            }
            return text;
        } catch (IOException ioe) {
        }
        return text;
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
            int line=e.line;
            int col=e.charPositionInLine;
            if (node != null) {
                line = node.getLine();
                col = node.getCharPositionInLine();
            }
            String text=getLine(reader, line);
            String msg = e.getMessage();
            if (e.getType() == Py.IndentationError) {
                return new PyIndentationError(msg, line, col, text, filename);
            }
            return new PySyntaxError(msg, line, col, text, filename);
        }
        else return Py.JavaError(t);
    }

    public static modType parse(InputStream stream,
                                String kind,
                                String filename,
                                CompilerFlags cflags) {
        //FIXME: npe?
        BufferedReader bufreader = null;
        modType node = null;
        try {
            if (kind.equals("eval")) {
                bufreader = prepBufreader(stream, cflags, filename);
                CharStream cs = new NoCloseReaderStream(bufreader);
                ExpressionParser e = new ExpressionParser(cs, filename);
                node = e.parse();
            } else if (kind.equals("single")) {
                bufreader = prepBufreader(stream, cflags, filename);
                InteractiveParser i = new InteractiveParser(bufreader, filename);
                node = i.parse();
            } else if (kind.equals("exec")) {
                bufreader = prepBufreader(stream, cflags, filename);
                CharStream cs = new NoCloseReaderStream(bufreader);
                ModuleParser g = new ModuleParser(cs, filename);
                node = g.file_input();
            } else {
                throw Py.ValueError("parse kind must be eval, exec, or single");
            }
        } catch (Throwable t) {
            throw fixParseError(bufreader, t, filename);
        } finally {
            try {
                if (bufreader != null) {
                    bufreader.close();
                }
            } catch (IOException i) {
                //XXX
            }
        }
        return node;
    }

    public static modType partialParse(String string,
                                       String kind,
                                       String filename,
                                       CompilerFlags cflags,
                                       boolean stdprompt) {
        ByteArrayInputStream istream = new ByteArrayInputStream(
                StringUtil.toBytes(string));
        //FIXME: npe?
        BufferedReader bufreader = null;
        modType node = null;
        try {
            if (kind.equals("single")) {
                bufreader = prepBufreader(istream, cflags, filename);
                InteractiveParser i = new InteractiveParser(bufreader, filename);
                node = i.parse();
            } else if (kind.equals("eval")) {
                bufreader = prepBufreader(istream, cflags, filename);
                CharStream cs = new NoCloseReaderStream(bufreader);
                ExpressionParser e = new ExpressionParser(cs, filename);
                node = e.parse();
            } else {
                throw Py.ValueError("parse kind must be eval, exec, or single");
            }
        } catch (Throwable t) {
            PyException p = fixParseError(bufreader, t, filename);
            if (validPartialSentence(bufreader, kind, filename)) {
                return null;
            }
            throw p;
        }
        return node;
    }

    private static boolean validPartialSentence(BufferedReader bufreader, String kind, String filename) {
        PythonLexer lexer = null;
        try {
            bufreader.reset();
            CharStream cs = new NoCloseReaderStream(bufreader);
            lexer = new BaseParser.PyLexer(cs);
            lexer.partial = true;
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            PythonTokenSource indentedSource = new PythonTokenSource(tokens, filename);
            tokens = new CommonTokenStream(indentedSource);
            PythonPartial parser = new PythonPartial(tokens);
            if (kind.equals("single")) {
                parser.single_input();
            } else if (kind.equals("eval")) {
                parser.eval_input();
            } else {
                return false;
            }

        } catch (Exception e) {
            return lexer.eofWhileNested;
        }
        return true;
    }

    private static BufferedReader prepBufreader(InputStream istream,
                                                CompilerFlags cflags,
                                                String filename) throws IOException {
        boolean bom = false;
        String encoding = null;
        InputStream bstream = new BufferedInputStream(istream);
        bom = adjustForBOM(bstream);
        encoding = readEncoding(bstream);

        if (encoding == null) {
            if (bom) {
                encoding = "UTF-8";
            } else if (cflags != null && cflags.encoding != null) {
                encoding = cflags.encoding;
            }
        } else if (cflags.source_is_utf8) {
            throw new ParseException("encoding declaration in Unicode string");
        }

        // Enable universal newlines mode on the input
        StreamIO rawIO = new StreamIO(bstream, true);
        org.python.core.io.BufferedReader bufferedIO =
                new org.python.core.io.BufferedReader(rawIO, 0);
        UniversalIOWrapper textIO = new UniversalIOWrapper(bufferedIO);
        bstream = new TextIOInputStream(textIO);

        Reader reader;
        if(encoding != null) {
            try {
                reader = new InputStreamReader(bstream, encoding);
            } catch(UnsupportedEncodingException exc) {
                throw new PySyntaxError("Encoding '" + encoding + "' isn't supported by this JVM.", 0, 0, "", filename);
            }
        } else {
            try {
                // Default to ISO-8859-1 to get bytes off the input stream since it leaves their values alone.
                reader = new InputStreamReader(bstream, "ISO-8859-1");
            } catch(UnsupportedEncodingException e) {
                // This JVM is whacked, it doesn't even have iso-8859-1
                throw Py.SystemError("Java couldn't find the ISO-8859-1 encoding");
            }
        }
        
        BufferedReader bufreader = new BufferedReader(reader);
        
        bufreader.mark(MARK_LIMIT);
        return bufreader;
    }

    /**
     * Check for a BOM mark at the begginning of stream.  If there is a BOM
     * mark, advance the stream passed it.  If not, reset() to start at the
     * beginning of the stream again.
     *
     * Only checks for EF BB BF right now, since that is all that CPython 2.5
     * Checks.
     *
     * @return true if a BOM was found and skipped.
     * @throws ParseException if only part of a BOM is matched.
     *
     */
    private static boolean adjustForBOM(InputStream stream) throws IOException {
        stream.mark(3);
        int ch = stream.read();
        if (ch == 0xEF) {
            if (stream.read() != 0xBB) {
                throw new ParseException("Incomplete BOM at beginning of file");
            }
            if (stream.read() != 0xBF) {
                throw new ParseException("Incomplete BOM at beginning of file");
            }
            return true;
        }
        stream.reset();
        return false;
	}

    private static String readEncoding(InputStream stream) throws IOException {
        stream.mark(MARK_LIMIT);
        String encoding = null;
        BufferedReader br = new BufferedReader(new InputStreamReader(stream), 512);
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
        // XXX: reset() can still raise an IOException if a line exceeds our large mark
        // limit
        stream.reset();
        return encodingMap(encoding);
    }

    private static String encodingMap(String encoding) {
        if (encoding == null) {
            return null;
        }
        if (encoding.equals("Latin-1") || encoding.equals("latin-1")) {
            return "ISO8859_1";
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
