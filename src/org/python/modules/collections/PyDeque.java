/**
 *   PyDeque.java - This class implements the functionalities of Deque data structure.
 *   Deques are a generalization of stacks and queues (the name is pronounced “deck” 
 *   and is short for “double-ended queue”). Deques support thread-safe, memory 
 *   efficient appends and pops from either side of the deque with approximately the 
 *   same O(1) performance in either direction.
 *   
 *   Though list objects support similar operations, they are optimized for fast 
 *   fixed-length operations and incur O(n) memory movement costs for pop(0) and 
 *   insert(0, v) operations which change both the size and position of the underlying
 *   data representation.
 *   
 *   collections.deque([iterable]) - returns a new deque object initialized left-to-right
 *   (using append()) with data from iterable. If iterable is not specified, the new 
 *   deque is empty.
 *    
 *   @author    Mehendran T (mehendran@gmail.com)
 *              Novell Software Development (I) Pvt. Ltd              
 *   @created   Mon 10-Sep-2007 19:54:27
 */
package org.python.modules.collections;

import org.python.core.PyIterator;
import org.python.core.PyObject;
import org.python.core.PySequenceIter;
import org.python.core.PyTuple;
import org.python.core.PyType;
import org.python.core.Py;
import org.python.core.PyException;
import org.python.core.PyInteger;
import org.python.core.PyLong;
import org.python.core.PyNewWrapper;
import org.python.core.PyMethodDescr;
import org.python.core.PyBuiltinFunction;
import org.python.core.PyBuiltinMethod;
import org.python.core.PyBuiltinMethodNarrow;
import org.python.core.PyString;
import org.python.core.ThreadState;

public class PyDeque extends PyObject {

    //~ BEGIN GENERATED REGION -- DO NOT EDIT SEE gexpose.py
    /* type info */

    public static final String exposed_name="deque";

    public static void typeSetup(PyObject dict,PyType.Newstyle marker) {
        class exposed___eq__ extends PyBuiltinMethodNarrow {

            exposed___eq__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___eq__(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=((PyDeque)self).deque___eq__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__eq__",new PyMethodDescr("__eq__",PyDeque.class,1,1,new exposed___eq__(null,null)));
        class exposed___ne__ extends PyBuiltinMethodNarrow {

            exposed___ne__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___ne__(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=((PyDeque)self).deque___ne__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__ne__",new PyMethodDescr("__ne__",PyDeque.class,1,1,new exposed___ne__(null,null)));
        class exposed___lt__ extends PyBuiltinMethodNarrow {

            exposed___lt__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___lt__(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=((PyDeque)self).deque___lt__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__lt__",new PyMethodDescr("__lt__",PyDeque.class,1,1,new exposed___lt__(null,null)));
        class exposed___le__ extends PyBuiltinMethodNarrow {

            exposed___le__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___le__(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=((PyDeque)self).deque___le__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__le__",new PyMethodDescr("__le__",PyDeque.class,1,1,new exposed___le__(null,null)));
        class exposed___gt__ extends PyBuiltinMethodNarrow {

            exposed___gt__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___gt__(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=((PyDeque)self).deque___gt__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__gt__",new PyMethodDescr("__gt__",PyDeque.class,1,1,new exposed___gt__(null,null)));
        class exposed___ge__ extends PyBuiltinMethodNarrow {

            exposed___ge__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___ge__(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                PyObject ret=((PyDeque)self).deque___ge__(arg0);
                if (ret==null)
                    return Py.NotImplemented;
                return ret;
            }

        }
        dict.__setitem__("__ge__",new PyMethodDescr("__ge__",PyDeque.class,1,1,new exposed___ge__(null,null)));
        class exposed_append extends PyBuiltinMethodNarrow {

            exposed_append(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_append(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                ((PyDeque)self).deque_append(arg0);
                return Py.None;
            }

        }
        dict.__setitem__("append",new PyMethodDescr("append",PyDeque.class,1,1,new exposed_append(null,null)));
        class exposed_appendleft extends PyBuiltinMethodNarrow {

            exposed_appendleft(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_appendleft(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                ((PyDeque)self).deque_appendleft(arg0);
                return Py.None;
            }

        }
        dict.__setitem__("appendleft",new PyMethodDescr("appendleft",PyDeque.class,1,1,new exposed_appendleft(null,null)));
        class exposed_pop extends PyBuiltinMethodNarrow {

            exposed_pop(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_pop(self,info);
            }

            public PyObject __call__() {
                return((PyDeque)self).deque_pop();
            }

        }
        dict.__setitem__("pop",new PyMethodDescr("pop",PyDeque.class,0,0,new exposed_pop(null,null)));
        class exposed_popleft extends PyBuiltinMethodNarrow {

            exposed_popleft(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_popleft(self,info);
            }

            public PyObject __call__() {
                return((PyDeque)self).deque_popleft();
            }

        }
        dict.__setitem__("popleft",new PyMethodDescr("popleft",PyDeque.class,0,0,new exposed_popleft(null,null)));
        class exposed_extend extends PyBuiltinMethodNarrow {

            exposed_extend(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_extend(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                ((PyDeque)self).deque_extend(arg0);
                return Py.None;
            }

        }
        dict.__setitem__("extend",new PyMethodDescr("extend",PyDeque.class,1,1,new exposed_extend(null,null)));
        class exposed_extendleft extends PyBuiltinMethodNarrow {

            exposed_extendleft(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_extendleft(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                ((PyDeque)self).deque_extendleft(arg0);
                return Py.None;
            }

        }
        dict.__setitem__("extendleft",new PyMethodDescr("extendleft",PyDeque.class,1,1,new exposed_extendleft(null,null)));
        class exposed_remove extends PyBuiltinMethodNarrow {

            exposed_remove(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_remove(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                ((PyDeque)self).deque_remove(arg0);
                return Py.None;
            }

        }
        dict.__setitem__("remove",new PyMethodDescr("remove",PyDeque.class,1,1,new exposed_remove(null,null)));
        class exposed_clear extends PyBuiltinMethodNarrow {

            exposed_clear(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_clear(self,info);
            }

            public PyObject __call__() {
                ((PyDeque)self).deque_clear();
                return Py.None;
            }

        }
        dict.__setitem__("clear",new PyMethodDescr("clear",PyDeque.class,0,0,new exposed_clear(null,null)));
        class exposed_rotate extends PyBuiltinMethodNarrow {

            exposed_rotate(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed_rotate(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                ((PyDeque)self).deque_rotate(arg0);
                return Py.None;
            }

            public PyObject __call__() {
                ((PyDeque)self).deque_rotate();
                return Py.None;
            }

        }
        dict.__setitem__("rotate",new PyMethodDescr("rotate",PyDeque.class,0,1,new exposed_rotate(null,null)));
        class exposed___getitem__ extends PyBuiltinMethodNarrow {

            exposed___getitem__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___getitem__(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                return((PyDeque)self).deque___getitem__(arg0);
            }

        }
        dict.__setitem__("__getitem__",new PyMethodDescr("__getitem__",PyDeque.class,1,1,new exposed___getitem__(null,null)));
        class exposed___setitem__ extends PyBuiltinMethodNarrow {

            exposed___setitem__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___setitem__(self,info);
            }

            public PyObject __call__(PyObject arg0,PyObject arg1) {
                ((PyDeque)self).deque___setitem__(arg0,arg1);
                return Py.None;
            }

        }
        dict.__setitem__("__setitem__",new PyMethodDescr("__setitem__",PyDeque.class,2,2,new exposed___setitem__(null,null)));
        class exposed___delitem__ extends PyBuiltinMethodNarrow {

            exposed___delitem__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___delitem__(self,info);
            }

            public PyObject __call__(PyObject arg0) {
                ((PyDeque)self).deque___delitem__(arg0);
                return Py.None;
            }

        }
        dict.__setitem__("__delitem__",new PyMethodDescr("__delitem__",PyDeque.class,1,1,new exposed___delitem__(null,null)));
        class exposed___len__ extends PyBuiltinMethodNarrow {

            exposed___len__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___len__(self,info);
            }

            public PyObject __call__() {
                return Py.newInteger(((PyDeque)self).deque___len__());
            }

        }
        dict.__setitem__("__len__",new PyMethodDescr("__len__",PyDeque.class,0,0,new exposed___len__(null,null)));
        class exposed___repr__ extends PyBuiltinMethodNarrow {

            exposed___repr__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___repr__(self,info);
            }

            public PyObject __call__() {
                return new PyString(((PyDeque)self).deque_toString());
            }

        }
        dict.__setitem__("__repr__",new PyMethodDescr("__repr__",PyDeque.class,0,0,new exposed___repr__(null,null)));
        class exposed___iter__ extends PyBuiltinMethodNarrow {

            exposed___iter__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___iter__(self,info);
            }

            public PyObject __call__() {
                return((PyDeque)self).deque___iter__();
            }

        }
        dict.__setitem__("__iter__",new PyMethodDescr("__iter__",PyDeque.class,0,0,new exposed___iter__(null,null)));
        class exposed___hash__ extends PyBuiltinMethodNarrow {

            exposed___hash__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___hash__(self,info);
            }

            public PyObject __call__() {
                return new PyInteger(((PyDeque)self).deque_hashCode());
            }

        }
        dict.__setitem__("__hash__",new PyMethodDescr("__hash__",PyDeque.class,0,0,new exposed___hash__(null,null)));
        class exposed___reduce__ extends PyBuiltinMethodNarrow {

            exposed___reduce__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___reduce__(self,info);
            }

            public PyObject __call__() {
                return((PyDeque)self).deque___reduce__();
            }

        }
        dict.__setitem__("__reduce__",new PyMethodDescr("__reduce__",PyDeque.class,0,0,new exposed___reduce__(null,null)));
        class exposed___copy__ extends PyBuiltinMethodNarrow {

            exposed___copy__(PyObject self,PyBuiltinFunction.Info info) {
                super(self,info);
            }

            public PyBuiltinFunction bind(PyObject self) {
                return new exposed___copy__(self,info);
            }

            public PyObject __call__() {
                return((PyDeque)self).deque___copy__();
            }

        }
        dict.__setitem__("__copy__",new PyMethodDescr("__copy__",PyDeque.class,0,0,new exposed___copy__(null,null)));
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
                ((PyDeque)self).deque_init(args,keywords);
                return Py.None;
            }

        }
        dict.__setitem__("__init__",new PyMethodDescr("__init__",PyDeque.class,-1,-1,new exposed___init__(null,null)));
        dict.__setitem__("__new__",new PyNewWrapper(PyDeque.class,"__new__",-1,-1) {

                                                                                       public PyObject new_impl(boolean init,PyType subtype,PyObject[]args,String[]keywords) {
                                                                                           PyDeque newobj;
                                                                                           if (for_type==subtype) {
                                                                                               newobj=new PyDeque();
                                                                                               if (init)
                                                                                                   newobj.deque_init(args,keywords);
                                                                                           } else {
                                                                                               newobj=new PyDequeDerived(subtype);
                                                                                           }
                                                                                           return newobj;
                                                                                       }

                                                                                   });
    }
    //~ END GENERATED REGION -- DO NOT EDIT SEE gexpose.py

    private static final PyType DEQUE_TYPE = PyType.fromClass(PyDeque.class);
    private int size = 0;
    private Node header = new Node(null, null, null); 
    
    public PyDeque() {
        this(DEQUE_TYPE);
    }

    public PyDeque(PyType subType) {
        super(subType);
        header.left = header.right = header;
    }

    final void deque_init(PyObject[] args, String[] kwds) {
        if (kwds.length > 0) {
            throw Py.TypeError("deque() does not take keyword arguments");
        }
        int nargs = args.length;
        if (nargs > 1) {
            throw PyBuiltinFunction.DefaultInfo.unexpectedCall(nargs, false, exposed_name, 0, 1);
        } 
        if (nargs == 0) {
            return;
        }
        PyObject data = args[0];

        if (data.__findattr__("__iter__") != null) {
            deque_extend(data);
        } else {
            try {
                deque_extend(new PySequenceIter(data));
            } catch (PyException e) {
                if(Py.matchException(e, Py.AttributeError)) { 
                    throw Py.TypeError("'" + data.getType().fastGetName() + 
                    "' object is not iterable");
                }
            }
        }
    }

    /**
     * Add obj to the right side of the deque.
     */	
    final void deque_append(PyObject obj) {
        addBefore(obj, header);		
    }

    /**
     * Add obj to the left side of the deque.
     */
    final void deque_appendleft(PyObject obj) {		
        addBefore(obj, header.right);
    }

    private Node addBefore(PyObject obj, Node node) {
        Node newNode = new Node(obj, node, node.left);
        newNode.left.right = newNode;
        newNode.right.left = newNode;
        size++;
        return newNode;
    }

    /**
     * Remove all elements from the deque leaving it with length 0.
     */
    final void deque_clear() {
        Node node = header.right;
        while (node != header) {
            Node right = node.right;
            node.left = null;
            node.right = null;
            node.data = null;
            node = right;
        }
        header.right = header.left = header;
        size = 0;
    }

    /**
     * Extend the right side of the deque by appending elements from the 
     * iterable argument.
     */
    final void deque_extend(PyObject iterable) {
        PyObject iter = iterable.__iter__();
        for (PyObject tmp = null; (tmp = iter.__iternext__()) != null; ) {
            deque_append(tmp);			
        } 
    }

    /**
     * Extend the left side of the deque by appending elements from iterable. 
     * Note, the series of left appends results in reversing the order of 
     * elements in the iterable argument.
     */
    final void deque_extendleft(PyObject iterable) {		
        PyObject iter = iterable.__iter__();
        for (PyObject tmp = null; (tmp = iter.__iternext__()) != null; ) { 
            deque_appendleft(tmp);
        }
    }

    /**
     * Remove and return an element from the right side of the deque. If no 
     * elements are present, raises an IndexError.
     */
    final PyObject deque_pop() {
        return removeNode(header.left);
    }

    /**
     * Remove and return an element from the left side of the deque. If no 
     * elements are present, raises an IndexError.
     */
    final PyObject deque_popleft() {
        return removeNode(header.right);
    }

    private PyObject removeNode(Node node) {
        if (node == header) {
            throw Py.IndexError("pop from an empty deque");
        }
        PyObject obj = node.data;
        node.left.right = node.right;
        node.right.left = node.left;
        node.right = null;
        node.left = null;
        node.data = null;
        size--;
        return obj;
    } 

    /**
     * Removed the first occurrence of value. If not found, raises a 
     * ValueError.
     */
    final PyObject deque_remove(PyObject value) {
        int n = size;
        Node tmp = header.right;
        boolean match = false;
        for (int i = 0; i < n; i++) {
            if (tmp.data.equals(value)) { 
                match = true;
            }
            if (n != size) { 
                throw Py.IndexError("deque mutated during remove().");
            }
            if (match) {
                return removeNode(tmp);
            }
            tmp = tmp.right;
        }
        throw Py.ValueError("deque.remove(x): x not in deque");
    }

    /**
     * Rotate the deque n steps to the right. If n is negative, rotate to the 
     * left. Rotating one step to the right is equivalent to: d.appendleft(d.pop()).
     */
    final void deque_rotate(PyObject stepsobj) {
        if (size == 0) {
            return;
        }

        int n = 0;
        if (stepsobj instanceof PyInteger || stepsobj instanceof PyLong) {
            n = ((PyInteger)stepsobj.__int__()).getValue();
        } else {
            throw Py.TypeError("an integer is required");
        }

        int halfsize = (size + 1) >> 1; 
        if (n > halfsize || n < -halfsize) {
            n %= size;
            if (n > halfsize) { 
                n -= size;
            } else if (n < -halfsize) {
                n += size;
            }
        }

        //rotate right 
        for (int i = 0; i < n; i++) {
            deque_appendleft(deque_pop());
        } 
        //rotate left
        for (int i = 0; i > n; i--) {
            deque_append(deque_popleft());
        } 
    }

    /**
     * Rotate the deque one step to the right.  
     * Rotating one step to the right is equivalent to: d.appendleft(d.pop()).
     */
    final void deque_rotate() {
        deque_rotate(Py.One);
    }

    public String toString() {
        return deque_toString();
    }

    final String deque_toString() {
        ThreadState ts = Py.getThreadState();
        if (!ts.enterRepr(this)) { 
            return "[...]";
        }
        String name = getType().fastGetName();
        StringBuffer buf = new StringBuffer(name).append("([");
        for (Node tmp = header.right; tmp != header; tmp = tmp.right) {
            buf.append(tmp.data.__repr__().toString());
            if (tmp.right != header) {
                buf.append(", ");
            }
        }
        buf.append("])");
        ts.exitRepr(this);
        return buf.toString();
    }

    public int __len__() {
        return deque___len__();
    }

    final int deque___len__() {
        return size;
    }

    public PyObject __finditem__(PyObject key) {
        try {
            return deque___getitem__(key);
        } catch (PyException pe) {
            if (Py.matchException(pe, Py.KeyError)) {
                return null;
            }
            throw pe;
        }
    }

    final PyObject deque___getitem__(PyObject index) {
        return getNode(index).data;				
    }	

    public void __setitem__(PyObject index, PyObject value) {
        deque___setitem__(index, value);
    }

    final void deque___setitem__(PyObject index, PyObject value) {
        Node node = getNode(index).right;
        removeNode(node.left);
        addBefore(value, node);
    }	

    public void __delitem__(PyObject key) {
        deque___delitem__(key);
    }

    final void deque___delitem__(PyObject key) {
        removeNode(getNode(key));
    }

    private Node getNode(PyObject index) {
        int pos = 0;
        if (index instanceof PyInteger || index instanceof PyLong) {
            pos = ((PyInteger)index.__int__()).getValue();
        } else {
            throw Py.TypeError("an integer is required");
        }

        if (pos < 0) {
            pos += size;
        }
        if (pos < 0 || pos >= size) {
            throw Py.IndexError("index out of range: " + index);
        }

        Node tmp = header;
        if (pos < (size >> 1)) {
            for (int i = 0; i <= pos; i++) {
                tmp = tmp.right;
            }
        } else {
            for (int i = size - 1; i >= pos; i--) {
                tmp = tmp.left;
            }
        }
        return tmp;
    }

    public PyObject __iter__() {
        return deque___iter__();
    }

    final PyObject deque___iter__() {
        return new PyDequeIter();
    }

    public synchronized PyObject __eq__(PyObject o) {
        return deque___eq__(o);
    }

    final synchronized PyObject deque___eq__(PyObject o) {
        if (!(getType() == o.getType()) && !(getType().isSubType(o.getType()))) {
            return null;
        }
        int tl = __len__();
        int ol = o.__len__();
        if (tl != ol) {
            return Py.False;
        }
        int i = cmp(this, tl, o, ol);
        return (i < 0) ? Py.True : Py.False;
    }

    public synchronized PyObject __ne__(PyObject o) {
        return deque___ne__(o);
    }

    final synchronized PyObject deque___ne__(PyObject o) {
        if (!(getType() == o.getType()) && !(getType().isSubType(o.getType()))) {
            return null;
        }
        int tl = __len__();
        int ol = o.__len__();
        if (tl != ol) {
            return Py.True;
        }
        int i = cmp(this, tl, o, ol);
        return (i < 0) ? Py.False : Py.True;
    }

    public synchronized PyObject __lt__(PyObject o) {
        return deque___lt__(o);
    }

    final synchronized PyObject deque___lt__(PyObject o) {
        if (!(getType() == o.getType()) && !(getType().isSubType(o.getType()))) {
            return null;
        }
        int i = cmp(this, -1, o, -1);
        if (i < 0) {
            return (i == -1) ? Py.True : Py.False;
        }
        return __finditem__(i)._lt(o.__finditem__(i));
    }

    public synchronized PyObject __le__(PyObject o) {
        return deque___le__(o);
    }

    final synchronized PyObject deque___le__(PyObject o) {
        if (!(getType() == o.getType()) && !(getType().isSubType(o.getType()))) {
            return null;
        }
        int i = cmp(this, -1, o, -1);
        if (i < 0) {
            return (i == -1 || i == -2) ? Py.True : Py.False;
        }
        return __finditem__(i)._le(o.__finditem__(i));
    }

    public synchronized PyObject __gt__(PyObject o) {
        return deque___gt__(o);
    }

    final synchronized PyObject deque___gt__(PyObject o) {
        if (!(getType() == o.getType()) && !(getType().isSubType(o.getType()))) {
            return null;
        }
        int i = cmp(this, -1, o, -1);
        if (i < 0) {
            return (i == -3) ? Py.True : Py.False;
        }
        return __finditem__(i)._gt(o.__finditem__(i));
    }

    public synchronized PyObject __ge__(PyObject o) {
        return deque___ge__(o);
    }

    final synchronized PyObject deque___ge__(PyObject o) {
        if (!(getType() == o.getType()) && !(getType().isSubType(o.getType()))) {
            return null;
        }
        int i = cmp(this, -1, o, -1);
        if (i < 0) {
            return (i == -3 || i == -2) ? Py.True : Py.False;
        }
        return __finditem__(i)._ge(o.__finditem__(i));
    }

    // Return value >= 0 is the index where the sequences differs.
    // -1: reached the end of o1 without a difference
    // -2: reached the end of both seqeunces without a difference
    // -3: reached the end of o2 without a difference
    protected static int cmp(PyObject o1, int ol1, PyObject o2, int ol2) {
        if (ol1 < 0) {
            ol1 = o1.__len__();
        }
        if (ol2 < 0) {
            ol2 = o2.__len__();
        }
        int i = 0;
        for ( ; i < ol1 && i < ol2; i++) {
            if (!o1.__getitem__(i)._eq(o2.__getitem__(i)).__nonzero__()) {
                return i;
            }
        }
        if (ol1 == ol2) {
            return -2;
        }
        return (ol1 < ol2) ? -1 : -3;
    }

    public int hashCode() {
        return deque_hashCode();
    }

    final int deque_hashCode() {
        throw Py.TypeError("deque objects are unhashable");
    }

    public PyObject __reduce__() {
        return deque___reduce__();
    }

    final PyObject deque___reduce__() {
        return new PyTuple(new PyObject [] {
                this.getType(),
                Py.EmptyTuple,
                Py.None,
                this.deque___iter__()
        });
    }

    final PyObject deque___copy__() {
        PyDeque pd = (PyDeque)this.getType().__call__();	
        pd.deque_extend(this);
        return pd;
    }

    private static class Node {
        private Node left;
        private Node right;
        private PyObject data;

        Node(PyObject data, Node right, Node left) {
            this.data = data;
            this.right = right;
            this.left = left;
        }
    }

    private class PyDequeIter extends PyIterator {

        private Node lastReturned = header;
        private int itersize;

        public PyDequeIter() {
            itersize = size;
        }

        public PyObject __iternext__() {        	   		
            if (itersize != size) { 
                throw Py.RuntimeError("deque changed size during iteration");
            }
            if (lastReturned.right != header) {
                lastReturned = lastReturned.right;
                return lastReturned.data;
            }
            return null;
        }
    }
}
