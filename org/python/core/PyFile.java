// Copyright © Corporation for National Research Initiatives
package org.python.core;

import java.io.*;

// To do:
// - readinto(array)
// - modes w, a should disallow reading
// - what to do about buffer size?
// - isatty()
// - fileno() (defined, but always raises an exception, for urllib)
// - name, mode, closed should be read-only

/**
 * A python file wrapper around a java stream, reader/writer or file.
 */

public class PyFile extends PyObject
{
    private static class FileWrapper {
        protected boolean reading;
        protected boolean writing;
        protected boolean binary;

        void setMode(String mode) {
            reading = mode.indexOf('r') >= 0;
            writing = mode.indexOf('w') >= 0 || mode.indexOf("+") >= 0 ||
                      mode.indexOf('a') >= 0;
            binary  = mode.indexOf('b') >= 0;
        }
        public String read(int n) throws java.io.IOException {
            throw new java.io.IOException("file not open for reading");
        }
        public int read() throws java.io.IOException {
            throw new java.io.IOException("file not open for reading");
        }
        public int available() throws java.io.IOException {
            throw new java.io.IOException("file not open for reading");
        }
        public void unread(int c) throws java.io.IOException {
            throw new java.io.IOException("file doesn't support unread");
        }
        public void write(String s) throws java.io.IOException {
            throw new java.io.IOException("file not open for writing");
        }
        public long tell() throws java.io.IOException {
            throw new java.io.IOException("file doesn't support tell/seek");
        }
        public void seek(long pos, int how) throws java.io.IOException {
            throw new java.io.IOException("file doesn't support tell/seek");
        }
        public void flush() throws java.io.IOException {
        }
        public void close() throws java.io.IOException {
        }

        public Object __tojava__(Class cls) throws IOException {
            return null;
        }
        protected byte[] getBytes(String s) {
            // Yes, I known the method is depricated, but it is the fastest
            // way of converting between between byte[] and String
            if (binary) {
                byte[] buf = new byte[s.length()];
                s.getBytes(0, s.length(), buf, 0);
                return buf;
            } else
                return s.getBytes();
        }
        protected String getString(byte[] buf, int offset, int len) {
            // Yes, I known the method is depricated, but it is the fastest
            // way of converting between between byte[] and String
            if (binary) {
                return new String(buf, 0, offset, len);
            } else
                return new String(buf, offset, len);
        }
    }

    private static class InputStreamWrapper extends FileWrapper {
        java.io.InputStream istream;

        public InputStreamWrapper(java.io.InputStream s) {
            istream = s;
        }

        public String read(int n) throws java.io.IOException {
            if (n == 0)
                // nothing to do
                return "";
            if (n < 0) {
                // read until we hit EOF
                byte buf[] = new byte[1024];
                StringBuffer sbuf = new StringBuffer();
                for (int read=0; read >= 0; read=istream.read(buf))
                    sbuf.append(getString(buf, 0, read));
                return sbuf.toString();
            }
            // read the next chunk available, but make sure it's at least
            // one byte so as not to trip the `empty string' return value
            // test done by the caller
            int avail = istream.available();
            //n = (n > avail) ? n : avail;
            byte buf[] = new byte[n];
            int read = istream.read(buf);
            if (read < 0)
                // EOF encountered
                return "";
            return new String(buf, 0, 0, read);
        }

        public int read() throws java.io.IOException {
            return istream.read();
        }

        public int available() throws java.io.IOException {
            return istream.available();
        }

        public void unread(int c) throws java.io.IOException {
            ((java.io.PushbackInputStream)istream).unread(c);
        }

        public void close() throws java.io.IOException {
            istream.close();
        }

        public Object __tojava__(Class cls) throws IOException {
            if (InputStream.class.isAssignableFrom(cls))
                return istream;
            return null;
        }
    }

    private static class OutputStreamWrapper extends FileWrapper {
        private java.io.OutputStream ostream;

        public OutputStreamWrapper(java.io.OutputStream s) {
            ostream = s;
        }

        private static final int MAX_WRITE = 30000;

        public void write(String s) throws java.io.IOException {
            byte[] bytes = getBytes(s);
            int n = bytes.length;
            int i = 0;
            while (i < n) {
                int sz = n-i;
                sz = sz > MAX_WRITE ? MAX_WRITE : sz;
                ostream.write(bytes, i, sz);
                i += sz;
            }
        }

        public void flush() throws java.io.IOException {
            ostream.flush();
        }

        public void close() throws java.io.IOException {
            ostream.close();
        }

        public Object __tojava__(Class cls) throws IOException {
            if (OutputStream.class.isAssignableFrom(cls))
                return ostream;
            return null;
        }
    }

    private static class IOStreamWrapper extends InputStreamWrapper {
        private java.io.OutputStream ostream;

        public IOStreamWrapper(java.io.InputStream istream,
                               java.io.OutputStream ostream) {
            super(istream);
            this.ostream = ostream;
        }

        public void write(String s) throws java.io.IOException {
            ostream.write(getBytes(s));
        }

        public void flush() throws java.io.IOException {
            ostream.flush();
        }

        public void close() throws java.io.IOException {
            ostream.close();
            istream.close();
        }

        public Object __tojava__(Class cls) throws IOException {
            if (OutputStream.class.isAssignableFrom(cls))
                return ostream;
            return super.__tojava__(cls);
        }
    }

    private static class WriterWrapper extends FileWrapper {
        private java.io.Writer writer;

        public WriterWrapper(java.io.Writer s) {
            writer = s;
        }

        private static final int MAX_WRITE = 30000;

        public void write(String s) throws java.io.IOException {
            writer.write(s);
        }

        public void flush() throws java.io.IOException {
            writer.flush();
        }

        public void close() throws java.io.IOException {
            writer.close();
        }
    }

    private static class RFileWrapper extends FileWrapper {
        /** The default buffer size, in bytes. */
        protected static final int defaultBufferSize = 4096;

        /** The underlying java.io.RandomAccessFile. */
        protected java.io.RandomAccessFile file;

        /** The offset in bytes from the file start, of the next read or
         *  write operation. */
        protected long filePosition;

        /** The buffer used to load the data. */
        protected byte buffer[];

        /** The offset in bytes of the start of the buffer, from the start
         *  of the file. */
        protected long bufferStart;

        /** The offset in bytes of the end of the data in the buffer, from
         *  the start of the file. This can be calculated from
         *  <code>bufferStart + dataSize</code>, but it is cached to speed
         *  up the read( ) method. */
        protected long dataEnd;

        /** The size of the data stored in the buffer, in bytes. This may be
         *  less than the size of the buffer.*/
        protected int dataSize;

        /** True if we are at the end of the file. */
        protected boolean endOfFile;

        /** True if the data in the buffer has been modified. */
        boolean bufferModified = false;

        public RFileWrapper(java.io.RandomAccessFile file) {
            this(file, 8092);
        }

        public RFileWrapper(java.io.RandomAccessFile file, int bufferSize) {
            this.file = file;
            bufferStart = 0;
            dataEnd = 0;
            dataSize = 0;
            filePosition = 0;
            buffer = new byte[bufferSize];
            endOfFile = false;
        }

        public String read(int n) throws java.io.IOException {
            if (n < 0) {
                n = (int)(file.length() - file.getFilePointer());
                if (n < 0)
                    n = 0;
            }
            byte[] buf = new byte[n];
            n = readBytes(buf, 0, n);
            if (n < 0)
                n = 0;
            return getString(buf, 0, n);
        }


        private int readBytes( byte b[], int off, int len )
             throws IOException
        {
            // Check for end of file.
            if( endOfFile )
                return -1;

            // See how many bytes are available in the buffer - if none,
            // seek to the file position to update the buffer and try again.
            int bytesAvailable = (int)(dataEnd - filePosition);
            if (bytesAvailable < 1) {
                seek(filePosition, 0);
                return readBytes( b, off, len );
            }

            // Copy as much as we can.
            int copyLength = (bytesAvailable >= len) ? len : bytesAvailable;
            System.arraycopy(buffer, (int)(filePosition - bufferStart),
                             b, off, copyLength);
            filePosition += copyLength;

            // If there is more to copy...
            if (copyLength < len) {
                int extraCopy = len - copyLength;

                // If the amount remaining is more than a buffer's
                // length, read it directly from the file.
                if (extraCopy > buffer.length) {
                    file.seek(filePosition);
                    extraCopy = file.read(b, off + copyLength,
                                          len - copyLength);
                } else {
                    // ...or read a new buffer full, and copy as much
                    // as possible...
                    seek(filePosition, 0);
                    if (!endOfFile) {
                        extraCopy = (extraCopy > dataSize) ?
                                        dataSize : extraCopy;
                        System.arraycopy(buffer, 0, b, off + copyLength,
                                         extraCopy);
                    } else {
                        extraCopy = -1;
                    }
                }

                // If we did manage to copy any more, update the file
                // position and return the amount copied.
                if (extraCopy > 0) {
                    filePosition += extraCopy;
                    return copyLength + extraCopy;
                }
            }

            // Return the amount copied.
            return copyLength;
        }


        public int read() throws java.io.IOException {
            // If the file position is within the data, return the byte...
            if (filePosition < dataEnd) {
                return (int)(buffer[(int)(filePosition++ - bufferStart)]
                                   & 0xff);
            } else if (endOfFile) {
               // ...or should we indicate EOF...
                return -1;
            } else {
                // ...or seek to fill the buffer, and try again.
                seek(filePosition, 0);
                return read();
            }
        }

        public int available() throws java.io.IOException {
            return 1;
        }

        public void unread(int c) throws java.io.IOException {
            filePosition--;
        }

        public void write(String s) throws java.io.IOException {
            byte[] b = getBytes(s);
            int len = b.length;

            // If the amount of data is small (less than a full buffer)...
            if (len < buffer.length) {
                // If any of the data fits within the buffer...
                int spaceInBuffer = 0;
                int copyLength = 0;
                if (filePosition >= bufferStart)
                    spaceInBuffer = (int)((bufferStart + buffer.length) -
                                          filePosition);
                if (spaceInBuffer > 0) {
                    // Copy as much as possible to the buffer.
                    copyLength = (spaceInBuffer > len) ?
                                       len : spaceInBuffer;
                    System.arraycopy(b, 0, buffer,
                                     (int)(filePosition - bufferStart),
                                     copyLength );
                    bufferModified = true;
                    long myDataEnd = filePosition + copyLength;
                    dataEnd = myDataEnd > dataEnd ? myDataEnd : dataEnd;
                    dataSize = (int)(dataEnd - bufferStart);
                    filePosition += copyLength;
                }

                // If there is any data remaining, move to the
                // new position and copy to the new buffer.
                if (copyLength < len) {
                    seek(filePosition, 0);
                    System.arraycopy(b, copyLength, buffer,
                                     (int)(filePosition - bufferStart),
                                     len - copyLength);
                    bufferModified = true;
                    long myDataEnd = filePosition + (len - copyLength);
                    dataEnd = myDataEnd > dataEnd ? myDataEnd : dataEnd;
                    dataSize = (int)(dataEnd - bufferStart);
                    filePosition += (len - copyLength);
                }
            } else {
                // ...or write a lot of data...

                // Flush the current buffer, and write this data to the file.
                if (bufferModified) {
                    flush( );
                    bufferStart = dataEnd = dataSize = 0;
                }
                file.write( b, 0, len );
                filePosition += len;
            }
        }

        public long tell() throws java.io.IOException {
            return filePosition;
        }

        public void seek(long pos, int how) throws java.io.IOException {
            if (how == 1)
                pos += filePosition;
            else if (how == 2)
                pos += file.length();
            if (pos < 0)
                pos = 0;

            // If the seek is into the buffer, just update the file pointer.
            if (pos >= bufferStart && pos < dataEnd) {
                filePosition = pos;
                endOfFile = false;
                return;
            }

            // If the current buffer is modified, write it to disk.
            if (bufferModified)
                flush();

            // Move to the position on the disk.
            file.seek(pos);
            filePosition = file.getFilePointer();
            bufferStart = filePosition;

            // Fill the buffer from the disk.
            dataSize = file.read(buffer);
            if (dataSize < 0) {
                dataSize = 0;
                endOfFile = true;
            } else {
                endOfFile = false;
            }

            // Cache the position of the buffer end.
            dataEnd = bufferStart + dataSize;
        }

        public void flush() throws java.io.IOException {
            file.seek(bufferStart);
            file.write(buffer, 0, dataSize);
            bufferModified = false;
            file.getFD().sync();
        }

        public void close() throws java.io.IOException {
            if (writing && bufferModified) {
                file.seek(bufferStart);
                file.write(buffer, 0, (int)dataSize);
            }

            file.close();
        }

        public Object __tojava__(Class cls) throws IOException {
            if (OutputStream.class.isAssignableFrom(cls) && writing)
                return new FileOutputStream(file.getFD());
            else if (InputStream.class.isAssignableFrom(cls) && reading)
                return new FileInputStream(file.getFD());
            return super.__tojava__(cls);
        }

    }

    private static class TextWrapper extends FileWrapper {
        private FileWrapper file;
        private String sep;
        private boolean sep_is_nl;

        public TextWrapper(FileWrapper file) {
            this.file = file;
            sep = System.getProperty("line.separator");
            sep_is_nl = (sep == "\n");
        }

        public String read(int n) throws java.io.IOException {
            String s = this.file.read(n);
            int index = s.indexOf('\r');
            if (index < 0)
                return s;
            StringBuffer buf = new StringBuffer();
            int start = 0;
            int end = s.length();
            do {
                buf.append(s.substring(start, index));
                buf.append('\n');
                start = index + 1;
                if (start < end && s.charAt(start) == '\n')
                    start++;
                index = s.indexOf('\r', start);
            } while (index >= 0);
            buf.append(s.substring(start));
            if (s.endsWith("\r") && file.available() > 0) {
                int c = file.read();
                if (c != -1 && c != '\n')
                    file.unread(c);
            }
            return buf.toString();
        }

        public int read() throws java.io.IOException {
            int c = file.read();
            if (c != '\r')
                return c;
            if (file.available() > 0) {
                c = file.read();
                if (c != -1 && c != '\n')
                    file.unread(c);
            }
            return '\n';
        }

        public void write(String s) throws java.io.IOException {
            if (!sep_is_nl) {
                int index = s.indexOf('\n');
                if (index >= 0) {
                    StringBuffer buf = new StringBuffer();
                    int start = 0;
                    do {
                        buf.append(s.substring(start, index));
                        buf.append(sep);
                        start = index + 1;
                        index = s.indexOf('\n', start);
                    } while (index >= 0);
                    buf.append(s.substring(start));
                    s = buf.toString();
                }
            }
            this.file.write(s);
        }

        public long tell() throws java.io.IOException {
            return file.tell();
        }

        public void seek(long pos, int how) throws java.io.IOException {
            file.seek(pos, how);
        }

        public void flush() throws java.io.IOException {
            file.flush();
        }

        public void close() throws java.io.IOException {
            file.close();
        }

        public Object __tojava__(Class cls) throws IOException {
            return file.__tojava__(cls);
        }
    }

    public String name;
    public String mode;
    public boolean softspace;
    public boolean closed;

    private FileWrapper file;

    private static java.io.InputStream _pb(java.io.InputStream s, String mode)
    {
        if (mode.indexOf('b') < 0) {
            try {
                s = (java.io.PushbackInputStream)s;
            } catch (ClassCastException e) {
                s = new java.io.PushbackInputStream(s);
            }
        }
        return s;
    }

    public PyFile(FileWrapper file, String name, String mode) {
        file.setMode(mode);
        this.name = name;
        this.mode = mode;
        this.softspace = false;
        this.closed = false;
        if (mode.indexOf('b') < 0)
            this.file = new TextWrapper(file);
        else
            this.file = file;
    }

    public PyFile(java.io.InputStream istream, java.io.OutputStream ostream,
                  String name, String mode)
    {
        this(new IOStreamWrapper(_pb(istream, mode), ostream), name, mode);
    }

    public PyFile(java.io.InputStream istream, java.io.OutputStream ostream,
                  String name)
    {
        this(istream, ostream, name, "r+");
    }

    public PyFile(java.io.InputStream istream, java.io.OutputStream ostream) {
        this(istream, ostream, "<???>", "r+");
    }

    public PyFile(java.io.InputStream istream, String name, String mode) {
        this(new InputStreamWrapper(_pb(istream, mode)), name, mode);
    }

    public PyFile(java.io.InputStream istream, String name) {
        this(istream, name, "r");
    }

    public PyFile(java.io.InputStream istream) {
        this(istream, "<???>", "r");
    }

    public PyFile(java.io.OutputStream ostream, String name, String mode) {
        this(new OutputStreamWrapper(ostream), name, mode);
    }

    public PyFile(java.io.OutputStream ostream, String name) {
        this(ostream, name, "w");
    }

    public PyFile(java.io.OutputStream ostream) {
        this(ostream, "<???>", "w");
    }

    public PyFile(java.io.Writer ostream, String name, String mode) {
        this(new WriterWrapper(ostream), name, mode);
    }

    public PyFile(java.io.Writer ostream, String name) {
        this(ostream, name, "w");
    }

    public PyFile(java.io.Writer ostream) {
        this(ostream, "<???>", "w");
    }

    public PyFile(java.io.RandomAccessFile file, String name, String mode) {
        this(new RFileWrapper(file), name, mode);
    }

    public PyFile(java.io.RandomAccessFile file, String name) {
        this(file, name, "r+");
    }

    public PyFile(java.io.RandomAccessFile file) {
        this(file, "<???>", "r+");
    }

    public PyFile(String name, String mode, int bufsize) {
        this(_setup(name, mode, bufsize), name, mode);
    }

    public void __setattr__(String name, PyObject value) {
        // softspace is the only writeable file object attribute
        if (name == "softspace")
            softspace = value.__nonzero__();
        else if (name == "mode" || name == "closed" || name == "name")
            throw Py.TypeError("readonly attribute: " + name);
        else
            throw Py.AttributeError(name);
    }

    public Object __tojava__(Class cls) {
        Object o = null;
        try {
            o = file.__tojava__(cls);
        } catch (java.io.IOException exc) { }
        if (o == null)
            o = super.__tojava__(cls);
        return o;
    }


    private static FileWrapper _setup(String name, String mode, int bufsize) {
        char c1 = ' ';
        char c2 = ' ';
        char c3 = ' ';
        int n = mode.length();
        if (n > 0) {
            c1 = mode.charAt(0);
            if (n > 1) {
                c2 = mode.charAt(1);
                if (n > 2)
                    c3 = mode.charAt(2);
            }
        }
        String jmode = "r";
        if (c1 == 'r') {
            if (c2 == '+' || c3 == '+') jmode = "rw";
            else jmode = "r";
        }
        else if (c1 == 'w' || c1 == 'a') jmode = "rw";
        try {
            java.io.File f = new java.io.File(name);
            if (c1 == 'w') {
                // Hack to truncate the file without deleting it:
                // create a FileOutputStream for it and close it again.
                java.io.FileOutputStream fo = new java.io.FileOutputStream(f);
                fo.close();
                fo = null;
            }
            // What about bufsize?
            java.io.RandomAccessFile rfile =
                new java.io.RandomAccessFile(f, jmode);
            RFileWrapper iofile = new RFileWrapper(rfile);
            if (c1 == 'a')
                iofile.seek(0, 2);
            return iofile;
        } catch (java.io.IOException e) {
            throw Py.IOError(e);
        }
    }

    public PyString read(int n) {
        if (closed)
            err_closed();
        StringBuffer data = new StringBuffer();
        try {
            while (n != 0) {
                String s = file.read(n);
                int len = s.length();
                if (len == 0)
                    break;
                data.append(s);
                if (n > 0) {
                    n -= len;
                    if (n <= 0)
                        break;
                }
            }
        } catch (java.io.IOException e) {
            throw Py.IOError(e);
        }
        return new PyString(data.toString());
    }

    public PyString read() {
        return read(-1);
    }

    public PyString readline(int max) {
        if (closed)
            err_closed();
        StringBuffer s = new StringBuffer();
        while (max < 0 || s.length() < max) {
            int c;
            try {
                c = file.read();
            } catch (java.io.IOException e) {
                throw Py.IOError(e);
            }
            if (c < 0)
                break;
            s.append((char)c);
            if ((char)c == '\n')
                break;
        }
        return new PyString(s.toString());
    }

    public PyString readline() {
        return readline(-1);
    }

    public PyObject readlines(int sizehint) {
        if (closed)
            err_closed();
        PyList list = new PyList();
        int bytesread = 0;
        for (;;) {
            PyString s = readline();
            int len = s.__len__();
            if (len == 0)
                // EOF
                break;
            bytesread += len;
            list.append(s);
            if (sizehint > 0 && bytesread > sizehint)
                break;
        }
        return list;
    }

    public PyObject readlines() {
        return readlines(0);
    }

    public void write(String s) {
        if (closed)
            err_closed();
        try {
            file.write(s);
            softspace = false;
        } catch (java.io.IOException e) {
            throw Py.IOError(e);
        }
    }

    public void writelines(PyObject a) {
        PyObject item = null;
        for (int i = 0; (item = a.__finditem__(i)) != null; i++) {
            if (!(item instanceof PyString))
                throw Py.TypeError("writelines() argument must be a " +
                                   "sequence of strings");
            write(item.toString());
        }
    }

    public long tell() {
        if (closed)
            err_closed();
        try {
            return file.tell();
        } catch (java.io.IOException e) {
            throw Py.IOError(e);
        }
    }

    public void seek(long pos, int how) {
        if (closed)
            err_closed();
        try {
            file.seek(pos, how);
        } catch (java.io.IOException e) {
            throw Py.IOError(e);
        }
    }

    public void seek(long pos) {
        seek(pos, 0);
    }

    public void flush() {
        if (closed)
            err_closed();
        try {
            file.flush();
        } catch (java.io.IOException e) {
            throw Py.IOError(e);
        }
    }

    public void close() {
        try {
            file.close();
        } catch (java.io.IOException e) {
            throw Py.IOError(e);
        }
        closed = true;
        file = new FileWrapper();
    }

    // TBD: should this be removed?  I think it's better to raise an
    // AttributeError than an IOError here.
    public PyObject fileno() {
        throw Py.IOError("fileno() is not supported in jpython");
    }

    public String toString() {
        return "<file " + name + ", mode " + mode + " at " +
               Py.id(this) + ">";
    }

    private void err_closed() {
        throw Py.ValueError("I/O operation on closed file");
    }
}
