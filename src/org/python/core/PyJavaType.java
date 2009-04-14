package org.python.core;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.EventListener;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.python.core.util.StringUtil;
import org.python.util.Generic;

public class PyJavaType extends PyType {

    private final static Class<?>[] OO = {PyObject.class, PyObject.class};

    /** Deprecated methods in java.awt.* that have bean property equivalents we prefer. */
    private final static Set<String> BAD_AWT_METHODS = Generic.set("layout",
                                                                   "insets",
                                                                   "size",
                                                                   "minimumSize",
                                                                   "preferredSize",
                                                                   "maximumSize",
                                                                   "bounds",
                                                                   "enable");

    private static Map<Class<?>, PyBuiltinMethod[]> collectionProxies;

    /**
     * Other Java classes this type has MRO conflicts with. This doesn't matter for Java method
     * resolution, but if Python methods are added to the type, the added methods can't overlap with
     * methods added to any of the types in this set. If this type doesn't have any known conflicts,
     * this is null.
     */
    private Set<PyJavaType> conflicted;

    /**
     * The names of methods that have been added to this class.
     */
    private Set<String> modified;

    public static PyObject wrapJavaObject(Object o) {
        PyObject obj = new PyObjectDerived(PyType.fromClass(o.getClass()));
        obj.javaProxy = o;
        return obj;
    }

    public PyJavaType() {
        super(TYPE == null ? fromClass(PyType.class) : TYPE);
    }

    protected boolean useMetatypeFirst(PyObject attr) {
        return !(attr instanceof PyReflectedField || attr instanceof PyReflectedFunction);
    }

    // Java types are ok with things being added and removed from their dicts as long as there isn't
    @Override
    void type___setattr__(String name, PyObject value) {
        PyObject field = lookup(name);// If we have a static field that takes this, go with that
        if (field != null) {
            if (field._doset(null, value)) {
                return;
            }
        }
        if (modified == null) {
            modified = Generic.set();
        }
        if (modified.add(name)) {
            if (conflicted != null) {
                for (PyJavaType conflict : conflicted) {
                    if (conflict.modified != null && conflict.modified.contains(name)) {
                        throw Py.TypeError(getName()
                                + " does not have a consistent method resolution order with "
                                + conflict.getName() + ", and it already has " + name
                                + " added for Python");
                    }
                }
            }
        }
        object___setattr__(name, value);
        postSetattr(name);
    }

    @Override
    void type___delattr__(String name) {
        PyObject field = lookup(name);
        if (field == null) {
            throw Py.NameError("attribute not found: "+name);
        }
        if (!field.jdontdel()) {
            object___delattr__(name);
        }
        if (modified != null) {
            modified.remove(name);
        }
        postDelattr(name);
    }

    @Override
    void handleMroError(MROMergeState[] toMerge, List<PyObject> mro) {
        if (underlying_class != null) {
            // If this descends from PyObject, don't do the Java mro cleanup
            super.handleMroError(toMerge, mro);
        }
        Set<PyJavaType> inConflict = Generic.set();
        PyJavaType winner = null;
        for (MROMergeState mergee : toMerge) {
            for (int i = mergee.next; i < mergee.mro.length; i++) {
                if (mergee.mro[i] == PyObject.TYPE
                        || mergee.mro[i] == PyType.fromClass(Object.class)) {
                    continue;
                }
                if (winner == null) {
                    // Pick an arbitrary class to be added to the mro next and break the conflict.
                    // If method name conflicts were allowed between methods added to Java types,
                    // it would go first, but that's prevented, so being a winner doesn't actually
                    // get it anything
                    winner = (PyJavaType)mergee.mro[i];
                }
                inConflict.add((PyJavaType)mergee.mro[i]);
            }
        }

        Set<String> allModified = Generic.set();
        PyJavaType[] conflicted = inConflict.toArray(new PyJavaType[inConflict.size()]);
        for (PyJavaType type : conflicted) {
            if (type.modified == null) {
                continue;
            }
            for (String method : type.modified) {
                if (!allModified.add(method)) { // Another type in conflict has this method, fail
                    PyList types = new PyList();
                    for (PyJavaType othertype : conflicted) {
                        if (othertype.modified != null && othertype.modified.contains(method)) {
                            types.add(othertype);
                        }
                    }
                    throw Py.TypeError(String.format("Supertypes that share a modified attribute "
                            + " have an MRO conflict[attribute=%s, types=%s]", method, types));
                }
            }
        }

        // We can keep trucking, there aren't any existing method name conflicts.  Mark the
        // conflicts in all the classes so further method additions can check for trouble
        for (PyJavaType type : conflicted) {
            for (PyJavaType otherType : inConflict) {
                if (otherType != type) {
                    if (type.conflicted == null) {
                        type.conflicted = Generic.set();
                    }
                    type.conflicted.add(otherType);
                }
            }
        }

        // Add our winner to the mro, clear the clog, and try to finish the rest
        mro.add(winner);
        for (MROMergeState mergee : toMerge) {
            mergee.removeFromUnmerged(winner);
        }
        computeMro(toMerge, mro);
    }

    @Override
    protected void init(Class<?> forClass, Set<PyJavaType> needsInners) {
        name = forClass.getName();
        // Strip the java fully qualified class name from Py classes in core
        if (name.startsWith("org.python.core.Py")) {
            name = name.substring("org.python.core.Py".length()).toLowerCase();
        }
        dict = new PyStringMap();
        Class<?> baseClass = forClass.getSuperclass();
        if (PyObject.class.isAssignableFrom(forClass)) {
            // Non-exposed subclasses of PyObject use a simple linear mro to PyObject that ignores
            // their interfaces
            underlying_class = forClass;
            computeLinearMro(baseClass);
        } else {
                needsInners.add(this);
            javaProxy = forClass;
            objtype = PyType.fromClassSkippingInners(Class.class, needsInners);
            // Wrapped Java types fill in their mro first using all of their interfaces then their
            // super class.
            List<PyObject> visibleBases = Generic.list();
            for (Class<?> iface : forClass.getInterfaces()) {
                if (iface == PyProxy.class || iface == ClassDictInit.class) {
                    // Don't show the interfaces added by proxy type construction; otherwise Python
                    // subclasses of proxy types and another Java interface can't make a consistent
                    // mro
                    continue;
                }
                visibleBases.add(PyType.fromClassSkippingInners(iface, needsInners));
            }
            if (javaProxy == Object.class) {
                base = PyType.fromClassSkippingInners(PyObject.class, needsInners);
            } else if(baseClass == null) {
                base = PyType.fromClassSkippingInners(Object.class, needsInners);
            }else if (javaProxy == Class.class) {
                base = PyType.fromClassSkippingInners(PyType.class, needsInners);
            } else {
                base = PyType.fromClassSkippingInners(baseClass, needsInners);
            }
            visibleBases.add(base);
            this.bases = visibleBases.toArray(new PyObject[visibleBases.size()]);
            mro = computeMro();
        }

        // PyReflected* can't call or access anything from non-public classes that aren't in
        // org.python.core
        if (!Modifier.isPublic(forClass.getModifiers()) &&
                !name.startsWith("org.python.core") && Options.respectJavaAccessibility) {
            handleSuperMethodArgCollisions(forClass);
            return;
        }

        // Add methods and determine bean properties declared on this class
        Map<String, PyBeanProperty> props = Generic.map();
        Map<String, PyBeanEvent> events = Generic.map();
        Method[] methods;
        if (Options.respectJavaAccessibility) {
            // returns just the public methods
            methods = forClass.getMethods();
        } else {
            methods = forClass.getDeclaredMethods();
            for (Method method : methods) {
                method.setAccessible(true);
            }
        }

        boolean isInAwt = name.startsWith("java.awt.") && name.indexOf('.', 9) == -1;
        for (Method meth : methods) {
            if (!declaredOnMember(baseClass, meth) || ignore(meth)) {
                continue;
            }

            String methname = meth.getName();

            // Special case a few troublesome methods in java.awt.*. These methods are all
            // deprecated and interfere too badly with bean properties to be tolerated. This is
            // totally a hack but a lot of code that uses java.awt will break without it.
            if (isInAwt && BAD_AWT_METHODS.contains(methname)) {
                continue;
            }

            String nmethname = normalize(methname);
            PyReflectedFunction reflfunc = (PyReflectedFunction)dict.__finditem__(nmethname);
            if (reflfunc == null) {
                dict.__setitem__(nmethname, new PyReflectedFunction(meth));
            } else {
                reflfunc.addMethod(meth);
            }

            // Now check if this is a bean method, for which it must be an instance method
            if (Modifier.isStatic(meth.getModifiers())) {
                continue;
            }

            // First check if this is a bean event addition method
            int n = meth.getParameterTypes().length;
            if ((methname.startsWith("add") || methname.startsWith("set"))
                    && methname.endsWith("Listener") && n == 1 &&
                    meth.getReturnType() == Void.TYPE &&
                    EventListener.class.isAssignableFrom(meth.getParameterTypes()[0])) {
                Class<?> eventClass = meth.getParameterTypes()[0];
                String ename = eventClass.getName();
                int idot = ename.lastIndexOf('.');
                if (idot != -1) {
                    ename = ename.substring(idot + 1);
                }
                ename = normalize(StringUtil.decapitalize(ename));
                events.put(ename, new PyBeanEvent(ename, eventClass, meth));
                continue;
            }

            // Now check if it's a bean property accessor
            String name = null;
            boolean get = true;
            if (methname.startsWith("get") && methname.length() > 3 && n == 0) {
                name = methname.substring(3);
            } else if (methname.startsWith("is") && methname.length() > 2 && n == 0
                    && meth.getReturnType() == Boolean.TYPE) {
                name = methname.substring(2);
            } else if (methname.startsWith("set") && methname.length() > 3 && n == 1) {
                name = methname.substring(3);
                get = false;
            }
            if (name != null) {
                name = normalize(StringUtil.decapitalize(name));
                PyBeanProperty prop = props.get(name);
                if (prop == null) {
                    prop = new PyBeanProperty(name, null, null, null);
                    props.put(name, prop);
                }
                if (get) {
                    prop.getMethod = meth;
                    prop.myType = meth.getReturnType();
                } else {
                    prop.setMethod = meth;
                    // Needed for readonly properties.  Getter will be used instead
                    // if there is one.  Only works if setX method has exactly one
                    // param, which is the only reasonable case.
                    // XXX: should we issue a warning if setX and getX have different
                    // types?
                    if (prop.myType == null) {
                        Class[] params = meth.getParameterTypes();
                        if (params.length == 1) {
                            prop.myType = params[0];
                        }
                    }
                }
            }
        }

        // Add superclass methods
        for (Method meth : methods) {
            String nmethname = normalize(meth.getName());
            PyReflectedFunction reflfunc = (PyReflectedFunction)dict.__finditem__(nmethname);
            if (reflfunc != null) {
                // The superclass method has the same name as one declared on this class, so add
                // the superclass version's arguments
                reflfunc.addMethod(meth);
            } else if (PyReflectedFunction.isPackagedProtected(meth.getDeclaringClass())
                    && lookup(nmethname) == null) {
                // This method must be a public method from a package protected superclass.  It's
                // visible from Java on this class, so do the same for Python here.  This is the
                // flipside of what handleSuperMethodArgCollisions does for inherited public methods
                // on package protected classes.
                dict.__setitem__(nmethname, new PyReflectedFunction(meth));
            }
        }

        // Add fields declared on this type
        Field[] fields;
        if (Options.respectJavaAccessibility) {
            // returns just the public fields
            fields = forClass.getFields();
        } else {
            fields = forClass.getDeclaredFields();
            for (Field field : fields) {
                field.setAccessible(true);
            }
        }
        for (Field field : fields) {
            if (!declaredOnMember(baseClass, field)) {
                continue;
            }
            String fldname = field.getName();
            if (Modifier.isStatic(field.getModifiers())) {
                if (fldname.startsWith("__doc__") && fldname.length() > 7
                        && field.getType() == PyString.class) {
                    String fname = fldname.substring(7).intern();
                    PyObject memb = dict.__finditem__(fname);
                    if (memb != null && memb instanceof PyReflectedFunction) {
                        PyString doc = null;
                        try {
                            doc = (PyString)field.get(null);
                        } catch (IllegalAccessException e) {
                            throw Py.JavaError(e);
                        }
                        ((PyReflectedFunction)memb).__doc__ = doc;
                    }
                }
            }
            if (dict.__finditem__(normalize(fldname)) == null) {
                dict.__setitem__(normalize(fldname), new PyReflectedField(field));
            }
        }

        for (PyBeanEvent ev : events.values()) {
            if (dict.__finditem__(ev.__name__) == null) {
                dict.__setitem__(ev.__name__, ev);
            }

            for (Method meth : ev.eventClass.getMethods()) {
                String name = meth.getName().intern();
                if (dict.__finditem__(name) != null) {
                    continue;
                }
                dict.__setitem__(name, new PyBeanEventProperty(name,
                                                               ev.eventClass,
                                                               ev.addMethod,
                                                               meth));
            }
        }

        // Fill in the bean properties picked up while going through the methods
        for (PyBeanProperty prop : props.values()) {
            PyObject prev = dict.__finditem__(prop.__name__);
            if (prev != null) {
                if (!(prev instanceof PyReflectedField)
                        || !Modifier.isStatic(((PyReflectedField)prev).field.getModifiers())) {
                    // Any methods or non-static fields take precedence over the bean property
                    continue;
                } else {
                    // Must've been a static field, so add it to the property
                    prop.field = ((PyReflectedField)prev).field;
                }
            }

            // If one of our superclasses  has something defined for this name, check if its a bean
            // property, and if so, try to fill in any gaps in our property from there
            PyObject fromType[] = new PyObject[] { null };
            PyObject superForName = lookup_where(prop.__name__, fromType);
            if (superForName instanceof PyBeanProperty) {
                PyBeanProperty superProp = ((PyBeanProperty)superForName);
                // If it has a set method and we don't, take it regardless.  If the types don't line
                // up, it'll be rejected below
                if (prop.setMethod == null) {
                    prop.setMethod = superProp.setMethod;
                } else if (superProp.myType == prop.setMethod.getParameterTypes()[0]) {
                    // Otherwise, we must not have a get method. Only take a get method if the type
                    // on it agrees with the set method we already have. The bean on this type
                    // overrides a conflicting one o the parent
                    prop.getMethod = superProp.getMethod;
                    prop.myType = superProp.myType;
                }

                if (prop.field == null) {
                    // If the parent bean is hiding a static field, we need it as well.
                    prop.field = superProp.field;
                }
            } else if (superForName != null  &&  fromType[0] != this  &&  !(superForName instanceof PyBeanEvent)) {
                // There is already an entry for this name
                // It came from a type which is not @this; it came from a superclass
                // It is not a bean event
                // Do not override methods defined in superclass
                continue;
            }
            // If the return types on the set and get methods for a property don't agree, the get
            // method takes precedence
            if (prop.getMethod != null && prop.setMethod != null
                    && prop.myType != prop.setMethod.getParameterTypes()[0]) {
                prop.setMethod = null;
            }
            dict.__setitem__(prop.__name__, prop);
        }

        final PyReflectedConstructor reflctr = new PyReflectedConstructor("_new_impl");
        Constructor<?>[] constructors;
        // No matter the security manager, trying to set the constructor on class to accessible
        // blows up
        if (Options.respectJavaAccessibility || Class.class == forClass) {
            // returns just the public constructors
            constructors = forClass.getConstructors();
        } else {
            constructors = forClass.getDeclaredConstructors();
            for (Constructor<?> ctr : constructors) {
                ctr.setAccessible(true);
            }
        }
        for (Constructor<?> ctr : constructors) {
            reflctr.addConstructor(ctr);
        }
        if (PyObject.class.isAssignableFrom(forClass)) {
            PyObject new_ = new PyNewWrapper(forClass, "__new__", -1, -1) {

                public PyObject new_impl(boolean init,
                                         PyType subtype,
                                         PyObject[] args,
                                         String[] keywords) {
                    return reflctr.make(args, keywords);
                }
            };
            dict.__setitem__("__new__", new_);
        } else {
            dict.__setitem__("__init__", reflctr);
        }
        for (Map.Entry<Class<?>, PyBuiltinMethod[]> entry : getCollectionProxies().entrySet()) {
            if (entry.getKey() == forClass) {
                for (PyBuiltinMethod meth : entry.getValue()) {
                    addMethod(meth);
                }
            }
        }
        if (ClassDictInit.class.isAssignableFrom(forClass) && forClass != ClassDictInit.class) {
            try {
                Method m = forClass.getMethod("classDictInit", PyObject.class);
                m.invoke(null, dict);
                // allow the class to override its name after it is loaded
                PyObject nameSpecified = dict.__finditem__("__name__");
                if (nameSpecified != null) {
                    name = nameSpecified.toString();
                }
            } catch (Exception exc) {
                throw Py.JavaError(exc);
            }
        }
        if (baseClass != Object.class) {
            has_set = getDescrMethod(forClass, "__set__", OO) != null
                    || getDescrMethod(forClass, "_doset", OO) != null;
            has_delete = getDescrMethod(forClass, "__delete__", PyObject.class) != null
                    || getDescrMethod(forClass, "_dodel", PyObject.class) != null;
        }
        if (forClass == Object.class) {
            // Pass __eq__ and __repr__ through to subclasses of Object
            addMethod(new PyBuiltinMethodNarrow("__eq__", 1) {
                @Override
                public PyObject __call__(PyObject o) {
                    Object proxy = self.getJavaProxy();
                    Object oAsJava = o.__tojava__(proxy.getClass());
                    return proxy.equals(oAsJava) ? Py.True : Py.False;
                }
            });
            addMethod(new PyBuiltinMethodNarrow("__ne__", 1) {
                @Override
                public PyObject __call__(PyObject o) {
                    Object proxy = self.getJavaProxy();
                    Object oAsJava = o.__tojava__(proxy.getClass());
                    return !proxy.equals(oAsJava) ? Py.True : Py.False;
                }
            });
            addMethod(new PyBuiltinMethodNarrow("__hash__") {
                @Override
                public PyObject __call__() {
                    return Py.newInteger(self.getJavaProxy().hashCode());
                }
            });
            addMethod(new PyBuiltinMethodNarrow("__repr__") {
                @Override
                public PyObject __call__() {
                    return Py.newString(self.getJavaProxy().toString());
                }
            });
        }
        if(forClass == Comparable.class) {
            addMethod(new ComparableMethod("__lt__", 1) {
                @Override
                protected boolean getResult(int comparison) {
                    return comparison < 0;
                }
            });
            addMethod(new ComparableMethod("__le__", 1) {
                @Override
                protected boolean getResult(int comparison) {
                    return comparison <= 0;
                }
            });
            addMethod(new ComparableMethod("__gt__", 1) {
                @Override
                protected boolean getResult(int comparison) {
                    return comparison > 0;
                }
            });
            addMethod(new ComparableMethod("__ge__", 1) {
                @Override
                protected boolean getResult(int comparison) {
                    return comparison >= 0;
                }
            });
        }
    }

    /**
     * Private, protected or package protected classes that implement public interfaces or extend
     * public classes can't have their implementations of the methods of their supertypes called
     * through reflection due to Sun VM bug 4071957(http://tinyurl.com/le9vo). They can be called
     * through the supertype version of the method though. Unfortunately we can't just let normal
     * mro lookup of those methods handle routing the call to the correct version as a class can
     * implement interfaces or classes that each have methods with the same name that takes
     * different number or types of arguments. Instead this method goes through all interfaces
     * implemented by this class, and combines same-named methods into a single PyReflectedFunction.
     *
     * Prior to Jython 2.5, this was handled in PyJavaClass.setMethods by setting methods in package
     * protected classes accessible which made them callable through reflection. That had the
     * drawback of failing when running in a security environment that didn't allow setting
     * accessibility, so this method replaced it.
     */
    private void handleSuperMethodArgCollisions(Class<?> forClass) {
        for (Class<?> iface : forClass.getInterfaces()) {
            mergeMethods(iface);
        }
        if (forClass.getSuperclass() != null) {
            mergeMethods(forClass.getSuperclass());
        }
    }

    private void mergeMethods(Class<?> parent) {
        for (Method meth : parent.getMethods()) {
            if (!Modifier.isPublic(meth.getDeclaringClass().getModifiers())) {
                // Ignore methods from non-public interfaces as they're similarly bugged
                continue;
            }
            String nmethname = normalize(meth.getName());
            PyObject[] where = new PyObject[1];
            PyObject obj = lookup_where(nmethname, where);
            if (obj == null) {
                // Nothing in our supertype hierarchy defines something with this name, so it
                // must not be visible there.
                continue;
            } else if (where[0] == this) {
                // This method is the only thing defining items in this class' dict, so it must
                // be a PyReflectedFunction created here. See if it needs the current method
                // added to it.
                if (!((PyReflectedFunction)obj).handles(meth)) {
                    ((PyReflectedFunction)obj).addMethod(meth);
                }
            } else {
                // There's something in a superclass with the same name. Add an item to this type's
                // dict to hide it.  If it's this method, nothing's changed.  If it's a field, we
                // want to make the method visible.  If it's a different method, it'll be added to
                // the reflected function created here in a later call.
                dict.__setitem__(nmethname, new PyReflectedFunction(meth));
            }
        }
    }

    private static boolean declaredOnMember(Class<?> base, Member declaring) {
        return base == null || (declaring.getDeclaringClass() != base &&
                base.isAssignableFrom(declaring.getDeclaringClass()));
    }

    private static String normalize(String name) {
        if (name.endsWith("$")) {
            name = name.substring(0, name.length() - 1);
        }
        return name.intern();
    }

    private static Method getDescrMethod(Class<?> c, String name, Class<?>... parmtypes) {
        Method meth;
        try {
            meth = c.getMethod(name, parmtypes);
        } catch (NoSuchMethodException e) {
            return null;
        }
        if (!Modifier.isStatic(meth.getModifiers()) && meth.getDeclaringClass() != PyObject.class) {
            return meth;
        }
        return null;
    }

    private static boolean ignore(Method meth) {
        Class<?>[] exceptions = meth.getExceptionTypes();
        for (Class<?> exception : exceptions) {
            if (exception == PyIgnoreMethodTag.class) {
                return true;
            }
        }
        return false;
    }

    private static class EnumerationIter extends PyIterator {

        private Enumeration<Object> proxy;

        public EnumerationIter(Enumeration<Object> proxy) {
            this.proxy = proxy;
        }

        public PyObject __iternext__() {
            return proxy.hasMoreElements() ? Py.java2py(proxy.nextElement()) : null;
        }
    }

    private static class IteratorIter extends PyIterator {

        private Iterator<Object> proxy;

        public IteratorIter(Iterable<Object> proxy) {
            this(proxy.iterator());
        }

        public IteratorIter(Iterator<Object> proxy) {
            this.proxy = proxy;
        }

        public PyObject __iternext__() {
            return proxy.hasNext() ? Py.java2py(proxy.next()) : null;
        }
    }

    private static class ListMethod extends PyBuiltinMethodNarrow {
        protected ListMethod(String name, int numArgs) {
            super(name, numArgs);
        }

        protected List<Object> asList(){
            return (List<Object>)self.getJavaProxy();
        }
    }

    private static class MapMethod extends PyBuiltinMethodNarrow {
        protected MapMethod(String name, int numArgs) {
            super(name, numArgs);
        }

        protected Map<Object, Object> asMap(){
            return (Map<Object, Object>)self.getJavaProxy();
        }
    }

    private static abstract class ComparableMethod extends PyBuiltinMethodNarrow {
        protected ComparableMethod(String name, int numArgs) {
            super(name, numArgs);
        }
        @Override
        public PyObject __call__(PyObject arg) {
            Object asjava = arg.__tojava__(Object.class);
            int compare;
            try {
                compare = ((Comparable<Object>)self.getJavaProxy()).compareTo(asjava);
            } catch(ClassCastException classCast) {
                return Py.NotImplemented;
            }
            return getResult(compare) ? Py.True : Py.False;
        }

        protected abstract boolean getResult(int comparison);
    }

    private static Map<Class<?>, PyBuiltinMethod[]> getCollectionProxies() {
        if (collectionProxies == null) {
            collectionProxies = Generic.map();

            PyBuiltinMethodNarrow iterableProxy = new PyBuiltinMethodNarrow("__iter__") {
                public PyObject __call__() {
                    return new IteratorIter(((Iterable)self.getJavaProxy()));
                }
            };
            collectionProxies.put(Iterable.class, new PyBuiltinMethod[] {iterableProxy});

            PyBuiltinMethodNarrow lenProxy = new PyBuiltinMethodNarrow("__len__") {
                @Override
                public PyObject __call__() {
                    return Py.newInteger(((Collection<?>)self.getJavaProxy()).size());
                }
            };

            PyBuiltinMethodNarrow containsProxy = new PyBuiltinMethodNarrow("__contains__") {
                @Override
                public PyObject __call__(PyObject obj) {
                    Object other = obj.__tojava__(Object.class);
                    boolean contained = ((Collection<?>)self.getJavaProxy()).contains(other);
                    return contained ? Py.True : Py.False;
                }
            };
            collectionProxies.put(Collection.class, new PyBuiltinMethod[] {lenProxy,
                                                                           containsProxy});

            PyBuiltinMethodNarrow iteratorProxy = new PyBuiltinMethodNarrow("__iter__") {
                public PyObject __call__() {
                    return new IteratorIter(((Iterator)self.getJavaProxy()));
                }
            };
            collectionProxies.put(Iterator.class, new PyBuiltinMethod[] {iteratorProxy});

            PyBuiltinMethodNarrow enumerationProxy = new PyBuiltinMethodNarrow("__iter__") {
                public PyObject __call__() {
                    return new EnumerationIter(((Enumeration)self.getJavaProxy()));
                }
            };
            collectionProxies.put(Enumeration.class, new PyBuiltinMethod[] {enumerationProxy});

            // Map doesn't extend Collection, so it needs its own version of len, iter and contains
            PyBuiltinMethodNarrow mapLenProxy = new MapMethod("__len__", 0) {
                @Override
                public PyObject __call__() {
                    return Py.java2py(asMap().size());
                }
            };
            PyBuiltinMethodNarrow mapIterProxy = new MapMethod("__iter__", 0) {
                @Override
                public PyObject __call__() {
                    return new IteratorIter(asMap().keySet());
                }
            };
            PyBuiltinMethodNarrow mapContainsProxy = new MapMethod("__contains__", 1) {
                public PyObject __call__(PyObject obj) {
                    Object other = obj.__tojava__(Object.class);
                    return asMap().containsKey(other) ? Py.True : Py.False;
                }
            };
            PyBuiltinMethodNarrow mapGetProxy = new MapMethod("__getitem__", 1) {
                @Override
                public PyObject __call__(PyObject key) {
                    return Py.java2py(asMap().get(Py.tojava(key, Object.class)));
                }
            };
            PyBuiltinMethodNarrow mapPutProxy = new MapMethod("__setitem__", 2) {
                @Override
                public PyObject __call__(PyObject key, PyObject value) {
                    return Py.java2py(asMap().put(Py.tojava(key, Object.class),
                                                  Py.tojava(value, Object.class)));
                }
            };
            PyBuiltinMethodNarrow mapRemoveProxy = new MapMethod("__delitem__", 1) {
                @Override
                public PyObject __call__(PyObject key) {
                    return Py.java2py(asMap().remove(Py.tojava(key, Object.class)));
                }
            };
            collectionProxies.put(Map.class, new PyBuiltinMethod[] {mapLenProxy,
                                                                    mapIterProxy,
                                                                    mapContainsProxy,
                                                                    mapGetProxy,
                                                                    mapPutProxy,
                                                                    mapRemoveProxy});

            PyBuiltinMethodNarrow listGetProxy = new ListMethod("__getitem__", 1) {
                @Override
                public PyObject __call__(PyObject key) {
                    return new ListIndexDelegate(asList()).checkIdxAndGetItem(key);
                }
            };
            PyBuiltinMethodNarrow listSetProxy = new ListMethod("__setitem__", 2) {
                @Override
                public PyObject __call__(PyObject key, PyObject value) {
                    new ListIndexDelegate(asList()).checkIdxAndSetItem(key, value);
                    return Py.None;
                }
            };
            PyBuiltinMethodNarrow listRemoveProxy = new ListMethod("__delitem__", 1) {
                 @Override
                public PyObject __call__(PyObject key) {
                     new ListIndexDelegate(asList()).checkIdxAndDelItem(key);
                     return Py.None;
                }
            };
            collectionProxies.put(List.class, new PyBuiltinMethod[] {listGetProxy,
                                                                     listSetProxy,
                                                                     listRemoveProxy});
        }
        return collectionProxies;
    }

    protected static class ListIndexDelegate extends SequenceIndexDelegate {

        private final List list;

        public ListIndexDelegate(List list) {
            this.list = list;
        }
        @Override
        public void delItem(int idx) {
            list.remove(idx);
        }

        @Override
        public PyObject getItem(int idx) {
            return Py.java2py(list.get(idx));
        }

        @Override
        public PyObject getSlice(int start, int stop, int step) {
            if (step > 0 && stop < start) {
                stop = start;
            }
            int n = PySequence.sliceLength(start, stop, step);
            List<Object> newList;
             try {
                newList = list.getClass().newInstance();
            } catch (Exception e) {
                throw Py.JavaError(e);
            }
            int j = 0;
            for (int i = start; j < n; i += step) {
                newList.add(list.get(i));
                j++;
            }
            return Py.java2py(newList);
        }

        @Override
        public String getTypeName() {
            return list.getClass().getName();
        }

        @Override
        public int len() {
            return list.size();
        }

        @Override
        public void setItem(int idx, PyObject value) {
            list.set(idx, value.__tojava__(Object.class));
        }

        @Override
        public void setSlice(int start, int stop, int step, PyObject value) {
            if (stop < start) {
                stop = start;
            }
            if (step == 0) {
                return;
            }
            if (value.javaProxy == this) {
                List newseq = new ArrayList(len());
                for (Object object : ((List)value.javaProxy)) {
                    newseq.add(object);
                }
                value = Py.java2py(newseq);
            }
            int j = start;
            for (PyObject obj : value.asIterable()) {
                setItem(j, obj);
                j += step;
            }
        }

        @Override
        public void delItems(int start, int stop) {
            int n = stop - start;
            while (n-- > 0) {
                delItem(start);
            }
        }
    }
}
