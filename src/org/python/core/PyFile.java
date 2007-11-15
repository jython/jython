// Copyright (c) Corporation for National Research Initiatives
package org.python.core;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.Writer;
import java.util.LinkedList;

import org.python.core.io.BinaryIOWrapper;
import org.python.core.io.BufferedIOBase;
import org.python.core.io.BufferedRandom;
import org.python.core.io.BufferedReader;
import org.python.core.io.BufferedWriter;
import org.python.core.io.FileIO;
import org.python.core.io.IOBase;
import org.python.core.io.LineBufferedRandom;
import org.python.core.io.LineBufferedWriter;
import org.python.core.io.RawIOBase;
import org.python.core.io.ReaderWriterIO;
import org.python.core.io.StreamIO;
import org.python.core.io.TextIOBase;
import org.python.core.io.TextIOWrapper;
import org.python.core.io.UniversalIOWrapper;

/**
 * A python file wrapper around a java stream, reader/writer or file.
 */
public class PyFile extends PyObject
{
    //~ BEGIN GENERATED REGION -- DO NOT EDIT SEE gexpose.py
    /* type info */

    public static final String exposed_name="file";

    public static final Class exposed_base=PyObject.class;

    public static void typeSetup(PyObject dict,PyType.Newstyle marker) {
        dict.__setitem__("mode",new PyGetSetDescr("mode",PyFile.class,"getMode",null,null));
        dict.__setitem__("name",new PyGetSetDescr("name",PyFile.class,"getName",null,null));
        dict.__setitem__("closed",new PyGetSetDescr("closed",PyFile.class,"getClosed",null,null));
        dict.__setitem__("newlines",new PyGetSetDescr("newlines",PyFile.class,"getNewlines",null,null));
        dict.__setitem__("softspace",new PyGetSetDescr("softspace",PyFile.class,"getSoftspace","setSoftspace","delSoftspace"));
        class exposed___cmp__ extends PyBuiltinMethodNarrow {

            exposed___cmp__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___cmp__(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                int ret=((PyFile)self).file___cmp__(arg0);
                if (ret==-2) {
                    throw Py.TypeError("file"+".__cmp__(x,y) requires y to be '"+"file"+"', not a '"+(arg0).getType().fastGetName()+"'");
                }
                return Py.newInteger(ret);
            }

        }
        dict.__setitem__("__cmp__",new PyMethodDescr("__cmp__",PyFile.class,1,1,new exposed___cmp__(null,null)));
        class exposed___iter__ extends PyBuiltinMethodNarrow {

            exposed___iter__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___iter__(self,info);
            }

            public PyObject __call__() {
                return((PyFile)self).file___iter__();
            }

        }
        dict.__setitem__("__iter__",new PyMethodDescr("__iter__",PyFile.class,0,0,new exposed___iter__(null,null)));
        class exposed___iternext__ extends PyBuiltinMethodNarrow {

            exposed___iternext__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___iternext__(self,info);
            }

            public PyObject __call__() {
                return((PyFile)self).file___iternext__();
            }

        }
        dict.__setitem__("__iternext__",new PyMethodDescr("__iternext__",PyFile.class,0,0,new exposed___iternext__(null,null)));
        class exposed___nonzero__ extends PyBuiltinMethodNarrow {

            exposed___nonzero__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___nonzero__(self,info);
            }

            public PyObject __call__() {
                return Py.newBoolean(((PyFile)self).file___nonzero__());
            }

        }
        dict.__setitem__("__nonzero__",new PyMethodDescr("__nonzero__",PyFile.class,0,0,new exposed___nonzero__(null,null)));
        class exposed___repr__ extends PyBuiltinMethodNarrow {

            exposed___repr__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___repr__(self,info);
            }

            public PyObject __call__() {
                return new PyString(((PyFile)self).file_toString());
            }

        }
        dict.__setitem__("__repr__",new PyMethodDescr("__repr__",PyFile.class,0,0,new exposed___repr__(null,null)));
        class exposed___str__ extends PyBuiltinMethodNarrow {

            exposed___str__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___str__(self,info);
            }

            public PyObject __call__() {
                return new PyString(((PyFile)self).file_toString());
            }

        }
        dict.__setitem__("__str__",new PyMethodDescr("__str__",PyFile.class,0,0,new exposed___str__(null,null)));
        class exposed_close extends PyBuiltinMethodNarrow {

            exposed_close(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_close(self,info);
            }

            public PyObject __call__() {
                ((PyFile)self).file_close();
                return Py.None;
            }

        }
        dict.__setitem__("close",new PyMethodDescr("close",PyFile.class,0,0,new exposed_close(null,null)));
        class exposed_flush extends PyBuiltinMethodNarrow {

            exposed_flush(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_flush(self,info);
            }

            public PyObject __call__() {
                ((PyFile)self).file_flush();
                return Py.None;
            }

        }
        dict.__setitem__("flush",new PyMethodDescr("flush",PyFile.class,0,0,new exposed_flush(null,null)));
        class exposed_read extends PyBuiltinMethodNarrow {

            exposed_read(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_read(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                try {
                    return new PyString(((PyFile)self).file_read(arg0.asInt(0)));
                } catch (PyObject.ConversionException e) {
                    String msg;
                    switch (e.index) {
                    case 0:
                        msg="expected an integer";
                        break;
                    default:
                        msg="xxx";
                    }
                    throw Py.TypeError(msg);
                }
            }

            public PyObject __call__() {
                return new PyString(((PyFile)self).file_read());
            }

        }
        dict.__setitem__("read",new PyMethodDescr("read",PyFile.class,0,1,new exposed_read(null,null)));
        class exposed_readinto extends PyBuiltinMethodNarrow {

            exposed_readinto(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_readinto(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                return Py.newInteger(((PyFile)self).file_readinto(arg0));
            }

        }
        dict.__setitem__("readinto",new PyMethodDescr("readinto",PyFile.class,1,1,new exposed_readinto(null,null)));
        class exposed_readline extends PyBuiltinMethodNarrow {

            exposed_readline(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_readline(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                try {
                    return new PyString(((PyFile)self).file_readline(arg0.asInt(0)));
                } catch (PyObject.ConversionException e) {
                    String msg;
                    switch (e.index) {
                    case 0:
                        msg="expected an integer";
                        break;
                    default:
                        msg="xxx";
                    }
                    throw Py.TypeError(msg);
                }
            }

            public PyObject __call__() {
                return new PyString(((PyFile)self).file_readline());
            }

        }
        dict.__setitem__("readline",new PyMethodDescr("readline",PyFile.class,0,1,new exposed_readline(null,null)));
        class exposed_readlines extends PyBuiltinMethodNarrow {

            exposed_readlines(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_readlines(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                try {
                    return((PyFile)self).file_readlines(arg0.asInt(0));
                } catch (PyObject.ConversionException e) {
                    String msg;
                    switch (e.index) {
                    case 0:
                        msg="expected an integer";
                        break;
                    default:
                        msg="xxx";
                    }
                    throw Py.TypeError(msg);
                }
            }

            public PyObject __call__() {
                return((PyFile)self).file_readlines();
            }

        }
        dict.__setitem__("readlines",new PyMethodDescr("readlines",PyFile.class,0,1,new exposed_readlines(null,null)));
        class exposed_seek extends PyBuiltinMethodNarrow {

            exposed_seek(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_seek(self,info);
            }

            public PyObject __call__(PyObject arg0,PyObject arg1) {
                try {
                    ((PyFile)self).file_seek(arg0.asLong(0),arg1.asInt(1));
                    return Py.None;
                } catch (PyObject.ConversionException e) {
                    String msg;
                    switch (e.index) {
                    case 0:
                        msg="expected a long";
                        break;
                    case 1:
                        msg="expected an integer";
                        break;
                    default:
                        msg="xxx";
                    }
                    throw Py.TypeError(msg);
                }
            }

            public PyObject __call__(PyObject arg0) {
                try {
                    ((PyFile)self).file_seek(arg0.asLong(0));
                    return Py.None;
                } catch (PyObject.ConversionException e) {
                    String msg;
                    switch (e.index) {
                    case 0:
                        msg="expected a long";
                        break;
                    default:
                        msg="xxx";
                    }
                    throw Py.TypeError(msg);
                }
            }

        }
        dict.__setitem__("seek",new PyMethodDescr("seek",PyFile.class,1,2,new exposed_seek(null,null)));
        class exposed_tell extends PyBuiltinMethodNarrow {

            exposed_tell(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_tell(self,info);
            }

            public PyObject __call__() {
                return new PyLong(((PyFile)self).file_tell());
            }

        }
        dict.__setitem__("tell",new PyMethodDescr("tell",PyFile.class,0,0,new exposed_tell(null,null)));
        class exposed_next extends PyBuiltinMethodNarrow {

            exposed_next(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_next(self,info);
            }

            public PyObject __call__() {
                return((PyFile)self).file_next();
            }

        }
        dict.__setitem__("next",new PyMethodDescr("next",PyFile.class,0,0,new exposed_next(null,null)));
        class exposed_truncate extends PyBuiltinMethodNarrow {

            exposed_truncate(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_truncate(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                try {
                    ((PyFile)self).file_truncate(arg0.asLong(0));
                    return Py.None;
                } catch (PyObject.ConversionException e) {
                    String msg;
                    switch (e.index) {
                    case 0:
                        msg="expected a long";
                        break;
                    default:
                        msg="xxx";
                    }
                    throw Py.TypeError(msg);
                }
            }

            public PyObject __call__() {
                ((PyFile)self).file_truncate();
                return Py.None;
            }

        }
        dict.__setitem__("truncate",new PyMethodDescr("truncate",PyFile.class,0,1,new exposed_truncate(null,null)));
        class exposed_write extends PyBuiltinMethodNarrow {

            exposed_write(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_write(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                ((PyFile)self).file_write(arg0);
                return Py.None;
            }

        }
        dict.__setitem__("write",new PyMethodDescr("write",PyFile.class,1,1,new exposed_write(null,null)));
        class exposed_writelines extends PyBuiltinMethodNarrow {

            exposed_writelines(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_writelines(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                ((PyFile)self).file_writelines(arg0);
                return Py.None;
            }

        }
        dict.__setitem__("writelines",new PyMethodDescr("writelines",PyFile.class,1,1,new exposed_writelines(null,null)));
        class exposed_xreadlines extends PyBuiltinMethodNarrow {

            exposed_xreadlines(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_xreadlines(self,info);
            }

            public PyObject __call__() {
                return((PyFile)self).file_xreadlines();
            }

        }
        dict.__setitem__("xreadlines",new PyMethodDescr("xreadlines",PyFile.class,0,0,new exposed_xreadlines(null,null)));
        class exposed___init__ extends PyBuiltinMethod {

            exposed___init__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___init__(self,info);
            }

            public PyObject __call__(PyObject[]args) {
                return __call__(args,Py.NoKeywords);
            }

            public PyObject __call__(PyObject[]args,String[]keywords) {
                ((PyFile)self).file_init(args,keywords);
                return Py.None;
            }

        }
        dict.__setitem__("__init__",new PyMethodDescr("__init__",PyFile.class,-1,-1,new exposed___init__(null,null)));
        dict.__setitem__("__new__",new PyNewWrapper(PyFile.class,"__new__",-1,-1) {

                                                                                      public PyObject new_impl(boolean init,PyType subtype,PyObject[]args,String[]keywords) {
                                                                                          PyFile newobj;
                                                                                          if (for_type == subtype) {
                                                                                              newobj = null;
                                                                                              if (init) {
                                                                                                  if (args.length == 0) {
                                                                                                      newobj = new PyFile();
                                                                                                      newobj.file_init(args, keywords);
                                                                                                  } else if (args[0] instanceof PyString ||
                                                                                                          (args[0] instanceof PyJavaInstance &&
                                                                                                          ((PyJavaInstance) args[0]).javaProxy == String.class)) {
                                                                                                      // If first arg is a PyString or String, assume its being 
                                                                                                      // called as a builtin.
                                                                                                      newobj = new PyFile();
                                                                                                      newobj.file_init(args, keywords);
                                                                                                      newobj.closer = new Closer(newobj.file);
                                                                                                  } else {
                                                                                                      // assume it's being called as a java class
                                                                                                      PyJavaClass pjc = new PyJavaClass(PyFile.class);
                                                                                                      newobj = (PyFile) pjc.__call__(args, keywords);
                                                                                                  }
                                                                                              } else {
                                                                                                  newobj = new PyFile();
                                                                                              }
                                                                                          } else {
                                                                                              newobj = new PyFileDerived(subtype);
                                                                                          }
                                                                                          return newobj;
                                                                                      }

                                                                                  });
    }
    //~ END GENERATED REGION -- DO NOT EDIT SEE gexpose.py

    /** The filename */
    private PyObject name;

    /** The mode string */
    public String mode;

    /** Indicator dictating whether a space should be written to this
     * file on the next print statement (not currently implemented in
     * print ) */
    public boolean softspace = false;

    /** Whether this file is opened for reading */
    private boolean reading = false;

    /** Whether this file is opened for writing */
    private boolean writing = false;

    /** Whether this file is opened in appending mode */
    private boolean appending = false;

    /** Whether this file is opened for updating */
    private boolean updating = false;

    /** Whether this file is opened in binary mode */
    private boolean binary = false;

    /** Whether this file is opened in universal newlines mode */
    private boolean universal = false;

    /** The underlying IO object */
    private TextIOBase file;

    /** The file's closer object; ensures the file is closed at
     * shutdown */
    private Closer closer;

    /** All PyFiles' closers */
    private static LinkedList closers = new LinkedList();

    static {
        initCloser();
    }

    public PyFile() {
    }

    public PyFile(PyType subType) {
        super(subType);
    }

    public PyFile(RawIOBase raw, String name, String mode, int bufsize) {
        parseMode(mode);
        file_init(raw, name, mode, bufsize);
    }

    public PyFile(InputStream istream, OutputStream ostream, String name,
                  String mode, int bufsize, boolean closefd) {
        parseMode(mode);
        file_init(new StreamIO(istream, ostream, closefd), name, mode, bufsize);
    }

    public PyFile(InputStream istream, OutputStream ostream, String name,
                  String mode, int bufsize)
    {
        this(istream, ostream, name, mode, -1, true);
    }

    public PyFile(InputStream istream, OutputStream ostream, String name,
                  String mode)
    {
        this(istream, ostream, name, mode, -1);
    }

    public PyFile(InputStream istream, OutputStream ostream, String name)
    {
        this(istream, ostream, name, "r+");
    }

    public PyFile(InputStream istream, OutputStream ostream) {
        this(istream, ostream, "<???>", "r+");
    }

    public PyFile(InputStream istream, String name, String mode, int bufsize,
                  boolean closefd) {
        parseMode(mode);
        file_init(new StreamIO(istream, closefd), name, mode, bufsize);
    }

    public PyFile(InputStream istream, String name, String mode, int bufsize) {
        this(istream, name, mode, -1, true);
    }

    public PyFile(InputStream istream, String name, String mode) {
        this(istream, name, mode, -1);
    }

    public PyFile(InputStream istream, String name) {
        this(istream, name, "r");
    }

    public PyFile(InputStream istream) {
        this(istream, "<???>", "r");
    }

    public PyFile(OutputStream ostream, String name, String mode, int bufsize,
                  boolean closefd) {
        parseMode(mode);
        file_init(new StreamIO(ostream, closefd), name, mode, bufsize);
    }

    public PyFile(OutputStream ostream, String name, String mode, int bufsize) {
        this(ostream, name, mode, -1, true);
    }

    public PyFile(OutputStream ostream, String name, String mode) {
        this(ostream, name, mode, -1);
    }

    public PyFile(OutputStream ostream, String name) {
        this(ostream, name, "w");
    }

    public PyFile(OutputStream ostream) {
        this(ostream, "<???>", "w");
    }

    public PyFile(Writer ostream, String name, String mode, int bufsize) {
        parseMode(mode);
        file_init(new ReaderWriterIO(ostream), name, mode, bufsize);
    }

    public PyFile(Writer ostream, String name, String mode) {
        this(ostream, name, mode, -1);
    }

    public PyFile(Writer ostream, String name) {
        this(ostream, name, "w");
    }

    public PyFile(Writer ostream) {
        this(ostream, "<???>", "w");
    }

    public PyFile(RandomAccessFile file, String name, String mode, int bufsize) {
        file_init(new FileIO(file.getChannel(), parseMode(mode)), name, mode, bufsize);
    }

    public PyFile(RandomAccessFile file, String name, String mode) {
        this(file, name, mode, -1);
    }

    public PyFile(RandomAccessFile file, String name) {
        this(file, name, "r+");
    }

    public PyFile(RandomAccessFile file) {
        this(file, "<???>", "r+");
    }

    public PyFile(String name, String mode, int bufsize) {
        file_init(new FileIO(name, parseMode(mode)), name, mode, bufsize);
    }

    final void file_init(PyObject[] args,String[] kwds) {
        ArgParser ap = new ArgParser("file", args, kwds,
                                     new String[] { "name", "mode", "bufsize" }, 1);
        PyObject name = ap.getPyObject(0);
        if (!(name instanceof PyString)) {
            throw Py.TypeError("coercing to Unicode: need string, '" +
                               name.getType().getFullName() + "'type found");
        }
        String mode = ap.getString(1, "r");
        int bufsize = ap.getInt(2, -1);
        file_init(new FileIO(name.toString(), parseMode(mode)), name, mode, bufsize);
    }

    private void file_init(RawIOBase raw, String name, String mode, int bufsize) {
        file_init(raw, new PyString(name), mode, bufsize);
    }

    private void file_init(RawIOBase raw, PyObject name, String mode, int bufsize) {
        this.name = name;
        this.mode = mode;

        BufferedIOBase buffer = createBuffer(raw, bufsize);
        if (universal) {
            this.file = new UniversalIOWrapper(buffer);
        } else if (!binary) {
            this.file = new TextIOWrapper(buffer);
        } else {
            this.file = new BinaryIOWrapper(buffer);
        }
    }

    /**
     * Wrap the given RawIOBase with a BufferedIOBase according to the
     * mode and given bufsize.
     *
     * @param raw a RawIOBase value
     * @param bufsize an int size of the buffer
     * @return a BufferedIOBase wrapper
     */
    private BufferedIOBase createBuffer(RawIOBase raw, int bufsize) {
        if (bufsize < 0) {
            bufsize = IOBase.DEFAULT_BUFFER_SIZE;
        }
        boolean lineBuffered = bufsize == 1;
        BufferedIOBase buffer;
        if (updating) {
            buffer = lineBuffered ?
                    new LineBufferedRandom(raw) :
                    new BufferedRandom(raw, bufsize);
        } else if (writing || appending) {
            buffer = lineBuffered ?
                    new LineBufferedWriter(raw) :
                    new BufferedWriter(raw, bufsize);
        } else if (reading) {
            // Line buffering is for output only
            buffer = new BufferedReader(raw, lineBuffered ? 0 : bufsize);
        } else {
            // Should never happen
            throw Py.ValueError("unknown mode: '" + mode + "'");
        }
        return buffer;
    }

    /**
     * Parse and validate the python file mode, returning a cleaned
     * file mode suitable for FileIO.
     *
     * @param mode a python file mode String
     * @return a RandomAccessFile mode String
     */
    private String parseMode(String mode) {
        if (mode.length() == 0) {
            throw Py.ValueError("empty mode string");
        }

        String origMode = mode;
        if (mode.contains("U")) {
            universal = true;
            mode = mode.replace("U", "");
            if (mode.length() == 0) {
                mode = "r";
            } else if ("wa+".indexOf(mode.charAt(0)) > -1) {
                throw Py.ValueError("universal newline mode can only be used with " +
                                    "modes starting with 'r'");
            }
        }
        if ("rwa".indexOf(mode.charAt(0)) == -1) {
            throw Py.ValueError("mode string must begin with one of 'r', 'w', 'a' or " +
                                "'U', not '" + origMode + "'");
        }

        binary = mode.contains("b");
        reading = mode.contains("r");
        writing = mode.contains("w");
        appending = mode.contains("a");
        updating = mode.contains("+");

        return (reading ? "r" : "") + (writing ? "w" : "") +
                (appending ? "a" : "") + (updating ? "+" : "");
    }

    final synchronized String file_read(int n) {
        checkClosed();
        return file.read(n);
    }

    public String read(int n) {
        return file_read(n);
    }

    final String file_read() {
        return file_read(-1);
    }

    public String read() {
        return file_read();
    }

    final synchronized int file_readinto(PyObject buf) {
        checkClosed();
        return file.readinto(buf);
    }

    public int readinto(PyObject buf) {
        return file_readinto(buf);
    }

    final synchronized String file_readline(int max) {
        checkClosed();
        return file.readline(max);
    }

    public String readline(int max) {
        return file_readline(max);
    }

    public String readline() {
        return file_readline();
    }

    final String file_readline() {
        return file_readline(-1);
    }

    final synchronized PyObject file_readlines(int sizehint) {
        checkClosed();
        PyList list = new PyList();
        int count = 0;
        do {
            String line = file.readline(-1);
            int len = line.length();
            if (len == 0) {
                // EOF
                break;
            }
            count += len;
            list.append(new PyString(line));
        } while (sizehint <= 0 || count < sizehint);
        return list;
    }

    public PyObject readlines(int sizehint) {
        return file_readlines(sizehint);
    }

    final PyObject file_readlines() {
        return file_readlines(0);
    }

    public PyObject readlines() {
        return file_readlines();
    }

    public PyObject __iter__() {
        return file___iter__();
    }

    final PyObject file___iter__() {
        checkClosed();
        return this;
    }

    public PyObject __iternext__() {
        return file___iternext__();
    }

    final synchronized PyObject file___iternext__() {
        checkClosed();
        String next = file.readline(-1);
        if (next.length() == 0) {
            return null;
        }
        return new PyString(next);
    }

    final PyObject file_next() {
        PyObject ret = __iternext__();
        if (ret == null)
            throw Py.StopIteration("");
        return ret;
    }

    public PyObject next() {
        return file_next();
    }

    final PyObject file_xreadlines() {
        checkClosed();
        return this;
    }

    public PyObject xreadlines() {
        return file_xreadlines();
    }

    final void file_write(PyObject o) {
        if (o instanceof PyUnicode) {
            // Call __str__ on unicode objects to encode them before writing
            file_write(o.__str__().string);
        } else if (o instanceof PyString) {
            file_write(((PyString)o).string);
        } else {
            throw Py.TypeError("write requires a string as its argument");
        }
    }

    final synchronized void file_write(String s) {
        checkClosed();
        softspace = false;
        file.write(s);
    }

    public void write(String s) {
        file_write(s);
    }

    final synchronized void file_writelines(PyObject a) {
        checkClosed();
        PyObject iter = Py.iter(a, "writelines() requires an iterable argument");

        PyObject item = null;
        while ((item = iter.__iternext__()) != null) {
            if (!(item instanceof PyString)) {
                throw Py.TypeError("writelines() argument must be a " +
                                   "sequence of strings");
            }
            file.write(item.toString());
        }
    }

    public void writelines(PyObject a) {
        file_writelines(a);
    }

    final synchronized long file_tell() {
        checkClosed();
        return file.tell();
    }

    public long tell() {
        return file_tell();
    }

    final synchronized void file_seek(long pos, int how) {
        checkClosed();
        file.seek(pos, how);
    }

    public void seek(long pos, int how) {
        file_seek(pos, how);
    }

    final void file_seek(long pos) {
        seek(pos, 0);
    }

    public void seek(long pos) {
        file_seek(pos);
    }

    final synchronized void file_flush() {
        checkClosed();
        file.flush();
    }

    public void flush() {
        file_flush();
    }

    final synchronized void file_close() {
        if (closer != null) {
            closer.close();
            closer = null;
        } else {
            file.close();
        }
    }

    public void close() {
        file_close();
    }

    final synchronized void file_truncate() {
        file.truncate(file.tell());
     }

    public void truncate() {
        file_truncate();
    }

    final synchronized void file_truncate(long position) {
        file.truncate(position);
     }

     public void truncate(long position) {
         file_truncate(position);
     }

    public boolean isatty() {
        return file_isatty();
    }

    final boolean file_isatty() {
        return file.isatty();
    }

    public int fileno() {
        return file_fileno();
    }

    final int file_fileno() {
        return file.fileno();
    }

    final String file_toString() {
        StringBuffer s = new StringBuffer("<");
        if (file.closed()) {
            s.append("closed ");
        } else {
            s.append("open ");
        }
        s.append("file ");
        s.append(name.__repr__());
        s.append(", mode '");
        s.append(mode);
        s.append("' ");
        s.append(Py.idstr(this));
        s.append(">");
        return s.toString();
    }

    public String toString() {
        return file_toString();
    }

    final int file___cmp__(PyObject o) {
        return super.__cmp__(o);
    }

    final boolean file___nonzero__() {
        return super.__nonzero__();
    }

    private void checkClosed() {
        file.checkClosed();
    }

    public String getMode() {
        return mode;
    }

    public PyObject getName() {
        return name;
    }

    public boolean getClosed() {
        return file.closed();
    }

    public PyObject getNewlines() {
        return file.getNewlines();
    }

    public PyObject getSoftspace() {
        return softspace ? new PyInteger(1) : new PyInteger(0);
    }

    public void setSoftspace(PyObject obj) {
        softspace = obj.__nonzero__();
    }

    public void delSoftspace() {
        throw Py.TypeError("can't delete numeric/char attribute");
    }

    public Object __tojava__(Class cls) {
        Object o = file.__tojava__(cls);
        if (o == null) {
            o = super.__tojava__(cls);
        }
        return o;
    }

    protected void finalize() throws Throwable {
        super.finalize();
        if (closer != null) {
            closer.close();
        }
    }

    private static void initCloser() {
        try {
            Runtime.getRuntime().addShutdownHook(new PyFileCloser());
        } catch(SecurityException e) {
            Py.writeDebug("PyFile", "Can't register file closer hook");
        }
    }

    /**
     * A mechanism to make sure PyFiles are closed on exit. On
     * creation Closer adds itself to a list of Closers that will be
     * run by PyFileCloser on JVM shutdown. When a PyFile's close or
     * finalize methods are called, PyFile calls its Closer.close
     * which clears Closer out of the shutdown queue.
     * 
     * We use a regular object here rather than WeakReferences and
     * their ilk as they may be collected before the shutdown hook
     * runs. There's no guarantee that finalize will be called during
     * shutdown, so we can't use it. It's vital that this Closer has
     * no reference to the PyFile it's closing so the PyFile remains
     * garbage collectable.
     */
    private static class Closer {

        /** The underlying file */
        private TextIOBase file;
        
        public Closer(TextIOBase file) {
            this.file = file;
            // Add ourselves to the queue of Closers to be run on shutdown
            synchronized (closers) {
                closers.add(this);
            }
        }

        public void close() {
            synchronized (closers) {
                if (!closers.remove(this)) {
                    return;
                }
            }
            _close();
        }
        
        public void _close() {
            file.close();
        }
    }

    private static class PyFileCloser extends Thread {

        public PyFileCloser() {
            super("Jython Shutdown File Closer");
        }

        public void run() {
            synchronized (closers) {
                while (closers.size() > 0) {
                    try {
                        ((Closer)closers.removeFirst())._close();
                    } catch (PyException e) {
                        // continue
                    }
                }
            }
        }
    }

}
