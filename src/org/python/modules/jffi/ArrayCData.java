
package org.python.modules.jffi;

import org.python.core.Py;
import org.python.core.PyIterator;
import org.python.core.PyList;
import org.python.core.PyNewWrapper;
import org.python.core.PyObject;
import org.python.core.PySequenceList;
import org.python.core.PyType;
import org.python.core.SequenceIndexDelegate;
import org.python.expose.ExposedClassMethod;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedType;

@ExposedType(name = "jffi.ArrayCData", base = CData.class)
public class ArrayCData extends CData implements Pointer {
    public static final PyType TYPE = PyType.fromClass(ArrayCData.class);

    final CType.Array arrayType;
    final CType componentType;
    final MemoryOp componentMemoryOp;

    ArrayCData(PyType subtype, CType.Array arrayType, DirectMemory memory, MemoryOp componentMemoryOp) {
        super(subtype, arrayType, memory);
        this.arrayType = arrayType;
        this.componentType = arrayType.componentType;
        this.componentMemoryOp = componentMemoryOp;
    }

    @ExposedNew
    public static PyObject ArrayCData_new(PyNewWrapper new_, boolean init, PyType subtype,
            PyObject[] args, String[] keywords) {

        CType.Array arrayType = getArrayType(subtype);

        // Only clear the array if it is not going to be completely filled
        boolean clear = args.length < arrayType.length;
        DirectMemory memory = AllocatedNativeMemory.allocateAligned(arrayType.componentType.size() * arrayType.length,
                arrayType.componentType.alignment(), clear);
        int offset = 0;
        for (PyObject value : args) {
            arrayType.componentMemoryOp.put(memory, offset, value);
            offset += arrayType.componentType.size();
        }
        return new ArrayCData(subtype, arrayType, memory, arrayType.componentMemoryOp);
    }

    static final CType.Array getArrayType(PyType subtype) {
        PyObject jffi_type = subtype.__getattr__("_jffi_type");

        if (!(jffi_type instanceof CType.Array)) {
            throw Py.TypeError("invalid _jffi_type for " + subtype.getName());
        }

        return (CType.Array) jffi_type;
    }

    @ExposedClassMethod(names= { "from_address" })
    public static final PyObject from_address(PyType subtype, PyObject address) {

        CType.Array arrayType = getArrayType(subtype);
        DirectMemory m = Util.getMemoryForAddress(address);
        PointerCData cdata = new PointerCData(subtype, arrayType, m.getMemory(0), arrayType.componentMemoryOp);
        cdata.setReferenceMemory(m);

        return cdata;
    }

    public final DirectMemory getMemory() {
        return getReferenceMemory();
    }

    protected final void initReferenceMemory(Memory m) {
        // Nothing to do, since the reference memory was initialized during construction
    }

    @Override
    public PyObject __finditem__(int index) {
        return delegator.checkIdxAndFindItem(index);
    }

    @Override
    public PyObject __getitem__(PyObject index) {
        return delegator.checkIdxAndGetItem(index);
    }

    @Override
    public void __setitem__(int index, PyObject value) {
        delegator.checkIdxAndSetItem(index, value);
    }

    @Override
    public void __setitem__(PyObject index, PyObject value) {
        delegator.checkIdxAndSetItem(index, value);
    }

    @Override
    public void __delitem__(PyObject key) {
        throw Py.TypeError("Array does not support item deletion");
    }


    @Override
    public PyObject __iter__() {
        return new ArrayIter();
    }

    protected final SequenceIndexDelegate delegator = new SequenceIndexDelegate() {

        @Override
        public String getTypeName() {
            return getType().fastGetName();
        }

        @Override
        public void setItem(int idx, PyObject value) {
            componentMemoryOp.put(getReferenceMemory(), idx * componentType.size(), value);
        }

        @Override
        public void setSlice(int start, int stop, int step, PyObject value) {
            if (!(value instanceof PySequenceList)) {
                throw Py.TypeError("expected list or tuple");
            }
            PySequenceList list = (PySequenceList) value;
            for (int i = 0; i < stop - start; ++i) {
                setItem(start + i, list.pyget(i));
            }
        }

        @Override
        public int len() {
            return arrayType.length;
        }

        @Override
        public void delItem(int idx) {
            throw Py.TypeError("Array does not support item deletion");
        }

        @Override
        public void delItems(int start, int stop) {
            throw Py.TypeError("Array does not support item deletion");
        }


        @Override
        public PyObject getItem(int idx) {
            return componentMemoryOp.get(getReferenceMemory(), idx * componentType.size());
        }

        @Override
        public PyObject getSlice(int start, int stop, int step) {
            PyObject[] result = new PyObject[stop - start];
            for (int i = 0; i < result.length; ++i) {
                result[i] = getItem(start + i);
            }
            return new PyList(result);
        }
    };

    public class ArrayIter extends PyIterator {

        private int index = 0;

        public PyObject __iternext__() {
            if (index >= arrayType.length) {
                return null;
            }
            return componentMemoryOp.get(getReferenceMemory(), index++ * componentType.size());
        }
    }

}
