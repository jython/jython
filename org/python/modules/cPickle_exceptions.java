package org.python.modules;

import org.python.core.*;

public class cPickle_exceptions extends java.lang.Object implements InitModule {
    static String[] jpy$properties = new String[] {"python.modules.builtin", "exceptions", "python.options.showJavaExceptions", "true", "python.packages.paths", "", "python.packages.directories", ""};
    static String[] jpy$packages = new String[] {};
    
    public static class _PyInner extends PyFunctionTable implements PyRunnable {
        private static PyObject s$0;
        private static PyObject s$1;
        private static PyObject i$2;
        private static PyObject s$3;
        private static PyObject s$4;
        private static PyFunctionTable funcTable;
        private static PyCode c$0___init__;
        private static PyCode c$1___str__;
        private static PyCode c$2_PickleError;
        private static PyCode c$3_PicklingError;
        private static PyCode c$4___init__;
        private static PyCode c$5___str__;
        private static PyCode c$6_UnpickleableError;
        private static PyCode c$7_UnpicklingError;
        private static PyCode c$8_main;
        private static void initConstants() {
            s$0 = Py.newString("I:\\java\\JPython-1.1\\org\\python\\modules\\cPickle_exceptions.py");
            s$1 = Py.newString("%s");
            i$2 = Py.newInteger(0);
            s$3 = Py.newString("(what)");
            s$4 = Py.newString("Cannot pickle %s objects");
            funcTable = new _PyInner();
            c$0___init__ = Py.newCode(2, new String[] {"self", "args"}, "I:\\java\\JPython-1.1\\org\\python\\modules\\cPickle_exceptions.py", "__init__", true, false, funcTable, 0);
            c$1___str__ = Py.newCode(1, new String[] {"self"}, "I:\\java\\JPython-1.1\\org\\python\\modules\\cPickle_exceptions.py", "__str__", false, false, funcTable, 1);
            c$2_PickleError = Py.newCode(0, new String[] {"__init__", "__str__"}, "I:\\java\\JPython-1.1\\org\\python\\modules\\cPickle_exceptions.py", "PickleError", false, false, funcTable, 2);
            c$3_PicklingError = Py.newCode(0, new String[] {}, "I:\\java\\JPython-1.1\\org\\python\\modules\\cPickle_exceptions.py", "PicklingError", false, false, funcTable, 3);
            c$4___init__ = Py.newCode(2, new String[] {"self", "args"}, "I:\\java\\JPython-1.1\\org\\python\\modules\\cPickle_exceptions.py", "__init__", true, false, funcTable, 4);
            c$5___str__ = Py.newCode(1, new String[] {"self", "a"}, "I:\\java\\JPython-1.1\\org\\python\\modules\\cPickle_exceptions.py", "__str__", false, false, funcTable, 5);
            c$6_UnpickleableError = Py.newCode(0, new String[] {"__init__", "__str__"}, "I:\\java\\JPython-1.1\\org\\python\\modules\\cPickle_exceptions.py", "UnpickleableError", false, false, funcTable, 6);
            c$7_UnpicklingError = Py.newCode(0, new String[] {}, "I:\\java\\JPython-1.1\\org\\python\\modules\\cPickle_exceptions.py", "UnpicklingError", false, false, funcTable, 7);
            c$8_main = Py.newCode(0, new String[] {}, "I:\\java\\JPython-1.1\\org\\python\\modules\\cPickle_exceptions.py", "main", false, false, funcTable, 8);
        }
        
        
        public PyCode getMain() {
            if (c$8_main == null) _PyInner.initConstants();
            return c$8_main;
        }
        
        public PyObject call_function(int index, PyFrame frame) {
            switch (index){
                case 0:
                return _PyInner.__init__$1(frame);
                case 1:
                return _PyInner.__str__$2(frame);
                case 2:
                return _PyInner.PickleError$3(frame);
                case 3:
                return _PyInner.PicklingError$4(frame);
                case 4:
                return _PyInner.__init__$5(frame);
                case 5:
                return _PyInner.__str__$6(frame);
                case 6:
                return _PyInner.UnpickleableError$7(frame);
                case 7:
                return _PyInner.UnpicklingError$8(frame);
                case 8:
                return _PyInner.main$9(frame);
                default:
                return null;
            }
        }
        
        private static PyObject __init__$1(PyFrame frame) {
            frame.getlocal(0).__setattr__("args", frame.getlocal(1));
            return Py.None;
        }
        
        private static PyObject __str__$2(PyFrame frame) {
            // Temporary Variables
            PyObject t$0$PyObject, t$1$PyObject;
            
            // Code
            return (t$0$PyObject = ((t$1$PyObject = frame.getlocal(0).__getattr__("args")).__nonzero__() ? s$1._mod(frame.getlocal(0).__getattr__("args").__getitem__(i$2)) : t$1$PyObject)).__nonzero__() ? t$0$PyObject : s$3;
        }
        
        private static PyObject PickleError$3(PyFrame frame) {
            frame.setlocal("__init__", new PyFunction(frame.f_globals, new PyObject[] {}, c$0___init__));
            frame.setlocal("__str__", new PyFunction(frame.f_globals, new PyObject[] {}, c$1___str__));
            return frame.getf_locals();
        }
        
        private static PyObject PicklingError$4(PyFrame frame) {
            // pass
            return frame.getf_locals();
        }
        
        private static PyObject __init__$5(PyFrame frame) {
            frame.getlocal(0).__setattr__("args", frame.getlocal(1));
            return Py.None;
        }
        
        private static PyObject __str__$6(PyFrame frame) {
            // Temporary Variables
            PyObject t$0$PyObject, t$1$PyObject;
            
            // Code
            frame.setlocal(1, frame.getlocal(0).__getattr__("args"));
            frame.setlocal(1, (t$0$PyObject = ((t$1$PyObject = frame.getlocal(1)).__nonzero__() ? frame.getglobal("type").__call__(frame.getlocal(1).__getitem__(i$2)) : t$1$PyObject)).__nonzero__() ? t$0$PyObject : s$3);
            return s$4._mod(frame.getlocal(1));
        }
        
        private static PyObject UnpickleableError$7(PyFrame frame) {
            frame.setlocal("__init__", new PyFunction(frame.f_globals, new PyObject[] {}, c$4___init__));
            frame.setlocal("__str__", new PyFunction(frame.f_globals, new PyObject[] {}, c$5___str__));
            return frame.getf_locals();
        }
        
        private static PyObject UnpicklingError$8(PyFrame frame) {
            // pass
            return frame.getf_locals();
        }
        
        private static PyObject main$9(PyFrame frame) {
            frame.setglobal("__file__", s$0);
            frame.setglobal("PickleError", Py.makeClass("PickleError", new PyObject[] {frame.getglobal("Exception")}, c$2_PickleError, null));
            frame.setglobal("PicklingError", Py.makeClass("PicklingError", new PyObject[] {frame.getglobal("PickleError")}, c$3_PicklingError, null));
            frame.setglobal("UnpickleableError", Py.makeClass("UnpickleableError", new PyObject[] {frame.getglobal("PicklingError")}, c$6_UnpickleableError, null));
            frame.setglobal("UnpicklingError", Py.makeClass("UnpicklingError", new PyObject[] {frame.getglobal("PickleError")}, c$7_UnpicklingError, null));
            return Py.None;
        }
        
    }
    public void initModule(PyObject dict) {
        dict.__setitem__("__name__", new PyString("cPickle_exceptions"));
        Py.runCode(new _PyInner().getMain(), dict, dict);
    }
    
    public static void main(String[] args) {
        String[] newargs = new String[args.length+1];
        newargs[0] = "cPickle_exceptions";
        System.arraycopy(args, 0, newargs, 1, args.length);
        Py.runMain("org.python.modules.cPickle_exceptions$_PyInner", newargs, jpy$packages, jpy$properties, null, null);
    }
    
}
