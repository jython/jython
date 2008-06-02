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
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedType;
import org.python.expose.MethodType;

/**
 * PyDeque - This class implements the functionalities of Deque data structure. Deques are a
 * generalization of stacks and queues (the name is pronounced 'deck' and is short for 'double-ended
 * queue'). Deques support thread-safe, memory efficient appends and pops from either side of the
 * deque with approximately the same O(1) performance in either direction.
 * 
 * Though list objects support similar operations, they are optimized for fast fixed-length
 * operations and incur O(n) memory movement costs for pop(0) and insert(0, v) operations which
 * change both the size and position of the underlying data representation.
 * 
 * collections.deque([iterable]) - returns a new deque object initialized left-to-right (using
 * append()) with data from iterable. If iterable is not specified, the new deque is empty.
 */
@ExposedType(name = "collections.deque")
public class PyDeque extends PyObject {

    public static final PyType TYPE = PyType.fromClass(PyDeque.class);

    private int size = 0;

    private Node header = new Node(null, null, null); 
    
    public PyDeque() {
        this(TYPE);
    }

    public PyDeque(PyType subType) {
        super(subType);
        header.left = header.right = header;
    }

    @ExposedNew
    @ExposedMethod
    final void deque___init__(PyObject[] args, String[] kwds) {
        if (kwds.length > 0) {
            throw Py.TypeError("deque() does not take keyword arguments");
        }
        int nargs = args.length;
        if (nargs > 1) {
            throw PyBuiltinFunction.DefaultInfo.unexpectedCall(nargs, false, "deque", 0, 1);
        } 
        if (nargs == 0) {
            return;
        }
        deque_extend(args[0]);
    }

    /**
     * Add obj to the right side of the deque.
     */	
    @ExposedMethod
    final void deque_append(PyObject obj) {
        addBefore(obj, header);		
    }

    /**
     * Add obj to the left side of the deque.
     */
    @ExposedMethod
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
    @ExposedMethod
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
    @ExposedMethod
    final void deque_extend(PyObject iterable) {
        for (PyObject item : iterable.asIterable()) {
            deque_append(item);			
        } 
    }

    /**
     * Extend the left side of the deque by appending elements from iterable. 
     * Note, the series of left appends results in reversing the order of 
     * elements in the iterable argument.
     */
    @ExposedMethod
    final void deque_extendleft(PyObject iterable) {
        for (PyObject item : iterable.asIterable()) {
            deque_appendleft(item);
        }
    }

    /**
     * Remove and return an element from the right side of the deque. If no 
     * elements are present, raises an IndexError.
     */
    @ExposedMethod
    final PyObject deque_pop() {
        return removeNode(header.left);
    }

    /**
     * Remove and return an element from the left side of the deque. If no 
     * elements are present, raises an IndexError.
     */
    @ExposedMethod
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
    @ExposedMethod
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
    @ExposedMethod(defaults = {"1"})
    final void deque_rotate(int steps) {
        if (size == 0) {
            return;
        }

        int halfsize = (size + 1) >> 1; 
        if (steps > halfsize || steps < -halfsize) {
            steps %= size;
            if (steps > halfsize) { 
                steps -= size;
            } else if (steps < -halfsize) {
                steps += size;
            }
        }

        //rotate right 
        for (int i = 0; i < steps; i++) {
            deque_appendleft(deque_pop());
        } 
        //rotate left
        for (int i = 0; i > steps; i--) {
            deque_append(deque_popleft());
        } 
    }

    public String toString() {
        return deque_toString();
    }

    @ExposedMethod(names = "__repr__")
    final String deque_toString() {
        ThreadState ts = Py.getThreadState();
        if (!ts.enterRepr(this)) { 
            return "[...]";
        }
        StringBuffer buf = new StringBuffer("deque").append("([");
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

    @ExposedMethod
    final int deque___len__() {
        return size;
    }

    public boolean __nonzero__() {
        return deque___nonzero__();
    }

    @ExposedMethod
    final boolean deque___nonzero__() {
        return size != 0;
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

    @ExposedMethod
    final PyObject deque___getitem__(PyObject index) {
        return getNode(index).data;				
    }	

    public void __setitem__(PyObject index, PyObject value) {
        deque___setitem__(index, value);
    }

    @ExposedMethod
    final void deque___setitem__(PyObject index, PyObject value) {
        Node node = getNode(index).right;
        removeNode(node.left);
        addBefore(value, node);
    }	

    public void __delitem__(PyObject key) {
        deque___delitem__(key);
    }

    @ExposedMethod
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

    @ExposedMethod
    final PyObject deque___iter__() {
        return new PyDequeIter();
    }

    public synchronized PyObject __eq__(PyObject o) {
        return deque___eq__(o);
    }

    @ExposedMethod(type = MethodType.BINARY)
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

    @ExposedMethod(type = MethodType.BINARY)
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

    @ExposedMethod(type = MethodType.BINARY)
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

    @ExposedMethod(type = MethodType.BINARY)
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

    @ExposedMethod(type = MethodType.BINARY)
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

    @ExposedMethod(type = MethodType.BINARY)
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
        for (int i = 0 ; i < ol1 && i < ol2; i++) {
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

    @ExposedMethod(names = "__hash__")
    final int deque_hashCode() {
        throw Py.TypeError("deque objects are unhashable");
    }

    public PyObject __reduce__() {
        return deque___reduce__();
    }

    @ExposedMethod
    final PyObject deque___reduce__() {
        PyObject dict = getDict();
        if (dict == null) {
            dict = Py.None;
        }
        return new PyTuple(getType(), Py.EmptyTuple, dict, __iter__());
    }

    @ExposedMethod
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
