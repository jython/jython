// Copyright (c) Corporation for National Research Initiatives

// Implementation of the standard Python list objects

package org.python.core;
import java.util.Vector;

/**
 * A builtin python list.
 */

public class PyList extends PySequence {
    protected int length;
    protected PyObject[] list;

    public static void classDictInit(PyObject dict) throws PyIgnoreMethodTag {}

    /* type info */

    public static final String exposed_name="list";

    public static void typeSetup(PyObject dict,PyType.Newstyle marker) {
        class exposed___ne__ extends PyBuiltinFunctionNarrow {

            private PyList self;

            public PyObject getSelf() {
                return self;
            }

            exposed___ne__(PyList self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___ne__((PyList)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=self.list___ne__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyList self=(PyList)gself;
                PyObject ret=self.list___ne__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__ne__",new PyMethodDescr("__ne__",PyList.class,1,1,new exposed___ne__(null,null)));
        class exposed___eq__ extends PyBuiltinFunctionNarrow {

            private PyList self;

            public PyObject getSelf() {
                return self;
            }

            exposed___eq__(PyList self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___eq__((PyList)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=self.list___eq__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyList self=(PyList)gself;
                PyObject ret=self.list___eq__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__eq__",new PyMethodDescr("__eq__",PyList.class,1,1,new exposed___eq__(null,null)));
        class exposed_append extends PyBuiltinFunctionNarrow {

            private PyList self;

            public PyObject getSelf() {
                return self;
            }

            exposed_append(PyList self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed_append((PyList)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                self.list_append(arg0);
                return Py.None;
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyList self=(PyList)gself;
                self.list_append(arg0);
                return Py.None;
            }

        }
        dict.__setitem__("append",new PyMethodDescr("append",PyList.class,1,1,new exposed_append(null,null)));
        class exposed_count extends PyBuiltinFunctionNarrow {

            private PyList self;

            public PyObject getSelf() {
                return self;
            }

            exposed_count(PyList self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed_count((PyList)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                return Py.newInteger(self.list_count(arg0));
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyList self=(PyList)gself;
                return Py.newInteger(self.list_count(arg0));
            }

        }
        dict.__setitem__("count",new PyMethodDescr("count",PyList.class,1,1,new exposed_count(null,null)));
        class exposed_extend extends PyBuiltinFunctionNarrow {

            private PyList self;

            public PyObject getSelf() {
                return self;
            }

            exposed_extend(PyList self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed_extend((PyList)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                self.list_extend(arg0);
                return Py.None;
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyList self=(PyList)gself;
                self.list_extend(arg0);
                return Py.None;
            }

        }
        dict.__setitem__("extend",new PyMethodDescr("extend",PyList.class,1,1,new exposed_extend(null,null)));
        class exposed_index extends PyBuiltinFunctionNarrow {

            private PyList self;

            public PyObject getSelf() {
                return self;
            }

            exposed_index(PyList self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed_index((PyList)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                return Py.newInteger(self.list_index(arg0));
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyList self=(PyList)gself;
                return Py.newInteger(self.list_index(arg0));
            }

        }
        dict.__setitem__("index",new PyMethodDescr("index",PyList.class,1,1,new exposed_index(null,null)));
        class exposed_insert extends PyBuiltinFunctionNarrow {

            private PyList self;

            public PyObject getSelf() {
                return self;
            }

            exposed_insert(PyList self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed_insert((PyList)self,info);
            }

            public PyObject __call__(PyObject arg0,PyObject arg1) {
                try {
                    self.list_insert(arg0.asInt(0),arg1);
                    return Py.None;
                } catch (PyObject.ConversionException e) {
                    String msg;
                    switch (e.index) {
                    case 0:
                        msg="expected an integer";
                        break;
                    default:
                        msg="xxx";
                    }
                    throw Py.TypeError(msg);
                }
            }

            public PyObject inst_call(PyObject gself,PyObject arg0,PyObject arg1) {
                PyList self=(PyList)gself;
                try {
                    self.list_insert(arg0.asInt(0),arg1);
                    return Py.None;
                } catch (PyObject.ConversionException e) {
                    String msg;
                    switch (e.index) {
                    case 0:
                        msg="expected an integer";
                        break;
                    default:
                        msg="xxx";
                    }
                    throw Py.TypeError(msg);
                }
            }

        }
        dict.__setitem__("insert",new PyMethodDescr("insert",PyList.class,2,2,new exposed_insert(null,null)));
        class exposed_pop extends PyBuiltinFunctionNarrow {

            private PyList self;

            public PyObject getSelf() {
                return self;
            }

            exposed_pop(PyList self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed_pop((PyList)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                try {
                    return self.list_pop(arg0.asInt(0));
                } catch (PyObject.ConversionException e) {
                    String msg;
                    switch (e.index) {
                    case 0:
                        msg="expected an integer";
                        break;
                    default:
                        msg="xxx";
                    }
                    throw Py.TypeError(msg);
                }
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyList self=(PyList)gself;
                try {
                    return self.list_pop(arg0.asInt(0));
                } catch (PyObject.ConversionException e) {
                    String msg;
                    switch (e.index) {
                    case 0:
                        msg="expected an integer";
                        break;
                    default:
                        msg="xxx";
                    }
                    throw Py.TypeError(msg);
                }
            }

            public PyObject __call__() {
                return self.list_pop();
            }

            public PyObject inst_call(PyObject gself) {
                PyList self=(PyList)gself;
                return self.list_pop();
            }

        }
        dict.__setitem__("pop",new PyMethodDescr("pop",PyList.class,0,1,new exposed_pop(null,null)));
        class exposed_remove extends PyBuiltinFunctionNarrow {

            private PyList self;

            public PyObject getSelf() {
                return self;
            }

            exposed_remove(PyList self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed_remove((PyList)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                self.list_remove(arg0);
                return Py.None;
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyList self=(PyList)gself;
                self.list_remove(arg0);
                return Py.None;
            }

        }
        dict.__setitem__("remove",new PyMethodDescr("remove",PyList.class,1,1,new exposed_remove(null,null)));
        class exposed_reverse extends PyBuiltinFunctionNarrow {

            private PyList self;

            public PyObject getSelf() {
                return self;
            }

            exposed_reverse(PyList self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed_reverse((PyList)self,info);
            }

            public PyObject __call__() {
                self.list_reverse();
                return Py.None;
            }

            public PyObject inst_call(PyObject gself) {
                PyList self=(PyList)gself;
                self.list_reverse();
                return Py.None;
            }

        }
        dict.__setitem__("reverse",new PyMethodDescr("reverse",PyList.class,0,0,new exposed_reverse(null,null)));
        class exposed_sort extends PyBuiltinFunctionNarrow {

            private PyList self;

            public PyObject getSelf() {
                return self;
            }

            exposed_sort(PyList self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed_sort((PyList)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                self.list_sort(arg0);
                return Py.None;
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyList self=(PyList)gself;
                self.list_sort(arg0);
                return Py.None;
            }

            public PyObject __call__() {
                self.list_sort();
                return Py.None;
            }

            public PyObject inst_call(PyObject gself) {
                PyList self=(PyList)gself;
                self.list_sort();
                return Py.None;
            }

        }
        dict.__setitem__("sort",new PyMethodDescr("sort",PyList.class,0,1,new exposed_sort(null,null)));
        class exposed___contains__ extends PyBuiltinFunctionNarrow {

            private PyList self;

            public PyObject getSelf() {
                return self;
            }

            exposed___contains__(PyList self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___contains__((PyList)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                return Py.newBoolean(self.list___contains__(arg0));
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyList self=(PyList)gself;
                return Py.newBoolean(self.list___contains__(arg0));
            }

        }
        dict.__setitem__("__contains__",new PyMethodDescr("__contains__",PyList.class,1,1,new exposed___contains__(null,null)));
        class exposed___len__ extends PyBuiltinFunctionNarrow {

            private PyList self;

            public PyObject getSelf() {
                return self;
            }

            exposed___len__(PyList self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___len__((PyList)self,info);
            }

            public PyObject __call__() {
                return Py.newInteger(self.list___len__());
            }

            public PyObject inst_call(PyObject gself) {
                PyList self=(PyList)gself;
                return Py.newInteger(self.list___len__());
            }

        }
        dict.__setitem__("__len__",new PyMethodDescr("__len__",PyList.class,0,0,new exposed___len__(null,null)));
        class exposed___add__ extends PyBuiltinFunctionNarrow {

            private PyList self;

            public PyObject getSelf() {
                return self;
            }

            exposed___add__(PyList self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___add__((PyList)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                return self.list___add__(arg0);
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyList self=(PyList)gself;
                return self.list___add__(arg0);
            }

        }
        dict.__setitem__("__add__",new PyMethodDescr("__add__",PyList.class,1,1,new exposed___add__(null,null)));
        class exposed___iadd__ extends PyBuiltinFunctionNarrow {

            private PyList self;

            public PyObject getSelf() {
                return self;
            }

            exposed___iadd__(PyList self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___iadd__((PyList)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                return self.list___iadd__(arg0);
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyList self=(PyList)gself;
                return self.list___iadd__(arg0);
            }

        }
        dict.__setitem__("__iadd__",new PyMethodDescr("__iadd__",PyList.class,1,1,new exposed___iadd__(null,null)));
        class exposed___imul__ extends PyBuiltinFunctionNarrow {

            private PyList self;

            public PyObject getSelf() {
                return self;
            }

            exposed___imul__(PyList self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___imul__((PyList)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                return self.list___imul__(arg0);
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyList self=(PyList)gself;
                return self.list___imul__(arg0);
            }

        }
        dict.__setitem__("__imul__",new PyMethodDescr("__imul__",PyList.class,1,1,new exposed___imul__(null,null)));
        class exposed___mul__ extends PyBuiltinFunctionNarrow {

            private PyList self;

            public PyObject getSelf() {
                return self;
            }

            exposed___mul__(PyList self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___mul__((PyList)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                return self.list___mul__(arg0);
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyList self=(PyList)gself;
                return self.list___mul__(arg0);
            }

        }
        dict.__setitem__("__mul__",new PyMethodDescr("__mul__",PyList.class,1,1,new exposed___mul__(null,null)));
        class exposed___rmul__ extends PyBuiltinFunctionNarrow {

            private PyList self;

            public PyObject getSelf() {
                return self;
            }

            exposed___rmul__(PyList self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___rmul__((PyList)self,info);
            }

            public PyObject __call__(PyObject arg0) {
                return self.list___rmul__(arg0);
            }

            public PyObject inst_call(PyObject gself,PyObject arg0) {
                PyList self=(PyList)gself;
                return self.list___rmul__(arg0);
            }

        }
        dict.__setitem__("__rmul__",new PyMethodDescr("__rmul__",PyList.class,1,1,new exposed___rmul__(null,null)));
        class exposed___hash__ extends PyBuiltinFunctionNarrow {

            private PyList self;

            public PyObject getSelf() {
                return self;
            }

            exposed___hash__(PyList self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___hash__((PyList)self,info);
            }

            public PyObject __call__() {
                return Py.newInteger(self.list_hashCode());
            }

            public PyObject inst_call(PyObject gself) {
                PyList self=(PyList)gself;
                return Py.newInteger(self.list_hashCode());
            }

        }
        dict.__setitem__("__hash__",new PyMethodDescr("__hash__",PyList.class,0,0,new exposed___hash__(null,null)));
        class exposed___repr__ extends PyBuiltinFunctionNarrow {

            private PyList self;

            public PyObject getSelf() {
                return self;
            }

            exposed___repr__(PyList self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___repr__((PyList)self,info);
            }

            public PyObject __call__() {
                return new PyString(self.list_toString());
            }

            public PyObject inst_call(PyObject gself) {
                PyList self=(PyList)gself;
                return new PyString(self.list_toString());
            }

        }
        dict.__setitem__("__repr__",new PyMethodDescr("__repr__",PyList.class,0,0,new exposed___repr__(null,null)));
        class exposed___init__ extends PyBuiltinFunctionWide {

            private PyList self;

            public PyObject getSelf() {
                return self;
            }

            exposed___init__(PyList self,PyBuiltinFunction.Info info) {
                super(info);
                this.self=self;
            }

            public PyBuiltinFunction makeBound(PyObject self) {
                return new exposed___init__((PyList)self,info);
            }

            public PyObject inst_call(PyObject self,PyObject[]args) {
                return inst_call(self,args,Py.NoKeywords);
            }

            public PyObject __call__(PyObject[]args) {
                return __call__(args,Py.NoKeywords);
            }

            public PyObject __call__(PyObject[]args,String[]keywords) {
                self.list_init(args,keywords);
                return Py.None;
            }

            public PyObject inst_call(PyObject gself,PyObject[]args,String[]keywords) {
                PyList self=(PyList)gself;
                self.list_init(args,keywords);
                return Py.None;
            }

        }
        dict.__setitem__("__init__",new PyMethodDescr("__init__",PyList.class,-1,-1,new exposed___init__(null,null)));
        dict.__setitem__("__new__",new PyNewWrapper(PyList.class,"__new__",-1,-1) {
            public PyObject new_impl(boolean init,PyType subtype,PyObject[]args,String[]keywords) {
                PyList newobj;
                if (for_type==subtype) {
                    newobj=new PyList();
                    if (init)
                        newobj.list_init(args,keywords);
                } else {
                    newobj=new PyListDerived(subtype);
                }
                return newobj;
            }
        });
    }

    public PyList() {
        this(Py.EmptyObjects);
    }

    public PyList(PyType type) {
        super(type);
        length = 0;
        list = Py.EmptyObjects;
    }

    public PyList(Vector ilist) {
        this(new PyObject[ilist.size()]);
        for (int i=0; i<ilist.size(); i++) {
            list[i] = (PyObject)ilist.elementAt(i);
        }
    }

    public PyList(PyObject elements[]) {
        list = elements;
        length = elements.length;
    }

    public PyList(PyObject o) {
        this();
        PyObject iter = o.__iter__();
        for (PyObject item = null; (item = iter.__iternext__()) != null; ) {
            append(item);
        }
    }

    final void list_init(PyObject[] args,String[] kwds) {
        int nargs = args.length - kwds.length;
        if (nargs > 1) {
            throw PyBuiltinFunction.DefaultInfo.unexpectedCall(nargs,false,exposed_name,0,1);
        }
        if (nargs == 0) {
            return;
        }

        PyObject o = args[0];
        if (o instanceof PyList) {
            PyList p = (PyList) o.__getslice__(Py.None, Py.None, Py.One);
            this.list = p.list;
            this.length = p.length;
        } else if (o instanceof PyTuple) {
            PyTuple t = (PyTuple)o;
            PyObject[] a = new PyObject[t.__len__()];
            System.arraycopy(t.list, 0, a, 0, a.length);
            this.list = a;
            this.length = a.length;
        } else {
            PyObject iter = o.__iter__();
            for (PyObject item = null; (item = iter.__iternext__()) != null; ) {
                append(item);
            }
        }
    }

    public String safeRepr() throws PyIgnoreMethodTag {
        return "'list' object";
    }

    public int __len__() {
        return list___len__();
    }

    final int list___len__() {
        return length;
    }

    final PyObject list___eq__(PyObject o) {
        return super.__eq__(o);
    }

    final PyObject list___ne__(PyObject o) {
        return super.__ne__(o);
    }

    final boolean list___contains__(PyObject o) {
        return super.__contains__(o);
    }

    protected PyObject get(int i) {
        return list[i];
    }

    protected PyObject getslice(int start, int stop, int step) {
        if (step > 0 && stop < start)
            stop = start;
        int n = sliceLength(start, stop, step);
        PyObject[] newList = new PyObject[n];

        if (step == 1) {
            System.arraycopy(list, start, newList, 0, stop-start);
            return new PyList(newList);
        }
        int j = 0;
        for (int i=start; j<n; i+=step) {
            newList[j] = list[i];
            j++;
        }
        return new PyList(newList);
    }

    protected void del(int i) {
        length = length-1;
        System.arraycopy(list, i+1, list, i, length-i);
        list[length] = null;
    }

    protected void delRange(int start, int stop, int step) {
        if (step != 1)
            throw Py.ValueError("step size must be 1 for deleting "+
                                "list slice");

        System.arraycopy(list, stop, list, start, length-stop);
        int newLength = length-(stop-start);
        int oldLength = length;

        for(int i = newLength; i < oldLength; i++)
            list[i] = null;
        length = newLength;
    }

    protected void set(int i, PyObject value) {
        list[i] = value;
    }

    protected void setslice(int start, int stop, int step, PyObject value) {
        if (!(value instanceof PySequence))
            throw Py.TypeError("rhs of setslice must be a sequence");

        if (step != 1)
            throw Py.ValueError("step size must be 1 for setting list slice");
        if (stop < start)
            stop = start;

        PySequence seq = (PySequence)value;

        // Hack to make recursive setslices work correctly.
        // Could avoid the copy if we wanted to be more clever about
        // the code to do the moving

        int n = seq.__len__();
        int i;
        int length = this.length;
        int newLength = length-(stop-start)+n;

        if (newLength > length || newLength < length) {
            resize(newLength);
            System.arraycopy(list, stop, list, stop+(newLength-length),
                             length-stop);
            if (newLength < length) {
                for (i = newLength; i < length; i++)
                    list[i] = null;
            }
        }
//         else if (newLength < length) {
//             System.arraycopy(list, stop, list, stop+(newLength-length),
//                              length-stop);
//             this.length = newLength;
//         }

        PyObject[] otherList = null;

        if (value instanceof PyTuple)
            otherList = ((PyTuple)value).list;
        if (value instanceof PyList) {
            otherList = ((PyList)value).list;
            if (otherList == list)
                otherList = (PyObject[])otherList.clone();
        }
        if (otherList != null) {
            System.arraycopy(otherList, 0, list, start, n);
        }
        else {
            for(i=0; i<n; i++) {
                list[i+start] = seq.get(i);
            }
        }
    }

    protected PyObject repeat(int count) {
        int l = length;
        PyObject[] newList = new PyObject[l*count];
        for (int i=0; i<count; i++) {
            System.arraycopy(list, 0, newList, i*l, l);
        }
        return new PyList(newList);
    }

    public PyObject __imul__(PyObject o) {
        return list___imul__(o);
    }

    final PyObject list___imul__(PyObject o) {
        if (!(o instanceof PyInteger || o instanceof PyLong))
            throw Py.TypeError("can't multiply sequence to non-int");
        int l = length;
        int count = o.__int__().getValue();

        resize(l * count);

        for (int i=0; i<count; i++) {
            System.arraycopy(list, 0, list, i*l, l);
        }
        return this;
    }

    final PyObject list___mul__(PyObject o) {
        if (!(o instanceof PyInteger || o instanceof PyLong))
            throw Py.TypeError("can't multiply sequence to non-int");
        int count = o.__int__().getValue();
        return repeat(count);
    }

    final PyObject list___rmul__(PyObject o) {
        if (!(o instanceof PyInteger || o instanceof PyLong))
            throw Py.TypeError("can't multiply sequence to non-int");
        int count = o.__int__().getValue();
        return repeat(count);
    }

    public PyObject __add__(PyObject genericOther) {
        return list___add__(genericOther);
    }

    final PyObject list___add__(PyObject genericOther) {
        if (genericOther instanceof PyList) {
            PyList other = (PyList)genericOther;

            PyObject[] newList = new PyObject[length+other.length];
            System.arraycopy(list, 0, newList, 0, length);
            System.arraycopy(other.list, 0, newList, length, other.length);

            return new PyList(newList);
        } else {
            return null;
        }
    }

    public String toString() {
        return list_toString();
    }

    final String list_toString() {
        ThreadState ts = Py.getThreadState();
        if (!ts.enterRepr(this)) {
            return "[...]";
        }

        StringBuffer buf = new StringBuffer("[");
        for (int i=0; i<length-1; i++) {
            buf.append(((PyObject)list[i]).__repr__().toString());
            buf.append(", ");
        }
        if (length > 0)
            buf.append(((PyObject)list[length-1]).__repr__().toString());
        buf.append("]");

        ts.exitRepr(this);
        return buf.toString();
    }

    protected void resize(int n) {
        if (list.length < n) {
            PyObject[] newList = new PyObject[(int)(n*1.5)];
            System.arraycopy(list, 0, newList, 0, length);
            list = newList;
        }
        length = n;
    }

    /**
     * Add a single element to the end of list.
     *
     * @param o the element to add.
     */
    public void append(PyObject o) {
        list_append(o);
    }

    final void list_append(PyObject o) {
        resize(length+1);
        list[length-1] = o;
    }

    /**
     * Return the number elements in the list that equals the argument.
     *
     * @param o the argument to test for. Testing is done with
     *          the <code>==</code> operator.
     */
    public int count(PyObject o) {
        return list_count(o);
    }

    final int list_count(PyObject o) {
        int count = 0;
        int n = length;
        PyObject[] list = this.list;
        for (int i=0; i<n; i++) {
            if (list[i].equals(o))
                count++;
        }
        return count;
    }

    /**
     * return smallest index where an element in the list equals
     * the argument.
     *
     * @param o the argument to test for. Testing is done with
     *          the <code>==</code> operator.
     */
    public int index(PyObject o) {
        return list_index(o);
    }

    final int list_index(PyObject o) {
        return _index(o, "list.index(x): x not in list");
    }

    private int _index(PyObject o, String message) {
        int n = length;
        PyObject[] list = this.list;
        int i=0;
        for (; i<n; i++) {
            if (list[i].equals(o))
                break;
        }
        if (i == n)
            throw Py.ValueError(message);
        return i;
    }

    /**
     * Insert the argument element into the list at the specified
     * index.
     * <br>
     * Same as <code>s[index:index] = [o] if index &gt;= 0</code>.
     *
     * @param index the position where the element will be inserted.
     * @param o     the element to insert.
     */
    public void insert(int index, PyObject o) {
        list_insert(index, o);
    }

    final void list_insert(int index, PyObject o) {
        if (index < 0)
            index = 0;
        if (index > length)
            index = length;
        resize(length+1);
        System.arraycopy(list, index, list, index+1, length-index-1);
        list[index] = o;
    }

    /**
     * Remove the first occurence of the argument from the list.
     * The elements arecompared with the <code>==</code> operator.
     * <br>
     * Same as <code>del s[s.index(x)]</code>
     *
     * @param o the element to search for and remove.
     */
    public void remove(PyObject o) {
        list_remove(o);
    }

    final void list_remove(PyObject o) {
        del(_index(o, "list.remove(x): x not in list"));
    }

    /**
     * Reverses the items of s in place.
     * The reverse() methods modify the list in place for economy
     * of space when reversing a large list. It doesn't return the
     * reversed list to remind you of this side effect.
     */
    public void reverse() {
        list_reverse();
    }

    final void list_reverse() {
        PyObject tmp;
        int n = length;
        PyObject[] list = this.list;
        int j = n-1;
        for (int i=0; i<n/2; i++, j--) {
            tmp = list[i];
            list[i] = list[j];
            list[j] = tmp;
        }
    }

    /**
     * Removes and return the last element in the list.
     */
    public PyObject pop() {
        return list_pop();
    }

    final PyObject list_pop() {
        return pop(-1);
    }

    /**
     * Removes and return the <code>n</code> indexed element in the
     * list.
     *
     * @param n the index of the element to remove and return.
     */
    public PyObject pop(int n) {
        return list_pop(n);
    }

    final PyObject list_pop(int n) {
        if (length==0) {
            throw Py.IndexError("pop from empty list");
        }
        if (n < 0)
            n += length;
        if (n < 0 || n >= length)
            throw Py.IndexError("pop index out of range");
        PyObject v = list[n];

        setslice(n, n+1, 1, Py.EmptyTuple);
        return v;
    }

    /**
     * Append the elements in the argument sequence to the end of the list.
     * <br>
     * Same as <code>s[len(s):len(s)] = o</code>.
     *
     * @param o the sequence of items to append to the list.
     */
    public void extend(PyObject o) {
        list_extend(o);
    }

    final void list_extend(PyObject o) {
        setslice(length, length, 1, o);
    }

    public PyObject __iadd__(PyObject o) {
        return list___iadd__(o);
    }

    final PyObject list___iadd__(PyObject o) {
        extend(fastSequence(o, "argument to += must be a sequence"));
        return this;
    }

    /**
     * Sort the items of the list in place. The compare argument is a
     * function of two arguments (list items) which should return
     * -1, 0 or 1 depending on whether the first argument is
     * considered smaller than, equal to, or larger than the second
     * argument. Note that this slows the sorting process down
     * considerably; e.g. to sort a list in reverse order it is much
     * faster to use calls to the methods sort() and reverse() than
     * to use the built-in function sort() with a comparison function
     * that reverses the ordering of the elements.
     *
     * @param compare the comparison function.
     */
    public synchronized void sort(PyObject compare) {
        list_sort(compare);
    }

    final synchronized void list_sort(PyObject compare) {
        MergeState ms = new MergeState(list, length, compare);
        ms.sort();
    }

    /**
     * Sort the items of the list in place. Items is compared with the
     * normal relative comparison operators.
     */
    public void sort() {
        list_sort();
    }

    final void list_sort() {
        list_sort(null);
    }

    public int hashCode() {
        return list_hashCode();
    }

    final int list_hashCode() {
        throw Py.TypeError("unhashable type");
    }
}
