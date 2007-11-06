
/* Copyright (c) 2007 Jython Developers */
package org.python.core.io;

/**
 * A line buffered writer and a non buffered reader together for a
 * random access file.
 *
 * @author Philip Jenvey
 */
public class LineBufferedRandom extends BufferedRandom {

    /**
     * Construct a LineBufferedRandom wrapping the given RawIOBase.
     *
     * @param rawIO {@inheritDoc}
     */
    public LineBufferedRandom(RawIOBase rawIO) {
        super(rawIO, 1);
    }

    /** {@inheritDoc} */
    protected void initChildBuffers() {
        // Line buffering is for output only
        this.reader = new BufferedReader(rawIO, 0);
        this.writer = new LineBufferedWriter(rawIO);
    }
}
