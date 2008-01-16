//Copyright (c) Corporation for National Research Initiatives
package org.python.core;

import org.python.core.PyObject.ConversionException;

/**
 * A python slice object.
 */

public class PySlice extends PyObject {

    //~ BEGIN GENERATED REGION -- DO NOT EDIT SEE gexpose.py
    /* type info */

    public static final String exposed_name="slice";

    public static void typeSetup(PyObject dict,PyType.Newstyle marker) {
        dict.__setitem__("start",new PyGetSetDescr("start",PySlice.class,"getStart",null,null));
        dict.__setitem__("stop",new PyGetSetDescr("stop",PySlice.class,"getStop",null,null));
        dict.__setitem__("step",new PyGetSetDescr("step",PySlice.class,"getStep",null,null));
        class exposed_indices extends PyBuiltinMethodNarrow {

            exposed_indices(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_indices(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                return((PySlice)self).slice_indices(arg0);
            }

        }
        dict.__setitem__("indices",new PyMethodDescr("indices",PySlice.class,1,1,new exposed_indices(null,null)));
        class exposed___hash__ extends PyBuiltinMethodNarrow {

            exposed___hash__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___hash__(self,info);
            }

            public PyObject __call__() {
                throw Py.TypeError("unhashable type");
            }

        }
        dict.__setitem__("__hash__",new PyMethodDescr("__hash__",PySlice.class,0,0,new exposed___hash__(null,null)));
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
                ((PySlice)self).slice_init(args,keywords);
                return Py.None;
            }

        }
        dict.__setitem__("__init__",new PyMethodDescr("__init__",PySlice.class,-1,-1,new exposed___init__(null,null)));
        dict.__setitem__("__new__",new PyNewWrapper(PySlice.class,"__new__",-1,-1) {

                                                                                       public PyObject new_impl(boolean init,PyType subtype,PyObject[]args,String[]keywords) {
                                                                                           PySlice newobj;
                                                                                           if (for_type==subtype) {
                                                                                               newobj=new PySlice();
                                                                                               if (init)
                                                                                                   newobj.slice_init(args,keywords);
                                                                                           } else {
                                                                                               newobj=new PySliceDerived(subtype);
                                                                                           }
                                                                                           return newobj;
                                                                                       }

                                                                                   });
    }
    //~ END GENERATED REGION -- DO NOT EDIT SEE gexpose.py

    final void slice_init(PyObject[] args, String[] keywords) {
        if(args.length == 0) {
            throw Py.TypeError("slice expected at least 1 arguments, got " + args.length);
        } else if(args.length > 3) {
            throw Py.TypeError("slice expected at most 3 arguments, got " + args.length);
        }
        ArgParser ap = new ArgParser("slice", args, keywords, "start", "stop", "step");
        if(args.length == 1) {
            stop = ap.getPyObject(0);
        } else if(args.length == 2) {
            start = ap.getPyObject(0);
            stop = ap.getPyObject(1);
        } else if(args.length == 3) {
            start = ap.getPyObject(0);
            stop = ap.getPyObject(1);
            step = ap.getPyObject(2);
        }
    }

    private static final PyType SLICE_TYPE = PyType.fromClass(PySlice.class);

    public PySlice(PyObject start, PyObject stop, PyObject step) {
        this(SLICE_TYPE);
        if(start != null) {
            this.start = start;
        }
        if(stop != null) {
            this.stop = stop;
        }
        if(step != null) {
            this.step = step;
        }
    }

    public PySlice(PyType type) {
        super(type);
    }
    
    public PySlice() {
        this(SLICE_TYPE);
    }

    public final PyObject getStart() {
        return start;
    }

    public final PyObject getStop() {
        return stop;
    }

    public final PyObject getStep() {
        return step;
    }

    public int hashCode() { 
        return slice_hashCode();
    }

    final int slice_hashCode() {
        throw Py.TypeError("unhashable type");
    } 

    public PyString __str__() {
        return new PyString(getStart().__repr__() + ":" + getStop().__repr__() + ":" +
                getStep().__repr__());
    }

    public PyString __repr__() {
        return new PyString("slice(" + getStart().__repr__() + ", " +
                getStop().__repr__() + ", " +
                getStep().__repr__() + ")");
    }

    public PyObject __eq__(PyObject o) {
        if(getType() != o.getType() && !(getType().isSubType(o.getType()))) {
            return null;
        }
        if(this == o) {
            return Py.True;
        }
        PySlice oSlice = (PySlice)o;
        if(eq(getStart(), oSlice.getStart()) && eq(getStop(), oSlice.getStop())
                && eq(getStep(), oSlice.getStep())) {
            return Py.True;
        }
        return Py.False;
    }
    
    private static final boolean eq(PyObject o1, PyObject o2) {
        return o1._cmp(o2) == 0;
    }

    public PyObject __ne__(PyObject o) {
        return __eq__(o).__not__();
    }
    
    public PyObject slice_indices(PyObject len) {
        int ilen;
        try {
            ilen = len.asInt(0);
        } catch(ConversionException e) {
            throw Py.TypeError("length must be an int");
        }
        int[] slice = indices(ilen);
        PyInteger[] pyInts = new PyInteger[slice.length];
        for(int i = 0; i < pyInts.length; i++) {
            pyInts[i] = Py.newInteger(slice[i]);
        }
        return new PyTuple(pyInts);
    }

    private static int calculateSliceIndex(PyObject v) {
        if(v instanceof PyInteger) {
            return ((PyInteger)v).getValue();
        } else if(v instanceof PyLong) {
            try {
                return ((PyInteger)v.__int__()).getValue();
            } catch (PyException exc) {
                if (Py.matchException(exc, Py.OverflowError)) {
                    if (new PyLong(0L).__cmp__(v) < 0) {
                        return Integer.MAX_VALUE;
                    }else {
                        return 0;
                    }
                }
            }
        }
        throw Py.TypeError("slice indices must be integers or None");
    }
    
    /**
     * Calculates the actual indices of a slice with this slice's start, stop
     * and step values for a sequence of length <code>len</code>.
     * 
     * @return an array with the start at index 0, stop at index 1 and step and
     *         index 2.
     */
    public int[] indices(int len) {
        int[] slice = new int[3];
        if (getStep() == Py.None) {
            slice[STEP] = 1;
        } else {
            slice[STEP] = calculateSliceIndex(getStep());
            if (slice[STEP] == 0) {
                throw Py.ValueError("slice step cannot be zero");
            }
        }

        if (getStart() == Py.None) {
            slice[START] = slice[STEP] < 0 ? len - 1 : 0;
        } else {
            slice[START] = calculateSliceIndex(getStart());
            if(slice[START] < 0) {
                slice[START] += len;
            }
            if(slice[START] < 0) {
                slice[START] = slice[STEP] < 0 ? -1 : 0;
            }
            if(slice[START] >= len) {
                slice[START] = slice[STEP] < 0 ? len - 1 : len;
            }
        }
        if(getStop() == Py.None) {
            slice[STOP] = slice[STEP] < 0 ? -1 : len;
        } else {
            slice[STOP] = calculateSliceIndex(getStop());
            if(slice[STOP] < 0) {
                slice[STOP] += len;
            }
            if(slice[STOP] < 0) {
                slice[STOP] = -1;
            }
            if(slice[STOP] > len) {
                slice[STOP] = len;
            }
        }
        return slice;
    }
    private static final int START = 0, STOP = 1, STEP = 2;

    public PyObject start = Py.None;

    public PyObject stop = Py.None;

    public PyObject step = Py.None;
}
