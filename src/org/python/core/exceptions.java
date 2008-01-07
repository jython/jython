// Copyright 2001 Finn Bock

package org.python.core;
import org.python.modules.zipimport.zipimport;

/**
 * The builtin exceptions module. The entire module should be imported from
 * python. None of the methods defined here should be called from java.
 */

public class exceptions implements ClassDictInit {

    public static String __doc__ = "Python's standard exception class hierarchy.\n"
            + "\n"
            + "Here is a rundown of the class hierarchy.  The classes found here are\n"
            + "inserted into both the exceptions module and the `built-in' module. "
            + "It is\n"
            + "recommended that user defined class based exceptions be derived from the\n"
            + "`Exception' class, although this is currently not enforced.\n"
            + "\n"
            + "Exception\n"
            + " |\n"
            + " +-- GeneratorExit\n"
            + " +-- SystemExit\n"
            + " +-- StopIteration\n"
            + " +-- StandardError\n"
            + " |    |\n"
            + " |    +-- KeyboardInterrupt\n"
            + " |    +-- ImportError\n"
            + " |    +-- EnvironmentError\n"
            + " |    |    |\n"
            + " |    |    +-- IOError\n"
            + " |    |    +-- OSError\n"
            + " |    |         |\n"
            + " |    |         +-- WindowsError\n"
            + " |    |\n"
            + " |    +-- EOFError\n"
            + " |    +-- RuntimeError\n"
            + " |    |    |\n"
            + " |    |    +-- NotImplementedError\n"
            + " |    |\n"
            + " |    +-- NameError\n"
            + " |    |    |\n"
            + " |    |    +-- UnboundLocalError\n"
            + " |    |\n"
            + " |    +-- AttributeError\n"
            + " |    +-- SyntaxError\n"
            + " |    |    |\n"
            + " |    |    +-- IndentationError\n"
            + " |    |         |\n"
            + " |    |         +-- TabError\n"
            + " |    |\n"
            + " |    +-- TypeError\n"
            + " |    +-- AssertionError\n"
            + " |    +-- LookupError\n"
            + " |    |    |\n"
            + " |    |    +-- IndexError\n"
            + " |    |    +-- KeyError\n"
            + " |    |\n"
            + " |    +-- ArithmeticError\n"
            + " |    |    |\n"
            + " |    |    +-- OverflowError\n"
            + " |    |    +-- ZeroDivisionError\n"
            + " |    |    +-- FloatingPointError\n"
            + " |    |\n"
            + " |    +-- ValueError\n"
            + " |    |    |\n"
            + " |    |    +-- UnicodeError\n"
            + " |    |        |\n"
            + " |    |        +-- UnicodeEncodeError\n"
            + " |    |        +-- UnicodeDecodeError\n"
            + " |    |        +-- UnicodeTranslateError\n"
            + " |    |\n"
            + " |    +-- ReferenceError\n"
            + " |    +-- SystemError\n"
            + " |    +-- MemoryError\n"
            + " |\n"
            + " +---Warning\n"
            + "      |\n"
            + "      +-- UserWarning\n"
            + "      +-- DeprecationWarning\n"
            + "      +-- SyntaxWarning\n"
            + "      +-- OverflowWarning\n" + "      +-- RuntimeWarning";

    private exceptions() {
        ;
    }

    /** <i>Internal use only. Do not call this method explicit.</i> */
    public static void classDictInit(PyObject dict) {
        dict.invoke("clear");
        dict.__setitem__("__name__", new PyString("exceptions"));
        dict.__setitem__("__doc__", new PyString(__doc__));

        ThreadState ts = Py.getThreadState();
        if (ts.systemState == null) {
            ts.systemState = Py.defaultSystemState;
        }
        // Push frame
        PyFrame frame = new PyFrame(null, new PyStringMap());
        frame.f_back = ts.frame;
        if (frame.f_builtins == null) {
            if (frame.f_back != null) {
                frame.f_builtins = frame.f_back.f_builtins;
            } else {
                frame.f_builtins = PySystemState.builtins;
            }
        }
        ts.frame = frame;

        buildClass(dict, "Exception", null, "Exception",
                "Proposed base class for all exceptions.");

        buildClass(dict, "StandardError", "Exception", "empty__init__",
                "Base class for all standard Python exceptions.");

        buildClass(dict, "SyntaxError", "StandardError", "SyntaxError",
                "Invalid syntax");

        buildClass(dict, "IndentationError", "SyntaxError", "empty__init__",
                "Improper indentation");

        buildClass(dict, "TabError", "IndentationError", "empty__init__",
                "Improper mixture of spaces and tabs.");

        buildClass(dict, "EnvironmentError", "StandardError",
                "EnvironmentError", "Base class for I/O related errors.");

        buildClass(dict, "IOError", "EnvironmentError", "empty__init__",
                "I/O operation failed.");

        buildClass(dict, "OSError", "EnvironmentError", "empty__init__",
                "OS system call failed.");

        buildClass(dict, "RuntimeError", "StandardError", "empty__init__",
                "Unspecified run-time error.");

        buildClass(dict, "NotImplementedError", "RuntimeError",
                "empty__init__",
                "Method or function hasn't been implemented yet.");

        buildClass(dict, "SystemError", "StandardError", "empty__init__",
                "Internal error in the Python interpreter.\n\n"
                        + "Please report this to the Python maintainer, "
                        + "along with the traceback,\n"
                        + "the Python version, and the hardware/OS "
                        + "platform and version.");

        buildClass(dict, "ReferenceError", "StandardError", "empty__init__",
                "Weak ref proxy used after referent went away.");

        buildClass(dict, "EOFError", "StandardError", "empty__init__",
                "Read beyond end of file.");

        buildClass(dict, "ImportError", "StandardError", "empty__init__",
                "Import can't find module, or can't find name in module.");

        buildClass(dict, "TypeError", "StandardError", "empty__init__",
                "Inappropriate argument type.");

        buildClass(dict, "ValueError", "StandardError", "empty__init__",
                "Inappropriate argument value (of correct type).");

        buildClass(dict, "UnicodeError", "ValueError", "empty__init__",
                "Unicode related error.");

        buildClass(dict, "UnicodeEncodeError", "UnicodeError", "UnicodeEncodeError",
                "Unicode encoding error.");

        buildClass(dict, "UnicodeDecodeError", "UnicodeError", "UnicodeDecodeError",
                "Unicode decoding error.");

        buildClass(dict, "UnicodeTranslateError", "UnicodeError", "UnicodeTranslateError",
                "Unicode translation error.");

        buildClass(dict, "KeyboardInterrupt", "StandardError", "empty__init__",
                "Program interrupted by user.");

        buildClass(dict, "AssertionError", "StandardError", "empty__init__",
                "Assertion failed.");

        buildClass(dict, "ArithmeticError", "StandardError", "empty__init__",
                "Base class for arithmetic errors.");

        buildClass(dict, "OverflowError", "ArithmeticError", "empty__init__",
                "Result too large to be represented.");

        buildClass(dict, "FloatingPointError", "ArithmeticError",
                "empty__init__", "Floating point operation failed.");

        buildClass(dict, "ZeroDivisionError", "ArithmeticError",
                "empty__init__",
                "Second argument to a division or modulo operation "
                        + "was zero.");

        buildClass(dict, "LookupError", "StandardError", "empty__init__",
                "Base class for lookup errors.");

        buildClass(dict, "IndexError", "LookupError", "empty__init__",
                "Sequence index out of range.");

        buildClass(dict, "KeyError", "LookupError", "empty__init__",
                "Mapping key not found.");

        buildClass(dict, "AttributeError", "StandardError", "empty__init__",
                "Attribute not found.");

        buildClass(dict, "NameError", "StandardError", "empty__init__",
                "Name not found globally.");

        buildClass(dict, "UnboundLocalError", "NameError", "empty__init__",
                "Local name referenced but not bound to a value.");

        buildClass(dict, "MemoryError", "StandardError", "empty__init__",
                "Out of memory.");

        buildClass(dict, "SystemExit", "Exception", "SystemExit",
                "Request to exit from the interpreter.");

        buildClass(dict, "StopIteration", "Exception", "empty__init__",
                "Signal the end from iterator.next().");
        
        buildClass(dict, "GeneratorExit", "Exception", "empty__init__",
                "Request that a generator exit.");

        buildClass(dict, "Warning", "Exception", "empty__init__",
                "Base class for warning categories.");

        buildClass(dict, "UserWarning", "Warning", "empty__init__",
                "Base class for warnings generated by user code.");

        buildClass(dict, "DeprecationWarning", "Warning", "empty__init__",
                "Base class for warnings about deprecated features.");
        
        buildClass(dict, "PendingDeprecationWarning", "Warning", "empty__init__",
                "Base class for warnings about features which will be deprecated in the future.");

        buildClass(dict, "SyntaxWarning", "Warning", "empty__init__",
                "Base class for warnings about dubious syntax.");

        buildClass(dict, "RuntimeWarning", "Warning", "empty__init__",
                "Base class for warnings about dubious runtime behavior.");

        buildClass(dict, "OverflowWarning", "Warning", "empty__init__",
                "Base class for warnings about numeric overflow.");
        
        buildClass(dict, "FutureWarning", "Warning", "empty__init__",
                "Base class for warnings about constructs that will change semantically in the future.");

        // Initialize ZipImportError here, where it's safe to; it's
        // needed immediately
        zipimport.initClassExceptions(dict);

        ts.frame = ts.frame.f_back;
    }

    // An empty __init__ method
    public static PyObject empty__init__(PyObject[] arg, String[] kws) {
        PyObject dict = new PyStringMap();
        dict.__setitem__("__module__", new PyString("exceptions"));
        return dict;
    }

    public static PyObject Exception(PyObject[] arg, String[] kws) {
        PyObject dict = empty__init__(arg, kws);
        dict.__setitem__("__init__", getJavaFunc("Exception__init__"));
        dict.__setitem__("__str__", getJavaFunc("Exception__str__"));
        dict.__setitem__("__getitem__", getJavaFunc("Exception__getitem__"));
        return dict;
    }

    public static void Exception__init__(PyObject[] arg, String[] kws) {
        ArgParser ap = new ArgParser("__init__", arg, kws, "self", "args");
        PyObject self = ap.getPyObject(0);
        PyObject args = ap.getList(1);
        
        if (arg.length == 2) {
            self.__setattr__("message", ap.getPyObject(1));
        }
        self.__setattr__("args", args);
    }

    public static PyString Exception__str__(PyObject[] arg, String[] kws) {
        ArgParser ap = new ArgParser("__str__", arg, kws, "self");
        PyObject self = ap.getPyObject(0);

        PyObject args = self.__getattr__("args");
        if (!args.__nonzero__()) {
            return new PyString("");
        } else if (args.__len__() == 1) {
            return args.__getitem__(0).__str__();
        } else {
            return args.__str__();
        }
    }

    public static PyObject Exception__getitem__(PyObject[] arg, String[] kws) {
        ArgParser ap = new ArgParser("__getitem__", arg, kws, "self", "i");
        PyObject self = ap.getPyObject(0);
        PyObject i = ap.getPyObject(1);

        return self.__getattr__("args").__getitem__(i);
    }

    public static PyObject SyntaxError(PyObject[] arg, String[] kws) {
        PyObject __dict__ = empty__init__(arg, kws);
        __dict__.__setitem__("filename", Py.None);
        __dict__.__setitem__("lineno", Py.None);
        __dict__.__setitem__("offset", Py.None);
        __dict__.__setitem__("text", Py.None);
        __dict__.__setitem__("msg", new PyString(""));

        __dict__.__setitem__("__init__", getJavaFunc("SyntaxError__init__"));
        __dict__.__setitem__("__str__", getJavaFunc("SyntaxError__str__"));
        return __dict__;
    }

    public static void SyntaxError__init__(PyObject[] arg, String[] kws) {
        ArgParser ap = new ArgParser("__init__", arg, kws, "self", "args");
        PyObject self = ap.getPyObject(0);
        PyObject args = ap.getList(1);

        self.__setattr__("args", args);
        if (args.__len__() >= 1) {
            self.__setattr__("msg", args.__getitem__(0));
        }
        if (args.__len__() == 2) {
            PyObject info = args.__getitem__(1);
            try {
                PyObject[] tmp = Py.unpackSequence(info, 4);
                self.__setattr__("filename", tmp[0]);
                self.__setattr__("lineno", tmp[1]);
                self.__setattr__("offset", tmp[2]);
                self.__setattr__("text", tmp[3]);
            } catch (PyException exc) {
                ;
            }
        }
    }

    public static PyString SyntaxError__str__(PyObject[] arg, String[] kws) {
        ArgParser ap = new ArgParser("__str__", arg, kws, "self", "args");
        PyObject self = ap.getPyObject(0);
        PyString str = self.__getattr__("msg").__str__();
        PyObject filename = basename(self.__findattr__("filename"));
        PyObject lineno = self.__findattr__("lineno");
        if (filename instanceof PyString && lineno instanceof PyInteger) {
            return new PyString(str + " (" + filename + ", line " + lineno
                    + ")");
        } else if (filename instanceof PyString) {
            return new PyString(str + " (" + filename + ")");
        } else if (lineno instanceof PyInteger) {
            return new PyString(str + " (line " + lineno + ")");
        }
        return str;
    }

    private static PyObject basename(PyObject filename) {
        if (filename instanceof PyString) {
            int i = ((PyString) filename).rfind(java.io.File.separator);
            if (i >= 0) {
                return filename.__getslice__(new PyInteger(i + 1),
                        new PyInteger(Integer.MAX_VALUE));
            }
        }
        return filename;
    }

    public static PyObject EnvironmentError(PyObject[] arg, String[] kws) {
        PyObject dict = empty__init__(arg, kws);
        dict.__setitem__("__init__", getJavaFunc("EnvironmentError__init__"));
        dict.__setitem__("__str__", getJavaFunc("EnvironmentError__str__"));
        return dict;
    }

    public static void EnvironmentError__init__(PyObject[] arg, String[] kws) {
        ArgParser ap = new ArgParser("__init__", arg, kws, "self", "args");
        PyObject self = ap.getPyObject(0);
        PyObject args = ap.getList(1);

        self.__setattr__("args", args);
        self.__setattr__("errno", Py.None);
        self.__setattr__("strerror", Py.None);
        self.__setattr__("filename", Py.None);
        if (args.__len__() == 3) {
            // open() errors give third argument which is the filename. BUT,
            // so common in-place unpacking doesn't break, e.g.:
            //
            // except IOError, (errno, strerror):
            //
            // we hack args so that it only contains two items. This also
            // means we need our own __str__() which prints out the filename
            // when it was supplied.
            PyObject[] tmp = Py.unpackSequence(args, 3);
            self.__setattr__("errno", tmp[0]);
            self.__setattr__("strerror", tmp[1]);
            self.__setattr__("filename", tmp[2]);
            self.__setattr__("args", args.__getslice__(Py.Zero, Py
                    .newInteger(2), Py.One));
        }
        if (args.__len__() == 2) {
            // common case: PyErr_SetFromErrno()
            PyObject[] tmp = Py.unpackSequence(args, 2);
            self.__setattr__("errno", tmp[0]);
            self.__setattr__("strerror", tmp[1]);
        }
    }

    public static PyString EnvironmentError__str__(PyObject[] arg, String[] kws) {
        ArgParser ap = new ArgParser("__str__", arg, kws, "self");
        PyObject self = ap.getPyObject(0);

        if (self.__getattr__("filename") != Py.None) {
            return Py.newString("[Errno %s] %s: %r").__mod__(
                    new PyTuple( self.__getattr__("errno"),
                            self.__getattr__("strerror"),
                            self.__getattr__("filename") )).__str__();
        } else if (self.__getattr__("errno").__nonzero__()
                && self.__getattr__("strerror").__nonzero__()) {
            return Py.newString("[Errno %s] %s").__mod__(
                    new PyTuple( self.__getattr__("errno"),
                            self.__getattr__("strerror") )).__str__();
        } else {
            return Exception__str__(arg, kws);
        }
    }

    public static PyObject SystemExit(PyObject[] arg, String[] kws) {
        PyObject dict = empty__init__(arg, kws);
        dict.__setitem__("__init__", getJavaFunc("SystemExit__init__"));
        return dict;
    }

    public static void SystemExit__init__(PyObject[] arg, String[] kws) {
        ArgParser ap = new ArgParser("__init__", arg, kws, "self", "args");
        PyObject self = ap.getPyObject(0);
        PyObject args = ap.getList(1);

        self.__setattr__("args", args);
        if (args.__len__() == 0) {
            self.__setattr__("code", Py.None);
        } else if (args.__len__() == 1) {
            self.__setattr__("code", args.__getitem__(0));
        } else {
            self.__setattr__("code", args);
        }
    }

    public static PyObject UnicodeError(PyObject[] arg, String[] kws) {
        PyObject dict = empty__init__(arg, kws);
        dict.__setitem__("__init__", getJavaFunc("UnicodeError__init__"));
        return dict;
    }
    
    public static void UnicodeError__init__(PyObject[] arg, String[] kws, PyObject objectType) {
        ArgParser ap = new ArgParser("__init__", arg, kws, "self", "args");
        PyObject self = ap.getPyObject(0);
        PyObject args = ap.getList(1);
        self.__setattr__("args", args);
        if(args.__len__() != 5){
            throw Py.TypeError("");
        }
        if(!isPyString(args.__getitem__(0))
                || !Py.isInstance(args.__getitem__(1), objectType)||
                !isPyInt(args.__getitem__(2)) ||
                         !isPyInt(args.__getitem__(3))
                         || !isPyString(args.__getitem__(4))){
            throw Py.TypeError("");
        }
        self.__setattr__("encoding", args.__getitem__(0));
        self.__setattr__("object", args.__getitem__(1));
        self.__setattr__("start", args.__getitem__(2));
        self.__setattr__("end", args.__getitem__(3));
        self.__setattr__("reason", args.__getitem__(4));
    }

    private static boolean isPyInt(PyObject object) {
        return Py.isInstance(object, PyType.fromClass(PyInteger.class));
     }

    private static boolean isPyString(PyObject item) {
        return Py.isInstance(item, PyType.fromClass(PyString.class));
    }
    
    public static void UnicodeDecodeError__init__(PyObject[] arg, String[] kws) {
        UnicodeError__init__(arg, kws, PyType.fromClass(PyString.class));
    }

    public static PyString UnicodeDecodeError__str__(PyObject[] arg, String[] kws) {
        ArgParser ap = new ArgParser("__str__", arg, kws, "self");
        PyObject self = ap.getPyObject(0);
        int start = ((PyInteger)self.__getattr__("start")).getValue();
        int end = ((PyInteger)self.__getattr__("end")).getValue();

        if(end == (start + 1)) {
            PyInteger badByte = new PyInteger((self.__getattr__("object")
                    .toString().charAt(start)) & 0xff);
            return Py.newString("'%.400s' codec can't decode byte 0x%02x in position %d: %.400s")
                    .__mod__(new PyTuple(self.__getattr__("encoding"),
                                         badByte,
                                         self.__getattr__("start"),
                                         self.__getattr__("reason")))
                    .__str__();
        } else {
            return Py.newString("'%.400s' codec can't decode bytes in position %d-%d: %.400s")
                    .__mod__(new PyTuple(self.__getattr__("encoding"),
                                         self.__getattr__("start"),
                                         new PyInteger(end - 1),
                                         self.__getattr__("reason")))
                    .__str__();
        } 
    }

    public static PyObject UnicodeDecodeError(PyObject[] arg, String[] kws) {
        PyObject dict = empty__init__(arg, kws);
        dict.__setitem__("__init__", getJavaFunc("UnicodeDecodeError__init__"));
        dict.__setitem__("__str__", getJavaFunc("UnicodeDecodeError__str__"));
        return dict;
    }
    
    public static void UnicodeEncodeError__init__(PyObject[] arg, String[] kws) {
        UnicodeError__init__(arg, kws, PyType.fromClass(PyBaseString.class));
    }

    public static PyString UnicodeEncodeError__str__(PyObject[] arg, String[] kws) {
        ArgParser ap = new ArgParser("__str__", arg, kws, "self");
        PyObject self = ap.getPyObject(0);
        int start = ((PyInteger)self.__getattr__("start")).getValue();
        int end = ((PyInteger)self.__getattr__("end")).getValue();

        if(end == (start + 1)) {
            int badchar = self.__getattr__("object").toString().charAt(start);
            String format;
            if(badchar <= 0xff)
                format = "'%.400s' codec can't encode character u'\\x%02x' in position %d: %.400s";
            else if(badchar <= 0xffff)
                format = "'%.400s' codec can't encode character u'\\u%04x' in position %d: %.400s";
            else
                format = "'%.400s' codec can't encode character u'\\U%08x' in position %d: %.400s";
            return Py.newString(format)
                    .__mod__(new PyTuple(self.__getattr__("encoding"),
                                                         new PyInteger(badchar),
                                                         self.__getattr__("start"),
                                                         self.__getattr__("reason")))
                    .__str__();
        } else {
            return Py.newString("'%.400s' codec can't encode characters in position %d-%d: %.400s")
                    .__mod__(new PyTuple(self.__getattr__("encoding"),
                                                         self.__getattr__("start"),
                                                         new PyInteger(end - 1),
                                                         self.__getattr__("reason")))
                    .__str__();
        } 
    }

    public static PyObject UnicodeEncodeError(PyObject[] arg, String[] kws) {
        PyObject dict = empty__init__(arg, kws);
        dict.__setitem__("__init__", getJavaFunc("UnicodeEncodeError__init__"));
        dict.__setitem__("__str__", getJavaFunc("UnicodeEncodeError__str__"));
        return dict;
    }
    
    public static void UnicodeTranslateError__init__(PyObject[] arg, String[] kws) {
        ArgParser ap = new ArgParser("__init__", arg, kws, "self", "args");
        PyObject self = ap.getPyObject(0);
        PyObject args = ap.getList(1);
        if(args.__len__() != 4){
            throw Py.TypeError("");
        }
        if(!Py.isInstance(args.__getitem__(0), PyType.fromClass(PyBaseString.class))||
                !isPyInt(args.__getitem__(1)) ||
                         !isPyInt(args.__getitem__(2))
                         || !isPyString(args.__getitem__(3))){
            throw Py.TypeError("");
        }
        self.__setattr__("args", args);
        self.__setattr__("object", args.__getitem__(0));
        self.__setattr__("start", args.__getitem__(1));
        self.__setattr__("end", args.__getitem__(2));
        self.__setattr__("reason", args.__getitem__(3));
    }

    public static PyString UnicodeTranslateError__str__(PyObject[] arg, String[] kws) {
        ArgParser ap = new ArgParser("__str__", arg, kws, "self");
        PyObject self = ap.getPyObject(0);
        int start = ((PyInteger)self.__getattr__("start")).getValue();
        int end = ((PyInteger)self.__getattr__("end")).getValue();

        if(end == (start + 1)) {
            int badchar = (self.__getattr__("object").toString().charAt(start));
            String format;
            if(badchar <= 0xff)
                format = "can't translate character u'\\x%02x' in position %d: %.400s";
            else if(badchar <= 0xffff)
                format = "can't translate character u'\\u%04x' in position %d: %.400s";
            else
                format = "can't translate character u'\\U%08x' in position %d: %.400s";
            return Py.newString(format)
                    .__mod__(new PyTuple(new PyInteger(badchar),
                                                            self.__getattr__("start"),
                                                            self.__getattr__("reason")))
                    .__str__();
        } else {
            return Py.newString("can't translate characters in position %d-%d: %.400s")
                    .__mod__(new PyTuple(self.__getattr__("start"),
                                                         new PyInteger(end - 1),
                                                         self.__getattr__("reason")))
                    .__str__();
        } 
    }

    public static PyObject UnicodeTranslateError(PyObject[] arg, String[] kws) {
        PyObject dict = empty__init__(arg, kws);
        dict.__setitem__("__init__", getJavaFunc("UnicodeTranslateError__init__"));
        dict.__setitem__("__str__", getJavaFunc("UnicodeTranslateError__str__"));
        return dict;
    }

    private static PyObject getJavaFunc(String name) {
        return Py.newJavaFunc(exceptions.class, name);
    }

    private static PyObject buildClass(PyObject dict, String classname,
            String superclass, String classCodeName, String doc) {
        PyObject[] sclass = Py.EmptyObjects;
        if (superclass != null) {
            sclass = new PyObject[] { dict
                    .__getitem__(new PyString(superclass)) };
        }
        PyObject cls = Py.makeClass(classname, sclass, Py.newJavaCode(
                exceptions.class, classCodeName), new PyString(doc));
        dict.__setitem__(classname, cls);
        return cls;
    }
}
