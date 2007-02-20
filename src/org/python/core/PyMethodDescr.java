package org.python.core;

public class PyMethodDescr extends PyDescriptor implements PyBuiltinFunction.Info
{

    protected int minargs, maxargs;
       
    protected PyBuiltinFunction func;

    public PyMethodDescr(String name, Class c, int minargs,
                                int maxargs, PyBuiltinFunction func)
    {
        this.name = name;
        this.dtype = PyType.fromClass(c);
        this.minargs = minargs;
        this.maxargs = maxargs;
        this.func = func;
        this.func.setInfo(this);
    }

    public String getName() {
        return name;
    }

    public int getMaxargs() {
        return maxargs;
    }
    public int getMinargs() {
        return minargs;
    }
    
    public String toString() {
        return "<method '"+name+"' of '"+dtype.fastGetName()+"' objects>";
    }

    public PyObject __call__(PyObject[] args) {
        return extractSelfAndCall(args); 
    }

    public PyObject __call__(PyObject[] args, String[] kws) {
        if (kws.length == args.length)
            throw Py.TypeError(blurb()+" needs an argument");
        return extractSelfAndCall(args);
    }

    private PyObject extractSelfAndCall(PyObject[] args) {
        PyObject self = args[0];
        checkCallerType(self);              
        int n = args.length;
        PyObject[] rest = new PyObject[n-1];
        System.arraycopy(args,1,rest,0,n-1);
        return func.inst_call(self,rest);
    }

    public PyObject __call__() {
        throw Py.TypeError(blurb()+" needs an argument");
    }

    public PyObject __call__(PyObject arg1) {
        checkCallerType(arg1);     
        return func.inst_call(arg1);
    }

    public PyObject __call__(PyObject arg1, PyObject arg2) {
        checkCallerType(arg1);
        return func.inst_call(arg1,arg2);
    }

    public PyObject __call__(PyObject arg1, PyObject arg2, PyObject arg3) {
        checkCallerType(arg1);      
        return func.inst_call(arg1,arg2,arg3);
    }

    public PyObject __call__(PyObject arg1, PyObject arg2, PyObject arg3,
                             PyObject arg4)
    {
        checkCallerType(arg1);
        return func.inst_call(arg1,arg2,arg3,arg4);
    }
 
    public PyException unexpectedCall(int nargs,boolean keywords) {
        return PyBuiltinFunction.DefaultInfo.unexpectedCall(nargs,keywords,name,minargs,maxargs);
    }

    public PyObject __get__(PyObject obj, PyObject type) {
        if (obj != null) {
            checkCallerType(obj);
            return func.makeBound(obj); 
        }
        return this;
    }

    protected void checkCallerType(PyObject obj) {
        PyType objtype = obj.getType();
        if (objtype != dtype && !objtype.isSubType(dtype))
            throw get_wrongtype(objtype);
    }

}
