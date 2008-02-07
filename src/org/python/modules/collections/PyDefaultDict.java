package org.python.modules.collections;

import java.util.Map;

import org.python.core.PyBuiltinFunction;
import org.python.core.PyBuiltinMethod;
import org.python.core.PyBuiltinMethodNarrow;
import org.python.core.PyException;
import org.python.core.PyMethodDescr;
import org.python.core.PyGetSetDescr;
import org.python.core.PyNewWrapper;
import org.python.core.PyDictionary;
import org.python.core.PyObject;
import org.python.core.PyType;
import org.python.core.Py;
import org.python.core.PyTuple;
import org.python.core.PyString;
import org.python.core.PyFunction;
import org.python.core.__builtin__;

/**
 * PyDefaultDict - This is a subclass of the builtin dict(PyDictionary) class. 
 * It supports one additional method __missing__ and adds one writable instance 
 * variable default_factory. The remaining functionality is the same as for the dict
 * class. 
 *   
 * collections.defaultdict([default_factory[, ...]]) - returns a new dictionary-like 
 * object. The first argument provides the initial value for the default_factory 
 * attribute; it defaults to None. All remaining arguments are treated the same as 
 * if they were passed to the dict constructor, including keyword arguments.
 *    
 * @author    Mehendran T (mehendran@gmail.com)
 *            Novell Software Development (I) Pvt. Ltd              
 * @created   Tue 18-Sep-2007 21:09:09
 */
public class PyDefaultDict extends PyDictionary {
    
    //~ BEGIN GENERATED REGION -- DO NOT EDIT SEE gexpose.py
    /* type info */

    public static final String exposed_name="defaultdict";

    public static void typeSetup(PyObject dict,PyType.Newstyle marker) {
        dict.__setitem__("default_factory",new PyGetSetDescr("default_factory",PyDefaultDict.class,"getDefaultFactory","setDefaultFactory",null));
        class exposed___getitem__ extends PyBuiltinMethodNarrow {

            exposed___getitem__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___getitem__(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                return((PyDefaultDict)self).defaultdict___getitem__(arg0);
            }

        }
        dict.__setitem__("__getitem__",new PyMethodDescr("__getitem__",PyDefaultDict.class,1,1,new exposed___getitem__(null,null)));
        class exposed___missing__ extends PyBuiltinMethodNarrow {

            exposed___missing__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___missing__(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                return((PyDefaultDict)self).defaultdict___missing__(arg0);
            }

        }
        dict.__setitem__("__missing__",new PyMethodDescr("__missing__",PyDefaultDict.class,1,1,new exposed___missing__(null,null)));
        class exposed___reduce__ extends PyBuiltinMethodNarrow {

            exposed___reduce__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___reduce__(self,info);
            }

            public PyObject __call__() {
                return((PyDefaultDict)self).defaultdict___reduce__();
            }

        }
        dict.__setitem__("__reduce__",new PyMethodDescr("__reduce__",PyDefaultDict.class,0,0,new exposed___reduce__(null,null)));
        class exposed_copy extends PyBuiltinMethodNarrow {

            exposed_copy(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_copy(self,info);
            }

            public PyObject __call__() {
                return((PyDefaultDict)self).defaultdict_copy();
            }

        }
        dict.__setitem__("copy",new PyMethodDescr("copy",PyDefaultDict.class,0,0,new exposed_copy(null,null)));
        class exposed___copy__ extends PyBuiltinMethodNarrow {

            exposed___copy__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___copy__(self,info);
            }

            public PyObject __call__() {
                return((PyDefaultDict)self).defaultdict___copy__();
            }

        }
        dict.__setitem__("__copy__",new PyMethodDescr("__copy__",PyDefaultDict.class,0,0,new exposed___copy__(null,null)));
        class exposed___repr__ extends PyBuiltinMethodNarrow {

            exposed___repr__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___repr__(self,info);
            }

            public PyObject __call__() {
                return new PyString(((PyDefaultDict)self).defaultdict_toString());
            }

        }
        dict.__setitem__("__repr__",new PyMethodDescr("__repr__",PyDefaultDict.class,0,0,new exposed___repr__(null,null)));
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
                ((PyDefaultDict)self).defaultdict_init(args,keywords);
                return Py.None;
            }

        }
        dict.__setitem__("__init__",new PyMethodDescr("__init__",PyDefaultDict.class,-1,-1,new exposed___init__(null,null)));
        dict.__setitem__("__new__",new PyNewWrapper(PyDefaultDict.class,"__new__",-1,-1) {

                                                                                             public PyObject new_impl(boolean init,PyType subtype,PyObject[]args,String[]keywords) {
                                                                                                 PyDefaultDict newobj;
                                                                                                 if (for_type==subtype) {
                                                                                                     newobj=new PyDefaultDict();
                                                                                                     if (init)
                                                                                                         newobj.defaultdict_init(args,keywords);
                                                                                                 } else {
                                                                                                     newobj=new PyDefaultDictDerived(subtype);
                                                                                                 }
                                                                                                 return newobj;
                                                                                             }

                                                                                         });
    }
    //~ END GENERATED REGION -- DO NOT EDIT SEE gexpose.py
    
    private static final PyType DEFAULTDICT_TYPE = PyType.fromClass(PyDefaultDict.class);
    
    /**
     * This attribute is used by the __missing__ method; it is initialized from
     * the first argument to the constructor, if present, or to None, if absent. 
     */
    private PyObject default_factory = Py.None;

    public PyDefaultDict() {
        this(DEFAULTDICT_TYPE);
    }

    public PyDefaultDict(PyType subtype) {
        super(subtype);
    }
   
    public PyDefaultDict(PyType subtype, Map<PyObject, PyObject> map) {
        super(subtype, map);
    }
    
    final void defaultdict_init(PyObject[] args, String[] kwds) {
        int nargs = args.length - kwds.length;
        if (nargs != 0) {    		
            default_factory = args[0];
            if (default_factory.__findattr__("__call__") == null) {
                throw Py.TypeError("first argument must be callable");
            }
            PyObject newargs[] = new PyObject[args.length - 1];
            System.arraycopy(args, 1, newargs, 0, newargs.length);
            dict_init(newargs , kwds);
        }
    }

    public PyObject getDefaultFactory() {
        return default_factory;
    }

    public void setDefaultFactory(PyObject value) {
        default_factory = value;
    }

    final PyObject defaultdict___getitem__(PyObject key) {
        PyObject val = super.__finditem__(key);
        if (val == null) {
            val = defaultdict___missing__(key);
        }
        return val;
    }

    public PyObject __finditem__(PyObject key) {
        return defaultdict___getitem__(key);
    }

    /**
     * This method is called by the __getitem__ method of the dict class when 
     * the requested key is not found; whatever it returns or raises is then 
     * returned or raised by __getitem__.
     */
    final PyObject defaultdict___missing__(PyObject key) {
        if (default_factory == Py.None) {
            throw Py.KeyError(key);
        }    	       
        PyObject value = default_factory.__call__();
        if (value == null) {
            return value;
        }        
        __setitem__(key, value);
        return value;
    }

    public PyObject __reduce__() {
        return defaultdict___reduce__();
    }

    final PyObject defaultdict___reduce__() {
        PyTuple args = null;
        if (default_factory == Py.None) {
            args = new PyTuple();
        } else {	
            PyObject[] ob = {default_factory};
            args = new PyTuple(ob);
        }	
        return new PyTuple(new PyObject[]{this.getType(), args, Py.None, 
                Py.None, this.items()});
    }

    public PyDictionary copy() {
        return defaultdict_copy();
    }

    final PyDefaultDict defaultdict_copy() { 
        return defaultdict___copy__();
    }

    final PyDefaultDict defaultdict___copy__() {
        PyDefaultDict ob = new PyDefaultDict(DEFAULTDICT_TYPE, table);
        ob.default_factory = default_factory;
        return ob;
    }

    public String toString() {
        return defaultdict_toString();
    }

    final String defaultdict_toString() {
        return "defaultdict(" + default_factory  +", " + super.toString() + ")";
    }
}
