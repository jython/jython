// Copyright (c) Corporation for National Research Initiatives
package org.python.core;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.Reader;
import java.io.StringReader;
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

    /**
     * Internal parser entry point.
     *
     * Users of this method should call fixParseError on any Throwable thrown
     * from it, to translate ParserExceptions into PySyntaxErrors or
     * PyIndentationErrors.
     */
    private static modType parse(BufferedReader reader,
                                String kind,
                                String filename,
                                CompilerFlags cflags) throws Throwable {
        reader.mark(MARK_LIMIT); // We need the ability to move back on the
                                 // reader, for the benefit of fixParseError and
                                 // validPartialSentence
        if (kind.equals("eval")) {
            CharStream cs = new NoCloseReaderStream(reader);
            ExpressionParser e = new ExpressionParser(cs, filename);
            return e.parse();
        } else if (kind.equals("single")) {
            InteractiveParser i = new InteractiveParser(reader, filename);
            return i.parse();
        } else if (kind.equals("exec")) {
            CharStream cs = new NoCloseReaderStream(reader);
            ModuleParser g = new ModuleParser(cs, filename);
            return g.file_input();
        } else {
            throw Py.ValueError("parse kind must be eval, exec, or single");
        }
    }

    public static modType parse(InputStream stream,
                                String kind,
                                String filename,
                                CompilerFlags cflags) {
        BufferedReader bufReader = null;
        try {
            // prepBufReader takes care of encoding detection and universal
            // newlines:
            bufReader = prepBufreader(stream, cflags, filename);
            return parse(bufReader, kind, filename, cflags );
        } catch (Throwable t) {
            throw fixParseError(bufReader, t, filename);
        } finally {
            close(bufReader);
        }
    }

    public static modType parse(String string,
                                String kind,
                                String filename,
                                CompilerFlags cflags) {
        BufferedReader bufReader = null;
        try {
            bufReader = prepBufReader(string);
            return parse(bufReader, kind, filename, cflags);
        } catch (Throwable t) {
            throw fixParseError(bufReader, t, filename);
        } finally {
            close(bufReader);
        }
    }

    public static modType partialParse(String string,
                                       String kind,
                                       String filename,
                                       CompilerFlags cflags,
                                       boolean stdprompt) {
        // XXX: What's the idea of the stdprompt argument?
        BufferedReader reader = null;
        try {
            reader = prepBufReader(string);
            return parse(reader, kind, filename, cflags);
        } catch (Throwable t) {
            PyException p = fixParseError(reader, t, filename);
            if (reader != null && validPartialSentence(reader, kind, filename)) {
                return null;
            }
            throw p;
        } finally {
            close(reader);
        }
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
            System.out.println(e);
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
        return bufreader;
    }

    private static BufferedReader prepBufReader(String string) throws IOException {
        BufferedReader bufReader;

        // LineNumberReader takes care of universal newlines
        bufReader = new LineNumberReader(new StringReader(string));

        // If the input is a decoded string (implied from the String argument
        // for prepBufReader), it can't have an encoding declaration.
        bufReader.mark(MARK_LIMIT);
        if (findEncoding(bufReader) != null) {
            throw new ParseException("encoding declaration in Unicode string");
        }
        bufReader.reset();

        return bufReader;
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
        encoding = findEncoding(br);
        // XXX: reset() can still raise an IOException if a line exceeds our large mark
        // limit
        stream.reset();
        return encodingMap(encoding);
    }

    /**
     * Reads the first two lines of the reader, searching for an encoding
     * declaration.
     *
     * Note that reseting the reader (if needed) is responsibility of the caller.
     *
     * @return The declared encoding, or null if no encoding declaration is
     *         found
     */
    private static String findEncoding(BufferedReader br)
            throws IOException {
        String encoding = null;
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
        return encoding;
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

    private static void close(BufferedReader reader) {
        try {
            if (reader != null) {
                reader.close();
            }
        } catch (IOException i) {
            // XXX: Log the error?
        }
    }

}
