package org.python.core;

public abstract class PyBuiltinFunctionWide extends PyBuiltinFunction {

    public static final Class exposed_as = PyBuiltinFunction.class;

    public PyBuiltinFunctionWide(Info info) {
        super(info);
    }

    public PyObject inst_call(PyObject self) {
        return inst_call(self,Py.EmptyObjects);
    }

    public PyObject inst_call(PyObject self, PyObject arg0) {
        return inst_call(self,new PyObject[] {arg0});
    }

    public PyObject inst_call(PyObject self, PyObject arg0, PyObject arg1) {
        return inst_call(self,new PyObject[] {arg0,arg1});
    }

    public PyObject inst_call(
        PyObject self,
        PyObject arg0,
        PyObject arg1,
        PyObject arg2) {
        return inst_call(self,new PyObject[] {arg0,arg1,arg2});            
    }

    public PyObject inst_call(
        PyObject self,
        PyObject arg0,
        PyObject arg1,
        PyObject arg2,
        PyObject arg3) {
        return inst_call(self,new PyObject[] {arg0,arg1,arg2,arg3});
    }

    /* to override */

    public PyObject inst_call(
        PyObject self,
        PyObject[] args,
        String[] keywords) {
            if (keywords.length != 0 ) {
                throw info.unexpectedCall(args.length,true);
            }
            return inst_call(self,args);
   }
    
    abstract public PyObject inst_call(PyObject self, PyObject[] args);

    public PyObject __call__(PyObject[] args, String[] keywords) {
        return inst_call(getSelf(),args, keywords);
    }

    public PyObject __call__(PyObject[] args) {
        return inst_call(getSelf(),args);
    }

}
