/* Copyright (c) 2007 Jython Developers */
package org.python.core.io;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.ByteBuffer;

import org.python.core.Py;

/**
 * Raw I/O implementation for Readers and Writers.
 *
 * @author Philip Jenvey
 */
public class ReaderWriterIO extends RawIOBase {

    /** The underlying Reader */
    private Reader reader;

    /** The underlying Writer */
    private Writer writer;

    /**
     * Construct a ReaderWriterIO from the given reader/writer combo.
     *
     * @param reader a Reader
     * @param writer a Writer
     */
    public ReaderWriterIO(Reader reader, Writer writer) {
        this.reader = reader;
        this.writer = writer;
    }

    /**
     * Construct a ReaderWriterIO from the given reader.
     *
     * @param reader a Reader
     */
    public ReaderWriterIO(Reader reader) {
        this(reader, null);
    }

    /**
     * Construct a ReaderWriterIO from the given writer.
     *
     * @param writer a Writer
     */
    public ReaderWriterIO(Writer writer) {
        this(null, writer);
    }

    /** {@inheritDoc} */
    public int readinto(ByteBuffer buf) {
        checkClosed();
        checkReadable();

        int count = 0;
        try {
            while (buf.hasRemaining()) {
                buf.put((byte)reader.read());
                count++;
            }
        } catch (IOException ioe) {
            throw Py.IOError(ioe);
        }
        return count;
    }

    /** {@inheritDoc} */
    public int write(ByteBuffer buf) {
        checkClosed();
        checkWritable();

        int count = 0;
        try {
            while (buf.hasRemaining()) {
                writer.write(buf.get() & 0xff);
                count++;
            }
        } catch (IOException ioe) {
            throw Py.IOError(ioe);
        }
        return count;
    }

    /** {@inheritDoc} */
    public void close() {
        if (closed()) {
            return;
        }
        try {
            if (reader != null) {
                reader.close();
            }
            if (writer != null) {
                writer.close();
            }
        } catch (IOException ioe) {
            throw Py.IOError(ioe);
        }
        super.close();
    }

    /** {@inheritDoc} */
    public Object __tojava__(Class cls) {
        return null;
    }

    /** {@inheritDoc} */
    public boolean readable() {
        return reader != null;
    }

    /** {@inheritDoc} */
    public boolean writable() {
        return writer != null;
    }
}
