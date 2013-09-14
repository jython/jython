// Copyright (c) 2013 Jython Developers
package org.python.util;

import java.io.EOFException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;

import org.gnu.readline.Readline;
import org.gnu.readline.ReadlineLibrary;
import org.python.core.PlainConsole;

/**
 * Uses: <a href="http://java-readline.sourceforge.net/">Java Readline</a> to provide readline like
 * functionality to its console through native readline support (either GNU Readline or Editline).
 */
public class ReadlineConsole extends PlainConsole {

    /** The longest we expect a console prompt to be (in encoded bytes) */
    public static final int MAX_PROMPT = 512;
    /** Stream wrapping System.out in order to capture the last prompt. */
    private ConsoleOutputStream outWrapper;

    /**
     * Construct an instance of the console class specifying the character encoding. This encoding
     * must be one supported by the JVM. The particular backing library loaded will be as specified
     * by registry item <code>python.console.readlinelib</code>, or "Editline" by default.
     * <p>
     * Most of the initialisation is deferred to the {@link #install()} method so that any prior
     * console can uninstall itself before we change system console settings and
     * <code>System.in</code>.
     *
     * @param encoding name of a supported encoding or <code>null</code> for
     *            <code>Charset.defaultCharset()</code>
     */
    public ReadlineConsole(String encoding) {
        super(encoding);
        /*
         * Load the chosen native library. If it's not there, raise UnsatisfiedLinkError. We cannot
         * fall back to Readline's Java mode since it reads from System.in, which would be pointless
         * ... and fatal once we have replaced System.in with a wrapper on Readline.
         */
        String backingLib = System.getProperty("python.console.readlinelib", "Editline");
        Readline.load(ReadlineLibrary.byName(backingLib));

        /*
         * The following is necessary to compensate for (a possible thinking error in) Readline's
         * handling of the bytes returned from the library, and of the prompt.
         */
        String name = encodingCharset.name();
        if (name.equals("ISO-8859-1") || name.equals("US-ASCII")) {
            // Indicate that Readline's Latin fixation will work for this encoding
            latin1 = null;
        } else {
            // We'll need the bytes-to-pointcode mapping
            latin1 = Charset.forName("ISO-8859-1");
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation overrides that by setting <code>System.in</code> to a
     * <code>FilterInputStream</code> object that wraps the configured console library, and wraps
     * <code>System.out</code> in a stream that captures the prompt.
     */
    @Override
    public void install() {

        // Complete the initialisation
        Readline.initReadline("jython");

        try {
            // Force rebind of tab to insert a tab instead of complete
            Readline.parseAndBind("tab: tab-insert");
        } catch (UnsupportedOperationException uoe) {
            // parseAndBind not supported by this readline
        }

        /*
         * Wrap System.out in a special PrintStream that keeps the last incomplete line in case it
         * turns out to be a console prompt.
         */
        try {
            outWrapper = new ConsoleOutputStream(System.out, MAX_PROMPT);
            System.setOut(new PrintStream(outWrapper, true, encoding));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Replace System.in
        FilterInputStream wrapper = new Stream();
        System.setIn(wrapper);
    }

    /**
     * Class to wrap the line-oriented interface to Readline with an InputStream that can replace
     * <code>System.in</code>.
     */
    protected class Stream extends ConsoleInputStream {

        /** Create a System.in replacement with Readline that adds Unix-like line endings */
        Stream() {
            super(System.in, encodingCharset, EOLPolicy.ADD, LINE_SEPARATOR);
        }

        @Override
        protected CharSequence getLine() throws IOException, EOFException {
            // The prompt is the current partial output line.
            CharSequence prompt = outWrapper.getPrompt(encodingCharset).toString();
            // Compensate for Readline.readline prompt handling
            prompt = preEncode(prompt);
            // Get the line from the console via the library
            String line = Readline.readline(prompt.toString());
            // If Readline.readline really returned the line as typed, next would have been:
            // return line==null ? "" : line;
            return postDecode(line);
        }
    }

    /**
     * Encode a prompt to bytes in the console encoding and represent these bytes as the point codes
     * of a Java String. The actual GNU readline function expects a prompt string that is C char
     * array in the console encoding, but the wrapper <code>Readline.readline</code> acts as if this
     * encoding is always Latin-1. This transformation compensates by encoding correctly then
     * representing those bytes as point codes.
     *
     * @param prompt to display via <code>Readline.readline</code>
     * @return encoded form of prompt
     */
    private CharSequence preEncode(CharSequence prompt) {
        if (prompt == null || prompt.length() == 0) {
            return "";
        } else if (latin1 == null) {
            // Encoding is such that readline does the right thing
            return prompt;
        } else {
            // Compensate for readline prompt handling
            CharBuffer cb = CharBuffer.wrap(prompt);
            ByteBuffer bb = encodingCharset.encode(cb);
            return latin1.decode(bb).toString();
        }
    }

    /**
     * Decode the bytes argument (a return from code>Readline.readline</code>) to the String
     * actually entered at the console. The actual GNU readline function returns a C char array in
     * the console encoding, but the wrapper <code>Readline.readline</code> acts as if this encoding
     * is always Latin-1, and on this basis it gives us a Java String whose point codes are the
     * encoded bytes. This method gets the bytes back, then decodes them correctly to a String.
     *
     * @param bytes encoded line (or <code>null</code> for an empty line)
     * @return bytes recovered from the argument
     */
    private CharSequence postDecode(String line) {
        if (line == null) {
            // Library returns null for an empty line
            return "";
        } else if (latin1 == null) {
            // Readline's assumed Latin-1 encoding will have produced the correct result
            return line;
        } else {
            // We have to transcode the line
            CharBuffer cb = CharBuffer.wrap(line);
            ByteBuffer bb = latin1.encode(cb);
            return encodingCharset.decode(bb).toString();
        }
    }

    private final Charset latin1;

}
