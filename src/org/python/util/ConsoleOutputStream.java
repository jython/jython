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
 * string, and so know the true position of the cursor. It achieves this by buffering bytes written
 * from upstream until a newline arrives, or a defined capacity is reached. This interferes
 * necessarily with upstream expectations about <code>flush()</code>. If client code requests the
 * partial line as a prompt, that action empties the buffer. In that case, the client (which is the
 * console object) is responsible for making the prompt emerge on the real console.
 */
public class ConsoleOutputStream extends FilterOutputStream {

    /** The storage buffer for accumulating the partial line */
    protected ByteBuffer buf;

    /**
     * Create a wrapper on an <code>OutputStream</code> that holds back the last incomplete line
     * written to it (as bytes), in case it is needed as a console prompt.
     *
     * @param out the stream wrapped (normally <code>System.out</code>)
     * @param promptCapacity maximum number of bytes to buffer
     */
    public ConsoleOutputStream(OutputStream out, int promptCapacity) {
        super(out);
        buf = ByteBuffer.allocate(Math.max(4, promptCapacity));
    }

    /**
     * This write method buffers up the bytes until there is a complete line, then this is written
     * all at once either to the wrapped stream, or by the client as a console prompt.
     */
    @Override
    public void write(int b) throws IOException {
        buf.put((byte)b);
        if (b == '\n' || buf.remaining() == 0) {
            // Send these bytes downstream
            writeBufOut();
        }
    }

    /**
     * This class does not flush on request, only at line endings.
     */
    @Override
    public void flush() throws IOException {
        // Flush disabled until we are ready to write the buffer out
    }

    /**
     * Ensure bytes stored in the buffer are written (and flushed), then close downstream.
     */
    @Override
    public void close() throws IOException {
        writeBufOut();
        super.close();
    }

    /**
     * Write the stored bytes downstream, with a flush following. We do this when an end-of-line
     * byte is written, since the buffer contents cannot then be intended as a prompt, and when we
     * run out of space.
     *
     * @throws IOException
     */
    private void writeBufOut() throws IOException {
        // Could flip before and compact after, but it's really not necessary.
        out.write(buf.array(), 0, buf.position());
        buf.position(0);
        out.flush();
    }

    /**
     * Return the stored bytes encoded as characters instead of sending them downstream. Whatever is
     * in the buffer at the point this method is called will be returned, decoded as a CharSequence
     * (from which a String can easily be got) and the buffer left empty. The expectation is that
     * the characters will be issued by the caller as a prompt.
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
