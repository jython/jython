package org.python.core;

import org.python.core.*;

public class exceptions extends java.lang.Object implements InitModule {
    static String[] jpy$properties = new String[] {"python.modules.builtin", "exceptions:org.python.core.exceptions", "python.options.showJavaExceptions", "true", "python.packages.paths", "", "python.packages.directories", ""};
    static String[] jpy$packages = new String[] {};
    
    public static class _PyInner extends PyFunctionTable implements PyRunnable {
        private static PyObject s$0;
        private static PyObject s$1;
        private static PyObject s$2;
        private static PyObject s$3;
        private static PyObject i$4;
        private static PyObject i$5;
        private static PyObject s$6;
        private static PyObject s$7;
        private static PyObject i$8;
        private static PyObject s$9;
        private static PyObject s$10;
        private static PyObject s$11;
        private static PyObject i$12;
        private static PyObject s$13;
        private static PyObject s$14;
        private static PyObject s$15;
        private static PyObject s$16;
        private static PyObject s$17;
        private static PyObject s$18;
        private static PyObject s$19;
        private static PyObject s$20;
        private static PyObject s$21;
        private static PyObject s$22;
        private static PyObject s$23;
        private static PyObject s$24;
        private static PyObject s$25;
        private static PyObject s$26;
        private static PyObject s$27;
        private static PyObject s$28;
        private static PyObject s$29;
        private static PyObject s$30;
        private static PyObject s$31;
        private static PyObject s$32;
        private static PyObject s$33;
        private static PyObject s$34;
        private static PyObject s$35;
        private static PyObject s$36;
        private static PyObject s$37;
        private static PyObject s$38;
        private static PyFunctionTable funcTable;
        private static PyCode c$0___init__;
        private static PyCode c$1___str__;
        private static PyCode c$2___getitem__;
        private static PyCode c$3_Exception;
        private static PyCode c$4_StandardError;
        private static PyCode c$5___init__;
        private static PyCode c$6___str__;
        private static PyCode c$7_SyntaxError;
        private static PyCode c$8_IndentationError;
        private static PyCode c$9_TabError;
        private static PyCode c$10___init__;
        private static PyCode c$11___str__;
        private static PyCode c$12_EnvironmentError;
        private static PyCode c$13_IOError;
        private static PyCode c$14_OSError;
        private static PyCode c$15_RuntimeError;
        private static PyCode c$16_NotImplementedError;
        private static PyCode c$17_SystemError;
        private static PyCode c$18_EOFError;
        private static PyCode c$19_ImportError;
        private static PyCode c$20_TypeError;
        private static PyCode c$21_ValueError;
        private static PyCode c$22_UnicodeError;
        private static PyCode c$23_KeyboardInterrupt;
        private static PyCode c$24_AssertionError;
        private static PyCode c$25_ArithmeticError;
        private static PyCode c$26_OverflowError;
        private static PyCode c$27_FloatingPointError;
        private static PyCode c$28_ZeroDivisionError;
        private static PyCode c$29_LookupError;
        private static PyCode c$30_IndexError;
        private static PyCode c$31_KeyError;
        private static PyCode c$32_AttributeError;
        private static PyCode c$33_NameError;
        private static PyCode c$34_UnboundLocalError;
        private static PyCode c$35_MemoryError;
        private static PyCode c$36___init__;
        private static PyCode c$37_SystemExit;
        private static PyCode c$38_main;
        private static void initConstants() {
            s$0 = Py.newString("I:\\java\\jpython.CVS\\dist\\Lib\\exceptions.py");
            s$1 = Py.newString("Class based built-in exception hierarchy.\012\012New with Python 1.5, all standard built-in exceptions are now class objects by\012default.  This gives Python's exception handling mechanism a more\012object-oriented feel.  Traditionally they were string objects.  Python will\012fallback to string based exceptions if the interpreter is invoked with the -X\012option, or if some failure occurs during class exception initialization (in\012this case a warning will be printed).\012\012Most existing code should continue to work with class based exceptions.  Some\012tricky uses of IOError may break, but the most common uses should work.\012\012Here is a rundown of the class hierarchy.  You can change this by editing this\012file, but it isn't recommended because the old string based exceptions won't\012be kept in sync.  The class names described here are expected to be found by\012the bltinmodule.c file.  If you add classes here, you must modify\012bltinmodule.c or the exceptions won't be available in the __builtin__ module,\012nor will they be accessible from C.\012\012The classes with a `*' are new since Python 1.5.  They are defined as tuples\012containing the derived exceptions when string-based exceptions are used.  If\012you define your own class based exceptions, they should be derived from\012Exception.\012\012Exception(*)\012 |\012 +-- SystemExit\012 +-- StandardError(*)\012      |\012      +-- KeyboardInterrupt\012      +-- ImportError\012      +-- EnvironmentError(*)\012      |    |\012      |    +-- IOError\012      |    +-- OSError(*)\012      |\012      +-- EOFError\012      +-- RuntimeError\012      |    |\012      |    +-- NotImplementedError(*)\012      |\012      +-- NameError\012      |    |\012      |    +-- UnboundLocalError(*)\012      |\012      +-- AttributeError\012      +-- SyntaxError\012      |    |\012      |    +-- IndentationError\012      |         |\012      |         +-- TabError\012      |\012      +-- TypeError\012      +-- AssertionError\012      +-- LookupError(*)\012      |    |\012      |    +-- IndexError\012      |    +-- KeyError\012      |\012      +-- ArithmeticError(*)\012      |    |\012      |    +-- OverflowError\012      |    +-- ZeroDivisionError\012      |    +-- FloatingPointError\012      |\012      +-- ValueError\012      |    |\012      |    +-- UnicodeError\012      |\012      +-- SystemError\012      +-- MemoryError\012");
            s$2 = Py.newString("Proposed base class for all exceptions.");
            s$3 = Py.newString("");
            i$4 = Py.newInteger(1);
            i$5 = Py.newInteger(0);
            s$6 = Py.newString("Base class for all standard Python exceptions.");
            s$7 = Py.newString("Invalid syntax.");
            i$8 = Py.newInteger(2);
            s$9 = Py.newString("Improper indentation");
            s$10 = Py.newString("Improper mixture of spaces and tabs.");
            s$11 = Py.newString("Base class for I/O related errors.");
            i$12 = Py.newInteger(3);
            s$13 = Py.newString("[Errno %s] %s: %s");
            s$14 = Py.newString("[Errno %s] %s");
            s$15 = Py.newString("I/O operation failed.");
            s$16 = Py.newString("OS system call failed.");
            s$17 = Py.newString("Unspecified run-time error.");
            s$18 = Py.newString("Method or function hasn't been implemented yet.");
            s$19 = Py.newString("Internal error in the Python interpreter.\012\012    Please report this to the Python maintainer, along with the traceback,\012    the Python version, and the hardware/OS platform and version.");
            s$20 = Py.newString("Read beyond end of file.");
            s$21 = Py.newString("Import can't find module, or can't find name in module.");
            s$22 = Py.newString("Inappropriate argument type.");
            s$23 = Py.newString("Inappropriate argument value (of correct type).");
            s$24 = Py.newString("Unicode related error.");
            s$25 = Py.newString("Program interrupted by user.");
            s$26 = Py.newString("Assertion failed.");
            s$27 = Py.newString("Base class for arithmetic errors.");
            s$28 = Py.newString("Result too large to be represented.");
            s$29 = Py.newString("Floating point operation failed.");
            s$30 = Py.newString("Second argument to a division or modulo operation was zero.");
            s$31 = Py.newString("Base class for lookup errors.");
            s$32 = Py.newString("Sequence index out of range.");
            s$33 = Py.newString("Mapping key not found.");
            s$34 = Py.newString("Attribute not found.");
            s$35 = Py.newString("Name not found globally.");
            s$36 = Py.newString("Local name referenced but not bound to a value.");
            s$37 = Py.newString("Out of memory.");
            s$38 = Py.newString("Request to exit from the interpreter.");
            funcTable = new _PyInner();
            c$0___init__ = Py.newCode(2, new String[] {"self", "args"}, "I:\\java\\jpython.CVS\\dist\\Lib\\exceptions.py", "__init__", true, false, funcTable, 0);
            c$1___str__ = Py.newCode(1, new String[] {"self"}, "I:\\java\\jpython.CVS\\dist\\Lib\\exceptions.py", "__str__", false, false, funcTable, 1);
            c$2___getitem__ = Py.newCode(2, new String[] {"self", "i"}, "I:\\java\\jpython.CVS\\dist\\Lib\\exceptions.py", "__getitem__", false, false, funcTable, 2);
            c$3_Exception = Py.newCode(0, new String[] {"__init__", "__str__", "__getitem__"}, "I:\\java\\jpython.CVS\\dist\\Lib\\exceptions.py", "Exception", false, false, funcTable, 3);
            c$4_StandardError = Py.newCode(0, new String[] {}, "I:\\java\\jpython.CVS\\dist\\Lib\\exceptions.py", "StandardError", false, false, funcTable, 4);
            c$5___init__ = Py.newCode(2, new String[] {"self", "args", "info"}, "I:\\java\\jpython.CVS\\dist\\Lib\\exceptions.py", "__init__", true, false, funcTable, 5);
            c$6___str__ = Py.newCode(1, new String[] {"self"}, "I:\\java\\jpython.CVS\\dist\\Lib\\exceptions.py", "__str__", false, false, funcTable, 6);
            c$7_SyntaxError = Py.newCode(0, new String[] {"filename", "lineno", "offset", "text", "msg", "__init__", "__str__"}, "I:\\java\\jpython.CVS\\dist\\Lib\\exceptions.py", "SyntaxError", false, false, funcTable, 7);
            c$8_IndentationError = Py.newCode(0, new String[] {}, "I:\\java\\jpython.CVS\\dist\\Lib\\exceptions.py", "IndentationError", false, false, funcTable, 8);
            c$9_TabError = Py.newCode(0, new String[] {}, "I:\\java\\jpython.CVS\\dist\\Lib\\exceptions.py", "TabError", false, false, funcTable, 9);
            c$10___init__ = Py.newCode(2, new String[] {"self", "args"}, "I:\\java\\jpython.CVS\\dist\\Lib\\exceptions.py", "__init__", true, false, funcTable, 10);
            c$11___str__ = Py.newCode(1, new String[] {"self"}, "I:\\java\\jpython.CVS\\dist\\Lib\\exceptions.py", "__str__", false, false, funcTable, 11);
            c$12_EnvironmentError = Py.newCode(0, new String[] {"__init__", "__str__"}, "I:\\java\\jpython.CVS\\dist\\Lib\\exceptions.py", "EnvironmentError", false, false, funcTable, 12);
            c$13_IOError = Py.newCode(0, new String[] {}, "I:\\java\\jpython.CVS\\dist\\Lib\\exceptions.py", "IOError", false, false, funcTable, 13);
            c$14_OSError = Py.newCode(0, new String[] {}, "I:\\java\\jpython.CVS\\dist\\Lib\\exceptions.py", "OSError", false, false, funcTable, 14);
            c$15_RuntimeError = Py.newCode(0, new String[] {}, "I:\\java\\jpython.CVS\\dist\\Lib\\exceptions.py", "RuntimeError", false, false, funcTable, 15);
            c$16_NotImplementedError = Py.newCode(0, new String[] {}, "I:\\java\\jpython.CVS\\dist\\Lib\\exceptions.py", "NotImplementedError", false, false, funcTable, 16);
            c$17_SystemError = Py.newCode(0, new String[] {}, "I:\\java\\jpython.CVS\\dist\\Lib\\exceptions.py", "SystemError", false, false, funcTable, 17);
            c$18_EOFError = Py.newCode(0, new String[] {}, "I:\\java\\jpython.CVS\\dist\\Lib\\exceptions.py", "EOFError", false, false, funcTable, 18);
            c$19_ImportError = Py.newCode(0, new String[] {}, "I:\\java\\jpython.CVS\\dist\\Lib\\exceptions.py", "ImportError", false, false, funcTable, 19);
            c$20_TypeError = Py.newCode(0, new String[] {}, "I:\\java\\jpython.CVS\\dist\\Lib\\exceptions.py", "TypeError", false, false, funcTable, 20);
            c$21_ValueError = Py.newCode(0, new String[] {}, "I:\\java\\jpython.CVS\\dist\\Lib\\exceptions.py", "ValueError", false, false, funcTable, 21);
            c$22_UnicodeError = Py.newCode(0, new String[] {}, "I:\\java\\jpython.CVS\\dist\\Lib\\exceptions.py", "UnicodeError", false, false, funcTable, 22);
            c$23_KeyboardInterrupt = Py.newCode(0, new String[] {}, "I:\\java\\jpython.CVS\\dist\\Lib\\exceptions.py", "KeyboardInterrupt", false, false, funcTable, 23);
            c$24_AssertionError = Py.newCode(0, new String[] {}, "I:\\java\\jpython.CVS\\dist\\Lib\\exceptions.py", "AssertionError", false, false, funcTable, 24);
            c$25_ArithmeticError = Py.newCode(0, new String[] {}, "I:\\java\\jpython.CVS\\dist\\Lib\\exceptions.py", "ArithmeticError", false, false, funcTable, 25);
            c$26_OverflowError = Py.newCode(0, new String[] {}, "I:\\java\\jpython.CVS\\dist\\Lib\\exceptions.py", "OverflowError", false, false, funcTable, 26);
            c$27_FloatingPointError = Py.newCode(0, new String[] {}, "I:\\java\\jpython.CVS\\dist\\Lib\\exceptions.py", "FloatingPointError", false, false, funcTable, 27);
            c$28_ZeroDivisionError = Py.newCode(0, new String[] {}, "I:\\java\\jpython.CVS\\dist\\Lib\\exceptions.py", "ZeroDivisionError", false, false, funcTable, 28);
            c$29_LookupError = Py.newCode(0, new String[] {}, "I:\\java\\jpython.CVS\\dist\\Lib\\exceptions.py", "LookupError", false, false, funcTable, 29);
            c$30_IndexError = Py.newCode(0, new String[] {}, "I:\\java\\jpython.CVS\\dist\\Lib\\exceptions.py", "IndexError", false, false, funcTable, 30);
            c$31_KeyError = Py.newCode(0, new String[] {}, "I:\\java\\jpython.CVS\\dist\\Lib\\exceptions.py", "KeyError", false, false, funcTable, 31);
            c$32_AttributeError = Py.newCode(0, new String[] {}, "I:\\java\\jpython.CVS\\dist\\Lib\\exceptions.py", "AttributeError", false, false, funcTable, 32);
            c$33_NameError = Py.newCode(0, new String[] {}, "I:\\java\\jpython.CVS\\dist\\Lib\\exceptions.py", "NameError", false, false, funcTable, 33);
            c$34_UnboundLocalError = Py.newCode(0, new String[] {}, "I:\\java\\jpython.CVS\\dist\\Lib\\exceptions.py", "UnboundLocalError", false, false, funcTable, 34);
            c$35_MemoryError = Py.newCode(0, new String[] {}, "I:\\java\\jpython.CVS\\dist\\Lib\\exceptions.py", "MemoryError", false, false, funcTable, 35);
            c$36___init__ = Py.newCode(2, new String[] {"self", "args"}, "I:\\java\\jpython.CVS\\dist\\Lib\\exceptions.py", "__init__", true, false, funcTable, 36);
            c$37_SystemExit = Py.newCode(0, new String[] {"__init__"}, "I:\\java\\jpython.CVS\\dist\\Lib\\exceptions.py", "SystemExit", false, false, funcTable, 37);
            c$38_main = Py.newCode(0, new String[] {}, "I:\\java\\jpython.CVS\\dist\\Lib\\exceptions.py", "main", false, false, funcTable, 38);
        }
        
        
        public PyCode getMain() {
            if (c$38_main == null) _PyInner.initConstants();
            return c$38_main;
        }
        
        public PyObject call_function(int index, PyFrame frame) {
            switch (index){
                case 0:
                return _PyInner.__init__$1(frame);
                case 1:
                return _PyInner.__str__$2(frame);
                case 2:
                return _PyInner.__getitem__$3(frame);
                case 3:
                return _PyInner.Exception$4(frame);
                case 4:
                return _PyInner.StandardError$5(frame);
                case 5:
                return _PyInner.__init__$6(frame);
                case 6:
                return _PyInner.__str__$7(frame);
                case 7:
                return _PyInner.SyntaxError$8(frame);
                case 8:
                return _PyInner.IndentationError$9(frame);
                case 9:
                return _PyInner.TabError$10(frame);
                case 10:
                return _PyInner.__init__$11(frame);
                case 11:
                return _PyInner.__str__$12(frame);
                case 12:
                return _PyInner.EnvironmentError$13(frame);
                case 13:
                return _PyInner.IOError$14(frame);
                case 14:
                return _PyInner.OSError$15(frame);
                case 15:
                return _PyInner.RuntimeError$16(frame);
                case 16:
                return _PyInner.NotImplementedError$17(frame);
                case 17:
                return _PyInner.SystemError$18(frame);
                case 18:
                return _PyInner.EOFError$19(frame);
                case 19:
                return _PyInner.ImportError$20(frame);
                case 20:
                return _PyInner.TypeError$21(frame);
                case 21:
                return _PyInner.ValueError$22(frame);
                case 22:
                return _PyInner.UnicodeError$23(frame);
                case 23:
                return _PyInner.KeyboardInterrupt$24(frame);
                case 24:
                return _PyInner.AssertionError$25(frame);
                case 25:
                return _PyInner.ArithmeticError$26(frame);
                case 26:
                return _PyInner.OverflowError$27(frame);
                case 27:
                return _PyInner.FloatingPointError$28(frame);
                case 28:
                return _PyInner.ZeroDivisionError$29(frame);
                case 29:
                return _PyInner.LookupError$30(frame);
                case 30:
                return _PyInner.IndexError$31(frame);
                case 31:
                return _PyInner.KeyError$32(frame);
                case 32:
                return _PyInner.AttributeError$33(frame);
                case 33:
                return _PyInner.NameError$34(frame);
                case 34:
                return _PyInner.UnboundLocalError$35(frame);
                case 35:
                return _PyInner.MemoryError$36(frame);
                case 36:
                return _PyInner.__init__$37(frame);
                case 37:
                return _PyInner.SystemExit$38(frame);
                case 38:
                return _PyInner.main$39(frame);
                default:
                return null;
            }
        }
        
        private static PyObject __init__$1(PyFrame frame) {
            frame.getlocal(0).__setattr__("args", frame.getlocal(1));
            return Py.None;
        }
        
        private static PyObject __str__$2(PyFrame frame) {
            if (frame.getlocal(0).__getattr__("args").__not__().__nonzero__()) {
                return s$3;
            }
            else if (frame.getglobal("len").__call__(frame.getlocal(0).__getattr__("args"))._eq(i$4).__nonzero__()) {
                return frame.getglobal("str").__call__(frame.getlocal(0).__getattr__("args").__getitem__(i$5));
            }
            else {
                return frame.getglobal("str").__call__(frame.getlocal(0).__getattr__("args"));
            }
        }
        
        private static PyObject __getitem__$3(PyFrame frame) {
            return frame.getlocal(0).__getattr__("args").__getitem__(frame.getlocal(1));
        }
        
        private static PyObject Exception$4(PyFrame frame) {
            /* Proposed base class for all exceptions. */
            frame.setlocal("__init__", new PyFunction(frame.f_globals, new PyObject[] {}, c$0___init__));
            frame.setlocal("__str__", new PyFunction(frame.f_globals, new PyObject[] {}, c$1___str__));
            frame.setlocal("__getitem__", new PyFunction(frame.f_globals, new PyObject[] {}, c$2___getitem__));
            return frame.getf_locals();
        }
        
        private static PyObject StandardError$5(PyFrame frame) {
            /* Base class for all standard Python exceptions. */
            // pass
            return frame.getf_locals();
        }
        
        private static PyObject __init__$6(PyFrame frame) {
            // Temporary Variables
            PyException t$0$PyException;
            PyObject t$0$PyObject;
            
            // Code
            frame.getlocal(0).__setattr__("args", frame.getlocal(1));
            if (frame.getglobal("len").__call__(frame.getlocal(0).__getattr__("args"))._ge(i$4).__nonzero__()) {
                frame.getlocal(0).__setattr__("msg", frame.getlocal(0).__getattr__("args").__getitem__(i$5));
            }
            if (frame.getglobal("len").__call__(frame.getlocal(0).__getattr__("args"))._eq(i$8).__nonzero__()) {
                frame.setlocal(2, frame.getlocal(0).__getattr__("args").__getitem__(i$4));
                try {
                    t$0$PyObject = frame.getlocal(2);
                    frame.getlocal(0).__setattr__("filename", t$0$PyObject.__getitem__(0));
                    frame.getlocal(0).__setattr__("lineno", t$0$PyObject.__getitem__(1));
                    frame.getlocal(0).__setattr__("offset", t$0$PyObject.__getitem__(2));
                    frame.getlocal(0).__setattr__("text", t$0$PyObject.__getitem__(3));
                }
                catch (Throwable x$0) {
                    t$0$PyException = Py.setException(x$0, frame);
                    // pass
                }
            }
            return Py.None;
        }
        
        private static PyObject __str__$7(PyFrame frame) {
            return frame.getglobal("str").__call__(frame.getlocal(0).__getattr__("msg"));
        }
        
        private static PyObject SyntaxError$8(PyFrame frame) {
            // Temporary Variables
            PyObject t$0$PyObject;
            
            // Code
            /* Invalid syntax. */
            t$0$PyObject = frame.getglobal("None");
            frame.setlocal("filename", t$0$PyObject);
            frame.setlocal("lineno", t$0$PyObject);
            frame.setlocal("offset", t$0$PyObject);
            frame.setlocal("text", t$0$PyObject);
            frame.setlocal("msg", s$3);
            frame.setlocal("__init__", new PyFunction(frame.f_globals, new PyObject[] {}, c$5___init__));
            frame.setlocal("__str__", new PyFunction(frame.f_globals, new PyObject[] {}, c$6___str__));
            return frame.getf_locals();
        }
        
        private static PyObject IndentationError$9(PyFrame frame) {
            /* Improper indentation */
            // pass
            return frame.getf_locals();
        }
        
        private static PyObject TabError$10(PyFrame frame) {
            /* Improper mixture of spaces and tabs. */
            // pass
            return frame.getf_locals();
        }
        
        private static PyObject __init__$11(PyFrame frame) {
            // Temporary Variables
            PyObject t$0$PyObject;
            
            // Code
            frame.getlocal(0).__setattr__("args", frame.getlocal(1));
            frame.getlocal(0).__setattr__("errno", frame.getglobal("None"));
            frame.getlocal(0).__setattr__("strerror", frame.getglobal("None"));
            frame.getlocal(0).__setattr__("filename", frame.getglobal("None"));
            if (frame.getglobal("len").__call__(frame.getlocal(1))._eq(i$12).__nonzero__()) {
                t$0$PyObject = frame.getlocal(1);
                frame.getlocal(0).__setattr__("errno", t$0$PyObject.__getitem__(0));
                frame.getlocal(0).__setattr__("strerror", t$0$PyObject.__getitem__(1));
                frame.getlocal(0).__setattr__("filename", t$0$PyObject.__getitem__(2));
                frame.getlocal(0).__setattr__("args", frame.getlocal(1).__getslice__(i$5, i$8, null));
            }
            if (frame.getglobal("len").__call__(frame.getlocal(1))._eq(i$8).__nonzero__()) {
                t$0$PyObject = frame.getlocal(1);
                frame.getlocal(0).__setattr__("errno", t$0$PyObject.__getitem__(0));
                frame.getlocal(0).__setattr__("strerror", t$0$PyObject.__getitem__(1));
            }
            return Py.None;
        }
        
        private static PyObject __str__$12(PyFrame frame) {
            // Temporary Variables
            PyObject t$0$PyObject;
            
            // Code
            if (frame.getlocal(0).__getattr__("filename")._isnot(frame.getglobal("None")).__nonzero__()) {
                return s$13._mod(new PyTuple(new PyObject[] {frame.getlocal(0).__getattr__("errno"), frame.getlocal(0).__getattr__("strerror"), frame.getglobal("repr").__call__(frame.getlocal(0).__getattr__("filename"))}));
            }
            else if (((t$0$PyObject = frame.getlocal(0).__getattr__("errno")).__nonzero__() ? frame.getlocal(0).__getattr__("strerror") : t$0$PyObject).__nonzero__()) {
                return s$14._mod(new PyTuple(new PyObject[] {frame.getlocal(0).__getattr__("errno"), frame.getlocal(0).__getattr__("strerror")}));
            }
            else {
                return frame.getglobal("StandardError").invoke("__str__", frame.getlocal(0));
            }
        }
        
        private static PyObject EnvironmentError$13(PyFrame frame) {
            /* Base class for I/O related errors. */
            frame.setlocal("__init__", new PyFunction(frame.f_globals, new PyObject[] {}, c$10___init__));
            frame.setlocal("__str__", new PyFunction(frame.f_globals, new PyObject[] {}, c$11___str__));
            return frame.getf_locals();
        }
        
        private static PyObject IOError$14(PyFrame frame) {
            /* I/O operation failed. */
            // pass
            return frame.getf_locals();
        }
        
        private static PyObject OSError$15(PyFrame frame) {
            /* OS system call failed. */
            // pass
            return frame.getf_locals();
        }
        
        private static PyObject RuntimeError$16(PyFrame frame) {
            /* Unspecified run-time error. */
            // pass
            return frame.getf_locals();
        }
        
        private static PyObject NotImplementedError$17(PyFrame frame) {
            /* Method or function hasn't been implemented yet. */
            // pass
            return frame.getf_locals();
        }
        
        private static PyObject SystemError$18(PyFrame frame) {
            /* Internal error in the Python interpreter.
            
                Please report this to the Python maintainer, along with the traceback,
                the Python version, and the hardware/OS platform and version. */
            // pass
            return frame.getf_locals();
        }
        
        private static PyObject EOFError$19(PyFrame frame) {
            /* Read beyond end of file. */
            // pass
            return frame.getf_locals();
        }
        
        private static PyObject ImportError$20(PyFrame frame) {
            /* Import can't find module, or can't find name in module. */
            // pass
            return frame.getf_locals();
        }
        
        private static PyObject TypeError$21(PyFrame frame) {
            /* Inappropriate argument type. */
            // pass
            return frame.getf_locals();
        }
        
        private static PyObject ValueError$22(PyFrame frame) {
            /* Inappropriate argument value (of correct type). */
            // pass
            return frame.getf_locals();
        }
        
        private static PyObject UnicodeError$23(PyFrame frame) {
            /* Unicode related error. */
            // pass
            return frame.getf_locals();
        }
        
        private static PyObject KeyboardInterrupt$24(PyFrame frame) {
            /* Program interrupted by user. */
            // pass
            return frame.getf_locals();
        }
        
        private static PyObject AssertionError$25(PyFrame frame) {
            /* Assertion failed. */
            // pass
            return frame.getf_locals();
        }
        
        private static PyObject ArithmeticError$26(PyFrame frame) {
            /* Base class for arithmetic errors. */
            // pass
            return frame.getf_locals();
        }
        
        private static PyObject OverflowError$27(PyFrame frame) {
            /* Result too large to be represented. */
            // pass
            return frame.getf_locals();
        }
        
        private static PyObject FloatingPointError$28(PyFrame frame) {
            /* Floating point operation failed. */
            // pass
            return frame.getf_locals();
        }
        
        private static PyObject ZeroDivisionError$29(PyFrame frame) {
            /* Second argument to a division or modulo operation was zero. */
            // pass
            return frame.getf_locals();
        }
        
        private static PyObject LookupError$30(PyFrame frame) {
            /* Base class for lookup errors. */
            // pass
            return frame.getf_locals();
        }
        
        private static PyObject IndexError$31(PyFrame frame) {
            /* Sequence index out of range. */
            // pass
            return frame.getf_locals();
        }
        
        private static PyObject KeyError$32(PyFrame frame) {
            /* Mapping key not found. */
            // pass
            return frame.getf_locals();
        }
        
        private static PyObject AttributeError$33(PyFrame frame) {
            /* Attribute not found. */
            // pass
            return frame.getf_locals();
        }
        
        private static PyObject NameError$34(PyFrame frame) {
            /* Name not found globally. */
            // pass
            return frame.getf_locals();
        }
        
        private static PyObject UnboundLocalError$35(PyFrame frame) {
            /* Local name referenced but not bound to a value. */
            // pass
            return frame.getf_locals();
        }
        
        private static PyObject MemoryError$36(PyFrame frame) {
            /* Out of memory. */
            // pass
            return frame.getf_locals();
        }
        
        private static PyObject __init__$37(PyFrame frame) {
            frame.getlocal(0).__setattr__("args", frame.getlocal(1));
            if (frame.getglobal("len").__call__(frame.getlocal(1))._eq(i$5).__nonzero__()) {
                frame.getlocal(0).__setattr__("code", frame.getglobal("None"));
            }
            else if (frame.getglobal("len").__call__(frame.getlocal(1))._eq(i$4).__nonzero__()) {
                frame.getlocal(0).__setattr__("code", frame.getlocal(1).__getitem__(i$5));
            }
            else {
                frame.getlocal(0).__setattr__("code", frame.getlocal(1));
            }
            return Py.None;
        }
        
        private static PyObject SystemExit$38(PyFrame frame) {
            /* Request to exit from the interpreter. */
            frame.setlocal("__init__", new PyFunction(frame.f_globals, new PyObject[] {}, c$36___init__));
            return frame.getf_locals();
        }
        
        private static PyObject main$39(PyFrame frame) {
            frame.setglobal("__file__", s$0);
            /* Class based built-in exception hierarchy.
            
            New with Python 1.5, all standard built-in exceptions are now class objects by
            default.  This gives Python's exception handling mechanism a more
            object-oriented feel.  Traditionally they were string objects.  Python will
            fallback to string based exceptions if the interpreter is invoked with the -X
            option, or if some failure occurs during class exception initialization (in
            this case a warning will be printed).
            
            Most existing code should continue to work with class based exceptions.  Some
            tricky uses of IOError may break, but the most common uses should work.
            
            Here is a rundown of the class hierarchy.  You can change this by editing this
            file, but it isn't recommended because the old string based exceptions won't
            be kept in sync.  The class names described here are expected to be found by
            the bltinmodule.c file.  If you add classes here, you must modify
            bltinmodule.c or the exceptions won't be available in the __builtin__ module,
            nor will they be accessible from C.
            
            The classes with a `*' are new since Python 1.5.  They are defined as tuples
            containing the derived exceptions when string-based exceptions are used.  If
            you define your own class based exceptions, they should be derived from
            Exception.
            
            Exception(*)
             |
             +-- SystemExit
             +-- StandardError(*)
                  |
                  +-- KeyboardInterrupt
                  +-- ImportError
                  +-- EnvironmentError(*)
                  |    |
                  |    +-- IOError
                  |    +-- OSError(*)
                  |
                  +-- EOFError
                  +-- RuntimeError
                  |    |
                  |    +-- NotImplementedError(*)
                  |
                  +-- NameError
                  |    |
                  |    +-- UnboundLocalError(*)
                  |
                  +-- AttributeError
                  +-- SyntaxError
                  |    |
                  |    +-- IndentationError
                  |         |
                  |         +-- TabError
                  |
                  +-- TypeError
                  +-- AssertionError
                  +-- LookupError(*)
                  |    |
                  |    +-- IndexError
                  |    +-- KeyError
                  |
                  +-- ArithmeticError(*)
                  |    |
                  |    +-- OverflowError
                  |    +-- ZeroDivisionError
                  |    +-- FloatingPointError
                  |
                  +-- ValueError
                  |    |
                  |    +-- UnicodeError
                  |
                  +-- SystemError
                  +-- MemoryError
             */
            frame.setglobal("Exception", Py.makeClass("Exception", new PyObject[] {}, c$3_Exception, null));
            frame.setglobal("StandardError", Py.makeClass("StandardError", new PyObject[] {frame.getglobal("Exception")}, c$4_StandardError, null));
            frame.setglobal("SyntaxError", Py.makeClass("SyntaxError", new PyObject[] {frame.getglobal("StandardError")}, c$7_SyntaxError, null));
            frame.setglobal("IndentationError", Py.makeClass("IndentationError", new PyObject[] {frame.getglobal("SyntaxError")}, c$8_IndentationError, null));
            frame.setglobal("TabError", Py.makeClass("TabError", new PyObject[] {frame.getglobal("IndentationError")}, c$9_TabError, null));
            frame.setglobal("EnvironmentError", Py.makeClass("EnvironmentError", new PyObject[] {frame.getglobal("StandardError")}, c$12_EnvironmentError, null));
            frame.setglobal("IOError", Py.makeClass("IOError", new PyObject[] {frame.getglobal("EnvironmentError")}, c$13_IOError, null));
            frame.setglobal("OSError", Py.makeClass("OSError", new PyObject[] {frame.getglobal("EnvironmentError")}, c$14_OSError, null));
            frame.setglobal("RuntimeError", Py.makeClass("RuntimeError", new PyObject[] {frame.getglobal("StandardError")}, c$15_RuntimeError, null));
            frame.setglobal("NotImplementedError", Py.makeClass("NotImplementedError", new PyObject[] {frame.getglobal("RuntimeError")}, c$16_NotImplementedError, null));
            frame.setglobal("SystemError", Py.makeClass("SystemError", new PyObject[] {frame.getglobal("StandardError")}, c$17_SystemError, null));
            frame.setglobal("EOFError", Py.makeClass("EOFError", new PyObject[] {frame.getglobal("StandardError")}, c$18_EOFError, null));
            frame.setglobal("ImportError", Py.makeClass("ImportError", new PyObject[] {frame.getglobal("StandardError")}, c$19_ImportError, null));
            frame.setglobal("TypeError", Py.makeClass("TypeError", new PyObject[] {frame.getglobal("StandardError")}, c$20_TypeError, null));
            frame.setglobal("ValueError", Py.makeClass("ValueError", new PyObject[] {frame.getglobal("StandardError")}, c$21_ValueError, null));
            frame.setglobal("UnicodeError", Py.makeClass("UnicodeError", new PyObject[] {frame.getglobal("ValueError")}, c$22_UnicodeError, null));
            frame.setglobal("KeyboardInterrupt", Py.makeClass("KeyboardInterrupt", new PyObject[] {frame.getglobal("StandardError")}, c$23_KeyboardInterrupt, null));
            frame.setglobal("AssertionError", Py.makeClass("AssertionError", new PyObject[] {frame.getglobal("StandardError")}, c$24_AssertionError, null));
            frame.setglobal("ArithmeticError", Py.makeClass("ArithmeticError", new PyObject[] {frame.getglobal("StandardError")}, c$25_ArithmeticError, null));
            frame.setglobal("OverflowError", Py.makeClass("OverflowError", new PyObject[] {frame.getglobal("ArithmeticError")}, c$26_OverflowError, null));
            frame.setglobal("FloatingPointError", Py.makeClass("FloatingPointError", new PyObject[] {frame.getglobal("ArithmeticError")}, c$27_FloatingPointError, null));
            frame.setglobal("ZeroDivisionError", Py.makeClass("ZeroDivisionError", new PyObject[] {frame.getglobal("ArithmeticError")}, c$28_ZeroDivisionError, null));
            frame.setglobal("LookupError", Py.makeClass("LookupError", new PyObject[] {frame.getglobal("StandardError")}, c$29_LookupError, null));
            frame.setglobal("IndexError", Py.makeClass("IndexError", new PyObject[] {frame.getglobal("LookupError")}, c$30_IndexError, null));
            frame.setglobal("KeyError", Py.makeClass("KeyError", new PyObject[] {frame.getglobal("LookupError")}, c$31_KeyError, null));
            frame.setglobal("AttributeError", Py.makeClass("AttributeError", new PyObject[] {frame.getglobal("StandardError")}, c$32_AttributeError, null));
            frame.setglobal("NameError", Py.makeClass("NameError", new PyObject[] {frame.getglobal("StandardError")}, c$33_NameError, null));
            frame.setglobal("UnboundLocalError", Py.makeClass("UnboundLocalError", new PyObject[] {frame.getglobal("NameError")}, c$34_UnboundLocalError, null));
            frame.setglobal("MemoryError", Py.makeClass("MemoryError", new PyObject[] {frame.getglobal("StandardError")}, c$35_MemoryError, null));
            frame.setglobal("SystemExit", Py.makeClass("SystemExit", new PyObject[] {frame.getglobal("Exception")}, c$37_SystemExit, null));
            return Py.None;
        }
        
    }
    public void initModule(PyObject dict) {
        dict.__setitem__("__name__", new PyString("exceptions"));
        Py.runCode(new _PyInner().getMain(), dict, dict);
    }
    
    public static void main(String[] args) {
        String[] newargs = new String[args.length+1];
        newargs[0] = "exceptions";
        System.arraycopy(args, 0, newargs, 1, args.length);
        Py.runMain("org.python.core.exceptions$_PyInner", newargs, jpy$packages, jpy$properties, null);
    }
    
}
