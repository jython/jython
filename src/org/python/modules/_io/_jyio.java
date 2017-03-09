/* Copyright (c)2012 Jython Developers */
package org.python.modules._io;

import org.python.core.ArgParser;
import org.python.core.ClassDictInit;
import org.python.core.Py;
import org.python.core.PyException;
import org.python.core.PyInteger;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.core.PyStringMap;
import org.python.core.PyType;
import org.python.core.imp;
import org.python.core.io.IOBase;

/**
 * The Python _io module implemented in Java.
 */
public class _jyio implements ClassDictInit {

    /**
     * This method is called when the module is loaded, to populate the namespace (dictionary) of
     * the module. The dictionary has been initialised at this point reflectively from the methods
     * of this class and this method nulls those entries that ought not to be exposed.
     *
     * @param dict namespace of the module
     */
    public static void classDictInit(PyObject dict) {
        dict.__setitem__("__name__", new PyString("_jyio"));
        dict.__setitem__("__doc__", new PyString(__doc__));
        dict.__setitem__("DEFAULT_BUFFER_SIZE", DEFAULT_BUFFER_SIZE);

        dict.__setitem__("_IOBase", PyIOBase.TYPE);
        dict.__setitem__("_RawIOBase", PyRawIOBase.TYPE);
        dict.__setitem__("FileIO", PyFileIO.TYPE);

        // Define UnsupportedOperation exception by constructing the type

        PyObject exceptions = imp.load("exceptions");
        PyObject ValueError = exceptions.__getattr__("ValueError");
        PyObject IOError = exceptions.__getattr__("IOError");
        // Equivalent to class UnsupportedOperation(ValueError, IOError) : pass
        UnsupportedOperation = makeException(dict, "UnsupportedOperation", ValueError, IOError);

        // Hide from Python
        dict.__setitem__("classDictInit", null);
        dict.__setitem__("makeException", null);
    }

    /** A Python class for the <code>UnsupportedOperation</code> exception. */
    public static PyType UnsupportedOperation;

    /**
     * A function that returns a {@link PyException}, which is a Java exception suitable for
     * throwing, and that will be raised as an <code>UnsupportedOperation</code> Python exception.
     *
     * @param message text message parameter to the Python exception
     * @return nascent <code>UnsupportedOperation</code> Python exception
     */
    public static PyException UnsupportedOperation(String message) {
        return new PyException(UnsupportedOperation, message);
    }

    /**
     * Convenience method for constructing a type object of a Python exception, named as given, and
     * added to the namespace of the "_io" module.
     *
     * @param dict module dictionary
     * @param excname name of the exception
     * @param bases one or more bases (superclasses)
     * @return the constructed exception type
     */
    private static PyType makeException(PyObject dict, String excname, PyObject... bases) {
        PyStringMap classDict = new PyStringMap();
        classDict.__setitem__("__module__", Py.newString("_io"));
        PyType type = (PyType)Py.makeClass(excname, bases, classDict);
        dict.__setitem__(excname, type);
        return type;
    }

    /** Default buffer size obtained from {@link IOBase#DEFAULT_BUFFER_SIZE}. */
    private static final int _DEFAULT_BUFFER_SIZE = IOBase.DEFAULT_BUFFER_SIZE;

    /** Default buffer size for export. */
    public static final PyInteger DEFAULT_BUFFER_SIZE = new PyInteger(_DEFAULT_BUFFER_SIZE);

    /**
     * Open file and return a stream. Raise IOError upon failure. This is a port to Java of the
     * CPython _io.open (Modules/_io/_iomodule.c) following the same logic, but expressed with the
     * benefits of Java syntax.
     *
     * @param args array of arguments from Python call via Jython framework
     * @param kwds array of keywords from Python call via Jython framework
     * @return the stream object
     */
    public static PyObject open(PyObject[] args, String[] kwds) {

        // Get the arguments to variables
        ArgParser ap = new ArgParser("open", args, kwds, openKwds, 1);
        PyObject file = ap.getPyObject(0);
        String m = ap.getString(1, "r");
        int buffering = ap.getInt(2, -1);
        final String encoding = ap.getString(3, null);
        final String errors = ap.getString(4, null);
        final String newline = ap.getString(5, null);
        boolean closefd = Py.py2boolean(ap.getPyObject(6, Py.True));

        // Decode the mode string
        OpenMode mode = new OpenMode(m) {

            @Override
            public void validate() {
                super.validate();
                validate(encoding, errors, newline);
            }
        };

        mode.checkValid();

        /*
         * Create the Raw file stream. Let the constructor deal with the variants and argument
         * checking.
         */
        PyFileIO raw = new PyFileIO(file, mode, closefd);

        /*
         * From the Python documentation for io.open() buffering = 0 to switch buffering off (only
         * allowed in binary mode), 1 to select line buffering (only usable in text mode), and an
         * integer > 1 to indicate the size of a fixed-size buffer.
         *
         * When no buffering argument is given, the default buffering policy works as follows:
         * Binary files are buffered in fixed-size chunks; "Interactive" text files (files for which
         * isatty() returns True) use line buffering. Other text files use the policy described
         * above for binary files.
         *
         * In Java, it seems a stream never is *known* to be interactive, but we ask anyway, and
         * maybe one day we shall know.
         */
        boolean line_buffering = false;

        if (buffering == 0) {
            if (!mode.binary) {
                throw Py.ValueError("can't have unbuffered text I/O");
            }
            return raw;

        } else if (buffering == 1) {
            // The stream is to be read line-by-line.
            line_buffering = true;
            // Force default size for actual buffer
            buffering = -1;

        } else if (buffering < 0 && raw.isatty()) {
            // No buffering argument given but stream is inteeractive.
            line_buffering = true;
        }

        if (buffering < 0) {
            /*
             * We are still being asked for the default buffer size. CPython establishes the default
             * buffer size using fstat(fd), but Java appears to give no clue. A useful study of
             * buffer sizes in NIO is http://www.evanjones.ca/software/java-bytebuffers.html . This
             * leads us to the fixed choice of _DEFAULT_BUFFER_SIZE (=8KB).
             */
            buffering = _DEFAULT_BUFFER_SIZE;
        }

        /*
         * We now know just what particular class of file we are opening, and therefore what stack
         * (buffering and text encoding) we should build.
         */
        if (buffering == 0) {
            // Not buffering, return the raw file object
            return raw;

        } else {
            // We are buffering, so wrap raw into a buffered file
            PyObject bufferType = null;
            PyObject io = imp.load("io");

            if (mode.updating) {
                bufferType = io.__getattr__("BufferedRandom");
            } else if (mode.writing || mode.appending) {
                bufferType = io.__getattr__("BufferedWriter");
            } else {                        // = reading
                bufferType = io.__getattr__("BufferedReader");
            }

            PyInteger pyBuffering = new PyInteger(buffering);
            PyObject buffer = bufferType.__call__(raw, pyBuffering);

            if (mode.binary) {
                // If binary, return the just the buffered file
                return buffer;

            } else {
                // We are opening in text mode, so wrap buffered file in a TextIOWrapper.
                PyObject textType = io.__getattr__("TextIOWrapper");
                PyObject[] textArgs =
                        {buffer, ap.getPyObject(3, Py.None), ap.getPyObject(4, Py.None),
                                ap.getPyObject(5, Py.None), Py.newBoolean(line_buffering)};
                PyObject wrapper = textType.__call__(textArgs);
                wrapper.__setattr__("mode", new PyString(m));
                return wrapper;
            }
        }
    }

    private static final String[] openKwds = {"file", "mode", "buffering", "encoding", "errors",
            "newline", "closefd"};

    public static final String __doc__ =
            "The io module provides the Python interfaces to stream handling. The\n"
                    + "builtin open function is defined in this module.\n" + "\n"
                    + "At the top of the I/O hierarchy is the abstract base class IOBase. It\n"
                    + "defines the basic interface to a stream. Note, however, that there is no\n"
                    + "seperation between reading and writing to streams; implementations are\n"
                    + "allowed to throw an IOError if they do not support a given operation.\n"
                    + "\n"
                    + "Extending IOBase is RawIOBase which deals simply with the reading and\n"
                    + "writing of raw bytes to a stream. FileIO subclasses RawIOBase to provide\n"
                    + "an interface to OS files.\n" + "\n"
                    + "BufferedIOBase deals with buffering on a raw byte stream (RawIOBase). Its\n"
                    + "subclasses, BufferedWriter, BufferedReader, and BufferedRWPair buffer\n"
                    + "streams that are readable, writable, and both respectively.\n"
                    + "BufferedRandom provides a buffered interface to random access\n"
                    + "streams. BytesIO is a simple stream of in-memory bytes.\n" + "\n"
                    + "Another IOBase subclass, TextIOBase, deals with the encoding and decoding\n"
                    + "of streams into text. TextIOWrapper, which extends it, is a buffered text\n"
                    + "interface to a buffered raw stream (`BufferedIOBase`). Finally, StringIO\n"
                    + "is a in-memory stream for text.\n" + "\n"
                    + "Argument names are not part of the specification, and only the arguments\n"
                    + "of open() are intended to be used as keyword arguments.\n";

    public static final String __doc__open =
            "Open file and return a stream.  Raise IOError upon failure.\n" + "\n"
                    + "file is either a text or byte string giving the name (and the path\n"
                    + "if the file isn't in the current working directory) of the file to\n"
                    + "be opened or an integer file descriptor of the file to be\n"
                    + "wrapped. (If a file descriptor is given, it is closed when the\n"
                    + "returned I/O object is closed, unless closefd is set to False.)\n" + "\n"
                    + "mode is an optional string that specifies the mode in which the file\n"
                    + "is opened. It defaults to 'r' which means open for reading in text\n"
                    + "mode.  Other common values are 'w' for writing (truncating the file if\n"
                    + "it already exists), and 'a' for appending (which on some Unix systems,\n"
                    + "means that all writes append to the end of the file regardless of the\n"
                    + "current seek position). In text mode, if encoding is not specified the\n"
                    + "encoding used is platform dependent. (For reading and writing raw\n"
                    + "bytes use binary mode and leave encoding unspecified.) The available\n"
                    + "modes are:\n" + "\n"
                    + "========= ===============================================================\n"
                    + "Character Meaning\n"
                    + "--------- ---------------------------------------------------------------\n"
                    + "'r'       open for reading (default)\n"
                    + "'w'       open for writing, truncating the file first\n"
                    + "'a'       open for writing, appending to the end of the file if it exists\n"
                    + "'b'       binary mode\n" + "'t'       text mode (default)\n"
                    + "'+'       open a disk file for updating (reading and writing)\n"
                    + "'U'       universal newline mode (for backwards compatibility; unneeded\n"
                    + "          for new code)\n"
                    + "========= ===============================================================\n"
                    + "\n"
                    + "The default mode is 'rt' (open for reading text). For binary random\n"
                    + "access, the mode 'w+b' opens and truncates the file to 0 bytes, while\n"
                    + "'r+b' opens the file without truncation.\n" + "\n"
                    + "Python distinguishes between files opened in binary and text modes,\n"
                    + "even when the underlying operating system doesn't. Files opened in\n"
                    + "binary mode (appending 'b' to the mode argument) return contents as\n"
                    + "bytes objects without any decoding. In text mode (the default, or when\n"
                    + "'t' is appended to the mode argument), the contents of the file are\n"
                    + "returned as strings, the bytes having been first decoded using a\n"
                    + "platform-dependent encoding or using the specified encoding if given.\n"
                    + "\n" + "buffering is an optional integer used to set the buffering policy.\n"
                    + "Pass 0 to switch buffering off (only allowed in binary mode), 1 to select\n"
                    + "line buffering (only usable in text mode), and an integer > 1 to indicate\n"
                    + "the size of a fixed-size chunk buffer.  When no buffering argument is\n"
                    + "given, the default buffering policy works as follows:\n" + "\n"
                    + "* Binary files are buffered in fixed-size chunks; the size of the buffer\n"
                    + "  is chosen using a heuristic trying to determine the underlying device's\n"
                    + "  \"block size\" and falling back on `io.DEFAULT_BUFFER_SIZE`.\n"
                    + "  On many systems, the buffer will typically be 4096 or 8192 bytes long.\n"
                    + "\n"
                    + "* \"Interactive\" text files (files for which isatty() returns True)\n"
                    + "  use line buffering.  Other text files use the policy described above\n"
                    + "  for binary files.\n" + "\n"
                    + "encoding is the name of the encoding used to decode or encode the\n"
                    + "file. This should only be used in text mode. The default encoding is\n"
                    + "platform dependent, but any encoding supported by Python can be\n"
                    + "passed.  See the codecs module for the list of supported encodings.\n"
                    + "\n"
                    + "errors is an optional string that specifies how encoding errors are to\n"
                    + "be handled---this argument should not be used in binary mode. Pass\n"
                    + "'strict' to raise a ValueError exception if there is an encoding error\n"
                    + "(the default of None has the same effect), or pass 'ignore' to ignore\n"
                    + "errors. (Note that ignoring encoding errors can lead to data loss.)\n"
                    + "See the documentation for codecs.register for a list of the permitted\n"
                    + "encoding error strings.\n" + "\n"
                    + "newline controls how universal newlines works (it only applies to text\n"
                    + "mode). It can be None, '', '\\n', '\\r', and '\\r\\n'.  It works as\n"
                    + "follows:\n" + "\n"
                    + "* On input, if newline is None, universal newlines mode is\n"
                    + "  enabled. Lines in the input can end in '\\n', '\\r', or '\\r\\n', and\n"
                    + "  these are translated into '\\n' before being returned to the\n"
                    + "  caller. If it is '', universal newline mode is enabled, but line\n"
                    + "  endings are returned to the caller untranslated. If it has any of\n"
                    + "  the other legal values, input lines are only terminated by the given\n"
                    + "  string, and the line ending is returned to the caller untranslated.\n"
                    + "\n" + "* On output, if newline is None, any '\\n' characters written are\n"
                    + "  translated to the system default line separator, os.linesep. If\n"
                    + "  newline is '', no translation takes place. If newline is any of the\n"
                    + "  other legal values, any '\\n' characters written are translated to\n"
                    + "  the given string.\n" + "\n"
                    + "If closefd is False, the underlying file descriptor will be kept open\n"
                    + "when the file is closed. This does not work when a file name is given\n"
                    + "and must be True in that case.\n" + "\n"
                    + "open() returns a file object whose type depends on the mode, and\n"
                    + "through which the standard file operations such as reading and writing\n"
                    + "are performed. When open() is used to open a file in a text mode ('w',\n"
                    + "'r', 'wt', 'rt', etc.), it returns a TextIOWrapper. When used to open\n"
                    + "a file in a binary mode, the returned class varies: in read binary\n"
                    + "mode, it returns a BufferedReader; in write binary and append binary\n"
                    + "modes, it returns a BufferedWriter, and in read/write mode, it returns\n"
                    + "a BufferedRandom.\n" + "\n"
                    + "It is also possible to use a string or bytearray as a file for both\n"
                    + "reading and writing. For strings StringIO can be used like a file\n"
                    + "opened in a text mode, and for bytes a BytesIO can be used like a file\n"
                    + "opened in a binary mode.\n";
}
