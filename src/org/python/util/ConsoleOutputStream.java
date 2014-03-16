// Copyright (c) 2013 Jython Developers
package org.python.util;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

/**
 * This class may be used to wrap and replace <code>System.out</code> so that the console handling
 * library (JLine or Java Readline) can treat an incomplete line (one without a newline) as a prompt
 * string, and so know the true position of the cursor. It achieves this by keeping a copy of bytes
 * that pass through from from upstream to the true <code>System.out</code>, until either a newline
 * arrives, or a defined capacity (typically the console width) is reached. If client code requests
 * the partial line as a prompt, that action also empties the buffer. In that case, the client
 * (which is the console object) is responsible for making the prompt emerge on the real console.
 */
public class ConsoleOutputStream extends FilterOutputStream {

    /** The storage buffer for accumulating the partial line */
    protected ByteBuffer buf;

    /**
     * Create a wrapper on an <code>OutputStream</code> that holds a copy of the last incomplete
     * line written to it (as bytes), in case it is needed as a console prompt.
     *
     * @param out the stream wrapped (normally <code>System.out</code>)
     * @param promptCapacity maximum number of bytes to buffer
     */
    public ConsoleOutputStream(OutputStream out, int promptCapacity) {
        super(out);
        buf = ByteBuffer.allocate(Math.max(4, promptCapacity));
    }

    /**
     * This write method steals a copy of each byte in a buffer while passing it on to the wrapped
     * stream. The buffer is reset by each newline, when it overflows, or by the client when it is
     * taken as a console prompt in {@link #getPrompt(Charset)}.
     */
    @Override
    public void write(int b) throws IOException {
        buf.put((byte)b);
        out.write(b);
        if (b == '\n' || buf.remaining() == 0) {
            // Empty the prompt buffer
            buf.position(0);
        }
    }

    @Override
    public void flush() throws IOException {
        // Flush passed on to wrapped System.out
        out.flush();
    }

    @Override
    public void close() throws IOException {
        super.close(); // ... with a flush
        out.close();
    }

    /**
     * Return the stored bytes encoded as characters. Whatever is in the buffer at the point this
     * method is called will be returned, decoded as a CharSequence (from which a String can easily
     * be got) and the buffer left empty. The expectation is that the characters will be issued by
     * the caller as a prompt.
     *
     * @param encoding with which to decode the bytes
     * @return the decoded prompt
     */
    protected CharSequence getPrompt(Charset encoding) {
        buf.flip();
        CharSequence prompt = encoding.decode(buf);
        buf.compact();
        return prompt;
    }

}
