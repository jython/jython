// Copyright © Corporation for National Research Initiatives
package org.python.core;

// To do:
// - readlines(sizehint)
// - readinto(array)
// - modes w, a should disallow reading
// - what to do about buffer size?
// - isatty()
// - fileno() (defined, but always raises an exception, for urllib)
// - name, mode, closed should be read-only

public class PyFile extends PyObject
{
    private static class FileWrapper {
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
                    sbuf.append(new String(buf, 0, 0, read));
                return sbuf.toString();
            }
            // read the next chunk available, but make sure it's at least
            // one byte so as not to trip the `empty string' return value
            // test done by the caller
            int avail = istream.available();
            n = (n > avail) ? n : avail;
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
    }

    private static class OutputStreamWrapper extends FileWrapper {
        private java.io.OutputStream ostream;

        public OutputStreamWrapper(java.io.OutputStream s) {
            ostream = s;
        }

        private static final int MAX_WRITE = 30000;

        public void write(String s) throws java.io.IOException {
            byte[] bytes = s.getBytes();
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
    }

    private static class IOStreamWrapper extends InputStreamWrapper {
        private java.io.OutputStream ostream;

        public IOStreamWrapper(java.io.InputStream istream,
                               java.io.OutputStream ostream) {
            super(istream);
            this.ostream = ostream;
        }

        public void write(String s) throws java.io.IOException {
            ostream.write(s.getBytes());
        }

        public void flush() throws java.io.IOException {
            ostream.flush();
        }

        public void close() throws java.io.IOException {
            ostream.close();
            istream.close();
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
        java.io.RandomAccessFile file;

        public RFileWrapper(java.io.RandomAccessFile file) {
            this.file = file;
        }

        public String read(int n) throws java.io.IOException {
            if (n < 0) {
                n = (int)(file.length() - file.getFilePointer());
                if (n < 0)
                    n = 0;
            }
            byte[] buf = new byte[n];
            n = file.read(buf);
            if (n < 0)
                n = 0;
            return new String(buf, 0, 0, n);
        }

        public int read() throws java.io.IOException {
            return file.read();
        }

        public int available() throws java.io.IOException {
            return 1;
        }

        public void unread(int c) throws java.io.IOException {
            file.seek(file.getFilePointer() - 1);
        }

        public void write(String s) throws java.io.IOException {
            file.write(s.getBytes());
        }

        public long tell() throws java.io.IOException {
            return file.getFilePointer();
        }

        public void seek(long pos, int how) throws java.io.IOException {
            if (how == 1)
                pos += file.getFilePointer();
            else if (how == 2)
                pos += file.length();
            if (pos < 0)
                pos = 0;
            file.seek(pos);
        }

        public void flush() throws java.io.IOException {
            file.getFD().sync();
        }

        public void close() throws java.io.IOException {
            file.close();
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
            if (c1 == 'a')
                rfile.seek(rfile.length());
            RFileWrapper iofile = new RFileWrapper(rfile);
            return iofile;
        } catch (java.io.IOException e) {
            throw Py.IOError(e);
        }
    }

    public PyString read(int n) {
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
        try {                
            file.write(s);
            softspace = false;
        } catch (java.io.IOException e) {
            throw Py.IOError(e);
        }
    }

    public void writelines(PyList a) {
        int n = a.__len__();
        for (int i = 0; i < n; i++)
            write(a.__getitem__(i).toString());
    }

    public long tell() {
        try {
            return file.tell();
        } catch (java.io.IOException e) {
            throw Py.IOError(e);
        }
    }

    public void seek(long pos, int how) {
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
        return "<file " + name + ", mode " + mode + " at " + Py.id(this) + ">";
    }
}
