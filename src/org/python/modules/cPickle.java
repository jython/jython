/*
 * Copyright 1998 Finn Bock.
 *
 * This program contains material copyrighted by:
 * Copyright (c) 1991-1995 by Stichting Mathematisch Centrum, Amsterdam,
 * The Netherlands.
 */

/* note about impl:
  instanceof vs. CPython type(.) is .
*/

package org.python.modules;

import java.math.BigInteger;
import java.util.Hashtable;

import org.python.core.ClassDictInit;
import org.python.core.Py;
import org.python.core.PyBoolean;
import org.python.core.PyBuiltinCallable;
import org.python.core.PyClass;
import org.python.core.PyDictionary;
import org.python.core.PyException;
import org.python.core.PyFile;
import org.python.core.PyFloat;
import org.python.core.PyFunction;
import org.python.core.PyInstance;
import org.python.core.PyInteger;
import org.python.core.PyList;
import org.python.core.PyLong;
import org.python.core.PyModule;
import org.python.core.PyNone;
import org.python.core.PyObject;
import org.python.core.PyReflectedFunction;
import org.python.core.PySequence;
import org.python.core.PyString;
import org.python.core.PyStringMap;
import org.python.core.PyTuple;
import org.python.core.PyType;
import org.python.core.PyUnicode;
import org.python.core.__builtin__;
import org.python.core.codecs;
import org.python.core.exceptions;
import org.python.core.imp;

/**
 *
 * From the python documentation:
 * <p>
 * The <tt>cPickle.java</tt> module implements a basic but powerful algorithm
 * for ``pickling'' (a.k.a. serializing, marshalling or flattening) nearly
 * arbitrary Python objects.  This is the act of converting objects to a
 * stream of bytes (and back: ``unpickling'').
 * This is a more primitive notion than
 * persistency -- although <tt>cPickle.java</tt> reads and writes file
 * objects, it does not handle the issue of naming persistent objects, nor
 * the (even more complicated) area of concurrent access to persistent
 * objects.  The <tt>cPickle.java</tt> module can transform a complex object
 * into a byte stream and it can transform the byte stream into an object
 * with the same internal structure.  The most obvious thing to do with these
 * byte streams is to write them onto a file, but it is also conceivable
 * to send them across a network or store them in a database.  The module
 * <tt>shelve</tt> provides a simple interface to pickle and unpickle
 * objects on ``dbm''-style database files.
 * <P>
 * <b>Note:</b> The <tt>cPickle.java</tt> have the same interface as the
 * standard module <tt>pickle</tt>except that <tt>Pickler</tt> and
 * <tt>Unpickler</tt> are factory functions, not classes (so they cannot be
 * used as base classes for inheritance).
 * This limitation is similar for the original cPickle.c version.
 *
 * <P>
 * Unlike the built-in module <tt>marshal</tt>, <tt>cPickle.java</tt> handles
 * the following correctly:
 * <P>
 *
 * <UL><LI>recursive objects (objects containing references to themselves)
 *
 * <P>
 *
 * <LI>object sharing (references to the same object in different places)
 *
 * <P>
 *
 * <LI>user-defined classes and their instances
 *
 * <P>
 *
 * </UL>
 *
 * <P>
 * The data format used by <tt>cPickle.java</tt> is Python-specific.  This has
 * the advantage that there are no restrictions imposed by external
 * standards such as XDR (which can't represent pointer sharing); however
 * it means that non-Python programs may not be able to reconstruct
 * pickled Python objects.
 *
 * <P>
 * By default, the <tt>cPickle.java</tt> data format uses a printable ASCII
 * representation.  This is slightly more voluminous than a binary
 * representation.  The big advantage of using printable ASCII (and of
 * some other characteristics of <tt>cPickle.java</tt>'s representation) is
 * that for debugging or recovery purposes it is possible for a human to read
 * the pickled file with a standard text editor.
 *
 * <P>
 * A binary format, which is slightly more efficient, can be chosen by
 * specifying a nonzero (true) value for the <i>bin</i> argument to the
 * <tt>Pickler</tt> constructor or the <tt>dump()</tt> and <tt>dumps()</tt>
 * functions.  The binary format is not the default because of backwards
 * compatibility with the Python 1.4 pickle module.  In a future version,
 * the default may change to binary.
 *
 * <P>
 * The <tt>cPickle.java</tt> module doesn't handle code objects.
 * <P>
 * For the benefit of persistency modules written using <tt>cPickle.java</tt>,
 * it supports the notion of a reference to an object outside the pickled
 * data stream.  Such objects are referenced by a name, which is an
 * arbitrary string of printable ASCII characters.  The resolution of
 * such names is not defined by the <tt>cPickle.java</tt> module -- the
 * persistent object module will have to implement a method
 * <tt>persistent_load()</tt>.  To write references to persistent objects,
 * the persistent module must define a method <tt>persistent_id()</tt> which
 * returns either <tt>None</tt> or the persistent ID of the object.
 *
 * <P>
 * There are some restrictions on the pickling of class instances.
 *
 * <P>
 * First of all, the class must be defined at the top level in a module.
 * Furthermore, all its instance variables must be picklable.
 *
 * <P>
 *
 * <P>
 * When a pickled class instance is unpickled, its <tt>__init__()</tt> method
 * is normally <i>not</i> invoked.  <b>Note:</b> This is a deviation
 * from previous versions of this module; the change was introduced in
 * Python 1.5b2.  The reason for the change is that in many cases it is
 * desirable to have a constructor that requires arguments; it is a
 * (minor) nuisance to have to provide a <tt>__getinitargs__()</tt> method.
 *
 * <P>
 * If it is desirable that the <tt>__init__()</tt> method be called on
 * unpickling, a class can define a method <tt>__getinitargs__()</tt>,
 * which should return a <i>tuple</i> containing the arguments to be
 * passed to the class constructor (<tt>__init__()</tt>).  This method is
 * called at pickle time; the tuple it returns is incorporated in the
 * pickle for the instance.
 * <P>
 * Classes can further influence how their instances are pickled -- if the
 * class defines the method <tt>__getstate__()</tt>, it is called and the
 * return state is pickled as the contents for the instance, and if the class
 * defines the method <tt>__setstate__()</tt>, it is called with the
 * unpickled state.  (Note that these methods can also be used to
 * implement copying class instances.)  If there is no
 * <tt>__getstate__()</tt> method, the instance's <tt>__dict__</tt> is
 * pickled.  If there is no <tt>__setstate__()</tt> method, the pickled
 * object must be a dictionary and its items are assigned to the new
 * instance's dictionary.  (If a class defines both <tt>__getstate__()</tt>
 * and <tt>__setstate__()</tt>, the state object needn't be a dictionary
 * -- these methods can do what they want.)  This protocol is also used
 * by the shallow and deep copying operations defined in the <tt>copy</tt>
 * module.
 * <P>
 * Note that when class instances are pickled, their class's code and
 * data are not pickled along with them.  Only the instance data are
 * pickled.  This is done on purpose, so you can fix bugs in a class or
 * add methods and still load objects that were created with an earlier
 * version of the class.  If you plan to have long-lived objects that
 * will see many versions of a class, it may be worthwhile to put a version
 * number in the objects so that suitable conversions can be made by the
 * class's <tt>__setstate__()</tt> method.
 *
 * <P>
 * When a class itself is pickled, only its name is pickled -- the class
 * definition is not pickled, but re-imported by the unpickling process.
 * Therefore, the restriction that the class must be defined at the top
 * level in a module applies to pickled classes as well.
 *
 * <P>
 *
 * <P>
 * The interface can be summarized as follows.
 *
 * <P>
 * To pickle an object <tt>x</tt> onto a file <tt>f</tt>, open for writing:
 *
 * <P>
 * <dl><dd><pre>
 * p = pickle.Pickler(f)
 * p.dump(x)
 * </pre></dl>
 *
 * <P>
 * A shorthand for this is:
 *
 * <P>
 * <dl><dd><pre>
 * pickle.dump(x, f)
 * </pre></dl>
 *
 * <P>
 * To unpickle an object <tt>x</tt> from a file <tt>f</tt>, open for reading:
 *
 * <P>
 * <dl><dd><pre>
 * u = pickle.Unpickler(f)
 * x = u.load()
 * </pre></dl>
 *
 * <P>
 * A shorthand is:
 *
 * <P>
 * <dl><dd><pre>
 * x = pickle.load(f)
 * </pre></dl>
 *
 * <P>
 * The <tt>Pickler</tt> class only calls the method <tt>f.write()</tt> with a
 * string argument.  The <tt>Unpickler</tt> calls the methods
 * <tt>f.read()</tt> (with an integer argument) and <tt>f.readline()</tt>
 * (without argument), both returning a string.  It is explicitly allowed to
 * pass non-file objects here, as long as they have the right methods.
 *
 * <P>
 * The constructor for the <tt>Pickler</tt> class has an optional second
 * argument, <i>bin</i>.  If this is present and nonzero, the binary
 * pickle format is used; if it is zero or absent, the (less efficient,
 * but backwards compatible) text pickle format is used.  The
 * <tt>Unpickler</tt> class does not have an argument to distinguish
 * between binary and text pickle formats; it accepts either format.
 *
 * <P>
 * The following types can be pickled:
 *
 * <UL><LI><tt>None</tt>
 *
 * <P>
 *
 * <LI>integers, long integers, floating point numbers
 *
 * <P>
 *
 * <LI>strings
 *
 * <P>
 *
 * <LI>tuples, lists and dictionaries containing only picklable objects
 *
 * <P>
 *
 * <LI>classes that are defined at the top level in a module
 *
 * <P>
 *
 * <LI>instances of such classes whose <tt>__dict__</tt> or
 * <tt>__setstate__()</tt> is picklable
 *
 * <P>
 *
 * </UL>
 *
 * <P>
 * Attempts to pickle unpicklable objects will raise the
 * <tt>PicklingError</tt> exception; when this happens, an unspecified
 * number of bytes may have been written to the file.
 *
 * <P>
 * It is possible to make multiple calls to the <tt>dump()</tt> method of
 * the same <tt>Pickler</tt> instance.  These must then be matched to the
 * same number of calls to the <tt>load()</tt> method of the
 * corresponding <tt>Unpickler</tt> instance.  If the same object is
 * pickled by multiple <tt>dump()</tt> calls, the <tt>load()</tt> will all
 * yield references to the same object.  <i>Warning</i>: this is intended
 * for pickling multiple objects without intervening modifications to the
 * objects or their parts.  If you modify an object and then pickle it
 * again using the same <tt>Pickler</tt> instance, the object is not
 * pickled again -- a reference to it is pickled and the
 * <tt>Unpickler</tt> will return the old value, not the modified one.
 * (There are two problems here: (a) detecting changes, and (b)
 * marshalling a minimal set of changes.  I have no answers.  Garbage
 * Collection may also become a problem here.)
 *
 * <P>
 * Apart from the <tt>Pickler</tt> and <tt>Unpickler</tt> classes, the
 * module defines the following functions, and an exception:
 *
 * <P>
 * <dl><dt><b><tt>dump</tt></a></b> (<var>object, file</var><big>[</big><var>,
 *      bin</var><big>]</big>)
 * <dd>
 * Write a pickled representation of <i>obect</i> to the open file object
 * <i>file</i>.  This is equivalent to
 * "<tt>Pickler(<i>file</i>, <i>bin</i>).dump(<i>object</i>)</tt>".
 * If the optional <i>bin</i> argument is present and nonzero, the binary
 * pickle format is used; if it is zero or absent, the (less efficient)
 * text pickle format is used.
 * </dl>
 *
 * <P>
 * <dl><dt><b><tt>load</tt></a></b> (<var>file</var>)
 * <dd>
 * Read a pickled object from the open file object <i>file</i>.  This is
 * equivalent to "<tt>Unpickler(<i>file</i>).load()</tt>".
 * </dl>
 *
 * <P>
 * <dl><dt><b><tt>dumps</tt></a></b> (<var>object</var><big>[</big><var>,
 *     bin</var><big>]</big>)
 * <dd>
 * Return the pickled representation of the object as a string, instead
 * of writing it to a file.  If the optional <i>bin</i> argument is
 * present and nonzero, the binary pickle format is used; if it is zero
 * or absent, the (less efficient) text pickle format is used.
 * </dl>
 *
 * <P>
 * <dl><dt><b><tt>loads</tt></a></b> (<var>string</var>)
 * <dd>
 * Read a pickled object from a string instead of a file.  Characters in
 * the string past the pickled object's representation are ignored.
 * </dl>
 *
 * <P>
 * <dl><dt><b><a name="l2h-3763"><tt>PicklingError</tt></a></b>
 * <dd>
 * This exception is raised when an unpicklable object is passed to
 * <tt>Pickler.dump()</tt>.
 * </dl>
 *
 *
 * <p>
 * For the complete documentation on the pickle module, please see the
 * "Python Library Reference"
 * <p><hr><p>
 *
 * The module is based on both original pickle.py and the cPickle.c
 * version, except that all mistakes and errors are my own.
 * <p>
 * @author Finn Bock, bckfnn@pipmail.dknet.dk
 * @version cPickle.java,v 1.30 1999/05/15 17:40:12 fb Exp
 */
public class cPickle implements ClassDictInit {
    /**
     * The doc string
     */
    public static String __doc__ =
       "Java implementation and optimization of the Python pickle module\n" +
       "\n" +
       "$Id$\n";


    /**
     * The program version.
     */
    public static String __version__ = "1.30";

    /**
     * File format version we write.
     */
    public static final String format_version = "2.0";

    /**
     * Old format versions we can read.
     */
    public static final String[] compatible_formats =
                new String[] { "1.0", "1.1", "1.2", "1.3", "2.0" };

    /**
     * Highest protocol version supported.
     */
    public static final int HIGHEST_PROTOCOL = 2;

    public static String[] __depends__ = new String[] {
        "copy_reg",
    };

    public static PyObject PickleError;
    public static PyObject PicklingError;
    public static PyObject UnpickleableError;
    public static PyObject UnpicklingError;

    public static final PyString BadPickleGet =
                new PyString("cPickle.BadPickleGet");


    final static char MARK            = '(';
    final static char STOP            = '.';
    final static char POP             = '0';
    final static char POP_MARK        = '1';
    final static char DUP             = '2';
    final static char FLOAT           = 'F';
    final static char INT             = 'I';
    final static char BININT          = 'J';
    final static char BININT1         = 'K';
    final static char LONG            = 'L';
    final static char BININT2         = 'M';
    final static char NONE            = 'N';
    final static char PERSID          = 'P';
    final static char BINPERSID       = 'Q';
    final static char REDUCE          = 'R';
    final static char STRING          = 'S';
    final static char BINSTRING       = 'T';
    final static char SHORT_BINSTRING = 'U';
    final static char UNICODE         = 'V';
    final static char BINUNICODE      = 'X';
    final static char APPEND          = 'a';
    final static char BUILD           = 'b';
    final static char GLOBAL          = 'c';
    final static char DICT            = 'd';
    final static char EMPTY_DICT      = '}';
    final static char APPENDS         = 'e';
    final static char GET             = 'g';
    final static char BINGET          = 'h';
    final static char INST            = 'i';
    final static char LONG_BINGET     = 'j';
    final static char LIST            = 'l';
    final static char EMPTY_LIST      = ']';
    final static char OBJ             = 'o';
    final static char PUT             = 'p';
    final static char BINPUT          = 'q';
    final static char LONG_BINPUT     = 'r';
    final static char SETITEM         = 's';
    final static char TUPLE           = 't';
    final static char EMPTY_TUPLE     = ')';
    final static char SETITEMS        = 'u';
    final static char BINFLOAT        = 'G';

    final static char PROTO           = 0x80;
    final static char NEWOBJ          = 0x81;
    final static char EXT1            = 0x82;
    final static char EXT2            = 0x83;
    final static char EXT4            = 0x84;
    final static char TUPLE1          = 0x85;
    final static char TUPLE2          = 0x86;
    final static char TUPLE3          = 0x87;
    final static char NEWTRUE         = 0x88;
    final static char NEWFALSE        = 0x89;
    final static char LONG1           = 0x8A;
    final static char LONG4           = 0x8B;

    private static PyDictionary dispatch_table;
    private static PyDictionary extension_registry;
    private static PyDictionary inverted_registry;


    private static PyType BuiltinCallableType = PyType.fromClass(PyBuiltinCallable.class);

    private static PyType ReflectedFunctionType = PyType.fromClass(PyReflectedFunction.class);

    private static PyType ClassType = PyType.fromClass(PyClass.class);

    private static PyType TypeType = PyType.fromClass(PyType.class);

    private static PyType DictionaryType = PyType.fromClass(PyDictionary.class);

    private static PyType StringMapType = PyType.fromClass(PyStringMap.class);

    private static PyType FloatType = PyType.fromClass(PyFloat.class);

    private static PyType FunctionType = PyType.fromClass(PyFunction.class);

    private static PyType InstanceType = PyType.fromClass(PyInstance.class);

    private static PyType IntType = PyType.fromClass(PyInteger.class);

    private static PyType ListType = PyType.fromClass(PyList.class);

    private static PyType LongType = PyType.fromClass(PyLong.class);

    private static PyType NoneType = PyType.fromClass(PyNone.class);

    private static PyType StringType = PyType.fromClass(PyString.class);

    private static PyType UnicodeType = PyType.fromClass(PyUnicode.class);

    private static PyType TupleType = PyType.fromClass(PyTuple.class);

    private static PyType FileType = PyType.fromClass(PyFile.class);

    private static PyType BoolType = PyType.fromClass(PyBoolean.class);


    private static PyObject dict;

    private static final int BATCHSIZE = 1024;

    /**
     * Initialization when module is imported.
     */
    public static void classDictInit(PyObject dict) {
        cPickle.dict = dict;

        // XXX: Hack for JPython 1.0.1. By default __builtin__ is not in
        // sys.modules.
        imp.importName("__builtin__", true);

        PyModule copyreg = (PyModule)importModule("copy_reg");

        dispatch_table = (PyDictionary)copyreg.__getattr__("dispatch_table");
        extension_registry = (PyDictionary)copyreg.__getattr__("_extension_registry");
        inverted_registry = (PyDictionary)copyreg.__getattr__("_inverted_registry");

        PickleError = Py.makeClass("PickleError", Py.Exception, _PickleError());
        PicklingError = Py.makeClass("PicklingError", PickleError, exceptionNamespace());
        UnpickleableError = Py.makeClass("UnpickleableError", PicklingError, _UnpickleableError());
        UnpicklingError = Py.makeClass("UnpicklingError", PickleError, exceptionNamespace());
    }

    public static PyObject exceptionNamespace() {
        PyObject dict = new PyStringMap();
        dict.__setitem__("__module__", new PyString("cPickle"));
        return dict;
    }

    public static PyObject _PickleError() {
        dict = exceptionNamespace();
        dict.__setitem__("__str__", getJavaFunc("__str__", "_PickleError__str__"));
        return dict;
    }

    public static PyString _PickleError__str__(PyObject self, PyObject[] args, String[] kwargs) {
        PyObject selfArgs = self.__getattr__("args");
        if (selfArgs.__len__() > 0 && selfArgs.__getitem__(0).__len__()  > 0) {
            return selfArgs.__getitem__(0).__str__();
        } else {
            return new PyString("(what)");
        }
    }

    public static PyObject _UnpickleableError() {
        dict = exceptionNamespace();
        dict.__setitem__("__str__", getJavaFunc("__str__", "_UnpickleableError__str__"));
        return dict;
    }

    public static PyString _UnpickleableError__str__(PyObject self, PyObject[] args,
                                                     String[] kwargs) {
        PyObject selfArgs = self.__getattr__("args");
        PyObject a = selfArgs.__len__() > 0 ? selfArgs.__getitem__(0) : new PyString("(what)");
        return new PyString("Cannot pickle %s objects").__mod__(a).__str__();
    }

    /**
     * @param file      a file-like object, can be a cStringIO.StringIO,
     *                  a PyFile or any python object which implements a
     *                  <i>write</i> method. The data will be written as text.
     * @returns a new Pickler instance.
     */
    public static Pickler Pickler(PyObject file) {
        return new Pickler(file, 0);
    }


    /**
     * @param file      a file-like object, can be a cStringIO.StringIO,
     *                  a PyFile or any python object which implements a
     *                  <i>write</i> method.
     * @param protocol  pickle protocol version (0 - text, 1 - pre-2.3 binary, 2 - 2.3)
     * @returns         a new Pickler instance.
     */
    public static Pickler Pickler(PyObject file, int protocol) {
        return new Pickler(file, protocol);
    }


    /**
     * Returns a unpickler instance.
     * @param file      a file-like object, can be a cStringIO.StringIO,
     *                  a PyFile or any python object which implements a
     *                  <i>read</i> and <i>readline</i> method.
     * @returns         a new Unpickler instance.
     */
    public static Unpickler Unpickler(PyObject file) {
        return new Unpickler(file);
    }


    /**
     * Shorthand function which pickles the object on the file.
     * @param object    a data object which should be pickled.
     * @param file      a file-like object, can be a cStringIO.StringIO,
     *                  a PyFile or any python object which implements a
     *                  <i>write</i>  method. The data will be written as
     *                  text.
     * @returns         a new Unpickler instance.
     */
    public static void dump(PyObject object, PyObject file) {
        dump(object, file, 0);
    }

    /**
     * Shorthand function which pickles the object on the file.
     * @param object    a data object which should be pickled.
     * @param file      a file-like object, can be a cStringIO.StringIO,
     *                  a PyFile or any python object which implements a
     *                  <i>write</i> method.
     * @param protocol  pickle protocol version (0 - text, 1 - pre-2.3 binary, 2 - 2.3)
     * @returns         a new Unpickler instance.
     */
    public static void dump(PyObject object, PyObject file, int protocol) {
        new Pickler(file, protocol).dump(object);
    }


    /**
     * Shorthand function which pickles and returns the string representation.
     * @param object    a data object which should be pickled.
     * @returns         a string representing the pickled object.
     */
    public static PyString dumps(PyObject object) {
        return dumps(object, 0);
    }


    /**
     * Shorthand function which pickles and returns the string representation.
     * @param object    a data object which should be pickled.
     * @param protocol  pickle protocol version (0 - text, 1 - pre-2.3 binary, 2 - 2.3)
     * @returns         a string representing the pickled object.
     */
    public static PyString dumps(PyObject object, int protocol) {
        cStringIO.StringIO file = cStringIO.StringIO();
        dump(object, file, protocol);
        return file.getvalue();
    }


    /**
     * Shorthand function which unpickles a object from the file and returns
     * the new object.
     * @param file      a file-like object, can be a cStringIO.StringIO,
     *                  a PyFile or any python object which implements a
     *                  <i>read</i> and <i>readline</i> method.
     * @returns         a new object.
     */
    public static Object load(PyObject file) {
        return new Unpickler(file).load();
    }


    /**
     * Shorthand function which unpickles a object from the string and
     * returns the new object.
     * @param str       a strings which must contain a pickled object
     *                  representation.
     * @returns         a new object.
     */
    public static Object loads(PyObject str) {
        cStringIO.StringIO file = cStringIO.StringIO(str.toString());
        return new Unpickler(file).load();
    }



    // Factory for creating IOFile representation.
    private static IOFile createIOFile(PyObject file) {
        Object f = file.__tojava__(cStringIO.StringIO.class);
        if (f != Py.NoConversion)
            return new cStringIOFile(file);
        else if (__builtin__.isinstance(file, FileType))
            return new FileIOFile(file);
        else
            return new ObjectIOFile(file);
    }


    // IOFiles encapsulates and optimise access to the different file
    // representation.
    interface IOFile {
        public abstract void write(String str);
        // Usefull optimization since most data written are chars.
        public abstract void write(char str);
        public abstract void flush();
        public abstract String read(int len);
        // Usefull optimization since all readlines removes the
        // trainling newline.
        public abstract String readlineNoNl();

    }


    // Use a cStringIO as a file.
    static class cStringIOFile implements IOFile {
        cStringIO.StringIO file;

        cStringIOFile(PyObject file) {
            this.file = (cStringIO.StringIO)file.__tojava__(Object.class);
        }

        public void write(String str) {
            file.write(str);
        }

        public void write(char ch) {
            file.writeChar(ch);
        }

        public void flush() {}

        public String read(int len) {
            return file.read(len).asString();
        }

        public String readlineNoNl() {
            return file.readlineNoNl().asString();
        }
    }


    // Use a PyFile as a file.
    static class FileIOFile implements IOFile {
        PyFile file;

        FileIOFile(PyObject file) {
            this.file = (PyFile)file.__tojava__(PyFile.class);
            if (this.file.getClosed())
                throw Py.ValueError("I/O operation on closed file");
        }

        public void write(String str) {
            file.write(str);
        }

        public void write(char ch) {
            file.write(cStringIO.getString(ch));
        }

        public void flush() {}

        public String read(int len) {
            return file.read(len).toString();
        }

        public String readlineNoNl() {
            String line = file.readline().toString();
            return line.substring(0, line.length()-1);
        }
    }


    // Use any python object as a file.
    static class ObjectIOFile implements IOFile {
        char[] charr = new char[1];
        StringBuffer buff = new StringBuffer();
        PyObject write;
        PyObject read;
        PyObject readline;
        final int BUF_SIZE = 256;

        ObjectIOFile(PyObject file) {
//          this.file = file;
            write = file.__findattr__("write");
            read = file.__findattr__("read");
            readline = file.__findattr__("readline");
        }

        public void write(String str) {
            buff.append(str);
            if (buff.length() > BUF_SIZE)
                flush();
        }

        public void write(char ch) {
            buff.append(ch);
            if (buff.length() > BUF_SIZE)
                flush();
        }

        public void flush() {
            write.__call__(new PyString(buff.toString()));
            buff.setLength(0);
        }

        public String read(int len) {
            return read.__call__(new PyInteger(len)).toString();
        }

        public String readlineNoNl() {
            String line = readline.__call__().toString();
            return line.substring(0, line.length()-1);
        }
    }


    /**
     * The Pickler object
     * @see cPickle#Pickler(PyObject)
     * @see cPickle#Pickler(PyObject,int)
     */
    static public class Pickler {
        private IOFile file;
        private int protocol;

        /**
         * The undocumented attribute fast of the C version of cPickle disables
         * memoization. Since having memoization on won't break anything, having
         * this dummy setter for fast here won't break any code expecting it to
         * do something. However without it code that sets fast fails(ie
         * test_cpickle.py), so it's worth having.
         */
         public boolean fast = false;

        private PickleMemo memo = new PickleMemo();

        /**
         * To write references to persistent objects, the persistent module
         * must assign a method to persistent_id which returns either None
         * or the persistent ID of the object.
         * For the benefit of persistency modules written using pickle,
         * it supports the notion of a reference to an object outside
         * the pickled data stream.
         * Such objects are referenced by a name, which is an arbitrary
         * string of printable ASCII characters.
         */
        public PyObject persistent_id = null;

        /**
         * Hmm, not documented, perhaps it shouldn't be public? XXX: fixme.
         */
        public PyObject inst_persistent_id = null;


        public Pickler(PyObject file, int protocol) {
            this.file = createIOFile(file);
            this.protocol = protocol;
        }


        /**
         * Write a pickled representation of the object.
         * @param object        The object which will be pickled.
         */
        public void dump(PyObject object) {
            if (protocol >= 2) {
                file.write(PROTO);
                file.write((char) protocol);
            }
            save(object);
            file.write(STOP);
            file.flush();
        }

        private static final int get_id(PyObject o) {
            // we don't pickle Java instances so we don't have to consider that case
            return System.identityHashCode(o);
        }


        // Save name as in pickle.py but semantics are slightly changed.
        private void put(int i) {
            if (protocol > 0) {
                if (i < 256) {
                    file.write(BINPUT);
                    file.write((char)i);
                    return;
                }
                file.write(LONG_BINPUT);
                file.write((char)( i         & 0xFF));
                file.write((char)((i >>>  8) & 0xFF));
                file.write((char)((i >>> 16) & 0xFF));
                file.write((char)((i >>> 24) & 0xFF));
                return;
            }
            file.write(PUT);
            file.write(String.valueOf(i));
            file.write("\n");
        }


        // Same name as in pickle.py but semantics are slightly changed.
        private void get(int i) {
            if (protocol > 0) {
                if (i < 256) {
                    file.write(BINGET);
                    file.write((char)i);
                    return;
                }
                file.write(LONG_BINGET);
                file.write((char)( i         & 0xFF));
                file.write((char)((i >>>  8) & 0xFF));
                file.write((char)((i >>> 16) & 0xFF));
                file.write((char)((i >>> 24) & 0xFF));
                return;
            }
            file.write(GET);
            file.write(String.valueOf(i));
            file.write("\n");
        }


        private void save(PyObject object) {
            save(object, false);
        }


        private void save(PyObject object, boolean pers_save) {
            if (!pers_save) {
                if (persistent_id != null) {
                    PyObject pid = persistent_id.__call__(object);
                    if (pid != Py.None) {
                        save_pers(pid);
                        return;
                    }
                }
            }

            int d = get_id(object);

            PyType t = object.getType();

            if (t == TupleType && object.__len__() == 0) {
                if (protocol > 0)
                    save_empty_tuple(object);
                else
                    save_tuple(object);
                return;
            }

            int m = getMemoPosition(d, object);
            if (m >= 0) {
                get(m);
                return;
            }

            if (save_type(object, t))
                return;

            if (inst_persistent_id != null) {
                PyObject pid = inst_persistent_id.__call__(object);
                if (pid != Py.None) {
                    save_pers(pid);
                    return;
                }
            }

            if (Py.isSubClass(t, PyType.TYPE)) {
                save_global(object);
                return;
            }

            PyObject tup = null;
            PyObject reduce = dispatch_table.__finditem__(t);
            if (reduce == null) {
                reduce = object.__findattr__("__reduce_ex__");
                if (reduce != null) {
                    tup = reduce.__call__(Py.newInteger(protocol));
                } else {
                    reduce = object.__findattr__("__reduce__");
                    if (reduce == null)
                        throw new PyException(UnpickleableError, object);
                    tup = reduce.__call__();
                }
            } else {
                tup = reduce.__call__(object);
            }

            if (tup instanceof PyString) {
                save_global(object, tup);
                return;
            }

            if (!(tup instanceof PyTuple)) {
                throw new PyException(PicklingError,
                            "Value returned by " + reduce.__repr__() +
                            " must be a tuple");
            }

            int l = tup.__len__();
            if (l < 2 || l > 5) {
                throw new PyException(PicklingError,
                            "tuple returned by " + reduce.__repr__() +
                            " must contain two to five elements");
            }

            PyObject callable = tup.__finditem__(0);
            PyObject arg_tup = tup.__finditem__(1);
            PyObject state = (l > 2) ? tup.__finditem__(2) : Py.None;
            PyObject listitems = (l > 3) ? tup.__finditem__(3) : Py.None;
            PyObject dictitems = (l > 4) ? tup.__finditem__(4) : Py.None;

            if (!(arg_tup instanceof PyTuple) && arg_tup != Py.None) {
                throw new PyException(PicklingError,
                            "Second element of tupe returned by " +
                            reduce.__repr__() + " must be a tuple");
            }
            save_reduce(callable, arg_tup, state, listitems, dictitems, putMemo(d, object));

        }


        final private void save_pers(PyObject pid) {
            if (protocol == 0) {
                file.write(PERSID);
                file.write(pid.toString());
                file.write("\n");
            } else {
                save(pid, true);
                file.write(BINPERSID);
            }
        }

        final private void save_reduce(PyObject callable, PyObject arg_tup,
                                       PyObject state, PyObject listitems, PyObject dictitems, int memoId)
        {
            PyObject callableName = callable.__findattr__("__name__");
            if(protocol >= 2 && callableName != null
                    && "__newobj__".equals(callableName.toString())) {
                PyObject cls = arg_tup.__finditem__(0);
                if(cls.__findattr__("__new__") == null)
                    throw new PyException(PicklingError,
                                          "args[0] from __newobj__ args has no __new__");
                // TODO: check class
                save(cls);
                save(arg_tup.__getslice__(Py.One, Py.None));
                file.write(NEWOBJ);
            } else {
                save(callable);
                save(arg_tup);
                file.write(REDUCE);
            }
            put(memoId);
            if (listitems != Py.None) {
                batch_appends(listitems);
            }
            if (dictitems != Py.None) {
                batch_setitems(dictitems);
            }
            if (state != Py.None) {
                save(state);
                file.write(BUILD);
            }
        }



        final private boolean save_type(PyObject object, PyType type) {
            //System.out.println("save_type " + object + " " + cls);
            if (type == NoneType)
                save_none(object);
            else if (type == StringType)
                save_string(object);
            else if (type == UnicodeType)
                save_unicode(object);
            else if (type == IntType)
                save_int(object);
            else if (type == LongType)
                save_long(object);
            else if (type == FloatType)
                save_float(object);
            else if (type == TupleType)
                save_tuple(object);
            else if (type == ListType)
                save_list(object);
            else if (type == DictionaryType || type == StringMapType)
                save_dict(object);
            else if (type == InstanceType)
                save_inst((PyInstance)object);
            else if (type == ClassType)
                save_global(object);
            else if (type == TypeType)
                save_global(object);
            else if (type == FunctionType)
                save_global(object);
            else if (type == BuiltinCallableType)
                save_global(object);
            else if (type == ReflectedFunctionType)
                save_global(object);
            else if (type == BoolType)
                save_bool(object);
            else
                return false;
            return true;
        }



        final private void save_none(PyObject object) {
            file.write(NONE);
        }

        final private void save_int(PyObject object) {
            if (protocol > 0) {
                int l = ((PyInteger)object).getValue();
                char i1 = (char)( l         & 0xFF);
                char i2 = (char)((l >>> 8 ) & 0xFF);
                char i3 = (char)((l >>> 16) & 0xFF);
                char i4 = (char)((l >>> 24) & 0xFF);

                if (i3 == '\0' && i4 == '\0') {
                    if (i2 == '\0') {
                        file.write(BININT1);
                        file.write(i1);
                        return;
                    }
                    file.write(BININT2);
                    file.write(i1);
                    file.write(i2);
                    return;
                }
                file.write(BININT);
                file.write(i1);
                file.write(i2);
                file.write(i3);
                file.write(i4);
            } else {
                file.write(INT);
                file.write(object.toString());
                file.write("\n");
            }
        }

        private void save_bool(PyObject object) {
            int value = ((PyBoolean)object).getValue();
            if(protocol >= 2) {
                file.write(value != 0 ? NEWTRUE : NEWFALSE);
            } else {
                file.write(INT);
                file.write(value != 0 ? "01" : "00");
                file.write("\n");
            }
        }

        private void save_long(PyObject object) {
            if(protocol >= 2) {
                BigInteger integer = ((PyLong)object).getValue();

                if (integer.compareTo(BigInteger.ZERO) == 0) {
                    // It's 0 -- an empty bytestring.
                    file.write(LONG1);
                    file.write((char)0);
                    return;
                }

                byte[] bytes = integer.toByteArray();
                int l = bytes.length;
                if (l < 256) {
                    file.write(LONG1);
                    file.write((char)l);
                } else {
                    file.write(LONG4);
                    writeInt4(l);
                }
                // Write in reverse order: pickle orders by little
                // endian whereas BigInteger orders by big endian
                for (int i = l - 1; i >= 0; i--) {
                    int b = bytes[i] & 0xff;
                    file.write((char)b);
                }
            } else {
                file.write(LONG);
                file.write(object.toString());
                file.write("\n");
            }
        }

        private void writeInt4(int l) {
            char i1 = (char)( l         & 0xFF);
            char i2 = (char)((l >>> 8 ) & 0xFF);
            char i3 = (char)((l >>> 16) & 0xFF);
            char i4 = (char)((l >>> 24) & 0xFF);
            file.write(i1);
            file.write(i2);
            file.write(i3);
            file.write(i4);
        }


        final private void save_float(PyObject object) {
            if (protocol > 0) {
                file.write(BINFLOAT);
                double value= ((PyFloat) object).getValue();
                // It seems that struct.pack('>d', ..) and doubleToLongBits
                // are the same. Good for me :-)
                long bits = Double.doubleToLongBits(value);
                file.write((char)((bits >>> 56) & 0xFF));
                file.write((char)((bits >>> 48) & 0xFF));
                file.write((char)((bits >>> 40) & 0xFF));
                file.write((char)((bits >>> 32) & 0xFF));
                file.write((char)((bits >>> 24) & 0xFF));
                file.write((char)((bits >>> 16) & 0xFF));
                file.write((char)((bits >>>  8) & 0xFF));
                file.write((char)((bits >>>  0) & 0xFF));
            } else {
                file.write(FLOAT);
                file.write(object.toString());
                file.write("\n");
            }
        }


        final private void save_string(PyObject object) {
            boolean unicode = ((PyString) object).isunicode();
            String str = object.toString();

            if (protocol > 0) {
                if (unicode)
                    str = codecs.PyUnicode_EncodeUTF8(str, "struct");
                int l = str.length();
                if (l < 256 && !unicode) {
                    file.write(SHORT_BINSTRING);
                    file.write((char)l);
                } else {
                    if (unicode)
                        file.write(BINUNICODE);
                    else
                        file.write(BINSTRING);
                    file.write((char)( l         & 0xFF));
                    file.write((char)((l >>> 8 ) & 0xFF));
                    file.write((char)((l >>> 16) & 0xFF));
                    file.write((char)((l >>> 24) & 0xFF));
                }
                file.write(str);
            } else {
                if (unicode) {
                    file.write(UNICODE);
                    file.write(codecs.PyUnicode_EncodeRawUnicodeEscape(str,
                                                            "strict", true));
                } else {
                    file.write(STRING);
                    file.write(object.__repr__().toString());
                }
                file.write("\n");
            }
            put(putMemo(get_id(object), object));
        }

        private void save_unicode(PyObject object) {
            if (protocol > 0) {
                String str = codecs.PyUnicode_EncodeUTF8(object.toString(), "struct");
                file.write(BINUNICODE);
                writeInt4(str.length());
                file.write(str);
            } else {
                file.write(UNICODE);
                file.write(codecs.PyUnicode_EncodeRawUnicodeEscape(object.toString(),
                                                        "strict", true));
                file.write("\n");
            }
            put(putMemo(get_id(object), object));
        }

        private void save_tuple(PyObject object) {
            int d = get_id(object);

            int len = object.__len__();

            if (len > 0 && len <= 3 && protocol >= 2) {
                for (int i = 0; i < len; i++)
                    save(object.__finditem__(i));
                int m = getMemoPosition(d, object);
                if (m >= 0) {
                    for (int i = 0; i < len; i++)
                        file.write(POP);
                    get(m);
                }
                else {
                    char opcode = (char) (TUPLE1 + len - 1);
                    file.write(opcode);
                    put(putMemo(d, object));
                }
                return;
            }

            file.write(MARK);

            for (int i = 0; i < len; i++)
                save(object.__finditem__(i));

            if (len > 0) {
                int m = getMemoPosition(d, object);
                if (m >= 0) {
                    if (protocol > 0) {
                        file.write(POP_MARK);
                        get(m);
                        return;
                    }
                    for (int i = 0; i < len+1; i++)
                        file.write(POP);
                    get(m);
                    return;
                }
            }
            file.write(TUPLE);
            put(putMemo(d, object));
        }


        final private void save_empty_tuple(PyObject object) {
            file.write(EMPTY_TUPLE);
        }

        private void save_list(PyObject object) {
            if (protocol > 0)
                file.write(EMPTY_LIST);
            else {
                file.write(MARK);
                file.write(LIST);
            }

            put(putMemo(get_id(object), object));

            batch_appends(object);
        }

        private void batch_appends(PyObject object) {
            int countInBatch = 0;
            for (PyObject nextObj : object.asIterable()) {
                if(protocol == 0) {
                    save(nextObj);
                    file.write(APPEND);
                } else {
                    if(countInBatch == 0) {
                        file.write(MARK);
                    }
                    countInBatch++;
                    save(nextObj);
                    if(countInBatch == BATCHSIZE) {
                        file.write(APPENDS);
                        countInBatch = 0;
                    }
                }
            }
            if (countInBatch > 0)
                file.write(APPENDS);
        }


        private void save_dict(PyObject object) {
            if (protocol > 0)
                file.write(EMPTY_DICT);
            else {
                file.write(MARK);
                file.write(DICT);
            }

            put(putMemo(get_id(object), object));

            batch_setitems(object.invoke("iteritems"));
        }

        private void batch_setitems(PyObject object) {
            if (protocol == 0) {
                // SETITEMS isn't available; do one at a time.
                for (PyObject p : object.asIterable()) {
                    if (!(p instanceof PyTuple) || p.__len__() != 2) {
                        throw Py.TypeError("dict items iterator must return 2-tuples");
                    }
                    save(p.__getitem__(0));
                    save(p.__getitem__(1));
                    file.write(SETITEM);
                }
            } else {
                // proto > 0:  write in batches of BATCHSIZE.
                PyObject obj;
                PyObject[] slice = new PyObject[BATCHSIZE];
                int n;
                do {
                    // Get next group of (no more than) BATCHSIZE elements.
                    for (n = 0; n < BATCHSIZE; n++) {
                        obj = object.__iternext__();
                        if (obj == null) {
                            break;
                        }
                        slice[n] = obj;
                    }

                    if (n > 1) {
                        // Pump out MARK, slice[0:n], APPENDS.
                        file.write(MARK);
                        for (int i = 0; i < n; i++) {
                            obj = slice[i];
                            save(obj.__getitem__(0));
                            save(obj.__getitem__(1));
                        }
                        file.write(SETITEMS);
                    } else if (n == 1) {
                        obj = slice[0];
                        save(obj.__getitem__(0));
                        save(obj.__getitem__(1));
                        file.write(SETITEM);
                    }
                } while (n == BATCHSIZE);
            }
        }


        final private void save_inst(PyInstance object) {
            PyClass cls = object.instclass;

            PySequence args = null;
            PyObject getinitargs = object.__findattr__("__getinitargs__");
            if (getinitargs != null) {
                args = (PySequence)getinitargs.__call__();
                // XXX Assert it's a sequence
                keep_alive(args);
            }

            file.write(MARK);
            if (protocol > 0)
                save(cls);

            if (args != null) {
                int len = args.__len__();
                for (int i = 0; i < len; i++)
                    save(args.__finditem__(i));
            }

            int mid = putMemo(get_id(object), object);
            if (protocol > 0) {
                file.write(OBJ);
                put(mid);
            } else {
                file.write(INST);
                file.write(cls.__findattr__("__module__").toString());
                file.write("\n");
                file.write(cls.__name__);
                file.write("\n");
                put(mid);
            }

            PyObject stuff = null;
            PyObject getstate = object.__findattr__("__getstate__");
            if (getstate == null) {
                stuff = object.__dict__;
            } else {
                stuff = getstate.__call__();
                keep_alive(stuff);
            }
            save(stuff);
            file.write(BUILD);
        }


        final private void save_global(PyObject object) {
            save_global(object, null);
        }


        final private void save_global(PyObject object, PyObject name) {
            if (name == null)
                name = object.__findattr__("__name__");

            PyObject module = object.__findattr__("__module__");
            if (module == null || module == Py.None)
                module = whichmodule(object, name);

            if(protocol >= 2) {
                PyTuple extKey = new PyTuple(module, name);
                PyObject extCode = extension_registry.get(extKey);
                if(extCode != Py.None) {
                    int code = ((PyInteger)extCode).getValue();
                    if(code <= 0xFF) {
                        file.write(EXT1);
                        file.write((char)code);
                    } else if(code <= 0xFFFF) {
                        file.write(EXT2);
                        file.write((char)(code & 0xFF));
                        file.write((char)(code >> 8));
                    } else {
                        file.write(EXT4);
                        writeInt4(code);
                    }
                    return;
                }
            }

            file.write(GLOBAL);
            file.write(module.toString());
            file.write("\n");
            file.write(name.toString());
            file.write("\n");
            put(putMemo(get_id(object), object));
        }


        final private int getMemoPosition(int id, Object o) {
            return memo.findPosition(id, o);
        }

        final private int putMemo(int id, PyObject object) {
            int memo_len = memo.size() + 1;
            memo.put(id, memo_len, object);
            return memo_len;
        }


        /**
         * Keeps a reference to the object x in the memo.
         *
         * Because we remember objects by their id, we have
         * to assure that possibly temporary objects are kept
         * alive by referencing them.
         * We store a reference at the id of the memo, which should
         * normally not be used unless someone tries to deepcopy
         * the memo itself...
         */
        final private void keep_alive(PyObject obj) {
            int id = System.identityHashCode(memo);
            PyList list = (PyList) memo.findValue(id, memo);
            if (list == null) {
                list = new PyList();
                memo.put(id, -1, list);
            }
            list.append(obj);
        }

    }




    private static Hashtable classmap = new Hashtable();

    final private static PyObject whichmodule(PyObject cls,
                                              PyObject clsname)
    {
        PyObject name = (PyObject)classmap.get(cls);
        if (name != null)
            return name;

        name = new PyString("__main__");

        // For use with JPython1.0.x
        //PyObject modules = sys.modules;

        // For use with JPython1.1.x
        //PyObject modules = Py.getSystemState().modules;

        PyObject sys = imp.importName("sys", true);
        PyObject modules = sys.__findattr__("modules");
        PyObject keylist = modules.invoke("keys");

        int len = keylist.__len__();
        for (int i = 0; i < len; i++) {
            PyObject key = keylist.__finditem__(i);
            PyObject value = modules.__finditem__(key);

            if (!key.equals("__main__") &&
                    value.__findattr__(clsname.toString().intern()) == cls) {
                name = key;
                break;
            }
        }

        classmap.put(cls, name);
        //System.out.println(name);
        return name;
    }


    /*
     * A very specialized and simplified version of PyStringMap. It can
     * only use integers as keys and stores both an integer and an object
     * as value. It is very private!
     */
    static private class PickleMemo {
        //Table of primes to cycle through
        private final int[] primes = {
            13, 61, 251, 1021, 4093,
            5987, 9551, 15683, 19609, 31397,
            65521, 131071, 262139, 524287, 1048573, 2097143,
            4194301, 8388593, 16777213, 33554393, 67108859,
            134217689, 268435399, 536870909, 1073741789,};

        private transient int[] keys;
        private transient int[] position;
        private transient Object[] values;

        private int size;
        private transient int filled;
        private transient int prime;

        public PickleMemo(int capacity) {
            prime = 0;
            keys = null;
            values = null;
            resize(capacity);
        }

        public PickleMemo() {
            this(4);
        }

        public synchronized int size() {
            return size;
        }

        private int findIndex(int key, Object value) {
            int[] table = keys;
            int maxindex = table.length;
            int index = (key & 0x7fffffff) % maxindex;

            // Fairly aribtrary choice for stepsize...
            int stepsize = maxindex / 5;

            // Cycle through possible positions for the key;
            //int collisions = 0;
            while (true) {
                int tkey = table[index];
                if (tkey == key && value == values[index]) {
                    return index;
                }
                if (values[index] == null) return -1;
                index = (index+stepsize) % maxindex;
            }
        }

        public int findPosition(int key, Object value) {
            int idx = findIndex(key, value);
            if (idx < 0) return -1;
            return position[idx];
        }


        public Object findValue(int key, Object value) {
            int idx = findIndex(key, value);
            if (idx < 0) return null;
            return values[idx];
        }


        private final void insertkey(int key, int pos, Object value) {
            int[] table = keys;
            int maxindex = table.length;
            int index = (key & 0x7fffffff) % maxindex;

            // Fairly aribtrary choice for stepsize...
            int stepsize = maxindex / 5;

            // Cycle through possible positions for the key;
            while (true) {
                int tkey = table[index];
                if (values[index] == null) {
                    table[index] = key;
                    position[index] = pos;
                    values[index] = value;
                    filled++;
                    size++;
                    break;
                } else if (tkey == key && values[index] == value) {
                    position[index] = pos;
                    break;
                }
                index = (index+stepsize) % maxindex;
            }
        }


        private synchronized final void resize(int capacity) {
            int p = prime;
            for(; p<primes.length; p++) {
                if (primes[p] >= capacity) break;
            }
            if (primes[p] < capacity) {
                throw Py.ValueError("can't make hashtable of size: " +
                                    capacity);
            }
            capacity = primes[p];
            prime = p;

            int[] oldKeys = keys;
            int[] oldPositions = position;
            Object[] oldValues = values;

            keys = new int[capacity];
            position = new int[capacity];
            values = new Object[capacity];
            size = 0;
            filled = 0;

            if (oldValues != null) {
                int n = oldValues.length;

                for(int i=0; i<n; i++) {
                    Object value = oldValues[i];
                    if (value == null) continue;
                    insertkey(oldKeys[i], oldPositions[i], value);
                }
            }
        }

        public void put(int key, int pos, Object value) {
            if (2*filled > keys.length) resize(keys.length+1);
            insertkey(key, pos, value);
        }
    }






    /**
     * The Unpickler object. Unpickler instances are create by the factory
     * methods Unpickler.
     * @see cPickle#Unpickler(PyObject)
     */
    static public class Unpickler {

        private IOFile file;

        public Hashtable memo = new Hashtable();

        /**
         * For the benefit of persistency modules written using pickle,
         * it supports the notion of a reference to an object outside
         * the pickled data stream.
         * Such objects are referenced by a name, which is an arbitrary
         * string of printable ASCII characters.
         * The resolution of such names is not defined by the pickle module
         * -- the persistent object module will have to add a method
         * persistent_load().
         */
        public PyObject persistent_load = null;

        private PyObject mark = new PyString("spam");

        private int stackTop;
        private PyObject[] stack;


        Unpickler(PyObject file) {
            this.file = createIOFile(file);
        }


        /**
         * Unpickle and return an instance of the object represented by
         * the file.
         */
        public PyObject load() {
            stackTop = 0;
            stack = new PyObject[10];

            while (true) {
                String s = file.read(1);
//              System.out.println("load:" + s);
//              for (int i = 0; i < stackTop; i++)
//                  System.out.println("   " + stack[i]);
                if (s.length() < 1)
                    load_eof();
                char key = s.charAt(0);
                switch (key) {
                case PERSID:          load_persid(); break;
                case BINPERSID:       load_binpersid(); break;
                case NONE:            load_none(); break;
                case INT:             load_int(); break;
                case BININT:          load_binint(); break;
                case BININT1:         load_binint1(); break;
                case BININT2:         load_binint2(); break;
                case LONG:            load_long(); break;
                case FLOAT:           load_float(); break;
                case BINFLOAT:        load_binfloat(); break;
                case STRING:          load_string(); break;
                case BINSTRING:       load_binstring(); break;
                case SHORT_BINSTRING: load_short_binstring(); break;
                case UNICODE:         load_unicode(); break;
                case BINUNICODE:      load_binunicode(); break;
                case TUPLE:           load_tuple(); break;
                case EMPTY_TUPLE:     load_empty_tuple(); break;
                case EMPTY_LIST:      load_empty_list(); break;
                case EMPTY_DICT:      load_empty_dictionary(); break;
                case LIST:            load_list(); break;
                case DICT:            load_dict(); break;
                case INST:            load_inst(); break;
                case OBJ:             load_obj(); break;
                case GLOBAL:          load_global(); break;
                case REDUCE:          load_reduce(); break;
                case POP:             load_pop(); break;
                case POP_MARK:        load_pop_mark(); break;
                case DUP:             load_dup(); break;
                case GET:             load_get(); break;
                case BINGET:          load_binget(); break;
                case LONG_BINGET:     load_long_binget(); break;
                case PUT:             load_put(); break;
                case BINPUT:          load_binput(); break;
                case LONG_BINPUT:     load_long_binput(); break;
                case APPEND:          load_append(); break;
                case APPENDS:         load_appends(); break;
                case SETITEM:         load_setitem(); break;
                case SETITEMS:        load_setitems(); break;
                case BUILD:           load_build(); break;
                case MARK:            load_mark(); break;
                case PROTO:           load_proto(); break;
                case NEWOBJ:          load_newobj(); break;
                case EXT1:            load_ext(1); break;
                case EXT2:            load_ext(2); break;
                case EXT4:            load_ext(4); break;
                case TUPLE1:          load_small_tuple(1); break;
                case TUPLE2:          load_small_tuple(2); break;
                case TUPLE3:          load_small_tuple(3); break;
                case NEWTRUE:         load_boolean(true); break;
                case NEWFALSE:        load_boolean(false); break;
                case LONG1:           load_bin_long(1); break;
                case LONG4:           load_bin_long(4); break;
                case STOP:
                    return load_stop();
                }
            }
        }


        final private int marker() {
            for (int k = stackTop-1; k >= 0; k--)
                if (stack[k] == mark)
                    return stackTop-k-1;
            throw new PyException(UnpicklingError,
                        "Inputstream corrupt, marker not found");
        }


        final private void load_eof() {
            throw new PyException(Py.EOFError);
        }

        private void load_proto() {
            int proto = file.read(1).charAt(0);
            if (proto < 0 || proto > 2)
                throw Py.ValueError("unsupported pickle protocol: " + proto);
        }


        final private void load_persid() {
            String pid = file.readlineNoNl();
            push(persistent_load.__call__(new PyString(pid)));
        }


        final private void load_binpersid() {
            PyObject pid = pop();
            push(persistent_load.__call__(pid));
        }


        final private void load_none() {
            push(Py.None);
        }

        final private void load_int() {
            String line = file.readlineNoNl();
            PyObject value;
            // The following could be abstracted into a common string
            // -> int/long method.
            if (line.equals("01")) {
                value = Py.True;
            }
            else if (line.equals("00")) {
                value = Py.False;
            }
            else {
                try {
                    value = Py.newInteger(Integer.parseInt(line));
                } catch(NumberFormatException e) {
                    try {
                        value = Py.newLong(line);
                    } catch(NumberFormatException e2) {
                        throw Py.ValueError("could not convert string to int");
                    }
                }
            }
            push(value);
        }

        private void load_boolean(boolean value) {
            push(value ? Py.True : Py.False);
        }

        final private void load_binint() {
            int x = read_binint();
            push(new PyInteger(x));
        }

        private int read_binint() {
            String s = file.read(4);
            return s.charAt(0) |
                   (s.charAt(1)<<8) |
                   (s.charAt(2)<<16) |
                   (s.charAt(3)<<24);
        }


        final private void load_binint1() {
            int val = file.read(1).charAt(0);
            push(new PyInteger(val));
        }

        final private void load_binint2() {
            int val = read_binint2();
            push(new PyInteger(val));
        }

        private int read_binint2() {
            String s = file.read(2);
            return (s.charAt(1)) << 8 | (s.charAt(0));
        }


        final private void load_long() {
            String line = file.readlineNoNl();
            push(new PyLong(line.substring(0, line.length()-1)));
        }

        private void load_bin_long(int length) {
            int longLength = read_binint(length);
            if (longLength == 0) {
                push(new PyLong(BigInteger.ZERO));
                return;
            }
            String s = file.read(longLength);
            byte[] bytes = new byte[s.length()];
            // Write to the byte array in reverse order: pickle orders
            // by little endian whereas BigInteger orders by big
            // endian
            int n = s.length() - 1;
            for (int i = 0; i < s.length(); i++, n--) {
                char c = s.charAt(i);
                if(c >= 128) {
                    bytes[n] = (byte)(c - 256);
                } else {
                    bytes[n] = (byte)c;
                }
            }
            BigInteger bigint = new BigInteger(bytes);
            push(new PyLong(bigint));
        }

        private int read_binint(int length) {
            if (length == 1)
                return file.read(1).charAt(0);
            else if (length == 2)
                return read_binint2();
            else
                return read_binint();
        }

        final private void load_float() {
            String line = file.readlineNoNl();
            push(new PyFloat(Double.valueOf(line).doubleValue()));
        }

        final private void load_binfloat() {
            String s = file.read(8);
            long bits = s.charAt(7) |
                        ((long)s.charAt(6) << 8) |
                        ((long)s.charAt(5) << 16) |
                        ((long)s.charAt(4) << 24) |
                        ((long)s.charAt(3) << 32) |
                        ((long)s.charAt(2) << 40) |
                        ((long)s.charAt(1) << 48) |
                        ((long)s.charAt(0) << 56);
            push(new PyFloat(Double.longBitsToDouble(bits)));
        }

        final private void load_string() {
            String line = file.readlineNoNl();

            String value;
            char quote = line.charAt(0);
            if (quote != '"' && quote != '\'')
                throw Py.ValueError("insecure string pickle");

            int nslash = 0;
            int i;
            char ch = '\0';
            int n = line.length();
            for (i = 1; i < n; i++) {
                ch = line.charAt(i);
                if (ch == quote && nslash % 2 == 0)
                    break;
                if (ch == '\\')
                    nslash++;
                else
                    nslash = 0;
            }
            if (ch != quote)
                throw Py.ValueError("insecure string pickle");

            for (i++ ; i < line.length(); i++) {
                if (line.charAt(i) > ' ')
                    throw Py.ValueError("insecure string pickle " + i);
            }
            value = PyString.decode_UnicodeEscape(line, 1, n-1,
                                                  "strict", false);

            push(new PyString(value));
        }


        final private void load_binstring() {
            int len = read_binint();
            push(new PyString(file.read(len)));
        }


        final private void load_short_binstring() {
            int len = file.read(1).charAt(0);
            push(new PyString(file.read(len)));
        }


        final private void load_unicode() {
            String line = file.readlineNoNl();
            String value = codecs.PyUnicode_DecodeRawUnicodeEscape(line,
                                                                   "strict");
            push(new PyUnicode(value));
        }

        final private void load_binunicode() {
            int len = read_binint();
            String line = file.read(len);
            push(new PyUnicode(codecs.PyUnicode_DecodeUTF8(line, "strict")));
        }

        final private void load_tuple() {
            PyObject[] arr = new PyObject[marker()];
            pop(arr);
            pop();
            push(new PyTuple(arr));
        }

        final private void load_empty_tuple() {
            push(new PyTuple(Py.EmptyObjects));
        }

        private void load_small_tuple(int length) {
            PyObject[] data = new PyObject[length];
            for(int i=length-1; i >= 0; i--) {
                data [i] = pop();
            }
            push(new PyTuple(data));
        }

        final private void load_empty_list() {
            push(new PyList(Py.EmptyObjects));
        }

        final private void load_empty_dictionary() {
            push(new PyDictionary());
        }


        final private void load_list() {
            PyObject[] arr = new PyObject[marker()];
            pop(arr);
            pop();
            push(new PyList(arr));
        }


        final private void load_dict() {
            int k = marker();
            PyDictionary d = new PyDictionary();
            for (int i = 0; i < k; i += 2) {
                PyObject value = pop();
                PyObject key = pop();
                d.__setitem__(key, value);
            }
            pop();
            push(d);
        }


        final private void load_inst() {
            PyObject[] args = new PyObject[marker()];
            pop(args);
            pop();

            String module = file.readlineNoNl();
            String name = file.readlineNoNl();
            PyObject klass = find_class(module, name);

            PyObject value = null;
            if (args.length == 0 && klass instanceof PyClass &&
                        klass.__findattr__("__getinitargs__") == null) {
                value = new PyInstance((PyClass)klass);
            } else {
                value = klass.__call__(args);
            }
            push(value);
        }


        final private void load_obj() {
            PyObject[] args = new PyObject[marker()-1];
            pop(args);
            PyObject klass = pop();
            pop();

            PyObject value = null;
            if (args.length == 0 && klass instanceof PyClass &&
                        klass.__findattr__("__getinitargs__") == null) {
                value = new PyInstance((PyClass)klass);
            } else {
                value = klass.__call__(args);
            }
            push(value);
        }

        final private void load_global() {
            String module = file.readlineNoNl();
            String name = file.readlineNoNl();
            PyObject klass = find_class(module, name);
            push(klass);
        }


        final private PyObject find_class(String module, String name) {
            PyObject fc = dict.__finditem__("find_global");
            if (fc != null) {
               if (fc == Py.None)
                   throw new PyException(UnpicklingError,
                         "Global and instance pickles are not supported.");
               return fc.__call__(new PyString(module), new PyString(name));
            }

            PyObject modules = Py.getSystemState().modules;
            PyObject mod = modules.__finditem__(module.intern());
            if (mod == null) {
                mod = importModule(module);
            }
            PyObject global = mod.__findattr__(name.intern());
            if (global == null) {
                throw new PyException(Py.SystemError,
                          "Failed to import class " + name + " from module " +
                          module);
            }
            return global;
        }

        private void load_ext(int length) {
            int code = read_binint(length);
            // TODO: support _extension_cache
            PyObject key = inverted_registry.get(Py.newInteger(code));
            if (key == null) {
                throw new PyException(Py.ValueError, "unregistered extension code " + code);
            }
            String module = key.__finditem__(0).toString();
            String name = key.__finditem__(1).toString();
            push(find_class(module, name));
        }


        final private void load_reduce() {
            PyObject arg_tup = pop();
            PyObject callable = pop();
            PyObject value = null;
            if (arg_tup == Py.None) {
                // XXX __basicnew__ ?
                value = callable.__findattr__("__basicnew__").__call__();
            } else {
                value = callable.__call__(make_array(arg_tup));
            }
            push(value);
        }

        private void load_newobj() {
            PyObject arg_tup = pop();
            PyObject cls = pop();
            PyObject[] args = new PyObject[arg_tup.__len__() + 1];
            args [0] = cls;
            for(int i=1; i<args.length; i++) {
                args [i] = arg_tup.__finditem__(i-1);
            }
            push(cls.__getattr__("__new__").__call__(args));
        }

        final private PyObject[] make_array(PyObject seq) {
            int n = seq.__len__();
            PyObject[] objs= new PyObject[n];

            for(int i=0; i<n; i++)
                objs[i] = seq.__finditem__(i);
            return objs;
        }

        final private void load_pop() {
            pop();
        }


        final private void load_pop_mark() {
            pop(marker());
        }

        final private void load_dup() {
            push(peek());
        }

        final private void load_get() {
            String py_str = file.readlineNoNl();
            PyObject value = (PyObject)memo.get(py_str);
            if (value == null)
                throw new PyException(BadPickleGet, py_str);
            push(value);
        }

        final private void load_binget() {
            String py_key = String.valueOf((int)file.read(1).charAt(0));
            PyObject value = (PyObject)memo.get(py_key);
            if (value == null)
                throw new PyException(BadPickleGet, py_key);
            push(value);
        }

        final private void load_long_binget() {
            int i = read_binint();
            String py_key = String.valueOf(i);
            PyObject value = (PyObject)memo.get(py_key);
            if (value == null)
                throw new PyException(BadPickleGet, py_key);
            push(value);
        }


        final private void load_put() {
            memo.put(file.readlineNoNl(), peek());
        }


        final private void load_binput() {
            int i = file.read(1).charAt(0);
            memo.put(String.valueOf(i), peek());
        }


        final private void load_long_binput() {
            int i = read_binint();
            memo.put(String.valueOf(i), peek());
        }

        final private void load_append() {
            PyObject value = pop();
            PyObject obj = peek();
            if(obj instanceof PyList) {
                ((PyList)obj).append(value);
            } else {
                PyObject appender = obj.__getattr__("append");
                appender.__call__(value);
            }
        }

        final private void load_appends() {
            int mark = marker();
            PyObject obj = peek(mark + 1);
            if(obj instanceof PyList) {
                for(int i = mark - 1; i >= 0; i--) {
                    ((PyList)obj).append(peek(i));
                }
            } else {
                PyObject appender = obj.__getattr__("append");
                for(int i = mark - 1; i >= 0; i--) {
                    appender.__call__(peek(i));
                }
            }
            pop(mark + 1);
        }

        final private void load_setitem() {
            PyObject value = pop();
            PyObject key   = pop();
            PyDictionary dict = (PyDictionary)peek();
            dict.__setitem__(key, value);
        }


        final private void load_setitems() {
            int mark = marker();
            PyDictionary dict = (PyDictionary)peek(mark+1);
            for (int i = 0; i < mark; i += 2) {
                PyObject key   = peek(i+1);
                PyObject value = peek(i);
                dict.__setitem__(key, value);
            }
            pop(mark+1);
        }

        private void load_build() {
            PyObject value = pop();
            PyObject inst  = peek();
            PyObject setstate = inst.__findattr__("__setstate__");
            if(setstate == null) {
                PyObject slotstate = null;
                // A default __setstate__.  First see whether state
                // embeds a slot state dict too (a proto 2 addition).
                if (value instanceof PyTuple && value.__len__() == 2) {
                    PyObject temp = value;
                    value = temp.__getitem__(0);
                    slotstate = temp.__getitem__(1);
                }

                PyObject dict;
                if(inst instanceof PyInstance) {
                    dict = ((PyInstance)inst).__dict__;
                } else {
                    dict = inst.getDict();
                }
                dict.__findattr__("update").__call__(value);

                // Also set instance attributes from the slotstate
                // dict (if any).
                if (slotstate != null) {
                    if (!(slotstate instanceof PyDictionary)) {
                        throw new PyException(UnpicklingError, "slot state is not a dictionary");
                    }
                    for (PyObject item : ((PyDictionary)slotstate).iteritems().asIterable()) {
                        inst.__setattr__(PyObject.asName(item.__getitem__(0)),
                                         item.__getitem__(1));
                    }
                }
            } else {
                setstate.__call__(value);
            }
        }

        final private void load_mark() {
            push(mark);
        }

        final private PyObject load_stop() {
            return pop();
        }



        final private PyObject peek() {
            return stack[stackTop-1];
        }

        final private PyObject peek(int count) {
            return stack[stackTop-count-1];
        }


        final private PyObject pop() {
            PyObject val = stack[--stackTop];
            stack[stackTop] = null;
            return val;
        }

        final private void pop(int count) {
            for (int i = 0; i < count; i++)
                stack[--stackTop] = null;
        }


        final private void pop(PyObject[] arr) {
            int len = arr.length;
            System.arraycopy(stack, stackTop - len, arr, 0, len);
            stackTop -= len;
        }

        final private void push(PyObject val) {
            if (stackTop >= stack.length) {
                PyObject[] newStack = new PyObject[(stackTop+1) * 2];
                System.arraycopy(stack, 0, newStack, 0, stack.length);
                stack = newStack;
            }
            stack[stackTop++] = val;
        }
    }


    private static PyObject importModule(String name) {
        PyObject fromlist = new PyTuple(Py.newString("__doc__"));
        return __builtin__.__import__(name, null, null, fromlist);
    }

    private static PyObject getJavaFunc(String name, String methodName) {
        return exceptions.bindStaticJavaMethod(name, cPickle.class, methodName);
    }
}
