package org.python.core;

import org.python.expose.ExposedMethod;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedType;

@ExposedType(name = "enumerate", base = PyObject.class)
public class PyEnumerate extends PyIterator {
    
    public static final PyType TYPE = PyType.fromClass(PyEnumerate.class);

    private long en_index;          /* current index of enumeration */
    private PyObject en_sit;        /* secondary iterator of enumeration */

    public PyObject next() {
        return enumerate_next();
    }

    @ExposedMethod
    final PyObject enumerate_next() {
        return doNext(enumerate___iternext__());
    }

    @ExposedMethod
    final PyObject enumerate___iter__() {
        return super.__iter__();
    }

    @ExposedNew
    public final static PyObject enumerate_new(PyNewWrapper new_,
                                            boolean init,
                                            PyType subtype,
                                            PyObject[] args,
                                            String[] keywords) {
        if (args.length != 1) {
            throw PyBuiltinFunction.DefaultInfo.unexpectedCall(args.length,
                                                               false,
                                                               "enumerate",
                                                               0,
                                                               1);
        }
        if (new_.for_type == subtype) {
            return new PyEnumerate(args[0]);
        } else {
            return new PyEnumerateDerived(subtype, args[0]);
        }
    }
    
    public PyEnumerate(PyType subType) {
        super(subType);
    }
    
    public PyEnumerate(PyType subType, PyObject seq) {
        super(subType);
        en_index = 0;
        en_sit = seq.__iter__();
    }
    
    public PyEnumerate(PyObject seq) {
        this(TYPE, seq);
    }
    
    public PyObject __iternext__() {
        return enumerate___iternext__();
    }

    final PyObject enumerate___iternext__() {
        PyObject next_item;

        next_item = en_sit.__iternext__();
        if(next_item == null){
            if(en_sit instanceof PyIterator && ((PyIterator)en_sit).stopException != null){
                stopException = ((PyIterator)en_sit).stopException;
            }
            return null;
        }

        return new PyTuple(new PyInteger((int)en_index++), next_item);
    }
}
