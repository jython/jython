// A file-like object for reading from java.io.Reader objects;
// only to be used for stdin in PythonInterpreter#setIn(Reader)
// (for JSR 223 support)

package org.python.core;

import java.io.Reader;
import java.io.IOException;
import java.io.BufferedReader;


public class PyFileReader extends PyObject
{
    static final int DEFAULT_BUF_SIZE = 1024;

    private final BufferedReader reader;
    private boolean closed;

    private char[] reuseableBuffer = null;

    public PyFileReader(Reader reader)
    {
        this.reader = (reader instanceof BufferedReader) ? (BufferedReader) reader : new BufferedReader(reader);
        closed = false;
    }

    public boolean closed()
    {
        return closed;
    }

    public void checkClosed()
    {
        if (closed()) {
            throw Py.ValueError("I/O operation on closed file");
        }
    }

    public synchronized void flush()
    {
        checkClosed();
    }

    public void close()
    {
        try {
            if (!closed()) {
                reader.close();
                closed = true;
            }
        } catch (IOException e) {
            throw Py.IOError(e);
        }
    }

    protected char[] needBuffer(int size)
    {
        if (reuseableBuffer == null) {
            if (size > DEFAULT_BUF_SIZE)
                return new char[size];
                
            reuseableBuffer = new char[DEFAULT_BUF_SIZE];
        }

        if (size <= reuseableBuffer.length)
            return reuseableBuffer;

        return new char[size];
    }

    public PyString read(int n)
    {
        if (n < 0) {
            synchronized(reader) {
                checkClosed();

                final StringBuilder sb = new StringBuilder();

                final char[] cbuf = needBuffer(DEFAULT_BUF_SIZE);
                final int buflen = cbuf.length;

                while (true) {
                    try {
                        final int x = reader.read(cbuf, 0, buflen);

                        if (x < 0)
                            break;

                        sb.append(cbuf, 0, x);

                        if (x < buflen)
                            break;
                    } catch (IOException e) {
                        throw Py.IOError(e);
                    }
                }

                return new PyString(sb.toString());
            }
        }

        synchronized(reader) {
            checkClosed();

            final char[] cbuf = needBuffer(n);
            final int buflen = cbuf.length;

            try {
                final int x = reader.read(cbuf, 0, n);

                if (x < 1)
                    return new PyString("");

                return new PyString(new String(cbuf, 0, x));
            } catch (IOException e) {
                throw Py.IOError(e);
            }
        }
    }

    public PyString read()
    {
        return read(-1);
    }

    public PyString readline(int max)
    {
        if (!(max < 0))
            throw Py.NotImplementedError("size argument to readline not implemented for PyFileReader");

        synchronized (reader) {
            try {
                final String line = reader.readLine();

                if (line == null) {
                    return new PyString("");
                } else {
                    return new PyString(line + "\n");
                }
            } catch (IOException e) {
                throw Py.IOError(e);
            }
        }
    }

    public PyString readline()
    {
        return readline(-1);
    }

    public PyObject readlines(final int sizehint) {
        synchronized (reader) {
            checkClosed();
            final PyList list = new PyList();
            int size = 0;
            do {
                final PyString line = readline(-1);
                int len = line.getString().length();
                if (len == 0) {
                    // EOF
                    break;
                }
                size += len;
                list.append(line);
            } while (sizehint <= 0 || size < sizehint);

            return list;
        }
    }

    public PyObject readlines() {
        return readlines(0);
    }


}
