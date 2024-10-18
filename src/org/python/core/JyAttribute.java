package org.python.core;

import java.io.Serializable;

/**
 * Manages a linked list of general purpose Object-attributes that
 * can be attached to arbitrary {@link org.python.core.PyObject}s.
 * This method replaces the formerly used method of maintaining weak
 * hash-maps ({@link java.util.WeakHashMap}) for such cases.
 * These weak hash-maps were used to map
 * {@code PyObject}s to such attributes, for instance
 * to attach
 * {@link org.python.modules._weakref.GlobalRef}-objects in the
 * {@link org.python.modules._weakref.WeakrefModule}.
 * <p>
 * Attributes attached via the weak hash-map-method break, if the
 * {@code PyObject} is resurrected in its finalizer. The
 * {@code JyAttribute}-method is resurrection-safe.
 * <p>
 * To reduce memory footprint of {@code PyObject}s, the fields for
 * {@link org.python.core.finalization.FinalizeTrigger}s and
 * {@code javaProxy} are included in the list; {@code javaProxy} always
 * on top so there is no speed-regression,
 * {@link org.python.core.finalization.FinalizeTrigger} on bottom,
 * as it is usually never accessed.
 */
public abstract class JyAttribute implements Serializable {
    /* ordered by priority; indices >= 0 indicate transient attributes.
       since it is intended for rare use, 128 indices for ordinary and
       transient attributes each should be enough in foreseeable future.
       If needed, it would be trivial to change format to short.
    */
    public static final byte JAVA_PROXY_ATTR = Byte.MIN_VALUE;

    /**
     * Stores list of weak references linking to this {@code PyObject}.
     * This list is weakref-based, so it does not keep the
     * weakrefs alive. This is the only way to find out which
     * weakrefs (i.e. {@link org.python.modules._weakref.AbstractReference})
     * linked to the object after a resurrection. A weak
     * hash-map-based approach for this purpose would break on
     * resurrection.
     */
    public static final byte WEAK_REF_ATTR = 0; //first transient

    /**
     * Reserved for use by <a href="https://jyni.12hp.de" target="_blank">JyNI</a>.
     */
    public static final byte JYNI_HANDLE_ATTR = 1;

    /**
     * Allows the id of a {@link org.python.core.PyObject} to persist
     * resurrection of that object.
     */
    public static final byte PY_ID_ATTR = 2;

    /**
     * Holds the current thread for an
     * {@link org.python.modules._weakref.AbstractReference}
     * while referent-retrieval is pending due to a potentially
     * restored-by-resurrection weak reference. After the
     * restore has happened or the clear was confirmed, the
     * thread is interrupted and the attribute is cleared.
     */
    public static final byte WEAKREF_PENDING_GET_ATTR = 3;

    /** Only used internally by {@link Py#javaPyClass(PyObject, Class)} */
    public static final byte PYCLASS_PY2JY_CACHE_ATTR = 4;

    /**
     * Used by {@link org.python.modules.gc}-module to mark cyclic
     * trash. Searching for cyclic trash is usually not required
     * by Jython. It is only done if gc-features are enabled that
     * mimic CPython behavior.
     */
    public static final byte GC_CYCLE_MARK_ATTR = 5;

    /**
     * Used by {@link org.python.modules.gc}-module to mark
     * finalizable objects that might have been resurrected
     * during a delayed finalization process.
     */
    public static final byte GC_DELAYED_FINALIZE_CRITICAL_MARK_ATTR = 6;

    public static final byte FINALIZE_TRIGGER_ATTR = Byte.MAX_VALUE;
    private static volatile byte nonBuiltinAttrTypeOffset = Byte.MIN_VALUE+1;
    private static volatile byte nonBuiltinTransientAttrTypeOffset = 7;

    /**
     * Reserves and returns a new non-transient attr type for custom use.
     *
     * @return a non-transient attr type for custom use
     */
    public static synchronized byte reserveCustomAttrType() {
        if (nonBuiltinAttrTypeOffset == 0) {
            throw new IllegalStateException("No more attr types available.");
        }
        return nonBuiltinAttrTypeOffset++;
    }

    /**
     * Reserves and returns a new transient attr type for custom use.
     *
     * @return a transient attr type for custom use
     */
    public static synchronized byte reserveTransientCustomAttrType() {
        if (nonBuiltinTransientAttrTypeOffset == Byte.MAX_VALUE) {
            throw new IllegalStateException("No more transient attr types available.");
        }
        return nonBuiltinTransientAttrTypeOffset++;
    }

    final byte attr_type;

    static class AttributeLink extends JyAttribute {
        volatile JyAttribute next;
        volatile Object value;

        protected AttributeLink(byte attr_type, Object value) {
            super(attr_type);
            this.value = value;
        }

        @Override
        protected JyAttribute getNext() {
            return next;
        }

        @Override
        protected void setNext(JyAttribute next) {
            this.next = next;
        }

        @Override
        protected Object getValue() {
            return value;
        }

        @Override
        protected void setValue(Object value) {
            this.value = value;
        }
    }

    static class TransientAttributeLink extends JyAttribute {
        volatile transient JyAttribute next;
        volatile transient Object value;

        protected TransientAttributeLink(byte attr_type, Object value) {
            super(attr_type);
            this.value = value;
        }

        @Override
        protected JyAttribute getNext() {
            return next;
        }

        @Override
        protected void setNext(JyAttribute next) {
            this.next = next;
        }

        @Override
        protected Object getValue() {
            return value;
        }

        @Override
        protected void setValue(Object value) {
            this.value = value;
        }
    }

    protected JyAttribute(byte attr_type) {
        this.attr_type = attr_type;
    }

    protected abstract JyAttribute getNext();
    protected abstract void setNext(JyAttribute next);
    protected abstract Object getValue();
    protected abstract void setValue(Object value);

    /**
     * Checks whether the given {@link org.python.core.PyObject}
     * has an attribute of the given type attached.
     */
    public static boolean hasAttr(PyObject ob, byte attr_type) {
        return getAttr(ob, attr_type) != null;
    }

    /**
     * Retrieves the attribute of the given type from the given
     * {@link org.python.core.PyObject}.
     * If no attribute of the given type is attached, null is returned.
     * 
     * This class is generally thread safe, but this method is not synchronized with
     * concurrent write access to avoid performance issues for multithreaded scenarios.
     * The trade-off for that choice is that in case of concurrent write access, it is
     * unspecified whether this method returns according to the old state or the new state.
     * This method is thread safe in the sense that a concurrent write access never exposes
     * an invalid intermediate state to this read access.
     */
    public static Object getAttr(PyObject ob, byte attr_type) {
        /*
        Here used to be a synchronized(ob) block encapsulating the whole method code.
        It has been removed due to performance issues reported in
        github.com/jython/jython/issues/360.
        By making ob.attributes in PyObject and next and value in AttributeLink and
        TransientAttributeLink volatile, the read access in this method does not
        fatally interfere with a concurrent write operation.
        (Write operations continue to be synchronized against each other.)
        https://docs.oracle.com/javase/specs/jls/se17/html/jls-17.html#jls-17.4.4 states:
        "A write to a volatile variable v (§8.3.1.4) synchronizes-with all subsequent reads
         of v by any thread"
        This induces an x happens-before y relation hb(x, y) according to
        https://docs.oracle.com/javase/specs/jls/se17/html/jls-17.html#jls-17.4.5:
        "If an action x synchronizes-with a following action y, then we also have hb(x, y)."
        Also see https://www.cs.umd.edu/~pugh/java/memoryModel/DoubleCheckedLocking.html,
        which confirms the anticipated behavior of volatile fields for Java 5 and onwards.
        Write accesses in the linked attribute list are designed to update the list tail
        such that a concurrent read access would iterate either through the old state or
        the new state. No invalid intermediate state is exposed to the read access.
        Due to volatile semantics, this assumption cannot be broken by code reordering
        issues like pointed out in the linked DoubleCheckedLocking document.
        */
        Object oatt = ob.attributes; // atomic one-time access to ob.attributes, which is volatile
        if (oatt instanceof JyAttribute) {
            JyAttribute att = (JyAttribute) oatt;
            while (att != null && att.attr_type < attr_type) {
                att = att.getNext(); // atomic access to next, which is volatile
                // Perhaps att is affected by a concurrent write access, perhaps not.
                // Since the next-field is volatile, att is always a properly initialized
                // attribute sequence head at this point.
                // The overall iteration is therefore either according to the old state
                // or according to the new state.
            }
            return att != null && att.attr_type == attr_type ? att.getValue() : null;
        }
        return attr_type == JAVA_PROXY_ATTR ? oatt : null;
    }

    /**
     * Prints the current state of the attribute-list of the
     * given object to the given stream.
     * (Intended for debugging)
     */
    public static void debugPrintAttributes(PyObject o, java.io.PrintStream out) {
        synchronized (o) {
            out.println("debugPrintAttributes of " + System.identityHashCode(o) + ":");
            if (o.attributes == null) {
                out.println("null");
            } else if (!(o.attributes instanceof JyAttribute)) {
                out.println("only javaProxy");
            } else {
                JyAttribute att = (JyAttribute) o.attributes;
                while (att != null) {
                    out.println("att type: " + att.attr_type + " value: " + att.getValue());
                    att = att.getNext();
                }
            }
            out.println("debugPrintAttributes done");
        }
    }

    /**
     * Sets the attribute of type {@code attr_type} in {@code ob} to {@code value}. If no
     * corresponding attribute exists yet, one is created. If {@code value == null}, the attribute
     * is removed (if it existed at all).
     */
    public static void setAttr(PyObject ob, byte attr_type, Object value) {
        synchronized (ob) {
            if (value == null) {
                delAttr(ob, attr_type);
            } else {
                if (ob.attributes == null) {
                    if (attr_type == JyAttribute.JAVA_PROXY_ATTR) {
                        ob.attributes = value;
                    } else {
                        ob.attributes = attr_type < 0 ?
                                new AttributeLink(attr_type, value) :
                                new TransientAttributeLink(attr_type, value);
                    }
                } else if (!(ob.attributes instanceof JyAttribute)) {
                    if (attr_type == JyAttribute.JAVA_PROXY_ATTR) {
                        ob.attributes = value;
                    } else {
                        ob.attributes = new AttributeLink(JyAttribute.JAVA_PROXY_ATTR, ob.attributes);
                        ((JyAttribute) ob.attributes).setNext(attr_type < 0 ?
                                new AttributeLink(attr_type, value) :
                                new TransientAttributeLink(attr_type, value));
                    }
                } else {
                    JyAttribute att = (JyAttribute) ob.attributes;
                    if (att.attr_type > attr_type) {
                        JyAttribute newAtt = attr_type < 0 ?
                                new AttributeLink(attr_type, value) :
                                new TransientAttributeLink(attr_type, value);
                        newAtt.setNext(att);
                        ob.attributes = newAtt;
                    } else {
                        while (att.getNext() != null && att.getNext().attr_type <= attr_type) {
                            att = att.getNext();
                        }
                        if (att.attr_type == attr_type) {
                            att.setValue(value);
                        } else if (att.getNext() == null) {
                            att.setNext(attr_type < 0 ?
                                    new AttributeLink(attr_type, value) :
                                    new TransientAttributeLink(attr_type, value));
                        } else {
                            JyAttribute newAtt = attr_type < 0 ?
                                    new AttributeLink(attr_type, value) :
                                    new TransientAttributeLink(attr_type, value);
                            newAtt.setNext(att.getNext());
                            att.setNext(newAtt);
                        }
                    }
                }
            }
        }
    }

    /**
     * Removes the attribute of given type from the given object's attribute-list
     * (if it existed at all). This is equivalent to calling
     * {@code setAttr(ob, attr_type, null)}.
     */
    public static void delAttr(PyObject ob, byte attr_type) {
        synchronized (ob) {
            if (ob.attributes == null) {
                return;
            } else if (attr_type == JAVA_PROXY_ATTR && !(ob.attributes instanceof JyAttribute)) {
                ob.attributes = null;
            }
            JyAttribute att = (JyAttribute) ob.attributes;
            if (att.attr_type == attr_type) {
                ob.attributes = att.getNext();
            } else {
                while (att.getNext() != null && att.getNext().attr_type < attr_type) {
                    att = att.getNext();
                }
                if (att.getNext() != null && att.getNext().attr_type == attr_type) {
                    att.setNext(att.getNext().getNext());
                }
            }
            if (ob.attributes != null) {
                att = (JyAttribute) ob.attributes;
                if (att.getNext() == null && att.attr_type == JyAttribute.JAVA_PROXY_ATTR) {
                    ob.attributes = att.getValue();
                }
            }
        }
    }
}
