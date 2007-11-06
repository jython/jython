/* Copyright (c) 2007 Jython Developers */
package org.python.core.io;

import java.nio.ByteBuffer;

/**
 * A buffered reader and writer together for a random access file.
 *
 * @author Philip Jenvey
 */
public class BufferedRandom extends BufferedIOMixin {

    /** The buffered reader */
    protected BufferedIOBase reader;

    /** The buffered writer */
    protected BufferedIOBase writer;

    /**
     * Construct a BufferedRandom of bufferSize, wrapping the given
     * RawIOBase.
     *
     * @param rawIO {@inheritDoc}
     * @param bufferSize {@inheritDoc}
     */
    public BufferedRandom(RawIOBase rawIO, int bufferSize) {
        super(rawIO, bufferSize);
        initChildBuffers();
    }

    /**
     * Initialize the child read/write buffers.
     *
     */
    protected void initChildBuffers() {
        this.reader = new BufferedReader(rawIO, bufferSize);
        this.writer = new BufferedWriter(rawIO, bufferSize);
    }

    /** {@inheritDoc} */
    public long seek(long pos, int whence) {
        flush();
        // First do the raw seek, then empty the read buffer, so that
        // if the raw seek fails, we don't lose buffered data forever.
        pos = writer.seek(pos, whence);
        reader.clear();
        return pos;
    }

    /** {@inheritDoc} */
    public long tell() {
        if (writer.buffered()) {
            return writer.tell();
        }
        return reader.tell();
    }

    /** {@inheritDoc} */
    public ByteBuffer read(int size) {
        flush();
        return reader.read(size);
    }

    /** {@inheritDoc} */
    public ByteBuffer readall() {
        flush();
        return reader.readall();
    }

    /** {@inheritDoc} */
    public int readinto(ByteBuffer bytes) {
        flush();
        return reader.readinto(bytes);
    }

    /** {@inheritDoc} */
    public int write(ByteBuffer bytes) {
        if (reader.buffered()) {
            reader.clear();
        }
        return writer.write(bytes);
    }

    /** {@inheritDoc} */
    public ByteBuffer peek(int size) {
        flush();
        return reader.peek(size);
    }

    /** {@inheritDoc} */
    public int read1(ByteBuffer bytes) {
        flush();
        return reader.read1(bytes);
    }

    /** {@inheritDoc} */
    public void flush() {
        writer.flush();
    }
}
