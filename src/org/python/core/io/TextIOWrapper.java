/* Copyright (c) 2007 Jython Developers */
package org.python.core.io;

import java.nio.ByteBuffer;
import java.util.regex.Pattern;

/**
 * A Buffered text stream.
 *
 * This differs from py3k TextIOWrapper, which currently handles both
 * text mode (py3k text mode is incompatible with Python 2.x's text
 * mode) as well as universal mode.
 *
 * @see UniversalIOWrapper
 *
 * @author Philip Jenvey
 */
public class TextIOWrapper extends BinaryIOWrapper {

    /** LF RE pattern, used for writes */
    private static final Pattern LF_PATTERN = Pattern.compile("\n");

    /** The platform's Newline character */
    private String newline;

    /** Whether or not newline is \n */
    private boolean newlineIsLF;

    /**
     * Contruct a TextIOWrapper wrapping the given BufferedIOBase.
     *
     * @param bufferedIO {@inheritDoc}
     */
    public TextIOWrapper(BufferedIOBase bufferedIO) {
        super(bufferedIO);
        newline = System.getProperty("line.separator");
        newlineIsLF = newline.equals("\n");
    }

    /** {@inheritDoc} */
    public String read(int size) {
        if (newlineIsLF) {
            return super.read(size);
        }
        if (size < 0) {
            return readall();
        }

        // Avoid ByteBuffer (this.readahead) method calls in the inner
        // loop by reading directly from the readahead's backing array
        byte[] readaheadArray;
        int readaheadPos;
        char[] builderArray = new char[size];
        int builderPos = 0;

        do {
            readaheadArray = readahead.array();
            readaheadPos = readahead.arrayOffset() + readahead.position();

            while (readaheadPos < readahead.limit() && builderPos < size) {
                char next = (char)(readaheadArray[readaheadPos++] & 0xff);

                if (next == '\r') {
                    // Don't translate CR at EOF
                    if (readaheadPos == readahead.limit()) {
                        if (readChunk() == 0) {
                            // EOF
                            builderArray[builderPos++] = next;
                            return new String(builderArray, 0, builderPos);
                        }

                        // Not EOF and readChunk replaced the
                        // readahead; reset the readahead info
                        readaheadArray = readahead.array();
                        readaheadPos = readahead.arrayOffset() + readahead.position();
                    }

                    if (readaheadArray[readaheadPos] == LF_BYTE) {
                        next = '\n';
                        readaheadPos++;
                    }
                }

                builderArray[builderPos++] = next;
            }

        } while (builderPos < size && readChunk(size - builderPos) > 0);

        // Finally reposition the readahead to where we ended. The
        // position is invalid if the readahead is empty (at EOF;
        // readChunk() returned 0)
        if (readahead.hasRemaining()) {
            readahead.position(readaheadPos - readahead.arrayOffset());
        }
        // Shrink the readahead if it grew
        packReadahead();

        return new String(builderArray, 0, builderPos);
    }

    /** {@inheritDoc} */
    public String readall() {
        if (newlineIsLF) {
            return super.readall();
        }

        // Read the remainder of file
        ByteBuffer remaining = bufferedIO.readall();

        // Detect CRLF spanning the readahead and remaining
        int length = 0;
        if (readahead.hasRemaining() && readahead.get(readahead.limit() - 1) == CR_BYTE &&
            remaining.hasRemaining() && remaining.get(remaining.position()) == LF_BYTE) {
            // The trailing value of readahead is CR, the first value
            // of remaining is LF. Overwrite CR with that LF (CRLF ->
            // LF)
            length--;
        }

        // Create an array that accommodates the readahead and the
        // remainder
        char[] all = new char[readahead.remaining() + remaining.remaining()];

        // Consume the readahead
        length += readLoop(readahead.array(), readahead.arrayOffset() +
                           readahead.position(), all, 0, readahead.remaining());
        readahead.position(readahead.limit());

        // Consume the remainder of the file
        length += readLoop(remaining.array(), remaining.arrayOffset() +
                           remaining.position(), all, length, remaining.remaining());

        return new String(all, 0, length);
    }

    /**
     * Read and convert the src byte array into the dest char array.
     *
     * Converts CRLF to LF. src is assumed to be the end of the file;
     * that is, when src ends with CR, no attempt is made to peek into
     * the underlying file to detect a succeeding LF.
     *
     * @param src the source byte array
     * @param srcPos starting position in the source array
     * @param dest the destination char array
     * @param destPos starting position in the destination array
     * @param length the number of array elements to be copied
     * @return the number of chars written to the destination array
     */
    private int readLoop(byte[] src, int srcPos, char[] dest, int destPos, int length) {
        int destStartPos = destPos;
        int srcEndPos = srcPos + length;

        while (srcPos < srcEndPos) {
            char next = (char)(src[srcPos++] & 0xff);

            if (next == '\r') {
                // Don't translate CR at EOF
                if (srcPos == srcEndPos) {
                    dest[destPos++] = next;
                    continue;
                }

                if (src[srcPos] == LF_BYTE) {
                    next = '\n';
                    srcPos++;
                }
            }

            dest[destPos++] = next;
        }

        return destPos - destStartPos;
    }

    /** {@inheritDoc} */
    public String readline(int size) {
        if (newlineIsLF) {
            return super.readline(size);
        }

        // Avoid ByteBuffer (this.readahead) and StringBuilder
        // (this.builder) method calls in the inner loop by reading
        // directly from the readahead's backing array and writing to
        // an interim char array (this.interimBuilder)
        byte[] readaheadArray;
        int readaheadPos;
        int interimBuilderPos;

        do {
            readaheadArray = readahead.array();
            readaheadPos = readahead.arrayOffset() + readahead.position();
            interimBuilderPos = 0;

            while (readaheadPos < readahead.limit() &&
                   (size < 0 || builder.length() + interimBuilderPos < size)) {
                char next = (char)(readaheadArray[readaheadPos++] & 0xff);
                interimBuilder[interimBuilderPos++] = next;

                if (next == '\r') {
                    boolean flushInterimBuilder = false;
                    // Don't translate CR at EOF
                    if (readaheadPos == readahead.limit()) {
                        if (readChunk() == 0) {
                            // EOF
                            builder.append(interimBuilder, 0, interimBuilderPos);
                            return drainBuilder();
                        }

                        // Not EOF and readChunk replaced the
                        // readahead; reset the readahead info and
                        // flush the interimBuilder (it's full)
                        readaheadArray = readahead.array();
                        readaheadPos = readahead.arrayOffset() + readahead.position();
                        flushInterimBuilder = true;
                    }

                    if (readaheadArray[readaheadPos] == LF_BYTE) {
                        // A CRLF: overwrite CR with the LF
                        readaheadPos++;
                        interimBuilder[interimBuilderPos - 1] = next = '\n';
                    }

                    if (flushInterimBuilder) {
                        // Safe to flush now, incase a CRLF was
                        // changed
                        builder.append(interimBuilder, 0, interimBuilderPos);
                        interimBuilderPos = 0;
                    }
                }

                if (next == '\n') {
                    builder.append(interimBuilder, 0, interimBuilderPos);

                    // Reposition the readahead to where we ended
                    readahead.position(readaheadPos - readahead.arrayOffset());

                    return drainBuilder();
                }
            }

            builder.append(interimBuilder, 0, interimBuilderPos);

        } while ((size < 0 || builder.length() < size) && readChunk() > 0);

        // Finally reposition the readahead to where we ended. The
        // position is invalid if the readahead is empty (at EOF;
        // readChunk() returned 0)
        if (readahead.hasRemaining()) {
            readahead.position(readaheadPos - readahead.arrayOffset());
        }

        return drainBuilder();
    }

    /** {@inheritDoc} */
    public int write(String buf) {
        if (!newlineIsLF) {
            buf = LF_PATTERN.matcher(buf).replaceAll(newline);
        }
        return super.write(buf);
    }
}
