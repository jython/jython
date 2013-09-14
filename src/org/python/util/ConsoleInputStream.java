// Copyright (c) 2013 Jython Developers
package org.python.util;

import java.io.EOFException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;

/**
 * This class is intended to replace <code>System.in</code> for use with console libraries that
 * provide a line-oriented input mechanism. The console libraries provide a method to get the next
 * line from the console as a String. Particular sub-classes should wrap this character-oriented
 * method in a definition of {@link #getLine()}.
 * <p>
 * The libraries JLine and Java Readline have both been used to give Jython line-recall, editing and
 * a line history preserved between sessions. Both deal with the console encoding internally, and
 * interact with the user in terms of a buffer of characters. Our need in Jython is to access a
 * byte-stream encoding the characters, with line-endings, since it is the text layer of the Python
 * io stack, whether we are using the <code>io</code> module or <code>file</code> built-in, that
 * should deal with encoding.
 */
public abstract class ConsoleInputStream extends FilterInputStream {

    /**
     * Enumeration used to specify whether an end-of-line should be added or replaced at the end of
     * each line read. LEAVE means process the line exactly as the library returns it; ADD means
     * always add an end-of-line; and REPLACE means strip any final '\n', '\r', or '\r\n' and add an
     * end-of-line. The end-of-line to add is specified as a String in the constructor.
     */
    public enum EOLPolicy {
        LEAVE, ADD, REPLACE
    };

    /** The {@link EOLPolicy} specified in the constructor. */
    protected final EOLPolicy eolPolicy;
    /** The end-of-line String specified in the constructor. */
    protected final String eol;
    /** The character encoding specified in the constructor. */
    protected final Charset encoding;
    /** Bytes decoded from the last line read. */
    private ByteBuffer buf;
    /** Empty buffer */
    protected static final ByteBuffer EMPTY_BUF = ByteBuffer.allocate(0);
    /** Platform-defined end-of-line for convenience */
    protected static final String LINE_SEPARATOR = System.getProperty("line.separator");

    /**
     * Create a wrapper configured with end-of-line handling that matches the specific console
     * library being wrapped, and a character encoding matching the expectations of the client.
     * Since this is an abstract class, this constructor will be called as the first action of the
     * library-specific concrete class. The end-of-line policy can be chosen from <code>LEAVE</code>
     * (do not modify the line), <code>ADD</code> (always append <code>eol</code>, and
     * <code>REPLACE</code> (remove a trailing '\n', '\r', or '\r\n' provided by the library, then
     * add <code>eol</code>).
     *
     * @param in stream to wrap, normally <code>System.in</code>
     * @param encoding to use to encode the buffered characters
     * @param eolPolicy choice of how to treat an end-of-line marker
     * @param eol the end-of-line to use when <code>eolPolicy</code> is not <code>LEAVE</code>
     */
    ConsoleInputStream(InputStream in, Charset encoding, EOLPolicy eolPolicy, String eol) {

        // Wrap original in so <code>StreamIO.isatty()</code> will find it reflectively
        super(in);

        // But our real input comes from (re-)encoding the console line
        this.encoding = encoding;
        this.eolPolicy = eolPolicy;
        this.eol = eol != null ? eol : LINE_SEPARATOR;

        // The logic is simpler if we always supply a buffer
        buf = EMPTY_BUF;
    }

   /**
     * Get one line of input from the console. Override this method with the actions specific to the
     * library in use.
     *
     * @return Line entered by user
     * @throws IOException in case of an error
     * @throws EOFException if the library recognises an end-of-file condition
     */
    protected abstract CharSequence getLine() throws IOException, EOFException;

    /**
     * Get a line of text from the console and re-encode it using the console encoding to bytes that
     * will be returned from this InputStream in subsequent read operations.
     *
     * @throws IOException
     * @throws EOFException
     */
    private void fillBuffer() throws IOException, EOFException {

        // In case we exit on an exception ...
        buf = EMPTY_BUF;

        // Bring in another line
        CharSequence line = getLine();
        CharBuffer cb = CharBuffer.allocate(line.length() + eol.length());
        cb.append(line);

        // Apply the EOL policy
        switch (eolPolicy) {

            case LEAVE:
                // Do nothing
                break;

            case ADD:
                // Always add eol
                cb.append(eol);
                break;

            case REPLACE:
                // Strip '\n', '\r', or '\r\n' and add eol
                int n = cb.position() - 1;
                if (n >= 0 && cb.charAt(n) == '\n') {
                    n -= 1;
                }
                if (n >= 0 && cb.charAt(n) == '\r') {
                    n -= 1;
                }
                cb.position(n + 1);
                cb.append(eol);
                break;
        }

        // Prepare to read
        cb.flip();

        // Make this line into a new buffer of encoded bytes
        if (cb.hasRemaining()) {
            buf = encoding.encode(cb); // includes a flip()
        }
    }

    /**
     * Read the next byte of data from the buffered input line.
     * The byte is returned as an int in the range 0 to 255. If no byte is available because the end
     * of the stream has been recognised, the value -1 is returned. This method blocks until input
     * data are available, the end of the stream is detected, or an exception is thrown. Normally, an
     * empty line results in an encoded end-of-line being returned.
     */
    @Override
    public int read() throws IOException {
        try {
            // Do we need to refill?
            while (!buf.hasRemaining()) {
                fillBuffer();
            }
            return buf.get() & 0xff;
        } catch (EOFException e) {
            // End of file condition recognised (e.g. ctrl-D, ctrl-Z)
            return -1;
        }
    }

    /**
     * Reads up to len bytes of data from this input stream into an array of bytes. If len is not
     * zero, the method blocks until some input is available; otherwise, no bytes are read and 0 is
     * returned. This implementation calls {@link #getLine()} at most once to get a line of
     * characters from the console, and encodes them as bytes to be read
     * back from the stream.
     */
    @Override
    public int read(byte[] b, int off, int len) throws IOException, EOFException {

        if (off < 0 || len < 0 || len > b.length - off) {
            throw new IndexOutOfBoundsException();

        } else {
            try {
                if (len > 0) {
                    // Do we need to refill? (Not if zero bytes demanded.)
                    int n = buf.remaining();
                    if (n <= 0) {
                        fillBuffer();
                        n = buf.remaining();
                    }

                    // Deliver all there is, or all that's wanted, whichever is less.
                    len = n < len ? n : len;
                    buf.get(b, off, len);
                }
                return len;

            } catch (EOFException e) {
                // Thrown from getLine
                return -1;
            }
        }
    }

    /**
     * Skip forward n bytes within the current encoded line. A call to <code>skip</code> will not
     * result in reading a new line with {@link #getLine()}.
     */
    @Override
    public long skip(long n) throws IOException {
        long r = buf.remaining();
        if (n > r) {
            n = r;
        }
        buf.position(buf.position() + (int)n);
        return n;
    }

    /** The number of bytes left unread in the current encoded line. */
    @Override
    public int available() throws IOException {
        return buf.remaining();
    }

    /** Mark is not supported. */
    @Override
    public synchronized void mark(int readlimit) {}

    /** Mark is not supported. */
    @Override
    public synchronized void reset() throws IOException {}

    /** Mark is not supported. */
    @Override
    public boolean markSupported() {
        return false;
    }

}
