/* Copyright (c) 2007 Jython Developers */
package org.python.core.io;

import java.nio.ByteBuffer;
import java.util.EnumSet;
import java.util.Iterator;

import org.python.core.Py;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.core.PyTuple;

/**
 * A Buffered text stream in universal newlines mode.
 *
 * @author Philip Jenvey
 */
public class UniversalIOWrapper extends TextIOBase {

    /** Whether the next character, if it's an LF, should be skipped
     * (the previous character was a CR) */
    private boolean skipNextLF = false;

    /** The Newlines encountered in this file */
    private EnumSet<Newline> newlineTypes = EnumSet.noneOf(Newline.class);

    /**
     * Contruct a UniversalIOWrapper wrapping the given
     * BufferedIOBase.
     *
     * @param bufferedIO {@inheritDoc}
     */
    public UniversalIOWrapper(BufferedIOBase bufferedIO) {
        super(bufferedIO);
    }

    /** {@inheritDoc} */
    public String read(int size) {
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
            readaheadPos = readahead.position();

            while (readaheadPos < readahead.limit() && builderPos < size) {
                char next = (char)(readaheadArray[readaheadPos++] & 0xff);

                switch (next) {
                case '\r':
                    next = '\n';
                    // Don't skipNextLF (identify CR immediately) at
                    // EOF
                    if (readaheadPos == readahead.limit()) {
                        if (readChunk() == 0) {
                            // EOF
                            newlineTypes.add(Newline.CR);
                            builderArray[builderPos++] = next;
                            return new String(builderArray, 0, builderPos);
                        }

                        // Not EOF and readChunk replaced the
                        // readahead; reset the readahead info
                        readaheadArray = readahead.array();
                        readaheadPos = readahead.position();
                    }
                    skipNextLF = true;
                    break;
                case '\n':
                    if (skipNextLF) {
                        skipNextLF = false;
                        newlineTypes.add(Newline.CRLF);
                        continue;
                    }
                    newlineTypes.add(Newline.LF);
                    break;
                default:
                    if (skipNextLF) {
                        skipNextLF = false;
                        newlineTypes.add(Newline.CR);
                    }
                }

                builderArray[builderPos++] = next;
            }

        } while (builderPos < size && readChunk(size - builderPos) > 0);

        // Finally reposition the readahead to where we ended. The
        // position is invalid if the readahead is empty (at EOF;
        // readChunk() returned 0)
        if (readahead.hasRemaining()) {
            readahead.position(readaheadPos);
        }
        // Shrink the readahead if it grew
        packReadahead();

        return new String(builderArray, 0, builderPos);
    }

    /** {@inheritDoc} */
    public String readall() {
        // Read the remainder of file
        ByteBuffer remaining = bufferedIO.readall();

        // Create an array that accommodates the readahead and the
        // remainder
        char[] all = new char[readahead.remaining() + remaining.remaining()];

        // Consume the readahead
        int length = readLoop(readahead.array(), readahead.position(), all, 0,
                              readahead.remaining());
        readahead.position(readahead.limit());

        // Consume the remainder of the file
        length += readLoop(remaining.array(), remaining.position(), all, length,
                           remaining.remaining());

        // Handle skipNextLF at EOF
        if (skipNextLF) {
            skipNextLF = false;
            newlineTypes.add(Newline.CR);
        }

        return new String(all, 0, length);
    }

    /**
     * Read and convert the src byte array into the dest char array.
     *
     * Converts CR and CRLF to LF. No attempt is made to handle CRLF
     * at EOF; skipNextLF may be toggled true after this method
     * returns.
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

            switch (next) {
            case '\r':
                next = '\n';
                skipNextLF = true;
                break;
            case '\n':
                if (skipNextLF) {
                    skipNextLF = false;
                    newlineTypes.add(Newline.CRLF);
                    continue;
                }
                newlineTypes.add(Newline.LF);
                break;
            default:
                if (skipNextLF) {
                    skipNextLF = false;
                    newlineTypes.add(Newline.CR);
                }
            }

            dest[destPos++] = next;
        }

        return destPos - destStartPos;
    }

    /** {@inheritDoc} */
    public String readline(int size) {
        // Avoid ByteBuffer (this.readahead) and StringBuilder
        // (this.builder) method calls in the inner loop by reading
        // directly from the readahead's backing array and writing to
        // an interim char array (this.interimBuilder)
        byte[] readaheadArray;
        int readaheadPos;
        int interimBuilderPos;
        String line;

        do {
            readaheadArray = readahead.array();
            readaheadPos = readahead.position();
            interimBuilderPos = 0;

            while (readaheadPos < readahead.limit() &&
                   (size < 0 || builder.length() + interimBuilderPos < size)) {
                char next = (char)(readaheadArray[readaheadPos++] & 0xff);

                switch (next) {
                case '\r':
                    next = '\n';
                    // Don't skipNextLF (identify CR immediately) at
                    // EOF
                    if (readaheadPos == readahead.limit()) {
                        if (readChunk() == 0) {
                            // EOF
                            newlineTypes.add(Newline.CR);

                            interimBuilder[interimBuilderPos++] = next;
                            builder.append(interimBuilder, 0, interimBuilderPos);

                            // The readahead position is already valid
                            // (it's at 0)
                            return drainBuilder();
                        }

                        // Not EOF and readChunk replaced the
                        // readahead; reset the readahead info
                        readaheadPos = readahead.position();
                    }
                    skipNextLF = true;

                    interimBuilder[interimBuilderPos++] = next;
                    builder.append(interimBuilder, 0, interimBuilderPos);

                    // Reposition the readahead to where we ended
                    readahead.position(readaheadPos);

                    return drainBuilder();
                case '\n':
                    if (skipNextLF) {
                        skipNextLF = false;
                        newlineTypes.add(Newline.CRLF);
                        continue;
                    }
                    newlineTypes.add(Newline.LF);

                    interimBuilder[interimBuilderPos++] = next;
                    builder.append(interimBuilder, 0, interimBuilderPos);

                    // Reposition the readahead to where we ended
                    readahead.position(readaheadPos);

                    return drainBuilder();
                default:
                    if (skipNextLF) {
                        skipNextLF = false;
                        newlineTypes.add(Newline.CR);
                    }
                }

                interimBuilder[interimBuilderPos++] = next;
            }

            builder.append(interimBuilder, 0, interimBuilderPos);

        } while ((size < 0 || builder.length() < size) && readChunk() > 0);

        // Finally reposition the readahead to where we ended. The
        // position is invalid if the readahead is empty (at EOF;
        // readChunk() returned 0)
        if (readahead.hasRemaining()) {
            readahead.position(readaheadPos);
        }

        return drainBuilder();
    }

    /** {@inheritDoc} */
    public int write(String buf) {
        // Universal newlines doesn't support writing; just raise the
        // appropriate exception
        checkClosed();
        checkWritable();
        return -1;
    }

    /** {@inheritDoc} */
    public long seek(long pos, int whence) {
        pos = super.seek(pos, whence);
        skipNextLF = false;
        return pos;
    }

    /** {@inheritDoc} */
    public long tell() {
        long pos = super.tell();
        if (skipNextLF) {
            // Look for a succeeding LF; if it exists, consume it and
            // report the position as the beginning of the next
            // newline
            if (!atEOF()) {
                int readaheadPos = readahead.position();

                if (readahead.get(readaheadPos) == LF_BYTE) {
                    skipNextLF = false;
                    newlineTypes.add(Newline.CRLF);
                    readahead.position(++readaheadPos);
                    pos++;
                }
            }
        }
        return pos;
    }

    /** {@inheritDoc} */
    public PyObject getNewlines() {
        int size = newlineTypes.size();
        if (size == 0) {
            return Py.None;
        } else if (size == 1) {
            Newline newline = (Newline)newlineTypes.iterator().next();
            return new PyString(newline.getValue());
        }

        int i = 0;
        PyObject[] newlines = new PyObject[size];
        for (Newline newline : newlineTypes) {
            newlines[i++] = new PyString(newline.getValue());
        }
        return new PyTuple(newlines);
    }

    /**
     * Newline types.
     *
     */
    private enum Newline {
        /** Carriage return */
        CR("\r"),

        /** Line feed  */
        LF("\n"),

        /** Carriage return line feed **/
        CRLF("\r\n");

        /** The String value */
        private final String value;

        /**
         * Return the String value of this newline
         *
         * @return the newline character as a String
         */
        public String getValue() { return value; }

        /**
         * Construct a new Newline.
         *
         * @param value the newline character as a String
         */
        Newline(String value) {
            this.value = value;
        }
    }
}
